package nl.tivek.multiversepowers.engine.client.pose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class FootPlantingTest {
    private static final float CLOSE = 1.0E-3F;

    // Where each leg's sole is in the model as it was built, in pixels.
    private static void solesOnTheFloor(EntityModel<?> model) {
        List<ModelParts.Part> parts = ModelParts.of(model);
        assertNotNull(parts);
        int legs = 0;
        for (ModelParts.Part part : parts) {
            if (part.role() != ModelParts.Role.LEG) {
                continue;
            }
            float[] sole = FootPlanting.sole(part);
            Vector3f at = ModelParts.rest(part, new Matrix4f())
                    .transformPosition(new Vector3f(sole[0] * 16.0F, sole[1] * 16.0F, sole[2] * 16.0F));
            assertEquals(Stance.GROUND, at.y, CLOSE, part.name());
            legs++;
        }
        assertEquals(2, legs);
    }

    @Test
    void anEndermansLegsStandOnTheFloorThoughBuiltAPixelBelowIt() {
        solesOnTheFloor(new EndermanModel<EnderMan>(EndermanModel.createBodyLayer().bakeRoot()));
    }

    @Test
    void aPersonsLegsStandWhereTheyEnd() {
        HumanoidModel<LivingEntity> person = new HumanoidModel<>(
                LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot());
        solesOnTheFloor(person);
        for (ModelParts.Part part : ModelParts.of(person)) {
            if (part.role() == ModelParts.Role.LEG) {
                assertEquals(12.0F / 16.0F, FootPlanting.sole(part)[1], CLOSE);
            }
        }
    }
}
