package nl.tivek.multiversepowers.character.greenlantern.client.body;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipLash;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.engine.math.Keyframes;

abstract class WhipCurves {
    static final double OWN_WHIP = 0.82;
    static final double WHIP_SCALE = 1.0;

    // View space: x right, y up, -z ahead. The body channels are the flamethrower's (see FlameCurves); leftOn is how
    // far the left arm leaves its own swing for the left hand's spot. The last channels are the lash (see WhipLash).
    record Pose(Vec3 grip, Vec3 handle, Vec3 top, Vec3 left, float leftOn, float twist, float lean, float step,
            float rest, float orbit, float roll, float squat, float kneel, float wide, float hop, float[] lash) {
        static final int BODY = 23;
        static final int SIZE = BODY + WhipLash.SIZE;
        static final int LEFT_ON = 12;
        static final int TWIST = 13;
        static final int LEAN = 14;
        static final int STEP = 15;
        static final int REST = 16;
        static final int ORBIT = 17;
        static final int ROLL = 18;
        static final int SQUAT = 19;
        static final int KNEEL = 20;
        static final int WIDE = 21;
        static final int HOP = 22;

        float[] numbers() {
            float[] n = new float[SIZE];
            put(n, 0, this.grip);
            put(n, 3, this.handle);
            put(n, 6, this.top);
            put(n, 9, this.left);
            n[LEFT_ON] = this.leftOn;
            n[TWIST] = this.twist;
            n[LEAN] = this.lean;
            n[STEP] = this.step;
            n[REST] = this.rest;
            n[ORBIT] = this.orbit;
            n[ROLL] = this.roll;
            n[SQUAT] = this.squat;
            n[KNEEL] = this.kneel;
            n[WIDE] = this.wide;
            n[HOP] = this.hop;
            System.arraycopy(this.lash, 0, n, BODY, WhipLash.SIZE);
            return n;
        }

        static Pose of(float[] n) {
            Vec3 handle = unit(new Vec3(n[3], n[4], n[5]), new Vec3(0.0, 0.0, -1.0));
            Vec3 top = square(new Vec3(n[6], n[7], n[8]), handle);
            float[] lash = new float[WhipLash.SIZE];
            System.arraycopy(n, BODY, lash, 0, WhipLash.SIZE);
            return new Pose(new Vec3(n[0], n[1], n[2]), handle, top, new Vec3(n[9], n[10], n[11]),
                    Mth.clamp(n[LEFT_ON], 0.0F, 1.0F), n[TWIST], n[LEAN], n[STEP], n[REST], n[ORBIT], n[ROLL], n[SQUAT],
                    n[KNEEL], n[WIDE], n[HOP], lash);
        }

        Pose mix(Pose to, float t) {
            float[] a = this.numbers();
            float[] b = to.numbers();
            float[] lash = lashOf(b);
            WhipLash.nearest(lash, lashOf(a));
            System.arraycopy(lash, 0, b, BODY, WhipLash.SIZE);
            for (int k = 0; k < a.length; k++) {
                a[k] = Mth.lerp(t, a[k], b[k]);
            }
            return of(a);
        }

        Pose gripping(Vec3 grip, Vec3 left) {
            return new Pose(grip, this.handle, this.top, left, this.leftOn, this.twist, this.lean, this.step, this.rest,
                    this.orbit, this.roll, this.squat, this.kneel, this.wide, this.hop, this.lash);
        }

        Pose lashed(float[] lash) {
            return new Pose(this.grip, this.handle, this.top, this.left, this.leftOn, this.twist, this.lean,
                    this.step, this.rest, this.orbit, this.roll, this.squat, this.kneel, this.wide, this.hop, lash);
        }

        // The same pose with whole turns taken off the orbit and the lash's angles, so the next move does not unwind
        // them.
        Pose unwound() {
            float[] n = this.numbers();
            n[ORBIT] -= Math.round(n[ORBIT] / Mth.TWO_PI) * Mth.TWO_PI;
            float[] lash = lashOf(n);
            WhipLash.nearest(lash, WhipMove.REST);
            System.arraycopy(lash, 0, n, BODY, WhipLash.SIZE);
            return of(n);
        }

        // In first person the view stays put while the body turns: the hands and the handle go round the eyes.
        Pose orbited() {
            if (Math.abs(this.orbit) < 1.0E-4F) {
                return this;
            }
            double angle = -this.orbit;
            return new Pose(yawed(this.grip, angle), yawed(this.handle, angle), yawed(this.top, angle),
                    yawed(this.left, angle), this.leftOn, this.twist, this.lean, this.step, this.rest, 0.0F, this.roll,
                    this.squat, this.kneel, this.wide, this.hop, this.lash);
        }

        private static void put(float[] n, int at, Vec3 v) {
            n[at] = (float) v.x;
            n[at + 1] = (float) v.y;
            n[at + 2] = (float) v.z;
        }
    }

    static float[] lashOf(float[] numbers) {
        float[] lash = new float[WhipLash.SIZE];
        System.arraycopy(numbers, Pose.BODY, lash, 0, WhipLash.SIZE);
        return lash;
    }

    static Pose pose(double gx, double gy, double gz, double hx, double hy, double hz, double lx, double ly, double lz,
            float leftOn, float twist, float lean, float step) {
        Vec3 handle = unit(new Vec3(hx, hy, hz), new Vec3(0.0, 0.0, -1.0));
        Vec3 top = square(new Vec3(0.0, 1.0, 0.0), handle);
        float[] n = new float[Pose.SIZE];
        n[0] = (float) gx;
        n[1] = (float) gy;
        n[2] = (float) gz;
        n[3] = (float) handle.x;
        n[4] = (float) handle.y;
        n[5] = (float) handle.z;
        n[6] = (float) top.x;
        n[7] = (float) top.y;
        n[8] = (float) top.z;
        n[9] = (float) lx;
        n[10] = (float) ly;
        n[11] = (float) lz;
        n[Pose.LEFT_ON] = leftOn;
        n[Pose.TWIST] = twist * Mth.DEG_TO_RAD;
        n[Pose.LEAN] = lean;
        n[Pose.STEP] = step;
        System.arraycopy(WhipMove.REST, 0, n, Pose.BODY, WhipLash.SIZE);
        return Pose.of(n);
    }

    static Keyframes.Key key(float tick, boolean stop, Pose pose) {
        float[] body = new float[Pose.BODY];
        System.arraycopy(pose.numbers(), 0, body, 0, Pose.BODY);
        return new Keyframes.Key(tick, stop, body);
    }

    // The body through its keys, starting from where the last move left it (at the speed it had) and settling into
    // the rest; the lash is worked out on its own (see along).
    static Pose keyed(Keyframes.Key[] body, float t, Pose from, @Nullable float[] fromSpeed, Pose rest,
            float settle) {
        float end = Keyframes.end(body) + settle;
        if (t >= end) {
            return rest;
        }
        float[] start = from.numbers();
        float[] out = new float[Pose.SIZE];
        float[] ends = rest.numbers();
        float time = Math.max(0.0F, t);
        float[] bodyFrom = new float[Pose.BODY];
        float[] bodyEnd = new float[Pose.BODY];
        System.arraycopy(start, 0, bodyFrom, 0, Pose.BODY);
        System.arraycopy(ends, 0, bodyEnd, 0, Pose.BODY);
        Keyframes.Key[] line = new Keyframes.Key[body.length + 2];
        line[0] = new Keyframes.Key(0.0F, fromSpeed == null, bodyFrom, part(fromSpeed, 0, Pose.BODY));
        System.arraycopy(body, 0, line, 1, body.length);
        line[line.length - 1] = new Keyframes.Key(end, true, bodyEnd);
        System.arraycopy(Keyframes.at(line, time), 0, out, 0, Pose.BODY);
        System.arraycopy(ends, Pose.BODY, out, Pose.BODY, WhipLash.SIZE);
        return Pose.of(out);
    }

    // The lash through keys from where it was, its angles written as the keys write them.
    static float[] along(Keyframes.Key[] keys, float t, float[] from, @Nullable float[] speed, float[] rest,
            float end) {
        float[] first = from.clone();
        float[] speeds = speed == null ? null : speed.clone();
        if (WhipLash.nearest(first, keys[0].values()) && speeds != null) {
            speeds[WhipLash.PITCH] = -speeds[WhipLash.PITCH];
        }
        float[] last = rest.clone();
        WhipLash.nearest(last, keys[keys.length - 1].values());
        Keyframes.Key[] line = new Keyframes.Key[keys.length + 2];
        line[0] = new Keyframes.Key(0.0F, speeds == null, first, speeds);
        System.arraycopy(keys, 0, line, 1, keys.length);
        line[line.length - 1] = new Keyframes.Key(end, true, last);
        return Keyframes.at(line, t);
    }

    // From the last pose into one that is worked out as it goes (a spin, a whirl), keeping the speed it had.
    static Pose carried(Pose from, @Nullable float[] speed, Pose to, float t, float over) {
        float u = Math.max(0.0F, t) / over;
        if (u >= 1.0F) {
            return to;
        }
        float[] n = from.numbers();
        float[] target = to.numbers();
        float[] lash = lashOf(n);
        boolean flip = WhipLash.nearest(lash, lashOf(target));
        System.arraycopy(lash, 0, n, Pose.BODY, WhipLash.SIZE);
        if (speed != null) {
            float on = u * over * (1.0F - u) * (1.0F - u);
            for (int c = 0; c < n.length; c++) {
                float v = flip && c == Pose.BODY + WhipLash.PITCH ? -speed[c] : speed[c];
                n[c] += v * on;
            }
        }
        float w = u * u * (3.0F - 2.0F * u);
        for (int c = 0; c < n.length; c++) {
            n[c] = Mth.lerp(w, n[c], target[c]);
        }
        return Pose.of(n);
    }

    @Nullable
    private static float[] part(@Nullable float[] all, int from, int to) {
        if (all == null) {
            return null;
        }
        float[] out = new float[to - from];
        System.arraycopy(all, from, out, 0, to - from);
        return out;
    }

    static Vec3 unit(Vec3 way, Vec3 otherwise) {
        double length = way.length();
        return length < 1.0E-4 ? otherwise : way.scale(1.0 / length);
    }

    static Vec3 square(Vec3 way, Vec3 axis) {
        Vec3 flat = way.subtract(axis.scale(way.dot(axis)));
        if (flat.lengthSqr() < 1.0E-6) {
            Vec3 other = Math.abs(axis.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            flat = other.subtract(axis.scale(other.dot(axis)));
        }
        return flat.normalize();
    }

    // Turns a view-space way to the right by the angle (radians), about the view's up.
    static Vec3 yawed(Vec3 way, double angle) {
        if (angle == 0.0) {
            return way;
        }
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(way.x * cos - way.z * sin, way.y, way.x * sin + way.z * cos);
    }

    // A view-space way that points where a lash aim of the given yaw and pitch (radians) would, in first person.
    static Vec3 aimed(double yaw, double pitch) {
        double cos = Math.cos(pitch);
        return new Vec3(Math.sin(yaw) * cos, Math.sin(pitch), -Math.cos(yaw) * cos);
    }
}
