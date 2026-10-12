package nl.tivek.multiversepowers.sky.client;

import nl.tivek.multiversepowers.engine.math.Perlin;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Kind;

final class Surfaces {
    static final int R = 0;
    static final int G = 1;
    static final int B = 2;
    static final int GLOW_R = 3;
    static final int GLOW_G = 4;
    static final int GLOW_B = 5;
    static final int SHINE = 6;
    static final int OUT = 7;

    private Surfaces() {
    }

    static boolean bumpy(Kind kind) {
        return switch (kind) {
            case MOON, ICE_MOON, ROCK, MARS, LAVA -> true;
            default -> false;
        };
    }

    static double air(Kind kind) {
        return switch (kind) {
            case EARTH -> 1.0;
            case ICE, TEAL -> 0.8;
            case GAS, SATURN -> 0.45;
            case VIOLET -> 0.7;
            case MARS -> 0.35;
            case LAVA -> 0.4;
            default -> 0.0;
        };
    }

    static int airColor(Kind kind) {
        return switch (kind) {
            case EARTH -> 0x5A9CFF;
            case ICE -> 0x6FA8FF;
            case TEAL -> 0x7FE6E6;
            case GAS -> 0xF0D2A8;
            case SATURN -> 0xF2DEB0;
            case VIOLET -> 0xE07CF0;
            case MARS -> 0xFF9A70;
            case LAVA -> 0xFF7030;
            default -> 0x000000;
        };
    }

    static void paint(Kind kind, long seed, double x, double y, double z, double[] out) {
        java.util.Arrays.fill(out, 0.0);
        double o = (seed % 97) * 3.17;
        switch (kind) {
            case EARTH -> earth(x, y, z, o, out);
            case GAS -> gas(x, y, z, o, out);
            case SATURN -> saturn(x, y, z, o, out);
            case ICE -> ice(x, y, z, o, out);
            case TEAL -> teal(x, y, z, o, out);
            case VIOLET -> violet(x, y, z, o, out);
            case MARS -> mars(x, y, z, o, out);
            case MOON -> moon(x, y, z, o, out);
            case ICE_MOON -> iceMoon(x, y, z, o, out);
            case ROCK -> rock(x, y, z, o, out);
            case LAVA -> lava(x, y, z, o, out);
        }
    }

    static double height(Kind kind, long seed, double x, double y, double z) {
        double o = (seed % 97) * 3.17;
        return switch (kind) {
            case MOON -> Craters.at(x, y, z, 5.0, seed) * 0.8 + Craters.at(x, y, z, 13.0, seed + 1) * 0.35
                    + Perlin.fbm(x * 6 + o, y * 6, z * 6, 4) * 0.08;
            case ROCK -> Craters.at(x, y, z, 4.0, seed) + Perlin.fbm(x * 3 + o, y * 3, z * 3, 5) * 0.35;
            case ICE_MOON -> -Perlin.ridged(x * 3 + o, y * 3, z * 3, 4) * 0.25;
            case MARS -> Craters.at(x, y, z, 7.0, seed) * 0.15 + Perlin.fbm(x * 4 + o, y * 4, z * 4, 5) * 0.2;
            case LAVA -> Perlin.fbm(x * 4 + o, y * 4, z * 4, 5) * 0.3;
            default -> 0.0;
        };
    }

    private static void earth(double x, double y, double z, double o, double[] out) {
        double wx = Perlin.fbm(x * 1.4 + o, y * 1.4, z * 1.4, 4);
        double wy = Perlin.fbm(x * 1.4, y * 1.4 + o + 5.2, z * 1.4, 4);
        double wz = Perlin.fbm(x * 1.4, y * 1.4, z * 1.4 + o + 9.1, 4);
        double qx = x * 1.2 + wx * 0.55;
        double qy = y * 1.2 + wy * 0.55;
        double qz = z * 1.2 + wz * 0.55;
        double h = Perlin.fbm(qx + o, qy, qz, 7);
        double lat = Math.abs(y);
        double sea = 0.05;
        double r;
        double g;
        double b;
        boolean land = h > sea;
        if (land) {
            double t = h - sea;
            double dry = smooth(-0.15, 0.45, Perlin.fbm(x * 2.6 + o, y * 2.6, z * 2.6 + 4.0, 4)
                    + 0.5 - Math.abs(lat - 0.32) * 2.2);
            r = mix(0.10, 0.62, dry);
            g = mix(0.24, 0.48, dry);
            b = mix(0.08, 0.28, dry);
            double rough = smooth(0.18, 0.45, t);
            r = mix(r, 0.40, rough);
            g = mix(g, 0.34, rough);
            b = mix(b, 0.28, rough);
            double coast = 1.0 - smooth(0.0, 0.03, t);
            r = mix(r, 0.55, coast * 0.4);
            g = mix(g, 0.50, coast * 0.4);
            b = mix(b, 0.36, coast * 0.4);
            double grain = 0.85 + 0.3 * Perlin.noise(x * 40 + o, y * 40, z * 40);
            r *= grain;
            g *= grain;
            b *= grain;
        } else {
            double deep = smooth(0.0, 0.28, sea - h);
            r = mix(0.05, 0.015, deep);
            g = mix(0.30, 0.06, deep);
            b = mix(0.45, 0.22, deep);
            out[SHINE] = 1.0;
        }
        double ice = smooth(0.88, 0.92, lat + Perlin.fbm(x * 4 + o, y * 4, z * 4, 3) * 0.06);
        r = mix(r, 0.92, ice);
        g = mix(g, 0.95, ice);
        b = mix(b, 0.98, ice);
        out[SHINE] *= 1.0 - ice;
        double cx = x * 2.0 + wy * 0.9;
        double cy = y * 2.0 * 1.6 + wz * 0.9;
        double cz = z * 2.0 + wx * 0.9;
        double c = Perlin.fbm(cx + o + 21.0, cy, cz, 7) + 0.18 * Perlin.ridged(cx * 2.0, cy * 2.0, cz * 2.0 + o, 4);
        double cover = smooth(0.12, 0.62, c) * 0.92;
        r = mix(r, 0.95, cover);
        g = mix(g, 0.96, cover);
        b = mix(b, 0.98, cover);
        out[SHINE] *= 1.0 - cover;
        if (land && ice < 0.5) {
            double towns = Math.max(0.0, Perlin.fbm(x * 22 + o, y * 22, z * 22, 3));
            double where = smooth(0.0, 0.5, Perlin.fbm(x * 3.5 + o, y * 3.5, z * 3.5 + 7.0, 3));
            double lights = towns * towns * where * 3.0 * (1.0 - cover * 0.85);
            out[GLOW_R] = lights * 1.0;
            out[GLOW_G] = lights * 0.7;
            out[GLOW_B] = lights * 0.35;
        }
        out[R] = r;
        out[G] = g;
        out[B] = b;
    }

    private static void gas(double x, double y, double z, double o, double[] out) {
        double[] p = swirl(x, y, z, -0.38, 0.9, 0.12, 1.2);
        double px = p[0];
        double py = p[1];
        double pz = p[2];
        double v = py + 0.014 * Perlin.fbm(px * 3.0 + o, py * 26.0, pz * 3.0, 6)
                + 0.006 * Perlin.fbm(px * 12.0, py * 60.0 + o, pz * 12.0, 3);
        double layout = Perlin.noise(v * 6.5, o, 0.5) + 0.5 * Perlin.noise(v * 15.0, o + 3.0, 0.5)
                + 0.35 * Math.sin(v * Math.PI * 9.0);
        double t = 0.22 + 0.78 * smooth(-0.3, 0.45, layout);
        double kind = 0.5 + 0.5 * Perlin.noise(v * 4.0, o + 7.0, 0.5) * 1.6;
        double beltR = mix(0.50, 0.80, kind);
        double beltG = mix(0.32, 0.50, kind);
        double beltB = mix(0.22, 0.30, kind);
        double zoneR = mix(0.90, 0.97, kind);
        double zoneG = mix(0.83, 0.92, kind);
        double zoneB = mix(0.72, 0.84, kind);
        double r = mix(beltR, zoneR, t);
        double g = mix(beltG, zoneG, t);
        double b = mix(beltB, zoneB, t);
        double streak = 0.9 + 0.1 * Perlin.noise(px * 7.0, py * 70.0, pz * 7.0 + o);
        double pole = smooth(0.68, 0.95, Math.abs(y));
        r = mix(r * streak, 0.52, pole * 0.7);
        g = mix(g * streak, 0.48, pole * 0.7);
        b = mix(b * streak, 0.46, pole * 0.7);
        double eye = oval(x, y, z, -0.38, 0.9, 0.2, 0.1);
        double rim = Math.exp(-Math.pow((eye - 0.55) / 0.18, 2.0));
        double core = smooth(0.45, 0.85, eye);
        r = mix(mix(r, 0.97, rim * 0.5), 0.80, core);
        g = mix(mix(g, 0.90, rim * 0.5), 0.38, core);
        b = mix(mix(b, 0.80, rim * 0.5), 0.22, core);
        out[R] = r;
        out[G] = g;
        out[B] = b;
    }

    private static void saturn(double x, double y, double z, double o, double[] out) {
        double v = y + 0.02 * Perlin.fbm(x * 2.0 + o, y * 14.0, z * 2.0, 4);
        double band = Math.sin(v * Math.PI * 9.0 + 0.6 * Math.sin(v * 5.0));
        double t = smooth(-0.6, 0.6, band);
        double r = mix(0.80, 0.95, t);
        double g = mix(0.66, 0.86, t);
        double b = mix(0.42, 0.62, t);
        double fine = 0.94 + 0.06 * Perlin.noise(x * 5.0, y * 60.0, z * 5.0 + o);
        double pole = smooth(0.72, 0.97, Math.abs(y));
        out[R] = mix(r * fine, 0.62, pole * 0.6);
        out[G] = mix(g * fine, 0.62, pole * 0.6);
        out[B] = mix(b * fine, 0.58, pole * 0.6);
    }

    private static void ice(double x, double y, double z, double o, double[] out) {
        double v = y + 0.03 * Perlin.fbm(x * 2.0 + o, y * 10.0, z * 2.0, 4);
        double band = 0.5 + 0.5 * Math.sin(v * Math.PI * 5.0);
        double r = mix(0.16, 0.30, band);
        double g = mix(0.36, 0.55, band);
        double b = mix(0.86, 1.0, band);
        double spot = smooth(0.4, 0.8, oval(x, y, z, -0.3, 2.2, 0.16, 0.09));
        r = mix(r, 0.06, spot);
        g = mix(g, 0.14, spot);
        b = mix(b, 0.45, spot);
        double cloud = smooth(0.35, 0.7, Perlin.fbm(x * 3.0 + o, y * 22.0, z * 3.0, 4));
        out[R] = mix(r, 0.92, cloud * 0.8);
        out[G] = mix(g, 0.96, cloud * 0.8);
        out[B] = mix(b, 1.0, cloud * 0.8);
    }

    private static void teal(double x, double y, double z, double o, double[] out) {
        double v = y + 0.06 * Perlin.fbm(x * 2.0 + o, y * 7.0, z * 2.0, 5);
        double band = Math.sin(v * Math.PI * 6.0 + Math.sin(v * 13.0) * 0.7);
        double t = smooth(-0.5, 0.5, band);
        double light = smooth(0.75, 0.95, band) * 0.5;
        out[R] = mix(0.10, 0.30, t) + light * 0.5;
        out[G] = mix(0.34, 0.66, t) + light * 0.3;
        out[B] = mix(0.42, 0.70, t) + light * 0.25;
    }

    private static void violet(double x, double y, double z, double o, double[] out) {
        double wx = Perlin.fbm(x * 2.0 + o, y * 2.0, z * 2.0, 4);
        double wz = Perlin.fbm(x * 2.0, y * 2.0, z * 2.0 + o, 4);
        double v = y * 3.0 + wx * 0.9 + wz * 0.4;
        double swirl = Perlin.fbm(x * 3.0 + wx, v, z * 3.0 + wz, 6);
        double t = smooth(-0.5, 0.6, swirl);
        double pink = smooth(0.35, 0.8, Perlin.fbm(x * 4.0 + wz, y * 9.0, z * 4.0 + o, 4));
        out[R] = mix(mix(0.18, 0.50, t), 0.92, pink * 0.6);
        out[G] = mix(mix(0.10, 0.30, t), 0.50, pink * 0.6);
        out[B] = mix(mix(0.32, 0.66, t), 0.74, pink * 0.6);
    }

    private static void mars(double x, double y, double z, double o, double[] out) {
        double dark = smooth(0.0, 0.5, Perlin.fbm(x * 2.2 + o, y * 2.2, z * 2.2, 6));
        double dust = smooth(-0.1, 0.5, Perlin.fbm(x * 5.0, y * 5.0 + o, z * 5.0, 4));
        double r = mix(mix(0.74, 0.90, dust), 0.42, dark);
        double g = mix(mix(0.36, 0.56, dust), 0.21, dark);
        double b = mix(mix(0.17, 0.34, dust), 0.13, dark);
        double cap = smooth(0.86, 0.9, y + Perlin.fbm(x * 6.0, y * 6.0, z * 6.0 + o, 3) * 0.04);
        out[R] = mix(r, 0.95, cap);
        out[G] = mix(g, 0.93, cap);
        out[B] = mix(b, 0.92, cap);
    }

    private static void moon(double x, double y, double z, double o, double[] out) {
        double sea = smooth(0.05, 0.4, Perlin.fbm(x * 1.6 + o, y * 1.6, z * 1.6, 5));
        double grain = 0.9 + 0.2 * Perlin.fbm(x * 12.0, y * 12.0 + o, z * 12.0, 3);
        double tint = 0.5 + 0.5 * Perlin.noise(x + o, y, z);
        out[R] = mix(0.60, 0.33, sea) * grain * mix(0.95, 1.05, tint);
        out[G] = mix(0.58, 0.33, sea) * grain;
        out[B] = mix(0.55, 0.35, sea) * grain * mix(1.05, 0.95, tint);
    }

    private static void iceMoon(double x, double y, double z, double o, double[] out) {
        double cracks = smooth(0.82, 0.95, Perlin.ridged(x * 3.0 + o, y * 3.0, z * 3.0, 4));
        double patch = smooth(0.0, 0.6, Perlin.fbm(x * 2.0, y * 2.0 + o, z * 2.0, 4));
        out[R] = mix(mix(0.82, 0.72, patch), 0.58, cracks);
        out[G] = mix(mix(0.87, 0.78, patch), 0.45, cracks);
        out[B] = mix(mix(0.93, 0.80, patch), 0.38, cracks);
    }

    private static void rock(double x, double y, double z, double o, double[] out) {
        double mottle = smooth(-0.3, 0.5, Perlin.fbm(x * 3.0 + o, y * 3.0, z * 3.0, 5));
        out[R] = mix(0.42, 0.62, mottle);
        out[G] = mix(0.34, 0.50, mottle);
        out[B] = mix(0.27, 0.38, mottle);
    }

    private static void lava(double x, double y, double z, double o, double[] out) {
        double veins = Perlin.ridged(x * 2.5 + o, y * 2.5, z * 2.5, 5);
        double hot = smooth(0.72, 0.92, veins);
        double pools = smooth(0.35, 0.6, Perlin.fbm(x * 3.0, y * 3.0 + o, z * 3.0, 4));
        double glow = Math.max(hot, pools * 0.8);
        out[R] = mix(0.16, 0.30, pools);
        out[G] = mix(0.11, 0.12, pools);
        out[B] = mix(0.09, 0.06, pools);
        out[GLOW_R] = glow * 1.3;
        out[GLOW_G] = glow * glow * 0.55;
        out[GLOW_B] = glow * glow * glow * 0.12;
    }

    private static double oval(double x, double y, double z, double lat, double lon, double wide, double high) {
        double plat = Math.asin(Math.max(-1.0, Math.min(1.0, y)));
        double plon = Math.atan2(z, x);
        double dl = Math.atan2(Math.sin(plon - lon), Math.cos(plon - lon)) * Math.cos(plat);
        double dp = plat - Math.asin(lat);
        double e = Math.sqrt((dl / wide) * (dl / wide) + (dp / high) * (dp / high));
        return Math.max(0.0, 1.0 - e);
    }

    private static double[] swirl(double x, double y, double z, double lat, double lon, double size, double turns) {
        double cl = Math.sqrt(1.0 - lat * lat);
        double cx = Math.cos(lon) * cl;
        double cy = lat;
        double cz = Math.sin(lon) * cl;
        double dx = x - cx;
        double dy = y - cy;
        double dz = z - cz;
        double d2 = dx * dx + dy * dy + dz * dz;
        double angle = turns * Math.exp(-d2 / (size * size));
        if (angle < 1.0E-3) {
            return new double[] {x, y, z};
        }
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double dot = dx * cx + dy * cy + dz * cz;
        double kx = cy * dz - cz * dy;
        double ky = cz * dx - cx * dz;
        double kz = cx * dy - cy * dx;
        return new double[] {
            cx + dx * c + kx * s + cx * dot * (1.0 - c),
            cy + dy * c + ky * s + cy * dot * (1.0 - c),
            cz + dz * c + kz * s + cz * dot * (1.0 - c)};
    }

    static double smooth(double a, double b, double v) {
        double t = Math.max(0.0, Math.min(1.0, (v - a) / (b - a)));
        return t * t * (3.0 - 2.0 * t);
    }

    static double mix(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
