package nl.tivek.multiversepowers.creature.animal.client;

import java.util.Arrays;
import net.minecraft.util.Mth;

// A pose as numbers by channel (a joint's turn, a part's offset), so a gait and each act can each work out a whole pose
// and be blended into one by how far each act is in.
public final class Channels {
    public final float[] v;

    public Channels(int count) {
        this.v = new float[count];
    }

    public Channels clear() {
        Arrays.fill(this.v, 0.0F);
        return this;
    }

    public Channels copy(Channels from) {
        System.arraycopy(from.v, 0, this.v, 0, this.v.length);
        return this;
    }

    // Moves every channel `w` of the way to `to`.
    public void mix(Channels to, float w) {
        if (w <= 0.0F) {
            return;
        }
        for (int k = 0; k < this.v.length; k++) {
            this.v[k] = Mth.lerp(w, this.v[k], to.v[k]);
        }
    }

    // A swinging limb: its swing at phase `p`, and how far it is lifted (only while it swings forward).
    public static float swing(float p) {
        return Mth.sin(p);
    }

    public static float lift(float p) {
        return Math.max(0.0F, -Mth.cos(p));
    }

    // A twitch now and then, different for each animal (`seed`): 0 at rest, up to 1 for a few ticks every so often.
    public static float twitch(float age, int seed, int every, float length) {
        float t = (age + (seed * 7919 & 0xFFFF)) % every;
        return t < length ? Mth.sin(t / length * Mth.PI) : 0.0F;
    }
}
