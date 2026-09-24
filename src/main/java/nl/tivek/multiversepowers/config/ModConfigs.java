package nl.tivek.multiversepowers.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterConfig;
import nl.tivek.multiversepowers.stamina.StaminaConfig;

/**
 * Every settings file of this mod, in one place. They all live together in one folder,
 * {@code config/welcomescreen}:
 *
 * <ul>
 * <li>{@code stamina.toml}: the stamina bar, the same for everyone (see {@link StaminaConfig}).</li>
 * <li>{@code doc_ock.toml}, {@code green_lantern.toml}, ...: one file per character, with a section
 * per ability (see {@link CharacterConfig}).</li>
 * </ul>
 *
 * One file per character on purpose: everything about a character stays together, and a new character
 * brings a new file along instead of making one big file longer.
 */
public final class ModConfigs {
    /** The folder inside {@code config} that holds every settings file of this mod. */
    public static final String FOLDER = MultiversePowers.MODID;

    private ModConfigs() {
    }

    /** Called once when the mod starts: gives every settings file its place. */
    public static void register(ModContainer container, IEventBus modEventBus) {
        container.registerConfig(ModConfig.Type.COMMON, StaminaConfig.SPEC, file("stamina"));
        CharacterConfig.register(container, modEventBus);
    }

    /** The path of one settings file: "welcomescreen/doc_ock.toml". */
    public static String file(String name) {
        return FOLDER + "/" + name + ".toml";
    }
}
