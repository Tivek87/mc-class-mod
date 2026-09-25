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

final class SlamRisers {
    private SlamRisers() {
    }

    private static final Shape SPIKE = Shape.of(
            Mesh.lathe(6, 1.0, 0.0, 0.0, 0.36, 0.0, 0.30, 1.90, 0.12, 2.70, 0.0, 3.0),
            Mesh.lathe(6, 1.05, 0.0, 0.0, 0.16, 0.0, 0.13, 0.80, 0.05, 1.15, 0.0, 1.30).turned(0.0, 0.0, 1.0, -25.0)
                    .moved(0.26, 0.0, 0.08),
            Mesh.lathe(6, 1.05, 0.0, 0.0, 0.14, 0.0, 0.11, 0.65, 0.04, 0.95, 0.0, 1.05).turned(1.0, 0.0, 0.0, 28.0)
                    .moved(-0.20, 0.0, -0.14));

    static Vec3 spikes(LanternPainter painter, Moment m) {
        Vec3 ground = m.ground();
        double warn = Mth.clamp(m.t() / (LandingSlam.IMPACT_TICK - 1.0), 0.0, 1.0);
        SlamPainter.cracks(painter, ground, (1.0 + 2.6 * warn) * m.size(),
                m.struck() ? 1.0 - m.since() / 12.0 : 0.35 + 0.65 * warn, 11);
        SlamPainter.buildUp(painter, ground, 3.2 * m.size(), m, 11);
        int[] counts = { 7, 11 };
        double[] radii = { 1.5, 3.2 };
        double[] sizes = { 1.3, 1.0 };
        double back = Ease.smooth((m.t() - LandingSlam.BURST_TICK) / 7.0);
        for (int ring = 0; ring < counts.length; ring++) {
            double rise = Mth.clamp((m.t() - (LandingSlam.IMPACT_TICK - 1.0 + ring * 1.5)) / 2.5, 0.0, 1.0);
            double up = SlamPainter.backOut(rise) - back;
            if (up <= 0.02) {
                continue;
            }
            double size = sizes[ring] * m.size();
            for (int i = 0; i < counts[ring]; i++) {
                double angle = Math.PI * 2.0 * (i + 0.5 * ring) / counts[ring];
                Vec3 out = m.right().scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
                double sway = 0.08 * Math.sin(1.7 * m.t() + i) * Math.max(0.0, 1.0 - Math.max(0.0, m.since()) / 10.0);
                Vec3 lean = SlamPainter.UP.add(out.scale(0.4 + sway)).normalize();
                Vec3 along = out.cross(SlamPainter.UP);
                Vec3 base = ground.add(out.scale(radii[ring] * m.size())).add(lean.scale((up - 1.0) * 3.2 * size));
                Frame frame = new Frame(base, along.cross(lean), lean, along, size);
                painter.shape(SPIKE, frame, 1.0, m.bright());
                if (rise >= 1.0 && back <= 0.0) {
                    double glint = Math.sin(0.9 * m.t() - i * 0.9 - ring);
                    if (glint > 0.8) {
                        painter.flare(frame.at(0.0, 3.0, 0.0), 0.25 * size, (glint - 0.8) * 5.0);
                    }
                }
            }
        }
        SlamPainter.rubble(painter, ground, LandingSlam.IMPACT_TICK - 0.5, m.t(), 10, 0.3 * m.size(), 0.35, 11);
        return ground.add(0.0, 1.5 * m.size(), 0.0);
    }

    private static final double PILLAR_SCALE = 1.2;
    private static final double PILLAR_HEIGHT = 4.45;
    private static final double[] DRUMS = { 0.40, 1.60, 2.80, 4.00 };

    private static final Shape PILLAR_BASE = new Shape(new double[][] {
            { -0.80, 0.00, -0.80, 0.80, 0.25, 0.80, 1.1 } },
            Mesh.torus(24, 6, 0.62, 0.10, 1.15).moved(0.0, 0.33, 0.0),
            Mesh.cylinder(20, 0.60, 0.25, 0.42, 1.0));
    private static final Shape[] PILLAR_DRUMS = { drum(0), drum(1), drum(2) };
    private static final Shape CAPITAL = new Shape(new double[][] {
            { -0.88, 4.22, -0.88, 0.88, 4.45, 0.88, 1.1 } },
            Mesh.lathe(20, 1.05, 0.0, 4.00, 0.60, 4.00, 0.70, 4.08, 0.80, 4.16, 0.84, 4.22, 0.0, 4.22));

    private static Shape drum(int k) {
        double from = DRUMS[k];
        double to = DRUMS[k + 1];
        Mesh[] meshes = new Mesh[2 + 12];
        meshes[0] = Mesh.cylinder(16, 0.58, from, to, 1.0);
        meshes[1] = Mesh.torus(16, 4, 0.585, 0.03, 1.3).moved(0.0, from + 0.02, 0.0);
        for (int i = 0; i < 12; i++) {
            meshes[2 + i] = Mesh.box(-0.035, from + 0.06, -0.62, 0.035, to - 0.06, -0.55, 1.2)
                    .turned(0.0, 1.0, 0.0, 30.0 * i);
        }
        return Shape.of(meshes);
    }

    static Vec3 pillar(LanternPainter painter, Moment m) {
        double s = PILLAR_SCALE * m.size();
        double height = PILLAR_HEIGHT * s;
        Vec3 base = m.ground().subtract(m.forward().scale(height * 0.5));
        double rise = Mth.clamp((m.t() - 2.0) / 4.0, 0.0, 1.0);
        double up = 1.0 - (1.0 - rise) * (1.0 - rise);
        double tip = Mth.clamp((m.t() - 6.0) / (LandingSlam.IMPACT_TICK - 6.0), 0.0, 1.0);
        double angle = Mth.HALF_PI * tip * tip;
        Vec3 along = SlamPainter.UP.scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
        Vec3 ahead = m.forward().scale(Math.cos(angle)).subtract(SlamPainter.UP.scale(Math.sin(angle)));
        double crack = m.t() < 12.0 ? Math.min(1.0, m.t() / 4.0) : 1.0 - (m.t() - 12.0) / 8.0;
        SlamPainter.cracks(painter, base, (1.2 + 1.2 * rise) * m.size(), crack, 23);
        SlamPainter.rubble(painter, base, 2.5, m.t(), 10, 0.32 * m.size(), 0.35, 23);
        double since = m.since();
        double bounce = m.struck() ? 0.35 * s * Math.abs(Math.sin(1.4 * since)) * Math.exp(-0.6 * since) : 0.0;
        Frame frame = new Frame(base.subtract(along.scale((1.0 - up) * height)).add(0.0, bounce, 0.0), m.right(),
                along, ahead, s);
        if (up > 0.02) {
            double apart = m.struck() ? Ease.smooth(since / 4.0) : 0.0;
            SlamPainter.piece(painter, PILLAR_BASE, frame, m);
            for (int k = 0; k < PILLAR_DRUMS.length; k++) {
                Frame drum = frame.moved(0.0, 0.12 * (k + 1) * apart, 0.0).turned(0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                        ((k & 1) == 0 ? 0.35 : -0.35) * apart);
                SlamPainter.piece(painter, PILLAR_DRUMS[k], drum, m);
            }
            SlamPainter.piece(painter, CAPITAL, frame.moved(0.0, 0.5 * apart, 0.0), m);
        }
        return frame.at(0.0, PILLAR_HEIGHT * 0.5, 0.0);
    }

    private static final double EMBLEM_HALF = 1.4;

    private static final Shape EMBLEM = new Shape(new double[][] {
            { -0.85, 1.17, 0.12, 0.85, 1.35, 0.14, 1.3 },
            { -0.85, -1.35, 0.12, 0.85, -1.17, 0.14, 1.3 } },
            Mesh.ring(40, 1.05, 0.70, -0.12, 0.96, -0.12, 1.0, -0.08, 1.0, 0.08, 0.96, 0.12, 0.70, 0.12, 0.66, 0.08,
                    0.66, -0.08).alongZ(),
            bar(1.26), bar(-1.26));

    private static Mesh bar(double y) {
        return Mesh.prism(-0.12, 0.12, 1.1, -0.95, y - 0.10, -0.88, y - 0.14, 0.88, y - 0.14, 0.95, y - 0.10, 0.95,
                y + 0.10, 0.88, y + 0.14, -0.88, y + 0.14, -0.95, y + 0.10);
    }

    static Vec3 emblem(LanternPainter painter, Moment m) {
        double s = LandingSlam.EMBLEM_HALF / EMBLEM_HALF * m.scale(1.0);
        double half = EMBLEM_HALF * s;
        Vec3 edge = m.ground().subtract(m.forward().scale(half));
        double since = m.since();
        double hop = m.struck() ? 0.12 * Math.abs(Math.sin(1.6 * since)) * Math.exp(-0.45 * since) : 0.0;
        double angle = Mth.HALF_PI * (m.fall() - hop);
        Vec3 up = SlamPainter.UP.scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
        Vec3 face = m.forward().scale(-Math.cos(angle)).add(SlamPainter.UP.scale(Math.sin(angle)));
        Vec3 middle = edge.add(up.scale(half));
        SlamPainter.piece(painter, EMBLEM, new Frame(middle, m.right(), up, face, s), m);
        if (m.struck()) {
            for (int k = 0; k < 2; k++) {
                double age = since - 2.0 * k;
                if (age > 0.0 && age < 7.0) {
                    double fade = 1.0 - age / 7.0;
                    painter.circle(middle.add(0.0, 0.05, 0.0), m.right(), m.forward(), (0.9 + 0.5 * age) * s, 0.06,
                            0.3, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
                }
            }
        }
        return middle;
    }
}
