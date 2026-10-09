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

// The mech's eyes landing: the ray strikes the first creature in its way and throws it back limp; the held beam burns
// everything along it again and again and drives it back, stopped only by blocks. Neither breaks a block.
final class MechBeams {
    private static final double EYE_PUSH = 1.6;
    private static final double EYE_LIFT = 0.35;
    private static final int GLARE_EVERY = 4;
    // Driven back gently, what it burns stays in it.
    private static final double GLARE_PUSH = 0.25;
    private static final double GLARE_LIFT = 0.05;

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

    // The held beam, `firing` ticks after it burst out of the eyes.
    static void glare(ServerLevel level, ServerPlayer owner, MechScript.Stage torso, CharacterAbility ability,
            int firing) {
        Vec3 from = MechBeam.visor(torso);
        Vec3 end = MechBeam.reach(level, from, MechBeam.aim(level, owner, MechBeam.GLARE_RANGE), MechBeam.GLARE_RANGE,
                owner);
        if (firing == 0) {
            Sounds.play(level, from, SoundEvents.BEACON_ACTIVATE, 3.0F, 1.6F);
            Sounds.play(level, from, SoundEvents.FIREWORK_ROCKET_BLAST, 2.0F, 0.5F);
        }
        if (firing % 8 == 0) {
            Sounds.play(level, from, SoundEvents.BEACON_AMBIENT, 3.0F, 1.5F);
            Sounds.play(level, end, SoundEvents.FIRE_EXTINGUISH, 1.5F, 0.8F);
        }
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), end, 3, 0.4, 0.2);
        if (firing % GLARE_EVERY != 0) {
            return;
        }
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, end, 8, 0.5, 0.4);
        Vec3 way = end.subtract(from).normalize();
        double[] a0 = { from.x, from.y, from.z };
        double[] a1 = { end.x, end.y, end.z };
        double[] out = new double[2];
        double damage = ability.value("mechGlareDamage");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end)
                .inflate(MechBeam.GLARE_RADIUS + 1.0), entity -> PowerRing.canHit(owner, entity))) {
            Vec3 middle = living.getBoundingBox().getCenter();
            double[] b = { middle.x, middle.y, middle.z };
            double reach = MechBeam.GLARE_RADIUS + living.getBbWidth() * 0.5;
            if (Segments.closest(a0, a1, b, b, out) > reach * reach) {
                continue;
            }
            MechAttack.hit(level, owner, living, damage);
            Knockdowns.knock(living);
            MechAttack.push(living, way.scale(GLARE_PUSH).add(0.0, GLARE_LIFT, 0.0));
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), middle, 8, 0.4, 0.15);
        }
    }
}
