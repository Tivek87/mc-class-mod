package nl.tivek.multiversepowers.engine.physics;

// Quaternions and small vector sums on plain arrays (x, y, z, w at an offset), so a solver step makes no garbage.
public final class Quat {
    private Quat() {
    }

    // v' = q v q*, the vector (x, y, z) turned by the quaternion at q[o].
    public static void rotate(double[] q, int o, double x, double y, double z, double[] out, int at) {
        double qx = q[o];
        double qy = q[o + 1];
        double qz = q[o + 2];
        double qw = q[o + 3];
        double tx = 2.0 * (qy * z - qz * y);
        double ty = 2.0 * (qz * x - qx * z);
        double tz = 2.0 * (qx * y - qy * x);
        out[at] = x + qw * tx + (qy * tz - qz * ty);
        out[at + 1] = y + qw * ty + (qz * tx - qx * tz);
        out[at + 2] = z + qw * tz + (qx * ty - qy * tx);
    }

    // The vector turned back by the quaternion's inverse: world into the body's own axes.
    public static void unrotate(double[] q, int o, double x, double y, double z, double[] out, int at) {
        double qx = -q[o];
        double qy = -q[o + 1];
        double qz = -q[o + 2];
        double qw = q[o + 3];
        double tx = 2.0 * (qy * z - qz * y);
        double ty = 2.0 * (qz * x - qx * z);
        double tz = 2.0 * (qx * y - qy * x);
        out[at] = x + qw * tx + (qy * tz - qz * ty);
        out[at + 1] = y + qw * ty + (qz * tx - qx * tz);
        out[at + 2] = z + qw * tz + (qx * ty - qy * tx);
    }

    // Turns the quaternion by the small rotation vector (x, y, z): q += 0.5 [v, 0] q, then normalised.
    public static void spin(double[] q, int o, double x, double y, double z) {
        double qx = q[o];
        double qy = q[o + 1];
        double qz = q[o + 2];
        double qw = q[o + 3];
        q[o] = qx + 0.5 * (x * qw + y * qz - z * qy);
        q[o + 1] = qy + 0.5 * (y * qw + z * qx - x * qz);
        q[o + 2] = qz + 0.5 * (z * qw + x * qy - y * qx);
        q[o + 3] = qw + 0.5 * (-x * qx - y * qy - z * qz);
        normalize(q, o);
    }

    // out = a b: the turn b, then a.
    public static void multiply(double[] a, int ao, double[] b, int bo, double[] out, int at) {
        double ax = a[ao];
        double ay = a[ao + 1];
        double az = a[ao + 2];
        double aw = a[ao + 3];
        double bx = b[bo];
        double by = b[bo + 1];
        double bz = b[bo + 2];
        double bw = b[bo + 3];
        out[at] = aw * bx + ax * bw + ay * bz - az * by;
        out[at + 1] = aw * by + ay * bw + az * bx - ax * bz;
        out[at + 2] = aw * bz + az * bw + ax * by - ay * bx;
        out[at + 3] = aw * bw - ax * bx - ay * by - az * bz;
    }

    // out = a* b: b seen from a.
    public static void relative(double[] a, int ao, double[] b, int bo, double[] out, int at) {
        double ax = -a[ao];
        double ay = -a[ao + 1];
        double az = -a[ao + 2];
        double aw = a[ao + 3];
        double bx = b[bo];
        double by = b[bo + 1];
        double bz = b[bo + 2];
        double bw = b[bo + 3];
        out[at] = aw * bx + ax * bw + ay * bz - az * by;
        out[at + 1] = aw * by + ay * bw + az * bx - ax * bz;
        out[at + 2] = aw * bz + az * bw + ax * by - ay * bx;
        out[at + 3] = aw * bw - ax * bx - ay * by - az * bz;
    }

    public static void normalize(double[] q, int o) {
        double l = Math.sqrt(q[o] * q[o] + q[o + 1] * q[o + 1] + q[o + 2] * q[o + 2] + q[o + 3] * q[o + 3]);
        if (l < 1.0E-12) {
            q[o] = 0.0;
            q[o + 1] = 0.0;
            q[o + 2] = 0.0;
            q[o + 3] = 1.0;
            return;
        }
        q[o] /= l;
        q[o + 1] /= l;
        q[o + 2] /= l;
        q[o + 3] /= l;
    }

    // The angular velocity that turns `from` into `to` in time h: 2 (to from*).xyz / h, the short way round.
    public static void velocity(double[] to, int o, double[] from, int f, double h, double[] out, int at) {
        double ax = to[o];
        double ay = to[o + 1];
        double az = to[o + 2];
        double aw = to[o + 3];
        double bx = -from[f];
        double by = -from[f + 1];
        double bz = -from[f + 2];
        double bw = from[f + 3];
        double dx = aw * bx + ax * bw + ay * bz - az * by;
        double dy = aw * by + ay * bw + az * bx - ax * bz;
        double dz = aw * bz + az * bw + ax * by - ay * bx;
        double dw = aw * bw - ax * bx - ay * by - az * bz;
        double sign = dw < 0.0 ? -1.0 : 1.0;
        out[at] = 2.0 * dx * sign / h;
        out[at + 1] = 2.0 * dy * sign / h;
        out[at + 2] = 2.0 * dz * sign / h;
    }
}
