package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import org.joml.Quaterniond;
import org.joml.Vector3d;

// A thrown creature getting up from where it lies, in its own time from lying (0) to standing in its own pose (1). A
// person gets up as a person does, from its front by way of its hands and knees, from its back by sitting up onto a
// hand and a knee (PersonRise); anything else rolls upright, tucks its legs under it and pushes up (CreatureRise).
// Every part moves with the piece of the trunk it hangs from, so arms and legs stay on the body the whole way up.
public final class GetUp {
    public static final int PERSON_TICKS = 52;
    public static final int OTHER_TICKS = 30;
    private static final String[] NAMES = { "head", "body", "right_arm", "left_arm", "right_leg", "left_leg" };

    // How one body gets up: its pose `u` of the way up, from how it lay (lie) to its own pose now (own), into out; the
    // creature is drawn facing the way it rises all the while.
    public interface Rise {
        void pose(float u, BodyPose lie, BodyPose own, BodyPose out);
    }

    private GetUp() {
    }

    public static int ticks(boolean person) {
        return person ? PERSON_TICKS : OTHER_TICKS;
    }

    // The way (the game's degrees) a person lying with its pelvis turned by the quaternion at lay[o] (the world's axes,
    // y up) gets up facing, as PersonRise plans it: sitting up, the way it faces; turned onto its front, towards its
    // head; else (on its back, or on its side turned back), towards its feet. `own` when that is no way in particular.
    public static float facing(double[] lay, int o, float own) {
        Quaterniond pelvis = new Quaterniond(lay[o], lay[o + 1], lay[o + 2], lay[o + 3]);
        Vector3d head = pelvis.transform(new Vector3d(0.0, -1.0, 0.0));
        Vector3d front = pelvis.transform(new Vector3d(0.0, 0.0, -1.0));
        // Its model's y runs down the spine, the world's up.
        double sign = head.y > PersonRise.SITTING ? 0.0 : front.y < 0.0 ? 1.0 : -1.0;
        double x = sign == 0.0 ? front.x : head.x * sign;
        double z = sign == 0.0 ? front.z : head.z * sign;
        return x * x + z * z < 0.04 ? own : (float) Math.toDegrees(Math.atan2(-x, z));
    }

    // A person's model: the six parts a person gets up with, each arm and leg bending at its elbow or knee.
    public static boolean person(EntityModel<?> model, List<ModelParts.Part> parts, ModelBends.Bend[][] chains) {
        if (!(model instanceof HumanoidModel<?>)) {
            return false;
        }
        for (String name : NAMES) {
            int i = index(parts, name);
            boolean limb = name.endsWith("_arm") || name.endsWith("_leg");
            if (i < 0 || limb && chains[i].length == 0) {
                return false;
            }
        }
        return true;
    }

    // How this body gets up, planned from how it lies now (lie), the first time it is drawn getting up; a person with a
    // sword or an axe in hand leans on it (Brace).
    public static Rise start(Hanging body, EntityModel<?> model, List<ModelParts.Part> parts,
            ModelBends.Bend[][] chains, BodyPose lie, boolean person, @Nullable LivingEntity entity) {
        if (person) {
            int[] roles = new int[NAMES.length];
            for (int r = 0; r < NAMES.length; r++) {
                roles[r] = index(parts, NAMES[r]);
            }
            return new PersonRise(new Skeleton(body, parts, chains, roles), lie,
                    entity == null ? null : Brace.of(entity), entity != null,
                    entity == null ? 0L : entity.getId() * 7919L + entity.tickCount);
        }
        return new CreatureRise(body, parts, chains);
    }

    static int index(List<ModelParts.Part> parts, String name) {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

}
