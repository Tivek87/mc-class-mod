package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Vectors;

abstract class HandDuoScript extends HandDuoMotion {
    public static final int ARRIVES = HandPose.ARRIVES;
    public static final int OUT = ARRIVES + 14;
    public static final int SNAP = OUT + 26;
    public static final int OK = SNAP + 10;
    public static final int AXE_OPENS = OK + 42;
    public static final int GRAB = AXE_OPENS + 18;
    public static final int AXE_FREE = GRAB + 8;
    public static final int LOCKED_FROM = AXE_FREE + 4;
    public static final int RAISED = LOCKED_FROM + 12;
    public static final int IMPACT = RAISED + 8;
    public static final int RELEASE = IMPACT + 13;
    public static final int THUMBS = RELEASE + 12;
    public static final int RETRACT = THUMBS + 16;
    public static final int HANDS_GONE = RETRACT + 12;
    public static final int AXE_BREAKS = HANDS_GONE + 6;
    public static final int LIFE = AXE_BREAKS + 16;

    public static final double AXE_LENGTH = 14.0;
    public static final double HAFT_RADIUS = 0.42;
    public static final double GRIP_LOW = 2.4;
    public static final double GRIP_HIGH = 6.2;
    public static final double HEAD_AT = 11.4;
    public static final double BLADE_HALF = 2.3;
    public static final double EDGE_OUT = 3.2;
    public static final Vec3 FIST_HOLE = new Vec3(0.0, 2.95, 1.05);
    public static final Vec3 SNAP_AT = new Vec3(-0.63, 2.97, 2.35);
    public static final Vec3 OK_AT = new Vec3(1.35, 3.14, 1.06);
    public static final Vec3 THUMB_TIP = new Vec3(-4.14, 1.78, 1.17);
    public static final double REACH = 3.0;

    static final double GRIPS = (GRIP_LOW + GRIP_HIGH) * 0.5;
    private static final double LEVER = HEAD_AT - GRIPS;
    private static final double BITE = 0.6;
    static final double BREAK_TICKS = 14.0;

    static final Vec3 RIGHT_PORTAL = new Vec3(-7.8, 6.0, 4.5);
    static final double SIDE_RADIUS = 2.5;
    static final Vec3 AXE_PORTAL = new Vec3(0.0, 5.8, 12.9);
    static final double AXE_RADIUS = 3.9;
    static final double SLID = 11.2;
    static final double DRAWN = AXE_LENGTH + 0.6;
    static final double DRAWN_REST = DRAWN + 0.3;

    private static final Vec3 HOME = new Vec3(0.0, 7.0, 4.3);
    private static final double FOLLOW_SIDE = 0.35;
    private static final double FOLLOW_AWAY = 0.45;
    private static final double RAISE_Y = 7.1;
    private static final double RAISE_Z = 3.0;
    static final double RAISE_TILT = 0.66;
    static final double DIP = 0.85;
    static final double DIP_TILT = -0.15;
    static final double CHOP_FROM = RAISED + 1.5;
    static final double HANDS_DOWN = 0.75;
    static final double WRIST_SHARE = 0.5;
    static final double HOLD_CLEAR = 1.2;
    static final double DROP_OFF = 2.2;
    static final double SLIDE_OFF = 2.4;
    static final double LET_GO = 2.5;

    static final Vec3 HOVER = new Vec3(-4.5, 6.3, 1.4);
    private static final Vec3 HOVER_UP = new Vec3(0.22, -0.15, -1.0);
    private static final Vec3 HOVER_PALM = new Vec3(0.05, -1.0, 0.1);
    private static final Vec3 SNAPPING = new Vec3(-3.7, 8.3, 1.5);
    private static final Vec3 SNAPPING_UP = new Vec3(0.4, 1.0, -0.2);
    private static final Vec3 SNAPPING_PALM = new Vec3(0.25, 0.0, -1.0);
    private static final Vec3 WATCHING = new Vec3(-4.1, 7.0, 1.6);
    private static final Vec3 WATCHING_UP = new Vec3(0.3, 0.15, -1.0);
    private static final Vec3 WATCHING_PALM = new Vec3(0.15, -1.0, -0.1);
    private static final Vec3 ROLLING = new Vec3(-2.9, 6.9, 1.3);
    private static final Vec3 ROLLING_UP = new Vec3(0.0, 0.55, -1.0);
    private static final Vec3 ROLLING_PALM = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 CONJURING = new Vec3(-4.2, 7.9, 2.2);
    private static final Vec3 CONJURING_UP = new Vec3(0.3, 0.35, -1.0);
    private static final Vec3 CONJURING_PALM = new Vec3(0.05, 1.0, 0.35);
    private static final Vec3 REACHING = new Vec3(-4.0, 9.5, 7.0);
    private static final Vec3 REACHING_UP = new Vec3(1.0, 0.15, 0.1);
    private static final Vec3 REACHING_PALM = new Vec3(0.15, -1.0, 0.0);
    private static final Vec3 LEFT_REACHING = new Vec3(3.8, 7.8, 3.4);
    private static final Vec3 LEFT_REACHING_UP = new Vec3(-1.0, 0.2, -0.45);
    private static final Vec3 LEFT_REACHING_PALM = new Vec3(-0.15, -1.0, 0.0);
    private static final Vec3 GRABBING = new Vec3(-3.1, 7.9, 7.9);
    private static final Vec3 GRABBING_UP = new Vec3(1.0, 0.1, 0.0);
    private static final Vec3 GRABBING_PALM = new Vec3(0.1, -1.0, 0.0);
    private static final Vec3 LEFT_GRABBING = new Vec3(3.1, 7.9, 4.1);
    private static final Vec3 LETTING_GO = new Vec3(-5.6, 3.4, 3.5);
    private static final Vec3 LETTING_GO_UP = new Vec3(1.0, -0.08, 0.06);
    private static final Vec3 LETTING_GO_PALM = new Vec3(0.1, 0.8, -0.59);
    private static final Vec3 LEFT_LETTING_GO = new Vec3(5.6, 5.6, 6.6);
    private static final Vec3 LEFT_LETTING_GO_UP = new Vec3(-1.0, -0.08, 0.06);
    private static final Vec3 LEFT_LETTING_GO_PALM = new Vec3(-0.1, 0.8, -0.59);
    private static final Vec3 RISING = new Vec3(-5.0, 6.6, 2.4);
    private static final Vec3 LEFT_RISING = new Vec3(5.0, 8.4, 3.2);
    private static final Vec3 THUMB_UP = new Vec3(-3.6, 9.0, 0.4);
    private static final Vec3 THUMB_UP_UP = new Vec3(0.331, 0.0, -0.944);
    private static final Vec3 THUMB_UP_PALM = new Vec3(0.912, 0.259, 0.319);
    private static final Vec3[] RISING_TURN = halfway(LETTING_GO_UP, LETTING_GO_PALM, THUMB_UP_UP, THUMB_UP_PALM);
    private static final Vec3[] LEFT_RISING_TURN = halfway(LEFT_LETTING_GO_UP, LEFT_LETTING_GO_PALM,
            THUMB_UP_UP.multiply(-1.0, 1.0, 1.0), THUMB_UP_PALM.multiply(-1.0, 1.0, 1.0));

    static final double ROLL_RADIUS = 1.4;
    static final double ROLL_TURNS = Math.PI * 3.3;
    static final double ROLL_FROM = OK + 21.0;
    static final double ROLL_TO = AXE_OPENS - 4.5;

    private static final double[] BALLED = digits(0.55, 0.6, 0.62, 0.66, 0.4, 0.3, 0.3, 0.32, 0.35, 0.1, 0.0);
    private static final double[] LOOSE = digits(0.2, 0.24, 0.28, 0.33, 0.3, 0.16, 0.18, 0.2, 0.24, 0.08, 0.45);
    private static final double[] SNAP_READY = digits(0.1, 0.595, 0.86, 0.9, 0.568, 0.08, 0.0, 0.25, 0.3, 0.0, 0.15);
    private static final double[] SNAP_DOWN = with(SNAP_READY, 1, 0.95, 6, 0.1);
    private static final double[] SNAPPED = digits(0.18, 0.95, 0.92, 0.94, 0.22, 0.1, 0.1, 0.28, 0.3, 0.0, 0.2);
    private static final double[] FANNED = digits(0.2, 0.03, 0.05, 0.09, 0.15, 0.16, -0.05, -0.04, 0.0, 0.08, 0.9);
    private static final double[] OK_SIGN = digits(0.4, 0.03, 0.05, 0.09, 0.415, 0.28, -0.05, -0.04, 0.0, 0.0, 0.9);
    private static final double[] CRAWLING = digits(0.42, 0.45, 0.47, 0.5, 0.3, 0.25, 0.25, 0.25, 0.25, 0.1, 0.35);
    private static final double[] SPREAD_WIDE = digits(0.02, 0.0, 0.02, 0.05, 0.0, -0.08, -0.08, -0.08, -0.06, 0.0,
            1.0);
    private static final double[] OPEN = digits(0.14, 0.14, 0.17, 0.2, 0.08, 0.06, 0.06, 0.08, 0.1, 0.0, 0.55);
    private static final double[] LEFT_OPEN = with(OPEN, 4, 0.32, 9, -0.5);
    private static final double[] GRIPPING = digits(0.61, 0.695, 0.651, 0.483, 0.62, 0.158, 0.144, 0.158, 0.095, 0.05,
            0.0);
    private static final double[] RELEASED = digits(0.3, 0.32, 0.34, 0.36, 0.34, 0.08, 0.08, 0.08, 0.08, 0.0, 0.2);
    private static final double[] FIST = digits(1.0, 1.0, 1.0, 1.0, 0.55, 0.1, 0.1, 0.1, 0.1, 0.1, 0.0);
    private static final double[] THUMB_STRAIGHT = digits(1.0, 1.0, 1.0, 1.0, 0.0, 0.1, 0.1, 0.1, 0.1, 0.0, 0.0,
            0.748);
    private static final double[] TRAILING = digits(0.16, 0.18, 0.2, 0.23, 0.1, 0.12, 0.12, 0.14, 0.16, 0.04, 0.3);

    private static final double[] OPENING = { 0.0, 0.7, 1.4, 2.1, 0.35 };
    private static final double[] QUICK_OPENING = { 0.0, 0.3, 0.6, 0.9, 0.15 };
    private static final double[] CLOSING = { 1.2, 0.8, 0.4, 0.0, 1.6 };
    private static final double[] QUICK_CLOSING = { 0.6, 0.4, 0.2, 0.0, 0.8 };
    private static final double[] TOGETHER = { 0.0, 0.0, 0.0, 0.0, 0.0 };

    static final Track RIGHT = new Track(
            new Key(OUT, true, HOVER, HOVER_UP, HOVER_PALM),
            new Key(OUT + 8, false, HOVER.add(0.15, 0.25, -0.1), HOVER_UP, HOVER_PALM),
            new Key(SNAP - 7, true, SNAPPING, SNAPPING_UP, SNAPPING_PALM),
            new Key(SNAP - 1.5, true, SNAPPING.add(0.0, -0.06, 0.02), SNAPPING_UP, SNAPPING_PALM),
            new Key(SNAP + 5, true, SNAPPING.add(0.05, 0.12, 0.08), SNAPPING_UP, SNAPPING_PALM),
            new Key(OK + 5, false, WATCHING, WATCHING_UP, WATCHING_PALM),
            new Key(OK + 13, false, WATCHING.add(0.1, 0.12, -0.05), WATCHING_UP, WATCHING_PALM),
            new Key(ROLL_FROM + 1, true, ROLLING, ROLLING_UP, ROLLING_PALM),
            new Key(ROLL_TO - 1.5, true, ROLLING, ROLLING_UP, ROLLING_PALM),
            new Key(AXE_OPENS - 1, false, CONJURING.add(-0.2, 0.35, 0.1), CONJURING_UP, CONJURING_PALM),
            new Key(AXE_OPENS + 2, true, CONJURING, CONJURING_UP, CONJURING_PALM),
            new Key(GRAB - 7, false, REACHING, REACHING_UP, REACHING_PALM),
            new Key(GRAB, true, GRABBING, GRABBING_UP, GRABBING_PALM),
            new Key(RELEASE, true, LETTING_GO, LETTING_GO_UP, LETTING_GO_PALM),
            new Key(RELEASE + 6, false, RISING, RISING_TURN[0], RISING_TURN[1]),
            new Key(THUMBS, true, THUMB_UP, THUMB_UP_UP, THUMB_UP_PALM),
            new Key(RETRACT, true, THUMB_UP, THUMB_UP_UP, THUMB_UP_PALM));

    static final Track LEFT = new Track(
            new Key(OUT, true, HOVER, HOVER_UP, HOVER_PALM).mirrored(),
            new Key(OUT + 8, false, HOVER.add(0.1, 0.2, -0.05), HOVER_UP, HOVER_PALM).mirrored(),
            new Key(SNAP - 4, false, HOVER.add(0.35, 0.35, -0.15), new Vec3(0.38, 0.0, -1.0), HOVER_PALM).mirrored(),
            new Key(SNAP + 2, false, HOVER.add(0.4, 0.5, -0.1), new Vec3(0.34, 0.15, -1.0), HOVER_PALM).mirrored(),
            new Key(OK - 1, true, SNAPPING.add(0.0, -0.1, 0.1), SNAPPING_UP, SNAPPING_PALM).mirrored(),
            new Key(OK + 13, true, SNAPPING.add(0.0, 0.0, 0.1), SNAPPING_UP, SNAPPING_PALM).mirrored(),
            new Key(ROLL_FROM + 1, true, ROLLING, ROLLING_UP, ROLLING_PALM).mirrored(),
            new Key(ROLL_TO - 1.5, true, ROLLING, ROLLING_UP, ROLLING_PALM).mirrored(),
            new Key(AXE_OPENS - 1, false, CONJURING.add(-0.2, 0.35, 0.1), CONJURING_UP, CONJURING_PALM).mirrored(),
            new Key(AXE_OPENS + 2, true, CONJURING, CONJURING_UP, CONJURING_PALM).mirrored(),
            new Key(GRAB - 7, false, LEFT_REACHING, LEFT_REACHING_UP, LEFT_REACHING_PALM),
            new Key(GRAB, true, LEFT_GRABBING, GRABBING_UP.multiply(-1.0, 1.0, 1.0),
                    GRABBING_PALM.multiply(-1.0, 1.0, 1.0)),
            new Key(RELEASE, true, LEFT_LETTING_GO, LEFT_LETTING_GO_UP, LEFT_LETTING_GO_PALM),
            new Key(RELEASE + 6, false, LEFT_RISING, LEFT_RISING_TURN[0], LEFT_RISING_TURN[1]),
            new Key(THUMBS, true, THUMB_UP, THUMB_UP_UP, THUMB_UP_PALM).mirrored(),
            new Key(RETRACT, true, THUMB_UP, THUMB_UP_UP, THUMB_UP_PALM).mirrored());

    static final Fingers RIGHT_FINGERS = new Fingers(BALLED)
            .to(LOOSE, ARRIVES + 3, 7.0, SMOOTH, OPENING)
            .wave(ARRIVES + 2, ARRIVES + 7, SNAP - 13, SNAP - 8, 0.55, 0.95, 0.2, 0.15, 0.1)
            .to(SNAP_READY, SNAP - 12, 6.0, SMOOTH, new double[] { 1.2, 1.4, 0.0, 0.3, 1.6 })
            .tremble(SNAP - 5, SNAP - 1.4, 3.2, 0.012)
            .to(SNAP_DOWN, SNAP - 1.4, 1.4, LATE, TOGETHER)
            .to(SNAPPED, SNAP - 1.2, 1.6, EARLY, TOGETHER)
            .twitch(SNAP, 0.9, digits(-0.1, -0.07, 0.0, 0.0, -0.06, -0.05, -0.04, 0.0, 0.0, 0.0, 0.05))
            .to(LOOSE, SNAP + 4, 8.0, SMOOTH, OPENING)
            .wave(SNAP + 6, SNAP + 12, OK + 14, OK + 18, 0.42, 0.9, 0.09, 0.07, 0.05)
            .to(CRAWLING, OK + 15, 5.0, SMOOTH, OPENING)
            .wave(OK + 16, OK + 21, AXE_OPENS - 7, AXE_OPENS - 4, 1.35, 1.25, 0.3, 0.25, 0.15)
            .to(SPREAD_WIDE, AXE_OPENS - 5, 4.0, EARLY, QUICK_OPENING)
            .to(OPEN, AXE_OPENS + 3, 7.0, SMOOTH, OPENING)
            .to(GRIPPING, GRAB - 3, 3.5, SMOOTH, CLOSING)
            .to(RELEASED, RELEASE - 1.5, 3.0, SMOOTH, OPENING)
            .to(FIST, RELEASE + 2.5, 4.0, SMOOTH, QUICK_CLOSING)
            .to(THUMB_STRAIGHT, THUMBS - 1.5, 1.0, POP, TOGETHER)
            .twitch(THUMBS, 1.2, digits(0.05, 0.05, 0.05, 0.05, 0.0, 0.06, 0.06, 0.06, 0.06, -0.14, 0.0))
            .to(TRAILING, RETRACT + 1, 5.0, SMOOTH, OPENING);

    static final Fingers LEFT_FINGERS = new Fingers(BALLED)
            .to(LOOSE, ARRIVES + 3.4, 7.0, SMOOTH, OPENING)
            .wave(ARRIVES + 2, ARRIVES + 7, SNAP - 4, SNAP + 1, 0.5, 0.95, 0.2, 0.15, 0.1)
            .to(FANNED, OK - 9, 5.0, SMOOTH, new double[] { 0.0, 0.6, 0.3, 0.0, 0.0 })
            .to(OK_SIGN, OK - 5, 5.0, EARLY, new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 })
            .twitch(OK, 1.3, digits(0.0, -0.04, -0.04, -0.04, 0.0, 0.0, -0.05, -0.05, -0.05, 0.0, 0.08))
            .to(CRAWLING, OK + 15.4, 5.0, SMOOTH, OPENING)
            .wave(OK + 16.4, OK + 21.4, AXE_OPENS - 6.6, AXE_OPENS - 3.6, 1.35, 1.25, 0.3, 0.25, 0.15)
            .to(SPREAD_WIDE, AXE_OPENS - 4.6, 4.0, EARLY, QUICK_OPENING)
            .to(LEFT_OPEN, AXE_OPENS + 3.4, 7.0, SMOOTH, OPENING)
            .to(GRIPPING, GRAB - 3, 3.5, SMOOTH, CLOSING)
            .to(RELEASED, RELEASE - 1.1, 3.0, SMOOTH, OPENING)
            .to(FIST, RELEASE + 2.9, 4.0, SMOOTH, QUICK_CLOSING)
            .to(THUMB_STRAIGHT, THUMBS - 1.1, 1.0, POP, TOGETHER)
            .twitch(THUMBS + 0.4, 1.2, digits(0.05, 0.05, 0.05, 0.05, 0.0, 0.06, 0.06, 0.06, 0.06, -0.14, 0.0))
            .to(TRAILING, RETRACT + 1.4, 5.0, SMOOTH, OPENING);

    record Layout(Vec3 base, Vec3 way, Vec3 side, double scale, double aimSide, double aimAway) {
        static Layout of(Vec3 base, int variant, Vec3 aim, double scale) {
            Vec3 way = HandPose.axeWay(variant);
            Vec3 side = way.cross(Vectors.UP);
            double size = Math.max(1.0E-3, scale);
            double dx = aim.x - base.x;
            double dz = aim.z - base.z;
            double across = (dx * side.x + dz * side.z) / size;
            double away = (dx * way.x + dz * way.z) / size;
            double far = Math.sqrt(across * across + away * away);
            if (far > REACH) {
                across *= REACH / far;
                away *= REACH / far;
            }
            return new Layout(base, way, side, size, across, away);
        }

        Vec3 dir(Vec3 local) {
            return this.side.scale(local.x).add(0.0, local.y, 0.0).add(this.way.scale(local.z));
        }

        Vec3 point(Vec3 local) {
            return this.base.add(this.dir(local).scale(this.scale));
        }

        double across() {
            return this.aimSide * FOLLOW_SIDE;
        }

        double roll() {
            return Math.asin(Mth.clamp((this.aimSide - this.across()) / EDGE_OUT, -0.8, 0.8));
        }

        Vec3 strike() {
            return new Vec3(this.across() + EDGE_OUT * Math.sin(this.roll()), -BITE, this.aimAway);
        }

        Vec3 struck() {
            double edge = EDGE_OUT * Math.cos(this.roll());
            Vec3 strike = new Vec3(this.across(), -BITE, this.aimAway);
            Vec3 home = new Vec3(this.across(), HOME.y, HOME.z + this.aimAway * FOLLOW_AWAY);
            return strike.add(home.subtract(strike).normalize().scale(Math.sqrt(LEVER * LEVER + edge * edge)));
        }

        double lean() {
            Vec3 from = this.struck().subtract(new Vec3(this.across(), -BITE, this.aimAway));
            return Math.atan2(from.y, from.z) - Math.atan2(EDGE_OUT * Math.cos(this.roll()), LEVER);
        }

        Vec3 raised() {
            return new Vec3(this.across(), RAISE_Y, RAISE_Z);
        }
    }
}
