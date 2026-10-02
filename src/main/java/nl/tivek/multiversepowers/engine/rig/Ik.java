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

    // How deep a two-bone limb with its middle joint at (x, y, z) goes into something: 0 or less when clear.
    @FunctionalInterface
    public interface Clearance {
        double depth(double x, double y, double z);
    }

    // As twoBone, the middle joint swung round the line from root to end, from bent towards the pole (0) as far as
    // `low` (0 or less) and `high` (0 or more) radians: tried in `steps` steps the wider way, the nearer angles first
    // and at each the wider way first, and kept just where the limb comes clear (between that step and the one nearer),
    // so it swings smoothly as it moves, else where it goes least deep. Returns that angle; the joint is in out.
    public static double swivel(double[] root, double[] end, double[] pole, double upper, double lower, double low,
            double high, int steps, Clearance clearance, double tolerance, double[] out) {
        double[] across = across(root, end, pole);
        if (across == null) {
            twoBone(root, end, pole, upper, lower, out);
            return 0.0;
        }
        double[] turned = new double[3];
        double step = Math.max(Math.abs(low), Math.abs(high)) / Math.max(1, steps);
        double wide = Math.abs(high) >= Math.abs(low) ? 1.0 : -1.0;
        double best = 0.0;
        double least = Double.POSITIVE_INFINITY;
        for (int k = 0; k <= 2 * steps; k++) {
            double angle = (k + 1) / 2 * step * (k % 2 == 1 ? wide : -wide);
            if (angle < low - 1.0E-9 || angle > high + 1.0E-9) {
                continue;
            }
            double depth = tried(root, end, across, angle, upper, lower, clearance, turned, out);
            if (depth <= tolerance) {
                if (k == 0) {
                    return 0.0;
                }
                // Clear here but not a step nearer: found to a 64th of a step where it comes clear.
                double clear = angle;
                double blocked = angle - Math.signum(angle) * step;
                for (int i = 0; i < 6; i++) {
                    double middle = 0.5 * (clear + blocked);
                    if (tried(root, end, across, middle, upper, lower, clearance, turned, out) <= tolerance) {
                        clear = middle;
                    } else {
                        blocked = middle;
                    }
                }
                bent(root, end, across, clear, upper, lower, turned, out);
                return clear;
            }
            if (depth < least - 1.0E-9) {
                least = depth;
                best = angle;
            }
        }
        bent(root, end, across, best, upper, lower, turned, out);
        return best;
    }

    // The middle joint swung round the line from root to end by `angle` from bent towards the pole (see swivel).
    public static void twoBone(double[] root, double[] end, double[] pole, double upper, double lower, double angle,
            double[] out) {
        double[] across = angle == 0.0 ? null : across(root, end, pole);
        if (across == null) {
            twoBone(root, end, pole, upper, lower, out);
            return;
        }
        bent(root, end, across, angle, upper, lower, new double[3], out);
    }

    // The pole's part across the line from root to end, and that turned a quarter round it (the line's way crossed
    // with it), in one array; null where root and end meet.
    private static double[] across(double[] root, double[] end, double[] pole) {
        double ax = end[0] - root[0];
        double ay = end[1] - root[1];
        double az = end[2] - root[2];
        double length = Math.sqrt(ax * ax + ay * ay + az * az);
        if (length < 1.0E-9) {
            return null;
        }
        ax /= length;
        ay /= length;
        az /= length;
        double dot = pole[0] * ax + pole[1] * ay + pole[2] * az;
        double px = pole[0] - ax * dot;
        double py = pole[1] - ay * dot;
        double pz = pole[2] - az * dot;
        return new double[] { px, py, pz, ay * pz - az * py, az * px - ax * pz, ax * py - ay * px };
    }

    private static void bent(double[] root, double[] end, double[] across, double angle, double upper, double lower,
            double[] turned, double[] out) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        turned[0] = across[0] * c + across[3] * s;
        turned[1] = across[1] * c + across[4] * s;
        turned[2] = across[2] * c + across[5] * s;
        twoBone(root, end, turned, upper, lower, out);
    }

    private static double tried(double[] root, double[] end, double[] across, double angle, double upper,
            double lower, Clearance clearance, double[] turned, double[] out) {
        bent(root, end, across, angle, upper, lower, turned, out);
        return clearance.depth(out[0], out[1], out[2]);
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
