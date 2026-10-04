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
    public static final double[] PIPE_X = { -0.25, 0.25 };
    private static final double PIPE = 0.13;

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

    // The gauntlet: a thick forearm of rings of blocks, round at the elbow, ringed at the wrist.
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
        // The exhaust pipes, out of the gauntlet by the wrist, strapped down over its back and swept out past the
        // elbow like a motorcycle's, each ending in a ringed mouth.
        for (double x : PIPE_X) {
            Vec3 end = pipeEnd(x);
            Vec3 way = pipeWay(x);
            m.add(Mesh.tube(false, 8, PIPE, 1.15, new Vec3(x, 1.95, -0.6), new Vec3(x, 1.55, -0.94),
                    new Vec3(x, 0.4, -1.02), new Vec3(x, -0.5, -0.98), bend(x), end));
            m.add(Mesh.torus(10, 4, PIPE + 0.04, 0.04, 1.4).pointing(way.x, way.y, way.z).moved(end.x, end.y,
                    end.z));
        }
        m.add(Mesh.bevel(-0.44, 0.82, -1.2, 0.44, 1.0, -0.84, 0.03, 1.1));
        m.add(Mesh.bevel(-0.44, -0.22, -1.17, 0.44, -0.06, -0.82, 0.03, 1.1));
        return m.toArray(Mesh[]::new);
    }

    // The open end of the pipe at `x` across the forearm, and the way it points.
    public static Vec3 pipeEnd(double x) {
        return new Vec3(x * 1.4, -1.35, -1.35);
    }

    public static Vec3 pipeWay(double x) {
        return pipeEnd(x).subtract(bend(x)).normalize();
    }

    private static Vec3 bend(double x) {
        return new Vec3(x * 1.25, -1.05, -1.15);
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
