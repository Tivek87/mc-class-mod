package nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.Cosmos;
import nl.tivek.multiversepowers.engine.client.fx.Lens;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.fx.Singularity;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The black hole's light: the hole itself (Singularity: the world bent round it, a disc of glowing matter whirling
// round it, the picture dragged in towards it), jets of light out of its poles and streaks spiralling in (the blocks
// it tears out of the ground are real ones, engine/entity/TornBlock), and, crushed, a burst as far as it harms: a
// flare, shells and rays of light, the world bent in a running ring, a ring of light racing over the ground and a
// flash and a shake for whoever is near. Without the shader a window on black space with rings of light stands in.
final class HandHoleLight {
    private static final int WHITE = 0xF2FFF4;
    private static final int LIGHT = 0x5CFF7A;
    private static final int DEEP = 0x1FAE4A;
    private static final int PALE = 0xB8FFC8;
    private static final int VOID = 0x000000;
    private static final int STARS = 0x1C3A24;
    // The horizon against the hole's size: the shadow it casts is some 2.6 horizons in radius.
    private static final double HORIZON = 0.45;
    private static final double INNER = 2.2;
    private static final double OUTER = 5.0;
    private static final double TILT = 0.35;
    private static final double PULL_NEAR = 6.0;
    private static final double PULL_FAR = 45.0;
    private static final double JET = 7.0;
    private static final double WAVE = 34.0;
    private static final double FLASH_FAR = 40.0;
    private static final int FALLING_IN = 24;
    private static final int RAYS = 28;
    private static final int RING = 72;
    private static int flashed = Integer.MIN_VALUE;

    private HandHoleLight() {
    }

    static void draw(LanternPainter painter, int id, HandPose.Place place, double clock, double strength) {
        Vec3 center = place.at(HandPose.HOLE_AT);
        double scale = place.scale();
        double size = HandPose.holeSize(clock);
        double pull = HandPose.holePull(clock);
        double collapse = HandPose.holeCollapse(clock);
        double far = painter.camera().distanceTo(center);
        Vec3 ground = place.at(new Vec3(HandPose.HOLE_AT.x, 0.0, HandPose.HOLE_AT.z));
        burst(painter, id, center, ground, scale, HandPose.holeBurst(clock), far, strength);
        if (size <= 0.01) {
            return;
        }
        // Unsteady before the fist closes, it trembles in size.
        double radius = HandPose.HOLE_SIZE * size * scale * (1.0 + 0.1 * collapse * Math.sin(clock * 3.1));
        Vec3 tilt = Vectors.spin(new Vec3(0.0, 1.0, 0.0), place.right().normalize(), TILT);
        if (Singularity.available()) {
            double disc = Mth.clamp((size - 0.3) / 0.7, 0.0, 1.0);
            double near = Mth.clamp(1.0 - (far - PULL_NEAR) / (PULL_FAR - PULL_NEAR), 0.0, 1.0);
            Singularity.hole(center, radius * HORIZON, tilt, INNER, INNER + (OUTER - INNER) * disc,
                    Math.min(1.0, size * 4.0) * strength, collapse, WHITE, DEEP, 0.7 * pull * near * strength, id);
        } else {
            window(painter, id, center, radius, scale, tilt, clock, strength);
        }
        painter.glowDisc(center, radius * 3.0, LIGHT, (0.06 + 0.5 * collapse) * size * strength, 0.2, id);
        jets(painter, center, tilt, radius, scale, size, clock, pull * strength);
        streaks(painter, id, center, tilt, radius, scale, clock, pull * strength);
    }

    // Two narrow beams of light thrown out of its poles, flickering, longest while it pulls hardest.
    private static void jets(LanternPainter painter, Vec3 center, Vec3 axis, double radius, double scale,
            double size, double clock, double strength) {
        if (strength <= 0.02) {
            return;
        }
        double length = JET * scale * size * (0.85 + 0.15 * Math.sin(clock * 1.7));
        for (int pole = -1; pole <= 1; pole += 2) {
            Vec3 way = axis.scale(pole);
            Vec3 from = center.add(way.scale(radius * 1.1));
            for (int part = 0; part < 4; part++) {
                double a = length * part / 4.0;
                double b = length * (part + 1) / 4.0;
                double flicker = 0.75 + 0.25 * Math.sin(clock * 3.1 + part * 1.7 + pole);
                painter.lightLine(from.add(way.scale(a)), from.add(way.scale(b)), (0.5 - 0.1 * part) * scale,
                        part == 0 ? WHITE : PALE, Colors.alpha(0.85 * strength * flicker * (1.0 - 0.2 * part)));
            }
        }
    }

    // Streaks of light falling in from all round, swirled about its disc on the way.
    private static void streaks(LanternPainter painter, int id, Vec3 center, Vec3 tilt, double radius, double scale,
            double clock, double strength) {
        for (int k = 0; k < FALLING_IN; k++) {
            double run = (clock * (0.04 + 0.05 * strength) + Noise.of(id, k, 5)) % 1.0;
            Vec3 from = center.add(Noise.direction(k * 7 + (int) (clock * 0.05 + Noise.of(id, k, 6) * 9.0), 31)
                    .scale((radius + 12.0 * scale) * (1.0 - run)));
            Vec3 swirl = Vectors.spin(from.subtract(center), tilt, 1.2 * run).add(center);
            painter.lightLine(swirl, swirl.lerp(center, 0.25), 0.07 * scale, PALE,
                    Colors.alpha(0.8 * strength * Math.sin(Math.PI * run)));
        }
    }

    // Crushed, it bursts out as far as it harms: a flare, two shells of light, rays thrown every way, the world bent
    // in a running ring, a ring of light racing over the ground, and a flash and a shake for whoever is near.
    private static void burst(LanternPainter painter, int id, Vec3 center, Vec3 ground, double scale, double burst,
            double far, double strength) {
        if (burst <= 0.0 || burst >= 1.0) {
            return;
        }
        double fade = (1.0 - burst) * strength;
        double out = 1.0 - Math.pow(1.0 - burst, 3.0);
        double reach = HandPose.HOLE_BLAST_RADIUS * scale;
        painter.flare(center, 15.0 * (1.0 - burst) * scale, fade);
        painter.shell(center, reach * Ease.smooth(burst), PALE, 0.8 * fade);
        painter.shell(center, reach * 0.55 * Ease.smooth(Math.min(1.0, burst * 1.7)), WHITE, 0.55 * fade);
        if (Lens.available()) {
            Lens.bubble(center, reach * Ease.smooth(burst), 0.6 * fade, PALE, 0.5);
        }
        Singularity.wave(center, (2.0 + WAVE * out) * scale, fade, WHITE);
        rays(painter, id, center, reach, burst, fade, scale);
        groundRing(painter, ground, reach * out, fade, scale);
        if (burst < 0.15 && flashed != id && far < FLASH_FAR) {
            flashed = id;
            double near = 1.0 - far / FLASH_FAR;
            ScreenFlash.add(PALE, (float) (0.6 * near), 12);
            CameraShake.add((float) (6.0 * near), 14);
        }
    }

    // Narrow beams of light thrown out every way, shooting out fast and thinning as they fade.
    private static void rays(LanternPainter painter, int id, Vec3 center, double reach, double burst, double fade,
            double scale) {
        double shot = Ease.smooth(Math.min(1.0, burst * 2.5));
        for (int k = 0; k < RAYS; k++) {
            Vec3 way = Noise.direction(id * 31 + k, 47);
            double length = reach * (0.55 + 0.45 * Noise.of(id, k, 21)) * shot;
            Vec3 from = center.add(way.scale(length * 0.15));
            Vec3 to = center.add(way.scale(length));
            double width = (0.18 + 0.12 * Noise.of(id, k, 22)) * scale * (1.0 - burst);
            painter.lightLine(from, to, width, WHITE, Colors.alpha(0.9 * fade));
            painter.glowLine(from, to, width * 5.0, LIGHT, Colors.alpha(0.45 * fade));
        }
    }

    // A ring of light racing out over the ground under the hole.
    private static void groundRing(LanternPainter painter, Vec3 ground, double radius, double fade, double scale) {
        if (radius < 0.5 || fade <= 0.02) {
            return;
        }
        Vec3 last = null;
        for (int i = 0; i <= RING; i++) {
            double angle = Math.PI * 2.0 * i / RING;
            Vec3 point = ground.add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius);
            if (last != null) {
                painter.lightLine(last, point, (0.12 + 0.3 * fade) * scale, PALE, Colors.alpha(0.85 * fade));
                painter.glowLine(last, point, 1.8 * scale, LIGHT, Colors.alpha(0.4 * fade));
            }
            last = point;
        }
    }

    // Without the shader: a window on black, starless space always facing you, a thin bright ring at its edge and a
    // tilted disc of light racing round it.
    private static void window(LanternPainter painter, int id, Vec3 center, double radius, double scale, Vec3 tilt,
            double clock, double strength) {
        Vec3 view = painter.camera().subtract(center);
        Vec3 normal = view.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : view.normalize();
        Vec3[] face = Vectors.across(normal);
        List<Vec3> outline = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            double angle = Math.PI * 2.0 * i / 28;
            outline.add(center.add(face[0].scale(Math.cos(angle) * radius)).add(face[1].scale(Math.sin(angle)
                    * radius)));
        }
        Cosmos.window(center, normal, face[0], outline, 40.0, VOID, STARS, 0.4, id);
        if (Lens.available()) {
            Lens.bubble(center, radius * 1.9, 0.4 * strength, VOID, 0.0);
        }
        HandWonderLight.ring(painter, center.add(normal.scale(0.05)), face, radius * 1.04, 0.07 * scale, WHITE,
                0.95 * strength);
        Vec3[] disc = Vectors.across(tilt);
        for (int band = 0; band < 3; band++) {
            double r = radius * (1.5 + 0.55 * band);
            double fade = (0.8 - 0.2 * band) * strength;
            HandWonderLight.ring(painter, center, disc, r, (0.12 - 0.03 * band) * scale, band == 0 ? WHITE : LIGHT,
                    fade);
            for (int k = 0; k < 6; k++) {
                double angle = clock * (0.5 / (1.0 + band)) + k * Math.PI / 3.0 + band;
                Vec3 a = center.add(disc[0].scale(Math.cos(angle) * r)).add(disc[1].scale(Math.sin(angle) * r));
                Vec3 b = center.add(disc[0].scale(Math.cos(angle + 0.6) * r)).add(disc[1].scale(Math.sin(angle + 0.6)
                        * r));
                painter.lightLine(a, b, 0.16 * scale, WHITE, Colors.alpha(0.8 * fade));
            }
        }
    }
}
