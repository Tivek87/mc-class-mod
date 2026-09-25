package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.math.Ease;

abstract class HandMotion {
    static final double BURIED = -7.5;
    static final double RISE = 3.5;
    private static final double UNFURL = 4.4;
    private static final double UNFURL_BEATS = 2.2;
    static final double STAGGER = 0.55;
    private static final double LOOSE_CURL = 0.62;
    private static final double LOOSE_HOOK = 0.4;
    private static final double LOOSE_THUMB = 0.55;
    private static final double SPLAY = 0.1;
    private static final double SINK_LIFT = 0.3;

    public final double[] curl = new double[5];
    public double lean;
    public double length;
    public double twist;
    public double flex;
    public double sweep;
    public double spread;
    public double thumbOut;
    public final double[] hook = new double[5];

    static double rise(double t, double to, double speed, double damping) {
        return Mth.lerp(Ease.spring(t - RISE, speed, damping), BURIED, to);
    }

    static double corkscrew(double t, double turn) {
        return turn * (1.0 - Ease.spring(t - RISE, 1.25, 0.55));
    }

    static double window(double t, double in, double full, double fade, double gone) {
        return Ease.smoother((t - in) / (full - in)) * (1.0 - Ease.smoother((t - fade) / (gone - fade)));
    }

    void unfurl(double t, double curl, double hook, double thumb, double spread) {
        for (int k = 0; k < 4; k++) {
            double u = Ease.smoother((t - UNFURL - k * STAGGER) / UNFURL_BEATS);
            this.curl[k] = Mth.lerp(u, LOOSE_CURL, curl);
            this.hook[k] = Mth.lerp(u, LOOSE_HOOK, hook);
        }
        double u = Ease.smoother((t - UNFURL - 0.5 * STAGGER) / UNFURL_BEATS);
        this.curl[4] = Mth.lerp(u, LOOSE_THUMB, thumb);
        this.hook[4] = Mth.lerp(u, LOOSE_HOOK, 0.1);
        this.spread = Mth.lerp(Ease.smoother((t - UNFURL) / (UNFURL_BEATS + 3 * STAGGER)), 0.1, spread)
                + SPLAY * Ease.bump((t - UNFURL - UNFURL_BEATS - 1.5 * STAGGER) / 1.8);
    }

    void cascade(double t, double from, double beats, boolean closing, double curl, double hook,
            double thumb) {
        for (int k = 0; k < 5; k++) {
            double order = k == 4 ? (closing ? 4.0 : 0.5) : closing ? 3 - k : k;
            double u = Ease.smoother((t - from - order * STAGGER) / beats);
            this.curl[k] = Mth.lerp(u, this.curl[k], k == 4 ? thumb : curl);
            this.hook[k] = Mth.lerp(u, this.hook[k], hook);
        }
    }

    void breathe(double t, double amount) {
        if (amount <= 0.0) {
            return;
        }
        this.lean += amount * (0.032 * Math.sin(t * 0.363 + 0.4) + 0.01 * Math.sin(t * 0.83 + 1.1));
        this.length += amount * 0.07 * Math.sin(t * 0.363 + 2.0);
        this.twist += amount * 0.04 * Math.sin(t * 0.263 + 0.7);
        this.flex += amount * 0.035 * Math.sin(t * 0.48 + 2.1);
        this.sweep += amount * 0.018 * Math.sin(t * 0.31 + 0.2);
    }

    void fidget(double t, double amount, boolean middle) {
        if (amount <= 0.0) {
            return;
        }
        for (int k = 0; k < 5; k++) {
            if (k == 1 && !middle) {
                continue;
            }
            double pace = 0.52 + 0.09 * k;
            double curl = this.curl[k];
            this.curl[k] = curl + amount * 0.08 * (1.0 - curl) * (0.5 + 0.5 * Math.sin(t * pace + 1.7 * k));
            this.hook[k] += amount * 0.07 * (0.5 + 0.5 * Math.sin(t * pace * 0.71 + 1.7 * k + 0.9));
        }
    }

    void sink(double t, double from, double until, double side, boolean loosen) {
        double lift = SINK_LIFT * Ease.keys(t, from - 1.5, 0.0, 0.0, from + 1.2, 1.0, 0.0);
        double down = Ease.smoother((t - from - 0.8) / (until - from - 0.8));
        this.length = Mth.lerp(down, this.length + lift, BURIED);
        this.twist += side * 0.3 * down;
        if (loosen) {
            cascade(t, from, 3.0, true, 0.45, 0.35, 0.4);
            this.spread = Mth.lerp(Ease.smoother((t - from) / 4.0), this.spread, 0.35);
        }
    }
}
