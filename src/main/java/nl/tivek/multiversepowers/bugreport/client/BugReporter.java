package nl.tivek.multiversepowers.bugreport.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import nl.tivek.multiversepowers.update.client.UpdateChecker;
import org.slf4j.Logger;

final class BugReporter {
    static final int TITLE_MAX = 80;
    static final int DESCRIPTION_MAX = 2000;
    // Written by scripts/bugs.ps1 setup; a JVM property points tests at a local relay.
    private static final String RELAY_FILE = "relay.txt";
    private static final String RELAY_PROPERTY = "welcomescreen.bugrelay";
    private static final String USER_AGENT = "multiverse-powers-bug-report";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    enum Priority {
        LOW(0x7BD88F), MEDIUM(0xF2C84B), HIGH(0xFF7B7B);

        final int color;

        Priority(int color) {
            this.color = color;
        }

        String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    enum Category {
        POWER, CHARACTER, CHANGE, OTHER;

        String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    enum Kind {
        BUG(""), IDEA("idea.");

        private final String prefix;

        Kind(String prefix) {
            this.prefix = prefix;
        }

        String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        String key(String key) {
            return this.prefix + key;
        }
    }

    enum Outcome { SENT, LIMITED, REJECTED, FAILED }

    record Result(Outcome outcome, int issue) {
    }

    private BugReporter() {
    }

    static String username() {
        return Minecraft.getInstance().getUser().getName();
    }

    static CompletableFuture<Result> send(Kind kind, String title, String description, Category category,
            Priority priority) {
        JsonObject json = new JsonObject();
        json.addProperty("kind", kind.id());
        json.addProperty("title", title.strip());
        json.addProperty("description", description.strip());
        if (kind == Kind.IDEA) {
            json.addProperty("category", category.id());
        }
        json.addProperty("priority", priority.id());
        json.addProperty("username", username());
        json.addProperty("modVersion", UpdateChecker.installed());
        json.addProperty("minecraftVersion", SharedConstants.getCurrentVersion().getName());
        String relay = System.getProperty(RELAY_PROPERTY, relay());
        if (relay.isEmpty()) {
            LOGGER.warn("Bug reports are not set up: run scripts/bugs.ps1 setup");
            return CompletableFuture.completedFuture(new Result(Outcome.FAILED, 0));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(relay))
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                .build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(BugReporter::result)
                .exceptionally(error -> {
                    LOGGER.warn("Could not send the bug report: {}", error.toString());
                    return new Result(Outcome.FAILED, 0);
                });
    }

    private static String relay() {
        try (InputStream in = BugReporter.class.getResourceAsStream(RELAY_FILE)) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            return "";
        }
    }

    private static Result result(HttpResponse<String> response) {
        return switch (response.statusCode()) {
            case 201 -> new Result(Outcome.SENT, JsonParser.parseString(response.body()).getAsJsonObject()
                    .get("issue").getAsInt());
            case 429 -> new Result(Outcome.LIMITED, 0);
            case 400 -> new Result(Outcome.REJECTED, 0);
            default -> {
                LOGGER.warn("The bug report relay answered {}", response.statusCode());
                yield new Result(Outcome.FAILED, 0);
            }
        };
    }
}
