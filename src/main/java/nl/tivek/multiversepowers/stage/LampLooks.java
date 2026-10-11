package nl.tivek.multiversepowers.stage;

import java.util.List;
import java.util.function.Consumer;

// Ready-made looks for each kind of stage light: one click sets its pattern, speed, colours and flashing together.
// The light screen offers them first; the light show sets its lights with them.
public final class LampLooks {
    public static final int[] PARTY = { 0xFF2BD6, 0x3C6BFF, 0x2BFFF1, 0xFFE100 };
    public static final int[] NEON = { 0x39FF14, 0xFF00FF, 0x00FFFF };
    public static final int[] WARM = { 0xFFB347, 0xFF7A00 };
    public static final int[] COLD = { 0xFFFFFF, 0x9FD8FF, 0x2E6BFF };
    public static final int[] RGB = { 0xFF1F3A, 0x2EFF5A, 0x2E6BFF };

    public record Look(String id, Consumer<LampSettings> apply) {
    }

    private LampLooks() {
    }

    public static List<Look> of(LampKind kind) {
        return switch (kind) {
            case MOVING_HEAD -> List.of(
                    new Look("party", s -> move(s, LampKind.Pattern.FIGURE_EIGHT, 0.6, 70.0, 45.0, PARTY,
                            LampKind.ColorMode.JUMP, 1.0, 0.0)),
                    new Look("sweep", s -> move(s, LampKind.Pattern.SWEEP, 0.15, 90.0, 20.0,
                            new int[] { 0x3C6BFF, 0x2BFFF1 }, LampKind.ColorMode.FADE, 0.2, 0.0)),
                    new Look("spotlight", s -> aim(s, LampSettings.Aim.PLAYER, new int[] { 0xFFF4E0 }, 0.25)),
                    new Look("rave", s -> move(s, LampKind.Pattern.RANDOM, 1.6, 120.0, 60.0, NEON,
                            LampKind.ColorMode.JUMP, 3.0, 8.0)));
            case SEARCHLIGHT -> List.of(
                    new Look("premiere", s -> move(s, LampKind.Pattern.SWEEP, 0.08, 35.0, 20.0,
                            new int[] { 0xCFE4FF }, LampKind.ColorMode.SINGLE, 0.0, 0.0)),
                    new Look("circle", s -> move(s, LampKind.Pattern.CIRCLE, 0.12, 30.0, 25.0,
                            new int[] { 0xCFE4FF }, LampKind.ColorMode.SINGLE, 0.0, 0.0)),
                    new Look("hunt", s -> aim(s, LampSettings.Aim.HOSTILE, new int[] { 0xFFFFFF }, 0.45)),
                    new Look("still", s -> move(s, LampKind.Pattern.STILL, 0.0, 0.0, 0.0,
                            new int[] { 0xCFE4FF }, LampKind.ColorMode.SINGLE, 0.0, 0.0)));
            case LASER -> List.of(
                    new Look("fan", s -> laser(s, LampKind.Pattern.WAVE, 0.5, 6, 50.0, RGB, LampKind.ColorMode.JUMP,
                            0.5)),
                    new Look("scanner", s -> laser(s, LampKind.Pattern.SWEEP, 1.2, 1, 0.0, new int[] { 0xFF1F3A },
                            LampKind.ColorMode.SINGLE, 0.0)),
                    new Look("tunnel", s -> laser(s, LampKind.Pattern.CIRCLE, 0.8, 12, 30.0,
                            new int[] { 0x2EFF5A }, LampKind.ColorMode.SINGLE, 0.0)),
                    new Look("rainbow", s -> laser(s, LampKind.Pattern.WAVE, 0.4, 16, 90.0, RGB,
                            LampKind.ColorMode.RAINBOW, 0.5)));
            case PAR -> List.of(
                    new Look("warm", s -> wash(s, WARM, LampKind.ColorMode.FADE, 0.2, 0.0)),
                    new Look("fade", s -> wash(s, PARTY, LampKind.ColorMode.RAINBOW, 0.3, 0.0)),
                    new Look("pulse", s -> wash(s, new int[] { 0xFF3B9A, 0x8A3BFF }, LampKind.ColorMode.JUMP, 0.5,
                            2.0)),
                    new Look("follow", s -> aim(s, LampSettings.Aim.PLAYER, new int[] { 0xFFE2B8 }, s.width)));
            case STROBE -> List.of(
                    new Look("pulse", s -> flash(s, 2.0, 0.5, new int[] { 0xFFFFFF }, LampKind.ColorMode.SINGLE, 0.0)),
                    new Look("fast", s -> flash(s, 12.0, 0.25, new int[] { 0xFFFFFF }, LampKind.ColorMode.SINGLE,
                            0.0)),
                    new Look("burst", s -> flash(s, 25.0, 0.15, new int[] { 0xFFFFFF }, LampKind.ColorMode.SINGLE,
                            0.0)),
                    new Look("color", s -> flash(s, 6.0, 0.3, PARTY, LampKind.ColorMode.JUMP, 6.0)));
            case DISCO_BALL -> List.of(
                    new Look("classic", s -> ball(s, 36, 0.025, 0.15, new int[] { 0xFFFFFF, 0x9FD8FF },
                            LampKind.ColorMode.FADE, 0.2)),
                    new Look("party", s -> ball(s, 48, 0.03, 0.25, PARTY, LampKind.ColorMode.JUMP, 1.0)),
                    new Look("fast", s -> ball(s, 36, 0.025, 0.8, COLD, LampKind.ColorMode.FADE, 0.6)),
                    new Look("stars", s -> ball(s, 64, 0.012, 0.05, new int[] { 0xFFFFFF },
                            LampKind.ColorMode.SINGLE, 0.0)));
        };
    }

    private static void move(LampSettings s, LampKind.Pattern pattern, double speed, double side, double updown,
            int[] colors, LampKind.ColorMode mode, double colorSpeed, double strobe) {
        s.on = true;
        s.aim = LampSettings.Aim.PATTERN;
        s.pattern = pattern;
        s.speed = speed;
        s.panRange = side;
        s.tiltRange = updown;
        s.colors = colors.clone();
        s.colorMode = mode;
        s.colorSpeed = colorSpeed;
        s.strobe = strobe;
        s.duty = 0.3;
    }

    private static void aim(LampSettings s, LampSettings.Aim aim, int[] colors, double width) {
        s.on = true;
        s.aim = aim;
        s.colors = colors.clone();
        s.colorMode = LampKind.ColorMode.SINGLE;
        s.width = width;
        s.strobe = 0.0;
    }

    private static void laser(LampSettings s, LampKind.Pattern pattern, double speed, int beams, double fan,
            int[] colors, LampKind.ColorMode mode, double colorSpeed) {
        move(s, pattern, speed, 40.0, 25.0, colors, mode, colorSpeed, 0.0);
        s.beams = beams;
        s.fan = fan;
    }

    private static void wash(LampSettings s, int[] colors, LampKind.ColorMode mode, double colorSpeed,
            double strobe) {
        move(s, LampKind.Pattern.STILL, 0.0, 0.0, 0.0, colors, mode, colorSpeed, strobe);
        s.duty = 0.5;
    }

    private static void flash(LampSettings s, double rate, double duty, int[] colors, LampKind.ColorMode mode,
            double colorSpeed) {
        s.on = true;
        s.strobe = rate;
        s.duty = duty;
        s.colors = colors.clone();
        s.colorMode = mode;
        s.colorSpeed = colorSpeed;
    }

    private static void ball(LampSettings s, int dots, double size, double spin, int[] colors,
            LampKind.ColorMode mode, double colorSpeed) {
        move(s, LampKind.Pattern.SPIN, spin, 0.0, 0.0, colors, mode, colorSpeed, 0.0);
        s.beams = dots;
        s.width = size;
    }
}
