package nl.tivek.multiversepowers.engine.client.model;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.model.ModelParts.Part;
import nl.tivek.multiversepowers.engine.client.model.ModelParts.Role;
import nl.tivek.multiversepowers.engine.rig.Limits;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Where a model's parts bend: a long arm or leg at its knee or elbow and again at its wrist or ankle, a long trunk at
// its waist and again at its pelvis; and where an arm's shoulder blade meets the trunk.
public final class ModelBends {
    // Where a part bends, in pixels of its own frame: the axis it runs along, the cut across it (farSign +1 when its
    // far end lies at the high side of that axis), the joint's middle, the axis it turns about there and how far: from
    // min to max radians, the part past the cut bending the way the part folds. A wrist or an ankle also leans across
    // its hinge and twists about its bone, each as far as `lean` and `twist` either way, and the hand or foot past it
    // stays whole (`whole`): its forearm or shin takes its twist. A knee or an elbow only folds.
    public record Bend(int axis, float at, float farSign, float[] knee, float[] hinge, double min, double max,
            double lean, double twist, boolean whole) {
        public Bend(int axis, float at, float farSign, float[] knee, float[] hinge, double min, double max) {
            this(axis, at, farSign, knee, hinge, min, max, 0.0, 0.0, false);
        }

        // The turn of the piece past this bend (in the axes of the piece before it) kept to what the joint can do.
        public Quaternionf keep(Quaternionf turn) {
            if (this.lean == 0.0 && this.twist == 0.0) {
                return Limits.hinge(turn, this.hinge, (float) this.min, (float) this.max);
            }
            float[] bone = new float[3];
            bone[this.axis] = this.farSign;
            return Limits.end(turn, bone, this.hinge, (float) this.min, (float) this.max, (float) this.lean,
                    (float) this.twist);
        }
    }

    public static final Bend[] NONE = new Bend[0];

    // A limb must be this much longer than it is thick, this long in pixels and thicker than a plate, to have a knee.
    private static final float SLENDER = 2.0F;
    private static final float LONG = 7.0F;
    private static final float PLATE = 1.0F;
    // A hand is this share of an arm's length and a foot this share of a leg's, in whole pixels between these, and
    // past its knee a limb keeps at least SHIN_LEAST before its hand or foot.
    private static final float HAND = 0.25F;
    private static final float FOOT = 1.0F / 6.0F;
    private static final float HAND_LEAST = 2.0F;
    private static final float HAND_MOST = 4.0F;
    private static final float FOOT_LEAST = 1.0F;
    private static final float FOOT_MOST = 3.0F;
    private static final float SHIN_LEAST = 2.0F;
    // How far a hand folds either way, and a foot hanging down back (its toes up, as far as tucked under a kneeling
    // body) and forward (pointing); the tip of a leg standing out sideways folds down more than up.
    private static final double HAND_FOLD = 1.3;
    private static final double TOES_UP = 1.6;
    private static final double TOES_DOWN = 1.2;
    private static final double TIP_UP = 0.6;
    private static final double TIP_DOWN = 1.0;
    // A four-legged creature's hoof or paw folds back under its leg this far, and the other way only a little: the
    // same way for a front leg as its knee and against its hock for a hind one.
    private static final double HOOF_FOLD = 1.2;
    private static final double HOOF_BACK = 0.4;
    // How far a hand leans to its sides at the wrist and turns with its forearm, either way, and a foot at its ankle.
    private static final double HAND_LEAN = 0.35;
    private static final double HAND_TWIST = 1.6;
    private static final double FOOT_LEAN = 0.4;
    private static final double FOOT_TWIST = 0.3;
    // A pelvis and a belly are each at least this long (pixels) along the spine, and the pelvis folds this share of
    // the waist's reach.
    private static final float PELVIS_LEAST = 2.0F;
    private static final double PELVIS_SHARE = 0.5;
    // An arm hanging this near the spine's line (pixels) has no shoulder blade: crossed arms, or a golem's.
    private static final float SHOULDER_LEAST = 2.5F;
    // An arm part reaching this far (pixels) to both sides of its own middle is a pair folded across the chest.
    private static final float CROSSING = 2.0F;
    // A part's pivot this near another part's box (pixels) may hang it from that part.
    private static final float TOUCHES = 1.0F;

    // A four-legged creature's hind leg, or one of its front legs, by the name its model gives it.
    public static boolean hind(Part part) {
        String name = part.name();
        return part.role() == Role.LEG && (name.contains("hind") || name.contains("back_leg"));
    }

    public static boolean front(Part part) {
        return part.role() == Role.LEG && part.name().contains("front");
    }

    // Arms folded across the chest as one part (a villager's, an illager's): it hangs on the body's middle and
    // reaches across it to both sides.
    public static boolean crossed(Part part) {
        if (part.role() != Role.ARM) {
            return false;
        }
        float[] b = part.bounds();
        return Math.abs(ModelParts.rest(part, new Matrix4f()).m30()) < CROSSING && b[0] < -CROSSING
                && b[3] > CROSSING;
    }

    @Nullable
    public static Bend bend(Part part) {
        // A limb with parts of its own on it (a foot, a hoof) stays whole: only its own cubes could be drawn bent; so
        // do arms folded as one part.
        if (part.role() != Role.ARM && part.role() != Role.LEG || !part.part().children.isEmpty() || crossed(part)) {
            return null;
        }
        float[] b = part.bounds();
        int axis = 0;
        for (int a = 1; a < 3; a++) {
            if (b[a + 3] - b[a] > b[axis + 3] - b[axis]) {
                axis = a;
            }
        }
        float length = b[axis + 3] - b[axis];
        float thick = 0.0F;
        float thin = Float.POSITIVE_INFINITY;
        for (int a = 0; a < 3; a++) {
            if (a != axis) {
                thick = Math.max(thick, b[a + 3] - b[a]);
                thin = Math.min(thin, b[a + 3] - b[a]);
            }
        }
        // A plate (a turtle's flipper) is no limb with a knee.
        if (length < LONG || length < SLENDER * thick || thin <= PLATE) {
            return null;
        }
        float farSign = Math.abs(b[axis + 3]) >= Math.abs(b[axis]) ? 1.0F : -1.0F;
        float at = (b[axis] + b[axis + 3]) * 0.5F;
        float[] knee = { (b[0] + b[3]) * 0.5F, (b[1] + b[4]) * 0.5F, (b[2] + b[5]) * 0.5F };
        knee[axis] = at;
        float[] bone = new float[3];
        bone[axis] = farSign;
        float[] toward = new float[3];
        double min;
        double max;
        if (axis == 1) {
            // Hanging down (model y points down): legs fold back, arms and a four-legged creature's hind legs forward.
            boolean forward = part.role() == Role.ARM || hind(part);
            toward[2] = forward ? -1.0F : 1.0F;
            min = -0.08;
            max = part.role() == Role.ARM ? 2.5 : 2.3;
        } else {
            // Standing out sideways (a spider's legs): they fold down.
            toward[1] = 1.0F;
            min = -0.5;
            max = 1.7;
        }
        float[] hinge = { bone[1] * toward[2] - bone[2] * toward[1], bone[2] * toward[0] - bone[0] * toward[2],
                bone[0] * toward[1] - bone[1] * toward[0] };
        return new Bend(axis, at, farSign, knee, hinge, min, max);
    }

    // Where a limb with a knee bends again near its far end: an arm's wrist, a leg's ankle, cut across it as the knee
    // is and folding about the same axis, the hand or foot past it. Null when the limb is too short past its knee.
    @Nullable
    public static Bend end(Part part, Bend knee) {
        float[] b = part.bounds();
        int axis = knee.axis();
        float sign = knee.farSign();
        boolean arm = part.role() == Role.ARM;
        float length = b[axis + 3] - b[axis];
        float end = Mth.clamp(Math.round(length * (arm ? HAND : FOOT)), arm ? HAND_LEAST : FOOT_LEAST,
                arm ? HAND_MOST : FOOT_MOST);
        float at = (sign > 0.0F ? b[axis + 3] : b[axis]) - sign * end;
        if ((at - knee.at()) * sign < SHIN_LEAST) {
            return null;
        }
        float[] joint = knee.knee().clone();
        joint[axis] = at;
        double min = arm ? -HAND_FOLD : axis != 1 ? -TIP_UP : hind(part) ? -HOOF_FOLD : front(part) ? -HOOF_BACK
                : -TOES_UP;
        double max = arm ? HAND_FOLD : axis != 1 ? TIP_DOWN : hind(part) ? HOOF_BACK : front(part) ? HOOF_FOLD
                : TOES_DOWN;
        return new Bend(axis, at, sign, joint, knee.hinge().clone(), min, max, arm ? HAND_LEAN : FOOT_LEAN,
                arm ? HAND_TWIST : FOOT_TWIST, true);
    }

    // Every bend of part i from its near end: the trunk's waist and pelvis, a limb's knee or elbow and its wrist or
    // ankle.
    public static Bend[] chain(List<Part> parts, int core, int i) {
        if (i == core) {
            Bend waist = waist(parts, core);
            if (waist == null) {
                return NONE;
            }
            Bend pelvis = pelvis(parts, core, waist);
            return pelvis == null ? new Bend[] { waist } : new Bend[] { waist, pelvis };
        }
        Bend knee = bend(parts.get(i));
        if (knee == null) {
            return NONE;
        }
        Bend end = end(parts.get(i), knee);
        return end == null ? new Bend[] { knee } : new Bend[] { knee, end };
    }

    // A trunk must be this long in pixels along its spine, and this much longer than it is thick, to bend in two.
    private static final float TRUNK_LONG = 10.0F;
    private static final float TRUNK_SLENDER = 1.3F;
    // Its head sits at one end of it: past the middle by at least this share of its half length.
    private static final float HEAD_OUT = 0.5F;
    // The waist lies within the middle of the trunk's first box, never nearer an end than this share of it.
    private static final float WAIST_EDGE = 0.35F;
    // A spine pointing this steeply down the model stands up: it folds ahead, not down.
    private static final float UPRIGHT = 0.7F;
    // How far a trunk folds towards its belly and back the other way, standing up and on all fours.
    private static final double FOLD_UPRIGHT = 1.0;
    private static final double BACK_UPRIGHT = 0.4;
    private static final double FOLD_LOW = 0.6;
    private static final double BACK_LOW = 0.35;

    // The trunk: the biggest part that is a body, or the biggest of all when none is.
    public static int core(List<Part> parts) {
        int core = 0;
        double biggest = -1.0;
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            float[] b = part.bounds();
            double volume = (b[3] - b[0]) * (b[4] - b[1]) * (b[5] - b[2]);
            boolean body = part.role() == Role.BODY;
            boolean coreBody = parts.get(core).role() == Role.BODY;
            if (i == 0 || body && !coreBody || body == coreBody && volume > biggest) {
                core = i;
                biggest = volume;
            }
        }
        return core;
    }

    // Where a trunk bends in two, as a Bend of its part: across its spine (the axis its head sits at one end of),
    // halfway from that end to where its hindmost legs hang. The half its head is on stays the part's own; the other
    // folds towards the belly, ahead for a trunk standing up and down for one on all fours. Worked out from the model
    // as it was built, so every creature of a kind bends alike whatever it is doing. Null when the trunk has no head
    // at an end or is too short or thick to bend.
    @Nullable
    public static Bend waist(List<Part> parts, int core) {
        Part trunk = parts.get(core);
        if (trunk.role() != Role.BODY && trunk.role() != Role.OTHER || trunk.part().cubes.isEmpty()) {
            return null;
        }
        Matrix4f rest = ModelParts.rest(trunk, new Matrix4f());
        Matrix4f into = new Matrix4f(rest).invert();
        Vector3f head = head(parts, core, into);
        if (head == null) {
            return null;
        }
        float[] b = trunk.bounds();
        int axis = spine(b, head);
        if (axis < 0) {
            return null;
        }
        float length = b[axis + 3] - b[axis];
        float thick = 0.0F;
        for (int a = 0; a < 3; a++) {
            if (a != axis) {
                thick = Math.max(thick, b[a + 3] - b[a]);
            }
        }
        if (length < TRUNK_LONG || length < TRUNK_SLENDER * thick) {
            return null;
        }
        float farSign = head.get(axis) < (b[axis] + b[axis + 3]) * 0.5F ? 1.0F : -1.0F;
        float front = farSign > 0.0F ? b[axis] : b[axis + 3];
        float hind = hind(parts, core, into, axis, farSign);
        ModelPart.Cube first = trunk.part().cubes.get(0);
        float lo = axis == 0 ? first.minX : axis == 1 ? first.minY : first.minZ;
        float hi = axis == 0 ? first.maxX : axis == 1 ? first.maxY : first.maxZ;
        float cut = Float.isNaN(hind) || (hind - front) * farSign <= 0.0F ? (b[axis] + b[axis + 3]) * 0.5F
                : (front + hind) * 0.5F;
        cut = Mth.clamp(cut, lo + WAIST_EDGE * (hi - lo), hi - WAIST_EDGE * (hi - lo));
        float[] knee = { (b[0] + b[3]) * 0.5F, (b[1] + b[4]) * 0.5F, (b[2] + b[5]) * 0.5F };
        knee[axis] = cut;
        Vector3f spine = rest.transformDirection(unit(axis, farSign)).normalize();
        boolean upright = Math.abs(spine.y) > UPRIGHT;
        Vector3f belly = into.transformDirection(upright ? new Vector3f(0.0F, 0.0F, -1.0F)
                : new Vector3f(0.0F, 1.0F, 0.0F));
        belly.setComponent(axis, 0.0F);
        if (belly.lengthSquared() < 0.04F) {
            return null;
        }
        Vector3f hinge = unit(axis, farSign).cross(belly.normalize());
        return new Bend(axis, cut, farSign, knee, new float[] { hinge.x, hinge.y, hinge.z },
                upright ? -BACK_UPRIGHT : -BACK_LOW, upright ? FOLD_UPRIGHT : FOLD_LOW);
    }

    // Which parts hang from a bent trunk's far half (legs, a tail), as the model was built.
    public static boolean[] far(List<Part> parts, int core, Bend waist) {
        boolean[] far = new boolean[parts.size()];
        Matrix4f into = ModelParts.rest(parts.get(core), new Matrix4f()).invert();
        Vector3f at = new Vector3f();
        for (int i = 0; i < parts.size(); i++) {
            if (i != core) {
                far[i] = (pivot(parts.get(i), into, at).get(waist.axis()) - waist.at()) * waist.farSign() > 0.0F;
            }
        }
        return far;
    }

    // The axis of a trunk's box (bounds b) its head (in the trunk's pixels) sits at one end of, -1 when none.
    private static int spine(float[] b, Vector3f head) {
        int axis = -1;
        float best = HEAD_OUT;
        for (int a = 0; a < 3; a++) {
            float half = (b[a + 3] - b[a]) * 0.5F;
            float out = half > 0.0F ? Math.abs(head.get(a) - (b[a] + b[a + 3]) * 0.5F) / half : 0.0F;
            if (out > best) {
                best = out;
                axis = a;
            }
        }
        return axis;
    }

    // How far along the spine the hindmost legs hang, in the trunk's pixels; NaN without legs.
    private static float hind(List<Part> parts, int core, Matrix4f into, int axis, float farSign) {
        float hind = Float.NaN;
        Vector3f at = new Vector3f();
        for (int i = 0; i < parts.size(); i++) {
            if (i != core && parts.get(i).role() == Role.LEG) {
                float along = pivot(parts.get(i), into, at).get(axis);
                if (Float.isNaN(hind) || (along - hind) * farSign > 0.0F) {
                    hind = along;
                }
            }
        }
        return hind;
    }

    // Where a bent trunk bends again, halfway from its waist to where its hindmost legs hang (or to the far end of its
    // first box): its pelvis, which the legs and a tail hang from, folding the way the waist does, less far. Null
    // when the belly or the pelvis would be too short.
    @Nullable
    public static Bend pelvis(List<Part> parts, int core, Bend waist) {
        Part trunk = parts.get(core);
        Matrix4f into = ModelParts.rest(trunk, new Matrix4f()).invert();
        int axis = waist.axis();
        float sign = waist.farSign();
        ModelPart.Cube first = trunk.part().cubes.get(0);
        float end = sign > 0.0F ? axis == 0 ? first.maxX : axis == 1 ? first.maxY : first.maxZ
                : axis == 0 ? first.minX : axis == 1 ? first.minY : first.minZ;
        float hind = hind(parts, core, into, axis, sign);
        float to = !Float.isNaN(hind) && (hind - waist.at()) * sign > 0.0F && (end - hind) * sign > 0.0F ? hind : end;
        float cut = Math.round(waist.at() + (to - waist.at()) * 0.5F);
        if ((cut - waist.at()) * sign < PELVIS_LEAST || (end - cut) * sign < PELVIS_LEAST) {
            return null;
        }
        float[] knee = waist.knee().clone();
        knee[axis] = cut;
        return new Bend(axis, cut, sign, knee, waist.hinge().clone(), waist.min() * PELVIS_SHARE,
                waist.max() * PELVIS_SHARE);
    }

    // Which piece of the trunk each part hangs from, as the model was built: 0 its own (the chest), 1 past its waist
    // (the belly), 2 past its pelvis.
    public static int[] hang(List<Part> parts, int core, Bend[] trunk) {
        int[] hang = new int[parts.size()];
        if (trunk.length == 0) {
            return hang;
        }
        Matrix4f into = ModelParts.rest(parts.get(core), new Matrix4f()).invert();
        Vector3f at = new Vector3f();
        for (int i = 0; i < parts.size(); i++) {
            if (i == core) {
                continue;
            }
            float along = pivot(parts.get(i), into, at).get(trunk[0].axis());
            for (Bend cut : trunk) {
                hang[i] += (along - cut.at()) * cut.farSign() > 0.0F ? 1 : 0;
            }
        }
        return hang;
    }

    // Where arm i's shoulder blade meets the trunk, in the trunk's own pixels: on the spine's line, level with where
    // the arm hangs, the arm hanging from the blade's far end, which turns about this end. Null for an arm that hangs
    // near the spine's line (crossed arms, a golem's) or not from the chest.
    @Nullable
    public static float[] shoulder(List<Part> parts, int core, int arm, int[] hang) {
        if (arm == core || parts.get(arm).role() != Role.ARM || hang[arm] != 0) {
            return null;
        }
        Part trunk = parts.get(core);
        Matrix4f into = ModelParts.rest(trunk, new Matrix4f()).invert();
        Vector3f head = head(parts, core, into);
        float[] b = trunk.bounds();
        int axis = head == null ? -1 : spine(b, head);
        if (axis < 0) {
            return null;
        }
        Vector3f pivot = pivot(parts.get(arm), into, new Vector3f());
        float[] inner = { (b[0] + b[3]) * 0.5F, (b[1] + b[4]) * 0.5F, (b[2] + b[5]) * 0.5F };
        inner[axis] = pivot.get(axis);
        float dx = pivot.x - inner[0];
        float dy = pivot.y - inner[1];
        float dz = pivot.z - inner[2];
        return dx * dx + dy * dy + dz * dz < SHOULDER_LEAST * SHOULDER_LEAST ? null : inner;
    }

    // Which part each part hangs from, as the model was built: the trunk (core), or another part at least as big whose
    // box its pivot lies in or touches, the nearest (the trunk winning a tie): a wolf's head and front legs hang from
    // its chest, a spider's legs from its thorax, a cat's tail's tip from the rest of its tail. -1 for the trunk
    // itself.
    public static int[] parents(List<Part> parts, int core) {
        int n = parts.size();
        Vector3f[] pivots = new Vector3f[n];
        Matrix4f[] into = new Matrix4f[n];
        float[] volume = new float[n];
        for (int i = 0; i < n; i++) {
            Matrix4f rest = ModelParts.rest(parts.get(i), new Matrix4f());
            pivots[i] = rest.transformPosition(new Vector3f());
            into[i] = rest.invert();
            float[] b = parts.get(i).bounds();
            volume[i] = (b[3] - b[0]) * (b[4] - b[1]) * (b[5] - b[2]);
        }
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            if (i == core) {
                parent[i] = -1;
                continue;
            }
            int best = core;
            float nearest = outside(parts.get(core).bounds(), into[core].transformPosition(new Vector3f(pivots[i])));
            for (int j = 0; j < n; j++) {
                if (j == i || j == core || volume[j] < volume[i]) {
                    continue;
                }
                float d = outside(parts.get(j).bounds(), into[j].transformPosition(new Vector3f(pivots[i])));
                if (d <= TOUCHES && d < nearest - 1.0E-3F) {
                    best = j;
                    nearest = d;
                }
            }
            parent[i] = best;
        }
        // A ring of parts hanging from each other hangs from the trunk instead.
        for (int i = 0; i < n; i++) {
            int up = i;
            for (int step = 0; step <= n && up >= 0 && up != core; step++) {
                up = parent[up];
            }
            if (up != core && i != core) {
                parent[i] = core;
            }
        }
        return parent;
    }

    // How far (pixels) a point lies outside a box, 0 inside it.
    private static float outside(float[] b, Vector3f p) {
        float dx = Math.max(Math.max(b[0] - p.x, p.x - b[3]), 0.0F);
        float dy = Math.max(Math.max(b[1] - p.y, p.y - b[4]), 0.0F);
        float dz = Math.max(Math.max(b[2] - p.z, p.z - b[5]), 0.0F);
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Where leg i meets the spine, in the trunk's own pixels: on the spine's line level with where the leg hangs (a
    // robe reaches past the hips, so the trunk's far end is no hip). Null for a leg that hangs from no bent trunk.
    @Nullable
    public static float[] hip(List<Part> parts, int core, int leg, Bend[] trunk) {
        if (leg == core || parts.get(leg).role() != Role.LEG || trunk.length == 0) {
            return null;
        }
        Part body = parts.get(core);
        Matrix4f into = ModelParts.rest(body, new Matrix4f()).invert();
        float[] b = body.bounds();
        int axis = trunk[0].axis();
        float[] inner = { (b[0] + b[3]) * 0.5F, (b[1] + b[4]) * 0.5F, (b[2] + b[5]) * 0.5F };
        inner[axis] = Mth.clamp(pivot(parts.get(leg), into, new Vector3f()).get(axis), b[axis], b[axis + 3]);
        return inner;
    }

    // Where a bent trunk standing up ends along its spine (its own pixels) under a robe reaching on past it over its
    // legs (a villager's, an illager's): where its first box or its hips end, whichever is further. NaN without one.
    public static float robe(List<Part> parts, int core, Bend[] trunk) {
        if (trunk.length == 0) {
            return Float.NaN;
        }
        Part body = parts.get(core);
        Matrix4f rest = ModelParts.rest(body, new Matrix4f());
        int axis = trunk[0].axis();
        float sign = trunk[0].farSign();
        if (Math.abs(rest.transformDirection(unit(axis, sign)).normalize().y) <= UPRIGHT) {
            return Float.NaN;
        }
        float hind = hind(parts, core, rest.invert(), axis, sign);
        if (Float.isNaN(hind)) {
            return Float.NaN;
        }
        ModelPart.Cube first = body.part().cubes.get(0);
        float own = sign > 0.0F ? axis == 0 ? first.maxX : axis == 1 ? first.maxY : first.maxZ
                : axis == 0 ? first.minX : axis == 1 ? first.minY : first.minZ;
        float ends = sign > 0.0F ? Math.max(hind, own) : Math.min(hind, own);
        float[] b = body.bounds();
        float end = sign > 0.0F ? b[axis + 3] : b[axis];
        boolean robe = (end - ends) * sign > 0.0F && (ends - trunk[trunk.length - 1].at()) * sign > 0.0F;
        return robe ? ends : Float.NaN;
    }

    private static Vector3f unit(int axis, float sign) {
        Vector3f v = new Vector3f();
        v.setComponent(axis, sign);
        return v;
    }

    // Where the head is (the middle of its box) in the trunk's own pixels: its biggest head part, or a head the trunk
    // carries as a part of its own (a camel's).
    @Nullable
    private static Vector3f head(List<Part> parts, int core, Matrix4f into) {
        Part head = null;
        float biggest = -1.0F;
        for (int i = 0; i < parts.size(); i++) {
            float[] b = parts.get(i).bounds();
            float volume = (b[3] - b[0]) * (b[4] - b[1]) * (b[5] - b[2]);
            if (i != core && parts.get(i).role() == Role.HEAD && volume > biggest) {
                head = parts.get(i);
                biggest = volume;
            }
        }
        if (head != null) {
            float[] b = head.bounds();
            return new Matrix4f(into).mul(ModelParts.rest(head, new Matrix4f())).transformPosition(new Vector3f(
                    (b[0] + b[3]) * 0.5F, (b[1] + b[4]) * 0.5F, (b[2] + b[5]) * 0.5F));
        }
        for (Map.Entry<String, ModelPart> child : parts.get(core).part().children.entrySet()) {
            float[] b = { Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY };
            if (child.getKey().contains("head")) {
                ModelParts.grow(child.getValue(), new Matrix4f(), b, new Vector3f());
                if (b[0] <= b[3]) {
                    return ModelParts.first(new Matrix4f(), child.getValue()).transformPosition(new Vector3f(
                            (b[0] + b[3]) * 0.5F, (b[1] + b[4]) * 0.5F, (b[2] + b[5]) * 0.5F));
                }
            }
        }
        return null;
    }

    // Where a part hangs, in the pixels `into` leads into from the model as it was built.
    private static Vector3f pivot(Part part, Matrix4f into, Vector3f out) {
        return new Matrix4f(into).mul(ModelParts.rest(part, new Matrix4f())).transformPosition(out.set(0.0F, 0.0F, 0.0F));
    }

    private ModelBends() {
    }
}
