package nl.tivek.multiversepowers.schematic.format;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.schematic.Schematic;

// Litematica's .litematic: one or more regions, each a box from its position (a size may run the other way) with a
// palette and its cells packed tight in longs. Every region goes into one schematic; what lies between them is left
// untouched.
final class LitematicFormat {
    private record Region(CompoundTag tag, int minX, int minY, int minZ, int sx, int sy, int sz, int posX, int posY,
            int posZ) {
    }

    private LitematicFormat() {
    }

    static Schematic read(String name, CompoundTag root, int largest) throws SchematicException {
        int data = root.getInt("MinecraftDataVersion");
        CompoundTag regionsTag = root.getCompound("Regions");
        List<Region> regions = new ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (String key : regionsTag.getAllKeys()) {
            CompoundTag tag = regionsTag.getCompound(key);
            CompoundTag pos = tag.getCompound("Position");
            CompoundTag size = tag.getCompound("Size");
            int[] x = span(pos.getInt("x"), size.getInt("x"));
            int[] y = span(pos.getInt("y"), size.getInt("y"));
            int[] z = span(pos.getInt("z"), size.getInt("z"));
            if (x[1] == 0 || y[1] == 0 || z[1] == 0) {
                continue;
            }
            regions.add(new Region(tag, x[0], y[0], z[0], x[1], y[1], z[1], pos.getInt("x"), pos.getInt("y"),
                    pos.getInt("z")));
            minX = Math.min(minX, x[0]);
            minY = Math.min(minY, y[0]);
            minZ = Math.min(minZ, z[0]);
            maxX = Math.max(maxX, x[0] + x[1]);
            maxY = Math.max(maxY, y[0] + y[1]);
            maxZ = Math.max(maxZ, z[0] + z[1]);
        }
        if (regions.isEmpty()) {
            throw new SchematicException("empty");
        }
        SchematicReader.check((long) maxX - minX, (long) maxY - minY, (long) maxZ - minZ, largest);
        List<BlockState> palette = new ArrayList<>();
        Object2IntOpenHashMap<BlockState> known = new Object2IntOpenHashMap<>();
        known.defaultReturnValue(-1);
        int[] unknown = { 0 };
        List<short[]> remaps = new ArrayList<>();
        for (Region region : regions) {
            ListTag paletteTag = region.tag().getList("BlockStatePalette", Tag.TAG_COMPOUND);
            short[] remap = new short[paletteTag.size()];
            for (int k = 0; k < remap.length; k++) {
                BlockState state = Palettes.known(paletteTag.getCompound(k), data, unknown);
                int at = known.getInt(state);
                if (at < 0) {
                    at = palette.size();
                    SchematicReader.palette(at + 1);
                    known.put(state, at);
                    palette.add(state);
                }
                remap[k] = (short) at;
            }
            remaps.add(remap);
        }
        Schematic schematic = new Schematic(name, maxX - minX, maxY - minY, maxZ - minZ, palette);
        schematic.unknown = unknown[0];
        for (int r = 0; r < regions.size(); r++) {
            fill(schematic, regions.get(r), remaps.get(r), minX, minY, minZ, data);
        }
        return schematic;
    }

    // Where a region starts and how far it runs along one axis: a negative size runs back from its position.
    private static int[] span(int pos, int size) {
        return size < 0 ? new int[] { pos + size + 1, -size } : new int[] { pos, size };
    }

    private static void fill(Schematic schematic, Region region, short[] remap, int minX, int minY, int minZ, int data)
            throws SchematicException {
        long[] longs = region.tag().getLongArray("BlockStates");
        int bits = Packed.bits(Math.max(1, remap.length));
        int ox = region.minX() - minX;
        int oy = region.minY() - minY;
        int oz = region.minZ() - minZ;
        long layer = (long) region.sx() * region.sz();
        for (int y = 0; y < region.sy(); y++) {
            for (int z = 0; z < region.sz(); z++) {
                for (int x = 0; x < region.sx(); x++) {
                    int id = Packed.tight(longs, bits, y * layer + (long) z * region.sx() + x);
                    if (id < 0) {
                        throw new SchematicException("broken", schematic.name);
                    }
                    if (id < remap.length) {
                        schematic.cells[schematic.index(ox + x, oy + y, oz + z)] = remap[id];
                    }
                }
            }
        }
        for (Tag entry : region.tag().getList("TileEntities", Tag.TAG_COMPOUND)) {
            CompoundTag be = ((CompoundTag) entry).copy();
            int x = be.getInt("x");
            int y = be.getInt("y");
            int z = be.getInt("z");
            if (x < 0 || y < 0 || z < 0 || x >= region.sx() || y >= region.sy() || z >= region.sz()) {
                continue;
            }
            be.remove("x");
            be.remove("y");
            be.remove("z");
            schematic.blockEntities.put(schematic.index(ox + x, oy + y, oz + z), Palettes.blockEntity(be, data));
        }
        // A creature's place is kept from the region's position, not from its corner.
        Vec3 shift = new Vec3(region.posX() - minX, region.posY() - minY, region.posZ() - minZ);
        for (Tag entry : region.tag().getList("Entities", Tag.TAG_COMPOUND)) {
            CompoundTag creature = (CompoundTag) entry;
            Vec3 pos = SchematicReader.vec(creature.getList("Pos", Tag.TAG_DOUBLE)).add(shift);
            schematic.creatures.add(new Schematic.Creature(pos, Palettes.creature(creature.copy(), data)));
        }
    }
}
