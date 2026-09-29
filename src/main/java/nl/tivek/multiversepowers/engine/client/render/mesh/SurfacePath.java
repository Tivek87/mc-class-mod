package nl.tivek.multiversepowers.engine.client.render.mesh;

// The sections of a Surface joined smoothly (Catmull-Rom), read by how far along the path you are, so that equal steps
// of v are equal lengths on the body; a section given twice makes a corner there.
final class SurfacePath {
    private static final int STEPS = 48;

    private final double[][] sections;
    private final double[] lengths;

    SurfacePath(double[][] given) {
        this.sections = new double[given.length][];
        for (int i = 0; i < given.length; i++) {
            double[] s = given[i];
            this.sections[i] = new double[] { s[0], s[1], s[2], s[3], s.length > 4 ? s[4] : 2.0 };
        }
        int pieces = Math.max(1, this.sections.length - 1);
        this.lengths = new double[pieces * STEPS + 1];
        double[] before = this.smooth(0.0);
        for (int k = 1; k < this.lengths.length; k++) {
            double[] now = this.smooth((double) k / STEPS);
            double dy = now[0] - before[0];
            double dr = (now[1] + now[2]) * 0.5 - (before[1] + before[2]) * 0.5;
            double dz = now[3] - before[3];
            this.lengths[k] = this.lengths[k - 1] + Math.sqrt(dy * dy + dr * dr + dz * dz);
            before = now;
        }
    }

    double[] at(double v) {
        double total = this.lengths[this.lengths.length - 1];
        if (total <= 1.0E-12 || this.sections.length < 2) {
            return this.smooth(v * (this.sections.length - 1));
        }
        double want = Math.max(0.0, Math.min(1.0, v)) * total;
        int low = 0;
        int high = this.lengths.length - 1;
        while (high - low > 1) {
            int middle = (low + high) >>> 1;
            if (this.lengths[middle] <= want) {
                low = middle;
            } else {
                high = middle;
            }
        }
        double span = this.lengths[high] - this.lengths[low];
        double f = span <= 1.0E-12 ? 0.0 : (want - this.lengths[low]) / span;
        return this.smooth((low + f) / STEPS);
    }

    private double[] smooth(double position) {
        int last = this.sections.length - 1;
        if (last <= 0) {
            return this.sections[0].clone();
        }
        double clamped = Math.max(0.0, Math.min(last, position));
        int i = Math.min(last - 1, (int) Math.floor(clamped));
        double t = clamped - i;
        double[] p0 = this.sections[Math.max(0, i - 1)];
        double[] p1 = this.sections[i];
        double[] p2 = this.sections[i + 1];
        double[] p3 = this.sections[Math.min(last, i + 2)];
        double[] out = new double[5];
        double t2 = t * t;
        double t3 = t2 * t;
        for (int k = 0; k < 5; k++) {
            if (same(p1, p2)) {
                out[k] = p1[k];
                continue;
            }
            double m1 = same(p0, p1) ? 0.0 : (p2[k] - p0[k]) * 0.5;
            double m2 = same(p2, p3) ? 0.0 : (p3[k] - p1[k]) * 0.5;
            out[k] = (2.0 * t3 - 3.0 * t2 + 1.0) * p1[k] + (t3 - 2.0 * t2 + t) * m1 + (-2.0 * t3 + 3.0 * t2) * p2[k]
                    + (t3 - t2) * m2;
        }
        out[4] = Math.max(0.5, out[4]);
        return out;
    }

    private static boolean same(double[] a, double[] b) {
        return a == b || a[0] == b[0] && a[1] == b[1] && a[2] == b[2] && a[3] == b[3];
    }
}
