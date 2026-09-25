package nl.tivek.multiversepowers.character.greenlantern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * How the hands of the pair with the axe move (see {@link HandDuo}): each hand's way through the air from key to
 * key, its fingers' changes of pose with ripples and twitches on top, and the curves they all move along.
 */
abstract class HandDuoMotion {
    // The fingers: curl of index, middle, ring, little finger and thumb, then the hook of each, then the spread.
    private static final int DIGITS = 11;
    private static final int SPREAD = 10;

    /** How far behind its finger's root the two outer joints of a finger follow, in ticks. */
    private static final double HOOK_LAG = 0.5;

    static final int SMOOTH = 0;
    static final int LATE = 1;
    static final int EARLY = 2;
    static final int POP = 3;

    // ---- Turning a hand ----

    /**
     * A hand turned halfway the shortest way round from one way of standing (its fingers' way and palm) to another:
     * its fingers' way and palm, straightened out.
     */
    static Vec3[] halfway(Vec3 fromUp, Vec3 fromPalm, Vec3 toUp, Vec3 toPalm) {
        Vec3[] from = Vectors.frame(fromUp, fromPalm);
        Vec3 turn = Vectors.turn(from, Vectors.frame(toUp, toPalm)).scale(0.5);
        return new Vec3[] { Vectors.turned(from[0], turn), Vectors.turned(from[1], turn) };
    }

    // ---- The keys of the hands in the air ----

    /** A moment of a hand in the air: where its wrist is, which ways its fingers and palm point; still: at rest. */
    record Key(double t, boolean still, Vec3 at, Vec3 up, Vec3 palm) {
        /** The same for the other hand. */
        Key mirrored() {
            return new Key(this.t, this.still, this.at.multiply(-1.0, 1.0, 1.0), this.up.multiply(-1.0, 1.0, 1.0),
                    this.palm.multiply(-1.0, 1.0, 1.0));
        }
    }

    /**
     * A hand's way through the air: from key to key smoothly, passing through each at an even speed. Its wrist moves
     * along a smooth curve through the keys; the hand turns from each key's way of standing to the next the shortest
     * way round (never flipping through, however far apart they are), gathering and losing its speed of turning as
     * smoothly.
     */
    static final class Track {
        private final Key[] keys;
        // How fast the wrist moves at each key, and at each key which way the fingers point and the palm faces
        // (straightened out), how fast and about which way the hand turns there (radians a tick) and the turn to the
        // next key (see Vectors.turn).
        private final Vec3[] speeds;
        private final Vec3[][] frames;
        private final Vec3[] spins;
        private final Vec3[] turns;

        Track(Key... keys) {
            int n = keys.length;
            this.keys = keys;
            this.speeds = new Vec3[n];
            this.frames = new Vec3[n][];
            this.spins = new Vec3[n];
            this.turns = new Vec3[n];
            for (int i = 0; i < n; i++) {
                this.frames[i] = Vectors.frame(keys[i].up(), keys[i].palm());
            }
            for (int i = 0; i < n; i++) {
                this.turns[i] = i + 1 < n ? Vectors.turn(this.frames[i], this.frames[i + 1]) : Vec3.ZERO;
            }
            for (int i = 0; i < n; i++) {
                if (keys[i].still() || i == 0 || i == n - 1) {
                    this.speeds[i] = Vec3.ZERO;
                    this.spins[i] = Vec3.ZERO;
                } else {
                    double span = keys[i + 1].t() - keys[i - 1].t();
                    this.speeds[i] = keys[i + 1].at().subtract(keys[i - 1].at()).scale(1.0 / span);
                    this.spins[i] = this.turns[i - 1].add(this.turns[i]).scale(1.0 / span);
                }
            }
        }

        /** Where it is at {@code t}: wrist, fingers' way, palm (in the pair's terms; the last two one long each). */
        Vec3[] at(double t) {
            Key[] k = this.keys;
            if (t <= k[0].t()) {
                return new Vec3[] { k[0].at(), this.frames[0][0], this.frames[0][1] };
            }
            int last = k.length - 1;
            if (t >= k[last].t()) {
                return new Vec3[] { k[last].at(), this.frames[last][0], this.frames[last][1] };
            }
            int i = 0;
            while (t >= k[i + 1].t()) {
                i++;
            }
            Key a = k[i];
            Key b = k[i + 1];
            // How far it has turned from key a on its way to key b: a smooth curve from no turn at all to the whole
            // turn, leaving and reaching each key turning as fast as it passes through it.
            Vec3 turn = hermite(t, a.t(), Vec3.ZERO, this.spins[i], b.t(), this.turns[i], this.spins[i + 1]);
            return new Vec3[] { hermite(t, a.t(), a.at(), this.speeds[i], b.t(), b.at(), this.speeds[i + 1]),
                    Vectors.turned(this.frames[i][0], turn), Vectors.turned(this.frames[i][1], turn) };
        }
    }

    // ---- The fingers ----

    /**
     * How the fingers of a hand move: from a first pose, each change of pose after another, finger after finger (each
     * change adds its difference smoothly, so changes may overlap), with ripples and twitches on top.
     */
    static final class Fingers {
        private final double[] first;
        private final List<Part> parts = new ArrayList<>();
        private double[] last;

        Fingers(double[] first) {
            this.first = first;
            this.last = first;
        }

        /**
         * Change to {@code pose}, starting at {@code from} and taking {@code length} ticks, each finger (index, middle,
         * ring, little finger, thumb) {@code lags} ticks late, the outer joints of each a little later still.
         */
        Fingers to(double[] pose, double from, double length, int curve, double[] lags) {
            double[] by = new double[DIGITS];
            for (int i = 0; i < DIGITS; i++) {
                by[i] = pose[i] - this.last[i];
            }
            this.parts.add(new Change(by, from, length, curve, lags));
            this.last = pose;
            return this;
        }

        /**
         * Waves running through the fingers (index first), growing from {@code a} to {@code b} and dying away from
         * {@code c} to {@code d}: {@code speed} radians a tick, {@code step} radians from one finger to the next, how
         * far each curl and hook swings, and how far the thumb's curl swings.
         */
        Fingers wave(double a, double b, double c, double d, double speed, double step, double curl, double hook,
                double thumb) {
            this.parts.add(new Wave(a, b, c, d, speed, step, curl, hook, thumb));
            return this;
        }

        /** Middle finger and thumb pressed together, trembling a hair, from {@code from} to {@code to}. */
        Fingers tremble(double from, double to, double speed, double size) {
            this.parts.add(new Tremble(from, to, speed, size));
            return this;
        }

        /** A twitch of the fingers at {@code at}, peaking {@code rise} ticks later and dying away. */
        Fingers twitch(double at, double rise, double[] size) {
            this.parts.add(new Twitch(at, rise, size));
            return this;
        }

        /** The fingers at {@code t}, dragged by how the hand moves ({@code drag}) unless it holds on ({@code held}). */
        HandPose pose(double t, double drag, double held) {
            double[] d = this.first.clone();
            for (Part part : this.parts) {
                part.add(t, d);
            }
            HandPose pose = new HandPose();
            double free = drag * (1.0 - held);
            for (int k = 0; k < 5; k++) {
                pose.curl[k] = d[k] + free * (0.7 + 0.1 * k);
                pose.hook[k] = d[5 + k] + free * 0.8;
            }
            pose.spread = d[SPREAD];
            return pose;
        }
    }

    /** Something the fingers do, added onto their pose. */
    private interface Part {
        void add(double t, double[] digits);
    }

    /** A change of pose (see {@link Fingers#to}). */
    private record Change(double[] by, double from, double length, int curve, double[] lags) implements Part {
        @Override
        public void add(double t, double[] digits) {
            for (int i = 0; i < DIGITS; i++) {
                if (this.by[i] == 0.0) {
                    continue;
                }
                double lag = i == SPREAD ? this.lags[1] : this.lags[i % 5] + (i >= 5 ? HOOK_LAG : 0.0);
                double since = t - this.from - lag;
                digits[i] += this.by[i] * step(this.curve, since / this.length, since);
            }
        }
    }

    /** Waves running through the fingers (see {@link Fingers#wave}). */
    private record Wave(double a, double b, double c, double d, double speed, double step, double curl, double hook,
            double thumb) implements Part {
        @Override
        public void add(double t, double[] digits) {
            double grow = window(t, this.a, this.b, this.c, this.d);
            if (grow == 0.0) {
                return;
            }
            for (int k = 0; k < 5; k++) {
                double phase = this.speed * t - this.step * k;
                double size = grow * (k == 4 ? this.thumb / this.curl : 1.0);
                digits[k] += size * this.curl * Math.sin(phase);
                digits[5 + k] += size * this.hook * Math.sin(phase - 0.9);
            }
        }
    }

    /** The middle finger and thumb pressing on each other (see {@link Fingers#tremble}). */
    private record Tremble(double from, double to, double speed, double size) implements Part {
        @Override
        public void add(double t, double[] digits) {
            double press = window(t, this.from, this.from + 1.0, this.to - 0.4, this.to) * this.size;
            digits[1] += press * Math.sin(this.speed * t);
            digits[4] -= press * Math.sin(this.speed * t + 0.5);
        }
    }

    /** A twitch of the fingers (see {@link Fingers#twitch}). */
    private record Twitch(double at, double rise, double[] size) implements Part {
        @Override
        public void add(double t, double[] digits) {
            double k = kick(t - this.at, this.rise);
            if (k == 0.0) {
                return;
            }
            for (int i = 0; i < DIGITS; i++) {
                digits[i] += k * this.size[i];
            }
        }
    }

    /** A pose of the fingers: curl of index, middle, ring, little finger and thumb, their hooks, the spread. */
    static double[] digits(double... values) {
        return values;
    }

    /** A pose of the fingers with some of its numbers (index, value, index, value...) changed. */
    static double[] with(double[] pose, double... changes) {
        double[] out = pose.clone();
        for (int i = 0; i + 1 < changes.length; i += 2) {
            out[(int) changes[i]] = changes[i + 1];
        }
        return out;
    }

    // ---- Curves ----

    /** How far through a change of shape {@code curve} is at {@code u} (0 before, 1 after; {@code since} in ticks). */
    private static double step(int curve, double u, double since) {
        return switch (curve) {
            case LATE -> late(u);
            case EARLY -> early(u);
            case POP -> Ease.spring(since, 1.1, 0.42);
            default -> Ease.smoother(u);
        };
    }

    /** 0 to 1, speeding up most of the way and stopping short: a slam. */
    static double late(double u) {
        double c = Mth.clamp(u, 0.0, 1.0);
        return c * c * c * (4.0 - 3.0 * c);
    }

    /** 0 to 1, fast at first and settling slowly: a fling. */
    static double early(double u) {
        return 1.0 - late(1.0 - u);
    }

    /** 0 to 1 for the chop: slow over the top, ever faster, then stopped short as the blade bites. */
    static double chop(double u) {
        double c = Mth.clamp(u, 0.0, 1.0);
        double c4 = c * c * c * c;
        return c4 * c * (6.0 - 5.0 * c);
    }

    /** 0 before a, growing to 1 by b, 1 until c, dying back to 0 by d. */
    static double window(double t, double a, double b, double c, double d) {
        return Ease.smoother((t - a) / (b - a)) * (1.0 - Ease.smoother((t - c) / (d - c)));
    }

    /** A jolt at u = 0: rises from nothing to 1 at {@code rise}, then dies away. */
    static double kick(double u, double rise) {
        if (u <= 0.0) {
            return 0.0;
        }
        double x = u / rise;
        return x * x * Math.exp(2.0 * (1.0 - x));
    }

    /** A wobble starting at u = 0: {@code speed} radians a tick, dying away, grown in over {@code ramp} ticks. */
    static double wobble(double u, double speed, double decay, double ramp) {
        if (u <= 0.0) {
            return 0.0;
        }
        return Math.sin(speed * u) * Math.exp(-decay * u) * Ease.smoother(u / ramp);
    }

    /** A cubic from {@code a} at {@code ta} (moving {@code va} a tick) to {@code b} at {@code tb} (at {@code vb}). */
    static double hermite(double t, double ta, double a, double va, double tb, double b, double vb) {
        double span = tb - ta;
        return Ease.hermite(a, va * span, b, vb * span, (t - ta) / span);
    }

    static Vec3 hermite(double t, double ta, Vec3 a, Vec3 va, double tb, Vec3 b, Vec3 vb) {
        return new Vec3(hermite(t, ta, a.x, va.x, tb, b.x, vb.x), hermite(t, ta, a.y, va.y, tb, b.y, vb.y),
                hermite(t, ta, a.z, va.z, tb, b.z, vb.z));
    }
}
