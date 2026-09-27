package nl.tivek.multiversepowers.engine.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StrandsTest {
    private static final double TICK = 0.05;
    private static final int SUBSTEPS = 10;

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

    @Test
    void aPinnedRopeHangsStraightDownAndKeepsItsLength() {
        Strands rope = Strands.rope(8, 2.1, 0.0, 5.0, 0.0);
        for (int i = 0; i < 6; i++) {
            rope.place(i + 1, 0.3 * (i + 1), 5.0, 0.0);
        }
        rope.pin(0, 0.0, 5.0, 0.0, TICK);
        for (int t = 0; t < 200; t++) {
            rope.pin(0, 0.0, 5.0, 0.0, TICK);
            rope.step(TICK, SUBSTEPS, Blocks.NONE);
        }
        double[] end = new double[3];
        rope.point(7, 1.0, end);
        assertEquals(0.0, end[0], 0.05, "the end hangs under the pin");
        assertEquals(5.0 - 2.1, end[1], 0.05, "a rope's full length below it");
        for (int i = 1; i < 8; i++) {
            assertEquals(0.3, rope.gap(i - 1, i), 0.01, "each link keeps its length");
        }
    }

    @Test
    void aRopeFallsOntoTheFloorAndStaysOnIt() {
        Strands rope = Strands.rope(6, 1.5, 0.0, 3.0, 0.0);
        for (int i = 0; i < 6; i++) {
            rope.place(i, 0.25 * i, 3.0 - 0.2 * i, 0.0);
        }
        for (int t = 0; t < 120; t++) {
            rope.step(TICK, SUBSTEPS, FLOOR);
        }
        double[] p = new double[3];
        for (int i = 0; i < 6; i++) {
            rope.point(i, 1.0, p);
            assertTrue(p[1] >= -1.0E-3 && p[1] < 0.2, "point " + i + " lies on the floor: " + p[1]);
        }
    }

    @Test
    void clothCarriedAlongTrailsBehind() {
        Strands cloth = Strands.cloth(6, 9, 0.625, 1.0, -0.3125, 2.0, 0.0);
        cloth.drag = 6.0;
        double z = 0.0;
        for (int t = 0; t < 60; t++) {
            z += 0.28;
            for (int c = 0; c < 6; c++) {
                cloth.pin(c, -0.3125 + 0.125 * c, 2.0, z, TICK);
            }
            cloth.step(TICK, 8, Blocks.NONE);
        }
        double[] bottom = new double[3];
        cloth.point(51, 1.0, bottom);
        double behind = z - bottom[2];
        double below = 2.0 - bottom[1];
        double angle = Math.toDegrees(Math.atan2(behind, below));
        System.out.println("TRAIL behind " + behind + " below " + below + " angle " + angle);
        assertTrue(angle > 35.0, "running lifts the cloth well out behind: " + angle);
    }

    @Test
    void clothHangsFromItsTopEdgeAndStaysOutOfABody() {
        Strands cloth = Strands.cloth(5, 8, 0.6, 1.0, -0.3, 2.0, 0.0);
        for (int t = 0; t < 100; t++) {
            for (int c = 0; c < 5; c++) {
                cloth.pin(c, -0.3 + 0.15 * c, 2.0, 0.0, TICK);
            }
            cloth.clearCapsules();
            cloth.capsule(0.0, 1.8, 0.35, 0.0, 0.8, 0.35, 0.3);
            cloth.step(TICK, SUBSTEPS, Blocks.NONE);
        }
        double[] p = new double[3];
        for (int i = 5; i < 40; i++) {
            cloth.point(i, 1.0, p);
            double dz = p[2] - 0.35;
            double dy = Math.max(0.8, Math.min(1.8, p[1])) - p[1];
            double dx = p[0];
            assertTrue(Math.sqrt(dx * dx + dy * dy + dz * dz) > 0.28, "point " + i + " stays out of the body");
        }
        cloth.point(37, 1.0, p);
        assertTrue(p[1] < 1.3, "the bottom hangs well below the top: " + p[1]);
    }
}
