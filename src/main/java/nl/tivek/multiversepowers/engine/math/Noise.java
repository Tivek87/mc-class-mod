package nl.tivek.multiversepowers.engine.math;

import net.minecraft.world.phys.Vec3;

/**
 * Numbers that look random but are always the same for the same input: every frame, on the server and on every
 * client alike. So a spark, a crack or a piece of debris keeps its own place and way without anything being stored
 * or sent.
 */
public final class Noise {
    private Noise() {
    }

    /** A number from 0 up to 1, the same every time for the same three numbers. */
    public static double of(int a, int b, int c) {
        long h = a * 73856093L ^ b * 19349663L ^ c * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536.0;
    }

    /** A direction (one long) of its own for every pair of numbers: for a speck, a piece or a crack. */
    public static Vec3 direction(int a, int b) {
        double yaw = of(a, b, 0) * Math.PI * 2;
        double y = of(a, b, 1) * 2.0 - 1.0;
        double flat = Math.sqrt(1.0 - y * y);
        return new Vec3(Math.cos(yaw) * flat, y, Math.sin(yaw) * flat);
    }
}
