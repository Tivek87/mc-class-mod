package nl.tivek.multiversepowers.character.greenlantern.client.body;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FlamePainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;

final class FlamePoses extends FlameKeys {
    // From the first-person view to the body: the view's reach is squeezed onto the body's.
    private static final Vec3 BODY_SCALE = new Vec3(0.65, 1.12, -0.9);
    // Where the guard's fist sits on the body: the gun held low before the belly, the body turned behind it.
    private static final Vec3 BODY_GUARD = new Vec3(0.02, -0.42, 0.24);
    private static final Vec3 BODY_SHIFT = BODY_GUARD.subtract(GUARD.grip().multiply(BODY_SCALE));
    private static final Vec3 EYES = new Vec3(0.0, 0.41, 0.12);

    private FlamePoses() {
    }

    static Pose at(FlameMove move, float t, float time, Pose from, @Nullable float[] fromSpeed) {
        return switch (move) {
            case INFERNO -> carried(from, fromSpeed, inferno(t, time), t, BLEND_IN);
            case VORTEX -> carried(from, fromSpeed, vortex(t, time), t, BLEND_IN);
            default -> {
                Keyframes.Key[] keys = MOVES.get(move);
                float end = keys == null ? 0.0F : Keyframes.end(keys) + SETTLE;
                Pose rest = idle(time, (float) Ease.smoother((t - end) / IDLE_IN));
                yield keys == null ? rest : along(move, t, keyed(keys, t, from, fromSpeed, rest, SETTLE));
            }
        };
    }

    // While a stroke sprays the nozzle points just where its flame goes; the keys at both ends of it agree.
    private static Pose along(FlameMove move, float t, Pose pose) {
        FlameMove.Aim aim = move.aim(t);
        if (aim == null) {
            return pose;
        }
        float[] n = pose.numbers();
        double raise = Math.toRadians(aim.pitch());
        Vec3 muzzle = pitched(GUARD.muzzle(), raise);
        Vec3 top = pitched(GUARD.top(), raise);
        n[3] = (float) muzzle.x;
        n[4] = (float) muzzle.y;
        n[5] = (float) muzzle.z;
        n[6] = (float) top.x;
        n[7] = (float) top.y;
        n[8] = (float) top.z;
        n[17] = (float) Math.toRadians(aim.yaw()) + pose.orbit();
        return Pose.of(n);
    }

    static Vec3 body(Vec3 view) {
        return view.multiply(BODY_SCALE).add(BODY_SHIFT);
    }

    // A view-space way as a body-space one.
    static Vec3 way(Vec3 view) {
        return new Vec3(view.x, view.y, -view.z);
    }

    // The right fist where the body's arm really puts it, in body space.
    static Vec3 fist(Pose pose) {
        FlameBody.Torso torso = FlameBody.torso(pose, 1.0F);
        Vec3 shoulder = torso.shoulder(true);
        float[] rot = FlameBody.reach(shoulder, FlameBody.model(body(pose.grip())), torso.twist());
        return FlameBody.body(FlameBody.fist(shoulder, rot, true));
    }

    // The gun hangs from the right fist, so the left hand finds the front grip from there.
    static Vec3 leftTarget(Pose pose, Vec3 fist) {
        Vec3 ahead = way(pose.aim(1.0));
        Vec3 up = square(way(pose.up(1.0)), ahead);
        Vec3 fore = spot(fist, ahead, up, FlamePainter.FORE);
        Vec3 valve = spot(fist, ahead, up, FlamePainter.VALVE);
        return body(pose.left()).lerp(fore.lerp(valve, pose.leftValve()), pose.leftOn());
    }

    private static Vec3 spot(Vec3 fist, Vec3 ahead, Vec3 up, Vec3 local) {
        Vec3 from = local.subtract(FlamePainter.GRIP).scale(GUN_SCALE);
        return fist.add(up.scale(from.y)).add(ahead.scale(from.z));
    }

    static FlamePainter.Glow glow(FlameMove move, float t, float heat, boolean firing) {
        return switch (move.kind()) {
            case EQUIP -> {
                double fill = Ease.smooth((t - FlameMove.VALVE) / (FlameMove.FILLED - FlameMove.VALVE));
                double pilot = Ease.smooth((t - FlameMove.PILOT) / 2.0);
                double spark = Math.max(flash(t - FlameMove.SPARK), flash(t - FlameMove.SPARK_AGAIN));
                double test = t >= FlameMove.TEST - 1 && t < FlameMove.TEST + FlameMove.TEST_TICKS ? 1.0
                        : flash((t - FlameMove.TEST - FlameMove.TEST_TICKS) * 0.5);
                double valve = Ease.smooth((t - FlameMove.VALVE + 1.5) / 2.5) * Math.PI * 0.5;
                yield new FlamePainter.Glow(fill, heat, pilot, spark, test, valve);
            }
            case ATTACK -> new FlamePainter.Glow(1.0, heat, 1.0, 0.0, move.spraying(t) ? 1.0
                    : flash(move.sinceSpray(t)), Math.PI * 0.5);
            case INFERNO -> new FlamePainter.Glow(1.0, heat, 1.0, 0.0, firing && t >= FlameMove.BRACE - 1 ? 1.0
                    : 0.4, Math.PI * 0.5);
            case WALL -> new FlamePainter.Glow(1.0, heat, 1.0, 0.0, t >= FlameMove.LAY_FROM
                    && t < FlameMove.LAY_TO + 1 ? 0.8 : 0.0, Math.PI * 0.5);
            case VORTEX -> new FlamePainter.Glow(1.0, heat, 1.0, 0.0, 0.7, Math.PI * 0.5);
            case BURST -> new FlamePainter.Glow(1.0, heat, 1.0, 0.0, flash((t - FlameMove.BLAST) * 0.6),
                    Math.PI * 0.5);
            case VENT -> new FlamePainter.Glow(1.0, heat, Ease.smooth((t - 4.0) / 6.0), 0.0, 0.0, Math.PI * 0.5);
        };
    }

    static float heatWanted(FlameMove move, float t, boolean firing, boolean swirling) {
        return switch (move.kind()) {
            case INFERNO -> firing ? 1.0F : 0.0F;
            case VORTEX -> swirling ? 0.7F : 0.0F;
            case ATTACK -> move.spraying(t) ? 0.6F * (float) move.power() : 0.0F;
            case EQUIP -> t >= FlameMove.TEST && t < FlameMove.HISS ? 0.8F : 0.0F;
            case WALL -> t >= FlameMove.LAY_FROM && t < FlameMove.LAY_TO ? 0.6F : 0.0F;
            case BURST -> t < 4.0F ? 0.9F : 0.0F;
            case VENT -> 0.0F;
        };
    }

    private static double flash(double since) {
        return since < 0.0 || since > 2.0 ? 0.0 : 1.0 - since / 2.0;
    }

    static float[] look(float t, Pose pose) {
        float[] look = new float[2];
        double forming = Ease.smooth((t - 2.0) / 4.0) * (1.0 - Ease.smooth((t - (FlameMove.LEFT_GRAB + 1.0)) / 4.0));
        Vec3 middle = pose.gun(1.0, OWN_GUN).at(0.0, -0.05, 0.15);
        if (forming > 0.0) {
            glance(look, middle, forming, 0.45, 0.4, 18.0);
        }
        double valve = Ease.smooth((t - (FlameMove.VALVE - 5.0)) / 4.0)
                * (1.0 - Ease.smooth((t - (FlameMove.LEFT_BACK - 1.0)) / 4.0));
        if (valve > 0.0) {
            Vec3 at = pose.gun(1.0, OWN_GUN).at(FlamePainter.VALVE.x, FlamePainter.VALVE.y, FlamePainter.VALVE.z);
            glance(look, at, valve, 0.5, 0.45, 20.0);
        }
        double nozzle = Ease.smooth((t - (FlameMove.SPARK - 3.0)) / 3.0)
                * (1.0 - Ease.smooth((t - (FlameMove.PILOT + 2.0)) / 4.0));
        if (nozzle > 0.0) {
            Vec3 at = pose.gun(1.0, OWN_GUN).at(FlamePainter.PILOT.x, FlamePainter.PILOT.y, FlamePainter.PILOT.z);
            glance(look, at, nozzle, 0.5, 0.45, 18.0);
        }
        double up = Ease.smooth((t - (FlameMove.TEST - 3.0)) / 4.0)
                * (1.0 - Ease.smooth((t - (FlameMove.TEST + FlameMove.TEST_TICKS + 2.0)) / 6.0));
        if (up > 0.0) {
            look[0] += (float) (up * Math.toRadians(16.0 + 5.0 * Ease.smooth((t - FlameMove.TEST) / 6.0)));
        }
        return look;
    }

    private static void glance(float[] look, Vec3 at, double weight, double up, double across, double most) {
        double ahead = Math.max(0.1, -at.z);
        double limit = most * Mth.DEG_TO_RAD;
        look[0] += (float) (weight * Mth.clamp(Math.atan2(at.y, ahead) * up, -limit, limit));
        look[1] += (float) (weight * Mth.clamp(Math.atan2(at.x, ahead) * across, -limit, limit));
    }

    static float[] head(float t, Pose pose) {
        float[] head = new float[3];
        double forming = Ease.smooth((t - 2.0) / 4.0) * (1.0 - Ease.smooth((t - (FlameMove.LEFT_BACK + 1.0)) / 5.0));
        double nozzle = Ease.smooth((t - (FlameMove.SPARK - 3.0)) / 3.0)
                * (1.0 - Ease.smooth((t - (FlameMove.PILOT + 2.0)) / 4.0));
        double up = Ease.smooth((t - (FlameMove.TEST - 3.0)) / 4.0)
                * (1.0 - Ease.smooth((t - (FlameMove.TEST + FlameMove.TEST_TICKS + 2.0)) / 6.0));
        Vec3 fist = fist(pose);
        Vec3 ahead = way(pose.aim(1.0));
        Vec3 top = square(way(pose.up(1.0)), ahead);
        double weight = 0.0;
        if (forming > 0.0) {
            weight += watch(head, spot(fist, ahead, top, FlamePainter.TANK), forming * 0.8);
        }
        if (nozzle > 0.0) {
            weight += watch(head, spot(fist, ahead, top, FlamePainter.PILOT), nozzle * 0.8);
        }
        if (up > 0.0) {
            weight += watch(head, EYES.add(0.0, 2.0, 2.0), up * 0.6);
        }
        if (weight > 1.0E-6) {
            head[0] /= (float) weight;
            head[1] /= (float) weight;
        }
        head[2] = (float) Math.min(1.0, weight);
        return head;
    }

    private static double watch(float[] head, Vec3 at, double weight) {
        Vec3 to = at.subtract(EYES);
        head[0] += (float) (weight * -Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        head[1] += (float) (weight * Math.atan2(to.x, to.z));
        return weight;
    }
}
