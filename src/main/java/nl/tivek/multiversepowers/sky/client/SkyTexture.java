package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;

// Sharp pixels as the moon's, clamped so a box face never bleeds into the next.
final class SkyTexture extends AbstractTexture {
    // The moon's pixel in degrees: the game draws its 32-pixel phase 40 wide, 100 away.
    static final double TEXEL = Math.toDegrees(2.0 * Math.atan(0.2)) / 32.0;

    SkyTexture(NativeImage image) {
        this(image, false);
    }

    // `soft`: blended from pixel to pixel, for a glow.
    SkyTexture(NativeImage image, boolean soft) {
        int width = image.getWidth();
        int height = image.getHeight();
        TextureUtil.prepareImage(this.getId(), 0, width, height);
        image.upload(0, 0, 0, 0, 0, width, height, soft, true, false, true);
    }

    static int pixels(double degrees) {
        return Math.max(3, (int) Math.round(degrees / TEXEL));
    }

    @Override
    public void load(ResourceManager resourceManager) {
    }
}
