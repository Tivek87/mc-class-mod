package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.rig.RigFrames;
import nl.tivek.multiversepowers.engine.rig.RigPose;
import nl.tivek.multiversepowers.engine.rig.RigSpace;
import org.junit.jupiter.api.Test;

class MechHandRigTest {
    // The mech's fingers exactly as MechPainter drew them before the rig (2026-09-27), kept as the reference.
    static Frame[] reference(Frame hand, MechMoves.Arm arm, boolean right) {
        Frame[] bones = new Frame[15];
        double flip = right ? 1.0 : -1.0;
        for (int k = 0; k < 4; k++) {
            Frame joint = hand.moved(flip * MechHandRig.FINGER_X[k], MechArmShapes.KNUCKLES, 0.0)
                    .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, flip * MechHandRig.SPREADS[k] * arm.spread());
            for (int j = 0; j < 3; j++) {
                joint = joint.turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.08 + arm.curl() * MechHandRig.BENDS[j] * 1.45);
                bones[MechHandRig.bone(k, j)] = joint;
                joint = joint.moved(0.0, MechHandRig.FINGER_LENGTHS[k][j], 0.0);
            }
        }
        Vec3 root = MechHandRig.THUMB_ROOT;
        Frame thumb = hand.moved(flip * root.x, root.y, root.z).turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                flip * (-0.85 + 0.45 * arm.curl())).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.35 + 0.5 * arm.curl());
        for (int j = 0; j < 3; j++) {
            if (j > 0) {
                thumb = thumb.turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.12 + arm.curl() * 0.7);
            }
            bones[MechHandRig.bone(4, j)] = thumb;
            thumb = thumb.moved(0.0, MechHandRig.THUMB_LENGTHS[j], 0.0);
        }
        return bones;
    }

    private static boolean same(Frame a, Frame b) {
        return a.center().equals(b.center()) && a.right().equals(b.right()) && a.up().equals(b.up())
                && a.forward().equals(b.forward()) && Double.compare(a.scale(), b.scale()) == 0;
    }

    private static MechMoves.Arm arm(double curl, double spread) {
        return new MechMoves.Arm(Vec3.ZERO, new Vec3(0, 1, 0), new Vec3(0, 0, 1), curl, spread, 1.0);
    }

    @Test
    void theRigDrawsTheMechsFingersBitForBitAsBefore() {
        Frame hand = Frame.of(new Vec3(3.3, 71.2, -8.9), new Vec3(0.3, -0.2, 0.9), new Vec3(0, 1, 0), 1.0);
        for (boolean right : new boolean[] { true, false }) {
            for (int c = 0; c <= 50; c++) {
                for (double spread : new double[] { 0.0, 0.5, 1.0 }) {
                    MechMoves.Arm arm = arm(c / 50.0, spread);
                    Frame[] expected = reference(hand, arm, right);
                    Frame[] frames = new Frame[15];
                    RigFrames.pose(right ? MechHandRig.FREE_RIGHT : MechHandRig.FREE_LEFT,
                            MechHandRig.angles(arm, right, right ? MechHandRig.FREE_RIGHT : MechHandRig.FREE_LEFT),
                            hand, frames, true);
                    for (int b = 0; b < 15; b++) {
                        assertTrue(same(expected[b], frames[b]), "curl " + c + " spread " + spread + " bone " + b);
                    }
                }
            }
        }
    }

    // How deep the fingers and thumb of posed hand bones go into the ground, their thickness counted, and how low
    // their tips reach.
    private static double[] inGround(Frame[] bones, MechHandRig.Ground ground) {
        double deepest = Double.NEGATIVE_INFINITY;
        double lowest = Double.POSITIVE_INFINITY;
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 3; j++) {
                Frame bone = bones[MechHandRig.bone(k, j)];
                double length = MechHandRig.length(k, j);
                for (int i = 0; i <= 4; i++) {
                    Vec3 at = bone.at(0.0, length * i / 4.0, 0.0);
                    deepest = Math.max(deepest, ground.depth(at.x, at.y, at.z, k < 4 ? 0.15 : 0.17));
                    lowest = Math.min(lowest, at.y);
                }
            }
        }
        return new double[] { deepest, lowest };
    }

    // A hand held palm down just over the ground, its fingers curled more or less: none goes into it.
    @Test
    void fingersRestOnTheGroundInsteadOfGoingIntoIt() {
        MechHandRig.Ground ground = (x, y, z, radius) -> radius - y;
        Frame hand = Frame.of(new Vec3(0.0, 0.36, 0.0), new Vec3(0.0, -1.0, 0.0), new Vec3(0.0, 0.0, 1.0), 1.0);
        double worst = Double.NEGATIVE_INFINITY;
        for (boolean right : new boolean[] { true, false }) {
            for (int c = 0; c <= 20; c++) {
                Frame[] bones = MechHandRig.frames(hand, arm(c / 20.0, 0.5), right, null, null, ground);
                worst = Math.max(worst, inGround(bones, ground)[0]);
            }
        }
        assertTrue(worst <= MechHandRig.TOUCH + 0.01, "fingers go into the ground " + worst);
    }

    // A gripping hand laid on a ledge, its knuckles just past the edge: its fingers hook down over the edge, onto the
    // face of the wall under it, and never into the ledge.
    @Test
    void aGrippingHandWrapsItsFingersRoundAnEdge() {
        double edge = MechArmShapes.KNUCKLES - 0.15;
        MechHandRig.Ground ledge = (x, y, z, radius) -> {
            double d = y < 0.0 && z < edge ? -Math.min(-y, edge - z)
                    : Math.sqrt(Math.max(y, 0.0) * Math.max(y, 0.0) + Math.max(z - edge, 0.0) * Math.max(z - edge,
                            0.0));
            return radius - d;
        };
        Frame hand = Frame.of(new Vec3(0.0, 0.36, 0.0), new Vec3(0.0, -1.0, 0.0), new Vec3(0.0, 0.0, 1.0), 1.0);
        for (boolean right : new boolean[] { true, false }) {
            double[] gripped = inGround(MechHandRig.frames(hand, arm(0.7, 0.25), right, null, null, ledge), ledge);
            double[] loose = inGround(MechHandRig.frames(hand, arm(0.3, 0.25), right, null, null, ledge), ledge);
            assertTrue(gripped[0] <= MechHandRig.TOUCH + 0.01, "fingers go into the ledge " + gripped[0]);
            assertTrue(gripped[1] < -0.3, "the fingers hook down over the edge, lowest at " + gripped[1]);
            assertTrue(loose[1] > gripped[1], "a loose hand does not grip");
        }
    }

    @Test
    void settledMechHandsNeverGoIntoThemselves() {
        double[] a0 = new double[3];
        double[] a1 = new double[3];
        double[] b0 = new double[3];
        double[] b1 = new double[3];
        double[] out = new double[2];
        double worstBefore = 0.0;
        double worstAfter = 0.0;
        for (boolean right : new boolean[] { true, false }) {
            double[] space = RigSpace.create(right ? MechHandRig.RIGHT : MechHandRig.LEFT);
            for (int c = 0; c <= 100; c++) {
                for (double spread : new double[] { 0.0, 0.5, 1.0 }) {
                    MechMoves.Arm arm = arm(c / 100.0, spread);
                    RigPose raw = MechHandRig.angles(arm, right, right ? MechHandRig.FREE_RIGHT : MechHandRig.FREE_LEFT);
                    RigSpace.pose(right ? MechHandRig.FREE_RIGHT : MechHandRig.FREE_LEFT, raw, space);
                    double before = MechHandRig.thumbDepth(space, a0, a1, b0, b1, out);
                    for (int k = 0; k < 4; k++) {
                        for (int j = 1; j < 3; j++) {
                            before = Math.max(before, MechHandRig.palmDepth(space, k, j));
                        }
                    }
                    RigPose settled = MechHandRig.settled(arm, right);
                    RigSpace.pose(right ? MechHandRig.RIGHT : MechHandRig.LEFT, settled, space);
                    double after = MechHandRig.thumbDepth(space, a0, a1, b0, b1, out);
                    for (int k = 0; k < 4; k++) {
                        for (int j = 1; j < 3; j++) {
                            after = Math.max(after, MechHandRig.palmDepth(space, k, j));
                        }
                    }
                    worstBefore = Math.max(worstBefore, before);
                    worstAfter = Math.max(worstAfter, after);
                    if (before <= MechHandRig.TOUCH) {
                        for (int b = 0; b < 15; b++) {
                            for (int t = 0; t < MechHandRig.RIGHT.joint(b).size(); t++) {
                                assertTrue(Double.compare(raw.get(b, t), settled.get(b, t)) == 0,
                                        "a clean pose stays: curl " + c + " bone " + b);
                            }
                        }
                    }
                }
            }
        }
        System.out.printf("MECH deepest before %.3f, after %.3f%n", worstBefore, worstAfter);
        assertTrue(worstAfter <= MechHandRig.TOUCH + 0.01, "deepest after settling " + worstAfter);
    }
}
