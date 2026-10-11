package nl.tivek.multiversepowers.character.greenlantern.heavy;

// The heavy two-handed construct weapons' moves, the same on both sides: each weapon's moves by number, how long each
// lasts in ticks, the tick it lands, and for a held move the stretch it repeats while the button stays down.
public final class HeavyMoves {
    public static final int AXE = 0;
    public static final int SAW = 1;
    public static final int RPG = 2;
    public static final int SHOTGUN = 3;
    // Two revolvers, one in each hand; the arm cannon over his right forearm; the minigun at his hip.
    public static final int REVOLVERS = 4;
    public static final int CANNON = 5;
    public static final int MINIGUN = 6;
    public static final int WEAPONS = 7;

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
    // The guns: a shot, the aim held and let fly (cluster rocket, both barrels, the revolvers' fanned hammers, the
    // cannon's charge, the minigun's stream), the right click's move (blast jump, stock strike, pistol whip, cannon
    // bash, vent), the right button held (guided rocket, deflection, dead-eye, rapid fire, spun barrels) and let go.
    public static final int SHOOT = 1;
    public static final int AIM = 2;
    public static final int LOOSE = 3;
    public static final int KICK = 4;
    public static final int BRACE = 5;
    public static final int UNBRACE = 6;
    // An empty gun reloads by itself: the shotgun breaks open for two shells, the launcher takes a new rocket, the
    // revolvers swing out their cylinders; an overheated minigun cools down.
    public static final int RELOAD = 7;
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
    // The whirlwind spins 5 seconds at most, then ends in its wide swing as if let go.
    public static final int WHIRL_MOST = 100;
    // A gun's aim lets fly by itself after 3 seconds.
    public static final int AIM_MOST = 60;
    // A guided rocket is steered 10 seconds at most, then bursts as if let go.
    public static final int GUIDE_MOST = 200;
    // A reload fills the gun this many ticks before it ends.
    public static final int RELOADED = 4;
    private static final int[] AMMO = { 0, 0, 1, 2, 12, 0, 0 };

    private static final int[][] LENGTH = { { 20, 14, 14, 18, 30, 20, 14, 14 }, { 20, 12, 12, 10, 8, 20, 8, 7 },
            { 20, 14, 24, 14, 18, 12, 8, 26 }, { 20, 16, 16, 14, 12, 10, 12, 32 }, { 22, 8, 10, 8, 12, 10, 16, 38 },
            { 20, 10, 16, 18, 12, 10, 10, 1 }, { 24, 20, 16, 14, 16, 10, 14, 40 } };
    private static final int[][] HIT = { { -1, 6, 6, 9, 18, 6, 4, 4 }, { -1, 5, 5, 4, -1, 5, 3, -1 },
            { -1, 3, -1, 2, 4, 3, 1, -1 }, { -1, 2, -1, 2, 5, -1, 2, -1 }, { -1, 1, -1, -1, 4, -1, -1, -1 },
            { -1, 2, -1, 2, 4, -1, -1, -1 }, { -1, 6, -1, -1, 4, -1, -1, -1 } };

    private HeavyMoves() {
    }

    public static int length(int weapon, int move) {
        return move < 0 || move >= MOVES ? 0 : LENGTH[weapon][move];
    }

    public static int hit(int weapon, int move) {
        return move < 0 || move >= MOVES ? -1 : HIT[weapon][move];
    }

    public static boolean gun(int weapon) {
        return weapon >= RPG;
    }

    // One gun in each hand, each held and drawn on its own.
    public static boolean dual(int weapon) {
        return weapon == REVOLVERS;
    }

    // How many shots a full gun holds; 0 for a weapon with none.
    public static int ammo(int weapon) {
        return weapon < 0 || weapon >= WEAPONS ? 0 : AMMO[weapon];
    }

    // A move that lasts while its button is held: the whirlwind, the rend, the guard, a gun's aim and brace.
    public static boolean held(int weapon, int move) {
        return switch (weapon) {
            case AXE -> move == WHIRL;
            case SAW -> move == REND || move == GUARD;
            default -> move == AIM || move == BRACE;
        };
    }

    // What follows a held move when its button is let go.
    public static int ending(int weapon, int move) {
        return switch (weapon) {
            case AXE -> WHIRL_OUT;
            case SAW -> move == GUARD ? GUARD_DOWN : REND_OUT;
            default -> move == AIM ? LOOSE : UNBRACE;
        };
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
        if (gun(weapon) && move == RELOAD) {
            return length(weapon, move);
        }
        return (int) Math.ceil(length(weapon, move) * 0.7);
    }
}
