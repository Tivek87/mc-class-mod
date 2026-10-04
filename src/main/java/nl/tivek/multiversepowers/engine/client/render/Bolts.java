package nl.tivek.multiversepowers.engine.client.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
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
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final int FORKS = 7;
    private static final double LEADER = 1.5;
    private static final int STROKES = 3;
    private static final double STROKE_GAP = 2.0;

    // A stroke's core is the material's hot colour and its glow the material's mass; `halo` is the wide light round a
    // stroke and `scorch` the patch it leaves on the ground.
    public record Look(Material material, int halo, int scorch) {
    }

    private Bolts() {
    }

    public static void bolt(ConstructPainter painter, Look look, Vec3 top, Vec3 ground, double age, int seed) {
        bolt(painter, look, top, ground, age, seed, 1.0, ground);
    }

    // The bolt: first a faint leader feels its way down from the cloud, forking as it goes, then the return strokes
    // blaze up the channel it found: three of them, flickering, each lighting forks of its own off it, and a flash.
    // Where it strikes the `floor` (null when it struck something up in the air) a dome of plasma swells and a scorched
    // patch with glowing cracks is left that cools. `scale` makes all of it thicker and wider.
    public static void bolt(ConstructPainter painter, Look look, Vec3 top, Vec3 ground, double age, int seed,
            double scale, @Nullable Vec3 floor) {
        double time = painter.time();
        painter.material(look.material());
        int glow = look.material().mass();
        int core = look.material().hot();
        double length = top.distanceTo(ground);
        List<Vec3> channel = jagged(top, ground, 32, wander(length), seed);
        double since = age - LEADER;
        int stroke = since < 0.0 ? -1 : Math.min(STROKES - 1, (int) (since / STROKE_GAP));
        double flash = 0.0;
        if (stroke >= 0) {
            double inStroke = since - stroke * STROKE_GAP;
            flash = Math.max(0.0, 1.0 - inStroke / 4.0) * (1.0 - 0.25 * stroke);
        }
        if (age < LEADER) {
            double reach = age / LEADER;
            for (int i = 1; i < channel.size(); i++) {
                if ((double) i / channel.size() > reach) {
                    break;
                }
                painter.glowTaper(channel.get(i - 1), channel.get(i), 0.6 * scale, 0.6 * scale, glow, 0.35, 0.35);
                painter.lightTaper(channel.get(i - 1), channel.get(i), 0.08 * scale, 0.08 * scale, core, 0.7, 0.7);
            }
            forks(painter, look, channel, length, reach, 0.4, 0.5 * scale, seed + 977);
        }
        if (stroke >= 0 && since < STROKES * STROKE_GAP + 6.0) {
            double inStroke = since - stroke * STROKE_GAP;
            double on = since >= STROKES * STROKE_GAP ? 0.18 * (1.0 - (since - STROKES * STROKE_GAP) / 6.0)
                    : Math.max(0.2, 1.0 - inStroke / STROKE_GAP) * (1.0 - 0.2 * stroke);
            // Each stroke flickers: the channel shivers a little, never wholly the same twice.
            List<Vec3> path = shiver(channel, 0.12 * scale, seed + stroke * 31 + (int) (time * 3.0));
            for (int i = 1; i < path.size(); i++) {
                double u = (double) i / path.size();
                painter.glowTaper(path.get(i - 1), path.get(i), 1.6 * scale, 1.6 * scale, glow, 0.6 * on, 0.6 * on);
                painter.lightTaper(path.get(i - 1), path.get(i), 0.3 * scale, 0.3 * scale, core, on, on);
                painter.glowTaper(path.get(i - 1), path.get(i), 5.0 * scale, 5.0 * scale, look.halo(),
                        0.16 * on * (1.0 - u * 0.3), 0.16 * on);
            }
            forks(painter, look, path, length, 1.0, on, scale, seed + stroke * 31);
        }
        if (stroke >= 0) {
            if (flash > 0.0) {
                painter.flare(ground.add(0.0, 0.5, 0.0), 5.0 * flash * scale, flash);
                painter.glowDisc(ground.add(0.0, 1.0, 0.0), 8.0 * flash * scale, glow, 0.45 * flash, 0.2,
                        seed + stroke);
                painter.glowDisc(top, 7.0 * flash * scale, glow, 0.35 * flash, 0.3, seed + 1 + stroke);
            }
            double dome = Math.max(0.0, 1.0 - since / 10.0);
            if (dome > 0.0) {
                double size = (0.6 + 1.8 * Ease.smooth(Math.min(1.0, since / 4.0))) * scale;
                Vec3 at = floor != null ? floor.add(0.0, 0.1, 0.0) : ground;
                double tall = floor != null ? 0.8 : 1.0;
                painter.haze(at, EAST.scale(size), new Vec3(0.0, size * tall, 0.0), SOUTH.scale(size), glow,
                        (floor != null ? 0.5 : 0.35) * dome);
            }
        }
        if (floor == null) {
            return;
        }
        Vec3 low = floor.add(0.0, 0.05, 0.0);
        if (age < 12.0) {
            double u = age / 12.0;
            painter.circle(low, EAST, SOUTH, 0.5 + 4.0 * scale * Ease.smooth(u), 0.1, 0.6,
                    Colors.alpha(0.95 * (1.0 - u)), Colors.alpha(0.5 * (1.0 - u)));
        }
        double cool = Math.max(0.0, 1.0 - age / BOLT);
        painter.lightDisc(low, 1.4 * scale, look.scorch(), 0.5 * cool, 0.4, seed + 3);
        for (int k = 0; k < 9; k++) {
            double angle = Math.PI * 2.0 * k / 9.0 + Noise.of(seed, k, 4);
            double reach = (1.0 + 1.3 * Noise.of(seed, k, 5)) * scale;
            Vec3 end = low.add(Math.cos(angle) * reach, 0.0, Math.sin(angle) * reach);
            jag(painter, look, low, end, 4, 0.25, 0.05, cool * cool, seed + k * 3);
        }
        if (cool > 0.3) {
            int flick = (int) (time * 2.0);
            Vec3 a = low.add((Noise.of(seed, flick, 6) - 0.5) * 2.0, 0.0, (Noise.of(seed, flick, 7) - 0.5) * 2.0);
            jag(painter, look, a, a.add(0.0, 0.5 + Noise.of(seed, flick, 8), 0.0), 3, 0.2, 0.04, cool, flick);
        }
    }

    // How far a bolt `length` blocks long wanders off its straight line.
    private static double wander(double length) {
        return Math.max(0.6, Math.min(3.0, length * 0.06));
    }

    // Forks off the upper part of a channel, down and out from it, each with a twig of its own; only those along the
    // first `reach` of it, as a leader still feeling its way down has no more.
    private static void forks(ConstructPainter painter, Look look, List<Vec3> channel, double length, double reach,
            double on, double scale, int seed) {
        int glow = look.material().mass();
        int core = look.material().hot();
        int n = channel.size();
        Vec3 down = channel.get(n - 1).subtract(channel.get(0)).normalize();
        for (int b = 0; b < FORKS; b++) {
            double where = 0.08 + 0.7 * Noise.of(seed, b, 2);
            if (where > reach) {
                continue;
            }
            Vec3 start = channel.get((int) (where * (n - 1)));
            double angle = Noise.of(seed, b, 4) * Math.PI * 2.0;
            double far = length * (0.08 + 0.12 * Noise.of(seed, b, 3)) * (1.0 - 0.5 * where);
            Vec3 out = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            Vec3 end = start.add(out.scale(far * 0.75)).add(down.scale(far * 0.65));
            List<Vec3> fork = jagged(start, end, 8, far * 0.14, seed + b * 7);
            limb(painter, fork, 0.75 * scale, 0.13 * scale, glow, core, on);
            Vec3 middle = fork.get(fork.size() / 2);
            Vec3 aside = new Vec3(Math.cos(angle + 0.8), 0.0, Math.sin(angle + 0.8));
            Vec3 tip = middle.add(aside.scale(far * 0.35)).add(down.scale(far * 0.3));
            limb(painter, jagged(middle, tip, 4, far * 0.08, seed + b * 7 + 3), 0.45 * scale, 0.08 * scale, glow, core,
                    0.7 * on);
        }
    }

    // A fork thinning and fading towards its tip.
    private static void limb(ConstructPainter painter, List<Vec3> path, double glowWidth, double coreWidth, int glow,
            int core, double on) {
        for (int i = 1; i < path.size(); i++) {
            double a = on * (1.0 - (double) (i - 1) / path.size());
            double b = on * (1.0 - (double) i / path.size());
            double wa = 1.0 - 0.5 * (i - 1) / path.size();
            double wb = 1.0 - 0.5 * i / path.size();
            painter.glowTaper(path.get(i - 1), path.get(i), glowWidth * wa, glowWidth * wb, glow, 0.45 * a, 0.45 * b);
            painter.lightTaper(path.get(i - 1), path.get(i), coreWidth * wa, coreWidth * wb, core, 0.85 * a, 0.85 * b);
        }
    }

    // The same path shaken a little at every point but its ends.
    private static List<Vec3> shiver(List<Vec3> path, double amount, int seed) {
        List<Vec3> shaken = new ArrayList<>(path.size());
        for (int i = 0; i < path.size(); i++) {
            boolean end = i == 0 || i == path.size() - 1;
            shaken.add(end ? path.get(i) : path.get(i).add(Noise.direction(seed, i).scale(amount)));
        }
        return shaken;
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
        if (strength > 0.01) {
            path(painter, look, jagged(a, b, parts, jitter, seed), width, strength);
        }
    }

    // A crackling line along `path`: a hot core `width` wide in a glow six times as wide.
    public static void path(ConstructPainter painter, Look look, List<Vec3> path, double width, double strength) {
        if (strength <= 0.01) {
            return;
        }
        int glow = look.material().mass();
        int core = look.material().hot();
        for (int i = 1; i < path.size(); i++) {
            painter.glowTaper(path.get(i - 1), path.get(i), width * 6.0, width * 6.0, glow, 0.45 * strength,
                    0.45 * strength);
            painter.lightTaper(path.get(i - 1), path.get(i), width, width, core, 0.9 * strength, 0.9 * strength);
        }
    }

    // The points of a jagged path from `from` to `to` in at least `parts` pieces, as lightning forks its way: the
    // middle of each piece pushed aside, level by level, a big bend first and ever smaller kinks on it, so it swings
    // out at most about `jitter` and never at its ends.
    public static List<Vec3> jagged(Vec3 from, Vec3 to, int parts, double jitter, int seed) {
        int levels = 1;
        while ((1 << levels) < parts) {
            levels++;
        }
        Vec3[] points = new Vec3[(1 << levels) + 1];
        points[0] = from;
        points[points.length - 1] = to;
        Vec3 axis = to.subtract(from);
        Vec3 along = axis.lengthSqr() < 1.0E-8 ? UP : axis.normalize();
        Vec3 side = along.cross(Math.abs(along.y) > 0.9 ? EAST : UP).normalize();
        Vec3 other = along.cross(side);
        double swing = jitter * 0.6;
        for (int level = 0; level < levels; level++) {
            int step = (points.length - 1) >> level;
            int half = step / 2;
            for (int i = half; i < points.length - 1; i += step) {
                double a = Noise.of(seed, i, 11 + level * 2) * 2.0 - 1.0;
                double b = Noise.of(seed, i, 12 + level * 2) * 2.0 - 1.0;
                points[i] = points[i - half].lerp(points[i + half], 0.5).add(side.scale(a * swing))
                        .add(other.scale(b * swing));
            }
            swing *= 0.55;
        }
        List<Vec3> path = new ArrayList<>(points.length);
        Collections.addAll(path, points);
        return path;
    }
}
