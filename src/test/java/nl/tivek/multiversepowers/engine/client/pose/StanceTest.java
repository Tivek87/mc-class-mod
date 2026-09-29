package nl.tivek.multiversepowers.engine.client.pose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StanceTest {
    private static final float CLOSE = 1.0E-3F;
    // A limb reaching out straight is held a hair short of it (Ik), so it never snaps.
    private static final float REACH = 0.02F;

    private static HumanoidModel<LivingEntity> person() {
        ModelPart root = LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64)
                .bakeRoot();
        return new HumanoidModel<>(root);
    }

    @BeforeEach
    void inWorld() {
        EntityPass.inWorld(true);
    }

    @AfterEach
    void outOfWorld() {
        EntityPass.inWorld(false);
    }

    // Where a limb's far end lands, as it is drawn: straight to the joint, then turned about the joint's hinge.
    private static Vector3f drawnEnd(ModelPart limb, float upper, float lower, float fold, float hingeX) {
        Quaternionf turn = new Quaternionf().rotationZYX(limb.zRot, limb.yRot, limb.xRot);
        Vector3f joint = turn.transform(new Vector3f(0.0F, upper, 0.0F)).add(limb.x, limb.y, limb.z);
        Vector3f far = new Quaternionf().setAngleAxis(fold, hingeX, 0.0F, 0.0F)
                .transform(new Vector3f(0.0F, lower, 0.0F));
        return turn.transform(far).add(joint);
    }

    @Test
    void aLegPutsItsSoleOnTheTargetWithTheKneeAhead() {
        HumanoidModel<LivingEntity> model = person();
        Vector3f ahead = new Vector3f(0.0F, 0.0F, -1.0F);
        Vector3f[] feet = { new Vector3f(-1.9F, 24.0F, 0.0F), new Vector3f(-1.9F, 21.0F, -2.0F),
                new Vector3f(-3.5F, 20.0F, 2.5F), new Vector3f(-1.9F, 17.0F, 0.0F), new Vector3f(-0.5F, 19.0F, -5.0F) };
        for (Vector3f foot : feet) {
            Stance.leg(model, true, foot, ahead);
            float fold = Limbs.bent(model, Limbs.Joint.RIGHT_KNEE);
            Vector3f end = drawnEnd(model.rightLeg, Stance.THIGH, Stance.SHIN, fold, 1.0F);
            assertEquals(0.0F, end.distance(foot), REACH, "sole for " + foot);
            Vector3f knee = new Quaternionf().rotationZYX(model.rightLeg.zRot, model.rightLeg.yRot,
                    model.rightLeg.xRot).transform(new Vector3f(0.0F, Stance.THIGH, 0.0F));
            float midZ = foot.z * 0.5F + model.rightLeg.z * 0.5F;
            assertTrue(fold < CLOSE || knee.z + model.rightLeg.z <= midZ + CLOSE, "knee ahead for " + foot);
        }
    }

    @Test
    void anArmPutsItsHandOnTheTargetWithTheElbowBack() {
        HumanoidModel<LivingEntity> model = person();
        Vector3f back = new Vector3f(0.3F, 0.0F, 1.0F);
        Vector3f[] hands = { new Vector3f(5.0F, 12.0F, 0.0F), new Vector3f(4.0F, 6.0F, -6.0F),
                new Vector3f(2.0F, 0.0F, -5.0F), new Vector3f(6.0F, 9.0F, -2.0F) };
        for (Vector3f hand : hands) {
            Stance.arm(model, false, hand, back);
            float fold = Limbs.bent(model, Limbs.Joint.LEFT_ELBOW);
            Vector3f end = drawnEnd(model.leftArm, Stance.UPPER_ARM, Stance.FOREARM, fold, -1.0F);
            assertEquals(0.0F, end.distance(hand), REACH, "hand for " + hand);
        }
    }

    @Test
    void theTrunkStaysOnItsHipsHoweverItLeansAndBends() {
        HumanoidModel<LivingEntity> model = person();
        Vector3f hips = new Vector3f(0.0F, 15.0F, 1.0F);
        Quaternionf lean = new Quaternionf().rotationXYZ(0.6F, 0.2F, -0.1F);
        Quaternionf waist = new Quaternionf().rotationX(0.4F);
        Stance.trunk(model, hips, lean, waist);
        Quaternionf chest = new Quaternionf().rotationZYX(model.body.zRot, model.body.yRot, model.body.xRot);
        Vector3f waistAt = chest.transform(new Vector3f(0.0F, 6.0F, 0.0F)).add(model.body.x, model.body.y,
                model.body.z);
        Vector3f bottom = new Quaternionf(chest).mul(new Quaternionf(waist).conjugate())
                .transform(new Vector3f(0.0F, 6.0F, 0.0F)).add(waistAt);
        assertEquals(0.0F, bottom.distance(hips), CLOSE);
        assertEquals(model.body.x, model.head.x, CLOSE);
        assertEquals(model.body.y, model.head.y, CLOSE);
    }
}
