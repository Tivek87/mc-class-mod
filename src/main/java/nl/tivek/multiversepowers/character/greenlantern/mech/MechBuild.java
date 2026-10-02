package nl.tivek.multiversepowers.character.greenlantern.mech;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;

// The mech's body through its build, in MechScript's ticks: its hips sink and its torso leans as weight comes onto
// its legs, it braces for the clap, flexes as its arms lock on and settles into a wide ready crouch, flinches at the
// head's crash, rears up and lunges down to dig the head out, winds it back past its hip and springs tall to fling it
// up, watches it tumble and hunches square to take it, stands tall as it locks, and eases back to rest at SETTLED,
// where the walk takes over. One timeline for the server (where the pilot hangs) and every client.
public final class MechBuild {
    // The hips moved out to the right and up and turned (yaw, pitch front up, roll right side down), the torso turned
    // over the waist the same way, each shoulder's shrug (up +) and the head's own look on its neck (yaw, pitch down +).
    private static final int SWAY = 0;
    private static final int RISE = 1;
    private static final int HIPS_YAW = 2;
    private static final int HIPS_PITCH = 3;
    private static final int HIPS_ROLL = 4;
    private static final int TURN = 5;
    private static final int LEAN = 6;
    private static final int BANK = 7;
    private static final int SHRUG = 8;
    private static final int LOOK_YAW = 10;
    private static final int LOOK_PITCH = 11;
    private static final int CHANNELS = 12;
    // While the upper arms join the shoulders the forearms go over from moving round the ground to riding the torso.
    private static final double ARMS_FROM = MechScript.RELEASE + 4.0;
    private static final double ARMS_ON = MechScript.SIT;
    // A slow breath once the chest is shut, faded out before the walk takes over.
    private static final double BREATH_FROM = MechScript.SIT - 4.0;
    private static final double BREATH_TICKS = 36.0;
    private static final double BREATH_RISE = 0.025;
    private static final double BREATH_SHRUG = 0.03;
    // A lifted foot's roll (toes up +) and its shin's lean the way it goes, by how far through its step it is (0 to 1,
    // on past 1 as it slaps down flat): heel first off the ground, toes up through the swing, heel first back down.
    private static final double[] STEP_TIP = { 0.0, 0.0, 0.0, 0.18, -0.3, 0.0, 0.55, 0.22, 0.0, 1.0, 0.16, 0.0, 1.25,
            0.0, 0.0 };
    private static final double[] STEP_LEAN = { 0.0, 0.0, 0.0, 0.15, -0.12, 0.0, 0.6, 0.26, 0.0, 1.0, 0.04, 0.0, 1.3,
            0.0, 0.0 };
    // A stomping foot wobbles on its sole as it slams down.
    private static final double STOMP_WOBBLE = 0.05;
    private static final Keyframes.Key[] KEYS = {
            key(0, true), key(MechScript.CORE - 1, true),
            key(MechScript.ARMS_IN + 3, true),
            key(MechScript.SPREAD - 3, false, RISE, -0.08, LEAN, -0.03),
            key(MechScript.SWING - 3, true, RISE, -0.16, LEAN, 0.02),
            key(MechScript.CLAP - 2, false, RISE, -0.12, LEAN, -0.02),
            key(MechScript.CLAP + 2, false, RISE, -0.24, LEAN, -0.06),
            key(MechScript.RELEASE - 2, true, RISE, -0.22, LEAN, -0.05),
            key(MechScript.RELEASE + 4, false, RISE, -0.06, LEAN, 0.01),
            key(MechScript.UPPER_ARMS, true),
            key(MechScript.ELBOWS + 3, false, RISE, -0.04),
            // Flexing as its arms lock on: chest out, shoulders up, then down into its guard.
            key(MechScript.ELBOWS + 7, true, RISE, 0.05, LEAN, 0.07, SHRUG, 0.14, SHRUG + 1, 0.14),
            key(MechScript.ELBOWS + 12, false, RISE, -0.12, LEAN, -0.03, SHRUG, 0.06, SHRUG + 1, 0.06),
            // Ready: a wide crouch over its knees, leaning in, the shoulders up a little.
            key(MechScript.HEAD_FORM, true, RISE, -0.34, LEAN, -0.07, HIPS_PITCH, -0.03, SHRUG, 0.08, SHRUG + 1,
                    0.08),
            // Up and back a little, watching the head take shape high above.
            key(MechScript.HEAD_FORM + 8, true, RISE, -0.2, LEAN, 0.05, SHRUG, 0.04, SHRUG + 1, 0.04),
            key(MechScript.HEAD_DROP + 2, false, RISE, -0.3, LEAN, -0.02, SHRUG, 0.06, SHRUG + 1, 0.06),
            key(MechScript.CRASH + 4, false, RISE, -0.36, LEAN, -0.04, SHRUG, 0.12, SHRUG + 1, 0.12),
            // Rearing up with the right arm raised to strike, turned away from the head...
            key(MechScript.REACH + 5, true, RISE, -0.12, LEAN, 0.1, TURN, -0.15, BANK, -0.06, HIPS_YAW, -0.05, SHRUG,
                    0.22, SHRUG + 1, 0.02),
            // ...lunging down onto it with the right shoulder driven in...
            key(MechScript.GRAB - 3, false, RISE, -0.9, LEAN, -0.5, TURN, 0.18, BANK, 0.06, HIPS_PITCH, -0.05, SHRUG,
                    0.1, SHRUG + 1, 0.04),
            key(MechScript.GRAB, true, RISE, -1.5, LEAN, -0.75, TURN, 0.3, BANK, 0.1, HIPS_PITCH, -0.1, HIPS_YAW, 0.08,
                    SHRUG + 1, 0.1),
            // ...hauling it out and swinging it back past the hip, low and wound up...
            key(MechScript.GRAB + 3, false, RISE, -1.35, LEAN, -0.6, TURN, 0.24, BANK, 0.08, HIPS_PITCH, -0.08,
                    HIPS_YAW, 0.06, SHRUG, 0.12),
            key(MechScript.GRAB + 6, false, RISE, -1.05, LEAN, -0.35, TURN, 0.0, BANK, 0.08, HIPS_PITCH, -0.05),
            key(MechScript.WIND, true, RISE, -1.15, LEAN, -0.28, TURN, -0.42, BANK, 0.14, HIPS_YAW, -0.12,
                    HIPS_PITCH, -0.05, SHRUG, 0.05, SHRUG + 1, 0.12),
            // ...and flinging it up as it springs tall, leaning back after it.
            key(MechScript.TOSS - 2, false, RISE, -0.6, LEAN, -0.05, TURN, -0.1, BANK, 0.04, HIPS_YAW, -0.03),
            key(MechScript.TOSS, false, RISE, 0.12, LEAN, 0.16, TURN, 0.12, BANK, -0.06, HIPS_YAW, 0.03, SHRUG, 0.3),
            key(MechScript.TOSS + 5, true, RISE, 0.1, LEAN, 0.2, TURN, 0.08, BANK, -0.04, SHRUG, 0.18, SHRUG + 1,
                    0.04),
            // Watching it tumble high above, then hunched square under it to take it on its neck: nothing turned at
            // the lock, so the head goes on as it falls.
            key(MechScript.LOCK - 10, false, RISE, -0.04, LEAN, 0.14, TURN, 0.03, SHRUG, 0.06, SHRUG + 1, 0.06),
            key(MechScript.LOCK - 3, false, RISE, -0.26, LEAN, 0.02, SHRUG, 0.2, SHRUG + 1, 0.2),
            key(MechScript.LOCK, true, RISE, -0.32, SHRUG, 0.24, SHRUG + 1, 0.24),
            key(MechScript.LOCK + 3, false, RISE, -0.1, LEAN, 0.02, SHRUG, 0.1, SHRUG + 1, 0.1),
            // Standing tall with its chest out and its head thrown back, then a look round.
            key(MechScript.LOCK + 7, true, RISE, 0.07, LEAN, 0.08, LOOK_PITCH, -0.3),
            key(MechScript.DONE, true, RISE, 0.06, LEAN, 0.07, LOOK_YAW, -0.12, LOOK_PITCH, -0.25),
            key(MechScript.DONE + 5, false, RISE, -0.06, LEAN, -0.01, LOOK_YAW, 0.15, LOOK_PITCH, 0.05),
            key(MechScript.SETTLED, true) };
    // Each blow and lock jolts the body: when, how far the hips drop, the torso pitches and the shoulders jump, and
    // how fast it swings and dies away (ticks).
    private static final double[][] JOLTS = {
            { MechScript.CORE + 1, -0.1, 0.0, 0.0, 7.0, 5.0 },
            { MechScript.CLAP, -0.12, -0.05, 0.0, 8.0, 5.0 },
            { MechScript.ELBOWS, -0.06, 0.0, 0.2, 8.0, 5.0 },
            { MechScript.CRASH, -0.28, 0.09, 0.3, 10.0, 7.0 },
            { MechScript.GRAB, -0.12, -0.05, 0.12, 7.0, 4.0 },
            { MechScript.LOCK, -0.22, -0.05, 0.15, 8.0, 5.0 } };
    // How far the pilot throws each stick (right, left; ahead +) as the mech moves: pulled back to brace, the right
    // one hauled back and shoved ahead as the right arm rears up and lunges, winds back and throws.
    private static final Keyframes.Key[] LEVERS = {
            lever(MechScript.GRIP, true, 0.0, 0.0),
            lever(MechScript.HEAD_FORM, true, -0.25, -0.25),
            lever(MechScript.HEAD_FORM + 8, true, -0.1, -0.1),
            lever(MechScript.CRASH, false, -0.15, -0.15),
            lever(MechScript.CRASH + 3, true, -0.55, -0.55),
            lever(MechScript.REACH + 5, true, -0.7, 0.2),
            lever(MechScript.GRAB, true, 1.0, 0.6),
            lever(MechScript.GRAB + 4, false, 0.3, 0.3),
            lever(MechScript.WIND, true, -0.9, 0.35),
            lever(MechScript.TOSS, false, 1.0, -0.2),
            lever(MechScript.TOSS + 5, true, 0.5, 0.0),
            lever(MechScript.LOCK - 3, true, -0.35, -0.35),
            lever(MechScript.LOCK + 2, false, 0.0, 0.0),
            lever(MechScript.DONE, true, 0.0, 0.0) };

    // The body at one moment of the build, in Stage.turned's terms for the hips and MechScript.upper's for the torso.
    public record Body(double sway, double rise, double hipsYaw, double hipsPitch, double hipsRoll, double turn,
            double lean, double bank, double shrugRight, double shrugLeft, double lookYaw, double lookPitch) {
        public double shrug(boolean right) {
            return right ? this.shrugRight : this.shrugLeft;
        }
    }

    private MechBuild() {
    }

    private static Keyframes.Key key(int t, boolean stop, double... set) {
        float[] values = new float[CHANNELS];
        for (int i = 0; i + 1 < set.length; i += 2) {
            values[(int) set[i]] = (float) set[i + 1];
        }
        return new Keyframes.Key(t, stop, values);
    }

    private static Keyframes.Key lever(int t, boolean stop, double right, double left) {
        return new Keyframes.Key(t, stop, new float[] { (float) right, (float) left });
    }

    // How far a stick is thrown ahead (+1) or back (-1) `t` into the build.
    public static double lever(boolean right, double t) {
        if (t <= MechScript.GRIP || t >= MechScript.DONE) {
            return 0.0;
        }
        return Keyframes.at(LEVERS, (float) t)[right ? 0 : 1];
    }

    public static Body body(double t) {
        float[] v = Keyframes.at(KEYS, (float) Mth.clamp(t, 0.0, MechScript.SETTLED));
        double rise = v[RISE];
        double lean = v[LEAN];
        double shrug = 0.0;
        // Every jolt has died away by the time the walk takes over.
        double calm = 1.0 - Ease.smooth((t - MechScript.DONE) / (MechScript.SETTLED - MechScript.DONE));
        for (double[] jolt : JOLTS) {
            double wave = wave(t - jolt[0], jolt[4], jolt[5]) * calm;
            rise += jolt[1] * wave;
            lean += jolt[2] * wave;
            shrug += jolt[3] * Math.max(0.0, wave);
        }
        double breath = Math.sin(2.0 * Math.PI * (t - BREATH_FROM) / BREATH_TICKS)
                * Ease.smooth((t - BREATH_FROM) / 8.0) * calm;
        rise += BREATH_RISE * breath;
        shrug += BREATH_SHRUG * breath;
        return new Body(v[SWAY], rise, v[HIPS_YAW], v[HIPS_PITCH], v[HIPS_ROLL], v[TURN], lean, v[BANK],
                v[SHRUG] + shrug, v[SHRUG + 1] + shrug, v[LOOK_YAW], v[LOOK_PITCH]);
    }

    // A damped swing set off `since` ticks ago: its first swing reaches 1, a quarter of `period` in.
    private static double wave(double since, double period, double decay) {
        if (since <= 0.0) {
            return 0.0;
        }
        return Math.exp(-(since - period * 0.25) / decay) * Math.sin(2.0 * Math.PI * since / period);
    }

    public static MechScript.Stage hips(MechScript.Stage stage, Body body) {
        if (body.rise() == 0.0 && body.sway() == 0.0 && body.hipsYaw() == 0.0 && body.hipsPitch() == 0.0
                && body.hipsRoll() == 0.0) {
            return stage;
        }
        return stage.turned(MechScript.WAIST, new Vec3(body.sway(), body.rise(), 0.0), body.hipsYaw(),
                body.hipsPitch(), body.hipsRoll());
    }

    public static MechScript.Stage torso(MechScript.Stage hips, Body body) {
        return body.turn() == 0.0 && body.lean() == 0.0 && body.bank() == 0.0 ? hips
                : hips.turned(MechScript.WAIST, Vec3.ZERO, body.turn(), body.lean(), body.bank());
    }

    // Where the torso is `t` ticks into the build: the pilot hangs and sits in it.
    public static MechScript.Stage torso(MechScript.Stage stage, double t) {
        Body body = body(t);
        return torso(hips(stage, body), body);
    }

    // The frame the build's arms move in: round the ground while the forearms fly loose and clap, the torso's once
    // the upper arms have them.
    public static MechScript.Stage arms(MechScript.Stage stage, MechScript.Stage torso, double t) {
        return stage.toward(torso, Ease.smooth((t - ARMS_FROM) / (ARMS_ON - ARMS_FROM)));
    }

    // How far through its step a foot is (0 lifting off, 1 down again), or below 0 before it steps.
    public static double stepped(boolean right, double t) {
        return (t - MechScript.STEPS[right ? 0 : 1]) / MechScript.STEP_TICKS;
    }

    // The roll of a foot on its ankle (toes up +, radians): stepping heel and toe, and slammed flat by its stomp.
    public static double tip(boolean right, double t) {
        double u = stepped(right, t);
        if (u > 0.0 && u < 1.3) {
            return Ease.keys(u, STEP_TIP);
        }
        double drop = right ? MechScript.FOOT_DROP : MechScript.FOOT2_DROP;
        double stomp = right ? MechScript.STOMP : MechScript.STOMP2;
        if (t > drop && t < stomp) {
            // Toes a little up as it falls, slapped flat as it lands.
            return 0.12 * Ease.smooth((t - drop) / (stomp - drop)) * (1.0 - Ease.smooth((t - stomp + 0.6) / 0.6));
        }
        return STOMP_WOBBLE * wave(t - stomp, 5.0, 3.0);
    }

    // How far a lower leg leans the way its foot steps (radians), swung from where its hip will be.
    public static double lean(boolean right, double t) {
        double u = stepped(right, t);
        return u > 0.0 && u < 1.3 ? Ease.keys(u, STEP_LEAN) : 0.0;
    }
}
