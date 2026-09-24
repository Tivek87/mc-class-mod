package nl.tivek.multiversepowers.character.greenlantern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The pair of giant hands with the axe (the move {@link HandPose#AXE} of the Giant Hands), worked out alike by the
 * server (when and where it strikes, where its sounds and sparks go) and by every client (how it looks), from nothing
 * but where it was called, the way it is laid out, the spot its axe strikes and how long ago it was called.
 *
 * <p>Two portals of the ring's light burst open beyond the creature, one on either side of it, and a giant right hand
 * (with the ring) and left hand push out of them fingertips first, their fingers wriggling, a little corkscrewed, and
 * come to rest in front of them: the hands of an invisible giant standing behind the creature, facing the one who
 * called them. Their fingers ripple up and down like a pianist's warming up. The right hand rises, turns its palm to
 * him, presses its middle finger to its thumb and snaps its fingers, jolting with it; the left answers with the OK
 * sign and a proud little wobble. Then they glide together, palms to each other, and roll round each other like
 * winding wool, their fingers crawling, and fling apart palms up as if conjuring: a third portal bursts open beyond
 * them. A giant axe slides out of it pommel first; both hands reach for it, close on its haft one finger after another,
 * and draw it out like a sword from its sheath. They dip, heave it up and back, hang there a beat trembling with its
 * weight, and chop it down over the top onto the creature, the middle of its edge biting into the ground where the
 * creature stands. They leave it stuck there quivering, let go, rise and give him a thumbs up, then pull back into
 * their portals, which pop shut, and the axe left behind breaks into solid pieces.
 *
 * <p>Everything is laid out along the flat way from the caster to the creature ({@link HandPose#axeWay}): the portals
 * and all the hands do before the raise stay where they are while the spot the axe strikes follows the creature; from
 * {@link #LOCKED_FROM} on that spot stays put. Every value moves smoothly: nothing jumps, and nothing starts or stops
 * moving with a jolt.
 */
public final class HandDuo {
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
    private static final double GRIPS = (GRIP_LOW + GRIP_HIGH) * 0.5;
    private static final double LEVER = HEAD_AT - GRIPS;
    /** How deep the middle of the edge bites into the ground, at scale 1. */
    private static final double BITE = 0.6;
    /** How long the axe takes to break into pieces, in ticks. */
    private static final double BREAK_TICKS = 14.0;

    // The layout at scale 1, in the pair's own terms: x to the caster's right, y up, z away from him (towards and past
    // the creature). The right hand works on his left (x below 0), the left hand on his right, like the hands of a
    // giant facing him; what is given for the right hand is mirrored for the left.
    /** The right hand's portal: beyond the creature on the caster's left. */
    private static final Vec3 RIGHT_PORTAL = new Vec3(-7.8, 6.0, 4.5);
    private static final double SIDE_RADIUS = 2.5;
    /** The axe's portal: beyond the hands, facing the caster. */
    private static final Vec3 AXE_PORTAL = new Vec3(0.0, 5.8, 12.9);
    private static final double AXE_RADIUS = 3.9;
    /** How far the axe has slid out of its portal (from the end of its pommel) when the hands close on it. */
    private static final double SLID = 11.2;
    /** How far it is out once its head is free, and when the draw comes to rest. */
    private static final double DRAWN = AXE_LENGTH + 0.6;
    private static final double DRAWN_REST = DRAWN + 0.3;

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
    private static final double RAISE_TILT = 0.66;
    private static final double DIP = 0.85;
    private static final double DIP_TILT = -0.15;
    // When the chop starts (after a beat hanging at the top) and how much of the chop the hands take to come down.
    private static final double CHOP_FROM = RAISED + 1.5;
    private static final double HANDS_DOWN = 0.75;

    // The hands' poses in the air (right hand; the left mirrors them): where the wrist is, which way the fingers point
    // and which way the palm faces. Not one long: they are straightened out where they are used.
    /** Hovering in front of its portal after coming out: palm down, fingers towards the caster and in. */
    private static final Vec3 HOVER = new Vec3(-2.9, 6.3, 1.6);
    private static final Vec3 HOVER_UP = new Vec3(0.45, -0.15, -1.0);
    private static final Vec3 HOVER_PALM = new Vec3(0.05, -1.0, 0.1);
    /** Raised to snap: fingers up and in, palm to the caster. */
    private static final Vec3 SNAPPING = new Vec3(-2.7, 8.2, 1.6);
    private static final Vec3 SNAPPING_UP = new Vec3(0.55, 1.0, -0.2);
    private static final Vec3 SNAPPING_PALM = new Vec3(0.25, 0.0, -1.0);
    /** Hanging relaxed while the other hand makes its sign. */
    private static final Vec3 WATCHING = new Vec3(-3.1, 7.0, 1.8);
    private static final Vec3 WATCHING_UP = new Vec3(0.55, 0.15, -1.0);
    private static final Vec3 WATCHING_PALM = new Vec3(0.15, -1.0, -0.1);
    /** Beside the middle, palm to the other hand, to roll round it. */
    private static final Vec3 ROLLING = new Vec3(-1.8, 6.6, 1.4);
    private static final Vec3 ROLLING_UP = new Vec3(0.7, 0.35, -1.0);
    private static final Vec3 ROLLING_PALM = new Vec3(1.0, 0.0, 0.15);
    /** Flung apart, palm up and fingers spread, as if conjuring. */
    private static final Vec3 CONJURING = new Vec3(-4.2, 7.9, 2.2);
    private static final Vec3 CONJURING_UP = new Vec3(0.3, 0.35, -1.0);
    private static final Vec3 CONJURING_PALM = new Vec3(0.05, 1.0, 0.35);
    /** Reaching over the haft sliding out. */
    private static final Vec3 REACHING = new Vec3(-3.8, 7.8, 6.8);
    private static final Vec3 REACHING_UP = new Vec3(1.0, 0.15, 0.1);
    private static final Vec3 REACHING_PALM = new Vec3(0.15, -1.0, 0.0);
    private static final Vec3 LEFT_REACHING = new Vec3(3.8, 7.8, 3.4);
    private static final Vec3 LEFT_REACHING_UP = new Vec3(-1.0, 0.2, -0.45);
    private static final Vec3 LEFT_REACHING_PALM = new Vec3(-0.15, -1.0, 0.0);
    /** About where each hand closes on the haft (the grip itself is worked out from the axe). */
    private static final Vec3 GRABBING = new Vec3(-3.1, 6.85, 7.9);
    private static final Vec3 GRABBING_UP = new Vec3(1.0, 0.1, 0.0);
    private static final Vec3 GRABBING_PALM = new Vec3(0.1, -1.0, 0.0);
    private static final Vec3 LEFT_GRABBING = new Vec3(3.1, 6.85, 4.1);
    /** About where each hand holds the haft once it is stuck in the ground. */
    private static final Vec3 LETTING_GO = new Vec3(-3.1, 4.8, 2.4);
    private static final Vec3 LETTING_GO_UP = new Vec3(1.0, 0.2, -0.3);
    private static final Vec3 LETTING_GO_PALM = new Vec3(0.0, 0.8, -0.6);
    private static final Vec3 LEFT_LETTING_GO = new Vec3(3.1, 7.3, 5.8);
    private static final Vec3 LEFT_LETTING_GO_UP = new Vec3(-1.0, 0.1, 0.1);
    private static final Vec3 LEFT_LETTING_GO_PALM = new Vec3(0.0, 0.8, -0.6);
    /** Rising off the haft, turning to the caster. */
    private static final Vec3 RISING = new Vec3(-3.3, 8.0, 1.8);
    private static final Vec3 RISING_UP = new Vec3(0.4, 0.4, -1.0);
    private static final Vec3 RISING_PALM = new Vec3(0.9, 0.0, 0.3);
    /**
     * The thumbs up: a fist, its knuckles to the caster and in, tipped up so the thumb (which grows out of the hand at
     * a slant) points straight up; worked out from where the painter's thumb points when it is straight.
     */
    private static final Vec3 THUMB_UP = new Vec3(-3.6, 9.0, 0.4);
    private static final Vec3 THUMB_UP_UP = new Vec3(0.246, 0.668, -0.702);
    private static final Vec3 THUMB_UP_PALM = new Vec3(0.790, 0.282, 0.545);

    // Rolling round each other: how far each hand swings out from its place, and how far round they go (radians).
    private static final double ROLL_RADIUS = 1.4;
    private static final double ROLL_TURNS = Math.PI * 3.3;
    private static final double ROLL_FROM = OK + 21.0;
    private static final double ROLL_TO = AXE_OPENS - 4.5;

    // The fingers: curl of index, middle, ring, little finger and thumb, then the hook of each, then the spread.
    private static final int DIGITS = 11;
    private static final int SPREAD = 10;
    /** Loosely curled, as a hand still in its portal. */
    private static final double[] BALLED = digits(0.55, 0.6, 0.62, 0.66, 0.4, 0.3, 0.3, 0.32, 0.35, 0.1, 0.0);
    /** Relaxed. */
    private static final double[] LOOSE = digits(0.2, 0.24, 0.28, 0.33, 0.15, 0.16, 0.18, 0.2, 0.24, 0.08, 0.45);
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
    /** Closed round the haft, the thumb over the fingers: fitted to the painter's finger sizes round HAFT_RADIUS. */
    private static final double[] GRIPPING = digits(0.61, 0.695, 0.651, 0.483, 0.62, 0.158, 0.144, 0.158, 0.095, 0.05,
            0.0);
    /** Let go, opened wide. */
    private static final double[] RELEASED = digits(0.1, 0.1, 0.12, 0.15, 0.06, 0.04, 0.04, 0.06, 0.08, 0.0, 0.5);
    /** A fist, the thumb still out. */
    private static final double[] FIST = digits(1.0, 1.0, 1.0, 1.0, 0.06, 0.1, 0.1, 0.1, 0.1, 0.0, 0.0);
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
    /** How far behind its finger's root the two outer joints of a finger follow, in ticks. */
    private static final double HOOK_LAG = 0.5;

    private static final int SMOOTH = 0;
    private static final int LATE = 1;
    private static final int EARLY = 2;
    private static final int POP = 3;

    private static final Track RIGHT = new Track(
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
            new Key(RELEASE + 6, false, RISING, RISING_UP, RISING_PALM),
            new Key(THUMBS, true, THUMB_UP, THUMB_UP_UP, THUMB_UP_PALM),
            new Key(RETRACT, true, THUMB_UP, THUMB_UP_UP, THUMB_UP_PALM));

    private static final Track LEFT = new Track(
            new Key(OUT, true, HOVER, HOVER_UP, HOVER_PALM).mirrored(),
            new Key(OUT + 8, false, HOVER.add(0.1, 0.2, -0.05), HOVER_UP, HOVER_PALM).mirrored(),
            new Key(SNAP - 4, false, HOVER.add(0.35, 0.35, -0.15), new Vec3(0.6, 0.0, -1.0), HOVER_PALM).mirrored(),
            new Key(SNAP + 2, false, HOVER.add(0.4, 0.5, -0.1), new Vec3(0.55, 0.15, -1.0), HOVER_PALM).mirrored(),
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
            new Key(RELEASE + 6, false, RISING, RISING_UP, RISING_PALM).mirrored(),
            new Key(THUMBS, true, THUMB_UP, THUMB_UP_UP, THUMB_UP_PALM).mirrored(),
            new Key(RETRACT, true, THUMB_UP, THUMB_UP_UP, THUMB_UP_PALM).mirrored());

    private static final Fingers RIGHT_FINGERS = new Fingers(BALLED)
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
            .to(FIST, THUMBS - 3.5, 3.0, SMOOTH, QUICK_CLOSING)
            .to(THUMB_STRAIGHT, THUMBS - 1.5, 1.0, POP, TOGETHER)
            .twitch(THUMBS, 1.2, digits(0.05, 0.05, 0.05, 0.05, 0.0, 0.06, 0.06, 0.06, 0.06, -0.14, 0.0))
            .to(TRAILING, RETRACT + 1, 5.0, SMOOTH, OPENING);

    private static final Fingers LEFT_FINGERS = new Fingers(BALLED)
            .to(LOOSE, ARRIVES + 3.4, 7.0, SMOOTH, OPENING)
            .wave(ARRIVES + 2, ARRIVES + 7, SNAP - 4, SNAP + 1, 0.5, 0.95, 0.2, 0.15, 0.1)
            .to(FANNED, OK - 9, 5.0, SMOOTH, new double[] { 0.0, 0.6, 0.3, 0.0, 0.0 })
            .to(OK_SIGN, OK - 5, 5.0, EARLY, new double[] { 0.0, 0.0, 0.0, 0.0, 0.0 })
            .twitch(OK, 1.3, digits(0.0, -0.04, -0.04, -0.04, 0.0, 0.0, -0.05, -0.05, -0.05, 0.0, 0.08))
            .to(CRAWLING, OK + 15.4, 5.0, SMOOTH, OPENING)
            .wave(OK + 16.4, OK + 21.4, AXE_OPENS - 6.6, AXE_OPENS - 3.6, 1.35, 1.25, 0.3, 0.25, 0.15)
            .to(SPREAD_WIDE, AXE_OPENS - 4.6, 4.0, EARLY, QUICK_OPENING)
            .to(OPEN, AXE_OPENS + 3.4, 7.0, SMOOTH, OPENING)
            .to(GRIPPING, GRAB - 3, 3.5, SMOOTH, CLOSING)
            .to(RELEASED, RELEASE - 1.1, 3.0, SMOOTH, OPENING)
            .to(FIST, THUMBS - 3.1, 3.0, SMOOTH, QUICK_CLOSING)
            .to(THUMB_STRAIGHT, THUMBS - 1.1, 1.0, POP, TOGETHER)
            .twitch(THUMBS + 0.4, 1.2, digits(0.05, 0.05, 0.05, 0.05, 0.0, 0.06, 0.06, 0.06, 0.06, -0.14, 0.0))
            .to(TRAILING, RETRACT + 1.4, 5.0, SMOOTH, OPENING);

    /** A portal of the ring's light: a round opening facing {@code normal} (what comes out comes out that way). */
    public record Portal(Vec3 center, Vec3 normal, Vec3 a, Vec3 b, double radius, double open) {
    }

    /** Where the LEFT hand comes out. */
    public final Portal leftPortal;
    /** Where the RIGHT hand comes out. */
    public final Portal rightPortal;
    /** Where the axe comes out. */
    public final Portal axePortal;
    /** The left hand's fingers: curl, hook and spread (the rest unused). */
    public final HandPose leftPose;
    /** The right hand's fingers: curl, hook and spread (the rest unused). */
    public final HandPose rightPose;
    /**
     * Where the left hand is. Its right points from its little finger's side to its THUMB (it is drawn as the right
     * hand's shapes mirrored), and its forearm runs out of its portal: arm is the way from the portal's middle to the
     * wrist.
     */
    public final HandPose.Place leftPlace;
    /** Where the right hand is (the ring on its middle finger); its forearm runs out of its portal as the left's. */
    public final HandPose.Place rightPlace;
    /** True from ARRIVES until HANDS_GONE: the hands are drawn. */
    public final boolean handsThere;
    /** The end of the axe's pommel, in the world. */
    public final Vec3 axeEnd;
    /** One long: along the haft, pommel to head. */
    public final Vec3 axeUp;
    /** One long, square to axeUp: the way the blade's edge faces. */
    public final Vec3 axeFace;
    /** True from the moment its pommel first shows until it has broken away. */
    public final boolean axeThere;
    /** True while it still comes out of its portal: draw it cut at the axe portal's plane. */
    public final boolean axeCut;
    /** 0 whole .. 1 broken away (from AXE_BREAKS). */
    public final double axeBreak;

    private HandDuo(Portal leftPortal, Portal rightPortal, Portal axePortal, HandPose leftPose, HandPose rightPose,
            HandPose.Place leftPlace, HandPose.Place rightPlace, double t, Axe axe) {
        this.leftPortal = leftPortal;
        this.rightPortal = rightPortal;
        this.axePortal = axePortal;
        this.leftPose = leftPose;
        this.rightPose = rightPose;
        this.leftPlace = leftPlace;
        this.rightPlace = rightPlace;
        this.handsThere = t >= ARRIVES && t < HANDS_GONE;
        this.axeEnd = axe.end();
        this.axeUp = axe.up();
        this.axeFace = axe.face();
        this.axeBreak = Mth.clamp((t - AXE_BREAKS) / BREAK_TICKS, 0.0, 1.0);
        this.axeThere = drawnOut(t) > 0.0 && this.axeBreak < 1.0;
        this.axeCut = this.axeThere && drawnOut(t) < DRAWN;
    }

    /**
     * The pair and its axe {@code t} ticks after it was called.
     *
     * @param base    the ground where it was called (the creature's feet)
     * @param variant its variant: the way it is laid out (see {@link HandPose#axeVariant})
     * @param aim     the spot the axe strikes (only its x and z count; kept within REACH of base)
     * @param scale   its size (1: the size it is made at)
     */
    public static HandDuo at(Vec3 base, int variant, Vec3 aim, double t, double scale) {
        Layout layout = Layout.of(base, variant, aim, scale);
        Axe axe = axe(layout, t);
        Hand left = hand(layout, false, t, axe);
        Hand right = hand(layout, true, t, axe);
        return new HandDuo(sidePortal(layout, false, t, left.arm), sidePortal(layout, true, t, right.arm),
                axePortal(layout, t), LEFT_FINGERS.pose(t, left.drag, left.held),
                RIGHT_FINGERS.pose(t, right.drag, right.held), left.place, right.place, t, axe);
    }

    /** The middle of the blade's edge as it strikes (at IMPACT), in the ground at the (clamped) aim. */
    public static Vec3 strike(Vec3 base, int variant, Vec3 aim, double scale) {
        Layout layout = Layout.of(base, variant, aim, scale);
        return layout.point(layout.strike());
    }

    // ---- The axe ----

    /** Where the axe is: the end of its pommel, the way along its haft (pommel to head) and the way its edge faces. */
    private record Axe(Vec3 end, Vec3 up, Vec3 face) {
    }

    /**
     * How far the axe is out of its portal (from the end of its pommel): it slides out slowly, pommel first, until the
     * hands close on it, is drawn out by them until its head comes free and comes to rest in their hands. Below 0 it is
     * still inside, sliding towards the opening.
     */
    private static double drawnOut(double t) {
        double from = -1.5;
        if (t < AXE_OPENS) {
            return from + 0.3 * (t - AXE_OPENS);
        }
        if (t < GRAB) {
            return hermite(t, AXE_OPENS, from, 0.3, GRAB, SLID, 0.1);
        }
        if (t < AXE_FREE) {
            return hermite(t, GRAB, SLID, 0.1, AXE_FREE, DRAWN, 0.55);
        }
        if (t < LOCKED_FROM) {
            return hermite(t, AXE_FREE, DRAWN, 0.55, LOCKED_FROM, DRAWN_REST, 0.0);
        }
        return DRAWN_REST;
    }

    /**
     * The axe at {@code t}: out of its portal level, head last and edge up; carried by the hands (the middle of its
     * grips, how far it leans up from level with its head away from the caster, and how far it is rolled about its
     * haft so its edge reaches a strike off to the side) through the dip, the heave, the hang and the chop; and left
     * where it struck, quivering a moment. Its haft always stays square to the caster's right, as the forearms come
     * from there.
     */
    private static Axe axe(Layout layout, double t) {
        Vec3 grips;
        double tilt;
        double roll = 0.0;
        Vec3 drawn = AXE_PORTAL.add(0.0, 0.0, GRIPS - drawnOut(Math.min(t, LOCKED_FROM)));
        if (t < LOCKED_FROM) {
            grips = drawn;
            tilt = t < AXE_FREE - 1 ? 0.0 : hermite(t, AXE_FREE - 1, 0.0, 0.0, LOCKED_FROM + 3, DIP_TILT, 0.0);
        } else {
            Vec3 raised = layout.raised();
            roll = layout.roll() * Ease.smoother((t - LOCKED_FROM - 2.0) / 9.0);
            double dipAt = LOCKED_FROM + 3.5;
            Vec3 dipped = drawn.add(raised.subtract(drawn).multiply(0.12, 0.0, 0.12)).add(0.0, -DIP, 0.0);
            if (t < dipAt) {
                grips = hermite(t, LOCKED_FROM, drawn, Vec3.ZERO, dipAt, dipped,
                        raised.subtract(drawn).multiply(0.1, 0.0, 0.1));
            } else if (t < RAISED) {
                double u = (t - dipAt) / (RAISED - dipAt);
                Vec3 flat = hermite(t, dipAt, dipped, raised.subtract(drawn).multiply(0.1, 0.0, 0.1), RAISED, raised,
                        Vec3.ZERO);
                grips = new Vec3(flat.x, Mth.lerp(early(u), dipped.y, raised.y), flat.z);
            } else if (t < CHOP_FROM) {
                grips = raised;
            } else {
                double u = Math.min(1.0, (t - CHOP_FROM) / (IMPACT - CHOP_FROM));
                grips = raised.lerp(layout.struck(), Ease.smoother(u / HANDS_DOWN));
            }
            if (t < LOCKED_FROM + 3) {
                tilt = hermite(t, AXE_FREE - 1, 0.0, 0.0, LOCKED_FROM + 3, DIP_TILT, 0.0);
            } else if (t < RAISED) {
                tilt = Mth.lerp(early((t - LOCKED_FROM - 3) / (RAISED - LOCKED_FROM - 3)), DIP_TILT, RAISE_TILT);
            } else if (t < CHOP_FROM) {
                tilt = RAISE_TILT;
            } else {
                double u = Math.min(1.0, (t - CHOP_FROM) / (IMPACT - CHOP_FROM));
                tilt = Mth.lerp(chop(u), RAISE_TILT, Math.PI + layout.lean());
            }
            // Trembling with its weight at the top.
            double hang = window(t, RAISED - 1.5, RAISED + 0.5, CHOP_FROM - 0.5, CHOP_FROM + 1.0);
            tilt += 0.012 * Math.sin(2.4 * t) * hang;
            grips = grips.add(0.0, 0.05 * Math.sin(3.1 * t + 1.0) * hang, 0.0);
        }
        Vec3 away = layout.way();
        Vec3 up = away.scale(Math.cos(tilt)).add(Vectors.UP.scale(Math.sin(tilt)));
        // Its edge leads the way it swings up and over, rolled towards the caster's right by roll.
        Vec3 face = away.scale(-Math.sin(tilt)).add(Vectors.UP.scale(Math.cos(tilt))).scale(Math.cos(roll))
                .add(layout.side().scale(Math.sin(roll)));
        Vec3 end = layout.point(grips).subtract(up.scale(GRIPS * layout.scale()));
        if (t > IMPACT) {
            // Stuck in the ground it quivers a moment, turning about where it bites, and is still again before the
            // hands let go.
            double u = t - IMPACT;
            double quiver = 0.05 * wobble(u, 1.9, 0.3, 0.7) * (1.0 - Ease.smoother((u - 7.0) / 5.0));
            if (quiver != 0.0) {
                Vec3 axis = layout.side();
                Vec3 bite = layout.point(layout.strike());
                end = bite.add(Vectors.spin(end.subtract(bite), axis, quiver));
                up = Vectors.spin(up, axis, quiver);
                face = Vectors.spin(face, axis, quiver);
            }
        }
        return new Axe(end, up, face);
    }

    // ---- The hands ----

    /** A hand worked out: where it is, the way its forearm runs, how its fingers are dragged and how hard it holds. */
    private record Hand(HandPose.Place place, Vec3 arm, double drag, double held) {
    }

    /**
     * One hand at {@code t}: pushing out of its portal, free in the air (its keys, bobbing, snapping, rolling...),
     * closing on and carrying the haft, and pulling back into its portal.
     */
    private static Hand hand(Layout layout, boolean right, double t, Axe axe) {
        Vec3 portal = layout.point(portal(right));
        double scale = layout.scale();
        if (t < OUT) {
            return coming(layout, right, t, portal);
        }
        if (t >= RETRACT) {
            return going(layout, right, t, portal);
        }
        Vec3[] free = free(layout, right, t);
        Vec3 wrist = free[0];
        Vec3 up = free[1];
        Vec3 palm = free[2];
        double held = Ease.smoother((t - GRAB + 8.0) / 8.0) * (1.0 - Ease.smoother((t - RELEASE) / 10.0));
        if (held > 0.0) {
            Vec3[] grip = grip(layout, right, t, axe, portal);
            wrist = wrist.lerp(grip[0], held);
            up = up.lerp(grip[1], held);
            palm = palm.lerp(grip[2], held);
        }
        Vec3 arm = wrist.subtract(portal).normalize();
        return new Hand(place(wrist, arm, up, palm, scale), arm, drag(right, t), held);
    }

    /** The hand in the air (not in its portal, not holding on): its wrist, the way its fingers point, its palm. */
    private static Vec3[] free(Layout layout, boolean right, double t) {
        Vec3[] key = (right ? RIGHT : LEFT).at(t);
        Vec3 wrist = layout.point(key[0].add(lift(right, t)));
        Vec3 up = layout.dir(key[1]);
        Vec3 palm = layout.dir(key[2]);
        Vec3 side = layout.side();
        // Rolling round the other hand, it tips with its swing.
        double swing = roll(t);
        if (swing != 0.0) {
            double tip = (right ? 0.2 : -0.2) * swing * Math.sin(rollTurn(t) - Math.PI * 0.5);
            up = Vectors.spin(up, side, tip);
            palm = Vectors.spin(palm, side, tip);
        }
        Vec3 face = palm.normalize();
        if (right) {
            // The snap flicks its fingers down towards its palm, turning about the way across the hand.
            double jolt = -0.12 * kick(t - SNAP, 0.9);
            Vec3 across = face.cross(up).normalize();
            up = Vectors.spin(up, across, jolt);
            palm = Vectors.spin(palm, across, jolt);
        } else {
            // A proud little wobble with its OK sign, turning about the way its palm faces.
            double proud = 0.12 * wobble(t - OK, 0.9, 0.18, 1.5) * (1.0 - Ease.smoother((t - OK - 11.0) / 5.0));
            up = Vectors.spin(up, face, proud);
        }
        // A nod with the thumbs up.
        double nod = 0.16 * wobble(t - THUMBS - 0.5 - (right ? 0.0 : 0.4), 0.8, 0.22, 1.5)
                * (1.0 - Ease.smoother((t - THUMBS - 9.0) / 5.0));
        up = Vectors.spin(up, side, nod);
        palm = Vectors.spin(palm, side, nod);
        return new Vec3[] { wrist, up, palm };
    }

    /**
     * How far the wrist of a hand in the air is moved off its keys, at scale 1 in the pair's terms: breathing while it
     * hovers, the jolt of the snap, the pop of the OK sign, rolling round the other hand and the pop of the thumbs up.
     */
    private static Vec3 lift(boolean right, double t) {
        double p = right ? 0.0 : 2.1;
        double breathe = window(t, OUT, OUT + 6, GRAB - 10, GRAB - 4) + window(t, RELEASE + 4, RELEASE + 10,
                RETRACT - 4, RETRACT);
        Vec3 lift = new Vec3(0.07 * Math.sin(0.31 * t + p), 0.14 * Math.sin(0.19 * t + 0.4 + p)
                + 0.05 * Math.sin(0.47 * t + 2.0 + p), 0.06 * Math.sin(0.26 * t + 2.6 + p)).scale(breathe);
        if (right) {
            lift = lift.add(new Vec3(0.05, -0.22, -0.15).scale(kick(t - SNAP, 0.9)));
        } else {
            lift = lift.add(new Vec3(0.0, 0.22, -0.1).scale(kick(t - OK, 1.4)));
        }
        double swing = roll(t);
        if (swing != 0.0) {
            double turn = rollTurn(t);
            lift = lift.add(new Vec3(0.0, Math.sin(turn), Math.cos(turn)).scale((right ? 1.0 : -1.0) * swing
                    * ROLL_RADIUS));
        }
        return lift.add(new Vec3(0.0, 0.4, -0.12).scale(kick(t - THUMBS - (right ? 0.0 : 0.4), 1.2)));
    }

    /** How far out the hands swing as they roll round each other: 0 before and after, 1 in full swing. */
    private static double roll(double t) {
        return window(t, ROLL_FROM, ROLL_FROM + 3.5, ROLL_TO - 3.5, ROLL_TO);
    }

    /** How far round the hands have rolled (radians). */
    private static double rollTurn(double t) {
        return Math.PI * 0.5 + ROLL_TURNS * Ease.smoother((t - ROLL_FROM) / (ROLL_TO - ROLL_FROM));
    }

    /**
     * How much a hand's fingers are dragged by how it moves: moving towards its palm pushes them open, moving away
     * curls them (from how far it went the last one and a half ticks, so it follows smoothly).
     */
    private static double drag(boolean right, double t) {
        Track track = right ? RIGHT : LEFT;
        Vec3[] now = track.at(t);
        Vec3[] then = track.at(t - 1.5);
        Vec3 moved = now[0].add(lift(right, t)).subtract(then[0].add(lift(right, t - 1.5))).scale(1.0 / 1.5);
        double along = moved.dot(now[2].normalize()) / 0.35;
        return -0.1 * along / Math.sqrt(1.0 + along * along) * window(t, OUT, OUT + 3.0, RETRACT - 3.0, RETRACT);
    }

    /**
     * Pushing out of its portal (before OUT): fingertips first, straight along the way it comes out, a little
     * corkscrewed, coasting to rest in front of it with a soft overshoot and bending into its hovering pose.
     */
    private static Hand coming(Layout layout, boolean right, double t, Vec3 portal) {
        Vec3[] rest = free(layout, right, OUT);
        Vec3 arm = rest[0].subtract(portal).normalize();
        double reach = rest[0].distanceTo(portal) / layout.scale();
        double out;
        if (t < ARRIVES + 1) {
            out = -7.6;
        } else if (t < ARRIVES + 6.5) {
            out = hermite(t, ARRIVES + 1, -7.6, 0.0, ARRIVES + 6.5, -0.4, 1.6);
        } else if (t < ARRIVES + 10.5) {
            out = hermite(t, ARRIVES + 6.5, -0.4, 1.6, ARRIVES + 10.5, reach + 0.55, 0.0);
        } else {
            out = hermite(t, ARRIVES + 10.5, reach + 0.55, 0.0, OUT, reach, 0.0);
        }
        Vec3 wrist = portal.add(arm.scale(out * layout.scale()));
        Vec3 straight = rest[2].subtract(arm.scale(rest[2].dot(arm)));
        double bend = Ease.smoother((t - ARRIVES - 5.0) / (OUT - ARRIVES - 5.0));
        Vec3 up = arm.lerp(rest[1], bend);
        Vec3 palm = straight.lerp(rest[2], bend);
        double screw = (right ? 0.9 : -0.9) * (1.0 - Ease.smoother((t - ARRIVES) / (OUT - ARRIVES - 2.0)));
        up = Vectors.spin(up, arm, screw);
        palm = Vectors.spin(palm, arm, screw);
        return new Hand(place(wrist, arm, up, palm, layout.scale()), arm, 0.0, 0.0);
    }

    /**
     * Pulling back into its portal (from RETRACT): it lifts a hair, then slides back along its forearm into the
     * portal, straightening out, its fingers trailing last.
     */
    private static Hand going(Layout layout, boolean right, double t, Vec3 portal) {
        Vec3[] last = free(layout, right, RETRACT);
        Vec3 arm = last[0].subtract(portal).normalize();
        double reach = last[0].distanceTo(portal) / layout.scale();
        double in;
        if (t < RETRACT + 2.5) {
            in = hermite(t, RETRACT, reach, 0.0, RETRACT + 2.5, reach + 0.35, 0.0);
        } else {
            in = Mth.lerp(late((t - RETRACT - 2.5) / (HANDS_GONE - 2.0 - RETRACT - 2.5)), reach + 0.35, -7.6);
        }
        Vec3 wrist = portal.add(arm.scale(in * layout.scale()));
        double bend = Ease.smoother((t - RETRACT - 1.0) / 6.0);
        Vec3 straight = last[2].subtract(arm.scale(last[2].dot(arm)));
        Vec3 up = last[1].lerp(arm, bend);
        Vec3 palm = last[2].lerp(straight, bend);
        return new Hand(place(wrist, arm, up, palm, layout.scale()), arm, 0.0, 0.0);
    }

    /**
     * A hand's fist exactly round the haft: the haft runs through FIST_HOLE along the hand's x, its thumb's side to the
     * head (the right hand's right is down the haft, the left hand's up it), its fingers pointing the way its forearm
     * comes from its portal, turned square to the haft, so the wrist bends least. Trembling after the blow, it turns a
     * hair about the haft. Gives its wrist, the way its fingers point and its palm.
     */
    private static Vec3[] grip(Layout layout, boolean right, double t, Axe axe, Vec3 portal) {
        double scale = layout.scale();
        Vec3 hole = axe.end().add(axe.up().scale((right ? GRIP_HIGH : GRIP_LOW) * scale));
        Vec3 thumbSide = right ? axe.up().scale(-1.0) : axe.up();
        double shake = 0.07 * wobble(t - IMPACT - (right ? 0.0 : 0.3), 2.2, 0.35, 0.8)
                * (1.0 - Ease.smoother((t - IMPACT - 8.0) / 4.0));
        // The fingers point the way from the portal to the wrist, and the wrist hangs off the fist the way the fingers
        // point: going round this a few times settles them.
        Vec3 toward = hole.subtract(portal);
        Vec3[] fist = new Vec3[0];
        for (int i = 0; i < 4; i++) {
            Vec3 up = Vectors.spin(toward.subtract(axe.up().scale(toward.dot(axe.up()))).normalize(), axe.up(), shake);
            Vec3 palm = up.cross(thumbSide);
            Vec3 wrist = hole.subtract(up.scale(FIST_HOLE.y * scale)).subtract(palm.scale(FIST_HOLE.z * scale));
            fist = new Vec3[] { wrist, up, palm };
            toward = wrist.subtract(portal);
        }
        return fist;
    }

    /**
     * The place of a hand from its wrist, the way its forearm runs, the way its fingers point and its palm (these two
     * need not be one long nor square): straightened out, its right worked out so the frame is left-handed like every
     * construct's, and its forearm's palm side the palm turned as little as can be to lie square to the forearm.
     */
    private static HandPose.Place place(Vec3 wrist, Vec3 arm, Vec3 up, Vec3 palm, double scale) {
        Vec3 u = up.normalize();
        Vec3 f = palm.subtract(u.scale(palm.dot(u))).normalize();
        Vec3 r = f.cross(u);
        // The palm carried round by the turn that takes the fingers' way onto the forearm's way.
        Vec3 axis = u.cross(arm);
        double cos = u.dot(arm);
        Vec3 turned = f.scale(cos).add(axis.cross(f)).add(axis.scale(axis.dot(f) / (1.0 + cos)));
        Vec3 armForward = turned.subtract(arm.scale(turned.dot(arm))).normalize();
        return new HandPose.Place(wrist, arm, armForward, r, u, f, scale);
    }

    // ---- The portals ----

    /** The middle of a hand's portal, at scale 1 in the pair's terms. */
    private static Vec3 portal(boolean right) {
        return right ? RIGHT_PORTAL : RIGHT_PORTAL.multiply(-1.0, 1.0, 1.0);
    }

    /** Where a hand hovers once it is out (its wrist at OUT), at scale 1 in the pair's terms. */
    private static Vec3 hover(boolean right) {
        return right ? HOVER : HOVER.multiply(-1.0, 1.0, 1.0);
    }

    /**
     * A hand's portal: facing along the hand's forearm (it swivels with it, so the forearm always runs square through
     * it); bursting open as the ring's light reaches the spot, shrinking and popping shut as the hand is gone.
     */
    private static Portal sidePortal(Layout layout, boolean right, double t, Vec3 arm) {
        Vec3 center = layout.point(portal(right));
        Vec3 rest = layout.dir(hover(right).subtract(portal(right))).normalize();
        Vec3 a0 = Vectors.UP.cross(rest).normalize();
        // Its own ways carried round by the turn that takes its first facing onto the forearm's.
        Vec3 axis = rest.cross(arm);
        double cos = rest.dot(arm);
        Vec3 a = a0.scale(cos).add(axis.cross(a0)).add(axis.scale(axis.dot(a0) / (1.0 + cos))).normalize();
        Vec3 b = arm.cross(a).normalize();
        double open = Ease.spring(t - ARRIVES, 1.05, 0.62) * shut((t - HANDS_GONE + 5.0) / 5.0);
        return new Portal(center, arm, a, b, SIDE_RADIUS * layout.scale(), open);
    }

    /** The axe's portal: facing the caster, bursting open as the hands fling apart, shut once the head is out. */
    private static Portal axePortal(Layout layout, double t) {
        Vec3 normal = layout.dir(new Vec3(0.0, 0.0, -1.0));
        Vec3 a = layout.side();
        double open = Ease.spring(t - AXE_OPENS, 0.9, 0.62) * shut((t - AXE_FREE) / 3.5);
        return new Portal(layout.point(AXE_PORTAL), normal, a, normal.cross(a), AXE_RADIUS * layout.scale(), open);
    }

    /** A portal shrinking shut over u from 0 to 1: it swells a touch, then snaps shut (1 before, 0 after). */
    private static double shut(double u) {
        if (u <= 0.0) {
            return 1.0;
        }
        if (u < 0.35) {
            return 1.0 + 0.06 * Ease.smoother(u / 0.35);
        }
        return 1.06 * (1.0 - Ease.smoother((u - 0.35) / 0.65));
    }

    // ---- The layout ----

    /**
     * Where the pair is laid out: where it was called, the flat way from the caster to it and his right (both one
     * long), its size, and the spot the axe strikes at scale 1 in the pair's terms (to his right, away from him), kept
     * within REACH.
     */
    private record Layout(Vec3 base, Vec3 way, Vec3 side, double scale, double aimSide, double aimAway) {
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

    // ---- The keys of the hands in the air ----

    /** A moment of a hand in the air: where its wrist is, which ways its fingers and palm point; still: at rest. */
    private record Key(double t, boolean still, Vec3 at, Vec3 up, Vec3 palm) {
        /** The same for the other hand. */
        Key mirrored() {
            return new Key(this.t, this.still, this.at.multiply(-1.0, 1.0, 1.0), this.up.multiply(-1.0, 1.0, 1.0),
                    this.palm.multiply(-1.0, 1.0, 1.0));
        }
    }

    /** A hand's way through the air: from key to key smoothly, passing through each at an even speed. */
    private static final class Track {
        private final Key[] keys;
        private final Vec3[][] speeds;

        Track(Key... keys) {
            this.keys = keys;
            this.speeds = new Vec3[keys.length][];
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].still() || i == 0 || i == keys.length - 1) {
                    this.speeds[i] = new Vec3[] { Vec3.ZERO, Vec3.ZERO, Vec3.ZERO };
                } else {
                    Key before = keys[i - 1];
                    Key after = keys[i + 1];
                    double span = after.t() - before.t();
                    this.speeds[i] = new Vec3[] { after.at().subtract(before.at()).scale(1.0 / span),
                            after.up().subtract(before.up()).scale(1.0 / span),
                            after.palm().subtract(before.palm()).scale(1.0 / span) };
                }
            }
        }

        /** Where it is at {@code t}: wrist, fingers' way, palm (in the pair's terms). */
        Vec3[] at(double t) {
            Key[] k = this.keys;
            if (t <= k[0].t()) {
                return new Vec3[] { k[0].at(), k[0].up(), k[0].palm() };
            }
            int last = k.length - 1;
            if (t >= k[last].t()) {
                return new Vec3[] { k[last].at(), k[last].up(), k[last].palm() };
            }
            int i = 0;
            while (t >= k[i + 1].t()) {
                i++;
            }
            Key a = k[i];
            Key b = k[i + 1];
            Vec3[] va = this.speeds[i];
            Vec3[] vb = this.speeds[i + 1];
            return new Vec3[] { hermite(t, a.t(), a.at(), va[0], b.t(), b.at(), vb[0]),
                    hermite(t, a.t(), a.up(), va[1], b.t(), b.up(), vb[1]),
                    hermite(t, a.t(), a.palm(), va[2], b.t(), b.palm(), vb[2]) };
        }
    }

    // ---- The fingers ----

    /**
     * How the fingers of a hand move: from a first pose, each change of pose after another, finger after finger (each
     * change adds its difference smoothly, so changes may overlap), with ripples and twitches on top.
     */
    private static final class Fingers {
        private final double[] first;
        private final List<Part> parts = new ArrayList<>();
        private double[] last;

        Fingers(double[] first) {
            this.first = first;
            this.last = first;
        }

        /**
         * Change to {@code pose}, starting at {@code from} and taking {@code length} ticks, each finger (index, middle,
         * ring, little finger, thumb) {@code lags} ticks late, the outer joints of each a little later still.
         */
        Fingers to(double[] pose, double from, double length, int curve, double[] lags) {
            double[] by = new double[DIGITS];
            for (int i = 0; i < DIGITS; i++) {
                by[i] = pose[i] - this.last[i];
            }
            this.parts.add(new Change(by, from, length, curve, lags));
            this.last = pose;
            return this;
        }

        /**
         * Waves running through the fingers (index first), growing from {@code a} to {@code b} and dying away from
         * {@code c} to {@code d}: {@code speed} radians a tick, {@code step} radians from one finger to the next, how
         * far each curl and hook swings, and how far the thumb's curl swings.
         */
        Fingers wave(double a, double b, double c, double d, double speed, double step, double curl, double hook,
                double thumb) {
            this.parts.add(new Wave(a, b, c, d, speed, step, curl, hook, thumb));
            return this;
        }

        /** Middle finger and thumb pressed together, trembling a hair, from {@code from} to {@code to}. */
        Fingers tremble(double from, double to, double speed, double size) {
            this.parts.add(new Tremble(from, to, speed, size));
            return this;
        }

        /** A twitch of the fingers at {@code at}, peaking {@code rise} ticks later and dying away. */
        Fingers twitch(double at, double rise, double[] size) {
            this.parts.add(new Twitch(at, rise, size));
            return this;
        }

        /** The fingers at {@code t}, dragged by how the hand moves ({@code drag}) unless it holds on ({@code held}). */
        HandPose pose(double t, double drag, double held) {
            double[] d = this.first.clone();
            for (Part part : this.parts) {
                part.add(t, d);
            }
            HandPose pose = new HandPose();
            double free = drag * (1.0 - held);
            for (int k = 0; k < 5; k++) {
                pose.curl[k] = d[k] + free * (0.7 + 0.1 * k);
                pose.hook[k] = d[5 + k] + free * 0.8;
            }
            pose.spread = d[SPREAD];
            return pose;
        }
    }

    /** Something the fingers do, added onto their pose. */
    private interface Part {
        void add(double t, double[] digits);
    }

    /** A change of pose (see {@link Fingers#to}). */
    private record Change(double[] by, double from, double length, int curve, double[] lags) implements Part {
        @Override
        public void add(double t, double[] digits) {
            for (int i = 0; i < DIGITS; i++) {
                if (this.by[i] == 0.0) {
                    continue;
                }
                double lag = i == SPREAD ? this.lags[1] : this.lags[i % 5] + (i >= 5 ? HOOK_LAG : 0.0);
                double since = t - this.from - lag;
                digits[i] += this.by[i] * step(this.curve, since / this.length, since);
            }
        }
    }

    /** Waves running through the fingers (see {@link Fingers#wave}). */
    private record Wave(double a, double b, double c, double d, double speed, double step, double curl, double hook,
            double thumb) implements Part {
        @Override
        public void add(double t, double[] digits) {
            double grow = window(t, this.a, this.b, this.c, this.d);
            if (grow == 0.0) {
                return;
            }
            for (int k = 0; k < 5; k++) {
                double phase = this.speed * t - this.step * k;
                double size = grow * (k == 4 ? this.thumb / this.curl : 1.0);
                digits[k] += size * this.curl * Math.sin(phase);
                digits[5 + k] += size * this.hook * Math.sin(phase - 0.9);
            }
        }
    }

    /** The middle finger and thumb pressing on each other (see {@link Fingers#tremble}). */
    private record Tremble(double from, double to, double speed, double size) implements Part {
        @Override
        public void add(double t, double[] digits) {
            double press = window(t, this.from, this.from + 1.0, this.to - 0.4, this.to) * this.size;
            digits[1] += press * Math.sin(this.speed * t);
            digits[4] -= press * Math.sin(this.speed * t + 0.5);
        }
    }

    /** A twitch of the fingers (see {@link Fingers#twitch}). */
    private record Twitch(double at, double rise, double[] size) implements Part {
        @Override
        public void add(double t, double[] digits) {
            double k = kick(t - this.at, this.rise);
            if (k == 0.0) {
                return;
            }
            for (int i = 0; i < DIGITS; i++) {
                digits[i] += k * this.size[i];
            }
        }
    }

    /** A pose of the fingers: curl of index, middle, ring, little finger and thumb, their hooks, the spread. */
    private static double[] digits(double... values) {
        return values;
    }

    /** A pose of the fingers with some of its numbers (index, value, index, value...) changed. */
    private static double[] with(double[] pose, double... changes) {
        double[] out = pose.clone();
        for (int i = 0; i + 1 < changes.length; i += 2) {
            out[(int) changes[i]] = changes[i + 1];
        }
        return out;
    }

    // ---- Curves ----

    /** How far through a change of shape {@code curve} is at {@code u} (0 before, 1 after; {@code since} in ticks). */
    private static double step(int curve, double u, double since) {
        return switch (curve) {
            case LATE -> late(u);
            case EARLY -> early(u);
            case POP -> Ease.spring(since, 1.1, 0.42);
            default -> Ease.smoother(u);
        };
    }

    /** 0 to 1, speeding up most of the way and stopping short: a slam. */
    private static double late(double u) {
        double c = Mth.clamp(u, 0.0, 1.0);
        return c * c * c * (4.0 - 3.0 * c);
    }

    /** 0 to 1, fast at first and settling slowly: a fling. */
    private static double early(double u) {
        return 1.0 - late(1.0 - u);
    }

    /** 0 to 1 for the chop: slow over the top, ever faster, then stopped short as the blade bites. */
    private static double chop(double u) {
        double c = Mth.clamp(u, 0.0, 1.0);
        double c4 = c * c * c * c;
        return c4 * c * (6.0 - 5.0 * c);
    }

    /** 0 before a, growing to 1 by b, 1 until c, dying back to 0 by d. */
    private static double window(double t, double a, double b, double c, double d) {
        return Ease.smoother((t - a) / (b - a)) * (1.0 - Ease.smoother((t - c) / (d - c)));
    }

    /** A jolt at u = 0: rises from nothing to 1 at {@code rise}, then dies away. */
    private static double kick(double u, double rise) {
        if (u <= 0.0) {
            return 0.0;
        }
        double x = u / rise;
        return x * x * Math.exp(2.0 * (1.0 - x));
    }

    /** A wobble starting at u = 0: {@code speed} radians a tick, dying away, grown in over {@code ramp} ticks. */
    private static double wobble(double u, double speed, double decay, double ramp) {
        if (u <= 0.0) {
            return 0.0;
        }
        return Math.sin(speed * u) * Math.exp(-decay * u) * Ease.smoother(u / ramp);
    }

    /** A cubic from {@code a} at {@code ta} (moving {@code va} a tick) to {@code b} at {@code tb} (at {@code vb}). */
    private static double hermite(double t, double ta, double a, double va, double tb, double b, double vb) {
        double span = tb - ta;
        return Ease.hermite(a, va * span, b, vb * span, (t - ta) / span);
    }

    private static Vec3 hermite(double t, double ta, Vec3 a, Vec3 va, double tb, Vec3 b, Vec3 vb) {
        return new Vec3(hermite(t, ta, a.x, va.x, tb, b.x, vb.x), hermite(t, ta, a.y, va.y, tb, b.y, vb.y),
                hermite(t, ta, a.z, va.z, tb, b.z, vb.z));
    }
}
