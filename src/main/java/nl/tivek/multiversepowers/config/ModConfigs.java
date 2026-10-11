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
import nl.tivek.multiversepowers.command.CommandAliases;
import nl.tivek.multiversepowers.faction.mob.MobRules;
import nl.tivek.multiversepowers.stamina.StaminaConfig;

public final class ModConfigs {
    public static final String FOLDER = MultiversePowers.MODID;
    public static final String WIP = "!!! UNFINISHED / WORK IN PROGRESS !!! This mod is still being built: settings may"
            + " still change, break or be missing.";

    private static final Map<String, ModConfigSpec> WORLD = new LinkedHashMap<>();

    private ModConfigs() {
    }

    public static void register(ModContainer container, IEventBus modEventBus) {
        world(container, "general", PowerRules.SPEC);
        world(container, "stamina", StaminaConfig.SPEC);
        world(container, "mobs", MobRules.SPEC);
        world(container, "commands", CommandAliases.SPEC);
        CharacterConfig.register(container, modEventBus);
        modEventBus.addListener(ModConfigEvent.Reloading.class, WorldSettings::onReload);
        modEventBus.addListener(ModConfigEvent.Loading.class, CommandAliases::onConfig);
        modEventBus.addListener(ModConfigEvent.Reloading.class, CommandAliases::onConfig);
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
