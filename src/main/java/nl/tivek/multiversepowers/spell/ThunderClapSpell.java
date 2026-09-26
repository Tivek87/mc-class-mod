package nl.tivek.multiversepowers.spell;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

final class ThunderClapSpell {
    private static final int MEET = ClapPayload.HANDS_MEET;
    // StormFx.clap draws the ring at the same size and pace.
    private static final double RADIUS = 9.0;
    private static final double WAVE_SPEED = 1.1;
    private static final float DAMAGE = 5.0F;
    private static final double STRENGTH = 1.6;
    private static final double LIFT = 0.45;

    private static final int GLOW = 0x00D2FF;
    private static final int WHITE = 0xF4FBFF;

    private ThunderClapSpell() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        ClapPayload.send(player);
        UUID casterId = player.getUUID();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 2.0F);
        Set<UUID> hit = new HashSet<>();
        hit.add(casterId);
        Vec3[] center = { player.position() };
        Effects.start(level, (lvl, age) -> {
            ServerPlayer caster = lvl.getServer().getPlayerList().getPlayer(casterId);
            if (age < MEET) {
                if (caster != null && caster.level() == lvl) {
                    center[0] = caster.position();
                    gather(lvl, caster, age);
                }
                return true;
            }
            int t = age - MEET;
            if (t == 0) {
                boom(lvl, caster != null && caster.level() == lvl ? clapPoint(caster) : center[0].add(0, 1.2, 0),
                        center[0]);
            }
            double front = (t + 1) * WAVE_SPEED;
            if (caster != null) {
                push(lvl, caster, center[0], Math.min(front, RADIUS), hit);
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

    private static void gather(ServerLevel level, ServerPlayer player, int age) {
        Vec3 forward = flatLook(player);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 clap = clapPoint(player);
        double apart = 0.1 + 0.35 * Math.sin(Math.PI * age / MEET);
        for (int side = -1; side <= 1; side += 2) {
            Vec3 hand = clap.add(right.scale(side * apart));
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, hand, 3, 0.08, 0.05);
            ParticleFx.at(level, ParticleFx.dust(GLOW, 0.5F), hand);
        }
        if (age % 2 == 0) {
            ParticleFx.zigzag(level, ParticleFx.dust(WHITE, 0.4F), clap.add(right.scale(-apart)),
                    clap.add(right.scale(apart)), 3, 0.08, 0.05);
        }
    }

    private static void boom(ServerLevel level, Vec3 clap, Vec3 feet) {
        SpellFxPayload.send(level, SpellFxPayload.CLAP, clap, feet, -1, 0);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F,
                1.0F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0F,
                0.8F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.5F,
                1.2F);
    }

    private static void push(ServerLevel level, ServerPlayer caster, Vec3 center, double front, Set<UUID> hit) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(front + 1.0, 3.0, front + 1.0),
                entity -> SpellTargets.hits(caster, entity) && !hit.contains(entity.getUUID()))) {
            Vec3 away = new Vec3(target.getX() - center.x, 0, target.getZ() - center.z);
            double distance = away.length();
            if (distance > front) {
                continue;
            }
            hit.add(target.getUUID());
            double near = 1.0 - 0.5 * distance / RADIUS;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), (float) (DAMAGE * near));
            Vec3 way = distance < 1.0E-3 ? flatLook(caster) : away.scale(1.0 / distance);
            SpellTargets.push(target, way, STRENGTH * (0.5 + 0.5 * near), LIFT);
            Vec3 body = target.getBoundingBox().getCenter();
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, body, 14, 0.35, 0.2);
            ParticleFx.zigzag(level, ParticleFx.dust(WHITE, 0.5F), body.add(0, 0.6, 0), body.add(0, -0.6, 0), 3,
                    0.2, 0.08);
        }
    }
}
