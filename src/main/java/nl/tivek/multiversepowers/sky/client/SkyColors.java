package nl.tivek.multiversepowers.sky.client;

final class SkyColors {
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

    static int added(double r, double g, double b, double dither) {
        double d = dither / 255.0;
        return 255 << 24 | channel(soft(b) + d) << 16 | channel(soft(g) + d) << 8 | channel(soft(r) + d);
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
