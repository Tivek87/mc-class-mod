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

// Every joint of a body for a pose: a layer bends a person's arm at its elbow and wrist, a leg at its knee and ankle,
// any creature's trunk at its waist and pelvis, and turns a person's shoulder blades, for the creature being drawn now;
// BentParts draws each part cut across at its joints. Undone once that creature is drawn, as the model is shared by all
// of its kind.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Limbs {
    public enum Joint {
        RIGHT_ELBOW,
        LEFT_ELBOW,
        RIGHT_KNEE,
        LEFT_KNEE,
        // The trunk's far half turning about the waist (a person's belly under the chest): the part itself is the
        // near half.
        WAIST,
        // A hand at its wrist and a foot at its ankle: the arm's or leg's last piece, past its elbow or knee.
        RIGHT_WRIST,
        LEFT_WRIST,
        RIGHT_ANKLE,
        LEFT_ANKLE,
        // The trunk's last piece, the pelvis the legs hang from, turning past the belly below the waist.
        PELVIS
    }

    // A model's joints: each joint's part, which of the part's joints it is, the part's joints (ModelBends.chain) and
    // the parts that copy it. The model's parts, which are posed on their own and never bent with the trunk; those
    // that hang past the waist, and past the pelvis (with the parts that copy them), each drawn inside the trunk or
    // beside it; and each arm's shoulder blade where it meets the trunk, in the trunk's pixels.
    private record Rig(ModelPart[] parts, int[] order, ModelBends.Bend[][] chains, List<List<ModelPart>> followers,
            Set<ModelPart> apart, List<ModelPart> hanging, boolean[] inTrunk, List<ModelPart> hips,
            boolean[] hipsInTrunk, float[][] blades) {
    }

    private static final int VALUES = 6;
    private static final int JOINTS = Joint.values().length;
    private static final Map<EntityModel<?>, Rig> RIGS = new WeakHashMap<>();
    private static final Quaternionf TURN = new Quaternionf();
    private static final Quaternionf TRUNK = new Quaternionf();
    private static final Quaternionf OWN = new Quaternionf();
    private static final Vector3f AT = new Vector3f();
    private static final Vector3f EULER = new Vector3f();
    private static final Matrix3f M = new Matrix3f();
    private static final float[] BENT = new float[JOINTS];
    // Every joint's turn now, for the model being drawn (none: straight).
    private static final Quaternionf[] TURNS = new Quaternionf[JOINTS];

    static {
        for (int i = 0; i < JOINTS; i++) {
            TURNS[i] = new Quaternionf();
        }
    }
    // The parts spine() moved and where they were, put back once the creature is drawn.
    private static final List<ModelPart> MOVED = new ArrayList<>();
    private static float[] was = new float[VALUES * 8];
    @Nullable
    private static EntityModel<?> drawing;

    private Limbs() {
    }

    // Bends the joint `angle` radians the way it folds (a knee back, an elbow forward), within what it can; a wrist or
    // an ankle folds either way (an ankle ahead lifts the toes), not the waist or pelvis.
    public static void bend(HumanoidModel<?> model, Joint joint, float angle) {
        if (joint == Joint.WAIST || joint == Joint.PELVIS || !EntityPass.inWorld() || Math.abs(angle) < 1.0E-3F) {
            return;
        }
        ModelBends.Bend bend = start(model, joint);
        if (bend == null) {
            return;
        }
        boolean end = joint.ordinal() > Joint.WAIST.ordinal();
        float a = (float) Mth.clamp(angle, end ? bend.min() : 0.0, bend.max());
        if (Math.abs(a) < 1.0E-3F) {
            return;
        }
        BENT[joint.ordinal()] = Math.abs(a);
        put(model, joint, TURN.setAngleAxis(a, bend.hinge()[0], bend.hinge()[1], bend.hinge()[2]));
    }

    // Turns a hand at its wrist or a foot at its ankle by `turn`, in the axes of the arm's or leg's piece before it: a
    // fold, a lean to the side or both.
    public static void turn(HumanoidModel<?> model, Joint joint, Quaternionf turn) {
        boolean end = joint == Joint.RIGHT_WRIST || joint == Joint.LEFT_WRIST || joint == Joint.RIGHT_ANKLE
                || joint == Joint.LEFT_ANKLE;
        if (!end || !EntityPass.inWorld() || angle(turn) < 1.0E-3F || start(model, joint) == null) {
            return;
        }
        BENT[joint.ordinal()] = angle(turn);
        put(model, joint, TURN.set(turn));
    }

    // Turns the trunk's far half by `turn` (in the trunk part's own axes) about the waist, for any creature whose trunk
    // bends. What hangs from that half is left where it is: a person's legs are placed by Stance.
    public static void waist(EntityModel<?> model, Quaternionf turn) {
        if (!EntityPass.inWorld() || angle(turn) < 1.0E-3F) {
            return;
        }
        if (start(model, Joint.WAIST) != null) {
            BENT[Joint.WAIST.ordinal()] = angle(turn);
            put(model, Joint.WAIST, TURN.set(turn));
        }
    }

    // Turns the trunk's pelvis by `turn` (in the belly's axes) past the belly, for any creature whose trunk bends there;
    // what hangs from it is left where it is.
    public static void pelvis(EntityModel<?> model, Quaternionf turn) {
        if (!EntityPass.inWorld() || angle(turn) < 1.0E-3F) {
            return;
        }
        if (start(model, Joint.PELVIS) != null) {
            BENT[Joint.PELVIS.ordinal()] = angle(turn);
            put(model, Joint.PELVIS, TURN.set(turn));
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
        put(model, Joint.WAIST, TURN.set(turn));
        carry(rig, rig.hanging(), rig.inTrunk(), bend.knee(), TURN.set(turn));
    }

    // Bends any creature's pelvis as pelvis() does and carries what hangs from it (legs, a tail) along; with spine(),
    // in either order, what hangs from the pelvis goes where both joints take it.
    public static void hips(EntityModel<?> model, Quaternionf turn) {
        if (!EntityPass.inWorld() || angle(turn) < 1.0E-3F) {
            return;
        }
        ModelBends.Bend bend = start(model, Joint.PELVIS);
        if (bend == null) {
            return;
        }
        Rig rig = RIGS.get(model);
        BENT[Joint.PELVIS.ordinal()] = angle(turn);
        put(model, Joint.PELVIS, TURN.set(turn));
        // Where the pelvis's joint and axes are now, carried by the waist as it is bent.
        Quaternionf waist = TURNS[Joint.WAIST.ordinal()];
        float[] k = rig.chains()[Joint.WAIST.ordinal()][0].knee();
        float[] p = bend.knee();
        AT.set(p[0] - k[0], p[1] - k[1], p[2] - k[2]);
        waist.transform(AT).add(k[0], k[1], k[2]);
        carry(rig, rig.hips(), rig.hipsInTrunk(), new float[] { AT.x, AT.y, AT.z },
                new Quaternionf(waist).mul(turn).mul(new Quaternionf(waist).conjugate()));
    }

    // Parts carried along by a turn of the trunk's piece they hang from about `k` (in the trunk's own pixels and axes),
    // each drawn inside the trunk or beside it.
    private static void carry(Rig rig, List<ModelPart> parts, boolean[] inside, float[] k, Quaternionf turn) {
        ModelPart trunk = rig.parts()[Joint.WAIST.ordinal()];
        TRUNK.rotationZYX(trunk.zRot, trunk.yRot, trunk.xRot);
        for (int i = 0; i < parts.size(); i++) {
            ModelPart part = parts.get(i);
            keep(part);
            OWN.rotationZYX(part.zRot, part.yRot, part.xRot);
            if (inside[i]) {
                // Drawn inside the trunk: turned about the knee in the trunk's own pixels.
                AT.set(part.x, part.y, part.z).sub(k[0], k[1], k[2]);
                turn.transform(AT).add(k[0], k[1], k[2]);
                OWN.premul(turn);
            } else {
                // Beside it: into the trunk's frame, turned there, and back out.
                AT.set(part.x - trunk.x, part.y - trunk.y, part.z - trunk.z);
                TRUNK.transformInverse(AT).div(trunk.xScale, trunk.yScale, trunk.zScale).sub(k[0], k[1], k[2]);
                turn.transform(AT).add(k[0], k[1], k[2]).mul(trunk.xScale, trunk.yScale, trunk.zScale);
                TRUNK.transform(AT).add(trunk.x, trunk.y, trunk.z);
                OWN.premul(new Quaternionf(TRUNK).conjugate()).premul(turn).premul(TRUNK);
            }
            OWN.get(M).getEulerAnglesZYX(EULER);
            part.setPos(AT.x, AT.y, AT.z);
            part.setRotation(EULER.x, EULER.y, EULER.z);
        }
    }

    // Turns a person's shoulder blade by `turn` (in the trunk's own axes) about where it meets the spine: the arm hangs
    // from the blade's far end, so a turn up shrugs it and a turn ahead rolls the shoulder forward. The arm turns along.
    public static void shoulder(HumanoidModel<?> model, boolean right, Quaternionf turn) {
        if (!EntityPass.inWorld() || angle(turn) < 1.0E-3F) {
            return;
        }
        Rig rig = rig(model);
        float[] blade = rig == null ? null : rig.blades()[right ? 0 : 1];
        if (blade == null) {
            return;
        }
        if (drawing != model) {
            clear();
            drawing = model;
        }
        ModelPart arm = right ? model.rightArm : model.leftArm;
        List<ModelPart> moved = new ArrayList<>();
        moved.add(arm);
        moved.addAll(rig.followers().get(right ? Joint.RIGHT_ELBOW.ordinal() : Joint.LEFT_ELBOW.ordinal()));
        carry(rig, moved, new boolean[moved.size()], blade, turn);
    }

    private static float angle(Quaternionf turn) {
        return 2.0F * (float) Math.acos(Math.min(1.0F, Math.abs(turn.w)));
    }

    // The joint's own bend, starting the model being drawn now; null when the model has no such joint.
    @Nullable
    private static ModelBends.Bend start(EntityModel<?> model, Joint joint) {
        Rig rig = rig(model);
        ModelBends.Bend[] chain = rig == null ? null : rig.chains()[joint.ordinal()];
        int order = rig == null ? 0 : rig.order()[joint.ordinal()];
        ModelBends.Bend bend = chain == null || order >= chain.length ? null : chain[order];
        if (bend != null && drawing != model) {
            clear();
            drawing = model;
        }
        return bend;
    }

    // Bends the joint's part at all its joints: this one by `turn`, the others as they were bent already.
    private static void put(EntityModel<?> model, Joint joint, Quaternionf turn) {
        Rig rig = RIGS.get(model);
        int i = joint.ordinal();
        TURNS[i].set(turn);
        ModelBends.Bend[] chain = rig.chains()[i];
        Quaternionf[] turns = new Quaternionf[chain.length];
        for (int j = 0; j < JOINTS; j++) {
            if (rig.parts()[j] == rig.parts()[i] && rig.order()[j] < turns.length) {
                turns[rig.order()[j]] = TURNS[j];
            }
        }
        BentParts.bend(rig.parts()[i], chain, turns, rig.apart());
        for (ModelPart follower : rig.followers().get(i)) {
            BentParts.bend(follower, chain, turns, rig.apart());
        }
    }

    // How far a joint of the model being drawn is bent now, in radians.
    public static float bent(EntityModel<?> model, Joint joint) {
        return drawing == model ? BENT[joint.ordinal()] : 0.0F;
    }

    // A layer's own copy of the model (armour, a sheep's wool) bends where the model itself does, and what spine(),
    // hips() and shoulder() carried along stands where it does on the model.
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
            ModelPart to = rig.apart().contains(from) ? ModelParts.counterpart(model, copy, from) : null;
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
            ModelPart[] parts = new ModelPart[JOINTS];
            int[] order = new int[JOINTS];
            if (model instanceof HumanoidModel<?> person) {
                parts[Joint.RIGHT_ELBOW.ordinal()] = person.rightArm;
                parts[Joint.LEFT_ELBOW.ordinal()] = person.leftArm;
                parts[Joint.RIGHT_KNEE.ordinal()] = person.rightLeg;
                parts[Joint.LEFT_KNEE.ordinal()] = person.leftLeg;
                parts[Joint.RIGHT_WRIST.ordinal()] = person.rightArm;
                parts[Joint.LEFT_WRIST.ordinal()] = person.leftArm;
                parts[Joint.RIGHT_ANKLE.ordinal()] = person.rightLeg;
                parts[Joint.LEFT_ANKLE.ordinal()] = person.leftLeg;
            }
            parts[Joint.WAIST.ordinal()] = all.get(core).part();
            parts[Joint.PELVIS.ordinal()] = all.get(core).part();
            order[Joint.RIGHT_WRIST.ordinal()] = 1;
            order[Joint.LEFT_WRIST.ordinal()] = 1;
            order[Joint.RIGHT_ANKLE.ordinal()] = 1;
            order[Joint.LEFT_ANKLE.ordinal()] = 1;
            order[Joint.PELVIS.ordinal()] = 1;
            ModelBends.Bend[][] chains = new ModelBends.Bend[JOINTS][];
            List<List<ModelPart>> followers = new ArrayList<>();
            for (int i = 0; i < JOINTS; i++) {
                List<ModelPart> own = List.of();
                for (int p = 0; p < all.size(); p++) {
                    if (all.get(p).part() == parts[i]) {
                        chains[i] = ModelBends.chain(all, core, p);
                        own = all.get(p).followers();
                    }
                }
                followers.add(own);
            }
            ModelBends.Bend[] trunk = chains[Joint.WAIST.ordinal()];
            int[] hang = ModelBends.hang(all, core, trunk);
            Set<ModelPart> apart = Collections.newSetFromMap(new IdentityHashMap<>());
            List<ModelPart> hanging = new ArrayList<>();
            List<Boolean> inTrunk = new ArrayList<>();
            List<ModelPart> hips = new ArrayList<>();
            List<Boolean> hipsInTrunk = new ArrayList<>();
            for (int i = 0; i < all.size(); i++) {
                ModelParts.Part part = all.get(i);
                apart.add(part.part());
                apart.addAll(part.followers());
                boolean inside = part.parents().contains(parts[Joint.WAIST.ordinal()]);
                for (int past = 1; past <= hang[i]; past++) {
                    List<ModelPart> list = past == 1 ? hanging : hips;
                    List<Boolean> in = past == 1 ? inTrunk : hipsInTrunk;
                    list.add(part.part());
                    in.add(inside);
                    for (ModelPart follower : part.followers()) {
                        list.add(follower);
                        in.add(inside);
                    }
                }
            }
            float[][] blades = new float[2][];
            if (model instanceof HumanoidModel<?> person) {
                for (int i = 0; i < all.size(); i++) {
                    int side = all.get(i).part() == person.rightArm ? 0 : all.get(i).part() == person.leftArm ? 1 : -1;
                    if (side >= 0) {
                        blades[side] = ModelBends.shoulder(all, core, i, hang);
                    }
                }
            }
            rig = new Rig(parts, order, chains, followers, apart, hanging, flags(inTrunk), hips, flags(hipsInTrunk),
                    blades);
        }
        RIGS.put(model, rig);
        return rig;
    }

    private static boolean[] flags(List<Boolean> list) {
        boolean[] flags = new boolean[list.size()];
        for (int i = 0; i < flags.length; i++) {
            flags[i] = list.get(i);
        }
        return flags;
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
            for (Quaternionf turn : TURNS) {
                turn.identity();
            }
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
