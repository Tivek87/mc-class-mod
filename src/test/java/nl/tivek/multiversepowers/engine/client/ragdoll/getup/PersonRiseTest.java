package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class PersonRiseTest {
    // A person lying face down (turn 1.57) or face up (-1.57), and standing in its own pose: getting up goes from
    // just how it lies to just its own pose, never jumps between frames and never breaks.
    @Test
    void aPersonGetsUpSmoothlyFromItsFrontAndItsBack() {
        for (float turn : new float[] { 1.5708F, -1.5708F, 1.2F, -1.9F }) {
            HumanoidModel<LivingEntity> model = new HumanoidModel<>(
                    LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot());
            model.young = false;
            List<ModelParts.Part> parts = ModelParts.of(model);
            int n = parts.size();
            int core = ModelBends.core(parts);
            ModelBends.Bend[][] chains = new ModelBends.Bend[n][];
            boolean[] inside = new boolean[n];
            for (int i = 0; i < n; i++) {
                chains[i] = ModelBends.chain(parts, core, i);
            }
            Hanging body = new Hanging(n, core, inside, ModelBends.hang(parts, core, chains[core]), chains[core]);
            BodyPose own = new BodyPose();
            BodyPose lie = new BodyPose();
            Quaternionf down = new Quaternionf().rotationX(turn);
            for (int i = 0; i < n; i++) {
                ModelPart part = parts.get(i).part();
                own.pos[i].set(part.x, part.y, part.z);
                own.rot[i].rotationZYX(part.zRot, part.yRot, part.xRot);
                down.transform(lie.pos[i].set(own.pos[i]).sub(0.0F, 12.0F, 0.0F)).add(3.0F, 22.0F, 5.0F);
                lie.rot[i].set(down).mul(own.rot[i]);
            }
            assertEquals(GetUp.Kind.PERSON, GetUp.kind(parts, chains, null));
            GetUp.Rise rise = GetUp.start(body, model, parts, chains, lie, GetUp.Kind.PERSON, null);
            BodyPose out = new BodyPose();
            Vector3f[] was = new Vector3f[n];
            float jump = 0.0F;
            for (int f = 0; f <= 200; f++) {
                float u = f / 200.0F;
                rise.pose(u, lie, own, out);
                for (int i = 0; i < n; i++) {
                    Vector3f p = out.pos[i];
                    assertTrue(Float.isFinite(p.x + p.y + p.z + out.rot[i].w), "broken at " + u);
                    if (was[i] != null) {
                        jump = Math.max(jump, was[i].distance(p));
                    }
                    was[i] = new Vector3f(p);
                }
                if (f == 0 || f == 200) {
                    BodyPose want = f == 0 ? lie : own;
                    for (int i = 0; i < n; i++) {
                        assertTrue(out.pos[i].distance(want.pos[i]) < 0.01F, "not where it starts or ends: " + u);
                    }
                }
            }
            // 200 frames over 2.6 seconds: a part moves at most a few pixels a frame.
            assertTrue(jump < 2.5F, "turn " + turn + ": a part jumps " + jump + " pixels in a frame");
        }
    }
}
