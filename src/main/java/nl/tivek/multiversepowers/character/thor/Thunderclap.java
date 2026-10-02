package nl.tivek.multiversepowers.character.thor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.spell.ClapPayload;
import nl.tivek.multiversepowers.spell.SpellFxPayload;
import nl.tivek.multiversepowers.spell.SpellTargets;

// Thor winds up while the button is held, leaning back with his arms flung wide (his game shows it, and ThorMoves tells
// the others), then slams his hands together with a crack of thunder: a shockwave full of lightning sparks rolls out
// the way he looks, hurting and throwing what stands in it, and thunder rolls on in the distance after it. ClapFx
// draws it.
final class Thunderclap {
    private static final int MEET = ClapPayload.HANDS_MEET;
    // Ticks after the clap when its thunder rolls in from afar.
    private static final int DISTANT = 9;
    // ClapFx draws the blast in the same cone, reach and pace.
    private static final double RADIUS = 9.0;
    private static final double HALF_ANGLE = 0.8;
    private static final double CONE_BACK = 1.0;
    private static final double WAVE_SPEED = 2.25;
    private static final double STRENGTH = 1.6;
    private static final double LIFT = 0.45;
    private static final double OFF_WALL = 0.3;

    private Thunderclap() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level, float damage) {
        ThorMoves.charging(player, false);
        ClapPayload.send(player);
        UUID casterId = player.getUUID();
        int casterEntity = player.getId();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 2.0F);
        Set<UUID> hit = new HashSet<>();
        hit.add(casterId);
        Vec3[] feet = { player.position() };
        Vec3[] eye = { player.getEyePosition() };
        Vec3[] ahead = { player.getLookAngle() };
        Vec3[] aim = { aimed(level, player) };
        double size = player.getScale();
        Effects.start(level, (lvl, age) -> {
            ServerPlayer caster = lvl.getServer().getPlayerList().getPlayer(casterId);
            boolean here = caster != null && caster.level() == lvl;
            if (here && age <= MEET) {
                feet[0] = caster.position();
                eye[0] = caster.getEyePosition();
                ahead[0] = caster.getLookAngle();
                aim[0] = aimed(lvl, caster);
            }
            if (age < MEET) {
                if (here) {
                    gather(lvl, caster, age);
                }
                return true;
            }
            int t = age - MEET;
            if (t == 0) {
                boom(lvl, casterEntity, hands(eye[0], ahead[0], size), aim[0], feet[0]);
            }
            if (t == DISTANT) {
                Vec3 far = feet[0];
                lvl.playSound(null, far.x, far.y, far.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F,
                        0.6F);
            }
            double front = (t + 1) * WAVE_SPEED;
            if (caster != null && front < RADIUS + WAVE_SPEED) {
                push(lvl, caster, eye[0], ahead[0], Math.min(front, RADIUS), hit, damage);
            }
            return t < DISTANT || front < RADIUS + WAVE_SPEED;
        });
        return true;
    }

    // Where the hands meet: a little ahead of the eyes, the way he looks, further for a bigger body.
    private static Vec3 hands(Vec3 eye, Vec3 look, double size) {
        return eye.add(look.scale(0.6 * size)).add(0.0, -0.3 * size, 0.0);
    }

    // What the crosshair points at within reach: the middle of a creature, else a block, else the end of the reach.
    private static Vec3 aimed(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RADIUS));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, stop,
                new AABB(eye, stop).inflate(1.0),
                target -> target instanceof LivingEntity && target.isPickable() && !target.isSpectator(),
                eye.distanceToSqr(stop));
        if (entity != null) {
            return entity.getEntity().getBoundingBox().getCenter();
        }
        return block.getType() == HitResult.Type.MISS ? stop
                : stop.subtract(look.scale(Math.min(OFF_WALL, eye.distanceTo(stop) * 0.5)));
    }

    // A rush of air as the hands slam together from wide apart.
    private static void gather(ServerLevel level, ServerPlayer player, int age) {
        Vec3 clap = hands(player.getEyePosition(), player.getLookAngle(), player.getScale());
        if (age == 0) {
            level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS,
                    0.7F, 1.6F);
        }
    }

    // The hands' height over the feet rides along in hundredths of a block, so ClapFx finds the ground; the caster
    // rides along so the caster's own screen can start it at the first-person hands.
    private static void boom(ServerLevel level, int caster, Vec3 clap, Vec3 aim, Vec3 feet) {
        int drop = (int) Math.round(Math.max(0.0, clap.y - feet.y) * 100.0);
        SpellFxPayload.send(level, SpellFxPayload.CLAP, clap, aim, caster, drop);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 2.0F, 0.55F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F,
                0.8F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0F,
                1.1F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.5F,
                1.2F);
    }

    // Only what stands in the cone the caster aims is hit; the cone starts a step behind him so it covers his sides.
    private static void push(ServerLevel level, ServerPlayer caster, Vec3 eye, Vec3 ahead, double front,
            Set<UUID> hit, float damage) {
        Vec3 origin = eye.subtract(ahead.scale(CONE_BACK));
        double cone = Math.cos(HALF_ANGLE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, eye).inflate(front + 2.0),
                entity -> SpellTargets.hits(caster, entity) && !hit.contains(entity.getUUID()))) {
            Vec3 near = nearest(target.getBoundingBox(), origin, ahead);
            Vec3 away = near.subtract(origin);
            double distance = near.distanceTo(eye);
            if (distance > front || away.lengthSqr() < 1.0E-6 || away.normalize().dot(ahead) < cone) {
                continue;
            }
            hit.add(target.getUUID());
            Vec3 way = away.normalize();
            double close = 1.0 - 0.5 * distance / RADIUS;
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(caster), (float) (damage * close));
            SpellTargets.push(target, way, STRENGTH * (0.5 + 0.5 * close),
                    LIFT + Math.max(0.0, way.y) * STRENGTH * 0.5);
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, target.getBoundingBox().getCenter(), 14, 0.35, 0.2);
        }
    }

    // The point of a box closest to the cone's middle line.
    private static Vec3 nearest(AABB box, Vec3 origin, Vec3 ahead) {
        Vec3 middle = box.getCenter();
        Vec3 onLine = origin.add(ahead.scale(Math.max(0.0, middle.subtract(origin).dot(ahead))));
        return new Vec3(Mth.clamp(onLine.x, box.minX, box.maxX), Mth.clamp(onLine.y, box.minY, box.maxY),
                Mth.clamp(onLine.z, box.minZ, box.maxZ));
    }
}
