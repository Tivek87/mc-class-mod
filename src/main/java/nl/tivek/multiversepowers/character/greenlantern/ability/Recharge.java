package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

public final class Recharge implements Effect {
    private static final Map<UUID, Recharge> ACTIVE = new HashMap<>();

    private final ServerPlayer owner;
    private final float restore;
    private int ticks;

    private Recharge(ServerPlayer owner, float restore) {
        this.owner = owner;
        this.restore = restore;
    }

    public static boolean recharge(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (busy(owner)) {
            return false;
        }
        if (GiantFist.holding(owner)) {
            PowerRing.tell(owner, "busy_fist");
            return false;
        }
        // In the air it works too, but not during the take-off: both fists are busy lifting him then.
        int flying = Flight.ticks(owner);
        if (flying >= 0 && flying < Flight.ARISE_TICKS) {
            PowerRing.tell(owner, "busy_flying");
            return false;
        }
        if (PowerRing.power(owner) >= PowerRing.MAX_POWER - 0.01F) {
            PowerRing.tell(owner, "full");
            return false;
        }
        // Both hands are needed for the lantern: whatever the mouse held up or poured out stops.
        LightBeam.stop(owner);
        LightShield.stop(owner);
        LightDome.lower(owner);
        Recharge lantern = new Recharge(owner, (float) ability.value("powerRestored"));
        ACTIVE.put(owner.getUUID(), lantern);
        Effects.start(level, lantern);
        lantern.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.8F, 0.8F);
        lantern.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
        PowerRing.sync(owner);
        return true;
    }

    public static void arrive(ServerPlayer owner, ServerLevel level) {
        CharacterAbility ability = GameCharacter.GREEN_LANTERN.byName("recharge");
        if (busy(owner) || ability == null) {
            return;
        }
        Recharge lantern = new Recharge(owner, (float) ability.value("powerRestored"));
        ACTIVE.put(owner.getUUID(), lantern);
        Effects.start(level, lantern);
        lantern.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.8F, 0.8F);
        PowerRing.sync(owner);
    }

    static boolean busy(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static int ticks(ServerPlayer player) {
        Recharge lantern = ACTIVE.get(player.getUUID());
        return lantern == null ? -1 : lantern.ticks;
    }

    public static void clear() {
        ACTIVE.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            return false;
        }
        if (!PowerRing.fuels(this.owner, level)) {
            this.stop();
            return false;
        }
        this.ticks++;
        if (this.ticks == PowerRing.RECHARGE_HIT) {
            this.hit(level);
        } else if (this.ticks > PowerRing.RECHARGE_HIT && this.ticks < PowerRing.RECHARGE_BACK
                && this.ticks % 2 == 0) {
            this.rays(level);
        }
        if (this.ticks >= PowerRing.RECHARGE_TICKS) {
            this.stop();
            return false;
        }
        return true;
    }

    private void hit(ServerLevel level) {
        Vec3 at = this.lanternPoint();
        Vec3 ahead = this.ahead();
        Vec3 front = at.add(ahead.scale(0.3));
        for (ServerPlayer viewer : level.players()) {
            if (viewer != this.owner && viewer.distanceToSqr(front) < 64.0 * 64.0) {
                level.sendParticles(viewer, ParticleFx.dust(PowerRing.BRIGHT, 2.0F), false, front.x, front.y, front.z,
                        8, 0.12, 0.12, 0.12, 0.0);
            }
        }
        this.cone(level, ParticleFx.dust(PowerRing.PALE, 1.6F), front, ahead, 50, 0.45, 0.42);
        this.cone(level, ParticleFx.dust(PowerRing.GREEN, 2.2F), front, ahead, 32, 0.65, 0.26);
        this.cone(level, ParticleFx.dust(PowerRing.PALE, 1.0F), front, ahead, 24, 0.4, 0.34);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.PALE, 1.2F), at, 18, 0.22);
        this.sound(level, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
        this.sound(level, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.7F);
        this.sound(level, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.5F);
        this.sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.9F);
        if (Flight.ticks(this.owner) >= 0) {
            ParticleFx.disc(level, ParticleFx.dust(PowerRing.PALE, 1.4F), front, ahead, 0.8, 22, 0.0);
            ParticleFx.disc(level, ParticleFx.dust(PowerRing.GREEN, 1.8F), at.subtract(ahead.scale(0.6)), ahead,
                    1.4, 30, 0.1);
            this.sound(level, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 0.7F);
        }
        PowerRing.setPower(this.owner, PowerRing.power(this.owner) + this.restore);
        PowerRing.tell(this.owner, "recharged");
        Flight.recharged(this.owner, level);
    }

    private void rays(ServerLevel level) {
        Vec3 ahead = this.ahead();
        Vec3 front = this.lanternPoint().add(ahead.scale(0.3));
        this.cone(level, ParticleFx.dust(PowerRing.GREEN, 1.4F), front, ahead, 5, 0.35, 0.32);
        this.cone(level, ParticleFx.dust(PowerRing.PALE, 1.2F), front, ahead, 7, 0.5, 0.24);
    }

    private void cone(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 way, int count, double spread,
            double speed) {
        RandomSource random = this.owner.getRandom();
        for (int i = 0; i < count; i++) {
            Vec3 out = way.add((random.nextDouble() * 2.0 - 1.0) * spread, (random.nextDouble() * 2.0 - 1.0) * spread,
                    (random.nextDouble() * 2.0 - 1.0) * spread);
            ParticleFx.fly(level, particle, from, out.lengthSqr() < 1.0E-6 ? way : out.normalize(), speed);
        }
    }

    private void stop() {
        ACTIVE.remove(this.owner.getUUID(), this);
        PowerRing.sync(this.owner);
    }

    private Vec3 lanternPoint() {
        Vec3 look = this.owner.getLookAngle();
        Vec3 flat = look.horizontalDistanceSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0)
                : new Vec3(look.x, 0.0, look.z).normalize();
        Vec3 left = new Vec3(flat.z, 0.0, -flat.x);
        return this.owner.getEyePosition().add(this.ahead().scale(0.85)).add(left.scale(0.2)).add(0.0, -0.35, 0.0)
                .add(Flight.velocity(this.owner));
    }

    private Vec3 ahead() {
        if (Flight.ticks(this.owner) >= 0) {
            return Flight.heading(this.owner);
        }
        Vec3 look = this.owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.owner.getX(), this.owner.getY() + 1.2, this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }
}
