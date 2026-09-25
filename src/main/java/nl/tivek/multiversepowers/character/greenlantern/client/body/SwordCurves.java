package nl.tivek.multiversepowers.character.greenlantern.client.body;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The poses of the sword and shield (see {@link SwordPoses}), and the smooth curves a move runs along through its key
 * poses; last, the ways everything turns by.
 */
abstract class SwordCurves {
    /**
     * One pose, as your own eyes see it (x to the right, y up, -z ahead, in blocks): where the sword hand grips, where
     * the blade points and where its edge faces (one long, square to each other), where the middle of the shield is,
     * where its face points and where its top is; how far the upper body turns to the right (twist, radians), bends
     * forward (lean, 0 to 1, a little back below 0) and has stepped forward (step, 0 to 1); how far the whole body has
     * spun round to the left (orbit, radians), which in first person takes everything round with it; and how far the
     * arms are the game's own empty hands (rest, 0 to 1).
     */
    record Pose(Vec3 hand, Vec3 blade, Vec3 edge, Vec3 shield, Vec3 face, Vec3 top, float twist, float lean,
            float step, float orbit, float rest) {
        static final int SIZE = 23;
        // Where the numbers of the blade are in numbers(), and of its edge; those of the shield (its middle, face and
        // top); those of the body; its spin; and how far the arms rest.
        static final int BLADE = 3;
        static final int EDGE = 6;
        static final int SHIELD_FROM = 9;
        static final int SHIELD_TO = 17;
        static final int BODY_FROM = 18;
        static final int ORBIT = 21;
        static final int REST = 22;

        float[] numbers() {
            return new float[] { (float) this.hand.x, (float) this.hand.y, (float) this.hand.z, (float) this.blade.x,
                    (float) this.blade.y, (float) this.blade.z, (float) this.edge.x, (float) this.edge.y,
                    (float) this.edge.z, (float) this.shield.x, (float) this.shield.y, (float) this.shield.z,
                    (float) this.face.x, (float) this.face.y, (float) this.face.z, (float) this.top.x, (float) this.top.y,
                    (float) this.top.z, this.twist, this.lean, this.step, this.orbit, this.rest };
        }

        /** A pose from its numbers; the ways are made one long and square to each other again. */
        static Pose of(float[] n) {
            Vec3 blade = unit(new Vec3(n[3], n[4], n[5]), new Vec3(0.0, 1.0, 0.0));
            Vec3 edge = square(new Vec3(n[6], n[7], n[8]), blade);
            Vec3 face = unit(new Vec3(n[12], n[13], n[14]), new Vec3(0.0, 0.0, -1.0));
            Vec3 top = square(new Vec3(n[15], n[16], n[17]), face);
            return new Pose(new Vec3(n[0], n[1], n[2]), blade, edge, new Vec3(n[9], n[10], n[11]), face, top, n[18],
                    n[19], n[20], n[21], n[22]);
        }

        Pose mix(Pose to, float t) {
            float[] a = this.numbers();
            float[] b = aligned(to.numbers(), a);
            for (int k = 0; k < a.length; k++) {
                a[k] = Mth.lerp(t, a[k], b[k]);
            }
            return of(a);
        }

        /** The same pose with the shield of {@code other}, {@code t} of the way. */
        Pose shieldOf(Pose other, float t) {
            if (t <= 0.0F) {
                return this;
            }
            float[] a = this.numbers();
            float[] b = other.numbers();
            for (int k = SHIELD_FROM; k <= SHIELD_TO; k++) {
                a[k] = Mth.lerp(t, a[k], b[k]);
            }
            return of(a);
        }

        /** The same pose with the blade pointing along {@code blade} and its edge towards {@code edge}. */
        Pose holding(Vec3 blade, Vec3 edge) {
            return new Pose(this.hand, blade, edge, this.shield, this.face, this.top, this.twist, this.lean, this.step,
                    this.orbit, this.rest);
        }

        /** The same pose with the fist at {@code hand}. */
        Pose gripping(Vec3 hand) {
            return new Pose(hand, this.blade, this.edge, this.shield, this.face, this.top, this.twist, this.lean,
                    this.step, this.orbit, this.rest);
        }

        /** The same pose with its twist and its spin brought back to within half a turn: a whole turn is no turn. */
        Pose unwound() {
            return new Pose(this.hand, this.blade, this.edge, this.shield, this.face, this.top, wrap(this.twist),
                    this.lean, this.step, wrap(this.orbit), this.rest);
        }

        /** The same pose turned {@code angle} (radians) to the left about the upright line through your eyes. */
        Pose turned(double angle) {
            if (angle == 0.0) {
                return this;
            }
            return new Pose(spin(this.hand, angle), spin(this.blade, angle), spin(this.edge, angle),
                    spin(this.shield, angle), spin(this.face, angle), spin(this.top, angle), this.twist, this.lean,
                    this.step, this.orbit, this.rest);
        }

        /** The shield's own right: along it the forearm lies on its back, from the elbow to the fist. */
        Vec3 shieldRight() {
            return this.face.cross(this.top).normalize();
        }

        /** Where the fist grips the shield: the grip on its back, at a shield of scale {@code scale}. */
        Vec3 shieldGrip(double scale) {
            return this.shield.add(this.shieldRight().scale(SwordPainter.GRIP_X * scale))
                    .add(this.face.scale(SwordPainter.GRIP_Z * scale));
        }
    }

    /**
     * One key pose of a move: on its own tick, whether the move stops there a moment (a windup before a strike), and
     * how fast each number reaches it ({@code in}) and leaves it ({@code out}), per tick; null, or a number that is not
     * a number, for the speed the keys round it give.
     */
    record Key(float tick, boolean stop, float[] numbers, @Nullable float[] in, @Nullable float[] out) {
        Key(float tick, boolean stop, float[] numbers) {
            this(tick, stop, numbers, null, null);
        }
    }

    /** The keys of one part of a move: for the numbers from {@code from} up to (not with) {@code to}. */
    record Track(int from, int to, Key[] keys) {
        float last() {
            return this.keys[this.keys.length - 1].tick();
        }
    }

    /**
     * The pose {@code t} ticks into a move of keys: along smooth curves, one per track, through {@code from} (at tick
     * 0), every key, and {@code rest} once the last key of the track is {@code settle} ticks past. Each key is passed
     * at the speed it says itself, or else at the speed the keys round it give (the way from the one before to the one
     * after), so nothing stops on a key but the stops, the start (unless the arms were moving then) and the end.
     */
    static Pose keyed(@Nullable Track[] tracks, float t, Pose from, @Nullable float[] fromSpeed, Pose rest,
            float settle) {
        if (tracks == null || tracks.length == 0) {
            return rest;
        }
        float[] restNumbers = rest.numbers();
        float[] out = restNumbers.clone();
        float time = Math.max(0.0F, t);
        for (Track track : tracks) {
            Key[] keys = track.keys();
            float[] ends = restNumbers.clone();
            if (track.from() <= Pose.ORBIT && track.to() > Pose.ORBIT) {
                // A move that spun the body round ends a whole number of turns further: that is where it rests.
                ends[Pose.ORBIT] += Math.round(keys[keys.length - 1].numbers()[Pose.ORBIT] / Mth.TWO_PI) * Mth.TWO_PI;
            }
            float end = track.last() + settle;
            if (time >= end) {
                if (track.from() <= Pose.EDGE && track.to() > Pose.EDGE) {
                    // Rest on whichever edge the blade came round to (both are the same).
                    float[] edge = from.numbers();
                    for (Key key : keys) {
                        edge = aligned(key.numbers(), edge);
                    }
                    ends = aligned(ends, edge);
                }
                System.arraycopy(ends, track.from(), out, track.from(), track.to() - track.from());
                continue;
            }
            boolean fromStart = keys[0].tick() > 0.0F;
            Key[] line = new Key[keys.length + (fromStart ? 2 : 1)];
            int i = 0;
            if (fromStart) {
                line[i++] = new Key(0.0F, fromSpeed == null, from.numbers(), null, fromSpeed);
            }
            for (Key key : keys) {
                line[i++] = key;
            }
            line[i] = new Key(end, true, ends);
            if (track.from() <= Pose.EDGE && track.to() > Pose.EDGE) {
                for (int k = 1; k < line.length; k++) {
                    line[k] = aligned(line[k], line[k - 1].numbers());
                }
            }
            curve(line, time, track.from(), track.to(), out);
        }
        return Pose.of(out);
    }

    /** The key with the blade's edge turned over to the other edge if it faces away from the one in {@code to}. */
    private static Key aligned(Key key, float[] to) {
        float[] numbers = aligned(key.numbers(), to);
        return numbers == key.numbers() ? key
                : new Key(key.tick(), key.stop(), numbers, flipped(key.in()), flipped(key.out()));
    }

    /** Speeds with those of the blade's edge the other way round. */
    @Nullable
    private static float[] flipped(@Nullable float[] speeds) {
        if (speeds == null) {
            return null;
        }
        float[] out = speeds.clone();
        for (int c = Pose.EDGE; c < Pose.EDGE + 3; c++) {
            out[c] = -out[c];
        }
        return out;
    }

    /**
     * {@code numbers} of a pose with the blade's edge turned over to the other edge if it faces away from the edge in
     * {@code to}. The blade is the same on both edges and both flats, so that changes nothing you see, but going from the
     * one pose to the other it now only turns as far as it has to, never half round through nothing.
     */
    private static float[] aligned(float[] numbers, float[] to) {
        int e = Pose.EDGE;
        if (numbers[e] * to[e] + numbers[e + 1] * to[e + 1] + numbers[e + 2] * to[e + 2] >= 0.0F) {
            return numbers;
        }
        float[] out = numbers.clone();
        for (int c = e; c < e + 3; c++) {
            out[c] = -out[c];
        }
        return out;
    }

    /**
     * Numbers {@code from} up to {@code to} of a smooth curve through keys, at tick {@code t}, into {@code out}:
     * between two keys a cubic that leaves the first and reaches the second at the speed each says, or else the speed
     * the keys round it give (zero at a stop and at both ends).
     */
    private static void curve(Key[] keys, float t, int from, int to, float[] out) {
        int n = keys.length;
        int i = 0;
        while (i + 2 < n && keys[i + 1].tick() <= t) {
            i++;
        }
        float t0 = keys[i].tick();
        float t1 = keys[i + 1].tick();
        float h = Math.max(1.0E-3F, t1 - t0);
        float s = Mth.clamp((t - t0) / h, 0.0F, 1.0F);
        float s2 = s * s;
        float s3 = s2 * s;
        float h00 = 2.0F * s3 - 3.0F * s2 + 1.0F;
        float h10 = s3 - 2.0F * s2 + s;
        float h01 = -2.0F * s3 + 3.0F * s2;
        float h11 = s3 - s2;
        float[] a = keys[i].numbers();
        float[] b = keys[i + 1].numbers();
        for (int c = from; c < to; c++) {
            float m0 = slope(keys, i, c, keys[i].out());
            float m1 = slope(keys, i + 1, c, keys[i + 1].in());
            out[c] = h00 * a[c] + h10 * h * m0 + h01 * b[c] + h11 * h * m1;
        }
    }

    /**
     * How fast number {@code c} runs through key {@code i}: as {@code said} says, or else from the key before to the
     * one after; zero at a stop and at both ends.
     */
    private static float slope(Key[] keys, int i, int c, @Nullable float[] said) {
        if (said != null && !Float.isNaN(said[c])) {
            return said[c];
        }
        if (keys[i].stop() || i == 0 || i == keys.length - 1) {
            return 0.0F;
        }
        return (keys[i + 1].numbers()[c] - keys[i - 1].numbers()[c])
                / Math.max(1.0E-3F, keys[i + 1].tick() - keys[i - 1].tick());
    }

    // ---- Ways ----

    /** {@code way} made one long, or {@code otherwise} when it has hardly any length. */
    private static Vec3 unit(Vec3 way, Vec3 otherwise) {
        double length = way.length();
        return length < 1.0E-4 ? otherwise : way.scale(1.0 / length);
    }

    /** {@code way} with the part along {@code axis} taken out, made one long: square to that axis. */
    static Vec3 square(Vec3 way, Vec3 axis) {
        Vec3 flat = way.subtract(axis.scale(way.dot(axis)));
        if (flat.lengthSqr() < 1.0E-6) {
            Vec3 other = Math.abs(axis.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            flat = other.subtract(axis.scale(other.dot(axis)));
        }
        return flat.normalize();
    }

    /** {@code way} turned {@code angle} (radians) to the left about the upright line: +x goes towards -z. */
    static Vec3 spin(Vec3 way, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(way.x * cos + way.z * sin, way.y, -way.x * sin + way.z * cos);
    }

    /** The turn (a way times its angle, radians) that brings the way {@code from} round onto the way {@code to}. */
    static Vec3 turnOnto(Vec3 from, Vec3 to) {
        Vec3 axis = from.cross(to);
        double angle = Math.atan2(axis.length(), from.dot(to));
        return axis.lengthSqr() < 1.0E-12 ? Vec3.ZERO : axis.normalize().scale(angle);
    }

    /** {@code way} turned by {@code turn} (a way times its angle, radians). */
    static Vec3 turnedBy(Vec3 way, Vec3 turn) {
        double angle = turn.length();
        return angle < 1.0E-9 ? way : Vectors.spin(way, turn.scale(1.0 / angle), angle);
    }

    private static float wrap(float radians) {
        return Mth.wrapDegrees(radians * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }
}
