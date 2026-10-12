package nl.tivek.multiversepowers.galactus.client;

import nl.tivek.multiversepowers.galactus.client.GalactusHull.Paint;
import nl.tivek.multiversepowers.galactus.client.GalactusHull.Ring;
import org.joml.Vector3f;

// Galactus's body, a hull per bone of `GalactusRig`, modelled in model pixels as he stands at rest. His helmet: a
// rounded shell round a deep-set face with glowing eyes under a heavy V brow, cheek guards, the narrow crown with its
// ridge and prongs, discs at the ears and the two great plates running out from them and up like horns. Domed
// pauldrons over a broad chest with a breastplate and the round emblem, a gorget, banded abdomen and belt with a
// glowing buckle and hanging plates, armoured legs and boots; flared ribbed gauntlets and armoured hands with every
// finger in three bones. Dark purple metal and a deep blue under-suit, the cracks of energy laid on by the shader.
final class GalactusShape {
    static final Paint ARMOR = Paint.of(0x4A2C66, 0.8F, 1.0F, 0.0F);
    static final Paint ARMOR_DARK = Paint.of(0x2F1B40, 0.7F, 0.7F, 0.0F);
    static final Paint SUIT = Paint.of(0x2B3B66, 0.35F, 0.3F, 0.0F);
    static final Paint TRIM = Paint.of(0x3C3B4C, 0.9F, 0.2F, 0.0F);
    static final Paint SILVER = Paint.of(0x9497AE, 0.95F, 0.0F, 0.0F);
    static final Paint SKIN = Paint.of(0x7E7088, 0.1F, 0.0F, 0.0F);
    static final Paint DARK = Paint.of(0x0D0A13, 0.2F, 0.0F, 0.0F);
    static final Paint EYE = Paint.of(0xD8B8FF, 0.0F, 0.0F, 1.0F);
    static final Paint ENERGY = Paint.of(0xA45CFF, 0.0F, 0.0F, 1.0F);
    static final Paint CORE = Paint.of(0xE2C8FF, 0.0F, 0.0F, 1.0F);
    private static final Vector3f X = new Vector3f(1.0F, 0.0F, 0.0F);
    private static final Vector3f Y = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f Z = new Vector3f(0.0F, 0.0F, 1.0F);

    final GalactusHull[] hulls = new GalactusHull[GalactusRig.COUNT];

    GalactusShape(GalactusRig rig) {
        for (int b = 0; b < GalactusRig.COUNT; b++) {
            this.hulls[b] = new GalactusHull(rig.pivot[b]);
        }
        this.pelvis();
        this.abdomen();
        this.chest();
        this.head();
        this.helmet();
        for (int side = 0; side < 2; side++) {
            this.leg(side);
            this.arm(side);
            this.hand(side, rig);
        }
        for (GalactusHull hull : this.hulls) {
            hull.moveBy(new Vector3f(hull.pivot).negate());
        }
    }

    private GalactusHull h(int bone) {
        return this.hulls[bone];
    }

    private static Vector3f v(double x, double y, double z) {
        return new Vector3f((float) x, (float) y, (float) z);
    }

    // A sweep up or down y: rows of (y, half-size along x, along z, middle x, middle z, shade).
    private static void ys(GalactusHull hull, Paint paint, int around, float n, double... rows) {
        Ring[] rings = new Ring[rows.length / 6];
        for (int i = 0; i < rings.length; i++) {
            int r = 6 * i;
            rings[i] = new Ring(v(rows[r + 3], rows[r], rows[r + 4]), X, Z, (float) rows[r + 1], (float) rows[r + 2],
                    n, (float) rows[r + 5]);
        }
        hull.sweep(paint, around, rings);
    }

    // A sweep along z: rows of (z, half-size along x, along y, middle x, middle y, shade).
    private static void zs(GalactusHull hull, Paint paint, int around, float n, double... rows) {
        Ring[] rings = new Ring[rows.length / 6];
        for (int i = 0; i < rings.length; i++) {
            int r = 6 * i;
            rings[i] = new Ring(v(rows[r + 3], rows[r + 4], rows[r]), X, Y, (float) rows[r + 1], (float) rows[r + 2],
                    n, (float) rows[r + 5]);
        }
        hull.sweep(paint, around, rings);
    }

    // A band round y from y0 to y1, its edges rounded.
    private static void band(GalactusHull hull, Paint paint, double y0, double y1, double a, double b, double x,
            double z, float n) {
        double e = Math.min(0.18, (y1 - y0) * 0.3);
        ys(hull, paint, 32, n, y0, a - e, b - e, x, z, 0.75, y0 + e, a, b, x, z, 1.0, y1 - e, a, b, x, z, 1.0, y1,
                a - e, b - e, x, z, 0.85);
    }

    private static void along(GalactusHull hull, Paint paint, int around, float n, Vector3f ref, double[][] points,
            double[] a, double[] b) {
        Vector3f[] at = new Vector3f[points.length];
        float[] fa = new float[points.length];
        float[] fb = new float[points.length];
        for (int i = 0; i < points.length; i++) {
            at[i] = v(points[i][0], points[i][1], points[i][2]);
            fa[i] = (float) a[Math.min(i, a.length - 1)];
            fb[i] = (float) b[Math.min(i, b.length - 1)];
        }
        hull.sweep(paint, around, GalactusHull.path(ref, n, at, fa, fb, null));
    }

    private void pelvis() {
        GalactusHull hull = h(GalactusRig.PELVIS);
        ys(hull, SUIT, 28, 2.6F, 25.6, 0, 0, 0, 0.3, 0.5, 26.0, 3.2, 2.6, 0, 0.3, 0.55, 27.0, 5.6, 3.8, 0, 0.2, 0.7,
                28.5, 6.9, 4.5, 0, 0.1, 0.85, 30.5, 7.4, 4.9, 0, 0, 1, 33.0, 7.2, 4.7, 0, 0, 1, 35.6, 6.9, 4.5, 0, 0.1,
                0.9);
        ys(hull, TRIM, 40, 4.0F, 32.6, 7.75, 5.1, 0, 0, 0.8, 33.0, 8.05, 5.4, 0, 0, 1, 35.3, 8.05, 5.4, 0, 0, 1, 35.7,
                7.8, 5.15, 0, 0, 0.85);
        for (int s = -1; s <= 1; s += 2) {
            hull.ball(ARMOR, 12, 6, v(s * 3.9, 34.15, 5.25), 0.75F, 0.75F, 0.35F, 5.0F, 1.0F);
            hull.ball(ARMOR, 12, 6, v(s * 6.7, 34.15, 3.75), 0.7F, 0.75F, 0.35F, 5.0F, 0.9F);
            along(hull, ARMOR, 20, 6.0F, Z, new double[][] { { s * 7.95, 33.6, 0.6 }, { s * 8.6, 30.5, 0.9 },
                    { s * 8.85, 27.5, 1.2 }, { s * 8.8, 26.2, 1.3 } }, new double[] { 3.0, 3.1, 2.7, 1.6 },
                    new double[] { 0.45 });
        }
        hull.lathe(ARMOR, 28, v(0, 34.15, 5.2), Z, X, 1.0F, 0, -0.3F, 2.0F, -0.3F, 2.15F, 0.2F, 1.7F, 0.6F, 0, 0.6F);
        hull.lathe(ENERGY, 20, v(0, 34.15, 5.2), Z, X, 1.0F, 0, 0.5F, 0.85F, 0.5F, 0.7F, 0.85F, 0, 0.9F);
        along(hull, ARMOR, 20, 6.0F, X, new double[][] { { 0, 33.4, 5.75 }, { 0, 30.5, 5.95 }, { 0, 27, 5.6 },
                { 0, 23.6, 4.9 }, { 0, 21.4, 4.45 } }, new double[] { 3.3, 3.2, 2.9, 2.2, 0.5 },
                new double[] { 0.45 });
        along(hull, ARMOR_DARK, 20, 6.0F, X, new double[][] { { 0, 33.4, -5.45 }, { 0, 30, -5.65 },
                { 0, 26.5, -5.2 }, { 0, 25.2, -5.0 } }, new double[] { 3.2, 3.0, 2.2, 0.8 }, new double[] { 0.45 });
    }

    private void leg(int side) {
        double s = GalactusRig.sign(side);
        GalactusHull thigh = h(GalactusRig.THIGH + side);
        ys(thigh, SUIT, 24, 2.3F, 31.5, 3.7, 3.8, s * 4.4, 0.2, 0.7, 29.0, 4.0, 4.1, s * 4.5, 0.25, 0.95, 25.0, 3.7,
                3.8, s * 4.6, 0.35, 1, 21.0, 3.2, 3.3, s * 4.7, 0.45, 0.95, 18.0, 2.8, 2.9, s * 4.7, 0.5, 0.8, 16.8,
                2.4, 2.5, s * 4.7, 0.5, 0.7);
        ys(thigh, ARMOR, 20, 4.0F, 28.5, 2.4, 1.0, s * 4.6, 3.3, 0.85, 27.8, 2.6, 1.2, s * 4.62, 3.25, 1, 25, 2.7,
                1.25, s * 4.7, 3.15, 1, 21.5, 2.3, 1.1, s * 4.75, 2.95, 0.95, 19.6, 1.6, 0.8, s * 4.75, 2.8, 0.85);
        GalactusHull shin = h(GalactusRig.SHIN + side);
        shin.ball(ARMOR, 16, 10, v(s * 4.7, 17.1, 2.6), 2.1F, 2.2F, 1.5F, 2.6F, 1.0F);
        ys(shin, ARMOR, 28, 2.6F, 18.2, 3.5, 3.6, s * 4.75, 0.45, 0.8, 17.0, 3.9, 4.0, s * 4.75, 0.45, 1, 15.6, 3.6,
                3.7, s * 4.8, 0.45, 0.9, 15.1, 3.2, 3.3, s * 4.8, 0.45, 0.7, 12.0, 3.15, 3.3, s * 4.85, 0.4, 1, 8.0,
                2.75, 3.0, s * 4.9, 0.35, 1, 4.6, 2.4, 2.7, s * 4.95, 0.4, 0.95, 3.0, 2.5, 2.8, s * 4.95, 0.5, 0.9,
                2.2, 2.3, 2.6, s * 4.95, 0.5, 0.8);
        band(shin, TRIM, 11.2, 11.9, 3.32, 3.47, s * 4.86, 0.4, 2.6F);
        band(shin, TRIM, 7.2, 7.9, 2.93, 3.17, s * 4.9, 0.36, 2.6F);
        zs(shin, ARMOR, 20, 3.0F, -2.9, 1.6, 1.2, s * 4.95, 1.6, 0.7, -2.4, 2.3, 1.7, s * 4.95, 1.8, 0.85, 0.0, 2.6,
                1.9, s * 4.95, 1.9, 1, 3.2, 2.5, 1.5, s * 4.95, 1.5, 1, 5.8, 2.1, 1.05, s * 4.95, 1.05, 0.95, 7.3, 1.3,
                0.7, s * 4.95, 0.75, 0.85, 7.7, 0, 0, s * 4.95, 0.7, 0.8);
    }

    private void abdomen() {
        GalactusHull hull = h(GalactusRig.ABDOMEN);
        ys(hull, SUIT, 28, 2.5F, 33.5, 6.8, 4.5, 0, 0.1, 0.8, 35.5, 7.0, 4.7, 0, 0.25, 1, 38.0, 7.5, 4.95, 0, 0.35, 1,
                40.5, 8.1, 5.15, 0, 0.4, 0.95, 41.8, 8.2, 5.0, 0, 0.4, 0.85);
        for (int i = 0; i < 3; i++) {
            double y0 = 35.4 + i * 1.9;
            double y1 = y0 + 1.45;
            double a = 4.6 + 0.55 * i;
            double z = 4.0 + 0.15 * i;
            ys(hull, ARMOR, 24, 4.0F, y0, a - 0.3, 1.05, 0, z, 0.7, y0 + 0.3, a, 1.3, 0, z, 1, y1 - 0.3, a, 1.3, 0, z,
                    1, y1, a - 0.3, 1.05, 0, z, 0.8);
        }
    }

    private void chest() {
        GalactusHull hull = h(GalactusRig.CHEST);
        ys(hull, SUIT, 32, 2.6F, 39.6, 8.0, 5.0, 0, 0.4, 0.85, 42.5, 9.2, 5.6, 0, 0.5, 1, 45.5, 9.7, 5.8, 0, 0.4, 1,
                48.0, 9.3, 5.3, 0, 0.1, 1, 50.0, 7.8, 4.4, 0, -0.2, 0.9, 51.3, 5.0, 3.6, 0, -0.3, 0.8, 51.9, 3.0, 2.8,
                0, -0.3, 0.7);
        ys(hull, ARMOR, 36, 3.0F, 40.4, 7.6, 3.0, 0, 3.0, 0.75, 41.0, 8.4, 3.4, 0, 3.0, 0.95, 43.0, 9.2, 3.6, 0, 3.15,
                1, 46.0, 9.5, 3.5, 0, 2.95, 1, 48.6, 8.6, 3.0, 0, 2.6, 1, 49.8, 6.6, 2.4, 0, 2.4, 0.9, 50.3, 4.5, 1.8,
                0, 2.4, 0.8);
        // The emblem: a silver ring round a dark disc, a glowing ring and its core.
        Vector3f emblem = v(0, 44.4, 6.85);
        hull.lathe(DARK, 28, new Vector3f(emblem).add(0, 0, -0.5F), Z, X, 1.0F, 0, 0, 2.4F, 0, 2.4F, 0.55F, 0,
                0.55F);
        hull.torus(SILVER, 36, 10, emblem, Z, 2.6F, 0.45F, 1.0F);
        hull.torus(ENERGY, 28, 8, new Vector3f(emblem).add(0, 0, 0.05F), Z, 1.4F, 0.3F, 1.0F);
        hull.ball(CORE, 12, 8, new Vector3f(emblem).add(0, 0, 0.05F), 0.45F, 0.45F, 0.25F, 2.0F, 1.0F);
        hull.lathe(ARMOR, 36, v(0, 49.0, -0.2), Y, X, 1.0F, 5.6F, 0, 5.9F, 1.0F, 5.5F, 2.3F, 4.4F, 3.4F, 3.6F, 3.5F,
                3.4F, 1.0F);
        for (int s = -1; s <= 1; s += 2) {
            hull.capsule(SUIT, 16, v(s * 3.0, 51.0, -0.4), v(s * 9.8, 49.0, -0.2), Y, 2.6F, 3.0F, 0.85F, 0.9F);
        }
    }

    private void head() {
        GalactusHull hull = h(GalactusRig.HEAD);
        ys(hull, SKIN, 20, 2.2F, 49.0, 2.6, 2.5, 0, -0.3, 0.45, 51.5, 2.6, 2.5, 0, -0.2, 0.55, 53.5, 2.4, 2.3, 0, 0,
                0.7);
        ys(hull, SKIN, 28, 2.3F, 51.5, 0, 0, 0, 1.9, 0.55, 51.7, 1.5, 1.3, 0, 1.8, 0.6, 52.3, 2.4, 2.4, 0, 1.3, 0.75,
                53.5, 3.0, 3.1, 0, 0.9, 0.9, 55.0, 3.25, 3.4, 0, 0.7, 1, 56.6, 3.3, 3.5, 0, 0.6, 1, 58.4, 3.2, 3.4, 0,
                0.4, 1, 60.0, 2.6, 2.9, 0, 0.2, 1, 60.8, 0, 0, 0, 0.1, 1);
        ys(hull, SKIN, 20, 3.2F, 51.6, 1.6, 1.0, 0, 2.6, 0.6, 52.2, 2.2, 1.4, 0, 2.6, 0.8, 53.4, 2.5, 1.5, 0, 2.5, 0.9,
                54.2, 2.3, 1.2, 0, 2.4, 0.9);
        hull.limb(SKIN, 12, 2.3F, v(0, 56.7, 3.95), v(0, 54.4, 4.45), X, 0, 0.25F, 0.3F, 0, 0, 1, 0.75F, 0.4F, 0.45F,
                0, 0, 1, 1, 0.3F, 0.3F, 0, 0, 0.9F);
        hull.limb(DARK, 8, 2.0F, v(-0.9, 53.3, 3.98), v(0.9, 53.3, 3.98), Y, 0, 0.07F, 0.1F, 0, 0, 1, 0.5F, 0.1F,
                0.15F, 0, 0, 1, 1, 0.07F, 0.1F, 0, 0, 1);
        for (int s = -1; s <= 1; s += 2) {
            hull.ball(DARK, 16, 8, v(s * 1.3, 56.1, 3.5), 1.05F, 0.62F, 0.5F, 2.0F, 1.0F);
            hull.ball(EYE, 14, 8, v(s * 1.3, 56.1, 3.82), 0.72F, 0.3F, 0.32F, 2.0F, 1.0F);
        }
    }

    private void helmet() {
        GalactusHull hull = h(GalactusRig.HEAD);
        ys(hull, ARMOR, 40, 3.0F, 51.0, 3.6, 3.7, 0, -1.6, 0.6, 52.5, 4.1, 4.2, 0, -1.4, 0.85, 55.0, 4.35, 4.5, 0,
                -1.25, 1, 58.0, 4.4, 4.6, 0, -1.1, 1, 60.5, 4.2, 4.5, 0, -1.0, 1, 61.8, 3.9, 4.2, 0, -1.0, 1, 62.4, 3.2,
                3.5, 0, -1.0, 0.9);
        for (int s = -1; s <= 1; s += 2) {
            ys(hull, ARMOR, 16, 4.0F, 51.4, 0.4, 0.9, s * 3.3, 2.4, 0.7, 52.0, 0.5, 1.4, s * 3.45, 2.3, 0.9, 54.0, 0.55,
                    1.6, s * 3.6, 2.1, 1, 56.5, 0.55, 1.6, s * 3.75, 1.9, 1, 57.8, 0.5, 1.5, s * 3.8, 1.8, 0.9);
        }
        along(hull, ARMOR, 16, 4.0F, Y, new double[][] { { -3.9, 57.6, 2.0 }, { -2.2, 57.85, 3.35 },
                { 0, 57.45, 3.95 }, { 2.2, 57.85, 3.35 }, { 3.9, 57.6, 2.0 } },
                new double[] { 0.6, 0.85, 0.75, 0.85, 0.6 }, new double[] { 0.55 });
        along(hull, ARMOR, 16, 4.0F, X, new double[][] { { 0, 57.9, 4.0 }, { 0, 59.5, 3.85 }, { 0, 61.5, 3.45 },
                { 0, 63.5, 2.6 }, { 0, 65.8, 2.1 }, { 0, 66.7, 1.9 } },
                new double[] { 0.55, 0.6, 0.6, 0.55, 0.5, 0.35 }, new double[] { 0.5 });
        ys(hull, ARMOR, 32, 5.0F, 61.0, 3.9, 3.7, 0, -0.9, 0.85, 62.5, 3.75, 3.55, 0, -0.9, 1, 64.5, 3.35, 3.15, 0,
                -1.0, 1, 66.0, 3.05, 2.85, 0, -1.0, 1, 66.4, 2.9, 2.7, 0, -1.0, 0.9);
        band(hull, TRIM, 61.0, 61.6, 4.05, 3.85, 0, -0.9, 5.0F);
        double[] prongs = { -2.25, -0.75, 0.75, 2.25 };
        for (double x : prongs) {
            float high = Math.abs(x) > 1.0 ? 2.1F : 1.6F;
            hull.limb(ARMOR, 12, 4.0F, v(x, 66.1, -1.0), v(x, 66.1 + high, -1.0), X, 0, 0.42F, 2.3F, 0, 0, 0.95F,
                    0.6F, 0.36F, 1.9F, 0, 0, 1, 1, 0.05F, 0.5F, 0, 0, 1);
        }
        for (int s = -1; s <= 1; s += 2) {
            hull.lathe(TRIM, 24, v(s * 4.15, 55.2, -0.6), v(s, 0, 0), Y, 1.0F, 0, 0, 1.9F, 0, 2.2F, 0.35F, 2.1F, 0.8F,
                    1.2F, 1.0F, 0, 1.05F);
            hull.ball(SILVER, 10, 6, v(s * 5.2, 55.2, -0.6), 0.25F, 0.6F, 0.6F, 2.0F, 1.0F);
            // The great side plates: out from the ear, then up and out like horns.
            double[][] horn = { { s * 4.8, 55.3, -0.6 }, { s * 6.8, 55.15, -0.6 }, { s * 8.5, 55.35, -0.6 },
                    { s * 9.9, 56.0, -0.6 }, { s * 10.9, 57.2, -0.6 }, { s * 11.5, 58.9, -0.6 },
                    { s * 11.9, 61.0, -0.6 }, { s * 12.4, 64.0, -0.6 }, { s * 12.9, 67.0, -0.6 },
                    { s * 13.25, 68.9, -0.6 }, { s * 13.45, 69.6, -0.6 } };
            along(hull, ARMOR, 28, 12.0F, Z, horn,
                    new double[] { 0.5, 0.52, 0.55, 0.55, 0.55, 0.55, 0.55, 0.55, 0.52, 0.5, 0.45 },
                    new double[] { 1.3, 1.55, 1.7, 1.8, 1.85, 1.9, 1.95, 2.1, 2.3, 2.4, 1.2 });
            // A raised rib up the middle of each plate's face.
            double[][] rib = new double[horn.length - 2][];
            for (int i = 1; i < horn.length - 1; i++) {
                rib[i - 1] = new double[] { horn[i][0], horn[i][1], -0.05 };
            }
            along(hull, TRIM, 12, 4.0F, Z, rib, new double[] { 0.12 }, new double[] { 0.35 });
        }
    }

    private void arm(int side) {
        double s = GalactusRig.sign(side);
        GalactusHull pauldron = h(GalactusRig.PAULDRON + side);
        Vector3f w = v(s * 0.5, 0.86, 0).normalize();
        Vector3f u = v(0.86, -s * 0.5, 0).normalize();
        Vector3f c = v(s * 11.4, 48.6, 0.1);
        double[] dome = { -2.2, 4.8, 5.2, 0.65, -1.7, 5.2, 5.6, 0.9, -1.2, 5.1, 5.5, 1, 0.8, 4.9, 5.3, 1, 2.6, 4.1, 4.4,
                1, 4.0, 2.9, 3.1, 1, 4.9, 1.4, 1.5, 1, 5.2, 0, 0, 1 };
        pauldron.sweep(ARMOR, 36, rings(c, w, u, 2.4F, dome));
        GalactusHull upper = h(GalactusRig.UPPER + side);
        ys(upper, SUIT, 24, 2.3F, 50.0, 3.0, 3.2, s * 11.0, 0.0, 0.7, 48.5, 3.8, 4.0, s * 11.0, 0.05, 0.9, 46.0, 4.1,
                4.2, s * 11.05, 0.15, 1, 43.0, 3.85, 4.0, s * 11.15, 0.3, 1, 40.0, 3.4, 3.6, s * 11.3, 0.3, 1, 38.0,
                3.1, 3.3, s * 11.4, 0.3, 0.9, 36.8, 2.8, 3.0, s * 11.45, 0.3, 0.75);
        band(upper, ARMOR, 45.0, 46.2, 4.45, 4.6, s * 11.05, 0.15, 2.4F);
        band(upper, ARMOR, 43.7, 44.8, 4.2, 4.35, s * 11.12, 0.25, 2.4F);
        band(upper, TRIM, 41.0, 41.8, 3.75, 3.95, s * 11.22, 0.3, 2.4F);
        GalactusHull fore = h(GalactusRig.FORE + side);
        fore.ball(ARMOR, 16, 10, v(s * 11.5, 37.6, -1.9), 2.3F, 2.5F, 1.6F, 2.6F, 1.0F);
        ys(fore, SUIT, 24, 2.3F, 38.6, 2.7, 2.9, s * 11.5, 0.3, 0.75, 37.5, 3.05, 3.2, s * 11.5, 0.3, 0.95, 35.5,
                3.25, 3.4, s * 11.55, 0.3, 1, 33.8, 3.1, 3.3, s * 11.6, 0.3, 1);
        ys(fore, ARMOR, 36, 2.5F, 34.6, 3.3, 3.45, s * 11.6, 0.3, 0.7, 34.0, 3.75, 3.9, s * 11.6, 0.3, 0.95, 31.5,
                4.1, 4.25, s * 11.65, 0.3, 1, 29.3, 4.5, 4.65, s * 11.7, 0.3, 1, 28.5, 4.65, 4.8, s * 11.7, 0.3, 1,
                28.0, 4.45, 4.6, s * 11.7, 0.3, 0.85, 27.8, 3.9, 4.0, s * 11.7, 0.3, 0.7);
        band(fore, TRIM, 32.9, 33.5, 3.95, 4.1, s * 11.62, 0.3, 2.5F);
        band(fore, TRIM, 31.0, 31.6, 4.27, 4.42, s * 11.66, 0.3, 2.5F);
        band(fore, TRIM, 29.4, 30.0, 4.58, 4.73, s * 11.7, 0.3, 2.5F);
        band(fore, ENERGY, 28.2, 28.45, 4.72, 4.87, s * 11.7, 0.3, 2.5F);
    }

    // Rings round axis w through c: rows of (height along w, half-size along u, along the axis square to both,
    // shade).
    private static Ring[] rings(Vector3f c, Vector3f w, Vector3f u, float n, double[] rows) {
        Vector3f other = new Vector3f(w).cross(u).normalize();
        Ring[] out = new Ring[rows.length / 4];
        for (int i = 0; i < out.length; i++) {
            out[i] = new Ring(new Vector3f(c).fma((float) rows[4 * i], w), u, other, (float) rows[4 * i + 1],
                    (float) rows[4 * i + 2], n, (float) rows[4 * i + 3]);
        }
        return out;
    }

    private void hand(int side, GalactusRig rig) {
        double s = GalactusRig.sign(side);
        double x = s * GalactusRig.HAND_X;
        GalactusHull hand = h(GalactusRig.HAND + side);
        ys(hand, ARMOR_DARK, 24, 3.5F, 28.0, 0.95, 1.7, x, 0.3, 0.7, 27.0, 1.05, 1.95, x, 0.28, 0.9, 25.0, 1.0, 2.25,
                x, 0.22, 1, 23.4, 0.9, 2.35, x, 0.15, 1, 22.7, 0.75, 2.2, x, 0.1, 0.85);
        ys(hand, ARMOR, 20, 4.0F, 27.2, 0.3, 1.7, x + s * 0.9, 0.3, 0.8, 26.6, 0.38, 1.95, x + s * 0.95, 0.28, 1, 24.4,
                0.38, 2.15, x + s * 0.92, 0.2, 1, 23.4, 0.3, 2.1, x + s * 0.85, 0.15, 0.85);
        zs(hand, TRIM, 14, 3.0F, -2.3, 0.4, 0.45, x + s * 0.75, 23.1, 0.8, -2.0, 0.55, 0.6, x + s * 0.8, 23.1, 1, 2.2,
                0.55, 0.6, x + s * 0.8, 23.05, 1, 2.5, 0.4, 0.45, x + s * 0.75, 23.05, 0.8);
        hand.ball(ARMOR_DARK, 14, 8, v(x - s * 0.25, 25.9, 1.65), 0.9F, 1.3F, 0.85F, 2.0F, 0.95F);
        for (int f = 0; f < 5; f++) {
            Vector3f way = f == GalactusRig.THUMB ? GalactusRig.thumbWay(side) : v(0, -1, 0);
            for (int k = 0; k < 3; k++) {
                int bone = GalactusRig.digit(side, f, k);
                Vector3f from = rig.pivot[bone];
                float length = GalactusRig.LENGTH[f][k];
                Vector3f to = new Vector3f(from).fma(length, way);
                float r0 = GalactusRig.RADIUS[f] * (float) Math.pow(GalactusRig.TAPER, k);
                float r1 = r0 * GalactusRig.TAPER;
                h(bone).capsule(ARMOR, 16, from, to, X, r0, r1, 0.92F, k == 0 ? 0.9F : 1.0F);
                if (f != GalactusRig.THUMB) {
                    // A plate over the back of each finger bone.
                    Vector3f back = v(s * r0 * 0.72, 0, 0);
                    h(bone).limb(TRIM, 12, 4.0F, new Vector3f(from).add(back).add(0, -0.1F, 0),
                            new Vector3f(to).add(back).add(0, 0.15F, 0), X, 0, 0.18F, r0 * 0.7F, 0, 0, 0.85F, 0.2F,
                            0.24F, r0 * 0.78F, 0, 0, 1, 1, 0.16F, r1 * 0.65F, 0, 0, 0.9F);
                }
            }
        }
    }
}
