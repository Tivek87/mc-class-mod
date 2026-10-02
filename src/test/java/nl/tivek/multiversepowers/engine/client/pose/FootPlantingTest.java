package nl.tivek.multiversepowers.engine.client.pose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // A leg 0.75 long turned t about its hip from hanging `from` radians: how much higher its tip is.
    private static double lift(double from, double t) {
        return 0.75 * (Math.cos(from) - Math.cos(from + t));
    }

    @Test
    void aFootTurnsJustEnoughToStandOnAStep() {
        for (double from : new double[] { -0.4, -0.05, 0.0, 0.05, 0.3 }) {
            double a = 0.75 * Math.sin(from);
            double b = 0.75 * Math.cos(from);
            double t = FootPlanting.turnFor(a, b, 0.06);
            assertEquals(0.06, lift(from, t), 1.0E-6, "from " + from);
            // It steps up the way it already swings, never kicking back through the step.
            if (Math.abs(from) > 0.01) {
                assertEquals(Math.signum(from), Math.signum(t), "from " + from);
            }
            assertTrue(Math.abs(t) < 0.5, "from " + from + ": " + t);
        }
    }

    @Test
    void aStraightLegNeverReachesDownAFootItCannotLower() {
        // Hanging straight down its tip is as low as it goes: it stays as it is, not kicked out to find the ground.
        assertTrue(Double.isNaN(FootPlanting.turnFor(0.0, 0.75, -0.06)));
        assertTrue(Double.isNaN(FootPlanting.turnFor(0.75 * Math.sin(0.02), 0.75 * Math.cos(0.02), -0.06)));
        // Spread out to its side, it turns down till it rests.
        double t = FootPlanting.turnFor(0.75 * Math.sin(1.2), 0.75 * Math.cos(1.2), -0.1);
        assertEquals(-0.1, lift(1.2, t), 1.0E-6);
    }

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
