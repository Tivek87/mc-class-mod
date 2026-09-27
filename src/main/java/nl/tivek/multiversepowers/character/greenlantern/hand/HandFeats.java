package nl.tivek.multiversepowers.character.greenlantern.hand;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;

// The ragdoll and its catch, the finger gun, the scissors, the swallow, the ring beam and the scoop.
abstract class HandFeats extends HandTricks {
    private static final double FEAT_KICK = 7.5;
    private static final double FEAT_CALM = 0.32;

    // Ragdoll: it grabs like the grab, then slams its catch down 2 to 5 times, each time to its own side, winds up
    // swinging faster and faster and flings it far away.
    private static final double RAGDOLL_FIRST = 19.0;
    private static final double RAGDOLL_EVERY = 5.0;
    private static final double RAGDOLL_WIND = 8.0;
    private static final double RAGDOLL_LIFT = 2.2;
    private static final double RAGDOLL_SLAM = 1.35;
    // The catch: a fist out of a portal under a falling creature, squeezing it at most 3 seconds.
    static final double CATCH_AT = 5.0;
    public static final int CATCH_CATCHES = ticks(CATCH_AT);
    public static final int CATCH_LETS_GO = CATCH_CATCHES + 60;
    private static final double LETGO_AT = CATCH_LETS_GO / SLOW;
    private static final double CATCH_LENGTH = 1.2;
    // Finger gun: three shots.
    private static final double[] GUN_BEATS = { 14.0, 18.0, 22.0 };
    public static final int[] GUN_SHOTS = { ticks(GUN_BEATS[0]), ticks(GUN_BEATS[1]), ticks(GUN_BEATS[2]) };
    public static final int GUN_SMOKES = ticks(26.0);
    private static final double[] GUN_AT = { GUN_SHOTS[0] / SLOW, GUN_SHOTS[1] / SLOW, GUN_SHOTS[2] / SLOW };
    public static final Vec3 GUN_TIP = new Vec3(-1.12, 6.2, 0.0);
    private static final double GUN_AWAY = 6.0;
    // Scissors: two snips.
    private static final double[] SNIP_BEATS = { 15.0, 21.0 };
    public static final int[] SNIPS = { ticks(SNIP_BEATS[0]), ticks(SNIP_BEATS[1]) };
    private static final double[] SNIP_AT = { SNIPS[0] / SLOW, SNIPS[1] / SLOW };
    public static final Vec3 SNIP_POINT = new Vec3(-0.75, 4.6, 0.0);
    // Swallow: up out of a portal in the ground, it grabs the creature and pulls it down in; it falls out of the sky.
    public static final int SWALLOW_CATCHES = GRAB_CATCHES;
    public static final int SWALLOW_GONE = ticks(GRAB_CATCHES / SLOW + 3.5);
    public static final int SKY_OPENS = SWALLOW_GONE - 4;
    public static final int SKY_SHUTS = SWALLOW_GONE + 18;
    public static final double SKY_HEIGHT = 14.0;
    // Ring beam: a fist aims its ring at the creature, charges it and fires a beam of light.
    static final double RINGBEAM_CHARGE = 10.0;
    public static final int BEAM_FIRES = ticks(18.0);
    public static final int BEAM_STOPS = ticks(28.0);
    private static final double FIRE_AT = BEAM_FIRES / SLOW;
    private static final double STOP_AT = BEAM_STOPS / SLOW;
    public static final Vec3 RING_POINT = new Vec3(-0.38, 3.72, 0.58);
    // Scoop: a palm slides under the creature and tosses it up over the hand, away from the caster.
    static final double SCOOP_AT = ticks(15.0) / SLOW;
    public static final int SCOOP_TOSSES = ticks(15.0);
    public static final Vec3 SCOOP_POINT = new Vec3(0.0, 2.6, 0.8);

    public static int ragdollSlams(int extra) {
        return 2 + (extra & 3);
    }

    // Each slam goes left, ahead or right, picked when the hand came.
    public static double ragdollSide(int extra, int k) {
        int bits = extra >> (2 + 2 * k) & 3;
        return bits == 0 ? -Math.PI * 0.5 : bits == 2 ? Math.PI * 0.5 : 0.0;
    }

    public static int ragdollSlam(int k) {
        return ticks(RAGDOLL_FIRST + RAGDOLL_EVERY * k);
    }

    public static int ragdollLaunch(int extra) {
        return ticks(RAGDOLL_FIRST + RAGDOLL_EVERY * (ragdollSlams(extra) - 1) + 3.0 + RAGDOLL_WIND);
    }

    public static int ragdollWinds(int extra) {
        return ticks(RAGDOLL_FIRST + RAGDOLL_EVERY * (ragdollSlams(extra) - 1) + 3.0);
    }

    static double ragdollEnds(int extra) {
        return ragdollLaunch(extra) / SLOW + 26.0;
    }

    static double ragdollSinks(int extra) {
        return ragdollLaunch(extra) / SLOW + 8.0;
    }

    // Out of a wall, leaning forward means down the wall: there it winds up downward and throws upward instead.
    void ragdoll(double t, int extra, boolean wall) {
        double throwWay = wall ? -1.0 : 1.0;
        double caught = GRAB_CATCHES / SLOW;
        this.grab(Math.min(t, caught + 1.0));
        if (t <= caught) {
            return;
        }
        int slams = ragdollSlams(extra);
        double lifted = Ease.smoother((t - caught) / 4.0);
        this.length = Mth.lerp(lifted, this.length, RAGDOLL_LIFT);
        this.lean = Mth.lerp(lifted, this.lean, -0.1);
        this.flex = Mth.lerp(lifted, this.flex, 0.0);
        this.sweep = 0.0;
        for (int k = 0; k < slams; k++) {
            double hit = ragdollSlam(k) / SLOW;
            double dir = ragdollSide(extra, k);
            this.sweep = Mth.lerp(Ease.smoother((t - hit + 4.0) / 2.5), this.sweep, dir);
            double up = Ease.smoother((t - hit + 3.5) / 2.3);
            this.lean = Mth.lerp(up, this.lean, -0.35);
            this.length = Mth.lerp(up, this.length, RAGDOLL_LIFT + 0.4);
            double u = (t - hit + 1.2) / 1.2;
            double down = u <= 0.0 ? 0.0 : u >= 1.0 ? 1.0 : Ease.hermite(0.0, 0.0, 1.0, 2.4, u);
            this.lean = Mth.lerp(down, this.lean, RAGDOLL_SLAM) + Ease.recoil(t - hit, -1.2, FEAT_KICK, FEAT_CALM);
            this.length = Mth.lerp(down, this.length, RAGDOLL_LIFT + 0.2);
            this.flex = 0.3 * Ease.recoil(t - hit, 1.5, FEAT_KICK, FEAT_CALM);
        }
        double wind = ragdollWinds(extra) / SLOW;
        double launch = ragdollLaunch(extra) / SLOW;
        double into = Mth.clamp((t - wind) / (launch - wind), 0.0, 1.0);
        if (t > wind) {
            // Swinging up and down ever faster, bending back further and further before the throw.
            double phase = Math.PI * 2.0 * (1.2 * into + 1.9 * into * into) * (launch - wind) / RAGDOLL_WIND;
            double swing = (0.25 + 0.45 * into) * Math.sin(phase);
            double settle = Ease.smoother((t - wind) / 2.0);
            this.sweep = Mth.lerp(settle, this.sweep, 0.0);
            this.lean = Mth.lerp(settle, this.lean, throwWay * (-0.2 - 0.5 * into) + swing);
            this.length = Mth.lerp(settle, this.length, RAGDOLL_LIFT + 0.5);
            this.flex = -0.5 * into * settle * throwWay;
        }
        double throwing = Ease.smoother((t - launch + 0.4) / 0.9);
        this.lean = Mth.lerp(throwing, this.lean, throwWay) + throwWay * Ease.recoil(t - launch - 0.5, 1.0,
                FEAT_KICK, FEAT_CALM);
        this.flex = Mth.lerp(throwing, this.flex, 0.35 * throwWay);
        cascade(t, launch - 0.1, 0.8, false, 0.1, 0.1, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - launch) / 1.2), this.spread, 0.9);
        double end = ragdollSinks(extra);
        cascade(t, launch + 4.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - launch - 4.0) / 3.0), this.spread, 0.5);
        breathe(t, window(t, launch + 2.0, launch + 4.0, end - 1.0, end));
        this.sink(t, end, ragdollEnds(extra), 1.0, true);
    }

    // Up out of a portal under the falling creature at once, fingers spread, then shut into a fist round it,
    // squeezing, till it lets go.
    void catchHand(double t) {
        this.lean = 0.0;
        this.length = Mth.lerp(Ease.smoother((t - 0.8) / 3.2), BURIED, CATCH_LENGTH)
                + 0.3 * Ease.bump((t - CATCH_AT) / 1.5);
        for (int k = 0; k < 4; k++) {
            this.curl[k] = 0.1;
            this.hook[k] = 0.1;
        }
        this.curl[4] = 0.2;
        this.spread = 1.0;
        cascade(t, CATCH_AT - 1.0, 1.2, true, 1.0, 0.2, 0.9);
        this.spread = Mth.lerp(Ease.smoother((t - CATCH_AT + 1.0) / 1.2), this.spread, 0.0);
        double squeeze = window(t, CATCH_AT + 1.0, CATCH_AT + 2.0, LETGO_AT - 1.0, LETGO_AT);
        for (int k = 0; k < 5; k++) {
            this.curl[k] += squeeze * 0.06 * Math.sin(t * 2.4 + k);
        }
        this.twist = squeeze * 0.08 * Math.sin(t * 1.7);
        this.flex = squeeze * 0.05 * Math.sin(t * 3.1);
        cascade(t, LETGO_AT - 0.2, 1.0, false, 0.1, 0.1, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - LETGO_AT) / 1.2), this.spread, 0.9);
        this.length = Mth.lerp(Ease.smoother((t - LETGO_AT - 3.0) / 6.0), this.length, BURIED);
    }

    // Level out of a portal, pointing a finger gun: bang, bang, bang, each with a kick and the thumb dropping like a
    // hammer, then a pause as if to blow the smoke off.
    void fingerGun(double t) {
        double kick = 0.0;
        double hammer = 0.0;
        for (double shot : GUN_AT) {
            kick += Ease.recoil(t - shot, 1.0, FEAT_KICK, FEAT_CALM);
            hammer += Ease.bump((t - shot) / 0.8);
        }
        this.lean = Math.PI * 0.5;
        this.length = rise(t, 0.8, 1.1, 0.6) - 0.35 * kick;
        this.flex = -0.5 * kick - 0.25 * window(t, 25.0, 27.0, 29.0, 31.0);
        this.twist = -Math.PI * 0.5 + corkscrew(t, 0.5);
        unfurl(t, 0.2, 0.15, 0.3, 0.6);
        set(t, 6.5, 2.0, new double[] { 0.0, 1.0, 1.0, 1.0 }, new double[] { 0.05, 0.3, 0.3, 0.3 }, 0.1, 0.5,
                0.05);
        this.curl[4] += 0.4 * hammer;
        breathe(t, 0.4 * window(t, 8.0, 10.0, 30.0, 32.0));
        this.sink(t, SINK[FINGERGUN], LIFE[FINGERGUN], 1.0, true);
    }

    public static Vec3 gunAim() {
        return GUN_TIP.add(0.0, GUN_AWAY, 0.0);
    }

    // Level out of a portal, two fingers out like scissors: it opens them wide, thrusts in and snips, twice.
    void scissors(double t) {
        double open = 4.0;
        double thrust = 0.0;
        for (double snip : SNIP_AT) {
            double shut = Ease.smoother((t - snip + 0.35) / 0.35) * (1.0 - Ease.smoother((t - snip - 1.0) / 2.5));
            open = Mth.lerp(shut, open, -1.5);
            thrust += 0.8 * Ease.bump((t - snip) / 2.0);
        }
        this.lean = Math.PI * 0.5;
        this.length = rise(t, 1.0, 1.1, 0.6) + thrust;
        this.twist = corkscrew(t, 0.5);
        unfurl(t, 0.2, 0.15, 0.3, 0.6);
        set(t, 6.5, 2.0, new double[] { 0.0, 0.0, 1.0, 1.0 }, new double[] { 0.0, 0.0, 0.3, 0.3 }, 0.9, 0.0, 0.5);
        this.spread = Mth.lerp(Ease.smoother((t - 9.0) / 2.0), this.spread, open);
        cascade(t, 26.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - 26.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.4 * window(t, 8.0, 10.0, 26.0, 29.0));
        this.sink(t, SINK[SCISSORS], LIFE[SCISSORS], 1.0, true);
    }

    // Up out of a portal in the ground like the grab; once it has hold it pulls the creature straight down in.
    void swallow(double t) {
        double caught = GRAB_CATCHES / SLOW;
        this.grab(Math.min(t, caught + 0.5));
        double pull = Ease.smoother((t - caught - 0.5) / (SWALLOW_GONE / SLOW - caught - 0.5));
        this.length = Mth.lerp(pull, this.length, BURIED + 1.0);
        this.lean = Mth.lerp(pull, this.lean, 0.0);
    }

    // Up out of the ground or a wall as a fist bent over to point its knuckles and ring at the creature: the ring
    // gathers light, trembling, fires a beam, and the fist lifts off after.
    void ringBeam(double t) {
        double firing = window(t, FIRE_AT - 0.3, FIRE_AT, STOP_AT, STOP_AT + 2.0);
        double charging = window(t, RINGBEAM_CHARGE, RINGBEAM_CHARGE + 3.0, FIRE_AT, FIRE_AT + 0.5);
        this.length = rise(t, 4.0, 1.0, 0.6);
        // Raised high and bent down at the wrist, so the knuckles point a little down at the creature ten blocks off.
        this.lean = Ease.keys(t, 6.0, 0.1, 0.0, RINGBEAM_CHARGE + 2.0, 0.8, 0.0, STOP_AT + 1.0, 0.8, 0.0,
                STOP_AT + 5.0, 0.3, 0.0) - 0.3 * Ease.recoil(t - FIRE_AT, 0.8, FEAT_KICK, FEAT_CALM);
        this.flex = 1.0 * Ease.smoother((t - RINGBEAM_CHARGE) / 3.0) * (1.0 - Ease.smoother((t - STOP_AT) / 4.0))
                + 0.02 * (charging + firing) * Math.sin(t * 9.0);
        this.twist = corkscrew(t, 0.5) + 0.015 * firing * Math.sin(t * 13.0);
        unfurl(t, 0.3, 0.3, 0.4, 0.5);
        set(t, 6.0, 2.5, new double[] { 1.0, 1.0, 1.0, 1.0 }, new double[] { 0.1, 0.1, 0.1, 0.1 }, 0.9, 0.0, 0.0);
        cascade(t, STOP_AT + 3.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - STOP_AT - 3.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.5 * window(t, 8.0, 10.0, 12.0, 14.0));
        this.sink(t, SINK[RINGBEAM], LIFE[RINGBEAM], 1.0, true);
    }

    // Low out of the ground or a wall on the far side of the creature, palm up, slid under it; then it swings up and
    // back, tossing the creature high over itself.
    void scoop(double t) {
        double slide = Ease.smoother((t - 9.5) / 4.0);
        double toss = Ease.smoother((t - SCOOP_AT + 0.6) / 1.2);
        this.length = rise(t, 1.2, 1.0, 0.6) + 1.2 * slide * (1.0 - toss);
        this.lean = Mth.lerp(slide, Mth.lerp(Ease.smoother((t - 4.0) / 5.0), 0.0, 1.05), 1.2);
        this.lean = Mth.lerp(toss, this.lean, -0.4) + Ease.recoil(t - SCOOP_AT - 0.6, -0.6, FEAT_KICK, FEAT_CALM);
        this.lean = Mth.lerp(Ease.smoother((t - SCOOP_AT - 3.0) / 4.0), this.lean, 0.2);
        this.flex = -0.3 * toss * (1.0 - Ease.smoother((t - SCOOP_AT - 2.0) / 3.0));
        this.twist = Math.PI + corkscrew(t, 0.5);
        unfurl(t, 0.25, 0.3, 0.4, 0.2);
        cascade(t, SCOOP_AT + 3.0, 3.0, false, 0.35, 0.25, 0.45);
        breathe(t, 0.5 * window(t, 8.0, 10.0, 12.0, 14.0));
        this.sink(t, SINK[SCOOP], LIFE[SCOOP], 1.0, true);
    }
}
