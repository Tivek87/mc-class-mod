package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The hands that come through a portal instead of out of the ground (flick, pinch) and the finger snap.
abstract class HandTricks extends HandMoves {
    private static final double FLICK_BEAT = 21.0;
    private static final double PINCH_BEAT = 15.0;
    private static final double DROP_BEAT = 33.0;
    private static final double SNAP_BEAT = 20.0;
    public static final int FLICK_HITS = ticks(FLICK_BEAT);
    public static final int PINCH_CATCHES = ticks(PINCH_BEAT);
    public static final int PINCH_DROPS = ticks(DROP_BEAT);
    public static final int SNAP_HITS = ticks(SNAP_BEAT);
    private static final double[] POKE_BEATS = { 16.0, 21.0, 27.5 };
    private static final double HAMMER_BEAT = 20.0;
    private static final double RAKE_BEAT = 20.0;
    public static final int[] POKE_HITS = { ticks(POKE_BEATS[0]), ticks(POKE_BEATS[1]), ticks(POKE_BEATS[2]) };
    public static final int HAMMER_HITS = ticks(HAMMER_BEAT);
    public static final int HAMMER_LIFTS = ticks(29.0);
    public static final int RAKE_HITS = ticks(RAKE_BEAT);
    static final double[] POKE_AT = { POKE_HITS[0] / SLOW, POKE_HITS[1] / SLOW, POKE_HITS[2] / SLOW };
    static final double HAMMER_AT = HAMMER_HITS / SLOW;
    static final double RAKE_AT = RAKE_HITS / SLOW;
    public static final int DRAG_CATCHES = ticks(15.0);
    public static final int DRAG_STARTS = DRAG_CATCHES + 3;
    public static final int DRAG_RELEASES = ticks(33.0);
    static final double DRAG_AT = DRAG_CATCHES / SLOW;
    static final double RELEASE_AT = DRAG_RELEASES / SLOW;
    // How far the portal flies off with the creature in its grip, in blocks.
    public static final double DRAG_DISTANCE = 24.0;
    private static final double DRAG_LENGTH = 1.5;
    // Back from the rounded tick to a beat, so the animation lands on the exact tick the server acts.
    static final double FLICK_AT = FLICK_HITS / SLOW;
    static final double PINCH_AT = PINCH_CATCHES / SLOW;
    static final double DROP_AT = PINCH_DROPS / SLOW;
    static final double SNAP_AT = SNAP_HITS / SLOW;
    // Where each does its work, in the hand's own frame: the flicked nail, the pinched fingertips, the snapped ones.
    public static final Vec3 FLICK_POINT = new Vec3(-0.38, 5.2, 1.9);
    public static final Vec3 PINCH_GRIP = new Vec3(-1.12, 3.32, 2.07);
    public static final Vec3 SNAP_POINT = new Vec3(-0.38, 2.84, 2.14);
    public static final Vec3 POKE_POINT = new Vec3(-1.12, 6.2, 0.0);
    // The bottom of the fist: where the hammer lands on the creature's feet.
    public static final Vec3 HAMMER_POINT = new Vec3(0.0, 3.5, 0.5);
    public static final Vec3 RAKE_POINT = new Vec3(0.0, 5.0, 1.6);
    private static final double[] POKE_REACH = { 1.4, 2.2, 3.2 };
    private static final double[] POKE_BACK = { 0.3, 0.4, 1.1 };
    private static final double POKE_LENGTH = 0.8;
    private static final double HAMMER_HIGH = 1.0;
    private static final double HAMMER_LOW = 5.0;
    private static final double RAKE_LENGTH = 2.2;
    private static final double[] POKE_CURL = { 0.0, 1.0, 1.0, 1.0 };
    private static final double[] POKE_HOOK = { 0.05, 0.3, 0.3, 0.3 };
    private static final double[] FIST_CURL = { 1.0, 1.0, 1.0, 1.0 };
    private static final double[] FIST_HOOK = { 0.1, 0.1, 0.1, 0.1 };
    private static final double[] CLAW_CURL = { 0.35, 0.35, 0.35, 0.35 };
    private static final double[] CLAW_HOOK = { 0.9, 0.9, 0.9, 0.9 };
    public static final double PINCH_LIFT = 9.0;
    private static final double FLICK_LENGTH = 1.2;
    private static final double PINCH_LENGTH = 7.5;
    private static final double SNAP_LENGTH = 2.0;
    private static final double KICK = 7.5;
    private static final double KICK_CALM = 0.32;
    private static final double PORTAL_RADIUS = 2.6;
    private static final double PORTAL_OPENS = 3.0;
    // Middle finger and thumb curls found by search so their tips press together.
    private static final double[] FLICK_CURL = { 0.35, 0.7, 0.45, 0.55 };
    private static final double[] FLICK_HOOK = { 0.2, 0.0, 0.2, 0.2 };
    private static final double[] PINCH_CURL = { 0.12, 0.85, 0.9, 0.9 };
    private static final double[] PINCH_HOOK = { 0.1, 0.3, 0.3, 0.3 };
    private static final double[] SNAP_CURL = { 0.35, 0.6, 0.88, 0.92 };
    private static final double[] SNAP_HOOK = { 0.15, 0.1, 0.3, 0.3 };

    public static boolean portal(int variant) {
        int move = HandPose.move(variant);
        return move == FLICK || move == PINCH || move == POKE || move == HAMMER || move == DRAG;
    }

    // How far the drag's portal has flown off by this tick: slow to start, then racing, easing out at the release.
    public static double dragged(double t) {
        return DRAG_DISTANCE * Ease.smoother((t - DRAG_STARTS) / (DRAG_RELEASES - DRAG_STARTS));
    }

    // A portal overhead reaches down; the others reach level.
    public static boolean overhead(int variant) {
        int move = HandPose.move(variant);
        return move == PINCH || move == HAMMER;
    }

    // The tick a portal hand first acts: its portal stops following the creature shortly before.
    public static int firstAct(int variant) {
        return switch (HandPose.move(variant)) {
            case PINCH -> PINCH_CATCHES;
            case POKE -> POKE_HITS[0];
            case HAMMER -> HAMMER_HITS;
            case DRAG -> DRAG_CATCHES;
            default -> FLICK_HITS;
        };
    }

    // What a hand holds sits here in its frame: between the pinching fingertips, or in the fist.
    public static Vec3 grip(int variant) {
        return HandPose.move(variant) == PINCH ? PINCH_GRIP : GRIP;
    }

    // The portal a hand comes through: level before a flick, overhead for a pinch; it opens as the hand is called
    // and shuts once the hand has gone back in.
    public static HandDuo.Portal portalOf(int variant, Vec3 base, Vec3 facing, double t, double scale) {
        Vec3 flat = new Vec3(facing.x, 0.0, facing.z);
        flat = flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 normal = overhead(variant) ? Vectors.UP.scale(-1.0) : flat;
        Vec3 a = overhead(variant) ? flat : Vectors.UP;
        double beat = t / SLOW;
        double life = LIFE[HandPose.move(variant)];
        double open = Ease.smoother((beat - 0.5) / (PORTAL_OPENS - 0.5))
                * (1.0 - Ease.smoother((beat - (life - 4.0)) / 3.5));
        return new HandDuo.Portal(base, normal, a, normal.cross(a), PORTAL_RADIUS * scale, open);
    }

    // Where a portal hand does its work at its moment, from the portal's middle: the creature's middle goes there, or
    // for the hammer its feet.
    public static Vec3 workOffset(int variant, Vec3 facing, double scale) {
        int move = HandPose.move(variant);
        double beat = switch (move) {
            case PINCH -> PINCH_AT;
            case POKE -> POKE_AT[0];
            case HAMMER -> HAMMER_AT;
            case DRAG -> DRAG_AT;
            default -> FLICK_AT - 1.0;
        };
        Vec3 point = switch (move) {
            case PINCH -> PINCH_GRIP;
            case DRAG -> GRIP;
            case POKE -> POKE_POINT;
            case HAMMER -> HAMMER_POINT;
            default -> FLICK_POINT;
        };
        return HandPose.at(variant, beat * SLOW, 0.0).place(Vec3.ZERO, facing, scale).at(point);
    }

    // Level out of its portal, palm down, the middle finger cocked against the thumb: a flick like a bug off a table.
    void flick(double t) {
        double since = t - FLICK_AT;
        double aiming = window(t, 11.0, 16.0, FLICK_AT - 0.3, FLICK_AT + 1.0);
        this.lean = Math.PI * 0.5;
        this.length = rise(t, FLICK_LENGTH, 1.1, 0.6) + 0.45 * aiming + Ease.recoil(since, -2.2, KICK, KICK_CALM);
        this.flex = -0.25 * window(t, 12.0, 16.0, FLICK_AT - 0.2, FLICK_AT + 0.3)
                + Ease.recoil(since, 2.0, KICK, KICK_CALM);
        this.twist = corkscrew(t, 0.5);
        unfurl(t, 0.25, 0.2, 0.3, 0.7);
        set(t, 8.0, 2.5, FLICK_CURL, FLICK_HOOK, 0.45, -0.5, 0.25);
        double strain = window(t, 12.0, 15.0, FLICK_AT - 0.3, FLICK_AT);
        this.curl[1] += strain * 0.02 * Math.sin(t * 11.0);
        this.curl[4] += strain * 0.02 * Math.sin(t * 11.0 + 1.3);
        double out = Ease.smoother((t - FLICK_AT + 0.35) / 0.7);
        this.curl[1] = Mth.lerp(out, this.curl[1], -0.08);
        this.curl[4] = Mth.lerp(out, this.curl[4], 0.3);
        this.thumbOut = Mth.lerp(out, this.thumbOut, 0.0);
        cascade(t, 29.0, 3.0, false, 0.3, 0.2, 0.4);
        this.spread = Mth.lerp(Ease.smoother((t - 29.0) / 3.0), this.spread, 0.5);
        fidget(t, window(t, 22.0, 25.0, 29.0, 32.0), true);
        breathe(t, 0.6 * window(t, 9.0, 12.0, 29.0, 33.0));
        this.sink(t, SINK[FLICK], LIFE[FLICK], 1.0, true);
    }

    // Straight down out of a portal overhead, thumb and finger wide; they pinch the creature, lift it high, dangling,
    // and let go.
    void pinch(double t) {
        double held = window(t, PINCH_AT + 2.0, PINCH_AT + 5.0, DROP_AT - 1.0, DROP_AT + 1.0);
        this.lean = Math.PI + 0.05 * held * Math.sin(t * 1.3 + 0.5);
        this.length = rise(t, PINCH_LENGTH, 1.15, 0.55) + 0.4 * Ease.bump((t - PINCH_AT) / 2.0)
                + Ease.keys(t, PINCH_AT + 0.5, 0.0, 0.0, PINCH_AT + 5.0, -2.2, -0.6, 28.0, -PINCH_LIFT + 0.6, -0.15,
                        30.0, -PINCH_LIFT, 0.0)
                + 0.3 * Ease.recoil(t - DROP_AT, 1.0, KICK, KICK_CALM);
        this.twist = corkscrew(t, -0.5) + 0.08 * held * Math.sin(t * 0.9);
        unfurl(t, 0.3, 0.2, 0.3, 0.6);
        set(t, 7.0, 2.5, PINCH_CURL, PINCH_HOOK, 0.2, 0.35, 0.15);
        // Found by search: these bring the tips of thumb and index finger together at PINCH_GRIP.
        double shut = Ease.smoother((t - PINCH_AT + 0.8) / 1.0) * (1.0 - Ease.smoother((t - DROP_AT + 0.2) / 0.8));
        this.curl[0] = Mth.lerp(shut, this.curl[0], 0.45);
        this.hook[0] = Mth.lerp(shut, this.hook[0], 0.2);
        this.curl[4] = Mth.lerp(shut, this.curl[4], 0.2);
        this.thumbOut = Mth.lerp(shut, this.thumbOut, -0.6);
        cascade(t, DROP_AT + 4.0, 3.0, false, 0.35, 0.25, 0.45);
        this.thumbOut = Mth.lerp(Ease.smoother((t - DROP_AT - 4.0) / 3.0), this.thumbOut, 0.0);
        fidget(t, 0.5 * window(t, DROP_AT + 1.0, DROP_AT + 3.0, DROP_AT + 8.0, DROP_AT + 11.0), true);
        breathe(t, 0.5 * window(t, 8.0, 11.0, PINCH_AT - 1.0, PINCH_AT));
        this.sink(t, SINK[PINCH], LIFE[PINCH], 1.0, true);
    }

    // Up out of the ground beside the creature, palm turned aside, thumb pressed to the middle finger; the snap sends
    // a ring of light out that leaves everything round it dazed.
    void snap(double t, double side) {
        double since = t - SNAP_AT;
        double ready = window(t, 11.0, 15.0, SNAP_AT, SNAP_AT + 2.0);
        this.length = rise(t, SNAP_LENGTH, 1.0, 0.6) + 0.3 * ready;
        this.lean = 0.3 - 0.12 * ready + 0.6 * Ease.recoil(since, 0.4, KICK, KICK_CALM);
        this.twist = side * Math.PI * 0.5 + side * corkscrew(t, 0.6);
        this.flex = -0.3 * window(t, 12.0, 16.0, SNAP_AT - 0.2, SNAP_AT + 0.4)
                + Ease.recoil(since, 3.0, KICK, KICK_CALM);
        unfurl(t, 0.2, 0.15, 0.3, 0.7);
        set(t, 8.5, 2.5, SNAP_CURL, SNAP_HOOK, 0.4, -0.5, 0.1);
        double strain = window(t, 13.0, 16.0, SNAP_AT - 0.2, SNAP_AT);
        this.curl[1] += strain * 0.02 * Math.sin(t * 12.0);
        this.curl[4] += strain * 0.02 * Math.sin(t * 12.0 + 0.9);
        double snapped = Ease.smoother((t - SNAP_AT + 0.2) / 0.45);
        this.curl[1] = Mth.lerp(snapped, this.curl[1], 1.08);
        this.curl[0] = Mth.lerp(snapped, this.curl[0], 0.25);
        this.curl[4] = Mth.lerp(snapped, this.curl[4], 0.2);
        this.thumbOut = Mth.lerp(snapped, this.thumbOut, 0.45);
        this.thumbOut = Mth.lerp(Ease.smoother((t - 27.0) / 3.0), this.thumbOut, 0.0);
        cascade(t, 27.0, 3.0, false, 0.35, 0.25, 0.45);
        fidget(t, window(t, 21.0, 24.0, 28.0, 31.0), false);
        breathe(t, window(t, 9.0, 12.0, 30.0, 34.0));
        this.sink(t, SINK[SNAP], LIFE[SNAP], side, true);
    }

    // Level out of its portal, palm turned aside, one finger out: it pokes the creature, pokes again a little further,
    // draws right back and gives it one hard last poke that sends it flying.
    void poke(double t) {
        double jabs = 0.0;
        double kick = 0.0;
        for (int k = 0; k < POKE_AT.length; k++) {
            double hit = POKE_AT[k];
            double back = POKE_BACK[k] * window(t, hit - 3.0, hit - 1.4, hit - 0.7, hit - 0.35);
            double out = Ease.smoother((t - hit + 0.5) / 0.5) - Ease.smoother((t - hit - 0.9) / 1.8);
            jabs += POKE_REACH[k] * out - back;
            kick += Ease.recoil(t - hit, k == POKE_AT.length - 1 ? 2.0 : 1.0, KICK, KICK_CALM);
        }
        this.lean = Math.PI * 0.5;
        this.length = rise(t, POKE_LENGTH, 1.1, 0.6) + jabs;
        this.flex = -0.12 * window(t, 24.0, 26.0, POKE_AT[2] - 0.3, POKE_AT[2]) + 0.4 * kick;
        // Turned so the pointing finger is the lowest: the portal then stays clear of the ground.
        this.twist = -Math.PI * 0.5 + corkscrew(t, 0.5);
        unfurl(t, 0.2, 0.15, 0.3, 0.6);
        set(t, 7.0, 2.5, POKE_CURL, POKE_HOOK, 0.85, 0.0, 0.05);
        cascade(t, 33.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - 33.0) / 3.0), this.spread, 0.5);
        fidget(t, window(t, 30.0, 32.0, 35.0, 38.0), true);
        breathe(t, 0.5 * window(t, 8.0, 11.0, 30.0, 34.0));
        this.sink(t, SINK[POKE], LIFE[POKE], 1.0, true);
    }

    // Straight down out of a portal overhead as a fist: it draws up, trembling, then hammers down on the creature,
    // grinds it into the ground and lifts off again.
    void hammer(double t) {
        double since = t - HAMMER_AT;
        double wind = window(t, 11.0, 16.0, HAMMER_AT - 0.6, HAMMER_AT - 0.3);
        double down = Ease.smoother((t - HAMMER_AT + 0.6) / 0.6) * (1.0 - Ease.smoother((t - 29.0) / 4.0));
        double grind = window(t, HAMMER_AT + 1.0, HAMMER_AT + 2.5, 27.0, 29.0);
        this.lean = Math.PI;
        this.length = rise(t, HAMMER_HIGH, 1.1, 0.6) - 1.2 * wind + (HAMMER_LOW - HAMMER_HIGH) * down
                + Ease.recoil(since, -2.0, KICK, KICK_CALM) + 0.05 * wind * Math.sin(t * 9.0);
        this.flex = -0.3 * wind + 0.5 * Ease.recoil(since, 1.5, KICK, KICK_CALM);
        this.twist = corkscrew(t, 0.5) + 0.06 * grind * Math.sin(t * 2.6);
        unfurl(t, 0.4, 0.3, 0.5, 0.5);
        set(t, 6.5, 2.5, FIST_CURL, FIST_HOOK, 0.9, 0.0, 0.0);
        cascade(t, 30.0, 3.0, false, 0.4, 0.3, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - 30.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.6 * window(t, 8.0, 11.0, 15.0, 17.0));
        this.sink(t, SINK[HAMMER], LIFE[HAMMER], 1.0, true);
    }

    // Up out of the ground before the creature with its fingers hooked into claws: it rears back, then rakes them down
    // through the creature and along the ground, clenching as it drags.
    void rake(double t) {
        double raise = window(t, 10.0, 15.0, RAKE_AT - 1.2, RAKE_AT - 0.8);
        this.length = rise(t, RAKE_LENGTH, 1.0, 0.6) + 0.8 * raise;
        this.lean = Ease.keys(t, 8.0, 0.15, 0.0, 15.0, -0.25, 0.0, RAKE_AT - 1.0, -0.3, 0.0, RAKE_AT + 1.0, 1.15,
                0.3, RAKE_AT + 3.5, 1.3, 0.0, RAKE_AT + 10.0, 0.35, 0.0);
        this.flex = -0.35 * raise + Ease.keys(t, RAKE_AT - 1.0, 0.0, 0.0, RAKE_AT + 1.0, 0.45, 0.0, RAKE_AT + 8.0,
                0.15, 0.0);
        this.twist = corkscrew(t, 0.5);
        unfurl(t, 0.2, 0.2, 0.3, 0.8);
        set(t, 7.5, 2.5, CLAW_CURL, CLAW_HOOK, 0.4, 0.2, 0.9);
        double clench = Ease.smoother((t - RAKE_AT) / 1.5) * (1.0 - Ease.smoother((t - 30.0) / 3.0));
        for (int k = 0; k < 4; k++) {
            this.curl[k] += 0.2 * clench;
        }
        cascade(t, 31.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - 31.0) / 3.0), this.spread, 0.5);
        fidget(t, window(t, 8.0, 10.0, 14.0, 16.0), true);
        breathe(t, window(t, 8.0, 11.0, 15.0, 17.0) + 0.5 * window(t, 26.0, 28.0, 33.0, 36.0));
        this.sink(t, SINK[RAKE], LIFE[RAKE], 1.0, true);
    }

    // Level out of a portal on the far side of the creature, palm down, fingers spread: it snatches the creature, and
    // as its portal races off the arm is pulled taut and the creature scraped along the ground behind it; then the
    // fingers fly open and fling it on.
    void drag(double t) {
        double pull = window(t, DRAG_AT + 1.0, DRAG_AT + 4.0, RELEASE_AT - 1.0, RELEASE_AT + 0.5);
        this.lean = Math.PI * 0.5 + 0.2 * pull;
        this.length = rise(t, DRAG_LENGTH, 1.1, 0.6) + 0.5 * Ease.bump((t - DRAG_AT) / 2.0) + 0.9 * pull;
        this.flex = 0.2 * pull + 0.04 * pull * Math.sin(t * 5.3) + Ease.recoil(t - RELEASE_AT, 1.5, KICK, KICK_CALM);
        this.twist = corkscrew(t, 0.5) + 0.05 * pull * Math.sin(t * 3.7);
        unfurl(t, 0.08, 0.3, 0.1, 1.0);
        cascade(t, DRAG_AT - 2.4, 1.4, false, 0.0, 0.12, 0.05);
        this.spread = Mth.lerp(Ease.smoother((t - DRAG_AT + 2.4) / 1.4), this.spread, 1.06);
        cascade(t, DRAG_AT - 1.0, 1.2, true, 0.72, 0.22, 0.72);
        this.spread = Mth.lerp(Ease.smoother((t - DRAG_AT + 1.0) / 2.0), this.spread, 0.05);
        cascade(t, RELEASE_AT - 0.4, 1.0, false, 0.1, 0.1, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - RELEASE_AT + 0.4) / 1.2), this.spread, 0.9);
        cascade(t, RELEASE_AT + 3.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - RELEASE_AT - 3.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.5 * window(t, 8.0, 10.0, DRAG_AT - 1.0, DRAG_AT));
        this.sink(t, SINK[DRAG], LIFE[DRAG], 1.0, true);
    }

    private void set(double t, double from, double beats, double[] curls, double[] hooks, double thumb,
            double thumbOut, double spread) {
        double u = Ease.smoother((t - from) / beats);
        for (int k = 0; k < 4; k++) {
            this.curl[k] = Mth.lerp(u, this.curl[k], curls[k]);
            this.hook[k] = Mth.lerp(u, this.hook[k], hooks[k]);
        }
        this.curl[4] = Mth.lerp(u, this.curl[4], thumb);
        this.hook[4] = Mth.lerp(u, this.hook[4], 0.3);
        this.thumbOut = Mth.lerp(u, this.thumbOut, thumbOut);
        this.spread = Mth.lerp(u, this.spread, spread);
    }
}
