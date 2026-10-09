package nl.tivek.multiversepowers.character.greenlantern.client.mech.shape;

import java.util.ArrayList;
import java.util.List;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.client.render.mesh.Surface;

// The right leg, x outwards (the left one is its mirror). The foot and the knee are drawn round their joint with y up
// and z ahead; the shin runs up its y from the ankle to the knee, the thigh from the knee to the hip.
public final class MechLegShapes {
    public static final double SHIN = MechScript.KNEE.subtract(MechScript.ANKLE).length();
    public static final double THIGH = MechScript.HIP.subtract(MechScript.KNEE).length();
    public static final double SOLE = -MechScript.ANKLE.y;

    public static final Shape FOOT = Shape.of(foot());
    public static final Shape FOOT_LEFT = MechParts.mirrored(FOOT);
    public static final Shape SHIN_PART = Shape.of(shin());
    public static final Shape SHIN_LEFT = MechParts.mirrored(SHIN_PART);
    public static final Shape KNEE = Shape.of(knee());
    public static final Shape KNEE_LEFT = MechParts.mirrored(KNEE);
    public static final Shape THIGH_PART = Shape.of(thigh());
    public static final Shape THIGH_LEFT = MechParts.mirrored(THIGH_PART);

    private MechLegShapes() {
    }

    // A jaeger's foot: a broad, heavy block of a sole, three squared toes and a heel, an armoured instep sloping down
    // from the ankle's round joint.
    private static Mesh[] foot() {
        double s = SOLE;
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.bevel(-0.78, s, -0.9, 0.78, s + 0.56, 0.8, 0.12, 0.9));
        m.add(MechParts.side(-0.66, 0.66, 0.1, 1.0, -0.62, s + 0.5, 0.95, s + 0.5, 0.82, s + 0.74, 0.0, s + 1.22,
                -0.48, s + 1.32));
        m.add(MechParts.side(-0.24, 0.24, 0.05, 1.14, -0.15, s + 1.25, 0.92, s + 0.72, 1.0, s + 0.8, 0.0,
                s + 1.36));
        for (int k = -1; k <= 1; k++) {
            double x = k * 0.5;
            m.add(MechParts.side(x - 0.22, x + 0.22, 0.07, 1.04, 0.72, s, 1.62 - 0.1 * Math.abs(k), s,
                    1.5 - 0.1 * Math.abs(k), s + 0.3, 0.72, s + 0.5));
            m.add(Mesh.bevel(x - 0.16, s + 0.3, 1.1, x + 0.16, s + 0.4, 1.42 - 0.1 * Math.abs(k), 0.03, 1.3));
        }
        m.add(MechParts.side(-0.5, 0.5, 0.08, 1.0, -1.32, s, -0.6, s, -0.6, s + 0.55, -1.12, s + 0.42));
        m.add(MechParts.hub(0.55, -0.8, 0.8, 0.95));
        MechParts.pair(m, MechParts.disc(0.44, 0.8, 0.07, 1.12));
        MechParts.pair(m, MechParts.disc(0.2, 0.86, 0.07, 1.4));
        MechParts.pair(m, MechParts.ring(0.5, 0.05, 0.82, 1.3));
        MechParts.pair(m, MechParts.ring(0.3, 0.035, 0.88, 1.3));
        MechParts.pair(m, MechParts.side(0.7, 0.84, 0.04, 1.02, -0.8, s + 0.5, 0.75, s + 0.5, 0.55, -0.15, -0.6,
                -0.12));
        return m.toArray(Mesh[]::new);
    }

    // The shin: a tall column, narrow at the ankle and swelling round the calf under the knee, a broad plated guard
    // down its whole front, a plate over the calf and a flared cuff over the ankle.
    private static Mesh[] shin() {
        double top = SHIN;
        Surface calf = Surface.loft(MechParts.at(-0.12, 0.0, 0.0, 0.02, 2.0),
                MechParts.at(0.06, 0.44, 0.46, 0.02, 3.0), MechParts.at(0.55, 0.52, 0.58, 0.0, 3.4),
                MechParts.at(1.25, 0.68, 0.8, -0.1, 3.8), MechParts.at(1.95, 0.8, 0.92, -0.17, 4.0),
                MechParts.at(2.5, 0.78, 0.86, -0.14, 3.8), MechParts.at(top - 0.05, 0.66, 0.72, -0.08, 3.4),
                MechParts.at(top + 0.25, 0.0, 0.0, -0.05, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(calf, 14, 9, 11, 1.0));
        Surface guard = MechParts.part(calf.offset(0.06, true), 0.16, 0.34, 0.1, 0.9);
        m.addAll(List.of(MechParts.plated(guard, 3, 7, 12, 0.14, 1.07)));
        Surface back = MechParts.part(calf.offset(0.05, true), 0.64, 0.86, 0.45, 0.85);
        m.addAll(List.of(MechParts.plated(back, 3, 3, 13, 0.12, 1.02)));
        m.add(Mesh.torus(24, 4, 0.68, 0.06, 1.35).scaled(1.0, 1.0, 1.1).moved(0.0, top - 0.1, -0.1));
        Surface cuff = Surface.loft(MechParts.at(0.15, 0.62, 0.66, 0.02, 3.4), MechParts.at(0.6, 0.56, 0.62, 0.0,
                3.4));
        m.addAll(List.of(MechParts.plated(cuff, 12, 1, 14, 0.1, 1.08)));
        return m.toArray(Mesh[]::new);
    }

    // The knee: a round joint under a big squared guard jutting out ahead of it.
    private static Mesh[] knee() {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.hub(0.6, -0.8, 0.8, 0.95));
        MechParts.pair(m, MechParts.ring(0.54, 0.05, 0.82, 1.3));
        MechParts.pair(m, MechParts.disc(0.36, 0.8, 0.07, 1.15));
        MechParts.pair(m, MechParts.disc(0.16, 0.86, 0.06, 1.4));
        Surface cap = Surface.loft(MechParts.at(-0.75, 0.0, 0.0, 0.0, 2.0), MechParts.at(-0.65, 0.6, 0.3, 0.0, 4.0),
                MechParts.at(-0.1, 0.74, 0.36, 0.05, 4.4), MechParts.at(0.55, 0.66, 0.32, 0.0, 4.0),
                MechParts.at(0.78, 0.0, 0.0, -0.05, 2.0)).moved(0.0, 0.05, 0.5);
        m.add(MechParts.skin(cap, 8, 4, 31, 1.08));
        m.add(MechParts.side(-0.1, 0.1, 0.04, 1.25, 0.78, -0.62, 0.98, -0.05, 0.94, 0.6, 0.7, 0.72));
        return m.toArray(Mesh[]::new);
    }

    // The thigh: thick and squared off, plated in front, on its outer side and behind.
    private static Mesh[] thigh() {
        double top = THIGH;
        Surface thigh = Surface.loft(MechParts.at(-0.25, 0.0, 0.0, 0.02, 2.0),
                MechParts.at(0.05, 0.62, 0.66, 0.02, 3.2), MechParts.at(0.6, 0.8, 0.86, 0.05, 3.8),
                MechParts.at(1.5, 0.9, 0.95, 0.04, 4.2), MechParts.at(2.4, 0.9, 0.95, 0.0, 4.2),
                MechParts.at(top, 0.8, 0.86, -0.04, 3.8), MechParts.at(top + 0.3, 0.0, 0.0, -0.05, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(thigh, 16, 8, 21, 1.0));
        Surface out = thigh.offset(0.06, true);
        m.addAll(List.of(MechParts.plated(MechParts.part(out, 0.16, 0.34, 0.2, 0.82), 3, 4, 22, 0.14, 1.06)));
        m.addAll(List.of(MechParts.plated(MechParts.part(out, -0.09, 0.09, 0.26, 0.86), 2, 4, 23, 0.14, 1.04)));
        m.addAll(List.of(MechParts.plated(MechParts.part(out, 0.66, 0.84, 0.3, 0.75), 3, 3, 24, 0.12, 1.02)));
        return m.toArray(Mesh[]::new);
    }
}
