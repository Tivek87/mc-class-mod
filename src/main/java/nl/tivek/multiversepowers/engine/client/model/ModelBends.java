package nl.tivek.multiversepowers.engine.client.model;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.model.ModelParts.Part;
import nl.tivek.multiversepowers.engine.client.model.ModelParts.Role;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// Where a model's parts bend: a long arm or leg at its knee or elbow, a long trunk at its waist.
public final class ModelBends {
    // Where a long arm or leg bends, a knee or an elbow, in pixels of its own frame: the axis it runs along, the cut
    // across it halfway (farSign +1 when its far end lies at the high side of that axis), the knee's middle, the axis
    // it turns about there and how far: from min to max radians, its far half bending the way the limb folds.
    public record Bend(int axis, float at, float farSign, float[] knee, float[] hinge, double min, double max) {
    }

    // A limb must be this much longer than it is thick, and this long in pixels, to have a knee.
    private static final float SLENDER = 2.0F;
    private static final float LONG = 7.0F;

    @Nullable
    public static Bend bend(Part part) {
        // A limb with parts of its own on it (a foot, a hoof) stays whole: only its own cubes could be drawn bent.
        if (part.role() != Role.ARM && part.role() != Role.LEG || !part.part().children.isEmpty()) {
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
        for (int a = 0; a < 3; a++) {
            if (a != axis) {
                thick = Math.max(thick, b[a + 3] - b[a]);
            }
        }
        if (length < LONG || length < SLENDER * thick) {
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
            boolean forward = part.role() == Role.ARM || part.name().contains("hind");
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
