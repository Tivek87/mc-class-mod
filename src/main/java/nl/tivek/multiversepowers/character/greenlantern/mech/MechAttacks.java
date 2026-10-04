package nl.tivek.multiversepowers.character.greenlantern.mech;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.rig.Ik;

// A built mech's blows on a left click and its beams on a right click, in ticks: where its right hand goes (both
// hands for the slam, the stomp and the Unibeam), how far its body crouches, stoops and twists over the waist, and how
// high the stomping foot is lifted. Hands are placed in the blow's frame: upright on the mech's ground spot and facing
// where its torso faces (x right, y up, z ahead); the arm reaches them from the shoulder with its elbow worked out,
// never stretched.
public final class MechAttacks {
    public static final int NONE = 0;
    public static final int SWEEP = 1;
    public static final int STOMP = 2;
    public static final int SLAM = 3;
    public static final int THROW = 4;
    // A throw whose creature is gone: from where the throw had got to back to the walk.
    public static final int DROP = 5;
    // A right click: one beam from the eye slits; held, the chest's port charges and fires the Unibeam.
    public static final int EYE = 6;
    public static final int UNIBEAM = 7;

    public static final int EYE_FIRE = 3;
    public static final int EYE_SHOWN = 7;
    public static final int UNIBEAM_FROM = 10;
    public static final int UNIBEAM_TO = 60;

    public static final int SWEEP_FROM = 10;
    public static final int SWEEP_TO = 16;
    public static final int STOMP_HIT = 12;
    public static final int SLAM_HIT = 14;
    public static final int GRAB = 11;
    public static final int SMASH = 26;
    public static final int SMASH2 = 39;
    public static final int RELEASE = 52;
    public static final int DROP_TICKS = 14;
    // From the palm's middle to its inside face, where a held creature's skin lies.
    public static final double SKIN = 0.45;
    private static final double REACH = MechScript.UPPER_ARM + MechScript.PALM_ALONG;
    private static final double GRAB_ROOM = 0.15;
    private static final int GRAB_AIMS = 4;
    private static final double GRAB_MISS = 0.02;
    private static final double NEAREST = 2.0;
    // Where, seen from the waist, a creature lies best for the right hand to pick up: this far to the right.
    private static final double SWEET = Math.atan2(2.5, 4.0);
    private static final double MOST_TURN = 0.8;
    private static final int[] LENGTHS = { 0, 30, 26, 34, 66, DROP_TICKS, 16, 78 };

    // How the body stands under a blow: how far its hips sink, its torso stoops forward (radians) and twists to its
    // left over the waist, how high the right foot is lifted, and how far (radians, to its left) the whole blow turns
    // towards the creature a throw goes for.
    public record Body(double crouch, double stoop, double twist, double foot, double turn) {
        public static final Body STILL = new Body(0.0, 0.0, 0.0, 0.0, 0.0);
    }

    // A blow as the mech strikes it: which one, how far in (ticks, a fraction between two), for a drop how far the
    // throw had got, and for a throw how far it turns towards its creature (radians, to its left).
    public record Blow(int kind, double age, int from, double turn) {
        public static final Blow NONE = new Blow(MechAttacks.NONE, 0.0, 0, 0.0);

        public boolean striking() {
            return this.kind != MechAttacks.NONE;
        }
    }

    // The creature in the mech's hand: the middle of its box in the world and its half width and height.
    public record Held(Vec3 center, double halfWidth, double halfHeight) {
    }

    private record Move(Keyframes.Key[] hand, Keyframes.Key[] body, boolean both, Vec3 pole, int[][] pulls) {
    }

    private static final Move[] MOVES = new Move[8];

    static {
        Vec3 rest = new Vec3(4.0, 4.1, 2.0);
        Vec3 inward = new Vec3(-1.0, 0.0, 0.0);
        MOVES[SWEEP] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(7, true, new Vec3(-0.8, 5.3, 2.5), new Vec3(0.43, 0.2, -0.88), 0.2, 0.1, 1.0, 0.0, 0.0),
                hand(SWEEP_FROM, false, new Vec3(-0.6, 1.8, 6.1), new Vec3(-1.0, 0.0, -0.1), 0.15, 0.05, 1.0, 0.0,
                        0.0),
                hand(12, false, new Vec3(2.0, 1.7, 6.3), new Vec3(-1.0, 0.0, 0.05), 0.15, 0.05, 1.0, 0.0, 0.0),
                hand(14, false, new Vec3(5.0, 1.7, 5.0), new Vec3(-0.85, 0.0, 0.5), 0.15, 0.05, 1.0, 0.0, 0.0),
                hand(SWEEP_TO, false, new Vec3(6.9, 1.9, 2.0), new Vec3(-0.3, 0.0, 0.95), 0.15, 0.05, 1.0, 0.0, 0.0),
                hand(21, true, new Vec3(6.6, 3.4, -0.6), new Vec3(0.0, 0.0, 1.0), 0.3, 0.3, 1.0, 0.0, 0.0),
                hand(30, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(7, true, bent(0.5, 0.2, 0.45, 0.0)),
                        body(SWEEP_FROM, false, bent(1.9, 0.95, 0.45, 0.0)),
                        body(12, false, bent(1.9, 0.95, 0.2, 0.0)),
                        body(14, false, bent(1.9, 0.95, -0.1, 0.0)),
                        body(SWEEP_TO, false, bent(1.8, 0.9, -0.4, 0.0)),
                        body(21, true, bent(1.2, 0.5, -0.5, 0.0)), body(30, true, Body.STILL) },
                false, new Vec3(1.0, -0.3, -0.6), new int[0][]);
        Vec3 down = new Vec3(-0.4, -0.9, 0.0);
        MOVES[STOMP] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(9, true, new Vec3(5.8, 6.6, 1.2), new Vec3(0.0, -1.0, 0.2), 0.6, 0.5, 1.0, 0.0, 0.0),
                hand(STOMP_HIT, true, new Vec3(4.6, 3.8, 2.4), down, 0.9, 0.3, 1.0, 0.0, 0.0),
                hand(16, true, new Vec3(4.6, 3.9, 2.3), down, 0.9, 0.3, 1.0, 0.0, 0.0),
                hand(26, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(9, true, bent(-0.15, -0.12, 0.0, 2.8)),
                        body(STOMP_HIT, true, bent(0.9, 0.3, 0.0, 0.0)),
                        body(16, true, bent(0.7, 0.25, 0.0, 0.0)), body(26, true, Body.STILL) },
                true, new Vec3(1.0, -0.2, -0.5), new int[][] { { 9, STOMP_HIT, 3 } });
        Vec3 edge = new Vec3(-1.0, 0.0, 0.2);
        MOVES[SLAM] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(9, true, new Vec3(1.6, 12.6, 0.9), edge, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(SLAM_HIT, true, new Vec3(1.5, 1.0, 5.0), inward, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(20, true, new Vec3(1.5, 1.25, 4.9), inward, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(34, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(9, true, bent(-0.2, -0.15, 0.0, 0.0)),
                        body(SLAM_HIT, true, bent(2.0, 1.0, 0.0, 0.0)),
                        body(20, true, bent(1.9, 0.95, 0.0, 0.0)), body(34, true, Body.STILL) },
                true, new Vec3(1.0, 0.1, -0.4), new int[][] { { 9, SLAM_HIT, 3 } });
        Vec3 under = new Vec3(0.0, -1.0, 0.25);
        Vec3 high = new Vec3(2.0, 11.2, 1.7);
        Vec3 ground = new Vec3(1.3, 0.0, 4.3);
        Body low = bent(1.8, 0.8, 0.1, 0.0);
        Body up = bent(0.15, -0.1, 0.0, 0.0);
        MOVES[THROW] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(9, true, new Vec3(2.0, 1.8, 4.6), under, 0.0, 0.9, 1.0, 1.0, 0.0),
                hand(GRAB + 1, true, new Vec3(2.0, 1.8, 4.6), under, 1.0, 0.2, 1.0, 1.0, 0.0),
                hand(20, true, high, under, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(SMASH, true, ground, under, 1.0, 0.2, 1.0, 0.0, 1.0),
                hand(33, true, high, under, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(SMASH2, true, ground, under, 1.0, 0.2, 1.0, 0.0, 1.0),
                hand(48, true, new Vec3(3.4, 10.6, -1.2), new Vec3(0.0, -0.5, 0.8), 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(53, false, new Vec3(1.2, 9.2, 4.6), new Vec3(0.0, -0.2, 1.0), 0.1, 0.6, 1.0, 0.0, 0.0),
                hand(57, true, new Vec3(0.9, 7.0, 4.4), new Vec3(0.0, -0.3, 1.0), 0.05, 0.8, 1.0, 0.0, 0.0),
                hand(66, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(9, true, bent(1.9, 0.95, 0.1, 0.0)),
                        body(GRAB + 1, true, bent(1.9, 0.95, 0.1, 0.0)), body(20, true, up),
                        body(SMASH, true, low), body(33, true, up), body(SMASH2, true, low),
                        body(48, true, bent(0.3, -0.25, -0.4, 0.0)),
                        body(53, false, bent(0.6, 0.25, 0.3, 0.0)),
                        body(57, true, bent(0.7, 0.3, 0.35, 0.0)), body(66, true, Body.STILL) },
                false, new Vec3(1.0, -0.2, -0.5), new int[][] { { 20, SMASH, 2 }, { 33, SMASH2, 2 }, { 48, 53, 2 } });
        // The eye beam: the head snaps forward as it fires and the body rocks back from it; the arms swing on.
        MOVES[EYE] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(LENGTHS[EYE], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(EYE_FIRE, true, bent(0.15, 0.08, 0.0, 0.0)),
                        body(EYE_FIRE + 2, false, bent(0.2, -0.08, 0.0, 0.0)),
                        body(LENGTHS[EYE], true, Body.STILL) },
                false, new Vec3(1.0, -0.3, -0.6), new int[0][]);
        // The Unibeam: knees bent and feet planted, it leans back and pulls both fists back beside its ribs, baring
        // the port on its chest, and rocks back further as the beam bursts out.
        Vec3 ribs = new Vec3(3.3, 6.3, -1.3);
        Vec3 palmUp = new Vec3(-0.6, 0.8, 0.0);
        MOVES[UNIBEAM] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(UNIBEAM_FROM - 2, true, ribs, palmUp, 0.95, 0.05, 1.0, 0.0, 0.0),
                hand(UNIBEAM_TO, true, ribs, palmUp, 0.95, 0.05, 1.0, 0.0, 0.0),
                hand(LENGTHS[UNIBEAM], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL),
                        body(UNIBEAM_FROM - 2, true, bent(0.55, -0.2, 0.0, 0.0)),
                        body(UNIBEAM_FROM + 2, false, bent(0.75, -0.3, 0.0, 0.0)),
                        body(UNIBEAM_TO, true, bent(0.6, -0.24, 0.0, 0.0)),
                        body(LENGTHS[UNIBEAM], true, Body.STILL) },
                true, new Vec3(0.6, -0.4, -1.0), new int[0][]);
    }

    private MechAttacks() {
    }

    private static Keyframes.Key hand(int t, boolean stop, Vec3 at, Vec3 palm, double curl, double spread,
            double weight, double grab, double below) {
        Vec3 p = palm.normalize();
        return new Keyframes.Key(t, stop, new float[] { (float) at.x, (float) at.y, (float) at.z, (float) p.x,
                (float) p.y, (float) p.z, (float) curl, (float) spread, (float) weight, (float) grab, (float) below });
    }

    private static Body bent(double crouch, double stoop, double twist, double foot) {
        return new Body(crouch, stoop, twist, foot, 0.0);
    }

    private static Keyframes.Key body(int t, boolean stop, Body body) {
        return new Keyframes.Key(t, stop, new float[] { (float) body.crouch(), (float) body.stoop(),
                (float) body.twist(), (float) body.foot() });
    }

    public static int length(int kind) {
        return kind == DROP ? DROP_TICKS : kind > NONE && kind < LENGTHS.length ? LENGTHS[kind] : 0;
    }

    // The blow in a whole number sent with the mech (see MechScript.variant): kind, age, a drop's start and a throw's
    // turn in whole degrees.
    public static int pack(int kind, int age, int from, double turn) {
        int degrees = (int) Math.round(Math.toDegrees(turn)) + 64;
        return kind == NONE ? 0 : kind | Mth.clamp(age, 0, 127) << 3 | Mth.clamp(from, 0, 127) << 10
                | Mth.clamp(degrees, 0, 127) << 17;
    }

    public static Blow unpack(int packed) {
        return packed == 0 ? Blow.NONE : new Blow(packed & 7, packed >>> 3 & 127, packed >>> 10 & 127,
                Math.toRadians((packed >>> 17 & 127) - 64));
    }

    // How far a throw turns to bring a creature at `at` round to where the right hand reaches best.
    public static double turnFor(MechScript.Stage frame, Vec3 at) {
        Vec3 local = frame.local(at);
        return Mth.clamp(SWEET - Math.atan2(local.x, local.z), -MOST_TURN, MOST_TURN);
    }

    // The frame a blow's hands are placed in, from the upright one facing the pilot's look: turned to its creature.
    public static MechScript.Stage aimed(MechScript.Stage upright, Body body) {
        return body.turn() == 0.0 ? upright : upright.turned(Vec3.ZERO, Vec3.ZERO, body.turn(), 0.0, 0.0);
    }

    // What lands at an exact tick: how hard (0 nothing), for the ground's shake.
    public static double impact(int kind, int age) {
        return switch (kind) {
            case STOMP -> age == STOMP_HIT ? 2.2 : 0.0;
            case SLAM -> age == SLAM_HIT ? 3.0 : 0.0;
            case THROW -> age == SMASH || age == SMASH2 ? 1.4 : 0.0;
            // The Unibeam bursts out with a jolt and rumbles while it lasts.
            case UNIBEAM -> age == UNIBEAM_FROM ? 1.2 : age > UNIBEAM_FROM && age < UNIBEAM_TO && age % 5 == 0 ? 0.3
                    : 0.0;
            default -> 0.0;
        };
    }

    public static Body body(Blow blow) {
        if (blow.kind() == DROP) {
            Body from = body(new Blow(THROW, blow.from(), 0, blow.turn()));
            double kept = 1.0 - Ease.smooth(blow.age() / DROP_TICKS);
            return new Body(from.crouch() * kept, from.stoop() * kept, from.twist() * kept, 0.0, from.turn() * kept);
        }
        Move move = move(blow.kind());
        if (move == null) {
            return Body.STILL;
        }
        float[] v = values(move.body(), move, blow.age());
        // The turn comes and goes with the arm the blow takes over.
        double turn = blow.turn() == 0.0 ? 0.0 : blow.turn() * values(move.hand(), move, blow.age())[8];
        return new Body(v[0], v[1], v[2], Math.max(0.0, v[3]), turn);
    }

    // The frame the blow's hands are placed in: upright on the ground spot, facing as the torso would without the
    // blow's own twist.
    public static MechScript.Stage frame(Vec3 base, MechScript.Stage torso, double twist) {
        Vec3 ahead = Vectors.spin(torso.ahead(), Vectors.UP, -twist);
        return MechScript.Stage.of(base, new Vec3(ahead.x, 0.0, ahead.z), MechScript.COCKPIT.y, MechScript.COCKPIT.z);
    }

    // The torso over an upright frame as a blow bends it (how the server stands it; clients add their walk).
    public static MechScript.Stage torso(MechScript.Stage frame, Body body) {
        if (body.crouch() == 0.0 && body.stoop() == 0.0 && body.twist() == 0.0) {
            return frame;
        }
        return frame.turned(MechScript.WAIST, new Vec3(0.0, -body.crouch(), 0.0), body.twist(), -body.stoop(), 0.0);
    }

    // One arm under a blow, in the torso's places as MechMoves.Arm has it, blended from the arm it had (`was`) as
    // the blow takes it over and gives it back.
    public static MechMoves.Arm arm(Blow blow, boolean right, MechScript.Stage frame, MechScript.Stage torso,
            @Nullable Held held, MechMoves.Arm was) {
        Aim aim = aim(blow, right, frame, held);
        if (aim == null) {
            return was;
        }
        Vec3 shoulder = right ? MechScript.SHOULDER : MechScript.mirror(MechScript.SHOULDER);
        Vec3 pole = localDir(torso, aim.pole());
        MechMoves.Arm arm = reach(torso.local(aim.at()), shoulder, pole, localDir(torso, aim.palm()), aim, was);
        if (held != null && aim.grab() > 0.0) {
            // The palm turns square to the forearm: aimed again from where it faces, until it lies on the creature.
            for (int i = 0; i < GRAB_AIMS; i++) {
                Vec3 on = held.center().subtract(torso.dir(arm.palm()).scale(SKIN + held.halfWidth()));
                arm = reach(torso.local(aim.key().lerp(on, aim.grab())), shoulder, pole, arm.palm(), aim, was);
            }
        }
        return aim.weight() >= 1.0 ? arm : blend(was, arm, aim.weight(), shoulder);
    }

    private static MechMoves.Arm reach(Vec3 target, Vec3 shoulder, Vec3 pole, Vec3 palm, Aim aim,
            MechMoves.Arm was) {
        double[] out = new double[3];
        Ik.twoBone(new double[] { shoulder.x, shoulder.y, shoulder.z }, new double[] { target.x, target.y, target.z },
                new double[] { pole.x, pole.y, pole.z }, MechScript.UPPER_ARM, MechScript.PALM_ALONG, out);
        Vec3 elbow = new Vec3(out[0], out[1], out[2]);
        Vec3 way = target.subtract(elbow);
        way = way.lengthSqr() < 1.0E-8 ? was.way() : way.normalize();
        return new MechMoves.Arm(elbow, way, MechMoves.square(palm, way), aim.curl(), aim.spread(), 1.0);
    }

    // Where a hand is sent, in the world: its palm's middle (`key` as the blow's keys have it, `at` taken over by the
    // creature it grabs by `grab`), which way the palm faces, the fingers and how much of the arm the blow has taken
    // over; null while the blow leaves it be.
    record Aim(Vec3 key, Vec3 at, Vec3 palm, Vec3 pole, double curl, double spread, double weight, double grab) {
    }

    @Nullable
    static Aim aim(Blow blow, boolean right, MechScript.Stage frame, @Nullable Held held) {
        int kind = blow.kind() == DROP ? THROW : blow.kind();
        Move move = move(kind);
        if (move == null || !right && !move.both()) {
            return null;
        }
        double age = blow.kind() == DROP ? blow.from() : blow.age();
        float[] v = values(move.hand(), move, age);
        double weight = v[8];
        double curl = v[6];
        if (blow.kind() == DROP) {
            weight *= 1.0 - Ease.smooth(blow.age() / DROP_TICKS);
            curl *= 1.0 - Ease.smooth(blow.age() / 4.0);
        }
        if (weight <= 0.0) {
            return null;
        }
        double side = right ? 1.0 : -1.0;
        Vec3 palm = frame.dir(new Vec3(side * v[3], v[4], v[5]).normalize());
        Vec3 key = frame.point(new Vec3(side * v[0], v[1], v[2]));
        double grab = held == null ? 0.0 : v[9];
        Vec3 at = key;
        if (held != null) {
            double skin = SKIN + held.halfWidth();
            key = key.add(0.0, v[10] * (held.halfHeight() + skin * Math.max(0.0, -palm.y)), 0.0);
            at = key.lerp(held.center().subtract(palm.scale(skin)), grab);
        }
        Vec3 pole = frame.dir(new Vec3(side * move.pole().x, move.pole().y, move.pole().z));
        return new Aim(key, at, palm, pole, curl, v[7], weight, grab);
    }

    private static MechMoves.Arm blend(MechMoves.Arm from, MechMoves.Arm to, double u, Vec3 shoulder) {
        Vec3 elbow = from.elbow().lerp(to.elbow(), u).subtract(shoulder);
        elbow = shoulder.add(elbow.lengthSqr() < 1.0E-8 ? to.elbow().subtract(shoulder)
                : elbow.normalize().scale(MechScript.UPPER_ARM));
        Vec3 way = from.way().lerp(to.way(), u);
        way = way.lengthSqr() < 1.0E-8 ? to.way() : way.normalize();
        return new MechMoves.Arm(elbow, way, MechMoves.square(from.palm().lerp(to.palm(), u), way),
                Mth.lerp(u, from.curl(), to.curl()), Mth.lerp(u, from.spread(), to.spread()), 1.0);
    }

    // Where the middle of a held creature sits, in the torso's places: against the palm's inside.
    public static Vec3 grip(MechMoves.Arm arm, double halfWidth) {
        return arm.hand().add(arm.palm().scale(SKIN + halfWidth));
    }

    // Whether the right hand can reach a creature to pick it up: in front of the knees, not up in the air, and
    // within the arm's length of the shoulder once the body has bent down for it. `frame` is already turned to it.
    public static boolean reachable(MechScript.Stage frame, Held held) {
        Vec3 local = frame.local(held.center());
        if (local.z < NEAREST || local.y < -1.5 || local.y - held.halfHeight() > 2.5) {
            return false;
        }
        Blow grab = new Blow(THROW, GRAB, 0, 0.0);
        MechScript.Stage torso = torso(frame, body(grab));
        MechMoves.Arm arm = arm(grab, true, frame, torso, held, MechMoves.arm(true, frame, MechScript.SETTLED));
        return arm.hand().distanceTo(MechScript.SHOULDER) <= REACH - GRAB_ROOM
                && torso.point(grip(arm, held.halfWidth())).distanceTo(held.center()) < GRAB_MISS;
    }

    @Nullable
    private static Move move(int kind) {
        return kind > NONE && kind < MOVES.length ? MOVES[kind] : null;
    }

    // The keys at t; within a pull (from, to, power) the values run straight from one key to the next, gathering
    // speed into the blow instead of easing off before it.
    private static float[] values(Keyframes.Key[] keys, Move move, double t) {
        for (int[] pull : move.pulls()) {
            if (t > pull[0] && t < pull[1]) {
                float[] from = Keyframes.at(keys, pull[0]);
                float[] to = Keyframes.at(keys, pull[1]);
                float u = (float) Math.pow((t - pull[0]) / (pull[1] - pull[0]), pull[2]);
                for (int i = 0; i < from.length; i++) {
                    from[i] = Mth.lerp(u, from[i], to[i]);
                }
                return from;
            }
        }
        return Keyframes.at(keys, (float) t);
    }

    private static Vec3 localDir(MechScript.Stage stage, Vec3 world) {
        return new Vec3(world.dot(stage.right()), world.dot(stage.up()), world.dot(stage.ahead()));
    }
}
