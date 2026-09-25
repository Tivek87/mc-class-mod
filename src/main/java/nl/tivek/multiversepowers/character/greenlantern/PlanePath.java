package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

public record PlanePath(Vec3 start, Vec3 way, double drop, int attack, double end) {
    public static final int FORM = 40;
    public static final int FAIL = 36;
    public static final int DIVE = 64;
    public static final double SPEED = 0.18;
    public static final int MISSILE_FIRST = 20;
    public static final int HATCH_OPENS = 10;
    public static final int HATCH_CLOSES = 6;
    private static final int HATCH_SWING = 6;
    private static final int LOWERING = 5;
    public static final int JETS = 2;
    public static final int JET_BOOM = 11;
    public static final int JET_GONE = 20;
    public static final int JET_GROWS = 14;
    public static final double JET_WING = 24.0;
    public static final double[][] REACHES = { { 0.0, -0.55, 22.4 }, { 0.0, -3.8, 16.0 }, { 0.0, -2.62, 0.0 },
            { -28.0, 3.45, 0.4 }, { 28.0, 3.45, 0.4 }, { -16.4, -0.9, 8.6 }, { 16.4, -0.9, 8.6 },
            { -8.6, -0.9, 8.6 }, { 8.6, -0.9, 8.6 }, { -9.4, -5.3, 11.9 }, { 9.4, -5.3, 11.9 },
            { -4.3, -8.5, 10.0 }, { 4.3, -8.5, 10.0 } };
    public static final double DROP_PUSH = 0.12;
    public static final double GRAVITY = 0.07;
    public static final double DRAG = 0.985;
    public static final double PYLON_PUSH = 0.15;
    public static final int GUN_LAG = 4;
    public static final int GUN_HOLDS = 24;
    private static final double GUN_TURN = 0.11;
    private static final double GUN_PUSH = 0.025;
    private static final double GUN_PULL = 0.3;
    private static final double GUN_HIGHEST = 0.1;
    private static final double DIVE_PUSH = 5.0;
    private static final double FALL = 2.2;
    private static final double NOSE = 0.42;
    private static final double ROLL = 1.1;
    private static final double JET_RADIUS = 42.0;
    private static final double JET_SPEED = 1.5;
    private static final int JET_JOIN = 40;
    private static final double[] JET_HIGH = { 4.0, -9.0 };
    private static final double JET_KICK = 0.024;
    private static final double BANK_PULL = 0.08;
    private static final int BANK_ROLLS = 4;

    public double failTick() {
        return FORM + this.attack - FAIL;
    }

    public double diveTick() {
        return FORM + this.attack;
    }

    public double crashTick() {
        return this.diveTick() + DIVE * Mth.clamp(this.end, 0.0, 1.0);
    }

    public double down(double t) {
        return Mth.clamp((t - this.diveTick()) / DIVE, 0.0, 1.0);
    }

    public double failing(double t) {
        return Mth.clamp((t - this.failTick()) / FAIL, 0.0, 1.0);
    }

    private double gone(double t) {
        if (t < FORM) {
            // The integral of the smooth S-curve it speeds up along.
            double x = Math.max(0.0, t) / FORM;
            return SPEED * FORM * (x * x * x - 0.5 * x * x * x * x);
        }
        double level = SPEED * (FORM * 0.5 + Math.min(t, this.diveTick()) - FORM);
        double u = this.down(t);
        return level + SPEED * DIVE * (u + 0.5 * (DIVE_PUSH - 1.0) * u * u);
    }

    private double fallen(double t) {
        return this.drop * Math.pow(this.down(t), FALL);
    }

    public Vec3 at(double t) {
        double sway = 0.35 * Math.sin(t * 0.045) * ramp(t) * (1.0 - Ease.smooth(this.down(t) * 3.0));
        return this.start.add(this.way.scale(this.gone(t))).add(0.0, sway - this.fallen(t), 0.0);
    }

    public double pitch(double t) {
        double u = this.down(t);
        double stall = 0.06 * Ease.smooth(this.failing(t) * 2.0) * (1.0 - Ease.smooth(u / 0.35))
                * (0.7 + 0.3 * Math.sin(t * 0.19));
        if (u <= 0.0) {
            return -stall;
        }
        double ahead = SPEED * (1.0 + (DIVE_PUSH - 1.0) * u);
        double sink = this.drop * FALL * Math.pow(Math.max(u, 1.0E-3), FALL - 1.0) / DIVE;
        double falling = Math.atan2(sink, ahead);
        return falling * Ease.smoother(u / NOSE) - stall;
    }

    public double roll(double t) {
        double u = this.down(t);
        double f = Ease.smooth(this.failing(t));
        double sway = 0.05 * Math.sin(t * 0.06 + 1.0) * ramp(t);
        double lean = 0.14 * f;
        double shudder = 0.06 * f * (1.0 - Ease.smooth(u * 2.0)) * Math.sin(t * 0.31) * Math.sin(t * 0.13 + 0.7);
        return sway + lean + shudder + ROLL * Ease.smooth(u) * u + 0.05 * u * u * Math.sin(t * 0.4);
    }

    public Vec3[] axes(double t) {
        Vec3 flatRight = this.way.cross(Vectors.UP).normalize();
        double pitch = this.pitch(t);
        Vec3 forward = this.way.scale(Math.cos(pitch)).add(0.0, -Math.sin(pitch), 0.0);
        Vec3 up = flatRight.cross(forward).normalize();
        double roll = this.roll(t);
        double cos = Math.cos(roll);
        double sin = Math.sin(roll);
        Vec3 right = flatRight.scale(cos).subtract(up.scale(sin));
        Vec3 turnedUp = up.scale(cos).add(flatRight.scale(sin));
        return new Vec3[] { right, turnedUp, forward };
    }

    public Vec3 point(double t, double x, double y, double z) {
        Vec3[] axes = this.axes(t);
        return this.at(t).add(axes[0].scale(x)).add(axes[1].scale(y)).add(axes[2].scale(z));
    }

    public Vec3 velocity(double t) {
        return this.at(t + 0.5).subtract(this.at(t - 0.5));
    }

    public Vec3 crash() {
        double t = this.crashTick();
        Vec3 lowest = null;
        for (double[] part : REACHES) {
            Vec3 at = this.point(t, part[0], part[1], part[2]);
            if (lowest == null || at.y < lowest.y) {
                lowest = at;
            }
        }
        return lowest;
    }

    public Vec3 diveEnd() {
        return this.start.add(this.way.scale(this.gone(this.diveTick() + DIVE)));
    }

    private static double ramp(double t) {
        return Ease.smooth((t - FORM) / 40.0);
    }

    public boolean releases(int t, int every) {
        int since = t - FORM - MISSILE_FIRST;
        return since >= 0 && since % every == 0 && t < this.failTick();
    }

    public int lastRelease(double t, int every) {
        int first = FORM + MISSILE_FIRST;
        if (t < first) {
            return -1;
        }
        int release = first + (int) Math.floor((Math.min(t, this.failTick() - 1.0E-6) - first) / every) * every;
        return release >= first && release < this.failTick() ? release : -1;
    }

    public double hatch(double t, int every) {
        double open = 0.0;
        int first = Math.max(0,
                (int) Math.floor((t - HATCH_CLOSES - HATCH_SWING - FORM - MISSILE_FIRST) / every));
        for (int n = first;; n++) {
            double release = FORM + MISSILE_FIRST + (double) n * every;
            if (release >= this.failTick() || release > t + HATCH_OPENS) {
                break;
            }
            double d = t - release;
            double here = d < HATCH_SWING - HATCH_OPENS ? Ease.smooth((d + HATCH_OPENS) / HATCH_SWING)
                    : d <= HATCH_CLOSES ? 1.0 : 1.0 - Ease.smooth((d - HATCH_CLOSES) / HATCH_SWING);
            open = Math.max(open, here);
        }
        return open;
    }

    public double lowered(double t, int every) {
        int n = Math.max(0, (int) Math.ceil((t - FORM - MISSILE_FIRST) / every));
        double release = FORM + MISSILE_FIRST + (double) n * every;
        if (release >= this.failTick() || release - t > HATCH_OPENS) {
            return -1.0;
        }
        return Ease.smooth(1.0 - (release - t) / LOWERING);
    }

    public Vec3[] dropsOut(int release) {
        Vec3[] axes = this.axes(release);
        return new Vec3[] { this.point(release, 0.0, AirStrike.DROP_Y, AirStrike.BAY_Z),
                this.velocity(release).add(0.0, -DROP_PUSH, 0.0), axes[2], axes[1] };
    }

    public Vec3[] firedOff(int k, int side, int fired) {
        Vec3[] axes = this.jetAxes(k, fired);
        Vec3 right = axes[0].cross(axes[1]).normalize();
        Vec3 from = this.jetAt(k, fired).add(right.scale((side == 0 ? -1.0 : 1.0) * AirStrike.PYLON_X))
                .add(axes[1].scale(AirStrike.PYLON_Y)).add(axes[0].scale(AirStrike.PYLON_Z));
        Vec3 moving = this.jetVelocity(k, fired).add(axes[1].scale(-PYLON_PUSH));
        return new Vec3[] { from, moving, axes[0], axes[1] };
    }

    public static Vec3[] fall(Vec3[] state, boolean small) {
        Vec3 moving = state[1].scale(DRAG).add(0.0, -GRAVITY, 0.0);
        Vec3 nose = state[2].lerp(moving.normalize(), small ? 0.2 : 0.1).normalize();
        return new Vec3[] { state[0].add(moving), moving, nose, carried(state[3], nose) };
    }

    public static Vec3 carried(Vec3 up, Vec3 nose) {
        Vec3 flat = up.subtract(nose.scale(up.dot(nose)));
        return flat.lengthSqr() < 1.0E-10 ? up : flat.normalize();
    }

    public Vec3 pivot(int gun, double t) {
        return this.point(t, (gun == 0 ? -1.0 : 1.0) * AirStrike.GUN_X, AirStrike.GUN_Y, AirStrike.GUN_Z);
    }

    public Vec3 gunRest(int gun, double t) {
        Vec3[] axes = this.axes(t);
        return axes[2].scale(0.3).add(axes[0].scale((gun == 0 ? -1.0 : 1.0) * 0.8)).add(axes[1].scale(-0.55))
                .normalize();
    }

    public Vec3 gunGoal(int gun, double t, Vec3 at) {
        Vec3 want = at.subtract(this.pivot(gun, t));
        if (want.lengthSqr() < 1.0E-6) {
            return this.gunRest(gun, t);
        }
        want = want.normalize();
        Vec3 up = this.axes(t)[1];
        double high = want.dot(up);
        if (high <= GUN_HIGHEST) {
            return want;
        }
        Vec3 flat = want.subtract(up.scale(high));
        if (flat.lengthSqr() < 1.0E-6) {
            return this.gunRest(gun, t);
        }
        return flat.normalize().scale(Math.sqrt(1.0 - GUN_HIGHEST * GUN_HIGHEST)).add(up.scale(GUN_HIGHEST));
    }

    public static Vec3[] swing(Vec3 aim, Vec3 turning, Vec3 goal) {
        Vec3 axis = aim.cross(goal);
        double sin = axis.length();
        double angle = Math.atan2(sin, aim.dot(goal));
        Vec3 wanted = sin < 1.0E-9 ? Vec3.ZERO : axis.scale(Math.min(GUN_TURN, angle * GUN_PULL) / sin);
        Vec3 change = wanted.subtract(turning);
        double much = change.length();
        if (much > GUN_PUSH) {
            change = change.scale(GUN_PUSH / much);
        }
        Vec3 spin = turning.add(change);
        double turn = spin.length();
        Vec3 next = turn < 1.0E-9 ? aim : Vectors.spin(aim, spin.scale(1.0 / turn), turn).normalize();
        return new Vec3[] { next, spin };
    }

    public static final class Turret extends PlaneTurret {
        public Turret(PlanePath path, int gun) {
            super(path, gun);
        }
    }

    public double jetFrom(int k) {
        return FORM + 6 + 16 * k;
    }

    public boolean hasJet(int k) {
        return this.jetFrom(k) + JET_GROWS < this.failTick();
    }

    public double jetGrown(int k, double t) {
        return Ease.backOut((t - this.jetFrom(k)) / JET_GROWS);
    }

    public double jetsFled(double t) {
        return t - this.failTick();
    }

    public Vec3 jetAt(int k, double t) {
        double fled = this.jetsFled(t);
        if (fled <= 0.0) {
            return this.racing(k, t);
        }
        double from = this.failTick();
        Vec3 at = this.racing(k, from);
        Vec3 going = this.racing(k, from + 0.5).subtract(this.racing(k, from - 0.5));
        Vec3 out = at.subtract(this.orbitMiddle(from));
        out = new Vec3(out.x, 0.0, out.z);
        out = out.lengthSqr() < 1.0E-6 ? this.way : out.normalize();
        Vec3 off = going.normalize().scale(0.6).add(out.scale(0.6)).add(0.0, 0.45, 0.0).normalize();
        Vec3 escape = at.add(going.scale(fled)).add(off.scale(JET_KICK * fled * fled * fled));
        return this.racing(k, t).lerp(escape, Ease.smooth(fled / 8.0));
    }

    public Vec3[] jetAxes(int k, double t) {
        Vec3 forward = this.jetAt(k, t + 1.0).subtract(this.jetAt(k, t - 1.0));
        forward = forward.lengthSqr() < 1.0E-8 ? this.way : forward.normalize();
        Vec3 up = Vec3.ZERO;
        for (int s = -BANK_ROLLS; s <= BANK_ROLLS; s++) {
            up = up.add(this.banked(k, t + s).scale(BANK_ROLLS + 1 - Math.abs(s)));
        }
        up = up.subtract(forward.scale(up.dot(forward)));
        up = up.lengthSqr() < 1.0E-8 ? Vectors.UP : up.normalize();
        return new Vec3[] { forward, up };
    }

    private Vec3 banked(int k, double t) {
        Vec3 behind = this.jetAt(k, t - 1.0);
        Vec3 here = this.jetAt(k, t);
        Vec3 ahead = this.jetAt(k, t + 1.0);
        Vec3 forward = ahead.subtract(behind);
        forward = forward.lengthSqr() < 1.0E-8 ? this.way : forward.normalize();
        Vec3 turning = ahead.add(behind).subtract(here.scale(2.0));
        Vec3 across = turning.subtract(forward.scale(turning.dot(forward)));
        Vec3 up = Vectors.UP.scale(BANK_PULL).add(across);
        up = up.subtract(forward.scale(up.dot(forward)));
        return up.lengthSqr() < 1.0E-8 ? Vectors.UP : up.normalize();
    }

    public double jetSpeed(int k, double t) {
        return this.jetVelocity(k, t).length();
    }

    public Vec3 jetVelocity(int k, double t) {
        return this.jetAt(k, t + 0.5).subtract(this.jetAt(k, t - 0.5));
    }

    private Vec3 racing(int k, double t) {
        double from = this.jetFrom(k);
        double side = k == 0 ? -1.0 : 1.0;
        Vec3 right = this.way.cross(Vectors.UP).normalize();
        double since = t - from;
        double pace = (JET_SPEED - SPEED) / JET_JOIN;
        Vec3 wing = this.at(from).add(right.scale(side * JET_WING)).add(0.0, -3.0, 0.0);
        Vec3 off = wing.add(this.way.scale(SPEED * since + 0.5 * pace * since * Math.abs(since)));
        double turns = JET_SPEED / JET_RADIUS * (since - JET_JOIN * 0.5);
        double angle = side * (Math.PI / 3.0 - turns);
        double radius = JET_RADIUS * (1.0 + 0.16 * Math.sin(2.0 * angle + k * 1.3));
        double high = JET_HIGH[k] + 3.0 * Math.sin(1.5 * angle + k);
        Vec3 round = this.orbitMiddle(t).add(this.way.scale(radius * Math.cos(angle)))
                .add(right.scale(radius * Math.sin(angle))).add(0.0, high, 0.0);
        return off.lerp(round, Ease.smoother(since / JET_JOIN));
    }

    private Vec3 orbitMiddle(double t) {
        return this.start.add(this.way.scale(this.gone(Math.min(t, this.diveTick()))));
    }
}
