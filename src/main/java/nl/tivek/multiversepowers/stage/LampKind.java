package nl.tivek.multiversepowers.stage;

import java.util.Locale;

// The kinds of stage light, each a block of its own, and how each comes when placed.
public enum LampKind {
    // A small head on a yoke throwing one sharp beam that pans, tilts and flashes fast.
    MOVING_HEAD(Look.BEAM, 48.0, 0.09, 1.5, 1, 0.0, 12, Pattern.FIGURE_EIGHT, 0.6, 70.0, 45.0, 0.0,
            new int[] { 0x3C6BFF, 0xFF2BD6, 0x2BFFF1 }, ColorMode.JUMP, 1.0),
    // A searchlight throwing a wide, very bright beam far up into the sky, sweeping slowly.
    SEARCHLIGHT(Look.BEAM, 320.0, 0.45, 0.25, 1, 0.0, 15, Pattern.SWEEP, 0.08, 35.0, 20.0, 0.0,
            new int[] { 0xCFE4FF }, ColorMode.SINGLE, 0.0),
    // A laser: thin hard lines in a fan, scanning.
    LASER(Look.LASER, 40.0, 0.018, 0.0, 6, 50.0, 6, Pattern.WAVE, 0.5, 40.0, 25.0, 0.0,
            new int[] { 0xFF1F3A, 0x2EFF5A, 0x2E6BFF }, ColorMode.JUMP, 0.5),
    // A wash light: a wide soft cone of colour over a stage.
    PAR(Look.CONE, 18.0, 0.35, 18.0, 1, 0.0, 13, Pattern.STILL, 0.2, 20.0, 15.0, 0.0,
            new int[] { 0xFF3B9A, 0x8A3BFF }, ColorMode.FADE, 0.3),
    // A strobe: a bright panel that flashes, with a short burst of light ahead of it.
    STROBE(Look.FLASH, 6.0, 0.6, 30.0, 1, 0.0, 15, Pattern.STILL, 0.0, 0.0, 0.0, 8.0,
            new int[] { 0xFFFFFF }, ColorMode.SINGLE, 0.0),
    // A mirror ball: dozens of small beams turning round the room.
    DISCO_BALL(Look.DOTS, 24.0, 0.025, 0.0, 36, 0.0, 10, Pattern.SPIN, 0.15, 0.0, 0.0, 0.0,
            new int[] { 0xFFFFFF, 0x9FD8FF }, ColorMode.FADE, 0.2);

    // How its light is drawn.
    public enum Look {
        BEAM, LASER, CONE, FLASH, DOTS
    }

    public enum Pattern {
        STILL, SWEEP, CIRCLE, FIGURE_EIGHT, WAVE, RANDOM, BOUNCE, SPIN
    }

    public enum ColorMode {
        SINGLE, FADE, JUMP, RAINBOW, RANDOM
    }

    public final Look look;
    public final double length;
    public final double width;
    public final double spread;
    public final int beams;
    public final double fan;
    public final int light;
    public final Pattern pattern;
    public final double speed;
    public final double panRange;
    public final double tiltRange;
    public final double strobe;
    public final int[] colors;
    public final ColorMode colorMode;
    public final double colorSpeed;

    LampKind(Look look, double length, double width, double spread, int beams, double fan, int light, Pattern pattern,
            double speed, double panRange, double tiltRange, double strobe, int[] colors, ColorMode colorMode,
            double colorSpeed) {
        this.look = look;
        this.length = length;
        this.width = width;
        this.spread = spread;
        this.beams = beams;
        this.fan = fan;
        this.light = light;
        this.pattern = pattern;
        this.speed = speed;
        this.panRange = panRange;
        this.tiltRange = tiltRange;
        this.strobe = strobe;
        this.colors = colors;
        this.colorMode = colorMode;
        this.colorSpeed = colorSpeed;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static LampKind of(int ordinal) {
        LampKind[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }
}
