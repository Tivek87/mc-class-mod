package nl.tivek.multiversepowers.character.greenlantern.hand;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The Cosmos Test: one hand out of a portal draws a circle of light on the ground with a fingertip, and the circle opens
// on a deep sky of stars; a second hand out of a portal overhead dives into it at full speed. Then a rift opens in the
// sky beside the moon, the hand reaches out of it, takes the moon (the sky's own, its square face made a cube) and
// draws it nearer, crushes it slowly and it bursts. The sky goes black without it until the moon comes back. All of it
// slow, as a filmed moment. Server and client both work out the moments from here; only each client knows
// where the moon stands in its own sky.
public final class HandCosmos {
    private static final double SLOW = 2.6;
    // In beats.
    private static final double A_REACH = 2.5;
    private static final double DRAW = 7.0;
    private static final double DRAWN = 29.0;
    private static final double FILL = 32.5;
    private static final double A_BACK = 30.5;
    private static final double A_GONE = 36.0;
    private static final double B_OUT = 31.5;
    private static final double B_COCK = 35.5;
    private static final double DIVE = 37.0;
    private static final double DIVEN = 38.6;
    private static final double HOLE_SHUT = 40.5;
    private static final double HOLE_GONE = 44.5;
    private static final double NIGHT_FROM = 22.0;
    private static final double NIGHT_TO = 41.0;
    private static final double RIFT_OPEN = 42.5;
    private static final double RIFT_WIDE = 51.0;
    private static final double MOON_SWAP = 44.0;
    private static final double MOON_REACH = 51.0;
    private static final double GRAB = 57.0;
    private static final double CRUSH = 63.5;
    private static final double BOOM = 96.5;
    private static final double LET = 98.5;
    private static final double RIFT_SHUT = 104.5;
    private static final double RIFT_GONE = 109.0;
    private static final double BLACK = 106.5;
    private static final double DAWN = 126.5;
    private static final double DAY = 138.5;
    public static final double LIFE = 142.5;
    public static final int DRAWS = ticks(DRAW);
    public static final int OPENS = ticks(DRAWN);
    public static final int DIVES = ticks(DIVE);
    public static final int PLUNGES = ticks(DIVE + (DIVEN - DIVE) * 0.65);
    public static final int SHUTS = ticks(HOLE_SHUT);
    public static final int TRACES = ticks(RIFT_OPEN - 3.0);
    public static final int TEARS = ticks(RIFT_OPEN);
    public static final int GRABS = ticks(GRAB);
    public static final int CRUSHES = ticks(CRUSH);
    public static final int BURSTS = ticks(BOOM);
    public static final int RETURNS = ticks(DAWN);
    // The circle's radius, in blocks; the overhead portal's height over it.
    public static final double RADIUS = 2.4;
    private static final double OVERHEAD = 13.0;
    private static final double SCALE = 0.68;
    private static final double PORTAL = 2.6;
    // The pointing fingertip and the middle fingertip of a flat hand, in the hand's own frame.
    private static final Vec3 POINT = new Vec3(-0.38, 6.3, 0.0);
    private static final Vec3 TIP = new Vec3(0.0, 6.2, 0.2);
    // Where a held ball sits in the hand's frame, and the biggest it holds at scale 1.
    private static final Vec3 GRIP = new Vec3(0.0, 3.05, 1.25);
    private static final double HOLDS = 1.55;
    // How far the moon's rift stands beside it, and how big it opens, in moon radii.
    private static final double RIFT_AWAY = 2.7;
    public static final double RIFT_SIZE = 1.5;

    private HandCosmos() {
    }

    private static int ticks(double beats) {
        return (int) Math.round(beats * SLOW);
    }

    public static int life() {
        return ticks(LIFE);
    }

    public static int sinks() {
        return ticks(LIFE - 6.0);
    }

    // The ground's frame: along the way it faces and across it, up out of the ground.
    public static Vec3 along(Vec3 facing) {
        return HandGroup.flat(facing);
    }

    public static Vec3 across(Vec3 facing) {
        return Vectors.UP.cross(along(facing)).normalize();
    }

    // How much of the circle the fingertip has drawn at tick t, 0 to 1.
    public static double drawn(double t) {
        return Ease.smoother((t / SLOW - DRAW) / (DRAWN - DRAW));
    }

    // Where the fingertip is on the circle at tick t.
    public static Vec3 tip(Vec3 center, Vec3 facing, double t) {
        double turn = Math.PI * 2.0 * drawn(t) - Math.PI * 0.5;
        return center.add(along(facing).scale(Math.cos(turn) * RADIUS)).add(across(facing).scale(Math.sin(turn)
                * RADIUS)).add(0.0, 0.06, 0.0);
    }

    // How far the hole in the circle is open at tick t, 0 to 1.
    public static double hole(double t) {
        double beat = t / SLOW;
        return Ease.smoother((beat - DRAWN) / (FILL - DRAWN))
                * (1.0 - Ease.smoother((beat - HOLE_SHUT) / (HOLE_GONE - HOLE_SHUT)));
    }

    // How far the diving hand has come down at tick t: its fingertip's height over the hole.
    public static double diverHeight(double t) {
        double beat = t / SLOW;
        double hover = 7.0 + 0.25 * Math.sin(beat * 1.3);
        double cock = 1.4 * Ease.smoother((beat - B_COCK) / (DIVE - B_COCK));
        double dive = (beat - DIVE) / (DIVEN - DIVE);
        double fall = dive <= 0.0 ? 0.0 : 17.5 * dive * dive * (1.4 - 0.4 * Math.min(1.0, dive));
        return hover + cock - fall;
    }

    // The flash where the diving hand meets the stars, 0 before.
    public static double plunge(double t) {
        return Ease.jolt((t - PLUNGES) / 10.0);
    }

    // How dark the sky is turned toward night at tick t, 0 to 1.
    public static double night(double t) {
        double beat = t / SLOW;
        return Ease.smoother((beat - NIGHT_FROM) / (NIGHT_TO - NIGHT_FROM))
                * (1.0 - Ease.smoother((beat - DAWN) / (DAY - DAWN)));
    }

    // How black the sky is drained once the moon is gone, 0 to 1.
    public static double black(double t) {
        double beat = t / SLOW;
        return Ease.smoother((beat - BOOM) / (BLACK - BOOM)) * (1.0 - Ease.smoother((beat - DAWN) / (DAY - DAWN)));
    }

    // Whether the sky's own moon is left out: from the moment the moon of the hand takes its place until it returns.
    public static boolean moonless(double t) {
        double beat = t / SLOW;
        return beat >= MOON_SWAP && beat < DAWN;
    }

    // Whether the hand's moon is drawn whole: from the swap until it bursts.
    public static boolean moonWhole(double t) {
        double beat = t / SLOW;
        return beat >= MOON_SWAP && beat < BOOM;
    }

    // How much of the moon's rift's lips are traced at tick t, 0 to 1: they run round before it opens.
    public static double riftTraced(double t) {
        return Ease.smoother((t / SLOW - RIFT_OPEN + 3.0) / 4.0);
    }

    // How far the moon's rift is open at tick t, 0 to 1.
    public static double rift(double t) {
        double beat = t / SLOW;
        return Ease.smoother((beat - RIFT_OPEN) / (RIFT_WIDE - RIFT_OPEN))
                * (1.0 - Ease.smoother((beat - RIFT_SHUT) / (RIFT_GONE - RIFT_SHUT)));
    }

    // How far the moon is crushed at tick t, 0 to 1: slowly at first, faster and faster.
    public static double crush(double t) {
        double u = Mth.clamp((t / SLOW - CRUSH) / (BOOM - CRUSH), 0.0, 1.0);
        return u * u * (0.6 + 0.4 * u);
    }

    // Ticks since the moon burst, or below 0.
    public static double sinceBurst(double t) {
        return t - BURSTS;
    }

    // The way across the sky beside the moon, the same for every eye: level, square to the moon's way.
    public static Vec3 skySide(Vec3 moonWay) {
        Vec3 side = moonWay.cross(Vectors.UP);
        return side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
    }

    // Where the moon's rift opens: beside the moon, a little nearer the eye.
    public static Vec3 riftCenter(Vec3 moon, double radius, Vec3 eye) {
        Vec3 way = moon.subtract(eye).normalize();
        return moon.add(skySide(way).scale(radius * RIFT_AWAY)).subtract(way.scale(radius * 0.6))
                .add(0.0, radius * 0.5, 0.0);
    }

    // The rift's face: turned half to the eye, half to the moon, so the arm comes out toward both.
    public static Vec3 riftNormal(Vec3 moon, double radius, Vec3 eye) {
        Vec3 rift = riftCenter(moon, radius, eye);
        return moon.subtract(rift).normalize().add(eye.subtract(rift).normalize()).normalize();
    }

    // How big the moon is at tick t, in moon radii from its middle to a face: as big as the sky shows it at the swap,
    // drawn nearer until the hand closes round it.
    public static double moonSize(double t) {
        return 0.5 + 0.35 * Ease.smoother((t / SLOW - MOON_SWAP - 2.0) / (GRAB - MOON_SWAP - 2.0));
    }

    // How far the moon has turned out of its flat face toward the eye at tick t, in radians.
    public static double moonTurn(double t) {
        double beat = t / SLOW;
        double u = Math.max(0.0, beat - MOON_SWAP - 2.0);
        return 0.6 * Ease.smoother(u / 12.0) + 0.006 * u * u / (1.0 + u * 0.05);
    }

    // Where the moon is at tick t: where the sky has it, drawn a little toward the rift once taken.
    public static Vec3 moonAt(Vec3 moon, double radius, Vec3 eye, double t) {
        double beat = t / SLOW;
        double pulled = 0.22 * Ease.smoother((beat - GRAB - 0.5) / 4.0);
        double shake = radius * 0.03 * crush(t) * Math.sin(beat * 9.0);
        return moon.lerp(riftCenter(moon, radius, eye), pulled).add(skySide(moon.subtract(eye).normalize())
                .scale(shake));
    }

    // The hand on the moon: out of the rift, fingers open as it reaches, closed round the moon, squeezing tighter and
    // shaking as it crushes, blown open as the moon bursts, and drawn back in.
    public static HandGroup.Sub moonHand(Vec3 moon, double radius, Vec3 eye, double t) {
        double beat = t / SLOW;
        Vec3 rift = riftCenter(moon, radius, eye);
        Vec3 normal = riftNormal(moon, radius, eye);
        Vec3 held = moonAt(moon, radius, eye, t);
        double scale = radius / HOLDS;
        double reach = Ease.smoother((beat - MOON_REACH) / (GRAB - MOON_REACH))
                * (1.0 - Ease.smoother((beat - LET) / (RIFT_SHUT - LET)));
        double grabbed = Ease.smoother((beat - GRAB + 1.5) / 2.0);
        double crushed = crush(t);
        double blown = Ease.smoother((beat - BOOM) / 0.8);
        double curl = Mth.lerp(blown, Mth.lerp(grabbed, 0.12, 0.5) + 0.38 * crushed, 0.05);
        HandPose pose = HandGroup.fingers(curl, Mth.lerp(grabbed, 0.1, 0.45) + 0.3 * crushed,
                Mth.lerp(grabbed, 0.2, 0.55) + 0.3 * crushed, Mth.lerp(blown, Mth.lerp(grabbed, 0.8, 0.3)
                        - 0.15 * crushed, 1.0));
        Vec3 up = held.subtract(rift).normalize();
        Vec3 toEye = eye.subtract(held).normalize();
        Vec3 palm = HandGroup.square(toEye.cross(up).normalize().add(toEye.scale(0.45)), up);
        HandPose.Place place = HandGroup.reaching(rift, held, up, palm, GRIP, reach, scale);
        return new HandGroup.Sub(pose, place, HandGroup.portalAt(rift, normal, radius * RIFT_SIZE, rift(t)), false);
    }

    // The hand drawing the circle, out of a portal hovering beside it; and the hand diving into it, out of a portal
    // high over it. Each waits in its portal while it is not out.
    public static List<HandGroup.Sub> hands(Vec3 center, Vec3 facing, double t) {
        double beat = t / SLOW;
        Vec3 along = along(facing);
        Vec3 across = across(facing);
        List<HandGroup.Sub> subs = new ArrayList<>();
        Vec3 portalA = center.subtract(along.scale(1.2)).add(across.scale(4.6)).add(0.0, 5.6, 0.0);
        Vec3 tip = tip(center, facing, t);
        Vec3 upA = tip.subtract(portalA).normalize();
        double reachA = Ease.smoother((beat - A_REACH) / 4.0) * (1.0 - Ease.smoother((beat - A_BACK) / 4.0));
        double openA = Ease.smoother((beat - 0.5) / 2.5) * (1.0 - Ease.smoother((beat - A_GONE) / 2.0));
        HandPose point = new HandPose();
        for (int k = 0; k < 4; k++) {
            point.curl[k] = k == 1 ? 0.04 : 0.95;
            point.hook[k] = k == 1 ? 0.0 : 0.1;
        }
        point.curl[4] = 0.85;
        point.hook[4] = 0.2;
        point.spread = 0.05;
        Vec3 palmA = HandGroup.square(center.subtract(tip).add(0.0, -1.0, 0.0), upA);
        subs.add(new HandGroup.Sub(point, HandGroup.reaching(portalA, tip, upA, palmA, POINT, reachA, SCALE),
                HandGroup.portalAt(portalA, upA, PORTAL * SCALE * 1.1, openA), false));
        Vec3 portalB = center.add(0.0, OVERHEAD, 0.0);
        Vec3 down = new Vec3(0.0, -1.0, 0.0);
        Vec3 goal = center.add(0.0, diverHeight(t), 0.0);
        double reachB = Ease.smoother((beat - B_OUT) / 3.0);
        double openB = Ease.smoother((beat - B_OUT + 2.0) / 2.0) * (1.0 - Ease.smoother((beat - DIVEN - 1.0) / 2.0));
        HandPose flat = HandGroup.fingers(0.05, 0.0, 0.3, 0.0);
        subs.add(new HandGroup.Sub(flat, HandGroup.reaching(portalB, goal, down, along.scale(-1.0), TIP, reachB,
                SCALE), HandGroup.portalAt(portalB, down, PORTAL * SCALE * 1.15, openB), true));
        return subs;
    }
}
