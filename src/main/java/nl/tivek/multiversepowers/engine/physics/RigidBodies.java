package nl.tivek.multiversepowers.engine.physics;

// A rigid world's bodies and the XPBD moves that push, join and turn them.
abstract class RigidBodies {
    public static final int MOST = 32;

    public final double[] x = new double[MOST * 3];
    final double[] px = new double[MOST * 3];
    final double[] v = new double[MOST * 3];
    public final double[] q = new double[MOST * 4];
    final double[] pq = new double[MOST * 4];
    final double[] w = new double[MOST * 3];
    final double[] invMass = new double[MOST];
    final double[] invInertia = new double[MOST * 3];
    public final double[] half = new double[MOST * 3];
    int count;
    final double[] t1 = new double[3];
    final double[] t2 = new double[3];
    final double[] t3 = new double[3];
    public double gravity = -24.0;
    public double friction = 0.6;
    public double linearDamping = 0.4;
    public double angularDamping = 1.2;
    // Turning damped harder while touching something: a limp body lands with a thud, it does not roll like a die.
    public double contactDamping = 0.0;

    // A point given in body b's own axes, in the world.
    public void point(int b, double lx, double ly, double lz, double[] out) {
        Quat.rotate(this.q, b * 4, lx, ly, lz, out, 0);
        out[0] += this.x[b * 3];
        out[1] += this.x[b * 3 + 1];
        out[2] += this.x[b * 3 + 2];
    }

    void integrate(int b, double h) {
        int o = b * 3;
        int r = b * 4;
        System.arraycopy(this.x, o, this.px, o, 3);
        System.arraycopy(this.q, r, this.pq, r, 4);
        if (this.invMass[b] == 0.0) {
            return;
        }
        this.v[o + 1] += this.gravity * h;
        this.x[o] += this.v[o] * h;
        this.x[o + 1] += this.v[o + 1] * h;
        this.x[o + 2] += this.v[o + 2] * h;
        Quat.spin(this.q, r, this.w[o] * h, this.w[o + 1] * h, this.w[o + 2] * h);
    }

    // Moves body b's world point by `depth` along the unit n against the unmovable world, as its mass allows.
    void pushOut(int b, double px, double py, double pz, double nx, double ny, double nz, double depth) {
        double weight = this.weight(b, px, py, pz, nx, ny, nz);
        if (weight <= 0.0 || depth <= 0.0) {
            return;
        }
        double lambda = depth / weight;
        this.apply(b, px, py, pz, nx * lambda, ny * lambda, nz * lambda);
    }

    // The positional constraint that brings point a on body i and point c on body j (j < 0: a fixed point in the
    // world) together; compliance 0 is rigid.
    public void attach(int i, double ax, double ay, double az, int j, double cx, double cy, double cz, double compliance,
            double h) {
        double dx = ax - cx;
        double dy = ay - cy;
        double dz = az - cz;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-9) {
            return;
        }
        double nx = dx / length;
        double ny = dy / length;
        double nz = dz / length;
        double wi = this.weight(i, ax, ay, az, nx, ny, nz);
        double wj = j < 0 ? 0.0 : this.weight(j, cx, cy, cz, nx, ny, nz);
        double soft = compliance / (h * h);
        if (wi + wj + soft <= 0.0) {
            return;
        }
        double lambda = -length / (wi + wj + soft);
        this.apply(i, ax, ay, az, nx * lambda, ny * lambda, nz * lambda);
        if (j >= 0) {
            this.apply(j, cx, cy, cz, -nx * lambda, -ny * lambda, -nz * lambda);
        }
    }

    // Pushes point a on body i and point c on body j apart by `depth` along the unit n (from j towards i).
    public void separate(int i, double ax, double ay, double az, int j, double cx, double cy, double cz, double nx,
            double ny, double nz, double depth) {
        double wi = this.weight(i, ax, ay, az, nx, ny, nz);
        double wj = this.weight(j, cx, cy, cz, nx, ny, nz);
        if (wi + wj <= 0.0 || depth <= 0.0) {
            return;
        }
        double lambda = depth / (wi + wj);
        this.apply(i, ax, ay, az, nx * lambda, ny * lambda, nz * lambda);
        this.apply(j, cx, cy, cz, -nx * lambda, -ny * lambda, -nz * lambda);
    }

    // Turns body i by the rotation vector e (and body j, when there is one, the other way) as far as their inertia
    // shares it out; compliance 0 is rigid.
    public void turn(int i, int j, double ex, double ey, double ez, double compliance, double h) {
        double angle = Math.sqrt(ex * ex + ey * ey + ez * ez);
        if (angle < 1.0E-9) {
            return;
        }
        double nx = ex / angle;
        double ny = ey / angle;
        double nz = ez / angle;
        double wi = this.angularWeight(i, nx, ny, nz);
        double wj = j < 0 ? 0.0 : this.angularWeight(j, nx, ny, nz);
        double soft = compliance / (h * h);
        if (wi + wj + soft <= 0.0) {
            return;
        }
        double lambda = angle / (wi + wj + soft);
        this.spinBy(i, nx * lambda, ny * lambda, nz * lambda, 1.0);
        if (j >= 0) {
            this.spinBy(j, nx * lambda, ny * lambda, nz * lambda, -1.0);
        }
    }

    // How much body b gives way to a push along n at a world point: 1/m + (r x n) . I^-1 (r x n).
    double weight(int b, double px, double py, double pz, double nx, double ny, double nz) {
        if (this.invMass[b] == 0.0) {
            return 0.0;
        }
        int o = b * 3;
        double rx = px - this.x[o];
        double ry = py - this.x[o + 1];
        double rz = pz - this.x[o + 2];
        double ax = ry * nz - rz * ny;
        double ay = rz * nx - rx * nz;
        double az = rx * ny - ry * nx;
        Quat.unrotate(this.q, b * 4, ax, ay, az, this.t3, 0);
        return this.invMass[b] + this.t3[0] * this.t3[0] * this.invInertia[o]
                + this.t3[1] * this.t3[1] * this.invInertia[o + 1] + this.t3[2] * this.t3[2] * this.invInertia[o + 2];
    }

    double angularWeight(int b, double nx, double ny, double nz) {
        if (this.invMass[b] == 0.0) {
            return 0.0;
        }
        int o = b * 3;
        Quat.unrotate(this.q, b * 4, nx, ny, nz, this.t3, 0);
        return this.t3[0] * this.t3[0] * this.invInertia[o] + this.t3[1] * this.t3[1] * this.invInertia[o + 1]
                + this.t3[2] * this.t3[2] * this.invInertia[o + 2];
    }

    // Applies the positional impulse (x, y, z) at a world point of body b: it moves and turns as its mass allows.
    void apply(int b, double px, double py, double pz, double ix, double iy, double iz) {
        if (this.invMass[b] == 0.0) {
            return;
        }
        int o = b * 3;
        double rx = px - this.x[o];
        double ry = py - this.x[o + 1];
        double rz = pz - this.x[o + 2];
        this.x[o] += ix * this.invMass[b];
        this.x[o + 1] += iy * this.invMass[b];
        this.x[o + 2] += iz * this.invMass[b];
        this.spinBy(b, ry * iz - rz * iy, rz * ix - rx * iz, rx * iy - ry * ix, 1.0);
    }

    // Turns body b by I^-1 times the angular impulse (x, y, z), in the given direction.
    void spinBy(int b, double x, double y, double z, double sign) {
        int o = b * 3;
        Quat.unrotate(this.q, b * 4, x, y, z, this.t3, 0);
        double bx = this.t3[0] * this.invInertia[o] * sign;
        double by = this.t3[1] * this.invInertia[o + 1] * sign;
        double bz = this.t3[2] * this.invInertia[o + 2] * sign;
        Quat.rotate(this.q, b * 4, bx, by, bz, this.t3, 0);
        Quat.spin(this.q, b * 4, this.t3[0], this.t3[1], this.t3[2]);
    }
}
