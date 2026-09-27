package nl.tivek.multiversepowers.character.greenlantern.mech;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The mech's build in ticks, timed on docs/reference/mech-robot-ultimate.mp4 (20 ticks = 30 frames of the clip). Places
// are in blocks round its ground spot: x to its right, y up, z ahead, towards the target it builds itself over.
public final class MechScript {
    public static final int BUILDING = 0;
    public static final int BREAKING = 1;

    public static final int FOOT_FORM = 0;
    public static final int FOOT_DROP = 8;
    public static final int STOMP = 11;
    public static final int FOOT2_FORM = 12;
    public static final int FOOT2_DROP = 19;
    public static final int STOMP2 = 22;
    public static final int[] STEPS = { 34, 39 };
    public static final int STEP_TICKS = 8;
    public static final int LEAP = 30;
    public static final int ABOARD = 50;
    public static final int THIGHS = 48;
    public static final int HIPS = 54;
    public static final int CORE = 57;
    public static final int FORM_TICKS = 8;
    public static final int ARMS_FORM = 58;
    public static final int ARMS_IN = 63;
    public static final int SPREAD = 75;
    public static final int SWING = 81;
    public static final int CLAP = 86;
    public static final int RELEASE = 97;
    public static final int RISE = 103;
    public static final int ARMOR = 100;
    public static final int SHOULDERS = 104;
    public static final int UPPER_ARMS = 108;
    public static final int ELBOWS = 118;
    public static final int STICKS = 118;
    public static final int HEAD_FORM = 132;
    public static final int HEAD_DROP = 144;
    public static final int CRASH = 149;
    public static final int HEAD_RISE = 157;
    public static final int HEAD_LIFT = 162;
    public static final int HEAD_LAND = 170;
    public static final int LOCK = 176;
    public static final int DONE = 188;
    public static final int SETTLED = 200;
    public static final int BREAK_TICKS = 14;

    public static final double TARGET_AHEAD = 3.8;
    public static final Vec3 ANKLE = new Vec3(2.05, 0.95, 0.05);
    public static final Vec3 KNEE = new Vec3(2.05, 3.0, 0.3);
    public static final Vec3 HIP = new Vec3(1.5, 4.85, -0.05);
    public static final Vec3 SHOULDER = new Vec3(3.2, 8.2, 0.0);
    public static final double UPPER_ARM = 1.9;
    public static final double FOREARM = 2.3;
    public static final double PALM_ALONG = FOREARM + 0.62;
    public static final Vec3 COCKPIT = new Vec3(0.0, 6.0, 0.45);
    // The pilot sits: the seat is at their hips, a Minecraft body's 0.75 above its feet.
    public static final double SEAT = 0.72;
    public static final int SIT = 112;
    public static final int GRIP = 124;
    public static final Vec3 LEVER = new Vec3(0.36, 6.62, 1.08);
    public static final double LEVER_LENGTH = 0.62;
    public static final double LEVER_THROW = 0.42;
    public static final Vec3[] BUTTONS = { new Vec3(0.44, 6.99, 1.33), new Vec3(0.15, 7.02, 1.38),
            new Vec3(-0.15, 7.02, 1.38), new Vec3(-0.44, 6.99, 1.33) };
    public static final Vec3 NECK = new Vec3(0.0, 9.1, 0.75);
    public static final double HEAD_SCALE = 1.65;
    public static final double HEAD_UP = 0.62 * HEAD_SCALE;

    private static final double FOOT_HIGH = 9.0;
    private static final Vec3 FOOT_ON = new Vec3(0.35, 0.0, 0.0);
    private static final Vec3 FOOT2_ON = new Vec3(-1.25, 0.0, -0.3);
    private static final double STEP_HEIGHT = 1.3;
    private static final double LEAP_RISE = 2.4;
    private static final double HEAD_HIGH = 13.5;
    private static final Vec3 HEAD_ABOVE = new Vec3(0.0, 13.6, 0.9);
    private static final double HEAD_SINK = 0.5;
    private static final double HEAD_LIFT_HIGH = 2.6;
    private static final double HEAD_FORM_SPIN = Math.PI * 3.0;
    private static final double HEAD_RISE_SPIN = -Math.PI * 2.0;
    private static final double HEAD_FLIP = -Math.PI * 2.0;

    // Where the mech stands and faces, how high the target's feet are and where its pilot set off from. Its axes stay
    // upright on the ground; a turned copy (see turned) carries the swaying body of a walking mech.
    public record Stage(Vec3 base, Vec3 ahead, Vec3 right, Vec3 up, double targetY, double pilotY, double pilotZ) {
        public static Stage of(Vec3 base, Vec3 toTarget, double pilotY, double pilotZ) {
            Vec3 flat = new Vec3(toTarget.x, 0.0, toTarget.z);
            Vec3 ahead = flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
            return new Stage(base, ahead, new Vec3(-ahead.z, 0.0, ahead.x), Vectors.UP, toTarget.y, pilotY, pilotZ);
        }

        public static Stage facing(Vec3 base, float yaw) {
            double radians = Math.toRadians(yaw);
            return of(base, new Vec3(-Math.sin(radians), 0.0, Math.cos(radians)), COCKPIT.y, COCKPIT.z);
        }

        // Moved by shift along its own axes and turned round pivot (both its own places): about up by yaw (towards
        // its left), then about right by pitch (its front up), then about ahead by roll (its right side down).
        public Stage turned(Vec3 pivot, Vec3 shift, double yaw, double pitch, double roll) {
            Vec3 r = Vectors.spin(this.right, this.up, yaw);
            Vec3 a = Vectors.spin(this.ahead, this.up, yaw);
            Vec3 u = Vectors.spin(this.up, r, pitch);
            a = Vectors.spin(a, r, pitch);
            r = Vectors.spin(r, a, roll);
            u = Vectors.spin(u, a, roll);
            Vec3 at = this.point(pivot.add(shift));
            Vec3 base = at.subtract(r.scale(pivot.x)).subtract(u.scale(pivot.y)).subtract(a.scale(pivot.z));
            return new Stage(base, a, r, u, this.targetY, this.pilotY, this.pilotZ);
        }

        public float yaw() {
            return (float) Math.toDegrees(Math.atan2(-this.ahead.x, this.ahead.z));
        }

        // Read back from what the server sends (see MechAssembly.send).
        public static Stage of(ConstructPayload mech) {
            boolean building = !breaking(mech.variant());
            return of(mech.center(), mech.facing(), building ? mech.charge() : COCKPIT.y, mech.size());
        }

        public Vec3 point(Vec3 local) {
            return this.base.add(this.dir(local));
        }

        public Vec3 point(double x, double y, double z) {
            return this.base.add(this.right.scale(x)).add(this.up.scale(y)).add(this.ahead.scale(z));
        }

        public Vec3 dir(Vec3 local) {
            return this.right.scale(local.x).add(this.up.scale(local.y)).add(this.ahead.scale(local.z));
        }

        public Vec3 local(Vec3 world) {
            Vec3 way = world.subtract(this.base);
            return new Vec3(way.dot(this.right), way.dot(this.up), way.dot(this.ahead));
        }

        public Vec3 target() {
            return new Vec3(0.0, this.targetY, TARGET_AHEAD);
        }

        public Vec3 pilotFrom() {
            return new Vec3(0.0, this.pilotY, this.pilotZ);
        }
    }

    // A creature under the mech's blows: how high it still stands (1 = all), how thin the clap has pressed it across
    // and how far it hangs above the spot of its feet.
    public record Victim(double high, double narrow, double lift) {
    }

    private MechScript() {
    }

    // The variant sent with the mech: bit 0 whether it breaks up, the rest the target's entity id plus one.
    public static int variant(boolean breaking, int target) {
        return (target + 1) << 1 | (breaking ? BREAKING : BUILDING);
    }

    public static boolean breaking(int variant) {
        return (variant & 1) == BREAKING;
    }

    public static int target(int variant) {
        return (variant >>> 1) - 1;
    }

    public static double grown(double t, double from) {
        return Mth.clamp((t - from) / FORM_TICKS, 0.0, 1.0);
    }

    // The ankle of a lower leg as it drops out of the sky onto the target and then steps back under the mech.
    public static Vec3 ankle(boolean right, Stage stage, double t) {
        Vec3 stance = right ? ANKLE : mirror(ANKLE);
        Vec3 on = stage.target().add(right ? FOOT_ON : FOOT2_ON).add(0.0, ANKLE.y, 0.0);
        double drop = right ? FOOT_DROP : FOOT2_DROP;
        double stomp = right ? STOMP : STOMP2;
        if (t < drop) {
            return on.add(0.0, FOOT_HIGH, 0.0);
        }
        if (t < stomp) {
            double u = (t - drop) / (stomp - drop);
            return on.add(0.0, FOOT_HIGH * (1.0 - u * u), 0.0);
        }
        double lift = STEPS[right ? 0 : 1];
        if (t < lift) {
            return on;
        }
        double u = Mth.clamp((t - lift) / STEP_TICKS, 0.0, 1.0);
        return on.lerp(stance, Ease.smooth(u)).add(0.0, STEP_HEIGHT * Math.sin(Math.PI * u), 0.0);
    }

    // How long ago a foot came down, for the stomps and the steps (negative before it did).
    public static double landed(boolean right, double t) {
        double stomp = right ? STOMP : STOMP2;
        double step = STEPS[right ? 0 : 1] + STEP_TICKS;
        return t >= step ? t - step : t - stomp;
    }

    public static boolean stomping(boolean right, double t) {
        return t < STEPS[right ? 0 : 1] + STEP_TICKS;
    }

    // Where the pilot stands, by the soles of their feet: still at first, then a leap up into the cockpit.
    public static Vec3 pilot(Stage stage, double t) {
        Vec3 from = stage.pilotFrom();
        if (t < LEAP) {
            return from;
        }
        if (t < ABOARD) {
            double u = (t - LEAP) / (ABOARD - LEAP);
            Vec3 at = from.lerp(COCKPIT, Ease.smoother(u));
            return at.add(0.0, LEAP_RISE * Math.sin(Math.PI * Math.pow(u, 0.8)), 0.0);
        }
        return COCKPIT;
    }

    // The pilot floating back down to the ground while the mech breaks up round them.
    public static Vec3 lowered(double since) {
        return COCKPIT.scale(1.0 - Ease.smoother(since / BREAK_TICKS));
    }

    // The head's middle: made high above the target, dropped onto it, hauled back up and set onto the neck.
    public static Vec3 head(Stage stage, double t) {
        Vec3 target = stage.target();
        Vec3 high = target.add(0.0, HEAD_HIGH, 0.0);
        Vec3 sunk = target.add(0.0, HEAD_UP - HEAD_SINK, 0.0);
        Vec3 neck = NECK.add(0.0, HEAD_UP, 0.0);
        if (t < HEAD_DROP) {
            return high.add(0.0, 0.25 * Math.sin((t - HEAD_FORM) * 0.25), 0.0);
        }
        if (t < CRASH) {
            double u = (t - HEAD_DROP) / (CRASH - HEAD_DROP);
            return high.lerp(sunk.add(0.0, HEAD_SINK, 0.0), u * u);
        }
        if (t < CRASH + 3.0) {
            return sunk.add(0.0, HEAD_SINK * (1.0 - Ease.smooth((t - CRASH) / 3.0)), 0.0);
        }
        if (t < HEAD_RISE) {
            return sunk;
        }
        Vec3 lifted = sunk.add(0.0, HEAD_LIFT_HIGH, 0.0);
        if (t < HEAD_LIFT) {
            return sunk.lerp(lifted, Ease.smooth((t - HEAD_RISE) / (HEAD_LIFT - HEAD_RISE)));
        }
        if (t < HEAD_LAND) {
            double u = (t - HEAD_LIFT) / (HEAD_LAND - HEAD_LIFT);
            return new Vec3(Ease.hermite(lifted.x, 0.0, HEAD_ABOVE.x, 0.0, Ease.smooth(u)),
                    Ease.hermite(lifted.y, 16.0, HEAD_ABOVE.y, 0.0, u),
                    Ease.hermite(lifted.z, 0.0, HEAD_ABOVE.z, -2.0, Ease.smooth(u)));
        }
        if (t < LOCK) {
            double u = (t - HEAD_LAND) / (LOCK - HEAD_LAND);
            return HEAD_ABOVE.lerp(neck, u * u);
        }
        return neck.add(0.0, -0.08 * Ease.recoil(t - LOCK, 1.0, 1.1, 0.35), 0.0);
    }

    // How the head is turned, in radians: about the upright, then tipped over about its own x, then rolled about its
    // own z. It spins fast as it forms and slows to face down at the target, lies tipped in the crater, and tumbles
    // head over heels on its way up and back down onto the neck, landing upright and facing ahead.
    public record Turn(double yaw, double pitch, double roll) {
    }

    public static Turn headTurn(double t) {
        if (t < HEAD_DROP) {
            double left = 1.0 - Mth.clamp((t - HEAD_FORM) / (HEAD_DROP - HEAD_FORM), 0.0, 1.0);
            return new Turn(HEAD_FORM_SPIN * left * left, 0.0, 0.0);
        }
        if (t < CRASH) {
            return new Turn(0.0, 0.25 * Ease.smooth((t - HEAD_DROP) / (CRASH - HEAD_DROP)), 0.0);
        }
        if (t < HEAD_RISE) {
            double settle = Ease.smooth((t - CRASH) / 3.0);
            return new Turn(0.0, 0.25 - 0.1 * settle, 0.3 * settle);
        }
        double u = Mth.clamp((t - HEAD_RISE) / (LOCK - HEAD_RISE), 0.0, 1.0);
        double left = 1.0 - Ease.smoother(u);
        return new Turn(HEAD_RISE_SPIN * left, 0.15 * left + HEAD_FLIP * left, 0.3 * left);
    }

    public static Victim victim(double t) {
        double high = 1.0;
        if (t >= STOMP && t < STEPS[0] + STEP_TICKS) {
            double lift = t - STEPS[0];
            high = lift > 0.0 ? Mth.lerp(Ease.backOut(lift / STEP_TICKS), 0.18, 1.0)
                    : Mth.lerp(Ease.smooth((t - STOMP) / 2.0), 1.0, 0.18);
        }
        if (t >= CRASH) {
            double up = t - DONE;
            high = up < 0.0 ? Mth.lerp(Ease.smooth((t - CRASH) / 2.0), 1.0, 0.22)
                    : Mth.lerp(Ease.backOut(up / 9.0), 0.22, 1.0);
        }
        double narrow = 1.0;
        double lift = 0.0;
        if (t >= CLAP && t < RELEASE + 4.0) {
            double in = Ease.smooth((t - CLAP) / 1.5) * (1.0 - Ease.smooth((t - RELEASE) / 4.0));
            narrow = 1.0 - 0.55 * in;
            lift = 0.35 * in * Ease.smooth((t - CLAP) / 6.0);
        }
        return new Victim(high, narrow, lift);
    }

    public static float working(double t) {
        return t < DONE ? 0.9F : 0.0F;
    }

    public static Vec3 mirror(Vec3 local) {
        return new Vec3(-local.x, local.y, local.z);
    }
}
