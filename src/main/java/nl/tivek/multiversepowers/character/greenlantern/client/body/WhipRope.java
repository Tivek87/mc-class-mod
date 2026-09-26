package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipLash;
import nl.tivek.multiversepowers.engine.math.Ease;

// The lash as it is drawn: a rope of pieces that keep their length. Where the lash is taut or wound up the rope keeps
// right to the shape its move gives it; where it is slack the rope falls, lands on the ground and slides to a stop,
// and where the shape runs into the ground the rope lies on it. So it never jumps, stretches or shrinks.
final class WhipRope {
    private static final Map<Integer, WhipRope> ROPES = new HashMap<>();
    // Per tick, and per tick squared for the fall.
    private static final double FALL = 0.06;
    private static final double KEEP_SPEED = 0.9;
    private static final double GROUND_SLIDE = 0.45;
    private static final double FIRM_PULL = 40.0;
    private static final double SLACK_PULL = 1.2;
    // How much a piece lying on the ground keeps the way it lay, so one that points straight down does not spin round.
    private static final double LEAN = 0.3;
    // Where the shape dives into the ground steeper than this (the flat share of a piece) the rope lets go of it.
    private static final double STEEP = 0.75;
    // How fast a piece far from its place comes back to it, on top of the way its place moves, in blocks per tick.
    private static final double REEL = 3.0;
    // The most speed a piece keeps of its own, in blocks per tick: after a crack the lash drops, it never flails.
    private static final double LOOSE_SPEED = 4.0;
    // The most a piece may bend from the one before it: a rope bends in curves and loops, it never folds into zigzags.
    private static final double MOST_BEND = Math.toRadians(70.0);
    private static final double COS_BEND = Math.cos(MOST_BEND);
    private static final double SIN_BEND = Math.sin(MOST_BEND);
    // A handle that jumps this far between two frames (a teleport, another view) takes a fresh rope.
    private static final double LOST = 2.5;
    private static final float GONE_AFTER = 40.0F;

    private Vec3[] points = new Vec3[0];
    private Vec3[] before = new Vec3[0];
    private Vec3[] aimed = new Vec3[0];
    private float at = Float.NaN;
    private float lastStep = 1.0F;

    private WhipRope() {
    }

    static Vec3[] follow(int owner, WhipLine.Shape shape, double ground, float now) {
        return ROPES.computeIfAbsent(owner, id -> new WhipRope()).step(shape, ground, now);
    }

    static void forgetOld(float now) {
        ROPES.values().removeIf(rope -> !(now - rope.at < GONE_AFTER));
    }

    static void clear() {
        ROPES.clear();
    }

    private Vec3[] step(WhipLine.Shape shape, double ground, float now) {
        Vec3[] target = shape.points();
        int n = target.length;
        float dt = now - this.at;
        if (this.points.length != n || Float.isNaN(this.at) || dt < 0.0F || dt > 2.0F
                || this.points[0].distanceToSqr(target[0]) > LOST * LOST) {
            this.points = laid(target, target, ground);
            this.before = this.points.clone();
            this.aimed = target.clone();
            this.at = now;
            this.lastStep = 1.0F;
            return this.points.clone();
        }
        if (dt <= 0.0F) {
            return this.points.clone();
        }
        this.at = now;
        Vec3[] aim = laid(target, this.points, ground);
        float[] firm = shape.firm();
        // Frames do not all take as long: the speed a piece keeps is its last move, scaled to this frame.
        double keep = Math.pow(KEEP_SPEED, dt) * Math.min(2.0, dt / this.lastStep);
        this.lastStep = dt;
        double slide = Math.pow(GROUND_SLIDE, dt);
        this.points[0] = target[0];
        this.before[0] = target[0];
        for (int i = 1; i < n; i++) {
            Vec3 here = this.points[i];
            Vec3 speed = here.subtract(this.before[i]).scale(keep);
            if (here.y <= ground + 1.0E-3) {
                speed = new Vec3(speed.x * slide, Math.max(0.0, speed.y), speed.z * slide);
            }
            double fast = speed.length();
            if (fast > LOOSE_SPEED * dt) {
                speed = speed.scale(LOOSE_SPEED * dt / fast);
            }
            this.before[i] = here;
            double hold = Mth.clamp(firm[i], 0.0F, 1.0F);
            double kept = kept(target, i, ground);
            Vec3 free = here.add(speed).add(0.0, -FALL * (1.0 - hold * kept) * dt * dt, 0.0);
            double pull = kept * (1.0 - Math.exp(-Mth.lerp(hold, SLACK_PULL, FIRM_PULL) * dt));
            Vec3 pulled = free.lerp(aim[i], pull);
            // A piece far from its place comes back no faster than REEL on top of the way the shape moves there:
            // pulled off the ground or round from elsewhere, the lash never jumps.
            Vec3 follow = here.add(target[i].subtract(this.aimed[i]));
            double most = REEL * dt + (1.0 - pull) * free.distanceTo(follow);
            Vec3 off = pulled.subtract(follow);
            double far = off.length();
            this.points[i] = far > most ? follow.add(off.scale(most / far)) : pulled;
        }
        this.aimed = target.clone();
        // Out from the handle every piece gets back the length the shape gives it. Where the lash is held it takes the
        // shape's own way, so a taut or wound lash never lags into folds; nowhere may it bend sharper than a rope.
        Vec3 last = null;
        for (int i = 1; i < n; i++) {
            Vec3 way = this.points[i].subtract(this.points[i - 1]);
            double hold = Mth.clamp(firm[i], 0.0F, 1.0F) * kept(target, i, ground);
            if (hold > 0.0) {
                Vec3 shaped = aim[i].subtract(aim[i - 1]);
                way = WhipLash.mix(unit(way, shaped), unit(shaped, way), hold);
            }
            way = bent(last, way);
            this.points[i] = placed(this.points[i - 1], way, this.before[i].subtract(this.before[i - 1]),
                    target[i].distanceTo(target[i - 1]), ground, last);
            Vec3 laid = this.points[i].subtract(this.points[i - 1]);
            last = laid.lengthSqr() < 1.0E-12 ? last : laid.normalize();
        }
        return this.points.clone();
    }

    private static Vec3 unit(Vec3 way, Vec3 otherwise) {
        double length = way.length();
        if (length > 1.0E-9) {
            return way.scale(1.0 / length);
        }
        double other = otherwise.length();
        return other > 1.0E-9 ? otherwise.scale(1.0 / other) : new Vec3(0.0, -1.0, 0.0);
    }

    // The way turned back towards the piece before it where it bends sharper than MOST_BEND.
    private static Vec3 bent(@Nullable Vec3 before, Vec3 way) {
        if (before == null || way.lengthSqr() < 1.0E-12) {
            return way;
        }
        Vec3 to = way.normalize();
        double dot = before.dot(to);
        if (dot >= COS_BEND) {
            return way;
        }
        Vec3 side = to.subtract(before.scale(dot));
        if (side.lengthSqr() < 1.0E-8) {
            side = before.cross(Math.abs(before.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0));
        }
        return before.scale(COS_BEND).add(side.normalize().scale(SIN_BEND));
    }

    // How firmly a piece keeps to the shape: fully above the ground, not at all where the shape dives steeply into it
    // (there the rope lets go, and the lash falls and lies where it lands).
    private static double kept(Vec3[] target, int i, double ground) {
        if (target[i].y >= ground) {
            return 1.0;
        }
        Vec3 way = target[i].subtract(target[i - 1]);
        double length = way.length();
        double flat = length < 1.0E-9 ? 0.0 : Math.sqrt(way.x * way.x + way.z * way.z) / length;
        return Ease.smooth((flat - STEEP) / (0.95 - STEEP));
    }

    // The shape, but where it runs into the ground it lies on it instead, every piece as long as in the shape.
    static Vec3[] laid(Vec3[] target, Vec3[] rope, double ground) {
        Vec3[] out = new Vec3[target.length];
        out[0] = target[0];
        for (int i = 1; i < target.length; i++) {
            Vec3 way = target[i].subtract(target[i - 1]);
            out[i] = placed(out[i - 1], way, rope[i].subtract(rope[i - 1]), way.length(), ground, null);
        }
        return out;
    }

    // The piece from `from` along `way`; one that would sink into the ground lies flat on it, the way it points
    // mixed with the way it lay before, and bending no sharper there than MOST_BEND from the piece behind it.
    private static Vec3 placed(Vec3 from, Vec3 way, Vec3 before, double length, double ground,
            @Nullable Vec3 last) {
        double far = way.length();
        Vec3 end = far > 1.0E-6 ? from.add(way.scale(length / far)) : from;
        if (end.y >= ground) {
            return end;
        }
        double drop = Math.max(0.0, from.y - ground);
        double flat = Math.sqrt(Math.max(0.0, length * length - drop * drop));
        double x = way.x + LEAN * before.x;
        double z = way.z + LEAN * before.z;
        if (last != null && last.x * last.x + last.z * last.z > 1.0E-8) {
            Vec3 flatWay = bent(new Vec3(last.x, 0.0, last.z).normalize(), new Vec3(x, 0.0, z));
            x = flatWay.x;
            z = flatWay.z;
        }
        double size = Math.sqrt(x * x + z * z);
        return size < 1.0E-9 ? new Vec3(end.x, ground, end.z)
                : new Vec3(from.x + x * flat / size, ground, from.z + z * flat / size);
    }
}
