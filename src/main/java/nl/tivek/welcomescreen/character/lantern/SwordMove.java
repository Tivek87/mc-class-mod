package nl.tivek.welcomescreen.character.lantern;

import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;

/**
 * Everything the hard-light sword and shield (see {@link SwordShield}) can do, the same on the server, which strikes,
 * and on every client, which plays it: how long each move takes, from when the next one may follow, how hard and how
 * far it strikes, over which arc in front of him, and on which ticks it lands.
 *
 * <p>The sword has twelve cuts and thrusts and the shield six bashes; every click picks one of them at random, never the
 * same twice in a row, so a fight never looks the same. Every move starts from wherever the last one left his arms, so
 * they flow into each other.
 */
public enum SwordMove {
    // ---- The sword (left click) ----
    /** A cut across, from his right to his left. */
    SLASH(Kind.ATTACK, 12, 9, 1.0, 3.2, -70, 70, 6),
    /** A backhand cut across, from his left to his right. */
    BACKHAND(Kind.ATTACK, 12, 9, 1.0, 3.2, -70, 70, 6),
    /** A cut down from high on his right to low on his left. */
    CLEAVE(Kind.ATTACK, 13, 10, 1.1, 3.2, -55, 55, 7),
    /** A cut down from high on his left to low on his right. */
    REVERSE_CLEAVE(Kind.ATTACK, 13, 10, 1.1, 3.2, -55, 55, 7),
    /** A cut up from low on his left to high on his right. */
    RISING(Kind.ATTACK, 12, 9, 1.0, 3.2, -60, 60, 6),
    /** An uppercut: straight up from below, throwing what it strikes into the air. */
    UPPERCUT(Kind.ATTACK, 13, 10, 1.15, 3.0, -35, 35, 7),
    /** A chop straight down from high over his head: slow, and the hardest cut. */
    OVERHEAD(Kind.ATTACK, 15, 12, 1.35, 3.3, -30, 30, 9),
    /** A quick thrust straight ahead: short, and reaching further. */
    STAB(Kind.ATTACK, 10, 7, 0.9, 3.9, -22, 22, 5),
    /** A step forward and a long thrust. */
    LUNGE(Kind.ATTACK, 16, 12, 1.25, 4.6, -20, 20, 8),
    /** Down low, a cut at the legs from his right to his left. */
    LOW_SWEEP(Kind.ATTACK, 13, 10, 0.9, 3.3, -75, 75, 7),
    /** A whole turn with the blade held out: it strikes all round him. */
    SPIN(Kind.ATTACK, 18, 14, 1.2, 3.3, -180, 180, 9),
    /** Two quick cuts crossing in an X: two strikes, each a little softer. */
    CROSS(Kind.ATTACK, 16, 13, 0.75, 3.2, -50, 50, 5, 10),
    // ---- The shield (right click) ----
    /** The shield rammed straight ahead: shoves straight away. */
    BASH(Kind.BASH, 11, 9, 1.0, 2.6, -45, 45, 5),
    /** The shield swung across from his left to his right: shoves to his right. */
    BASH_SWEEP(Kind.BASH, 12, 10, 1.0, 2.6, -70, 70, 6),
    /** The shield swung back across to his left: shoves to his left. */
    BASH_BACKHAND(Kind.BASH, 12, 10, 1.0, 2.6, -70, 70, 6),
    /** The shield driven up from below: throws up into the air. */
    BASH_UP(Kind.BASH, 12, 10, 1.0, 2.5, -40, 40, 6),
    /** The shield raised and brought down edge first: knocks down and away, harder. */
    BASH_DOWN(Kind.BASH, 13, 11, 1.2, 2.5, -40, 40, 7),
    /** A turn, and the face of the shield into everything round him. */
    BASH_SPIN(Kind.BASH, 15, 12, 1.1, 2.8, -180, 180, 8),
    // ---- Holding a button ----
    /** The sword held 2 seconds: the shield before his chest and twelve quick stabs all round the front. */
    FLURRY(Kind.FLURRY, 42, 42, 1.0, 3.4, -45, 45),
    /** The shield held 2 seconds: bent forward behind it, he charges straight ahead. */
    CHARGE(Kind.CHARGE, 1000, 1000, 1.0, 1.6, -80, 80),
    /** The end of a charge: the shield slammed into the ground for a small shockwave. */
    SLAM(Kind.SLAM, 14, 12, 1.0, 3.5, -180, 180, 7),
    // ---- Taking them out ----
    /** The sword and shield take shape: the sword tossed up spinning and caught, and knocked on the shield. */
    EQUIP(Kind.EQUIP, 32, 32, 0.0, 0.0, 0, 0);

    /** What a move is. */
    public enum Kind {
        ATTACK, BASH, FLURRY, CHARGE, SLAM, EQUIP
    }

    /** How many stabs the flurry has, how many ticks apart they come, and the tick the first one lands on. */
    public static final int STABS = 12;
    public static final int STAB_EVERY = 3;
    public static final int FIRST_STAB = 3;

    private static final SwordMove[] ATTACKS = { SLASH, BACKHAND, CLEAVE, REVERSE_CLEAVE, RISING, UPPERCUT, OVERHEAD,
            STAB, LUNGE, LOW_SWEEP, SPIN, CROSS };
    private static final SwordMove[] BASHES = { BASH, BASH_SWEEP, BASH_BACKHAND, BASH_UP, BASH_DOWN, BASH_SPIN };

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

    /** One of the sword's twelve moves at random, never {@code last} again. */
    public static SwordMove randomAttack(RandomSource random, @Nullable SwordMove last) {
        return pick(ATTACKS, random, last);
    }

    /** One of the shield's six bashes at random, never {@code last} again. */
    public static SwordMove randomBash(RandomSource random, @Nullable SwordMove last) {
        return pick(BASHES, random, last);
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

    /**
     * Which way stab {@code k} of a flurry goes, as degrees round from the way he faces (negative to his left) and up
     * (negative down): spread all over the front, the same on the server and on every client.
     */
    public static double[] stab(int k) {
        double side = (PlanePath.noise(k, 7, 311) - 0.5) * 80.0;
        double up = (PlanePath.noise(k, 7, 312) - 0.45) * 36.0;
        return new double[] { side, up };
    }
}
