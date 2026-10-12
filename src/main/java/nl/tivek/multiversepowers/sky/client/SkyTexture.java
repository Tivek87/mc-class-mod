package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

// Mipmapped and clamped, so it stays calm when drawn small and a box face never bleeds into the next.
final class SkyTexture extends AbstractTexture {
    SkyTexture(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int levels = 31 - Integer.numberOfLeadingZeros(Math.max(1, Math.min(width, height) / 4));
        TextureUtil.prepareImage(this.getId(), levels, width, height);
        image.upload(0, 0, 0, 0, 0, width, height, true, true, true, true);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    @Override
    public void load(ResourceManager resourceManager) {
    }
}
