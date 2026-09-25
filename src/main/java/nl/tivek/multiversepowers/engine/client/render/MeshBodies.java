package nl.tivek.multiversepowers.engine.client.render;

import java.util.Arrays;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.Mesh.Builder;

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

    private static int[] section(Builder builder, double x, double y, double back, double front, double thick) {
        double crest = front - (front - back) * 0.35;
        return new int[] { builder.point(x, y, front), builder.point(x, y + thick * 0.5, crest),
                builder.point(x, y, back), builder.point(x, y - thick * 0.5, crest) };
    }
}
