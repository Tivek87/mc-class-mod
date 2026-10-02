package nl.tivek.multiversepowers.character.greenlantern.mech;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The head through the build: made high above the target, dropped onto it and left in its crater, dug out by the right
// hand (MechMoves) and held fast in it as it is swung back and tossed high, tumbling head over heels from the way it
// left the hand down onto the neck, where it locks on and rides the torso. One path for the server and every client.
public final class MechHead {
    // From the middle of the palm to the middle of the head it holds.
    public static final double HOLD = 2.0;
    // Half the head's size round its middle, for the fingers closing on it.
    public static final Vec3 HALF = new Vec3(1.05, 1.15, 1.05);
    private static final double SINK = 0.5;
    // Where it lies in its crater on a target standing level with the mech.
    public static final Vec3 SUNK = new Vec3(0.0, MechScript.HEAD_UP - SINK, MechScript.TARGET_AHEAD);
    private static final double HIGH = 13.5;
    private static final double FORM_SPIN = Math.PI * 3.0;
    // How it lies in its crater, tipped (as turned() takes it).
    private static final double LIE_PITCH = 0.15;
    private static final double LIE_ROLL = 0.3;
    // The tumble it is thrown into: about a whole turn about the upright and two head over heels, slowing until it
    // lands upright.
    private static final double SPIN = -Math.PI * 2.0;
    private static final double FLIP = -Math.PI * 4.0;
    private static final double SLOWING = 2.2;
    private static final double LEAST_FALL = 0.04;
    private static final double LAUNCH_STEP = 0.5;
    private static final double TAKE_TICKS = 2.0;

    // Where the head's middle is and which ways its face and its top point, in the world.
    public record Pose(Vec3 at, Vec3 ahead, Vec3 up) {
    }

    private MechHead() {
    }

    public static Pose pose(MechScript.Stage stage, double t) {
        if (t >= MechScript.LOCK) {
            // On its neck it throws itself back and looks round (MechBuild).
            MechScript.Stage torso = MechBuild.torso(stage, t);
            MechBuild.Body body = MechBuild.body(t);
            double on = Ease.smooth((t - MechScript.LOCK) / 3.0);
            double recoil = 0.08 * Ease.recoil(t - MechScript.LOCK, 1.0, 1.1, 0.35);
            return turned(torso.point(MechScript.NECK.add(0.0, MechScript.HEAD_UP - recoil, 0.0)), torso,
                    body.lookYaw() * on, body.lookPitch() * on, 0.0);
        }
        if (t >= MechScript.TOSS) {
            return flight(stage, t);
        }
        if (t >= MechScript.GRAB) {
            return carried(stage, t, stage.point(sunk(stage)).lerp(held(stage, t),
                    Ease.smooth((t - MechScript.GRAB) / TAKE_TICKS)));
        }
        Vec3 target = stage.target();
        if (t >= MechScript.CRASH) {
            double settle = Ease.smooth((t - MechScript.CRASH) / 3.0);
            return turned(stage.point(sunk(stage).add(0.0, SINK * (1.0 - settle), 0.0)), stage, 0.0,
                    Mth.lerp(settle, 0.25, LIE_PITCH), LIE_ROLL * settle);
        }
        Vec3 high = target.add(0.0, HIGH, 0.0);
        if (t >= MechScript.HEAD_DROP) {
            double u = (t - MechScript.HEAD_DROP) / (MechScript.CRASH - MechScript.HEAD_DROP);
            return turned(stage.point(high.lerp(sunk(stage).add(0.0, SINK, 0.0), u * u)), stage, 0.0,
                    0.25 * Ease.smooth(u), 0.0);
        }
        // It spins fast as it forms and slows to face down at the target.
        double left = 1.0 - Mth.clamp((t - MechScript.HEAD_FORM) / (MechScript.HEAD_DROP - MechScript.HEAD_FORM), 0.0,
                1.0);
        return turned(stage.point(high.add(0.0, 0.25 * Math.sin((t - MechScript.HEAD_FORM) * 0.25), 0.0)), stage,
                FORM_SPIN * left * left, 0.0, 0.0);
    }

    // Held fast in the right hand: turned with it from the way it lay in the crater as the fingers closed on it.
    private static Pose carried(MechScript.Stage stage, double t, Vec3 at) {
        Pose lying = turned(at, stage, 0.0, LIE_PITCH, LIE_ROLL);
        Vec3[] from = hand(stage, MechScript.GRAB);
        Vec3[] now = hand(stage, t);
        return new Pose(at, carry(lying.ahead(), from, now), carry(lying.up(), from, now));
    }

    // The right hand's axes in the world: along its fingers, out of its palm and across it.
    private static Vec3[] hand(MechScript.Stage stage, double t) {
        MechScript.Stage torso = MechBuild.torso(stage, t);
        MechMoves.Arm arm = MechMoves.arm(true, stage, t);
        Vec3 way = torso.dir(arm.way());
        Vec3 palm = torso.dir(arm.palm());
        return new Vec3[] { way, palm, way.cross(palm) };
    }

    // A way held in the hand with the axes `from`, as the hand has turned to `now`.
    private static Vec3 carry(Vec3 way, Vec3[] from, Vec3[] now) {
        return now[0].scale(way.dot(from[0])).add(now[1].scale(way.dot(from[1]))).add(now[2].scale(way.dot(from[2])));
    }

    // `frame`'s axes turned as the painter turns a frame (ConstructPainter.Frame.turned, its axes about its upright,
    // its own right and its own face): by yaw (face to its right +), then pitch (face down +), then roll.
    public static Pose turned(Vec3 at, MechScript.Stage frame, double yaw, double pitch, double roll) {
        Vec3 right = frame.right();
        Vec3 up = frame.up();
        Vec3 ahead = frame.ahead();
        if (yaw != 0.0) {
            right = Vectors.spin(right, up, -yaw);
            ahead = Vectors.spin(ahead, up, -yaw);
        }
        if (pitch != 0.0) {
            up = Vectors.spin(up, right, -pitch);
            ahead = Vectors.spin(ahead, right, -pitch);
        }
        if (roll != 0.0) {
            up = Vectors.spin(up, ahead, -roll);
        }
        return new Pose(at, ahead, up);
    }

    // The yaw, pitch and roll that turn `frame`'s axes to these (as turned() takes them).
    static double[] angles(MechScript.Stage frame, Vec3 ahead, Vec3 up) {
        double yaw = Math.atan2(ahead.dot(frame.right()), ahead.dot(frame.ahead()));
        Vec3 heading = frame.ahead().scale(Math.cos(yaw)).add(frame.right().scale(Math.sin(yaw)));
        double pitch = Math.atan2(-ahead.dot(frame.up()), ahead.dot(heading));
        Vec3 right = frame.right().scale(Math.cos(yaw)).subtract(frame.ahead().scale(Math.sin(yaw)));
        Vec3 tipped = frame.up().scale(Math.cos(pitch)).add(heading.scale(Math.sin(pitch)));
        return new double[] { yaw, pitch, Math.atan2(-up.dot(right), up.dot(tipped)) };
    }

    // Where the head lies in its crater, in the stage's places.
    public static Vec3 sunk(MechScript.Stage stage) {
        return SUNK.add(0.0, stage.targetY(), 0.0);
    }

    // The middle of the head as the right hand holds it, in the world: against the inside of its palm.
    public static Vec3 held(MechScript.Stage stage, double t) {
        MechScript.Stage torso = MechBuild.torso(stage, t);
        MechMoves.Arm arm = MechMoves.arm(true, stage, t);
        return torso.point(arm.hand()).add(torso.dir(arm.palm()).scale(HOLD));
    }

    // Let go at the toss, it flies as anything thrown does and lands square on the neck as the torso has it at the
    // lock: as fast up as the hand flung it, straight across to the neck, tumbling ever slower until it sits upright.
    private static Pose flight(MechScript.Stage stage, double t) {
        double ticks = MechScript.LOCK - MechScript.TOSS;
        double since = t - MechScript.TOSS;
        double s = since / ticks;
        Vec3 from = held(stage, MechScript.TOSS);
        Vec3 to = MechBuild.torso(stage, MechScript.LOCK).point(MechScript.NECK.add(0.0, MechScript.HEAD_UP, 0.0));
        double up = (from.y - held(stage, MechScript.TOSS - LAUNCH_STEP).y) / LAUNCH_STEP;
        double fall = 2.0 * (from.y + up * ticks - to.y) / (ticks * ticks);
        if (fall < LEAST_FALL) {
            fall = LEAST_FALL;
            up = (to.y - from.y + 0.5 * fall * ticks * ticks) / ticks;
        }
        Vec3 across = from.lerp(to, s);
        Vec3 at = new Vec3(across.x, from.y + up * since - 0.5 * fall * since * since, across.z);
        Pose left = carried(stage, MechScript.TOSS, from);
        double[] was = angles(stage, left.ahead(), left.up());
        double turned = 1.0 - Math.pow(1.0 - s, SLOWING);
        return turned(at, stage, Mth.lerp(turned, was[0], whole(was[0] + SPIN)),
                Mth.lerp(turned, was[1], whole(was[1] + FLIP)), Mth.lerp(turned, was[2], whole(was[2])));
    }

    // The whole number of turns nearest to an angle.
    private static double whole(double angle) {
        return Math.PI * 2.0 * Math.round(angle / (Math.PI * 2.0));
    }
}
