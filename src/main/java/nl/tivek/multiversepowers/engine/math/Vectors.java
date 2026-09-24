package nl.tivek.multiversepowers.engine.math;

import net.minecraft.world.phys.Vec3;

/** Directions in the world: turning them, and laying things out around them. */
public final class Vectors {
    /** Straight up. */
    public static final Vec3 UP = new Vec3(0, 1, 0);

    private Vectors() {
    }

    /** {@code v} turned by {@code angle} (radians) around the unit vector {@code axis}. */
    public static Vec3 spin(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }

    /** Two directions square to {@code axis} and to each other, to lay things out around it. */
    public static Vec3[] across(Vec3 axis) {
        Vec3 side = Math.abs(axis.y) < 0.95 ? axis.cross(UP) : axis.cross(new Vec3(1, 0, 0));
        side = side.normalize();
        return new Vec3[] { side, side.cross(axis).normalize() };
    }
}
