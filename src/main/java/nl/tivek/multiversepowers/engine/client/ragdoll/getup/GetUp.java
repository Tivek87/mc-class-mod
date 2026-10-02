package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3f;

// A thrown creature getting up from where it lies, in its own time from lying (0) to standing in its own pose (1), the
// way its kind of body does (Kind). Every part moves with the piece of the trunk it hangs from, so arms and legs stay
// on the body the whole way up.
public final class GetUp {
    // The longest any body takes to get up: the server gives it no longer (Knockdowns).
    public static final int MOST_TICKS = 52;
    private static final String[] NAMES = { "head", "body", "right_arm", "left_arm", "right_leg", "left_leg" };

    // How a kind of body gets up, and how long it takes (ticks). A person by way of its hands and knees (PersonRise);
    // one with its arms folded (a villager) without its hands, by way of its knees or a crouch; four legs rolling onto
    // the chest, then up onto the front legs first (a horse, a dog, a pig) or the hind legs first (cattle, sheep, a
    // goat), or both at once; a bird flapping onto its feet; anything else rolling upright (CreatureRise).
    public enum Kind {
        PERSON(52),
        FOLDED(46),
        FRONT_FIRST(40),
        HIND_FIRST(44),
        EVEN(30),
        BIRD(22);

        public final int ticks;

        Kind(int ticks) {
            this.ticks = ticks;
        }

        // A person's body, getting up on its own two feet.
        public boolean person() {
            return this == PERSON || this == FOLDED;
        }
    }

    // How one body gets up: its pose `u` of the way up, from how it lay (lie) to its own pose now (own), into out; the
    // creature is drawn facing the way it rises all the while.
    public interface Rise {
        void pose(float u, BodyPose lie, BodyPose own, BodyPose out);
    }

    private GetUp() {
    }

    // How this body gets up: as `wanted` (its kind's profile) where its shape allows, else by its shape. A person's
    // shape (a head, a trunk and two legs that bend, named as a person's are) gets up as a person, with its hands when
    // it has two arms that bend, else with them folded; four legs under a lying trunk front first; two legs and wings
    // as a bird; anything else evenly.
    public static Kind kind(List<ModelParts.Part> parts, ModelBends.Bend[][] chains, @Nullable Kind wanted) {
        boolean shaped = person(parts, chains, false);
        boolean hands = person(parts, chains, true);
        if (wanted != null && (!wanted.person() || shaped) && (wanted != Kind.PERSON || hands)) {
            return wanted;
        }
        if (shaped) {
            return hands ? Kind.PERSON : Kind.FOLDED;
        }
        int core = ModelBends.core(parts);
        int legs = 0;
        boolean wings = false;
        for (int i = 0; i < parts.size(); i++) {
            legs += i != core && parts.get(i).role() == ModelParts.Role.LEG ? 1 : 0;
            wings |= parts.get(i).name().contains("wing");
        }
        if (legs == 2 && wings) {
            return Kind.BIRD;
        }
        return legs == 4 && lying(parts, core) ? Kind.FRONT_FIRST : Kind.EVEN;
    }

    // Whether the trunk lies along the ground as the model was built, its head ahead of it rather than on top.
    static boolean lying(List<ModelParts.Part> parts, int core) {
        Matrix4f rest = new Matrix4f();
        float[] c = parts.get(core).bounds();
        Vector3f trunk = ModelParts.rest(parts.get(core), rest).transformPosition(new Vector3f((c[0] + c[3]) * 0.5F,
                (c[1] + c[4]) * 0.5F, (c[2] + c[5]) * 0.5F));
        for (int i = 0; i < parts.size(); i++) {
            if (i != core && parts.get(i).role() == ModelParts.Role.HEAD) {
                float[] h = parts.get(i).bounds();
                Vector3f head = ModelParts.rest(parts.get(i), rest).transformPosition(new Vector3f(
                        (h[0] + h[3]) * 0.5F, (h[1] + h[4]) * 0.5F, (h[2] + h[5]) * 0.5F));
                return Math.abs(head.z - trunk.z) > Math.abs(head.y - trunk.y);
            }
        }
        float[] b = parts.get(core).bounds();
        return b[5] - b[2] > b[4] - b[1];
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

    // A person's shape: a head, a trunk and two legs bending at their knees, named as a person's are, and with
    // `hands` two arms bending at their elbows as well (not folded across the chest as one part).
    private static boolean person(List<ModelParts.Part> parts, ModelBends.Bend[][] chains, boolean hands) {
        for (String name : NAMES) {
            boolean arm = name.endsWith("_arm");
            if (arm && !hands) {
                continue;
            }
            int i = index(parts, name);
            boolean limb = arm || name.endsWith("_leg");
            if (i < 0 || limb && chains[i].length == 0) {
                return false;
            }
        }
        return true;
    }

    // How this body gets up, planned from how it lies now (lie), the first time it is drawn getting up; a person with a
    // sword or an axe in hand leans on it (Brace).
    public static Rise start(Hanging body, EntityModel<?> model, List<ModelParts.Part> parts,
            ModelBends.Bend[][] chains, BodyPose lie, Kind kind, @Nullable LivingEntity entity) {
        if (kind.person()) {
            int[] roles = new int[NAMES.length];
            for (int r = 0; r < NAMES.length; r++) {
                boolean arm = r == Skeleton.BODY + 1 || r == Skeleton.BODY + 2;
                roles[r] = arm && kind == Kind.FOLDED ? -1 : index(parts, NAMES[r]);
            }
            return new PersonRise(new Skeleton(body, parts, chains, roles), lie,
                    entity == null || kind == Kind.FOLDED ? null : Brace.of(entity), entity != null,
                    entity == null ? 0L : entity.getId() * 7919L + entity.tickCount);
        }
        return new CreatureRise(body, parts, chains, kind);
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
