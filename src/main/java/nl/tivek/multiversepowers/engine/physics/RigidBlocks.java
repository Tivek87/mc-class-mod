package nl.tivek.multiversepowers.engine.physics;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

// A rigid world against the blocks round it: corners and sharp edges pushed back out, then friction, and no bounce.
abstract class RigidBlocks extends RigidBodies {
    private static final int MOST_BLOCKS = 256;
    private static final int MOST_TOUCHES = 48;
    private static final int MOST_EDGE_POINTS = 4096;
    // How far apart the points are that stand for a block's sharp edges, in blocks: closer than the thinnest part.
    private static final double EDGE_STEP = 0.125;
    // Per body, the most blocks and edge points it is tested against in a step: only those it can reach, the nearest
    // first (a fast body reaches deep into the ground, and the top it lands on must be among them).
    private static final int NEAR_BLOCKS = 48;
    private static final int NEAR_EDGES = 768;
    // Room past how far a body can move by itself in a step, for what pulls it along (a joint, a pin).
    private static final double LEEWAY = 0.35;
    // A block's six faces all shut: nothing reaches it before the blocks round it.
    private static final int BURIED = 63;
    // Room for how far the pushes out of blocks may move a body in one pass.
    private static final double ROOM = 0.25;
    // A point in a block is stopped as far as it came in this substep, as by a blow; the rest of the way out (it was
    // put there: built in a wall, carried into one) the body is only moved, this much in all a substep. Pushed out
    // further at once, it would take the push as speed and fly off.
    private static final double EASE_OUT = 0.005;

    final double[] touchLocal = new double[MOST_TOUCHES * 3];
    final double[] touchNormal = new double[MOST_TOUCHES * 3];
    final double[] touchDepth = new double[MOST_TOUCHES];
    int touches;
    // Each body's touches in this substep (its own axes, the world normal and how deep it went), for friction and the
    // pass on its speed.
    final double[] contactLocal = new double[MOST * MOST_TOUCHES * 3];
    final double[] contactNormal = new double[MOST * MOST_TOUCHES * 3];
    final double[] contactDepth = new double[MOST * MOST_TOUCHES];
    final int[] contacts = new int[MOST];
    final double[] blocks = new double[MOST_BLOCKS * 6];
    int blockCount;
    // Per block, which of its faces (axis * 2 + 0 for the low side, + 1 for the high) lie against another whole block.
    final int[] covered = new int[MOST_BLOCKS];
    final Long2IntOpenHashMap cells = new Long2IntOpenHashMap();
    final double[] edgePoints = new double[MOST_EDGE_POINTS * 3];
    int edgeCount;
    final int[] nearBlocks = new int[MOST * NEAR_BLOCKS];
    final int[] nearBlockCount = new int[MOST];
    private final double[] nearBlockFar = new double[NEAR_BLOCKS];
    private final int[] reached = new int[NEAR_BLOCKS];
    final int[] nearEdges = new int[MOST * NEAR_EDGES];
    final int[] nearEdgeCount = new int[MOST];
    // How much a body may still be moved out of blocks without speed this substep.
    private double ease;
    // The step's hardest hit on a block: how hard (hit()), how fast the point went in (blocks a second), the body, and
    // the point and the face's normal it was pushed out by.
    double hitWeighed;
    double hitSpeed;
    int hitBody = -1;
    final double[] hit = new double[6];

    void gather(Blocks world, double dt) {
        this.collect(world, dt);
        this.seal();
        this.sortOut(dt);
    }

    // The solid boxes the bodies can reach in a step of dt seconds; with dt 0, those round them as they lie.
    void collect(Blocks world, double dt) {
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
    }

    // How far the middle of body o (its first coordinate's index) is from block k's box, squared.
    private double away(int k, int o) {
        int e = k * 6;
        double far = 0.0;
        for (int a = 0; a < 3; a++) {
            double p = this.x[o + a];
            double d = p < this.blocks[e + a] ? this.blocks[e + a] - p
                    : p > this.blocks[e + 3 + a] ? p - this.blocks[e + 3 + a] : 0.0;
            far += d * d;
        }
        return far;
    }

    // For each body, the blocks and edge points it can touch during the coming step.
    void sortOut(double dt) {
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
            int furthest = -1;
            for (int k = 0; k < this.blockCount; k++) {
                int e = k * 6;
                if (this.covered[k] == BURIED || this.blocks[e] > this.x[o] + reach
                        || this.blocks[e + 3] < this.x[o] - reach || this.blocks[e + 1] > this.x[o + 1] + reach
                        || this.blocks[e + 4] < this.x[o + 1] - reach || this.blocks[e + 2] > this.x[o + 2] + reach
                        || this.blocks[e + 5] < this.x[o + 2] - reach) {
                    continue;
                }
                double far = this.away(k, o);
                if (blocks < NEAR_BLOCKS) {
                    this.nearBlockFar[blocks] = far;
                    this.nearBlocks[b * NEAR_BLOCKS + blocks++] = k;
                    furthest = -1;
                    continue;
                }
                if (furthest < 0) {
                    furthest = 0;
                    for (int i = 1; i < NEAR_BLOCKS; i++) {
                        if (this.nearBlockFar[i] > this.nearBlockFar[furthest]) {
                            furthest = i;
                        }
                    }
                }
                if (far < this.nearBlockFar[furthest]) {
                    this.nearBlockFar[furthest] = far;
                    this.nearBlocks[b * NEAR_BLOCKS + furthest] = k;
                    furthest = -1;
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
    void seal() {
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
    boolean whole(int k) {
        int e = k * 6;
        return this.blocks[e + 3] - this.blocks[e] == 1.0 && this.blocks[e + 4] - this.blocks[e + 1] == 1.0
                && this.blocks[e + 5] - this.blocks[e + 2] == 1.0 && this.blocks[e] == Math.floor(this.blocks[e])
                && this.blocks[e + 1] == Math.floor(this.blocks[e + 1])
                && this.blocks[e + 2] == Math.floor(this.blocks[e + 2]);
    }

    private static long cell(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (long) y & 0xFFF;
    }

    // Pushing a body out of the ground moves it, and the move would be kept as a speed away from the ground, so it
    // would bounce: the speed away from each surface it touched this substep is taken off at that point (a
    // restitution of nothing, as in Mueller et al.'s velocity pass).
    void unbounce(int b) {
        int o = b * 3;
        for (int pass = 0; pass < 2; pass++) {
            for (int c = 0; c < this.contacts[b]; c++) {
                int k = (b * MOST_TOUCHES + c) * 3;
                Quat.rotate(this.q, b * 4, this.contactLocal[k], this.contactLocal[k + 1], this.contactLocal[k + 2],
                        this.t1, 0);
                double rx = this.t1[0];
                double ry = this.t1[1];
                double rz = this.t1[2];
                double nx = this.contactNormal[k];
                double ny = this.contactNormal[k + 1];
                double nz = this.contactNormal[k + 2];
                double vx = this.v[o] + this.w[o + 1] * rz - this.w[o + 2] * ry;
                double vy = this.v[o + 1] + this.w[o + 2] * rx - this.w[o] * rz;
                double vz = this.v[o + 2] + this.w[o] * ry - this.w[o + 1] * rx;
                double away = vx * nx + vy * ny + vz * nz;
                if (away <= 0.0) {
                    continue;
                }
                double weight = this.weight(b, this.x[o] + rx, this.x[o + 1] + ry, this.x[o + 2] + rz, nx, ny, nz);
                if (weight <= 0.0) {
                    continue;
                }
                double j = -away / weight;
                this.v[o] += nx * j * this.invMass[b];
                this.v[o + 1] += ny * j * this.invMass[b];
                this.v[o + 2] += nz * j * this.invMass[b];
                Quat.unrotate(this.q, b * 4, (ry * nz - rz * ny) * j, (rz * nx - rx * nz) * j,
                        (rx * ny - ry * nx) * j, this.t3, 0);
                Quat.rotate(this.q, b * 4, this.t3[0] * this.invInertia[o], this.t3[1] * this.invInertia[o + 1],
                        this.t3[2] * this.invInertia[o + 2], this.t3, 0);
                this.w[o] += this.t3[0];
                this.w[o + 1] += this.t3[1];
                this.w[o + 2] += this.t3[2];
            }
        }
    }

    void keepContacts(int b) {
        int start = this.contacts[b];
        int n = Math.min(this.touches, MOST_TOUCHES - start);
        for (int t = 0; t < n; t++) {
            int from = t * 3;
            int to = (b * MOST_TOUCHES + start + t) * 3;
            System.arraycopy(this.touchLocal, from, this.contactLocal, to, 3);
            System.arraycopy(this.touchNormal, from, this.contactNormal, to, 3);
            this.contactDepth[b * MOST_TOUCHES + start + t] = this.touchDepth[t];
        }
        this.contacts[b] = start + n;
    }

    // Each corner of the box that went into a block is pushed back out through an open face (the one it came in by,
    // else the shallowest), and every point of a block's sharp edge that went into the box pushes the box off it, so a
    // box lying across the edge of a step rests on it instead of sinking in. Each touch is kept for friction().
    void touchBlocks(int b) {
        if (this.invMass[b] == 0.0 || this.blockCount == 0 || this.ghost[b]) {
            return;
        }
        int o = b * 3;
        this.touches = 0;
        this.ease = EASE_OUT;
        // Only the blocks its round bound reaches can hold a corner, with room for the pushes out on the way.
        double round = Math.sqrt(this.half[o] * this.half[o] + this.half[o + 1] * this.half[o + 1]
                + this.half[o + 2] * this.half[o + 2]) + ROOM;
        int reached = 0;
        for (int n = 0; n < this.nearBlockCount[b]; n++) {
            int k = this.nearBlocks[b * NEAR_BLOCKS + n];
            if (this.away(k, o) <= round * round) {
                this.reached[reached++] = k;
            }
        }
        for (int c = 0; c < 8 && reached > 0; c++) {
            double lx = (c & 1) == 0 ? -this.half[o] : this.half[o];
            double ly = (c & 2) == 0 ? -this.half[o + 1] : this.half[o + 1];
            double lz = (c & 4) == 0 ? -this.half[o + 2] : this.half[o + 2];
            this.point(b, lx, ly, lz, this.t1);
            for (int n = 0; n < reached; n++) {
                int k = this.reached[n];
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
                this.hit(b, cx, cy, cz, nx, ny, nz);
                double out = this.out(b, cx, cy, cz, nx, ny, nz, depth, (ox - cx) * nx + (oy - cy) * ny
                        + (oz - cz) * nz);
                this.point(b, lx, ly, lz, this.t1);
                if (out > 0.0) {
                    this.touch(lx, ly, lz, nx, ny, nz, out);
                }
            }
        }
        this.edgesInto(b);
        this.keepContacts(b);
    }

    // Body b's point c went into a block whose face turns n: kept when it is the step's hardest hit yet, by how fast
    // the point went in times the root of the body's weight (a falling trunk outweighs a flopping hand).
    private void hit(int b, double cx, double cy, double cz, double nx, double ny, double nz) {
        int o = b * 3;
        double rx = cx - this.x[o];
        double ry = cy - this.x[o + 1];
        double rz = cz - this.x[o + 2];
        double speed = -((this.v[o] + this.w[o + 1] * rz - this.w[o + 2] * ry) * nx
                + (this.v[o + 1] + this.w[o + 2] * rx - this.w[o] * rz) * ny
                + (this.v[o + 2] + this.w[o] * ry - this.w[o + 1] * rx) * nz);
        double weighed = speed / Math.sqrt(this.invMass[b]);
        if (speed > 0.0 && weighed > this.hitWeighed) {
            this.hitWeighed = weighed;
            this.hitSpeed = speed;
            this.hitBody = b;
            this.hit[0] = cx;
            this.hit[1] = cy;
            this.hit[2] = cz;
            this.hit[3] = nx;
            this.hit[4] = ny;
            this.hit[5] = nz;
        }
    }

    // Moves body b's point p out along n by `depth`: stopped as far as it came in (`into`, this substep, along -n),
    // only moved the rest, as far as this substep's easing still allows. Returns how far it went out.
    private double out(int b, double px, double py, double pz, double nx, double ny, double nz, double depth,
            double into) {
        double blow = Math.min(depth, Math.max(0.0, into));
        double slide = Math.min(depth - blow, this.ease);
        this.ease -= slide;
        if (blow > 0.0) {
            this.pushOut(b, px, py, pz, nx, ny, nz, blow);
        }
        if (slide > 0.0) {
            // Moved where it was a substep ago too: no speed from it.
            int o = b * 3;
            this.x[o] += nx * slide;
            this.x[o + 1] += ny * slide;
            this.x[o + 2] += nz * slide;
            this.px[o] += nx * slide;
            this.px[o + 1] += ny * slide;
            this.px[o + 2] += nz * slide;
        }
        return blow + slide;
    }

    // Each of the substep's touches holds its point against sliding since the substep began, as far as friction
    // reaches, once every touch is out and every joint joined again. Held any earlier, the tilt a later push-out gives
    // the box, or a joint joined again, would move a body lying still a little every substep, and it would creep and
    // turn over the ground.
    void friction(int b) {
        if (this.invMass[b] == 0.0) {
            return;
        }
        int o = b * 3;
        for (int c = 0; c < this.contacts[b]; c++) {
            int k = (b * MOST_TOUCHES + c) * 3;
            double lx = this.contactLocal[k];
            double ly = this.contactLocal[k + 1];
            double lz = this.contactLocal[k + 2];
            this.point(b, lx, ly, lz, this.t1);
            Quat.rotate(this.pq, b * 4, lx, ly, lz, this.t2, 0);
            double nx = this.contactNormal[k];
            double ny = this.contactNormal[k + 1];
            double nz = this.contactNormal[k + 2];
            double dx = this.t1[0] - this.px[o] - this.t2[0];
            double dy = this.t1[1] - this.px[o + 1] - this.t2[1];
            double dz = this.t1[2] - this.px[o + 2] - this.t2[2];
            double along = dx * nx + dy * ny + dz * nz;
            double sx = dx - nx * along;
            double sy = dy - ny * along;
            double sz = dz - nz * along;
            double slide = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (slide > 1.0E-9) {
                double held = Math.min(slide, this.friction * this.contactDepth[b * MOST_TOUCHES + c]);
                this.pushOut(b, this.t1[0], this.t1[1], this.t1[2], -sx / slide, -sy / slide, -sz / slide, held);
            }
        }
    }

    // The face (axis * 2 + side) a corner at c that was at o a substep ago leaves block k by: an open one it came in
    // through, else the shallowest open one, else (all shut) the shallowest.
    int exit(int k, double cx, double cy, double cz, double ox, double oy, double oz) {
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
    void edgesInto(int b) {
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
            double nx = this.t2[0];
            double ny = this.t2[1];
            double nz = this.t2[2];
            // Where the box's point now on the edge was a substep ago: how far the box came onto the edge.
            Quat.rotate(this.pq, b * 4, lx, ly, lz, this.t1, 0);
            double into = (ex - this.px[o] - this.t1[0]) * -nx + (ey - this.px[o + 1] - this.t1[1]) * -ny
                    + (ez - this.px[o + 2] - this.t1[2]) * -nz;
            double out = this.out(b, ex, ey, ez, nx, ny, nz, depth, into);
            if (out > 0.0) {
                this.touch(lx, ly, lz, nx, ny, nz, out);
            }
        }
    }

    void touch(double lx, double ly, double lz, double nx, double ny, double nz, double depth) {
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
}
