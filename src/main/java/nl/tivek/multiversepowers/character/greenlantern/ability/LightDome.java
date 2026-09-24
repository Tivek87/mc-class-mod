package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

/**
 * The dome: what the ring does when Green Lantern holds the button of the hand that defends for a while. The
 * shield opens out into a dome of hard light all around him, that takes part of every hit from any side, for as
 * long as he keeps the button down. It covers more than the shield and holds less. While he flies it catches the
 * wind like a brake chute, and his own game halves his speed.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class LightDome implements Effect {
    // How wide the dome is, in blocks: it has to hold the whole of him.
    private static final float SIZE = 3.1F;
    private static final double VIEW_RANGE = 128.0;
    // How long it takes to open out, and to fall apart, in ticks.
    private static final int OPEN_TICKS = 5;

    private static final Map<UUID, LightDome> UP = new HashMap<>();

    private final int id;
    private final ServerPlayer owner;
    private final float kept;
    private final float perTick;
    private int open;
    private int closing = -1;
    private int flash;

    private LightDome(ServerPlayer owner, CharacterAbility ability) {
        this.id = PowerRing.newId();
        this.owner = owner;
        this.kept = (float) ability.value("domeDamageKept");
        this.perTick = (float) (ability.value("domePowerPerSecond") / 20.0);
    }

    /**
     * The button has been held long enough: the dome goes up.
     *
     * @return true when it went up
     */
    static boolean raise(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (UP.containsKey(owner.getUUID()) || Recharge.busy(owner) || Flight.descending(owner)) {
            return false;
        }
        if (PowerRing.power(owner) <= 0.0F) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        LightDome dome = new LightDome(owner, ability);
        UP.put(owner.getUUID(), dome);
        Effects.start(level, dome);
        dome.send(level);
        level.playSound(null, owner.getX(), owner.getY() + 1.0, owner.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 0.7F, 1.7F);
        level.playSound(null, owner.getX(), owner.getY() + 1.0, owner.getZ(), SoundEvents.AMETHYST_CLUSTER_PLACE,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        PowerRing.sync(owner);
        return true;
    }

    /**
     * The button comes up: the dome comes down.
     *
     * @return true when there was a dome
     */
    static boolean lower(ServerPlayer owner) {
        LightDome dome = UP.remove(owner.getUUID());
        if (dome == null) {
            return false;
        }
        dome.closing = 0;
        owner.level().playSound(null, owner.getX(), owner.getY() + 1.0, owner.getZ(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8F, 1.1F);
        PowerRing.sync(owner);
        return true;
    }

    /** True while this player holds the dome up. */
    public static boolean up(ServerPlayer player) {
        return UP.containsKey(player.getUUID());
    }

    /** The server stops: every dome is gone. */
    public static void clear() {
        UP.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (this.closing >= 0) {
            this.closing++;
            if (this.closing >= OPEN_TICKS) {
                ConstructPayload.sendRemove(level, this.id, this.owner.position());
                return false;
            }
            this.send(level);
            return true;
        }
        if (UP.get(this.owner.getUUID()) != this) {
            this.closing = 0;
            return true;
        }
        float power = PowerRing.power(this.owner);
        if (!PowerRing.fuels(this.owner, level) || power <= 0.0F || Flight.descending(this.owner)) {
            UP.remove(this.owner.getUUID(), this);
            this.closing = 0;
            if (power <= 0.0F) {
                PowerRing.tell(this.owner, "no_power");
            }
            this.send(level);
            PowerRing.sync(this.owner);
            return true;
        }
        PowerRing.setPower(this.owner, power - this.perTick);
        this.open = Math.min(OPEN_TICKS, this.open + 1);
        this.flash = Math.max(0, this.flash - 1);
        this.send(level);
        return true;
    }

    private Vec3 middle() {
        return this.owner.getBoundingBox().getCenter();
    }

    private void send(ServerLevel level) {
        Vec3 at = this.middle();
        float shown = this.closing >= 0 ? 1.0F - (float) this.closing / OPEN_TICKS : (float) this.open / OPEN_TICKS;
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), at, this.owner.getLookAngle(), SIZE, shown,
                        this.flash > 0 ? 1.0F : 0.0F, true, ConstructPayload.DOME));
    }

    /** Every hit on a player under the dome, from whatever side it comes. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LightDome dome = UP.get(player.getUUID());
        if (dome == null || event.getAmount() <= 0.0F || LightShield.goesThrough(event.getSource())) {
            return;
        }
        float before = event.getAmount();
        event.setAmount(before * dome.kept);
        dome.flash = 3;
        ServerLevel level = player.serverLevel();
        Vec3 from = event.getSource().getSourcePosition();
        Vec3 middle = dome.middle();
        // The light flares on the side the hit came from.
        Vec3 at = from == null || from.distanceToSqr(middle) < 1.0E-4 ? middle
                : middle.add(from.subtract(middle).normalize().scale(SIZE * 0.5));
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.GREEN, 1.3F), at, 10 + (int) Math.min(16.0F, before),
                0.25);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.7F);
    }
}
