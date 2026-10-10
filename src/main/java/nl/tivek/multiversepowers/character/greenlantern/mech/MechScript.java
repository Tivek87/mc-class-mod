package nl.tivek.multiversepowers.character.greenlantern.mech;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The mech's build in ticks, first timed on docs/reference/mech-robot-ultimate.mp4 (20 ticks = 30 frames of the clip)
// and since drawn out round its pilot. Places are in blocks round its ground spot: x to its right, y up, z ahead,
// towards the target it builds itself over.
public final class MechScript {
    public static final int BUILDING = 0;
    public static final int BREAKING = 1;

    // The pilot gathers the ring's light, then calls each foot down out of the sky onto the target.
    public static final int FOOT_FORM = 12;
    public static final int FOOT_DROP = 24;
    public static final int STOMP = 28;
    public static final int FOOT2_FORM = 32;
    public static final int FOOT2_DROP = 42;
    public static final int STOMP2 = 46;
    // A deep crouch and a leap into the chest, the feet stepping off the target under it.
    public static final int LEAP = 60;
    public static final int[] STEPS = { 62, 68 };
    public static final int STEP_TICKS = 8;
    public static final int THIGHS = 82;
    public static final int ABOARD = 86;
    public static final int HIPS = 90;
    public static final int CORE = 94;
    public static final int FORM_TICKS = 8;
    // The forearms fly in, spread round the target and clap it.
    public static final int ARMS_FORM = 96;
    public static final int ARMS_IN = 102;
    public static final int SPREAD = 116;
    public static final int SWING = 124;
    public static final int CLAP = 130;
    public static final int RELEASE = 144;
    // The chest closes round the pilot, the arms join it and the pilot sits down to the sticks.
    public static final int ARMOR = 148;
    public static final int RISE = 151;
    public static final int SHOULDERS = 152;
    public static final int UPPER_ARMS = 156;
    public static final int ELBOWS = 168;
    public static final int STICKS = 168;
    // The head forms high above and crashes onto the target; the right hand digs it out of the crater, swings it back
    // and tosses it high, and it tumbles down onto the neck.
    public static final int HEAD_FORM = 186;
    public static final int HEAD_DROP = 200;
    public static final int CRASH = 205;
    public static final int REACH = 211;
    public static final int GRAB = 222;
    public static final int WIND = 230;
    public static final int TOSS = 238;
    public static final int LOCK = 260;
    public static final int DONE = 274;
    public static final int SETTLED = 286;
    public static final int BREAK_TICKS = 14;

    public static final double TARGET_AHEAD = 3.8;
    // Built like a jaeger: legs over half its height, a chest wide at the shoulders and narrow at the waist, long arms
    // whose fists hang by its thighs.
    public static final Vec3 ANKLE = new Vec3(1.75, 1.1, 0.05);
    public static final Vec3 KNEE = new Vec3(1.65, 4.0, 0.4);
    public static final Vec3 HIP = new Vec3(1.45, 7.1, -0.05);
    // Where the upper body turns on the hips.
    public static final Vec3 WAIST = new Vec3(0.0, HIP.y, 0.0);
    public static final Vec3 SHOULDER = new Vec3(3.25, 10.45, 0.0);
    public static final double UPPER_ARM = 2.25;
    public static final double FOREARM = 2.75;
    public static final double PALM_ALONG = FOREARM + 0.62;
    public static final Vec3 COCKPIT = new Vec3(0.0, 8.25, 0.45);
    // The pilot sits: the seat is at their hips, a Minecraft body's 0.75 above its feet.
    public static final double SEAT = 0.72;
    public static final int SIT = 162;
    public static final int GRIP = 176;
    public static final Vec3 LEVER = new Vec3(0.36, 8.87, 1.08);
    public static final double LEVER_LENGTH = 0.62;
    public static final double LEVER_THROW = 0.42;
    public static final Vec3[] BUTTONS = { new Vec3(0.44, 9.24, 1.33), new Vec3(0.15, 9.27, 1.38),
            new Vec3(-0.15, 9.27, 1.38), new Vec3(-0.44, 9.24, 1.33) };
    public static final Vec3 NECK = new Vec3(0.0, 11.45, 0.55);
    public static final double HEAD_SCALE = 1.95;
    public static final double HEAD_UP = 0.62 * HEAD_SCALE;

    private static final double FOOT_HIGH = 9.0;
    private static final Vec3 FOOT_ON = new Vec3(0.35, 0.0, 0.0);
    private static final Vec3 FOOT2_ON = new Vec3(-1.25, 0.0, -0.3);
    private static final double STEP_HEIGHT = 1.3;
    private static final double LEAP_RISE = 3.4;

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

        // `u` of the way from this stage to `other`: its spot moved and its axes turned between theirs.
        public Stage toward(Stage other, double u) {
            if (u <= 0.0) {
                return this;
            }
            if (u >= 1.0) {
                return other;
            }
            Vec3 a = this.ahead.lerp(other.ahead, u).normalize();
            Vec3 up = this.up.lerp(other.up, u);
            up = up.subtract(a.scale(up.dot(a))).normalize();
            return new Stage(this.base.lerp(other.base, u), a, a.cross(up), up, other.targetY, other.pilotY,
                    other.pilotZ);
        }

        // Read back from what the server sends (see MechAssembly.send); once built, charge holds the creature a blow
        // takes hold of instead of where the pilot set off from.
        public static Stage of(ConstructPayload mech) {
            boolean building = !breaking(mech.variant()) && mech.age() < SETTLED;
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

    // The variant sent with the mech: bit 0 whether it breaks up, bit 1 whether it strikes a blow, bit 2 whether it
    // climbs; the rest is that blow (MechAttacks.pack), that climb (as its pilot's game packs it) or else the build's
    // target's entity id plus one.
    public static int variant(boolean breaking, int target, int blow, int climb) {
        int rest = blow != 0 ? blow << 2 | 1 : climb != 0 ? climb << 2 | 2 : (target + 1) << 2;
        return rest << 1 | (breaking ? BREAKING : BUILDING);
    }

    public static boolean breaking(int variant) {
        return (variant & 1) == BREAKING;
    }

    public static int target(int variant) {
        return (variant & 6) != 0 ? -1 : (variant >>> 3) - 1;
    }

    public static MechAttacks.Blow blow(int variant) {
        return (variant & 2) != 0 ? MechAttacks.unpack(variant >>> 3) : MechAttacks.Blow.NONE;
    }

    public static int climb(int variant) {
        return (variant & 6) == 4 ? variant >>> 3 : 0;
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
    // A built mech's body turns on its hips to face where its pilot looks (a yaw as the game counts it), all the way
    // round if need be, while its legs catch up: how far, in radians as Stage.turned takes them.
    public static double turnTo(Stage legs, float look) {
        return -Math.toRadians(Mth.wrapDegrees(look - legs.yaw()));
    }

    public static Stage upper(Stage hips, double turn) {
        return turn == 0.0 ? hips : hips.turned(WAIST, Vec3.ZERO, turn, 0.0, 0.0);
    }

    // The torso turned on the waist, leaning ahead (pitch) and to one side (roll) as it goes.
    public static Stage upper(Stage hips, double turn, double pitch, double roll) {
        return pitch == 0.0 && roll == 0.0 ? upper(hips, turn) : hips.turned(WAIST, Vec3.ZERO, turn, pitch, roll);
    }

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

    // How a head is turned on its neck, in radians: about the upright, then tipped over about its own x, then rolled
    // about its own z (as MechHead.turned turns it).
    public record Turn(double yaw, double pitch, double roll) {
    }

    public static Victim victim(double t) {
        double high = 1.0;
        if (t >= STOMP && t < STEPS[0] + STEP_TICKS) {
            double lift = t - STEPS[0];
            high = lift > 0.0 ? Mth.lerp(Ease.backOut(lift / STEP_TICKS), 0.18, 1.0)
                    : Mth.lerp(Ease.smooth((t - STOMP) / 2.0), 1.0, 0.18);
        }
        if (t >= CRASH) {
            // Pressed flat by the head until the hand pulls it off, then springing back up, dazed.
            double up = t - GRAB - 2.0;
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

    // A place given for the right side, on the side asked for.
    public static Vec3 side(Vec3 local, boolean right) {
        return right ? local : mirror(local);
    }
}
