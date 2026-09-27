package nl.tivek.multiversepowers.character.greenlantern.client.mech;

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
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.MechArmShapes.KNUCKLES;
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.MechArmShapes.WRIST;

// The mech's hands on bones: four fingers of three joints and a thumb, the left hand the mirror of the right. The
// fingers close into a fist resting on the palm and the thumb lies against them, never in them.
final class MechHandRig {
    static final double[] FINGER_X = { 0.47, 0.16, -0.16, -0.47 };
    static final double[][] FINGER_LENGTHS = { { 0.5, 0.42, 0.36 }, { 0.55, 0.45, 0.38 }, { 0.5, 0.42, 0.36 },
            { 0.42, 0.36, 0.3 } };
    static final Vec3 THUMB_ROOT = new Vec3(0.62, WRIST + 0.36, 0.1);
    static final double[] THUMB_LENGTHS = { 0.5, 0.42, 0.34 };
    static final double[] BENDS = { 0.95, 1.1, 0.85 };
    static final double[] SPREADS = { -0.2, -0.06, 0.08, 0.22 };
    static final Rig RIGHT = rig(1.0, true);
    static final Rig LEFT = rig(-1.0, true);
    static final Rig FREE_RIGHT = rig(1.0, false);
    static final Rig FREE_LEFT = rig(-1.0, false);
    static final int THUMB_BONE = 12;
    static final double TOUCH = 0.02;
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

    static int bone(int k, int j) {
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

    static Frame[] frames(Frame hand, MechMoves.Arm arm, boolean right) {
        Rig rig = right ? RIGHT : LEFT;
        Frame[] frames = new Frame[rig.size()];
        RigFrames.pose(rig, settled(arm, right), hand, frames, true);
        return frames;
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

    static double length(int k, int j) {
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
