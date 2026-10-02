package nl.tivek.multiversepowers.character.greenlantern.mech;

import java.util.Arrays;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Keyframes;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.rig.Ik;

// The mech's arms through its build, in MechScript's places: two loose forearms that fly in, reach for the target, clap
// it and rise, then set on the shoulders by their upper arms, the right one digging the head out and throwing it. Keys
// are the right arm's, x outwards; the left mirrors.
public final class MechMoves {
    private static final double CLAP_HIGH = 1.25;
    private static final double CLAP_OUT = 0.5;
    private static final Vec3 CLAP_WAY = new Vec3(0.0, 0.12, 1.0).normalize();
    private static final double SQUEEZE_LIFT = 0.3;
    private static final double SWING_PULL = 2.2;
    private static final double SPIN = Math.PI * 3.0;
    private static final int SPUN = MechScript.ARMS_FORM + 16;
    private static final double WALK_SWING = 0.3;
    private static final double WALK_BEND = 0.22;
    private static final Vec3 WALK_HANG = new Vec3(0.06, -0.94, 0.34).normalize();
    // Running, the forearm is held bent up ahead, in towards the middle, the fist clenched with its palm turned in; it
    // comes up further as the arm pumps forward.
    private static final Vec3 RUN_FORE = new Vec3(-0.22, -0.2, 0.95).normalize();
    private static final Vec3 RUN_PALM = new Vec3(-1.0, 0.0, 0.0);
    private static final double RUN_BEND = 0.18;
    private static final double RUN_CURL = 0.95;
    private static final double RUN_SPREAD = 0.2;
    // The right arm digging out and tossing the head: its elbow out and back, the palm aimed a few times over so the
    // head's middle lands where the keys want it.
    public static final int PICK_END = MechScript.TOSS + 12;
    private static final Vec3 PICK_POLE = new Vec3(1.0, -0.2, -0.5);
    private static final int PICK_AIMS = 4;
    private static final Keyframes.Key[] LOOSE;
    private static final Keyframes.Key[] SET;
    private static final Keyframes.Key[] SET_LEFT;
    private static final Keyframes.Key[] PICK;

    // A forearm with its hand: its elbow, which way it runs to the wrist, which way the palm faces, how far the fingers
    // curl (0 open, 1 a fist) and spread; upper is how far the upper arm has grown out to its elbow (0: none yet). The
    // hand turns at the wrist: folded towards its palm (+) and then tilted across it, in radians.
    public record Arm(Vec3 elbow, Vec3 way, Vec3 palm, double curl, double spread, double upper, double fold,
            double tilt) {
        public Arm(Vec3 elbow, Vec3 way, Vec3 palm, double curl, double spread, double upper) {
            this(elbow, way, palm, curl, spread, upper, 0.0, 0.0);
        }

        // The middle of the palm.
        public Vec3 hand() {
            if (this.fold == 0.0 && this.tilt == 0.0) {
                return this.elbow.add(this.way.scale(MechScript.PALM_ALONG));
            }
            // A stage's own places turn the other way round from the world's, so across the hand is way x palm here.
            Vec3 across = this.way.cross(this.palm).normalize();
            Vec3 folded = this.way.scale(Math.cos(this.fold)).add(this.palm.scale(Math.sin(this.fold)));
            Vec3 along = folded.scale(Math.cos(this.tilt)).subtract(across.scale(Math.sin(this.tilt)));
            return this.elbow.add(this.way.scale(MechScript.FOREARM))
                    .add(along.scale(MechScript.PALM_ALONG - MechScript.FOREARM));
        }
    }

    static {
        Vec3 clapHand = new Vec3(CLAP_OUT, CLAP_HIGH, MechScript.TARGET_AHEAD);
        Vec3 clapElbow = clapHand.subtract(CLAP_WAY.scale(MechScript.PALM_ALONG));
        Vec3 set = MechScript.SHOULDER.add(new Vec3(0.95, -0.2, 0.2).normalize().scale(MechScript.UPPER_ARM));
        Vec3 setWay = new Vec3(0.6, 0.12, 0.8);
        Vec3 setPalm = new Vec3(0.0, -0.6, 0.8);
        Vec3 inward = new Vec3(-1.0, 0.0, 0.0);
        // The wrists: hanging limp as the forearms spin in, cocked back while they reach and spread round the target,
        // snapped straight as the palms meet, flicked open as they let go and trailing as they rise.
        LOOSE = new Keyframes.Key[] {
                loose(MechScript.ARMS_FORM, true, new Vec3(6.2, 13.5, -1.2), new Vec3(0.15, 0.85, 0.5),
                        new Vec3(-0.3, -0.4, 0.85), 0.2, 0.6, 0.0, 0.35, 0.0),
                loose(MechScript.ARMS_IN, true, new Vec3(6.0, 12.8, -0.6), new Vec3(0.15, 0.85, 0.5),
                        new Vec3(-0.3, -0.4, 0.85), 0.2, 0.6, 0.0, 0.35, 0.0),
                loose(MechScript.ARMS_IN + 5, true, new Vec3(3.4, 5.4, 1.3), new Vec3(0.3, 0.85, 0.45),
                        new Vec3(-0.2, -0.4, 0.9), 0.1, 1.0, 0.0, -0.15, 0.1),
                loose(MechScript.SPREAD - 3, false, new Vec3(3.4, 3.0, 1.2), new Vec3(-0.1, -0.3, 0.95),
                        new Vec3(-0.9, 0.0, -0.1), 0.25, 0.7, 1.0, -0.35, 0.0),
                loose(MechScript.SPREAD, true, new Vec3(4.2, 1.5, 1.05), new Vec3(0.08, 0.12, 1.0), inward, 0.05, 1.0,
                        1.0, -0.45, 0.0),
                loose(MechScript.SWING, true, new Vec3(4.8, 1.6, 0.9), new Vec3(0.13, 0.14, 1.0), inward, 0.0, 1.0,
                        1.0, -0.6, 0.12),
                loose(MechScript.CLAP, true, clapElbow, CLAP_WAY, inward, 0.0, 0.2, 1.0, 0.0, 0.0),
                loose(MechScript.CLAP + 6, false, clapElbow.add(0.0, SQUEEZE_LIFT, 0.0), CLAP_WAY, inward, 0.3, 0.1,
                        1.0, 0.0, 0.0),
                loose(MechScript.RELEASE, true, clapElbow.add(0.0, SQUEEZE_LIFT, 0.0), CLAP_WAY, inward, 0.3, 0.1,
                        1.0, 0.0, 0.0),
                loose(MechScript.RELEASE + 4, false, new Vec3(4.3, 2.8, 0.9), new Vec3(0.25, 0.3, 0.92),
                        new Vec3(0.0, 0.1, 1.0), 0.05, 1.0, 0.6, -0.5, 0.0),
                loose(MechScript.RELEASE + 10, false, new Vec3(4.9, 5.6, 0.6), new Vec3(0.5, 0.75, 0.43),
                        new Vec3(0.0, -0.3, 0.95), 0.1, 1.0, 0.0, 0.45, 0.0),
                loose(MechScript.SIT, true, set, setWay, setPalm, 0.15, 0.8, 0.0, 0.1, 0.0),
                loose(MechScript.ELBOWS, true, set, setWay, setPalm, 0.15, 0.8, 0.0, 0.0, 0.0) };
        SET = sets(true, setWay, setPalm, inward);
        SET_LEFT = sets(false, setWay, setPalm, inward);
        // The right hand reaches up, claws down onto the head in its crater and closes on it, lifts it out and swings it
        // up beside it and back past the hip, low behind, then bowls it forward and up, letting go with the arm out
        // ahead (moving straight up as it does, a little back towards the neck it will land on), and follows through,
        // open. Places are the head's middle as the hand holds it, the palm facing it: the hand pushes it along its
        // swing.
        PICK = new Keyframes.Key[] {
                pick(MechScript.REACH, true, new Vec3(3.4, 8.6, 2.2), 0.0, new Vec3(-0.2, -0.5, 0.85), 0.3, 0.6, 0.0),
                pick(MechScript.REACH + 5, true, new Vec3(3.7, 10.6, 2.4), 0.0, new Vec3(-0.15, -0.45, 0.88), 0.15,
                        1.0, 1.0),
                pick(MechScript.GRAB - 3, false, new Vec3(1.0, 3.6, 4.1), 0.6, new Vec3(-0.15, -0.95, 0.25), 0.1, 1.0,
                        1.0),
                pick(MechScript.GRAB, true, MechHead.SUNK, 1.0, new Vec3(0.0, -1.0, 0.05), 0.25, 0.7, 1.0),
                pick(MechScript.GRAB + 2, false, new Vec3(0.4, 1.4, 3.6), 1.0, new Vec3(0.15, -0.98, 0.1), 0.8, 0.35,
                        1.0),
                pick(MechScript.GRAB + 5, false, new Vec3(4.6, 2.4, 3.4), 0.3, new Vec3(0.0, -0.2, 0.98), 0.85, 0.3,
                        1.0),
                // The swing: the arm straight, turning ever faster about the shoulder, the head 2 out from the palm.
                pick(MechScript.WIND, true, new Vec3(4.05, 1.94, -2.73), 0.0, new Vec3(0.05, -0.71, 0.71), 0.85, 0.3,
                        1.0),
                moving(pick(MechScript.WIND + 4, false, new Vec3(4.16, 2.26, 0.03), 0.0, new Vec3(0.0, -0.3, 0.95),
                        0.85, 0.3, 1.0), new Vec3(-0.05, 0.44, 1.31)),
                moving(pick(MechScript.WIND + 6, false, new Vec3(3.52, 4.04, 2.96), 0.0, new Vec3(0.0, 0.3, 0.95),
                        0.8, 0.35, 1.0), new Vec3(-0.15, 1.51, 1.43)),
                moving(pick(MechScript.TOSS, false, new Vec3(2.74, 8.48, 4.72), 0.0, new Vec3(-0.05, 0.91, 0.41), 0.6,
                        0.5, 1.0), new Vec3(-0.125, 2.6, -0.18)),
                pick(MechScript.TOSS + 3, true, new Vec3(3.08, 11.85, 3.11), 0.0, new Vec3(0.0, 0.94, -0.34), 0.05,
                        1.0, 1.0),
                pick(PICK_END, true, new Vec3(3.08, 11.85, 3.11), 0.0, new Vec3(0.0, 0.94, -0.34), 0.05, 1.0, 0.0) };
    }

    private MechMoves() {
    }

    // Once on the shoulders: held out wide as the elbows lock, flexed with the fists clenched, then lowered into a
    // wide ready guard, reaching up as the head forms high above and flinching at its crash. The right hand digs the
    // head out and tosses it (PICK) while the left one balances the body: out behind as the right reaches up, down
    // towards its knee as it stoops, swung across as the right winds back and back again as it throws. Both open
    // to the sky as the head falls and hunch as it lands; at the lock the right fist goes up (with the pilot's own) and
    // the left hand spreads wide like a claw, before both come down to hang.
    private static Keyframes.Key[] sets(boolean right, Vec3 setWay, Vec3 setPalm, Vec3 inward) {
        Vec3 guard = new Vec3(0.7, -0.63, 0.34);
        Vec3 guardWay = new Vec3(0.16, -0.32, 0.93);
        Vec3 guardPalm = new Vec3(-0.8, -0.15, 0.55);
        Vec3 open = new Vec3(0.85, 0.3, 0.42);
        Vec3 openWay = new Vec3(0.45, 0.72, 0.52);
        Vec3 openPalm = new Vec3(-0.45, 0.4, 0.8);
        return new Keyframes.Key[] {
                set(MechScript.ELBOWS, true, new Vec3(0.95, -0.2, 0.2), setWay, setPalm, 0.15, 0.8, 0.0, 0.0),
                set(MechScript.ELBOWS + 3, false, new Vec3(0.95, -0.26, 0.2), new Vec3(0.6, 0.04, 0.8), setPalm, 0.2,
                        0.75, 0.28, 0.0),
                set(MechScript.ELBOWS + 7, true, new Vec3(0.93, -0.22, 0.12), new Vec3(0.2, 0.88, 0.42),
                        new Vec3(-0.6, -0.1, 0.8), 0.95, 0.2, 0.25, 0.0),
                set(MechScript.ELBOWS + 12, false, new Vec3(0.84, -0.47, 0.28), new Vec3(0.42, 0.02, 0.9),
                        new Vec3(-0.35, -0.5, 0.8), 0.45, 0.6, 0.18, -0.05),
                set(MechScript.HEAD_FORM + 1, true, guard, guardWay, guardPalm, 0.42, 0.5, 0.22, -0.1),
                set(MechScript.HEAD_FORM + 8, true, new Vec3(0.8, -0.42, 0.42), new Vec3(0.3, 0.32, 0.9),
                        new Vec3(-0.45, 0.25, 0.85), 0.12, 1.0, -0.32, 0.0),
                set(MechScript.HEAD_DROP + 1, false, new Vec3(0.82, -0.36, 0.44), new Vec3(0.34, 0.42, 0.84),
                        new Vec3(-0.4, 0.3, 0.86), 0.1, 1.0, -0.4, 0.0),
                set(MechScript.CRASH, false, new Vec3(0.86, -0.24, 0.45), new Vec3(0.5, 0.42, 0.76),
                        new Vec3(-0.2, 0.2, 0.95), 0.3, 0.9, 0.4, 0.0),
                set(MechScript.CRASH + 4, false, new Vec3(0.72, -0.6, 0.35), new Vec3(0.2, -0.22, 0.95),
                        new Vec3(-0.75, -0.1, 0.65), 0.4, 0.6, 0.3, -0.05),
                right ? set(MechScript.REACH + 5, true, new Vec3(0.85, 0.35, 0.3), new Vec3(0.2, 0.85, 0.5),
                        new Vec3(-0.2, -0.35, 0.9), 0.15, 1.0, -0.3, 0.0)
                        : set(MechScript.REACH + 5, true, new Vec3(0.78, -0.42, -0.46), new Vec3(0.55, -0.45, -0.7),
                                new Vec3(0.3, -0.6, 0.75), 0.45, 0.6, 0.2, 0.0),
                right ? set(MechScript.GRAB, true, new Vec3(0.6, -0.6, 0.55), new Vec3(-0.2, -0.75, 0.6),
                        new Vec3(0.0, -0.6, -0.8), 0.25, 0.7, 0.0, 0.0)
                        : set(MechScript.GRAB, true, new Vec3(0.55, -0.78, 0.3), new Vec3(0.05, -0.85, 0.5),
                                new Vec3(-0.85, -0.1, 0.5), 0.55, 0.5, -0.25, 0.0),
                right ? set(MechScript.WIND, true, new Vec3(0.75, -0.55, -0.4), new Vec3(0.2, -0.8, -0.55),
                        new Vec3(0.3, -0.5, 0.8), 0.85, 0.3, 0.0, 0.0)
                        : set(MechScript.WIND, false, new Vec3(0.55, -0.45, 0.7), new Vec3(-0.2, 0.0, 1.0),
                                new Vec3(-0.9, 0.0, -0.3), 0.6, 0.4, 0.1, 0.0),
                right ? set(MechScript.TOSS, false, new Vec3(0.55, 0.8, 0.25), new Vec3(-0.15, 0.95, 0.25),
                        new Vec3(-0.2, -0.3, 0.95), 0.6, 0.5, 0.0, 0.0)
                        : set(MechScript.TOSS, false, new Vec3(0.75, -0.6, -0.3), new Vec3(0.45, -0.65, -0.6),
                                new Vec3(0.2, -0.3, 0.9), 0.5, 0.5, 0.2, 0.0),
                set(PICK_END, true, open, openWay, openPalm, 0.1, 1.0, -0.3, 0.0),
                set(MechScript.LOCK - 3, true, new Vec3(0.8, -0.4, 0.45), new Vec3(0.3, 0.42, 0.85),
                        new Vec3(-0.5, 0.1, 0.85), 0.55, 0.4, 0.1, 0.0),
                set(MechScript.LOCK, false, new Vec3(0.79, -0.46, 0.42), new Vec3(0.28, 0.3, 0.9),
                        new Vec3(-0.5, 0.05, 0.85), 0.7, 0.3, 0.32, 0.0),
                right ? set(MechScript.LOCK + 6, true, new Vec3(0.86, 0.28, 0.42), new Vec3(0.4, 0.8, 0.44),
                        new Vec3(-0.15, -0.25, 1.0), 0.95, 0.2, 0.05, 0.0)
                        : set(MechScript.LOCK + 6, true, new Vec3(0.9, 0.12, 0.42), new Vec3(0.8, 0.42, 0.42),
                                new Vec3(0.0, 0.2, 1.0), 0.45, 1.0, -0.45, 0.12),
                right ? set(MechScript.DONE, true, new Vec3(0.85, 0.3, 0.42), new Vec3(0.38, 0.82, 0.43),
                        new Vec3(-0.15, -0.25, 1.0), 0.95, 0.2, 0.05, 0.0)
                        : set(MechScript.DONE, true, new Vec3(0.9, 0.14, 0.41), new Vec3(0.8, 0.44, 0.41),
                                new Vec3(0.0, 0.2, 1.0), 0.45, 1.0, -0.45, 0.12),
                set(MechScript.DONE + 5, false, new Vec3(0.55, -0.8, 0.22), new Vec3(0.15, -0.5, 0.85),
                        new Vec3(-0.7, -0.2, 0.6), 0.5, 0.5, 0.2, 0.0),
                set(MechScript.SETTLED - 2, true, new Vec3(0.34, -0.93, 0.1), new Vec3(0.06, -0.78, 0.62), inward, 0.5,
                        0.4, 0.0, 0.0) };
    }

    private static Keyframes.Key pick(double t, boolean stop, Vec3 head, double onTarget, Vec3 palm, double curl,
            double spread, double weight) {
        Vec3 p = palm.normalize();
        return new Keyframes.Key((float) t, stop, new float[] { (float) head.x, (float) head.y, (float) head.z,
                (float) onTarget, (float) p.x, (float) p.y, (float) p.z, (float) curl, (float) spread, (float) weight });
    }

    // A PICK key passed through with the head moving this fast (blocks a tick).
    private static Keyframes.Key moving(Keyframes.Key key, Vec3 speed) {
        float[] said = new float[key.values().length];
        Arrays.fill(said, Float.NaN);
        said[0] = (float) speed.x;
        said[1] = (float) speed.y;
        said[2] = (float) speed.z;
        return new Keyframes.Key(key.tick(), key.stop(), key.values(), said);
    }

    private static Keyframes.Key loose(int t, boolean stop, Vec3 elbow, Vec3 way, Vec3 palm, double curl,
            double spread, double onTarget, double fold, double tilt) {
        Vec3 w = way.normalize();
        Vec3 p = square(palm, w);
        return new Keyframes.Key(t, stop, new float[] { (float) elbow.x, (float) elbow.y, (float) elbow.z, (float) w.x,
                (float) w.y, (float) w.z, (float) p.x, (float) p.y, (float) p.z, (float) curl, (float) spread,
                (float) onTarget, (float) fold, (float) tilt });
    }

    private static Keyframes.Key set(int t, boolean stop, Vec3 upper, Vec3 way, Vec3 palm, double curl,
            double spread, double fold, double tilt) {
        Vec3 u = upper.normalize();
        Vec3 w = way.normalize();
        Vec3 p = square(palm, w);
        return new Keyframes.Key(t, stop, new float[] { (float) u.x, (float) u.y, (float) u.z, (float) w.x,
                (float) w.y, (float) w.z, (float) p.x, (float) p.y, (float) p.z, (float) curl, (float) spread,
                (float) fold, (float) tilt });
    }

    // `stage`: the spot the mech is built on. The arm comes back in the places of the frame it moves in (MechBuild.arms:
    // the torso's once its upper arm has it).
    public static Arm arm(boolean right, MechScript.Stage stage, double t) {
        Arm arm = t < MechScript.ELBOWS ? loose(stage, t) : set(right, t);
        if (!right) {
            return mirror(arm);
        }
        return t > MechScript.REACH && t < PICK_END ? pick(stage, arm, t) : arm;
    }

    // Mirrored, a hand folds the same way towards its palm but tilts across it the other way.
    private static Arm mirror(Arm arm) {
        return new Arm(MechScript.mirror(arm.elbow()), MechScript.mirror(arm.way()), MechScript.mirror(arm.palm()),
                arm.curl(), arm.spread(), arm.upper(), arm.fold(), -arm.tilt());
    }

    // The right arm on the head (PICK), taken over from the arm its own keys give it (`was`) as far as the keys say.
    private static Arm pick(MechScript.Stage stage, Arm was, double t) {
        float[] v = Keyframes.at(PICK, (float) t);
        double weight = Mth.clamp(v[9], 0.0, 1.0);
        if (weight <= 0.0) {
            return was;
        }
        MechScript.Stage torso = MechBuild.torso(stage, t);
        Vec3 head = torso.local(stage.point(v[0], v[1] + v[3] * stage.targetY(), v[2]));
        Vec3 palm = within(torso, stage.dir(new Vec3(v[4], v[5], v[6])));
        Arm arm = was;
        // The palm turns square to the forearm: aimed again from where it faces, until the head lies against it.
        for (int i = 0; i < PICK_AIMS; i++) {
            arm = reach(head.subtract(palm.scale(MechHead.HOLD)), palm, was, v[7], v[8]);
            palm = arm.palm();
        }
        return weight >= 1.0 ? arm : blend(was, arm, weight);
    }

    private static Arm reach(Vec3 target, Vec3 palm, Arm was, double curl, double spread) {
        double[] out = new double[3];
        Ik.twoBone(new double[] { MechScript.SHOULDER.x, MechScript.SHOULDER.y, MechScript.SHOULDER.z },
                new double[] { target.x, target.y, target.z }, new double[] { PICK_POLE.x, PICK_POLE.y, PICK_POLE.z },
                MechScript.UPPER_ARM, MechScript.PALM_ALONG, out);
        Vec3 elbow = new Vec3(out[0], out[1], out[2]);
        Vec3 way = target.subtract(elbow);
        way = way.lengthSqr() < 1.0E-8 ? was.way() : way.normalize();
        return new Arm(elbow, way, square(palm, way), curl, spread, 1.0);
    }

    private static Arm blend(Arm from, Arm to, double u) {
        Vec3 elbow = from.elbow().lerp(to.elbow(), u).subtract(MechScript.SHOULDER);
        elbow = MechScript.SHOULDER.add(elbow.lengthSqr() < 1.0E-8 ? to.elbow().subtract(MechScript.SHOULDER)
                : elbow.normalize().scale(MechScript.UPPER_ARM));
        Vec3 way = from.way().lerp(to.way(), u);
        way = way.lengthSqr() < 1.0E-8 ? to.way() : way.normalize();
        return new Arm(elbow, way, square(from.palm().lerp(to.palm(), u), way), Mth.lerp(u, from.curl(), to.curl()),
                Mth.lerp(u, from.spread(), to.spread()), 1.0, Mth.lerp(u, from.fold(), to.fold()),
                Mth.lerp(u, from.tilt(), to.tilt()));
    }

    // A way in the world in `frame`'s own places.
    private static Vec3 within(MechScript.Stage frame, Vec3 way) {
        return new Vec3(way.dot(frame.right()), way.dot(frame.up()), way.dot(frame.ahead()));
    }

    private static Arm loose(MechScript.Stage stage, double t) {
        float[] v = Keyframes.at(LOOSE, (float) t);
        if (t > MechScript.SWING && t < MechScript.CLAP) {
            // The swing gathers speed right into the clap instead of easing off before it.
            float[] from = Keyframes.at(LOOSE, MechScript.SWING);
            float[] to = Keyframes.at(LOOSE, MechScript.CLAP);
            float u = (float) Math.pow((t - MechScript.SWING) / (MechScript.CLAP - MechScript.SWING), SWING_PULL);
            for (int i = 0; i < v.length; i++) {
                v[i] = Mth.lerp(u, from[i], to[i]);
            }
        }
        Vec3 elbow = new Vec3(v[0], v[1] + v[11] * stage.targetY(), v[2]);
        if (t > MechScript.CLAP && t < MechScript.RELEASE) {
            double shake = Math.sin((t - MechScript.CLAP) * 2.3)
                    * (1.0 - (t - MechScript.CLAP) / (MechScript.RELEASE - MechScript.CLAP + 3.0));
            elbow = elbow.add(0.05 * shake, 0.0, 0.0);
        }
        Vec3 way = new Vec3(v[3], v[4], v[5]).normalize();
        Vec3 palm = square(new Vec3(v[6], v[7], v[8]), way);
        double left = 1.0 - Mth.clamp((t - MechScript.ARMS_FORM) / (SPUN - MechScript.ARMS_FORM), 0.0, 1.0);
        if (left > 0.0) {
            // It spins about its length as it flies in, slowing down until it hangs ready by the target.
            palm = Vectors.spin(palm, way, SPIN * left * left);
        }
        return new Arm(elbow, way, palm, v[9], v[10], upper(t), v[12], v[13]);
    }

    private static Arm set(boolean right, double t) {
        float[] v = Keyframes.at(right ? SET : SET_LEFT, (float) t);
        Vec3 upper = new Vec3(v[0], v[1], v[2]);
        if (t > MechScript.SETTLED) {
            upper = upper.add(0.0, 0.015 * Math.sin(t * 0.09), 0.0);
        } else if (t > MechScript.LOCK + 2 && t < MechScript.DONE + 2) {
            upper = upper.add(0.0, 0.02 * Math.sin(t * 1.9), 0.0);
        }
        upper = upper.normalize();
        Vec3 way = new Vec3(v[3], v[4], v[5]).normalize();
        Vec3 elbow = MechScript.SHOULDER.add(upper.scale(MechScript.UPPER_ARM));
        return new Arm(elbow, way, square(new Vec3(v[6], v[7], v[8]), way), v[9], v[10], 1.0, v[11], v[12]);
    }

    // A walking mech's arm, swinging from the shoulder against its legs: swing 1 has the right arm forward and the
    // left one back. The further it walks (walking 0 to 1) the lower its forearm hangs; it bends up a little as it
    // swings forward.
    public static Arm walking(boolean right, double t, double swing, double walking) {
        return walking(right, t, swing, walking, 0.0);
    }

    // As above, running (0 to 1) the arm pumping bent with its fist clenched.
    public static Arm walking(boolean right, double t, double swing, double walking, double running) {
        Arm set = set(right, Math.max(t, MechScript.SETTLED));
        double run = Mth.clamp(running, 0.0, 1.0);
        double angle = WALK_SWING * (right ? swing : -swing);
        Vec3 across = new Vec3(1.0, 0.0, 0.0);
        Vec3 elbow = MechScript.SHOULDER.add(Vectors.spin(set.elbow().subtract(MechScript.SHOULDER), across, -angle));
        double ahead = Math.max(0.0, angle) / WALK_SWING;
        double bend = angle + Mth.lerp(run, WALK_BEND * Math.min(1.0, ahead), RUN_BEND * ahead);
        Vec3 hang = set.way().lerp(WALK_HANG, Mth.clamp(walking, 0.0, 1.0)).lerp(RUN_FORE, run).normalize();
        Vec3 palm = square(set.palm().lerp(RUN_PALM, run), hang);
        Arm arm = new Arm(elbow, Vectors.spin(hang, across, -bend), Vectors.spin(palm, across, -bend),
                Mth.lerp(run, set.curl(), RUN_CURL), Mth.lerp(run, set.spread(), RUN_SPREAD), 1.0);
        if (right) {
            return arm;
        }
        return new Arm(MechScript.mirror(arm.elbow()), MechScript.mirror(arm.way()), MechScript.mirror(arm.palm()),
                arm.curl(), arm.spread(), arm.upper());
    }

    public static double upper(double t) {
        return Mth.clamp((t - MechScript.UPPER_ARMS) / (MechScript.ELBOWS - MechScript.UPPER_ARMS), 0.0, 1.0);
    }

    static Vec3 square(Vec3 palm, Vec3 way) {
        Vec3 flat = palm.subtract(way.scale(palm.dot(way)));
        if (flat.lengthSqr() < 1.0E-8) {
            Vec3 side = Math.abs(way.y) < 0.9 ? way.cross(new Vec3(0.0, 1.0, 0.0)) : way.cross(new Vec3(1.0, 0.0, 0.0));
            return side.normalize();
        }
        return flat.normalize();
    }
}
