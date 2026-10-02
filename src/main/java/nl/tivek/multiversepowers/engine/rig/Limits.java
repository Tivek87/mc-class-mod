package nl.tivek.multiversepowers.engine.rig;

import org.joml.Quaternionf;
import org.joml.Vector3f;

// A joint's turn kept within how far it can go: a hinge (an elbow, a knee) only about its axis and between its two
// angles; a wrist or an ankle folding about its hinge, leaning across it and twisting about its bone, each within its
// own range. Every axis is a unit vector in the joint's own frame.
public final class Limits {
    private Limits() {
    }

    // The angle (radians, -pi to pi) `q` turns about the unit axis (ax, ay, az), the rest of it left out.
    public static float about(Quaternionf q, float ax, float ay, float az) {
        float along = q.x * ax + q.y * ay + q.z * az;
        float w = q.w;
        if (w < 0.0F) {
            along = -along;
            w = -w;
        }
        return 2.0F * (float) Math.atan2(along, w);
    }

    // The turn that points `bone` (a unit vector) along `way`, as a joint can: folded about the unit `hinge` (kept
    // between min and max), then leaned across it (kept within `side` either way), with no twist. Steady for any way,
    // even straight back along the bone. Into out.
    public static Quaternionf aim(Vector3f way, float[] bone, float[] hinge, float min, float max, float side,
            Quaternionf out) {
        Vector3f b = new Vector3f(bone[0], bone[1], bone[2]);
        Vector3f h = new Vector3f(hinge[0], hinge[1], hinge[2]);
        Vector3f fold = new Vector3f(h).cross(b);
        Vector3f lean = new Vector3f(b).cross(h);
        float folded = (float) Math.atan2(way.dot(fold), way.dot(b));
        folded = Math.max(min, Math.min(max, folded));
        out.setAngleAxis(folded, h.x, h.y, h.z);
        // What is left of the way, in the folded piece's own axes, leans it across the hinge: turning about
        // bone x hinge takes the bone towards the hinge.
        Vector3f left = new Quaternionf(out).conjugate().transform(new Vector3f(way));
        float leaned = (float) Math.atan2(left.dot(h), left.dot(b));
        leaned = Math.max(-side, Math.min(side, leaned));
        return leaned == 0.0F ? out : out.rotateAxis(leaned, lean.x, lean.y, lean.z);
    }

    // `q` made a turn about the unit `hinge` alone, its angle kept between min and max.
    public static Quaternionf hinge(Quaternionf q, float[] hinge, float min, float max) {
        float angle = about(q, hinge[0], hinge[1], hinge[2]);
        angle = Math.max(min, Math.min(max, angle));
        return q.setAngleAxis(angle, hinge[0], hinge[1], hinge[2]);
    }

    // `q` kept as a joint at the end of a limb turns: its fold about the unit `hinge` between min and max, its lean
    // about bone x hinge within `side` either way, its twist about the unit `bone` within `twist` either way. A turn
    // already within all three comes back the same.
    public static Quaternionf end(Quaternionf q, float[] bone, float[] hinge, float min, float max, float side,
            float twist) {
        float bx = bone[0];
        float by = bone[1];
        float bz = bone[2];
        // Split into a twist about the bone and a swing after it: q = swing * twist.
        float along = q.x * bx + q.y * by + q.z * bz;
        Quaternionf turn = new Quaternionf(along * bx, along * by, along * bz, q.w);
        float size = (float) Math.sqrt(turn.x * turn.x + turn.y * turn.y + turn.z * turn.z + turn.w * turn.w);
        if (size < 1.0E-6F) {
            turn.identity();
        } else {
            turn.set(turn.x / size, turn.y / size, turn.z / size, turn.w / size);
        }
        Quaternionf swing = new Quaternionf(q).mul(new Quaternionf(turn).conjugate());
        float twisted = about(turn, bx, by, bz);
        float keptTwist = Math.max(-twist, Math.min(twist, twisted));
        // The swing as a turn vector, split into its fold (about the hinge) and its lean (about the third axis).
        Vector3f lean = new Vector3f(bx, by, bz).cross(hinge[0], hinge[1], hinge[2]);
        if (lean.lengthSquared() < 1.0E-8F) {
            return hinge(q, hinge, min, max);
        }
        lean.normalize();
        if (swing.w < 0.0F) {
            swing.set(-swing.x, -swing.y, -swing.z, -swing.w);
        }
        float s = (float) Math.sqrt(swing.x * swing.x + swing.y * swing.y + swing.z * swing.z);
        float angle = 2.0F * (float) Math.atan2(s, swing.w);
        float fold = 0.0F;
        float across = 0.0F;
        if (s > 1.0E-7F) {
            float k = angle / s;
            fold = (swing.x * hinge[0] + swing.y * hinge[1] + swing.z * hinge[2]) * k;
            across = (swing.x * lean.x + swing.y * lean.y + swing.z * lean.z) * k;
        }
        float keptFold = Math.max(min, Math.min(max, fold));
        float keptAcross = Math.max(-side, Math.min(side, across));
        if (keptFold == fold && keptAcross == across && keptTwist == twisted) {
            return q;
        }
        Vector3f vector = new Vector3f(hinge[0], hinge[1], hinge[2]).mul(keptFold).add(lean.mul(keptAcross));
        float length = vector.length();
        Quaternionf kept = length < 1.0E-7F ? new Quaternionf()
                : new Quaternionf().setAngleAxis(length, vector.x / length, vector.y / length, vector.z / length);
        return q.set(kept.mul(new Quaternionf().setAngleAxis(keptTwist, bx, by, bz)));
    }
}
