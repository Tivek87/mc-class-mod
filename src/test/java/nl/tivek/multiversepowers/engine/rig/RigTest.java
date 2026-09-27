package nl.tivek.multiversepowers.engine.rig;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RigTest {
    static Rig finger() {
        Rig.Builder builder = Rig.builder();
        int knuckle = builder.bone("knuckle", -1, -1.12, 3.05, 0.0,
                new Joint(Joint.z(-0.3, 0.3), Joint.x(-0.35, 1.6)));
        int middle = builder.bone("middle", knuckle, 0.0, 1.25, 0.0, new Joint(Joint.x(0.0, 1.9)));
        builder.bone("tip", middle, 0.0, 0.85, 0.0, new Joint(Joint.x(0.0, 1.4)));
        builder.bone("nail", 2, 0.0, 0.72, 0.0, Joint.FIXED);
        return builder.build();
    }

    @Test
    void bonesKeepTheirOrderParentsAndOffsets() {
        Rig rig = finger();
        assertEquals(4, rig.size());
        assertEquals(-1, rig.parent(0));
        assertEquals(0, rig.parent(1));
        assertEquals(2, rig.parent(3));
        assertEquals(2, rig.index("tip"));
        assertEquals("middle", rig.name(1));
        assertArrayEquals(new double[] { -1.12, 3.05, 0.0 },
                new double[] { rig.offsetX(0), rig.offsetY(0), rig.offsetZ(0) });
    }

    @Test
    void everyTurnHasItsOwnValueSlotInOrder() {
        Rig rig = finger();
        assertEquals(4, rig.values());
        assertEquals(0, rig.value(0, 0));
        assertEquals(1, rig.value(0, 1));
        assertEquals(2, rig.value(1, 0));
        assertEquals(3, rig.value(2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> rig.value(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> rig.value(1, 1));
    }

    @Test
    void aBoneComesAfterItsParentAndNamesAreUnique() {
        Rig.Builder builder = Rig.builder();
        builder.bone("a", -1, 0.0, 0.0, 0.0, Joint.FIXED);
        assertThrows(IllegalArgumentException.class, () -> builder.bone("b", 1, 0.0, 0.0, 0.0, Joint.FIXED));
        assertThrows(IllegalArgumentException.class, () -> builder.bone("a", 0, 0.0, 0.0, 0.0, Joint.FIXED));
        assertThrows(IllegalArgumentException.class, () -> builder.bone("c", -2, 0.0, 0.0, 0.0, Joint.FIXED));
        assertThrows(IllegalArgumentException.class, () -> finger().index("thumb"));
    }

    @Test
    void aSocketFindsItsBoneByName() {
        Socket palm = Socket.on(finger(), "tip", 0.0, 0.5, -0.2);
        assertEquals(2, palm.bone());
        assertEquals(-0.2, palm.z());
    }
}
