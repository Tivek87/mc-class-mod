package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;

// The evil eye and the megaphone.
abstract class HandMarvels extends HandFeats {
    private static final double MARVEL_KICK = 7.5;
    private static final double MARVEL_CALM = 0.32;

    // Evil eye, two hands at once: one up with an eye shut in its palm, one beside it as the puppeteer. The eye opens,
    // strings of light shoot from the puppeteer's fingertips to three creatures, it works them up into the air like
    // puppets, the eye goes wild and turns them to statues of hard light, and both hands clench: the statues shatter.
    static final double EYE_OPEN = 9.0;
    private static final double STRINGS_AT = 11.0;
    private static final double CRAZY_AT = 23.0;
    private static final double STONE_AT = 27.0;
    private static final double SHATTER_AT = 37.0;
    public static final int EYE_OPENS = ticks(EYE_OPEN);
    public static final int EYE_STRINGS = ticks(STRINGS_AT);
    public static final int EYE_CRAZY = ticks(CRAZY_AT);
    public static final int EYE_STONE = ticks(STONE_AT);
    public static final int EYE_SHATTERS = ticks(SHATTER_AT);
    public static final int EYE_VICTIMS = 3;
    // How high the puppets are lifted, in blocks, and how long that takes.
    public static final double PUPPET_LIFT = 3.0;
    public static final int PUPPET_RISES = ticks(5.0);
    // The eye in the palm, in the hand's own frame, and its size.
    public static final Vec3 EYE_POINT = new Vec3(0.05, 1.7, 0.56);
    public static final double EYE_SIZE = 0.78;
    // Held high, so the eye looks down over the puppets instead of hiding behind them.
    private static final double EYE_HIGH = 4.4;
    private static final double PUPPETEER_HIGH = 5.2;
    // The puppeteer's wrist bent forward, palm down over the creatures like a hand over a marionette.
    private static final double PUPPETEER_BEND = 1.25;

    // Megaphone: up out of the ground, a megaphone grows out of the ring's light into its hand; it grips it, aims it
    // and blares three shockwaves; whatever they catch clasps its ears and bursts. The megaphone then breaks apart.
    private static final double HORN_FORMS = 5.0;
    private static final double HORN_GRIPS = 9.0;
    static final double HORN_AIM = 11.0;
    private static final double[] BLARE_BEATS = { 15.0, 19.0, 23.0 };
    private static final double HORN_BREAK = 27.0;
    private static final double POP_AT = 30.0;
    public static final int HORN_FORMED = ticks(HORN_GRIPS);
    public static final int[] BLARES = { ticks(BLARE_BEATS[0]), ticks(BLARE_BEATS[1]), ticks(BLARE_BEATS[2]) };
    public static final int HORN_BREAKS = ticks(HORN_BREAK);
    public static final int POPS = ticks(POP_AT);
    private static final double[] BLARE_AT = { BLARES[0] / SLOW, BLARES[1] / SLOW, BLARES[2] / SLOW };
    // The handle runs across the fist; the horn sits on its index-finger end and points where the fingers do.
    public static final Vec3 HORN_HANDLE = new Vec3(-0.1, 3.55, 0.95);
    public static final Vec3 HORN_AXIS = new Vec3(-1.85, 0.0, 0.95);
    public static final double HORN_BACK = 2.2;
    public static final double HORN_BELL = 6.2;
    public static final double HORN_MOUTH = 1.3;
    // Raised like an arm holding a megaphone to its mouth: the forearm up, the wrist bent to point it.
    private static final double HORN_LENGTH = 3.0;
    private static final double HORN_LIFT = 0.5;
    private static final double HORN_LEAN = 0.15;
    private static final double HORN_TARGET = 1.0;

    public static int puppetStart(int k) {
        return EYE_STRINGS + 2 * k;
    }

    // How far the eye is open at tick t, 0 to 1.
    public static double eyeOpen(double t) {
        double beat = t / SLOW;
        double blink = 1.0 - Ease.bump((beat - 17.5) / 0.8);
        return Ease.smoother((beat - EYE_OPEN) / 1.5) * (1.0 - Ease.smoother((beat - SHATTER_AT - 2.0) / 2.5))
                * Math.max(0.15, blink);
    }

    // How taut the puppeteer holds its strings at tick t, 0 slack to 1 working them.
    public static double puppetPull(double t) {
        return window(t / SLOW, STRINGS_AT, STRINGS_AT + 2.0, SHATTER_AT - 1.0, SHATTER_AT + 0.5);
    }

    // How wild the eye is at tick t: its pupil darting, the eye trembling and glaring.
    public static double eyeWild(double t) {
        double beat = t / SLOW;
        return window(beat, CRAZY_AT, CRAZY_AT + 1.0, STONE_AT + 0.5, STONE_AT + 3.0);
    }

    // How grown the megaphone is at tick t, 0 to 1; it breaks at HORN_BREAKS.
    public static double hornGrown(double t) {
        return Ease.smoother((t / SLOW - HORN_FORMS) / (HORN_GRIPS - HORN_FORMS));
    }

    // The eye's hand holds still, palm to the creatures, fingers spread round the eye, quivering once it goes wild.
    void eye(double t) {
        double staring = window(t, STRINGS_AT, STRINGS_AT + 2.0, SHATTER_AT - 1.0, SHATTER_AT + 0.5);
        double wild = window(t, CRAZY_AT, CRAZY_AT + 1.0, STONE_AT + 0.5, STONE_AT + 3.0);
        this.length = rise(t, EYE_HIGH, 1.0, 0.6) + 0.3 * staring;
        this.lean = 0.1 + 0.2 * staring + wild * 0.03 * Math.sin(t * 7.3);
        this.flex = -0.3 * staring + 0.4 * Ease.recoil(t - SHATTER_AT, 1.0, MARVEL_KICK, MARVEL_CALM);
        this.twist = corkscrew(t, 0.5) + 0.05 * staring * Math.sin(t * 0.45) + wild * 0.04 * Math.sin(t * 9.1);
        unfurl(t, 0.08, 0.06, 0.15, 1.0);
        for (int k = 0; k < 5; k++) {
            this.curl[k] += wild * 0.08 * Math.sin(t * 6.1 + k * 1.7);
        }
        this.spread = Mth.lerp(staring, this.spread, 1.0);
        cascade(t, SHATTER_AT - 0.7, 0.7, true, 1.0, 0.1, 0.9);
        this.spread = Mth.lerp(Ease.smoother((t - SHATTER_AT + 0.7) / 0.9), this.spread, 0.0);
        cascade(t, SHATTER_AT + 5.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - SHATTER_AT - 5.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.5 * window(t, 6.0, 8.0, STRINGS_AT, STRINGS_AT + 2.0));
        this.sink(t, SINK[EYE], LIFE[EYE], 1.0, true);
    }

    // The puppeteer rises beside the eye, bends its wrist over the creatures and works the strings: each finger dips
    // in turn, the thumb against them, the whole hand jerking up as it lifts them; it clenches as they shatter.
    void puppeteer(double t) {
        double pulling = window(t, STRINGS_AT, STRINGS_AT + 2.0, SHATTER_AT - 1.0, SHATTER_AT + 0.5);
        double over = window(t, EYE_OPEN - 2.0, STRINGS_AT, SHATTER_AT + 1.0, SHATTER_AT + 4.0);
        double lifting = Ease.smoother((t - STRINGS_AT) / 4.0) * (1.0 - Ease.smoother((t - SHATTER_AT - 2.0) / 5.0));
        this.length = rise(t, PUPPETEER_HIGH, 1.0, 0.6) + 0.8 * lifting + 0.25 * pulling * Math.sin(t * 0.9);
        this.lean = 0.15 + 0.2 * pulling;
        this.flex = PUPPETEER_BEND * Ease.smoother(over) + 0.18 * pulling * Math.sin(t * 0.9)
                + 0.4 * Ease.recoil(t - SHATTER_AT, 1.0, MARVEL_KICK, MARVEL_CALM);
        this.twist = corkscrew(t, 0.5) + 0.1 * pulling * Math.sin(t * 0.55 + 0.4);
        unfurl(t, 0.12, 0.1, 0.2, 1.0);
        for (int k = 0; k < 4; k++) {
            this.curl[k] += pulling * 0.32 * (0.5 + 0.5 * Math.sin(t * 1.3 - k * 0.9));
            this.hook[k] += pulling * 0.24 * (0.5 + 0.5 * Math.sin(t * 1.3 - k * 0.9 + 0.6));
        }
        this.curl[4] += pulling * 0.2 * (0.5 + 0.5 * Math.sin(t * 1.3 + 1.5));
        cascade(t, SHATTER_AT - 0.7, 0.7, true, 1.0, 0.1, 0.9);
        this.spread = Mth.lerp(Ease.smoother((t - SHATTER_AT + 0.7) / 0.9), this.spread, 0.0);
        cascade(t, SHATTER_AT + 5.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - SHATTER_AT - 5.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.5 * window(t, 6.0, 8.0, STRINGS_AT, STRINGS_AT + 2.0));
        this.sink(t, SINK[PUPPETEER], LIFE[PUPPETEER], 1.0, true);
    }

    // The wrist bends so the horn points at the creature's middle, reach away over the ground the hand stands on.
    private static double hornFlex(double reach) {
        double wrist = HORN_LENGTH + HORN_LIFT;
        double ahead = reach - wrist * Math.sin(HORN_LEAN);
        double below = HORN_TARGET - wrist * Math.cos(HORN_LEAN);
        return Mth.clamp(Math.atan2(ahead, below) - HORN_LEAN, 0.5, 2.2);
    }

    void megaphone(double t, double reach) {
        double blare = 0.0;
        for (double at : BLARE_AT) {
            blare += Ease.recoil(t - at, 1.0, MARVEL_KICK, MARVEL_CALM);
        }
        double aim = Ease.smoother((t - HORN_AIM) / 3.0) * (1.0 - Ease.smoother((t - HORN_BREAK - 1.0) / 5.0));
        double shout = window(t, BLARE_AT[0] - 1.0, BLARE_AT[0], BLARE_AT[2] + 1.0, BLARE_AT[2] + 3.0);
        this.length = rise(t, HORN_LENGTH, 1.0, 0.6) + HORN_LIFT * aim - 0.25 * blare;
        this.lean = 0.1 + (HORN_LEAN - 0.1) * aim - 0.12 * blare + 0.02 * shout * Math.sin(t * 8.0);
        this.flex = hornFlex(reach) * aim - 0.25 * blare;
        this.twist = corkscrew(t, 0.5) + 0.02 * shout * Math.sin(t * 6.3);
        unfurl(t, 0.2, 0.15, 0.3, 0.8);
        // Closing round the handle as the megaphone forms, and flying open when it breaks.
        cascade(t, HORN_GRIPS - 1.2, 1.2, true, 0.86, 0.25, 0.75);
        this.spread = Mth.lerp(Ease.smoother((t - HORN_GRIPS + 1.2) / 1.2), this.spread, 0.0);
        cascade(t, HORN_BREAK, 0.8, false, 0.1, 0.1, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - HORN_BREAK) / 1.2), this.spread, 0.9);
        cascade(t, HORN_BREAK + 4.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - HORN_BREAK - 4.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.5 * window(t, 5.0, 7.0, HORN_AIM, HORN_AIM + 2.0));
        this.sink(t, SINK[MEGAPHONE], LIFE[MEGAPHONE], 1.0, true);
    }
}
