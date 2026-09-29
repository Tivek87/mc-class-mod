package nl.tivek.multiversepowers.character.greenlantern.hand;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;

// The two hands that work with their ring: the ring hammer and the ring chains.
abstract class HandRings extends HandMarvels {
    private static final double RING_KICK = 7.5;
    private static final double RING_CALM = 0.32;

    // Ring hammer: up out of the ground as a fist turned side on, a hammer of hard light grows out of its ring, the
    // fist swings it back over itself and smashes it down on the creature; the hammer then breaks apart.
    private static final double HAMMER_RING = 4.5;
    private static final double HAMMER_FORMS = 6.0;
    private static final double HAMMER_GRIPS = 10.0;
    private static final double HAMMER_RAISE = 11.0;
    private static final double HAMMER_SWING = 15.0;
    private static final double HAMMER_SMASH = 16.2;
    private static final double HAMMER_BREAK = 21.0;
    public static final int HAMMER_GLOWS = ticks(HAMMER_RING);
    public static final int HAMMER_FORMED = ticks(HAMMER_GRIPS);
    public static final int HAMMER_SWINGS = ticks(HAMMER_SWING);
    public static final int HAMMER_SMASHES = ticks(HAMMER_SMASH);
    public static final int HAMMER_BREAKS = ticks(HAMMER_BREAK);
    // The haft runs through the fist along the hand's right; the head sits HAFT out on its end.
    public static final Vec3 HAMMER_GRIP = new Vec3(-0.1, 3.55, 0.95);
    public static final double HAFT = 4.6;
    public static final Vec3 HAMMER_HEAD = HAMMER_GRIP.add(HAFT, 0.0, 0.0);
    private static final double HAMMER_LENGTH = 2.0;
    private static final double HAMMER_BACK = 0.95;

    // Ring chains: a fist points its ring at the creature, chains of hard light shoot out of it and wind round the
    // creature, the fist yanks it through the air into its grip, squeezes it and hurls it away.
    private static final double CHAIN_AIM = 6.0;
    private static final double CHAIN_SHOOT = 11.0;
    private static final double CHAIN_REACH = 14.0;
    private static final double CHAIN_WRAP = 16.0;
    private static final double CHAIN_YANK = 18.0;
    private static final double CHAIN_CATCH = 22.0;
    private static final double CHAIN_THROW = 28.0;
    public static final int CHAINS_SHOOT = ticks(CHAIN_SHOOT);
    public static final int CHAINS_REACH = ticks(CHAIN_REACH);
    public static final int CHAINS_YANK = ticks(CHAIN_YANK);
    public static final int CHAINS_CATCH = ticks(CHAIN_CATCH);
    public static final int[] CHAINS_SQUEEZE = { ticks(CHAIN_CATCH + 1.5), ticks(CHAIN_CATCH + 3.5) };
    public static final int CHAINS_THROW = ticks(CHAIN_THROW);
    private static final double CHAIN_LENGTH = 3.4;

    // How grown the hammer is at tick t, 0 to 1.
    public static double hammerGrown(double t) {
        return Ease.smoother((t / SLOW - HAMMER_FORMS) / (HAMMER_GRIPS - HAMMER_FORMS));
    }

    // How far the chains have shot out of the ring at tick t, 0 to 1, and how far they have wound round the creature.
    public static double chainsOut(double t) {
        return Ease.smooth((t / SLOW - CHAIN_SHOOT) / (CHAIN_REACH - CHAIN_SHOOT));
    }

    public static double chainsWound(double t) {
        return Ease.smooth((t / SLOW - CHAIN_REACH) / (CHAIN_WRAP - CHAIN_REACH));
    }

    // How far the yank has pulled the creature from where it stood to the fist, 0 to 1.
    public static double chainsPulled(double t) {
        double u = Mth.clamp((t / SLOW - CHAIN_YANK) / (CHAIN_CATCH - CHAIN_YANK), 0.0, 1.0);
        return u * u * (3.0 - 2.0 * u) * (0.6 + 0.4 * u);
    }

    // The lean that brings the hammer's head down on the ground reach away: the arm and the haft at a right angle.
    static double hammerLean(double reach) {
        double arm = HAMMER_LENGTH + HAMMER_GRIP.y;
        double out = HAMMER_HEAD.x;
        double far = Math.sqrt(arm * arm + out * out);
        double lean = Math.PI - Math.asin(Mth.clamp(reach / far, 0.0, 1.0)) - Math.atan2(out, arm);
        return Mth.clamp(lean, 0.35, 1.45);
    }

    void ringHammer(double t, double reach) {
        double raised = Ease.smoother((t - HAMMER_RAISE) / 3.0);
        double strike = t < HAMMER_SWING ? 0.0 : Ease.hermite(0.0, 0.0, 1.0, 1.6,
                Mth.clamp((t - HAMMER_SWING) / (HAMMER_SMASH - HAMMER_SWING), 0.0, 1.0));
        double back = 1.0 - Ease.smoother((t - HAMMER_BREAK - 1.0) / 5.0);
        double recoil = Ease.recoil(t - HAMMER_SMASH, 1.0, RING_KICK, RING_CALM);
        double down = hammerLean(reach);
        this.length = rise(t, HAMMER_LENGTH, 1.0, 0.6) + 0.5 * raised * (1.0 - strike);
        this.lean = Mth.lerp(strike, -HAMMER_BACK * raised, down) * back + 0.1 * (1.0 - back) - 0.12 * recoil;
        this.twist = Math.PI * 0.5 * Ease.smoother((t - HAMMER_RING) / 4.0) * back + corkscrew(t, 0.5);
        this.flex = 0.25 * strike * back - 0.2 * raised * (1.0 - strike) + 0.03 * Math.sin(t * 7.0)
                * window(t, HAMMER_SWING - 3.0, HAMMER_SWING - 1.0, HAMMER_SWING, HAMMER_SWING + 0.3);
        unfurl(t, 0.2, 0.15, 0.3, 0.8);
        cascade(t, HAMMER_GRIPS - 1.2, 1.2, true, 0.9, 0.25, 0.8);
        this.spread = Mth.lerp(Ease.smoother((t - HAMMER_GRIPS + 1.2) / 1.2), this.spread, 0.0);
        cascade(t, HAMMER_BREAK, 0.8, false, 0.1, 0.1, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - HAMMER_BREAK) / 1.2), this.spread, 0.9);
        cascade(t, HAMMER_BREAK + 4.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - HAMMER_BREAK - 4.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.5 * window(t, 5.0, 7.0, HAMMER_RAISE, HAMMER_RAISE + 2.0));
        this.sink(t, SINK[RINGHAMMER], LIFE[RINGHAMMER], 1.0, true);
    }

    void ringChains(double t) {
        double aim = Ease.smoother((t - CHAIN_AIM) / 3.0);
        double yank = Ease.hermite(0.0, 0.0, 1.0, 0.0, Mth.clamp((t - CHAIN_YANK) / 1.4, 0.0, 1.0))
                * (1.0 - Ease.smoother((t - CHAIN_CATCH) / 3.0));
        double squeeze = window(t, CHAIN_CATCH, CHAIN_CATCH + 0.6, CHAIN_THROW - 1.5, CHAIN_THROW - 0.6);
        double wind = window(t, CHAIN_THROW - 3.0, CHAIN_THROW - 1.0, CHAIN_THROW, CHAIN_THROW + 0.3);
        double hurl = Ease.hermite(0.0, 0.0, 1.0, 1.5, Mth.clamp((t - CHAIN_THROW) / 1.2, 0.0, 1.0))
                * (1.0 - Ease.smoother((t - CHAIN_THROW - 2.0) / 5.0));
        double charging = window(t, CHAIN_AIM + 2.0, CHAIN_SHOOT - 1.0, CHAIN_SHOOT, CHAIN_SHOOT + 0.5);
        this.length = rise(t, CHAIN_LENGTH, 1.0, 0.6) + 0.6 * squeeze;
        this.lean = 0.75 * aim - 0.9 * yank + 0.2 * squeeze - 0.55 * wind + 0.55 * hurl
                - 0.15 * Ease.recoil(t - CHAIN_SHOOT, 1.0, RING_KICK, RING_CALM);
        this.flex = 1.0 * aim * (1.0 - Ease.smoother((t - CHAIN_YANK) / 2.0)) + 0.02 * charging * Math.sin(t * 9.0)
                - 0.3 * wind + 0.5 * hurl;
        this.twist = corkscrew(t, 0.5) + 0.06 * squeeze * Math.sin(t * 3.1);
        unfurl(t, 0.3, 0.3, 0.4, 0.5);
        set(t, CHAIN_AIM, 2.5, new double[] { 1.0, 1.0, 1.0, 1.0 }, new double[] { 0.1, 0.1, 0.1, 0.1 }, 0.9, 0.0,
                0.0);
        // Open to take the creature as it flies in, closed round it, flung open to let it go.
        cascade(t, CHAIN_CATCH - 2.0, 1.0, false, 0.15, 0.1, 0.2);
        this.spread = Mth.lerp(Ease.smoother((t - CHAIN_CATCH + 2.0) / 1.0), this.spread, 0.8);
        cascade(t, CHAIN_CATCH, 0.8, true, 0.95, 0.35, 0.85);
        this.spread = Mth.lerp(Ease.smoother((t - CHAIN_CATCH) / 0.8), this.spread, 0.0);
        for (int k = 0; k < 4; k++) {
            this.curl[k] += 0.05 * squeeze * Math.sin(t * 4.3 + k);
        }
        cascade(t, CHAIN_THROW + 0.4, 0.6, false, 0.05, 0.05, 0.1);
        this.spread = Mth.lerp(Ease.smoother((t - CHAIN_THROW - 0.4) / 0.6), this.spread, 1.0);
        cascade(t, CHAIN_THROW + 5.0, 3.0, false, 0.35, 0.25, 0.45);
        this.spread = Mth.lerp(Ease.smoother((t - CHAIN_THROW - 5.0) / 3.0), this.spread, 0.5);
        breathe(t, 0.5 * window(t, 4.0, 6.0, CHAIN_AIM, CHAIN_AIM + 2.0));
        this.sink(t, SINK[RINGCHAINS], LIFE[RINGCHAINS], 1.0, true);
    }
}
