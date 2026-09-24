package nl.tivek.multiversepowers.engine.math;

import net.minecraft.util.Mth;

/** Colours as the game keeps them: 0xRRGGBB, with the alpha (how see-through) apart from 0 to 255. */
public final class Colors {
    private Colors() {
    }

    /** How strongly something shows, 0 to 1, as an alpha from 0 to 255. */
    public static int alpha(double value) {
        return (int) (255 * Mth.clamp(value, 0.0, 1.0));
    }

    /** The same colour, darker (below 1) or lighter (above 1). */
    public static int shade(int rgb, double amount) {
        int red = (int) Mth.clamp((rgb >> 16 & 0xFF) * amount, 0.0, 255.0);
        int green = (int) Mth.clamp((rgb >> 8 & 0xFF) * amount, 0.0, 255.0);
        int blue = (int) Mth.clamp((rgb & 0xFF) * amount, 0.0, 255.0);
        return red << 16 | green << 8 | blue;
    }

    /** Between two colours: 0 = the first, 1 = the second. */
    public static int mix(int a, int b, float t) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        int red = (int) Mth.lerp(c, a >> 16 & 0xFF, b >> 16 & 0xFF);
        int green = (int) Mth.lerp(c, a >> 8 & 0xFF, b >> 8 & 0xFF);
        int blue = (int) Mth.lerp(c, a & 0xFF, b & 0xFF);
        return red << 16 | green << 8 | blue;
    }
}
