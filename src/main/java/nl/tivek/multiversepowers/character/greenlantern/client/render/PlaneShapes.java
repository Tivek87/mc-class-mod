package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;

/**
 * The shapes of the air strike (see {@link PlanePainter}): the plane and the parts of it that move by themselves,
 * its missiles, the rounds of its miniguns and its jets, every one at scale 1 with x to its right, y up and z ahead.
 */
final class PlaneShapes {
    // How long the tip of a propeller blade is from the hub, in blocks at scale 1.
    static final double PROP_RADIUS = 3.25;
    // The bomb bay: how far round the body its doors reach either way from the bottom of its belly, in degrees, and how
    // far they swing open.
    static final double BAY_HALF_ANGLE = 25.0;
    static final double DOOR_OPEN = Math.toRadians(95.0);

    /**
     * The body of the plane, as sections along z (see {@link Mesh#loft}): z, half width, half height, and how high its
     * middle is. A radome nose, the cockpit, the long cabin, and the tail sweeping up over the cargo ramp.
     */
    private static final double[][] HULL = { { 22.4, 0.0, 0.0, -0.55 }, { 22.0, 0.78, 0.66, -0.5 },
            { 21.2, 1.48, 1.28, -0.38 }, { 20.0, 2.08, 1.88, -0.18 }, { 18.4, 2.52, 2.32, 0.0 },
            { 16.2, 2.8, 2.6, 0.1 }, { 13.0, 2.92, 2.72, 0.12 }, { 6.0, 2.96, 2.74, 0.12 },
            { -4.0, 2.96, 2.74, 0.12 }, { -9.0, 2.9, 2.64, 0.3 }, { -13.0, 2.62, 2.22, 0.85 },
            { -16.5, 2.12, 1.62, 1.45 }, { -19.5, 1.46, 1.06, 1.95 }, { -21.8, 0.76, 0.56, 2.3 },
            { -22.6, 0.0, 0.0, 2.4 } };

    /** The plane itself, everything on it that does not move by itself: at scale 1, x to its right, y up, z ahead. */
    static final ConstructPainter.Shape BODY = ConstructPainter.Shape.of(body());
    /** One propeller round its hub, turning about z: its spinner and four twisted blades. */
    static final ConstructPainter.Shape PROPELLER = ConstructPainter.Shape.of(propeller());
    /** A minigun round the ball it turns on, pointing along z: the ball, the housing, its motor and its ammunition. */
    static final ConstructPainter.Shape GUN = ConstructPainter.Shape.of(gun());
    /** The six barrels of a minigun, with their clamps and muzzle ring, turning about z. */
    static final ConstructPainter.Shape BARRELS = ConstructPainter.Shape.of(barrels());
    /**
     * A homing missile of hard light along z, its nose ahead: 4.6 blocks long at scale 1, drawn {@link #MISSILE_SCALE}
     * times that, on its rail and in the air alike.
     */
    static final ConstructPainter.Shape MISSILE = ConstructPainter.Shape.of(missile(0.36));
    static final double MISSILE_TAIL = -2.3;
    static final double MISSILE_SCALE = AirStrike.MISSILE_SCALE;
    /** How big a jet's small missile is drawn, next to the model of a missile. */
    static final double SMALL_MISSILE_SCALE = AirStrike.SMALL_MISSILE_SCALE;
    /** A round from a minigun: a slug of hard light along z, its nose ahead, drawn {@link #SLUG_SCALE} times as big. */
    static final ConstructPainter.Shape SLUG = ConstructPainter.Shape.of(Mesh.lathe(8, 1.35, 0.0, -0.45, 0.09,
            -0.4, 0.11, 0.15, 0.07, 0.4, 0.0, 0.5).alongZ());
    static final double SLUG_SCALE = 1.6;
    /** One door of the bomb bay, the right one, on its hinge along the body: the left one is its mirror image. */
    static final ConstructPainter.Shape BAY_DOOR = ConstructPainter.Shape.of(bayDoor());
    /** The inside of the bomb bay, deep in the belly, that shows while its doors are open. */
    static final ConstructPainter.Shape BAY_INSIDE = ConstructPainter.Shape.of(bayInside());
    /** A jet: at scale 1 some 13.5 blocks long and 11 wide, x to its right, y up, z ahead. */
    static final ConstructPainter.Shape JET = ConstructPainter.Shape.of(jet());

    private PlaneShapes() {
    }

    /** The body's section at {@code z}, worked out between the sections of {@link #HULL}. */
    static double[] hull(double z) {
        for (int i = 0; i + 1 < HULL.length; i++) {
            double[] a = HULL[i];
            double[] b = HULL[i + 1];
            if (z <= a[0] && z >= b[0]) {
                double t = (a[0] - z) / (a[0] - b[0]);
                return new double[] { z, Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2]), Mth.lerp(t, a[3], b[3]) };
            }
        }
        double[] end = z > HULL[0][0] ? HULL[0] : HULL[HULL.length - 1];
        return new double[] { z, end[1], end[2], end[3] };
    }

    /** A plate on the body (see {@link Mesh#panel}) from {@code z0} to {@code z1}, between two angles in degrees. */
    private static Mesh plate(double z0, double z1, double from, double to, double thick, double bright) {
        int pieces = Math.max(1, (int) Math.ceil(Math.abs(z1 - z0) / 1.2));
        double[][] sections = new double[pieces + 1][];
        for (int i = 0; i <= pieces; i++) {
            sections[i] = hull(Mth.lerp((double) i / pieces, z1, z0));
        }
        int steps = Math.max(2, (int) Math.ceil(Math.abs(to - from) / 9.0));
        return Mesh.panel(steps, bright, Math.toRadians(from), Math.toRadians(to), thick, sections);
    }

    /** The same plate on both sides of the body: as given, and mirrored to the other side. */
    private static void pair(List<Mesh> parts, Mesh right) {
        parts.add(right);
        parts.add(right.mirrored());
    }

    private static Mesh[] body() {
        List<Mesh> parts = new ArrayList<>();
        // The body, and the fairing on its back where the wing sits.
        parts.add(Mesh.loft(32, 1.0, HULL));
        parts.add(Mesh.loft(20, 1.0, new double[] { 6.8, 0.0, 0.0, 2.7 }, new double[] { 5.4, 1.5, 0.5, 2.72 },
                new double[] { -4.6, 1.5, 0.5, 2.72 }, new double[] { -6.8, 0.0, 0.0, 2.7 }));
        // The windscreen and the side windows of the cockpit, burning bright, like glass full of light.
        parts.add(plate(18.5, 20.1, 58.0, 122.0, 0.05, 1.75));
        pair(parts, plate(17.2, 18.4, 26.0, 50.0, 0.05, 1.6));
        // A seam round the radome, and bands of plating round the body.
        parts.add(plate(20.55, 20.75, 0.0, 360.0, 0.025, 1.35));
        for (double z : new double[] { 14.5, 4.0, -6.0, -11.5 }) {
            parts.add(plate(z - 0.12, z + 0.12, 0.0, 360.0, 0.03, 1.2));
        }
        // A row of windows along each side, the crew door up front on the left, the paratroop doors at the back, and the
        // cargo ramp under the tail.
        for (double z : new double[] { 11.6, 8.2, 4.8, 1.4, -2.0 }) {
            pair(parts, plate(z, z + 0.75, 6.0, 16.0, 0.04, 1.5));
        }
        parts.add(plate(14.8, 16.1, 162.0, 202.0, 0.03, 1.2));
        pair(parts, plate(-9.6, -8.0, -14.0, 22.0, 0.03, 1.22));
        parts.add(plate(-19.0, -11.5, 232.0, 308.0, 0.035, 1.18));
        // The wings, high on top of the body, each with a flap and an aileron along its trailing edge.
        pair(parts, Mesh.wing(27.0, -3.6, 4.4, -1.8, 2.2, 0.5, 1.15, 0.42, 1.0).moved(1.0, 2.95, 0.0));
        pair(parts, Mesh.wing(13.0, -4.55, -3.5, -3.62, -2.62, 0.24, 0.26, 0.18, 1.08).moved(3.0, 2.86, 0.0));
        pair(parts, Mesh.wing(9.5, -3.4, -2.52, -2.72, -1.9, 0.18, 0.2, 0.14, 1.08).moved(17.0, 3.25, 0.0));
        // Four engines slung under the wings, each with its intake under the propeller and its exhaust stack outboard.
        for (double x : new double[] { AirStrike.ENGINE_X, AirStrike.ENGINE_OUTER_X }) {
            pair(parts, Mesh.loft(18, 1.0, new double[] { 8.0, 0.95, 1.0, 0.0 },
                    new double[] { 7.2, 1.08, 1.12, -0.05 }, new double[] { 4.2, 1.1, 1.22, -0.12 },
                    new double[] { 0.5, 1.0, 1.12, -0.1 }, new double[] { -2.6, 0.78, 0.85, 0.05 },
                    new double[] { -4.8, 0.3, 0.32, 0.18 }, new double[] { -5.3, 0.0, 0.0, 0.2 })
                    .moved(x, AirStrike.ENGINE_Y, 0.0));
            pair(parts, Mesh.box(-0.42, -1.42, 5.0, 0.42, -0.95, 7.7, 1.15).moved(x, AirStrike.ENGINE_Y, 0.0));
            pair(parts, Mesh.tube(false, 8, 0.2, 1.1, new Vec3(x + 0.95, AirStrike.ENGINE_Y + 0.35, 1.6),
                    new Vec3(x + 1.2, AirStrike.ENGINE_Y + 0.42, -0.7)));
        }
        // The tail: a tall fin with its rudder and a fillet running forward from it, and the tailplanes with their
        // elevators.
        parts.add(Mesh.wing(10.5, -22.2, -13.5, -23.4, -19.6, 0.0, 0.7, 0.35, 1.0).turned(0.0, 0.0, 1.0, 90.0)
                .moved(0.0, 2.6, 0.0));
        parts.add(Mesh.wing(9.8, -23.3, -22.15, -24.3, -23.35, 0.0, 0.3, 0.2, 1.08).turned(0.0, 0.0, 1.0, 90.0)
                .moved(0.0, 3.0, 0.0));
        parts.add(Mesh.wing(2.3, -14.0, -7.0, -14.2, -13.3, 0.0, 0.45, 0.3, 1.0).turned(0.0, 0.0, 1.0, 90.0)
                .moved(0.0, 2.7, 0.0));
        pair(parts, Mesh.wing(9.0, -21.6, -16.0, -21.2, -18.8, 0.3, 0.55, 0.25, 1.0).moved(0.9, 2.2, 0.0));
        pair(parts, Mesh.wing(8.4, -22.5, -21.55, -21.95, -21.15, 0.28, 0.25, 0.15, 1.08).moved(1.0, 2.2, 0.0));
        // The landing gear pods on its lower sides, each with the seam of its doors.
        pair(parts, Mesh.loft(16, 1.0, new double[] { 6.6, 0.0, 0.0, 0.0 }, new double[] { 5.8, 0.82, 0.86, 0.0 },
                new double[] { -3.4, 0.86, 0.9, 0.0 }, new double[] { -4.8, 0.0, 0.0, 0.0 }).moved(2.55, -1.75, 0.0));
        pair(parts, Mesh.box(-0.05, -0.02, -3.0, 0.05, 0.05, 5.4, 1.45).moved(2.62, -2.62, 0.0));
        // The sponsons of the miniguns: a long blister on each side of the belly, a stub reaching out of it and the big
        // round collar the gun's ball turns in, with a glowing ring round its lip.
        pair(parts, Mesh.loft(18, 1.0, new double[] { 15.4, 0.0, 0.0, 0.0 }, new double[] { 14.2, 0.7, 0.8, 0.0 },
                new double[] { 11.8, 0.95, 1.05, 0.0 }, new double[] { 7.6, 0.95, 1.05, 0.0 },
                new double[] { 5.6, 0.6, 0.7, 0.0 }, new double[] { 4.4, 0.0, 0.0, 0.0 })
                .moved(2.45, AirStrike.GUN_Y + 0.35, 0.0));
        pair(parts, Mesh.cylinder(20, 1.22, 0.0, 0.9, 1.08).alongX().moved(AirStrike.GUN_X - 1.2, AirStrike.GUN_Y,
                AirStrike.GUN_Z));
        pair(parts, Mesh.torus(24, 6, 1.22, 0.11, 1.55).alongX().moved(AirStrike.GUN_X - 0.3, AirStrike.GUN_Y,
                AirStrike.GUN_Z));
        // The bomb bay in its belly: a glowing rim round its hatch, at its front and back and along the hinges of its
        // two doors.
        double bayBack = AirStrike.BAY_Z - AirStrike.BAY_LENGTH * 0.5;
        double bayFront = AirStrike.BAY_Z + AirStrike.BAY_LENGTH * 0.5;
        for (double z : new double[] { bayBack - 0.18, bayFront }) {
            parts.add(plate(z, z + 0.18, 270.0 - BAY_HALF_ANGLE - 3.0, 270.0 + BAY_HALF_ANGLE + 3.0, 0.07, 1.55));
        }
        pair(parts, plate(bayBack, bayFront, 270.0 + BAY_HALF_ANGLE, 270.0 + BAY_HALF_ANGLE + 3.0, 0.07, 1.55));
        // The ring's light running through it: glowing strips along the leading edges of its wings and tailplanes, a
        // spine along its back and two seams along its belly, broken off round the hatch.
        pair(parts, Mesh.tube(false, 6, 0.13, 1.7, new Vec3(3.0, 2.99, 4.32), new Vec3(28.0, 3.45, 2.24)));
        pair(parts, Mesh.tube(false, 5, 0.09, 1.65, new Vec3(1.3, 2.2, -16.0), new Vec3(9.9, 2.5, -18.8)));
        parts.add(plate(-13.5, 17.2, 87.0, 93.0, 0.035, 1.55));
        pair(parts, plate(-11.5, bayBack - 0.3, 256.0, 260.0, 0.035, 1.6));
        pair(parts, plate(bayFront + 0.3, 17.0, 256.0, 260.0, 0.035, 1.6));
        // Flap track fairings: slim pods under the trailing edge of each wing, where the flaps run out on their tracks.
        for (double x : new double[] { 5.0, 9.5, 14.2, 19.5, 24.5 }) {
            double rise = 2.95 + 0.5 * (x - 1.0) / 27.0;
            pair(parts, Mesh.loft(10, 1.02, new double[] { -6.0 + 0.07 * x, 0.0, 0.0, 0.0 },
                    new double[] { -5.3 + 0.07 * x, 0.16, 0.2, 0.0 }, new double[] { -3.2 + 0.07 * x, 0.25, 0.32, 0.0 },
                    new double[] { -0.8 + 0.07 * x, 0.2, 0.24, 0.05 }, new double[] { 0.4 + 0.07 * x, 0.0, 0.0, 0.1 })
                    .moved(x, rise - 0.45, 0.0));
        }
        // Landing lights under its nose and at the roots of its wings, burning bright.
        pair(parts, Mesh.ball(10, 6, 0.26, 2.0).moved(0.85, -2.2, 17.0));
        pair(parts, Mesh.ball(10, 6, 0.3, 2.0).moved(4.2, 2.62, 3.4));
        // The sensor ball under the nose, on its mount, with its lens looking forward and down.
        parts.add(Mesh.cylinder(14, 0.36, -0.55, 0.05, 1.0).moved(0.0, AirStrike.SENSOR_Y + 0.72, AirStrike.SENSOR_Z));
        parts.add(Mesh.ball(16, 10, 0.55, 1.1).moved(0.0, AirStrike.SENSOR_Y, AirStrike.SENSOR_Z));
        parts.add(Mesh.cylinder(12, 0.26, 0.0, 0.08, 1.8).pointing(0.0, -0.55, 0.84)
                .moved(0.0, AirStrike.SENSOR_Y - 0.28, AirStrike.SENSOR_Z + 0.42));
        // Blade antennas on its back and belly, and pitot tubes on its nose.
        for (double z : new double[] { 9.0, -2.5 }) {
            parts.add(Mesh.wing(1.0, z - 0.45, z + 0.35, z - 0.6, z - 0.1, 0.0, 0.12, 0.06, 1.2)
                    .turned(0.0, 0.0, 1.0, 90.0).moved(0.0, 2.8, 0.0));
        }
        parts.add(Mesh.wing(0.8, 5.4, 6.1, 5.3, 5.7, 0.0, 0.1, 0.05, 1.2).turned(0.0, 0.0, 1.0, -90.0)
                .moved(0.0, -2.55, 0.0));
        pair(parts, Mesh.cylinder(6, 0.05, 0.0, 1.3, 1.3).alongZ().moved(1.45, 0.35, 19.6));
        // Lights on the wingtips and the tail.
        pair(parts, Mesh.ball(8, 6, 0.24, 1.9).moved(28.0, 3.45, 0.4));
        parts.add(Mesh.ball(8, 6, 0.22, 1.9).moved(0.0, 2.45, -22.75));
        // The lantern emblem: on both sides of the fin, its bars along the fin, and on top of each wing.
        pair(parts, emblem(1.25).turned(0.0, 1.0, 0.0, 90.0).turned(0.0, 0.0, 1.0, -90.0).moved(0.4, 7.4, -20.3));
        pair(parts, emblem(1.6).moved(21.0, 3.62, 0.3));
        // ... and under each wing, big, where you see it from the ground.
        pair(parts, emblem(1.9).moved(19.0, 2.8, 1.0));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * The lantern emblem as a roundel lying flat round y: a ring with a bar over and under its middle, the way the ring
     * marks what it makes.
     */
    private static Mesh emblem(double radius) {
        double bar = radius * 0.24;
        return Mesh.merged(Mesh.torus(24, 5, radius, radius * 0.12, 1.75),
                Mesh.box(-radius, -0.07, bar * 0.9, radius, 0.1, bar * 1.6, 1.75),
                Mesh.box(-radius, -0.07, -bar * 1.6, radius, 0.1, -bar * 0.9, 1.75));
    }

    private static Mesh[] propeller() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(16, 1.2, 0.0, 0.0, 0.58, 0.0, 0.56, 0.45, 0.44, 0.95, 0.24, 1.32, 0.0, 1.5).alongZ());
        for (int k = 0; k < 4; k++) {
            // Each blade twisted a little along its length against the air, and turned a quarter from the last.
            Mesh blade = Mesh.wing(PROP_RADIUS - 0.5, -0.34, 0.3, -0.2, 0.2, 0.0, 0.16, 0.07, 1.12)
                    .turned(1.0, 0.0, 0.0, 24.0).moved(0.5, 0.0, 0.55);
            parts.add(blade.turned(0.0, 0.0, 1.0, 90.0 * k));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A minigun round the ball it turns on, pointing along z: the ball, the receiver with a glowing sight on top, the
     * cooling jacket round the root of the barrels, the motor behind it, a big ammunition drum on its left side with the
     * feed chute into the receiver, and two spade grips at its back.
     */
    private static Mesh[] gun() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.ball(18, 10, 1.0, 1.0));
        parts.add(Mesh.box(-0.66, -0.62, -2.1, 0.66, 0.62, 1.05, 1.0));
        parts.add(Mesh.cylinder(14, 0.5, -2.1, 1.05, 1.04).alongZ().moved(0.0, 0.5, 0.0));
        parts.add(Mesh.box(-0.12, 1.0, -0.9, 0.12, 1.28, 0.6, 1.7));
        parts.add(Mesh.cylinder(18, 0.66, 0.9, 2.3, 1.02).alongZ());
        parts.add(Mesh.torus(20, 5, 0.66, 0.07, 1.5).alongZ().moved(0.0, 0.0, 2.3));
        parts.add(Mesh.cylinder(14, 0.44, -2.9, -2.1, 0.96).alongZ());
        parts.add(Mesh.torus(16, 4, 0.44, 0.06, 1.4).alongZ().moved(0.0, 0.0, -2.9));
        parts.add(Mesh.cylinder(20, 0.8, -0.45, 0.45, 1.0).alongX().moved(-1.25, -0.25, -0.9));
        parts.add(Mesh.torus(20, 5, 0.8, 0.06, 1.5).alongX().moved(-0.8, -0.25, -0.9));
        parts.add(Mesh.tube(false, 8, 0.16, 1.05, new Vec3(-1.0, 0.45, -0.7), new Vec3(-0.75, 0.75, -0.45),
                new Vec3(-0.45, 0.62, -0.2)));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.tube(false, 6, 0.08, 1.1, new Vec3(0.3 * side, 0.2, -2.1), new Vec3(0.34 * side, 0.1, -2.7),
                    new Vec3(0.34 * side, -0.35, -2.8)));
        }
        return parts.toArray(Mesh[]::new);
    }

    /** The six barrels of a minigun, with the clamps that hold them together and a muzzle brake, turning about z. */
    private static Mesh[] barrels() {
        List<Mesh> parts = new ArrayList<>();
        for (int k = 0; k < 6; k++) {
            double angle = Math.PI * 2.0 * k / 6.0;
            parts.add(Mesh.cylinder(8, 0.13, 1.0, AirStrike.GUN_LENGTH - 0.05, 1.05).alongZ()
                    .moved(0.37 * Math.cos(angle), 0.37 * Math.sin(angle), 0.0));
        }
        parts.add(Mesh.cylinder(10, 0.13, 1.0, AirStrike.GUN_LENGTH - 0.3, 1.0).alongZ());
        for (double z : new double[] { 3.0, 4.6 }) {
            parts.add(Mesh.torus(20, 5, 0.52, 0.08, 1.2).alongZ().moved(0.0, 0.0, z));
        }
        parts.add(Mesh.cylinder(20, 0.6, AirStrike.GUN_LENGTH - 0.45, AirStrike.GUN_LENGTH, 1.1).alongZ());
        parts.add(Mesh.torus(20, 5, 0.6, 0.09, 1.5).alongZ().moved(0.0, 0.0, AirStrike.GUN_LENGTH));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A missile along z, {@code r} thick: its bright seeker in the nose, canards behind it, a band round its body and
     * four fins at its tail.
     */
    private static Mesh[] missile(double r) {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.loft(14, 1.1, new double[] { 2.3, 0.0, 0.0, 0.0 }, new double[] { 1.95, r * 0.55, r * 0.55, 0.0 },
                new double[] { 1.5, r * 0.9, r * 0.9, 0.0 }, new double[] { 1.15, r, r, 0.0 },
                new double[] { -2.05, r, r, 0.0 }, new double[] { -2.3, r * 0.8, r * 0.8, 0.0 }));
        parts.add(Mesh.ball(10, 6, r * 0.42, 1.85).moved(0.0, 0.0, 2.12));
        parts.add(Mesh.torus(16, 4, r + 0.02, 0.03, 1.4).alongZ().moved(0.0, 0.0, -0.6));
        for (int k = 0; k < 4; k++) {
            parts.add(Mesh.wing(r * 1.7, -2.3, -1.2, -2.3, -1.85, 0.0, 0.08, 0.04, 1.1).moved(r * 0.7, 0.0, 0.0)
                    .turned(0.0, 0.0, 1.0, 90.0 * k));
            parts.add(Mesh.wing(r * 0.9, 0.85, 1.35, 0.95, 1.15, 0.0, 0.05, 0.03, 1.1).moved(r * 0.7, 0.0, 0.0)
                    .turned(0.0, 0.0, 1.0, 45.0 + 90.0 * k));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * The right door of the bomb bay: a curved plate from the middle of the belly out to its hinge, stiffened by three
     * glowing ribs across it and a glowing lip along its free edge.
     */
    private static Mesh[] bayDoor() {
        double back = AirStrike.BAY_Z - AirStrike.BAY_LENGTH * 0.5 + 0.05;
        double front = AirStrike.BAY_Z + AirStrike.BAY_LENGTH * 0.5 - 0.05;
        List<Mesh> parts = new ArrayList<>();
        parts.add(plate(back, front, 270.0, 270.0 + BAY_HALF_ANGLE, 0.06, 1.08));
        for (double z : new double[] { back + 1.6, AirStrike.BAY_Z, front - 1.6 }) {
            parts.add(plate(z - 0.08, z + 0.08, 271.0, 270.0 + BAY_HALF_ANGLE - 1.0, 0.085, 1.45));
        }
        parts.add(plate(back, front, 270.2, 271.6, 0.08, 1.6));
        return parts.toArray(Mesh[]::new);
    }

    /** The inside of the bomb bay: a dark hollow under the doors, and the glowing clamps a missile hangs from. */
    private static Mesh[] bayInside() {
        double back = AirStrike.BAY_Z - AirStrike.BAY_LENGTH * 0.5;
        double front = AirStrike.BAY_Z + AirStrike.BAY_LENGTH * 0.5;
        List<Mesh> parts = new ArrayList<>();
        parts.add(plate(back, front, 270.0 - BAY_HALF_ANGLE, 270.0 + BAY_HALF_ANGLE, 0.012, 0.32));
        for (double z : new double[] { AirStrike.BAY_Z - 2.2, AirStrike.BAY_Z + 2.2 }) {
            parts.add(Mesh.box(-0.5, -1.4, z - 0.14, 0.5, -1.2, z + 0.14, 1.5));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A jet: a long, slim body with a pointed nose and a seam round it, a bubble canopy of bright light, two air intakes
     * with glowing lips, swept wings with glowing leading edges, the lantern emblem and a light on each tip, a pylon under
     * each wing, two tails canted outward, tailplanes, two small fins under its tail, a glowing spine along its back and
     * two exhaust nozzles with a ring of light in each.
     */
    private static Mesh[] jet() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.loft(20, 1.0, new double[] { 7.4, 0.0, 0.0, -0.05 }, new double[] { 6.9, 0.2, 0.18, -0.04 },
                new double[] { 6.0, 0.42, 0.38, 0.0 }, new double[] { 4.6, 0.62, 0.55, 0.05 },
                new double[] { 3.0, 0.8, 0.62, 0.08 }, new double[] { 1.2, 1.1, 0.62, 0.02 },
                new double[] { -1.8, 1.25, 0.6, 0.0 }, new double[] { -4.2, 1.2, 0.55, 0.02 },
                new double[] { -5.5, 1.05, 0.48, 0.05 }, new double[] { -5.9, 0.0, 0.0, 0.05 }));
        parts.add(Mesh.torus(20, 4, 0.47, 0.035, 1.45).alongZ().scaled(1.0, 0.9, 1.0).moved(0.0, 0.0, 5.7));
        // The canopy, like glass full of light, with a frame across it.
        parts.add(Mesh.loft(16, 1.8, new double[] { 5.2, 0.0, 0.0, 0.42 }, new double[] { 4.6, 0.28, 0.26, 0.47 },
                new double[] { 3.4, 0.4, 0.36, 0.52 }, new double[] { 2.0, 0.34, 0.3, 0.5 },
                new double[] { 1.2, 0.0, 0.0, 0.45 }));
        parts.add(Mesh.tube(false, 6, 0.045, 1.25, new Vec3(-0.38, 0.5, 3.2), new Vec3(0.0, 0.9, 3.25),
                new Vec3(0.38, 0.5, 3.2)));
        // The intakes on its sides, each with a glowing lip.
        pair(parts, Mesh.loft(10, 1.0, new double[] { 3.2, 0.0, 0.0, 0.0 }, new double[] { 3.1, 0.34, 0.4, 0.0 },
                new double[] { 0.5, 0.36, 0.42, 0.0 }, new double[] { -1.2, 0.0, 0.0, 0.0 }).moved(1.05, -0.15, 0.0));
        pair(parts, Mesh.torus(12, 4, 0.34, 0.05, 1.7).alongZ().scaled(1.0, 1.18, 1.0).moved(1.05, -0.15, 3.12));
        // The wings, their glowing leading edges, the emblem on each, the lights on their tips and a pylon under each.
        pair(parts, Mesh.wing(4.4, -3.8, 1.6, -4.4, -2.9, -0.15, 0.26, 0.08, 1.0).moved(1.0, -0.05, 0.0));
        pair(parts, Mesh.tube(false, 5, 0.06, 1.7, new Vec3(1.1, 0.0, 1.55), new Vec3(5.35, -0.18, -2.9)));
        pair(parts, emblem(0.7).moved(3.0, 0.08, -1.7));
        pair(parts, Mesh.ball(8, 6, 0.14, 1.9).moved(5.42, -0.2, -3.6));
        pair(parts, Mesh.box(-0.07, -0.34, -0.9, 0.07, -0.08, 0.8, 1.05).moved(AirStrike.PYLON_X, 0.0, 0.0));
        // The tails, canted outward, with glowing leading edges, and the tailplanes.
        pair(parts, Mesh.wing(2.6, -5.6, -3.4, -6.0, -5.0, 0.0, 0.2, 0.07, 1.0).turned(0.0, 0.0, 1.0, 70.0)
                .moved(0.75, 0.35, 0.0));
        pair(parts, Mesh.tube(false, 5, 0.05, 1.65, new Vec3(0.76, 0.4, -3.45), new Vec3(1.63, 2.78, -5.0)));
        pair(parts, Mesh.wing(2.3, -6.1, -4.4, -6.3, -5.6, 0.0, 0.16, 0.06, 1.0).moved(1.05, -0.05, 0.0));
        pair(parts, Mesh.wing(0.7, -5.2, -3.9, -5.4, -4.9, 0.0, 0.1, 0.05, 1.0).turned(0.0, 0.0, 1.0, -80.0)
                .moved(0.7, -0.45, 0.0));
        parts.add(Mesh.tube(false, 5, 0.05, 1.6, new Vec3(0.0, 0.62, 1.0), new Vec3(0.0, 0.58, -4.5)));
        // The nozzles, each with a ring of light deep inside it.
        pair(parts, Mesh.lathe(16, 1.05, 0.0, -0.9, 0.46, -0.9, 0.5, -0.2, 0.44, 0.35, 0.0, 0.35).alongZ()
                .moved(0.55, 0.0, -5.6));
        pair(parts, Mesh.torus(16, 4, 0.38, 0.06, 2.0).alongZ().moved(0.55, 0.0, -6.46));
        return parts.toArray(Mesh[]::new);
    }
}
