package nl.tivek.multiversepowers.character.greenlantern.client.minion;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechParts;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.client.render.mesh.Surface;

// The helper's parts, built like the big mech's: lofted bodies covered in glowing tiles, plates laid over them. Each
// part sits in the frame of the joint it turns on (x its right, y up, z ahead); a limb runs down its -y from the joint.
// The left limbs are the right ones mirrored. The lofted skins are kept, so cracks can run over them (`MinionCracks`).
final class MinionShapes {
    static final double HIP_Y = 1.55;
    static final double HIP_X = 0.3;
    static final double THIGH = 0.72;
    static final double SHIN = 0.68;
    static final double WAIST_Y = 1.98;
    static final double NECK_Y = 3.0;
    static final double SHOULDER_X = 0.8;
    static final double SHOULDER_Y = 2.8;
    static final double UPPER = 0.76;
    static final double FOREARM = 0.66;
    static final double TOP = 3.62;
    static final double PALM = 0.2;
    static final double[] FINGER_Z = { -0.085, -0.028, 0.028, 0.085 };
    static final double[] FINGER = { 0.1, 0.085 };
    static final double CANNON_X = 0.27;
    static final double MUZZLE = FOREARM + 0.62;
    // The core on its chest, in the trunk's places, and the two nozzles of the pack on its back.
    static final double CORE_Y = 2.6;
    static final double CORE_Z = 0.47;
    static final double[] NOZZLE_X = { 0.2, -0.2 };
    static final double NOZZLE_Y = 2.36;
    static final double NOZZLE_Z = -0.56;
    // The plates laid over each skin, as (u0, u1, v0, v1) on it: cracks run up over them.
    static final double[][] CHEST_PLATES = { { 0.16, 0.34, 0.42, 0.86 }, { 0.64, 0.86, 0.45, 0.85 } };
    static final double[][] FOREARM_PLATES = { { -0.08, 0.08, 0.2, 0.82 } };
    static final double[][] THIGH_PLATES = { { -0.07, 0.07, 0.25, 0.85 } };
    static final double[][] SHIN_PLATES = { { 0.17, 0.33, 0.18, 0.8 } };

    static final Surface PELVIS_SKIN = Surface.loft(MechParts.at(1.36, 0.0, 0.0, 0.0, 2.0),
            MechParts.at(1.42, 0.36, 0.25, 0.0, 3.0), MechParts.at(1.55, 0.45, 0.3, 0.0, 3.6),
            MechParts.at(1.86, 0.43, 0.29, 0.0, 3.6), MechParts.at(2.0, 0.3, 0.22, 0.0, 3.0),
            MechParts.at(2.02, 0.0, 0.0, 0.0, 2.0));
    static final Surface CHEST_SKIN = Surface.loft(MechParts.at(2.2, 0.0, 0.0, 0.0, 2.0),
            MechParts.at(2.24, 0.44, 0.3, 0.0, 3.4), MechParts.at(2.5, 0.6, 0.38, 0.02, 3.6),
            MechParts.at(2.82, 0.69, 0.43, 0.03, 3.8), MechParts.at(2.98, 0.56, 0.36, 0.0, 3.4),
            MechParts.at(3.03, 0.0, 0.0, 0.0, 2.0));
    static final Surface HEAD_SKIN = Surface.loft(MechParts.at(3.02, 0.0, 0.0, 0.0, 2.0),
            MechParts.at(3.05, 0.23, 0.25, 0.02, 3.0), MechParts.at(3.12, 0.28, 0.3, 0.02, 3.6),
            MechParts.at(3.34, 0.29, 0.31, 0.0, 3.6), MechParts.at(3.46, 0.22, 0.25, -0.02, 3.2),
            MechParts.at(3.5, 0.0, 0.0, 0.0, 2.0));
    static final Surface UPPER_SKIN = Surface.loft(MechParts.at(-UPPER - 0.06, 0.0, 0.0, 0.0, 2.0),
            MechParts.at(-UPPER + 0.06, 0.14, 0.14, 0.0, 2.6), MechParts.at(-0.42, 0.18, 0.18, 0.0, 3.0),
            MechParts.at(-0.08, 0.17, 0.17, 0.0, 3.0), MechParts.at(0.1, 0.0, 0.0, 0.0, 2.0));
    static final Surface FOREARM_SKIN = Surface.loft(MechParts.at(-FOREARM - 0.03, 0.0, 0.0, 0.0, 2.0),
            MechParts.at(-FOREARM + 0.06, 0.14, 0.14, 0.0, 3.0), MechParts.at(-0.42, 0.2, 0.2, 0.0, 3.8),
            MechParts.at(-0.12, 0.17, 0.17, 0.0, 3.4), MechParts.at(0.04, 0.0, 0.0, 0.0, 2.0));
    static final Surface THIGH_SKIN = Surface.loft(MechParts.at(-THIGH - 0.04, 0.0, 0.0, 0.0, 2.0),
            MechParts.at(-THIGH + 0.08, 0.15, 0.16, 0.0, 3.0), MechParts.at(-0.38, 0.2, 0.21, 0.01, 3.6),
            MechParts.at(-0.04, 0.19, 0.2, 0.0, 3.4), MechParts.at(0.1, 0.0, 0.0, 0.0, 2.0));
    static final Surface SHIN_SKIN = Surface.loft(MechParts.at(-SHIN - 0.02, 0.0, 0.0, 0.0, 2.0),
            MechParts.at(-SHIN + 0.08, 0.13, 0.14, 0.0, 3.0), MechParts.at(-0.3, 0.17, 0.19, -0.01, 3.6),
            MechParts.at(-0.06, 0.15, 0.16, 0.0, 3.2), MechParts.at(0.06, 0.0, 0.0, 0.0, 2.0));

    static final Shape PELVIS = Shape.of(pelvis());
    static final Shape ABDOMEN = Shape.of(abdomen());
    static final Shape TRUNK = Shape.of(trunk());
    static final Shape HEAD = Shape.of(head());
    static final Shape THIGH_RIGHT = Shape.of(thigh());
    static final Shape THIGH_LEFT = MechParts.mirrored(THIGH_RIGHT);
    static final Shape SHIN_RIGHT = Shape.of(shin());
    static final Shape SHIN_LEFT = MechParts.mirrored(SHIN_RIGHT);
    static final Shape FOOT_RIGHT = Shape.of(foot());
    static final Shape FOOT_LEFT = MechParts.mirrored(FOOT_RIGHT);
    static final Shape UPPER_RIGHT = Shape.of(upper());
    static final Shape UPPER_LEFT = MechParts.mirrored(UPPER_RIGHT);
    static final Shape FOREARM_RIGHT = Shape.of(forearm());
    static final Shape FOREARM_LEFT = MechParts.mirrored(FOREARM_RIGHT);
    static final Shape PALM_RIGHT = Shape.of(palm());
    static final Shape PALM_LEFT = MechParts.mirrored(PALM_RIGHT);
    static final Shape[] FINGERS = { segment(FINGER[0], 0.05), segment(FINGER[1], 0.045) };
    static final Shape THUMB = segment(0.09, 0.055);
    static final Shape CANNON = Shape.of(cannon());

    private MinionShapes() {
    }

    // Built on a thread of their own as the game starts, so the first helper drawn does not stall it.
    static void onClientSetup(FMLClientSetupEvent event) {
        Thread.ofVirtual().name("helper-parts").start(() -> CANNON.meshes());
    }

    // The hips: a tiled block with a belt round it, a plate over the front and two hanging at each side, and the
    // round joints the legs turn on.
    private static Mesh[] pelvis() {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(PELVIS_SKIN, 10, 3, 201, 0.95));
        m.add(Mesh.torus(20, 4, 0.47, 0.035, 1.35).scaled(1.0, 1.0, 0.68).moved(0.0, 1.88, 0.0));
        m.add(Mesh.bevel(-0.16, 1.44, 0.26, 0.16, 1.82, 0.36, 0.04, 1.1));
        m.add(Mesh.bevel(-0.08, 1.86, 0.28, 0.08, 1.94, 0.37, 0.02, 1.5));
        for (double side : new double[] { 1.0, -1.0 }) {
            m.add(Mesh.bevel(-0.12, -0.36, -0.15, 0.12, 0.02, 0.15, 0.03, 1.05).turned(0.0, 0.0, 1.0, side * 10.0)
                    .moved(side * 0.42, 1.86, 0.06));
            m.add(Mesh.bevel(-0.1, -0.24, -0.13, 0.1, 0.02, 0.13, 0.025, 1.12).turned(0.0, 0.0, 1.0, side * 14.0)
                    .moved(side * 0.47, 1.62, 0.06));
            m.add(Mesh.cylinder(14, 0.13, -0.08, 0.08, 1.0).alongX().moved(side * HIP_X, HIP_Y, 0.0));
            m.add(Mesh.torus(14, 4, 0.13, 0.025, 1.4).alongX().moved(side * (HIP_X + 0.09), HIP_Y, 0.0));
        }
        return m.toArray(Mesh[]::new);
    }

    // The belly between hips and chest: three bands, narrow to broad, seen as it twists.
    private static Mesh[] abdomen() {
        List<Mesh> m = new ArrayList<>();
        for (int k = 0; k < 3; k++) {
            double y = 1.96 + k * 0.105;
            double wide = 0.34 + 0.04 * k;
            double deep = 0.24 + 0.025 * k;
            m.add(Mesh.bevel(-wide, y, -deep, wide, y + 0.09, deep, 0.03, 1.0 + 0.04 * k));
        }
        m.add(Mesh.cylinder(12, 0.2, 1.94, 2.28, 0.8));
        return m.toArray(Mesh[]::new);
    }

    // The chest: a broad tiled V over the belly, plates over its front, the Lantern's sign (a ring between two bars)
    // round the core, a collar, the ball joints of the shoulders and a pack with two nozzles on its back.
    private static Mesh[] trunk() {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(CHEST_SKIN, 14, 5, 211, 1.0));
        Surface out = CHEST_SKIN.offset(0.05, true);
        m.addAll(List.of(MechParts.plated(plate(out, CHEST_PLATES[0]), 3, 3, 212, 0.12, 1.06)));
        m.addAll(List.of(MechParts.plated(plate(out, CHEST_PLATES[1]), 3, 3, 213, 0.1, 1.02)));
        m.add(Mesh.torus(20, 5, 0.15, 0.04, 1.6).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, CORE_Y, CORE_Z));
        m.add(Mesh.cylinder(16, 0.1, -0.03, 0.03, 1.3).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, CORE_Y, CORE_Z - 0.02));
        for (double y : new double[] { CORE_Y + 0.22, CORE_Y - 0.22 }) {
            m.add(Mesh.bevel(-0.2, y - 0.03, CORE_Z - 0.05, 0.2, y + 0.03, CORE_Z + 0.03, 0.015, 1.55));
        }
        m.add(Mesh.cylinder(14, 0.18, 2.96, 3.06, 0.95));
        m.add(Mesh.torus(14, 4, 0.19, 0.03, 1.35).moved(0.0, 3.0, 0.0));
        for (double side : new double[] { 1.0, -1.0 }) {
            m.add(Mesh.ball(12, 8, 0.2, 1.0).moved(side * SHOULDER_X, SHOULDER_Y, 0.0));
            m.add(Mesh.bevel(-0.04, -0.28, -0.045, 0.04, 0.28, 0.045, 0.015, 1.3).turned(0.0, 0.0, 1.0, side * 18.0)
                    .moved(side * 0.36, 2.6, 0.41));
        }
        m.add(Mesh.bevel(-0.34, 2.32, -0.56, 0.34, 2.9, -0.36, 0.06, 1.0));
        m.add(Mesh.bevel(-0.28, 2.42, -0.6, 0.28, 2.82, -0.54, 0.03, 1.12));
        for (double x : NOZZLE_X) {
            m.add(Mesh.cylinder(12, 0.085, -0.12, 0.08, 1.05).moved(x, NOZZLE_Y + 0.08, NOZZLE_Z + 0.06));
            m.add(Mesh.cone(12, 0.11, 0.075, -0.1, 0.0, 1.25).moved(x, NOZZLE_Y - 0.04, NOZZLE_Z + 0.06));
            m.add(Mesh.torus(12, 4, 0.11, 0.022, 1.5).moved(x, NOZZLE_Y - 0.14, NOZZLE_Z + 0.06));
        }
        return m.toArray(Mesh[]::new);
    }

    // The helmet: tiled and rounded, a fin along its crown, a V-shaped visor recessed in its face, a guard over its
    // jaw, a disc over each ear with vents, and an antenna up from the right one.
    private static Mesh[] head() {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(HEAD_SKIN, 10, 4, 221, 1.0));
        m.add(side(-0.035, 0.035, 0.015, 1.3, 0.24, 3.42, -0.22, 3.47, -0.34, 3.6, 0.04, 3.52));
        Mesh visor = Mesh.bevel(0.0, -0.035, 0.0, 0.25, 0.035, 0.07, 0.012, 0.55).turned(0.0, 0.0, 1.0, 14.0)
                .moved(0.0, 3.23, 0.28);
        m.add(visor);
        m.add(visor.mirrored());
        for (double side : new double[] { 1.0, -1.0 }) {
            m.add(Mesh.cylinder(14, 0.095, -0.03, 0.03, 1.05).alongX().moved(side * 0.3, 3.24, -0.02));
            m.add(Mesh.torus(14, 4, 0.095, 0.02, 1.45).alongX().moved(side * 0.33, 3.24, -0.02));
            for (int k = 0; k < 3; k++) {
                m.add(Mesh.bevel(-0.02, -0.012, -0.05, 0.02, 0.012, 0.05, 0.005, 1.2).moved(side * 0.345,
                        3.2 + k * 0.04, -0.02));
            }
        }
        m.add(Mesh.bevel(-0.2, 3.05, 0.22, 0.2, 3.15, 0.33, 0.03, 1.05));
        m.add(Mesh.bevel(-0.03, 3.06, 0.3, 0.03, 3.16, 0.35, 0.012, 1.3));
        m.add(Mesh.cylinder(6, 0.018, 0.0, 0.3, 1.3).turned(1.0, 0.0, 0.0, -12.0).moved(0.33, 3.3, -0.06));
        m.add(Mesh.ball(8, 5, 0.045, 1.6).moved(0.33, 3.593, -0.122));
        return m.toArray(Mesh[]::new);
    }

    private static Surface plate(Surface out, double[] at) {
        return MechParts.part(out, at[0], at[1], at[2], at[3]);
    }

    // A side view (z, y pairs, convex) given thickness across x.
    private static Mesh side(double x0, double x1, double cut, double bright, double... zy) {
        double[] outline = new double[zy.length];
        for (int i = 0; i < zy.length; i += 2) {
            outline[i] = -zy[i];
            outline[i + 1] = zy[i + 1];
        }
        return Mesh.slab(x0, x1, cut, bright, outline).turned(0.0, 1.0, 0.0, 90.0);
    }

    // The upper arm: a tiled column under two layered plates over the shoulder, the elbow's round joint at its end.
    private static Mesh[] upper() {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(UPPER_SKIN, 8, 4, 231, 1.0));
        m.add(Mesh.bevel(-0.18, -0.03, -0.25, 0.25, 0.1, 0.25, 0.035, 1.05).turned(0.0, 0.0, 1.0, -26.0)
                .moved(0.06, 0.08, 0.0));
        m.add(Mesh.bevel(-0.1, -0.04, -0.21, 0.23, 0.05, 0.21, 0.025, 1.12).turned(0.0, 0.0, 1.0, -40.0)
                .moved(0.14, -0.12, 0.0));
        m.add(Mesh.bevel(0.19, 0.06, -0.03, 0.27, 0.13, 0.03, 0.012, 1.4).turned(0.0, 0.0, 1.0, -26.0)
                .moved(0.06, 0.08, 0.0));
        m.add(Mesh.cylinder(14, 0.13, -0.15, 0.15, 0.95).alongX().moved(0.0, -UPPER, 0.0));
        m.add(Mesh.torus(14, 4, 0.13, 0.025, 1.4).alongX().moved(0.16, -UPPER, 0.0));
        m.add(Mesh.torus(14, 4, 0.13, 0.025, 1.4).alongX().moved(-0.16, -UPPER, 0.0));
        return m.toArray(Mesh[]::new);
    }

    // The gauntlet: a forearm swelling to its middle, a plated ridge down its outer side, a guard over the back of the
    // elbow and a ring at the wrist.
    private static Mesh[] forearm() {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(FOREARM_SKIN, 10, 5, 241, 1.0));
        Surface out = FOREARM_SKIN.offset(0.05, true);
        m.addAll(List.of(MechParts.plated(plate(out, FOREARM_PLATES[0]), 1, 3, 242, 0.1, 1.08)));
        m.add(Mesh.bevel(-0.15, -0.16, -0.24, 0.15, 0.08, -0.12, 0.04, 1.1));
        m.add(Mesh.torus(16, 4, 0.155, 0.03, 1.4).moved(0.0, -FOREARM + 0.07, 0.0));
        return m.toArray(Mesh[]::new);
    }

    // The hand's palm and back below the wrist; the fingers hang from its lower edge along z, the thumb at its front,
    // the palm facing in (-x).
    private static Mesh[] palm() {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.bevel(-0.07, -PALM, -0.12, 0.06, 0.0, 0.12, 0.03, 1.0));
        m.add(Mesh.bevel(0.04, -PALM + 0.02, -0.11, 0.1, -0.03, 0.11, 0.02, 1.12));
        for (double z : FINGER_Z) {
            m.add(Mesh.bevel(0.05, -PALM - 0.01, z - 0.025, 0.1, -PALM + 0.05, z + 0.025, 0.01, 1.3));
        }
        m.add(Mesh.cylinder(10, 0.07, -0.02, 0.03, 1.2));
        return m.toArray(Mesh[]::new);
    }

    // One finger joint hanging down its -y, so long and wide, its knuckle round.
    private static Shape segment(double length, double wide) {
        return Shape.of(Mesh.bevel(-wide * 0.55, -length, -wide * 0.5, wide * 0.55, 0.0, wide * 0.5, wide * 0.18,
                1.0), Mesh.ball(8, 5, wide * 0.5, 1.15));
    }

    // The cannon along the outer side of the right forearm: a mount, a barrel in a shroud ringed by three coils, a
    // rail along its top and a muzzle brake slotted at the sides.
    private static Mesh[] cannon() {
        List<Mesh> m = new ArrayList<>();
        double x = CANNON_X;
        m.add(Mesh.bevel(0.12, -0.52, -0.1, x + 0.02, -0.1, 0.1, 0.03, 1.0));
        m.add(Mesh.cylinder(14, 0.085, -MUZZLE + 0.12, -0.08, 1.05).moved(x, 0.0, 0.0));
        m.add(Mesh.cylinder(16, 0.125, -0.78, -0.22, 0.95).moved(x, 0.0, 0.0));
        for (double y : new double[] { -0.34, -0.5, -0.66 }) {
            m.add(Mesh.torus(16, 4, 0.14, 0.025, 1.5).moved(x, y, 0.0));
        }
        m.add(Mesh.bevel(x - 0.025, -0.82, 0.12, x + 0.025, -0.2, 0.17, 0.01, 1.25));
        m.add(Mesh.cylinder(14, 0.115, -MUZZLE, -MUZZLE + 0.14, 1.1).moved(x, 0.0, 0.0));
        for (double side : new double[] { 1.0, -1.0 }) {
            m.add(Mesh.bevel(-0.02, -MUZZLE + 0.03, -0.02, 0.02, -MUZZLE + 0.11, 0.02, 0.005, 0.4)
                    .moved(x + side * 0.115, 0.0, 0.0));
        }
        m.add(Mesh.torus(16, 4, 0.12, 0.03, 1.6).moved(x, -MUZZLE, 0.0));
        return m.toArray(Mesh[]::new);
    }

    // The thigh: a tiled column, a plate down its outer side, the knee's round joint at its end.
    private static Mesh[] thigh() {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(THIGH_SKIN, 8, 4, 251, 1.0));
        Surface out = THIGH_SKIN.offset(0.05, true);
        m.addAll(List.of(MechParts.plated(plate(out, THIGH_PLATES[0]), 1, 3, 252, 0.1, 1.05)));
        m.add(Mesh.cylinder(14, 0.12, -0.13, 0.13, 0.95).alongX().moved(0.0, -THIGH, 0.0));
        return m.toArray(Mesh[]::new);
    }

    // The shin: a tiled column with a cap over the knee, a guard down its front and vents in its calf.
    private static Mesh[] shin() {
        List<Mesh> m = new ArrayList<>();
        m.add(MechParts.skin(SHIN_SKIN, 8, 4, 261, 1.0));
        Surface out = SHIN_SKIN.offset(0.05, true);
        m.addAll(List.of(MechParts.plated(plate(out, SHIN_PLATES[0]), 2, 3, 262, 0.1, 1.06)));
        m.add(Mesh.bevel(-0.12, -0.12, 0.1, 0.12, 0.1, 0.24, 0.05, 1.12).turned(1.0, 0.0, 0.0, -12.0));
        for (int k = 0; k < 3; k++) {
            m.add(Mesh.bevel(-0.09, -0.24 - k * 0.07, -0.21, 0.09, -0.2 - k * 0.07, -0.16, 0.01, 1.3));
        }
        return m.toArray(Mesh[]::new);
    }

    // The foot under the ankle: a sole, a plated top, two toe caps and a spur at the heel, round the ankle's joint.
    private static Mesh[] foot() {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.bevel(-0.17, -0.15, -0.22, 0.17, -0.06, 0.3, 0.03, 1.0));
        m.add(Mesh.bevel(-0.14, -0.08, -0.16, 0.14, 0.04, 0.2, 0.04, 1.05));
        for (double x : new double[] { 0.085, -0.085 }) {
            m.add(Mesh.bevel(x - 0.075, -0.15, 0.28, x + 0.075, -0.04, 0.42, 0.03, 1.12));
        }
        m.add(Mesh.bevel(-0.06, -0.15, -0.32, 0.06, -0.05, -0.2, 0.02, 1.15));
        m.add(Mesh.cylinder(12, 0.09, -0.15, 0.15, 0.95).alongX());
        return m.toArray(Mesh[]::new);
    }
}
