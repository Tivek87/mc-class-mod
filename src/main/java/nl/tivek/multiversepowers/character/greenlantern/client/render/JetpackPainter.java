package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Ease;

// Sizes in blocks on a frame at the middle of the back: x across, y up the spine, z out of the back.
public final class JetpackPainter {
    public static final float FLIES = 6.0F;
    public static final float SPARK = 15.0F;
    public static final float LIT = 16.0F;
    public static final float BREAK_TICKS = 8.0F;
    private static final double TANK_X = 0.105;
    private static final double TANK_Z = 0.115;
    private static final double EXIT = -0.31;
    private static final double FLING = 1.3;
    private static final int TRAIL = 7;

    private static final int PLATE = 0;
    private static final int TANK = 1;
    private static final int NOZZLE = 3;
    private static final int FIN = 5;
    private static final int STRAP = 7;
    private static final Vec3[] ANCHORS = { new Vec3(0.0, 0.0, 0.02), new Vec3(TANK_X, -0.04, TANK_Z),
            new Vec3(-TANK_X, -0.04, TANK_Z), new Vec3(TANK_X, -0.2, TANK_Z), new Vec3(-TANK_X, -0.2, TANK_Z),
            new Vec3(0.17, -0.06, TANK_Z), new Vec3(-0.17, -0.06, TANK_Z), new Vec3(0.0, 0.115, 0.0) };
    // Tick each part starts forming after the shot and how long it takes: plate, tanks, nozzles, fins, strap.
    private static final float[][] GROWS = { { 6.0F, 4.0F }, { 8.0F, 5.0F }, { 8.6F, 5.0F }, { 11.0F, 3.0F },
            { 11.5F, 3.0F }, { 12.0F, 3.0F }, { 12.4F, 3.0F }, { 12.5F, 4.0F } };
    private static final ConstructPainter.Shape[] PARTS = parts();

    private JetpackPainter() {
    }

    private static ConstructPainter.Shape[] parts() {
        Mesh[][] groups = new Mesh[STRAP + 1][];
        groups[PLATE] = new Mesh[] { Mesh.box(-0.165, -0.24, 0.0, 0.165, 0.2, 0.04, 1.0),
                Mesh.box(-0.03, -0.19, 0.04, 0.03, 0.17, 0.07, 1.3),
                Mesh.box(-0.13, 0.12, 0.04, 0.13, 0.16, 0.1, 1.2),
                Mesh.torus(12, 4, 0.022, 0.006, 1.9).alongZ().moved(0.0, 0.14, 0.1) };
        for (int side = 0; side < 2; side++) {
            double x = side == 0 ? TANK_X : -TANK_X;
            groups[TANK + side] = new Mesh[] { Mesh.cylinder(16, 0.068, -0.2, 0.12, 1.0).moved(x, 0.0, TANK_Z),
                    Mesh.ball(16, 8, 0.068, 1.0).scaled(1.0, 0.8, 1.0).moved(x, 0.12, TANK_Z),
                    Mesh.torus(16, 4, 0.07, 0.009, 1.8).moved(x, -0.19, TANK_Z),
                    Mesh.torus(16, 4, 0.07, 0.008, 1.8).moved(x, 0.02, TANK_Z),
                    Mesh.torus(16, 4, 0.07, 0.008, 1.8).moved(x, 0.1, TANK_Z) };
            groups[NOZZLE + side] = new Mesh[] {
                    Mesh.lathe(16, 1.15, 0.0, EXIT, 0.072, EXIT, 0.068, -0.295, 0.054, -0.26, 0.042, -0.225, 0.05,
                            -0.2, 0.0, -0.2).moved(x, 0.0, TANK_Z),
                    Mesh.torus(16, 4, 0.07, 0.007, 2.0).moved(x, EXIT + 0.005, TANK_Z) };
            Mesh fin = Mesh.prism(TANK_Z - 0.015, TANK_Z + 0.015, 1.4, 0.168, -0.16, 0.235, -0.215, 0.235, -0.1,
                    0.168, 0.04);
            groups[FIN + side] = new Mesh[] { side == 0 ? fin : fin.mirrored() };
        }
        Mesh band = Mesh.box(0.165, 0.09, 0.0, 0.285, 0.14, 0.02, 1.2);
        Mesh flank = Mesh.box(0.268, 0.09, -0.3, 0.285, 0.14, 0.0, 1.2);
        groups[STRAP] = new Mesh[] { band, band.mirrored(), flank, flank.mirrored(),
                Mesh.box(-0.27, 0.09, -0.305, 0.27, 0.14, -0.285, 1.2),
                Mesh.torus(14, 4, 0.032, 0.008, 1.9).alongZ().moved(0.0, 0.115, -0.31),
                Mesh.cylinder(12, 0.02, -0.004, 0.004, 1.6).alongZ().moved(0.0, 0.115, -0.31) };
        ConstructPainter.Shape[] parts = new ConstructPainter.Shape[groups.length];
        for (int p = 0; p < groups.length; p++) {
            Vec3 anchor = ANCHORS[p];
            Mesh[] moved = new Mesh[groups[p].length];
            for (int m = 0; m < moved.length; m++) {
                moved[m] = groups[p][m].moved(-anchor.x, -anchor.y, -anchor.z);
            }
            parts[p] = ConstructPainter.Shape.of(moved);
        }
        return parts;
    }

    // The ring's ball of light, from the ring round in a bend onto the back.
    public static void ball(LanternPainter painter, Vec3 ring, Vec3 back, Vec3 out, double t) {
        if (t < FLIES) {
            Vec3 bend = ring.lerp(back, 0.5).add(out.scale(0.9));
            Vec3 at = along(ring, bend, back, t / FLIES);
            double near = near(painter, at);
            Vec3 last = at;
            for (int k = 1; k <= TRAIL; k++) {
                Vec3 next = along(ring, bend, back, Math.max(0.0, t - 0.35 * k) / FLIES);
                double fade = 1.0 - (double) k / (TRAIL + 1);
                painter.edge(last, next, 0.09 * fade, 0.9 * fade * near);
                last = next;
            }
            painter.flare(at, 0.34, near);
            painter.flare(at, 0.12, near);
            if (t < 2.0) {
                painter.flare(ring, 0.3 * (1.0 - t / 2.0), (1.0 - t / 2.0) * near(painter, ring));
            }
            return;
        }
        double hit = t - FLIES;
        if (hit < 3.0) {
            painter.flare(back, 0.7 * (1.0 - hit / 3.0), (1.0 - hit / 3.0) * near(painter, back));
        }
    }

    private static Vec3 along(Vec3 from, Vec3 bend, Vec3 to, double u) {
        double s = Ease.smooth(u);
        return from.lerp(bend, s).lerp(bend.lerp(to, s), s);
    }

    // A light right at the eye would fill the whole view: it thins out as the eye comes close.
    private static double near(LanternPainter painter, Vec3 at) {
        return Ease.smooth((painter.camera().distanceTo(at) - 0.4) / 0.8);
    }

    public static void pack(LanternPainter painter, ConstructPainter.Frame back, double t, double apart) {
        boolean forming = t < GROWS[STRAP][0] + GROWS[STRAP][1] + 4.0;
        for (int p = 0; p < PARTS.length; p++) {
            double grown = forming ? grown(p, t) : 1.0;
            if (grown <= 0.01) {
                continue;
            }
            ConstructPainter.Frame at = part(back, p, grown);
            if (apart > 0.0) {
                painter.fling(FLING);
                painter.shattered(PARTS[p], at, apart, 1.2, 40 + 7 * p);
                painter.fling(1.0);
                continue;
            }
            double fresh = forming ? 1.0 - Ease.smooth((t - GROWS[p][0] - GROWS[p][1]) / 3.0) : 0.0;
            painter.glare(0.75 * Math.max(fresh, 0.0));
            painter.ambient(0.12);
            painter.shape(PARTS[p], at, 1.0, 1.0);
            painter.ambient(0.0);
            painter.glare(0.0);
            if (forming && grown < 1.0) {
                painter.flare(back.at(ANCHORS[p].x, ANCHORS[p].y, ANCHORS[p].z), 0.08 * back.scale(),
                        0.8 * (1.0 - grown));
            }
        }
    }

    private static double grown(int part, double t) {
        double u = (t - GROWS[part][0]) / GROWS[part][1];
        return part == PLATE || part == TANK || part == TANK + 1 || part == STRAP ? Ease.smooth(u) : Ease.backOut(u);
    }

    private static ConstructPainter.Frame part(ConstructPainter.Frame back, int p, double grown) {
        Vec3 anchor = ANCHORS[p];
        ConstructPainter.Frame at = back.moved(anchor.x, anchor.y, anchor.z);
        if (grown >= 1.0) {
            return at;
        }
        double g = Math.max(1.0E-3, grown);
        if (p == PLATE) {
            double wide = Math.max(1.0E-3, Ease.backOut(Math.min(1.0, grown * 1.4)));
            return at.stretched(wide, g, g);
        }
        if (p == TANK || p == TANK + 1) {
            double round = Math.max(1.0E-3, Ease.backOut(Math.min(1.0, grown * 1.6)));
            return at.stretched(round, g, round);
        }
        if (p == STRAP) {
            return at.stretched(g, 1.0, g);
        }
        return at.stretched(g, g, g);
    }

    // Plasma out of both nozzles, down along the spine: long and roaring at speed, a small flame hovering.
    public static void flames(LanternPainter painter, ConstructPainter.Frame back, double t, double thrust) {
        double s = back.scale();
        Vec3 down = back.up().normalize().scale(-1.0);
        Vec3 out = back.forward().normalize();
        for (int side = 0; side < 2; side++) {
            Vec3 exit = back.at(side == 0 ? TANK_X : -TANK_X, EXIT, TANK_Z);
            if (t >= SPARK - 0.5 && t < LIT + 2.0) {
                double flash = 1.0 - Math.abs(t - LIT) / 2.5;
                FirePainter.spark(painter, exit, down, 0.05 * s, Math.max(0.0, flash), 17 * side + (int) (t * 3.0));
            }
            double on = Ease.smooth((t - LIT) / 4.0);
            if (on <= 0.0) {
                continue;
            }
            double power = on * (0.25 + 0.75 * thrust);
            painter.exhaust(exit, down, (0.3 + 1.6 * thrust) * on * s, 0.045 * s, power);
            double time = painter.time();
            FirePainter.tongue(painter, exit, down.add(out.scale(0.08)).normalize(),
                    (0.22 + 0.85 * thrust) * on * s, 0.075 * s, 0.85 * on, 31 + side, time * 1.5);
            FirePainter.tongue(painter, exit, down.subtract(out.scale(0.08)).normalize(),
                    (0.16 + 0.6 * thrust) * on * s, 0.06 * s, 0.7 * on, 57 + side, time * 1.7 + 2.0);
            painter.glowDisc(exit, 0.085 * s, FirePainter.FLAME, 0.7 * on, 0.2, (int) (time * 4.0) + side);
        }
    }
}
