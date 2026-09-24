package nl.tivek.multiversepowers.engine.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * The least time between two of one kind of thing from one player. A game that asks faster (a changed one, or a key
 * held in a loop) only gets one every so many ticks, so it cannot flood everyone near him with what each one starts; a
 * player who plays normally never runs into it.
 */
public final class Throttle {
    private final Map<UUID, Integer> last = new HashMap<>();
    private final int ticks;

    /** @param ticks the least time between two, in ticks */
    public Throttle(int ticks) {
        this.ticks = ticks;
    }

    /** True when this player may do it now, and then it counts as done; false when he did it too short ago. */
    public boolean allow(ServerPlayer player) {
        int now = player.server.getTickCount();
        Integer before = this.last.get(player.getUUID());
        if (before != null && now - before < this.ticks && now >= before) {
            return false;
        }
        this.last.put(player.getUUID(), now);
        return true;
    }

    /** Forgets everyone (the server stops). */
    public void clear() {
        this.last.clear();
    }
}
