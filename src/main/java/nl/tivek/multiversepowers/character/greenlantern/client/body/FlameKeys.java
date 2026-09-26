package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FirePainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;
import nl.tivek.multiversepowers.engine.math.Noise;

abstract class FlameKeys extends FlameCurves {
    static final float SETTLE = 7.0F;
    static final float BLEND_IN = 4.0F;
    static final float IDLE_IN = 10.0F;
    static final double VIEW_SWEEP = 0.8;

    static final Pose GUARD = pose(0.42, -0.50, -0.86, -0.13, 0.04, -1.0, 0.05, 1.0, 0.03, -0.30, -0.62, -0.80, 1.0F,
            0.0F, 30.0F, 0.05F, 0.1F);
    static final Pose REST = rest(pose(RechargeAnimation.HAND_RIGHT.x(), RechargeAnimation.HAND_RIGHT.y(),
            RechargeAnimation.HAND_RIGHT.z(), -0.2, -0.6, -0.77, 0.0, 0.77, -0.6, -0.46, -1.3, -0.84, 0.0F, 0.0F, 0.0F,
            0.0F, 0.0F));
    private static final Pose BRACED = pose(0.39, -0.45, -0.82, -0.17, 0.07, -0.99, 0.04, 1.0, 0.06, -0.3, -0.6,
            -0.8, 1.0F, 0.0F, 25.0F, 0.2F, 0.35F);
    private static final Pose SWIRL = pose(0.36, -0.58, -0.72, -0.05, -0.8, -0.55, 0.0, 0.55, -0.8, -0.3, -0.6, -0.8,
            1.0F, 0.0F, 20.0F, -0.05F, 0.35F);

    static final Map<FlameMove, Keyframes.Key[]> MOVES = new EnumMap<>(FlameMove.class);

    static {
        equip();
        attacks();
        MOVES.put(FlameMove.VENT, new Keyframes.Key[] {
                key(2, false, pose(0.42, -0.47, -0.77, -0.06, 0.14, -0.99, 0.04, 1.0, -0.1, -0.3, -0.6, -0.8, 1.0F,
                        0.0F, 26.0F, 0.12F, 0.3F)),
                key(6, true, pose(0.43, -0.54, -0.85, -0.10, -0.14, -0.98, 0.05, 1.0, 0.1, -0.3, -0.6, -0.8, 1.0F,
                        0.0F, 30.0F, 0.06F, 0.15F)),
                key(11, false, GUARD) });
        MOVES.put(FlameMove.WALL, new Keyframes.Key[] {
                key(3, true, pose(0.46, -0.53, -0.81, 0.45, -0.62, -0.64, 0.0, 0.7, -0.7, -0.3, -0.6, -0.8, 1.0F, 0.0F,
                        45.0F, 0.35F, 0.3F)),
                key(FlameMove.LAY_FROM, false, pose(0.46, -0.54, -0.81, 0.44, -0.63, -0.64, 0.0, 0.7, -0.7, -0.3,
                        -0.6, -0.8, 1.0F, 0.0F, 44.0F, 0.36F, 0.3F)),
                key(7.5F, false, pose(0.41, -0.56, -0.83, 0.0, -0.68, -0.73, 0.0, 0.73, -0.68, -0.3, -0.6, -0.8, 1.0F,
                        0.0F, 20.0F, 0.4F, 0.3F)),
                key(FlameMove.LAY_TO, true, pose(0.37, -0.54, -0.83, -0.45, -0.62, -0.64, 0.0, 0.7, -0.7, -0.3, -0.6,
                        -0.8, 1.0F, 0.0F, -5.0F, 0.36F, 0.3F)),
                key(12.5F, true, pose(0.44, -0.43, -0.73, -0.25, 0.15, -0.96, 0.04, 1.0, 0.15, -0.3, -0.6, -0.8, 1.0F,
                        0.0F, 10.0F, -0.15F, 0.2F)),
                key(16, false, pose(0.43, -0.48, -0.83, -0.12, 0.06, -0.99, 0.05, 1.0, 0.05, -0.3, -0.6, -0.8, 1.0F,
                        0.0F, 26.0F, 0.02F, 0.12F)) });
        MOVES.put(FlameMove.BURST, new Keyframes.Key[] {
                key(FlameMove.BLAST, true, pose(0.42, -0.65, -0.81, 0.0, -0.85, -0.53, 0.0, 0.53, -0.85, -0.3, -0.6,
                        -0.8, 1.0F, 0.0F, 25.0F, 0.3F, 0.4F)),
                key(5, true, pose(0.42, -0.43, -0.81, -0.08, 0.25, -0.96, 0.04, 1.0, 0.25, -0.3, -0.6, -0.8, 1.0F, 0.0F,
                        22.0F, -0.1F, 0.25F)),
                key(9, false, GUARD) });
    }

    private static void equip() {
        float grab = FlameMove.LEFT_GRAB;
        float valve = FlameMove.VALVE;
        float back = FlameMove.LEFT_BACK;
        float test = FlameMove.TEST;
        MOVES.put(FlameMove.EQUIP, new Keyframes.Key[] {
                key(3, false, pose(0.30, -0.50, -0.78, -0.42, 0.06, -0.90, 0.05, 1.0, 0.0, -0.14, -0.60, -0.86, 0.0F,
                        0.0F, 10.0F, 0.0F, 0.0F)),
                key(8, false, pose(0.28, -0.43, -0.80, -0.50, 0.10, -0.86, 0.08, 1.0, 0.05, -0.06, -0.58, -0.90, 0.0F,
                        0.0F, 12.0F, 0.02F, 0.0F)),
                key(12, false, pose(0.27, -0.41, -0.80, -0.55, 0.12, -0.83, 0.1, 1.0, 0.08, 0.0, -0.56, -0.95, 0.3F,
                        0.0F, 14.0F, 0.03F, 0.0F)),
                key(grab, true, pose(0.27, -0.44, -0.80, -0.52, 0.07, -0.85, 0.08, 1.0, 0.06, 0.0, -0.56, -0.95, 1.0F,
                        0.0F, 16.0F, 0.05F, 0.05F)),
                key(17, false, pose(0.25, -0.40, -0.78, -0.70, 0.05, -0.71, 0.05, 1.0, 0.1, 0.0, -0.56, -0.95, 1.0F,
                        0.0F, 18.0F, 0.04F, 0.05F)),
                key(valve - 2.0F, false, pose(0.25, -0.40, -0.78, -0.74, 0.02, -0.67, 0.05, 1.0, 0.1, 0.0, -0.56,
                        -0.95, 1.0F, 1.0F, 18.0F, 0.04F, 0.05F)),
                key(valve, true, pose(0.25, -0.41, -0.78, -0.74, 0.01, -0.67, 0.08, 1.0, 0.1, 0.0, -0.56, -0.95, 1.0F,
                        1.0F, 18.0F, 0.05F, 0.05F)),
                key(valve + 3.0F, true, pose(0.25, -0.40, -0.78, -0.73, 0.03, -0.68, 0.05, 1.0, 0.1, 0.0, -0.56,
                        -0.95, 1.0F, 1.0F, 18.0F, 0.04F, 0.05F)),
                key(back, false, pose(0.26, -0.40, -0.79, -0.45, 0.10, -0.89, 0.05, 1.0, 0.1, 0.0, -0.56, -0.95, 1.0F,
                        0.0F, 20.0F, 0.04F, 0.05F)),
                key(FlameMove.SPARK, true, pose(0.26, -0.38, -0.80, -0.30, 0.22, -0.93, 0.05, 1.0, 0.2, 0.0, -0.56,
                        -0.95, 1.0F, 0.0F, 22.0F, 0.03F, 0.05F)),
                key(FlameMove.SPARK_AGAIN, true, pose(0.265, -0.385, -0.79, -0.29, 0.23, -0.93, 0.05, 1.0, 0.2, 0.0,
                        -0.56, -0.95, 1.0F, 0.0F, 22.0F, 0.03F, 0.05F)),
                key(FlameMove.PILOT, true, pose(0.26, -0.38, -0.80, -0.28, 0.24, -0.93, 0.05, 1.0, 0.22, 0.0, -0.56,
                        -0.95, 1.0F, 0.0F, 22.0F, 0.02F, 0.05F)),
                key(38, false, pose(0.30, -0.34, -0.76, -0.12, 0.55, -0.83, 0.05, 0.83, 0.55, 0.0, -0.56, -0.95, 1.0F,
                        0.0F, 26.0F, -0.06F, 0.12F)),
                key(test - 1.0F, true, pose(0.31, -0.33, -0.77, -0.10, 0.60, -0.79, 0.05, 0.79, 0.6, 0.0, -0.56,
                        -0.95, 1.0F, 0.0F, 27.0F, -0.08F, 0.15F)),
                key(test, false, pose(0.32, -0.30, -0.68, -0.08, 0.68, -0.73, 0.05, 0.73, 0.68, 0.0, -0.56, -0.95,
                        1.0F, 0.0F, 27.0F, -0.14F, 0.18F)),
                key(test + 3.0F, false, pose(0.31, -0.33, -0.74, -0.10, 0.62, -0.78, 0.05, 0.78, 0.62, 0.0, -0.56,
                        -0.95, 1.0F, 0.0F, 27.0F, -0.1F, 0.18F)),
                key(test + FlameMove.TEST_TICKS, true, pose(0.31, -0.34, -0.75, -0.10, 0.58, -0.81, 0.05, 0.81, 0.58,
                        0.0, -0.56, -0.95, 1.0F, 0.0F, 28.0F, -0.06F, 0.15F)),
                key(53, false, pose(0.41, -0.47, -0.86, -0.12, 0.12, -0.99, 0.05, 1.0, -0.1, 0.0, -0.56, -0.95, 1.0F,
                        0.0F, 30.0F, 0.04F, 0.1F)),
                key(58, true, pose(0.42, -0.51, -0.85, -0.10, -0.02, -1.0, 0.05, 1.0, 0.03, 0.0, -0.56, -0.95, 1.0F,
                        0.0F, 30.0F, 0.06F, 0.1F)),
                key(FlameMove.EQUIP.ticks(), false, GUARD) });
    }

    // Each attack: its wind-up, the body along every stroke (the nozzle follows the stroke's own path), and its
    // follow-through. Keys give degrees right and up, the fist's shift, and twist, lean and step on top of the aim's.
    private static void attacks() {
        attack(FlameMove.SWEEP, new Body(0.0, 0.0, 0.0, 0.0F, 0.16F, 0.28F),
                at(2, true, 58, 0, 0.0, 0.01, 0.03, 2, 0.12F, 0.2F, 0),
                at(10.5F, true, -60, 0, 0.0, -0.01, 0.03, -4, 0.12F, 0.2F, 0));
        attack(FlameMove.SWEEP_BACK, new Body(0.0, 0.0, 0.0, 0.0F, 0.16F, 0.28F),
                at(2, true, -58, 0, 0.0, 0.01, 0.03, -2, 0.12F, 0.2F, 0),
                at(10.5F, true, 60, 0, 0.0, -0.01, 0.03, 4, 0.12F, 0.2F, 0));
        attack(FlameMove.RISING, new Body(0.0, 0.0, 0.0, 0.0F, 0.12F, 0.35F),
                at(2, true, -52, -34, -0.02, -0.03, 0.0, 0, 0.28F, 0.3F, 0),
                at(10.5F, true, 52, 40, 0.02, 0.03, 0.02, 4, -0.05F, 0.25F, 0));
        attack(FlameMove.CHOP, new Body(0.0, 0.0, 0.0, 0.0F, 0.2F, 0.35F),
                at(2, true, 52, 40, 0.02, 0.04, 0.03, 6, -0.08F, 0.2F, 0),
                at(10.5F, true, -52, -36, 0.0, -0.02, -0.02, -4, 0.35F, 0.35F, 0));
        attack(FlameMove.GEYSER, new Body(0.0, 0.0, -0.02, 0.0F, 0.15F, 0.4F),
                at(2.5F, true, 0, -58, -0.03, -0.02, -0.04, -6, 0.42F, 0.35F, 0),
                at(10.5F, true, 0, 48, 0.0, 0.05, 0.03, -4, -0.18F, 0.3F, 0));
        attack(FlameMove.OVERHEAD, new Body(0.0, 0.02, -0.02, -4.0F, 0.25F, 0.45F),
                at(2.5F, false, 10, 55, -0.08, 0.12, 0.18, -10, -0.15F, 0.15F, 0),
                at(5, true, 8, 80, -0.12, 0.2, 0.26, -14, -0.25F, 0.1F, 0),
                at(11.5F, true, -4, -46, 0.0, -0.03, -0.06, -2, 0.55F, 0.55F, 0),
                at(14, false, -2, -20, 0.0, -0.01, -0.03, 0, 0.3F, 0.35F, 0));
        attack(FlameMove.JAB, new Body(0.0, 0.0, -0.08, 2.0F, 0.22F, 0.35F),
                at(1.2F, true, 0, 2, 0.0, 0.0, 0.06, 4, 0.0F, 0.1F, 0),
                at(5.5F, true, 0, 6, 0.0, 0.01, 0.02, 2, 0.1F, 0.25F, 0));
        attack(FlameMove.LUNGE, new Body(0.0, 0.0, -0.1, 0.0F, 0.4F, 0.95F),
                at(2.5F, true, 0, -2, -0.02, -0.01, 0.08, 8, -0.05F, 0.0F, 0),
                at(11.5F, true, 0, 0, 0.0, 0.0, -0.06, 0, 0.3F, 0.8F, 0));
        attack(FlameMove.LOW_SWEEP, new Body(0.0, -0.08, 0.0, 0.0F, 0.55F, 0.65F),
                at(2.5F, true, 78, -30, 0.02, -0.08, 0.0, 6, 0.55F, 0.6F, 0),
                at(11.5F, true, -80, -26, 0.0, -0.07, 0.0, -4, 0.5F, 0.6F, 0));
        attack(FlameMove.SPIN, new Body(0.0, 0.0, 0.0, 0.0F, 0.22F, 0.4F),
                at(2.5F, true, 30, -4, 0.03, 0.0, 0.0, 10, 0.15F, 0.3F, -25),
                at(15.5F, true, SPIN_HELD - 10, -6, 0.0, 0.0, 0.0, 0, 0.15F, 0.3F, 380));
        attack(FlameMove.CROSS, new Body(0.0, 0.0, 0.0, 0.0F, 0.18F, 0.3F),
                at(2, true, -55, 38, -0.02, 0.04, 0.03, -4, -0.05F, 0.2F, 0),
                at(8, true, 55, 38, 0.02, 0.05, 0.03, 4, -0.05F, 0.3F, 0),
                at(14.5F, true, -55, -30, 0.0, -0.02, -0.02, -4, 0.3F, 0.35F, 0));
        attack(FlameMove.SPIRAL, new Body(0.0, 0.0, -0.05, 0.0F, 0.25F, 0.4F),
                at(2, true, 26, -4, 0.0, 0.0, 0.04, 2, 0.1F, 0.2F, 0),
                at(13.5F, true, 8, 6, 0.0, 0.0, 0.0, 0, 0.15F, 0.3F, 0));
    }

    // In the spin the gun stays out a little to the right while the whole body turns round.
    static final double SPIN_HELD = 20.0;

    private record Body(double dx, double dy, double dz, float twist, float lean, float step) {
    }

    private static void attack(FlameMove move, Body body, Keyframes.Key... around) {
        List<Keyframes.Key> keys = new ArrayList<>(List.of(around));
        boolean spin = move == FlameMove.SPIN;
        for (FlameMove.Stroke stroke : move.strokes()) {
            float length = stroke.to() - stroke.from();
            int parts = length >= 6.0F ? 4 : 2;
            for (int i = 0; i <= parts; i++) {
                float tick = stroke.from() + length * i / parts;
                FlameMove.Aim aim = stroke.aim(tick);
                double yaw = spin ? SPIN_HELD : aim.yaw();
                double orbit = spin ? SPIN_HELD - aim.yaw() : 0.0;
                keys.add(at(tick, false, yaw, aim.pitch(), body.dx(), body.dy(), body.dz(), body.twist(), body.lean(),
                        body.step(), orbit));
            }
        }
        keys.sort(Comparator.comparingDouble(Keyframes.Key::tick));
        MOVES.put(move, keys.toArray(Keyframes.Key[]::new));
    }

    private static Keyframes.Key at(float tick, boolean stop, double yaw, double pitch, double dx, double dy,
            double dz, double twist, float lean, float step, double orbit) {
        return key(tick, stop, aimed(yaw, pitch, dx, dy, dz, twist, lean, step, orbit));
    }

    // The guard, aimed: the fist follows the nozzle a little, the body turns with it and bends over to aim low.
    static Pose aimed(double yaw, double pitch, double dx, double dy, double dz, double twist, float lean, float step,
            double orbit) {
        float[] n = GUARD.numbers();
        double raise = Math.toRadians(pitch);
        Vec3 muzzle = pitched(GUARD.muzzle(), raise);
        Vec3 top = pitched(GUARD.top(), raise);
        n[0] += (float) (0.0009 * yaw + dx);
        n[1] += (float) (0.0022 * pitch + dy);
        n[2] += (float) (0.0012 * pitch + dz);
        n[3] = (float) muzzle.x;
        n[4] = (float) muzzle.y;
        n[5] = (float) muzzle.z;
        n[6] = (float) top.x;
        n[7] = (float) top.y;
        n[8] = (float) top.z;
        n[14] = (float) Math.toRadians(30.0 + 0.36 * yaw + twist);
        n[15] = lean - 0.004F * (float) pitch;
        n[16] = step;
        n[17] = (float) Math.toRadians(yaw);
        n[Pose.ORBIT] = (float) Math.toRadians(orbit);
        return Pose.of(n);
    }

    // A view-space way raised by the angle (radians) about the view's right.
    static Vec3 pitched(Vec3 way, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(way.x, way.y * cos - way.z * sin, way.y * sin + way.z * cos);
    }

    private static Pose rest(Pose pose) {
        float[] n = pose.numbers();
        n[18] = 1.0F;
        return Pose.of(n);
    }

    static Pose idle(float time, float amount) {
        if (amount <= 0.0F) {
            return GUARD;
        }
        float[] n = GUARD.numbers();
        float breath = Mth.sin(time * 0.13F);
        float sway = Mth.sin(time * 0.061F + 1.3F);
        float drift = Mth.sin(time * 0.037F + 0.4F);
        n[0] += amount * 0.005F * sway;
        n[1] += amount * 0.008F * breath;
        n[2] += amount * 0.004F * drift;
        n[3] += amount * 0.012F * sway;
        n[4] += amount * 0.01F * breath;
        n[14] += amount * 0.03F * sway;
        n[15] += amount * 0.012F * breath;
        return Pose.of(n);
    }

    static Pose inferno(float t, float time) {
        float[] n = BRACED.numbers();
        double kick = t < FlameMove.BRACE ? 0.0 : Math.exp(-(t - FlameMove.BRACE) / 3.0) * Math.min(1.0,
                (t - FlameMove.BRACE) * 2.0);
        float tremble = t < FlameMove.BRACE ? 0.0F : 1.0F;
        n[0] += tremble * 0.004F * (float) (Noise.of((int) (time * 2.0F), 1, 3) - 0.5);
        n[1] += tremble * 0.006F * Mth.sin(time * 2.7F) + 0.012F * (float) kick;
        n[2] += 0.06F * (float) kick + tremble * 0.003F * Mth.sin(time * 3.1F);
        n[3] += tremble * 0.008F * Mth.sin(time * 2.3F + 1.0F);
        n[4] += tremble * 0.008F * Mth.sin(time * 1.9F) + 0.05F * (float) kick;
        n[15] += 0.06F * (float) kick;
        return Pose.of(n);
    }

    static Pose vortex(float t, float time) {
        float[] n = SWIRL.numbers();
        float phase = (float) (time * FirePainter.VORTEX_SPIN);
        float spun = (float) Ease.smooth(t / FlameMove.SPIN_UP);
        n[3] += spun * 0.42F * Mth.sin(phase);
        n[5] += spun * 0.3F * Mth.cos(phase);
        n[0] += spun * 0.04F * Mth.sin(phase);
        n[14] += spun * 0.25F * Mth.sin(phase);
        return Pose.of(n);
    }
}
