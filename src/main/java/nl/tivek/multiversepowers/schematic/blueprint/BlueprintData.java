package nl.tivek.multiversepowers.schematic.blueprint;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import nl.tivek.multiversepowers.MultiversePowers;

// The blueprints set down in a world, kept with it.
public final class BlueprintData extends SavedData {
    private static final String NAME = MultiversePowers.MODID + "_blueprints";
    private final List<Blueprint> blueprints = new ArrayList<>();
    private int next = 1;

    public static BlueprintData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BlueprintData::new, BlueprintData::load, null), NAME);
    }

    private static BlueprintData load(CompoundTag tag, HolderLookup.Provider registries) {
        BlueprintData data = new BlueprintData();
        ListTag list = tag.getList("blueprints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.blueprints.add(Blueprint.load(list.getCompound(i)));
        }
        data.next = Math.max(1, tag.getInt("next"));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Blueprint blueprint : this.blueprints) {
            list.add(blueprint.save());
        }
        tag.put("blueprints", list);
        tag.putInt("next", this.next);
        return tag;
    }

    public List<Blueprint> all() {
        return List.copyOf(this.blueprints);
    }

    @Nullable
    public Blueprint get(int id) {
        for (Blueprint blueprint : this.blueprints) {
            if (blueprint.id() == id) {
                return blueprint;
            }
        }
        return null;
    }

    int takeId() {
        this.setDirty();
        return this.next++;
    }

    void add(Blueprint blueprint) {
        this.blueprints.add(blueprint);
        this.setDirty();
    }

    @Nullable
    Blueprint remove(int id) {
        Blueprint found = this.get(id);
        if (found != null) {
            this.blueprints.remove(found);
            this.setDirty();
        }
        return found;
    }
}
