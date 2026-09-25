package nl.tivek.multiversepowers.character.greenlantern;

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
public final class HandPose extends HandMoves {
    /** How far from its base each move reaches for its creature, at scale 1, in blocks: where it comes up. */
    private static final double[] SPOT = { 5.8, 1.25, 2.2, 2.9, 5.0 };
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
