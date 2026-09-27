package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

// Whatever is drawn through it keeps its own texture and light, its colour multiplied by a tint and washed toward white
// (whiten, 0 to 1: the white a creeper flashes), with no red flash of a hit; glowing, it is lit from within.
public final class ShadedBuffers implements MultiBufferSource {
    private final MultiBufferSource inner;
    private final int red;
    private final int green;
    private final int blue;
    private final boolean glow;
    private final int overlay;

    public ShadedBuffers(MultiBufferSource inner, int rgb, boolean glow, float whiten) {
        this.inner = inner;
        this.red = rgb >> 16 & 0xFF;
        this.green = rgb >> 8 & 0xFF;
        this.blue = rgb & 0xFF;
        this.glow = glow;
        this.overlay = OverlayTexture.pack(whiten, false);
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return new Shaded(this.inner.getBuffer(type), this.red, this.green, this.blue, this.glow, this.overlay);
    }

    private static final class Shaded implements VertexConsumer {
        private final VertexConsumer inner;
        private final int red;
        private final int green;
        private final int blue;
        private final boolean glow;
        private final int overlay;

        Shaded(VertexConsumer inner, int red, int green, int blue, boolean glow, int overlay) {
            this.inner = inner;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.glow = glow;
            this.overlay = overlay;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.inner.setColor(red * this.red / 255, green * this.green / 255, blue * this.blue / 255, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.inner.setOverlay(this.overlay);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            if (this.glow) {
                this.inner.setLight(LightTexture.FULL_BRIGHT);
            } else {
                this.inner.setUv2(u, v);
            }
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.inner.setNormal(x, y, z);
            return this;
        }
    }
}
