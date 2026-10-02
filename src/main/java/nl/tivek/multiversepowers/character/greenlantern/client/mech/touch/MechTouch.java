package nl.tivek.multiversepowers.character.greenlantern.client.mech.touch;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechLegShapes;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.world.LevelBlocks;
import nl.tivek.multiversepowers.engine.math.Sdf;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.rig.Ik;
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
    // A knee swings out round its leg up to KNEE_OUT radians and in up to KNEE_IN, tried in so many steps each way,
    // and is let back RELAX_SWING radians a tick. The thigh, the shin and the cap in front of the knee are so thick.
    private static final double KNEE_OUT = 1.3;
    private static final double KNEE_IN = 0.35;
    private static final int KNEE_STEPS = 10;
    private static final double KNEE_ROOM = 2.4;
    private static final double RELAX_SWING = 0.05;
    private static final double THIGH_RADIUS = 0.78;
    private static final double SHIN_RADIUS = 0.62;
    private static final double CAP_AHEAD = 0.28;
    private static final double CAP_RADIUS = 0.6;
    private static final double[] SHIN_THIGH = { MechLegShapes.SHIN, MechLegShapes.THIGH };
    private static final LevelBlocks BLOCKS = new LevelBlocks();
    private static final double[] BOXES = new double[MOST_BOXES * 6];
    private static int boxes;
    // How far each arm (mech id and side) was held out of the blocks, and at what time of its build.
    private static final Long2DoubleOpenHashMap HELD = new Long2DoubleOpenHashMap();
    private static final Long2DoubleOpenHashMap HELD_AT = new Long2DoubleOpenHashMap();
    // How far each limb (mech id, two bits: arm or leg and side) is swung round its line out of the blocks, and when.
    private static final Long2DoubleOpenHashMap SWUNG = new Long2DoubleOpenHashMap();
    private static final Long2DoubleOpenHashMap SWUNG_AT = new Long2DoubleOpenHashMap();

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
        return new MechMoves.Arm(elbow, way, palm, arm.curl(), arm.spread(), arm.upper(), arm.fold(), arm.tilt());
    }

    // How deep the forearm, the hand or its knuckles lie in the blocks at most (0 or less: clear).
    private static double deepest(MechScript.Stage torso, MechMoves.Arm arm) {
        Frame forearm = MechArmRig.forearm(torso, arm);
        Frame hand = MechArmRig.wrist(forearm, arm.fold(), arm.tilt());
        double deepest = Double.NEGATIVE_INFINITY;
        for (double[] p : ARM) {
            Vec3 at = (p[1] < WRIST ? forearm : hand).at(p[0], p[1], p[2]);
            deepest = Math.max(deepest, depth(at.x, at.y, at.z, p[3]));
        }
        // Open fingers reach on past the knuckles.
        Vec3 tips = hand.at(0.0, KNUCKLES + 0.9 * (1.0 - arm.curl()), 0.25 * arm.curl());
        return Math.max(deepest, depth(tips.x, tips.y, tips.z, 0.25));
    }

    // The knee of mech `id`'s leg from `hip` to `ankle` (in the world), bent towards `forward`, kept out of the blocks:
    // swung round the line from the hip to the ankle, outwards (`out`) before inwards, just as far as it takes, and
    // eased back only slowly after.
    public static Vec3 knee(Level level, int id, boolean right, Vec3 hip, Vec3 ankle, Vec3 forward, Vec3 out,
            double t) {
        long key = (long) id << 2 | (right ? 2L : 3L);
        double[] root = { hip.x, hip.y, hip.z };
        double[] end = { ankle.x, ankle.y, ankle.z };
        double[] pole = { forward.x, forward.y, forward.z };
        double[] knee = new double[3];
        Vec3 axis = ankle.subtract(hip);
        // Which way a turn of the swivel takes the knee: outwards for a positive one.
        double sign = Math.signum(axis.cross(forward).dot(out));
        sign = sign == 0.0 ? 1.0 : sign;
        double need = 0.0;
        double reach = Math.max(KNEE_ROOM, axis.length() * 0.5 + KNEE_ROOM);
        Vec3 middle = hip.add(ankle).scale(0.5);
        if (near(level, middle, reach)) {
            double flip = sign;
            Ik.Clearance clearance = (x, y, z) -> legDepth(root, end, x, y, z);
            Ik.twoBone(root, end, pole, SHIN_THIGH[1], SHIN_THIGH[0], knee);
            double straight = clearance.depth(knee[0], knee[1], knee[2]);
            if (straight > TOUCH) {
                double found = Ik.swivel(root, end, pole, SHIN_THIGH[1], SHIN_THIGH[0],
                        flip > 0.0 ? -KNEE_IN : -KNEE_OUT, flip > 0.0 ? KNEE_OUT : KNEE_IN, KNEE_STEPS, clearance,
                        TOUCH, knee);
                double left = clearance.depth(knee[0], knee[1], knee[2]);
                // Turning it only helps where it comes out further than it was.
                need = left < straight - LEEWAY ? found * flip : 0.0;
            }
        }
        double swung = swung(key, need, t);
        Ik.twoBone(root, end, pole, SHIN_THIGH[1], SHIN_THIGH[0], swung * sign, knee);
        return new Vec3(knee[0], knee[1], knee[2]);
    }

    // How deep the thigh (hip to knee), the knee cap and the shin (knee to ankle) go into the blocks gathered last,
    // leaving out their ends at the hip and ankle, which no swing of the knee moves.
    private static double legDepth(double[] hip, double[] ankle, double x, double y, double z) {
        double deepest = Double.NEGATIVE_INFINITY;
        for (int i = 1; i <= 4; i++) {
            double u = i / 4.0;
            deepest = Math.max(deepest, depth(hip[0] + (x - hip[0]) * u, hip[1] + (y - hip[1]) * u,
                    hip[2] + (z - hip[2]) * u, THIGH_RADIUS));
            if (i < 4) {
                deepest = Math.max(deepest, depth(x + (ankle[0] - x) * u, y + (ankle[1] - y) * u,
                        z + (ankle[2] - z) * u, SHIN_RADIUS));
            }
        }
        // The cap stands out in front of the knee, the way it bends.
        double ax = ankle[0] - hip[0];
        double ay = ankle[1] - hip[1];
        double az = ankle[2] - hip[2];
        double length = ax * ax + ay * ay + az * az;
        double along = length < 1.0E-9 ? 0.0 : ((x - hip[0]) * ax + (y - hip[1]) * ay + (z - hip[2]) * az) / length;
        double bx = x - hip[0] - ax * along;
        double by = y - hip[1] - ay * along;
        double bz = z - hip[2] - az * along;
        double bend = Math.sqrt(bx * bx + by * by + bz * bz);
        if (bend > 1.0E-6) {
            double s = CAP_AHEAD / bend;
            deepest = Math.max(deepest, depth(x + bx * s, y + by * s, z + bz * s, CAP_RADIUS));
        }
        return deepest;
    }

    // A swing kept per limb: taken at once as far out as it is needed, let back towards what is needed at RELAX_SWING
    // radians a tick.
    private static double swung(long key, double need, double t) {
        double was = SWUNG.getOrDefault(key, 0.0);
        double since = Math.max(0.0, t - SWUNG_AT.getOrDefault(key, t));
        SWUNG_AT.put(key, t);
        double shown;
        if (need * was >= 0.0 && Math.abs(need) >= Math.abs(was)) {
            shown = need;
        } else {
            // Needed on the other side, it swings over twice as fast.
            double most = RELAX_SWING * since * (need * was < 0.0 ? 2.0 : 1.0);
            shown = was + Math.max(-most, Math.min(most, need - was));
        }
        SWUNG.put(key, shown);
        return shown;
    }

    public static void forget(int id) {
        for (long side = 0; side < 2; side++) {
            HELD.remove((long) id << 1 | side);
            HELD_AT.remove((long) id << 1 | side);
        }
        for (long limb = 0; limb < 4; limb++) {
            SWUNG.remove((long) id << 2 | limb);
            SWUNG_AT.remove((long) id << 2 | limb);
        }
        MechFingers.forget(id);
    }

    public static void clear() {
        HELD.clear();
        HELD_AT.clear();
        SWUNG.clear();
        SWUNG_AT.clear();
        MechFingers.clear();
    }
}
