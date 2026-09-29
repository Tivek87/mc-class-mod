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

    // A thigh hanging from a fixed hip and a shin on a knee that turns about x, bending the shin back (+z) only.
    private static int[] leg(RigidWorld world) {
        int hip = world.add(0.0, 0.2, 0.2, 0.2);
        world.place(hip, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        int thigh = world.add(1.0, 0.12, 0.3, 0.12);
        world.place(thigh, 0.0, 5.5, 0.0, 0.0, 0.0, 0.0, 1.0);
        int shin = world.add(1.0, 0.12, 0.3, 0.12);
        world.place(shin, 0.0, 4.9, 0.0, 0.0, 0.0, 0.0, 1.0);
        world.add(new BallJoint(hip, new double[] { 0.0, -0.2, 0.0 }, new double[] { 0.0, -1.0, 0.0 },
                new double[] { 1.0, 0.0, 0.0 }, thigh, new double[] { 0.0, 0.3, 0.0 }, new double[] { 0.0, -1.0, 0.0 },
                new double[] { 1.0, 0.0, 0.0 }, 1.2, -0.4, 0.4));
        double[] hinge = { -1.0, 0.0, 0.0 };
        double[] bone = { 0.0, -1.0, 0.0 };
        world.add(new HingeJoint(thigh, new double[] { 0.0, -0.3, 0.0 }, hinge, bone, shin,
                new double[] { 0.0, 0.3, 0.0 }, hinge, bone, -0.05, 2.3));
        return new int[] { hip, thigh, shin };
    }

    // The shin's bend about the thigh's hinge, and how far its hinge has left the thigh's.
    private static double[] bend(RigidWorld world, int thigh, int shin) {
        double[] a = new double[3];
        double[] b = new double[3];
        double[] n = new double[3];
        double[] m = new double[3];
        Quat.rotate(world.q, thigh * 4, 0.0, -1.0, 0.0, a, 0);
        Quat.rotate(world.q, shin * 4, 0.0, -1.0, 0.0, b, 0);
        Quat.rotate(world.q, thigh * 4, -1.0, 0.0, 0.0, n, 0);
        Quat.rotate(world.q, shin * 4, -1.0, 0.0, 0.0, m, 0);
        double sin = (a[1] * b[2] - a[2] * b[1]) * n[0] + (a[2] * b[0] - a[0] * b[2]) * n[1]
                + (a[0] * b[1] - a[1] * b[0]) * n[2];
        double cos = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        double off = Math.acos(Math.max(-1.0, Math.min(1.0, n[0] * m[0] + n[1] * m[1] + n[2] * m[2])));
        return new double[] { Math.atan2(sin, cos), off };
    }

    @Test
    void aHingeBendsOneWayOnlyAndStaysWhole() {
        RigidWorld world = new RigidWorld();
        int[] leg = leg(world);
        Random random = new Random(7);
        double[] a = new double[3];
        double[] b = new double[3];
        double least = 0.0;
        double most = 0.0;
        for (int i = 0; i < 200; i++) {
            if (i % 10 == 0) {
                world.velocity(leg[2], random.nextGaussian() * 6.0, random.nextGaussian() * 6.0,
                        random.nextGaussian() * 6.0, random.nextGaussian() * 10.0, random.nextGaussian() * 10.0,
                        random.nextGaussian() * 10.0);
            }
            world.step(TICK, SUBSTEPS, Blocks.NONE);
            world.point(leg[1], 0.0, -0.3, 0.0, a);
            world.point(leg[2], 0.0, 0.3, 0.0, b);
            double gap = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1])
                    + (a[2] - b[2]) * (a[2] - b[2]));
            assertTrue(gap < 0.03, "tick " + i + " knee gap " + gap);
            double[] now = bend(world, leg[1], leg[2]);
            assertTrue(now[1] < 0.15, "tick " + i + " hinge off its axis by " + now[1]);
            least = Math.min(least, now[0]);
            most = Math.max(most, now[0]);
        }
        assertTrue(least >= -0.05 - 0.06, "never bent the wrong way, at most " + least);
        assertTrue(most <= 2.3 + 0.06, "never past its range, at most " + most);
        assertTrue(most > 0.3, "it did bend, up to " + most);
    }

    // A chest held still and a pelvis hanging from its waist, folding ahead (-z) up to 1.0, back up to 0.4, out to the
    // sides up to 0.35 and twisting up to 0.45 either way.
    private static int[] waist(RigidWorld world) {
        int chest = world.add(0.0, 0.25, 0.2, 0.12);
        world.place(chest, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        int pelvis = world.add(1.0, 0.25, 0.2, 0.12);
        world.place(pelvis, 0.0, 5.6, 0.0, 0.0, 0.0, 0.0, 1.0);
        double[] hinge = { 1.0, 0.0, 0.0 };
        double[] bone = { 0.0, -1.0, 0.0 };
        world.add(new SpineJoint(chest, new double[] { 0.0, -0.2, 0.0 }, hinge, bone, pelvis,
                new double[] { 0.0, 0.2, 0.0 }, hinge, bone, -0.4, 1.0, 0.35, 0.45));
        return new int[] { chest, pelvis };
    }

    // The pelvis's fold ahead, its lean to the side and its twist, as seen from the chest.
    private static double[] spine(RigidWorld world, int chest, int pelvis) {
        double[] h = new double[3];
        double[] b = new double[3];
        double[] d = new double[3];
        double[] g = new double[3];
        Quat.rotate(world.q, chest * 4, 1.0, 0.0, 0.0, h, 0);
        Quat.rotate(world.q, chest * 4, 0.0, -1.0, 0.0, b, 0);
        Quat.rotate(world.q, pelvis * 4, 0.0, -1.0, 0.0, d, 0);
        Quat.rotate(world.q, pelvis * 4, 1.0, 0.0, 0.0, g, 0);
        double[] f = { h[1] * b[2] - h[2] * b[1], h[2] * b[0] - h[0] * b[2], h[0] * b[1] - h[1] * b[0] };
        double out = d[0] * h[0] + d[1] * h[1] + d[2] * h[2];
        double along = d[0] * b[0] + d[1] * b[1] + d[2] * b[2];
        double ahead = d[0] * f[0] + d[1] * f[1] + d[2] * f[2];
        double[] n = { b[0] + d[0], b[1] + d[1], b[2] + d[2] };
        double nl = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        for (int k = 0; k < 3; k++) {
            n[k] /= nl;
        }
        double ha = h[0] * n[0] + h[1] * n[1] + h[2] * n[2];
        double ga = g[0] * n[0] + g[1] * n[1] + g[2] * n[2];
        double[] p = { h[0] - n[0] * ha, h[1] - n[1] * ha, h[2] - n[2] * ha };
        double[] q = { g[0] - n[0] * ga, g[1] - n[1] * ga, g[2] - n[2] * ga };
        double sin = (p[1] * q[2] - p[2] * q[1]) * n[0] + (p[2] * q[0] - p[0] * q[2]) * n[1]
                + (p[0] * q[1] - p[1] * q[0]) * n[2];
        double cos = p[0] * q[0] + p[1] * q[1] + p[2] * q[2];
        return new double[] { Math.atan2(ahead, along), Math.atan2(out, Math.sqrt(along * along + ahead * ahead)),
                Math.atan2(sin, cos) };
    }

    @Test
    void aSpineStaysWithinItsFoldLeanAndTwistAndStaysWhole() {
        RigidWorld world = new RigidWorld();
        int[] pair = waist(world);
        Random random = new Random(11);
        double[] a = new double[3];
        double[] b = new double[3];
        double[] least = { 0.0, 0.0, 0.0 };
        double[] most = { 0.0, 0.0, 0.0 };
        for (int i = 0; i < 300; i++) {
            if (i % 10 == 0) {
                world.velocity(pair[1], random.nextGaussian() * 6.0, random.nextGaussian() * 6.0,
                        random.nextGaussian() * 6.0, random.nextGaussian() * 12.0, random.nextGaussian() * 12.0,
                        random.nextGaussian() * 12.0);
            }
            world.step(TICK, SUBSTEPS, Blocks.NONE);
            world.point(pair[0], 0.0, -0.2, 0.0, a);
            world.point(pair[1], 0.0, 0.2, 0.0, b);
            double gap = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1])
                    + (a[2] - b[2]) * (a[2] - b[2]));
            assertTrue(gap < 0.03, "tick " + i + " waist gap " + gap);
            double[] now = spine(world, pair[0], pair[1]);
            for (int k = 0; k < 3; k++) {
                least[k] = Math.min(least[k], now[k]);
                most[k] = Math.max(most[k], now[k]);
            }
        }
        System.out.printf("SPINE fold %.3f..%.3f lean %.3f..%.3f twist %.3f..%.3f%n", least[0], most[0], least[1],
                most[1], least[2], most[2]);
        assertTrue(least[0] >= -0.4 - 0.06 && most[0] <= 1.0 + 0.06, "fold within its range");
        assertTrue(least[1] >= -0.35 - 0.06 && most[1] <= 0.35 + 0.06, "lean within its range");
        assertTrue(least[2] >= -0.45 - 0.06 && most[2] <= 0.45 + 0.06, "twist within its range");
        assertTrue(most[0] > 0.5 && least[0] < -0.2, "it did fold both ways");
        assertTrue(Math.max(most[1], -least[1]) > 0.15 && Math.max(most[2], -least[2]) > 0.15,
                "it did lean and twist");
    }

    @Test
    void aPelvisPushedAheadFoldsTheWaistAhead() {
        RigidWorld world = new RigidWorld();
        int[] pair = waist(world);
        world.velocity(pair[1], 0.0, 0.0, -3.0, 0.0, 0.0, 0.0);
        world.step(TICK, SUBSTEPS, Blocks.NONE);
        world.step(TICK, SUBSTEPS, Blocks.NONE);
        assertTrue(spine(world, pair[0], pair[1])[0] > 0.05, "folded ahead: " + spine(world, pair[0], pair[1])[0]);
    }

    @Test
    void aShinKickedBackBendsTheKneeBackwards() {
        RigidWorld world = new RigidWorld();
        int[] leg = leg(world);
        // The shin's foot kicked towards +z turns the shin back about the knee.
        world.velocity(leg[2], 0.0, 0.0, 3.0, 0.0, 0.0, 0.0);
        world.step(TICK, SUBSTEPS, Blocks.NONE);
        world.step(TICK, SUBSTEPS, Blocks.NONE);
        assertTrue(bend(world, leg[1], leg[2])[0] > 0.05, "bent back: " + bend(world, leg[1], leg[2])[0]);
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
    void theNearestPointOfAnEllipseLiesOnItSquareToItsEdge() {
        double[] out = new double[2];
        double[][] cases = { { 3.0, 0.5, 1.9, 0.45 }, { 0.2, 2.5, 1.2, 0.8 }, { 2.5, 1.5, 1.9, 1.7 }, { 4.0, 0.0, 1.0,
                0.5 }, { 0.0, 3.0, 1.0, 0.5 } };
        for (double[] c : cases) {
            LimbJoint.nearest(c[0], c[1], c[2], c[3], out);
            double on = out[0] * out[0] / (c[2] * c[2]) + out[1] * out[1] / (c[3] * c[3]);
            assertEquals(1.0, on, 1.0E-6, "on the edge");
            // The way out to the point runs along the edge's normal there.
            double nx = out[0] / (c[2] * c[2]);
            double ny = out[1] / (c[3] * c[3]);
            double cross = (c[0] - out[0]) * ny - (c[1] - out[1]) * nx;
            assertEquals(0.0, cross, 1.0E-6, "square to the edge");
        }
    }

    // A fixed trunk and an arm on a LimbJoint whose middle lies ahead of where the arm hangs (+y): kicked every which
    // way, the arm keeps within its ellipse about the middle and its twist from the rest.
    @Test
    void aLimbStaysWithinItsLopsidedReachAndTwist() {
        RigidWorld world = new RigidWorld();
        world.gravity = 0.0;
        int trunk = world.add(0.0, 0.25, 0.375, 0.125);
        int arm = world.add(4.0, 0.125, 0.375, 0.125);
        world.place(trunk, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        world.place(arm, 0.4, 1.625, 0.0, 0.0, 0.0, 0.0, 1.0);
        // Hanging down (-y here); the middle turned 0.8 ahead (-z) about x, the ellipse wide 1.2 that way, 0.5 across.
        double tilt = 0.8;
        double[] middle = { 0.0, -Math.cos(tilt), -Math.sin(tilt) };
        double[] across = { 1.0, 0.0, 0.0 };
        double[] rest = { 0.0, -1.0, 0.0 };
        double[] ahead = { 0.0, 0.0, -1.0 };
        world.add(new LimbJoint(trunk, new double[] { 0.4, 0.0, 0.0 }, middle, across, rest, ahead, arm,
                new double[] { 0.0, 0.375, 0.0 }, new double[] { 0.0, -1.0, 0.0 }, ahead, 1.2, 0.5, -0.4, 0.4));
        Random random = new Random(7);
        double[] q = new double[7];
        double worst = 0.0;
        double worstTwist = 0.0;
        for (int t = 0; t < 300; t++) {
            if (t % 15 == 0) {
                world.velocity(arm, random.nextGaussian() * 3.0, random.nextGaussian() * 3.0,
                        random.nextGaussian() * 3.0, random.nextGaussian() * 12.0, random.nextGaussian() * 12.0,
                        random.nextGaussian() * 12.0);
            }
            world.step(TICK, SUBSTEPS, Blocks.NONE);
            world.pose(arm, q);
            double[] d = new double[3];
            Quat.rotate(q, 3, 0.0, -1.0, 0.0, d, 0);
            double e2x = middle[1] * across[2] - middle[2] * across[1];
            double e2y = middle[2] * across[0] - middle[0] * across[2];
            double e2z = middle[0] * across[1] - middle[1] * across[0];
            double cx = middle[1] * d[2] - middle[2] * d[1];
            double cy = middle[2] * d[0] - middle[0] * d[2];
            double cz = middle[0] * d[1] - middle[1] * d[0];
            double sin = Math.sqrt(cx * cx + cy * cy + cz * cz);
            double angle = Math.atan2(sin, middle[0] * d[0] + middle[1] * d[1] + middle[2] * d[2]);
            if (sin > 1.0E-9) {
                double r1 = angle * (cx * across[0] + cy * across[1] + cz * across[2]) / sin;
                double r2 = angle * (cx * e2x + cy * e2y + cz * e2z) / sin;
                worst = Math.max(worst, r1 * r1 / 1.44 + r2 * r2 / 0.25);
            }
            // The twist: the arm's ahead against the rest's ahead swung the shortest way to where the arm points.
            double[] own = new double[3];
            Quat.rotate(q, 3, 0.0, 0.0, -1.0, own, 0);
            double ax = rest[1] * d[2] - rest[2] * d[1];
            double ay = rest[2] * d[0] - rest[0] * d[2];
            double az = rest[0] * d[1] - rest[1] * d[0];
            double s = Math.sqrt(ax * ax + ay * ay + az * az);
            double c = rest[0] * d[0] + rest[1] * d[1] + rest[2] * d[2];
            double[] v = ahead.clone();
            if (s > 1.0E-9) {
                ax /= s;
                ay /= s;
                az /= s;
                double along = (ax * v[0] + ay * v[1] + az * v[2]) * (1.0 - c);
                double[] w = { ay * v[2] - az * v[1], az * v[0] - ax * v[2], ax * v[1] - ay * v[0] };
                v = new double[] { v[0] * c + w[0] * s + ax * along, v[1] * c + w[1] * s + ay * along,
                        v[2] * c + w[2] * s + az * along };
            }
            double twist = Math.atan2((v[1] * own[2] - v[2] * own[1]) * d[0] + (v[2] * own[0] - v[0] * own[2]) * d[1]
                    + (v[0] * own[1] - v[1] * own[0]) * d[2], v[0] * own[0] + v[1] * own[1] + v[2] * own[2]);
            worstTwist = Math.max(worstTwist, Math.abs(twist));
        }
        assertTrue(worst < 1.15, "within its ellipse, at most " + worst);
        assertTrue(worstTwist < 0.46, "within its twist, at most " + worstTwist);
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

    // Drops a body and returns how far it came off the floor again after it first met it: how high the lowest corner of
    // all its parts rose. Tipping over an edge keeps that edge down and is no bounce.
    private static double bounce(RigidWorld world, int ticks) {
        boolean landed = false;
        double rose = 0.0;
        for (int t = 0; t < ticks; t++) {
            world.step(TICK, SUBSTEPS, FLOOR);
            double lowest = Double.POSITIVE_INFINITY;
            for (int b = 0; b < world.count(); b++) {
                lowest = Math.min(lowest, lowestCorner(world, b));
            }
            landed |= lowest < 0.02;
            if (landed) {
                rose = Math.max(rose, lowest);
            }
        }
        return rose;
    }

    @Test
    void aBoxThrownDownOnTheFloorDoesNotBounce() {
        RigidWorld world = new RigidWorld();
        world.gravity = -32.0;
        int box = world.add(1.0, 0.3, 0.3, 0.3);
        world.place(box, 0.0, 2.0, 0.0, 0.1, 0.0, 0.05, 1.0);
        world.velocity(box, 3.0, -18.0, 0.0, 2.0, 0.0, 1.0);
        double rose = bounce(world, 60);
        assertTrue(rose < 0.02, "it lands and stays down, rose " + rose);
    }

    @Test
    void aLimpBodyThrownOnTheFloorDoesNotBounce() {
        RigidWorld world = new RigidWorld();
        world.gravity = -32.0;
        world.friction = 0.8;
        world.angularDamping = 1.6;
        int trunk = world.add(15.0, 0.25, 0.375, 0.125);
        int leg = world.add(6.0, 0.125, 0.375, 0.125);
        int arm = world.add(4.0, 0.125, 0.375, 0.125);
        world.place(trunk, 0.0, 2.2, 0.0, 0.3, 0.0, 0.2, 0.93);
        world.place(leg, 0.12, 1.45, 0.0, 0.3, 0.0, 0.2, 0.93);
        world.place(arm, 0.45, 2.3, 0.0, 0.3, 0.0, 0.2, 0.93);
        double[] down = { 0.0, -1.0, 0.0 };
        double[] ahead = { 0.0, 0.0, 1.0 };
        world.add(new BallJoint(trunk, new double[] { 0.12, -0.375, 0.0 }, down, ahead, leg,
                new double[] { 0.0, 0.375, 0.0 }, down, ahead, 1.3, -0.4, 0.4));
        world.add(new BallJoint(trunk, new double[] { 0.3, 0.3, 0.0 }, down, ahead, arm,
                new double[] { 0.0, 0.3, 0.0 }, down, ahead, 2.4, -1.4, 1.4));
        for (int b : new int[] { trunk, leg, arm }) {
            world.velocity(b, 6.0, -12.0, 1.0, 3.0, 1.0, -2.0);
        }
        double rose = bounce(world, 100);
        assertTrue(rose < 0.02, "the body comes down and stays down, rose " + rose);
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
