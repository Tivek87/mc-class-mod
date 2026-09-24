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

    /**
     * A curve from {@code from} (at u = 0, moving at speed {@code fromSpeed}) to {@code to} (at u = 1, moving at speed
     * {@code toSpeed}); speeds per whole u. Chain these with matching speeds where they meet and a move never jolts.
     * u is not clamped.
     */
    public static double hermite(double from, double fromSpeed, double to, double toSpeed, double u) {
        double u2 = u * u;
        double u3 = u2 * u;
        return from * (2 * u3 - 3 * u2 + 1) + fromSpeed * (u3 - 2 * u2 + u) + to * (3 * u2 - 2 * u3)
                + toSpeed * (u3 - u2);
    }

    /**
     * A curve through keys, given as (time, value, speed) three by three with time rising: it passes each key's value
     * at its time, moving at its speed (per unit of time), and between two keys it is the {@link #hermite} curve
     * joining them, so a move laid out in keys never jolts. Before the first key it holds the first value and after
     * the last key the last one: give those two keys speed 0 for a move that starts and ends at rest.
     */
    public static double keys(double t, double... keys) {
        int count = keys.length / 3;
        if (count == 0) {
            return 0.0;
        }
        if (t <= keys[0]) {
            return keys[1];
        }
        for (int k = 1; k < count; k++) {
            double until = keys[3 * k];
            if (t < until) {
                double from = keys[3 * k - 3];
                double span = until - from;
                return hermite(keys[3 * k - 2], keys[3 * k - 1] * span, keys[3 * k + 1], keys[3 * k + 2] * span,
                        (t - from) / span);
            }
        }
        return keys[3 * count - 2];
    }

    /** 1 at x = 0, falling smoothly to 0 at x = -1 and x = 1, and 0 beyond: a soft bump. */
    public static double bump(double x) {
        return 1.0 - smoother(Math.abs(x));
    }

    /**
     * What something does after it lands a blow at s = 0 moving at {@code speed}: it gives a little further, springs
     * back past where it landed and trembles to rest there, as on a stiff spring ({@code swing}: how fast it swings,
     * per unit of s; {@code damping} below 1: how soon it calms down). How far off where it landed it is: 0 up to
     * s = 0, and it leaves s = 0 at {@code speed}, so it carries on a curve that lands at that speed without a jolt.
     */
    public static double recoil(double s, double speed, double swing, double damping) {
        if (s <= 0.0) {
            return 0.0;
        }
        double calm = Math.min(damping, 0.99);
        double turn = swing * Math.sqrt(1.0 - calm * calm);
        return speed * Math.exp(-calm * swing * s) * Math.sin(turn * s) / turn;
    }

    /** {@code x} as it is while small, leaning off softly towards {@code limit} (either way) and never past it. */
    public static double soft(double x, double limit) {
        return limit * Math.tanh(x / limit);
    }

    /**
     * A spring let go at t = 0: goes from 0 (at rest) to 1, as a weight on a spring would. {@code speed} is how fast it
     * swings (per unit of t), {@code damping} how soon it calms down: below 1 it overshoots and wobbles a few times,
     * 1 or more it settles without passing 1. 0 before t = 0, and it starts without a jolt.
     */
    public static double spring(double t, double speed, double damping) {
        if (t <= 0.0) {
            return 0.0;
        }
        if (damping >= 1.0) {
            return 1.0 - Math.exp(-speed * t) * (1.0 + speed * t);
        }
        double swing = speed * Math.sqrt(1.0 - damping * damping);
        return 1.0 - Math.exp(-damping * speed * t)
                * (Math.cos(swing * t) + damping / Math.sqrt(1.0 - damping * damping) * Math.sin(swing * t));
    }
}
