package nl.tivek.multiversepowers.engine.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class RigidWorldTest {
    private static final double TICK = 0.05;
    private static final int SUBSTEPS = 20;

    // A floor of one block thick, its top at y = 0.
    private static final Blocks FLOOR = (minX, minY, minZ, maxX, maxY, maxZ, out) -> {
        if (minY > 0.0 || maxY < -1.0) {
            return 0;
        }
        out[0] = -100.0;
        out[1] = -1.0;
        out[2] = -100.0;
        out[3] = 100.0;
        out[4] = 0.0;
        out[5] = 100.0;
        return 1;
    };

    private static double lowestCorner(RigidWorld world, int b) {
        double[] p = new double[3];
        double lowest = Double.POSITIVE_INFINITY;
        for (int c = 0; c < 8; c++) {
            world.point(b, (c & 1) == 0 ? -world.half(b, 0) : world.half(b, 0),
                    (c & 2) == 0 ? -world.half(b, 1) : world.half(b, 1),
                    (c & 4) == 0 ? -world.half(b, 2) : world.half(b, 2), p);
            lowest = Math.min(lowest, p[1]);
        }
        return lowest;
    }

    private static void run(RigidWorld world, int ticks, Blocks blocks) {
        for (int i = 0; i < ticks; i++) {
            world.step(TICK, SUBSTEPS, blocks);
        }
    }

    @Test
    void aBoxFallsAndRestsOnTheFloor() {
        RigidWorld world = new RigidWorld();
        int box = world.add(1.0, 0.5, 0.5, 0.5);
        world.place(box, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        run(world, 80, FLOOR);
        double[] pose = new double[7];
        world.pose(box, pose);
        assertEquals(0.5, pose[1], 0.03, "rests with its bottom on the floor");
        assertEquals(0.0, lowestCorner(world, box), 0.03);
        assertTrue(world.sleeping(), "a box at rest sleeps");
    }

    @Test
    void aTiltedBoxTopplesAndLiesFlat() {
        RigidWorld world = new RigidWorld();
        int box = world.add(1.0, 0.25, 0.75, 0.25);
        double half = Math.toRadians(35.0) * 0.5;
        world.place(box, 0.0, 1.2, 0.0, 0.0, 0.0, Math.sin(half), Math.cos(half));
        run(world, 200, FLOOR);
        assertEquals(0.0, lowestCorner(world, box), 0.04, "lies on the floor");
        double[] pose = new double[7];
        world.pose(box, pose);
        assertEquals(0.25, pose[1], 0.05, "on its long side, its centre a half width up");
    }

    @Test
    void aFastBoxDoesNotGoThroughAThinFloor() {
        RigidWorld world = new RigidWorld();
        int box = world.add(1.0, 0.3, 0.3, 0.3);
        world.place(box, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        world.velocity(box, 0.0, -80.0, 0.0, 0.0, 0.0, 0.0);
        run(world, 40, FLOOR);
        assertTrue(lowestCorner(world, box) > -0.05, "stays above the floor");
    }

    @Test
    void aPinnedBoxSwingsFromItsPin() {
        RigidWorld world = new RigidWorld();
        int box = world.add(1.0, 0.2, 0.6, 0.2);
        world.place(box, 1.0, 4.4, 0.0, 0.0, 0.0, 0.0, 1.0);
        Pin pin = new Pin(box, 0.0, 0.6, 0.0, 0.0);
        pin.to(0.0, 5.0, 0.0);
        world.add(pin);
        double[] p = new double[3];
        for (int i = 0; i < 100; i++) {
            world.step(TICK, SUBSTEPS, Blocks.NONE);
            world.point(box, 0.0, 0.6, 0.0, p);
            if (i > 2) {
                double gap = Math.sqrt(p[0] * p[0] + (p[1] - 5.0) * (p[1] - 5.0) + p[2] * p[2]);
                assertTrue(gap < 0.01, "tick " + i + " gap " + gap);
            }
        }
        double[] pose = new double[7];
        world.pose(box, pose);
        assertTrue(pose[1] < 5.0, "hangs below its pin");
    }

    private static int[] hanging(RigidWorld world, double swing, double twistMin, double twistMax) {
        int root = world.add(0.0, 0.2, 0.2, 0.2);
        world.place(root, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        int limb = world.add(1.0, 0.15, 0.5, 0.15);
        world.place(limb, 0.0, 5.3, 0.0, 0.0, 0.0, 0.0, 1.0);
        world.add(new BallJoint(root, new double[] { 0.0, -0.2, 0.0 }, new double[] { 0.0, -1.0, 0.0 },
                new double[] { 1.0, 0.0, 0.0 }, limb, new double[] { 0.0, 0.5, 0.0 }, new double[] { 0.0, -1.0, 0.0 },
                new double[] { 1.0, 0.0, 0.0 }, swing, twistMin, twistMax));
        return new int[] { root, limb };
    }

    @Test
    void aBallJointHoldsItsAnchorsAndItsSwing() {
        RigidWorld world = new RigidWorld();
        int[] pair = hanging(world, 0.4, -0.3, 0.3);
        world.velocity(pair[1], 12.0, 0.0, 3.0, 0.0, 0.0, 0.0);
        double[] a = new double[3];
        double[] b = new double[3];
        double[] axis = new double[3];
        double worst = 0.0;
        for (int i = 0; i < 120; i++) {
            world.step(TICK, SUBSTEPS, Blocks.NONE);
            world.point(pair[0], 0.0, -0.2, 0.0, a);
            world.point(pair[1], 0.0, 0.5, 0.0, b);
            double gap = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1])
                    + (a[2] - b[2]) * (a[2] - b[2]));
            assertTrue(gap < 0.02, "tick " + i + " gap " + gap);
            Quat.rotate(world.q, pair[1] * 4, 0.0, -1.0, 0.0, axis, 0);
            worst = Math.max(worst, Math.acos(Math.max(-1.0, Math.min(1.0, -axis[1]))));
        }
        assertTrue(worst <= 0.4 + 0.06, "swing stayed within its cone, at most " + worst);
    }

    @Test
    void aBallJointHoldsItsTwist() {
        RigidWorld world = new RigidWorld();
        int[] pair = hanging(world, 0.4, -0.3, 0.3);
        world.velocity(pair[1], 0.0, 0.0, 0.0, 0.0, 25.0, 0.0);
        double[] ref = new double[3];
        double worst = 0.0;
        for (int i = 0; i < 60; i++) {
            world.step(TICK, SUBSTEPS, Blocks.NONE);
            Quat.rotate(world.q, pair[1] * 4, 1.0, 0.0, 0.0, ref, 0);
            worst = Math.max(worst, Math.abs(Math.atan2(ref[2], ref[0])));
        }
        assertTrue(worst <= 0.3 + 0.06, "twist stayed within its range, at most " + worst);
    }

    @Test
    void aChainStaysWholeUnderRandomKicks() {
        RigidWorld world = new RigidWorld();
        int previous = world.add(0.0, 0.2, 0.2, 0.2);
        world.place(previous, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        int[] links = new int[6];
        for (int i = 0; i < 6; i++) {
            links[i] = world.add(1.0, 0.12, 0.4, 0.12);
            world.place(links[i], 0.0, 9.4 - i * 0.8, 0.0, 0.0, 0.0, 0.0, 1.0);
            world.add(new BallJoint(previous, new double[] { 0.0, i == 0 ? -0.2 : -0.4, 0.0 },
                    new double[] { 0.0, -1.0, 0.0 }, new double[] { 1.0, 0.0, 0.0 }, links[i],
                    new double[] { 0.0, 0.4, 0.0 }, new double[] { 0.0, -1.0, 0.0 }, new double[] { 1.0, 0.0, 0.0 },
                    0.8, -0.5, 0.5));
            previous = links[i];
        }
        Random random = new Random(5);
        double[] a = new double[3];
        double[] b = new double[3];
        double[] pose = new double[7];
        double worstAtKick = 0.0;
        double worstAfter = 0.0;
        for (int i = 0; i < 400; i++) {
            if (i % 20 == 0) {
                int kicked = links[random.nextInt(6)];
                world.velocity(kicked, random.nextGaussian() * 30.0, random.nextGaussian() * 30.0,
                        random.nextGaussian() * 30.0, random.nextGaussian() * 20.0, random.nextGaussian() * 20.0,
                        random.nextGaussian() * 20.0);
            }
            world.step(TICK, SUBSTEPS, FLOOR);
            for (int l = 0; l < 6; l++) {
                world.pose(links[l], pose);
                for (double value : pose) {
                    assertFalse(Double.isNaN(value) || Double.isInfinite(value), "tick " + i);
                }
            }
            for (int l = 1; l < 6; l++) {
                world.point(links[l - 1], 0.0, -0.4, 0.0, a);
                world.point(links[l], 0.0, 0.4, 0.0, b);
                double gap = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1])
                        + (a[2] - b[2]) * (a[2] - b[2]));
                if (i % 20 < 2) {
                    worstAtKick = Math.max(worstAtKick, gap);
                } else {
                    worstAfter = Math.max(worstAfter, gap);
                }
            }
        }
        System.out.printf("CHAIN worst gap at a kick %.4f, after %.4f%n", worstAtKick, worstAfter);
        assertTrue(worstAtKick < 0.05, "a hard kick opens a joint only a little: " + worstAtKick);
        assertTrue(worstAfter < 0.03, "and it stays shut after: " + worstAfter);
    }

    // A floor of whole blocks with a step one block high on the +x side.
    private static final Blocks STEP = (minX, minY, minZ, maxX, maxY, maxZ, out) -> {
        int n = 0;
        for (int x = (int) Math.floor(minX); x <= Math.floor(maxX); x++) {
            for (int z = (int) Math.floor(minZ); z <= Math.floor(maxZ); z++) {
                for (int y = Math.max(-1, (int) Math.floor(minY)); y <= Math.min(x >= 0 ? 0 : -1,
                        (int) Math.floor(maxY)); y++) {
                    if (n >= out.length / 6) {
                        return n;
                    }
                    out[n * 6] = x;
                    out[n * 6 + 1] = y;
                    out[n * 6 + 2] = z;
                    out[n * 6 + 3] = x + 1;
                    out[n * 6 + 4] = y + 1;
                    out[n * 6 + 5] = z + 1;
                    n++;
                }
            }
        }
        return n;
    };

    @Test
    void aBigBoxAcrossTheEdgeOfAStepRestsOnItAndNeverSinksIn() {
        for (double tilt : new double[] { 0.0, 0.3, -0.25 }) {
            RigidWorld world = new RigidWorld();
            int box = world.add(40.0, 0.7, 0.3, 0.35);
            world.place(box, 0.1, 1.6, 0.5, 0.0, 0.0, Math.sin(tilt / 2.0), Math.cos(tilt / 2.0));
            double deepest = 0.0;
            for (int t = 0; t < 100; t++) {
                world.step(TICK, SUBSTEPS, STEP);
                double[] p = new double[3];
                for (int i = 0; i <= 6; i++) {
                    for (int j = 0; j <= 4; j++) {
                        for (int k = 0; k <= 4; k++) {
                            world.point(box, -0.7 + 1.4 * i / 6.0, -0.3 + 0.6 * j / 4.0, -0.35 + 0.7 * k / 4.0, p);
                            double inStep = p[0] > 0.0 && p[1] < 1.0 ? Math.min(p[0], 1.0 - p[1]) : 0.0;
                            double inFloor = p[1] < 0.0 ? -p[1] : 0.0;
                            deepest = Math.max(deepest, Math.max(inStep, inFloor));
                        }
                    }
                }
            }
            assertTrue(deepest < 0.06, "tilt " + tilt + ": the box never sinks into the step or the floor, deepest "
                    + deepest);
        }
    }

    @Test
    void jointedBoxesLyingStillStayPutAndSleep() {
        RigidWorld world = new RigidWorld();
        int a = world.add(10.0, 0.25, 0.125, 0.375);
        int b = world.add(5.0, 0.125, 0.125, 0.375);
        world.place(a, 0.0, 0.125, 0.0, 0.0, 0.0, 0.0, 1.0);
        world.place(b, -0.125, 0.125, -0.75, 0.0, 0.0, 0.0, 1.0);
        double[] back = { 0.0, 0.0, -1.0 };
        double[] up = { 0.0, 1.0, 0.0 };
        world.add(new BallJoint(a, new double[] { -0.125, 0.0, -0.375 }, back, up, b, new double[] { 0.0, 0.0, 0.375 },
                back, up, 1.3, -0.4, 0.4));
        run(world, 40, FLOOR);
        double[] pose = new double[7];
        world.pose(a, pose);
        assertEquals(0.0, pose[0], 0.005, "no creeping sideways");
        assertEquals(0.0, pose[2], 0.005, "no creeping forward");
        assertEquals(1.0, Math.abs(pose[6]), 1.0E-4, "no turning");
        assertTrue(world.sleeping(), "lying still, it sleeps");
    }

    @Test
    void aLimbIsPushedOutOfTheChest() {
        RigidWorld world = new RigidWorld();
        world.gravity = 0.0;
        int chest = world.add(0.0, 0.25, 0.375, 0.125);
        world.place(chest, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        int arm = world.add(1.0, 0.125, 0.375, 0.125);
        world.place(arm, 0.2, 5.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        world.add(new SelfContact(arm, new double[] { 0.0, -0.25, 0.0 }, new double[] { 0.0, 0.25, 0.0 }, 0.125,
                chest));
        run(world, 10, Blocks.NONE);
        double[] pose = new double[7];
        world.pose(arm, pose);
        assertTrue(pose[0] >= 0.25 + 0.125 - 0.02, "the arm stands beside the chest, at x " + pose[0]);
    }

    @Test
    void depthKnowsInsideAndOutside() {
        double[] n = new double[3];
        assertEquals(0.5, SelfContact.depth(0.5, 0.0, 0.0, 1.0, 1.0, 1.0, n), 1.0E-12);
        assertEquals(1.0, n[0]);
        assertEquals(-1.0, SelfContact.depth(0.0, 3.0, 0.0, 1.0, 2.0, 1.0, n), 1.0E-12);
        assertEquals(1.0, n[1]);
    }
}
