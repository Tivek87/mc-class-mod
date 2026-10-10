package nl.tivek.multiversepowers.engine.client.world;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// The sky as only this game draws it, never the world's own time: something near can turn it toward a time of night
// (sun, moon, stars and light all going along), drain it to black and take the moon out of it. A wish holds only while
// it is made again every tick; left alone the sky comes back by itself.
public final class LocalSky {
    // Ticks a wish holds without being made again.
    private static final long HOLD = 3L;
    private static double night;
    private static long time;
    private static double dark;
    private static boolean moonless;
    private static long wishedAt = Long.MIN_VALUE;

    private LocalSky() {
    }

    // Turns the sky `toward` of the way to `dayTime` (0 to 24000, as /time set takes it), drains `black` of its light
    // (0 to 1) and hides the moon; made again every tick while it should last.
    public static void wish(double toward, long dayTime, double black, boolean hideMoon) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (wishedAt != now) {
            night = 0.0;
            dark = 0.0;
            moonless = false;
        }
        wishedAt = now;
        if (toward >= night) {
            night = Mth.clamp(toward, 0.0, 1.0);
            time = Math.floorMod(dayTime, 24000L);
        }
        dark = Math.max(dark, Mth.clamp(black, 0.0, 1.0));
        moonless |= hideMoon;
    }

    private static boolean on(Level level) {
        return level == Minecraft.getInstance().level && level.getGameTime() - wishedAt <= HOLD;
    }

    // The time of day this game draws, from the world's own.
    public static long dayTime(Level level, long real) {
        if (!on(level) || night <= 0.0) {
            return real;
        }
        long day = Math.floorDiv(real, 24000L);
        long target = day * 24000L + time;
        return real + Math.round((target - real) * night);
    }

    public static Vec3 skyColor(Level level, Vec3 color) {
        return on(level) && dark > 0.0 ? color.scale(1.0 - dark) : color;
    }

    public static float stars(Level level, float brightness) {
        return on(level) && dark > 0.0 ? (float) (brightness * (1.0 - dark)) : brightness;
    }

    public static boolean moonHidden(Level level) {
        return on(level) && moonless;
    }

    // Where the moon stands in this game's sky, as a direction from the eye.
    public static Vec3 moon(Level level, float partialTick) {
        double turn = level.getTimeOfDay(partialTick) * Math.PI * 2.0;
        return new Vec3(Math.sin(turn), -Math.cos(turn), 0.0);
    }
}
