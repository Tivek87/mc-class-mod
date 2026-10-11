package nl.tivek.multiversepowers.schematic.format;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.state.BlockState;
import nl.tivek.multiversepowers.schematic.Schematic;

// The game's own structure files (.nbt, as structure blocks save them): only the cells listed are placed, so a
// structure void stays untouched.
final class StructureFormat {
    private StructureFormat() {
    }

    static Schematic read(String name, CompoundTag root, int largest) throws SchematicException {
        CompoundTag tag = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), root,
                NbtUtils.getDataVersion(root, 500));
        ListTag size = tag.getList("size", Tag.TAG_INT);
        if (size.size() != 3) {
            throw new SchematicException("broken", name);
        }
        int sx = size.getInt(0);
        int sy = size.getInt(1);
        int sz = size.getInt(2);
        SchematicReader.check(sx, sy, sz, largest);
        ListTag paletteTag = tag.contains("palettes", Tag.TAG_LIST) ? tag.getList("palettes", Tag.TAG_LIST).getList(0)
                : tag.getList("palette", Tag.TAG_COMPOUND);
        SchematicReader.palette(paletteTag.size());
        List<BlockState> palette = new ArrayList<>();
        Palettes.Missing missing = new Palettes.Missing();
        for (int k = 0; k < paletteTag.size(); k++) {
            palette.add(Palettes.known(paletteTag.getCompound(k), Palettes.CURRENT, missing, k));
        }
        Schematic schematic = new Schematic(name, sx, sy, sz, palette);
        for (Tag entry : tag.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) entry;
            ListTag pos = block.getList("pos", Tag.TAG_INT);
            int state = block.getInt("state");
            if (pos.size() != 3 || state < 0 || state >= palette.size()) {
                continue;
            }
            int x = pos.getInt(0);
            int y = pos.getInt(1);
            int z = pos.getInt(2);
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) {
                continue;
            }
            int index = schematic.index(x, y, z);
            schematic.cells[index] = (short) state;
            if (block.contains("nbt", Tag.TAG_COMPOUND)) {
                schematic.blockEntities.put(index, block.getCompound("nbt").copy());
            }
        }
        for (Tag entry : tag.getList("entities", Tag.TAG_COMPOUND)) {
            CompoundTag creature = (CompoundTag) entry;
            if (creature.contains("nbt", Tag.TAG_COMPOUND)) {
                schematic.creatures.add(new Schematic.Creature(SchematicReader.vec(creature.getList("pos",
                        Tag.TAG_DOUBLE)), creature.getCompound("nbt").copy()));
            }
        }
        missing.count(schematic);
        return schematic;
    }
}
