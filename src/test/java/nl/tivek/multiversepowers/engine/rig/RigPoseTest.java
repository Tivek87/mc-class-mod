package nl.tivek.multiversepowers.engine.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RigPoseTest {
    @Test
    void setStoresWhatTheJointAllows() {
        RigPose pose = new RigPose(RigTest.finger());
        assertEquals(1.6, pose.set(0, 1, 3.0));
        assertEquals(1.6, pose.get(0, 1));
        assertEquals(0.25, pose.set(0, 0, 0.25));
        assertEquals(0.0, pose.set(1, 0, -1.0));
    }

    @Test
    void restIsZeroOrTheNearestEndOfTheRange() {
        Rig.Builder builder = Rig.builder();
        builder.bone("bent", -1, 0.0, 0.0, 0.0, new Joint(Joint.x(0.2, 1.0), Joint.z(-1.0, -0.5)));
        RigPose pose = new RigPose(builder.build());
        assertEquals(0.2, pose.get(0, 0));
        assertEquals(-0.5, pose.get(0, 1));
    }

    @Test
    void lerpRunsFromOnePoseToTheOther() {
        Rig rig = RigTest.finger();
        RigPose open = new RigPose(rig);
        RigPose fist = new RigPose(rig);
        fist.set(0, 1, 1.6);
        fist.set(1, 0, 1.8);
        RigPose half = new RigPose(rig);
        half.lerp(open, fist, 0.5);
        assertEquals(0.8, half.get(0, 1), 1.0E-15);
        assertEquals(0.9, half.get(1, 0), 1.0E-15);
        half.lerp(open, fist, 1.0);
        assertEquals(1.8, half.get(1, 0));
    }

    @Test
    void blendMovesEachBoneByItsOwnWeight() {
        Rig rig = RigTest.finger();
        RigPose pose = new RigPose(rig);
        RigPose other = new RigPose(rig);
        other.set(0, 1, 1.0);
        other.set(1, 0, 1.0);
        other.set(2, 0, 1.0);
        pose.blend(other, new double[] { 1.0, 0.5, 0.0, 1.0 });
        assertEquals(1.0, pose.get(0, 1));
        assertEquals(0.5, pose.get(1, 0));
        assertEquals(0.0, pose.get(2, 0));
        assertThrows(IllegalArgumentException.class, () -> pose.blend(other, new double[] { 1.0 }));
    }

    @Test
    void posesOfDifferentRigsDoNotMix() {
        RigPose a = new RigPose(RigTest.finger());
        RigPose b = new RigPose(RigTest.finger());
        assertThrows(IllegalArgumentException.class, () -> a.copy(b));
    }
}
