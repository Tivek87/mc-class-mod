package nl.tivek.multiversepowers.engine.client.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;

// Whether the world's creatures are being drawn right now: from the cut-out blocks to the end of the entities. A
// creature drawn anywhere else (the inventory, the first-person arm) is left to its own pose by the steps that only
// belong in the world, and their memory of the last frame is not muddled by it.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class EntityPass {
    private static boolean inWorld;

    private EntityPass() {
    }

    public static boolean inWorld() {
        return inWorld;
    }

    // For something that draws creatures of its own right after the world's (bodies left lying where they fell).
    public static void inWorld(boolean drawing) {
        inWorld = drawing;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            inWorld = true;
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            inWorld = false;
        }
    }
}
