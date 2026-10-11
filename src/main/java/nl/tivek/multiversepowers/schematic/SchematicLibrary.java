package nl.tivek.multiversepowers.schematic;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.neoforged.fml.loading.FMLPaths;
import nl.tivek.multiversepowers.schematic.format.SchematicReader;

// The schematic files on the server: the folder 'schematics' in its game folder, with folders of the players' own.
// Every path a player sends is checked to stay inside it.
public final class SchematicLibrary {
    public static final String FOLDER = "schematics";
    private static final int MOST = 500;
    private static final int NAME = 64;

    public record Entry(String name, boolean folder, long size, long modified) {
    }

    private SchematicLibrary() {
    }

    public static Path root() {
        return FMLPaths.GAMEDIR.get().resolve(FOLDER).toAbsolutePath().normalize();
    }

    // The path a player sent ("" is the library itself, parts split by '/'), or null when it would lead out of it.
    @Nullable
    public static Path resolve(String relative) {
        if (relative.length() > 512) {
            return null;
        }
        Path root = root();
        String clean = relative.replace('\\', '/');
        for (String part : clean.split("/")) {
            if (part.equals("..") || part.equals(".") || part.contains(":") || part.startsWith("~")) {
                return null;
            }
        }
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        Path path = clean.isEmpty() ? root : root.resolve(clean).normalize();
        if (!path.startsWith(root)) {
            return null;
        }
        try {
            if (Files.exists(path) && !path.toRealPath().startsWith(root.toRealPath())) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return path;
    }

    public static String relative(Path path) {
        return root().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    // Folders first, then every file the wand can read, each by name.
    public static List<Entry> list(Path folder) throws IOException {
        Files.createDirectories(root());
        List<Entry> entries = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            return entries;
        }
        try (Stream<Path> files = Files.list(folder)) {
            for (Path path : (Iterable<Path>) files::iterator) {
                String name = path.getFileName().toString();
                if (name.startsWith(".")) {
                    continue;
                }
                boolean dir = Files.isDirectory(path);
                if (!dir && !SchematicReader.readable(name)) {
                    continue;
                }
                entries.add(new Entry(name, dir, dir ? 0L : Files.size(path),
                        Files.getLastModifiedTime(path).toMillis()));
                if (entries.size() >= MOST) {
                    break;
                }
            }
        }
        entries.sort(Comparator.comparing((Entry entry) -> !entry.folder())
                .thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
        return entries;
    }

    // A name fit for a file or folder: letters, digits, spaces and - _ ( ) ., never starting with a dot.
    public static String clean(String name) {
        String kept = name.replaceAll("[^\\p{L}\\p{N} ._()\\-]", "").trim();
        while (kept.startsWith(".")) {
            kept = kept.substring(1);
        }
        return kept.length() > NAME ? kept.substring(0, NAME).trim() : kept;
    }

    public static Path makeFolder(Path parent, String name) throws IOException {
        String clean = clean(name);
        if (clean.isEmpty()) {
            throw new IOException("name");
        }
        Path folder = parent.resolve(clean);
        Files.createDirectories(folder);
        return folder;
    }

    // The name `typed` gives a file or folder called `old`: cleaned, a file keeping its kind (its ending); "" when
    // nothing fit is left.
    static String renamed(String old, String typed, boolean folder) {
        String clean = clean(typed);
        if (!folder) {
            int dot = old.lastIndexOf('.');
            String kind = dot < 0 ? "" : old.substring(dot);
            if (clean.toLowerCase(Locale.ROOT).endsWith(kind.toLowerCase(Locale.ROOT))) {
                clean = clean.substring(0, clean.length() - kind.length()).trim();
            }
            clean = clean.isEmpty() ? "" : clean + kind;
        }
        return clean.startsWith(".") ? "" : clean;
    }

    // A file or folder named anew in its own folder; a file keeps its kind (its ending), whatever was typed.
    public static Path rename(Path path, String name) throws IOException {
        if (path.equals(root()) || !Files.exists(path)) {
            throw new IOException("gone");
        }
        String clean = renamed(path.getFileName().toString(), name, Files.isDirectory(path));
        if (clean.isEmpty()) {
            throw new IOException("name");
        }
        Path to = path.resolveSibling(clean);
        if (to.equals(path)) {
            return path;
        }
        // Only the case changed: on Windows the name already "exists" as itself.
        if (Files.exists(to) && !to.getFileName().toString().equalsIgnoreCase(path.getFileName().toString())) {
            throw new IOException("taken");
        }
        return Files.move(path, to);
    }

    // A file, or a folder with all it holds; never the library itself.
    public static void delete(Path path) throws IOException {
        if (path.equals(root()) || !Files.exists(path)) {
            throw new IOException("gone");
        }
        if (!Files.isDirectory(path)) {
            Files.delete(path);
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException failed) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // Writes an imported file into a folder, next to any of the same name; returns where it went.
    public static Path save(Path folder, String fileName, byte[] bytes) throws IOException {
        Path path = free(folder, fileName);
        Files.write(path, bytes);
        return path;
    }

    // A copy of a file beside it, "name (2).schem"; returns the copy.
    public static Path duplicate(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("gone");
        }
        return Files.copy(file, free(file.getParent(), file.getFileName().toString()));
    }

    // Where a file of this name can go in `folder` without writing over another: "name (2).schem" and on.
    private static Path free(Path folder, String fileName) throws IOException {
        String clean = clean(fileName);
        if (!SchematicReader.readable(clean)) {
            throw new IOException("kind");
        }
        Files.createDirectories(folder);
        int dot = clean.lastIndexOf('.');
        String stem = clean.substring(0, dot);
        String kind = clean.substring(dot);
        Path path = folder.resolve(clean);
        for (int k = 2; Files.exists(path); k++) {
            path = folder.resolve(stem + " (" + k + ")" + kind);
        }
        return path;
    }
}
