package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import org.joml.Quaternionf;
import org.joml.Vector3f;

// A body's parts as a pose puts them: each part's place and turn in the frame it hangs in (pixels, as a model part
// takes them), and the turn at each of its joints, the piece past the joint in the axes of the piece before it
// (ModelBends.chain: a knee then an ankle, an elbow then a wrist, a waist then a pelvis). Scratch for the render thread.
public final class BodyPose {
    // The most parts a model a ragdoll moves can have, and the most joints one part bends at.
    public static final int MOST = 32;
    public static final int JOINTS = 2;

    public final Vector3f[] pos = new Vector3f[MOST];
    public final Quaternionf[] rot = new Quaternionf[MOST];
    public final Quaternionf[][] joint = new Quaternionf[JOINTS][MOST];

    public BodyPose() {
        for (int i = 0; i < MOST; i++) {
            this.pos[i] = new Vector3f();
            this.rot[i] = new Quaternionf();
            for (int j = 0; j < JOINTS; j++) {
                this.joint[j][i] = new Quaternionf();
            }
        }
    }

    public BodyPose set(BodyPose other, int n) {
        for (int i = 0; i < n; i++) {
            this.pos[i].set(other.pos[i]);
            this.rot[i].set(other.rot[i]);
            for (int j = 0; j < JOINTS; j++) {
                this.joint[j][i].set(other.joint[j][i]);
            }
        }
        return this;
    }
}
