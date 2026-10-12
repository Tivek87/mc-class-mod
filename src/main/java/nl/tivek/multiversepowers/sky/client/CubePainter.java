package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import java.util.stream.IntStream;
import nl.tivek.multiversepowers.engine.math.Perlin;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Kind;

// The skins of the sky's cubes, all in one picture: a row for each skin, its six faces side by side (z-, z+, x-, x+,
// y-, y+, as TexturedBox lays them), each FACE pixels square. A face is painted as the globe's surface seen through
// it, in as many pixels as the cube is wide in the sky (`res`), so every pixel is about the moon's size. A ring's row
// holds one strip across the ring, from its inner edge out.
final class CubePainter {
    static final int FACE = 16;
    static final int FACES = 6;

    interface Paint {
        // rgb (and cover, 1 unless set) at a point of the unit sphere; a ring's at `x` of the way across it.
        void at(double x, double y, double z, double[] rgba);
    }

    record Skin(int res, boolean ring, Paint paint) {
    }

    private CubePainter() {
    }

    static NativeImage atlas(List<Skin> skins) {
        NativeImage image = new NativeImage(FACE * FACES, FACE * skins.size(), true);
        IntStream.range(0, skins.size()).parallel().forEach(row -> paint(image, row, skins.get(row)));
        return image;
    }

    // A face's square in the atlas: u0, v0, u1, v1, drawn in a hair so no face bleeds into the next.
    static float[] square(int row, int rows, int face) {
        float inset = 0.02F;
        return new float[] { (face * FACE + inset) / (FACE * FACES), (row * FACE + inset) / (FACE * rows),
            ((face + 1) * FACE - inset) / (FACE * FACES), ((row + 1) * FACE - inset) / (FACE * rows) };
    }

    static Skin globe(Kind kind, long seed, double degrees) {
        int res = Math.max(2, Math.min(FACE, (int) Math.round(degrees * 0.62 / SkyTexture.TEXEL)));
        return new Skin(res, false, (x, y, z, rgba) -> {
            double[] surface = new double[Surfaces.OUT];
            Surfaces.paint(kind, seed, x, y, z, surface);
            // Craters and ridges show as shade: no light falls across so small a face.
            double relief = !Surfaces.bumpy(kind) ? 1.15
                    : 1.15 * Math.max(0.6, Math.min(1.2, 1.0 + 0.9 * Surfaces.height(kind, seed, x, y, z)));
            rgba[0] = surface[Surfaces.R] * relief + surface[Surfaces.GLOW_R] * 0.8;
            rgba[1] = surface[Surfaces.G] * relief + surface[Surfaces.GLOW_G] * 0.8;
            rgba[2] = surface[Surfaces.B] * relief + surface[Surfaces.GLOW_B] * 0.8;
        });
    }

    // The sun: each face white-hot in its middle, deepening to gold at its edges, mottled a little.
    static Skin sun() {
        return new Skin(FACE, false, (x, y, z, rgba) -> {
            double most = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
            double a = Math.abs(x) / most;
            double b = Math.abs(y) / most;
            double c = Math.abs(z) / most;
            // How far out on its face: the two smaller of the three run 0 to 1 across it.
            double edge = a + b + c - 1.0 - Math.min(a, Math.min(b, c));
            double rim = Surfaces.smooth(0.25, 1.0, Math.max(edge, 0.0));
            double mottle = 0.06 * Perlin.fbm(x * 3.0 + 4.0, y * 3.0, z * 3.0, 3);
            rgba[0] = 1.0;
            rgba[1] = Surfaces.mix(0.99, 0.80, rim) + mottle;
            rgba[2] = Surfaces.mix(0.90, 0.34, rim) + mottle;
        });
    }

    // A ring's strip: bands of its colour with gaps between them, brighter in its middle.
    static Skin ring(int rgb, long seed) {
        return new Skin(FACE, true, (x, y, z, rgba) -> {
            double band = Perlin.noise(x * 9.0 + seed * 1.7, seed * 0.31, 0.5);
            double light = 0.62 + 0.38 * Math.sin(x * Math.PI);
            rgba[3] = band > -0.18 && x > 0.04 && x < 0.97 ? 1.0 : 0.0;
            rgba[0] = SkyColors.red(rgb) * light * (0.85 + 0.3 * band);
            rgba[1] = SkyColors.green(rgb) * light * (0.85 + 0.3 * band);
            rgba[2] = SkyColors.blue(rgb) * light * (0.85 + 0.3 * band);
        });
    }

    private static void paint(NativeImage image, int row, Skin skin) {
        double[] rgba = new double[4];
        int res = skin.res();
        for (int face = 0; face < (skin.ring() ? 1 : FACES); face++) {
            for (int j = 0; j < FACE; j++) {
                for (int i = 0; i < FACE; i++) {
                    int ci = i * res / FACE;
                    int cj = j * res / FACE;
                    double u = (ci + 0.5) / res * 2.0 - 1.0;
                    double v = (cj + 0.5) / res * 2.0 - 1.0;
                    rgba[3] = 1.0;
                    if (skin.ring()) {
                        skin.paint().at((ci + 0.5) / res, 0.0, 0.0, rgba);
                    } else {
                        double x = face == 2 ? -1.0 : face == 3 ? 1.0 : u;
                        double y = face == 4 ? -1.0 : face == 5 ? 1.0 : v;
                        double z = face == 0 ? -1.0 : face == 1 ? 1.0 : face >= 4 ? v : u;
                        double length = Math.sqrt(x * x + y * y + z * z);
                        skin.paint().at(x / length, y / length, z / length, rgba);
                    }
                    boolean solid = rgba[3] >= 0.5;
                    // Its brightness in a few shades, its colour kept: stepped each on its own, red, green and blue
                    // would speckle a grey face pink and yellow.
                    double most = Math.max(rgba[0], Math.max(rgba[1], rgba[2]));
                    double shade = most < 1.0E-6 ? 0.0 : SkyColors.pixel(SkyColors.soft(most), ci, cj, 0.0) / most;
                    image.setPixelRGBA(face * FACE + i, row * FACE + j, solid ? SkyColors.premultiplied(
                            rgba[0] * shade, rgba[1] * shade, rgba[2] * shade, 1.0) : 0);
                }
            }
        }
    }
}
