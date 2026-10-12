package nl.tivek.multiversepowers.space;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.sky.SkyMoon;

// The way between the world and the moon straight above it, the same numbers on the server and in every game. The
// moon is a world of its own, as no block can lie as high as it hangs: half-way up, in empty space, a flyer passes
// from one world into the other, turned over about the x axis, so what was above lies below and the way on stays the
// way on.
public final class Space {
    public static final ResourceKey<Level> MOON = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "moon"));
    // Ground to ground, in blocks.
    public static final double DISTANCE = 7000.0;
    public static final double HALF_WAY = DISTANCE * 0.5;
    // This far past half-way a flyer changes worlds, so one hovering there is not passed to and fro.
    public static final double PAST = 8.0;
    public static final double EARTH_GROUND = 64.0;
    public static final double MOON_GROUND = 96.0;
    // Half the edge of each as a cube: the moon as wide as the sky shows it from the ground, its middle that far off.
    public static final double MOON_HALF = SkyMoon.HALF * DISTANCE / (1.0 - SkyMoon.HALF);
    public static final double EARTH_HALF = MOON_HALF * 3.5;
    // Where the sky goes black above the world's ground, and where a power's flight speeds up.
    private static final double AIR_FROM = 500.0;
    private static final double AIR_TO = 1600.0;
    private static final double HASTE_FROM = 120.0;
    private static final double HASTE_TO = 900.0;
    private static final double HASTE = 8.0;

    // High above its ground a flyer goes round the world below instead of over it: from ORBIT up (coming in over
    // RAMP blocks of height), a lap is LAP blocks flown sideways from where they rose. Only near the line between the
    // two worlds (CROSS radians off it at most) do they pass into the other.
    private static final double EARTH_ORBIT = 1000.0;
    private static final double MOON_ORBIT = 500.0;
    private static final double RAMP = 400.0;
    private static final double EARTH_LAP = 2400.0;
    private static final double MOON_LAP = 1200.0;
    public static final double CROSS = 0.15;

    private Space() {
    }

    // Whether a flyer this high keeps the place they rose from as their way round; lower, it follows them.
    public static boolean orbits(Level level, double height) {
        return height >= (onMoon(level) ? MOON_ORBIT : EARTH_ORBIT);
    }

    // Whether a step is mostly up or down: the way round counts none of it, so a climb a little off straight up stays
    // on the line between the two worlds.
    public static boolean climbs(double dx, double dy, double dz) {
        return Math.hypot(dx, dz) < Math.abs(dy) * 0.5;
    }

    // How far round the world below a flyer has gone, in radians, `dx`, `dz` sideways from where they rose.
    public static double orbit(Level level, double height, double dx, double dz) {
        double from = onMoon(level) ? MOON_ORBIT : EARTH_ORBIT;
        double lap = onMoon(level) ? MOON_LAP : EARTH_LAP;
        return Math.PI * 2.0 * Math.hypot(dx, dz) / lap * Ease.smooth((height - from) / RAMP);
    }

    public static boolean onMoon(Level level) {
        return level.dimension() == MOON;
    }

    // Whether this world is one end of the way: the overworld or the moon.
    public static boolean reaches(Level level) {
        return level.dimension() == Level.OVERWORLD || onMoon(level);
    }

    public static double ground(Level level) {
        return onMoon(level) ? MOON_GROUND : EARTH_GROUND;
    }

    public static double height(Level level, double y) {
        return y - ground(level);
    }

    // How far the air has thinned at a height above the ground, 0 to 1: the moon has none at all.
    public static double thin(Level level, double height) {
        return onMoon(level) ? 1.0 : Ease.smooth((height - AIR_FROM) / (AIR_TO - AIR_FROM));
    }

    // How much faster a power's own flight goes at a height: up there is nothing to fly into.
    public static double haste(double height) {
        return 1.0 + (HASTE - 1.0) * Ease.smooth((height - HASTE_FROM) / (HASTE_TO - HASTE_FROM));
    }

    // A way or a speed in one world as it runs in the other.
    public static Vec3 turned(Vec3 way) {
        return new Vec3(way.x, -way.y, -way.z);
    }

    public static float turnedYaw(float yaw) {
        return 180.0F - yaw;
    }
}
