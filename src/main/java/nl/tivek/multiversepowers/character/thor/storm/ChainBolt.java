package nl.tivek.multiversepowers.character.thor.storm;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.entity.DeathStyles;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.faction.Factions;

// A bolt of Thor's onto one creature, in the air as much as on the ground: it hurts only what it strikes and knocks it
// down, then leaps on to the nearest foe and from there to the next, up to LEAPS of them, each at most LEAP blocks from
// the one before. Every game near draws the bolt and each leap (StormFxPayload).
public final class ChainBolt {
    public static final int LEAPS = 3;
    // The most room between two creatures a leap crosses, in blocks, past half a body's width each side.
    public static final double LEAP = 2.0;
    private static final double BODY = 0.6;
    private static final float LEAP_SHARE = 0.75F;
    private static final int SHOCKED = 30;

    private ChainBolt() {
    }

    // From `top` onto `target`.
    public static void onto(ServerLevel level, ServerPlayer player, Vec3 top, LivingEntity target, float damage,
            float size) {
        Vec3 at = target.getBoundingBox().getCenter();
        StormFxPayload.send(level, StormFxPayload.BOLT, top, at, size);
        List<LivingEntity> struck = new ArrayList<>();
        struck.add(target);
        if (damage > 0.0F) {
            hurt(level, player, target, damage);
            Knockdowns.knock(target);
            leap(level, player, at, struck, damage * LEAP_SHARE);
        }
    }

    // From `top` down to `ground`: onto the first creature in its way; none there, into the ground, from where it
    // leaps to a foe that stands within LEAP.
    public static void down(ServerLevel level, ServerPlayer player, Vec3 top, Vec3 ground, float damage, float size) {
        LivingEntity first = damage > 0.0F ? firstInWay(level, player, top, ground) : null;
        if (first != null) {
            onto(level, player, top, first, damage, size);
            return;
        }
        StormFxPayload.send(level, StormFxPayload.BOLT, top, ground, size);
        if (damage > 0.0F) {
            leap(level, player, ground.add(0.0, 0.5, 0.0), new ArrayList<>(), damage * LEAP_SHARE);
        }
    }

    // What a bolt from `top` to `ground` meets first on its way down.
    @Nullable
    private static LivingEntity firstInWay(ServerLevel level, ServerPlayer player, Vec3 top, Vec3 ground) {
        LivingEntity first = null;
        double highest = Double.NEGATIVE_INFINITY;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(top, ground).inflate(1.0),
                entity -> Targeting.mayStrike(player, entity))) {
            AABB box = entity.getBoundingBox().inflate(0.3);
            if (box.maxY > highest && box.clip(top, ground).isPresent()) {
                highest = box.maxY;
                first = entity;
            }
        }
        return first;
    }

    private static void leap(ServerLevel level, ServerPlayer player, Vec3 from, List<LivingEntity> struck,
            float damage) {
        Vec3 at = from;
        for (int k = 0; k < LEAPS; k++) {
            LivingEntity next = Targeting.nextInChain(level, player, at, LEAP + BODY, struck,
                    entity -> Factions.hostile(player, entity) && Targeting.mayStrike(player, entity));
            if (next == null) {
                return;
            }
            struck.add(next);
            Vec3 to = next.getBoundingBox().getCenter();
            StormFxPayload.send(level, StormFxPayload.SPARK, at, to, 0.6F);
            hurt(level, player, next, damage);
            next.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHOCKED, 2), player);
            at = to;
        }
    }

    // A bolt burns: what it kills is left as ash. It throws nothing away from him, as his blows would.
    private static void hurt(ServerLevel level, ServerPlayer player, LivingEntity target, float damage) {
        Vec3 before = target.getDeltaMovement();
        DeathStyles.mark(target, DeathStyles.Style.ASH);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().source(DamageTypes.LIGHTNING_BOLT, player), damage);
        target.setDeltaMovement(before);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, target.getBoundingBox().getCenter(), 16, 0.35, 0.25);
    }
}
