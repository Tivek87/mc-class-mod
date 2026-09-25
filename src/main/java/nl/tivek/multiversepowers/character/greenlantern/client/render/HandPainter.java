package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The Giant Hands as everyone sees them (see {@link GiantHands}, and {@link HandPose} and {@link HandDuo} for how they
 * move). The ring's light shoots off from the ring to where a hand will come up, a ring of light glows on the ground
 * there in a haze of green light and the ground cracks, and the hand bursts up out of it, white-hot at first and
 * cooling to green. Each hand is a construct of hard light in the shape of Green Lantern's own right hand, and made in
 * detail: a forearm with a glowing gauntlet ring and seams, a glowing cuff round the wrist, a palm with its pads and
 * glowing creases, the lantern emblem, knuckles and glowing tendons on its back, four fingers of three joints each with
 * a nail on every tip, a thumb of three joints, and the ring itself on its middle finger. Nothing of it shows under the
 * ground: where it comes out of it a seam of light runs round it. Its blows leave streaks of light through the air and
 * rings running out over the ground, and a middle finger shooting up leaves a streak of light behind it; it sinks back
 * into the ground when it is done, and breaks into solid pieces, fingers and all, if its maker lets go of it.
 *
 * <p>A pair of hands with an axe comes out of two portals of the ring's light instead: rims of light turning round,
 * arms of light swirling into their middles, a green haze in them and sparks flung off them. Its right hand wears the
 * ring, its left hand is the mirror image of it, bare. Each is cut off where it comes out of its portal, with a seam of
 * light there. Its snap, its OK sign, its grip on the haft and its thumbs up flash and twinkle with light; a third
 * portal brings the axe, a battle axe of hard light that it chops down with a streak of light behind the head, a
 * flare, rings of light and cracks of light running out over the ground; left in the ground it cracks with light and
 * breaks into solid pieces.
 */
public final class HandPainter {
    // The four fingers (index, middle, ring, little): where each one's knuckle is on the hand (x, y), how thick it is at
    // its root, how long each of its three joints is, and how far it spreads out to the side when the hand is open.
    private static final double[][] KNUCKLES = { { -1.12, 3.05 }, { -0.38, 3.18 }, { 0.38, 3.1 }, { 1.1, 2.88 } };
    private static final double[] THICK = { 0.36, 0.38, 0.35, 0.3 };
    private static final double[][] JOINTS = { { 1.25, 0.85, 0.72 }, { 1.38, 0.95, 0.78 }, { 1.3, 0.9, 0.72 },
            { 1.0, 0.68, 0.6 } };
    private static final double[] SPREAD = { 0.13, 0.03, -0.07, -0.18 };
    // How far each joint of a finger bends in a fist, in radians, and how far its two outer joints bend more when it is
    // hooked all the way.
    private static final double[] BENDS = { 1.5, 1.7, 1.25 };
    private static final double[] HOOKS = { 1.35, 1.1 };
    // The thumb: where it grows out of the hand, how long and thick its three joints are, and how far its two outer
    // joints bend more when it is hooked all the way.
    private static final Vec3 THUMB_ROOT = new Vec3(-1.2, 0.95, 0.35);
    private static final double[] THUMB = { 1.25, 1.0, 0.82 };
    private static final double[] THUMB_THICK = { 0.44, 0.4, 0.36 };
    private static final double[] THUMB_HOOKS = { 0.9, 0.8 };
    /** The palm, the back of the hand and everything on them. */
    private static final ConstructPainter.Shape HAND = ConstructPainter.Shape.of(hand());
    /** The cuff round the wrist and the forearm. */
    private static final ConstructPainter.Shape ARM = ConstructPainter.Shape.of(arm());
    /** The same apart, for a pair's hand, whose wrist may twist: the cuff alone, and the forearm without it. */
    private static final ConstructPainter.Shape CUFF = ConstructPainter.Shape.of(cuff());
    private static final ConstructPainter.Shape FOREARM = ConstructPainter.Shape.of(forearm());
    /** Each joint of each finger, standing up along y from its root, its knuckle round its root. */
    private static final ConstructPainter.Shape[][] FINGERS = fingers();
    /** The root joint of the middle finger of a left hand: bare, without the ring. */
    private static final ConstructPainter.Shape MIDDLE_BARE = ConstructPainter.Shape.of(joint(JOINTS[1][0], THICK[1],
            THICK[1] * 0.9, false));
    /** The three joints of the thumb. */
    private static final ConstructPainter.Shape[] THUMBS = thumb();
    // The outline of the axe's blade (see blade), and the three steps it is built of from its edge in: how thick each
    // is either way, and the point each one's outline is drawn in towards (y from the middle of the head, z out from
    // the haft), to what share of the whole.
    private static final double[] BLADE = blade(HandDuo.BLADE_HALF, HandDuo.EDGE_OUT, HandDuo.HAFT_RADIUS);
    private static final double[] BLADE_STEPS = { 0.06, 0.15, 0.26 };
    private static final double[][] BLADE_TOWARDS = { { 0.0, 0.0 },
            { 0.0, 0.3 * HandDuo.HAFT_RADIUS + 0.25 * HandDuo.EDGE_OUT }, { 0.0, 0.3 * HandDuo.HAFT_RADIUS } };
    private static final double[] BLADE_SHARES = { 1.0, 0.8, 0.55 };
    /** The pair's battle axe, along y from the end of its pommel, the edge of its blade towards +z. */
    private static final ConstructPainter.Shape AXE = ConstructPainter.Shape.of(axe());
    // How long a hand that its maker let go of takes to break up, in ticks.
    private static final double BREAK_TICKS = 14.0;
    // How much light from within gets on top of the sky's, so its underside still reads.
    private static final double GLOWS = 0.22;
    // How bright the seam of light is where a hand comes out of the ground, and where it or the axe comes out of a
    // portal.
    private static final double GROUND_SEAM = 0.7;
    private static final double PORTAL_SEAM = 1.0;
    // How far over the top of the ground a hand (or the axe) coming out of it is cut off, in blocks: its seam of light
    // then lies over the ground and shows however steeply it is looked down on, and the sliver of ground left under the
    // cut is far too thin to see.
    private static final double GROUND_CUT = 0.015;
    // How far round its middle a pair may reach while it is on screen, at scale 1: the axe's portal stands about 15
    // beyond it and the axe swung up over the top about 17 over it.
    private static final double PAIR_REACH = 26.0;
    // The numbers the pieces of the pair's left hand and axe start from as they break up, so no two pieces of the pair
    // fly off alike (a hand's own pieces count up to about 60 from its own start).
    private static final int LEFT_PIECES = 100;
    private static final int AXE_PIECES = 200;
    // How many ticks before its portal bursts open the ring's light shoots off to it.
    private static final double AXE_CALL = 5.0;
    // When the side portals and the axe's portal are shut again (see shut).
    private static final double SIDES_SHUT = shut(false);
    private static final double AXE_SHUT = shut(true);

    private HandPainter() {
    }

    // ---- The shapes ----

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

    // ---- Drawing ----

    /**
     * One hand, with the ring's light on its way to it and the ground breaking open where it comes up; or a pair of
     * hands with an axe (see {@link HandDuo}).
     *
     * @param facing the flat way from its base to what it reaches for, blended between two updates
     * @param clock  ticks since it was called, by the client's own clock
     * @param ring   where its maker's ring is, or null when he is out of sight
     */
    public static void draw(LanternPainter painter, ConstructPayload hand, Vec3 facing, double clock,
            @Nullable Vec3 ring) {
        Vec3 base = hand.center();
        int variant = hand.variant();
        double scale = Math.max(0.1, hand.size());
        if (HandPose.move(variant) == HandPose.AXE) {
            pair(painter, hand, facing, clock, ring, scale);
            return;
        }
        double reach = Math.sqrt(facing.x * facing.x + facing.z * facing.z) / scale;
        HandPose pose = HandPose.at(variant, clock, reach);
        HandPose.Place place = pose.place(base, facing, scale);
        ground(painter, hand.id(), base, clock, variant, scale, 1.0);
        if (ring != null && clock < HandPose.ARRIVES + 3.0) {
            // The ring's light shoots off to where it will come up.
            double u = Ease.smooth(clock / HandPose.ARRIVES);
            double fade = 1.0 - Ease.smooth((clock - HandPose.ARRIVES) / 3.0);
            painter.beamOfLight(ring, ring.lerp(base, u), fade, clock, 0.55);
        }
        if (clock < HandPose.ARRIVES - 0.5 || !painter.visible(base, 16.0 * scale)) {
            return;
        }
        // White-hot as it bursts out of the ground, cooling to green; nothing of it shows under the ground.
        painter.glare(0.7 * (1.0 - Ease.smooth((clock - HandPose.ARRIVES) / 16.0)));
        painter.ambient(GLOWS);
        painter.clip(new Vec3(base.x, base.y + GROUND_CUT, base.z), Vectors.UP, GROUND_SEAM);
        drawHand(painter, pose, place, false, 1.0, -1.0, 0, false);
        painter.noClip();
        painter.ambient(0.0);
        painter.glare(0.0);
        blows(painter, hand, facing, clock, scale, 1.0);
    }

    /**
     * The hand in this pose: its palm, back and forearm, each finger joint by joint, and the thumb. A left hand is the
     * mirror image of the right, and bare: it has no ring.
     *
     * @param apart  below 0 for the whole hand, else how far it has broken up into solid pieces, 0 to 1
     * @param seed   the number its pieces start from as it breaks up
     * @param twists true for a hand whose wrist may twist round its forearm (a pair's): its cuff turns halfway with
     *               the hand, so the twist is shared between both sides of the cuff
     */
    private static void drawHand(LanternPainter painter, HandPose pose, HandPose.Place place, boolean left,
            double bright, double apart, int seed, boolean twists) {
        ConstructPainter.Frame hand = handFrame(place, left);
        part(painter, HAND, hand, bright, apart, seed);
        // The forearm runs straight on back, however the wrist bends: down into the ground, or into its portal.
        if (twists) {
            // Where the forearm's palm side would be if the wrist only bent: the hand's palm carried onto the forearm
            // by the turn that takes the fingers' way onto the forearm's way (as HandDuo lays it). The cuff is turned
            // round the forearm halfway from the forearm's palm side to there.
            Vec3 u = place.up();
            Vec3 f = place.forward();
            Vec3 axis = u.cross(place.arm());
            double cos = u.dot(place.arm());
            Vec3 carried = f.scale(cos).add(axis.cross(f)).add(axis.scale(axis.dot(f) / (1.0 + cos)));
            double twist = Math.atan2(place.arm().dot(place.armForward().cross(carried)),
                    place.armForward().dot(carried));
            Vec3 cuffForward = Vectors.spin(place.armForward(), place.arm(), twist * 0.5);
            part(painter, FOREARM, armFrame(place, place.armForward(), left), bright, apart, seed + 20);
            part(painter, CUFF, armFrame(place, cuffForward, left), bright, apart, seed + 30);
        } else {
            part(painter, ARM, armFrame(place, place.armForward(), left), bright, apart, seed + 20);
        }
        for (int k = 0; k < 4; k++) {
            ConstructPainter.Frame[] joints = digit(hand, pose, k);
            for (int j = 0; j < 3; j++) {
                ConstructPainter.Shape shape = left && k == 1 && j == 0 ? MIDDLE_BARE : FINGERS[k][j];
                part(painter, shape, joints[j], bright, apart, seed + 40 + 3 * k + j);
            }
        }
        ConstructPainter.Frame[] thumb = digit(hand, pose, 4);
        for (int j = 0; j < 3; j++) {
            part(painter, THUMBS[j], thumb[j], bright, apart, seed + 60 + j);
        }
    }

    /**
     * The frame the forearm (or its cuff) is drawn in, its palm side facing {@code forward}: it has a right of its
     * own, square to its length, however the hand turns on the wrist (a left hand's mirrored, as the hand's).
     */
    private static ConstructPainter.Frame armFrame(HandPose.Place place, Vec3 forward, boolean left) {
        Vec3 armRight = forward.cross(place.arm());
        armRight = armRight.lengthSqr() < 1.0E-8 ? place.right() : armRight.normalize();
        return new ConstructPainter.Frame(place.wrist(), left ? armRight.scale(-1.0) : armRight, place.arm(), forward,
                place.scale());
    }

    /** The frame a hand's own shapes are drawn in: a left hand's is the right hand's mirrored across its right. */
    private static ConstructPainter.Frame handFrame(HandPose.Place place, boolean left) {
        return new ConstructPainter.Frame(place.wrist(), left ? place.right().scale(-1.0) : place.right(), place.up(),
                place.forward(), place.scale());
    }

    /** One part of a hand or the axe: whole (apart below 0), or breaking up into solid pieces. */
    private static void part(LanternPainter painter, ConstructPainter.Shape shape, ConstructPainter.Frame frame,
            double bright, double apart, int seed) {
        if (apart < 0.0) {
            painter.shape(shape, frame, 1.0, bright);
        } else {
            painter.shattered(shape, frame, apart, bright, seed);
        }
    }

    /**
     * Where the three joints of a finger (0 the index finger to 3 the little finger, 4 the thumb) are in this pose: the
     * frame each one stands up along y in, from its root out. It curls, its two outer joints hook on top of that, and
     * an open hand spreads it out to the side.
     */
    private static ConstructPainter.Frame[] digit(ConstructPainter.Frame hand, HandPose pose, int k) {
        ConstructPainter.Frame[] joints = new ConstructPainter.Frame[3];
        if (k < 4) {
            double curl = pose.curl[k];
            double x = KNUCKLES[k][0];
            double y = KNUCKLES[k][1];
            ConstructPainter.Frame joint = hand.turned(x, y, 0.0, 0.0, 0.0, 1.0,
                    SPREAD[k] * pose.spread * (1.0 - curl)).turned(x, y, 0.0, 1.0, 0.0, 0.0, curl * BENDS[0])
                    .moved(x, y, 0.0);
            joints[0] = joint;
            for (int j = 1; j < 3; j++) {
                double back = JOINTS[k][j - 1];
                joint = joint.turned(0.0, back, 0.0, 1.0, 0.0, 0.0, curl * BENDS[j] + pose.hook[k] * HOOKS[j - 1])
                        .moved(0.0, back, 0.0);
                joints[j] = joint;
            }
            return joints;
        }
        double thumb = pose.curl[4];
        ConstructPainter.Frame joint = hand.turned(THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z, 0.0, 0.0, 1.0,
                Mth.lerp(thumb, 0.8, 0.12)).turned(THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z, 1.0, 0.0, 0.0,
                Mth.lerp(thumb, 0.2, 0.95)).moved(THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z);
        joints[0] = joint;
        for (int j = 1; j < 3; j++) {
            double back = THUMB[j - 1];
            joint = joint.turned(0.0, back, 0.0, 1.0, 0.0, 0.0, Mth.lerp(thumb, 0.1, j == 1 ? 0.45 : 0.5)
                    + pose.hook[4] * THUMB_HOOKS[j - 1]).turned(0.0, back, 0.0, 0.0, 0.0, 1.0,
                    Mth.lerp(thumb, 0.0, j == 1 ? -0.95 : -0.45)).moved(0.0, back, 0.0);
            joints[j] = joint;
        }
        return joints;
    }

    /** Where the tip of the middle finger of a right hand in this pose is, out in the world. */
    private static Vec3 middleTip(HandPose pose, HandPose.Place place) {
        return digit(handFrame(place, false), pose, 1)[2].at(0.0, JOINTS[1][2], 0.0);
    }

    /** Where the tip of the thumb of a hand in this pose is, out in the world. */
    private static Vec3 thumbTip(HandPose pose, HandPose.Place place, boolean left) {
        return digit(handFrame(place, left), pose, 4)[2].at(0.0, THUMB[2] + THUMB_THICK[2] * 0.9, 0.0);
    }

    /**
     * Where it comes up: a ring of light on the ground in a low haze of green light, and cracks running out from it as
     * it bursts out, fading once it is out, and glowing faintly again as it sinks back in.
     */
    private static void ground(LanternPainter painter, int id, Vec3 base, double clock, int variant, double scale,
            double strength) {
        double arrives = HandPose.ARRIVES;
        int life = HandPose.life(variant);
        double burst = strength * Ease.smooth((clock - arrives * 0.25) / (arrives * 0.75))
                * (1.0 - Ease.smooth((clock - arrives - 12.0) / 18.0));
        double sink = strength * Ease.smooth((clock - HandPose.sinks(variant)) / 6.0)
                * (1.0 - Ease.smooth((clock - life + 3.0) / 3.0));
        double glow = Math.max(burst, 0.6 * sink);
        if (glow <= 0.01) {
            return;
        }
        Vec3 at = base.add(0.0, 0.06, 0.0);
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        double wide = (1.6 + 1.5 * Ease.smooth(clock / (HandPose.ARRIVES + 4.0))) * scale;
        painter.circle(at, east, south, wide, 0.12, 1.1, Colors.alpha(0.9 * glow), Colors.alpha(0.45 * glow));
        painter.circle(at, east, south, wide * 0.6, 0.08, 0.7, Colors.alpha(0.6 * glow), Colors.alpha(0.3 * glow));
        if (burst > 0.01) {
            painter.flare(base.add(0.0, 0.4, 0.0), (1.5 + 2.5 * burst) * scale, burst);
            // Its light glows in the dust it throws up.
            painter.haze(base.add(0.0, 0.7 * scale, 0.0), east.scale(3.2 * scale), Vectors.UP.scale(2.4 * scale),
                    south.scale(3.2 * scale), painter.material().glow(), 0.35 * burst);
            // The ground cracks open round it.
            cracks(painter, id, at, 10, 2.2 * scale, Ease.smooth((clock - HandPose.ARRIVES + 1.5) / 6.0), burst,
                    0.08 * scale);
        }
    }

    /** Cracks of light running out over the ground from {@code at}, each 1 to 2 times {@code length} long, grown. */
    private static void cracks(LanternPainter painter, int seed, Vec3 at, int count, double length, double grow,
            double strength, double width) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * (i + 0.4 * Noise.of(seed, i, 1)) / count;
            crack(painter, seed, i, at, angle, length * (1.0 + Noise.of(seed, i, 2)), grow, strength, width);
        }
    }

    /** One crack of light running out over the ground from {@code at} the way {@code angle} points, in three bends. */
    private static void crack(LanternPainter painter, int seed, int i, Vec3 at, double angle, double length,
            double grow, double strength, double width) {
        Vec3 from = at;
        for (int s = 1; s <= 3; s++) {
            double bend = angle + (Noise.of(seed, i, 3 + s) - 0.5) * 0.6;
            double out = length * grow * s / 3.0;
            Vec3 to = at.add(Math.cos(bend) * out, 0.0, Math.sin(bend) * out);
            painter.edge(from, to, width * (1.2 - 0.25 * s), strength);
            from = to;
        }
    }

    /**
     * The light its blows leave: a streak through the air behind a swatting or throwing palm, a streak behind a middle
     * finger shooting up, and rings running out over the ground where a palm or a fist lands or a middle finger bursts
     * out.
     */
    private static void blows(LanternPainter painter, ConstructPayload hand, Vec3 facing, double clock, double scale,
            double strength) {
        Vec3 base = hand.center();
        int move = HandPose.move(hand.variant());
        double reach = Math.sqrt(facing.x * facing.x + facing.z * facing.z) / scale;
        double swing = HandPose.SWING_TICKS;
        int blow = move == HandPose.SMACK ? HandPose.SMACK_HITS : move == HandPose.GRAB ? HandPose.GRAB_THROWS : -1;
        if (blow >= 0 && clock > blow - swing * 0.5 && clock < blow + swing * 0.75) {
            // Fading in as the swing starts and out after the blow, never popping in or out.
            streak(painter, hand.variant(), base, facing, reach, clock, scale,
                    strength * (1.0 - Ease.smooth((clock - blow) / (swing * 0.6)))
                            * Ease.smooth((clock - (blow - swing * 0.5)) / 2.0));
        }
        double slammed = clock - HandPose.SLAM_HITS;
        if (move == HandPose.SLAM && slammed >= 0.0 && slammed <= shockwaveTicks(3) && strength > 0.01) {
            // Only while its rings run out (see shockwave): where it landed is worked out for nothing else.
            HandPose.Place land = HandPose.at(hand.variant(), HandPose.SLAM_HITS, reach).place(base, facing, scale);
            Vec3 palm = land.at(HandPose.PALM);
            shockwave(painter, new Vec3(palm.x, base.y, palm.z), clock - HandPose.SLAM_HITS, 4.5 * scale, 3,
                    strength);
        }
        if (move == HandPose.FINGER) {
            // Bursting out it throws a wave of light out over the ground, wider than any blow.
            shockwave(painter, base, clock - HandPose.FINGER_BURSTS, 6.5 * scale, 4, strength);
            fingerStreak(painter, hand.variant(), base, facing, reach, clock, scale, strength);
        }
        if (move == HandPose.POUND) {
            for (int hit : HandPose.POUND_HITS) {
                double since = clock - hit;
                if (since >= 0.0 && since <= shockwaveTicks(2)) {
                    Vec3 fist = HandPose.at(hand.variant(), hit, reach).place(base, facing, scale).at(HandPose.FIST);
                    shockwave(painter, new Vec3(fist.x, base.y, fist.z), since, 3.2 * scale, 2, strength);
                }
            }
        }
    }

    /** A streak of light behind a palm swinging through the air: from its wrist to its fingertips, over the last ticks. */
    private static void streak(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double reach, double clock,
            double scale, double strength) {
        if (strength <= 0.01) {
            return;
        }
        Vec3 lastIn = null;
        Vec3 lastOut = null;
        for (int k = 0; k <= 5; k++) {
            double t = clock - k * HandPose.SWING_TICKS * 0.15;
            HandPose.Place place = HandPose.at(variant, t, reach).place(base, facing, scale);
            Vec3 in = place.at(new Vec3(0.0, 1.2, 0.0));
            Vec3 out = place.at(new Vec3(0.0, 5.6, 0.0));
            if (lastIn != null) {
                double a = strength * (1.0 - (k - 1) / 5.0) * 0.55;
                double b = strength * (1.0 - k / 5.0) * 0.55;
                painter.sheet(lastIn, lastOut, out, in, a, a, b, b);
            }
            lastIn = in;
            lastOut = out;
        }
    }

    /**
     * The streak of light a middle finger leaves as it shoots up out of the ground: a ribbon of light along the path of
     * its tip over the last few ticks, bright along its middle and soft at its sides, with a bright line down it.
     */
    private static void fingerStreak(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double reach,
            double clock, double scale, double fade) {
        double bursts = HandPose.FINGER_BURSTS;
        double strength = fade * Ease.smooth((clock - bursts + 1.5) / 1.5)
                * (1.0 - Ease.smooth((clock - bursts - 3.0) / 7.0));
        if (strength <= 0.01) {
            return;
        }
        int count = 8;
        Vec3[] tips = new Vec3[count];
        for (int k = 0; k < count; k++) {
            HandPose pose = HandPose.at(variant, clock - k * 0.55, reach);
            Vec3 tip = middleTip(pose, pose.place(base, facing, scale));
            tips[k] = new Vec3(tip.x, Math.max(tip.y, base.y + 0.05), tip.z);
        }
        Vec3[] sides = new Vec3[count];
        double[] alphas = new double[count];
        for (int k = 0; k < count; k++) {
            Vec3 along = tips[Math.max(0, k - 1)].subtract(tips[Math.min(count - 1, k + 1)]);
            Vec3 side = along.cross(painter.camera().subtract(tips[k]));
            double f = (double) k / (count - 1);
            sides[k] = side.lengthSqr() < 1.0E-8 ? Vec3.ZERO : side.normalize().scale(0.9 * scale * (1.0 - 0.7 * f));
            alphas[k] = strength * (1.0 - f);
        }
        for (int k = 0; k + 1 < count; k++) {
            Vec3 a = tips[k];
            Vec3 b = tips[k + 1];
            if (a.distanceToSqr(b) < 1.0E-6) {
                continue;
            }
            painter.sheet(a, b, b.add(sides[k + 1]), a.add(sides[k]), alphas[k], alphas[k + 1], 0.0, 0.0);
            painter.sheet(a, b, b.subtract(sides[k + 1]), a.subtract(sides[k]), alphas[k], alphas[k + 1], 0.0, 0.0);
            painter.edge(a, b, 0.14 * scale * (1.0 - 0.6 * k / (count - 1.0)), 0.8 * alphas[k]);
        }
    }

    /** How long a blow's rings of light run out over the ground (see shockwave), in ticks: until the last is out. */
    private static double shockwaveTicks(int rings) {
        return 12.0 + (rings - 1) * 2.5;
    }

    /** Rings of light running out over the ground from where a blow landed, and a flash, {@code strength} strong. */
    private static void shockwave(LanternPainter painter, Vec3 at, double since, double reach, int rings,
            double strength) {
        if (since < 0.0 || since > shockwaveTicks(rings) || strength <= 0.01) {
            return;
        }
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        Vec3 ground = at.add(0.0, 0.08, 0.0);
        for (int k = 0; k < rings; k++) {
            double ring = since - k * 2.5;
            if (ring < 0.0) {
                continue;
            }
            double wave = 1.0 - Math.pow(1.0 - Math.min(1.0, ring / 9.0), 2.0);
            double fade = strength * Math.max(0.0, 1.0 - ring / 12.0) * Ease.smooth(ring / 0.6);
            painter.circle(ground, east, south, 0.5 + reach * wave, 0.1, 0.8, Colors.alpha(fade),
                    Colors.alpha(0.5 * fade));
        }
        if (since < 4.0) {
            painter.flare(ground.add(0.0, 0.4, 0.0), reach * (1.0 - since / 4.0),
                    strength * (1.0 - since / 4.0) * Ease.smooth(since / 0.5));
        }
    }

    // ---- The pair with the axe ----

    /**
     * A pair of hands with an axe: the ring's light shooting off to where its portals open, the three portals, both
     * hands each cut off at its own portal, the axe and all the light of what they do.
     */
    private static void pair(LanternPainter painter, ConstructPayload hand, Vec3 facing, double clock,
            @Nullable Vec3 ring, double scale) {
        Vec3 base = hand.center();
        int variant = hand.variant();
        Vec3 aim = base.add(facing);
        HandDuo duo = HandDuo.at(base, variant, aim, clock, scale);
        if (ring != null) {
            calls(painter, duo, clock, ring);
        }
        if (!painter.visible(base, PAIR_REACH * scale)) {
            return;
        }
        int seed = hand.id() * 7;
        flashes(painter, duo, clock, 1.0);
        portal(painter, duo.rightPortal, seed, clock, 1.0);
        portal(painter, duo.leftPortal, seed + 1, clock, 1.0);
        portal(painter, duo.axePortal, seed + 2, clock, 1.0);
        if (duo.handsThere) {
            // White-hot as they push out of their portals, cooling to green, and warming up again as they pull back.
            double retract = Math.max(1.0, HandDuo.HANDS_GONE - HandDuo.RETRACT);
            painter.glare(Math.max(0.7 * (1.0 - Ease.smooth((clock - HandDuo.ARRIVES) / 16.0)),
                    0.35 * Ease.smooth((clock - HandDuo.RETRACT) / retract)));
            painter.ambient(GLOWS);
            pairHand(painter, duo.rightPortal, duo.rightPose, duo.rightPlace, false, 1.0, -1.0, 0);
            pairHand(painter, duo.leftPortal, duo.leftPose, duo.leftPlace, true, 1.0, -1.0, LEFT_PIECES);
            painter.ambient(0.0);
            painter.glare(0.0);
        }
        if (duo.axeThere) {
            ConstructPainter.Frame frame = ConstructPainter.Frame.of(duo.axeEnd, duo.axeFace, duo.axeUp, scale);
            painter.glare(axeGlare(clock, duo.axeBreak));
            painter.ambient(GLOWS);
            painter.fling(1.6);
            axe(painter, duo, base, frame, duo.axeBreak > 0.0 ? duo.axeBreak : -1.0);
            painter.fling(1.0);
            painter.ambient(0.0);
            painter.glare(0.0);
            axeCracks(painter, frame, seed, clock, duo.axeBreak);
        }
        lights(painter, seed, base, variant, aim, duo, clock, scale, 1.0);
    }

    /**
     * The light of what a pair does, {@code strength} strong (1, and less as it fades away when the pair breaks up):
     * the snap, the OK sign, the grab, the streak of the chop, the blow and the thumbs up.
     */
    private static void lights(LanternPainter painter, int seed, Vec3 base, int variant, Vec3 aim, HandDuo duo,
            double clock, double scale, double strength) {
        snap(painter, duo, clock, scale, strength);
        double ok = strength * Ease.smooth((clock - HandDuo.OK + 1.5) / 2.5)
                * (1.0 - Ease.smooth((clock - HandDuo.OK - 16.0) / 6.0));
        if (ok > 0.01) {
            // The OK sign: a twinkle of light in its ring.
            twinkle(painter, duo.leftPlace.at(HandDuo.OK_AT), 0.8 * scale, ok, clock);
        }
        grab(painter, duo, clock, scale, strength);
        chop(painter, base, variant, aim, clock, scale, strength);
        impact(painter, seed, base, variant, aim, clock, scale, strength);
        double thumbs = strength * Ease.smooth((clock - HandDuo.THUMBS + 1.0) / 2.5)
                * (1.0 - Ease.smooth((clock - HandDuo.THUMBS - 11.0) / 5.0));
        if (thumbs > 0.01 && duo.handsThere) {
            // The thumbs up: a twinkle of light at the tip of each thumb.
            twinkle(painter, thumbTip(duo.rightPose, duo.rightPlace, false), 0.9 * scale, thumbs, clock);
            twinkle(painter, thumbTip(duo.leftPose, duo.leftPlace, true), 0.9 * scale, thumbs, clock + 2.3);
        }
    }

    /**
     * The ring's light shooting off from the ring: to where the side portals burst open as the pair is called, and a
     * little before the axe's portal bursts open, to it.
     */
    private static void calls(LanternPainter painter, HandDuo duo, double clock, Vec3 ring) {
        double arrives = HandDuo.ARRIVES;
        if (clock < arrives + 3.0) {
            double u = Ease.smooth(clock / arrives);
            double fade = 1.0 - Ease.smooth((clock - arrives) / 3.0);
            painter.beamOfLight(ring, ring.lerp(duo.rightPortal.center(), u), fade, clock, 0.55);
            painter.beamOfLight(ring, ring.lerp(duo.leftPortal.center(), u), fade, clock, 0.55);
        }
        double from = HandDuo.AXE_OPENS - AXE_CALL;
        if (clock > from && clock < HandDuo.AXE_OPENS + 3.0) {
            double u = Ease.smooth((clock - from) / AXE_CALL);
            double fade = 1.0 - Ease.smooth((clock - HandDuo.AXE_OPENS) / 3.0);
            painter.beamOfLight(ring, ring.lerp(duo.axePortal.center(), u), fade, clock - from, 0.55);
        }
    }

    /**
     * A portal of the ring's light, all of it light (see through it): a bright rim with arcs of light running round it,
     * fainter arcs turning the other way inside it and a soft halo outside it, arms of light swirling into its middle,
     * a green haze filling it (on both sides, over the solid lid of an arm cut off in it) with a soft glow in its middle,
     * and sparks flung off its rim.
     *
     * @param spin ticks that turn it round
     * @param keep how much of its size it keeps: 1, and less as it shrinks away when the pair breaks up
     */
    private static void portal(LanternPainter painter, HandDuo.Portal portal, int seed, double spin, double keep) {
        double full = portal.radius();
        double open = Math.max(0.0, portal.open() * keep);
        double r = full * open;
        if (open <= 0.01 || full <= 0.0) {
            return;
        }
        double shown = Mth.clamp(open * 2.5, 0.0, 1.0);
        double w = full * 0.4;
        Vec3 c = portal.center();
        Vec3 n = portal.normal();
        Vec3 a = portal.a();
        Vec3 b = portal.b();
        painter.circle(c, a, b, r, 0.13 * w, 1.0 * w, Colors.alpha(0.95 * shown), Colors.alpha(0.5 * shown));
        painter.circle(c, a, b, r * (1.12 + 0.03 * Math.sin(spin * 0.3)), 0.05 * w, 0.7 * w,
                Colors.alpha(0.3 * shown), Colors.alpha(0.25 * shown));
        arcs(painter, c, a, b, r, 3, 0.9, spin * 0.14 + seed, 0.2 * w, 0.9 * shown);
        arcs(painter, c, a, b, r * 0.82, 5, 0.5, -spin * 0.22 - seed, 0.09 * w, 0.6 * shown);
        // Arms of light swirling into its middle.
        int arms = 5;
        int steps = 8;
        double turn = spin * 0.25 + seed * 1.7;
        for (int arm = 0; arm < arms; arm++) {
            Vec3 last = null;
            for (int s = 0; s <= steps; s++) {
                double q = (double) s / steps;
                double angle = turn + Math.PI * 2.0 * arm / arms + 2.6 * q;
                double out = r * (0.94 - 0.84 * q);
                Vec3 at = c.add(a.scale(Math.cos(angle) * out)).add(b.scale(Math.sin(angle) * out));
                if (last != null) {
                    painter.edge(last, at, 0.1 * w * (1.0 - 0.6 * q),
                            0.55 * shown * Math.sin(Math.PI * (0.15 + 0.85 * q)));
                }
                last = at;
            }
        }
        painter.haze(c, a.scale(r * 0.95), n.scale(r * 0.22), b.scale(r * 0.95), painter.material().glow(),
                0.28 * shown);
        painter.flare(c, r * 0.55, 0.3 * shown);
        // Sparks flung off its rim the way it turns, each one flaring up and dying away again.
        for (int i = 0; i < 10; i++) {
            double period = 8.0 + 6.0 * Noise.of(seed, i, 1);
            double run = (spin + period * Noise.of(seed, i, 2)) / period;
            int round = (int) Math.floor(run);
            double f = run - round;
            double angle = Math.PI * 2.0 * Noise.of(seed * 31 + round, i, 3);
            Vec3 outward = a.scale(Math.cos(angle)).add(b.scale(Math.sin(angle)));
            Vec3 along = a.scale(-Math.sin(angle)).add(b.scale(Math.cos(angle)));
            Vec3 way = outward.scale(0.8).add(along.scale(0.7)).add(n.scale(0.35 * (Noise.of(seed, i, 4) - 0.3)));
            Vec3 at = c.add(outward.scale(r)).add(way.scale(full * 0.7 * f));
            painter.edge(at.subtract(way.scale(full * 0.18)), at, 0.05 * w, 0.85 * shown * Math.sin(Math.PI * f));
        }
    }

    /** Arcs of light running round a circle: {@code count} of them, each {@code length} radians long, tapering. */
    private static void arcs(LanternPainter painter, Vec3 c, Vec3 a, Vec3 b, double radius, int count, double length,
            double turn, double width, double strength) {
        int pieces = 5;
        for (int k = 0; k < count; k++) {
            double start = turn + Math.PI * 2.0 * k / count;
            Vec3 last = null;
            for (int s = 0; s <= pieces; s++) {
                double angle = start + length * s / pieces;
                Vec3 at = c.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
                if (last != null) {
                    painter.edge(last, at, width, strength * Math.sin(Math.PI * (s - 0.5) / pieces));
                }
                last = at;
            }
        }
    }

    /** The flashes of the pair's three portals (see flashes), {@code strength} strong. */
    private static void flashes(LanternPainter painter, HandDuo duo, double clock, double strength) {
        flashes(painter, duo.rightPortal, clock, HandDuo.ARRIVES, SIDES_SHUT, strength);
        flashes(painter, duo.leftPortal, clock, HandDuo.ARRIVES, SIDES_SHUT, strength);
        flashes(painter, duo.axePortal, clock, HandDuo.AXE_OPENS, AXE_SHUT, strength);
    }

    /**
     * The flashes of a portal: one as it bursts open, and a smaller one with a ring of light popping out of it as it
     * snaps shut.
     */
    private static void flashes(LanternPainter painter, HandDuo.Portal portal, double clock, double opens,
            double shut, double strength) {
        double full = portal.radius();
        double burst = strength * Ease.smooth((clock - opens + 1.0) / 1.2)
                * (1.0 - Ease.smooth((clock - opens) / 7.0));
        if (burst > 0.01) {
            painter.flare(portal.center(), full * (1.0 + 0.8 * burst), burst);
        }
        double pop = strength * Ease.smooth((clock - shut + 2.0) / 2.0) * (1.0 - Ease.smooth((clock - shut) / 5.0));
        if (pop > 0.01) {
            painter.flare(portal.center(), full * 0.9 * pop, pop);
            double u = Ease.smooth((clock - shut + 1.0) / 6.0);
            painter.circle(portal.center(), portal.a(), portal.b(), full * (0.2 + 1.1 * u), 0.05 * full,
                    0.35 * full, Colors.alpha(0.8 * pop), Colors.alpha(0.4 * pop));
        }
    }

    /**
     * When the portals of a pair are shut again, in ticks: the side portals once the hands have pulled back into them,
     * the axe's portal once the axe's head is out of it. They open and shut alike wherever a pair is.
     */
    private static double shut(boolean axe) {
        int variant = HandPose.axeVariant(new Vec3(0.0, 0.0, 1.0));
        double from = axe ? HandDuo.AXE_FREE : HandDuo.RETRACT;
        for (double t = from; t <= HandDuo.LIFE; t += 0.25) {
            HandDuo duo = HandDuo.at(Vec3.ZERO, variant, Vec3.ZERO, t, 1.0);
            if ((axe ? duo.axePortal : duo.leftPortal).open() <= 0.01) {
                return t;
            }
        }
        return axe ? HandDuo.AXE_FREE + 3.0 : HandDuo.HANDS_GONE;
    }

    /** One hand of the pair, cut off where it comes out of its portal (see drawHand). */
    private static void pairHand(LanternPainter painter, HandDuo.Portal portal, HandPose pose, HandPose.Place place,
            boolean left, double bright, double apart, int seed) {
        painter.clip(portal.center(), portal.normal(), PORTAL_SEAM);
        drawHand(painter, pose, place, left, bright, apart, seed, true);
        painter.noClip();
    }

    /**
     * How white-hot the axe glows: as it comes out of its portal, in a flash as it bites into the ground, and more and
     * more as the light cracks it before it breaks.
     */
    private static double axeGlare(double clock, double breaking) {
        double out = 0.6 * (1.0 - Ease.smooth((clock - HandDuo.AXE_OPENS) / 14.0));
        double bite = 0.45 * Ease.smooth((clock - HandDuo.IMPACT + 0.5) / 0.5)
                * (1.0 - Ease.smooth((clock - HandDuo.IMPACT) / 6.0));
        double cracked = 0.4 * crackGrowth(clock, 0.0, 1.0) + 0.3 * breaking;
        return Math.max(out, Math.max(bite, cracked));
    }

    /**
     * The axe, solid: cut off where it comes out of its portal while it still does, and never showing under the ground
     * (where its blade bites in, a seam of light runs round it).
     *
     * @param apart below 0 whole, else how far it has broken up into solid pieces, 0 to 1
     */
    private static void axe(LanternPainter painter, HandDuo duo, Vec3 base, ConstructPainter.Frame frame,
            double apart) {
        if (duo.axeCut) {
            painter.clip(duo.axePortal.center(), duo.axePortal.normal(), PORTAL_SEAM);
        } else {
            painter.clip(new Vec3(base.x, base.y + GROUND_CUT, base.z), Vectors.UP, GROUND_SEAM);
        }
        part(painter, AXE, frame, apart < 0.0 ? 1.0 : 1.2, apart, AXE_PIECES);
        painter.noClip();
    }

    /**
     * How far the cracks of light in the axe left in the ground have grown, 0 to 1, growing from {@code from} to
     * {@code to} of the time from a little after its blow until it breaks.
     */
    private static double crackGrowth(double clock, double from, double to) {
        double start = HandDuo.IMPACT + 2.0;
        double span = Math.max(4.0, HandDuo.AXE_BREAKS - start);
        return Ease.smooth((clock - start - from * span) / ((to - from) * span));
    }

    /**
     * Cracks of light creeping through the axe left in the ground: over both flats of its blade from its edge in
     * towards the socket, and then up its haft from the socket towards the pommel. They glow brighter as they grow,
     * flicker, and die away as it breaks up.
     */
    private static void axeCracks(LanternPainter painter, ConstructPainter.Frame frame, int seed, double clock,
            double breaking) {
        double strength = Ease.smooth((clock - HandDuo.IMPACT - 2.0) / 3.0) * (1.0 - Ease.smooth(breaking * 3.0));
        if (strength <= 0.01) {
            return;
        }
        double head = HandDuo.HEAD_AT;
        double half = HandDuo.BLADE_HALF;
        double out = HandDuo.EDGE_OUT;
        double r = HandDuo.HAFT_RADIUS;
        double width = 0.07 * frame.scale();
        double blade = crackGrowth(clock, 0.0, 0.5);
        for (int c = 0; c < 4; c++) {
            double side = c < 2 ? 1.0 : -1.0;
            double y = (Noise.of(seed, c, 1) - 0.5) * 0.9 * half;
            double z = out * 0.93;
            Vec3[] path = new Vec3[6];
            for (int s = 0; s < path.length; s++) {
                if (s > 0) {
                    y += (Noise.of(seed, c, 2 + s) - 0.5) * 0.22 * half;
                    z -= out * 0.15;
                }
                path[s] = frame.at(side * (flat(y, z) + 0.025), head + y, z);
            }
            double glow = strength * (0.6 + 0.4 * blade) * (0.85 + 0.15 * Math.sin(clock * 2.1 + c * 1.7));
            grown(painter, path, blade, width, glow);
        }
        double haft = crackGrowth(clock, 0.3, 1.0);
        double top = head - 0.6 * half - 0.1;
        for (int c = 0; c < 2; c++) {
            double angle = Math.PI * 2.0 * Noise.of(seed, 10 + c, 1);
            Vec3[] path = new Vec3[8];
            for (int s = 0; s < path.length; s++) {
                if (s > 0) {
                    angle += (Noise.of(seed, 10 + c, 2 + s) - 0.5) * 0.7;
                }
                double y = top - (top - 1.2) * s / (path.length - 1);
                path[s] = frame.at(Math.cos(angle) * (r + 0.05), y, Math.sin(angle) * (r + 0.05));
            }
            double glow = strength * (0.6 + 0.4 * haft) * (0.85 + 0.15 * Math.sin(clock * 1.9 + c * 2.3));
            grown(painter, path, haft, width, glow);
        }
    }

    /** How thick the axe's blade is either way at a point (y from the middle of its head, z out from its haft). */
    private static double flat(double y, double z) {
        for (int step = BLADE_STEPS.length - 1; step > 0; step--) {
            double[] towards = BLADE_TOWARDS[step];
            double share = BLADE_SHARES[step];
            // Inside this step's outline: the point, pushed back out from where the step is drawn in towards, lies
            // inside the whole blade's.
            if (inside(BLADE, towards[0] + (y - towards[0]) / share, towards[1] + (z - towards[1]) / share)) {
                return BLADE_STEPS[step];
            }
        }
        return BLADE_STEPS[0];
    }

    /** Whether a point lies inside an outline of pairs of numbers. */
    private static boolean inside(double[] outline, double x, double y) {
        boolean in = false;
        int n = outline.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = outline[2 * i];
            double yi = outline[2 * i + 1];
            double xj = outline[2 * j];
            double yj = outline[2 * j + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }

    /** A line of light along a path of points, drawn as far as {@code grow} (0 to 1) of it, thinning as it runs. */
    private static void grown(LanternPainter painter, Vec3[] path, double grow, double width, double strength) {
        double reach = grow * (path.length - 1);
        for (int i = 0; i + 1 < path.length && i < reach; i++) {
            Vec3 to = path[i].lerp(path[i + 1], Math.min(1.0, reach - i));
            painter.edge(path[i], to, width * (1.0 - 0.5 * i / (path.length - 1)), strength);
        }
    }

    /** The snap: a sharp flash between the right hand's thumb and middle finger, a ring of light, sparks bursting. */
    private static void snap(LanternPainter painter, HandDuo duo, double clock, double scale, double strength) {
        double since = clock - HandDuo.SNAP;
        if (since < -0.4 || since > 8.0 || strength <= 0.01) {
            return;
        }
        Vec3 at = duo.rightPlace.at(HandDuo.SNAP_AT);
        double in = strength * Ease.smooth((since + 0.4) / 0.5);
        double flash = in * (1.0 - Ease.smooth(since / 5.0));
        painter.flare(at, 2.2 * scale * (0.5 + 0.5 * flash), flash);
        Vec3 view = painter.camera().subtract(at);
        if (view.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] across = Vectors.across(view.normalize());
        double u = 1.0 - Math.pow(1.0 - Mth.clamp(since / 6.0, 0.0, 1.0), 2.0);
        double ring = in * (1.0 - Ease.smooth(since / 7.0));
        painter.circle(at, across[0], across[1], (0.3 + 2.4 * u) * scale, 0.07 * scale, 0.5 * scale,
                Colors.alpha(0.9 * ring), Colors.alpha(0.45 * ring));
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * (i + 0.3 * Noise.of(i, 5, 1)) / 6.0;
            Vec3 way = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)));
            painter.edge(at.add(way.scale((0.2 + 2.2 * u) * scale)), at.add(way.scale((0.4 + 3.0 * u) * scale)),
                    0.06 * scale, ring);
        }
    }

    /** The grab: a flash at each fist as it closes on the haft, and a ring of light running out round the haft. */
    private static void grab(LanternPainter painter, HandDuo duo, double clock, double scale, double strength) {
        double since = clock - HandDuo.GRAB;
        double flash = strength * Ease.smooth((since + 0.5) / 0.8) * (1.0 - Ease.smooth(since / 6.0));
        if (flash <= 0.01 || !duo.axeThere) {
            return;
        }
        Vec3[] round = Vectors.across(duo.axeUp);
        double u = Ease.smooth(since / 6.0);
        for (double grip : new double[] { HandDuo.GRIP_LOW, HandDuo.GRIP_HIGH }) {
            Vec3 at = duo.axeEnd.add(duo.axeUp.scale(grip * scale));
            painter.flare(at, 2.5 * scale * (0.6 + 0.4 * flash), flash);
            painter.circle(at, round[0], round[1], (HandDuo.HAFT_RADIUS * 2.2 + 1.8 * u) * scale, 0.06 * scale,
                    0.4 * scale, Colors.alpha(0.85 * flash), Colors.alpha(0.4 * flash));
        }
    }

    /**
     * The chop: a sheet of light behind the axe's blade over the last few ticks, from the middle of its head out to the
     * middle of its edge, as bright as the blade is fast (so only the chop leaves one, not the slow raise).
     */
    private static void chop(LanternPainter painter, Vec3 base, int variant, Vec3 aim, double clock, double scale,
            double strength) {
        double window = strength * Ease.smooth((clock - HandDuo.RAISED + 1.0) / 3.0)
                * (1.0 - Ease.smooth((clock - HandDuo.IMPACT - 1.0) / 5.0));
        if (window <= 0.01) {
            return;
        }
        int count = 8;
        double step = 0.7;
        Vec3[] in = new Vec3[count];
        Vec3[] out = new Vec3[count];
        for (int k = 0; k < count; k++) {
            // Only the axe of the pair as it was then: its hands need not be worked out for this.
            HandDuo.Axe past = HandDuo.axe(base, variant, aim, clock - k * step, scale);
            Vec3 head = past.end().add(past.up().scale(HandDuo.HEAD_AT * scale));
            in[k] = head.add(past.face().scale(HandDuo.HAFT_RADIUS * 1.5 * scale));
            out[k] = head.add(past.face().scale(HandDuo.EDGE_OUT * scale));
        }
        double[] alphas = new double[count - 1];
        for (int k = 0; k + 1 < count; k++) {
            double speed = out[k].distanceTo(out[k + 1]) / (step * scale);
            alphas[k] = window * Ease.smooth((speed - 0.6) / 1.6) * (1.0 - (double) k / (count - 1)) * 0.6;
        }
        for (int k = 0; k + 2 < count; k++) {
            painter.sheet(in[k], out[k], out[k + 1], in[k + 1], alphas[k], alphas[k], alphas[k + 1], alphas[k + 1]);
        }
    }

    /**
     * The blow: where the blade bites into the ground a big flare, four rings of light running out over the ground and
     * cracks of light running out from it (the longest along the chop), glowing on faintly while the axe stays.
     */
    private static void impact(LanternPainter painter, int seed, Vec3 base, int variant, Vec3 aim, double clock,
            double scale, double strength) {
        double since = clock - HandDuo.IMPACT;
        if (since < -0.5 || clock > HandDuo.AXE_BREAKS + 10.0 || strength <= 0.01) {
            return;
        }
        Vec3 strike = HandDuo.strike(base, variant, aim, scale);
        Vec3 ground = new Vec3(strike.x, base.y + 0.06, strike.z);
        shockwave(painter, ground, since, 9.0 * scale, 4, strength);
        double flash = strength * Ease.smooth((since + 0.5) / 0.6) * (1.0 - Ease.smooth(since / 7.0));
        if (flash > 0.01) {
            painter.flare(ground.add(0.0, 0.8 * scale, 0.0), 6.0 * scale * (0.5 + 0.5 * flash), flash);
        }
        double glow = strength * Ease.smooth((since + 0.5) / 1.0) * (0.3 + 0.7 * (1.0 - Ease.smooth(since / 25.0)))
                * (1.0 - Ease.smooth((clock - HandDuo.AXE_BREAKS) / 10.0));
        if (glow <= 0.01) {
            return;
        }
        double grow = Ease.smooth((since + 0.3) / 5.0);
        cracks(painter, seed + 3, ground, 12, 3.0 * scale, grow, glow, 0.1 * scale);
        Vec3 way = HandPose.axeWay(variant);
        double angle = Math.atan2(way.z, way.x);
        for (int side = 0; side < 2; side++) {
            crack(painter, seed + 4, side, ground, angle + Math.PI * side, 7.5 * scale, grow, glow, 0.14 * scale);
        }
    }

    /** A twinkle of light facing you: a soft flare, four long rays and four short ones turning the other way. */
    private static void twinkle(LanternPainter painter, Vec3 at, double size, double strength, double spin) {
        Vec3 view = painter.camera().subtract(at);
        if (view.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] across = Vectors.across(view.normalize());
        double pulse = 0.8 + 0.2 * Math.sin(spin * 0.9);
        painter.flare(at, size * 0.8 * pulse, strength * 0.8);
        for (int k = 0; k < 8; k++) {
            boolean big = k % 2 == 0;
            double angle = (big ? spin * 0.12 : -spin * 0.09) + Math.PI * 0.25 * k;
            Vec3 ray = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)))
                    .scale(size * (big ? 1.9 : 0.8) * pulse);
            painter.edge(at, at.add(ray), size * (big ? 0.07 : 0.05), strength * (big ? 0.9 : 0.6));
        }
    }

    // ---- Broken up ----

    /**
     * A hand its maker let go of before it was done: it breaks into solid pieces where it was, fingers and all, flung
     * apart, and they tumble down. A pair breaks up alike, both hands and the axe, and its portals shrink shut.
     *
     * @param since ticks since it was let go
     */
    public static void broken(LanternPainter painter, ConstructPayload hand, double clock, double since) {
        double apart = since / BREAK_TICKS;
        if (apart >= 1.0) {
            return;
        }
        Vec3 facing = hand.facing();
        Vec3 base = hand.center();
        double scale = Math.max(0.1, hand.size());
        if (HandPose.move(hand.variant()) == HandPose.AXE) {
            brokenPair(painter, hand, clock, since, apart, scale);
            return;
        }
        double reach = Math.sqrt(facing.x * facing.x + facing.z * facing.z) / scale;
        HandPose pose = HandPose.at(hand.variant(), clock, reach);
        HandPose.Place place = pose.place(base, facing, scale);
        // What light it was making dies away where it was.
        double fade = 1.0 - Ease.smooth(since / 6.0);
        ground(painter, hand.id(), base, clock, hand.variant(), scale, fade);
        blows(painter, hand, facing, clock, scale, fade);
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 5.0));
        painter.ambient(GLOWS);
        painter.fling(1.8);
        painter.clip(new Vec3(base.x, base.y + GROUND_CUT, base.z), Vectors.UP, GROUND_SEAM);
        drawHand(painter, pose, place, false, 1.2, apart, 0, false);
        painter.noClip();
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
    }

    /**
     * A pair its maker let go of: both hands, still cut off at their portals, and the axe break up (see broken), the
     * portals shrink shut, flashing as they go, and what light the pair was making dies away where it was.
     */
    private static void brokenPair(LanternPainter painter, ConstructPayload hand, double clock, double since,
            double apart, double scale) {
        Vec3 base = hand.center();
        int variant = hand.variant();
        Vec3 aim = base.add(hand.facing());
        HandDuo duo = HandDuo.at(base, variant, aim, clock, scale);
        int seed = hand.id() * 7;
        double fade = 1.0 - Ease.smooth(since / 6.0);
        flashes(painter, duo, clock, fade);
        lights(painter, seed, base, variant, aim, duo, clock, scale, fade);
        double keep = 1.0 - Ease.smooth(apart * 1.4);
        double pop = Ease.smooth((apart - 0.45) / 0.25) * (1.0 - Ease.smooth((apart - 0.72) / 0.25));
        HandDuo.Portal[] portals = { duo.rightPortal, duo.leftPortal, duo.axePortal };
        for (int i = 0; i < portals.length; i++) {
            HandDuo.Portal portal = portals[i];
            portal(painter, portal, seed + i, clock + since, keep);
            if (pop > 0.01 && portal.open() > 0.01) {
                painter.flare(portal.center(), portal.radius() * Math.min(1.0, portal.open()) * 0.8 * pop, pop);
            }
        }
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 5.0));
        painter.ambient(GLOWS);
        painter.fling(1.8);
        if (duo.handsThere) {
            pairHand(painter, duo.rightPortal, duo.rightPose, duo.rightPlace, false, 1.2, apart, 0);
            pairHand(painter, duo.leftPortal, duo.leftPose, duo.leftPlace, true, 1.2, apart, LEFT_PIECES);
        }
        ConstructPainter.Frame frame = ConstructPainter.Frame.of(duo.axeEnd, duo.axeFace, duo.axeUp, scale);
        if (duo.axeThere) {
            axe(painter, duo, base, frame, apart);
        }
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
        if (duo.axeThere) {
            axeCracks(painter, frame, seed, clock, apart);
        }
    }

    /** How long a hand stays drawn broken up after its maker let go of it, in ticks. */
    public static int breakTicks() {
        return (int) BREAK_TICKS;
    }
}
