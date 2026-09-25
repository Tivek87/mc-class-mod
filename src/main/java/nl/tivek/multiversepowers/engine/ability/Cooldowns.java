package nl.tivek.multiversepowers.engine.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class Cooldowns<K> {
    private final Map<UUID, Map<K, int[]>> readyAt = new HashMap<>();
    private final int slots;

    public Cooldowns(int slots) {
        this.slots = slots;
    }

    public int left(ServerPlayer player, K key, int slot) {
        Map<K, int[]> perKey = this.readyAt.get(player.getUUID());
        int[] ready = perKey == null ? null : perKey.get(key);
        return ready == null ? 0 : Math.max(0, ready[slot] - player.server.getTickCount());
    }

    public void start(ServerPlayer player, K key, int slot, int ticks) {
        this.readyAt.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                .computeIfAbsent(key, id -> new int[this.slots])[slot] = player.server.getTickCount() + ticks;
    }

    public void forget(ServerPlayer player) {
        this.readyAt.remove(player.getUUID());
    }

    // Only drops entries with nothing left running, so a relog or death never shortens a cooldown.
    public void forgetReady(ServerPlayer player) {
        Map<K, int[]> perKey = this.readyAt.get(player.getUUID());
        if (perKey == null) {
            return;
        }
        int now = player.server.getTickCount();
        for (int[] ready : perKey.values()) {
            for (int tick : ready) {
                if (tick > now) {
                    return;
                }
            }
        }
        this.readyAt.remove(player.getUUID());
    }

    public void clear() {
        this.readyAt.clear();
    }
}
