package nl.tivek.multiversepowers.config.transfer.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.transfer.SettingsExportAskPayload;
import nl.tivek.multiversepowers.config.transfer.SettingsExportPayload;
import nl.tivek.multiversepowers.config.transfer.SettingsImportPayload;
import nl.tivek.multiversepowers.config.transfer.SettingsTransfer;

// World settings kept on this computer: zips in the game folder's `world_settings`, one written whenever the server
// sends an export, one read and sent back in parts to import it. Each can be copied to another computer as it is.
public final class SavedSettings {
    private static final String FOLDER = "world_settings";
    private static final String KEY = "config." + MultiversePowers.MODID + ".share.";
    private static final int NAME = 40;

    public record Saved(String name, Path path, FileTime when) {
    }

    @Nullable
    private static ByteArrayOutputStream incoming;
    // What happened last, for the page to show.
    @Nullable
    private static Component status;
    private static long changed;

    private SavedSettings() {
    }

    public static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve(FOLDER);
    }

    // Every saved export, the newest first.
    public static List<Saved> list() {
        List<Saved> saved = new ArrayList<>();
        Path folder = folder();
        if (!Files.isDirectory(folder)) {
            return saved;
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(
                    ".zip")).forEach(path -> {
                        String file = path.getFileName().toString();
                        try {
                            saved.add(new Saved(file.substring(0, file.length() - 4), path,
                                    Files.getLastModifiedTime(path)));
                        } catch (IOException ignored) {
                            // Gone meanwhile.
                        }
                    });
        } catch (IOException e) {
            MultiversePowers.LOGGER.warn("Could not list {}", folder, e);
        }
        saved.sort(Comparator.comparing(Saved::when).reversed());
        return saved;
    }

    // A name fit for a file: letters, digits, spaces, dashes and underscores, at most NAME long.
    public static String clean(String name) {
        String kept = name.replaceAll("[^\\p{L}\\p{N} _-]", "").trim();
        kept = kept.length() > NAME ? kept.substring(0, NAME).trim() : kept;
        return kept.isEmpty() ? "world settings" : kept;
    }

    public static void export(String name) {
        incoming = null;
        say(Component.translatable(KEY + "exporting"));
        PacketDistributor.sendToServer(new SettingsExportAskPayload(clean(name)));
    }

    // A part of an export from the server; the last one is written to the folder, next to any of the same name.
    public static void receive(SettingsExportPayload payload) {
        if (payload.part() == 0) {
            incoming = new ByteArrayOutputStream();
        }
        if (incoming == null) {
            return;
        }
        incoming.writeBytes(payload.bytes());
        if (payload.part() < payload.parts() - 1) {
            return;
        }
        byte[] zip = incoming.toByteArray();
        incoming = null;
        try {
            Files.createDirectories(folder());
            Path path = free(clean(payload.name()));
            Files.write(path, zip);
            String file = path.getFileName().toString();
            say(Component.translatable(KEY + "exported", file.substring(0, file.length() - 4)));
        } catch (IOException e) {
            MultiversePowers.LOGGER.warn("Could not save the exported world settings", e);
            say(Component.translatable(KEY + "failed"));
        }
    }

    private static Path free(String name) {
        Path path = folder().resolve(name + ".zip");
        for (int k = 2; Files.exists(path); k++) {
            path = folder().resolve(name + " (" + k + ").zip");
        }
        return path;
    }

    // Sends a saved export to the server in parts; it sets what it holds and says how many settings it changed.
    public static void send(Saved saved) {
        byte[] zip;
        try {
            zip = Files.readAllBytes(saved.path());
        } catch (IOException e) {
            say(Component.translatable(KEY + "failed"));
            return;
        }
        int parts = Math.max(1, (zip.length + SettingsTransfer.PART - 1) / SettingsTransfer.PART);
        for (int part = 0; part < parts; part++) {
            int from = part * SettingsTransfer.PART;
            PacketDistributor.sendToServer(new SettingsImportPayload(part, parts, Arrays.copyOfRange(zip, from,
                    Math.min(zip.length, from + SettingsTransfer.PART))));
        }
        say(Component.translatable(KEY + "importing", saved.name()));
    }

    public static void openFolder() {
        try {
            Files.createDirectories(folder());
        } catch (IOException e) {
            return;
        }
        Util.getPlatform().openPath(folder());
    }

    // What the server said of an import or export.
    public static void result(String result, int count) {
        say(Component.translatable(KEY + result, count));
    }

    public static void say(Component text) {
        status = text;
        changed = Util.getMillis();
    }

    @Nullable
    public static Component status() {
        return status;
    }

    // When the status last changed, so a page can redraw its list.
    public static long changed() {
        return changed;
    }
}
