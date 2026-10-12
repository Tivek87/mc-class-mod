package nl.tivek.multiversepowers.character.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.update.client.UpdateChecker;
import org.slf4j.Logger;

// The newest characters (`GameCharacter.newIn`, the latest version any playable one came in) are marked NEW in the
// power wheel until this player has been them once; who they have been is kept in `config/welcomescreen/characters.json`.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class NewCharacters {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE = "characters.json";
    @Nullable
    private static Set<String> played;

    private NewCharacters() {
    }

    public static boolean isNew(@Nullable GameCharacter character) {
        return character != null && newest().contains(character) && !played().contains(character.getId());
    }

    // The new characters this player has not been yet, newest roster order.
    public static List<GameCharacter> waiting() {
        List<GameCharacter> waiting = new ArrayList<>();
        for (GameCharacter character : newest()) {
            if (!played().contains(character.getId())) {
                waiting.add(character);
            }
        }
        return waiting;
    }

    private static List<GameCharacter> newest() {
        String latest = null;
        for (GameCharacter character : GameCharacter.values()) {
            String in = character.newIn();
            if (character.playable() && in != null && (latest == null || UpdateChecker.compare(in, latest) > 0)) {
                latest = in;
            }
        }
        List<GameCharacter> newest = new ArrayList<>();
        for (GameCharacter character : GameCharacter.values()) {
            if (character.playable() && latest != null && latest.equals(character.newIn())) {
                newest.add(character);
            }
        }
        return newest;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        GameCharacter active = ClientCharacter.active();
        if (active != null && !played().contains(active.getId())) {
            played.add(active.getId());
            save();
        }
    }

    private static Set<String> played() {
        if (played == null) {
            played = new LinkedHashSet<>();
            Path file = file();
            if (Files.exists(file)) {
                try {
                    JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    for (JsonElement id : root.getAsJsonArray("played")) {
                        played.add(id.getAsString());
                    }
                } catch (IOException | RuntimeException e) {
                    LOGGER.warn("Could not read {}: {}", file, e.toString());
                }
            }
        }
        return played;
    }

    private static void save() {
        JsonArray ids = new JsonArray();
        played().forEach(ids::add);
        JsonObject root = new JsonObject();
        root.add("played", ids);
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Could not write {}: {}", file, e.toString());
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(ModConfigs.FOLDER).resolve(FILE);
    }
}
