package nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.HandVictims;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandRift;
import nl.tivek.multiversepowers.engine.client.fx.Cosmos;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// The rift itself, between its two hands: torn lips of light round a window into a sky of stars, shards of hard light
// standing along them, the tentacle that lashes out of it, winds round its creature and pulls it in, flares from deep
// inside as it burns, and the burst of starlight as it slams shut and its shards break apart.
public final class HandRiftLight {
    private static final int POINTS = 14;
    private static final int DEEP = 0x010C07;
    private static final int LIGHT = 0x5CFF7A;
    private static final int WHITE = 0xF2FFF4;
    private static final int BURST = 0xB8FFC8;
    // How far behind the surface the nearest stars lie, in blocks.
    private static final double SKY = 3.0;
    private static final double RAGGED = 0.35;
    private static final int STEPS = 22;
    private static final int COIL = 12;
    private static final int ROUND = 9;
    private static final double THICK = 0.36;
    private static final double WRIST = 0.17;
    private static final double TIP = 0.07;
    private static final double TURNS = 1.4;
    private static final int SPARKS = 24;
    private static final double BLAST_TICKS = 20.0;
    private static final double BREAK_TICKS = 12.0;
    private static final ConstructPainter.Frame WORLD = new ConstructPainter.Frame(Vec3.ZERO, new Vec3(1.0, 0.0, 0.0),
            new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0), 1.0);
    private static final ConstructPainter.Shape SHARD = ConstructPainter.Shape.of(Mesh.cone(4, 0.13, 0.0, 0.0, 0.5,
            1.15));

    private HandRiftLight() {
    }

    // Caught is the creature the tentacle holds, or -1; apart from 0 on the rift breaks up with its hands.
    public static void draw(LanternPainter painter, int id, int variant, Vec3 base, Vec3 facing, double clock,
            int caught, double strength, double apart) {
        HandRift.Frame frame = HandRift.frame(variant, base, facing);
        double gap = HandRift.gap(clock);
        if (gap > 0.01 && apart < 0.0) {
            List<Vec3> upper = lip(frame, id, gap, 1.0);
            List<Vec3> lower = lip(frame, id, gap, -1.0);
            List<Vec3> outline = new ArrayList<>(upper);
            for (int k = lower.size() - 2; k >= 1; k--) {
                outline.add(lower.get(k));
            }
            Cosmos.window(frame.center(), frame.normal(), frame.along(), outline, SKY, DEEP, LIGHT,
                    1.0 + 1.2 * HandRift.burning(clock), id);
            for (List<Vec3> lip : List.of(upper, lower)) {
                for (int k = 0; k + 1 < lip.size(); k++) {
                    painter.lightLine(lip.get(k), lip.get(k + 1), 0.07, WHITE, Colors.alpha(0.95 * strength));
                    painter.glowLine(lip.get(k), lip.get(k + 1), 0.45, LIGHT, Colors.alpha(0.4 * strength));
                }
            }
        }
        shards(painter, frame, id, clock, gap, apart);
        if (apart < 0.0) {
            tentacle(painter, variant, frame, id, clock, held(caught));
        }
        flashes(painter, frame, clock, strength);
        blast(painter, frame, id, clock, strength);
    }

    @Nullable
    private static Entity held(int caught) {
        Minecraft minecraft = Minecraft.getInstance();
        return caught < 0 || minecraft.level == null ? null : minecraft.level.getEntity(caught);
    }

    // One lip of the tear from end to end: furthest from the middle line midway, ragged, closing to its ends; the tear
    // runs out from the middle as it opens and closes back in to it.
    private static List<Vec3> lip(HandRift.Frame frame, int id, double gap, double side) {
        double half = HandRift.HALF * (0.3 + 0.7 * Ease.smooth(gap / (HandRift.WIDE * 0.5)));
        List<Vec3> points = new ArrayList<>();
        for (int k = 0; k <= POINTS; k++) {
            double s = -1.0 + 2.0 * k / POINTS;
            double rag = k == 0 || k == POINTS ? 0.0 : 1.0 + RAGGED * (Noise.of(id, k, side > 0.0 ? 71 : 72) - 0.5);
            points.add(frame.at(s * half, side * gap * Math.pow(1.0 - s * s, 0.65) * rag, 0.0));
        }
        return points;
    }

    // Shards of hard light stand along both lips, leaning out over the rift, grown with it; as it slams shut they
    // break apart.
    private static void shards(LanternPainter painter, HandRift.Frame frame, int id, double clock, double gap,
            double apart) {
        double broken = apart >= 0.0 ? apart : (clock - HandRift.SHUTS) / BREAK_TICKS;
        if (broken >= 1.0) {
            return;
        }
        double grown = broken > 0.0 ? 1.0 : Ease.smooth(gap / (HandRift.WIDE * 0.6));
        if (grown <= 0.01) {
            return;
        }
        for (int s = 0; s < 2; s++) {
            double side = s == 0 ? -1.0 : 1.0;
            List<Vec3> lip = lip(frame, id, gap, side);
            for (int k = 1; k < POINTS; k += 2) {
                double lean = 0.5 + 0.35 * Noise.of(id, k, 81 + s);
                Vec3 up = frame.normal().scale(Math.cos(lean)).add(frame.across().scale(-side * Math.sin(lean)));
                double size = grown * (0.6 + 0.7 * Noise.of(id, k, 83 + s));
                ConstructPainter.Frame shard = ConstructPainter.Frame.of(lip.get(k), frame.along(), up, size);
                if (broken > 0.0) {
                    painter.shattered(SHARD, shard, broken, 1.2, id + 31 * k + s);
                } else {
                    painter.shape(SHARD, shard, 1.0, 1.1);
                }
            }
        }
    }

    // The tentacle lashes up out of the rift at its creature, winds round it, drags it over and pulls it down in; with
    // nothing to hold it slides back in.
    private static void tentacle(LanternPainter painter, int variant, HandRift.Frame frame, int id, double clock,
            @Nullable Entity held) {
        double out = HandRift.lashed(clock);
        double back = held == null ? Ease.smoother((clock - HandRift.SNARES) / 8.0) : 0.0;
        double sunk = held == null ? 0.0 : HandRift.sunk(clock);
        double shown = out * (1.0 - back);
        if (shown <= 0.01 || sunk >= 1.0) {
            return;
        }
        Vec3 normal = frame.normal();
        Vec3 target = HandRift.lashAt(variant, frame);
        double wide = 0.42;
        if (held != null) {
            AABB box = HandVictims.box(held);
            double size = Math.max(box.getYsize(), box.getXsize());
            target = box.getCenter().subtract(normal.scale(sunk * (size + 0.4)));
            wide = Math.max(0.4, box.getXsize()) * 0.5 + 0.12;
        }
        Vec3 toRift = frame.center().subtract(target);
        Vec3 a = toRift.subtract(normal.scale(toRift.dot(normal)));
        a = a.lengthSqr() < 1.0E-6 ? frame.along() : a.normalize();
        Vec3 b = normal.cross(a);
        Vec3 root = frame.at(0.0, 0.0, -1.2);
        Vec3 rise = frame.at(0.0, 0.0, 1.6);
        Vec3 end = target.add(a.scale(wide)).subtract(normal.scale(0.35));
        Vec3 over = end.add(normal.scale(1.2));
        double coil = held == null ? 0.0 : Ease.smooth((clock - HandRift.SNARES) / 4.0);
        int count = STEPS + 1 + (coil > 0.0 ? COIL : 0);
        Vec3[] path = new Vec3[count];
        double[] radius = new double[count];
        double sway = held == null ? 0.25 : 0.1;
        for (int i = 0; i <= STEPS; i++) {
            double u = shown * i / STEPS;
            double wave = sway * Math.sin(u * 9.0 - clock * 0.6) * Math.sin(Math.PI * u);
            path[i] = bezier(root, rise, over, end, u).add(frame.across().scale(wave));
            radius[i] = THICK + (WRIST - THICK) * u + (TIP - WRIST) * Math.max(0.0, u - 0.85) / 0.15 * (1.0 - coil);
        }
        for (int j = 1; coil > 0.0 && j <= COIL; j++) {
            double u = (double) j / COIL;
            double angle = Math.PI * 2.0 * TURNS * coil * u;
            path[STEPS + j] = target.add(a.scale(Math.cos(angle) * wide)).add(b.scale(Math.sin(angle) * wide))
                    .add(normal.scale(-0.35 + 0.6 * u * coil));
            radius[STEPS + j] = WRIST + (TIP - WRIST) * u;
        }
        painter.mesh(Mesh.taper(ROUND, 1.2, frame.across(), radius, path), WORLD, 1.0, 1.1);
        for (int k = 0; k < 2; k++) {
            double run = (clock * 0.07 + 0.5 * k) % 1.0;
            painter.flare(path[(int) Math.round(run * STEPS)], 0.45, 0.6);
        }
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double u) {
        double v = 1.0 - u;
        return a.scale(v * v * v).add(b.scale(3.0 * v * v * u)).add(c.scale(3.0 * v * u * u)).add(d.scale(u * u * u));
    }

    // Light out of the rift: a flare as the fingers dig in and at every jerk, and from deep inside as it burns its
    // creature and hurls it back out.
    private static void flashes(LanternPainter painter, HandRift.Frame frame, double clock, double strength) {
        Vec3 middle = frame.at(0.0, 0.0, 0.3);
        double dug = (clock - HandRift.DIGS) / 8.0;
        if (dug >= 0.0 && dug < 1.0) {
            painter.flare(middle, 2.5 * (1.0 - dug), (1.0 - dug) * strength);
        }
        for (int k = 0; k < HandRift.TEARS.length; k++) {
            double since = (clock - HandRift.TEARS[k]) / 6.0;
            if (since >= 0.0 && since < 1.0) {
                painter.flare(middle, (1.5 + k) * (1.0 - since), 0.8 * (1.0 - since) * strength);
            }
        }
        double burn = HandRift.burning(clock);
        if (burn > 0.0) {
            painter.flare(frame.at(0.0, 0.0, 0.2), 2.0 + 2.0 * burn, burn * strength);
        }
        double hurled = (clock - HandRift.EJECTS) / 10.0;
        if (hurled >= 0.0 && hurled < 1.0) {
            double fade = (1.0 - hurled) * strength;
            painter.flare(middle, 4.0 * (1.0 - hurled), fade);
            painter.circle(middle, frame.along(), frame.across(), 0.5 + 3.0 * Ease.smooth(hurled), 0.08, 0.6,
                    Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
        }
    }

    // The rift slamming shut: a flash and a column of light, rings racing out over the surface and up, and sparks of
    // starlight flung off.
    private static void blast(LanternPainter painter, HandRift.Frame frame, int id, double clock, double strength) {
        double since = clock - HandRift.SHUTS;
        if (since < 0.0 || since > BLAST_TICKS) {
            return;
        }
        double u = since / BLAST_TICKS;
        double fade = strength * (1.0 - u) * (1.0 - u);
        Vec3 middle = frame.at(0.0, 0.0, 0.4);
        Vec3 normal = frame.normal();
        if (since < 5.0) {
            double flash = 1.0 - since / 5.0;
            painter.flare(middle, 8.0 * flash, strength);
            painter.glowDisc(middle, 4.5 * flash, BURST, 0.8 * strength, 0.3, id);
            painter.beamOfLight(middle, middle.add(normal.scale(9.0)), flash * strength, clock, 0.9);
        }
        double radius = 0.5 + 7.0 * (1.0 - (1.0 - u) * (1.0 - u));
        Vec3 low = frame.at(0.0, 0.0, 0.12);
        painter.circle(low, frame.along(), frame.across(), radius, 0.12, 0.9, Colors.alpha(0.9 * fade),
                Colors.alpha(0.45 * fade));
        painter.circle(low, frame.along(), frame.across(), radius * 0.7, 0.08, 0.7, Colors.alpha(0.7 * fade),
                Colors.alpha(0.35 * fade));
        painter.circle(middle, frame.along(), normal, radius * 0.8, 0.1, 0.8, Colors.alpha(0.8 * fade),
                Colors.alpha(0.4 * fade));
        painter.circle(middle, frame.across(), normal, radius * 0.8, 0.1, 0.8, Colors.alpha(0.8 * fade),
                Colors.alpha(0.4 * fade));
        for (int k = 0; k < SPARKS; k++) {
            Vec3 way = Noise.direction(id + 7 * k, 91);
            if (way.dot(normal) < 0.0) {
                way = way.subtract(normal.scale(2.0 * way.dot(normal)));
            }
            double far = radius * (0.5 + 0.6 * Noise.of(id, k, 92));
            Vec3 head = middle.add(way.scale(far));
            painter.edge(middle.add(way.scale(far * 0.6)), head, 0.08, fade);
            painter.flare(head, 0.5, fade);
        }
    }
}
