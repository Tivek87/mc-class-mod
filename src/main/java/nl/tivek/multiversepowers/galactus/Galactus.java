package nl.tivek.multiversepowers.galactus;

import net.minecraft.util.Mth;

// Galactus in the sky (`/galactus`): a giant far bigger than the world, played out in real time for every player in
// the overworld. He comes in out of deep space, a speck by the moon growing until he fills half the sky, raises his
// arm (RAISE to REACHED), pokes the moon with a fingertip as big as it (TOUCH), draws back (LET_GO, BACK), lowers his
// arm (LOWERED) and goes back out (GONE). One timeline for server and clients, in ticks at speed 1; how he moves in
// between is the client's (`client/GalactusMotion`).
public final class Galactus {
    public static final int RAISE = 1500;
    public static final int REACHED = 1800;
    public static final int TOUCH = 1830;
    // He keeps his fingertip on the moon for five seconds before he lets go.
    public static final int LET_GO = 1930;
    public static final int BACK = 1960;
    public static final int LOWERED = 2040;
    public static final int GONE = 2840;
    // How many times further off than at the poke he is while still a speck.
    public static final double FAR = 400.0;

    private Galactus() {
    }

    // How many times further off than at the poke he is at `t`: in from far away, slowing as he comes, and out again.
    public static double far(double t) {
        if (t < REACHED) {
            double u = Mth.clamp(t / REACHED, 0.0, 1.0);
            return Math.exp(Math.log(FAR) * Math.pow(1.0 - u, 1.4));
        }
        if (t < LOWERED) {
            return 1.0;
        }
        double v = Mth.clamp((t - LOWERED) / (double) (GONE - LOWERED), 0.0, 1.0);
        return Math.exp(Math.log(FAR) * Math.pow(v, 1.6));
    }

    // How far the poke has gone in, 1 with the fingertip on the moon.
    public static double poke(double t) {
        if (t < REACHED || t >= BACK) {
            return 0.0;
        }
        if (t < TOUCH) {
            double u = (t - REACHED) / (TOUCH - REACHED);
            return u * u;
        }
        if (t < LET_GO) {
            return 1.0;
        }
        return 1.0 - smooth((t - LET_GO) / (BACK - LET_GO));
    }

    private static double smooth(double u) {
        double x = Mth.clamp(u, 0.0, 1.0);
        return x * x * (3.0 - 2.0 * x);
    }
}
