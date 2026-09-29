package nl.tivek.multiversepowers.engine.client.pose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Knees, elbows and waists for a pose: a layer bends a person's arm or leg, or any creature's trunk, for the creature
// being drawn now, and BentParts draws it cut across at the joint. Undone once that creature is drawn, as the model is
// shared by all of its kind.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Limbs {
    public enum Joint {
        RIGHT_ELBOW,
        LEFT_ELBOW,
        RIGHT_KNEE,
        LEFT_KNEE,
        // The trunk's far half turning about the waist (a person's belly under the chest): the part itself is the
        // near half.
        WAIST
    }

    // A model's joints: each joint's part, its bend and the parts that copy it. For the waist: the model's parts, which
    // are posed on their own and never bent with the trunk, and those that hang from its far half (with the parts that
    // copy them), each drawn inside the trunk or beside it.
    private record Rig(ModelPart[] parts, ModelBends.Bend[] bends, List<List<ModelPart>> followers,
            Set<ModelPart> apart, List<ModelPart> hanging, boolean[] inTrunk) {
    }

    private static final int VALUES = 6;
    private static final Map<EntityModel<?>, Rig> RIGS = new WeakHashMap<>();
    private static final Quaternionf TURN = new Quaternionf();
    private static final Quaternionf TRUNK = new Quaternionf();
    private static final Quaternionf OWN = new Quaternionf();
    private static final Vector3f AT = new Vector3f();
    private static final Vector3f EULER = new Vector3f();
    private static final Matrix3f M = new Matrix3f();
    private static final float[] BENT = new float[Joint.values().length];
    // The parts spine() moved and where they were, put back once the creature is drawn.
    private static final List<ModelPart> MOVED = new ArrayList<>();
    private static float[] was = new float[VALUES * 8];
    @Nullable
    private static EntityModel<?> drawing;

    private Limbs() {
    }

    // Bends the joint `angle` radians the way it folds (a knee back, an elbow forward), within what it can.
    public static void bend(HumanoidModel<?> model, Joint joint, float angle) {
        if (joint == Joint.WAIST || !EntityPass.inWorld() || angle < 1.0E-3F) {
            return;
        }
        ModelBends.Bend bend = start(model, joint);
        if (bend == null) {
            return;
        }
        float a = (float) Mth.clamp(angle, 0.0, bend.max());
        BENT[joint.ordinal()] = a;
        put(model, joint, bend, TURN.setAngleAxis(a, bend.hinge()[0], bend.hinge()[1], bend.hinge()[2]));
    }

    // Turns the trunk's far half by `turn` (in the trunk part's own axes) about the waist, for any creature whose trunk
    // bends. What hangs from that half is left where it is: a person's legs are placed by Stance.
    public static void waist(EntityModel<?> model, Quaternionf turn) {
        if (!EntityPass.inWorld() || angle(turn) < 1.0E-3F) {
            return;
        }
        ModelBends.Bend bend = start(model, Joint.WAIST);
        if (bend != null) {
            BENT[Joint.WAIST.ordinal()] = angle(turn);
            put(model, Joint.WAIST, bend, TURN.set(turn));
        }
    }

    // Bends any creature's trunk as waist() does and carries what hangs from its far half (legs, a tail) along.
    public static void spine(EntityModel<?> model, Quaternionf turn) {
        if (!EntityPass.inWorld() || angle(turn) < 1.0E-3F) {
            return;
        }
        ModelBends.Bend bend = start(model, Joint.WAIST);
        if (bend == null) {
            return;
        }
        Rig rig = RIGS.get(model);
        BENT[Joint.WAIST.ordinal()] = angle(turn);
        put(model, Joint.WAIST, bend, TURN.set(turn));
        ModelPart trunk = rig.parts()[Joint.WAIST.ordinal()];
        TRUNK.rotationZYX(trunk.zRot, trunk.yRot, trunk.xRot);
        float[] k = bend.knee();
        for (int i = 0; i < rig.hanging().size(); i++) {
            ModelPart part = rig.hanging().get(i);
            keep(part);
            OWN.rotationZYX(part.zRot, part.yRot, part.xRot);
            if (rig.inTrunk()[i]) {
                // Drawn inside the trunk: turned about the knee in the trunk's own pixels.
                AT.set(part.x, part.y, part.z).sub(k[0], k[1], k[2]);
                TURN.transform(AT).add(k[0], k[1], k[2]);
                OWN.premul(TURN);
            } else {
                // Beside it: into the trunk's frame, turned there, and back out.
                AT.set(part.x - trunk.x, part.y - trunk.y, part.z - trunk.z);
                TRUNK.transformInverse(AT).div(trunk.xScale, trunk.yScale, trunk.zScale).sub(k[0], k[1], k[2]);
                TURN.transform(AT).add(k[0], k[1], k[2]).mul(trunk.xScale, trunk.yScale, trunk.zScale);
                TRUNK.transform(AT).add(trunk.x, trunk.y, trunk.z);
                OWN.premul(new Quaternionf(TRUNK).conjugate()).premul(TURN).premul(TRUNK);
            }
            OWN.get(M).getEulerAnglesZYX(EULER);
            part.setPos(AT.x, AT.y, AT.z);
            part.setRotation(EULER.x, EULER.y, EULER.z);
        }
    }

    private static float angle(Quaternionf turn) {
        return 2.0F * (float) Math.acos(Math.min(1.0F, Math.abs(turn.w)));
    }

    @Nullable
    private static ModelBends.Bend start(EntityModel<?> model, Joint joint) {
        Rig rig = rig(model);
        ModelBends.Bend bend = rig == null ? null : rig.bends()[joint.ordinal()];
        if (bend != null && drawing != model) {
            clear();
            drawing = model;
        }
        return bend;
    }

    private static void put(EntityModel<?> model, Joint joint, ModelBends.Bend bend, Quaternionf turn) {
        Rig rig = RIGS.get(model);
        BentParts.bend(rig.parts()[joint.ordinal()], bend, turn, rig.apart());
        for (ModelPart follower : rig.followers().get(joint.ordinal())) {
            BentParts.bend(follower, bend, turn, rig.apart());
        }
    }

    // How far a joint of the model being drawn is bent now, in radians.
    public static float bent(EntityModel<?> model, Joint joint) {
        return drawing == model ? BENT[joint.ordinal()] : 0.0F;
    }

    // A layer's own copy of the model (armour, a sheep's wool) bends where the model itself does, and what spine()
    // carried along stands where it does on the model.
    public static void layer(EntityModel<?> copy) {
        EntityModel<?> model = drawing;
        Rig rig = model == null || copy == model ? null : RIGS.get(model);
        if (rig == null) {
            return;
        }
        for (int i = 0; i < BENT.length; i++) {
            if (BENT[i] > 0.0F && rig.parts()[i] != null) {
                ModelPart to = ModelParts.counterpart(model, copy, rig.parts()[i]);
                if (to != null && to != rig.parts()[i]) {
                    BentParts.same(rig.parts()[i], to);
                }
            }
        }
        for (int i = 0; i < MOVED.size(); i++) {
            ModelPart from = MOVED.get(i);
            ModelPart to = rig.hanging().contains(from) ? ModelParts.counterpart(model, copy, from) : null;
            if (to != null && to != from) {
                keep(to);
                to.copyFrom(from);
            }
        }
    }

    @Nullable
    private static Rig rig(EntityModel<?> model) {
        if (RIGS.containsKey(model)) {
            return RIGS.get(model);
        }
        List<ModelParts.Part> all = ModelParts.of(model);
        Rig rig = null;
        if (all != null) {
            int core = ModelBends.core(all);
            ModelPart[] parts = new ModelPart[Joint.values().length];
            if (model instanceof HumanoidModel<?> person) {
                parts[Joint.RIGHT_ELBOW.ordinal()] = person.rightArm;
                parts[Joint.LEFT_ELBOW.ordinal()] = person.leftArm;
                parts[Joint.RIGHT_KNEE.ordinal()] = person.rightLeg;
                parts[Joint.LEFT_KNEE.ordinal()] = person.leftLeg;
            }
            parts[Joint.WAIST.ordinal()] = all.get(core).part();
            ModelBends.Bend[] bends = new ModelBends.Bend[parts.length];
            List<List<ModelPart>> followers = new ArrayList<>();
            for (int i = 0; i < parts.length; i++) {
                List<ModelPart> own = List.of();
                for (ModelParts.Part part : all) {
                    if (part.part() == parts[i]) {
                        bends[i] = i == Joint.WAIST.ordinal() ? ModelBends.waist(all, core) : ModelBends.bend(part);
                        own = part.followers();
                    }
                }
                followers.add(own);
            }
            Set<ModelPart> apart = Collections.newSetFromMap(new IdentityHashMap<>());
            List<ModelPart> hanging = new ArrayList<>();
            List<Boolean> inTrunk = new ArrayList<>();
            ModelBends.Bend waist = bends[Joint.WAIST.ordinal()];
            boolean[] far = waist == null ? new boolean[all.size()] : ModelBends.far(all, core, waist);
            for (int i = 0; i < all.size(); i++) {
                ModelParts.Part part = all.get(i);
                apart.add(part.part());
                apart.addAll(part.followers());
                if (far[i]) {
                    boolean inside = part.parents().contains(parts[Joint.WAIST.ordinal()]);
                    hanging.add(part.part());
                    inTrunk.add(inside);
                    for (ModelPart follower : part.followers()) {
                        hanging.add(follower);
                        inTrunk.add(inside);
                    }
                }
            }
            boolean[] inside = new boolean[inTrunk.size()];
            for (int i = 0; i < inside.length; i++) {
                inside[i] = inTrunk.get(i);
            }
            rig = new Rig(parts, bends, followers, apart, hanging, inside);
        }
        RIGS.put(model, rig);
        return rig;
    }

    private static void keep(ModelPart part) {
        if (MOVED.contains(part)) {
            return;
        }
        int o = MOVED.size() * VALUES;
        if (o + VALUES > was.length) {
            was = Arrays.copyOf(was, was.length * 2);
        }
        MOVED.add(part);
        was[o] = part.x;
        was[o + 1] = part.y;
        was[o + 2] = part.z;
        was[o + 3] = part.xRot;
        was[o + 4] = part.yRot;
        was[o + 5] = part.zRot;
    }

    private static void clear() {
        if (drawing != null) {
            Arrays.fill(BENT, 0.0F);
            drawing = null;
            BentParts.clear();
        }
        for (int i = 0; i < MOVED.size(); i++) {
            int o = i * VALUES;
            MOVED.get(i).setPos(was[o], was[o + 1], was[o + 2]);
            MOVED.get(i).setRotation(was[o + 3], was[o + 4], was[o + 5]);
        }
        MOVED.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        clear();
    }
}
