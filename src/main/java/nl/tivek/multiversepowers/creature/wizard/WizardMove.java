package nl.tivek.multiversepowers.creature.wizard;

// The Wizard's moves on one timeline for the server and every game that draws him: how long each lasts, the tick its
// magic is let go, how long before he may cast it again, from which phase on he knows it and how hard it strikes (times
// his attack). A big spell is followed by a breath in which he only strikes or flicks sparks; an ultimate by a longer
// one before the next ultimate. GLIDE and DASH are what he and his clones do in the circle of mirrors (WizardMirrors);
// ASCEND, UNBIND, AWAKEN and APOTHEOSIS carry him into his second to fifth phase. SPARK and BOLT are his light spells,
// cast between the others; SKIP a quick glide to a better place, AIRSTEP a blink to another spot in the air; ARRIVE
// how he comes down out of his rift when called, DEATH how he dies.
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
    public static final int SKIP = 22;
    public static final int ARRIVE = 23;
    public static final int DEATH = 24;
    public static final int ORBIT = 25;
    public static final int LANCES = 26;
    public static final int NOVA = 27;
    public static final int TORNADO = 28;
    public static final int COMET = 29;
    public static final int INFERNO = 30;
    public static final int PHANTOM = 31;
    public static final int TENDRILS = 32;
    public static final int VOID_ORB = 33;
    public static final int DRAIN = 34;
    public static final int SPIRAL = 35;
    public static final int SUPERNOVA = 36;
    public static final int AWAKEN = 37;
    public static final int APOTHEOSIS = 38;
    public static final int AIRSTEP = 39;
    public static final int KINDS = 39;
    // The last phase.
    public static final int PHASES = 5;

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
    // He lands from his rift, and his staff strikes the ground.
    public static final int LAND = 62;
    // Light breaks out of the dying Wizard, and he is gone.
    public static final int BREAK = 52;
    public static final int GONE = 104;
    // Ticks his last glow lingers once he is gone.
    public static final int AFTERGLOW = 8;
    // The comet: he is up and hangs aiming until COMET_DIVE, then dives.
    public static final int COMET_UP = 14;
    public static final int COMET_DIVE = 26;
    // The phantom strike: gone at its release, over them until PHANTOM_SLAM, then down onto them.
    public static final int PHANTOM_SLAM = 18;
    // The soul drain and the star spiral run from their release until here.
    public static final int DRAIN_STOP = 64;
    public static final int SPIRAL_STOP = 56;
    // The supernova: he rises until NOVA_RISE, the star grows until its release and bursts.
    public static final int NOVA_RISE = 24;

    private static final int[] LENGTH = new int[KINDS + 1];
    private static final int[] RELEASE = new int[KINDS + 1];
    private static final int[] AGAIN = new int[KINDS + 1];
    private static final int[] PHASE = new int[KINDS + 1];
    private static final double[] HARM = new double[KINDS + 1];
    private static final boolean[] BIG = new boolean[KINDS + 1];
    private static final boolean[] ULTIMATE = new boolean[KINDS + 1];
    // The sweep after the thrust strikes harder.
    public static final double SWEEP_HARM = 1.25;
    // How much harder his magic strikes in each phase.
    private static final double[] POWER = { 1.0, 1.0, 1.15, 1.32, 1.5, 1.7 };
    // Never his to choose: they come by themselves (a phase, his mirrors, his arrival, his death).
    private static final int NEVER = 9;

    static {
        //   kind        length release again phase harm
        move(STRIKE, 26, JAB, 30, 1, 1.0);
        move(SPARK, 12, 6, 12, 1, 0.55);
        move(BOLT, 22, 11, 36, 1, 0.9);
        big(FIREBALL, 28, 17, 130, 1, 1.5);
        big(BLIZZARD, 34, 22, 380, 1, 0.25);
        move(BLINK, 16, 8, 200, 1, 0.0);
        big(CHAINS, 30, 14, 280, 1, 0.5);
        big(LIGHTNING, 26, 14, 200, 1, 1.2);
        big(ORBIT, 40, 18, 240, 1, 0.6);
        big(STARFALL, 42, 20, 440, 2, 1.0);
        big(RUNES, 32, 18, 320, 2, 1.5);
        big(LIFT, 44, 14, 360, 2, 1.2);
        big(FAMILIARS, 36, 22, 640, 2, 0.0);
        big(LANCES, 34, 16, 260, 2, 1.1);
        big(NOVA, 30, 16, 300, 2, 1.0);
        big(TORNADO, 40, 24, 700, 2, 1.0);
        move(AIRSTEP, 10, 3, 70, 2, 0.0);
        ultimate(BEAM, 90, 26, 520, 3, 0.4);
        ultimate(METEOR, 50, 26, 760, 3, 2.0);
        ultimate(WARP, 34, 20, 820, 3, 0.0);
        ultimate(MIRRORS, 30, 22, 1000, 3, 0.6);
        big(COMET, 56, COMET_DIVE, 520, 3, 1.8);
        big(INFERNO, 36, 20, 480, 3, 1.2);
        big(PHANTOM, 34, 6, 360, 3, 1.5);
        big(TENDRILS, 36, 20, 520, 4, 0.35);
        ultimate(VOID_ORB, 44, 26, 820, 4, 2.2);
        big(DRAIN, 70, 18, 600, 4, 0.3);
        big(SPIRAL, 64, 14, 420, 5, 0.45);
        ultimate(SUPERNOVA, 132, 104, 1600, 5, 4.0);
        move(ASCEND, 70, BURST, 0, NEVER, 0.0);
        move(UNBIND, 70, BURST, 0, NEVER, 0.0);
        move(AWAKEN, 70, BURST, 0, NEVER, 0.0);
        move(APOTHEOSIS, 84, BURST, 0, NEVER, 0.0);
        move(STUNNED, 50, 0, 0, NEVER, 0.0);
        move(GLIDE, 400, 0, 0, NEVER, 0.0);
        move(DASH, 12, DASH_HIT, 0, NEVER, 0.6);
        move(SKIP, 10, 1, 44, 1, 0.0);
        move(ARRIVE, 120, LAND, 0, NEVER, 1.0);
        move(DEATH, 112, BREAK, 0, NEVER, 0.0);
    }

    private WizardMove() {
    }

    private static void move(int kind, int length, int release, int again, int phase, double harm) {
        LENGTH[kind] = length;
        RELEASE[kind] = release;
        AGAIN[kind] = again;
        PHASE[kind] = phase;
        HARM[kind] = harm;
    }

    private static void big(int kind, int length, int release, int again, int phase, double harm) {
        move(kind, length, release, again, phase, harm);
        BIG[kind] = true;
    }

    private static void ultimate(int kind, int length, int release, int again, int phase, double harm) {
        big(kind, length, release, again, phase, harm);
        ULTIMATE[kind] = true;
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
        return POWER[Math.max(0, Math.min(PHASES, phase))];
    }

    // A spell after which he takes a breath before the next big one.
    public static boolean big(int kind) {
        return kind > 0 && kind <= KINDS && BIG[kind];
    }

    // The move that carries him into `phase` (2 to 5), and the phase a move carries him into (0 for none).
    public static int into(int phase) {
        return switch (phase) {
            case 2 -> ASCEND;
            case 3 -> UNBIND;
            case 4 -> AWAKEN;
            default -> APOTHEOSIS;
        };
    }

    public static int carries(int kind) {
        return switch (kind) {
            case ASCEND -> 2;
            case UNBIND -> 3;
            case AWAKEN -> 4;
            case APOTHEOSIS -> 5;
            default -> 0;
        };
    }

    // A light spell he may cast between the big ones.
    public static boolean light(int kind) {
        return kind == SPARK || kind == BOLT;
    }

    public static boolean ultimate(int kind) {
        return kind > 0 && kind <= KINDS && ULTIMATE[kind];
    }

    // A move he makes with his crystal blazing and a circle of runes turning under his feet as it gathers (in the
    // air, under him where he hangs).
    public static boolean circled(int kind) {
        return big(kind) && kind != FIREBALL && kind != COMET && kind != PHANTOM;
    }

    // The colour of a spell's light and its circle: arcane violet, fire, frost, the stars, gravity, time, lightning,
    // the void's magenta.
    public static int colour(int kind) {
        return switch (kind) {
            case FIREBALL, METEOR, INFERNO, COMET -> Wizard.FIRE;
            case BLIZZARD, NOVA -> Wizard.ICE;
            case STARFALL, MIRRORS, SPIRAL, SUPERNOVA -> Wizard.STARLIGHT;
            case LIFT -> Wizard.GRAVITY;
            case WARP -> Wizard.TIME;
            case LIGHTNING, LANCES -> Wizard.STORM;
            case TENDRILS, VOID_ORB, DRAIN -> Wizard.VOID;
            default -> Wizard.VIOLET;
        };
    }
}
