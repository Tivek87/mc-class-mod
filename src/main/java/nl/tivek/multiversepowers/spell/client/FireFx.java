package nl.tivek.multiversepowers.spell.client;

import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The fireball: a boiling ball of fire with flames streaming off it and a trail of heat, and the burst where it lands.
final class FireFx {
    static final int BURST = 40;
    static final int WHITE = 0xFFF6D8;
    static final int YELLOW = 0xFFD55A;
    static final int ORANGE = 0xFF7A1A;
    static final int RED = 0xC8300E;
    static final int SMOKE = 0x2A201A;
    private static final Material FIRE = new Material(0xFF7A1A, 0xFFB347, 0xFF6A10, 0xFFF0C0);

    private FireFx() {
    }

    static void fly(ConstructPainter painter, SpellFx.Fx fx, float partialTick, double age) {
        Vec3 at = fx.was.lerp(fx.at, partialTick);
        Vec3 way = fx.at.subtract(fx.was);
        Vec3 back = way.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, -1.0) : way.normalize().reverse();
        if (!painter.visible(at, 3.0)) {
            return;
        }
        double time = painter.time();
        double grow = Ease.smooth(Math.min(1.0, age / 3.0));
        trail(painter, fx, at);
        // Flames stream off the back of the ball, waving as they go.
        Vec3[] across = Vectors.across(back);
        for (int k = 0; k < 6; k++) {
            double phase = time * 1.3 + k * 1.05 + fx.seed() * 0.1;
            Vec3 last = at.add(across[0].scale(Math.cos(k * 1.047) * 0.18)).add(across[1].scale(Math.sin(k * 1.047)
                    * 0.18));
            double wide = 0.26 * grow;
            for (int i = 1; i <= 5; i++) {
                double u = i / 5.0;
                double sway = Math.sin(phase + u * 5.0) * 0.18 * u;
                Vec3 next = at.add(back.scale(u * (0.9 + 0.35 * Math.sin(phase * 0.7))))
                        .add(across[0].scale(Math.cos(k * 1.047) * (0.2 + 0.25 * u) + sway))
                        .add(across[1].scale(Math.sin(k * 1.047) * (0.2 + 0.25 * u) + sway * 0.5));
                double w = 0.26 * (1.0 - u) * grow;
                painter.glowTaper(last, next, wide * 2.2, w * 2.2, Colors.mix(ORANGE, RED, (float) u), 0.45 * (1.0
                        - u * 0.7), 0.45 * (1.0 - u));
                painter.lightTaper(last, next, wide, w, Colors.mix(YELLOW, ORANGE, (float) u), 0.7 * (1.0 - u * 0.5),
                        0.7 * (1.0 - u));
                last = next;
                wide = w;
            }
        }
        ball(painter, at, 0.42 * grow, 1.0, fx.seed());
        painter.material(FIRE);
        painter.flare(at, 0.5 * grow, 0.35);
    }

    // The way it came, fading from yellow heat to a smoky red.
    private static void trail(ConstructPainter painter, SpellFx.Fx fx, Vec3 at) {
        Vec3 last = at;
        double wide = 0.34;
        int i = 0;
        int count = fx.trail.size();
        for (Iterator<Vec3> points = fx.trail.iterator(); points.hasNext(); i++) {
            Vec3 next = points.next();
            if (i == 0) {
                continue;
            }
            double u = (double) i / Math.max(1, count - 1);
            double w = 0.34 * (1.0 - u);
            painter.glowTaper(last, next, wide * 2.4, w * 2.4, Colors.mix(ORANGE, RED, (float) u), 0.35 * (1.0 - u),
                    0.35 * (1.0 - u));
            painter.lightTaper(last, next, wide * 0.7, w * 0.7, Colors.mix(YELLOW, ORANGE, (float) u), 0.55 * (1.0
                    - u), 0.55 * (1.0 - u));
            if (i % 3 == 0) {
                painter.lightDisc(next.add(0.0, 0.2 * u, 0.0), 0.25 + 0.4 * u, SMOKE, 0.22 * u * (1.0 - u) * 4.0,
                        0.35, fx.seed() + i);
            }
            last = next;
            wide = w;
        }
    }

    // A ball of fire that boils: a white-hot heart, rolling lobes of yellow and orange, a red glow round it.
    static void ball(ConstructPainter painter, Vec3 at, double radius, double heat, int seed) {
        if (radius <= 1.0E-3) {
            return;
        }
        double time = painter.time();
        int boil = (int) (time * 0.9);
        double close = Ease.smooth((painter.camera().distanceTo(at) - radius * 0.5) / (radius * 1.5 + 0.5));
        if (close <= 0.01) {
            return;
        }
        float hot = (float) Mth.clamp(heat, 0.0, 1.0);
        painter.glowDisc(at, radius * 2.0, Colors.mix(RED, ORANGE, hot), 0.4 * close, 0.3, seed + boil);
        for (int k = 0; k < 4; k++) {
            double phase = time * 1.1 + k * 1.7 + seed * 0.3;
            Vec3 off = Noise.direction(seed, k + 3).scale(radius * 0.35 * (0.6 + 0.4 * Math.sin(phase)));
            painter.lightDisc(at.add(off), radius * (0.62 + 0.15 * Math.sin(phase * 1.3)), Colors.mix(ORANGE,
                    YELLOW, hot * 0.8F), (0.35 + 0.3 * hot) * close, 0.4, seed + 5 * k + boil);
        }
        painter.glowDisc(at, radius * 0.7, Colors.mix(YELLOW, WHITE, hot), 0.8 * hot * close, 0.2, seed + 1);
    }

    // The burst: a flash, a ball of fire swelling and rolling up, a ring of heat racing over the ground, flames
    // licking up in a circle, and a dark cloud of smoke climbing on its own column.
    static void burst(ConstructPainter painter, Vec3 at, double age, int seed) {
        if (!painter.visible(at, 6.0)) {
            return;
        }
        double time = painter.time();
        painter.material(FIRE);
        if (age < 4.0) {
            double flash = 1.0 - age / 4.0;
            painter.flare(at, 2.6 * flash, flash);
            painter.glowDisc(at, 3.5 * flash, YELLOW, 0.5 * flash, 0.2, seed);
        }
        double swell = Ease.backOut(Math.min(1.0, age / 6.0));
        double cool = Math.max(0.0, 1.0 - age / 18.0);
        if (cool > 0.0) {
            for (int k = 0; k < 7; k++) {
                Vec3 lobe = Noise.direction(seed, k).multiply(1.0, 0.6, 1.0).scale(0.9 * swell)
                        .add(0.0, 0.25 * swell + 0.05 * age, 0.0);
                ball(painter, at.add(lobe), (0.7 + 0.4 * Noise.of(seed, k, 1)) * swell, cool, seed + k * 7);
            }
        }
        Vec3 ground = at.add(0.0, 0.08, 0.0);
        if (age < 10.0) {
            double u = age / 10.0;
            painter.circle(ground, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 0.6 + 4.2 * Ease.smooth(u), 0.12,
                    0.7, Colors.alpha(0.9 * (1.0 - u)), Colors.alpha(0.5 * (1.0 - u)));
        }
        double burn = Math.max(0.0, 1.0 - Math.max(0.0, age - 12.0) / (BURST - 12.0));
        for (int k = 0; k < 10; k++) {
            double angle = Math.PI * 2.0 * k / 10.0 + seed * 0.3;
            double reach = (0.9 + 0.8 * Noise.of(seed, k, 2)) * Math.min(1.0, age / 5.0);
            Vec3 foot = ground.add(Math.cos(angle) * reach, 0.0, Math.sin(angle) * reach);
            double tall = (0.7 + 0.6 * Noise.of(seed, k, (int) (time * 0.3))) * burn * Ease.smooth(age / 4.0);
            tongue(painter, foot, tall, 0.35, burn, seed * 11 + k);
        }
        // The cloud of smoke: it climbs, spreads into a cap and thins away.
        if (age > 3.0) {
            double rise = Ease.smooth((age - 3.0) / 30.0);
            double fade = Math.sin(Math.PI * Math.min(1.0, (age - 3.0) / (BURST - 3.0)));
            for (int k = 0; k < 9; k++) {
                double u = k / 8.0;
                Vec3 puff = at.add(Noise.direction(seed, k + 20).multiply(0.5 + 1.2 * u, 0.2, 0.5 + 1.2 * u)
                        .scale(0.5 + rise)).add(0.0, 0.5 + 3.2 * rise * (0.6 + 0.4 * u), 0.0);
                painter.lightDisc(puff, 0.7 + 1.2 * rise, SMOKE, 0.4 * fade, 0.4, seed + k + (int) (time * 0.3));
            }
        }
    }

    static void tongue(ConstructPainter painter, Vec3 foot, double tall, double wide, double strength, int seed) {
        if (strength <= 0.01 || tall <= 0.02) {
            return;
        }
        double time = painter.time();
        double phase = Noise.of(seed, 3, 1) * Math.PI * 2.0;
        Vec3 last = foot;
        double lastWide = wide;
        for (int i = 1; i <= 5; i++) {
            double u = i / 5.0;
            Vec3 next = foot.add(Math.sin(time * 0.9 + phase + u * 4.0) * wide * 0.5 * u, tall * u,
                    Math.cos(time * 0.7 + phase + u * 3.0) * wide * 0.4 * u);
            double w = wide * (1.0 - 0.85 * u);
            painter.glowTaper(last, next, lastWide * 2.0, w * 2.0, Colors.mix(ORANGE, RED, (float) u),
                    0.45 * strength, 0.3 * strength);
            painter.lightTaper(last, next, lastWide, w, Colors.mix(YELLOW, ORANGE, (float) u), 0.75 * strength,
                    0.4 * strength);
            last = next;
            lastWide = w;
        }
    }
}
