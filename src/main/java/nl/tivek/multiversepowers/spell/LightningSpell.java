package nl.tivek.multiversepowers.spell;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;

final class LightningSpell {
    private static final double RANGE = 40.0;
    private static final int CHARGE = 14;
    private static final int DURATION = CHARGE + 30;
    private static final double RUNE_RADIUS = 2.2;
    private static final double CLOUD_HEIGHT = 18.0;
    private static final double SHOCK_RADIUS = 3.0;
    private static final int SHOCKED = 30;
    private static final double CHAIN_REACH = 6.0;
    private static final float CHAIN_DAMAGE = 4.0F;
    private static final int CHAIN = 3;
    // Rain carries the charge further.
    private static final int STORM_CHAIN = 5;
    private static final int CHAIN_EVERY = 2;

    private static final int GLOW = 0x00D2FF;

    private LightningSpell() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        Targeting.Aim aim = Targeting.aim(player, level, RANGE);
        Vec3 hand = Targeting.handPoint(player);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, hand, 16, 0.2, 0.25);
        ParticleFx.zigzag(level, ParticleFx.dust(GLOW, 0.6F), hand, hand.add(0, 1.6, 0), 5, 0.25, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.0F, 1.8F);
        SpellFxPayload.send(level, SpellFxPayload.STORM, aim.current(), aim.current(), -1, CHARGE);
        Effects.start(level, strike(player, aim));
        return true;
    }

    private static Effect strike(ServerPlayer caster, Targeting.Aim aim) {
        Vec3[] target = { aim.current() };
        List<LivingEntity> struck = new ArrayList<>();
        Vec3[] from = new Vec3[1];
        return (level, age) -> {
            if (age <= CHARGE && (aim.entity() == null || aim.entity().level() == level)) {
                target[0] = aim.current();
            }
            Vec3 at = target[0];
            if (age < CHARGE) {
                charge(level, at, age);
            } else if (age == CHARGE) {
                hit(level, caster, at);
                shock(level, caster, at, struck);
                from[0] = at.add(0, 0.6, 0);
            } else {
                afterglow(level, at, age - CHARGE);
                int hop = age - CHARGE;
                int most = level.isRainingAt(BlockPos.containing(at)) ? STORM_CHAIN : CHAIN;
                if (hop % CHAIN_EVERY == 0 && hop / CHAIN_EVERY <= most && from[0] != null) {
                    from[0] = chain(level, caster, from[0], struck);
                }
            }
            return age < DURATION;
        };
    }

    // Everything hostile close round the strike is shocked stiff for a moment.
    private static void shock(ServerLevel level, ServerPlayer caster, Vec3 at, List<LivingEntity> struck) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(SHOCK_RADIUS),
                entity -> SpellTargets.hits(caster, entity))) {
            if (target.distanceToSqr(at) <= SHOCK_RADIUS * SHOCK_RADIUS) {
                struck.add(target);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHOCKED, 2), caster);
                ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY()
                        + target.getBbHeight() * 0.5, target.getZ(), 10, target.getBbWidth() * 0.5,
                        target.getBbHeight() * 0.4, target.getBbWidth() * 0.5, 0.1);
            }
        }
    }

    // The bolt leaps on to the nearest hostile it has not touched yet; null once there is none left in reach.
    @Nullable
    private static Vec3 chain(ServerLevel level, ServerPlayer caster, Vec3 from, List<LivingEntity> struck) {
        LivingEntity next = null;
        double best = CHAIN_REACH * CHAIN_REACH;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(
                CHAIN_REACH), entity -> SpellTargets.hits(caster, entity) && !struck.contains(entity))) {
            Vec3 middle = target.getBoundingBox().getCenter();
            double far = middle.distanceToSqr(from);
            if (far < best && Targeting.clearPath(level, from, middle, caster)) {
                best = far;
                next = target;
            }
        }
        if (next == null) {
            return null;
        }
        struck.add(next);
        Vec3 to = next.getBoundingBox().getCenter();
        SpellFxPayload.send(level, SpellFxPayload.ARC, from, to, -1, 0);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, to, 14, 0.3, 0.2);
        ParticleFx.at(level, ParticleTypes.FLASH, to);
        next.hurt(level.damageSources().source(DamageTypes.LIGHTNING_BOLT, caster), CHAIN_DAMAGE);
        next.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHOCKED, 2), caster);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8F,
                1.5F + ParticleFx.RANDOM.nextFloat() * 0.3F);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.6F, 1.8F);
        return to;
    }

    private static void charge(ServerLevel level, Vec3 at, int age) {
        double progress = (double) age / CHARGE;
        if (age == 0) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.35F,
                    0.55F);
        }
        if (age % 3 == 0) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                    0.3F + 0.4F * (float) progress, 1.2F + 0.8F * (float) progress);
        }
        // Loose static crawls over whatever stands under the cloud: a warning of what comes.
        if (age % 2 == 1) {
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(
                    RUNE_RADIUS))) {
                ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, near.getX(), near.getY() + near.getBbHeight()
                        * 0.5, near.getZ(), 2, near.getBbWidth() * 0.4, near.getBbHeight() * 0.4, near.getBbWidth()
                        * 0.4, 0.02);
            }
        }
        if (age == CHARGE / 2) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 2.0F);
        }
    }

    private static void hit(ServerLevel level, ServerPlayer caster, Vec3 at) {
        // The game's own bolt is never spawned, so only our drawn one shows; it still strikes through the game's
        // own hit, so creepers charge, the struck burn and everything else lightning does still happens.
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at);
            bolt.setCause(caster);
            for (Entity struck : level.getEntities((Entity) null, new AABB(at.x - 3.0, at.y - 3.0, at.z - 3.0,
                    at.x + 3.0, at.y + 9.0, at.z + 3.0), Entity::isAlive)) {
                if (!EventHooks.onEntityStruckByLightning(struck, bolt)) {
                    struck.thunderHit(level, bolt);
                }
            }
            ignite(level, BlockPos.containing(at));
        }

        Vec3 top = at.add(ParticleFx.spread(1.5), CLOUD_HEIGHT, ParticleFx.spread(1.5));
        SpellFxPayload.send(level, SpellFxPayload.BOLT, top, at, -1, 0);
        ParticleFx.at(level, ParticleTypes.FLASH, at.add(0, 0.5, 0));
        ParticleFx.shockwave(level, ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.2, 0), 56, 0.85);
        ParticleFx.cloud(level, ParticleTypes.LARGE_SMOKE, at, 12, 0.5, 0.05);
        // Ground read only where loaded, so this can't force a chunk to load.
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
        BlockState ground = level.isLoaded(below) ? level.getBlockState(below) : Blocks.AIR.defaultBlockState();
        if (!ground.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                    40, 0.6, 0.2, 0.6, 0.3);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 1.0F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 2.0F, 1.0F);
    }

    // Fire where it strikes and round it, as the game's own lightning lights it.
    private static void ignite(ServerLevel level, BlockPos pos) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)
                || level.getDifficulty() != Difficulty.NORMAL && level.getDifficulty() != Difficulty.HARD) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            BlockPos spot = i == 0 ? pos : pos.offset(level.getRandom().nextInt(3) - 1, level.getRandom().nextInt(3)
                    - 1, level.getRandom().nextInt(3) - 1);
            BlockState fire = BaseFireBlock.getState(level, spot);
            if (level.isLoaded(spot) && level.getBlockState(spot).isAir() && fire.canSurvive(level, spot)) {
                level.setBlockAndUpdate(spot, fire);
            }
        }
    }

    private static void afterglow(ServerLevel level, Vec3 at, int t) {
        double fade = 1.0 - t / 30.0;
        if (ParticleFx.chance(fade)) {
            ParticleFx.fly(level, ParticleTypes.ELECTRIC_SPARK,
                    at.add(ParticleFx.spread(1.2), 0.1, ParticleFx.spread(1.2)), new Vec3(0, 1, 0), 0.15);
        }
        if (t % 4 == 0 && ParticleFx.chance(fade)) {
            ParticleFx.fly(level, ParticleTypes.SMOKE, at.add(ParticleFx.spread(0.6), 0.2, ParticleFx.spread(0.6)),
                    new Vec3(0, 1, 0), 0.04);
        }
    }
}
