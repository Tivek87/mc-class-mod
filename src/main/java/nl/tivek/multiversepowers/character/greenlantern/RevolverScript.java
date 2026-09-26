package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Vectors;

abstract class RevolverScript extends HandDuoMotion {
    public static final int WAYS = 64;

    public static final int ARRIVES = HandPose.ARRIVES;
    public static final int OUT = ARRIVES + 14;
    public static final int WIGGLE_TO = OUT + 24;
    public static final int TOP_OPENS = OUT + 20;
    public static final int HAT_REACH = TOP_OPENS + 10;
    public static final int HAT_DIP = HAT_REACH + 7;
    public static final int HAT_PINCH = HAT_DIP + 3;
    public static final int HAT_OUT = HAT_PINCH + 10;
    public static final int HAT_OVER = HAT_OUT + 14;
    public static final int HAT_ON = HAT_OVER + 6;
    public static final int HAT_TAP = HAT_ON + 7;
    public static final int SHAKE_FROM = HAT_ON + 10;
    public static final int SHAKE_TO = SHAKE_FROM + 34;
    public static final int GUN_FORM = SHAKE_TO + 6;
    public static final int[] PEWS = { GUN_FORM + 8, GUN_FORM + 15, GUN_FORM + 21, GUN_FORM + 30, GUN_FORM + 37 };
    public static final int BOLT_TICKS = 3;
    public static final int BLOW = GUN_FORM + 46;
    public static final int HAT_FLICK = BLOW + 18;
    public static final int HAT_CATCH = HAT_FLICK + 8;
    public static final int HAT_THROW = HAT_CATCH + 10;
    public static final int HAT_IN = HAT_THROW + 10;
    public static final int PARTS_REACH = HAT_IN + 2;
    public static final int PARTS_DIP = PARTS_REACH + 8;
    public static final int PARTS_GRAB = PARTS_DIP + 4;
    public static final int PARTS_OUT = PARTS_GRAB + 10;
    public static final int PARTS_FLICK = PARTS_OUT + 8;
    public static final int CLAW_UP = PARTS_FLICK + 2;
    public static final int CLAW_CLAMP = CLAW_UP + 25;
    public static final int CLAW_OUT = CLAW_CLAMP + 13;
    public static final int CLAW_DROP = CLAW_OUT + 10;
    public static final int CONDUCT = CLAW_DROP + 6;
    public static final int FLY_TICKS = 6;
    public static final int FRAME = 0;
    public static final int GRIP = 1;
    public static final int BARREL = 2;
    public static final int CYLINDER = 3;
    public static final int HAMMER = 4;
    public static final int GUARD = 5;
    public static final int PARTS = 6;
    public static final int[] BUILT = { CONDUCT + 6, CONDUCT + 13, CONDUCT + 20, CONDUCT + 27, CONDUCT + 34,
            CONDUCT + 41 };
    public static final int WHOLE = CONDUCT + 41;
    public static final int GRABS = WHOLE + 19;
    public static final int LOAD_POSE = GRABS + 12;
    public static final int[] BULLETS_IN_HAND = { GRABS + 8, GRABS + 10, GRABS + 12, GRABS + 14, GRABS + 16,
            GRABS + 18 };
    public static final int CYL_OPEN = GRABS + 22;
    public static final int[] LOADS = { CYL_OPEN + 4, CYL_OPEN + 6, CYL_OPEN + 8, CYL_OPEN + 10, CYL_OPEN + 12,
            CYL_OPEN + 14 };
    public static final int CYL_CLOSE = CYL_OPEN + 22;
    public static final int AIMS = CYL_CLOSE + 4;
    public static final int[] SHOTS = { AIMS + 10, AIMS + 22, AIMS + 33, AIMS + 43, AIMS + 52, AIMS + 61 };
    public static final int[] DRY = { AIMS + 74, AIMS + 82 };
    public static final int PRESENTS = SHOTS[5] + 6;
    public static final int SPIN_FROM = DRY[1] + 8;
    public static final int SPIN_TO = SPIN_FROM + 8;
    public static final int[] SLAMS = { SPIN_TO + 16, SPIN_TO + 30, SPIN_TO + 44 };
    public static final int TOSS = SLAMS[2] + 18;
    public static final int GUN_IN = TOSS + 14;
    public static final int TOP_SHUT = GUN_IN + 2;
    public static final int WIPE = TOP_SHUT + 6;
    public static final int WIPED = WIPE + 12;
    public static final int TIRED = WIPED + 6;
    public static final int HIGH_FIVE = TIRED + 32;
    public static final int SALUTE = HIGH_FIVE + 16;
    public static final int RETRACT = SALUTE + 14;
    public static final int HANDS_GONE = RETRACT + 12;
    public static final int LIFE = HANDS_GONE + 14;

    static final double SIDE_RADIUS = 2.5;
    public static final Vec3 TOP = new Vec3(0.0, 9.6, 5.2);
    public static final Vec3 TOP_NORMAL = new Vec3(0.0, 0.6, -0.8);
    static final double TOP_RADIUS = 3.2;
    public static final Vec3 SLAM_AT = new Vec3(2.6, 0.0, -1.6);
    public static final Vec3 INDEX_TIP = new Vec3(-1.12, 5.95, 0.0);

    public record Stage(Vec3 base, Vec3 way, Vec3 side, double scale) {
        public static Stage of(Vec3 base, int variant, double scale) {
            Vec3 way = RevolverScript.way(variant);
            return new Stage(base, way, Vectors.UP.cross(way).normalize(), Math.max(1.0E-3, scale));
        }

        public Vec3 dir(Vec3 local) {
            return this.side.scale(local.x).add(0.0, local.y, 0.0).add(this.way.scale(local.z));
        }

        public Vec3 point(Vec3 local) {
            return this.base.add(this.dir(local).scale(this.scale));
        }

        public Vec3 local(Vec3 world) {
            Vec3 d = world.subtract(this.base).scale(1.0 / this.scale);
            return this.localDir(d);
        }

        public Vec3 localDir(Vec3 world) {
            return new Vec3(world.dot(this.side), world.y, world.dot(this.way));
        }
    }

    public static int variant(Vec3 way) {
        double theta = Math.atan2(way.x, way.z);
        return Math.floorMod((int) Math.round(theta / (Math.PI * 2.0 / WAYS)), WAYS);
    }

    public static Vec3 way(int variant) {
        double theta = Math.floorMod(variant, WAYS) * Math.PI * 2.0 / WAYS;
        return new Vec3(Math.sin(theta), 0.0, Math.cos(theta));
    }

    static Vec3 above(double out, double across) {
        return TOP.add(TOP_NORMAL.scale(out)).add(across, 0.0, 0.0);
    }

    // The right hand's thumb lies along up x palm; the left hand, built mirrored, along palm x up.
    static Vec3 palmFor(boolean right, Vec3 up, Vec3 thumb) {
        Vec3 u = up.normalize();
        Vec3 t = thumb.subtract(u.scale(thumb.dot(u))).normalize();
        return right ? t.cross(u) : u.cross(t);
    }

    static Vec3 wristFor(Vec3 point, Vec3 inHand, Vec3 up, Vec3 palm, boolean right) {
        Vec3 u = up.normalize();
        Vec3 f = palm.subtract(u.scale(palm.dot(u))).normalize();
        Vec3 r = f.cross(u);
        double x = right ? inHand.x : -inHand.x;
        return point.subtract(r.scale(x)).subtract(u.scale(inHand.y)).subtract(f.scale(inHand.z));
    }

    // Keeps a pointing direction out of the ground and off the sky, whichever way it turns round.
    public static Vec3 steady(Vec3 way) {
        double flat = Math.sqrt(way.x * way.x + way.z * way.z);
        if (flat < 1.0E-6) {
            return new Vec3(0.0, 0.0, -1.0);
        }
        double pitch = Mth.clamp(Math.atan2(way.y, flat), -0.5, 0.9);
        return new Vec3(way.x / flat * Math.cos(pitch), Math.sin(pitch), way.z / flat * Math.cos(pitch));
    }

    public static int shotsFired(double t) {
        int fired = 0;
        for (int shot : SHOTS) {
            if (t >= shot) {
                fired++;
            }
        }
        return fired;
    }
}
