package nl.tivek.multiversepowers.character.greenlantern.client.mech.shape;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.client.render.mesh.Surface;

// The trunk round the mech's ground spot, x to its right, y up, z ahead, built like a jaeger: a chest broad at the
// shoulders tapering in a V to a narrow ribbed waist, a heavy pelvis with plates over the hips. The pilot sits in a
// cockpit deep in the chest, looking out through a round port of see-through green glass set in a thick flange of
// blocks where a jaeger's reactor glows.
public final class MechBodyShapes {
    public static final double PORT_Y = 9.8;
    public static final double PORT_IN = 1.05;
    static final double PORT_OUT = 1.62;
    static final double PORT_BACK = 1.0;
    static final double PORT_FRONT = 2.02;
    public static final double GLASS_Z = 1.88;
    static final double GLASS_BULGE = 0.27;
    public static final Vec3 CORE = new Vec3(0.0, PORT_Y, 1.0);
    static final double CORE_RADIUS = 1.35;
    public static final double CHEST_TOP = 11.75;
    private static final double FLOOR = MechScript.COCKPIT.y;
    private static final double CABIN_BACK = -0.32;
    private static final double CABIN_SIDE = 1.3;
    private static final double CABIN_TOP = 11.25;
    private static final double HOLE_WIDE = 1.1;
    private static final double HOLE_HIGH = 1.15;
    private static final int RIM_BLOCKS = 18;
    private static final Surface CHEST_SKIN = Surface.loft(MechParts.at(8.0, 1.45, 1.3, 0.05, 3.6),
            MechParts.at(8.6, 1.75, 1.4, 0.08, 3.8), MechParts.at(9.3, 2.25, 1.74, 0.12, 4.2),
            MechParts.at(10.15, 2.62, 1.84, 0.1, 4.5), MechParts.at(10.9, 2.66, 1.76, 0.04, 4.5),
            MechParts.at(11.45, 2.1, 1.45, -0.04, 4.0), MechParts.at(CHEST_TOP, 0.0, 0.0, -0.08, 2.0));

    // Half the turn the port takes out of the front of the chest.
    private static final double OPEN = open();

    public static final Shape PELVIS = Shape.of(pelvis());
    public static final Shape WAIST = Shape.of(waist());
    public static final Shape CHEST = Shape.of(chest());
    public static final Shape RIM = Shape.of(rim());
    public static final Shape GLASS = Shape.of(glass());
    public static final Shape BUBBLE = Shape.of(Mesh.ball(28, 16, CORE_RADIUS, 1.0));
    public static final Shape CABIN = Shape.of(cabin());
    public static final Shape WALLS = Shape.of(walls());
    public static final Shape SEAT = Shape.of(seat());
    public static final Shape CONSOLE = Shape.of(console());
    public static final Shape LEVER = Shape.of(lever());
    public static final Shape BUTTON = Shape.of(Mesh.bevel(-0.07, -0.035, -0.06, 0.07, 0.035, 0.06, 0.02, 1.0));
    public static final Shape SHOULDER = Shape.of(shoulder());
    public static final Shape SHOULDER_LEFT = MechParts.mirrored(SHOULDER);

    private MechBodyShapes() {
    }

    // A heavy pelvis round the hips: a plate down its front, pointed at the bottom, and a guard of two plates over each
    // hip joint, clear of the thigh that swings under it.
    private static Mesh[] pelvis() {
        Vec3 hip = MechScript.HIP;
        Surface pelvis = Surface.loft(MechParts.at(6.35, 0.0, 0.0, -0.05, 2.0),
                MechParts.at(6.5, 1.0, 0.72, -0.05, 3.4), MechParts.at(6.85, 1.7, 1.0, -0.05, 4.2),
                MechParts.at(7.35, 1.98, 1.12, -0.05, 4.5), MechParts.at(7.75, 1.86, 1.06, -0.05, 4.2),
                MechParts.at(7.95, 0.0, 0.0, -0.05, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(pelvis, 22, 4, 41, 0.95));
        MechParts.pair(m, Mesh.ball(16, 10, 0.74, 1.0).moved(hip.x, hip.y, hip.z));
        m.add(MechParts.front(1.0, 1.34, 0.09, 1.08, -0.8, 7.82, 0.8, 7.82, 0.68, 7.0, 0.0, 6.15, -0.68, 7.0));
        m.add(MechParts.front(1.3, 1.44, 0.03, 1.3, -0.2, 7.62, 0.2, 7.62, 0.22, 7.0, 0.0, 6.55, -0.22, 7.0));
        for (int k = 0; k < 2; k++) {
            double x = 2.44 + 0.12 * k;
            double top = 8.0 - 0.55 * k;
            Mesh guard = MechParts.side(x, x + 0.18, 0.05, 1.04 + 0.06 * k, -0.8 + 0.1 * k, top - 1.25, 0.05,
                    top - 1.5, 0.85 - 0.1 * k, top - 1.15, 1.0 - 0.1 * k, top, -1.0 + 0.1 * k, top);
            MechParts.pair(m, flared(guard, x, top - 0.6, 7.0 + 4.0 * k));
        }
        Surface outside = pelvis.offset(0.05, true);
        m.addAll(List.of(MechParts.plated(MechParts.part(outside, 0.425, 0.575, 0.18, 0.8), 2, 2, 43, 0.12,
                1.05)));
        return m.toArray(Mesh[]::new);
    }

    // A side plate at `x` turned out at its foot about a line along z at height `y`, by so many degrees.
    private static Mesh flared(Mesh plate, double x, double y, double degrees) {
        return plate.moved(-x, -y, 0.0).turned(0.0, 0.0, 1.0, degrees).moved(x, y, 0.0);
    }

    // The narrow waist between the pelvis and the chest, ribbed with armoured bands across its belly.
    private static Mesh[] waist() {
        Surface waist = Surface.loft(MechParts.at(7.5, 0.0, 0.0, 0.0, 2.0), MechParts.at(7.56, 1.15, 0.86, 0.0, 3.6),
                MechParts.at(7.85, 1.26, 0.94, 0.02, 3.8), MechParts.at(8.15, 1.2, 0.9, 0.02, 3.8),
                MechParts.at(8.45, 1.32, 0.98, 0.03, 3.8), MechParts.at(8.75, 0.0, 0.0, 0.03, 2.0));
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(waist, 18, 3, 51, 0.95));
        for (int k = 0; k < 2; k++) {
            double y = 7.66 + 0.36 * k;
            MechParts.pair(m, Mesh.bevel(0.1, y, 0.82, 0.92, y + 0.28, 1.04, 0.04, 1.14 - 0.05 * k));
        }
        m.add(Mesh.bevel(-0.08, 7.58, 0.9, 0.08, 8.42, 1.08, 0.03, 1.35));
        return m.toArray(Mesh[]::new);
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
        double pecTop = MechParts.along(chest, 11.05);
        double pecLow = MechParts.along(chest, 9.15);
        m.addAll(List.of(MechParts.plated(MechParts.part(outside, 0.25 + OPEN + 0.01, 0.25 + OPEN + 0.12, pecLow,
                pecTop), 2, 3, 64, 0.16, 1.06)));
        m.addAll(List.of(MechParts.plated(MechParts.part(outside, 0.25 - OPEN - 0.12, 0.25 - OPEN - 0.01, pecLow,
                pecTop), 2, 3, 65, 0.16, 1.06)));
        // Under the port two plates close in a V down to the waist, as a jaeger's ribs do.
        Surface rib = strip(outside, 0.235, MechParts.along(chest, 8.25), 0.1, MechParts.along(chest, 9.25), 0.045);
        for (Mesh plate : MechParts.plated(rib, 1, 4, 67, 0.14, 1.1)) {
            MechParts.pair(m, plate);
        }
        // The back: a raised plate between the shoulder blades and two vents low down on it.
        double backLow = MechParts.along(chest, 9.0);
        double backTop = MechParts.along(chest, 11.3);
        m.addAll(List.of(MechParts.plated(MechParts.part(outside, 0.66, 0.84, backLow, backTop), 3, 4, 66, 0.18,
                1.06)));
        MechParts.pair(m, Mesh.cylinder(14, 0.36, 0.0, 0.4, 1.1).alongZ().moved(0.75, 9.25, -1.98));
        MechParts.pair(m, Mesh.torus(14, 4, 0.36, 0.05, 1.35).alongZ().moved(0.75, 9.25, -1.98));
        m.add(Mesh.torus(24, 4, 0.82, 0.08, 1.3).moved(0.0, CHEST_TOP - 0.14, -0.02));
        return m.toArray(Mesh[]::new);
    }

    // A strip of a body's skin from (u0, v0) to (u1, v1) in its own places, `wide` across in u.
    private static Surface strip(Surface skin, double u0, double v0, double u1, double v1, double wide) {
        return (u, v) -> skin.at(u0 + (u1 - u0) * v + (u - 0.5) * wide, v0 + (v1 - v0) * v);
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

    // The right pauldron, a jaeger's: a big squared shell over the joint, tipped out at its top, open below where the
    // arm comes out, two layered plates hanging down its outer side and a raised crest along its top.
    private static Mesh[] shoulder() {
        Vec3 joint = MechScript.SHOULDER;
        List<Mesh> m = new ArrayList<>();
        Surface shell = Surface.loft(MechParts.at(-1.05, 1.2, 1.22, 0.0, 4.6), MechParts.at(-0.4, 1.46, 1.42, 0.0, 5.8),
                MechParts.at(0.35, 1.54, 1.48, -0.02, 6.2), MechParts.at(1.0, 1.44, 1.38, -0.05, 6.0),
                MechParts.at(1.3, 1.18, 1.14, -0.08, 5.2), MechParts.at(1.42, 0.0, 0.0, -0.1, 2.0))
                .turned(0.0, 0.0, 1.0, -16.0).moved(joint.x + 0.3, joint.y + 0.35, joint.z);
        m.addAll(List.of(MechParts.plated(shell, 16, 6, 71, 0.18, 1.0)));
        for (int k = 0; k < 2; k++) {
            double y = -0.95 - 0.42 * k;
            Surface tier = Surface.loft(MechParts.at(y - 0.42, 1.5 - 0.1 * k, 1.36 - 0.1 * k, 0.0, 4.4),
                    MechParts.at(y, 1.58 - 0.1 * k, 1.44 - 0.1 * k, 0.0, 4.6));
            Surface outer = MechParts.part(tier, -0.17, 0.17, 0.0, 1.0);
            for (Mesh plate : MechParts.plated(outer, 4, 1, 72 + k, 0.12, 1.04 + 0.04 * k)) {
                m.add(plate.turned(0.0, 0.0, 1.0, -16.0).moved(joint.x + 0.3, joint.y + 0.35, joint.z));
            }
        }
        m.add(Mesh.ball(16, 10, 0.82, 0.95).moved(joint.x, joint.y, joint.z));
        // The crest along its top, from front to back, and a lamp in its front.
        m.add(MechParts.side(-0.1, 0.1, 0.04, 1.15, -1.2, -0.05, 1.1, -0.05, 0.7, 0.42, -1.05, 0.4)
                .turned(0.0, 0.0, 1.0, -16.0).moved(joint.x + 0.72, joint.y + 1.62, joint.z));
        m.add(Mesh.bevel(-0.32, -0.16, 0.0, 0.32, 0.16, 0.14, 0.04, 1.5).turned(0.0, 0.0, 1.0, -16.0)
                .moved(joint.x + 0.55, joint.y + 0.65, joint.z + 1.42));
        return m.toArray(Mesh[]::new);
    }
}
