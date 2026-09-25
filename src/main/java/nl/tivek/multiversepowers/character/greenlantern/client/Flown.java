package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.ArrayDeque;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;

/**
 * What this client keeps of one missile of an air strike: where it was on the ticks the server told of, so it is
 * drawn smoothly by its own clock between them (see missile); how it was let go; and, once it has struck, when by
 * that clock, so it is seen to get there before it breaks up.
 */
final class Flown {
    /** Where a missile was on one tick it was told of: its middle, its nose and its up (carried along as it swings). */
    record Spot(double age, Vec3 at, Vec3 nose, Vec3 up) {
    }

    // How many of the ticks it was told of are kept, and how soon a correction dies away (in ticks, most of it).
    private static final int KEPT = 8;
    private static final double SETTLES = 1.5;
    final ArrayDeque<Spot> spots = new ArrayDeque<>();
    final boolean small;
    final int variant;
    final int fired;
    final int ignites;
    // The way the plane flies it left, when this client draws that plane (null: it does not): the plane then draws
    // it itself while it falls with its motor dead (see PlanePainter).
    @Nullable
    private final PlanePath path;
    // Ticks after it was let go (by its clock) that it struck, or forever while it flies on; and whether the blast
    // it struck with is timed from it yet.
    double ends = Double.POSITIVE_INFINITY;
    boolean blasted;
    // What is left of a correction: where it was drawn less where it is now worked out to be, as a tick told of
    // came in later than it was drawn (see settle), and from when (ticks after the first tick it was told of, by its
    // clock). It dies away instead of making the missile jump.
    private Vec3 offset = Vec3.ZERO;
    private double offsetFrom;

    Flown(ConstructPayload first, @Nullable PlanePath path) {
        this.small = first.variant() >= AirStrike.JET_MISSILE;
        this.variant = first.variant();
        this.fired = Math.round(first.size());
        this.ignites = Math.round(first.charge());
        this.path = path;
    }

    /** Until how many ticks after it was let go the plane draws it: while it still falls with its motor dead. */
    double leaves() {
        return this.path == null ? Double.NEGATIVE_INFINITY : Math.min(this.ignites, this.ends);
    }

    /** Where it is on a tick the server tells of, heard of as it was drawn {@code drawn} ticks after the first. */
    void add(ConstructPayload update, double drawn) {
        Spot last = this.spots.peekLast();
        if (this.ends < Double.POSITIVE_INFINITY || last != null && update.age() <= last.age()) {
            return;
        }
        Vec3 was = this.drawnAt(drawn);
        Vec3 nose = update.facing().lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : update.facing().normalize();
        Vec3 up = last != null ? PlanePath.carried(last.up(), nose) : this.firstUp(update.age(), nose);
        this.spots.add(new Spot(update.age(), update.center(), nose, up));
        while (this.spots.size() > KEPT) {
            this.spots.poll();
        }
        this.settle(was, drawn);
    }

    /**
     * Where it is drawn {@code age} ticks after the first tick it was told of, with what is left of a correction; null
     * while nothing is drawn of it yet, or the plane draws it (see leaves).
     */
    @Nullable
    Vec3 drawnAt(double age) {
        return this.spots.isEmpty() || age + 1.0 <= this.leaves() ? null : this.at(age).at().add(this.off(age));
    }

    /** What is left of a correction {@code age} ticks after the first tick it was told of. */
    Vec3 off(double age) {
        return this.offset.scale(Math.exp(-Math.max(0.0, age - this.offsetFrom) / SETTLES));
    }

    /**
     * News came in that moves where it is worked out to be right now, where it was drawn at {@code was}: it goes on
     * from there and eases over to where it now is, instead of jumping.
     */
    private void settle(@Nullable Vec3 was, double age) {
        if (was != null && age + 1.0 > this.leaves()) {
            this.offset = was.subtract(this.at(age).at());
            this.offsetFrom = age;
        }
    }

    /** Its up on the first tick heard of: carried along from how it left the plane, just as the plane draws it. */
    private Vec3 firstUp(int age, Vec3 nose) {
        if (this.path == null) {
            return PlanePath.carried(new Vec3(0.0, 1.0, 0.0), nose);
        }
        int k = (this.variant - AirStrike.JET_MISSILE) / 2;
        Vec3[] state = this.small ? this.path.firedOff(k, (this.variant - AirStrike.JET_MISSILE) % 2, this.fired)
                : this.path.dropsOut(this.fired);
        for (int step = 0; step <= age && step < this.ignites; step++) {
            state = PlanePath.fall(state, this.small);
        }
        return PlanePath.carried(state[3], nose);
    }

    /**
     * It struck, or was let go of: the last tick told of is where it ends. Struck partway through that tick, it
     * gets there as much sooner as it went less far, so it never slows down on its last stretch. Heard of as it was
     * drawn {@code drawn} ticks after the first tick told of.
     */
    void strikes(double drawn) {
        if (this.ends < Double.POSITIVE_INFINITY || this.spots.isEmpty()) {
            return;
        }
        Vec3 was = this.drawnAt(drawn);
        Spot last = this.spots.pollLast();
        Spot before = this.spots.peekLast();
        double part = 1.0;
        if (before != null) {
            Spot earlier = null;
            for (Spot spot : this.spots) {
                if (spot != before) {
                    earlier = spot;
                }
            }
            double step = earlier == null ? 0.0 : before.at().distanceTo(earlier.at());
            if (step > 1.0E-6) {
                part = Mth.clamp(last.at().distanceTo(before.at()) / step, 0.05, 1.0);
            }
        }
        double age = before == null ? last.age() : before.age() + part;
        this.spots.add(new Spot(age, last.at(), last.nose(), last.up()));
        this.ends = age + 1.0;
        if (drawn < age) {
            this.settle(was, drawn);
        } else {
            // Already drawn on past where it struck (into the ground, or through the creature): it breaks up where
            // it really struck, in the flash of its blast.
            this.offset = Vec3.ZERO;
        }
    }

    /**
     * Where it is {@code age} ticks after the first tick it was told of by its own clock: between the ticks told
     * of, smooth; past the last one, on the way it went, a little way, until it hears more.
     */
    Spot at(double age) {
        Spot before = null;
        Spot after = null;
        Spot earlier = null;
        for (Spot spot : this.spots) {
            if (spot.age() <= age) {
                earlier = before;
                before = spot;
            } else {
                after = spot;
                break;
            }
        }
        if (before == null) {
            return this.spots.peekFirst();
        }
        if (after == null) {
            if (earlier == null || this.ends < Double.POSITIVE_INFINITY) {
                return before;
            }
            double on = Math.min(2.0, age - before.age()) / Math.max(1.0E-6, before.age() - earlier.age());
            return new Spot(age, before.at().add(before.at().subtract(earlier.at()).scale(on)), before.nose(),
                    before.up());
        }
        double u = (age - before.age()) / Math.max(1.0E-6, after.age() - before.age());
        Vec3 nose = before.nose().lerp(after.nose(), u).normalize();
        return new Spot(age, before.at().lerp(after.at(), u), nose,
                PlanePath.carried(before.up().lerp(after.up(), u), nose));
    }
}
