package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * The ways a giant hand moves whatever it does (see {@link HandPose}): its forearm, wrist and fingers, and how it
 * bursts up out of the ground springing and corkscrewing, unfurls its fingers from a loose fist and closes and opens
 * them one after another, breathes and fidgets while it waits, and sinks back into the ground.
 */
abstract class HandMotion {
    // How far under the ground the wrist starts, at scale 1: the whole hand is under it then.
    static final double BURIED = -7.5;
    // The beat it starts to move up under the ground, the tips of its fingers breaking out of it about when the ring's
    // light arrives.
    static final double RISE = 3.5;
    // Its fingers unfurling from a loose fist as it comes out: from when, over how long each, how far apart one after
    // another (as they close and open later on too), how loose that fist is, and how far they splay on the way.
    private static final double UNFURL = 4.4;
    private static final double UNFURL_BEATS = 2.2;
    static final double STAGGER = 0.55;
    private static final double LOOSE_CURL = 0.62;
    private static final double LOOSE_HOOK = 0.4;
    private static final double LOOSE_THUMB = 0.55;
    private static final double SPLAY = 0.1;
    // How far it lifts itself before it sinks back into the ground.
    private static final double SINK_LIFT = 0.3;

    /**
     * How far each finger curls (index, middle, ring, little finger, thumb), 0 open to 1 a fist (a little below 0 as
     * they are flung back).
     */
    public final double[] curl = new double[5];
    /** How far the forearm leans over towards what it reaches for, in radians from standing straight up. */
    public double lean;
    /** How far the wrist is out of the ground along the forearm, in blocks at scale 1 (below 0 still under it). */
    public double length;
    /** How far the forearm is turned about its own length, in radians: 0 has the palm to what it reaches for. */
    public double twist;
    /** How far the wrist bends the hand towards its palm, in radians. */
    public double flex;
    /** How far it sweeps round its base from what it reaches for, in radians (a smack). */
    public double sweep;
    /** How far the fingers are spread, 0 together to 1 wide. */
    public double spread;
    /**
     * How far the two outer joints of each finger (index, middle, ring, little finger, thumb) bend on top of its curl,
     * 0 not at all to 1 hooked.
     */
    public final double[] hook = new double[5];

    // ---- How it moves ----

    /** How far out of the ground it has burst: from under it at {@link #RISE}, springing up past {@code to} onto it. */
    static double rise(double t, double to, double speed, double damping) {
        return Mth.lerp(Ease.spring(t - RISE, speed, damping), BURIED, to);
    }

    /** How far it is still turned about its forearm as it corkscrews up out of the ground: {@code turn}, unwinding. */
    static double corkscrew(double t, double turn) {
        return turn * (1.0 - Ease.spring(t - RISE, 1.25, 0.55));
    }

    /** 0 before {@code in}, softly up to 1 by {@code full}, and softly back to 0 from {@code fade} to {@code gone}. */
    static double window(double t, double in, double full, double fade, double gone) {
        return Ease.smoother((t - in) / (full - in)) * (1.0 - Ease.smoother((t - fade) / (gone - fade)));
    }

    /**
     * Its fingers unfurling from a loose fist as it comes up out of the ground, the index finger first and the little
     * finger last, into a hand curled and hooked this far, its thumb that far and its fingers spread that wide: they
     * splay a little wider on the way and settle back.
     */
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

    /**
     * Carries its fingers on from where they are to this curl and hook (the thumb to {@code thumb}), one after another
     * {@link #STAGGER} apart and each over {@code beats} from {@code from}: when {@code closing} from the little finger
     * with the thumb last, over the others, else from the index finger with the thumb right after it.
     */
    void cascade(double t, double from, double beats, boolean closing, double curl, double hook,
            double thumb) {
        for (int k = 0; k < 5; k++) {
            double order = k == 4 ? (closing ? 4.0 : 0.5) : closing ? 3 - k : k;
            double u = Ease.smoother((t - from - order * STAGGER) / beats);
            this.curl[k] = Mth.lerp(u, this.curl[k], k == 4 ? thumb : curl);
            this.hook[k] = Mth.lerp(u, this.hook[k], hook);
        }
    }

    /** A slow breath of a sway, {@code amount} of it (0 still, 1 full), each part at a pace of its own. */
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

    /** Its fingers drifting a little, {@code amount} of it, each at a pace of its own; the middle one too if asked. */
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

    /**
     * It sinks back into the ground from {@code from} until {@code until}: it lifts itself a little first, then goes
     * down, turning a little as it goes and (when {@code loosen}) its fingers curling loosely.
     */
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
