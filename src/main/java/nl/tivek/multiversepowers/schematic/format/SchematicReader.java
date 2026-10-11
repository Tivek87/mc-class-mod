package nl.tivek.multiversepowers.schematic.format;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.schematic.Schematic;

// Reads a schematic file of any of the kinds players bring: WorldEdit's and Sponge's .schem (versions 1 to 3), old
// MCEdit and Schematica .schematic (numeric ids), Litematica's .litematic (every region in one) and the game's own
// structure .nbt.
public final class SchematicReader {
    public static final String[] KINDS = { ".schem", ".schematic", ".litematic", ".nbt" };
    // The most memory the file's data may take as it is read.
    private static final long QUOTA = 512L * 1024L * 1024L;

    private SchematicReader() {
    }

    public static boolean readable(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String kind : KINDS) {
            if (lower.endsWith(kind)) {
                return true;
            }
        }
        return false;
    }

    // `largest`: the most cells (width times height times length) it may have.
    public static Schematic read(byte[] bytes, String fileName, int largest) throws SchematicException {
        CompoundTag root;
        try {
            root = parse(bytes);
        } catch (IOException | RuntimeException e) {
            throw new SchematicException("unreadable", fileName);
        }
        String name = fileName.replaceFirst("\\.[^.]+$", "");
        String lower = fileName.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".litematic")) {
                return LitematicFormat.read(name, root, largest);
            }
            if (lower.endsWith(".nbt")) {
                return StructureFormat.read(name, root, largest);
            }
            if (SpongeFormat.is(root)) {
                return SpongeFormat.read(name, root, largest);
            }
            if (LegacyFormat.is(root)) {
                return LegacyFormat.read(name, root, largest);
            }
        } catch (SchematicException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SchematicException("unreadable", fileName);
        }
        throw new SchematicException("kind", fileName);
    }

    private static CompoundTag parse(byte[] bytes) throws IOException {
        try {
            return NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.create(QUOTA));
        } catch (ZipException e) {
            return NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)), NbtAccounter.create(QUOTA));
        }
    }

    static void check(long x, long y, long z, int largest) throws SchematicException {
        if (x <= 0 || y <= 0 || z <= 0) {
            throw new SchematicException("empty");
        }
        long volume = x * y * z;
        if (volume > largest) {
            throw new SchematicException("too_big", x + "x" + y + "x" + z, largest);
        }
    }

    static void palette(int size) throws SchematicException {
        if (size > Short.MAX_VALUE) {
            throw new SchematicException("palette", size);
        }
    }

    static Vec3 vec(ListTag list) {
        return list.size() == 3 ? new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2)) : Vec3.ZERO;
    }
}
