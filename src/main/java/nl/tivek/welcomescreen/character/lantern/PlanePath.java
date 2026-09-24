package nl.tivek.welcomescreen.character.lantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The way the plane of the air strike flies (see {@link AirStrike}), worked out alike by the server and by every client
 * from how long ago it was called, so a thing this big glides smoothly however unevenly the updates come in.
 * <ul>
 * <li><b>Taking shape</b> ({@link #FORM} ticks): it grows out of the ring's light high over its maker and sets off
 * slowly, picking up speed.</li>
 * <li><b>Attacking</b> ({@code attack} ticks): it drones on in one straight line at its slow, steady speed, level, only
 * swaying a little in the air.</li>
 * <li><b>The crash</b> (at most {@link #DIVE} ticks): all at once its nose drops and it plunges into the ground, rolling
 * as it falls, faster and faster, until it strikes: {@link #crashTick}.</li>
 * </ul>
 *
 * @param start where it takes shape, high in the air
 * @param way   the way it flies, flat and one long
 * @param drop  how far below {@code start} the ground is where a full dive would end, in blocks
 * @param attack how many ticks it drones on and fires before it goes down
 * @param end   how far through its dive it strikes the ground, 0 to 1: less than 1 when something stands in its way
 */
public record PlanePath(Vec3 start, Vec3 way, double drop, int attack, double end) {
    /** Ticks from the call until it flies at its full speed: it takes shape meanwhile. */
    public static final int FORM = 40;
    /** Ticks a full dive takes, from the moment its nose drops to the ground. */
    public static final int DIVE = 30;
    /** How fast it flies, in blocks per tick: a big, slow plane. */
    public static final double SPEED = 0.18;
    // How much faster it goes forward at the end of the dive than while it drones on, and how steeply the fall
    // speeds up (the power of the part of the dive that has gone).
    private static final double DIVE_PUSH = 1.4;
    private static final double FALL = 2.2;
    // How long the nose takes to drop into the way it falls, as a part of the dive, and how far it rolls over.
    private static final double NOSE = 0.3;
    private static final double ROLL = 1.3;

    /** The tick its nose drops and it starts to go down. */
    public double diveTick() {
        return FORM + this.attack;
    }

    /** The tick it strikes the ground. */
    public double crashTick() {
        return this.diveTick() + DIVE * Mth.clamp(this.end, 0.0, 1.0);
    }

    /** 0 while it flies level, rising to 1 at the end of a full dive: how far through its dive it is. */
    public double down(double t) {
        return Mth.clamp((t - this.diveTick()) / DIVE, 0.0, 1.0);
    }

    /** How far along its way it has come, in blocks, {@code t} ticks after the call. */
    private double gone(double t) {
        if (t < FORM) {
            // Speeding up smoothly: the integral of the smooth S-curve, half of FORM at the end of it.
            double x = Math.max(0.0, t) / FORM;
            return SPEED * FORM * (x * x * x - 0.5 * x * x * x * x);
        }
        double level = SPEED * (FORM * 0.5 + Math.min(t, this.diveTick()) - FORM);
        double u = this.down(t);
        // Diving it keeps going forward, a little faster as it falls.
        return level + SPEED * DIVE * (u + 0.5 * (DIVE_PUSH - 1.0) * u * u);
    }

    /** How far below its height it has fallen, in blocks. */
    private double fallen(double t) {
        return this.drop * Math.pow(this.down(t), FALL);
    }

    /** Where it is, {@code t} ticks after the call. */
    public Vec3 at(double t) {
        // A gentle rise and fall in the air while it drones on, gone once it plunges.
        double sway = 0.35 * Math.sin(t * 0.045) * ramp(t) * (1.0 - this.down(t));
        return this.start.add(this.way.scale(this.gone(t))).add(0.0, sway - this.fallen(t), 0.0);
    }

    /** How far its nose points down, in radians: 0 while it flies level. */
    public double pitch(double t) {
        double u = this.down(t);
        if (u <= 0.0) {
            return 0.0;
        }
        // The way it really falls, which the nose follows once it has dropped.
        double ahead = SPEED * (1.0 + (DIVE_PUSH - 1.0) * u);
        double sink = this.drop * FALL * Math.pow(Math.max(u, 1.0E-3), FALL - 1.0) / DIVE;
        double falling = Math.atan2(sink, ahead);
        return falling * smooth(u / NOSE);
    }

    /**
     * How far it leans about the way it flies, in radians (positive: its right wing down): a slow sway while it drones
     * on, and a roll over as it plunges.
     */
    public double roll(double t) {
        double u = this.down(t);
        double sway = 0.05 * Math.sin(t * 0.06 + 1.0) * ramp(t);
        return sway + ROLL * u * u + 0.12 * u * Math.sin(t * 0.9);
    }

    /** Its right, up and forward, one long each, as it flies: pitched down and rolled in its dive. */
    public Vec3[] axes(double t) {
        Vec3 flatRight = this.way.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
        double pitch = this.pitch(t);
        Vec3 forward = this.way.scale(Math.cos(pitch)).add(0.0, -Math.sin(pitch), 0.0);
        Vec3 up = flatRight.cross(forward).normalize();
        double roll = this.roll(t);
        double cos = Math.cos(roll);
        double sin = Math.sin(roll);
        // Turned about the way it flies: its right wing goes down for a positive roll.
        Vec3 right = flatRight.scale(cos).subtract(up.scale(sin));
        Vec3 turnedUp = up.scale(cos).add(flatRight.scale(sin));
        return new Vec3[] { right, turnedUp, forward };
    }

    /** A point of the plane, given in blocks to its right, up and ahead of its middle, out in the world. */
    public Vec3 point(double t, double x, double y, double z) {
        Vec3[] axes = this.axes(t);
        return this.at(t).add(axes[0].scale(x)).add(axes[1].scale(y)).add(axes[2].scale(z));
    }

    /** Where it strikes the ground. */
    public Vec3 crash() {
        return this.at(this.crashTick());
    }

    /** Where it would be at the end of a full dive, on the ground under its height: to look for that ground. */
    public Vec3 diveEnd() {
        return this.start.add(this.way.scale(this.gone(this.diveTick() + DIVE)));
    }

    /** A point on its way down, {@code u} (0 to 1) through a full dive, if it fell {@code drop} blocks in all. */
    public static Vec3 diving(Vec3 start, Vec3 way, int attack, double drop, double u) {
        return new PlanePath(start, way, drop, attack, 1.0).at(FORM + attack + DIVE * u);
    }

    /** 0 at the call, 1 once it has taken shape: how far its sway has come in. */
    private static double ramp(double t) {
        return smooth((t - FORM) / 40.0);
    }

    /** 0 below 0, 1 above 1, and a smooth S-curve in between. */
    public static double smooth(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    /**
     * The same number between 0 and 1 for the same three numbers, on the server and on every client: how the rounds of
     * its guns spread.
     */
    public static double noise(int a, int b, int c) {
        long h = a * 73856093L ^ b * 19349663L ^ c * 83492791L;
        h ^= h >>> 13;
        h *= 0x5bd1e995L;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65536.0;
    }
}
