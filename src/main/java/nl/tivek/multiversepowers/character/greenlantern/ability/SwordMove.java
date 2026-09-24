package nl.tivek.multiversepowers.character.greenlantern.ability;

import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import nl.tivek.multiversepowers.engine.math.Noise;

/**
 * Everything the hard-light sword and shield (see {@link SwordShield}) can do, the same on the server, which strikes,
 * and on every client, which plays it: how long each move takes, from when the next one may follow, how hard and how
 * far it strikes, over which arc in front of him, and on which ticks it lands.
 *
 * <p>The sword has twelve cuts and thrusts; every click picks one of them at random, never the same twice in a row, so a
 * fight never looks the same. Every move starts from wherever the last one left his arms, so they flow into each other.
 * The shield has six rams, one of which he throws at every creature in his way while he charges behind it.
 */
public enum SwordMove {
    // ---- The sword (left click) ----
    /** A cut across, from his right to his left. */
    SLASH(Kind.ATTACK, 14, 10, 1.0, 3.2, -75, 75, 6),
    /** A backhand cut across, from his left to his right. */
    BACKHAND(Kind.ATTACK, 14, 10, 1.0, 3.2, -75, 75, 6),
    /** A cut down from high on his right to low on his left. */
    CLEAVE(Kind.ATTACK, 15, 11, 1.1, 3.2, -60, 60, 7),
    /** A cut down from high on his left to low on his right. */
    REVERSE_CLEAVE(Kind.ATTACK, 15, 11, 1.1, 3.2, -60, 60, 7),
    /** A cut up from low on his left to high on his right. */
    RISING(Kind.ATTACK, 14, 10, 1.0, 3.2, -60, 60, 6),
    /** An uppercut: straight up from below, throwing what it strikes into the air. */
    UPPERCUT(Kind.ATTACK, 15, 11, 1.15, 3.0, -35, 35, 7),
    /** A chop straight down from high over his head, with a step into it: slow, and the hardest cut. */
    OVERHEAD(Kind.ATTACK, 18, 13, 1.35, 3.4, -30, 30, 9),
    /** A quick thrust straight ahead: short, and reaching further. */
    STAB(Kind.ATTACK, 11, 8, 0.9, 3.9, -22, 22, 5),
    /** A long step forward and a deep thrust. */
    LUNGE(Kind.ATTACK, 17, 12, 1.25, 4.6, -20, 20, 8),
    /** Down low on one knee, a cut at the legs from his right to his left. */
    LOW_SWEEP(Kind.ATTACK, 16, 12, 0.9, 3.3, -80, 80, 8),
    /** A whole turn with the blade held out: it strikes all round him. */
    SPIN(Kind.ATTACK, 19, 15, 1.2, 3.3, -180, 180, 10),
    /** Two quick cuts crossing in an X: two strikes, each a little softer. */
    CROSS(Kind.ATTACK, 17, 13, 0.75, 3.2, -55, 55, 5, 11),
    // ---- The shield: six ways to ram what stands in the way of a charge ----
    /** The face of the shield punched straight out: sends it flying ahead and aside. */
    BASH(Kind.BASH, 9, 9, 1.0, 1.6, -90, 90, 2),
    /** The shield swung out to his right: sweeps it off to his right. */
    BASH_SWEEP(Kind.BASH, 9, 9, 1.0, 1.6, -90, 90, 2),
    /** The shield swung out to his left: sweeps it off to his left. */
    BASH_BACKHAND(Kind.BASH, 9, 9, 1.0, 1.6, -90, 90, 2),
    /** The shield driven up from under it: throws it up and aside. */
    BASH_UP(Kind.BASH, 9, 9, 1.0, 1.6, -90, 90, 2),
    /** The rim of the shield brought down on it: knocks it down aside and slows it. */
    BASH_DOWN(Kind.BASH, 10, 10, 1.2, 1.6, -90, 90, 3),
    /** A shoulder turned into it behind the shield: bowls it over hardest of all. */
    BASH_SPIN(Kind.BASH, 10, 10, 1.1, 1.6, -90, 90, 3),
    // ---- Holding a button ----
    /** The sword held 2 seconds: the shield before his chest and twelve quick stabs all round the front. */
    FLURRY(Kind.FLURRY, 44, 44, 1.0, 3.4, -45, 45),
    /** The shield clicked: bent forward behind it, he charges straight ahead. */
    CHARGE(Kind.CHARGE, 1000, 1000, 1.0, 1.6, -80, 80),
    /** The end of a charge: the shield slammed into the ground for a small shockwave. */
    SLAM(Kind.SLAM, 16, 13, 1.0, 3.5, -180, 180, 7),
    // ---- Taking them out ----
    /**
     * The sword and shield take shape: the sword grows out of his fist and the shield on his forearm as both come up, he
     * holds the sword up before his eyes and turns it to see both flats, twirls it once round like a wheel and knocks it
     * twice on the rim of the shield.
     */
    EQUIP(Kind.EQUIP, 36, 32, 0.0, 0.0, 0, 0);

    /** What a move is. */
    public enum Kind {
        ATTACK, BASH, FLURRY, CHARGE, SLAM, EQUIP
    }

    /**
     * The ticks of taking them out: the twirl of the sword begins and ends, and it is knocked on the shield (the second
     * knock three ticks after the first).
     */
    public static final int TWIRL = 12;
    public static final int TWIRLED = 20;
    public static final int KNOCK = 27;
    /** How many stabs the flurry has, how many ticks apart they come, and the tick the first one lands on. */
    public static final int STABS = 12;
    public static final int STAB_EVERY = 3;
    public static final int FIRST_STAB = 5;
    /** What the move number is sent with: whether he holds the shield up, and whether he charges. */
    public static final int BLOCKING = 32;
    public static final int CHARGING = 64;
    private static final int MOVE_BITS = 31;

    private static final SwordMove[] ATTACKS = { SLASH, BACKHAND, CLEAVE, REVERSE_CLEAVE, RISING, UPPERCUT, OVERHEAD,
            STAB, LUNGE, LOW_SWEEP, SPIN, CROSS };
    // The rams that throw a creature off to his right, and those that throw it off to his left.
    private static final SwordMove[] RAMS_RIGHT = { BASH, BASH_SWEEP, BASH_UP };
    private static final SwordMove[] RAMS_LEFT = { BASH_BACKHAND, BASH_DOWN, BASH_SPIN };

    private final Kind kind;
    private final int ticks;
    private final int ready;
    private final double power;
    private final double reach;
    private final int arcFrom;
    private final int arcTo;
    private final int[] hits;

    SwordMove(Kind kind, int ticks, int ready, double power, double reach, int arcFrom, int arcTo, int... hits) {
        this.kind = kind;
        this.ticks = ticks;
        this.ready = ready;
        this.power = power;
        this.reach = reach;
        this.arcFrom = arcFrom;
        this.arcTo = arcTo;
        this.hits = hits;
    }

    public Kind kind() {
        return this.kind;
    }

    /** How long the move takes, in ticks. */
    public int ticks() {
        return this.ticks;
    }

    /** The tick of the move from which the next one may start, flowing on from this one. */
    public int ready() {
        return this.ready;
    }

    /** How hard it strikes, next to the usual damage. */
    public double power() {
        return this.power;
    }

    /** How far it reaches from his middle, in blocks. */
    public double reach() {
        return this.reach;
    }

    /**
     * Whether a creature this far round from the way he faces is in the arc the move sweeps, in degrees: negative to
     * his left, positive to his right.
     */
    public boolean inArc(double degrees) {
        return degrees >= this.arcFrom && degrees <= this.arcTo;
    }

    /** The ticks of the move it strikes on. */
    public int[] hits() {
        return this.hits;
    }

    /**
     * True while this move, {@code t} ticks in, swings the sword: a cut, a thrust or the flurry. The shield cannot block
     * meanwhile; held up, it comes back once the move is over.
     */
    public boolean swings(double t) {
        return (this.kind == Kind.ATTACK || this.kind == Kind.FLURRY) && t >= 0.0 && t < this.ticks;
    }

    /** One of the sword's twelve moves at random, never {@code last} again. */
    public static SwordMove randomAttack(RandomSource random, @Nullable SwordMove last) {
        return pick(ATTACKS, random, last);
    }

    /**
     * One of the shield's rams for a creature in the way of a charge, never {@code last} again: one that throws it off
     * to the side of him it stands on ({@code right} true for his right).
     */
    public static SwordMove randomRam(RandomSource random, boolean right, @Nullable SwordMove last) {
        return pick(right ? RAMS_RIGHT : RAMS_LEFT, random, last);
    }

    private static SwordMove pick(SwordMove[] moves, RandomSource random, @Nullable SwordMove last) {
        SwordMove move;
        do {
            move = moves[random.nextInt(moves.length)];
        } while (move == last);
        return move;
    }

    /** The move with this number, or null. */
    @Nullable
    public static SwordMove byIndex(int index) {
        SwordMove[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }

    /** The move a number sent with {@link #BLOCKING} and {@link #CHARGING} stands for, or null. */
    @Nullable
    public static SwordMove sent(int number) {
        return byIndex(number & MOVE_BITS);
    }

    /**
     * Which way stab {@code k} of a flurry goes, as degrees round from the way he faces (negative to his left) and up
     * (negative down): spread all over the front, the same on the server and on every client.
     */
    public static double[] stab(int k) {
        double side = (Noise.of(k, 7, 311) - 0.5) * 80.0;
        double up = (Noise.of(k, 7, 312) - 0.45) * 36.0;
        return new double[] { side, up };
    }
}
