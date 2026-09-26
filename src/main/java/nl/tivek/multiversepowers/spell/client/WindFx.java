package nl.tivek.multiversepowers.spell.client;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The gust: ribbons of wind spiralling up round the caster, then a crescent of air rolling forward, curling as it
// goes, with streaks racing through it.
final class WindFx {
    static final int LIFE = 14;
    private static final double RANGE = 8.0;
    private static final double HALF = Math.toRadians(50.0);
    private static final int WHITE = 0xF4FBFF;
    private static final int PALE = 0xC8E6EE;
    private static final int SKY = 0x9FD3E0;

    private WindFx() {
    }

    static void gust(ConstructPainter painter, Vec3 origin, Vec3 look, double age, int seed) {
        Vec3 ahead = look.normalize();
        Vec3[] across = Vectors.across(ahead);
        Vec3 side = ahead.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() < 1.0E-6 ? across[0] : side.normalize();
        Vec3 up = side.cross(ahead).normalize();
        double time = painter.time();
        swirl(painter, origin.add(0.0, -1.3, 0.0), age, seed);
        double front = Math.min(RANGE, (age + 1.0) * 1.0);
        double fade = 1.0 - Ease.smooth((age - 7.0) / (LIFE - 7.0));
        if (fade <= 0.0) {
            return;
        }
        // The crescent: a few layers of air, the front one brightest, bowed forward in the middle.
        for (int layer = 0; layer < 4; layer++) {
            double d = front - layer * 0.55;
            if (d <= 0.3) {
                continue;
            }
            double strength = fade * (1.0 - layer * 0.22) * Math.min(1.0, age / 1.5);
            Vec3 last = null;
            for (int i = 0; i <= 18; i++) {
                double u = i / 18.0;
                double yaw = (u * 2.0 - 1.0) * HALF;
                double bow = d * (1.0 + 0.12 * Math.cos(yaw * 1.8));
                double lift = 0.35 * Math.sin(u * Math.PI * 3.0 + time * 0.5 + layer) * (0.5 + 0.5 * layer);
                Vec3 way = ahead.scale(Math.cos(yaw)).add(side.scale(Math.sin(yaw)));
                Vec3 at = origin.add(way.scale(bow)).add(up.scale(lift - 0.2 * layer));
                if (last != null) {
                    double edge = Math.sin(Math.PI * u);
                    painter.lightTaper(last, at, 0.75, 0.75, WHITE, 0.55 * strength * edge, 0.55 * strength * edge);
                    painter.glowTaper(last, at, 2.2, 2.2, SKY, 0.32 * strength * edge, 0.32 * strength * edge);
                    painter.lightTaper(last.add(up.scale(0.3)), at.add(up.scale(0.3)), 0.16, 0.16, PALE,
                            0.75 * strength * edge, 0.75 * strength * edge);
                }
                last = at;
            }
        }
        // Curls of air tumbling along the front.
        for (int k = 0; k < 6; k++) {
            double yaw = (Noise.of(seed, k, 1) * 2.0 - 1.0) * HALF * 0.9;
            Vec3 way = ahead.scale(Math.cos(yaw)).add(side.scale(Math.sin(yaw)));
            Vec3 at = origin.add(way.scale(Math.max(0.5, front - 0.3))).add(up.scale((Noise.of(seed, k, 2) - 0.5)
                    * 1.2));
            curl(painter, at, way, side, 0.35 + 0.25 * Noise.of(seed, k, 3), time * 0.6 + k, fade);
        }
        // Streaks racing out through the cone.
        for (int k = 0; k < 12; k++) {
            double yaw = (Noise.of(seed, k, 4) * 2.0 - 1.0) * HALF;
            double pitch = (Noise.of(seed, k, 5) - 0.5) * 0.5;
            Vec3 way = ahead.scale(Math.cos(yaw)).add(side.scale(Math.sin(yaw))).add(up.scale(pitch)).normalize();
            double head = Math.min(RANGE, front * (0.8 + 0.4 * Noise.of(seed, k, 6)));
            Vec3 a = origin.add(way.scale(Math.max(0.2, head - 1.8)));
            Vec3 b = origin.add(way.scale(head));
            painter.lightTaper(a, b, 0.0, 0.07, WHITE, 0.0, 0.7 * fade);
            painter.glowTaper(a, b, 0.0, 0.3, SKY, 0.0, 0.3 * fade);
        }
    }

    private static void curl(ConstructPainter painter, Vec3 at, Vec3 way, Vec3 side, double size, double phase,
            double strength) {
        Vec3 axis = way.cross(side).normalize();
        Vec3 last = null;
        for (int i = 0; i <= 10; i++) {
            double u = i / 10.0;
            double angle = phase + u * Math.PI * 1.6;
            double r = size * (1.0 - 0.6 * u);
            Vec3 next = at.add(way.scale(Math.cos(angle) * r)).add(axis.scale(Math.sin(angle) * r));
            if (last != null) {
                painter.lightTaper(last, next, 0.1 * (1.0 - u), 0.1 * (1.0 - u), WHITE, 0.6 * strength * (1.0 - u),
                        0.5 * strength * (1.0 - u));
            }
            last = next;
        }
    }

    // Three ribbons of wind wind up round the caster's feet and fly off.
    private static void swirl(ConstructPainter painter, Vec3 feet, double age, int seed) {
        double fade = 1.0 - Ease.smooth(age / 9.0);
        if (fade <= 0.0) {
            return;
        }
        double climb = Ease.smooth(Math.min(1.0, age / 5.0));
        for (int j = 0; j < 3; j++) {
            Vec3 last = null;
            for (int i = 0; i <= 16; i++) {
                double u = i / 16.0;
                if (u > climb) {
                    break;
                }
                double angle = j * 2.094 + u * Math.PI * 3.0 + age * 0.4;
                double r = 1.1 - 0.4 * u + 0.4 * age / 9.0;
                Vec3 next = feet.add(Math.cos(angle) * r, 2.3 * u, Math.sin(angle) * r);
                if (last != null) {
                    double a = fade * Math.sin(Math.PI * u);
                    painter.lightTaper(last, next, 0.22, 0.22, WHITE, 0.45 * a, 0.45 * a);
                    painter.glowTaper(last, next, 0.7, 0.7, SKY, 0.2 * a, 0.2 * a);
                }
                last = next;
            }
        }
    }
}
