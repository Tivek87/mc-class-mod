package nl.tivek.multiversepowers.engine.math;

import net.minecraft.util.Mth;

public final class Ease {
    private Ease() {
    }

    public static double smooth(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    public static double smoother(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * c * (c * (c * 6 - 15) + 10);
    }

    public static double backOut(double t) {
        double c = Mth.clamp(t, 0.0, 1.0) - 1.0;
        return 1.0 + c * c * (2.70158 * c + 1.70158);
    }

    public static double hermite(double from, double fromSpeed, double to, double toSpeed, double u) {
        double u2 = u * u;
        double u3 = u2 * u;
        return from * (2 * u3 - 3 * u2 + 1) + fromSpeed * (u3 - 2 * u2 + u) + to * (3 * u2 - 2 * u3)
                + toSpeed * (u3 - u2);
    }

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

    public static double bump(double x) {
        return 1.0 - smoother(Math.abs(x));
    }

    public static double recoil(double s, double speed, double swing, double damping) {
        if (s <= 0.0) {
            return 0.0;
        }
        double calm = Math.min(damping, 0.99);
        double turn = swing * Math.sqrt(1.0 - calm * calm);
        return speed * Math.exp(-calm * swing * s) * Math.sin(turn * s) / turn;
    }

    public static double soft(double x, double limit) {
        return limit * Math.tanh(x / limit);
    }

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
