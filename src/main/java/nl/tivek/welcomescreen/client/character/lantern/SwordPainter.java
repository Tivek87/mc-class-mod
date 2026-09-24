package nl.tivek.welcomescreen.client.character.lantern;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * The sword and shield of the construct wheel, as hard light: solid like every construct, drawn wherever the hands that
 * hold them are (see {@link SwordArms}).
 * <ul>
 * <li>The sword: a plain knight's sword, its blade a flattened diamond with a bright fuller down its middle, a straight
 * crossguard with a knob at either end, a wrapped grip and a round pommel.</li>
 * <li>The shield: a heater shield, flat on top and coming to a point below, with a raised rim, a raised field on its
 * face, a boss in its middle and the lantern emblem over it, and straps on its back.</li>
 * </ul>
 * Both grow out of the ring's light, white-hot at first, and break into solid pieces when they are put away. A swung
 * blade leaves a streak of light in the air behind it: light, not a construct.
 */
final class SwordPainter {
    /** The sword, its grip round the middle: x across its flat, y towards its top edge, z to its tip. */
    private static final ConstructPainter.Shape SWORD = ConstructPainter.Shape.of(sword());
    /** The shield round its middle: x across it, y up, z out of its face. */
    private static final ConstructPainter.Shape SHIELD = ConstructPainter.Shape.of(shield());
    /** How long the blade is from the grip, and where it begins, at scale 1. */
    static final double TIP = 1.1;
    static final double BLADE_FROM = 0.13;
    // How far the pieces fly when they break up, next to how far pieces usually do.
    private static final double FLING = 1.3;

    private SwordPainter() {
    }

    private static Mesh[] sword() {
        Mesh blade = Mesh.loft(4, 1.05, new double[] { BLADE_FROM, 0.024, 0.062, 0.0 },
                new double[] { 0.3, 0.022, 0.058, 0.0 }, new double[] { 0.88, 0.017, 0.047, 0.0 },
                new double[] { 1.0, 0.012, 0.032, 0.0 }, new double[] { TIP, 0.0, 0.0, 0.0 });
        Mesh fuller = Mesh.box(-0.026, -0.011, 0.18, 0.026, 0.011, 0.8, 1.6);
        Mesh guard = Mesh.box(-0.034, -0.2, 0.085, 0.034, 0.2, 0.13, 1.15);
        Mesh knob = Mesh.ball(8, 5, 0.042, 1.3);
        Mesh grip = Mesh.cylinder(8, 0.032, -0.14, 0.085, 0.95).alongZ();
        Mesh[] bands = new Mesh[3];
        double[] at = { -0.09, -0.02, 0.05 };
        for (int k = 0; k < bands.length; k++) {
            bands[k] = Mesh.torus(10, 4, 0.034, 0.008, 1.3).alongZ().moved(0.0, 0.0, at[k]);
        }
        Mesh pommel = Mesh.ball(10, 6, 0.054, 1.35).moved(0.0, 0.0, -0.19);
        return new Mesh[] { blade, fuller, guard, knob.moved(0.0, 0.21, 0.107), knob.moved(0.0, -0.21, 0.107), grip,
                bands[0], bands[1], bands[2], pommel };
    }

    private static Mesh[] shield() {
        double[] outline = { 0.35, 0.4, 0.0, 0.43, -0.35, 0.4, -0.36, 0.1, -0.3, -0.15, -0.18, -0.34, 0.0, -0.47,
                0.18, -0.34, 0.3, -0.15, 0.36, 0.1 };
        double[] field = new double[outline.length];
        Vec3[] rim = new Vec3[outline.length / 2];
        for (int i = 0; i < outline.length; i += 2) {
            field[i] = outline[i] * 0.8;
            field[i + 1] = outline[i + 1] * 0.8 + 0.01;
            rim[i / 2] = new Vec3(outline[i], outline[i + 1], 0.035);
        }
        Mesh body = Mesh.prism(-0.035, 0.035, 1.0, outline);
        Mesh raised = Mesh.prism(0.035, 0.05, 1.08, field);
        Mesh edge = Mesh.tube(true, 6, 0.028, 1.35, rim);
        Mesh boss = Mesh.ball(12, 6, 0.09, 1.3).scaled(1.0, 1.0, 0.55).moved(0.0, 0.02, 0.05);
        Mesh ring = Mesh.torus(24, 5, 0.16, 0.022, 1.75).alongZ().moved(0.0, 0.02, 0.065);
        Mesh over = Mesh.box(-0.21, 0.13, 0.05, 0.21, 0.165, 0.075, 1.75);
        Mesh under = Mesh.box(-0.21, -0.125, 0.05, 0.21, -0.09, 0.075, 1.75);
        Mesh strap = Mesh.box(-0.2, 0.06, -0.075, 0.2, 0.11, -0.035, 0.9);
        Mesh handle = Mesh.box(-0.05, -0.12, -0.08, 0.05, 0.06, -0.035, 0.9);
        return new Mesh[] { body, raised, edge, boss, ring, over, under, strap, handle };
    }

    /**
     * The sword, its grip at {@code grip}, its blade along {@code forward} and its top edge towards {@code edge}.
     *
     * @param grown how far it has grown out of the ring's light, 0 to 1
     * @param apart 0 while whole; above that it is breaking up, gone at 1
     */
    static void sword(ConstructPainter painter, Vec3 grip, Vec3 forward, Vec3 edge, double scale, double grown,
            double apart) {
        ConstructPainter.Frame frame = frame(grip, forward, edge, scale * Math.max(grown, 1.0E-3));
        if (apart > 0.0) {
            painter.fling(FLING);
            painter.shattered(SWORD, frame, apart, 1.2);
            painter.fling(1.0);
            return;
        }
        if (grown <= 0.01) {
            return;
        }
        painter.glare(0.75 * (1.0 - grown));
        painter.shape(SWORD, frame, 1.0, 1.0);
        painter.glare(0.0);
        if (grown < 1.0) {
            // The light of the ring it grows out of, burning at the fist.
            painter.flare(grip, 0.3 * scale * (1.0 - grown), 1.0 - grown);
        }
    }

    /** The shield, its middle at {@code center}, its face towards {@code face} and its top towards {@code up}. */
    static void shield(ConstructPainter painter, Vec3 center, Vec3 face, Vec3 up, double scale, double grown,
            double apart) {
        ConstructPainter.Frame frame = frame(center, face, up, scale * Math.max(grown, 1.0E-3));
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
        painter.shape(SHIELD, frame, 1.0, 1.0);
        painter.glare(0.0);
    }

    /**
     * The streak a swung blade leaves in the air: a sheet of light between where its tip and the root of its blade were
     * over the last moments, newest first, fading out behind it.
     *
     * @param strength how strongly it shows, 0 to 1
     */
    static void trail(ConstructPainter painter, List<Vec3> tips, List<Vec3> roots, double strength) {
        int n = Math.min(tips.size(), roots.size());
        for (int i = 0; i + 1 < n; i++) {
            double a = strength * (1.0 - (double) i / (n - 1));
            double b = strength * (1.0 - (double) (i + 1) / (n - 1));
            painter.sheet(roots.get(i), tips.get(i), tips.get(i + 1), roots.get(i + 1), 0.25 * a, a, b, 0.25 * b);
            painter.edge(tips.get(i), tips.get(i + 1), 0.03, 0.8 * a);
        }
    }

    /** A frame at {@code center} facing {@code forward} with its up towards {@code up}, the way every construct stands. */
    private static ConstructPainter.Frame frame(Vec3 center, Vec3 forward, Vec3 up, double scale) {
        Vec3 ahead = forward.normalize();
        Vec3 top = up.subtract(ahead.scale(up.dot(ahead)));
        top = top.lengthSqr() < 1.0E-8 ? ConstructPainter.Frame.of(center, ahead, ConstructPainter.UP, 1.0).up()
                : top.normalize();
        return new ConstructPainter.Frame(center, ahead.cross(top).normalize(), top, ahead, scale);
    }
}
