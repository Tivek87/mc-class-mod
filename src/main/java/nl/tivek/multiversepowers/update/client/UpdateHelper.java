package nl.tivek.multiversepowers.update.client;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

public final class UpdateHelper {
    static final String PLAN = "plan.properties";
    static final String LOG = "update.log";
    private static final int TRIES = 120;
    private static final long TRY_PAUSE_MS = 500L;

    private UpdateHelper() {
    }

    public static void main(String[] args) throws Exception {
        long pid = Long.parseLong(args[0]);
        Path folder = Path.of(args[1]);
        log(folder, "waiting for the game (process " + pid + ") to close");
        Optional<ProcessHandle> game = ProcessHandle.of(pid);
        if (game.isPresent()) {
            game.get().onExit().get();
        }
        Properties plan = new Properties();
        Path planFile = folder.resolve(PLAN);
        if (!Files.exists(planFile)) {
            log(folder, "no plan, nothing to do");
            return;
        }
        try (Reader reader = Files.newBufferedReader(planFile, StandardCharsets.UTF_8)) {
            plan.load(reader);
        }
        Files.deleteIfExists(planFile);
        swap(folder, Path.of(plan.getProperty("old")), Path.of(plan.getProperty("new")),
                Path.of(plan.getProperty("target")));
    }

    private static boolean swap(Path folder, Path oldJar, Path newJar, Path target) throws InterruptedException {
        // New jar in first, old one out after: never end up with neither installed.
        if (!retry(folder, "put the new jar in place", () -> Files.move(newJar, target, StandardCopyOption.REPLACE_EXISTING))) {
            return false;
        }
        if (oldJar.equals(target) || retry(folder, "remove the old jar", () -> Files.deleteIfExists(oldJar))) {
            log(folder, "installed " + target.getFileName());
            return true;
        }
        retry(folder, "take the new jar out again", () -> Files.deleteIfExists(target));
        return false;
    }

    private interface Step {
        void run() throws IOException;
    }

    private static boolean retry(Path folder, String what, Step step) throws InterruptedException {
        IOException last = null;
        for (int i = 0; i < TRIES; i++) {
            try {
                step.run();
                return true;
            } catch (IOException e) {
                last = e;
                Thread.sleep(TRY_PAUSE_MS);
            }
        }
        log(folder, "could not " + what + ": " + last);
        return false;
    }

    private static void log(Path folder, String line) {
        try {
            Files.writeString(folder.resolve(LOG), LocalDateTime.now() + " " + line + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
