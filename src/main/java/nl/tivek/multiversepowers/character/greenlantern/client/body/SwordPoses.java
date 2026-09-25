package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * How Green Lantern moves with the sword and shield of the construct wheel (see {@link SwordMove}).
 *
 * <p>Every move is made as it looks through his own eyes, the way a game with a sword in first person makes it: a
 * handful of key poses, each saying where the sword hand grips, where the blade points and which way its edge faces,
 * and where the shield hangs and faces. Between the keys everything runs along smooth curves through all of them
 * (never from key to key and stopping), so a cut winds up, whips through the moment it strikes and brakes after, and
 * the tip of the blade draws one clean arc; a key can also be a stop, where the move hangs a moment before it strikes,
 * or say outright how fast it is passed (the flick that tosses the sword, a bang on the shield and its rebound). A new
 * move starts from wherever the last one left the arms, at the speed they were moving, and once a move is over they
 * settle back into the guard, where they breathe a little.
 *
 * <p>Taking them out is played on tracks of its own: the sword hand, its wrist, the shield and the body each have keys
 * of their own, so the body leads, the blade follows the hand and the shield arm answers what the sword does. The
 * tossed sword flies freely: its balance point falls along the curve a thrown thing falls along while it turns over at
 * an even speed, and the fist lets go of it and catches it again at exactly the speed it flies.
 *
 * <p>In the guard the sword stands upright in the right fist, the blade a little forward, and the shield hangs on the
 * left forearm, its back to you and its face turned out and forward. The body seen from outside is posed from the very
 * same poses (see {@link SwordArms}); for that each key also says how far the upper body turns into the move (twist),
 * bends forward (lean) and steps into it (step), and for the spinning cut how far the whole body has spun round
 * (orbit). Last, each pose says how far the arms are still (or again) the game's own empty hands (rest): at 1 they are
 * exactly where the game holds them, so they come out of that and go back into it without a jump.
 */
final class SwordPoses {
    /**
     * One pose, as your own eyes see it (x to the right, y up, -z ahead, in blocks): where the sword hand grips, where
     * the blade points and where its edge faces (one long, square to each other), where the middle of the shield is,
     * where its face points and where its top is; how far the upper body turns to the right (twist, radians), bends
     * forward (lean, 0 to 1, a little back below 0) and has stepped forward (step, 0 to 1); how far the whole body has
     * spun round to the left (orbit, radians), which in first person takes everything round with it; and how far the
     * arms are the game's own empty hands (rest, 0 to 1).
     */
    record Pose(Vec3 hand, Vec3 blade, Vec3 edge, Vec3 shield, Vec3 face, Vec3 top, float twist, float lean,
            float step, float orbit, float rest) {
        static final int SIZE = 23;
        // Where the numbers of the blade are in numbers(), and of its edge; those of the shield (its middle, face and
        // top); those of the body; its spin; and how far the arms rest.
        static final int BLADE = 3;
        static final int EDGE = 6;
        static final int SHIELD_FROM = 9;
        static final int SHIELD_TO = 17;
        static final int BODY_FROM = 18;
        static final int ORBIT = 21;
        static final int REST = 22;

        float[] numbers() {
            return new float[] { (float) this.hand.x, (float) this.hand.y, (float) this.hand.z, (float) this.blade.x,
                    (float) this.blade.y, (float) this.blade.z, (float) this.edge.x, (float) this.edge.y,
                    (float) this.edge.z, (float) this.shield.x, (float) this.shield.y, (float) this.shield.z,
                    (float) this.face.x, (float) this.face.y, (float) this.face.z, (float) this.top.x, (float) this.top.y,
                    (float) this.top.z, this.twist, this.lean, this.step, this.orbit, this.rest };
        }

        /** A pose from its numbers; the ways are made one long and square to each other again. */
        static Pose of(float[] n) {
            Vec3 blade = unit(new Vec3(n[3], n[4], n[5]), new Vec3(0.0, 1.0, 0.0));
            Vec3 edge = square(new Vec3(n[6], n[7], n[8]), blade);
            Vec3 face = unit(new Vec3(n[12], n[13], n[14]), new Vec3(0.0, 0.0, -1.0));
            Vec3 top = square(new Vec3(n[15], n[16], n[17]), face);
            return new Pose(new Vec3(n[0], n[1], n[2]), blade, edge, new Vec3(n[9], n[10], n[11]), face, top, n[18],
                    n[19], n[20], n[21], n[22]);
        }

        Pose mix(Pose to, float t) {
            float[] a = this.numbers();
            float[] b = aligned(to.numbers(), a);
            for (int k = 0; k < a.length; k++) {
                a[k] = Mth.lerp(t, a[k], b[k]);
            }
            return of(a);
        }

        /** The same pose with the shield of {@code other}, {@code t} of the way. */
        Pose shieldOf(Pose other, float t) {
            if (t <= 0.0F) {
                return this;
            }
            float[] a = this.numbers();
            float[] b = other.numbers();
            for (int k = SHIELD_FROM; k <= SHIELD_TO; k++) {
                a[k] = Mth.lerp(t, a[k], b[k]);
            }
            return of(a);
        }

        /** The same pose with the blade pointing along {@code blade} and its edge towards {@code edge}. */
        Pose holding(Vec3 blade, Vec3 edge) {
            return new Pose(this.hand, blade, edge, this.shield, this.face, this.top, this.twist, this.lean, this.step,
                    this.orbit, this.rest);
        }

        /** The same pose with the fist at {@code hand}. */
        Pose gripping(Vec3 hand) {
            return new Pose(hand, this.blade, this.edge, this.shield, this.face, this.top, this.twist, this.lean,
                    this.step, this.orbit, this.rest);
        }

        /** The same pose with its twist and its spin brought back to within half a turn: a whole turn is no turn. */
        Pose unwound() {
            return new Pose(this.hand, this.blade, this.edge, this.shield, this.face, this.top, wrap(this.twist),
                    this.lean, this.step, wrap(this.orbit), this.rest);
        }

        /** The same pose turned {@code angle} (radians) to the left about the upright line through your eyes. */
        Pose turned(double angle) {
            if (angle == 0.0) {
                return this;
            }
            return new Pose(spin(this.hand, angle), spin(this.blade, angle), spin(this.edge, angle),
                    spin(this.shield, angle), spin(this.face, angle), spin(this.top, angle), this.twist, this.lean,
                    this.step, this.orbit, this.rest);
        }

        /** The shield's own right: along it the forearm lies on its back, from the elbow to the fist. */
        Vec3 shieldRight() {
            return this.face.cross(this.top).normalize();
        }

        /** Where the fist grips the shield: the grip on its back, at a shield of scale {@code scale}. */
        Vec3 shieldGrip(double scale) {
            return this.shield.add(this.shieldRight().scale(SwordPainter.GRIP_X * scale))
                    .add(this.face.scale(SwordPainter.GRIP_Z * scale));
        }
    }

    /**
     * One key pose of a move: on its own tick, whether the move stops there a moment (a windup before a strike), and
     * how fast each number reaches it ({@code in}) and leaves it ({@code out}), per tick; null, or a number that is not
     * a number, for the speed the keys round it give.
     */
    private record Key(float tick, boolean stop, float[] numbers, @Nullable float[] in, @Nullable float[] out) {
        Key(float tick, boolean stop, float[] numbers) {
            this(tick, stop, numbers, null, null);
        }
    }

    /** The keys of one part of a move: for the numbers from {@code from} up to (not with) {@code to}. */
    private record Track(int from, int to, Key[] keys) {
        float last() {
            return this.keys[this.keys.length - 1].tick();
        }
    }

    // ---- How big the sword and shield are ----

    /** How big the sword and shield are in your own hands in first person, and on a body seen from outside. */
    static final double OWN_SWORD = 0.74;
    static final double OWN_SHIELD = 0.62;
    static final double SWORD_SCALE = 0.9;
    static final double SHIELD_SCALE = 0.76;
    /** How far the shield sits out in front of the forearm seen from outside. */
    static final double SHIELD_OUT = 0.07;

    // ---- From your own eyes to the body seen from outside ----

    /**
     * Seen from outside, a place before your eyes in first person is a place before the body, measured from the middle
     * of the chest at the height of the shoulders (x to his right, y up, z ahead): across and up it is squeezed and
     * raised this much, and ahead it is this much of the depth, less this much.
     */
    private static final double BODY_ACROSS = 0.65;
    private static final double BODY_UP = 1.12;
    private static final double BODY_RAISE = 0.16;
    private static final double BODY_AHEAD = 0.9;
    private static final double BODY_BACK = 0.46;
    /** How far the shoulders are from the middle of the body, in blocks. */
    static final double SHOULDER = 0.31;
    /** How far the upper body bends forward at a lean of 1, in radians, the way it does when crouching. */
    static final float TILT = 0.5F;
    // The model of a body, in its own pixels: how far out and below the neck the shoulders turn, how far the upper body
    // bends the arms down with it, where the fist grips along the right arm and where the shield sits on the left
    // forearm.
    private static final double PIVOT_ACROSS = 5.0;
    private static final double PIVOT_DOWN = 2.0;
    private static final double BEND_DOWN = 3.2;
    private static final Vec3 FIST = new Vec3(-1.0, 9.4, 0.0);
    private static final Vec3 FOREARM = new Vec3(3.4, 6.4, 0.0);
    // Where his eyes are, from the middle of his chest, and how far ahead of the tossed sword they are (ticks).
    private static final Vec3 EYES = new Vec3(0.0, 0.41, 0.12);
    private static final float LEAD = 1.5F;

    // ---- The moves ----

    // How long the arms take to settle back into the guard once the last key of a move is past, and into the run once a
    // ram of the shield is past, in ticks.
    private static final float SETTLE = 7.0F;
    private static final float RAM_SETTLE = 4.0F;
    // How long a flurry or a charge takes to come in from wherever the arms were, in ticks.
    private static final float BLEND_IN = 3.0F;
    // How long the guard takes to start breathing once a move has settled into it, in ticks.
    private static final float IDLE_IN = 10.0F;

    /**
     * The guard: the sword upright in the right fist before the right hip, its blade a little forward and its edge
     * turned so you see its flat, and the shield on the left forearm low before the left hip, its back to you.
     */
    static final Pose GUARD = pose(0.46, -0.50, -0.92, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0,
            -0.50, -0.52, -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0);
    /**
     * The arms as the game holds your empty hands: the right one resting low on the right of your screen (the sword
     * about to grow out of its fist), the left one out of sight below. Seen from outside the arms are the game's own.
     */
    static final Pose REST = rest(pose(RechargeAnimation.HAND_RIGHT.x(), RechargeAnimation.HAND_RIGHT.y(),
            RechargeAnimation.HAND_RIGHT.z(), 0.04, 0.99, -0.12, -1.0, 0.0, 0.0, -0.46, -1.3, -0.84, -0.45, 0.05,
            -1.0, 0.08, 1.0, 0.12, 0, 0, 0, 0));
    /**
     * The shield held up to block: the forearm across before the chest, the shield square to the front just below your
     * line of sight, so you look over it. Laid over the shield only, so the sword can still cut behind it.
     */
    private static final Pose BLOCK = pose(0.46, -0.50, -0.92, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0,
            -0.28, -0.44, -0.76, 0.14, 0.03, -1.0, 0.0, 1.0, 0.08, 0, 0.12F, 0, 0);
    // The flurry: the shield up before the chest, the sword pulled back beside it between two stabs, and how far a stab
    // reaches out, as a part of the way from pulled back to the full thrust.
    private static final Pose FLURRY_GUARD = pose(0.46, -0.36, -0.72, -0.12, 0.08, -0.99, 1.0, 0.0, 0.0,
            -0.30, -0.38, -0.80, 0.06, 0.05, -1.0, 0.0, 1.0, 0.05, -6, 0.2F, 0.15F, 0);
    private static final float STAB_OUT = 1.5F;

    // ---- The toss as they take shape ----

    /**
     * About which way before your eyes the tossed sword turns over: half across (the way a flick of the wrist turns it
     * over, end over end) and half along the way you look, so from your own eyes it turns over at a slant you can
     * follow all the way round, and from outside it cartwheels at a slant too.
     */
    static final Vec3 TOSS_AXIS = new Vec3(0.70, 0.05, -0.71).normalize();
    /** How fast it turns over in the air, in radians per tick: evenly, all through its flight. */
    static final double SPIN = SwordMove.TOSS_TURNS * Mth.TWO_PI / (SwordMove.CATCH - SwordMove.TOSS);
    // Where along its reach to the tip it balances (and turns about): just above the guard, the way a sword does; and
    // how far that is from the grip in your own hands, in blocks.
    private static final double BALANCE = 0.2;
    static final double OWN_BALANCE = BALANCE * SwordPainter.TIP * OWN_SWORD;
    // Where your fist lets go of it and catches it again, before your eyes: well out in front, so the whole flight
    // stays in sight; and how high its balance point rises over where it left the hand.
    private static final Vec3 RELEASE = new Vec3(0.26, -0.40, -1.26);
    private static final Vec3 CAUGHT = new Vec3(0.24, -0.30, -1.26);
    private static final double TOSS_HIGH = 0.5;
    // How it stands in the hand as it is tossed and caught: upright, a little forward, the flat towards you.
    private static final Vec3 UPRIGHT = new Vec3(0.04, 0.99, -0.12).normalize();
    private static final Vec3 FLAT = square(new Vec3(-1.0, 0.0, 0.0), UPRIGHT);
    // How far back the wrist is cocked before the flick, how far it turns ahead to meet the spinning sword, and how far
    // the caught sword carries it on before it springs back (radians).
    private static final double COCK = 0.8;
    private static final double MEET = 0.9;
    private static final double CARRY = 0.42;
    // How fast the blade and its edge turn in the air, per tick.
    private static final Vec3 BLADE_TURN = TOSS_AXIS.cross(UPRIGHT).scale(SPIN);
    private static final Vec3 EDGE_TURN = TOSS_AXIS.cross(FLAT).scale(SPIN);
    // How fast the balance point falls (blocks per tick per tick) and how fast it leaves the hand, worked out below so
    // that it rises TOSS_HIGH and comes down right into the hand; and how fast the fist moves as it lets go and as it
    // catches, which is how fast the grip moves then.
    private static final double FALL;
    private static final Vec3 THROWN;
    private static final Vec3 LET_GO;
    private static final Vec3 CAUGHT_AT;

    // ---- Looking the sword over, and banging it on the shield ----

    // The blade as he looks it over: across your view from low on the right to high on the left; and from when to when
    // the wrist turns it over: a while after the catch, until a while before the first bang.
    private static final Vec3 LOOKED_AT = new Vec3(-0.55, 0.74, -0.40).normalize();
    private static final float INSPECT_FROM = SwordMove.CATCH + 8.0F;
    private static final float INSPECT_TO = SwordMove.KNOCK - 6.0F;
    // Where on the rim of the shield the blade comes down (in the shield's own blocks at scale 1: on its arched top,
    // left of the middle), how thick the rim is, how far along the blade it strikes and how far the edge is from the
    // middle of the blade there (at scale 1).
    private static final double RIM_X = -0.30;
    private static final double RIM_Y = 0.52 + 0.04 * (1.0 - (RIM_X / 0.46) * (RIM_X / 0.46));
    private static final double RIM_THICK = 0.034;
    private static final double STRIKE_AT = 1.05;
    private static final double BLADE_WIDE = 0.08;
    // The blade as it comes down on the rim: lying across the top of the shield, its edge leading down onto it; and the
    // way the blade turns as it comes down.
    private static final Vec3 KNOCKING = new Vec3(-0.95, 0.05, -0.30).normalize();
    private static final Vec3 KNOCK_EDGE = square(new Vec3(0.0, -1.0, 0.25), KNOCKING);
    private static final Vec3 CHOP = KNOCKING.cross(KNOCK_EDGE).normalize();
    // The shield raised to meet the bangs: its middle, the way its face points and where its top is.
    private static final Vec3 RAISED = new Vec3(-0.44, -0.44, -0.88);
    private static final Vec3 RAISED_FACE = new Vec3(-0.32, 0.16, -1.0).normalize();
    private static final Vec3 RAISED_TOP = square(new Vec3(0.05, 1.0, 0.18), RAISED_FACE);
    // How far the fist is raised and the blade turned back up before the first bang and between the two (blocks and
    // radians), how long each swing down takes (ticks), and how much of its speed the blade keeps as it bounces off.
    private static final double WIND_UP = 0.17;
    private static final double WIND_TURN = 0.6;
    private static final double AGAIN_UP = 0.1;
    private static final double AGAIN_TURN = 0.32;
    private static final float WIND_TICKS = 2.5F;
    private static final float AGAIN_TICKS = 2.0F;
    private static final double BOUNCE = 0.33;
    /** Where the blade strikes the rim of the shield before your eyes (the same at both bangs). */
    static final Vec3 STRUCK;

    private static final Map<SwordMove, Track[]> MOVES = new EnumMap<>(SwordMove.class);
    // The tick each move of keys has settled into its rest.
    private static final Map<SwordMove, Float> ENDS = new EnumMap<>(SwordMove.class);

    static {
        // The toss: the balance point leaves the hand, rises TOSS_HIGH and comes down into the hand where it catches
        // it, in the time between; the grip moves as the balance point does, less the turn of the blade about it.
        double time = SwordMove.CATCH - SwordMove.TOSS;
        double half = time / 2.0;
        FALL = fall(TOSS_HIGH, (CAUGHT.y - RELEASE.y) / time, time);
        THROWN = CAUGHT.subtract(RELEASE).scale(1.0 / time).add(0.0, FALL * half, 0.0);
        Vec3 turning = BLADE_TURN.scale(OWN_BALANCE);
        LET_GO = THROWN.subtract(turning);
        CAUGHT_AT = THROWN.subtract(0.0, FALL * time, 0.0).subtract(turning);
        STRUCK = struck(SwordMove.KNOCK);
        swords();
        equip();
        for (SwordMove move : SwordMove.values()) {
            Track[] tracks = MOVES.get(move);
            if (tracks != null) {
                float end = 0.0F;
                for (Track track : tracks) {
                    end = Math.max(end, track.last());
                }
                ENDS.put(move, end + (move.kind() == SwordMove.Kind.BASH ? RAM_SETTLE : SETTLE));
            }
        }
    }

    /** The cuts, thrusts, rams and slam: every number of a pose along the one track. */
    private static void swords() {
        // ---- The sword: twelve cuts and thrusts, the shield kept in its guard. Numbers per key: the tick, whether it
        // stops there (1), where the fist is (x, y, z), where the blade points, where its edge faces, the twist of the
        // upper body (degrees), how far it bends forward and how far it steps. ----
        // Seen through your own eyes every cut stays in sight: the fist well out before you, and where a blade strikes it
        // lies across the screen (never pointing straight away from you, where it would shrink to a stub).
        sword(SwordMove.SLASH,
                4, 1, 0.66, 0.06, -0.86, 0.80, 0.50, 0.33, -0.35, 0.10, -0.93, 34, 0.10F, 0,
                5, 0, 0.50, -0.02, -0.98, 0.74, 0.26, -0.62, -0.64, 0.05, -0.76, 22, 0.16F, 0.12F,
                6, 0, 0.16, -0.14, -1.06, -0.80, -0.12, -0.58, -0.58, -0.12, 0.80, 6, 0.24F, 0.25F,
                8, 0, -0.20, -0.30, -0.94, -0.90, -0.30, 0.30, 0.30, -0.40, 0.86, -30, 0.28F, 0.30F,
                11, 0, 0.10, -0.46, -0.92, -0.35, 0.65, -0.55, -0.30, 0.0, -1.0, -14, 0.12F, 0.12F);
        sword(SwordMove.BACKHAND,
                4, 1, -0.18, 0.06, -0.84, -0.78, 0.52, 0.35, 0.35, 0.10, -0.93, -34, 0.10F, 0,
                5, 0, -0.06, -0.02, -0.96, -0.72, 0.26, -0.64, 0.64, 0.05, -0.76, -22, 0.16F, 0.12F,
                6, 0, 0.24, -0.14, -1.06, 0.80, -0.12, -0.58, 0.58, -0.12, 0.80, -6, 0.24F, 0.25F,
                8, 0, 0.62, -0.30, -0.90, 0.90, -0.30, 0.30, -0.30, -0.40, 0.86, 30, 0.26F, 0.28F,
                11, 0, 0.52, -0.44, -0.92, 0.08, 0.85, -0.52, -0.30, 0.0, -1.0, 14, 0.12F, 0.10F);
        sword(SwordMove.CLEAVE,
                5, 1, 0.56, 0.12, -0.86, 0.40, 0.80, 0.45, -0.40, 0.25, -0.88, 28, -0.10F, 0,
                6, 0, 0.36, -0.02, -0.98, -0.10, 0.75, -0.65, -0.61, -0.56, -0.55, 16, 0.10F, 0.15F,
                7, 0, 0.14, -0.14, -1.06, -0.62, -0.42, -0.66, -0.21, -0.72, 0.66, 2, 0.32F, 0.32F,
                9, 0, -0.16, -0.42, -0.90, -0.62, -0.70, 0.30, 0.44, 0.0, 0.90, -28, 0.46F, 0.36F,
                12, 0, 0.10, -0.48, -0.92, -0.28, 0.70, -0.62, -0.30, 0.0, -1.0, -12, 0.20F, 0.15F);
        sword(SwordMove.REVERSE_CLEAVE,
                5, 1, -0.14, 0.10, -0.86, -0.42, 0.80, 0.42, 0.40, 0.25, -0.88, -28, -0.10F, 0,
                6, 0, 0.02, -0.02, -0.98, 0.12, 0.75, -0.65, 0.61, -0.56, -0.55, -16, 0.10F, 0.15F,
                7, 0, 0.26, -0.14, -1.06, 0.64, -0.42, -0.64, 0.21, -0.72, 0.66, -2, 0.32F, 0.32F,
                9, 0, 0.60, -0.44, -0.88, 0.64, -0.70, 0.30, -0.44, 0.0, 0.90, 28, 0.46F, 0.36F,
                12, 0, 0.52, -0.46, -0.92, 0.02, 0.84, -0.54, -0.30, 0.0, -1.0, 12, 0.20F, 0.15F);
        sword(SwordMove.RISING,
                4, 1, -0.04, -0.50, -0.86, -0.62, -0.62, 0.40, -0.30, -0.20, -0.93, -30, 0.36F, 0.10F,
                5, 0, 0.04, -0.36, -0.98, -0.30, -0.35, -0.89, 0.69, 0.55, -0.45, -14, 0.30F, 0.16F,
                6, 0, 0.20, -0.20, -1.06, 0.70, 0.35, -0.62, 0.40, 0.53, 0.74, 0, 0.20F, 0.22F,
                8, 0, 0.56, 0.02, -0.88, 0.55, 0.78, 0.30, -0.69, 0.21, 0.69, 30, 0.0F, 0.25F,
                11, 0, 0.50, -0.40, -0.92, 0.05, 0.90, -0.42, -0.30, 0.0, -1.0, 10, 0.04F, 0.10F);
        sword(SwordMove.UPPERCUT,
                5, 1, 0.24, -0.52, -0.86, 0.04, -0.55, -0.83, 0.0, 0.83, -0.55, 4, 0.60F, 0.25F,
                7, 0, 0.18, -0.22, -1.10, 0.0, 0.60, -0.80, 0.0, 0.80, 0.60, 0, 0.28F, 0.30F,
                9, 0, 0.16, 0.08, -0.88, 0.0, 0.94, 0.34, 0.0, -0.34, 0.94, 0, -0.18F, 0.26F,
                12, 0, 0.40, -0.40, -0.92, -0.05, 0.92, -0.38, -0.30, 0.0, -1.0, 2, 0.0F, 0.10F);
        sword(SwordMove.OVERHEAD,
                6, 1, 0.22, 0.12, -0.83, 0.05, 0.62, 0.78, 0.0, 0.78, -0.62, 0, -0.25F, 0,
                7, 1, 0.22, 0.15, -0.82, 0.05, 0.55, 0.83, 0.0, 0.83, -0.55, 0, -0.30F, 0.10F,
                8, 0, 0.19, 0.06, -0.94, 0.02, 0.85, -0.52, 0.0, -0.52, -0.85, 0, 0.10F, 0.35F,
                9, 0, 0.14, -0.22, -1.06, 0.0, -0.60, -0.80, 0.0, -0.80, 0.60, 0, 0.55F, 0.60F,
                11, 0, 0.12, -0.50, -0.90, 0.0, -0.92, -0.40, 0.0, -0.40, 0.92, 0, 0.72F, 0.62F,
                13, 0, 0.28, -0.52, -0.94, 0.05, 0.10, -1.0, 0.0, 1.0, 0.10, 0, 0.50F, 0.45F,
                15, 0, 0.42, -0.46, -0.92, -0.05, 0.85, -0.50, -0.30, 0.0, -1.0, 0, 0.30F, 0.30F);
        // The thrusts go in from low at the right to where you aim, the flat of the blade level.
        sword(SwordMove.STAB,
                3, 1, 0.56, -0.44, -0.72, -0.25, 0.10, -0.96, 1.0, 0.0, -0.26, 14, 0.10F, 0,
                5, 0, 0.30, -0.30, -1.20, -0.30, 0.20, -0.93, 1.0, 0.0, -0.32, -16, 0.32F, 0.55F,
                7, 1, 0.30, -0.31, -1.16, -0.30, 0.20, -0.93, 1.0, 0.0, -0.32, -16, 0.34F, 0.55F,
                9, 0, 0.45, -0.48, -0.96, -0.10, 0.82, -0.56, -0.30, 0.0, -1.0, -4, 0.12F, 0.18F);
        sword(SwordMove.LUNGE,
                4, 1, 0.60, -0.42, -0.74, -0.28, 0.12, -0.95, 1.0, 0.0, -0.30, 22, 0.20F, 0,
                6, 0, 0.44, -0.34, -0.98, -0.28, 0.16, -0.95, 1.0, 0.0, -0.30, 10, 0.40F, 0.50F,
                8, 0, 0.26, -0.26, -1.30, -0.26, 0.19, -0.95, 1.0, 0.0, -0.27, -24, 0.62F, 1.0F,
                11, 1, 0.26, -0.27, -1.26, -0.26, 0.19, -0.95, 1.0, 0.0, -0.27, -24, 0.62F, 1.0F,
                14, 0, 0.45, -0.48, -0.96, -0.10, 0.82, -0.56, -0.30, 0.0, -1.0, -6, 0.20F, 0.30F);
        sword(SwordMove.LOW_SWEEP,
                5, 1, 0.56, -0.36, -0.84, 0.80, -0.10, 0.58, 0.58, -0.08, -0.80, 32, 0.80F, 0.50F,
                7, 0, 0.42, -0.40, -0.98, 0.62, -0.20, -0.76, -0.77, -0.06, -0.62, 16, 0.88F, 0.50F,
                8, 0, 0.10, -0.40, -1.04, -0.80, -0.20, -0.56, -0.56, -0.06, 0.82, 0, 0.90F, 0.50F,
                10, 0, -0.18, -0.40, -0.90, -0.95, -0.20, 0.22, 0.22, -0.06, 0.97, -34, 0.86F, 0.50F,
                13, 0, 0.08, -0.48, -0.92, -0.25, 0.68, -0.68, -0.30, 0.0, -1.0, -14, 0.40F, 0.20F);
        // The spinning cut: the blade held out to his right while the whole body goes round once to the left.
        spin(SwordMove.SPIN,
                4, 1, 0.62, -0.28, -0.80, 0.80, 0.06, 0.60, -0.60, 0.0, 0.80, 20, 0.20F, -20,
                7, 0, 0.58, -0.26, -0.90, 0.82, 0.04, -0.57, 0.57, 0.0, 0.82, 0, 0.26F, 40,
                10, 0, 0.58, -0.26, -0.90, 0.82, 0.04, -0.57, 0.57, 0.0, 0.82, 0, 0.28F, 190,
                13, 0, 0.58, -0.26, -0.90, 0.82, 0.04, -0.57, 0.57, 0.0, 0.82, 0, 0.26F, 310,
                15, 0, 0.50, -0.34, -0.90, 0.30, 0.45, -0.84, 0.60, 0.0, 0.80, 0, 0.18F, 360,
                18, 0, 0.46, -0.48, -0.92, -0.08, 0.90, -0.42, -0.30, 0.0, -1.0, 0, 0.08F, 360);
        sword(SwordMove.CROSS,
                3, 1, 0.56, 0.10, -0.86, 0.36, 0.80, 0.48, -0.40, 0.30, -0.85, 24, 0.0F, 0,
                4, 0, 0.40, -0.02, -0.98, 0.20, 0.75, -0.63, -0.61, -0.56, -0.55, 14, 0.08F, 0.10F,
                5, 0, 0.14, -0.16, -1.06, -0.62, -0.42, -0.66, -0.21, -0.72, 0.66, 0, 0.24F, 0.20F,
                7, 0, -0.14, -0.40, -0.90, -0.62, -0.70, 0.30, 0.44, 0.0, 0.90, -20, 0.20F, 0.20F,
                8, 0, -0.22, -0.18, -0.86, -0.85, 0.10, 0.50, 0.10, 1.0, 0.0, -24, 0.14F, 0.20F,
                9, 1, -0.14, 0.08, -0.86, -0.42, 0.80, 0.42, 0.40, 0.25, -0.88, -24, 0.10F, 0.20F,
                10, 0, 0.02, -0.02, -0.98, 0.12, 0.75, -0.65, 0.61, -0.56, -0.55, -12, 0.16F, 0.25F,
                11, 0, 0.26, -0.16, -1.06, 0.64, -0.42, -0.64, 0.21, -0.72, 0.66, 0, 0.32F, 0.35F,
                13, 0, 0.60, -0.44, -0.88, 0.64, -0.70, 0.30, -0.44, 0.0, 0.90, 22, 0.36F, 0.35F,
                14, 0, 0.62, -0.40, -0.90, 0.85, 0.30, -0.40, -0.40, 0.0, -0.85, 16, 0.24F, 0.25F,
                16, 0, 0.50, -0.46, -0.92, 0.0, 0.85, -0.52, -0.30, 0.0, -1.0, 8, 0.10F, 0.10F);
        // ---- The shield's six rams, thrown at whatever stands in the way of a charge while the sword is held back
        // along his side. Numbers per key: the tick, whether it stops there, the middle of the shield, the way its face
        // points and where its top is, the twist of the upper body (degrees) and how far it bends forward. ----
        ram(SwordMove.BASH,
                1, 1, -0.12, -0.40, -0.60, 0.05, 0.02, -1.0, 0.0, 1.0, 0.05, 12, 0.70F,
                2, 0, -0.08, -0.30, -1.08, 0.06, 0.04, -1.0, 0.0, 1.0, 0.05, -14, 0.82F,
                5, 0, -0.10, -0.34, -0.90, 0.07, 0.03, -1.0, 0.0, 1.0, 0.05, -10, 0.80F);
        ram(SwordMove.BASH_SWEEP,
                1, 1, -0.42, -0.36, -0.70, -0.55, 0.02, -0.84, 0.0, 1.0, 0.0, -22, 0.70F,
                2, 0, 0.0, -0.33, -1.02, 0.25, 0.02, -0.97, 0.0, 1.0, 0.0, 4, 0.76F,
                5, 0, 0.46, -0.36, -0.84, 0.85, 0.02, -0.52, 0.0, 1.0, 0.0, 30, 0.74F);
        ram(SwordMove.BASH_BACKHAND,
                1, 1, 0.14, -0.36, -0.72, 0.45, 0.02, -0.90, 0.0, 1.0, 0.0, 24, 0.70F,
                2, 0, -0.18, -0.33, -1.02, -0.25, 0.02, -0.97, 0.0, 1.0, 0.0, -4, 0.76F,
                5, 0, -0.58, -0.36, -0.80, -0.85, 0.02, -0.50, 0.0, 1.0, 0.0, -30, 0.74F);
        ram(SwordMove.BASH_UP,
                1, 1, -0.10, -0.62, -0.72, 0.05, -0.50, -0.86, 0.0, 0.86, -0.50, 0, 0.90F,
                2, 0, -0.08, -0.22, -1.02, 0.05, 0.38, -0.92, 0.0, 0.92, 0.38, 0, 0.60F,
                5, 0, -0.08, 0.05, -0.90, 0.05, 0.75, -0.66, 0.0, 0.66, 0.75, 0, 0.42F);
        ram(SwordMove.BASH_DOWN,
                2, 1, -0.10, 0.05, -0.70, 0.05, 0.62, -0.78, 0.0, 0.78, 0.62, 6, 0.50F,
                3, 0, -0.08, -0.36, -1.02, 0.05, -0.42, -0.90, 0.0, 0.90, -0.42, 0, 0.80F,
                6, 0, -0.10, -0.56, -0.90, 0.05, -0.75, -0.66, 0.0, 0.66, -0.75, -4, 0.96F);
        ram(SwordMove.BASH_SPIN,
                2, 1, -0.46, -0.32, -0.64, -0.62, 0.02, -0.78, 0.0, 1.0, 0.0, 46, 0.78F,
                3, 0, -0.04, -0.28, -1.06, 0.10, 0.02, -1.0, 0.0, 1.0, 0.0, 0, 0.90F,
                6, 0, 0.12, -0.30, -1.0, 0.30, 0.02, -0.95, 0.0, 1.0, 0.0, -34, 0.94F);
        // ---- The end of a charge: the shield raised high and slammed down into the ground before him, face down. ----
        whole(SwordMove.SLAM,
                key(4, true, pose(0.55, -0.52, -0.78, -0.08, 0.94, -0.34, -0.30, 0.0, -1.0, -0.10, 0.22, -0.70, 0.05,
                        0.35, -0.94, 0.0, 0.94, -0.35, 0, -0.10F, 0, 0)),
                key(6, false, pose(0.55, -0.54, -0.80, -0.08, 0.94, -0.34, -0.30, 0.0, -1.0, -0.08, -0.18, -0.95,
                        0.05, -0.55, -0.83, 0.0, 0.83, -0.55, 0, 0.40F, 0.20F, 0)),
                key(7, false, pose(0.56, -0.58, -0.82, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0, -0.06, -0.64, -1.06,
                        0.03, -0.97, -0.25, 0.0, 0.25, -0.97, 0, 0.90F, 0.40F, 0)),
                key(10, true, pose(0.56, -0.58, -0.82, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0, -0.06, -0.62, -1.05,
                        0.03, -0.97, -0.25, 0.0, 0.25, -0.97, 0, 0.96F, 0.40F, 0)));
    }

    /**
     * Taking them out, on four tracks of their own. The sword hand comes up out of the game's own resting pose while
     * the sword grows out of the fist, dips, and flicks the sword up into the air (see {@link #flight}); the fist
     * follows through, sinks to wait under it, reaches up to meet it and rides it down as it catches it, then brings it
     * up before your eyes, holds it there while the wrist turns it over, raises it and brings it down on the rim of the
     * shield twice, bouncing off it each time. The wrist cocks back before the flick and snaps round with it, turns
     * ahead to meet the spin of the sword as it catches it, is carried on by it and springs back. The shield comes up
     * while it grows out of the ring's light, dips as its strap closes, swings back with the toss, gives with the
     * catch, and rises to meet each bang and gives under it. The body leads all of it by a moment: it dips before the
     * hand does, rises into the flick, leans back to watch the sword go up, gives with the catch, and turns into the
     * bangs.
     */
    private static void equip() {
        float toss = SwordMove.TOSS;
        float caught = SwordMove.CATCH;
        float knock = SwordMove.KNOCK;
        float again = SwordMove.KNOCK_AGAIN;
        // Each swing down onto the rim speeds up evenly from where it hangs raised to the moment it strikes, the
        // fastest there, and bounces off at a part of that speed.
        Vec3 hit = contact();
        Vec3 raised = hit.subtract(KNOCK_EDGE.scale(WIND_UP));
        Vec3 lifted = hit.subtract(KNOCK_EDGE.scale(AGAIN_UP));
        Vec3 strike = hit.subtract(raised).scale(2.0 / WIND_TICKS);
        Vec3 strikeAgain = hit.subtract(lifted).scale(2.0 / AGAIN_TICKS);
        Track hand = new Track(0, Pose.BLADE, new Key[] {
                at(4, false, 0.40, -0.44, -0.92),
                at(8, false, 0.34, -0.47, -0.98),
                at(toss - 1.5F, true, 0.33, -0.50, -1.08),
                moving(at(toss, false, RELEASE), 0, LET_GO, LET_GO),
                at(toss + 2.5F, true, RELEASE.add(LET_GO.scale(1.3))),
                at(toss + 7.0F, false, 0.28, -0.44, -1.20),
                at(caught - 5.0F, false, 0.29, -0.46, -1.21),
                moving(at(caught, false, CAUGHT), 0, CAUGHT_AT, CAUGHT_AT),
                at(caught + 2.5F, true, CAUGHT.add(CAUGHT_AT.scale(1.25))),
                at(INSPECT_FROM - 2.5F, false, 0.24, -0.36, -0.92),
                at(INSPECT_FROM, false, 0.21, -0.31, -0.84),
                at((INSPECT_FROM + INSPECT_TO) / 2.0F, false, 0.19, -0.29, -0.82),
                at(INSPECT_TO, false, 0.21, -0.30, -0.83),
                at(knock - WIND_TICKS, true, raised),
                moving(at(knock, false, hit), 0, strike, strike.scale(-BOUNCE)),
                at(again - AGAIN_TICKS, true, lifted),
                moving(at(again, false, hit), 0, strikeAgain, strikeAgain.scale(-0.8 * BOUNCE)),
                at(again + 1.8F, true, hit.subtract(KNOCK_EDGE.scale(0.03))),
                at(again + 5.0F, false, 0.42, -0.43, -0.93) });
        Track shield = new Track(Pose.SHIELD_FROM, Pose.SHIELD_TO + 1, new Key[] {
                held(3, -0.44, -0.56, -0.90, -0.40, 0.06, -1.0, 0.06, 1.0, 0.12),
                held(7, -0.46, -0.53, -0.91, -0.43, 0.06, -1.0, 0.07, 1.0, 0.12),
                held(9, -0.47, -0.52, -0.92, -0.44, 0.06, -1.0, 0.08, 1.0, 0.12),
                held(SwordMove.SHIELD_LOCK + 0.6F, -0.48, -0.57, -0.92, -0.45, -0.03, -1.0, 0.08, 1.0, 0.03),
                held(toss + 0.5F, -0.50, -0.52, -0.93, -0.47, 0.06, -1.0, 0.08, 1.0, 0.13),
                held(toss + 6.0F, -0.50, -0.51, -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12),
                held(caught - 2.0F, -0.50, -0.52, -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12),
                held(caught + 1.0F, -0.51, -0.545, -0.925, -0.46, 0.03, -1.0, 0.08, 1.0, 0.10),
                held(caught + 4.0F, -0.50, -0.53, -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12),
                held(INSPECT_FROM + 2.0F, -0.54, -0.62, -0.94, -0.50, 0.0, -1.0, 0.10, 1.0, 0.10),
                held(INSPECT_TO - 1.0F, -0.54, -0.61, -0.94, -0.50, 0.01, -1.0, 0.10, 1.0, 0.10),
                raised(knock - 3.0F, 0.0),
                moving(raised(knock, 0.0), Pose.SHIELD_FROM, new Vec3(0.0, 0.012, 0.0), KNOCK_EDGE.scale(0.03)),
                raised(knock + 1.8F, 1.0),
                moving(raised(again, 0.0), Pose.SHIELD_FROM, new Vec3(0.0, 0.01, 0.0), KNOCK_EDGE.scale(0.022)),
                raised(again + 1.8F, 0.75),
                held(again + 5.0F, -0.47, -0.49, -0.92, -0.43, 0.07, -1.0, 0.07, 1.0, 0.13) });
        Track body = new Track(Pose.BODY_FROM, Pose.SIZE, new Key[] {
                arrived(leaning(4, 0, 0.04F)),
                leaning(toss - 3.0F, 5, 0.12F),
                leaning(toss - 1.0F, -3, -0.02F),
                leaning(toss + 4.0F, -2, -0.10F),
                leaning(caught - 3.0F, 0, -0.06F),
                leaning(caught + 1.0F, 3, 0.09F),
                leaning(caught + 5.0F, -2, 0.02F),
                leaning(INSPECT_FROM + 2.0F, -7, -0.05F),
                leaning(INSPECT_TO - 1.0F, -5, -0.04F),
                leaning(knock - WIND_TICKS - 0.5F, 10, -0.02F),
                leaning(knock - 0.5F, -13, 0.10F),
                leaning(again - AGAIN_TICKS - 0.3F, -7, 0.06F),
                leaning(again - 0.5F, -12, 0.09F),
                leaning(again + 3.0F, -4, 0.05F) });
        MOVES.put(SwordMove.EQUIP, new Track[] { hand, new Track(Pose.BLADE, Pose.SHIELD_FROM, equipWrist()), shield,
                body });
    }

    /**
     * The wrist as they take shape: upright as the sword grows, cocked back before the flick and snapping round with
     * it, turned ahead to meet the spin of the sword as it comes down and carried on by it; then turning the blade
     * before your eyes from the one flat over its edge to the other, slowly at first and last while the blade swings a
     * little towards you; raised, and brought down edge first on the rim twice.
     */
    private static Key[] equipWrist() {
        float toss = SwordMove.TOSS;
        float caught = SwordMove.CATCH;
        float knock = SwordMove.KNOCK;
        float again = SwordMove.KNOCK_AGAIN;
        Key[] keys = new Key[22];
        int k = 0;
        keys[k++] = wrist(4, false, new Vec3(0.06, 0.98, -0.18), new Vec3(-1.0, 0.0, 0.0));
        keys[k++] = wrist(8, false, UPRIGHT, FLAT);
        keys[k++] = wrist(toss - 1.5F, true, tossTurn(-COCK, UPRIGHT), tossTurn(-COCK, FLAT));
        keys[k++] = moving(wrist(toss, false, UPRIGHT, FLAT), BLADE_TURN, EDGE_TURN, BLADE_TURN, EDGE_TURN);
        keys[k++] = wrist(toss + 2.5F, true, tossTurn(0.5, UPRIGHT), tossTurn(0.5, FLAT));
        keys[k++] = wrist(caught - 2.5F, true, tossTurn(-MEET, UPRIGHT), tossTurn(-MEET, FLAT));
        keys[k++] = moving(wrist(caught, false, UPRIGHT, FLAT), BLADE_TURN, EDGE_TURN, BLADE_TURN, EDGE_TURN);
        keys[k++] = wrist(caught + 1.4F, true, tossTurn(CARRY, UPRIGHT), tossTurn(CARRY, FLAT));
        keys[k++] = wrist(caught + 3.6F, false, tossTurn(-0.07, UPRIGHT), tossTurn(-0.07, FLAT));
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 facing = square(LOOKED_AT.cross(new Vec3(0.0, 0.0, 1.0)), LOOKED_AT);
        for (int i = 0; i <= 6; i++) {
            double u = Ease.smoother(i / 6.0);
            double swing = 0.16 - 0.32 * u;
            Vec3 blade = Vectors.spin(LOOKED_AT, up, swing);
            Vec3 edge = Vectors.spin(square(Vectors.spin(facing, up, swing), blade), blade, Math.PI * u);
            keys[k++] = wrist(Mth.lerp(i / 6.0F, INSPECT_FROM, INSPECT_TO), false, blade, edge);
        }
        // Raised, and swung down onto the rim: evenly faster all the way, bouncing off it; lifted a little way and
        // swung down again, a lighter bang.
        Vec3 bladeDown = KNOCKING.subtract(chopped(KNOCKING, WIND_TURN)).scale(2.0 / WIND_TICKS);
        Vec3 edgeDown = KNOCK_EDGE.subtract(chopped(KNOCK_EDGE, WIND_TURN)).scale(2.0 / WIND_TICKS);
        Vec3 bladeAgain = KNOCKING.subtract(chopped(KNOCKING, AGAIN_TURN)).scale(2.0 / AGAIN_TICKS);
        Vec3 edgeAgain = KNOCK_EDGE.subtract(chopped(KNOCK_EDGE, AGAIN_TURN)).scale(2.0 / AGAIN_TICKS);
        keys[k++] = wrist(knock - WIND_TICKS, true, chopped(KNOCKING, WIND_TURN), chopped(KNOCK_EDGE, WIND_TURN));
        keys[k++] = moving(wrist(knock, false, KNOCKING, KNOCK_EDGE), bladeDown, edgeDown, bladeDown.scale(-BOUNCE),
                edgeDown.scale(-BOUNCE));
        keys[k++] = wrist(again - AGAIN_TICKS, true, chopped(KNOCKING, AGAIN_TURN), chopped(KNOCK_EDGE, AGAIN_TURN));
        keys[k++] = moving(wrist(again, false, KNOCKING, KNOCK_EDGE), bladeAgain, edgeAgain,
                bladeAgain.scale(-0.8 * BOUNCE), edgeAgain.scale(-0.8 * BOUNCE));
        keys[k++] = wrist(again + 1.8F, true, chopped(KNOCKING, 0.08), chopped(KNOCK_EDGE, 0.08));
        keys[k] = wrist(again + 5.0F, false, GUARD.blade(), GUARD.edge());
        return keys;
    }

    /** The blade (or its edge) as it lies on the rim at a bang, turned {@code angle} (radians) back up off it. */
    private static Vec3 chopped(Vec3 way, double angle) {
        return Vectors.spin(way, CHOP, -angle);
    }

    private SwordPoses() {
    }

    /** A pose from its numbers: the fist, blade and edge, the shield, its face and top, twist (degrees), lean, step, orbit (degrees). */
    private static Pose pose(double hx, double hy, double hz, double bx, double by, double bz, double ex, double ey,
            double ez, double sx, double sy, double sz, double fx, double fy, double fz, double tx, double ty, double tz,
            float twist, float lean, float step, float orbit) {
        return Pose.of(new float[] { (float) hx, (float) hy, (float) hz, (float) bx, (float) by, (float) bz, (float) ex,
                (float) ey, (float) ez, (float) sx, (float) sy, (float) sz, (float) fx, (float) fy, (float) fz,
                (float) tx, (float) ty, (float) tz, twist * Mth.DEG_TO_RAD, lean, step, orbit * Mth.DEG_TO_RAD, 0.0F });
    }

    /** The same pose with the arms fully the game's own. */
    private static Pose rest(Pose pose) {
        float[] n = pose.numbers();
        n[Pose.REST] = 1.0F;
        return Pose.of(n);
    }

    private static Key key(float tick, boolean stop, Pose pose) {
        return new Key(tick, stop, pose.numbers());
    }

    /** A move whose every number runs along the one track. */
    private static void whole(SwordMove move, Key... keys) {
        MOVES.put(move, new Track[] { new Track(0, Pose.SIZE, keys) });
    }

    /** A key of the sword hand: where the fist is. */
    private static Key at(float tick, boolean stop, double x, double y, double z) {
        return at(tick, stop, new Vec3(x, y, z));
    }

    private static Key at(float tick, boolean stop, Vec3 fist) {
        return key(tick, stop, GUARD.gripping(fist));
    }

    /** A key of the wrist: where the blade points and where its edge faces. */
    private static Key wrist(float tick, boolean stop, Vec3 blade, Vec3 edge) {
        Vec3 way = blade.normalize();
        return key(tick, stop, GUARD.holding(way, square(edge, way)));
    }

    /** A key of the shield: its middle, the way its face points and where its top is. */
    private static Key held(float tick, double sx, double sy, double sz, double fx, double fy, double fz, double tx,
            double ty, double tz) {
        return key(tick, false, pose(GUARD.hand().x, GUARD.hand().y, GUARD.hand().z, GUARD.blade().x, GUARD.blade().y,
                GUARD.blade().z, GUARD.edge().x, GUARD.edge().y, GUARD.edge().z, sx, sy, sz, fx, fy, fz, tx, ty, tz, 0,
                0, 0, 0));
    }

    /**
     * A key of the shield raised to meet the bangs, and how far ({@code give}, 0 to 1) it has given under the blade:
     * pushed down along the way the blade struck, and tipped away from it.
     */
    private static Key raised(float tick, double give) {
        Vec3 tipped = Vectors.spin(RAISED_FACE, RAISED_FACE.cross(RAISED_TOP).normalize(), -0.07 * give);
        Vec3 middle = RAISED.add(KNOCK_EDGE.scale(0.035 * give));
        return held(tick, middle.x, middle.y, middle.z, tipped.x, tipped.y, tipped.z, RAISED_TOP.x, RAISED_TOP.y,
                RAISED_TOP.z);
    }

    /** A key of the body: how far it turns to the right (degrees) and bends forward, the arms fully its own. */
    private static Key leaning(float tick, float twist, float lean) {
        return key(tick, false, pose(GUARD.hand().x, GUARD.hand().y, GUARD.hand().z, GUARD.blade().x, GUARD.blade().y,
                GUARD.blade().z, GUARD.edge().x, GUARD.edge().y, GUARD.edge().z, GUARD.shield().x, GUARD.shield().y,
                GUARD.shield().z, GUARD.face().x, GUARD.face().y, GUARD.face().z, GUARD.top().x, GUARD.top().y,
                GUARD.top().z, twist, lean, 0, 0));
    }

    /** The same key of the body, where the arms have come all the way out of the game's own: they stop coming there. */
    private static Key arrived(Key key) {
        float[] still = unknown();
        still[Pose.REST] = 0.0F;
        return new Key(key.tick(), key.stop(), key.numbers(), still, still);
    }

    /** The same key of the wrist, its blade and its edge reached and left at these speeds (per tick). */
    private static Key moving(Key key, Vec3 bladeIn, Vec3 edgeIn, Vec3 bladeOut, Vec3 edgeOut) {
        return moving(moving(key, Pose.BLADE, bladeIn, bladeOut), Pose.EDGE, edgeIn, edgeOut);
    }

    /** The same key with the three numbers from {@code at} on reached and left at these speeds (per tick). */
    private static Key moving(Key key, int at, Vec3 in, Vec3 out) {
        float[] ins = key.in() == null ? unknown() : key.in().clone();
        float[] outs = key.out() == null ? unknown() : key.out().clone();
        ins[at] = (float) in.x;
        ins[at + 1] = (float) in.y;
        ins[at + 2] = (float) in.z;
        outs[at] = (float) out.x;
        outs[at + 1] = (float) out.y;
        outs[at + 2] = (float) out.z;
        return new Key(key.tick(), key.stop(), key.numbers(), ins, outs);
    }

    /** Speeds for every number of a pose, none of them said yet. */
    private static float[] unknown() {
        float[] speeds = new float[Pose.SIZE];
        Arrays.fill(speeds, Float.NaN);
        return speeds;
    }

    /**
     * A move of the sword, as keys of fourteen numbers: the tick, whether it stops there, the fist, the blade, its edge,
     * the twist of the upper body (degrees), how far it bends forward and how far it steps. The shield stays in its guard.
     */
    private static void sword(SwordMove move, double... n) {
        Key[] keys = new Key[n.length / 14];
        for (int k = 0; k < keys.length; k++) {
            int i = k * 14;
            keys[k] = key((float) n[i], n[i + 1] > 0.5, pose(n[i + 2], n[i + 3], n[i + 4], n[i + 5], n[i + 6], n[i + 7],
                    n[i + 8], n[i + 9], n[i + 10], GUARD.shield().x, GUARD.shield().y, GUARD.shield().z,
                    GUARD.face().x, GUARD.face().y, GUARD.face().z, GUARD.top().x, GUARD.top().y, GUARD.top().z,
                    (float) n[i + 11], (float) n[i + 12], (float) n[i + 13], 0));
        }
        whole(move, keys);
    }

    /**
     * The spinning cut, as keys of fourteen numbers: the tick, whether it stops there, the fist, the blade, its edge, the
     * twist of the upper body (degrees), how far it bends forward, and how far the whole body has spun round (degrees).
     */
    private static void spin(SwordMove move, double... n) {
        Key[] keys = new Key[n.length / 14];
        for (int k = 0; k < keys.length; k++) {
            int i = k * 14;
            keys[k] = key((float) n[i], n[i + 1] > 0.5, pose(n[i + 2], n[i + 3], n[i + 4], n[i + 5], n[i + 6], n[i + 7],
                    n[i + 8], n[i + 9], n[i + 10], GUARD.shield().x, GUARD.shield().y, GUARD.shield().z,
                    GUARD.face().x, GUARD.face().y, GUARD.face().z, GUARD.top().x, GUARD.top().y, GUARD.top().z,
                    (float) n[i + 11], (float) n[i + 12], 0, (float) n[i + 13]));
        }
        whole(move, keys);
    }

    /**
     * A ram of the shield in a charge, as keys of thirteen numbers: the tick, whether it stops there, the middle of the
     * shield, its face, its top, the twist of the upper body (degrees) and how far it bends forward. The sword stays held
     * back as it runs (see {@link #charge}).
     */
    private static void ram(SwordMove move, double... n) {
        Pose run = charge(0.0F);
        Key[] keys = new Key[n.length / 13];
        for (int k = 0; k < keys.length; k++) {
            int i = k * 13;
            keys[k] = key((float) n[i], n[i + 1] > 0.5, pose(run.hand().x, run.hand().y, run.hand().z, run.blade().x,
                    run.blade().y, run.blade().z, run.edge().x, run.edge().y, run.edge().z, n[i + 2], n[i + 3],
                    n[i + 4], n[i + 5], n[i + 6], n[i + 7], n[i + 8], n[i + 9], n[i + 10], (float) n[i + 11],
                    (float) n[i + 12], 0, 0));
        }
        whole(move, keys);
    }

    // ---- Playing a move ----

    /**
     * The pose {@code t} ticks into a move, coming in from {@code from} (where the arms were as it began, moving at
     * {@code fromSpeed} per tick, or standing still when that is null) and settling back into the guard (or, after a
     * ram, into the run) once it is over.
     *
     * @param time ticks of the client's own clock, for everything that sways
     */
    static Pose at(SwordMove move, float t, float time, Pose from, @Nullable float[] fromSpeed) {
        return switch (move) {
            case FLURRY -> carried(from, fromSpeed, flurry(t), t);
            case CHARGE -> carried(from, fromSpeed, charge(time), t);
            default -> {
                boolean ram = move.kind() == SwordMove.Kind.BASH;
                Pose rest = ram ? charge(time)
                        : idle(time, (float) Ease.smoother((t - ENDS.getOrDefault(move, 0.0F)) / IDLE_IN));
                yield keyed(MOVES.get(move), t, from, fromSpeed, rest, ram ? RAM_SETTLE : SETTLE);
            }
        };
    }

    /**
     * {@code to}, reached from {@code from} over the first few ticks of a move: the arms go on the way they were moving
     * for a moment, and ease into it.
     */
    private static Pose carried(Pose from, @Nullable float[] speed, Pose to, float t) {
        float u = Math.max(0.0F, t) / BLEND_IN;
        if (u >= 1.0F) {
            return to;
        }
        float[] n = from.numbers();
        if (speed != null) {
            float on = u * BLEND_IN * (1.0F - u) * (1.0F - u);
            for (int c = 0; c < n.length; c++) {
                n[c] += speed[c] * on;
            }
        }
        return Pose.of(n).mix(to, (float) Ease.smooth(u));
    }

    /**
     * The guard, breathing: the hands and the shield rise and sink a little with every breath, the blade sways and the
     * body turns a hair; {@code amount} of it (0 to 1).
     */
    private static Pose idle(float time, float amount) {
        if (amount <= 0.0F) {
            return GUARD;
        }
        float[] n = GUARD.numbers();
        float breath = Mth.sin(time * 0.13F);
        float sway = Mth.sin(time * 0.061F + 1.3F);
        float drift = Mth.sin(time * 0.037F + 0.4F);
        n[0] += amount * 0.005F * sway;
        n[1] += amount * 0.008F * breath;
        n[2] += amount * 0.004F * drift;
        n[Pose.BLADE] += amount * 0.014F * sway;
        n[Pose.BLADE + 2] += amount * 0.012F * drift;
        n[Pose.SHIELD_FROM] += amount * 0.004F * drift;
        n[Pose.SHIELD_FROM + 1] += amount * 0.007F * Mth.sin(time * 0.13F - 0.5F);
        n[Pose.BODY_FROM] += amount * 0.012F * sway;
        n[Pose.BODY_FROM + 1] += amount * 0.012F * breath;
        return Pose.of(n);
    }

    /**
     * The pose {@code t} ticks into a move of keys: along smooth curves, one per track, through {@code from} (at tick
     * 0), every key, and {@code rest} once the last key of the track is {@code settle} ticks past. Each key is passed
     * at the speed it says itself, or else at the speed the keys round it give (the way from the one before to the one
     * after), so nothing stops on a key but the stops, the start (unless the arms were moving then) and the end.
     */
    private static Pose keyed(@Nullable Track[] tracks, float t, Pose from, @Nullable float[] fromSpeed, Pose rest,
            float settle) {
        if (tracks == null || tracks.length == 0) {
            return rest;
        }
        float[] restNumbers = rest.numbers();
        float[] out = restNumbers.clone();
        float time = Math.max(0.0F, t);
        for (Track track : tracks) {
            Key[] keys = track.keys();
            float[] ends = restNumbers.clone();
            if (track.from() <= Pose.ORBIT && track.to() > Pose.ORBIT) {
                // A move that spun the body round ends a whole number of turns further: that is where it rests.
                ends[Pose.ORBIT] += Math.round(keys[keys.length - 1].numbers()[Pose.ORBIT] / Mth.TWO_PI) * Mth.TWO_PI;
            }
            float end = track.last() + settle;
            if (time >= end) {
                if (track.from() <= Pose.EDGE && track.to() > Pose.EDGE) {
                    // Rest on whichever edge the blade came round to (both are the same).
                    float[] edge = from.numbers();
                    for (Key key : keys) {
                        edge = aligned(key.numbers(), edge);
                    }
                    ends = aligned(ends, edge);
                }
                System.arraycopy(ends, track.from(), out, track.from(), track.to() - track.from());
                continue;
            }
            boolean fromStart = keys[0].tick() > 0.0F;
            Key[] line = new Key[keys.length + (fromStart ? 2 : 1)];
            int i = 0;
            if (fromStart) {
                line[i++] = new Key(0.0F, fromSpeed == null, from.numbers(), null, fromSpeed);
            }
            for (Key key : keys) {
                line[i++] = key;
            }
            line[i] = new Key(end, true, ends);
            if (track.from() <= Pose.EDGE && track.to() > Pose.EDGE) {
                for (int k = 1; k < line.length; k++) {
                    line[k] = aligned(line[k], line[k - 1].numbers());
                }
            }
            curve(line, time, track.from(), track.to(), out);
        }
        return Pose.of(out);
    }

    /** The key with the blade's edge turned over to the other edge if it faces away from the one in {@code to}. */
    private static Key aligned(Key key, float[] to) {
        float[] numbers = aligned(key.numbers(), to);
        return numbers == key.numbers() ? key
                : new Key(key.tick(), key.stop(), numbers, flipped(key.in()), flipped(key.out()));
    }

    /** Speeds with those of the blade's edge the other way round. */
    @Nullable
    private static float[] flipped(@Nullable float[] speeds) {
        if (speeds == null) {
            return null;
        }
        float[] out = speeds.clone();
        for (int c = Pose.EDGE; c < Pose.EDGE + 3; c++) {
            out[c] = -out[c];
        }
        return out;
    }

    /**
     * {@code numbers} of a pose with the blade's edge turned over to the other edge if it faces away from the edge in
     * {@code to}. The blade is the same on both edges and both flats, so that changes nothing you see, but going from the
     * one pose to the other it now only turns as far as it has to, never half round through nothing.
     */
    private static float[] aligned(float[] numbers, float[] to) {
        int e = Pose.EDGE;
        if (numbers[e] * to[e] + numbers[e + 1] * to[e + 1] + numbers[e + 2] * to[e + 2] >= 0.0F) {
            return numbers;
        }
        float[] out = numbers.clone();
        for (int c = e; c < e + 3; c++) {
            out[c] = -out[c];
        }
        return out;
    }

    /**
     * Numbers {@code from} up to {@code to} of a smooth curve through keys, at tick {@code t}, into {@code out}:
     * between two keys a cubic that leaves the first and reaches the second at the speed each says, or else the speed
     * the keys round it give (zero at a stop and at both ends).
     */
    private static void curve(Key[] keys, float t, int from, int to, float[] out) {
        int n = keys.length;
        int i = 0;
        while (i + 2 < n && keys[i + 1].tick() <= t) {
            i++;
        }
        float t0 = keys[i].tick();
        float t1 = keys[i + 1].tick();
        float h = Math.max(1.0E-3F, t1 - t0);
        float s = Mth.clamp((t - t0) / h, 0.0F, 1.0F);
        float s2 = s * s;
        float s3 = s2 * s;
        float h00 = 2.0F * s3 - 3.0F * s2 + 1.0F;
        float h10 = s3 - 2.0F * s2 + s;
        float h01 = -2.0F * s3 + 3.0F * s2;
        float h11 = s3 - s2;
        float[] a = keys[i].numbers();
        float[] b = keys[i + 1].numbers();
        for (int c = from; c < to; c++) {
            float m0 = slope(keys, i, c, keys[i].out());
            float m1 = slope(keys, i + 1, c, keys[i + 1].in());
            out[c] = h00 * a[c] + h10 * h * m0 + h01 * b[c] + h11 * h * m1;
        }
    }

    /**
     * How fast number {@code c} runs through key {@code i}: as {@code said} says, or else from the key before to the
     * one after; zero at a stop and at both ends.
     */
    private static float slope(Key[] keys, int i, int c, @Nullable float[] said) {
        if (said != null && !Float.isNaN(said[c])) {
            return said[c];
        }
        if (keys[i].stop() || i == 0 || i == keys.length - 1) {
            return 0.0F;
        }
        return (keys[i + 1].numbers()[c] - keys[i - 1].numbers()[c])
                / Math.max(1.0E-3F, keys[i + 1].tick() - keys[i - 1].tick());
    }

    /**
     * The flurry: the shield comes up before the chest, and the sword stabs out twelve times all over the front, pulled
     * back between two stabs, each along its own way (see {@link SwordMove#stab}).
     */
    private static Pose flurry(float t) {
        float first = SwordMove.FIRST_STAB;
        float every = SwordMove.STAB_EVERY;
        int k = Mth.clamp(Math.round((t - first) / every), 0, SwordMove.STABS - 1);
        float u = (t - (first + k * every)) / STAB_OUT;
        float out = t < first - STAB_OUT || t > first + (SwordMove.STABS - 1) * every + STAB_OUT ? 0.0F
                : Math.max(0.0F, 1.0F - u * u);
        double[] way = SwordMove.stab(k);
        double side = way[0] * Mth.DEG_TO_RAD;
        double up = way[1] * Mth.DEG_TO_RAD;
        Vec3 aim = new Vec3(Math.sin(side) * Math.cos(up), Math.sin(up), -Math.cos(side) * Math.cos(up));
        // Every stab thrown with the shoulder behind it: the body twists into it and leans a little further.
        Pose stab = pose(0.16 + aim.x * 0.9, -0.22 + aim.y * 0.9, -1.48, aim.x, aim.y, aim.z, 1.0, 0.0, 0.0,
                FLURRY_GUARD.shield().x, FLURRY_GUARD.shield().y, FLURRY_GUARD.shield().z, FLURRY_GUARD.face().x,
                FLURRY_GUARD.face().y, FLURRY_GUARD.face().z, 0.0, 1.0, 0.05, (float) (-16.0 + way[0] * 0.25), 0.3F,
                0.3F, 0);
        Pose pose = FLURRY_GUARD.mix(stab, out);
        return t > SwordMove.FLURRY.ticks()
                ? pose.mix(GUARD, (float) Ease.smooth((t - SwordMove.FLURRY.ticks()) / SETTLE)) : pose;
    }

    /**
     * Running behind the shield in a charge: the shield locked before the body, the sword held back low along the right
     * side, and everything bobbing with the steps; seen from outside he is bent far forward.
     */
    static Pose charge(float time) {
        float bob = Mth.sin(time * 1.4F);
        return pose(0.62, -0.62 + 0.015 * bob, -0.66, 0.30, 0.62, 0.72, 0.0, 0.76, -0.64, -0.14, -0.40 + 0.02 * bob,
                -0.74, 0.08, 0.02, -1.0, 0.0, 1.0, 0.08, -8.0F * bob * 0.3F, 0.72F, 0, 0);
    }

    /** The pose with the shield held up to block laid over it, {@code amount} (0 to 1) of the way. */
    static Pose block(Pose pose, float amount) {
        return pose.shieldOf(BLOCK, amount);
    }

    // When the sword starts to grow out of the fist as they take shape, and how long it takes to grow, in ticks.
    private static final float SWORD_FROM = 1.0F;
    private static final float SWORD_GROWS = 7.0F;

    /** How far the sword has grown out of the ring's light while they take shape, 0 to 1 (1 for every other move). */
    static float swordGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? (float) Ease.smooth((t - SWORD_FROM) / SWORD_GROWS) : 1.0F;
    }

    /**
     * How far the shield has come out of the ring's light while they take shape, 0 to 1 (1 for every other move): it
     * builds up evenly from the moment the ring's light reaches the forearm until its strap closes (see
     * {@link SwordPainter#shield}).
     */
    static float shieldGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? Mth.clamp((t - 2.0F) / (SwordMove.SHIELD_LOCK - 2.0F), 0.0F, 1.0F) : 1.0F;
    }

    // ---- The toss ----

    /** Where the tossed sword is in the air: its grip, and where its blade and edge point. */
    record Flight(Vec3 grip, Vec3 blade, Vec3 edge) {
    }

    /** How far through its flight the tossed sword is as they take shape, 0 to 1, or -1 while it is in the hand. */
    static float tossed(SwordMove move, float t) {
        if (move != SwordMove.EQUIP || t <= SwordMove.TOSS || t >= SwordMove.CATCH) {
            return -1.0F;
        }
        return (t - SwordMove.TOSS) / (SwordMove.CATCH - SwordMove.TOSS);
    }

    /** The blade (or its edge) as the wrist has turned it {@code angle} (radians) along with the toss. */
    private static Vec3 tossTurn(double angle, Vec3 way) {
        return Vectors.spin(way, TOSS_AXIS, angle);
    }

    /**
     * How fast a thrown thing falls (blocks per tick per tick) that rises {@code high} over where it leaves the hand and
     * lands {@code time} ticks later, {@code climb} per tick higher than it left (on average).
     */
    private static double fall(double high, double climb, double time) {
        double half = time / 2.0;
        double b = 2.0 * half * climb - 2.0 * high;
        return (-b + Math.sqrt(Math.max(0.0, b * b - 4.0 * half * half * climb * climb))) / (2.0 * half * half);
    }

    /**
     * The tossed sword {@code t} ticks into taking them out, before your own eyes: its balance point falls freely from
     * where the fist let go of it, and it turns over evenly about that, so it lands in the fist at the catch exactly as
     * the fist moves then. Past the catch it flies on the same way (only ever seen when it broke up in the air).
     */
    static Flight flight(float t) {
        double tau = t - SwordMove.TOSS;
        Vec3 balance = RELEASE.add(UPRIGHT.scale(OWN_BALANCE)).add(THROWN.scale(tau)).subtract(0.0,
                0.5 * FALL * tau * tau, 0.0);
        Vec3 blade = tossTurn(SPIN * tau, UPRIGHT);
        return new Flight(balance.subtract(blade.scale(OWN_BALANCE)), blade, tossTurn(SPIN * tau, FLAT));
    }

    // ---- Seen from outside ----

    /** A place before your eyes in first person as a place before the body (see {@link #BODY_ACROSS}). */
    static Vec3 body(Vec3 view) {
        return new Vec3(view.x * BODY_ACROSS, view.y * BODY_UP + BODY_RAISE, -view.z * BODY_AHEAD - BODY_BACK);
    }

    /** A way before your eyes in first person as a way seen from the body (x to his right, y up, z ahead). */
    static Vec3 way(Vec3 view) {
        return new Vec3(view.x, view.y, -view.z);
    }

    /** A place before the body as seen from the upper body turned {@code twist} to his right. */
    static Vec3 unturned(Vec3 at, float twist) {
        double cos = Mth.cos(twist);
        double sin = Mth.sin(twist);
        return new Vec3(at.x * cos - at.z * sin, at.y, at.x * sin + at.z * cos);
    }

    /** A place before the upper body as it is before the game bends it forward by {@code tilt} (radians). */
    static Vec3 unbent(Vec3 at, float tilt) {
        double cos = Mth.cos(tilt);
        double sin = Mth.sin(tilt);
        return new Vec3(at.x, at.y * cos + at.z * sin, -at.y * sin + at.z * cos);
    }

    /**
     * Where an arm of a body seen from outside points for this pose, as the model's turns of it about x and about y
     * (before the upper body bends forward): the right arm at the fist, the left one at the middle of the shield, each
     * seen from its shoulder on the upper body as that turns into the move.
     */
    static float[] aim(Pose pose, boolean right) {
        float twist = pose.twist();
        float tilt = TILT * Mth.clamp(pose.lean(), -0.35F, 1.0F);
        Vec3 target = unbent(unturned(body(right ? pose.hand() : pose.shield()), twist), tilt);
        Vec3 reach = target.subtract(right ? SHOULDER : -SHOULDER, 0.0, 0.0);
        reach = reach.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : reach.normalize();
        float pitch = (float) Math.asin(Mth.clamp(reach.y, -1.0, 1.0));
        float yaw = (float) Mth.atan2(reach.x, reach.z);
        return new float[] { -(Mth.HALF_PI + pitch), yaw + twist };
    }

    /**
     * Where the fist of a body seen from outside grips for this pose (or, not {@code right}, where the shield sits on
     * the left forearm), worked out the way the model draws its arms: in blocks from the middle of the chest at the
     * height of the shoulders, x to his right, y up, z ahead, at the model's own size. The game's own swing of the arms
     * as he walks is left out.
     */
    static Vec3 drawn(Pose pose, boolean right) {
        float[] aim = aim(pose, right);
        float lean = Mth.clamp(pose.lean(), -0.35F, 1.0F);
        double xRot = aim[0] + TILT * lean;
        double yRot = aim[1];
        Vec3 local = right ? FIST : FOREARM;
        double cx = Math.cos(xRot);
        double sx = Math.sin(xRot);
        Vec3 bent = new Vec3(local.x, local.y * cx - local.z * sx, local.y * sx + local.z * cx);
        double cy = Math.cos(yRot);
        double sy = Math.sin(yRot);
        Vec3 turned = new Vec3(bent.x * cy + bent.z * sy, bent.y, -bent.x * sy + bent.z * cy);
        double side = right ? -1.0 : 1.0;
        double twist = pose.twist();
        Vec3 pivot = new Vec3(side * Math.cos(twist) * PIVOT_ACROSS, PIVOT_DOWN + BEND_DOWN * Math.max(0.0F, lean),
                -side * Math.sin(twist) * PIVOT_ACROSS);
        Vec3 model = pivot.add(turned).scale(1.0 / 16.0);
        return new Vec3(-model.x, PIVOT_DOWN / 16.0 - model.y, -model.z);
    }

    /**
     * Where the fist of a body seen from outside grips while he takes them out, {@code t} ticks in (see {@link
     * #drawn}).
     */
    private static Vec3 drawnGrip(float t) {
        return drawn(at(SwordMove.EQUIP, t, 0.0F, REST, null), true);
    }

    /** How fast the fist of a body seen from outside moves while he takes them out, {@code t} ticks in, per tick. */
    private static Vec3 drawnSpeed(float t) {
        float step = 0.02F;
        return drawnGrip(t + step).subtract(drawnGrip(t - step)).scale(0.5 / step);
    }

    // The toss seen from outside, from the middle of the chest: how far the balance point is from the grip, and how high
    // it rises over where it leaves the drawn fist (a real toss, well over his head: about as high as a thing thrown up
    // rises in the time it flies). Worked out below: where it leaves the fist, how fast it flies off and how fast it
    // falls, so it comes down right into the fist where it catches it; and how much faster than the grip of the sword
    // flying freely the fist moves as it lets go and as it catches, which the grip takes along for a moment (ticks).
    private static final double BODY_BALANCE = BALANCE * SwordPainter.TIP * SWORD_SCALE;
    private static final double BODY_TOSS_HIGH = 1.0;
    private static final Vec3 BODY_THROWN_FROM;
    private static final Vec3 BODY_THROWN;
    private static final double BODY_FALL;
    private static final Vec3 BODY_LET_GO;
    private static final Vec3 BODY_CAUGHT;
    private static final double HANDOFF = 2.0;
    // How far the blade seen from outside is turned so it strikes the rim of the shield at each bang (a way times its
    // angle), and where on the rim it strikes the first time, from the middle of the chest; and how long it takes to
    // turn onto the rim before the first bang and back off it after the second, in ticks.
    private static final Vec3 BODY_KNOCK_TURN;
    private static final Vec3 BODY_KNOCK_AGAIN_TURN;
    static final Vec3 BODY_STRUCK;
    private static final float KNOCK_IN = 4.0F;
    private static final float KNOCK_OUT = 5.0F;

    static {
        // The balance point flies the curve a thrown thing flies, from where it leaves the fist to where it lands in
        // it; the grip flying freely moves as the balance point does, less the turn of the blade about it.
        double time = SwordMove.CATCH - SwordMove.TOSS;
        Vec3 spun = way(BLADE_TURN).scale(BODY_BALANCE);
        Vec3 standing = way(UPRIGHT).scale(BODY_BALANCE);
        BODY_THROWN_FROM = drawnGrip(SwordMove.TOSS).add(standing);
        Vec3 lands = drawnGrip(SwordMove.CATCH).add(standing);
        BODY_FALL = fall(BODY_TOSS_HIGH, (lands.y - BODY_THROWN_FROM.y) / time, time);
        BODY_THROWN = lands.subtract(BODY_THROWN_FROM).scale(1.0 / time).add(0.0, BODY_FALL * time / 2.0, 0.0);
        BODY_LET_GO = drawnSpeed(SwordMove.TOSS).subtract(BODY_THROWN.subtract(spun));
        BODY_CAUGHT = drawnSpeed(SwordMove.CATCH).subtract(BODY_THROWN.subtract(0.0, BODY_FALL * time, 0.0)
                .subtract(spun));
        Vec3[] first = bodyKnock(SwordMove.KNOCK);
        BODY_KNOCK_TURN = first[0];
        BODY_STRUCK = first[1];
        BODY_KNOCK_AGAIN_TURN = bodyKnock(SwordMove.KNOCK_AGAIN)[0];
    }

    /**
     * The tossed sword seen from outside, {@code t} ticks into taking them out, from the middle of the chest: its
     * balance point flies freely from the drawn fist high over his head and falls back into the fist where it catches
     * it, while it turns over the same as before your own eyes. Just after the fist lets go of it and just before it
     * catches it again the grip still goes a little with the fist, so it leaves and lands at the speed the fist moves.
     * Past the catch it flies on.
     */
    static Flight bodyFlight(float t) {
        double tau = t - SwordMove.TOSS;
        Vec3 balance = BODY_THROWN_FROM.add(BODY_THROWN.scale(tau)).subtract(0.0, 0.5 * BODY_FALL * tau * tau, 0.0);
        Vec3 blade = way(tossTurn(SPIN * tau, UPRIGHT));
        Vec3 grip = balance.subtract(blade.scale(BODY_BALANCE)).add(BODY_LET_GO.scale(handoff(tau)))
                .subtract(BODY_CAUGHT.scale(handoff(SwordMove.CATCH - t)));
        return new Flight(grip, blade, way(tossTurn(SPIN * tau, FLAT)));
    }

    /**
     * How far the grip of the tossed sword seen from outside still goes along with the fist {@code since} ticks after
     * the fist let go of it (or before it catches it), times the difference in speed: leaving (or landing) exactly as
     * fast as the fist, then less and less, none after {@link #HANDOFF}. The other way round past the catch (a sword
     * that broke up in the air flies on), so it flies on smoothly there too.
     */
    private static double handoff(double since) {
        if (Math.abs(since) >= HANDOFF) {
            return 0.0;
        }
        double left = 1.0 - Math.abs(since) / HANDOFF;
        return since * left * left;
    }

    // ---- Banging it on the shield ----

    /**
     * Where the fist is as the blade comes down on the rim of the shield, before your eyes: the edge of the blade just
     * touches the rim there, {@link #STRIKE_AT} along it.
     */
    private static Vec3 contact() {
        double clear = RIM_THICK * OWN_SHIELD + BLADE_WIDE * OWN_SWORD;
        return STRUCK.subtract(KNOCK_EDGE.scale(clear)).subtract(KNOCKING.scale(STRIKE_AT * OWN_SWORD));
    }

    /** Where the blade strikes the rim of the shield before your eyes at a bang {@code t} ticks in. */
    private static Vec3 struck(float t) {
        Pose shield = Pose.of(raised(t, 0.0).numbers());
        return rim(shield.shield(), shield.shieldRight(), shield.top(), OWN_SHIELD);
    }

    /**
     * Where the blade strikes the rim of a shield of scale {@code scale} at a bang: {@code middle} is its middle,
     * {@code right} and {@code top} its own right and top.
     */
    static Vec3 rim(Vec3 middle, Vec3 right, Vec3 top, double scale) {
        return middle.add(right.scale(RIM_X * scale)).add(top.scale(RIM_Y * scale));
    }

    /**
     * A bang seen from outside, {@code t} ticks in: the turn (a way times its angle) that brings the blade, as it is
     * drawn in the fist, onto the rim of the shield as it is drawn on the forearm, and where on the rim it strikes.
     */
    private static Vec3[] bodyKnock(float t) {
        Pose pose = at(SwordMove.EQUIP, t, 0.0F, REST, null);
        Vec3[] strike = bodyStrike(pose);
        return new Vec3[] { turnOnto(way(pose.blade()), strike[1]), strike[0] };
    }

    /**
     * Where the blade of a body seen from outside strikes the rim of the shield at a bang, for this pose, as the model
     * draws the arms (from the middle of the chest, see {@link #drawn}); and the way from the fist to where the blade
     * has to lie to strike it there with its edge.
     */
    static Vec3[] bodyStrike(Pose pose) {
        Vec3 grip = drawn(pose, true);
        Vec3 face = way(pose.face());
        Vec3 top = square(way(pose.top()), face);
        // The body's own ways are a mirror image of the world's, so its right is worked out the other way round.
        Vec3 right = top.cross(face).normalize();
        Vec3 middle = drawn(pose, false).add(face.scale(SHIELD_OUT));
        Vec3 struck = rim(middle, right, top, SHIELD_SCALE);
        // The edge that leads down onto the rim (the pose may hold the blade by its other edge: it looks the same).
        Vec3 edge = square(way(KNOCK_EDGE), way(pose.blade()));
        Vec3 aim = struck.subtract(edge.scale(RIM_THICK * SHIELD_SCALE + BLADE_WIDE * SWORD_SCALE)).subtract(grip);
        return new Vec3[] { struck, aim };
    }

    /** The turn (a way times its angle, radians) that brings the way {@code from} round onto the way {@code to}. */
    static Vec3 turnOnto(Vec3 from, Vec3 to) {
        Vec3 axis = from.cross(to);
        double angle = Math.atan2(axis.length(), from.dot(to));
        return axis.lengthSqr() < 1.0E-12 ? Vec3.ZERO : axis.normalize().scale(angle);
    }

    /**
     * How far the blade seen from outside is turned (a way times its angle) {@code t} ticks into taking them out, so
     * that it comes down right on the rim at each bang: all of it at the bangs, eased in and out round them.
     */
    static Vec3 bodyKnockTurn(float t) {
        float knock = SwordMove.KNOCK;
        float again = SwordMove.KNOCK_AGAIN;
        double first;
        double second;
        if (t <= knock) {
            first = Ease.smooth((t - (knock - KNOCK_IN)) / KNOCK_IN);
            second = 0.0;
        } else if (t <= again) {
            second = Ease.smooth((t - knock) / (again - knock));
            first = 1.0 - second;
        } else {
            first = 0.0;
            second = 1.0 - Ease.smooth((t - again) / KNOCK_OUT);
        }
        return BODY_KNOCK_TURN.scale(first).add(BODY_KNOCK_AGAIN_TURN.scale(second));
    }

    /**
     * How much of the way onto the rim of the shield the blade seen from outside is turned {@code t} ticks into taking
     * them out, 0 to 1 (see {@link #bodyKnockTurn}).
     */
    static float bodyKnocking(float t) {
        if (t <= SwordMove.KNOCK) {
            return (float) Ease.smooth((t - (SwordMove.KNOCK - KNOCK_IN)) / KNOCK_IN);
        }
        return t <= SwordMove.KNOCK_AGAIN ? 1.0F
                : (float) (1.0 - Ease.smooth((t - SwordMove.KNOCK_AGAIN) / KNOCK_OUT));
    }

    /** {@code way} turned by {@code turn} (a way times its angle, radians). */
    static Vec3 turnedBy(Vec3 way, Vec3 turn) {
        double angle = turn.length();
        return angle < 1.0E-9 ? way : Vectors.spin(way, turn.scale(1.0 / angle), angle);
    }

    // ---- Where he looks ----

    /**
     * How far your own eyes follow the sword while you take them out, {@code t} ticks in, as a turn up and a turn to
     * the right (radians): up after the tossed sword (a moment ahead of it, the way eyes lead), at the blade as you
     * look it over, and a little at the rim of the shield as you bang it. {@code pose} is the pose right now.
     */
    static float[] look(float t, Pose pose) {
        float[] look = new float[2];
        double toss = Ease.smooth((t - (SwordMove.TOSS - 3.0F)) / 5.0F)
                * (1.0 - Ease.smooth((t - (SwordMove.CATCH - 4.0F)) / 7.0F));
        if (toss > 0.0) {
            glance(look, balance(t + LEAD), toss, 0.8, 0.35, 25.0);
        }
        double inspect = Ease.smooth((t - (INSPECT_FROM - 4.0F)) / 5.0F)
                * (1.0 - Ease.smooth((t - INSPECT_TO) / 5.0F));
        if (inspect > 0.0) {
            glance(look, pose.hand().add(pose.blade().scale(0.55 * SwordPainter.TIP * OWN_SWORD)), inspect, 0.45,
                    0.45, 16.0);
        }
        double bang = Ease.smooth((t - (SwordMove.KNOCK - 5.0F)) / 4.0F)
                * (1.0 - Ease.smooth((t - (SwordMove.KNOCK_AGAIN + 1.0F)) / 6.0F));
        if (bang > 0.0) {
            glance(look, STRUCK, bang, 0.3, 0.3, 10.0);
        }
        return look;
    }

    /**
     * Where the balance point of the sword is {@code t} ticks into taking them out, before your eyes: in the hand, or
     * in the air while it is tossed.
     */
    private static Vec3 balance(float t) {
        if (tossed(SwordMove.EQUIP, t) >= 0.0F) {
            Flight flight = flight(t);
            return flight.grip().add(flight.blade().scale(OWN_BALANCE));
        }
        Pose pose = at(SwordMove.EQUIP, t, 0.0F, REST, null);
        return pose.hand().add(pose.blade().scale(OWN_BALANCE));
    }

    /** Where the balance point of the sword is {@code t} ticks into taking them out, seen from outside (see above). */
    private static Vec3 bodyBalance(float t) {
        if (tossed(SwordMove.EQUIP, t) >= 0.0F) {
            Flight flight = bodyFlight(t);
            return flight.grip().add(flight.blade().scale(BODY_BALANCE));
        }
        Pose pose = at(SwordMove.EQUIP, t, 0.0F, REST, null);
        return drawn(pose, true).add(way(pose.blade()).scale(BODY_BALANCE));
    }

    /**
     * Adds a glance at {@code at} (before your eyes) to {@code look}: this much of the way up and across, at most so
     * far.
     */
    private static void glance(float[] look, Vec3 at, double weight, double up, double across, double most) {
        double ahead = Math.max(0.1, -at.z);
        double limit = most * Mth.DEG_TO_RAD;
        look[0] += (float) (weight * Mth.clamp(Math.atan2(at.y, ahead) * up, -limit, limit));
        look[1] += (float) (weight * Mth.clamp(Math.atan2(at.x, ahead) * across, -limit, limit));
    }

    /**
     * How his head turns seen from outside while he takes them out, {@code t} ticks in: how far down and to his right
     * it looks (radians), and how much of its own way it gives up for that (0 to 1). He watches the sword go up and
     * come down, looks the blade over, and nods with the bangs. {@code pose} is the pose right now.
     */
    static float[] head(float t, Pose pose) {
        float[] head = new float[3];
        double toss = Ease.smooth((t - (SwordMove.TOSS - 3.0F)) / 5.0F)
                * (1.0 - Ease.smooth((t - (SwordMove.CATCH - 3.0F)) / 7.0F));
        double inspect = Ease.smooth((t - (INSPECT_FROM - 4.0F)) / 5.0F)
                * (1.0 - Ease.smooth((t - INSPECT_TO) / 5.0F));
        double bang = Ease.smooth((t - (SwordMove.KNOCK - 5.0F)) / 4.0F)
                * (1.0 - Ease.smooth((t - (SwordMove.KNOCK_AGAIN + 2.0F)) / 6.0F));
        double weight = 0.0;
        if (toss > 0.0) {
            weight += watch(head, bodyBalance(t + LEAD), toss * 0.9);
        }
        if (inspect > 0.0) {
            Vec3 blade = way(pose.blade()).scale(0.55 * SwordPainter.TIP * SWORD_SCALE);
            weight += watch(head, drawn(pose, true).add(blade), inspect * 0.75);
        }
        if (bang > 0.0) {
            weight += watch(head, BODY_STRUCK, bang * 0.5);
        }
        if (weight > 1.0E-6) {
            head[0] /= (float) weight;
            head[1] /= (float) weight;
        }
        head[2] = (float) Math.min(1.0, weight);
        // A nod with each bang.
        head[0] += 0.12F * (nod(t - SwordMove.KNOCK) + 0.7F * nod(t - SwordMove.KNOCK_AGAIN));
        return head;
    }

    /** Adds the way to {@code at} (from the middle of the chest) to {@code head}, weighed; returns the weight. */
    private static double watch(float[] head, Vec3 at, double weight) {
        Vec3 to = at.subtract(EYES);
        head[0] += (float) (weight * -Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        head[1] += (float) (weight * Math.atan2(to.x, to.z));
        return weight;
    }

    /** A quick nod {@code since} ticks after a bang: jolted down by it at once, and back up over a few ticks. */
    private static float nod(float since) {
        if (since < 0.0F || since > 5.0F) {
            return 0.0F;
        }
        float u = since / 5.0F;
        return 6.75F * u * (1.0F - u) * (1.0F - u);
    }

    /**
     * How far a gleam of light has run up the blade as he looks it over, 0 (at the guard) to 1 (at the tip), or -1 when
     * none runs.
     */
    static float gleam(SwordMove move, float t) {
        if (move != SwordMove.EQUIP) {
            return -1.0F;
        }
        float u = (t - SwordMove.GLEAM) / 7.0F;
        return u < 0.0F || u > 1.0F ? -1.0F : u;
    }

    /**
     * The pose with the edge of the blade turned into the way the blade sweeps, the faster the more: a cut always leads
     * with its edge, whichever way it goes. Of the two edges the one nearer to where the keys put it leads, so the blade
     * never flips over. {@code before} is the pose a moment earlier; {@code amount} (0 to 1) is how much of that turn it
     * takes.
     */
    static Pose led(Pose now, Pose before, float amount) {
        Vec3 tip = now.hand().add(now.blade());
        Vec3 was = before.hand().add(before.blade());
        Vec3 sweep = tip.subtract(was);
        Vec3 across = sweep.subtract(now.blade().scale(sweep.dot(now.blade())));
        double speed = across.length();
        if (speed < 1.0E-3) {
            return now;
        }
        Vec3 lead = across.scale(1.0 / speed);
        if (lead.dot(now.edge()) < 0.0) {
            lead = lead.scale(-1.0);
        }
        double w = amount * (float) Ease.smooth((float) (speed / 0.12));
        return now.holding(now.blade(), square(now.edge().lerp(lead, w), now.blade()));
    }

    // ---- Ways ----

    /** {@code way} made one long, or {@code otherwise} when it has hardly any length. */
    private static Vec3 unit(Vec3 way, Vec3 otherwise) {
        double length = way.length();
        return length < 1.0E-4 ? otherwise : way.scale(1.0 / length);
    }

    /** {@code way} with the part along {@code axis} taken out, made one long: square to that axis. */
    static Vec3 square(Vec3 way, Vec3 axis) {
        Vec3 flat = way.subtract(axis.scale(way.dot(axis)));
        if (flat.lengthSqr() < 1.0E-6) {
            Vec3 other = Math.abs(axis.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            flat = other.subtract(axis.scale(other.dot(axis)));
        }
        return flat.normalize();
    }

    /** {@code way} turned {@code angle} (radians) to the left about the upright line: +x goes towards -z. */
    static Vec3 spin(Vec3 way, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(way.x * cos + way.z * sin, way.y, -way.x * sin + way.z * cos);
    }

    private static float wrap(float radians) {
        return Mth.wrapDegrees(radians * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }
}
