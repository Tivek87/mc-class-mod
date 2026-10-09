package nl.tivek.multiversepowers.character.greenlantern.mech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MechHeadTest {
    private static final double STEP = 0.05;
    private static final MechScript.Stage[] STAGES = {
            MechScript.Stage.facing(new Vec3(12.5, 64.0, -3.25), 30.0F),
            MechScript.Stage.of(new Vec3(-40.0, 80.0, 7.0), new Vec3(1.0, 1.5, 3.6), MechScript.COCKPIT.y,
                    MechScript.COCKPIT.z) };

    @Test
    void theHeadNeverJumpsNorSnapsRound() {
        for (MechScript.Stage stage : STAGES) {
            MechHead.Pose was = MechHead.pose(stage, MechScript.HEAD_FORM);
            for (double t = MechScript.HEAD_FORM + STEP; t <= MechScript.SETTLED; t += STEP) {
                MechHead.Pose now = MechHead.pose(stage, t);
                // Falling out of the sky it covers a few blocks a tick, never more.
                assertTrue(now.at().distanceTo(was.at()) < 0.35, "the head jumps at tick " + t);
                assertTrue(now.ahead().distanceTo(was.ahead()) < 0.12, "the head's face snaps round at tick " + t);
                assertTrue(now.up().distanceTo(was.up()) < 0.12, "the head's top snaps round at tick " + t);
                was = now;
            }
        }
    }

    @Test
    void theHandTakesTheHeadWhereItLies() {
        for (MechScript.Stage stage : STAGES) {
            Vec3 lying = stage.point(MechHead.sunk(stage));
            double miss = MechHead.held(stage, MechScript.GRAB).distanceTo(lying);
            assertTrue(miss < 0.1, "the hand misses the head by " + miss);
        }
    }

    @Test
    void theHeadIsFlungHighAndLandsSquareOnTheNeck() {
        for (MechScript.Stage stage : STAGES) {
            double from = MechHead.pose(stage, MechScript.TOSS).at().y;
            double top = from;
            for (double t = MechScript.TOSS; t < MechScript.LOCK; t += 0.5) {
                top = Math.max(top, MechHead.pose(stage, t).at().y);
            }
            assertTrue(top - from > 8.0, "flung only " + (top - from) + " up");
            MechHead.Pose landing = MechHead.pose(stage, MechScript.LOCK - 1.0E-4);
            MechScript.Stage torso = MechBuild.torso(stage, MechScript.LOCK);
            assertEquals(0.0, landing.at().distanceTo(torso.point(MechScript.NECK.add(0.0, MechScript.HEAD_UP, 0.0))),
                    0.01);
            assertEquals(0.0, landing.ahead().distanceTo(torso.ahead()), 0.01);
            assertEquals(0.0, landing.up().distanceTo(torso.up()), 0.01);
        }
    }

    @Test
    void anglesTurnBackToTheSameAxes() {
        MechScript.Stage stage = STAGES[0];
        for (double yaw = -3.0; yaw <= 3.0; yaw += 0.7) {
            for (double pitch = -1.5; pitch <= 1.5; pitch += 0.6) {
                for (double roll = -3.0; roll <= 3.0; roll += 0.9) {
                    MechHead.Pose pose = MechHead.turned(Vec3.ZERO, stage, yaw, pitch, roll);
                    double[] angles = MechHead.angles(stage, pose.ahead(), pose.up());
                    MechHead.Pose again = MechHead.turned(Vec3.ZERO, stage, angles[0], angles[1], angles[2]);
                    assertEquals(0.0, again.ahead().distanceTo(pose.ahead()), 1.0E-9);
                    assertEquals(0.0, again.up().distanceTo(pose.up()), 1.0E-9);
                }
            }
        }
    }
}
