package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientRing {
    // A recharge the server stopped telling about (a lost packet) is dropped this long after it should end.
    private static final int SLACK = 10;
    // A new server start only nudges an already-playing recharge, so it never stutters.
    private static final int RESYNC = 3;
    private static final int HISTORY = 20;

    private static final Map<Integer, State> RINGS = new HashMap<>();
    private static final float[] OWN = new float[HISTORY];
    private static int ownCount;
    private static int clientTicks;

    private ClientRing() {
    }

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
            since = old != null && old.recharging() && Math.abs(old.since() - told) <= RESYNC ? old.since() : told;
        }
        int took = Integer.MIN_VALUE;
        if (payload.flight() >= 0) {
            int told = clientTicks - payload.flight();
            took = old != null && old.took() != Integer.MIN_VALUE && Math.abs(old.took() - told) <= RESYNC
                    ? old.took() : told;
        }
        int came = Integer.MIN_VALUE;
        if (payload.arrival() >= 0) {
            int told = clientTicks - payload.arrival();
            came = old != null && old.came() != Integer.MIN_VALUE && Math.abs(old.came() - told) <= RESYNC
                    ? old.came() : told;
        }
        int charged = Integer.MIN_VALUE;
        if (payload.charge() >= 0) {
            int told = clientTicks - payload.charge();
            charged = old != null && old.charged() != Integer.MIN_VALUE && Math.abs(old.charged() - told) <= RESYNC
                    ? old.charged() : told;
        }
        RINGS.put(payload.entity(), new State(payload.power(), payload.pending(), since, took, payload.state(), came,
                payload.from(), charged));
    }

    public static float charging(Entity player, float partialTick) {
        State state = RINGS.get(player.getId());
        return state == null || state.charged() == Integer.MIN_VALUE ? -1.0F
                : Math.max(0.0F, clientTicks - state.charged() + partialTick);
    }

    public static float arrival(Entity player, float partialTick) {
        State state = RINGS.get(player.getId());
        if (state == null || state.came() == Integer.MIN_VALUE) {
            return -1.0F;
        }
        float ticks = clientTicks - state.came() + partialTick;
        return ticks < Arrival.TICKS + SLACK ? Math.max(0.0F, ticks) : -1.0F;
    }

    @Nullable
    public static Vec3 arrivalFrom(Entity player) {
        State state = RINGS.get(player.getId());
        return state == null || state.came() == Integer.MIN_VALUE ? null : state.from();
    }

    public static float flight(Entity player, float partialTick) {
        State state = RINGS.get(player.getId());
        return state == null || state.took() == Integer.MIN_VALUE ? -1.0F
                : Math.max(0.0F, clientTicks - state.took() + partialTick);
    }

    public static boolean has(Entity player, int bit) {
        State state = RINGS.get(player.getId());
        return state != null && (state.state() & bit) != 0;
    }

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

    public static float charge(Entity player) {
        State state = RINGS.get(player.getId());
        return state == null ? 1.0F : state.power() / PowerRing.MAX_POWER;
    }

    public static float power(Entity player) {
        State state = RINGS.get(player.getId());
        return state == null ? PowerRing.MAX_POWER : state.power();
    }

    public static float pending(Entity player) {
        State state = RINGS.get(player.getId());
        return state == null ? 0.0F : state.pending();
    }

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
            int own = minecraft.player == null ? Integer.MIN_VALUE : minecraft.player.getId();
            ClientLevel level = minecraft.level;
            // Out of sight is forgotten: the server tells you afresh once you see him come back into view.
            RINGS.keySet().removeIf(id -> id != own && level.getEntity(id) == null);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RINGS.clear();
        ownCount = 0;
    }
}
