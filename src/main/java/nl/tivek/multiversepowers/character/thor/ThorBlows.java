package nl.tivek.multiversepowers.character.thor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// Thor's blows on the server: his game picks each one and shows it at once; this lands it on its tick on what stands
// within its reach and width ahead of him, and shows it to everyone who sees him.
final class ThorBlows {
    // Blows sent closer together than their pace allows (a burst after lag) wait their turn instead of being dropped;
    // one that would wait longer than this is refused.
    private static final int SLACK = 2;
    private static final int MOST_WAIT = 10;
    private static final double RIGHT_THERE = 0.8;
    private static final int GLOW = 0x9FE8FF;
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private ThorBlows() {
    }

    static boolean start(ServerPlayer player, int index, float damage) {
        ThorBlow blow = ThorBlow.byIndex(index);
        if (blow == null || player.isPassenger() || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        long start = Math.max(now, NEXT.getOrDefault(player.getUUID(), now) - SLACK);
        if (start - now > MOST_WAIT) {
            return false;
        }
        NEXT.put(player.getUUID(), start + blow.ready());
        ThorStatePayload.send(player, ThorMoves.flags(player), ThorStatePayload.BLOW, index);
        UUID id = player.getUUID();
        int wait = (int) (start - now);
        Effects.start(level, (lvl, age) -> {
            ServerPlayer thor = lvl.getServer().getPlayerList().getPlayer(id);
            if (thor == null || thor.level() != lvl || !thor.isAlive()) {
                return false;
            }
            int t = age - wait;
            if (t == 0) {
                whoosh(lvl, thor, blow);
            }
            if (blow == ThorBlow.THUNDER_PUNCH && t > 0 && t < blow.hit()) {
                ParticleFx.cloud(lvl, ParticleTypes.ELECTRIC_SPARK, fist(thor, 0.3), 3, 0.15, 0.1);
            }
            if (t < blow.hit()) {
                return true;
            }
            land(lvl, thor, blow, damage);
            return false;
        });
        return true;
    }

    static void forget(ServerPlayer player) {
        NEXT.remove(player.getUUID());
    }

    static void clear() {
        NEXT.clear();
    }

    // The swing is heard by everyone near but him: his own game already plays it.
    private static void whoosh(ServerLevel level, ServerPlayer thor, ThorBlow blow) {
        boolean heavy = blow.kick() || blow.finisher();
        SoundEvent swing = heavy ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_NODAMAGE;
        level.playSound(thor, thor.getX(), thor.getEyeY(), thor.getZ(), swing, SoundSource.PLAYERS,
                heavy ? 0.45F : 0.55F, heavy ? 0.8F : 1.35F + 0.2F * level.random.nextFloat());
    }

    // Where his striking hand (or foot) is when it lands: ahead of him at the blow's height.
    private static Vec3 fist(ServerPlayer thor, double ahead) {
        Vec3 look = thor.getLookAngle();
        return thor.getEyePosition().add(look.scale(0.6 + ahead)).add(0.0, -0.35, 0.0);
    }

    private static Vec3 flat(Vec3 way) {
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static void land(ServerLevel level, ServerPlayer thor, ThorBlow blow, float damage) {
        Vec3 eye = thor.getEyePosition();
        Vec3 look = thor.getLookAngle();
        Vec3 ahead = flat(look);
        Vec3 aimEnd = eye.add(look.scale(blow.reach()));
        double wide = Math.cos(Math.toRadians(blow.arc()));
        List<LivingEntity> struck = new ArrayList<>();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                thor.getBoundingBox().inflate(blow.reach() + 1.0), entity -> Targeting.mayStrike(thor, entity))) {
            AABB box = target.getBoundingBox();
            Vec3 near = new Vec3(Mth.clamp(eye.x, box.minX, box.maxX), Mth.clamp(eye.y, box.minY, box.maxY),
                    Mth.clamp(eye.z, box.minZ, box.maxZ));
            double far = near.distanceTo(eye);
            if (far > blow.reach()) {
                continue;
            }
            // Aimed straight at, it is hit whatever its height; else it must stand in the blow's width and band.
            boolean aimed = box.inflate(0.3).clip(eye, aimEnd).isPresent();
            double facing = far < RIGHT_THERE ? 1.0 : flat(box.getCenter().subtract(eye)).dot(ahead);
            double feet = thor.getY();
            boolean inBand = box.maxY >= feet + blow.height().from() && box.minY <= feet + blow.height().to();
            if (!aimed && (facing < wide || !inBand)) {
                continue;
            }
            if (LoadedWorld.clip(level, new ClipContext(eye, near, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    thor)).getType() != HitResult.Type.MISS) {
                continue;
            }
            if (blow.sweep()) {
                struck.add(target);
                continue;
            }
            double score = far + 3.0 * (1.0 - facing) - (aimed ? 1.0 : 0.0);
            if (score < bestScore) {
                bestScore = score;
                best = target;
            }
        }
        if (best != null) {
            struck.add(best);
        }
        for (LivingEntity target : struck) {
            hit(level, thor, blow, target, damage);
        }
        if (blow == ThorBlow.DOUBLE_HAMMER || blow == ThorBlow.THUNDER_PUNCH) {
            boom(level, thor, blow, !struck.isEmpty());
        }
    }

    // A hit pushes what it hits as the blow says, in place of the game's own knockback: jabs keep it close for the
    // next blow, the finishers throw it.
    private static void hit(ServerLevel level, ServerPlayer thor, ThorBlow blow, LivingEntity target, float damage) {
        target.invulnerableTime = 0;
        if (!target.hurt(level.damageSources().playerAttack(thor), (float) (damage * blow.power()))) {
            return;
        }
        Vec3 away = target.position().subtract(thor.position());
        Vec3 way = away.horizontalDistanceSqr() < 1.0E-4 ? flat(thor.getLookAngle()) : flat(away);
        double keep = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        Vec3 was = target.getDeltaMovement();
        double up = blow.lift() >= 0.0 ? blow.lift() * keep : Math.min(was.y, blow.lift());
        target.setDeltaMovement(way.x * blow.push() * keep, up, way.z * blow.push() * keep);
        target.hasImpulse = true;
        target.hurtMarked = true;
        Vec3 at = target.getBoundingBox().getCenter().lerp(thor.getEyePosition(), 0.3);
        boolean heavy = blow.finisher() || blow.kick();
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, heavy ? 14 : 6, 0.2, 0.15);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, heavy ? 10 : 4, 0.2, 0.2);
        SoundEvent sound = blow.push() >= 0.9 ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_STRONG;
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 1.0F,
                0.85F + 0.25F * level.random.nextFloat());
        if (heavy) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 0.8F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.3F,
                    1.8F);
        }
    }

    // The two blows that end in thunder: a charged fist crackling out, both fists hammering the ground.
    private static void boom(ServerLevel level, ServerPlayer thor, ThorBlow blow, boolean landed) {
        if (blow == ThorBlow.THUNDER_PUNCH) {
            Vec3 at = fist(thor, 0.8);
            ParticleFx.sphereOut(level, ParticleFx.dust(GLOW, 1.2F), at, 18, 0.3);
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 24, 0.35, 0.3);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS,
                    landed ? 0.9F : 0.5F, 1.5F);
            return;
        }
        Vec3 ground = thor.position().add(flat(thor.getLookAngle()).scale(1.4)).add(0.0, 0.1, 0.0);
        ParticleFx.shockwave(level, ParticleFx.dust(GLOW, 1.1F), ground, 20, 0.3);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, ground.add(0.0, 0.3, 0.0), 16, 0.5, 0.15);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.9F,
                0.9F);
    }
}
