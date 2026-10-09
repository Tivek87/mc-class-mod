package nl.tivek.multiversepowers.character.greenlantern.mech;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.rig.Ik;

// A built mech's moves, in ticks: its blows on a left click, its eyes on a right click, the missile arm, the rocket
// boots, the dive and the spin. For each, where its right hand goes (both hands for the slam, the stomp, the flight,
// the dive and the spin), how far its body crouches, stoops and twists over the waist, and how high the stomping foot
// is lifted. Hands are placed in the blow's frame: upright on the mech's ground spot and facing where its torso faces
// (x right, y up, z ahead); the arm reaches them from the shoulder with its elbow worked out, never stretched.
public final class MechAttacks {
    public static final int NONE = 0;
    public static final int SWEEP = 1;
    public static final int STOMP = 2;
    public static final int SLAM = 3;
    public static final int THROW = 4;
    // A throw whose creature is gone: from where the throw had got to back to the walk.
    public static final int DROP = 5;
    // A right click: tapped, one ray from the eye slits; held, a beam from them for as long as it is held.
    public static final int EYE = 6;
    public static final int GLARE = 7;
    // The combo's first blow: a straight right fist driven down at what stands before it.
    public static final int CROSS = 8;
    // The missile arm: the right arm aimed at the crosshair, the back of its hand open on a pod of missiles.
    public static final int AIM = 9;
    // Off the ground on the rocket boots, the dive from the air onto the ground, and the spin on folded legs.
    public static final int FLY = 10;
    public static final int DIVE = 11;
    public static final int SPIN = 12;

    public static final int EYE_FIRE = 3;
    public static final int EYE_SHOWN = 7;
    // Held, the beam burns from GLARE_FIRE until it is let go, GLARE_MOST at most, and dies away over GLARE_FADE.
    public static final int GLARE_FIRE = 4;
    public static final int GLARE_MOST = 64;
    public static final int GLARE_FADE = 8;

    public static final int CROSS_HIT = 10;
    public static final int SWEEP_FROM = 10;
    public static final int SWEEP_TO = 16;
    public static final int STOMP_HIT = 12;
    public static final int SLAM_HIT = 14;
    public static final int GRAB = 11;
    public static final int SMASH = 26;
    public static final int SMASH2 = 39;
    public static final int RELEASE = 52;
    public static final int DROP_TICKS = 14;
    // The missile arm opens over AIM_OPEN ticks, stays up AIM_MOST at most and closes over AIM_CLOSE; it fires SALVOS
    // salvos at most, each kicking the arm back for SALVO_KICK ticks.
    public static final int AIM_OPEN = 8;
    public static final int AIM_MOST = 200;
    public static final int AIM_CLOSE = 10;
    public static final int SALVOS = 3;
    public static final int SALVO_KICK = 8;
    // The rocket boots: crouched, it springs up at FLY_LAUNCH and flies FLY_MOST ticks at most; out of thrust, it
    // spreads its arms by FLY_FALL and falls until it lands at FLY_LAND.
    public static final int FLY_LAUNCH = 6;
    public static final int FLY_MOST = 100;
    public static final int FLY_FALL = FLY_LAUNCH + FLY_MOST + 6;
    public static final int FLY_LAND = FLY_FALL + 1;
    // The dive: wound up over its head, it plunges fists first from DIVE_PLUNGE until it strikes the ground at
    // DIVE_LAND.
    public static final int DIVE_PLUNGE = 7;
    public static final int DIVE_LAND = 8;
    // The spin: its legs fold under it by SPIN_FROM, its torso spins SPIN_TURNS times round till SPIN_TO (3 seconds)
    // and its legs unfold by the end; each arm hammers the ground every SPIN_BEAT ticks.
    public static final int SPIN_FROM = 10;
    public static final int SPIN_TO = 70;
    public static final double SPIN_TURNS = 4.0;
    public static final int SPIN_BEAT = 16;
    public static final double SPIN_CROUCH = 5.1;
    // From the palm's middle to its inside face, where a held creature's skin lies.
    public static final double SKIN = 0.45;
    private static final double REACH = MechScript.UPPER_ARM + MechScript.PALM_ALONG;
    // How far from the shoulder the aimed missile arm holds its hand, almost straight.
    private static final double AIM_REACH = REACH * 0.94;
    private static final double GRAB_ROOM = 0.15;
    private static final int GRAB_AIMS = 4;
    private static final double GRAB_MISS = 0.02;
    private static final double NEAREST = 2.0;
    // Where, seen from the waist, a creature lies best for the right hand to pick up: this far to the right.
    private static final double SWEET = Math.atan2(2.5, 4.0);
    private static final double MOST_TURN = 0.8;
    static final int[] LENGTHS = { 0, 30, 26, 36, 66, DROP_TICKS, 16, GLARE_MOST + GLARE_FADE, 26,
            AIM_MOST + AIM_CLOSE, FLY_LAND + 14, 32, SPIN_TO + 16 };

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

    // The spin's fists (in the spinning torso's frame): swung round low, raised high and hammered onto the ground.
    // Each trails a little behind the way it spins.
    private static final Vec3 SPIN_LOW = new Vec3(6.2, 2.6, -0.6);
    private static final Vec3 SPIN_HIGH = new Vec3(5.0, 8.4, -1.2);
    private static final Vec3 SPIN_GROUND = new Vec3(6.0, 1.0, -0.3);
    private static final double SPIN_RAISE = 0.45;
    public static final double SPIN_SLAMMED = 0.6;

    private MechAttacks() {
    }

    public static int length(int kind) {
        return kind == DROP ? DROP_TICKS : kind > NONE && kind < LENGTHS.length ? LENGTHS[kind] : 0;
    }

    // The blow in a whole number sent with the mech (see MechScript.variant): kind, age, `from` (a drop's start; the
    // missile arm's salvos, see salvos) and a throw's turn in whole degrees.
    public static int pack(int kind, int age, int from, double turn) {
        int degrees = (int) Math.round(Math.toDegrees(turn)) + 64;
        return kind == NONE ? 0 : kind | Mth.clamp(age, 0, 255) << 4 | Mth.clamp(from, 0, 127) << 12
                | Mth.clamp(degrees, 0, 127) << 19;
    }

    public static Blow unpack(int packed) {
        return packed == 0 ? Blow.NONE : new Blow(packed & 15, packed >>> 4 & 255, packed >>> 12 & 127,
                Math.toRadians((packed >>> 19 & 127) - 64));
    }

    // The missile arm's `from`: how many salvos it has fired, and how many ticks ago the last one (at most 31).
    public static int salvos(int fired, int since) {
        return fired | Mth.clamp(since, 0, 31) << 2;
    }

    public static int fired(Blow blow) {
        return blow.from() & 3;
    }

    // How hard the last salvo still kicks the arm back: hardest just after it fires, gone once it has settled.
    public static double kick(Blow blow) {
        if (blow.kind() != AIM || fired(blow) == 0) {
            return 0.0;
        }
        double since = (blow.from() >>> 2) + blow.age() - Math.floor(blow.age()) - 1.0;
        return since < 0.0 || since > SALVO_KICK ? 0.0 : Ease.jolt(since / SALVO_KICK);
    }

    // How far the back of the right hand stands open on its missiles (0 shut).
    public static double opened(Blow blow) {
        if (blow.kind() != AIM) {
            return 0.0;
        }
        double age = blow.age();
        return Ease.smooth((age - 2.0) / (AIM_OPEN - 2.0)) * (1.0 - Ease.smooth((age - AIM_MOST) / (AIM_CLOSE - 2.0)));
    }

    // How far its legs are folded under it for the spin (0 standing, 1 on its knees).
    public static double folded(Blow blow) {
        return blow.kind() == SPIN ? Mth.clamp(body(blow).crouch() / SPIN_CROUCH, 0.0, 1.0) : 0.0;
    }

    // Whether the mech stands its ground for this move, its walk stopped: every blow of the combo, the spin, and the
    // rocket boots and the dive on the ground; its eyes and the missile arm let it walk on.
    public static boolean plants(Blow blow) {
        return switch (blow.kind()) {
            case NONE, EYE, GLARE, AIM -> false;
            default -> true;
        };
    }

    // Whether the mech is off the ground on its rocket boots or diving from the air: its feet hang, its walk waits.
    public static boolean airborne(Blow blow) {
        return blow.kind() == FLY && blow.age() >= FLY_LAUNCH && blow.age() < FLY_LAND
                || blow.kind() == DIVE && blow.age() < DIVE_LAND;
    }

    // Whether the rocket boots fire now: from the launch until their thrust runs out, and holding the dive up while it
    // winds up; it plunges with them cut.
    public static boolean thrusting(Blow blow) {
        return blow.kind() == FLY && blow.age() >= FLY_LAUNCH - 1 && blow.age() < FLY_FALL - 6
                || blow.kind() == DIVE && blow.age() < DIVE_PLUNGE;
    }

    // How far the spin has turned the torso at `age`: SPIN_TURNS whole turns, gathering speed and slowing again.
    public static double spinTwist(double age) {
        double u = Mth.clamp((age - SPIN_FROM + 2.0) / (SPIN_TO - SPIN_FROM + 6.0), 0.0, 1.0);
        return Math.PI * 2.0 * SPIN_TURNS * Ease.smooth(u);
    }

    // Where in its beat an arm of the spin is (0 to 1, the left half a beat behind the right; -1 before and after the
    // spin): raised up to SPIN_RAISE, hammered down to SPIN_SLAMMED, then swung out low again.
    public static double beat(double age, boolean right) {
        double from = age - SPIN_FROM - (right ? 0.0 : SPIN_BEAT * 0.5);
        return from < 0.0 || age > SPIN_TO ? -1.0 : from % SPIN_BEAT / SPIN_BEAT;
    }

    // Whether the spin's right (or left) fist strikes the ground at this tick.
    public static boolean slams(int age, boolean right) {
        double now = beat(age, right);
        double was = beat(age - 1, right);
        return was >= 0.0 && was < SPIN_SLAMMED && now >= SPIN_SLAMMED;
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
            case CROSS -> age == CROSS_HIT ? 0.9 : 0.0;
            case STOMP -> age == STOMP_HIT ? 2.2 : 0.0;
            case SLAM -> age == SLAM_HIT ? 3.0 : 0.0;
            case THROW -> age == SMASH || age == SMASH2 ? 1.4 : 0.0;
            case FLY -> age == FLY_LAUNCH ? 1.4 : age == FLY_LAND ? 2.4 : 0.0;
            case DIVE -> age == DIVE_LAND ? 3.6 : 0.0;
            // Its knees strike the ground, then every fist it hammers down.
            case SPIN -> age == SPIN_FROM ? 2.0 : slams(age, true) || slams(age, false) ? 0.8 : 0.0;
            default -> 0.0;
        };
    }

    public static Body body(Blow blow) {
        if (blow.kind() == DROP) {
            Body from = body(new Blow(THROW, blow.from(), 0, blow.turn()));
            double kept = 1.0 - Ease.smooth(blow.age() / DROP_TICKS);
            return new Body(from.crouch() * kept, from.stoop() * kept, from.twist() * kept, 0.0, from.turn() * kept);
        }
        MechAttackKeys.Move move = MechAttackKeys.move(blow.kind());
        if (move == null) {
            return Body.STILL;
        }
        float[] v = MechAttackKeys.values(move.body(), move, blow.age());
        // The turn comes and goes with the arm the blow takes over.
        double turn = blow.turn() == 0.0 ? 0.0
                : blow.turn() * MechAttackKeys.values(move.hand(), move, blow.age())[8];
        double twist = blow.kind() == SPIN ? spinTwist(blow.age()) : v[2];
        return new Body(v[0], v[1], twist, Math.max(0.0, v[3]), turn);
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
        return arm(blow, right, frame, torso, held, was, null);
    }

    // As above; `target` is what the pilot's crosshair rests on, for the missile arm (null: straight ahead).
    public static MechMoves.Arm arm(Blow blow, boolean right, MechScript.Stage frame, MechScript.Stage torso,
            @Nullable Held held, MechMoves.Arm was, @Nullable Vec3 target) {
        Aim aim = switch (blow.kind()) {
            case AIM -> right ? aimArm(blow, torso, target) : null;
            case SPIN -> spinAim(blow, right, frame(frame.base(), torso, 0.0));
            default -> aim(blow, right, frame, held);
        };
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
        MechAttackKeys.Move move = MechAttackKeys.move(kind);
        if (move == null || !right && !move.both() && move.left() == null) {
            return null;
        }
        double age = blow.kind() == DROP ? blow.from() : blow.age();
        float[] v = MechAttackKeys.values(right || move.left() == null ? move.hand() : move.left(), move, age);
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

    // The missile arm, aimed: the right hand held out almost straight from the shoulder at `target`, the back of the
    // hand up, kicked back along the arm by each salvo.
    @Nullable
    private static Aim aimArm(Blow blow, MechScript.Stage torso, @Nullable Vec3 target) {
        double age = blow.age();
        double weight = Ease.smooth(age / AIM_OPEN) * (1.0 - Ease.smooth((age - AIM_MOST) / AIM_CLOSE));
        if (weight <= 0.0) {
            return null;
        }
        Vec3 shoulder = torso.point(MechScript.SHOULDER);
        Vec3 to = target == null ? torso.ahead() : target.subtract(shoulder);
        Vec3 way = to.lengthSqr() < 1.0E-6 ? torso.ahead() : to.normalize();
        double kick = kick(blow);
        Vec3 hand = shoulder.add(way.scale(AIM_REACH - 1.1 * kick)).add(0.0, 0.5 * kick, 0.0);
        Vec3 down = new Vec3(0.0, -1.0, 0.0);
        Vec3 palm = down.subtract(way.scale(down.dot(way)));
        palm = palm.lengthSqr() < 1.0E-4 ? torso.ahead() : palm.normalize();
        return new Aim(hand, hand, palm, torso.dir(new Vec3(0.6, -1.0, -0.2)).normalize(), 1.0, 0.0, weight, 0.0);
    }

    // The spin's arms, in the spinning torso's frame: swung round out and low, each in turn raised high and hammered
    // onto the ground every SPIN_BEAT ticks, trailing a little behind the way it spins.
    @Nullable
    private static Aim spinAim(Blow blow, boolean right, MechScript.Stage frame) {
        double age = blow.age();
        double weight = Ease.smooth((age - 2.0) / (SPIN_FROM - 2.0))
                * (1.0 - Ease.smooth((age - SPIN_TO - 2.0) / 10.0));
        if (weight <= 0.0) {
            return null;
        }
        double u = beat(age, right);
        Vec3 at;
        if (u < 0.0) {
            at = SPIN_LOW;
        } else if (u < SPIN_RAISE) {
            at = SPIN_LOW.lerp(SPIN_HIGH, Ease.smooth(u / SPIN_RAISE));
        } else if (u < SPIN_SLAMMED) {
            double s = (u - SPIN_RAISE) / (SPIN_SLAMMED - SPIN_RAISE);
            at = SPIN_HIGH.lerp(SPIN_GROUND, s * s);
        } else {
            at = SPIN_GROUND.lerp(SPIN_LOW, Ease.smooth((u - SPIN_SLAMMED) / (1.0 - SPIN_SLAMMED)));
        }
        double side = right ? 1.0 : -1.0;
        Vec3 key = frame.point(new Vec3(side * at.x, at.y, side * at.z));
        Vec3 palm = frame.dir(new Vec3(-side * 0.6, -0.8, 0.0)).normalize();
        Vec3 pole = frame.dir(new Vec3(side, -0.2, -side * 0.3)).normalize();
        return new Aim(key, key, palm, pole, 1.0, 0.0, weight, 0.0);
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

    private static Vec3 localDir(MechScript.Stage stage, Vec3 world) {
        return new Vec3(world.dot(stage.right()), world.dot(stage.up()), world.dot(stage.ahead()));
    }
}
