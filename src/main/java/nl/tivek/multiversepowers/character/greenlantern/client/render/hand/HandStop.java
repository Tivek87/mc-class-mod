package nl.tivek.multiversepowers.character.greenlantern.client.render.hand;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.world.Solid;

// A hand out of the ground or a wall never passes into the blocks round it: it is held back, its arm leaning back
// towards where it stands and its wrist straightening, only as far as it takes for arm, hand and fingers to be clear.
// Its moves are known ahead, so it sees a wall coming and gives way ticks before, never faster than RATE: it never
// jumps. Foliage gives way to it. The blow is the server's; only what is drawn stops.
final class HandStop {
    private static final int REFINE = 7;
    // How far out of the ground (or the wall) a point must be to count: the arm rises out of it on purpose.
    private static final double ROOTED = 0.35;
    // Held back all the way, the arm leans this far past upright, away from where it strikes.
    private static final double BACK = 0.3;
    private static final double RATE = 0.2;
    private static final int AHEAD = 6;
    private static final double MOVED = 0.5;
    private static final long FORGET = 100L;
    private static final double FREE = Double.POSITIVE_INFINITY;
    // Points are looked at this far out from the surface, so a block's corner cannot slip in between them.
    private static final double MARGIN = 0.12;
    private static final double[] PALM = palm(1.62, 3.2, 0.52, -0.55, 4);
    // Along each finger bone: its joint and its middle, and the last bone's tip too.
    private static final double[] ALONG = { 0.0, 0.5, 1.0 };
    private static final int FINGER_ROUND = 8;
    private static final double[] FINGERS = new double[5 * 7 * FINGER_ROUND * 3];
    // Down the forearm from the wrist, how thick it is, before it is drawn wide and flat.
    private static final double[][] FOREARM_SHAPE = { { -0.35, 1.06 }, { 0.6, 1.12 }, { 2.2, 1.3 }, { 5.0, 1.55 },
            { 9.0, 1.62 }, { 14.0, 1.55 } };
    private static final double[][] FOREARM = forearm(1.2);
    private static final double WIDE = 1.18;
    private static final double FLAT = 0.82;
    private static final int ROUND = 16;
    // Ticks of the move worked out anew in one frame at most, nearest first: turning to a new target costs little.
    private static final int BUDGET = 2;
    private static final Int2ObjectOpenHashMap<Memory> MEMORY = new Int2ObjectOpenHashMap<>();
    private static long sweptAt;

    private HandStop() {
    }

    // How far a hand may go at each tick of its move, worked out once while it keeps facing the same way.
    private static final class Memory {
        private int variant = -1;
        private Vec3 facing = Vec3.ZERO;
        private double scale;
        private double[] most = new double[0];
        private long used;
        // How far it went in the frame before, so it lets go no faster than it gave way.
        private double lastClock = Double.NaN;
        private double lastMost = FREE;
    }

    // The palm's box as points on its faces, in the hand's own frame: across, up the hand, and front and back.
    private static double[] palm(double side, double top, double front, double back, int steps) {
        double[] points = new double[((steps + 1) * (steps + 1) * 2 + (steps + 1) * 3) * 3];
        double middle = (front + back) * 0.5;
        int n = 0;
        for (int i = 0; i <= steps; i++) {
            double x = -side + 2.0 * side * i / steps;
            for (int j = 0; j <= steps; j++) {
                double y = top * j / steps;
                n = put(points, n, x, y, front + MARGIN);
                n = put(points, n, x, y, back - MARGIN);
            }
            double y = top * i / steps;
            n = put(points, n, side + MARGIN, y, middle);
            n = put(points, n, -side - MARGIN, y, middle);
            n = put(points, n, x, top + MARGIN, middle);
        }
        return points;
    }

    private static int put(double[] points, int n, double x, double y, double z) {
        points[n] = x;
        points[n + 1] = y;
        points[n + 2] = z;
        return n + 3;
    }

    // Rings down the forearm, every step and at its end: how far from the wrist, and how thick there.
    private static double[][] forearm(double step) {
        double end = FOREARM_SHAPE[FOREARM_SHAPE.length - 1][0];
        int count = (int) Math.ceil(end / step) + 1;
        double[][] rings = new double[count][];
        for (int i = 0; i < count; i++) {
            double d = Math.min(end, i * step);
            double r = FOREARM_SHAPE[FOREARM_SHAPE.length - 1][1];
            for (int k = 1; k < FOREARM_SHAPE.length; k++) {
                if (d <= FOREARM_SHAPE[k][0]) {
                    double u = (d - FOREARM_SHAPE[k - 1][0]) / (FOREARM_SHAPE[k][0] - FOREARM_SHAPE[k - 1][0]);
                    r = Mth.lerp(u, FOREARM_SHAPE[k - 1][1], FOREARM_SHAPE[k][1]);
                    break;
                }
            }
            rings[i] = new double[] { d, r };
        }
        return rings;
    }

    static void clear(Level level, int id, int variant, double clock, double reach, HandPose pose, Vec3 base,
            Vec3 facing, double scale, Vec3 root) {
        Memory memory = memory(id, variant, facing, scale, level.getGameTime());
        double most = FREE;
        int now = (int) Math.floor(clock);
        int budget = BUDGET;
        for (int k = 0; k <= AHEAD + 1; k++) {
            for (int way = 1; way >= -1; way -= 2) {
                int tick = now + way * k;
                if (k == 0 && way < 0 || way < 0 && k > AHEAD || tick < 0 || tick >= memory.most.length) {
                    continue;
                }
                if (Double.isNaN(memory.most[tick])) {
                    if (budget == 0) {
                        continue;
                    }
                    budget--;
                    memory.most[tick] = most(level, HandPose.at(variant, tick, reach), base, facing, scale, root);
                }
                most = Math.min(most, memory.most[tick] + RATE * Math.abs(tick - clock));
            }
        }
        // This frame's own pose last: the fingers it bends are the ones the painter draws next.
        most = Math.min(most, most(level, pose, base, facing, scale, root));
        double since = clock - memory.lastClock;
        if (since >= 0.0 && since < AHEAD) {
            most = Math.min(most, memory.lastMost + RATE * since);
        }
        memory.lastClock = clock;
        memory.lastMost = most;
        if (most < 1.0) {
            hold(pose, pose.lean, pose.flex, most);
        }
    }

    // The pose held back by s: as it moves at 1; at 0 its wrist straight and its arm leant back past upright, away
    // from where it strikes.
    private static void hold(HandPose pose, double lean, double flex, double s) {
        pose.lean = Mth.lerp(s, -BACK, lean);
        pose.flex = flex * s;
    }

    private static Memory memory(int id, int variant, Vec3 facing, double scale, long time) {
        if (Math.abs(time - sweptAt) > FORGET) {
            sweptAt = time;
            MEMORY.values().removeIf(memory -> Math.abs(time - memory.used) > FORGET);
        }
        Memory memory = MEMORY.computeIfAbsent(id, key -> new Memory());
        int ticks = HandPose.life(variant) + 1;
        if (memory.variant != variant || memory.scale != scale || memory.most.length != ticks
                || memory.facing.distanceToSqr(facing) > MOVED * MOVED) {
            memory.variant = variant;
            memory.scale = scale;
            memory.facing = facing;
            if (memory.most.length != ticks) {
                memory.most = new double[ticks];
            }
            Arrays.fill(memory.most, Double.NaN);
        }
        memory.used = time;
        return memory;
    }

    // How far this pose may go, from 0 (held back all the way) to 1 (as it moves), with arm, hand and fingers clear:
    // FREE when it is clear as it is, or when even held back all the way it would not be.
    private static double most(Level level, HandPose pose, Vec3 base, Vec3 facing, double scale, Vec3 root) {
        double lean = pose.lean;
        double flex = pose.flex;
        fingers(pose, pose.place(base, facing, scale));
        try {
            if (hits(level, pose, base, facing, scale, root, 1) == 0) {
                return FREE;
            }
            hold(pose, lean, flex, 0.0);
            if (hits(level, pose, base, facing, scale, root, 1) > 0) {
                return FREE;
            }
            double low = 0.0;
            double high = 1.0;
            for (int i = 0; i < REFINE; i++) {
                double mid = (low + high) * 0.5;
                hold(pose, lean, flex, mid);
                if (hits(level, pose, base, facing, scale, root, 1) > 0) {
                    high = mid;
                } else {
                    low = mid;
                }
            }
            return low;
        } finally {
            pose.lean = lean;
            pose.flex = flex;
        }
    }

    // Every finger's surface, round each joint, middle and tip, in the hand's own frame as drawn: the fingers bend
    // the same however the arm goes.
    private static void fingers(HandPose pose, HandPose.Place place) {
        ConstructPainter.Frame[] bones = HandRig.frames(HandPainter.handFrame(place, false), pose, null);
        Vec3 wrist = place.wrist();
        double s = place.scale();
        int n = 0;
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 3; j++) {
                ConstructPainter.Frame bone = bones[HandRig.bone(k, j)];
                double length = k < 4 ? HandRig.JOINTS[k][j] : HandRig.THUMB[j];
                double thick = (k < 4 ? HandRig.THICK[k] : HandRig.THUMB_THICK[j]) + MARGIN;
                for (int a = 0; a < (j == 2 ? 3 : 2); a++) {
                    double y = ALONG[a] * length;
                    for (int r = 0; r < FINGER_ROUND; r++) {
                        double angle = Math.PI * 2.0 * r / FINGER_ROUND;
                        Vec3 d = bone.at(Math.cos(angle) * thick, y, Math.sin(angle) * thick).subtract(wrist);
                        n = put(FINGERS, n, d.dot(place.right()) / s, d.dot(place.up()) / s, d.dot(place.forward()) / s);
                    }
                }
            }
        }
    }

    // How many of the points on the arm, palm and fingers lie in blocks, counting no further than limit.
    private static int hits(Level level, HandPose pose, Vec3 base, Vec3 facing, double scale, Vec3 root, int limit) {
        HandPose.Place place = pose.place(base, facing, scale);
        int in = forearm(level, place, base, root, limit);
        in += points(level, place, PALM, base, root, limit - in);
        return in >= limit ? in : in + points(level, place, FINGERS, base, root, limit - in);
    }

    private static int points(Level level, HandPose.Place place, double[] points, Vec3 base, Vec3 root, int limit) {
        double s = place.scale();
        Vec3 w = place.wrist();
        Vec3 r = place.right();
        Vec3 u = place.up();
        Vec3 f = place.forward();
        int in = 0;
        for (int n = 0; n < points.length && in < limit; n += 3) {
            double x = points[n];
            double y = points[n + 1];
            double z = points[n + 2];
            if (point(level, w.x + (r.x * x + u.x * y + f.x * z) * s, w.y + (r.y * x + u.y * y + f.y * z) * s,
                    w.z + (r.z * x + u.z * y + f.z * z) * s, base, root)) {
                in++;
            }
        }
        return in;
    }

    // The forearm as rings along it, each the oval it is drawn as; a ring wholly in what it rises out of is skipped.
    private static int forearm(Level level, HandPose.Place place, Vec3 base, Vec3 root, int limit) {
        Vec3 arm = place.arm();
        Vec3 forward = place.armForward();
        Vec3 right = forward.cross(arm);
        right = right.lengthSqr() < 1.0E-8 ? place.right() : right.normalize();
        double s = place.scale();
        Vec3 wrist = place.wrist();
        int in = 0;
        for (double[] ring : FOREARM) {
            double cx = wrist.x - arm.x * ring[0] * s;
            double cy = wrist.y - arm.y * ring[0] * s;
            double cz = wrist.z - arm.z * ring[0] * s;
            double wide = (ring[1] * WIDE + MARGIN) * s;
            double flat = (ring[1] * FLAT + MARGIN) * s;
            if ((cx - base.x) * root.x + (cy - base.y) * root.y + (cz - base.z) * root.z + wide <= ROOTED) {
                continue;
            }
            for (int k = 0; k < ROUND && in < limit; k++) {
                double a = Math.PI * 2.0 * k / ROUND;
                double across = Math.cos(a) * wide;
                double ahead = Math.sin(a) * flat;
                if (point(level, cx + right.x * across + forward.x * ahead, cy + right.y * across + forward.y * ahead,
                        cz + right.z * across + forward.z * ahead, base, root)) {
                    in++;
                }
            }
            if (in >= limit) {
                return in;
            }
        }
        return in;
    }

    private static boolean point(Level level, double x, double y, double z, Vec3 base, Vec3 root) {
        return (x - base.x) * root.x + (y - base.y) * root.y + (z - base.z) * root.z > ROOTED
                && Solid.firm(level, x, y, z);
    }
}
