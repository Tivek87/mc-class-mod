package nl.tivek.multiversepowers.engine.math;

import net.minecraft.util.Mth;

/**
 * Easing curves: how a move speeds up and slows down between its start (0) and its end (1). The same on the server
 * and on every client, so both sides can play one timeline alike.
 */
public final class Ease {
    private Ease() {
    }

    /** 0 below 0, 1 above 1, and a smooth S-curve in between: starts slow, speeds up, slows down again. */
    public static double smooth(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    /** An even softer S-curve than {@link #smooth}: its speed, too, starts and ends without a jolt. */
    public static double smoother(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * c * (c * (c * 6 - 15) + 10);
    }

    /** Shoots a little past 1 and settles back onto it: something that snaps into place. */
    public static double backOut(double t) {
        double c = Mth.clamp(t, 0.0, 1.0) - 1.0;
        return 1.0 + c * c * (2.70158 * c + 1.70158);
    }
}
