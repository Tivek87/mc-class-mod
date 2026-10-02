package nl.tivek.multiversepowers.character.greenlantern.client.mech.touch;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.rig.Ik;
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes.WRIST;

// The mech's arm on its bones: the upper arm from the shoulder to the elbow, the forearm on to the wrist and the hand
// turned there, folded towards its palm and tilted across it, never further than a wrist goes. An arm laid with its
// hand on a hold finds its elbow from the shoulder, swung round out of the blocks, and turns its wrist so the hand
// lies as the hold asks.
public final class MechArmRig {
    // How far a wrist folds either way and tilts across (radians).
    public static final double MOST_FOLD = 1.45;
    public static final double MOST_TILT = 0.7;
    // How far an elbow swings out round the line from its shoulder to its wrist, and in, tried in so many steps.
    private static final double SWING_OUT = 1.3;
    private static final double SWING_IN = 0.45;
    private static final int SWING_STEPS = 9;
    // Points along the upper arm and the forearm (a share of each from its root) and how thick the arm is round them.
    private static final double[][] UPPER = { { 0.45, 0.6 }, { 0.8, 0.62 } };
    private static final double[][] FORE = { { 0.0, 0.62 }, { 0.15, 0.78 }, { 0.52, 0.82 }, { 0.89, 0.6 } };

    private MechArmRig() {
    }

    public static Frame forearm(MechScript.Stage torso, MechMoves.Arm arm) {
        return Frame.of(torso.point(arm.elbow()), torso.dir(arm.palm()), torso.dir(arm.way()), 1.0);
    }

    // The hand on its forearm, turned at the wrist: folded towards the palm, then tilted across it.
    public static Frame wrist(Frame forearm, double fold, double tilt) {
        Frame hand = fold == 0.0 ? forearm : forearm.turned(0.0, WRIST, 0.0, 1.0, 0.0, 0.0, fold);
        return tilt == 0.0 ? hand : hand.turned(0.0, WRIST, 0.0, 0.0, 0.0, 1.0, tilt);
    }

    // An arm's hand in the world, turned at its wrist as the arm has it and by `fold` and `tilt` more.
    public static Frame hand(MechScript.Stage torso, MechMoves.Arm arm, double fold, double tilt) {
        return wrist(forearm(torso, arm), arm.fold() + fold, arm.tilt() + tilt);
    }

    // The forearm and hand of an arm whose shoulder has moved from `was` to `shoulder` (world): the elbow found again
    // from where it is now, the upper arm as long as before, the wrist and the hand kept where they were and the wrist
    // turned to fit, so the arm is never stretched or squashed by its shoulder moving. A wrist the arm no longer
    // reaches is drawn in with it, the hand turned as before.
    public static Frame[] reattached(MechScript.Stage torso, MechMoves.Arm arm, Frame hand, Vec3 shoulder, Vec3 was) {
        Frame forearm = forearm(torso, arm);
        Vec3 elbow = forearm.center();
        Vec3 wrist = hand.at(0.0, WRIST, 0.0);
        double upper = elbow.distanceTo(was);
        double[] bent = new double[3];
        Ik.twoBone(new double[] { shoulder.x, shoulder.y, shoulder.z }, new double[] { wrist.x, wrist.y, wrist.z },
                new double[] { elbow.x - shoulder.x, elbow.y - shoulder.y, elbow.z - shoulder.z }, upper, WRIST, bent);
        Vec3 at = new Vec3(bent[0], bent[1], bent[2]);
        Vec3 way = wrist.subtract(at);
        if (way.lengthSqr() < 1.0E-8) {
            return new Frame[] { forearm, hand };
        }
        way = way.normalize();
        Frame moved = Frame.of(at, unturned(hand, way), way, 1.0);
        if (Math.abs(wrist.distanceTo(at) - WRIST) < 1.0E-9) {
            return new Frame[] { moved, hand };
        }
        double[] turn = wristTurn(hand, way);
        return new Frame[] { moved, wrist(moved, turn[0], turn[1]) };
    }

    // Which way a forearm running `way` faces its palm so that, turned at the wrist (wristTurn), its hand is `hand`.
    public static Vec3 unturned(Frame hand, Vec3 way) {
        double[] turn = wristTurn(hand, way);
        Vec3 x = hand.right().normalize();
        Vec3 y = hand.up().normalize();
        Vec3 z = hand.forward().normalize();
        Vec3 tilted = x.scale(Math.sin(turn[1])).add(y.scale(Math.cos(turn[1])));
        return tilted.scale(Math.sin(turn[0])).add(z.scale(Math.cos(turn[0])));
    }

    // How far `hand` is folded and tilted at the wrist (as wrist() turns it) on a forearm running `way`.
    public static double[] wristTurn(Frame hand, Vec3 way) {
        double fold = -Math.asin(Mth.clamp(way.dot(hand.forward().normalize()), -1.0, 1.0));
        double tilt = Math.atan2(way.dot(hand.right().normalize()), way.dot(hand.up().normalize()));
        return new double[] { fold, tilt };
    }

    // An arm from `shoulder` (the torso's own places, `right` or left) with the middle of its palm at `palm`, its
    // fingers running `along` and its palm facing `facing` (all in the world): the elbow bent towards `pole` (the
    // torso's own places) and swung round out of `ground` (null: no blocks), the wrist turned so the hand lies so, as
    // far as a wrist goes. A hold out of reach leaves the hand short of it, the arm straight towards it.
    public static MechMoves.Arm laid(MechScript.Stage torso, boolean right, Vec3 shoulder, Vec3 pole, Vec3 palm,
            Vec3 along, Vec3 facing, double curl, double spread, @Nullable MechHandRig.Ground ground) {
        Vec3 from = torso.point(shoulder);
        Vec3 wrist = palm.subtract(along.scale(MechScript.PALM_ALONG - WRIST));
        Vec3 bend = torso.dir(pole);
        double[] root = { from.x, from.y, from.z };
        double[] end = { wrist.x, wrist.y, wrist.z };
        double[] towards = { bend.x, bend.y, bend.z };
        double[] elbow = new double[3];
        if (ground == null) {
            Ik.twoBone(root, end, towards, MechScript.UPPER_ARM, WRIST, elbow);
        } else {
            // Which way a turn of the swivel takes the elbow: away from the body for a positive one.
            Vec3 axis = wrist.subtract(from);
            Vec3 out = torso.dir(MechPainter.side(new Vec3(1.0, 0.0, 0.0), right));
            double sign = Math.signum(axis.cross(bend).dot(out));
            sign = sign == 0.0 ? 1.0 : sign;
            double low = sign > 0.0 ? -SWING_IN : -SWING_OUT;
            double high = sign > 0.0 ? SWING_OUT : SWING_IN;
            Ik.swivel(root, end, towards, MechScript.UPPER_ARM, WRIST, low, high, SWING_STEPS,
                    (x, y, z) -> depth(root, end, x, y, z, ground), MechTouch.TOUCH, elbow);
        }
        Vec3 at = new Vec3(elbow[0], elbow[1], elbow[2]);
        Vec3 way = wrist.subtract(at);
        way = way.lengthSqr() < 1.0E-8 ? along : way.normalize();
        Frame hand = Frame.of(wrist.subtract(along.scale(WRIST)), facing, along, 1.0);
        double[] turn = wristTurn(hand, way);
        Vec3 palmWay = unturned(hand, way);
        return new MechMoves.Arm(torso.local(at), local(torso, way), local(torso, palmWay), curl, spread, 1.0,
                Mth.clamp(turn[0], -MOST_FOLD, MOST_FOLD), Mth.clamp(turn[1], -MOST_TILT, MOST_TILT));
    }

    // A way in the world in the torso's own places.
    public static Vec3 local(MechScript.Stage torso, Vec3 way) {
        return new Vec3(way.dot(torso.right()), way.dot(torso.up()), way.dot(torso.ahead()));
    }

    // How deep the upper arm (shoulder to elbow) and the forearm (elbow to wrist) go into the ground, the hand left out.
    private static double depth(double[] shoulder, double[] wrist, double x, double y, double z,
            MechHandRig.Ground ground) {
        double deepest = Double.NEGATIVE_INFINITY;
        for (double[] p : UPPER) {
            deepest = Math.max(deepest, ground.depth(shoulder[0] + (x - shoulder[0]) * p[0],
                    shoulder[1] + (y - shoulder[1]) * p[0], shoulder[2] + (z - shoulder[2]) * p[0], p[1]));
        }
        for (double[] p : FORE) {
            deepest = Math.max(deepest, ground.depth(x + (wrist[0] - x) * p[0], y + (wrist[1] - y) * p[0],
                    z + (wrist[2] - z) * p[0], p[1]));
        }
        return deepest;
    }
}
