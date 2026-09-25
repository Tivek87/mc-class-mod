package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;

abstract class HandMoves extends HandMotion {
    public static final int SMACK = 0;
    public static final int GRAB = 1;
    public static final int FINGER = 2;
    public static final int SLAM = 3;
    public static final int POUND = 4;
    public static final int AXE = 5;
    public static final int MOVES = 6;
    static final double SLOW = 1.6;
    private static final double ARRIVE = 4.0;
    static final int[] LIFE = { 50, 58, 56, 62, 76 };
    static final int[] SINK = { 36, 44, 42, 47, 62 };
    private static final double SMACK_BEAT = 19.0;
    static final double CATCH_BEAT = 13.0;
    private static final double THROW_BEAT = 33.0;
    private static final double SLAM_BEAT = 25.0;
    private static final double PRESS_BEAT = 40.0;
    static final double[] POUND_BEATS = { 24.0, 38.0, 52.0 };
    public static final int ARRIVES = ticks(ARRIVE);
    public static final int SMACK_HITS = ticks(SMACK_BEAT);
    public static final int GRAB_CATCHES = ticks(CATCH_BEAT);
    public static final int GRAB_THROWS = ticks(THROW_BEAT);
    public static final int SLAM_HITS = ticks(SLAM_BEAT);
    public static final int SLAM_PRESSES = ticks(PRESS_BEAT);
    public static final int[] POUND_HITS = { ticks(POUND_BEATS[0]), ticks(POUND_BEATS[1]), ticks(POUND_BEATS[2]) };
    public static final int FINGER_BURSTS = ticks(4.6);
    public static final int FINGER_UP = ticks(12.0);
    public static final double SWING_TICKS = 4.0 * SLOW;
    // Back from the rounded tick to a beat, so the animation lands on the exact tick the server strikes.
    private static final double SMACK_AT = SMACK_HITS / SLOW;
    private static final double CATCH_AT = GRAB_CATCHES / SLOW;
    private static final double THROW_AT = GRAB_THROWS / SLOW;
    private static final double SLAM_AT = SLAM_HITS / SLOW;
    private static final double BURST_AT = FINGER_BURSTS / SLOW;
    private static final double UP_AT = FINGER_UP / SLOW;
    private static final double[] POUND_AT = { POUND_HITS[0] / SLOW, POUND_HITS[1] / SLOW, POUND_HITS[2] / SLOW };
    private static final double SMACK_LEAN = 1.26;
    private static final double SMACK_STRUCK = -0.2624;
    private static final double SMACK_STRUCK_FLEX = -0.0619;
    private static final double SMACK_TRAILS = 0.1246;
    private static final double THROW_TRAILS = 0.1597;
    private static final double[] SMACK_SWEEP = { 8.0, -0.7, 0.0, 15.0, -0.95, 0.0, 16.6, -0.82, 0.2, SMACK_AT,
            SMACK_STRUCK, 0.64, 20.2, 0.58, 0.28, 21.3, 0.72, 0.0, 23.2, 0.56, 0.0, 25.4, 0.62, 0.0, 27.5, 0.6, 0.0 };
    private static final double[] SMACK_FLEX = { 9.0, 0.0, 0.0, 15.0, -0.34, 0.0, 16.8, -0.3, 0.05, SMACK_AT,
            SMACK_STRUCK_FLEX + SMACK_TRAILS, 0.3, 20.6, 0.48, 0.0, 22.8, 0.34, 0.0, 25.0, 0.4, 0.0, 28.0, 0.3, 0.0,
            34.0, 0.1, 0.0 };
    private static final double[] SMACK_REACH = { 11.0, 0.0, 0.0, 15.2, -0.25, 0.0, SMACK_AT, 0.0, 0.2, 20.6, 0.25, 0.0,
            23.5, 0.0, 0.0 };
    private static final double[] SMACK_DIP = { 11.0, 0.0, 0.0, 15.2, -0.08, 0.0, SMACK_AT, 0.0, 0.05, 20.6, 0.06, 0.0,
            23.5, -0.01, 0.0, 26.0, 0.0, 0.0 };
    private static final double GRAB_HELD = -1.5;
    private static final double THROW_LIFT = 4.0;
    private static final double THROW_LEAN = 0.3632;
    private static final double THROW_FLEX = 0.2011;
    private static final double[] GRAB_LIFT = { CATCH_AT, 0.0, 0.0, 14.4, 0.25, 0.55, 16.6, 1.6, 0.5, 21.0, 3.0, 0.18,
            27.5, 3.85, 0.06, 30.6, 4.12, 0.0, 31.4, 4.1, -0.02, THROW_AT, THROW_LIFT, -0.12, 35.0, 4.28, 0.0, 37.5,
            4.0, 0.0 };
    private static final double[] GRAB_LEAN = { 13.4, 0.0, 0.0, 21.0, -0.2, -0.03, 27.5, -0.33, -0.025, 30.6, -0.46,
            0.0, 31.3, -0.44, 0.08, THROW_AT, THROW_LEAN, 0.62, 34.7, 0.8, 0.1, 35.4, 0.82, 0.0, 37.6, 0.54, 0.0, 39.8,
            0.6, 0.0, 42.0, 0.57, 0.0 };
    private static final double[] GRAB_FLEX = { 13.4, 0.0, 0.0, 21.0, -0.12, 0.0, 27.5, -0.2, 0.0, 30.6, -0.36, 0.0,
            31.3, -0.38, 0.0, THROW_AT, THROW_FLEX + THROW_TRAILS, 0.55, 34.8, 0.62, 0.0, 36.8, 0.4, 0.0, 39.0, 0.47,
            0.0, 42.0, 0.45, 0.0 };
    private static final double BURST_LENGTH = -3.294;
    private static final double FINGER_STANDS = 2.4;
    private static final double[] FINGER_RISE = { 3.55, BURIED, 0.0, BURST_AT, BURST_LENGTH, 9.0, 5.95, 4.35, 0.0, 7.95,
            1.85, 0.0, 9.9, 2.62, 0.0, UP_AT, FINGER_STANDS, 0.0 };
    private static final double[] FINGER_TILT = { 3.55, -0.12, 0.0, BURST_AT, 0.0, 0.3, 6.0, 0.24, 0.0, 8.0, -0.1, 0.0,
            9.9, 0.05, 0.0, UP_AT, 0.0, 0.0, 13.4, 0.0, 0.0, 17.6, 0.26, 0.0, 23.2, -0.14, 0.0, 29.4, 0.24, 0.0, 36.0,
            0.1, 0.0, 41.0, 0.0, 0.0 };
    private static final double[] FINGER_COCK = { UP_AT, 0.0, 0.0, 13.6, 0.1, 0.0, 36.0, 0.1, 0.0, 41.0, 0.0, 0.0 };
    private static final double[] JAB = { -2.4, 0.0, 0.0, -1.1, -0.3, 0.0, -0.7, -0.28, 0.15, 0.0, 1.0, 0.0, 0.9, -0.25,
            0.0, 1.9, 0.08, 0.0, 3.0, 0.0, 0.0 };
    private static final double[] JAB_AT = { 15.2, 21.0, 27.2 };
    private static final double[] JAB_HARD = { 1.0, 0.85, 1.2 };
    private static final double FIST_FROM = 4.4;
    private static final double SLAM_HIGH = 4.0;
    private static final double SLAM_LANDS = 0.95;
    private static final double SLAM_FLAT = 1.57;
    private static final double SLAM_DROP_SPEED = -1.5;
    private static final double SLAM_BEND_SPEED = 0.85;
    private static final double[] SLAM_DROP = { 12.5, 0.0, 0.0, 17.6, 0.55, 0.0, 20.0, 0.62, 0.0, SLAM_AT,
            SLAM_LANDS - SLAM_HIGH, SLAM_DROP_SPEED };
    private static final double[] SLAM_PRESS = { SLAM_AT, SLAM_LANDS - SLAM_HIGH, 0.0, 27.0, -3.14, 0.0, 38.5, -3.1,
            0.0, 40.0, -3.16, 0.0, 43.6, -2.3, 0.3, 47.0, -2.05, 0.0 };
    private static final double[] SLAM_BEND = { 12.5, 0.0, 0.0, 17.6, -0.42, 0.0, 20.0, -0.45, 0.0, SLAM_AT, SLAM_FLAT,
            SLAM_BEND_SPEED };
    private static final double[] SLAM_UNBEND = { SLAM_AT, SLAM_FLAT, 0.0, 40.0, SLAM_FLAT, 0.0, 41.2, 1.6, 0.0, 44.4,
            1.28, -0.1, 47.0, 1.17, 0.0 };
    private static final double[] SLAM_LEAN = { 12.5, 0.0, 0.0, 17.6, -0.07, 0.0, 20.0, -0.08, 0.0, SLAM_AT, 0.0, 0.02,
            27.0, 0.015, 0.0, 40.0, 0.0, 0.0 };
    private static final double RECOIL = 7.5;
    private static final double RECOIL_CALM = 0.32;
    private static final double FIST_ALONG = 2.0;
    private static final double FIST_SIDE = 1.65;
    private static final double POUND_NEAR = 3.0;
    private static final double POUND_FAR = 9.0;
    private static final double POUND_LIFT = 8.6;
    private static final double POUND_LIFTING = 5.4;
    private static final double POUND_DOWN = 2.3;
    private static final double POUND_SNAP = 2.4;
    public static final Vec3 GRIP = new Vec3(0.0, 3.05, 1.25);
    public static final Vec3 PALM = new Vec3(0.0, 1.55, 0.55);
    public static final Vec3 FIST = new Vec3(FIST_SIDE, FIST_ALONG, 0.45);

    static int ticks(double beats) {
        return (int) Math.round(beats * SLOW);
    }

    static double[] blows(int move) {
        return switch (move) {
            case SMACK -> new double[] { SMACK_AT };
            case GRAB -> new double[] { CATCH_AT, THROW_AT };
            case FINGER -> new double[] { BURST_AT, UP_AT };
            case SLAM -> new double[] { SLAM_AT };
            default -> POUND_AT;
        };
    }

    static HandPose struck(int move, double side, double blow, double reach) {
        HandPose pose = new HandPose();
        switch (move) {
            case SMACK -> {
                pose.lean = SMACK_LEAN;
                pose.length = smackOut(reach);
                pose.twist = -side * Math.PI * 0.5;
                pose.flex = SMACK_STRUCK_FLEX;
                pose.sweep = side * SMACK_STRUCK;
            }
            case GRAB -> {
                boolean thrown = blow > CATCH_AT;
                pose.length = GRAB_HELD + (thrown ? THROW_LIFT : 0.0);
                pose.lean = thrown ? THROW_LEAN : 0.0;
                pose.flex = thrown ? THROW_FLEX : 0.0;
            }
            case FINGER -> {
                pose.length = blow > BURST_AT ? FINGER_STANDS : BURST_LENGTH;
                pose.twist = Math.PI;
            }
            case SLAM -> {
                pose.length = SLAM_LANDS;
                pose.flex = SLAM_FLAT;
            }
            default -> {
                double r = Mth.clamp(reach, POUND_NEAR, POUND_FAR);
                pose.lean = poundLean(r);
                pose.length = poundLength(r);
                pose.twist = Math.PI * 0.5;
            }
        }
        return pose;
    }

    void smack(double t, double side, double reach) {
        this.length = rise(t, smackOut(reach), 1.0, 0.6) + Ease.keys(t, SMACK_REACH);
        this.lean = SMACK_LEAN - 0.32 * (1.0 - Ease.spring(t - RISE, 1.1, 0.7)) + Ease.keys(t, SMACK_DIP);
        this.twist = -side * Math.PI * 0.5 + side * corkscrew(t, 0.6);
        this.sweep = side * Ease.keys(t, SMACK_SWEEP);
        this.flex = Ease.keys(t, SMACK_FLEX);
        unfurl(t, 0.1, 0.14, 0.15, 1.0);
        cascade(t, 13.2, 2.0, false, 0.04, 0.03, 0.1);
        this.spread = Mth.lerp(Ease.smoother((t - 13.2) / 2.6), this.spread, 0.72);
        cascade(t, 22.0, 3.0, true, 0.18, 0.22, 0.18);
        this.spread = Mth.lerp(Ease.smoother((t - 22.0) / 4.0), this.spread, 0.85);
        cascade(t, 28.0, 4.0, true, 0.26, 0.25, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - 28.0) / 6.0), this.spread, 0.5);
        fidget(t, window(t, 8.0, 10.5, 13.0, 15.0) + window(t, 23.5, 27.0, 33.5, 36.0), true);
        breathe(t, window(t, 8.5, 11.0, 12.5, 14.8) + window(t, 24.0, 28.0, 33.0, 36.0));
        this.sink(t, SINK[SMACK], LIFE[SMACK], side, true);
    }

    private static double smackOut(double reach) {
        return Mth.clamp(reach / Math.sin(SMACK_LEAN) - PALM.y, 2.0, 8.0);
    }

    void grab(double t) {
        this.length = rise(t, GRAB_HELD, 1.15, 0.55) + Ease.keys(t, GRAB_LIFT);
        this.lean = Ease.keys(t, GRAB_LEAN);
        this.flex = Ease.keys(t, GRAB_FLEX);
        double struggle = window(t, 13.4, 15.5, 28.5, 30.6);
        this.twist = corkscrew(t, 0.5) + struggle * 0.03 * Math.sin(t * 2.3);
        this.lean += struggle * 0.012 * Math.sin(t * 3.1 + 0.8);
        unfurl(t, 0.06, 0.3, 0.1, 1.0);
        cascade(t, 8.8, 1.4, false, 0.0, 0.12, 0.05);
        this.spread = Mth.lerp(Ease.smoother((t - 8.8) / 1.4), this.spread, 1.06);
        cascade(t, 10.2, 1.4, true, 0.72, 0.22, 0.72);
        this.spread = Mth.lerp(Ease.smoother((t - 10.2) / 2.8), this.spread, 0.05);
        double squeeze = 0.06 * Ease.bump((t - 13.9) / 1.2);
        for (int k = 0; k < 4; k++) {
            this.curl[k] += squeeze;
        }
        cascade(t, 32.5, 1.2, false, 0.08, 0.1, 0.12);
        this.spread = Mth.lerp(Ease.smoother((t - 32.5) / 1.5), this.spread, 0.95);
        cascade(t, 36.5, 3.0, false, 0.16, 0.22, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - 36.5) / 3.5), this.spread, 0.7);
        fidget(t, 0.5 * window(t, 15.0, 17.0, 28.0, 30.0) + window(t, 36.0, 39.0, 42.0, 44.0), true);
        breathe(t, 0.6 * window(t, 15.0, 18.0, 27.0, 29.5) + window(t, 37.0, 40.0, 42.0, 44.0));
        this.sink(t, SINK[GRAB], LIFE[GRAB], 1.0, true);
    }

    void finger(double t) {
        this.length = Ease.keys(t, FINGER_RISE);
        this.twist = Math.PI + Ease.keys(t, FINGER_TILT);
        this.flex = Ease.keys(t, FINGER_COCK);
        for (int k = 0; k < JAB_AT.length; k++) {
            double jab = JAB_HARD[k] * Ease.keys(t - JAB_AT[k], JAB);
            this.lean += 0.22 * jab;
            this.flex -= 0.42 * jab;
            this.length += 0.3 * jab;
        }
        for (int k = 0; k < 4; k++) {
            if (k == 1) {
                continue;
            }
            double shut = Ease.smoother((t - FIST_FROM - (3 - k) * STAGGER) / 1.2);
            this.curl[k] = Mth.lerp(shut, 0.3, 1.0);
            this.hook[k] = Mth.lerp(shut, 0.25, 0.0);
        }
        double tuck = Ease.smoother((t - FIST_FROM - 3.3 * STAGGER) / 1.3);
        this.curl[4] = Mth.lerp(tuck, 0.25, 0.95);
        this.hook[4] = Mth.lerp(tuck, 0.2, 0.05);
        this.spread = Mth.lerp(Ease.smoother((t - FIST_FROM + 0.2) / 2.2), 0.55, 0.0);
        fidget(t, 0.6 * window(t, 12.4, 14.0, 38.0, 41.0), false);
        breathe(t, 0.7 * window(t, 12.4, 14.0, 38.0, 41.0));
        this.sink(t, SINK[FINGER], LIFE[FINGER], -1.0, false);
    }

    void slam(double t) {
        boolean landed = t >= SLAM_AT;
        double since = t - SLAM_AT;
        this.length = rise(t, SLAM_HIGH, 0.95, 0.6) + (landed
                ? Ease.keys(t, SLAM_PRESS) + Ease.recoil(since, SLAM_DROP_SPEED, RECOIL, RECOIL_CALM)
                : Ease.keys(t, SLAM_DROP));
        this.flex = landed ? Ease.keys(t, SLAM_UNBEND) + Ease.recoil(since, SLAM_BEND_SPEED, RECOIL, RECOIL_CALM)
                : Ease.keys(t, SLAM_BEND);
        this.lean = Ease.keys(t, SLAM_LEAN);
        double grind = window(t, 26.0, 28.0, 37.5, 39.5);
        this.twist = corkscrew(t, -0.55) + grind * 0.018 * Math.sin(t * 2.6);
        this.length += grind * 0.02 * Math.sin(t * 3.7 + 0.5);
        unfurl(t, 0.08, 0.12, 0.1, 1.0);
        cascade(t, 15.0, 2.0, false, 0.0, 0.04, 0.05);
        this.spread = Mth.lerp(Ease.smoother((t - 15.0) / 2.5), this.spread, 1.05);
        cascade(t, 41.0, 2.4, true, 0.28, 0.3, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - 41.0) / 3.0), this.spread, 0.7);
        fidget(t, window(t, 9.0, 11.5, 14.0, 15.5), true);
        breathe(t, window(t, 9.5, 12.5, 14.0, 16.5) + 0.3 * window(t, 27.5, 30.0, 36.5, 39.0));
        this.sink(t, SINK[SLAM], LIFE[SLAM], -1.0, true);
    }

    void pound(double t, double reach) {
        double r = Mth.clamp(reach, POUND_NEAR, POUND_FAR);
        double downLean = poundLean(r);
        double downLength = poundLength(r);
        double upLean = downLean * 0.5;
        double upLength = downLength + 1.4;
        double restLean = downLean * 0.8;
        double restLength = downLength + 0.45;
        this.twist = Math.PI * 0.5 + corkscrew(t, 0.5);
        this.length = rise(t, 3.6, 0.95, 0.6);
        this.lean = 0.1;
        for (double hit : POUND_AT) {
            double up = Ease.smoother((t - hit + POUND_LIFT) / POUND_LIFTING);
            double cock = Ease.keys(t - hit, -4.2, 0.0, 0.0, -2.6, 1.0, 0.0, -0.4, 0.0, 0.0);
            this.lean = Mth.lerp(up, this.lean, upLean) - 0.06 * cock;
            this.length = Mth.lerp(up, this.length, upLength) + 0.3 * cock;
            double u = (t - hit + POUND_DOWN) / POUND_DOWN;
            double down = u <= 0.0 ? 0.0 : u >= 1.0 ? 1.0 : Ease.hermite(0.0, 0.0, 1.0, POUND_SNAP, u);
            double landing = POUND_SNAP / POUND_DOWN;
            this.lean = Mth.lerp(down, this.lean, downLean)
                    + Ease.recoil(t - hit, landing * (downLean - upLean), RECOIL, RECOIL_CALM);
            this.length = Mth.lerp(down, this.length, downLength)
                    + Ease.recoil(t - hit, landing * (downLength - upLength), RECOIL, RECOIL_CALM);
            double settle = Ease.smoother((t - hit - 0.7) / 3.2);
            this.lean = Mth.lerp(settle, this.lean, restLean);
            this.length = Mth.lerp(settle, this.length, restLength);
        }
        double last = POUND_AT[POUND_AT.length - 1];
        double back = Ease.smoother((t - last - 3.8) / (SINK[POUND] - last - 3.8));
        this.lean = Mth.lerp(back, this.lean, 0.3);
        this.length = Mth.lerp(back, this.length, 3.0);
        unfurl(t, 0.2, 0.22, 0.25, 0.6);
        cascade(t, 9.2, 1.5, true, 1.0, 0.0, 1.0);
        this.spread = Mth.lerp(Ease.smoother((t - 9.2) / 2.8), this.spread, 0.0);
        fidget(t, window(t, 11.0, 13.0, 14.0, 16.0) + window(t, 56.5, 58.5, 60.0, 62.0), true);
        breathe(t, window(t, 10.0, 12.5, 13.5, 15.5) + window(t, 56.5, 58.5, 60.0, 62.0));
        this.sink(t, SINK[POUND], LIFE[POUND], 1.0, true);
    }

    private static double poundLean(double r) {
        double h = 0.1;
        return Math.acos(Mth.clamp(FIST_SIDE / Math.sqrt(r * r + h * h), -1.0, 1.0)) - Math.atan2(h, r);
    }

    private static double poundLength(double r) {
        double lean = poundLean(r);
        return (r - FIST_SIDE * Math.cos(lean)) / Math.max(0.2, Math.sin(lean)) - FIST_ALONG;
    }
}
