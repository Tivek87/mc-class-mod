package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws a part of Green Lantern's uniform only where it already covers him, while the ring dresses him (or undresses
 * him): every side of the part is cut off along the edge the uniform has got to, so the uniform seems to be written
 * onto him by the light as that edge moves. Right along the edge it burns bright, and a seam of light runs along it.
 *
 * <p>How far a part is covered is a {@link Field} over the part's own pixels: 0 on the edge, above 0 where the uniform
 * is. Every side is cut into small pieces first, so an edge that runs in a curve (round the lantern on his chest) is
 * followed closely; each piece is cut along where the field crosses 0.
 */
final class SuitSpread implements VertexConsumer {
    /** How far a point of a part, in the part's own pixels, lies inside the uniform: 0 on its edge, below 0 outside. */
    interface Field {
        float at(float x, float y, float z);
    }

    // Every side is cut into this many pieces each way.
    private static final int CUTS = 4;
    // How close to the edge the uniform burns bright, in pixels.
    private static final float FRESH = 1.6F;
    private static final int FULL_BRIGHT = 0xF000F0;
    // The seam of light along the edge: its bright middle and the glow round it, in blocks.
    private static final float SEAM = 0.03F;
    private static final float SEAM_GLOW = 0.11F;
    private static final int SEAM_COLOR = 0xB8FFC8;
    private static final int SEAM_GLOW_COLOR = 0x3CE86A;

    private final VertexConsumer inner;
    private final Matrix4f toPart = new Matrix4f();
    private Field field = (x, y, z) -> 1.0F;
    // The corners of the side coming in: where, which part of the texture, and in the part's own pixels.
    private final float[] x = new float[4];
    private final float[] y = new float[4];
    private final float[] z = new float[4];
    private final float[] u = new float[4];
    private final float[] v = new float[4];
    private final float[][] local = new float[4][3];
    private int color;
    private int overlay;
    private int light;
    private float nx;
    private float ny;
    private float nz;
    private int count;
    // The seam: pairs of points along the edge, in the frame the corners come in.
    private final List<Vector3f> seam = new ArrayList<>();

    SuitSpread(VertexConsumer inner) {
        this.inner = inner;
    }

    /**
     * Draws one part of the uniform (as {@link ModelPart#render} would), only where {@code field} says it covers him.
     */
    void render(ModelPart part, Field field, PoseStack pose, int light, int overlay) {
        if (!part.visible) {
            return;
        }
        pose.pushPose();
        part.translateAndRotate(pose);
        this.toPart.set(pose.last().pose()).invert();
        pose.popPose();
        this.field = field;
        this.count = 0;
        part.render(pose, this, light, overlay);
    }

    /**
     * The seam of light along the edges drawn since the last time, glowing: a bright thread with a haze round it. The
     * camera must be where the frame the parts were drawn in has its middle, as it is for bodies and for your own
     * hands.
     */
    void seam(MultiBufferSource buffers, float strength) {
        if (this.seam.isEmpty() || strength <= 0.0F) {
            this.seam.clear();
            return;
        }
        VertexConsumer glow = buffers.getBuffer(Ring.HALO);
        for (int i = 0; i + 1 < this.seam.size(); i += 2) {
            Vector3f a = this.seam.get(i);
            Vector3f b = this.seam.get(i + 1);
            strip(glow, a, b, SEAM_GLOW, SEAM_GLOW_COLOR, 0.55F * strength);
            strip(glow, a, b, SEAM, SEAM_COLOR, strength);
        }
        this.seam.clear();
    }

    /** A strip of light from {@code a} to {@code b}, turned towards the camera at the middle, fading to its sides. */
    private static void strip(VertexConsumer buffer, Vector3f a, Vector3f b, float width, int rgb, float alpha) {
        Vector3f along = new Vector3f(b).sub(a);
        Vector3f middle = new Vector3f(a).add(b).mul(0.5F);
        Vector3f side = along.cross(middle, new Vector3f());
        float length = side.length();
        if (length < 1.0E-6F) {
            return;
        }
        side.mul(width * 0.5F / length);
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int bl = rgb & 0xFF;
        int full = (int) (255 * Mth.clamp(alpha, 0.0F, 1.0F));
        for (int k = -1; k <= 1; k += 2) {
            buffer.addVertex(a.x, a.y, a.z).setColor(r, g, bl, full);
            buffer.addVertex(b.x, b.y, b.z).setColor(r, g, bl, full);
            buffer.addVertex(b.x + side.x * k, b.y + side.y * k, b.z + side.z * k).setColor(r, g, bl, 0);
            buffer.addVertex(a.x + side.x * k, a.y + side.y * k, a.z + side.z * k).setColor(r, g, bl, 0);
        }
    }

    @Override
    public void addVertex(float px, float py, float pz, int rgba, float tu, float tv, int packedOverlay,
            int packedLight, float normalX, float normalY, float normalZ) {
        int i = this.count;
        this.x[i] = px;
        this.y[i] = py;
        this.z[i] = pz;
        this.u[i] = tu;
        this.v[i] = tv;
        Vector3f inPart = this.toPart.transformPosition(px, py, pz, new Vector3f()).mul(16.0F);
        this.local[i][0] = inPart.x;
        this.local[i][1] = inPart.y;
        this.local[i][2] = inPart.z;
        if (i == 0) {
            this.color = rgba;
            this.overlay = packedOverlay;
            this.light = packedLight;
            this.nx = normalX;
            this.ny = normalY;
            this.nz = normalZ;
        }
        this.count++;
        if (this.count == 4) {
            this.count = 0;
            this.side();
        }
    }

    /** One whole side has come in: cut it into pieces, and draw what of each piece is covered. */
    private void side() {
        float[] corner = new float[4];
        for (int k = 0; k < 4; k++) {
            corner[k] = this.field.at(this.local[k][0], this.local[k][1], this.local[k][2]);
        }
        // Covered all over, well away from the edge: whole, at once. Every field here is least in a corner of a side,
        // so the corners tell. (A side all outside is still cut up: a round edge may lie inside it.)
        float least = Math.min(Math.min(corner[0], corner[1]), Math.min(corner[2], corner[3]));
        if (least >= FRESH + 1.0F) {
            for (int k = 0; k < 4; k++) {
                this.inner.addVertex(this.x[k], this.y[k], this.z[k], this.color, this.u[k], this.v[k], this.overlay,
                        this.light, this.nx, this.ny, this.nz);
            }
            return;
        }
        for (int i = 0; i < CUTS; i++) {
            for (int j = 0; j < CUTS; j++) {
                this.piece((float) i / CUTS, (float) (i + 1) / CUTS, (float) j / CUTS, (float) (j + 1) / CUTS);
            }
        }
    }

    /** One piece of a side, from a0 to a1 along its first edge and b0 to b1 along its second. */
    private void piece(float a0, float a1, float b0, float b1) {
        float[][] points = { this.at(a0, b0), this.at(a1, b0), this.at(a1, b1), this.at(a0, b1) };
        List<float[]> kept = new ArrayList<>(6);
        float[] cut0 = null;
        float[] cut1 = null;
        for (int k = 0; k < 4; k++) {
            float[] p = points[k];
            float[] q = points[(k + 1) % 4];
            boolean inP = p[5] >= 0.0F;
            boolean inQ = q[5] >= 0.0F;
            if (inP) {
                kept.add(p);
            }
            if (inP != inQ) {
                float t = p[5] / (p[5] - q[5]);
                float[] cut = new float[6];
                for (int c = 0; c < 6; c++) {
                    cut[c] = Mth.lerp(t, p[c], q[c]);
                }
                cut[5] = 0.0F;
                kept.add(cut);
                if (cut0 == null) {
                    cut0 = cut;
                } else {
                    cut1 = cut;
                }
            }
        }
        if (cut0 != null && cut1 != null) {
            this.seam.add(new Vector3f(cut0[0], cut0[1], cut0[2]));
            this.seam.add(new Vector3f(cut1[0], cut1[1], cut1[2]));
        }
        int n = kept.size();
        if (n < 3) {
            return;
        }
        // Drawn in fours: a piece cut to three or five corners is drawn as a fan, a triangle as a four with a doubled
        // corner.
        for (int k = 1; k + 1 < n; k += 2) {
            float[] c = kept.get(k + 1);
            float[] d = k + 2 < n ? kept.get(k + 2) : c;
            this.emit(kept.get(0));
            this.emit(kept.get(k));
            this.emit(c);
            this.emit(d);
        }
    }

    /** A point of the side coming in, a of the way along its first edge and b along its second: x y z u v field. */
    private float[] at(float a, float b) {
        float[] p = new float[6];
        float[][] corners = { this.x, this.y, this.z, this.u, this.v };
        for (int c = 0; c < 5; c++) {
            float[] w = corners[c];
            float near = Mth.lerp(a, w[0], w[1]);
            float far = Mth.lerp(a, w[3], w[2]);
            p[c] = Mth.lerp(b, near, far);
        }
        float lx = Mth.lerp(b, Mth.lerp(a, this.local[0][0], this.local[1][0]),
                Mth.lerp(a, this.local[3][0], this.local[2][0]));
        float ly = Mth.lerp(b, Mth.lerp(a, this.local[0][1], this.local[1][1]),
                Mth.lerp(a, this.local[3][1], this.local[2][1]));
        float lz = Mth.lerp(b, Mth.lerp(a, this.local[0][2], this.local[1][2]),
                Mth.lerp(a, this.local[3][2], this.local[2][2]));
        p[5] = this.field.at(lx, ly, lz);
        return p;
    }

    /** A corner of what is kept of a piece: right along the edge of the uniform it burns bright. */
    private void emit(float[] p) {
        int shine = p[5] < FRESH ? FULL_BRIGHT : this.light;
        this.inner.addVertex(p[0], p[1], p[2], this.color, p[3], p[4], this.overlay, shine, this.nx, this.ny,
                this.nz);
    }

    @Override
    public VertexConsumer addVertex(float px, float py, float pz) {
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer setUv(float tu, float tv) {
        return this;
    }

    @Override
    public VertexConsumer setUv1(int tu, int tv) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int tu, int tv) {
        return this;
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        return this;
    }
}
