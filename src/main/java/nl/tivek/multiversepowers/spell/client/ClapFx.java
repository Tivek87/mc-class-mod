package nl.tivek.multiversepowers.spell.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Lens;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.spell.ClapPayload;
import nl.tivek.multiversepowers.spell.SpellFxPayload;
import org.joml.Vector3f;

// The thunder clap, everything at once out of the one point where the hands meet, the way the caster aims: a blinding
// light, a spray of thunder sparks, a small bubble of bent, slightly blurred light in which time all but stands still
// with sparks frozen in it, a cloud of thunder sparks streaming to where it was aimed, and a wall of mist rolling out.
final class ClapFx {
    static final int LIFE = 50;
    // The same reach and cone as the hits in ThunderClapSpell.
    static final double REACH = 9.0;
    static final double HALF_ANGLE = 0.8;
    static final int BURSTS = ClapPayload.BUBBLE_BURSTS;
    private static final int FLASH = 8;
    private static final double BUBBLE_SIZE = 1.275;
    private static final double BLUR = 0.35;
    private static final int STREAKS = 110;
    private static final int FROZEN = 45;
    private static final int SPRAY = 300;
    private static final int PUFFS = 30;
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

    // Which way the blast goes over the ground: from the hands towards where it was aimed.
    static Vec3 facing(Vec3 hands, Vec3 aim) {
        Vec3 flat = new Vec3(aim.x - hands.x, 0.0, aim.z - hands.z);
        return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    static Vec3 aimed(Vec3 hands, Vec3 aim) {
        Vec3 way = aim.subtract(hands);
        return way.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : way.normalize();
    }

    // A direction inside the blast's cone, picked by a noise value from 0 to 1: turned about the aim's own up.
    static Vec3 within(Vec3 ahead, double pick) {
        Vec3 side = ahead.cross(Vectors.UP);
        Vec3 up = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize().cross(ahead);
        return Vectors.spin(ahead, up, (pick * 2.0 - 1.0) * HALF_ANGLE);
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
        Vec3 aim = said.to();
        Vec3 feet = feet(said);
        Vec3 ahead = aimed(hands, aim);
        painter.material(LIGHT);
        light(painter, hands, age, seed);
        bubble(painter, hands, ahead, age, seed);
        streaks(painter, hands, ahead, age, seed);
        cloud(painter, hands, aim, age, seed);
        dust(painter, feet, facing(hands, aim), age, seed);
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

    // The bubble swells out of the hands the way they aim, the world behind it bent and a little blurred as through a
    // magnifying glass, sparks frozen inside, until it is spent. Without the lens shader it is drawn as a clear shell
    // with a bright rim.
    private static void bubble(ConstructPainter painter, Vec3 hands, Vec3 ahead, double age, int seed) {
        if (age >= BURSTS) {
            return;
        }
        double u = age / BURSTS;
        double swell = Math.min(1.0, age / 5.0);
        double radius = 0.05 + (BUBBLE_SIZE - 0.05) * (1.0 - (1.0 - swell) * (1.0 - swell) * (1.0 - swell));
        double on = Math.min(1.0, 0.6 + age) * (1.0 - Ease.smooth((u - 0.6) / 0.4));
        Vec3 center = hands.add(ahead.scale(radius * 0.8));
        if (Lens.available()) {
            Lens.bubble(center, radius, on, PALE, BLUR);
            painter.shell(center, radius * 1.01, PALE, 0.15 * on);
        } else {
            painter.shell(center, radius, PALE, 0.9 * on);
            painter.shell(center, radius * 0.94, ICE, 0.35 * on);
        }
        frozen(painter, hands, center, radius, age, on, seed);
    }

    // Sparks caught in the bubble: shot out of the hands with it, then hanging all but still, flickering, crawling a
    // hair's breadth a tick.
    private static void frozen(ConstructPainter painter, Vec3 hands, Vec3 center, double radius, double age, double on,
            int seed) {
        for (int k = 0; k < FROZEN; k++) {
            double born = 0.5 * Noise.of(seed, k, 61);
            if (age < born) {
                continue;
            }
            Vec3 spot = center.add(Noise.direction(seed * 31 + k, 62).scale(radius * 0.85
                    * Math.cbrt(Noise.of(seed, k, 63))));
            Vec3 at = hands.lerp(spot, Ease.smooth((age - born) / 2.5));
            Vec3 way = Noise.direction(seed * 31 + k, 64);
            double crawl = 0.004 * (age - born);
            Vec3 head = at.add(way.scale(0.07 + 0.12 * Noise.of(seed, k, 65) + crawl));
            Vec3 tail = at.subtract(way.scale(crawl));
            double flicker = 0.55 + 0.45 * Math.sin(age * (1.5 + 2.0 * Noise.of(seed, k, 66)) + k);
            double alpha = on * flicker;
            painter.glowTaper(tail, head, 0.04, 0.08, k % 3 == 0 ? BLUE : ICE, 0.2 * alpha, 0.5 * alpha);
            painter.lightTaper(tail, head, 0.01, 0.025, WHITE, 0.3 * alpha, 0.95 * alpha);
        }
    }

    // A cloud of thunder sparks flung out of the hands the instant they meet, streaming to where the clap was aimed,
    // slowing as they get there, then hanging round it and drifting down as a glittering haze.
    private static void cloud(ConstructPainter painter, Vec3 hands, Vec3 aim, double age, int seed) {
        for (int k = 0; k < SPRAY; k++) {
            double t = age - 0.8 * Noise.of(seed, k, 71);
            double life = 16.0 + 22.0 * Noise.of(seed, k, 72);
            if (t < 0.0 || t > life) {
                continue;
            }
            Vec3 goal = aim.add(Noise.direction(seed * 17 + k, 73).scale(0.3 + 1.6 * Math.cbrt(Noise.of(seed, k,
                    74))));
            double pace = 0.25 + 0.2 * Noise.of(seed, k, 76);
            double slow = Math.exp(-pace * t);
            Vec3 path = goal.subtract(hands);
            Vec3 head = hands.add(path.scale(1.0 - slow)).add(0.0, -0.004 * t * t, 0.0);
            Vec3 speed = path.scale(pace * slow);
            Vec3 tail = head.subtract(speed.lengthSqr() < 0.0025 ? speed.normalize().scale(0.05) : speed.scale(1.5));
            double fade = 1.0 - t / life;
            double twinkle = t > 8.0 ? 0.6 + 0.4 * Math.sin(t * (2.0 + 3.0 * Noise.of(seed, k, 75)) + k) : 1.0;
            double alpha = fade * twinkle;
            boolean blue = k % 3 == 0;
            painter.glowTaper(tail, head, 0.06, 0.14, blue ? BLUE : ICE, 0.18 * alpha, 0.6 * alpha);
            painter.lightTaper(tail, head, 0.014, 0.036, blue ? PALE : WHITE, 0.25 * alpha, alpha);
        }
    }

    // Thunder sparks: short bright streaks sprayed forward out of the clap, fast at first, slowing and dropping.
    private static void streaks(ConstructPainter painter, Vec3 hands, Vec3 ahead, double age, int seed) {
        for (int k = 0; k < STREAKS; k++) {
            double t = age - 0.4 * Noise.of(seed, k, 41);
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
}
