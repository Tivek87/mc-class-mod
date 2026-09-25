package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
import nl.tivek.multiversepowers.engine.math.Ease;

final class SwordPoses extends SwordEquip {
    private static final double BODY_ACROSS = 0.65;
    private static final double BODY_UP = 1.12;
    private static final double BODY_RAISE = 0.16;
    private static final double BODY_AHEAD = 0.9;
    private static final double BODY_BACK = 0.46;
    static final double SHOULDER = 0.31;
    static final float TILT = 0.5F;
    private static final double PIVOT_ACROSS = 5.0;
    private static final double PIVOT_DOWN = 2.0;
    private static final double BEND_DOWN = 3.2;
    private static final Vec3 FIST = new Vec3(-1.0, 9.4, 0.0);
    private static final Vec3 FOREARM = new Vec3(3.4, 6.4, 0.0);
    private static final Vec3 EYES = new Vec3(0.0, 0.41, 0.12);
    private static final float LEAD = 1.5F;

    private static final Map<SwordMove, Float> ENDS = new EnumMap<>(SwordMove.class);

    static {
        for (SwordMove move : SwordMove.values()) {
            Track[] tracks = MOVES.get(move);
            if (tracks != null) {
                float end = 0.0F;
                for (Track track : tracks) {
                    end = Math.max(end, track.last());
                }
                ENDS.put(move, end + (move.kind() == SwordMove.Kind.BASH ? RAM_SETTLE : SETTLE));
            }
        }
    }

    private SwordPoses() {
    }

    static Pose at(SwordMove move, float t, float time, Pose from, @Nullable float[] fromSpeed) {
        return switch (move) {
            case FLURRY -> carried(from, fromSpeed, flurry(t), t);
            case CHARGE -> carried(from, fromSpeed, charge(time), t);
            default -> {
                boolean ram = move.kind() == SwordMove.Kind.BASH;
                Pose rest = ram ? charge(time)
                        : idle(time, (float) Ease.smoother((t - ENDS.getOrDefault(move, 0.0F)) / IDLE_IN));
                yield keyed(MOVES.get(move), t, from, fromSpeed, rest, ram ? RAM_SETTLE : SETTLE);
            }
        };
    }

    private static Pose carried(Pose from, @Nullable float[] speed, Pose to, float t) {
        float u = Math.max(0.0F, t) / BLEND_IN;
        if (u >= 1.0F) {
            return to;
        }
        float[] n = from.numbers();
        if (speed != null) {
            float on = u * BLEND_IN * (1.0F - u) * (1.0F - u);
            for (int c = 0; c < n.length; c++) {
                n[c] += speed[c] * on;
            }
        }
        return Pose.of(n).mix(to, (float) Ease.smooth(u));
    }

    static Vec3 body(Vec3 view) {
        return new Vec3(view.x * BODY_ACROSS, view.y * BODY_UP + BODY_RAISE, -view.z * BODY_AHEAD - BODY_BACK);
    }

    static Vec3 way(Vec3 view) {
        return new Vec3(view.x, view.y, -view.z);
    }

    static Vec3 unturned(Vec3 at, float twist) {
        double cos = Mth.cos(twist);
        double sin = Mth.sin(twist);
        return new Vec3(at.x * cos - at.z * sin, at.y, at.x * sin + at.z * cos);
    }

    static Vec3 unbent(Vec3 at, float tilt) {
        double cos = Mth.cos(tilt);
        double sin = Mth.sin(tilt);
        return new Vec3(at.x, at.y * cos + at.z * sin, -at.y * sin + at.z * cos);
    }

    static float[] aim(Pose pose, boolean right) {
        float twist = pose.twist();
        float tilt = TILT * Mth.clamp(pose.lean(), -0.35F, 1.0F);
        Vec3 target = unbent(unturned(body(right ? pose.hand() : pose.shield()), twist), tilt);
        Vec3 reach = target.subtract(right ? SHOULDER : -SHOULDER, 0.0, 0.0);
        reach = reach.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : reach.normalize();
        float pitch = (float) Math.asin(Mth.clamp(reach.y, -1.0, 1.0));
        float yaw = (float) Mth.atan2(reach.x, reach.z);
        return new float[] { -(Mth.HALF_PI + pitch), yaw + twist };
    }

    static Vec3 drawn(Pose pose, boolean right) {
        float[] aim = aim(pose, right);
        float lean = Mth.clamp(pose.lean(), -0.35F, 1.0F);
        double xRot = aim[0] + TILT * lean;
        double yRot = aim[1];
        Vec3 local = right ? FIST : FOREARM;
        double cx = Math.cos(xRot);
        double sx = Math.sin(xRot);
        Vec3 bent = new Vec3(local.x, local.y * cx - local.z * sx, local.y * sx + local.z * cx);
        double cy = Math.cos(yRot);
        double sy = Math.sin(yRot);
        Vec3 turned = new Vec3(bent.x * cy + bent.z * sy, bent.y, -bent.x * sy + bent.z * cy);
        double side = right ? -1.0 : 1.0;
        double twist = pose.twist();
        Vec3 pivot = new Vec3(side * Math.cos(twist) * PIVOT_ACROSS, PIVOT_DOWN + BEND_DOWN * Math.max(0.0F, lean),
                -side * Math.sin(twist) * PIVOT_ACROSS);
        Vec3 model = pivot.add(turned).scale(1.0 / 16.0);
        return new Vec3(-model.x, PIVOT_DOWN / 16.0 - model.y, -model.z);
    }

    private static Vec3 drawnGrip(float t) {
        return drawn(at(SwordMove.EQUIP, t, 0.0F, REST, null), true);
    }

    private static Vec3 drawnSpeed(float t) {
        float step = 0.02F;
        return drawnGrip(t + step).subtract(drawnGrip(t - step)).scale(0.5 / step);
    }

    private static final double BODY_BALANCE = BALANCE * SwordPainter.TIP * SWORD_SCALE;
    private static final double BODY_TOSS_HIGH = 1.0;
    private static final Vec3 BODY_THROWN_FROM;
    private static final Vec3 BODY_THROWN;
    private static final double BODY_FALL;
    private static final Vec3 BODY_LET_GO;
    private static final Vec3 BODY_CAUGHT;
    private static final double HANDOFF = 2.0;
    private static final Vec3 BODY_KNOCK_TURN;
    private static final Vec3 BODY_KNOCK_AGAIN_TURN;
    static final Vec3 BODY_STRUCK;
    private static final float KNOCK_IN = 4.0F;
    private static final float KNOCK_OUT = 5.0F;

    static {
        double time = SwordMove.CATCH - SwordMove.TOSS;
        Vec3 spun = way(BLADE_TURN).scale(BODY_BALANCE);
        Vec3 standing = way(UPRIGHT).scale(BODY_BALANCE);
        BODY_THROWN_FROM = drawnGrip(SwordMove.TOSS).add(standing);
        Vec3 lands = drawnGrip(SwordMove.CATCH).add(standing);
        BODY_FALL = fall(BODY_TOSS_HIGH, (lands.y - BODY_THROWN_FROM.y) / time, time);
        BODY_THROWN = lands.subtract(BODY_THROWN_FROM).scale(1.0 / time).add(0.0, BODY_FALL * time / 2.0, 0.0);
        BODY_LET_GO = drawnSpeed(SwordMove.TOSS).subtract(BODY_THROWN.subtract(spun));
        BODY_CAUGHT = drawnSpeed(SwordMove.CATCH).subtract(BODY_THROWN.subtract(0.0, BODY_FALL * time, 0.0)
                .subtract(spun));
        Vec3[] first = bodyKnock(SwordMove.KNOCK);
        BODY_KNOCK_TURN = first[0];
        BODY_STRUCK = first[1];
        BODY_KNOCK_AGAIN_TURN = bodyKnock(SwordMove.KNOCK_AGAIN)[0];
    }

    static Flight bodyFlight(float t) {
        double tau = t - SwordMove.TOSS;
        Vec3 balance = BODY_THROWN_FROM.add(BODY_THROWN.scale(tau)).subtract(0.0, 0.5 * BODY_FALL * tau * tau, 0.0);
        Vec3 blade = way(tossTurn(SPIN * tau, UPRIGHT));
        Vec3 grip = balance.subtract(blade.scale(BODY_BALANCE)).add(BODY_LET_GO.scale(handoff(tau)))
                .subtract(BODY_CAUGHT.scale(handoff(SwordMove.CATCH - t)));
        return new Flight(grip, blade, way(tossTurn(SPIN * tau, FLAT)));
    }

    private static double handoff(double since) {
        if (Math.abs(since) >= HANDOFF) {
            return 0.0;
        }
        double left = 1.0 - Math.abs(since) / HANDOFF;
        return since * left * left;
    }

    private static Vec3[] bodyKnock(float t) {
        Pose pose = at(SwordMove.EQUIP, t, 0.0F, REST, null);
        Vec3[] strike = bodyStrike(pose);
        return new Vec3[] { turnOnto(way(pose.blade()), strike[1]), strike[0] };
    }

    static Vec3[] bodyStrike(Pose pose) {
        Vec3 grip = drawn(pose, true);
        Vec3 face = way(pose.face());
        Vec3 top = square(way(pose.top()), face);
        // the body's frame mirrors the world's, so right uses the reversed cross order
        Vec3 right = top.cross(face).normalize();
        Vec3 middle = drawn(pose, false).add(face.scale(SHIELD_OUT));
        Vec3 struck = onFace(middle, right, top, face, SHIELD_SCALE);
        Vec3 edge = square(way(KNOCK_EDGE), way(pose.blade()));
        Vec3 aim = struck.subtract(edge.scale(BLADE_WIDE * SWORD_SCALE)).subtract(grip);
        return new Vec3[] { struck, aim };
    }

    static Vec3 bodyKnockTurn(float t) {
        float knock = SwordMove.KNOCK;
        float again = SwordMove.KNOCK_AGAIN;
        double first;
        double second;
        if (t <= knock) {
            first = Ease.smooth((t - (knock - KNOCK_IN)) / KNOCK_IN);
            second = 0.0;
        } else if (t <= again) {
            second = Ease.smooth((t - knock) / (again - knock));
            first = 1.0 - second;
        } else {
            first = 0.0;
            second = 1.0 - Ease.smooth((t - again) / KNOCK_OUT);
        }
        return BODY_KNOCK_TURN.scale(first).add(BODY_KNOCK_AGAIN_TURN.scale(second));
    }

    static float bodyKnocking(float t) {
        if (t <= SwordMove.KNOCK) {
            return (float) Ease.smooth((t - (SwordMove.KNOCK - KNOCK_IN)) / KNOCK_IN);
        }
        return t <= SwordMove.KNOCK_AGAIN ? 1.0F
                : (float) (1.0 - Ease.smooth((t - SwordMove.KNOCK_AGAIN) / KNOCK_OUT));
    }

    static float[] look(float t, Pose pose) {
        float[] look = new float[2];
        double toss = Ease.smooth((t - (SwordMove.TOSS - 3.0F)) / 5.0F)
                * (1.0 - Ease.smooth((t - (SwordMove.CATCH - 4.0F)) / 7.0F));
        if (toss > 0.0) {
            glance(look, balance(t + LEAD), toss, 0.8, 0.35, 25.0);
        }
        double inspect = Ease.smooth((t - (INSPECT_FROM - 4.0F)) / 5.0F)
                * (1.0 - Ease.smooth((t - INSPECT_TO) / 5.0F));
        if (inspect > 0.0) {
            glance(look, pose.hand().add(pose.blade().scale(0.55 * SwordPainter.TIP * OWN_SWORD)), inspect, 0.45,
                    0.45, 16.0);
        }
        double bang = Ease.smooth((t - (SwordMove.KNOCK - 5.0F)) / 4.0F)
                * (1.0 - Ease.smooth((t - (SwordMove.KNOCK_AGAIN + 1.0F)) / 6.0F));
        if (bang > 0.0) {
            glance(look, STRUCK, bang, 0.45, 0.3, 14.0);
        }
        return look;
    }

    private static Vec3 balance(float t) {
        if (tossed(SwordMove.EQUIP, t) >= 0.0F) {
            Flight flight = flight(t);
            return flight.grip().add(flight.blade().scale(OWN_BALANCE));
        }
        Pose pose = at(SwordMove.EQUIP, t, 0.0F, REST, null);
        return pose.hand().add(pose.blade().scale(OWN_BALANCE));
    }

    private static Vec3 bodyBalance(float t) {
        if (tossed(SwordMove.EQUIP, t) >= 0.0F) {
            Flight flight = bodyFlight(t);
            return flight.grip().add(flight.blade().scale(BODY_BALANCE));
        }
        Pose pose = at(SwordMove.EQUIP, t, 0.0F, REST, null);
        return drawn(pose, true).add(way(pose.blade()).scale(BODY_BALANCE));
    }

    private static void glance(float[] look, Vec3 at, double weight, double up, double across, double most) {
        double ahead = Math.max(0.1, -at.z);
        double limit = most * Mth.DEG_TO_RAD;
        look[0] += (float) (weight * Mth.clamp(Math.atan2(at.y, ahead) * up, -limit, limit));
        look[1] += (float) (weight * Mth.clamp(Math.atan2(at.x, ahead) * across, -limit, limit));
    }

    static float[] head(float t, Pose pose) {
        float[] head = new float[3];
        double toss = Ease.smooth((t - (SwordMove.TOSS - 3.0F)) / 5.0F)
                * (1.0 - Ease.smooth((t - (SwordMove.CATCH - 3.0F)) / 7.0F));
        double inspect = Ease.smooth((t - (INSPECT_FROM - 4.0F)) / 5.0F)
                * (1.0 - Ease.smooth((t - INSPECT_TO) / 5.0F));
        double bang = Ease.smooth((t - (SwordMove.KNOCK - 5.0F)) / 4.0F)
                * (1.0 - Ease.smooth((t - (SwordMove.KNOCK_AGAIN + 2.0F)) / 6.0F));
        double weight = 0.0;
        if (toss > 0.0) {
            weight += watch(head, bodyBalance(t + LEAD), toss * 0.9);
        }
        if (inspect > 0.0) {
            Vec3 blade = way(pose.blade()).scale(0.55 * SwordPainter.TIP * SWORD_SCALE);
            weight += watch(head, drawn(pose, true).add(blade), inspect * 0.75);
        }
        if (bang > 0.0) {
            weight += watch(head, BODY_STRUCK, bang * 0.5);
        }
        if (weight > 1.0E-6) {
            head[0] /= (float) weight;
            head[1] /= (float) weight;
        }
        head[2] = (float) Math.min(1.0, weight);
        head[0] += 0.12F * (nod(t - SwordMove.KNOCK) + 0.7F * nod(t - SwordMove.KNOCK_AGAIN));
        return head;
    }

    private static double watch(float[] head, Vec3 at, double weight) {
        Vec3 to = at.subtract(EYES);
        head[0] += (float) (weight * -Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        head[1] += (float) (weight * Math.atan2(to.x, to.z));
        return weight;
    }

    private static float nod(float since) {
        if (since < 0.0F || since > 5.0F) {
            return 0.0F;
        }
        float u = since / 5.0F;
        return 6.75F * u * (1.0F - u) * (1.0F - u);
    }
}
