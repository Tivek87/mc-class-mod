package nl.tivek.multiversepowers.engine.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.rig.RigFrames;
import org.junit.jupiter.api.Test;

class RigSpaceTest {
    static Rig arm() {
        Rig.Builder builder = Rig.builder();
        int shoulder = builder.bone("shoulder", -1, 0.4, 2.0, -0.3, new Joint(Joint.free(Joint.Axis.Y),
                Joint.free(Joint.Axis.Z), Joint.free(Joint.Axis.X)));
        int elbow = builder.bone("elbow", shoulder, 0.0, 1.5, 0.0, new Joint(Joint.free(Joint.Axis.X)));
        builder.bone("wrist", elbow, 0.1, 1.2, 0.05, new Joint(Joint.free(Joint.Axis.Z), Joint.free(Joint.Axis.Y)));
        return builder.build();
    }

    private static void close(Vec3 expected, double x, double y, double z, String what) {
        assertEquals(expected.x, x, 1.0E-9, what);
        assertEquals(expected.y, y, 1.0E-9, what);
        assertEquals(expected.z, z, 1.0E-9, what);
    }

    @Test
    void theSpaceIsTheDrawnBonesInTheirRootsOwnAxesForBothHands() {
        Rig rig = arm();
        RigPose pose = new RigPose(rig);
        double[] space = RigSpace.create(rig);
        Random random = new Random(11);
        for (int run = 0; run < 400; run++) {
            for (int b = 0; b < rig.size(); b++) {
                for (int t = 0; t < rig.joint(b).size(); t++) {
                    pose.set(b, t, random.nextGaussian());
                }
            }
            ConstructPainter.Frame root = ConstructPainter.Frame.of(new Vec3(random.nextGaussian() * 9, 70.0,
                    random.nextGaussian() * 9), new Vec3(random.nextGaussian(), random.nextGaussian(),
                    random.nextGaussian()), new Vec3(0, 1, 0), 0.5 + random.nextDouble() * 3.0);
            if (run % 2 == 1) {
                root = new ConstructPainter.Frame(root.center(), root.right().scale(-1.0), root.up(), root.forward(),
                        root.scale());
            }
            ConstructPainter.Frame[] frames = RigFrames.pose(rig, pose, root);
            RigSpace.pose(rig, pose, space);
            for (int b = 0; b < rig.size(); b++) {
                int o = b * RigSpace.STRIDE;
                Vec3 at = root.at(space[o], space[o + 1], space[o + 2]);
                close(frames[b].center(), at.x, at.y, at.z, "origin " + b + " run " + run);
                for (int axis = 0; axis < 3; axis++) {
                    int a = o + 3 + axis * 3;
                    Vec3 way = root.right().scale(space[a]).add(root.up().scale(space[a + 1]))
                            .add(root.forward().scale(space[a + 2]));
                    Vec3 drawn = axis == 0 ? frames[b].right() : axis == 1 ? frames[b].up() : frames[b].forward();
                    close(drawn, way.x, way.y, way.z, "axis " + axis + " of " + b + " run " + run);
                }
            }
        }
    }

    @Test
    void pointsFollowTheirBone() {
        Rig rig = arm();
        RigPose pose = new RigPose(rig);
        pose.set(1, 0, Math.PI / 2.0);
        double[] space = RigSpace.create(rig);
        RigSpace.pose(rig, pose, space);
        double[] out = new double[3];
        RigSpace.point(space, 1, 0.0, 1.0, 0.0, out);
        assertEquals(0.4, out[0], 1.0E-12);
        assertEquals(3.5, out[1], 1.0E-12);
        assertEquals(-0.3 + 1.0, out[2], 1.0E-12, "a quarter turn about x sends the bone's y to z");
    }

    @Test
    void settleStopsABoneOnAFloor() {
        Rig.Builder builder = Rig.builder();
        builder.bone("stick", -1, 0.0, 1.0, 0.0, new Joint(Joint.x(-3.0, 3.0)));
        Rig rig = builder.build();
        RigPose pose = new RigPose(rig);
        pose.set(0, 0, 2.5);
        double[] space = RigSpace.create(rig);
        double[] tip = new double[3];
        // A floor at y = 0.5 the stick's tip (1 long, from y = 1) must not go under.
        Settle.Depth depth = s -> {
            RigSpace.point(s, 0, 0.0, 1.0, 0.0, tip);
            return 0.5 - tip[1];
        };
        double angle = Settle.back(rig, pose, 0, 0, 0.0, space, depth, 1.0E-6, 30);
        assertTrue(Math.abs(depth.of(space)) < 1.0E-6, "the tip rests on the floor");
        assertEquals(Math.PI * 2.0 / 3.0, angle, 5.0E-6, "cos(angle) = -0.5, give or take the depth tolerance");
        assertEquals(angle, pose.get(0, 0));
        pose.set(0, 0, 0.5);
        assertEquals(0.5, Settle.back(rig, pose, 0, 0, 0.0, space, depth, 1.0E-6, 30), "clear poses stay");
    }
}
