package nl.tivek.multiversepowers.sky.client;

import java.util.SplittableRandom;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.sky.client.SkyBodies.Cluster;

// The stars that belong to the Milky Way and the clusters, square as the game's own (whose stars stay as they are),
// each turned its own way, a cluster's brightest a cross of five squares.
final class StarField {
    // Per star corner: x, y, z, red, green, blue.
    static final int STRIDE = 6;
    private static final int BAND = 1800;

    private StarField() {
    }

    static float[] build() {
        SplittableRandom random = new SplittableRandom(0x57A25L);
        StarList list = new StarList(BAND + 600);
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
        for (Cluster cluster : SkyBodies.CLUSTERS) {
            cluster(list, random, cluster);
        }
        return list.done();
    }

    private static void faint(StarList list, SplittableRandom random, Vec3 d) {
        double b = Math.pow(random.nextDouble(), 3.0);
        list.add(d, 0.13 + 0.09 * b, random, color(random), 0.3 + 0.6 * b);
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
                list.add(d, 0.11 + 0.04 * random.nextDouble(), random, tint, 0.5 + 0.5 * random.nextDouble());
            } else if (i < 3) {
                list.cross(d, 0.2 + 0.06 * random.nextDouble(), random, tint, 0.95);
            } else if (i < 7) {
                list.add(d, 0.24 + 0.08 * random.nextDouble(), random, tint, 0.9);
            } else {
                list.add(d, 0.13 + 0.07 * random.nextDouble(), random, tint, 0.4 + 0.4 * random.nextDouble());
            }
        }
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

        void add(Vec3 dir, double half, SplittableRandom random, double[] color, double light) {
            Vec3[] axes = turned(dir, random.nextDouble(Math.PI * 2.0));
            this.square(dir.scale(100.0), axes[0].scale(half), axes[1].scale(half), color, light);
        }

        // A plus of five squares, its arms a little dimmer.
        void cross(Vec3 dir, double half, SplittableRandom random, double[] color, double light) {
            Vec3[] axes = turned(dir, random.nextDouble(Math.PI * 2.0));
            Vec3 middle = dir.scale(100.0);
            Vec3 right = axes[0].scale(half);
            Vec3 up = axes[1].scale(half);
            this.square(middle, right, up, color, light);
            for (Vec3 arm : new Vec3[] {right, up, right.scale(-1.0), up.scale(-1.0)}) {
                this.square(middle.add(arm.scale(2.0)), right, up, color, light * 0.5);
            }
        }

        private static Vec3[] turned(Vec3 dir, double roll) {
            Vec3[] frame = SkyBodies.frame(dir);
            double c = Math.cos(roll);
            double s = Math.sin(roll);
            return new Vec3[] {frame[0].scale(c).add(frame[1].scale(s)), frame[1].scale(c).subtract(frame[0].scale(s))};
        }

        private void square(Vec3 middle, Vec3 right, Vec3 up, double[] color, double light) {
            if (this.at + 4 * STRIDE > this.data.length) {
                this.data = java.util.Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.corner(middle.subtract(right).add(up), color, light);
            this.corner(middle.add(right).add(up), color, light);
            this.corner(middle.add(right).subtract(up), color, light);
            this.corner(middle.subtract(right).subtract(up), color, light);
        }

        private void corner(Vec3 p, double[] color, double light) {
            float[] d = this.data;
            d[this.at++] = (float) p.x;
            d[this.at++] = (float) p.y;
            d[this.at++] = (float) p.z;
            d[this.at++] = (float) (color[0] * light);
            d[this.at++] = (float) (color[1] * light);
            d[this.at++] = (float) (color[2] * light);
        }

        float[] done() {
            return java.util.Arrays.copyOf(this.data, this.at);
        }
    }
}
