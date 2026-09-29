package nl.tivek.multiversepowers.engine.client.render.mesh;

import java.util.Arrays;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh.Builder;

final class MeshBodies {
    private MeshBodies() {
    }

    static Mesh loft(int round, double bright, double[]... sections) {
        int n = sections.length;
        Builder builder = new Builder();
        int[][] ring = new int[n][round];
        boolean[] point = new boolean[n];
        for (int i = 0; i < n; i++) {
            double[] section = sections[i];
            point[i] = section[1] <= 1.0E-9 && section[2] <= 1.0E-9;
            if (point[i]) {
                Arrays.fill(ring[i], builder.point(0.0, section[3], section[0]));
                continue;
            }
            for (int j = 0; j < round; j++) {
                double angle = Math.PI * 2.0 * j / round;
                ring[i][j] = builder.point(Math.cos(angle) * section[1], section[3] + Math.sin(angle) * section[2],
                        section[0]);
            }
        }
        for (int i = 0; i + 1 < n; i++) {
            Vec3 axis = new Vec3(0.0, (sections[i][3] + sections[i + 1][3]) * 0.5,
                    (sections[i][0] + sections[i + 1][0]) * 0.5);
            for (int j = 0; j < round; j++) {
                int k = (j + 1) % round;
                builder.outward(axis, bright, ring[i][j], ring[i][k], ring[i + 1][k], ring[i + 1][j]);
            }
        }
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : n - 1;
            if (point[i] || n < 2) {
                continue;
            }
            int other = end == 0 ? 1 : n - 2;
            Vec3 inside = new Vec3(0.0, sections[i][3], sections[i][0] + (sections[other][0] - sections[i][0]) * 0.01);
            int middle = builder.point(0.0, sections[i][3], sections[i][0]);
            for (int j = 0; j < round; j++) {
                int k = (j + 1) % round;
                builder.outward(inside, bright, middle, ring[i][j], ring[i][k], ring[i][k]);
            }
        }
        return builder.build();
    }

    static Mesh panel(int steps, double bright, double from, double to, double thick, double[]... sections) {
        int n = sections.length;
        Builder builder = new Builder();
        int[][] outer = new int[n][steps + 1];
        int[][] inner = new int[n][steps + 1];
        for (int i = 0; i < n; i++) {
            double[] s = sections[i];
            for (int j = 0; j <= steps; j++) {
                double angle = from + (to - from) * j / steps;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                inner[i][j] = builder.point(cos * s[1], s[3] + sin * s[2], s[0]);
                outer[i][j] = builder.point(cos * (s[1] + thick), s[3] + sin * (s[2] + thick), s[0]);
            }
        }
        double middleZ = (sections[0][0] + sections[n - 1][0]) * 0.5;
        double middleAngle = (from + to) * 0.5;
        for (int i = 0; i + 1 < n; i++) {
            double[] a = sections[i];
            double[] b = sections[i + 1];
            Vec3 axis = new Vec3(0.0, (a[3] + b[3]) * 0.5, (a[0] + b[0]) * 0.5);
            for (int j = 0; j < steps; j++) {
                builder.outward(axis, bright, outer[i][j], outer[i][j + 1], outer[i + 1][j + 1], outer[i + 1][j]);
                double angle = from + (to - from) * (j + 0.5) / steps;
                double far = 4.0 * Math.max(a[1] + b[1], a[2] + b[2]);
                Vec3 beyond = axis.add(Math.cos(angle) * far, Math.sin(angle) * far, 0.0);
                builder.outward(beyond, bright, inner[i][j], inner[i][j + 1], inner[i + 1][j + 1], inner[i + 1][j]);
            }
            for (int end = 0; end < 2; end++) {
                int j = end == 0 ? 0 : steps;
                Vec3 inside = axis.add(Math.cos(middleAngle) * a[1], Math.sin(middleAngle) * a[2], 0.0);
                builder.outward(inside, bright, inner[i][j], outer[i][j], outer[i + 1][j], inner[i + 1][j]);
            }
        }
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : n - 1;
            Vec3 inside = new Vec3(0.0, sections[i][3], middleZ);
            for (int j = 0; j < steps; j++) {
                builder.outward(inside, bright, inner[i][j], inner[i][j + 1], outer[i][j + 1], outer[i][j]);
            }
        }
        return builder.build();
    }

    static Mesh wing(double span, double rootBack, double rootFront, double tipBack, double tipFront,
            double rise, double rootThick, double tipThick, double bright) {
        Builder builder = new Builder();
        int[] root = section(builder, 0.0, 0.0, rootBack, rootFront, rootThick);
        int[] tip = section(builder, span, rise, tipBack, tipFront, tipThick);
        Vec3 inside = new Vec3(span * 0.5, rise * 0.5, (rootBack + rootFront + tipBack + tipFront) * 0.25);
        for (int k = 0; k < 4; k++) {
            int next = (k + 1) % 4;
            builder.outward(inside, bright, root[k], root[next], tip[next], tip[k]);
        }
        builder.outward(inside, bright, root[0], root[1], root[2], root[3]);
        builder.outward(inside, bright, tip[0], tip[1], tip[2], tip[3]);
        return builder.build();
    }

    static Mesh sweep(double bright, double[] outline, double[]... sections) {
        int round = outline.length / 2;
        int n = sections.length;
        Builder builder = new Builder();
        int[][] ring = new int[n][round];
        boolean[] point = new boolean[n];
        for (int i = 0; i < n; i++) {
            double[] section = sections[i];
            point[i] = section[1] <= 1.0E-9 && section[2] <= 1.0E-9;
            if (point[i]) {
                Arrays.fill(ring[i], builder.point(0.0, section[3], section[0]));
                continue;
            }
            for (int j = 0; j < round; j++) {
                ring[i][j] = builder.point(outline[2 * j] * section[1], section[3] + outline[2 * j + 1] * section[2],
                        section[0]);
            }
        }
        for (int i = 0; i + 1 < n; i++) {
            Vec3 axis = new Vec3(0.0, (sections[i][3] + sections[i + 1][3]) * 0.5,
                    (sections[i][0] + sections[i + 1][0]) * 0.5);
            for (int j = 0; j < round; j++) {
                int k = (j + 1) % round;
                builder.outward(axis, bright, ring[i][j], ring[i][k], ring[i + 1][k], ring[i + 1][j]);
            }
        }
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : n - 1;
            if (point[i] || n < 2) {
                continue;
            }
            int other = end == 0 ? 1 : n - 2;
            Vec3 inside = new Vec3(0.0, sections[i][3], sections[i][0] + (sections[other][0] - sections[i][0]) * 0.01);
            int middle = builder.point(0.0, sections[i][3], sections[i][0]);
            for (int j = 0; j < round; j++) {
                int k = (j + 1) % round;
                builder.outward(inside, bright, middle, ring[i][j], ring[i][k], ring[i][k]);
            }
        }
        return builder.build();
    }

    static Mesh dish(int rings, double back, double front, double bulge, double bright, double... outline) {
        int n = outline.length / 2;
        double cx = 0.0;
        double cy = 0.0;
        for (int i = 0; i < n; i++) {
            cx += outline[2 * i] / n;
            cy += outline[2 * i + 1] / n;
        }
        Builder builder = new Builder();
        int[][] face = new int[rings + 1][n];
        int[][] rear = new int[rings + 1][n];
        for (int r = 0; r <= rings; r++) {
            double f = (double) r / rings;
            double bow = bulge * (1.0 - f * f);
            for (int i = 0; i < n; i++) {
                double x = cx + (outline[2 * i] - cx) * f;
                double y = cy + (outline[2 * i + 1] - cy) * f;
                if (r == 0) {
                    if (i == 0) {
                        face[0][0] = builder.point(x, y, front + bow);
                        rear[0][0] = builder.point(x, y, back + bow);
                    }
                    face[0][i] = face[0][0];
                    rear[0][i] = rear[0][0];
                    continue;
                }
                face[r][i] = builder.point(x, y, front + bow);
                rear[r][i] = builder.point(x, y, back + bow);
            }
        }
        double middle = (front + back) * 0.5;
        for (int r = 0; r < rings; r++) {
            double f = (r + 0.5) / rings;
            double bow = bulge * (1.0 - f * f);
            Vec3 behind = new Vec3(cx, cy, middle + bow - 8.0 * Math.max(0.05, Math.abs(front - back)));
            Vec3 before = new Vec3(cx, cy, middle + bow + 8.0 * Math.max(0.05, Math.abs(front - back)));
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                builder.outward(behind, bright, face[r][i], face[r][j], face[r + 1][j], face[r + 1][i]);
                builder.outward(before, bright, rear[r][i], rear[r][j], rear[r + 1][j], rear[r + 1][i]);
            }
        }
        Vec3 inside = new Vec3(cx, cy, middle);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            builder.outward(inside, bright, rear[rings][i], rear[rings][j], face[rings][j], face[rings][i]);
        }
        return builder.build();
    }

    static Mesh bevel(double x0, double y0, double z0, double x1, double y1, double z1, double cut, double bright) {
        double b = Math.max(1.0E-4, Math.min(cut, 0.45 * Math.min(x1 - x0, Math.min(y1 - y0, z1 - z0))));
        Builder builder = new Builder();
        // Each corner splits into three points, one on each face that meets there.
        int[][][][] p = new int[2][2][2][3];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    double x = i == 0 ? x0 : x1;
                    double y = j == 0 ? y0 : y1;
                    double z = k == 0 ? z0 : z1;
                    double dx = i == 0 ? b : -b;
                    double dy = j == 0 ? b : -b;
                    double dz = k == 0 ? b : -b;
                    p[i][j][k][0] = builder.point(x, y + dy, z + dz);
                    p[i][j][k][1] = builder.point(x + dx, y, z + dz);
                    p[i][j][k][2] = builder.point(x + dx, y + dy, z);
                }
            }
        }
        Vec3 inside = new Vec3((x0 + x1) * 0.5, (y0 + y1) * 0.5, (z0 + z1) * 0.5);
        for (int s = 0; s < 2; s++) {
            builder.outward(inside, bright, p[s][0][0][0], p[s][1][0][0], p[s][1][1][0], p[s][0][1][0]);
            builder.outward(inside, bright, p[0][s][0][1], p[1][s][0][1], p[1][s][1][1], p[0][s][1][1]);
            builder.outward(inside, bright, p[0][0][s][2], p[1][0][s][2], p[1][1][s][2], p[0][1][s][2]);
        }
        for (int a = 0; a < 2; a++) {
            for (int c = 0; c < 2; c++) {
                builder.outward(inside, bright, p[a][c][0][0], p[a][c][1][0], p[a][c][1][1], p[a][c][0][1]);
                builder.outward(inside, bright, p[a][0][c][0], p[a][1][c][0], p[a][1][c][2], p[a][0][c][2]);
                builder.outward(inside, bright, p[0][a][c][1], p[1][a][c][1], p[1][a][c][2], p[0][a][c][2]);
            }
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    int[] c = p[i][j][k];
                    builder.outward(inside, bright, c[0], c[1], c[2], c[2]);
                }
            }
        }
        return builder.build();
    }

    static Mesh plate(double back, double front, double cut, boolean both, double bright, double... outline) {
        int n = outline.length / 2;
        double b = Math.max(1.0E-4, Math.min(cut, (both ? 0.45 : 0.9) * (front - back)));
        double area = 0.0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += outline[2 * i] * outline[2 * j + 1] - outline[2 * j] * outline[2 * i + 1];
        }
        double turn = area >= 0.0 ? 1.0 : -1.0;
        double[] inset = new double[outline.length];
        for (int i = 0; i < n; i++) {
            int before = (i - 1 + n) % n;
            int after = (i + 1) % n;
            double[] n1 = inward(outline, before, i, turn);
            double[] n2 = inward(outline, i, after, turn);
            double join = Math.max(0.2, 1.0 + n1[0] * n2[0] + n1[1] * n2[1]);
            inset[2 * i] = outline[2 * i] + b * (n1[0] + n2[0]) / join;
            inset[2 * i + 1] = outline[2 * i + 1] + b * (n1[1] + n2[1]) / join;
        }
        Builder builder = new Builder();
        int[] rear = new int[n];
        int[] rearRim = new int[n];
        int[] rim = new int[n];
        int[] face = new int[n];
        double cx = 0.0;
        double cy = 0.0;
        for (int i = 0; i < n; i++) {
            rear[i] = both ? builder.point(inset[2 * i], inset[2 * i + 1], back)
                    : builder.point(outline[2 * i], outline[2 * i + 1], back);
            rearRim[i] = both ? builder.point(outline[2 * i], outline[2 * i + 1], back + b) : rear[i];
            rim[i] = builder.point(outline[2 * i], outline[2 * i + 1], front - b);
            face[i] = builder.point(inset[2 * i], inset[2 * i + 1], front);
            cx += outline[2 * i] / n;
            cy += outline[2 * i + 1] / n;
        }
        int rearMiddle = builder.point(cx, cy, back);
        int faceMiddle = builder.point(cx, cy, front);
        Vec3 inside = new Vec3(cx, cy, (back + front) * 0.5);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            builder.outward(inside, bright, rearMiddle, rear[i], rear[j], rear[j]);
            if (both) {
                builder.outward(inside, bright, rear[i], rear[j], rearRim[j], rearRim[i]);
            }
            builder.outward(inside, bright, rearRim[i], rearRim[j], rim[j], rim[i]);
            builder.outward(inside, bright, rim[i], rim[j], face[j], face[i]);
            builder.outward(inside, bright, faceMiddle, face[i], face[j], face[j]);
        }
        return builder.build();
    }

    private static double[] inward(double[] outline, int from, int to, double turn) {
        double dx = outline[2 * to] - outline[2 * from];
        double dy = outline[2 * to + 1] - outline[2 * from + 1];
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-9) {
            return new double[] { 0.0, 0.0 };
        }
        return new double[] { -dy / length * turn, dx / length * turn };
    }

    private static int[] section(Builder builder, double x, double y, double back, double front, double thick) {
        double crest = front - (front - back) * 0.35;
        return new int[] { builder.point(x, y, front), builder.point(x, y + thick * 0.5, crest),
                builder.point(x, y, back), builder.point(x, y - thick * 0.5, crest) };
    }
}
