package nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.Abyssed;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.HandVictims;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandAbyss;
import nl.tivek.multiversepowers.engine.client.fx.Cosmos;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The abyss between its two hands: cracks of light round the creature as the ground tears, the pit's rim of shards,
// veins of light down its walls, its floor a window on a sky of stars, the tentacles of hard light writhing up out of
// it and winding round what they drag down, and the bursts as the ground gives way and as it slams shut.
public final class HandAbyssLight {
    private static final int DEEP = 0x010C07;
    private static final int LIGHT = 0x5CFF7A;
    private static final int WHITE = 0xF2FFF4;
    private static final int BURST = 0xB8FFC8;
    private static final double SKY = 6.0;
    private static final int RIM = 28;
    private static final int CRACKS = 14;
    private static final int SHARDS = 16;
    private static final int VEINS = 14;
    private static final int STEPS = 18;
    private static final int COIL = 12;
    private static final int ROUND = 9;
    private static final double THICK = 0.38;
    private static final double TIP = 0.06;
    private static final double BURST_TICKS = 18.0;
    private static final double BREAK_TICKS = 12.0;
    private static final ConstructPainter.Frame WORLD = new ConstructPainter.Frame(Vec3.ZERO, new Vec3(1.0, 0.0, 0.0),
            new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0), 1.0);
    private static final ConstructPainter.Shape SHARD = ConstructPainter.Shape.of(Mesh.cone(4, 0.16, 0.0, 0.0, 0.7,
            1.15));

    private HandAbyssLight() {
    }

    public static void draw(LanternPainter painter, int id, int variant, Vec3 base, Vec3 facing, double clock,
            double strength, double apart) {
        Vec3 along = Vectors.flat(facing);
        Vec3 across = Vectors.UP.cross(along).normalize();
        Vec3 ground = base.add(0.0, 0.06, 0.0);
        Vec3 floor = HandAbyss.floor(variant, base).add(0.0, 0.06, 0.0);
        boolean deep = HandAbyss.depth(variant) > 0;
        double torn = HandAbyss.torn(clock);
        if (!HandAbyss.open(clock)) {
            cracks(painter, id, ground, along, across, torn, clock, strength);
        }
        double window = HandAbyss.window(clock);
        if (window > 0.01 && apart < 0.0) {
            List<Vec3> outline = new ArrayList<>();
            double radius = (HandAbyss.RADIUS - (deep ? 0.3 : 0.0)) * window;
            for (int k = 0; k < RIM; k++) {
                double turn = Math.PI * 2.0 * k / RIM;
                double ragged = 1.0 - 0.08 * Noise.of(id, k, (int) (clock * 0.25));
                outline.add(ring(floor, along, across, radius * ragged, turn));
            }
            Cosmos.window(floor, Vectors.UP, along, outline, SKY, DEEP, LIGHT, 1.0 + 0.6 * window, id);
            painter.circle(floor, along, across, radius, 0.06, 0.6, Colors.alpha(0.9 * strength),
                    Colors.alpha(0.45 * strength));
            if (deep) {
                walls(painter, id, ground, floor, along, across, window, clock, strength);
            }
        }
        if (HandAbyss.open(clock) || apart >= 0.0) {
            painter.circle(ground, along, across, HandAbyss.RADIUS + 0.1, 0.08, 0.8, Colors.alpha(0.95 * strength),
                    Colors.alpha(0.45 * strength));
        }
        shards(painter, id, ground, along, across, clock, apart);
        if (apart < 0.0) {
            tentacles(painter, id, variant, base, floor, along, across, clock);
        }
        bursts(painter, id, ground, along, across, clock, strength);
    }

    private static Vec3 ring(Vec3 middle, Vec3 along, Vec3 across, double radius, double turn) {
        return middle.add(along.scale(Math.cos(turn) * radius)).add(across.scale(Math.sin(turn) * radius));
    }

    // A ring of light where the ground will give way, and jagged cracks running out of it, longer at every jerk.
    private static void cracks(LanternPainter painter, int id, Vec3 ground, Vec3 along, Vec3 across, double torn,
            double clock, double strength) {
        if (torn <= 0.01) {
            return;
        }
        double glow = Math.min(1.0, 0.4 + torn) * strength;
        painter.circle(ground, along, across, HandAbyss.RADIUS, 0.04 + 0.06 * torn, 0.6, Colors.alpha(glow),
                Colors.alpha(0.5 * glow));
        for (int k = 0; k < CRACKS; k++) {
            double turn = Math.PI * 2.0 * (k + 0.5 * Noise.of(id, k, 21)) / CRACKS;
            double inner = HandAbyss.RADIUS * (1.0 - 0.7 * torn * (0.5 + 0.5 * Noise.of(id, k, 22)));
            double outer = HandAbyss.RADIUS * (1.0 + 0.3 * torn * Noise.of(id, k, 23));
            Vec3 from = ring(ground, along, across, inner, turn);
            for (int s = 1; s <= 4; s++) {
                double bend = turn + (Noise.of(id, k * 5 + s, 24) - 0.5) * 0.25;
                Vec3 to = ring(ground, along, across, inner + (outer - inner) * s / 4.0, bend);
                painter.edge(from, to, 0.06, glow * (0.6 + 0.4 * Math.sin(clock * 0.4 + k)));
                from = to;
            }
        }
    }

    // Veins of light down the pit's walls from rim to floor, light running down them, and rings round the walls.
    private static void walls(LanternPainter painter, int id, Vec3 ground, Vec3 floor, Vec3 along, Vec3 across,
            double window, double clock, double strength) {
        double wall = HandAbyss.RADIUS - 0.55;
        double fade = window * strength;
        for (int k = 0; k < VEINS; k++) {
            double turn = Math.PI * 2.0 * (k + 0.3 * Noise.of(id, k, 31)) / VEINS;
            Vec3 top = ring(ground, along, across, wall, turn);
            Vec3 bottom = ring(floor, along, across, wall, turn + 0.15 * (Noise.of(id, k, 32) - 0.5));
            painter.lightLine(top, bottom, 0.05, WHITE, Colors.alpha(0.55 * fade));
            painter.glowLine(top, bottom, 0.45, LIGHT, Colors.alpha(0.25 * fade));
            double run = (clock * 0.04 + Noise.of(id, k, 33)) % 1.0;
            painter.flare(top.lerp(bottom, run), 0.5, 0.7 * fade);
        }
        double depth = ground.y - floor.y;
        for (int k = 1; k <= 2; k++) {
            Vec3 at = ground.subtract(0.0, depth * k / 3.0, 0.0);
            painter.circle(at, along, across, wall, 0.04, 0.4, Colors.alpha(0.5 * fade), Colors.alpha(0.25 * fade));
        }
    }

    // Shards of hard light standing round the rim, leaning out over the ground, grown as it gives way; as the pit
    // slams shut they break apart.
    private static void shards(LanternPainter painter, int id, Vec3 ground, Vec3 along, Vec3 across, double clock,
            double apart) {
        double broken = apart >= 0.0 ? apart : (clock - HandAbyss.SHUTS) / BREAK_TICKS;
        if (broken >= 1.0) {
            return;
        }
        double grown = broken > 0.0 ? 1.0 : Ease.smooth((clock - HandAbyss.OPENS + 2.0) / 6.0);
        if (grown <= 0.01) {
            return;
        }
        for (int k = 0; k < SHARDS; k++) {
            double turn = Math.PI * 2.0 * (k + 0.4 * Noise.of(id, k, 41)) / SHARDS;
            Vec3 out = along.scale(Math.cos(turn)).add(across.scale(Math.sin(turn)));
            double lean = 0.35 + 0.4 * Noise.of(id, k, 42);
            Vec3 up = Vectors.UP.scale(Math.cos(lean)).add(out.scale(Math.sin(lean)));
            Vec3 at = ring(ground, along, across, HandAbyss.RADIUS + 0.2, turn).subtract(0.0, 0.1, 0.0);
            double size = grown * (0.7 + 0.8 * Noise.of(id, k, 43));
            ConstructPainter.Frame shard = ConstructPainter.Frame.of(at, out.cross(Vectors.UP).normalize(), up, size);
            if (broken > 0.0) {
                painter.shattered(SHARD, shard, broken, 1.2, id + 31 * k);
            } else {
                painter.shape(SHARD, shard, 1.0, 1.1);
            }
        }
    }

    // Tentacles writhe up out of the floor, and one winds round each creature the pit holds and drags it down; they
    // sink back into the stars before the pit shuts.
    private static void tentacles(LanternPainter painter, int id, int variant, Vec3 base, Vec3 floor, Vec3 along,
            Vec3 across, double clock) {
        double risen = HandAbyss.risen(clock);
        if (risen <= 0.01) {
            return;
        }
        double high = base.y - floor.y + 2.2;
        for (int k = 0; k < HandAbyss.TENTACLES; k++) {
            double turn = Math.PI * 2.0 * (k + 0.5 * Noise.of(id, k, 51)) / HandAbyss.TENTACLES;
            double far = HandAbyss.RADIUS * (0.3 + 0.45 * Noise.of(id, k, 52));
            Vec3 root = ring(floor, along, across, far, turn).subtract(0.0, 0.5, 0.0);
            Vec3 out = along.scale(Math.cos(turn)).add(across.scale(Math.sin(turn)));
            Vec3 side = Vectors.UP.cross(out).normalize();
            double tall = high * risen * (0.6 + 0.5 * Noise.of(id, k, 53));
            double phase = Noise.of(id, k, 54) * 6.0;
            Vec3[] path = new Vec3[STEPS + 1];
            double[] radius = new double[STEPS + 1];
            for (int i = 0; i <= STEPS; i++) {
                double u = (double) i / STEPS;
                double sway = 0.7 * Math.sin(u * 4.0 - clock * 0.16 + phase) * u;
                double swing = 0.5 * Math.cos(u * 3.0 - clock * 0.11 + phase) * u;
                double curl = -0.9 * u * u * u;
                path[i] = root.add(0.0, tall * u, 0.0).add(side.scale(sway)).add(out.scale(swing + curl));
                radius[i] = THICK + (TIP - THICK) * u;
            }
            painter.mesh(Mesh.taper(ROUND, 1.2, side, radius, path), WORLD, 1.0, 1.1);
        }
        List<Entity> held = Abyssed.heldBy(id);
        for (int k = 0; k < held.size(); k++) {
            AABB box = HandVictims.box(held.get(k));
            Vec3 middle = box.getCenter();
            double wide = Math.max(0.4, box.getXsize()) * 0.5 + 0.12;
            Vec3 under = new Vec3(middle.x, floor.y, middle.z);
            Vec3 toMiddle = under.subtract(floor.x, floor.y, floor.z);
            double far = toMiddle.length();
            if (far > HandAbyss.RADIUS * 0.8) {
                under = floor.add(toMiddle.scale(HandAbyss.RADIUS * 0.8 / far));
            }
            Vec3 root = under.subtract(0.0, 0.5, 0.0);
            Vec3 a = Vectors.flat(middle.subtract(root));
            Vec3 b = Vectors.UP.cross(a).normalize();
            Vec3 end = middle.add(a.scale(wide)).subtract(0.0, 0.35, 0.0);
            double coil = Ease.smooth(risen);
            Vec3[] path = new Vec3[STEPS + 1 + COIL];
            double[] radius = new double[STEPS + 1 + COIL];
            for (int i = 0; i <= STEPS; i++) {
                double u = (double) i / STEPS;
                double wave = 0.15 * Math.sin(u * 8.0 - clock * 0.5 + k) * Math.sin(Math.PI * u);
                path[i] = bezier(root, root.add(0.0, 1.8, 0.0), end.add(0.0, 1.0, 0.0), end, u).add(b.scale(wave));
                radius[i] = THICK + (0.18 - THICK) * u;
            }
            for (int j = 1; j <= COIL; j++) {
                double u = (double) j / COIL;
                double angle = Math.PI * 2.0 * 1.6 * coil * u;
                path[STEPS + j] = middle.add(a.scale(Math.cos(angle) * wide)).add(b.scale(Math.sin(angle) * wide))
                        .add(0.0, -0.35 + 0.9 * u, 0.0);
                radius[STEPS + j] = 0.18 + (TIP - 0.18) * u;
            }
            painter.mesh(Mesh.taper(ROUND, 1.2, b, radius, path), WORLD, 1.0, 1.1);
            painter.flare(path[(int) Math.round((clock * 0.08 + 0.3 * k) % 1.0 * STEPS)], 0.45, 0.6);
        }
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double u) {
        double v = 1.0 - u;
        return a.scale(v * v * v).add(b.scale(3.0 * v * v * u)).add(c.scale(3.0 * v * u * u)).add(d.scale(u * u * u));
    }

    // A flare as the fingers dig in and at every jerk, a burst of starlight with a ring of shock as the ground gives
    // way, and another as the hands slam it shut.
    private static void bursts(LanternPainter painter, int id, Vec3 ground, Vec3 along, Vec3 across, double clock,
            double strength) {
        double dug = (clock - HandAbyss.DIGS) / 8.0;
        if (dug >= 0.0 && dug < 1.0) {
            painter.flare(ground.add(0.0, 0.3, 0.0), 2.0 * (1.0 - dug), (1.0 - dug) * strength);
        }
        for (int jerk : HandAbyss.TEARS) {
            double since = (clock - jerk) / 6.0;
            if (since >= 0.0 && since < 1.0) {
                painter.flare(ground.add(0.0, 0.3, 0.0), 2.5 * (1.0 - since), (1.0 - since) * strength);
            }
        }
        for (int at : new int[] { HandAbyss.OPENS, HandAbyss.SHUTS }) {
            double since = clock - at;
            if (since < 0.0 || since >= BURST_TICKS) {
                continue;
            }
            double u = since / BURST_TICKS;
            double fade = strength * (1.0 - u) * (1.0 - u);
            if (since < 4.0) {
                painter.flare(ground.add(0.0, 0.5, 0.0), 6.0 * (1.0 - since / 4.0), strength);
                painter.glowDisc(ground.add(0.0, 0.2, 0.0), 4.0 * (1.0 - since / 4.0), BURST, 0.7 * strength, 0.25,
                        id);
            }
            double radius = HandAbyss.RADIUS + 7.0 * (1.0 - (1.0 - u) * (1.0 - u));
            painter.circle(ground.add(0.0, 0.1, 0.0), along, across, radius, 0.1, 0.9, Colors.alpha(0.9 * fade),
                    Colors.alpha(0.45 * fade));
            painter.circle(ground.add(0.0, 0.3, 0.0), along, across, radius * 0.75, 0.07, 0.6,
                    Colors.alpha(0.6 * fade), Colors.alpha(0.3 * fade));
        }
    }
}
