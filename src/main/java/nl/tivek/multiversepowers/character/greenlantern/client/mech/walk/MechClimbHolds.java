package nl.tivek.multiversepowers.character.greenlantern.client.mech.walk;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.math.Ease;

// What one climb holds on to, found once in the world as it starts, the same in every game from the same blocks:
// where the mech set off from, where and when each hand takes hold and where each foot stands. While the top is out of
// their reach the hands go up the wall's face hand over hand, each taking its next hold once the body has risen a rung
// past its last, and then onto the edge; the feet step up the face under the rising body, toes against it, and up onto
// the top at the end, the right one first.
final class MechClimbHolds {
    // How far the body rises between one hold of a hand or foot and its next; the right ones go first.
    static final double RUNG = 2.2;
    private static final double[] HAND_PHASE = { 0.5, 1.0 };
    private static final double[] FOOT_PHASE = { 0.05, 0.55 };
    // A hand takes hold with its knuckles this high over its shoulder, and from the ground no higher than FIRST_REACH
    // over the base; on the edge they stand KNUCKLE_OVER over the top.
    private static final double REACH_UP = 2.6;
    private static final double FIRST_REACH = MechScript.SHOULDER.y + 3.2;
    static final double KNUCKLE_OVER = 0.15;
    // Where the hands take hold: this far out from the middle.
    static final double HAND_OUT = 2.7;
    // A hand or foot goes from one hold to the next in MOVE ticks, swung SWING_OFF off the wall on the way.
    static final int MOVE = 6;
    static final double SWING_OFF = 0.7;
    // A foot on the face stands its ankle FOOT_UP over the base (as low again as the body sinks) and its toes TOE_OFF
    // short of the face, never nearer than FOOT_UNDER under the top.
    private static final double FOOT_UP = 3.5;
    private static final double TOE_OFF = 0.1;
    private static final double FOOT_UNDER = 0.8;
    // Off the ground with no hold on the face, a foot is drawn up under the hips, knees bent, and scrapes at the wall.
    private static final double TUCK_UP = 1.1;
    private static final double TUCK_IN = 0.5;
    private static final double SCRAPE = 0.3;
    // Swung up onto the top a foot takes FOOT_SWING ticks and clears the edge by FOOT_CLEAR; the right one comes down
    // FIRST_ON past the edge, the left where it stands once over.
    static final int FOOT_SWING = 8;
    private static final double FOOT_CLEAR = 0.9;
    private static final double FIRST_ON = 1.0;
    // A foot still in the air as the climb starts comes down onto where it stood in this many ticks.
    private static final double SET_DOWN = 4.0;

    final MechScript.Stage start;
    final double height;
    final double edge;
    // Per hand (0 the right): its holds in order, the knuckles' line on the wall's face (on the edge, at its top,
    // when onEdge), when it sets off for each and when it gets there.
    final Vec3[][] hands = new Vec3[2][];
    final boolean[][] onEdge = new boolean[2][];
    final double[][] handFrom = new double[2][];
    final double[][] handAt = new double[2][];
    // Per foot: its ankle's holds in order (where it stood, on the face, on the top) and when it sets off for and gets
    // to each.
    final Vec3[][] feet = new Vec3[2][];
    final double[][] footFrom = new double[2][];
    final double[][] footAt = new double[2][];
    // Per foot: where it was in the air as this game found the holds (null: on the ground), and when that was.
    private final Vec3[] air;
    private final double found;

    private MechClimbHolds(MechScript.Stage start, double height, double edge, Vec3[] air, double found) {
        this.start = start;
        this.height = height;
        this.edge = edge;
        this.air = air;
        this.found = found;
    }

    // `stood`: where each foot stands, or comes down from `air` (null: on the ground), `age` ticks in.
    static MechClimbHolds find(ClientLevel level, MechScript.Stage start, double height, double edge, Vec3[] stood,
            Vec3[] air, double age) {
        MechClimbHolds holds = new MechClimbHolds(start, height, edge, air.clone(), age);
        double floor = start.base().y;
        Vec3 ahead = start.ahead();
        double lift = MechClimb.lift(height);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            Vec3 lane = start.point(MechPainter.side(new Vec3(HAND_OUT, 0.0, 0.0), right));
            double[] ledge = MechGround.ledge(level, lane, ahead, floor + height - 1.5, floor + height + 1.0,
                    MechClimb.FARTHEST + 1.5);
            double top = ledge == null ? floor + height : ledge[1];
            Double face = MechGround.face(level, lane, ahead, top - 0.3, (ledge == null ? edge : ledge[0]) + 0.5);
            double off = face != null ? face : ledge == null ? edge : ledge[0];
            holds.hand(side, lane, ahead, floor, top, off, lift, level);
            holds.foot(side, stood[side], ahead, floor, top, lift, level);
        }
        return holds;
    }

    // The hand's holds: from the ground as high as it reaches (or on the edge, if that is in reach), then a rung up
    // the face each time the body has risen one, until it takes the edge.
    private void hand(int side, Vec3 lane, Vec3 ahead, double floor, double top, double off, double lift,
            ClientLevel level) {
        List<Vec3> at = new ArrayList<>();
        List<Boolean> rim = new ArrayList<>();
        List<Double> from = new ArrayList<>();
        List<Double> to = new ArrayList<>();
        double knuckles = Math.min(top + KNUCKLE_OVER, floor + FIRST_REACH);
        boolean onTop = knuckles >= top + KNUCKLE_OVER - 1.0E-6;
        at.add(this.held(lane, ahead, knuckles, top, off, onTop, level));
        rim.add(onTop);
        from.add(0.0);
        to.add(MechClimb.GRAB - 1.0);
        int last = MechClimb.GRAB + MechClimb.pull(this.height);
        for (int k = 0; !onTop; k++) {
            double rise = (k + HAND_PHASE[side]) * RUNG;
            double leave = rise < lift ? MechClimb.ageAt(rise, this.height) : last - MOVE;
            // However it falls, the hand is up on the edge before the heave.
            leave = Math.min(leave, last - MOVE);
            double then = MechClimb.path(leave + MOVE, this.height, this.edge).y;
            knuckles = Math.min(top + KNUCKLE_OVER, floor + then + MechScript.SHOULDER.y + REACH_UP);
            onTop = knuckles >= top + KNUCKLE_OVER - 1.0E-6 || leave >= last - MOVE;
            at.add(this.held(lane, ahead, onTop ? top + KNUCKLE_OVER : knuckles, top, off, onTop, level));
            rim.add(onTop);
            from.add(leave);
            to.add(leave + MOVE);
        }
        this.hands[side] = at.toArray(Vec3[]::new);
        this.onEdge[side] = new boolean[rim.size()];
        for (int k = 0; k < rim.size(); k++) {
            this.onEdge[side][k] = rim.get(k);
        }
        this.handFrom[side] = from.stream().mapToDouble(Double::doubleValue).toArray();
        this.handAt[side] = to.stream().mapToDouble(Double::doubleValue).toArray();
    }

    // Where a hand's knuckles meet the face with them at `knuckles`: on the edge at the top, or on the face where it
    // stands at that height (as far off as the edge where none is found), one knuckle's height under them.
    private Vec3 held(Vec3 lane, Vec3 ahead, double knuckles, double top, double off, boolean onTop,
            ClientLevel level) {
        double d = off;
        if (!onTop) {
            Double face = MechGround.face(level, lane, ahead, knuckles - 0.6, off + 1.5);
            d = face == null ? off : face;
        }
        Vec3 spot = lane.add(ahead.scale(d));
        return new Vec3(spot.x, onTop ? top : knuckles - KNUCKLE_OVER, spot.z);
    }

    // The foot's holds: where it stood, then a rung up the face each time the body has risen one while it hangs, and
    // up onto the top.
    private void foot(int side, Vec3 stood, Vec3 ahead, double floor, double top, double lift, ClientLevel level) {
        boolean right = side == 0;
        List<Vec3> at = new ArrayList<>();
        List<Double> from = new ArrayList<>();
        List<Double> to = new ArrayList<>();
        at.add(stood);
        from.add(-1.0);
        to.add(-1.0);
        double up = upAt(side);
        Vec3 lane = this.start.point(MechPainter.side(new Vec3(MechScript.ANKLE.x, 0.0, 0.0), right));
        for (int k = 0; lift > 0.0; k++) {
            double rise = (k + FOOT_PHASE[side]) * RUNG;
            if (rise >= lift) {
                break;
            }
            double leave = MechClimb.ageAt(rise, this.height);
            if (leave + MOVE > up) {
                break;
            }
            double ankle = floor + MechClimb.rise(leave + MOVE, this.height) + FOOT_UP
                    + MechClimb.low(leave + MOVE, this.height, this.edge);
            if (ankle > top - FOOT_UNDER) {
                break;
            }
            Double face = MechGround.face(level, lane, ahead, ankle - 0.7, this.edge + 1.5);
            if (face == null) {
                continue;
            }
            Vec3 spot = lane.add(ahead.scale(face - MechGround.SOLE_AHEAD - TOE_OFF));
            at.add(new Vec3(spot.x, ankle, spot.z));
            from.add(leave);
            to.add(leave + MOVE);
        }
        double on = right ? this.edge + FIRST_ON : this.edge + MechClimb.PAST;
        Vec3 spot = this.start.point(MechPainter.side(MechScript.ANKLE, right).add(0.0, this.height, on));
        double base = floor + this.height;
        Double ground = MechGround.foot(level, spot, ahead, base, base + 1.0);
        at.add(new Vec3(spot.x, (ground == null ? base : ground) + MechScript.ANKLE.y, spot.z));
        from.add(up);
        to.add(up + FOOT_SWING);
        this.feet[side] = at.toArray(Vec3[]::new);
        this.footFrom[side] = from.stream().mapToDouble(Double::doubleValue).toArray();
        this.footAt[side] = to.stream().mapToDouble(Double::doubleValue).toArray();
    }

    // When a foot swings up onto the top: the right one as the body comes level with the ledge, the left once the
    // body is over it.
    private double upAt(int side) {
        double heave = MechClimb.GRAB + MechClimb.pull(this.height);
        return side == 0 ? heave + MechClimb.OVER * MechClimb.HEAVE - FOOT_SWING : heave + 0.5 * MechClimb.HEAVE;
    }

    // Which of a limb's holds it is at or on its way to `age` ticks in.
    static int step(double[] from, double age) {
        int k = 0;
        while (k + 1 < from.length && age >= from[k + 1]) {
            k++;
        }
        return k;
    }

    // How far a limb has got on its way to hold k, 0 to 1 (1: there).
    static double moved(double[] from, double[] to, int k, double age) {
        return to[k] <= from[k] ? 1.0 : Mth.clamp((age - from[k]) / (to[k] - from[k]), 0.0, 1.0);
    }

    // Where a foot's ankle is `age` ticks in: on its hold, on its way between two (swung off the wall, or up over the
    // edge onto the top), or, still where it stood as the body lifts off, drawn up under it.
    Vec3 foot(int side, double age) {
        int k = step(this.footFrom[side], age);
        if (k == 0) {
            return this.hanging(side, age);
        }
        double u = moved(this.footFrom[side], this.footAt[side], k, age);
        Vec3 to = this.feet[side][k];
        if (u >= 1.0) {
            return to;
        }
        Vec3 from = k == 1 ? this.hanging(side, this.footFrom[side][k]) : this.feet[side][k - 1];
        if (k == this.feet[side].length - 1) {
            double across = Ease.smooth((u - 0.25) / 0.75);
            double y = Mth.lerp(Ease.smooth(u / 0.7), from.y, to.y) + FOOT_CLEAR * Math.sin(Math.PI * u);
            return new Vec3(Mth.lerp(across, from.x, to.x), y, Mth.lerp(across, from.z, to.z));
        }
        double s = Ease.smooth(u);
        return from.lerp(to, s).subtract(this.start.ahead().scale(SWING_OFF * Math.sin(Math.PI * u)));
    }

    // Whether a foot has just come down on a hold this tick, and on the top.
    boolean footLands(int side, double age) {
        return lands(this.footAt[side], age, 1);
    }

    boolean onTop(int side, double age) {
        return step(this.footFrom[side], age) == this.feet[side].length - 1;
    }

    // Whether a hand has just taken a hold this tick.
    boolean handLands(int side, double age) {
        return lands(this.handAt[side], age, 0);
    }

    private static boolean lands(double[] at, double age, int first) {
        for (int k = first; k < at.length; k++) {
            if (at[k] >= 0.0 && age >= at[k] && age - 1.0 < at[k]) {
                return true;
            }
        }
        return false;
    }

    private Vec3 hanging(int side, double age) {
        Vec3 stood = this.feet[side][0];
        if (this.air[side] != null) {
            stood = this.air[side].lerp(stood, Ease.smooth((age - this.found) / SET_DOWN));
        }
        Vec3 path = MechClimb.path(age, this.height, this.edge);
        double off = Ease.smooth(path.y / MechClimb.LIFT_OFF);
        if (off <= 0.0) {
            return stood;
        }
        double scrape = SCRAPE * Math.sin(age * 0.9 + side * Math.PI) * off;
        double low = MechClimb.low(age, this.height, this.edge);
        Vec3 tucked = this.start.point(MechPainter.side(MechScript.ANKLE, side == 0)
                .add(0.0, path.y + low + TUCK_UP + scrape, path.z + TUCK_IN));
        return stood.lerp(tucked, off);
    }
}
