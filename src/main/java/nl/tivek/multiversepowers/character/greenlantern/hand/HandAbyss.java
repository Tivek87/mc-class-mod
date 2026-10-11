package nl.tivek.multiversepowers.character.greenlantern.hand;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The abyss: two hands out of portals dig their fingers into the ground round the creature and tear it open, jerk by
// jerk, until the ground gives way into a deep round pit whose floor opens on a sky of stars. Tentacles of hard light
// rise out of that floor, wind round everything in the pit and drag it down into the stars, where it dies. Then the
// hands push the pit shut and the ground is whole again. Server and client both work it out from here; the pit's
// depth rides in the variant (0: the ground could not be opened, and the hole is only a window on the stars).
public final class HandAbyss {
    private static final double SLOW = 1.6;
    // In beats.
    private static final double REACH = 2.5;
    private static final double DIG = 9.0;
    private static final double[] JERKS = { 10.5, 12.5, 14.5 };
    private static final double OPEN = 16.0;
    private static final double RISE = 17.5;
    private static final double SNARE = 21.0;
    private static final double DRAGGED = 26.0;
    private static final double SUNK = 29.0;
    // The pit stays open this long after its last creature could be taken, three seconds.
    private static final double HELD_OPEN = 37.5;
    private static final double CLOSE = 36.0 + HELD_OPEN;
    private static final double SHUT = 40.0 + HELD_OPEN;
    private static final double BACK = 41.5 + HELD_OPEN;
    private static final double GONE = 47.0 + HELD_OPEN;
    // A creature that lives through being sunk rises again and is thrown out over the rim this long after its catch.
    private static final double SPAT = SUNK + 4.0;
    public static final int DIGS = ticks(DIG);
    public static final int[] TEARS = { ticks(JERKS[0]), ticks(JERKS[1]), ticks(JERKS[2]) };
    public static final int OPENS = ticks(OPEN);
    public static final int RISES = ticks(RISE);
    public static final int SNARES = ticks(SNARE);
    // Creatures that fall in after the snare are still taken, until this tick: late enough that the last is sunk and,
    // living, thrown out again before the pit shuts.
    public static final int SNARES_UNTIL = ticks(CLOSE - (SPAT - SNARE) - 1.0);
    public static final int DRAGS = ticks(DRAGGED - SNARE);
    public static final int SINKS = ticks(SUNK - SNARE);
    public static final int SPITS = ticks(SPAT - SNARE);
    public static final int CLOSES = ticks(CLOSE);
    public static final int SHUTS = ticks(SHUT);
    public static final int LEAVES = ticks(BACK);
    // The pit's radius and its deepest, in blocks.
    public static final double RADIUS = 3.2;
    public static final int DEEPEST = 9;
    public static final int SHALLOWEST = 3;
    // How many tentacles writhe in the pit besides those holding a creature.
    public static final int TENTACLES = 6;
    private static final double SCALE = 0.72;
    private static final double PORTAL_OUT = 5.4;
    private static final double PORTAL_SIDE = 3.4;
    private static final double PORTAL = 2.6;
    private static final Vec3 TIPS = new Vec3(0.0, 5.0, 1.6);

    private HandAbyss() {
    }

    private static int ticks(double beats) {
        return (int) Math.round(beats * SLOW);
    }

    public static int depth(int variant) {
        return HandPose.extra(variant);
    }

    // The floor of the pit, under its middle; with no pit the window lies on the ground itself.
    public static Vec3 floor(int variant, Vec3 center) {
        return center.subtract(0.0, depth(variant), 0.0);
    }

    // How far the ground has torn round the pit at tick t, 0 to 1: a share at each jerk, whole once it opens, and
    // pushed shut at the end.
    public static double torn(double t) {
        double beat = t / SLOW;
        double torn = 0.0;
        for (double jerk : JERKS) {
            torn += 0.3 * Ease.smoother((beat - jerk) / 1.0);
        }
        torn += 0.1 * Ease.smoother((beat - OPEN + 0.5) / 0.5);
        return torn * (1.0 - Ease.smoother((beat - CLOSE) / (SHUT - CLOSE)));
    }

    // Whether the ground is open at tick t: from the moment it gives way until the hands have pushed it shut.
    public static boolean open(double t) {
        return t >= OPENS && t < SHUTS;
    }

    // How wide the window on the stars at the pit's floor is at tick t, as a share of the pit's radius.
    public static double window(double t) {
        double beat = t / SLOW;
        return Ease.smoother((beat - OPEN) / 2.0) * (1.0 - Ease.smoother((beat - CLOSE - 1.0) / (SHUT - CLOSE - 1.0)));
    }

    // How far the tentacles have risen out of the floor at tick t, 0 to 1.
    public static double risen(double t) {
        double beat = t / SLOW;
        return Ease.smoother((beat - RISE) / 3.0) * (1.0 - Ease.smoother((beat - CLOSE + 2.0) / 3.0));
    }

    // Where a creature caught at `caught` (its middle) is held `since` ticks after: dragged down to the floor under
    // it, then sunk below it into the stars.
    public static Vec3 held(int variant, Vec3 center, Vec3 caught, double tall, double since) {
        Vec3 floor = floor(variant, center);
        Vec3 flat = new Vec3(caught.x - center.x, 0.0, caught.z - center.z);
        double far = flat.length();
        if (far > RADIUS * 0.7) {
            flat = flat.scale(RADIUS * 0.7 / far);
        }
        Vec3 down = floor.add(flat).add(0.0, tall * 0.5, 0.0);
        double drag = Ease.smoother(since / DRAGS);
        double sink = sunk(since);
        return caught.lerp(down, drag).subtract(0.0, sink * (tall + 0.6), 0.0);
    }

    // Where a creature that lived through it is, `since` ticks after its catch: risen from under the floor straight up
    // through the pit, then set over its rim onto the ground beside it, from where it is thrown out.
    public static Vec3 rising(int variant, Vec3 center, Vec3 caught, double tall, double since) {
        Vec3 sunk = held(variant, center, caught, tall, SINKS);
        Vec3 flat = Vectors.flat(new Vec3(caught.x - center.x, 0.0, caught.z - center.z));
        Vec3 top = new Vec3(sunk.x, center.y + tall * 0.5 + 0.5, sunk.z);
        Vec3 out = center.add(flat.scale(RADIUS + 1.0)).add(0.0, tall * 0.5 + 0.1, 0.0);
        double u = (since - SINKS) / (double) (SPITS - SINKS);
        return sunk.lerp(top, Ease.smoother(u / 0.7)).lerp(out, Ease.smoother((u - 0.7) / 0.3));
    }

    // How far a caught creature has sunk below the floor `since` ticks after it was caught, 0 to 1.
    public static double sunk(double since) {
        return Ease.smooth((since - DRAGS) / (double) (SINKS - DRAGS));
    }

    // Both hands reach down out of portals over two sides of the pit, fingers hooked into its rim, palms turned out:
    // they dig in, tear it open a jerk at a time, hold it open and push it shut again.
    public static List<HandGroup.Sub> hands(Vec3 center, Vec3 facing, double t) {
        Vec3 along = HandGroup.flat(facing);
        Vec3 across = Vectors.UP.cross(along).normalize();
        double beat = t / SLOW;
        double torn = torn(t);
        double reach = Ease.smoother((beat - REACH) / 6.5) * (1.0 - Ease.smoother((beat - BACK) / 5.0));
        double open = Ease.smoother((beat - 0.5) / 2.5) * (1.0 - Ease.smoother((beat - GONE) / 3.0));
        double dug = Ease.smoother((beat - DIG + 1.2) / 1.2) * (1.0 - Ease.smoother((beat - SHUT) / 1.5));
        double pulling = HandMotion.window(beat, DIG, JERKS[0], CLOSE, SHUT);
        double jerk = 0.0;
        for (double at : JERKS) {
            jerk += Ease.bump((beat - at - 0.3) / 0.9);
        }
        jerk += 1.5 * Ease.bump((beat - OPEN - 0.2) / 1.2);
        double shake = 0.04 * pulling * Math.sin(beat * 7.0);
        HandPose pose = HandGroup.fingers(Mth.lerp(dug, 0.15, 0.4) + 0.1 * pulling, Mth.lerp(dug, 0.05, 0.95),
                Mth.lerp(dug, 0.35, 0.55), Mth.lerp(dug, 0.7, 0.25));
        double rim = RADIUS * (0.75 + 0.25 * Math.min(1.0, torn));
        List<HandGroup.Sub> subs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            double side = i == 0 ? -1.0 : 1.0;
            Vec3 portal = center.add(across.scale(side * (PORTAL_SIDE + RADIUS))).add(0.0, PORTAL_OUT, 0.0);
            Vec3 grip = center.add(across.scale(side * (rim + 0.12 + 0.3 * jerk))).add(along.scale(shake * side))
                    .add(0.0, -0.15 + 0.2 * jerk, 0.0);
            Vec3 up = grip.subtract(portal).normalize();
            Vec3 palm = HandGroup.square(across.scale(side), up);
            subs.add(new HandGroup.Sub(pose, HandGroup.reaching(portal, grip, up, palm, TIPS, reach, SCALE),
                    HandGroup.portalAt(portal, up, PORTAL * SCALE * 1.15, open), side < 0.0));
        }
        return subs;
    }
}
