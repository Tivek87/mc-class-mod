package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructIcons;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.GunShapes.armCannon;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.GunShapes.flamethrower;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.GunShapes.grenadeLauncher;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.GunShapes.minigun;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.GunShapes.revolver;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.GunShapes.rocketLauncher;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.GunShapes.shotgun;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.GunShapes.smg;

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
    static final double[] SQUIRCLE = squircle(16);
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


    // ---- Parts ----

    /** A rod along z from {@code from} to {@code to}. */
    static Mesh rod(int sides, double radius, double from, double to, double bright) {
        return Mesh.cylinder(sides, radius, from, to, bright).alongZ();
    }

    /** A thin ring round the z axis at {@code z}: a band round a haft, a barrel or a grip. */
    static Mesh band(double major, double minor, double z, double bright) {
        return Mesh.torus(16, 5, major, minor, bright).alongZ().moved(0.0, 0.0, z);
    }

    /** A grip along z from 0 to {@code length}: {@code depth} from its middle to front and back, {@code thick} across. */
    static Mesh grip(double length, double depth, double thick, double bright) {
        return Mesh.sweep(bright, SQUIRCLE, new double[] { 0.0, thick, depth, 0.0 },
                new double[] { length * 0.5, thick * 1.08, depth * 1.08, 0.0 },
                new double[] { length, thick, depth * 0.96, 0.0 });
    }

    /**
     * A part built along z, leant over about x by {@code degrees} (so +z comes to point down and back for a grip,
     * between 90 and 180) and moved to where it hangs from, at {@code y}, {@code z}.
     */
    static Mesh leaning(Mesh part, double degrees, double y, double z) {
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
    static Vec3[] path(double... xyz) {
        Vec3[] points = new Vec3[xyz.length / 3];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vec3(xyz[3 * i], xyz[3 * i + 1], xyz[3 * i + 2]);
        }
        return points;
    }

    /** Adds a part and its mirror image on the other side (across x). */
    static void twice(List<Mesh> parts, Mesh part) {
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
