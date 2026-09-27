package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// Places in blocks round the ground spot under the pilot: x to the mech's right, y up, z ahead.
public final class MechScript {
    public static final int BUILDING = 0;
    public static final int BREAKING = 1;

    public static final int LIFT_END = 16;
    public static final int BOOT_FORM = 6;
    public static final int BOOT_GAP = 4;
    public static final int FORM_TICKS = 8;
    public static final int[] STOMPS = { 22, 26 };
    public static final int DROP_TICKS = 4;
    public static final int[] STEPS = { 40, 48, 56, 64 };
    public static final int STEP_TICKS = 8;
    public static final int[] SHINS = { 66, 68 };
    public static final int[] KNEES = { 76, 78 };
    public static final int[] THIGHS = { 80, 82 };
    public static final int HIPS = 90;
    public static final int HIPS_LOCK = 96;
    public static final int ARMS_FORM = 96;
    public static final int SWING = 104;
    public static final int CLAP = 112;
    public static final int FLY = 116;
    public static final int APEX = 128;
    public static final int TORSO = 124;
    public static final int TORSO_TICKS = 12;
    public static final int SHOULDERS = 128;
    public static final int GLASS = 134;
    public static final int SIT_FROM = 124;
    public static final int SIT_TO = 136;
    public static final int ARMS_LOCK = 140;
    public static final int HEAD_FORM = 150;
    public static final int HEAD_AHEAD = 156;
    public static final int DRILL = 162;
    public static final int IMPACT = 168;
    public static final int GRIND = 174;
    public static final int HEAD_BACK = 186;
    public static final int CLACK = 194;
    public static final int DONE = 200;
    public static final int BREAK_TICKS = 14;

    public static final double SEAT_Y = 5.75;
    public static final Vec3 GRIP = new Vec3(0.4, 6.98, 0.6);
    public static final Vec3 CLAP_AT = new Vec3(0.0, 6.9, 4.6);
    public static final Vec3 DRILL_AT = new Vec3(0.0, 0.0, 5.2);
    public static final Vec3 NECK = new Vec3(0.0, 9.35, -0.1);
    public static final double ARM_SCALE = 1.3;
    public static final double LEG_WIDTH = 1.3;
    public static final double FOOT_SCALE = 1.2;
    public static final double PALM_ALONG = 2.55 * ARM_SCALE;
    public static final double PALM_OUT = 0.2 * ARM_SCALE;

    private static final Vec3[] FOOT_PATH = { new Vec3(4.2, 0.0, 2.2), new Vec3(2.8, 0.0, 1.1),
            new Vec3(1.35, 0.0, 0.1) };
    private static final Vec3[] LEFT_FOOT_PATH = { new Vec3(-4.2, 0.0, 2.6), new Vec3(-2.8, 0.0, 1.3),
            new Vec3(-1.35, 0.0, 0.1) };
    private static final double DROP_HEIGHT = 3.2;
    private static final double STEP_HEIGHT = 1.1;
    private static final double STEP_TOE_IN = 0.18;

    public static final Vec3 ANKLE = new Vec3(1.35, 0.95, -0.15);
    public static final Vec3 KNEE = new Vec3(1.35, 3.1, 0.25);
    public static final Vec3 HIP = new Vec3(1.2, 5.0, -0.05);
    public static final Vec3 SHOULDER = new Vec3(2.3, 7.85, -0.05);
    public static final Vec3 ELBOW = new Vec3(3.1, 6.35, 0.25);

    private static final Vec3 ARM_FROM = new Vec3(6.9, 6.3, 1.4);
    private static final Vec3 ARM_FROM_WAY = new Vec3(-0.3, 0.25, 1.0).normalize();
    private static final Vec3 CLAP_WAY = new Vec3(0.0, 0.15, 1.0).normalize();
    private static final Vec3 ARM_APEX = new Vec3(3.6, 10.6, 1.2);
    private static final Vec3 APEX_WAY = new Vec3(0.2, 1.0, 0.2).normalize();
    public static final Vec3 ARM_WAY = new Vec3(0.75, 0.35, 0.55).normalize();
    private static final Vec3 PALM_REST = new Vec3(-0.35, -0.1, 0.93);
    private static final int FLY_TURNS = 3;

    private static final Vec3 HEAD_FROM = new Vec3(0.0, 13.0, -0.1);
    private static final Vec3 HEAD_OVER = new Vec3(0.0, 13.6, 5.2);
    private static final Vec3 HEAD_DOWN = new Vec3(0.0, 0.55, 5.2);
    private static final Vec3 HEAD_DEEP = new Vec3(0.0, 0.35, 5.2);
    private static final Vec3 HEAD_HIGH = new Vec3(0.0, 11.6, -0.1);
    private static final double[] SPIN_KEYS = { 150, 0.15, 156, 0.5, 162, 1.3, 168, 1.8, 174, 1.8, 180, 1.2, 186, 0.6,
            194, 0.0 };
    private static final double SPIN_RESIDUE = spinRaw(CLACK) % (Math.PI * 2.0);

    public record Stage(Vec3 base, Vec3 ahead, Vec3 right) {
        public static Stage of(Vec3 base, Vec3 facing) {
            Vec3 flat = new Vec3(facing.x, 0.0, facing.z);
            Vec3 ahead = flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
            return new Stage(base, ahead, new Vec3(-ahead.z, 0.0, ahead.x));
        }

        public Vec3 point(Vec3 local) {
            return this.base.add(this.dir(local));
        }

        public Vec3 point(double x, double y, double z) {
            return this.base.add(this.right.scale(x)).add(0.0, y, 0.0).add(this.ahead.scale(z));
        }

        public Vec3 dir(Vec3 local) {
            return this.right.scale(local.x).add(0.0, local.y, 0.0).add(this.ahead.scale(local.z));
        }
    }

    // A foot turns (yaw) in radians, to the right.
    public record Foot(Vec3 at, double yaw, double grown) {
    }

    public record Arm(Vec3 elbow, Vec3 way, Vec3 palm, double curl, double grown) {
    }

    private MechScript() {
    }

    public static double seatHeight(double startY, double t) {
        return Mth.lerp(Ease.smoother(t / LIFT_END), startY, SEAT_Y);
    }

    public static double lowered(double since) {
        return SEAT_Y * (1.0 - Ease.smoother(since / BREAK_TICKS));
    }

    public static double grown(double t, double from) {
        return Mth.clamp((t - from) / FORM_TICKS, 0.0, 1.0);
    }

    public static Foot foot(boolean right, double t) {
        int i = right ? 0 : 1;
        Vec3[] path = right ? FOOT_PATH : LEFT_FOOT_PATH;
        double form = BOOT_FORM + i * BOOT_GAP;
        double grown = grown(t, form);
        double stomp = STOMPS[i];
        if (t < stomp) {
            double u = Mth.clamp((t - (stomp - DROP_TICKS)) / DROP_TICKS, 0.0, 1.0);
            return new Foot(path[0].add(0.0, DROP_HEIGHT * (1.0 - u * u), 0.0), 0.0, grown);
        }
        int step = 0;
        for (int k = i; k < STEPS.length; k += 2) {
            double end = STEPS[k];
            if (t < end - STEP_TICKS) {
                break;
            }
            if (t < end) {
                double u = (t - (end - STEP_TICKS)) / STEP_TICKS;
                Vec3 from = path[step];
                Vec3 to = path[step + 1];
                Vec3 at = from.lerp(to, Ease.smooth(u)).add(0.0, STEP_HEIGHT * Math.sin(Math.PI * u), 0.0);
                double toe = (right ? -1.0 : 1.0) * STEP_TOE_IN * Math.sin(Math.PI * u);
                return new Foot(at, toe, 1.0);
            }
            step++;
        }
        return new Foot(path[step], 0.0, 1.0);
    }

    // How far a foot has come down onto the ground, for the stomps and steps (negative before it lands).
    public static double landed(boolean right, double t) {
        int i = right ? 0 : 1;
        double since = t - STOMPS[i];
        for (int k = i; k < STEPS.length; k += 2) {
            if (t >= STEPS[k]) {
                since = t - STEPS[k];
            }
        }
        return since;
    }

    public static Arm arm(boolean right, double t) {
        Arm arm = rightArm(t);
        if (right) {
            return arm;
        }
        return new Arm(mirror(arm.elbow()), mirror(arm.way()), mirror(arm.palm()), arm.curl(), arm.grown());
    }

    private static Vec3 mirror(Vec3 v) {
        return new Vec3(-v.x, v.y, v.z);
    }

    private static Arm rightArm(double t) {
        double grown = grown(t, ARMS_FORM);
        Vec3 clapElbow = CLAP_AT.add(PALM_OUT * 2.0, 0.0, 0.0).subtract(CLAP_WAY.scale(PALM_ALONG));
        Vec3 inward = new Vec3(-1.0, 0.0, 0.0);
        if (t < SWING) {
            return new Arm(ARM_FROM, ARM_FROM_WAY, square(inward, ARM_FROM_WAY), 0.05, grown);
        }
        if (t < CLAP) {
            double u = Math.pow((t - SWING) / (CLAP - SWING), 2.2);
            Vec3 way = ARM_FROM_WAY.lerp(CLAP_WAY, u).normalize();
            Vec3 elbow = ARM_FROM.lerp(clapElbow, u).add(0.0, 0.6 * Math.sin(Math.PI * u), 0.0);
            return new Arm(elbow, way, square(inward, way), 0.05 * (1.0 - u), 1.0);
        }
        if (t < FLY) {
            double bounce = 0.14 * Math.sin(Math.PI * (t - CLAP) / (FLY - CLAP));
            return new Arm(clapElbow.add(bounce, 0.0, 0.0), CLAP_WAY, square(inward, CLAP_WAY), 0.0, 1.0);
        }
        if (t < ARMS_LOCK) {
            double u = (t - FLY) / (ARMS_LOCK - FLY);
            double split = (double) (APEX - FLY) / (ARMS_LOCK - FLY);
            Vec3 elbow;
            Vec3 way;
            if (u < split) {
                double v = Ease.smooth(u / split);
                elbow = hermite(clapElbow, new Vec3(3.0, 6.0, 0.0), ARM_APEX, new Vec3(0.0, 2.0, -2.0), v);
                way = CLAP_WAY.lerp(APEX_WAY, v).normalize();
            } else {
                double v = Ease.smooth((u - split) / (1.0 - split));
                elbow = hermite(ARM_APEX, new Vec3(0.0, 2.0, -2.0), ELBOW, new Vec3(-1.0, -4.0, 0.0), v);
                way = APEX_WAY.lerp(ARM_WAY, v).normalize();
            }
            double spin = FLY_TURNS * Math.PI * 2.0 * (1.0 - Math.pow(1.0 - u, 2.0));
            Vec3 facing = inward.lerp(PALM_REST, Ease.smooth(u)).normalize();
            Vec3 palm = Vectors.spin(square(facing, way), way, spin);
            double curl = 0.55 * Math.sin(Math.PI * Mth.clamp(u * 1.2, 0.0, 1.0));
            return new Arm(elbow, way, palm, curl, 1.0);
        }
        double settle = Ease.recoil(t - ARMS_LOCK, 0.08, 0.9, 0.35);
        Vec3 way = Vectors.spin(ARM_WAY, new Vec3(0.0, 0.0, 1.0), settle).normalize();
        return new Arm(ELBOW, way, square(PALM_REST, way), 0.18, 1.0);
    }

    private static Vec3 square(Vec3 palm, Vec3 way) {
        Vec3 flat = palm.subtract(way.scale(palm.dot(way)));
        return flat.lengthSqr() < 1.0E-8 ? Vectors.across(way)[0] : flat.normalize();
    }

    private static Vec3 hermite(Vec3 a, Vec3 speedA, Vec3 b, Vec3 speedB, double u) {
        return new Vec3(Ease.hermite(a.x, speedA.x, b.x, speedB.x, u), Ease.hermite(a.y, speedA.y, b.y, speedB.y, u),
                Ease.hermite(a.z, speedA.z, b.z, speedB.z, u));
    }

    public static Vec3 head(double t) {
        if (t < HEAD_AHEAD) {
            return HEAD_FROM;
        }
        if (t < DRILL) {
            return hermite(HEAD_FROM, new Vec3(0.0, 2.0, 0.0), HEAD_OVER, new Vec3(0.0, 0.0, 3.0),
                    Ease.smooth((t - HEAD_AHEAD) / (DRILL - HEAD_AHEAD)));
        }
        if (t < IMPACT) {
            double u = (t - DRILL) / (IMPACT - DRILL);
            return HEAD_OVER.lerp(HEAD_DOWN, u * u);
        }
        if (t < GRIND) {
            return HEAD_DOWN.lerp(HEAD_DEEP, Ease.smooth((t - IMPACT) / (GRIND - IMPACT)));
        }
        if (t < HEAD_BACK) {
            return hermite(HEAD_DEEP, new Vec3(0.0, 12.0, 0.0), HEAD_HIGH, new Vec3(0.0, 0.0, -6.0),
                    Ease.smooth((t - GRIND) / (HEAD_BACK - GRIND)));
        }
        if (t < CLACK) {
            double u = (t - HEAD_BACK) / (CLACK - HEAD_BACK);
            return HEAD_HIGH.lerp(NECK, 1.0 - (1.0 - u) * (1.0 - u) * (1.0 - u));
        }
        return NECK.add(0.0, -0.06 * Ease.recoil(t - CLACK, 1.0, 1.0, 0.4), 0.0);
    }

    // The head spins like a drill; it slows to a stop facing ahead just as it locks onto the neck.
    public static double headSpin(double t) {
        double raw = spinRaw(Math.min(t, CLACK));
        return raw - SPIN_RESIDUE * Ease.smooth((t - (CLACK - 16.0)) / 16.0);
    }

    private static double spinRaw(double t) {
        double angle = 0.0;
        for (int k = 0; k + 3 < SPIN_KEYS.length; k += 2) {
            double from = SPIN_KEYS[k];
            double to = SPIN_KEYS[k + 2];
            if (t <= from) {
                break;
            }
            double end = Math.min(t, to);
            double rateFrom = SPIN_KEYS[k + 1];
            double rateTo = Mth.lerp((end - from) / (to - from), rateFrom, SPIN_KEYS[k + 3]);
            angle += (rateFrom + rateTo) * 0.5 * (end - from);
        }
        return angle;
    }

    public static float working(double t) {
        return t < DONE ? 0.9F : 0.0F;
    }
}
