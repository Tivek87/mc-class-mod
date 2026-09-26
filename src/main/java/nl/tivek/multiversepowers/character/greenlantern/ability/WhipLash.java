package nl.tivek.multiversepowers.character.greenlantern.ability;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The lash of the energy whip. Every point along it goes the way the whip was flung a moment ago, the further along
// the longer ago, so a flick of the wrist runs down the lash and cracks at the tip. A lash that is not flung hangs.
// An aim is: yaw right and pitch up (radians) from where the owner looks, how taut it is, its length as a share of
// the whole whip, how much it holds level instead of following the look up and down, a ripple and a coil.
public final class WhipLash {
    public static final int YAW = 0;
    public static final int PITCH = 1;
    public static final int TAUT = 2;
    public static final int REACH = 3;
    public static final int LEVEL = 4;
    public static final int WAVE = 5;
    public static final int COIL = 6;
    public static final int SIZE = 7;
    // Ticks a flick takes to run from the handle to the tip of the whole whip.
    public static final double TRAVEL = 2.0;
    private static final double GRIP_BLOCKS = 0.42;
    private static final double DROOP_BLOCKS = 0.6;
    private static final double WAVE_BLOCKS = 1.5;
    private static final double WAVE_SPEED = 0.85;
    private static final double COIL_RADIUS = 0.5;
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);

    @FunctionalInterface
    public interface Aims {
        float[] at(double t);
    }

    // Where the whip is looked along: the look itself, the look held level, and the level right.
    public record Look(Vec3 ahead, Vec3 flat, Vec3 right) {
        public static Look of(float yRot, float xRot) {
            Vec3 flat = Vec3.directionFromRotation(0.0F, yRot);
            return new Look(Vec3.directionFromRotation(xRot, yRot), flat, new Vec3(-flat.z, 0.0, flat.x));
        }

        public Vec3 at(Vec3 from, double right, double up, double ahead) {
            Vec3 top = this.right.cross(this.ahead);
            return from.add(this.right.scale(right)).add(top.scale(up)).add(this.ahead.scale(ahead));
        }
    }

    // How a slack lash lies: the ground it rests on, the way it hangs, the way it lies down along the ground, and the
    // time for its ripples. The server leaves it out: its whip hangs in the air.
    public record Lie(double ground, Vec3 hang, Vec3 lie, double time) {
    }

    private WhipLash() {
    }

    public static Vec3 way(float[] aim, Look look) {
        double level = Mth.clamp(aim[LEVEL], 0.0, 1.0);
        Vec3 ahead = level <= 0.0 ? look.ahead() : level >= 1.0 ? look.flat()
                : mix(look.ahead(), look.flat(), level);
        Vec3 up = look.right().cross(ahead);
        double cos = Math.cos(aim[PITCH]);
        return look.right().scale(Math.sin(aim[YAW]) * cos).add(up.scale(Math.sin(aim[PITCH])))
                .add(ahead.scale(Math.cos(aim[YAW]) * cos));
    }

    public static Vec3[] shape(Vec3 root, @Nullable Vec3 handle, Look look, double length, Aims aims, double t,
            int segments, @Nullable Lie lie) {
        Vec3[] points = new Vec3[segments + 1];
        points[0] = root;
        double total = length * Math.max(0.0, aims.at(t)[REACH]);
        if (total <= 1.0E-4 || length <= 1.0E-4) {
            for (int i = 1; i <= segments; i++) {
                points[i] = root;
            }
            return points;
        }
        double step = total / segments;
        Vec3 heading = null;
        Vec3 hang = lie == null ? DOWN : lie.hang();
        for (int i = 0; i < segments; i++) {
            double s = (i + 0.5) * step;
            float[] aim = aims.at(t - TRAVEL * s / length);
            Vec3 way = way(aim, look);
            if (handle != null) {
                way = mix(handle, way, smooth(s / GRIP_BLOCKS));
            }
            double slack = 1.0 - Mth.clamp(aim[TAUT], 0.0F, 1.0F);
            if (slack > 0.0) {
                way = mix(way, hang, slack * Math.min(1.0, s / DROOP_BLOCKS));
            }
            if (lie != null && Math.abs(aim[WAVE]) > 1.0E-4) {
                Vec3 axis = way.cross(Vectors.UP);
                if (axis.lengthSqr() < 1.0E-6) {
                    axis = look.right();
                }
                double angle = aim[WAVE] * Math.sin(Math.PI * 2.0 * s / WAVE_BLOCKS - lie.time() * WAVE_SPEED)
                        * Math.min(1.0, s / GRIP_BLOCKS);
                way = Vectors.spin(way, axis.normalize(), angle);
            }
            Vec3 next = points[i].add(way.scale(step));
            if (lie != null && next.y < lie.ground()) {
                heading = lying(heading, way, lie, aim[COIL] * step / COIL_RADIUS, i);
                next = new Vec3(points[i].x + heading.x * step, lie.ground(), points[i].z + heading.z * step);
            } else {
                heading = null;
            }
            points[i + 1] = next;
        }
        return points;
    }

    // On the ground the lash goes on the way it came down, or the lie when it came straight down, and a coil curls
    // it round a little more with every step.
    private static Vec3 lying(@Nullable Vec3 heading, Vec3 way, Lie lie, double curl, int i) {
        Vec3 dir = heading;
        if (dir == null) {
            Vec3 flat = new Vec3(way.x, 0.0, way.z);
            double along = flat.length();
            dir = along < 1.0E-4 ? lie.lie() : mix(lie.lie(), flat.scale(1.0 / along), Math.min(1.0, along * 3.0));
        }
        double turn = curl + 0.05 * Math.sin(i * 0.9 + 1.7);
        return Vectors.spin(dir, Vectors.UP, turn);
    }

    public static Vec3 mix(Vec3 a, Vec3 b, double w) {
        if (w <= 0.0) {
            return a;
        }
        if (w >= 1.0) {
            return b;
        }
        Vec3 sum = a.scale(1.0 - w).add(b.scale(w));
        double length = sum.length();
        return length < 1.0E-6 ? (w < 0.5 ? a : b) : sum.scale(1.0 / length);
    }

    // The same way as the aim, written as close as it can be to another: a yaw and pitch can go a whole turn round,
    // and a lash thrown over the top reads as the one behind turned half round.
    public static boolean nearest(float[] aim, float[] to) {
        float yaw = aim[YAW];
        float pitch = aim[PITCH];
        float flippedYaw = yaw + Mth.PI;
        float flippedPitch = Mth.PI - pitch;
        yaw += Math.round((to[YAW] - yaw) / Mth.TWO_PI) * Mth.TWO_PI;
        pitch += Math.round((to[PITCH] - pitch) / Mth.TWO_PI) * Mth.TWO_PI;
        flippedYaw += Math.round((to[YAW] - flippedYaw) / Mth.TWO_PI) * Mth.TWO_PI;
        flippedPitch += Math.round((to[PITCH] - flippedPitch) / Mth.TWO_PI) * Mth.TWO_PI;
        boolean flip = Math.abs(to[YAW] - flippedYaw) + Math.abs(to[PITCH] - flippedPitch)
                < Math.abs(to[YAW] - yaw) + Math.abs(to[PITCH] - pitch);
        aim[YAW] = flip ? flippedYaw : yaw;
        aim[PITCH] = flip ? flippedPitch : pitch;
        return flip;
    }

    private static double smooth(double t) {
        double u = Mth.clamp(t, 0.0, 1.0);
        return u * u * (3.0 - 2.0 * u);
    }
}
