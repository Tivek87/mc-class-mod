package nl.tivek.multiversepowers.creature.wizard;

// The Wizard's moves on one timeline for the server and every game that draws him: how long each lasts, the tick its
// magic is let go, how long before he may cast it again, from which phase on he knows it and how hard it strikes (times
// his attack). A big spell is followed by a breath in which he only strikes or flicks sparks; an ultimate by a longer
// one before the next ultimate. GLIDE and DASH are what he and his clones do in the circle of mirrors (WizardMirrors);
// ASCEND, UNBIND, AWAKEN and APOTHEOSIS carry him into his second to fifth phase. SPARK and BOLT are his light spells,
// cast between the others; SKIP a quick glide to a better place, AIRSTEP a blink to another spot in the air; ARRIVE
// how he comes down out of his rift when called, DEATH how he dies. ALIGHT and TAKEOFF take him from the air to his
// feet and back; WATCH a pause in which he only watches; the STAFF_ moves throw his staff as a spear, call it home and
// set it free to fight on its own; FLICKER, RIFT_STEP and VEIL are his other ways of going (WizardSteps); the
// PASTIMES are what he does with no one to fight (WizardPastimes).
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
    public static final int REPULSE = 40;
    public static final int FROST_CONE = 41;
    public static final int FLAME_LASH = 42;
    public static final int COMBO = 43;
    public static final int WHIRL = 44;
    public static final int TETHER = 45;
    public static final int GLACIAL = 46;
    public static final int PILLARS = 47;
    public static final int SENTRY = 48;
    public static final int SIGIL = 49;
    public static final int RAIN = 50;
    public static final int FLARE = 51;
    public static final int ECLIPSE = 52;
    public static final int JUDGMENT = 53;
    public static final int ALIGHT = 54;
    public static final int TAKEOFF = 55;
    public static final int WATCH = 56;
    public static final int STAFF_THROW = 57;
    public static final int STAFF_CALL = 58;
    public static final int STAFF_FREE = 59;
    public static final int FLICKER = 60;
    public static final int RIFT_STEP = 61;
    public static final int VEIL = 62;
    public static final int READ = 63;
    public static final int PERCH = 64;
    public static final int JUGGLE = 65;
    public static final int MEDITATE = 66;
    public static final int PORTALS = 67;
    public static final int BREW = 68;
    public static final int WISP = 69;
    public static final int SCRIBE = 70;
    public static final int PRACTICE = 71;
    public static final int RIFT_VOLLEY = 72;
    public static final int KINDS = 72;
    // What he does at rest with no foe about (WizardPastimes).
    public static final int[] PASTIMES = { READ, PERCH, JUGGLE, MEDITATE, PORTALS, BREW, WISP, SCRIBE, PRACTICE };
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
    // The frost cone breathes from its release every CONE_EVERY ticks until CONE_STOP.
    public static final int CONE_EVERY = 3;
    public static final int CONE_STOP = 23;
    // The staff combo's three blows: an overhead smash, a backhand sweep, a rising thrust.
    public static final int COMBO_SMASH = 9;
    public static final int COMBO_SWEEP = 19;
    public static final int COMBO_THRUST = 31;
    // The whirl strikes all round him every WHIRL_EVERY ticks from its release until WHIRL_STOP.
    public static final int WHIRL_EVERY = 6;
    public static final int WHIRL_STOP = 28;
    // The flicker hops from its release every FLICKER_EVERY ticks, FLICKERS times.
    public static final int FLICKER_EVERY = 6;
    public static final int FLICKERS = 3;
    // The rift step: he steps into his rift at its release and out of the other at RIFT_OUT.
    public static final int RIFT_OUT = 27;
    // The veil: gone at its release, behind his foe at VEIL_BACK, his staff swung at VEIL_STRIKE.
    public static final int VEIL_BACK = 28;
    public static final int VEIL_STRIKE = 33;
    // Called home, his staff is in his hand again by CALL_CATCH.
    public static final int CALL_CATCH = 18;

    private static final int[] LENGTH = new int[KINDS + 1];
    private static final int[] RELEASE = new int[KINDS + 1];
    private static final int[] AGAIN = new int[KINDS + 1];
    private static final int[] PHASE = new int[KINDS + 1];
    private static final double[] HARM = new double[KINDS + 1];
    private static final boolean[] BIG = new boolean[KINDS + 1];
    private static final boolean[] ULTIMATE = new boolean[KINDS + 1];
    private static final boolean[] CLOSE = new boolean[KINDS + 1];
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
        big(FIREBALL, 28, 17, 100, 1, 1.5);
        big(BLIZZARD, 34, 22, 100, 1, 0.25);
        move(BLINK, 16, 8, 100, 1, 0.0);
        big(CHAINS, 30, 14, 100, 1, 0.5);
        big(LIGHTNING, 26, 14, 100, 1, 1.2);
        big(ORBIT, 40, 18, 100, 1, 0.6);
        big(STARFALL, 42, 20, 100, 2, 1.0);
        big(RUNES, 32, 18, 100, 2, 1.5);
        big(LIFT, 44, 14, 100, 2, 1.2);
        big(FAMILIARS, 36, 22, 100, 2, 0.0);
        big(LANCES, 34, 16, 100, 2, 1.1);
        big(NOVA, 30, 16, 100, 2, 1.0);
        big(TORNADO, 40, 24, 100, 2, 1.0);
        move(AIRSTEP, 10, 3, 70, 2, 0.0);
        ultimate(BEAM, 90, 26, 100, 3, 0.4);
        ultimate(METEOR, 50, 26, 100, 3, 2.0);
        ultimate(WARP, 34, 20, 100, 3, 0.0);
        ultimate(MIRRORS, 30, 22, 100, 3, 0.6);
        big(COMET, 56, COMET_DIVE, 100, 3, 1.8);
        big(INFERNO, 36, 20, 100, 3, 1.2);
        big(PHANTOM, 34, 6, 100, 3, 1.5);
        big(TENDRILS, 36, 20, 100, 4, 0.35);
        ultimate(VOID_ORB, 44, 26, 100, 4, 2.2);
        big(DRAIN, 70, 18, 400, 4, 0.3);
        big(SPIRAL, 64, 14, 100, 5, 0.45);
        ultimate(SUPERNOVA, 132, 104, 100, 5, 2.6);
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
        close(REPULSE, 20, 9, 100, 1, 0.9);
        close(FROST_CONE, 30, 10, 100, 1, 0.3);
        close(FLAME_LASH, 24, 11, 100, 2, 1.1);
        close(COMBO, 42, COMBO_SMASH, 100, 1, 0.75);
        close(WHIRL, 36, 10, 100, 2, 0.55);
        move(TETHER, 30, 12, 100, 1, 0.5);
        big(GLACIAL, 30, 15, 100, 2, 1.2);
        big(PILLARS, 32, 18, 100, 3, 1.4);
        big(SENTRY, 30, 20, 100, 2, 0.45);
        move(SIGIL, 56, 22, 0, NEVER, 0.9);
        move(RAIN, 64, 18, 0, NEVER, 0.85);
        move(FLARE, 60, 22, 0, NEVER, 1.0);
        move(ECLIPSE, 64, 24, 0, NEVER, 1.2);
        move(JUDGMENT, 84, 18, 0, NEVER, 1.5);
        move(ALIGHT, 24, 20, 0, NEVER, 0.0);
        move(TAKEOFF, 18, 8, 0, NEVER, 0.0);
        move(WATCH, 40, 0, 100, 1, 0.0);
        big(STAFF_THROW, 26, 14, 160, 1, 1.4);
        move(STAFF_CALL, 24, 6, 0, NEVER, 0.9);
        big(STAFF_FREE, 30, 18, 400, 2, 0.5);
        move(FLICKER, 24, 6, 100, 2, 0.0);
        move(RIFT_STEP, 40, 20, 100, 1, 0.0);
        move(VEIL, 40, 8, 100, 3, 1.3);
        move(READ, 360, 20, 0, NEVER, 0.0);
        move(PERCH, 400, 24, 0, NEVER, 0.0);
        move(JUGGLE, 320, 30, 0, NEVER, 0.0);
        move(MEDITATE, 360, 30, 0, NEVER, 0.0);
        move(PORTALS, 300, 16, 0, NEVER, 0.0);
        move(BREW, 340, 24, 0, NEVER, 0.0);
        move(WISP, 300, 16, 0, NEVER, 0.0);
        move(SCRIBE, 320, 20, 0, NEVER, 0.0);
        move(PRACTICE, 280, 16, 0, NEVER, 0.0);
        big(RIFT_VOLLEY, 40, 16, 100, 2, 0.55);
    }

    // Something he does at rest, with no foe about.
    public static boolean pastime(int kind) {
        return kind >= READ && kind <= PRACTICE;
    }

    // A way of going from one place to another in a moment.
    public static boolean teleport(int kind) {
        return kind == BLINK || kind == FLICKER || kind == RIFT_STEP || kind == VEIL || kind == AIRSTEP;
    }

    private static void close(int kind, int length, int release, int again, int phase, double harm) {
        move(kind, length, release, again, phase, harm);
        CLOSE[kind] = true;
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

    // A move for a foe up close: a short spell or a blow of his staff.
    public static boolean close(int kind) {
        return kind > 0 && kind <= KINDS && CLOSE[kind];
    }

    // The great spell each phase opens with as it takes him, his first phase's as he lands (SIGIL), each fitting the
    // phase: the Archmage's circle of runes, the Ascended's rain of light, the Unbound's solar flare, the Voidborn's
    // eclipse, the Eternal's judgment as time stands still.
    public static int special(int phase) {
        return switch (phase) {
            case 1 -> SIGIL;
            case 2 -> RAIN;
            case 3 -> FLARE;
            case 4 -> ECLIPSE;
            default -> JUDGMENT;
        };
    }

    public static boolean isSpecial(int kind) {
        return kind >= SIGIL && kind <= JUDGMENT;
    }

    // A move he makes with his crystal blazing and a circle of runes turning under his feet as it gathers (in the
    // air, under him where he hangs).
    public static boolean circled(int kind) {
        return big(kind) && kind != FIREBALL && kind != COMET && kind != PHANTOM && kind != STAFF_THROW
                || isSpecial(kind);
    }

    // The colour of a spell's light and its circle: arcane violet, fire, frost, the stars, gravity, time, lightning,
    // the void's magenta.
    public static int colour(int kind) {
        return switch (kind) {
            case FIREBALL, METEOR, INFERNO, COMET, FLAME_LASH, PILLARS, FLARE -> Wizard.FIRE;
            case BLIZZARD, NOVA, FROST_CONE, GLACIAL -> Wizard.ICE;
            case STARFALL, MIRRORS, SPIRAL, SUPERNOVA, RAIN -> Wizard.STARLIGHT;
            case LIFT, REPULSE -> Wizard.GRAVITY;
            case WARP, JUDGMENT -> Wizard.TIME;
            case LIGHTNING, LANCES -> Wizard.STORM;
            case TENDRILS, VOID_ORB, DRAIN, TETHER, ECLIPSE -> Wizard.VOID;
            default -> Wizard.VIOLET;
        };
    }
}
