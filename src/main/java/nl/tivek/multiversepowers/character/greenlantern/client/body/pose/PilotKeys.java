package nl.tivek.multiversepowers.character.greenlantern.client.body.pose;

import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.math.Keyframes;

// The pilot through the mech's build, key by key (PilotBody poses them): the ring fist raised before the face to
// gather its light, a wind-up and the ring fist thrown at each foot as it forms and driven down with it, the other
// hand bracing its wrist; a crouch, arms swung back, and a leap with the toes pointed, one knee drawn up and the ring
// fist high; a catch in the light, the arms flared wide and conducting the mech's forearms in, spread with the wrists
// cocked, a clap and a squeeze; a lift with the palms up and a look round the chest closing in, then down onto the
// seat, hands on the thighs, and forward to take the sticks; watching the head form and bracing for its crash; working
// the sticks with the whole body as the mech digs the head out, winds it back and flings it (MechBuild.lever moves the
// sticks), watching it tumble high and ducking as it locks on.
// Model space, in pixels: y runs down, -z is ahead, +x the pilot's own left; hands in the chest's own axes from the
// neck, feet from the ground under the hips.
final class PilotKeys {
    // Hips (drop, back), trunk (lean ahead, waist, twist, roll), right and left foot, right and left hand, each
    // hand's fingers and the way its palm faces (the left's mirrored), the toes pointed down (right, left), and the
    // head's look (yaw to its right, pitch down, and how far that look leads the player's own).
    static final int DROP = 0;
    static final int BACK = 1;
    static final int PITCH = 2;
    static final int WAIST = 3;
    static final int TWIST = 4;
    static final int ROLL = 5;
    static final int FEET = 6;
    static final int HANDS = 12;
    static final int PALMS = 18;
    static final int TOES = 30;
    static final int LOOK = 32;
    static final int VALUES = 35;

    // A right hand's fingers and the way its palm faces, in the chest's axes; the left hand's mirror it.
    private record Palm(float fx, float fy, float fz, float nx, float ny, float nz) {
    }

    private static final Palm HANG = new Palm(0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F);
    private static final Palm DOWN = new Palm(0.0F, 0.0F, -1.0F, 0.0F, 1.0F, 0.0F);
    private static final Palm UP = new Palm(0.0F, 0.0F, -1.0F, 0.0F, -1.0F, 0.0F);
    private static final Palm IN = new Palm(0.0F, 0.0F, -1.0F, 1.0F, 0.0F, 0.0F);
    private static final Palm GRIP = new Palm(0.0F, 0.35F, -0.94F, 1.0F, 0.0F, 0.0F);
    private static final Palm RAISED = new Palm(0.0F, -1.0F, 0.0F, 1.0F, 0.0F, 0.0F);
    private static final Palm STOP = new Palm(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, -1.0F);
    private static final Palm FLARE = new Palm(-0.7F, -0.7F, 0.0F, 0.0F, 0.0F, -1.0F);
    private static final Palm BACKWARD = new Palm(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
    // Spread round the target with the wrists cocked back, palms in.
    private static final Palm COCKED = new Palm(-0.35F, -0.6F, -0.72F, 1.0F, 0.0F, 0.0F);
    // Cupping the other wrist from below.
    private static final Palm BRACE = new Palm(1.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F);

    private static final Pose STAND = new Pose().hips(0.8F, 0.0F).trunk(0.05F, 0.0F, 0.0F, 0.0F)
            .feet(-2.4F, 24.0F, -0.8F, 2.4F, 24.0F, 0.8F).hands(-5.8F, 11.5F, 0.5F, 5.8F, 11.5F, 0.5F)
            .palms(HANG, HANG);
    // The ring fist raised before the face, its back to the eyes and the other hand under its wrist, the knees giving
    // as the light gathers; then lifted higher as it blazes.
    private static final Pose CHARGE = new Pose().hips(1.4F, 0.2F).trunk(0.12F, 0.05F, 0.15F, 0.0F)
            .feet(-2.7F, 24.0F, 0.8F, 2.7F, 24.0F, -0.6F).hands(-2.0F, 1.5F, -6.5F, 0.4F, 4.2F, -5.6F)
            .palms(STOP, BRACE).look(0.12F, 0.3F, 0.85F);
    private static final Pose CHARGE_HIGH = CHARGE.copy().hips(1.9F, 0.3F).trunk(0.04F, -0.04F, 0.1F, 0.0F)
            .hands(-1.8F, -1.6F, -6.2F, 0.6F, 2.4F, -5.6F).look(0.08F, -0.08F, 0.85F);
    // Winding up: the ring fist drawn back by the waist, the other hand out, looking up to where the foot forms.
    private static final Pose READY = new Pose().hips(2.4F, 0.4F).trunk(0.2F, 0.05F, 0.25F, 0.04F)
            .feet(-3.2F, 24.0F, 1.6F, 3.0F, 24.0F, -1.4F).hands(-5.0F, 9.5F, 3.0F, 5.0F, 6.0F, -6.0F)
            .palms(DOWN, STOP).look(0.0F, -0.35F, 0.8F);
    // The ring fist thrown at the forming foot (PilotBody aims it), the other hand bracing its wrist.
    private static final Pose CAST = new Pose().hips(2.0F, 0.3F).trunk(0.12F, 0.0F, -0.25F, 0.0F)
            .feet(-3.2F, 24.0F, 1.6F, 3.0F, 24.0F, -1.4F).hands(-1.5F, 2.8F, -8.8F, -1.0F, 4.0F, -6.2F)
            .palms(DOWN, BRACE).look(0.0F, -0.6F, 0.9F);
    // Driving it down onto the target with the whole body.
    private static final Pose DRIVE = CAST.copy().hips(3.4F, 0.5F).trunk(0.32F, 0.08F, -0.2F, 0.0F);
    private static final Pose RECOVER = CAST.copy().hips(2.2F, 0.3F).trunk(0.12F, 0.0F, -0.1F, 0.0F);
    private static final Pose SETTLE = new Pose().hips(1.5F, 0.2F).trunk(0.1F, 0.0F, 0.0F, 0.0F)
            .feet(-2.6F, 24.0F, 0.4F, 2.6F, 24.0F, -0.4F).hands(-6.5F, 9.5F, -1.0F, 6.5F, 9.5F, -1.0F)
            .palms(HANG, HANG).look(0.0F, 0.25F, 0.7F);
    // A deep crouch, arms swung back, eyes on the spot to leap to.
    private static final Pose WIND_UP = new Pose().hips(5.0F, 1.0F).trunk(0.55F, 0.2F, 0.0F, 0.0F)
            .feet(-2.6F, 24.0F, -1.0F, 2.6F, 24.0F, 0.6F).hands(-6.5F, 9.5F, 5.5F, 6.5F, 9.5F, 5.5F)
            .palms(BACKWARD, BACKWARD).look(0.0F, -0.45F, 0.85F);
    private static final Pose PUSH = new Pose().hips(2.0F, 0.3F).trunk(0.2F, 0.05F, 0.0F, 0.0F)
            .feet(-2.4F, 24.0F, 0.2F, 2.4F, 24.0F, 0.8F).hands(-5.5F, 4.0F, -5.0F, 5.5F, 4.0F, -5.0F)
            .palms(IN, IN).toes(0.5F, 0.5F).look(0.0F, -0.5F, 0.85F);
    private static final Pose LAUNCH = new Pose().hips(-0.5F, 0.0F).trunk(-0.1F, -0.1F, 0.0F, 0.0F)
            .feet(-2.0F, 24.5F, 1.5F, 2.0F, 24.5F, 2.5F).hands(-4.2F, -8.0F, -1.5F, 4.0F, -4.5F, -6.5F)
            .palms(RAISED, STOP).toes(0.9F, 0.9F).look(0.0F, -0.6F, 0.85F);
    // In the air one knee drawn up, the other leg trailing with its toes pointed, the ring fist high, the free arm out.
    private static final Pose HERO = new Pose().hips(0.5F, 0.0F).trunk(0.12F, 0.05F, 0.0F, 0.0F)
            .feet(-2.3F, 17.5F, -4.5F, 2.2F, 23.0F, 3.8F).hands(-4.5F, -7.5F, -2.5F, 13.0F, 2.5F, -2.0F)
            .palms(RAISED, DOWN).toes(0.35F, 0.95F).look(0.0F, -0.35F, 0.7F);
    private static final Pose GLIDE = HERO.copy().hands(-4.6F, -7.8F, -2.8F, 12.6F, 2.0F, -2.6F);
    private static final Pose REACH_DOWN = new Pose().hips(1.5F, 0.0F).trunk(0.2F, 0.0F, 0.0F, 0.0F)
            .feet(-2.4F, 22.5F, -1.8F, 2.4F, 22.5F, -0.8F).hands(-12.0F, 5.0F, -3.0F, 12.0F, 5.0F, -3.0F)
            .palms(DOWN, DOWN).toes(0.5F, 0.5F).look(0.0F, 0.3F, 0.7F);
    // Caught by the light in the chest: a crouch in the air, then unfurling to hang in it.
    private static final Pose CATCH = new Pose().hips(3.5F, 0.0F).trunk(0.35F, 0.0F, 0.0F, 0.0F)
            .feet(-2.5F, 22.0F, -3.0F, 2.5F, 22.0F, -2.0F).hands(-4.5F, 9.0F, -6.5F, 4.5F, 9.0F, -6.5F)
            .palms(DOWN, DOWN).toes(0.45F, 0.45F).look(0.0F, 0.4F, 0.7F);
    private static final Pose FLOAT = new Pose().hips(1.4F, 0.0F).trunk(0.12F, 0.0F, 0.0F, 0.03F)
            .feet(-2.4F, 23.2F, -0.8F, 2.4F, 23.6F, 0.6F).hands(-7.0F, 8.0F, -3.0F, 7.0F, 8.5F, -2.5F)
            .palms(DOWN, DOWN).toes(0.6F, 0.65F).look(0.0F, 0.15F, 0.6F);
    // The light flares: arms flung wide and up, chest open, head back.
    private static final Pose FLARED = new Pose().hips(0.3F, 0.0F).trunk(-0.22F, -0.1F, 0.0F, 0.0F)
            .feet(-2.8F, 24.0F, 1.0F, 2.8F, 24.0F, 1.0F).hands(-13.0F, -2.0F, -1.5F, 13.0F, -2.0F, -1.5F)
            .palms(FLARE, FLARE).toes(0.7F, 0.7F).look(0.0F, -0.55F, 0.8F);
    private static final Pose FLARED_HOLD = FLARED.copy().hands(-13.4F, -2.6F, -1.0F, 13.4F, -2.6F, -1.0F);
    // Reaching up to the forearms forming high above, then pulling them down onto the target.
    private static final Pose REACH_UP = new Pose().hips(0.8F, 0.0F).trunk(0.05F, 0.0F, 0.0F, 0.0F)
            .feet(-2.3F, 23.5F, 0.3F, 2.3F, 23.2F, -0.3F).hands(-4.5F, -9.0F, -6.0F, 4.5F, -9.0F, -6.0F)
            .palms(STOP, STOP).toes(0.6F, 0.6F).look(0.0F, -0.65F, 0.8F);
    private static final Pose PULL_DOWN = new Pose().hips(1.6F, 0.0F).trunk(0.25F, 0.0F, 0.0F, 0.0F)
            .feet(-2.3F, 23.2F, -0.3F, 2.3F, 23.5F, 0.3F).hands(-3.5F, 5.5F, -6.5F, 3.5F, 5.5F, -6.5F)
            .palms(DOWN, DOWN).toes(0.55F, 0.55F).look(0.0F, 0.35F, 0.8F);
    private static final Pose REACH = PULL_DOWN.copy().hips(1.0F, 0.0F).trunk(0.15F, 0.0F, 0.0F, 0.0F)
            .hands(-4.5F, 4.0F, -9.0F, 4.5F, 4.0F, -9.0F).palms(IN, IN);
    // Spread wide round the target, the wrists cocked back, then drawn further back before the swing.
    private static final Pose SPREAD = new Pose().hips(0.6F, 0.0F).trunk(-0.05F, 0.0F, 0.0F, 0.0F)
            .feet(-2.6F, 23.6F, 0.2F, 2.6F, 23.6F, 0.2F).hands(-14.0F, 2.5F, -2.5F, 14.0F, 2.5F, -2.5F)
            .palms(COCKED, COCKED).toes(0.6F, 0.6F).look(0.0F, 0.3F, 0.8F);
    private static final Pose CLAP_BACK = SPREAD.copy().trunk(-0.1F, 0.0F, 0.0F, 0.0F)
            .hands(-12.5F, 2.0F, -0.5F, 12.5F, 2.0F, -0.5F);
    // Palm flat on palm before the chest, bent into it, then squeezing with the head down.
    private static final Pose CLAP = new Pose().hips(1.8F, 0.0F).trunk(0.28F, 0.1F, 0.0F, 0.0F)
            .feet(-2.3F, 22.5F, -2.2F, 2.3F, 22.3F, -1.8F).hands(-0.7F, 3.2F, -8.2F, 0.7F, 3.2F, -8.2F)
            .palms(IN, IN).toes(0.45F, 0.45F).look(0.0F, 0.45F, 0.8F);
    private static final Pose SQUEEZE = CLAP.copy().hips(2.0F, 0.0F).trunk(0.32F, 0.12F, 0.0F, 0.0F)
            .hands(-0.6F, 3.6F, -8.0F, 0.6F, 3.6F, -8.0F).look(0.0F, 0.55F, 0.8F);
    private static final Pose RELEASE = new Pose().hips(0.8F, 0.0F).trunk(0.0F, 0.0F, 0.0F, 0.0F)
            .feet(-2.4F, 23.5F, 0.0F, 2.4F, 23.5F, 0.0F).hands(-9.0F, 3.0F, -6.5F, 9.0F, 3.0F, -6.5F)
            .palms(STOP, STOP).toes(0.55F, 0.55F).look(0.0F, 0.1F, 0.7F);
    // Lifting with the palms up as the chest rises round them, then a look round it to either side.
    private static final Pose LIFT = new Pose().hips(0.5F, 0.0F).trunk(-0.12F, 0.0F, 0.0F, 0.0F)
            .feet(-2.4F, 23.8F, 0.4F, 2.4F, 23.8F, 0.4F).hands(-8.5F, -6.5F, -3.0F, 8.5F, -6.5F, -3.0F)
            .palms(UP, UP).toes(0.6F, 0.6F).look(0.0F, -0.35F, 0.7F);
    private static final Pose LOOK_LEFT = LIFT.copy().hips(0.6F, 0.0F).trunk(-0.05F, 0.0F, -0.12F, 0.0F)
            .hands(-8.0F, 6.0F, -1.5F, 8.0F, 6.0F, -1.5F).palms(DOWN, DOWN).look(-0.6F, -0.15F, 0.85F);
    private static final Pose LOOK_RIGHT = LOOK_LEFT.copy().trunk(0.0F, 0.0F, 0.1F, 0.0F).look(0.5F, -0.1F, 0.85F);
    // Down onto the seat, the feet up onto the rest and the hands onto the thighs.
    private static final Pose SITTING = new Pose().hips(1.3F, 0.9F).trunk(0.1F, 0.0F, 0.0F, 0.0F)
            .feet(-2.4F, 20.5F, -7.0F, 2.4F, 20.8F, -6.5F).hands(-4.5F, 11.5F, -5.5F, 4.5F, 11.5F, -5.5F)
            .palms(DOWN, DOWN).toes(0.3F, 0.3F).look(0.0F, 0.4F, 0.8F);
    private static final Pose SEATED = new Pose().hips(0.4F, 1.2F).trunk(-0.02F, 0.0F, 0.0F, 0.0F)
            .feet(-2.4F, 18.4F, -10.0F, 2.4F, 18.4F, -10.0F).hands(-4.2F, 12.2F, -6.2F, 4.2F, 12.2F, -6.2F)
            .palms(DOWN, DOWN).look(0.0F, 0.3F, 0.7F);
    private static final Pose SEATED_OUT = SEATED.copy().look(0.0F, 0.0F, 0.6F);
    // Forward to the sticks (PilotBody puts the hands on them).
    private static final Pose SEATED_AHEAD = SEATED.copy().trunk(0.12F, 0.0F, 0.0F, 0.0F)
            .hands(-5.5F, 3.0F, -8.0F, 5.5F, 3.0F, -8.0F).palms(GRIP, GRIP).look(0.0F, 0.1F, 0.6F);
    private static final Pose WATCH_UP = SEATED_AHEAD.copy().trunk(0.04F, -0.06F, 0.0F, 0.0F)
            .look(0.0F, -0.5F, 0.8F);
    private static final Pose BRACED = SEATED_AHEAD.copy().trunk(0.2F, 0.05F, 0.0F, 0.0F).look(0.0F, 0.15F, 0.8F);
    private static final Pose WATCH_CRATER = SEATED_AHEAD.copy().trunk(0.16F, 0.0F, 0.0F, 0.0F)
            .look(0.0F, 0.3F, 0.8F);
    private static final Pose LOOK_UP = SEATED_AHEAD.copy().trunk(0.0F, -0.06F, 0.0F, 0.0F)
            .look(0.0F, -0.85F, 0.85F);
    // Working the sticks as the mech digs the head out and throws it: thrown back as its arm rears up, hunched over them
    // as it lunges, heaving as it lifts, twisted round after the head as it is swung back past its hip, flung round the
    // other way as it is thrown, then leaning back to watch it tumble high and ducking as it comes down.
    private static final Pose HAUL_BACK = SEATED_AHEAD.copy().trunk(-0.1F, -0.06F, 0.12F, 0.0F)
            .look(0.2F, -0.55F, 0.85F);
    private static final Pose SHOVE = SEATED_AHEAD.copy().hips(0.9F, 1.0F).trunk(0.34F, 0.1F, -0.08F, 0.0F)
            .look(0.05F, 0.55F, 0.85F);
    private static final Pose HEAVE = SEATED_AHEAD.copy().trunk(0.08F, 0.0F, 0.0F, 0.0F).look(0.15F, 0.25F, 0.8F);
    private static final Pose TWIST_BACK = SEATED_AHEAD.copy().trunk(0.16F, 0.02F, 0.32F, 0.06F)
            .look(0.55F, 0.2F, 0.85F);
    private static final Pose FLING = SEATED_AHEAD.copy().trunk(-0.12F, -0.08F, -0.24F, -0.04F)
            .look(-0.1F, -0.7F, 0.9F);
    private static final Pose WATCH_TOSS = SEATED_AHEAD.copy().trunk(-0.08F, -0.1F, 0.0F, 0.0F)
            .look(0.0F, -0.95F, 0.9F);
    private static final Pose DUCK = SEATED_AHEAD.copy().hips(0.8F, 1.1F).trunk(0.22F, 0.06F, 0.0F, 0.0F)
            .look(0.0F, 0.3F, 0.8F);
    private static final Pose TRIUMPH = SEATED_AHEAD.copy().trunk(-0.05F, -0.05F, 0.0F, 0.0F)
            .look(0.15F, -0.2F, 0.7F);
    private static final Pose DRIVING = SEATED_AHEAD.copy().look(0.0F, 0.0F, 0.0F);

    static final Keyframes.Key[] MOVES = {
            key(0, true, STAND), key(4, true, CHARGE), key(8, false, CHARGE_HIGH),
            key(MechScript.FOOT_FORM - 2, false, READY), key(MechScript.FOOT_FORM + 1, true, CAST),
            key(MechScript.FOOT_DROP, true, CAST), key(MechScript.STOMP, false, DRIVE),
            key(MechScript.STOMP + 2, false, RECOVER), key(MechScript.FOOT2_FORM + 1, true, CAST),
            key(MechScript.FOOT2_DROP, true, CAST), key(MechScript.STOMP2, false, DRIVE),
            key(MechScript.STOMP2 + 4, true, SETTLE), key(MechScript.LEAP - 4, true, WIND_UP),
            key(MechScript.LEAP, false, PUSH), key(MechScript.LEAP + 3, false, LAUNCH),
            key(MechScript.LEAP + 9, true, HERO), key(MechScript.LEAP + 15, true, GLIDE),
            key(MechScript.ABOARD - 3, false, REACH_DOWN), key(MechScript.ABOARD + 1, true, CATCH),
            key(MechScript.HIPS + 2, false, FLOAT), key(MechScript.CORE + 2, true, FLARED),
            key(MechScript.ARMS_FORM + 4, true, FLARED_HOLD), key(MechScript.ARMS_IN + 2, false, REACH_UP),
            key(MechScript.ARMS_IN + 7, false, PULL_DOWN), key(MechScript.SPREAD - 4, false, REACH),
            key(MechScript.SPREAD + 1, true, SPREAD), key(MechScript.SWING - 2, true, SPREAD),
            key(MechScript.CLAP - 3, false, CLAP_BACK), key(MechScript.CLAP, true, CLAP),
            key(MechScript.CLAP + 5, false, SQUEEZE), key(MechScript.RELEASE - 1, true, SQUEEZE),
            key(MechScript.RELEASE + 3, false, RELEASE), key(MechScript.RISE + 2, false, LIFT),
            key(MechScript.UPPER_ARMS + 1, true, LOOK_LEFT), key(MechScript.SIT - 1, false, LOOK_RIGHT),
            key(MechScript.SIT + 4, false, SITTING), key(MechScript.ELBOWS + 1, true, SEATED),
            key(MechScript.ELBOWS + 5, true, SEATED_OUT), key(MechScript.GRIP + 3, false, SEATED_AHEAD),
            key(MechScript.HEAD_FORM - 3, true, SEATED_AHEAD), key(MechScript.HEAD_FORM + 4, true, WATCH_UP),
            key(MechScript.HEAD_DROP - 2, true, WATCH_UP), key(MechScript.HEAD_DROP + 2, false, BRACED),
            key(MechScript.CRASH + 4, true, WATCH_CRATER), key(MechScript.REACH + 2, false, HAUL_BACK),
            key(MechScript.REACH + 5, true, HAUL_BACK), key(MechScript.GRAB - 2, false, SHOVE),
            key(MechScript.GRAB + 1, true, SHOVE), key(MechScript.GRAB + 5, false, HEAVE),
            key(MechScript.WIND, true, TWIST_BACK), key(MechScript.TOSS + 1, false, FLING),
            key(MechScript.TOSS + 5, true, WATCH_TOSS), key(MechScript.LOCK - 9, false, LOOK_UP),
            key(MechScript.LOCK - 3, true, DUCK), key(MechScript.LOCK + 4, true, TRIUMPH),
            key(MechScript.DONE, true, TRIUMPH), key(MechScript.DONE + 6, false, SEATED_AHEAD),
            key(MechScript.SETTLED, true, DRIVING) };

    private PilotKeys() {
    }

    // Sinking back to the ground as the mech breaks up round them.
    static float[] lowered() {
        return new Pose().hips(0.8F, 0.0F).trunk(0.05F, 0.0F, 0.0F, 0.0F).feet(-2.5F, 24.0F, -0.4F, 2.5F, 24.0F, 0.6F)
                .hands(-8.0F, 9.5F, -1.5F, 8.0F, 9.5F, -1.5F).palms(HANG, HANG).values.clone();
    }

    private static Keyframes.Key key(int t, boolean stop, Pose pose) {
        return new Keyframes.Key(t, stop, pose.values.clone());
    }

    // One key's values, set part by part.
    static final class Pose {
        private final float[] values = new float[VALUES];

        Pose copy() {
            Pose pose = new Pose();
            System.arraycopy(this.values, 0, pose.values, 0, VALUES);
            return pose;
        }

        Pose hips(float drop, float back) {
            return this.set(DROP, drop, back);
        }

        Pose trunk(float pitch, float waist, float twist, float roll) {
            return this.set(PITCH, pitch, waist, twist, roll);
        }

        Pose feet(float rx, float ry, float rz, float lx, float ly, float lz) {
            return this.set(FEET, rx, ry, rz, lx, ly, lz);
        }

        Pose hands(float rx, float ry, float rz, float lx, float ly, float lz) {
            return this.set(HANDS, rx, ry, rz, lx, ly, lz);
        }

        Pose palms(Palm right, Palm left) {
            this.set(PALMS, right.fx(), right.fy(), right.fz(), right.nx(), right.ny(), right.nz());
            return this.set(PALMS + 6, -left.fx(), left.fy(), left.fz(), -left.nx(), left.ny(), left.nz());
        }

        Pose toes(float right, float left) {
            return this.set(TOES, right, left);
        }

        Pose look(float yaw, float pitch, float lead) {
            return this.set(LOOK, yaw, pitch, lead);
        }

        private Pose set(int from, float... values) {
            System.arraycopy(values, 0, this.values, from, values.length);
            return this;
        }
    }
}
