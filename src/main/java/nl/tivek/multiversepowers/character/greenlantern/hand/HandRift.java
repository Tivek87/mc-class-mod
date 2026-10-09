package nl.tivek.multiversepowers.character.greenlantern.hand;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The rift: two hands out of portals dig their fingers into the ground or a wall beyond the creature and tear it open
// on a deep sky of stars; a tentacle of hard light lashes out of it, winds round the creature, drags it over and pulls
// it in. Whatever is still alive when the rift is done with it is hurled back out; then the hands push the rift shut
// and it closes in a burst of starlight. Server and client both work it out from here.
public final class HandRift {
    private static final double SLOW = 1.6;
    // In beats.
    private static final double REACH = 2.5;
    private static final double DIG = 9.0;
    private static final double[] JERKS = { 10.5, 12.5, 14.5 };
    private static final double LASH = 16.0;
    private static final double SNARE = 19.0;
    private static final double DRAGGED = 24.5;
    private static final double SUNK = 27.5;
    private static final double[] BURNS = { 29.5, 32.5, 35.5, 38.5 };
    private static final double EJECT = 41.0;
    private static final double CLOSE = 42.0;
    private static final double SHUT = 45.0;
    private static final double BACK = 46.5;
    private static final double GONE = 52.0;
    public static final int DIGS = ticks(DIG);
    public static final int[] TEARS = { ticks(JERKS[0]), ticks(JERKS[1]), ticks(JERKS[2]) };
    public static final int LASHES = ticks(LASH);
    public static final int SNARES = ticks(SNARE);
    public static final int DRAGS = ticks(DRAGGED);
    public static final int SINKS = ticks(SUNK);
    public static final int[] BURNS_AT = { ticks(BURNS[0]), ticks(BURNS[1]), ticks(BURNS[2]), ticks(BURNS[3]) };
    public static final int EJECTS = ticks(EJECT);
    public static final int CLOSES = ticks(CLOSE);
    public static final int SHUTS = ticks(SHUT);
    public static final int LEAVES = ticks(BACK);
    // Half the rift's length, and how far each lip is drawn from its middle line once torn open, in blocks.
    public static final double HALF = 2.4;
    public static final double WIDE = 1.1;
    // How far beyond the creature the rift opens, and how far its tentacle reaches out for one.
    public static final double AWAY = 3.6;
    public static final double LASH_REACH = 7.0;
    private static final double SCALE = 0.68;
    private static final double PORTAL_OUT = 5.2;
    private static final double PORTAL_SIDE = 3.6;
    private static final double PORTAL = 2.6;
    // The tips of the hooked fingers, in the hand's own frame: they dig in just inside each lip.
    private static final Vec3 TIPS = new Vec3(0.0, 5.0, 1.6);

    // The rift's own frame: its middle on the surface, the way out of the surface, along its length and across it.
    public record Frame(Vec3 center, Vec3 normal, Vec3 along, Vec3 across) {
        public Vec3 at(double length, double side, double out) {
            return this.center.add(this.along.scale(length)).add(this.across.scale(side))
                    .add(this.normal.scale(out));
        }
    }

    private HandRift() {
    }

    private static int ticks(double beats) {
        return (int) Math.round(beats * SLOW);
    }

    // In the ground the rift runs along the way to the creature; in a wall, facing out of it, it runs upright.
    public static Frame frame(int variant, Vec3 center, Vec3 facing) {
        Vec3 flat = HandGroup.flat(facing);
        boolean wall = HandPose.wall(variant);
        Vec3 normal = wall ? flat : Vectors.UP;
        Vec3 along = wall ? Vectors.UP : flat;
        return new Frame(center, normal, along, normal.cross(along).normalize());
    }

    // How far each lip is from the middle line at tick t: a hairline crack as the fingers dig in, a third wider at each
    // jerk, breathing while it is open, then pushed shut faster and faster.
    public static double gap(double t) {
        double beat = t / SLOW;
        double torn = 0.0;
        for (double jerk : JERKS) {
            torn += Ease.smoother((beat - jerk) / 1.1) / JERKS.length;
        }
        double open = 0.05 * Ease.smoother((beat - DIG) / 0.6) + (WIDE - 0.05) * torn
                + 0.04 * torn * Math.sin(beat * 0.9);
        double shut = Mth.clamp((beat - CLOSE) / (SHUT - CLOSE), 0.0, 1.0);
        return Math.max(0.0, open * (1.0 - shut * shut));
    }

    // How far the tentacle has lashed out of the rift at tick t, 0 to 1: it has hold of the creature at 1.
    public static double lashed(double t) {
        return Ease.smoother((t / SLOW - LASH) / (SNARE - LASH));
    }

    // How far the caught creature has sunk into the rift at tick t, 0 to 1, once dragged to its middle: in until the
    // rift lets go of it.
    public static double sunk(double t) {
        return Ease.smooth((t - DRAGS) / (double) (SINKS - DRAGS));
    }

    // How hard the rift burns what it holds at tick t: a flare at each burn.
    public static double burning(double t) {
        double beat = t / SLOW;
        double burn = 0.0;
        for (double at : BURNS) {
            burn += Ease.jolt((beat - at) / 2.0);
        }
        return burn;
    }

    // Where the tentacle lashes out to before it has hold: the middle of a creature standing where the rift was torn
    // open for it.
    public static Vec3 lashAt(int variant, Frame frame) {
        return HandPose.wall(variant) ? frame.at(0.9 - HALF - 0.3, 0.0, 1.4) : frame.at(AWAY, 0.0, 0.9);
    }

    // How far the caught creature's middle stands out of the surface it is held on: half its height, or half its
    // width out of a wall.
    public static double extent(int variant, double width, double height) {
        return (HandPose.wall(variant) ? width : height) * 0.5 + 0.02;
    }

    // Where the caught creature's middle is held at tick t: dragged from where it was caught over to the rift's middle,
    // with a hop over the lip, and held there.
    public static Vec3 held(Frame frame, Vec3 caught, double extent, double t) {
        Vec3 middle = frame.center().add(frame.normal().scale(extent));
        double u = Ease.smoother((t - SNARES) / (double) (DRAGS - SNARES));
        return caught.lerp(middle, u).add(frame.normal().scale(0.7 * Math.sin(Math.PI * u)));
    }

    // Both hands reach out of portals over each side of the rift, fingers down and hooked, palms turned out: they dig
    // in at the middle of each lip, pull it out a jerk at a time, hold it open and push it shut again.
    public static List<HandGroup.Sub> hands(int variant, Vec3 center, Vec3 facing, double t) {
        Frame frame = frame(variant, center, facing);
        double beat = t / SLOW;
        double gap = gap(t);
        double reach = Ease.smoother((beat - REACH) / 6.5) * (1.0 - Ease.smoother((beat - BACK) / 5.0));
        double open = Ease.smoother((beat - 0.5) / 2.5) * (1.0 - Ease.smoother((beat - GONE) / 3.0));
        double dug = Ease.smoother((beat - DIG + 1.2) / 1.2) * (1.0 - Ease.smoother((beat - SHUT) / 1.5));
        double pulling = HandMotion.window(beat, DIG, JERKS[0], CLOSE, SHUT);
        double jerk = 0.0;
        for (double at : JERKS) {
            jerk += Ease.bump((beat - at - 0.3) / 0.9);
        }
        double shake = 0.03 * pulling * Math.sin(beat * 7.0);
        HandPose pose = HandGroup.fingers(Mth.lerp(dug, 0.15, 0.4) + 0.1 * pulling, Mth.lerp(dug, 0.05, 0.95),
                Mth.lerp(dug, 0.35, 0.55), Mth.lerp(dug, 0.7, 0.25));
        List<HandGroup.Sub> subs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            double side = i == 0 ? -1.0 : 1.0;
            Vec3 portal = frame.at(0.0, side * PORTAL_SIDE, PORTAL_OUT);
            Vec3 grip = frame.at(shake * side, side * (gap + 0.12), -0.15 + 0.25 * jerk);
            Vec3 up = grip.subtract(portal).normalize();
            Vec3 palm = HandGroup.square(frame.across().scale(side), up);
            subs.add(new HandGroup.Sub(pose, HandGroup.reaching(portal, grip, up, palm, TIPS, reach, SCALE),
                    HandGroup.portalAt(portal, up, PORTAL * SCALE * 1.15, open), side < 0.0));
        }
        return subs;
    }
}
