package nl.tivek.multiversepowers.sky.client;

import java.util.List;
import net.minecraft.world.phys.Vec3;

// In the frame the game turns its stars in (sun +y, moon -y): `round` from the moon to +z, `aside` to +x.
final class SkyBodies {
    static final Vec3 SUN = new Vec3(0.0, 1.0, 0.0);
    static final Vec3 CORE = at(-40.0, 22.0);
    static final Vec3 BAND = CORE.cross(at(75.0, -28.0)).normalize();

    static final List<Planet> PLANETS = List.of(
            new Planet(Kind.GAS, at(-62.0, -28.0), 19.0, 11L, 3.42, 0.10, 0.0, 0.0),
            new Planet(Kind.EARTH, at(30.0, -14.0), 14.0, 23L, -0.35, 0.22, 0.0, 0.0),
            new Planet(Kind.SATURN, at(-12.0, 44.0), 10.0, 37L, -0.42, 0.40, 1.28, 2.35),
            new Planet(Kind.MARS, at(74.0, 30.0), 6.5, 41L, 0.2, 0.3, 0.0, 0.0),
            new Planet(Kind.ICE, at(-100.0, 18.0), 5.0, 53L, -0.15, 0.2, 0.0, 0.0),
            new Planet(Kind.TEAL, at(106.0, -34.0), 8.0, 67L, 0.75, 0.30, 1.35, 2.2),
            new Planet(Kind.VIOLET, at(-114.0, -14.0), 4.0, 71L, 0.5, 0.2, 0.0, 0.0),
            new Planet(Kind.MOON, at(-44.0, -40.0), 2.2, 83L, 0.0, 0.3, 0.0, 0.0),
            new Planet(Kind.ICE_MOON, at(-28.0, 52.0), 1.5, 97L, 0.0, 0.3, 0.0, 0.0),
            new Planet(Kind.ROCK, at(16.0, 30.0), 2.4, 101L, 0.7, 0.4, 0.0, 0.0),
            new Planet(Kind.LAVA, at(128.0, 8.0), 3.0, 113L, -0.3, 0.3, 0.0, 0.0),
            new Planet(Kind.MOON, at(42.0, -3.0), 1.6, 127L, 0.0, 0.3, 0.0, 0.0));

    static final List<Nebula> NEBULAE = List.of(
            new Nebula(at(-64.0, 8.0), 24.0, 0xFF8C3A, 0xE0405E, 1.0, 3L),
            new Nebula(at(18.0, -42.0), 30.0, 0xD24ED8, 0x5068FF, 0.9, 5L),
            new Nebula(at(86.0, 6.0), 26.0, 0x3C78FF, 0x40E0D0, 0.85, 7L),
            new Nebula(at(-110.0, 36.0), 14.0, 0xFF5A96, 0xFF9A40, 1.0, 9L),
            new Nebula(at(-22.0, -14.0), 40.0, 0x4A5AD8, 0x9A48C8, 0.35, 13L),
            new Nebula(at(-80.0, -5.0), 6.0, 0x6E9CFF, 0xA0C8FF, 0.55, 17L));

    static final Vec3 GALAXY = at(40.0, 47.0);
    static final double GALAXY_DEGREES = 19.0;
    static final double GALAXY_ROLL = 0.6;

    static final List<Cluster> CLUSTERS = List.of(
            new Cluster(at(-80.0, -5.0), 3.2, 16, 0xB8D0FF, false, 19L),
            new Cluster(at(62.0, -52.0), 2.2, 320, 0xFFE2B8, true, 29L),
            new Cluster(at(-150.0, 25.0), 4.0, 40, 0xFFF0D8, false, 31L),
            new Cluster(at(150.0, -20.0), 3.0, 30, 0xE6EEFF, false, 43L),
            new Cluster(at(-6.0, -58.0), 1.7, 220, 0xFFF4E0, true, 47L));

    enum Kind {
        EARTH, GAS, SATURN, MARS, ICE, TEAL, VIOLET, MOON, ICE_MOON, ROCK, LAVA
    }

    record Planet(Kind kind, Vec3 dir, double degrees, long seed, double roll, double tilt, double ringIn,
            double ringOut) {
        boolean ringed() {
            return this.ringOut > 0.0;
        }

        double reach() {
            return this.ringed() ? this.ringOut * 1.04 : 1.22;
        }
    }

    record Nebula(Vec3 dir, double degrees, int colorA, int colorB, double strength, long seed) {
    }

    record Cluster(Vec3 dir, double degrees, int count, int color, boolean globular, long seed) {
    }

    private SkyBodies() {
    }

    static Vec3 at(double round, double aside) {
        double r = Math.toRadians(round);
        double a = Math.toRadians(aside);
        return new Vec3(Math.sin(a), -Math.cos(a) * Math.cos(r), Math.cos(a) * Math.sin(r));
    }

    static Vec3[] frame(Vec3 dir) {
        Vec3 up = SUN.subtract(dir.scale(SUN.dot(dir)));
        up = up.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : up.normalize();
        Vec3 right = dir.cross(up);
        return new Vec3[] {right, up};
    }

    static Vec3 light(Vec3 dir) {
        Vec3[] f = frame(dir);
        return new Vec3(SUN.dot(f[0]), SUN.dot(f[1]), -SUN.dot(dir)).normalize();
    }
}
