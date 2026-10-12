package nl.tivek.multiversepowers.galactus.client;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Blocky bodies for Galactus, built as the game's own creatures are: boxes in model pixels, each face laid with square
// pixels half a model pixel wide, every pixel its own colour from its paint's look as a texture would hold it. Every
// vertex carries its colour, a number of its pixel's own (so cracks pulse each on its own), how far it is a crack of
// energy and how far it glows. One hull per bone, its points kept relative to the bone's pivot.
final class GalactusHull {
    static final int STRIDE = 12;
    static final float TEXEL = 0.5F;

    // How a paint's pixels are laid: bevelled plates (with cracks of energy if `crack`), ribbed suit, riveted trim,
    // plain, glowing, and the emblem's rings on a plate's front.
    enum Look {
        PLATE, SUIT, TRIM, PLAIN, GLOW, EMBLEM
    }

    record Paint(float r, float g, float b, Look look, boolean crack) {
        static Paint of(int rgb, Look look, boolean crack) {
            return new Paint((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, look,
                    crack);
        }
    }

    // A box's six faces in its own frame: the way out, then the ways its columns and rows run (rows top down).
    private static final float[][] FACES = {
        { 0, 0, 1, 1, 0, 0, 0, -1, 0 }, { 0, 0, -1, -1, 0, 0, 0, -1, 0 }, { 1, 0, 0, 0, 0, -1, 0, -1, 0 },
        { -1, 0, 0, 0, 0, 1, 0, -1, 0 }, { 0, 1, 0, 1, 0, 0, 0, 0, 1 }, { 0, -1, 0, 1, 0, 0, 0, 0, -1 } };

    final Vector3f pivot;
    private float[] data = new float[STRIDE * 6 * 512];
    private int floats;
    private int boxes;

    GalactusHull(Vector3f pivot) {
        this.pivot = pivot;
    }

    int vertices() {
        return this.floats / STRIDE;
    }

    float[] data() {
        return this.data;
    }

    void box(Paint paint, double x0, double y0, double z0, double x1, double y1, double z1) {
        this.box(paint, new Vector3f((float) (x0 + x1) * 0.5F, (float) (y0 + y1) * 0.5F, (float) (z0 + z1) * 0.5F),
                new Vector3f((float) Math.abs(x1 - x0) * 0.5F, (float) Math.abs(y1 - y0) * 0.5F,
                        (float) Math.abs(z1 - z0) * 0.5F), null);
    }

    // A box about `center` with half-sizes `half` along its own axes, turned by `turn` (none: square to the bone).
    void box(Paint paint, Vector3f center, Vector3f half, @Nullable Quaternionf turn) {
        int box = this.boxes++;
        float[] size = { half.x, half.y, half.z };
        for (int f = 0; f < 6; f++) {
            float[] face = FACES[f];
            Vector3f out = new Vector3f(face[0], face[1], face[2]);
            Vector3f across = new Vector3f(face[3], face[4], face[5]);
            Vector3f down = new Vector3f(face[6], face[7], face[8]);
            float wide = 2.0F * reach(across, size);
            float high = 2.0F * reach(down, size);
            Vector3f corner = new Vector3f(out).mul(reach(out, size)).fma(-wide * 0.5F, across)
                    .fma(-high * 0.5F, down);
            int cols = Math.max(1, Math.round(wide / TEXEL));
            int rows = Math.max(1, Math.round(high / TEXEL));
            boolean[] cracks = paint.crack() ? GalactusPixels.cracks(cols, rows, box * 6 + f) : null;
            Vector3f normal = turn == null ? out : turn.transform(new Vector3f(out));
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < cols; i++) {
                    float[] pixel = GalactusPixels.pixel(paint, f, i, j, cols, rows, box,
                            cracks != null && cracks[j * cols + i]);
                    Vector3f a = place(corner, across, wide * i / cols, down, high * j / rows, center, turn);
                    Vector3f b = place(corner, across, wide * (i + 1) / cols, down, high * j / rows, center, turn);
                    Vector3f c = place(corner, across, wide * (i + 1) / cols, down, high * (j + 1) / rows, center,
                            turn);
                    Vector3f d = place(corner, across, wide * i / cols, down, high * (j + 1) / rows, center, turn);
                    this.vertex(a, normal, pixel);
                    this.vertex(b, normal, pixel);
                    this.vertex(c, normal, pixel);
                    this.vertex(a, normal, pixel);
                    this.vertex(c, normal, pixel);
                    this.vertex(d, normal, pixel);
                }
            }
        }
    }

    void moveBy(Vector3f by) {
        for (int i = 0; i < this.floats; i += STRIDE) {
            this.data[i] += by.x;
            this.data[i + 1] += by.y;
            this.data[i + 2] += by.z;
        }
    }

    private static float reach(Vector3f way, float[] size) {
        return Math.abs(way.x) * size[0] + Math.abs(way.y) * size[1] + Math.abs(way.z) * size[2];
    }

    private static Vector3f place(Vector3f corner, Vector3f across, float u, Vector3f down, float v, Vector3f center,
            @Nullable Quaternionf turn) {
        Vector3f p = new Vector3f(corner).fma(u, across).fma(v, down);
        if (turn != null) {
            turn.transform(p);
        }
        return p.add(center);
    }

    // A pixel: red, green, blue, its own number, crack, glow.
    private void vertex(Vector3f at, Vector3f normal, float[] pixel) {
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
        d[i + 6] = pixel[0];
        d[i + 7] = pixel[1];
        d[i + 8] = pixel[2];
        d[i + 9] = pixel[3];
        d[i + 10] = pixel[4];
        d[i + 11] = pixel[5];
        this.floats += STRIDE;
    }
}
