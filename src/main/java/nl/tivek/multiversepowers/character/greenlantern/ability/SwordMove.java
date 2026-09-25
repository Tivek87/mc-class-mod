package nl.tivek.multiversepowers.character.greenlantern.ability;

import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import nl.tivek.multiversepowers.engine.math.Noise;

public enum SwordMove {
    SLASH(Kind.ATTACK, 14, 10, 1.0, 3.2, -75, 75, 6),
    BACKHAND(Kind.ATTACK, 14, 10, 1.0, 3.2, -75, 75, 6),
    CLEAVE(Kind.ATTACK, 15, 11, 1.1, 3.2, -60, 60, 7),
    REVERSE_CLEAVE(Kind.ATTACK, 15, 11, 1.1, 3.2, -60, 60, 7),
    RISING(Kind.ATTACK, 14, 10, 1.0, 3.2, -60, 60, 6),
    UPPERCUT(Kind.ATTACK, 15, 11, 1.15, 3.0, -35, 35, 7),
    OVERHEAD(Kind.ATTACK, 18, 13, 1.35, 3.4, -30, 30, 9),
    STAB(Kind.ATTACK, 11, 8, 0.9, 3.9, -22, 22, 5),
    LUNGE(Kind.ATTACK, 17, 12, 1.25, 4.6, -20, 20, 8),
    LOW_SWEEP(Kind.ATTACK, 16, 12, 0.9, 3.3, -80, 80, 8),
    SPIN(Kind.ATTACK, 19, 15, 1.2, 3.3, -180, 180, 10),
    CROSS(Kind.ATTACK, 17, 13, 0.75, 3.2, -55, 55, 5, 11),
    BASH(Kind.BASH, 9, 9, 1.0, 1.6, -90, 90, 2),
    BASH_SWEEP(Kind.BASH, 9, 9, 1.0, 1.6, -90, 90, 2),
    BASH_BACKHAND(Kind.BASH, 9, 9, 1.0, 1.6, -90, 90, 2),
    BASH_UP(Kind.BASH, 9, 9, 1.0, 1.6, -90, 90, 2),
    BASH_DOWN(Kind.BASH, 10, 10, 1.2, 1.6, -90, 90, 3),
    BASH_SPIN(Kind.BASH, 10, 10, 1.1, 1.6, -90, 90, 3),
    FLURRY(Kind.FLURRY, 44, 44, 1.0, 3.4, -45, 45),
    CHARGE(Kind.CHARGE, 1000, 1000, 1.0, 1.6, -80, 80),
    SLAM(Kind.SLAM, 16, 13, 1.0, 3.5, -180, 180, 7),
    EQUIP(Kind.EQUIP, 78, 34, 0.0, 0.0, 0, 0);

    public enum Kind {
        ATTACK, BASH, FLURRY, CHARGE, SLAM, EQUIP
    }

    public static final int SHIELD_LOCK = 10;
    public static final int TOSS = 12;
    public static final int CATCH = 30;
    public static final int GLEAM = 41;
    public static final int KNOCK = 57;
    public static final int KNOCK_AGAIN = 61;
    public static final int TOSS_TURNS = 2;
    public static final int STABS = 12;
    public static final int STAB_EVERY = 3;
    public static final int FIRST_STAB = 5;
    public static final int BLOCKING = 32;
    public static final int CHARGING = 64;
    private static final int MOVE_BITS = 31;

    private static final SwordMove[] ATTACKS = { SLASH, BACKHAND, CLEAVE, REVERSE_CLEAVE, RISING, UPPERCUT, OVERHEAD,
            STAB, LUNGE, LOW_SWEEP, SPIN, CROSS };
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

    public int ticks() {
        return this.ticks;
    }

    public int ready() {
        return this.ready;
    }

    public double power() {
        return this.power;
    }

    public double reach() {
        return this.reach;
    }

    public boolean inArc(double degrees) {
        return degrees >= this.arcFrom && degrees <= this.arcTo;
    }

    public int[] hits() {
        return this.hits;
    }

    public boolean swings(double t) {
        if (this.kind == Kind.EQUIP) {
            return t >= KNOCK - 11 && t < KNOCK_AGAIN + 10;
        }
        return (this.kind == Kind.ATTACK || this.kind == Kind.FLURRY) && t >= 0.0 && t < this.ticks;
    }

    public static SwordMove randomAttack(RandomSource random, @Nullable SwordMove last) {
        return pick(ATTACKS, random, last);
    }

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

    public static float whir(int k) {
        return TOSS + k * (CATCH - TOSS) / (2.0F * TOSS_TURNS);
    }

    @Nullable
    public static SwordMove byIndex(int index) {
        SwordMove[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }

    @Nullable
    public static SwordMove sent(int number) {
        return byIndex(number & MOVE_BITS);
    }

    public static double[] stab(int k) {
        double side = (Noise.of(k, 7, 311) - 0.5) * 80.0;
        double up = (Noise.of(k, 7, 312) - 0.45) * 36.0;
        return new double[] { side, up };
    }
}
