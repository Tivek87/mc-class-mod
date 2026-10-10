package nl.tivek.multiversepowers.character.greenlantern.ability.ring;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.flight.Flight;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightDome;
import nl.tivek.multiversepowers.character.greenlantern.ability.flight.RamCone;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The ring held to the lantern's emblem: from the touch it fills little by little while the lantern hums and lights
// up, until it is full.
public final class Recharge implements Effect {
    private static final Map<UUID, Recharge> ACTIVE = new HashMap<>();
    // Ticks (at the recharge's own pace) between the lantern's hums.
    private static final double HUM = 12.0;

    private final ServerPlayer owner;
    private final float restore;
    private final int made;
    private final int played;
    private int ticks;

    private Recharge(ServerPlayer owner, float restore, int made, int played) {
        this.owner = owner;
        this.restore = restore;
        this.made = made;
        this.played = played;
    }

    public static boolean recharge(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (busy(owner)) {
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
        RamCone.stop(owner);
        LightDome.lower(owner);
        Recharge lantern = new Recharge(owner, (float) ability.value("powerRestored"), 1, 1);
        ACTIVE.put(owner.getUUID(), lantern);
        Effects.start(level, lantern);
        lantern.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.8F, 0.8F);
        PowerRing.sync(owner);
        return true;
    }

    public static void arrive(ServerPlayer owner, ServerLevel level) {
        CharacterAbility ability = GameCharacter.GREEN_LANTERN.byName("recharge");
        if (busy(owner) || ability == null) {
            return;
        }
        // Part of the arrival, so it plays at the arrival's slower pace.
        Recharge lantern = new Recharge(owner, (float) ability.value("powerRestored"), Arrival.TICKS,
                Arrival.PLAYED_TICKS);
        ACTIVE.put(owner.getUUID(), lantern);
        Effects.start(level, lantern);
        lantern.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.8F, 0.8F);
        PowerRing.sync(owner);
    }

    public static boolean busy(ServerPlayer player) {
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
        double moment = (double) this.ticks * this.made / this.played;
        double before = (double) (this.ticks - 1) * this.made / this.played;
        if (before < PowerRing.RECHARGE_HIT && moment >= PowerRing.RECHARGE_HIT) {
            this.touch(level);
        }
        double filled = filled(moment) - filled(before);
        if (filled > 0.0) {
            PowerRing.setPower(this.owner, PowerRing.power(this.owner) + (float) (this.restore * filled));
        }
        if (moment >= PowerRing.RECHARGE_HIT && moment < PowerRing.RECHARGE_BACK
                && (int) (moment / HUM) != (int) (before / HUM)) {
            this.sound(level, SoundEvents.BEACON_AMBIENT, 1.4F, 1.3F);
        }
        if (before < PowerRing.RECHARGE_BACK && moment >= PowerRing.RECHARGE_BACK) {
            this.full(level);
        }
        if (moment >= PowerRing.RECHARGE_TICKS) {
            this.stop();
            return false;
        }
        return true;
    }

    // How much of the charge has gone into the ring by this moment, 0 to 1.
    private static double filled(double moment) {
        return Mth.clamp((moment - PowerRing.RECHARGE_HIT) / (PowerRing.RECHARGE_BACK - PowerRing.RECHARGE_HIT), 0.0,
                1.0);
    }

    // The ring touches the emblem: the lantern starts to hum.
    private void touch(ServerLevel level) {
        this.sound(level, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.2F);
        this.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.8F);
    }

    // Full: a soft flash of green in the lantern and the ring ready.
    private void full(ServerLevel level) {
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.PALE, 1.0F), this.lanternPoint(), 12, 0.1);
        this.sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 1.2F);
        this.sound(level, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.4F);
        PowerRing.tell(this.owner, "recharged");
        Flight.recharged(this.owner, level);
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
