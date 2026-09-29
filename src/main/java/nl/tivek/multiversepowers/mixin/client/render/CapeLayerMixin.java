package nl.tivek.multiversepowers.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import nl.tivek.multiversepowers.engine.client.cloth.CapeCloth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A cape drawn as cloth instead of the game's stiff board, when that is on.
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I"
            + "Lnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$cloth(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch, CallbackInfo info) {
        if (CapeCloth.draw(pose, buffers, light, player, ((CapeLayer) (Object) this).getParentModel(), partialTick)) {
            info.cancel();
        }
    }
}
