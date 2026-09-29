package nl.tivek.multiversepowers.engine.physics.joint;

import nl.tivek.multiversepowers.engine.physics.Constraint;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.RigidWorld;

// A ball joint shaped like a shoulder, a hip or a neck: the second body's bone swings about a middle direction of the
// first body's, within an ellipse of turns (at most wide1 about the axis `across`, wide2 about the axis across both),
// and twists about itself only between twistMin and twistMax. A middle away from where the limb hangs at rest makes
// the reach lopsided, as an arm goes far up in front and hardly behind the back. The twist is counted from how the
// limb hangs at rest (`rest`, with referenceA across it), swung the shortest way to where it points: counted from
// the middle, a limb only swung would seem twisted, and its twist limit would turn it. All vectors are given in each
// body's own axes; `across` stands across `middle`, and each twist reference across its body's bone.
public final class LimbJoint implements Constraint {
    // Pointing this near straight away from where it hangs at rest, its twist is not counted.
    private static final double TWIST_EDGE = 0.1;
    private final int a;
    private final int b;
    private final double[] anchorA;
    private final double[] anchorB;
    private final double[] middle;
    private final double[] across;
    private final double[] rest;
    private final double[] bone;
    private final double[] referenceA;
    private final double[] referenceB;
    private final double wide1;
    private final double wide2;
    private final double twistMin;
    private final double twistMax;
    private final double[] t = new double[12];

    public LimbJoint(int a, double[] anchorA, double[] middle, double[] across, double[] rest, double[] referenceA,
            int b, double[] anchorB, double[] bone, double[] referenceB, double wide1, double wide2, double twistMin,
            double twistMax) {
        this.a = a;
        this.b = b;
        this.anchorA = anchorA.clone();
        this.anchorB = anchorB.clone();
        this.middle = middle.clone();
        this.across = across.clone();
        this.rest = rest.clone();
        this.bone = bone.clone();
        this.referenceA = referenceA.clone();
        this.referenceB = referenceB.clone();
        this.wide1 = wide1;
        this.wide2 = wide2;
        this.twistMin = twistMin;
        this.twistMax = twistMax;
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

    // How far the bone is turned from the middle, as a turn (angle times axis) split along the ellipse's two axes.
    // Past the ellipse it is brought back to the nearest point on its edge: drawn in along any other line, a limb
    // lying against its limit would be moved along the edge a little every step, and creep.
    private void swing(RigidWorld world, double h) {
        Quat.rotate(world.q, this.a * 4, this.middle[0], this.middle[1], this.middle[2], this.t, 0);
        Quat.rotate(world.q, this.a * 4, this.across[0], this.across[1], this.across[2], this.t, 3);
        Quat.rotate(world.q, this.b * 4, this.bone[0], this.bone[1], this.bone[2], this.t, 6);
        double mx = this.t[0];
        double my = this.t[1];
        double mz = this.t[2];
        double e1x = this.t[3];
        double e1y = this.t[4];
        double e1z = this.t[5];
        double dx = this.t[6];
        double dy = this.t[7];
        double dz = this.t[8];
        double e2x = my * e1z - mz * e1y;
        double e2y = mz * e1x - mx * e1z;
        double e2z = mx * e1y - my * e1x;
        double cx = my * dz - mz * dy;
        double cy = mz * dx - mx * dz;
        double cz = mx * dy - my * dx;
        double sin = Math.sqrt(cx * cx + cy * cy + cz * cz);
        double cos = mx * dx + my * dy + mz * dz;
        double angle = Math.atan2(sin, cos);
        double r1;
        double r2;
        if (sin > 1.0E-9) {
            r1 = angle * (cx * e1x + cy * e1y + cz * e1z) / sin;
            r2 = angle * (cx * e2x + cy * e2y + cz * e2z) / sin;
        } else if (cos < 0.0) {
            // Straight the other way from the middle: any way back will do.
            r1 = angle;
            r2 = 0.0;
        } else {
            return;
        }
        double k = r1 * r1 / (this.wide1 * this.wide1) + r2 * r2 / (this.wide2 * this.wide2);
        if (k <= 1.0) {
            return;
        }
        nearest(Math.abs(r1), Math.abs(r2), this.wide1, this.wide2, this.t);
        double n1 = Math.copySign(this.t[0], r1);
        double n2 = Math.copySign(this.t[1], r2);
        double held = Math.sqrt(n1 * n1 + n2 * n2);
        if (held < 1.0E-9) {
            return;
        }
        double nx = (n1 * e1x + n2 * e2x) / held;
        double ny = (n1 * e1y + n2 * e2y) / held;
        double nz = (n1 * e1z + n2 * e2z) / held;
        // Where the bone may point: the middle turned by the held turn about an axis across it.
        double s = Math.sin(held);
        double c = Math.cos(held);
        double wx = mx * c + (ny * mz - nz * my) * s;
        double wy = my * c + (nz * mx - nx * mz) * s;
        double wz = mz * c + (nx * my - ny * mx) * s;
        double ux = wy * dz - wz * dy;
        double uy = wz * dx - wx * dz;
        double uz = wx * dy - wy * dx;
        double back = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (back < 1.0E-9) {
            return;
        }
        double over = Math.atan2(back, wx * dx + wy * dy + wz * dz) / back;
        world.turn(this.a, this.b, ux * over, uy * over, uz * over, 0.0, h);
    }

    // The point of the ellipse x^2/a^2 + y^2/b^2 = 1 nearest (u, v), a point outside it with u, v >= 0, into out[0]
    // and out[1]. The nearest point is (a^2 u / (s + a^2), b^2 v / (s + b^2)) for the s > 0 that puts it on the edge;
    // Newton's method finds s from 0 without overshooting, as that sum falls and bends up as s grows.
    public static void nearest(double u, double v, double a, double b, double[] out) {
        double aa = a * a;
        double bb = b * b;
        double s = 0.0;
        for (int i = 0; i < 16; i++) {
            double x = a * u / (s + aa);
            double y = b * v / (s + bb);
            double f = x * x + y * y - 1.0;
            if (f < 1.0E-10) {
                break;
            }
            double slope = -2.0 * (x * x / (s + aa) + y * y / (s + bb));
            s -= f / slope;
        }
        out[0] = aa * u / (s + aa);
        out[1] = bb * v / (s + bb);
    }

    // The twist about the bone: the angle from the rest reference, swung the shortest way from the rest to the bone,
    // to the limb's own reference. Turned back about the bone itself, it moves nothing else.
    private void twist(RigidWorld world, double h) {
        Quat.rotate(world.q, this.a * 4, this.rest[0], this.rest[1], this.rest[2], this.t, 0);
        Quat.rotate(world.q, this.b * 4, this.bone[0], this.bone[1], this.bone[2], this.t, 3);
        Quat.rotate(world.q, this.a * 4, this.referenceA[0], this.referenceA[1], this.referenceA[2], this.t, 6);
        Quat.rotate(world.q, this.b * 4, this.referenceB[0], this.referenceB[1], this.referenceB[2], this.t, 9);
        double rx = this.t[0];
        double ry = this.t[1];
        double rz = this.t[2];
        double dx = this.t[3];
        double dy = this.t[4];
        double dz = this.t[5];
        double cos = rx * dx + ry * dy + rz * dz;
        if (cos < TWIST_EDGE - 1.0) {
            return;
        }
        // Rest reference swung about r x d by the angle between them: v cos + (a x v) sin + a (a . v)(1 - cos).
        double ax = ry * dz - rz * dy;
        double ay = rz * dx - rx * dz;
        double az = rx * dy - ry * dx;
        double sin = Math.sqrt(ax * ax + ay * ay + az * az);
        double vx = this.t[6];
        double vy = this.t[7];
        double vz = this.t[8];
        if (sin > 1.0E-9) {
            ax /= sin;
            ay /= sin;
            az /= sin;
            double along = (ax * vx + ay * vy + az * vz) * (1.0 - cos);
            double cx = ay * vz - az * vy;
            double cy = az * vx - ax * vz;
            double cz = ax * vy - ay * vx;
            vx = vx * cos + cx * sin + ax * along;
            vy = vy * cos + cy * sin + ay * along;
            vz = vz * cos + cz * sin + az * along;
        }
        double wx = this.t[9];
        double wy = this.t[10];
        double wz = this.t[11];
        double angle = Math.atan2((vy * wz - vz * wy) * dx + (vz * wx - vx * wz) * dy + (vx * wy - vy * wx) * dz,
                vx * wx + vy * wy + vz * wz);
        double held = Math.max(this.twistMin, Math.min(this.twistMax, angle));
        if (angle == held) {
            return;
        }
        double over = angle - held;
        world.turn(this.a, this.b, dx * over, dy * over, dz * over, 0.0, h);
    }
}
