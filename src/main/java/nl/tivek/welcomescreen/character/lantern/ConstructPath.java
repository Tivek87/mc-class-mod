package nl.tivek.welcomescreen.character.lantern;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * The way a construct on its way flies. A Giant Fist goes from where it hung beside its owner along a smooth curve
 * onto his line of sight, and from there straight on along that line (or straight at what he aims at, when that is
 * too close for a curve); a bolt simply flies straight. The server and every client work it out the same way from
 * the same few numbers, so a client can move the construct along it smoothly by its own clock instead of jumping
 * from one update of the server to the next.
 */
public final class ConstructPath {
    // How many straight pieces the curve is cut into.
    private static final int STEPS = 16;

    private final Vec3 start;
    @Nullable
    private final Vec3 control;
    @Nullable
    private final Vec3 end;
    private final Vec3 line;
    private final double speed;
    private final double range;
    // The curve as points, and how far along the curve each point lies; null when it flies straight.
    @Nullable
    private final Vec3[] curve;
    @Nullable
    private final double[] curveAt;

    /**
     * @param start   where it was let go
     * @param control the middle point that bends the curve, or null when it flies straight from the start
     * @param end     where the curve joins the line of sight, or null when it flies straight from the start
     * @param line    the way it flies once the curve is behind it, a unit vector
     * @param speed   how far it flies a tick, in blocks
     * @param range   how far it flies in all before it falls apart, in blocks
     */
    public ConstructPath(Vec3 start, @Nullable Vec3 control, @Nullable Vec3 end, Vec3 line, double speed,
            double range) {
        this.start = start;
        this.control = control;
        this.end = end;
        this.line = line;
        this.speed = speed;
        this.range = range;
        if (control == null || end == null) {
            this.curve = null;
            this.curveAt = null;
            return;
        }
        this.curve = new Vec3[STEPS + 1];
        this.curveAt = new double[STEPS + 1];
        for (int i = 0; i <= STEPS; i++) {
            double t = (double) i / STEPS;
            double u = 1.0 - t;
            this.curve[i] = start.scale(u * u).add(control.scale(2.0 * u * t)).add(end.scale(t * t));
            this.curveAt[i] = i == 0 ? 0.0 : this.curveAt[i - 1] + this.curve[i].distanceTo(this.curve[i - 1]);
        }
    }

    /** How far it has flown after this many ticks on its way (a part of a tick counts too). */
    public double travelled(double ticks) {
        return Math.max(0.0, Math.min(this.range, this.speed * ticks));
    }

    /** Where the middle of the fist is once it has flown this far: along the curve, then straight on. */
    public Vec3 along(double distance) {
        if (this.curve == null || this.curveAt == null) {
            return this.start.add(this.line.scale(distance));
        }
        int last = this.curve.length - 1;
        if (distance >= this.curveAt[last]) {
            return this.curve[last].add(this.line.scale(distance - this.curveAt[last]));
        }
        for (int i = 1; i <= last; i++) {
            if (distance <= this.curveAt[i]) {
                double piece = this.curveAt[i] - this.curveAt[i - 1];
                double t = piece < 1.0E-9 ? 0.0 : (distance - this.curveAt[i - 1]) / piece;
                return this.curve[i - 1].lerp(this.curve[i], t);
            }
        }
        return this.curve[last];
    }

    /** The way it flies once it has come this far, a unit vector. */
    public Vec3 way(double distance) {
        if (this.curve == null || this.curveAt == null || distance >= this.curveAt[this.curve.length - 1]) {
            return this.line;
        }
        for (int i = 1; i < this.curve.length; i++) {
            if (distance <= this.curveAt[i]) {
                Vec3 piece = this.curve[i].subtract(this.curve[i - 1]);
                return piece.lengthSqr() < 1.0E-12 ? this.line : piece.normalize();
            }
        }
        return this.line;
    }

    public void write(FriendlyByteBuf buf) {
        writeVec(buf, this.start);
        buf.writeBoolean(this.control != null && this.end != null);
        if (this.control != null && this.end != null) {
            writeVec(buf, this.control);
            writeVec(buf, this.end);
        }
        writeVec(buf, this.line);
        buf.writeDouble(this.speed);
        buf.writeDouble(this.range);
    }

    public static ConstructPath read(FriendlyByteBuf buf) {
        Vec3 start = readVec(buf);
        Vec3 control = null;
        Vec3 end = null;
        if (buf.readBoolean()) {
            control = readVec(buf);
            end = readVec(buf);
        }
        Vec3 line = readVec(buf);
        return new ConstructPath(start, control, end, line, buf.readDouble(), buf.readDouble());
    }

    // Doubles, so both sides cut the curve into exactly the same pieces.
    private static void writeVec(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
