package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.client.render.Surface;

// The right arm (the left one is its mirror). The upper arm runs up its y from the shoulder joint to the elbow, x
// outwards; the forearm and hand run up y from the elbow to the finger tips, the palm facing z, the thumb at x.
final class MechArmShapes {
    static final double WRIST = MechScript.FOREARM;
    static final double KNUCKLES = WRIST + 1.24;
    static final double[] FINGER_X = { 0.47, 0.16, -0.16, -0.47 };
    static final double[][] FINGER_LENGTHS = { { 0.5, 0.42, 0.36 }, { 0.55, 0.45, 0.38 }, { 0.5, 0.42, 0.36 },
            { 0.42, 0.36, 0.3 } };
    static final Vec3 THUMB_ROOT = new Vec3(0.62, WRIST + 0.36, 0.1);
    static final double[] THUMB_LENGTHS = { 0.5, 0.42, 0.34 };

    static final Shape UPPER = Shape.of(upper());
    static final Shape UPPER_LEFT = MechParts.mirrored(UPPER);
    static final Shape FOREARM = Shape.of(forearm());
    static final Shape FOREARM_LEFT = MechParts.mirrored(FOREARM);
    static final Shape[][] FINGERS = new Shape[4][3];
    static final Shape[][] FINGERS_LEFT = new Shape[4][3];
    static final Shape[] THUMB = new Shape[3];
    static final Shape[] THUMB_LEFT = new Shape[3];

    static {
        for (int k = 0; k < 4; k++) {
            for (int j = 0; j < 3; j++) {
                FINGERS[k][j] = segment(FINGER_LENGTHS[k][j], 0.145, 0.165, j == 2);
                FINGERS_LEFT[k][j] = MechParts.mirrored(FINGERS[k][j]);
            }
        }
        for (int j = 0; j < 3; j++) {
            THUMB[j] = segment(THUMB_LENGTHS[j], 0.17, 0.18, j == 2);
            THUMB_LEFT[j] = MechParts.mirrored(THUMB[j]);
        }
    }

    private MechArmShapes() {
    }

    private static Mesh[] upper() {
        double top = MechScript.UPPER_ARM;
        Surface upper = Surface.loft(MechParts.at(-0.3, 0.0, 0.0, 0.0, 2.0), MechParts.at(0.02, 0.58, 0.58, 0.0, 2.3),
                MechParts.at(0.5, 0.67, 0.66, 0.0, 2.5), MechParts.at(1.15, 0.65, 0.64, 0.0, 2.5),
                MechParts.at(top - 0.3, 0.56, 0.57, 0.0, 2.3), MechParts.at(top + 0.05, 0.0, 0.0, 0.0, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(upper, 12, 5, 81, 1.0));
        m.add(MechParts.hub(0.55, -0.72, 0.72, 0.95).moved(0.0, top, 0.0));
        MechParts.pair(m, MechParts.ring(0.5, 0.05, 0.74, 1.3).moved(0.0, top, 0.0));
        MechParts.pair(m, MechParts.disc(0.3, 0.72, 0.07, 1.15).moved(0.0, top, 0.0));
        Surface outer = MechParts.part(upper.offset(0.05, true), -0.07, 0.07, 0.2, 0.75);
        m.addAll(List.of(MechParts.plated(outer, 2, 3, 82, 0.12, 1.05)));
        return m.toArray(Mesh[]::new);
    }

    // The gauntlet: a thick forearm of rings of blocks, round at the elbow, with the hand's palm, knuckles and plates.
    private static Mesh[] forearm() {
        double wrist = WRIST;
        Surface fore = Surface.loft(MechParts.at(-0.8, 0.0, 0.0, 0.0, 2.0), MechParts.at(-0.56, 0.52, 0.52, 0.0, 2.2),
                MechParts.at(-0.15, 0.76, 0.74, 0.0, 2.5), MechParts.at(0.55, 0.86, 0.82, 0.0, 2.7),
                MechParts.at(1.25, 0.84, 0.8, 0.0, 2.7), MechParts.at(1.85, 0.74, 0.7, 0.0, 2.6),
                MechParts.at(wrist - 0.2, 0.6, 0.56, 0.0, 2.4), MechParts.at(wrist + 0.02, 0.0, 0.0, 0.0, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(fore, 14, 8, 91, 1.0, 0.075));
        Surface ridge = MechParts.part(fore.offset(0.07, true), 0.7, 0.8, 0.2, 0.82);
        m.addAll(List.of(MechParts.plated(ridge, 1, 4, 92, 0.14, 1.08)));
        m.add(Mesh.torus(24, 4, 0.64, 0.06, 1.3).scaled(1.0, 1.0, 0.94).moved(0.0, wrist - 0.24, 0.0));
        m.add(Mesh.bevel(-0.64, wrist - 0.05, -0.28, 0.64, KNUCKLES, 0.3, 0.11, 1.0));
        for (int col = 0; col < 3; col++) {
            for (int row = 0; row < 3; row++) {
                double x0 = -0.6 + col * 0.41;
                double y0 = wrist + 0.08 + row * 0.38;
                m.add(Mesh.bevel(x0, y0, -0.37, x0 + 0.37, y0 + 0.34, -0.22, 0.045, 1.08 + 0.04 * row));
            }
        }
        for (double x : FINGER_X) {
            m.add(Mesh.bevel(x - 0.15, KNUCKLES - 0.22, -0.38, x + 0.15, KNUCKLES + 0.06, -0.08, 0.05, 1.2));
        }
        m.add(Mesh.bevel(-0.52, wrist + 0.16, 0.24, 0.52, KNUCKLES - 0.14, 0.36, 0.05, 0.9));
        m.add(Mesh.bevel(0.3, wrist + 0.1, 0.02, 0.72, wrist + 0.72, 0.3, 0.06, 1.0));
        return m.toArray(Mesh[]::new);
    }

    // A finger joint: from its knuckle along y, the palm side at z, a plate on its back; the last one ends in a tip.
    private static Shape segment(double length, double half, double deep, boolean tip) {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.hub(half + 0.01, -half - 0.01, half + 0.01, 0.95));
        double end = tip ? length - 0.08 : length - 0.02;
        m.add(Mesh.bevel(-half, 0.03, -deep, half, end, deep, 0.06, 1.02));
        if (tip) {
            m.add(Mesh.bevel(-half * 0.8, end - 0.02, -deep * 0.85, half * 0.8, length, deep * 0.7, 0.045, 1.15));
        }
        m.add(Mesh.bevel(-half * 0.72, length * 0.18, -deep - 0.06, half * 0.72, length * 0.78, -deep + 0.02, 0.03,
                1.25));
        m.add(Mesh.bevel(-half * 0.6, length * 0.3, deep - 0.02, half * 0.6, length * 0.7, deep + 0.04, 0.025, 1.0));
        return Shape.of(m.toArray(Mesh[]::new));
    }
}
