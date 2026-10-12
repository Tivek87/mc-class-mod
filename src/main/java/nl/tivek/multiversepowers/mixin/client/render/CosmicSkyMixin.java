package nl.tivek.multiversepowers.mixin.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import nl.tivek.multiversepowers.sky.client.CosmicSky;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class CosmicSkyMixin {
    // Right after the game's sun, moon and stars, by day too: the sky's only call that turns blending off.
    @Inject(method = "renderSky", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableBlend()V"))
    private void welcomescreen$cosmos(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
            Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo info) {
        CosmicSky.draw(frustumMatrix, projectionMatrix, partialTick);
    }
}
