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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import nl.tivek.multiversepowers.bugreport.client.form.Form;
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

    // An unsent report: an idea's category, and every answer typed or picked, of any form of its kind.
    record Draft(String category, Map<String, String> values) {
        static final Draft EMPTY = new Draft("", Map.of());

        Draft {
            values = Map.copyOf(values);
        }
    }

    record Sent(int issue, String name, BugReporter.Priority priority, long time, BugReporter.State state,
                String title, long checked, String category) {
        Sent with(BugReporter.Status status, long now) {
            return new Sent(this.issue, this.name, this.priority, this.time, status.state(), status.title(), now,
                    this.category);
        }
    }

    private static final class Desk {
        Draft draft = Draft.EMPTY;
        final List<Sent> sent = new ArrayList<>();
        final Set<Integer> checking = new HashSet<>();
        boolean sending;
        // The last report sent, as sent: the same one twice is refused.
        String last = "";
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

    static boolean duplicate(BugReporter.Kind kind, BugReporter.Submission report) {
        return desk(kind).last.equals(report.json().toString());
    }

    // Sends `report`, made from the draft by `form`; once sent, the draft loses that form's answers.
    static void send(BugReporter.Kind kind, BugReporter.Submission report, Form form) {
        Desk desk = desk(kind);
        Draft draft = desk.draft;
        if (desk.sending || duplicate(kind, report)) {
            return;
        }
        desk.sending = true;
        desk.result = null;
        BugReporter.send(kind, report)
                .thenAcceptAsync(result -> sent(kind, draft, report, form, result), Minecraft.getInstance());
    }

    private static void sent(BugReporter.Kind kind, Draft draft, BugReporter.Submission report, Form form,
                             BugReporter.Result result) {
        Desk desk = desk(kind);
        desk.sending = false;
        desk.result = result;
        if (result.outcome() != BugReporter.Outcome.SENT) {
            return;
        }
        long now = System.currentTimeMillis();
        String name = report.title().strip();
        desk.sent.add(0, new Sent(result.issue(), name, report.priority(), now, BugReporter.State.OPEN, name, now,
                report.categoryName()));
        trim(desk.sent);
        desk.last = report.json().toString();
        if (desk.draft.equals(draft)) {
            Map<String, String> kept = new LinkedHashMap<>(draft.values());
            kept.keySet().removeIf(form::owns);
            desk.draft = new Draft(draft.category(), kept);
        }
        save();
    }

    static void check(BugReporter.Kind kind) {
        look(kind, FRESH_MS);
    }

    // Looks every report up again now, however fresh its status.
    static void refresh(BugReporter.Kind kind) {
        retryAt = 0L;
        look(kind, 0L);
    }

    private static void look(BugReporter.Kind kind, long fresh) {
        Desk desk = desk(kind);
        long now = System.currentTimeMillis();
        if (now < retryAt) {
            return;
        }
        for (Sent entry : desk.sent) {
            int issue = entry.issue();
            if (now - entry.checked() >= fresh && desk.checking.add(issue)) {
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
                    read(kind, DESKS.get(kind), json);
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}: {}", file, e.toString());
        }
    }

    private static void read(BugReporter.Kind kind, Desk desk, JsonObject json) {
        if (json.get("draft") instanceof JsonObject draft) {
            Map<String, String> values = new LinkedHashMap<>();
            String category = BugReporter.string(draft, "category");
            if (draft.get("values") instanceof JsonObject saved) {
                saved.entrySet().forEach(entry -> {
                    if (entry.getValue().isJsonPrimitive()) {
                        values.put(entry.getKey(), entry.getValue().getAsString());
                    }
                });
            } else {
                // A draft from before the forms: its name, description and priority, an idea of no category.
                String form = kind == BugReporter.Kind.BUG ? "bug." : "idea.";
                values.put(form + "name", BugReporter.string(draft, "name"));
                values.put(form + "description", BugReporter.string(draft, "description"));
                values.put(form + "priority", priority(draft).id());
                if (kind == BugReporter.Kind.IDEA && !values.values().stream().allMatch(String::isBlank)) {
                    category = FeedbackForms.GLOBAL;
                }
            }
            desk.draft = new Draft(category, values);
        }
        desk.last = BugReporter.string(json, "last");
        if (json.get("sent") instanceof JsonArray sent) {
            for (JsonElement element : sent) {
                JsonObject entry = element.getAsJsonObject();
                desk.sent.add(new Sent(entry.get("issue").getAsInt(), BugReporter.string(entry, "name"),
                        priority(entry), entry.get("time").getAsLong(),
                        named(BugReporter.State.class, BugReporter.string(entry, "state"), BugReporter.State.OPEN),
                        BugReporter.string(entry, "title"),
                        entry.has("checked") ? entry.get("checked").getAsLong() : 0L,
                        BugReporter.string(entry, "category")));
            }
            trim(desk.sent);
        }
    }

    private static BugReporter.Priority priority(JsonObject json) {
        return named(BugReporter.Priority.class, BugReporter.string(json, "priority"), BugReporter.Priority.MEDIUM);
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
        draft.addProperty("category", desk.draft.category());
        JsonObject values = new JsonObject();
        desk.draft.values().forEach(values::addProperty);
        draft.add("values", values);
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
            json.addProperty("category", entry.category());
            sent.add(json);
        }
        JsonObject json = new JsonObject();
        json.add("draft", draft);
        json.add("sent", sent);
        json.addProperty("last", desk.last);
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
