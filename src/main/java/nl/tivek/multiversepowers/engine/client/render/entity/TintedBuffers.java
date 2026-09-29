package nl.tivek.multiversepowers.engine.client.render.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

// Whatever is drawn through it comes out solid, in one colour, lit only by its own shape, with no red flash of a hit:
// a creature drawn this way looks carved out of that colour, like a statue of light.
public final class TintedBuffers implements MultiBufferSource {
    private static final RenderType SOLID = RenderType.entitySolid(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"));

    private final MultiBufferSource inner;
    private final int red;
    private final int green;
    private final int blue;

    public TintedBuffers(MultiBufferSource inner, int rgb) {
        this.inner = inner;
        this.red = rgb >> 16 & 0xFF;
        this.green = rgb >> 8 & 0xFF;
        this.blue = rgb & 0xFF;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return new Tinted(this.inner.getBuffer(SOLID), this.red, this.green, this.blue);
    }

    private static final class Tinted implements VertexConsumer {
        private final VertexConsumer inner;
        private final int red;
        private final int green;
        private final int blue;

        Tinted(VertexConsumer inner, int red, int green, int blue) {
            this.inner = inner;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.inner.setColor(this.red, this.green, this.blue, 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.inner.setOverlay(OverlayTexture.NO_OVERLAY);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.inner.setLight(LightTexture.FULL_BRIGHT);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.inner.setNormal(x, y, z);
            return this;
        }
    }
}
