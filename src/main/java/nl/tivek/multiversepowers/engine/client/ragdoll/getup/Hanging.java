package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

import java.util.Arrays;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import org.joml.Vector3f;

// How a body's parts hang together, for blending two of its poses: each part hangs from a piece of the trunk (0 its
// own, 1 past its waist, 2 past its pelvis, where its joints are `joints`, in the trunk's own pixels); it is drawn
// beside the trunk, its place taken into the trunk's parent's pixels by a scale and a move (a young creature's head is
// drawn bigger), or inside it, placed in the trunk's own frame.
public final class Hanging {
    public final int n;
    public final int core;
    public final boolean[] inside;
    public final int[] hang;
    public final float[][] joints;
    public final float[] scale = new float[BodyPose.MOST];
    public final Vector3f[] move = new Vector3f[BodyPose.MOST];
    public final Vector3f trunk = new Vector3f(1.0F, 1.0F, 1.0F);

    public Hanging(int n, int core, boolean[] inside, int[] hang, ModelBends.Bend[] trunk) {
        this.n = n;
        this.core = core;
        this.inside = inside;
        this.hang = hang;
        this.joints = new float[trunk.length][];
        for (int j = 0; j < trunk.length; j++) {
            this.joints[j] = trunk[j].knee();
        }
        Arrays.fill(this.scale, 1.0F);
        for (int i = 0; i < BodyPose.MOST; i++) {
            this.move[i] = new Vector3f();
        }
    }

    // How many of the trunk's joints part i hangs past.
    int past(int i) {
        return Math.min(this.hang[i], this.joints.length);
    }
}
