package nl.tivek.multiversepowers.engine.math;

import net.minecraft.world.phys.Vec3;

public final class Vectors {
    public static final Vec3 UP = new Vec3(0, 1, 0);

    private Vectors() {
    }

    public static Vec3 spin(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }

    public static Vec3[] across(Vec3 axis) {
        Vec3 side = Math.abs(axis.y) < 0.95 ? axis.cross(UP) : axis.cross(new Vec3(1, 0, 0));
        side = side.normalize();
        return new Vec3[] { side, side.cross(axis).normalize() };
    }

    public static Vec3[] frame(Vec3 first, Vec3 second) {
        Vec3 u = first.normalize();
        return new Vec3[] { u, second.subtract(u.scale(second.dot(u))).normalize() };
    }

    public static Vec3 turn(Vec3[] from, Vec3[] to) {
        Vec3 a2 = from[0].cross(from[1]);
        Vec3 b2 = to[0].cross(to[1]);
        // The turn as a rotation matrix: what it takes each of from's axes to.
        double[][] m = new double[3][3];
        Vec3[] a = { from[0], from[1], a2 };
        Vec3[] b = { to[0], to[1], b2 };
        for (int i = 0; i < 3; i++) {
            double[] bi = { b[i].x, b[i].y, b[i].z };
            double[] ai = { a[i].x, a[i].y, a[i].z };
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    m[row][col] += bi[row] * ai[col];
                }
            }
        }
        // Quaternion from the matrix's largest term, so it stays accurate near a half turn.
        double trace = m[0][0] + m[1][1] + m[2][2];
        double w;
        double x;
        double y;
        double z;
        if (trace > 0.0) {
            double s = 2.0 * Math.sqrt(trace + 1.0);
            w = 0.25 * s;
            x = (m[2][1] - m[1][2]) / s;
            y = (m[0][2] - m[2][0]) / s;
            z = (m[1][0] - m[0][1]) / s;
        } else if (m[0][0] > m[1][1] && m[0][0] > m[2][2]) {
            double s = 2.0 * Math.sqrt(Math.max(1.0E-12, 1.0 + m[0][0] - m[1][1] - m[2][2]));
            w = (m[2][1] - m[1][2]) / s;
            x = 0.25 * s;
            y = (m[0][1] + m[1][0]) / s;
            z = (m[0][2] + m[2][0]) / s;
        } else if (m[1][1] > m[2][2]) {
            double s = 2.0 * Math.sqrt(Math.max(1.0E-12, 1.0 + m[1][1] - m[0][0] - m[2][2]));
            w = (m[0][2] - m[2][0]) / s;
            x = (m[0][1] + m[1][0]) / s;
            y = 0.25 * s;
            z = (m[1][2] + m[2][1]) / s;
        } else {
            double s = 2.0 * Math.sqrt(Math.max(1.0E-12, 1.0 + m[2][2] - m[0][0] - m[1][1]));
            w = (m[1][0] - m[0][1]) / s;
            x = (m[0][2] + m[2][0]) / s;
            y = (m[1][2] + m[2][1]) / s;
            z = 0.25 * s;
        }
        if (w < 0.0) {
            w = -w;
            x = -x;
            y = -y;
            z = -z;
        }
        double sin = Math.sqrt(x * x + y * y + z * z);
        if (sin < 1.0E-9) {
            return Vec3.ZERO;
        }
        return new Vec3(x, y, z).scale(2.0 * Math.atan2(sin, w) / sin);
    }

    public static Vec3 turned(Vec3 v, Vec3 turn) {
        double angle = turn.length();
        return angle < 1.0E-9 ? v : spin(v, turn.scale(1.0 / angle), angle);
    }
}
