package nl.tivek.multiversepowers.engine.physics.joint;

import nl.tivek.multiversepowers.engine.physics.Constraint;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;

// A muscle across a joint: it turns the second body towards a turn of its own seen from the first (its target), as
// hard as its tone says (0 slack, 1 firm), and damps how fast the two turn apart. With muscles a limp body can hold
// the pose it went limp in, buckle as their tone fades, brace for a fall or reach up and hang. The joint's limits still
// hold: add it before the joint (RigidWorld.addFirst).
public final class Muscle implements Constraint {
    // The give at full tone (XPBD compliance: radians per unit of torque), and how fast the turning apart is damped at
    // full tone (a share per second). Below SLACK a muscle does nothing at all.
    private static final double STIFF = 0.002;
    private static final double DAMPING = 14.0;
    private static final double SLACK = 0.01;
    private final int a;
    private final int b;
    // The second body's turn seen from the first as it was built, and the one the muscle pulls towards now.
    private final double[] rest;
    private final double[] target;
    private final double[] t = new double[4];
    private final double[] s = new double[6];
    private double tone;

    public Muscle(int a, int b, double[] rest) {
        this.a = a;
        this.b = b;
        this.rest = rest.clone();
        this.target = rest.clone();
    }

    public int first() {
        return this.a;
    }

    public int second() {
        return this.b;
    }

    // The turn it was built with, seen from the first body.
    public double[] rest() {
        return this.rest;
    }

    public void target(double x, double y, double z, double w) {
        this.target[0] = x;
        this.target[1] = y;
        this.target[2] = z;
        this.target[3] = w;
        Quat.normalize(this.target, 0);
    }

    public void relax() {
        System.arraycopy(this.rest, 0, this.target, 0, 4);
    }

    public void tone(double tone) {
        this.tone = Math.max(0.0, tone);
    }

    public double tone() {
        return this.tone;
    }

    @Override
    public void solve(RigidWorld world, double h) {
        if (this.tone < SLACK) {
            return;
        }
        // Where the second body should be turned (the first's turn, then the target), and the turn from where it is.
        Quat.multiply(world.q, this.a * 4, this.target, 0, this.t, 0);
        double[] q = world.q;
        int o = this.b * 4;
        double bx = -q[o];
        double by = -q[o + 1];
        double bz = -q[o + 2];
        double bw = q[o + 3];
        double dx = this.t[3] * bx + this.t[0] * bw + this.t[1] * bz - this.t[2] * by;
        double dy = this.t[3] * by + this.t[1] * bw + this.t[2] * bx - this.t[0] * bz;
        double dz = this.t[3] * bz + this.t[2] * bw + this.t[0] * by - this.t[1] * bx;
        double dw = this.t[3] * bw - this.t[0] * bx - this.t[1] * by - this.t[2] * bz;
        if (dw < 0.0) {
            dx = -dx;
            dy = -dy;
            dz = -dz;
            dw = -dw;
        }
        double sin = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (sin < 1.0E-9) {
            return;
        }
        double over = 2.0 * Math.atan2(sin, dw) / sin;
        world.turn(this.b, this.a, dx * over, dy * over, dz * over, STIFF / this.tone, h);
    }

    @Override
    public void damp(RigidWorld world, double h) {
        if (this.tone < SLACK) {
            return;
        }
        world.spin(this.a, this.s);
        double ax = this.s[0];
        double ay = this.s[1];
        double az = this.s[2];
        world.spin(this.b, this.s);
        double rx = this.s[0] - ax;
        double ry = this.s[1] - ay;
        double rz = this.s[2] - az;
        double apart = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (apart < 1.0E-6) {
            return;
        }
        double nx = rx / apart;
        double ny = ry / apart;
        double nz = rz / apart;
        double wa = world.turnWeight(this.a, nx, ny, nz);
        double wb = world.turnWeight(this.b, nx, ny, nz);
        if (wa + wb <= 0.0) {
            return;
        }
        double j = Math.min(1.0, DAMPING * this.tone * h) * apart / (wa + wb);
        world.addSpin(this.b, -nx * j * wb, -ny * j * wb, -nz * j * wb);
        world.addSpin(this.a, nx * j * wa, ny * j * wa, nz * j * wa);
    }
}
