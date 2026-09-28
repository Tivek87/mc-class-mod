package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.client.render.Surface;

// The trunk round the mech's ground spot, x to its right, y up, z ahead: the pilot sits in a cockpit deep in the chest,
// looking out through a round port of see-through green glass set in a thick flange of blocks, as in the clip.
final class MechBodyShapes {
    static final double PORT_Y = 7.55;
    static final double PORT_IN = 1.05;
    static final double PORT_OUT = 1.62;
    static final double PORT_BACK = 1.0;
    static final double PORT_FRONT = 2.0;
    static final double GLASS_Z = 1.86;
    static final double GLASS_BULGE = 0.27;
    static final Vec3 CORE = new Vec3(0.0, PORT_Y, 1.0);
    static final double CORE_RADIUS = 1.35;
    static final double CHEST_TOP = 9.3;
    private static final double FLOOR = MechScript.COCKPIT.y;
    private static final double CABIN_BACK = -0.32;
    private static final double CABIN_SIDE = 1.3;
    private static final double CABIN_TOP = 9.0;
    private static final double HOLE_WIDE = 1.1;
    private static final double HOLE_HIGH = 1.15;
    private static final int RIM_BLOCKS = 18;
    private static final Surface CHEST_SKIN = Surface.loft(MechParts.at(5.85, 1.45, 1.12, 0.0, 2.6),
            MechParts.at(6.3, 1.98, 1.45, 0.05, 2.8), MechParts.at(7.1, 2.34, 1.7, 0.08, 3.0),
            MechParts.at(8.0, 2.5, 1.78, 0.05, 3.0), MechParts.at(8.65, 2.34, 1.62, 0.0, 2.8),
            MechParts.at(9.1, 1.8, 1.28, -0.05, 2.6), MechParts.at(CHEST_TOP, 0.0, 0.0, -0.08, 2.0));

    // Half the turn the port takes out of the front of the chest.
    private static final double OPEN = open();

    static final Shape PELVIS = Shape.of(pelvis());
    static final Shape WAIST = Shape.of(waist());
    static final Shape CHEST = Shape.of(chest());
    static final Shape RIM = Shape.of(rim());
    static final Shape GLASS = Shape.of(glass());
    static final Shape BUBBLE = Shape.of(Mesh.ball(28, 16, CORE_RADIUS, 1.0));
    static final Shape CABIN = Shape.of(cabin());
    static final Shape WALLS = Shape.of(walls());
    static final Shape SEAT = Shape.of(seat());
    static final Shape CONSOLE = Shape.of(console());
    static final Shape LEVER = Shape.of(lever());
    static final Shape BUTTON = Shape.of(Mesh.bevel(-0.07, -0.035, -0.06, 0.07, 0.035, 0.06, 0.02, 1.0));
    // The lamp on the upper left of the chest, between the port and the shoulder: a block it sits on, and the lamp
    // itself round its pivot, its length along y.
    static final Vec3 LAMP = new Vec3(-1.75, 8.45, 1.5);
    static final Shape LAMP_MOUNT = Shape.of(Mesh.bevel(LAMP.x - 0.24, LAMP.y - 0.22, 1.0, LAMP.x + 0.24,
            LAMP.y + 0.22, LAMP.z - 0.02, 0.05, 1.0), Mesh.ball(12, 8, 0.2, 1.0).moved(LAMP.x, LAMP.y, LAMP.z));
    static final Shape LAMP_HOUSING = Shape.of(Mesh.cylinder(14, 0.23, -0.05, 0.34, 1.0),
            Mesh.cylinder(14, 0.3, 0.26, 0.42, 1.02), Mesh.cylinder(14, 0.16, -0.22, -0.05, 0.96));
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

    // How far round from the front of the chest its surface is HOLE_WIDE out to the side, at the port's height.
    private static double open() {
        double v = MechParts.along(CHEST_SKIN, PORT_Y);
        double low = 0.0;
        double high = 0.2;
        boolean rightwards = Math.abs(CHEST_SKIN.at(0.3, v).x) > Math.abs(CHEST_SKIN.at(0.25, v).x);
        for (int k = 0; k < 40; k++) {
            double middle = (low + high) * 0.5;
            if (Math.abs(CHEST_SKIN.at(0.25 + (rightwards ? middle : -middle), v).x) < HOLE_WIDE) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) * 0.5;
    }

    // The barrel of the chest round the cockpit, open in front for the port, closed above and below it.
    private static Mesh[] chest() {
        Surface chest = CHEST_SKIN;
        double floor = MechParts.along(chest, PORT_Y - HOLE_HIGH);
        double brow = MechParts.along(chest, PORT_Y + HOLE_HIGH);
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
        m.add(Mesh.torus(24, 4, 0.78, 0.07, 1.3).moved(0.0, CHEST_TOP - 0.12, -0.02));
        return m.toArray(Mesh[]::new);
    }

    // The port's flange: a ring of heavy blocks round the glass, every other one standing a little further out.
    private static Mesh[] rim() {
        List<Mesh> m = new ArrayList<>();
        double gap = 0.012;
        for (int k = 0; k < RIM_BLOCKS; k++) {
            double a0 = Math.PI * 2.0 * k / RIM_BLOCKS + gap;
            double a1 = Math.PI * 2.0 * (k + 1) / RIM_BLOCKS - gap;
            double out = k % 2 == 0 ? PORT_OUT : PORT_OUT - 0.08;
            double front = k % 2 == 0 ? PORT_FRONT + 0.08 : PORT_FRONT;
            m.add(Mesh.slab(PORT_BACK, front, 0.08, k % 2 == 0 ? 1.12 : 1.0, Math.cos(a0) * PORT_IN,
                    PORT_Y + Math.sin(a0) * PORT_IN, Math.cos(a0) * out, PORT_Y + Math.sin(a0) * out,
                    Math.cos(a1) * out, PORT_Y + Math.sin(a1) * out, Math.cos(a1) * PORT_IN,
                    PORT_Y + Math.sin(a1) * PORT_IN));
        }
        m.add(Mesh.torus(36, 4, PORT_IN + 0.02, 0.06, 1.35).alongZ().moved(0.0, PORT_Y, GLASS_Z));
        m.add(Mesh.torus(36, 4, PORT_OUT - 0.02, 0.05, 1.2).alongZ().moved(0.0, PORT_Y, PORT_FRONT + 0.02));
        for (int k = 0; k < 4; k++) {
            double a = Math.PI * 0.5 * k + Math.PI * 0.25;
            double r = (PORT_IN + PORT_OUT) * 0.5;
            m.add(MechParts.disc(0.1, PORT_FRONT + 0.08, 0.05, 1.4).turned(0.0, 1.0, 0.0, -90.0)
                    .moved(Math.cos(a) * r, PORT_Y + Math.sin(a) * r, 0.0));
        }
        return m.toArray(Mesh[]::new);
    }

    // The glass: a shallow dome bulging out of the port.
    private static Mesh[] glass() {
        int rings = 8;
        double sphere = (PORT_IN * PORT_IN + GLASS_BULGE * GLASS_BULGE) / (2.0 * GLASS_BULGE);
        double[] profile = new double[(rings + 1) * 2];
        for (int i = 0; i <= rings; i++) {
            double r = PORT_IN * i / rings;
            profile[2 * i] = r;
            profile[2 * i + 1] = Math.sqrt(sphere * sphere - r * r) - (sphere - GLASS_BULGE);
        }
        return new Mesh[] { Mesh.lathe(40, 1.0, profile).alongZ() };
    }

    // The cockpit's back and floor, which close the chest behind the pilot.
    private static Mesh[] cabin() {
        List<Mesh> m = new ArrayList<>();
        Surface wall = (u, v) -> new Vec3(CABIN_SIDE - 2.0 * CABIN_SIDE * u, FLOOR + 0.02 + (CABIN_TOP - FLOOR) * v,
                CABIN_BACK);
        m.addAll(List.of(MechParts.plated(wall, 3, 4, 66, 0.2, 0.82)));
        m.add(Mesh.bevel(-CABIN_SIDE, FLOOR - 0.25, CABIN_BACK, CABIN_SIDE, FLOOR, PORT_BACK + 0.4, 0.04, 0.85));
        return m.toArray(Mesh[]::new);
    }

    // Its sides and roof: seen through the glass from outside, never from the pilot's own seat.
    private static Mesh[] walls() {
        List<Mesh> m = new ArrayList<>();
        MechParts.pair(m, Mesh.slab(CABIN_BACK, PORT_BACK + 0.2, 0.04, 0.8, CABIN_SIDE - 0.08, FLOOR,
                CABIN_SIDE, FLOOR, CABIN_SIDE, CABIN_TOP, CABIN_SIDE - 0.08, CABIN_TOP));
        m.add(Mesh.bevel(-CABIN_SIDE, CABIN_TOP - 0.08, CABIN_BACK, CABIN_SIDE, CABIN_TOP, PORT_BACK + 0.2, 0.03,
                0.8));
        return m.toArray(Mesh[]::new);
    }

    // The pilot's seat, with a high back and a rest for the feet, round the cockpit's floor spot.
    private static Mesh[] seat() {
        double s = MechScript.SEAT - 0.12;
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.bevel(-0.34, s - 0.22, 0.18, 0.34, s, 0.95, 0.05, 1.0));
        m.add(Mesh.bevel(-0.12, 0.0, 0.4, 0.12, s - 0.22, 0.7, 0.03, 0.9));
        m.add(MechParts.side(-0.36, 0.36, 0.05, 1.05, 0.08, s, 0.26, s, 0.24, s + 1.3, 0.1, s + 1.34));
        m.add(Mesh.bevel(-0.2, s + 1.36, 0.08, 0.2, s + 1.62, 0.24, 0.04, 1.05));
        MechParts.pair(m, Mesh.bevel(0.28, s + 0.02, 0.2, 0.4, s + 0.3, 0.8, 0.03, 1.1));
        m.add(Mesh.bevel(-0.3, 0.2, 1.1, 0.3, 0.34, 1.34, 0.03, 1.0));
        return m.toArray(Mesh[]::new);
    }

    // The console before the seat: a sloping desk under the glass with the levers' slots in its back.
    private static Mesh[] console() {
        List<Mesh> m = new ArrayList<>();
        double f = FLOOR;
        m.add(MechParts.side(-0.58, 0.58, 0.05, 0.95, 1.14, f, 1.56, f, 1.56, f + 0.96, 1.26, f + 1.0, 1.14, f + 0.86));
        MechParts.pair(m, Mesh.bevel(0.26, f + 0.52, 1.0, 0.46, f + 0.66, 1.16, 0.02, 1.15));
        m.add(Mesh.bevel(-0.5, f + 0.9, 1.22, 0.5, f + 0.94, 1.3, 0.01, 1.4));
        return m.toArray(Mesh[]::new);
    }

    // One lever round its pivot: a rod up y with a grip on top.
    private static Mesh[] lever() {
        double l = MechScript.LEVER_LENGTH;
        return new Mesh[] { MechParts.hub(0.06, -0.08, 0.08, 1.1), Mesh.cylinder(8, 0.035, 0.0, l - 0.08, 1.2),
                Mesh.cylinder(10, 0.06, l - 0.14, l + 0.04, 1.05), Mesh.ball(8, 5, 0.055, 1.6).moved(0.0, l + 0.05,
                        0.0) };
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
