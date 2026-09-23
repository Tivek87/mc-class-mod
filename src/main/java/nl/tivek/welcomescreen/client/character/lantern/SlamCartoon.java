package nl.tivek.welcomescreen.client.character.lantern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.character.lantern.LandingSlam;
import nl.tivek.welcomescreen.client.character.lantern.ConstructPainter.Frame;
import nl.tivek.welcomescreen.client.character.lantern.ConstructPainter.Shape;
import nl.tivek.welcomescreen.client.character.lantern.SlamPainter.Moment;

/**
 * The landing-slam constructs that drop on you in a cartoon (see {@link SlamPainter}): a safe that bursts open and
 * spills its money, a piano whose lids fly open, an anvil, a ton weight, a bundle of TNT whose fuse burns down, a toy
 * brick and a rubber stamp. Unless it says otherwise a shape stands on y = 0 and faces -z, the way towards Green
 * Lantern, with x to his right; in blocks at scale 1.
 */
final class SlamCartoon {
    private SlamCartoon() {
    }

    /** Letters five pixels tall, row by row from the top: '#' is a pixel, anything else is empty. */
    private static final Map<Character, String[]> FONT = Map.of(
            '1', new String[] { ".#.", "##.", ".#.", ".#.", "###" },
            'T', new String[] { "###", ".#.", ".#.", ".#.", ".#." },
            'O', new String[] { "###", "#.#", "#.#", "#.#", "###" },
            'N', new String[] { "#..#", "##.#", "#.##", "#..#", "#..#" },
            ' ', new String[] { ".", ".", ".", ".", "." });

    // ---- The safe ----

    private static final double SAFE_SCALE = 1.45;
    // The line the door turns about: straight up through here.
    private static final double HINGE_X = -0.64;
    private static final double HINGE_Z = -0.76;
    // Where the wheel on the door has its middle.
    private static final double WHEEL_X = 0.14;
    private static final double WHEEL_Y = 0.66;
    private static final double WHEEL_Z = -0.83;
    // How far the door flies open, and how much money bursts out.
    private static final double DOOR_OPEN = Math.toRadians(105.0);
    private static final int COINS = 12;
    private static final int NOTES = 7;
    private static final int BARS = 2;

    /** The safe, open at the front: its walls, the darker lining inside, a shelf, and the money that stays in it. */
    private static final Shape SAFE = new Shape(new double[][] {
            // The walls (back, left, right, top, bottom), a rim round its top and round its foot
            { -0.80, 0.14, 0.50, 0.80, 1.74, 0.70, 0.95 },
            { -0.80, 0.14, -0.70, -0.62, 1.74, 0.50, 0.95 },
            { 0.62, 0.14, -0.70, 0.80, 1.74, 0.50, 0.95 },
            { -0.62, 1.56, -0.70, 0.62, 1.74, 0.50, 0.95 },
            { -0.62, 0.14, -0.70, 0.62, 0.32, 0.50, 0.95 },
            { -0.85, 1.74, -0.75, 0.85, 1.83, 0.75, 1.05 },
            { -0.85, 0.07, -0.75, 0.85, 0.15, 0.75, 1.0 },
            // Steel strips down its front corners
            { -0.87, 0.15, -0.77, -0.75, 1.74, -0.65, 1.1 },
            { 0.75, 0.15, -0.77, 0.87, 1.74, -0.65, 1.1 },
            // Inside: the lining, a shelf, and stacks of notes with a band round each
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
            // Round feet, the hinges of the door, and two gold bars on the shelf
            foot(-0.66, -0.56), foot(0.66, -0.56), foot(-0.66, 0.56), foot(0.66, 0.56),
            Mesh.cylinder(10, 0.05, 0.0, 0.20, 1.2).moved(HINGE_X, 0.46, HINGE_Z),
            Mesh.cylinder(10, 0.05, 0.0, 0.20, 1.2).moved(HINGE_X, 1.24, HINGE_Z),
            bar().moved(0.26, 1.015, 0.05), bar().moved(0.26, 1.105, 0.05));
    /** The door, shut: a thick slab with a raised panel, a name plate, and a dial with marks round it. */
    private static final Shape DOOR = new Shape(new double[][] {
            { -0.62, 0.32, -0.80, 0.62, 1.56, -0.70, 1.0 },
            { -0.50, 0.44, -0.83, 0.50, 1.44, -0.80, 1.07 },
            { -0.22, 1.33, -0.855, 0.22, 1.41, -0.83, 1.3 } },
            Mesh.lathe(16, 1.1, 0.0, 0.0, 0.18, 0.0, 0.18, 0.025, 0.15, 0.05, 0.05, 0.05, 0.05, 0.10, 0.03, 0.12, 0.0,
                    0.12).turned(1.0, 0.0, 0.0, -90.0).moved(0.14, 1.06, -0.83),
            mark(0), mark(45), mark(90), mark(135), mark(180), mark(225), mark(270), mark(315));
    /**
     * The wheel that opens the door: three spokes through a hub with knobs on their ends, round its own middle and
     * sticking out along -z.
     */
    private static final Shape WHEEL = Shape.of(Mesh.cylinder(12, 0.07, 0.0, 0.10, 1.1).turned(1.0, 0.0, 0.0, -90.0),
            spoke(0), spoke(60), spoke(120), knob(0), knob(60), knob(120), knob(180), knob(240), knob(300));
    /** The bolts that lock the door, out of its edge into the frame. */
    private static final Shape BOLTS = Shape.of(bolt(0.55), bolt(0.94), bolt(1.33));
    /** A coin lying flat round its middle, its face raised in the middle on both sides. */
    private static final Shape COIN = Shape.of(Mesh.lathe(12, 1.2, 0.0, -0.024, 0.07, -0.024, 0.08, -0.018, 0.115,
            -0.018, 0.125, -0.008, 0.125, 0.008, 0.115, 0.018, 0.08, 0.018, 0.07, 0.024, 0.0, 0.024));
    /** A bank note lying flat round its middle, with a round seal in the middle. */
    private static final Shape NOTE = new Shape(new double[][] { { -0.18, -0.006, -0.085, 0.18, 0.006, 0.085, 1.05 } },
            Mesh.cylinder(8, 0.05, -0.012, 0.012, 1.3));
    /** A gold bar lying round its middle, along x. */
    private static final Shape GOLD = Shape.of(bar());

    private static Mesh foot(double x, double z) {
        return Mesh.cylinder(10, 0.10, 0.0, 0.08, 1.0).moved(x, 0.0, z);
    }

    private static Mesh bar() {
        return Mesh.prism(-0.075, 0.075, 1.2, -0.15, -0.045, 0.15, -0.045, 0.11, 0.045, -0.11, 0.045);
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

    /**
     * A safe: it drops and lands with a thud that squashes it a moment; the wheel on its door spins and the bolts shoot
     * back, and the door flies open on its hinges and swings to and fro. Money bursts out: coins that bounce and skid,
     * notes that flutter down, and gold bars that thud down; it all sinks into the ground as the safe breaks up.
     */
    static Vec3 safe(ConstructPainter painter, Moment m) {
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

    /** Where a piece of money is, in the safe's own terms, and how it is turned: about which way, and how far. */
    private record Loot(Vec3 at, Vec3 axis, double angle) {
    }

    /**
     * The money bursting out of the open safe, one piece after the other, out through the door towards him and down
     * onto the ground. Once the safe breaks up it all sinks into the ground.
     */
    private static void money(ConstructPainter painter, Frame safe, Moment m) {
        double since = m.since() - 1.6;
        if (since <= 0.0) {
            return;
        }
        double sink = Math.max(0.0, m.t() - m.burst()) * 0.05;
        for (int k = 0; k < COINS + NOTES + BARS; k++) {
            double dt = since - 0.32 * k - 0.3 * ConstructPainter.noise(k, 61, 0);
            if (dt <= 0.0) {
                continue;
            }
            Vec3 start = new Vec3(Mth.lerp(ConstructPainter.noise(k, 61, 1), -0.42, 0.42),
                    Mth.lerp(ConstructPainter.noise(k, 61, 2), 0.45, 1.35), -0.35);
            Vec3 thrown = new Vec3((ConstructPainter.noise(k, 61, 3) - 0.5) * 0.10,
                    0.07 + 0.07 * ConstructPainter.noise(k, 61, 4), -(0.05 + 0.05 * ConstructPainter.noise(k, 61, 5)));
            Loot loot;
            Shape shape;
            if (k < COINS) {
                loot = coin(k, start, thrown, dt);
                shape = COIN;
            } else if (k < COINS + NOTES) {
                loot = note(k, start, thrown, dt);
                shape = NOTE;
            } else {
                loot = gold(k, start, thrown, dt);
                shape = GOLD;
            }
            Vec3 axis = safe.right().scale(loot.axis().x).add(safe.up().scale(loot.axis().y))
                    .add(safe.forward().scale(loot.axis().z)).normalize();
            painter.shape(shape, SlamPainter.loose(safe, loot.at().subtract(0.0, sink, 0.0), axis, loot.angle()), 1.0,
                    1.0);
        }
    }

    /** The way a piece of money tumbles about, in the safe's own terms. */
    private static Vec3 tumble(int k) {
        return new Vec3(ConstructPainter.noise(k, 62, 0) - 0.5, ConstructPainter.noise(k, 62, 1) - 0.5,
                ConstructPainter.noise(k, 62, 2) - 0.5).add(0.0, 0.0, 0.01).normalize();
    }

    /** A coin: it flies in an arc, tumbling, lands flat, bounces once and skids on a little way. */
    private static Loot coin(int k, Vec3 start, Vec3 thrown, double dt) {
        double g = 0.03;
        double floor = 0.024;
        double turn = 0.45 + 0.3 * ConstructPainter.noise(k, 63, 0);
        double land = (thrown.y + Math.sqrt(thrown.y * thrown.y + 2.0 * g * (start.y - floor))) / g;
        if (dt < land) {
            return new Loot(start.add(thrown.scale(dt)).subtract(0.0, 0.5 * g * dt * dt, 0.0), tumble(k), turn * dt);
        }
        double up = 0.28 * (g * land - thrown.y);
        double hop = Math.max(0.5, 2.0 * up / g);
        double after = dt - land;
        double air = Math.min(after, hop);
        double skid = Math.min(after, hop + 3.0);
        double height = floor + Math.max(0.0, up * air - 0.5 * g * air * air);
        Vec3 at = new Vec3(start.x + thrown.x * (land + 0.4 * skid), height, start.z + thrown.z * (land + 0.4 * skid));
        return new Loot(at, tumble(k), turn * land * (1.0 - ConstructPainter.smooth(after / hop)));
    }

    /** A bank note: thrown out, it slows down in the air and flutters down, swaying, until it lies flat. */
    private static Loot note(int k, Vec3 start, Vec3 thrown, double dt) {
        double floor = 0.008;
        double phase = ConstructPainter.noise(k, 64, 0) * Math.PI * 2.0;
        double landed = dt;
        for (double s = 0.0; s < dt; s += 0.25) {
            if (noteHeight(start, thrown, s) <= floor) {
                landed = s;
                break;
            }
        }
        double s = Math.min(dt, landed);
        double spread = (1.0 - Math.exp(-0.2 * s)) / 0.2;
        double sway = 0.09 * Math.sin(0.9 * s + phase) * Math.min(1.0, s / 3.0);
        Vec3 at = new Vec3(start.x + thrown.x * spread + sway, Math.max(floor, noteHeight(start, thrown, s)),
                start.z + thrown.z * spread);
        double flutter = 0.9 * Math.sin(1.3 * s + phase) * (1.0 - ConstructPainter.smooth(dt - landed));
        return new Loot(at, new Vec3(1.0, 0.0, 0.35).normalize(), flutter);
    }

    /** How high a bank note is once it has flown for {@code s} ticks: first thrown, then sinking slowly. */
    private static double noteHeight(Vec3 start, Vec3 thrown, double s) {
        double spread = (1.0 - Math.exp(-0.2 * s)) / 0.2;
        return start.y + thrown.y * spread - 0.02 * (s - spread);
    }

    /** A gold bar: heavy, it drops in a short arc, turning over slowly, and thuds down where it lands. */
    private static Loot gold(int k, Vec3 start, Vec3 thrown, double dt) {
        double g = 0.04;
        double floor = 0.045;
        double land = (thrown.y + Math.sqrt(thrown.y * thrown.y + 2.0 * g * (start.y - floor))) / g;
        Vec3 axis = new Vec3(1.0, 0.0, 0.0);
        if (dt < land) {
            return new Loot(start.add(thrown.scale(dt)).subtract(0.0, 0.5 * g * dt * dt, 0.0), axis, 0.25 * dt);
        }
        double skid = Math.min(dt - land, 2.0);
        Vec3 at = new Vec3(start.x + thrown.x * (land + 0.3 * skid), floor, start.z + thrown.z * (land + 0.3 * skid));
        double settle = 1.0 - ConstructPainter.smooth((dt - land) / 1.5);
        return new Loot(at, axis, 0.25 * land * settle);
    }

    // ---- The piano ----

    private static final double PIANO_SCALE = 1.4;
    private static final int WHITE_KEYS = 14;
    private static final double KEY_WIDTH = 0.15;
    // The white keys a black key sits after, over two octaves.
    private static final int[] BLACK_AFTER = { 0, 1, 3, 4, 5, 7, 8, 10, 11, 12 };

    /** An upright piano, its keyboard towards him; its lids and keys are drawn on their own, so they can move. */
    private static final Shape PIANO = new Shape(new double[][] {
            // The case, its cheeks and the arms either side of the keys
            { -1.12, 0.10, -0.30, 1.12, 2.00, 0.45, 0.95 },
            { -1.24, 0.00, -0.50, -1.12, 2.02, 0.47, 1.0 },
            { 1.12, 0.00, -0.50, 1.24, 2.02, 0.47, 1.0 },
            { -1.24, 0.94, -0.98, -1.10, 1.28, -0.50, 1.0 },
            { 1.10, 0.94, -0.98, 1.24, 1.28, -0.50, 1.0 },
            // The panel under the keys with a raised panel on it, the key bed and the strip in front of the keys
            { -1.12, 0.10, -0.44, 1.12, 0.94, -0.30, 1.0 },
            { -0.92, 0.24, -0.47, 0.92, 0.80, -0.44, 1.08 },
            { -1.10, 0.94, -0.98, 1.10, 1.10, -0.44, 0.95 },
            { -1.10, 0.98, -1.01, 1.10, 1.10, -0.98, 1.12 },
            // The board behind the keys, and the panel above them with two raised panels
            { -1.12, 1.10, -0.50, 1.12, 1.30, -0.40, 0.95 },
            { -1.12, 1.30, -0.40, 1.12, 1.96, -0.30, 1.0 },
            { -1.00, 1.42, -0.43, -0.06, 1.86, -0.40, 1.08 },
            { 0.06, 1.42, -0.43, 1.00, 1.86, -0.40, 1.08 },
            // Toe blocks, the pedal box and the three pedals
            { -1.24, 0.00, -0.98, -1.10, 0.10, -0.50, 1.0 },
            { 1.10, 0.00, -0.98, 1.24, 0.10, -0.50, 1.0 },
            { -0.42, 0.00, -0.50, 0.42, 0.14, -0.44, 0.95 },
            { -0.30, 0.05, -0.66, -0.20, 0.10, -0.50, 1.25 },
            { -0.05, 0.05, -0.66, 0.05, 0.10, -0.50, 1.25 },
            { 0.20, 0.05, -0.66, 0.30, 0.10, -0.50, 1.25 } },
            // Two turned legs under the arms
            leg(-1.17), leg(1.17));
    /** The lid on top, with a bright lip along its front; it turns up about its back edge. */
    private static final Shape PIANO_LID = new Shape(new double[][] {
            { -1.27, 2.02, -0.49, 1.27, 2.09, 0.50, 1.05 },
            { -1.27, 2.00, -0.54, 1.27, 2.09, -0.49, 1.15 } });
    /** The cover over the keys, with a name strip on it; it flips up about its back edge. */
    private static final Shape FALLBOARD = new Shape(new double[][] {
            { -1.09, 1.23, -0.97, 1.09, 1.29, -0.50, 1.02 },
            { -0.24, 1.29, -0.90, 0.24, 1.305, -0.84, 1.3 } });
    private static final double[][] WHITE_KEY = { { -0.069, 1.10, -0.95, 0.069, 1.17, -0.52, 1.2 } };
    private static final double[][] BLACK_KEY = { { -0.04, 1.17, -0.80, 0.04, 1.23, -0.52, 0.5 } };

    private static Mesh leg(double x) {
        return Mesh.lathe(10, 1.0, 0.0, 0.10, 0.07, 0.10, 0.07, 0.20, 0.045, 0.32, 0.085, 0.55, 0.045, 0.80, 0.065,
                0.88, 0.065, 0.94, 0.0, 0.94).moved(x, 0.0, -0.86);
    }

    /**
     * An upright piano: it drops, tumbling a little, and lands with a crash that squashes it a moment. Its lid flies up
     * and swings on its hinge, the cover over the keys flips up and bangs against the case, the keys jump up and down,
     * and notes of light float up out of it.
     */
    static Vec3 piano(ConstructPainter painter, Moment m) {
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
                    double x = (ConstructPainter.noise(k, 65, 0) - 0.5) * 1.6;
                    Vec3 at = body.at(x + 0.12 * Math.sin(age * 0.8 + k), 2.35 + 0.11 * age, 0.1);
                    note(painter, at, 0.26, 1.0 - age / 9.0);
                }
            }
        }
        return body.at(0.0, 2.1, 0.0);
    }

    /** How far a key is pressed down by the crash, at scale 1: every key jumps once, each at its own moment. */
    private static double keyDip(int key, double since) {
        double when = 0.3 + 3.5 * ConstructPainter.noise(key, 66, 0);
        return 0.035 * Math.max(0.0, 1.0 - Math.abs(since - when) / 0.8);
    }

    // ---- The anvil ----

    private static final double ANVIL_SCALE = 1.9;

    /** A blacksmith's anvil standing on its base, the horn towards +x. */
    private static final Shape ANVIL = new Shape(new double[][] {
            // The foot and a step on it
            { -0.78, 0.00, -0.52, 0.78, 0.10, 0.52, 0.95 },
            { -0.68, 0.10, -0.44, 0.68, 0.24, 0.44, 1.0 },
            // The body under the face, the hard face plate on it and the heel at the back
            { -0.80, 0.86, -0.34, 0.70, 1.16, 0.34, 1.05 },
            { -0.82, 1.16, -0.36, 0.72, 1.22, 0.36, 1.15 },
            { -1.02, 0.96, -0.24, -0.80, 1.20, 0.24, 1.0 },
            // The square hole in the face, dark
            { -0.72, 1.22, -0.07, -0.58, 1.228, 0.07, 0.35 } },
            // The waist, narrowing from the foot and widening again under the face
            frustum(0.50, 0.26, 0.24, 0.55, 0.70), frustum(0.26, 0.50, 0.55, 0.86, 0.70),
            // The horn, round and pointed, and the round hole
            Mesh.cone(12, 0.24, 0.02, 0.0, 0.78, 1.05).alongX().scaled(1.0, 0.85, 1.0).moved(0.70, 1.02, 0.0),
            Mesh.cylinder(8, 0.045, 1.22, 1.228, 0.35).moved(-0.40, 0.0, 0.0),
            // A small emblem on its front
            Mesh.torus(14, 6, 0.075, 0.022, 1.3).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 1.01, -0.35));

    /**
     * An anvil: it drops, lands with a thud that squashes it, sparks fly off its foot, and it hops once more before it
     * settles.
     */
    static Vec3 anvil(ConstructPainter painter, Moment m) {
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

    /**
     * A cut-off pyramid standing up along y with four flat sides facing x and z: {@code below} and {@code above} are
     * half as wide along x at its bottom and top, and {@code deep} times that along z.
     */
    private static Mesh frustum(double below, double above, double bottom, double top, double deep) {
        double corner = Math.sqrt(2.0);
        return Mesh.lathe(4, 1.0, 0.0, bottom, below * corner, bottom, above * corner, top, 0.0, top)
                .turned(0.0, 1.0, 0.0, 45.0).scaled(1.0, 1.0, deep);
    }

    // ---- The ton weight ----

    private static final double WEIGHT_SCALE = 1.6;
    // How far the front of the weight leans back, and where the label on it turns about.
    private static final double WEIGHT_LEAN = Math.toDegrees(Math.atan2(0.92 * 0.70 - 0.62 * 0.70, 1.14));
    private static final double LABEL_Y = 0.70;
    private static final double LABEL_Z = -(0.92 - (0.92 - 0.62) * (LABEL_Y - 0.16) / 1.14) * 0.70;

    /** A cartoon weight: a plinth, a body narrowing upwards, a cap and a ring to lift it by. */
    private static final Shape WEIGHT = new Shape(new double[][] {
            { -1.00, 0.00, -0.70, 1.00, 0.16, 0.70, 1.0 },
            { -0.66, 1.30, -0.46, 0.66, 1.40, 0.46, 1.05 } },
            frustum(0.92, 0.62, 0.16, 1.30, 0.70),
            Mesh.torus(18, 8, 0.36, 0.08, 1.1).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 1.70, 0.0));
    /** The label on its front with "1 TON" raised on it, upright; it is turned back to lie on the leaning front. */
    private static final Shape LABEL = new Shape(join(
            new double[][] { { -0.60, 0.42, LABEL_Z - 0.03, 0.60, 0.98, LABEL_Z + 0.01, 1.1 } },
            text("1 TON", 0.55, 0.06, LABEL_Z - 0.06, LABEL_Z - 0.03, 1.35)));

    /** A ton weight: it drops, lands with a heavy thud that squashes it flat a moment, and settles. */
    static Vec3 weight(ConstructPainter painter, Moment m) {
        Frame body = SlamPainter.squashed(SlamPainter.dropped(m, WEIGHT_SCALE, 0.0, m.right(), 0.0), m, 0.22);
        SlamPainter.marker(painter, m, WEIGHT_SCALE);
        SlamPainter.piece(painter, WEIGHT, body, m);
        SlamPainter.piece(painter, LABEL, body.turned(0.0, LABEL_Y, LABEL_Z, 1.0, 0.0, 0.0,
                Math.toRadians(WEIGHT_LEAN)), m);
        return body.at(0.0, 2.14, 0.0);
    }

    // ---- The TNT ----

    private static final double TNT_SCALE = 1.6;
    // The tick it lands, on the server too, and the points its fuse runs through, from the top of the block.
    private static final double TNT_LANDS = 9.0;
    private static final Vec3[] FUSE = { new Vec3(0.0, 1.50, 0.0), new Vec3(0.0, 1.62, 0.02),
            new Vec3(0.03, 1.72, 0.05), new Vec3(0.08, 1.80, 0.06), new Vec3(0.14, 1.85, 0.04),
            new Vec3(0.20, 1.87, 0.0) };

    /** A bundle of sixteen sticks of dynamite, a paper band round it with "TNT" on every side. */
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

    /**
     * A block of TNT: it drops, lands with a bounce, swells and flashes while its fuse burns down, and blows apart as
     * it strikes.
     */
    static Vec3 tnt(ConstructPainter painter, Moment m) {
        double s = m.scale(TNT_SCALE);
        // It drops early, straight out of where it took shape, so its fuse has time to burn down on the ground.
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
        // The fuse burns down from its end, piece by piece, a spark at the end.
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

    /** A round rod from {@code a} to {@code b}. */
    private static Mesh rod(Vec3 a, Vec3 b, double radius) {
        Vec3 way = b.subtract(a);
        return Mesh.cylinder(6, radius, 0.0, way.length(), 1.1).pointing(way.x, way.y, way.z).moved(a.x, a.y, a.z);
    }

    // ---- The toy brick ----

    private static final double BRICK_SCALE = 1.5;

    /** A toy brick: a hollow shell with three tubes inside it and eight studs on top. */
    private static final Shape BRICK = new Shape(new double[][] {
            // The four walls and the top between them
            { -1.60, 0.00, -0.80, 1.60, 0.96, -0.70, 1.0 },
            { -1.60, 0.00, 0.70, 1.60, 0.96, 0.80, 1.0 },
            { -1.60, 0.00, -0.70, -1.50, 0.96, 0.70, 1.0 },
            { 1.50, 0.00, -0.70, 1.60, 0.96, 0.70, 1.0 },
            { -1.50, 0.78, -0.70, 1.50, 0.96, 0.70, 1.0 },
            // The inside, darker
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

    /** A toy brick: it drops, tumbling a little, lands with a squash and bounces a few times, lower every time. */
    static Vec3 brick(ConstructPainter painter, Moment m) {
        Frame base = SlamPainter.dropped(m, BRICK_SCALE, 0.0, m.forward(), 0.7);
        double since = m.since();
        double hop = since > 0.0 ? 0.45 * Math.exp(-0.35 * since) * Math.abs(Math.sin(0.9 * since)) : 0.0;
        Frame body = SlamPainter.squashed(base, m, 0.12).moved(0.0, hop, 0.0);
        SlamPainter.marker(painter, m, BRICK_SCALE);
        SlamPainter.piece(painter, BRICK, body, m);
        return body.at(0.0, 1.16, 0.0);
    }

    // ---- The stamp ----

    private static final double STAMP_SCALE = 1.5;

    /**
     * A rubber stamp: the lantern emblem raised under its rubber, the mount with a bevel round its top, and a turned
     * wooden handle with a band round its neck.
     */
    private static final Shape STAMP = new Shape(new double[][] {
            { -1.10, 0.06, -0.80, 1.10, 0.20, 0.80, 1.2 },
            { -1.00, 0.20, -0.72, 1.00, 0.50, 0.72, 0.95 },
            // The bars of the emblem, under the rubber
            { -0.55, 0.00, -0.68, 0.55, 0.06, -0.56, 1.25 },
            { -0.55, 0.00, 0.56, 0.55, 0.06, 0.68, 1.25 } },
            Mesh.ring(20, 1.25, 0.30, 0.0, 0.46, 0.0, 0.46, 0.06, 0.30, 0.06),
            frustum(1.00, 0.70, 0.50, 0.62, 0.72),
            Mesh.lathe(14, 1.0, 0.0, 0.62, 0.30, 0.62, 0.30, 0.70, 0.18, 0.78, 0.16, 1.10, 0.24, 1.25, 0.42, 1.45,
                    0.48, 1.65, 0.42, 1.85, 0.25, 1.98, 0.0, 2.02),
            Mesh.torus(14, 6, 0.17, 0.035, 1.25).moved(0.0, 0.95, 0.0));

    /**
     * A rubber stamp: it drops and stamps the ground, rocks a little as it presses down, lifts again, and leaves the
     * lantern emblem glowing in the ground where it stood.
     */
    static Vec3 stamp(ConstructPainter painter, Moment m) {
        double since = m.since();
        double lift = m.struck() ? 1.1 * ConstructPainter.smooth((since - 1.5) / 5.0) : 0.0;
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

    /** The lantern emblem lying in the ground, as a stamp leaves it: a ring with a bar above and below it. */
    private static void print(ConstructPainter painter, Vec3 at, Vec3 right, Vec3 forward, double size,
            double strength) {
        if (strength <= 0.0) {
            return;
        }
        int edge = ConstructPainter.alpha(0.95 * strength);
        int halo = ConstructPainter.alpha(0.45 * strength);
        painter.circle(at, right, forward, 0.46 * size, 0.08, 0.3, edge, halo);
        painter.circle(at, right, forward, 0.30 * size, 0.06, 0.25, edge, halo);
        for (double bar : new double[] { -0.62, 0.62 }) {
            Vec3 middle = at.add(forward.scale(bar * size));
            painter.edge(middle.subtract(right.scale(0.55 * size)), middle.add(right.scale(0.55 * size)), 0.12,
                    strength);
        }
    }

    // ---- Shared ----

    /**
     * Raised letters made of pixels, standing up in the plane of x and y: {@code text} centred on x = 0 with its foot
     * at y = {@code bottom}, each pixel {@code pixel} wide and sticking out from z = {@code back} to z = {@code front}.
     * The letters run along +x, his right, so they read from left to right on a side that faces him (-z).
     */
    private static double[][] text(String text, double bottom, double pixel, double back, double front,
            double bright) {
        int width = -1;
        for (char c : text.toCharArray()) {
            width += FONT.get(c)[0].length() + 1;
        }
        List<double[]> boxes = new ArrayList<>();
        double x = -0.5 * width * pixel;
        for (char c : text.toCharArray()) {
            String[] glyph = FONT.get(c);
            boolean[][] used = new boolean[glyph.length][glyph[0].length()];
            for (int row = 0; row < glyph.length; row++) {
                String line = glyph[row];
                for (int from = 0; from < line.length(); from++) {
                    if (line.charAt(from) != '#' || used[row][from]) {
                        continue;
                    }
                    int to = from;
                    while (to < line.length() && line.charAt(to) == '#' && !used[row][to]) {
                        to++;
                    }
                    // As few boxes as it takes: a run of pixels goes on down as long as the rows below have it too.
                    int last = row;
                    while (last + 1 < glyph.length && filled(glyph[last + 1], used[last + 1], from, to)) {
                        last++;
                    }
                    for (int r = row; r <= last; r++) {
                        for (int i = from; i < to; i++) {
                            used[r][i] = true;
                        }
                    }
                    double y = bottom + (glyph.length - 1 - last) * pixel;
                    boxes.add(new double[] { x + from * pixel, y, Math.min(back, front), x + to * pixel,
                            y + (last - row + 1) * pixel, Math.max(back, front), bright });
                    from = to - 1;
                }
            }
            x += (glyph[0].length() + 1) * pixel;
        }
        return boxes.toArray(double[][]::new);
    }

    /** Whether a row of a letter has a pixel from {@code from} up to {@code to} that no box has taken yet. */
    private static boolean filled(String line, boolean[] used, int from, int to) {
        for (int i = from; i < to; i++) {
            if (line.charAt(i) != '#' || used[i]) {
                return false;
            }
        }
        return true;
    }



    /** Boxes one after the other in one list. */
    private static double[][] join(double[][]... parts) {
        int count = 0;
        for (double[][] part : parts) {
            count += part.length;
        }
        double[][] all = new double[count][];
        int n = 0;
        for (double[][] part : parts) {
            for (double[] box : part) {
                all[n++] = box;
            }
        }
        return all;
    }

    /**
     * Boxes four times over: as they are and turned a quarter, a half and three quarters round the y axis, so what
     * is on the side facing him is on every side.
     */
    private static double[][] quarters(double[][] boxes) {
        double[][] all = new double[boxes.length * 4][];
        for (int b = 0; b < boxes.length; b++) {
            double[] box = boxes[b];
            double x0 = box[0];
            double z0 = box[2];
            double x1 = box[3];
            double z1 = box[5];
            for (int q = 0; q < 4; q++) {
                all[4 * b + q] = new double[] { x0, box[1], z0, x1, box[4], z1, box[6] };
                // A quarter turn takes (x, z) to (z, -x).
                double nx0 = z0;
                double nx1 = z1;
                double nz0 = -x1;
                double nz1 = -x0;
                x0 = nx0;
                x1 = nx1;
                z0 = nz0;
                z1 = nz1;
            }
        }
        return all;
    }

    /**
     * A note of music drawn in light, facing the camera: a round head, a stem up from it and a flag. Light, not a
     * construct, so it fades.
     */
    private static void note(ConstructPainter painter, Vec3 at, double size, double strength) {
        Vec3 view = painter.camera().subtract(at);
        Vec3 side = view.cross(SlamPainter.UP);
        if (side.lengthSqr() < 1.0E-6) {
            return;
        }
        side = side.normalize();
        Vec3 up = SlamPainter.UP;
        painter.circle(at, side, up, 0.28 * size, 0.07 * size, 0.3 * size, ConstructPainter.alpha(0.95 * strength),
                ConstructPainter.alpha(0.5 * strength));
        painter.flare(at, 0.35 * size, 0.6 * strength);
        Vec3 stemFoot = at.add(side.scale(0.26 * size));
        Vec3 stemTop = stemFoot.add(up.scale(1.1 * size));
        painter.edge(stemFoot, stemTop, 0.07 * size, strength);
        painter.edge(stemTop, stemTop.add(side.scale(0.35 * size)).subtract(up.scale(0.35 * size)), 0.07 * size,
                strength);
    }
}
