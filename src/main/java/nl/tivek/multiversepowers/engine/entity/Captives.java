package nl.tivek.multiversepowers.engine.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

// Players a power holds who may fight their way out: while held they move and use no power, and their own game shows
// them an escape game (`game`, as content numbers them; CaptivePayload). Won, their game says so (EscapePayload) and
// the power that holds them lets go.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Captives {
    @FunctionalInterface
    public interface Escape {
        void escaped(ServerPlayer captive);
    }

    // `fastest`: ticks no honest game can be won in. A win told sooner (a changed client) waits until then.
    private static final class Hold {
        final Escape escape;
        final int fastest;
        int age;
        boolean won;

        Hold(Escape escape, int fastest) {
            this.escape = escape;
            this.fastest = fastest;
        }
    }

    private static final Map<UUID, Hold> HELD = new HashMap<>();

    private Captives() {
    }

    public static void hold(ServerPlayer captive, int game, int fastest, Escape escape) {
        HELD.put(captive.getUUID(), new Hold(escape, fastest));
        PacketDistributor.sendToPlayer(captive, new CaptivePayload(game));
    }

    public static void release(ServerPlayer captive) {
        if (HELD.remove(captive.getUUID()) != null) {
            PacketDistributor.sendToPlayer(captive, new CaptivePayload(0));
        }
    }

    public static boolean held(ServerPlayer player) {
        return HELD.containsKey(player.getUUID());
    }

    // The captive's game says they won.
    public static void escaped(ServerPlayer captive) {
        Hold hold = HELD.get(captive.getUUID());
        if (hold == null) {
            return;
        }
        hold.won = true;
        if (hold.age >= hold.fastest) {
            free(captive, hold);
        }
    }

    private static void free(ServerPlayer captive, Hold hold) {
        HELD.remove(captive.getUUID());
        PacketDistributor.sendToPlayer(captive, new CaptivePayload(0));
        hold.escape.escaped(captive);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (HELD.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Hold> entry : new ArrayList<>(HELD.entrySet())) {
            Hold hold = entry.getValue();
            hold.age++;
            ServerPlayer captive = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (captive == null) {
                HELD.remove(entry.getKey());
            } else if (hold.won && hold.age >= hold.fastest) {
                free(captive, hold);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HELD.remove(event.getEntity().getUUID());
    }

    public static void clear() {
        HELD.clear();
    }
}
