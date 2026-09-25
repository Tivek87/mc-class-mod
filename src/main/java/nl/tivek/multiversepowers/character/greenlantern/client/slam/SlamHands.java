package nl.tivek.multiversepowers.character.greenlantern.client.slam;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter.Moment;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;

final class SlamHands {
    private SlamHands() {
    }

    private static final double FIST_WIDTH = 1.23;
    private static final double FIST_REACH = 0.57;

    private static final Shape SKY_ARM = new Shape(new double[][] {
            { -0.35, -0.26, -3.20, 0.35, 0.24, -0.86, 0.95 } },
            Mesh.cone(16, 0.40, 0.58, 0.0, 0.70, 1.1).pointing(0.0, 0.0, -1.0).scaled(1.0, 0.75, 1.0)
                    .moved(0.0, -0.01, -2.30),
            Mesh.torus(16, 6, 0.40, 0.05, 1.3).alongZ().scaled(1.0, 0.75, 1.0).moved(0.0, -0.01, -2.30));
    private static final double[][] FOREARM = {
            { -0.35, -0.26, -4.60, 0.35, 0.24, -0.86, 0.95 },
            { -0.38, -0.29, -2.40, 0.38, 0.27, -2.20, 1.15 } };
    private static final Shape BUMP_ARM = new Shape(new double[][] {
            { -0.35, -0.26, -2.60, 0.35, 0.24, -0.86, 0.95 },
            { -0.38, -0.29, -1.90, 0.38, 0.27, -1.75, 1.15 } });

    private static final Shape PALM = new Shape(new double[][] {
            { -0.12, -0.48, -0.40, 0.12, 0.30, 0.40, 1.0 },
            { -0.15, -0.62, -0.36, 0.14, -0.40, 0.36, 0.95 },
            { -0.13, -0.90, -0.30, 0.13, -0.62, 0.30, 0.9 } },
            Mesh.ball(10, 6, 1.0, 1.0).scaled(0.12, 0.26, 0.16).moved(-0.08, -0.30, 0.28),
            Mesh.ball(10, 6, 1.0, 1.0).scaled(0.08, 0.22, 0.12).moved(-0.08, -0.30, -0.30),
            knuckle(0.30), knuckle(0.10), knuckle(-0.10), knuckle(-0.29),
            Mesh.torus(16, 6, 0.30, 0.06, 1.25).scaled(0.55, 1.0, 1.0).moved(0.0, -0.96, 0.0),
            Mesh.cylinder(12, 0.30, -1.45, -0.96, 0.9).scaled(0.5, 1.0, 1.0));
    private static final double[][] FINGERS = {
            { 0.30, 0.085, 0.36, 0.24, 0.20 },
            { 0.10, 0.088, 0.40, 0.27, 0.21 },
            { -0.10, 0.085, 0.37, 0.25, 0.20 },
            { -0.29, 0.075, 0.29, 0.20, 0.17 } };
    private static final double[] THUMB = { 0.10, 0.26, 0.22, 0.18 };
    private static final Shape[][] BONES = bones();
    private static final Shape HAND_RING = Shape.of(Mesh.torus(12, 5, 0.105, 0.025, 1.25).moved(0.0, 0.16, 0.0),
            Mesh.ball(10, 6, 0.06, 1.6).scaled(0.6, 1.0, 1.0).moved(0.13, 0.16, 0.0));

    private static Mesh knuckle(double z) {
        return Mesh.ball(8, 5, 0.075, 1.1).moved(0.10, 0.30, z);
    }

    private static Shape[][] bones() {
        Shape[][] bones = new Shape[5][3];
        for (int f = 0; f < 5; f++) {
            double r = f < 4 ? FINGERS[f][1] : THUMB[0];
            for (int b = 0; b < 3; b++) {
                double length = f < 4 ? FINGERS[f][2 + b] : THUMB[1 + b];
                double thick = r * (1.0 - 0.06 * b);
                Mesh bone = Mesh.lathe(10, 1.0, 0.0, -0.01, thick * 0.75, -0.01, thick, thick * 0.35, thick,
                        length - thick * 0.35, thick * 0.75, length + 0.01, 0.0, length + 0.01).scaled(0.9, 1.0, 1.0);
                if (b < 2) {
                    bones[f][b] = Shape.of(bone);
                } else {
                    bones[f][b] = new Shape(new double[][] { { thick * 0.35, length - 0.16, -thick * 0.6,
                            thick * 0.9 + 0.012, length - 0.02, thick * 0.6, 1.25 } }, bone);
                }
            }
        }
        return bones;
    }

    private static void hand(LanternPainter painter, Frame frame, Moment m, double[] curl, boolean ring) {
        SlamPainter.piece(painter, PALM, frame, m);
        for (int f = 0; f < 5; f++) {
            Frame joint;
            if (f < 4) {
                joint = frame.moved(0.0, 0.30, FINGERS[f][0]);
            } else {
                joint = frame.moved(-0.04, -0.22, 0.40).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Math.toRadians(40.0))
                        .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Math.toRadians(15.0));
            }
            for (int b = 0; b < 3; b++) {
                // Bending towards the palm, -x, turns about z; the middle joint bends furthest.
                joint = joint.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, curl[f] * (b == 1 ? 1.2 : 0.9));
                SlamPainter.piece(painter, BONES[f][b], joint, m);
                if (ring && f == 1 && b == 0) {
                    SlamPainter.piece(painter, HAND_RING, joint, m);
                }
                double length = f < 4 ? FINGERS[f][2 + b] : THUMB[1 + b];
                joint = joint.moved(0.0, length, 0.0);
            }
        }
    }

    private static double[] bent(double fingers, double thumb) {
        return new double[] { fingers, fingers, fingers, fingers, thumb };
    }

    static Vec3 skyFist(LanternPainter painter, Moment m) {
        double s = m.scale(3.3 / FIST_WIDTH);
        double height = SlamPainter.drop(m) - (m.struck() ? 0.15 * s : 0.0);
        Vec3 towards = m.forward().scale(-1.0);
        Frame frame = new Frame(m.ground().add(0.0, FIST_REACH * s + height, 0.0), SlamPainter.DOWN.cross(towards),
                towards, SlamPainter.DOWN, s);
        SlamPainter.marker(painter, m, 1.5);
        fist(painter, frame, m, true);
        SlamPainter.piece(painter, SKY_ARM, frame, m);
        return frame.at(0.0, 0.0, -3.2);
    }

    private static void fist(LanternPainter painter, Frame frame, Moment m, boolean ring) {
        SlamPainter.piece(painter, LanternPainter.fistModel(), frame, m);
        if (ring) {
            SlamPainter.piece(painter, LanternPainter.FIST_RING, frame, m);
        }
    }

    static Vec3 hands(LanternPainter painter, Moment m) {
        double s = m.scale(1.6);
        Vec3 clap = m.ground().add(0.0, 1.9 * m.size(), 0.0);
        double apart = 0.14 * s + (3.2 + 0.7 * m.windup()) * (1.0 - m.fall()) + SlamPainter.vibrate(m, 0.06);
        Vec3 across = SlamPainter.facingHim(m.right());
        Vec3 back = SlamPainter.facingHim(m.forward().scale(-1.0));
        double bend = m.struck() ? 0.3 * Math.max(0.0, Math.sin(0.8 * m.since())) * Math.exp(-0.25 * m.since())
                : 0.3 * (1.0 - m.fall());
        double[] curl = bent(bend, 0.5 * bend);
        hand(painter, new Frame(clap.add(across.scale(apart)), across, SlamPainter.UP, back, s), m, curl, true);
        hand(painter, new Frame(clap.subtract(across.scale(apart)), across.scale(-1.0), SlamPainter.UP, back, s), m,
                curl, false);
        clapRings(painter, clap, across, m, s);
        return clap;
    }

    static void clapRings(LanternPainter painter, Vec3 at, Vec3 across, Moment m, double s) {
        if (!m.struck()) {
            return;
        }
        Vec3 side = across.cross(SlamPainter.UP).normalize();
        for (int k = 0; k < 2; k++) {
            double age = m.since() - 1.5 * k;
            if (age > 0.0 && age < 7.0) {
                double fade = 1.0 - age / 7.0;
                painter.circle(at, side, SlamPainter.UP, (0.6 + 0.5 * age) * s, 0.07, 0.35,
                        Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
            }
        }
        if (m.since() < 4.0) {
            painter.flare(at, (0.8 + 0.6 * (1.0 - m.since() / 4.0)) * s, 1.0 - m.since() / 4.0);
        }
    }

    static Vec3 fists(LanternPainter painter, Moment m) {
        double s = m.scale(2.4 / FIST_WIDTH);
        Vec3 meet = m.ground().add(0.0, 1.7 * m.size(), 0.0);
        double apart = FIST_REACH * s + (3.0 + 0.7 * m.windup()) * (1.0 - m.fall()) + SlamPainter.vibrate(m, 0.05);
        Vec3 right = SlamPainter.facingHim(m.right());
        Vec3 in = right.scale(-1.0);
        Frame rightFist = new Frame(meet.add(right.scale(apart)), in.cross(SlamPainter.UP), SlamPainter.UP, in, s);
        fist(painter, rightFist, m, true);
        SlamPainter.piece(painter, BUMP_ARM, rightFist, m);
        Frame leftFist = new Frame(meet.subtract(right.scale(apart)), right.cross(SlamPainter.UP).scale(-1.0),
                SlamPainter.UP, right, s);
        fist(painter, leftFist, m, false);
        SlamPainter.piece(painter, BUMP_ARM, leftFist, m);
        clapRings(painter, meet, right, m, s);
        return meet;
    }

    static Vec3 uppercut(LanternPainter painter, Moment m) {
        Vec3 ground = m.ground();
        double s = 3.0 / FIST_WIDTH * m.size();
        double warn = Mth.clamp(m.t() / (LandingSlam.IMPACT_TICK - 1.0), 0.0, 1.0);
        SlamPainter.cracks(painter, ground, (1.2 + 1.6 * warn) * m.size(),
                m.struck() ? 1.0 - m.since() / 12.0 : 0.35 + 0.65 * warn, 7);
        SlamPainter.buildUp(painter, ground, 1.8 * m.size(), m, 7);
        double out = Mth.clamp((m.t() - (LandingSlam.IMPACT_TICK - 0.6)) / 3.5, 0.0, 1.0);
        double rise = 1.0 - Math.pow(1.0 - out, 2.2);
        double back = Ease.smooth((m.t() - LandingSlam.BURST_TICK) / 7.0);
        double knuckles = (Mth.lerp(rise, -0.8, 4.4) - back * 6.0) * m.size();
        Vec3 towards = m.forward().scale(-1.0);
        Frame frame = new Frame(ground.add(0.0, knuckles - FIST_REACH * s, 0.0), SlamPainter.UP.cross(towards),
                towards, SlamPainter.UP, s);
        if (knuckles > -1.5 * m.size()) {
            painter.model(LanternPainter.fistModel(), frame, 1.0, m.bright());
            painter.shape(LanternPainter.FIST_RING, frame, 1.0, m.bright());
            painter.model(FOREARM, frame, 1.0, m.bright());
        }
        SlamPainter.rubble(painter, ground, LandingSlam.IMPACT_TICK - 0.5, m.t(), 12, 0.34, 0.42, 3);
        return out > 0.0 ? frame.at(0.0, 0.0, -1.6) : ground;
    }

    static Vec3 palm(LanternPainter painter, Moment m) {
        double s = m.scale(2.2);
        double height = SlamPainter.drop(m) + 0.16 * s - (m.struck() ? 0.08 * s : 0.0);
        Frame frame = new Frame(m.ground().add(0.0, height, 0.0), SlamPainter.UP, m.forward(),
                m.right().scale(-1.0), s);
        double[] curl = new double[5];
        double since = m.since();
        for (int f = 0; f < 4; f++) {
            curl[f] = !m.struck() ? -0.15 * (1.0 - m.fall())
                    : -0.35 * Math.max(0.0, Math.sin(1.8 * (since - 1.0) - 0.8 * f)) * Math.exp(-0.15 * since)
                            * (since > 1.0 ? 1.0 : 0.0);
        }
        SlamPainter.marker(painter, m, 2.2);
        hand(painter, frame, m, curl, true);
        return frame.at(0.0, -1.2, 0.0);
    }
}
