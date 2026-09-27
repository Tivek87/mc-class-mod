package nl.tivek.multiversepowers.engine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;

class SegmentsTest {
    private final double[] out = new double[2];

    @Test
    void crossingSegmentsTouch() {
        double d = Segments.closest(new double[] { -1, 0, 0 }, new double[] { 1, 0, 0 }, new double[] { 0, -1, 0 },
                new double[] { 0, 1, 0 }, this.out);
        assertEquals(0.0, d, 1.0E-24);
        assertEquals(0.5, this.out[0], 1.0E-12);
        assertEquals(0.5, this.out[1], 1.0E-12);
    }

    @Test
    void skewSegmentsMeetAtTheirCommonNormal() {
        double d = Segments.closest(new double[] { -1, 0, 0 }, new double[] { 1, 0, 0 }, new double[] { 0, -1, 2 },
                new double[] { 0, 1, 2 }, this.out);
        assertEquals(4.0, d, 1.0E-12);
    }

    @Test
    void parallelSegmentsKeepTheirGap() {
        double d = Segments.closest(new double[] { 0, 0, 0 }, new double[] { 2, 0, 0 }, new double[] { 1, 3, 0 },
                new double[] { 3, 3, 0 }, this.out);
        assertEquals(9.0, d, 1.0E-12);
    }

    @Test
    void endsAreClampedToTheSegments() {
        double d = Segments.closest(new double[] { 0, 0, 0 }, new double[] { 1, 0, 0 }, new double[] { 3, 0, 0 },
                new double[] { 4, 0, 0 }, this.out);
        assertEquals(4.0, d, 1.0E-12);
        assertEquals(1.0, this.out[0], 1.0E-12);
        assertEquals(0.0, this.out[1], 1.0E-12);
    }

    @Test
    void pointsWork() {
        double d = Segments.closest(new double[] { 1, 1, 1 }, new double[] { 1, 1, 1 }, new double[] { 1, 1, 3 },
                new double[] { 1, 1, 3 }, this.out);
        assertEquals(4.0, d, 1.0E-12);
    }

    @Test
    void agreesWithBruteForceOnRandomSegments() {
        Random random = new Random(7);
        for (int run = 0; run < 500; run++) {
            double[][] p = new double[4][3];
            for (double[] point : p) {
                for (int i = 0; i < 3; i++) {
                    point[i] = random.nextGaussian();
                }
            }
            double fast = Segments.closest(p[0], p[1], p[2], p[3], this.out);
            double best = Double.MAX_VALUE;
            for (int i = 0; i <= 200; i++) {
                for (int j = 0; j <= 200; j++) {
                    double s = i / 200.0;
                    double t = j / 200.0;
                    double dx = p[0][0] + (p[1][0] - p[0][0]) * s - p[2][0] - (p[3][0] - p[2][0]) * t;
                    double dy = p[0][1] + (p[1][1] - p[0][1]) * s - p[2][1] - (p[3][1] - p[2][1]) * t;
                    double dz = p[0][2] + (p[1][2] - p[0][2]) * s - p[2][2] - (p[3][2] - p[2][2]) * t;
                    best = Math.min(best, dx * dx + dy * dy + dz * dz);
                }
            }
            assertEquals(Math.sqrt(best), Math.sqrt(fast), 0.03, "run " + run);
            assertEquals(true, fast <= best + 1.0E-12, "run " + run);
        }
    }
}
