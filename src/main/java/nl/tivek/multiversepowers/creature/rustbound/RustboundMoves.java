package nl.tivek.multiversepowers.creature.rustbound;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The Rustbound's moves. Close up it SLAMs its scrap fist down (a raised shield is knocked aside first). From 4 to 14
// blocks, dry and every 9 seconds or so, it PULLs: feet planted, arms out, its core flares and drags whatever stands in
// front of it in, the more metal it wears and holds the harder, and at the end it jerks its arms back, tearing a metal
// weapon or shield out of a hand now and then (it lands at its feet). From 4 to 16 blocks it flings its circling scrap
// in a SHRAPNEL fan; each piece that strikes leaves a wound that bleeds.
public final class RustboundMoves extends Goal {
    public static final int SLAM = 1;
    public static final int PULL = 2;
    public static final int SHRAPNEL = 3;
    public static final int KINDS = 3;
    public static final int SLAM_HIT = 12;
    public static final int SLAM_END = 24;
    public static final int PULL_FROM = 12;
    public static final int YANK = 40;
    public static final int PULL_END = 50;
    public static final int THROW = 10;
    public static final int THROW_END = 20;
    private static final double REACH = 1.5;
    private static final double PULL_LEAST = 4.0;
    private static final double PULL_RANGE = 14.0;
    private static final double PULL_CONE = 0.35;
    private static final double THROW_RANGE = 16.0;
    private static final int THROWN_MOST = 5;
    private static final float SHARD_DAMAGE = 3.0F;
    private static final int SHIELD_OFF = 100;
    private static final double YANK_CHANCE = 0.45;

    private final Rustbound rustbound;
    private final List<LivingEntity> pulled = new ArrayList<>();
    private int kind;
    private int t;
    private int slamReady;
    private int pullReady = 60;
    private int throwReady = 40;
    private int repath;

    RustboundMoves(Rustbound rustbound) {
        this.rustbound = rustbound;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.rustbound.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.kind != 0 || this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.rustbound.setAggressive(true);
    }

    @Override
    public void stop() {
        if (this.kind != 0) {
            this.rustbound.start(0);
        }
        this.kind = 0;
        this.rustbound.setAggressive(false);
        this.rustbound.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.rustbound.getTarget();
        if (target != null) {
            this.rustbound.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.kind != 0) {
            this.t++;
            this.go(target);
            return;
        }
        if (target == null) {
            return;
        }
        int now = this.rustbound.tickCount;
        double far = MonsterBlows.gap(this.rustbound, target);
        boolean seen = this.rustbound.getSensing().hasLineOfSight(target);
        if (far <= REACH && now >= this.slamReady) {
            this.begin(SLAM);
        } else if (far >= PULL_LEAST && far <= PULL_RANGE && now >= this.pullReady && seen && !this.rustbound.wet()) {
            this.begin(PULL);
        } else if (far >= PULL_LEAST && far <= THROW_RANGE && now >= this.throwReady && seen
                && this.rustbound.shards() >= 3) {
            this.begin(SHRAPNEL);
        } else if (--this.repath <= 0) {
            this.repath = 5;
            this.rustbound.getNavigation().moveTo(target, 1.0);
        }
    }

    private void begin(int kind) {
        this.kind = kind;
        this.t = 0;
        this.rustbound.getNavigation().stop();
        this.rustbound.start(kind);
        switch (kind) {
            case SLAM -> this.rustbound.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 0.6F);
            case PULL -> {
                this.pulled.clear();
                this.rustbound.playSound(SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
                this.rustbound.playSound(SoundEvents.ZOMBIE_AMBIENT, 1.0F, 0.6F);
            }
            case SHRAPNEL -> this.rustbound.playSound(SoundEvents.CHAIN_HIT, 1.0F, 0.8F);
            default -> {
            }
        }
    }

    private void go(@Nullable LivingEntity target) {
        int now = this.rustbound.tickCount;
        switch (this.kind) {
            case SLAM -> {
                if (this.t == SLAM_HIT) {
                    this.slam(target);
                }
                if (this.t >= SLAM_END) {
                    this.slamReady = now + 10;
                    this.end();
                }
            }
            case PULL -> {
                if (this.t >= PULL_FROM && this.t < YANK) {
                    this.pull();
                }
                if (this.t == YANK) {
                    this.yank();
                }
                if (this.t >= PULL_END) {
                    this.pullReady = now + 160 + this.rustbound.getRandom().nextInt(60);
                    this.end();
                }
            }
            case SHRAPNEL -> {
                if (this.t == THROW && target != null) {
                    this.fling(target);
                }
                if (this.t >= THROW_END) {
                    this.throwReady = now + 100 + this.rustbound.getRandom().nextInt(40);
                    this.end();
                }
            }
            default -> this.end();
        }
    }

    private void end() {
        this.kind = 0;
        this.rustbound.start(0);
    }

    // The fist comes down on what is in front of it: a raised shield is knocked aside first, then the blow lands and
    // throws it back.
    private void slam(@Nullable LivingEntity target) {
        if (!(this.rustbound.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 ahead = MonsterBlows.ahead(this.rustbound);
        Vec3 at = this.rustbound.position().add(ahead.scale(1.1));
        MonsterBlows.dust(level, at, 16, 0.6);
        ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()),
                at.x, at.y + 0.3, at.z, 8, 0.3, 0.2, 0.3, 0.1);
        this.rustbound.playSound(SoundEvents.ANVIL_LAND, 0.6F, 0.7F);
        if (target == null || MonsterBlows.gap(this.rustbound, target) > REACH + 0.6) {
            return;
        }
        if (target instanceof Player player && player.isBlocking()) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
                    player.getCooldowns().addCooldown(held.getItem(), SHIELD_OFF);
                }
            }
            player.stopUsingItem();
            player.level().broadcastEntityEvent(player, (byte) 30);
        }
        MonsterBlows.hurt(target, this.rustbound.damageSources().mobAttack(this.rustbound), MonsterBlows.damage(
                this.rustbound), ahead.scale(0.7).add(0.0, 0.3, 0.0));
    }

    // Each tick of the pull: whatever stands in front of it, in sight, is dragged towards its core, harder the more
    // metal it carries; iron dust streams in to the core.
    private void pull() {
        if (!(this.rustbound.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 core = this.rustbound.core();
        Vec3 ahead = MonsterBlows.ahead(this.rustbound);
        AABB box = new AABB(core, core).inflate(PULL_RANGE);
        for (LivingEntity caught : MonsterBlows.foes(this.rustbound, box)) {
            Vec3 to = caught.getBoundingBox().getCenter().subtract(core);
            double far = to.length();
            if (far > PULL_RANGE || far < 1.6 || to.scale(1.0 / far).dot(ahead) < PULL_CONE
                    || !this.rustbound.hasLineOfSight(caught)) {
                continue;
            }
            if (!this.pulled.contains(caught)) {
                this.pulled.add(caught);
            }
            int metal = Rustbound.metal(caught);
            double pull = Math.min(0.32, 0.05 + 0.035 * metal);
            Vec3 way = to.scale(-1.0 / far);
            double resist = Math.max(0.0, 1.0 - caught.getAttributeValue(
                    Attributes.KNOCKBACK_RESISTANCE) * 0.5);
            Vec3 drag = new Vec3(way.x, Math.max(-0.05, way.y) * 0.5 + 0.02, way.z).scale(pull * resist);
            caught.setDeltaMovement(caught.getDeltaMovement().scale(0.6).add(drag));
            caught.hasImpulse = true;
            caught.hurtMarked = true;
            if (this.t % 2 == 0) {
                Vec3 from = caught.getBoundingBox().getCenter();
                ParticleFx.fly(level, ParticleFx.dust(0x8A8A8A, 0.8F), from, way, 0.6);
                ParticleFx.fly(level, ParticleFx.dust(Rustbound.CORE, 0.7F), from.add(way.scale(far * 0.5)), way,
                        0.6);
            }
        }
        if (this.t % 3 == 0) {
            ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, core.x, core.y, core.z, 3, 0.15, 0.15, 0.15, 0.1);
        }
        if (this.t % 8 == 0) {
            this.rustbound.playSound(SoundEvents.BEACON_AMBIENT, 1.2F, 1.8F);
        }
    }

    // The pull ends with a jerk of its arms: now and then a weapon or shield of metal flies out of a hand it caught
    // and lands at its feet, to be picked up again after 2 seconds.
    private void yank() {
        if (!(this.rustbound.level() instanceof ServerLevel level)) {
            return;
        }
        this.rustbound.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 0.9F);
        for (LivingEntity caught : this.pulled) {
            if (!caught.isAlive() || caught.distanceToSqr(this.rustbound) > PULL_RANGE * PULL_RANGE
                    || this.rustbound.getRandom().nextDouble() >= YANK_CHANCE) {
                continue;
            }
            InteractionHand hand = caught.getMainHandItem().is(Rustbound.METAL) ? InteractionHand.MAIN_HAND
                    : caught.getOffhandItem().is(Rustbound.METAL) ? InteractionHand.OFF_HAND : null;
            if (hand == null) {
                continue;
            }
            ItemStack torn = caught.getItemInHand(hand).copy();
            caught.setItemInHand(hand, ItemStack.EMPTY);
            if (caught instanceof Player player) {
                player.stopUsingItem();
            }
            Vec3 from = caught.getEyePosition().subtract(0.0, 0.4, 0.0);
            ItemEntity flying = new ItemEntity(level, from.x, from.y, from.z, torn);
            Vec3 to = this.rustbound.position().subtract(from);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            double reach = flat.length();
            Vec3 way = reach < 1.0E-3 ? Vec3.ZERO : flat.scale(1.0 / reach);
            flying.setDeltaMovement(way.scale(Math.min(1.0, 0.12 + reach * 0.08)).add(0.0, 0.3, 0.0));
            flying.setPickUpDelay(40);
            level.addFreshEntity(flying);
            level.playSound(null, caught.blockPosition(), SoundEvents.ITEM_BREAK, caught.getSoundSource(), 0.8F, 0.6F);
            ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 10, 0.1, 0.1, 0.1, 0.2);
        }
        this.pulled.clear();
    }

    // A fan of the scrap circling it, thrown at `target`.
    private void fling(LivingEntity target) {
        int count = Math.min(THROWN_MOST, this.rustbound.shards());
        if (count <= 0) {
            return;
        }
        Vec3 hand = this.rustbound.position().add(MonsterBlows.ahead(this.rustbound).scale(0.6)).add(0.0, 1.4, 0.0);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(hand);
        double flat = aim.horizontalDistance();
        for (int i = 0; i < count; i++) {
            float spread = count == 1 ? 0.0F : (i / (float) (count - 1) - 0.5F) * 26.0F;
            Vec3 turned = aim.yRot((float) Math.toRadians(spread));
            RustShard shard = new RustShard(this.rustbound.level(), this.rustbound, SHARD_DAMAGE);
            shard.setPos(hand);
            shard.shoot(turned.x, turned.y + flat * 0.06, turned.z, 1.5F, 1.5F);
            this.rustbound.level().addFreshEntity(shard);
        }
        this.rustbound.useShards(count);
        this.rustbound.playSound(SoundEvents.CHAIN_BREAK, 1.2F, 1.3F);
        this.rustbound.playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.7F);
    }
}
