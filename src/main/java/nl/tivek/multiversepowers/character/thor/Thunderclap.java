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
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.spell.ClapPayload;
import nl.tivek.multiversepowers.spell.SpellFxPayload;
import nl.tivek.multiversepowers.spell.SpellTargets;

// Thor claps his hands with a crack of thunder: static builds between them as they draw apart, and when they meet a
// shockwave rolls out the way he looks, hurting and throwing what wants to hurt him. ClapFx draws it.
final class Thunderclap {
    private static final int MEET = ClapPayload.HANDS_MEET;
    private static final double CONE_BACK = 1.0;
    private static final double OFF_WALL = 0.3;

    private static final int GLOW = 0x00D2FF;

    private Thunderclap() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level, float damage) {
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
                boom(lvl, casterEntity, hands(eye[0], ahead[0]), aim[0], feet[0]);
            }
            double front = (t + 1) * setting("waveSpeed");
            if (caster != null && front < setting("radiusBlocks") + setting("waveSpeed")) {
                push(lvl, caster, eye[0], ahead[0], Math.min(front, setting("radiusBlocks")), hit, damage);
            }
            return front < setting("radiusBlocks") + setting("waveSpeed");
        });
        return true;
    }

    // Where the hands meet: a little ahead of the eyes, the way he looks.
    private static Vec3 hands(Vec3 eye, Vec3 look) {
        return eye.add(look.scale(0.6)).add(0.0, -0.3, 0.0);
    }

    // What the crosshair points at within reach: the middle of a creature, else a block, else the end of the reach.
    private static Vec3 aimed(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(setting("radiusBlocks")));
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

    private static Vec3 side(Vec3 look) {
        Vec3 right = look.cross(Vectors.UP);
        return right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
    }

    // Static builds in the spread hands while the arms are drawn back, stronger the closer the clap comes.
    private static void gather(ServerLevel level, ServerPlayer player, int age) {
        Vec3 forward = player.getLookAngle();
        Vec3 right = side(forward);
        Vec3 clap = hands(player.getEyePosition(), forward);
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

    // The hands' height over the feet rides along in hundredths of a block, so ClapFx finds the ground; the caster
    // rides along so the caster's own screen can start it at the first-person hands.
    private static void boom(ServerLevel level, int caster, Vec3 clap, Vec3 aim, Vec3 feet) {
        int drop = (int) Math.round(Math.max(0.0, clap.y - feet.y) * 100.0);
        SpellFxPayload.send(level, SpellFxPayload.CLAP, clap, aim, caster, drop);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 2.0F, 0.55F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0F,
                1.0F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0F,
                0.8F);
        level.playSound(null, clap.x, clap.y, clap.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.5F,
                1.2F);
    }

    // Only what stands in the cone the caster aims is hit; the cone starts a step behind him so it covers his sides.
    private static void push(ServerLevel level, ServerPlayer caster, Vec3 eye, Vec3 ahead, double front,
            Set<UUID> hit, float damage) {
        Vec3 origin = eye.subtract(ahead.scale(CONE_BACK));
        double cone = Math.cos(Math.toRadians(setting("halfAngleDegrees")));
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
            double close = 1.0 - 0.5 * distance / setting("radiusBlocks");
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(caster), (float) (damage * close));
            SpellTargets.push(target, way, setting("push") * (0.5 + 0.5 * close),
                    setting("lift") + Math.max(0.0, way.y) * setting("push") * 0.5);
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

    // ClapFx draws the blast in the same cone and reach.
    private static double setting(String key) {
        return GameCharacter.THOR.byName("thunderclap").value(key);
    }
}
