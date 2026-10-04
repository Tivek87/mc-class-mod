package nl.tivek.multiversepowers.engine.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// Lightning drawn with a painter: a bolt out of the sky, an arc leaping between two points and the crackling line both
// are made of. A power brings its colours as a `Look`.
public final class Bolts {
    // How long a bolt and an arc are drawn, in ticks.
    public static final int BOLT = 32;
    public static final int ARC = 9;
    private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final double LEADER = 1.5;
    private static final int STROKES = 3;
    private static final double STROKE_GAP = 2.0;

    // A stroke's core is the material's hot colour and its glow the material's mass; `halo` is the wide light round a
    // stroke and `scorch` the patch it leaves on the ground.
    public record Look(Material material, int halo, int scorch) {
    }

    private Bolts() {
    }

    // The bolt: first a faint leader feels its way down from the cloud, then the return strokes blaze up it: three
    // of them, each along a new path with side branches, a flash over everything, a dome of plasma, then a scorched
    // patch with glowing cracks that cools.
    public static void bolt(ConstructPainter painter, Look look, Vec3 top, Vec3 ground, double age, int seed) {
        double time = painter.time();
        painter.material(look.material());
        int glow = look.material().mass();
        int core = look.material().hot();
        double since = age - LEADER;
        int stroke = since < 0.0 ? -1 : Math.min(STROKES - 1, (int) (since / STROKE_GAP));
        double flash = 0.0;
        if (stroke >= 0) {
            double inStroke = since - stroke * STROKE_GAP;
            flash = Math.max(0.0, 1.0 - inStroke / 4.0) * (1.0 - 0.25 * stroke);
        }
        if (age < LEADER) {
            double reach = age / LEADER;
            List<Vec3> path = jagged(top, ground, 22, 1.4, seed);
            for (int i = 1; i < path.size(); i++) {
                double u = (double) i / path.size();
                if (u > reach) {
                    break;
                }
                painter.glowTaper(path.get(i - 1), path.get(i), 0.6, 0.6, glow, 0.35, 0.35);
                painter.lightTaper(path.get(i - 1), path.get(i), 0.08, 0.08, core, 0.7, 0.7);
            }
        }
        if (stroke >= 0 && since < STROKES * STROKE_GAP + 6.0) {
            int shape = seed + stroke * 31;
            double inStroke = since - stroke * STROKE_GAP;
            double on = since >= STROKES * STROKE_GAP ? 0.18 * (1.0 - (since - STROKES * STROKE_GAP) / 6.0)
                    : Math.max(0.2, 1.0 - inStroke / STROKE_GAP) * (1.0 - 0.2 * stroke);
            List<Vec3> path = jagged(top, ground, 22, 1.1, shape);
            for (int i = 1; i < path.size(); i++) {
                double u = (double) i / path.size();
                painter.glowTaper(path.get(i - 1), path.get(i), 1.8, 1.8, glow, 0.55 * on, 0.55 * on);
                painter.lightTaper(path.get(i - 1), path.get(i), 0.36, 0.36, core, on, on);
                painter.glowTaper(path.get(i - 1), path.get(i), 4.5, 4.5, look.halo(), 0.14 * on * (1.0 - u * 0.3),
                        0.14 * on);
            }
            for (int b = 0; b < 5; b++) {
                int from = 3 + (int) (Noise.of(shape, b, 2) * (path.size() - 8));
                Vec3 start = path.get(from);
                Vec3 end = start.add(Noise.direction(shape, b + 9).multiply(3.5, 0.0, 3.5)).add(0.0,
                        -2.0 - 3.0 * Noise.of(shape, b, 3), 0.0);
                List<Vec3> side = jagged(start, end, 6, 0.6, shape + b);
                for (int i = 1; i < side.size(); i++) {
                    double fade = on * (1.0 - (double) i / side.size());
                    painter.glowTaper(side.get(i - 1), side.get(i), 0.8, 0.5, glow, 0.45 * fade, 0.3 * fade);
                    painter.lightTaper(side.get(i - 1), side.get(i), 0.14, 0.08, core, 0.8 * fade, 0.5 * fade);
                }
            }
        }
        if (stroke >= 0) {
            if (flash > 0.0) {
                painter.flare(ground.add(0.0, 0.5, 0.0), 5.0 * flash, flash);
                painter.glowDisc(ground.add(0.0, 1.0, 0.0), 8.0 * flash, glow, 0.45 * flash, 0.2, seed + stroke);
                painter.glowDisc(top, 7.0 * flash, glow, 0.35 * flash, 0.3, seed + 1 + stroke);
            }
            double dome = Math.max(0.0, 1.0 - since / 10.0);
            if (dome > 0.0) {
                double size = 0.6 + 1.8 * Ease.smooth(Math.min(1.0, since / 4.0));
                painter.haze(ground.add(0.0, 0.1, 0.0), EAST.scale(size), new Vec3(0.0, size * 0.8, 0.0),
                        SOUTH.scale(size), glow, 0.5 * dome);
            }
        }
        Vec3 floor = ground.add(0.0, 0.05, 0.0);
        if (age < 12.0) {
            double u = age / 12.0;
            painter.circle(floor, EAST, SOUTH, 0.5 + 4.0 * Ease.smooth(u), 0.1, 0.6, Colors.alpha(0.95 * (1.0 - u)),
                    Colors.alpha(0.5 * (1.0 - u)));
        }
        double cool = Math.max(0.0, 1.0 - age / BOLT);
        painter.lightDisc(floor, 1.4, look.scorch(), 0.5 * cool, 0.4, seed + 3);
        for (int k = 0; k < 9; k++) {
            double angle = Math.PI * 2.0 * k / 9.0 + Noise.of(seed, k, 4);
            Vec3 end = floor.add(Math.cos(angle) * (1.0 + 1.3 * Noise.of(seed, k, 5)), 0.0, Math.sin(angle) * (1.0
                    + 1.3 * Noise.of(seed, k, 5)));
            jag(painter, look, floor, end, 4, 0.25, 0.05, cool * cool, seed + k * 3);
        }
        if (cool > 0.3) {
            int flick = (int) (time * 2.0);
            Vec3 a = floor.add((Noise.of(seed, flick, 6) - 0.5) * 2.0, 0.0, (Noise.of(seed, flick, 7) - 0.5) * 2.0);
            jag(painter, look, a, a.add(0.0, 0.5 + Noise.of(seed, flick, 8), 0.0), 3, 0.2, 0.04, cool, flick);
        }
    }

    // The bolt leaping on to the next foe: a bright crackling arc that flickers out.
    public static void arc(ConstructPainter painter, Look look, Vec3 from, Vec3 to, double age, int seed) {
        double on = 1.0 - age / ARC;
        painter.material(look.material());
        int shape = seed + (int) (age * 1.5);
        int parts = Math.max(4, (int) (from.distanceTo(to) * 1.5));
        jag(painter, look, from, to, parts, 0.45, 0.14, on, shape);
        jag(painter, look, from, to, parts, 0.7, 0.05, 0.5 * on, shape + 7);
        if (age < 3.0) {
            painter.flare(to, 0.9 * (1.0 - age / 3.0), 1.0 - age / 3.0);
        }
    }

    // A crackling line from `a` to `b` in `parts` jagged pieces.
    public static void jag(ConstructPainter painter, Look look, Vec3 a, Vec3 b, int parts, double jitter, double width,
            double strength, int seed) {
        if (strength <= 0.01) {
            return;
        }
        int glow = look.material().mass();
        int core = look.material().hot();
        List<Vec3> path = jagged(a, b, parts, jitter, seed);
        for (int i = 1; i < path.size(); i++) {
            painter.glowTaper(path.get(i - 1), path.get(i), width * 6.0, width * 6.0, glow, 0.45 * strength,
                    0.45 * strength);
            painter.lightTaper(path.get(i - 1), path.get(i), width, width, core, 0.9 * strength, 0.9 * strength);
        }
    }

    // The points of a jagged path from `from` to `to`, swinging out most halfway.
    public static List<Vec3> jagged(Vec3 from, Vec3 to, int parts, double jitter, int seed) {
        List<Vec3> points = new ArrayList<>();
        points.add(from);
        for (int i = 1; i < parts; i++) {
            double u = (double) i / parts;
            double swing = jitter * Math.sin(Math.PI * u);
            points.add(from.lerp(to, u).add(Noise.direction(seed, i).scale(swing)));
        }
        points.add(to);
        return points;
    }
}
