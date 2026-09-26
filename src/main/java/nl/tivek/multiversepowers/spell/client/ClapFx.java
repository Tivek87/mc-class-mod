package nl.tivek.multiversepowers.spell.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Lens;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The thunder clap, blasting forward out of the hands: a blinding light between them, a bubble of bent light in which
// time all but stands still rolling ahead with the shock, a spray of thunder sparks crawling inside it, rings rippling
// out over the ground and a wall of mist rolling out.
final class ClapFx {
    static final int LIFE = 46;
    // The same reach and cone as the hits in ThunderClapSpell.
    static final double REACH = 9.0;
    static final double HALF_ANGLE = 0.8;
    private static final int FLASH = 8;
    private static final int BUBBLE = 18;
    private static final double BUBBLE_SIZE = 5.0;
    // How fast time runs for the sparks while the bubble holds them.
    private static final double SLOWED = 0.35;
    private static final int STREAKS = 190;
    private static final int PUFFS = 30;
    private static final int RIPPLES = 3;
    private static final double RIPPLE_TICKS = 22.0;
    private static final double SHAKE_REACH = 32.0;
    private static final double FLASH_REACH = 20.0;

    private static final int WHITE = 0xF4FBFF;
    private static final int PALE = 0xBFE8FF;
    private static final int ICE = 0x8FD3FF;
    private static final int BLUE = 0x3FA2FF;
    private static final int DUST = 0xC9D6E0;
    private static final Material LIGHT = new Material(0x8FD3FF, 0xDDF3FF, 0x3FA2FF, 0xF4FBFF);

    private ClapFx() {
    }

    // Which way the blast goes: from the caster's feet towards his hands.
    static Vec3 facing(Vec3 hands, Vec3 feet) {
        Vec3 flat = new Vec3(hands.x - feet.x, 0.0, hands.z - feet.z);
        return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    // A direction inside the blast's cone, picked by a noise value from 0 to 1.
    static Vec3 within(Vec3 facing, double pick) {
        return Vectors.spin(facing, Vectors.UP, (pick * 2.0 - 1.0) * HALF_ANGLE);
    }

    // The first tick of a clap: every player feels it by how close they are.
    static void felt(Vec3 feet) {
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance = eye.distanceTo(feet);
        double shake = 1.0 - distance / SHAKE_REACH;
        if (shake > 0.0) {
            CameraShake.add((float) (5.0 * shake * shake), 14);
        }
        double flash = 1.0 - distance / FLASH_REACH;
        if (flash > 0.0) {
            ScreenFlash.add(0xE6F4FF, (float) (0.7 * flash), 10);
        }
    }

    static void draw(ConstructPainter painter, Vec3 hands, Vec3 feet, double age, int seed) {
        Vec3 ahead = facing(hands, feet);
        painter.material(LIGHT);
        light(painter, hands, age, seed);
        bubble(painter, hands, ahead, age);
        streaks(painter, hands, ahead, age, seed);
        ripples(painter, feet, ahead, age, seed);
        dust(painter, feet, ahead, age, seed);
    }

    // Time for what the bubble holds: it crawls while the bubble stands and runs on at full pace once it is gone.
    private static double slowed(double age) {
        return age < BUBBLE ? age * SLOWED : BUBBLE * SLOWED + (age - BUBBLE);
    }

    private static void light(ConstructPainter painter, Vec3 hands, double age, int seed) {
        if (age >= FLASH) {
            return;
        }
        double on = 1.0 - age / FLASH;
        painter.flare(hands, 3.2 * on, on);
        painter.glowDisc(hands, 5.0 * on, ICE, 0.6 * on, 0.25, seed);
        painter.glowDisc(hands, 1.4 * on, WHITE, on, 0.1, seed + 1);
    }

    // The bubble swells out of the hands and rolls ahead with the shock, the world behind it bent as through a lens,
    // until it is spent. Without the lens shader it is drawn as a clear shell with a bright rim.
    private static void bubble(ConstructPainter painter, Vec3 hands, Vec3 ahead, double age) {
        if (age >= BUBBLE) {
            return;
        }
        double u = age / BUBBLE;
        double radius = 0.5 + (BUBBLE_SIZE - 0.5) * (1.0 - (1.0 - u) * (1.0 - u) * (1.0 - u));
        double on = Ease.smooth(age / 1.5) * (1.0 - Ease.smooth((u - 0.45) / 0.55));
        Vec3 center = hands.add(ahead.scale(radius * 0.8));
        if (Lens.available()) {
            Lens.bubble(center, radius, on, PALE);
            painter.shell(center, radius * 1.01, PALE, 0.15 * on);
        } else {
            painter.shell(center, radius, PALE, 0.9 * on);
            painter.shell(center, radius * 0.94, ICE, 0.35 * on);
        }
    }

    // Thunder sparks: short bright streaks sprayed forward out of the clap, fast at first, slowing and dropping; while
    // the bubble holds them they hang almost still.
    private static void streaks(ConstructPainter painter, Vec3 hands, Vec3 ahead, double age, int seed) {
        double held = slowed(age);
        for (int k = 0; k < STREAKS; k++) {
            double t = held - 3.0 * SLOWED * Noise.of(seed, k, 41);
            double life = 8.0 + 10.0 * Noise.of(seed, k, 42);
            if (t < 0.0 || t > life) {
                continue;
            }
            double speed = 0.7 + 1.1 * Noise.of(seed, k, 43);
            Vec3 way = within(ahead, Noise.of(seed, k, 44)).add(0.0, (Noise.of(seed, k, 45) - 0.35) * 0.7, 0.0)
                    .normalize();
            double slow = Math.exp(-0.22 * t);
            double gone = speed * (1.0 - slow) / 0.22;
            Vec3 head = hands.add(way.scale(gone)).add(0.0, -0.012 * t * t, 0.0);
            Vec3 tail = head.subtract(way.scale(Math.min(gone, 0.12 + 0.45 * speed * slow)));
            double fade = 1.0 - t / life;
            boolean blue = k % 3 == 0;
            painter.glowTaper(tail, head, 0.05, 0.12, blue ? BLUE : ICE, 0.15 * fade, 0.6 * fade);
            painter.lightTaper(tail, head, 0.012, 0.03, blue ? PALE : WHITE, 0.2 * fade, 0.95 * fade);
        }
    }

    // The mist wall: puffs rolling forward low over the ground, rising, then hanging and thinning as a haze.
    private static void dust(ConstructPainter painter, Vec3 feet, Vec3 ahead, double age, int seed) {
        double left = 1.0 - age / LIFE;
        if (left <= 0.0) {
            return;
        }
        double rolled = Ease.smooth(Math.min(1.0, age / 10.0));
        for (int k = 0; k < PUFFS; k++) {
            Vec3 way = within(ahead, Noise.of(seed, k, 11));
            double reach = 1.0 + REACH * (0.2 + 0.85 * Noise.of(seed, k, 12)) * rolled;
            double rise = 0.3 + 1.5 * Noise.of(seed, k, 13) * rolled + age * 0.02;
            Vec3 at = feet.add(way.scale(reach)).add(0.0, rise, 0.0);
            double size = 0.7 + 1.5 * rolled + age * 0.04;
            painter.lightDisc(at, size, DUST, 0.45 * left * left, 0.35, seed + k);
        }
        if (age < 6.0) {
            double burst = 1.0 - age / 6.0;
            painter.lightDisc(feet.add(ahead.scale(1.5 + age)).add(0.0, 0.8, 0.0), 1.5 + age * 0.9, WHITE,
                    0.45 * burst, 0.3, seed + 99);
        }
    }

    // Rings of light ripple out over the ground ahead, one after another, with a swirl turning in the first.
    private static void ripples(ConstructPainter painter, Vec3 feet, Vec3 ahead, double age, int seed) {
        Vec3 side = ahead.cross(Vectors.UP).normalize();
        Vec3 ground = feet.add(0.0, 0.06, 0.0);
        for (int k = 0; k < RIPPLES; k++) {
            double t = age - 2.5 * k;
            if (t < 0.0 || t > RIPPLE_TICKS) {
                continue;
            }
            double u = t / RIPPLE_TICKS;
            double grow = 1.0 - (1.0 - u) * (1.0 - u);
            double fade = (1.0 - u) * (1.0 - u);
            Vec3 at = ground.add(ahead.scale(1.8 + 2.4 * k + 1.5 * grow));
            double radius = 0.3 + (1.8 + 0.8 * k) * grow;
            painter.circle(at, ahead, side, radius, 0.035, 0.3, Colors.alpha(0.9 * fade),
                    Colors.alpha(0.45 * fade));
            painter.circle(at, ahead, side, radius * 0.62, 0.02, 0.18, Colors.alpha(0.5 * fade),
                    Colors.alpha(0.25 * fade));
            if (k == 0) {
                swirl(painter, at, ahead, side, radius * 0.95, t, fade, seed);
            }
        }
    }

    private static void swirl(ConstructPainter painter, Vec3 at, Vec3 ahead, Vec3 side, double radius, double t,
            double fade, int seed) {
        double turn = 0.12 * t + Noise.of(seed, 0, 51) * Math.PI * 2.0;
        for (int arm = 0; arm < 2; arm++) {
            Vec3 last = at;
            for (int i = 1; i <= 16; i++) {
                double s = i / 16.0;
                double angle = turn + arm * Math.PI + s * Math.PI * 2.2;
                Vec3 next = at.add(ahead.scale(Math.cos(angle) * radius * s))
                        .add(side.scale(Math.sin(angle) * radius * s));
                painter.lightLine(last, next, 0.03, PALE, Colors.alpha(0.8 * fade * s));
                painter.glowLine(last, next, 0.2, ICE, Colors.alpha(0.35 * fade * s));
                last = next;
            }
        }
    }
}
