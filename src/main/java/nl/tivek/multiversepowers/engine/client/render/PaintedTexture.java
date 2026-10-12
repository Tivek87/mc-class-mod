package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

// A texture painted pixel by pixel in code, no image file: made the first time it is drawn.
public final class PaintedTexture {
    // The colour (ARGB) of pixel (x, y).
    @FunctionalInterface
    public interface Paint {
        int at(int x, int y);
    }

    private final ResourceLocation id;
    private final int width;
    private final int height;
    private final Paint paint;
    private boolean made;

    public PaintedTexture(ResourceLocation id, int width, int height, Paint paint) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.paint = paint;
    }

    public ResourceLocation id() {
        if (!this.made) {
            NativeImage image = new NativeImage(this.width, this.height, false);
            for (int y = 0; y < this.height; y++) {
                for (int x = 0; x < this.width; x++) {
                    int argb = this.paint.at(x, y);
                    // NativeImage keeps its pixels as ABGR.
                    image.setPixelRGBA(x, y, argb & 0xFF00FF00 | (argb & 0xFF) << 16 | argb >> 16 & 0xFF);
                }
            }
            Minecraft.getInstance().getTextureManager().register(this.id, new DynamicTexture(image));
            this.made = true;
        }
        return this.id;
    }
}
