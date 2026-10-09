package nl.tivek.multiversepowers.character.greenlantern.client.mech.walk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final double SOLE_ACROSS = 0.6;
    static final double SOLE_AHEAD = 1.45;
    private static final double SOLE_BACK = 1.2;
    // Along its stride a foot can stand this far ahead of or behind where it rests.
    private static final double STRIDE_REACH = 1.2;
    private static final int ROOM = 2;
    private static final double FACE_STEP = 0.25;
    // Ground no further than this under a level counts as standing on it: a dip, or a step the legs take.
    private static final double LEVEL = 1.0;

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

    // The ground the legs can hold the mech up on where `stage` stands, up to `top`: the highest level either foot finds
    // anywhere along its stride that lies under the mech's middle or all round it. A hole narrower than its stance never
    // drops it in, but its middle never stands out over an edge: past one it drops, or stops where the drop is too deep
    // to see the bottom of (null: nothing holds it up there).
    @Nullable
    static Double support(ClientLevel level, MechScript.Stage stage, double top) {
        double from = stage.base().y;
        Vec3 middle = stage.base();
        Vec3 ahead = stage.ahead();
        Vec3 across = new Vec3(-ahead.z, 0.0, ahead.x).scale(SOLE_ACROSS);
        List<Vec3> found = new ArrayList<>();
        Double under = below(level, middle, from, top);
        for (int side = 0; side < 2; side++) {
            Vec3 home = MechPainter.side(MechScript.ANKLE, side == 0);
            for (int k = -1; k <= 1; k++) {
                Vec3 at = stage.point(home.add(0.0, 0.0, k * STRIDE_REACH));
                for (Vec3 spot : new Vec3[] { at, at.add(across), at.subtract(across), at.add(ahead.scale(SOLE_AHEAD)),
                        at.subtract(ahead.scale(SOLE_BACK)) }) {
                    Double ground = below(level, spot, from, top);
                    if (ground != null) {
                        found.add(new Vec3(spot.x, ground, spot.z));
                    }
                }
            }
        }
        if (under != null) {
            found.add(new Vec3(middle.x, under, middle.z));
        }
        found.sort((a, b) -> Double.compare(b.y, a.y));
        for (int i = 0; i < found.size(); i++) {
            double height = found.get(i).y;
            if (i > 0 && height == found.get(i - 1).y) {
                continue;
            }
            if (under != null && under >= height - LEVEL) {
                return height;
            }
            int holding = i;
            while (holding < found.size() && found.get(holding).y >= height - LEVEL) {
                holding++;
            }
            if (round(found, holding, middle.x, middle.z)) {
                return height;
            }
        }
        return null;
    }

    @Nullable
    private static Double below(ClientLevel level, Vec3 at, double from, double top) {
        Double ground = ground(level, at, from);
        return ground == null || ground > top ? null : ground;
    }

    // Whether the first `count` spots stand all round (x, z): no line through it has them all to one side.
    private static boolean round(List<Vec3> spots, int count, double x, double z) {
        if (count < 3) {
            return false;
        }
        double[] angles = new double[count];
        for (int i = 0; i < count; i++) {
            angles[i] = Math.atan2(spots.get(i).z - z, spots.get(i).x - x);
        }
        Arrays.sort(angles);
        double widest = angles[0] + 2.0 * Math.PI - angles[count - 1];
        for (int i = 1; i < count; i++) {
            widest = Math.max(widest, angles[i] - angles[i - 1]);
        }
        return widest < Math.PI;
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

    // The first ledge along `ahead` from `at`, within `far`: {how far off its face is, the height of its top}. Its face
    // stands solid from `low` up (open ground under a roof is none), and its top is the lowest one up to `high` with
    // room for a foot above it.
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
                VoxelShape shape = wades(block) ? null : block.getCollisionShape(level, pos);
                if (shape == null || shape.isEmpty() || y + shape.max(Direction.Axis.Y) <= low) {
                    if (y == Mth.floor(low)) {
                        break;
                    }
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

    // How far along `ahead` from `at` the first solid block stands at the height `y`, within `far`, worked out to a
    // hand's breadth; null when there is none.
    @Nullable
    static Double face(ClientLevel level, Vec3 at, Vec3 ahead, double y, double far) {
        for (double d = 0.0; d <= far; d += FACE_STEP) {
            if (solid(level, at.x + ahead.x * d, y, at.z + ahead.z * d)) {
                double open = Math.max(0.0, d - FACE_STEP);
                double shut = d;
                for (int i = 0; i < 5; i++) {
                    double middle = (open + shut) * 0.5;
                    if (solid(level, at.x + ahead.x * middle, y, at.z + ahead.z * middle)) {
                        shut = middle;
                    } else {
                        open = middle;
                    }
                }
                return d == 0.0 ? 0.0 : shut;
            }
        }
        return null;
    }

    // Whether the point stands in something solid a mech cannot wade through.
    static boolean solid(ClientLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState block = level.getBlockState(pos);
        if (wades(block)) {
            return false;
        }
        VoxelShape shape = block.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.bounds().move(pos).contains(x, y, z);
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
