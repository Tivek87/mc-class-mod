package nl.tivek.multiversepowers.engine.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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

    private record Hold(int game, Escape escape) {
    }

    private static final Map<UUID, Hold> HELD = new HashMap<>();

    private Captives() {
    }

    public static void hold(ServerPlayer captive, int game, Escape escape) {
        HELD.put(captive.getUUID(), new Hold(game, escape));
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
        Hold hold = HELD.remove(captive.getUUID());
        if (hold == null) {
            return;
        }
        PacketDistributor.sendToPlayer(captive, new CaptivePayload(0));
        hold.escape().escaped(captive);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HELD.remove(event.getEntity().getUUID());
    }

    public static void clear() {
        HELD.clear();
    }
}
