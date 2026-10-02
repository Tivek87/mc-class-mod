package nl.tivek.multiversepowers.engine.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class IkTest {
    private static double distance(double[] points, int a, int b) {
        double dx = points[a * 3] - points[b * 3];
        double dy = points[a * 3 + 1] - points[b * 3 + 1];
        double dz = points[a * 3 + 2] - points[b * 3 + 2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    @Test
    void twoBoneReachesAReachableEndWithBothLengths() {
        double[] root = { 0.0, 3.0, 0.0 };
        double[] end = { 0.4, 0.2, 0.3 };
        double[] mid = new double[3];
        Ik.twoBone(root, end, new double[] { 0.0, 0.0, 1.0 }, 1.8, 1.6, mid);
        assertEquals(1.8, length(mid[0] - root[0], mid[1] - root[1], mid[2] - root[2]), 1.0E-9);
        assertEquals(1.6, length(end[0] - mid[0], end[1] - mid[1], end[2] - mid[2]), 1.0E-9);
    }

    @Test
    void twoBoneBendsTowardsThePole() {
        double[] root = { 0.0, 2.0, 0.0 };
        double[] end = { 0.0, 0.0, 0.0 };
        double[] mid = new double[3];
        Ik.twoBone(root, end, new double[] { 0.0, 0.0, 1.0 }, 1.5, 1.5, mid);
        assertTrue(mid[2] > 0.5, "the knee comes forward");
        Ik.twoBone(root, end, new double[] { 0.0, 0.0, -1.0 }, 1.5, 1.5, mid);
        assertTrue(mid[2] < -0.5, "and backward with the pole behind");
    }

    @Test
    void twoBoneNeverSnapsStraightOrShut() {
        double[] root = { 0.0, 0.0, 0.0 };
        double[] mid = new double[3];
        Ik.twoBone(root, new double[] { 0.0, -10.0, 0.0 }, new double[] { 0.0, 0.0, 1.0 }, 1.0, 1.0, mid);
        assertEquals(1.0, length(mid[0], mid[1], mid[2]), 1.0E-9);
        assertTrue(mid[2] > 0.0, "a stretched limb keeps a slight bend");
        Ik.twoBone(root, root, new double[] { 0.0, 0.0, 1.0 }, 1.0, 1.0, mid);
        assertFalse(Double.isNaN(mid[0] + mid[1] + mid[2]));
    }

    @Test
    void twoBoneWithThePoleAlongTheLimbStillPicksASide() {
        double[] mid = new double[3];
        Ik.twoBone(new double[] { 0.0, 0.0, 0.0 }, new double[] { 0.0, 1.5, 0.0 }, new double[] { 0.0, 1.0, 0.0 },
                1.0, 1.0, mid);
        assertFalse(Double.isNaN(mid[0] + mid[1] + mid[2]));
        assertEquals(1.0, length(mid[0], mid[1], mid[2]), 1.0E-9);
    }

    @Test
    void swivelSwingsTheWiderWayFirstAndOnlyAsFarAsItTakes() {
        double[] root = { 0.0, 2.0, 0.0 };
        double[] end = { 0.0, 0.0, 0.0 };
        double[] pole = { 0.0, 0.0, 1.0 };
        double[] knee = new double[3];
        // Bent forward the knee stands sqrt(1.25) off the line; a wall from z 0.5 on lets it be from this far round.
        double clear = Math.acos(0.5 / Math.sqrt(1.25));
        double angle = Ik.swivel(root, end, pole, 1.5, 1.5, -1.3, 1.2, 10, (x, y, z) -> z - 0.5, 0.0, knee);
        assertTrue(angle < 0.0, "the wider way first");
        assertEquals(-clear, angle, 0.003);
        assertTrue(knee[2] <= 0.5 + 1.0E-9, "and clear of the wall");
        assertEquals(1.5, length(knee[0] - root[0], knee[1] - root[1], knee[2] - root[2]), 1.0E-9);
        angle = Ik.swivel(root, end, pole, 1.5, 1.5, -1.2, 1.3, 10, (x, y, z) -> z - 0.5, 0.0, knee);
        assertEquals(clear, angle, 0.003);
        assertEquals(0.0, Ik.swivel(root, end, pole, 1.5, 1.5, -1.3, 1.3, 10, (x, y, z) -> -1.0, 0.0, knee));
    }

    @Test
    void swivelMovesOnSmoothlyAsWhatItKeepsOutOfMoves() {
        double[] root = { 0.0, 2.0, 0.0 };
        double[] end = { 0.0, 0.0, 0.0 };
        double[] pole = { 0.0, 0.0, 1.0 };
        double[] knee = new double[3];
        double was = Double.NaN;
        for (int i = 0; i <= 40; i++) {
            double face = 0.6 - 0.005 * i;
            double angle = Ik.swivel(root, end, pole, 1.5, 1.5, -1.3, 1.3, 10, (x, y, z) -> z - face, 0.0, knee);
            if (!Double.isNaN(was)) {
                assertTrue(Math.abs(angle - was) < 0.02, "no jump at face " + face);
            }
            was = angle;
        }
    }

    @Test
    void swivelWithNoClearWayTakesTheLeastDeep() {
        double[] knee = new double[3];
        double angle = Ik.swivel(new double[] { 0.0, 2.0, 0.0 }, new double[] { 0.0, 0.0, 0.0 },
                new double[] { 0.0, 0.0, 1.0 }, 1.5, 1.5, -1.3, 1.2, 10, (x, y, z) -> z, -1.0, knee);
        assertEquals(-1.3, angle, 1.0E-9);
        assertEquals(Math.sqrt(1.25) * Math.cos(1.3), knee[2], 1.0E-9);
    }

    @Test
    void fabrikReachesAReachableTargetAndKeepsLengthsAndRoot() {
        double[] joints = { 0, 0, 0, 0, 1, 0, 0, 2, 0, 0, 3, 0, 0, 4, 0 };
        double[] lengths = { 1, 1, 1, 1 };
        double gap = Ik.fabrik(joints, lengths, 2.0, 1.5, 1.0, 64, 1.0E-6);
        assertTrue(gap <= 1.0E-6, "gap " + gap);
        for (int i = 0; i < 4; i++) {
            assertEquals(1.0, distance(joints, i, i + 1), 1.0E-9);
        }
        assertEquals(0.0, joints[0]);
        assertEquals(0.0, joints[1]);
        assertEquals(0.0, joints[2]);
    }

    @Test
    void fabrikStretchesStraightAtAnUnreachableTarget() {
        double[] joints = { 0, 0, 0, 0, 1, 0, 0, 2, 0 };
        double gap = Ik.fabrik(joints, new double[] { 1, 1 }, 10.0, 0.0, 0.0, 16, 1.0E-6);
        assertEquals(8.0, gap, 1.0E-9);
        assertEquals(1.0, joints[3], 1.0E-12);
        assertEquals(2.0, joints[6], 1.0E-12);
        assertEquals(0.0, joints[7], 1.0E-12);
    }

    @Test
    void fabrikKeepsEveryBendWithinItsLimit() {
        double[] joints = { 0, 0, 0, 0, 1, 0, 0, 2, 0, 0, 3, 0, 0, 4, 0, 0, 5, 0 };
        double[] lengths = { 1, 1, 1, 1, 1 };
        double limit = Math.toRadians(35.0);
        double[] bends = { limit, limit, limit, limit };
        Ik.fabrik(joints, lengths, bends, -1.0, 0.5, 0.2, 64, 1.0E-6);
        for (int i = 1; i < 5; i++) {
            double px = joints[i * 3] - joints[(i - 1) * 3];
            double py = joints[i * 3 + 1] - joints[(i - 1) * 3 + 1];
            double pz = joints[i * 3 + 2] - joints[(i - 1) * 3 + 2];
            double cx = joints[(i + 1) * 3] - joints[i * 3];
            double cy = joints[(i + 1) * 3 + 1] - joints[i * 3 + 1];
            double cz = joints[(i + 1) * 3 + 2] - joints[i * 3 + 2];
            double cos = (px * cx + py * cy + pz * cz) / (length(px, py, pz) * length(cx, cy, cz));
            assertTrue(Math.acos(Math.min(1.0, cos)) <= limit + 1.0E-9, "bend at joint " + i);
            assertEquals(1.0, distance(joints, i, i + 1), 1.0E-9);
        }
    }

    @Test
    void fabrikStaysSoundOnRandomChainsAndTargets() {
        Random random = new Random(42);
        for (int run = 0; run < 2000; run++) {
            int count = 2 + random.nextInt(8);
            double[] joints = new double[count * 3];
            double[] lengths = new double[count - 1];
            for (int i = 0; i < joints.length; i++) {
                joints[i] = random.nextGaussian();
            }
            for (int i = 0; i < count - 1; i++) {
                lengths[i] = 0.1 + random.nextDouble() * 2.0;
            }
            double gap = Ik.fabrik(joints, lengths, random.nextGaussian() * 4.0, random.nextGaussian() * 4.0,
                    random.nextGaussian() * 4.0, 32, 1.0E-4);
            assertFalse(Double.isNaN(gap));
            for (int i = 0; i < count - 1; i++) {
                assertEquals(lengths[i], distance(joints, i, i + 1), 1.0E-9, "run " + run + " bone " + i);
            }
        }
    }
}
