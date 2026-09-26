package nl.tivek.multiversepowers.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nl.tivek.multiversepowers.MultiversePowers;

public final class WorldSettings {
    private static final LevelResource SERVER_CONFIG = new LevelResource("serverconfig");

    private WorldSettings() {
    }

    public static void prepare(MinecraftServer server) {
        Path folder = server.getWorldPath(SERVER_CONFIG);
        for (Map.Entry<String, ModConfigSpec> entry : ModConfigs.worldFiles().entrySet()) {
            Path own = folder.resolve(entry.getKey());
            if (Files.exists(own)) {
                continue;
            }
            try {
                Path start = startingCopy(entry.getKey(), entry.getValue());
                Files.createDirectories(own.getParent());
                Files.copy(start, own);
            } catch (IOException | RuntimeException e) {
                // The game then simply reads the shared copy, as it would without this.
                MultiversePowers.LOGGER.warn("Could not give this world its own {}", entry.getKey(), e);
            }
        }
    }

    private static Path startingCopy(String file, ModConfigSpec spec) throws IOException {
        Path start = FMLPaths.CONFIGDIR.get().resolve(file);
        if (!Files.exists(start)) {
            Files.createDirectories(start.getParent());
            Path pack = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(file);
            if (Files.exists(pack)) {
                Files.copy(pack, start);
            } else {
                write(start, rebuilt(spec, ordered()));
                return start;
            }
        }
        CommentedConfig old = ordered();
        try (Reader reader = Files.newBufferedReader(start)) {
            TomlFormat.instance().createParser().parse(reader, old, ParsingMode.REPLACE);
        }
        CommentedConfig now = rebuilt(spec, old);
        if (!TomlFormat.instance().createWriter().writeToString(now).equals(Files.readString(start))) {
            write(start, now);
        }
        return start;
    }

    private static CommentedConfig rebuilt(ModConfigSpec spec, UnmodifiableConfig old) {
        CommentedConfig fresh = ordered();
        spec.correct(fresh);
        keep(fresh, old);
        // A number out of its range goes back to the default, as the game would do.
        spec.correct(fresh);
        return fresh;
    }

    private static void keep(Config into, UnmodifiableConfig from) {
        for (Config.Entry entry : into.entrySet()) {
            Object had = from.getRaw(List.of(entry.getKey()));
            if (entry.getRawValue() instanceof Config section) {
                if (had instanceof UnmodifiableConfig oldSection) {
                    keep(section, oldSection);
                }
            } else if (had != null && !(had instanceof UnmodifiableConfig)) {
                entry.setValue(had);
            }
        }
    }

    private static CommentedConfig ordered() {
        return CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
    }

    private static void write(Path file, CommentedConfig config) {
        TomlFormat.instance().createWriter().write(config, file, WritingMode.REPLACE_ATOMIC);
    }

    static void onReload(ModConfigEvent.Reloading event) {
        ModConfig config = event.getConfig();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || config.getType() != ModConfig.Type.SERVER
                || !ModConfigs.worldFiles().containsKey(config.getFileName())) {
            return;
        }
        // Saved from the settings screen or seen changing on disk: either way not on the server's own thread.
        server.execute(() -> send(config));
    }

    public static boolean mayEdit(ServerPlayer player) {
        return player.server.isSingleplayerOwner(player.getGameProfile()) || player.hasPermissions(2);
    }

    public static void edit(ServerPlayer player, List<WorldSettingsEditPayload.Entry> entries) {
        if (!mayEdit(player)) {
            player.displayClientMessage(Component.translatable("config." + MultiversePowers.MODID + ".denied"), false);
            return;
        }
        Set<String> changed = new LinkedHashSet<>();
        for (WorldSettingsEditPayload.Entry entry : entries) {
            ModConfigSpec spec = ModConfigs.worldFiles().get(entry.file());
            if (spec == null || !spec.isLoaded() || !(spec.getSpec().get(entry.path()) instanceof ModConfigSpec.ValueSpec rule)) {
                continue;
            }
            Object value = spec.getValues().get(entry.path());
            if (value instanceof ModConfigSpec.IntValue whole) {
                int number = (int) Math.round(entry.value());
                if (rule.test(number)) {
                    whole.set(number);
                    changed.add(entry.file());
                }
            } else if (value instanceof ModConfigSpec.DoubleValue decimal && Double.isFinite(entry.value())
                    && rule.test(entry.value())) {
                decimal.set(entry.value());
                changed.add(entry.file());
            }
        }
        for (String file : changed) {
            ModConfigs.worldFiles().get(file).save();
            ModConfig config = net.neoforged.fml.config.ModConfigs.getFileMap().get(file);
            if (config != null) {
                send(config);
            }
        }
        if (!changed.isEmpty()) {
            MultiversePowers.LOGGER.info("{} changed the world settings in {}", player.getGameProfile().getName(), changed);
        }
    }

    private static void send(ModConfig config) {
        byte[] contents;
        try {
            contents = Files.readAllBytes(config.getFullPath());
        } catch (IOException | IllegalStateException e) {
            MultiversePowers.LOGGER.warn("Could not send the changed {} to the players", config.getFileName(), e);
            return;
        }
        PacketDistributor.sendToAllPlayers(new WorldSettingsPayload(config.getFileName(), contents));
    }
}
