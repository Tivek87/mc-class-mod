package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import java.util.Arrays;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Two poses of one body blended: the trunk as it is, every joint by its own turn, and every other part in the frame of
// the trunk's piece it hangs from, so arms and legs stay on the body the whole way. Render thread only.
public final class PoseBlend {
    private static final Vector3f V = new Vector3f();
    private static final Vector3f W = new Vector3f();
    private static final Quaternionf A = new Quaternionf();
    private static final Quaternionf B = new Quaternionf();
    private static final Quaternionf BACK = new Quaternionf();
    private static final float[] EVEN = new float[BodyPose.MOST];

    private PoseBlend() {
    }

    // Pose a blended into pose b: the trunk and its joints by `trunk`, every other part and its joints by `limbs`.
    public static void blend(Hanging body, BodyPose a, BodyPose b, float trunk, float limbs, BodyPose out) {
        blend(body, a, b, trunk, trunk, limbs, out);
    }

    // As above, the trunk's turn and joints by `turn` and its place by `place`.
    public static void blend(Hanging body, BodyPose a, BodyPose b, float turn, float place, float limbs,
            BodyPose out) {
        Arrays.fill(EVEN, 0, body.n, limbs);
        blend(body, a, b, turn, place, EVEN, out);
    }

    // As above, each other part i and its joints by its own share limbs[i].
    static void blend(Hanging body, BodyPose a, BodyPose b, float turn, float place, float[] limbs, BodyPose out) {
        int core = body.core;
        for (int i = 0; i < body.n; i++) {
            float w = i == core ? turn : limbs[i];
            for (int j = 0; j < BodyPose.JOINTS; j++) {
                out.joint[j][i].set(a.joint[j][i]).slerp(b.joint[j][i], w);
            }
        }
        out.pos[core].set(a.pos[core]).lerp(b.pos[core], place);
        out.rot[core].set(a.rot[core]).slerp(b.rot[core], turn);
        for (int i = 0; i < body.n; i++) {
            if (i == core) {
                continue;
            }
            inTrunk(body, i, a, V, A);
            inTrunk(body, i, b, W, B);
            V.lerp(W, limbs[i]);
            A.slerp(B, limbs[i]);
            fromTrunk(body, i, V, A, out);
        }
    }

    // Part i's place and turn in the trunk's own frame and, past its joints, in the frame of the piece it hangs from.
    static void inTrunk(Hanging body, int i, BodyPose pose, Vector3f p, Quaternionf r) {
        int core = body.core;
        if (body.inside[i]) {
            p.set(pose.pos[i]);
            r.set(pose.rot[i]);
        } else {
            p.set(pose.pos[i]).mul(body.scale[i]).add(body.move[i]).sub(pose.pos[core]);
            BACK.set(pose.rot[core]).conjugate().transform(p).div(body.trunk);
            r.set(BACK).mul(pose.rot[i]);
        }
        for (int j = 0; j < body.past(i); j++) {
            float[] k = body.joints[j];
            p.sub(k[0], k[1], k[2]);
            BACK.set(pose.joint[j][core]).conjugate().transform(p).add(k[0], k[1], k[2]);
            r.premul(BACK);
        }
    }

    // Back from the frame of the trunk's piece part i hangs from to where it hangs, the trunk and its joints as `out`
    // has them already: into out.
    static void fromTrunk(Hanging body, int i, Vector3f p, Quaternionf r, BodyPose out) {
        int core = body.core;
        for (int j = body.past(i) - 1; j >= 0; j--) {
            float[] k = body.joints[j];
            Quaternionf joint = out.joint[j][core];
            p.sub(k[0], k[1], k[2]);
            joint.transform(p).add(k[0], k[1], k[2]);
            r.premul(joint);
        }
        if (body.inside[i]) {
            out.pos[i].set(p);
            out.rot[i].set(r);
        } else {
            out.rot[core].transform(p.mul(body.trunk)).add(out.pos[core]);
            out.pos[i].set(p).sub(body.move[i]).div(body.scale[i]);
            out.rot[i].set(out.rot[core]).mul(r);
        }
    }
}
