package nl.tivek.multiversepowers.engine.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * When things can be used again, per player: the server tick each one is ready on. One table per kind of thing (the
 * abilities of the characters, the spells), where every thing has its own slots: a character eleven, a spell one.
 * Only the server keeps these; clients are told what is left.
 *
 * @param <K> what has cooldowns: a character, a spell
 */
public final class Cooldowns<K> {
    private final Map<UUID, Map<K, int[]>> readyAt = new HashMap<>();
    private final int slots;

    /** @param slots how many cooldowns every thing has */
    public Cooldowns(int slots) {
        this.slots = slots;
    }

    /** Ticks left before this player can use slot {@code slot} of {@code key} again; 0 when it is ready. */
    public int left(ServerPlayer player, K key, int slot) {
        Map<K, int[]> perKey = this.readyAt.get(player.getUUID());
        int[] ready = perKey == null ? null : perKey.get(key);
        return ready == null ? 0 : Math.max(0, ready[slot] - player.server.getTickCount());
    }

    /** Starts a cooldown of {@code ticks} on slot {@code slot} of {@code key}, from now. */
    public void start(ServerPlayer player, K key, int slot, int ticks) {
        this.readyAt.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                .computeIfAbsent(key, id -> new int[this.slots])[slot] = player.server.getTickCount() + ticks;
    }

    /** Every cooldown of this player is ready again at once. */
    public void forget(ServerPlayer player) {
        this.readyAt.remove(player.getUUID());
    }

    /** Every cooldown of everyone is ready again (the server stops). */
    public void clear() {
        this.readyAt.clear();
    }
}
