package nl.tivek.multiversepowers.engine.client.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.rig.Joint;
import nl.tivek.multiversepowers.engine.rig.Rig;
import nl.tivek.multiversepowers.engine.rig.RigPose;
import nl.tivek.multiversepowers.engine.rig.Socket;
import org.junit.jupiter.api.Test;

class RigFramesTest {
    private static void same(ConstructPainter.Frame expected, ConstructPainter.Frame actual, String what) {
        assertEquals(expected.center(), actual.center(), what + " center");
        assertEquals(expected.right(), actual.right(), what + " right");
        assertEquals(expected.up(), actual.up(), what + " up");
        assertEquals(expected.forward(), actual.forward(), what + " forward");
        assertEquals(expected.scale(), actual.scale(), what + " scale");
    }

    @Test
    void aRigGivesTheFramesOfAHandBuiltChainBitForBit() {
        double[] root = { 0.38, 3.1 };
        double[] lengths = { 1.3, 0.9, 0.72 };
        Rig.Builder builder = Rig.builder();
        int knuckle = builder.bone("knuckle", -1, root[0], root[1], 0.0, new Joint(Joint.free(Joint.Axis.Z),
                Joint.free(Joint.Axis.X)));
        int middle = builder.bone("middle", knuckle, 0.0, lengths[0], 0.0, new Joint(Joint.free(Joint.Axis.X)));
        builder.bone("tip", middle, 0.0, lengths[1], 0.0,
                new Joint(Joint.free(Joint.Axis.X), Joint.free(Joint.Axis.Z)));
        Rig rig = builder.build();
        RigPose pose = new RigPose(rig);
        Random random = new Random(3);
        for (int run = 0; run < 500; run++) {
            Vec3 forward = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            ConstructPainter.Frame hand = ConstructPainter.Frame.of(
                    new Vec3(random.nextGaussian() * 50.0, 60.0 + random.nextGaussian(), random.nextGaussian() * 50.0),
                    forward, new Vec3(0.0, 1.0, 0.0), 0.5 + random.nextDouble() * 4.0);
            double spread = random.nextGaussian() * 0.2;
            double curl = random.nextDouble() * 1.6;
            double bend = random.nextDouble() * 1.8;
            double hook = random.nextDouble() * 1.2;
            double twist = random.nextGaussian() * 0.4;
            ConstructPainter.Frame k = hand.turned(root[0], root[1], 0.0, 0.0, 0.0, 1.0, spread)
                    .turned(root[0], root[1], 0.0, 1.0, 0.0, 0.0, curl).moved(root[0], root[1], 0.0);
            ConstructPainter.Frame m = k.turned(0.0, lengths[0], 0.0, 1.0, 0.0, 0.0, bend).moved(0.0, lengths[0], 0.0);
            ConstructPainter.Frame t = m.turned(0.0, lengths[1], 0.0, 1.0, 0.0, 0.0, hook)
                    .turned(0.0, lengths[1], 0.0, 0.0, 0.0, 1.0, twist).moved(0.0, lengths[1], 0.0);
            pose.set(0, 0, spread);
            pose.set(0, 1, curl);
            pose.set(1, 0, bend);
            pose.set(2, 0, hook);
            pose.set(2, 1, twist);
            ConstructPainter.Frame[] frames = RigFrames.pose(rig, pose, hand);
            same(k, frames[0], "knuckle " + run);
            same(m, frames[1], "middle " + run);
            same(t, frames[2], "tip " + run);
            same(t.moved(0.0, lengths[2], 0.0), RigFrames.at(new Socket(2, 0.0, lengths[2], 0.0), frames),
                    "tip end " + run);
        }
    }
}
