package nl.tivek.multiversepowers.engine.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class Throttle {
    private final Map<UUID, Integer> last = new HashMap<>();
    private final int ticks;

    public Throttle(int ticks) {
        this.ticks = ticks;
    }

    public boolean allow(ServerPlayer player) {
        int now = player.server.getTickCount();
        Integer before = this.last.get(player.getUUID());
        // now >= before guards a tick count that went backward (server restart, time reset).
        if (before != null && now - before < this.ticks && now >= before) {
            return false;
        }
        this.last.put(player.getUUID(), now);
        return true;
    }

    public void clear() {
        this.last.clear();
    }
}
