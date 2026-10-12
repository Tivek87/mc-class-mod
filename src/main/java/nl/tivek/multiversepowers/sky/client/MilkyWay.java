package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.stream.IntStream;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Perlin;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Cluster;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Nebula;

// The Milky Way and the clouds of light in the night sky, each a thing of its own laid on the game's sky as the moon
// is, the sky between them left as it is: the band an arc of torn clouds along its great circle, thick at its core
// and gone before it comes round, painted on a strip round the sky (`band`, drawn in PIECES x ROWS quads), each
// nebula and each cluster's glow a picture of its own (`nebula`, `cluster`). Painted in sharp pixels the moon's
// size, the light only added to what is behind.
final class MilkyWay {
    // The band reaches this far either side of its middle line, in degrees.
    static final double HALF = 15.0;
    static final int PIECES = 48;
    static final int ROWS = 4;
    // A nebula's picture is this many times as wide as the nebula, a cluster's glow this many.
    static final double NEBULA_REACH = 1.9;
    static final double CLUSTER_REACH = 1.6;

    private static final Vec3 N = SkyBodies.BAND;
    private static final Vec3 C = SkyBodies.CORE.subtract(N.scale(SkyBodies.CORE.dot(N))).normalize();
    private static final Vec3 S = N.cross(C);

    private MilkyWay() {
    }

    // A point on the band, `lon` round from its core and `lat` off its middle line, both in radians.
    static Vec3 along(double lon, double lat) {
        return C.scale(Math.cos(lon)).add(S.scale(Math.sin(lon))).scale(Math.cos(lat)).add(N.scale(Math.sin(lat)));
    }

    // The band's strip, its left edge at lon -180, its top row at lat +HALF, fading out towards its edges.
    static NativeImage band() {
        int wide = SkyTexture.pixels(360.0);
        int tall = SkyTexture.pixels(2.0 * HALF);
        NativeImage image = new NativeImage(wide, tall, false);
        IntStream.range(0, tall).parallel().forEach(row -> {
            double[] rgb = new double[3];
            double lat = Math.toRadians((0.5 - (row + 0.5) / tall) * 2.0 * HALF);
            // The band's light is gone well inside the strip, so its edge never shows as a straight line.
            double edge = 1.0 - Surfaces.smooth(0.55, 0.95, Math.abs(Math.toDegrees(lat)) / HALF);
            for (int col = 0; col < wide; col++) {
                double lon = ((col + 0.5) / wide * 2.0 - 1.0) * Math.PI;
                rgb[0] = 0.0;
                rgb[1] = 0.0;
                rgb[2] = 0.0;
                band(along(lon, lat), rgb);
                image.setPixelRGBA(col, row, SkyColors.added(rgb[0] * edge, rgb[1] * edge, rgb[2] * edge, col, row));
            }
        });
        return image;
    }

    static NativeImage nebula(Nebula nebula) {
        return picture(nebula.dir(), nebula.degrees() * NEBULA_REACH, (d, rgb) -> nebula(nebula, d, rgb));
    }

    // A cluster's soft glow; its stars are the star field's.
    static NativeImage cluster(Cluster cluster) {
        return picture(cluster.dir(), cluster.degrees() * CLUSTER_REACH, (d, rgb) -> {
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, d.dot(cluster.dir()))));
            double r = angle / Math.toRadians(cluster.degrees() * (cluster.globular() ? 0.45 : 0.6));
            double glow = Math.exp(-r * r) * (cluster.globular() ? 0.45 : 0.06);
            rgb[0] += glow * SkyColors.red(cluster.color());
            rgb[1] += glow * SkyColors.green(cluster.color());
            rgb[2] += glow * SkyColors.blue(cluster.color());
        });
    }

    private interface Light {
        void at(Vec3 d, double[] rgb);
    }

    // A square picture `degrees` wide round `dir`, its top towards the sun's side (SkyBodies.frame), its rim faded out.
    private static NativeImage picture(Vec3 dir, double degrees, Light light) {
        int size = SkyTexture.pixels(degrees);
        Vec3[] frame = SkyBodies.frame(dir);
        double span = Math.tan(Math.toRadians(degrees * 0.5));
        NativeImage image = new NativeImage(size, size, false);
        IntStream.range(0, size).parallel().forEach(row -> {
            double[] rgb = new double[3];
            for (int col = 0; col < size; col++) {
                double a = (col + 0.5) / size * 2.0 - 1.0;
                double b = 1.0 - (row + 0.5) / size * 2.0;
                Vec3 d = dir.add(frame[0].scale(a * span)).add(frame[1].scale(b * span)).normalize();
                rgb[0] = 0.0;
                rgb[1] = 0.0;
                rgb[2] = 0.0;
                light.at(d, rgb);
                double rim = 1.0 - Surfaces.smooth(0.75, 1.0, Math.sqrt(a * a + b * b));
                image.setPixelRGBA(col, row, SkyColors.added(rgb[0] * rim, rgb[1] * rim, rgb[2] * rim, col, row));
            }
        });
        return image;
    }

    private static void band(Vec3 d, double[] rgb) {
        double lat = d.dot(N);
        if (Math.abs(lat) > 0.6) {
            return;
        }
        double c = d.dot(C);
        double s = d.dot(S);
        double lon = Math.atan2(s, c);
        double sx = c * 3.2 + 7.0;
        double sy = s * 3.2;
        double sz = lat * 20.0;
        double wave = Perlin.fbm(sx * 0.5, sy * 0.5, sz * 0.4 + 2.0, 4);
        // Thick at its core, thinning and wandering away from it, as the real one does.
        double width = (0.055 + 0.07 * Math.max(0.0, Math.min(1.0, wave + 0.5)) + 0.05 * Math.exp(-lon * lon / 0.3))
                * (0.55 + 0.45 * Math.exp(-lon * lon / 3.0));
        double warped = lat + 0.045 * Perlin.fbm(sx * 0.8 + 11.0, sy * 0.8, sz * 0.3, 4) + 0.012 * Math.sin(
                lon * 2.0 + 1.0);
        double across = warped / width;
        // Its edges are torn clouds, not a smooth fade.
        double torn = Surfaces.smooth(-0.2, 0.5, 1.6 * Perlin.fbm(sx * 2.2 + 41.0, sy * 2.2, sz * 1.5, 5) + 1.2
                - Math.abs(across));
        // Away from the core it breaks up into patches with dark sky between them, and it ends before it has gone
        // round the sky: an arc of light, not a ring.
        double patch = Surfaces.mix(1.0, Surfaces.smooth(-0.3, 0.3, Perlin.fbm(sx * 0.7 + 60.0, sy * 0.7, 3.0, 4)),
                0.85 * (1.0 - Math.exp(-lon * lon / 2.5))) * (1.0 - Surfaces.smooth(1.2, 2.2, Math.abs(lon)));
        double body = Math.exp(-across * across) * torn * patch * (0.2 + 0.8 * Math.exp(-lon * lon / 1.2));
        double outer = Math.exp(-across * across * 0.4) * torn * 0.25 * Math.exp(-lon * lon / 2.0);
        double clouds = 0.45 + 0.55 * Math.max(0.0, Perlin.fbm(sx, sy, sz, 6) + 0.45);
        double clumps = Math.pow(Math.max(0.0, Perlin.fbm(sx * 3.0 + 4.0, sy * 3.0, sz * 2.0, 4) + 0.2), 1.5);
        double bulge = Math.exp(-(lon * lon / 0.09 + warped * warped / 0.006));
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
