package nl.tivek.multiversepowers.sky.client;

import nl.tivek.multiversepowers.engine.math.Noise;

final class Craters {
    private Craters() {
    }

    static double at(double x, double y, double z, double scale, long seed) {
        double px = x * scale;
        double py = y * scale;
        double pz = z * scale;
        int cx = (int) Math.floor(px);
        int cy = (int) Math.floor(py);
        int cz = (int) Math.floor(pz);
        int s = (int) seed * 7919;
        double h = 0.0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    int gx = cx + i;
                    int gy = cy + j;
                    int gz = cz + k;
                    if (Noise.of(gx + s, gy, gz) < 0.35) {
                        continue;
                    }
                    double fx = gx + Noise.of(gx, gy + s, gz);
                    double fy = gy + Noise.of(gx, gy, gz + s);
                    double fz = gz + Noise.of(gx + s, gy + s, gz);
                    double radius = 0.15 + 0.35 * Math.pow(Noise.of(gx - s, gy, gz + 3), 2.0);
                    double dx = px - fx;
                    double dy = py - fy;
                    double dz = pz - fz;
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz) / radius;
                    if (d < 1.0) {
                        h += (d * d - 1.0) * 0.5 * radius;
                    } else if (d < 1.6) {
                        double r = (d - 1.0) / 0.22;
                        h += 0.18 * radius * Math.exp(-r * r);
                    }
                }
            }
        }
        return h;
    }
}
