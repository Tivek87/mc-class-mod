package nl.tivek.multiversepowers.character.greenlantern.client.mech.shape;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.client.render.mesh.Surface;
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechHandRig.FINGER_LENGTHS;
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechHandRig.FINGER_X;
import static nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechHandRig.THUMB_LENGTHS;

// The right arm (the left one is its mirror). The upper arm runs up its y from the shoulder joint to the elbow, x
// outwards; the forearm runs up y from the elbow to the wrist and the hand on from there to the finger tips, the palm
// facing z, the thumb at x.
public final class MechArmShapes {
    public static final double WRIST = MechScript.FOREARM;
    public static final double KNUCKLES = WRIST + 1.24;
    // Two exhaust pipes run along the back of each forearm and sweep out past the elbow: their open ends and the way
    // they point, in the forearm's places (x across, y from the elbow to the wrist, the back of the hand at -z).
    public static final double[] PIPE_X = { -0.3, 0.3 };
    public static final double PIPE = 0.21;

    public static final Shape UPPER = Shape.of(upper());
    public static final Shape UPPER_LEFT = MechParts.mirrored(UPPER);
    public static final Shape FOREARM = Shape.of(forearm());
    public static final Shape FOREARM_LEFT = MechParts.mirrored(FOREARM);
    public static final Shape HAND = Shape.of(hand());
    public static final Shape HAND_LEFT = MechParts.mirrored(HAND);
    public static final Shape[][] FINGERS = new Shape[4][3];
    public static final Shape[][] FINGERS_LEFT = new Shape[4][3];
    public static final Shape[] THUMB = new Shape[3];
    public static final Shape[] THUMB_LEFT = new Shape[3];

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

    // The upper arm: a thick column plated on its outer side and in front, with the elbow's round joint at its end.
    private static Mesh[] upper() {
        double top = MechScript.UPPER_ARM;
        Surface upper = Surface.loft(MechParts.at(-0.3, 0.0, 0.0, 0.0, 2.0), MechParts.at(0.02, 0.64, 0.62, 0.0, 2.8),
                MechParts.at(0.6, 0.74, 0.7, 0.0, 3.2), MechParts.at(1.4, 0.72, 0.68, 0.0, 3.2),
                MechParts.at(top - 0.3, 0.62, 0.6, 0.0, 3.0), MechParts.at(top + 0.05, 0.0, 0.0, 0.0, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(upper, 12, 6, 81, 1.0));
        m.add(MechParts.hub(0.62, -0.8, 0.8, 0.95).moved(0.0, top, 0.0));
        MechParts.pair(m, MechParts.ring(0.56, 0.05, 0.82, 1.3).moved(0.0, top, 0.0));
        MechParts.pair(m, MechParts.disc(0.34, 0.8, 0.07, 1.15).moved(0.0, top, 0.0));
        Surface out = upper.offset(0.05, true);
        m.addAll(List.of(MechParts.plated(MechParts.part(out, -0.08, 0.08, 0.18, 0.78), 2, 4, 82, 0.14, 1.05)));
        m.addAll(List.of(MechParts.plated(MechParts.part(out, 0.18, 0.32, 0.25, 0.7), 2, 3, 83, 0.12, 1.03)));
        return m.toArray(Mesh[]::new);
    }

    // The gauntlet: a forearm bigger than the upper arm, squared off and swelling to its middle, a guard over the back
    // of the elbow, a plated ridge down its back and a ring at the wrist.
    private static Mesh[] forearm() {
        double wrist = WRIST;
        Surface fore = Surface.loft(MechParts.at(-0.85, 0.0, 0.0, 0.0, 2.0), MechParts.at(-0.6, 0.56, 0.56, 0.0, 2.6),
                MechParts.at(-0.15, 0.82, 0.8, 0.0, 3.6), MechParts.at(0.7, 0.96, 0.92, 0.0, 4.2),
                MechParts.at(1.6, 0.96, 0.9, 0.0, 4.2), MechParts.at(2.3, 0.82, 0.76, 0.0, 3.8),
                MechParts.at(wrist - 0.2, 0.66, 0.6, 0.0, 3.0), MechParts.at(wrist + 0.02, 0.0, 0.0, 0.0, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(fore, 16, 9, 91, 1.0, 0.075));
        Surface out = fore.offset(0.07, true);
        m.addAll(List.of(MechParts.plated(MechParts.part(out, 0.7, 0.8, 0.22, 0.84), 1, 4, 92, 0.16, 1.08)));
        m.addAll(List.of(MechParts.plated(MechParts.part(out, -0.1, 0.1, 0.3, 0.8), 2, 3, 93, 0.12, 1.04)));
        Surface guard = Surface.loft(MechParts.at(-0.75, 0.62, 0.66, 0.0, 3.4), MechParts.at(0.45, 0.98, 0.96, 0.0,
                4.2));
        m.addAll(List.of(MechParts.plated(MechParts.part(guard, 0.58, 0.92, 0.0, 1.0), 4, 2, 94, 0.14, 1.1)));
        m.add(Mesh.torus(24, 4, 0.7, 0.07, 1.3).scaled(1.0, 1.0, 0.94).moved(0.0, wrist - 0.24, 0.0));
        // The exhaust pipes, out of the gauntlet by the wrist, strapped down over its back and swept out past the
        // elbow like a motorcycle's, each ending in a flared, ringed mouth.
        for (double x : PIPE_X) {
            Vec3 end = pipeEnd(x);
            Vec3 way = pipeWay(x);
            m.add(Mesh.tube(false, 10, PIPE, 1.15, new Vec3(x, 2.3, -0.66), new Vec3(x, 1.9, -1.08),
                    new Vec3(x, 0.4, -1.16), new Vec3(x, -0.55, -1.12), bend(x), end));
            m.add(Mesh.cone(12, PIPE + 0.02, PIPE + 0.1, 0.0, 0.26, 1.2).pointing(way.x, way.y, way.z).moved(end.x,
                    end.y, end.z));
            m.add(Mesh.torus(12, 4, PIPE + 0.1, 0.05, 1.45).pointing(way.x, way.y, way.z).moved(end.x + way.x * 0.26,
                    end.y + way.y * 0.26, end.z + way.z * 0.26));
        }
        m.add(Mesh.bevel(-0.56, 0.95, -1.42, 0.56, 1.15, -0.96, 0.03, 1.1));
        m.add(Mesh.bevel(-0.56, -0.2, -1.4, 0.56, -0.02, -0.96, 0.03, 1.1));
        return m.toArray(Mesh[]::new);
    }

    // The open end of the pipe at `x` across the forearm, and the way it points.
    public static Vec3 pipeEnd(double x) {
        return new Vec3(x * 1.45, -1.55, -1.5);
    }

    public static Vec3 pipeWay(double x) {
        return pipeEnd(x).subtract(bend(x)).normalize();
    }

    private static Vec3 bend(double x) {
        return new Vec3(x * 1.3, -1.2, -1.32);
    }

    // The hand past the wrist: its palm, the plates on its back, the knuckles and the pads.
    private static Mesh[] hand() {
        double wrist = WRIST;
        List<Mesh> m = new ArrayList<>();
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
            // A spike on each knuckle, leaning towards the fingers, so a fist leads with them.
            m.add(Mesh.cone(4, 0.1, 0.0, 0.0, 0.36, 1.3).pointing(0.0, 0.5, -1.0).moved(x, KNUCKLES - 0.08,
                    -0.36));
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
