package nl.tivek.multiversepowers.character.greenlantern.client.render.weapon;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.SQUIRCLE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.band;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.grip;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.leaning;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.path;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.rod;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes.twice;

public final class GunShapes {
    private GunShapes() {
    }

    static Mesh[] shotgun() {
        List<Mesh> parts = new ArrayList<>(List.of(shotgunBarrels()));
        parts.addAll(List.of(shotgunStock()));
        return parts.toArray(Mesh[]::new);
    }

    // The barrels and fore-end, which break open about the hinge pin at (0, -0.01, -0.005).
    static Mesh[] shotgunBarrels() {
        List<Mesh> parts = new ArrayList<>();
        for (int side = -1; side <= 1; side += 2) {
            parts.add(rod(16, 0.038, 0.0, 0.5, 1.0).moved(side * 0.039, 0.05, 0.0));
            parts.add(Mesh.torus(16, 5, 0.026, 0.009, 1.9).alongZ().moved(side * 0.039, 0.05, 0.5));
            parts.add(Mesh.torus(16, 4, 0.039, 0.006, 1.6).alongZ().moved(side * 0.039, 0.05, 0.03));
        }
        parts.add(Mesh.box(-0.012, 0.08, 0.0, 0.012, 0.097, 0.5, 1.15));
        parts.add(Mesh.box(-0.012, 0.0, 0.0, 0.012, 0.03, 0.5, 1.0));
        parts.add(Mesh.ball(8, 5, 0.011, 2.0).moved(0.0, 0.1, 0.48));
        parts.add(Mesh.sweep(0.95, SQUIRCLE, new double[] { 0.02, 0.07, 0.032, 0.0 },
                new double[] { 0.26, 0.064, 0.03, 0.0 }));
        twice(parts, Mesh.box(0.062, -0.006, 0.06, 0.071, 0.006, 0.22, 1.8));
        return parts.toArray(Mesh[]::new);
    }

    static Mesh[] shotgunStock() {
        List<Mesh> parts = new ArrayList<>();
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.box(-0.01, 0.0, -0.012, 0.01, 0.06, 0.012, 1.3).turned(1.0, 0.0, 0.0, -40.0)
                    .moved(side * 0.04, 0.075, -0.135));
        }
        parts.add(Mesh.box(-0.074, -0.035, -0.15, 0.074, 0.09, 0.005, 1.0));
        twice(parts, Mesh.box(0.07, -0.02, -0.135, 0.08, 0.075, -0.015, 1.25));
        twice(parts, Mesh.tube(false, 4, 0.004, 1.9, path(0.081, 0.0, -0.12, 0.081, 0.03, -0.095, 0.081, 0.052,
                -0.065, 0.081, 0.038, -0.035, 0.081, 0.012, -0.03)));
        for (double[] at : new double[][] { { 0.058, -0.122 }, { -0.006, -0.03 } }) {
            twice(parts, Mesh.ball(6, 4, 0.006, 2.0).moved(0.081, at[0], at[1]));
        }
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

    // A shell, its rim at z 0 and its body along +z, as it sits in a chamber.
    static Mesh[] shell() {
        return new Mesh[] { rod(10, 0.029, 0.0, 0.07, 1.3), Mesh.cylinder(10, 0.035, -0.008, 0.0, 1.9).alongZ() };
    }

    // The launcher's round body and its rocket drawn long and a little thicker than first made; its grips hang lower
    // under the thicker tube and sit further apart along the longer one.
    private static final double TUBE_THICK = 1.45;
    private static final double WARHEAD_THICK = 1.35;
    private static final double TUBE_LONG = 1.3;
    private static final double GRIP_DROP = 0.056 * (TUBE_THICK - 1.0);

    static Mesh[] rocketLauncher() {
        List<Mesh> parts = new ArrayList<>(List.of(rocketTube()));
        parts.addAll(List.of(rocketWarhead()));
        return parts.toArray(Mesh[]::new);
    }

    // The rocket sitting in the launcher's mouth, along +z from 0.26 to its tip at 0.81, times TUBE_LONG.
    static Mesh[] rocketWarhead() {
        Mesh[] parts = { Mesh.lathe(18, 1.05, 0.0, 0.26, 0.03, 0.26, 0.03, 0.36, 0.085, 0.46, 0.088, 0.49, 0.085,
                0.52, 0.04, 0.68, 0.018, 0.72, 0.012, 0.8, 0.0, 0.81).alongZ(), band(0.088, 0.008, 0.49, 1.8),
                Mesh.ball(8, 5, 0.014, 2.0).moved(0.0, 0.0, 0.805) };
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].scaled(WARHEAD_THICK, WARHEAD_THICK, TUBE_LONG);
        }
        return parts;
    }

    // The launcher without its rocket: the tube along +z, its mouth at 0.27, its flared back at -0.7, times TUBE_LONG.
    static Mesh[] rocketTube() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(rod(14, 0.042, -0.5, 0.27, 1.0));
        parts.add(Mesh.lathe(16, 1.05, 0.0, -0.58, 0.035, -0.58, 0.07, -0.69, 0.085, -0.7, 0.09, -0.68, 0.048, -0.52,
                0.048, -0.5, 0.0, -0.5).alongZ());
        parts.add(band(0.05, 0.01, -0.5, 1.6));
        parts.add(rod(16, 0.056, -0.22, 0.08, 0.95));
        for (double z : new double[] { -0.22, -0.07, 0.08 }) {
            parts.add(band(0.058, 0.009, z, 1.6));
        }
        parts.add(Mesh.box(0.05, 0.03, -0.13, 0.095, 0.09, -0.01, 1.0));
        parts.add(Mesh.box(0.035, 0.0, -0.1, 0.055, 0.04, -0.04, 1.1));
        parts.add(rod(12, 0.026, -0.17, -0.13, 1.1).moved(0.0725, 0.06, 0.0));
        parts.add(rod(12, 0.026, -0.01, 0.03, 1.1).moved(0.0725, 0.06, 0.0));
        parts.add(Mesh.torus(12, 4, 0.018, 0.005, 1.9).alongZ().moved(0.0725, 0.06, 0.031));
        parts.add(Mesh.box(-0.008, 0.04, 0.2, 0.008, 0.085, 0.215, 1.4));
        parts.add(Mesh.box(-0.018, 0.05, -0.04, 0.018, 0.08, -0.025, 1.4));
        for (double z : new double[] { -0.22, -0.07, 0.08 }) {
            for (int k = 0; k < 8; k++) {
                double turn = Math.PI * 2.0 * (k + 0.5) / 8.0;
                parts.add(Mesh.ball(6, 4, 0.006, 1.9).moved(Math.cos(turn) * 0.066, Math.sin(turn) * 0.066, z));
            }
        }
        parts.add(Mesh.box(-0.068, 0.02, -0.13, -0.044, 0.06, -0.04, 1.2));
        parts.add(rod(12, 0.018, -0.17, 0.0, 1.05).moved(-0.056, 0.075, 0.0));
        for (double z : new double[] { -0.17, 0.0 }) {
            parts.add(Mesh.torus(12, 4, 0.02, 0.005, 1.9).alongZ().moved(-0.056, 0.075, z));
        }
        parts.add(Mesh.cylinder(10, 0.012, -0.002, 0.002, 2.3).alongZ().moved(-0.056, 0.075, 0.001));
        for (int i = 0; i < parts.size(); i++) {
            parts.set(i, parts.get(i).scaled(TUBE_THICK, TUBE_THICK, TUBE_LONG));
        }
        int round = parts.size();
        parts.add(leaning(grip(0.2, 0.046, 0.033, 0.95), 105.0, -0.05, -0.13));
        parts.add(Mesh.tube(false, 6, 0.01, 1.2, path(0.0, -0.05, 0.0, 0.0, -0.1, -0.005, 0.0, -0.125, -0.04, 0.0, -0.11,
                -0.075, 0.0, -0.08, -0.09)));
        parts.add(Mesh.tube(false, 5, 0.008, 1.5, path(0.0, -0.05, -0.035, 0.0, -0.075, -0.03, 0.0, -0.095, -0.042)));
        parts.add(leaning(grip(0.17, 0.04, 0.03, 0.95), 95.0, -0.035, 0.14));
        // The grips keep their shape; each moves along with where it sits on the tube (the rear one with the trigger).
        for (int i = round; i < parts.size(); i++) {
            double along = i == parts.size() - 1 ? 0.14 : -0.13;
            parts.set(i, parts.get(i).moved(0.0, -GRIP_DROP, along * (TUBE_LONG - 1.0)));
        }
        return parts.toArray(Mesh[]::new);
    }

    static Mesh[] flamethrower() {
        List<Mesh> parts = new ArrayList<>();
        for (Mesh[] part : flamethrowerParts()) {
            parts.addAll(List.of(part));
        }
        return parts.toArray(Mesh[]::new);
    }

    // Plain points, not path(): GunShapes and WeaponShapes load each other, so this must not wait on WeaponShapes.
    public static final Vec3[] FLAME_HOSE = { new Vec3(0.04, -0.1, -0.08), new Vec3(0.1, -0.1, -0.14),
            new Vec3(0.125, -0.02, -0.22), new Vec3(0.1, 0.06, -0.27), new Vec3(0.04, 0.08, -0.28) };
    public static final int FLAME_FINS = 7;

    // Grip, body, barrel, the fins one by one, nozzle, pilot, tank, hose, front grip, valve: in that order.
    public static Mesh[][] flamethrowerParts() {
        List<Mesh[]> groups = new ArrayList<>();
        List<Mesh> grip = new ArrayList<>();
        grip.add(leaning(grip(0.21, 0.046, 0.034, 0.95), 108.0, -0.055, -0.2));
        grip.add(Mesh.tube(false, 6, 0.01, 1.2, path(0.0, -0.06, -0.1, 0.0, -0.11, -0.105, 0.0, -0.135, -0.14, 0.0,
                -0.12, -0.175)));
        grip.add(Mesh.tube(false, 5, 0.008, 1.5, path(0.0, -0.06, -0.135, 0.0, -0.085, -0.13, 0.0, -0.105, -0.142)));
        groups.add(grip.toArray(Mesh[]::new));
        List<Mesh> body = new ArrayList<>();
        body.add(Mesh.sweep(1.0, SQUIRCLE, new double[] { -0.3, 0.048, 0.062, 0.0 },
                new double[] { -0.28, 0.055, 0.075, 0.0 }, new double[] { 0.02, 0.055, 0.075, 0.0 },
                new double[] { 0.06, 0.045, 0.06, 0.01 }));
        for (int k = 0; k < 4; k++) {
            double z = -0.24 + k * 0.06;
            twice(body, Mesh.box(0.052, 0.0, z, 0.059, 0.035, z + 0.04, 1.9));
        }
        body.add(rod(14, 0.03, -0.24, 0.0, 1.2).moved(0.0, 0.1, 0.0));
        for (double z : new double[] { -0.2, -0.12, -0.04 }) {
            body.add(Mesh.torus(12, 4, 0.032, 0.006, 1.9).alongZ().moved(0.0, 0.1, z));
        }
        body.add(Mesh.box(-0.015, 0.06, -0.2, 0.015, 0.085, -0.17, 1.1));
        body.add(Mesh.box(-0.015, 0.06, -0.07, 0.015, 0.085, -0.04, 1.1));
        groups.add(body.toArray(Mesh[]::new));
        groups.add(new Mesh[] { rod(12, 0.03, 0.05, 0.5, 1.0).moved(0.0, 0.02, 0.0) });
        for (int k = 0; k < FLAME_FINS; k++) {
            double z = 0.1 + k * 0.045;
            groups.add(new Mesh[] { rod(20, 0.052, z, z + 0.012, 1.1).moved(0.0, 0.02, 0.0) });
        }
        groups.add(new Mesh[] {
                Mesh.lathe(18, 1.1, 0.0, 0.46, 0.035, 0.46, 0.05, 0.52, 0.062, 0.6, 0.065, 0.62, 0.045, 0.62, 0.04,
                        0.58, 0.0, 0.58).alongZ().moved(0.0, 0.02, 0.0),
                Mesh.torus(18, 4, 0.05, 0.007, 1.8).alongZ().moved(0.0, 0.02, 0.52),
                Mesh.ball(12, 8, 0.034, 2.3).moved(0.0, 0.02, 0.585) });
        groups.add(new Mesh[] { rod(8, 0.011, 0.38, 0.6, 1.2).moved(0.0, -0.035, 0.0),
                Mesh.ball(8, 5, 0.016, 2.1).moved(0.0, -0.035, 0.605) });
        List<Mesh> tank = new ArrayList<>();
        tank.add(Mesh.box(-0.018, -0.06, 0.02, 0.018, -0.005, 0.2, 1.05));
        tank.add(rod(18, 0.068, -0.06, 0.26, 1.0).moved(0.0, -0.115, 0.0));
        for (double z : new double[] { -0.06, 0.26 }) {
            tank.add(Mesh.ball(18, 10, 0.068, 1.0).scaled(1.0, 1.0, 0.7).moved(0.0, -0.115, z));
        }
        for (double z : new double[] { -0.02, 0.22 }) {
            tank.add(Mesh.torus(18, 5, 0.07, 0.009, 1.6).alongZ().moved(0.0, -0.115, z));
        }
        twice(tank, Mesh.box(0.062, -0.13, 0.02, 0.072, -0.1, 0.18, 2.0));
        groups.add(tank.toArray(Mesh[]::new));
        List<Mesh> hose = new ArrayList<>();
        hose.add(Mesh.tube(false, 7, 0.018, 1.15, FLAME_HOSE));
        for (int i = 1; i < FLAME_HOSE.length - 1; i++) {
            hose.add(hoseRing(i));
        }
        groups.add(hose.toArray(Mesh[]::new));
        groups.add(new Mesh[] { leaning(grip(0.13, 0.042, 0.032, 0.95), 100.0, -0.178, 0.12),
                Mesh.torus(12, 4, 0.036, 0.007, 1.8).moved(0.0, -0.19, 0.118) });
        List<Mesh> valve = new ArrayList<>();
        valve.add(rod(8, 0.01, -0.14, -0.1, 1.2).moved(0.0, -0.115, 0.0));
        valve.add(Mesh.torus(14, 4, 0.034, 0.007, 1.9).alongZ().moved(0.0, -0.115, -0.14));
        for (int k = 0; k < 3; k++) {
            valve.add(Mesh.box(-0.004, 0.0, -0.144, 0.004, 0.032, -0.136, 1.6).turned(0.0, 0.0, 1.0, 120.0 * k)
                    .moved(0.0, -0.115, 0.0));
        }
        groups.add(valve.toArray(Mesh[]::new));
        return groups.toArray(Mesh[][]::new);
    }

    public static Mesh hoseRing(int i) {
        Vec3 along = FLAME_HOSE[i + 1].subtract(FLAME_HOSE[i - 1]);
        return Mesh.torus(10, 4, 0.02, 0.005, 1.7).pointing(along.x, along.y, along.z)
                .moved(FLAME_HOSE[i].x, FLAME_HOSE[i].y, FLAME_HOSE[i].z);
    }
}
