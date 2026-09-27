package nl.tivek.multiversepowers.engine.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JointTest {
    @Test
    void clampKeepsAnAngleInRangeBitForBit() {
        Joint.Turn turn = Joint.x(-0.35, 1.6);
        double angle = 0.1 + 0.2;
        assertEquals(Double.doubleToRawLongBits(angle), Double.doubleToRawLongBits(turn.clamp(angle)));
        assertEquals(-0.35, turn.clamp(-3.0));
        assertEquals(1.6, turn.clamp(9.0));
        assertEquals(-0.35, turn.clamp(-0.35));
        assertEquals(1.6, turn.clamp(1.6));
    }

    @Test
    void clampLetsNaNThroughSoABadValueShows() {
        assertTrue(Double.isNaN(Joint.z(-1.0, 1.0).clamp(Double.NaN)));
    }

    @Test
    void holdsTellsWhetherAnAngleIsInRange() {
        Joint.Turn turn = Joint.y(0.0, 1.0);
        assertTrue(turn.holds(0.5));
        assertTrue(turn.holds(0.0));
        assertFalse(turn.holds(-0.01));
        assertFalse(turn.holds(Double.NaN));
    }

    @Test
    void aRangeMustRunFromMinToMax() {
        assertThrows(IllegalArgumentException.class, () -> Joint.x(1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> Joint.x(Double.NaN, 1.0));
    }

    @Test
    void freeTurnsHaveNoLimit() {
        Joint.Turn turn = Joint.free(Joint.Axis.Z);
        assertEquals(1.0E9, turn.clamp(1.0E9));
        assertEquals(-1.0E9, turn.clamp(-1.0E9));
    }

    @Test
    void axesAreTheBonesOwnUnitAxes() {
        assertEquals(1.0, Joint.Axis.X.x);
        assertEquals(1.0, Joint.Axis.Y.y);
        assertEquals(1.0, Joint.Axis.Z.z);
        assertEquals(0, Joint.FIXED.size());
        Joint knuckle = new Joint(Joint.z(-0.2, 0.2), Joint.x(-0.35, 1.6));
        assertEquals(2, knuckle.size());
        assertEquals(Joint.Axis.X, knuckle.turn(1).axis());
    }
}
