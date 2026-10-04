package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBeam;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Segments;

// The mech's beams landing: the eye beam strikes the first creature in its way and throws it back limp; the Unibeam
// burns everything along it again and again and drives it back, stopped only by blocks. Neither breaks a block.
final class MechBeams {
    private static final double EYE_PUSH = 1.6;
    private static final double EYE_LIFT = 0.35;
    private static final int UNIBEAM_EVERY = 5;
    // Driven back gently, what it burns stays in it.
    private static final double UNIBEAM_PUSH = 0.25;
    private static final double UNIBEAM_LIFT = 0.05;

    private MechBeams() {
    }

    static void eye(ServerLevel level, ServerPlayer owner, MechScript.Stage torso, CharacterAbility ability) {
        Vec3 from = MechBeam.visor(torso);
        Vec3 end = MechBeam.reach(level, from, MechBeam.aim(level, owner, MechBeam.EYE_RANGE), MechBeam.EYE_RANGE,
                owner);
        LivingEntity first = MechBeam.first(level, owner, from, end);
        Vec3 way = end.subtract(from).normalize();
        if (first != null && PowerRing.canHit(owner, first)) {
            end = first.getBoundingBox().getCenter();
            MechAttack.hit(level, owner, first, ability.value("mechEyeDamage"));
            Knockdowns.knock(first);
            MechAttack.push(first, way.scale(EYE_PUSH).add(0.0, EYE_LIFT, 0.0));
        }
        Sounds.play(level, from, SoundEvents.BEACON_POWER_SELECT, 2.5F, 1.8F);
        Sounds.play(level, from, SoundEvents.FIREWORK_ROCKET_BLAST, 2.0F, 0.6F);
        Sounds.play(level, end, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.6F);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), end, 18, 0.5, 0.25);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, end, 14, 0.4, 0.4);
    }

    // `firing`: ticks since it burst out.
    static void unibeam(ServerLevel level, ServerPlayer owner, MechScript.Stage torso, CharacterAbility ability,
            int firing) {
        Vec3 from = MechBeam.port(torso);
        Vec3 end = MechBeam.reach(level, from, MechBeam.aim(level, owner, MechBeam.UNIBEAM_RANGE),
                MechBeam.UNIBEAM_RANGE, owner);
        if (firing == 0) {
            Sounds.play(level, from, SoundEvents.BEACON_ACTIVATE, 3.0F, 0.6F);
            Sounds.play(level, from, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.7F);
            ParticleFx.disc(level, ParticleFx.dust(PowerRing.BRIGHT, 2.0F), from, end.subtract(from), 2.2, 28, 0.0);
        }
        if (firing % 8 == 0) {
            Sounds.play(level, from, SoundEvents.BEACON_AMBIENT, 4.0F, 0.5F);
            Sounds.play(level, end, SoundEvents.FIRE_EXTINGUISH, 1.5F, 0.6F);
        }
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 2.0F), end, 4, 0.7, 0.3);
        if (firing % UNIBEAM_EVERY != 0) {
            return;
        }
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, end, 10, 0.8, 0.5);
        Vec3 way = end.subtract(from).normalize();
        double[] a0 = { from.x, from.y, from.z };
        double[] a1 = { end.x, end.y, end.z };
        double[] out = new double[2];
        double damage = ability.value("mechUnibeamDamage");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end)
                .inflate(MechBeam.UNIBEAM_RADIUS + 1.0), entity -> PowerRing.canHit(owner, entity))) {
            Vec3 middle = living.getBoundingBox().getCenter();
            double[] b = { middle.x, middle.y, middle.z };
            double reach = MechBeam.UNIBEAM_RADIUS + living.getBbWidth() * 0.5;
            if (Segments.closest(a0, a1, b, b, out) > reach * reach) {
                continue;
            }
            MechAttack.hit(level, owner, living, damage);
            Knockdowns.knock(living);
            MechAttack.push(living, way.scale(UNIBEAM_PUSH).add(0.0, UNIBEAM_LIFT, 0.0));
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), middle, 8, 0.4, 0.15);
        }
    }
}
