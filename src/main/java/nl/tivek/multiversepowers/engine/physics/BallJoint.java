package nl.tivek.multiversepowers.engine.physics;

// A ball joint between two bodies: their anchor points held together, the second body's bone axis swinging at most
// `swing` radians away from the first's, and twisting about it only between twistMin and twistMax. All vectors are
// given in each body's own axes; the twist references stand across the bone axes.
public final class BallJoint implements Constraint {
    private final int a;
    private final int b;
    private final double[] anchorA;
    private final double[] anchorB;
    private final double[] axisA;
    private final double[] axisB;
    private final double[] referenceA;
    private final double[] referenceB;
    private final double swing;
    private final double twistMin;
    private final double twistMax;
    private final double[] t = new double[12];

    public BallJoint(int a, double[] anchorA, double[] axisA, double[] referenceA, int b, double[] anchorB,
            double[] axisB, double[] referenceB, double swing, double twistMin, double twistMax) {
        this.a = a;
        this.b = b;
        this.anchorA = anchorA.clone();
        this.anchorB = anchorB.clone();
        this.axisA = axisA.clone();
        this.axisB = axisB.clone();
        this.referenceA = referenceA.clone();
        this.referenceB = referenceB.clone();
        this.swing = swing;
        this.twistMin = twistMin;
        this.twistMax = twistMax;
    }

    @Override
    public void solve(RigidWorld world, double h) {
        this.swing(world, h);
        this.twist(world, h);
        // The limits turn each body about its own centre, which pulls the anchors apart: join them last.
        this.rejoin(world, h);
    }

    @Override
    public void rejoin(RigidWorld world, double h) {
        world.point(this.a, this.anchorA[0], this.anchorA[1], this.anchorA[2], this.t);
        double ax = this.t[0];
        double ay = this.t[1];
        double az = this.t[2];
        world.point(this.b, this.anchorB[0], this.anchorB[1], this.anchorB[2], this.t);
        world.attach(this.a, ax, ay, az, this.b, this.t[0], this.t[1], this.t[2], 0.0, h);
    }

    private void swing(RigidWorld world, double h) {
        Quat.rotate(world.q, this.a * 4, this.axisA[0], this.axisA[1], this.axisA[2], this.t, 0);
        Quat.rotate(world.q, this.b * 4, this.axisB[0], this.axisB[1], this.axisB[2], this.t, 3);
        double cx = this.t[1] * this.t[5] - this.t[2] * this.t[4];
        double cy = this.t[2] * this.t[3] - this.t[0] * this.t[5];
        double cz = this.t[0] * this.t[4] - this.t[1] * this.t[3];
        double sin = Math.sqrt(cx * cx + cy * cy + cz * cz);
        double cos = this.t[0] * this.t[3] + this.t[1] * this.t[4] + this.t[2] * this.t[5];
        double angle = Math.atan2(sin, cos);
        if (angle <= this.swing || sin < 1.0E-9) {
            return;
        }
        double over = (angle - this.swing) / sin;
        world.turn(this.a, this.b, cx * over, cy * over, cz * over, 0.0, h);
    }

    private void twist(RigidWorld world, double h) {
        Quat.rotate(world.q, this.a * 4, this.axisA[0], this.axisA[1], this.axisA[2], this.t, 0);
        Quat.rotate(world.q, this.b * 4, this.axisB[0], this.axisB[1], this.axisB[2], this.t, 3);
        double nx = this.t[0] + this.t[3];
        double ny = this.t[1] + this.t[4];
        double nz = this.t[2] + this.t[5];
        double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1.0E-9) {
            return;
        }
        nx /= nl;
        ny /= nl;
        nz /= nl;
        Quat.rotate(world.q, this.a * 4, this.referenceA[0], this.referenceA[1], this.referenceA[2], this.t, 6);
        Quat.rotate(world.q, this.b * 4, this.referenceB[0], this.referenceB[1], this.referenceB[2], this.t, 9);
        double da = this.t[6] * nx + this.t[7] * ny + this.t[8] * nz;
        double db = this.t[9] * nx + this.t[10] * ny + this.t[11] * nz;
        double ax = this.t[6] - nx * da;
        double ay = this.t[7] - ny * da;
        double az = this.t[8] - nz * da;
        double bx = this.t[9] - nx * db;
        double by = this.t[10] - ny * db;
        double bz = this.t[11] - nz * db;
        double la = Math.sqrt(ax * ax + ay * ay + az * az);
        double lb = Math.sqrt(bx * bx + by * by + bz * bz);
        if (la < 1.0E-9 || lb < 1.0E-9) {
            return;
        }
        double sin = ((ay * bz - az * by) * nx + (az * bx - ax * bz) * ny + (ax * by - ay * bx) * nz) / (la * lb);
        double cos = (ax * bx + ay * by + az * bz) / (la * lb);
        double angle = Math.atan2(sin, cos);
        double held = Math.max(this.twistMin, Math.min(this.twistMax, angle));
        if (angle == held) {
            return;
        }
        double over = angle - held;
        world.turn(this.a, this.b, nx * over, ny * over, nz * over, 0.0, h);
    }
}
