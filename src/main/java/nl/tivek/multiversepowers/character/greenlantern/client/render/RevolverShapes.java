package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.RevolverDuo;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;

final class RevolverShapes {
    static final Vec3 HAMMER_PIVOT = new Vec3(0.0, 1.2, -0.85);
    static final Vec3[] CENTERS = { new Vec3(0.0, 0.7, 0.3), new Vec3(0.0, -1.9, -1.5), new Vec3(0.0, 0.95, 4.8),
            new Vec3(0.0, 0.75, 0.6), new Vec3(0.0, 1.5, -1.0), new Vec3(0.0, -0.6, 0.3) };
    static final ConstructPainter.Shape HAT = ConstructPainter.Shape.of(hat());
    static final ConstructPainter.Shape[] PARTS = new ConstructPainter.Shape[RevolverDuo.PARTS];
    static final ConstructPainter.Shape BULLET = ConstructPainter.Shape.of(
            Mesh.lathe(10, 1.5, 0.0, 0.0, 0.27, 0.0, 0.27, 0.6, 0.23, 0.7, 0.13, 0.98, 0.0, 1.08),
            Mesh.torus(10, 4, 0.27, 0.05, 1.9));

    static {
        PARTS[RevolverDuo.FRAME] = ConstructPainter.Shape.of(frame());
        PARTS[RevolverDuo.GRIP] = ConstructPainter.Shape.of(grip());
        PARTS[RevolverDuo.BARREL] = ConstructPainter.Shape.of(barrel());
        PARTS[RevolverDuo.CYLINDER] = ConstructPainter.Shape.of(cylinder());
        PARTS[RevolverDuo.HAMMER] = ConstructPainter.Shape.of(hammer());
        PARTS[RevolverDuo.GUARD] = ConstructPainter.Shape.of(guard());
    }

    private RevolverShapes() {
    }

    private static Mesh[] hat() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(24, 1.0, 0.0, -0.08, 2.5, -0.04, 2.78, 0.2, 2.66, 0.3, 2.4, 0.12, 1.25, 0.06, 0.0, 0.06)
                .scaled(0.86, 1.0, 1.0));
        parts.add(Mesh.lathe(20, 1.0, 0.0, 0.05, 1.3, 0.05, 1.28, 0.9, 1.2, 1.7, 1.05, 2.15, 0.6, 2.3, 0.35, 2.12, 0.0,
                2.18).scaled(0.85, 1.0, 1.0));
        parts.add(Mesh.torus(24, 5, 1.31, 0.12, 1.6).scaled(0.85, 1.0, 1.0).moved(0.0, 0.35, 0.0));
        double[] star = new double[20];
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * 2.0 * i / 10.0 + Math.PI * 0.5;
            double r = i % 2 == 0 ? 0.42 : 0.18;
            star[2 * i] = Math.cos(angle) * r;
            star[2 * i + 1] = Math.sin(angle) * r;
        }
        parts.add(Mesh.prism(0.0, 0.1, 1.9, star).moved(0.0, 0.62, 1.29));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] frame() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.box(-0.45, -0.45, -1.15, 0.45, 1.85, -0.45, 1.0));
        parts.add(Mesh.box(-0.3, 1.62, -0.5, 0.3, 1.9, 1.75, 1.0));
        parts.add(Mesh.box(-0.42, -0.45, -0.5, 0.42, -0.12, 1.75, 1.0));
        parts.add(Mesh.box(-0.42, -0.45, 1.6, 0.42, 1.9, 2.15, 1.0));
        parts.add(Mesh.box(-0.32, -0.9, -1.45, 0.32, -0.2, -0.75, 1.0));
        parts.add(Mesh.box(-0.08, 1.9, -0.6, 0.08, 2.02, -0.3, 1.8));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.ball(8, 5, 0.08, 1.8).moved(side * 0.46, 0.2, -0.8));
            parts.add(Mesh.ball(8, 5, 0.08, 1.8).moved(side * 0.46, 1.4, -0.8));
            parts.add(Mesh.box(side * 0.43, 1.55, -0.5, side * 0.47, 1.62, 1.75, 1.7));
        }
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] grip() {
        double[] outline = { 0.45, -0.12, 0.6, -0.9, 0.95, -2.2, 1.25, -3.15, 1.7, -3.4, 2.2, -3.3, 2.45, -2.95, 2.1,
                -2.2, 1.65, -1.2, 1.4, -0.6, 1.35, -0.2 };
        double[] panel = shrunk(outline, 0.78);
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.prism(-0.42, 0.42, 1.0, outline).turned(0.0, 1.0, 0.0, 90.0));
        parts.add(Mesh.prism(0.42, 0.5, 1.25, panel).turned(0.0, 1.0, 0.0, 90.0));
        parts.add(Mesh.prism(-0.5, -0.42, 1.25, panel).turned(0.0, 1.0, 0.0, 90.0));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.torus(18, 5, 0.34, 0.06, 1.9).alongX().moved(side * 0.52, -1.95, -1.4));
            parts.add(Mesh.box(side * 0.5, -2.0, -1.62, side * 0.56, -1.9, -1.18, 1.9));
            parts.add(Mesh.box(side * 0.5, -1.82, -1.62, side * 0.56, -1.72, -1.18, 1.9));
        }
        parts.add(Mesh.box(-0.3, -3.5, -2.0, 0.3, -3.3, -1.5, 1.7));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] barrel() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(8, 1.0, 0.0, 0.0, 0.37, 0.0, 0.37, 2.2, 0.34, 2.4, 0.34, 5.5, 0.0, 5.5)
                .turned(0.0, 1.0, 0.0, 22.5).alongZ().moved(0.0, 0.95, 2.1));
        parts.add(Mesh.torus(16, 5, 0.34, 0.08, 1.8).alongZ().moved(0.0, 0.95, 7.55));
        parts.add(Mesh.box(-0.14, 1.2, 2.1, 0.14, 1.36, 7.4, 1.2));
        parts.add(Mesh.box(-0.07, 1.3, 7.1, 0.07, 1.62, 7.45, 1.8));
        parts.add(Mesh.lathe(10, 1.0, 0.0, 0.0, 0.18, 0.0, 0.18, 3.5, 0.0, 3.5).alongZ().moved(0.0, 0.45, 2.1));
        parts.add(Mesh.ball(8, 5, 0.22, 1.5).moved(0.0, 0.45, 5.65));
        parts.add(Mesh.torus(16, 5, 0.36, 0.07, 1.7).alongZ().moved(0.0, 0.95, 2.3));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] cylinder() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(18, 1.0, 0.0, 0.0, 0.85, 0.0, 0.95, 0.12, 0.95, 1.88, 0.85, 2.0, 0.0, 2.0).alongZ()
                .moved(0.0, 0.75, -0.4));
        for (int k = 0; k < 6; k++) {
            double angle = Math.PI / 3.0 * k;
            double between = angle + Math.PI / 6.0;
            parts.add(Mesh.torus(12, 4, 0.2, 0.05, 1.9).alongZ()
                    .moved(0.55 * Math.sin(angle), 0.75 + 0.55 * Math.cos(angle), 1.6));
            parts.add(Mesh.box(-0.05, 0.9, 0.2, 0.05, 0.99, 1.4, 1.6).turned(0.0, 0.0, 1.0, -Math.toDegrees(between))
                    .moved(0.0, 0.75, 0.0));
        }
        parts.add(Mesh.torus(12, 4, 0.14, 0.05, 1.8).alongZ().moved(0.0, 0.75, 1.62));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] hammer() {
        double[] outline = { 0.95, 1.0, 1.05, 1.6, 1.45, 2.2, 1.25, 2.3, 0.75, 1.8, 0.55, 1.25, 0.7, 0.95 };
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.prism(-0.14, 0.14, 1.0, outline).turned(0.0, 1.0, 0.0, 90.0));
        parts.add(Mesh.box(-0.16, 2.1, -1.45, 0.16, 2.26, -1.2, 1.9));
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] guard() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.tube(false, 6, 0.1, 1.2, new Vec3(0.0, -0.12, 0.95), new Vec3(0.0, -0.55, 1.05),
                new Vec3(0.0, -0.95, 0.8), new Vec3(0.0, -1.1, 0.3), new Vec3(0.0, -0.95, -0.2),
                new Vec3(0.0, -0.6, -0.45), new Vec3(0.0, -0.35, -0.5)));
        parts.add(Mesh.tube(false, 5, 0.075, 1.5, new Vec3(0.0, -0.12, 0.35), new Vec3(0.0, -0.45, 0.4),
                new Vec3(0.0, -0.75, 0.28), new Vec3(0.0, -0.85, 0.1)));
        return parts.toArray(Mesh[]::new);
    }

    private static double[] shrunk(double[] outline, double share) {
        double cx = 0.0;
        double cy = 0.0;
        int n = outline.length / 2;
        for (int i = 0; i < n; i++) {
            cx += outline[2 * i] / n;
            cy += outline[2 * i + 1] / n;
        }
        double[] out = new double[outline.length];
        for (int i = 0; i < n; i++) {
            out[2 * i] = cx + (outline[2 * i] - cx) * share;
            out[2 * i + 1] = cy + (outline[2 * i + 1] - cy) * share;
        }
        return out;
    }
}
