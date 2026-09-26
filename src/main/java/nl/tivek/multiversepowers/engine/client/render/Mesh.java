package nl.tivek.multiversepowers.engine.client.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

public final class Mesh {
    public final Vec3[] points;
    public final int[][] sides;
    public final Vec3[] normals;
    final Vec3[] middles;
    final double[] bright;
    final int[] edgeFrom;
    final int[] edgeTo;
    final int[] edgeLeft;
    final int[] edgeRight;
    final Vec3 middle;
    final double fine;
    public final double[] px;
    public final double[] py;
    public final double[] pz;
    final double[] nx;
    final double[] ny;
    final double[] nz;
    final double[] middleZ;
    final double boundX;
    final double boundY;
    final double boundZ;
    final double boundRadius;

    private Mesh(Vec3[] points, int[][] sides, double[] bright) {
        this.points = points;
        this.sides = sides;
        this.bright = bright;
        this.normals = new Vec3[sides.length];
        this.middles = new Vec3[sides.length];
        Map<Long, Integer> known = new HashMap<>();
        List<int[]> edges = new ArrayList<>();
        for (int s = 0; s < sides.length; s++) {
            int[] side = sides[s];
            double nx = 0.0;
            double ny = 0.0;
            double nz = 0.0;
            Vec3 sum = Vec3.ZERO;
            int corners = 0;
            for (int i = 0; i < 4; i++) {
                Vec3 a = points[side[i]];
                Vec3 b = points[side[(i + 1) % 4]];
                nx += (a.y - b.y) * (a.z + b.z);
                ny += (a.z - b.z) * (a.x + b.x);
                nz += (a.x - b.x) * (a.y + b.y);
                boolean seen = false;
                for (int j = 0; j < i; j++) {
                    seen |= side[j] == side[i];
                }
                if (!seen) {
                    sum = sum.add(a);
                    corners++;
                }
            }
            Vec3 normal = new Vec3(nx, ny, nz);
            double length = normal.length();
            this.normals[s] = length < 1.0E-12 ? Vec3.ZERO : normal.scale(1.0 / length);
            this.middles[s] = sum.scale(1.0 / corners);
            if (length < 1.0E-12) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                int a = side[i];
                int b = side[(i + 1) % 4];
                if (a == b) {
                    continue;
                }
                long key = (long) Math.min(a, b) << 32 | Math.max(a, b);
                Integer edge = known.get(key);
                if (edge == null) {
                    known.put(key, edges.size());
                    edges.add(new int[] { a, b, s, -1 });
                } else if (edges.get(edge)[3] < 0) {
                    edges.get(edge)[3] = s;
                }
            }
        }
        this.edgeFrom = new int[edges.size()];
        this.edgeTo = new int[edges.size()];
        this.edgeLeft = new int[edges.size()];
        this.edgeRight = new int[edges.size()];
        for (int e = 0; e < edges.size(); e++) {
            int[] edge = edges.get(e);
            this.edgeFrom[e] = edge[0];
            this.edgeTo[e] = edge[1];
            this.edgeLeft[e] = edge[2];
            this.edgeRight[e] = edge[3];
        }
        Vec3 all = Vec3.ZERO;
        Vec3 low = new Vec3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        Vec3 high = low.scale(-1.0);
        for (Vec3 point : points) {
            all = all.add(point);
            low = new Vec3(Math.min(low.x, point.x), Math.min(low.y, point.y), Math.min(low.z, point.z));
            high = new Vec3(Math.max(high.x, point.x), Math.max(high.y, point.y), Math.max(high.z, point.z));
        }
        this.middle = points.length == 0 ? Vec3.ZERO : all.scale(1.0 / points.length);
        this.fine = points.length == 0 ? 1.0 : ConstructPainter.fine(high.x - low.x, high.y - low.y, high.z - low.z);
        this.px = new double[points.length];
        this.py = new double[points.length];
        this.pz = new double[points.length];
        for (int i = 0; i < points.length; i++) {
            this.px[i] = points[i].x;
            this.py[i] = points[i].y;
            this.pz[i] = points[i].z;
        }
        this.nx = new double[sides.length];
        this.ny = new double[sides.length];
        this.nz = new double[sides.length];
        this.middleZ = new double[sides.length];
        for (int s = 0; s < sides.length; s++) {
            this.nx[s] = this.normals[s].x;
            this.ny[s] = this.normals[s].y;
            this.nz[s] = this.normals[s].z;
            this.middleZ[s] = this.middles[s].z;
        }
        if (points.length == 0) {
            this.boundX = 0.0;
            this.boundY = 0.0;
            this.boundZ = 0.0;
            this.boundRadius = 0.0;
        } else {
            this.boundX = (low.x + high.x) * 0.5;
            this.boundY = (low.y + high.y) * 0.5;
            this.boundZ = (low.z + high.z) * 0.5;
            this.boundRadius = high.subtract(low).length() * 0.5;
        }
    }

    public static Mesh lathe(int sides, double bright, double... profile) {
        return turning(sides, bright, false, profile);
    }

    public static Mesh ring(int sides, double bright, double... profile) {
        return turning(sides, bright, true, profile);
    }

    public static Mesh cylinder(int sides, double radius, double bottom, double top, double bright) {
        return lathe(sides, bright, 0.0, bottom, radius, bottom, radius, top, 0.0, top);
    }

    public static Mesh cone(int sides, double below, double above, double bottom, double top, double bright) {
        return lathe(sides, bright, 0.0, bottom, below, bottom, above, top, 0.0, top);
    }

    public static Mesh ball(int sides, int rings, double radius, double bright) {
        double[] profile = new double[(rings + 1) * 2];
        for (int i = 0; i <= rings; i++) {
            double angle = Math.PI * i / rings;
            profile[2 * i] = i == 0 || i == rings ? 0.0 : Math.sin(angle) * radius;
            profile[2 * i + 1] = -Math.cos(angle) * radius;
        }
        return lathe(sides, bright, profile);
    }

    public static Mesh torus(int sides, int round, double major, double minor, double bright) {
        double[] profile = new double[round * 2];
        for (int i = 0; i < round; i++) {
            double angle = Math.PI * 2.0 * i / round;
            profile[2 * i] = major + Math.cos(angle) * minor;
            profile[2 * i + 1] = Math.sin(angle) * minor;
        }
        return ring(sides, bright, profile);
    }

    public static Mesh prism(double back, double front, double bright, double... outline) {
        int n = outline.length / 2;
        Builder builder = new Builder();
        double cx = 0.0;
        double cy = 0.0;
        for (int i = 0; i < n; i++) {
            cx += outline[2 * i] / n;
            cy += outline[2 * i + 1] / n;
        }
        int[] rear = new int[n];
        int[] face = new int[n];
        for (int i = 0; i < n; i++) {
            rear[i] = builder.point(outline[2 * i], outline[2 * i + 1], back);
            face[i] = builder.point(outline[2 * i], outline[2 * i + 1], front);
        }
        int rearMiddle = builder.point(cx, cy, back);
        int faceMiddle = builder.point(cx, cy, front);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            builder.side(faceMiddle, face[i], face[j], face[j], bright);
            builder.side(rearMiddle, rear[j], rear[i], rear[i], bright);
            builder.side(rear[i], rear[j], face[j], face[i], bright);
        }
        return builder.build();
    }

    public static Mesh box(double x0, double y0, double z0, double x1, double y1, double z1, double bright) {
        return prism(z0, z1, bright, x0, y0, x1, y0, x1, y1, x0, y1);
    }

    public static Mesh lump(int sides, int rings, double radius, double rough, int seed, double bright) {
        Builder builder = new Builder();
        int[][] ring = new int[rings + 1][sides];
        for (int i = 0; i <= rings; i++) {
            double polar = Math.PI * i / rings;
            if (i == 0 || i == rings) {
                double r = radius * (1.0 + rough * (Noise.of(seed, i, 99) - 0.5));
                Arrays.fill(ring[i], builder.point(0.0, -Math.cos(polar) * r, 0.0));
                continue;
            }
            for (int j = 0; j < sides; j++) {
                double around = Math.PI * 2.0 * j / sides;
                double r = radius * (1.0 + rough * (Noise.of(seed, i, j) - 0.5) * 2.0);
                ring[i][j] = builder.point(Math.sin(polar) * Math.cos(around) * r, -Math.cos(polar) * r,
                        Math.sin(polar) * Math.sin(around) * r);
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                int k = (j + 1) % sides;
                builder.side(ring[i][j], ring[i + 1][j], ring[i + 1][k], ring[i][k], bright);
            }
        }
        return builder.build();
    }

    public static Mesh tube(boolean closed, int round, double radius, double bright, Vec3... path) {
        int n = path.length;
        Builder builder = new Builder();
        int[][] ring = new int[n][round];
        Vec3 normal = null;
        for (int i = 0; i < n; i++) {
            Vec3 before = path[closed ? (i - 1 + n) % n : Math.max(0, i - 1)];
            Vec3 after = path[closed ? (i + 1) % n : Math.min(n - 1, i + 1)];
            Vec3 along = after.subtract(before).normalize();
            if (normal == null) {
                normal = Math.abs(along.y) < 0.9 ? along.cross(new Vec3(0, 1, 0)) : along.cross(new Vec3(1, 0, 0));
            }
            normal = normal.subtract(along.scale(normal.dot(along))).normalize();
            Vec3 side = along.cross(normal);
            for (int j = 0; j < round; j++) {
                double angle = Math.PI * 2.0 * j / round;
                Vec3 at = path[i].add(normal.scale(Math.cos(angle) * radius)).add(side.scale(Math.sin(angle) * radius));
                ring[i][j] = builder.point(at.x, at.y, at.z);
            }
        }
        int pieces = closed ? n : n - 1;
        for (int i = 0; i < pieces; i++) {
            int next = (i + 1) % n;
            Vec3 axis = path[i].add(path[next]).scale(0.5);
            for (int j = 0; j < round; j++) {
                int k = (j + 1) % round;
                builder.outward(axis, bright, ring[i][j], ring[i][k], ring[next][k], ring[next][j]);
            }
        }
        if (!closed) {
            for (int end = 0; end < 2; end++) {
                int i = end == 0 ? 0 : n - 1;
                Vec3 inside = path[i].lerp(path[end == 0 ? 1 : n - 2], 0.01);
                int middle = builder.point(path[i].x, path[i].y, path[i].z);
                for (int j = 0; j < round; j++) {
                    builder.outward(inside, bright, middle, ring[i][j], ring[i][(j + 1) % round],
                            ring[i][(j + 1) % round]);
                }
            }
        }
        return builder.build();
    }

    // An open tube whose thickness changes along the path; its round starts square to the given normal and is carried
    // along from there, so a path that moves from one frame to the next does not make the tube twist.
    public static Mesh taper(int round, double bright, Vec3 normal, double[] radius, Vec3... path) {
        int n = path.length;
        Builder builder = new Builder();
        int[][] ring = new int[n][round];
        Vec3 across = normal;
        for (int i = 0; i < n; i++) {
            Vec3 along = path[Math.min(n - 1, i + 1)].subtract(path[Math.max(0, i - 1)]);
            along = along.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : along.normalize();
            Vec3 square = across.subtract(along.scale(across.dot(along)));
            if (square.lengthSqr() < 1.0E-10) {
                square = Math.abs(along.y) < 0.9 ? along.cross(new Vec3(0, 1, 0)) : along.cross(new Vec3(1, 0, 0));
            }
            across = square.normalize();
            Vec3 side = along.cross(across);
            if (radius[i] <= 1.0E-9) {
                Arrays.fill(ring[i], builder.point(path[i].x, path[i].y, path[i].z));
                continue;
            }
            for (int j = 0; j < round; j++) {
                double angle = Math.PI * 2.0 * j / round;
                Vec3 at = path[i].add(across.scale(Math.cos(angle) * radius[i]))
                        .add(side.scale(Math.sin(angle) * radius[i]));
                ring[i][j] = builder.point(at.x, at.y, at.z);
            }
        }
        for (int i = 0; i + 1 < n; i++) {
            Vec3 axis = path[i].add(path[i + 1]).scale(0.5);
            for (int j = 0; j < round; j++) {
                int k = (j + 1) % round;
                builder.outward(axis, bright, ring[i][j], ring[i][k], ring[i + 1][k], ring[i + 1][j]);
            }
        }
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : n - 1;
            if (radius[i] <= 1.0E-9 || n < 2) {
                continue;
            }
            Vec3 inside = path[i].lerp(path[end == 0 ? 1 : n - 2], 0.01);
            int middle = builder.point(path[i].x, path[i].y, path[i].z);
            for (int j = 0; j < round; j++) {
                builder.outward(inside, bright, middle, ring[i][j], ring[i][(j + 1) % round],
                        ring[i][(j + 1) % round]);
            }
        }
        return builder.build();
    }

    public static Mesh loft(int round, double bright, double[]... sections) {
        return MeshBodies.loft(round, bright, sections);
    }

    public static Mesh panel(int steps, double bright, double from, double to, double thick, double[]... sections) {
        return MeshBodies.panel(steps, bright, from, to, thick, sections);
    }

    public static Mesh wing(double span, double rootBack, double rootFront, double tipBack, double tipFront,
            double rise, double rootThick, double tipThick, double bright) {
        return MeshBodies.wing(span, rootBack, rootFront, tipBack, tipFront, rise, rootThick, tipThick, bright);
    }

    public static Mesh sweep(double bright, double[] outline, double[]... sections) {
        return MeshBodies.sweep(bright, outline, sections);
    }

    public static Mesh dish(int rings, double back, double front, double bulge, double bright, double... outline) {
        return MeshBodies.dish(rings, back, front, bulge, bright, outline);
    }

    public static Mesh merged(Mesh... parts) {
        Builder builder = new Builder();
        for (Mesh part : parts) {
            int base = builder.points.size();
            for (Vec3 point : part.points) {
                builder.point(point.x, point.y, point.z);
            }
            for (int s = 0; s < part.sides.length; s++) {
                int[] side = part.sides[s];
                builder.side(base + side[0], base + side[1], base + side[2], base + side[3], part.bright[s]);
            }
        }
        return builder.build();
    }

    private static Mesh turning(int sides, double bright, boolean loop, double[] profile) {
        int n = profile.length / 2;
        Builder builder = new Builder();
        int[][] ring = new int[n][sides];
        for (int i = 0; i < n; i++) {
            double r = profile[2 * i];
            double y = profile[2 * i + 1];
            if (r <= 1.0E-9) {
                Arrays.fill(ring[i], builder.point(0.0, y, 0.0));
                continue;
            }
            for (int j = 0; j < sides; j++) {
                double angle = Math.PI * 2.0 * j / sides;
                ring[i][j] = builder.point(Math.cos(angle) * r, y, Math.sin(angle) * r);
            }
        }
        int pieces = loop ? n : n - 1;
        for (int i = 0; i < pieces; i++) {
            int next = (i + 1) % n;
            for (int j = 0; j < sides; j++) {
                int k = (j + 1) % sides;
                builder.side(ring[i][j], ring[next][j], ring[next][k], ring[i][k], bright);
            }
        }
        return builder.build();
    }

    public Mesh moved(double x, double y, double z) {
        Vec3 by = new Vec3(x, y, z);
        Vec3[] moved = new Vec3[this.points.length];
        for (int i = 0; i < moved.length; i++) {
            moved[i] = this.points[i].add(by);
        }
        return new Mesh(moved, this.sides, this.bright);
    }

    public Mesh turned(double x, double y, double z, double degrees) {
        Vec3 axis = new Vec3(x, y, z).normalize();
        double angle = Math.toRadians(degrees);
        Vec3[] turned = new Vec3[this.points.length];
        for (int i = 0; i < turned.length; i++) {
            turned[i] = Vectors.spin(this.points[i], axis, angle);
        }
        return new Mesh(turned, this.sides, this.bright);
    }

    public Mesh scaled(double x, double y, double z) {
        Vec3[] scaled = new Vec3[this.points.length];
        for (int i = 0; i < scaled.length; i++) {
            scaled[i] = this.points[i].multiply(x, y, z);
        }
        if (x * y * z >= 0.0) {
            return new Mesh(scaled, this.sides, this.bright);
        }
        // A mirror flips winding; reverse each side so it still faces outward (see quad's culling).
        int[][] sides = new int[this.sides.length][];
        for (int s = 0; s < sides.length; s++) {
            int[] side = this.sides[s];
            sides[s] = new int[] { side[3], side[2], side[1], side[0] };
        }
        return new Mesh(scaled, sides, this.bright);
    }

    public Mesh pointing(double x, double y, double z) {
        Vec3 way = new Vec3(x, y, z).normalize();
        Vec3 axis = new Vec3(0, 1, 0).cross(way);
        if (axis.lengthSqr() < 1.0E-12) {
            return way.y > 0.0 ? this : this.turned(1.0, 0.0, 0.0, 180.0);
        }
        return this.turned(axis.x, axis.y, axis.z, Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, way.y)))));
    }

    public Mesh mirrored() {
        return this.scaled(-1.0, 1.0, 1.0);
    }

    public Mesh alongX() {
        return this.turned(0.0, 0.0, 1.0, -90.0);
    }

    public Mesh alongZ() {
        return this.turned(1.0, 0.0, 0.0, 90.0);
    }

    Mesh brighter(double factor) {
        double[] bright = new double[this.bright.length];
        for (int s = 0; s < bright.length; s++) {
            bright[s] = this.bright[s] * factor;
        }
        return new Mesh(this.points, this.sides, bright);
    }

    static final class Builder {
        private final List<Vec3> points = new ArrayList<>();
        private final List<int[]> sides = new ArrayList<>();
        private final List<Double> bright = new ArrayList<>();

        int point(double x, double y, double z) {
            this.points.add(new Vec3(x, y, z));
            return this.points.size() - 1;
        }

        void side(int a, int b, int c, int d, double brightness) {
            this.sides.add(new int[] { a, b, c, d });
            this.bright.add(brightness);
        }

        void outward(Vec3 inside, double brightness, int a, int b, int c, int d) {
            Vec3 pa = this.points.get(a);
            Vec3 pb = this.points.get(b);
            Vec3 pc = this.points.get(c);
            Vec3 pd = this.points.get(d);
            Vec3 normal = pc.subtract(pa).cross(pd.subtract(pb));
            Vec3 middle = pa.add(pb).add(pc).add(pd).scale(0.25);
            if (normal.dot(middle.subtract(inside)) >= 0.0) {
                this.side(a, b, c, d, brightness);
            } else {
                this.side(d, c, b, a, brightness);
            }
        }

        Mesh build() {
            double[] bright = new double[this.bright.size()];
            for (int i = 0; i < bright.length; i++) {
                bright[i] = this.bright.get(i);
            }
            return new Mesh(this.points.toArray(Vec3[]::new), this.sides.toArray(int[][]::new), bright);
        }
    }
}
