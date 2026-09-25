package nl.tivek.multiversepowers.faction;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import nl.tivek.multiversepowers.MultiversePowers;

public final class FactionData extends SavedData {
    private static final String NAME = MultiversePowers.MODID + "_factions";
    private final Set<String> allies = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();

    public static FactionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FactionData::new, FactionData::load, null), NAME);
    }

    private static FactionData load(CompoundTag tag, HolderLookup.Provider registries) {
        FactionData data = new FactionData();
        read(tag.getList("allies", Tag.TAG_STRING), data.allies);
        read(tag.getList("enemies", Tag.TAG_STRING), data.enemies);
        return data;
    }

    private static void read(ListTag list, Set<String> into) {
        for (int i = 0; i < list.size(); i++) {
            into.add(list.getString(i));
        }
    }

    private static ListTag write(Set<String> pairs) {
        ListTag list = new ListTag();
        for (String pair : pairs) {
            list.add(StringTag.valueOf(pair));
        }
        return list;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("allies", write(this.allies));
        tag.put("enemies", write(this.enemies));
        return tag;
    }

    public Standings.Relation relation(String team, String other) {
        String pair = Standings.pair(team, other);
        return this.allies.contains(pair) ? Standings.Relation.ALLY
                : this.enemies.contains(pair) ? Standings.Relation.ENEMY : Standings.Relation.NONE;
    }

    public void set(String team, String other, Standings.Relation relation) {
        String pair = Standings.pair(team, other);
        this.allies.remove(pair);
        this.enemies.remove(pair);
        if (relation == Standings.Relation.ALLY) {
            this.allies.add(pair);
        } else if (relation == Standings.Relation.ENEMY) {
            this.enemies.add(pair);
        }
        this.setDirty();
    }

    public void forget(String team) {
        if (this.allies.removeIf(pair -> holds(pair, team)) | this.enemies.removeIf(pair -> holds(pair, team))) {
            this.setDirty();
        }
    }

    private static boolean holds(String pair, String team) {
        return pair.startsWith(team + "|") || pair.endsWith("|" + team);
    }

    public Set<String> allies() {
        return this.allies;
    }

    public Set<String> enemies() {
        return this.enemies;
    }
}
