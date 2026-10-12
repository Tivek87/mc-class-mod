package nl.tivek.multiversepowers.engine.math;

import java.util.SplittableRandom;

// Gradient noise in three dimensions and sums of it: smooth values from -1 to 1 for painting clouds, land and gas.
public final class Perlin {
    private static final int[] P = new int[512];

    static {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        SplittableRandom random = new SplittableRandom(0x5EEDL);
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        for (int i = 0; i < 512; i++) {
            P[i] = p[i & 255];
        }
    }

    private Perlin() {
    }

    public static double noise(double x, double y, double z) {
        double fx = Math.floor(x);
        double fy = Math.floor(y);
        double fz = Math.floor(z);
        int xi = (int) fx & 255;
        int yi = (int) fy & 255;
        int zi = (int) fz & 255;
        x -= fx;
        y -= fy;
        z -= fz;
        double u = fade(x);
        double v = fade(y);
        double w = fade(z);
        int a = P[xi] + yi;
        int aa = P[a] + zi;
        int ab = P[a + 1] + zi;
        int b = P[xi + 1] + yi;
        int ba = P[b] + zi;
        int bb = P[b + 1] + zi;
        double x1 = lerp(u, grad(P[aa], x, y, z), grad(P[ba], x - 1, y, z));
        double x2 = lerp(u, grad(P[ab], x, y - 1, z), grad(P[bb], x - 1, y - 1, z));
        double y1 = lerp(v, x1, x2);
        x1 = lerp(u, grad(P[aa + 1], x, y, z - 1), grad(P[ba + 1], x - 1, y, z - 1));
        x2 = lerp(u, grad(P[ab + 1], x, y - 1, z - 1), grad(P[bb + 1], x - 1, y - 1, z - 1));
        return lerp(w, y1, lerp(v, x1, x2));
    }

    public static double fbm(double x, double y, double z, int octaves) {
        double sum = 0.0;
        double amp = 1.0;
        double total = 0.0;
        for (int i = 0; i < octaves; i++) {
            sum += noise(x, y, z) * amp;
            total += amp;
            amp *= 0.5;
            x = x * 2.03 + 17.1;
            y = y * 2.03 - 9.7;
            z = z * 2.03 + 3.3;
        }
        return sum / total * 1.6;
    }

    public static double ridged(double x, double y, double z, int octaves) {
        double sum = 0.0;
        double amp = 1.0;
        double total = 0.0;
        for (int i = 0; i < octaves; i++) {
            double n = 1.0 - Math.abs(noise(x, y, z));
            sum += n * n * amp;
            total += amp;
            amp *= 0.5;
            x = x * 2.07 + 5.3;
            y = y * 2.07 + 11.9;
            z = z * 2.07 - 7.1;
        }
        return sum / total;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
