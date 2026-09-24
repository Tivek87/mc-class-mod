package nl.tivek.multiversepowers.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterConfig;
import nl.tivek.multiversepowers.stamina.StaminaConfig;

/**
 * Every settings file of this mod, in one place. There are two kinds:
 *
 * <ul>
 * <li><b>World settings</b>: how the game plays, the same for everyone in a world. Every world keeps its own copy, in
 * {@code <world>/serverconfig/welcomescreen/}, and the server sends it to everyone who joins (see
 * {@link WorldSettings}); the copies in {@code config/welcomescreen/} are what a new world starts with.
 * <ul>
 * <li>{@code stamina.toml}: the stamina bar (see {@link StaminaConfig}).</li>
 * <li>{@code doc_ock.toml}, {@code green_lantern.toml}, ...: one file per character, with a section per ability (see
 * {@link CharacterConfig}).</li>
 * </ul>
 * </li>
 * <li><b>Client settings</b>: what only you see and feel, in {@code config/welcomescreen/client.toml}, in your own game
 * only (the client registers it itself, see {@code config.client.ClientSettings}).</li>
 * </ul>
 *
 * One file per character on purpose: everything about a character stays together, and a new character
 * brings a new file along instead of making one big file longer.
 */
public final class ModConfigs {
    /** The folder inside {@code config} (and inside a world's {@code serverconfig}) that holds this mod's files. */
    public static final String FOLDER = MultiversePowers.MODID;

    // Every world settings file, by its path ("welcomescreen/doc_ock.toml"), and what it holds.
    private static final Map<String, ModConfigSpec> WORLD = new LinkedHashMap<>();

    private ModConfigs() {
    }

    /** Called once when the mod starts: gives every settings file its place. */
    public static void register(ModContainer container, IEventBus modEventBus) {
        world(container, "stamina", StaminaConfig.SPEC);
        CharacterConfig.register(container, modEventBus);
        modEventBus.addListener(ModConfigEvent.Reloading.class, WorldSettings::onReload);
    }

    /** One world settings file: every world keeps its own, and the server sends it to everyone who plays in it. */
    public static void world(ModContainer container, String name, ModConfigSpec spec) {
        String file = file(name);
        container.registerConfig(ModConfig.Type.SERVER, spec, file);
        WORLD.put(file, spec);
    }

    /** Every world settings file, by its path, and what it holds. */
    public static Map<String, ModConfigSpec> worldFiles() {
        return Collections.unmodifiableMap(WORLD);
    }

    /** The path of one settings file: "welcomescreen/doc_ock.toml". */
    public static String file(String name) {
        return FOLDER + "/" + name + ".toml";
    }
}
