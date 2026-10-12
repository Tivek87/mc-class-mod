package nl.tivek.multiversepowers.character.docock.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.docock.TentacleReach;

// The shapes of Doctor Octopus's tentacles: each a smooth curve out of its socket on the small of his back, first
// straight out and up (the upper pair over his shoulders, the lower pair out to his sides), then round to its tip,
// and always TentacleReach.LENGTH long, never stretching: the length its tip does not need is taken up in a wide bow.
// Free, a tentacle arches high at the ready and sways; on the ground its tip stands on a foot spot well out round him;
// holding, it arches over to the creature it holds.
final class TentacleShapes {
    private static final int POINTS = 56;
    private static final int MEASURE = 28;
    private static final int SOLVE = 16;
    // A tip reaches no further than this share of the length, so even reaching out the tentacle still bends a little.
    private static final double STRAIGHT = 0.96;

    private TentacleShapes() {
    }

    // A body standing at `feet` turned to `yaw` (body yaw), crouched lower.
    record Body(Vec3 feet, float yaw, boolean crouching) {
        Vec3 forward() {
            return TentacleReach.forward(this.yaw);
        }

        Vec3 left() {
            return TentacleReach.left(this.yaw);
        }

        Vec3 up() {
            return new Vec3(0.0, 1.0, 0.0);
        }

        // A point in the body's frame: `x` to his left, `y` up from his feet, `z` ahead.
        Vec3 at(double x, double y, double z) {
            double drop = this.crouching ? 0.25 : 0.0;
            return this.feet.add(this.left().scale(x)).add(0.0, y - drop * Mth.clamp(y / 1.4, 0.0, 1.0), 0.0)
                    .add(this.forward().scale(z - (this.crouching ? 0.1 : 0.0)));
        }

        Vec3 socket(int index) {
            double side = TentacleReach.side(index);
            return TentacleReach.upper(index) ? this.at(side * 0.15, 1.1, -0.3) : this.at(side * 0.19, 0.8, -0.28);
        }

        // The way a tentacle leaves its socket.
        Vec3 out(int index) {
            double side = TentacleReach.side(index);
            Vec3 back = this.forward().scale(-1.0);
            return TentacleReach.upper(index) ? back.scale(0.6).add(this.up().scale(0.9)).add(this.left().scale(side
                    * 0.35)).normalize() : back.scale(0.7).add(this.left().scale(side * 0.8)).normalize();
        }

        // Where a free tentacle's claw waits, swaying with `time` (ticks): the upper pair high over his shoulders,
        // the lower pair out at his sides, all looking ahead.
        Vec3 rest(int index, float time) {
            double side = TentacleReach.side(index);
            double sway = Mth.sin(time * 0.07F + index * 1.7F) * 0.25;
            double bob = Mth.sin(time * 0.05F + index * 2.3F) * 0.2;
            if (TentacleReach.upper(index)) {
                return this.at(side * (1.9 + sway), 3.4 + bob, 1.2);
            }
            return this.at(side * (2.6 + sway), 1.6 + bob, 1.0);
        }

        // The way a free tentacle bows: up and back over him, or out to his side and back, breathing with `time`.
        Vec3 restBow(int index, float time) {
            double side = TentacleReach.side(index);
            double drift = Mth.sin(time * 0.04F + index * 1.3F) * 0.2;
            Vec3 back = this.forward().scale(-1.0);
            if (TentacleReach.upper(index)) {
                return this.up().add(back.scale(0.7 + drift)).add(this.left().scale(side * 0.5));
            }
            return this.left().scale(side).add(back.scale(0.9 + drift)).add(this.up().scale(0.35));
        }

        // Where a tentacle on the ground would stand, before the ground under it is found: well out round him, as a
        // spider stands.
        Vec3 foot(int index) {
            double side = TentacleReach.side(index);
            return TentacleReach.upper(index) ? this.at(side * 3.6, 0.0, 2.6) : this.at(side * 3.8, 0.0, -2.4);
        }

        // A tentacle on the ground arches up and out like a spider's leg.
        Vec3 groundBow(int index) {
            return this.up().add(this.left().scale(TentacleReach.side(index) * 0.7));
        }

        // Holding or striking it arches up over the creature, a little out to its side.
        Vec3 reachBow(int index) {
            return this.up().add(this.left().scale(TentacleReach.side(index) * 0.4))
                    .add(this.forward().scale(-0.3));
        }
    }

    // Where a tentacle's tip can be at most: `want`, or as far towards it as the tentacle reaches nearly straight.
    static Vec3 reach(Vec3 socket, Vec3 want) {
        Vec3 chord = want.subtract(socket);
        double far = chord.length();
        double most = TentacleReach.LENGTH * STRAIGHT;
        return far <= most ? want : socket.add(chord.scale(most / far));
    }

    // A curve from the socket to `tip` always TentacleReach.LENGTH long, leaving the socket along `out` and coming
    // into the tip along `in`: what length the way from one to the other leaves over is taken up by bowing out
    // towards `bow` (a wide arch over him, a spider's leg on the ground), with a little S-turn to one side.
    static List<Vec3> curve(Vec3 socket, Vec3 out, Vec3 tip, Vec3 in, Vec3 bow) {
        tip = reach(socket, tip);
        Vec3 chord = tip.subtract(socket);
        double far = Math.max(1.0E-3, chord.length());
        Vec3 along = chord.scale(1.0 / far);
        Vec3 arch = bow.subtract(along.scale(bow.dot(along)));
        if (arch.lengthSqr() < 1.0E-6) {
            arch = Math.abs(along.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0).subtract(along.scale(along.y))
                    : new Vec3(1.0, 0.0, 0.0).subtract(along.scale(along.x));
        }
        arch = arch.normalize();
        Vec3 twist = along.cross(arch);
        double handle = Math.min(far * 0.35, 1.6);
        Vec3 c1 = socket.add(out.scale(handle));
        Vec3 c2 = tip.subtract(in.scale(handle));
        // How far it bows out: found so the curve is just its length (longer the further it bows).
        double low = 0.0;
        double high = TentacleReach.LENGTH;
        for (int k = 0; k < SOLVE; k++) {
            double mid = (low + high) * 0.5;
            if (length(socket, c1, c2, tip, arch, twist, mid) < TentacleReach.LENGTH) {
                low = mid;
            } else {
                high = mid;
            }
        }
        double amount = (low + high) * 0.5;
        List<Vec3> points = new ArrayList<>(POINTS);
        for (int k = 0; k < POINTS; k++) {
            points.add(point(socket, c1, c2, tip, arch, twist, amount, (double) k / (POINTS - 1)));
        }
        return points;
    }

    private static double length(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, Vec3 arch, Vec3 twist, double amount) {
        double sum = 0.0;
        Vec3 last = a;
        for (int k = 1; k <= MEASURE; k++) {
            Vec3 next = point(a, c1, c2, b, arch, twist, amount, (double) k / MEASURE);
            sum += next.distanceTo(last);
            last = next;
        }
        return sum;
    }

    private static Vec3 point(Vec3 a, Vec3 c1, Vec3 c2, Vec3 b, Vec3 arch, Vec3 twist, double amount, double t) {
        double u = 1.0 - t;
        double lift = Math.sin(Math.PI * t);
        double s = Math.sin(2.0 * Math.PI * t);
        return a.scale(u * u * u).add(c1.scale(3 * u * u * t)).add(c2.scale(3 * u * t * t)).add(b.scale(t * t * t))
                .add(arch.scale(amount * lift)).add(twist.scale(amount * 0.22 * s));
    }
}
