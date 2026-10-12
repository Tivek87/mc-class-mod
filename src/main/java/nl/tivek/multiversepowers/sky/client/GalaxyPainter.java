package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.stream.IntStream;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Perlin;

final class GalaxyPainter {
    static final int SIZE = 640;
    private static final double TIP = Math.cos(Math.toRadians(62.0));
    private static final double WIND = 3.0;

    private GalaxyPainter() {
    }

    static NativeImage paint() {
        NativeImage image = new NativeImage(SIZE, SIZE, false);
        IntStream.range(0, SIZE).parallel().forEach(row -> {
            for (int col = 0; col < SIZE; col++) {
                double u = (col + 0.5) / SIZE * 2.0 - 1.0;
                double v = (row + 0.5) / SIZE * 2.0 - 1.0;
                double[] rgb = light(u, v, col, row);
                double edge = Surfaces.smooth(1.0, 0.8, Math.max(Math.abs(u), Math.abs(v)));
                double dither = Noise.of(col, row, 91) + Noise.of(col, row, 92) - 1.0;
                image.setPixelRGBA(col, row, SkyColors.added(rgb[0] * edge, rgb[1] * edge, rgb[2] * edge,
                        dither));
            }
        });
        return image;
    }

    private static double[] light(double u, double v, int col, int row) {
        double y = v / TIP;
        double r = Math.sqrt(u * u + y * y);
        double angle = Math.atan2(y, u);
        double wx = Perlin.fbm(u * 3.0 + 2.0, y * 3.0, 0.5, 4);
        double wy = Perlin.fbm(u * 3.0, y * 3.0 + 6.0, 1.5, 4);
        double phase = angle + Math.log(r + 0.03) * WIND + wx * 0.5;
        double arm = Math.pow(0.5 + 0.5 * Math.cos(2.0 * phase), 2.6);
        double lane = Math.pow(0.5 + 0.5 * Math.cos(2.0 * (phase + 0.55)), 7.0) * Surfaces.smooth(0.05, 0.18, r);
        double disk = Math.exp(-r / 0.42) * Surfaces.smooth(1.0, 0.75, r);
        double clumps = 0.6 + 0.4 * Math.max(0.0, Perlin.fbm(u * 6.0 + wx, y * 6.0 + wy, 3.0, 5) + 0.35);
        double arms = disk * (0.1 + 1.3 * arm) * clumps * (1.0 - 0.75 * lane);
        double bulgeR = Math.sqrt(u * u + (v / 0.72) * (v / 0.72));
        double bulge = Math.exp(-bulgeR * bulgeR / 0.016) * 1.1 + Math.exp(-bulgeR * bulgeR / 0.07) * 0.35;
        double halo = Math.exp(-r / 0.5) * 0.035;
        double out = Surfaces.smooth(0.12, 0.75, r);
        double red = arms * Surfaces.mix(1.05, 0.55, out) * 0.95;
        double green = arms * Surfaces.mix(0.74, 0.70, out) * 0.95;
        double blue = arms * Surfaces.mix(0.42, 1.05, out) * 0.95;
        double knots = Surfaces.smooth(0.55, 0.85, Perlin.noise(u * 46.0, y * 46.0, 7.0)) * arm * disk
                * Surfaces.smooth(0.12, 0.3, r);
        red += knots * 1.1;
        green += knots * 0.35;
        blue += knots * 0.6;
        red += bulge * 1.0 + halo * 0.6;
        green += bulge * 0.82 + halo * 0.7;
        blue += bulge * 0.55 + halo * 1.0;
        if (Noise.of(col, row, 95) < arm * disk * 0.04) {
            double b = 0.15 + 0.35 * Noise.of(col, row, 96);
            red += b * 0.75;
            green += b * 0.85;
            blue += b;
        }
        return new double[] {red, green, blue};
    }
}
