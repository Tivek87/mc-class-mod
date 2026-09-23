package nl.tivek.welcomescreen.client.character.lantern;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.lantern.Arrival;
import nl.tivek.welcomescreen.character.lantern.PowerRing;
import nl.tivek.welcomescreen.network.RingPayload;

/**
 * How full every Green Lantern's ring around you is (the server tells), so it glows that brightly; who is
 * recharging theirs at the lantern right now, so the lantern and the punch can be played; who flies; and which
 * of the ring's lasting shapes everyone holds up.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class ClientRing {
    // A recharge the server stopped telling about (a lost packet) is dropped this long after it should end.
    private static final int SLACK = 10;
    // A new start from the server only moves a recharge that is already playing when it is this far off.
    private static final int RESYNC = 3;
    // How far back the power of your own ring is remembered, in ticks, to tell how fast it drains.
    private static final int HISTORY = 20;

    private static final Map<Integer, State> RINGS = new HashMap<>();
    private static final float[] OWN = new float[HISTORY];
    private static int ownCount;
    private static int clientTicks;

    private ClientRing() {
    }

    /**
     * One ring.
     *
     * @param power   0 up to {@link PowerRing#MAX_POWER}
     * @param pending what the fist being charged would cost now
     * @param since   the client tick the recharge started, or {@link Integer#MIN_VALUE} when there is none
     * @param took    the client tick its owner took off, or {@link Integer#MIN_VALUE} while he is not flying
     * @param state   what the ring holds up or pours out, see {@link RingPayload#SHIELD}
     * @param came    the client tick the ring set out to make him Green Lantern, or {@link Integer#MIN_VALUE} when
     *                that is over (see {@link Arrival})
     * @param from    where the ring showed up for that, or null
     * @param charged the client tick the ring started gathering its light for the beam, or {@link Integer#MIN_VALUE}
     *                while it gathers none
     */
    private record State(float power, float pending, int since, int took, int state, int came, @Nullable Vec3 from,
            int charged) {
        boolean recharging() {
            return this.since != Integer.MIN_VALUE && clientTicks - this.since < PowerRing.RECHARGE_TICKS + SLACK;
        }
    }

    public static void update(RingPayload payload) {
        State old = RINGS.get(payload.entity());
        int since = Integer.MIN_VALUE;
        if (payload.lantern() >= 0) {
            int told = clientTicks - payload.lantern();
            // Keep a recharge that is already playing where it is, so it never stutters.
            since = old != null && old.recharging() && Math.abs(old.since() - told) <= RESYNC ? old.since() : told;
        }
        int took = Integer.MIN_VALUE;
        if (payload.flight() >= 0) {
            int told = clientTicks - payload.flight();
            // The same for a flight: its take-off keeps playing where it is.
            took = old != null && old.took() != Integer.MIN_VALUE && Math.abs(old.took() - told) <= RESYNC
                    ? old.took() : told;
        }
        int came = Integer.MIN_VALUE;
        if (payload.arrival() >= 0) {
            int told = clientTicks - payload.arrival();
            // The same for the ring's arrival.
            came = old != null && old.came() != Integer.MIN_VALUE && Math.abs(old.came() - told) <= RESYNC
                    ? old.came() : told;
        }
        int charged = Integer.MIN_VALUE;
        if (payload.charge() >= 0) {
            int told = clientTicks - payload.charge();
            // The same for the light the ring gathers for the beam.
            charged = old != null && old.charged() != Integer.MIN_VALUE && Math.abs(old.charged() - told) <= RESYNC
                    ? old.charged() : told;
        }
        RINGS.put(payload.entity(), new State(payload.power(), payload.pending(), since, took, payload.state(), came,
                payload.from(), charged));
    }

    /**
     * How many ticks ago this player's ring started gathering its light for the beam (with the part of a tick), or -1
     * while it gathers none. Your own is better known from your own button (see {@link BeamCharge}).
     */
    static float charging(Entity player, float partialTick) {
        State state = RINGS.get(player.getId());
        return state == null || state.charged() == Integer.MIN_VALUE ? -1.0F
                : Math.max(0.0F, clientTicks - state.charged() + partialTick);
    }

    /**
     * How many ticks ago the ring set out to make this player Green Lantern (with the part of a tick), or -1 when
     * that is over or never was (see {@link Arrival}).
     */
    public static float arrival(Entity player, float partialTick) {
        State state = RINGS.get(player.getId());
        if (state == null || state.came() == Integer.MIN_VALUE) {
            return -1.0F;
        }
        float ticks = clientTicks - state.came() + partialTick;
        return ticks < Arrival.TICKS + SLACK ? Math.max(0.0F, ticks) : -1.0F;
    }

    /** Where the ring showed up to make this player Green Lantern, or null when it is not on its way. */
    @Nullable
    static Vec3 arrivalFrom(Entity player) {
        State state = RINGS.get(player.getId());
        return state == null || state.came() == Integer.MIN_VALUE ? null : state.from();
    }

    /** How many ticks ago this player took off (with the part of a tick), or -1 while he is not flying. */
    public static float flight(Entity player, float partialTick) {
        State state = RINGS.get(player.getId());
        return state == null || state.took() == Integer.MIN_VALUE ? -1.0F
                : Math.max(0.0F, clientTicks - state.took() + partialTick);
    }

    /** True when this player's ring does this right now, see {@link RingPayload#SHIELD}. */
    public static boolean has(Entity player, int bit) {
        State state = RINGS.get(player.getId());
        return state != null && (state.state() & bit) != 0;
    }

    /**
     * How fast your own ring drains by itself right now, in power a second, measured over the last second; 0 while
     * it does not drain. One-off costs (a bolt, a fist) and a recharge are left out: of the drops from tick to
     * tick only the middle ones count.
     */
    public static float drain() {
        if (ownCount < HISTORY) {
            return 0.0F;
        }
        float[] drops = new float[HISTORY - 1];
        for (int i = 0; i < drops.length; i++) {
            drops[i] = OWN[(clientTicks + i) % HISTORY] - OWN[(clientTicks + i + 1) % HISTORY];
        }
        Arrays.sort(drops);
        float sum = 0.0F;
        int trim = 4;
        for (int i = trim; i < drops.length - trim; i++) {
            sum += drops[i];
        }
        return Math.max(0.0F, sum / (drops.length - 2 * trim) * 20.0F);
    }

    /** How full this player's ring is, 0 to 1. A ring nobody told us about is full. */
    public static float charge(Entity player) {
        State state = RINGS.get(player.getId());
        return state == null ? 1.0F : state.power() / PowerRing.MAX_POWER;
    }

    /** How much power this player's ring holds, 0 up to {@link PowerRing#MAX_POWER}. */
    public static float power(Entity player) {
        State state = RINGS.get(player.getId());
        return state == null ? PowerRing.MAX_POWER : state.power();
    }

    /** What the fist this player charges would cost if they let go now; 0 when they charge none. */
    public static float pending(Entity player) {
        State state = RINGS.get(player.getId());
        return state == null ? 0.0F : state.pending();
    }

    /**
     * How far into recharging at the lantern this player is, in ticks (with the part of a tick), or -1 when
     * they are not recharging.
     */
    public static float recharge(Entity player, float partialTick) {
        State state = RINGS.get(player.getId());
        if (state == null || !state.recharging()) {
            return -1.0F;
        }
        float ticks = clientTicks - state.since() + partialTick;
        return ticks < PowerRing.RECHARGE_TICKS ? ticks : -1.0F;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && !minecraft.isPaused()) {
            if (minecraft.player != null) {
                OWN[clientTicks % HISTORY] = power(minecraft.player);
                ownCount = Math.min(HISTORY, ownCount + 1);
            }
            clientTicks++;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RINGS.clear();
        ownCount = 0;
    }
}
