package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.math.Ease;

public final class ExpressScript {
    public static final int RUN = 0;
    public static final int BRAKE = 1;
    public static final int TIP = 2;
    public static final int SLIDE = 3;
    public static final int STEAM = 4;
    public static final int BOOM = 5;
    public static final int PHASE_BITS = 7;
    // The variant: the phase, then the side it falls to, whether it just falls apart, and how many it has rammed.
    public static final int FALLS_RIGHT = 8;
    public static final int QUIET = 16;
    public static final int RAM_SHIFT = 5;

    public static final double SCALE = 1.25;
    public static final int OPEN_TICKS = 8;
    public static final int ROLLS = 8;
    public static final int SPEED_UP = 44;
    public static final double TOP_SPEED = 0.95;
    public static final int BRAKE_TICKS = 14;
    public static final int TIP_TICKS = 12;
    public static final int STEAM_TICKS = 36;
    public static final int BOOM_TICKS = 16;
    public static final int TENDER_LAG = 3;

    // Distances back from the nose along the rails, in model blocks (times SCALE in the world).
    public static final double BOGIE = 2.4;
    public static final double DRIVER = 8.0;
    public static final double TENDER_FRONT = 12.8;
    public static final double TENDER_BACK = 17.2;
    public static final double TENDER_MIDDLE = 15.0;
    public static final double LENGTH = 18.5;
    public static final double DRIVER_RADIUS = 1.2;
    public static final double LEAD_RADIUS = 0.55;
    public static final double TENDER_WHEEL = 0.5;
    // A chuff for every quarter turn of the driving wheels.
    public static final double CHUFF_BLOCKS = Math.PI * 2.0 * DRIVER_RADIUS * SCALE / 4.0;
    public static final double FUNNEL_BACK = 1.55;
    public static final double FUNNEL_UP = 5.3;
    public static final double BOILER_BACK = 4.7;
    public static final double BOILER_UP = 2.75;
    // Where a lying engine rests: tipped over the outer edge of its running boards.
    public static final double PIVOT_OUT = 1.5;
    // The rails run this far above the ground, so the sleepers rest on it.
    public static final double RAIL_LIFT = 0.3;

    public static final double PORTAL_RADIUS = 3.4;
    public static final double PORTAL_UP = 2.7;

    private ExpressScript() {
    }

    public static int phase(int variant) {
        return variant & PHASE_BITS;
    }

    public static int side(int variant) {
        return (variant & FALLS_RIGHT) != 0 ? 1 : -1;
    }

    public static int rams(int variant) {
        return variant >>> RAM_SHIFT;
    }

    public static boolean quiet(int variant) {
        return (variant & QUIET) != 0;
    }

    public static int variant(int phase, int side, int rams) {
        return phase | (side > 0 ? FALLS_RIGHT : 0) | rams << RAM_SHIFT;
    }

    public static double speedAt(double sinceRolling) {
        return TOP_SPEED * Ease.smooth(sinceRolling / SPEED_UP);
    }

    public static double roll(double sinceTip) {
        if (sinceTip <= 0.0) {
            return 0.0;
        }
        double u = sinceTip / TIP_TICKS;
        if (u < 1.0) {
            return Math.PI * 0.5 * Math.pow(u, 2.4);
        }
        double after = sinceTip - TIP_TICKS;
        return Math.PI * 0.5 - 0.16 * Math.exp(-after * 0.35) * Math.abs(Math.sin(after * 0.9));
    }

    public static double portalOpen(double age, double closesAt) {
        double open = Ease.smooth(age / OPEN_TICKS);
        return closesAt < 0.0 ? open : open * (1.0 - Ease.smooth((age - closesAt) / OPEN_TICKS));
    }

    public static double pressure(double sinceSteam) {
        return Mth.clamp(sinceSteam / STEAM_TICKS, 0.0, 1.0);
    }
}
