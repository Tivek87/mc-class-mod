package nl.tivek.multiversepowers.character.greenlantern.client.minion;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.character.greenlantern.minion.MechMinion;

// The mech's helpers are drawn with the other constructs (MinionPainter, from ClientConstructs): the game's own pass
// only gives them a shadow.
public final class MinionRenderer extends EntityRenderer<MechMinion> {
    private MinionRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6F;
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MechMinion.TYPE.get(), MinionRenderer::new);
    }

    @Override
    public ResourceLocation getTextureLocation(MechMinion minion) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
