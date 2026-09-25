package nl.tivek.multiversepowers.engine.math;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;

public final class Keyframes {
    private Keyframes() {
    }

    public record Key(float tick, boolean stop, float[] values, @Nullable float[] speed) {
        public Key(float tick, boolean stop, float[] values) {
            this(tick, stop, values, null);
        }
    }

    // Hermite through the keys; a stop key halts there, the others pass through with Catmull-Rom speed.
    public static float[] at(Key[] keys, float t) {
        int n = keys.length;
        if (n == 1 || t <= keys[0].tick()) {
            return keys[0].values().clone();
        }
        if (t >= keys[n - 1].tick()) {
            return keys[n - 1].values().clone();
        }
        int i = 0;
        while (i + 2 < n && keys[i + 1].tick() <= t) {
            i++;
        }
        float t0 = keys[i].tick();
        float h = Math.max(1.0E-3F, keys[i + 1].tick() - t0);
        float s = Mth.clamp((t - t0) / h, 0.0F, 1.0F);
        float s2 = s * s;
        float s3 = s2 * s;
        float h00 = 2.0F * s3 - 3.0F * s2 + 1.0F;
        float h10 = s3 - 2.0F * s2 + s;
        float h01 = -2.0F * s3 + 3.0F * s2;
        float h11 = s3 - s2;
        float[] a = keys[i].values();
        float[] b = keys[i + 1].values();
        float[] out = new float[a.length];
        for (int c = 0; c < a.length; c++) {
            float m0 = slope(keys, i, c);
            float m1 = slope(keys, i + 1, c);
            out[c] = h00 * a[c] + h10 * h * m0 + h01 * b[c] + h11 * h * m1;
        }
        return out;
    }

    private static float slope(Key[] keys, int i, int c) {
        float[] said = keys[i].speed();
        if (said != null && !Float.isNaN(said[c])) {
            return said[c];
        }
        if (keys[i].stop() || i == 0 || i == keys.length - 1) {
            return 0.0F;
        }
        return (keys[i + 1].values()[c] - keys[i - 1].values()[c])
                / Math.max(1.0E-3F, keys[i + 1].tick() - keys[i - 1].tick());
    }

    public static float end(Key[] keys) {
        return keys[keys.length - 1].tick();
    }
}
