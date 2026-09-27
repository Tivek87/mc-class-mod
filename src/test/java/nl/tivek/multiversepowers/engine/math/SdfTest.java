package nl.tivek.multiversepowers.engine.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SdfTest {
    @Test
    void sphereIsNegativeInsideZeroOnAndPositiveOutside() {
        assertEquals(-1.0, Sdf.sphere(1, 2, 3, 1, 2, 3, 1.0), 1.0E-12);
        assertEquals(0.0, Sdf.sphere(2, 2, 3, 1, 2, 3, 1.0), 1.0E-12);
        assertEquals(4.0, Sdf.sphere(1, 7, 3, 1, 2, 3, 1.0), 1.0E-12);
    }

    @Test
    void capsuleMeasuresFromItsSegment() {
        assertEquals(0.5, Sdf.capsule(1, 1, 0, 0, 0, 0, 0, 2, 0, 0.5), 1.0E-12);
        assertEquals(0.5, Sdf.capsule(0, 3, 0, 0, 0, 0, 0, 2, 0, 0.5), 1.0E-12);
        assertEquals(0.5, Sdf.capsule(0, -1, 0, 0, 0, 0, 0, 2, 0, 0.5), 1.0E-12);
        assertEquals(-0.5, Sdf.capsule(0, 1, 0, 0, 0, 0, 0, 2, 0, 0.5), 1.0E-12);
        assertEquals(1.0, Sdf.capsule(0, 2, 0, 0, 0, 0, 0, 0, 0, 1.0), 1.0E-12, "a capsule of no length is a ball");
    }

    @Test
    void boxIsExactOutsideAndInside() {
        assertEquals(1.0, Sdf.box(2, 0, 0, 1, 1, 1), 1.0E-12);
        assertEquals(Math.sqrt(2.0), Sdf.box(2, 2, 0, 1, 1, 1), 1.0E-12);
        assertEquals(-0.5, Sdf.box(0.5, 0, 0, 1, 2, 3), 1.0E-12);
        assertEquals(-1.0, Sdf.box(0, 0, 0, 1, 2, 3), 1.0E-12);
    }

    @Test
    void roundBoxRoundsItsCorners() {
        double corner = Sdf.roundBox(2, 2, 2, 1, 1, 1, 0.5);
        double sharp = Sdf.box(2, 2, 2, 1, 1, 1);
        assertTrue(corner > sharp, "the rounded corner lies further in");
        assertEquals(1.0, Sdf.roundBox(2, 0, 0, 1, 1, 1, 0.5), 1.0E-12);
    }
}
