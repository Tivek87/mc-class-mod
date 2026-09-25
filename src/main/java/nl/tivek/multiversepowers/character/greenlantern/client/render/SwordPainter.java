package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The sword and shield of the construct wheel, as hard light: solid like every construct, drawn wherever the hands that
 * hold them are (see {@link SwordArms}).
 * <ul>
 * <li>The sword: a broad knight's sword, its blade bevelled to both edges with a glowing channel down the middle of each
 * face and the lantern emblem at its root, a crossguard curving towards the blade with a knob at either end and the
 * emblem in its middle, a wrapped grip and a round pommel with a gem.</li>
 * <li>The shield: a big heater shield, flat on top and coming to a point below, bowed out to the front, with a round
 * rim, a raised border inside it, a ring of rivets, a boss in its middle with the lantern emblem round it. On its back
 * the forearm lies across a pad, through a strap by the elbow, and the fist closes round a grip square to it; ribs of
 * light run out from the back of the boss.</li>
 * </ul>
 * Both grow out of the ring's light, white-hot at first: the sword out of the fist, hilt first and then the blade running
 * out to its point; the shield out of its boss, its body spreading out from there, then a bright edge running round it
 * from the middle of its top down both sides with the rim solid behind it and the rivets popping up after it, the two
 * ends meeting in a flash at its point, and last the pad, strap and grip closing on its back. They break into solid
 * pieces when they are put away. A swung blade leaves a streak of light in the air behind it, and a blade banged on the
 * face of the shield throws sparks of light: light, not constructs.
 */
public final class SwordPainter {
    /** Where the fist grips the shield, on its back: across it (towards its right) and behind its face, at scale 1. */
    public static final double GRIP_X = 0.15;
    public static final double GRIP_Z = -0.045;
    // The outline of the blade across it, the edges on top and below: a flat middle bevelled off to both edges.
    private static final double[] BLADE = { 0.0, 1.0, -0.35, 0.72, -1.0, 0.25, -1.0, -0.25, -0.35, -0.72, 0.0, -1.0,
            0.35, -0.72, 1.0, -0.25, 1.0, 0.25, 0.35, 0.72 };
    /** How far the tip of the blade is from the middle of the grip, and where the blade begins, at scale 1. */
    public static final double TIP = 1.4;
    static final double BLADE_FROM = 0.2;
    /** The sword, its grip round the middle: x across its flat, y towards its top edge, z to its tip. */
    private static final ConstructPainter.Shape SWORD = ConstructPainter.Shape.of(sword());
    // How far the face of the shield stands out of its middle plane at its rim and how much further it bows out in its
    // middle, and how thick its rim is.
    private static final double FACE_FRONT = 0.03;
    private static final double BULGE = 0.07;
    private static final double RIM_THICK = 0.034;
    // The shield in parts (x across it, y up, z out of its face), so it can take shape part by part: its body, its
    // boss, the pad, strap and grip on its back (round the grip), a rivet and where every rivet sits, and the rim: its
    // points from the middle of its top round and back to it (that one is the fifth point of the outline), how far
    // round it each is, and a piece of it for every stretch between two of them.
    private static final Mesh[] BODY = body();
    private static final Mesh[] BOSS = boss();
    private static final Mesh[] BACK = back();
    private static final Mesh RIVET = Mesh.ball(8, 5, 0.019, 1.5);
    private static final Vec3[] RIVETS = rivets();
    private static final int RIM_TOP = 4;
    private static final Vec3[] RIM = rim();
    private static final double[] RIM_ALONG = along(RIM);
    private static final Mesh[] RIM_EDGES = edges(RIM);
    /** The whole shield round its middle: x across it, y up, z out of its face. */
    private static final ConstructPainter.Shape SHIELD = ConstructPainter.Shape.of(shield());
    private static final ConstructPainter.Shape SHIELD_BODY = ConstructPainter.Shape.of(BODY);
    private static final ConstructPainter.Shape SHIELD_BOSS = ConstructPainter.Shape.of(BOSS);
    private static final ConstructPainter.Shape SHIELD_BACK = ConstructPainter.Shape.of(BACK);
    // How the shield takes shape, as parts of the way from the ring's light reaching it (0) to its strap closing (1):
    // the boss forms, the body spreads out from it (a little too far, and back), a bright edge runs round from the
    // middle of the top down both sides, leaving the rim behind it and the rivets popping up after it, the two meet at
    // the point below in a flash, and last the pad, strap and grip close on its back.
    private static final double BOSS_UNTIL = 0.2;
    private static final double SPREAD_FROM = 0.06;
    private static final double SPREAD_TOP = 0.56;
    private static final double SPREAD_UNTIL = 0.76;
    private static final double SPREAD_OVER = 1.06;
    private static final double RIM_FROM = 0.4;
    private static final double RIM_UNTIL = 0.88;
    private static final double BACK_FROM = 0.82;
    // How far round the rim a rivet takes to pop up once the edge has passed it (blocks at scale 1).
    private static final double RIVET_POP = 0.15;
    // How long the clang of the blade on the shield lasts, in ticks.
    private static final double CLANG_TICKS = 4.0;
    // How far the pieces fly when they break up, next to how far pieces usually do.
    private static final double FLING = 1.3;

    private SwordPainter() {
    }

    private static Mesh[] sword() {
        List<Mesh> parts = new ArrayList<>();
        // The blade, bevelled to both edges and narrowing into its point.
        parts.add(Mesh.sweep(1.05, BLADE, new double[] { BLADE_FROM, 0.034, 0.1, 0.0 },
                new double[] { 0.32, 0.033, 0.098, 0.0 }, new double[] { 0.9, 0.028, 0.088, 0.0 },
                new double[] { 1.2, 0.022, 0.07, 0.0 }, new double[] { 1.33, 0.012, 0.036, 0.0 },
                new double[] { TIP, 0.0, 0.0, 0.0 }));
        // The glowing channel down the middle of each face, and the emblem at the root of the blade.
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.box(side * 0.03, -0.014, 0.36, side * 0.038, 0.014, 1.12, 1.95));
            parts.add(Mesh.torus(14, 4, 0.042, 0.009, 1.9).alongX().moved(side * 0.034, 0.0, 0.27));
        }
        // The crossguard, curving towards the blade, with a knob at either end and the emblem in its middle.
        parts.add(Mesh.tube(false, 8, 0.034, 1.15, new Vec3(0.0, -0.31, 0.2), new Vec3(0.0, -0.22, 0.155),
                new Vec3(0.0, -0.1, 0.14), new Vec3(0.0, 0.1, 0.14), new Vec3(0.0, 0.22, 0.155),
                new Vec3(0.0, 0.31, 0.2)));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.ball(10, 6, 0.046, 1.4).moved(0.0, side * 0.325, 0.21));
        }
        parts.add(Mesh.box(-0.05, -0.07, 0.1, 0.05, 0.07, 0.2, 1.1));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.torus(16, 5, 0.052, 0.013, 1.85).alongX().moved(side * 0.052, 0.0, 0.15));
        }
        // The grip, wrapped, and the pommel with its gem.
        parts.add(Mesh.cylinder(10, 0.034, -0.14, 0.1, 0.92).alongZ());
        for (double z : new double[] { -0.1, -0.04, 0.02, 0.08 }) {
            parts.add(Mesh.torus(12, 4, 0.036, 0.008, 1.25).alongZ().moved(0.0, 0.0, z));
        }
        parts.add(Mesh.ball(14, 8, 0.062, 1.3).scaled(1.0, 1.0, 0.8).moved(0.0, 0.0, -0.19));
        parts.add(Mesh.ball(10, 6, 0.028, 2.0).moved(0.0, 0.0, -0.24));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * How far the face of the shield stands out of its middle plane at a point of it (at scale 1: {@code x} across it,
     * {@code y} up): the dish of its body bows out most in its middle and less and less towards its rim (see
     * {@link #body}).
     */
    public static double faceOut(double x, double y) {
        double[] outline = outline(1.0);
        int n = outline.length / 2;
        double cx = 0.0;
        double cy = 0.0;
        for (int i = 0; i < n; i++) {
            cx += outline[2 * i] / n;
            cy += outline[2 * i + 1] / n;
        }
        // How far out from the middle of the dish towards its rim the point is: where the way out through it crosses
        // the outline.
        double dx = x - cx;
        double dy = y - cy;
        double reach = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double ax = outline[2 * i] - cx;
            double ay = outline[2 * i + 1] - cy;
            double ex = outline[2 * j] - cx - ax;
            double ey = outline[2 * j + 1] - cy - ay;
            double across = dx * ey - dy * ex;
            if (Math.abs(across) < 1.0E-12) {
                continue;
            }
            double s = (ax * ey - ay * ex) / across;
            double u = (ax * dy - ay * dx) / across;
            if (s > 0.0 && u >= 0.0 && u <= 1.0) {
                reach = Math.min(reach, s);
            }
        }
        double f = reach == Double.MAX_VALUE ? 0.0 : Math.min(1.0, 1.0 / reach);
        return FACE_FRONT + BULGE * (1.0 - f * f);
    }

    /** The outline of the shield, seen from the front: an arched top, straight sides, curving in to a point below. */
    private static double[] outline(double scale) {
        List<double[]> points = new ArrayList<>();
        int top = 8;
        for (int i = 0; i <= top; i++) {
            double x = -0.46 + 0.92 * i / top;
            points.add(new double[] { x, 0.52 + 0.04 * (1.0 - (x / 0.46) * (x / 0.46)) });
        }
        points.add(new double[] { 0.46, 0.26 });
        double[][] curve = { { 0.46, 0.04 }, { 0.44, -0.1 }, { 0.4, -0.23 }, { 0.33, -0.37 }, { 0.24, -0.5 },
                { 0.13, -0.61 }, { 0.0, -0.7 } };
        for (double[] point : curve) {
            points.add(point);
        }
        for (int i = curve.length - 2; i >= 0; i--) {
            points.add(new double[] { -curve[i][0], curve[i][1] });
        }
        points.add(new double[] { -0.46, 0.26 });
        double[] outline = new double[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            outline[2 * i] = points.get(i)[0] * scale;
            outline[2 * i + 1] = 0.02 + (points.get(i)[1] - 0.02) * scale;
        }
        return outline;
    }

    /** The whole shield, as it stands once it has taken shape: every part below, the rim all round. */
    private static Mesh[] shield() {
        List<Mesh> parts = new ArrayList<>(List.of(BODY));
        // The round rim all round it.
        parts.add(Mesh.tube(true, 6, RIM_THICK, 1.4, ring(outline(1.0), 0.0)));
        for (Vec3 rivet : RIVETS) {
            parts.add(RIVET.moved(rivet.x, rivet.y, rivet.z));
        }
        parts.addAll(List.of(BOSS));
        for (Mesh part : BACK) {
            parts.add(part.moved(GRIP_X, 0.0, 0.0));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * The body of the shield: its face bowed out to the front, the raised border inside the rim, the lantern emblem
     * round the boss (a ring with a bar over and under it), and on its back the ribs of light running out from the boss
     * all round over the hollow.
     */
    private static Mesh[] body() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.dish(5, -FACE_FRONT, FACE_FRONT, BULGE, 1.0, outline(1.0)));
        parts.add(Mesh.tube(true, 5, 0.013, 1.55, ring(outline(0.8), FACE_FRONT + BULGE * (1.0 - 0.64) + 0.006)));
        parts.add(Mesh.torus(28, 6, 0.2, 0.028, 1.75).alongZ().moved(0.0, 0.02, 0.095));
        parts.add(Mesh.box(-0.25, 0.235, 0.075, 0.25, 0.275, 0.105, 1.75));
        parts.add(Mesh.box(-0.25, -0.235, 0.075, 0.25, -0.195, 0.105, 1.75));
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI * 2.0 * (k + 0.5) / 8.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            parts.add(Mesh.tube(false, 4, 0.009, 1.9, new Vec3(cos * 0.13, 0.02 + sin * 0.13, 0.034),
                    new Vec3(cos * 0.36, 0.02 + sin * 0.4, 0.004)));
        }
        return parts.toArray(Mesh[]::new);
    }

    /** The boss in the middle of the shield, and its back. */
    private static Mesh[] boss() {
        return new Mesh[] { Mesh.ball(14, 8, 0.1, 1.3).scaled(1.0, 1.0, 0.55).moved(0.0, 0.02, 0.095),
                Mesh.torus(20, 5, 0.09, 0.018, 1.7).alongZ().moved(0.0, 0.02, 0.035) };
    }

    /**
     * On its back (hollow, as the body bows out to the front), round the grip (at x 0 here): the pad the forearm lies
     * on, across the shield from the elbow on its left to the fist on its right, the strap over the forearm by the
     * elbow, and the grip for the fist, square to the forearm.
     */
    private static Mesh[] back() {
        return new Mesh[] { Mesh.box(-0.3 - GRIP_X, -0.06, 0.008, 0.06 - GRIP_X, 0.06, 0.036, 0.95),
                Mesh.tube(false, 6, 0.016, 1.3, new Vec3(-0.2 - GRIP_X, 0.085, 0.027),
                        new Vec3(-0.2 - GRIP_X, 0.08, -0.05), new Vec3(-0.2 - GRIP_X, -0.08, -0.05),
                        new Vec3(-0.2 - GRIP_X, -0.085, 0.027)),
                Mesh.tube(false, 8, 0.02, 1.25, new Vec3(0.0, 0.105, 0.032), new Vec3(0.0, 0.09, GRIP_Z),
                        new Vec3(0.0, -0.09, GRIP_Z), new Vec3(0.0, -0.105, 0.032)) };
    }

    /** Where the rivets sit, a ring of them between the rim and the border: by every other point of the outline. */
    private static Vec3[] rivets() {
        double[] outline = outline(0.9);
        List<Vec3> rivets = new ArrayList<>();
        for (int i = 0; i < outline.length / 2; i += 2) {
            rivets.add(new Vec3(outline[2 * i], outline[2 * i + 1], FACE_FRONT + BULGE * (1.0 - 0.81) + 0.004));
        }
        return rivets.toArray(Vec3[]::new);
    }

    /** The points of the rim, from the middle of the top round the shield and back to it. */
    private static Vec3[] rim() {
        Vec3[] ring = ring(outline(1.0), 0.0);
        Vec3[] rim = new Vec3[ring.length + 1];
        for (int i = 0; i <= ring.length; i++) {
            rim[i] = ring[(RIM_TOP + i) % ring.length];
        }
        return rim;
    }

    /** How far round the rim every point of it is, from the middle of the top. */
    private static double[] along(Vec3[] rim) {
        double[] along = new double[rim.length];
        for (int i = 1; i < rim.length; i++) {
            along[i] = along[i - 1] + rim[i].distanceTo(rim[i - 1]);
        }
        return along;
    }

    /** The rim, a piece for every stretch of it between two points. */
    private static Mesh[] edges(Vec3[] rim) {
        Mesh[] edges = new Mesh[rim.length - 1];
        for (int i = 0; i < edges.length; i++) {
            edges[i] = Mesh.tube(false, 6, RIM_THICK, 1.4, rim[i], rim[i + 1]);
        }
        return edges;
    }

    /** The points of an outline as a closed ring at height {@code z} out of the shield's face. */
    private static Vec3[] ring(double[] outline, double z) {
        Vec3[] ring = new Vec3[outline.length / 2];
        for (int i = 0; i < ring.length; i++) {
            ring[i] = new Vec3(outline[2 * i], outline[2 * i + 1], z);
        }
        return ring;
    }

    /**
     * The sword, its grip at {@code grip}, its blade along {@code forward} and its top edge towards {@code edge}.
     *
     * @param grown how far it has grown out of the ring's light, 0 to 1
     * @param apart 0 while whole; above that it is breaking up, gone at 1
     */
    public static void sword(LanternPainter painter, Vec3 grip, Vec3 forward, Vec3 edge, double scale, double grown,
            double apart) {
        if (grown <= 0.01) {
            return;
        }
        ConstructPainter.Frame whole = frame(grip, forward, edge, scale);
        // Growing: the hilt takes its full width first, and the blade runs out of it to its point. Broken up while it
        // grows, only what had grown flies apart.
        double wide = Math.min(1.0, grown * 1.8);
        ConstructPainter.Frame frame = grown < 1.0 ? whole.stretched(wide, wide, grown) : whole;
        if (apart > 0.0) {
            painter.fling(FLING);
            painter.shattered(SWORD, frame, apart, 1.2);
            painter.fling(1.0);
            return;
        }
        painter.glare(0.75 * (1.0 - grown));
        painter.ambient(0.12);
        painter.shape(SWORD, frame, 1.0, 1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
        if (grown < 1.0) {
            // The light of the ring it grows out of, burning at the fist and at the point as it runs out.
            painter.flare(grip, 0.35 * scale * (1.0 - grown), 1.0 - grown);
            painter.flare(grip.add(forward.normalize().scale(TIP * scale * grown)), 0.2 * scale, 1.0 - grown);
        }
    }

    /**
     * The shield, its middle at {@code center}, its face towards {@code face} and its top towards {@code up}.
     *
     * @param grown how far it has taken shape out of the ring's light, 0 to 1 (see {@link #BOSS_UNTIL} and on)
     * @param apart 0 while whole; above that it is breaking up, gone at 1
     */
    public static void shield(LanternPainter painter, Vec3 center, Vec3 face, Vec3 up, double scale, double grown,
            double apart) {
        if (grown <= 0.0) {
            return;
        }
        ConstructPainter.Frame whole = frame(center, face, up, scale);
        double spread = spread(grown);
        ConstructPainter.Frame frame = grown < 1.0 ? whole.stretched(spread, spread, 1.0) : whole;
        double half = RIM_ALONG[RIM_ALONG.length - 1] / 2.0;
        double run = (grown - RIM_FROM) / (RIM_UNTIL - RIM_FROM);
        double head = half * Mth.clamp(run, 0.0, 1.0);
        if (apart > 0.0) {
            // Broken up while it takes shape, only what had formed flies apart.
            painter.fling(FLING);
            if (grown >= 1.0) {
                painter.shattered(SHIELD, frame, apart, 1.2);
            } else {
                formed(whole, frame, grown, run, (shape, at, seed) -> painter.shattered(shape, at, apart, 1.2, seed));
            }
            painter.fling(1.0);
            return;
        }
        painter.ambient(0.12);
        if (grown >= 1.0) {
            painter.shape(SHIELD, frame, 1.0, 1.0);
            painter.ambient(0.0);
            return;
        }
        painter.glare(0.75 * (1.0 - grown));
        formed(whole, frame, grown, run, (shape, at, seed) -> painter.shape(shape, at, 1.0, 1.0));
        painter.glare(0.0);
        painter.ambient(0.0);
        // The light it takes shape out of: burning at the boss as it forms, running round as the two ends of the bright
        // edge, and flashing where they meet below.
        painter.flare(whole.at(0.0, 0.02, 0.1), 0.3 * scale * (1.0 - grown), Math.max(0.0, 1.0 - grown * 2.0));
        if (run > 0.0 && run < 1.0) {
            edgeLight(painter, frame, head, scale);
            edgeLight(painter, frame, 2.0 * half - head, scale);
        }
        if (run >= 1.0) {
            double flash = 1.0 - (grown - RIM_UNTIL) / (1.0 - RIM_UNTIL);
            painter.flare(frame.at(0.0, -0.7, 0.0), 0.25 * scale * (1.5 - flash), flash);
        }
    }

    /** Something done with one part of a shield taking shape, at its own frame: drawn, or broken up. */
    @FunctionalInterface
    private interface Part {
        void at(ConstructPainter.Shape shape, ConstructPainter.Frame frame, int seed);
    }

    /**
     * Every part of a shield that has formed so far, taking shape {@code grown} of the way ({@code run} of the way the
     * bright edge has run round its rim): the boss, the body spread out from it, the rim behind the two ends of the
     * bright edge running round from the middle of the top, the rivets popped up after them, and the pad, strap and
     * grip on its back. Each comes with a piece number of its own, for the way it flies off when it breaks up.
     */
    private static void formed(ConstructPainter.Frame whole, ConstructPainter.Frame frame, double grown, double run,
            Part part) {
        double boss = Ease.backOut(grown / BOSS_UNTIL);
        part.at(SHIELD_BOSS, whole.stretched(boss, boss, boss), 0);
        if (grown > SPREAD_FROM) {
            part.at(SHIELD_BODY, frame, 8);
        }
        double half = RIM_ALONG[RIM_ALONG.length - 1] / 2.0;
        double head = half * Mth.clamp(run, 0.0, 1.0);
        if (head > 0.0) {
            part.at(ConstructPainter.Shape.of(rim(head)), frame, 24);
        }
        for (int i = 0; i < RIVETS.length; i++) {
            double popped = Ease.backOut((half * run - rivetRound(i)) / RIVET_POP);
            if (popped > 0.0) {
                Vec3 at = RIVETS[i];
                part.at(ConstructPainter.Shape.of(RIVET), frame.moved(at.x, at.y, at.z).stretched(popped, popped,
                        popped), 60 + i);
            }
        }
        if (grown > BACK_FROM) {
            double closed = Ease.backOut((grown - BACK_FROM) / (1.0 - BACK_FROM));
            part.at(SHIELD_BACK, frame.moved(GRIP_X, 0.0, 0.0).stretched(closed, closed, closed), 90);
        }
    }

    /** How far the body of the shield has spread out from its boss, taking shape {@code grown} of the way. */
    private static double spread(double grown) {
        if (grown >= SPREAD_UNTIL) {
            return 1.0;
        }
        if (grown >= SPREAD_TOP) {
            return Ease.hermite(SPREAD_OVER, 0.0, 1.0, 0.0, (grown - SPREAD_TOP) / (SPREAD_UNTIL - SPREAD_TOP));
        }
        double u = Math.max(0.0, (grown - SPREAD_FROM) / (SPREAD_TOP - SPREAD_FROM));
        return Math.max(1.0E-3, Ease.hermite(0.12, 2.4, SPREAD_OVER, 0.0, u));
    }

    /** How far round the rim (from the middle of its top, either way) rivet {@code i} sits. */
    private static double rivetRound(int i) {
        double round = RIM_ALONG[(2 * i - RIM_TOP + RIM.length - 1) % (RIM.length - 1)];
        return Math.min(round, RIM_ALONG[RIM_ALONG.length - 1] - round);
    }

    /** The pieces of the rim of the shield as far as {@code head} round from the middle of its top, both ways. */
    private static Mesh[] rim(double head) {
        double length = RIM_ALONG[RIM_ALONG.length - 1];
        double tail = length - head;
        List<Mesh> pieces = new ArrayList<>();
        for (int i = 0; i < RIM_EDGES.length; i++) {
            double from = RIM_ALONG[i];
            double to = RIM_ALONG[i + 1];
            if (to <= head || from >= tail) {
                pieces.add(RIM_EDGES[i]);
            } else if (from < head) {
                pieces.add(Mesh.tube(false, 6, RIM_THICK, 1.4, RIM[i], rimAt(head)));
            } else if (to > tail) {
                pieces.add(Mesh.tube(false, 6, RIM_THICK, 1.4, rimAt(tail), RIM[i + 1]));
            }
        }
        return pieces.toArray(Mesh[]::new);
    }

    /** The point of the rim {@code round} round it from the middle of its top (in the shield's own blocks). */
    private static Vec3 rimAt(double round) {
        int i = 0;
        while (i + 2 < RIM.length && RIM_ALONG[i + 1] < round) {
            i++;
        }
        double stretch = Math.max(1.0E-6, RIM_ALONG[i + 1] - RIM_ALONG[i]);
        return RIM[i].lerp(RIM[i + 1], Mth.clamp((round - RIM_ALONG[i]) / stretch, 0.0, 1.0));
    }

    /** One end of the bright edge running round the rim as the shield takes shape: a spark, and a streak behind it. */
    private static void edgeLight(LanternPainter painter, ConstructPainter.Frame frame, double round, double scale) {
        double length = RIM_ALONG[RIM_ALONG.length - 1];
        double back = round <= length / 2.0 ? -1.0 : 1.0;
        Vec3 head = rimAt(round);
        Vec3 at = frame.at(head.x, head.y, head.z);
        painter.flare(at, 0.12 * scale, 1.0);
        Vec3 last = at;
        for (int k = 1; k <= 4; k++) {
            Vec3 point = rimAt(Mth.clamp(round + back * 0.07 * k, 0.0, length));
            Vec3 next = frame.at(point.x, point.y, point.z);
            painter.edge(last, next, 0.03 * scale, 1.0 - 0.22 * k);
            last = next;
        }
    }

    /**
     * The clang of the blade on the shield, {@code since} ticks after it struck at {@code at}: a hot flash, and sparks of
     * light spraying off it, along the blade ({@code along}) either way and up away from the shield ({@code off}).
     * Light, gone in a few ticks.
     */
    public static void clang(LanternPainter painter, Vec3 at, Vec3 along, Vec3 off, double since, double scale) {
        if (since < 0.0 || since > CLANG_TICKS) {
            return;
        }
        double u = since / CLANG_TICKS;
        double fade = (1.0 - u) * (1.0 - u);
        painter.flare(at, 0.2 * scale * (1.0 + u), fade);
        for (int k = 0; k < 7; k++) {
            double angle = (k / 6.0 - 0.5) * 2.4;
            Vec3 way = along.scale(Math.sin(angle)).add(off.scale(Math.cos(angle))).normalize();
            double reach = scale * (0.08 + 0.3 * Math.sqrt(u)) * (0.7 + 0.075 * ((k * 3) % 5));
            painter.edge(at.add(way.scale(reach * 0.5)), at.add(way.scale(reach)), 0.014 * scale, fade);
        }
    }

    /**
     * The streak a swung blade leaves in the air: a sheet of light between where its tip and the root of its blade were
     * over the last moments, newest first, fading out behind it, with a bright line where the tip ran.
     *
     * @param strength how strongly it shows, 0 to 1
     */
    public static void trail(LanternPainter painter, List<Vec3> tips, List<Vec3> roots, double strength) {
        int n = Math.min(tips.size(), roots.size());
        for (int i = 0; i + 1 < n; i++) {
            double a = strength * (1.0 - (double) i / (n - 1));
            double b = strength * (1.0 - (double) (i + 1) / (n - 1));
            painter.sheet(roots.get(i), tips.get(i), tips.get(i + 1), roots.get(i + 1), 0.15 * a, a, b, 0.15 * b);
            painter.edge(tips.get(i), tips.get(i + 1), 0.045 * (0.4 + 0.6 * a / Math.max(strength, 1.0E-3)), 0.9 * a);
        }
    }

    /** A frame at {@code center} facing {@code forward} with its up towards {@code up}, the way every construct stands. */
    private static ConstructPainter.Frame frame(Vec3 center, Vec3 forward, Vec3 up, double scale) {
        Vec3 ahead = forward.normalize();
        Vec3 top = up.subtract(ahead.scale(up.dot(ahead)));
        top = top.lengthSqr() < 1.0E-8 ? ConstructPainter.Frame.of(center, ahead, Vectors.UP, 1.0).up()
                : top.normalize();
        return new ConstructPainter.Frame(center, ahead.cross(top).normalize(), top, ahead, scale);
    }
}
