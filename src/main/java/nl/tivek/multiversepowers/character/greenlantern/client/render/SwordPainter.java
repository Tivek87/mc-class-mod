package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

public final class SwordPainter {
    public static final double GRIP_X = 0.15;
    public static final double GRIP_Z = -0.045;
    private static final double[] BLADE = { 0.0, 1.0, -0.35, 0.72, -1.0, 0.25, -1.0, -0.25, -0.35, -0.72, 0.0, -1.0,
            0.35, -0.72, 1.0, -0.25, 1.0, 0.25, 0.35, 0.72 };
    public static final double TIP = 1.4;
    static final double BLADE_FROM = 0.2;
    private static final ConstructPainter.Shape SWORD = ConstructPainter.Shape.of(sword());
    private static final double FACE_FRONT = 0.03;
    private static final double BULGE = 0.07;
    private static final double RIM_THICK = 0.034;
    private static final Mesh[] BODY = body();
    private static final Mesh[] BOSS = boss();
    private static final Mesh[] BACK = back();
    private static final Mesh RIVET = Mesh.ball(8, 5, 0.019, 1.5);
    private static final Vec3[] RIVETS = rivets();
    private static final int RIM_TOP = 4;
    private static final Vec3[] RIM = rim();
    private static final double[] RIM_ALONG = along(RIM);
    private static final Mesh[] RIM_EDGES = edges(RIM);
    private static final ConstructPainter.Shape SHIELD = ConstructPainter.Shape.of(shield());
    private static final ConstructPainter.Shape SHIELD_BODY = ConstructPainter.Shape.of(BODY);
    private static final ConstructPainter.Shape SHIELD_BOSS = ConstructPainter.Shape.of(BOSS);
    private static final ConstructPainter.Shape SHIELD_BACK = ConstructPainter.Shape.of(BACK);
    private static final double BOSS_UNTIL = 0.2;
    private static final double SPREAD_FROM = 0.06;
    private static final double SPREAD_TOP = 0.56;
    private static final double SPREAD_UNTIL = 0.76;
    private static final double SPREAD_OVER = 1.06;
    private static final double RIM_FROM = 0.4;
    private static final double RIM_UNTIL = 0.88;
    private static final double BACK_FROM = 0.82;
    private static final double RIVET_POP = 0.15;
    private static final double CLANG_TICKS = 4.0;
    private static final double FLING = 1.3;

    private SwordPainter() {
    }

    private static Mesh[] sword() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.sweep(1.05, BLADE, new double[] { BLADE_FROM, 0.034, 0.1, 0.0 },
                new double[] { 0.32, 0.033, 0.098, 0.0 }, new double[] { 0.9, 0.028, 0.088, 0.0 },
                new double[] { 1.2, 0.022, 0.07, 0.0 }, new double[] { 1.33, 0.012, 0.036, 0.0 },
                new double[] { TIP, 0.0, 0.0, 0.0 }));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.box(side * 0.03, -0.014, 0.36, side * 0.038, 0.014, 1.12, 1.95));
            parts.add(Mesh.torus(14, 4, 0.042, 0.009, 1.9).alongX().moved(side * 0.034, 0.0, 0.27));
        }
        parts.add(Mesh.tube(false, 8, 0.034, 1.15, new Vec3(0.0, -0.31, 0.2), new Vec3(0.0, -0.22, 0.155),
                new Vec3(0.0, -0.1, 0.14), new Vec3(0.0, 0.1, 0.14), new Vec3(0.0, 0.22, 0.155),
                new Vec3(0.0, 0.31, 0.2)));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.ball(10, 6, 0.046, 1.4).moved(0.0, side * 0.325, 0.21));
        }
        parts.add(Mesh.box(-0.05, -0.07, 0.1, 0.05, 0.07, 0.2, 1.1));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.torus(16, 5, 0.052, 0.013, 1.85).alongX().moved(side * 0.052, 0.0, 0.15));
        }
        parts.add(Mesh.cylinder(10, 0.034, -0.14, 0.1, 0.92).alongZ());
        for (double z : new double[] { -0.1, -0.04, 0.02, 0.08 }) {
            parts.add(Mesh.torus(12, 4, 0.036, 0.008, 1.25).alongZ().moved(0.0, 0.0, z));
        }
        parts.add(Mesh.ball(14, 8, 0.062, 1.3).scaled(1.0, 1.0, 0.8).moved(0.0, 0.0, -0.19));
        parts.add(Mesh.ball(10, 6, 0.028, 2.0).moved(0.0, 0.0, -0.24));
        return parts.toArray(Mesh[]::new);
    }

    public static double faceOut(double x, double y) {
        double[] outline = outline(1.0);
        int n = outline.length / 2;
        double cx = 0.0;
        double cy = 0.0;
        for (int i = 0; i < n; i++) {
            cx += outline[2 * i] / n;
            cy += outline[2 * i + 1] / n;
        }
        double dx = x - cx;
        double dy = y - cy;
        double reach = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double ax = outline[2 * i] - cx;
            double ay = outline[2 * i + 1] - cy;
            double ex = outline[2 * j] - cx - ax;
            double ey = outline[2 * j + 1] - cy - ay;
            double across = dx * ey - dy * ex;
            if (Math.abs(across) < 1.0E-12) {
                continue;
            }
            double s = (ax * ey - ay * ex) / across;
            double u = (ax * dy - ay * dx) / across;
            if (s > 0.0 && u >= 0.0 && u <= 1.0) {
                reach = Math.min(reach, s);
            }
        }
        double f = reach == Double.MAX_VALUE ? 0.0 : Math.min(1.0, 1.0 / reach);
        return FACE_FRONT + BULGE * (1.0 - f * f);
    }

    private static double[] outline(double scale) {
        List<double[]> points = new ArrayList<>();
        int top = 8;
        for (int i = 0; i <= top; i++) {
            double x = -0.46 + 0.92 * i / top;
            points.add(new double[] { x, 0.52 + 0.04 * (1.0 - (x / 0.46) * (x / 0.46)) });
        }
        points.add(new double[] { 0.46, 0.26 });
        double[][] curve = { { 0.46, 0.04 }, { 0.44, -0.1 }, { 0.4, -0.23 }, { 0.33, -0.37 }, { 0.24, -0.5 },
                { 0.13, -0.61 }, { 0.0, -0.7 } };
        for (double[] point : curve) {
            points.add(point);
        }
        for (int i = curve.length - 2; i >= 0; i--) {
            points.add(new double[] { -curve[i][0], curve[i][1] });
        }
        points.add(new double[] { -0.46, 0.26 });
        double[] outline = new double[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            outline[2 * i] = points.get(i)[0] * scale;
            outline[2 * i + 1] = 0.02 + (points.get(i)[1] - 0.02) * scale;
        }
        return outline;
    }

    private static Mesh[] shield() {
        List<Mesh> parts = new ArrayList<>(List.of(BODY));
        parts.add(Mesh.tube(true, 6, RIM_THICK, 1.4, ring(outline(1.0), 0.0)));
        for (Vec3 rivet : RIVETS) {
            parts.add(RIVET.moved(rivet.x, rivet.y, rivet.z));
        }
        parts.addAll(List.of(BOSS));
        for (Mesh part : BACK) {
            parts.add(part.moved(GRIP_X, 0.0, 0.0));
        }
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] body() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.dish(5, -FACE_FRONT, FACE_FRONT, BULGE, 1.0, outline(1.0)));
        parts.add(Mesh.tube(true, 5, 0.013, 1.55, ring(outline(0.8), FACE_FRONT + BULGE * (1.0 - 0.64) + 0.006)));
        parts.add(Mesh.torus(28, 6, 0.2, 0.028, 1.75).alongZ().moved(0.0, 0.02, 0.095));
        parts.add(Mesh.box(-0.25, 0.235, 0.075, 0.25, 0.275, 0.105, 1.75));
        parts.add(Mesh.box(-0.25, -0.235, 0.075, 0.25, -0.195, 0.105, 1.75));
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI * 2.0 * (k + 0.5) / 8.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            parts.add(Mesh.tube(false, 4, 0.009, 1.9, new Vec3(cos * 0.13, 0.02 + sin * 0.13, 0.034),
                    new Vec3(cos * 0.36, 0.02 + sin * 0.4, 0.004)));
        }
        return parts.toArray(Mesh[]::new);
    }

    private static Mesh[] boss() {
        return new Mesh[] { Mesh.ball(14, 8, 0.1, 1.3).scaled(1.0, 1.0, 0.55).moved(0.0, 0.02, 0.095),
                Mesh.torus(20, 5, 0.09, 0.018, 1.7).alongZ().moved(0.0, 0.02, 0.035) };
    }

    private static Mesh[] back() {
        return new Mesh[] { Mesh.box(-0.3 - GRIP_X, -0.06, 0.008, 0.06 - GRIP_X, 0.06, 0.036, 0.95),
                Mesh.tube(false, 6, 0.016, 1.3, new Vec3(-0.2 - GRIP_X, 0.085, 0.027),
                        new Vec3(-0.2 - GRIP_X, 0.08, -0.05), new Vec3(-0.2 - GRIP_X, -0.08, -0.05),
                        new Vec3(-0.2 - GRIP_X, -0.085, 0.027)),
                Mesh.tube(false, 8, 0.02, 1.25, new Vec3(0.0, 0.105, 0.032), new Vec3(0.0, 0.09, GRIP_Z),
                        new Vec3(0.0, -0.09, GRIP_Z), new Vec3(0.0, -0.105, 0.032)) };
    }

    private static Vec3[] rivets() {
        double[] outline = outline(0.9);
        List<Vec3> rivets = new ArrayList<>();
        for (int i = 0; i < outline.length / 2; i += 2) {
            rivets.add(new Vec3(outline[2 * i], outline[2 * i + 1], FACE_FRONT + BULGE * (1.0 - 0.81) + 0.004));
        }
        return rivets.toArray(Vec3[]::new);
    }

    private static Vec3[] rim() {
        Vec3[] ring = ring(outline(1.0), 0.0);
        Vec3[] rim = new Vec3[ring.length + 1];
        for (int i = 0; i <= ring.length; i++) {
            rim[i] = ring[(RIM_TOP + i) % ring.length];
        }
        return rim;
    }

    private static double[] along(Vec3[] rim) {
        double[] along = new double[rim.length];
        for (int i = 1; i < rim.length; i++) {
            along[i] = along[i - 1] + rim[i].distanceTo(rim[i - 1]);
        }
        return along;
    }

    private static Mesh[] edges(Vec3[] rim) {
        Mesh[] edges = new Mesh[rim.length - 1];
        for (int i = 0; i < edges.length; i++) {
            edges[i] = Mesh.tube(false, 6, RIM_THICK, 1.4, rim[i], rim[i + 1]);
        }
        return edges;
    }

    private static Vec3[] ring(double[] outline, double z) {
        Vec3[] ring = new Vec3[outline.length / 2];
        for (int i = 0; i < ring.length; i++) {
            ring[i] = new Vec3(outline[2 * i], outline[2 * i + 1], z);
        }
        return ring;
    }

    public static void sword(LanternPainter painter, Vec3 grip, Vec3 forward, Vec3 edge, double scale, double grown,
            double apart) {
        if (grown <= 0.01) {
            return;
        }
        ConstructPainter.Frame whole = frame(grip, forward, edge, scale);
        double wide = Math.min(1.0, grown * 1.8);
        ConstructPainter.Frame frame = grown < 1.0 ? whole.stretched(wide, wide, grown) : whole;
        if (apart > 0.0) {
            painter.fling(FLING);
            painter.shattered(SWORD, frame, apart, 1.2);
            painter.fling(1.0);
            return;
        }
        painter.glare(0.75 * (1.0 - grown));
        painter.ambient(0.12);
        painter.shape(SWORD, frame, 1.0, 1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
        if (grown < 1.0) {
            painter.flare(grip, 0.35 * scale * (1.0 - grown), 1.0 - grown);
            painter.flare(grip.add(forward.normalize().scale(TIP * scale * grown)), 0.2 * scale, 1.0 - grown);
        }
    }

    public static void shield(LanternPainter painter, Vec3 center, Vec3 face, Vec3 up, double scale, double grown,
            double apart) {
        if (grown <= 0.0) {
            return;
        }
        ConstructPainter.Frame whole = frame(center, face, up, scale);
        double spread = spread(grown);
        ConstructPainter.Frame frame = grown < 1.0 ? whole.stretched(spread, spread, 1.0) : whole;
        double half = RIM_ALONG[RIM_ALONG.length - 1] / 2.0;
        double run = (grown - RIM_FROM) / (RIM_UNTIL - RIM_FROM);
        double head = half * Mth.clamp(run, 0.0, 1.0);
        if (apart > 0.0) {
            painter.fling(FLING);
            if (grown >= 1.0) {
                painter.shattered(SHIELD, frame, apart, 1.2);
            } else {
                formed(whole, frame, grown, run, (shape, at, seed) -> painter.shattered(shape, at, apart, 1.2, seed));
            }
            painter.fling(1.0);
            return;
        }
        painter.ambient(0.12);
        if (grown >= 1.0) {
            painter.shape(SHIELD, frame, 1.0, 1.0);
            painter.ambient(0.0);
            return;
        }
        painter.glare(0.75 * (1.0 - grown));
        formed(whole, frame, grown, run, (shape, at, seed) -> painter.shape(shape, at, 1.0, 1.0));
        painter.glare(0.0);
        painter.ambient(0.0);
        painter.flare(whole.at(0.0, 0.02, 0.1), 0.3 * scale * (1.0 - grown), Math.max(0.0, 1.0 - grown * 2.0));
        if (run > 0.0 && run < 1.0) {
            edgeLight(painter, frame, head, scale);
            edgeLight(painter, frame, 2.0 * half - head, scale);
        }
        if (run >= 1.0) {
            double flash = 1.0 - (grown - RIM_UNTIL) / (1.0 - RIM_UNTIL);
            painter.flare(frame.at(0.0, -0.7, 0.0), 0.25 * scale * (1.5 - flash), flash);
        }
    }

    @FunctionalInterface
    private interface Part {
        void at(ConstructPainter.Shape shape, ConstructPainter.Frame frame, int seed);
    }

    private static void formed(ConstructPainter.Frame whole, ConstructPainter.Frame frame, double grown, double run,
            Part part) {
        double boss = Ease.backOut(grown / BOSS_UNTIL);
        part.at(SHIELD_BOSS, whole.stretched(boss, boss, boss), 0);
        if (grown > SPREAD_FROM) {
            part.at(SHIELD_BODY, frame, 8);
        }
        double half = RIM_ALONG[RIM_ALONG.length - 1] / 2.0;
        double head = half * Mth.clamp(run, 0.0, 1.0);
        if (head > 0.0) {
            part.at(ConstructPainter.Shape.of(rim(head)), frame, 24);
        }
        for (int i = 0; i < RIVETS.length; i++) {
            double popped = Ease.backOut((half * run - rivetRound(i)) / RIVET_POP);
            if (popped > 0.0) {
                Vec3 at = RIVETS[i];
                part.at(ConstructPainter.Shape.of(RIVET), frame.moved(at.x, at.y, at.z).stretched(popped, popped,
                        popped), 60 + i);
            }
        }
        if (grown > BACK_FROM) {
            double closed = Ease.backOut((grown - BACK_FROM) / (1.0 - BACK_FROM));
            part.at(SHIELD_BACK, frame.moved(GRIP_X, 0.0, 0.0).stretched(closed, closed, closed), 90);
        }
    }

    private static double spread(double grown) {
        if (grown >= SPREAD_UNTIL) {
            return 1.0;
        }
        if (grown >= SPREAD_TOP) {
            return Ease.hermite(SPREAD_OVER, 0.0, 1.0, 0.0, (grown - SPREAD_TOP) / (SPREAD_UNTIL - SPREAD_TOP));
        }
        double u = Math.max(0.0, (grown - SPREAD_FROM) / (SPREAD_TOP - SPREAD_FROM));
        return Math.max(1.0E-3, Ease.hermite(0.12, 2.4, SPREAD_OVER, 0.0, u));
    }

    private static double rivetRound(int i) {
        double round = RIM_ALONG[(2 * i - RIM_TOP + RIM.length - 1) % (RIM.length - 1)];
        return Math.min(round, RIM_ALONG[RIM_ALONG.length - 1] - round);
    }

    private static Mesh[] rim(double head) {
        double length = RIM_ALONG[RIM_ALONG.length - 1];
        double tail = length - head;
        List<Mesh> pieces = new ArrayList<>();
        for (int i = 0; i < RIM_EDGES.length; i++) {
            double from = RIM_ALONG[i];
            double to = RIM_ALONG[i + 1];
            if (to <= head || from >= tail) {
                pieces.add(RIM_EDGES[i]);
            } else if (from < head) {
                pieces.add(Mesh.tube(false, 6, RIM_THICK, 1.4, RIM[i], rimAt(head)));
            } else if (to > tail) {
                pieces.add(Mesh.tube(false, 6, RIM_THICK, 1.4, rimAt(tail), RIM[i + 1]));
            }
        }
        return pieces.toArray(Mesh[]::new);
    }

    private static Vec3 rimAt(double round) {
        int i = 0;
        while (i + 2 < RIM.length && RIM_ALONG[i + 1] < round) {
            i++;
        }
        double stretch = Math.max(1.0E-6, RIM_ALONG[i + 1] - RIM_ALONG[i]);
        return RIM[i].lerp(RIM[i + 1], Mth.clamp((round - RIM_ALONG[i]) / stretch, 0.0, 1.0));
    }

    private static void edgeLight(LanternPainter painter, ConstructPainter.Frame frame, double round, double scale) {
        double length = RIM_ALONG[RIM_ALONG.length - 1];
        double back = round <= length / 2.0 ? -1.0 : 1.0;
        Vec3 head = rimAt(round);
        Vec3 at = frame.at(head.x, head.y, head.z);
        painter.flare(at, 0.12 * scale, 1.0);
        Vec3 last = at;
        for (int k = 1; k <= 4; k++) {
            Vec3 point = rimAt(Mth.clamp(round + back * 0.07 * k, 0.0, length));
            Vec3 next = frame.at(point.x, point.y, point.z);
            painter.edge(last, next, 0.03 * scale, 1.0 - 0.22 * k);
            last = next;
        }
    }

    public static void clang(LanternPainter painter, Vec3 at, Vec3 along, Vec3 off, double since, double scale) {
        if (since < 0.0 || since > CLANG_TICKS) {
            return;
        }
        double u = since / CLANG_TICKS;
        double fade = (1.0 - u) * (1.0 - u);
        painter.flare(at, 0.2 * scale * (1.0 + u), fade);
        for (int k = 0; k < 7; k++) {
            double angle = (k / 6.0 - 0.5) * 2.4;
            Vec3 way = along.scale(Math.sin(angle)).add(off.scale(Math.cos(angle))).normalize();
            double reach = scale * (0.08 + 0.3 * Math.sqrt(u)) * (0.7 + 0.075 * ((k * 3) % 5));
            painter.edge(at.add(way.scale(reach * 0.5)), at.add(way.scale(reach)), 0.014 * scale, fade);
        }
    }

    public static void trail(LanternPainter painter, List<Vec3> tips, List<Vec3> roots, double strength) {
        int n = Math.min(tips.size(), roots.size());
        for (int i = 0; i + 1 < n; i++) {
            double a = strength * (1.0 - (double) i / (n - 1));
            double b = strength * (1.0 - (double) (i + 1) / (n - 1));
            painter.sheet(roots.get(i), tips.get(i), tips.get(i + 1), roots.get(i + 1), 0.15 * a, a, b, 0.15 * b);
            painter.edge(tips.get(i), tips.get(i + 1), 0.045 * (0.4 + 0.6 * a / Math.max(strength, 1.0E-3)), 0.9 * a);
        }
    }

    private static ConstructPainter.Frame frame(Vec3 center, Vec3 forward, Vec3 up, double scale) {
        Vec3 ahead = forward.normalize();
        Vec3 top = up.subtract(ahead.scale(up.dot(ahead)));
        top = top.lengthSqr() < 1.0E-8 ? ConstructPainter.Frame.of(center, ahead, Vectors.UP, 1.0).up()
                : top.normalize();
        return new ConstructPainter.Frame(center, ahead.cross(top).normalize(), top, ahead, scale);
    }
}
