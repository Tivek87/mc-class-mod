package nl.tivek.multiversepowers.spell;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;

final class WindGustSpell {
    private static final double RANGE = 8.0;
    private static final double WAVE_SPEED = 1.0;
    // Degrees, not radians: used with Math.toRadians below.
    private static final double HALF_ANGLE = 50.0;
    private static final double STRENGTH = 2.2;
    private static final double LIFT = 0.55;

    private static final int WIND = 0xE8F4F8;
    private static final int PALE = 0xB8D8E0;

    private WindGustSpell() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        Vec3 origin = player.getEyePosition().add(0, -0.3, 0);
        Vec3 look = player.getLookAngle();
        Vec3 feet = player.position();
        UUID casterId = player.getUUID();

        swirl(level, feet);
        ParticleFx.at(level, ParticleTypes.SWEEP_ATTACK, origin.add(look.scale(1.2)));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 1.0F, 0.9F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.8F, 0.7F);

        Set<UUID> hit = new HashSet<>();
        hit.add(casterId);
        Effects.start(level, (lvl, age) -> {
            double front = (age + 1) * WAVE_SPEED;
            drawWave(lvl, origin, look, front, age);
            push(lvl, origin, look, front, casterId, hit);
            if (age % 3 == 1) {
                lvl.playSound(null, origin.x + look.x * front, origin.y + look.y * front, origin.z + look.z * front,
                        SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS, 0.6F, 1.4F);
            }
            return front < RANGE;
        });
        return true;
    }

    private static void swirl(ServerLevel level, Vec3 feet) {
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < 24; i++) {
                double t = i / 24.0;
                double angle = t * Math.PI * 4 + strand * Math.PI;
                double radius = 1.1 - t * 0.4;
                Vec3 point = feet.add(Math.cos(angle) * radius, t * 2.2, Math.sin(angle) * radius);
                ParticleFx.at(level, ParticleFx.dust(WIND, 1.0F), point);
                if (i % 4 == 0) {
                    ParticleFx.fly(level, ParticleTypes.CLOUD, point, new Vec3(-Math.sin(angle), 0.3, Math.cos(angle)),
                            0.1);
                }
            }
        }
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, feet.add(0, 0.1, 0), 20, 0.25);
    }

    private static void drawWave(ServerLevel level, Vec3 origin, Vec3 look, double front, int age) {
        Vec3[] b = ParticleFx.basis(look);
        int rows = 3;
        int columns = 9 + (int) front;
        ParticleOptions wall = ParticleFx.fade(WIND, PALE, 1.3F);
        for (int c = 0; c <= columns; c++) {
            double yaw = Math.toRadians(Mth.lerp((double) c / columns, -HALF_ANGLE, HALF_ANGLE));
            for (int r = 0; r < rows; r++) {
                double pitch = Math.toRadians((r - (rows - 1) / 2.0) * 14.0);
                Vec3 direction = look.add(b[0].scale(Math.tan(yaw))).add(b[1].scale(Math.tan(pitch))).normalize();
                Vec3 point = origin.add(direction.scale(front));
                ParticleFx.at(level, wall, point);
                if ((c + r + age) % 4 == 0) {
                    ParticleFx.fly(level, ParticleTypes.CLOUD, point, direction, 0.25);
                }
            }
        }
        for (int i = 0; i < 5; i++) {
            double yaw = Math.toRadians(ParticleFx.spread(HALF_ANGLE));
            double pitch = Math.toRadians(ParticleFx.spread(18));
            Vec3 direction = look.add(b[0].scale(Math.tan(yaw))).add(b[1].scale(Math.tan(pitch))).normalize();
            Vec3 tail = origin.add(direction.scale(Math.max(0.5, front - 1.5)));
            ParticleFx.line(level, ParticleFx.dust(WIND, 0.5F), tail, origin.add(direction.scale(front)), 0.2);
        }
        if (age % 2 == 0) {
            ParticleFx.at(level, ParticleTypes.SMALL_GUST, origin.add(look.scale(front)));
        }
        if (age == 3 || age == 6) {
            ParticleFx.at(level, ParticleTypes.GUST, origin.add(look.scale(front)));
        }
    }

    private static void push(ServerLevel level, Vec3 origin, Vec3 look, double front, UUID casterId, Set<UUID> hit) {
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterId);
        double cone = Math.cos(Math.toRadians(HALF_ANGLE + 10));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(origin, origin).inflate(front + 1.0),
                entity -> entity.isAlive() && !entity.isSpectator())) {
            if (hit.contains(target.getUUID())
                    || target instanceof Player player && (caster == null || !Targeting.isTargetable(caster, player))) {
                continue;
            }
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
            double distance = toTarget.length();
            if (distance > front || distance > RANGE + 0.5 || distance < 1.0E-4
                    || toTarget.normalize().dot(look) < cone) {
                continue;
            }
            hit.add(target.getUUID());
            double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
            double strength = STRENGTH * (1.0 - distance / RANGE * 0.6) * (1.0 - resist);
            Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z);
            Vec3 direction = flat.lengthSqr() < 1.0E-4 ? new Vec3(look.x, 0, look.z).normalize() : flat.normalize();
            target.setDeltaMovement(target.getDeltaMovement()
                    .add(direction.x * strength, LIFT * (1.0 - resist), direction.z * strength));
            target.hasImpulse = true;
            // Players move themselves client-side; this flag tells them about the push.
            target.hurtMarked = true;
            Vec3 center = target.getBoundingBox().getCenter();
            ParticleFx.cloud(level, ParticleTypes.CLOUD, center, 10, 0.3, 0.12);
            ParticleFx.at(level, ParticleTypes.GUST_EMITTER_SMALL, center);
        }
    }
}
