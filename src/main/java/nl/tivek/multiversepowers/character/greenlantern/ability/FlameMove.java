package nl.tivek.multiversepowers.character.greenlantern.ability;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.math.Ease;

public enum FlameMove {
    EQUIP(Kind.EQUIP, 62, 34),
    SWEEP(Kind.SWEEP, 15, 10),
    SWEEP_BACK(Kind.SWEEP, 15, 10),
    INFERNO(Kind.INFERNO, 100000, 100000),
    VENT(Kind.VENT, 16, 6),
    WALL(Kind.WALL, 22, 16),
    VORTEX(Kind.VORTEX, 100000, 100000),
    BURST(Kind.BURST, 14, 8);

    public enum Kind {
        EQUIP, SWEEP, INFERNO, VENT, WALL, VORTEX, BURST
    }

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

    public static final int SPRAY_FROM = 3;
    public static final int SPRAY_TO = 9;
    public static final double SWEEP_ARC = 55.0;
    public static final int BRACE = 4;
    public static final int LAY_FROM = 4;
    public static final int LAY_TO = 11;
    public static final int SPIN_UP = 8;
    public static final int BLAST = 2;

    public static final int FIRING = 8;
    public static final int SWIRLING = 16;
    private static final int MOVE_BITS = 7;

    private final Kind kind;
    private final int ticks;
    private final int ready;

    FlameMove(Kind kind, int ticks, int ready) {
        this.kind = kind;
        this.ticks = ticks;
        this.ready = ready;
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
    public static FlameMove byIndex(int index) {
        FlameMove[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }

    @Nullable
    public static FlameMove sent(int number) {
        return byIndex(number & MOVE_BITS);
    }

    public static FlameMove sweepAfter(@Nullable FlameMove last) {
        return last == SWEEP ? SWEEP_BACK : SWEEP;
    }

    // Degrees the nozzle points right of the look while a sweep sprays: right to left, or back again.
    public static double sweepYaw(FlameMove move, double t) {
        double side = move == SWEEP_BACK ? -1.0 : 1.0;
        double u = Ease.smooth((t - SPRAY_FROM) / (SPRAY_TO - SPRAY_FROM));
        return side * Mth.lerp(u, SWEEP_ARC, -SWEEP_ARC);
    }

    public static boolean spraying(FlameMove move, double t) {
        return move.kind == Kind.SWEEP && t >= SPRAY_FROM && t < SPRAY_TO;
    }

    public static double laid(double t) {
        return Ease.smooth((t - LAY_FROM) / (LAY_TO - LAY_FROM));
    }
}
