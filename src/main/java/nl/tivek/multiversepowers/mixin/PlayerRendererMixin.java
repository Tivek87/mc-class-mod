package nl.tivek.multiversepowers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import nl.tivek.multiversepowers.character.greenlantern.client.body.LanternArms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("HEAD"))
    private void welcomescreen$turnBody(AbstractClientPlayer player, PoseStack pose, float bob, float yBodyRot,
            float partialTick, float scale, CallbackInfo info) {
        LanternArms.turnBody(player, pose, scale);
    }
}
