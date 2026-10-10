package nl.tivek.multiversepowers.character.greenlantern.heavy;

import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// When the revolvers, the arm cannon and the minigun fire, tick by tick through a move, the same on both sides: the
// server lands each shot, every game draws and sounds it. The revolvers fire in turn, right first with a full pair.
public final class HeavyShots {
    // Fanning the hammers: the first shot this far into the hold, then one every FAN_EVERY ticks.
    public static final int FAN_FROM = 4;
    public static final int FAN_EVERY = 3;
    // Dead-eye: as many marks at most, fired one every DEAD_EVERY ticks once the guns come down.
    public static final int MARKS = 6;
    public static final int DEAD_EVERY = 2;
    // The minigun's barrels spin up this long before the first round, unless they were already spinning.
    public static final int SPIN_UP = 12;
    public static final int SPUN = 2;
    // A click fires a burst of BURST rounds from BURST_FROM.
    public static final int BURST_FROM = 6;
    public static final int BURST = 6;
    // The cannon is fully charged after this long.
    public static final int CHARGE = 40;
    // The minigun overheats after this many rounds in a row; it cools by as many a tick when it rests.
    public static final int HEAT_ROUNDS = 90;
    public static final double COOL = 1.5;

    private HeavyShots() {
    }

    // Whether a shot leaves the gun `age` ticks into `move`. `spun`: the minigun's barrels were already spinning as
    // the move began; `marks`: how many creatures dead-eye marked.
    public static boolean fires(int weapon, int move, int age, boolean spun, int marks) {
        return switch (weapon) {
            case REVOLVERS -> move == SHOOT && age == hit(weapon, move)
                    || move == AIM && age >= FAN_FROM && (age - FAN_FROM) % FAN_EVERY == 0
                    || move == UNBRACE && age >= DEAD_EVERY && age % DEAD_EVERY == 0 && age / DEAD_EVERY <= marks;
            case CANNON -> (move == SHOOT || move == LOOSE) && age == hit(weapon, move);
            case MINIGUN -> move == SHOOT && age >= BURST_FROM && age < BURST_FROM + BURST
                    || move == AIM && age >= (spun ? SPUN : SPIN_UP);
            default -> false;
        };
    }

    // How many shots `move` has fired by `age` ticks in (that tick's own counted).
    public static int fired(int weapon, int move, int age, boolean spun, int marks) {
        int count = 0;
        for (int tick = 0; tick <= age; tick++) {
            if (fires(weapon, move, tick, spun, marks)) {
                count++;
            }
        }
        return count;
    }

    // The hand a revolver shot leaves from: 0 right, 1 left, by how many of the pair's rounds were spent before it.
    public static int side(int roundsLeft) {
        return Math.floorMod(ammo(REVOLVERS) - roundsLeft, 2);
    }

    // How far the minigun's barrels spin, 0 at rest to 1 at full speed, `age` ticks into `move`.
    public static double spin(int move, double age, boolean spun) {
        return switch (move) {
            case SHOOT -> rise(age / BURST_FROM) * (1.0 - rise((age - BURST_FROM - BURST) / 6.0));
            case AIM -> spun ? 1.0 : rise(age / SPIN_UP);
            case BRACE -> rise(age / SPIN_UP);
            case LOOSE, UNBRACE -> 1.0 - rise(age / 12.0);
            case FORM -> rise((age - 12.0) / 4.0) * (1.0 - rise((age - 17.0) / 6.0));
            case RELOAD -> 0.4 * (1.0 - rise(age / 20.0));
            default -> 0.0;
        };
    }

    private static double rise(double u) {
        double t = Math.max(0.0, Math.min(1.0, u));
        return t * t * (3.0 - 2.0 * t);
    }
}
