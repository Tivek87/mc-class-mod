package nl.tivek.multiversepowers.config.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Your own settings (see {@code ModConfigs}): what only you see and feel, in {@code config/welcomescreen/client.toml}.
 * They stay yours whatever world or server you play on, and no one else's game ever hears of them; how the powers
 * themselves play is up to the world settings.
 */
public final class ClientSettings {
    public static final ModConfigSpec SPEC;
    /** How hard your view shakes and jolts from the powers: 1 = as the mod makes it, 0 = never. */
    public static final ModConfigSpec.DoubleValue CAMERA_SHAKE;
    /** How hard your view shakes while you fly with the ram cone low along the ground. */
    public static final ModConfigSpec.DoubleValue RAM_GROUND_SHAKE;

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
        SPEC = builder.build();
    }

    private ClientSettings() {
    }

    /** A setting from your file, or its default before the file is read. */
    public static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** How hard your view shakes and jolts from the powers: 1 = as the mod makes it, 0 = never. */
    public static float cameraShake() {
        return (float) get(CAMERA_SHAKE);
    }
}
