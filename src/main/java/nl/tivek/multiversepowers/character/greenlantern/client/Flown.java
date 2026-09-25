package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.ArrayDeque;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;

final class Flown {
    record Spot(double age, Vec3 at, Vec3 nose, Vec3 up) {
    }

    private static final int KEPT = 8;
    private static final double SETTLES = 1.5;
    final ArrayDeque<Spot> spots = new ArrayDeque<>();
    final boolean small;
    final int variant;
    final int fired;
    final int ignites;
    @Nullable
    private final PlanePath path;
    double ends = Double.POSITIVE_INFINITY;
    boolean blasted;
    private Vec3 offset = Vec3.ZERO;
    private double offsetFrom;

    Flown(ConstructPayload first, @Nullable PlanePath path) {
        this.small = first.variant() >= AirStrike.JET_MISSILE;
        this.variant = first.variant();
        this.fired = Math.round(first.size());
        this.ignites = Math.round(first.charge());
        this.path = path;
    }

    double leaves() {
        return this.path == null ? Double.NEGATIVE_INFINITY : Math.min(this.ignites, this.ends);
    }

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

    @Nullable
    Vec3 drawnAt(double age) {
        return this.spots.isEmpty() || age + 1.0 <= this.leaves() ? null : this.at(age).at().add(this.off(age));
    }

    Vec3 off(double age) {
        return this.offset.scale(Math.exp(-Math.max(0.0, age - this.offsetFrom) / SETTLES));
    }

    private void settle(@Nullable Vec3 was, double age) {
        if (was != null && age + 1.0 > this.leaves()) {
            this.offset = was.subtract(this.at(age).at());
            this.offsetFrom = age;
        }
    }

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
            this.offset = Vec3.ZERO;
        }
    }

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
