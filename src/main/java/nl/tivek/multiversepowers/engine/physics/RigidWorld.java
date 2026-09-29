package nl.tivek.multiversepowers.engine.physics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Rigid boxes held together by constraints, stepped in small substeps of XPBD (Mueller et al., Detailed Rigid Body
// Simulation with Extended Position Based Dynamics, 2020; Macklin et al., Small Steps in Physics Simulation, 2019).
// Every number lives in flat arrays so a step makes no garbage. Lengths in blocks, time in seconds.
public final class RigidWorld extends RigidCrowd {
    // No part ever moves faster than this (blocks per second), whatever goes wrong: a body never flies off.
    private static final double FASTEST = 80.0;
    // Nor turns faster than this (radians per second): a rigid box coming down hard on one corner takes the whole blow
    // as spin and cartwheels off, where flesh gives.
    private static final double MOST_SPIN = 20.0;
    // Sleeps once no part got further than this (blocks, its far corners too) from where it was QUIET_STEPS steps
    // ago: a body lying all but still that only trembles or creeps a hair is let sleep.
    private static final double STILL = 0.05;
    private static final int QUIET_STEPS = 12;
    // A sleeping world stepped once to see whether it still rests on something wakes up when a part got further than
    // this (blocks): falling for a step from rest goes several times as far.
    private static final double SLIPPED = 0.01;

    private final double[] sx = new double[MOST * 3];
    private final double[] sq = new double[MOST * 4];
    private final double[] keptX = new double[MOST * 3];
    private final double[] keptQ = new double[MOST * 4];
    private final List<Constraint> constraints = new ArrayList<>();
    private int quiet;
    private boolean sleeping;

    public int add(double mass, double hx, double hy, double hz) {
        if (this.count >= MOST) {
            throw new IllegalStateException("At most " + MOST + " bodies");
        }
        int b = this.count++;
        this.half[b * 3] = hx;
        this.half[b * 3 + 1] = hy;
        this.half[b * 3 + 2] = hz;
        this.invMass[b] = mass > 0.0 ? 1.0 / mass : 0.0;
        double ix = mass / 3.0 * (hy * hy + hz * hz);
        double iy = mass / 3.0 * (hx * hx + hz * hz);
        double iz = mass / 3.0 * (hx * hx + hy * hy);
        this.invInertia[b * 3] = ix > 0.0 ? 1.0 / ix : 0.0;
        this.invInertia[b * 3 + 1] = iy > 0.0 ? 1.0 / iy : 0.0;
        this.invInertia[b * 3 + 2] = iz > 0.0 ? 1.0 / iz : 0.0;
        this.place(b, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0);
        return b;
    }

    public void place(int b, double x, double y, double z, double qx, double qy, double qz, double qw) {
        int o = b * 3;
        this.x[o] = x;
        this.x[o + 1] = y;
        this.x[o + 2] = z;
        System.arraycopy(this.x, o, this.px, o, 3);
        int r = b * 4;
        this.q[r] = qx;
        this.q[r + 1] = qy;
        this.q[r + 2] = qz;
        this.q[r + 3] = qw;
        Quat.normalize(this.q, r);
        System.arraycopy(this.q, r, this.pq, r, 4);
        this.v[o] = 0.0;
        this.v[o + 1] = 0.0;
        this.v[o + 2] = 0.0;
        this.w[o] = 0.0;
        this.w[o + 1] = 0.0;
        this.w[o + 2] = 0.0;
    }

    public void velocity(int b, double vx, double vy, double vz, double wx, double wy, double wz) {
        int o = b * 3;
        this.v[o] = vx;
        this.v[o + 1] = vy;
        this.v[o + 2] = vz;
        this.w[o] = wx;
        this.w[o + 1] = wy;
        this.w[o + 2] = wz;
        this.wake();
    }

    public void add(Constraint constraint) {
        this.constraints.add(constraint);
    }

    public int count() {
        return this.count;
    }

    // Moves every body the same way at once, as if the whole world were carried there.
    public void shift(double dx, double dy, double dz) {
        for (int b = 0; b < this.count; b++) {
            int o = b * 3;
            this.x[o] += dx;
            this.x[o + 1] += dy;
            this.x[o + 2] += dz;
        }
    }

    public boolean sleeping() {
        return this.sleeping;
    }

    // How many steps in a row nothing moved much: a body lying all but still counts some before it sleeps.
    public int quiet() {
        return this.sleeping ? QUIET_STEPS : this.quiet;
    }

    public void wake() {
        this.sleeping = false;
        this.quiet = 0;
    }

    // Whether any body touched a block in the last substep (lying still, when it went to sleep).
    public boolean touching() {
        for (int b = 0; b < this.count; b++) {
            if (this.contacts[b] > 0) {
                return true;
            }
        }
        return false;
    }

    // Whether body b touched a block or another body in the last substep.
    public boolean touching(int b) {
        return this.contacts[b] > 0;
    }

    // A sleeping world looks whether it still rests on something (the block under it may have been broken): it takes
    // one step, and wakes if anything slipped; else it is put back as it lay and sleeps on.
    public void probe(double dt, int substeps, Blocks world) {
        if (!this.sleeping || this.count == 0) {
            return;
        }
        System.arraycopy(this.x, 0, this.keptX, 0, this.count * 3);
        System.arraycopy(this.q, 0, this.keptQ, 0, this.count * 4);
        this.wake();
        this.step(dt, substeps, world);
        if (this.moved() > SLIPPED) {
            return;
        }
        System.arraycopy(this.keptX, 0, this.x, 0, this.count * 3);
        System.arraycopy(this.keptQ, 0, this.q, 0, this.count * 4);
        Arrays.fill(this.v, 0, this.count * 3, 0.0);
        Arrays.fill(this.w, 0, this.count * 3, 0.0);
        this.sleeping = true;
        this.quiet = QUIET_STEPS;
    }

    // Position then orientation of body b: x, y, z, qx, qy, qz, qw.
    public void pose(int b, double[] out) {
        System.arraycopy(this.x, b * 3, out, 0, 3);
        System.arraycopy(this.q, b * 4, out, 3, 4);
    }

    public void velocity(int b, double[] out) {
        System.arraycopy(this.v, b * 3, out, 0, 3);
        System.arraycopy(this.w, b * 3, out, 3, 3);
    }

    public double half(int b, int axis) {
        return this.half[b * 3 + axis];
    }

    // 0 for a body that never moves.
    public double mass(int b) {
        return this.invMass[b] > 0.0 ? 1.0 / this.invMass[b] : 0.0;
    }

    public void step(double dt, int substeps, Blocks world) {
        if (this.count == 0 || this.sleeping) {
            return;
        }
        this.gather(world, dt);
        this.sortOthers(dt);
        if (this.quiet == 0) {
            System.arraycopy(this.x, 0, this.sx, 0, this.count * 3);
            System.arraycopy(this.q, 0, this.sq, 0, this.count * 4);
        }
        double h = dt / substeps;
        for (int s = 0; s < substeps; s++) {
            this.leadWas = s * h;
            this.lead = (s + 1) * h;
            Arrays.fill(this.contacts, 0, this.count, 0);
            for (int b = 0; b < this.count; b++) {
                this.integrate(b, h);
            }
            for (Constraint constraint : this.constraints) {
                constraint.solve(this, h);
            }
            for (int b = 0; b < this.count; b++) {
                this.touchBlocks(b);
                this.touchOthers(b);
            }
            for (Constraint constraint : this.constraints) {
                constraint.rejoin(this, h);
            }
            // Joining the joints again can push a part back into a block: out once more, then friction for every
            // touch of the substep.
            for (int b = 0; b < this.count; b++) {
                this.touchBlocks(b);
                this.touchOthers(b);
            }
            for (int b = 0; b < this.count; b++) {
                this.friction(b);
            }
            for (int b = 0; b < this.count; b++) {
                this.settle(b, h);
            }
        }
        if (this.moved() < STILL) {
            if (++this.quiet >= QUIET_STEPS) {
                this.sleeping = true;
                Arrays.fill(this.v, 0, this.count * 3, 0.0);
                Arrays.fill(this.w, 0, this.count * 3, 0.0);
            }
        } else {
            this.quiet = 0;
        }
    }

    // How far the body that moved most got since the quiet steps began, its far corners too. Only where it got
    // counts, not the solver's back and forth on the way, or a body lying still would never be let sleep.
    private double moved() {
        double furthest = 0.0;
        for (int b = 0; b < this.count; b++) {
            if (this.invMass[b] == 0.0) {
                continue;
            }
            int o = b * 3;
            int r = b * 4;
            double dx = this.x[o] - this.sx[o];
            double dy = this.x[o + 1] - this.sx[o + 1];
            double dz = this.x[o + 2] - this.sx[o + 2];
            double dot = Math.abs(this.q[r] * this.sq[r] + this.q[r + 1] * this.sq[r + 1]
                    + this.q[r + 2] * this.sq[r + 2]
                    + this.q[r + 3] * this.sq[r + 3]);
            double turned = 2.0 * Math.acos(Math.min(1.0, dot));
            double reach = Math.max(this.half[o], Math.max(this.half[o + 1], this.half[o + 2]));
            furthest = Math.max(furthest, Math.sqrt(dx * dx + dy * dy + dz * dz) + turned * reach);
        }
        return furthest;
    }

    // New velocities from how far the body got this substep, then damped.
    private void settle(int b, double h) {
        int o = b * 3;
        if (this.invMass[b] == 0.0) {
            return;
        }
        this.v[o] = (this.x[o] - this.px[o]) / h;
        this.v[o + 1] = (this.x[o + 1] - this.px[o + 1]) / h;
        this.v[o + 2] = (this.x[o + 2] - this.px[o + 2]) / h;
        Quat.velocity(this.q, b * 4, this.pq, b * 4, h, this.w, o);
        this.unbounce(b);
        double keep = Math.max(0.0, 1.0 - this.linearDamping * h);
        double turn = Math.max(0.0, 1.0 - (this.angularDamping + (this.contacts[b] > 0 ? this.contactDamping : 0.0))
                * h);
        for (int i = 0; i < 3; i++) {
            this.v[o + i] *= keep;
            this.w[o + i] *= turn;
        }
        double speed = Math.sqrt(this.v[o] * this.v[o] + this.v[o + 1] * this.v[o + 1] + this.v[o + 2] * this.v[o + 2]);
        if (speed > FASTEST) {
            double scale = FASTEST / speed;
            this.v[o] *= scale;
            this.v[o + 1] *= scale;
            this.v[o + 2] *= scale;
        }
        double spin = Math.sqrt(this.w[o] * this.w[o] + this.w[o + 1] * this.w[o + 1] + this.w[o + 2] * this.w[o + 2]);
        if (spin > MOST_SPIN) {
            double scale = MOST_SPIN / spin;
            this.w[o] *= scale;
            this.w[o + 1] *= scale;
            this.w[o + 2] *= scale;
        }
    }

    public void clear() {
        this.count = 0;
        this.constraints.clear();
        this.sleeping = false;
        this.quiet = 0;
    }
}
