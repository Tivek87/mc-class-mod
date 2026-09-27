package nl.tivek.multiversepowers.engine.client.pose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.junit.jupiter.api.Test;

class PoseGuardTest {
    private static ModelPart person() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot();
    }

    @Test
    void posesThatClearStayExactlyAsTheyWere() {
        ModelPart root = person();
        ModelPart body = root.getChild("body");
        ModelPart arm = root.getChild("right_arm");
        ModelPart leftArm = root.getChild("left_arm");
        ModelPart rightLeg = root.getChild("right_leg");
        ModelPart leftLeg = root.getChild("left_leg");
        float[][] arms = { { 0.0F, 0.0F, 0.0F }, { -1.0F, 0.0F, 0.1F }, { 1.0F, 0.0F, 0.1F },
                { -1.5708F, -0.1F, 0.0F }, { -1.5708F, 0.4F, 0.0F }, { -3.1F, 0.0F, -0.3F }, { 0.0F, 0.0F, 1.2F },
                { -1.5708F, -0.6F, 0.0F }, { -1.9F, -0.7F, 0.0F }, { -1.2F, -0.6F, 0.0F } };
        for (float[] pose : arms) {
            arm.setRotation(pose[0], pose[1], pose[2]);
            leftArm.setRotation(pose[0], -pose[1], -pose[2]);
            PoseGuard.out(arm, body);
            PoseGuard.out(leftArm, body);
            assertEquals(pose[2], arm.zRot, "right arm " + java.util.Arrays.toString(pose));
            assertEquals(-pose[2], leftArm.zRot, "left arm " + java.util.Arrays.toString(pose));
        }
        for (float swing : new float[] { 0.0F, 0.6F, 1.0F }) {
            rightLeg.setRotation(swing, 0.0F, 0.0F);
            leftLeg.setRotation(-swing, 0.0F, 0.0F);
            PoseGuard.apart(rightLeg, leftLeg);
            assertEquals(0.0F, rightLeg.zRot);
            assertEquals(0.0F, leftLeg.zRot);
        }
    }

    @Test
    void anArmThroughTheChestIsTurnedOutOfIt() {
        ModelPart root = person();
        ModelPart body = root.getChild("body");
        ModelPart arm = root.getChild("right_arm");
        arm.setRotation(0.0F, 0.0F, -0.9F);
        assertTrue(PoseGuard.depth(arm, body) > 1.0F, "the arm starts deep in the chest");
        PoseGuard.out(arm, body);
        assertNotEquals(-0.9F, arm.zRot);
        assertTrue(PoseGuard.depth(arm, body) <= 0.75F, "and ends outside: " + PoseGuard.depth(arm, body));
        assertTrue(arm.zRot > -0.9F, "turned back out the way it came, not all round: " + arm.zRot);
    }

    @Test
    void crossedLegsAreTurnedApart() {
        ModelPart root = person();
        ModelPart right = root.getChild("right_leg");
        ModelPart left = root.getChild("left_leg");
        right.setRotation(0.0F, 0.0F, -0.5F);
        left.setRotation(0.0F, 0.0F, 0.5F);
        assertTrue(PoseGuard.overlap(right, left) > 0.75F, "the legs start crossed");
        PoseGuard.apart(right, left);
        assertTrue(PoseGuard.overlap(right, left) <= 0.75F, "and end apart: " + PoseGuard.overlap(right, left));
    }
}
