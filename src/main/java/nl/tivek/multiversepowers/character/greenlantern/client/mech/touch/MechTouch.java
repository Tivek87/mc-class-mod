package nl.tivek.multiversepowers.character.greenlantern.client.mech.touch;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.world.LevelBlocks;
import nl.tivek.multiversepowers.engine.math.Sdf;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes.KNUCKLES;
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes.WRIST;

// The mech's arms against the blocks round them, as this player's own game has them: an arm never passes into them.
// Its elbow folds, and past that its upper arm lifts, just as far as it takes for the forearm and hand to stay out,
// and it eases back to how it swings only slowly after. Its fingers (MechHandRig) rest on what they touch.
public final class MechTouch {
    // A point counts as in the blocks this far in: a hand laid on the ground is on it, not in it.
    static final double TOUCH = 0.04;
    private static final int MOST_BOXES = 256;
    // Room round the arm the blocks are gathered in: its thickness and its fingers past the palm.
    private static final double ROOM = 2.2;
    // How far the elbow may fold (radians, never closer than FOLDED to the upper arm) and the arm lift at the
    // shoulder past that, and how fast the arm eases back out (a share of all of that a tick).
    private static final double MOST_FOLD = 1.6;
    private static final double FOLDED = 0.5;
    private static final double MOST_LIFT = 1.4;
    private static final double RELAX = 0.06;
    private static final int STEPS = 12;
    // Held all the way, it must come this much further out to be worth it, and may stay in this much past that.
    private static final double LEEWAY = 0.05;
    // Points on the forearm, palm and knuckles in the hand's frame (across, along the forearm, palm side) and how
    // thick it is round each.
    private static final double[][] ARM = {
            { 0.0, 0.35, 0.0, 0.78 },
            { 0.0, 1.2, 0.0, 0.82 },
            { 0.0, WRIST - 0.25, 0.0, 0.6 },
            { 0.45, WRIST + 0.35, 0.0, 0.3 },
            { -0.45, WRIST + 0.35, 0.0, 0.3 },
            { 0.45, KNUCKLES - 0.1, 0.0, 0.3 },
            { -0.45, KNUCKLES - 0.1, 0.0, 0.3 } };
    private static final LevelBlocks BLOCKS = new LevelBlocks();
    private static final double[] BOXES = new double[MOST_BOXES * 6];
    private static int boxes;
    // How far each arm (mech id and side) was held out of the blocks, and at what time of its build.
    private static final Long2DoubleOpenHashMap HELD = new Long2DoubleOpenHashMap();
    private static final Long2DoubleOpenHashMap HELD_AT = new Long2DoubleOpenHashMap();

    private MechTouch() {
    }

    // Gathers the blocks within `reach` of `at`; false when there are none.
    public static boolean near(Level level, Vec3 at, double reach) {
        boxes = BLOCKS.in(level).collect(at.x - reach, at.y - reach, at.z - reach, at.x + reach, at.y + reach,
                at.z + reach, BOXES);
        return boxes > 0;
    }

    // Gathers the blocks round a shoulder, its hand and elbow, with room for the hand as it folds and the fingers.
    private static boolean near(Level level, Vec3 shoulder, Vec3 hand, Vec3 elbow) {
        double room = ROOM;
        boxes = BLOCKS.in(level).collect(Math.min(shoulder.x, Math.min(hand.x, elbow.x)) - room,
                Math.min(shoulder.y, Math.min(hand.y, elbow.y)) - room,
                Math.min(shoulder.z, Math.min(hand.z, elbow.z)) - room,
                Math.max(shoulder.x, Math.max(hand.x, elbow.x)) + room,
                Math.max(shoulder.y, Math.max(hand.y, elbow.y)) + room,
                Math.max(shoulder.z, Math.max(hand.z, elbow.z)) + room, BOXES);
        return boxes > 0;
    }

    // How deep a ball of `radius` at (x, y, z) lies in the blocks gathered last: 0 or less when clear.
    public static double depth(double x, double y, double z, double radius) {
        double deepest = Double.NEGATIVE_INFINITY;
        for (int b = 0; b < boxes; b++) {
            int o = b * 6;
            double hx = (BOXES[o + 3] - BOXES[o]) * 0.5;
            double hy = (BOXES[o + 4] - BOXES[o + 1]) * 0.5;
            double hz = (BOXES[o + 5] - BOXES[o + 2]) * 0.5;
            double d = Sdf.box(x - BOXES[o] - hx, y - BOXES[o + 1] - hy, z - BOXES[o + 2] - hz, hx, hy, hz);
            deepest = Math.max(deepest, radius - d);
        }
        return deepest;
    }

    // The arm of mech `id`, `t` ticks into its build, kept out of the blocks: folded at the elbow and lifted at the
    // shoulder no further than it takes, and never let back faster than it relaxes.
    public static MechMoves.Arm clear(Level level, int id, MechScript.Stage torso, MechMoves.Arm arm, boolean right,
            double t) {
        Vec3 shoulder = MechPainter.side(MechScript.SHOULDER, right);
        long key = (long) id << 1 | (right ? 1L : 0L);
        double was = HELD.getOrDefault(key, 0.0);
        double since = Math.max(0.0, t - HELD_AT.getOrDefault(key, t));
        double least = Math.max(0.0, was - RELAX * since);
        HELD_AT.put(key, t);
        if (!near(level, torso.point(shoulder), torso.point(arm.hand()), torso.point(arm.elbow()))) {
            HELD.put(key, least);
            return least <= 0.0 ? arm : held(arm, shoulder, least);
        }
        double need = 0.0;
        double now = deepest(torso, held(arm, shoulder, least));
        if (now > TOUCH) {
            // How far it must go, the fold (0 to 1) then the lift (1 to 2), found by halving: to where it is out, or
            // as far out as going all the way gets it. Held all the way and no less in, it is left as it swings:
            // stuck at the shoulder, folding it would only twist it.
            double most = deepest(torso, held(arm, shoulder, 2.0));
            double enough = Math.max(TOUCH, most + LEEWAY);
            if (most < now - LEEWAY) {
                double free = 2.0;
                double stuck = least;
                for (int s = 0; s < STEPS; s++) {
                    double middle = (free + stuck) * 0.5;
                    if (deepest(torso, held(arm, shoulder, middle)) > enough) {
                        stuck = middle;
                    } else {
                        free = middle;
                    }
                }
                need = free;
            }
        }
        double amount = Math.max(least, need);
        HELD.put(key, amount);
        return amount <= 0.0 ? arm : held(arm, shoulder, amount);
    }

    // The arm folded at the elbow by `amount` (0 to 1 of how far it may), then lifted at the shoulder (1 to 2).
    private static MechMoves.Arm held(MechMoves.Arm arm, Vec3 shoulder, double amount) {
        Vec3 elbow = arm.elbow();
        Vec3 way = arm.way();
        Vec3 palm = arm.palm();
        Vec3 up = shoulder.subtract(elbow).normalize();
        double open = Math.acos(Math.max(-1.0, Math.min(1.0, way.dot(up))));
        double fold = Math.min(1.0, amount) * Math.max(0.0, Math.min(MOST_FOLD, open - FOLDED));
        if (fold > 1.0E-6) {
            Vec3 axis = way.cross(up);
            axis = axis.lengthSqr() < 1.0E-8 ? Vectors.across(way)[0] : axis.normalize();
            way = Vectors.spin(way, axis, fold);
            palm = Vectors.spin(palm, axis, fold);
        }
        double lift = Math.max(0.0, amount - 1.0) * MOST_LIFT;
        if (lift > 1.0E-6) {
            // Up and ahead about the line across the shoulders, as an arm raised in front.
            Vec3 across = new Vec3(1.0, 0.0, 0.0);
            elbow = shoulder.add(Vectors.spin(elbow.subtract(shoulder), across, -lift));
            way = Vectors.spin(way, across, -lift);
            palm = Vectors.spin(palm, across, -lift);
        }
        return new MechMoves.Arm(elbow, way, palm, arm.curl(), arm.spread(), arm.upper());
    }

    // How deep the forearm, the hand or its knuckles lie in the blocks at most (0 or less: clear).
    private static double deepest(MechScript.Stage torso, MechMoves.Arm arm) {
        Frame hand = Frame.of(torso.point(arm.elbow()), torso.dir(arm.palm()), torso.dir(arm.way()), 1.0);
        double deepest = Double.NEGATIVE_INFINITY;
        for (double[] p : ARM) {
            Vec3 at = hand.at(p[0], p[1], p[2]);
            deepest = Math.max(deepest, depth(at.x, at.y, at.z, p[3]));
        }
        // Open fingers reach on past the knuckles.
        Vec3 tips = hand.at(0.0, KNUCKLES + 0.9 * (1.0 - arm.curl()), 0.25 * arm.curl());
        return Math.max(deepest, depth(tips.x, tips.y, tips.z, 0.25));
    }

    public static void forget(int id) {
        for (long side = 0; side < 2; side++) {
            HELD.remove((long) id << 1 | side);
            HELD_AT.remove((long) id << 1 | side);
        }
    }

    public static void clear() {
        HELD.clear();
        HELD_AT.clear();
    }
}
