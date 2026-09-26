package nl.tivek.multiversepowers.spell.client;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The void: a sphere of nothing that sucks the light in and bursts, the way back out, and the rip of an ambush.
final class VoidFx {
    static final int IN = 30;
    static final int OUT = 16;
    static final int STRIKE = 14;
    private static final int ABYSS = 0x07000E;
    private static final int VIOLET = 0x9B5CFF;
    private static final int PALE = 0xD9B8FF;
    private static final int DEEP = 0x4A1A99;
    private static final Material RIFT = new Material(0x9B5CFF, 0xD9B8FF, 0x7A3CFF, 0xF2E6FF);
    private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final double PULL = 6.0;

    private VoidFx() {
    }

    static void enter(ConstructPainter painter, Vec3 feet, double age, int seed) {
        Vec3 core = feet.add(0.0, 1.0, 0.0);
        painter.material(RIFT);
        if (age < PULL) {
            // Light is drawn in: streaks of violet fall toward a darkening heart.
            double u = age / PULL;
            for (int k = 0; k < 18; k++) {
                Vec3 out = Noise.direction(seed, k);
                double far = 3.2 * (1.0 - u) + 0.4;
                Vec3 a = core.add(out.scale(far));
                Vec3 b = core.add(out.scale(far * 0.55));
                painter.lightTaper(a, b, 0.02, 0.12, PALE, 0.2 * u, 0.9 * u);
                painter.glowTaper(a, b, 0.1, 0.4, VIOLET, 0.1 * u, 0.4 * u);
            }
            sphere(painter, core, 0.35 + 0.9 * u, 0.9 * u, seed);
            return;
        }
        double t = age - PULL;
        if (t < 4.0) {
            painter.flare(core, 3.0 * (1.0 - t / 4.0), 1.0 - t / 4.0);
        }
        double grow = Ease.backOut(Math.min(1.0, t / 5.0));
        double fade = 1.0 - Ease.smooth((t - 6.0) / (IN - PULL - 6.0));
        sphere(painter, core, 1.25 * grow + 0.2, fade, seed);
        if (t < 12.0) {
            double u = t / 12.0;
            painter.circle(feet.add(0.0, 0.06, 0.0), EAST, SOUTH, 0.5 + 5.5 * Ease.smooth(u), 0.14, 0.8,
                    Colors.alpha(0.9 * (1.0 - u)), Colors.alpha(0.5 * (1.0 - u)));
            painter.circle(core, EAST, SOUTH, 0.3 + 3.5 * Ease.smooth(u), 0.06, 0.4, Colors.alpha(0.7 * (1.0 - u)),
                    Colors.alpha(0.3 * (1.0 - u)));
        }
        shards(painter, core, t, seed, 18);
    }

    static void leave(ConstructPainter painter, Vec3 feet, double age, int seed) {
        Vec3 core = feet.add(0.0, 1.0, 0.0);
        painter.material(RIFT);
        double u = age / OUT;
        if (age < 3.0) {
            painter.flare(core, 2.2 * (1.0 - age / 3.0), 1.0 - age / 3.0);
        }
        sphere(painter, core, 1.3 * (1.0 - Ease.smooth(u)) + 0.05, 1.0 - u, seed);
        painter.circle(feet.add(0.0, 0.06, 0.0), EAST, SOUTH, 2.2 * (1.0 - Ease.smooth(u)) + 0.2, 0.1, 0.6,
                Colors.alpha(0.9 * (1.0 - u)), Colors.alpha(0.45 * (1.0 - u)));
        for (int k = 0; k < 10; k++) {
            Vec3 out = Noise.direction(seed, k + 40);
            Vec3 a = core.add(out.scale(0.3 + 1.8 * u));
            Vec3 b = core.add(out.scale(0.6 + 2.6 * u));
            painter.lightTaper(a, b, 0.1, 0.0, PALE, 0.8 * (1.0 - u), 0.0);
        }
    }

    // A rip torn through the air where the blow lands: three claws of darkness edged with violet.
    static void strike(ConstructPainter painter, Vec3 at, Vec3 from, double age, int seed) {
        painter.material(RIFT);
        double u = age / STRIKE;
        double open = Ease.smooth(Math.min(1.0, age / 2.0));
        Vec3 toward = at.subtract(from);
        Vec3 ahead = toward.lengthSqr() < 1.0E-6 ? SOUTH : toward.normalize();
        Vec3[] across = Vectors.across(ahead);
        for (int k = -1; k <= 1; k++) {
            Vec3 shift = across[1].scale(k * 0.28);
            Vec3 a = at.add(across[0].scale(-0.9 * open)).add(across[1].scale(0.7 * open)).add(shift);
            Vec3 b = at.add(across[0].scale(0.9 * open)).add(across[1].scale(-0.7 * open)).add(shift);
            Vec3 mid = a.lerp(b, 0.5);
            painter.lightTaper(a, mid, 0.0, 0.22, ABYSS, 0.0, 0.9 * (1.0 - u));
            painter.lightTaper(mid, b, 0.22, 0.0, ABYSS, 0.9 * (1.0 - u), 0.0);
            painter.glowTaper(a, mid, 0.0, 0.6, VIOLET, 0.0, 0.7 * (1.0 - u));
            painter.glowTaper(mid, b, 0.6, 0.0, VIOLET, 0.7 * (1.0 - u), 0.0);
        }
        sphere(painter, at, 0.6 * (1.0 - u) * open, 1.0 - u, seed);
        shards(painter, at, age, seed, 10);
    }

    // A sphere of nothing: black in the middle, a violet rim, light bending round its edge.
    private static void sphere(ConstructPainter painter, Vec3 at, double radius, double strength, int seed) {
        if (strength <= 0.01 || radius <= 0.01) {
            return;
        }
        double time = painter.time();
        int boil = (int) (time * 0.6);
        painter.glowDisc(at, radius * 1.9, DEEP, 0.4 * strength, 0.3, seed + boil);
        painter.glowDisc(at, radius * 1.25, VIOLET, 0.5 * strength, 0.25, seed + 1 + boil);
        painter.lightDisc(at, radius * 1.05, ABYSS, 0.95 * strength, 0.12, seed + 2 + boil);
        painter.lightDisc(at, radius * 0.8, ABYSS, strength, 0.08, seed + 3);
        Vec3 view = painter.camera().subtract(at);
        if (view.lengthSqr() > 1.0E-6) {
            Vec3[] across = Vectors.across(view.normalize());
            painter.circle(at, across[0], across[1], radius * 1.08, radius * 0.05, radius * 0.3,
                    Colors.alpha(0.9 * strength), Colors.alpha(0.5 * strength));
        }
    }

    private static void shards(ConstructPainter painter, Vec3 core, double t, int seed, int count) {
        if (t < 0.0 || t > 14.0) {
            return;
        }
        double u = t / 14.0;
        for (int k = 0; k < count; k++) {
            Vec3 out = Noise.direction(seed, k + 70);
            double fly = Ease.smooth(Math.min(1.0, t / 8.0)) * (2.0 + 2.5 * Noise.of(seed, k, 1));
            Vec3 a = core.add(out.scale(fly * 0.6));
            Vec3 b = core.add(out.scale(fly));
            painter.lightTaper(a, b, 0.16 * (1.0 - u), 0.0, ABYSS, 0.9 * (1.0 - u), 0.0);
            painter.glowTaper(a, b, 0.4 * (1.0 - u), 0.0, VIOLET, 0.45 * (1.0 - u), 0.0);
        }
    }
}
