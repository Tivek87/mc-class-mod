package nl.tivek.multiversepowers.engine.fx;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class ParticleFx {
    private static final double VIEW_RANGE = 128.0;
    private static final double NEAR_RANGE = 32.0;
    public static final RandomSource RANDOM = RandomSource.create();

    private ParticleFx() {
    }

    public static Vector3f color(int rgb) {
        return new Vector3f(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
    }

    public static ParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(color(rgb), Mth.clamp(size, 0.01F, 4.0F));
    }

    public static ParticleOptions fade(int fromRgb, int toRgb, float size) {
        return new DustColorTransitionOptions(color(fromRgb), color(toRgb), Mth.clamp(size, 0.01F, 4.0F));
    }

    public static double spread(double range) {
        return (RANDOM.nextDouble() * 2.0 - 1.0) * range;
    }

    public static boolean chance(double probability) {
        return RANDOM.nextDouble() < probability;
    }

    public static void send(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count,
            double dx, double dy, double dz, double speed) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) < VIEW_RANGE * VIEW_RANGE) {
                ParticleBatch.add(player, particle, true, x, y, z, count, dx, dy, dz, speed);
            }
        }
    }

    public static void sendNear(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count,
            double dx, double dy, double dz, double speed) {
        Vec3 at = new Vec3(x, y, z);
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerToCenterThan(at, NEAR_RANGE)) {
                ParticleBatch.add(player, particle, false, x, y, z, count, dx, dy, dz, speed);
            }
        }
    }

    public static void at(ServerLevel level, ParticleOptions particle, Vec3 pos) {
        send(level, particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    public static void fly(ServerLevel level, ParticleOptions particle, Vec3 pos, Vec3 direction, double speed) {
        send(level, particle, pos.x, pos.y, pos.z, 0, direction.x, direction.y, direction.z, speed);
    }

    public static void cloud(ServerLevel level, ParticleOptions particle, Vec3 pos, int count, double spread,
            double speed) {
        send(level, particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double spacing) {
        double length = from.distanceTo(to);
        int count = Math.max(1, (int) Math.ceil(length / spacing));
        for (int i = 0; i <= count; i++) {
            at(level, particle, from.lerp(to, (double) i / count));
        }
    }

    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points,
            double rotation) {
        for (int i = 0; i < points; i++) {
            double angle = rotation + Math.PI * 2 * i / points;
            at(level, particle, center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
        }
    }

    public static void shockwave(ServerLevel level, ParticleOptions particle, Vec3 center, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points + spread(0.1);
            fly(level, particle, center, new Vec3(Math.cos(angle), 0, Math.sin(angle)), speed);
        }
    }

    public static void sphereOut(ServerLevel level, ParticleOptions particle, Vec3 center, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double y = 1.0 - 2.0 * (i + 0.5) / count;
            double r = Math.sqrt(1.0 - y * y);
            double angle = i * 2.399963;
            fly(level, particle, center, new Vec3(Math.cos(angle) * r, y, Math.sin(angle) * r), speed);
        }
    }

    public static void sphere(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count,
            double rotation) {
        for (int i = 0; i < count; i++) {
            double y = 1.0 - 2.0 * (i + 0.5) / count;
            double r = Math.sqrt(1.0 - y * y);
            double angle = i * 2.399963 + rotation;
            at(level, particle, center.add(Math.cos(angle) * r * radius, y * radius, Math.sin(angle) * r * radius));
        }
    }

    public static void zigzag(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int segments,
            double jitter, double spacing) {
        Vec3 previous = from;
        for (int i = 1; i <= segments; i++) {
            double f = (double) i / segments;
            Vec3 next = from.lerp(to, f);
            if (i < segments) {
                next = next.add(spread(jitter), spread(jitter * 0.4), spread(jitter));
            }
            line(level, particle, previous, next, spacing);
            previous = next;
        }
    }

    public static Vec3[] basis(Vec3 normal) {
        Vec3 up = Math.abs(normal.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = normal.cross(up).normalize();
        Vec3 v = u.cross(normal).normalize();
        return new Vec3[] { u, v };
    }

    public static void disc(ServerLevel level, ParticleOptions particle, Vec3 center, Vec3 normal, double radius,
            int points, double rotation) {
        Vec3[] b = basis(normal);
        for (int i = 0; i < points; i++) {
            double angle = rotation + Math.PI * 2 * i / points;
            at(level, particle,
                    center.add(b[0].scale(Math.cos(angle) * radius)).add(b[1].scale(Math.sin(angle) * radius)));
        }
    }

    public static void discStar(ServerLevel level, ParticleOptions particle, Vec3 center, Vec3 normal, double radius,
            int points, int skip, double rotation, double spacing) {
        Vec3[] b = basis(normal);
        for (int i = 0; i < points; i++) {
            double a1 = rotation + Math.PI * 2 * i / points;
            double a2 = rotation + Math.PI * 2 * ((i + skip) % points) / points;
            Vec3 p1 = center.add(b[0].scale(Math.cos(a1) * radius)).add(b[1].scale(Math.sin(a1) * radius));
            Vec3 p2 = center.add(b[0].scale(Math.cos(a2) * radius)).add(b[1].scale(Math.sin(a2) * radius));
            line(level, particle, p1, p2, spacing);
        }
    }

    public static void groundStar(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points,
            int skip, double rotation, double spacing) {
        discStar(level, particle, center, new Vec3(0, 1, 0), radius, points, skip, rotation, spacing);
    }

    public static void helix(ServerLevel level, ParticleOptions particle, Vec3 base, double radius, double height,
            double turns, int points, double rotation) {
        for (int i = 0; i <= points; i++) {
            double fraction = (double) i / points;
            double angle = rotation + fraction * turns * Math.PI * 2;
            double y = fraction * height;
            at(level, particle, base.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
        }
    }

    public static void magicCircle(ServerLevel level, ParticleOptions primary, ParticleOptions secondary,
            Vec3 center, double radius, double rotation) {
        ring(level, primary, center, radius, 36, rotation);
        ring(level, secondary, center, radius * 0.7, 24, -rotation * 0.8);
        ring(level, primary, center, radius * 0.35, 16, rotation * 1.2);
        for (int i = 0; i < 6; i++) {
            double angle = rotation + (Math.PI * 2 * i / 6.0);
            Vec3 outer = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            Vec3 inner = center.add(Math.cos(angle) * (radius * 0.35), 0, Math.sin(angle) * (radius * 0.35));
            line(level, secondary, inner, outer, 0.4);
        }
    }

    public static void implosion(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count,
            double speed) {
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = radius * (0.4 + RANDOM.nextDouble() * 0.6);
            double y = spread(radius * 0.4);
            Vec3 pos = center.add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
            Vec3 inward = center.subtract(pos).normalize();
            fly(level, particle, pos, inward, speed);
        }
    }
}
