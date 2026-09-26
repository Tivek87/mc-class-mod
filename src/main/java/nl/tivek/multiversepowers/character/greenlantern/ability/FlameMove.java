package nl.tivek.multiversepowers.character.greenlantern.ability;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// An attack's flame follows its strokes: degrees right of and up from the look, over each stroke's ticks.
public enum FlameMove {
    EQUIP(Kind.EQUIP, 62, 34),
    SWEEP(15, 10, Fire.FAN, 1.0, 1.0, 0.27, 0.225, 0.12,
            stroke(3, 9, u -> aim(swing(u, 55.0, -55.0), 0.0))),
    SWEEP_BACK(15, 10, Fire.FAN, 1.0, 1.0, 0.27, 0.225, 0.12,
            stroke(3, 9, u -> aim(swing(u, -55.0, 55.0), 0.0))),
    INFERNO(Kind.INFERNO, 100000, 100000),
    VENT(Kind.VENT, 16, 6),
    WALL(Kind.WALL, 22, 16),
    VORTEX(Kind.VORTEX, 100000, 100000),
    BURST(Kind.BURST, 14, 8),
    RISING(15, 10, Fire.FAN, 1.0, 1.0, 0.2, 0.15, 0.38,
            stroke(3, 9, u -> aim(swing(u, -45.0, 45.0), swing(u, -28.0, 32.0)))),
    CHOP(15, 10, Fire.FAN, 1.05, 1.0, 0.28, 0.15, -0.12,
            stroke(3, 9, u -> aim(swing(u, 45.0, -45.0), swing(u, 32.0, -28.0)))),
    GEYSER(15, 11, Fire.FAN, 1.1, 0.82, 0.1, 0.0, 0.72,
            stroke(4, 9, u -> aim(4.0 * Math.sin(Math.PI * u), swing(u, -50.0, 38.0)))),
    OVERHEAD(18, 13, Fire.FAN, 1.35, 1.0, 0.32, 0.0, -0.2,
            stroke(6, 10, u -> aim(swing(u, 6.0, -4.0), swing(u, 62.0, -38.0)))),
    JAB(11, 8, Fire.BALL, 0.85, 0.82, 0.5, 0.0, 0.14,
            stroke(2, 4, u -> aim(0.0, 2.0))),
    LUNGE(17, 12, Fire.JET, 1.2, 1.36, 0.55, 0.0, 0.1,
            stroke(4, 10, u -> aim(3.0 * Math.sin(Mth.TWO_PI * u), -2.0 + 2.0 * Math.sin(Math.PI * u)))),
    LOW_SWEEP(16, 12, Fire.FAN, 0.9, 0.91, 0.12, 0.25, 0.3,
            stroke(4, 10, u -> aim(swing(u, 70.0, -70.0), -30.0))),
    SPIN(19, 15, Fire.FAN, 1.2, 0.82, 0.42, 0.18, 0.15,
            stroke(4, 14, u -> aim(swing(u, 20.0, -340.0), -6.0))),
    CROSS(17, 13, Fire.FAN, 0.75, 1.0, 0.22, 0.15, 0.08,
            stroke(3, 7, u -> aim(swing(u, -48.0, 48.0), swing(u, 30.0, -24.0))),
            stroke(9, 13, u -> aim(swing(u, 48.0, -48.0), swing(u, 30.0, -24.0)))),
    SPIRAL(16, 12, Fire.FAN, 1.0, 1.1, 0.3, 0.2, 0.2,
            stroke(3, 12, FlameMove::spiral));

    public enum Kind {
        EQUIP, ATTACK, INFERNO, VENT, WALL, VORTEX, BURST
    }

    // How the flame leaves the nozzle, and how wide it reaches beside and above its aim (degrees).
    public enum Fire {
        FAN(10.0, 20.0),
        BALL(20.0, 20.0),
        JET(7.0, 9.0);

        public final double across;
        public final double high;

        Fire(double across, double high) {
            this.across = across;
            this.high = high;
        }
    }

    public record Aim(double yaw, double pitch) {
    }

    @FunctionalInterface
    public interface Path {
        Aim at(double u);
    }

    public record Stroke(float from, float to, Path path) {
        public Aim aim(double t) {
            return this.path.at(Mth.clamp((t - this.from) / (this.to - this.from), 0.0, 1.0));
        }
    }

    private static final FlameMove[] ATTACKS = { SWEEP, SWEEP_BACK, RISING, CHOP, GEYSER, OVERHEAD, JAB, LUNGE,
            LOW_SWEEP, SPIN, CROSS, SPIRAL };

    public static final int LEFT_GRAB = 15;
    public static final int VALVE = 21;
    public static final int FILLED = 29;
    public static final int LEFT_BACK = 27;
    public static final int SPARK = 30;
    public static final int SPARK_AGAIN = 32;
    public static final int PILOT = 34;
    public static final int TEST = 42;
    public static final int TEST_TICKS = 6;
    public static final int HISS = 51;

    public static final int BRACE = 4;
    public static final int LAY_FROM = 4;
    public static final int LAY_TO = 11;
    public static final int SPIN_UP = 8;
    public static final int BLAST = 2;

    public static final int FIRING = 32;
    public static final int SWIRLING = 64;
    private static final int MOVE_BITS = 31;

    private final Kind kind;
    private final int ticks;
    private final int ready;
    @Nullable
    private final Fire fire;
    private final double power;
    private final double reach;
    private final double away;
    private final double side;
    private final double lift;
    private final Stroke[] strokes;

    FlameMove(Kind kind, int ticks, int ready) {
        this.kind = kind;
        this.ticks = ticks;
        this.ready = ready;
        this.fire = null;
        this.power = 0.0;
        this.reach = 0.0;
        this.away = 0.0;
        this.side = 0.0;
        this.lift = 0.0;
        this.strokes = new Stroke[0];
    }

    // An attack: its damage and reach against the sweep's settings, and how hard it throws what it hits.
    FlameMove(int ticks, int ready, Fire fire, double power, double reach, double away, double side, double lift,
            Stroke... strokes) {
        this.kind = Kind.ATTACK;
        this.ticks = ticks;
        this.ready = ready;
        this.fire = fire;
        this.power = power;
        this.reach = reach;
        this.away = away;
        this.side = side;
        this.lift = lift;
        this.strokes = strokes;
    }

    public Kind kind() {
        return this.kind;
    }

    public int ticks() {
        return this.ticks;
    }

    public int ready() {
        return this.ready;
    }

    public Fire fire() {
        return this.fire == null ? Fire.FAN : this.fire;
    }

    public double power() {
        return this.power;
    }

    public double reach() {
        return this.reach;
    }

    public double away() {
        return this.away;
    }

    public double side() {
        return this.side;
    }

    public double lift() {
        return this.lift;
    }

    public Stroke[] strokes() {
        return this.strokes;
    }

    @Nullable
    public Aim aim(double t) {
        for (Stroke stroke : this.strokes) {
            if (t >= stroke.from() && t < stroke.to()) {
                return stroke.aim(t);
            }
        }
        return null;
    }

    public boolean spraying(double t) {
        return this.aim(t) != null;
    }

    // Ticks since the last stroke ended, or -1 before the first one is done.
    public double sinceSpray(double t) {
        double since = -1.0;
        for (Stroke stroke : this.strokes) {
            if (t >= stroke.to()) {
                since = t - stroke.to();
            }
        }
        return since;
    }

    @Nullable
    public static FlameMove byIndex(int index) {
        FlameMove[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }

    @Nullable
    public static FlameMove sent(int number) {
        return byIndex(number & MOVE_BITS);
    }

    // A different one every time, never the same twice in a row.
    public static FlameMove randomAttack(RandomSource random, @Nullable FlameMove last) {
        FlameMove move;
        do {
            move = ATTACKS[random.nextInt(ATTACKS.length)];
        } while (move == last);
        return move;
    }

    // The way the flame goes: the look turned right about the world's up, then raised about its own side.
    public static Vec3 way(Vec3 look, Aim aim) {
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-8) {
            return look;
        }
        double yaw = Math.toRadians(aim.yaw());
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        Vec3 swung = flat.scale(Math.cos(yaw)).add(right.scale(Math.sin(yaw)));
        Vec3 turned = new Vec3(swung.x, look.y, swung.z);
        if (aim.pitch() == 0.0) {
            return turned;
        }
        Vec3 side = new Vec3(-swung.z, 0.0, swung.x).normalize();
        return Vectors.spin(turned, side, Math.toRadians(aim.pitch()));
    }

    private static Stroke stroke(float from, float to, Path path) {
        return new Stroke(from, to, path);
    }

    private static Aim aim(double yaw, double pitch) {
        return new Aim(yaw, pitch);
    }

    private static double swing(double u, double from, double to) {
        return Mth.lerp(Ease.smooth(u), from, to);
    }

    // Two turns and a bit, round and round in a narrowing corkscrew.
    private static Aim spiral(double u) {
        double angle = Mth.TWO_PI * 2.2 * u;
        double wide = 24.0 * (1.0 - 0.45 * u);
        return new Aim(wide * Math.cos(angle), 0.7 * wide * Math.sin(angle));
    }

    public static double laid(double t) {
        return Ease.smooth((t - LAY_FROM) / (LAY_TO - LAY_FROM));
    }
}
