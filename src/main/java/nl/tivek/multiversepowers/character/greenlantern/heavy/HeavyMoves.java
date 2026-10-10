package nl.tivek.multiversepowers.character.greenlantern.heavy;

// The two heavy two-handed construct weapons' moves, the same on both sides: each weapon's moves by number, how long
// each lasts in ticks, the tick it lands, and for a held move the stretch it repeats while the button stays down.
public final class HeavyMoves {
    public static final int AXE = 0;
    public static final int SAW = 1;
    public static final int WEAPONS = 2;

    // Between moves, both hands on the weapon at rest.
    public static final int IDLE = -1;
    public static final int FORM = 0;
    // The battleaxe: three chops in turn, the leaping earthbreaker, the hook, the whirlwind and its last wide swing.
    public static final int CHOP = 1;
    public static final int CHOP_BACK = 2;
    public static final int CLEAVE = 3;
    public static final int LEAP = 4;
    public static final int HOOK = 5;
    public static final int WHIRL = 6;
    public static final int WHIRL_OUT = 7;
    // The chainsaw: two slashes in turn, the rend and its pull out, the impale, the guard and its lowering.
    public static final int REV = 1;
    public static final int REV_BACK = 2;
    public static final int REND = 3;
    public static final int REND_OUT = 4;
    public static final int IMPALE = 5;
    public static final int GUARD = 6;
    public static final int GUARD_DOWN = 7;
    public static final int MOVES = 8;
    // Sent while the weapon breaks up.
    public static final int BREAK = 8;
    public static final int BREAK_TICKS = 8;

    // The earthbreaker's leap off the ground, the hook's yank and the impale's eject.
    public static final int LEAP_OFF = 6;
    public static final int YANK = 10;
    public static final int EJECT = 12;
    // A held move repeats from LOOP_FROM until LENGTH while the button stays down.
    public static final int LOOP_FROM = 4;

    private static final int[][] LENGTH = { { 12, 14, 14, 18, 30, 16, 14, 14 }, { 12, 12, 12, 10, 8, 20, 8, 7 } };
    private static final int[][] HIT = { { -1, 6, 6, 9, 18, 6, 4, 4 }, { -1, 5, 5, 4, -1, 5, 3, -1 } };

    private HeavyMoves() {
    }

    public static int length(int weapon, int move) {
        return move < 0 || move >= MOVES ? 0 : LENGTH[weapon][move];
    }

    public static int hit(int weapon, int move) {
        return move < 0 || move >= MOVES ? -1 : HIT[weapon][move];
    }

    // A move that lasts while its button is held: the whirlwind, the rend and the guard.
    public static boolean held(int weapon, int move) {
        return move == (weapon == AXE ? WHIRL : REND) || weapon == SAW && move == GUARD;
    }

    // Where a held move is `age` ticks in: past its lead-in it goes round its loop.
    public static double looped(int weapon, int move, double age) {
        int length = length(weapon, move);
        if (!held(weapon, move) || age < length) {
            return age;
        }
        double loop = length - LOOP_FROM;
        return LOOP_FROM + (age - LOOP_FROM) % loop;
    }

    // The next click may start the next move this far into this one.
    public static int open(int weapon, int move) {
        return (int) Math.ceil(length(weapon, move) * 0.7);
    }
}
