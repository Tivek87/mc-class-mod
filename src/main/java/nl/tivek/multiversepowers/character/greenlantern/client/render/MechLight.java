package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

final class MechLight {
    private static final double BEAM = 0.55;
    private static final Vec3 X = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 Z = new Vec3(0.0, 0.0, 1.0);

    private MechLight() {
    }

    static void lights(LanternPainter painter, MechScript.Stage stage, double t, @Nullable Vec3 ring, boolean own) {
        if (ring != null) {
            calls(painter, stage, t, ring);
        }
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            double landed = MechScript.landed(right, t);
            if (landed >= 0.0 && landed < 10.0) {
                Vec3 foot = stage.point(MechScript.foot(right, t).at());
                ground(painter, foot, landed, t < MechScript.STEPS[side] - MechScript.STEP_TICKS ? 3.2 : 2.0);
            }
            lock(painter, stage.point(MechPainter.side(MechScript.KNEE, right)), t - MechScript.KNEES[side], 1.0);
            lock(painter, stage.point(MechPainter.side(MechScript.ELBOW, right)), t - MechScript.ARMS_LOCK, 1.4);
        }
        lock(painter, stage.point(0.0, 5.2, 0.0), t - MechScript.HIPS_LOCK, 1.4);
        clap(painter, stage, t);
        drill(painter, stage, t);
        lock(painter, stage.point(MechScript.NECK).add(0.0, -0.45, 0.0), t - MechScript.CLACK, 2.2);
        if (t >= MechScript.DONE - 6.0) {
            idle(painter, stage, t, own);
        }
    }

    private static void calls(LanternPainter painter, MechScript.Stage stage, double t, Vec3 ring) {
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            double form = MechScript.BOOT_FORM + side * MechScript.BOOT_GAP;
            beam(painter, ring, stage.point(MechScript.foot(right, t).at()).add(0.0, 0.6, 0.0), t, form,
                    MechScript.FORM_TICKS);
            Vec3 ankle = stage.point(MechPainter.side(MechScript.ANKLE, right));
            Vec3 knee = stage.point(MechPainter.side(MechScript.KNEE, right));
            Vec3 hip = stage.point(MechPainter.side(MechScript.HIP, right));
            beam(painter, ring, ankle.lerp(knee, MechScript.grown(t, MechScript.SHINS[side])), t,
                    MechScript.SHINS[side], MechScript.FORM_TICKS);
            beam(painter, ring, knee.lerp(hip, MechScript.grown(t, MechScript.THIGHS[side])), t,
                    MechScript.THIGHS[side], MechScript.FORM_TICKS);
            MechScript.Arm arm = MechScript.arm(right, t);
            beam(painter, ring, stage.point(arm.elbow()).add(stage.dir(arm.way()).scale(1.8 * arm.grown())), t,
                    MechScript.ARMS_FORM, MechScript.FORM_TICKS);
            beam(painter, ring, stage.point(MechPainter.side(MechScript.SHOULDER, right)), t, MechScript.SHOULDERS,
                    10.0);
        }
        beam(painter, ring, stage.point(0.0, 5.2, 0.0), t, MechScript.HIPS, MechScript.HIPS_LOCK - MechScript.HIPS);
        double torso = Mth.clamp((t - MechScript.TORSO) / MechScript.TORSO_TICKS, 0.0, 1.0);
        beam(painter, ring, stage.point(0.0, 5.5 + torso * 3.0, 0.9), t, MechScript.TORSO, MechScript.TORSO_TICKS);
        beam(painter, ring, stage.point(MechScript.head(t)), t, MechScript.HEAD_FORM,
                MechScript.HEAD_AHEAD - MechScript.HEAD_FORM);
    }

    private static void beam(LanternPainter painter, Vec3 ring, Vec3 target, double t, double from, double ticks) {
        double since = t - from;
        if (since < -1.0 || since > ticks + 3.0) {
            return;
        }
        double strength = Ease.smooth((since + 1.0) / 2.0) * (1.0 - Ease.smooth((since - ticks) / 3.0));
        painter.beamOfLight(ring, target, strength, since + 1.0, BEAM);
        painter.flare(target, 0.6, 0.7 * strength);
    }

    private static void lock(LanternPainter painter, Vec3 at, double since, double size) {
        if (since < 0.0 || since > 6.0) {
            return;
        }
        double flash = 1.0 - since / 6.0;
        painter.flare(at, size * (0.6 + 0.6 * (1.0 - flash)), flash);
    }

    private static void ground(LanternPainter painter, Vec3 at, double since, double reach) {
        double u = Ease.smooth(since / 8.0);
        double fade = 1.0 - since / 10.0;
        painter.circle(at.add(0.0, 0.08, 0.0), X, Z, 0.4 + reach * u, 0.08, 0.5, Colors.alpha(0.9 * fade),
                Colors.alpha(0.45 * fade));
    }

    private static void clap(LanternPainter painter, MechScript.Stage stage, double t) {
        double since = t - MechScript.CLAP;
        if (since < -1.0 || since > 10.0) {
            return;
        }
        Vec3 at = stage.point(MechScript.CLAP_AT);
        double flash = since < 0.0 ? Ease.smooth(since + 1.0) : 1.0 - Ease.smooth(since / 6.0);
        painter.flare(at, 1.8 * (0.6 + 0.8 * Math.max(0.0, since) / 6.0), flash);
        if (since >= 0.0) {
            double u = Ease.smooth(since / 7.0);
            double fade = 1.0 - since / 10.0;
            Vec3 a = stage.dir(new Vec3(0.0, 1.0, 0.0));
            Vec3 b = stage.dir(new Vec3(0.0, 0.0, 1.0));
            painter.circle(at, a, b, 0.5 + 3.5 * u, 0.1, 0.8, Colors.alpha(0.95 * fade), Colors.alpha(0.5 * fade));
            painter.circle(at, a, b, 0.3 + 2.4 * u, 0.06, 0.5, Colors.alpha(0.7 * fade), Colors.alpha(0.35 * fade));
            painter.shell(at, 0.4 + 3.0 * u, painter.material().glow(), 0.7 * fade);
        }
    }

    private static void drill(LanternPainter painter, MechScript.Stage stage, double t) {
        Vec3 at = stage.point(MechScript.DRILL_AT);
        double since = t - MechScript.IMPACT;
        if (t > MechScript.DRILL && since < 0.0) {
            double u = (t - MechScript.DRILL) / (MechScript.IMPACT - MechScript.DRILL);
            Vec3 head = stage.point(MechScript.head(t));
            painter.glowTaper(head.add(0.0, -1.2, 0.0), head.add(0.0, 2.5, 0.0), 0.3, 1.4, LanternBeams.GREEN,
                    0.4 * u, 0.0);
        }
        if (since < 0.0 || since > 14.0) {
            return;
        }
        double flash = 1.0 - Ease.smooth(since / 8.0);
        painter.flare(at.add(0.0, 0.3, 0.0), 2.6 * (0.7 + 0.5 * (1.0 - flash)), flash);
        double u = Ease.smooth(since / 9.0);
        double fade = 1.0 - since / 14.0;
        painter.circle(at.add(0.0, 0.08, 0.0), X, Z, 0.6 + 4.8 * u, 0.12, 0.9, Colors.alpha(0.95 * fade),
                Colors.alpha(0.5 * fade));
        for (int k = 0; k < 10; k++) {
            double angle = Math.PI * 2.0 * (k + Noise.of(k, 5, 1) * 0.5) / 10.0;
            double reach = (1.4 + 2.2 * Noise.of(k, 5, 2)) * Ease.smooth(since / 3.0);
            Vec3 last = at.add(0.0, 0.06, 0.0);
            for (int step = 1; step <= 3; step++) {
                double bend = angle + (Noise.of(k, step, 3) - 0.5) * 0.5;
                Vec3 next = at.add(Math.cos(bend) * reach * step / 3.0, 0.06, Math.sin(bend) * reach * step / 3.0);
                painter.lightLine(last, next, 0.06, LanternBeams.HOT, Colors.alpha(0.9 * fade));
                painter.glowLine(last, next, 0.3, LanternBeams.GREEN, Colors.alpha(0.45 * fade));
                last = next;
            }
        }
    }

    private static void idle(LanternPainter painter, MechScript.Stage stage, double t, boolean own) {
        double on = Ease.smooth((t - (MechScript.DONE - 6.0)) / 6.0);
        double breath = 0.7 + 0.3 * Math.sin(t * 0.12);
        Vec3 glass = stage.point(MechShapes.GLASS_AT.x, MechShapes.GLASS_AT.y, MechShapes.GLASS_AT.z + 0.24);
        painter.circle(glass, stage.right(), new Vec3(0.0, 1.0, 0.0), MechShapes.GLASS_RADIUS + 0.28, 0.05,
                own ? 0.2 : 0.6, Colors.alpha(0.8 * on * breath), Colors.alpha((own ? 0.15 : 0.45) * on * breath));
        Vec3 visor = stage.point(MechScript.NECK.x, MechScript.NECK.y + 0.12, MechScript.NECK.z + 0.78);
        painter.flare(visor, 0.5, 0.8 * on * breath);
        for (int side = -1; side <= 1; side += 2) {
            painter.flare(stage.point(MechShapes.EXHAUST.x * side, MechShapes.EXHAUST.y + 0.05, MechShapes.EXHAUST.z),
                    0.4, 0.7 * on * breath);
        }
    }
}
