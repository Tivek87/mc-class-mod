package nl.tivek.multiversepowers.schematic;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// A structure read from a schematic file: its size, the block states it uses and which of them fills each cell (VOID
// where the file says nothing, so nothing is placed there), with its block entities and creatures as their saved data.
// Cells run x first, then z, then y, as in the files.
public final class Schematic {
    public static final short VOID = -1;

    public record Creature(Vec3 pos, CompoundTag tag) {
    }

    public final String name;
    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;
    public final List<BlockState> palette;
    public final short[] cells;
    // By cell; each holds its "id" and data, never its place.
    public final Int2ObjectMap<CompoundTag> blockEntities = new Int2ObjectOpenHashMap<>();
    public final List<Creature> creatures = new ArrayList<>();
    // Block names the game does not know, left as air.
    public int unknown;

    public Schematic(String name, int sizeX, int sizeY, int sizeZ, List<BlockState> palette) {
        this.name = name;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = palette;
        this.cells = new short[sizeX * sizeY * sizeZ];
        Arrays.fill(this.cells, VOID);
    }

    public int index(int x, int y, int z) {
        return (y * this.sizeZ + z) * this.sizeX + x;
    }

    public int volume() {
        return this.cells.length;
    }

    @Nullable
    public BlockState state(int index) {
        short cell = this.cells[index];
        return cell == VOID ? null : this.palette.get(cell);
    }

    @Nullable
    public BlockState state(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= this.sizeX || y >= this.sizeY || z >= this.sizeZ) {
            return null;
        }
        return this.state(this.index(x, y, z));
    }

    // How many cells hold a block other than air.
    public int blocks() {
        boolean[] air = new boolean[this.palette.size()];
        for (int k = 0; k < air.length; k++) {
            air[k] = this.palette.get(k).isAir();
        }
        int count = 0;
        for (short cell : this.cells) {
            if (cell != VOID && !air[cell]) {
                count++;
            }
        }
        return count;
    }
}
