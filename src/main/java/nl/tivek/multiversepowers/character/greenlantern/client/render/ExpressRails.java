package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ExpressScript;
import nl.tivek.multiversepowers.character.greenlantern.client.ExpressTrails;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

final class ExpressRails {
    private static final double STEP = 0.62;
    private static final double HALF = STEP / ExpressScript.SCALE * 0.5 + 0.02;
    private static final double AHEAD = 0.9;
    private static final double FORMING = 2.4;
    private static final double CRUMBLE = 3.2;
    private static final double BEYOND_DERAIL = 1.0;
    private static final ConstructPainter.Shape SEGMENT = new ConstructPainter.Shape(segment());

    private ExpressRails() {
    }

    private static double[][] segment() {
        double r = ExpressShapes.RAIL;
        return new double[][] {
                { r - 0.06, -0.08, -HALF, r + 0.06, 0.0, HALF, 1.25 },
                { r - 0.02, -0.14, -HALF, r + 0.02, -0.08, HALF, 1.0 },
                { r - 0.09, -0.16, -HALF, r + 0.09, -0.14, HALF, 1.1 },
                { -r - 0.06, -0.08, -HALF, -r + 0.06, 0.0, HALF, 1.25 },
                { -r - 0.02, -0.14, -HALF, -r + 0.02, -0.08, HALF, 1.0 },
                { -r - 0.09, -0.16, -HALF, -r + 0.09, -0.14, HALF, 1.1 },
                { -1.12, -0.24, -0.11, 1.12, -0.16, 0.11, 0.85 },
                { r - 0.13, -0.17, -0.1, r + 0.13, -0.15, 0.1, 1.2 },
                { -r - 0.13, -0.17, -0.1, -r + 0.13, -0.15, 0.1, 1.2 } };
    }

    static void draw(LanternPainter painter, ExpressTrails.Trail trail, double o, double s, double age,
            double trainApart) {
        double edge = o + AHEAD * s;
        boolean derailed = !Double.isNaN(trail.derailedAt());
        if (derailed) {
            edge = Math.min(edge, trail.derailedAt() + BEYOND_DERAIL * s);
        }
        double tail = o - ExpressScript.LENGTH * s - 0.6 * s;
        int first = Mth.floor((tail - CRUMBLE * s) / STEP);
        int last = Mth.floor(edge / STEP) - 1;
        for (int k = first; k <= last; k++) {
            double from = k * STEP;
            double to = from + STEP;
            double middle = (from + to) * 0.5;
            Vec3 a = trail.at(from);
            Vec3 b = trail.at(to);
            Vec3 way = b.subtract(a);
            if (way.lengthSqr() < 1.0E-8) {
                continue;
            }
            Frame frame = Frame.of(a.add(b).scale(0.5), way, Vectors.UP, s);
            double apart = Math.max(trainApart, (tail - middle) / (CRUMBLE * s));
            double fresh = derailed ? 0.0 : 1.0 - Mth.clamp((edge - middle) / (FORMING * s), 0.0, 1.0);
            double bright = 1.0 + 1.3 * fresh * fresh;
            if (apart > 0.0) {
                if (apart < 1.0) {
                    painter.shattered(SEGMENT, frame, apart, bright, k);
                }
            } else {
                painter.shape(SEGMENT, frame, 1.0, bright);
            }
        }
        if (!derailed && trainApart < 0.0) {
            front(painter, trail, edge, s, age);
        }
    }

    private static void front(LanternPainter painter, ExpressTrails.Trail trail, double edge, double s, double age) {
        Vec3 at = trail.at(edge);
        Vec3 way = at.subtract(trail.at(edge - 0.5));
        if (way.lengthSqr() < 1.0E-8) {
            return;
        }
        Frame frame = Frame.of(at, way, Vectors.UP, s);
        double flicker = 0.8 + 0.2 * Math.sin(age * 2.3);
        Vec3 left = frame.at(-ExpressShapes.RAIL, -0.04, 0.0);
        Vec3 right = frame.at(ExpressShapes.RAIL, -0.04, 0.0);
        painter.flare(left, 0.28 * s * flicker, 0.85);
        painter.flare(right, 0.28 * s * flicker, 0.85);
        painter.edge(frame.at(-1.12, -0.2, -0.05), frame.at(1.12, -0.2, -0.05), 0.05 * s, 0.9 * flicker);
        double grow = Ease.smooth(Mth.frac(age * 0.35));
        painter.glowLine(left, left.add(frame.forward().scale(0.8 * s * grow)), 0.3 * s, LanternBeams.GREEN,
                (int) (140 * (1.0 - grow)));
        painter.glowLine(right, right.add(frame.forward().scale(0.8 * s * grow)), 0.3 * s, LanternBeams.GREEN,
                (int) (140 * (1.0 - grow)));
    }
}
