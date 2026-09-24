package nl.tivek.multiversepowers.character.docock.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilityActionPayload;
import nl.tivek.multiversepowers.character.docock.ThrowGrabPayload;

/**
 * What the mouse does while the tentacles have something in their claws:
 * <ul>
 * <li><b>left click</b> throws it away (a creature, or the blocks they carry);</li>
 * <li><b>right click</b> puts carried blocks back down where you aim, instead of using your item.</li>
 * </ul>
 * Both only while the tentacles really hold something; otherwise the mouse does its normal job.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientGrabState {
    private static boolean holding;
    private static boolean blocks;

    private ClientGrabState() {
    }

    public static void set(boolean throwable, boolean carrying) {
        holding = throwable;
        blocks = carrying;
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (holding && event.isAttack()) {
            holding = false;
            blocks = false;
            event.setCanceled(true);
            event.setSwingHand(true);
            PacketDistributor.sendToServer(new ThrowGrabPayload());
        } else if (blocks && event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(true);
            PacketDistributor.sendToServer(new AbilityActionPayload(AbilityActionPayload.PLACE, true, 0));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        holding = false;
        blocks = false;
    }
}
