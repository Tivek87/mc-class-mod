package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The script of the pair of hands with the axe (see {@link HandDuo}): its timeline, the axe's measures, where
 * everything is laid out, and the keys the hands and their fingers move through.
 */
abstract class HandDuoScript extends HandDuoMotion {
    // The timeline, in ticks since the pair was called.
    /** The ring's light reaches the spot: the two side portals burst open. */
    public static final int ARRIVES = HandPose.ARRIVES;
    /** Both hands are all the way out of their portals. */
    public static final int OUT = ARRIVES + 14;
    /** The right hand's fingers snap: the click. */
    public static final int SNAP = OUT + 26;
    /** The left hand's OK sign is made. */
    public static final int OK = SNAP + 10;
    /** The axe's portal bursts open. */
    public static final int AXE_OPENS = OK + 42;
    /** Both hands close on the haft. */
    public static final int GRAB = AXE_OPENS + 18;
    /** The axe's head is out of its portal, which snaps shut. */
    public static final int AXE_FREE = GRAB + 8;
    /** From here on the spot the axe strikes stays put: the raise starts. */
    public static final int LOCKED_FROM = AXE_FREE + 4;
    /** The axe is up at the top of its swing. */
    public static final int RAISED = LOCKED_FROM + 12;
    /** The blade bites into the ground. */
    public static final int IMPACT = RAISED + 8;
    /** The hands let go of the haft. */
    public static final int RELEASE = IMPACT + 13;
    /** Both thumbs are up. */
    public static final int THUMBS = RELEASE + 12;
    /** The hands start to pull back into their portals. */
    public static final int RETRACT = THUMBS + 16;
    /** The hands are gone and the side portals shut. */
    public static final int HANDS_GONE = RETRACT + 12;
    /** The axe left in the ground starts to break into solid pieces. */
    public static final int AXE_BREAKS = HANDS_GONE + 6;
    /** Everything is gone. */
    public static final int LIFE = AXE_BREAKS + 16;

    // The axe at scale 1, along its haft from the end of its pommel (0) to the top of its head (AXE_LENGTH).
    /** How long the axe is, from the end of its pommel to the top of its head. */
    public static final double AXE_LENGTH = 14.0;
    /** How thick its haft is (its radius). */
    public static final double HAFT_RADIUS = 0.42;
    /** Where the LEFT hand's fist holds it, along the haft. */
    public static final double GRIP_LOW = 2.4;
    /** Where the RIGHT hand's fist holds it, along the haft (a fist is about 3.2 wide). */
    public static final double GRIP_HIGH = 6.2;
    /** Where the middle of the blade is along the haft. */
    public static final double HEAD_AT = 11.4;
    /** Half the length of the blade's edge, along the haft. */
    public static final double BLADE_HALF = 2.3;
    /** How far the middle of the edge stands out from the haft's axis. */
    public static final double EDGE_OUT = 3.2;
    /** Where a haft held in a closed fist runs, at scale 1 in the hand's own terms (along its x through here). */
    public static final Vec3 FIST_HOLE = new Vec3(0.0, 2.95, 1.05);
    /** Where on the right hand (at scale 1, for HandPose.Place.at) the snap flashes: between thumb and middle tip. */
    public static final Vec3 SNAP_AT = new Vec3(-0.63, 2.97, 2.35);
    /**
     * Where on the left hand (at scale 1, for HandPose.Place.at) the ring of its OK sign is: the middle of the ring its
     * thumb and index finger make. Its x is to its thumb's side, as the left hand's place has its right to its thumb.
     */
    public static final Vec3 OK_AT = new Vec3(1.35, 3.14, 1.06);
    /** How far the spot the axe strikes may be from where the pair was called, at scale 1 (aim is clamped to it). */
    public static final double REACH = 3.0;

    // The fists sit round the middle of the two grips; how far that is from the middle of the blade along the haft.
    static final double GRIPS = (GRIP_LOW + GRIP_HIGH) * 0.5;
    private static final double LEVER = HEAD_AT - GRIPS;
    /** How deep the middle of the edge bites into the ground, at scale 1. */
    private static final double BITE = 0.6;
    /** How long the axe takes to break into pieces, in ticks. */
    static final double BREAK_TICKS = 14.0;

    // The layout at scale 1, in the pair's own terms: x to the caster's right, y up, z away from him (towards and past
    // the creature). The right hand works on his left (x below 0), the left hand on his right, like the hands of a
    // giant facing him; what is given for the right hand is mirrored for the left.
    /** The right hand's portal: beyond the creature on the caster's left. */
    static final Vec3 RIGHT_PORTAL = new Vec3(-7.8, 6.0, 4.5);
    static final double SIDE_RADIUS = 2.5;
    /** The axe's portal: beyond the hands, facing the caster. */
    static final Vec3 AXE_PORTAL = new Vec3(0.0, 5.8, 12.9);
    static final double AXE_RADIUS = 3.9;
    /** How far the axe has slid out of its portal (from the end of its pommel) when the hands close on it. */
    static final double SLID = 11.2;
    /** How far it is out once its head is free, and when the draw comes to rest. */
    static final double DRAWN = AXE_LENGTH + 0.6;
    static final double DRAWN_REST = DRAWN + 0.3;

    // Where the hands like to hold the axe as it strikes (the middle of the grips): straight beyond where the pair was
    // called, and how much of the way the spot it strikes is off that they follow it (to the side, the axe rolls about
    // its haft so its edge reaches the rest of the way).
    private static final Vec3 HOME = new Vec3(0.0, 7.0, 4.3);
    private static final double FOLLOW_SIDE = 0.35;
    private static final double FOLLOW_AWAY = 0.45;
    // Where the middle of the grips is at the top of the swing (x follows the strike as above), how far the axe leans
    // back there (radians up from level, head away from the caster), and how far it dips before the heave.
    private static final double RAISE_Y = 7.1;
    private static final double RAISE_Z = 3.0;
    static final double RAISE_TILT = 0.66;
    static final double DIP = 0.85;
    static final double DIP_TILT = -0.15;
    // When the chop starts (after a beat hanging at the top) and how much of the chop the hands take to come down.
    static final double CHOP_FROM = RAISED + 1.5;
    static final double HANDS_DOWN = 0.75;
    /** How much of a fist's turn with the haft its wrist takes, twisting, so its forearm need not take all of it. */
    static final double WRIST_SHARE = 0.5;
    /**
     * How far off the haft (along its palm, at scale 1) a hand stays as it comes down on it until its fingers start to
     * close.
     */
    static final double HOLD_CLEAR = 1.2;
    /**
     * Letting go of the haft, at scale 1: how far a hand drops off it (away from its palm) and then slides back off it
     * (along its fingers), and how many ticks after letting go it starts to leave the haft for its own way up.
     */
    static final double DROP_OFF = 2.2;
    static final double SLIDE_OFF = 2.4;
    static final double LET_GO = 2.5;

    // The hands' poses in the air (right hand; the left mirrors them): where the wrist is, which way the fingers point
    // and which way the palm faces. Not one long: they are straightened out where they are used.
    /** Hovering in front of its portal after coming out: palm down, fingers towards the caster and in. */
    static final Vec3 HOVER = new Vec3(-4.5, 6.3, 1.4);
    private static final Vec3 HOVER_UP = new Vec3(0.22, -0.15, -1.0);
    private static final Vec3 HOVER_PALM = new Vec3(0.05, -1.0, 0.1);
    /** Raised to snap: fingers up and in, palm to the caster. */
    private static final Vec3 SNAPPING = new Vec3(-3.7, 8.3, 1.5);
    private static final Vec3 SNAPPING_UP = new Vec3(0.4, 1.0, -0.2);
    private static final Vec3 SNAPPING_PALM = new Vec3(0.25, 0.0, -1.0);
    /** Hanging relaxed while the other hand makes its sign. */
    private static final Vec3 WATCHING = new Vec3(-4.1, 7.0, 1.6);
    private static final Vec3 WATCHING_UP = new Vec3(0.3, 0.15, -1.0);
    private static final Vec3 WATCHING_PALM = new Vec3(0.15, -1.0, -0.1);
    /** Beside the middle, palm to the other hand, to roll round it. */
    private static final Vec3 ROLLING = new Vec3(-2.9, 6.9, 1.3);
    private static final Vec3 ROLLING_UP = new Vec3(0.0, 0.55, -1.0);
    private static final Vec3 ROLLING_PALM = new Vec3(1.0, 0.0, 0.0);
    /** Flung apart, palm up and fingers spread, as if conjuring. */
    private static final Vec3 CONJURING = new Vec3(-4.2, 7.9, 2.2);
    private static final Vec3 CONJURING_UP = new Vec3(0.3, 0.35, -1.0);
    private static final Vec3 CONJURING_PALM = new Vec3(0.05, 1.0, 0.35);
    /** Reaching over the haft sliding out: the right hand passes high over the left on its way to the far grip. */
    private static final Vec3 REACHING = new Vec3(-4.0, 9.5, 7.0);
    private static final Vec3 REACHING_UP = new Vec3(1.0, 0.15, 0.1);
    private static final Vec3 REACHING_PALM = new Vec3(0.15, -1.0, 0.0);
    private static final Vec3 LEFT_REACHING = new Vec3(3.8, 7.8, 3.4);
    private static final Vec3 LEFT_REACHING_UP = new Vec3(-1.0, 0.2, -0.45);
    private static final Vec3 LEFT_REACHING_PALM = new Vec3(-0.15, -1.0, 0.0);
    /** About where each hand closes on the haft (the grip itself is worked out from the axe). */
    private static final Vec3 GRABBING = new Vec3(-3.1, 7.9, 7.9);
    private static final Vec3 GRABBING_UP = new Vec3(1.0, 0.1, 0.0);
    private static final Vec3 GRABBING_PALM = new Vec3(0.1, -1.0, 0.0);
    private static final Vec3 LEFT_GRABBING = new Vec3(3.1, 7.9, 4.1);
    /**
     * About where each hand is once it has let go of the haft stuck in the ground: dropped off it and slid back off it
     * along its own forearm (see DROP_OFF), still turned as it held it.
     */
    private static final Vec3 LETTING_GO = new Vec3(-5.6, 3.4, 3.5);
    private static final Vec3 LETTING_GO_UP = new Vec3(1.0, -0.08, 0.06);
    private static final Vec3 LETTING_GO_PALM = new Vec3(0.1, 0.8, -0.59);
    private static final Vec3 LEFT_LETTING_GO = new Vec3(5.6, 5.6, 6.6);
    private static final Vec3 LEFT_LETTING_GO_UP = new Vec3(-1.0, -0.08, 0.06);
    private static final Vec3 LEFT_LETTING_GO_PALM = new Vec3(-0.1, 0.8, -0.59);
    /**
     * Rising clear of the haft, closing into a fist and turning to the caster (turned halfway from letting go to the
     * thumbs up; see RISING_TURN).
     */
    private static final Vec3 RISING = new Vec3(-5.0, 6.6, 2.4);
    private static final Vec3 LEFT_RISING = new Vec3(5.0, 8.4, 3.2);
    /**
     * The thumbs up: a fist, its knuckles to the caster and in, tipped up so the thumb (which grows out of the hand at
     * a slant) points straight up; worked out from where the painter's thumb points when it is straight.
     */
    private static final Vec3 THUMB_UP = new Vec3(-3.6, 9.0, 0.4);
    private static final Vec3 THUMB_UP_UP = new Vec3(0.246, 0.668, -0.702);
    private static final Vec3 THUMB_UP_PALM = new Vec3(0.790, 0.282, 0.545);
    /**
     * Which way the fingers point and the palm faces as the hand rises off the haft: halfway round from letting go to
     * the thumbs up, so it turns the whole way evenly.
     */
    private static final Vec3[] RISING_TURN = halfway(LETTING_GO_UP, LETTING_GO_PALM, THUMB_UP_UP, THUMB_UP_PALM);
    // The same for the left hand, from its own way of letting go to the mirror image of the thumbs up.
    private static final Vec3[] LEFT_RISING_TURN = halfway(LEFT_LETTING_GO_UP, LEFT_LETTING_GO_PALM,
            THUMB_UP_UP.multiply(-1.0, 1.0, 1.0), THUMB_UP_PALM.multiply(-1.0, 1.0, 1.0));

    // Rolling round each other: how far each hand swings out from its place, and how far round they go (radians).
    static final double ROLL_RADIUS = 1.4;
    static final double ROLL_TURNS = Math.PI * 3.3;
    static final double ROLL_FROM = OK + 21.0;
    static final double ROLL_TO = AXE_OPENS - 4.5;

    // The poses of the fingers (see DIGITS).
    /** Loosely curled, as a hand still in its portal. */
    private static final double[] BALLED = digits(0.55, 0.6, 0.62, 0.66, 0.4, 0.3, 0.3, 0.32, 0.35, 0.1, 0.0);
    /** Relaxed. */
    private static final double[] LOOSE = digits(0.2, 0.24, 0.28, 0.33, 0.3, 0.16, 0.18, 0.2, 0.24, 0.08, 0.45);
    /** Ready to snap: middle finger pressed to the thumb tip, index loosely out, ring and little finger curled. */
    private static final double[] SNAP_READY = digits(0.1, 0.595, 0.86, 0.9, 0.568, 0.08, 0.0, 0.25, 0.3, 0.0, 0.15);
    /** The middle finger slammed down into the palm. */
    private static final double[] SNAP_DOWN = with(SNAP_READY, 1, 0.95, 6, 0.1);
    /** And the thumb flicked out. */
    private static final double[] SNAPPED = digits(0.18, 0.95, 0.92, 0.94, 0.22, 0.1, 0.1, 0.28, 0.3, 0.0, 0.2);
    /** Middle, ring and little finger straight up and fanned (the left hand, as it makes its sign). */
    private static final double[] FANNED = digits(0.2, 0.03, 0.05, 0.09, 0.15, 0.16, -0.05, -0.04, 0.0, 0.08, 0.9);
    /** The OK sign: index and thumb curled to meet tip to tip in a ring. */
    private static final double[] OK_SIGN = digits(0.4, 0.03, 0.05, 0.09, 0.415, 0.28, -0.05, -0.04, 0.0, 0.0, 0.9);
    /** Half curled, for the crawling. */
    private static final double[] CRAWLING = digits(0.42, 0.45, 0.47, 0.5, 0.3, 0.25, 0.25, 0.25, 0.25, 0.1, 0.35);
    /** Spread wide open. */
    private static final double[] SPREAD_WIDE = digits(0.02, 0.0, 0.02, 0.05, 0.0, -0.08, -0.08, -0.08, -0.06, 0.0,
            1.0);
    /** Open, to grab. */
    private static final double[] OPEN = digits(0.14, 0.14, 0.17, 0.2, 0.08, 0.06, 0.06, 0.08, 0.1, 0.0, 0.55);
    /**
     * The same for the left hand: its thumb, which points up the haft at the right hand's little finger, kept in, its
     * tip bent back so it passes over the haft as the hand comes down on it.
     */
    private static final double[] LEFT_OPEN = with(OPEN, 4, 0.32, 9, -0.5);
    /** Closed round the haft, the thumb over the fingers: fitted to the painter's finger sizes round HAFT_RADIUS. */
    private static final double[] GRIPPING = digits(0.61, 0.695, 0.651, 0.483, 0.62, 0.158, 0.144, 0.158, 0.095, 0.05,
            0.0);
    /** Let go: opened just enough to come off the haft. */
    private static final double[] RELEASED = digits(0.3, 0.32, 0.34, 0.36, 0.34, 0.08, 0.08, 0.08, 0.08, 0.0, 0.2);
    /** A fist, the thumb over the fingers. */
    private static final double[] FIST = digits(1.0, 1.0, 1.0, 1.0, 0.55, 0.1, 0.1, 0.1, 0.1, 0.1, 0.0);
    /** A fist with the thumb straight up. */
    private static final double[] THUMB_STRAIGHT = digits(1.0, 1.0, 1.0, 1.0, 0.0, 0.1, 0.1, 0.1, 0.1, 0.0, 0.0);
    /** Trailing behind as the hand pulls back. */
    private static final double[] TRAILING = digits(0.16, 0.18, 0.2, 0.23, 0.1, 0.12, 0.12, 0.14, 0.16, 0.04, 0.3);

    // How far behind each finger (index, middle, ring, little, thumb) follows when the hand opens (index first) or
    // closes (little finger first), in ticks.
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

    // ---- The layout ----

    /**
     * Where the pair is laid out: where it was called, the flat way from the caster to it and his right (both one
     * long), its size, and the spot the axe strikes at scale 1 in the pair's terms (to his right, away from him), kept
     * within REACH.
     */
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

        /** A way given in the pair's terms, in the world. */
        Vec3 dir(Vec3 local) {
            return this.side.scale(local.x).add(0.0, local.y, 0.0).add(this.way.scale(local.z));
        }

        /** A point given at scale 1 in the pair's terms, in the world. */
        Vec3 point(Vec3 local) {
            return this.base.add(this.dir(local).scale(this.scale));
        }

        /** How far to the caster's right the hands hold the axe as it strikes: part of the way to the strike. */
        double across() {
            return this.aimSide * FOLLOW_SIDE;
        }

        /**
         * How far the axe is rolled about its haft as it strikes (radians, its edge towards the caster's right), so
         * its edge reaches the rest of the way to the strike.
         */
        double roll() {
            return Math.asin(Mth.clamp((this.aimSide - this.across()) / EDGE_OUT, -0.8, 0.8));
        }

        /** The middle of the blade's edge as it strikes, in the pair's terms. */
        Vec3 strike() {
            return new Vec3(this.across() + EDGE_OUT * Math.sin(this.roll()), -BITE, this.aimAway);
        }

        /**
         * Where the middle of the grips is as the blade strikes, in the pair's terms: as near as it can be to where
         * the hands like to hold it (followed a little towards the strike), the length of the lever from the edge.
         */
        Vec3 struck() {
            double edge = EDGE_OUT * Math.cos(this.roll());
            Vec3 strike = new Vec3(this.across(), -BITE, this.aimAway);
            Vec3 home = new Vec3(this.across(), HOME.y, HOME.z + this.aimAway * FOLLOW_AWAY);
            return strike.add(home.subtract(strike).normalize().scale(Math.sqrt(LEVER * LEVER + edge * edge)));
        }

        /** How steeply the haft slants down to the ground as it strikes (radians below level). */
        double lean() {
            Vec3 from = this.struck().subtract(new Vec3(this.across(), -BITE, this.aimAway));
            return Math.atan2(from.y, from.z) - Math.atan2(EDGE_OUT * Math.cos(this.roll()), LEVER);
        }

        /** Where the middle of the grips is at the top of the swing, in the pair's terms. */
        Vec3 raised() {
            return new Vec3(this.across(), RAISE_Y, RAISE_Z);
        }
    }
}
