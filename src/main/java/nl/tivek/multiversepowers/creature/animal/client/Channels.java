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

    // How far an eye is shut, 0 to 1: a quick blink every few seconds, at uneven times, different for each animal.
    public static float blink(float age, int seed) {
        return Math.max(twitch(age, seed, 89, 3.5F), twitch(age, seed + 17, 151, 4.0F));
    }

    // The channels' own springs: `fast` for every channel but those listed, which follow more slowly and swing a
    // little past (ears, a tail, a trunk), as loose parts lag behind the body that carries them.
    public static double[][] springs(int count, double fast, double slow, double looseDamping, int... loose) {
        double[] frequencies = new double[count];
        double[] dampings = new double[count];
        Arrays.fill(frequencies, fast);
        Arrays.fill(dampings, 1.0);
        for (int k : loose) {
            frequencies[k] = slow;
            dampings[k] = looseDamping;
        }
        return new double[][] { frequencies, dampings };
    }
}
