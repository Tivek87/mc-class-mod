package nl.tivek.multiversepowers.spell.lightning.client;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.Bolts;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// The lightning: a rune drawing itself on the ground under a gathering storm, the bolt, and the arcs it leaps on in.
public final class StormFx {
    static final double CLOUD = 18.0;
    private static final double RUNE = 2.2;
    private static final int WHITE = 0xF4FBFF;
    private static final int CYAN = 0x48DBFB;
    private static final int DEEP = 0x0984E3;
    private static final int STORM = 0x1E272E;
    private static final int GREY = 0x3A4650;
    private static final Material SPARK = new Material(0x48DBFB, 0xBFF4FF, 0x00D2FF, 0xF4FBFF);
    private static final Bolts.Look LOOK = new Bolts.Look(SPARK, DEEP, STORM);

    private StormFx() {
    }

    public static void charge(ConstructPainter painter, Vec3 at, double age, int ticks, int seed) {
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
            Bolts.jag(painter, LOOK, a, b, 3, 0.15, 0.04, 0.8, seed + flick);
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
            Bolts.jag(painter, LOOK, inside, across, 4, 0.35, 0.06, 0.9, flick);
        }
    }

    public static void bolt(ConstructPainter painter, Vec3 top, Vec3 ground, double age, int seed) {
        Bolts.bolt(painter, LOOK, top, ground, age, seed);
    }

    public static void arc(ConstructPainter painter, Vec3 from, Vec3 to, double age, int seed) {
        Bolts.arc(painter, LOOK, from, to, age, seed);
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
