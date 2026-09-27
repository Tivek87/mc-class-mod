package nl.tivek.multiversepowers.engine.math;

public final class Segments {
    private Segments() {
    }

    // Closest points of segments a0-a1 and b0-b1 (Ericson, Real-Time Collision Detection 5.1.9): the squared distance,
    // with where along each segment (0 to 1) written to out[0] and out[1].
    public static double closest(double[] a0, double[] a1, double[] b0, double[] b1, double[] out) {
        double d1x = a1[0] - a0[0];
        double d1y = a1[1] - a0[1];
        double d1z = a1[2] - a0[2];
        double d2x = b1[0] - b0[0];
        double d2y = b1[1] - b0[1];
        double d2z = b1[2] - b0[2];
        double rx = a0[0] - b0[0];
        double ry = a0[1] - b0[1];
        double rz = a0[2] - b0[2];
        double a = d1x * d1x + d1y * d1y + d1z * d1z;
        double e = d2x * d2x + d2y * d2y + d2z * d2z;
        double f = d2x * rx + d2y * ry + d2z * rz;
        double s;
        double t;
        if (a <= 1.0E-24 && e <= 1.0E-24) {
            s = 0.0;
            t = 0.0;
        } else if (a <= 1.0E-24) {
            s = 0.0;
            t = clamp(f / e);
        } else {
            double c = d1x * rx + d1y * ry + d1z * rz;
            if (e <= 1.0E-24) {
                t = 0.0;
                s = clamp(-c / a);
            } else {
                double b = d1x * d2x + d1y * d2y + d1z * d2z;
                double denom = a * e - b * b;
                s = denom > 1.0E-24 ? clamp((b * f - c * e) / denom) : 0.0;
                t = (b * s + f) / e;
                if (t < 0.0) {
                    t = 0.0;
                    s = clamp(-c / a);
                } else if (t > 1.0) {
                    t = 1.0;
                    s = clamp((b - c) / a);
                }
            }
        }
        out[0] = s;
        out[1] = t;
        double dx = a0[0] + d1x * s - (b0[0] + d2x * t);
        double dy = a0[1] + d1y * s - (b0[1] + d2y * t);
        double dz = a0[2] + d1z * s - (b0[2] + d2z * t);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double clamp(double value) {
        return value < 0.0 ? 0.0 : Math.min(value, 1.0);
    }
}
