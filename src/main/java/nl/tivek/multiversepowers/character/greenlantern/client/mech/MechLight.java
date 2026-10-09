package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechBodyShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechHeadShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechLegShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBuild;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechHead;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The light round the mech's build: the ring's beams, the fizzing edge a part grows behind, the white burst of the
// stomp, the ring of light behind the chest, the clap's star, the shards the head forms from, its meteor streak and
// crater, the lock flash and the glow of the cockpit.
final class MechLight {
    private static final double BEAM = 0.55;
    private static final Vec3 X = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 Z = new Vec3(0.0, 0.0, 1.0);
    private static final int SPIKES = 44;
    private static final int CRACKS = 12;
    private static final int SHARDS = 44;
    private static final Vec3 HALO = new Vec3(0.0, 10.35, -0.9);
    private static final double HALO_RADIUS = 3.0;

    private MechLight() {
    }

    static void lights(LanternPainter painter, MechPose pose, double t, @Nullable Vec3 ring, boolean own) {
        MechScript.Stage stage = pose.stage();
        MechScript.Stage torso = pose.torso();
        boolean walking = t >= MechScript.SETTLED;
        if (t >= MechScript.HEAD_FORM + 3.0) {
            eyes(painter, MechPainter.head(pose, t, -1.0, walking), t);
        }
        if (t >= MechScript.GRIP) {
            console(painter, pose, t, own);
        }
        core(painter, torso, t, own);
        if (t >= MechScript.DONE - 6.0) {
            idle(painter, torso, t, own);
        }
        if (walking) {
            return;
        }
        if (ring != null) {
            calls(painter, pose, t, ring);
        }
        fronts(painter, pose, t);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            double landed = MechScript.landed(right, t);
            Vec3 ground = stage.point(MechScript.ankle(right, stage, t)).subtract(0.0, MechScript.ANKLE.y, 0.0);
            if (MechScript.stomping(right, t)) {
                burst(painter, ground.add(0.0, 0.5, 0.0), landed, right ? 1.0 : 0.6, side * 97);
                ring(painter, ground, landed, right ? 4.0 : 2.8);
            }
        }
        halo(painter, torso, t);
        clap(painter, stage, t);
        head(painter, pose, t);
        crash(painter, stage, t);
        flash(painter, torso.point(MechScript.NECK.add(0.0, 0.3, 0.0)), t - MechScript.LOCK, 2.4);
        flash(painter, pose.hips().point(MechScript.HIP), t - MechScript.THIGHS - MechScript.FORM_TICKS, 1.0);
        flash(painter, pose.hips().point(MechScript.mirror(MechScript.HIP)),
                t - MechScript.THIGHS - MechScript.FORM_TICKS, 1.0);
        MechScript.Stage arms = MechBuild.arms(stage, torso, t);
        for (int side = 0; side < 2; side++) {
            MechMoves.Arm arm = MechMoves.arm(side == 0, stage, t);
            flash(painter, arms.point(arm.elbow()), t - MechScript.ELBOWS, 1.4);
        }
    }

    // The eye slits under the brow.
    private static void eyes(LanternPainter painter, Frame head, double t) {
        double on = Ease.smooth((t - MechScript.HEAD_FORM - 3.0) / 6.0);
        double flash = 1.0 + 1.5 * Math.max(0.0, 1.0 - Math.abs(t - MechScript.LOCK - 2.0) / 5.0);
        double breath = 0.85 + 0.15 * Math.sin(t * 0.21);
        for (int side = -1; side <= 1; side += 2) {
            Vec3 inner = head.at(side * MechHeadShapes.EYE_X[0], MechHeadShapes.EYE_Y - 0.035,
                    MechHeadShapes.EYE_Z);
            Vec3 outer = head.at(side * MechHeadShapes.EYE_X[1], MechHeadShapes.EYE_Y + 0.03,
                    MechHeadShapes.EYE_Z - 0.04);
            double strength = Math.min(1.0, on * breath * flash);
            painter.lightLine(inner, outer, 0.07, LanternPainter.HOT, Colors.alpha(0.95 * strength));
            painter.glowLine(inner, outer, 0.35, LanternPainter.GREEN, Colors.alpha(0.5 * strength));
        }
    }

    // The console's buttons glow; the one being pressed flares.
    private static void console(LanternPainter painter, MechPose pose, double t, boolean own) {
        double on = Ease.smooth((t - MechScript.GRIP) / 6.0);
        for (int k = 0; k < MechScript.BUTTONS.length; k++) {
            Vec3 at = pose.torso().point(MechScript.BUTTONS[k].add(0.0, 0.05, 0.0));
            double blink = 0.55 + 0.45 * Math.sin(t * (0.3 + 0.07 * k) + k * 1.7);
            double pressed = pose.button == k ? pose.press : 0.0;
            painter.flare(at, 0.06 + 0.1 * pressed, on * (own ? 0.5 : 0.8) * Math.max(blink, pressed));
        }
    }

    private static void calls(LanternPainter painter, MechPose pose, double t, Vec3 ring) {
        MechScript.Stage stage = pose.stage();
        MechScript.Stage torso = pose.torso();
        MechScript.Stage arms = MechBuild.arms(stage, torso, t);
        charge(painter, ring, t);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            double form = right ? MechScript.FOOT_FORM : MechScript.FOOT2_FORM;
            Vec3 ankle = pose.ankle[side];
            beam(painter, ring, ankle.add(0.0, MechLegShapes.SHIN * (1.0 - MechScript.grown(t, form)), 0.0), t, form,
                    MechScript.FORM_TICKS);
            Vec3 knee = MechBuilding.knee(pose, right, t);
            Vec3 hip = pose.hips().point(MechPainter.side(MechScript.HIP, right));
            beam(painter, ring, knee.lerp(hip, MechScript.grown(t, MechScript.THIGHS)), t, MechScript.THIGHS,
                    MechScript.FORM_TICKS);
            MechMoves.Arm arm = MechMoves.arm(right, stage, t);
            beam(painter, ring, arms.point(arm.hand()), t, MechScript.ARMS_FORM,
                    MechScript.ARMS_IN - MechScript.ARMS_FORM);
            beam(painter, ring, torso.point(MechPainter.side(new Vec3(2.2 + 1.9 * Mth.clamp((t - MechScript.SHOULDERS)
                    / 12.0, 0.0, 1.0), 10.75, 0.0), right)), t, MechScript.SHOULDERS, 12.0);
            beam(painter, ring, torso.point(MechPainter.side(MechScript.SHOULDER, right)).lerp(arms.point(arm.elbow()),
                    MechMoves.upper(t)), t, MechScript.UPPER_ARMS, MechScript.ELBOWS - MechScript.UPPER_ARMS);
        }
        beam(painter, ring, pose.hips().point(0.0, 6.6 + 1.6 * Mth.clamp((t - MechScript.HIPS) / 6.0, 0.0, 1.0),
                0.6), t, MechScript.HIPS, 6.0);
        beam(painter, ring, torso.point(MechBodyShapes.CORE), t, MechScript.CORE, MechScript.FORM_TICKS);
        beam(painter, ring, torso.point(0.0, 8.1 + 3.2 * Mth.clamp((t - MechScript.ARMOR) / 16.0, 0.0, 1.0), 1.4), t,
                MechScript.ARMOR, 16.0);
        beam(painter, ring, MechHead.pose(stage, t).at(), t, MechScript.HEAD_FORM, 4.0);
    }

    // Before the first foot the ring gathers its light: a glow swelling round it and sparks winding in.
    private static void charge(LanternPainter painter, Vec3 ring, double t) {
        double on = Ease.smooth((t + 1.0) / 4.0) * (1.0 - Ease.smooth((t - MechScript.FOOT_FORM) / 3.0));
        if (on <= 0.0) {
            return;
        }
        double full = Ease.smooth(t / MechScript.FOOT_FORM);
        painter.flare(ring, 0.25 + 0.75 * full, on * (0.6 + 0.4 * Math.sin(t * 1.7)));
        painter.glowDisc(ring, 0.4 + 0.5 * full, LanternPainter.GREEN, 0.35 * on, 0.15, 3);
        for (int k = 0; k < 10; k++) {
            double age = (t * 0.22 + Noise.of(k, 13, 1)) % 1.0;
            Vec3 way = Noise.direction(k, 13);
            double reach = 1.6 * (1.0 - age);
            Vec3 spot = ring.add(Vectors.spin(way, Vectors.UP, 2.5 * age).scale(reach));
            Vec3 tail = ring.add(Vectors.spin(way, Vectors.UP, 2.5 * age - 0.4).scale(reach + 0.3));
            double strength = on * Ease.smooth(age * 4.0);
            painter.lightLine(spot, tail, 0.03, LanternPainter.HOT, Colors.alpha(0.8 * strength));
            painter.glowLine(spot, tail, 0.12, LanternPainter.GREEN, Colors.alpha(0.35 * strength));
        }
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

    // The fizzing, crackling edge each part grows out behind, and the open ends still waiting for their next part.
    private static void fronts(LanternPainter painter, MechPose pose, double t) {
        MechScript.Stage stage = pose.stage();
        MechScript.Stage torso = pose.torso();
        MechScript.Stage frame = MechBuild.arms(stage, torso, t);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            double form = right ? MechScript.FOOT_FORM : MechScript.FOOT2_FORM;
            Vec3 ankle = pose.ankle[side];
            double grown = MechScript.grown(t, form);
            if (t >= form && grown < 1.0) {
                double y = Mth.lerp(Ease.smooth(grown), MechLegShapes.SHIN + 0.1, MechLegShapes.SOLE - 0.05);
                fizz(painter, ankle.add(0.0, y, 0.0), 0.75, t, side * 11 + 1, 1.0);
            }
            Vec3 top = MechBuilding.knee(pose, right, t);
            if (t >= form + MechScript.FORM_TICKS && t < MechScript.THIGHS + 2.0) {
                double fade = 1.0 - Ease.smooth((t - MechScript.THIGHS + 2.0) / 4.0);
                fizz(painter, top, 0.65, t, side * 11 + 2, 0.7 * fade);
                painter.glowDisc(top.add(0.0, 0.25, 0.0), 1.0, LanternPainter.GREEN, 0.35 * fade, 0.3, (int) t);
            }
            double thighs = MechScript.grown(t, MechScript.THIGHS);
            if (t >= MechScript.THIGHS && thighs < 1.0) {
                double y = Mth.lerp(Ease.smooth(thighs), MechScript.KNEE.y - 0.7, MechScript.HIP.y + 0.6);
                Vec3 knee = stage.local(top);
                fizz(painter, stage.point(knee.x, y, knee.z), 0.8, t, side * 11 + 3, 1.0);
            }
            MechMoves.Arm arm = MechMoves.arm(right, frame, t);
            Vec3 elbow = frame.point(arm.elbow());
            Vec3 way = frame.dir(arm.way());
            double arms = Mth.clamp((t - MechScript.ARMS_FORM) / (MechScript.ARMS_IN - MechScript.ARMS_FORM), 0.0, 1.0);
            if (t >= MechScript.ARMS_FORM && arms < 1.0) {
                double along = Mth.lerp(Ease.smooth(arms), MechArmShapes.KNUCKLES + 1.1, -0.6);
                fizz(painter, elbow.add(way.scale(along)), 0.75, t, side * 11 + 4, 1.0);
            }
            if (t >= MechScript.ARMS_IN && t < MechScript.ELBOWS + 2.0) {
                double fade = 1.0 - Ease.smooth((t - MechScript.ELBOWS + 2.0) / 4.0);
                Vec3 end = elbow.subtract(way.scale(0.35));
                fizz(painter, end, 0.55, t, side * 11 + 5, 0.8 * fade);
                painter.glowDisc(end.subtract(way.scale(0.3)), 0.9, LanternPainter.GREEN, 0.4 * fade, 0.35, (int) t + 7);
                painter.glowTaper(end, end.subtract(way.scale(1.4)), 0.9, 0.1, LanternPainter.GREEN, 0.35 * fade, 0.0);
            }
            double upper = MechMoves.upper(t);
            if (upper > 0.0 && upper < 1.0) {
                Vec3 shoulder = torso.point(MechPainter.side(MechScript.SHOULDER, right));
                fizz(painter, shoulder.lerp(elbow, Ease.smooth(upper)), 0.6, t, side * 11 + 6, 1.0);
            }
            double pauldron = Mth.clamp((t - MechScript.SHOULDERS) / 12.0, 0.0, 1.0);
            if (t >= MechScript.SHOULDERS && pauldron < 1.0) {
                double x = 1.8 + 3.7 * Ease.smooth(pauldron);
                fizz(painter, torso.point(MechPainter.side(new Vec3(x, 10.55, 0.0), right)), 1.0, t, side * 11 + 7,
                        1.0);
            }
        }
        double hips = Mth.clamp((t - MechScript.HIPS) / 6.0, 0.0, 1.0);
        if (t >= MechScript.HIPS && hips < 1.0) {
            double y = Mth.lerp(Ease.smooth(hips), 6.35, 8.75);
            fizz(painter, pose.hips().point(0.9, y, 0.0), 0.9, t, 31, 1.0);
            fizz(painter, pose.hips().point(-0.9, y, 0.0), 0.9, t, 32, 1.0);
        }
        double armor = Mth.clamp((t - MechScript.ARMOR) / 16.0, 0.0, 1.0);
        if (t >= MechScript.ARMOR && armor < 1.0) {
            double y = Mth.lerp(Ease.smooth(armor), 7.9, MechBodyShapes.CHEST_TOP + 0.3);
            for (int k = -1; k <= 1; k++) {
                fizz(painter, torso.point(k * 1.4, y, k == 0 ? -1.0 : 0.3), 1.0, t, 33 + k, 1.0);
            }
        }
    }

    // Crackling sparks and short rays round a growing edge.
    private static void fizz(LanternPainter painter, Vec3 at, double radius, double t, int seed, double strength) {
        if (strength <= 0.0) {
            return;
        }
        int tick = (int) t;
        painter.flare(at, 0.45 * radius, 0.55 * strength);
        for (int k = 0; k < 9; k++) {
            double angle = Math.PI * 2.0 * (k + Noise.of(seed, k, tick)) / 9.0;
            double reach = radius * (0.6 + 0.5 * Noise.of(seed, k, tick + 1));
            Vec3 spot = at.add(Math.cos(angle) * reach, (Noise.of(seed, k, tick + 2) - 0.5) * 0.3,
                    Math.sin(angle) * reach);
            double size = 0.08 + 0.14 * Noise.of(seed, k, tick + 3);
            painter.flare(spot, size, strength * (0.5 + 0.5 * Noise.of(seed, k, tick + 4)));
            if (k % 3 == 0) {
                Vec3 ray = spot.add(Noise.direction(seed * 31 + k, tick).scale(0.25 + 0.35 * Noise.of(seed, k,
                        tick + 5)));
                painter.lightLine(spot, ray, 0.035, LanternPainter.HOT, Colors.alpha(0.85 * strength));
            }
        }
    }

    // The clip's white impact star: thin spikes flung out all round the foot as it lands.
    private static void burst(LanternPainter painter, Vec3 at, double since, double size, int seed) {
        if (since < 0.0 || since > 7.0) {
            return;
        }
        double out = Ease.smooth(since / 2.0);
        double fade = 1.0 - Ease.smooth((since - 1.0) / 6.0);
        painter.flare(at, 2.2 * size * (0.5 + 0.5 * out), fade);
        for (int k = 0; k < SPIKES; k++) {
            Vec3 way = Noise.direction(seed + k, 17);
            way = new Vec3(way.x, Math.abs(way.y) * 0.9 + 0.1, way.z).normalize();
            double reach = size * (1.6 + 2.2 * Noise.of(seed + k, 3, 1)) * out;
            Vec3 from = at.add(way.scale(0.4 * size));
            Vec3 to = at.add(way.scale(0.4 * size + reach));
            painter.lightTaper(from, to, 0.09 * size, 0.0, LanternPainter.HOT, 0.95 * fade, 0.0);
            painter.glowTaper(from, to, 0.3 * size, 0.0, LanternPainter.GREEN, 0.4 * fade, 0.0);
        }
    }

    private static void ring(LanternPainter painter, Vec3 ground, double since, double reach) {
        if (since < 0.0 || since > 10.0) {
            return;
        }
        double u = Ease.smooth(since / 8.0);
        double fade = 1.0 - since / 10.0;
        painter.circle(ground.add(0.0, 0.08, 0.0), X, Z, 0.4 + reach * u, 0.08, 0.5, Colors.alpha(0.9 * fade),
                Colors.alpha(0.45 * fade));
    }

    private static void flash(LanternPainter painter, Vec3 at, double since, double size) {
        if (since < 0.0 || since > 6.0) {
            return;
        }
        double flash = 1.0 - since / 6.0;
        painter.flare(at, size * (0.6 + 0.6 * (1.0 - flash)), flash);
    }

    // The pilot's light: a blinding sun in the bubble while the mech takes shape, a steady glow once it is shut in.
    private static void core(LanternPainter painter, MechScript.Stage stage, double t, boolean own) {
        if (t < MechScript.ABOARD - 6.0) {
            return;
        }
        double on = Ease.smooth((t - (MechScript.ABOARD - 6.0)) / 8.0);
        double dim = 1.0 - 0.5 * Ease.smooth((t - MechScript.SPREAD + 5.0) / 10.0);
        double closed = Ease.smooth((t - MechScript.ARMOR - 6.0) / 12.0);
        double pulse = 0.85 + 0.15 * Math.sin(t * 0.45);
        Vec3 heart = stage.point(MechScript.COCKPIT.add(0.0, 1.25, 0.1));
        double strength = on * pulse * Mth.lerp(closed, dim, own ? 0.0 : 0.45);
        painter.flare(heart, Mth.lerp(closed, 1.9, 0.9), strength);
        painter.glowDisc(heart, Mth.lerp(closed, 2.6, 1.2), LanternPainter.GREEN, 0.4 * strength, 0.15, 3);
    }

    // The big ring of the lantern's light standing behind the chest while the mech takes shape, as in the clip.
    private static void halo(LanternPainter painter, MechScript.Stage torso, double t) {
        double on = Ease.smooth((t - MechScript.CORE) / 8.0)
                * (1.0 - Ease.smooth((t - MechScript.HEAD_FORM + 4.0) / 8.0));
        if (on <= 0.0) {
            return;
        }
        double breath = 0.85 + 0.15 * Math.sin(t * 0.3);
        painter.circle(torso.point(HALO), torso.right(), torso.up(), HALO_RADIUS * (0.75 + 0.25 * on), 0.32, 1.5,
                Colors.alpha(0.9 * on * breath), Colors.alpha(0.5 * on * breath));
    }

    private static void clap(LanternPainter painter, MechScript.Stage stage, double t) {
        Vec3 at = stage.point(stage.target().add(0.0, 1.35, 0.0));
        double since = t - MechScript.CLAP;
        if (since >= -1.0 && since <= 10.0) {
            double flash = since < 0.0 ? Ease.smooth(since + 1.0) : 1.0 - Ease.smooth(since / 6.0);
            painter.flare(at, 2.2 * (0.6 + 0.8 * Math.max(0.0, since) / 6.0), flash);
            if (since >= 0.0) {
                double out = Ease.smooth(since / 2.5);
                double fade = 1.0 - Ease.smooth((since - 1.0) / 7.0);
                Vec3 across = stage.right();
                for (int k = 0; k < 28; k++) {
                    double angle = Math.PI * 2.0 * (k + 0.4 * Noise.of(k, 5, 2)) / 28.0;
                    Vec3 way = Vectors.UP.scale(Math.cos(angle)).add(stage.ahead().scale(Math.sin(angle)))
                            .add(across.scale((Noise.of(k, 5, 3) - 0.5) * 0.8)).normalize();
                    double reach = (1.8 + 2.6 * Noise.of(k, 5, 4)) * out;
                    painter.lightTaper(at, at.add(way.scale(reach)), 0.1, 0.0, LanternPainter.HOT, 0.95 * fade, 0.0);
                    painter.glowTaper(at, at.add(way.scale(reach)), 0.35, 0.0, LanternPainter.GREEN, 0.4 * fade, 0.0);
                }
                double u = Ease.smooth(since / 7.0);
                painter.circle(at, Vectors.UP, stage.ahead(), 0.5 + 3.5 * u, 0.1, 0.8, Colors.alpha(0.95 * fade),
                        Colors.alpha(0.5 * fade));
                painter.shell(at, 0.3 + 1.8 * u, painter.material().glow(), 0.35 * fade);
            }
        }
        if (t > MechScript.CLAP && t < MechScript.RELEASE) {
            // The squeeze crackles between the palms.
            int tick = (int) t;
            for (int k = 0; k < 4; k++) {
                Vec3 spot = at.add((Noise.of(k, tick, 9) - 0.5) * 0.5, (Noise.of(k, tick, 10) - 0.5) * 1.4,
                        (Noise.of(k, tick, 11) - 0.5) * 1.2);
                painter.flare(spot, 0.2 + 0.2 * Noise.of(k, tick, 12), 0.7);
            }
        }
    }

    private static void head(LanternPainter painter, MechPose pose, double t) {
        if (t < MechScript.HEAD_FORM - 2.0 || t >= MechScript.LOCK) {
            return;
        }
        MechScript.Stage stage = pose.stage();
        Vec3 at = MechHead.pose(stage, t).at();
        double since = t - MechScript.HEAD_FORM;
        if (since >= -2.0 && since < 7.0) {
            // A cloud of light shards swirling in and closing up into the head, as in the clip.
            double in = Ease.smooth((since + 2.0) / 7.0);
            painter.flare(at, 2.2 * (1.0 - 0.5 * in), 1.0 - Ease.smooth((since - 2.0) / 5.0));
            for (int k = 0; k < SHARDS; k++) {
                Vec3 way = Noise.direction(k, 41);
                double reach = (1.2 + 2.6 * Noise.of(k, 41, 2)) * (1.0 - in);
                double swirl = 2.2 * (1.0 - in) + Noise.of(k, 41, 3) * 0.6;
                Vec3 spot = at.add(Vectors.spin(way, Vectors.UP, swirl).scale(reach + 0.35 * (1.0 - in)));
                Vec3 tail = at.add(Vectors.spin(way, Vectors.UP, swirl + 0.35).scale(reach * 1.15 + 0.4));
                double strength = 1.0 - in * 0.7;
                painter.flare(spot, 0.12 + 0.12 * Noise.of(k, 41, 4), strength);
                painter.lightLine(spot, tail, 0.05, LanternPainter.HOT, Colors.alpha(0.85 * strength));
                painter.glowLine(spot, tail, 0.22, LanternPainter.GREEN, Colors.alpha(0.4 * strength));
            }
        }
        if (t >= MechScript.HEAD_FORM && t < MechScript.HEAD_DROP) {
            painter.glowDisc(at, 1.6, LanternPainter.GREEN, 0.3, 0.2, 5);
        }
        if (t >= MechScript.HEAD_DROP && t < MechScript.CRASH) {
            double u = (t - MechScript.HEAD_DROP) / (MechScript.CRASH - MechScript.HEAD_DROP);
            painter.glowTaper(at, at.add(0.0, 2.5 + 5.0 * u, 0.0), 1.3, 0.2, LanternPainter.GREEN, 0.6, 0.0);
            painter.lightTaper(at, at.add(0.0, 1.5 + 3.5 * u, 0.0), 0.35, 0.05, LanternPainter.HOT, 0.7, 0.0);
        }
        double grabbed = t - MechScript.GRAB;
        if (grabbed >= -1.0 && grabbed < 6.0) {
            // The claw closing on it flashes.
            painter.flare(at, 1.6, grabbed < 0.0 ? Ease.smooth(grabbed + 1.0) : 1.0 - Ease.smooth(grabbed / 6.0));
        }
        if (t >= MechScript.TOSS) {
            // Flung up, it streaks behind itself: short as it slows at the top, long as it falls.
            Vec3 speed = MechHead.pose(stage, t + 0.5).at().subtract(at).scale(2.0);
            double fade = 1.0 - Ease.smooth((t - MechScript.LOCK + 4.0) / 4.0);
            painter.glowTaper(at, at.subtract(speed.scale(3.0)), 1.0, 0.1, LanternPainter.GREEN, 0.45 * fade, 0.0);
            painter.lightTaper(at, at.subtract(speed.scale(2.0)), 0.3, 0.03, LanternPainter.HOT, 0.6 * fade, 0.0);
            double thrown = t - MechScript.TOSS;
            if (thrown < 5.0) {
                painter.flare(at, 2.0, 1.0 - Ease.smooth(thrown / 5.0));
            }
        }
    }

    private static void crash(LanternPainter painter, MechScript.Stage stage, double t) {
        double since = t - MechScript.CRASH;
        if (since < 0.0 || since > 16.0) {
            return;
        }
        Vec3 at = stage.point(stage.target());
        double flash = 1.0 - Ease.smooth(since / 8.0);
        painter.flare(at.add(0.0, 0.5, 0.0), 3.2 * (0.7 + 0.5 * (1.0 - flash)), flash);
        double u = Ease.smooth(since / 10.0);
        double fade = 1.0 - since / 16.0;
        painter.circle(at.add(0.0, 0.08, 0.0), X, Z, 0.6 + 5.5 * u, 0.12, 0.9, Colors.alpha(0.95 * fade),
                Colors.alpha(0.5 * fade));
        painter.circle(at.add(0.0, 0.1, 0.0), X, Z, 0.4 + 3.2 * u, 0.08, 0.6, Colors.alpha(0.8 * fade),
                Colors.alpha(0.4 * fade));
        for (int k = 0; k < CRACKS; k++) {
            double angle = Math.PI * 2.0 * (k + Noise.of(k, 7, 1) * 0.5) / CRACKS;
            double reach = (1.6 + 2.6 * Noise.of(k, 7, 2)) * Ease.smooth(since / 3.0);
            Vec3 last = at.add(0.0, 0.06, 0.0);
            for (int step = 1; step <= 3; step++) {
                double bend = angle + (Noise.of(k, step, 3) - 0.5) * 0.5;
                Vec3 next = at.add(Math.cos(bend) * reach * step / 3.0, 0.06, Math.sin(bend) * reach * step / 3.0);
                painter.lightLine(last, next, 0.07, LanternPainter.HOT, Colors.alpha(0.9 * fade));
                painter.glowLine(last, next, 0.32, LanternPainter.GREEN, Colors.alpha(0.45 * fade));
                last = next;
            }
        }
        if (since < 7.0) {
            burst(painter, at.add(0.0, 0.4, 0.0), since, 1.3, 211);
        }
    }

    // The rim round the glass glows and breathes once the mech stands ready.
    private static void idle(LanternPainter painter, MechScript.Stage stage, double t, boolean own) {
        double on = Ease.smooth((t - (MechScript.DONE - 6.0)) / 6.0);
        double breath = 0.7 + 0.3 * Math.sin(t * 0.12);
        Vec3 port = stage.point(0.0, MechBodyShapes.PORT_Y, MechBodyShapes.GLASS_Z + 0.02);
        painter.circle(port, stage.right(), stage.up(), MechBodyShapes.PORT_IN + 0.02, own ? 0.03 : 0.06,
                own ? 0.15 : 0.45, Colors.alpha((own ? 0.25 : 0.85) * on * breath),
                Colors.alpha(0.4 * on * breath));
    }
}
