package nl.tivek.multiversepowers.character.greenlantern.client.mech.walk;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechHandRig;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.rig.Ik;

// A mech hauling itself up onto a ledge too high to step onto, out of a pit, a trench or a cave: it reaches up and
// slams both hands onto the top, hauls its body up the wall with its knees against it, swings its right foot up onto
// the ledge, heaves itself over and brings the left one up after. The pilot's game starts it (MechDrive) and moves the
// mech along its path; every game poses it from how far it has got, the ledge's height and how far off its face was,
// which travel packed in one number.
final class MechClimb {
    static final double HIGHEST = 9.5;
    static final double FARTHEST = 3.4;
    // Its base ends this far past the ledge's edge, both feet on the top.
    private static final double PAST = 1.6;
    private static final double FIRST_ON = 1.0;
    private static final int GRAB = 9;
    // Up to this height the arms push it up as its foot steps onto the ledge; above it they haul it up first.
    private static final double PULL_FROM = 3.4;
    private static final double PULL_PER_BLOCK = 5.0;
    private static final int HEAVE = 22;
    // How far through the heave the right foot is up on the top and the body starts over it.
    private static final double OVER = 0.3;
    private static final double QUANTUM = 8.0;
    // Where the hands take hold: this far out from the middle, past the edge and over the top.
    private static final double HAND_OUT = 2.7;
    private static final double GRIP_IN = 0.3;
    static final double GRIP_UP = 0.35;
    // Off the ground the feet are drawn up under the hips, knees bent against the wall, and scrape at it.
    private static final double TUCK_UP = 1.1;
    private static final double TUCK_IN = 0.5;
    private static final double SCRAPE = 0.3;
    private static final int FOOT_SWING = 8;
    private static final double FOOT_CLEAR = 0.9;
    private static final Vec3 ELBOWS = new Vec3(0.8, -0.25, -0.55);
    // From the palm on along the hand to its knuckles, and how high over the top they rest: their thickness.
    private static final double KNUCKLES_ON = MechArmShapes.KNUCKLES - MechScript.PALM_ALONG;
    private static final double KNUCKLE_REST = 0.17;
    // The palm on the top faces down, a little towards the wall; its fingers settle onto the top in this many tries.
    private static final double PALM_AHEAD = 0.35;
    private static final int RESTS = 3;

    private MechClimb() {
    }

    // What one climb holds on to, found once as it starts: where the mech set off from, where each hand takes hold,
    // where each foot stood and where it comes to stand on the top.
    static final class Hold {
        final MechScript.Stage start;
        final double height;
        final double edge;
        final Vec3[] hands = new Vec3[2];
        final Vec3[] stood = new Vec3[2];
        final Vec3[] tops = new Vec3[2];

        private Hold(MechScript.Stage start, double height, double edge) {
            this.start = start;
            this.height = height;
            this.edge = edge;
        }
    }

    static int pack(int age, double height, double edge) {
        int h = Mth.clamp((int) Math.round(height * QUANTUM), 0, 127);
        int e = Mth.clamp((int) Math.round(edge * QUANTUM), 0, 63);
        return 1 | Mth.clamp(age, 0, 127) << 1 | h << 8 | e << 15;
    }

    static int age(int packed) {
        return packed >>> 1 & 127;
    }

    static double height(int packed) {
        return (packed >>> 8 & 127) / QUANTUM;
    }

    static double edge(int packed) {
        return (packed >>> 15 & 63) / QUANTUM;
    }

    // A height or distance as the packed number carries it.
    static double quantized(double value) {
        return Math.round(value * QUANTUM) / QUANTUM;
    }

    private static int pull(double height) {
        return (int) Math.round(PULL_PER_BLOCK * Math.max(0.0, height - PULL_FROM));
    }

    static int ticks(double height) {
        return GRAB + pull(height) + HEAVE;
    }

    // How far its base has risen and gone ahead of where it started (in its own places), `age` ticks in: still while
    // the hands reach up, hauled straight up the wall until its hips are level with the top, then, once its right foot
    // is up there, heaved up and over onto it.
    static Vec3 path(double age, double height, double edge) {
        int pull = pull(height);
        double lift = Math.max(0.0, height - PULL_FROM);
        if (age <= GRAB) {
            return Vec3.ZERO;
        }
        if (age <= GRAB + pull) {
            return new Vec3(0.0, lift * Ease.smooth((age - GRAB) / pull), 0.0);
        }
        double v = Mth.clamp((age - GRAB - pull) / HEAVE, 0.0, 1.0);
        return new Vec3(0.0, lift + (height - lift) * Ease.smooth((v - OVER) / 0.55),
                (edge + PAST) * Ease.smooth((v - OVER) / (1.0 - OVER)));
    }

    static Hold hold(ClientLevel level, MechScript.Stage start, double height, double edge, Vec3[] stood) {
        Hold hold = new Hold(start, height, edge);
        double floor = start.base().y;
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            hold.stood[side] = stood[side];
            Vec3 from = start.point(MechPainter.side(new Vec3(HAND_OUT, 0.0, 0.0), right));
            double[] ledge = MechGround.ledge(level, from, start.ahead(), floor + height - 1.5, floor + height + 1.0,
                    FARTHEST + 1.0);
            double d = ledge == null ? edge : ledge[0];
            double top = ledge == null ? floor + height : ledge[1];
            Vec3 grip = from.add(start.ahead().scale(d + GRIP_IN));
            hold.hands[side] = new Vec3(grip.x, top + GRIP_UP, grip.z);
            // The right foot comes down just past the edge, the left one where it stands once over.
            double on = right ? edge + FIRST_ON : edge + PAST;
            Vec3 spot = start.point(MechPainter.side(MechScript.ANKLE, right).add(0.0, height, on));
            double base = floor + height;
            Double ground = MechGround.foot(level, spot, start.ahead(), base, base + 1.0);
            hold.tops[side] = new Vec3(spot.x, (ground == null ? base : ground) + MechScript.ANKLE.y, spot.z);
        }
        return hold;
    }

    // Where the mech's base set off from, `age` ticks into a climb that has it at `now`.
    static MechScript.Stage start(MechScript.Stage now, double age, double height, double edge) {
        Vec3 path = path(age, height, edge);
        return now.turned(Vec3.ZERO, path.scale(-1.0), 0.0, 0.0, 0.0);
    }

    // When a foot swings up onto the top: the right one as the body comes level with the ledge, the left once the
    // body is over it.
    private static double upAt(int side, double height) {
        return side == 0 ? GRAB + pull(height) + OVER * HEAVE - FOOT_SWING : GRAB + pull(height) + 0.5 * HEAVE;
    }

    // Where a foot is `age` ticks in: where it stood until the body lifts it off, then drawn up and scraping the wall,
    // then swung up over the edge onto the top.
    static Vec3 foot(Hold hold, int side, double age) {
        double up = upAt(side, hold.height);
        if (age < up) {
            return hanging(hold, side, age);
        }
        double u = Mth.clamp((age - up) / FOOT_SWING, 0.0, 1.0);
        Vec3 from = hanging(hold, side, up);
        Vec3 to = hold.tops[side];
        double across = Ease.smooth((u - 0.25) / 0.75);
        double y = Mth.lerp(Ease.smooth(u / 0.7), from.y, to.y) + FOOT_CLEAR * Math.sin(Math.PI * u);
        return new Vec3(Mth.lerp(across, from.x, to.x), y, Mth.lerp(across, from.z, to.z));
    }

    // Whether a foot has just come down on the top this tick.
    static boolean lands(Hold hold, int side, double age) {
        double at = upAt(side, hold.height) + FOOT_SWING;
        return age >= at && age - 1.0 < at;
    }

    private static Vec3 hanging(Hold hold, int side, double age) {
        Vec3 path = path(age, hold.height, hold.edge);
        double off = Ease.smooth(path.y / 1.2);
        if (off <= 0.0) {
            return hold.stood[side];
        }
        double scrape = SCRAPE * Math.sin(age * 0.9 + side * Math.PI) * off;
        Vec3 tucked = hold.start.point(MechPainter.side(MechScript.ANKLE, side == 0)
                .add(0.0, path.y + TUCK_UP + scrape, TUCK_IN));
        return hold.stood[side].lerp(tucked, off);
    }

    // How firmly the hands hold the ledge `age` ticks in: reaching up to it, holding on, letting go as the body goes
    // up over the right foot, before the shoulders rise out of the arms' reach.
    static double grip(double age, double height) {
        double on = Ease.smooth((age - 1.0) / (GRAB - 2.0));
        double off = Ease.smooth((age - GRAB - pull(height) - OVER * HEAVE) / (0.25 * HEAVE));
        return Math.min(on, 1.0 - off);
    }

    // Whether the hands have just slammed onto the ledge this tick.
    static boolean slams(double age) {
        return age >= GRAB - 1.0 && age - 1.0 < GRAB - 1.0;
    }

    // How far the body is lowered and leant into the wall `age` ticks in, both back to nothing at the end: it crouches
    // as it reaches up, hangs with its chest to the wall and throws its weight forward over the top.
    static double low(double age, double height) {
        double total = ticks(height);
        double reach = 0.5 * Ease.bump((age - GRAB * 0.55) / (GRAB * 0.55));
        double heave = 0.6 * Ease.bump((age - GRAB - pull(height) - (OVER + 0.15) * HEAVE) / (0.4 * HEAVE));
        return -(reach + heave) * (1.0 - Ease.smooth((age - total + 4.0) / 4.0));
    }

    static double lean(double age, double height) {
        double total = ticks(height);
        double into = 0.12 + 0.22 * Mth.clamp((6.0 - height) / 3.0, 0.0, 1.0);
        double over = 0.2 * Ease.bump((age - GRAB - pull(height) - (OVER + 0.25) * HEAVE) / (0.35 * HEAVE));
        double on = Ease.smooth(age / GRAB) * (1.0 - Ease.smooth((age - total + 8.0) / 8.0));
        return -(into * on + over);
    }

    // An arm reaching for, or holding, its hand's spot on the ledge (in the world) of a wall facing `wall`, blended
    // from the walking arm by how firmly it holds.
    static MechMoves.Arm arm(MechMoves.Arm walking, MechScript.Stage torso, boolean right, Vec3 spot, Vec3 wall,
            double grip) {
        Vec3 shoulder = MechPainter.side(MechScript.SHOULDER, right);
        Vec3 pole = MechPainter.side(ELBOWS, right);
        double[] out = new double[3];
        Vec3 target = torso.local(spot);
        Vec3 elbow = reach(shoulder, target, pole, out);
        // The hand and forearm are one: coming down onto the top, its knuckles reach on past the palm. The hand is
        // raised till they rest on the top instead of in it, so its fingers can take hold of it.
        Vec3 knuckles = torso.point(target.add(target.subtract(elbow).normalize().scale(KNUCKLES_ON)));
        double sunk = spot.y - GRIP_UP + KNUCKLE_REST - knuckles.y;
        if (sunk > 0.0) {
            target = torso.local(spot.add(0.0, sunk, 0.0));
            elbow = reach(shoulder, target, pole, out);
        }
        Vec3 way = target.subtract(elbow);
        way = way.lengthSqr() < 1.0E-8 ? walking.way() : way.normalize();
        Vec3 down = torso.local(torso.base().add(wall.scale(PALM_AHEAD).subtract(0.0, 1.0, 0.0)));
        MechMoves.Arm held = new MechMoves.Arm(elbow, way, square(down, way), 0.7, 0.25, 1.0);
        if (grip >= 1.0) {
            return held;
        }
        Vec3 bend = walking.elbow().lerp(held.elbow(), grip).subtract(shoulder);
        Vec3 at = bend.lengthSqr() < 1.0E-8 ? held.elbow() : shoulder.add(bend.normalize().scale(MechScript.UPPER_ARM));
        Vec3 turned = walking.way().lerp(held.way(), grip);
        turned = turned.lengthSqr() < 1.0E-8 ? held.way() : turned.normalize();
        return new MechMoves.Arm(at, turned, square(walking.palm().lerp(held.palm(), grip), turned),
                Mth.lerp(grip, walking.curl(), held.curl()), Mth.lerp(grip, walking.spread(), held.spread()), 1.0);
    }

    // As arm(), the hand laid on the top with `ground` round it: raised as far as its fingers, opened as far as they
    // go, would still reach into it, so they rest on the top.
    static MechMoves.Arm laid(MechMoves.Arm walking, MechScript.Stage torso, boolean right, Vec3 spot, Vec3 wall,
            double grip, @Nullable MechHandRig.Ground ground) {
        MechMoves.Arm arm = arm(walking, torso, right, spot, wall, grip);
        if (ground == null || grip <= 0.0) {
            return arm;
        }
        double raise = 0.0;
        for (int i = 0; i < RESTS; i++) {
            Frame hand = Frame.of(torso.point(arm.elbow()), torso.dir(arm.palm()), torso.dir(arm.way()), 1.0);
            // Drawn with the other side's bones in its left-handed frame (see MechPainter.arm).
            double left = MechHandRig.rest(hand, arm, !right, ground);
            if (left <= MechHandRig.TOUCH) {
                break;
            }
            raise += left;
            arm = arm(walking, torso, right, spot.add(0.0, raise, 0.0), wall, grip);
        }
        return arm;
    }

    // The elbow of an arm from the shoulder with its palm at the target, bent towards the pole.
    private static Vec3 reach(Vec3 shoulder, Vec3 target, Vec3 pole, double[] out) {
        Ik.twoBone(new double[] { shoulder.x, shoulder.y, shoulder.z }, new double[] { target.x, target.y, target.z },
                new double[] { pole.x, pole.y, pole.z }, MechScript.UPPER_ARM, MechScript.PALM_ALONG, out);
        return new Vec3(out[0], out[1], out[2]);
    }

    private static Vec3 square(Vec3 palm, Vec3 way) {
        Vec3 flat = palm.subtract(way.scale(palm.dot(way)));
        return flat.lengthSqr() < 1.0E-8 ? Vectors.across(way)[0] : flat.normalize();
    }
}
