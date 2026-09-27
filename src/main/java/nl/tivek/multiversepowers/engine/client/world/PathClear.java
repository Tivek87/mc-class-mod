package nl.tivek.multiversepowers.engine.client.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A thick line (a tentacle, a cable) as drawn, kept out of blocks: its ends stay where they are, a stretch that runs
// into a block goes straight from the free point before it to the free point after, and what still grazes a block
// is pushed off and eased, so it bends round what is in the way. Where it is meant to be in a block, next to its far
// end (a claw gripping a block it lifts), it stays; a path that cannot get clear (through a thick wall) is drawn as
// it came. Foliage gives way. The client's own drawing only: nothing it does reaches the server.
public final class PathClear {
    private static final int ROUNDS = 3;
    private static final double EASE = 0.25;
    private static final BlockPos.MutableBlockPos AT = new BlockPos.MutableBlockPos();
    private static double[] xs = new double[128];
    private static double[] ys = new double[128];
    private static double[] zs = new double[128];

    private PathClear() {
    }

    // The path as it may be drawn, pieces no longer than spacing; the very list given when nothing is in the way.
    public static List<Vec3> clear(Level level, List<Vec3> points, double radius, double spacing) {
        int n = points.size();
        if (n < 2 || radius <= 0.0 || spacing <= 0.0) {
            return points;
        }
        int count = 1;
        for (int i = 1; i < n; i++) {
            count += pieces(points.get(i - 1), points.get(i), spacing);
        }
        if (xs.length < count) {
            xs = new double[count * 2];
            ys = new double[count * 2];
            zs = new double[count * 2];
        }
        int m = put(0, points.get(0));
        for (int i = 1; i < n; i++) {
            Vec3 a = points.get(i - 1);
            Vec3 b = points.get(i);
            int pieces = pieces(a, b, spacing);
            for (int k = 1; k <= pieces; k++) {
                m = put(m, a.lerp(b, (double) k / pieces));
            }
        }
        boolean touched = false;
        for (int i = 1; i < count - 1 && !touched; i++) {
            touched = push(level, i, radius, false);
        }
        if (!touched) {
            return points;
        }
        int first = 1;
        int last = count - 2;
        if (inside(level, count - 1)) {
            while (last >= first && inside(level, last)) {
                last--;
            }
        }
        if (first > last) {
            return points;
        }
        for (int i = first; i <= last; i++) {
            if (!inside(level, i)) {
                continue;
            }
            int from = i - 1;
            int to = i;
            while (to <= last && inside(level, to)) {
                to++;
            }
            for (int k = i; k < to; k++) {
                double t = (double) (k - from) / (to - from);
                xs[k] = xs[from] + (xs[to] - xs[from]) * t;
                ys[k] = ys[from] + (ys[to] - ys[from]) * t;
                zs[k] = zs[from] + (zs[to] - zs[from]) * t;
            }
            i = to;
        }
        for (int round = 0; round < ROUNDS; round++) {
            for (int i = first; i <= last; i++) {
                push(level, i, radius, true);
            }
            for (int i = first; i <= last; i++) {
                xs[i] += ((xs[i - 1] + xs[i + 1]) * 0.5 - xs[i]) * EASE;
                ys[i] += ((ys[i - 1] + ys[i + 1]) * 0.5 - ys[i]) * EASE;
                zs[i] += ((zs[i - 1] + zs[i + 1]) * 0.5 - zs[i]) * EASE;
            }
        }
        for (int i = first; i <= last; i++) {
            push(level, i, radius, true);
        }
        for (int i = first; i <= last; i++) {
            if (inside(level, i) || i < last && Solid.firm(level, (xs[i] + xs[i + 1]) * 0.5,
                    (ys[i] + ys[i + 1]) * 0.5, (zs[i] + zs[i + 1]) * 0.5)) {
                return points;
            }
        }
        List<Vec3> clear = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            clear.add(new Vec3(xs[i], ys[i], zs[i]));
        }
        return clear;
    }

    private static int pieces(Vec3 a, Vec3 b, double spacing) {
        return Math.max(1, (int) Math.ceil(a.distanceTo(b) / spacing));
    }

    private static int put(int m, Vec3 point) {
        xs[m] = point.x;
        ys[m] = point.y;
        zs[m] = point.z;
        return m + 1;
    }

    private static boolean inside(Level level, int i) {
        return Solid.firm(level, xs[i], ys[i], zs[i]);
    }

    // Whether point i's ball of radius touches a solid block; with move, it is pushed clear of each it touches.
    private static boolean push(Level level, int i, double radius, boolean move) {
        boolean touched = false;
        int x0 = (int) Math.floor(xs[i] - radius);
        int x1 = (int) Math.floor(xs[i] + radius);
        int y0 = (int) Math.floor(ys[i] - radius);
        int y1 = (int) Math.floor(ys[i] + radius);
        int z0 = (int) Math.floor(zs[i] - radius);
        int z1 = (int) Math.floor(zs[i] + radius);
        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    AT.set(bx, by, bz);
                    BlockState state = level.getBlockState(AT);
                    if (state.isAir() || state.is(BlockTags.LEAVES)) {
                        continue;
                    }
                    if (state.isCollisionShapeFullBlock(level, AT)) {
                        touched |= out(i, bx, by, bz, bx + 1.0, by + 1.0, bz + 1.0, radius, move);
                        continue;
                    }
                    for (AABB box : state.getCollisionShape(level, AT).toAabbs()) {
                        touched |= out(i, bx + box.minX, by + box.minY, bz + box.minZ, bx + box.maxX, by + box.maxY,
                                bz + box.maxZ, radius, move);
                    }
                }
            }
        }
        return touched;
    }

    private static boolean out(int i, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            double radius, boolean move) {
        double x = xs[i];
        double y = ys[i];
        double z = zs[i];
        double dx = x - Math.max(minX, Math.min(x, maxX));
        double dy = y - Math.max(minY, Math.min(y, maxY));
        double dz = z - Math.max(minZ, Math.min(z, maxZ));
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 >= radius * radius) {
            return false;
        }
        if (!move) {
            return true;
        }
        if (d2 > 1.0E-12) {
            double d = Math.sqrt(d2);
            double by = (radius - d) / d;
            xs[i] += dx * by;
            ys[i] += dy * by;
            zs[i] += dz * by;
            return true;
        }
        // Inside the box: out through the nearest face.
        double left = x - minX;
        double right = maxX - x;
        double down = y - minY;
        double up = maxY - y;
        double back = z - minZ;
        double front = maxZ - z;
        double least = Math.min(Math.min(Math.min(left, right), Math.min(down, up)), Math.min(back, front));
        if (least == left) {
            xs[i] -= left + radius;
        } else if (least == right) {
            xs[i] += right + radius;
        } else if (least == down) {
            ys[i] -= down + radius;
        } else if (least == up) {
            ys[i] += up + radius;
        } else if (least == back) {
            zs[i] -= back + radius;
        } else {
            zs[i] += front + radius;
        }
        return true;
    }
}
