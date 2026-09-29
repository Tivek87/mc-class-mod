package nl.tivek.multiversepowers.engine.physics;

import nl.tivek.multiversepowers.engine.physics.joint.SelfContact;

// A rigid world among the boxes of other worlds about it (other limp bodies lying there): each body, as a capsule
// along its longest side, is pushed out of them, and they are not moved. Every world pushes itself out of the others,
// so bodies falling on each other come to lie on each other instead of through.
abstract class RigidCrowd extends RigidBlocks {
    // A box: its middle, its turn (a quaternion), its half sizes and how fast it moves. Carried along at that speed
    // through the step, as the other world steps too: seen jumping ahead a whole step at once, it would be deep in a
    // body all at once and throw it off.
    public static final int BOX = 13;
    private static final int MOST_OTHERS = 256;
    private static final int NEAR_OTHERS = 24;
    private static final int SAMPLES = 5;
    // Room past how far a body can move by itself in a step, as for blocks.
    private static final double LEEWAY = 0.35;
    // Only a body this far or less into another is pushed out, as one landing on it or lying on it is: bodies deep in
    // each other began that way (creatures dying in a crowd), and pushed out they would be flung off. They pass through
    // each other until they meet again from outside.
    private static final double DEEP = 0.06;
    // A point is stopped as far as it came into the other this substep, as by a blow; the rest of the way out the body
    // is only moved, this much in all a substep: pushed out further at once, it would gather speed and fly off.
    private static final double EASE_OUT = 0.0015;

    private final double[] others = new double[MOST_OTHERS * BOX];
    private int otherCount;
    // How far into the step the substep being solved has got, and the one before (seconds).
    double lead;
    double leadWas;
    private final int[] nearOthers = new int[MOST * NEAR_OTHERS];
    private final int[] nearOtherCount = new int[MOST];
    private final double[] sample = new double[3];
    private final double[] inOther = new double[3];
    private final double[] out = new double[3];
    // How far each sample point of the body came this substep.
    private final double[] came = new double[SAMPLES * 3];

    // The boxes of the other bodies about, for the coming steps.
    public void others(double[] boxes, int count) {
        this.otherCount = Math.min(count, MOST_OTHERS);
        System.arraycopy(boxes, 0, this.others, 0, this.otherCount * BOX);
    }

    // For each body, the other boxes it can touch during the coming step.
    void sortOthers(double dt) {
        for (int b = 0; b < this.count; b++) {
            int o = b * 3;
            double size = Math.sqrt(this.half[o] * this.half[o] + this.half[o + 1] * this.half[o + 1]
                    + this.half[o + 2] * this.half[o + 2]);
            double speed = Math.sqrt(this.v[o] * this.v[o] + this.v[o + 1] * this.v[o + 1] + this.v[o + 2] * this.v[o
                    + 2]);
            double reach = size + speed * dt + 0.5 * Math.abs(this.gravity) * dt * dt + LEEWAY;
            int near = 0;
            for (int k = 0; k < this.otherCount && near < NEAR_OTHERS; k++) {
                int e = k * BOX;
                double hx = this.others[e + 7];
                double hy = this.others[e + 8];
                double hz = this.others[e + 9];
                double vx = this.others[e + 10];
                double vy = this.others[e + 11];
                double vz = this.others[e + 12];
                double far = reach + Math.sqrt(hx * hx + hy * hy + hz * hz) + Math.sqrt(vx * vx + vy * vy + vz * vz) * dt;
                double dx = this.others[e] - this.x[o];
                double dy = this.others[e + 1] - this.x[o + 1];
                double dz = this.others[e + 2] - this.x[o + 2];
                if (dx * dx + dy * dy + dz * dz < far * far) {
                    this.nearOthers[b * NEAR_OTHERS + near++] = k;
                }
            }
            this.nearOtherCount[b] = near;
        }
    }

    // Body b, as a capsule along its longest side, pushed out of the other boxes near it; each touch kept as one with a
    // block is, for friction and so it does not bounce off.
    void touchOthers(int b) {
        if (this.invMass[b] == 0.0 || this.nearOtherCount[b] == 0) {
            return;
        }
        int o = b * 3;
        int longest = 0;
        for (int a = 1; a < 3; a++) {
            if (this.half[o + a] > this.half[o + longest]) {
                longest = a;
            }
        }
        double radius = Double.POSITIVE_INFINITY;
        for (int a = 0; a < 3; a++) {
            if (a != longest) {
                radius = Math.min(radius, this.half[o + a]);
            }
        }
        double reach = Math.max(0.0, this.half[o + longest] - radius);
        for (int s = 0; s < SAMPLES; s++) {
            double along = -reach + 2.0 * reach * s / (SAMPLES - 1);
            double lx = longest == 0 ? along : 0.0;
            double ly = longest == 1 ? along : 0.0;
            double lz = longest == 2 ? along : 0.0;
            this.point(b, lx, ly, lz, this.sample);
            Quat.rotate(this.pq, b * 4, lx, ly, lz, this.inOther, 0);
            for (int a = 0; a < 3; a++) {
                this.came[s * 3 + a] = this.sample[a] - this.px[o + a] - this.inOther[a];
            }
        }
        double ease = EASE_OUT;
        double h = this.lead - this.leadWas;
        this.touches = 0;
        for (int s = 0; s < SAMPLES; s++) {
            double along = -reach + 2.0 * reach * s / (SAMPLES - 1);
            double lx = longest == 0 ? along : 0.0;
            double ly = longest == 1 ? along : 0.0;
            double lz = longest == 2 ? along : 0.0;
            for (int n = 0; n < this.nearOtherCount[b]; n++) {
                int e = this.nearOthers[b * NEAR_OTHERS + n] * BOX;
                this.point(b, lx, ly, lz, this.sample);
                Quat.unrotate(this.others, e + 3, this.sample[0] - this.others[e] - this.others[e + 10] * this.lead,
                        this.sample[1] - this.others[e + 1] - this.others[e + 11] * this.lead,
                        this.sample[2] - this.others[e + 2] - this.others[e + 12] * this.lead, this.inOther, 0);
                double depth = SelfContact.depth(this.inOther[0], this.inOther[1], this.inOther[2],
                        this.others[e + 7], this.others[e + 8], this.others[e + 9], this.out) + radius;
                if (depth <= 0.0 || depth > DEEP) {
                    continue;
                }
                Quat.rotate(this.others, e + 3, this.out[0], this.out[1], this.out[2], this.out, 0);
                // How far it came into the other this substep, the other coming on as well.
                double into = 0.0;
                for (int a = 0; a < 3; a++) {
                    into -= (this.came[s * 3 + a] - this.others[e + 10 + a] * h) * this.out[a];
                }
                double blow = Math.min(depth, Math.max(0.0, into));
                double slide = Math.min(depth - blow, ease);
                ease -= slide;
                if (blow + slide <= 0.0) {
                    continue;
                }
                // Counted as come no further, should another box it lies in push it out as well.
                for (int a = 0; a < 3; a++) {
                    this.came[s * 3 + a] += this.out[a] * blow;
                }
                double sx = this.sample[0] - this.out[0] * radius;
                double sy = this.sample[1] - this.out[1] * radius;
                double sz = this.sample[2] - this.out[2] * radius;
                this.pushOut(b, sx, sy, sz, this.out[0], this.out[1], this.out[2], blow);
                // Moved where it was a substep ago too: no speed from it.
                for (int a = 0; a < 3; a++) {
                    this.x[o + a] += this.out[a] * slide;
                    this.px[o + a] += this.out[a] * slide;
                }
                depth = blow + slide;
                // Where it touched, in its own axes, after the push.
                this.point(b, lx, ly, lz, this.sample);
                Quat.unrotate(this.q, b * 4, this.sample[0] - this.out[0] * radius - this.x[o],
                        this.sample[1] - this.out[1] * radius - this.x[o + 1],
                        this.sample[2] - this.out[2] * radius - this.x[o + 2], this.inOther, 0);
                this.touch(this.inOther[0], this.inOther[1], this.inOther[2], this.out[0], this.out[1], this.out[2],
                        depth);
            }
        }
        this.keepContacts(b);
    }
}
