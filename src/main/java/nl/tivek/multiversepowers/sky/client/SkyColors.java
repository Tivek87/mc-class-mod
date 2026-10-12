package nl.tivek.multiversepowers.sky.client;

final class SkyColors {
    private static final int SHADES = 9;
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    private SkyColors() {
    }

    // A pixel that only glows keeps the least cover there is: the game's shader drops a pixel with none.
    static int premultiplied(double r, double g, double b, double a) {
        int ir = channel(r);
        int ig = channel(g);
        int ib = channel(b);
        int ia = channel(a);
        if (ia == 0 && (ir | ig | ib) != 0) {
            ia = 1;
        }
        return ia << 24 | ib << 16 | ig << 8 | ir;
    }

    static int added(double r, double g, double b, int x, int y) {
        return 255 << 24 | channel(pixel(soft(b), x, y, 1.0)) << 16 | channel(pixel(soft(g), x, y, 1.0)) << 8
                | channel(pixel(soft(r), x, y, 1.0));
    }

    // A few shades as pixel art has them (even steps of brightness), soft light stepped between them in a 4x4 pattern
    // as much as `dither` lets it: 0 rounds to the nearest shade.
    static double pixel(double v, int x, int y, double dither) {
        double step = Math.sqrt(Math.max(0.0, Math.min(1.0, v))) * SHADES;
        double shade = Math.floor(step + 0.5 + dither * (BAYER[(y & 3) * 4 + (x & 3)] / 16.0 - 0.5 + 1.0 / 32.0));
        double q = Math.max(0.0, Math.min(SHADES, shade)) / SHADES;
        return q * q;
    }

    static double soft(double v) {
        return v < 0.6 ? v : 0.6 + 0.4 * (1.0 - Math.exp(-(v - 0.6) / 0.4));
    }

    static int channel(double v) {
        return (int) Math.round(Math.max(0.0, Math.min(1.0, v)) * 255.0);
    }

    static double red(int rgb) {
        return (rgb >> 16 & 255) / 255.0;
    }

    static double green(int rgb) {
        return (rgb >> 8 & 255) / 255.0;
    }

    static double blue(int rgb) {
        return (rgb & 255) / 255.0;
    }
}
