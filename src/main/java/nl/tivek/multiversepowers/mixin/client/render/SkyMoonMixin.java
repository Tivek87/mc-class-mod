package nl.tivek.multiversepowers.mixin.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.world.LocalSky;
import nl.tivek.multiversepowers.sky.client.CosmicSky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class SkyMoonMixin {
    @Unique
    private static final ResourceLocation WELCOMESCREEN$NO_MOON = ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "textures/environment/no_moon.png");

    // The sun and the moon are laid on the sky adding their light: drawn from a clear texture they add none. So the
    // moon goes while something has taken it, and both while the cosmic sky draws its own in their place.
    @ModifyArg(method = "renderSky", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"),
            index = 1)
    private ResourceLocation welcomescreen$moon(ResourceLocation texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return texture;
        }
        boolean moon = texture.getPath().equals("textures/environment/moon_phases.png");
        boolean sun = texture.getPath().equals("textures/environment/sun.png");
        if (moon && LocalSky.moonHidden(minecraft.level) || (moon || sun) && CosmicSky.bodies()) {
            return WELCOMESCREEN$NO_MOON;
        }
        return texture;
    }
}
