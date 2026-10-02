package nl.tivek.multiversepowers.spell.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.fx.Lens;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.spell.SpellFxPayload;
import org.joml.Vector3f;

// The thunder clap, blasting out of the hands the way the caster aims: a blinding light between them, a bubble of
// bent light in which time all but stands still rolling ahead with the shock, a spray of thunder sparks crawling
// inside it, forked lightning sparks crackling through the wave as it rolls (never a bolt from the sky) and a wall
// of mist rolling out.
final class ClapFx {
    static final int LIFE = 46;
    // The same reach and cone as the hits in Thunderclap.
    static final double REACH = 9.0;
    static final double HALF_ANGLE = 0.8;
    private static final int FLASH = 8;
    private static final int BUBBLE = 8;
    private static final double BUBBLE_SIZE = 4.0;
    // How strongly the bubble bends and tints what is behind it: a thin, light glass.
    private static final double BUBBLE_GLASS = 0.6;
    // How fast time runs for the sparks while the bubble holds them.
    private static final double SLOWED = 0.35;
    private static final int STREAKS = 190;
    private static final int PUFFS = 30;
    private static final double SHAKE_REACH = 32.0;
    private static final double FLASH_REACH = 20.0;
    // The lightning sparks: how many at once, for how long, and how fast the wave they ride rolls (as Thunderclap).
    private static final int ARCS = 14;
    private static final double ARC_TICKS = 12.0;
    private static final double WAVE = 2.25;

    private static final int WHITE = 0xF4FBFF;
    private static final int PALE = 0xBFE8FF;
    private static final int ICE = 0x8FD3FF;
    private static final int BLUE = 0x3FA2FF;
    private static final int DUST = 0xC9D6E0;
    private static final Material LIGHT = new Material(0x8FD3FF, 0xDDF3FF, 0x3FA2FF, 0xF4FBFF);

    private ClapFx() {
    }

    // The caster's feet: the hands' height over them rides along in hundredths of a block.
    static Vec3 feet(SpellFxPayload said) {
        return said.from().subtract(0.0, said.ticks() / 100.0, 0.0);
    }

    // The caster's own first-person hands draw with their own projection and sit higher than the server's guess for
    // the body, so on the caster's own screen the clap starts where those hands meet.
    static SpellFxPayload seen(SpellFxPayload said) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || said.entity() != minecraft.player.getId()
                || !minecraft.options.getCameraType().isFirstPerson()) {
            return said;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vector3f clap = ClientClaps.CLAP;
        Vec3 hands = camera.getPosition().add(new Vec3(camera.getLookVector()).scale(-clap.z))
                .add(new Vec3(camera.getUpVector()).scale(clap.y));
        int drop = (int) Math.round(Math.max(0.0, hands.y - feet(said).y) * 100.0);
        return new SpellFxPayload(said.kind(), hands, said.to(), said.entity(), said.seed(), drop);
    }

    // Which way the blast goes: from the hands towards where it was aimed, up or down as well.
    static Vec3 aimed(Vec3 hands, Vec3 aim) {
        Vec3 way = aim.subtract(hands);
        return way.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : way.normalize();
    }

    // A direction inside the blast's cone, picked by a noise value from 0 to 1: turned about the aim's own up.
    static Vec3 within(Vec3 ahead, double pick) {
        return Vectors.spin(ahead, across(ahead).cross(ahead), (pick * 2.0 - 1.0) * HALF_ANGLE);
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

    static void draw(ConstructPainter painter, SpellFxPayload said, double age, int seed) {
        Vec3 hands = said.from();
        Vec3 ahead = aimed(hands, said.to());
        painter.material(LIGHT);
        light(painter, hands, age, seed);
        bubble(painter, hands, ahead, age);
        streaks(painter, hands, ahead, age, seed);
        arcs(painter, hands, ahead, age, seed);
        dust(painter, hands, ahead, age, seed);
    }

    // A direction square to the aim, level with the ground.
    private static Vec3 across(Vec3 ahead) {
        Vec3 side = ahead.cross(Vectors.UP);
        return side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
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

    // The bubble swells out of the hands and rolls ahead the way they aim, the world behind it bent as through a lens,
    // until it is spent. Without the lens shader it is drawn as a clear shell with a bright rim.
    private static void bubble(ConstructPainter painter, Vec3 hands, Vec3 ahead, double age) {
        if (age >= BUBBLE) {
            return;
        }
        double u = age / BUBBLE;
        double radius = 0.5 + (BUBBLE_SIZE - 0.5) * (1.0 - (1.0 - u) * (1.0 - u) * (1.0 - u));
        double on = Ease.smooth(age / 1.0) * (1.0 - Ease.smooth((u - 0.35) / 0.65)) * BUBBLE_GLASS;
        Vec3 center = hands.add(ahead.scale(radius * 0.8));
        if (Lens.available()) {
            Lens.bubble(center, radius, on, PALE, 0.0);
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
        int step = ClientSettings.detailStep();
        for (int k = 0; k < STREAKS; k += step) {
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

    // Lightning sparks: short forked arcs crackling in the wave as it rolls out, each struck anew somewhere else every
    // tick, fewer and fainter as the wave spends itself.
    private static void arcs(ConstructPainter painter, Vec3 hands, Vec3 ahead, double age, int seed) {
        if (age >= ARC_TICKS) {
            return;
        }
        double left = 1.0 - age / ARC_TICKS;
        double front = Math.min(REACH, (age + 1.0) * WAVE);
        int tick = (int) age;
        int step = ClientSettings.detailStep();
        for (int k = 0; k < ARCS; k += step) {
            int s = seed * 31 + tick * 97 + k;
            if (Noise.of(s, k, 61) > 0.35 + 0.6 * left) {
                continue;
            }
            Vec3 way = within(ahead, Noise.of(s, k, 63));
            double along = 0.8 + (front - 0.8) * (0.35 + 0.65 * Noise.of(s, k, 62));
            Vec3 start = hands.add(way.scale(along)).add(0.0, (Noise.of(s, k, 64) - 0.5) * 1.4, 0.0);
            Vec3 dir = new Vec3(Noise.of(s, k, 65) - 0.5, Noise.of(s, k, 66) - 0.5, Noise.of(s, k, 67) - 0.5);
            dir = dir.lengthSqr() < 1.0E-4 ? way : dir.normalize();
            jagged(painter, start, dir, 0.7 + 1.5 * Noise.of(s, k, 68), 5, s, left, true);
        }
    }

    // One crackling arc: a line broken into kinks, now and then forking half way.
    private static void jagged(ConstructPainter painter, Vec3 from, Vec3 dir, double length, int kinks, int seed,
            double fade, boolean fork) {
        Vec3 side = across(dir);
        Vec3 up = side.cross(dir).normalize();
        double kink = length / kinks * 0.9;
        Vec3 last = from;
        for (int i = 1; i <= kinks; i++) {
            Vec3 next = from.add(dir.scale(length * i / kinks));
            if (i < kinks) {
                next = next.add(side.scale((Noise.of(seed, i, 71) - 0.5) * kink))
                        .add(up.scale((Noise.of(seed, i, 72) - 0.5) * kink));
            }
            painter.lightLine(last, next, 0.03, WHITE, Colors.alpha(0.95 * fade));
            painter.glowLine(last, next, 0.22, BLUE, Colors.alpha(0.45 * fade));
            if (fork && i == kinks / 2 && Noise.of(seed, i, 73) < 0.6) {
                Vec3 off = dir.add(side.scale(Noise.of(seed, i, 74) - 0.5)).add(up.scale(0.6)).normalize();
                jagged(painter, next, off, length * 0.45, 3, seed + 7, fade * 0.8, false);
            }
            last = next;
        }
    }

    // The mist wall: puffs rolling out of the hands the way they aim, swelling, then hanging and thinning as a haze.
    private static void dust(ConstructPainter painter, Vec3 hands, Vec3 ahead, double age, int seed) {
        double left = 1.0 - age / LIFE;
        if (left <= 0.0) {
            return;
        }
        double rolled = Ease.smooth(Math.min(1.0, age / 10.0));
        int every = ClientSettings.detailStep();
        for (int k = 0; k < PUFFS; k += every) {
            Vec3 way = within(ahead, Noise.of(seed, k, 11));
            double reach = 1.0 + REACH * (0.2 + 0.85 * Noise.of(seed, k, 12)) * rolled;
            double drift = (Noise.of(seed, k, 13) - 0.5) * 1.2 * rolled + age * 0.02;
            Vec3 at = hands.add(way.scale(reach)).add(0.0, drift, 0.0);
            double size = 0.7 + 1.5 * rolled + age * 0.04;
            painter.lightDisc(at, size, DUST, 0.45 * left * left, 0.35, seed + k);
        }
        if (age < 6.0) {
            double burst = 1.0 - age / 6.0;
            painter.lightDisc(hands.add(ahead.scale(1.5 + age)), 1.5 + age * 0.9, WHITE, 0.45 * burst, 0.3,
                    seed + 99);
        }
    }
}
