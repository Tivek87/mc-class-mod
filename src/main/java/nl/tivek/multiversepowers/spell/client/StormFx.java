package nl.tivek.multiversepowers.spell.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// The lightning: a rune drawing itself on the ground under a gathering storm, the bolt, and the arcs it leaps on in.
final class StormFx {
    static final int BOLT = 32;
    static final int ARC = 9;
    static final int CLAP = 26;
    static final double CLAP_REACH = 9.0;
    private static final double CLAP_SPEED = 1.1;
    static final double CLOUD = 18.0;
    private static final double RUNE = 2.2;
    private static final int WHITE = 0xF4FBFF;
    private static final int CYAN = 0x48DBFB;
    private static final int DEEP = 0x0984E3;
    private static final int STORM = 0x1E272E;
    private static final int GREY = 0x3A4650;
    private static final Material SPARK = new Material(0x48DBFB, 0xBFF4FF, 0x00D2FF, 0xF4FBFF);
    private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final double LEADER = 1.5;
    private static final int STROKES = 3;
    private static final double STROKE_GAP = 2.0;

    private StormFx() {
    }

    static void charge(ConstructPainter painter, Vec3 at, double age, int ticks, int seed) {
        double p = Math.min(1.0, age / Math.max(1, ticks));
        double time = painter.time();
        painter.material(SPARK);
        Vec3 ground = at.add(0.0, 0.06, 0.0);
        // The rune draws itself: rings growing round, then spokes and glyphs between them.
        double drawn = Ease.smooth(p * 1.6);
        double spin = time * 0.08;
        for (int r = 0; r < 3; r++) {
            double radius = RUNE * (1.0 - 0.3 * r);
            arcOnGround(painter, ground, radius, spin * (r % 2 == 0 ? 1.0 : -1.3), drawn, 0.06, 0.4, 0.5 + 0.5 * p);
        }
        for (int k = 0; k < 8; k++) {
            double angle = spin + Math.PI * 2.0 * k / 8.0;
            double on = Ease.smooth((p - 0.2 - 0.05 * k) * 4.0);
            if (on <= 0.0) {
                continue;
            }
            Vec3 in = ground.add(Math.cos(angle) * RUNE * 0.4, 0.0, Math.sin(angle) * RUNE * 0.4);
            Vec3 out = ground.add(Math.cos(angle) * RUNE * (0.4 + 0.6 * on), 0.0, Math.sin(angle) * RUNE * (0.4 + 0.6
                    * on));
            painter.edge(in, out, 0.05, 0.8 * on);
            Vec3 tip = ground.add(Math.cos(angle + 0.12) * RUNE * 0.85, 0.0, Math.sin(angle + 0.12) * RUNE * 0.85);
            painter.edge(out.lerp(in, 0.3), tip, 0.035, 0.6 * on);
        }
        painter.glowDisc(ground.add(0.0, 0.1, 0.0), RUNE * 1.2, CYAN, 0.12 + 0.2 * p, 0.2, seed);
        // Static crawling over the rune.
        for (int k = 0; k < 3 + (int) (p * 5); k++) {
            int flick = (int) (time * 3.0) + k * 13;
            double angle = Noise.of(seed, flick, 1) * Math.PI * 2.0;
            double reach = Math.sqrt(Noise.of(seed, flick, 2)) * RUNE;
            Vec3 a = ground.add(Math.cos(angle) * reach, 0.05, Math.sin(angle) * reach);
            Vec3 b = a.add(Noise.direction(seed, flick).multiply(0.6, 0.4, 0.6).add(0.0, 0.3, 0.0));
            jag(painter, a, b, 3, 0.15, 0.04, 0.8, seed + flick);
        }
        // A thin thread of light joins the ground to the cloud just before it breaks.
        double thread = Ease.smooth((p - 0.7) / 0.3);
        if (thread > 0.0) {
            Vec3 sky = at.add(0.0, CLOUD, 0.0);
            painter.glowTaper(ground, sky, 0.5 * thread, 0.9 * thread, CYAN, 0.35 * thread, 0.15 * thread);
            painter.lightTaper(ground, sky, 0.06, 0.1, WHITE, 0.5 * thread, 0.2 * thread);
        }
        cloud(painter, at, 1.2 + 3.0 * p, 0.35 + 0.4 * p, seed, age);
    }

    // A dark cloud of rolling puffs, lit from inside by flickers of lightning.
    private static void cloud(ConstructPainter painter, Vec3 at, double size, double strength, int seed, double age) {
        Vec3 sky = at.add(0.0, CLOUD, 0.0);
        double time = painter.time();
        for (int k = 0; k < 16; k++) {
            double angle = Noise.of(seed, k, 3) * Math.PI * 2.0 + time * 0.01 * (k % 2 == 0 ? 1 : -1);
            double reach = Math.sqrt(Noise.of(seed, k, 4)) * size;
            Vec3 puff = sky.add(Math.cos(angle) * reach, (Noise.of(seed, k, 5) - 0.5) * 1.2, Math.sin(angle) * reach);
            painter.lightDisc(puff, 1.4 + 1.2 * Noise.of(seed, k, 6), k % 3 == 0 ? GREY : STORM, strength, 0.35,
                    seed + k + (int) (time * 0.2));
        }
        int flick = (int) (time * 1.5);
        if (Noise.of(seed, flick, 7) < 0.35) {
            Vec3 inside = sky.add((Noise.of(seed, flick, 8) - 0.5) * size, -0.3, (Noise.of(seed, flick, 9) - 0.5)
                    * size);
            painter.glowDisc(inside, 1.5 + size * 0.4, CYAN, 0.35, 0.3, flick);
            Vec3 across = inside.add(Noise.direction(seed, flick).multiply(2.0, 0.2, 2.0));
            jag(painter, inside, across, 4, 0.35, 0.06, 0.9, flick);
        }
    }

    // The bolt: first a faint leader feels its way down from the cloud, then the return strokes blaze up it: three
    // of them, each along a new path with side branches, a flash over everything, a dome of plasma, then a scorched
    // patch with glowing cracks that cools.
    static void bolt(ConstructPainter painter, Vec3 top, Vec3 ground, double age, int seed) {
        double time = painter.time();
        painter.material(SPARK);
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
                painter.glowTaper(path.get(i - 1), path.get(i), 0.6, 0.6, CYAN, 0.35, 0.35);
                painter.lightTaper(path.get(i - 1), path.get(i), 0.08, 0.08, WHITE, 0.7, 0.7);
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
                painter.glowTaper(path.get(i - 1), path.get(i), 1.8, 1.8, CYAN, 0.55 * on, 0.55 * on);
                painter.lightTaper(path.get(i - 1), path.get(i), 0.36, 0.36, WHITE, on, on);
                painter.glowTaper(path.get(i - 1), path.get(i), 4.5, 4.5, DEEP, 0.14 * on * (1.0 - u * 0.3),
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
                    painter.glowTaper(side.get(i - 1), side.get(i), 0.8, 0.5, CYAN, 0.45 * fade, 0.3 * fade);
                    painter.lightTaper(side.get(i - 1), side.get(i), 0.14, 0.08, WHITE, 0.8 * fade, 0.5 * fade);
                }
            }
        }
        if (stroke >= 0) {
            if (flash > 0.0) {
                painter.flare(ground.add(0.0, 0.5, 0.0), 5.0 * flash, flash);
                painter.glowDisc(ground.add(0.0, 1.0, 0.0), 8.0 * flash, CYAN, 0.45 * flash, 0.2, seed + stroke);
                painter.glowDisc(top, 7.0 * flash, CYAN, 0.35 * flash, 0.3, seed + 1 + stroke);
            }
            double dome = Math.max(0.0, 1.0 - since / 10.0);
            if (dome > 0.0) {
                double size = 0.6 + 1.8 * Ease.smooth(Math.min(1.0, since / 4.0));
                painter.haze(ground.add(0.0, 0.1, 0.0), EAST.scale(size), new Vec3(0.0, size * 0.8, 0.0),
                        SOUTH.scale(size), CYAN, 0.5 * dome);
            }
        }
        Vec3 floor = ground.add(0.0, 0.05, 0.0);
        if (age < 12.0) {
            double u = age / 12.0;
            painter.circle(floor, EAST, SOUTH, 0.5 + 4.0 * Ease.smooth(u), 0.1, 0.6, Colors.alpha(0.95 * (1.0 - u)),
                    Colors.alpha(0.5 * (1.0 - u)));
        }
        double cool = Math.max(0.0, 1.0 - age / BOLT);
        painter.lightDisc(floor, 1.4, STORM, 0.5 * cool, 0.4, seed + 3);
        for (int k = 0; k < 9; k++) {
            double angle = Math.PI * 2.0 * k / 9.0 + Noise.of(seed, k, 4);
            Vec3 end = floor.add(Math.cos(angle) * (1.0 + 1.3 * Noise.of(seed, k, 5)), 0.0, Math.sin(angle) * (1.0
                    + 1.3 * Noise.of(seed, k, 5)));
            jag(painter, floor, end, 4, 0.25, 0.05, cool * cool, seed + k * 3);
        }
        if (cool > 0.3) {
            int flick = (int) (time * 2.0);
            Vec3 a = floor.add((Noise.of(seed, flick, 6) - 0.5) * 2.0, 0.0, (Noise.of(seed, flick, 7) - 0.5) * 2.0);
            jag(painter, a, a.add(0.0, 0.5 + Noise.of(seed, flick, 8), 0.0), 3, 0.2, 0.04, cool, flick);
        }
    }

    // The bolt leaping on to the next foe: a bright crackling arc that flickers out.
    static void arc(ConstructPainter painter, Vec3 from, Vec3 to, double age, int seed) {
        double on = 1.0 - age / ARC;
        painter.material(SPARK);
        int shape = seed + (int) (age * 1.5);
        int parts = Math.max(4, (int) (from.distanceTo(to) * 1.5));
        jag(painter, from, to, parts, 0.45, 0.14, on, shape);
        jag(painter, from, to, parts, 0.7, 0.05, 0.5 * on, shape + 7);
        if (age < 3.0) {
            painter.flare(to, 0.9 * (1.0 - age / 3.0), 1.0 - age / 3.0);
        }
    }

    // The thunder clap: a flash between the hands, arcs leaping out of them, and a crackling ring rolling over the
    // ground round the caster, as far and as fast as the hits in ThunderClapSpell.
    static void clap(ConstructPainter painter, Vec3 hands, Vec3 feet, double age, int seed) {
        double time = painter.time();
        painter.material(SPARK);
        if (age < 5.0) {
            double flash = 1.0 - age / 5.0;
            painter.flare(hands, 2.2 * flash, flash);
            painter.glowDisc(hands, 3.0 * flash, CYAN, 0.5 * flash, 0.2, seed);
        }
        if (age < 7.0) {
            double on = 1.0 - age / 7.0;
            int shape = seed + (int) (age * 2.0);
            for (int k = 0; k < 8; k++) {
                Vec3 end = hands.add(Noise.direction(seed, k).multiply(2.6, 1.4, 2.6));
                jag(painter, hands, end, 5, 0.35, 0.05, on, shape + k * 5);
            }
        }
        double rolled = CLAP_REACH / CLAP_SPEED;
        double ring = age < rolled ? 1.0 : 1.0 - (age - rolled) / 6.0;
        if (ring <= 0.0) {
            return;
        }
        Vec3 floor = feet.add(0.0, 0.08, 0.0);
        double radius = Math.min(CLAP_REACH, (age + 1.0) * CLAP_SPEED);
        painter.circle(floor, EAST, SOUTH, radius, 0.12, 0.7, Colors.alpha(0.95 * ring), Colors.alpha(0.5 * ring));
        arcOnGround(painter, floor, Math.max(0.2, radius - 0.5), time * 0.1, 1.0, 0.05, 0.35, 0.6 * ring);
        painter.glowDisc(floor.add(0.0, 0.1, 0.0), radius, CYAN, 0.12 * ring, 0.2, seed);
        int flick = (int) (time * 2.0);
        double inner = Math.max(0.3, radius - 1.6);
        for (int k = 0; k < 6; k++) {
            double angle = Noise.of(seed, flick + k * 7, 1) * Math.PI * 2.0;
            Vec3 a = floor.add(Math.cos(angle) * inner, 0.0, Math.sin(angle) * inner);
            Vec3 b = floor.add(Math.cos(angle) * radius, 0.35, Math.sin(angle) * radius);
            jag(painter, a, b, 3, 0.25, 0.05, ring, seed + flick + k);
        }
    }

    private static void jag(ConstructPainter painter, Vec3 a, Vec3 b, int parts, double jitter, double width,
            double strength, int seed) {
        if (strength <= 0.01) {
            return;
        }
        List<Vec3> path = jagged(a, b, parts, jitter, seed);
        for (int i = 1; i < path.size(); i++) {
            painter.glowTaper(path.get(i - 1), path.get(i), width * 6.0, width * 6.0, CYAN, 0.45 * strength,
                    0.45 * strength);
            painter.lightTaper(path.get(i - 1), path.get(i), width, width, WHITE, 0.9 * strength, 0.9 * strength);
        }
    }

    private static List<Vec3> jagged(Vec3 from, Vec3 to, int parts, double jitter, int seed) {
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

    private static void arcOnGround(ConstructPainter painter, Vec3 center, double radius, double from, double share,
            double width, double glow, double strength) {
        int segments = 40;
        int shown = (int) Math.ceil(segments * share);
        Vec3 last = null;
        for (int i = 0; i <= shown; i++) {
            double angle = from + Math.PI * 2.0 * i / segments;
            Vec3 next = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            if (last != null) {
                painter.lightLine(last, next, width, WHITE, Colors.alpha(0.9 * strength));
                painter.glowLine(last, next, glow, CYAN, Colors.alpha(0.45 * strength));
            }
            last = next;
        }
    }
}
