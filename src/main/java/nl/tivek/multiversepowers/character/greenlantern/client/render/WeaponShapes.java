package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructIcons;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;

/**
 * The hard-light models of the weapons on the construct wheel besides the sword and shield (see {@link SwordPainter}):
 * solid like every construct, with their details (edges, bores, grooves, the lantern emblem) picked out in brighter
 * light. For now they are the pictures on the wheel and on the bar above your hotbar (see {@link ConstructIcons}).
 *
 * <p>Every model is measured in blocks at scale 1: z along its length (from the grip to the end that does the work), y
 * up (the top of a gun, the edge of a blade) and x across. A glove is a right hand, palm down, its knuckles along z.
 */
public final class WeaponShapes {
    /** A square with well rounded corners, for bodies and grips; and a blade bevelled off to both edges. */
    private static final double[] SQUIRCLE = squircle(16);
    private static final double[] BLADE = { 0.0, 1.0, -0.35, 0.72, -1.0, 0.25, -1.0, -0.25, -0.35, -0.72, 0.0, -1.0,
            0.35, -0.72, 1.0, -0.25, 1.0, 0.25, 0.35, 0.72 };
    private static final double[] OCTAGON = { 1.0, 0.414, 0.414, 1.0, -0.414, 1.0, -1.0, 0.414, -1.0, -0.414, -0.414,
            -1.0, 0.414, -1.0, 1.0, -0.414 };

    public static final ConstructPainter.Shape WHIP = ConstructPainter.Shape.of(whip());
    public static final ConstructPainter.Shape GLOVE = ConstructPainter.Shape.of(glove());
    /** The left glove: the right one mirrored. */
    public static final ConstructPainter.Shape LEFT_GLOVE = mirrored(GLOVE);
    public static final ConstructPainter.Shape DAGGER = ConstructPainter.Shape.of(dagger());
    public static final ConstructPainter.Shape BATTLEAXE = ConstructPainter.Shape.of(battleaxe());
    public static final ConstructPainter.Shape WAR_HAMMER = ConstructPainter.Shape.of(warHammer());
    public static final ConstructPainter.Shape HALBERD = ConstructPainter.Shape.of(halberd());
    public static final ConstructPainter.Shape CHAINSAW = ConstructPainter.Shape.of(chainsaw());
    public static final ConstructPainter.Shape REVOLVER = ConstructPainter.Shape.of(revolver());
    public static final ConstructPainter.Shape SHOTGUN = ConstructPainter.Shape.of(shotgun());
    public static final ConstructPainter.Shape SMG = ConstructPainter.Shape.of(smg());
    public static final ConstructPainter.Shape ARM_CANNON = ConstructPainter.Shape.of(armCannon());
    public static final ConstructPainter.Shape GRENADE_LAUNCHER = ConstructPainter.Shape.of(grenadeLauncher());
    public static final ConstructPainter.Shape MINIGUN = ConstructPainter.Shape.of(minigun());
    public static final ConstructPainter.Shape ROCKET_LAUNCHER = ConstructPainter.Shape.of(rocketLauncher());
    public static final ConstructPainter.Shape FLAMETHROWER = ConstructPainter.Shape.of(flamethrower());

    private WeaponShapes() {
    }

    // ---- The melee weapons ----

    /**
     * The energy whip: a handle with a pommel and its gem, a cord wound round the grip and a collar; out of that the lash
     * curls up and round into a spiral, thinner and thinner, knotted where it narrows, with a cracker at its tip.
     */
    private static Mesh[] whip() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.ball(14, 8, 0.062, 1.2).scaled(1.0, 1.0, 0.85).moved(0.0, 0.0, -0.37));
        parts.add(Mesh.ball(10, 6, 0.026, 2.0).moved(0.0, 0.0, -0.42));
        parts.add(rod(12, 0.042, -0.34, 0.0, 0.95));
        parts.add(Mesh.tube(false, 5, 0.01, 1.35, helix(0.046, -0.32, -0.02, 6.0)));
        parts.add(band(0.05, 0.014, -0.33, 1.5));
        parts.add(Mesh.cone(14, 0.058, 0.026, 0.0, 0.1, 1.2).alongZ());
        parts.add(band(0.058, 0.014, 0.004, 1.6));
        int count = 49;
        Vec3[] lash = new Vec3[count];
        for (int i = 0; i < count; i++) {
            double s = (double) i / (count - 1);
            double angle = -Math.PI * 0.5 + s * Math.PI * 2.6;
            double radius = 0.33 * (1.0 - 0.72 * s);
            lash[i] = new Vec3(0.08 * Math.sin(s * Math.PI), 0.33 + Math.sin(angle) * radius,
                    0.1 + Math.cos(angle) * radius);
        }
        int pieces = 6;
        for (int k = 0; k < pieces; k++) {
            int from = k * (count - 1) / pieces;
            int to = (k + 1) * (count - 1) / pieces;
            double thick = 0.03 - 0.021 * k / (pieces - 1);
            parts.add(Mesh.tube(false, 7, thick, 1.45, Arrays.copyOfRange(lash, from, to + 1)));
            if (k > 0) {
                Vec3 along = lash[from + 1].subtract(lash[from - 1]);
                parts.add(Mesh.torus(12, 4, thick + 0.004, 0.006, 1.9).pointing(along.x, along.y, along.z)
                        .moved(lash[from].x, lash[from].y, lash[from].z));
            }
        }
        Vec3 tip = lash[count - 1];
        Vec3 way = tip.subtract(lash[count - 2]);
        parts.add(Mesh.cone(8, 0.012, 0.0, 0.0, 0.07, 1.9).pointing(way.x, way.y, way.z).moved(tip.x, tip.y, tip.z));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A boxing glove: the padded fist with a roll of padding bulging over the knuckles and a row of studs in it, the
     * thumb laid along its side, and a long cuff round the wrist with a strap and its buckle, the lantern emblem on both
     * sides and a rim round its opening.
     */
    private static Mesh[] glove() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.ball(20, 14, 1.0, 1.0).scaled(0.2, 0.21, 0.24).moved(0.0, 0.02, 0.12));
        parts.add(Mesh.ball(16, 10, 1.0, 1.05).scaled(0.185, 0.15, 0.14).moved(0.0, 0.08, 0.24));
        for (double x : new double[] { -0.12, -0.04, 0.04, 0.12 }) {
            double z = 0.24 + 0.14 * Math.sqrt(1.0 - (x / 0.185) * (x / 0.185)) - 0.01;
            parts.add(Mesh.ball(8, 5, 0.028, 1.7).moved(x, 0.1, z));
        }
        parts.add(Mesh.ball(14, 10, 1.0, 0.95).scaled(0.075, 0.085, 0.17).turned(0.0, 1.0, 0.0, 14.0)
                .moved(-0.18, -0.04, 0.1));
        parts.add(Mesh.cone(18, 0.13, 0.15, -0.42, -0.04, 0.95).alongZ().scaled(1.0, 0.92, 1.0).moved(0.0, -0.02, 0.0));
        parts.add(Mesh.ring(24, 1.3, 0.146, -0.035, 0.164, -0.035, 0.164, 0.035, 0.146, 0.035).alongZ()
                .scaled(1.0, 0.92, 1.0).moved(0.0, -0.02, -0.11));
        parts.add(Mesh.box(-0.05, 0.12, -0.15, 0.05, 0.16, -0.07, 1.5));
        twice(parts, Mesh.torus(18, 5, 0.045, 0.01, 1.9).alongX().moved(0.14, -0.02, -0.26));
        twice(parts, Mesh.box(0.11, 0.042, -0.31, 0.14, 0.056, -0.21, 1.9));
        twice(parts, Mesh.box(0.11, -0.096, -0.31, 0.14, -0.082, -0.21, 1.9));
        parts.add(Mesh.torus(20, 6, 0.13, 0.02, 1.4).alongZ().scaled(1.0, 0.92, 1.0).moved(0.0, -0.02, -0.42));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * An energy dagger: a straight blade bevelled to both edges with a glowing channel and a ring of light on each face,
     * a crossguard curving towards the blade with a knob at either end, a wrapped grip and a pommel with a gem.
     */
    private static Mesh[] dagger() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.sweep(1.15, BLADE, new double[] { 0.05, 0.02, 0.068, 0.0 },
                new double[] { 0.12, 0.02, 0.074, 0.0 }, new double[] { 0.34, 0.017, 0.058, 0.0 },
                new double[] { 0.47, 0.011, 0.034, 0.0 }, new double[] { 0.56, 0.0, 0.0, 0.0 }));
        twice(parts, Mesh.box(0.013, -0.011, 0.14, 0.021, 0.011, 0.4, 2.0));
        twice(parts, Mesh.torus(12, 4, 0.026, 0.006, 1.9).alongX().moved(0.02, 0.0, 0.095));
        parts.add(Mesh.tube(false, 7, 0.016, 1.2, path(0.0, -0.13, 0.085, 0.0, -0.1, 0.055, 0.0, -0.05, 0.04, 0.0, 0.05,
                0.04, 0.0, 0.1, 0.055, 0.0, 0.13, 0.085)));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.ball(10, 6, 0.024, 1.5).moved(0.0, side * 0.135, 0.09));
        }
        parts.add(Mesh.box(-0.03, -0.045, 0.01, 0.03, 0.045, 0.06, 1.1));
        parts.add(rod(10, 0.022, -0.13, 0.01, 0.92));
        for (double z : new double[] { -0.11, -0.075, -0.04, -0.005 }) {
            parts.add(band(0.024, 0.006, z, 1.3));
        }
        parts.add(Mesh.ball(12, 7, 0.034, 1.25).scaled(1.0, 1.0, 0.8).moved(0.0, 0.0, -0.16));
        parts.add(Mesh.ball(8, 5, 0.014, 2.0).moved(0.0, 0.0, -0.19));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A double-bitted battleaxe: a long haft with a wound grip, bands and a pommel; a socket with bands; two crescent
     * bits with a raised cheek, rivets and a glowing edge; and a spike on top.
     */
    private static Mesh[] battleaxe() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(rod(12, 0.034, -0.92, 0.52, 0.95));
        parts.add(Mesh.tube(false, 5, 0.009, 1.3, helix(0.037, -0.86, -0.48, 8.0)));
        for (double z : new double[] { -0.88, -0.46, 0.12 }) {
            parts.add(band(0.042, 0.012, z, 1.5));
        }
        parts.add(Mesh.ball(12, 8, 0.05, 1.25).scaled(1.0, 1.0, 0.8).moved(0.0, 0.0, -0.95));
        parts.add(Mesh.cone(10, 0.036, 0.0, 0.5, 0.74, 1.3).alongZ());
        parts.add(Mesh.box(-0.05, -0.06, 0.16, 0.05, 0.06, 0.5, 1.1));
        parts.add(Mesh.box(-0.056, -0.066, 0.17, 0.056, 0.066, 0.2, 1.5));
        parts.add(Mesh.box(-0.056, -0.066, 0.46, 0.056, 0.066, 0.49, 1.5));
        double[] outline = { 0.44, 0.02, 0.43, 0.13, 0.4, 0.23, 0.35, 0.31, 0.29, 0.21, 0.21, 0.12, 0.12, 0.07, 0.04,
                0.07, 0.04, -0.07, 0.12, -0.07, 0.21, -0.12, 0.29, -0.2, 0.34, -0.29, 0.4, -0.21, 0.43, -0.1 };
        List<Mesh> bit = new ArrayList<>();
        bit.add(plate(0.014, 1.0, outline));
        bit.add(plate(0.026, 1.1, resized(outline, 0.04, 0.0, 0.62)));
        bit.add(Mesh.tube(false, 5, 0.012, 1.9, path(0.0, 0.34, -0.29, 0.0, 0.4, -0.21, 0.0, 0.43, -0.1, 0.0, 0.44, 0.02,
                0.0, 0.43, 0.13, 0.0, 0.4, 0.23, 0.0, 0.35, 0.31)));
        for (double[] at : new double[][] { { 0.1, 0.03 }, { 0.1, -0.03 }, { 0.17, 0.0 } }) {
            twice(bit, Mesh.ball(8, 5, 0.013, 1.6).moved(0.027, at[0], at[1]));
        }
        for (Mesh part : bit) {
            parts.add(part.moved(0.0, 0.0, 0.33));
            parts.add(part.scaled(1.0, -1.0, 1.0).moved(0.0, 0.0, 0.33));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A two-handed war hammer: a long haft with a long wound grip, bands and a pommel; a big eight-sided head with a
     * flared, glowing face at either end, a raised plate in its middle with the lantern emblem on both sides, and a
     * point on top.
     */
    private static Mesh[] warHammer() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(rod(12, 0.036, -0.88, 0.4, 0.95));
        parts.add(Mesh.tube(false, 5, 0.009, 1.3, helix(0.04, -0.82, -0.25, 10.0)));
        for (double z : new double[] { -0.84, -0.25, 0.26 }) {
            parts.add(band(0.045, 0.012, z, 1.5));
        }
        parts.add(Mesh.ball(12, 8, 0.058, 1.25).scaled(1.0, 1.0, 0.8).moved(0.0, 0.0, -0.91));
        parts.add(Mesh.sweep(1.0, OCTAGON, new double[] { -0.42, 0.14, 0.14, 0.0 },
                new double[] { -0.39, 0.16, 0.16, 0.0 }, new double[] { -0.31, 0.16, 0.16, 0.0 },
                new double[] { -0.29, 0.135, 0.135, 0.0 }, new double[] { 0.29, 0.135, 0.135, 0.0 },
                new double[] { 0.31, 0.16, 0.16, 0.0 }, new double[] { 0.39, 0.16, 0.16, 0.0 },
                new double[] { 0.42, 0.14, 0.14, 0.0 }).turned(1.0, 0.0, 0.0, -90.0).moved(0.0, 0.0, 0.5));
        Mesh face = Mesh.sweep(1.6, OCTAGON, new double[] { 0.415, 0.125, 0.125, 0.0 },
                new double[] { 0.44, 0.105, 0.105, 0.0 }).turned(1.0, 0.0, 0.0, -90.0).moved(0.0, 0.0, 0.5);
        parts.add(face);
        parts.add(face.scaled(1.0, -1.0, 1.0));
        parts.add(Mesh.box(-0.147, -0.11, 0.355, 0.147, 0.11, 0.645, 1.15));
        twice(parts, Mesh.torus(20, 5, 0.065, 0.012, 1.9).alongX().moved(0.15, 0.0, 0.5));
        twice(parts, Mesh.box(0.142, -0.075, 0.585, 0.157, 0.075, 0.61, 1.9));
        twice(parts, Mesh.box(0.142, -0.075, 0.39, 0.157, 0.075, 0.415, 1.9));
        parts.add(Mesh.cone(4, 0.08, 0.0, 0.63, 0.78, 1.2).alongZ());
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A halberd: a long pole with a wound grip and a spike at its foot; a socket with bands and langets; a leaf-shaped
     * spear point with a glowing rib; a broad axe blade on one side with a raised cheek, rivets and a glowing edge; and a
     * hooked beak on the other.
     */
    private static Mesh[] halberd() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(rod(12, 0.03, -1.05, 0.62, 0.95));
        parts.add(Mesh.tube(false, 5, 0.008, 1.3, helix(0.033, -0.6, -0.25, 7.0)));
        for (double z : new double[] { -1.05, -0.62, -0.23 }) {
            parts.add(band(0.036, 0.01, z, 1.5));
        }
        parts.add(Mesh.cone(10, 0.0, 0.034, -1.18, -1.05, 1.3).alongZ());
        parts.add(rod(12, 0.042, 0.1, 0.62, 1.05));
        parts.add(band(0.047, 0.011, 0.11, 1.5));
        parts.add(band(0.047, 0.011, 0.61, 1.5));
        twice(parts, Mesh.box(0.03, -0.012, -0.12, 0.046, 0.012, 0.12, 1.25));
        parts.add(Mesh.sweep(1.15, BLADE, new double[] { 0.6, 0.03, 0.04, 0.0 }, new double[] { 0.67, 0.026, 0.085, 0.0 },
                new double[] { 0.8, 0.021, 0.1, 0.0 }, new double[] { 0.95, 0.015, 0.07, 0.0 },
                new double[] { 1.08, 0.008, 0.03, 0.0 }, new double[] { 1.16, 0.0, 0.0, 0.0 }));
        twice(parts, Mesh.tube(false, 4, 0.005, 1.9, path(0.027, 0.0, 0.67, 0.022, 0.0, 0.8, 0.016, 0.0, 0.95, 0.0095,
                0.0, 1.05)));
        double[] blade = { 0.04, -0.145, 0.175, -0.213, 0.256, -0.36, 0.378, -0.307, 0.445, -0.172, 0.472, -0.01, 0.445,
                0.152, 0.391, 0.287, 0.256, 0.26, 0.04, 0.206 };
        List<Mesh> axe = new ArrayList<>();
        axe.add(plate(0.015, 1.0, blade));
        axe.add(plate(0.026, 1.1, resized(blade, 0.04, 0.03, 0.6)));
        axe.add(Mesh.tube(false, 5, 0.013, 1.9, path(0.0, 0.256, -0.36, 0.0, 0.378, -0.307, 0.0, 0.445, -0.172, 0.0,
                0.472, -0.01, 0.0, 0.445, 0.152, 0.0, 0.391, 0.287)));
        for (double[] at : new double[][] { { 0.12, 0.1 }, { 0.12, -0.04 }, { 0.21, 0.03 } }) {
            twice(axe, Mesh.ball(8, 5, 0.013, 1.6).moved(0.027, at[0], at[1]));
        }
        for (Mesh part : axe) {
            parts.add(part.moved(0.0, 0.0, 0.38));
        }
        double[] beak = { 0.03, -0.078, 0.199, -0.039, 0.342, -0.104, 0.251, 0.0, 0.121, 0.065, 0.03, 0.078 };
        parts.add(plate(0.02, 1.05, beak).scaled(1.0, -1.0, 1.0).moved(0.0, 0.0, 0.4));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A heavy chainsaw: the engine housing with cooling fins, a fuel cap and the starter on its side; a loop handle
     * behind with its trigger and a wrap handle over the top in front with the hand guard; and the long guide bar with a
     * glowing groove, bolts, the sprocket at its nose and the chain running all round it, its cutters standing out.
     */
    private static Mesh[] chainsaw() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.sweep(0.95, SQUIRCLE, new double[] { -0.4, 0.1, 0.12, 0.0 },
                new double[] { -0.36, 0.125, 0.15, 0.01 }, new double[] { -0.06, 0.125, 0.15, 0.01 },
                new double[] { 0.03, 0.1, 0.12, 0.0 }));
        for (int k = 0; k < 6; k++) {
            double z = -0.32 + k * 0.04;
            parts.add(Mesh.box(-0.075, 0.15, z, 0.075, 0.2, z + 0.018, 1.3));
        }
        parts.add(Mesh.cylinder(10, 0.03, 0.13, 0.19, 1.3).moved(-0.06, 0.0, -0.37));
        parts.add(Mesh.cylinder(18, 0.09, 0.118, 0.14, 1.1).alongX().moved(0.0, 0.0, -0.22));
        parts.add(Mesh.torus(18, 4, 0.06, 0.008, 1.7).alongX().moved(0.142, 0.0, -0.22));
        parts.add(Mesh.box(0.14, 0.05, -0.27, 0.16, 0.075, -0.17, 1.5));
        parts.add(Mesh.tube(false, 7, 0.028, 1.05, path(0.0, 0.1, -0.37, 0.0, 0.15, -0.46, 0.0, 0.14, -0.6, 0.0, 0.05,
                -0.67, 0.0, -0.06, -0.64, 0.0, -0.11, -0.55, 0.0, -0.11, -0.39)));
        parts.add(Mesh.box(-0.012, 0.075, -0.54, 0.012, 0.125, -0.49, 1.6));
        parts.add(Mesh.tube(false, 7, 0.022, 1.1, path(0.14, -0.13, -0.03, 0.155, 0.06, -0.03, 0.13, 0.23, -0.03, 0.05,
                0.29, -0.03, -0.05, 0.29, -0.03, -0.13, 0.23, -0.03, -0.155, 0.06, -0.03, -0.14, -0.1, -0.03)));
        parts.add(Mesh.box(-0.11, 0.08, 0.035, 0.11, 0.34, 0.055, 1.2));
        // The bar, drawn flat in its own plane and stood on edge along z.
        List<Double> bar = new ArrayList<>(List.of(-0.05, -0.075, 0.85, -0.055));
        for (int t = -60; t <= 60; t += 30) {
            bar.add(0.85 + 0.055 * Math.cos(Math.toRadians(t)));
            bar.add(0.055 * Math.sin(Math.toRadians(t)));
        }
        bar.addAll(List.of(0.85, 0.055, -0.05, 0.075));
        parts.add(Mesh.prism(-0.018, 0.018, 1.0, bar.stream().mapToDouble(Double::doubleValue).toArray())
                .turned(0.0, 1.0, 0.0, -90.0).moved(0.0, -0.02, 0.0));
        parts.add(Mesh.box(-0.02, -0.028, 0.08, 0.02, -0.012, 0.8, 1.8));
        for (double z : new double[] { 0.07, 0.13 }) {
            parts.add(Mesh.cylinder(10, 0.016, -0.024, 0.024, 1.6).alongX().moved(0.0, -0.02, z));
        }
        twice(parts, Mesh.torus(12, 4, 0.03, 0.007, 1.8).alongX().moved(0.019, -0.02, 0.85));
        // The chain: a band all round the bar, and its cutters standing out of it, every other one to the other side.
        List<Vec3> chain = new ArrayList<>(List.of(new Vec3(0.0, -0.103, -0.05), new Vec3(0.0, -0.093, 0.4),
                new Vec3(0.0, -0.083, 0.85)));
        for (int t = -60; t <= 60; t += 20) {
            chain.add(new Vec3(0.0, -0.02 + 0.063 * Math.sin(Math.toRadians(t)),
                    0.85 + 0.063 * Math.cos(Math.toRadians(t))));
        }
        chain.addAll(List.of(new Vec3(0.0, 0.043, 0.85), new Vec3(0.0, 0.053, 0.4), new Vec3(0.0, 0.063, -0.05)));
        parts.add(Mesh.tube(true, 5, 0.011, 1.35, chain.toArray(Vec3[]::new)));
        int tooth = 0;
        for (double z = 0.05; z <= 0.83; z += 0.07) {
            double drop = 0.02 * (z + 0.05) / 0.9;
            parts.add(cutter(tooth++, -0.103 + drop, z, 180.0));
            parts.add(cutter(tooth++, 0.063 - drop, z, 0.0));
        }
        for (int t = -45; t <= 45; t += 45) {
            double angle = Math.toRadians(t);
            parts.add(cutter(tooth++, -0.02 + 0.063 * Math.sin(angle), 0.85 + 0.063 * Math.cos(angle),
                    Math.toDegrees(Math.atan2(Math.cos(angle), Math.sin(angle)))));
        }
        return parts.toArray(Mesh[]::new);
    }

    /** One cutter of the chain at {@code y}, {@code z}, standing out towards {@code degrees} (0 = up, 90 = ahead). */
    private static Mesh cutter(int index, double y, double z, double degrees) {
        return Mesh.box(-0.013, 0.0, -0.018, 0.013, 0.026, 0.018, 1.5).turned(1.0, 0.0, 0.0, degrees)
                .moved(index % 2 == 0 ? 0.006 : -0.006, y, z);
    }

    // ---- The guns ----

    /**
     * A heavy revolver, a hand cannon: a long barrel with a vented rib, a full underlug, a front sight and a glowing
     * muzzle; a fluted cylinder with its chambers; the frame round it with the hammer; a trigger in its guard; and a
     * grip with the lantern emblem on both sides.
     */
    private static Mesh[] revolver() {
        List<Mesh> parts = new ArrayList<>();
        double bore = 0.06;
        double axis = 0.01;
        parts.add(rod(14, 0.03, 0.09, 0.5, 1.0).moved(0.0, bore, 0.0));
        parts.add(Mesh.box(-0.018, 0.08, 0.09, 0.018, 0.1, 0.5, 1.15));
        for (int k = 0; k < 4; k++) {
            parts.add(Mesh.box(-0.01, 0.1, 0.14 + k * 0.08, 0.01, 0.104, 0.18 + k * 0.08, 1.9));
        }
        parts.add(Mesh.box(-0.026, -0.005, 0.09, 0.026, bore, 0.5, 1.0));
        parts.add(Mesh.box(-0.007, 0.1, 0.44, 0.007, 0.13, 0.49, 1.5));
        parts.add(Mesh.torus(14, 5, 0.021, 0.008, 1.9).alongZ().moved(0.0, bore, 0.5));
        parts.add(Mesh.lathe(20, 1.0, 0.0, -0.075, 0.062, -0.075, 0.07, -0.065, 0.07, 0.065, 0.062, 0.075, 0.0, 0.075)
                .alongZ().moved(0.0, axis, 0.0));
        for (int k = 0; k < 6; k++) {
            parts.add(Mesh.box(-0.008, 0.066, -0.05, 0.008, 0.074, 0.05, 1.9).turned(0.0, 0.0, 1.0, 30.0 + 60.0 * k)
                    .moved(0.0, axis, 0.0));
        }
        for (int k = 1; k < 6; k++) {
            double angle = Math.toRadians(90.0 + 60.0 * k);
            parts.add(Mesh.torus(10, 4, 0.016, 0.005, 1.7).alongZ().moved(Math.cos(angle) * 0.045,
                    axis + Math.sin(angle) * 0.045, 0.076));
        }
        parts.add(Mesh.box(-0.03, 0.08, -0.1, 0.03, 0.1, 0.1, 1.0));
        parts.add(Mesh.box(-0.035, -0.075, -0.16, 0.035, 0.1, -0.075, 1.0));
        parts.add(Mesh.box(-0.028, -0.075, -0.16, 0.028, -0.055, 0.1, 1.0));
        parts.add(Mesh.box(-0.03, -0.075, 0.075, 0.03, -0.005, 0.1, 1.0));
        parts.add(Mesh.box(0.035, 0.02, -0.14, 0.042, 0.04, -0.1, 1.5));
        parts.add(Mesh.box(-0.011, 0.0, -0.016, 0.011, 0.075, 0.016, 1.3).turned(1.0, 0.0, 0.0, -35.0)
                .moved(0.0, 0.07, -0.15));
        parts.add(Mesh.tube(false, 6, 0.011, 1.2, path(0.0, -0.075, 0.055, 0.0, -0.125, 0.045, 0.0, -0.148, 0.005, 0.0,
                -0.14, -0.045, 0.0, -0.105, -0.075, 0.0, -0.075, -0.08)));
        parts.add(Mesh.tube(false, 5, 0.009, 1.5, path(0.0, -0.075, -0.005, 0.0, -0.1, 0.0, 0.0, -0.125, -0.015)));
        parts.add(leaning(grip(0.24, 0.05, 0.034, 0.95), 115.0, -0.07, -0.13));
        twice(parts, Mesh.torus(12, 4, 0.02, 0.006, 1.9).alongX().moved(0.038, -0.179, -0.181));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A sawed-off double-barrelled shotgun: two short, fat barrels side by side with ribs, a bead and glowing muzzles; a
     * fore-end under them; the break action with its hinge pin, raised side plates, top lever and two hammers; two
     * triggers in their guard; and the stock sawn off behind its wrist, flaring out to the cut, which glows.
     */
    private static Mesh[] shotgun() {
        List<Mesh> parts = new ArrayList<>();
        for (int side = -1; side <= 1; side += 2) {
            parts.add(rod(16, 0.038, 0.0, 0.5, 1.0).moved(side * 0.039, 0.05, 0.0));
            parts.add(Mesh.torus(16, 5, 0.026, 0.009, 1.9).alongZ().moved(side * 0.039, 0.05, 0.5));
            parts.add(Mesh.box(-0.01, 0.0, -0.012, 0.01, 0.06, 0.012, 1.3).turned(1.0, 0.0, 0.0, -40.0)
                    .moved(side * 0.04, 0.075, -0.135));
        }
        parts.add(Mesh.box(-0.012, 0.08, 0.0, 0.012, 0.097, 0.5, 1.15));
        parts.add(Mesh.box(-0.012, 0.0, 0.0, 0.012, 0.03, 0.5, 1.0));
        parts.add(Mesh.ball(8, 5, 0.011, 2.0).moved(0.0, 0.1, 0.48));
        parts.add(Mesh.sweep(0.95, SQUIRCLE, new double[] { 0.02, 0.07, 0.032, 0.0 },
                new double[] { 0.26, 0.064, 0.03, 0.0 }));
        twice(parts, Mesh.box(0.062, -0.006, 0.06, 0.071, 0.006, 0.22, 1.8));
        parts.add(Mesh.box(-0.074, -0.035, -0.15, 0.074, 0.09, 0.005, 1.0));
        twice(parts, Mesh.box(0.07, -0.02, -0.135, 0.08, 0.075, -0.015, 1.25));
        parts.add(Mesh.cylinder(12, 0.02, -0.082, 0.082, 1.6).alongX().moved(0.0, -0.01, -0.005));
        parts.add(Mesh.box(-0.01, 0.09, -0.13, 0.035, 0.105, -0.07, 1.4));
        parts.add(Mesh.tube(false, 6, 0.011, 1.2, path(0.0, -0.035, 0.0, 0.0, -0.085, -0.01, 0.0, -0.105, -0.05, 0.0,
                -0.095, -0.1, 0.0, -0.06, -0.13, 0.0, -0.035, -0.13)));
        for (double z : new double[] { -0.035, -0.075 }) {
            parts.add(Mesh.tube(false, 5, 0.008, 1.5,
                    path(0.0, -0.035, z, 0.0, -0.06, z + 0.005, 0.0, -0.08, z - 0.005)));
        }
        parts.add(leaning(Mesh.sweep(0.95, SQUIRCLE, new double[] { 0.0, 0.046, 0.06, 0.0 },
                new double[] { 0.13, 0.047, 0.07, 0.0 }, new double[] { 0.27, 0.052, 0.1, 0.0 },
                new double[] { 0.3, 0.054, 0.105, 0.0 }), 150.0, 0.0, -0.14));
        parts.add(leaning(Mesh.sweep(1.9, SQUIRCLE, new double[] { 0.3, 0.05, 0.1, 0.0 },
                new double[] { 0.312, 0.046, 0.093, 0.0 }), 150.0, 0.0, -0.14));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A micro submachine gun: a boxy receiver with a toothed rail, sights, a charging knob, a glowing ejection port and
     * the lantern emblem; a short threaded barrel; a trigger in its guard; and the magazine running through the grip,
     * with a glowing window and base plate.
     */
    private static Mesh[] smg() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.box(-0.045, -0.02, -0.2, 0.045, 0.1, 0.14, 1.0));
        parts.add(Mesh.box(-0.02, 0.1, -0.17, 0.02, 0.112, 0.1, 1.2));
        for (int k = 0; k < 8; k++) {
            double z = -0.16 + k * 0.034;
            parts.add(Mesh.box(-0.026, 0.112, z, 0.026, 0.122, z + 0.012, 1.45));
        }
        parts.add(Mesh.box(-0.03, 0.1, -0.2, 0.03, 0.145, -0.185, 1.3));
        parts.add(Mesh.box(-0.008, 0.1, 0.11, 0.008, 0.14, 0.13, 1.5));
        parts.add(Mesh.cylinder(10, 0.013, 0.045, 0.075, 1.5).alongX().moved(0.0, 0.06, 0.02));
        parts.add(Mesh.box(0.044, 0.035, -0.02, 0.049, 0.075, 0.07, 1.9));
        parts.add(Mesh.torus(12, 4, 0.022, 0.006, 1.9).alongX().moved(0.046, 0.04, -0.12));
        parts.add(Mesh.box(0.044, 0.068, -0.15, 0.049, 0.075, -0.09, 1.9));
        parts.add(Mesh.box(0.044, 0.005, -0.15, 0.049, 0.012, -0.09, 1.9));
        parts.add(rod(12, 0.03, 0.14, 0.17, 1.2).moved(0.0, 0.055, 0.0));
        parts.add(rod(10, 0.019, 0.17, 0.3, 1.0).moved(0.0, 0.055, 0.0));
        for (double z : new double[] { 0.2, 0.225, 0.25, 0.275 }) {
            parts.add(Mesh.torus(10, 4, 0.02, 0.004, 1.4).alongZ().moved(0.0, 0.055, z));
        }
        parts.add(Mesh.torus(12, 4, 0.013, 0.005, 1.9).alongZ().moved(0.0, 0.055, 0.3));
        parts.add(leaning(grip(0.19, 0.046, 0.036, 0.95), 104.0, -0.02, -0.02));
        parts.add(leaning(Mesh.sweep(1.0, SQUIRCLE, new double[] { 0.19, 0.028, 0.036, 0.0 },
                new double[] { 0.36, 0.028, 0.036, 0.0 }), 104.0, -0.02, -0.02));
        parts.add(leaning(Mesh.sweep(1.7, SQUIRCLE, new double[] { 0.36, 0.034, 0.043, 0.0 },
                new double[] { 0.385, 0.034, 0.043, 0.0 }), 104.0, -0.02, -0.02));
        twice(parts, leaning(Mesh.box(0.026, -0.01, 0.22, 0.031, 0.01, 0.33, 1.9), 104.0, -0.02, -0.02));
        parts.add(Mesh.tube(false, 6, 0.01, 1.2, path(0.0, -0.02, 0.11, 0.0, -0.075, 0.105, 0.0, -0.1, 0.075, 0.0, -0.1,
                0.04, 0.0, -0.075, 0.02)));
        parts.add(Mesh.tube(false, 5, 0.008, 1.5, path(0.0, -0.02, 0.06, 0.0, -0.045, 0.065, 0.0, -0.065, 0.055)));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * An arm cannon, a mega blaster: a big round barrel you put your forearm in, with a rim round its opening, bands,
     * vents on top, the lantern emblem on both sides and a power cell underneath; its muzzle hollow, with a core of light
     * burning deep inside.
     */
    private static Mesh[] armCannon() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(24, 1.0, 0.0, -0.42, 0.16, -0.42, 0.2, -0.395, 0.215, -0.34, 0.215, -0.08, 0.2, -0.04, 0.14,
                0.0, 0.12, 0.02, 0.12, 0.26, 0.14, 0.28, 0.14, 0.33, 0.12, 0.345, 0.08, 0.345, 0.08, 0.24, 0.0, 0.24)
                .alongZ());
        parts.add(band(0.17, 0.018, -0.42, 1.4));
        parts.add(band(0.215, 0.014, -0.34, 1.5));
        parts.add(band(0.215, 0.014, -0.12, 1.5));
        parts.add(band(0.12, 0.01, 0.09, 1.6));
        parts.add(band(0.12, 0.01, 0.18, 1.6));
        parts.add(band(0.08, 0.008, 0.34, 2.0));
        parts.add(Mesh.ball(14, 8, 0.06, 2.3).moved(0.0, 0.0, 0.24));
        for (int k = 0; k < 4; k++) {
            double z = -0.3 + k * 0.055;
            parts.add(Mesh.box(-0.06, 0.2, z, 0.06, 0.232, z + 0.03, 1.35));
        }
        twice(parts, Mesh.torus(20, 5, 0.06, 0.012, 1.9).alongX().moved(0.216, 0.0, -0.21));
        twice(parts, Mesh.box(0.185, 0.08, -0.29, 0.22, 0.098, -0.13, 1.9));
        twice(parts, Mesh.box(0.185, -0.098, -0.29, 0.22, -0.08, -0.13, 1.9));
        parts.add(rod(12, 0.045, -0.36, -0.06, 1.1).moved(0.0, -0.22, 0.0));
        for (double z : new double[] { -0.32, -0.21, -0.1 }) {
            parts.add(Mesh.torus(12, 4, 0.047, 0.008, 1.8).alongZ().moved(0.0, -0.22, z));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A rotary grenade launcher: a revolving drum of six chambers with a band round it; a short fat barrel with a
     * glowing muzzle, bands, a toothed rail and a ladder sight on top; the frame in front of and behind the drum, a rail
     * underneath with a foregrip; a pistol grip with its trigger; and a skeleton stock.
     */
    private static Mesh[] grenadeLauncher() {
        List<Mesh> parts = new ArrayList<>();
        double drum = -0.02;
        double bore = drum + 0.09;
        for (int k = 0; k < 6; k++) {
            double angle = Math.toRadians(90.0 + 60.0 * k);
            double x = Math.cos(angle) * 0.09;
            double y = drum + Math.sin(angle) * 0.09;
            parts.add(rod(14, 0.05, -0.13, 0.13, 1.0).moved(x, y, 0.0));
            if (k > 0) {
                parts.add(Mesh.torus(12, 4, 0.034, 0.007, 1.8).alongZ().moved(x, y, 0.131));
            }
        }
        parts.add(rod(12, 0.05, -0.14, 0.14, 1.1).moved(0.0, drum, 0.0));
        parts.add(rod(24, 0.152, -0.155, -0.13, 1.05).moved(0.0, drum, 0.0));
        parts.add(Mesh.torus(24, 5, 0.142, 0.008, 1.7).alongZ().moved(0.0, drum, 0.0));
        parts.add(rod(16, 0.05, 0.13, 0.5, 1.0).moved(0.0, bore, 0.0));
        parts.add(Mesh.torus(16, 5, 0.04, 0.01, 1.9).alongZ().moved(0.0, bore, 0.5));
        for (double z : new double[] { 0.3, 0.45 }) {
            parts.add(Mesh.torus(16, 4, 0.052, 0.008, 1.6).alongZ().moved(0.0, bore, z));
        }
        parts.add(Mesh.box(-0.024, bore + 0.045, 0.0, 0.024, bore + 0.062, 0.46, 1.15));
        for (int k = 0; k < 11; k++) {
            double z = 0.02 + k * 0.04;
            parts.add(Mesh.box(-0.03, bore + 0.062, z, 0.03, bore + 0.072, z + 0.015, 1.45));
        }
        twice(parts, Mesh.box(0.017, bore + 0.07, 0.04, 0.025, bore + 0.13, 0.05, 1.3));
        parts.add(Mesh.box(-0.025, bore + 0.12, 0.04, 0.025, bore + 0.13, 0.05, 1.6));
        parts.add(Mesh.box(-0.05, drum - 0.17, 0.13, 0.05, bore + 0.045, 0.2, 1.05));
        parts.add(Mesh.box(-0.03, drum - 0.19, 0.13, 0.03, drum - 0.15, 0.44, 1.05));
        parts.add(Mesh.cylinder(12, 0.028, drum - 0.38, drum - 0.19, 1.0).moved(0.0, 0.0, 0.34));
        for (double y : new double[] { drum - 0.25, drum - 0.3, drum - 0.35 }) {
            parts.add(Mesh.torus(12, 4, 0.03, 0.006, 1.4).moved(0.0, y, 0.34));
        }
        parts.add(Mesh.box(-0.05, drum - 0.13, -0.27, 0.05, bore + 0.045, -0.155, 1.0));
        parts.add(rod(10, 0.014, -0.155, 0.13, 1.3).moved(0.0, drum - 0.16, 0.0));
        parts.add(leaning(grip(0.22, 0.048, 0.034, 0.95), 108.0, drum - 0.13, -0.25));
        parts.add(Mesh.tube(false, 6, 0.01, 1.2, path(0.0, drum - 0.13, -0.16, 0.0, drum - 0.2, -0.17, 0.0, drum - 0.215,
                -0.205, 0.0, drum - 0.2, -0.23)));
        parts.add(Mesh.tube(false, 5, 0.008, 1.5, path(0.0, drum - 0.13, -0.195, 0.0, drum - 0.16, -0.19, 0.0,
                drum - 0.185, -0.2)));
        parts.add(Mesh.tube(false, 6, 0.016, 1.1, path(0.0, bore, -0.27, 0.0, bore - 0.01, -0.54)));
        parts.add(Mesh.tube(false, 6, 0.016, 1.1, path(0.0, drum - 0.1, -0.27, 0.0, drum - 0.17, -0.54)));
        parts.add(Mesh.box(-0.035, drum - 0.21, -0.575, 0.035, bore + 0.03, -0.535, 1.2));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A minigun: six barrels round a spindle with glowing muzzles, held by two clamps; the rotor housing with bands; the
     * housing behind it with the motor on its side, a carry handle over the top and two spade grips with a bar between
     * them; and the ammunition chute curving away underneath.
     */
    private static Mesh[] minigun() {
        List<Mesh> parts = new ArrayList<>();
        for (int k = 0; k < 6; k++) {
            double angle = Math.toRadians(90.0 + 60.0 * k);
            double x = Math.cos(angle) * 0.062;
            double y = Math.sin(angle) * 0.062;
            parts.add(rod(10, 0.02, -0.05, 0.75, 1.0).moved(x, y, 0.0));
            parts.add(Mesh.torus(10, 4, 0.013, 0.005, 1.9).alongZ().moved(x, y, 0.75));
        }
        parts.add(rod(10, 0.022, -0.05, 0.72, 1.1));
        parts.add(rod(24, 0.095, 0.33, 0.37, 1.1));
        parts.add(rod(24, 0.092, 0.68, 0.72, 1.15));
        parts.add(band(0.093, 0.007, 0.7, 1.8));
        parts.add(rod(24, 0.105, -0.32, -0.02, 1.0));
        parts.add(band(0.107, 0.01, -0.3, 1.5));
        parts.add(band(0.107, 0.01, -0.04, 1.5));
        parts.add(Mesh.box(-0.085, -0.085, -0.47, 0.085, 0.09, -0.31, 1.0));
        parts.add(rod(12, 0.045, -0.46, -0.3, 1.1).moved(0.11, -0.03, 0.0));
        parts.add(Mesh.torus(12, 4, 0.047, 0.008, 1.7).alongZ().moved(0.11, -0.03, -0.31));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.cylinder(10, 0.022, -0.12, 0.07, 1.0).moved(side * 0.1, 0.0, -0.57));
            parts.add(Mesh.tube(false, 6, 0.014, 1.1, path(side * 0.1, 0.07, -0.57, side * 0.075, 0.07, -0.46)));
            parts.add(Mesh.tube(false, 6, 0.014, 1.1, path(side * 0.1, -0.12, -0.57, side * 0.075, -0.08, -0.46)));
        }
        parts.add(Mesh.tube(false, 6, 0.012, 1.2, path(-0.1, 0.06, -0.57, 0.1, 0.06, -0.57)));
        parts.add(Mesh.box(-0.025, 0.07, -0.585, 0.025, 0.09, -0.555, 1.9));
        parts.add(Mesh.tube(false, 7, 0.016, 1.15, path(0.0, 0.09, -0.42, 0.0, 0.19, -0.38, 0.0, 0.205, -0.26, 0.0, 0.19,
                -0.13, 0.0, 0.1, -0.09)));
        Vec3[] chute = path(0.09, -0.02, -0.2, 0.16, -0.08, -0.2, 0.19, -0.18, -0.23, 0.19, -0.28, -0.29, 0.17, -0.36,
                -0.37);
        parts.add(Mesh.tube(false, 8, 0.03, 0.95, chute));
        for (int i = 1; i < chute.length; i++) {
            Vec3 along = chute[Math.min(chute.length - 1, i + 1)].subtract(chute[i - 1]);
            parts.add(Mesh.torus(12, 4, 0.032, 0.006, 1.6).pointing(along.x, along.y, along.z)
                    .moved(chute[i].x, chute[i].y, chute[i].z));
        }
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A rocket launcher, an RPG: a long tube with a heat shield and a flared, hollow venturi behind; the warhead on its
     * front with a glowing band and fuze; an optic sight on its side and iron sights on top; a pistol grip with its
     * trigger and a front grip.
     */
    private static Mesh[] rocketLauncher() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(rod(14, 0.042, -0.5, 0.27, 1.0));
        parts.add(Mesh.lathe(16, 1.05, 0.0, -0.58, 0.035, -0.58, 0.07, -0.69, 0.085, -0.7, 0.09, -0.68, 0.048, -0.52,
                0.048, -0.5, 0.0, -0.5).alongZ());
        parts.add(band(0.05, 0.01, -0.5, 1.6));
        parts.add(rod(16, 0.056, -0.22, 0.08, 0.95));
        for (double z : new double[] { -0.22, -0.07, 0.08 }) {
            parts.add(band(0.058, 0.009, z, 1.6));
        }
        parts.add(Mesh.lathe(18, 1.05, 0.0, 0.26, 0.03, 0.26, 0.03, 0.36, 0.085, 0.46, 0.088, 0.49, 0.085, 0.52, 0.04,
                0.68, 0.018, 0.72, 0.012, 0.8, 0.0, 0.81).alongZ());
        parts.add(band(0.088, 0.008, 0.49, 1.8));
        parts.add(Mesh.ball(8, 5, 0.014, 2.0).moved(0.0, 0.0, 0.805));
        parts.add(Mesh.box(0.05, 0.03, -0.13, 0.095, 0.09, -0.01, 1.0));
        parts.add(Mesh.box(0.035, 0.0, -0.1, 0.055, 0.04, -0.04, 1.1));
        parts.add(rod(12, 0.026, -0.17, -0.13, 1.1).moved(0.0725, 0.06, 0.0));
        parts.add(rod(12, 0.026, -0.01, 0.03, 1.1).moved(0.0725, 0.06, 0.0));
        parts.add(Mesh.torus(12, 4, 0.018, 0.005, 1.9).alongZ().moved(0.0725, 0.06, 0.031));
        parts.add(Mesh.box(-0.008, 0.04, 0.2, 0.008, 0.085, 0.215, 1.4));
        parts.add(Mesh.box(-0.018, 0.05, -0.04, 0.018, 0.08, -0.025, 1.4));
        parts.add(leaning(grip(0.2, 0.046, 0.033, 0.95), 105.0, -0.05, -0.13));
        parts.add(Mesh.tube(false, 6, 0.01, 1.2, path(0.0, -0.05, 0.0, 0.0, -0.1, -0.005, 0.0, -0.125, -0.04, 0.0, -0.11,
                -0.075, 0.0, -0.08, -0.09)));
        parts.add(Mesh.tube(false, 5, 0.008, 1.5, path(0.0, -0.05, -0.035, 0.0, -0.075, -0.03, 0.0, -0.095, -0.042)));
        parts.add(leaning(grip(0.17, 0.04, 0.03, 0.95), 95.0, -0.035, 0.14));
        return parts.toArray(Mesh[]::new);
    }

    /**
     * A plasma flamethrower: a body with glowing vents and a plasma cell on top; a barrel with cooling fins ending in a
     * flared, hollow nozzle with plasma burning in it and an igniter under it; the fuel tank slung underneath with a
     * glowing window and a ribbed hose looping up from it along the side into the body; and a pistol grip with its
     * trigger.
     */
    private static Mesh[] flamethrower() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.sweep(1.0, SQUIRCLE, new double[] { -0.3, 0.048, 0.062, 0.0 },
                new double[] { -0.28, 0.055, 0.075, 0.0 }, new double[] { 0.02, 0.055, 0.075, 0.0 },
                new double[] { 0.06, 0.045, 0.06, 0.01 }));
        for (int k = 0; k < 4; k++) {
            double z = -0.24 + k * 0.06;
            twice(parts, Mesh.box(0.052, 0.0, z, 0.059, 0.035, z + 0.04, 1.9));
        }
        parts.add(rod(14, 0.03, -0.24, 0.0, 1.2).moved(0.0, 0.1, 0.0));
        for (double z : new double[] { -0.2, -0.12, -0.04 }) {
            parts.add(Mesh.torus(12, 4, 0.032, 0.006, 1.9).alongZ().moved(0.0, 0.1, z));
        }
        parts.add(Mesh.box(-0.015, 0.06, -0.2, 0.015, 0.085, -0.17, 1.1));
        parts.add(Mesh.box(-0.015, 0.06, -0.07, 0.015, 0.085, -0.04, 1.1));
        parts.add(rod(12, 0.03, 0.05, 0.5, 1.0).moved(0.0, 0.02, 0.0));
        for (int k = 0; k < 7; k++) {
            double z = 0.1 + k * 0.045;
            parts.add(rod(20, 0.052, z, z + 0.012, 1.1).moved(0.0, 0.02, 0.0));
        }
        parts.add(Mesh.lathe(18, 1.1, 0.0, 0.46, 0.035, 0.46, 0.05, 0.52, 0.062, 0.6, 0.065, 0.62, 0.045, 0.62, 0.04,
                0.58, 0.0, 0.58).alongZ().moved(0.0, 0.02, 0.0));
        parts.add(Mesh.torus(18, 4, 0.05, 0.007, 1.8).alongZ().moved(0.0, 0.02, 0.52));
        parts.add(Mesh.ball(12, 8, 0.034, 2.3).moved(0.0, 0.02, 0.585));
        parts.add(rod(8, 0.011, 0.38, 0.6, 1.2).moved(0.0, -0.035, 0.0));
        parts.add(Mesh.ball(8, 5, 0.016, 2.1).moved(0.0, -0.035, 0.605));
        parts.add(Mesh.box(-0.018, -0.06, 0.02, 0.018, -0.005, 0.2, 1.05));
        parts.add(rod(18, 0.068, -0.06, 0.26, 1.0).moved(0.0, -0.115, 0.0));
        for (double z : new double[] { -0.06, 0.26 }) {
            parts.add(Mesh.ball(18, 10, 0.068, 1.0).scaled(1.0, 1.0, 0.7).moved(0.0, -0.115, z));
        }
        for (double z : new double[] { -0.02, 0.22 }) {
            parts.add(Mesh.torus(18, 5, 0.07, 0.009, 1.6).alongZ().moved(0.0, -0.115, z));
        }
        twice(parts, Mesh.box(0.062, -0.13, 0.02, 0.072, -0.1, 0.18, 2.0));
        Vec3[] hose = path(0.04, -0.1, -0.08, 0.1, -0.1, -0.14, 0.125, -0.02, -0.22, 0.1, 0.06, -0.27, 0.04, 0.08,
                -0.28);
        parts.add(Mesh.tube(false, 7, 0.018, 1.15, hose));
        for (int i = 1; i < hose.length - 1; i++) {
            Vec3 along = hose[i + 1].subtract(hose[i - 1]);
            parts.add(Mesh.torus(10, 4, 0.02, 0.005, 1.7).pointing(along.x, along.y, along.z)
                    .moved(hose[i].x, hose[i].y, hose[i].z));
        }
        parts.add(leaning(grip(0.21, 0.046, 0.034, 0.95), 108.0, -0.055, -0.2));
        parts.add(Mesh.tube(false, 6, 0.01, 1.2, path(0.0, -0.06, -0.1, 0.0, -0.11, -0.105, 0.0, -0.135, -0.14, 0.0,
                -0.12, -0.175)));
        parts.add(Mesh.tube(false, 5, 0.008, 1.5, path(0.0, -0.06, -0.135, 0.0, -0.085, -0.13, 0.0, -0.105, -0.142)));
        return parts.toArray(Mesh[]::new);
    }

    // ---- Parts ----

    /** A rod along z from {@code from} to {@code to}. */
    private static Mesh rod(int sides, double radius, double from, double to, double bright) {
        return Mesh.cylinder(sides, radius, from, to, bright).alongZ();
    }

    /** A thin ring round the z axis at {@code z}: a band round a haft, a barrel or a grip. */
    private static Mesh band(double major, double minor, double z, double bright) {
        return Mesh.torus(16, 5, major, minor, bright).alongZ().moved(0.0, 0.0, z);
    }

    /** A grip along z from 0 to {@code length}: {@code depth} from its middle to front and back, {@code thick} across. */
    private static Mesh grip(double length, double depth, double thick, double bright) {
        return Mesh.sweep(bright, SQUIRCLE, new double[] { 0.0, thick, depth, 0.0 },
                new double[] { length * 0.5, thick * 1.08, depth * 1.08, 0.0 },
                new double[] { length, thick, depth * 0.96, 0.0 });
    }

    /**
     * A part built along z, leant over about x by {@code degrees} (so +z comes to point down and back for a grip,
     * between 90 and 180) and moved to where it hangs from, at {@code y}, {@code z}.
     */
    private static Mesh leaning(Mesh part, double degrees, double y, double z) {
        return part.turned(1.0, 0.0, 0.0, degrees).moved(0.0, y, z);
    }

    /**
     * A flat plate {@code thick} thick either way across x: a blade, a bit, a beak. Its outline gives pairs of how far
     * out along y and where along z, counter-clockwise as seen from +x, and must be seen whole from its own middle.
     */
    private static Mesh plate(double thick, double bright, double... outline) {
        // Drawn as a prism flat in x and y, then turned so its x lies along y, its y along z and its depth along x.
        return Mesh.prism(-thick, thick, bright, outline).turned(1.0, 1.0, 1.0, 120.0);
    }

    /** An outline drawn in towards (or out from) the point ({@code x}, {@code y}), to {@code share} of its size. */
    private static double[] resized(double[] outline, double x, double y, double share) {
        double[] resized = new double[outline.length];
        for (int i = 0; i < outline.length; i += 2) {
            resized[i] = x + (outline[i] - x) * share;
            resized[i + 1] = y + (outline[i + 1] - y) * share;
        }
        return resized;
    }

    /** A cord wound round the z axis, {@code radius} out, from {@code from} to {@code to}. */
    private static Vec3[] helix(double radius, double from, double to, double turns) {
        int count = (int) Math.ceil(turns * 10.0) + 1;
        Vec3[] points = new Vec3[count];
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            double angle = Math.PI * 2.0 * turns * t;
            points[i] = new Vec3(Math.cos(angle) * radius, Math.sin(angle) * radius, from + (to - from) * t);
        }
        return points;
    }

    /** Points given as x, y, z after each other. */
    private static Vec3[] path(double... xyz) {
        Vec3[] points = new Vec3[xyz.length / 3];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vec3(xyz[3 * i], xyz[3 * i + 1], xyz[3 * i + 2]);
        }
        return points;
    }

    /** Adds a part and its mirror image on the other side (across x). */
    private static void twice(List<Mesh> parts, Mesh part) {
        parts.add(part);
        parts.add(part.mirrored());
    }

    private static ConstructPainter.Shape mirrored(ConstructPainter.Shape shape) {
        Mesh[] meshes = new Mesh[shape.meshes().length];
        for (int i = 0; i < meshes.length; i++) {
            meshes[i] = shape.meshes()[i].mirrored();
        }
        return ConstructPainter.Shape.of(meshes);
    }

    /** A square with rounded corners, {@code points} round, one across either way. */
    private static double[] squircle(int points) {
        double[] outline = new double[points * 2];
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            outline[2 * i] = Math.signum(cos) * Math.pow(Math.abs(cos), 0.6);
            outline[2 * i + 1] = Math.signum(sin) * Math.pow(Math.abs(sin), 0.6);
        }
        return outline;
    }
}
