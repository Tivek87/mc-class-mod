package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;

// Every move of the energy whip and the way its lash is flung through it (see WhipLash), in degrees from the look.
public enum WhipMove {
    EQUIP(Kind.EQUIP, 62, 34, null,
            curled(0, true, 20, -50, 0, 0, 1, 0, 0, 1), curled(8, true, 20, -50, 0, 0, 1, 0, 0, 1),
            curled(15, false, 16, -52, 0, 0.55, 1, 0.1, 0, 1), curled(22, true, 14, -50, 0, 1, 1, 0.08, 0, 1),
            curled(25, false, 25, -20, 0.35, 1, 1, 0.3, 0, 0.55), curled(29, false, 55, 12, 0.8, 1, 1, 0.22, 0, 0),
            k(31, false, 60, 14, 1, 1, 1, 0.08, 0), k(34, false, -30, 10, 1, 1, 1, 0, 0),
            k(37, false, -120, 10, 1, 1, 1, 0, 0), k(40, false, -190, 22, 1, 1, 1, 0, 0),
            k(42.5F, false, -182, 110, 1, 1, 0.5, 0, 0), k(44, true, -180, 186, 1, 1.05, 0, 0, 0),
            k(47, true, -180, 200, 0.8, 1, 0.2, 0, 0), curled(51, false, -178, 208, 0.3, 1, 0.6, 0, 0, 0.35),
            curled(55, false, -172, 216, 0.1, 1, 1, 0, 0.35, 0.85), curled(58, true, -168, 220, 0, 1, 1, 0, 0.35, 1)),
    FOREHAND(Kind.ATTACK, 18, 13, strike(1.0, 0.25, 0.6, 0.15, 0).window(6, 12),
            k(3, false, 100, 6, 0.9, 0.3), k(4.5F, true, 118, 5, 1, 0.3), k(8.5F, false, -95, -2, 1, 0.3),
            k(10.5F, true, -128, -6, 0.85, 0.3), k(14, false, -70, -35, 0.2, 0.6)),
    BACKHAND(Kind.ATTACK, 18, 13, strike(1.0, 0.25, 0.6, 0.15, 0).window(6, 12),
            k(3, false, -100, 10, 0.9, 0.3), k(4.5F, true, -115, 8, 1, 0.3), k(8.5F, false, 95, -2, 1, 0.3),
            k(10.5F, true, 128, -6, 0.85, 0.3), k(14, false, 60, -35, 0.2, 0.6)),
    OVERHEAD(Kind.ATTACK, 22, 16, strike(1.4, 0.2, 0.1, -0.1, 40).window(11, 15),
            k(3, false, 12, 70, 0.8, 0), k(6, true, 8, 158, 1, 0), k(7.5F, false, 6, 150, 1, 0),
            k(10, false, 4, 60, 1, 0), k(11.5F, true, 2, -12, 1, 0), k(14, true, 0, -30, 0.7, 0),
            k(18, false, 5, -45, 0.1, 0.5)),
    SIDEARM(Kind.ATTACK, 20, 14, strike(1.1, 0.55, 0.2, 0.15, 0).window(9, 13),
            k(3, false, 95, 10, 0.85, 0.5), k(5.5F, true, 150, 12, 1, 0.5), k(8, false, 70, 4, 1, 0.3),
            k(9.5F, true, 0, 0, 1, 1.1, 0, 0, 0), k(12, true, -8, -6, 0.9, 1.05, 0, 0, 0),
            k(15, false, -10, -35, 0.2, 0.5)),
    RISING(Kind.ATTACK, 19, 14, strike(1.1, 0.2, 0.15, 0.75, 0).window(7, 13),
            k(3, false, 70, -35, 0.8, 0.2), k(5, true, 95, -55, 1, 0.2), k(9, false, -30, 45, 1, 0),
            k(10.5F, true, -45, 70, 0.9, 0), k(14, false, -20, 10, 0.3, 0.4)),
    CLEAVE(Kind.ATTACK, 19, 14, strike(1.15, 0.25, 0.45, -0.05, 0).window(7, 13),
            k(3, false, 55, 60, 0.85, 0), k(5, true, 75, 95, 1, 0), k(9, false, -45, -30, 1, 0),
            k(10.5F, true, -70, -48, 0.9, 0), k(14, false, -40, -45, 0.2, 0.4)),
    REVERSE_CLEAVE(Kind.ATTACK, 19, 14, strike(1.15, 0.25, 0.45, -0.05, 0).window(7, 13),
            k(3, false, -55, 60, 0.85, 0), k(5, true, -75, 95, 1, 0), k(9, false, 45, -30, 1, 0),
            k(10.5F, true, 70, -48, 0.9, 0), k(14, false, 40, -45, 0.2, 0.4)),
    FIGURE_EIGHT(Kind.ATTACK, 24, 18, strike(0.8, 0.2, 0.4, 0.1, 0).window(5, 10).window(12, 17),
            k(2.5F, false, 50, 55, 0.85, 0), k(4, true, 60, 80, 1, 0), k(7, false, -40, -30, 1, 0),
            k(8.5F, false, -70, 5, 1, 0), k(10.5F, true, -60, 75, 1, 0), k(13.5F, false, 40, -30, 1, 0),
            k(15, true, 65, -45, 0.9, 0), k(19, false, 30, -40, 0.2, 0.4)),
    LEG_SWEEP(Kind.ATTACK, 20, 15, strike(0.9, -0.25, 0.35, 0.25, 50).window(7, 14),
            k(3, false, 90, -15, 0.85, 1), k(5, true, 125, -18, 1, 1), k(10, false, -95, -22, 1, 1),
            k(12, true, -125, -24, 0.85, 1), k(15, false, -60, -40, 0.2, 1)),
    SPIN(Kind.ATTACK, 24, 18, strike(1.15, 0.5, 0.4, 0.2, 0).window(6, 17),
            k(3, false, 100, 0, 0.9, 0.8), k(5, false, 150, -2, 1, 0.8), k(8, false, 20, -4, 1, 0.8),
            k(11, false, -150, -6, 1, 0.8), k(14, false, -320, -6, 1, 0.8), k(15.5F, true, -385, -8, 0.85, 0.8),
            k(19, false, -330, -40, 0.2, 0.9)),
    SNAP(Kind.ATTACK, 13, 9, strike(0.85, 0.45, 0.1, 0.1, 0).window(6, 10),
            k(2, false, 20, 55, 0.8, 0), k(3.5F, true, 18, 95, 1, 0), k(6, true, 3, 2, 1, 1.1, 0, 0, 0),
            k(9, false, 0, -20, 0.6, 0), k(11, false, 5, -40, 0.15, 0.4)),
    COWBOY(Kind.ATTACK, 28, 21, strike(1.3, 0.5, 0.1, 0.2, 0).window(13, 18),
            k(3, false, 70, 16, 0.85, 1), k(6, false, -30, 10, 1, 1), k(9, false, -130, 10, 1, 1),
            k(11.5F, false, -195, 26, 1, 1), k(13, false, -182, 110, 1, 1, 0.5, 0, 0),
            k(14.5F, true, -180, 186, 1, 1.05, 0, 0, 0), k(17, true, -180, 200, 0.8, 1, 0.2, 0, 0),
            k(21, false, -178, 218, 0.2, 1, 0.6, 0, 0)),
    WHIRL(Kind.WHIRL, 100000, 100000, null),
    WHIRL_CRACK(Kind.WHIRL_CRACK, 22, 16, strike(1.0, 0.8, 0.0, 0.3, 0).window(10, 16)),
    LASSO(Kind.LASSO, 36, 30, null,
            k(2, false, 25, 70, 0.85, 0), k(4, true, 15, 150, 1, 0), k(6.5F, false, 5, 70, 1, 1.4, 0, 0, 0),
            k(8, true, 0, 0, 1, 2.2, 0, 0, 0), k(11, true, 0, -4, 1, 2.2, 0, 0, 0),
            k(16, false, 0, -8, 0.8, 1.6, 0, 0, 0), k(23, false, 5, -30, 0.4, 1.0, 0.4, 0, 0),
            k(28, false, 10, -40, 0.1, 1.0, 0.8, 0, 0.3)),
    SPIN_SHIELD(Kind.SPIN, 100000, 100000, null),
    SPIN_END(Kind.SPIN_END, 12, 7, null);

    public enum Kind {
        EQUIP, ATTACK, WHIRL, WHIRL_CRACK, LASSO, SPIN, SPIN_END
    }

    // The equip: the handle forms, the lash pours out and coils, rises, twirls overhead and cracks.
    public static final int FORMED = 8;
    public static final int POURED = 22;
    public static final int RISE = 24;
    public static final int TWIRL = 31;
    public static final int THROW = 42;

    public static final int WHIRL_UP = 8;
    public static final int WHIRL_EVERY = 4;
    public static final double WHIRL_TURN = Math.toRadians(-36.0);
    public static final double WHIRL_PITCH = Math.toRadians(-10.0);
    private static final double WHIRL_START = Math.toRadians(60.0);
    private static final float CRACK_TURN = 8.0F;
    private static final float CRACK_THROW = 12.0F;

    public static final int SPIN_UP = 6;
    public static final int SPIN_GUARD = 3;
    public static final double SPIN_TURN = Math.toRadians(-50.0);
    public static final double SPIN_CONE = Math.toRadians(72.0);
    public static final float SPIN_REACH = 0.4F;

    public static final int LASSO_REACH = 8;
    public static final int LASSO_WRAPPED = 14;
    public static final int LASSO_GRAB = 15;
    public static final int LASSO_HAUL = 16;
    public static final int LASSO_LAND = 23;

    public static final int WHIRLING = 32;
    public static final int SPINNING = 64;
    private static final int MOVE_BITS = 31;

    // At rest the lash hangs curled up in loops below the handle.
    public static final float[] REST = aim(12, -40, 0, 1, 1, 0, 0.35, 1);

    private static final WhipMove[] ATTACKS = { FOREHAND, BACKHAND, OVERHEAD, SIDEARM, RISING, CLEAVE, REVERSE_CLEAVE,
            FIGURE_EIGHT, LEG_SWEEP, SPIN, SNAP, COWBOY };

    public record Strike(double power, double away, double side, double lift, int slow, float[][] windows) {
    }

    // One crack of the tip: when it snaps, and how hard (1 is the hardest crack a whip makes).
    public record Crack(float tick, float strength) {
    }

    private final Kind kind;
    private final int ticks;
    private final int ready;
    @Nullable
    private final Strike strike;
    @Nullable
    private final Keyframes.Key[] lash;
    private Crack[] cracks = new Crack[0];

    WhipMove(Kind kind, int ticks, int ready, @Nullable Strikes strike, Keyframes.Key... lash) {
        this.kind = kind;
        this.ticks = ticks;
        this.ready = ready;
        this.strike = strike == null ? null : strike.build();
        this.lash = lash.length == 0 ? null : lash;
    }

    static {
        for (WhipMove move : values()) {
            move.cracks = findCracks(move);
        }
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

    @Nullable
    public Strike strike() {
        return this.strike;
    }

    @Nullable
    public Keyframes.Key[] lash() {
        return this.lash;
    }

    public Crack[] cracks() {
        return this.cracks;
    }

    public boolean held() {
        return this.kind == Kind.WHIRL || this.kind == Kind.SPIN;
    }

    // The lash of this move at its own time t, without what came before: the server's whip, and the client's once
    // the move has blended in. Before is how long the whirl or spin ran that this move ends.
    public float[] aim(double t, double before) {
        return switch (this.kind) {
            case WHIRL -> whirl(t);
            case WHIRL_CRACK -> whirlCrack(t, whirlYaw(before));
            case SPIN -> spin(t);
            case SPIN_END -> spinEnd(t, spinTurn(before));
            default -> this.lash == null ? REST.clone() : Keyframes.at(this.lash, (float) t);
        };
    }

    public static double whirlYaw(double t) {
        double up = WHIRL_UP;
        double turned = t < up ? t * t / (2.0 * up) : t - up / 2.0;
        return WHIRL_START + WHIRL_TURN * turned;
    }

    private static float[] whirl(double t) {
        return raw((float) whirlYaw(t), (float) WHIRL_PITCH, (float) Ease.smooth(t / 5.0), 1.0F, 1.0F, 0.0F, 0.0F);
    }

    // Let go, the lash goes on round until it is behind, faster or slower as it needs, and is thrown over the top.
    private static float[] whirlCrack(double t, double released) {
        double behind = Math.PI + Math.floor((released - Math.PI) / Mth.TWO_PI) * Mth.TWO_PI;
        double left = released - behind;
        if (left < Math.toRadians(120.0)) {
            left += Mth.TWO_PI;
        }
        double start = -WHIRL_TURN * CRACK_TURN / left;
        double yaw = released - left * Ease.hermite(0.0, start, 1.0, 0.0, Math.min(1.0, t / CRACK_TURN));
        if (t <= CRACK_TURN) {
            double rise = Ease.smooth(t / CRACK_TURN);
            return raw((float) yaw, (float) Mth.lerp(rise, WHIRL_PITCH, Math.toRadians(35.0)), 1.0F, 1.0F, 1.0F,
                    0.0F, 0.0F);
        }
        double u = Math.min(1.0, (t - CRACK_TURN) / (CRACK_THROW - CRACK_TURN));
        double thrown = Ease.smooth(u);
        if (t <= CRACK_THROW) {
            return raw((float) yaw, (float) Math.toRadians(Mth.lerp(thrown, 35.0, 186.0)), 1.0F, 1.05F,
                    (float) (1.0 - thrown), 0.0F, 0.0F);
        }
        double after = t - CRACK_THROW;
        double drop = Ease.smooth(after / 8.0);
        return raw((float) yaw, (float) Math.toRadians(186.0 + 36.0 * drop), (float) (1.0 - 0.9 * drop), 1.0F,
                (float) (0.7 * drop), 0.0F, 0.0F);
    }

    public static double spinTurn(double t) {
        double up = SPIN_UP;
        return SPIN_TURN * (t < up ? t * t / (2.0 * up) : t - up / 2.0);
    }

    private static float[] spin(double t) {
        return spinning(spinTurn(t), SPIN_REACH, (float) Ease.smooth(t / 3.0));
    }

    private static float[] spinEnd(double t, double released) {
        double slow = Math.min(t, 8.0);
        double turn = released + SPIN_TURN * (slow - slow * slow / 16.0);
        double drop = Ease.smooth(t / 9.0);
        float[] aim = spinning(turn, (float) Mth.lerp(drop, SPIN_REACH, 1.0), (float) (1.0 - drop));
        aim[WhipLash.PITCH] -= (float) (drop * Math.toRadians(40.0));
        return aim;
    }

    // Round the look like a propeller: the lash on a wide cone about the way ahead.
    private static float[] spinning(double turn, float reach, float taut) {
        Vec3 way = new Vec3(Math.sin(SPIN_CONE) * Math.cos(turn), Math.sin(SPIN_CONE) * Math.sin(turn),
                Math.cos(SPIN_CONE));
        return raw((float) Math.atan2(way.x, way.z), (float) Math.asin(way.y), taut, reach, 0.0F, 0.0F, 0.0F);
    }

    // The moments the tip snaps: as a real whip does, where the flick has run out to the tip and the lash
    // straightens, so where the flung tip is stopped hardest in each window. Worked out once, from the look.
    private static Crack[] findCracks(WhipMove move) {
        Strike strike = move.strike;
        float[][] windows = strike != null ? strike.windows()
                : move == EQUIP ? new float[][] { { THROW, THROW + 6 } } : new float[0][];
        List<Crack> found = new ArrayList<>();
        WhipLash.Look look = WhipLash.Look.of(0.0F, 0.0F);
        WhipLash.Aims aims = t -> move.aim(Math.max(0.0, t), 0.0);
        double step = 0.05;
        int gap = 6;
        for (float[] window : windows) {
            List<Double> speeds = new ArrayList<>();
            Vec3 last = null;
            double from = window[0] - 1.0;
            for (double t = from; t <= window[1] + 2.0; t += step) {
                Vec3[] points = WhipLash.shape(Vec3.ZERO, null, look, 1.0, aims, t, 16, null);
                Vec3 tip = points[points.length - 1];
                speeds.add(last == null ? 0.0 : tip.distanceTo(last) / step);
                last = tip;
            }
            double best = 0.0;
            double bestAt = window[1];
            double fastest = 0.0;
            for (int i = gap; i + gap < speeds.size(); i++) {
                double t = from + i * step;
                if (t < window[0] || t > window[1] + 1.0) {
                    continue;
                }
                double stopped = speeds.get(i - gap) - speeds.get(i + gap);
                if (stopped > best) {
                    best = stopped;
                    bestAt = t;
                    fastest = speeds.get(i - gap);
                }
            }
            found.add(new Crack((float) bestAt, (float) Mth.clamp(fastest / 1.4, 0.3, 1.0)));
        }
        return found.toArray(Crack[]::new);
    }

    public static WhipMove randomAttack(RandomSource random, @Nullable WhipMove last) {
        WhipMove move;
        do {
            move = ATTACKS[random.nextInt(ATTACKS.length)];
        } while (move == last);
        return move;
    }

    @Nullable
    public static WhipMove byIndex(int index) {
        WhipMove[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }

    @Nullable
    public static WhipMove sent(int number) {
        return byIndex(number & MOVE_BITS);
    }

    public static float[] aim(double yaw, double pitch, double taut, double reach, double level, double wave,
            double coil, double curl) {
        return new float[] { (float) Math.toRadians(yaw), (float) Math.toRadians(pitch), (float) taut, (float) reach,
                (float) level, (float) wave, (float) coil, (float) curl };
    }

    private static float[] raw(float yaw, float pitch, float taut, float reach, float level, float wave, float coil) {
        return new float[] { yaw, pitch, taut, reach, level, wave, coil, 0.0F };
    }

    private static Keyframes.Key k(float tick, boolean stop, double yaw, double pitch, double taut, double level) {
        return new Keyframes.Key(tick, stop, aim(yaw, pitch, taut, 1.0, level, 0.0, 0.0, 0.0));
    }

    private static Keyframes.Key k(float tick, boolean stop, double yaw, double pitch, double taut, double reach,
            double level, double wave, double coil) {
        return new Keyframes.Key(tick, stop, aim(yaw, pitch, taut, reach, level, wave, coil, 0.0));
    }

    private static Keyframes.Key curled(float tick, boolean stop, double yaw, double pitch, double taut, double reach,
            double level, double wave, double coil, double curl) {
        return new Keyframes.Key(tick, stop, aim(yaw, pitch, taut, reach, level, wave, coil, curl));
    }

    private static Strikes strike(double power, double away, double side, double lift, int slow) {
        return new Strikes(power, away, side, lift, slow);
    }

    // Builds a move's strike while the enum is made: its windows are the ticks the lash can hit in.
    static final class Strikes {
        private final double power;
        private final double away;
        private final double side;
        private final double lift;
        private final int slow;
        private final List<float[]> windows = new ArrayList<>();

        Strikes(double power, double away, double side, double lift, int slow) {
            this.power = power;
            this.away = away;
            this.side = side;
            this.lift = lift;
            this.slow = slow;
        }

        Strikes window(float from, float to) {
            this.windows.add(new float[] { from, to });
            return this;
        }

        Strike build() {
            return new Strike(this.power, this.away, this.side, this.lift, this.slow,
                    this.windows.toArray(float[][]::new));
        }
    }
}
