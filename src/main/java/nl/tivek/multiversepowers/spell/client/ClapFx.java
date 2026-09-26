package nl.tivek.multiversepowers.spell.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The thunder clap, blasting forward out of the hands: a blinding light between them, a clear shock bubble bursting
// ahead, a spray of thunder sparks, slabs of ground heaving up, chunks flung away and a wall of mist rolling out.
final class ClapFx {
    static final int LIFE = 46;
    // The same reach and cone as the hits in ThunderClapSpell.
    static final double REACH = 9.0;
    static final double HALF_ANGLE = 0.8;
    private static final int FLASH = 8;
    private static final int BUBBLE = 6;
    private static final double BUBBLE_SIZE = 3.6;
    private static final int STREAKS = 150;
    private static final int CHUNKS = 22;
    private static final int PUFFS = 30;
    private static final int SLABS = 5;
    private static final double GRAVITY = 0.07;
    private static final double SLAB_HEIGHT = 1.2;
    private static final double SLABS_SINK = 24.0;
    private static final double SLABS_GONE = 38.0;
    private static final double[][] SLAB = { { -0.5, -0.3, -0.18, 0.5, SLAB_HEIGHT, 0.18, 1.0 } };
    private static final double SHAKE_REACH = 32.0;
    private static final double FLASH_REACH = 20.0;

    private static final int WHITE = 0xF4FBFF;
    private static final int PALE = 0xBFE8FF;
    private static final int ICE = 0x8FD3FF;
    private static final int BLUE = 0x3FA2FF;
    private static final int DUST = 0xC9D6E0;
    private static final int STONE = 0x5E5A55;
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
        dust(painter, feet, ahead, age, seed);
        int rock = Colors.mix(ground(feet), STONE, 0.45F);
        painter.material(new Material(rock, Colors.shade(rock, 0.7), 0x000000, WHITE));
        chunks(painter, feet, ahead, age, seed);
        slabs(painter, feet, ahead, age, seed);
        painter.material(LIGHT);
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

    // A clear sphere with a bright rim swells out of the hands and bulges ahead, then is gone.
    private static void bubble(ConstructPainter painter, Vec3 hands, Vec3 ahead, double age) {
        if (age >= BUBBLE) {
            return;
        }
        double u = age / BUBBLE;
        double radius = 0.4 + (BUBBLE_SIZE - 0.4) * (1.0 - (1.0 - u) * (1.0 - u));
        double on = 1.0 - Ease.smooth(u);
        Vec3 center = hands.add(ahead.scale(radius * 0.75));
        painter.shell(center, radius, PALE, 0.9 * on);
        painter.shell(center, radius * 0.94, ICE, 0.35 * on);
    }

    // Thunder sparks: short bright streaks sprayed forward out of the clap, fast at first, slowing and dropping.
    private static void streaks(ConstructPainter painter, Vec3 hands, Vec3 ahead, double age, int seed) {
        for (int k = 0; k < STREAKS; k++) {
            double t = age - 3.0 * Noise.of(seed, k, 41);
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
        int tint = Colors.mix(DUST, ground(feet), 0.25F);
        double rolled = Ease.smooth(Math.min(1.0, age / 10.0));
        for (int k = 0; k < PUFFS; k++) {
            Vec3 way = within(ahead, Noise.of(seed, k, 11));
            double reach = 1.0 + REACH * (0.2 + 0.85 * Noise.of(seed, k, 12)) * rolled;
            double rise = 0.3 + 1.5 * Noise.of(seed, k, 13) * rolled + age * 0.02;
            Vec3 at = feet.add(way.scale(reach)).add(0.0, rise, 0.0);
            double size = 0.7 + 1.5 * rolled + age * 0.04;
            painter.lightDisc(at, size, tint, 0.45 * left * left, 0.35, seed + k);
        }
        if (age < 6.0) {
            double burst = 1.0 - age / 6.0;
            painter.lightDisc(feet.add(ahead.scale(1.5 + age)).add(0.0, 0.8, 0.0), 1.5 + age * 0.9, WHITE,
                    0.45 * burst, 0.3, seed + 99);
        }
    }

    // Chunks torn out of the ground ahead, flung forward and up; they land, lie a moment and shrink away.
    private static void chunks(ConstructPainter painter, Vec3 feet, Vec3 ahead, double age, int seed) {
        for (int k = 0; k < CHUNKS; k++) {
            Vec3 way = within(ahead, Noise.of(seed, k, 21));
            double start = 1.0 + 2.0 * Noise.of(seed, k, 22);
            double speed = 0.35 + 0.45 * Noise.of(seed, k, 23);
            double up = 0.25 + 0.4 * Noise.of(seed, k, 24);
            double t = Math.min(age, 2.0 * up / GRAVITY);
            double out = start + speed * t;
            double height = Math.max(0.0, up * t - 0.5 * GRAVITY * t * t);
            double size = (0.12 + 0.2 * Noise.of(seed, k, 25)) * (1.0 - Ease.smooth((age - (LIFE - 10)) / 10.0));
            if (size <= 0.0) {
                continue;
            }
            Vec3 at = feet.add(way.scale(out)).add(0.0, height + size * 0.5, 0.0);
            painter.chunk(at, size, Noise.direction(seed, k + 40), t * (0.3 + 0.4 * Noise.of(seed, k, 26)), 0.9);
        }
    }

    // Slabs of ground heave up in a fan ahead, tipped away by the blast; they stand a moment, then sink back.
    private static void slabs(ConstructPainter painter, Vec3 feet, Vec3 ahead, double age, int seed) {
        if (age >= SLABS_GONE) {
            return;
        }
        double up = Ease.smooth(age / 3.0) * (1.0 - Ease.smooth((age - SLABS_SINK) / (SLABS_GONE - SLABS_SINK)));
        if (up <= 0.0) {
            return;
        }
        for (int k = 0; k < SLABS; k++) {
            Vec3 out = within(ahead, (k + 0.3 + 0.4 * Noise.of(seed, k, 31)) / SLABS);
            double reach = 2.2 + 2.6 * Noise.of(seed, k, 32);
            double size = 0.6 + 0.5 * Noise.of(seed, k, 33);
            double tilt = 0.35 + 0.45 * Noise.of(seed, k, 34);
            Vec3 stand = Vectors.UP.scale(Math.cos(tilt)).add(out.scale(Math.sin(tilt)));
            Vec3 face = out.scale(Math.cos(tilt)).subtract(Vectors.UP.scale(Math.sin(tilt)));
            Vec3 base = feet.add(out.scale(reach)).subtract(stand.scale(SLAB_HEIGHT * size * (1.0 - up)));
            painter.model(SLAB, new ConstructPainter.Frame(base, face.cross(stand), stand, face, size), 1.0, 0.9);
        }
    }

    private static int ground(Vec3 feet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return DUST;
        }
        BlockPos below = BlockPos.containing(feet.x, feet.y - 0.2, feet.z);
        BlockState state = level.getBlockState(below);
        MapColor color = state.getMapColor(level, below);
        return state.isAir() || color == MapColor.NONE ? DUST : color.col;
    }
}
