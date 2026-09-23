package nl.tivek.welcomescreen.client.character.lantern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

/**
 * A solid shape of flat sides, for the parts of a construct that are round or slanted where boxes will not do: a
 * drum, a bell, a coin, a ring, a blade. It is measured in blocks at scale 1 (x to the right, y up, z ahead) like the
 * box models (see {@link ConstructPainter}), and drawn the same way: solid sides lit from above, and bright lines only
 * where its outline runs as you look at it. Every side goes round its outward face counter-clockwise. A mesh never
 * changes: moving, turning or stretching one gives a new one.
 */
final class Mesh {
    /** Its corners. */
    final Vec3[] points;
    /** Its sides, four corners each (one with three corners repeats its last), round the outward face. */
    final int[][] sides;
    /** The way each side faces, outwards and one long; zero for a side without area. */
    final Vec3[] normals;
    /** The middle of each side. */
    final Vec3[] middles;
    /** How brightly each side burns, next to the rest of the construct. */
    final double[] bright;
    /** Every edge once: its two ends, and the side on either side of it (-1 when there is none). */
    final int[] edgeFrom;
    final int[] edgeTo;
    final int[] edgeLeft;
    final int[] edgeRight;
    /** The middle of all its corners: where it flies out from when the construct breaks up. */
    final Vec3 middle;
    /** How thick the lines along it are drawn, as a part of the usual (see {@link ConstructPainter#fine}). */
    final double fine;

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
    }

    // ---- Shapes ----

    /**
     * A shape turned round the y axis, the way wood is turned on a lathe: {@code profile} gives pairs of how far out
     * and how high, with the outside on its right as it runs. Starting and ending on the axis (a radius of 0) closes it
     * there, so a profile from the axis at the bottom, out, up and back in to the axis at the top makes a solid thing.
     *
     * @param sides how many sides it has all round
     */
    static Mesh lathe(int sides, double bright, double... profile) {
        return turning(sides, bright, false, profile);
    }

    /**
     * A ring turned round the y axis: like {@link #lathe}, but the profile goes all the way round (counter-clockwise,
     * with x out to the right and y up) and never touches the axis, so there is a hole through the middle. A washer,
     * a tyre, a hoop.
     */
    static Mesh ring(int sides, double bright, double... profile) {
        return turning(sides, bright, true, profile);
    }

    /** A cylinder standing on y = {@code bottom} round the y axis. */
    static Mesh cylinder(int sides, double radius, double bottom, double top, double bright) {
        return lathe(sides, bright, 0.0, bottom, radius, bottom, radius, top, 0.0, top);
    }

    /** A cone or a cut-off cone round the y axis, {@code below} wide at the bottom and {@code above} at the top. */
    static Mesh cone(int sides, double below, double above, double bottom, double top, double bright) {
        return lathe(sides, bright, 0.0, bottom, below, bottom, above, top, 0.0, top);
    }

    /** A ball round the middle. */
    static Mesh ball(int sides, int rings, double radius, double bright) {
        double[] profile = new double[(rings + 1) * 2];
        for (int i = 0; i <= rings; i++) {
            double angle = Math.PI * i / rings;
            profile[2 * i] = i == 0 || i == rings ? 0.0 : Math.sin(angle) * radius;
            profile[2 * i + 1] = -Math.cos(angle) * radius;
        }
        return lathe(sides, bright, profile);
    }

    /**
     * A ring like a doughnut, lying flat round the y axis: {@code major} out to the middle of its body, which is
     * {@code minor} thick either way.
     */
    static Mesh torus(int sides, int round, double major, double minor, double bright) {
        double[] profile = new double[round * 2];
        for (int i = 0; i < round; i++) {
            double angle = Math.PI * 2.0 * i / round;
            profile[2 * i] = major + Math.cos(angle) * minor;
            profile[2 * i + 1] = Math.sin(angle) * minor;
        }
        return ring(sides, bright, profile);
    }

    /**
     * A flat outline stood out along z, from {@code back} to {@code front}: a blade, a fin, a wedge. The outline gives
     * pairs of x and y counter-clockwise as seen from the front, and must be seen whole from its own middle.
     */
    static Mesh prism(double back, double front, double bright, double... outline) {
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

    /** A box, as a mesh: for a box that has to be turned inside a shape. */
    static Mesh box(double x0, double y0, double z0, double x1, double y1, double z1, double bright) {
        return prism(z0, z1, bright, x0, y0, x1, y0, x1, y1, x0, y1);
    }

    /**
     * A rough lump round the middle, like a rock: a ball whose corners stick out or sink in by up to {@code rough}
     * times its radius, the same way every time for the same {@code seed}.
     */
    static Mesh lump(int sides, int rings, double radius, double rough, int seed, double bright) {
        Builder builder = new Builder();
        int[][] ring = new int[rings + 1][sides];
        for (int i = 0; i <= rings; i++) {
            double polar = Math.PI * i / rings;
            if (i == 0 || i == rings) {
                double r = radius * (1.0 + rough * (ConstructPainter.noise(seed, i, 99) - 0.5));
                Arrays.fill(ring[i], builder.point(0.0, -Math.cos(polar) * r, 0.0));
                continue;
            }
            for (int j = 0; j < sides; j++) {
                double around = Math.PI * 2.0 * j / sides;
                double r = radius * (1.0 + rough * (ConstructPainter.noise(seed, i, j) - 0.5) * 2.0);
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

    /**
     * A round tube {@code radius} thick along a path of points: a chain link, the arm of an anchor, a spring, a wire.
     * An open path gets its ends closed; a closed one runs from its last point back to its first, like a ring.
     *
     * @param round how many sides it has round its body
     */
    static Mesh tube(boolean closed, int round, double radius, double bright, Vec3... path) {
        int n = path.length;
        Builder builder = new Builder();
        int[][] ring = new int[n][round];
        Vec3 normal = null;
        for (int i = 0; i < n; i++) {
            Vec3 before = path[closed ? (i - 1 + n) % n : Math.max(0, i - 1)];
            Vec3 after = path[closed ? (i + 1) % n : Math.min(n - 1, i + 1)];
            Vec3 along = after.subtract(before).normalize();
            // Carry the way round the body on from point to point, so the tube does not twist.
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

    // ---- Changes ----

    /** The same shape, moved. */
    Mesh moved(double x, double y, double z) {
        Vec3 by = new Vec3(x, y, z);
        Vec3[] moved = new Vec3[this.points.length];
        for (int i = 0; i < moved.length; i++) {
            moved[i] = this.points[i].add(by);
        }
        return new Mesh(moved, this.sides, this.bright);
    }

    /** The same shape, turned by {@code degrees} about the line through the origin along (x, y, z). */
    Mesh turned(double x, double y, double z, double degrees) {
        Vec3 axis = new Vec3(x, y, z).normalize();
        double angle = Math.toRadians(degrees);
        Vec3[] turned = new Vec3[this.points.length];
        for (int i = 0; i < turned.length; i++) {
            turned[i] = ConstructPainter.spin(this.points[i], axis, angle);
        }
        return new Mesh(turned, this.sides, this.bright);
    }

    /** The same shape, stretched (or squashed) along x, y and z; mirrored when an odd number of them is negative. */
    Mesh scaled(double x, double y, double z) {
        Vec3[] scaled = new Vec3[this.points.length];
        for (int i = 0; i < scaled.length; i++) {
            scaled[i] = this.points[i].multiply(x, y, z);
        }
        if (x * y * z >= 0.0) {
            return new Mesh(scaled, this.sides, this.bright);
        }
        // Mirrored, every side goes round the other way: turned back, so they still face outwards.
        int[][] sides = new int[this.sides.length][];
        for (int s = 0; s < sides.length; s++) {
            int[] side = this.sides[s];
            sides[s] = new int[] { side[3], side[2], side[1], side[0] };
        }
        return new Mesh(scaled, sides, this.bright);
    }

    /** The same shape, turned so that what stood up along y points along (x, y, z). */
    Mesh pointing(double x, double y, double z) {
        Vec3 way = new Vec3(x, y, z).normalize();
        Vec3 axis = new Vec3(0, 1, 0).cross(way);
        if (axis.lengthSqr() < 1.0E-12) {
            return way.y > 0.0 ? this : this.turned(1.0, 0.0, 0.0, 180.0);
        }
        return this.turned(axis.x, axis.y, axis.z, Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, way.y)))));
    }

    /** The same shape, lying along x: what stood up along y now points along +x. */
    Mesh alongX() {
        return this.turned(0.0, 0.0, 1.0, -90.0);
    }

    /** The same shape, lying along z: what stood up along y now points along +z. */
    Mesh alongZ() {
        return this.turned(1.0, 0.0, 0.0, 90.0);
    }

    /** The same shape, burning this many times as brightly. */
    Mesh brighter(double factor) {
        double[] bright = new double[this.bright.length];
        for (int s = 0; s < bright.length; s++) {
            bright[s] = this.bright[s] * factor;
        }
        return new Mesh(this.points, this.sides, bright);
    }

    /** Puts a shape together side by side. */
    private static final class Builder {
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

        /** A side of these corners in either order: the one that faces away from {@code inside}. */
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
