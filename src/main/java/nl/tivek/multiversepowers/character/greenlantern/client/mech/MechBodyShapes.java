package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.client.render.Surface;

// The trunk round the mech's ground spot, x to its right, y up, z ahead: the pilot stands in an open cockpit in the
// chest behind an arch of keystones, inside a bubble of see-through light, as in the clip.
final class MechBodyShapes {
    static final double WINDOW_Y = 7.55;
    static final double WINDOW_IN = 0.88;
    static final double WINDOW_OUT = 1.34;
    static final double WINDOW_BACK = 1.82;
    static final double WINDOW_Z = 2.26;
    static final Vec3 CORE = new Vec3(0.0, WINDOW_Y, 1.0);
    static final double CORE_RADIUS = 1.35;
    static final double CHEST_TOP = 9.3;
    private static final double FLOOR = MechScript.COCKPIT.y;
    // Half the turn the window takes out of the front of the chest.
    private static final double OPEN = 0.07;
    private static final Surface CHEST_SKIN = Surface.loft(MechParts.at(5.85, 1.45, 1.12, 0.0, 2.6),
            MechParts.at(6.3, 1.98, 1.45, 0.05, 2.8), MechParts.at(7.1, 2.34, 1.7, 0.08, 3.0),
            MechParts.at(8.0, 2.5, 1.78, 0.05, 3.0), MechParts.at(8.65, 2.34, 1.62, 0.0, 2.8),
            MechParts.at(9.1, 1.8, 1.28, -0.05, 2.6), MechParts.at(CHEST_TOP, 0.0, 0.0, -0.08, 2.0));

    static final Shape PELVIS = Shape.of(pelvis());
    static final Shape WAIST = Shape.of(waist());
    static final Shape CHEST = Shape.of(chest());
    static final Shape ARCH = Shape.of(arch());
    static final Shape BUBBLE = Shape.of(Mesh.ball(28, 16, CORE_RADIUS, 1.0));
    static final Shape STICK = Shape.of(stick());
    static final Shape SHOULDER = Shape.of(shoulder());
    static final Shape SHOULDER_LEFT = MechParts.mirrored(SHOULDER);

    private MechBodyShapes() {
    }

    private static Mesh[] pelvis() {
        Vec3 hip = MechScript.HIP;
        Surface pelvis = Surface.loft(MechParts.at(4.2, 0.0, 0.0, -0.05, 2.0),
                MechParts.at(4.35, 1.15, 0.78, -0.05, 3.0), MechParts.at(4.65, 1.8, 1.02, -0.05, 3.4),
                MechParts.at(5.1, 1.96, 1.08, -0.05, 3.4), MechParts.at(5.42, 1.78, 1.0, -0.05, 3.2),
                MechParts.at(5.58, 0.0, 0.0, -0.05, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(pelvis, 22, 4, 41, 0.95));
        MechParts.pair(m, Mesh.ball(16, 10, 0.66, 1.0).moved(hip.x, hip.y, hip.z));
        m.add(MechParts.front(1.0, 1.36, 0.09, 1.08, -0.62, 5.4, 0.62, 5.4, 0.7, 4.8, 0.0, 4.02, -0.7, 4.8));
        m.add(MechParts.front(1.3, 1.44, 0.03, 1.3, -0.2, 5.25, 0.2, 5.25, 0.24, 4.75, 0.0, 4.35, -0.24, 4.75));
        Surface outside = pelvis.offset(0.05, true);
        m.addAll(List.of(MechParts.plated(MechParts.part(outside, -0.075, 0.075, 0.18, 0.8), 2, 2, 42, 0.12,
                1.05)));
        m.addAll(List.of(MechParts.plated(MechParts.part(outside, 0.425, 0.575, 0.18, 0.8), 2, 2, 43, 0.12,
                1.05)));
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] waist() {
        Surface waist = Surface.loft(MechParts.at(5.3, 0.0, 0.0, 0.0, 2.0), MechParts.at(5.42, 1.3, 0.96, 0.0, 2.6),
                MechParts.at(5.62, 1.42, 1.04, 0.01, 2.6), MechParts.at(5.78, 1.34, 0.99, 0.01, 2.6),
                MechParts.at(5.96, 1.46, 1.08, 0.02, 2.6), MechParts.at(6.1, 0.0, 0.0, 0.02, 2.0));
        return new Mesh[] { MechParts.skin(waist, 18, 3, 51, 0.95),
                Mesh.bevel(-0.1, 5.35, 0.95, 0.1, 6.0, 1.14, 0.03, 1.35) };
    }

    // The barrel of the chest round the cockpit, open in front for the window, closed above and below it.
    private static Mesh[] chest() {
        Surface chest = CHEST_SKIN;
        double floor = MechParts.along(chest, FLOOR);
        double brow = MechParts.along(chest, WINDOW_Y + WINDOW_IN + 0.12);
        List<Mesh> m = new ArrayList<>();
        m.addAll(List.of(MechParts.plated(MechParts.part(chest, 0.25 + OPEN, 1.25 - OPEN, 0.0, 1.0), 22, 9, 61,
                0.34, 1.0)));
        m.addAll(List.of(MechParts.plated(MechParts.part(chest, 0.25 - OPEN, 0.25 + OPEN, brow, 1.0), 3, 2, 62,
                0.34, 1.02)));
        m.addAll(List.of(MechParts.plated(MechParts.part(chest, 0.25 - OPEN, 0.25 + OPEN, 0.0, floor), 3, 1, 63,
                0.34, 1.0)));
        Surface outside = chest.offset(0.06, true);
        double pecTop = MechParts.along(chest, 8.55);
        double pecLow = MechParts.along(chest, 6.9);
        m.addAll(List.of(MechParts.plated(MechParts.part(outside, 0.25 + OPEN + 0.01, 0.25 + OPEN + 0.1, pecLow,
                pecTop), 2, 3, 64, 0.14, 1.06)));
        m.addAll(List.of(MechParts.plated(MechParts.part(outside, 0.25 - OPEN - 0.1, 0.25 - OPEN - 0.01, pecLow,
                pecTop), 2, 3, 65, 0.14, 1.06)));
        Surface wall = (u, v) -> new Vec3(1.05 - 2.1 * u, FLOOR + 0.05 + 2.2 * v, 0.42);
        m.addAll(List.of(MechParts.plated(wall, 3, 4, 66, 0.2, 0.82)));
        m.add(Mesh.bevel(-1.16, FLOOR - 0.25, 0.25, 1.16, FLOOR, 2.0, 0.04, 0.85));
        m.add(Mesh.torus(24, 4, 0.78, 0.07, 1.3).moved(0.0, CHEST_TOP - 0.12, -0.02));
        return m.toArray(Mesh[]::new);
    }

    // Keystones round the round top of the window and down its sides, like the rim of the clip's cockpit.
    private static Mesh[] arch() {
        List<Mesh> m = new ArrayList<>();
        int stones = 9;
        double gap = 0.03;
        for (int k = 0; k < stones; k++) {
            double a0 = Math.PI * k / stones + gap;
            double a1 = Math.PI * (k + 1) / stones - gap;
            double out = k == stones / 2 ? WINDOW_OUT + 0.14 : WINDOW_OUT;
            m.add(Mesh.slab(WINDOW_BACK, WINDOW_Z + (k == stones / 2 ? 0.08 : 0.0), 0.1,
                    k == stones / 2 ? 1.25 : 1.1, Math.cos(a0) * WINDOW_IN, WINDOW_Y + Math.sin(a0) * WINDOW_IN,
                    Math.cos(a0) * out, WINDOW_Y + Math.sin(a0) * out, Math.cos(a1) * out,
                    WINDOW_Y + Math.sin(a1) * out, Math.cos(a1) * WINDOW_IN, WINDOW_Y + Math.sin(a1) * WINDOW_IN));
        }
        int posts = 3;
        double post = (WINDOW_Y - FLOOR) / posts;
        for (int k = 0; k < posts; k++) {
            double y0 = FLOOR + k * post;
            MechParts.pair(m, Mesh.bevel(WINDOW_IN, y0 + 0.02, WINDOW_BACK, WINDOW_OUT, y0 + post - 0.02, WINDOW_Z,
                    0.09, 1.1));
        }
        m.add(Mesh.bevel(-WINDOW_OUT - 0.05, FLOOR - 0.3, WINDOW_BACK - 0.1, WINDOW_OUT + 0.05, FLOOR, WINDOW_Z + 0.06,
                0.09, 1.15));
        return m.toArray(Mesh[]::new);
    }

    // One control stick, round its foot on the cockpit floor.
    private static Mesh[] stick() {
        return new Mesh[] { Mesh.bevel(-0.12, 0.0, -0.12, 0.12, 0.1, 0.12, 0.03, 1.0),
                Mesh.cylinder(8, 0.045, 0.1, 0.78, 1.25), Mesh.cylinder(10, 0.07, 0.72, 0.92, 1.1),
                Mesh.ball(8, 5, 0.06, 1.7).moved(0.0, 0.95, 0.0) };
    }

    // The right pauldron: a tiled dome tipped outwards over the shoulder, two tiers under it and a horn on top.
    private static Mesh[] shoulder() {
        Vec3 joint = MechScript.SHOULDER;
        List<Mesh> m = new ArrayList<>();
        Surface dome = Surface.lathe(0.0, -0.72, 1.12, -0.68, 1.38, -0.36, 1.42, 0.0, 1.28, 0.4, 0.98, 0.72, 0.52,
                0.96, 0.0, 1.04).scaled(1.0, 1.0, 0.92).turned(0.0, 0.0, 1.0, -24.0)
                .moved(joint.x + 0.2, joint.y + 0.32, joint.z);
        m.add(MechParts.skin(dome, 18, 6, 71, 1.0));
        for (int k = 0; k < 2; k++) {
            double r = 1.3 - 0.16 * k;
            double y = -0.62 - 0.36 * k;
            Surface tier = Surface.lathe(0.0, y - 0.3, r, y - 0.24, r + 0.04, y, r - 0.1, y + 0.12, 0.0, y + 0.14)
                    .scaled(1.0, 1.0, 0.9).turned(0.0, 0.0, 1.0, -24.0).moved(joint.x + 0.2, joint.y + 0.32,
                            joint.z);
            m.add(MechParts.skin(tier, 16, 1, 72 + k, 1.02));
        }
        m.add(Mesh.ball(16, 10, 0.74, 0.95).moved(joint.x, joint.y, joint.z));
        m.add(MechParts.horn(0.26, 1.15, new Vec3(joint.x + 0.35, joint.y + 1.2, joint.z - 0.15),
                new Vec3(joint.x + 0.75, joint.y + 1.75, joint.z - 0.35), new Vec3(joint.x + 1.25, joint.y + 2.1,
                        joint.z - 0.55), new Vec3(joint.x + 1.75, joint.y + 2.2, joint.z - 0.7)));
        m.add(MechParts.side(-0.08, 0.08, 0.04, 1.15, 0.95, 0.0, -0.55, 0.0, -0.95, 0.75, 0.2, 0.62)
                .turned(0.0, 0.0, 1.0, -30.0).moved(joint.x + 0.55, joint.y + 1.05, joint.z));
        return m.toArray(Mesh[]::new);
    }
}
