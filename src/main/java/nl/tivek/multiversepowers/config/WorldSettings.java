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
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * The world settings (see {@link ModConfigs}): how the game plays, the same for everyone in a world, and kept by every
 * world for itself.
 *
 * <ul>
 * <li>A world keeps its own copy of every world settings file, in {@code <world>/serverconfig/welcomescreen/}. The
 * first time a world opens it gets one of what a new world starts with: the copy in {@code config/welcomescreen/}
 * (made from the mod's own defaults, or from a modpack's {@code defaultconfigs/}, when there is none yet). Changes,
 * in the settings screen or by hand, only ever touch the world's own copy.</li>
 * <li>The server sends the world's settings to everyone who joins (the game does that by itself), and again to everyone
 * in the world whenever they change while it is open, so every player's own game (which moves him, and keeps his
 * stamina) always plays by the same numbers as the server.</li>
 * </ul>
 */
public final class WorldSettings {
    // The folder in a world that the game reads its server settings from before those in config/.
    private static final LevelResource SERVER_CONFIG = new LevelResource("serverconfig");

    private WorldSettings() {
    }

    /**
     * A server starts, right before the game reads its settings: a world that has no copy of its own of a world
     * settings file yet gets one, so the game reads (and saves) that one instead of the shared copy in {@code config/}.
     */
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

    /**
     * What a new world starts with: {@code config/welcomescreen/<file>}, made first when it is missing, from a
     * modpack's {@code defaultconfigs/} or else from the mod's own defaults.
     */
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
        // Brought up to date with the mod first (settings it has now added, ones it no longer has taken out), keeping
        // every number in it, so a new world never starts with an outdated copy that the game would have to set right.
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

    /** A settings file of this spec in its own order, with its explanations, holding every number {@code old} had. */
    private static CommentedConfig rebuilt(ModConfigSpec spec, UnmodifiableConfig old) {
        CommentedConfig fresh = ordered();
        spec.correct(fresh);
        keep(fresh, old);
        // A number out of its range goes back to the default, as the game would do.
        spec.correct(fresh);
        return fresh;
    }

    /** Puts every number {@code from} has in {@code into}, where {@code into} has a setting of that name. */
    private static void keep(Config into, UnmodifiableConfig from) {
        for (Map.Entry<String, Object> entry : into.valueMap().entrySet()) {
            Object had = from.valueMap().get(entry.getKey());
            if (entry.getValue() instanceof Config section) {
                if (had instanceof UnmodifiableConfig oldSection) {
                    keep(section, oldSection);
                }
            } else if (had != null && !(had instanceof UnmodifiableConfig)) {
                entry.setValue(had);
            }
        }
    }

    /** An empty settings file that keeps what is in it in the order it was put there. */
    private static CommentedConfig ordered() {
        return CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
    }

    private static void write(Path file, CommentedConfig config) {
        TomlFormat.instance().createWriter().write(config, file, WritingMode.REPLACE_ATOMIC);
    }

    /**
     * A world settings file was saved or changed on disk while the world is open (on this game's server): everyone in it
     * gets the new numbers. The game only sends them as a player joins by itself.
     */
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
