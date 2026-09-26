package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameMove;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;

// The left-click sweeps: a wind-up that gathers the body, the body along the stroke (the nozzle follows the
// stroke's own path) and a follow-through that lets the weight settle.
final class FlameAttacks {
    private FlameAttacks() {
    }

    // One moment of an attack: aim in degrees right and up, the fist's shift from the guard (view space), the body's
    // twist in degrees on top of what the aim gives, and the rest as the pose's own channels.
    static final class Beat {
        final float tick;
        boolean stop;
        double yaw;
        double pitch;
        double dx;
        double dy;
        double dz;
        double twist;
        float lean = 0.1F;
        float roll;
        float squat = 0.08F;
        float step;
        float wide = 0.3F;
        float leftOn = 1.0F;
        Vec3 left = FlameKeys.GUARD.left();

        Beat(float tick) {
            this.tick = tick;
        }

        Beat stop() {
            this.stop = true;
            return this;
        }

        Beat aim(double yaw, double pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            return this;
        }

        Beat grip(double dx, double dy, double dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            return this;
        }

        Beat twist(double degrees) {
            this.twist = degrees;
            return this;
        }

        Beat lean(double lean) {
            this.lean = (float) lean;
            return this;
        }

        Beat roll(double roll) {
            this.roll = (float) roll;
            return this;
        }

        Beat squat(double squat) {
            this.squat = (float) squat;
            return this;
        }

        Beat step(double step) {
            this.step = (float) step;
            return this;
        }

        Beat wide(double wide) {
            this.wide = (float) wide;
            return this;
        }

        FlameCurves.Pose pose() {
            float[] n = FlameKeys.GUARD.numbers();
            double raise = Math.toRadians(this.pitch);
            Vec3 muzzle = FlameKeys.pitched(FlameKeys.GUARD.muzzle(), raise);
            Vec3 top = FlameKeys.pitched(FlameKeys.GUARD.top(), raise);
            n[0] += (float) (0.0009 * this.yaw + this.dx);
            n[1] += (float) (0.0022 * this.pitch + this.dy);
            n[2] += (float) (0.0012 * this.pitch + this.dz);
            n[3] = (float) muzzle.x;
            n[4] = (float) muzzle.y;
            n[5] = (float) muzzle.z;
            n[6] = (float) top.x;
            n[7] = (float) top.y;
            n[8] = (float) top.z;
            n[9] = (float) this.left.x;
            n[10] = (float) this.left.y;
            n[11] = (float) this.left.z;
            n[12] = this.leftOn;
            n[13] = 0.0F;
            n[14] = (float) Math.toRadians(30.0 + 0.36 * this.yaw + this.twist);
            n[15] = this.lean - 0.004F * (float) this.pitch;
            n[16] = this.step;
            n[17] = (float) Math.toRadians(this.yaw);
            n[18] = 0.0F;
            n[FlameCurves.Pose.ROLL] = this.roll;
            n[FlameCurves.Pose.SQUAT] = this.squat;
            n[FlameCurves.Pose.WIDE] = this.wide;
            return FlameCurves.Pose.of(n);
        }

        Keyframes.Key key() {
            return FlameCurves.key(this.tick, this.stop, this.pose());
        }
    }

    // The body at a share of the way along a stroke, given where the nozzle points there.
    @FunctionalInterface
    interface Body {
        Beat at(double u, FlameMove.Aim aim);
    }

    static Beat beat(float tick) {
        return new Beat(tick);
    }

    static Beat body() {
        return new Beat(0.0F);
    }

    static Beat mix(Beat a, Beat b, double u, float tick) {
        Beat m = new Beat(tick);
        m.yaw = Mth.lerp(u, a.yaw, b.yaw);
        m.pitch = Mth.lerp(u, a.pitch, b.pitch);
        m.dx = Mth.lerp(u, a.dx, b.dx);
        m.dy = Mth.lerp(u, a.dy, b.dy);
        m.dz = Mth.lerp(u, a.dz, b.dz);
        m.twist = Mth.lerp(u, a.twist, b.twist);
        float f = (float) u;
        m.lean = Mth.lerp(f, a.lean, b.lean);
        m.roll = Mth.lerp(f, a.roll, b.roll);
        m.squat = Mth.lerp(f, a.squat, b.squat);
        m.step = Mth.lerp(f, a.step, b.step);
        m.wide = Mth.lerp(f, a.wide, b.wide);
        m.leftOn = Mth.lerp(f, a.leftOn, b.leftOn);
        m.left = a.left.lerp(b.left, u);
        return m;
    }

    private static Body between(Beat from, Beat to) {
        return (u, aim) -> mix(from, to, Ease.smooth(u), 0.0F);
    }

    static void fill(Map<FlameMove, Keyframes.Key[]> moves) {
        sweeps(moves);
    }

    // A wide sweep from the hip, level from side to side: the body turns with the gun and the weight moves from one
    // foot to the other, but nothing rises or dips. Right to left, and back again.
    private static void sweeps(Map<FlameMove, Keyframes.Key[]> moves) {
        attack(moves, FlameMove.SWEEP, List.of(
                beat(1.6F).aim(30, 0).grip(0.01, 0.0, 0.04).twist(4).lean(0.15).squat(0.2).wide(0.5).step(-0.1)
                        .roll(0.03),
                beat(3.2F).stop().aim(84, 0).grip(0.02, 0.0, 0.05).twist(10).lean(0.15).squat(0.2).wide(0.55)
                        .step(-0.25).roll(0.06),
                beat(12.6F).aim(-92, 0).grip(-0.01, 0.0, 0.01).twist(-12).lean(0.15).squat(0.2).wide(0.55)
                        .step(0.28).roll(-0.06),
                beat(15.2F).aim(-40, 0).grip(0.0, 0.0, 0.02).twist(-4).lean(0.12).squat(0.14).wide(0.45).step(0.1)),
                between(body().grip(0.02, 0.0, 0.05).twist(8).lean(0.15).squat(0.2).wide(0.55).step(-0.22).roll(0.06),
                        body().grip(-0.01, 0.0, 0.01).twist(-8).lean(0.15).squat(0.2).wide(0.55).step(0.25)
                                .roll(-0.06)));
        attack(moves, FlameMove.SWEEP_BACK, List.of(
                beat(1.6F).aim(-30, 0).grip(-0.04, 0.0, 0.03).twist(-6).lean(0.15).squat(0.2).wide(0.5).step(0.1)
                        .roll(-0.03),
                beat(3.2F).stop().aim(-84, 0).grip(-0.08, 0.0, 0.04).twist(-14).lean(0.15).squat(0.2).wide(0.55)
                        .step(0.25).roll(-0.06),
                beat(12.6F).aim(92, 0).grip(0.05, 0.0, 0.0).twist(14).lean(0.15).squat(0.2).wide(0.55).step(-0.28)
                        .roll(0.06),
                beat(15.2F).aim(40, 0).grip(0.02, 0.0, 0.01).twist(5).lean(0.12).squat(0.14).wide(0.45).step(-0.1)),
                between(body().grip(-0.07, 0.0, 0.04).twist(-12).lean(0.15).squat(0.2).wide(0.55).step(0.22)
                        .roll(-0.06),
                        body().grip(0.04, 0.0, 0.0).twist(12).lean(0.15).squat(0.2).wide(0.55).step(-0.25).roll(0.06)));
    }

    private static void attack(Map<FlameMove, Keyframes.Key[]> moves, FlameMove move, List<Beat> beats,
            Body... strokes) {
        List<Keyframes.Key> keys = new ArrayList<>();
        for (Beat beat : beats) {
            keys.add(beat.key());
        }
        FlameMove.Stroke[] all = move.strokes();
        for (int s = 0; s < all.length; s++) {
            FlameMove.Stroke stroke = all[s];
            Body body = strokes[Math.min(s, strokes.length - 1)];
            float length = stroke.to() - stroke.from();
            int parts = length >= 6.0F ? 5 : 3;
            for (int i = 0; i <= parts; i++) {
                double u = (double) i / parts;
                float tick = stroke.from() + length * (float) u;
                FlameMove.Aim aim = stroke.aim(tick);
                Beat b = body.at(u, aim);
                Beat at = mix(b, b, 0.0, tick);
                at.yaw = aim.yaw();
                at.pitch = aim.pitch();
                keys.add(at.key());
            }
        }
        keys.sort(Comparator.comparingDouble(Keyframes.Key::tick));
        moves.put(move, keys.toArray(Keyframes.Key[]::new));
    }
}
