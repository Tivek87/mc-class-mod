package nl.tivek.multiversepowers.character.greenlantern.hand;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;

// The baseball bat, the hourglass, the gravity flip and the black hole.
abstract class HandWonders extends HandShots {
    // Baseball bat: up out of the ground beside the creature, a bat grows out of the ring's light into its fist, side
    // on; it winds back, waggles and swings flat through the creature, follows through and flips the bat away.
    private static final double BAT_FORM = 5.0;
    private static final double BAT_GRIP = 8.5;
    static final double BAT_SWING = 14.5;
    private static final double BAT_HIT = 15.6;
    private static final double BAT_FLIP = 20.5;
    private static final double BAT_BREAK = 26.5;
    public static final int BAT_SWINGS = ticks(BAT_SWING);
    public static final int BAT_HITS = ticks(BAT_HIT);
    public static final int BAT_FLIPS = ticks(BAT_FLIP);
    public static final int BAT_BREAKS = ticks(BAT_BREAK);
    private static final double BAT_AT = BAT_HITS / SLOW;
    // The handle runs through the fist across the hand, the barrel out on the thumb's side and the knob on the little
    // finger's: along is measured that way from the middle of the fist.
    public static final Vec3 BAT_HOLD = new Vec3(-0.1, 3.55, 0.95);
    public static final double BAT_KNOB = -1.25;
    public static final double BAT_TIP = 6.3;
    // At the hit the barrel's middle sweeps through a creature standing this far from the hand.
    static final double BAT_SPOT = 6.0;
    private static final double BAT_LENGTH = 0.7;
    private static final double[] BAT_SWEEP = { 7.5, 0.25, 0.0, 12.5, -1.75, 0.0, 13.7, -1.85, 0.0, BAT_SWING, -1.8,
            0.0, BAT_AT, 0.0, 2.6, 17.2, 1.55, 0.3, 19.5, 1.8, 0.0 };
    private static final double[] BAT_LEAN = { 7.5, 0.22, 0.0, 12.5, 0.15, 0.0, BAT_SWING, 0.12, 0.0, BAT_AT, 0.6,
            0.0, 17.2, 0.2, 0.0, 19.5, 0.08, 0.0 };
    private static final double[] BAT_FLEX = { 9.0, 0.0, 0.0, BAT_SWING, -0.15, 0.0, BAT_AT, 0.1, 0.0, 17.5, -0.35,
            0.0, BAT_FLIP - 0.6, -0.2, 0.0, BAT_FLIP, -0.75, 0.0, BAT_FLIP + 1.5, 0.0, 0.0 };

    // Hourglass: up out of the ground palm up, an hourglass grows standing on the palm; the hand tosses it over and as
    // it lands time stops round it. The sand runs out, the fist closes and crushes it, and time goes on.
    private static final double GLASS_FORM = 5.0;
    private static final double GLASS_FORMED = 8.0;
    private static final double GLASS_TURN = 9.5;
    private static final double GLASS_TURNED = 11.5;
    private static final double TIME_STOP = 12.0;
    private static final double TIME_GO = 60.0;
    private static final double GLASS_CRUSH = 58.5;
    public static final int TIME_STOPS = ticks(TIME_STOP);
    public static final int TIME_GOES = ticks(TIME_GO);
    public static final int GLASS_BREAKS = ticks(GLASS_CRUSH + 0.7);
    public static final Vec3 GLASS_FOOT = new Vec3(0.0, 1.6, 0.55);
    public static final double GLASS_TALL = 2.4;

    // Gravity flip: a hand spread palm down over the creature turns palm up, and everything under it falls up into
    // the sky; it hangs there until the hand turns over again and slams it all back down into the ground.
    static final double GRAVITY_FLIP = 11.0;
    private static final double GRAVITY_TURNED = 12.5;
    private static final double GRAVITY_SLAM = 23.0;
    private static final double GRAVITY_DOWN = 24.4;
    public static final int GRAVITY_FLIPS = ticks(GRAVITY_TURNED - 0.5);
    public static final int GRAVITY_SLAMS = ticks(GRAVITY_DOWN);
    private static final double GRAVITY_LEAN = 0.9;
    private static final double GRAVITY_FLEX = 0.65;

    // Black hole: up out of the ground palm up and cupped; a black hole grows over the palm, drawing everything round
    // it in and round, harder and harder, until the fist closes on it, it falls in on itself and bursts out.
    private static final double HOLE_FORM = 7.0;
    private static final double HOLE_FORMED = 11.0;
    private static final double HOLE_CRUSH = 52.0;
    private static final double HOLE_GONE = 53.5;
    private static final double HOLE_BLAST = 54.1;
    private static final double HOLE_UNSTEADY = 8.0;
    public static final int HOLE_FORMS = ticks(HOLE_FORM);
    public static final int HOLE_CRUSHES = ticks(HOLE_CRUSH);
    public static final int HOLE_BLASTS = ticks(HOLE_BLAST);
    public static final Vec3 HOLE_AT = new Vec3(0.0, 1.7, 2.9);
    public static final double HOLE_SIZE = 1.25;
    // How far round it the hole draws things in and its burst reaches, at scale 1.
    public static final double HOLE_PULL = 18.0;
    public static final double HOLE_BLAST_RADIUS = 14.0;
    private static final double PALM_UP_LEAN = -0.45;
    private static final double PALM_UP_FLEX = -1.12;

    // How grown the bat is at tick t, 0 to 1.
    public static double batGrown(double t) {
        return Ease.smoother((t / SLOW - BAT_FORM) / (BAT_GRIP - BAT_FORM));
    }

    // Ticks since the fist let the bat fly, below 0 while it still holds it.
    public static double batFlying(double t) {
        return t - BAT_FLIPS;
    }

    // How far the bat has broken at tick t: below 0 while whole, 1 when its pieces are gone.
    public static double batBroken(double t) {
        return (t - BAT_BREAKS) / 14.0;
    }

    public static double glassGrown(double t) {
        return Ease.smoother((t / SLOW - GLASS_FORM) / (GLASS_FORMED - GLASS_FORM));
    }

    // How far the hourglass has turned over about its middle, 0 to pi, and how high it is tossed meanwhile.
    public static double glassTurn(double t) {
        return Math.PI * Ease.smoother((t / SLOW - GLASS_TURN) / (GLASS_TURNED - GLASS_TURN));
    }

    public static double glassToss(double t) {
        double u = (t / SLOW - GLASS_TURN) / (GLASS_TURNED - GLASS_TURN);
        return u <= 0.0 || u >= 1.0 ? 0.0 : 4.0 * u * (1.0 - u);
    }

    // How much sand is left in the hourglass's upper half, 1 to 0, while time stands still.
    public static double glassSand(double t) {
        return 1.0 - Mth.clamp((t / SLOW - TIME_STOP) / (TIME_GO - TIME_STOP - 1.0), 0.0, 1.0);
    }

    public static double glassBroken(double t) {
        return (t - GLASS_BREAKS) / 14.0;
    }

    // How still time stands round the hourglass, 0 to 1.
    public static double timeStill(double t) {
        double beat = t / SLOW;
        return Ease.smoother((beat - TIME_STOP) / 1.5) * (1.0 - Ease.smoother((beat - TIME_GO) / 1.0));
    }

    // How far the gravity under the hand is turned over, 0 (as it is) to 1 (falling up).
    public static double gravityFlipped(double t) {
        return window(t / SLOW, GRAVITY_FLIP + 0.5, GRAVITY_TURNED, GRAVITY_SLAM + 0.4, GRAVITY_DOWN);
    }

    // The shockwave of the slam, 0 to 1 as it runs out.
    public static double gravitySlammed(double t) {
        return (t / SLOW - GRAVITY_DOWN) / 4.0;
    }

    // How big the black hole is at tick t, 0 to 1, and how hard it draws.
    public static double holeSize(double t) {
        double beat = t / SLOW;
        double grown = Ease.smoother((beat - HOLE_FORM) / (HOLE_FORMED - HOLE_FORM));
        double crushed = Ease.smoother((beat - HOLE_CRUSH) / (HOLE_GONE - HOLE_CRUSH));
        return grown * (1.0 - crushed) * (1.0 + 0.04 * Math.sin(beat * 2.3));
    }

    // How far the black hole has fallen in on itself, 0 to 1: unsteady a while before the fist closes, its disc
    // whirling faster and brighter as it sinks in, gone as the fist shuts.
    public static double holeCollapse(double t) {
        return Ease.smoother((t / SLOW - HOLE_CRUSH + HOLE_UNSTEADY) / (HOLE_GONE - HOLE_CRUSH + HOLE_UNSTEADY));
    }

    public static double holePull(double t) {
        double beat = t / SLOW;
        return Ease.smooth((beat - HOLE_FORM) / 16.0) * (1.0 - Ease.smoother((beat - HOLE_CRUSH) / 1.0));
    }

    // The blast as the crushed hole bursts out of the fist, 0 to 1 as it runs out.
    public static double holeBurst(double t) {
        return (t / SLOW - HOLE_BLAST) / 5.0;
    }

    // A point on the bat in the hand's own frame, `along` from the middle of the fist towards the barrel's end.
    public static Vec3 batPoint(double along) {
        return BAT_HOLD.add(-along, 0.0, 0.0);
    }

    // Mirrored, the same swing goes the other way round, so it always sends the creature away from the caster.
    void bat(double t, double side) {
        double back = Ease.smoother((t - BAT_FLIP - 2.0) / 5.0);
        this.length = rise(t, BAT_LENGTH, 1.0, 0.6) + 0.15 * Math.sin(t * 2.4)
                * window(t, 12.0, 13.0, BAT_SWING - 0.6, BAT_SWING);
        this.sweep = side * Ease.keys(t, BAT_SWEEP) * (1.0 - back);
        this.lean = Ease.keys(t, BAT_LEAN) * (1.0 - back) + 0.1 * back
                - 0.08 * Ease.recoil(t - BAT_AT, 1.0, 7.5, 0.32);
        this.flex = Ease.keys(t, BAT_FLEX) + 0.06 * Math.sin(t * 2.4) * window(t, 12.0, 13.0, BAT_SWING - 0.6,
                BAT_SWING);
        this.twist = -Math.PI * 0.5 * Ease.smoother((t - BAT_FORM + 0.5) / 4.0) * (1.0 - back) + corkscrew(t, 0.5);
        unfurl(t, 0.2, 0.15, 0.3, 0.8);
        cascade(t, BAT_GRIP - 1.2, 1.2, true, 0.9, 0.25, 0.8);
        this.spread = Mth.lerp(Ease.smoother((t - BAT_GRIP + 1.2) / 1.2), this.spread, 0.0);
        for (int k = 0; k < 4; k++) {
            this.curl[k] += 0.04 * Math.sin(t * 2.4 + k * 0.7) * window(t, 12.0, 13.0, BAT_SWING - 0.6, BAT_SWING);
        }
        cascade(t, BAT_FLIP - 0.3, 0.6, false, 0.1, 0.1, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - BAT_FLIP + 0.3) / 0.6), this.spread, 0.9);
        cascade(t, BAT_FLIP + 3.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - BAT_FLIP - 3.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.5 * window(t, 5.0, 7.0, 11.0, 12.0));
        this.sink(t, SINK[BAT], LIFE[BAT], 1.0, true);
    }

    // Palm up, as a tray: the arm leans back and the wrist bends back as far, so the palm faces the sky.
    private void palmUp(double t, double high) {
        this.length = rise(t, high, 1.0, 0.6);
        double up = Ease.smoother((t - 3.0) / 3.0);
        this.lean = Mth.lerp(up, 0.1, PALM_UP_LEAN);
        this.flex = Mth.lerp(up, 0.0, PALM_UP_FLEX);
        this.twist = corkscrew(t, 0.5);
    }

    void hourglass(double t) {
        palmUp(t, 3.0);
        double toss = Ease.bump((t - GLASS_TURN - 0.4) / 1.1);
        double stop = Ease.recoil(t - TIME_STOP, 1.0, 6.0, 0.35);
        double crush = window(t, GLASS_CRUSH, GLASS_CRUSH + 1.2, TIME_GO + 1.5, TIME_GO + 4.0);
        this.length += 0.35 * toss + 0.3 * stop - 0.25 * crush;
        this.flex += 0.3 * toss - 0.12 * stop + 0.3 * crush;
        this.lean += 0.06 * stop;
        unfurl(t, 0.12, 0.1, 0.25, 0.5);
        for (int k = 0; k < 4; k++) {
            this.curl[k] += 0.12 * toss;
        }
        cascade(t, GLASS_CRUSH, 1.2, true, 1.0, 0.45, 0.9);
        this.spread = Mth.lerp(Ease.smoother((t - GLASS_CRUSH) / 1.2), this.spread, 0.0);
        cascade(t, TIME_GO + 2.0, 2.5, false, 0.3, 0.2, 0.4);
        this.spread = Mth.lerp(Ease.smoother((t - TIME_GO - 2.0) / 2.5), this.spread, 0.5);
        fidget(t, 0.5 * window(t, TIME_STOP + 3.0, TIME_STOP + 6.0, GLASS_CRUSH - 3.0, GLASS_CRUSH), true);
        breathe(t, 0.6 * window(t, 5.0, 7.0, GLASS_CRUSH - 2.0, GLASS_CRUSH));
        this.sink(t, SINK[HOURGLASS], LIFE[HOURGLASS], 1.0, true);
    }

    void gravity(double t) {
        double turned = Ease.smoother((t - GRAVITY_FLIP) / (GRAVITY_TURNED - GRAVITY_FLIP));
        double slam = t < GRAVITY_SLAM ? 0.0 : Ease.hermite(0.0, 0.0, 1.0, 1.4,
                Mth.clamp((t - GRAVITY_SLAM) / (GRAVITY_DOWN - GRAVITY_SLAM), 0.0, 1.0));
        double flat = 1.0 - Ease.smoother((t - GRAVITY_DOWN - 2.5) / 3.0);
        double up = turned * (1.0 - slam);
        double recoil = Ease.recoil(t - GRAVITY_DOWN, 1.0, 7.0, 0.3);
        double reach = Ease.smoother((t - 3.5) / 3.0);
        this.length = rise(t, 3.0, 1.0, 0.6) + 1.0 * up + 0.25 * Math.sin(t * 0.9) * up - 1.4 * slam * flat
                + 0.3 * recoil;
        this.lean = GRAVITY_LEAN * reach + 0.35 * slam * flat - 0.15 * recoil;
        this.flex = Mth.lerp(up, GRAVITY_FLEX * reach, -0.67) + 0.2 * slam * flat;
        this.twist = Math.PI * up + corkscrew(t, 0.5);
        unfurl(t, 0.15, 0.1, 0.25, 1.0);
        double beckon = window(t, GRAVITY_TURNED, GRAVITY_TURNED + 1.0, GRAVITY_SLAM - 1.0, GRAVITY_SLAM);
        for (int k = 0; k < 4; k++) {
            this.curl[k] += beckon * (0.25 + 0.2 * Math.sin(t * 1.3 - k * 0.6));
            this.hook[k] += beckon * 0.2;
        }
        this.spread = Mth.lerp(beckon, this.spread, 0.6);
        breathe(t, 0.5 * window(t, 5.0, 7.0, GRAVITY_FLIP - 1.0, GRAVITY_FLIP));
        this.sink(t, SINK[GRAVITY], LIFE[GRAVITY], 1.0, true);
    }

    void blackHole(double t) {
        palmUp(t, 2.8);
        double strain = window(t, HOLE_FORMED, HOLE_FORMED + 6.0, HOLE_CRUSH, HOLE_CRUSH + 0.5);
        double crush = window(t, HOLE_CRUSH, HOLE_GONE, HOLE_BLAST, HOLE_BLAST + 0.4);
        double blast = Ease.recoil(t - HOLE_BLAST, 1.0, 6.5, 0.3);
        this.length += -0.35 * strain - 0.4 * crush + 0.5 * blast + 0.06 * strain * Math.sin(t * 11.0);
        this.lean += 0.03 * strain * Math.sin(t * 9.0 + 1.0) - 0.25 * blast;
        this.flex += 0.15 * crush - 0.3 * blast;
        unfurl(t, 0.45, 0.3, 0.45, 0.35);
        for (int k = 0; k < 4; k++) {
            this.curl[k] += 0.06 * strain * (0.5 + 0.5 * Math.sin(t * 7.0 + k * 1.3));
        }
        cascade(t, HOLE_CRUSH, 1.0, true, 1.0, 0.45, 0.9);
        this.spread = Mth.lerp(Ease.smoother((t - HOLE_CRUSH) / 1.0), this.spread, 0.0);
        cascade(t, HOLE_BLAST - 0.1, 0.4, false, 0.0, 0.0, 0.1);
        this.spread = Mth.lerp(Ease.smoother((t - HOLE_BLAST + 0.1) / 0.4), this.spread, 1.0);
        cascade(t, HOLE_BLAST + 3.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - HOLE_BLAST - 3.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.5 * window(t, 5.0, 7.0, HOLE_FORMED, HOLE_FORMED + 2.0));
        this.sink(t, SINK[BLACKHOLE], LIFE[BLACKHOLE], 1.0, true);
    }
}
