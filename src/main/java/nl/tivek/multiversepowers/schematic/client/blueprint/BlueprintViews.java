package nl.tivek.multiversepowers.schematic.client.blueprint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.ModConfigs;

// How this player sees each blueprint, only for themselves: hidden, without its box, a layer at a time, with a guide
// to the nearest block still to do. Kept per world (or server) in config/welcomescreen/blueprints.json.
public final class BlueprintViews {
    public static final int HIDDEN = 1;
    public static final int NO_BOX = 2;
    public static final int LAYERS = 4;
    public static final int GUIDE = 8;
    private static final String FILE = "blueprints.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Map<Integer, Integer>> WORLDS = new HashMap<>();
    private static boolean loaded;

    private BlueprintViews() {
    }

    public static boolean has(int id, int flag) {
        return (flags(id) & flag) != 0;
    }

    public static int flags(int id) {
        load();
        return WORLDS.getOrDefault(world(), Map.of()).getOrDefault(id, 0);
    }

    public static void set(int id, int flag, boolean on) {
        load();
        Map<Integer, Integer> views = WORLDS.computeIfAbsent(world(), key -> new HashMap<>());
        int now = views.getOrDefault(id, 0);
        int next = on ? now | flag : now & ~flag;
        if (next == now) {
            return;
        }
        if (next == 0) {
            views.remove(id);
        } else {
            views.put(id, next);
        }
        save();
    }

    public static void toggle(int id, int flag) {
        set(id, flag, !has(id, flag));
    }

    // Blueprints no longer in this world are forgotten.
    static void keepOnly(Set<Integer> ids) {
        load();
        Map<Integer, Integer> views = WORLDS.get(world());
        if (views != null && views.keySet().retainAll(ids)) {
            save();
        }
    }

    // This world's name: its folder in singleplayer, the server's address otherwise.
    private static String world() {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer local = minecraft.getSingleplayerServer();
        if (local != null) {
            Path folder = local.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
            return "world:" + (folder == null ? "" : folder.toString());
        }
        ServerData server = minecraft.getCurrentServer();
        return "server:" + (server == null ? "" : server.ip);
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(ModConfigs.FOLDER).resolve(FILE);
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = file();
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> world : root.entrySet()) {
                Map<Integer, Integer> views = new HashMap<>();
                for (Map.Entry<String, JsonElement> view : world.getValue().getAsJsonObject().entrySet()) {
                    views.put(Integer.parseInt(view.getKey()), view.getValue().getAsInt());
                }
                WORLDS.put(world.getKey(), views);
            }
        } catch (IOException | RuntimeException e) {
            MultiversePowers.LOGGER.warn("Could not read {}: {}", file, e.toString());
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        WORLDS.forEach((world, views) -> {
            if (!views.isEmpty()) {
                JsonObject json = new JsonObject();
                views.forEach((id, flags) -> json.addProperty(String.valueOf(id), flags));
                root.add(world, json);
            }
        });
        Path file = file();
        Path temp = file.resolveSibling(FILE + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            MultiversePowers.LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }
}
