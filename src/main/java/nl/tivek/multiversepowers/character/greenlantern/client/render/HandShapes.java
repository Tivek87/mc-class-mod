package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;

/**
 * The shapes of the Giant Hands (see {@link HandPainter}): the hand, its forearm and cuff, each joint of its fingers
 * and thumb, and the battle axe of a pair; and how a hand's fingers are laid out on it.
 */
final class HandShapes {
    // The four fingers (index, middle, ring, little): where each one's knuckle is on the hand (x, y), how thick it is at
    // its root, how long each of its three joints is, and how far it spreads out to the side when the hand is open.
    static final double[][] KNUCKLES = { { -1.12, 3.05 }, { -0.38, 3.18 }, { 0.38, 3.1 }, { 1.1, 2.88 } };
    private static final double[] THICK = { 0.36, 0.38, 0.35, 0.3 };
    static final double[][] JOINTS = { { 1.25, 0.85, 0.72 }, { 1.38, 0.95, 0.78 }, { 1.3, 0.9, 0.72 },
            { 1.0, 0.68, 0.6 } };
    static final double[] SPREAD = { 0.13, 0.03, -0.07, -0.18 };
    // How far each joint of a finger bends in a fist, in radians, and how far its two outer joints bend more when it is
    // hooked all the way.
    static final double[] BENDS = { 1.5, 1.7, 1.25 };
    static final double[] HOOKS = { 1.35, 1.1 };
    // The thumb: where it grows out of the hand, how long and thick its three joints are, and how far its two outer
    // joints bend more when it is hooked all the way.
    static final Vec3 THUMB_ROOT = new Vec3(-1.2, 0.95, 0.35);
    static final double[] THUMB = { 1.25, 1.0, 0.82 };
    static final double[] THUMB_THICK = { 0.44, 0.4, 0.36 };
    static final double[] THUMB_HOOKS = { 0.9, 0.8 };
    /** The palm, the back of the hand and everything on them. */
    static final ConstructPainter.Shape HAND = ConstructPainter.Shape.of(hand());
    /** The cuff round the wrist and the forearm. */
    static final ConstructPainter.Shape ARM = ConstructPainter.Shape.of(arm());
    /** The same apart, for a pair's hand, whose wrist may twist: the cuff alone, and the forearm without it. */
    static final ConstructPainter.Shape CUFF = ConstructPainter.Shape.of(cuff());
    static final ConstructPainter.Shape FOREARM = ConstructPainter.Shape.of(forearm());
    /** Each joint of each finger, standing up along y from its root, its knuckle round its root. */
    static final ConstructPainter.Shape[][] FINGERS = fingers();
    /** The root joint of the middle finger of a left hand: bare, without the ring. */
    static final ConstructPainter.Shape MIDDLE_BARE = ConstructPainter.Shape.of(joint(JOINTS[1][0], THICK[1],
            THICK[1] * 0.9, false));
    /** The three joints of the thumb. */
    static final ConstructPainter.Shape[] THUMBS = thumb();
    // The outline of the axe's blade (see blade), and the three steps it is built of from its edge in: how thick each
    // is either way, and the point each one's outline is drawn in towards (y from the middle of the head, z out from
    // the haft), to what share of the whole.
    static final double[] BLADE = blade(HandDuo.BLADE_HALF, HandDuo.EDGE_OUT, HandDuo.HAFT_RADIUS);
    static final double[] BLADE_STEPS = { 0.06, 0.15, 0.26 };
    static final double[][] BLADE_TOWARDS = { { 0.0, 0.0 },
            { 0.0, 0.3 * HandDuo.HAFT_RADIUS + 0.25 * HandDuo.EDGE_OUT }, { 0.0, 0.3 * HandDuo.HAFT_RADIUS } };
    static final double[] BLADE_SHARES = { 1.0, 0.8, 0.55 };
    /** The pair's battle axe, along y from the end of its pommel, the edge of its blade towards +z. */
    static final ConstructPainter.Shape AXE = ConstructPainter.Shape.of(axe());

    private HandShapes() {
    }

    private static Mesh[] hand() {
        List<Mesh> parts = new ArrayList<>();
        // The palm: a thick, rounded slab, wider at the knuckles, with the pad of the thumb and the one under the little
        // finger bulging from it.
        double[] outline = { -1.05, -1.5, 1.05, -1.5, 1.45, -0.9, 1.6, 0.2, 1.62, 1.2, 1.35, 1.55, 0.5, 1.68, -0.5,
                1.7, -1.35, 1.6, -1.62, 1.1, -1.6, -0.3, -1.4, -1.1 };
        parts.add(Mesh.sweep(1.0, outline, new double[] { 0.52, 0.88, 0.92, 0.0 },
                new double[] { 0.36, 1.0, 1.0, 0.0 }, new double[] { -0.36, 1.0, 1.0, 0.0 },
                new double[] { -0.55, 0.9, 0.94, 0.0 }).moved(0.0, 1.5, 0.0));
        parts.add(Mesh.ball(14, 10, 0.78, 1.0).scaled(1.0, 1.35, 0.62).moved(-0.95, 1.05, 0.3));
        parts.add(Mesh.ball(12, 8, 0.6, 1.0).scaled(0.8, 1.55, 0.5).moved(1.15, 1.35, 0.26));
        // Glowing creases across the palm, as a real hand has.
        parts.add(Mesh.tube(false, 5, 0.035, 1.6, new Vec3(1.45, 2.55, 0.53), new Vec3(0.4, 2.72, 0.54),
                new Vec3(-0.55, 2.62, 0.53)));
        parts.add(Mesh.tube(false, 5, 0.035, 1.6, new Vec3(-1.45, 2.3, 0.52), new Vec3(-0.2, 2.1, 0.54),
                new Vec3(1.15, 1.85, 0.53)));
        parts.add(Mesh.tube(false, 5, 0.035, 1.6, new Vec3(-1.35, 2.35, 0.52), new Vec3(-0.45, 1.6, 0.55),
                new Vec3(-0.35, 0.45, 0.53)));
        // On its back: a knuckle for every finger, a glowing tendon running up to each, and the lantern emblem.
        for (int k = 0; k < 4; k++) {
            double x = KNUCKLES[k][0];
            double y = KNUCKLES[k][1];
            parts.add(Mesh.ball(10, 6, THICK[k] * 0.95, 1.05).scaled(1.0, 0.9, 0.7).moved(x, y - 0.1, -0.3));
            parts.add(Mesh.tube(false, 4, 0.04, 1.55, new Vec3(x * 0.35, 0.35, -0.53), new Vec3(x * 0.8, 1.9, -0.56),
                    new Vec3(x, y - 0.35, -0.5)));
        }
        parts.add(emblem(0.55).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 1.35, -0.58));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * The glowing cuff round the wrist, and the forearm back from it (down into the ground, or into its portal), a
     * glowing ring round it like the rim of a gauntlet and glowing seams along it.
     */
    private static Mesh[] arm() {
        List<Mesh> parts = new ArrayList<>(List.of(cuff()));
        parts.addAll(List.of(forearm()));
        return parts.toArray(Mesh[]::new);
    }

    /** The glowing cuff round the wrist. */
    private static Mesh[] cuff() {
        return new Mesh[] { Mesh.torus(24, 6, 1.12, 0.15, 1.7).scaled(1.2, 1.0, 0.76).moved(0.0, 0.12, 0.0) };
    }

    /** The forearm back from the wrist, its gauntlet ring and its seams. */
    private static Mesh[] forearm() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(18, 1.0, 0.0, -14.0, 1.55, -14.0, 1.62, -9.0, 1.55, -5.0, 1.3, -2.2, 1.12, -0.6, 1.06,
                0.35, 0.0, 0.35).scaled(1.18, 1.0, 0.82));
        parts.add(Mesh.torus(24, 5, 1.36, 0.12, 1.6).scaled(1.18, 1.0, 0.82).moved(0.0, -2.0, 0.0));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.tube(false, 4, 0.05, 1.5, new Vec3(side * 0.6, -2.3, -1.18),
                    new Vec3(side * 0.66, -6.0, -1.3), new Vec3(side * 0.7, -10.0, -1.33)));
        }
        return parts.toArray(Mesh[]::new);
    }

    /** The lantern emblem lying flat round y: a ring with a bar over and under its middle. */
    private static Mesh emblem(double radius) {
        double bar = radius * 0.24;
        return Mesh.merged(Mesh.torus(24, 5, radius, radius * 0.12, 1.75),
                Mesh.box(-radius, -0.05, bar * 0.9, radius, 0.06, bar * 1.6, 1.75),
                Mesh.box(-radius, -0.05, -bar * 1.6, radius, 0.06, -bar * 0.9, 1.75));
    }

    /** One joint of a finger along y, {@code length} long, {@code root} thick at its root and thinning to its end. */
    private static Mesh joint(double length, double root, double end, boolean tip) {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(12, 1.0, 0.0, 0.0, root, 0.0, root * 0.97, length * 0.5, end, length, 0.0, length)
                .scaled(1.0, 1.0, 0.86));
        parts.add(Mesh.ball(10, 6, root * 1.02, 1.0).scaled(1.0, 1.0, 0.86));
        if (tip) {
            parts.add(Mesh.ball(10, 6, end, 1.0).scaled(1.0, 1.0, 0.86).moved(0.0, length, 0.0));
            // The nail, on its back.
            parts.add(Mesh.box(-end * 0.62, length - 0.5, -end * 0.9, end * 0.62, length + 0.05, -end * 0.78, 1.4));
        }
        return Mesh.merged(parts.toArray(Mesh[]::new));
    }

    private static ConstructPainter.Shape[][] fingers() {
        ConstructPainter.Shape[][] fingers = new ConstructPainter.Shape[4][3];
        for (int k = 0; k < 4; k++) {
            double root = THICK[k];
            for (int j = 0; j < 3; j++) {
                double end = root * 0.9;
                Mesh joint = joint(JOINTS[k][j], root, end, j == 2);
                if (k == 1 && j == 0) {
                    // The ring itself, on the middle finger, with its stone on the back.
                    joint = Mesh.merged(joint, Mesh.torus(16, 5, root + 0.05, 0.09, 1.75).scaled(1.0, 1.0, 0.9)
                            .moved(0.0, 0.62, 0.0), Mesh.box(-0.17, 0.46, -root - 0.24, 0.17, 0.78, -root + 0.02, 2.1));
                }
                fingers[k][j] = ConstructPainter.Shape.of(joint);
                root = end;
            }
        }
        return fingers;
    }

    private static ConstructPainter.Shape[] thumb() {
        ConstructPainter.Shape[] thumb = new ConstructPainter.Shape[3];
        for (int j = 0; j < 3; j++) {
            double root = THUMB_THICK[j];
            double end = j < 2 ? THUMB_THICK[j + 1] : root * 0.9;
            thumb[j] = ConstructPainter.Shape.of(joint(THUMB[j], root, end, j == 2));
        }
        return thumb;
    }

    /**
     * The pair's battle axe, standing up along y from the end of its pommel (0) to the top of its head, the edge of its
     * blade towards +z and its flats to either side: a long haft, thickening a little towards the head, with a grip
     * wound round it where each fist holds it and a band at either end of each; a knob of a pommel with a band over it;
     * a socket where the head sits, flared at its ends with a band round each and two straps with rivets running down
     * the haft from it; a big crescent blade, thick at the socket and thinner in two steps out to its thin, glowing
     * edge, its horns curving back, the lantern emblem on both its flats; a spike out of its back and a point on top.
     */
    private static Mesh[] axe() {
        double r = HandDuo.HAFT_RADIUS;
        double head = HandDuo.HEAD_AT;
        double half = HandDuo.BLADE_HALF;
        double out = HandDuo.EDGE_OUT;
        double socket = 0.6 * half;
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(10, 1.15, 0.0, 0.0, r * 0.9, 0.0, r * 1.45, 0.25, r * 1.6, 0.55, r * 1.35, 0.85, r * 0.95,
                1.05, 0.0, 1.05));
        parts.add(Mesh.torus(16, 5, r * 1.05, 0.09, 1.6).moved(0.0, 1.12, 0.0));
        parts.add(Mesh.lathe(12, 1.0, 0.0, 1.0, r * 0.96, 1.0, r, 1.4, r * 1.06, head, 0.0, head));
        // The grips: each as long as a fist is wide either way, but never running into the pommel or each other.
        double between = (HandDuo.GRIP_LOW + HandDuo.GRIP_HIGH) * 0.5;
        double[][] grips = {
                { Math.max(1.35, HandDuo.GRIP_LOW - 1.6), Math.min(between - 0.2, HandDuo.GRIP_LOW + 1.6) },
                { Math.max(between + 0.2, HandDuo.GRIP_HIGH - 1.6), HandDuo.GRIP_HIGH + 1.6 } };
        for (double[] grip : grips) {
            double turns = Math.max(2.0, (grip[1] - grip[0]) * 2.0);
            parts.add(Mesh.tube(false, 5, 0.075, 1.15, helix(r + 0.03, grip[0], grip[1], turns)));
            for (double y : grip) {
                parts.add(Mesh.torus(16, 5, r + 0.07, 0.11, 1.6).moved(0.0, y, 0.0));
            }
        }
        parts.add(Mesh.lathe(12, 1.2, 0.0, head - socket - 0.1, r * 1.12, head - socket - 0.1, r * 1.42,
                head - socket + 0.2, r * 1.3, head, r * 1.42, head + socket - 0.2, r * 1.12, head + socket + 0.1, 0.0,
                head + socket + 0.1));
        for (int end = -1; end <= 1; end += 2) {
            parts.add(Mesh.torus(16, 5, r * 1.42, 0.12, 1.6).moved(0.0, head + end * socket, 0.0));
        }
        // The straps run down to just above the upper grip.
        double strap = Math.max(0.6, Math.min(2.3, head - socket - (HandDuo.GRIP_HIGH + 1.85)));
        Mesh straps = Mesh.merged(Mesh.box(r * 0.85, head - socket - strap, -0.2, r + 0.08, head - socket, 0.2, 1.3),
                Mesh.ball(8, 5, 0.09, 1.7).moved(r + 0.08, head - socket - strap * 0.3, 0.0),
                Mesh.ball(8, 5, 0.09, 1.7).moved(r + 0.08, head - socket - strap * 0.78, 0.0));
        parts.add(straps);
        parts.add(straps.mirrored());
        for (int step = 0; step < BLADE_STEPS.length; step++) {
            double[] outline = resized(BLADE, BLADE_TOWARDS[step][0], BLADE_TOWARDS[step][1], BLADE_SHARES[step]);
            parts.add(plate(BLADE_STEPS[step], 1.0 + 0.05 * step, outline).moved(0.0, head, 0.0));
        }
        parts.add(Mesh.tube(false, 5, 0.075, 1.9, edge(half, out)).moved(0.0, head, 0.0));
        // The emblem sits on the thickest step, between the socket and that step's outer rim.
        double inner = r * 1.42;
        double outer = BLADE_TOWARDS[2][1] + BLADE_SHARES[2] * (0.97 * out - BLADE_TOWARDS[2][1]);
        double badge = Math.min(0.62, Math.min(0.42 * (outer - inner), 0.3 * half));
        Mesh emblem = emblem(badge).turned(1.0, 1.0, 1.0, -120.0).moved(BLADE_STEPS[2] + 0.03, head,
                (inner + outer) * 0.5);
        parts.add(emblem);
        parts.add(emblem.mirrored());
        double spike = Math.max(1.2, 0.42 * out);
        parts.add(Mesh.cone(4, 0.55, 0.0, 0.0, spike, 1.2).turned(0.0, 1.0, 0.0, 45.0).turned(1.0, 0.0, 0.0, -98.0)
                .moved(0.0, head, -r * 1.2));
        double point = HandDuo.AXE_LENGTH - (head + socket + 0.1);
        if (point > 0.3) {
            parts.add(Mesh.cone(4, r * 1.1, 0.0, 0.0, point, 1.25).turned(0.0, 1.0, 0.0, 45.0)
                    .moved(0.0, head + socket + 0.1, 0.0));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * The outline of the axe's blade, as pairs of y (from the middle of the head, along the haft) and z (out from the
     * haft), counter-clockwise as seen from +x: from its narrow neck deep in the socket up the inside of its upper horn
     * to its tip, round its edge (an arc {@code half} either way, standing out {@code out} in its middle) to the tip of
     * its lower horn, and back in to the neck.
     */
    private static double[] blade(double half, double out, double haft) {
        double root = 0.3 * haft;
        double sag = 0.22 * out;
        double tips = out - sag;
        double[] horn = { 0.42 * half, root, 0.47 * half, 0.3 * out, 0.58 * half, 0.5 * out, 0.8 * half,
                tips - 0.17 * out, half, tips };
        Vec3[] edge = edge(half, out);
        double[] outline = new double[horn.length * 2 + (edge.length - 2) * 2];
        System.arraycopy(horn, 0, outline, 0, horn.length);
        for (int i = 1; i + 1 < edge.length; i++) {
            outline[horn.length + 2 * (i - 1)] = edge[i].y;
            outline[horn.length + 2 * (i - 1) + 1] = edge[i].z;
        }
        // The lower horn: the upper one mirrored, run the other way round.
        int lower = horn.length + (edge.length - 2) * 2;
        for (int i = 0; i < horn.length / 2; i++) {
            int from = horn.length - 2 - 2 * i;
            outline[lower + 2 * i] = -horn[from];
            outline[lower + 2 * i + 1] = horn[from + 1];
        }
        return outline;
    }

    /**
     * The edge of the axe's blade (see blade), at x = 0: an arc from the tip of its upper horn over its middle, which
     * stands out {@code out} from the haft, round to the tip of the lower one.
     */
    private static Vec3[] edge(double half, double out) {
        double sag = 0.22 * out;
        double radius = (half * half + sag * sag) / (2.0 * sag);
        double middle = out - radius;
        double from = Math.atan2(out - sag - middle, half);
        Vec3[] points = new Vec3[11];
        for (int i = 0; i < points.length; i++) {
            double angle = from + (Math.PI - 2.0 * from) * i / (points.length - 1);
            points[i] = new Vec3(0.0, radius * Math.cos(angle), middle + radius * Math.sin(angle));
        }
        return points;
    }

    /**
     * A flat plate {@code thick} thick either way across x. Its outline gives pairs of how far out along y and where
     * along z, counter-clockwise as seen from +x, and must be seen whole from its own middle.
     */
    private static Mesh plate(double thick, double bright, double[] outline) {
        // Drawn as a prism flat in x and y, then turned so its x lies along y, its y along z and its depth along x.
        return Mesh.prism(-thick, thick, bright, outline).turned(1.0, 1.0, 1.0, 120.0);
    }

    /** An outline drawn in towards (or out from) the point ({@code x}, {@code y}), to {@code share} of its size. */
    private static double[] resized(double[] outline, double x, double y, double share) {
        double[] resized = new double[outline.length];
        for (int i = 0; i < outline.length; i += 2) {
            resized[i] = x + (outline[i] - x) * share;
            resized[i + 1] = y + (outline[i + 1] - y) * share;
        }
        return resized;
    }

    /** A cord wound round the y axis, {@code radius} out, from {@code from} to {@code to}. */
    private static Vec3[] helix(double radius, double from, double to, double turns) {
        int count = (int) Math.ceil(turns * 10.0) + 1;
        Vec3[] points = new Vec3[count];
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            double angle = Math.PI * 2.0 * turns * t;
            points[i] = new Vec3(Math.cos(angle) * radius, from + (to - from) * t, Math.sin(angle) * radius);
        }
        return points;
    }
}
