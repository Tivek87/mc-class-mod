package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.WeaponShapes.SQUIRCLE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.WeaponShapes.band;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.WeaponShapes.grip;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.WeaponShapes.leaning;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.WeaponShapes.path;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.WeaponShapes.rod;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.WeaponShapes.twice;

/**
 * The guns among the weapons on the construct wheel (see {@link WeaponShapes}, which also has the parts they are
 * built of), measured the same way.
 */
final class GunShapes {
    private GunShapes() {
    }

    /**
     * A heavy revolver, a hand cannon: a long barrel with a vented rib, a full underlug, a front sight and a glowing
     * muzzle; a fluted cylinder with its chambers; the frame round it with the hammer; a trigger in its guard; and a
     * grip with the lantern emblem on both sides.
     */
    static Mesh[] revolver() {
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
    static Mesh[] shotgun() {
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
    static Mesh[] smg() {
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
    static Mesh[] armCannon() {
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
    static Mesh[] grenadeLauncher() {
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
    static Mesh[] minigun() {
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
    static Mesh[] rocketLauncher() {
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
    static Mesh[] flamethrower() {
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
}
