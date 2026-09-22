package nl.tivek.welcomescreen.character.docock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;
import nl.tivek.welcomescreen.spell.SpellTargeting;

/**
 * What the tentacles do with blocks: pick one up, pull a whole cluster or a small building out of the
 * world, put it back down somewhere else, or hurl it at whatever is in front of you.
 *
 * <p>A cluster is taken by walking from the block you aim at to the blocks touching it, so a wall, a
 * pillar or a little hut comes along in one piece, keeping every block exactly where it was relative
 * to the others. Blocks with something inside them (chests, furnaces) and unbreakable blocks are
 * always left alone, and nothing is taken where the player is not allowed to build.
 */
final class TentacleBlocks {
    /** How fast a thrown load flies. */
    static final double THROW_SPEED = 1.5;
    private static final int HIT_TIME = 60;
    private static final double HIT_RADIUS = 0.8;

    private TentacleBlocks() {
    }

    /** One block of a load: where it sits relative to the first one, and what it is. */
    record Piece(BlockPos offset, BlockState state) {
    }

    /** Everything one tentacle carries. */
    record Load(List<Piece> pieces) {
        int size() {
            return this.pieces.size();
        }
    }

    /**
     * What came of setting a load down.
     *
     * @param placed how many blocks really went into the world
     * @param left   what did not fit and stays in the claw; null when everything was placed
     */
    record Result(int placed, @Nullable Load left) {
    }

    // ---- Picking up ----

    /**
     * Pulls the block the player aims at out of the world, with everything touching it when
     * {@code cluster} is on.
     *
     * @return what the tentacle now carries, or null when there is nothing to take there
     */
    @Nullable
    static Load pickUp(ServerLevel level, ServerPlayer player, boolean cluster, int max, double range) {
        BlockHitResult hit = aim(player, level, range);
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockPos origin = hit.getBlockPos();
        if (!canTake(level, player, origin)) {
            return null;
        }
        List<BlockPos> taken = new ArrayList<>();
        if (cluster) {
            Set<BlockPos> seen = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(origin);
            seen.add(origin);
            while (!queue.isEmpty() && taken.size() < max) {
                BlockPos pos = queue.poll();
                taken.add(pos);
                for (Direction side : Direction.values()) {
                    BlockPos next = pos.relative(side);
                    if (seen.add(next) && next.distSqr(origin) <= 64 && canTake(level, player, next)) {
                        queue.add(next);
                    }
                }
            }
        } else {
            taken.add(origin);
        }
        List<Piece> pieces = new ArrayList<>(taken.size());
        for (BlockPos pos : taken) {
            BlockState state = level.getBlockState(pos);
            pieces.add(new Piece(pos.subtract(origin), state));
            level.removeBlock(pos, false);
            level.levelEvent(2001, pos, Block.getId(state));
        }
        level.playSound(null, origin, SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 0.7F);
        level.playSound(null, origin, SoundEvents.PISTON_CONTRACT, SoundSource.PLAYERS, 0.8F, 0.6F);
        return new Load(List.copyOf(pieces));
    }

    /** A block the tentacles may take: solid, breakable, nothing stored inside, and allowed here. */
    private static boolean canTake(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.mayInteract(player, pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && level.getBlockEntity(pos) == null
                && state.getDestroySpeed(level, pos) >= 0.0F
                && !state.getCollisionShape(level, pos).isEmpty();
    }

    // ---- Putting down ----

    /**
     * Sets the load down where the player aims, keeping its shape. Blocks that do not fit there stay in
     * the claw, so nothing is ever lost.
     */
    static Result place(ServerLevel level, ServerPlayer player, Load load, double range) {
        BlockHitResult hit = aim(player, level, range);
        BlockPos origin;
        if (hit.getType() == HitResult.Type.MISS) {
            Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(range));
            origin = BlockPos.containing(end);
        } else {
            BlockState there = level.getBlockState(hit.getBlockPos());
            origin = there.canBeReplaced() ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
        }
        int placed = 0;
        List<Piece> left = new ArrayList<>();
        for (Piece piece : load.pieces()) {
            BlockPos pos = origin.offset(piece.offset());
            if (level.isOutsideBuildHeight(pos) || !level.mayInteract(player, pos)
                    || !level.getBlockState(pos).canBeReplaced()) {
                left.add(piece);
                continue;
            }
            level.setBlockAndUpdate(pos, piece.state());
            placed++;
        }
        if (placed > 0) {
            BlockState first = load.pieces().get(0).state();
            level.playSound(null, origin, first.getSoundType(level, origin, player).getPlaceSound(),
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            level.playSound(null, origin, SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        return new Result(placed, left.isEmpty() ? null : new Load(List.copyOf(left)));
    }

    // ---- Throwing ----

    /** Hurls the load away from {@code from} in {@code direction}; what it hits gets hurt. */
    static void hurl(ServerLevel level, ServerPlayer player, Load load, Vec3 from, Vec3 direction, float damage,
            double speed) {
        List<FallingBlockEntity> flying = new ArrayList<>();
        for (Piece piece : load.pieces()) {
            Vec3 at = from.add(piece.offset().getX(), piece.offset().getY(), piece.offset().getZ());
            BlockPos spawn = freeSpot(level, at);
            if (spawn == null) {
                // Nowhere to let it go: hand the block over as an item instead of losing it.
                Block.popResource(level, BlockPos.containing(at), new ItemStack(piece.state().getBlock()));
                continue;
            }
            // A falling block starts in the middle of the spot it is born in and is left right there.
            // Moving it afterwards is what made thrown blocks jump across the screen once.
            FallingBlockEntity block = FallingBlockEntity.fall(level, spawn, piece.state());
            block.setDeltaMovement(direction.scale(speed)
                    .add(SpellFx.spread(0.06), SpellFx.spread(0.06), SpellFx.spread(0.06)));
            block.hasImpulse = true;
            flying.add(block);
        }
        if (flying.isEmpty()) {
            return;
        }
        level.playSound(null, from.x, from.y, from.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F,
                0.6F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 1.3F);
        SpellCasting.start(level, hitting(player, flying, damage));
    }

    /** Lets the load go where it is, slowly, so nothing is ever lost (the arms fold in). */
    static void drop(ServerLevel level, ServerPlayer player, Load load, Vec3 from) {
        hurl(level, player, load, from, new Vec3(0, -0.2, 0), 0.0F, 0.2);
    }

    /** A block position the falling block may appear in: its own spot, else one right beside it. */
    @Nullable
    private static BlockPos freeSpot(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        if (isFree(level, pos)) {
            return pos;
        }
        // Its own spot is taken (it was pulled through a wall): the nearest open block around it.
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (isFree(level, next)) {
                return next;
            }
        }
        return null;
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getBlockState(pos).canBeReplaced();
    }

    /** The flying blocks hurt every creature they fly through, each one only once. */
    private static SpellEffect hitting(ServerPlayer caster, List<FallingBlockEntity> flying, float damage) {
        Set<Entity> already = new HashSet<>();
        return (level, age) -> {
            if (age > HIT_TIME || damage <= 0.0F) {
                return false;
            }
            boolean any = false;
            for (FallingBlockEntity block : flying) {
                if (!block.isAlive()) {
                    continue;
                }
                any = true;
                AABB box = block.getBoundingBox().inflate(HIT_RADIUS);
                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box,
                        entity -> entity != caster && already.add(entity)
                                && SpellTargeting.isTargetable(caster, entity))) {
                    living.invulnerableTime = 0;
                    living.hurt(level.damageSources().playerAttack(caster), damage);
                    Vec3 push = block.getDeltaMovement();
                    living.knockback(0.6, -push.x, -push.z);
                    Vec3 at = living.getBoundingBox().getCenter();
                    SpellFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, block.getBlockState()),
                            at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.2);
                    SpellFx.cloud(level, ParticleTypes.CRIT, at, 8, 0.25, 0.3);
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7F,
                            1.2F);
                }
            }
            return any;
        };
    }

    // ---- Helpers ----

    private static BlockHitResult aim(ServerPlayer player, ServerLevel level, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        return level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }
}
