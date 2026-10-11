package nl.tivek.multiversepowers.creature.wizard;

// The Wizard's moves on one timeline for the server and every game that draws him: how long each lasts, the tick its
// magic is let go, how long before he may cast it again, from which phase on he knows it and how hard it strikes (times
// his attack). A big spell is followed by a breath in which he only strikes or flicks sparks; an ultimate by a longer
// one before the next ultimate. GLIDE and DASH are what he and his clones do in the circle of mirrors (WizardMirrors);
// ASCEND and UNBIND carry him into his second and third phase.
public final class WizardMove {
    public static final int STRIKE = 1;
    public static final int SPARK = 2;
    public static final int BOLT = 3;
    public static final int FIREBALL = 4;
    public static final int BLIZZARD = 5;
    public static final int BLINK = 6;
    public static final int CHAINS = 7;
    public static final int LIGHTNING = 8;
    public static final int STARFALL = 9;
    public static final int RUNES = 10;
    public static final int LIFT = 11;
    public static final int FAMILIARS = 12;
    public static final int BEAM = 13;
    public static final int METEOR = 14;
    public static final int WARP = 15;
    public static final int MIRRORS = 16;
    public static final int ASCEND = 17;
    public static final int UNBIND = 18;
    public static final int STUNNED = 19;
    public static final int GLIDE = 20;
    public static final int DASH = 21;
    public static final int KINDS = 21;

    // The staff strike's two blows: a thrust, then a sweep.
    public static final int JAB = 8;
    public static final int SWEEP = 17;
    // The three bolts of his third phase, after the first.
    public static final int BOLT_AGAIN = 5;
    // The gravity well lets go and he drives them down.
    public static final int SLAM = 34;
    // The beam burns from its release until here.
    public static final int BEAM_STOP = 80;
    // The phase bursts out of him.
    public static final int BURST = 46;
    // A dash strikes halfway.
    public static final int DASH_HIT = 6;

    private static final int[] LENGTH = { 0, 26, 12, 22, 28, 34, 16, 30, 26, 42, 32, 44, 36, 90, 50, 34, 30, 70, 70,
            50, 400, 12 };
    private static final int[] RELEASE = { 0, JAB, 6, 11, 17, 22, 8, 14, 14, 20, 18, 14, 22, 26, 26, 20, 22, BURST,
            BURST, 0, 0, DASH_HIT };
    private static final int[] AGAIN = { 0, 34, 26, 90, 130, 380, 220, 280, 200, 440, 320, 360, 640, 520, 760, 820,
            1000, 0, 0, 0, 0, 0 };
    private static final int[] PHASE = { 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 9, 9, 9, 9, 9 };
    private static final double[] HARM = { 0.0, 1.0, 0.7, 1.2, 1.5, 0.25, 0.0, 0.5, 1.2, 1.0, 1.5, 1.2, 0.0, 0.4, 2.0,
            0.0, 0.6, 0.0, 0.0, 0.0, 0.0, 0.6 };
    // The sweep after the thrust strikes harder.
    public static final double SWEEP_HARM = 1.25;
    // How much harder his magic strikes in each phase.
    private static final double[] POWER = { 1.0, 1.0, 1.15, 1.3 };

    private WizardMove() {
    }

    public static int length(int kind) {
        return LENGTH[kind];
    }

    public static int release(int kind) {
        return RELEASE[kind];
    }

    public static int again(int kind) {
        return AGAIN[kind];
    }

    public static int phase(int kind) {
        return PHASE[kind];
    }

    public static double harm(int kind) {
        return HARM[kind];
    }

    public static double power(int phase) {
        return POWER[Math.max(0, Math.min(3, phase))];
    }

    // A spell after which he takes a breath before the next.
    public static boolean big(int kind) {
        return kind >= BOLT && kind <= MIRRORS && kind != BLINK;
    }

    public static boolean ultimate(int kind) {
        return kind >= BEAM && kind <= MIRRORS;
    }

    // A move he makes with his crystal blazing and a circle of runes turning under his feet as it gathers.
    public static boolean circled(int kind) {
        return big(kind) && kind != BOLT && kind != FIREBALL;
    }

    // The colour of a spell's light and its circle: arcane violet, fire, frost, the stars, gravity, time, lightning.
    public static int colour(int kind) {
        return switch (kind) {
            case FIREBALL, METEOR -> Wizard.FIRE;
            case BLIZZARD -> Wizard.ICE;
            case STARFALL, MIRRORS -> Wizard.STARLIGHT;
            case LIFT -> Wizard.GRAVITY;
            case WARP -> Wizard.TIME;
            case LIGHTNING -> Wizard.STORM;
            default -> Wizard.VIOLET;
        };
    }
}
