package nl.tivek.multiversepowers.galactus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

// Galactus's boxes as the game's own pixels: every face cut into cells a model pixel wide, each a shade of its paint
// a little lighter or darker than the next, darker round a plate's edge, cloth woven and steel brushed; faces lit by
// the way they look as blocks are. Made once into buffers kept on the graphics card: the body, its glowing eyes and
// emblem (drawn at full light, day or night), the upper arm and the forearm with the hand.
final class GalactusMesh {
    private static final float TOP = 1.0F;
    private static final float BOTTOM = 0.55F;
    private static final float FRONT = 0.85F;
    private static final float SIDE = 0.7F;
    private static final float EDGE = 0.8F;

    @Nullable
    private VertexBuffer body;
    @Nullable
    private VertexBuffer glow;
    @Nullable
    private VertexBuffer upper;
    @Nullable
    private VertexBuffer lower;
    private boolean made;

    // Draws each part in its own place: the body lit by `light`, the arm by `armLight` (the glow always whole), as
    // solid as `alpha`.
    void draw(Matrix4f body, Matrix4f upper, Matrix4f lower, Matrix4f projection, float light, float armLight,
            float alpha) {
        if (!this.made) {
            this.make();
        }
        ShaderInstance shader = GameRenderer.getPositionColorShader();
        if (shader == null) {
            return;
        }
        RenderSystem.setShaderColor(light, light, light, alpha);
        draw(this.body, body, projection, shader);
        RenderSystem.setShaderColor(armLight, armLight, armLight, alpha);
        draw(this.upper, upper, projection, shader);
        draw(this.lower, lower, projection, shader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        draw(this.glow, body, projection, shader);
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void draw(@Nullable VertexBuffer buffer, Matrix4f pose, Matrix4f projection,
            ShaderInstance shader) {
        if (buffer != null) {
            buffer.bind();
            buffer.drawWithShader(pose, projection, shader);
        }
    }

    private void make() {
        this.made = true;
        GalactusShape shape = new GalactusShape();
        this.body = build(shape.body, false);
        this.glow = build(shape.body, true);
        this.upper = build(shape.upper, false);
        this.lower = build(shape.lower, false);
    }

    @Nullable
    private static VertexBuffer build(List<GalactusShape.Box> boxes, boolean glowing) {
        ByteBufferBuilder memory = new ByteBufferBuilder(1 << 20);
        BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int k = 0; k < boxes.size(); k++) {
            GalactusShape.Box box = boxes.get(k);
            if (box.paint().glows() == glowing) {
                faces(buffer, box, k + 1);
            }
        }
        MeshData mesh = buffer.build();
        VertexBuffer made = null;
        if (mesh != null) {
            made = new VertexBuffer(VertexBuffer.Usage.STATIC);
            made.bind();
            made.upload(mesh);
            VertexBuffer.unbind();
        }
        memory.close();
        return made;
    }

    // The six faces of a box, each cut into cells: (origin, along u, along v) in pixels.
    private static void faces(BufferBuilder buffer, GalactusShape.Box b, int seed) {
        float dx = b.x1() - b.x0();
        float dy = b.y1() - b.y0();
        float dz = b.z1() - b.z0();
        face(buffer, b, seed, 0, TOP, b.x0(), b.y1(), b.z0(), dx, 0, 0, 0, 0, dz);
        face(buffer, b, seed, 1, BOTTOM, b.x0(), b.y0(), b.z0(), dx, 0, 0, 0, 0, dz);
        face(buffer, b, seed, 2, FRONT, b.x0(), b.y0(), b.z1(), dx, 0, 0, 0, dy, 0);
        face(buffer, b, seed, 3, FRONT * 0.9F, b.x0(), b.y0(), b.z0(), dx, 0, 0, 0, dy, 0);
        face(buffer, b, seed, 4, SIDE, b.x1(), b.y0(), b.z0(), 0, 0, dz, 0, dy, 0);
        face(buffer, b, seed, 5, SIDE, b.x0(), b.y0(), b.z0(), 0, 0, dz, 0, dy, 0);
    }

    private static void face(BufferBuilder buffer, GalactusShape.Box box, int seed, int side, float shade, float ox,
            float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz) {
        float lengthU = Math.abs(ux + uy + uz);
        float lengthV = Math.abs(vx + vy + vz);
        int cellsU = Math.max(1, Math.round(lengthU));
        int cellsV = Math.max(1, Math.round(lengthV));
        GalactusShape.Paint paint = box.paint();
        boolean glows = paint.glows();
        for (int i = 0; i < cellsU; i++) {
            for (int j = 0; j < cellsV; j++) {
                float light = glows ? 0.94F + 0.06F * noise(seed, side, i, j) : shade * cell(paint, seed, side, i, j,
                        cellsU, cellsV);
                int color = 0xFF000000 | scale(paint.rgb, light);
                float a0 = i / (float) cellsU;
                float a1 = (i + 1) / (float) cellsU;
                float b0 = j / (float) cellsV;
                float b1 = (j + 1) / (float) cellsV;
                corner(buffer, ox, oy, oz, ux, uy, uz, vx, vy, vz, a0, b0, color);
                corner(buffer, ox, oy, oz, ux, uy, uz, vx, vy, vz, a1, b0, color);
                corner(buffer, ox, oy, oz, ux, uy, uz, vx, vy, vz, a1, b1, color);
                corner(buffer, ox, oy, oz, ux, uy, uz, vx, vy, vz, a0, b1, color);
            }
        }
    }

    // How light one cell of a paint is: a little noise, darker round the face's edge, cloth woven, steel brushed.
    private static float cell(GalactusShape.Paint paint, int seed, int side, int i, int j, int cellsU, int cellsV) {
        float light = 0.93F + 0.14F * noise(seed, side, i, j);
        if (cellsU >= 3 && cellsV >= 3 && (i == 0 || j == 0 || i == cellsU - 1 || j == cellsV - 1)) {
            light *= EDGE;
        }
        switch (paint) {
            case SUIT, SUIT_DARK -> light *= (i + j) % 2 == 0 ? 0.92F : 1.0F;
            case STEEL, STEEL_LIGHT, STEEL_DARK -> light *= 0.95F + 0.1F * noise(seed, side, i, 97);
            default -> {
            }
        }
        return light;
    }

    private static void corner(BufferBuilder buffer, float ox, float oy, float oz, float ux, float uy, float uz,
            float vx, float vy, float vz, float a, float b, int color) {
        buffer.addVertex(ox + ux * a + vx * b, oy + uy * a + vy * b, oz + uz * a + vz * b).setColor(color);
    }

    private static float noise(int seed, int side, int i, int j) {
        int h = seed * 73856093 ^ side * 19349663 ^ i * 83492791 ^ j * 50331653;
        h ^= h >>> 13;
        h *= 0x5BD1E995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535.0F;
    }

    private static int scale(int rgb, float light) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * light));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * light));
        int b = Math.min(255, Math.round((rgb & 0xFF) * light));
        return r << 16 | g << 8 | b;
    }
}
