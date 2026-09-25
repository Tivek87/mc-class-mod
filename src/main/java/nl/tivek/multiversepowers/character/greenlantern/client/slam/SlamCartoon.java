package nl.tivek.multiversepowers.character.greenlantern.client.slam;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter.Moment;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import static nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamLetters.join;
import static nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamLetters.quarters;
import static nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamLetters.text;
import static nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamMoney.bar;
import static nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamMoney.money;

final class SlamCartoon {
    private SlamCartoon() {
    }

    private static final double SAFE_SCALE = 1.45;
    private static final double HINGE_X = -0.64;
    private static final double HINGE_Z = -0.76;
    private static final double WHEEL_X = 0.14;
    private static final double WHEEL_Y = 0.66;
    private static final double WHEEL_Z = -0.83;
    private static final double DOOR_OPEN = Math.toRadians(105.0);

    private static final Shape SAFE = new Shape(new double[][] {
            { -0.80, 0.14, 0.50, 0.80, 1.74, 0.70, 0.95 },
            { -0.80, 0.14, -0.70, -0.62, 1.74, 0.50, 0.95 },
            { 0.62, 0.14, -0.70, 0.80, 1.74, 0.50, 0.95 },
            { -0.62, 1.56, -0.70, 0.62, 1.74, 0.50, 0.95 },
            { -0.62, 0.14, -0.70, 0.62, 0.32, 0.50, 0.95 },
            { -0.85, 1.74, -0.75, 0.85, 1.83, 0.75, 1.05 },
            { -0.85, 0.07, -0.75, 0.85, 0.15, 0.75, 1.0 },
            { -0.87, 0.15, -0.77, -0.75, 1.74, -0.65, 1.1 },
            { 0.75, 0.15, -0.77, 0.87, 1.74, -0.65, 1.1 },
            { -0.62, 0.32, 0.46, 0.62, 1.56, 0.50, 0.55 },
            { -0.62, 0.32, -0.62, -0.58, 1.56, 0.46, 0.6 },
            { 0.58, 0.32, -0.62, 0.62, 1.56, 0.46, 0.6 },
            { -0.58, 1.52, -0.62, 0.58, 1.56, 0.46, 0.6 },
            { -0.58, 0.32, -0.62, 0.58, 0.35, 0.46, 0.6 },
            { -0.58, 0.92, -0.62, 0.58, 0.97, 0.46, 0.85 },
            { -0.50, 0.35, -0.20, -0.14, 0.53, 0.28, 1.05 },
            { -0.34, 0.345, -0.21, -0.30, 0.535, 0.29, 1.3 },
            { 0.10, 0.35, -0.10, 0.46, 0.49, 0.36, 1.05 },
            { 0.26, 0.345, -0.11, 0.30, 0.495, 0.37, 1.3 },
            { -0.44, 0.97, -0.05, -0.08, 1.11, 0.40, 1.05 },
            { -0.28, 0.965, -0.06, -0.24, 1.115, 0.41, 1.3 } },
            foot(-0.66, -0.56), foot(0.66, -0.56), foot(-0.66, 0.56), foot(0.66, 0.56),
            Mesh.cylinder(10, 0.05, 0.0, 0.20, 1.2).moved(HINGE_X, 0.46, HINGE_Z),
            Mesh.cylinder(10, 0.05, 0.0, 0.20, 1.2).moved(HINGE_X, 1.24, HINGE_Z),
            bar().moved(0.26, 1.015, 0.05), bar().moved(0.26, 1.105, 0.05));
    private static final Shape DOOR = new Shape(new double[][] {
            { -0.62, 0.32, -0.80, 0.62, 1.56, -0.70, 1.0 },
            { -0.50, 0.44, -0.83, 0.50, 1.44, -0.80, 1.07 },
            { -0.22, 1.33, -0.855, 0.22, 1.41, -0.83, 1.3 } },
            Mesh.lathe(16, 1.1, 0.0, 0.0, 0.18, 0.0, 0.18, 0.025, 0.15, 0.05, 0.05, 0.05, 0.05, 0.10, 0.03, 0.12, 0.0,
                    0.12).turned(1.0, 0.0, 0.0, -90.0).moved(0.14, 1.06, -0.83),
            mark(0), mark(45), mark(90), mark(135), mark(180), mark(225), mark(270), mark(315));
    private static final Shape WHEEL = Shape.of(Mesh.cylinder(12, 0.07, 0.0, 0.10, 1.1).turned(1.0, 0.0, 0.0, -90.0),
            spoke(0), spoke(60), spoke(120), knob(0), knob(60), knob(120), knob(180), knob(240), knob(300));
    private static final Shape BOLTS = Shape.of(bolt(0.55), bolt(0.94), bolt(1.33));

    private static Mesh foot(double x, double z) {
        return Mesh.cylinder(10, 0.10, 0.0, 0.08, 1.0).moved(x, 0.0, z);
    }

    private static Mesh mark(double degrees) {
        return Mesh.box(-0.012, 0.195, -0.845, 0.012, 0.235, -0.83, 1.35).turned(0.0, 0.0, 1.0, degrees)
                .moved(0.14, 1.06, 0.0);
    }

    private static Mesh spoke(double degrees) {
        return Mesh.box(-0.25, -0.024, -0.10, 0.25, 0.024, -0.052, 1.05).turned(0.0, 0.0, 1.0, degrees);
    }

    private static Mesh knob(double degrees) {
        return Mesh.ball(8, 5, 0.045, 1.2).moved(0.25, 0.0, -0.076).turned(0.0, 0.0, 1.0, degrees);
    }

    private static Mesh bolt(double y) {
        return Mesh.cylinder(10, 0.055, 0.0, 0.14, 1.15).alongX().moved(0.60, y, -0.75);
    }

    static Vec3 safe(LanternPainter painter, Moment m) {
        Frame base = SlamPainter.dropped(m, SAFE_SCALE, 0.0, m.forward(), 0.5);
        Frame body = SlamPainter.squashed(base, m, 0.12);
        SlamPainter.marker(painter, m, SAFE_SCALE);
        SlamPainter.piece(painter, SAFE, body, m);
        double since = m.since();
        double unlock = Mth.clamp(since / 1.2, 0.0, 1.0);
        double open = DOOR_OPEN * SlamPainter.spring(since - 1.2, 0.9);
        Frame door = body.turned(HINGE_X, 0.0, HINGE_Z, 0.0, 1.0, 0.0, open);
        SlamPainter.piece(painter, DOOR, door, m);
        SlamPainter.piece(painter, BOLTS, door.moved(-0.13 * unlock, 0.0, 0.0), m);
        SlamPainter.piece(painter, WHEEL, door.moved(WHEEL_X, WHEEL_Y, WHEEL_Z).turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                5.0 * unlock * unlock), m);
        if (m.struck()) {
            SlamPainter.sparks(painter, body.at(0.0, 0.05, -0.8), since, 8, 0.25, 41);
            money(painter, base, m);
        }
        return body.at(0.0, 1.83, 0.0);
    }

    private static final double PIANO_SCALE = 1.4;
    private static final int WHITE_KEYS = 14;
    private static final double KEY_WIDTH = 0.15;
    private static final int[] BLACK_AFTER = { 0, 1, 3, 4, 5, 7, 8, 10, 11, 12 };

    private static final Shape PIANO = new Shape(new double[][] {
            { -1.12, 0.10, -0.30, 1.12, 2.00, 0.45, 0.95 },
            { -1.24, 0.00, -0.50, -1.12, 2.02, 0.47, 1.0 },
            { 1.12, 0.00, -0.50, 1.24, 2.02, 0.47, 1.0 },
            { -1.24, 0.94, -0.98, -1.10, 1.28, -0.50, 1.0 },
            { 1.10, 0.94, -0.98, 1.24, 1.28, -0.50, 1.0 },
            { -1.12, 0.10, -0.44, 1.12, 0.94, -0.30, 1.0 },
            { -0.92, 0.24, -0.47, 0.92, 0.80, -0.44, 1.08 },
            { -1.10, 0.94, -0.98, 1.10, 1.10, -0.44, 0.95 },
            { -1.10, 0.98, -1.01, 1.10, 1.10, -0.98, 1.12 },
            { -1.12, 1.10, -0.50, 1.12, 1.30, -0.40, 0.95 },
            { -1.12, 1.30, -0.40, 1.12, 1.96, -0.30, 1.0 },
            { -1.00, 1.42, -0.43, -0.06, 1.86, -0.40, 1.08 },
            { 0.06, 1.42, -0.43, 1.00, 1.86, -0.40, 1.08 },
            { -1.24, 0.00, -0.98, -1.10, 0.10, -0.50, 1.0 },
            { 1.10, 0.00, -0.98, 1.24, 0.10, -0.50, 1.0 },
            { -0.42, 0.00, -0.50, 0.42, 0.14, -0.44, 0.95 },
            { -0.30, 0.05, -0.66, -0.20, 0.10, -0.50, 1.25 },
            { -0.05, 0.05, -0.66, 0.05, 0.10, -0.50, 1.25 },
            { 0.20, 0.05, -0.66, 0.30, 0.10, -0.50, 1.25 } },
            leg(-1.17), leg(1.17));
    private static final Shape PIANO_LID = new Shape(new double[][] {
            { -1.27, 2.02, -0.49, 1.27, 2.09, 0.50, 1.05 },
            { -1.27, 2.00, -0.54, 1.27, 2.09, -0.49, 1.15 } });
    private static final Shape FALLBOARD = new Shape(new double[][] {
            { -1.09, 1.23, -0.97, 1.09, 1.29, -0.50, 1.02 },
            { -0.24, 1.29, -0.90, 0.24, 1.305, -0.84, 1.3 } });
    private static final double[][] WHITE_KEY = { { -0.069, 1.10, -0.95, 0.069, 1.17, -0.52, 1.2 } };
    private static final double[][] BLACK_KEY = { { -0.04, 1.17, -0.80, 0.04, 1.23, -0.52, 0.5 } };

    private static Mesh leg(double x) {
        return Mesh.lathe(10, 1.0, 0.0, 0.10, 0.07, 0.10, 0.07, 0.20, 0.045, 0.32, 0.085, 0.55, 0.045, 0.80, 0.065,
                0.88, 0.065, 0.94, 0.0, 0.94).moved(x, 0.0, -0.86);
    }

    static Vec3 piano(LanternPainter painter, Moment m) {
        Frame body = SlamPainter.squashed(SlamPainter.dropped(m, PIANO_SCALE, 0.0, m.right(), 0.3), m, 0.1);
        SlamPainter.marker(painter, m, PIANO_SCALE);
        SlamPainter.piece(painter, PIANO, body, m);
        double since = m.since();
        SlamPainter.piece(painter, PIANO_LID, body.turned(0.0, 2.09, 0.50, 1.0, 0.0, 0.0,
                Math.toRadians(75.0) * SlamPainter.spring(since - 0.4, 1.0)), m);
        SlamPainter.piece(painter, FALLBOARD, body.turned(0.0, 1.29, -0.50, 1.0, 0.0, 0.0,
                Math.toRadians(93.0) * SlamPainter.bounce(since - 0.9, 1.1)), m);
        for (int i = 0; i < WHITE_KEYS; i++) {
            double x = -1.05 + KEY_WIDTH * (i + 0.5);
            SlamPainter.piece(painter, WHITE_KEY, body.moved(x, -keyDip(i, since), 0.0), m);
        }
        for (int i = 0; i < BLACK_AFTER.length; i++) {
            double x = -1.05 + KEY_WIDTH * (BLACK_AFTER[i] + 1);
            SlamPainter.piece(painter, BLACK_KEY, body.moved(x, -keyDip(WHITE_KEYS + i, since), 0.0), m);
        }
        if (m.struck() && m.apart() <= 0.0) {
            for (int k = 0; k < 4; k++) {
                double age = since - 1.0 - 1.3 * k;
                if (age > 0.0 && age < 9.0) {
                    double x = (Noise.of(k, 65, 0) - 0.5) * 1.6;
                    Vec3 at = body.at(x + 0.12 * Math.sin(age * 0.8 + k), 2.35 + 0.11 * age, 0.1);
                    note(painter, at, 0.26, 1.0 - age / 9.0);
                }
            }
        }
        return body.at(0.0, 2.1, 0.0);
    }

    private static double keyDip(int key, double since) {
        double when = 0.3 + 3.5 * Noise.of(key, 66, 0);
        return 0.035 * Math.max(0.0, 1.0 - Math.abs(since - when) / 0.8);
    }

    private static final double ANVIL_SCALE = 1.9;

    private static final Shape ANVIL = new Shape(new double[][] {
            { -0.78, 0.00, -0.52, 0.78, 0.10, 0.52, 0.95 },
            { -0.68, 0.10, -0.44, 0.68, 0.24, 0.44, 1.0 },
            { -0.80, 0.86, -0.34, 0.70, 1.16, 0.34, 1.05 },
            { -0.82, 1.16, -0.36, 0.72, 1.22, 0.36, 1.15 },
            { -1.02, 0.96, -0.24, -0.80, 1.20, 0.24, 1.0 },
            { -0.72, 1.22, -0.07, -0.58, 1.228, 0.07, 0.35 } },
            frustum(0.50, 0.26, 0.24, 0.55, 0.70), frustum(0.26, 0.50, 0.55, 0.86, 0.70),
            Mesh.cone(12, 0.24, 0.02, 0.0, 0.78, 1.05).alongX().scaled(1.0, 0.85, 1.0).moved(0.70, 1.02, 0.0),
            Mesh.cylinder(8, 0.045, 1.22, 1.228, 0.35).moved(-0.40, 0.0, 0.0),
            Mesh.torus(14, 6, 0.075, 0.022, 1.3).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 1.01, -0.35));

    static Vec3 anvil(LanternPainter painter, Moment m) {
        Frame base = SlamPainter.dropped(m, ANVIL_SCALE, 0.0, m.right(), 0.0);
        double since = m.since();
        double hop = since > 1.0 && since < 3.5 ? 0.16 * Math.sin(Math.PI * (since - 1.0) / 2.5) : 0.0;
        Frame body = SlamPainter.squashed(base, m, 0.15).moved(0.0, hop, 0.0);
        SlamPainter.marker(painter, m, ANVIL_SCALE);
        SlamPainter.piece(painter, ANVIL, body, m);
        SlamPainter.sparks(painter, base.at(0.0, 0.05, 0.0), since, 12, 0.3, 43);
        SlamPainter.sparks(painter, base.at(0.0, 0.05, 0.0), since - 3.5, 6, 0.2, 44);
        return body.at(0.0, 1.22, 0.0);
    }

    private static Mesh frustum(double below, double above, double bottom, double top, double deep) {
        double corner = Math.sqrt(2.0);
        return Mesh.lathe(4, 1.0, 0.0, bottom, below * corner, bottom, above * corner, top, 0.0, top)
                .turned(0.0, 1.0, 0.0, 45.0).scaled(1.0, 1.0, deep);
    }

    private static final double WEIGHT_SCALE = 1.6;
    private static final double WEIGHT_LEAN = Math.toDegrees(Math.atan2(0.92 * 0.70 - 0.62 * 0.70, 1.14));
    private static final double LABEL_Y = 0.70;
    private static final double LABEL_Z = -(0.92 - (0.92 - 0.62) * (LABEL_Y - 0.16) / 1.14) * 0.70;

    private static final Shape WEIGHT = new Shape(new double[][] {
            { -1.00, 0.00, -0.70, 1.00, 0.16, 0.70, 1.0 },
            { -0.66, 1.30, -0.46, 0.66, 1.40, 0.46, 1.05 } },
            frustum(0.92, 0.62, 0.16, 1.30, 0.70),
            Mesh.torus(18, 8, 0.36, 0.08, 1.1).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 1.70, 0.0));
    private static final Shape LABEL = new Shape(join(
            new double[][] { { -0.60, 0.42, LABEL_Z - 0.03, 0.60, 0.98, LABEL_Z + 0.01, 1.1 } },
            text("1 TON", 0.55, 0.06, LABEL_Z - 0.06, LABEL_Z - 0.03, 1.35)));

    static Vec3 weight(LanternPainter painter, Moment m) {
        Frame body = SlamPainter.squashed(SlamPainter.dropped(m, WEIGHT_SCALE, 0.0, m.right(), 0.0), m, 0.22);
        SlamPainter.marker(painter, m, WEIGHT_SCALE);
        SlamPainter.piece(painter, WEIGHT, body, m);
        SlamPainter.piece(painter, LABEL, body.turned(0.0, LABEL_Y, LABEL_Z, 1.0, 0.0, 0.0,
                Math.toRadians(WEIGHT_LEAN)), m);
        return body.at(0.0, 2.14, 0.0);
    }

    private static final double TNT_SCALE = 1.6;
    // Must match LandingSlam.TNT_LANDS on the server, or the fuse and the drop would disagree.
    private static final double TNT_LANDS = 9.0;
    private static final Vec3[] FUSE = { new Vec3(0.0, 1.50, 0.0), new Vec3(0.0, 1.62, 0.02),
            new Vec3(0.03, 1.72, 0.05), new Vec3(0.08, 1.80, 0.06), new Vec3(0.14, 1.85, 0.04),
            new Vec3(0.20, 1.87, 0.0) };

    private static final Shape TNT = new Shape(join(
            new double[][] { { -0.77, 0.48, -0.77, 0.77, 1.02, 0.77, 1.0 } },
            quarters(text("TNT", 0.57, 0.08, -0.81, -0.77, 1.35))), sticks());

    private static Mesh[] sticks() {
        Mesh[] sticks = new Mesh[16];
        for (int i = 0; i < 16; i++) {
            double x = -0.5625 + 0.375 * (i % 4);
            double z = -0.5625 + 0.375 * (i / 4);
            sticks[i] = Mesh.lathe(10, 0.95, 0.0, 0.0, 0.18, 0.0, 0.18, 1.48, 0.14, 1.50, 0.0, 1.50).moved(x, 0.0, z);
        }
        return sticks;
    }

    static Vec3 tnt(LanternPainter painter, Moment m) {
        double s = m.scale(TNT_SCALE);
        double go = Mth.clamp((m.t() - LandingSlam.FORM_TICKS) / (TNT_LANDS - LandingSlam.FORM_TICKS), 0.0, 1.0);
        double height = SlamPainter.HANG * m.size() * (1.0 - go * go);
        double sitting = m.t() - TNT_LANDS;
        if (sitting > 0.0) {
            height = 0.4 * s * Math.sin(Math.PI * Mth.clamp(sitting / 1.4, 0.0, 1.0));
        }
        boolean waiting = sitting > 0.0 && !m.struck();
        double swell = waiting ? 1.0 + 0.12 * sitting / (LandingSlam.IMPACT_TICK - TNT_LANDS) : 1.0;
        double blink = waiting ? 0.35 * Math.abs(Math.sin(sitting * 4.0)) : 0.0;
        Frame frame = new Frame(m.ground().add(0.0, height, 0.0), m.right(), SlamPainter.UP, m.forward(), s * swell);
        if (m.struck()) {
            painter.shattered(TNT, frame, Mth.clamp(m.since() / 7.0, 0.0, 1.0), 1.5);
            if (m.since() < 6.0) {
                double burst = 1.0 - m.since() / 6.0;
                painter.flare(m.ground().add(0.0, 1.2, 0.0), 1.0 + 3.0 * burst, burst);
            }
            return frame.at(0.0, 0.75, 0.0);
        }
        SlamPainter.marker(painter, m, TNT_SCALE);
        painter.shape(TNT, frame, 1.0, 1.0 + blink);
        double left = (1.0 - Mth.clamp((m.t() - TNT_LANDS + 1.0) / (LandingSlam.IMPACT_TICK - TNT_LANDS + 1.0),
                0.0, 0.95)) * (FUSE.length - 1);
        int whole = (int) left;
        for (int i = 0; i < whole; i++) {
            painter.mesh(rod(FUSE[i], FUSE[i + 1], 0.035), frame, 1.0, 1.1);
        }
        Vec3 tip = FUSE[whole];
        if (whole < FUSE.length - 1 && left - whole > 0.05) {
            tip = FUSE[whole].lerp(FUSE[whole + 1], left - whole);
            painter.mesh(rod(FUSE[whole], tip, 0.035), frame, 1.0, 1.1);
        }
        if (m.t() > TNT_LANDS - 1.0) {
            painter.flare(frame.at(tip.x, tip.y, tip.z), 0.22 + 0.08 * Math.sin(m.t() * 5.0), 1.0);
        }
        return frame.at(0.0, 0.75, 0.0);
    }

    private static Mesh rod(Vec3 a, Vec3 b, double radius) {
        Vec3 way = b.subtract(a);
        return Mesh.cylinder(6, radius, 0.0, way.length(), 1.1).pointing(way.x, way.y, way.z).moved(a.x, a.y, a.z);
    }

    private static final double BRICK_SCALE = 1.5;

    private static final Shape BRICK = new Shape(new double[][] {
            { -1.60, 0.00, -0.80, 1.60, 0.96, -0.70, 1.0 },
            { -1.60, 0.00, 0.70, 1.60, 0.96, 0.80, 1.0 },
            { -1.60, 0.00, -0.70, -1.50, 0.96, 0.70, 1.0 },
            { 1.50, 0.00, -0.70, 1.60, 0.96, 0.70, 1.0 },
            { -1.50, 0.78, -0.70, 1.50, 0.96, 0.70, 1.0 },
            { -1.50, 0.76, -0.70, 1.50, 0.78, 0.70, 0.55 } },
            tube(-0.8), tube(0.0), tube(0.8),
            stud(-1.2, -0.4), stud(-0.4, -0.4), stud(0.4, -0.4), stud(1.2, -0.4),
            stud(-1.2, 0.4), stud(-0.4, 0.4), stud(0.4, 0.4), stud(1.2, 0.4));

    private static Mesh tube(double x) {
        return Mesh.ring(12, 0.8, 0.18, 0.0, 0.26, 0.0, 0.26, 0.76, 0.18, 0.76).moved(x, 0.0, 0.0);
    }

    private static Mesh stud(double x, double z) {
        return Mesh.lathe(14, 1.1, 0.0, 0.96, 0.25, 0.96, 0.25, 1.14, 0.22, 1.16, 0.13, 1.16, 0.13, 1.175, 0.0, 1.175)
                .moved(x, 0.0, z);
    }

    static Vec3 brick(LanternPainter painter, Moment m) {
        Frame base = SlamPainter.dropped(m, BRICK_SCALE, 0.0, m.forward(), 0.7);
        double since = m.since();
        double hop = since > 0.0 ? 0.45 * Math.exp(-0.35 * since) * Math.abs(Math.sin(0.9 * since)) : 0.0;
        Frame body = SlamPainter.squashed(base, m, 0.12).moved(0.0, hop, 0.0);
        SlamPainter.marker(painter, m, BRICK_SCALE);
        SlamPainter.piece(painter, BRICK, body, m);
        return body.at(0.0, 1.16, 0.0);
    }

    private static final double STAMP_SCALE = 1.5;

    private static final Shape STAMP = new Shape(new double[][] {
            { -1.10, 0.06, -0.80, 1.10, 0.20, 0.80, 1.2 },
            { -1.00, 0.20, -0.72, 1.00, 0.50, 0.72, 0.95 },
            { -0.55, 0.00, -0.68, 0.55, 0.06, -0.56, 1.25 },
            { -0.55, 0.00, 0.56, 0.55, 0.06, 0.68, 1.25 } },
            Mesh.ring(20, 1.25, 0.30, 0.0, 0.46, 0.0, 0.46, 0.06, 0.30, 0.06),
            frustum(1.00, 0.70, 0.50, 0.62, 0.72),
            Mesh.lathe(14, 1.0, 0.0, 0.62, 0.30, 0.62, 0.30, 0.70, 0.18, 0.78, 0.16, 1.10, 0.24, 1.25, 0.42, 1.45,
                    0.48, 1.65, 0.42, 1.85, 0.25, 1.98, 0.0, 2.02),
            Mesh.torus(14, 6, 0.17, 0.035, 1.25).moved(0.0, 0.95, 0.0));

    static Vec3 stamp(LanternPainter painter, Moment m) {
        double since = m.since();
        double lift = m.struck() ? 1.1 * Ease.smooth((since - 1.5) / 5.0) : 0.0;
        double rock = m.struck() ? 0.07 * Math.sin(since * 2.2) * Math.exp(-0.4 * since) : 0.0;
        Frame body = SlamPainter.squashed(SlamPainter.dropped(m, STAMP_SCALE, 0.0, m.right(), 0.0), m, 0.1)
                .moved(0.0, lift, 0.0).turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, rock);
        SlamPainter.marker(painter, m, STAMP_SCALE);
        SlamPainter.piece(painter, STAMP, body, m);
        if (m.struck()) {
            double glow = 1.0 - Math.max(0.0, since - 8.0) / 10.0;
            print(painter, m.ground().add(0.0, 0.04, 0.0), m.right(), m.forward(),
                    m.scale(STAMP_SCALE), glow);
        }
        return body.at(0.0, 2.02, 0.0);
    }

    private static void print(LanternPainter painter, Vec3 at, Vec3 right, Vec3 forward, double size,
            double strength) {
        if (strength <= 0.0) {
            return;
        }
        int edge = Colors.alpha(0.95 * strength);
        int halo = Colors.alpha(0.45 * strength);
        painter.circle(at, right, forward, 0.46 * size, 0.08, 0.3, edge, halo);
        painter.circle(at, right, forward, 0.30 * size, 0.06, 0.25, edge, halo);
        for (double bar : new double[] { -0.62, 0.62 }) {
            Vec3 middle = at.add(forward.scale(bar * size));
            painter.edge(middle.subtract(right.scale(0.55 * size)), middle.add(right.scale(0.55 * size)), 0.12,
                    strength);
        }
    }

    private static void note(LanternPainter painter, Vec3 at, double size, double strength) {
        Vec3 view = painter.camera().subtract(at);
        Vec3 side = view.cross(SlamPainter.UP);
        if (side.lengthSqr() < 1.0E-6) {
            return;
        }
        side = side.normalize();
        Vec3 up = SlamPainter.UP;
        painter.circle(at, side, up, 0.28 * size, 0.07 * size, 0.3 * size, Colors.alpha(0.95 * strength),
                Colors.alpha(0.5 * strength));
        painter.flare(at, 0.35 * size, 0.6 * strength);
        Vec3 stemFoot = at.add(side.scale(0.26 * size));
        Vec3 stemTop = stemFoot.add(up.scale(1.1 * size));
        painter.edge(stemFoot, stemTop, 0.07 * size, strength);
        painter.edge(stemTop, stemTop.add(side.scale(0.35 * size)).subtract(up.scale(0.35 * size)), 0.07 * size,
                strength);
    }
}
