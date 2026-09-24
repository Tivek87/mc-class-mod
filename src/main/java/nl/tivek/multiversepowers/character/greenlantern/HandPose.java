package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * How one giant hand of hard light moves (see {@link GiantHands}), worked out alike by the server (where it strikes,
 * what it holds) and by every client (how it looks), from how long ago it was called, where it came up out of the
 * ground and what it reaches for.
 *
 * <p>A hand is a forearm standing up out of the ground at its base, with the hand on top of it. The forearm leans
 * over towards what it reaches for by {@link #lean}, comes up out of the ground by {@link #length} (below 0 the wrist
 * is still under the ground), turns about its own length by {@link #twist} (0: the palm faces what it reaches for), the
 * wrist bends the hand towards its palm by {@link #flex}, and the fingers curl (0 open, 1 a fist) and hook at their
 * tips. Measured at scale 1 in blocks: the hand is a right hand, 3.2 blocks wide and 6.3 long from its wrist to the tip
 * of its middle finger.
 *
 * <p>The moves:
 * <ul>
 * <li>{@link #SMACK}: it bursts out of the ground beside its creature, low over it, winds up and sweeps its open palm
 * through it sideways, swatting it away;</li>
 * <li>{@link #GRAB}: it bursts out of the ground right in front of its creature, fingers up round it, closes its fingers
 * on it, lifts it high, leans back and throws it;</li>
 * <li>{@link #FINGER}: it shoots up out of the ground right in front of its creature, middle finger first, the back of
 * the hand to it: bursting out it launches its creature and all round it far away, and then it gives it the middle
 * finger, jabbing it at it;</li>
 * <li>{@link #SLAM}: it comes straight up out of the ground, cocks its wrist back and bends it over, slapping its open
 * palm down flat on its creature and pressing it flat against the ground;</li>
 * <li>{@link #POUND}: it comes up, makes a fist and pounds the flat side of it down on the ground three times, each
 * time on the creature nearest to it within its reach;</li>
 * <li>{@link #AXE}: no hand of its own comes up: a pair of hands comes out of portals with an axe (see
 * {@link HandDuo}).</li>
 * </ul>
 * Every hand sinks back into the ground when it is done.
 *
 * <p>How it moves, so a hand this big feels heavy and alive: it bursts up out of the ground springing past where it
 * stays and settling back, corkscrewing a little and unfurling its fingers from a loose fist; while it waits it
 * breathes, swaying a little, its fingers never quite still; before a blow it winds up and holds a beat, strikes faster
 * and faster, gives a little as it lands, springs back, trembles and settles heavily; its wrist drags behind its
 * forearm and whips on past when the forearm stops, and its fingers trail behind the hand the same way; fingers close
 * one after another from the little finger and open from the index finger; and it lifts itself a little before it
 * sinks back into the ground. Its blows still land on the very ticks the server strikes them, where it strikes them.
 */
public final class HandPose {
    public static final int SMACK = 0;
    public static final int GRAB = 1;
    public static final int FINGER = 2;
    public static final int SLAM = 3;
    public static final int POUND = 4;
    /** The pair of hands with the axe (see {@link HandDuo}): no hand of its own comes up out of the ground. */
    public static final int AXE = 5;
    /**
     * How many moves there are: a variant of a hand is its move, plus this for a smack swept the other way round, plus
     * this times 1 to 16 for the way an axe pair faces (see {@link #axeVariant}).
     */
    public static final int MOVES = 6;
    /**
     * How many ticks one beat lasts: the moves below are laid out in beats and played this much slower, so a hand this
     * big moves slowly and heavily and stays a good while.
     */
    private static final double SLOW = 1.6;
    // The timeline of the moves, in beats: when the ring's light reaches its spot, how long each move takes until it has
    // sunk back into the ground, when a smack sweeps through, a grab closes and throws, a slam lands and stops pressing,
    // a pound strikes, and when each move starts to sink.
    private static final double ARRIVE = 4.0;
    private static final int[] LIFE = { 50, 58, 56, 62, 76 };
    private static final int[] SINK = { 36, 44, 42, 47, 62 };
    private static final double SMACK_BEAT = 19.0;
    private static final double CATCH_BEAT = 13.0;
    private static final double THROW_BEAT = 33.0;
    private static final double SLAM_BEAT = 25.0;
    private static final double PRESS_BEAT = 40.0;
    private static final double[] POUND_BEATS = { 24.0, 38.0, 52.0 };
    /** Ticks after a hand is called that the ring's light reaches its spot and it bursts out of the ground. */
    public static final int ARRIVES = ticks(ARRIVE);
    /** The tick a smack's palm sweeps through its creature. */
    public static final int SMACK_HITS = ticks(SMACK_BEAT);
    /** The ticks a grab closes on its creature and lets go of it, throwing it. */
    public static final int GRAB_CATCHES = ticks(CATCH_BEAT);
    public static final int GRAB_THROWS = ticks(THROW_BEAT);
    /** The tick a slam's palm lands flat on the ground, and until when it presses down. */
    public static final int SLAM_HITS = ticks(SLAM_BEAT);
    public static final int SLAM_PRESSES = ticks(PRESS_BEAT);
    /** The ticks the fist of a pound strikes the ground. */
    public static final int[] POUND_HITS = { ticks(POUND_BEATS[0]), ticks(POUND_BEATS[1]), ticks(POUND_BEATS[2]) };
    /** The tick the fist of a middle finger bursts out of the ground, its finger shooting up past it. */
    public static final int FINGER_BURSTS = ticks(4.6);
    /** The tick the middle finger is up and jabbing. */
    public static final int FINGER_UP = ticks(12.0);
    /** How far a swing of a smack or a throw reaches before and after its middle, in ticks, for its streak of light. */
    public static final double SWING_TICKS = 4.0 * SLOW;
    // The same moments in beats as the ticks fall, when the server strikes: the blows land exactly on them.
    private static final double SMACK_AT = SMACK_HITS / SLOW;
    private static final double CATCH_AT = GRAB_CATCHES / SLOW;
    private static final double THROW_AT = GRAB_THROWS / SLOW;
    private static final double SLAM_AT = SLAM_HITS / SLOW;
    private static final double BURST_AT = FINGER_BURSTS / SLOW;
    private static final double UP_AT = FINGER_UP / SLOW;
    private static final double[] POUND_AT = { POUND_HITS[0] / SLOW, POUND_HITS[1] / SLOW, POUND_HITS[2] / SLOW };
    /** How far from its base each move reaches for its creature, at scale 1, in blocks: where it comes up. */
    private static final double[] SPOT = { 5.8, 1.25, 2.2, 2.9, 5.0 };
    // How far under the ground the wrist starts, at scale 1: the whole hand is under it then.
    private static final double BURIED = -7.5;
    // The beat it starts to move up under the ground, the tips of its fingers breaking out of it about when the ring's
    // light arrives.
    private static final double RISE = 3.5;
    // Its fingers unfurling from a loose fist as it comes out: from when, over how long each, how far apart one after
    // another (as they close and open later on too), how loose that fist is, and how far they splay on the way.
    private static final double UNFURL = 4.4;
    private static final double UNFURL_BEATS = 2.2;
    private static final double STAGGER = 0.55;
    private static final double LOOSE_CURL = 0.62;
    private static final double LOOSE_HOOK = 0.4;
    private static final double LOOSE_THUMB = 0.55;
    private static final double SPLAY = 0.1;
    // How far it lifts itself before it sinks back into the ground.
    private static final double SINK_LIFT = 0.3;
    // How it trails behind itself: it looks back this many beats at this many moments, remembering them as a spring
    // would (the wrist heavier and slower than the fingers); how hard the wrist and the fingers drag, and how far at
    // most.
    private static final int TAPS = 10;
    private static final double TAP = 0.35;
    private static final double[] WRIST_WEIGHTS = remembered(2.4, 0.45);
    private static final double[] FINGER_WEIGHTS = remembered(3.2, 0.4);
    private static final double WRIST_DRAG = 0.6;
    private static final double WRIST_DRAG_MOST = 0.35;
    private static final double FINGER_DRAG = 1.2;
    private static final double FINGER_DRAG_MOST = 0.5;
    // How much each finger (index, middle, ring, little) trails: the little finger is the lightest.
    private static final double[] TRAIL = { 1.0, 0.9, 1.0, 1.15 };
    // How near a blow, in beats, the hand is drawn onto where that blow lands.
    private static final double ONTO = 1.6;
    // A smack: how low over the ground the arm leans as it sweeps, and where its sweep and wrist are on the tick it
    // strikes (the palm sweeps through its creature, at sweep 0, half a beat later).
    private static final double SMACK_LEAN = 1.26;
    private static final double SMACK_STRUCK = -0.2624;
    private static final double SMACK_STRUCK_FLEX = -0.0619;
    // How far a smack's and a throw's wrist still trails behind its forearm on the tick it strikes: their keys lay the
    // wrist out that much further on, so it trails right onto where the blow lands.
    private static final double SMACK_TRAILS = 0.1246;
    private static final double THROW_TRAILS = 0.1597;
    // A smack in keys (beat, value, speed per beat): how far it sweeps round its base (winding up the other way first,
    // following through past where it stops and springing back), how its wrist cocks back and whips through, and how
    // far it draws its forearm in and leans up as it winds up, reaching through and dipping as it strikes.
    private static final double[] SMACK_SWEEP = { 8.0, -0.7, 0.0, 15.0, -0.95, 0.0, 16.6, -0.82, 0.2, SMACK_AT,
            SMACK_STRUCK, 0.64, 20.2, 0.58, 0.28, 21.3, 0.72, 0.0, 23.2, 0.56, 0.0, 25.4, 0.62, 0.0, 27.5, 0.6, 0.0 };
    private static final double[] SMACK_FLEX = { 9.0, 0.0, 0.0, 15.0, -0.34, 0.0, 16.8, -0.3, 0.05, SMACK_AT,
            SMACK_STRUCK_FLEX + SMACK_TRAILS, 0.3, 20.6, 0.48, 0.0, 22.8, 0.34, 0.0, 25.0, 0.4, 0.0, 28.0, 0.3, 0.0,
            34.0, 0.1, 0.0 };
    private static final double[] SMACK_REACH = { 11.0, 0.0, 0.0, 15.2, -0.25, 0.0, SMACK_AT, 0.0, 0.2, 20.6, 0.25, 0.0,
            23.5, 0.0, 0.0 };
    private static final double[] SMACK_DIP = { 11.0, 0.0, 0.0, 15.2, -0.08, 0.0, SMACK_AT, 0.0, 0.05, 20.6, 0.06, 0.0,
            23.5, -0.01, 0.0, 26.0, 0.0, 0.0 };
    // A grab: where its wrist is as it closes on its creature, and how high it has lifted it, its lean and its wrist
    // as it throws it.
    private static final double GRAB_HELD = -1.5;
    private static final double THROW_LIFT = 4.0;
    private static final double THROW_LEAN = 0.3632;
    private static final double THROW_FLEX = 0.2011;
    // A grab in keys: how high it lifts its creature (a yank, then heavier), leaning back and cocking its wrist to wind
    // up, and throwing it: leaning over hard, its wrist whipping through, past where it stops and back.
    private static final double[] GRAB_LIFT = { CATCH_AT, 0.0, 0.0, 14.4, 0.25, 0.55, 16.6, 1.6, 0.5, 21.0, 3.0, 0.18,
            27.5, 3.85, 0.06, 30.6, 4.12, 0.0, 31.4, 4.1, -0.02, THROW_AT, THROW_LIFT, -0.12, 35.0, 4.28, 0.0, 37.5,
            4.0, 0.0 };
    private static final double[] GRAB_LEAN = { 13.4, 0.0, 0.0, 21.0, -0.2, -0.03, 27.5, -0.33, -0.025, 30.6, -0.46,
            0.0, 31.3, -0.44, 0.08, THROW_AT, THROW_LEAN, 0.62, 34.7, 0.8, 0.1, 35.4, 0.82, 0.0, 37.6, 0.54, 0.0, 39.8,
            0.6, 0.0, 42.0, 0.57, 0.0 };
    private static final double[] GRAB_FLEX = { 13.4, 0.0, 0.0, 21.0, -0.12, 0.0, 27.5, -0.2, 0.0, 30.6, -0.36, 0.0,
            31.3, -0.38, 0.0, THROW_AT, THROW_FLEX + THROW_TRAILS, 0.55, 34.8, 0.62, 0.0, 36.8, 0.4, 0.0, 39.0, 0.47,
            0.0, 42.0, 0.45, 0.0 };
    // A middle finger: where its wrist is as its fist breaks out of the ground and once its finger stands up.
    private static final double BURST_LENGTH = -3.294;
    private static final double FINGER_STANDS = 2.4;
    // A middle finger in keys: shooting up fast, far past where it stands and springing back onto it; turning a little
    // as it shoots up and cockily this way and that later on; and leaning its hand back a little between jabs.
    private static final double[] FINGER_RISE = { 3.55, BURIED, 0.0, BURST_AT, BURST_LENGTH, 9.0, 5.95, 4.35, 0.0, 7.95,
            1.85, 0.0, 9.9, 2.62, 0.0, UP_AT, FINGER_STANDS, 0.0 };
    private static final double[] FINGER_TILT = { 3.55, -0.12, 0.0, BURST_AT, 0.0, 0.3, 6.0, 0.24, 0.0, 8.0, -0.1, 0.0,
            9.9, 0.05, 0.0, UP_AT, 0.0, 0.0, 13.4, 0.0, 0.0, 17.6, 0.26, 0.0, 23.2, -0.14, 0.0, 29.4, 0.24, 0.0, 36.0,
            0.1, 0.0, 41.0, 0.0, 0.0 };
    private static final double[] FINGER_COCK = { UP_AT, 0.0, 0.0, 13.6, 0.1, 0.0, 36.0, 0.1, 0.0, 41.0, 0.0, 0.0 };
    // One jab of the middle finger, around its thrust at 0: pulled back a little, held, thrust at its creature, and a
    // little wobble after. When the jabs thrust and how hard, and when its fist closes round its finger.
    private static final double[] JAB = { -2.4, 0.0, 0.0, -1.1, -0.3, 0.0, -0.7, -0.28, 0.15, 0.0, 1.0, 0.0, 0.9, -0.25,
            0.0, 1.9, 0.08, 0.0, 3.0, 0.0, 0.0 };
    private static final double[] JAB_AT = { 15.2, 21.0, 27.2 };
    private static final double[] JAB_HARD = { 1.0, 0.85, 1.2 };
    private static final double FIST_FROM = 4.4;
    // A slam: how high it stands before it winds up, where its wrist is and how far the hand is bent over as it lands
    // flat, and how fast each goes then.
    private static final double SLAM_HIGH = 4.0;
    private static final double SLAM_LANDS = 0.95;
    private static final double SLAM_FLAT = 1.57;
    private static final double SLAM_DROP_SPEED = -1.5;
    private static final double SLAM_BEND_SPEED = 0.85;
    // A slam in keys: rising a little more as it cocks its wrist back, holding it, and dropping faster and faster;
    // pressing down and peeling its hand off the ground afterwards, its wrist first; and leaning back a hair to wind
    // up.
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
    // How hard a landing blow springs back: how fast it swings and how soon it calms down.
    private static final double RECOIL = 7.5;
    private static final double RECOIL_CALM = 0.32;
    // A pound: how far along its hand the flat side of the fist is from the wrist, how far out to its side, and how
    // close and how far off it can strike.
    private static final double FIST_ALONG = 2.0;
    private static final double FIST_SIDE = 1.65;
    private static final double POUND_NEAR = 3.0;
    private static final double POUND_FAR = 9.0;
    // A pound's blow: how long before it lands it starts to lift its fist and over how long, how long it brings it
    // down, and how fast it goes as it lands (per whole blow: 0 would stop, 3 at most).
    private static final double POUND_LIFT = 8.6;
    private static final double POUND_LIFTING = 5.4;
    private static final double POUND_DOWN = 2.3;
    private static final double POUND_SNAP = 2.4;
    /** Where a grab holds its creature: in front of its palm, halfway along its fingers, at scale 1. */
    public static final Vec3 GRIP = new Vec3(0.0, 3.05, 1.25);
    /** The middle of the palm, on its face, at scale 1. */
    public static final Vec3 PALM = new Vec3(0.0, 1.55, 0.55);
    /** The flat side of the fist of a pound, where it strikes, at scale 1. */
    public static final Vec3 FIST = new Vec3(FIST_SIDE, FIST_ALONG, 0.45);

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

    HandPose() {
    }

    /** The move of a hand of this variant. */
    public static int move(int variant) {
        return Math.floorMod(variant, MOVES);
    }

    /** 1 for a smack swept round the usual way, -1 for one swept the other way round. */
    public static double side(int variant) {
        return variant >= MOVES ? -1.0 : 1.0;
    }

    /** How many ticks a hand of this variant lasts. */
    public static int life(int variant) {
        int move = move(variant);
        return move == AXE ? HandDuo.LIFE : ticks(LIFE[move]);
    }

    /** The tick a hand of this variant starts to sink back into the ground (an axe pair: its axe starts to break). */
    public static int sinks(int variant) {
        int move = move(variant);
        return move == AXE ? HandDuo.AXE_BREAKS : ticks(SINK[move]);
    }

    /**
     * The variant of an axe pair laid out facing {@code way} (flat): AXE + MOVES * (1 + k), k the way in sixteenths of
     * a turn.
     */
    public static int axeVariant(Vec3 way) {
        double theta = Math.atan2(way.x, way.z);
        int k = Math.floorMod((int) Math.round(theta / (Math.PI / 8.0)), 16);
        return AXE + MOVES * (1 + k);
    }

    /** The flat way (one long) an axe pair of this variant is laid out facing. */
    public static Vec3 axeWay(int variant) {
        int k = Math.floorMod(variant / MOVES - 1, 16);
        double theta = k * Math.PI / 8.0;
        return new Vec3(Math.sin(theta), 0.0, Math.cos(theta));
    }

    /** A time of the moves in beats, in ticks. */
    private static int ticks(double beats) {
        return (int) Math.round(beats * SLOW);
    }

    /**
     * True while a hand of this variant, {@code t} ticks after it was called, strikes or holds on: meanwhile it goes
     * after no creature, so a hand's turning glides to a stop and an axe pair's spot stays put right away; it only
     * turns after its creature before and after.
     */
    public static boolean locked(int variant, double t) {
        double beat = t / SLOW;
        return switch (move(variant)) {
            case SMACK -> beat >= 17.0;
            case GRAB -> beat >= CATCH_BEAT - 2.0;
            case SLAM -> beat >= 20.0;
            case AXE -> t >= HandDuo.LOCKED_FROM;
            case POUND -> {
                for (double hit : POUND_BEATS) {
                    if (beat >= hit - 4.0 && beat <= hit + 3.0) {
                        yield true;
                    }
                }
                yield false;
            }
            default -> false;
        };
    }

    /** How far from its base a hand of this move comes up, next to what it reaches for, at scale 1. */
    public static double spot(int move) {
        return move >= 0 && move < SPOT.length ? SPOT[move] : 0.0;
    }

    /**
     * How a hand stands {@code t} ticks after it was called.
     *
     * @param variant its move (see {@link #move}), with the way round it sweeps
     * @param reach   how far off what it reaches for is, flat along the ground, in blocks at scale 1
     */
    public static HandPose at(int variant, double t, double reach) {
        int move = move(variant);
        if (move == AXE) {
            // An axe pair: no hand of its own comes up out of the ground (see HandDuo), it stays buried.
            HandPose buried = new HandPose();
            buried.length = BURIED;
            return buried;
        }
        double side = side(variant);
        // Everything below is laid out in beats.
        double beat = t / SLOW;
        HandPose pose = moving(move, side, beat, reach);
        // Its blows land exactly where and when the server strikes them: near each, it is drawn softly onto it.
        for (double blow : blows(move)) {
            double near = Ease.bump((beat - blow) / ONTO);
            if (near > 0.0) {
                pose.onto(near, moving(move, side, blow, reach), struck(move, side, blow, reach));
            }
        }
        return pose;
    }

    /** The beats the blows of a move land on, as the server strikes them. */
    private static double[] blows(int move) {
        return switch (move) {
            case SMACK -> new double[] { SMACK_AT };
            case GRAB -> new double[] { CATCH_AT, THROW_AT };
            case FINGER -> new double[] { BURST_AT, UP_AT };
            case SLAM -> new double[] { SLAM_AT };
            default -> POUND_AT;
        };
    }

    /** How its forearm and wrist stand as a blow of its move lands on the beat {@code blow}. */
    private static HandPose struck(int move, double side, double blow, double reach) {
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

    /** Draws its forearm and wrist this far ({@code near}, 0 to 1) from where they are {@code there} onto a blow. */
    private void onto(double near, HandPose there, HandPose blow) {
        this.lean += near * (blow.lean - there.lean);
        this.length += near * (blow.length - there.length);
        this.twist += near * (blow.twist - there.twist);
        this.flex += near * (blow.flex - there.flex);
        this.sweep += near * (blow.sweep - there.sweep);
    }

    /**
     * The move as it is laid out, with its wrist and fingers trailing behind as a real hand's would: each is carried
     * on as by a spring after where its forearm and hand pointed a moment ago, so it drags behind as they swing and
     * whips on past where they stop.
     */
    private static HandPose moving(int move, double side, double t, double reach) {
        HandPose pose = shaped(move, side, t, reach);
        Vec3 ahead = new Vec3(0.0, 0.0, 1.0);
        Vec3[] now = pose.axes(ahead);
        Vec3 arm = Vec3.ZERO;
        Vec3 hand = Vec3.ZERO;
        for (int k = 1; k <= TAPS; k++) {
            Vec3[] then = shaped(move, side, t - k * TAP, reach).axes(ahead);
            arm = arm.add(then[0].scale(WRIST_WEIGHTS[k]));
            hand = hand.add(then[3].scale(FINGER_WEIGHTS[k]));
        }
        // The wrist: how far behind the forearm the hand still points, towards its palm or its back.
        double lag = Math.atan2(arm.dot(now[1]), arm.dot(now[0]));
        pose.flex += Ease.soft(WRIST_DRAG * lag, WRIST_DRAG_MOST);
        // The fingers: bent towards the palm as the hand swings back first, flung back as it swings palm first.
        double flung = Ease.soft(FINGER_DRAG * Math.atan2(hand.dot(now[4]), hand.dot(now[3])), FINGER_DRAG_MOST);
        for (int k = 0; k < 5; k++) {
            if (move == FINGER && k == 1) {
                // A middle finger held up stays stiff.
                continue;
            }
            double trail = k < 4 ? TRAIL[k] : 0.5;
            double curl = pose.curl[k];
            pose.curl[k] = curl + flung * trail * (1.0 - curl) * (0.3 + curl);
            pose.hook[k] *= 1.0 + 0.8 * flung * trail;
        }
        return pose;
    }

    /** The move as it is laid out, beat by beat. */
    private static HandPose shaped(int move, double side, double t, double reach) {
        HandPose pose = new HandPose();
        pose.length = BURIED;
        switch (move) {
            case SMACK -> pose.smack(t, side, reach);
            case GRAB -> pose.grab(t);
            case FINGER -> pose.finger(t);
            case SLAM -> pose.slam(t);
            default -> pose.pound(t, reach);
        }
        return pose;
    }

    /**
     * How much each look back counts, the way a spring of this swing and damping remembers a knock: its pull a moment
     * later. They add up to 1, so a hand at rest points where its forearm points.
     */
    private static double[] remembered(double swing, double damping) {
        double[] weights = new double[TAPS + 1];
        double turn = swing * Math.sqrt(1.0 - damping * damping);
        double sum = 0.0;
        for (int k = 1; k <= TAPS; k++) {
            weights[k] = Math.exp(-damping * swing * k * TAP) * Math.sin(turn * k * TAP);
            sum += weights[k];
        }
        for (int k = 1; k <= TAPS; k++) {
            weights[k] /= sum;
        }
        return weights;
    }

    // ---- The moves (t in beats) ----

    /** It rises up low beside its creature, winds up and holds a beat, and sweeps its open palm through it. */
    private void smack(double t, double side, double reach) {
        // Out of the ground steeper, leaning over low above its creature as it comes.
        this.length = rise(t, smackOut(reach), 1.0, 0.6) + Ease.keys(t, SMACK_REACH);
        this.lean = SMACK_LEAN - 0.32 * (1.0 - Ease.spring(t - RISE, 1.1, 0.7)) + Ease.keys(t, SMACK_DIP);
        this.twist = -side * Math.PI * 0.5 + side * corkscrew(t, 0.6);
        this.sweep = side * Ease.keys(t, SMACK_SWEEP);
        this.flex = Ease.keys(t, SMACK_FLEX);
        unfurl(t, 0.1, 0.14, 0.15, 1.0);
        // It draws its fingers together and stiffens them for the blow, and lets them go loose after it.
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

    /** How far a smack's forearm comes out for its palm to sweep through what it reaches for. */
    private static double smackOut(double reach) {
        return Mth.clamp(reach / Math.sin(SMACK_LEAN) - PALM.y, 2.0, 8.0);
    }

    /**
     * It bursts up in front of its creature, fingers up round it like a claw, closes them on it from the little finger
     * and squeezes; lifts it, leans back and holds a beat, and throws it, letting go from the index finger.
     */
    private void grab(double t) {
        this.length = rise(t, GRAB_HELD, 1.15, 0.55) + Ease.keys(t, GRAB_LIFT);
        this.lean = Ease.keys(t, GRAB_LEAN);
        this.flex = Ease.keys(t, GRAB_FLEX);
        // What it holds struggles: the hand shakes a little as it carries it.
        double struggle = window(t, 13.4, 15.5, 28.5, 30.6);
        this.twist = corkscrew(t, 0.5) + struggle * 0.03 * Math.sin(t * 2.3);
        this.lean += struggle * 0.012 * Math.sin(t * 3.1 + 0.8);
        unfurl(t, 0.06, 0.3, 0.1, 1.0);
        // The claw opens a little wider, then closes on its creature and squeezes it.
        cascade(t, 8.8, 1.4, false, 0.0, 0.12, 0.05);
        this.spread = Mth.lerp(Ease.smoother((t - 8.8) / 1.4), this.spread, 1.06);
        cascade(t, 10.2, 1.4, true, 0.72, 0.22, 0.72);
        this.spread = Mth.lerp(Ease.smoother((t - 10.2) / 2.8), this.spread, 0.05);
        double squeeze = 0.06 * Ease.bump((t - 13.9) / 1.2);
        for (int k = 0; k < 4; k++) {
            this.curl[k] += squeeze;
        }
        // It lets go as it throws, flicking its fingers wide, and lets them hang loose after.
        cascade(t, 32.5, 1.2, false, 0.08, 0.1, 0.12);
        this.spread = Mth.lerp(Ease.smoother((t - 32.5) / 1.5), this.spread, 0.95);
        cascade(t, 36.5, 3.0, false, 0.16, 0.22, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - 36.5) / 3.5), this.spread, 0.7);
        fidget(t, 0.5 * window(t, 15.0, 17.0, 28.0, 30.0) + window(t, 36.0, 39.0, 42.0, 44.0), true);
        breathe(t, 0.6 * window(t, 15.0, 18.0, 27.0, 29.5) + window(t, 37.0, 40.0, 42.0, 44.0));
        this.sink(t, SINK[GRAB], LIFE[GRAB], 1.0, true);
    }

    /**
     * It shoots up with the back of the hand to its creature, its middle finger leading and its fist snapping shut
     * round it, far past where it stays and springing back onto it; then gives its creature the middle finger, jabbing
     * it at it three times, cocky, and sinks back with its finger still up.
     */
    private void finger(double t) {
        this.length = Ease.keys(t, FINGER_RISE);
        this.twist = Math.PI + Ease.keys(t, FINGER_TILT);
        this.flex = Ease.keys(t, FINGER_COCK);
        for (int k = 0; k < JAB_AT.length; k++) {
            // Towards its creature: the forearm leans at it, the hand bends to its back (the palm faces away).
            double jab = JAB_HARD[k] * Ease.keys(t - JAB_AT[k], JAB);
            this.lean += 0.22 * jab;
            this.flex -= 0.42 * jab;
            this.length += 0.3 * jab;
        }
        for (int k = 0; k < 4; k++) {
            if (k == 1) {
                // The middle finger stands straight up.
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

    /**
     * It comes straight up, rises a little more as it cocks its wrist back, holds it a beat, and slaps its palm down
     * flat, faster and faster: it lands hard, springs back and trembles, presses its creature flat, and peels off.
     */
    private void slam(double t) {
        boolean landed = t >= SLAM_AT;
        double since = t - SLAM_AT;
        this.length = rise(t, SLAM_HIGH, 0.95, 0.6) + (landed
                ? Ease.keys(t, SLAM_PRESS) + Ease.recoil(since, SLAM_DROP_SPEED, RECOIL, RECOIL_CALM)
                : Ease.keys(t, SLAM_DROP));
        this.flex = landed ? Ease.keys(t, SLAM_UNBEND) + Ease.recoil(since, SLAM_BEND_SPEED, RECOIL, RECOIL_CALM)
                : Ease.keys(t, SLAM_BEND);
        this.lean = Ease.keys(t, SLAM_LEAN);
        // Pressing down, it grinds its creature into the ground.
        double grind = window(t, 26.0, 28.0, 37.5, 39.5);
        this.twist = corkscrew(t, -0.55) + grind * 0.018 * Math.sin(t * 2.6);
        this.length += grind * 0.02 * Math.sin(t * 3.7 + 0.5);
        unfurl(t, 0.08, 0.12, 0.1, 1.0);
        // It opens its hand wide to slap, and curls its fingers loosely as it peels off.
        cascade(t, 15.0, 2.0, false, 0.0, 0.04, 0.05);
        this.spread = Mth.lerp(Ease.smoother((t - 15.0) / 2.5), this.spread, 1.05);
        cascade(t, 41.0, 2.4, true, 0.28, 0.3, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - 41.0) / 3.0), this.spread, 0.7);
        fidget(t, window(t, 9.0, 11.5, 14.0, 15.5), true);
        breathe(t, window(t, 9.5, 12.5, 14.0, 16.5) + 0.3 * window(t, 27.5, 30.0, 36.5, 39.0));
        this.sink(t, SINK[SLAM], LIFE[SLAM], -1.0, true);
    }

    /**
     * It comes up, makes a fist from the little finger, and pounds the flat of it down three times on what is nearest:
     * each time it lifts it, holds it up a beat, and brings it down faster and faster, landing hard and bouncing.
     */
    private void pound(double t, double reach) {
        double r = Mth.clamp(reach, POUND_NEAR, POUND_FAR);
        double downLean = poundLean(r);
        double downLength = poundLength(r);
        // Up to strike, and at rest between two blows: not all the way up.
        double upLean = downLean * 0.5;
        double upLength = downLength + 1.4;
        double restLean = downLean * 0.8;
        double restLength = downLength + 0.45;
        this.twist = Math.PI * 0.5 + corkscrew(t, 0.5);
        this.length = rise(t, 3.6, 0.95, 0.6);
        this.lean = 0.1;
        for (double hit : POUND_AT) {
            double up = Ease.smoother((t - hit + POUND_LIFT) / POUND_LIFTING);
            // Held up a beat, it cocks its fist a little higher still.
            double cock = Ease.keys(t - hit, -4.2, 0.0, 0.0, -2.6, 1.0, 0.0, -0.4, 0.0, 0.0);
            this.lean = Mth.lerp(up, this.lean, upLean) - 0.06 * cock;
            this.length = Mth.lerp(up, this.length, upLength) + 0.3 * cock;
            // Down, faster and faster, landing at full speed: it gives, springs back and trembles.
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
        // Open as it comes up, it clenches its fist: the little finger first, the thumb wrapping over them last.
        unfurl(t, 0.2, 0.22, 0.25, 0.6);
        cascade(t, 9.2, 1.5, true, 1.0, 0.0, 1.0);
        this.spread = Mth.lerp(Ease.smoother((t - 9.2) / 2.8), this.spread, 0.0);
        fidget(t, window(t, 11.0, 13.0, 14.0, 16.0) + window(t, 56.5, 58.5, 60.0, 62.0), true);
        breathe(t, window(t, 10.0, 12.5, 13.5, 15.5) + window(t, 56.5, 58.5, 60.0, 62.0));
        this.sink(t, SINK[POUND], LIFE[POUND], 1.0, true);
    }

    /** How far a pound's forearm leans to strike the ground {@code r} blocks off with the flat of its fist. */
    private static double poundLean(double r) {
        double h = 0.1;
        return Math.acos(Mth.clamp(FIST_SIDE / Math.sqrt(r * r + h * h), -1.0, 1.0)) - Math.atan2(h, r);
    }

    /** How far a pound's forearm comes out of the ground to strike the ground {@code r} blocks off. */
    private static double poundLength(double r) {
        double lean = poundLean(r);
        return (r - FIST_SIDE * Math.cos(lean)) / Math.max(0.2, Math.sin(lean)) - FIST_ALONG;
    }

    // ---- How it moves ----

    /** How far out of the ground it has burst: from under it at {@link #RISE}, springing up past {@code to} onto it. */
    private static double rise(double t, double to, double speed, double damping) {
        return Mth.lerp(Ease.spring(t - RISE, speed, damping), BURIED, to);
    }

    /** How far it is still turned about its forearm as it corkscrews up out of the ground: {@code turn}, unwinding. */
    private static double corkscrew(double t, double turn) {
        return turn * (1.0 - Ease.spring(t - RISE, 1.25, 0.55));
    }

    /** 0 before {@code in}, softly up to 1 by {@code full}, and softly back to 0 from {@code fade} to {@code gone}. */
    private static double window(double t, double in, double full, double fade, double gone) {
        return Ease.smoother((t - in) / (full - in)) * (1.0 - Ease.smoother((t - fade) / (gone - fade)));
    }

    /**
     * Its fingers unfurling from a loose fist as it comes up out of the ground, the index finger first and the little
     * finger last, into a hand curled and hooked this far, its thumb that far and its fingers spread that wide: they
     * splay a little wider on the way and settle back.
     */
    private void unfurl(double t, double curl, double hook, double thumb, double spread) {
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
    private void cascade(double t, double from, double beats, boolean closing, double curl, double hook,
            double thumb) {
        for (int k = 0; k < 5; k++) {
            double order = k == 4 ? (closing ? 4.0 : 0.5) : closing ? 3 - k : k;
            double u = Ease.smoother((t - from - order * STAGGER) / beats);
            this.curl[k] = Mth.lerp(u, this.curl[k], k == 4 ? thumb : curl);
            this.hook[k] = Mth.lerp(u, this.hook[k], hook);
        }
    }

    /** A slow breath of a sway, {@code amount} of it (0 still, 1 full), each part at a pace of its own. */
    private void breathe(double t, double amount) {
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
    private void fidget(double t, double amount, boolean middle) {
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
    private void sink(double t, double from, double until, double side, boolean loosen) {
        double lift = SINK_LIFT * Ease.keys(t, from - 1.5, 0.0, 0.0, from + 1.2, 1.0, 0.0);
        double down = Ease.smoother((t - from - 0.8) / (until - from - 0.8));
        this.length = Mth.lerp(down, this.length + lift, BURIED);
        this.twist += side * 0.3 * down;
        if (loosen) {
            cascade(t, from, 3.0, true, 0.45, 0.35, 0.4);
            this.spread = Mth.lerp(Ease.smoother((t - from) / 4.0), this.spread, 0.35);
        }
    }

    // ---- Where it is in the world ----

    /**
     * The hand out in the world: where its wrist is, the way its forearm runs (out of the ground up to the wrist), and
     * the hand's right, up (to its fingers) and forward (out of its palm), one long each.
     *
     * @param base  where it comes up out of the ground
     * @param reach the flat way from its base to what it reaches for (need not be one long)
     * @param scale its size (1: the size it is made at)
     */
    public record Place(Vec3 wrist, Vec3 arm, Vec3 armForward, Vec3 right, Vec3 up, Vec3 forward, double scale) {
        /** A point of the hand, given at scale 1 to its right, up and ahead from its wrist, out in the world. */
        public Vec3 at(Vec3 local) {
            return this.wrist.add(this.right.scale(local.x * this.scale)).add(this.up.scale(local.y * this.scale))
                    .add(this.forward.scale(local.z * this.scale));
        }
    }

    /** Where this pose puts the hand, coming up out of the ground at {@code base} and reaching along {@code reach}. */
    public Place place(Vec3 base, Vec3 reach, double scale) {
        Vec3 flat = new Vec3(reach.x, 0.0, reach.z);
        flat = flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3[] axes = this.axes(flat);
        return new Place(base.add(axes[0].scale(this.length * scale)), axes[0], axes[1], axes[2], axes[3], axes[4],
                scale);
    }

    /**
     * The ways of its forearm, the palm side of its forearm, and its hand's right, up and forward, one long each, for a
     * hand reaching along the flat way {@code flat} (one long).
     */
    private Vec3[] axes(Vec3 flat) {
        Vec3 f = Vectors.spin(flat, Vectors.UP, -this.sweep);
        Vec3 s = f.cross(Vectors.UP).normalize();
        double cos = Math.cos(this.lean);
        double sin = Math.sin(this.lean);
        Vec3 arm = Vectors.UP.scale(cos).add(f.scale(sin));
        Vec3 face = f.scale(cos).subtract(Vectors.UP.scale(sin));
        // Turned about the forearm: the palm swings round from facing what it reaches for.
        double tc = Math.cos(this.twist);
        double ts = Math.sin(this.twist);
        Vec3 right = s.scale(tc).add(face.scale(ts));
        Vec3 palm = face.scale(tc).subtract(s.scale(ts));
        // The wrist bends the hand over towards its palm.
        double fc = Math.cos(this.flex);
        double fs = Math.sin(this.flex);
        Vec3 up = arm.scale(fc).add(palm.scale(fs));
        Vec3 forward = palm.scale(fc).subtract(arm.scale(fs));
        return new Vec3[] { arm, palm, right, up, forward };
    }
}
