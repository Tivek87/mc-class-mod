package nl.tivek.multiversepowers.character.greenlantern.client.render.hand;

import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.BENDS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.HOOKS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.JOINTS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.KNUCKLES;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.SPREAD;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.THUMB;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.THUMB_HOOKS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.THUMB_ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.rig.RigFrames;
import nl.tivek.multiversepowers.engine.rig.RigPose;
import nl.tivek.multiversepowers.engine.rig.RigSpace;
import org.junit.jupiter.api.Test;

class HandRigTest {
    // The hand's fingers exactly as HandPainter built them before the rig (2026-09-27), kept as the reference.
    static ConstructPainter.Frame[] referenceDigit(ConstructPainter.Frame hand, HandPose pose, int k) {
        ConstructPainter.Frame[] joints = new ConstructPainter.Frame[3];
        if (k < 4) {
            double curl = pose.curl[k];
            double x = KNUCKLES[k][0];
            double y = KNUCKLES[k][1];
            ConstructPainter.Frame joint = hand.turned(x, y, 0.0, 0.0, 0.0, 1.0,
                    SPREAD[k] * pose.spread * (1.0 - curl)).turned(x, y, 0.0, 1.0, 0.0, 0.0, curl * BENDS[0])
                    .moved(x, y, 0.0);
            joints[0] = joint;
            for (int j = 1; j < 3; j++) {
                double back = JOINTS[k][j - 1];
                joint = joint.turned(0.0, back, 0.0, 1.0, 0.0, 0.0, curl * BENDS[j] + pose.hook[k] * HOOKS[j - 1])
                        .moved(0.0, back, 0.0);
                joints[j] = joint;
            }
            return joints;
        }
        double thumb = pose.curl[4];
        ConstructPainter.Frame joint = hand.turned(THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z, 0.0, 0.0, 1.0,
                Mth.lerp(thumb, 0.8, 0.12)).turned(THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z, 1.0, 0.0, 0.0,
                Mth.lerp(thumb, 0.2, 0.95)).moved(THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z);
        joints[0] = joint;
        for (int j = 1; j < 3; j++) {
            double back = THUMB[j - 1];
            joint = joint.turned(0.0, back, 0.0, 1.0, 0.0, 0.0, Mth.lerp(thumb, 0.1, j == 1 ? 0.45 : 0.5)
                    + pose.hook[4] * THUMB_HOOKS[j - 1]).turned(0.0, back, 0.0, 0.0, 0.0, 1.0,
                    Mth.lerp(thumb, 0.0, j == 1 ? -0.95 : -0.45) + (j == 1 ? pose.thumbOut : 0.0))
                    .moved(0.0, back, 0.0);
            joints[j] = joint;
        }
        return joints;
    }

    static int[] variants() {
        int[] variants = new int[HandPose.MOVES * 8];
        int n = 0;
        for (int move = 0; move < HandPose.MOVES; move++) {
            for (int bits = 0; bits < 8; bits++) {
                variants[n++] = HandPose.variant(move, (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0 ? 3 : 0);
            }
        }
        return variants;
    }

    private static boolean same(ConstructPainter.Frame a, ConstructPainter.Frame b) {
        return a.center().equals(b.center()) && a.right().equals(b.right()) && a.up().equals(b.up())
                && a.forward().equals(b.forward()) && Double.compare(a.scale(), b.scale()) == 0;
    }

    @Test
    void theRigDrawsEveryMoveBitForBitAsBefore() {
        int compared = 0;
        for (int variant : variants()) {
            int life = HandPose.life(variant);
            for (double reach : new double[] { 2.5, 11.0 }) {
                for (int tick = 0; tick <= life; tick++) {
                    for (double part : new double[] { 0.0, 0.37 }) {
                        HandPose pose = HandPose.at(variant, tick + part, reach);
                        HandPose.Place place = pose.place(new Vec3(12.3, 64.0, -40.7), new Vec3(reach * 2.5, 0.0,
                                -reach), 2.5);
                        for (boolean left : new boolean[] { false, true }) {
                            ConstructPainter.Frame hand = HandPainter.handFrame(place, left);
                            ConstructPainter.Frame[] frames = RigFrames.pose(HandRig.FREE,
                                    HandRig.angles(pose, HandRig.FREE), hand);
                            for (int k = 0; k < 5; k++) {
                                ConstructPainter.Frame[] expected = referenceDigit(hand, pose, k);
                                for (int j = 0; j < 3; j++) {
                                    assertTrue(same(expected[j], frames[HandRig.bone(k, j)]), "variant " + variant
                                            + " tick " + (tick + part) + " finger " + k + " joint " + j);
                                    compared++;
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue(compared > 100000, "compared " + compared);
    }

    private static double deepest(double[] space) {
        double[] a0 = new double[3];
        double[] a1 = new double[3];
        double[] b0 = new double[3];
        double[] b1 = new double[3];
        double[] out = new double[2];
        double deepest = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < 4; k++) {
            for (int j = 1; j < 3; j++) {
                deepest = Math.max(deepest, HandRig.palmDepth(space, k, j));
            }
        }
        for (int j = 1; j < 3; j++) {
            deepest = Math.max(deepest, HandRig.thumbDepth(space, j, a0, a1, b0, b1, out));
        }
        for (int k = 0; k < 3; k++) {
            deepest = Math.max(deepest, HandRig.besideDepth(space, k, k + 1, a0, a1, b0, b1, out));
        }
        return deepest;
    }

    @Test
    void settledHandsNeverGoIntoThemselvesAndLeaveCleanPosesAlone() {
        double[] space = RigSpace.create(HandRig.RIG);
        double[] free = RigSpace.create(HandRig.FREE);
        int poses = 0;
        int changed = 0;
        double worstBefore = 0.0;
        double worstAfter = 0.0;
        String worstAt = "";
        for (int variant : variants()) {
            int life = HandPose.life(variant);
            for (int tick = 0; tick <= life; tick++) {
                for (double part : new double[] { 0.0, 0.5 }) {
                    HandPose pose = HandPose.at(variant, tick + part, 6.0);
                    RigPose raw = HandRig.angles(pose, HandRig.FREE);
                    RigPose settled = HandRig.settled(pose, null, null);
                    RigSpace.pose(HandRig.FREE, raw, free);
                    RigSpace.pose(HandRig.RIG, settled, space);
                    double before = deepest(free);
                    double after = deepest(space);
                    worstBefore = Math.max(worstBefore, before);
                    if (after > worstAfter) {
                        worstAfter = after;
                        worstAt = "move " + HandPose.move(variant) + " t " + (tick + part);
                    }
                    boolean inRange = true;
                    boolean same = true;
                    for (int b = 0; b < HandRig.RIG.size(); b++) {
                        for (int t = 0; t < HandRig.RIG.joint(b).size(); t++) {
                            inRange &= HandRig.RIG.joint(b).turn(t).holds(raw.get(b, t));
                            same &= Double.compare(raw.get(b, t), settled.get(b, t)) == 0;
                        }
                    }
                    if (inRange && before <= HandRig.TOUCH) {
                        assertTrue(same, "a clean pose must stay as it was: move " + HandPose.move(variant) + " t "
                                + (tick + part));
                    }
                    poses++;
                    changed += same ? 0 : 1;
                }
            }
        }
        System.out.printf("SETTLE %d poses, %d changed; deepest before %.3f, after %.3f (%s)%n", poses, changed,
                worstBefore, worstAfter, worstAt);
        assertTrue(worstAfter <= HandRig.TOUCH + 0.02, "deepest after settling " + worstAfter + " at " + worstAt);
    }

    @Test
    void aHeldCreatureStopsTheFingersOnItsSkin() {
        ConstructPainter.Frame hand = new ConstructPainter.Frame(new Vec3(0.0, 70.0, 0.0), new Vec3(1, 0, 0),
                new Vec3(0, 1, 0), new Vec3(0, 0, 1), 2.0);
        HandPose fist = new HandPose();
        for (int k = 0; k < 5; k++) {
            fist.curl[k] = 1.0;
        }
        // A box in front of the palm, where a fist would close.
        HandRig.Held held = new HandRig.Held(0.0, 70.0 + 2.2 * 2.0, 1.6 * 2.0, 1.2, 1.1, 1.0);
        RigPose settled = HandRig.settled(fist, hand, held);
        double wrapped = settled.get(HandRig.bone(1, 1), 0);
        RigPose open = HandRig.settled(fist, null, null);
        assertTrue(wrapped < open.get(HandRig.bone(1, 1), 0) - 0.05,
                "the middle finger closes less round the creature than into an empty fist");
    }

    @Test
    void theRigHasFifteenBonesInFingerOrder() {
        assertEquals(15, HandRig.RIG.size());
        assertEquals("finger1_tip", HandRig.RIG.name(HandRig.bone(1, 2)));
        assertEquals("thumb_middle", HandRig.RIG.name(HandRig.bone(4, 1)));
    }
}
