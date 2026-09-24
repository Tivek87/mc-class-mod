package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
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
 * out to its point, the shield out of its middle, spreading sideways first. They break into solid pieces when they are
 * put away. A swung blade leaves a streak of light in the air behind it: light, not a construct.
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
    /** The shield round its middle: x across it, y up, z out of its face. */
    private static final ConstructPainter.Shape SHIELD = ConstructPainter.Shape.of(shield());
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

    private static Mesh[] shield() {
        List<Mesh> parts = new ArrayList<>();
        double bulge = 0.07;
        double[] outline = outline(1.0);
        // The body, bowed out to the front.
        parts.add(Mesh.dish(5, -0.03, 0.03, bulge, 1.0, outline));
        // The round rim all round it, and the raised border inside it.
        parts.add(Mesh.tube(true, 6, 0.034, 1.4, ring(outline, 0.0)));
        double[] inner = outline(0.8);
        parts.add(Mesh.tube(true, 5, 0.013, 1.55, ring(inner, 0.03 + bulge * (1.0 - 0.64) + 0.006)));
        // A ring of rivets between the rim and the border.
        double[] rivets = outline(0.9);
        int count = rivets.length / 2;
        for (int i = 0; i < count; i += 2) {
            parts.add(Mesh.ball(8, 5, 0.019, 1.5).moved(rivets[2 * i], rivets[2 * i + 1],
                    0.03 + bulge * (1.0 - 0.81) + 0.004));
        }
        // The boss in its middle and the lantern emblem round it: a ring with a bar over and under it.
        parts.add(Mesh.ball(14, 8, 0.1, 1.3).scaled(1.0, 1.0, 0.55).moved(0.0, 0.02, 0.095));
        parts.add(Mesh.torus(28, 6, 0.2, 0.028, 1.75).alongZ().moved(0.0, 0.02, 0.095));
        parts.add(Mesh.box(-0.25, 0.235, 0.075, 0.25, 0.275, 0.105, 1.75));
        parts.add(Mesh.box(-0.25, -0.235, 0.075, 0.25, -0.195, 0.105, 1.75));
        // On its back (hollow, as the body bows out to the front): the pad the forearm lies on, across the shield from
        // the elbow on its left to the fist on its right, the strap over the forearm by the elbow, and the grip for the
        // fist, square to the forearm.
        parts.add(Mesh.box(-0.3, -0.06, 0.008, 0.06, 0.06, 0.036, 0.95));
        parts.add(Mesh.tube(false, 6, 0.016, 1.3, new Vec3(-0.2, 0.085, 0.027), new Vec3(-0.2, 0.08, -0.05),
                new Vec3(-0.2, -0.08, -0.05), new Vec3(-0.2, -0.085, 0.027)));
        parts.add(Mesh.tube(false, 8, 0.02, 1.25, new Vec3(GRIP_X, 0.105, 0.032), new Vec3(GRIP_X, 0.09, GRIP_Z),
                new Vec3(GRIP_X, -0.09, GRIP_Z), new Vec3(GRIP_X, -0.105, 0.032)));
        // The back of the boss, with ribs of light running out from it all round over the hollow of the back.
        parts.add(Mesh.torus(20, 5, 0.09, 0.018, 1.7).alongZ().moved(0.0, 0.02, 0.035));
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI * 2.0 * (k + 0.5) / 8.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            parts.add(Mesh.tube(false, 4, 0.009, 1.9, new Vec3(cos * 0.13, 0.02 + sin * 0.13, 0.034),
                    new Vec3(cos * 0.36, 0.02 + sin * 0.4, 0.004)));
        }
        return parts.toArray(Mesh[]::new);
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
        ConstructPainter.Frame frame = frame(grip, forward, edge, scale);
        if (apart > 0.0) {
            painter.fling(FLING);
            painter.shattered(SWORD, frame, apart, 1.2);
            painter.fling(1.0);
            return;
        }
        if (grown <= 0.01) {
            return;
        }
        // Growing: the hilt takes its full width first, and the blade runs out of it to its point.
        double wide = Math.min(1.0, grown * 1.8);
        painter.glare(0.75 * (1.0 - grown));
        painter.ambient(0.12);
        painter.shape(SWORD, grown < 1.0 ? frame.stretched(wide, wide, grown) : frame, 1.0, 1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
        if (grown < 1.0) {
            // The light of the ring it grows out of, burning at the fist and at the point as it runs out.
            painter.flare(grip, 0.35 * scale * (1.0 - grown), 1.0 - grown);
            painter.flare(grip.add(forward.normalize().scale(TIP * scale * grown)), 0.2 * scale, 1.0 - grown);
        }
    }

    /** The shield, its middle at {@code center}, its face towards {@code face} and its top towards {@code up}. */
    public static void shield(LanternPainter painter, Vec3 center, Vec3 face, Vec3 up, double scale, double grown,
            double apart) {
        ConstructPainter.Frame frame = frame(center, face, up, scale);
        if (grown < 1.0 && apart <= 0.0) {
            // Growing out of its middle, spreading sideways first.
            double g = Math.max(grown, 1.0E-3);
            frame = frame.stretched(Math.min(1.0, g * 1.6), g, Math.min(1.0, g * 1.6));
        }
        if (apart > 0.0) {
            painter.fling(FLING);
            painter.shattered(SHIELD, frame, apart, 1.2);
            painter.fling(1.0);
            return;
        }
        if (grown <= 0.01) {
            return;
        }
        painter.glare(0.75 * (1.0 - grown));
        painter.ambient(0.12);
        painter.shape(SHIELD, frame, 1.0, 1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
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
