package nl.tivek.multiversepowers.engine.physics.joint;

import nl.tivek.multiversepowers.engine.physics.Constraint;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;

// The middle of a trunk cut in two, a waist or the middle of a back: the halves' anchor points held together, the
// second half folding about the first's hinge between min and max radians, leaning out to either side at most `side`
// and twisting about its bone at most `twist`. All vectors are given in each body's own axes; bones and hinges line
// up when it is straight.
public final class SpineJoint implements Constraint {
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
    private final double side;
    private final double twist;
    private final double[] t = new double[12];

    public SpineJoint(int a, double[] anchorA, double[] hingeA, double[] boneA, int b, double[] anchorB,
            double[] hingeB, double[] boneB, double min, double max, double side, double twist) {
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
        this.side = side;
        this.twist = twist;
    }

    @Override
    public void solve(RigidWorld world, double h) {
        this.swing(world, h);
        this.twist(world, h);
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

    // The second bone seen from the first half: how far it folds about the hinge and how far it leans out of that
    // fold to the side, each held within its range by turning it to the nearest way it may point.
    private void swing(RigidWorld world, double h) {
        Quat.rotate(world.q, this.a * 4, this.hingeA[0], this.hingeA[1], this.hingeA[2], this.t, 0);
        Quat.rotate(world.q, this.a * 4, this.boneA[0], this.boneA[1], this.boneA[2], this.t, 3);
        Quat.rotate(world.q, this.b * 4, this.boneB[0], this.boneB[1], this.boneB[2], this.t, 6);
        double hx = this.t[0];
        double hy = this.t[1];
        double hz = this.t[2];
        double bx = this.t[3];
        double by = this.t[4];
        double bz = this.t[5];
        double dx = this.t[6];
        double dy = this.t[7];
        double dz = this.t[8];
        // The way the second half folds: hinge x bone.
        double fx = hy * bz - hz * by;
        double fy = hz * bx - hx * bz;
        double fz = hx * by - hy * bx;
        double out = dx * hx + dy * hy + dz * hz;
        double along = dx * bx + dy * by + dz * bz;
        double ahead = dx * fx + dy * fy + dz * fz;
        double fold = Math.atan2(ahead, along);
        double lean = Math.atan2(out, Math.sqrt(along * along + ahead * ahead));
        double heldFold = Math.max(this.min, Math.min(this.max, fold));
        double heldLean = Math.max(-this.side, Math.min(this.side, lean));
        if (heldFold == fold && heldLean == lean) {
            return;
        }
        double c = Math.cos(heldLean);
        double s = Math.sin(heldLean);
        double cf = Math.cos(heldFold) * c;
        double sf = Math.sin(heldFold) * c;
        double wx = hx * s + bx * cf + fx * sf;
        double wy = hy * s + by * cf + fy * sf;
        double wz = hz * s + bz * cf + fz * sf;
        // The second half turns from where its bone points to where it may, the first half the other way.
        double cx = wy * dz - wz * dy;
        double cy = wz * dx - wx * dz;
        double cz = wx * dy - wy * dx;
        double sin = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (sin < 1.0E-9) {
            return;
        }
        double over = Math.atan2(sin, wx * dx + wy * dy + wz * dz) / sin;
        world.turn(this.a, this.b, cx * over, cy * over, cz * over, 0.0, h);
    }

    // The twist about the line between the two bones, measured by the hinges as they stand across it.
    private void twist(RigidWorld world, double h) {
        Quat.rotate(world.q, this.a * 4, this.boneA[0], this.boneA[1], this.boneA[2], this.t, 0);
        Quat.rotate(world.q, this.b * 4, this.boneB[0], this.boneB[1], this.boneB[2], this.t, 3);
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
        Quat.rotate(world.q, this.a * 4, this.hingeA[0], this.hingeA[1], this.hingeA[2], this.t, 6);
        Quat.rotate(world.q, this.b * 4, this.hingeB[0], this.hingeB[1], this.hingeB[2], this.t, 9);
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
        double held = Math.max(-this.twist, Math.min(this.twist, angle));
        if (angle == held) {
            return;
        }
        double over = angle - held;
        world.turn(this.a, this.b, nx * over, ny * over, nz * over, 0.0, h);
    }
}
