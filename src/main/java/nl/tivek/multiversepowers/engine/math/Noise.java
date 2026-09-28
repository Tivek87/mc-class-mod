package nl.tivek.multiversepowers.engine.math;

import net.minecraft.world.phys.Vec3;

public final class Noise {
    private Noise() {
    }

    public static double of(int a, int b, int c) {
        long h = a * 73856093L ^ b * 19349663L ^ c * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536.0;
    }

    // A value from 0 to 1 that wanders smoothly as t runs on: a new random value at every whole t, eased between.
    public static double smooth(int seed, double t) {
        int i = (int) Math.floor(t);
        double u = t - i;
        return of(seed, i, 7) + (of(seed, i + 1, 7) - of(seed, i, 7)) * u * u * (3.0 - 2.0 * u);
    }

    public static Vec3 direction(int a, int b) {
        double yaw = of(a, b, 0) * Math.PI * 2;
        double y = of(a, b, 1) * 2.0 - 1.0;
        double flat = Math.sqrt(1.0 - y * y);
        return new Vec3(Math.cos(yaw) * flat, y, Math.sin(yaw) * flat);
    }
}
