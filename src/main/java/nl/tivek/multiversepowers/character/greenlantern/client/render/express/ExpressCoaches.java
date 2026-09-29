package nl.tivek.multiversepowers.character.greenlantern.client.render.express;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;

// The Express's coaches, in blocks at scale 1, x right, y up from the rails, z forward, round the middle between their
// bogies: a wooden western coach of hard light with a row of windows, a raised clerestory roof and an open platform at
// each end; the last one an observation car with a railed rear platform, a round drumhead and marker lamps.
final class ExpressCoaches {
    static final double BODY = 5.7;
    static final double END = ExpressScript.COACH_LENGTH * 0.5;
    static final double[] AXLES = { ExpressScript.COACH_BOGIE + 0.7, ExpressScript.COACH_BOGIE - 0.7,
            -ExpressScript.COACH_BOGIE + 0.7, -ExpressScript.COACH_BOGIE - 0.7 };
    static final double WINDOW_Y = 2.6;
    static final double[] WINDOWS = windows();
    static final Vec3 TAIL_LAMP = new Vec3(0.95, 2.4, -END - 0.05);
    static final Vec3 DRUMHEAD = new Vec3(0.0, 2.2, -END - 0.1);
    private static final double SIDE = 1.36;
    private static final double FLOOR = 1.24;
    private static final double EAVES = 3.35;

    static final ConstructPainter.Shape COACH = new ConstructPainter.Shape(boxes(false), meshes(false));
    static final ConstructPainter.Shape OBSERVATION = new ConstructPainter.Shape(boxes(true), meshes(true));
    static final ConstructPainter.Shape COUPLER = ConstructPainter.Shape.of(Mesh.merged(
            Mesh.box(-0.1, -0.08, 0.0, 0.1, 0.08, 1.0, 1.1), Mesh.box(-0.18, -0.14, 0.42, 0.18, 0.14, 0.58, 1.3)));

    private ExpressCoaches() {
    }

    private static double[] windows() {
        double[] z = new double[8];
        for (int i = 0; i < z.length; i++) {
            z[i] = -4.8 + i * 1.371;
        }
        return z;
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

    private static double[][] boxes(boolean observation) {
        List<double[]> b = new ArrayList<>();
        for (double z : new double[] { ExpressScript.COACH_BOGIE, -ExpressScript.COACH_BOGIE }) {
            pair(b, 0.88, 0.3, z - 0.95, 1.0, 0.78, z + 0.95, 0.9);
            box(b, -1.0, 0.72, z - 0.16, 1.0, 1.02, z + 0.16, 0.9);
            pair(b, 0.86, 0.34, z - 0.82, 1.04, 0.62, z - 0.58, 1.0);
            pair(b, 0.86, 0.34, z + 0.58, 1.04, 0.62, z + 0.82, 1.0);
            pair(b, 1.0, 0.45, z - 0.3, 1.1, 0.72, z + 0.3, 1.15);
        }
        box(b, -1.32, 1.02, -END, 1.32, FLOOR, END, 0.95);
        pair(b, 1.32, 1.02, -END, 1.4, 1.1, END, 1.25);
        // Sides: a panelled wall under the windows, the posts between them, the letter board over them.
        pair(b, SIDE - 0.1, FLOOR, -BODY, SIDE, 2.1, BODY, 1.0);
        pair(b, SIDE - 0.02, 2.08, -BODY - 0.02, SIDE + 0.05, 2.16, BODY + 0.02, 1.3);
        for (int i = 0; i <= WINDOWS.length; i++) {
            double from = i == 0 ? -BODY : WINDOWS[i - 1] + 0.43;
            double to = i == WINDOWS.length ? BODY : WINDOWS[i] - 0.43;
            pair(b, SIDE - 0.1, 2.16, from, SIDE, 3.02, to, 1.05);
        }
        for (double z : WINDOWS) {
            pair(b, SIDE - 0.16, 2.16, z - 0.43, SIDE - 0.1, 3.02, z + 0.43, 0.62);
            pair(b, SIDE - 0.02, 3.0, z - 0.46, SIDE + 0.03, 3.06, z + 0.46, 1.25);
            pair(b, SIDE - 0.02, 2.14, z - 0.46, SIDE + 0.03, 2.2, z + 0.46, 1.2);
        }
        pair(b, SIDE - 0.1, 3.02, -BODY, SIDE, EAVES, BODY, 1.0);
        pair(b, SIDE - 0.02, EAVES - 0.06, -BODY - 0.05, SIDE + 0.06, EAVES + 0.02, BODY + 0.05, 1.3);
        for (double z = -BODY + 0.9; z < BODY - 0.5; z += 1.371) {
            pair(b, SIDE - 0.02, FLOOR + 0.12, z - 0.5, SIDE + 0.02, 1.98, z + 0.5, 1.12);
        }
        // Clerestory: a raised strip down the roof with a row of small lights along each side.
        box(b, -0.6, EAVES + 0.38, -BODY + 0.6, 0.6, EAVES + 0.7, BODY - 0.6, 1.0);
        box(b, -0.66, EAVES + 0.68, -BODY + 0.5, 0.66, EAVES + 0.76, BODY - 0.5, 1.2);
        for (double z = -BODY + 1.0; z < BODY - 0.8; z += 0.7) {
            pair(b, 0.58, EAVES + 0.46, z - 0.2, 0.62, EAVES + 0.62, z + 0.2, 0.7);
        }
        // Ends: a wall with a door, and platforms with steps and railings.
        for (int end = -1; end <= 1; end += 2) {
            boolean rear = end < 0;
            double wall = end * BODY;
            box(b, -SIDE, FLOOR, Math.min(wall, wall + end * 0.1), SIDE, EAVES, Math.max(wall, wall + end * 0.1),
                    1.0);
            box(b, -0.38, FLOOR, end > 0 ? wall + 0.1 : wall - 0.14, 0.38, 3.0, end > 0 ? wall + 0.14 : wall - 0.1,
                    0.8);
            box(b, -0.44, 3.0, end > 0 ? wall + 0.1 : wall - 0.16, 0.44, 3.08, end > 0 ? wall + 0.16 : wall - 0.1,
                    1.3);
            pair(b, 0.9, 2.4, end > 0 ? wall + 0.1 : wall - 0.13, 1.18, 2.9, end > 0 ? wall + 0.13 : wall - 0.1,
                    0.65);
            box(b, -1.25, 1.02, Math.min(wall, END * end), 1.25, 1.16, Math.max(wall, END * end), 0.95);
            double outer = END * end;
            pair(b, 1.2, 0.5, outer - (end > 0 ? 0.45 : -0.05), 1.3, 1.02, outer + (end > 0 ? 0.05 : 0.45), 1.05);
            pair(b, 1.2, 0.55, outer - (end > 0 ? 0.4 : -0.05), 1.34, 0.61, outer + (end > 0 ? 0.05 : 0.4), 1.2);
            if (!(observation && rear)) {
                pair(b, 1.18, 1.16, outer - 0.03, 1.24, 2.2, outer + 0.03, 1.2);
            }
        }
        if (observation) {
            box(b, -1.28, 2.1, -END - 0.04, 1.28, 2.16, -END + 0.04, 1.3);
            pair(b, 1.22, 1.16, -END - 0.04, 1.28, 2.16, -BODY - 0.1, 1.25);
            for (double x = -1.0; x <= 1.01; x += 0.25) {
                box(b, x - 0.02, 1.16, -END - 0.03, x + 0.02, 2.1, -END + 0.03, 1.15);
            }
            pair(b, 0.85, 2.2, -END - 0.1, 1.05, 2.6, -END + 0.1, 1.2);
        }
        return b.toArray(double[][]::new);
    }

    private static Mesh[] meshes(boolean observation) {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.panel(14, 1.02, Math.toRadians(4.0), Math.toRadians(176.0), 0.08,
                new double[] { -BODY - 0.25, SIDE + 0.08, 0.42, EAVES }, new double[] { BODY + 0.25, SIDE + 0.08, 0.42,
                        EAVES }));
        for (double x : new double[] { -0.9, 0.9 }) {
            m.add(Mesh.tube(false, 5, 0.03, 1.2, new Vec3(x, 0.98, 3.2), new Vec3(x, 0.58, 2.2),
                    new Vec3(x, 0.58, -2.2), new Vec3(x, 0.98, -3.2)));
        }
        m.add(ExpressShapes.sideEmblem(0.42).moved(SIDE + 0.03, 2.6, 0.0));
        m.add(ExpressShapes.sideEmblem(0.42).mirrored().moved(-SIDE - 0.03, 2.6, 0.0));
        for (int end = -1; end <= 1; end += 2) {
            m.add(Mesh.cylinder(8, 0.07, 0.0, 0.18, 1.2).moved(end > 0 ? 1.05 : -1.05, EAVES + 0.02, end * BODY));
        }
        if (observation) {
            m.add(Mesh.cylinder(24, 0.46, -0.06, 0.06, 1.05).alongZ().moved(DRUMHEAD.x, DRUMHEAD.y, DRUMHEAD.z));
            m.add(Mesh.torus(24, 5, 0.47, 0.04, 1.35).alongZ().moved(DRUMHEAD.x, DRUMHEAD.y, DRUMHEAD.z - 0.06));
            m.add(ExpressShapes.emblem(0.3).turned(1.0, 0.0, 0.0, 90.0).turned(0.0, 1.0, 0.0, 180.0)
                    .moved(DRUMHEAD.x, DRUMHEAD.y, DRUMHEAD.z - 0.08));
            m.add(Mesh.tube(false, 5, 0.035, 1.25, new Vec3(-1.25, 2.13, -END + 0.1), new Vec3(-1.05, 2.13,
                    -END - 0.2), new Vec3(1.05, 2.13, -END - 0.2), new Vec3(1.25, 2.13, -END + 0.1)));
        }
        return m.toArray(Mesh[]::new);
    }
}
