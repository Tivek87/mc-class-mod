package nl.tivek.multiversepowers.schematic.paste;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

// What a paste replaced, block by block (where, the old state's id and an old block entity's data), in which world, and
// the creatures it brought, so it can be put back. Past `most` blocks it gives up: that paste cannot be undone.
final class PasteUndo {
    final String name;
    final ResourceKey<Level> level;
    private final int most;
    long[] places = new long[1024];
    int[] states = new int[1024];
    final Int2ObjectMap<CompoundTag> blockEntities = new Int2ObjectOpenHashMap<>();
    final List<UUID> creatures = new ArrayList<>();
    int count;
    boolean full;

    PasteUndo(String name, ResourceKey<Level> level, int most) {
        this.name = name;
        this.level = level;
        this.most = most;
        this.full = most <= 0;
    }

    void add(long place, int state, @Nullable CompoundTag blockEntity) {
        if (this.full) {
            return;
        }
        if (this.count >= this.most) {
            this.full = true;
            this.places = new long[0];
            this.states = new int[0];
            this.blockEntities.clear();
            return;
        }
        if (this.count == this.places.length) {
            int grown = Math.min(this.most, this.places.length * 2);
            this.places = Arrays.copyOf(this.places, grown);
            this.states = Arrays.copyOf(this.states, grown);
        }
        if (blockEntity != null) {
            this.blockEntities.put(this.count, blockEntity);
        }
        this.places[this.count] = place;
        this.states[this.count] = state;
        this.count++;
    }
}
