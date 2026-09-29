package nl.tivek.multiversepowers.engine.physics.joint;

import nl.tivek.multiversepowers.engine.physics.Constraint;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;

// Keeps a limb out of another body: the limb is a capsule (from end to end in its own axes, with a radius) and the
// other body its box, so an arm never passes through the chest and a leg never through the other leg.
public final class SelfContact implements Constraint {
    private static final int SAMPLES = 5;
    private final int limb;
    private final int body;
    private final double[] from;
    private final double[] to;
    private final double radius;
    private final double[] p = new double[3];
    private final double[] local = new double[3];
    private final double[] n = new double[3];

    public SelfContact(int limb, double[] from, double[] to, double radius, int body) {
        this.limb = limb;
        this.body = body;
        this.from = from.clone();
        this.to = to.clone();
        this.radius = radius;
    }

    @Override
    public void solve(RigidWorld world, double h) {
        int o = this.body * 3;
        double hx = world.half[o];
        double hy = world.half[o + 1];
        double hz = world.half[o + 2];
        for (int i = 0; i < SAMPLES; i++) {
            double u = i / (SAMPLES - 1.0);
            world.point(this.limb, this.from[0] + (this.to[0] - this.from[0]) * u,
                    this.from[1] + (this.to[1] - this.from[1]) * u, this.from[2] + (this.to[2] - this.from[2]) * u,
                    this.p);
            Quat.unrotate(world.q, this.body * 4, this.p[0] - world.x[o], this.p[1] - world.x[o + 1],
                    this.p[2] - world.x[o + 2], this.local, 0);
            double depth = depth(this.local[0], this.local[1], this.local[2], hx, hy, hz, this.n) + this.radius;
            if (depth <= 0.0) {
                continue;
            }
            Quat.rotate(world.q, this.body * 4, this.n[0], this.n[1], this.n[2], this.n, 0);
            double sx = this.p[0] - this.n[0] * this.radius;
            double sy = this.p[1] - this.n[1] * this.radius;
            double sz = this.p[2] - this.n[2] * this.radius;
            world.separate(this.limb, sx, sy, sz, this.body, sx, sy, sz, this.n[0], this.n[1], this.n[2], depth);
        }
    }

    // How deep the limb is in the body as they lie now; 0 when they are apart.
    public double deepest(RigidWorld world) {
        int o = this.body * 3;
        double deepest = 0.0;
        for (int i = 0; i < SAMPLES; i++) {
            double u = i / (SAMPLES - 1.0);
            world.point(this.limb, this.from[0] + (this.to[0] - this.from[0]) * u,
                    this.from[1] + (this.to[1] - this.from[1]) * u, this.from[2] + (this.to[2] - this.from[2]) * u,
                    this.p);
            Quat.unrotate(world.q, this.body * 4, this.p[0] - world.x[o], this.p[1] - world.x[o + 1],
                    this.p[2] - world.x[o + 2], this.local, 0);
            deepest = Math.max(deepest, depth(this.local[0], this.local[1], this.local[2], world.half[o],
                    world.half[o + 1], world.half[o + 2], this.n) + this.radius);
        }
        return deepest;
    }

    // How far inside a box (half sizes hx, hy, hz round the origin) a point is, negative outside, with the way out.
    public static double depth(double x, double y, double z, double hx, double hy, double hz, double[] out) {
        double dx = Math.abs(x) - hx;
        double dy = Math.abs(y) - hy;
        double dz = Math.abs(z) - hz;
        if (dx > 0.0 || dy > 0.0 || dz > 0.0) {
            double ox = Math.max(dx, 0.0);
            double oy = Math.max(dy, 0.0);
            double oz = Math.max(dz, 0.0);
            double gap = Math.sqrt(ox * ox + oy * oy + oz * oz);
            if (gap < 1.0E-12) {
                out[0] = 0.0;
                out[1] = 1.0;
                out[2] = 0.0;
                return 0.0;
            }
            out[0] = Math.copySign(ox, x) / gap;
            out[1] = Math.copySign(oy, y) / gap;
            out[2] = Math.copySign(oz, z) / gap;
            return -gap;
        }
        if (dx >= dy && dx >= dz) {
            out[0] = Math.copySign(1.0, x);
            out[1] = 0.0;
            out[2] = 0.0;
            return -dx;
        }
        if (dy >= dz) {
            out[0] = 0.0;
            out[1] = Math.copySign(1.0, y);
            out[2] = 0.0;
            return -dy;
        }
        out[0] = 0.0;
        out[1] = 0.0;
        out[2] = Math.copySign(1.0, z);
        return -dz;
    }
}
