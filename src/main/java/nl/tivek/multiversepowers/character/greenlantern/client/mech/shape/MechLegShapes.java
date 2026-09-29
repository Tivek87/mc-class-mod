package nl.tivek.multiversepowers.character.greenlantern.client.mech.shape;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
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

    // A clawed foot as in the clip: three splayed toes of two joints each, a heel claw, an armoured instep over the
    // ankle's round joint.
    private static Mesh[] foot() {
        double s = SOLE;
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.bevel(-0.62, s, -0.7, 0.62, s + 0.5, 0.45, 0.1, 0.9));
        m.add(MechParts.side(-0.55, 0.55, 0.09, 1.0, -0.55, s + 0.46, 0.7, s + 0.46, 0.95, s + 0.58, 0.45,
                s + 0.98, -0.3, s + 1.12));
        m.add(MechParts.side(-0.28, 0.28, 0.05, 1.12, -0.1, s + 1.05, 0.85, s + 0.66, 0.92, s + 0.74, 0.4,
                s + 1.08));
        for (int k = -1; k <= 1; k++) {
            toe(m, k * 0.42, k * 18.0, k == 0 ? 0.12 : 0.0, k == 0 ? 0.22 : 0.19);
        }
        m.add(MechParts.side(-0.26, 0.26, 0.07, 1.0, -0.5, s, -1.25, s, -0.95, s + 0.4, -0.52, s + 0.5));
        m.add(MechParts.side(-0.16, 0.16, 0.04, 1.15, -1.0, s + 0.35, -1.25, s + 0.05, -1.42, s + 0.02, -1.15,
                s + 0.3));
        m.add(MechParts.hub(0.48, -0.72, 0.72, 0.95));
        MechParts.pair(m, MechParts.disc(0.38, 0.72, 0.07, 1.12));
        MechParts.pair(m, MechParts.disc(0.18, 0.78, 0.07, 1.4));
        MechParts.pair(m, MechParts.ring(0.45, 0.045, 0.74, 1.3));
        MechParts.pair(m, MechParts.ring(0.28, 0.03, 0.8, 1.3));
        MechParts.pair(m, MechParts.side(0.64, 0.76, 0.04, 1.02, -0.7, -0.78, 0.65, -0.78, 0.52, -0.2, -0.55,
                -0.18));
        MechParts.pair(m, MechParts.bar(0.06, 1.25, new Vec3(0.36, s + 0.42, -0.62), new Vec3(0.3, 1.0, -0.45)));
        MechParts.pair(m, MechParts.bar(0.045, 1.3, new Vec3(0.36, s + 0.42, -0.62), new Vec3(0.36, s + 0.2,
                -1.0)));
        return m.toArray(Mesh[]::new);
    }

    // One toe from its root at z 0.35, splayed outwards by so many degrees, a root joint and a claw.
    private static void toe(List<Mesh> m, double x, double splay, double reach, double half) {
        double s = SOLE;
        List<Mesh> toe = new ArrayList<>();
        toe.add(MechParts.side(-half, half, 0.07, 1.02, 0.0, s, 0.62 + reach, s, 0.58 + reach, s + 0.46, 0.0,
                s + 0.6));
        toe.add(MechParts.side(-half * 0.92, half * 0.92, 0.05, 1.12, 0.52 + reach, s, 1.08 + reach, s,
                0.92 + reach, s + 0.22, 0.56 + reach, s + 0.42));
        toe.add(MechParts.side(-half * 0.55, half * 0.55, 0.03, 1.3, 0.15, s + 0.56, 0.52 + reach, s + 0.45,
                0.58 + reach, s + 0.51, 0.2, s + 0.65));
        for (Mesh part : toe) {
            m.add(part.turned(0.0, 1.0, 0.0, splay).moved(x, 0.0, 0.35));
        }
    }

    // The shin: a tall column, narrow at the ankle and swelling round the calf to a rounded top under the knee, with a
    // plated guard down its front and a plate over the calf.
    private static Mesh[] shin() {
        double top = SHIN;
        Surface calf = Surface.loft(MechParts.at(-0.08, 0.0, 0.0, 0.02, 2.0),
                MechParts.at(0.08, 0.34, 0.36, 0.02, 2.2), MechParts.at(0.42, 0.42, 0.48, 0.0, 2.3),
                MechParts.at(0.88, 0.57, 0.68, -0.08, 2.4), MechParts.at(1.32, 0.68, 0.8, -0.15, 2.5),
                MechParts.at(1.72, 0.7, 0.8, -0.16, 2.5), MechParts.at(top - 0.1, 0.62, 0.7, -0.12, 2.4),
                MechParts.at(top + 0.2, 0.0, 0.0, -0.1, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(calf, 12, 7, 11, 1.0));
        Surface guard = MechParts.part(calf.offset(0.05, true), 0.2, 0.3, 0.12, 0.88);
        m.addAll(List.of(MechParts.plated(guard, 2, 6, 12, 0.12, 1.06)));
        Surface back = MechParts.part(calf.offset(0.05, true), 0.66, 0.84, 0.46, 0.8);
        m.addAll(List.of(MechParts.plated(back, 3, 3, 13, 0.1, 1.02)));
        m.add(Mesh.torus(24, 4, 0.64, 0.05, 1.35).scaled(1.0, 1.0, 1.1).moved(0.0, top - 0.1, -0.12));
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] knee() {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.hub(0.5, -0.72, 0.72, 0.95));
        MechParts.pair(m, MechParts.ring(0.45, 0.045, 0.74, 1.3));
        MechParts.pair(m, MechParts.disc(0.3, 0.72, 0.07, 1.15));
        MechParts.pair(m, MechParts.disc(0.14, 0.78, 0.06, 1.4));
        Surface cap = Surface.lathe(0.0, -0.1, 0.62, 0.0, 0.62, 0.14, 0.48, 0.34, 0.26, 0.45, 0.0, 0.5)
                .turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 0.08, 0.28);
        m.add(MechParts.skin(cap, 10, 3, 31, 1.08));
        m.add(MechParts.side(-0.1, 0.1, 0.04, 1.25, 0.64, 0.55, 0.86, 0.1, 0.8, -0.4, 0.52, -0.62));
        return m.toArray(Mesh[]::new);
    }

    // The thigh, thick and squared off like the clip's, plated in front and on its outer side.
    private static Mesh[] thigh() {
        double top = THIGH;
        Surface thigh = Surface.loft(MechParts.at(-0.2, 0.0, 0.0, 0.02, 2.0),
                MechParts.at(0.05, 0.55, 0.58, 0.02, 2.5), MechParts.at(0.45, 0.76, 0.8, 0.05, 2.8),
                MechParts.at(1.0, 0.84, 0.86, 0.04, 3.0), MechParts.at(1.5, 0.82, 0.84, 0.0, 3.0),
                MechParts.at(top, 0.72, 0.75, -0.04, 2.7), MechParts.at(top + 0.3, 0.0, 0.0, -0.05, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(thigh, 16, 6, 21, 1.0));
        Surface front = MechParts.part(thigh.offset(0.06, true), 0.17, 0.33, 0.24, 0.78);
        m.addAll(List.of(MechParts.plated(front, 3, 3, 22, 0.12, 1.06)));
        Surface outer = MechParts.part(thigh.offset(0.06, true), -0.08, 0.08, 0.3, 0.82);
        m.addAll(List.of(MechParts.plated(outer, 2, 3, 23, 0.12, 1.04)));
        return m.toArray(Mesh[]::new);
    }
}
