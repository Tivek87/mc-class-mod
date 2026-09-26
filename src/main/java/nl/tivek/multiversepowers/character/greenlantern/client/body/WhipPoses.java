package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipLash;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;

final class WhipPoses extends WhipKeys {
    // From the first-person view to the body: the view's reach is squeezed onto the body's.
    private static final Vec3 BODY_SCALE = new Vec3(0.65, 1.12, -0.9);
    // Where the guard's fist sits on the body: low before the right hip, the lash hanging beside the right foot.
    private static final Vec3 BODY_GUARD = new Vec3(0.3, -0.56, 0.26);
    private static final Vec3 BODY_SHIFT = BODY_GUARD.subtract(GUARD.grip().multiply(BODY_SCALE));
    private static final Vec3 EYES = new Vec3(0.0, 0.41, 0.12);
    private static final Vec3 COIL = new Vec3(0.38, -1.5, -1.5);
    private static final Map<WhipMove, Float> ENDS = new EnumMap<>(WhipMove.class);

    static {
        for (WhipMove move : WhipMove.values()) {
            Keyframes.Key[] body = MOVES.get(move);
            Keyframes.Key[] lash = move.lash();
            float end = Math.max(body == null ? 0.0F : Keyframes.end(body), lash == null ? 0.0F : Keyframes.end(lash));
            ENDS.put(move, move.held() ? Float.POSITIVE_INFINITY : Math.max(end, move.ticks() - SETTLE) + SETTLE);
        }
    }

    private WhipPoses() {
    }

    static float end(WhipMove move) {
        return ENDS.get(move);
    }

    static Pose at(WhipMove move, float t, float time, Pose from, @Nullable float[] fromSpeed, double before) {
        float[] lash = lash(move, t, time, from, fromSpeed, before);
        return switch (move.kind()) {
            case WHIRL -> carried(from, fromSpeed, whirl(t, lash), t, BLEND_IN).lashed(lash);
            case SPIN -> carried(from, fromSpeed, spin(lash), t, BLEND_IN).lashed(lash);
            default -> {
                Keyframes.Key[] keys = MOVES.get(move);
                Pose rest = idle(time, (float) Ease.smoother((t - end(move)) / IDLE_IN));
                yield (keys == null ? rest : keyed(keys, t, from, fromSpeed, rest, SETTLE)).lashed(lash);
            }
        };
    }

    // The lash alone, for the moments along it: from where the last move left it into this one, then to rest.
    static float[] lash(WhipMove move, float t, float time, Pose from, @Nullable float[] fromSpeed, double before) {
        float end = end(move);
        float[] rest = idleLash(time, (float) Ease.smoother((t - end) / IDLE_IN));
        if (t >= end) {
            return rest;
        }
        float[] start = from.lash();
        float[] speed = fromSpeed == null ? null : lashOf(fromSpeed);
        Keyframes.Key[] keys = move.lash();
        if (keys != null) {
            return along(keys, Math.max(0.0F, t), start, speed, rest, end);
        }
        float[] aim = move.aim(Math.max(0.0F, t), before);
        if (!move.held()) {
            float calm = (float) Ease.smooth((t - (end - SETTLE)) / SETTLE);
            if (calm > 0.0F) {
                float[] settled = rest.clone();
                WhipLash.nearest(settled, aim);
                for (int c = 0; c < aim.length; c++) {
                    aim[c] = Mth.lerp(calm, aim[c], settled[c]);
                }
            }
        }
        return blendIn(start, speed, aim, t);
    }

    private static float[] blendIn(float[] from, @Nullable float[] speed, float[] to, float t) {
        float u = Math.max(0.0F, t) / BLEND_IN;
        if (u >= 1.0F) {
            return to;
        }
        float[] n = from.clone();
        boolean flip = WhipLash.nearest(n, to);
        if (speed != null) {
            float on = u * BLEND_IN * (1.0F - u) * (1.0F - u);
            for (int c = 0; c < n.length; c++) {
                n[c] += (flip && c == WhipLash.PITCH ? -speed[c] : speed[c]) * on;
            }
        }
        float w = u * u * (3.0F - 2.0F * u);
        for (int c = 0; c < n.length; c++) {
            n[c] = Mth.lerp(w, n[c], to[c]);
        }
        return n;
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
        FlameBody.Torso torso = torso(pose, 1.0F);
        Vec3 shoulder = torso.shoulder(true);
        float[] rot = FlameBody.reach(shoulder, FlameBody.model(body(pose.grip())), torso.twist());
        return FlameBody.body(FlameBody.fist(shoulder, rot, true));
    }

    static FlameBody.Torso torso(Pose pose, float ours) {
        return FlameBody.torso(pose.lean(), pose.twist(), pose.roll(), ours);
    }

    static FlameBody.Legs legs(Pose pose, float ours) {
        return FlameBody.legs(pose.squat(), pose.step(), pose.kneel(), pose.wide(), pose.hop(), ours);
    }

    // Where the eyes go while the whip is made: to the fist as the handle grows, down to the coils as the lash
    // pours out, up to the twirl overhead.
    static float[] look(float t, Pose pose) {
        float[] look = new float[2];
        double forming = window(t, 1.0F, 3.0F, WhipMove.FORMED, 4.0F);
        if (forming > 0.0) {
            glance(look, pose.grip().add(pose.handle().scale(0.1)), forming, 0.45, 0.4, 16.0);
        }
        double pouring = window(t, WhipMove.FORMED + 1.0F, 4.0F, WhipMove.POURED + 1.0F, 4.0F);
        if (pouring > 0.0) {
            glance(look, COIL, pouring, 0.5, 0.3, 24.0);
        }
        double up = window(t, WhipMove.TWIRL - 5.0F, 5.0F, WhipMove.THROW - 1.0F, 3.0F);
        if (up > 0.0) {
            look[0] += (float) (up * Math.toRadians(17.0));
        }
        return look;
    }

    static float[] head(float t, Pose pose) {
        float[] head = new float[3];
        double weight = 0.0;
        double forming = window(t, 1.0F, 3.0F, WhipMove.FORMED, 4.0F);
        if (forming > 0.0) {
            weight += watch(head, fist(pose), forming * 0.8);
        }
        double pouring = window(t, WhipMove.FORMED + 1.0F, 4.0F, WhipMove.POURED + 1.0F, 4.0F);
        if (pouring > 0.0) {
            weight += watch(head, new Vec3(0.35, -1.55, 1.1), pouring * 0.85);
        }
        double up = window(t, WhipMove.TWIRL - 5.0F, 5.0F, WhipMove.THROW - 1.0F, 3.0F);
        if (up > 0.0) {
            weight += watch(head, EYES.add(0.3, 2.2, 1.2), up * 0.7);
        }
        if (weight > 1.0E-6) {
            head[0] /= (float) weight;
            head[1] /= (float) weight;
        }
        head[2] = (float) Math.min(1.0, weight);
        return head;
    }

    private static double window(float t, float from, float in, float to, float out) {
        return Ease.smooth((t - from) / in) * (1.0 - Ease.smooth((t - to) / out));
    }

    private static void glance(float[] look, Vec3 at, double weight, double up, double across, double most) {
        double ahead = Math.max(0.1, -at.z);
        double limit = most * Mth.DEG_TO_RAD;
        look[0] += (float) (weight * Mth.clamp(Math.atan2(at.y, ahead) * up, -limit, limit));
        look[1] += (float) (weight * Mth.clamp(Math.atan2(at.x, ahead) * across, -limit, limit));
    }

    private static double watch(float[] head, Vec3 at, double weight) {
        Vec3 to = at.subtract(EYES);
        head[0] += (float) (weight * -Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        head[1] += (float) (weight * Math.atan2(to.x, to.z));
        return weight;
    }
}
