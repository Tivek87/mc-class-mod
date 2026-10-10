package nl.tivek.multiversepowers.mixin.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.world.LocalSky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class SkyMoonMixin {
    @Unique
    private static final ResourceLocation WELCOMESCREEN$NO_MOON = ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "textures/environment/no_moon.png");

    // The moon is laid on the sky adding its light: drawn from a clear texture it adds none.
    @ModifyArg(method = "renderSky", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"),
            index = 1)
    private ResourceLocation welcomescreen$moon(ResourceLocation texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && texture.getPath().equals("textures/environment/moon_phases.png")
                && LocalSky.moonHidden(minecraft.level)) {
            return WELCOMESCREEN$NO_MOON;
        }
        return texture;
    }
}
