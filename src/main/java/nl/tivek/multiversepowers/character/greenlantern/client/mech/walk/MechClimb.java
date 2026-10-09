package nl.tivek.multiversepowers.character.greenlantern.client.mech.walk;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechArmRig;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechHandRig;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// A mech hauling itself up onto a ledge too high to step onto, out of a pit, a trench or a cave: it reaches up and
// slams both hands onto the wall, goes up its face hand over hand while the top is out of reach, hooks its fingers over
// the edge and hauls itself up with its feet stepping up the face, turns its hands over to press down on the top as its
// shoulders come over it, swings its right foot up onto the ledge, heaves itself over and brings the left one up
// after. Hanging from its hands it keeps its chest off the wall. The pilot's game starts it (MechDrive) and moves the
// mech along its path; every game poses it from how far it has got, the ledge's height and how far off its face was,
// which travel packed in one number, and the holds each game finds in the same blocks (MechClimbHolds).
final class MechClimb {
    static final double HIGHEST = 30.0;
    static final double FARTHEST = 3.4;
    // Its base ends this far past the ledge's edge, both feet on the top.
    static final double PAST = 1.6;
    static final int GRAB = 9;
    // Up to this height the arms push it up as its foot steps onto the ledge; above it they haul it up first.
    private static final double PULL_FROM = 4.4;
    private static final double PULL_PER_BLOCK = 5.0;
    static final int HEAVE = 22;
    // How far through the heave the right foot is up on the top and the body starts over it.
    static final double OVER = 0.3;
    // The steps the packed height and edge come in, per block.
    private static final double HEIGHT_STEPS = 4.0;
    private static final double EDGE_STEPS = 8.0;
    // How far the front of the chest stands ahead of the spine at its middle (CHEST_ROUND less at its sides); hanging
    // from its hands, the mech keeps it CHEST_ROOM off the wall, pushed off as its feet leave the ground over LIFT_OFF.
    static final double CHEST = 2.35;
    static final double CHEST_ROUND = 0.85;
    private static final double CHEST_ROOM = 0.35;
    static final double LIFT_OFF = 1.2;
    // The chest's height on the body and where the body leans over the hips; leaning, its front keeps CHEST_GAP off
    // the wall.
    static final double CHEST_BOTTOM = 8.0;
    static final double CHEST_TOP = 11.45;
    static final double LEAN_FROM = MechScript.HIP.y;
    private static final double CHEST_GAP = 0.1;
    // While its hands press down on the top, the body leans MANTLE_LEAN over it and sinks as far as its arms then need
    // to reach it with the wrists no further than MANTLE_REACH from the shoulders, but no further than MOST_SINK.
    private static final double MANTLE_LEAN = 0.5;
    private static final double MANTLE_REACH = 4.65;
    private static final double MOST_SINK = 1.7;
    // A hand pressed on the top lies with its palm this high over it and its wrist this far in from the edge; hooked
    // over the edge or held to the face, its palm stands this far off the face.
    static final double GRIP_UP = 0.36;
    private static final double TOP_IN = 0.05;
    private static final double FACE_OFF = 0.38;
    // Its hands hook over the edge until its shoulders come up near the top, and are turned over to press down on it
    // once they are this far over it.
    private static final double TURN_FROM = -0.6;
    private static final double TURN_OVER = 1.6;
    private static final double HELD_CURL = 0.7;
    private static final double OPEN_CURL = 0.15;
    private static final double SPREAD = 0.25;
    private static final Vec3 ELBOWS = new Vec3(0.8, -0.25, -0.55);
    // Its fingers settle onto what they hold in this many tries.
    private static final int RESTS = 3;

    private MechClimb() {
    }

    static int pack(int age, double height, double edge) {
        int h = Mth.clamp((int) Math.round(height * HEIGHT_STEPS), 0, 127);
        int e = Mth.clamp((int) Math.round(edge * EDGE_STEPS), 0, 63);
        return 1 | Mth.clamp(age, 0, 255) << 1 | h << 9 | e << 16;
    }

    static int age(int packed) {
        return packed >>> 1 & 255;
    }

    static double height(int packed) {
        return (packed >>> 9 & 127) / HEIGHT_STEPS;
    }

    static double edge(int packed) {
        return (packed >>> 16 & 63) / EDGE_STEPS;
    }

    // A height or a distance off as the packed number carries it.
    static double quantizedHeight(double value) {
        return Math.round(value * HEIGHT_STEPS) / HEIGHT_STEPS;
    }

    static double quantizedEdge(double value) {
        return Math.round(value * EDGE_STEPS) / EDGE_STEPS;
    }

    static int pull(double height) {
        return (int) Math.round(PULL_PER_BLOCK * lift(height));
    }

    // How far it hauls itself up hanging from its hands before it heaves itself over.
    static double lift(double height) {
        return Math.max(0.0, height - PULL_FROM);
    }

    static int ticks(double height) {
        return GRAB + pull(height) + HEAVE;
    }

    // How far it pushes itself back off the wall as it hangs, so its chest stays off it.
    static double standoff(double edge) {
        return Math.max(0.0, CHEST + CHEST_ROOM - edge);
    }

    // How far its base has risen and gone ahead of where it started (in its own places), `age` ticks in: still while
    // the hands reach up, hauled up the wall until its hips are over the top, pushed back off it as its feet leave the
    // ground, then, once its right foot is up there, heaved up and over onto it.
    static Vec3 path(double age, double height, double edge) {
        int pull = pull(height);
        double lift = lift(height);
        if (age <= GRAB) {
            return Vec3.ZERO;
        }
        double back = standoff(edge);
        double y = rise(age, height);
        if (age <= GRAB + pull) {
            return new Vec3(0.0, y, -back * Ease.smooth(y / LIFT_OFF));
        }
        double v = Mth.clamp((age - GRAB - pull) / HEAVE, 0.0, 1.0);
        double hung = -back * Ease.smooth(lift / LIFT_OFF);
        return new Vec3(0.0, y, hung + (edge + PAST - hung) * Ease.smooth((v - OVER) / (1.0 - OVER)));
    }

    // How far its base has risen `age` ticks in (path's height).
    static double rise(double age, double height) {
        int pull = pull(height);
        double lift = lift(height);
        if (age <= GRAB) {
            return 0.0;
        }
        if (age <= GRAB + pull) {
            return lift * Ease.smooth((age - GRAB) / pull);
        }
        double v = Mth.clamp((age - GRAB - pull) / HEAVE, 0.0, 1.0);
        return lift + (height - lift) * Ease.smooth((v - OVER) / 0.55);
    }

    // How many ticks into the climb its body has risen `rise` up the wall (no more than it hauls itself up).
    static double ageAt(double rise, double height) {
        int pull = pull(height);
        double share = Mth.clamp(rise / Math.max(1.0E-6, lift(height)), 0.0, 1.0);
        double low = 0.0;
        double high = 1.0;
        for (int i = 0; i < 24; i++) {
            double middle = (low + high) * 0.5;
            if (Ease.smooth(middle) < share) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return GRAB + high * pull;
    }

    // Where the mech's base set off from, `age` ticks into a climb that has it at `now`.
    static MechScript.Stage start(MechScript.Stage now, double age, double height, double edge) {
        Vec3 path = path(age, height, edge);
        return now.turned(Vec3.ZERO, path.scale(-1.0), 0.0, 0.0, 0.0);
    }

    // Where each hand of a climbing mech takes hold `age` ticks in, posed with its torso where it is: the middle of its
    // palm, which way its fingers run and its palm faces, how firmly it holds and how far its fingers curl.
    static void place(MechClimbHolds holds, double age, MechPose pose) {
        double grip = grip(age, holds.height);
        double over = turned(age, holds.height);
        Vec3 wall = holds.start.ahead();
        for (int side = 0; side < 2; side++) {
            int k = MechClimbHolds.step(holds.handFrom[side], age);
            double u = k == 0 ? 1.0 : MechClimbHolds.moved(holds.handFrom[side], holds.handAt[side], k, age);
            Vec3[] to = palm(holds.hands[side][k], wall, holds.onEdge[side][k] ? over : 0.0);
            Vec3[] at = to;
            if (u < 1.0) {
                Vec3[] from = palm(holds.hands[side][k - 1], wall, holds.onEdge[side][k - 1] ? over : 0.0);
                double s = Ease.smooth(u);
                at = new Vec3[] { from[0].lerp(to[0], s).subtract(wall.scale(MechClimbHolds.SWING_OFF
                        * Math.sin(Math.PI * u))), from[1].lerp(to[1], s).normalize(), from[2].lerp(to[2], s)
                                .normalize() };
            }
            pose.ledge[side] = at[0];
            pose.along[side] = at[1];
            pose.facing[side] = at[2];
            pose.grip[side] = grip;
            pose.curl[side] = u < 1.0 ? Mth.lerp(Math.sin(Math.PI * u), HELD_CURL, OPEN_CURL) : HELD_CURL;
        }
    }

    // A hand on a hold (its knuckles' line on the face, at the top for one on the edge) as {the middle of its palm,
    // which way its fingers run, which way its palm faces}: held to the face, fingers up, or hooked over the edge
    // (`over` 0); turned `over` of the way over about the edge to press down on the top, fingers on into the wall.
    static Vec3[] palm(Vec3 hold, Vec3 wall, double over) {
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        double reach = MechClimbHolds.KNUCKLE_OVER - (MechArmShapes.KNUCKLES - MechScript.PALM_ALONG);
        Vec3 hooked = hold.subtract(wall.scale(FACE_OFF)).add(0.0, reach, 0.0);
        if (over <= 0.0) {
            return new Vec3[] { hooked, up, wall };
        }
        Vec3 pressed = hold.add(wall.scale(TOP_IN + MechScript.PALM_ALONG - MechArmShapes.WRIST))
                .add(0.0, GRIP_UP, 0.0);
        // Turned over the edge, not through it.
        Vec3 at = hooked.lerp(pressed, over).add(up.subtract(wall).scale(0.35 * Math.sin(Math.PI * over)));
        Vec3 axis = up.cross(wall).normalize();
        double turn = 0.5 * Math.PI * over;
        return new Vec3[] { at, Vectors.spin(up, axis, turn).normalize(), Vectors.spin(wall, axis, turn).normalize() };
    }

    // How firmly the hands hold the ledge `age` ticks in: reaching up to it, holding on, letting go as the body goes
    // up over the right foot, before the shoulders rise out of the arms' reach.
    static double grip(double age, double height) {
        double on = Ease.smooth((age - 1.0) / (GRAB - 2.0));
        double off = Ease.smooth((age - GRAB - pull(height) - OVER * HEAVE) / (0.25 * HEAVE));
        return Math.min(on, 1.0 - off);
    }

    // How far the hands on the edge are turned over onto the top `age` ticks in: hooked over it (0) until the shoulders
    // come up near it, pressed down on it (1) once they are over it.
    static double turned(double age, double height) {
        return Ease.smooth((rise(age, height) + MechScript.SHOULDER.y - height - TURN_FROM) / TURN_OVER);
    }

    // How far the body is lowered `age` ticks in, back to nothing at the end: it crouches as it reaches up and as it
    // throws its weight forward over the top, and sinks as far as its hands pressing down on the top need.
    static double low(double age, double height, double edge) {
        double total = ticks(height);
        double reach = 0.5 * Ease.bump((age - GRAB * 0.55) / (GRAB * 0.55));
        double heave = 0.6 * Ease.bump((age - GRAB - pull(height) - (OVER + 0.15) * HEAVE) / (0.4 * HEAVE));
        double sink = mantle(age, height, edge)[0];
        return -Math.max(reach + heave, sink) * (1.0 - Ease.smooth((age - total + 4.0) / 4.0));
    }

    // How far the body leans into the wall `age` ticks in (radians, ahead negative), back to nothing at the end: in as
    // it reaches up and hangs, over the top while its hands press down on it, forward as it throws its weight over,
    // but never so far that the front of its chest goes into the wall under the top.
    static double lean(double age, double height, double edge) {
        double total = ticks(height);
        double into = 0.12 + 0.22 * Mth.clamp((6.0 - height) / 3.0, 0.0, 1.0);
        double over = 0.2 * Ease.bump((age - GRAB - pull(height) - (OVER + 0.25) * HEAVE) / (0.35 * HEAVE));
        double on = Ease.smooth(age / GRAB) * (1.0 - Ease.smooth((age - total + 8.0) / 8.0));
        double lean = Math.max(into * on, mantle(age, height, edge)[1]) + over;
        Vec3 path = path(age, height, edge);
        double pivot = path.y + LEAN_FROM + low(age, height, edge);
        return -mostLean(edge - path.z, height - pivot, lean);
    }

    // How far the body sinks and leans over the top `age` ticks in as its hands press down on it, so that they reach
    // it: {sink, lean}. Through the heave it stays as at its start, as the arms let go.
    private static double[] mantle(double age, double height, double edge) {
        double pressing = grip(age, height) * turned(age, height);
        if (pressing <= 0.0) {
            return new double[] { 0.0, 0.0 };
        }
        double lean = MANTLE_LEAN * pressing;
        Vec3 path = path(Math.min(age, GRAB + pull(height)), height, edge);
        double arm = MechScript.SHOULDER.y - LEAN_FROM;
        double above = path.y + LEAN_FROM + arm * Math.cos(lean) - height - GRIP_UP;
        double x = MechScript.SHOULDER.x - MechClimbHolds.HAND_OUT;
        double z = edge + TOP_IN - path.z - arm * Math.sin(lean);
        double room = MANTLE_REACH * MANTLE_REACH - x * x - z * z;
        double sink = above - (room > 0.0 ? Math.sqrt(room) : 0.0);
        return new double[] { Mth.clamp(sink, 0.0, MOST_SINK) * pressing, lean };
    }

    // The most the body leans ahead, up to `wanted` (radians), before the front of its chest goes into a wall whose
    // edge stands `ahead` in front of and `over` above the pivot it leans about.
    private static double mostLean(double ahead, double over, double wanted) {
        if (wanted <= 0.0 || clear(ahead, over, wanted)) {
            return wanted;
        }
        double low = 0.0;
        double high = wanted;
        for (int i = 0; i < 16; i++) {
            double middle = 0.5 * (low + high);
            if (clear(ahead, over, middle)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    // Whether the front of the chest, leaned `lean` ahead, stays CHEST_GAP out of that wall: no part of it both ahead
    // of the face and under the top.
    static boolean clear(double ahead, double over, double lean) {
        double c = Math.cos(lean);
        double s = Math.sin(lean);
        double bottom = CHEST_BOTTOM - LEAN_FROM;
        double top = CHEST_TOP - LEAN_FROM;
        // How far up the front from the pivot's height it stands under the top, and from where it stands past the face.
        double under = (over + CHEST_GAP + CHEST * s) / c;
        double past = ahead - CHEST_GAP - CHEST * c;
        double from = s > 1.0E-9 ? past / s : past < 0.0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        return Math.max(bottom, from) >= Math.min(top, under);
    }

    // An arm holding its hand's hold, the middle of its palm at `palm` (in the world), its fingers running `along` and
    // its palm facing `facing`, blended from the walking arm by how firmly it holds: laid on its bones, its elbow out
    // of the blocks in `ground` (null: none) and its wrist turned to lay the hand so (MechArmRig).
    static MechMoves.Arm arm(MechMoves.Arm walking, MechScript.Stage torso, boolean right, Vec3 palm, Vec3 along,
            Vec3 facing, double grip, double curl, @Nullable MechHandRig.Ground ground) {
        Vec3 shoulder = MechPainter.side(MechScript.SHOULDER, right);
        MechMoves.Arm held = MechArmRig.laid(torso, right, shoulder, MechPainter.side(ELBOWS, right), palm, along,
                facing, curl, SPREAD, ground);
        if (grip >= 1.0) {
            return held;
        }
        Vec3 bend = walking.elbow().lerp(held.elbow(), grip).subtract(shoulder);
        Vec3 at = bend.lengthSqr() < 1.0E-8 ? held.elbow() : shoulder.add(bend.normalize().scale(MechScript.UPPER_ARM));
        Vec3 turned = walking.way().lerp(held.way(), grip);
        turned = turned.lengthSqr() < 1.0E-8 ? held.way() : turned.normalize();
        return new MechMoves.Arm(at, turned, square(walking.palm().lerp(held.palm(), grip), turned),
                Mth.lerp(grip, walking.curl(), held.curl()), Mth.lerp(grip, walking.spread(), held.spread()), 1.0,
                grip * held.fold(), grip * held.tilt());
    }

    // As arm(), its fingers settled on what the hand holds, with `ground` round it: the hand drawn back off it (the
    // way the back of the hand faces) as far as its fingers, opened as far as they go, would still reach into it.
    static MechMoves.Arm laid(MechMoves.Arm walking, MechScript.Stage torso, boolean right, Vec3 palm, Vec3 along,
            Vec3 facing, double grip, double curl, @Nullable MechHandRig.Ground ground) {
        MechMoves.Arm arm = arm(walking, torso, right, palm, along, facing, grip, curl, ground);
        if (ground == null || grip <= 0.0) {
            return arm;
        }
        double back = 0.0;
        for (int i = 0; i < RESTS; i++) {
            Frame hand = MechArmRig.hand(torso, arm, 0.0, 0.0);
            // Drawn with the other side's bones in its left-handed frame (see MechPainter.arm).
            double left = MechHandRig.rest(hand, arm, !right, ground);
            if (left <= MechHandRig.TOUCH) {
                break;
            }
            back += left;
            arm = arm(walking, torso, right, palm.subtract(facing.scale(back)), along, facing, grip, curl, ground);
        }
        return arm;
    }

    private static Vec3 square(Vec3 palm, Vec3 way) {
        Vec3 flat = palm.subtract(way.scale(palm.dot(way)));
        return flat.lengthSqr() < 1.0E-8 ? Vectors.across(way)[0] : flat.normalize();
    }
}
