package nl.tivek.multiversepowers.character.greenlantern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

abstract class RevolverKeys extends RevolverScript {
    public static final Vec3 PINCH = new Vec3(-0.95, 5.05, 0.95);
    public static final Vec3 BACK = new Vec3(0.0, 1.8, -0.62);
    public static final Vec3 PALM_AT = new Vec3(0.0, 1.6, 0.7);
    public static final double HAT_SIZE = 0.78;
    public static final double HAT_TALL = 2.3 * HAT_SIZE;

    private static final Vec3[] JAZZ_O = { new Vec3(-0.12, 1.0, -0.15), new Vec3(0.08, 0.15, -1.0) };
    private static final Vec3[] WARM_O = { new Vec3(-0.3, 1.0, -0.1), new Vec3(0.12, 0.1, -1.0) };
    private static final Vec3[] DIP_O = { TOP_NORMAL.scale(-1.0), new Vec3(0.0, 0.8, 0.6) };
    private static final Vec3[] HANG_O = { new Vec3(0.0, -1.0, 0.0), new Vec3(0.0, 0.0, 1.0) };
    private static final Vec3[] SHOW_O = { new Vec3(0.0, -0.85, 0.52), new Vec3(0.0, 0.52, 0.85) };
    private static final Vec3[] WATCH_O = { new Vec3(-0.45, 0.45, -0.77), new Vec3(0.2, -0.9, -0.3) };
    private static final Vec3[] FIST_O = { new Vec3(-0.6, 0.0, -0.8), new Vec3(0.0, -1.0, 0.0) };
    private static final Vec3[] GUN_O = { new Vec3(-0.3, 0.1, -0.95), new Vec3(-0.95, 0.0, 0.3) };
    private static final Vec3[] BLOW_O = { new Vec3(-0.1, 1.0, -0.15), new Vec3(-1.0, 0.0, 0.0) };
    private static final Vec3[] PALM_UP_O = { new Vec3(-0.55, 0.25, -0.8), new Vec3(0.0, 1.0, 0.3) };
    private static final Vec3[] CATCH_O = { new Vec3(-1.0, 0.25, -0.1), new Vec3(0.0, 1.0, -0.35) };
    private static final Vec3[] FLICK_O = { new Vec3(-0.5, 0.6, -0.62), new Vec3(0.0, 0.75, 0.6) };
    private static final Vec3[] WIND_O = { new Vec3(-0.9, 0.3, -0.3), new Vec3(0.0, 1.0, -0.3) };
    private static final Vec3[] THROW_O = { new Vec3(-0.5, 0.55, 0.67), new Vec3(0.0, 0.8, -0.6) };
    private static final Vec3[] FOLLOW_O = { new Vec3(-0.4, 0.7, 0.6), new Vec3(0.0, 0.7, -0.7) };
    private static final Vec3[] CONDUCT_O = { new Vec3(-0.3, 0.9, -0.25), new Vec3(0.2, 0.25, -1.0) };
    private static final Vec3[] CLAW_DOWN_O = { new Vec3(0.0, -0.9, 0.4), new Vec3(0.0, 0.4, 0.9) };
    private static final Vec3[] LEAN_O = { new Vec3(0.35, 0.9, 0.25), new Vec3(-0.2, 0.1, -1.0) };
    private static final Vec3[] AFTER_TOSS_O = { new Vec3(0.1, 0.5, 0.86), new Vec3(0.1, 0.86, -0.5) };
    private static final Vec3[] WIPE_O = { new Vec3(-1.0, 0.15, 0.05), new Vec3(0.0, -0.5, 0.86) };
    private static final Vec3[] WIPED_O = { new Vec3(-0.6, 0.75, 0.1), new Vec3(0.0, -0.2, 1.0) };
    private static final Vec3[] TIRED_O = { new Vec3(0.1, -0.8, -0.6), new Vec3(0.0, -0.6, 0.8) };
    private static final Vec3[] RAISED_O = { new Vec3(0.1, 1.0, 0.0), new Vec3(-1.0, 0.0, 0.0) };
    private static final Vec3[] HIGH_O = { new Vec3(-0.15, 1.0, 0.0), new Vec3(-1.0, -0.15, 0.0) };
    private static final Vec3[] BOUNCE_O = { new Vec3(-0.1, 1.0, 0.0), new Vec3(-1.0, 0.0, 0.1) };
    private static final Vec3[] BRIM_O = { new Vec3(-0.25, 1.0, -0.1), new Vec3(0.15, 0.05, -1.0) };
    private static final Vec3[] SALUTE_O = { new Vec3(0.45, 0.9, -0.1), new Vec3(0.2, 0.0, -1.0) };

    private static final Vec3 REST = new Vec3(4.6, 6.6, 1.2);
    private static final Vec3 WARM = new Vec3(4.3, 7.3, 0.8);
    private static final Vec3 WARMED = new Vec3(4.0, 7.7, 0.9);
    private static final Vec3 ARC_UP = new Vec3(2.8, 11.4, 0.4);
    private static final Vec3 WATCH = new Vec3(5.0, 7.4, 0.8);
    private static final Vec3 FIST_AT = new Vec3(3.0, 6.2, 0.2);
    private static final Vec3 GUN_AT = new Vec3(3.4, 7.8, 0.0);
    private static final Vec3 CONDUCT_AT = new Vec3(5.2, 5.6, 1.4);
    private static final Vec3 LEAN_AT = new Vec3(6.3, 7.9, 2.4);
    private static final Vec3 TIRED_AT = new Vec3(4.6, 6.0, 0.6);

    private static final Vec3 HAT_TOP = hatTop();

    private static final double[] BALLED = digits(0.55, 0.6, 0.62, 0.66, 0.4, 0.3, 0.3, 0.32, 0.35, 0.1, 0.0);
    private static final double[] LOOSE = digits(0.2, 0.24, 0.28, 0.33, 0.3, 0.16, 0.18, 0.2, 0.24, 0.08, 0.45);
    private static final double[] RELAXED = digits(0.3, 0.34, 0.38, 0.42, 0.3, 0.2, 0.22, 0.25, 0.28, 0.1, 0.3);
    private static final double[] JAZZ = digits(0.02, 0.0, 0.02, 0.05, 0.0, -0.06, -0.06, -0.06, -0.05, 0.0, 0.9,
            0.3);
    private static final double[] OPEN = digits(0.14, 0.14, 0.17, 0.2, 0.08, 0.06, 0.06, 0.08, 0.1, 0.0, 0.55);
    private static final double[] PINCH_READY = digits(0.12, 0.5, 0.55, 0.6, 0.15, 0.1, 0.2, 0.2, 0.2, 0.1, 0.35,
            0.45);
    private static final double[] PINCHED = digits(0.3, 0.72, 0.78, 0.82, 0.45, 0.35, 0.3, 0.3, 0.3, 0.3, 0.15, 0.2);
    private static final double[] POINT = digits(0.0, 0.95, 1.0, 1.0, 0.6, 0.0, 0.1, 0.1, 0.1, 0.2, 0.0, 0.2);
    private static final double[] FIST = digits(1.0, 1.0, 1.0, 1.0, 0.55, 0.1, 0.1, 0.1, 0.1, 0.1, 0.0);
    private static final double[] GUN = digits(0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.1, 0.1, 0.1, 0.0, 0.0, 0.75);
    private static final double[] GUN_DOWN = with(GUN, 4, 0.7, 9, 0.35, 11, 0.25);
    private static final double[] SNAP_READY = digits(0.1, 0.595, 0.86, 0.9, 0.568, 0.08, 0.0, 0.25, 0.3, 0.0,
            0.15);
    private static final double[] SNAP_DOWN = with(SNAP_READY, 1, 0.95, 6, 0.1);
    private static final double[] SNAPPED = digits(0.18, 0.95, 0.92, 0.94, 0.22, 0.1, 0.1, 0.28, 0.3, 0.0, 0.2);
    private static final double[] CLAW = digits(0.3, 0.3, 0.33, 0.38, 0.25, 0.9, 0.9, 0.9, 0.9, 0.6, 0.55, 0.45);
    private static final double[] CLAW_SHUT = digits(0.62, 0.64, 0.68, 0.72, 0.55, 1.0, 1.0, 1.0, 1.0, 0.8, 0.1,
            0.25);
    private static final double[] CUP = digits(0.18, 0.2, 0.22, 0.26, 0.25, 0.2, 0.2, 0.22, 0.25, 0.1, 0.1, 0.5);
    private static final double[] RUB = digits(0.35, 0.3, 0.3, 0.32, 0.5, 0.2, 0.15, 0.15, 0.2, 0.25, 0.05, 0.3);
    private static final double[] GRIPPING = digits(0.61, 0.695, 0.651, 0.483, 0.62, 0.158, 0.144, 0.158, 0.095,
            0.05, 0.0);
    private static final double[] PISTOL = with(GRIPPING, 0, 0.4, 5, 0.35);
    private static final double[] SPINNING = digits(0.55, 0.2, 0.2, 0.25, 0.3, 0.4, 0.1, 0.1, 0.1, 0.1, 0.1, 0.4);
    private static final double[] SALUTING = digits(0.0, 0.0, 1.0, 1.0, 0.8, 0.0, 0.0, 0.15, 0.15, 0.3, -0.05,
            0.0);
    private static final double[] LIMP = digits(0.25, 0.28, 0.3, 0.34, 0.2, 0.35, 0.38, 0.4, 0.45, 0.2, 0.1, 0.3);
    private static final double[] TAPPING = digits(0.62, 0.8, 0.85, 0.9, 0.5, 0.25, 0.3, 0.3, 0.3, 0.2, 0.0, 0.2);
    private static final double[] WATCHING = digits(0.15, 0.18, 0.22, 0.26, 0.2, 0.12, 0.14, 0.16, 0.18, 0.1, 0.3);
    private static final double[] FLAT = digits(0.02, 0.02, 0.03, 0.04, 0.05, 0.0, 0.0, 0.0, 0.0, 0.0, 0.25, 0.7);
    private static final double[] WIPING = digits(0.45, 0.5, 0.55, 0.6, 0.4, 0.3, 0.3, 0.3, 0.3, 0.2, 0.0, 0.1);
    private static final double[] TRAILING = digits(0.16, 0.18, 0.2, 0.23, 0.1, 0.12, 0.12, 0.14, 0.16, 0.04,
            0.3);

    private static final double[] OPENING = { 0.0, 0.7, 1.4, 2.1, 0.35 };
    private static final double[] QUICK_OPENING = { 0.0, 0.3, 0.6, 0.9, 0.15 };
    private static final double[] CLOSING = { 1.2, 0.8, 0.4, 0.0, 1.6 };
    private static final double[] TOGETHER = { 0.0, 0.0, 0.0, 0.0, 0.0 };

    static final Track A = new Track(
            key(OUT, true, REST, JAZZ_O),
            key(OUT + 12, false, WARM, WARM_O),
            key(WIGGLE_TO - 4, false, WARMED, JAZZ_O),
            key(HAT_REACH - 5, false, ARC_UP, between(JAZZ_O, DIP_O)),
            key(HAT_REACH, true, above(6.2, 1.0), DIP_O),
            key(HAT_DIP, true, above(3.2, 0.6), DIP_O),
            key(HAT_PINCH + 1, true, above(3.3, 0.6), DIP_O),
            key(HAT_PINCH + 4, false, above(4.8, 0.7), DIP_O),
            key(HAT_OUT - 2, false, above(8.0, 0.8), DIP_O),
            key(HAT_OUT + 1, true, above(7.5, 0.8), DIP_O),
            key(HAT_OUT + 6, false, above(7.5, 0.8).add(-0.5, 0.6, -0.6), between(DIP_O, SHOW_O)),
            key(HAT_OVER, false, over(0.8), SHOW_O),
            key(HAT_ON, true, over(0.0), HANG_O),
            key(HAT_ON + 4, false, over(0.9).add(0.2, 0.0, 0.05), HANG_O),
            key(HAT_TAP, true, tap(0.0), HANG_O),
            key(HAT_TAP + 3, false, tap(0.8).add(0.4, 0.0, 0.0), HANG_O),
            key(HAT_TAP + 11, true, WATCH, WATCH_O),
            key(PEWS[2], false, WATCH.add(0.1, 0.2, 0.0), WATCH_O),
            key(BLOW, true, WATCH.add(0.2, 0.3, 0.1), WATCH_O),
            key(PARTS_REACH - 7, true, WATCH, WATCH_O),
            key(PARTS_REACH - 3, false, ARC_UP, between(WATCH_O, DIP_O)),
            key(PARTS_REACH, true, above(6.2, 1.0), DIP_O),
            key(PARTS_DIP, true, above(3.2, 0.6), DIP_O),
            key(PARTS_GRAB + 1, true, above(3.3, 0.6), DIP_O),
            key(PARTS_GRAB + 5, false, above(5.0, 0.7), DIP_O),
            key(PARTS_OUT, true, above(7.4, 0.9), DIP_O),
            key(PARTS_OUT + 4, false, new Vec3(3.4, 10.4, -0.4), between(DIP_O, PALM_UP_O)),
            key(PARTS_FLICK - 2, true, new Vec3(3.6, 9.6, -0.6), PALM_UP_O),
            key(PARTS_FLICK + 2, false, new Vec3(3.4, 10.4, -0.8), FLICK_O),
            key(PARTS_FLICK + 10, true, CONDUCT_AT, CONDUCT_O),
            key(WHOLE + 4, true, CONDUCT_AT.add(0.2, 0.4, 0.0), CONDUCT_O),
            key(GRABS - 8, true, nearGrip(), gripAt()),
            key(TOSS + 6, false, new Vec3(4.3, 11.2, 3.0), AFTER_TOSS_O),
            key(WIPE, true, new Vec3(2.4, 9.0, -0.8), WIPE_O),
            key(WIPED - 2, false, new Vec3(5.0, 9.5, -0.5), WIPE_O),
            key(WIPED, true, new Vec3(5.6, 9.9, -0.3), WIPED_O),
            key(TIRED + 4, true, TIRED_AT, TIRED_O),
            key(HIGH_FIVE - 10, true, TIRED_AT.add(0.0, 0.2, 0.0), TIRED_O),
            key(HIGH_FIVE - 5, false, new Vec3(5.2, 8.8, 1.2), RAISED_O),
            key(HIGH_FIVE, true, new Vec3(0.95, 9.6, 0.4), HIGH_O),
            key(HIGH_FIVE + 5, false, new Vec3(2.6, 9.9, 0.6), BOUNCE_O),
            key(SALUTE - 5, false, new Vec3(3.2, 9.4, -0.2), BRIM_O),
            key(SALUTE, true, new Vec3(5.4, 10.0, 0.4), SALUTE_O),
            key(RETRACT, true, new Vec3(5.5, 10.0, 0.45), SALUTE_O));

    static final Track B = new Track(bKeys());

    static final Fingers A_FINGERS = aFingers();
    static final Fingers B_FINGERS = bFingers();

    private static Key key(double t, boolean still, Vec3 at, Vec3[] turn) {
        return new Key(t, still, at, turn[0], turn[1]);
    }

    private static Vec3[] between(Vec3[] from, Vec3[] to) {
        return halfway(from[0], from[1], to[0], to[1]);
    }

    private static Key[] bKeys() {
        List<Key> keys = new ArrayList<>();
        keys.add(key(OUT, true, REST, JAZZ_O).mirrored());
        keys.add(key(OUT + 12.5, false, WARM.add(0.05, -0.05, 0.05), WARM_O).mirrored());
        keys.add(key(WIGGLE_TO - 4, false, WARMED.add(0.1, -0.1, 0.05), JAZZ_O).mirrored());
        keys.add(key(HAT_REACH, true, WATCH, WATCH_O).mirrored());
        keys.add(key(HAT_OUT - 2, true, WATCH.add(0.2, 0.2, 0.0), WATCH_O).mirrored());
        keys.add(key(HAT_OVER - 10, false, FIST_AT.add(0.4, 0.8, 0.3), between(WATCH_O, FIST_O)).mirrored());
        keys.add(key(HAT_OVER - 5, true, FIST_AT, FIST_O).mirrored());
        keys.add(key(SHAKE_TO, true, FIST_AT, FIST_O).mirrored());
        keys.add(key(GUN_FORM - 3, false, FIST_AT.add(0.2, 0.9, -0.1), between(FIST_O, GUN_O)).mirrored());
        keys.add(key(GUN_FORM, true, GUN_AT, GUN_O).mirrored());
        keys.add(key(BLOW - 4, true, GUN_AT, GUN_O).mirrored());
        keys.add(key(BLOW, true, GUN_AT.add(0.0, 0.5, 0.3), BLOW_O).mirrored());
        keys.add(key(HAT_FLICK - 3, true, GUN_AT, GUN_O).mirrored());
        keys.add(key(HAT_FLICK + 1, false, GUN_AT.add(0.0, 0.6, -0.2), between(GUN_O, FLICK_O)).mirrored());
        keys.add(key(HAT_CATCH, true, new Vec3(3.4, 7.6, -0.2), CATCH_O).mirrored());
        keys.add(key(HAT_THROW - 6, true, new Vec3(5.2, 7.4, 1.0), WIND_O).mirrored());
        keys.add(key(HAT_THROW, false, new Vec3(2.6, 9.8, 1.4), THROW_O).mirrored());
        keys.add(key(HAT_THROW + 6, true, new Vec3(2.0, 10.6, 2.0), FOLLOW_O).mirrored());
        keys.add(key(PARTS_REACH + 8, true, WATCH, WATCH_O).mirrored());
        keys.add(key(CLAW_UP - 4, false, ARC_UP, between(WATCH_O, DIP_O)).mirrored());
        keys.add(key(CLAW_UP, true, above(6.4, 1.0), DIP_O).mirrored());
        keys.add(key(CLAW_UP + 6, true, above(6.4, 1.0), DIP_O).mirrored());
        keys.add(key(CLAW_UP + 12, true, above(5.0, 0.9), DIP_O).mirrored());
        keys.add(key(CLAW_UP + 16, true, above(5.0, 0.9), DIP_O).mirrored());
        keys.add(key(CLAW_UP + 22, true, above(3.4, 0.7), DIP_O).mirrored());
        keys.add(key(CLAW_CLAMP + 2, true, above(3.4, 0.7), DIP_O).mirrored());
        keys.add(key(CLAW_CLAMP + 5, false, above(4.2, 0.8), DIP_O).mirrored());
        keys.add(key(CLAW_OUT, true, above(6.8, 1.0), DIP_O).mirrored());
        keys.add(key(CLAW_DROP - 2, true, new Vec3(3.6, 13.4, 0.6), CLAW_DOWN_O).mirrored());
        keys.add(key(CLAW_DROP + 2, false, new Vec3(3.6, 13.7, 0.7), CLAW_DOWN_O).mirrored());
        keys.add(key(CONDUCT, true, CONDUCT_AT, CONDUCT_O).mirrored());
        keys.add(key(WHOLE + 4, true, CONDUCT_AT.add(0.2, 0.4, 0.0), CONDUCT_O).mirrored());
        RevolverGun.Pose load = RevolverGun.pose(LOAD_POSE + 1, Vec3.ZERO);
        Vec3 rear = RevolverGun.chamberCenter(load, 1.0).subtract(load.forward().scale(2.6));
        // B stays out to its own side, level with the cylinder and clear of the fist under the grip: the bullets fly
        // the rest of the way.
        Vec3 across = load.up();
        Vec3 fingers = load.forward().scale(0.8).add(across.scale(0.6)).normalize();
        Vec3 palm = across.scale(-1.0);
        palm = palm.subtract(fingers.scale(palm.dot(fingers))).normalize();
        keys.add(palmAt(BULLETS_IN_HAND[0] - 6, rear.add(across.scale(4.4)).subtract(load.forward().scale(0.6)),
                fingers, palm));
        keys.add(palmAt(CYL_OPEN - 1, rear.add(across.scale(4.3)).subtract(load.forward().scale(0.5)), fingers,
                palm));
        keys.add(palmAt(LOADS[0] - 3, rear.add(across.scale(4.1)).subtract(load.forward().scale(0.3)), fingers,
                palm));
        keys.add(palmAt(LOADS[5], rear.add(across.scale(3.5)).add(load.forward().scale(0.5)), fingers, palm));
        keys.add(palmAt(CYL_CLOSE, rear.add(across.scale(4.3)).subtract(load.forward().scale(0.4)), fingers, palm));
        keys.add(key(AIMS + 4, true, LEAN_AT, LEAN_O).mirrored());
        keys.add(key(WIPE - 4, true, LEAN_AT, LEAN_O).mirrored());
        keys.add(key(WIPE + 0.4, true, new Vec3(2.4, 9.0, -0.8), WIPE_O).mirrored());
        keys.add(key(WIPED - 1.6, false, new Vec3(5.0, 9.5, -0.5), WIPE_O).mirrored());
        keys.add(key(WIPED + 0.4, true, new Vec3(5.6, 9.9, -0.3), WIPED_O).mirrored());
        keys.add(key(TIRED + 4.4, true, TIRED_AT, TIRED_O).mirrored());
        keys.add(key(HIGH_FIVE - 10, true, TIRED_AT.add(0.0, 0.2, 0.0), TIRED_O).mirrored());
        keys.add(key(HIGH_FIVE - 5, false, new Vec3(5.2, 8.8, 1.2), RAISED_O).mirrored());
        keys.add(key(HIGH_FIVE, true, new Vec3(0.95, 9.6, 0.4), HIGH_O).mirrored());
        keys.add(key(HIGH_FIVE + 5, false, new Vec3(2.6, 9.9, 0.6), BOUNCE_O).mirrored());
        keys.add(key(SALUTE - 4.6, false, new Vec3(3.2, 9.4, -0.2), BRIM_O).mirrored());
        keys.add(key(SALUTE + 0.4, true, new Vec3(5.4, 10.0, 0.4), SALUTE_O).mirrored());
        keys.add(key(RETRACT, true, new Vec3(5.5, 10.0, 0.45), SALUTE_O).mirrored());
        return keys.toArray(Key[]::new);
    }

    private static Key palmAt(double t, Vec3 palm, Vec3 fingers, Vec3 facing) {
        return new Key(t, true, wristFor(palm, PALM_AT, fingers, facing, false), fingers, facing);
    }

    private static Fingers aFingers() {
        Fingers fingers = new Fingers(BALLED)
                .to(LOOSE, ARRIVES + 3, 7.0, SMOOTH, OPENING)
                .to(JAZZ, OUT - 3, 6.0, SMOOTH, OPENING)
                .wave(OUT + 1, OUT + 6, WIGGLE_TO - 3, WIGGLE_TO + 2, 0.55, 0.75, 0.2, 0.14, 0.1)
                .to(OPEN, HAT_REACH - 7, 5.0, SMOOTH, OPENING)
                .to(PINCH_READY, HAT_DIP - 4, 3.0, SMOOTH, CLOSING)
                .to(PINCHED, HAT_PINCH - 1.5, 2.0, EARLY, TOGETHER)
                .to(OPEN, HAT_ON - 0.5, 2.5, EARLY, QUICK_OPENING)
                .to(POINT, HAT_TAP - 4, 3.0, SMOOTH, CLOSING)
                .twitch(HAT_TAP, 0.8, digits(0.12, 0.0, 0.0, 0.0, 0.0, 0.1))
                .to(JAZZ, HAT_TAP + 6, 5.0, SMOOTH, OPENING)
                .wave(SHAKE_FROM, SHAKE_FROM + 4, SHAKE_TO - 2, SHAKE_TO + 2, 0.9, 0.9, 0.12, 0.08, 0.06)
                .to(WATCHING, SHAKE_TO, 6.0, SMOOTH, CLOSING);
        for (int pew : PEWS) {
            fingers.twitch(pew + 0.5, 0.9, digits(0.1, 0.12, 0.14, 0.16, 0.08, 0.08, 0.08, 0.08, 0.08, 0.0, -0.12));
        }
        fingers.to(OPEN, PARTS_REACH - 6, 5.0, SMOOTH, OPENING)
                .to(CLAW_SHUT, PARTS_GRAB - 1.5, 2.5, EARLY, TOGETHER)
                .to(CUP, PARTS_FLICK - 4, 3.0, SMOOTH, OPENING)
                .to(FLAT, PARTS_FLICK - 0.5, 1.6, POP, TOGETHER)
                .to(WATCHING, PARTS_FLICK + 6, 6.0, SMOOTH, CLOSING);
        for (int part : new int[] { BARREL, HAMMER, GUARD }) {
            snap(fingers, BUILT[part] - FLY_TICKS);
        }
        fingers.to(OPEN, GRABS - 10, 4.0, SMOOTH, OPENING)
                .to(PISTOL, GRABS - 2.5, 3.0, SMOOTH, CLOSING);
        for (int fall : SHOTS) {
            fingers.twitch(fall - 7, 2.0, digits(0.0, 0.0, 0.0, 0.0, -0.3, 0.0, 0.0, 0.0, 0.0, 0.25));
            fingers.twitch(fall, 0.7, digits(0.15));
        }
        for (int fall : DRY) {
            fingers.twitch(fall - 7, 2.0, digits(0.0, 0.0, 0.0, 0.0, -0.3, 0.0, 0.0, 0.0, 0.0, 0.25));
            fingers.twitch(fall, 0.7, digits(0.15));
        }
        return fingers.to(SPINNING, SPIN_FROM - 1, 2.0, SMOOTH, OPENING)
                .to(GRIPPING, SPIN_TO + 1, 3.0, SMOOTH, CLOSING)
                .to(OPEN, TOSS - 0.5, 2.0, POP, TOGETHER)
                .to(RELAXED, TOSS + 5, 6.0, SMOOTH, CLOSING)
                .to(WIPING, WIPE - 3, 3.0, SMOOTH, CLOSING)
                .twitch(WIPED, 1.0, digits(-0.3, -0.3, -0.3, -0.3, -0.2, -0.2, -0.2, -0.2, -0.2, 0.0, 0.3))
                .to(LIMP, TIRED, 6.0, SMOOTH, OPENING)
                .wave(TIRED + 2, TIRED + 6, HIGH_FIVE - 12, HIGH_FIVE - 8, 0.9, 0.5, 0.08, 0.3, 0.05)
                .to(FLAT, HIGH_FIVE - 8, 4.0, EARLY, QUICK_OPENING)
                .twitch(HIGH_FIVE, 0.8, digits(-0.1, -0.1, -0.1, -0.1, 0.0, -0.1, -0.1, -0.1, -0.1, 0.0, 0.25))
                .to(SALUTING, SALUTE - 6, 4.0, SMOOTH, CLOSING)
                .to(TRAILING, RETRACT + 1, 5.0, SMOOTH, OPENING);
    }

    private static Fingers bFingers() {
        Fingers fingers = new Fingers(BALLED)
                .to(LOOSE, ARRIVES + 3.4, 7.0, SMOOTH, OPENING)
                .to(JAZZ, OUT - 2.6, 6.0, SMOOTH, OPENING)
                .wave(OUT + 1.4, OUT + 6.4, WIGGLE_TO - 3, WIGGLE_TO + 2, 0.52, 0.78, 0.2, 0.14, 0.1)
                .to(RELAXED, WIGGLE_TO, 6.0, SMOOTH, CLOSING)
                .to(FIST, HAT_OVER - 9, 5.0, SMOOTH, CLOSING)
                .to(GUN, GUN_FORM - 4, 4.0, SMOOTH, OPENING);
        for (int pew : PEWS) {
            fingers.to(GUN_DOWN, pew - 0.8, 0.8, LATE, TOGETHER).to(GUN, pew + 1, 3.0, SMOOTH, TOGETHER);
        }
        fingers.twitch(HAT_FLICK, 0.7, digits(0.0, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.3))
                .to(CUP, HAT_CATCH - 4, 3.0, SMOOTH, OPENING)
                .to(OPEN, HAT_THROW - 1, 2.0, POP, TOGETHER)
                .to(RELAXED, HAT_THROW + 5, 5.0, SMOOTH, CLOSING)
                .to(CLAW, CLAW_UP - 4, 4.0, SMOOTH, OPENING)
                .tremble(CLAW_UP + 8, CLAW_CLAMP - 2, 3.0, 0.03)
                .to(CLAW_SHUT, CLAW_CLAMP - 1.5, 2.5, EARLY, TOGETHER)
                .to(OPEN, CLAW_DROP - 0.5, 2.0, POP, TOGETHER)
                .to(WATCHING, CLAW_DROP + 5, 5.0, SMOOTH, CLOSING);
        for (int part : new int[] { FRAME, GRIP, CYLINDER }) {
            int flies = BUILT[part] - FLY_TICKS;
            fingers.to(POINT, flies - 4, 3.0, SMOOTH, CLOSING).to(WATCHING, flies + 2, 4.0, SMOOTH, OPENING);
        }
        fingers.to(CUP, BULLETS_IN_HAND[0] - 8, 4.0, SMOOTH, OPENING)
                .to(RUB, BULLETS_IN_HAND[0] - 4, 3.0, SMOOTH, CLOSING)
                .tremble(BULLETS_IN_HAND[0] - 2, BULLETS_IN_HAND[5], 3.4, 0.05)
                .to(CUP, BULLETS_IN_HAND[5] + 1, 3.0, SMOOTH, OPENING)
                .to(FLAT, LOADS[0] - 3, 3.0, SMOOTH, OPENING)
                .to(RELAXED, LOADS[5] + 3, 4.0, SMOOTH, CLOSING)
                .to(TAPPING, AIMS + 2, 5.0, SMOOTH, CLOSING);
        for (int tap = AIMS + 10; tap < WIPE - 8; tap += 9) {
            fingers.twitch(tap, 0.8, digits(-0.3, 0.0, 0.0, 0.0, 0.0, -0.15));
        }
        return fingers.to(WIPING, WIPE - 2.6, 3.0, SMOOTH, CLOSING)
                .twitch(WIPED + 0.4, 1.0, digits(-0.3, -0.3, -0.3, -0.3, -0.2, -0.2, -0.2, -0.2, -0.2, 0.0, 0.3))
                .to(LIMP, TIRED + 0.4, 6.0, SMOOTH, OPENING)
                .wave(TIRED + 2.4, TIRED + 6.4, HIGH_FIVE - 12, HIGH_FIVE - 8, 0.85, 0.5, 0.08, 0.3, 0.05)
                .to(FLAT, HIGH_FIVE - 8, 4.0, EARLY, QUICK_OPENING)
                .twitch(HIGH_FIVE, 0.8, digits(-0.1, -0.1, -0.1, -0.1, 0.0, -0.1, -0.1, -0.1, -0.1, 0.0, 0.25))
                .to(SALUTING, SALUTE - 5.6, 4.0, SMOOTH, CLOSING)
                .to(TRAILING, RETRACT + 1.4, 5.0, SMOOTH, OPENING);
    }

    private static void snap(Fingers fingers, double at) {
        fingers.to(SNAP_READY, at - 5, 3.0, SMOOTH, CLOSING)
                .to(SNAP_DOWN, at - 1.4, 1.4, LATE, TOGETHER)
                .to(SNAPPED, at - 1.2, 1.6, EARLY, TOGETHER)
                .to(WATCHING, at + 2, 3.0, SMOOTH, OPENING);
    }

    private static Vec3 hatTop() {
        Key fist = key(0, true, FIST_AT, FIST_O).mirrored();
        Vec3 u = fist.up().normalize();
        Vec3 f = fist.palm().subtract(u.scale(fist.palm().dot(u))).normalize();
        return fist.at().add(u.scale(BACK.y)).add(f.scale(BACK.z)).add(0.0, HAT_TALL, 0.0);
    }

    private static Vec3 over(double lift) {
        return wristFor(HAT_TOP, PINCH, HANG_O[0], HANG_O[1], true).add(0.0, lift, 0.0);
    }

    private static Vec3 tap(double lift) {
        return wristFor(HAT_TOP.add(0.0, 0.05, 0.0), INDEX_TIP, HANG_O[0], HANG_O[1], true).add(0.0, lift, 0.0);
    }

    private static Vec3[] grip() {
        return RevolverGun.pistolGrip(RevolverGun.pose(GRABS, Vec3.ZERO));
    }

    private static Vec3 nearGrip() {
        return grip()[0].add(0.8, 0.6, 0.3);
    }

    private static Vec3[] gripAt() {
        Vec3[] grip = grip();
        return new Vec3[] { grip[1], grip[2] };
    }
}
