package nl.tivek.multiversepowers.character.greenlantern;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public final class ConstructPath {
    // steered flips what start/line/tilt/join mean below (see straight/steered).
    private final boolean steered;
    private final Vec3 start;
    private final Vec3 line;
    private final double tilt;
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

    public static ConstructPath straight(Vec3 start, Vec3 line, double speed, double range) {
        return new ConstructPath(false, start, line, 0.0, 0.0, speed, range);
    }

    public static ConstructPath steered(Vec3 offset, double tilt, double join, double speed, double range) {
        return new ConstructPath(true, offset, Vec3.ZERO, tilt, Math.max(1.0E-3, join), speed, range);
    }

    public boolean steered() {
        return this.steered;
    }

    public double travelled(double ticks) {
        return Math.max(0.0, Math.min(this.range, this.speed * ticks));
    }

    public Vec3 along(double distance, @Nullable Sight sight) {
        if (!this.steered || sight == null) {
            return this.start.add(this.line.scale(distance));
        }
        double aside = this.aside(distance);
        return sight.at(this.start.x * aside, this.start.y * aside, this.start.z + distance);
    }

    public Vec3 onSight(double distance, @Nullable Sight sight) {
        if (!this.steered || sight == null) {
            return this.along(distance, sight);
        }
        return sight.at(0.0, 0.0, this.start.z + distance);
    }

    public Vec3 way(double distance, @Nullable Sight sight) {
        if (!this.steered || sight == null) {
            return this.line;
        }
        double tipped = this.tilt * this.aside(distance);
        return sight.up().scale(Math.sin(tipped)).add(sight.forward().scale(Math.cos(tipped)));
    }

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

    private static void writeVec(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public record Sight(Vec3 eye, Vec3 right, Vec3 up, Vec3 forward) {
        public static Sight of(Vec3 eye, float yRot, float xRot) {
            Vec3 ahead = Vec3.directionFromRotation(0.0F, yRot);
            Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
            Vec3 forward = Vec3.directionFromRotation(xRot, yRot);
            return new Sight(eye, right, right.cross(forward), forward);
        }

        public Vec3 at(double x, double y, double z) {
            return this.eye.add(this.right.scale(x)).add(this.up.scale(y)).add(this.forward.scale(z));
        }

        public Vec3 local(Vec3 world) {
            Vec3 way = world.subtract(this.eye);
            return new Vec3(way.dot(this.right), way.dot(this.up), way.dot(this.forward));
        }
    }
}
