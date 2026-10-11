package nl.tivek.multiversepowers.creature.stalker;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// Where the Stalker can hide from someone: pressed behind a tree trunk on the side away from them, or folded small in
// or behind a bush, always somewhere their eye cannot reach; and where it can blink to unseen, behind their back.
final class StalkerCover {
    private static final double TRUNK_EYE = 1.8;
    private static final double BUSH_EYE = 0.5;
    // Half the widest its body gets (folded in a bush), with a little room.
    private static final double BODY = 0.37;

    record Spot(Vec3 at, int how) {
    }

    private StalkerCover() {
    }

    // The best hiding place within reach blocks of around, between nearest and farthest from the watcher's eye,
    // hidden from it; the nearest to the Stalker wins.
    @Nullable
    static Spot find(ServerLevel level, Stalker stalker, Vec3 around, LivingEntity watcher, int reach,
            double nearest, double farthest) {
        Vec3 eye = watcher.getEyePosition();
        BlockPos middle = BlockPos.containing(around);
        Spot best = null;
        double bestCost = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    pos.set(middle.getX() + dx, middle.getY() + dy, middle.getZ() + dz);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    Spot spot = null;
                    if (state.is(BlockTags.LOGS) && trunk(level, pos)) {
                        spot = behind(level, pos, eye, Stalker.TRUNK, TRUNK_EYE);
                    } else if (bush(level, pos, state)) {
                        spot = state.getCollisionShape(level, pos).isEmpty() ? inside(level, pos, eye)
                                : behind(level, pos, eye, Stalker.BUSH, BUSH_EYE);
                    }
                    if (spot == null) {
                        continue;
                    }
                    double far = spot.at.distanceTo(eye);
                    if (far < nearest || far > farthest) {
                        continue;
                    }
                    double cost = spot.at.distanceTo(stalker.position()) + (spot.how == Stalker.BUSH ? 1.5 : 0.0);
                    if (cost < bestCost) {
                        bestCost = cost;
                        best = spot;
                    }
                }
            }
        }
        return best;
    }

    // A place to blink to behind the watcher's back, between 9 and 15 blocks off: hidden if it can be, else at least
    // out of their sight.
    @Nullable
    static Vec3 behindBack(ServerLevel level, Stalker stalker, LivingEntity watcher) {
        Vec3 look = watcher.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0.0, -look.z);
        back = back.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : back.normalize();
        for (int tries = 0; tries < 12; tries++) {
            double angle = (stalker.getRandom().nextDouble() - 0.5) * 2.4;
            double far = 9.0 + stalker.getRandom().nextDouble() * 6.0;
            Vec3 way = back.yRot((float) angle).scale(far);
            Vec3 rough = watcher.position().add(way);
            Spot spot = find(level, stalker, rough, watcher, 3, 6.0, 20.0);
            if (spot != null) {
                return spot.at;
            }
            Vec3 ground = ground(level, BlockPos.containing(rough));
            if (ground != null && !seen(level, watcher.getEyePosition(), ground.add(0.0, TRUNK_EYE, 0.0))) {
                return ground;
            }
        }
        return null;
    }

    // Whether eye sees the point `at` with nothing solid or leafy in between.
    static boolean seen(ServerLevel level, Vec3 eye, Vec3 at) {
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(eye, at, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceTo(at) < 0.6;
    }

    // A log with more trunk or leaves above it: a tree, not a log lying on the ground.
    private static boolean trunk(ServerLevel level, BlockPos log) {
        BlockState above = level.getBlockState(log.above());
        return above.is(BlockTags.LOGS) || above.is(BlockTags.LEAVES);
    }

    // Leaves sitting on the ground, or a tall plant to fold down in.
    private static boolean bush(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.ROSE_BUSH) || state.is(Blocks.LILAC) || state.is(Blocks.PEONY)) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
        }
        if (!state.is(BlockTags.LEAVES)) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        return !below.is(BlockTags.LOGS) && !below.is(BlockTags.LEAVES) && below.isFaceSturdy(level, pos.below(),
                net.minecraft.core.Direction.UP);
    }

    // Folded inside a plant that lets it through.
    @Nullable
    private static Spot inside(ServerLevel level, BlockPos plant, Vec3 eye) {
        Vec3 at = new Vec3(plant.getX() + 0.5, plant.getY(), plant.getZ() + 0.5);
        return seen(level, eye, at.add(0.0, BUSH_EYE, 0.0)) ? null : new Spot(at, Stalker.BUSH);
    }

    // Against the side of the cover away from the eye, on ground it can stand on.
    @Nullable
    private static Spot behind(ServerLevel level, BlockPos cover, Vec3 eye, int how, double height) {
        Vec3 middle = Vec3.atCenterOf(cover);
        Vec3 away = new Vec3(middle.x - eye.x, 0.0, middle.z - eye.z);
        if (away.lengthSqr() < 1.0E-4) {
            return null;
        }
        away = away.normalize();
        double tall = how == Stalker.TRUNK ? 2.3 : 0.8;
        // Pressed as close to the cover as its body fits.
        for (double off = 0.86; off <= 1.4; off += 0.1) {
            double x = middle.x + away.x * off;
            double z = middle.z + away.z * off;
            BlockPos column = BlockPos.containing(x, middle.y, z);
            for (int down = 0; down <= 3; down++) {
                Vec3 ground = standing(level, column.below(down), how == Stalker.TRUNK ? 3 : 1);
                if (ground == null) {
                    continue;
                }
                Vec3 at = new Vec3(x, ground.y, z);
                if (!level.noCollision(new AABB(x - BODY, ground.y + 0.01, z - BODY, x + BODY, ground.y + tall,
                        z + BODY))) {
                    break;
                }
                return seen(level, eye, at.add(0.0, height, 0.0)) ? null : new Spot(at, how);
            }
        }
        return null;
    }

    @Nullable
    private static Vec3 ground(ServerLevel level, BlockPos rough) {
        for (int dy = 4; dy >= -6; dy--) {
            Vec3 at = standing(level, rough.above(dy), 3);
            if (at != null) {
                return at;
            }
        }
        return null;
    }

    // Feet at `feet` with room above for `tall` blocks and firm ground below.
    @Nullable
    private static Vec3 standing(ServerLevel level, BlockPos feet, int tall) {
        if (!level.isLoaded(feet) || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(),
                net.minecraft.core.Direction.UP)) {
            return null;
        }
        for (int up = 0; up < tall; up++) {
            if (!level.getBlockState(feet.above(up)).getCollisionShape(level, feet.above(up)).isEmpty()) {
                return null;
            }
        }
        return new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
    }
}
