package nl.tivek.multiversepowers.engine.rig;

// Inverse kinematics on plain coordinates (x, y, z in a double[]), so the solvers make no garbage.
public final class Ik {
    private Ik() {
    }

    // The middle joint of a two-bone limb: upper long from the root, lower long to the end, bent towards the pole. The
    // reach is held just short of straight and just wider than folded shut, so the joint never snaps.
    public static void twoBone(double[] root, double[] end, double[] pole, double upper, double lower, double[] out) {
        double tx = end[0] - root[0];
        double ty = end[1] - root[1];
        double tz = end[2] - root[2];
        double length = Math.sqrt(tx * tx + ty * ty + tz * tz);
        double ax;
        double ay;
        double az;
        if (length < 1.0E-12) {
            ax = 0.0;
            ay = 1.0;
            az = 0.0;
        } else {
            ax = tx / length;
            ay = ty / length;
            az = tz / length;
        }
        double far = Math.max(Math.abs(upper - lower) + 1.0E-3, Math.min(length, (upper + lower) * 0.999));
        double along = (upper * upper - lower * lower + far * far) / (2.0 * far);
        double side = Math.sqrt(Math.max(0.0, upper * upper - along * along));
        double dot = pole[0] * ax + pole[1] * ay + pole[2] * az;
        double bx = pole[0] - ax * dot;
        double by = pole[1] - ay * dot;
        double bz = pole[2] - az * dot;
        double bend = Math.sqrt(bx * bx + by * by + bz * bz);
        if (bend < 1.0E-9) {
            // The pole lies along the limb: any side will do, taken the same way every time.
            if (Math.abs(ay) < 0.95) {
                bx = az;
                by = 0.0;
                bz = -ax;
            } else {
                bx = 0.0;
                by = -az;
                bz = ay;
            }
            bend = Math.sqrt(bx * bx + by * by + bz * bz);
        }
        out[0] = root[0] + ax * along + bx / bend * side;
        out[1] = root[1] + ay * along + by / bend * side;
        out[2] = root[2] + az * along + bz / bend * side;
    }

    public static double fabrik(double[] joints, double[] lengths, double tx, double ty, double tz, int iterations,
            double tolerance) {
        return fabrik(joints, lengths, null, tx, ty, tz, iterations, tolerance);
    }

    // FABRIK (Aristidou and Lasenby, 2011): moves the joints so the last reaches the target, keeping every bone's
    // length and the first joint in place; maxBend (radians, per joint from the second on, or null) is the most a bone
    // may turn from the one before it. Returns how far the end is left from the target.
    public static double fabrik(double[] joints, double[] lengths, double[] maxBend, double tx, double ty, double tz,
            int iterations, double tolerance) {
        int count = joints.length / 3;
        if (count < 2 || lengths.length != count - 1) {
            throw new IllegalArgumentException("FABRIK needs n joints and n - 1 lengths");
        }
        double rootX = joints[0];
        double rootY = joints[1];
        double rootZ = joints[2];
        double total = 0.0;
        for (double length : lengths) {
            total += length;
        }
        double dx = tx - rootX;
        double dy = ty - rootY;
        double dz = tz - rootZ;
        if (dx * dx + dy * dy + dz * dz >= total * total) {
            for (int i = 0; i < count - 1; i++) {
                place(joints, i, i + 1, tx, ty, tz, lengths[i]);
            }
            backward(joints, lengths, maxBend, rootX, rootY, rootZ);
            return gap(joints, count, tx, ty, tz);
        }
        for (int round = 0; round < iterations && gap(joints, count, tx, ty, tz) > tolerance; round++) {
            int last = (count - 1) * 3;
            joints[last] = tx;
            joints[last + 1] = ty;
            joints[last + 2] = tz;
            for (int i = count - 2; i >= 0; i--) {
                place(joints, i + 1, i, joints[i * 3], joints[i * 3 + 1], joints[i * 3 + 2], lengths[i]);
            }
            backward(joints, lengths, maxBend, rootX, rootY, rootZ);
        }
        return gap(joints, count, tx, ty, tz);
    }

    private static void backward(double[] joints, double[] lengths, double[] maxBend, double rootX, double rootY,
            double rootZ) {
        int count = joints.length / 3;
        joints[0] = rootX;
        joints[1] = rootY;
        joints[2] = rootZ;
        for (int i = 0; i < count - 1; i++) {
            place(joints, i, i + 1, joints[(i + 1) * 3], joints[(i + 1) * 3 + 1], joints[(i + 1) * 3 + 2],
                    lengths[i]);
            if (maxBend != null && i > 0) {
                bendLimit(joints, i, lengths[i], maxBend[i - 1]);
            }
        }
    }

    // Puts joint `to` at `length` from joint `from`, towards (x, y, z).
    private static void place(double[] joints, int from, int to, double x, double y, double z, double length) {
        double fx = joints[from * 3];
        double fy = joints[from * 3 + 1];
        double fz = joints[from * 3 + 2];
        double wx = x - fx;
        double wy = y - fy;
        double wz = z - fz;
        double way = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (way < 1.0E-12) {
            wx = joints[to * 3] - fx;
            wy = joints[to * 3 + 1] - fy;
            wz = joints[to * 3 + 2] - fz;
            way = Math.sqrt(wx * wx + wy * wy + wz * wz);
            if (way < 1.0E-12) {
                wx = 0.0;
                wy = 1.0;
                wz = 0.0;
                way = 1.0;
            }
        }
        joints[to * 3] = fx + wx / way * length;
        joints[to * 3 + 1] = fy + wy / way * length;
        joints[to * 3 + 2] = fz + wz / way * length;
    }

    // Turns bone i (joint i to i + 1) back towards bone i - 1 until it bends no more than the limit.
    private static void bendLimit(double[] joints, int i, double length, double limit) {
        double px = joints[i * 3] - joints[(i - 1) * 3];
        double py = joints[i * 3 + 1] - joints[(i - 1) * 3 + 1];
        double pz = joints[i * 3 + 2] - joints[(i - 1) * 3 + 2];
        double cx = joints[(i + 1) * 3] - joints[i * 3];
        double cy = joints[(i + 1) * 3 + 1] - joints[i * 3 + 1];
        double cz = joints[(i + 1) * 3 + 2] - joints[i * 3 + 2];
        double pl = Math.sqrt(px * px + py * py + pz * pz);
        double cl = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (pl < 1.0E-12 || cl < 1.0E-12) {
            return;
        }
        px /= pl;
        py /= pl;
        pz /= pl;
        cx /= cl;
        cy /= cl;
        cz /= cl;
        double cos = Math.max(-1.0, Math.min(1.0, px * cx + py * cy + pz * cz));
        double angle = Math.acos(cos);
        if (angle <= limit) {
            return;
        }
        // The part of the bone across the one before it, to turn within their shared plane.
        double ox = cx - px * cos;
        double oy = cy - py * cos;
        double oz = cz - pz * cos;
        double ol = Math.sqrt(ox * ox + oy * oy + oz * oz);
        if (ol < 1.0E-12) {
            double[] side = anySide(px, py, pz);
            ox = side[0];
            oy = side[1];
            oz = side[2];
        } else {
            ox /= ol;
            oy /= ol;
            oz /= ol;
        }
        double c = Math.cos(limit);
        double s = Math.sin(limit);
        joints[(i + 1) * 3] = joints[i * 3] + (px * c + ox * s) * length;
        joints[(i + 1) * 3 + 1] = joints[i * 3 + 1] + (py * c + oy * s) * length;
        joints[(i + 1) * 3 + 2] = joints[i * 3 + 2] + (pz * c + oz * s) * length;
    }

    private static double[] anySide(double x, double y, double z) {
        double sx;
        double sy;
        double sz;
        if (Math.abs(y) < 0.95) {
            sx = z;
            sy = 0.0;
            sz = -x;
        } else {
            sx = 0.0;
            sy = -z;
            sz = y;
        }
        double l = Math.sqrt(sx * sx + sy * sy + sz * sz);
        return new double[] { sx / l, sy / l, sz / l };
    }

    private static double gap(double[] joints, int count, double tx, double ty, double tz) {
        int last = (count - 1) * 3;
        double dx = joints[last] - tx;
        double dy = joints[last + 1] - ty;
        double dz = joints[last + 2] - tz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
