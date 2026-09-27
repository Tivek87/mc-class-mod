package nl.tivek.multiversepowers.engine.physics;

// Points joined by rods that keep their lengths: a rope, a chain or a sheet of cloth. They fall, swing and flutter,
// keep out of blocks and out of the round bodies (capsules) given each step, and a pinned point goes where it is put,
// smoothly over the step. Positions as of the last two steps are kept, so drawing can blend between them.
public final class Strands {
    private static final int MOST_BLOCKS = 64;
    // How far out of a capsule a point is kept.
    private static final double SKIN = 0.01;

    private final int count;
    private final double[] x;
    private final double[] px;
    private final double[] v;
    private final double[] was;
    private final double[] now;
    private final double[] invMass;
    private final boolean[] pinned;
    private final double[] pinAt;
    private final double[] pinPace;
    private int[] rodA = new int[16];
    private int[] rodB = new int[16];
    private double[] rodLength = new double[16];
    private double[] rodSoft = new double[16];
    private int rods;
    private double[] capsules = new double[7 * 8];
    private int capsuleCount;
    private final double[] blocks = new double[MOST_BLOCKS * 6];
    private int blockCount;
    public double gravity = -24.0;
    // Air drag per second, and the wind the points are dragged towards (blocks per second).
    public double drag = 1.5;
    public double windX;
    public double windY;
    public double windZ;
    // How much of its speed along a block or capsule a point keeps when it touches it (0 = sticks, 1 = slides).
    public double slide = 0.6;

    public Strands(int count) {
        this.count = count;
        this.x = new double[count * 3];
        this.px = new double[count * 3];
        this.v = new double[count * 3];
        this.was = new double[count * 3];
        this.now = new double[count * 3];
        this.invMass = new double[count];
        this.pinned = new boolean[count];
        this.pinAt = new double[count * 3];
        this.pinPace = new double[count * 3];
        java.util.Arrays.fill(this.invMass, 1.0);
    }

    // A rope of `points` points, `length` long, hanging straight down from (x, y, z).
    public static Strands rope(int points, double length, double x, double y, double z) {
        Strands rope = new Strands(points);
        double step = length / (points - 1);
        for (int i = 0; i < points; i++) {
            rope.place(i, x, y - step * i, z);
            if (i > 0) {
                rope.rod(i - 1, i, step, 0.0);
            }
            if (i > 1) {
                rope.rod(i - 2, i, step * 2.0, 1.0E-4);
            }
        }
        return rope;
    }

    // A sheet `columns` by `rows` points, `width` wide along +x and `height` long down -y, its top left at (x, y, z):
    // point (c, r) is number r * columns + c. Rods along, down and across each square keep its shape; the longer ones
    // are soft, so it folds.
    public static Strands cloth(int columns, int rows, double width, double height, double x, double y, double z) {
        Strands cloth = new Strands(columns * rows);
        double dx = width / (columns - 1);
        double dy = height / (rows - 1);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int i = r * columns + c;
                cloth.place(i, x + dx * c, y - dy * r, z);
                if (c > 0) {
                    cloth.rod(i - 1, i, dx, 0.0);
                }
                if (r > 0) {
                    cloth.rod(i - columns, i, dy, 0.0);
                }
                if (c > 0 && r > 0) {
                    double diagonal = Math.sqrt(dx * dx + dy * dy);
                    cloth.rod(i - columns - 1, i, diagonal, 2.0E-5);
                    cloth.rod(i - columns, i - 1, diagonal, 2.0E-5);
                }
                if (c > 1) {
                    cloth.rod(i - 2, i, dx * 2.0, 2.0E-4);
                }
                if (r > 1) {
                    cloth.rod(i - 2 * columns, i, dy * 2.0, 2.0E-4);
                }
            }
        }
        return cloth;
    }

    public int count() {
        return this.count;
    }

    // A rod keeping points a and b `length` apart; compliance 0 is stiff, more lets it stretch and bend.
    public int rod(int a, int b, double length, double compliance) {
        if (this.rods == this.rodA.length) {
            int more = this.rods * 2;
            this.rodA = java.util.Arrays.copyOf(this.rodA, more);
            this.rodB = java.util.Arrays.copyOf(this.rodB, more);
            this.rodLength = java.util.Arrays.copyOf(this.rodLength, more);
            this.rodSoft = java.util.Arrays.copyOf(this.rodSoft, more);
        }
        this.rodA[this.rods] = a;
        this.rodB[this.rods] = b;
        this.rodLength[this.rods] = length;
        this.rodSoft[this.rods] = compliance;
        return this.rods++;
    }

    public void place(int i, double x, double y, double z) {
        int o = i * 3;
        this.x[o] = x;
        this.x[o + 1] = y;
        this.x[o + 2] = z;
        System.arraycopy(this.x, o, this.px, o, 3);
        System.arraycopy(this.x, o, this.was, o, 3);
        System.arraycopy(this.x, o, this.now, o, 3);
        this.v[o] = 0.0;
        this.v[o + 1] = 0.0;
        this.v[o + 2] = 0.0;
    }

    // Holds point i at a place that moves evenly from where it is to (x, y, z) over the next step.
    public void pin(int i, double x, double y, double z, double dt) {
        int o = i * 3;
        if (!this.pinned[i]) {
            this.pinned[i] = true;
            System.arraycopy(this.x, o, this.pinAt, o, 3);
        }
        this.pinPace[o] = (x - this.pinAt[o]) / dt;
        this.pinPace[o + 1] = (y - this.pinAt[o + 1]) / dt;
        this.pinPace[o + 2] = (z - this.pinAt[o + 2]) / dt;
    }

    public void release(int i) {
        this.pinned[i] = false;
    }

    public void clearCapsules() {
        this.capsuleCount = 0;
    }

    // A round body the points stay out of this step: from a to b, with a radius.
    public void capsule(double ax, double ay, double az, double bx, double by, double bz, double radius) {
        if (this.capsuleCount * 7 == this.capsules.length) {
            this.capsules = java.util.Arrays.copyOf(this.capsules, this.capsules.length * 2);
        }
        int o = this.capsuleCount++ * 7;
        this.capsules[o] = ax;
        this.capsules[o + 1] = ay;
        this.capsules[o + 2] = az;
        this.capsules[o + 3] = bx;
        this.capsules[o + 4] = by;
        this.capsules[o + 5] = bz;
        this.capsules[o + 6] = radius;
    }

    public void step(double dt, int substeps, Blocks world) {
        System.arraycopy(this.now, 0, this.was, 0, this.now.length);
        this.gather(world, dt);
        double h = dt / substeps;
        double keep = Math.max(0.0, 1.0 - this.drag * h);
        for (int s = 0; s < substeps; s++) {
            for (int i = 0; i < this.count; i++) {
                int o = i * 3;
                System.arraycopy(this.x, o, this.px, o, 3);
                if (this.pinned[i]) {
                    this.pinAt[o] += this.pinPace[o] * h;
                    this.pinAt[o + 1] += this.pinPace[o + 1] * h;
                    this.pinAt[o + 2] += this.pinPace[o + 2] * h;
                    System.arraycopy(this.pinAt, o, this.x, o, 3);
                    continue;
                }
                this.v[o] = this.windX + (this.v[o] - this.windX) * keep;
                this.v[o + 1] = this.windY + (this.v[o + 1] + this.gravity * h - this.windY) * keep;
                this.v[o + 2] = this.windZ + (this.v[o + 2] - this.windZ) * keep;
                this.x[o] += this.v[o] * h;
                this.x[o + 1] += this.v[o + 1] * h;
                this.x[o + 2] += this.v[o + 2] * h;
            }
            for (int r = 0; r < this.rods; r++) {
                this.hold(r, h);
            }
            for (int i = 0; i < this.count; i++) {
                if (!this.pinned[i]) {
                    this.outOfCapsules(i);
                    this.outOfBlocks(i);
                }
            }
            for (int i = 0; i < this.count; i++) {
                int o = i * 3;
                this.v[o] = (this.x[o] - this.px[o]) / h;
                this.v[o + 1] = (this.x[o + 1] - this.px[o + 1]) / h;
                this.v[o + 2] = (this.x[o + 2] - this.px[o + 2]) / h;
            }
        }
        for (int i = 0; i < this.count; i++) {
            if (this.pinned[i]) {
                this.pinPace[i * 3] = 0.0;
                this.pinPace[i * 3 + 1] = 0.0;
                this.pinPace[i * 3 + 2] = 0.0;
            }
        }
        System.arraycopy(this.x, 0, this.now, 0, this.now.length);
    }

    private void hold(int r, double h) {
        int a = this.rodA[r] * 3;
        int b = this.rodB[r] * 3;
        double wa = this.pinned[this.rodA[r]] ? 0.0 : this.invMass[this.rodA[r]];
        double wb = this.pinned[this.rodB[r]] ? 0.0 : this.invMass[this.rodB[r]];
        double dx = this.x[a] - this.x[b];
        double dy = this.x[a + 1] - this.x[b + 1];
        double dz = this.x[a + 2] - this.x[b + 2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double weight = wa + wb + this.rodSoft[r] / (h * h);
        if (length < 1.0E-9 || weight <= 0.0) {
            return;
        }
        double lambda = -(length - this.rodLength[r]) / weight / length;
        this.x[a] += dx * lambda * wa;
        this.x[a + 1] += dy * lambda * wa;
        this.x[a + 2] += dz * lambda * wa;
        this.x[b] -= dx * lambda * wb;
        this.x[b + 1] -= dy * lambda * wb;
        this.x[b + 2] -= dz * lambda * wb;
    }

    private void outOfCapsules(int i) {
        int o = i * 3;
        for (int k = 0; k < this.capsuleCount; k++) {
            int c = k * 7;
            double ax = this.capsules[c];
            double ay = this.capsules[c + 1];
            double az = this.capsules[c + 2];
            double bx = this.capsules[c + 3] - ax;
            double by = this.capsules[c + 4] - ay;
            double bz = this.capsules[c + 5] - az;
            double radius = this.capsules[c + 6] + SKIN;
            double px = this.x[o] - ax;
            double py = this.x[o + 1] - ay;
            double pz = this.x[o + 2] - az;
            double span = bx * bx + by * by + bz * bz;
            double u = span < 1.0E-12 ? 0.0 : Math.max(0.0, Math.min(1.0, (px * bx + py * by + pz * bz) / span));
            double nx = px - bx * u;
            double ny = py - by * u;
            double nz = pz - bz * u;
            double distance = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (distance >= radius || distance < 1.0E-9) {
                continue;
            }
            double push = (radius - distance) / distance;
            this.x[o] += nx * push;
            this.x[o + 1] += ny * push;
            this.x[o + 2] += nz * push;
            this.rub(o, nx / distance, ny / distance, nz / distance);
        }
    }

    private void outOfBlocks(int i) {
        int o = i * 3;
        for (int k = 0; k < this.blockCount; k++) {
            int e = k * 6;
            double cx = this.x[o];
            double cy = this.x[o + 1];
            double cz = this.x[o + 2];
            if (cx < this.blocks[e] || cx >= this.blocks[e + 3] || cy < this.blocks[e + 1] || cy >= this.blocks[e + 4]
                    || cz < this.blocks[e + 2] || cz >= this.blocks[e + 5]) {
                continue;
            }
            // Out through the face it came in by: the one it was outside of a substep ago, else the nearest.
            int axis = -1;
            double best = Double.POSITIVE_INFINITY;
            double to = 0.0;
            for (int a = 0; a < 3; a++) {
                double low = this.blocks[e + a];
                double high = this.blocks[e + 3 + a];
                double was = this.px[o + a];
                double p = this.x[o + a];
                boolean came = was < low || was >= high;
                double down = p - low;
                double up = high - p;
                double depth = Math.min(down, up) - (came ? 1.0e3 : 0.0);
                if (depth < best) {
                    best = depth;
                    axis = a;
                    to = down < up ? low - 1.0E-4 : high + 1.0E-4;
                }
            }
            this.x[o + axis] = to;
            this.rub(o, axis == 0 ? 1.0 : 0.0, axis == 1 ? 1.0 : 0.0, axis == 2 ? 1.0 : 0.0);
        }
    }

    // A point that touches something keeps only part of how it moved along it (across the unit n) this substep.
    private void rub(int o, double nx, double ny, double nz) {
        double dx = this.x[o] - this.px[o];
        double dy = this.x[o + 1] - this.px[o + 1];
        double dz = this.x[o + 2] - this.px[o + 2];
        double along = dx * nx + dy * ny + dz * nz;
        double lost = 1.0 - this.slide;
        this.x[o] -= (dx - nx * along) * lost;
        this.x[o + 1] -= (dy - ny * along) * lost;
        this.x[o + 2] -= (dz - nz * along) * lost;
    }

    private void gather(Blocks world, double dt) {
        if (world == Blocks.NONE) {
            this.blockCount = 0;
            return;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.count; i++) {
            int o = i * 3;
            double reach = Math.sqrt(this.v[o] * this.v[o] + this.v[o + 1] * this.v[o + 1] + this.v[o + 2] * this.v[o
                    + 2]) * dt + 0.5 * Math.abs(this.gravity) * dt * dt + 0.1;
            minX = Math.min(minX, this.x[o] - reach);
            minY = Math.min(minY, this.x[o + 1] - reach);
            minZ = Math.min(minZ, this.x[o + 2] - reach);
            maxX = Math.max(maxX, this.x[o] + reach);
            maxY = Math.max(maxY, this.x[o + 1] + reach);
            maxZ = Math.max(maxZ, this.x[o + 2] + reach);
        }
        this.blockCount = Math.min(MOST_BLOCKS, world.collect(minX, minY, minZ, maxX, maxY, maxZ, this.blocks));
    }

    // Where point i is, `partialTick` of the way from the step before the last to the last.
    public void point(int i, double partialTick, double[] out) {
        int o = i * 3;
        out[0] = this.was[o] + (this.now[o] - this.was[o]) * partialTick;
        out[1] = this.was[o + 1] + (this.now[o + 1] - this.was[o + 1]) * partialTick;
        out[2] = this.was[o + 2] + (this.now[o + 2] - this.was[o + 2]) * partialTick;
    }

    // How far apart points a and b are now.
    public double gap(int a, int b) {
        double dx = this.x[a * 3] - this.x[b * 3];
        double dy = this.x[a * 3 + 1] - this.x[b * 3 + 1];
        double dz = this.x[a * 3 + 2] - this.x[b * 3 + 2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Moves every point the same way at once (the thing it hangs from was carried off).
    public void shift(double dx, double dy, double dz) {
        for (int i = 0; i < this.count; i++) {
            int o = i * 3;
            this.x[o] += dx;
            this.x[o + 1] += dy;
            this.x[o + 2] += dz;
            this.px[o] += dx;
            this.px[o + 1] += dy;
            this.px[o + 2] += dz;
            this.was[o] += dx;
            this.was[o + 1] += dy;
            this.was[o + 2] += dz;
            this.now[o] += dx;
            this.now[o + 1] += dy;
            this.now[o + 2] += dz;
            this.pinAt[o] += dx;
            this.pinAt[o + 1] += dy;
            this.pinAt[o + 2] += dz;
        }
    }
}
