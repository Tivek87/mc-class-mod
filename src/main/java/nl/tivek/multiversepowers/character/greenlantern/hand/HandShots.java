package nl.tivek.multiversepowers.character.greenlantern.hand;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;

// The beam eye and the gunslinger.
abstract class HandShots extends HandRings {
    private static final double SHOT_KICK = 7.5;
    private static final double SHOT_CALM = 0.32;

    // Beam eye: up out of the ground or a wall, palm to the creature, an eye shut in it. The eye opens, gathers light
    // and fires a thick beam, following its creature while it burns; then the eye shuts and the hand clenches.
    private static final double BEAMEYE_OPEN = 8.0;
    private static final double BEAMEYE_CHARGE = 10.0;
    static final double BEAMEYE_FIRE = 16.0;
    private static final double BEAMEYE_STOP = 27.0;
    public static final int BEAMEYE_OPENS = ticks(BEAMEYE_OPEN);
    public static final int BEAMEYE_CHARGES = ticks(BEAMEYE_CHARGE);
    public static final int BEAMEYE_FIRES = ticks(BEAMEYE_FIRE);
    public static final int BEAMEYE_STOPS = ticks(BEAMEYE_STOP);
    private static final double BEAMEYE_HIGH = 4.0;

    // Gunslinger: up out of the ground, a revolver grows out of the ring's light into its fist; it shoots three
    // creatures one after another, turning to each, blows the smoke off the muzzle and the revolver breaks apart.
    private static final double GUN_FORM = 5.0;
    private static final double GUN_GRIP = 8.5;
    private static final double[] SLING_BEATS = { 13.0, 20.0, 27.0 };
    private static final double GUN_BLOW = 30.5;
    private static final double GUN_BREAK = 35.0;
    public static final int[] SLING_SHOTS = { ticks(SLING_BEATS[0]), ticks(SLING_BEATS[1]), ticks(SLING_BEATS[2]) };
    public static final int SLING_BLOWS = ticks(GUN_BLOW + 1.0);
    public static final int GUN_BREAKS = ticks(GUN_BREAK);
    private static final double[] SLING_AT = { SLING_SHOTS[0] / SLOW, SLING_SHOTS[1] / SLOW, SLING_SHOTS[2] / SLOW };
    private static final double SLING_LENGTH = 3.4;
    // The aimed muzzle lies about 13 out from where the hand stands: anything nearer the barrel passes over.
    public static final double SLING_CLEAR = 15.0;
    public static final double SLING_HOMING = 26.0;
    // Leaned and bent at the wrist so the fist points level at the creature, palm down, the revolver on its side.
    private static final double SLING_LEAN = 0.55;
    private static final double SLING_FLEX = 0.97;
    // The barrel runs along the knuckles turned this far towards the thumb: the hand turns back as much.
    private static final double SLING_SWEEP = 0.45;
    // The fist round a pistol grip, the index finger on the trigger.
    private static final double[] PISTOL_CURL = { 0.4, 0.695, 0.651, 0.483 };
    private static final double[] PISTOL_HOOK = { 0.35, 0.144, 0.158, 0.095 };
    private static final double PISTOL_THUMB = 0.62;

    // As HandPose.aimed, for a shot that leaves along `own` (the eye's gaze, a barrel) rather than the fingers.
    public static Vec3 aimedAlong(Vec3 own, Vec3 from, Vec3 base, Vec3 reach) {
        Vec3 way = base.add(reach).add(0.0, HandPose.AIMED_HEIGHT, 0.0).subtract(from);
        own = own.normalize();
        if (way.lengthSqr() < 1.0) {
            return own;
        }
        way = way.normalize();
        return way.dot(own) < 0.5 ? own : way;
    }

    // How far the beam eye is open at tick t, 0 to 1.
    public static double beamEyeOpen(double t) {
        double beat = t / SLOW;
        return Ease.smoother((beat - BEAMEYE_OPEN) / 1.5) * (1.0 - Ease.smoother((beat - BEAMEYE_STOP - 1.0) / 2.0));
    }

    // How much light the eye has gathered at tick t, 0 to 1: rising as it charges, full while it fires.
    public static double beamEyeCharge(double t) {
        double beat = t / SLOW;
        return Ease.smooth((beat - BEAMEYE_CHARGE) / (BEAMEYE_FIRE - BEAMEYE_CHARGE))
                * (1.0 - Ease.smoother((beat - BEAMEYE_STOP) / 1.5));
    }

    // How hard the beam burns at tick t, 0 to 1.
    public static double beamEyeFiring(double t) {
        return window(t / SLOW, BEAMEYE_FIRE - 0.3, BEAMEYE_FIRE + 0.4, BEAMEYE_STOP, BEAMEYE_STOP + 1.2);
    }

    // How grown the gunslinger's revolver is at tick t, 0 to 1; it breaks at GUN_BREAKS.
    public static double slingGrown(double t) {
        return Ease.smoother((t / SLOW - GUN_FORM) / (GUN_GRIP - GUN_FORM));
    }

    // How far the hammer is drawn back at tick t, 0 to 1: cocked before each shot, falling at it.
    public static double slingHammer(double t) {
        double beat = t / SLOW;
        double cocked = 0.0;
        for (double shot : SLING_AT) {
            cocked = Math.max(cocked, Ease.smoother((beat - shot + 3.0) / 1.2)
                    * (1.0 - Ease.smoother((beat - shot) / 0.25)));
        }
        return cocked;
    }

    // How far the cylinder has turned at tick t, in chambers: one as the hammer is cocked for each shot.
    public static double slingTurn(double t) {
        double beat = t / SLOW;
        double turned = 0.0;
        for (double shot : SLING_AT) {
            turned += Ease.smoother((beat - shot + 3.0) / 1.2);
        }
        return turned;
    }

    // Aimed, it keeps turned after its creature only slowly: from just before each shot until just after.
    static boolean aiming(double beat) {
        for (double shot : SLING_AT) {
            if (beat >= shot - 1.5 && beat <= shot + 1.0) {
                return true;
            }
        }
        return false;
    }

    void beamEye(double t) {
        double charging = window(t, BEAMEYE_CHARGE, BEAMEYE_CHARGE + 2.0, BEAMEYE_FIRE, BEAMEYE_FIRE + 0.5);
        double firing = window(t, BEAMEYE_FIRE - 0.3, BEAMEYE_FIRE + 0.4, BEAMEYE_STOP, BEAMEYE_STOP + 1.5);
        double braced = window(t, BEAMEYE_OPEN, BEAMEYE_OPEN + 2.0, BEAMEYE_STOP + 1.0, BEAMEYE_STOP + 3.0);
        double kick = Ease.recoil(t - BEAMEYE_FIRE, 1.0, SHOT_KICK, SHOT_CALM);
        this.length = rise(t, BEAMEYE_HIGH, 1.0, 0.6) + 0.25 * charging - 0.2 * firing;
        this.lean = 0.12 + 0.22 * braced - 0.12 * charging - 0.3 * kick
                + 0.025 * (charging + firing) * Math.sin(t * 9.0);
        this.flex = -0.3 * braced - 0.1 * firing + 0.25 * kick;
        this.twist = corkscrew(t, 0.5) + 0.02 * firing * Math.sin(t * 13.0);
        unfurl(t, 0.08, 0.06, 0.15, 1.0);
        this.spread = Mth.lerp(braced, this.spread, 1.0);
        for (int k = 0; k < 5; k++) {
            this.curl[k] += firing * 0.05 * (0.5 + 0.5 * Math.sin(t * 7.0 + k * 1.7));
        }
        cascade(t, BEAMEYE_STOP + 2.0, 1.2, true, 1.0, 0.1, 0.9);
        this.spread = Mth.lerp(Ease.smoother((t - BEAMEYE_STOP - 2.0) / 1.2), this.spread, 0.0);
        breathe(t, 0.5 * window(t, 5.0, 7.0, BEAMEYE_CHARGE, BEAMEYE_CHARGE + 2.0));
        this.sink(t, SINK[BEAMEYE], LIFE[BEAMEYE], 1.0, true);
    }

    void gunslinger(double t) {
        double kick = 0.0;
        double trigger = 0.0;
        for (double shot : SLING_AT) {
            kick += Ease.recoil(t - shot, 1.0, SHOT_KICK, SHOT_CALM);
            trigger += Ease.bump((t - shot + 0.3) / 0.7);
        }
        double aimed = window(t, GUN_GRIP, GUN_GRIP + 2.5, GUN_BLOW - 1.0, GUN_BLOW + 1.0);
        double blow = window(t, GUN_BLOW, GUN_BLOW + 1.5, GUN_BREAK - 1.0, GUN_BREAK);
        this.length = rise(t, SLING_LENGTH, 1.0, 0.6) - 0.2 * kick;
        this.lean = Mth.lerp(aimed, 0.15, SLING_LEAN) - 0.12 * kick;
        // Blowing the smoke off, the muzzle comes up before the face it does not have.
        this.flex = Mth.lerp(aimed, 0.3, SLING_FLEX) - 0.45 * kick - 0.75 * blow;
        this.sweep = SLING_SWEEP * Ease.smoother((t - GUN_FORM) / 3.0);
        this.twist = corkscrew(t, 0.5);
        unfurl(t, 0.2, 0.15, 0.3, 0.6);
        set(t, GUN_GRIP - 1.5, 1.5, PISTOL_CURL, PISTOL_HOOK, PISTOL_THUMB, 0.0, 0.0);
        this.curl[0] += 0.25 * trigger;
        breathe(t, 0.4 * window(t, 6.0, 8.0, GUN_BLOW + 2.0, GUN_BREAK));
        cascade(t, GUN_BREAK - 0.5, 1.5, false, 0.2, 0.15, 0.3);
        this.spread = Mth.lerp(Ease.smoother((t - GUN_BREAK) / 1.5), this.spread, 0.8);
        this.sink(t, SINK[GUNSLINGER], LIFE[GUNSLINGER], 1.0, true);
    }
}
