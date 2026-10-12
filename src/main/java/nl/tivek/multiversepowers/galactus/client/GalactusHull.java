package nl.tivek.multiversepowers.galactus.client;

import java.util.Arrays;
import org.joml.Vector3f;

// Smooth bodies for Galactus, in model pixels: each a sweep of rounded cross-sections (super-ellipses, from round to
// nearly square) along a path, its normals worked out along the surface so it shades smoothly, with flat caps where an
// end is open. Every vertex carries its paint, how metallic it is, how far energy cracks run over it and how much it
// glows. One hull per bone, its points kept relative to the bone's pivot.
final class GalactusHull {
    static final int STRIDE = 12;

    // An albedo with how metallic it is, how much the energy cracks show on it and how far it glows (in its own colour).
    record Paint(float r, float g, float b, float metal, float crack, float glow) {
        static Paint of(int rgb, float metal, float crack, float glow) {
            return new Paint((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, metal,
                    crack, glow);
        }
    }

    // A cross-section: its middle, the two axes it spans (half-sizes a and b along them), how square it is (2 round,
    // higher squarer) and how lit it is (darker in crevices).
    record Ring(Vector3f c, Vector3f u, Vector3f v, float a, float b, float n, float shade) {
    }

    final Vector3f pivot;
    private float[] data = new float[STRIDE * 3 * 256];
    private int floats;

    GalactusHull(Vector3f pivot) {
        this.pivot = pivot;
    }

    int vertices() {
        return this.floats / STRIDE;
    }

    float[] data() {
        return this.data;
    }

    // A sweep along rings; an end with a ring of any size gets a flat cap.
    void sweep(Paint paint, int around, Ring... rings) {
        this.sweep(paint, around, false, rings);
    }

    // With `loop` the last ring joins the first again, as round a torus, and nothing is capped.
    private void sweep(Paint paint, int around, boolean loop, Ring... rings) {
        int count = rings.length;
        Vector3f[][] p = new Vector3f[count][around];
        for (int i = 0; i < count; i++) {
            Ring ring = rings[i];
            for (int j = 0; j < around; j++) {
                double angle = Math.PI * 2.0 * j / around;
                float x = superCurve(Math.cos(angle), ring.n()) * ring.a();
                float y = superCurve(Math.sin(angle), ring.n()) * ring.b();
                p[i][j] = new Vector3f(ring.c()).fma(x, ring.u()).fma(y, ring.v());
            }
        }
        Vector3f[][] n = new Vector3f[count][around];
        for (int i = 0; i < count; i++) {
            int next = loop ? (i + 1) % count : Math.min(count - 1, i + 1);
            int last = loop ? (i - 1 + count) % count : Math.max(0, i - 1);
            Vector3f along = new Vector3f(rings[next].c()).sub(rings[last].c());
            for (int j = 0; j < around; j++) {
                Vector3f du = new Vector3f(p[i][(j + 1) % around]).sub(p[i][(j - 1 + around) % around]);
                Vector3f dv = new Vector3f(p[next][j]).sub(p[last][j]);
                Vector3f normal = new Vector3f(dv).cross(du);
                Vector3f out = new Vector3f(p[i][j]).sub(rings[i].c());
                if (du.lengthSquared() < 1.0E-10F || normal.lengthSquared() < 1.0E-12F) {
                    // A ring shrunk to a point: its normal points on out of the end.
                    normal.set(along).mul(i == 0 ? -1.0F : 1.0F);
                    if (normal.lengthSquared() < 1.0E-12F) {
                        normal.set(0.0F, 1.0F, 0.0F);
                    }
                } else if (normal.dot(out) < 0.0F) {
                    normal.negate();
                }
                n[i][j] = normal.normalize();
            }
        }
        for (int i = 0; i < (loop ? count : count - 1); i++) {
            int o = (i + 1) % count;
            for (int j = 0; j < around; j++) {
                int k = (j + 1) % around;
                this.triangle(paint, p[i][j], n[i][j], rings[i].shade(), p[i][k], n[i][k], rings[i].shade(),
                        p[o][k], n[o][k], rings[o].shade());
                this.triangle(paint, p[i][j], n[i][j], rings[i].shade(), p[o][k], n[o][k], rings[o].shade(),
                        p[o][j], n[o][j], rings[o].shade());
            }
        }
        for (int end = 0; end < 2 && count > 1 && !loop; end++) {
            int i = end == 0 ? 0 : count - 1;
            Ring ring = rings[i];
            if (ring.a() <= 1.0E-4F && ring.b() <= 1.0E-4F) {
                continue;
            }
            Vector3f normal = new Vector3f(rings[end == 0 ? 0 : count - 1].c())
                    .sub(rings[end == 0 ? 1 : count - 2].c()).normalize();
            for (int j = 0; j < around; j++) {
                int k = (j + 1) % around;
                this.triangle(paint, ring.c(), normal, ring.shade(), p[i][j], normal, ring.shade(), p[i][k], normal,
                        ring.shade());
            }
        }
    }

    // Rings along a path, parallel-carried so the path never twists: `a` lies along `ref` (squared off the path).
    static Ring[] path(Vector3f ref, float n, Vector3f[] at, float[] a, float[] b, float[] shade) {
        Ring[] rings = new Ring[at.length];
        Vector3f across = new Vector3f(ref);
        for (int i = 0; i < at.length; i++) {
            Vector3f along = new Vector3f(at[Math.min(at.length - 1, i + 1)]).sub(at[Math.max(0, i - 1)]);
            if (along.lengthSquared() < 1.0E-10F) {
                along.set(0.0F, 1.0F, 0.0F);
            }
            along.normalize();
            Vector3f square = new Vector3f(across).sub(new Vector3f(along).mul(across.dot(along)));
            if (square.lengthSquared() < 1.0E-8F) {
                square = Math.abs(along.y) < 0.9F ? new Vector3f(along).cross(0.0F, 1.0F, 0.0F)
                        : new Vector3f(along).cross(1.0F, 0.0F, 0.0F);
            }
            across = square.normalize();
            Vector3f side = new Vector3f(along).cross(across).normalize();
            rings[i] = new Ring(new Vector3f(at[i]), new Vector3f(across), side, a[i], b[i], n,
                    shade == null ? 1.0F : shade[i]);
        }
        return rings;
    }

    // A straight limb from `from` to `to`, its rings given as rows of (share along, a, b, shift along ref, shift
    // square to it, shade); `a` lies along `ref`.
    void limb(Paint paint, int around, float n, Vector3f from, Vector3f to, Vector3f ref, float... rows) {
        int count = rows.length / 6;
        Vector3f along = new Vector3f(to).sub(from);
        Ring[] base = path(ref, n, new Vector3f[] { from, to }, new float[] { 1, 1 }, new float[] { 1, 1 }, null);
        Vector3f u = base[0].u();
        Vector3f v = base[0].v();
        Ring[] rings = new Ring[count];
        for (int i = 0; i < count; i++) {
            float share = rows[6 * i];
            Vector3f c = new Vector3f(from).fma(share, along).fma(rows[6 * i + 3], u).fma(rows[6 * i + 4], v);
            rings[i] = new Ring(c, u, v, rows[6 * i + 1], rows[6 * i + 2], n, rows[6 * i + 5]);
        }
        this.sweep(paint, around, rings);
    }

    // A rounded capsule from `from` to `to`, radius r0 at the start and r1 at the end, `squash` flattening it along
    // `ref`.
    void capsule(Paint paint, int around, Vector3f from, Vector3f to, Vector3f ref, float r0, float r1, float squash,
            float shade) {
        float length = new Vector3f(to).sub(from).length();
        int caps = 4;
        float[] rows = new float[6 * (caps * 2 + 2)];
        int row = 0;
        for (int i = 0; i <= caps; i++) {
            double phi = Math.PI * 0.5 * i / caps;
            float r = (float) Math.sin(phi) * r0;
            float back = (float) -Math.cos(phi) * r0 / length;
            row = put(rows, row, back, r * squash, r, shade * (0.8F + 0.2F * i / caps));
        }
        for (int i = caps; i >= 0; i--) {
            double phi = Math.PI * 0.5 * i / caps;
            float r = (float) Math.sin(phi) * r1;
            float on = 1.0F + (float) Math.cos(phi) * r1 / length;
            row = put(rows, row, on, r * squash, r, shade);
        }
        this.limb(paint, around, 2.0F, from, to, ref, Arrays.copyOf(rows, row));
    }

    // An ellipsoid about `c` with half-sizes along x, y and z; `n` squares it off.
    void ball(Paint paint, int around, int rings, Vector3f c, float rx, float ry, float rz, float n, float shade) {
        Ring[] out = new Ring[rings + 1];
        for (int i = 0; i <= rings; i++) {
            double polar = Math.PI * i / rings;
            float s = superCurve(Math.sin(polar), n);
            float y = superCurve(-Math.cos(polar), n) * ry;
            out[i] = new Ring(new Vector3f(c.x, c.y + y, c.z), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1), rx * s,
                    rz * s, n, shade * (0.85F + 0.15F * (float) i / rings));
        }
        this.sweep(paint, around, out);
    }

    // A turned body round `axis` through `c`: rows of (radius, height along the axis); `ref` sets where its seam lies.
    void lathe(Paint paint, int around, Vector3f c, Vector3f axis, Vector3f ref, float shade, float... rows) {
        Vector3f w = new Vector3f(axis).normalize();
        Vector3f u = new Vector3f(ref).sub(new Vector3f(w).mul(ref.dot(w))).normalize();
        Vector3f v = new Vector3f(w).cross(u).normalize();
        Ring[] rings = new Ring[rows.length / 2];
        for (int i = 0; i < rings.length; i++) {
            rings[i] = new Ring(new Vector3f(c).fma(rows[2 * i + 1], w), u, v, rows[2 * i], rows[2 * i], 2.0F, shade);
        }
        this.sweep(paint, around, rings);
    }

    // A ring of round section (a torus) round `axis` through `c`.
    void torus(Paint paint, int around, int round, Vector3f c, Vector3f axis, float major, float minor, float shade) {
        Vector3f w = new Vector3f(axis).normalize();
        Vector3f u = Math.abs(w.y) < 0.9F ? new Vector3f(w).cross(0, 1, 0).normalize()
                : new Vector3f(w).cross(1, 0, 0).normalize();
        Vector3f v = new Vector3f(w).cross(u).normalize();
        Ring[] rings = new Ring[around];
        for (int i = 0; i < around; i++) {
            double angle = Math.PI * 2.0 * i / around;
            Vector3f out = new Vector3f(u).mul((float) Math.cos(angle)).fma((float) Math.sin(angle), v);
            rings[i] = new Ring(new Vector3f(c).fma(major, out), out, w, minor, minor, 2.0F, shade);
        }
        this.sweep(paint, round, true, rings);
    }

    void moveBy(Vector3f by) {
        for (int i = 0; i < this.floats; i += STRIDE) {
            this.data[i] += by.x;
            this.data[i + 1] += by.y;
            this.data[i + 2] += by.z;
        }
    }

    private static int put(float[] rows, int row, float share, float a, float b, float shade) {
        rows[row] = share;
        rows[row + 1] = a;
        rows[row + 2] = b;
        rows[row + 3] = 0.0F;
        rows[row + 4] = 0.0F;
        rows[row + 5] = shade;
        return row + 6;
    }

    private void triangle(Paint paint, Vector3f a, Vector3f na, float sa, Vector3f b, Vector3f nb, float sb,
            Vector3f c, Vector3f nc, float sc) {
        this.vertex(paint, a, na, sa);
        this.vertex(paint, b, nb, sb);
        this.vertex(paint, c, nc, sc);
    }

    private void vertex(Paint paint, Vector3f at, Vector3f normal, float shade) {
        if (this.floats + STRIDE > this.data.length) {
            this.data = Arrays.copyOf(this.data, this.data.length * 2);
        }
        float[] d = this.data;
        int i = this.floats;
        d[i] = at.x;
        d[i + 1] = at.y;
        d[i + 2] = at.z;
        d[i + 3] = normal.x;
        d[i + 4] = normal.y;
        d[i + 5] = normal.z;
        float lit = paint.glow() > 0.0F ? 1.0F : shade;
        d[i + 6] = paint.r() * lit;
        d[i + 7] = paint.g() * lit;
        d[i + 8] = paint.b() * lit;
        d[i + 9] = paint.metal();
        d[i + 10] = paint.crack();
        d[i + 11] = paint.glow();
        this.floats += STRIDE;
    }

    private static float superCurve(double value, float n) {
        return (float) (Math.signum(value) * Math.pow(Math.abs(value), 2.0 / n));
    }
}
