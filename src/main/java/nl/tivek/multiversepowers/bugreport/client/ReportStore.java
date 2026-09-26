package nl.tivek.multiversepowers.bugreport.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import nl.tivek.multiversepowers.config.ModConfigs;
import org.slf4j.Logger;

final class ReportStore {
    static final int KEPT = 3;
    private static final String FILE = "reports.json";
    private static final long SAVE_MS = 1_000L;
    private static final long FRESH_MS = 5 * 60_000L;
    private static final long RETRY_MS = 60_000L;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<BugReporter.Kind, Desk> DESKS = new EnumMap<>(BugReporter.Kind.class);
    private static long dirtySince;
    private static long retryAt;

    record Draft(String name, String description, BugReporter.Priority priority) {
        static final Draft EMPTY = new Draft("", "", BugReporter.Priority.MEDIUM);

        boolean ready() {
            return !this.name.isBlank() && !this.description.isBlank();
        }

        Draft withName(String name) {
            return new Draft(name, this.description, this.priority);
        }

        Draft withDescription(String description) {
            return new Draft(this.name, description, this.priority);
        }

        Draft withPriority(BugReporter.Priority priority) {
            return new Draft(this.name, this.description, priority);
        }
    }

    record Sent(int issue, String name, BugReporter.Priority priority, long time, BugReporter.State state,
                String title, long checked) {
        Sent with(BugReporter.Status status, long now) {
            return new Sent(this.issue, this.name, this.priority, this.time, status.state(), status.title(), now);
        }
    }

    private static final class Desk {
        Draft draft = Draft.EMPTY;
        final List<Sent> sent = new ArrayList<>();
        final Set<Integer> checking = new HashSet<>();
        boolean sending;
        @Nullable
        BugReporter.Result result;
        boolean checkFailed;
    }

    private ReportStore() {
    }

    static Draft draft(BugReporter.Kind kind) {
        return desk(kind).draft;
    }

    static void draft(BugReporter.Kind kind, Draft draft) {
        Desk desk = desk(kind);
        if (!desk.draft.equals(draft)) {
            desk.draft = draft;
            if (dirtySince == 0L) {
                dirtySince = System.currentTimeMillis();
            }
        }
    }

    static List<Sent> sent(BugReporter.Kind kind) {
        return List.copyOf(desk(kind).sent);
    }

    static boolean sending(BugReporter.Kind kind) {
        return desk(kind).sending;
    }

    @Nullable
    static BugReporter.Result result(BugReporter.Kind kind) {
        return desk(kind).result;
    }

    static boolean checking(BugReporter.Kind kind) {
        return !desk(kind).checking.isEmpty();
    }

    static boolean checkFailed(BugReporter.Kind kind) {
        return desk(kind).checkFailed;
    }

    static void send(BugReporter.Kind kind) {
        Desk desk = desk(kind);
        Draft draft = desk.draft;
        if (desk.sending || !draft.ready()) {
            return;
        }
        desk.sending = true;
        desk.result = null;
        BugReporter.send(kind, draft.name(), draft.description(), draft.priority())
                .thenAcceptAsync(result -> sent(kind, draft, result), Minecraft.getInstance());
    }

    private static void sent(BugReporter.Kind kind, Draft draft, BugReporter.Result result) {
        Desk desk = desk(kind);
        desk.sending = false;
        desk.result = result;
        if (result.outcome() != BugReporter.Outcome.SENT) {
            return;
        }
        long now = System.currentTimeMillis();
        String name = draft.name().strip();
        desk.sent.add(0, new Sent(result.issue(), name, draft.priority(), now, BugReporter.State.OPEN, name, now));
        trim(desk.sent);
        if (desk.draft.equals(draft)) {
            desk.draft = Draft.EMPTY;
        }
        save();
    }

    static void check(BugReporter.Kind kind) {
        Desk desk = desk(kind);
        long now = System.currentTimeMillis();
        if (now < retryAt) {
            return;
        }
        for (Sent entry : desk.sent) {
            int issue = entry.issue();
            if (now - entry.checked() >= FRESH_MS && desk.checking.add(issue)) {
                desk.checkFailed = false;
                BugReporter.status(issue).whenCompleteAsync((status, error) -> checked(kind, issue, status, error),
                        Minecraft.getInstance());
            }
        }
    }

    private static void checked(BugReporter.Kind kind, int issue, @Nullable BugReporter.Status status,
                                @Nullable Throwable error) {
        Desk desk = desk(kind);
        desk.checking.remove(issue);
        long now = System.currentTimeMillis();
        if (status == null) {
            desk.checkFailed = true;
            retryAt = now + RETRY_MS;
            LOGGER.debug("Could not check #{} on GitHub: {}", issue, String.valueOf(error));
            return;
        }
        desk.sent.replaceAll(entry -> entry.issue() == issue ? entry.with(status, now) : entry);
        save();
    }

    static void tick() {
        if (dirtySince != 0L && System.currentTimeMillis() - dirtySince >= SAVE_MS) {
            save();
        }
    }

    static void closed(BugReporter.Kind kind) {
        Desk desk = desk(kind);
        if (!desk.sending) {
            desk.result = null;
        }
        if (dirtySince != 0L) {
            save();
        }
    }

    private static Desk desk(BugReporter.Kind kind) {
        if (DESKS.isEmpty()) {
            load();
        }
        return DESKS.get(kind);
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(ModConfigs.FOLDER).resolve(FILE);
    }

    private static void load() {
        for (BugReporter.Kind kind : BugReporter.Kind.values()) {
            DESKS.put(kind, new Desk());
        }
        Path file = file();
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (BugReporter.Kind kind : BugReporter.Kind.values()) {
                if (root.get(kind.id()) instanceof JsonObject json) {
                    read(DESKS.get(kind), json);
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}: {}", file, e.toString());
        }
    }

    private static void read(Desk desk, JsonObject json) {
        if (json.get("draft") instanceof JsonObject draft) {
            desk.draft = new Draft(BugReporter.string(draft, "name"), BugReporter.string(draft, "description"),
                    priority(draft));
        }
        if (json.get("sent") instanceof JsonArray sent) {
            for (JsonElement element : sent) {
                JsonObject entry = element.getAsJsonObject();
                desk.sent.add(new Sent(entry.get("issue").getAsInt(), BugReporter.string(entry, "name"),
                        priority(entry), entry.get("time").getAsLong(),
                        named(BugReporter.State.class, BugReporter.string(entry, "state"), BugReporter.State.OPEN),
                        BugReporter.string(entry, "title"),
                        entry.has("checked") ? entry.get("checked").getAsLong() : 0L));
            }
            trim(desk.sent);
        }
    }

    private static BugReporter.Priority priority(JsonObject json) {
        return named(BugReporter.Priority.class, BugReporter.string(json, "priority"), Draft.EMPTY.priority());
    }

    private static void save() {
        dirtySince = 0L;
        JsonObject root = new JsonObject();
        DESKS.forEach((kind, desk) -> root.add(kind.id(), json(desk)));
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
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static JsonObject json(Desk desk) {
        JsonObject draft = new JsonObject();
        draft.addProperty("name", desk.draft.name());
        draft.addProperty("description", desk.draft.description());
        draft.addProperty("priority", desk.draft.priority().id());
        JsonArray sent = new JsonArray();
        for (Sent entry : desk.sent) {
            JsonObject json = new JsonObject();
            json.addProperty("issue", entry.issue());
            json.addProperty("name", entry.name());
            json.addProperty("priority", entry.priority().id());
            json.addProperty("time", entry.time());
            json.addProperty("state", entry.state().id());
            json.addProperty("title", entry.title());
            json.addProperty("checked", entry.checked());
            sent.add(json);
        }
        JsonObject json = new JsonObject();
        json.add("draft", draft);
        json.add("sent", sent);
        return json;
    }

    private static void trim(List<Sent> sent) {
        while (sent.size() > KEPT) {
            sent.remove(sent.size() - 1);
        }
    }

    private static <E extends Enum<E>> E named(Class<E> type, String id, E fallback) {
        for (E value : type.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(id)) {
                return value;
            }
        }
        return fallback;
    }
}
