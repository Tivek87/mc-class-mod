package nl.tivek.multiversepowers.character.greenlantern.client.render.hand;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.rig.RigFrames;
import nl.tivek.multiversepowers.engine.math.Sdf;
import nl.tivek.multiversepowers.engine.math.Segments;
import nl.tivek.multiversepowers.engine.rig.Joint;
import nl.tivek.multiversepowers.engine.rig.Rig;
import nl.tivek.multiversepowers.engine.rig.RigPose;
import nl.tivek.multiversepowers.engine.rig.RigSpace;
import nl.tivek.multiversepowers.engine.rig.Settle;

// The construct hand's bones and their sizes: four fingers of three joints each and a thumb (its root in the palm and
// two joints), every joint turning only the ways a hand can. Model units, y along the fingers, x across, z through.
final class HandRig {
    static final double[][] KNUCKLES = { { -1.12, 3.05 }, { -0.38, 3.18 }, { 0.38, 3.1 }, { 1.1, 2.88 } };
    static final double[] THICK = { 0.36, 0.38, 0.35, 0.3 };
    static final double[][] JOINTS = { { 1.25, 0.85, 0.72 }, { 1.38, 0.95, 0.78 }, { 1.3, 0.9, 0.72 },
            { 1.0, 0.68, 0.6 } };
    static final double[] SPREAD = { 0.13, 0.03, -0.07, -0.18 };
    static final double[] BENDS = { 1.5, 1.7, 1.25 };
    static final double[] HOOKS = { 1.35, 1.1 };
    static final Vec3 THUMB_ROOT = new Vec3(-1.2, 0.95, 0.35);
    static final double[] THUMB = { 1.25, 1.0, 0.82 };
    static final double[] THUMB_THICK = { 0.44, 0.4, 0.36 };
    static final double[] THUMB_HOOKS = { 0.9, 0.8 };
    // How far each finger may spread (about z) at its knuckle: the index and little finger outwards the most.
    private static final double[][] SPREADS = { { -0.3, 0.6 }, { -0.2, 0.2 }, { -0.2, 0.2 }, { -0.35, 0.2 } };
    // The same bones with no limits: how every pose was drawn before the joints had ranges.
    static final Rig FREE = rig(false);
    static final Rig RIG = rig(true);
    static final int THUMB_BONE = 12;
    // How deep one part may go into another before it is moved back, in model units.
    static final double TOUCH = 0.03;
    private static final int STEPS = 12;
    // What the fingers must stay out of: the palm (a rounded slab) and the ball of the thumb on its inside.
    private static final double[] PALM = { 0.0, 1.6, -0.015, 1.62, 1.6, 0.535, 0.3 };
    private static final double[] THENAR = { -0.95, 1.05, 0.3, 0.5 };
    // A finger's three bending turns (the knuckle's is its second) and where each sits open.
    private static final int[] BEND_TURNS = { 1, 0, 0 };
    private static final double[] OPEN = { 0.0, 0.0, 0.0 };
    // The turns that may give way together when the thumb presses deep across the index and middle finger.
    private static final int[] RELAX_BONES = { 12, 12, 13, 13, 14, 14, 0, 1, 2, 3, 4, 5 };
    private static final int[] RELAX_TURNS = { 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0, 0 };
    // The hand settled last: drawing a hand asks for the same pose again (its tips, its ring) within a frame.
    private static HandPose cachedPose;
    private static Held cachedHeld;
    private static ConstructPainter.Frame cachedHand;
    private static RigPose cachedAngles;

    // What a hand holds at its fingers: the creature's box in the world.
    record Held(double x, double y, double z, double halfX, double halfY, double halfZ) {
        static Held of(AABB box) {
            return new Held((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5,
                    (box.maxX - box.minX) * 0.5, (box.maxY - box.minY) * 0.5, (box.maxZ - box.minZ) * 0.5);
        }
    }

    private HandRig() {
    }

    // Finger k's bone j: 0 index to 3 little finger, 4 the thumb.
    static int bone(int k, int j) {
        return k < 4 ? k * 3 + j : THUMB_BONE + j;
    }

    // Ranges in radians, about x bending towards the palm: a knuckle 20 degrees back to 100 forward, the middle joint
    // straight to 110, the last one 10 back to 90; the thumb as far as it reaches across the palm.
    private static Rig rig(boolean limited) {
        Rig.Builder builder = Rig.builder();
        for (int k = 0; k < 4; k++) {
            int knuckle = builder.bone("finger" + k, -1, KNUCKLES[k][0], KNUCKLES[k][1], 0.0,
                    new Joint(turn(Joint.Axis.Z, limited, SPREADS[k][0], SPREADS[k][1]),
                            turn(Joint.Axis.X, limited, -0.35, 1.75)));
            int middle = builder.bone("finger" + k + "_middle", knuckle, 0.0, JOINTS[k][0], 0.0,
                    new Joint(turn(Joint.Axis.X, limited, -0.1, 1.92)));
            builder.bone("finger" + k + "_tip", middle, 0.0, JOINTS[k][1], 0.0,
                    new Joint(turn(Joint.Axis.X, limited, -0.17, 1.57)));
        }
        int root = builder.bone("thumb", -1, THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z,
                new Joint(turn(Joint.Axis.Z, limited, 0.0, 1.0), turn(Joint.Axis.X, limited, 0.0, 1.1)));
        int middle = builder.bone("thumb_middle", root, 0.0, THUMB[0], 0.0,
                new Joint(turn(Joint.Axis.X, limited, -0.1, 1.05), turn(Joint.Axis.Z, limited, -1.0, 0.5)));
        builder.bone("thumb_tip", middle, 0.0, THUMB[1], 0.0,
                new Joint(turn(Joint.Axis.X, limited, -0.17, 1.4), turn(Joint.Axis.Z, limited, -0.5, 0.1)));
        return builder.build();
    }

    private static Joint.Turn turn(Joint.Axis axis, boolean limited, double min, double max) {
        return limited ? new Joint.Turn(axis, min, max) : Joint.free(axis);
    }

    // The joint angles a pose asks for, worked out exactly as the hand was always drawn (within the rig's ranges).
    static RigPose angles(HandPose pose, Rig rig) {
        RigPose angles = new RigPose(rig);
        for (int k = 0; k < 4; k++) {
            double curl = pose.curl[k];
            angles.set(bone(k, 0), 0, SPREAD[k] * pose.spread * (1.0 - curl));
            angles.set(bone(k, 0), 1, curl * BENDS[0]);
            for (int j = 1; j < 3; j++) {
                angles.set(bone(k, j), 0, curl * BENDS[j] + pose.hook[k] * HOOKS[j - 1]);
            }
        }
        double thumb = pose.curl[4];
        angles.set(THUMB_BONE, 0, Mth.lerp(thumb, 0.8, 0.12));
        angles.set(THUMB_BONE, 1, Mth.lerp(thumb, 0.2, 0.95));
        for (int j = 1; j < 3; j++) {
            angles.set(THUMB_BONE + j, 0, Mth.lerp(thumb, 0.1, j == 1 ? 0.45 : 0.5) + pose.hook[4] * THUMB_HOOKS[j - 1]);
            angles.set(THUMB_BONE + j, 1, Mth.lerp(thumb, 0.0, j == 1 ? -0.95 : -0.45) + (j == 1 ? pose.thumbOut : 0.0));
        }
        return angles;
    }

    static ConstructPainter.Frame[] frames(ConstructPainter.Frame hand, HandPose pose, @Nullable Held held) {
        return RigFrames.pose(RIG, settled(pose, hand, held), hand);
    }

    // The pose as drawn: every joint within its range, round what the hand holds, the fingertips resting on the palm,
    // the thumb against the fingers and each finger beside the next instead of in them.
    static RigPose settled(HandPose pose, @Nullable ConstructPainter.Frame hand, @Nullable Held held) {
        if (pose == cachedPose && Objects.equals(held, cachedHeld) && (held == null || Objects.equals(hand,
                cachedHand))) {
            return cachedAngles;
        }
        RigPose angles = angles(pose, RIG);
        double[] space = RigSpace.create(RIG);
        double[] a0 = new double[3];
        double[] a1 = new double[3];
        double[] b0 = new double[3];
        double[] b1 = new double[3];
        double[] out = new double[2];
        if (hand != null && held != null) {
            for (int k = 0; k < 5; k++) {
                for (int j = 0; j < 3; j++) {
                    int finger = k;
                    int joint = j;
                    Settle.back(RIG, angles, bone(k, j), bendTurn(k, j), open(k, j), space,
                            s -> heldDepth(s, finger, joint, hand, held), TOUCH, STEPS);
                }
            }
        }
        Settle.Depth thumb = s -> Math.max(thumbDepth(s, 1, a0, a1, b0, b1, out), thumbDepth(s, 2, a0, a1, b0, b1,
                out));
        for (int round = 0; round < 2; round++) {
            // Against the palm the last joint straightens first, so a fist only lifts its tips off the palm; the
            // knuckle opens last, should the middle joint already lie in it.
            for (int k = 0; k < 4; k++) {
                int finger = k;
                for (int j = 2; j >= 0; j--) {
                    int from = Math.max(j, 1);
                    Settle.back(RIG, angles, bone(k, j), bendTurn(k, j), 0.0, space, s -> Math.max(palmDepth(s,
                            finger, from), from < 2 ? palmDepth(s, finger, 2) : -1.0), TOUCH, STEPS);
                }
                Settle.ease(RIG, angles, new int[] { bone(k, 0), bone(k, 1), bone(k, 2) }, BEND_TURNS, OPEN, space,
                        s -> Math.max(palmDepth(s, finger, 1), palmDepth(s, finger, 2)), TOUCH, STEPS);
            }
            for (int j = 2; j >= 1; j--) {
                Settle.back(RIG, angles, bone(4, j), 0, open(4, j), space, thumb, TOUCH, STEPS);
                Settle.back(RIG, angles, bone(4, j), 1, j == 1 ? 0.4 : 0.0, space, thumb, TOUCH, STEPS);
            }
            Settle.back(RIG, angles, THUMB_BONE, 1, open(4, 0), space, thumb, TOUCH, STEPS);
            Settle.back(RIG, angles, THUMB_BONE, 0, 0.8, space, thumb, TOUCH, STEPS);
            // Where the thumb cannot give way enough, the finger under it does, from its tip back.
            for (int k = 0; k < 4; k++) {
                for (int j = 2; j >= 0; j--) {
                    Settle.back(RIG, angles, bone(k, j), bendTurn(k, j), 0.0, space, thumb, TOUCH, STEPS);
                }
            }
            // A curled finger comes to rest on the thumb's root along the palm, not in it.
            for (int k = 0; k < 4; k++) {
                int finger = k;
                for (int j = 2; j >= 1; j--) {
                    Settle.back(RIG, angles, bone(k, j), bendTurn(k, j), 0.0, space,
                            s -> rootDepth(s, finger, a0, a1, b0, b1, out), TOUCH, STEPS);
                }
            }
        }
        RigSpace.pose(RIG, angles, space);
        if (thumb.of(space) > TOUCH) {
            // A thumb pressed deep across the fingers: thumb and the fingers under it give way together.
            Settle.relax(RIG, angles, RELAX_BONES, RELAX_TURNS, space, s -> Math.max(thumb.of(s),
                    Math.max(Math.max(palmDepth(s, 0, 1), palmDepth(s, 0, 2)),
                            Math.max(palmDepth(s, 1, 1), palmDepth(s, 1, 2)))), TOUCH, 40);
        }
        double[] outwards = { SPREADS[0][1], SPREADS[2][0], SPREADS[3][0] };
        int[] yields = { 0, 2, 3 };
        for (int p = 0; p < 3; p++) {
            int first = p;
            Settle.back(RIG, angles, bone(yields[p], 0), 0, outwards[p], space,
                    s -> besideDepth(s, first, first + 1, a0, a1, b0, b1, out), TOUCH, STEPS);
        }
        cachedPose = pose;
        cachedHeld = held;
        cachedHand = hand;
        cachedAngles = angles;
        return angles;
    }

    // Which of a bone's turns bends it towards the palm, and how that turn sits when the hand is open.
    private static int bendTurn(int k, int j) {
        return j == 0 ? 1 : 0;
    }

    private static double open(int k, int j) {
        return k < 4 ? 0.0 : j == 0 ? 0.2 : 0.1;
    }

    static double length(int k, int j) {
        return k < 4 ? JOINTS[k][j] : THUMB[j];
    }

    // A finger joint's thickness about halfway along, as its drawn shape tapers from root to end.
    static double radius(int k, int j) {
        if (k == 4) {
            double end = j < 2 ? THUMB_THICK[j + 1] : THUMB_THICK[j] * 0.9;
            return (THUMB_THICK[j] + end) * 0.5 * 0.9;
        }
        double root = THICK[k] * Math.pow(0.9, j);
        return root * 0.95 * 0.9;
    }

    private static void ends(double[] space, int k, int j, double[] from, double[] to) {
        int bone = bone(k, j);
        RigSpace.point(space, bone, 0.0, 0.0, 0.0, from);
        RigSpace.point(space, bone, 0.0, length(k, j), 0.0, to);
    }

    // How deep a finger joint goes into the palm or the ball of the thumb.
    static double palmDepth(double[] space, int k, int j) {
        int o = bone(k, j) * RigSpace.STRIDE;
        double length = length(k, j);
        double radius = radius(k, j);
        double deepest = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= 4; i++) {
            double u = length * i / 4.0;
            double x = space[o] + space[o + 6] * u;
            double y = space[o + 1] + space[o + 7] * u;
            double z = space[o + 2] + space[o + 8] * u;
            double palm = Sdf.roundBox(x - PALM[0], y - PALM[1], z - PALM[2], PALM[3], PALM[4], PALM[5], PALM[6]);
            double thenar = Sdf.sphere(x, y, z, THENAR[0], THENAR[1], THENAR[2], THENAR[3]);
            deepest = Math.max(deepest, radius - Math.min(palm, thenar));
        }
        return deepest;
    }

    // How deep a thumb joint goes into any finger.
    static double thumbDepth(double[] space, int j, double[] a0, double[] a1, double[] b0, double[] b1, double[] out) {
        ends(space, 4, j, a0, a1);
        double radius = radius(4, j);
        double deepest = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < 4; k++) {
            for (int f = 0; f < 3; f++) {
                ends(space, k, f, b0, b1);
                double gap = Math.sqrt(Segments.closest(a0, a1, b0, b1, out));
                deepest = Math.max(deepest, radius + radius(k, f) - gap);
            }
        }
        return deepest;
    }

    // How deep finger k's two outer joints go into the thumb's root, which lies along the palm by the index finger.
    static double rootDepth(double[] space, int k, double[] a0, double[] a1, double[] b0, double[] b1,
            double[] out) {
        ends(space, 4, 0, b0, b1);
        double deepest = Double.NEGATIVE_INFINITY;
        for (int j = 1; j < 3; j++) {
            ends(space, k, j, a0, a1);
            double gap = Math.sqrt(Segments.closest(a0, a1, b0, b1, out));
            deepest = Math.max(deepest, radius(k, j) + radius(4, 0) - gap);
        }
        return deepest;
    }

    // How deep the thumb goes into finger k from joint `from` to its tip.
    static double thumbOn(double[] space, int k, int from, double[] a0, double[] a1, double[] b0, double[] b1,
            double[] out) {
        double deepest = Double.NEGATIVE_INFINITY;
        for (int t = 1; t < 3; t++) {
            ends(space, 4, t, a0, a1);
            for (int f = from; f < 3; f++) {
                ends(space, k, f, b0, b1);
                double gap = Math.sqrt(Segments.closest(a0, a1, b0, b1, out));
                deepest = Math.max(deepest, radius(4, t) + radius(k, f) - gap);
            }
        }
        return deepest;
    }

    // How deep two neighbouring fingers go into each other, past the knuckles where they touch by design.
    static double besideDepth(double[] space, int k, int n, double[] a0, double[] a1, double[] b0, double[] b1,
            double[] out) {
        double deepest = Double.NEGATIVE_INFINITY;
        for (int j = 0; j < 3; j++) {
            ends(space, k, j, a0, a1);
            for (int f = 0; f < 3; f++) {
                if (j == 0 && f == 0) {
                    continue;
                }
                ends(space, n, f, b0, b1);
                double gap = Math.sqrt(Segments.closest(a0, a1, b0, b1, out));
                deepest = Math.max(deepest, radius(k, j) + radius(n, f) - gap);
            }
        }
        return deepest;
    }

    // How deep a finger joint goes into the held creature's box, measured in the world and given in model units.
    private static double heldDepth(double[] space, int k, int j, ConstructPainter.Frame hand, Held held) {
        int o = bone(k, j) * RigSpace.STRIDE;
        double length = length(k, j);
        double scale = hand.scale();
        double radius = radius(k, j) * scale;
        Vec3 c = hand.center();
        Vec3 r = hand.right();
        Vec3 u = hand.up();
        Vec3 f = hand.forward();
        double deepest = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= 4; i++) {
            double along = length * i / 4.0;
            double x = (space[o] + space[o + 6] * along) * scale;
            double y = (space[o + 1] + space[o + 7] * along) * scale;
            double z = (space[o + 2] + space[o + 8] * along) * scale;
            double wx = c.x + r.x * x + u.x * y + f.x * z;
            double wy = c.y + r.y * x + u.y * y + f.y * z;
            double wz = c.z + r.z * x + u.z * y + f.z * z;
            double box = Sdf.box(wx - held.x(), wy - held.y(), wz - held.z(), held.halfX(), held.halfY(),
                    held.halfZ());
            deepest = Math.max(deepest, radius - box);
        }
        return deepest / scale;
    }
}
