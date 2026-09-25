package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * How Green Lantern moves with the sword and shield of the construct wheel (see {@link SwordMove}).
 *
 * <p>Every move is made as it looks through his own eyes, the way a game with a sword in first person makes it: a
 * handful of key poses, each saying where the sword hand grips, where the blade points and which way its edge faces,
 * and where the shield hangs and faces. Between the keys everything runs along smooth curves through all of them
 * (never from key to key and stopping), so a cut winds up, whips through the moment it strikes and brakes after, and
 * the tip of the blade draws one clean arc; a key can also be a stop, where the move hangs a moment before it strikes,
 * or say outright how fast it is passed (the flick that tosses the sword, a bang on the shield and its rebound). A new
 * move starts from wherever the last one left the arms, at the speed they were moving, and once a move is over they
 * settle back into the guard, where they breathe a little.
 *
 * <p>Taking them out is played on tracks of its own: the sword hand, its wrist, the shield and the body each have keys
 * of their own, so the body leads, the blade follows the hand and the shield arm answers what the sword does. The
 * tossed sword flies freely: its balance point falls along the curve a thrown thing falls along while it turns over at
 * an even speed, and the fist lets go of it and catches it again at exactly the speed it flies.
 *
 * <p>In the guard the sword stands upright in the right fist, the blade a little forward, and the shield hangs on the
 * left forearm, its back to you and its face turned out and forward. The body seen from outside is posed from the very
 * same poses (see {@link SwordArms}); for that each key also says how far the upper body turns into the move (twist),
 * bends forward (lean) and steps into it (step), and for the spinning cut how far the whole body has spun round
 * (orbit). Last, each pose says how far the arms are still (or again) the game's own empty hands (rest): at 1 they are
 * exactly where the game holds them, so they come out of that and go back into it without a jump.
 *
 * <p>Built up in layers, each on the one before: {@link SwordCurves} (a pose, and the curves through key poses),
 * {@link SwordKeys} (the keys of the moves), {@link SwordEquip} (taking them out); this class plays the moves, and works
 * out the body seen from outside and where he looks.
 */
final class SwordPoses extends SwordEquip {
    // ---- From your own eyes to the body seen from outside ----

    /**
     * Seen from outside, a place before your eyes in first person is a place before the body, measured from the middle
     * of the chest at the height of the shoulders (x to his right, y up, z ahead): across and up it is squeezed and
     * raised this much, and ahead it is this much of the depth, less this much.
     */
    private static final double BODY_ACROSS = 0.65;
    private static final double BODY_UP = 1.12;
    private static final double BODY_RAISE = 0.16;
    private static final double BODY_AHEAD = 0.9;
    private static final double BODY_BACK = 0.46;
    /** How far the shoulders are from the middle of the body, in blocks. */
    static final double SHOULDER = 0.31;
    /** How far the upper body bends forward at a lean of 1, in radians, the way it does when crouching. */
    static final float TILT = 0.5F;
    // The model of a body, in its own pixels: how far out and below the neck the shoulders turn, how far the upper body
    // bends the arms down with it, where the fist grips along the right arm and where the shield sits on the left
    // forearm.
    private static final double PIVOT_ACROSS = 5.0;
    private static final double PIVOT_DOWN = 2.0;
    private static final double BEND_DOWN = 3.2;
    private static final Vec3 FIST = new Vec3(-1.0, 9.4, 0.0);
    private static final Vec3 FOREARM = new Vec3(3.4, 6.4, 0.0);
    // Where his eyes are, from the middle of his chest, and how far ahead of the tossed sword they are (ticks).
    private static final Vec3 EYES = new Vec3(0.0, 0.41, 0.12);
    private static final float LEAD = 1.5F;

    // ---- Playing a move ----

    // The tick each move of keys has settled into its rest.
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

    /**
     * The pose {@code t} ticks into a move, coming in from {@code from} (where the arms were as it began, moving at
     * {@code fromSpeed} per tick, or standing still when that is null) and settling back into the guard (or, after a
     * ram, into the run) once it is over.
     *
     * @param time ticks of the client's own clock, for everything that sways
     */
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

    /**
     * {@code to}, reached from {@code from} over the first few ticks of a move: the arms go on the way they were moving
     * for a moment, and ease into it.
     */
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

    // ---- Seen from outside ----

    /** A place before your eyes in first person as a place before the body (see {@link #BODY_ACROSS}). */
    static Vec3 body(Vec3 view) {
        return new Vec3(view.x * BODY_ACROSS, view.y * BODY_UP + BODY_RAISE, -view.z * BODY_AHEAD - BODY_BACK);
    }

    /** A way before your eyes in first person as a way seen from the body (x to his right, y up, z ahead). */
    static Vec3 way(Vec3 view) {
        return new Vec3(view.x, view.y, -view.z);
    }

    /** A place before the body as seen from the upper body turned {@code twist} to his right. */
    static Vec3 unturned(Vec3 at, float twist) {
        double cos = Mth.cos(twist);
        double sin = Mth.sin(twist);
        return new Vec3(at.x * cos - at.z * sin, at.y, at.x * sin + at.z * cos);
    }

    /** A place before the upper body as it is before the game bends it forward by {@code tilt} (radians). */
    static Vec3 unbent(Vec3 at, float tilt) {
        double cos = Mth.cos(tilt);
        double sin = Mth.sin(tilt);
        return new Vec3(at.x, at.y * cos + at.z * sin, -at.y * sin + at.z * cos);
    }

    /**
     * Where an arm of a body seen from outside points for this pose, as the model's turns of it about x and about y
     * (before the upper body bends forward): the right arm at the fist, the left one at the middle of the shield, each
     * seen from its shoulder on the upper body as that turns into the move.
     */
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

    /**
     * Where the fist of a body seen from outside grips for this pose (or, not {@code right}, where the shield sits on
     * the left forearm), worked out the way the model draws its arms: in blocks from the middle of the chest at the
     * height of the shoulders, x to his right, y up, z ahead, at the model's own size. The game's own swing of the arms
     * as he walks is left out.
     */
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

    /**
     * Where the fist of a body seen from outside grips while he takes them out, {@code t} ticks in (see {@link
     * #drawn}).
     */
    private static Vec3 drawnGrip(float t) {
        return drawn(at(SwordMove.EQUIP, t, 0.0F, REST, null), true);
    }

    /** How fast the fist of a body seen from outside moves while he takes them out, {@code t} ticks in, per tick. */
    private static Vec3 drawnSpeed(float t) {
        float step = 0.02F;
        return drawnGrip(t + step).subtract(drawnGrip(t - step)).scale(0.5 / step);
    }

    // The toss seen from outside, from the middle of the chest: how far the balance point is from the grip, and how high
    // it rises over where it leaves the drawn fist (a real toss, well over his head: about as high as a thing thrown up
    // rises in the time it flies). Worked out below: where it leaves the fist, how fast it flies off and how fast it
    // falls, so it comes down right into the fist where it catches it; and how much faster than the grip of the sword
    // flying freely the fist moves as it lets go and as it catches, which the grip takes along for a moment (ticks).
    private static final double BODY_BALANCE = BALANCE * SwordPainter.TIP * SWORD_SCALE;
    private static final double BODY_TOSS_HIGH = 1.0;
    private static final Vec3 BODY_THROWN_FROM;
    private static final Vec3 BODY_THROWN;
    private static final double BODY_FALL;
    private static final Vec3 BODY_LET_GO;
    private static final Vec3 BODY_CAUGHT;
    private static final double HANDOFF = 2.0;
    // How far the blade seen from outside is turned so it strikes the face of the shield at each bang (a way times its
    // angle), and where on the face it strikes the first time, from the middle of the chest; and how long it takes to
    // turn onto the face before the first bang and back off it after the second, in ticks.
    private static final Vec3 BODY_KNOCK_TURN;
    private static final Vec3 BODY_KNOCK_AGAIN_TURN;
    static final Vec3 BODY_STRUCK;
    private static final float KNOCK_IN = 4.0F;
    private static final float KNOCK_OUT = 5.0F;

    static {
        // The balance point flies the curve a thrown thing flies, from where it leaves the fist to where it lands in
        // it; the grip flying freely moves as the balance point does, less the turn of the blade about it.
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

    /**
     * The tossed sword seen from outside, {@code t} ticks into taking them out, from the middle of the chest: its
     * balance point flies freely from the drawn fist high over his head and falls back into the fist where it catches
     * it, while it turns over the same as before your own eyes. Just after the fist lets go of it and just before it
     * catches it again the grip still goes a little with the fist, so it leaves and lands at the speed the fist moves.
     * Past the catch it flies on.
     */
    static Flight bodyFlight(float t) {
        double tau = t - SwordMove.TOSS;
        Vec3 balance = BODY_THROWN_FROM.add(BODY_THROWN.scale(tau)).subtract(0.0, 0.5 * BODY_FALL * tau * tau, 0.0);
        Vec3 blade = way(tossTurn(SPIN * tau, UPRIGHT));
        Vec3 grip = balance.subtract(blade.scale(BODY_BALANCE)).add(BODY_LET_GO.scale(handoff(tau)))
                .subtract(BODY_CAUGHT.scale(handoff(SwordMove.CATCH - t)));
        return new Flight(grip, blade, way(tossTurn(SPIN * tau, FLAT)));
    }

    /**
     * How far the grip of the tossed sword seen from outside still goes along with the fist {@code since} ticks after
     * the fist let go of it (or before it catches it), times the difference in speed: leaving (or landing) exactly as
     * fast as the fist, then less and less, none after {@link #HANDOFF}. The other way round past the catch (a sword
     * that broke up in the air flies on), so it flies on smoothly there too.
     */
    private static double handoff(double since) {
        if (Math.abs(since) >= HANDOFF) {
            return 0.0;
        }
        double left = 1.0 - Math.abs(since) / HANDOFF;
        return since * left * left;
    }

    /**
     * A bang seen from outside, {@code t} ticks in: the turn (a way times its angle) that brings the blade, as it is
     * drawn in the fist, onto the face of the shield as it is drawn on the forearm, and where on the face it strikes.
     */
    private static Vec3[] bodyKnock(float t) {
        Pose pose = at(SwordMove.EQUIP, t, 0.0F, REST, null);
        Vec3[] strike = bodyStrike(pose);
        return new Vec3[] { turnOnto(way(pose.blade()), strike[1]), strike[0] };
    }

    /**
     * Where the blade of a body seen from outside strikes the face of the shield at a bang, for this pose, as the model
     * draws the arms (from the middle of the chest, see {@link #drawn}); and the way from the fist to where the blade
     * has to lie to strike it there with its edge.
     */
    static Vec3[] bodyStrike(Pose pose) {
        Vec3 grip = drawn(pose, true);
        Vec3 face = way(pose.face());
        Vec3 top = square(way(pose.top()), face);
        // The body's own ways are a mirror image of the world's, so its right is worked out the other way round.
        Vec3 right = top.cross(face).normalize();
        Vec3 middle = drawn(pose, false).add(face.scale(SHIELD_OUT));
        Vec3 struck = onFace(middle, right, top, face, SHIELD_SCALE);
        // The edge that leads into the face (the pose may hold the blade by its other edge: it looks the same).
        Vec3 edge = square(way(KNOCK_EDGE), way(pose.blade()));
        Vec3 aim = struck.subtract(edge.scale(BLADE_WIDE * SWORD_SCALE)).subtract(grip);
        return new Vec3[] { struck, aim };
    }

    /**
     * How far the blade seen from outside is turned (a way times its angle) {@code t} ticks into taking them out, so
     * that it lands right on the face of the shield at each bang: all of it at the bangs, eased in and out round them.
     */
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

    /**
     * How much of the way onto the face of the shield the blade seen from outside is turned {@code t} ticks into taking
     * them out, 0 to 1 (see {@link #bodyKnockTurn}).
     */
    static float bodyKnocking(float t) {
        if (t <= SwordMove.KNOCK) {
            return (float) Ease.smooth((t - (SwordMove.KNOCK - KNOCK_IN)) / KNOCK_IN);
        }
        return t <= SwordMove.KNOCK_AGAIN ? 1.0F
                : (float) (1.0 - Ease.smooth((t - SwordMove.KNOCK_AGAIN) / KNOCK_OUT));
    }

    // ---- Where he looks ----

    /**
     * How far your own eyes follow the sword while you take them out, {@code t} ticks in, as a turn up and a turn to
     * the right (radians): up after the tossed sword (a moment ahead of it, the way eyes lead), at the blade as you
     * look it over, and down at the face of the shield as you bang it. {@code pose} is the pose right now.
     */
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

    /**
     * Where the balance point of the sword is {@code t} ticks into taking them out, before your eyes: in the hand, or
     * in the air while it is tossed.
     */
    private static Vec3 balance(float t) {
        if (tossed(SwordMove.EQUIP, t) >= 0.0F) {
            Flight flight = flight(t);
            return flight.grip().add(flight.blade().scale(OWN_BALANCE));
        }
        Pose pose = at(SwordMove.EQUIP, t, 0.0F, REST, null);
        return pose.hand().add(pose.blade().scale(OWN_BALANCE));
    }

    /** Where the balance point of the sword is {@code t} ticks into taking them out, seen from outside (see above). */
    private static Vec3 bodyBalance(float t) {
        if (tossed(SwordMove.EQUIP, t) >= 0.0F) {
            Flight flight = bodyFlight(t);
            return flight.grip().add(flight.blade().scale(BODY_BALANCE));
        }
        Pose pose = at(SwordMove.EQUIP, t, 0.0F, REST, null);
        return drawn(pose, true).add(way(pose.blade()).scale(BODY_BALANCE));
    }

    /**
     * Adds a glance at {@code at} (before your eyes) to {@code look}: this much of the way up and across, at most so
     * far.
     */
    private static void glance(float[] look, Vec3 at, double weight, double up, double across, double most) {
        double ahead = Math.max(0.1, -at.z);
        double limit = most * Mth.DEG_TO_RAD;
        look[0] += (float) (weight * Mth.clamp(Math.atan2(at.y, ahead) * up, -limit, limit));
        look[1] += (float) (weight * Mth.clamp(Math.atan2(at.x, ahead) * across, -limit, limit));
    }

    /**
     * How his head turns seen from outside while he takes them out, {@code t} ticks in: how far down and to his right
     * it looks (radians), and how much of its own way it gives up for that (0 to 1). He watches the sword go up and
     * come down, looks the blade over, and nods with the bangs. {@code pose} is the pose right now.
     */
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
        // A nod with each bang.
        head[0] += 0.12F * (nod(t - SwordMove.KNOCK) + 0.7F * nod(t - SwordMove.KNOCK_AGAIN));
        return head;
    }

    /** Adds the way to {@code at} (from the middle of the chest) to {@code head}, weighed; returns the weight. */
    private static double watch(float[] head, Vec3 at, double weight) {
        Vec3 to = at.subtract(EYES);
        head[0] += (float) (weight * -Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        head[1] += (float) (weight * Math.atan2(to.x, to.z));
        return weight;
    }

    /** A quick nod {@code since} ticks after a bang: jolted down by it at once, and back up over a few ticks. */
    private static float nod(float since) {
        if (since < 0.0F || since > 5.0F) {
            return 0.0F;
        }
        float u = since / 5.0F;
        return 6.75F * u * (1.0F - u) * (1.0F - u);
    }
}
