package nl.tivek.multiversepowers.sky.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.SplittableRandom;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Cluster;

final class StarField {
    static final int SPRITE = 128;
    // Per star corner: x, y, z, u, v, red, green, blue.
    static final int STRIDE = 8;
    private static final int FIELD = 7000;
    private static final int BAND = 4500;
    private static final int GLOWING = 220;
    private static final int SPIKED = 46;

    private StarField() {
    }

    static NativeImage sprites() {
        NativeImage image = new NativeImage(SPRITE * 2, SPRITE, false);
        for (int y = 0; y < SPRITE; y++) {
            for (int x = 0; x < SPRITE * 2; x++) {
                boolean spiked = x >= SPRITE;
                double u = ((x % SPRITE) + 0.5) / SPRITE * 2.0 - 1.0;
                double v = (y + 0.5) / SPRITE * 2.0 - 1.0;
                double r = Math.sqrt(u * u + v * v);
                double light;
                if (spiked) {
                    light = Math.exp(-r * r / 0.0025) + 0.45 * Math.exp(-r * r / 0.0144)
                            + 0.12 * Math.exp(-r * r / 0.12);
                    double au = Math.abs(u);
                    double av = Math.abs(v);
                    light += 0.75 * Math.exp(-av / 0.009) * Math.pow(Math.max(0.0, 1.0 - au), 3.0);
                    light += 0.75 * Math.exp(-au / 0.009) * Math.pow(Math.max(0.0, 1.0 - av), 3.0);
                } else {
                    light = Math.exp(-r * r / 0.0625) + 0.2 * Math.exp(-r * r / 0.3);
                }
                if (!spiked) {
                    light *= Surfaces.smooth(1.0, 0.82, r);
                } else {
                    light *= Surfaces.smooth(1.0, 0.94, Math.max(Math.abs(u), Math.abs(v)));
                }
                double l = Math.min(1.0, light);
                int c = SkyColors.channel(l);
                image.setPixelRGBA(x, y, 255 << 24 | c << 16 | c << 8 | c);
            }
        }
        return image;
    }

    static float[] build() {
        SplittableRandom random = new SplittableRandom(0x57A25L);
        StarList list = new StarList(FIELD + BAND + GLOWING + SPIKED + 1000);
        for (int i = 0; i < FIELD; i++) {
            Vec3 d = anywhere(random);
            faint(list, random, d);
        }
        Vec3 n = SkyBodies.BAND;
        Vec3 c = SkyBodies.CORE.subtract(n.scale(SkyBodies.CORE.dot(n))).normalize();
        Vec3 s = n.cross(c);
        for (int i = 0; i < BAND; i++) {
            double lon = random.nextDouble() < 0.5 ? random.nextGaussian() * 0.9 : random.nextDouble(-Math.PI,
                    Math.PI);
            double lat = random.nextGaussian() * 0.09;
            Vec3 d = c.scale(Math.cos(lon)).add(s.scale(Math.sin(lon))).scale(Math.cos(lat)).add(n.scale(
                    Math.sin(lat))).normalize();
            faint(list, random, d);
        }
        for (int i = 0; i < GLOWING; i++) {
            Vec3 d = anywhere(random);
            double b = random.nextDouble();
            double[] color = color(random);
            double light = 0.6 + 0.4 * b;
            list.add(d, 0.7 + 0.8 * b, color, light, false);
        }
        for (int i = 0; i < SPIKED; i++) {
            Vec3 d = anywhere(random);
            double b = Math.pow(random.nextDouble(), 1.5);
            double[] color = color(random);
            list.add(d, 2.4 + 3.2 * b, color, 0.75 + 0.25 * b, true);
        }
        for (Cluster cluster : SkyBodies.CLUSTERS) {
            cluster(list, random, cluster);
        }
        return list.done();
    }

    private static void faint(StarList list, SplittableRandom random, Vec3 d) {
        double b = Math.pow(random.nextDouble(), 4.0);
        list.add(d, 0.32 + 0.4 * b, color(random), 0.28 + 0.72 * b, false);
    }

    private static void cluster(StarList list, SplittableRandom random, Cluster cluster) {
        Vec3[] frame = SkyBodies.frame(cluster.dir());
        double spread = Math.tan(Math.toRadians(cluster.degrees() * 0.5));
        double[] tint = {SkyColors.red(cluster.color()), SkyColors.green(cluster.color()),
            SkyColors.blue(cluster.color())};
        for (int i = 0; i < cluster.count(); i++) {
            double sigma = cluster.globular() ? 0.35 : 0.55;
            double x = random.nextGaussian() * sigma * spread;
            double y = random.nextGaussian() * sigma * spread;
            Vec3 d = cluster.dir().add(frame[0].scale(x)).add(frame[1].scale(y)).normalize();
            if (cluster.globular()) {
                list.add(d, 0.3 + 0.18 * random.nextDouble(), tint, 0.55 + 0.45 * random.nextDouble(), false);
            } else if (i < 7) {
                list.add(d, 1.3 + 1.2 * random.nextDouble(), tint, 0.9, i < 3);
            } else {
                list.add(d, 0.35 + 0.3 * random.nextDouble(), tint, 0.4 + 0.4 * random.nextDouble(), false);
            }
        }
    }

    private static Vec3 anywhere(SplittableRandom random) {
        double y = random.nextDouble(-1.0, 1.0);
        double a = random.nextDouble(Math.PI * 2.0);
        double flat = Math.sqrt(1.0 - y * y);
        return new Vec3(Math.cos(a) * flat, y, Math.sin(a) * flat);
    }

    private static double[] color(SplittableRandom random) {
        double t = random.nextDouble();
        if (t < 0.24) {
            return new double[] {0.66, 0.78, 1.0};
        } else if (t < 0.58) {
            return new double[] {0.95, 0.97, 1.0};
        } else if (t < 0.80) {
            return new double[] {1.0, 0.93, 0.80};
        } else if (t < 0.94) {
            return new double[] {1.0, 0.76, 0.52};
        }
        return new double[] {1.0, 0.58, 0.45};
    }

    private static final class StarList {
        private float[] data;
        private int at;

        StarList(int stars) {
            this.data = new float[stars * 4 * STRIDE];
        }

        void add(Vec3 dir, double half, double[] color, double light, boolean spiked) {
            if (this.at + 4 * STRIDE > this.data.length) {
                this.data = java.util.Arrays.copyOf(this.data, this.data.length * 2);
            }
            Vec3[] frame = SkyBodies.frame(dir);
            Vec3 middle = dir.scale(100.0);
            Vec3 right = frame[0].scale(half);
            Vec3 up = frame[1].scale(half);
            float u0 = spiked ? 0.5F : 0.0F;
            float u1 = spiked ? 1.0F : 0.5F;
            this.corner(middle.subtract(right).add(up), u0, 0.0F, color, light);
            this.corner(middle.add(right).add(up), u1, 0.0F, color, light);
            this.corner(middle.add(right).subtract(up), u1, 1.0F, color, light);
            this.corner(middle.subtract(right).subtract(up), u0, 1.0F, color, light);
        }

        private void corner(Vec3 p, float u, float v, double[] color, double light) {
            float[] d = this.data;
            d[this.at++] = (float) p.x;
            d[this.at++] = (float) p.y;
            d[this.at++] = (float) p.z;
            d[this.at++] = u;
            d[this.at++] = v;
            d[this.at++] = (float) (color[0] * light);
            d[this.at++] = (float) (color[1] * light);
            d[this.at++] = (float) (color[2] * light);
        }

        float[] done() {
            return java.util.Arrays.copyOf(this.data, this.at);
        }
    }
}
