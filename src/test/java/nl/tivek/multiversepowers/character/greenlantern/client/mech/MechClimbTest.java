package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import org.junit.jupiter.api.Test;

class MechClimbTest {
    private static final double[] HEIGHTS = { 2.75, 3.4, 5.0, 7.0, 9.5 };
    private static final double[] EDGES = { 1.625, 2.25, 3.375 };
    // What the server takes from the pilot's game in one tick (MechAssembly.drive).
    private static final double MOST_STRIDE = 1.0;
    private static final double MOST_CLIMB = 3.0;
    private static final double CROUCH = 0.6;
    private static final double ARM = MechScript.UPPER_ARM + MechScript.PALM_ALONG;

    // What the pilot's game packs comes out the same in every other game.
    @Test
    void packedClimbsComeBackTheSame() {
        for (int age = 0; age < 128; age += 9) {
            for (double height : HEIGHTS) {
                for (double edge : EDGES) {
                    int packed = MechClimb.pack(age, height, edge);
                    assertTrue(packed != 0);
                    assertEquals(age, MechClimb.age(packed));
                    assertEquals(MechClimb.quantized(height), MechClimb.height(packed), 1.0E-9);
                    assertEquals(MechClimb.quantized(edge), MechClimb.edge(packed), 1.0E-9);
                }
            }
        }
    }

    // The mech's variant carries a blow, a climb or the build's target, and each comes back on its own.
    @Test
    void variantCarriesABlowAClimbOrATarget() {
        int climb = MechClimb.pack(33, 7.0, 1.75);
        int blow = MechAttacks.pack(MechAttacks.STOMP, 12, 3, 0.4);
        int target = MechScript.variant(false, 123456, 0, 0);
        assertEquals(123456, MechScript.target(target));
        assertEquals(0, MechScript.climb(target));
        assertEquals(MechAttacks.Blow.NONE, MechScript.blow(target));
        int climbing = MechScript.variant(false, -1, 0, climb);
        assertEquals(climb, MechScript.climb(climbing));
        assertEquals(-1, MechScript.target(climbing));
        assertEquals(MechAttacks.Blow.NONE, MechScript.blow(climbing));
        int striking = MechScript.variant(true, -1, blow, climb);
        assertTrue(MechScript.breaking(striking));
        assertEquals(MechAttacks.unpack(blow), MechScript.blow(striking));
        assertEquals(0, MechScript.climb(striking));
        assertEquals(-1, MechScript.target(striking));
    }

    // The base only ever goes up and on, within what the server takes in a tick, and ends on the ledge past its edge;
    // it only moves on over the edge once its hips are up level with the top.
    @Test
    void pathGoesUpAndOverWithinWhatTheServerTakes() {
        for (double height : HEIGHTS) {
            for (double edge : EDGES) {
                int ticks = MechClimb.ticks(height);
                assertEquals(Vec3.ZERO, MechClimb.path(0, height, edge));
                Vec3 end = MechClimb.path(ticks, height, edge);
                assertEquals(height, end.y, 1.0E-9);
                assertTrue(end.z > edge + 1.0, "ends past the edge: " + end.z);
                Vec3 was = Vec3.ZERO;
                for (int age = 1; age <= ticks; age++) {
                    Vec3 at = MechClimb.path(age, height, edge);
                    String where = "height " + height + " edge " + edge + " age " + age;
                    assertTrue(at.y >= was.y - 1.0E-9 && at.z >= was.z - 1.0E-9, where);
                    assertTrue(at.y - was.y <= MOST_CLIMB && at.z - was.z <= MOST_STRIDE, where);
                    if (at.z > 0.05) {
                        assertTrue(at.y + MechScript.HIP.y - CROUCH >= height, where + ": hips under the top");
                    }
                    was = at;
                }
            }
        }
    }

    // The hands take hold after the reach and let go before the shoulders rise out of their reach; the body's crouch
    // and lean start and end at nothing, so the walk goes on from them without a jump.
    @Test
    void handsHoldWhileTheyReachAndTheBodyEndsAsItBegan() {
        for (double height : HEIGHTS) {
            int ticks = MechClimb.ticks(height);
            assertEquals(0.0, MechClimb.grip(0, height), 1.0E-9);
            assertEquals(0.0, MechClimb.grip(ticks, height), 1.0E-9);
            double most = 0.0;
            for (int age = 0; age <= ticks; age++) {
                double grip = MechClimb.grip(age, height);
                most = Math.max(most, grip);
                if (grip > 0.99) {
                    double shoulders = MechClimb.path(age, height, 2.0).y + MechScript.SHOULDER.y - CROUCH;
                    assertTrue(shoulders - (height + MechClimb.GRIP_UP) < ARM, "height " + height + " age " + age);
                }
            }
            assertEquals(1.0, most, 1.0E-9);
            assertEquals(0.0, MechClimb.low(0, height), 1.0E-9);
            assertEquals(0.0, MechClimb.lean(0, height), 1.0E-9);
            assertEquals(0.0, MechClimb.low(ticks, height), 1.0E-9);
            assertEquals(0.0, MechClimb.lean(ticks, height), 1.0E-9);
        }
    }
}
