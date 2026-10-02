package nl.tivek.multiversepowers.engine.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ModelPartsTest {
    private static final float CLOSE = 1.0E-3F;

    @AfterEach
    void unbend() {
        BentParts.clear();
    }

    private static int index(List<ModelParts.Part> parts, String name) {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).name().equals(name)) {
                return i;
            }
        }
        throw new AssertionError("no part " + name);
    }

    private static List<ModelParts.Part> parts(EntityModel<?> model) {
        List<ModelParts.Part> parts = ModelParts.of(model);
        assertNotNull(parts);
        return parts;
    }

    @Test
    void aPersonBendsHalfwayDownItsTrunkFoldingAhead() {
        HumanoidModel<LivingEntity> person = new HumanoidModel<>(
                LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot());
        List<ModelParts.Part> parts = parts(person);
        int core = ModelBends.core(parts);
        assertEquals(person.body, parts.get(core).part());
        ModelBends.Bend waist = ModelBends.waist(parts, core);
        assertNotNull(waist);
        assertEquals(1, waist.axis());
        assertEquals(6.0F, waist.at(), CLOSE);
        assertEquals(1.0F, waist.farSign());
        // The belly and hips fold ahead (-z) about -x, much further than back.
        assertEquals(-1.0F, waist.hinge()[0], CLOSE);
        assertTrue(waist.max() > 0.9 && waist.min() < 0.0 && -waist.min() < waist.max());
        boolean[] far = ModelBends.far(parts, core, waist);
        assertTrue(far[index(parts, "right_leg")] && far[index(parts, "left_leg")], "legs hang from the hips");
        assertFalse(far[index(parts, "head")] || far[index(parts, "right_arm")] || far[index(parts, "left_arm")],
                "head and arms hang from the chest");
    }

    @Test
    void aPersonHasWristsAnklesAPelvisAndShoulderBlades() {
        HumanoidModel<LivingEntity> person = new HumanoidModel<>(
                LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot());
        List<ModelParts.Part> parts = parts(person);
        int core = ModelBends.core(parts);
        ModelBends.Bend[] arm = ModelBends.chain(parts, core, index(parts, "right_arm"));
        ModelBends.Bend[] leg = ModelBends.chain(parts, core, index(parts, "left_leg"));
        ModelBends.Bend[] trunk = ModelBends.chain(parts, core, core);
        // Elbow and wrist, a three-pixel hand; knee and ankle, a two-pixel foot; waist and pelvis.
        assertEquals(2, arm.length);
        assertEquals(4.0F, arm[0].at(), CLOSE);
        assertEquals(7.0F, arm[1].at(), CLOSE);
        assertEquals(2, leg.length);
        assertEquals(6.0F, leg[0].at(), CLOSE);
        assertEquals(10.0F, leg[1].at(), CLOSE);
        assertEquals(2, trunk.length);
        assertEquals(9.0F, trunk[1].at(), CLOSE);
        assertEquals(trunk[0].hinge()[0], trunk[1].hinge()[0], CLOSE);
        int[] hang = ModelBends.hang(parts, core, trunk);
        assertEquals(2, hang[index(parts, "right_leg")], "legs hang from the pelvis");
        assertEquals(0, hang[index(parts, "right_arm")] + hang[index(parts, "head")], "head and arms from the chest");
        float[] blade = ModelBends.shoulder(parts, core, index(parts, "right_arm"), hang);
        assertNotNull(blade);
        assertEquals(0.0F, new Vector3f(blade[0], blade[1], blade[2]).distance(0.0F, 2.0F, 0.0F), CLOSE,
                "the blade meets the spine level with the shoulder");
        assertNull(ModelBends.shoulder(parts, core, index(parts, "head"), hang));
    }

    @Test
    void aGolemsArmsHangFromItsMiddleWithoutBlades() {
        IronGolemModel<IronGolem> golem = new IronGolemModel<>(IronGolemModel.createBodyLayer().bakeRoot());
        List<ModelParts.Part> parts = parts(golem);
        int core = ModelBends.core(parts);
        int[] hang = ModelBends.hang(parts, core, ModelBends.chain(parts, core, core));
        assertNull(ModelBends.shoulder(parts, core, index(parts, "right_arm"), hang));
        assertEquals(2, ModelBends.chain(parts, core, index(parts, "right_arm")).length, "its long arm has a hand");
    }

    @Test
    void aVillagerBendsAtItsWaistNotHalfwayDownItsRobe() {
        VillagerModel<LivingEntity> villager = new VillagerModel<>(
                LayerDefinition.create(VillagerModel.createBodyModel(), 64, 64).bakeRoot());
        List<ModelParts.Part> parts = parts(villager);
        int core = ModelBends.core(parts);
        assertEquals("body", parts.get(core).name());
        ModelBends.Bend waist = ModelBends.waist(parts, core);
        assertNotNull(waist);
        assertEquals(1, waist.axis());
        assertTrue(waist.at() > 5.0F && waist.at() < 7.0F, "at its waist, not halfway down the robe: " + waist.at());
        boolean[] far = ModelBends.far(parts, core, waist);
        assertTrue(far[index(parts, "right_leg")] && !far[index(parts, "arms")] && !far[index(parts, "head")]);
    }

    @Test
    void aVillagersFoldedArmsStayWholeAndItsHeadHasNoBrim() {
        VillagerModel<LivingEntity> villager = new VillagerModel<>(
                LayerDefinition.create(VillagerModel.createBodyModel(), 64, 64).bakeRoot());
        List<ModelParts.Part> parts = parts(villager);
        ModelParts.Part arms = parts.get(index(parts, "arms"));
        assertTrue(ModelBends.crossed(arms));
        assertEquals(0, ModelBends.chain(parts, ModelBends.core(parts), index(parts, "arms")).length);
        // Its hat's brim is a plate: the head is as wide as its own box (and the hat grown over it), not the brim.
        float[] head = parts.get(index(parts, "head")).bounds();
        assertTrue(head[3] - head[0] < 10.0F, "head " + (head[3] - head[0]) + " wide");
    }

    @Test
    void theTopPartsAreThePartsARagdollTakesNeverWhatHoldsThem() {
        VillagerModel<LivingEntity> villager = new VillagerModel<>(
                LayerDefinition.create(VillagerModel.createBodyModel(), 64, 64).bakeRoot());
        List<ModelPart> tops = ModelParts.tops(villager);
        assertFalse(tops.contains(villager.root()), "the root holds every part: hiding it hides the villager");
        assertEquals(parts(villager).size(), tops.size());
        HumanoidModel<LivingEntity> person = new HumanoidModel<>(
                LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot());
        assertEquals(6, ModelParts.tops(person).size());
    }

    @Test
    void aCreeperBendsWithItsFeetOnTheLowerHalf() {
        CreeperModel<LivingEntity> creeper = new CreeperModel<>(
                CreeperModel.createBodyLayer(CubeDeformation.NONE).bakeRoot());
        List<ModelParts.Part> parts = parts(creeper);
        int core = ModelBends.core(parts);
        ModelBends.Bend waist = ModelBends.waist(parts, core);
        assertNotNull(waist);
        assertEquals(1, waist.axis());
        boolean[] far = ModelBends.far(parts, core, waist);
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).role() == ModelParts.Role.LEG) {
                assertTrue(far[i], parts.get(i).name());
            }
        }
        assertFalse(far[index(parts, "head")]);
    }

    @Test
    void aSpiderKeepsItsRoundBackWhole() {
        SpiderModel<LivingEntity> spider = new SpiderModel<>(SpiderModel.createSpiderBodyLayer().bakeRoot());
        List<ModelParts.Part> parts = parts(spider);
        assertNull(ModelBends.waist(parts, ModelBends.core(parts)));
    }

    @Test
    void aRobeOnTheTrunkBendsWithIt() {
        VillagerModel<LivingEntity> villager = new VillagerModel<>(
                LayerDefinition.create(VillagerModel.createBodyModel(), 64, 64).bakeRoot());
        List<ModelParts.Part> parts = parts(villager);
        int core = ModelBends.core(parts);
        ModelBends.Bend waist = ModelBends.waist(parts, core);
        ModelPart body = parts.get(core).part();
        ModelPart robe = body.getChild("jacket");
        BentParts.bend(body, waist, new Quaternionf().rotationX(-0.6F), Set.of());
        Vector3f low = BentParts.place(body, 1.0F, 17.0F, -2.0F, new Vector3f());
        Vector3f lowRobe = BentParts.place(robe, 1.0F, 17.0F, -2.0F, new Vector3f());
        assertEquals(0.0F, low.distance(lowRobe), CLOSE, "the robe's hem goes where the trunk's would");
        assertEquals(0.0F, BentParts.place(robe, 1.0F, 2.0F, -2.0F, new Vector3f())
                .distance(new Vector3f(1.0F, 2.0F, -2.0F)), CLOSE, "above the waist it stays");
    }

    @Test
    void aPartTurnedInsideABentOneBendsWhereItLies() {
        ModelPart child = new ModelPart(List.of(), Map.of());
        child.setPos(2.0F, 5.0F, -1.0F);
        child.setRotation(0.3F, 1.1F, -0.4F);
        ModelPart parent = new ModelPart(List.of(), Map.of("child", child));
        float[] knee = { 0.0F, 6.0F, 0.0F };
        ModelBends.Bend bend = new ModelBends.Bend(1, 6.0F, 1.0F, knee, new float[] { -1.0F, 0.0F, 0.0F }, -0.4, 1.0);
        Quaternionf turn = new Quaternionf().rotationXYZ(-0.7F, 0.2F, 0.1F);
        BentParts.bend(parent, bend, turn, Set.of());
        Quaternionf own = new Quaternionf().rotationZYX(child.zRot, child.yRot, child.xRot);
        Vector3f[] points = { new Vector3f(1.0F, 4.0F, 2.0F), new Vector3f(-3.0F, -2.0F, 0.5F),
                new Vector3f(0.0F, 0.0F, 6.0F) };
        for (Vector3f point : points) {
            // The child's point in the parent's pixels, placed by the parent, and back into the child's.
            Vector3f inParent = own.transform(new Vector3f(point)).add(child.x, child.y, child.z);
            Vector3f want = BentParts.place(parent, inParent.x, inParent.y, inParent.z, new Vector3f())
                    .sub(child.x, child.y, child.z);
            own.transformInverse(want);
            Vector3f got = BentParts.place(child, point.x, point.y, point.z, new Vector3f());
            assertEquals(0.0F, got.distance(want), CLOSE, "point " + point);
        }
    }
}
