package nl.tivek.multiversepowers.faction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.rules.FactionTable;

// What a world remembers of its players and factions: the reputation each player earned or lost with each faction
// (counted from where that faction starts), a standing an operator pinned for one, and the day reputation last
// drifted. `allies` and `enemies` are the pairs of worlds from before factions had a settings file: read once, moved
// there and gone, as the teams such a world had become factions (`Factions.onServerStarted`).
public final class FactionData extends SavedData {
    private static final String NAME = MultiversePowers.MODID + "_factions";
    private final Map<UUID, Map<String, Integer>> earned = new HashMap<>();
    private final Map<UUID, Map<String, Standing>> pins = new HashMap<>();
    private final Set<String> allies = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();
    private long day = -1L;
    private boolean adopted;

    public static FactionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FactionData::new, FactionData::load, null), NAME);
    }

    private static FactionData load(CompoundTag tag, HolderLookup.Provider registries) {
        FactionData data = new FactionData();
        read(tag.getList("allies", Tag.TAG_STRING), data.allies);
        read(tag.getList("enemies", Tag.TAG_STRING), data.enemies);
        data.day = tag.contains("day") ? tag.getLong("day") : -1L;
        data.adopted = tag.getBoolean("adopted");
        CompoundTag players = tag.getCompound("players");
        for (String key : players.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            CompoundTag own = players.getCompound(key);
            CompoundTag earned = own.getCompound("earned");
            for (String faction : earned.getAllKeys()) {
                data.earned.computeIfAbsent(id, k -> new HashMap<>()).put(faction, earned.getInt(faction));
            }
            CompoundTag pins = own.getCompound("pins");
            for (String faction : pins.getAllKeys()) {
                Standing standing = FactionTable.standing(pins.getString(faction));
                if (standing != null) {
                    data.pins.computeIfAbsent(id, k -> new HashMap<>()).put(faction, standing);
                }
            }
        }
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
        tag.putLong("day", this.day);
        tag.putBoolean("adopted", this.adopted);
        if (!this.adopted) {
            tag.put("allies", write(this.allies));
            tag.put("enemies", write(this.enemies));
        }
        CompoundTag players = new CompoundTag();
        Set<UUID> ids = new HashSet<>(this.earned.keySet());
        ids.addAll(this.pins.keySet());
        for (UUID id : ids) {
            CompoundTag own = new CompoundTag();
            CompoundTag earned = new CompoundTag();
            this.earned.getOrDefault(id, Map.of()).forEach(earned::putInt);
            CompoundTag pins = new CompoundTag();
            this.pins.getOrDefault(id, Map.of()).forEach((faction, standing) -> pins.putString(faction,
                    FactionTable.name(standing)));
            own.put("earned", earned);
            own.put("pins", pins);
            players.put(id.toString(), own);
        }
        tag.put("players", players);
        return tag;
    }

    public int earned(UUID player, String faction) {
        return this.earned.getOrDefault(player, Map.of()).getOrDefault(faction, 0);
    }

    public void earned(UUID player, String faction, int value) {
        if (value == this.earned(player, faction)) {
            return;
        }
        if (value == 0) {
            this.earned.getOrDefault(player, new HashMap<>()).remove(faction);
        } else {
            this.earned.computeIfAbsent(player, k -> new HashMap<>()).put(faction, value);
        }
        this.setDirty();
    }

    public Map<String, Integer> earned(UUID player) {
        return this.earned.getOrDefault(player, Map.of());
    }

    public Set<UUID> players() {
        return new HashSet<>(this.earned.keySet());
    }

    @Nullable
    public Standing pin(UUID player, String faction) {
        return this.pins.getOrDefault(player, Map.of()).get(faction);
    }

    public void pin(UUID player, String faction, @Nullable Standing standing) {
        if (standing == null) {
            this.pins.getOrDefault(player, new HashMap<>()).remove(faction);
        } else {
            this.pins.computeIfAbsent(player, k -> new HashMap<>()).put(faction, standing);
        }
        this.setDirty();
    }

    public Map<String, Standing> pins(UUID player) {
        return this.pins.getOrDefault(player, Map.of());
    }

    // A faction that is no more: no one keeps a reputation with it.
    public void forget(String faction) {
        boolean had = false;
        for (Map<String, Integer> own : this.earned.values()) {
            had |= own.remove(faction) != null;
        }
        for (Map<String, Standing> own : this.pins.values()) {
            had |= own.remove(faction) != null;
        }
        if (had) {
            this.setDirty();
        }
    }

    public long day() {
        return this.day;
    }

    public void day(long day) {
        this.day = day;
        this.setDirty();
    }

    Set<String> allies() {
        return this.allies;
    }

    Set<String> enemies() {
        return this.enemies;
    }

    boolean adopted() {
        return this.adopted;
    }

    void adopt() {
        this.allies.clear();
        this.enemies.clear();
        this.adopted = true;
        this.setDirty();
    }
}
