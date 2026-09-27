package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ExpressScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Noise;

// In blocks at scale 1, x right, y up, z forward: the engine round its front bogie on the rails, the tender its middle.
final class ExpressShapes {
    static final double RAIL = 0.72;
    static final double AXLE = 1.2;
    static final double FRONT_DRIVER = -2.9;
    static final double REAR_DRIVER = -5.6;
    static final double CRANK = 0.42;
    static final double MAIN_ROD = 4.6;
    static final double SIDE_ROD_X = 0.98;
    static final double MAIN_ROD_X = 1.06;
    static final double CROSSHEAD_X = 1.035;
    static final double LEAD_AXLE = 0.55;
    static final double TENDER_AXLE = 0.5;
    static final double[] LEAD_AXLES = { 0.55, -0.55 };
    static final double[] TENDER_AXLES = { 2.75, 1.65, -1.65, -2.75 };
    static final Vec3 LENS = new Vec3(0.0, 4.04, 1.86);
    static final Vec3 FUNNEL_TOP = new Vec3(0.0, 5.3, 0.85);
    static final Vec3 FIRE_DOOR = new Vec3(0.0, 2.55, -6.4);
    static final double BOILER_AXIS = 2.75;
    static final Vec3 BOILER_MIDDLE = new Vec3(0.0, BOILER_AXIS, -2.3);
    static final double BOILER_RADIUS = 0.85;
    static final double BOILER_FRONT = 1.5;
    static final double BOILER_BACK = -6.3;

    static final ConstructPainter.Shape ENGINE = new ConstructPainter.Shape(engineBoxes(), engineMeshes());
    static final ConstructPainter.Shape BOILER = ConstructPainter.Shape.of(boiler());
    static final ConstructPainter.Shape DRIVER = ConstructPainter.Shape.of(driver());
    static final ConstructPainter.Shape DRIVER_LEFT = mirrored(DRIVER);
    static final ConstructPainter.Shape LEAD = ConstructPainter.Shape.of(small(LEAD_AXLE, 6));
    static final ConstructPainter.Shape LEAD_LEFT = mirrored(LEAD);
    static final ConstructPainter.Shape TENDER_WHEEL = ConstructPainter.Shape.of(small(TENDER_AXLE, 6));
    static final ConstructPainter.Shape TENDER_WHEEL_LEFT = mirrored(TENDER_WHEEL);
    static final ConstructPainter.Shape SIDE_ROD = ConstructPainter.Shape.of(sideRod());
    static final ConstructPainter.Shape MAIN = ConstructPainter.Shape.of(mainRod());
    static final ConstructPainter.Shape CROSSHEAD = ConstructPainter.Shape.of(crosshead());
    static final ConstructPainter.Shape TENDER = new ConstructPainter.Shape(tenderBoxes(), tenderMeshes());

    private ExpressShapes() {
    }

    private static ConstructPainter.Shape mirrored(ConstructPainter.Shape shape) {
        Mesh[] meshes = new Mesh[shape.meshes().length];
        for (int i = 0; i < meshes.length; i++) {
            meshes[i] = shape.meshes()[i].mirrored();
        }
        return ConstructPainter.Shape.of(meshes);
    }

    private static void pair(List<double[]> boxes, double x0, double y0, double z0, double x1, double y1, double z1,
            double bright) {
        boxes.add(new double[] { x0, y0, z0, x1, y1, z1, bright });
        boxes.add(new double[] { -x1, y0, z0, -x0, y1, z1, bright });
    }

    private static void box(List<double[]> boxes, double x0, double y0, double z0, double x1, double y1, double z1,
            double bright) {
        boxes.add(new double[] { x0, y0, z0, x1, y1, z1, bright });
    }

    private static void pair(List<Mesh> parts, Mesh right) {
        parts.add(right);
        parts.add(right.mirrored());
    }

    private static Mesh along(Mesh lathe, double y, double z) {
        return lathe.alongZ().moved(0.0, y, z);
    }

    private static Mesh bar(double radius, double bright, Vec3 from, Vec3 to) {
        return Mesh.tube(false, 6, radius, bright, from, to);
    }

    static Mesh emblem(double radius) {
        double bar = radius * 0.24;
        return Mesh.merged(Mesh.torus(24, 5, radius, radius * 0.12, 1.75),
                Mesh.box(-radius, -0.05, bar * 0.9, radius, 0.06, bar * 1.6, 1.75),
                Mesh.box(-radius, -0.05, -bar * 1.6, radius, 0.06, -bar * 0.9, 1.75));
    }

    // Facing out of a side of the train (+x), its bars running along it.
    static Mesh sideEmblem(double radius) {
        return emblem(radius).turned(1.0, 1.0, 1.0, -120.0);
    }

    private static double[][] engineBoxes() {
        List<double[]> b = new ArrayList<>();
        pair(b, 0.46, 0.78, -8.4, 0.58, 1.45, 1.3, 0.85);
        box(b, -1.35, 0.72, 1.32, 1.35, 1.12, 1.6, 1.15);
        box(b, -0.18, 0.78, 1.6, 0.18, 1.02, 1.78, 1.2);
        box(b, -0.16, 1.15, -9.0, 0.16, 1.35, -8.4, 1.0);
        pair(b, 0.9, 0.4, -0.95, 1.0, 0.7, 0.95, 0.9);
        box(b, -1.0, 0.62, -0.18, 1.0, 0.82, 0.18, 0.9);
        for (double z : LEAD_AXLES) {
            pair(b, 0.86, 0.42, z - 0.13, 1.04, 0.7, z + 0.13, 1.0);
        }
        pair(b, 0.84, 1.52, -0.35, 1.34, 1.95, 0.75, 1.1);
        box(b, -0.84, 1.3, -0.25, 0.84, 1.95, 0.65, 0.95);
        pair(b, 1.0, 1.31, -2.25, 1.07, 1.37, -0.35, 1.25);
        pair(b, 1.0, 1.03, -2.25, 1.07, 1.09, -0.35, 1.25);
        pair(b, 0.58, 0.98, -2.35, 1.12, 1.75, -2.2, 1.0);
        pair(b, 0.95, 1.92, -6.0, 1.5, 1.98, 0.1, 1.2);
        pair(b, 1.45, 1.72, -6.0, 1.5, 1.92, 0.1, 1.05);
        box(b, -1.42, 1.9, -8.62, 1.42, 2.02, -5.98, 0.95);
        pair(b, 0.9, 2.02, -6.1, 1.42, 3.3, -5.98, 1.0);
        pair(b, 0.9, 3.95, -6.1, 1.42, 4.3, -5.98, 1.0);
        pair(b, 0.9, 3.3, -6.1, 0.98, 3.95, -5.98, 1.15);
        pair(b, 1.32, 3.3, -6.1, 1.42, 3.95, -5.98, 1.15);
        box(b, -0.9, 3.75, -6.1, 0.9, 4.3, -5.98, 1.0);
        pair(b, 1.32, 2.02, -8.6, 1.42, 3.1, -6.1, 1.0);
        pair(b, 1.32, 3.1, -6.4, 1.42, 4.3, -6.1, 1.0);
        pair(b, 1.32, 3.1, -7.4, 1.42, 4.3, -7.2, 1.0);
        pair(b, 1.32, 3.1, -8.6, 1.42, 4.3, -8.3, 1.0);
        pair(b, 1.32, 4.08, -8.3, 1.42, 4.3, -6.4, 1.0);
        pair(b, 1.3, 3.06, -8.4, 1.46, 3.14, -6.2, 1.35);
        box(b, -1.42, 2.9, -8.62, 1.42, 3.0, -8.52, 1.3);
        box(b, -0.72, 2.0, -6.35, 0.72, 3.5, -6.1, 0.9);
        box(b, -0.3, 2.28, -6.4, 0.3, 2.84, -6.35, 1.45);
        box(b, -0.62, 0.95, -8.2, 0.62, 2.0, -6.25, 0.85);
        box(b, -0.52, 0.7, -8.0, 0.52, 0.95, -6.4, 0.75);
        pair(b, 1.35, 1.2, -8.5, 1.62, 1.26, -8.05, 1.1);
        pair(b, 1.35, 1.55, -8.5, 1.62, 1.61, -8.05, 1.1);
        pair(b, 1.56, 1.2, -8.5, 1.62, 1.92, -8.44, 1.05);
        pair(b, 1.56, 1.2, -8.11, 1.62, 1.92, -8.05, 1.05);
        box(b, -0.22, 3.6, 1.05, 0.22, 3.78, 1.55, 1.0);
        box(b, -0.36, 3.78, 1.15, 0.36, 4.3, 1.8, 1.1);
        box(b, -0.38, 3.76, 1.78, 0.38, 4.32, 1.84, 1.35);
        box(b, -0.4, 4.3, 1.1, 0.4, 4.36, 1.86, 1.2);
        box(b, -0.25, 4.44, -7.6, 0.25, 4.56, -7.0, 1.15);
        pair(b, 0.24, 3.58, -0.3, 0.3, 4.3, -0.2, 1.15);
        box(b, -0.3, 4.26, -0.3, 0.3, 4.32, -0.2, 1.15);
        return b.toArray(double[][]::new);
    }

    private static Mesh[] engineMeshes() {
        List<Mesh> m = new ArrayList<>();
        m.add(along(Mesh.cylinder(28, 0.95, 0.25, BOILER_FRONT, 1.0), 2.75, 0.0));
        m.add(along(Mesh.torus(28, 5, 0.97, 0.045, 1.3), 2.75, 0.27));
        m.add(along(Mesh.torus(32, 6, 0.95, 0.06, 1.3), 2.75, BOILER_FRONT + 0.02));
        m.add(along(Mesh.lathe(28, 1.05, 0.0, 0.0, 0.86, 0.0, 0.86, 0.05, 0.8, 0.1, 0.55, 0.15, 0.0, 0.17), 2.75,
                BOILER_FRONT + 0.02));
        m.add(Mesh.box(-0.78, 2.4, 1.64, 0.2, 2.47, 1.7, 1.3));
        m.add(Mesh.box(-0.78, 3.03, 1.64, 0.2, 3.1, 1.7, 1.3));
        m.add(along(Mesh.torus(14, 5, 0.13, 0.025, 1.45), 2.25, 1.72));
        m.add(emblem(0.27).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 2.9, 1.7));
        for (double z : new double[] { 1.3, -0.4, -2.0, -3.4, -5.0, -6.0 }) {
            pair(m, bar(0.025, 1.2, new Vec3(0.82, 2.98, z), new Vec3(1.03, 2.98, z)));
        }
        pair(m, bar(0.032, 1.3, new Vec3(1.03, 2.98, 1.35), new Vec3(1.03, 2.98, -5.95)));
        m.add(Mesh.lathe(24, 1.0, 0.0, 3.5, 0.3, 3.5, 0.3, 3.88, 0.37, 3.9, 0.37, 3.98, 0.27, 4.02, 0.27, 4.45,
                0.33, 4.55, 0.58, 4.84, 0.73, 5.04, 0.76, 5.12, 0.63, 5.2, 0.48, 5.24, 0.26, 5.28, 0.0, 5.3)
                .moved(0.0, 0.0, FUNNEL_TOP.z));
        m.add(Mesh.torus(24, 5, 0.75, 0.035, 1.45).moved(0.0, 5.1, FUNNEL_TOP.z));
        m.add(Mesh.torus(20, 5, 0.32, 0.03, 1.35).moved(0.0, 3.92, FUNNEL_TOP.z));
        m.add(Mesh.lathe(20, 1.0, 0.0, 3.5, 0.46, 3.5, 0.46, 3.66, 0.52, 3.68, 0.52, 3.72, 0.44, 3.74, 0.44, 3.9,
                0.38, 4.0, 0.26, 4.08, 0.0, 4.12).moved(0.0, 0.0, -1.2));
        m.add(Mesh.lathe(16, 1.3, 0.0, 4.2, 0.07, 4.2, 0.1, 4.16, 0.13, 4.05, 0.16, 3.92, 0.22, 3.8, 0.24, 3.76, 0.0,
                3.76).moved(0.0, 0.0, -0.25));
        m.add(Mesh.lathe(22, 1.0, 0.0, 3.45, 0.58, 3.45, 0.58, 3.6, 0.5, 3.62, 0.5, 4.12, 0.46, 4.28, 0.36, 4.4,
                0.2, 4.46, 0.0, 4.48).moved(0.0, 0.0, -2.6));
        m.add(Mesh.torus(20, 5, 0.52, 0.03, 1.35).moved(0.0, 4.1, -2.6));
        m.add(Mesh.cylinder(8, 0.05, 4.46, 4.68, 1.2).moved(0.0, 0.0, -2.6));
        m.add(Mesh.lathe(10, 1.4, 0.0, 4.66, 0.08, 4.66, 0.08, 4.9, 0.1, 4.93, 0.0, 4.95).moved(0.0, 0.0, -2.6));
        pair(m, Mesh.cylinder(8, 0.07, 3.5, 3.86, 1.2).moved(0.18, 0.0, -5.5));
        pair(m, Mesh.cylinder(8, 0.1, 3.86, 3.92, 1.35).moved(0.18, 0.0, -5.5));
        pair(m, Mesh.cylinder(20, 0.36, -0.35, 0.75, 1.0).alongZ().moved(1.08, AXLE, 0.0));
        pair(m, Mesh.torus(20, 5, 0.37, 0.045, 1.3).alongZ().moved(1.08, AXLE, -0.35));
        pair(m, Mesh.torus(20, 5, 0.37, 0.045, 1.3).alongZ().moved(1.08, AXLE, 0.75));
        pair(m, Mesh.lathe(16, 1.15, 0.0, 0.0, 0.3, 0.0, 0.22, 0.08, 0.0, 0.1).alongZ().moved(1.08, AXLE, 0.75));
        pair(m, bar(0.09, 1.0, new Vec3(0.72, 2.3, 0.75), new Vec3(1.05, 1.95, 0.35)));
        for (double z : new double[] { FRONT_DRIVER, REAR_DRIVER }) {
            pair(m, Mesh.panel(10, 1.2, Math.toRadians(30.0), Math.toRadians(150.0), 0.05,
                    new double[] { -0.13, 1.34, 1.34, 0.0 }, new double[] { 0.13, 1.34, 1.34, 0.0 })
                    .turned(0.0, 1.0, 0.0, 90.0).moved(RAIL, AXLE, z));
        }
        m.add(Mesh.panel(12, 1.05, Math.toRadians(12.0), Math.toRadians(168.0), 0.07,
                new double[] { -8.85, 1.62, 0.42, 4.05 }, new double[] { -5.8, 1.62, 0.42, 4.05 }));
        m.add(Mesh.prism(1.1, 1.86, 1.2, -0.42, 4.36, 0.42, 4.36, 0.0, 4.56));
        m.add(Mesh.cylinder(8, 0.07, 4.5, 4.74, 1.1).moved(0.0, 0.0, 1.45));
        m.add(Mesh.cylinder(10, 0.12, 4.74, 4.78, 1.25).moved(0.0, 0.0, 1.45));
        m.add(along(Mesh.cylinder(20, 0.25, 0.0, 0.04, 1.9), LENS.y, 1.82));
        m.add(along(Mesh.torus(20, 5, 0.26, 0.03, 1.4), LENS.y, LENS.z));
        m.add(Mesh.sweep(0.95, new double[] { -1.0, -1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0 },
                new double[] { 1.6, 1.3, 0.36, 0.5 }, new double[] { 2.42, 0.1, 0.05, 0.1 }));
        for (int k = -4; k <= 4; k++) {
            double x = k * 0.29;
            m.add(bar(0.03, 1.35, new Vec3(x, 0.86, 1.6), new Vec3(x * 0.08, 0.12, 2.4)));
        }
        pair(m, sideEmblem(0.36).moved(1.43, 2.55, -7.3));
        return m.toArray(Mesh[]::new);
    }

    // Round its own axis, so it can swell there under pressure.
    private static Mesh[] boiler() {
        List<Mesh> m = new ArrayList<>();
        m.add(along(Mesh.cylinder(28, BOILER_RADIUS, -4.35, 0.25, 1.0), 0.0, 0.0));
        m.add(along(Mesh.lathe(28, 1.0, 0.0, -4.65, 0.95, -4.65, BOILER_RADIUS, -4.35, 0.0, -4.35), 0.0, 0.0));
        m.add(along(Mesh.cylinder(28, 0.95, BOILER_BACK, -4.65, 1.0), 0.0, 0.0));
        for (double z : new double[] { -0.9, -2.0, -3.3 }) {
            m.add(along(Mesh.torus(28, 5, 0.87, 0.04, 1.3), 0.0, z));
        }
        m.add(along(Mesh.torus(28, 5, 0.97, 0.04, 1.3), 0.0, -5.4));
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] driver() {
        List<Mesh> m = new ArrayList<>();
        double tire = ExpressScript.DRIVER_RADIUS;
        m.add(Mesh.ring(32, 1.0, 0.98, -0.1, tire + 0.12, -0.1, tire + 0.12, -0.06, tire, -0.04, tire, 0.09, 0.98,
                0.09).alongX());
        for (int k = 0; k < 14; k++) {
            m.add(Mesh.box(-0.035, 0.22, -0.045, 0.035, 1.0, 0.045, 1.05).turned(1.0, 0.0, 0.0, k * 360.0 / 14.0));
        }
        m.add(Mesh.cylinder(14, 0.26, -0.13, 0.13, 1.1).alongX());
        m.add(Mesh.cylinder(10, 0.12, 0.13, 0.2, 1.3).alongX());
        m.add(Mesh.box(-0.05, -0.97, -0.4, 0.06, -0.52, 0.4, 0.95));
        m.add(Mesh.cylinder(10, 0.1, 0.06, 0.16, 1.2).alongX().moved(0.0, CRANK, 0.0));
        m.add(Mesh.cylinder(8, 0.06, 0.16, 0.42, 1.3).alongX().moved(0.0, CRANK, 0.0));
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] small(double radius, int spokes) {
        List<Mesh> m = new ArrayList<>();
        double rim = radius - 0.1;
        m.add(Mesh.ring(24, 1.0, rim, -0.09, radius + 0.07, -0.09, radius + 0.07, -0.05, radius, -0.03, radius, 0.08,
                rim, 0.08).alongX());
        m.add(Mesh.cylinder(18, rim, -0.03, 0.03, 0.9).alongX());
        for (int k = 0; k < spokes; k++) {
            m.add(Mesh.box(-0.045, 0.14, -0.04, 0.045, rim, 0.04, 1.1).turned(1.0, 0.0, 0.0, k * 360.0 / spokes));
        }
        m.add(Mesh.cylinder(12, 0.14, -0.09, 0.12, 1.2).alongX());
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] sideRod() {
        double length = FRONT_DRIVER - REAR_DRIVER;
        return new Mesh[] { Mesh.box(-0.035, -0.075, -0.1, 0.035, 0.075, length + 0.1, 1.15),
                Mesh.cylinder(12, 0.12, -0.045, 0.045, 1.25).alongX(),
                Mesh.cylinder(12, 0.12, -0.045, 0.045, 1.25).alongX().moved(0.0, 0.0, length) };
    }

    private static Mesh[] mainRod() {
        return new Mesh[] { Mesh.sweep(1.15, new double[] { -1.0, -1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0 },
                new double[] { 0.0, 0.035, 0.1, 0.0 }, new double[] { MAIN_ROD, 0.035, 0.07, 0.0 }),
                Mesh.cylinder(12, 0.16, -0.05, 0.05, 1.25).alongX(),
                Mesh.cylinder(10, 0.1, -0.05, 0.05, 1.25).alongX().moved(0.0, 0.0, MAIN_ROD) };
    }

    private static Mesh[] crosshead() {
        return new Mesh[] { Mesh.box(-0.06, -0.13, -0.2, 0.06, 0.13, 0.2, 1.1),
                Mesh.box(-0.03, -0.03, 0.0, 0.03, 0.03, 1.4, 1.3),
                Mesh.cylinder(8, 0.06, -0.08, 0.08, 1.35).alongX() };
    }

    private static double[][] tenderBoxes() {
        List<double[]> b = new ArrayList<>();
        box(b, -1.38, 1.02, -3.5, 1.38, 1.3, 3.5, 0.95);
        box(b, -1.4, 0.95, 3.42, 1.4, 1.3, 3.56, 1.2);
        box(b, -1.4, 0.95, -3.56, 1.4, 1.3, -3.42, 1.2);
        box(b, -1.38, 1.3, -3.42, 1.38, 2.62, 0.55, 1.0);
        pair(b, 1.3, 2.62, -3.5, 1.48, 2.76, 3.5, 1.3);
        box(b, -1.3, 2.62, -3.56, 1.3, 2.76, -3.42, 1.3);
        pair(b, 1.2, 1.3, 0.55, 1.38, 2.62, 3.42, 1.0);
        box(b, -1.2, 1.3, 3.28, 1.2, 1.95, 3.42, 1.05);
        for (double z : new double[] { 2.2, -2.2 }) {
            pair(b, 0.88, 0.3, z - 0.95, 1.0, 0.78, z + 0.95, 0.9);
            box(b, -1.0, 0.72, z - 0.16, 1.0, 1.02, z + 0.16, 0.9);
        }
        for (double z : TENDER_AXLES) {
            pair(b, 0.86, 0.34, z - 0.12, 1.04, 0.62, z + 0.12, 1.0);
        }
        box(b, -1.1, 2.62, -3.2, -0.5, 2.9, -2.6, 1.15);
        box(b, 0.5, 2.62, -3.2, 1.1, 2.9, -2.6, 1.15);
        pair(b, 0.4, 1.3, -3.62, 0.46, 2.62, -3.56, 1.2);
        for (double y : new double[] { 1.6, 1.9, 2.2, 2.5 }) {
            box(b, -0.4, y, -3.62, 0.4, y + 0.04, -3.58, 1.2);
        }
        return b.toArray(double[][]::new);
    }

    private static Mesh[] tenderMeshes() {
        List<Mesh> m = new ArrayList<>();
        double[] layers = { 1.5, 1.82, 2.14, 2.46 };
        for (int layer = 0; layer < layers.length; layer++) {
            int logs = 6 - layer;
            for (int k = 0; k < logs; k++) {
                double z = 0.85 + layer * 0.215 + k * 0.43;
                double bright = 0.85 + 0.25 * Noise.of(layer, k, 17);
                m.add(Mesh.cylinder(8, 0.17, -1.12, 1.12, bright).alongX().moved(0.0, layers[layer], z));
            }
        }
        m.add(Mesh.cylinder(16, 0.34, 2.62, 2.74, 1.2).moved(0.0, 0.0, -1.8));
        m.add(Mesh.torus(12, 4, 0.12, 0.025, 1.4).moved(0.0, 2.78, -1.8));
        m.add(bar(0.035, 1.2, new Vec3(-1.05, 1.3, -3.35), new Vec3(-1.05, 3.1, -3.35)));
        m.add(Mesh.torus(16, 4, 0.22, 0.025, 1.4).moved(-1.05, 3.1, -3.35));
        pair(m, sideEmblem(0.62).moved(1.39, 1.96, -1.45));
        return m.toArray(Mesh[]::new);
    }
}
