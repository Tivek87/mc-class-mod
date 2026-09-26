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

public final class ModConfigs {
    public static final String FOLDER = MultiversePowers.MODID;

    private static final Map<String, ModConfigSpec> WORLD = new LinkedHashMap<>();

    private ModConfigs() {
    }

    public static void register(ModContainer container, IEventBus modEventBus) {
        world(container, "general", PowerRules.SPEC);
        world(container, "stamina", StaminaConfig.SPEC);
        CharacterConfig.register(container, modEventBus);
        modEventBus.addListener(ModConfigEvent.Reloading.class, WorldSettings::onReload);
    }

    public static void world(ModContainer container, String name, ModConfigSpec spec) {
        String file = file(name);
        container.registerConfig(ModConfig.Type.SERVER, spec, file);
        WORLD.put(file, spec);
    }

    public static Map<String, ModConfigSpec> worldFiles() {
        return Collections.unmodifiableMap(WORLD);
    }

    public static String file(String name) {
        return FOLDER + "/" + name + ".toml";
    }
}
