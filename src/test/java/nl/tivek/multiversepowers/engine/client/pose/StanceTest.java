package nl.tivek.multiversepowers.engine.client.pose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import org.joml.Matrix3f;
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
        Limbs.onRenderLivingPost(null);
        EntityPass.inWorld(false);
    }

    // The turn of a bent part's piece past joint `joint`, in the axes of the piece before it (none: straight).
    private static Quaternionf piece(ModelPart part, int joint) {
        Matrix3f turn = BentParts.turn(part, joint);
        return turn == null ? new Quaternionf() : turn.getNormalizedRotation(new Quaternionf());
    }

    // A limb as it is drawn: its pivot, then each piece (lengths along its bone) turned at the joint before it. Where
    // its far end lands, and in `last` how its last piece is turned in model space.
    private static Vector3f drawnEnd(ModelPart limb, float[] lengths, Quaternionf last) {
        last.rotationZYX(limb.zRot, limb.yRot, limb.xRot);
        Vector3f at = new Vector3f(limb.x, limb.y, limb.z);
        for (int i = 0; i < lengths.length; i++) {
            if (i > 0) {
                last.mul(piece(limb, i - 1));
            }
            at.add(last.transform(new Vector3f(0.0F, lengths[i], 0.0F)));
        }
        return at;
    }

    private static final float[] LEG = { Stance.THIGH, Stance.SHIN - Stance.FOOT, Stance.FOOT };
    private static final float[] ARM = { Stance.UPPER_ARM, Stance.FOREARM - Stance.HAND, Stance.HAND };

    @Test
    void aLegPutsItsSoleOnTheTargetWithTheKneeAheadAndTheSoleFlat() {
        Vector3f ahead = new Vector3f(0.0F, 0.0F, -1.0F);
        Vector3f[] feet = { new Vector3f(-1.9F, 24.0F, 0.0F), new Vector3f(-1.9F, 21.0F, -2.0F),
                new Vector3f(-3.5F, 20.0F, 2.5F), new Vector3f(-1.9F, 17.0F, 0.0F), new Vector3f(-0.5F, 19.0F, -5.0F) };
        for (Vector3f foot : feet) {
            HumanoidModel<LivingEntity> model = person();
            model.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
            Stance.leg(model, true, foot, ahead);
            Quaternionf last = new Quaternionf();
            Vector3f end = drawnEnd(model.rightLeg, LEG, last);
            assertEquals(0.0F, end.distance(foot), REACH, "sole for " + foot);
            Vector3f way = last.transform(new Vector3f(0.0F, 1.0F, 0.0F));
            float knee = Limbs.bent(model, Limbs.Joint.RIGHT_KNEE);
            Quaternionf shin = new Quaternionf();
            drawnEnd(model.rightLeg, new float[] { Stance.THIGH, Stance.SHIN - Stance.FOOT }, shin);
            // A shin tilted further than an ankle turns, or a knee folded shut, leaves the sole short of flat.
            float tilt = (float) Math.acos(Math.min(1.0F, shin.transform(new Vector3f(0.0F, 1.0F, 0.0F)).y));
            assertTrue(tilt > 0.95F || way.y > 0.999F, "flat sole for " + foot + ": " + way);
            Vector3f kneeAt = new Quaternionf().rotationZYX(model.rightLeg.zRot, model.rightLeg.yRot,
                    model.rightLeg.xRot).transform(new Vector3f(0.0F, Stance.THIGH, 0.0F));
            float midZ = foot.z * 0.5F + model.rightLeg.z * 0.5F;
            assertTrue(knee < CLOSE || kneeAt.z + model.rightLeg.z <= midZ + CLOSE, "knee ahead for " + foot);
            Limbs.onRenderLivingPost(null);
        }
    }

    @Test
    void aFootKeepsTheWayItsLegSteppedOutAsTheKneeBends() {
        HumanoidModel<LivingEntity> model = person();
        model.rightLeg.setRotation(-0.4F, 0.0F, 0.0F);
        Quaternionf stepped = new Quaternionf().rotationX(-0.4F);
        Vector3f foot = Stance.end(model.rightLeg, Stance.THIGH + Stance.SHIN, new Vector3f()).add(0.0F, -3.0F, 0.0F);
        Stance.leg(model, true, foot, new Vector3f(0.0F, 0.0F, -1.0F));
        Quaternionf last = new Quaternionf();
        assertEquals(0.0F, drawnEnd(model.rightLeg, LEG, last).distance(foot), REACH);
        assertTrue(Limbs.bent(model, Limbs.Joint.RIGHT_KNEE) > 0.3F);
        Vector3f way = last.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        assertEquals(0.0F, way.distance(stepped.transform(new Vector3f(0.0F, 1.0F, 0.0F))), CLOSE);
    }

    @Test
    void aFootAPoseStoodFlatStaysFlatWhenStanceReachesTheLegAgain() {
        HumanoidModel<LivingEntity> model = person();
        model.rightLeg.setRotation(-0.6F, 0.0F, 0.0F);
        Limbs.bend(model, Limbs.Joint.RIGHT_KNEE, 0.9F);
        Stance.flat(model, true, 1.0F);
        Quaternionf last = new Quaternionf();
        Vector3f foot = drawnEnd(model.rightLeg, LEG, last);
        assertTrue(last.transform(new Vector3f(0.0F, 1.0F, 0.0F)).y > 0.999F, "stood flat");
        foot.add(0.0F, -1.0F, 0.0F);
        Stance.leg(model, true, foot, new Vector3f(0.0F, 0.0F, -1.0F));
        assertEquals(0.0F, drawnEnd(model.rightLeg, LEG, last).distance(foot), REACH);
        Vector3f way = last.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        assertTrue(way.y > 0.999F, "still flat: " + way);
    }

    @Test
    void anArmPutsItsHandOnTheTargetWithTheElbowBack() {
        Vector3f back = new Vector3f(0.3F, 0.0F, 1.0F);
        Vector3f[] hands = { new Vector3f(5.0F, 12.0F, 0.0F), new Vector3f(4.0F, 6.0F, -6.0F),
                new Vector3f(2.0F, 0.0F, -5.0F), new Vector3f(6.0F, 9.0F, -2.0F), new Vector3f(5.0F, -8.0F, -1.0F) };
        for (Vector3f hand : hands) {
            HumanoidModel<LivingEntity> model = person();
            Stance.arm(model, false, hand, back);
            Quaternionf last = new Quaternionf();
            Vector3f end = drawnEnd(model.leftArm, ARM, last);
            assertEquals(0.0F, end.distance(hand), REACH, "hand for " + hand);
            assertEquals(0.0F, last.transform(new Vector3f(0.0F, 1.0F, 0.0F)).distance(
                    end.sub(drawnEnd(model.leftArm, new float[] { Stance.UPPER_ARM, Stance.FOREARM - Stance.HAND },
                            new Quaternionf()), new Vector3f()).normalize()), CLOSE, "hand in line for " + hand);
            Limbs.onRenderLivingPost(null);
        }
    }

    @Test
    void aHandTurnsToItsPalmAsFarAsItsWrist() {
        HumanoidModel<LivingEntity> model = person();
        Vector3f hand = new Vector3f(-1.5F, 6.0F, -7.0F);
        Quaternionf palm = new Quaternionf().rotationX(-(float) Math.PI * 0.5F);
        Stance.arm(model, true, hand, new Vector3f(-0.3F, 0.0F, 1.0F), palm, 1.0F);
        Quaternionf last = new Quaternionf();
        assertEquals(0.0F, drawnEnd(model.rightArm, ARM, last).distance(hand), REACH + 0.05F);
        Vector3f fingers = last.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        assertEquals(0.0F, fingers.distance(new Vector3f(0.0F, 0.0F, -1.0F)), 0.02F, "fingers ahead: " + fingers);
    }

    @Test
    void aShoulderRisesUnderAnArmOverheadAndComesAheadWithAPunch() {
        HumanoidModel<LivingEntity> model = person();
        float hangsAt = model.rightArm.y;
        Stance.arm(model, true, new Vector3f(-5.0F, -8.0F, 0.0F), new Vector3f(0.0F, 0.0F, 1.0F));
        assertTrue(model.rightArm.y < hangsAt - 0.8F, "shrugged: " + model.rightArm.y);
        Limbs.onRenderLivingPost(null);
        model = person();
        Stance.arm(model, true, new Vector3f(-5.0F, 2.0F, -10.0F), new Vector3f(0.0F, 0.0F, 1.0F));
        assertTrue(model.rightArm.z < -0.8F, "ahead: " + model.rightArm.z);
        Limbs.onRenderLivingPost(null);
        model = person();
        Stance.arm(model, true, new Vector3f(-5.0F, 12.0F, 0.0F), new Vector3f(0.0F, 0.0F, 1.0F));
        assertEquals(hangsAt, model.rightArm.y, CLOSE, "hanging");
        assertEquals(0.0F, model.rightArm.z, CLOSE, "hanging");
    }

    @Test
    void theTrunkStaysOnItsHipsHoweverItLeansAndBends() {
        HumanoidModel<LivingEntity> model = person();
        Vector3f hips = new Vector3f(0.0F, 15.0F, 1.0F);
        Quaternionf lean = new Quaternionf().rotationXYZ(0.6F, 0.2F, -0.1F);
        Quaternionf waist = new Quaternionf().rotationX(0.4F);
        Stance.trunk(model, hips, lean, waist);
        Quaternionf chest = new Quaternionf().rotationZYX(model.body.zRot, model.body.yRot, model.body.xRot);
        Quaternionf belly = new Quaternionf(chest).mul(piece(model.body, 0));
        Quaternionf pelvis = new Quaternionf(belly).mul(piece(model.body, 1));
        Vector3f bottom = new Vector3f(model.body.x, model.body.y, model.body.z)
                .add(chest.transform(new Vector3f(0.0F, 6.0F, 0.0F)))
                .add(belly.transform(new Vector3f(0.0F, 3.0F, 0.0F)))
                .add(pelvis.transform(new Vector3f(0.0F, 3.0F, 0.0F)));
        assertEquals(0.0F, bottom.distance(hips), CLOSE);
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        assertEquals(0.0F, belly.transform(new Vector3f(up)).distance(lean.transform(new Vector3f(up))), CLOSE);
        float tilt = (float) Math.acos(pelvis.transform(new Vector3f(up)).y);
        float leaning = (float) Math.acos(lean.transform(new Vector3f(up)).y);
        assertTrue(tilt > 0.4F * leaning && tilt < 0.8F * leaning, "the pelvis takes part of the lean: " + tilt);
        assertEquals(model.body.x, model.head.x, CLOSE);
        assertEquals(model.body.y, model.head.y, CLOSE);
    }
}
