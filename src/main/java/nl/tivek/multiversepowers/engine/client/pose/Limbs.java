package nl.tivek.multiversepowers.engine.client.pose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.render.EntityPass;
import org.joml.Quaternionf;

// Knees and elbows for a pose: a layer bends a person's arm or leg for the creature being drawn now, and BentParts
// draws it cut across at the joint. Undone once that creature is drawn, as the model is shared by all of its kind.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Limbs {
    public enum Joint {
        RIGHT_ELBOW,
        LEFT_ELBOW,
        RIGHT_KNEE,
        LEFT_KNEE,
        // The torso's lower half turning under the chest: the body part itself is the chest.
        WAIST
    }

    private record Rig(ModelPart[] parts, ModelParts.Bend[] bends, List<List<ModelPart>> followers) {
    }

    // The trunk bends halfway down, where a person's waist is, as far as a person's back goes either way.
    private static final double WAIST_MOST = 1.1;

    private static final Map<EntityModel<?>, Rig> RIGS = new WeakHashMap<>();
    private static final Quaternionf TURN = new Quaternionf();
    private static final float[] BENT = new float[Joint.values().length];
    @Nullable
    private static HumanoidModel<?> drawing;

    private Limbs() {
    }

    // Bends the joint `angle` radians the way it folds (a knee back, an elbow forward), within what it can.
    public static void bend(HumanoidModel<?> model, Joint joint, float angle) {
        if (joint == Joint.WAIST || !EntityPass.inWorld() || angle < 1.0E-3F) {
            return;
        }
        ModelParts.Bend bend = start(model, joint);
        if (bend == null) {
            return;
        }
        float a = (float) Mth.clamp(angle, 0.0, bend.max());
        BENT[joint.ordinal()] = a;
        put(model, joint, bend, TURN.setAngleAxis(a, bend.hinge()[0], bend.hinge()[1], bend.hinge()[2]));
    }

    // Turns the lower half of the trunk by `turn` (in the chest's own axes) about the waist.
    public static void waist(HumanoidModel<?> model, Quaternionf turn) {
        float angle = 2.0F * (float) Math.acos(Math.min(1.0F, Math.abs(turn.w)));
        if (!EntityPass.inWorld() || angle < 1.0E-3F) {
            return;
        }
        ModelParts.Bend bend = start(model, Joint.WAIST);
        if (bend != null) {
            BENT[Joint.WAIST.ordinal()] = angle;
            put(model, Joint.WAIST, bend, TURN.set(turn));
        }
    }

    @Nullable
    private static ModelParts.Bend start(HumanoidModel<?> model, Joint joint) {
        Rig rig = rig(model);
        ModelParts.Bend bend = rig == null ? null : rig.bends()[joint.ordinal()];
        if (bend != null && drawing != model) {
            clear();
            drawing = model;
        }
        return bend;
    }

    private static void put(HumanoidModel<?> model, Joint joint, ModelParts.Bend bend, Quaternionf turn) {
        Rig rig = RIGS.get(model);
        BentParts.bend(rig.parts()[joint.ordinal()], bend, turn);
        for (ModelPart follower : rig.followers().get(joint.ordinal())) {
            BentParts.bend(follower, bend, turn);
        }
    }

    // How far a joint of the model being drawn is bent now, in radians.
    public static float bent(HumanoidModel<?> model, Joint joint) {
        return drawing == model ? BENT[joint.ordinal()] : 0.0F;
    }

    // A layer's own copy of the model (armour) bends where the model itself does.
    public static void layer(EntityModel<?> copy) {
        HumanoidModel<?> model = drawing;
        Rig rig = model == null || copy == model ? null : RIGS.get(model);
        if (rig == null) {
            return;
        }
        for (int i = 0; i < BENT.length; i++) {
            if (BENT[i] > 0.0F) {
                ModelPart to = ModelParts.counterpart(model, copy, rig.parts()[i]);
                if (to != null && to != rig.parts()[i]) {
                    BentParts.same(rig.parts()[i], to);
                }
            }
        }
    }

    @Nullable
    private static Rig rig(HumanoidModel<?> model) {
        if (RIGS.containsKey(model)) {
            return RIGS.get(model);
        }
        List<ModelParts.Part> all = ModelParts.of(model);
        Rig rig = null;
        if (all != null) {
            ModelPart[] parts = { model.rightArm, model.leftArm, model.rightLeg, model.leftLeg, model.body };
            ModelParts.Bend[] bends = new ModelParts.Bend[parts.length];
            List<List<ModelPart>> followers = new ArrayList<>();
            for (int i = 0; i < parts.length; i++) {
                List<ModelPart> own = List.of();
                for (ModelParts.Part part : all) {
                    if (part.part() == parts[i]) {
                        bends[i] = part.role() == ModelParts.Role.BODY ? waistOf(part) : ModelParts.bend(part);
                        own = part.followers();
                    }
                }
                followers.add(own);
            }
            rig = new Rig(parts, bends, followers);
        }
        RIGS.put(model, rig);
        return rig;
    }

    // A trunk standing up along y, cut halfway down; its lower half is the far one.
    @Nullable
    private static ModelParts.Bend waistOf(ModelParts.Part part) {
        float[] b = part.bounds();
        if (b[4] - b[1] < b[3] - b[0] || b[4] - b[1] < b[5] - b[2]) {
            return null;
        }
        float at = (b[1] + b[4]) * 0.5F;
        float[] knee = { (b[0] + b[3]) * 0.5F, at, (b[2] + b[5]) * 0.5F };
        return new ModelParts.Bend(1, at, 1.0F, knee, new float[] { 1.0F, 0.0F, 0.0F }, -WAIST_MOST, WAIST_MOST);
    }

    private static void clear() {
        if (drawing != null) {
            Arrays.fill(BENT, 0.0F);
            drawing = null;
            BentParts.clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        clear();
    }
}
