package nl.tivek.multiversepowers.character.greenlantern.client.render.weapon;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.band;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.grip;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.leaning;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.path;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.rod;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.twice;

// The revolver, the arm cannon and the minigun in the parts that move apart (barrels +z, top +y, blocks): the
// revolver's frame, its cylinder turning about (0, CYLINDER_Y) and swinging out on its crane, its hammer cocking about
// HAMMER and its rounds; the cannon's body, its vents opening as it charges, its core and its shield; the minigun's body
// and its barrels spinning about the z axis, and a spent casing.
public final class GunParts {
    public static final double CYLINDER_Y = 0.01;
    public static final Vec3 HAMMER = new Vec3(0.0, 0.07, -0.15);
    // The crane the cylinder swings out on: an axis along z through here, out to the gun's left (+x).
    public static final Vec3 CRANE = new Vec3(0.034, -0.04, 0.0);
    public static final double CHAMBER_RING = 0.045;
    public static final Vec3 REVOLVER_MUZZLE = new Vec3(0.0, 0.06, 0.52);
    public static final Vec3 REVOLVER_GRIP = new Vec3(0.0, -0.175, -0.19);
    public static final Vec3 CANNON_MUZZLE = new Vec3(0.0, 0.0, 0.36);
    public static final Vec3 CANNON_CORE = new Vec3(0.0, 0.0, 0.24);
    public static final Vec3 VENT_HINGE = new Vec3(0.2, 0.0, -0.29);
    public static final double BARREL_RING = 0.062;
    public static final Vec3 MINIGUN_MUZZLE = new Vec3(0.0, 0.0, 0.78);

    // Every point above is set before any shape: building one loads WeaponShapes, whose icons are built from here.
    public static final Shape REVOLVER_FRAME = Shape.of(revolverFrame());
    public static final Shape REVOLVER_CYLINDER = Shape.of(revolverCylinder());
    public static final Shape REVOLVER_HAMMER = Shape.of(revolverHammer());
    public static final Shape ROUND = Shape.of(round());

    public static final Shape CANNON_BODY = Shape.of(cannonBody());
    // One vent on each side, at +x; drawn turned open about its back edge.
    public static final Shape CANNON_VENT = Shape.of(cannonVent());
    public static final Shape CANNON_VENT_LEFT = Shape.of(cannonVent()[0].mirrored());
    public static final Shape CANNON_CORE_BALL = Shape.of(Mesh.ball(14, 8, 0.06, 2.3));
    public static final Shape CANNON_SHIELD = Shape.of(cannonShield());

    public static final Shape MINIGUN_BODY = Shape.of(minigunBody());
    public static final Shape MINIGUN_BARRELS = Shape.of(minigunBarrels());
    public static final Shape CASING = Shape.of(rod(6, 0.011, 0.0, 0.04, 1.4), Mesh.cylinder(6, 0.014, -0.004, 0.0,
            1.8).alongZ());

    private GunParts() {
    }

    // Everything of the revolver, as one, for the wheel's icon.
    static Mesh[] revolver() {
        List<Mesh> parts = new ArrayList<>(List.of(revolverFrame()));
        parts.addAll(List.of(revolverCylinder()));
        parts.addAll(List.of(revolverHammer()));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] revolverFrame() {
        List<Mesh> parts = new ArrayList<>();
        double bore = 0.06;
        parts.add(rod(16, 0.031, 0.09, 0.5, 1.0).moved(0.0, bore, 0.0));
        parts.add(Mesh.torus(16, 5, 0.022, 0.008, 1.9).alongZ().moved(0.0, bore, 0.505));
        // The ventilated rib along its top, its slots lit, and the sights.
        parts.add(Mesh.bevel(-0.019, 0.083, 0.09, 0.019, 0.1, 0.5, 0.004, 1.15));
        for (int k = 0; k < 5; k++) {
            parts.add(Mesh.box(-0.01, 0.1, 0.125 + k * 0.072, 0.01, 0.104, 0.16 + k * 0.072, 1.95));
        }
        parts.add(Mesh.bevel(-0.007, 0.1, 0.44, 0.007, 0.132, 0.49, 0.003, 1.5));
        parts.add(Mesh.bevel(-0.022, 0.1, -0.12, -0.008, 0.122, -0.095, 0.003, 1.4));
        parts.add(Mesh.bevel(0.008, 0.1, -0.12, 0.022, 0.122, -0.095, 0.003, 1.4));
        // The full-length lug under the barrel, a glowing line down each side of it.
        parts.add(Mesh.bevel(-0.027, -0.006, 0.09, 0.027, bore, 0.5, 0.006, 1.0));
        twice(parts, Mesh.box(0.027, 0.02, 0.11, 0.03, 0.028, 0.48, 1.95));
        parts.add(Mesh.cylinder(10, 0.012, 0.47, 0.5, 1.6).alongZ().moved(0.0, 0.012, 0.0));
        // The frame: its top strap over the cylinder, the window round it, the recoil shield behind.
        parts.add(Mesh.bevel(-0.032, 0.08, -0.105, 0.032, 0.1, 0.1, 0.005, 1.0));
        twice(parts, Mesh.box(0.032, 0.088, -0.09, 0.035, 0.094, 0.085, 1.9));
        parts.add(Mesh.bevel(-0.037, -0.078, -0.165, 0.037, 0.1, -0.078, 0.008, 1.0));
        parts.add(Mesh.bevel(-0.029, -0.078, -0.16, 0.029, -0.056, 0.1, 0.005, 1.0));
        parts.add(Mesh.bevel(-0.031, -0.078, 0.076, 0.031, -0.004, 0.1, 0.005, 1.0));
        twice(parts, Mesh.box(0.037, 0.02, -0.14, 0.043, 0.04, -0.1, 1.6));
        // Trigger guard and trigger.
        parts.add(Mesh.tube(false, 7, 0.011, 1.2, path(0.0, -0.077, 0.055, 0.0, -0.127, 0.045, 0.0, -0.15, 0.005, 0.0,
                -0.142, -0.045, 0.0, -0.107, -0.075, 0.0, -0.077, -0.08)));
        parts.add(Mesh.tube(false, 5, 0.009, 1.5, path(0.0, -0.077, -0.005, 0.0, -0.102, 0.0, 0.0, -0.127, -0.017)));
        // The grip, its plates bevelled and each set with the lantern's ring.
        parts.add(leaning(grip(0.25, 0.052, 0.035, 0.95), 115.0, -0.07, -0.13));
        twice(parts, Mesh.torus(14, 4, 0.021, 0.006, 1.95).alongX().moved(0.04, -0.179, -0.181));
        twice(parts, Mesh.ball(8, 5, 0.008, 2.0).moved(0.04, -0.179, -0.181));
        parts.add(Mesh.cylinder(10, 0.026, -0.005, 0.012, 1.5).alongX().moved(0.0, -0.27, -0.235));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] revolverCylinder() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(24, 1.0, 0.0, -0.075, 0.062, -0.075, 0.071, -0.064, 0.071, 0.064, 0.062, 0.075, 0.0, 0.075)
                .alongZ().moved(0.0, CYLINDER_Y, 0.0));
        for (int k = 0; k < 6; k++) {
            parts.add(Mesh.box(-0.008, 0.066, -0.052, 0.008, 0.075, 0.052, 1.9).turned(0.0, 0.0, 1.0, 30.0 + 60.0 * k)
                    .moved(0.0, CYLINDER_Y, 0.0));
            double angle = Math.toRadians(90.0 + 60.0 * k);
            parts.add(Mesh.torus(10, 4, 0.016, 0.005, 1.8).alongZ().moved(Math.cos(angle) * CHAMBER_RING,
                    CYLINDER_Y + Math.sin(angle) * CHAMBER_RING, 0.077));
        }
        parts.add(band(0.072, 0.006, 0.0, 1.95).moved(0.0, CYLINDER_Y, 0.0));
        parts.add(Mesh.cylinder(10, 0.012, 0.075, 0.12, 1.5).alongZ().moved(0.0, CYLINDER_Y - 0.03, 0.0));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] revolverHammer() {
        return new Mesh[] { Mesh.bevel(-0.011, 0.0, -0.016, 0.011, 0.075, 0.016, 0.004, 1.3)
                .turned(1.0, 0.0, 0.0, -35.0).moved(HAMMER.x, HAMMER.y, HAMMER.z),
                Mesh.box(-0.014, 0.06, -0.03, 0.014, 0.072, -0.01, 1.6).turned(1.0, 0.0, 0.0, -35.0)
                        .moved(HAMMER.x, HAMMER.y, HAMMER.z) };
    }

    // A round, its rim at z 0 and its bullet forward.
    private static Mesh[] round() {
        return new Mesh[] { rod(8, 0.013, 0.0, 0.07, 1.3),
                Mesh.lathe(8, 1.9, 0.0, 0.07, 0.013, 0.07, 0.008, 0.088, 0.0, 0.092).alongZ(),
                Mesh.cylinder(8, 0.016, -0.006, 0.0, 1.7).alongZ() };
    }

    static Mesh[] armCannon() {
        List<Mesh> parts = new ArrayList<>(List.of(cannonBody()));
        parts.add(cannonVent()[0]);
        parts.add(cannonVent()[0].mirrored());
        parts.add(Mesh.ball(14, 8, 0.06, 2.3).moved(CANNON_CORE.x, CANNON_CORE.y, CANNON_CORE.z));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] cannonBody() {
        List<Mesh> parts = new ArrayList<>();
        // The sleeve over his forearm, a waist, the barrel and its flared muzzle round the core's window.
        parts.add(Mesh.lathe(28, 1.0, 0.0, -0.44, 0.15, -0.44, 0.2, -0.415, 0.215, -0.36, 0.218, -0.1, 0.2, -0.05,
                0.142, -0.01, 0.124, 0.02, 0.124, 0.25, 0.145, 0.28, 0.15, 0.335, 0.125, 0.355, 0.085, 0.36, 0.085,
                0.26, 0.0, 0.26).alongZ());
        parts.add(band(0.17, 0.018, -0.44, 1.5));
        parts.add(band(0.218, 0.015, -0.36, 1.6));
        parts.add(band(0.218, 0.015, -0.11, 1.6));
        for (double z : new double[] { 0.06, 0.12, 0.18 }) {
            parts.add(band(0.124, 0.009, z, 1.7));
        }
        parts.add(band(0.086, 0.01, 0.358, 2.1));
        // Lit rails along the barrel, a sight on top, cooling fins on the sleeve's back.
        for (int k = 0; k < 4; k++) {
            double turn = 45.0 + 90.0 * k;
            parts.add(Mesh.box(-0.007, 0.124, 0.0, 0.007, 0.137, 0.27, 1.9).turned(0.0, 0.0, 1.0, turn));
        }
        parts.add(Mesh.bevel(-0.012, 0.21, -0.2, 0.012, 0.25, 0.0, 0.004, 1.2));
        parts.add(Mesh.torus(10, 4, 0.018, 0.005, 1.95).alongZ().moved(0.0, 0.25, 0.0));
        for (int k = 0; k < 5; k++) {
            double z = -0.33 + k * 0.045;
            parts.add(Mesh.bevel(-0.07, 0.205, z, 0.07, 0.238, z + 0.026, 0.004, 1.35));
        }
        // The lantern's ring on each side, the handle under it.
        twice(parts, Mesh.torus(22, 5, 0.062, 0.012, 1.95).alongX().moved(0.218, 0.0, -0.21));
        twice(parts, Mesh.ball(10, 6, 0.022, 2.1).moved(0.218, 0.0, -0.21));
        parts.add(rod(12, 0.045, -0.36, -0.06, 1.1).moved(0.0, -0.22, 0.0));
        for (double z : new double[] { -0.32, -0.21, -0.1 }) {
            parts.add(Mesh.torus(12, 4, 0.047, 0.008, 1.8).alongZ().moved(0.0, -0.22, z));
        }
        parts.add(Mesh.bevel(-0.02, -0.215, -0.34, 0.02, -0.17, -0.08, 0.006, 1.0));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] cannonVent() {
        return new Mesh[] { Mesh.bevel(0.188, -0.1, -0.29, 0.222, 0.1, -0.13, 0.006, 1.2) };
    }

    // A six-sided shield of hard light facing +z, its rim raised and bright and the ring at its heart.
    private static Mesh[] cannonShield() {
        return new Mesh[] { Mesh.lathe(6, 0.9, 0.0, -0.02, 0.5, -0.02, 0.5, 0.02, 0.0, 0.02).alongZ(),
                Mesh.ring(6, 1.3, 0.5, -0.035, 0.62, -0.035, 0.62, 0.035, 0.5, 0.035).alongZ(),
                Mesh.torus(24, 5, 0.18, 0.025, 1.95).alongZ(), Mesh.ball(12, 6, 0.07, 2.1).scaled(1.0, 1.0, 0.5) };
    }

    static Mesh[] minigun() {
        List<Mesh> parts = new ArrayList<>(List.of(minigunBody()));
        parts.addAll(List.of(minigunBarrels()));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] minigunBarrels() {
        List<Mesh> parts = new ArrayList<>();
        for (int k = 0; k < 6; k++) {
            double angle = Math.toRadians(90.0 + 60.0 * k);
            double x = Math.cos(angle) * BARREL_RING;
            double y = Math.sin(angle) * BARREL_RING;
            parts.add(rod(10, 0.02, -0.05, 0.76, 1.0).moved(x, y, 0.0));
            parts.add(Mesh.torus(10, 4, 0.014, 0.005, 1.9).alongZ().moved(x, y, 0.76));
        }
        parts.add(rod(10, 0.024, -0.05, 0.72, 1.1));
        parts.add(Mesh.cylinder(24, 0.096, 0.33, 0.37, 1.1).alongZ());
        parts.add(Mesh.cylinder(24, 0.092, 0.68, 0.72, 1.15).alongZ());
        parts.add(band(0.093, 0.007, 0.7, 1.9));
        parts.add(band(0.097, 0.007, 0.35, 1.9));
        for (int k = 0; k < 6; k++) {
            parts.add(Mesh.box(-0.006, 0.08, 0.37, 0.006, 0.09, 0.68, 1.8).turned(0.0, 0.0, 1.0, 60.0 * k));
        }
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] minigunBody() {
        List<Mesh> parts = new ArrayList<>();
        // The motor housing the barrels turn in, ribbed and lit, and the gearbox behind it.
        parts.add(Mesh.lathe(24, 1.0, 0.0, -0.33, 0.1, -0.33, 0.108, -0.31, 0.108, -0.03, 0.098, -0.01, 0.0, -0.01)
                .alongZ());
        for (double z : new double[] { -0.29, -0.2, -0.11, -0.04 }) {
            parts.add(band(0.109, 0.009, z, 1.6));
        }
        for (int k = 0; k < 8; k++) {
            parts.add(Mesh.box(-0.005, 0.104, -0.27, 0.005, 0.114, -0.06, 1.95).turned(0.0, 0.0, 1.0, 22.5 + 45.0 * k));
        }
        parts.add(Mesh.bevel(-0.088, -0.088, -0.48, 0.088, 0.093, -0.31, 0.012, 1.0));
        twice(parts, Mesh.box(0.088, -0.05, -0.46, 0.093, 0.05, -0.33, 1.9));
        // The spade grips behind, their trigger bar between them.
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.cylinder(10, 0.023, -0.12, 0.075, 1.0).moved(side * 0.1, 0.0, -0.57));
            parts.add(Mesh.tube(false, 6, 0.014, 1.1, path(side * 0.1, 0.07, -0.57, side * 0.076, 0.07, -0.47)));
            parts.add(Mesh.tube(false, 6, 0.014, 1.1, path(side * 0.1, -0.12, -0.57, side * 0.076, -0.08, -0.47)));
            parts.add(Mesh.torus(10, 4, 0.025, 0.006, 1.8).moved(side * 0.1, 0.075, -0.57));
        }
        parts.add(Mesh.tube(false, 6, 0.012, 1.2, path(-0.1, 0.06, -0.57, 0.1, 0.06, -0.57)));
        parts.add(Mesh.bevel(-0.028, 0.068, -0.588, 0.028, 0.092, -0.552, 0.004, 1.95));
        // The carry handle over it, and the feed chute down its right side to the belt box.
        parts.add(Mesh.tube(false, 8, 0.017, 1.15, path(0.0, 0.093, -0.42, 0.0, 0.19, -0.38, 0.0, 0.207, -0.26, 0.0,
                0.19, -0.13, 0.0, 0.1, -0.09)));
        Vec3[] chute = path(-0.09, -0.02, -0.2, -0.16, -0.08, -0.2, -0.19, -0.18, -0.23, -0.19, -0.28, -0.29, -0.17,
                -0.36, -0.37);
        parts.add(Mesh.tube(false, 8, 0.03, 0.95, chute));
        for (int i = 1; i < chute.length; i++) {
            Vec3 along = chute[Math.min(chute.length - 1, i + 1)].subtract(chute[i - 1]);
            parts.add(Mesh.torus(12, 4, 0.032, 0.006, 1.6).pointing(along.x, along.y, along.z)
                    .moved(chute[i].x, chute[i].y, chute[i].z));
        }
        parts.add(Mesh.bevel(-0.25, -0.5, -0.47, -0.09, -0.34, -0.27, 0.015, 1.0));
        parts.add(Mesh.torus(14, 4, 0.04, 0.008, 1.95).alongX().moved(-0.25, -0.42, -0.37));
        return parts.toArray(Mesh[]::new);
    }
}
