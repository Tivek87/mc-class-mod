package nl.tivek.multiversepowers.engine.math;

// Signed distances from a point to a shape: below zero inside, zero on the surface, above zero outside.
public final class Sdf {
    private Sdf() {
    }

    public static double sphere(double px, double py, double pz, double cx, double cy, double cz, double radius) {
        double dx = px - cx;
        double dy = py - cy;
        double dz = pz - cz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz) - radius;
    }

    public static double capsule(double px, double py, double pz, double ax, double ay, double az, double bx,
            double by, double bz, double radius) {
        double pax = px - ax;
        double pay = py - ay;
        double paz = pz - az;
        double bax = bx - ax;
        double bay = by - ay;
        double baz = bz - az;
        double long2 = bax * bax + bay * bay + baz * baz;
        double h = long2 < 1.0E-24 ? 0.0 : Math.max(0.0, Math.min(1.0, (pax * bax + pay * bay + paz * baz) / long2));
        double dx = pax - bax * h;
        double dy = pay - bay * h;
        double dz = paz - baz * h;
        return Math.sqrt(dx * dx + dy * dy + dz * dz) - radius;
    }

    // A box round the origin with half sizes hx, hy, hz; the point in the box's own axes.
    public static double box(double px, double py, double pz, double hx, double hy, double hz) {
        double qx = Math.abs(px) - hx;
        double qy = Math.abs(py) - hy;
        double qz = Math.abs(pz) - hz;
        double ox = Math.max(qx, 0.0);
        double oy = Math.max(qy, 0.0);
        double oz = Math.max(qz, 0.0);
        return Math.sqrt(ox * ox + oy * oy + oz * oz) + Math.min(Math.max(qx, Math.max(qy, qz)), 0.0);
    }

    public static double roundBox(double px, double py, double pz, double hx, double hy, double hz, double radius) {
        return box(px, py, pz, hx - radius, hy - radius, hz - radius) - radius;
    }
}
