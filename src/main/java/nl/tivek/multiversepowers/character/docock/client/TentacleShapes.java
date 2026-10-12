package nl.tivek.multiversepowers.character.docock.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.docock.TentacleReach;

// The shapes of Doctor Octopus's tentacles: each a smooth curve out of its socket on his back, first straight out and
// up (the upper pair over his shoulders, the lower pair out to his sides), then round to its tip. Free, a tentacle
// hangs coiled at the ready and sways; on the ground its tip stands on a foot spot round him; holding, it reaches the
// creature it holds.
final class TentacleShapes {
    private static final int POINTS = 16;

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
            return TentacleReach.upper(index) ? this.at(side * 0.13, 1.3, -0.2) : this.at(side * 0.12, 0.95, -0.2);
        }

        // The way a tentacle leaves its socket.
        Vec3 out(int index) {
            double side = TentacleReach.side(index);
            Vec3 back = this.forward().scale(-1.0);
            return TentacleReach.upper(index) ? back.scale(0.6).add(this.up().scale(0.9)).add(this.left().scale(side
                    * 0.35)).normalize() : back.scale(0.7).add(this.left().scale(side * 0.8)).normalize();
        }

        // Where a free tentacle's tip waits, swaying with `time` (ticks).
        Vec3 rest(int index, float time) {
            double side = TentacleReach.side(index);
            double sway = Mth.sin(time * 0.07F + index * 1.7F) * 0.12;
            double bob = Mth.sin(time * 0.05F + index * 2.3F) * 0.1;
            if (TentacleReach.upper(index)) {
                return this.at(side * (0.75 + sway), 2.25 + bob, 0.35);
            }
            return this.at(side * (1.15 + sway), 1.05 + bob, 0.15);
        }

        // Where a tentacle on the ground would stand, before the ground under it is found.
        Vec3 foot(int index) {
            double side = TentacleReach.side(index);
            return TentacleReach.upper(index) ? this.at(side * 1.2, 0.0, 0.8) : this.at(side * 1.3, 0.0, -0.7);
        }
    }

    // A curve from the socket to `tip`, leaving the socket along `out` and coming into the tip along `in`.
    static List<Vec3> curve(Vec3 socket, Vec3 out, Vec3 tip, Vec3 in) {
        double reach = socket.distanceTo(tip);
        Vec3 c1 = socket.add(out.scale(reach * 0.45 + 0.3));
        Vec3 c2 = tip.subtract(in.scale(reach * 0.35 + 0.2));
        List<Vec3> points = new ArrayList<>(POINTS);
        for (int k = 0; k < POINTS; k++) {
            double t = (double) k / (POINTS - 1);
            double u = 1.0 - t;
            points.add(socket.scale(u * u * u).add(c1.scale(3 * u * u * t)).add(c2.scale(3 * u * t * t))
                    .add(tip.scale(t * t * t)));
        }
        return points;
    }
}
