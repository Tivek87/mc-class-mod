package nl.tivek.multiversepowers.creature.client;

import net.minecraft.util.Mth;

// Points on a creature's trunk as its model turns it: where its neck and shoulders are once it leans, twists and rolls.
public final class Trunk {
    private Trunk() {
    }

    // A point in the trunk's frame (pixels from its pivot), the trunk turned as a model part turns: about z, then y,
    // then x, applied to the point in that order from the inside out. Returned as an offset from the pivot.
    public static float[] turn(float x, float y, float z, float xRot, float yRot, float zRot) {
        float c = Mth.cos(xRot);
        float s = Mth.sin(xRot);
        float y1 = y * c - z * s;
        float z1 = y * s + z * c;
        c = Mth.cos(yRot);
        s = Mth.sin(yRot);
        float x2 = x * c + z1 * s;
        float z2 = -x * s + z1 * c;
        c = Mth.cos(zRot);
        s = Mth.sin(zRot);
        return new float[] { x2 * c - y1 * s, x2 * s + y1 * c, z2 };
    }

    // Eases in from `from` to `to` (ticks), 0 before and 1 after.
    public static float in(double t, double from, double to) {
        double x = Mth.clamp((t - from) / (to - from), 0.0, 1.0);
        return (float) (x * x * (3.0 - 2.0 * x));
    }
}
