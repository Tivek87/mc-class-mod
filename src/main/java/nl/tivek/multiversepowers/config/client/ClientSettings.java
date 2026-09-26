package nl.tivek.multiversepowers.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientSettings {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue CAMERA_SHAKE;
    public static final ModConfigSpec.DoubleValue RAM_GROUND_SHAKE;
    public static final ModConfigSpec.IntValue THEME_MUSIC;
    public static final ModConfigSpec.IntValue UPDATE_CHECK;
    public static final ModConfigSpec.DoubleValue UPDATE_POPUP;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Your own settings: only what you see and feel, in your own game, whatever world or server you"
                + " play on.", "The same numbers can be changed in the game: Mods > this mod > Config.").push("view");
        CAMERA_SHAKE = builder.comment("How hard your view shakes and jolts from the powers: a landing slam, a crash or"
                + " a blast nearby, a blow of the sword, the beam breaking loose, the ring arriving (1 = as the mod makes"
                + " it, 0 = never)")
                .defineInRange("cameraShake", 1.0, 0.0, 2.0);
        RAM_GROUND_SHAKE = builder.comment("How hard your view shakes while you fly with the ram cone low along the"
                + " ground (0 = not at all); the camera shake above scales it too")
                .defineInRange("ramGroundShake", 1.0, 0.0, 3.0);
        builder.pop();
        builder.push("sound");
        THEME_MUSIC = builder.comment("Play the multiverse theme in the main menu (1 = yes, 0 = the game's own menu music)")
                .defineInRange("themeMusic", 1, 0, 1);
        builder.pop();
        builder.push("updates");
        UPDATE_CHECK = builder.comment("How often the game looks for a new version of the mod, in minutes (0 = never)")
                .defineInRange("updateCheckMinutes", 5, 0, 120);
        UPDATE_POPUP = builder.comment("How long the note about a new version stays on screen while you play, in"
                + " seconds (0 = only in the menus)")
                .defineInRange("updatePopupSeconds", 15.0, 0.0, 120.0);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientSettings() {
    }

    public static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static boolean themeMusic() {
        return get(THEME_MUSIC) != 0;
    }

    public static long updateCheckMs() {
        return get(UPDATE_CHECK) * 60_000L;
    }

    public static long updatePopupMs() {
        return Math.round(get(UPDATE_POPUP) * 1000.0);
    }

    public static float cameraShake() {
        return (float) get(CAMERA_SHAKE);
    }
}
