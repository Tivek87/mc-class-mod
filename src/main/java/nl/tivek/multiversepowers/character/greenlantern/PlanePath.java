package nl.tivek.multiversepowers.character.greenlantern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The way the plane of the air strike flies (see {@link AirStrike}), worked out alike by the server and by every client
 * from how long ago it was called, so a thing this big glides smoothly however unevenly the updates come in. The same
 * goes for the two jets that fly with it, for the hatch in its belly and the missiles that drop out of it and off the
 * jets' wings, and for how its miniguns swing (see {@link Turret}).
 * <ul>
 * <li><b>Taking shape</b> ({@link #FORM} ticks): it grows out of the ring's light high over its maker and sets off
 * slowly, picking up speed.</li>
 * <li><b>Attacking</b> ({@code attack} ticks): it drones on in one straight line at its slow, steady speed, level, only
 * swaying a little in the air. Its jets take shape beside it and race round it.</li>
 * <li><b>Failing</b> (the last {@link #FAIL} ticks of the attack): an engine bursts, it shudders and leans over to that
 * side, its nose lifting a little as it fights to stay up; its jets break away and race off.</li>
 * <li><b>The crash</b> (at most {@link #DIVE} ticks): its nose drops, slowly at first, and it plunges into the ground,
 * rolling as it falls, faster and faster, until the first of its parts strikes: {@link #crashTick}.</li>
 * </ul>
 * Its way, its speed, its pitch and its roll all run on from one phase into the next without a jolt.
 *
 * @param start where it takes shape, high in the air
 * @param way   the way it flies, flat and one long
 * @param drop  how far below {@code start} the ground is where a full dive would end, in blocks
 * @param attack how many ticks it drones on and fires before it goes down
 * @param end   how far through its dive it strikes the ground, 0 to 1: less than 1 when something stands in its way
 */
public record PlanePath(Vec3 start, Vec3 way, double drop, int attack, double end) {
    /** Ticks from the call until it flies at its full speed: it takes shape meanwhile. */
    public static final int FORM = 40;
    /** Ticks from the moment an engine bursts until its nose drops: the end of its attack. */
    public static final int FAIL = 36;
    /** Ticks a full dive takes, from the moment its nose starts to drop to the ground. */
    public static final int DIVE = 64;
    /** How fast it flies, in blocks per tick: a big, slow plane. */
    public static final double SPEED = 0.18;
    /** Ticks after it has taken shape its first missile drops out of the hatch. */
    public static final int MISSILE_FIRST = 20;
    /** Ticks before a missile drops that the hatch starts to open, and after it that the hatch starts to close. */
    public static final int HATCH_OPENS = 10;
    public static final int HATCH_CLOSES = 6;
    // How long the hatch's doors take to swing open or shut, and how long a missile takes to be lowered out of the bay,
    // in ticks.
    private static final int HATCH_SWING = 6;
    private static final int LOWERING = 5;
    /** How many jets fly with it. */
    public static final int JETS = 2;
    /** Ticks after the jets break away that they break the sound barrier, and that they are gone in a flash. */
    public static final int JET_BOOM = 11;
    public static final int JET_GONE = 20;
    /** Ticks a jet takes to grow out of the light. */
    public static final int JET_GROWS = 14;
    /** How far out under the plane's wing a jet takes shape, in blocks to its side (3 blocks under it). */
    public static final double JET_WING = 24.0;
    /**
     * The parts of it that reach out furthest below it and to its sides, in blocks to its right, up and ahead of its
     * middle: the tip of its nose, the sensor ball under it, the bottom of its belly, the tips of its wings, the
     * lowest tips of its four propellers and the muzzles of its two miniguns (at rest, and pointing straight down).
     * Its dive ends as the first of them strikes the ground.
     */
    public static final double[][] REACHES = { { 0.0, -0.55, 22.4 }, { 0.0, -3.8, 16.0 }, { 0.0, -2.62, 0.0 },
            { -28.0, 3.45, 0.4 }, { 28.0, 3.45, 0.4 }, { -16.4, -0.9, 8.6 }, { 16.4, -0.9, 8.6 },
            { -8.6, -0.9, 8.6 }, { 8.6, -0.9, 8.6 }, { -9.4, -5.3, 11.9 }, { 9.4, -5.3, 11.9 },
            { -4.3, -8.5, 10.0 }, { 4.3, -8.5, 10.0 } };
    /**
     * A missile out of the hatch drops this fast (blocks per tick, down), falls with this much gravity and keeps this
     * much of its speed every tick while its motor is dead.
     */
    public static final double DROP_PUSH = 0.12;
    public static final double GRAVITY = 0.07;
    public static final double DRAG = 0.985;
    /** A small missile drops off a jet's pylon this fast, down along the jet's up. */
    public static final double PYLON_PUSH = 0.15;
    /**
     * How long after a minigun is told where to fire next it starts to swing there, in ticks (every client has heard of
     * it by then), and how long it keeps pointing there after its last round before it swings back to rest.
     */
    public static final int GUN_LAG = 4;
    public static final int GUN_HOLDS = 24;
    // How fast a minigun swings at most, in radians per tick; how much faster or slower it may swing from one tick to
    // the next; and how hard it pulls towards where it is told to point (its swing per tick, next to how far off it
    // is).
    private static final double GUN_TURN = 0.11;
    private static final double GUN_PUSH = 0.025;
    private static final double GUN_PULL = 0.3;
    // How far up a minigun may point at most (the up of its aim, next to the plane's up): never up into its own wing.
    private static final double GUN_HIGHEST = 0.1;
    // How much faster it goes forward at the end of the dive than while it drones on, and how steeply the fall
    // speeds up (the power of the part of the dive that has gone).
    private static final double DIVE_PUSH = 5.0;
    private static final double FALL = 2.2;
    // How long the nose takes to drop into the way it falls, as a part of the dive, and how far it rolls over.
    private static final double NOSE = 0.42;
    private static final double ROLL = 1.1;
    // The jets: how far round the plane they race, how fast (blocks per tick), how long they take from peeling off its
    // wing to racing round it, how high over or under it each flies, and how hard they speed away once they break off.
    private static final double JET_RADIUS = 42.0;
    private static final double JET_SPEED = 1.5;
    private static final int JET_JOIN = 40;
    private static final double[] JET_HIGH = { 4.0, -9.0 };
    private static final double JET_KICK = 0.024;
    // How strongly gravity pulls, next to how hard a jet turns, for how far it banks into a turn; and over how many
    // ticks either side it rolls into a bank.
    private static final double BANK_PULL = 0.08;
    private static final int BANK_ROLLS = 4;

    /** The tick an engine bursts: the jets break away, its hatch stays shut and it starts to struggle. */
    public double failTick() {
        return FORM + this.attack - FAIL;
    }

    /** The tick its nose drops and it starts to go down. */
    public double diveTick() {
        return FORM + this.attack;
    }

    /** The tick it strikes the ground. */
    public double crashTick() {
        return this.diveTick() + DIVE * Mth.clamp(this.end, 0.0, 1.0);
    }

    /** 0 while it flies level, rising to 1 at the end of a full dive: how far through its dive it is. */
    public double down(double t) {
        return Mth.clamp((t - this.diveTick()) / DIVE, 0.0, 1.0);
    }

    /** 0 until an engine bursts, rising to 1 as its nose drops: how far it has got in its struggle. */
    public double failing(double t) {
        return Mth.clamp((t - this.failTick()) / FAIL, 0.0, 1.0);
    }

    /** How far along its way it has come, in blocks, {@code t} ticks after the call. */
    private double gone(double t) {
        if (t < FORM) {
            // Speeding up smoothly: the integral of the smooth S-curve, half of FORM at the end of it.
            double x = Math.max(0.0, t) / FORM;
            return SPEED * FORM * (x * x * x - 0.5 * x * x * x * x);
        }
        double level = SPEED * (FORM * 0.5 + Math.min(t, this.diveTick()) - FORM);
        double u = this.down(t);
        // Diving it keeps going forward, faster and faster as it falls.
        return level + SPEED * DIVE * (u + 0.5 * (DIVE_PUSH - 1.0) * u * u);
    }

    /** How far below its height it has fallen, in blocks. */
    private double fallen(double t) {
        return this.drop * Math.pow(this.down(t), FALL);
    }

    /** Where it is, {@code t} ticks after the call. */
    public Vec3 at(double t) {
        // A gentle rise and fall in the air while it drones on, gone once it plunges.
        double sway = 0.35 * Math.sin(t * 0.045) * ramp(t) * (1.0 - Ease.smooth(this.down(t) * 3.0));
        return this.start.add(this.way.scale(this.gone(t))).add(0.0, sway - this.fallen(t), 0.0);
    }

    /**
     * How far its nose points down, in radians: 0 while it flies level, a little below 0 while it fights to stay up
     * after an engine burst.
     */
    public double pitch(double t) {
        double u = this.down(t);
        // Losing an engine its nose comes up a little, bobbing, until it drops for good.
        double stall = 0.06 * Ease.smooth(this.failing(t) * 2.0) * (1.0 - Ease.smooth(u / 0.35))
                * (0.7 + 0.3 * Math.sin(t * 0.19));
        if (u <= 0.0) {
            return -stall;
        }
        // The way it really falls, which the nose follows as it drops.
        double ahead = SPEED * (1.0 + (DIVE_PUSH - 1.0) * u);
        double sink = this.drop * FALL * Math.pow(Math.max(u, 1.0E-3), FALL - 1.0) / DIVE;
        double falling = Math.atan2(sink, ahead);
        return falling * Ease.smoother(u / NOSE) - stall;
    }

    /**
     * How far it leans about the way it flies, in radians (positive: its right wing down): a slow sway while it drones
     * on, leaning over to the side of the engine that burst and shuddering as it struggles, and a roll over as it
     * plunges.
     */
    public double roll(double t) {
        double u = this.down(t);
        double f = Ease.smooth(this.failing(t));
        double sway = 0.05 * Math.sin(t * 0.06 + 1.0) * ramp(t);
        double lean = 0.14 * f;
        double shudder = 0.06 * f * (1.0 - Ease.smooth(u * 2.0)) * Math.sin(t * 0.31) * Math.sin(t * 0.13 + 0.7);
        return sway + lean + shudder + ROLL * Ease.smooth(u) * u + 0.05 * u * u * Math.sin(t * 0.4);
    }

    /** Its right, up and forward, one long each, as it flies: pitched down and rolled in its dive. */
    public Vec3[] axes(double t) {
        Vec3 flatRight = this.way.cross(Vectors.UP).normalize();
        double pitch = this.pitch(t);
        Vec3 forward = this.way.scale(Math.cos(pitch)).add(0.0, -Math.sin(pitch), 0.0);
        Vec3 up = flatRight.cross(forward).normalize();
        double roll = this.roll(t);
        double cos = Math.cos(roll);
        double sin = Math.sin(roll);
        // Turned about the way it flies: its right wing goes down for a positive roll.
        Vec3 right = flatRight.scale(cos).subtract(up.scale(sin));
        Vec3 turnedUp = up.scale(cos).add(flatRight.scale(sin));
        return new Vec3[] { right, turnedUp, forward };
    }

    /** A point of the plane, given in blocks to its right, up and ahead of its middle, out in the world. */
    public Vec3 point(double t, double x, double y, double z) {
        Vec3[] axes = this.axes(t);
        return this.at(t).add(axes[0].scale(x)).add(axes[1].scale(y)).add(axes[2].scale(z));
    }

    /** How fast it moves {@code t} ticks after the call, in blocks per tick. */
    public Vec3 velocity(double t) {
        return this.at(t + 0.5).subtract(this.at(t - 0.5));
    }

    /**
     * Where it strikes the ground: the lowest of the parts that reach out furthest (see {@link #REACHES}) the moment it
     * strikes.
     */
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

    /** Where it would be at the end of a full dive, on the ground under its height: to look for that ground. */
    public Vec3 diveEnd() {
        return this.start.add(this.way.scale(this.gone(this.diveTick() + DIVE)));
    }

    /** 0 at the call, 1 once it has taken shape: how far its sway has come in. */
    private static double ramp(double t) {
        return Ease.smooth((t - FORM) / 40.0);
    }

    // ---- The hatch and its missiles ----

    /** True when a missile drops out of the hatch on this tick, with one every {@code every} ticks. */
    public boolean releases(int t, int every) {
        int since = t - FORM - MISSILE_FIRST;
        return since >= 0 && since % every == 0 && t < this.failTick();
    }

    /** The tick the latest missile dropped out of the hatch on {@code t} or before, or -1 when none has yet. */
    public int lastRelease(double t, int every) {
        int first = FORM + MISSILE_FIRST;
        if (t < first) {
            return -1;
        }
        int release = first + (int) Math.floor((Math.min(t, this.failTick() - 1.0E-6) - first) / every) * every;
        return release >= first && release < this.failTick() ? release : -1;
    }

    /**
     * How far the hatch in its belly is open, 0 (shut) to 1: it opens before every missile drops out of it and shuts
     * again once the missile is clear; with missiles close after each other it stays open.
     */
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

    /**
     * How far the next missile has been lowered out of the open hatch, 0 (still in the bay) to 1 (hanging under the
     * belly, about to drop), or -1 when none waits in the open hatch.
     */
    public double lowered(double t, int every) {
        int n = Math.max(0, (int) Math.ceil((t - FORM - MISSILE_FIRST) / every));
        double release = FORM + MISSILE_FIRST + (double) n * every;
        if (release >= this.failTick() || release - t > HATCH_OPENS) {
            return -1.0;
        }
        return Ease.smooth(1.0 - (release - t) / LOWERING);
    }

    /**
     * A big missile the moment it drops out of the hatch on tick {@code release}: {where it hangs under the belly, how
     * it moves (with the plane, and a push down), the way its nose points, its up}, as {@link #fall} takes them on.
     */
    public Vec3[] dropsOut(int release) {
        Vec3[] axes = this.axes(release);
        return new Vec3[] { this.point(release, 0.0, AirStrike.DROP_Y, AirStrike.BAY_Z),
                this.velocity(release).add(0.0, -DROP_PUSH, 0.0), axes[2], axes[1] };
    }

    /**
     * A small missile the moment jet {@code k} fires it off its pylon on {@code side} (0 the left one, 1 the right one)
     * on tick {@code fired}: {where it hangs, how it moves (with the jet, and a push down), its nose, its up}.
     */
    public Vec3[] firedOff(int k, int side, int fired) {
        Vec3[] axes = this.jetAxes(k, fired);
        Vec3 right = axes[0].cross(axes[1]).normalize();
        Vec3 from = this.jetAt(k, fired).add(right.scale((side == 0 ? -1.0 : 1.0) * AirStrike.PYLON_X))
                .add(axes[1].scale(AirStrike.PYLON_Y)).add(axes[0].scale(AirStrike.PYLON_Z));
        Vec3 moving = this.jetVelocity(k, fired).add(axes[1].scale(-PYLON_PUSH));
        return new Vec3[] { from, moving, axes[0], axes[1] };
    }

    /**
     * One tick of a missile falling with its motor dead: {where it is, how it moves, its nose, its up} on to a tick
     * later. It falls, slowing a little in the air, and its nose dips into the way it falls (a small one's sooner). The
     * server moves every missile with this and clients draw one leaving the plane or a jet with it, so both see the
     * very same fall.
     */
    public static Vec3[] fall(Vec3[] state, boolean small) {
        Vec3 moving = state[1].scale(DRAG).add(0.0, -GRAVITY, 0.0);
        Vec3 nose = state[2].lerp(moving.normalize(), small ? 0.2 : 0.1).normalize();
        return new Vec3[] { state[0].add(moving), moving, nose, carried(state[3], nose) };
    }

    /**
     * An up carried along as a nose swings round ({@code up} the up it had, {@code nose} the new way it points, one
     * long): it turns with the nose and never flips over, however steeply the nose dives.
     */
    public static Vec3 carried(Vec3 up, Vec3 nose) {
        Vec3 flat = up.subtract(nose.scale(up.dot(nose)));
        return flat.lengthSqr() < 1.0E-10 ? up : flat.normalize();
    }

    // ---- The miniguns ----

    /** The middle of the ball minigun {@code gun} (0 the left one, 1 the right one) turns on. */
    public Vec3 pivot(int gun, double t) {
        return this.point(t, (gun == 0 ? -1.0 : 1.0) * AirStrike.GUN_X, AirStrike.GUN_Y, AirStrike.GUN_Z);
    }

    /** The way minigun {@code gun} points at rest: out to its side, ahead and down. */
    public Vec3 gunRest(int gun, double t) {
        Vec3[] axes = this.axes(t);
        return axes[2].scale(0.3).add(axes[0].scale((gun == 0 ? -1.0 : 1.0) * 0.8)).add(axes[1].scale(-0.55))
                .normalize();
    }

    /**
     * The way minigun {@code gun} should point to fire at {@code at}, from where it turns on tick {@code t}: never up
     * into the plane's own wing.
     */
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

    /**
     * One tick of a minigun's swing: {the way it points, how fast it turns (about the way of this vector, in radians
     * per tick)} on to a tick later, turning towards {@code goal}. It speeds up and slows down smoothly and never
     * swings faster than it can.
     */
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

    /**
     * How one minigun swings on its ball, worked out alike by the server and every client from the same few words:
     * every time it fires, the server says where it is to point next, and {@link #GUN_LAG} ticks later it starts to
     * swing there (see {@link #swing}); told nothing for {@link #GUN_HOLDS} ticks, it swings back to rest. The server
     * fires every round out of the barrel as it points then, so every round leaves the barrel that every client draws.
     */
    public static final class Turret {
        private final PlanePath path;
        private final int gun;
        // The ticks it was told on where to point next, oldest first, and where (one long).
        private final List<Integer> told = new ArrayList<>();
        private final List<Vec3> goals = new ArrayList<>();
        // The way it points and how fast it turns on every tick from FORM on, as far as that is worked out.
        private final List<Vec3> aims = new ArrayList<>();
        private final List<Vec3> spins = new ArrayList<>();
        // The latest tick it was asked about, and whether news came in since that changed how it pointed by then.
        private double asked = Double.NEGATIVE_INFINITY;
        private boolean revised;

        public Turret(PlanePath path, int gun) {
            this.path = path;
            this.gun = gun;
        }

        /** It fired on tick {@code tick} and was told to point {@code goal} (one long) next. */
        public void fired(int tick, Vec3 goal) {
            int at = this.told.size();
            while (at > 0 && this.told.get(at - 1) > tick) {
                at--;
            }
            if (at > 0 && this.told.get(at - 1) == tick) {
                return;
            }
            this.told.add(at, tick);
            this.goals.add(at, goal);
            // From GUN_LAG ticks on it swings otherwise: that is worked out again.
            int keep = Math.max(0, tick + GUN_LAG - FORM);
            if (this.aims.size() > keep) {
                if (tick + GUN_LAG <= Math.floor(this.asked) + 1.0) {
                    this.revised = true;
                }
                this.aims.subList(keep, this.aims.size()).clear();
                this.spins.subList(keep, this.spins.size()).clear();
            }
        }

        /** The way it points {@code t} ticks after the call, one long: smooth between the ticks. */
        public Vec3 aim(double t) {
            this.asked = Math.max(this.asked, t);
            int tick = (int) Math.floor(t);
            Vec3 from = this.onTick(tick);
            Vec3 to = this.onTick(tick + 1);
            double u = t - tick;
            Vec3 aim = from.scale(1.0 - u).add(to.scale(u));
            return aim.lengthSqr() < 1.0E-12 ? to : aim.normalize();
        }

        /** True once, when news came in that changed how it pointed on ticks it had already been asked about. */
        public boolean revised() {
            boolean was = this.revised;
            this.revised = false;
            return was;
        }

        /** The tick of its latest round fired on {@code t} or before, or -1 when it has fired none by then. */
        public int lastFired(double t) {
            for (int k = this.told.size() - 1; k >= 0; k--) {
                if (this.told.get(k) <= t) {
                    return this.told.get(k);
                }
            }
            return -1;
        }

        /** The way it points on a whole tick, worked out on from the last tick known. */
        private Vec3 onTick(int tick) {
            if (tick <= FORM) {
                return this.path.gunRest(this.gun, tick);
            }
            if (this.aims.isEmpty()) {
                this.aims.add(this.path.gunRest(this.gun, FORM));
                this.spins.add(Vec3.ZERO);
            }
            while (this.aims.size() <= tick - FORM) {
                int next = FORM + this.aims.size();
                Vec3[] swung = swing(this.aims.get(this.aims.size() - 1), this.spins.get(this.spins.size() - 1),
                        this.goal(next));
                this.aims.add(swung[0]);
                this.spins.add(swung[1]);
            }
            return this.aims.get(tick - FORM);
        }

        /** Where it swings to on this tick: where it was last told to point, or to rest after a while without word. */
        private Vec3 goal(int tick) {
            int heard = tick - GUN_LAG;
            for (int k = this.told.size() - 1; k >= 0; k--) {
                int when = this.told.get(k);
                if (when <= heard) {
                    return heard - when < GUN_HOLDS ? this.goals.get(k) : this.path.gunRest(this.gun, tick);
                }
            }
            return this.path.gunRest(this.gun, tick);
        }
    }

    // ---- The jets ----

    /** The tick jet {@code k} starts to take shape beside the plane. */
    public double jetFrom(int k) {
        return FORM + 6 + 16 * k;
    }

    /** True when jet {@code k} takes shape at all: not when the plane starts to fail right after taking shape. */
    public boolean hasJet(int k) {
        return this.jetFrom(k) + JET_GROWS < this.failTick();
    }

    /** How far jet {@code k} has grown out of the light, 0 to a hair over 1. */
    public double jetGrown(int k, double t) {
        return Ease.backOut((t - this.jetFrom(k)) / JET_GROWS);
    }

    /** How many ticks ago the jets broke away to race off, or below 0 before that. */
    public double jetsFled(double t) {
        return t - this.failTick();
    }

    /** Where jet {@code k} is, {@code t} ticks after the call. */
    public Vec3 jetAt(int k, double t) {
        double fled = this.jetsFled(t);
        if (fled <= 0.0) {
            return this.racing(k, t);
        }
        // Breaking away: on from where it was the way it went, speeding up harder and harder as it races off, out away
        // from the plane and up.
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

    /**
     * The way jet {@code k} points and its up, one long each: banked into its turns. It rolls into a bank over a few
     * ticks, as a jet does, never snapping over (even as it breaks away from its turn round the plane).
     */
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

    /** Jet {@code k}'s up if it leaned into its turn right away: as far as it turns, against how hard gravity pulls. */
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

    /** How fast jet {@code k} goes {@code t} ticks after the call, in blocks per tick. */
    public double jetSpeed(int k, double t) {
        return this.jetVelocity(k, t).length();
    }

    /** How fast and which way jet {@code k} moves {@code t} ticks after the call, in blocks per tick. */
    public Vec3 jetVelocity(int k, double t) {
        return this.jetAt(k, t + 0.5).subtract(this.jetAt(k, t - 0.5));
    }

    /**
     * Where jet {@code k} races round the plane before it breaks away: it peels off the plane's wing, speeding up along
     * its way, and swings out into its circuit round it, over and under it in turn.
     */
    private Vec3 racing(int k, double t) {
        double from = this.jetFrom(k);
        double side = k == 0 ? -1.0 : 1.0;
        Vec3 right = this.way.cross(Vectors.UP).normalize();
        // Off the wing: along the way the plane flies, faster and faster, until it is up to its own speed.
        double since = t - from;
        double pace = (JET_SPEED - SPEED) / JET_JOIN;
        Vec3 wing = this.at(from).add(right.scale(side * JET_WING)).add(0.0, -3.0, 0.0);
        Vec3 off = wing.add(this.way.scale(SPEED * since + 0.5 * pace * since * Math.abs(since)));
        // Round the plane: the left jet swings round one way and the right one the other, out ahead of it first.
        double turns = JET_SPEED / JET_RADIUS * (since - JET_JOIN * 0.5);
        double angle = side * (Math.PI / 3.0 - turns);
        double radius = JET_RADIUS * (1.0 + 0.16 * Math.sin(2.0 * angle + k * 1.3));
        double high = JET_HIGH[k] + 3.0 * Math.sin(1.5 * angle + k);
        Vec3 round = this.orbitMiddle(t).add(this.way.scale(radius * Math.cos(angle)))
                .add(right.scale(radius * Math.sin(angle))).add(0.0, high, 0.0);
        return off.lerp(round, Ease.smoother(since / JET_JOIN));
    }

    /** The middle the jets race round: where the plane is along its way, at the height it took shape at. */
    private Vec3 orbitMiddle(double t) {
        return this.start.add(this.way.scale(this.gone(Math.min(t, this.diveTick()))));
    }
}
