package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.RevolverScript.*;

public final class RevolverGun {
    public static final Vec3 MUZZLE = new Vec3(0.0, 0.9, 7.6);
    public static final Vec3 GRIP_HOLE = new Vec3(0.0, -1.55, -1.05);
    public static final Vec3 GRIP_AXIS = new Vec3(0.0, -2.9, -1.4).normalize();
    public static final Vec3 BARREL_HOLD = new Vec3(0.0, 0.9, 4.4);
    public static final Vec3 BUTT = new Vec3(0.0, -3.3, -1.95);
    public static final Vec3 GUARD_PIVOT = new Vec3(0.0, -0.75, 0.55);
    public static final Vec3 CYLINDER_AT = new Vec3(0.0, 0.75, 0.6);
    public static final Vec3 CRANE_PIVOT = new Vec3(-0.35, -0.05, 0.0);
    public static final double CRANE_OPEN = 1.45;
    public static final Vec3 CENTER = new Vec3(0.0, -0.4, 2.6);

    private static final Vec3 FLOAT_AT = new Vec3(0.0, 8.8, -2.0);
    private static final Pose FLOAT = centred(FLOAT_AT, new Vec3(1.0, 0.0, 0.0), Vectors.UP);
    private static final double SHOW_TURN = Math.PI - 0.5;
    private static final Pose LOAD = load();
    private static final Vec3 AIM_FROM = new Vec3(3.2, 7.4, -1.6);
    private static final Pose PRESENT = upright(AIM_FROM, new Vec3(-0.55, 0.2, -0.8));
    private static final Pose SPUN = spun(PRESENT, 1.0);
    private static final Vec3 SLAM_WAY = new Vec3(0.58, 0.81, 0.0).normalize();
    private static final Pose IMPACT = impact();
    private static final double[] RAISE = { 1.0, 1.15, 1.35 };
    private static final Pose WIND = held(new Vec3(3.9, 9.4, 0.4), new Vec3(0.3, 0.2, 0.93), BARREL_HOLD);
    private static final Vec3 TOSS_TOP = new Vec3(1.8, 14.6, 1.6);
    private static final double TUMBLE = Math.PI * 3.0;

    private RevolverGun() {
    }

    public record Pose(Vec3 origin, Vec3 forward, Vec3 up) {
        public Vec3 right() {
            return this.forward.cross(this.up);
        }

        public Vec3 at(Vec3 local) {
            return this.origin.add(this.right().scale(local.x)).add(this.up.scale(local.y))
                    .add(this.forward.scale(local.z));
        }

        Pose turned(Vec3 pivot, Vec3 axis, double angle) {
            if (angle == 0.0) {
                return this;
            }
            return new Pose(pivot.add(Vectors.spin(this.origin.subtract(pivot), axis, angle)),
                    Vectors.spin(this.forward, axis, angle), Vectors.spin(this.up, axis, angle));
        }

        Pose moved(Vec3 by) {
            return new Pose(this.origin.add(by), this.forward, this.up);
        }
    }

    public record Gun(Vec3 origin, Vec3 right, Vec3 up, Vec3 forward, double scale) {
        public Vec3 at(Vec3 local) {
            return this.origin.add(this.right.scale(local.x * this.scale)).add(this.up.scale(local.y * this.scale))
                    .add(this.forward.scale(local.z * this.scale));
        }
    }

    public static Gun at(Stage stage, Vec3 aim, double t) {
        Pose pose = pose(t, stage.local(aim));
        Vec3 forward = stage.dir(pose.forward()).normalize();
        Vec3 up = stage.dir(pose.up()).normalize();
        return new Gun(stage.point(pose.origin()), forward.cross(up), up, forward, stage.scale());
    }

    public static boolean whole(double t) {
        return t >= WHOLE && t < GUN_IN + 1.0;
    }

    public static Pose pose(double t, Vec3 aim) {
        if (t < GRABS) {
            return floating(t);
        }
        if (t < LOAD_POSE) {
            return blend(floating(GRABS), LOAD, Ease.smoother((t - GRABS) / (LOAD_POSE - GRABS)))
                    .moved(new Vec3(0.0, 0.5 * Math.sin(Math.PI * (t - GRABS) / (LOAD_POSE - GRABS)), 0.0));
        }
        if (t < CYL_CLOSE + 2) {
            Vec3 hold = LOAD.at(GRIP_HOLE);
            double flick = -0.35 * kick(t - CYL_OPEN, 1.0) + 0.3 * kick(t - CYL_CLOSE, 0.8);
            return LOAD.turned(hold, LOAD.forward(), flick);
        }
        if (t < AIMS + 4) {
            double u = Ease.smoother((t - CYL_CLOSE - 2) / (AIMS + 2 - CYL_CLOSE));
            return blend(LOAD, aimed(t, aim), u);
        }
        if (t < PRESENTS) {
            return aimed(t, aim);
        }
        if (t < SPIN_FROM) {
            Pose pose = blend(aimed(t, aim), PRESENT, Ease.smoother((t - PRESENTS) / 6.0));
            double shake = 0.0;
            for (int dry : DRY) {
                shake += 0.12 * wobble(t - dry, 2.4, 0.5, 0.4);
            }
            return pose.turned(pose.at(GRIP_HOLE), pose.forward(), shake);
        }
        if (t < SPIN_TO) {
            return spun(PRESENT, Ease.smoother((t - SPIN_FROM) / (SPIN_TO - SPIN_FROM)));
        }
        if (t < TOSS) {
            return slamming(t);
        }
        return flying(t);
    }

    private static Pose floating(double t) {
        double calm = 1.0 - Ease.smoother((t - GRABS + 8.0) / 8.0);
        double bob = 0.12 * Math.sin(0.21 * t) * calm;
        double rock = 0.06 * Math.sin(0.13 * t + 1.0) * calm;
        // Built with its cylinder side to the audience, then turned round to point away from the right hand,
        // the way that hand can hold it.
        double show = SHOW_TURN * Ease.smoother((t - WHOLE - 2.0) / 12.0);
        Pose pose = FLOAT.moved(new Vec3(0.0, bob, 0.0)).turned(FLOAT_AT, Vectors.UP, show);
        return pose.turned(pose.at(CENTER), pose.forward(), rock);
    }

    private static Pose aimed(double t, Vec3 aim) {
        Vec3 way = aim.subtract(AIM_FROM);
        if (way.lengthSqr() < 1.0E-4) {
            way = PRESENT.forward();
        }
        way = steady(way);
        Pose pose = upright(AIM_FROM.add(way.scale(0.6)), way);
        for (int shot : SHOTS) {
            double k = kick(t - shot, 1.3);
            if (k > 0.0) {
                pose = pose.turned(pose.at(GRIP_HOLE), pose.right(), 0.6 * k)
                        .moved(pose.forward().scale(-0.7 * k).add(0.0, 0.25 * k, 0.0));
            }
        }
        return pose;
    }

    private static Pose slamming(double t) {
        Pose last = SPUN;
        double lastAt = SPIN_TO;
        for (int i = 0; i < SLAMS.length; i++) {
            int hit = SLAMS[i];
            Pose raised = raised(RAISE[i]);
            if (t < hit - 4) {
                return blend(last, raised, Ease.smoother((t - lastAt) / (hit - 4 - lastAt)));
            }
            if (t < hit) {
                return blend(raised, IMPACT, chop((t - hit + 4) / 4.0));
            }
            if (t < hit + 3) {
                Vec3 hold = IMPACT.at(BARREL_HOLD);
                return IMPACT.turned(hold, IMPACT.right(), 0.12 * kick(t - hit, 0.8));
            }
            last = IMPACT;
            lastAt = hit + 3;
        }
        return blend(IMPACT, WIND, Ease.smoother((t - lastAt) / (TOSS - lastAt)));
    }

    private static Pose flying(double t) {
        double u = Mth.clamp((t - TOSS) / (GUN_IN - TOSS), 0.0, 1.0);
        Vec3 from = WIND.at(CENTER);
        Vec3 to = TOP.subtract(TOP_NORMAL.scale(3.0));
        Pose pose = WIND.turned(from, WIND.right(), TUMBLE * u);
        return pose.moved(bezier(from, TOSS_TOP, to, u).subtract(from));
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, double u) {
        double v = 1.0 - u;
        return a.scale(v * v).add(b.scale(2.0 * u * v)).add(c.scale(u * u));
    }

    private static Pose raised(double how) {
        Vec3 hold = IMPACT.at(BARREL_HOLD);
        return IMPACT.turned(hold, IMPACT.right(), 1.45 * how).moved(new Vec3(0.7, 3.0, 0.4).scale(how));
    }

    private static Pose spun(Pose from, double u) {
        return from.turned(from.at(GUARD_PIVOT), from.right(), -Math.PI * u);
    }

    private static Pose impact() {
        Vec3 up = new Vec3(-SLAM_WAY.y, SLAM_WAY.x, 0.0);
        Vec3 origin = SLAM_AT.subtract(up.scale(BUTT.y)).subtract(SLAM_WAY.scale(BUTT.z));
        return new Pose(origin, SLAM_WAY, up);
    }

    private static Pose load() {
        Vec3 forward = new Vec3(-0.35, 0.9, 0.25).normalize();
        Vec3 left = new Vec3(0.0, 0.0, -1.0);
        left = left.subtract(forward.scale(left.dot(forward))).normalize();
        return new Pose(new Vec3(1.2, 7.2, -1.4), forward, forward.cross(left));
    }

    private static Pose upright(Vec3 origin, Vec3 way) {
        Vec3 forward = way.normalize();
        Vec3 up = Vectors.UP.subtract(forward.scale(forward.y));
        up = up.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : up.normalize();
        return new Pose(origin, forward, up);
    }

    private static Pose centred(Vec3 center, Vec3 forward, Vec3 up) {
        Pose pose = new Pose(Vec3.ZERO, forward, up);
        return pose.moved(center.subtract(pose.at(CENTER)));
    }

    private static Pose held(Vec3 hand, Vec3 forward, Vec3 at) {
        Pose pose = upright(Vec3.ZERO, forward);
        return pose.moved(hand.subtract(pose.at(at)));
    }

    static Pose blend(Pose a, Pose b, double u) {
        if (u <= 0.0) {
            return a;
        }
        if (u >= 1.0) {
            return b;
        }
        Vec3[] from = { a.forward(), a.up() };
        Vec3 turn = Vectors.turn(Vectors.frame(a.forward(), a.up()), Vectors.frame(b.forward(), b.up())).scale(u);
        Vec3 center = a.at(CENTER).lerp(b.at(CENTER), u);
        Pose pose = new Pose(Vec3.ZERO, Vectors.turned(from[0], turn), Vectors.turned(from[1], turn));
        return pose.moved(center.subtract(pose.at(CENTER)));
    }

    // The right hand's fist round the grip: thumb up the grip, fingers wrapped towards the front strap.
    static Vec3[] pistolGrip(Pose gun) {
        Vec3 axis = dir(gun, GRIP_AXIS);
        Vec3 up = gun.forward().subtract(axis.scale(gun.forward().dot(axis))).normalize();
        return fist(gun.at(GRIP_HOLE), up, axis.scale(-1.0));
    }

    // The fist round the barrel, thumb towards the muzzle, the heavy grip hanging below the little finger.
    static Vec3[] barrelGrip(Pose gun, Vec3 from) {
        Vec3 hole = gun.at(BARREL_HOLD);
        Vec3 toward = hole.subtract(from);
        Vec3 up = toward.subtract(gun.forward().scale(toward.dot(gun.forward()))).normalize();
        return fist(hole, up, gun.forward());
    }

    private static Vec3[] fist(Vec3 hole, Vec3 up, Vec3 thumb) {
        Vec3 palm = thumb.cross(up).normalize();
        Vec3 wrist = hole.subtract(up.scale(HandDuoScript.FIST_HOLE.y)).subtract(palm.scale(HandDuoScript.FIST_HOLE.z));
        return new Vec3[] { wrist, up, palm };
    }

    private static Vec3 dir(Pose pose, Vec3 local) {
        return pose.right().scale(local.x).add(pose.up().scale(local.y)).add(pose.forward().scale(local.z));
    }

    public static Vec3 chamberCenter(Pose gun, double crane) {
        return swung(gun, gun.at(CYLINDER_AT), crane);
    }

    public static Vec3 chamber(Pose gun, double crane, double turn, int k, double along) {
        double angle = turn + Math.PI / 3.0 * k;
        Vec3 at = gun.at(CYLINDER_AT.add(0.55 * Math.sin(angle), 0.55 * Math.cos(angle), along));
        return swung(gun, at, crane);
    }

    // The crane swings the cylinder out to the gun's left; (right, up, forward) is left-handed, so the turn is negative.
    private static Vec3 swung(Pose gun, Vec3 at, double crane) {
        if (crane <= 0.0) {
            return at;
        }
        Vec3 pivot = gun.at(new Vec3(CRANE_PIVOT.x, CRANE_PIVOT.y, gun.forward().dot(at.subtract(gun.origin()))));
        return pivot.add(Vectors.spin(at.subtract(pivot), gun.forward(), -CRANE_OPEN * crane));
    }

    public static double crane(double t) {
        double open = Ease.spring(t - CYL_OPEN, 1.0, 0.55);
        return Math.max(0.0, open) * (1.0 - Ease.smoother((t - CYL_CLOSE + 1.2) / 1.2));
    }

    public static double hammer(double t) {
        double cocked = 0.0;
        for (int fall : falls()) {
            cocked += Ease.smoother((t - fall + 8.0) / 4.0) * (1.0 - Ease.smoother((t - fall + 0.3) / 0.6));
        }
        return Mth.clamp(cocked, 0.0, 1.0);
    }

    public static double cylinderTurn(double t) {
        double turn = Math.PI * 4.0 * Math.max(0.0, 1.0 - Math.exp(-Math.max(0.0, t - CYL_CLOSE) / 4.0));
        for (int fall : falls()) {
            turn += Math.PI / 3.0 * Ease.smoother((t - fall + 8.0) / 4.0);
        }
        return turn;
    }

    private static int[] falls() {
        int[] falls = new int[SHOTS.length + DRY.length];
        System.arraycopy(SHOTS, 0, falls, 0, SHOTS.length);
        System.arraycopy(DRY, 0, falls, SHOTS.length, DRY.length);
        return falls;
    }
}
