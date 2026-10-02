package nl.tivek.multiversepowers.character.greenlantern.client.mech.touch;

import javax.annotation.Nullable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.rig.RigFrames;
import nl.tivek.multiversepowers.engine.math.Sdf;
import nl.tivek.multiversepowers.engine.math.Segments;
import nl.tivek.multiversepowers.engine.rig.Joint;
import nl.tivek.multiversepowers.engine.rig.Rig;
import nl.tivek.multiversepowers.engine.rig.RigPose;
import nl.tivek.multiversepowers.engine.rig.RigSpace;
import nl.tivek.multiversepowers.engine.rig.Settle;
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes.KNUCKLES;
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes.WRIST;

// The mech's hands on bones: four fingers of three joints and a thumb, the left hand the mirror of the right. The
// fingers close into a fist resting on the palm and the thumb lies against them, never in them.
public final class MechHandRig {
    public static final double[] FINGER_X = { 0.47, 0.16, -0.16, -0.47 };
    public static final double[][] FINGER_LENGTHS = { { 0.5, 0.42, 0.36 }, { 0.55, 0.45, 0.38 }, { 0.5, 0.42, 0.36 },
            { 0.42, 0.36, 0.3 } };
    static final Vec3 THUMB_ROOT = new Vec3(0.62, WRIST + 0.36, 0.1);
    public static final double[] THUMB_LENGTHS = { 0.5, 0.42, 0.34 };
    static final double[] BENDS = { 0.95, 1.1, 0.85 };
    static final double[] SPREADS = { -0.2, -0.06, 0.08, 0.22 };
    static final Rig RIGHT = rig(1.0, true);
    static final Rig LEFT = rig(-1.0, true);
    static final Rig FREE_RIGHT = rig(1.0, false);
    static final Rig FREE_LEFT = rig(-1.0, false);
    static final int THUMB_BONE = 12;
    public static final double TOUCH = 0.02;
    // A hand curled this far or more grips: it closes on what it touches.
    static final double GRIP = 0.65;
    private static final int STEPS = 12;
    // The palm block with the plate on its inside, and each finger block's half thickness.
    private static final double[] PALM = { 0.0, (WRIST - 0.05 + KNUCKLES) * 0.5, 0.04, 0.64,
            (KNUCKLES - WRIST + 0.05) * 0.5, 0.32, 0.08 };
    private static final double FINGER = 0.15;
    private static final double THUMB = 0.17;
    // A finger's three bending turns (the knuckle's is its second) and where each sits open.
    private static final int[] BEND_TURNS = { 1, 0, 0 };
    private static final double[] OPEN = { 0.0, 0.0, 0.0 };

    private MechHandRig() {
    }

    public static int bone(int k, int j) {
        return k < 4 ? k * 3 + j : THUMB_BONE + j;
    }

    private static Rig rig(double flip, boolean limited) {
        Rig.Builder builder = Rig.builder();
        for (int k = 0; k < 4; k++) {
            int knuckle = builder.bone("finger" + k, -1, flip * FINGER_X[k], KNUCKLES, 0.0,
                    new Joint(turn(Joint.Axis.Z, limited, -0.4, 0.4), turn(Joint.Axis.X, limited, -0.2, 1.7)));
            int middle = builder.bone("finger" + k + "_middle", knuckle, 0.0, FINGER_LENGTHS[k][0], 0.0,
                    new Joint(turn(Joint.Axis.X, limited, 0.0, 1.9)));
            builder.bone("finger" + k + "_tip", middle, 0.0, FINGER_LENGTHS[k][1], 0.0,
                    new Joint(turn(Joint.Axis.X, limited, 0.0, 1.6)));
        }
        int root = builder.bone("thumb", -1, flip * THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z,
                new Joint(turn(Joint.Axis.Z, limited, -1.2, 1.2), turn(Joint.Axis.X, limited, 0.0, 1.2)));
        int middle = builder.bone("thumb_middle", root, 0.0, THUMB_LENGTHS[0], 0.0,
                new Joint(turn(Joint.Axis.X, limited, 0.0, 1.2)));
        builder.bone("thumb_tip", middle, 0.0, THUMB_LENGTHS[1], 0.0, new Joint(turn(Joint.Axis.X, limited, 0.0,
                1.2)));
        return builder.build();
    }

    private static Joint.Turn turn(Joint.Axis axis, boolean limited, double min, double max) {
        return limited ? new Joint.Turn(axis, min, max) : Joint.free(axis);
    }

    // The angles the arm asks for, worked out exactly as the fingers were always drawn.
    static RigPose angles(MechMoves.Arm arm, boolean right, Rig rig) {
        double flip = right ? 1.0 : -1.0;
        RigPose angles = new RigPose(rig);
        for (int k = 0; k < 4; k++) {
            angles.set(bone(k, 0), 0, flip * SPREADS[k] * arm.spread());
            for (int j = 0; j < 3; j++) {
                angles.set(bone(k, j), j == 0 ? 1 : 0, 0.08 + arm.curl() * BENDS[j] * 1.45);
            }
        }
        angles.set(THUMB_BONE, 0, flip * (-0.85 + 0.45 * arm.curl()));
        angles.set(THUMB_BONE, 1, 0.35 + 0.5 * arm.curl());
        for (int j = 1; j < 3; j++) {
            angles.set(THUMB_BONE + j, 0, 0.12 + arm.curl() * 0.7);
        }
        return angles;
    }

    public static Frame[] frames(Frame hand, MechMoves.Arm arm, boolean right) {
        return frames(hand, arm, right, null, null, null);
    }

    // With a wall (see wall()), no finger or thumb reaches past the plane between two clapping hands; with a held
    // creature's box (in the world), each opens only as far as it takes to rest on its skin; with the ground round
    // the hand, none goes into it (see posed()).
    public static Frame[] frames(Frame hand, MechMoves.Arm arm, boolean right, @Nullable double[] wall,
            @Nullable AABB held, @Nullable Ground ground) {
        Rig rig = right ? RIGHT : LEFT;
        Frame[] frames = new Frame[rig.size()];
        RigFrames.pose(rig, posed(hand, arm, right, wall, held, ground), hand, frames, true);
        return frames;
    }

    // How deep a ball of `radius` at a point in the world lies in the blocks round a hand: 0 or less when clear.
    @FunctionalInterface
    public interface Ground {
        double depth(double x, double y, double z, double radius);
    }

    // The fingers' angles, settled. Against the ground each finger and the thumb open, from the tip back, only as far
    // as it takes to rest on it; a gripping hand (curled past GRIP) that would close on the ground closes all the way
    // first, so its fingers wrap round the ledge or edge it holds instead of standing stiff.
    static RigPose posed(Frame hand, MechMoves.Arm arm, boolean right, @Nullable double[] wall, @Nullable AABB held,
            @Nullable Ground ground) {
        return posed(hand, arm, right, wall, held, ground, settled(arm, right));
    }

    // As above, from the arm's pose already settled.
    static RigPose posed(Frame hand, MechMoves.Arm arm, boolean right, @Nullable double[] wall, @Nullable AABB held,
            @Nullable Ground ground, RigPose settled) {
        Rig rig = right ? RIGHT : LEFT;
        RigPose pose = new RigPose(rig);
        pose.copy(settled);
        if (ground != null && arm.curl() >= GRIP && arm.curl() < 1.0) {
            RigPose fist = settled(fist(arm), right);
            double[] space = RigSpace.create(rig);
            RigSpace.pose(rig, fist, space);
            if (groundDepth(space, hand, ground) > TOUCH) {
                pose = fist;
            }
        }
        if (wall != null) {
            stopAt(pose, rig, right, wall);
        }
        if (held != null) {
            stopOn(pose, rig, hand, held);
        }
        if (ground != null) {
            stopIn(pose, rig, hand, right, ground);
        }
        return pose;
    }

    // How deep the posed fingers and thumb still go into the ground: 0 or less once they rest on it.
    public static double rest(Frame hand, MechMoves.Arm arm, boolean right, Ground ground) {
        Rig rig = right ? RIGHT : LEFT;
        double[] space = RigSpace.create(rig);
        RigSpace.pose(rig, posed(hand, arm, right, null, null, ground), space);
        return groundDepth(space, hand, ground);
    }

    // Each finger of `shown` that goes into the ground, the held creature's box or past the wall takes the angles
    // `posed` has for it, which rest on them.
    static void keepOut(RigPose shown, RigPose posed, Frame hand, boolean right, @Nullable double[] wall,
            @Nullable AABB held, @Nullable Ground ground) {
        Rig rig = right ? RIGHT : LEFT;
        double[] space = RigSpace.create(rig);
        RigSpace.pose(rig, shown, space);
        for (int k = 0; k < 5; k++) {
            double deepest = Double.NEGATIVE_INFINITY;
            if (ground != null) {
                deepest = Math.max(deepest, fingerDepth(space, hand, ground, k, 0));
            }
            if (held != null) {
                deepest = Math.max(deepest, heldDepth(space, k, 0, hand, held));
            }
            if (wall != null) {
                deepest = Math.max(deepest, wallDepth(space, wall, k, 0, k < 4 ? FINGER : THUMB));
            }
            if (deepest <= TOUCH + 0.01) {
                continue;
            }
            for (int j = 0; j < 3; j++) {
                int b = bone(k, j);
                for (int t = 0; t < rig.joint(b).size(); t++) {
                    shown.set(b, t, posed.get(b, t));
                }
            }
        }
    }

    private static MechMoves.Arm fist(MechMoves.Arm arm) {
        return new MechMoves.Arm(arm.elbow(), arm.way(), arm.palm(), 1.0, arm.spread(), arm.upper(), arm.fold(),
                arm.tilt());
    }

    // Each finger, from its tip back, and the thumb open only as far as it takes to rest on the ground; a finger that
    // cannot get out that way relaxes as a whole.
    private static void stopIn(RigPose angles, Rig rig, Frame hand, boolean right, Ground ground) {
        double[] space = RigSpace.create(rig);
        RigSpace.pose(rig, angles, space);
        if (groundDepth(space, hand, ground) <= TOUCH) {
            return;
        }
        for (int k = 0; k < 4; k++) {
            int finger = k;
            for (int j = 2; j >= 0; j--) {
                int from = j;
                Settle.back(rig, angles, bone(k, j), BEND_TURNS[j], 0.0, space,
                        s -> fingerDepth(s, hand, ground, finger, from), TOUCH, STEPS);
            }
            Settle.ease(rig, angles, new int[] { bone(k, 0), bone(k, 1), bone(k, 2) }, BEND_TURNS, OPEN, space,
                    s -> fingerDepth(s, hand, ground, finger, 0), TOUCH, STEPS);
        }
        for (int j = 2; j >= 1; j--) {
            int from = j;
            Settle.back(rig, angles, bone(4, j), 0, 0.0, space, s -> fingerDepth(s, hand, ground, 4, from), TOUCH,
                    STEPS);
        }
        Settle.Depth thumb = s -> fingerDepth(s, hand, ground, 4, 0);
        Settle.back(rig, angles, THUMB_BONE, 1, 0.0, space, thumb, TOUCH, STEPS);
        Settle.back(rig, angles, THUMB_BONE, 0, right ? -1.2 : 1.2, space, thumb, TOUCH, STEPS);
        Settle.ease(rig, angles, new int[] { THUMB_BONE, THUMB_BONE, bone(4, 1), bone(4, 2) },
                new int[] { 0, 1, 0, 0 }, new double[] { right ? -1.2 : 1.2, 0.0, 0.0, 0.0 }, space, thumb, TOUCH,
                STEPS);
    }

    private static double groundDepth(double[] space, Frame hand, Ground ground) {
        double deepest = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < 5; k++) {
            deepest = Math.max(deepest, fingerDepth(space, hand, ground, k, 0));
        }
        return deepest;
    }

    // How deep a finger's bones from `from` to its tip go into the ground, their thickness counted.
    private static double fingerDepth(double[] space, Frame hand, Ground ground, int k, int from) {
        double radius = (k < 4 ? FINGER : THUMB) * hand.scale();
        double s = hand.scale();
        Vec3 c = hand.center();
        Vec3 r = hand.right();
        Vec3 u = hand.up();
        Vec3 f = hand.forward();
        double deepest = Double.NEGATIVE_INFINITY;
        for (int j = from; j < 3; j++) {
            int o = bone(k, j) * RigSpace.STRIDE;
            double length = length(k, j);
            for (int i = 0; i <= 4; i++) {
                double along = length * i / 4.0;
                double x = (space[o] + space[o + 6] * along) * s;
                double y = (space[o + 1] + space[o + 7] * along) * s;
                double z = (space[o + 2] + space[o + 8] * along) * s;
                deepest = Math.max(deepest, ground.depth(c.x + r.x * x + u.x * y + f.x * z,
                        c.y + r.y * x + u.y * y + f.y * z, c.z + r.z * x + u.z * y + f.z * z, radius));
            }
        }
        return deepest / s;
    }

    private static void stopOn(RigPose angles, Rig rig, Frame hand, AABB held) {
        double[] space = RigSpace.create(rig);
        for (int k = 0; k < 5; k++) {
            int finger = k;
            for (int j = 2; j >= 0; j--) {
                int from = j;
                int turn = k < 4 ? BEND_TURNS[j] : j == 0 ? 1 : 0;
                Settle.back(rig, angles, bone(k, j), turn, 0.0, space, s -> heldDepth(s, finger, from, hand, held),
                        TOUCH, STEPS);
            }
        }
    }

    // How deep a finger's bones from `from` to its tip go into the held creature's box, their thickness counted.
    private static double heldDepth(double[] space, int k, int from, Frame hand, AABB held) {
        double radius = k < 4 ? FINGER : THUMB;
        Vec3 c = hand.center();
        Vec3 r = hand.right();
        Vec3 u = hand.up();
        Vec3 f = hand.forward();
        double s = hand.scale();
        double hx = held.getXsize() * 0.5;
        double hy = held.getYsize() * 0.5;
        double hz = held.getZsize() * 0.5;
        Vec3 middle = held.getCenter();
        double deepest = Double.NEGATIVE_INFINITY;
        for (int j = from; j < 3; j++) {
            int o = bone(k, j) * RigSpace.STRIDE;
            double length = length(k, j);
            for (int i = 0; i <= 4; i++) {
                double along = length * i / 4.0;
                double x = (space[o] + space[o + 6] * along) * s;
                double y = (space[o + 1] + space[o + 7] * along) * s;
                double z = (space[o + 2] + space[o + 8] * along) * s;
                double box = Sdf.box(c.x + r.x * x + u.x * y + f.x * z - middle.x,
                        c.y + r.y * x + u.y * y + f.y * z - middle.y, c.z + r.z * x + u.z * y + f.z * z - middle.z,
                        hx, hy, hz);
                deepest = Math.max(deepest, radius * s - box);
            }
        }
        return deepest / s;
    }

    // The plane through `at` facing `side` (world), as a signed distance over the hand's own places: a, x, y, z with
    // distance = a + x*px + y*py + z*pz, positive on the hand's side.
    public static double[] wall(Frame hand, Vec3 at, Vec3 side) {
        return new double[] { hand.center().subtract(at).dot(side), hand.right().dot(side) * hand.scale(),
                hand.up().dot(side) * hand.scale(), hand.forward().dot(side) * hand.scale() };
    }

    // Each finger, from its tip back, and the thumb open only as far as it takes to stay on the hand's side of the
    // wall, so two clapping hands press against each other instead of through.
    private static void stopAt(RigPose angles, Rig rig, boolean right, double[] wall) {
        double[] space = RigSpace.create(rig);
        for (int k = 0; k < 4; k++) {
            int finger = k;
            for (int j = 2; j >= 0; j--) {
                int from = j;
                Settle.back(rig, angles, bone(k, j), j == 0 ? 1 : 0, 0.0, space,
                        s -> wallDepth(s, wall, finger, from, FINGER), TOUCH, STEPS);
            }
            Settle.ease(rig, angles, new int[] { bone(k, 0), bone(k, 1), bone(k, 2) }, BEND_TURNS, OPEN, space,
                    s -> wallDepth(s, wall, finger, 0, FINGER), TOUCH, STEPS);
        }
        Settle.Depth thumb = s -> wallDepth(s, wall, 4, 0, THUMB);
        for (int j = 2; j >= 1; j--) {
            Settle.back(rig, angles, bone(4, j), 0, 0.0, space, thumb, TOUCH, STEPS);
        }
        Settle.back(rig, angles, THUMB_BONE, 1, 0.0, space, thumb, TOUCH, STEPS);
        Settle.back(rig, angles, THUMB_BONE, 0, right ? -1.2 : 1.2, space, thumb, TOUCH, STEPS);
        Settle.ease(rig, angles, new int[] { THUMB_BONE, THUMB_BONE, bone(4, 1), bone(4, 2) },
                new int[] { 0, 1, 0, 0 }, new double[] { right ? -1.2 : 1.2, 0.0, 0.0, 0.0 }, space, thumb, TOUCH,
                STEPS);
    }

    // How far the finger's bones from `from` to its tip reach past the wall, their thickness counted.
    private static double wallDepth(double[] space, double[] wall, int k, int from, double radius) {
        double deepest = Double.NEGATIVE_INFINITY;
        for (int j = from; j < 3; j++) {
            int o = bone(k, j) * RigSpace.STRIDE;
            double length = length(k, j);
            for (int i = 0; i <= 4; i++) {
                double u = length * i / 4.0;
                double x = space[o] + space[o + 6] * u;
                double y = space[o + 1] + space[o + 7] * u;
                double z = space[o + 2] + space[o + 8] * u;
                deepest = Math.max(deepest, radius - (wall[0] + wall[1] * x + wall[2] * y + wall[3] * z));
            }
        }
        return deepest;
    }

    static RigPose settled(MechMoves.Arm arm, boolean right) {
        Rig rig = right ? RIGHT : LEFT;
        RigPose angles = angles(arm, right, rig);
        double[] space = RigSpace.create(rig);
        double[] a0 = new double[3];
        double[] a1 = new double[3];
        double[] b0 = new double[3];
        double[] b1 = new double[3];
        double[] out = new double[2];
        // From the tip back to the knuckle, each joint opens only as far as it takes to lift what follows off the palm;
        // a finger that cannot get out that way relaxes as a whole.
        for (int k = 0; k < 4; k++) {
            int finger = k;
            for (int j = 2; j >= 0; j--) {
                int from = Math.max(j, 1);
                Settle.back(rig, angles, bone(k, j), j == 0 ? 1 : 0, 0.0, space, s -> Math.max(palmDepth(s, finger,
                        from), from < 2 ? palmDepth(s, finger, 2) : -1.0), TOUCH, STEPS);
            }
            Settle.ease(rig, angles, new int[] { bone(k, 0), bone(k, 1), bone(k, 2) }, BEND_TURNS, OPEN, space,
                    s -> Math.max(palmDepth(s, finger, 1), palmDepth(s, finger, 2)), TOUCH, STEPS);
        }
        Settle.Depth thumb = s -> thumbDepth(s, a0, a1, b0, b1, out);
        for (int j = 2; j >= 1; j--) {
            Settle.back(rig, angles, bone(4, j), 0, 0.0, space, thumb, TOUCH, STEPS);
        }
        Settle.back(rig, angles, THUMB_BONE, 1, 0.0, space, thumb, TOUCH, STEPS);
        Settle.back(rig, angles, THUMB_BONE, 0, right ? -1.2 : 1.2, space, thumb, TOUCH, STEPS);
        return angles;
    }

    public static double length(int k, int j) {
        return k < 4 ? FINGER_LENGTHS[k][j] : THUMB_LENGTHS[j];
    }

    static double palmDepth(double[] space, int k, int j) {
        int o = bone(k, j) * RigSpace.STRIDE;
        double length = length(k, j);
        double deepest = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= 4; i++) {
            double u = length * i / 4.0;
            double x = space[o] + space[o + 6] * u;
            double y = space[o + 1] + space[o + 7] * u;
            double z = space[o + 2] + space[o + 8] * u;
            double palm = Sdf.roundBox(x - PALM[0], y - PALM[1], z - PALM[2], PALM[3], PALM[4], PALM[5], PALM[6]);
            deepest = Math.max(deepest, FINGER - palm);
        }
        return deepest;
    }

    static double thumbDepth(double[] space, double[] a0, double[] a1, double[] b0, double[] b1, double[] out) {
        double deepest = Double.NEGATIVE_INFINITY;
        for (int t = 1; t < 3; t++) {
            RigSpace.point(space, bone(4, t), 0.0, 0.0, 0.0, a0);
            RigSpace.point(space, bone(4, t), 0.0, length(4, t), 0.0, a1);
            for (int k = 0; k < 4; k++) {
                for (int f = 0; f < 3; f++) {
                    RigSpace.point(space, bone(k, f), 0.0, 0.0, 0.0, b0);
                    RigSpace.point(space, bone(k, f), 0.0, length(k, f), 0.0, b1);
                    deepest = Math.max(deepest, THUMB + FINGER - Math.sqrt(Segments.closest(a0, a1, b0, b1, out)));
                }
            }
        }
        return deepest;
    }
}
