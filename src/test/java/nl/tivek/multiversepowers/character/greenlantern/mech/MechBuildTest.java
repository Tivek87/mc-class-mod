package nl.tivek.multiversepowers.character.greenlantern.mech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MechBuildTest {
    private static final double STEP = 0.05;

    private static double[] values(MechBuild.Body body) {
        return new double[] { body.sway(), body.rise(), body.hipsYaw(), body.hipsPitch(), body.hipsRoll(), body.turn(),
                body.lean(), body.bank(), body.shrugRight(), body.shrugLeft(), body.lookYaw(), body.lookPitch() };
    }

    @Test
    void theBodyRestsUntilItsHipsFormAndIsBackAtRestWhenTheWalkTakesOver() {
        MechScript.Stage stage = MechScript.Stage.facing(new Vec3(12.5, 64.0, -3.25), 30.0F);
        for (double t = 0.0; t <= MechScript.HIPS; t += STEP) {
            for (double v : values(MechBuild.body(t))) {
                assertEquals(0.0, v, 1.0E-9, "still at " + t);
            }
            assertSame(stage, MechBuild.torso(stage, t), "torso at " + t);
        }
        for (double v : values(MechBuild.body(MechScript.SETTLED))) {
            assertEquals(0.0, v, 1.0E-9, "at rest when settled");
        }
        assertSame(stage, MechBuild.torso(stage, MechScript.SETTLED));
    }

    @Test
    void theBodyNeverJumps() {
        double[] was = values(MechBuild.body(0.0));
        for (double t = STEP; t <= MechScript.SETTLED + 2.0; t += STEP) {
            double[] now = values(MechBuild.body(t));
            for (int i = 0; i < now.length; i++) {
                // A hard jolt moves the hips a few hundredths of a block in a twentieth of a tick, never more.
                assertTrue(Math.abs(now[i] - was[i]) < 0.04, "value " + i + " jumps at tick " + t);
            }
            was = now;
        }
    }

    @Test
    void thePilotsSeatRidesTheTorso() {
        MechScript.Stage stage = MechScript.Stage.facing(new Vec3(0.0, 70.0, 0.0), -60.0F);
        double lowest = 0.0;
        for (double t = MechScript.ABOARD; t < MechScript.SETTLED; t += 1.0) {
            Vec3 seat = MechBuild.torso(stage, t).point(MechScript.COCKPIT);
            lowest = Math.min(lowest, seat.y - stage.point(MechScript.COCKPIT).y);
            // Only digging the head out and winding it back takes the chest far down and over.
            boolean lunging = t > MechScript.REACH + 5 && t < MechScript.TOSS;
            double off = seat.distanceTo(stage.point(MechScript.COCKPIT));
            assertTrue(off < (lunging ? 4.0 : 1.0), "seat " + off + " from its place at " + t);
        }
        assertTrue(lowest < -1.0, "the seat sinks with the lunge: " + lowest);
    }

    @Test
    void theArmsComeToRestAsTheWalkHoldsThem() {
        for (boolean right : new boolean[] { true, false }) {
            MechMoves.Arm built = MechMoves.arm(right, MechScript.Stage.facing(Vec3.ZERO, 0.0F), MechScript.SETTLED);
            MechMoves.Arm walking = MechMoves.walking(right, MechScript.SETTLED, 0.0, 0.0);
            assertEquals(0.0, built.elbow().distanceTo(walking.elbow()), 1.0E-9);
            assertEquals(0.0, built.way().distanceTo(walking.way()), 1.0E-9);
            assertEquals(0.0, built.fold(), 1.0E-9);
            assertEquals(0.0, built.tilt(), 1.0E-9);
        }
    }

    @Test
    void aMirroredHandTiltsTheOtherWay() {
        MechScript.Stage stage = MechScript.Stage.facing(Vec3.ZERO, 0.0F);
        // Up to the head's crash both arms move alike; from there the right one digs the head out and throws it while
        // the left one balances the body.
        for (double t = MechScript.ARMS_FORM; t <= MechScript.CRASH; t += 0.5) {
            MechMoves.Arm right = MechMoves.arm(true, stage, t);
            MechMoves.Arm left = MechMoves.arm(false, stage, t);
            assertEquals(right.fold(), left.fold(), 1.0E-6, "fold at " + t);
            assertEquals(-right.tilt(), left.tilt(), 1.0E-6, "tilt at " + t);
            // The palm's middle mirrors too, wrist turn and all.
            Vec3 hand = right.hand();
            assertEquals(0.0, left.hand().distanceTo(new Vec3(-hand.x, hand.y, hand.z)), 1.0E-6, "hand at " + t);
        }
    }
}
