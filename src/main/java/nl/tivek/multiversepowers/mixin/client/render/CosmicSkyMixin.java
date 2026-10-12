package nl.tivek.multiversepowers.mixin.client.render;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import nl.tivek.multiversepowers.sky.client.CosmicSky;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class CosmicSkyMixin {
    // Right after the sky's colour and the sunset's glow, before the sun and moon are laid on.
    @Inject(method = "renderSky", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate("
                    + "Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;"
                    + "Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;"
                    + "Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;"
                    + "Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"))
    private void welcomescreen$cosmos(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
            Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo info) {
        CosmicSky.draw(frustumMatrix, projectionMatrix, partialTick);
    }

    // The second buffer the sky draws is the stars': the planets go on right after them, so no star shines through.
    @Redirect(method = "renderSky", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader("
                    + "Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V"))
    private void welcomescreen$stars(VertexBuffer buffer, Matrix4f modelView, Matrix4f projection,
            ShaderInstance shader) {
        buffer.drawWithShader(modelView, projection, shader);
        CosmicSky.planets();
    }
}
