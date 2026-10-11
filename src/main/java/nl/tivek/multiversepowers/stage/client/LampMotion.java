package nl.tivek.multiversepowers.stage.client;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.stage.LampKind;
import nl.tivek.multiversepowers.stage.LampSettings;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Where a stage light points and what colour it shines at a moment, worked out the same in every game from the world's
// clock, so lamps with the same settings move and flash as one.
final class LampMotion {
    // How far from straight out of its mount a head can turn.
    static final double MOST_TURN = Math.toRadians(135.0);

    private LampMotion() {
    }

    // The mount's own axes: x, up (out of the face it stands on) and z, as the model is turned for that face.
    static Vec3[] axes(Direction facing) {
        Quaternionf turn = facing.getRotation();
        return new Vec3[] { turned(turn, 1, 0, 0), turned(turn, 0, 1, 0), turned(turn, 0, 0, 1) };
    }

    private static Vec3 turned(Quaternionf turn, float x, float y, float z) {
        Vector3f v = turn.transform(new Vector3f(x, y, z));
        return new Vec3(v.x, v.y, v.z);
    }

    // The way it points when nothing moves it: turned `pan` degrees about its mount, leaning `tilt` away from it.
    static Vec3 rest(Vec3[] axes, LampSettings settings) {
        return toward(axes, Math.toRadians(settings.pan), Math.toRadians(settings.tilt));
    }

    static Vec3 toward(Vec3[] axes, double pan, double tilt) {
        Vec3 out = axes[2].scale(Math.cos(pan)).add(axes[0].scale(Math.sin(pan)));
        return axes[1].scale(Math.cos(tilt)).add(out.scale(Math.sin(tilt)));
    }

    // The pattern's swing at `seconds`: side to side and back and forth, in radians, round where it rests.
    static double[] swing(LampSettings settings, double seconds) {
        double w = (seconds * settings.speed + settings.phase) * Math.PI * 2.0;
        double h = Math.toRadians(settings.panRange);
        double v = Math.toRadians(settings.tiltRange);
        return switch (settings.pattern) {
            case STILL, SPIN -> new double[] { 0.0, 0.0 };
            case SWEEP -> new double[] { h * Math.sin(w), 0.0 };
            case CIRCLE -> new double[] { h * Math.cos(w), v * Math.sin(w) };
            case FIGURE_EIGHT -> new double[] { h * Math.sin(w), v * Math.sin(2.0 * w) };
            case WAVE -> new double[] { h * Math.sin(w), v * Math.sin(3.0 * w) };
            case BOUNCE -> new double[] { 0.0, v * Math.sin(w) };
            case RANDOM -> new double[] { h * (0.6 * Math.sin(w * 1.3) + 0.4 * Math.sin(w * 2.9 + 1.0)),
                    v * (0.6 * Math.sin(w * 1.7 + 2.0) + 0.4 * Math.sin(w * 3.1 + 0.5)) };
        };
    }

    // How far the head has spun about its own beam: a mirror ball's turn, a laser fan's.
    static double roll(LampSettings settings, double seconds) {
        return settings.pattern == LampKind.Pattern.SPIN
                ? (seconds * settings.speed + settings.phase) * Math.PI * 2.0
                : 0.0;
    }

    // `rest` swung `side` and `forth` radians.
    static Vec3 swung(Vec3 rest, Vec3[] axes, double side, double forth) {
        Vec3[] across = around(rest, axes);
        return rest.scale(Math.cos(side) * Math.cos(forth)).add(across[0].scale(Math.sin(side) * Math.cos(forth)))
                .add(across[1].scale(Math.sin(forth))).normalize();
    }

    // Two ways square to `beam`, kept steady as it turns: the first lies along the mount, the second across it.
    static Vec3[] around(Vec3 beam, Vec3[] axes) {
        Vec3 side = axes[1].cross(beam);
        if (side.lengthSqr() < 1.0E-4) {
            side = axes[0];
        }
        side = side.normalize();
        return new Vec3[] { side, beam.cross(side).normalize() };
    }

    // `dir` kept within MOST_TURN of the mount's up.
    static Vec3 kept(Vec3 dir, Vec3[] axes) {
        double angle = Math.acos(Mth.clamp(dir.dot(axes[1]), -1.0, 1.0));
        if (angle <= MOST_TURN) {
            return dir;
        }
        Vec3 out = dir.subtract(axes[1].scale(dir.dot(axes[1])));
        if (out.lengthSqr() < 1.0E-6) {
            out = axes[2];
        }
        return axes[1].scale(Math.cos(MOST_TURN)).add(out.normalize().scale(Math.sin(MOST_TURN)));
    }

    // Pan and tilt of the head for `dir`, in the mount's axes; `pan` stays as it was where the head points straight
    // out, as any way is right there.
    static double[] panTilt(Vec3 dir, Vec3[] axes, double pan) {
        double x = dir.dot(axes[0]);
        double up = dir.dot(axes[1]);
        double z = dir.dot(axes[2]);
        double tilt = Math.atan2(Math.sqrt(x * x + z * z), up);
        return new double[] { tilt < 1.0E-3 ? pan : Math.atan2(x, z), tilt };
    }

    // How bright its light is at `seconds`, the strobe's flash included: 0 off, 1 full.
    static double flash(LampSettings settings, double seconds) {
        if (settings.strobe <= 0.0) {
            return 1.0;
        }
        double at = seconds * settings.strobe + settings.phase;
        return at - Math.floor(at) < settings.duty ? 1.0 : 0.0;
    }

    // The colour of beam `beam` at `seconds`.
    static int color(LampSettings settings, double seconds, int beam) {
        int[] colors = settings.colors;
        int count = colors.length;
        double at = seconds * settings.colorSpeed + settings.phase * count;
        return switch (settings.colorMode) {
            case SINGLE -> colors[Math.floorMod(beam, count)];
            case JUMP -> colors[Math.floorMod((int) Math.floor(at) + beam, count)];
            case RANDOM -> colors[Math.floorMod(hash((long) Math.floor(at) * 31L + beam), count)];
            case FADE -> {
                int step = (int) Math.floor(at) + beam;
                double t = at - Math.floor(at);
                yield Colors.mix(colors[Math.floorMod(step, count)], colors[Math.floorMod(step + 1, count)],
                        (float) (t * t * (3.0 - 2.0 * t)));
            }
            case RAINBOW -> Mth.hsvToRgb((float) ((at * 0.25 + beam * 0.13) % 1.0 + 1.0) % 1.0F, 0.9F, 1.0F);
        };
    }

    private static int hash(long value) {
        long x = value * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        return (int) (x ^ x >>> 29) & 0x7FFFFFFF;
    }

    // The mirror ball's rays: `count` ways spread evenly over a ball, turned `roll` about its axis.
    static Vec3[] rays(int count, Vec3[] axes, double roll) {
        Vec3[] rays = new Vec3[count];
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < count; i++) {
            double y = 1.0 - 2.0 * (i + 0.5) / count;
            double r = Math.sqrt(1.0 - y * y);
            double a = golden * i + roll;
            rays[i] = axes[1].scale(y).add(axes[0].scale(r * Math.cos(a))).add(axes[2].scale(r * Math.sin(a)));
        }
        return rays;
    }
}
