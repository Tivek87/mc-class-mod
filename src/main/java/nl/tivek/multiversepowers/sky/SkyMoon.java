package nl.tivek.multiversepowers.sky;

import net.minecraft.world.phys.Vec3;

// The moon as the cosmic sky has it: a cube, always the same faces turned to the world, tilted so three of them show.
// What takes the moon out of the sky (the Giant Hands' Cosmos Test) takes this cube, standing just as it stood there.
// Only a game that draws the cosmic sky has it so; any other keeps the game's own flat moon.
public final class SkyMoon {
    // Half the cube's edge for every block it stands away, and the game's own moon's.
    public static final double HALF = 0.06;
    private static final double FLAT = 0.05;
    private static final double YAW = 0.62;
    private static final double PITCH = 0.52;
    private static volatile boolean cube;

    private SkyMoon() {
    }

    // Set by the game that draws the sky, every frame.
    public static void cube(boolean drawn) {
        cube = drawn;
    }

    public static boolean cube() {
        return cube;
    }

    public static double half() {
        return cube ? HALF : FLAT;
    }

    // The cube's own axes (right, up, forward) from the moon's face as the sky shows it: `north` across its path,
    // `path` along it, `away` from the eye. The flat moon keeps them as they are.
    public static Vec3[] axes(Vec3 north, Vec3 path, Vec3 away) {
        if (!cube) {
            return new Vec3[] { north, path, away };
        }
        return tilted(north, path, away);
    }

    public static Vec3[] tilted(Vec3 north, Vec3 path, Vec3 away) {
        return tilted(north, path, away, 1.0);
    }

    // `share` of the tilt: at 0 the cube turns one flat face to the eye.
    public static Vec3[] tilted(Vec3 north, Vec3 path, Vec3 away, double share) {
        double yaw = YAW * share;
        double pitch = PITCH * share;
        Vec3 right = north.scale(Math.cos(yaw)).add(away.scale(Math.sin(yaw)));
        Vec3 forward = away.scale(Math.cos(yaw)).subtract(north.scale(Math.sin(yaw)));
        Vec3 up = path.scale(Math.cos(pitch)).add(forward.scale(Math.sin(pitch)));
        forward = forward.scale(Math.cos(pitch)).subtract(path.scale(Math.sin(pitch)));
        return new Vec3[] { right, up, forward };
    }
}
