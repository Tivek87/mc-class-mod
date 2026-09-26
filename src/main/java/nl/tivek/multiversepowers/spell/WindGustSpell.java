package nl.tivek.multiversepowers.spell;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.faction.Factions;

final class WindGustSpell {
    private static final double RANGE = 8.0;
    private static final double WAVE_SPEED = 1.0;
    // Degrees, not radians: used with Math.toRadians below.
    private static final double HALF_ANGLE = 50.0;
    private static final double STRENGTH = 2.2;
    private static final double LIFT = 0.55;
    private static final int CUSHION = 20;

    private static final int WIND = 0xE8F4F8;

    private WindGustSpell() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        Vec3 origin = player.getEyePosition().add(0, -0.3, 0);
        Vec3 look = player.getLookAngle();
        Vec3 feet = player.position();
        UUID casterId = player.getUUID();

        swirl(level, feet);
        ParticleFx.at(level, ParticleTypes.SWEEP_ATTACK, origin.add(look.scale(1.2)));
        ParticleFx.at(level, ParticleTypes.GUST_EMITTER_SMALL, origin.add(look.scale(0.8)));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 1.0F, 0.9F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_SHOOT,
                SoundSource.PLAYERS, 0.8F, 1.1F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.8F, 0.7F);
        // The gust blows out the caster's own flames, and in the air it catches them like a breeze would.
        if (player.isOnFire()) {
            player.clearFire();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        if (!player.onGround() && player.getDeltaMovement().y < 0.0) {
            player.resetFallDistance();
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, CUSHION, 0, false, false, true));
            ParticleFx.cloud(level, ParticleTypes.CLOUD, feet, 10, 0.4, 0.05);
        }

        Set<UUID> hit = new HashSet<>();
        hit.add(casterId);
        Set<Integer> turned = new HashSet<>();
        SpellFxPayload.send(level, SpellFxPayload.GUST, origin, look, -1, 0);
        Effects.start(level, (lvl, age) -> {
            double front = (age + 1) * WAVE_SPEED;
            drawWave(lvl, origin, look, front, age);
            push(lvl, origin, look, front, casterId, hit);
            blowAway(lvl, origin, look, front, casterId, turned);
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
        for (int c = 0; c <= columns; c++) {
            double yaw = Math.toRadians(Mth.lerp((double) c / columns, -HALF_ANGLE, HALF_ANGLE));
            for (int r = 0; r < rows; r++) {
                double pitch = Math.toRadians((r - (rows - 1) / 2.0) * 14.0);
                Vec3 direction = look.add(b[0].scale(Math.tan(yaw))).add(b[1].scale(Math.tan(pitch))).normalize();
                Vec3 point = origin.add(direction.scale(front));
                if ((c + r + age) % 6 == 0) {
                    ParticleFx.fly(level, ParticleTypes.CLOUD, point, direction, 0.25);
                }
            }
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
                new AABB(origin, origin).inflate(front + 1.0), entity -> SpellTargets.hits(caster, entity))) {
            if (hit.contains(target.getUUID())) {
                continue;
            }
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(origin);
            double distance = toTarget.length();
            if (distance > front || distance > RANGE + 0.5 || distance < 1.0E-4
                    || toTarget.normalize().dot(look) < cone) {
                continue;
            }
            hit.add(target.getUUID());
            double strength = STRENGTH * (1.0 - distance / RANGE * 0.6);
            Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z);
            Vec3 direction = flat.lengthSqr() < 1.0E-4 ? new Vec3(look.x, 0, look.z).normalize() : flat.normalize();
            SpellTargets.push(target, direction, strength, LIFT);
            if (target.isOnFire()) {
                target.clearFire();
            }
            Vec3 center = target.getBoundingBox().getCenter();
            ParticleFx.cloud(level, ParticleTypes.CLOUD, center, 10, 0.3, 0.12);
            ParticleFx.at(level, ParticleTypes.GUST_EMITTER_SMALL, center);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.WIND_CHARGE_BURST.value(),
                    SoundSource.PLAYERS, 0.5F, 1.3F);
        }
    }

    // Loose things go with the wind: drops and orbs are swept along, and shots flying at the caster are turned back
    // as a breeze turns them, now the caster's own.
    private static void blowAway(ServerLevel level, Vec3 origin, Vec3 look, double front, UUID casterId,
            Set<Integer> turned) {
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterId);
        double cone = Math.cos(Math.toRadians(HALF_ANGLE + 10));
        for (Entity thing : level.getEntities((Entity) null, new AABB(origin, origin).inflate(front + 1.0),
                entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb
                        || entity instanceof Projectile)) {
            Vec3 to = thing.position().subtract(origin);
            double distance = to.length();
            if (distance > front || distance > RANGE + 0.5 || distance < 1.0E-4 || to.normalize().dot(look) < cone
                    || !turned.add(thing.getId())) {
                continue;
            }
            if (thing instanceof Projectile shot) {
                Entity shooter = shot.getOwner();
                if (caster == null || shooter == caster || shooter != null && !Factions.hostile(caster, shooter)
                        || shot.getDeltaMovement().lengthSqr() < 0.04) {
                    continue;
                }
                shot.setDeltaMovement(look.scale(Math.max(0.8, shot.getDeltaMovement().length() * 0.9)));
                shot.setOwner(caster);
                shot.hurtMarked = true;
                ParticleFx.cloud(level, ParticleTypes.CLOUD, shot.position(), 6, 0.15, 0.05);
                level.playSound(null, shot.getX(), shot.getY(), shot.getZ(), SoundEvents.BREEZE_DEFLECT,
                        SoundSource.PLAYERS, 0.8F, 1.0F);
            } else {
                thing.setDeltaMovement(thing.getDeltaMovement().add(look.x * 0.6, 0.25, look.z * 0.6));
                thing.hurtMarked = true;
            }
        }
    }
}
