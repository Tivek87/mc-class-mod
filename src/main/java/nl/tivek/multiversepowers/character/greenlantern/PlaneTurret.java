package nl.tivek.multiversepowers.character.greenlantern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import static nl.tivek.multiversepowers.character.greenlantern.PlanePath.FORM;
import static nl.tivek.multiversepowers.character.greenlantern.PlanePath.GUN_HOLDS;
import static nl.tivek.multiversepowers.character.greenlantern.PlanePath.GUN_LAG;
import static nl.tivek.multiversepowers.character.greenlantern.PlanePath.swing;

/**
 * How one minigun of the air strike's plane (see {@link PlanePath}) swings on its ball, worked out alike by the
 * server and every client from the same few words: every time it fires, the server says where it is to point next,
 * and {@link PlanePath#GUN_LAG} ticks later it starts to swing there (see {@link PlanePath#swing}); told nothing
 * for {@link PlanePath#GUN_HOLDS} ticks, it swings back to rest. The server fires every round out of the barrel
 * as it points then, so every round leaves the barrel that every client draws. Reached as
 * {@link PlanePath.Turret}.
 */
abstract class PlaneTurret {
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

    PlaneTurret(PlanePath path, int gun) {
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
