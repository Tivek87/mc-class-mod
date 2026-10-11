package nl.tivek.multiversepowers.schematic.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.schematic.format.SchematicReader;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

// The computer's own file windows (TinyFileDialogs, which comes with the game): a schematic picked to import, or a
// downloaded one saved where the player chooses. They run beside the game's thread, so the game keeps drawing.
public final class FileDialogs {
    private static volatile boolean open;

    private FileDialogs() {
    }

    static boolean open() {
        return open;
    }

    public static void pick(Consumer<Path> picked) {
        if (open) {
            return;
        }
        open = true;
        String title = Component.translatable(SchematicClient.PREFIX + "pick").getString();
        String kinds = Component.translatable(SchematicClient.PREFIX + "kinds").getString();
        String from = start().toString() + File.separator;
        run(() -> {
            String chosen;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                chosen = TinyFileDialogs.tinyfd_openFileDialog(title, from, filters(stack, SchematicReader.KINDS),
                        kinds, false);
            }
            if (chosen != null) {
                Path path = Path.of(chosen);
                Minecraft.getInstance().execute(() -> picked.accept(path));
            }
        });
    }

    static void save(String name, byte[] bytes) {
        if (open) {
            return;
        }
        open = true;
        String title = Component.translatable(SchematicClient.PREFIX + "save", name).getString();
        String kinds = Component.translatable(SchematicClient.PREFIX + "kinds").getString();
        String to = start().resolve(name).toString();
        String kind = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT) : ".schem";
        run(() -> {
            String chosen;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                chosen = TinyFileDialogs.tinyfd_saveFileDialog(title, to, filters(stack, new String[] { kind }), kinds);
            }
            if (chosen == null) {
                return;
            }
            String file = chosen.toLowerCase(Locale.ROOT).endsWith(kind) ? chosen : chosen + kind;
            try {
                Files.write(Path.of(file), bytes);
                Minecraft.getInstance().execute(() -> SchematicClient.say(
                        Component.translatable(SchematicClient.PREFIX + "notice.saved", file), false));
            } catch (IOException | RuntimeException e) {
                Minecraft.getInstance().execute(() -> SchematicClient.say(
                        Component.translatable(SchematicClient.PREFIX + "error.save", file), true));
            }
        });
    }

    private static void run(Runnable dialog) {
        Thread thread = new Thread(() -> {
            try {
                dialog.run();
            } finally {
                open = false;
            }
        }, "Schematic file window");
        thread.setDaemon(true);
        thread.start();
    }

    private static PointerBuffer filters(MemoryStack stack, String[] kinds) {
        PointerBuffer filters = stack.mallocPointer(kinds.length);
        for (String kind : kinds) {
            filters.put(stack.UTF8("*" + kind));
        }
        return filters.flip();
    }

    // The player's Downloads folder, else their home.
    private static Path start() {
        Path home = Path.of(System.getProperty("user.home"));
        Path downloads = home.resolve("Downloads");
        return Files.isDirectory(downloads) ? downloads : home;
    }
}
