package nl.tivek.multiversepowers.character.greenlantern.client.body;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FlamePainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Keyframes;

abstract class FlameCurves {
    static final double OWN_GUN = 0.7;
    static final double GUN_SCALE = 0.92;

    // View space, as the sword's: x right, y up, -z ahead. The body turns (twist), leans and steps only when seen.
    record Pose(Vec3 grip, Vec3 muzzle, Vec3 top, Vec3 left, float leftOn, float leftValve, float twist, float lean,
            float step, float sweep, float rest) {
        static final int SIZE = 19;

        float[] numbers() {
            return new float[] { (float) this.grip.x, (float) this.grip.y, (float) this.grip.z, (float) this.muzzle.x,
                    (float) this.muzzle.y, (float) this.muzzle.z, (float) this.top.x, (float) this.top.y,
                    (float) this.top.z, (float) this.left.x, (float) this.left.y, (float) this.left.z, this.leftOn,
                    this.leftValve, this.twist, this.lean, this.step, this.sweep, this.rest };
        }

        static Pose of(float[] n) {
            Vec3 muzzle = unit(new Vec3(n[3], n[4], n[5]), new Vec3(0.0, 0.0, -1.0));
            Vec3 top = square(new Vec3(n[6], n[7], n[8]), muzzle);
            return new Pose(new Vec3(n[0], n[1], n[2]), muzzle, top, new Vec3(n[9], n[10], n[11]),
                    Mth.clamp(n[12], 0.0F, 1.0F), Mth.clamp(n[13], 0.0F, 1.0F), n[14], n[15], n[16], n[17], n[18]);
        }

        Pose mix(Pose to, float t) {
            float[] a = this.numbers();
            float[] b = to.numbers();
            for (int k = 0; k < a.length; k++) {
                a[k] = Mth.lerp(t, a[k], b[k]);
            }
            return of(a);
        }

        Vec3 aim(double share) {
            return yawed(this.muzzle, this.sweep * share);
        }

        Vec3 up(double share) {
            return yawed(this.top, this.sweep * share);
        }

        ConstructPainter.Frame gun(double share, double scale) {
            return FlamePainter.held(this.grip, this.aim(share), this.up(share), scale);
        }

        Vec3 leftHand(double share, double scale) {
            if (this.leftOn <= 0.0F) {
                return this.left;
            }
            ConstructPainter.Frame gun = this.gun(share, scale);
            Vec3 fore = gun.at(FlamePainter.FORE.x, FlamePainter.FORE.y, FlamePainter.FORE.z);
            Vec3 valve = gun.at(FlamePainter.VALVE.x, FlamePainter.VALVE.y, FlamePainter.VALVE.z);
            return this.left.lerp(fore.lerp(valve, this.leftValve), this.leftOn);
        }
    }

    static Pose pose(double gx, double gy, double gz, double mx, double my, double mz, double tx, double ty,
            double tz, double lx, double ly, double lz, float leftOn, float leftValve, float twist, float lean,
            float step) {
        return Pose.of(new float[] { (float) gx, (float) gy, (float) gz, (float) mx, (float) my, (float) mz,
                (float) tx, (float) ty, (float) tz, (float) lx, (float) ly, (float) lz, leftOn, leftValve,
                twist * Mth.DEG_TO_RAD, lean, step, 0.0F, 0.0F });
    }

    static Keyframes.Key key(float tick, boolean stop, Pose pose) {
        return new Keyframes.Key(tick, stop, pose.numbers());
    }

    static Pose keyed(Keyframes.Key[] keys, float t, Pose from, @Nullable float[] fromSpeed, Pose rest,
            float settle) {
        float end = Keyframes.end(keys) + settle;
        if (t >= end) {
            return rest;
        }
        Keyframes.Key[] line = new Keyframes.Key[keys.length + 2];
        line[0] = new Keyframes.Key(0.0F, fromSpeed == null, from.numbers(), fromSpeed);
        System.arraycopy(keys, 0, line, 1, keys.length);
        line[line.length - 1] = new Keyframes.Key(end, true, rest.numbers());
        return Pose.of(Keyframes.at(line, Math.max(0.0F, t)));
    }

    static Pose carried(Pose from, @Nullable float[] speed, Pose to, float t, float over) {
        float u = Math.max(0.0F, t) / over;
        if (u >= 1.0F) {
            return to;
        }
        float[] n = from.numbers();
        if (speed != null) {
            float on = u * over * (1.0F - u) * (1.0F - u);
            for (int c = 0; c < n.length; c++) {
                n[c] += speed[c] * on;
            }
        }
        return Pose.of(n).mix(to, (float) (u * u * (3.0F - 2.0F * u)));
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
}
