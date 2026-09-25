package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;

public final class FlamePainter {
    public static final Vec3 GRIP = new Vec3(0.0, -0.155, -0.232);
    public static final Vec3 FORE = new Vec3(0.0, -0.245, 0.22);
    public static final Vec3 NOZZLE = new Vec3(0.0, 0.02, 0.63);
    public static final Vec3 PILOT = new Vec3(0.0, -0.035, 0.62);
    public static final Vec3 VALVE = new Vec3(0.0, -0.115, -0.15);
    public static final Vec3 TANK = new Vec3(0.0, -0.115, 0.1);
    public static final float GROWN_BY = 17.0F;

    private static final int GRIP_PART = 0;
    private static final int BODY_PART = 1;
    private static final int BARREL_PART = 2;
    private static final int FIN_PART = 3;
    private static final int NOZZLE_PART = FIN_PART + GunShapes.FLAME_FINS;
    private static final int PILOT_PART = NOZZLE_PART + 1;
    private static final int TANK_PART = PILOT_PART + 1;
    private static final int HOSE_PART = TANK_PART + 1;
    private static final int FORE_PART = HOSE_PART + 1;
    private static final int VALVE_PART = FORE_PART + 1;
    private static final double FIN_RADIUS = 0.056;
    private static final double FLING = 1.3;

    private static final Vec3[] ANCHORS = anchors();
    // Tick each part starts forming and how long it takes; the fins pop one after the other.
    private static final float[][] GROWS = grows();
    private static final ConstructPainter.Shape[] PARTS = parts();

    public record Glow(double fill, double heat, double pilot, double spark, double muzzle, double valve) {
        public static final Glow READY = new Glow(1.0, 0.0, 1.0, 0.0, 0.0, 0.0);
    }

    private FlamePainter() {
    }

    private static Vec3[] anchors() {
        Vec3[] anchors = new Vec3[VALVE_PART + 1];
        anchors[GRIP_PART] = GRIP;
        anchors[BODY_PART] = new Vec3(0.0, 0.0, -0.2);
        anchors[BARREL_PART] = new Vec3(0.0, 0.02, 0.05);
        for (int k = 0; k < GunShapes.FLAME_FINS; k++) {
            anchors[FIN_PART + k] = new Vec3(0.0, 0.02, 0.106 + 0.045 * k);
        }
        anchors[NOZZLE_PART] = new Vec3(0.0, 0.02, 0.46);
        anchors[PILOT_PART] = new Vec3(0.0, -0.035, 0.38);
        anchors[TANK_PART] = TANK;
        anchors[HOSE_PART] = Vec3.ZERO;
        anchors[FORE_PART] = new Vec3(0.0, -0.178, 0.23);
        anchors[VALVE_PART] = new Vec3(0.0, -0.115, -0.12);
        return anchors;
    }

    private static float[][] grows() {
        float[][] grows = new float[VALVE_PART + 1][];
        grows[GRIP_PART] = new float[] { 1.0F, 4.0F };
        grows[BODY_PART] = new float[] { 3.0F, 6.0F };
        grows[BARREL_PART] = new float[] { 7.0F, 5.0F };
        for (int k = 0; k < GunShapes.FLAME_FINS; k++) {
            grows[FIN_PART + k] = new float[] { 9.5F + 0.55F * k, 2.5F };
        }
        grows[NOZZLE_PART] = new float[] { 12.0F, 3.5F };
        grows[PILOT_PART] = new float[] { 14.0F, 3.0F };
        grows[TANK_PART] = new float[] { 5.0F, 7.0F };
        grows[HOSE_PART] = new float[] { 9.0F, 5.0F };
        grows[FORE_PART] = new float[] { 11.0F, 3.0F };
        grows[VALVE_PART] = new float[] { 12.5F, 2.5F };
        return grows;
    }

    private static ConstructPainter.Shape[] parts() {
        Mesh[][] groups = GunShapes.flamethrowerParts();
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

    public static ConstructPainter.Frame held(Vec3 grip, Vec3 forward, Vec3 up, double scale) {
        Vec3 ahead = forward.normalize();
        Vec3 top = up.subtract(ahead.scale(up.dot(ahead)));
        top = top.lengthSqr() < 1.0E-8 ? ConstructPainter.Frame.of(grip, ahead, new Vec3(0.0, 1.0, 0.0), 1.0).up()
                : top.normalize();
        ConstructPainter.Frame hand = new ConstructPainter.Frame(grip, ahead.cross(top).normalize(), top, ahead,
                scale);
        return hand.moved(-GRIP.x, -GRIP.y, -GRIP.z);
    }

    private static double grown(int part, double t) {
        float[] grow = GROWS[part];
        double u = (t - grow[0]) / grow[1];
        return part == BARREL_PART || part == PILOT_PART || part == BODY_PART ? Ease.smooth(u) : Ease.backOut(u);
    }

    public static void gun(LanternPainter painter, ConstructPainter.Frame frame, double t, Glow glow, double apart,
            @Nullable Vec3 ring) {
        boolean forming = t < GROWN_BY + 4.0;
        for (int p = 0; p < PARTS.length; p++) {
            double grown = forming ? grown(p, t) : 1.0;
            if (grown <= 0.01) {
                continue;
            }
            ConstructPainter.Shape shape = p == HOSE_PART && grown < 1.0 ? hose(grown) : PARTS[p];
            ConstructPainter.Frame at = part(frame, p, grown, glow.valve());
            if (apart > 0.0) {
                painter.fling(FLING);
                painter.shattered(shape, at, apart, 1.2, 30 + 7 * p);
                painter.fling(1.0);
                continue;
            }
            double fresh = forming ? 1.0 - Ease.smooth((t - GROWS[p][0] - GROWS[p][1]) / 3.0) : 0.0;
            painter.glare(0.75 * Math.max(fresh, 0.0));
            painter.ambient(0.12);
            painter.shape(shape, at, 1.0, 1.0 + 0.25 * (p >= FIN_PART && p < NOZZLE_PART ? glow.heat() : 0.0));
            painter.ambient(0.0);
            painter.glare(0.0);
            if (forming && grown < 1.0) {
                painter.flare(frame.at(ANCHORS[p].x, ANCHORS[p].y, ANCHORS[p].z), 0.07 * frame.scale(),
                        0.8 * (1.0 - grown));
            }
        }
        if (apart > 0.0) {
            return;
        }
        if (forming && ring != null) {
            double feed = 1.0 - Ease.smooth((t - (GROWN_BY - 3.0)) / 4.0);
            if (feed > 0.0) {
                double reach = Mth.clamp((t - 2.0) / (GROWN_BY - 4.0), 0.0, 1.0);
                Vec3 front = frame.at(0.0, -0.05, Mth.lerp(reach, GRIP.z, NOZZLE.z));
                painter.beam(ring, front, feed, frame.scale() * 0.5);
            }
        }
        lights(painter, frame, glow);
    }

    private static ConstructPainter.Frame part(ConstructPainter.Frame frame, int p, double grown, double valve) {
        Vec3 anchor = ANCHORS[p];
        ConstructPainter.Frame at = frame.moved(anchor.x, anchor.y, anchor.z);
        if (p == VALVE_PART && valve != 0.0) {
            at = at.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, valve);
        }
        if (grown >= 1.0 || p == HOSE_PART) {
            return at;
        }
        if (p == BARREL_PART || p == PILOT_PART) {
            double wide = Math.min(1.0, 0.3 + grown * 1.5);
            return at.stretched(wide, wide, Math.max(1.0E-3, grown));
        }
        if (p == BODY_PART) {
            double wide = Ease.backOut(Math.min(1.0, grown * 1.6));
            return at.stretched(wide, wide, Math.max(1.0E-3, grown));
        }
        if (p == TANK_PART) {
            double round = Ease.backOut(Math.min(1.0, grown * 1.8));
            return at.stretched(round, round, Math.max(1.0E-3, Math.min(1.0, grown)));
        }
        double s = Math.max(1.0E-3, grown);
        return at.stretched(s, s, s);
    }

    private static ConstructPainter.Shape hose(double grown) {
        Vec3[] path = GunShapes.FLAME_HOSE;
        double[] along = new double[path.length];
        for (int i = 1; i < path.length; i++) {
            along[i] = along[i - 1] + path[i].distanceTo(path[i - 1]);
        }
        double head = along[path.length - 1] * Mth.clamp(grown, 0.0, 1.0);
        List<Vec3> points = new ArrayList<>();
        List<Mesh> meshes = new ArrayList<>();
        points.add(path[0]);
        for (int i = 1; i < path.length; i++) {
            if (along[i] <= head) {
                points.add(path[i]);
                if (i < path.length - 1) {
                    meshes.add(GunShapes.hoseRing(i));
                }
                continue;
            }
            double share = (head - along[i - 1]) / Math.max(1.0E-6, along[i] - along[i - 1]);
            points.add(path[i - 1].lerp(path[i], share));
            break;
        }
        if (points.size() >= 2 && points.get(0).distanceToSqr(points.get(1)) > 1.0E-8) {
            meshes.add(Mesh.tube(false, 7, 0.018, 1.15, points.toArray(Vec3[]::new)));
        }
        return ConstructPainter.Shape.of(meshes.toArray(Mesh[]::new));
    }

    private static void lights(LanternPainter painter, ConstructPainter.Frame frame, Glow glow) {
        double scale = frame.scale();
        double fill = glow.fill();
        if (fill > 0.0 && fill < 1.0) {
            Vec3[] hose = GunShapes.FLAME_HOSE;
            double run = fill * 2.2;
            if (run < 1.0) {
                double spot = run * (hose.length - 1);
                int i = Math.min(hose.length - 2, (int) spot);
                Vec3 at = hose[i].lerp(hose[i + 1], spot - i);
                painter.flare(frame.at(at.x, at.y, at.z), 0.06 * scale, 1.0);
                painter.glowDisc(frame.at(TANK.x, TANK.y, TANK.z), 0.16 * scale, FirePainter.FLAME, 0.6 * (1.0 - run),
                        0.1, 3);
            } else if (run < 1.6) {
                double u = (run - 1.0) / 0.6;
                painter.flare(frame.at(0.0, 0.1, Mth.lerp(u, -0.24, 0.02)), 0.05 * scale, 1.0);
            }
        }
        double fins = Mth.clamp((fill - 0.55) / 0.45, 0.0, 1.0);
        for (int k = 0; k < GunShapes.FLAME_FINS; k++) {
            double lit = Mth.clamp(fins * GunShapes.FLAME_FINS - k, 0.0, 1.0);
            double flash = fill < 1.0 ? Math.max(0.0, 1.0 - Math.abs(fins * GunShapes.FLAME_FINS - k - 0.5)) : 0.0;
            double strength = Math.max(glow.heat(), 0.25 * lit) + 0.6 * flash;
            if (strength <= 0.02) {
                continue;
            }
            double z = 0.106 + 0.045 * k;
            painter.circle(frame.at(0.0, 0.02, z), frame.right().normalize(), frame.up().normalize(),
                    FIN_RADIUS * scale, 0.012 * scale, 0.05 * scale, Colors.alpha(0.9 * strength),
                    Colors.alpha(0.4 * strength));
        }
        Vec3 forward = frame.forward().normalize();
        Vec3 up = frame.up().normalize();
        Vec3 pilot = frame.at(PILOT.x, PILOT.y, PILOT.z);
        FirePainter.pilot(painter, pilot, forward, up, 0.03 * scale, glow.pilot(), painter.time());
        FirePainter.spark(painter, pilot, forward, 0.05 * scale, glow.spark(), (int) (painter.time() * 7.0));
        if (glow.muzzle() > 0.0) {
            Vec3 nozzle = frame.at(NOZZLE.x, NOZZLE.y, NOZZLE.z);
            FirePainter.ball(painter, nozzle.add(forward.scale(0.05 * scale)), 0.12 * scale * (0.8 + 0.4
                    * glow.muzzle()), 1.0, glow.muzzle(), (int) (painter.time() * 5.0));
        }
    }
}
