package nl.tivek.multiversepowers.spell;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

final class ThunderClapSpell {
    private static final int MEET = ClapPayload.HANDS_MEET;
    // ClapFx draws the blast in the same cone and reach.
    private static final double RADIUS = 9.0;
    private static final double HALF_ANGLE = 0.8;
    private static final double CONE_BACK = 1.0;
    private static final double WAVE_SPEED = 2.25;
    private static final float DAMAGE = 5.0F;
    private static final double STRENGTH = 1.6;
    private static final double LIFT = 0.45;

    private static final int GLOW = 0x00D2FF;

    private ThunderClapSpell() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        ClapPayload.send(player);
        UUID casterId = player.getUUID();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 2.0F);
        Set<UUID> hit = new HashSet<>();
        hit.add(casterId);
        Vec3[] feet = { player.position() };
        Vec3[] ahead = { flatLook(player) };
        Effects.start(level, (lvl, age) -> {
            ServerPlayer caster = lvl.getServer().getPlayerList().getPlayer(casterId);
            boolean here = caster != null && caster.level() == lvl;
            if (here && age <= MEET) {
                feet[0] = caster.position();
                ahead[0] = flatLook(caster);
            }
            if (age < MEET) {
                if (here) {
                    gather(lvl, caster, age);
                }
                return true;
            }
            int t = age - MEET;
            if (t == 0) {
                boom(lvl, feet[0].add(0.0, (here ? caster.getEyeHeight() : 1.6) - 0.3, 0.0).add(ahead[0].scale(0.6)),
                        feet[0]);
            }
            double front = (t + 1) * WAVE_SPEED;
            if (caster != null) {
                push(lvl, caster, feet[0], ahead[0], Math.min(front, RADIUS), hit);
            }
            return front < RADIUS + WAVE_SPEED;
        });
        return true;
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private static Vec3 clapPoint(ServerPlayer player) {
        return player.getEyePosition().add(flatLook(player).scale(0.6)).add(0, -0.3, 0);
    }

    // Static builds in the spread hands while the arms are drawn back, stronger the closer the clap comes.
    private static void gather(ServerLevel level, ServerPlayer player, int age) {
        Vec3 forward = flatLook(player);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 clap = clapPoint(player);
        double snap = Math.max(0.0, (age - (MEET - 2)) / 2.0);
        double spread = Math.min(1.0, age / 5.0) * (1.0 - snap * snap);
        int sparks = 1 + age / 3;
        for (int side = -1; side <= 1; side += 2) {
            Vec3 hand = clap.add(forward.scale(-0.35 * spread)).add(right.scale(side * (0.06 + 0.8 * spread)));
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, hand, sparks, 0.1, 0.06);
            ParticleFx.at(level, ParticleFx.dust(GLOW, 0.4F + 0.05F * age), hand);
        }
        if (age == MEET - 3) {
            level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS,
                    0.7F, 1.6F);
        }
    }

    private static void boom(ServerLevel level, Vec3 clap, Vec3 feet) {
        SpellFxPayload.send(level, SpellFxPayload.CLAP, clap, feet, -1, 0);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 2.0F, 0.55F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F,
                1.0F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0F,
                0.8F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.5F,
                1.2F);
    }

    // Only what stands in the cone ahead is hit; the cone starts a step behind the caster so it covers his sides.
    private static void push(ServerLevel level, ServerPlayer caster, Vec3 feet, Vec3 ahead, double front,
            Set<UUID> hit) {
        Vec3 origin = feet.subtract(ahead.scale(CONE_BACK));
        double cone = Math.cos(HALF_ANGLE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(feet, feet).inflate(front + 1.0, 3.0, front + 1.0),
                entity -> SpellTargets.hits(caster, entity) && !hit.contains(entity.getUUID()))) {
            Vec3 away = new Vec3(target.getX() - origin.x, 0, target.getZ() - origin.z);
            double distance = Math.sqrt((target.getX() - feet.x) * (target.getX() - feet.x)
                    + (target.getZ() - feet.z) * (target.getZ() - feet.z));
            if (distance > front || away.lengthSqr() < 1.0E-6 || away.normalize().dot(ahead) < cone) {
                continue;
            }
            hit.add(target.getUUID());
            double near = 1.0 - 0.5 * distance / RADIUS;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(caster), (float) (DAMAGE * near));
            SpellTargets.push(target, away.normalize(), STRENGTH * (0.5 + 0.5 * near), LIFT);
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, target.getBoundingBox().getCenter(), 14, 0.35, 0.2);
        }
    }
}
