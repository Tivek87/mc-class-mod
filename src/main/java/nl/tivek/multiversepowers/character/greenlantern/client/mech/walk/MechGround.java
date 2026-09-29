package nl.tivek.multiversepowers.character.greenlantern.client.mech.walk;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;

// What a mech stands on: the ground under a spot, under a whole foot and under both feet, walls a foot runs into and
// ledges it can climb. Trees and plants it wades through count as air, water as ground; ground is only a top with room
// above it, so a cave's roof is never taken for the floor.
final class MechGround {
    static final double GROUND_ABOVE = 4.0;
    static final double GROUND_BELOW = 9.0;
    // A foot's sole reaches this far to either side, ahead and back from its ankle.
    private static final double SOLE_ACROSS = 0.45;
    static final double SOLE_AHEAD = 1.0;
    private static final double SOLE_BACK = 0.8;
    // Along its stride a foot can stand this far ahead of or behind where it rests.
    private static final double STRIDE_REACH = 1.2;
    private static final int ROOM = 2;

    private MechGround() {
    }

    // The top of the ground under at, looking down from GROUND_ABOVE over from to GROUND_BELOW under it.
    @Nullable
    static Double ground(ClientLevel level, Vec3 at, double from) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(at.x), Mth.floor(from), Mth.floor(at.z));
        if (!level.isLoaded(pos)) {
            return null;
        }
        int top = Mth.floor(from + GROUND_ABOVE);
        boolean open = passable(level, pos.setY(top + 1));
        for (int y = top; y >= Mth.floor(from - GROUND_BELOW); y--) {
            pos.setY(y);
            BlockState block = level.getBlockState(pos);
            if (wades(block)) {
                open = true;
                continue;
            }
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.isEmpty()) {
                if (open) {
                    return y + (double) fluid.getHeight(level, pos);
                }
                continue;
            }
            VoxelShape shape = block.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                open = true;
            } else if (open) {
                return y + shape.max(Direction.Axis.Y);
            }
        }
        return null;
    }

    static boolean wades(BlockState block) {
        return block.is(BlockTags.LEAVES) || block.is(BlockTags.LOGS) || block.is(BlockTags.REPLACEABLE)
                || block.is(BlockTags.FLOWERS) || block.is(BlockTags.SAPLINGS);
    }

    private static boolean passable(ClientLevel level, BlockPos pos) {
        BlockState block = level.getBlockState(pos);
        return wades(block) || block.getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    // What a whole foot rests on at `at`, turned to `ahead`: the highest ground under its sole, leaving out what
    // stands higher than `top` (a wall its toes run into, not a step).
    @Nullable
    static Double foot(ClientLevel level, Vec3 at, Vec3 ahead, double from, double top) {
        Vec3 across = new Vec3(-ahead.z, 0.0, ahead.x).scale(SOLE_ACROSS);
        Double best = null;
        best = higher(best, ground(level, at, from), top);
        best = higher(best, ground(level, at.add(across), from), top);
        best = higher(best, ground(level, at.subtract(across), from), top);
        best = higher(best, ground(level, at.add(ahead.scale(SOLE_AHEAD)), from), top);
        return higher(best, ground(level, at.subtract(ahead.scale(SOLE_BACK)), from), top);
    }

    @Nullable
    private static Double higher(@Nullable Double best, @Nullable Double found, double top) {
        if (found == null || found > top) {
            return best;
        }
        return best == null || found > best ? found : best;
    }

    // The ground the legs can hold the mech up on where `stage` stands: the highest either foot finds anywhere along
    // its stride, up to `top`. A hole narrower than the mech's stance never drops it in.
    @Nullable
    static Double support(ClientLevel level, MechScript.Stage stage, double top) {
        double from = stage.base().y;
        Double best = null;
        for (int side = 0; side < 2; side++) {
            Vec3 home = MechPainter.side(MechScript.ANKLE, side == 0);
            for (int k = -1; k <= 1; k++) {
                Vec3 at = stage.point(home.add(0.0, 0.0, k * STRIDE_REACH));
                best = higher(best, foot(level, at, stage.ahead(), from, top), top);
            }
        }
        return best;
    }

    // Whether a foot at `at` runs into a wall: anything solid from just over `top` up to a knee's height above it.
    static boolean wall(ClientLevel level, Vec3 at, double top) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(at.x), Mth.floor(top), Mth.floor(at.z));
        for (int y = Mth.floor(top + 0.01); y <= Mth.floor(top + ROOM); y++) {
            pos.setY(y);
            BlockState block = level.getBlockState(pos);
            if (wades(block)) {
                continue;
            }
            VoxelShape shape = block.getCollisionShape(level, pos);
            if (!shape.isEmpty() && y + shape.max(Direction.Axis.Y) > top + 0.01) {
                return true;
            }
        }
        return false;
    }

    // The first ledge along `ahead` from `at`, within `far`: {how far off its face is, the height of its top}. Its top
    // is the lowest one over `low` and up to `high` with room for a foot above it.
    @Nullable
    static double[] ledge(ClientLevel level, Vec3 at, Vec3 ahead, double low, double high, double far) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double d = 0.3; d <= far; d += 0.2) {
            Vec3 spot = at.add(ahead.scale(d));
            pos.set(Mth.floor(spot.x), Mth.floor(low), Mth.floor(spot.z));
            if (!level.isLoaded(pos)) {
                return null;
            }
            for (int y = Mth.floor(low); y <= Mth.floor(high); y++) {
                pos.setY(y);
                BlockState block = level.getBlockState(pos);
                if (wades(block)) {
                    continue;
                }
                VoxelShape shape = block.getCollisionShape(level, pos);
                if (shape.isEmpty()) {
                    continue;
                }
                double top = y + shape.max(Direction.Axis.Y);
                if (top > low && top <= high && room(level, pos, y)) {
                    return new double[] { d, top };
                }
            }
        }
        return null;
    }

    private static boolean room(ClientLevel level, BlockPos.MutableBlockPos pos, int y) {
        for (int k = 1; k <= ROOM; k++) {
            if (!passable(level, pos.setY(y + k))) {
                pos.setY(y);
                return false;
            }
        }
        pos.setY(y);
        return true;
    }
}
