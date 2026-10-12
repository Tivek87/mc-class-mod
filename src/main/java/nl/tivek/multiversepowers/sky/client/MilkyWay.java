package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.stream.IntStream;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Perlin;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Cluster;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Nebula;

final class MilkyWay {
    static final int SIZE = 1024;
    static final Vec3[][] FACES = {
        {new Vec3(1, 0, 0), new Vec3(0, 0, -1), new Vec3(0, -1, 0)},
        {new Vec3(-1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, -1, 0)},
        {new Vec3(0, 1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1)},
        {new Vec3(0, -1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, -1)},
        {new Vec3(0, 0, 1), new Vec3(1, 0, 0), new Vec3(0, -1, 0)},
        {new Vec3(0, 0, -1), new Vec3(-1, 0, 0), new Vec3(0, -1, 0)}};

    private static final Vec3 N = SkyBodies.BAND;
    private static final Vec3 C = SkyBodies.CORE.subtract(N.scale(SkyBodies.CORE.dot(N))).normalize();
    private static final Vec3 S = N.cross(C);

    private MilkyWay() {
    }

    static NativeImage face(int index) {
        Vec3[] face = FACES[index];
        NativeImage image = new NativeImage(SIZE, SIZE, false);
        IntStream.range(0, SIZE).parallel().forEach(row -> {
            double[] rgb = new double[3];
            for (int col = 0; col < SIZE; col++) {
                double a = (col + 0.5) / SIZE * 2.0 - 1.0;
                double b = (row + 0.5) / SIZE * 2.0 - 1.0;
                Vec3 d = face[0].add(face[1].scale(a)).add(face[2].scale(b)).normalize();
                rgb[0] = 0.0;
                rgb[1] = 0.0;
                rgb[2] = 0.0;
                paint(d, col + index * SIZE, row, rgb);
                double dither = Noise.of(col, row, index + 40) + Noise.of(col, row, index + 80) - 1.0;
                image.setPixelRGBA(col, row, SkyColors.added(rgb[0], rgb[1], rgb[2], dither));
            }
        });
        return image;
    }

    private static void paint(Vec3 d, int px, int py, double[] rgb) {
        double x = d.x;
        double y = d.y;
        double z = d.z;
        double haze = 0.55 + 0.9 * Math.max(0.0, Perlin.fbm(x * 1.6 + 3.0, y * 1.6, z * 1.6, 4) + 0.25);
        rgb[0] += 0.006 * haze;
        rgb[1] += 0.010 * haze;
        rgb[2] += 0.026 * haze;
        double band = band(d, rgb);
        for (Nebula nebula : SkyBodies.NEBULAE) {
            nebula(nebula, d, rgb);
        }
        for (Cluster cluster : SkyBodies.CLUSTERS) {
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, d.dot(cluster.dir()))));
            double r = angle / Math.toRadians(cluster.degrees() * (cluster.globular() ? 0.45 : 0.6));
            double glow = Math.exp(-r * r) * (cluster.globular() ? 0.45 : 0.06);
            rgb[0] += glow * SkyColors.red(cluster.color());
            rgb[1] += glow * SkyColors.green(cluster.color());
            rgb[2] += glow * SkyColors.blue(cluster.color());
        }
        double chance = Noise.of(px, py, 3);
        double density = 0.004 + 0.12 * band;
        if (chance < density) {
            double bright = 0.08 + 0.6 * Math.pow(Noise.of(px, py, 5), 4.0);
            double warm = Noise.of(px, py, 7);
            rgb[0] += bright * (0.8 + 0.3 * warm);
            rgb[1] += bright * 0.9;
            rgb[2] += bright * (1.1 - 0.3 * warm);
        }
    }

    private static double band(Vec3 d, double[] rgb) {
        double lat = d.dot(N);
        if (Math.abs(lat) > 0.6) {
            return 0.0;
        }
        double c = d.dot(C);
        double s = d.dot(S);
        double lon = Math.atan2(s, c);
        double sx = c * 3.2 + 7.0;
        double sy = s * 3.2;
        double sz = lat * 12.0;
        double wave = Perlin.fbm(sx * 0.5, sy * 0.5, sz * 0.4 + 2.0, 4);
        double width = 0.15 + 0.05 * wave + 0.08 * Math.exp(-lon * lon / 0.3);
        double warped = lat + 0.035 * Perlin.fbm(sx * 0.8 + 11.0, sy * 0.8, sz * 0.3, 4);
        double across = warped / width;
        double body = Math.exp(-across * across) * (0.32 + 0.68 * Math.exp(-lon * lon / 1.4));
        double outer = Math.exp(-across * across * 0.25) * 0.25 * (0.4 + 0.6 * Math.exp(-lon * lon / 2.0));
        double clouds = 0.45 + 0.55 * Math.max(0.0, Perlin.fbm(sx, sy, sz, 6) + 0.45);
        double clumps = Math.pow(Math.max(0.0, Perlin.fbm(sx * 3.0 + 4.0, sy * 3.0, sz * 2.0, 4) + 0.2), 1.5);
        double bulge = Math.exp(-(lon * lon / 0.09 + warped * warped / 0.012));
        double heart = Math.exp(-(lon * lon / 0.012 + lat * lat / 0.0022));
        double lane = Math.exp(-Math.pow((warped - 0.012 * Math.sin(lon * 3.0)) / (width * 0.28), 2.0))
                * Surfaces.smooth(-0.3, 0.3, Perlin.fbm(sx * 0.9 + 21.0, sy * 0.9, sz * 0.6, 5));
        double clouds2 = Surfaces.smooth(0.15, 0.6, Perlin.fbm(sx * 1.6 + 5.0, sy * 1.6, sz * 1.1, 6))
                * Math.exp(-across * across * 0.6);
        double dust = Math.min(1.0, lane * 0.85 + clouds2 * 0.6);
        double clear = 1.0 - 0.85 * dust;
        double warmth = Math.exp(-lon * lon / 0.5);
        double pink = Surfaces.smooth(0.2, 0.7, Perlin.fbm(sx * 1.3 + 31.0, sy * 1.3, sz, 4)) * body;
        double light = (body * clouds * 0.5 + body * clumps * 0.4) * clear;
        double r = light * Surfaces.mix(0.62, 1.0, warmth) + pink * 0.12 * clear;
        double g = light * Surfaces.mix(0.70, 0.80, warmth) + pink * 0.04 * clear;
        double b = light * Surfaces.mix(1.0, 0.62, warmth) + pink * 0.11 * clear;
        r += outer * 0.08;
        g += outer * 0.07;
        b += outer * 0.15;
        double core = bulge * (0.55 + 0.45 * clouds) * (1.0 - 0.75 * dust);
        r += core * 0.95 + heart * 1.1;
        g += core * 0.62 + heart * 0.95;
        b += core * 0.32 + heart * 0.75;
        double redden = dust * (1.0 - dust) * 4.0 * (body + bulge) * 0.08;
        r += redden * 1.0;
        g += redden * 0.45;
        b += redden * 0.2;
        rgb[0] += r;
        rgb[1] += g;
        rgb[2] += b;
        return Math.min(1.0, (body + bulge) * clear);
    }

    private static void nebula(Nebula nebula, Vec3 d, double[] rgb) {
        Vec3 n = nebula.dir();
        double facing = d.dot(n);
        double radius = Math.toRadians(nebula.degrees() * 0.5);
        if (facing < Math.cos(radius * 1.8)) {
            return;
        }
        Vec3[] frame = SkyBodies.frame(n);
        double span = Math.tan(radius);
        double x = d.dot(frame[0]) / facing / span;
        double y = d.dot(frame[1]) / facing / span;
        double o = nebula.seed() * 5.31;
        double wx = Perlin.fbm(x * 1.4 + o, y * 1.4, o, 4);
        double wy = Perlin.fbm(x * 1.4, y * 1.4 + o, o + 3.0, 4);
        double qx = x + wx * 0.45;
        double qy = y + wy * 0.45;
        double r = Math.sqrt(qx * qx + qy * qy);
        double shape = Math.exp(-r * r * 1.3);
        if (shape < 0.002) {
            return;
        }
        double cloud = Surfaces.smooth(-0.3, 0.75, Perlin.fbm(qx * 2.2 + o, qy * 2.2, o * 0.5, 6));
        double veins = Math.pow(Perlin.ridged(qx * 2.6 + o, qy * 2.6, o + 7.0, 5), 3.0);
        double dark = Surfaces.smooth(0.25, 0.7, Perlin.fbm(qx * 3.0 + o + 9.0, qy * 3.0, o, 5));
        double mix = Surfaces.smooth(-0.45, 0.45, Perlin.fbm(qx * 1.2 + o + 17.0, qy * 1.2, o, 3));
        double glow = shape * (cloud * 0.55 + veins * cloud * 0.8 + Math.exp(-r * r * 6.0) * 0.3)
                * (1.0 - 0.55 * dark) * nebula.strength();
        double cr = Surfaces.mix(SkyColors.red(nebula.colorA()), SkyColors.red(nebula.colorB()), mix);
        double cg = Surfaces.mix(SkyColors.green(nebula.colorA()), SkyColors.green(nebula.colorB()), mix);
        double cb = Surfaces.mix(SkyColors.blue(nebula.colorA()), SkyColors.blue(nebula.colorB()), mix);
        double hot = Math.pow(veins * cloud * shape, 2.0) * 0.55 * nebula.strength();
        rgb[0] += glow * cr + hot;
        rgb[1] += glow * cg + hot;
        rgb[2] += glow * cb + hot;
    }
}
