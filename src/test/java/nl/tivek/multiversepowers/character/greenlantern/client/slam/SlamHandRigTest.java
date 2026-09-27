package nl.tivek.multiversepowers.character.greenlantern.client.slam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Random;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.rig.Joint;
import nl.tivek.multiversepowers.engine.rig.RigPose;
import org.junit.jupiter.api.Test;

class SlamHandRigTest {
    private static final double[] KNUCKLE_Z = { 0.30, 0.10, -0.10, -0.29 };
    private static final double[][] LENGTHS = { { 0.36, 0.24, 0.20 }, { 0.40, 0.27, 0.21 }, { 0.37, 0.25, 0.20 },
            { 0.29, 0.20, 0.17 }, { 0.26, 0.22, 0.18 } };

    // The fingers as the slam hands built them by hand before they had bones.
    private static Frame[] handBuilt(Frame frame, double[] curl) {
        Frame[] out = new Frame[15];
        for (int f = 0; f < 5; f++) {
            Frame joint;
            if (f < 4) {
                joint = frame.moved(0.0, 0.30, KNUCKLE_Z[f]);
            } else {
                joint = frame.moved(-0.04, -0.22, 0.40).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Math.toRadians(40.0))
                        .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Math.toRadians(15.0));
            }
            for (int b = 0; b < 3; b++) {
                double share = curl[f] < 0.0 ? (b == 0 ? 1.2 : 0.25) : (b == 1 ? 1.2 : 0.9);
                joint = joint.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, curl[f] * share);
                out[f * 3 + b] = joint;
                joint = joint.moved(0.0, LENGTHS[f][b], 0.0);
            }
        }
        return out;
    }

    private static void same(double[] curl, Frame palm) {
        Frame[] expected = handBuilt(palm, curl);
        Frame[] actual = SlamHandRig.frames(palm, curl);
        for (int b = 0; b < expected.length; b++) {
            String what = "bone " + b;
            assertEquals(expected[b].center(), actual[b].center(), what + " center");
            assertEquals(expected[b].right(), actual[b].right(), what + " right");
            assertEquals(expected[b].up(), actual[b].up(), what + " up");
            assertEquals(expected[b].forward(), actual[b].forward(), what + " forward");
            assertEquals(expected[b].scale(), actual[b].scale(), what + " scale");
        }
    }

    @Test
    void theBonesGiveTheHandBuiltFingersBitForBitOverEveryBendTheSlamsUse() {
        Random random = new Random(11);
        for (int run = 0; run < 3000; run++) {
            Vec3 forward = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            Frame palm = Frame.of(new Vec3(random.nextGaussian() * 40.0, 70.0 + random.nextGaussian(),
                    random.nextGaussian() * 40.0), forward, new Vec3(0.0, 1.0, 0.0), 0.5 + random.nextDouble() * 5.0);
            double[] curl = new double[5];
            if (run % 2 == 0) {
                double bend = run < 6 ? (run == 0 ? 0.0 : 0.3) : random.nextDouble() * 0.3;
                curl = new double[] { bend, bend, bend, bend, 0.5 * bend };
            } else {
                for (int f = 0; f < 4; f++) {
                    curl[f] = run < 6 ? (run == 1 ? -0.0 : -0.35) : -0.35 * random.nextDouble();
                }
            }
            same(curl, palm);
        }
    }

    @Test
    void aFingerNeverBendsPastWhatAHandCan() {
        RigPose pose = SlamHandRig.pose(new double[] { 5.0, -5.0, 5.0, -5.0, 5.0 });
        for (int b = 0; b < SlamHandRig.RIG.size(); b++) {
            Joint joint = SlamHandRig.RIG.joint(b);
            for (int t = 0; t < joint.size(); t++) {
                assertTrue(joint.turn(t).holds(pose.get(b, t)), "bone " + b + " turn " + t);
            }
        }
        assertEquals(1.6, pose.get(SlamHandRig.bone(0, 0), 0));
        assertEquals(-0.6, pose.get(SlamHandRig.bone(1, 0), 0));
        assertEquals(-0.1, pose.get(SlamHandRig.bone(1, 1), 0));
        assertEquals(Math.toRadians(40.0), pose.get(SlamHandRig.bone(SlamHandRig.THUMB_FINGER, 0), 0));
    }
}
