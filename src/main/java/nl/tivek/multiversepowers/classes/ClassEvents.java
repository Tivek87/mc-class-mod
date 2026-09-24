package nl.tivek.multiversepowers.classes;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.network.ModNetwork;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class ClassEvents {
    private ClassEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PlayerClass playerClass = ClassData.getClass(serverPlayer);
        if (playerClass != null) {
            ModNetwork.syncClass(serverPlayer, playerClass);
        } else {
            ChoosingState.enter(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer, new OpenWelcomePayload());
        }
    }

    // Respawning creates a new player object, so the chosen class has to be carried over.
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        ClassData.copy(event.getOriginal(), event.getEntity());
    }
}
