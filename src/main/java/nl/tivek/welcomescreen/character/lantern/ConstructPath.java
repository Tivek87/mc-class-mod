package nl.tivek.welcomescreen.character.lantern;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * The way a construct on its way flies. A bolt flies straight on from where it was shot. A Giant Fist is steered by
 * its owner's eyes: it stays on his line of sight wherever he looks, a little further out every tick, so its middle is
 * always right under his crosshair. It sets off from where it hung beside him and glides in onto that line over its
 * first few blocks, pointing the way he looks all the while instead of turning towards the line. The server and every
 * client work it out the same way from the same few numbers, so a client can move the construct along it smoothly by
 * its own clock instead of jumping from one update of the server to the next.
 */
public final class ConstructPath {
    private final boolean steered;
    // Straight: where it set off. Steered: where it hung around his eyes when he let go, measured along his line of
    // sight (x to the right of it, y above it, z out along it).
    private final Vec3 start;
    // Straight: the way it flies, a unit vector. Steered: not used.
    private final Vec3 line;
    // Steered: how far its nose pointed above his line of sight when he let go, in radians (below it when negative).
    private final double tilt;
    // Steered: how far it flies before it is on his line of sight and points straight along it.
    private final double join;
    private final double speed;
    private final double range;

    private ConstructPath(boolean steered, Vec3 start, Vec3 line, double tilt, double join, double speed,
            double range) {
        this.steered = steered;
        this.start = start;
        this.line = line;
        this.tilt = tilt;
        this.join = join;
        this.speed = speed;
        this.range = range;
    }

    /**
     * A construct that flies straight on.
     *
     * @param line  the way it flies, a unit vector
     * @param speed how far it flies a tick, in blocks
     * @param range how far it flies in all before it falls apart, in blocks
     */
    public static ConstructPath straight(Vec3 start, Vec3 line, double speed, double range) {
        return new ConstructPath(false, start, line, 0.0, 0.0, speed, range);
    }

    /**
     * A construct steered by its owner's eyes.
     *
     * @param offset where it hangs around his eyes as he lets go, along his line of sight (see {@link Sight#local})
     * @param tilt   how far its nose points above his line of sight as he lets go, in radians (below: negative)
     * @param join   how far it flies before it is on his line of sight, in blocks
     */
    public static ConstructPath steered(Vec3 offset, double tilt, double join, double speed, double range) {
        return new ConstructPath(true, offset, Vec3.ZERO, tilt, Math.max(1.0E-3, join), speed, range);
    }

    /** True for a construct its owner steers with his eyes, false for one that flies straight on. */
    public boolean steered() {
        return this.steered;
    }

    /** How far it has flown after this many ticks on its way (a part of a tick counts too). */
    public double travelled(double ticks) {
        return Math.max(0.0, Math.min(this.range, this.speed * ticks));
    }

    /**
     * Where its middle is once it has flown this far.
     *
     * @param sight where its owner's eyes are and the way they look right now; only a steered one needs it
     */
    public Vec3 along(double distance, @Nullable Sight sight) {
        if (!this.steered || sight == null) {
            return this.start.add(this.line.scale(distance));
        }
        double aside = this.aside(distance);
        return sight.at(this.start.x * aside, this.start.y * aside, this.start.z + distance);
    }

    /**
     * The point on its owner's line of sight a steered one is at, or heading for while it glides in onto it, once it
     * has flown this far; for one that flies straight, simply where it is.
     */
    public Vec3 onSight(double distance, @Nullable Sight sight) {
        if (!this.steered || sight == null) {
            return this.along(distance, sight);
        }
        return sight.at(0.0, 0.0, this.start.z + distance);
    }

    /** The way it points once it has flown this far, a unit vector (see {@link #along}). */
    public Vec3 way(double distance, @Nullable Sight sight) {
        if (!this.steered || sight == null) {
            return this.line;
        }
        double tipped = this.tilt * this.aside(distance);
        return sight.up().scale(Math.sin(tipped)).add(sight.forward().scale(Math.cos(tipped)));
    }

    /**
     * How much is left of where it hung beside its owner once it has flown this far: 1 as it sets off, 0 once it is on
     * his line of sight. It goes fast at first and slows down as it gets there, so it heads for the middle at once and
     * joins the line smoothly.
     */
    private double aside(double distance) {
        double left = 1.0 - Math.min(1.0, distance / this.join);
        return left * left;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.steered);
        writeVec(buf, this.start);
        if (this.steered) {
            buf.writeDouble(this.tilt);
            buf.writeDouble(this.join);
        } else {
            writeVec(buf, this.line);
        }
        buf.writeDouble(this.speed);
        buf.writeDouble(this.range);
    }

    public static ConstructPath read(FriendlyByteBuf buf) {
        boolean steered = buf.readBoolean();
        Vec3 start = readVec(buf);
        if (steered) {
            double tilt = buf.readDouble();
            double join = buf.readDouble();
            return steered(start, tilt, join, buf.readDouble(), buf.readDouble());
        }
        Vec3 line = readVec(buf);
        return straight(start, line, buf.readDouble(), buf.readDouble());
    }

    // Doubles, so both sides work out exactly the same way.
    private static void writeVec(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    /**
     * Where someone's eyes are and the way they look: out along the line of sight, to the right of it (always level,
     * so it stays true even looking straight up or down) and above it.
     */
    public record Sight(Vec3 eye, Vec3 right, Vec3 up, Vec3 forward) {
        /** The eyes at {@code eye}, turned {@code yRot} and tipped {@code xRot} (in degrees, as the game has them). */
        public static Sight of(Vec3 eye, float yRot, float xRot) {
            Vec3 ahead = Vec3.directionFromRotation(0.0F, yRot);
            Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
            Vec3 forward = Vec3.directionFromRotation(xRot, yRot);
            return new Sight(eye, right, right.cross(forward), forward);
        }

        /** A point along the line of sight (x to the right of it, y above it, z out along it) out in the world. */
        public Vec3 at(double x, double y, double z) {
            return this.eye.add(this.right.scale(x)).add(this.up.scale(y)).add(this.forward.scale(z));
        }

        /** The other way around: where a point of the world lies along the line of sight. */
        public Vec3 local(Vec3 world) {
            Vec3 way = world.subtract(this.eye);
            return new Vec3(way.dot(this.right), way.dot(this.up), way.dot(this.forward));
        }
    }
}
