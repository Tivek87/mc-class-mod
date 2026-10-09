package nl.tivek.multiversepowers.character.thor.hammer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import nl.tivek.multiversepowers.character.thor.ThorPowers;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import org.junit.jupiter.api.Test;

class HammerRulesTest {
    @Test
    void drawGoesFromShortestToFullAndNoFurther() {
        assertEquals(HammerRules.SHORTEST, HammerRules.drawn(0, 14.0), 1.0E-12);
        assertEquals(HammerRules.SHORTEST, HammerRules.drawn(HammerRules.DRAW_FROM, 14.0), 1.0E-12);
        assertEquals(9.0, HammerRules.drawn((HammerRules.DRAW_FROM + HammerRules.DRAW_FULL) / 2, 14.0), 1.0E-12);
        assertEquals(14.0, HammerRules.drawn(HammerRules.DRAW_FULL, 14.0), 1.0E-12);
        assertEquals(14.0, HammerRules.drawn(HammerRules.DRAW_LONGEST, 14.0), 1.0E-12);
        assertEquals(HammerRules.SHORTEST, HammerRules.drawn(HammerRules.DRAW_FULL, 1.0), 1.0E-12,
                "a reach set below the shortest throw still throws the shortest");
    }

    @Test
    void dashGetsGoingBrakesAndNeverPassesTheHammer() {
        for (double pace : new double[] {0.25, 1.0, 3.0}) {
            double gap = 20.0;
            int age = 0;
            assertEquals(pace / HammerRules.RAMP, HammerRules.dashStep(0, gap, pace), 1.0E-12, "the first tick");
            while (gap > 0.0 && age < 1000) {
                double step = HammerRules.dashStep(age, gap, pace);
                assertTrue(step > 0.0 && step <= gap, "a step forward, never past it");
                assertTrue(step <= pace + 1.0E-12, "never faster than its pace");
                assertTrue(step <= Math.max(0.05, Math.sqrt(2.0 * HammerRules.BRAKE * gap)) + 1.0E-12,
                        "slow enough to brake into the catch");
                gap -= step;
                age++;
            }
            assertEquals(0.0, gap, 1.0E-9, "it gets there");
        }
    }

    @Test
    void aStoppedThrowRestsByTheFaceItHit() {
        assertEquals(ThrownHammer.HANGING, HammerRules.restOn(null));
        assertEquals(ThrownHammer.LYING, HammerRules.restOn(Direction.UP));
        assertEquals(ThrownHammer.STUCK, HammerRules.restOn(Direction.NORTH));
        assertEquals(ThrownHammer.STUCK, HammerRules.restOn(Direction.DOWN));
    }

    @Test
    void flagsAndNeedsAreBitsOfTheirOwn() {
        bits(ThorStatePayload.FLYING, ThorStatePayload.FLOATING, ThorStatePayload.LIGHTNING, ThorStatePayload.CARRYING,
                ThorStatePayload.CHARGING, ThorStatePayload.ARMED, ThorStatePayload.THROWN, ThorStatePayload.CHARGED,
                ThorStatePayload.HAMMER_CHARGED, ThorStatePayload.STORMING, ThorStatePayload.PULLING,
                ThorStatePayload.CALLING, ThorStatePayload.COCKED);
        bits(ThorPowers.UNARMED, ThorPowers.ARMED, ThorPowers.WALKING, ThorPowers.SPRINTING, ThorPowers.HOME,
                ThorPowers.AWAY);
    }

    private static void bits(int... flags) {
        int seen = 0;
        for (int flag : flags) {
            assertEquals(1, Integer.bitCount(flag), "one bit: " + flag);
            assertEquals(0, seen & flag, "not shared: " + flag);
            seen |= flag;
        }
    }
}
