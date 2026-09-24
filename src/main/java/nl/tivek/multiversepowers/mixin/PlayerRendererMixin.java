package nl.tivek.multiversepowers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import nl.tivek.multiversepowers.character.greenlantern.client.body.LanternArms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Green Lantern's whole body turns with his flight and round for a spinning cut (see {@link LanternArms#turnBody}):
 * turned here, while the game draws his model, and not around all of his drawing, so the name over his head that
 * other players see stays upright.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("HEAD"))
    private void welcomescreen$turnBody(AbstractClientPlayer player, PoseStack pose, float bob, float yBodyRot,
            float partialTick, float scale, CallbackInfo info) {
        LanternArms.turnBody(player, pose, scale);
    }
}
