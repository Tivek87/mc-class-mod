package nl.tivek.multiversepowers.engine.physics;

// A hinge between two bodies, as a knee or an elbow: their anchor points held together, the second body turning
// about the first's hinge axis only, its bone bent away from the first's bone between min and max radians about that
// axis. All vectors are given in each body's own axes; both bones and hinges line up when the hinge is straight.
public final class HingeJoint implements Constraint {
    private final int a;
    private final int b;
    private final double[] anchorA;
    private final double[] anchorB;
    private final double[] hingeA;
    private final double[] hingeB;
    private final double[] boneA;
    private final double[] boneB;
    private final double min;
    private final double max;
    private final double[] t = new double[12];

    public HingeJoint(int a, double[] anchorA, double[] hingeA, double[] boneA, int b, double[] anchorB,
            double[] hingeB, double[] boneB, double min, double max) {
        this.a = a;
        this.b = b;
        this.anchorA = anchorA.clone();
        this.anchorB = anchorB.clone();
        this.hingeA = hingeA.clone();
        this.hingeB = hingeB.clone();
        this.boneA = boneA.clone();
        this.boneB = boneB.clone();
        this.min = min;
        this.max = max;
    }

    @Override
    public void solve(RigidWorld world, double h) {
        this.align(world, h);
        this.bend(world, h);
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

    // Both hinge axes brought into one line: the second body may only turn about it.
    private void align(RigidWorld world, double h) {
        Quat.rotate(world.q, this.a * 4, this.hingeA[0], this.hingeA[1], this.hingeA[2], this.t, 0);
        Quat.rotate(world.q, this.b * 4, this.hingeB[0], this.hingeB[1], this.hingeB[2], this.t, 3);
        double cx = this.t[1] * this.t[5] - this.t[2] * this.t[4];
        double cy = this.t[2] * this.t[3] - this.t[0] * this.t[5];
        double cz = this.t[0] * this.t[4] - this.t[1] * this.t[3];
        double sin = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (sin < 1.0E-9) {
            return;
        }
        double cos = this.t[0] * this.t[3] + this.t[1] * this.t[4] + this.t[2] * this.t[5];
        double over = Math.atan2(sin, cos) / sin;
        world.turn(this.a, this.b, cx * over, cy * over, cz * over, 0.0, h);
    }

    private void bend(RigidWorld world, double h) {
        Quat.rotate(world.q, this.a * 4, this.hingeA[0], this.hingeA[1], this.hingeA[2], this.t, 0);
        Quat.rotate(world.q, this.a * 4, this.boneA[0], this.boneA[1], this.boneA[2], this.t, 3);
        Quat.rotate(world.q, this.b * 4, this.boneB[0], this.boneB[1], this.boneB[2], this.t, 6);
        double nx = this.t[0];
        double ny = this.t[1];
        double nz = this.t[2];
        double ax = this.t[3];
        double ay = this.t[4];
        double az = this.t[5];
        double bx = this.t[6];
        double by = this.t[7];
        double bz = this.t[8];
        double sin = (ay * bz - az * by) * nx + (az * bx - ax * bz) * ny + (ax * by - ay * bx) * nz;
        double cos = ax * bx + ay * by + az * bz;
        double angle = Math.atan2(sin, cos);
        double held = Math.max(this.min, Math.min(this.max, angle));
        if (angle == held) {
            return;
        }
        double over = angle - held;
        world.turn(this.a, this.b, nx * over, ny * over, nz * over, 0.0, h);
    }
}
