package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class RiseKindsTest {
    // A model's parts as a ragdoll takes them; a four-legged one's lists are read as the game's own mixin would.
    private static List<ModelParts.Part> parts(EntityModel<?> model) throws ReflectiveOperationException {
        if (!(model instanceof AgeableListModel<?>)) {
            return ModelParts.of(model);
        }
        Method heads = AgeableListModel.class.getDeclaredMethod("headParts");
        Method bodies = AgeableListModel.class.getDeclaredMethod("bodyParts");
        Method names = ModelParts.class.getDeclaredMethod("fieldNames", EntityModel.class);
        Method addAll = ModelParts.class.getDeclaredMethod("addAll", List.class, Iterable.class,
                ModelParts.Role.class, int.class, Map.class);
        heads.setAccessible(true);
        bodies.setAccessible(true);
        names.setAccessible(true);
        addAll.setAccessible(true);
        List<ModelParts.Part> parts = new ArrayList<>();
        Object named = names.invoke(null, model);
        addAll.invoke(null, parts, heads.invoke(model), ModelParts.Role.HEAD, ModelParts.HEADS, named);
        addAll.invoke(null, parts, bodies.invoke(model), ModelParts.Role.OTHER, ModelParts.BODIES, named);
        return parts;
    }

    // The body lying turned by `down` about its trunk, getting up its kind's way (or `wanted`): it starts just as it
    // lies, ends just in its own pose, never breaks and never jumps between frames.
    private static GetUp.Kind rises(EntityModel<?> model, Quaternionf down, GetUp.Kind wanted)
            throws ReflectiveOperationException {
        model.young = false;
        List<ModelParts.Part> parts = parts(model);
        int n = parts.size();
        int core = ModelBends.core(parts);
        ModelBends.Bend[][] chains = new ModelBends.Bend[n][];
        boolean[] inside = new boolean[n];
        for (int i = 0; i < n; i++) {
            chains[i] = ModelBends.chain(parts, core, i);
            inside[i] = i != core && parts.get(i).parents().contains(parts.get(core).part());
        }
        Hanging body = new Hanging(n, core, inside, ModelBends.hang(parts, core, chains[core]), chains[core]);
        BodyPose own = new BodyPose();
        BodyPose lie = new BodyPose();
        for (int i = 0; i < n; i++) {
            ModelPart part = parts.get(i).part();
            own.pos[i].set(part.x, part.y, part.z);
            own.rot[i].rotationZYX(part.zRot, part.yRot, part.xRot);
        }
        Vector3f pivot = new Vector3f(own.pos[core]);
        for (int i = 0; i < n; i++) {
            if (inside[i]) {
                lie.pos[i].set(own.pos[i]);
                lie.rot[i].set(own.rot[i]);
            } else {
                down.transform(lie.pos[i].set(own.pos[i]).sub(pivot)).add(pivot).add(2.0F, 8.0F, -3.0F);
                lie.rot[i].set(down).mul(own.rot[i]);
            }
        }
        GetUp.Kind kind = GetUp.kind(parts, chains, wanted);
        GetUp.Rise rise = GetUp.start(body, model, parts, chains, lie, kind, null);
        BodyPose out = new BodyPose();
        Vector3f[] was = new Vector3f[n];
        float jump = 0.0F;
        int frames = kind.ticks * 4;
        for (int f = 0; f <= frames; f++) {
            float u = f / (float) frames;
            rise.pose(u, lie, own, out);
            for (int i = 0; i < n; i++) {
                Vector3f p = out.pos[i];
                assertTrue(Float.isFinite(p.x + p.y + p.z + out.rot[i].w), kind + " broken at " + u);
                if (was[i] != null) {
                    jump = Math.max(jump, was[i].distance(p));
                }
                was[i] = new Vector3f(p);
            }
            if (f == 0 || f == frames) {
                BodyPose want = f == 0 ? lie : own;
                for (int i = 0; i < n; i++) {
                    assertTrue(out.pos[i].distance(want.pos[i]) < 0.01F, kind + " not where it starts or ends");
                }
            }
        }
        // Four frames a tick: a part moves at most a few pixels a frame.
        assertTrue(jump < 3.0F, kind + ": a part jumps " + jump + " pixels in a frame");
        return kind;
    }

    @Test
    void aVillagerGetsUpWithItsArmsFolded() throws ReflectiveOperationException {
        for (float turn : new float[] { 1.5708F, -1.5708F }) {
            VillagerModel<LivingEntity> villager = new VillagerModel<>(
                    LayerDefinition.create(VillagerModel.createBodyModel(), 64, 64).bakeRoot());
            assertEquals(GetUp.Kind.FOLDED, rises(villager, new Quaternionf().rotationX(turn), null));
        }
    }

    @Test
    void fourLeggedCreaturesRiseOneEndFirstFromTheirSideOrTheirBack() throws ReflectiveOperationException {
        for (float roll : new float[] { 1.5708F, 3.1F }) {
            CowModel<LivingEntity> cow = new CowModel<>(CowModel.createBodyLayer().bakeRoot());
            assertEquals(GetUp.Kind.HIND_FIRST, rises(cow, new Quaternionf().rotationZ(roll), GetUp.Kind.HIND_FIRST));
            WolfModel<Wolf> wolf = new WolfModel<>(
                    LayerDefinition.create(WolfModel.createMeshDefinition(CubeDeformation.NONE), 64, 32).bakeRoot());
            assertEquals(GetUp.Kind.FRONT_FIRST, rises(wolf, new Quaternionf().rotationZ(roll), null));
        }
    }

    @Test
    void aSpiderRightsItselfEvenly() throws ReflectiveOperationException {
        SpiderModel<LivingEntity> spider = new SpiderModel<>(SpiderModel.createSpiderBodyLayer().bakeRoot());
        assertEquals(GetUp.Kind.EVEN, rises(spider, new Quaternionf().rotationZ(3.1F), null));
    }
}
