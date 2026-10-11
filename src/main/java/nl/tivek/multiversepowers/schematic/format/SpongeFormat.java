package nl.tivek.multiversepowers.schematic.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.schematic.Schematic;

// WorldEdit's and Sponge's .schem: a palette of block states as text and every cell's id as a variable-length number.
// Version 3 keeps them under "Blocks", inside a "Schematic" compound.
final class SpongeFormat {
    // Version 1 says no game version: the first it was made for.
    private static final int FIRST = 1631;

    private SpongeFormat() {
    }

    static boolean is(CompoundTag root) {
        return root.contains("Schematic", Tag.TAG_COMPOUND) || root.contains("Palette", Tag.TAG_COMPOUND)
                || root.contains("BlockData", Tag.TAG_BYTE_ARRAY);
    }

    static Schematic read(String name, CompoundTag root, int largest) throws SchematicException {
        CompoundTag tag = root.contains("Schematic", Tag.TAG_COMPOUND) ? root.getCompound("Schematic") : root;
        int version = tag.getInt("Version");
        int data = tag.contains("DataVersion") ? tag.getInt("DataVersion") : FIRST;
        int sx = tag.getShort("Width") & 0xFFFF;
        int sy = tag.getShort("Height") & 0xFFFF;
        int sz = tag.getShort("Length") & 0xFFFF;
        SchematicReader.check(sx, sy, sz, largest);
        boolean three = version >= 3 || tag.contains("Blocks", Tag.TAG_COMPOUND);
        CompoundTag blocks = three ? tag.getCompound("Blocks") : tag;
        CompoundTag paletteTag = blocks.getCompound("Palette");
        byte[] ids = three ? blocks.getByteArray("Data") : blocks.getByteArray("BlockData");
        ListTag blockEntities = three ? blocks.getList("BlockEntities", Tag.TAG_COMPOUND)
                : tag.contains("BlockEntities") ? tag.getList("BlockEntities", Tag.TAG_COMPOUND)
                        : tag.getList("TileEntities", Tag.TAG_COMPOUND);
        int most = 0;
        for (String key : paletteTag.getAllKeys()) {
            most = Math.max(most, paletteTag.getInt(key));
        }
        SchematicReader.palette(paletteTag.size());
        if (most > Short.MAX_VALUE * 2) {
            throw new SchematicException("palette", most);
        }
        short[] remap = new short[most + 1];
        Arrays.fill(remap, Schematic.VOID);
        List<BlockState> palette = new ArrayList<>();
        Palettes.Missing missing = new Palettes.Missing();
        for (String key : paletteTag.getAllKeys()) {
            remap[paletteTag.getInt(key)] = (short) palette.size();
            palette.add(Palettes.known(Palettes.tag(key), data, missing, palette.size()));
        }
        Schematic schematic = new Schematic(name, sx, sy, sz, palette);
        int[] cells = Packed.varints(ids, schematic.volume());
        if (cells == null) {
            throw new SchematicException("broken", name);
        }
        for (int i = 0; i < cells.length; i++) {
            schematic.cells[i] = cells[i] < remap.length ? remap[cells[i]] : Schematic.VOID;
        }
        missing.count(schematic);
        for (Tag entry : blockEntities) {
            CompoundTag be = (CompoundTag) entry;
            int[] pos = be.getIntArray("Pos");
            if (pos.length != 3 || pos[0] < 0 || pos[1] < 0 || pos[2] < 0 || pos[0] >= sx || pos[1] >= sy
                    || pos[2] >= sz) {
                continue;
            }
            schematic.blockEntities.put(schematic.index(pos[0], pos[1], pos[2]),
                    Palettes.blockEntity(own(be, three), data));
        }
        int[] offset = tag.getIntArray("Offset");
        for (Tag entry : tag.getList("Entities", Tag.TAG_COMPOUND)) {
            CompoundTag creature = (CompoundTag) entry;
            Vec3 pos = SchematicReader.vec(creature.getList("Pos", Tag.TAG_DOUBLE));
            // Some tools save where it stood in the world rather than in the schematic.
            if (offset.length == 3 && !inside(pos, sx, sy, sz)) {
                pos = pos.subtract(offset[0], offset[1], offset[2]);
            }
            schematic.creatures.add(new Schematic.Creature(pos, Palettes.creature(own(creature, three), data)));
        }
        return schematic;
    }

    // The saved data as the game keeps it: the fields themselves (in version 3 under "Data"), with "id".
    private static CompoundTag own(CompoundTag entry, boolean three) {
        CompoundTag tag = three && entry.contains("Data", Tag.TAG_COMPOUND) ? entry.getCompound("Data").copy()
                : entry.copy();
        tag.remove("Pos");
        tag.remove("Id");
        tag.remove("Data");
        tag.putString("id", entry.getString("Id"));
        return tag;
    }

    private static boolean inside(Vec3 pos, int sx, int sy, int sz) {
        return pos.x >= -1.0 && pos.y >= -1.0 && pos.z >= -1.0 && pos.x <= sx + 1.0 && pos.y <= sy + 1.0
                && pos.z <= sz + 1.0;
    }
}
