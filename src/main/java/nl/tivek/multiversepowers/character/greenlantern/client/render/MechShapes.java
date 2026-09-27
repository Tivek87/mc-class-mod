package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;

// In blocks: the body round the ground spot under the pilot, every limb from its lower joint up along y.
final class MechShapes {
    static final double SHIN = MechScript.KNEE.subtract(MechScript.ANKLE).length();
    static final double THIGH = MechScript.HIP.subtract(MechScript.KNEE).length();
    static final double UPPER_ARM = MechScript.SHOULDER.subtract(MechScript.ELBOW).length();
    static final Vec3 ANKLE_IN_BOOT = new Vec3(0.0, 0.95, -0.25);
    static final double[] FINGER_X = { 0.3, 0.1, -0.1, -0.3 };
    static final double[][] FINGER_LENGTHS = { { 0.4, 0.32, 0.26 }, { 0.42, 0.34, 0.27 }, { 0.4, 0.32, 0.26 },
            { 0.34, 0.27, 0.22 } };
    static final double FINGER_ROOT = 2.95;
    static final Vec3 THUMB_ROOT = new Vec3(0.44, 2.3, 0.05);
    static final double[] THUMB_LENGTHS = { 0.34, 0.28 };
    static final Vec3 GLASS_AT = new Vec3(0.0, 7.2, 1.2);
    static final Vec3 EXHAUST = new Vec3(0.75, 8.95, -1.78);
    static final double GLASS_RADIUS = 0.92;
    private static final Vec3 GRIP = MechScript.GRIP;

    static final Shape BOOT = Shape.of(boot());
    static final Shape BOOT_LEFT = mirrored(BOOT);
    static final Shape SHIN_PART = Shape.of(shin());
    static final Shape KNEE_PART = Shape.of(knee());
    static final Shape THIGH_PART = Shape.of(thigh());
    static final Shape PELVIS = Shape.of(pelvis());
    static final Shape TORSO = new Shape(torsoBoxes(), torsoMeshes());
    static final Shape COCKPIT = Shape.of(cockpit());
    static final Shape GLASS = Shape.of(Mesh.cylinder(40, GLASS_RADIUS, 0.0, 0.03, 1.0).alongZ());
    static final Shape SHOULDER = Shape.of(shoulder());
    static final Shape SHOULDER_LEFT = mirrored(SHOULDER);
    static final Shape UPPER = Shape.of(upperArm());
    static final Shape UPPER_LEFT = mirrored(UPPER);
    static final Shape FOREARM = Shape.of(forearm());
    static final Shape FOREARM_LEFT = mirrored(FOREARM);
    static final Shape[] FINGER = { segment(0.085, 0.4), segment(0.085, 0.32), segment(0.08, 0.26) };
    static final Shape[] THUMB = { segment(0.1, THUMB_LENGTHS[0]), segment(0.095, THUMB_LENGTHS[1]) };
    static final Shape HEAD = Shape.of(head());

    private MechShapes() {
    }

    private static Shape mirrored(Shape shape) {
        Mesh[] meshes = new Mesh[shape.meshes().length];
        for (int i = 0; i < meshes.length; i++) {
            meshes[i] = shape.meshes()[i].mirrored();
        }
        return Shape.of(meshes);
    }

    private static void pair(List<Mesh> parts, Mesh right) {
        parts.add(right);
        parts.add(right.mirrored());
    }

    private static void pair(List<double[]> boxes, double x0, double y0, double z0, double x1, double y1, double z1,
            double bright) {
        boxes.add(new double[] { x0, y0, z0, x1, y1, z1, bright });
        boxes.add(new double[] { -x1, y0, z0, -x0, y1, z1, bright });
    }

    private static Mesh bar(double radius, double bright, Vec3 from, Vec3 to) {
        return Mesh.tube(false, 6, radius, bright, from, to);
    }

    private static Mesh[] boot() {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.box(-0.62, 0.0, -1.05, 0.62, 0.16, 1.45, 0.85));
        m.add(Mesh.box(-0.56, 0.16, -0.85, 0.56, 0.62, 0.95, 1.0));
        m.add(Mesh.sweep(1.05, new double[] { -1.0, -1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0 },
                new double[] { 0.95, 0.56, 0.2, 0.36 }, new double[] { 1.5, 0.5, 0.06, 0.22 }));
        m.add(Mesh.box(-0.5, 0.62, 0.2, 0.5, 0.72, 0.6, 1.2));
        m.add(Mesh.box(-0.5, 0.62, 0.63, 0.5, 0.7, 0.95, 1.25));
        m.add(Mesh.box(-0.5, 0.1, -1.2, 0.5, 0.75, -0.8, 0.95));
        m.add(Mesh.box(-0.5, 0.62, -0.6, 0.5, 0.82, 0.25, 1.1));
        m.add(Mesh.cylinder(20, 0.42, -0.62, 0.62, 1.0).alongX().moved(0.0, ANKLE_IN_BOOT.y, ANKLE_IN_BOOT.z));
        m.add(Mesh.cylinder(20, 0.34, 0.62, 0.68, 1.3).alongX().moved(0.0, ANKLE_IN_BOOT.y, ANKLE_IN_BOOT.z));
        m.add(ExpressShapes.sideEmblem(0.25).moved(0.69, ANKLE_IN_BOOT.y, ANKLE_IN_BOOT.z));
        pair(m, bar(0.06, 1.3, new Vec3(0.5, 0.45, 0.6), new Vec3(0.5, 0.95, -0.2)));
        m.add(Mesh.box(-0.64, 0.05, 1.2, 0.64, 0.12, 1.5, 1.35));
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] shin() {
        double top = SHIN;
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.box(-0.36, 0.1, -0.4, 0.36, top - 0.15, 0.35, 0.9));
        m.add(Mesh.box(-0.46, 0.35, 0.3, 0.46, top - 0.25, 0.52, 1.1));
        m.add(Mesh.box(-0.4, 0.15, 0.28, 0.4, 0.4, 0.46, 1.25));
        m.add(Mesh.box(-0.5, 0.7, -0.62, 0.5, top - 0.35, -0.3, 1.0));
        pair(m, Mesh.box(0.36, 0.3, -0.35, 0.5, top - 0.3, 0.3, 1.05));
        m.add(bar(0.07, 1.35, new Vec3(0.0, 0.3, -0.62), new Vec3(0.0, top - 0.3, -0.58)));
        for (double y : new double[] { 0.85, 1.35 }) {
            m.add(Mesh.box(-0.48, y, 0.5, 0.48, y + 0.08, 0.56, 1.4));
        }
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] knee() {
        return new Mesh[] { Mesh.cylinder(20, 0.42, -0.55, 0.55, 1.0).alongX(),
                Mesh.box(-0.45, -0.35, 0.28, 0.45, 0.45, 0.6, 1.2),
                Mesh.prism(0.6, 0.78, 1.3, -0.4, -0.25, 0.4, -0.25, 0.0, 0.2),
                Mesh.torus(20, 4, 0.42, 0.04, 1.45).alongX().moved(0.56, 0.0, 0.0),
                Mesh.torus(20, 4, 0.42, 0.04, 1.45).alongX().moved(-0.56, 0.0, 0.0) };
    }

    private static Mesh[] thigh() {
        double top = THIGH;
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.box(-0.48, 0.2, -0.5, 0.48, top, 0.5, 0.9));
        m.add(Mesh.box(-0.56, 0.35, 0.45, 0.56, top - 0.15, 0.66, 1.1));
        pair(m, Mesh.box(0.46, 0.3, -0.45, 0.6, top - 0.2, 0.45, 1.05));
        m.add(Mesh.box(-0.5, 0.4, -0.66, 0.5, top - 0.25, -0.45, 1.0));
        m.add(Mesh.box(-0.58, 0.95, 0.64, 0.58, 1.05, 0.7, 1.4));
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] pelvis() {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.box(-1.65, 4.75, -0.95, 1.65, 5.5, 0.85, 0.95));
        pair(m, Mesh.cylinder(20, 0.45, 0.8, 1.75, 1.1).alongX().moved(0.0, MechScript.HIP.y, MechScript.HIP.z));
        m.add(Mesh.box(-1.15, 5.4, -0.85, 1.15, 5.9, 0.75, 1.0));
        m.add(Mesh.box(-0.55, 4.6, 0.8, 0.55, 5.45, 1.05, 1.2));
        pair(m, Mesh.box(0.6, 4.7, 0.82, 1.55, 5.35, 0.98, 1.15));
        m.add(Mesh.box(-1.7, 5.46, -1.0, 1.7, 5.54, 0.9, 1.4));
        return m.toArray(Mesh[]::new);
    }

    private static double[][] torsoBoxes() {
        List<double[]> b = new ArrayList<>();
        b.add(new double[] { -1.2, 5.85, -1.2, 1.2, 6.2, 1.25, 0.95 });
        b.add(new double[] { -1.9, 5.5, -1.45, 1.9, 8.6, -0.8, 1.0 });
        pair(b, 1.0, 5.5, -0.8, 1.9, 8.35, 1.3, 1.0);
        b.add(new double[] { -1.9, 8.05, -0.8, 1.9, 8.6, 1.3, 1.0 });
        b.add(new double[] { -1.9, 5.5, -0.8, 1.9, 5.85, 1.35, 0.95 });
        pair(b, 1.3, 5.6, 1.3, 1.95, 8.3, 1.55, 1.05);
        b.add(new double[] { -1.5, 8.45, 1.2, 1.5, 8.62, 1.55, 1.7 });
        b.add(new double[] { -1.5, 5.72, 1.2, 1.5, 5.9, 1.55, 1.7 });
        b.add(new double[] { -1.3, 8.55, -1.1, 1.3, 8.8, 0.9, 1.1 });
        pair(b, 1.9, 6.0, -1.3, 2.05, 8.1, 1.2, 1.15);
        b.add(new double[] { -1.6, 6.2, -1.62, 1.6, 7.9, -1.45, 1.1 });
        return b.toArray(double[][]::new);
    }

    private static Mesh[] torsoMeshes() {
        List<Mesh> m = new ArrayList<>();
        double inner = GLASS_RADIUS - 0.02;
        m.add(Mesh.panel(40, 1.5, 0.0, Math.PI * 2.0, 0.55, new double[] { 1.05, inner, inner, GLASS_AT.y },
                new double[] { 1.4, inner, inner, GLASS_AT.y }));
        m.add(Mesh.torus(40, 5, inner + 0.02, 0.05, 1.8).alongZ().moved(0.0, GLASS_AT.y, 1.42));
        m.add(Mesh.cylinder(16, 0.45, 8.7, 8.95, 1.0).moved(0.0, 0.0, MechScript.NECK.z));
        m.add(Mesh.torus(16, 4, 0.47, 0.04, 1.4).moved(0.0, 8.93, MechScript.NECK.z));
        m.add(Mesh.box(-1.1, 6.35, -1.95, 1.1, 8.25, -1.6, 1.05));
        for (double y : new double[] { 6.75, 7.15, 7.55 }) {
            m.add(Mesh.box(-0.85, y - 0.05, -2.0, 0.85, y + 0.05, -1.93, 1.4));
        }
        pair(m, Mesh.cylinder(14, 0.26, 7.3, EXHAUST.y, 1.1).moved(EXHAUST.x, 0.0, EXHAUST.z));
        pair(m, Mesh.torus(14, 4, 0.27, 0.035, 1.5).moved(EXHAUST.x, EXHAUST.y, EXHAUST.z));
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] cockpit() {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.box(-0.45, 6.2, -0.55, 0.45, 6.38, 0.3, 0.9));
        m.add(Mesh.box(-0.45, 6.38, -0.78, 0.45, 7.45, -0.62, 0.9));
        m.add(Mesh.box(-0.3, 7.45, -0.8, 0.3, 7.7, -0.66, 0.95));
        m.add(Mesh.box(-0.95, 6.2, 0.8, 0.95, 6.62, 1.05, 1.05));
        for (int flank = -1; flank <= 1; flank += 2) {
            double x = flank * GRIP.x;
            m.add(Mesh.box(x - 0.1, 6.2, GRIP.z - 0.1, x + 0.1, 6.32, GRIP.z + 0.1, 1.2));
            m.add(bar(0.035, 1.3, new Vec3(x, 6.3, GRIP.z), new Vec3(x, GRIP.y - 0.06, GRIP.z)));
            m.add(Mesh.cylinder(10, 0.055, GRIP.y - 0.08, GRIP.y + 0.08, 1.3).moved(x, 0.0, GRIP.z));
            m.add(Mesh.ball(8, 5, 0.065, 1.6).moved(x, GRIP.y + 0.1, GRIP.z));
        }
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] shoulder() {
        Vec3 joint = MechScript.SHOULDER;
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.ball(16, 10, 0.5, 1.0).moved(joint.x, joint.y, joint.z));
        m.add(Mesh.box(1.7, 8.2, -1.15, 3.25, 8.9, 1.05, 1.05));
        m.add(Mesh.box(2.5, 7.55, -1.1, 3.35, 8.25, 1.0, 1.15));
        m.add(Mesh.box(2.9, 7.0, -1.0, 3.45, 7.6, 0.9, 1.25));
        m.add(Mesh.box(1.7, 8.9, -1.0, 3.3, 8.98, 0.9, 1.4));
        m.add(Mesh.box(3.25, 8.3, -0.9, 3.3, 8.8, 0.8, 1.45));
        m.add(ExpressShapes.sideEmblem(0.32).moved(3.46, 7.3, -0.05));
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] upperArm() {
        double top = UPPER_ARM;
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.box(-0.38, 0.3, -0.4, 0.38, top, 0.4, 0.95));
        m.add(Mesh.box(0.3, 0.4, -0.45, 0.48, top - 0.2, 0.45, 1.1));
        m.add(Mesh.cylinder(20, 0.42, -0.5, 0.5, 1.15).alongX());
        m.add(Mesh.torus(20, 4, 0.43, 0.04, 1.5).alongX().moved(0.5, 0.0, 0.0));
        m.add(bar(0.06, 1.35, new Vec3(0.0, 0.3, 0.45), new Vec3(0.0, top - 0.3, 0.42)));
        return m.toArray(Mesh[]::new);
    }

    // A forearm with its hand: +y from the elbow to the knuckles, the palm facing +z, the thumb at +x.
    private static Mesh[] forearm() {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.cylinder(18, 0.36, -0.42, 0.42, 1.0).alongX());
        m.add(Mesh.box(-0.36, 0.15, -0.34, 0.36, 1.95, 0.3, 0.95));
        m.add(Mesh.box(-0.44, 0.3, -0.48, 0.44, 1.8, -0.3, 1.1));
        for (double y : new double[] { 0.85, 1.35 }) {
            m.add(Mesh.box(-0.46, y, -0.52, 0.46, y + 0.1, -0.46, 1.3));
        }
        pair(m, Mesh.box(0.36, 0.4, -0.3, 0.46, 1.7, 0.25, 1.05));
        m.add(Mesh.box(-0.44, 1.78, -0.4, 0.44, 2.0, 0.32, 1.25));
        m.add(Mesh.box(-0.42, 2.0, -0.2, 0.42, 2.92, 0.16, 1.0));
        m.add(Mesh.box(-0.36, 2.05, 0.16, 0.36, 2.85, 0.2, 1.2));
        m.add(Mesh.box(-0.44, 2.1, -0.3, 0.44, 2.9, -0.2, 1.1));
        m.add(Mesh.box(-0.44, 2.86, -0.28, 0.44, 3.0, 0.12, 1.3));
        return m.toArray(Mesh[]::new);
    }

    private static Shape segment(double half, double length) {
        return Shape.of(Mesh.box(-half, 0.0, -0.1, half, length, 0.1, 1.0),
                Mesh.cylinder(10, 0.09, -half - 0.01, half + 0.01, 1.15).alongX(),
                Mesh.box(-half * 0.8, length * 0.3, -0.13, half * 0.8, length * 0.8, -0.1, 1.3));
    }

    // The head round its middle, its face at +z, and under it the drill it screws into the neck with.
    private static Mesh[] head() {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.lathe(20, 1.0, 0.0, -0.45, 0.62, -0.45, 0.72, -0.2, 0.74, 0.15, 0.66, 0.42, 0.48, 0.62, 0.22, 0.72,
                0.0, 0.74));
        m.add(Mesh.box(-0.55, 0.02, 0.55, 0.55, 0.22, 0.76, 1.9));
        m.add(Mesh.box(-0.62, 0.22, 0.5, 0.62, 0.34, 0.78, 1.15));
        m.add(Mesh.box(-0.4, -0.42, 0.45, 0.4, -0.15, 0.72, 1.05));
        m.add(Mesh.box(-0.08, 0.5, -0.5, 0.08, 0.95, 0.55, 1.3));
        pair(m, Mesh.cylinder(12, 0.2, 0.68, 0.82, 1.2).alongX().moved(0.0, 0.05, -0.05));
        pair(m, bar(0.035, 1.4, new Vec3(0.78, 0.2, -0.1), new Vec3(0.85, 0.9, -0.3)));
        m.add(Mesh.cone(16, 0.0, 0.46, -1.15, -0.45, 1.05));
        for (int k = 0; k < 2; k++) {
            Vec3[] path = new Vec3[21];
            for (int i = 0; i <= 20; i++) {
                double u = i / 20.0;
                double y = -0.5 - 0.6 * u;
                double radius = 0.46 * (y + 1.15) / 0.7 + 0.03;
                double angle = k * Math.PI + u * Math.PI * 5.0;
                path[i] = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            }
            m.add(Mesh.tube(false, 5, 0.035, 1.45, path));
        }
        return m.toArray(Mesh[]::new);
    }
}
