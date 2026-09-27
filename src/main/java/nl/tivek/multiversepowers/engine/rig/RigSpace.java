package nl.tivek.multiversepowers.engine.rig;

// A rig's bones in its own space (the frame it is posed on as the identity), in plain numbers so a solver can pose
// it many times a frame without garbage. Per bone 12 values: its origin, then its axes x, y and z. In a frame's own
// axes a turn reads the same whichever hand the frame is, so these are the drawn bones' own coordinates.
public final class RigSpace {
    public static final int STRIDE = 12;

    private RigSpace() {
    }

    public static double[] create(Rig rig) {
        return new double[rig.size() * STRIDE];
    }

    public static void pose(Rig rig, RigPose pose, double[] space) {
        for (int b = 0; b < rig.size(); b++) {
            int o = b * STRIDE;
            int parent = rig.parent(b);
            double ox = rig.offsetX(b);
            double oy = rig.offsetY(b);
            double oz = rig.offsetZ(b);
            double xx;
            double xy;
            double xz;
            double yx;
            double yy;
            double yz;
            double zx;
            double zy;
            double zz;
            if (parent < 0) {
                space[o] = ox;
                space[o + 1] = oy;
                space[o + 2] = oz;
                xx = 1.0;
                xy = 0.0;
                xz = 0.0;
                yx = 0.0;
                yy = 1.0;
                yz = 0.0;
                zx = 0.0;
                zy = 0.0;
                zz = 1.0;
            } else {
                int p = parent * STRIDE;
                xx = space[p + 3];
                xy = space[p + 4];
                xz = space[p + 5];
                yx = space[p + 6];
                yy = space[p + 7];
                yz = space[p + 8];
                zx = space[p + 9];
                zy = space[p + 10];
                zz = space[p + 11];
                space[o] = space[p] + xx * ox + yx * oy + zx * oz;
                space[o + 1] = space[p + 1] + xy * ox + yy * oy + zy * oz;
                space[o + 2] = space[p + 2] + xz * ox + yz * oy + zz * oz;
            }
            Joint joint = rig.joint(b);
            for (int t = 0; t < joint.size(); t++) {
                double angle = pose.get(b, t);
                double c = Math.cos(angle);
                double s = Math.sin(angle);
                double nx;
                double ny;
                double nz;
                switch (joint.turn(t).axis()) {
                    case X -> {
                        nx = yx * c + zx * s;
                        ny = yy * c + zy * s;
                        nz = yz * c + zz * s;
                        zx = zx * c - yx * s;
                        zy = zy * c - yy * s;
                        zz = zz * c - yz * s;
                        yx = nx;
                        yy = ny;
                        yz = nz;
                    }
                    case Y -> {
                        nx = zx * c + xx * s;
                        ny = zy * c + xy * s;
                        nz = zz * c + xz * s;
                        xx = xx * c - zx * s;
                        xy = xy * c - zy * s;
                        xz = xz * c - zz * s;
                        zx = nx;
                        zy = ny;
                        zz = nz;
                    }
                    case Z -> {
                        nx = xx * c + yx * s;
                        ny = xy * c + yy * s;
                        nz = xz * c + yz * s;
                        yx = yx * c - xx * s;
                        yy = yy * c - xy * s;
                        yz = yz * c - xz * s;
                        xx = nx;
                        xy = ny;
                        xz = nz;
                    }
                }
            }
            space[o + 3] = xx;
            space[o + 4] = xy;
            space[o + 5] = xz;
            space[o + 6] = yx;
            space[o + 7] = yy;
            space[o + 8] = yz;
            space[o + 9] = zx;
            space[o + 10] = zy;
            space[o + 11] = zz;
        }
    }

    // A point given in a bone's own frame, in the rig's space.
    public static void point(double[] space, int bone, double x, double y, double z, double[] out) {
        int o = bone * STRIDE;
        out[0] = space[o] + space[o + 3] * x + space[o + 6] * y + space[o + 9] * z;
        out[1] = space[o + 1] + space[o + 4] * x + space[o + 7] * y + space[o + 10] * z;
        out[2] = space[o + 2] + space[o + 5] * x + space[o + 8] * y + space[o + 11] * z;
    }
}
