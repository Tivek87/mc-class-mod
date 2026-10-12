package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.stream.IntStream;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Perlin;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Kind;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Planet;

// Pixel art at the moon's pixel size: each pixel is either the globe (or its ring), solid, or the glow of its air,
// which only adds; colours premultiplied, in a few shades.
final class PlanetPainter {
    private static final int SAMPLES = 4;
    private static final double SUN = 1.18;
    private static final double BUMP = 0.12;

    private PlanetPainter() {
    }

    static NativeImage paint(Planet planet) {
        int size = SkyTexture.pixels(planet.degrees() * planet.reach());
        NativeImage image = new NativeImage(size, size, false);
        Body body = new Body(planet);
        IntStream.range(0, size).parallel().forEach(row -> {
            double[] surface = new double[Surfaces.OUT];
            double[] solid = new double[3];
            double[] glow = new double[3];
            double[] one = new double[4];
            for (int col = 0; col < size; col++) {
                java.util.Arrays.fill(solid, 0.0);
                java.util.Arrays.fill(glow, 0.0);
                int covered = 0;
                for (int a = 0; a < SAMPLES; a++) {
                    for (int b = 0; b < SAMPLES; b++) {
                        double sx = ((col + (a + 0.5) / SAMPLES) / size * 2.0 - 1.0) * body.reach;
                        double sy = (1.0 - (row + (b + 0.5) / SAMPLES) / size * 2.0) * body.reach;
                        body.sample(sx, sy, surface, one);
                        double[] into = one[3] >= 0.5 ? solid : glow;
                        double cover = one[3] >= 0.5 ? one[3] : 1.0;
                        covered += one[3] >= 0.5 ? 1 : 0;
                        for (int c = 0; c < 3; c++) {
                            into[c] += one[c] / cover;
                        }
                    }
                }
                int n = SAMPLES * SAMPLES;
                boolean globe = covered * 2 >= n;
                double[] rgb = globe ? solid : glow;
                double count = globe ? covered : n - covered;
                image.setPixelRGBA(col, row, SkyColors.premultiplied(shade(rgb[0] / count, col, row),
                        shade(rgb[1] / count, col, row), shade(rgb[2] / count, col, row), globe ? 1.0 : 0.0));
            }
        });
        return image;
    }

    private static double shade(double v, int col, int row) {
        return SkyColors.pixel(SkyColors.soft(v), col, row, 0.0);
    }

    private static final class Body {
        final Planet planet;
        final Kind kind;
        final double reach;
        final Vec3 light;
        final Vec3 pole;
        final Vec3 ax;
        final Vec3 az;
        final double air;
        final double airR;
        final double airG;
        final double airB;

        Body(Planet planet) {
            this.planet = planet;
            this.kind = planet.kind();
            this.reach = planet.reach();
            this.light = SkyBodies.light(planet.dir());
            double r = planet.roll();
            double t = planet.tilt();
            this.pole = new Vec3(-Math.sin(r) * Math.cos(t), Math.cos(r) * Math.cos(t), Math.sin(t));
            Vec3 east = this.pole.cross(new Vec3(0.0, 0.0, 1.0)).normalize();
            Vec3 front = east.cross(this.pole);
            double spin = front(planet) - Math.PI / 2.0;
            double c = Math.cos(spin);
            double s = Math.sin(spin);
            this.ax = east.scale(c).subtract(front.scale(s));
            this.az = east.scale(s).add(front.scale(c));
            this.air = Surfaces.air(this.kind);
            int rgb = Surfaces.airColor(this.kind);
            this.airR = (rgb >> 16 & 255) / 255.0;
            this.airG = (rgb >> 8 & 255) / 255.0;
            this.airB = (rgb & 255) / 255.0;
        }

        private static double front(Planet planet) {
            return switch (planet.kind()) {
                case GAS -> 0.9 - 0.35;
                case ICE -> 2.2 + 0.25;
                default -> (planet.seed() * 0.731) % (Math.PI * 2.0);
            };
        }

        void sample(double sx, double sy, double[] surface, double[] out) {
            double r2 = sx * sx + sy * sy;
            double r = Math.sqrt(r2);
            double cr = 0.0;
            double cg = 0.0;
            double cb = 0.0;
            double ca = 0.0;
            double sphereZ = -1.0E9;
            if (r2 < 1.0) {
                sphereZ = Math.sqrt(1.0 - r2);
                double[] lit = this.globe(sx, sy, sphereZ, surface);
                cr = lit[0];
                cg = lit[1];
                cb = lit[2];
                ca = 1.0;
            } else if (this.air > 0.0) {
                double width = 0.045;
                double fall = Math.exp(-(r - 1.0) / width) - Math.exp(-(this.reach - 1.0) / width);
                double facing = (sx * this.light.x + sy * this.light.y) / r;
                double lit = 0.12 + 0.88 * Surfaces.smooth(-0.35, 0.55, facing + this.light.z * 0.4);
                double glow = Math.max(0.0, fall) * this.air * lit * 0.85;
                cr = this.airR * glow;
                cg = this.airG * glow;
                cb = this.airB * glow;
            }
            if (this.planet.ringed() && Math.abs(this.pole.z) > 1.0E-3) {
                double rz = -(this.pole.x * sx + this.pole.y * sy) / this.pole.z;
                if (rz > sphereZ) {
                    double[] ring = this.ring(sx, sy, rz);
                    if (ring != null) {
                        double alpha = ring[3];
                        cr = ring[0] * alpha + cr * (1.0 - alpha);
                        cg = ring[1] * alpha + cg * (1.0 - alpha);
                        cb = ring[2] * alpha + cb * (1.0 - alpha);
                        ca = alpha + ca * (1.0 - alpha);
                    }
                }
            }
            out[0] = cr;
            out[1] = cg;
            out[2] = cb;
            out[3] = ca;
        }

        private double[] globe(double nx, double ny, double nz, double[] surface) {
            double bx = nx * this.ax.x + ny * this.ax.y + nz * this.ax.z;
            double by = nx * this.pole.x + ny * this.pole.y + nz * this.pole.z;
            double bz = nx * this.az.x + ny * this.az.y + nz * this.az.z;
            Surfaces.paint(this.kind, this.planet.seed(), bx, by, bz, surface);
            double sx = nx;
            double sy = ny;
            double sz = nz;
            if (Surfaces.bumpy(this.kind)) {
                double e = 0.004;
                long seed = this.planet.seed();
                double gx = (Surfaces.height(this.kind, seed, bx + e, by, bz)
                        - Surfaces.height(this.kind, seed, bx - e, by, bz)) / (2.0 * e);
                double gy = (Surfaces.height(this.kind, seed, bx, by + e, bz)
                        - Surfaces.height(this.kind, seed, bx, by - e, bz)) / (2.0 * e);
                double gz = (Surfaces.height(this.kind, seed, bx, by, bz + e)
                        - Surfaces.height(this.kind, seed, bx, by, bz - e)) / (2.0 * e);
                double along = gx * bx + gy * by + gz * bz;
                gx -= along * bx;
                gy -= along * by;
                gz -= along * bz;
                double ix = gx * this.ax.x + gy * this.pole.x + gz * this.az.x;
                double iy = gx * this.ax.y + gy * this.pole.y + gz * this.az.y;
                double iz = gx * this.ax.z + gy * this.pole.z + gz * this.az.z;
                sx -= ix * BUMP;
                sy -= iy * BUMP;
                sz -= iz * BUMP;
                double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
                sx /= len;
                sy /= len;
                sz /= len;
            }
            Vec3 l = this.light;
            double flat = nx * l.x + ny * l.y + nz * l.z;
            double ndl = sx * l.x + sy * l.y + sz * l.z;
            boolean gas = this.kind == Kind.GAS || this.kind == Kind.SATURN || this.kind == Kind.ICE
                    || this.kind == Kind.TEAL || this.kind == Kind.VIOLET;
            double soft = gas ? 0.12 : 0.05;
            double day = Surfaces.smooth(-soft, 1.0, ndl) * Surfaces.smooth(-0.18, 0.05, flat);
            day = Math.pow(day, gas ? 0.8 : 0.9) * SUN;
            day *= this.ringShadow(nx, ny, nz);
            double limb = gas ? Math.pow(nz, 0.32) : Math.pow(nz, 0.12);
            double night = 0.035;
            double r = surface[Surfaces.R] * (day * limb + night * 0.6);
            double g = surface[Surfaces.G] * (day * limb + night * 0.75);
            double b = surface[Surfaces.B] * (day * limb + night);
            double dusk = Math.exp(-Math.pow((flat - 0.04) / 0.10, 2.0)) * this.air;
            r += dusk * 0.18 * surface[Surfaces.R];
            g += dusk * 0.05 * surface[Surfaces.G];
            if (surface[Surfaces.SHINE] > 0.0 && day > 0.0) {
                double hx = l.x;
                double hy = l.y;
                double hz = l.z + 1.0;
                double hl = Math.sqrt(hx * hx + hy * hy + hz * hz);
                double ndh = Math.max(0.0, (nx * hx + ny * hy + nz * hz) / hl);
                double spec = Math.pow(ndh, 48.0) * 0.55 * surface[Surfaces.SHINE] * Math.min(1.0, day);
                r += spec * 0.95;
                g += spec * 0.92;
                b += spec * 0.85;
            }
            double dark = 1.0 - Surfaces.smooth(-0.12, 0.12, flat);
            r += surface[Surfaces.GLOW_R] * dark;
            g += surface[Surfaces.GLOW_G] * dark;
            b += surface[Surfaces.GLOW_B] * dark;
            if (this.kind == Kind.LAVA) {
                r += surface[Surfaces.GLOW_R] * 0.5 * (1.0 - dark);
                g += surface[Surfaces.GLOW_G] * 0.5 * (1.0 - dark);
                b += surface[Surfaces.GLOW_B] * 0.5 * (1.0 - dark);
            }
            if (this.air > 0.0) {
                double rim = Math.pow(1.0 - nz, 2.6) * this.air;
                double lit = Surfaces.smooth(-0.3, 0.45, flat);
                r += this.airR * rim * lit * 0.9;
                g += this.airG * rim * lit * 0.9;
                b += this.airB * rim * lit * 0.9;
            }
            return new double[] {r, g, b};
        }

        private double ringShadow(double nx, double ny, double nz) {
            if (!this.planet.ringed()) {
                return 1.0;
            }
            Vec3 l = this.light;
            double denom = this.pole.x * l.x + this.pole.y * l.y + this.pole.z * l.z;
            if (Math.abs(denom) < 1.0E-4) {
                return 1.0;
            }
            double t = -(this.pole.x * nx + this.pole.y * ny + this.pole.z * nz) / denom;
            if (t <= 0.0) {
                return 1.0;
            }
            double qx = nx + l.x * t;
            double qy = ny + l.y * t;
            double qz = nz + l.z * t;
            double rho = Math.sqrt(qx * qx + qy * qy + qz * qz);
            return 1.0 - 0.85 * this.ringAlpha(rho);
        }

        private double[] ring(double sx, double sy, double sz) {
            double rho = Math.sqrt(sx * sx + sy * sy + sz * sz);
            double alpha = this.ringAlpha(rho);
            if (alpha <= 0.0) {
                return null;
            }
            double u = (rho - this.planet.ringIn()) / (this.planet.ringOut() - this.planet.ringIn());
            double r;
            double g;
            double b;
            if (this.kind == Kind.SATURN) {
                r = Surfaces.mix(0.58, 0.92, Surfaces.smooth(0.05, 0.4, u));
                g = Surfaces.mix(0.52, 0.83, Surfaces.smooth(0.05, 0.4, u));
                b = Surfaces.mix(0.46, 0.66, Surfaces.smooth(0.05, 0.4, u));
                double outer = Surfaces.smooth(0.6, 0.75, u);
                r = Surfaces.mix(r, 0.80, outer);
                g = Surfaces.mix(g, 0.72, outer);
                b = Surfaces.mix(b, 0.60, outer);
            } else {
                r = 0.62;
                g = 0.80;
                b = 0.82;
            }
            double streak = 0.85 + 0.15 * Perlin.noise(rho * 90.0, this.planet.seed() * 0.37, 0.5);
            Vec3 l = this.light;
            double sun = this.pole.x * l.x + this.pole.y * l.y + this.pole.z * l.z;
            double bright = 0.35 + 0.65 * Math.sqrt(Math.abs(sun));
            if (Math.signum(sun) != Math.signum(this.pole.z)) {
                bright *= 0.45;
            }
            double bq = sx * l.x + sy * l.y + sz * l.z;
            double cq = rho * rho - 1.0;
            double disc = bq * bq - cq;
            if (disc > 0.0 && -bq + Math.sqrt(disc) > 0.0) {
                bright *= 0.06;
            }
            bright *= streak * SUN;
            return new double[] {r * bright, g * bright, b * bright, alpha};
        }

        private double ringAlpha(double rho) {
            double in = this.planet.ringIn();
            double out = this.planet.ringOut();
            if (rho < in || rho > out) {
                return 0.0;
            }
            double u = (rho - in) / (out - in);
            double fine = 0.75 + 0.25 * Perlin.noise(rho * 140.0, this.planet.seed() * 0.53, 0.25);
            double a;
            if (this.kind == Kind.SATURN) {
                if (u < 0.2) {
                    a = 0.18 + 0.12 * u / 0.2;
                } else if (u < 0.56) {
                    a = 0.88;
                } else if (u < 0.62) {
                    a = 0.04;
                } else if (u < 0.95) {
                    a = Math.abs(u - 0.875) < 0.008 ? 0.05 : 0.62;
                } else {
                    a = Math.abs(u - 0.985) < 0.006 ? 0.5 : 0.0;
                }
            } else {
                double bands = 0.5 + 0.5 * Math.sin(u * 30.0 + Math.sin(u * 7.0) * 2.0);
                a = Surfaces.smooth(0.3, 0.9, bands) * 0.5 + 0.22;
                a *= Surfaces.smooth(0.0, 0.08, u) * Surfaces.smooth(1.0, 0.9, u);
            }
            double edge = Surfaces.smooth(0.0, 0.01, u) * Surfaces.smooth(1.0, 0.99, u);
            return Math.max(0.0, Math.min(1.0, a * fine * edge));
        }
    }
}
