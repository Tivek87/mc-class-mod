package nl.tivek.multiversepowers.character.thor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.character.thor.ThrownHammer;
import nl.tivek.multiversepowers.character.thor.client.pose.ThorHammerLayer;

// Thrown Mjolnir: it turns end over end about the middle of its weight, a striking face leading, its rune faces to
// the sides, as big as it was in its thrower's hand.
public final class ThrownHammerRenderer extends EntityRenderer<ThrownHammer> {
    private ThrownHammerRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ThrownHammer.TYPE.get(), ThrownHammerRenderer::new);
    }

    @Override
    public void render(ThrownHammer hammer, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float size = hammer.size();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partialTick, hammer.yRotO, hammer.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.rotLerp(partialTick, hammer.xRotO, hammer.getXRot())));
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(size, size, size);
        float glow = hammer.charged() ? ThorHammerLayer.flicker(hammer.getId(), hammer.tickCount + partialTick) : 0.0F;
        ThorHammerLayer.draw(ThorHammerLayer.HEFT, glow, pose, buffers, light);
        pose.popPose();
        super.render(hammer, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownHammer hammer) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
