package nl.tivek.multiversepowers.update.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import nl.tivek.multiversepowers.MultiversePowers;
import org.slf4j.Logger;

/**
 * Downloads a release's jar, checks it, and leaves the swap to {@link UpdateHelper}, which does it once the game has
 * closed ("Update later") or right away with a restart ("Update and restart now").
 *
 * <p>The jar must come from the mod's own release page, match the SHA-256 checksum GitHub lists for it, and hold the
 * mod itself; anything else is thrown away. All of it happens in {@code .multiverse-powers-update} in the game folder.
 */
final class UpdateInstaller {
    enum State { IDLE, DOWNLOADING, READY, FAILED }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FOLDER = ".multiverse-powers-update";
    private static final String HELPER_PREFIX = "updater-";
    private static final String MOD_CLASS = MultiversePowers.class.getName().replace('.', '/') + ".class";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile State state = State.IDLE;
    private static volatile float progress;
    @Nullable
    private static Component error;
    /** The release being downloaded, or downloaded and waiting for the game to close. */
    @Nullable
    private static Release target;
    @Nullable
    private static Path downloaded;
    private static boolean restartWanted;
    private static boolean helperStarted;
    /** The mod's own jar; stays null in a development run, where the mod is a folder. */
    @Nullable
    private static Path modJar;

    private UpdateInstaller() {
    }

    static State state() {
        return state;
    }

    static float progress() {
        return progress;
    }

    @Nullable
    static Component error() {
        return error;
    }

    /** Whether that release is downloaded and will be put in place when the game closes. */
    static boolean scheduled(Release release) {
        return state == State.READY && target != null && target.version().equals(release.version());
    }

    /** Whether the running mod is a jar that can be swapped (not in a development run). */
    static boolean canInstall() {
        return modJar() != null;
    }

    @Nullable
    private static Path modJar() {
        if (modJar == null) {
            Path file = ModList.get().getModFileById(MultiversePowers.MODID).getFile().getFilePath();
            if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(".jar")) {
                modJar = file;
            }
        }
        return modJar;
    }

    /** Absolute: the helper runs from inside this folder, where a path relative to the game folder means nothing. */
    private static Path folder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FOLDER).toAbsolutePath().normalize();
    }

    /** Downloads it now; the game keeps running, the new jar goes in when the game closes. */
    static void updateLater(Release release) {
        begin(release, false);
    }

    /** Downloads it, then closes the game and starts it again with the new jar. */
    static void updateAndRestart(Release release) {
        begin(release, true);
    }

    private static void begin(Release release, boolean restart) {
        if (!canInstall() || release.jar() == null) {
            return;
        }
        boolean same = target != null && target.version().equals(release.version());
        if (same && state == State.DOWNLOADING) {
            restartWanted |= restart;
            return;
        }
        if (same && state == State.READY) {
            restartWanted |= restart;
            finish();
            return;
        }
        target = release;
        restartWanted = restart;
        error = null;
        progress = 0.0F;
        state = State.DOWNLOADING;
        CompletableFuture.supplyAsync(() -> download(release)).whenComplete((jar, failure) ->
                Minecraft.getInstance().execute(() -> downloaded(release, jar, failure)));
    }

    private static void downloaded(Release release, @Nullable Path jar, @Nullable Throwable failure) {
        if (target != release) {
            return;
        }
        if (failure != null || jar == null) {
            Throwable cause = failure != null && failure.getCause() != null ? failure.getCause() : failure;
            LOGGER.warn("Update download failed", cause);
            error = cause instanceof UpdateException problem ? problem.reason
                    : Component.translatable("screen." + MultiversePowers.MODID + ".update.error.network");
            state = State.FAILED;
            return;
        }
        downloaded = jar;
        try {
            writePlan(false);
            startHelper();
        } catch (IOException e) {
            LOGGER.warn("Could not prepare the update", e);
            error = Component.translatable("screen." + MultiversePowers.MODID + ".update.error.files");
            state = State.FAILED;
            return;
        }
        state = State.READY;
        UpdatePopup.hide();
        finish();
    }

    /** With "restart now": closes the game (which saves the world), the helper takes it from there. */
    private static void finish() {
        if (!restartWanted) {
            return;
        }
        boolean relaunch = false;
        List<String> arguments = Relaunch.possible() ? Relaunch.arguments() : null;
        try {
            if (arguments != null) {
                Relaunch.writeArgumentFile(folder().resolve(UpdateHelper.RELAUNCH), arguments);
                relaunch = true;
            }
            writePlan(relaunch);
        } catch (IOException e) {
            LOGGER.warn("Could not prepare the restart", e);
            relaunch = false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new GenericMessageScreen(Component.translatable("screen." + MultiversePowers.MODID
                + (relaunch ? ".update.restarting" : ".update.closing"))));
        minecraft.stop();
    }

    private static Path download(Release release) {
        Release.Asset asset = release.jar();
        try {
            Path folder = folder();
            Files.createDirectories(folder);
            Path part = folder.resolve(asset.name() + ".part");
            HttpRequest request = HttpRequest.newBuilder(asset.url())
                    .header("User-Agent", UpdateChecker.USER_AGENT)
                    .timeout(Duration.ofMinutes(5))
                    .build();
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new IOException("GitHub answered " + response.statusCode());
            }
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            long total = 0L;
            try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(part)) {
                byte[] buffer = new byte[64 * 1024];
                for (int read; (read = in.read(buffer)) >= 0; ) {
                    out.write(buffer, 0, read);
                    sha256.update(buffer, 0, read);
                    total += read;
                    if (asset.size() > 0) {
                        progress = Math.min(1.0F, (float) total / asset.size());
                    }
                }
            }
            String checksum = HexFormat.of().formatHex(sha256.digest()).toLowerCase(Locale.ROOT);
            if ((asset.size() > 0 && total != asset.size()) || !checksum.equals(asset.sha256())) {
                Files.deleteIfExists(part);
                throw new UpdateException("checksum");
            }
            if (!holdsTheMod(part)) {
                Files.deleteIfExists(part);
                throw new UpdateException("jar");
            }
            Path jar = folder.resolve(asset.name());
            Files.move(part, jar, StandardCopyOption.REPLACE_EXISTING);
            progress = 1.0F;
            return jar;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static boolean holdsTheMod(Path jar) {
        return holds(jar, MOD_CLASS) && holds(jar, "META-INF/neoforge.mods.toml");
    }

    private static boolean holds(Path jar, String entry) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            return zip.getEntry(entry) != null;
        } catch (IOException e) {
            return false;
        }
    }

    /** What the helper does once the game is gone; written again whenever the plan changes. */
    private static void writePlan(boolean relaunch) throws IOException {
        Path oldJar = modJar();
        Properties plan = new Properties();
        plan.setProperty("old", oldJar.toString());
        plan.setProperty("new", downloaded.toString());
        plan.setProperty("target", oldJar.resolveSibling(target.jar().name()).toString());
        plan.setProperty("relaunch", Boolean.toString(relaunch));
        plan.setProperty("java", Relaunch.javaCommand());
        plan.setProperty("workdir", Path.of(System.getProperty("user.dir")).toAbsolutePath().toString());
        try (Writer writer = Files.newBufferedWriter(folder().resolve(UpdateHelper.PLAN), StandardCharsets.UTF_8)) {
            plan.store(writer, "Multiverse Powers update");
        }
    }

    /**
     * One helper per game session: it runs from a copy of this jar, so the jar itself can be swapped. The copy is named
     * after this game's process, as the helper of the session before (which restarted this game) may still hold its own.
     */
    private static void startHelper() throws IOException {
        if (helperStarted) {
            return;
        }
        Path folder = folder();
        Path helperJar = folder.resolve(HELPER_PREFIX + ProcessHandle.current().pid() + ".jar");
        Files.copy(modJar(), helperJar, StandardCopyOption.REPLACE_EXISTING);
        if (!holds(helperJar, UpdateHelper.class.getName().replace('.', '/') + ".class")) {
            throw new IOException("The mod's jar holds no update helper");
        }
        new ProcessBuilder(javaProgram(true), "-cp", helperJar.toString(), UpdateHelper.class.getName(),
                Long.toString(ProcessHandle.current().pid()), folder.toString())
                .directory(folder.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        helperStarted = true;
    }

    /** The Java of this game; on Windows the windowless one for the helper, so no black window pops up. */
    static String javaProgram(boolean windowless) {
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        for (String name : windowless ? new String[] {"javaw.exe", "java.exe", "java"} : new String[] {"java.exe", "java"}) {
            if (Files.isRegularFile(bin.resolve(name))) {
                return bin.resolve(name).toString();
            }
        }
        return "java";
    }

    /** Clears what an earlier update left behind (the helper's log stays, for when something went wrong). */
    static void cleanUp() {
        Path folder = folder();
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
            for (Path file : files) {
                if (!file.getFileName().toString().equals(UpdateHelper.LOG)) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException ignored) {
                        // Still in use by a helper that is just finishing; the next start clears it.
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Could not clear the update folder", e);
        }
    }

    /** A download that arrived but is not the mod's jar. */
    private static final class UpdateException extends IllegalStateException {
        private final Component reason;

        UpdateException(String reason) {
            super(reason);
            this.reason = Component.translatable("screen." + MultiversePowers.MODID + ".update.error." + reason);
        }
    }
}
