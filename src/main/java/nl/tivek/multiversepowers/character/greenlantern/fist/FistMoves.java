package nl.tivek.multiversepowers.character.greenlantern.fist;

// Green Lantern's construct fists, as server and clients both count them: ten blows in a combo, one after another
// with each click, and four heavy blows, one at random each time the button is held. Ticks from a blow's start.
public final class FistMoves {
    public static final int JAB = 0;
    public static final int CROSS = 1;
    public static final int HOOK = 2;
    public static final int UPPERCUT = 3;
    public static final int BODY = 4;
    public static final int OVERHAND = 5;
    public static final int BACKFIST = 6;
    public static final int ELBOW = 7;
    public static final int DOUBLE = 8;
    public static final int FINISHER = 9;
    public static final int COMBO = 10;
    // A giant fist slammed down from above, one bursting up out of the ground, one rammed out ahead on a piston, and
    // both fists swollen huge in a full turn.
    public static final int HAMMER = 10;
    public static final int RISING = 11;
    public static final int PISTON = 12;
    public static final int SPIN = 13;
    public static final int KINDS = 14;
    // After its last blow the guard (and the gloves) stay up this long, then go.
    public static final int GUARD = 40;
    // How far ahead a heavy's giant fist comes down or up, and how far the piston reaches, in blocks.
    public static final double AHEAD = 2.6;
    public static final double PISTON_REACH = 5.5;

    private static final int[] LENGTH = { 7, 8, 9, 10, 9, 11, 8, 8, 12, 16, 24, 24, 22, 20 };
    private static final int[] HIT = { 2, 3, 4, 5, 4, 5, 3, 3, 6, 9, 14, 12, 11, 10 };
    // How hard each blow hits, as a part of a punch, and how far it throws what it hits.
    private static final double[] STRENGTH = { 0.8, 1.0, 1.1, 1.2, 1.0, 1.3, 0.9, 1.1, 1.3, 1.0, 1.0, 1.0, 1.0, 1.0 };
    private static final double[] PUSH = { 0.25, 0.35, 0.4, 0.3, 0.35, 0.45, 0.35, 0.4, 0.7, 1.4, 0.9, 0.5, 2.0, 1.2 };
    // Blows that sweep round and catch more than the one creature in front.
    private static final boolean[] WIDE = { false, false, true, false, true, false, true, true, false, false, true,
            true, false, true };

    private FistMoves() {
    }

    public static boolean heavy(int move) {
        return move >= COMBO;
    }

    public static int length(int move) {
        return LENGTH[Math.floorMod(move, KINDS)];
    }

    public static int hit(int move) {
        return HIT[Math.floorMod(move, KINDS)];
    }

    public static double strength(int move) {
        return STRENGTH[Math.floorMod(move, KINDS)];
    }

    public static double push(int move) {
        return PUSH[Math.floorMod(move, KINDS)];
    }

    public static boolean wide(int move) {
        return WIDE[Math.floorMod(move, KINDS)];
    }

    // From when a combo blow may give way to the next click: its blow landed and it is pulling back.
    public static int open(int move) {
        return heavy(move) ? length(move) : hit(move) + 2;
    }
}
