package nl.tivek.multiversepowers.engine.client.render.entity;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

// Whatever is drawn through it is cut off at a plane: only what lies on the side the normal points to is drawn, and
// once the model is done (close) the cut is filled with a flat face in one colour, so it looks sliced, not hollow.
public final class ClippedBuffers implements MultiBufferSource {
    private static final RenderType CAP = RenderType.entityCutoutNoCull(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"));
    private static final VertexConsumer NOTHING = new Dropped();
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final int RED = 3;
    private static final int ALPHA = 6;
    private static final int U = 7;
    private static final int V = 8;
    private static final int OVERLAY = 9;
    private static final int LIGHT = 11;
    private static final int NX = 13;
    private static final int VALUES = 16;

    private final MultiBufferSource inner;
    private final Vector3f point;
    private final Vector3f normal;
    private final int color;
    private final List<float[]> cuts = new ArrayList<>();

    public ClippedBuffers(MultiBufferSource inner, Vector3f point, Vector3f normal, int rgb) {
        this.inner = inner;
        this.point = point;
        this.normal = normal;
        this.color = 0xFF000000 | rgb;
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        // Only quads whose every vertex ends in a normal can be cut vertex by vertex; anything else is left out.
        if (type.mode() != VertexFormat.Mode.QUADS || !type.format().contains(VertexFormatElement.NORMAL)) {
            return NOTHING;
        }
        return new Clipped(this.inner.getBuffer(type), this);
    }

    // Fills the cut, a fan from the plane's point to every edge the plane cut; call once the model is drawn.
    public void close() {
        if (this.cuts.isEmpty()) {
            return;
        }
        VertexConsumer buffer = this.inner.getBuffer(CAP);
        float nx = -this.normal.x();
        float ny = -this.normal.y();
        float nz = -this.normal.z();
        for (float[] cut : this.cuts) {
            this.cap(buffer, this.point.x(), this.point.y(), this.point.z(), nx, ny, nz);
            this.cap(buffer, cut[0], cut[1], cut[2], nx, ny, nz);
            this.cap(buffer, cut[3], cut[4], cut[5], nx, ny, nz);
            this.cap(buffer, cut[3], cut[4], cut[5], nx, ny, nz);
        }
        this.cuts.clear();
    }

    private void cap(VertexConsumer buffer, float x, float y, float z, float nx, float ny, float nz) {
        buffer.addVertex(x, y, z, this.color, 0.5F, 0.5F, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, nx, ny,
                nz);
    }

    private float side(float[] vertex) {
        return (vertex[X] - this.point.x()) * this.normal.x() + (vertex[Y] - this.point.y()) * this.normal.y()
                + (vertex[Z] - this.point.z()) * this.normal.z();
    }

    // One plane of Sutherland-Hodgman: a quad cut by a plane keeps 3 to 5 corners, sent on as one or two quads.
    private void clip(VertexConsumer out, float[][] quad) {
        float[] sides = new float[4];
        int kept = 0;
        for (int k = 0; k < 4; k++) {
            sides[k] = this.side(quad[k]);
            if (sides[k] >= 0.0F) {
                kept++;
            }
        }
        if (kept == 0) {
            return;
        }
        if (kept == 4) {
            for (float[] vertex : quad) {
                emit(out, vertex);
            }
            return;
        }
        List<float[]> corners = new ArrayList<>(5);
        float[] first = null;
        float[] second = null;
        for (int k = 0; k < 4; k++) {
            float[] a = quad[k];
            float[] b = quad[(k + 1) % 4];
            float from = sides[k];
            float to = sides[(k + 1) % 4];
            if (from >= 0.0F) {
                corners.add(a);
            }
            if (from >= 0.0F != to >= 0.0F) {
                float[] cut = between(a, b, from / (from - to));
                corners.add(cut);
                if (first == null) {
                    first = cut;
                } else {
                    second = cut;
                }
            }
        }
        int last = corners.size() - 1;
        emit(out, corners.get(0));
        emit(out, corners.get(1));
        emit(out, corners.get(2));
        emit(out, corners.get(Math.min(3, last)));
        if (corners.size() == 5) {
            emit(out, corners.get(0));
            emit(out, corners.get(3));
            emit(out, corners.get(4));
            emit(out, corners.get(4));
        }
        if (first != null && second != null) {
            this.cuts.add(new float[] { first[X], first[Y], first[Z], second[X], second[Y], second[Z] });
        }
    }

    private static float[] between(float[] a, float[] b, float u) {
        float[] c = new float[VALUES];
        for (int k = 0; k < VALUES; k++) {
            c[k] = a[k] + (b[k] - a[k]) * u;
        }
        return c;
    }

    private static void emit(VertexConsumer out, float[] v) {
        out.addVertex(v[X], v[Y], v[Z])
                .setColor(Math.round(v[RED]), Math.round(v[RED + 1]), Math.round(v[RED + 2]), Math.round(v[ALPHA]))
                .setUv(v[U], v[V])
                .setUv1(Math.round(v[OVERLAY]), Math.round(v[OVERLAY + 1]))
                .setUv2(Math.round(v[LIGHT]), Math.round(v[LIGHT + 1]))
                .setNormal(v[NX], v[NX + 1], v[NX + 2]);
    }

    // Gathers a quad's four vertices; the normal comes last in every vertex, so the fourth normal closes the quad.
    private static final class Clipped implements VertexConsumer {
        private final VertexConsumer inner;
        private final ClippedBuffers cut;
        private final float[][] quad = new float[4][VALUES];
        private int count;

        Clipped(VertexConsumer inner, ClippedBuffers cut) {
            this.inner = inner;
            this.cut = cut;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (this.count == 4) {
                this.count = 0;
            }
            float[] vertex = this.quad[this.count++];
            vertex[X] = x;
            vertex[Y] = y;
            vertex[Z] = z;
            return this;
        }

        private void put(int at, float a, float b) {
            if (this.count > 0) {
                this.quad[this.count - 1][at] = a;
                this.quad[this.count - 1][at + 1] = b;
            }
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.put(RED, red, green);
            this.put(RED + 2, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.put(U, u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.put(OVERLAY, u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.put(LIGHT, u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            if (this.count == 0) {
                return this;
            }
            float[] vertex = this.quad[this.count - 1];
            vertex[NX] = x;
            vertex[NX + 1] = y;
            vertex[NX + 2] = z;
            if (this.count == 4) {
                this.cut.clip(this.inner, this.quad);
                this.count = 0;
            }
            return this;
        }
    }

    private static final class Dropped implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }
}
