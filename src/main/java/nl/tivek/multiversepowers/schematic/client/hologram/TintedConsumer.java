package nl.tivek.multiversepowers.schematic.client.hologram;

import com.mojang.blaze3d.vertex.VertexConsumer;

// Vertices passed on with their colour multiplied by a tint: a block drawn by the game's own block drawing, coloured.
final class TintedConsumer implements VertexConsumer {
    private final VertexConsumer out;
    private final int red;
    private final int green;
    private final int blue;

    TintedConsumer(VertexConsumer out, int tint) {
        this.out = out;
        this.red = tint >> 16 & 0xFF;
        this.green = tint >> 8 & 0xFF;
        this.blue = tint & 0xFF;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.out.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        this.out.setColor(r * this.red / 255, g * this.green / 255, b * this.blue / 255, a);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.out.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.out.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.out.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.out.setNormal(x, y, z);
        return this;
    }
}
