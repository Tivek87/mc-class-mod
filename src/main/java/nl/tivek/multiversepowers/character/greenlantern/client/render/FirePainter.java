package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameWall;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

public final class FirePainter {
    static final int CORE = 0xF2FFF4;
    static final int HOT = 0xB8FFC8;
    static final int FLAME = 0x4CF070;
    static final int DEEP = 0x14A83A;
    static final int SMOKE = 0xB4C8BA;
    private static final double WALL_STEP = 0.34;
    private static final int VORTEX_RIBBONS = 6;
    private static final int VORTEX_STEPS = 10;
    private static final double VORTEX_TWIST = 1.7;
    public static final double VORTEX_SPIN = 0.42;

    private FirePainter() {
    }

    public static void ball(LanternPainter painter, Vec3 at, double radius, double heat, double strength, int seed) {
        if (strength <= 0.01 || radius <= 1.0E-3 || !painter.visible(at, radius * 1.5)) {
            return;
        }
        // A flat disc seen from inside the ball would cover the whole view: it thins out as the eye comes close.
        double away = painter.camera().distanceTo(at);
        strength *= Ease.smooth((away - radius * 0.5) / (radius * 1.5 + 0.5));
        if (strength <= 0.01) {
            return;
        }
        float hot = (float) Mth.clamp(heat, 0.0, 1.0);
        painter.glowDisc(at, radius * 1.4, Colors.mix(DEEP, FLAME, hot), 0.5 * strength, 0.25, seed);
        painter.lightDisc(at, radius, Colors.mix(FLAME, HOT, hot), strength * (0.3 + 0.35 * hot), 0.35, seed + 1);
        if (hot > 0.2F) {
            painter.glowDisc(at, radius * 0.5, Colors.mix(HOT, CORE, hot), 0.6 * strength * hot, 0.2, seed + 2);
        }
    }

    public static void smoke(LanternPainter painter, Vec3 at, double radius, double strength, int seed) {
        if (strength > 0.01 && painter.visible(at, radius)) {
            painter.lightDisc(at, radius, SMOKE, 0.14 * strength, 0.3, seed);
        }
    }

    public static void ember(LanternPainter painter, Vec3 at, Vec3 way, double strength) {
        if (strength <= 0.01) {
            return;
        }
        painter.lightLine(at, at.add(way), 0.03, HOT, Colors.alpha(0.9 * strength));
        painter.glowLine(at, at.add(way), 0.12, FLAME, Colors.alpha(0.45 * strength));
    }

    public static void tongue(LanternPainter painter, Vec3 base, Vec3 up, double height, double width,
            double strength, int seed, double time) {
        if (strength <= 0.01 || height <= 0.01 || !painter.visible(base.add(up.scale(height * 0.5)), height)) {
            return;
        }
        Vec3[] across = Vectors.across(up);
        int steps = 6;
        Vec3 last = base;
        double lastWide = width * 0.7;
        double phase = Noise.of(seed, 3, 1) * Math.PI * 2.0;
        for (int i = 1; i <= steps; i++) {
            double u = (double) i / steps;
            double sway = Math.sin(time * 0.9 + phase + u * 4.0) * 0.55 + (Noise.of(seed, i, (int) (time * 0.5)) - 0.5)
                    * 0.5;
            double swing = Math.cos(time * 0.7 + phase * 1.3 + u * 3.0) * 0.4;
            double lick = 1.0 + 0.18 * Math.sin(time * 1.3 + phase);
            Vec3 next = base.add(up.scale(height * u * lick)).add(across[0].scale(sway * width * u))
                    .add(across[1].scale(swing * width * u));
            double wide = width * Math.sin(Math.PI * (0.18 + 0.82 * u)) * (1.0 - 0.55 * u);
            double a0 = strength * (1.0 - 0.75 * (u - 1.0 / steps));
            double a1 = strength * (1.0 - 0.75 * u);
            painter.glowTaper(last, next, lastWide * 2.0, wide * 2.0, Colors.mix(DEEP, FLAME, (float) (1.0 - u)),
                    0.5 * a0, 0.5 * a1);
            painter.lightTaper(last, next, lastWide, wide, Colors.mix(FLAME, HOT, (float) (0.6 - 0.6 * u)), 0.6 * a0,
                    0.6 * a1);
            if (u <= 0.5) {
                painter.lightTaper(last, next, lastWide * 0.45, wide * 0.45, CORE, 0.85 * a0, 0.85 * a1);
            }
            last = next;
            lastWide = wide;
        }
    }

    public static void pilot(LanternPainter painter, Vec3 tip, Vec3 forward, Vec3 up, double size, double strength,
            double time) {
        if (strength <= 0.01) {
            return;
        }
        Vec3 lean = forward.scale(0.75).add(up.scale(0.45)).normalize();
        double flicker = 0.85 + 0.15 * Math.sin(time * 2.1) * Math.sin(time * 1.3 + 0.7);
        tongue(painter, tip, lean, size * 2.2 * flicker, size * 0.9, strength, 91, time * 1.6);
        painter.glowDisc(tip, size * 1.2, FLAME, 0.6 * strength, 0.2, (int) (time * 3.0));
    }

    public static void spark(LanternPainter painter, Vec3 at, Vec3 forward, double size, double strength, int seed) {
        if (strength <= 0.01) {
            return;
        }
        painter.glowDisc(at, size * 1.6, HOT, 0.9 * strength, 0.1, seed);
        for (int k = 0; k < 6; k++) {
            Vec3 way = Noise.direction(seed, k).add(forward.scale(0.8)).normalize();
            double reach = size * (1.2 + 1.6 * Noise.of(seed, k, 5)) * (1.4 - strength * 0.4);
            painter.lightLine(at.add(way.scale(reach * 0.35)), at.add(way.scale(reach)), size * 0.12, CORE,
                    Colors.alpha(strength));
        }
    }

    public static void wall(LanternPainter painter, @Nullable ClientLevel level, Vec3 center, Vec3 normal,
            double width, double height, double age, double solid) {
        Vec3 along = new Vec3(normal.z, 0.0, -normal.x);
        double laid = Ease.smooth(age / FlameWall.LAY_TICKS);
        double since = age - FlameWall.LAY_TICKS;
        double rise = since <= 0.0 ? 0.0 : Math.min(1.0, since / FlameWall.RISE_TICKS);
        double burst = since <= 0.0 ? 0.0 : Math.max(0.0, 1.0 - Math.abs(since - FlameWall.RISE_TICKS) / 4.0);
        double fall = Mth.clamp(1.0 - solid, 0.0, 1.0);
        double time = painter.time();
        int columns = Math.max(2, (int) Math.round(width / WALL_STEP));
        for (int c = 0; c <= columns; c++) {
            double s = (double) c / columns;
            if (s > laid + 1.0E-3) {
                break;
            }
            Vec3 foot = center.add(along.scale((s - 0.5) * width));
            foot = new Vec3(foot.x, ground(level, foot, center.y), foot.z);
            double edge = Math.min(1.0, Math.min(s, 1.0 - s) * 6.0 + 0.35);
            double low = 0.25 + 0.15 * Math.sin(time * 1.1 + c);
            double full = height * (0.72 + 0.35 * Noise.of(c, 17, (int) (time * 0.25))) * edge;
            double tall = Mth.lerp(Ease.backOut(rise), low, full) * (1.0 + 0.3 * burst) * (1.0 - Ease.smooth(fall));
            double heat = 0.4 + 0.6 * burst;
            Vec3 jitter = normal.scale((Noise.of(c, 5, 9) - 0.5) * 0.35);
            tongue(painter, foot.add(jitter), Vectors.UP, tall, 0.55 + 0.25 * rise, 0.9 * (1.0 - 0.6 * fall), c * 7
                    + 3, time);
            if (c % 2 == 0) {
                ball(painter, foot.add(0.0, 0.15, 0.0), 0.3 + 0.15 * rise, heat, 0.7 * (1.0 - fall), c * 13);
            }
            double cycle = (time * 0.07 + Noise.of(c, 2, 2)) % 1.0;
            if (rise > 0.0 && fall < 0.8) {
                Vec3 spark = foot.add(normal.scale((Noise.of(c, 4, (int) (time * 0.07)) - 0.5) * 0.6))
                        .add(0.0, tall * (0.4 + 0.9 * cycle), 0.0);
                ember(painter, spark, new Vec3(0.0, 0.18, 0.0), (1.0 - cycle) * 0.8 * (1.0 - fall));
            }
            if (fall > 0.0) {
                smoke(painter, foot.add(0.0, 0.4 + 1.6 * fall, 0.0), 0.35 + 0.5 * fall, Math.sin(Math.PI * fall),
                        c * 11);
            }
        }
        if (burst > 0.0) {
            painter.flare(center.add(0.0, height * 0.45, 0.0), width * 0.35 * burst, burst * 0.8);
        }
    }

    private static double ground(@Nullable ClientLevel level, Vec3 at, double fallback) {
        if (level == null) {
            return fallback;
        }
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = BlockPos.containing(at.x, fallback + dy, at.z);
            BlockPos below = pos.below();
            if (!level.isLoaded(below)) {
                return fallback;
            }
            VoxelShape under = level.getBlockState(below).getCollisionShape(level, below);
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && !under.isEmpty()) {
                return below.getY() + under.max(Direction.Axis.Y);
            }
        }
        return fallback;
    }

    public static void vortex(LanternPainter painter, Vec3 feet, double radius, double spun, double strength,
            boolean inside) {
        if (strength <= 0.01) {
            return;
        }
        double time = painter.time();
        double tall = 3.2 * Ease.smooth(spun);
        double quiet = inside ? 0.35 : 1.0;
        double spin = time * VORTEX_SPIN;
        for (int j = 0; j < VORTEX_RIBBONS; j++) {
            double offset = Math.PI * 2.0 * j / VORTEX_RIBBONS;
            Vec3 last = null;
            double lastWide = 0.0;
            for (int i = 0; i <= VORTEX_STEPS; i++) {
                double u = (double) i / VORTEX_STEPS;
                double around = spin + offset + u * VORTEX_TWIST * Math.PI;
                double reach = radius * (0.55 + 0.45 * u) * (0.6 + 0.4 * Ease.smooth(spun))
                        * (1.0 + 0.06 * Math.sin(time * 1.7 + j + u * 5.0));
                Vec3 at = feet.add(Math.cos(around) * reach, tall * u, Math.sin(around) * reach);
                double wide = 0.55 * (1.0 - 0.55 * u) * (0.6 + 0.4 * spun);
                if (last != null) {
                    double a = strength * quiet * (1.0 - 0.8 * u);
                    painter.glowTaper(last, at, lastWide * 2.2, wide * 2.2, DEEP, 0.45 * a, 0.45 * a);
                    painter.lightTaper(last, at, lastWide, wide, FLAME, 0.55 * a, 0.5 * a);
                    painter.lightTaper(last, at, lastWide * 0.35, wide * 0.35, CORE, 0.8 * a, 0.6 * a);
                }
                last = at;
                lastWide = wide;
            }
        }
        painter.circle(feet.add(0.0, 0.08, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), radius * 0.95, 0.07,
                0.45, Colors.alpha(0.9 * strength * quiet), Colors.alpha(0.45 * strength * quiet));
        for (int k = 0; k < 10; k++) {
            double cycle = (time * 0.05 + Noise.of(k, 8, 1)) % 1.0;
            double around = spin * 1.2 + Noise.of(k, 8, 2) * Math.PI * 2.0;
            double reach = radius * (0.6 + 0.6 * cycle);
            Vec3 at = feet.add(Math.cos(around) * reach, 0.3 + tall * cycle, Math.sin(around) * reach);
            Vec3 way = new Vec3(-Math.sin(around), 0.4, Math.cos(around)).scale(0.2);
            ember(painter, at, way, strength * quiet * (1.0 - cycle));
        }
    }

    public static void burst(LanternPainter painter, Vec3 feet, double radius, double since) {
        if (since < 0.0 || since > 12.0) {
            return;
        }
        double u = since / 12.0;
        double reach = radius * 1.35 * Ease.smooth(Math.min(1.0, since / 6.0));
        double fade = (1.0 - u) * (1.0 - u);
        int count = 18;
        for (int k = 0; k < count; k++) {
            double around = Math.PI * 2.0 * k / count + Noise.of(k, 9, 3) * 0.3;
            Vec3 out = new Vec3(Math.cos(around), 0.0, Math.sin(around));
            Vec3 at = feet.add(out.scale(reach)).add(0.0, 0.3 + 0.4 * Noise.of(k, 9, 4), 0.0);
            ball(painter, at, 0.35 + 0.4 * u, 1.0 - u, fade, k * 5 + (int) since);
            tongue(painter, at, out.add(0.0, 1.2, 0.0).normalize(), 1.2 * fade, 0.4, fade, k * 3, painter.time());
        }
        painter.circle(feet.add(0.0, 0.1, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), reach, 0.08, 0.6,
                Colors.alpha(fade), Colors.alpha(0.5 * fade));
    }

    public static void burn(LanternPainter painter, Vec3 feet, double width, double height, double strength,
            int seed) {
        if (strength <= 0.01) {
            return;
        }
        double time = painter.time();
        double half = width * 0.55;
        int around = 7;
        painter.glowDisc(feet.add(0.0, height * 0.5, 0.0), Math.max(width, height) * 0.7, FLAME, 0.3 * strength, 0.3,
                seed);
        for (int k = 0; k < around; k++) {
            double angle = Math.PI * 2.0 * k / around + seed * 0.7;
            Vec3 foot = feet.add(Math.cos(angle) * half, height * 0.02, Math.sin(angle) * half);
            double tall = height * (0.7 + 0.45 * Noise.of(seed, k, (int) (time * 0.3)));
            tongue(painter, foot, Vectors.UP, tall, width * 0.7, strength, seed * 31 + k, time * 1.2);
        }
        tongue(painter, feet.add(0.0, height * 0.3, 0.0), Vectors.UP, height * 1.05, width * 0.8, strength * 0.8,
                seed * 31 + 9, time * 1.1);
        for (int k = 0; k < 3; k++) {
            double cycle = (time * 0.06 + Noise.of(seed, k, 6)) % 1.0;
            Vec3 at = feet.add((Noise.of(seed, k, 7) - 0.5) * width, height * (0.5 + 0.9 * cycle),
                    (Noise.of(seed, k, 8) - 0.5) * width);
            ember(painter, at, new Vec3(0.0, 0.15, 0.0), strength * (1.0 - cycle));
        }
    }
}
