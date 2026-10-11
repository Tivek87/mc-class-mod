package nl.tivek.multiversepowers.character.greenlantern.client.minion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.mesh.Surface;
import nl.tivek.multiversepowers.engine.math.Colors;

// The cracks a helper shows as it is worn down: jagged fissures run over the tiles of its parts, each opening once it
// has lost so much of its health and growing from where it starts, the first on its chest and head, until it is
// crazed all over. Where the worst run its light leaks out and flickers. Every helper has its own, the same in
// every game.
final class MinionCracks {
    enum Part {
        PELVIS,
        TRUNK,
        HEAD,
        THIGH_RIGHT,
        THIGH_LEFT,
        SHIN_RIGHT,
        SHIN_LEFT,
        UPPER_RIGHT,
        UPPER_LEFT,
        FOREARM_RIGHT,
        FOREARM_LEFT
    }

    // One crack: the part it runs over, its points in that part's places, the share of health lost when it opens,
    // and whether light leaks out where it starts once it is wide open.
    private record Crack(Part part, Vec3[] points, double opens, boolean leaks) {
    }

    private static final int COUNT = 64;
    // A crack takes this share of health lost to run its whole length.
    private static final double GROWS = 0.12;
    // Above the tiles, and above a plate laid over them.
    private static final double OVER = 0.05;
    private static final double OVER_PLATE = 0.14;
    private static final double LIFT = 0.14;
    // The light inside showing through: nearly white. (A dark groove does not read on the dark tile faces.)
    private static final int HOT = 0xF0FFF2;
    private static final Part[] FIRST = { Part.TRUNK, Part.HEAD, Part.TRUNK, Part.PELVIS, Part.TRUNK };
    private static final Part[] ANY = { Part.TRUNK, Part.TRUNK, Part.TRUNK, Part.HEAD, Part.HEAD, Part.PELVIS,
            Part.PELVIS, Part.THIGH_RIGHT, Part.THIGH_LEFT, Part.SHIN_RIGHT, Part.SHIN_LEFT, Part.UPPER_RIGHT,
            Part.UPPER_LEFT, Part.FOREARM_RIGHT, Part.FOREARM_LEFT };
    private static final Map<Integer, List<Crack>> MADE = new HashMap<>();

    private MinionCracks() {
    }

    private static List<Crack> of(int seed) {
        if (MADE.size() > 64) {
            MADE.clear();
        }
        return MADE.computeIfAbsent(seed, MinionCracks::make);
    }

    private static List<Crack> make(int seed) {
        Random random = new Random(seed * 7919L + 13L);
        List<Crack> cracks = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            Part part = i < FIRST.length ? FIRST[i] : ANY[random.nextInt(ANY.length)];
            double opens = 0.06 + 0.86 * Math.pow((double) i / COUNT, 1.15);
            double u = random.nextDouble();
            double v = 0.2 + 0.6 * random.nextDouble();
            double angle = random.nextDouble() * Math.PI * 2.0;
            List<Vec3> points = new ArrayList<>();
            int steps = 4 + random.nextInt(4);
            Surface skin = skin(part);
            for (int k = 0; k <= steps; k++) {
                points.add(on(part, skin, u, v));
                angle += random.nextGaussian() * 0.7;
                u += 0.035 * Math.cos(angle);
                v = Mth.clamp(v + 0.07 * Math.sin(angle), 0.1, 0.9);
                if (k == steps / 2 && random.nextDouble() < 0.45) {
                    cracks.add(branch(random, part, skin, u, v, angle, opens + 0.03));
                }
            }
            cracks.add(new Crack(part, points.toArray(Vec3[]::new), opens, i % 3 == 0));
        }
        return cracks;
    }

    // A shorter crack off the side of another, opening a little after it.
    private static Crack branch(Random random, Part part, Surface skin, double u, double v, double angle,
            double opens) {
        double turn = angle + (random.nextBoolean() ? 1.0 : -1.0) * (0.8 + random.nextDouble() * 0.6);
        List<Vec3> points = new ArrayList<>();
        int length = 3 + random.nextInt(2);
        for (int k = 0; k < length; k++) {
            points.add(on(part, skin, u, v));
            turn += random.nextGaussian() * 0.6;
            u += 0.028 * Math.cos(turn);
            v = Mth.clamp(v + 0.05 * Math.sin(turn), 0.1, 0.9);
        }
        return new Crack(part, points.toArray(Vec3[]::new), opens, false);
    }

    private static Surface skin(Part part) {
        return switch (part) {
            case PELVIS -> MinionShapes.PELVIS_SKIN;
            case TRUNK -> MinionShapes.CHEST_SKIN;
            case HEAD -> MinionShapes.HEAD_SKIN;
            case THIGH_RIGHT, THIGH_LEFT -> MinionShapes.THIGH_SKIN;
            case SHIN_RIGHT, SHIN_LEFT -> MinionShapes.SHIN_SKIN;
            case UPPER_RIGHT, UPPER_LEFT -> MinionShapes.UPPER_SKIN;
            case FOREARM_RIGHT, FOREARM_LEFT -> MinionShapes.FOREARM_SKIN;
        };
    }

    // Where a crack runs at (u, v): over the tiles, or over the plate there if there is one.
    private static Vec3 on(Part part, Surface skin, double u, double v) {
        double turn = u - Math.floor(u);
        double over = OVER;
        for (double[] plate : plates(part)) {
            double from = plate[0] - Math.floor(plate[0]);
            double width = plate[1] - plate[0];
            double into = turn - from - Math.floor(turn - from);
            if (into <= width && v >= plate[2] && v <= plate[3]) {
                over = OVER_PLATE;
            }
        }
        return mirror(part, skin.at(turn, v).add(skin.outward(turn, v, true).scale(over)));
    }

    private static double[][] plates(Part part) {
        return switch (part) {
            case TRUNK -> MinionShapes.CHEST_PLATES;
            case FOREARM_RIGHT, FOREARM_LEFT -> MinionShapes.FOREARM_PLATES;
            case THIGH_RIGHT, THIGH_LEFT -> MinionShapes.THIGH_PLATES;
            case SHIN_RIGHT, SHIN_LEFT -> MinionShapes.SHIN_PLATES;
            default -> new double[0][];
        };
    }

    // The left limbs are the right ones mirrored.
    private static Vec3 mirror(Part part, Vec3 at) {
        boolean left = part == Part.THIGH_LEFT || part == Part.SHIN_LEFT || part == Part.UPPER_LEFT
                || part == Part.FOREARM_LEFT;
        return left ? new Vec3(-at.x, at.y, at.z) : at;
    }

    // Every crack open at `lost` (0 whole to 1 broken), each part drawn in its frame (indexed by `Part`).
    static void draw(LanternPainter painter, int seed, double lost, Frame[] frames) {
        if (lost <= 0.0) {
            return;
        }
        double time = painter.time();
        Vec3 eye = painter.camera();
        for (Crack crack : of(seed)) {
            if (lost <= crack.opens()) {
                continue;
            }
            Frame on = frames[crack.part().ordinal()];
            double grown = Math.min(1.0, (lost - crack.opens()) / GROWS);
            double reach = grown * (crack.points().length - 1);
            double width = 0.04 + 0.03 * grown;
            double pulse = 0.85 + 0.15 * Math.sin(time * 0.7 + crack.opens() * 25.0);
            Vec3 last = seen(on, crack.points()[0], eye);
            for (int k = 1; k < crack.points().length && k - 1 < reach; k++) {
                Vec3 next = seen(on, crack.points()[k], eye);
                if (last != null && next != null) {
                    double part = Math.min(1.0, reach - (k - 1));
                    Vec3 end = last.add(next.subtract(last).scale(part));
                    double thin = width * (1.0 - 0.4 * k / crack.points().length);
                    painter.glowLine(last, end, thin * 4.0, LanternPainter.GREEN,
                            Colors.alpha(0.55 * grown * pulse));
                    painter.lightLine(last, end, thin, HOT, Colors.alpha(0.95 * pulse));
                }
                last = next;
            }
            Vec3 start = seen(on, crack.points()[0], eye);
            if (crack.leaks() && lost > crack.opens() + 0.3 && start != null) {
                double flicker = 0.6 + 0.4 * Math.sin(time * 0.9 + crack.opens() * 40.0);
                painter.flare(start, 0.08 + 0.06 * flicker, 0.55 * flicker);
            }
        }
    }

    // A crack's point drawn moved towards the eye along its own line of sight: on screen it stays put, but it lies in
    // front of the tiles and plates around it. Null on the far side of its part, which it would show through.
    private static Vec3 seen(Frame on, Vec3 point, Vec3 eye) {
        Vec3 at = on.at(point.x, point.y, point.z);
        Vec3 toEye = eye.subtract(at);
        if (at.subtract(on.at(0.0, point.y, 0.0)).dot(toEye) <= 0.0) {
            return null;
        }
        return at.add(toEye.normalize().scale(LIFT));
    }
}
