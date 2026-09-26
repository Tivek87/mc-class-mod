package nl.tivek.multiversepowers.spell.client;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// The poison: a glass vial tumbling through the air, then a churning cloud of green gas over a glowing rune.
final class PoisonFx {
    static final int SPREAD = 8;
    static final int FADE = 25;
    private static final int BRIGHT = 0x9BF26B;
    private static final int GREEN = 0x5FB04A;
    private static final int DARK = 0x2E5A1E;
    private static final int FOG = 0x3F7A2A;
    private static final int MURK = 0x1F3A14;
    private static final Material GLASS = new Material(0xCFF5D8, 0xE8FFEE, 0x9BF26B, 0xF4FFF6);
    private static final Material TOXIC = new Material(0x5FB04A, 0xB6FF8A, 0x7FD46B, 0xE6FFD6);
    private static final ConstructPainter.Shape VIAL = ConstructPainter.Shape.of(
            Mesh.lathe(14, 1.0, 0.0, -0.16, 0.1, -0.15, 0.13, -0.08, 0.13, 0.04, 0.09, 0.1, 0.045, 0.13, 0.045, 0.2,
                    0.06, 0.21, 0.0, 0.21),
            Mesh.cylinder(10, 0.05, 0.2, 0.27, 1.2));
    private static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);

    private PoisonFx() {
    }

    // The same arc the server's vial flies, so it lands where the cloud starts.
    static Vec3 arc(Vec3 from, Vec3 to, double t) {
        double peak = 1.2 + from.distanceTo(to) * 0.15;
        return from.lerp(to, t).add(0.0, Math.sin(t * Math.PI) * peak, 0.0);
    }

    static void vial(ConstructPainter painter, Vec3 from, Vec3 to, double age, int flight) {
        double t = Math.min(1.0, (age + 1.0) / flight);
        Vec3 at = arc(from, to, t);
        Vec3 ahead = arc(from, to, Math.min(1.0, t + 0.05)).subtract(at);
        double spin = age * 0.9;
        ConstructPainter.Frame frame = ConstructPainter.Frame.of(at, ahead.lengthSqr() < 1.0E-8 ? SOUTH : ahead,
                new Vec3(0.0, 1.0, 0.0), 1.3).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, spin);
        painter.material(GLASS);
        painter.shape(VIAL, frame, 1.0, 1.0);
        painter.material(TOXIC);
        painter.glowDisc(frame.at(0.0, -0.05, 0.0), 0.28, BRIGHT, 0.6, 0.2, (int) (age * 3.0));
        painter.lightDisc(frame.at(0.0, -0.06, 0.0), 0.14, 0xE6FFD6, 0.7, 0.2, 5);
        // It drips a thin trail of glowing poison.
        Vec3 last = at;
        for (int k = 1; k <= 5; k++) {
            Vec3 back = arc(from, to, Math.max(0.0, t - 0.04 * k));
            painter.glowTaper(last, back, 0.22 * (1.0 - k / 6.0), 0.22 * (1.0 - (k + 1) / 6.0), GREEN,
                    0.5 * (1.0 - k / 6.0), 0.4 * (1.0 - k / 6.0));
            last = back;
        }
    }

    static void cloud(ConstructPainter painter, Vec3 center, double radius, double age, int ticks, int seed) {
        double reach = radius * Ease.backOut(Math.min(1.0, (age + 1.0) / SPREAD));
        double thick = Math.min(1.0, (ticks - age) / FADE) * Math.min(1.0, age / 3.0);
        if (thick <= 0.0 || !painter.visible(center, reach + 3.0)) {
            return;
        }
        double time = painter.time();
        Vec3 ground = center.add(0.0, 0.05, 0.0);
        painter.material(TOXIC);
        // The rune on the ground: two rings turning apart and a five-pointed star between them.
        painter.circle(ground, EAST, SOUTH, reach, 0.06, 0.4, Colors.alpha(0.8 * thick), Colors.alpha(0.4 * thick));
        double spin = time * 0.02;
        for (int k = 0; k < 5; k++) {
            Vec3 a = ground.add(Math.cos(spin + k * 1.2566) * reach * 0.8, 0.0, Math.sin(spin + k * 1.2566) * reach
                    * 0.8);
            Vec3 b = ground.add(Math.cos(spin + (k + 2) * 1.2566) * reach * 0.8, 0.0, Math.sin(spin + (k + 2)
                    * 1.2566) * reach * 0.8);
            painter.edge(a, b, 0.04, 0.6 * thick);
        }
        // A green haze over it all, lit from within.
        painter.haze(center.add(0.0, 0.5, 0.0), EAST.scale(reach * 1.05), new Vec3(0.0, 1.3, 0.0),
                SOUTH.scale(reach * 1.05), GREEN, 0.28 * thick);
        painter.glowDisc(center.add(0.0, 0.3, 0.0), reach * 1.1, DARK, 0.25 * thick, 0.3, seed);
        // Low, heavy fog rolling round over the ground.
        for (int k = 0; k < 22; k++) {
            double angle = Noise.of(seed, k, 1) * Math.PI * 2.0 + time * (0.01 + 0.01 * Noise.of(seed, k, 2));
            double out = Math.sqrt(Noise.of(seed, k, 3)) * reach * 0.9;
            double bob = Math.sin(time * 0.07 + k) * 0.12;
            Vec3 puff = center.add(Math.cos(angle) * out, 0.35 + 0.4 * Noise.of(seed, k, 4) + bob, Math.sin(angle)
                    * out);
            painter.lightDisc(puff, 0.8 + 0.7 * Noise.of(seed, k, 5), k % 4 == 0 ? MURK : FOG, 0.3 * thick, 0.4,
                    seed + k + (int) (time * 0.15));
        }
        // Slow tendrils of gas winding up round the middle.
        for (int j = 0; j < 5; j++) {
            Vec3 last = null;
            double lastWide = 0.0;
            for (int i = 0; i <= 8; i++) {
                double u = i / 8.0;
                double angle = time * 0.05 + j * 1.2566 + u * 2.5;
                double out = reach * (0.3 + 0.5 * u);
                Vec3 at = center.add(Math.cos(angle) * out, 0.2 + 1.6 * u, Math.sin(angle) * out);
                double wide = 0.5 * (1.0 - 0.6 * u);
                if (last != null) {
                    painter.lightTaper(last, at, lastWide, wide, BRIGHT, 0.22 * thick * (1.0 - u), 0.18 * thick
                            * (1.0 - u));
                    painter.glowTaper(last, at, lastWide * 2.0, wide * 2.0, GREEN, 0.12 * thick, 0.08 * thick);
                }
                last = at;
                lastWide = wide;
            }
        }
        // Bubbles well up out of the muck, swell and pop.
        for (int k = 0; k < 14; k++) {
            double cycle = (time * 0.035 + Noise.of(seed, k, 6)) % 1.0;
            int round = (int) (time * 0.035 + Noise.of(seed, k, 6));
            double angle = Noise.of(seed + round, k, 7) * Math.PI * 2.0;
            double out = Math.sqrt(Noise.of(seed + round, k, 8)) * reach * 0.85;
            Vec3 at = center.add(Math.cos(angle) * out, 0.1 + 1.4 * cycle, Math.sin(angle) * out);
            double size = 0.06 + 0.14 * cycle;
            if (cycle < 0.9) {
                painter.circle(at, EAST, new Vec3(0.0, 1.0, 0.0), size, 0.015, 0.06, Colors.alpha(0.8 * thick),
                        Colors.alpha(0.3 * thick));
                painter.lightDisc(at, size, 0xE6FFD6, 0.2 * thick, 0.1, k);
            } else {
                double pop = (cycle - 0.9) / 0.1;
                painter.glowDisc(at, size * (1.0 + 2.0 * pop), BRIGHT, 0.6 * thick * (1.0 - pop), 0.3, k);
            }
        }
    }
}
