package nl.tivek.multiversepowers.engine.client.ragdoll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.tivek.multiversepowers.engine.client.ragdoll.getup.GetUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class KnockedTest {
    private static final int CREATURE = 7;
    // As long as the server keeps a thrown creature down from its landing (Knockdowns).
    private static final int DOWN = 125;

    @AfterEach
    void forget() {
        Knocked.clear();
    }

    // Ticks from the server's word until the body gets up, the body coming down `late` ticks after its creature.
    private static int getsUpAfter(int late, boolean person) {
        Knocked.told(CREATURE, DOWN);
        int down = 0;
        int lain = 0;
        for (int t = 1; t <= 400; t++) {
            Knocked.tick();
            down++;
            if (t > late) {
                lain++;
            }
            if (Knocked.getsUp(CREATURE, down, lain, person)) {
                return t;
            }
        }
        return -1;
    }

    @Test
    void aBodyLiesThreeSecondsOnTheGroundAndStandsBeforeItsCreatureMoves() {
        for (boolean person : new boolean[] { true, false }) {
            for (int late = 0; late <= 12; late += 4) {
                Knocked.clear();
                int up = getsUpAfter(late, person);
                assertTrue(up - late >= Knocked.LIES, "late " + late + ": lay " + (up - late) + " ticks");
                assertTrue(up + GetUp.ticks(person) < DOWN, "late " + late + ": stands at " + (up + GetUp.ticks(person))
                        + ", its creature moves at " + DOWN);
            }
        }
    }

    @Test
    void aBodyNeverHeldDownGetsUpAtOnce() {
        assertEquals(6, getsUpAfterNoWord());
    }

    private static int getsUpAfterNoWord() {
        for (int down = 1; down < 100; down++) {
            if (Knocked.getsUp(CREATURE, down, 0, true)) {
                return down;
            }
        }
        return -1;
    }

    // Word that the server sent a creature flying again (a blow as it got up) counts once, on the tick it came.
    @Test
    void wordOfBeingDownedAgainCountsOnceAndOnlyAtOnce() {
        Knocked.told(CREATURE, -1);
        Knocked.tick();
        assertTrue(Knocked.again(CREATURE));
        assertFalse(Knocked.again(CREATURE));
        Knocked.told(CREATURE, -1);
        Knocked.tick();
        Knocked.tick();
        assertFalse(Knocked.again(CREATURE));
    }

    @Test
    void aBodyStillFlyingByTheServerWaitsForWord() {
        Knocked.told(CREATURE, -1);
        assertFalse(Knocked.getsUp(CREATURE, 150, 150, true));
        assertTrue(Knocked.down(CREATURE));
    }
}
