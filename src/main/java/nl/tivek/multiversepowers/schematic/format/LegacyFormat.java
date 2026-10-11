package nl.tivek.multiversepowers.schematic.format;

import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.schematic.Schematic;

// MCEdit's and Schematica's old .schematic, from before 1.13: a numeric block id and a data value per cell (ids past 255
// in "AddBlocks"), made into today's blocks the way the game itself turned old worlds into new ones.
final class LegacyFormat {
    private LegacyFormat() {
    }

    static boolean is(CompoundTag root) {
        return root.contains("Blocks", Tag.TAG_BYTE_ARRAY) && root.contains("Data", Tag.TAG_BYTE_ARRAY);
    }

    static Schematic read(String name, CompoundTag root, int largest) throws SchematicException {
        int sx = root.getShort("Width") & 0xFFFF;
        int sy = root.getShort("Height") & 0xFFFF;
        int sz = root.getShort("Length") & 0xFFFF;
        SchematicReader.check(sx, sy, sz, largest);
        byte[] ids = root.getByteArray("Blocks");
        byte[] values = root.getByteArray("Data");
        byte[] add = root.getByteArray("AddBlocks");
        int volume = sx * sy * sz;
        if (ids.length < volume || values.length < volume) {
            throw new SchematicException("broken", name);
        }
        List<BlockState> palette = new ArrayList<>();
        Int2IntOpenHashMap seen = new Int2IntOpenHashMap();
        seen.defaultReturnValue(-1);
        int[] unknown = { 0 };
        short[] cells = new short[volume];
        for (int i = 0; i < volume; i++) {
            int id = ids[i] & 0xFF;
            if ((i >> 1) < add.length) {
                id |= (i & 1) == 0 ? (add[i >> 1] & 0x0F) << 8 : (add[i >> 1] & 0xF0) << 4;
            }
            int key = id << 4 | values[i] & 0x0F;
            int at = seen.get(key);
            if (at < 0) {
                at = palette.size();
                SchematicReader.palette(at + 1);
                palette.add(Palettes.known(old(id, values[i] & 0x0F), Palettes.FLATTENING, unknown));
                seen.put(key, at);
            }
            cells[i] = (short) at;
        }
        Schematic schematic = new Schematic(name, sx, sy, sz, palette);
        schematic.unknown = unknown[0];
        System.arraycopy(cells, 0, schematic.cells, 0, volume);
        for (Tag entry : root.getList("TileEntities", Tag.TAG_COMPOUND)) {
            CompoundTag be = ((CompoundTag) entry).copy();
            int x = be.getInt("x");
            int y = be.getInt("y");
            int z = be.getInt("z");
            if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) {
                continue;
            }
            be.remove("x");
            be.remove("y");
            be.remove("z");
            schematic.blockEntities.put(schematic.index(x, y, z), Palettes.blockEntity(be, Palettes.legacy(be)));
        }
        for (Tag entry : root.getList("Entities", Tag.TAG_COMPOUND)) {
            CompoundTag creature = (CompoundTag) entry;
            Vec3 pos = SchematicReader.vec(creature.getList("Pos", Tag.TAG_DOUBLE));
            schematic.creatures.add(new Schematic.Creature(pos, Palettes.creature(creature.copy(), Palettes.legacy(creature))));
        }
        return schematic;
    }

    // The state an old id and data value became in 1.13 (the plain block when that value meant nothing).
    private static CompoundTag old(int id, int value) {
        Dynamic<?> tag = BlockStateData.getTag(id << 4 | value);
        if (value != 0 && tag.get("Name").asString("minecraft:air").equals("minecraft:air") && id != 0) {
            tag = BlockStateData.getTag(id << 4);
        }
        return tag.getValue() instanceof CompoundTag compound ? compound.copy() : new CompoundTag();
    }
}
