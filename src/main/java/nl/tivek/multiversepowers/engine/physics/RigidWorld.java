package nl.tivek.multiversepowers.engine.physics;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;

// Rigid boxes held together by constraints, stepped in small substeps of XPBD (Mueller et al., Detailed Rigid Body
// Simulation with Extended Position Based Dynamics, 2020; Macklin et al., Small Steps in Physics Simulation, 2019).
// Every number lives in flat arrays so a step makes no garbage. Lengths in blocks, time in seconds.
public final class RigidWorld {
    public static final int MOST = 32;
    private static final int MOST_BLOCKS = 96;
    private static final int MOST_TOUCHES = 48;
    private static final int MOST_EDGE_POINTS = 4096;
    // How far apart the points are that stand for a block's sharp edges, in blocks: closer than the thinnest part.
    private static final double EDGE_STEP = 0.125;
    // Per body, the most blocks and edge points it is tested against in a step: only those it can reach.
    private static final int NEAR_BLOCKS = 32;
    private static final int NEAR_EDGES = 768;
    // Room past how far a body can move by itself in a step, for what pulls it along (a joint, a pin).
    private static final double LEEWAY = 0.35;
    // No part ever moves faster than this (blocks per second), whatever goes wrong: a body never flies off.
    private static final double FASTEST = 80.0;
    private static final double QUIET = 0.08;
    private static final int QUIET_STEPS = 12;

    final double[] x = new double[MOST * 3];
    final double[] px = new double[MOST * 3];
    final double[] v = new double[MOST * 3];
    final double[] q = new double[MOST * 4];
    final double[] pq = new double[MOST * 4];
    final double[] w = new double[MOST * 3];
    final double[] invMass = new double[MOST];
    final double[] invInertia = new double[MOST * 3];
    final double[] half = new double[MOST * 3];
    private final double[] sx = new double[MOST * 3];
    private final double[] sq = new double[MOST * 4];
    private final double[] touchLocal = new double[MOST_TOUCHES * 3];
    private final double[] touchNormal = new double[MOST_TOUCHES * 3];
    private final double[] touchDepth = new double[MOST_TOUCHES];
    private int touches;
    private boolean frictionless;
    private int count;
    private final List<Constraint> constraints = new ArrayList<>();
    private final double[] blocks = new double[MOST_BLOCKS * 6];
    private int blockCount;
    // Per block, which of its faces (axis * 2 + 0 for the low side, + 1 for the high) lie against another whole block.
    private final int[] covered = new int[MOST_BLOCKS];
    private final Long2IntOpenHashMap cells = new Long2IntOpenHashMap();
    private final double[] edgePoints = new double[MOST_EDGE_POINTS * 3];
    private int edgeCount;
    private final int[] nearBlocks = new int[MOST * NEAR_BLOCKS];
    private final int[] nearBlockCount = new int[MOST];
    private final int[] nearEdges = new int[MOST * NEAR_EDGES];
    private final int[] nearEdgeCount = new int[MOST];
    private final double[] t1 = new double[3];
    private final double[] t2 = new double[3];
    private final double[] t3 = new double[3];
    public double gravity = -24.0;
    public double friction = 0.6;
    public double linearDamping = 0.4;
    public double angularDamping = 1.2;
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

    public void wake() {
        this.sleeping = false;
        this.quiet = 0;
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

    // A point given in body b's own axes, in the world.
    public void point(int b, double lx, double ly, double lz, double[] out) {
        Quat.rotate(this.q, b * 4, lx, ly, lz, out, 0);
        out[0] += this.x[b * 3];
        out[1] += this.x[b * 3 + 1];
        out[2] += this.x[b * 3 + 2];
    }

    public void step(double dt, int substeps, Blocks world) {
        if (this.count == 0 || this.sleeping) {
            return;
        }
        this.gather(world, dt);
        System.arraycopy(this.x, 0, this.sx, 0, this.count * 3);
        System.arraycopy(this.q, 0, this.sq, 0, this.count * 4);
        double h = dt / substeps;
        for (int s = 0; s < substeps; s++) {
            for (int b = 0; b < this.count; b++) {
                this.integrate(b, h);
            }
            for (Constraint constraint : this.constraints) {
                constraint.solve(this, h);
            }
            for (int b = 0; b < this.count; b++) {
                this.touchBlocks(b);
            }
            for (Constraint constraint : this.constraints) {
                constraint.rejoin(this, h);
            }
            // Joining the joints again can push a part back into a block: out once more, without friction, which a
            // second time would hold a body lying still against nothing and let it creep.
            this.frictionless = true;
            for (int b = 0; b < this.count; b++) {
                this.touchBlocks(b);
            }
            this.frictionless = false;
            for (int b = 0; b < this.count; b++) {
                this.settle(b, h);
            }
        }
        if (this.moved(dt) < QUIET) {
            if (++this.quiet >= QUIET_STEPS) {
                this.sleeping = true;
                java.util.Arrays.fill(this.v, 0, this.count * 3, 0.0);
                java.util.Arrays.fill(this.w, 0, this.count * 3, 0.0);
            }
        } else {
            this.quiet = 0;
        }
    }

    private void gather(Blocks world, double dt) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int b = 0; b < this.count; b++) {
            int o = b * 3;
            double reach = Math.sqrt(this.half[o] * this.half[o] + this.half[o + 1] * this.half[o + 1]
                    + this.half[o + 2] * this.half[o + 2]);
            double sway = Math.sqrt(this.v[o] * this.v[o] + this.v[o + 1] * this.v[o + 1] + this.v[o + 2] * this.v[o
                    + 2]) * dt + 0.5 * Math.abs(this.gravity) * dt * dt;
            minX = Math.min(minX, this.x[o] - reach - sway);
            minY = Math.min(minY, this.x[o + 1] - reach - sway);
            minZ = Math.min(minZ, this.x[o + 2] - reach - sway);
            maxX = Math.max(maxX, this.x[o] + reach + sway);
            maxY = Math.max(maxY, this.x[o + 1] + reach + sway);
            maxZ = Math.max(maxZ, this.x[o + 2] + reach + sway);
        }
        this.blockCount = Math.min(MOST_BLOCKS, world.collect(minX, minY, minZ, maxX, maxY, maxZ, this.blocks));
        this.seal();
        this.sortOut(dt);
    }

    // For each body, the blocks and edge points it can touch during the coming step.
    private void sortOut(double dt) {
        for (int b = 0; b < this.count; b++) {
            int o = b * 3;
            double size = Math.sqrt(this.half[o] * this.half[o] + this.half[o + 1] * this.half[o + 1]
                    + this.half[o + 2] * this.half[o + 2]);
            double speed = Math.sqrt(this.v[o] * this.v[o] + this.v[o + 1] * this.v[o + 1] + this.v[o + 2] * this.v[o
                    + 2]);
            double spin = Math.sqrt(this.w[o] * this.w[o] + this.w[o + 1] * this.w[o + 1] + this.w[o + 2] * this.w[o
                    + 2]);
            double reach = size + (speed + spin * size) * dt + 0.5 * Math.abs(this.gravity) * dt * dt + LEEWAY;
            int blocks = 0;
            for (int k = 0; k < this.blockCount && blocks < NEAR_BLOCKS; k++) {
                int e = k * 6;
                if (this.blocks[e] <= this.x[o] + reach && this.blocks[e + 3] >= this.x[o] - reach
                        && this.blocks[e + 1] <= this.x[o + 1] + reach && this.blocks[e + 4] >= this.x[o + 1] - reach
                        && this.blocks[e + 2] <= this.x[o + 2] + reach && this.blocks[e + 5] >= this.x[o + 2] - reach) {
                    this.nearBlocks[b * NEAR_BLOCKS + blocks++] = k;
                }
            }
            this.nearBlockCount[b] = blocks;
            int edges = 0;
            double far = reach * reach;
            for (int i = 0; i < this.edgeCount && edges < NEAR_EDGES; i++) {
                double dx = this.edgePoints[i * 3] - this.x[o];
                double dy = this.edgePoints[i * 3 + 1] - this.x[o + 1];
                double dz = this.edgePoints[i * 3 + 2] - this.x[o + 2];
                if (dx * dx + dy * dy + dz * dz < far) {
                    this.nearEdges[b * NEAR_EDGES + edges++] = i;
                }
            }
            this.nearEdgeCount[b] = edges;
        }
    }

    // Finds which block faces are shut by a whole block next to them (a floor's seams, a wall's inside) and lays
    // points along every sharp edge left, where two open faces meet: the edge of a step or a wall's corner.
    private void seal() {
        this.cells.clear();
        this.edgeCount = 0;
        for (int k = 0; k < this.blockCount; k++) {
            this.covered[k] = 0;
            if (whole(k)) {
                this.cells.put(cell((int) Math.floor(this.blocks[k * 6]), (int) Math.floor(this.blocks[k * 6 + 1]),
                        (int) Math.floor(this.blocks[k * 6 + 2])), k);
            }
        }
        for (int k = 0; k < this.blockCount; k++) {
            if (!whole(k)) {
                continue;
            }
            int bx = (int) Math.floor(this.blocks[k * 6]);
            int by = (int) Math.floor(this.blocks[k * 6 + 1]);
            int bz = (int) Math.floor(this.blocks[k * 6 + 2]);
            for (int face = 0; face < 6; face++) {
                int step = (face & 1) == 0 ? -1 : 1;
                int axis = face >> 1;
                long next = cell(bx + (axis == 0 ? step : 0), by + (axis == 1 ? step : 0), bz + (axis == 2 ? step : 0));
                if (this.cells.containsKey(next)) {
                    this.covered[k] |= 1 << face;
                }
            }
        }
        for (int k = 0; k < this.blockCount; k++) {
            int e = k * 6;
            for (int along = 0; along < 3; along++) {
                int a = (along + 1) % 3;
                int c = (along + 2) % 3;
                for (int sa = 0; sa < 2; sa++) {
                    for (int sc = 0; sc < 2; sc++) {
                        if ((this.covered[k] & (1 << (a * 2 + sa) | 1 << (c * 2 + sc))) != 0) {
                            continue;
                        }
                        double low = this.blocks[e + along];
                        double high = this.blocks[e + 3 + along];
                        int points = Math.max(1, (int) Math.ceil((high - low) / EDGE_STEP));
                        for (int i = 0; i <= points && this.edgeCount < MOST_EDGE_POINTS; i++) {
                            int o = this.edgeCount++ * 3;
                            this.edgePoints[o + along] = low + (high - low) * i / points;
                            this.edgePoints[o + a] = sa == 0 ? this.blocks[e + a] : this.blocks[e + 3 + a];
                            this.edgePoints[o + c] = sc == 0 ? this.blocks[e + c] : this.blocks[e + 3 + c];
                        }
                    }
                }
            }
        }
    }

    // Whether block k is a whole block: one full cube on the block grid.
    private boolean whole(int k) {
        int e = k * 6;
        return this.blocks[e + 3] - this.blocks[e] == 1.0 && this.blocks[e + 4] - this.blocks[e + 1] == 1.0
                && this.blocks[e + 5] - this.blocks[e + 2] == 1.0 && this.blocks[e] == Math.floor(this.blocks[e])
                && this.blocks[e + 1] == Math.floor(this.blocks[e + 1])
                && this.blocks[e + 2] == Math.floor(this.blocks[e + 2]);
    }

    private static long cell(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (long) y & 0xFFF;
    }

    private void integrate(int b, double h) {
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

    // How fast the body that moved most got anywhere over the whole step, its far corners too. The solver's own
    // back and forth within a step does not count, or a body lying still would never be let sleep.
    private double moved(double dt) {
        double fastest = 0.0;
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
            fastest = Math.max(fastest, (Math.sqrt(dx * dx + dy * dy + dz * dz) + turned * reach) / dt);
        }
        return fastest;
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
        double keep = Math.max(0.0, 1.0 - this.linearDamping * h);
        double turn = Math.max(0.0, 1.0 - this.angularDamping * h);
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
    }

    // Each corner of the box that went into a block is pushed back out through an open face (the one it came in by,
    // else the shallowest), and every point of a block's sharp edge that went into the box pushes the box off it, so a
    // box lying across the edge of a step rests on it instead of sinking in. Then each touch holds the box against
    // sliding as far as friction reaches.
    private void touchBlocks(int b) {
        if (this.invMass[b] == 0.0 || this.blockCount == 0) {
            return;
        }
        int o = b * 3;
        this.touches = 0;
        for (int c = 0; c < 8; c++) {
            double lx = (c & 1) == 0 ? -this.half[o] : this.half[o];
            double ly = (c & 2) == 0 ? -this.half[o + 1] : this.half[o + 1];
            double lz = (c & 4) == 0 ? -this.half[o + 2] : this.half[o + 2];
            this.point(b, lx, ly, lz, this.t1);
            for (int n = 0; n < this.nearBlockCount[b]; n++) {
                int k = this.nearBlocks[b * NEAR_BLOCKS + n];
                int e = k * 6;
                double cx = this.t1[0];
                double cy = this.t1[1];
                double cz = this.t1[2];
                // A corner exactly on the line between two blocks belongs to the one above it, or to neither and
                // that corner would sink.
                if (cx < this.blocks[e] || cx >= this.blocks[e + 3] || cy < this.blocks[e + 1]
                        || cy >= this.blocks[e + 4] || cz < this.blocks[e + 2] || cz >= this.blocks[e + 5]) {
                    continue;
                }
                Quat.rotate(this.pq, b * 4, lx, ly, lz, this.t2, 0);
                double ox = this.px[o] + this.t2[0];
                double oy = this.px[o + 1] + this.t2[1];
                double oz = this.px[o + 2] + this.t2[2];
                int face = this.exit(k, cx, cy, cz, ox, oy, oz);
                int axis = face >> 1;
                double sign = (face & 1) == 0 ? -1.0 : 1.0;
                double p = axis == 0 ? cx : axis == 1 ? cy : cz;
                double depth = sign < 0.0 ? p - this.blocks[e + axis] : this.blocks[e + 3 + axis] - p;
                double nx = axis == 0 ? sign : 0.0;
                double ny = axis == 1 ? sign : 0.0;
                double nz = axis == 2 ? sign : 0.0;
                this.pushOut(b, cx, cy, cz, nx, ny, nz, depth);
                this.point(b, lx, ly, lz, this.t1);
                this.touch(lx, ly, lz, nx, ny, nz, depth);
            }
        }
        this.edgesInto(b);
        // Friction last, once every touch is out: held against a push-out on its own, the tilt that push gave the box
        // would be taken for sliding at the next touch, and a box lying still would creep and turn.
        for (int t = 0; t < (this.frictionless ? 0 : this.touches); t++) {
            double lx = this.touchLocal[t * 3];
            double ly = this.touchLocal[t * 3 + 1];
            double lz = this.touchLocal[t * 3 + 2];
            this.point(b, lx, ly, lz, this.t1);
            Quat.rotate(this.pq, b * 4, lx, ly, lz, this.t2, 0);
            double nx = this.touchNormal[t * 3];
            double ny = this.touchNormal[t * 3 + 1];
            double nz = this.touchNormal[t * 3 + 2];
            double dx = this.t1[0] - this.px[o] - this.t2[0];
            double dy = this.t1[1] - this.px[o + 1] - this.t2[1];
            double dz = this.t1[2] - this.px[o + 2] - this.t2[2];
            double along = dx * nx + dy * ny + dz * nz;
            double sx = dx - nx * along;
            double sy = dy - ny * along;
            double sz = dz - nz * along;
            double slide = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (slide > 1.0E-9) {
                double held = Math.min(slide, this.friction * this.touchDepth[t]);
                this.pushOut(b, this.t1[0], this.t1[1], this.t1[2], -sx / slide, -sy / slide, -sz / slide, held);
            }
        }
    }

    // The face (axis * 2 + side) a corner at c that was at o a substep ago leaves block k by: an open one it came in
    // through, else the shallowest open one, else (all shut) the shallowest.
    private int exit(int k, double cx, double cy, double cz, double ox, double oy, double oz) {
        int e = k * 6;
        int best = -1;
        int rank = -1;
        double depth = Double.POSITIVE_INFINITY;
        for (int face = 0; face < 6; face++) {
            int axis = face >> 1;
            boolean high = (face & 1) == 1;
            double p = axis == 0 ? cx : axis == 1 ? cy : cz;
            double was = axis == 0 ? ox : axis == 1 ? oy : oz;
            double d = high ? this.blocks[e + 3 + axis] - p : p - this.blocks[e + axis];
            boolean open = (this.covered[k] & 1 << face) == 0;
            boolean came = high ? was >= this.blocks[e + 3 + axis] : was <= this.blocks[e + axis];
            int r = (open ? 2 : 0) + (came ? 1 : 0);
            if (r > rank || r == rank && d < depth) {
                best = face;
                rank = r;
                depth = d;
            }
        }
        return best;
    }

    // Every point of a sharp block edge inside the box pushes the box off it through the box's nearest face.
    private void edgesInto(int b) {
        int o = b * 3;
        double reach = this.half[o] * this.half[o] + this.half[o + 1] * this.half[o + 1]
                + this.half[o + 2] * this.half[o + 2];
        for (int n = 0; n < this.nearEdgeCount[b]; n++) {
            int e = this.nearEdges[b * NEAR_EDGES + n] * 3;
            double ex = this.edgePoints[e];
            double ey = this.edgePoints[e + 1];
            double ez = this.edgePoints[e + 2];
            double rx = ex - this.x[o];
            double ry = ey - this.x[o + 1];
            double rz = ez - this.x[o + 2];
            if (rx * rx + ry * ry + rz * rz >= reach) {
                continue;
            }
            Quat.unrotate(this.q, b * 4, rx, ry, rz, this.t3, 0);
            int axis = -1;
            double depth = Double.POSITIVE_INFINITY;
            for (int a = 0; a < 3; a++) {
                double d = this.half[o + a] - Math.abs(this.t3[a]);
                if (d <= 0.0) {
                    axis = -1;
                    break;
                }
                if (d < depth) {
                    depth = d;
                    axis = a;
                }
            }
            if (axis < 0) {
                continue;
            }
            // The box moves away from the edge point: out along its face on the point's side, the other way.
            double sign = this.t3[axis] > 0.0 ? -1.0 : 1.0;
            this.t2[0] = axis == 0 ? sign : 0.0;
            this.t2[1] = axis == 1 ? sign : 0.0;
            this.t2[2] = axis == 2 ? sign : 0.0;
            Quat.rotate(this.q, b * 4, this.t2[0], this.t2[1], this.t2[2], this.t2, 0);
            double lx = this.t3[0];
            double ly = this.t3[1];
            double lz = this.t3[2];
            this.pushOut(b, ex, ey, ez, this.t2[0], this.t2[1], this.t2[2], depth);
            this.touch(lx, ly, lz, this.t2[0], this.t2[1], this.t2[2], depth);
        }
    }

    private void touch(double lx, double ly, double lz, double nx, double ny, double nz, double depth) {
        if (this.touches >= MOST_TOUCHES) {
            return;
        }
        int t = this.touches++;
        this.touchLocal[t * 3] = lx;
        this.touchLocal[t * 3 + 1] = ly;
        this.touchLocal[t * 3 + 2] = lz;
        this.touchNormal[t * 3] = nx;
        this.touchNormal[t * 3 + 1] = ny;
        this.touchNormal[t * 3 + 2] = nz;
        this.touchDepth[t] = depth;
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
    void attach(int i, double ax, double ay, double az, int j, double cx, double cy, double cz, double compliance,
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
    void separate(int i, double ax, double ay, double az, int j, double cx, double cy, double cz, double nx,
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
    void turn(int i, int j, double ex, double ey, double ez, double compliance, double h) {
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
    private double weight(int b, double px, double py, double pz, double nx, double ny, double nz) {
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

    private double angularWeight(int b, double nx, double ny, double nz) {
        if (this.invMass[b] == 0.0) {
            return 0.0;
        }
        int o = b * 3;
        Quat.unrotate(this.q, b * 4, nx, ny, nz, this.t3, 0);
        return this.t3[0] * this.t3[0] * this.invInertia[o] + this.t3[1] * this.t3[1] * this.invInertia[o + 1]
                + this.t3[2] * this.t3[2] * this.invInertia[o + 2];
    }

    // Applies the positional impulse (x, y, z) at a world point of body b: it moves and turns as its mass allows.
    private void apply(int b, double px, double py, double pz, double ix, double iy, double iz) {
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
    private void spinBy(int b, double x, double y, double z, double sign) {
        int o = b * 3;
        Quat.unrotate(this.q, b * 4, x, y, z, this.t3, 0);
        double bx = this.t3[0] * this.invInertia[o] * sign;
        double by = this.t3[1] * this.invInertia[o + 1] * sign;
        double bz = this.t3[2] * this.invInertia[o + 2] * sign;
        Quat.rotate(this.q, b * 4, bx, by, bz, this.t3, 0);
        Quat.spin(this.q, b * 4, this.t3[0], this.t3[1], this.t3[2]);
    }

    public void clear() {
        this.count = 0;
        this.constraints.clear();
        this.sleeping = false;
        this.quiet = 0;
    }
}
