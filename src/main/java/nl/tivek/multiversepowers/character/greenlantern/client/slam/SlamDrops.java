package nl.tivek.multiversepowers.character.greenlantern.client.slam;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FlareLight;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter.Moment;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The landing-slam constructs that drop out of the sky (see {@link SlamPainter}), apart from the cartoon ones (see
 * {@link SlamCartoon}): a war hammer, a boot, his lantern, an anchor on its chain, a spiked ball, a barbell, a bell, a
 * meteor, a sword and a volley of rockets. Unless it says otherwise a shape stands on y = 0 and faces -z, the way
 * towards Green Lantern, with x to his right; in blocks at scale 1.
 */
final class SlamDrops {
    private SlamDrops() {
    }

    // ---- The war hammer ----

    private static final double HAMMER_SCALE = 1.8;
    // How far the head reaches below the middle of the shape, and the height of the head's own middle.
    private static final double HAMMER_FOOT = 0.73;
    private static final double HEAD_Y = -0.34;

    /**
     * A war hammer standing on its head: an eight-sided head along x with wider striking faces and a band round its
     * middle with the emblem on it, a handle up along y with a wrapped grip, and a pommel.
     */
    private static final Shape HAMMER = Shape.of(
            head(0.38, -0.85, 0.85, 1.0), head(0.42, 0.85, 1.05, 1.2), head(0.42, -1.05, -0.85, 1.2),
            head(0.41, -0.16, 0.16, 1.15),
            Mesh.torus(12, 6, 0.11, 0.03, 1.35).alongZ().moved(0.0, HEAD_Y, -0.40),
            Mesh.cylinder(10, 0.10, 0.0, 2.35, 0.95),
            wrap(0.105, 1.55), wrap(0.105, 1.69), wrap(0.105, 1.83), wrap(0.105, 1.97), wrap(0.105, 2.11),
            wrap(0.105, 2.25),
            Mesh.lathe(12, 1.2, 0.0, 2.33, 0.15, 2.33, 0.19, 2.42, 0.19, 2.52, 0.14, 2.60, 0.0, 2.62));

    /** A piece of the hammer's head: eight-sided, along x from {@code from} to {@code to}, a flat side down. */
    private static Mesh head(double radius, double from, double to, double bright) {
        return Mesh.cylinder(8, radius, from, to, bright).turned(0.0, 1.0, 0.0, 22.5).alongX().moved(0.0, HEAD_Y,
                0.0);
    }

    /** A turn of the wrapping round a grip {@code radius} thick, at height {@code y}. */
    private static Mesh wrap(double radius, double y) {
        return Mesh.torus(10, 4, radius, 0.022, 1.15).moved(0.0, y, 0.0);
    }

    /**
     * A war hammer: it takes shape lying across the air before him, so he sees all of it, swings round head down as it
     * drops, and lands with sparks flying off its head.
     */
    static Vec3 hammer(LanternPainter painter, Moment m) {
        Frame frame = SlamPainter.dropped(m, HAMMER_SCALE, HAMMER_FOOT, m.forward(), 1.35);
        SlamPainter.marker(painter, m, HAMMER_SCALE);
        SlamPainter.piece(painter, HAMMER, frame, m);
        SlamPainter.sparks(painter, m.ground().add(0.0, 0.05, 0.0), m.since(), 12, 0.3, 45);
        return frame.at(0.0, 2.6, 0.0);
    }

    // ---- The boot ----

    private static final double BOOT_SCALE = 1.35;
    // How far its toe is up as it comes down, heel first.
    private static final double BOOT_TOE = Math.toRadians(22.0);

    /** A boot standing on its sole, the toe away from him: a heel, a round toe, a laced tongue and a pull tab. */
    private static final Shape BOOT = new Shape(new double[][] {
            // The sole, the heel under its back and the tread under its front
            { -0.50, 0.10, -0.80, 0.50, 0.22, 1.00, 1.1 },
            { -0.46, 0.00, -0.80, 0.46, 0.10, -0.32, 1.15 },
            { -0.50, 0.00, 0.20, 0.50, 0.10, 1.00, 1.15 },
            // The foot, the shaft up the leg, the cuff round its top and the tongue down its front
            { -0.48, 0.22, -0.78, 0.48, 0.62, 0.55, 1.0 },
            { -0.48, 0.62, -0.78, 0.48, 1.95, 0.10, 0.95 },
            { -0.54, 1.95, -0.84, 0.54, 2.12, 0.16, 1.2 },
            { -0.28, 0.62, 0.10, 0.28, 2.02, 0.16, 1.05 },
            // The stiff back of the heel and the pull tab
            { -0.50, 0.22, -0.84, 0.50, 0.70, -0.78, 1.1 },
            { -0.12, 1.80, -0.87, 0.12, 2.32, -0.84, 1.2 } },
            Mesh.lathe(14, 1.0, 0.0, 0.0, 1.0, 0.0, 0.92, 0.38, 0.70, 0.72, 0.38, 0.93, 0.0, 1.0)
                    .scaled(0.48, 0.40, 0.45).moved(0.0, 0.22, 0.55),
            lace(0.85, 28.0), lace(0.85, -28.0), lace(1.15, 28.0), lace(1.15, -28.0), lace(1.45, 28.0),
            lace(1.45, -28.0), lace(1.75, 28.0), lace(1.75, -28.0));

    private static Mesh lace(double y, double degrees) {
        return Mesh.box(-0.22, -0.025, 0.0, 0.22, 0.025, 0.05, 1.25).turned(0.0, 0.0, 1.0, degrees)
                .moved(0.0, y, 0.14);
    }

    /** A boot stamping down: it drops heel first, the toe slaps down after, and it squashes a moment. */
    static Vec3 boot(LanternPainter painter, Moment m) {
        Frame base = SlamPainter.dropped(m, BOOT_SCALE, 0.0, m.right(), 0.0);
        double since = m.since();
        double toe = since < 0.0 ? BOOT_TOE : BOOT_TOE * Math.max(0.0, 1.0 - since / 1.2);
        // Toe up: its back edge stays on the ground and its front turns up.
        Frame body = SlamPainter.squashed(base, m, 0.1).turned(0.0, 0.0, -0.80, 1.0, 0.0, 0.0, -toe);
        SlamPainter.marker(painter, m, BOOT_SCALE);
        SlamPainter.piece(painter, BOOT, body, m);
        return body.at(0.0, 2.1, 0.0);
    }

    // ---- The lantern ----

    private static final double LANTERN_SCALE = 1.8;
    // Where the handle turns about.
    private static final double HANDLE_Y = 2.0;

    /**
     * His lantern, the power battery: a turned base, the light inside it between four posts with two bands round
     * them, the emblem on its front, and a domed top.
     */
    private static final Shape LANTERN = new Shape(new double[][] {
            // The bars of the emblem, above and below its ring
            { -0.20, 1.30, -0.53, 0.20, 1.36, -0.45, 1.4 },
            { -0.20, 0.72, -0.53, 0.20, 0.78, -0.45, 1.4 } },
            Mesh.lathe(16, 1.0, 0.0, 0.0, 0.78, 0.0, 0.78, 0.12, 0.70, 0.16, 0.66, 0.30, 0.58, 0.38, 0.0, 0.38),
            Mesh.cylinder(16, 0.42, 0.38, 1.70, 1.3),
            post(0.39, 0.39), post(-0.39, 0.39), post(0.39, -0.39), post(-0.39, -0.39),
            Mesh.torus(16, 6, 0.47, 0.045, 1.2).moved(0.0, 0.62, 0.0),
            Mesh.torus(16, 6, 0.47, 0.045, 1.2).moved(0.0, 1.46, 0.0),
            Mesh.torus(14, 6, 0.15, 0.035, 1.45).alongZ().moved(0.0, 1.04, -0.48),
            Mesh.lathe(16, 1.0, 0.0, 1.70, 0.62, 1.70, 0.62, 1.80, 0.50, 1.90, 0.44, 2.02, 0.20, 2.10, 0.0, 2.12));
    /** The lantern's handle: an arch over its top, from one side to the other. */
    private static final Shape LANTERN_HANDLE = Shape.of(Mesh.tube(false, 6, 0.05, 1.1, arch(0.40, HANDLE_Y, 12)));

    /**
     * His lantern as a construct of its own, its foot at the middle of {@code frame}: whole, or {@code apart} of the
     * way broken into solid pieces. The Lantern Flare shapes it over his fist (see {@link FlareLight}).
     */
    static void lantern(LanternPainter painter, Frame frame, double apart, double bright) {
        if (apart > 0.0) {
            painter.shattered(LANTERN, frame, apart, bright);
            painter.shattered(LANTERN_HANDLE, frame, apart, bright);
        } else {
            painter.shape(LANTERN, frame, 1.0, bright);
            painter.shape(LANTERN_HANDLE, frame, 1.0, bright);
        }
    }

    private static Mesh post(double x, double z) {
        return Mesh.cylinder(8, 0.07, 0.38, 1.70, 1.1).moved(x, 0.0, z);
    }

    /** Points on half a circle over (0, {@code y}), in the plane of x and y, from +x over the top to -x. */
    private static Vec3[] arch(double radius, double y, int pieces) {
        Vec3[] points = new Vec3[pieces + 1];
        for (int i = 0; i <= pieces; i++) {
            double angle = Math.PI * i / pieces;
            points[i] = new Vec3(radius * Math.cos(angle), y + radius * Math.sin(angle), 0.0);
        }
        return points;
    }

    /** His lantern, the power battery, dropping out of the sky, its light burning inside it and its handle swinging. */
    static Vec3 lantern(LanternPainter painter, Moment m) {
        Frame frame = SlamPainter.dropped(m, LANTERN_SCALE, 0.0, m.right(), 0.0);
        SlamPainter.marker(painter, m, LANTERN_SCALE);
        SlamPainter.piece(painter, LANTERN, frame, m);
        double since = m.since();
        double swing = since < 0.0 ? 0.35 * (1.0 - m.go())
                : 0.7 * Math.sin(1.3 * since) * Math.exp(-0.25 * since);
        SlamPainter.piece(painter, LANTERN_HANDLE, frame.turned(0.0, HANDLE_Y, 0.0, 1.0, 0.0, 0.0, swing), m);
        if (m.apart() <= 0.0) {
            painter.flare(frame.at(0.0, 1.05, 0.0), 0.8 + 0.8 * m.flash(), 0.9);
        }
        return frame.at(0.0, 2.4, 0.0);
    }

    // ---- The anchor ----

    private static final double ANCHOR_SCALE = 1.7;
    // The arms: a curve round this middle, this far out, and how far round they reach either side of the bottom.
    private static final double ARM_RADIUS = 1.1;
    private static final double ARM_Y = 1.25;
    private static final double ARM_FROM = 200.0;
    private static final double ARM_TO = 340.0;
    // Where the chain starts, above its ring, how far apart its links are and how many there are.
    private static final double ANCHOR_TOP = 3.2;
    private static final double LINK_STEP = 0.36;
    private static final int LINKS = 12;

    /**
     * A ship's anchor standing on its crown: curved arms with a spade-shaped fluke at each end, the shank up from the
     * crown, a stock across it and a ring on top.
     */
    private static final Shape ANCHOR = Shape.of(
            Mesh.tube(false, 8, 0.12, 1.0, armPath()),
            fluke(ARM_FROM, -1.0), fluke(ARM_TO, 1.0),
            Mesh.ball(10, 6, 0.2, 1.1).moved(0.0, 0.18, 0.0),
            Mesh.cylinder(10, 0.11, 0.15, 2.80, 1.0),
            Mesh.cylinder(10, 0.08, -0.85, 0.85, 1.05).alongX().moved(0.0, 2.35, 0.0),
            Mesh.ball(8, 5, 0.12, 1.15).moved(0.88, 2.35, 0.0), Mesh.ball(8, 5, 0.12, 1.15).moved(-0.88, 2.35, 0.0),
            Mesh.torus(16, 6, 0.20, 0.05, 1.1).alongZ().moved(0.0, 2.95, 0.0));
    /** One link of the chain, round its middle: an oval standing along y in the plane of x and y. */
    private static final Shape LINK = Shape.of(link());
    /** The same link turned a quarter round, the way every other link of a chain hangs. */
    private static final Shape LINK_TURNED = Shape.of(link().turned(0.0, 1.0, 0.0, 90.0));

    private static Vec3[] armPath() {
        int pieces = 16;
        Vec3[] points = new Vec3[pieces + 1];
        for (int i = 0; i <= pieces; i++) {
            double angle = Math.toRadians(Mth.lerp((double) i / pieces, ARM_FROM, ARM_TO));
            points[i] = new Vec3(ARM_RADIUS * Math.cos(angle), ARM_Y + ARM_RADIUS * Math.sin(angle), 0.0);
        }
        return points;
    }

    /** A fluke at the end of an arm at {@code degrees} round the arms, pointing on the way the arm runs out there. */
    private static Mesh fluke(double degrees, double out) {
        double angle = Math.toRadians(degrees);
        double x = ARM_RADIUS * Math.cos(angle);
        double y = ARM_Y + ARM_RADIUS * Math.sin(angle);
        double tx = -Math.sin(angle) * out;
        double ty = Math.cos(angle) * out;
        // Its point, its left, its back and its right, counter-clockwise.
        return Mesh.prism(-0.05, 0.05, 1.15, x + 0.42 * tx, y + 0.42 * ty, x - 0.24 * ty, y + 0.24 * tx,
                x - 0.30 * tx, y - 0.30 * ty, x + 0.24 * ty, y - 0.24 * tx);
    }

    /** A chain link round its middle: an oval standing along y in the plane of x and y. */
    static Mesh link() {
        double half = 0.14;
        double radius = 0.11;
        int pieces = 6;
        Vec3[] points = new Vec3[2 * (pieces + 1)];
        for (int i = 0; i <= pieces; i++) {
            double angle = Math.PI * i / pieces;
            points[i] = new Vec3(radius * Math.cos(angle), half + radius * Math.sin(angle), 0.0);
            points[pieces + 1 + i] = new Vec3(-radius * Math.cos(angle), -half - radius * Math.sin(angle), 0.0);
        }
        return Mesh.tube(true, 6, 0.04, 1.05, points);
    }

    /**
     * A ship's anchor dropping crown first, its chain running on up into the sky link by link. Once it has struck the
     * chain comes down after it, link by link, and piles up round its foot.
     */
    static Vec3 anchor(LanternPainter painter, Moment m) {
        Frame frame = SlamPainter.dropped(m, ANCHOR_SCALE, 0.0, m.right(), 0.0);
        SlamPainter.marker(painter, m, 1.6);
        SlamPainter.piece(painter, ANCHOR, frame, m);
        double since = Math.max(0.0, m.since());
        for (int i = 0; i < LINKS; i++) {
            double high = ANCHOR_TOP + 0.25 + LINK_STEP * i;
            double around = i * 2.1;
            double low = 0.05 + 0.03 * (i % 3);
            double dt = Math.max(0.0, since - 0.1 * i);
            double y = high - 0.25 * dt * dt;
            double down = y <= low ? 1.0 : (high - y) / (high - low);
            double spread = (0.45 + 0.03 * i) * down * down;
            Vec3 at = new Vec3(spread * Math.cos(around), Math.max(low, y), spread * Math.sin(around));
            Vec3 axis = frame.right().scale(Math.cos(around)).add(frame.forward().scale(Math.sin(around)))
                    .normalize();
            SlamPainter.piece(painter, (i & 1) == 0 ? LINK : LINK_TURNED,
                    SlamPainter.loose(frame, at, axis, Mth.HALF_PI * down), m);
        }
        return frame.at(0.0, ANCHOR_TOP, 0.0);
    }

    // ---- The spiked ball ----

    private static final double MACE_SCALE = 1.6;
    // How far the tips of its spikes reach from its middle.
    private static final double MACE_REACH = 1.35;

    /** A spiked ball round its middle: a ball, a band round it, and spikes along its axes and to its corners. */
    private static final Shape MACE = Shape.of(spiked());

    private static Mesh[] spiked() {
        Vec3[] ways = new Vec3[14];
        int n = 0;
        for (int axis = 0; axis < 3; axis++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                ways[n++] = new Vec3(axis == 0 ? sign : 0, axis == 1 ? sign : 0, axis == 2 ? sign : 0);
            }
        }
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    ways[n++] = new Vec3(x, y, z);
                }
            }
        }
        Mesh[] meshes = new Mesh[ways.length + 2];
        meshes[0] = Mesh.ball(18, 10, 0.85, 0.95);
        meshes[1] = Mesh.torus(24, 6, 0.86, 0.06, 1.2);
        for (int i = 0; i < ways.length; i++) {
            meshes[2 + i] = Mesh.cone(8, 0.17, 0.0, 0.70, MACE_REACH, 1.15).pointing(ways[i].x, ways[i].y,
                    ways[i].z);
        }
        return meshes;
    }

    /** A spiked ball, tumbling as it drops, that lands with its spikes in the ground and rocks there a moment. */
    static Vec3 mace(LanternPainter painter, Moment m) {
        double s = m.scale(MACE_SCALE);
        double height = SlamPainter.drop(m) + MACE_REACH * s - (m.struck() ? 0.5 * s : 0.0);
        Vec3 center = m.ground().add(0.0, height, 0.0);
        Vec3 axis = m.right().add(m.forward()).normalize();
        double turn = 5.0 * (1.0 - m.go());
        Frame frame = new Frame(center, Vectors.spin(m.right(), axis, turn),
                Vectors.spin(SlamPainter.UP, axis, turn), Vectors.spin(m.forward(), axis, turn), s);
        if (m.struck()) {
            double rock = 0.12 * Math.sin(1.1 * m.since()) * Math.exp(-0.25 * m.since());
            frame = frame.turned(0.0, -MACE_REACH, 0.0, 1.0, 0.0, 0.0, rock);
        }
        SlamPainter.marker(painter, m, 1.4);
        SlamPainter.piece(painter, MACE, frame, m);
        return center;
    }

    // ---- The barbell ----

    private static final double BARBELL_SCALE = 1.75;
    // How far its plates reach below its middle.
    private static final double BARBELL_FOOT = 0.85;

    /**
     * A barbell lying along x round its middle: a bar with knurled rings on its grip, a collar, a big and a small
     * plate at each end, and end caps.
     */
    private static final Shape BARBELL = Shape.of(
            Mesh.cylinder(12, 0.065, -2.0, 2.0, 1.0).alongX(),
            knurl(-0.75), knurl(-0.45), knurl(-0.15), knurl(0.15), knurl(0.45), knurl(0.75),
            Mesh.cylinder(12, 0.15, 1.02, 1.20, 1.15).alongX(), Mesh.cylinder(12, 0.15, -1.20, -1.02, 1.15).alongX(),
            plate(0.82, 0.15, 1.35), plate(0.82, 0.15, -1.35), rim(0.82, 0.15, 1.35), rim(0.82, 0.15, -1.35),
            plate(0.60, 0.09, 1.61), plate(0.60, 0.09, -1.61), rim(0.60, 0.09, 1.61), rim(0.60, 0.09, -1.61),
            Mesh.cylinder(12, 0.09, 1.70, 1.85, 1.2).alongX(), Mesh.cylinder(12, 0.09, -1.85, -1.70, 1.2).alongX());

    private static Mesh knurl(double x) {
        return Mesh.torus(10, 4, 0.068, 0.014, 1.2).alongX().moved(x, 0.0, 0.0);
    }

    /** A weight plate on the bar: a disc {@code radius} wide and twice {@code half} thick, with a hole for the bar. */
    private static Mesh plate(double radius, double half, double x) {
        return Mesh.ring(28, 1.0, 0.075, -half, radius, -half, radius, half, 0.075, half).alongX().moved(x, 0.0, 0.0);
    }

    /** The raised lip round the edge of a plate, and the raised hub in its middle. */
    private static Mesh rim(double radius, double half, double x) {
        return Mesh.ring(28, 1.15, radius - 0.09, -half - 0.03, radius, -half - 0.03, radius, half + 0.03,
                radius - 0.09, half + 0.03).alongX().moved(x, 0.0, 0.0);
    }

    /** A barbell dropping out of the sky, tumbling end over end, that lands on its plates and bounces. */
    static Vec3 barbell(LanternPainter painter, Moment m) {
        Frame base = SlamPainter.dropped(m, BARBELL_SCALE, BARBELL_FOOT, m.forward(), 0.9);
        double since = m.since();
        double hop = since > 0.0 ? 0.35 * Math.exp(-0.4 * since) * Math.abs(Math.sin(1.0 * since)) : 0.0;
        Frame frame = base.moved(0.0, hop, 0.0);
        SlamPainter.marker(painter, m, BARBELL_SCALE);
        SlamPainter.piece(painter, BARBELL, frame, m);
        return frame.at(0.0, 0.0, 0.0);
    }

    // ---- The bell ----

    private static final double BELL_SCALE = 1.6;
    // Where the clapper hangs from inside the bell.
    private static final double CLAPPER_Y = 1.88;

    /**
     * A bell standing on its rim, open side down: a hollow wall widening from its crown to its lip, bands round it
     * and a loop on top to hang it by.
     */
    private static final Shape BELL = Shape.of(
            Mesh.ring(24, 1.0,
                    // The outside, from the lip up to the crown
                    1.08, 0.00, 1.06, 0.10, 0.96, 0.30, 0.86, 0.60, 0.80, 0.95, 0.74, 1.30, 0.68, 1.62, 0.56, 1.86,
                    0.36, 1.98, 0.14, 2.02,
                    // The inside, from the crown back down to the lip
                    0.12, 1.90, 0.34, 1.86, 0.50, 1.74, 0.60, 1.50, 0.66, 1.20, 0.72, 0.88, 0.80, 0.56, 0.90, 0.26,
                    0.98, 0.06, 0.97, 0.00),
            Mesh.cylinder(12, 0.20, 1.95, 2.12, 1.05),
            Mesh.torus(24, 6, 1.06, 0.05, 1.25).moved(0.0, 0.08, 0.0),
            Mesh.torus(24, 6, 0.88, 0.035, 1.2).moved(0.0, 0.55, 0.0),
            Mesh.torus(24, 6, 0.70, 0.035, 1.2).moved(0.0, 1.55, 0.0),
            Mesh.torus(14, 6, 0.18, 0.06, 1.1).alongZ().moved(0.0, 2.28, 0.0));
    /** The clapper hanging inside it: a rod and a ball at its end. */
    private static final Shape CLAPPER = Shape.of(Mesh.cylinder(8, 0.04, 0.50, CLAPPER_Y, 1.0),
            Mesh.ball(10, 6, 0.17, 1.15).moved(0.0, 0.45, 0.0));

    /**
     * A bell dropping on its rim, swinging a little on the way, that rings when it lands: it shivers, its clapper
     * swings, and rings of sound run out from it.
     */
    static Vec3 bell(LanternPainter painter, Moment m) {
        Frame frame = SlamPainter.dropped(m, BELL_SCALE, 0.0, m.forward(), 0.4);
        double since = m.since();
        if (m.struck() && m.apart() <= 0.0) {
            double ring = 0.05 * Math.sin(since * 3.0) * Math.max(0.0, 1.0 - since / 8.0);
            frame = new Frame(frame.center(), Vectors.spin(frame.right(), m.forward(), ring),
                    Vectors.spin(frame.up(), m.forward(), ring), frame.forward(), frame.scale());
        }
        SlamPainter.marker(painter, m, BELL_SCALE);
        SlamPainter.piece(painter, BELL, frame, m);
        double swing = m.struck() ? 0.5 * Math.sin(1.5 * since) * Math.exp(-0.2 * since) : 0.0;
        SlamPainter.piece(painter, CLAPPER, frame.turned(0.0, CLAPPER_Y, 0.0, 0.0, 0.0, 1.0, swing), m);
        if (m.struck()) {
            for (int k = 0; k < 3; k++) {
                double age = since - 1.5 * k;
                if (age > 0.0 && age < 8.0) {
                    double fade = 1.0 - age / 8.0;
                    painter.circle(frame.at(0.0, 0.9, 0.0), m.right(), m.forward(),
                            (1.3 + 0.35 * age) * frame.scale(), 0.05, 0.25, Colors.alpha(0.8 * fade),
                            Colors.alpha(0.4 * fade));
                }
            }
        }
        return frame.at(0.0, 2.38, 0.0);
    }

    // ---- The meteor ----

    private static final double METEOR_SCALE = 1.9;
    // How many sides and rings its rock has, so its cracks can find their way over its corners.
    private static final int ROCK_SIDES = 14;
    private static final int ROCK_RINGS = 9;
    private static final Mesh ROCK = Mesh.lump(ROCK_SIDES, ROCK_RINGS, 1.0, 0.35, 17, 0.9);

    /** A rough rock round its middle, with two smaller lumps stuck on it. */
    private static final Shape METEOR = Shape.of(ROCK,
            Mesh.lump(8, 5, 0.45, 0.4, 18, 0.95).moved(0.55, 0.5, 0.3),
            Mesh.lump(8, 5, 0.40, 0.4, 19, 0.95).moved(-0.5, -0.45, -0.4));

    /**
     * A meteor: it streaks in out of the sky from far ahead of him, burning and tumbling, cracks of fire glowing on
     * it, and slams half into the ground where it strikes.
     */
    static Vec3 meteor(LanternPainter painter, Moment m) {
        double s = m.scale(METEOR_SCALE);
        Vec3 end = m.ground().add(0.0, 0.55 * s - (m.struck() ? 0.45 * s : 0.0), 0.0);
        Vec3 start = m.ground().add(m.forward().scale(16.0)).add(m.right().scale(5.0)).add(0.0, 14.0, 0.0);
        // It is on its way from the moment it takes shape, far off in the sky, so he sees it coming.
        double p = Mth.clamp((m.t() - 1.0) / (LandingSlam.IMPACT_TICK - 1.0), 0.0, 1.0);
        Vec3 at = start.lerp(end, p * p);
        Vec3 axis = m.right().add(0.0, 0.4, 0.0).normalize();
        double turn = 6.0 * (1.0 - p);
        Frame frame = new Frame(at, Vectors.spin(m.right(), axis, turn),
                Vectors.spin(SlamPainter.UP, axis, turn), Vectors.spin(m.forward(), axis, turn), s);
        SlamPainter.marker(painter, m, 1.4);
        SlamPainter.piece(painter, METEOR, frame, m);
        if (m.apart() <= 0.0) {
            double glow = m.struck() ? Math.max(0.3, 1.0 - m.since() / 10.0) : 1.0;
            crack(painter, frame, 3, 0, glow);
            crack(painter, frame, 5, 7, glow);
        }
        if (!m.struck()) {
            // It burns as it comes in: a blaze on it, a long tail of fire behind it, and embers flying off.
            Vec3 back = start.subtract(end).normalize();
            painter.flare(at, 1.6 * s, 1.0);
            for (int k = 0; k < 4; k++) {
                double from = (0.7 + 1.6 * k) * s;
                painter.edge(at.add(back.scale(from)), at.add(back.scale(from + 2.2 * s)), (0.9 - 0.18 * k) * s,
                        0.9 - 0.18 * k);
            }
            for (int k = 0; k < 8; k++) {
                double drift = Mth.frac(m.t() * 0.35 + Noise.of(k, 73, 0));
                Vec3 ember = at.add(back.scale((1.0 + 7.0 * drift) * s))
                        .add(Noise.direction(k, 73).scale(0.9 * drift * s));
                painter.flare(ember, 0.18 * s, 1.0 - drift);
            }
        }
        return at;
    }

    /** A crack of fire zigzagging over the rock between two of its rings, from corner {@code from} on. */
    private static void crack(LanternPainter painter, Frame frame, int ring, int from, double strength) {
        Vec3 last = null;
        for (int k = 0; k < 6; k++) {
            int row = ring + (k & 1);
            Vec3 corner = ROCK.points[1 + (row - 1) * ROCK_SIDES + (from + k) % ROCK_SIDES].scale(1.06);
            Vec3 next = frame.at(corner.x, corner.y, corner.z);
            if (last != null) {
                painter.edge(last, next, 0.05 * frame.scale(), strength);
            }
            last = next;
        }
    }

    // ---- The sword ----

    private static final double SWORD_SCALE = 1.4;
    // How far it leans back towards him as it stands in the ground.
    private static final double SWORD_LEAN = Math.toRadians(6.0);

    /**
     * A sword standing on its point: a flat blade with a ridge down it, a guard with round ends and a gem on either
     * side, a wrapped grip and a pommel.
     */
    private static final Shape SWORD = new Shape(new double[][] {
            // The guard, and the bright line of the ridge down the blade
            { -0.90, 3.20, -0.14, 0.90, 3.40, 0.14, 1.15 },
            { -0.03, 0.70, -0.075, 0.03, 3.10, 0.075, 1.3 } },
            Mesh.lathe(4, 1.0, 0.0, 0.0, 0.32, 0.50, 0.30, 3.20, 0.0, 3.22).scaled(1.0, 1.0, 0.22),
            Mesh.ball(8, 5, 0.14, 1.2).moved(0.95, 3.30, 0.0), Mesh.ball(8, 5, 0.14, 1.2).moved(-0.95, 3.30, 0.0),
            Mesh.ball(10, 6, 0.13, 1.45).scaled(1.0, 1.0, 0.6).moved(0.0, 3.30, -0.14),
            Mesh.ball(10, 6, 0.13, 1.45).scaled(1.0, 1.0, 0.6).moved(0.0, 3.30, 0.14),
            Mesh.cylinder(10, 0.10, 3.40, 4.38, 0.95),
            wrap(0.105, 3.50), wrap(0.105, 3.62), wrap(0.105, 3.74), wrap(0.105, 3.86), wrap(0.105, 3.98),
            wrap(0.105, 4.10), wrap(0.105, 4.22),
            Mesh.lathe(12, 1.2, 0.0, 4.36, 0.14, 4.38, 0.20, 4.50, 0.18, 4.64, 0.0, 4.74));

    /**
     * The sword: it drops point first out of the sky and plunges into the ground, where it stays standing, quivering
     * a moment.
     */
    static Vec3 sword(LanternPainter painter, Moment m) {
        double s = m.scale(SWORD_SCALE);
        double quiver = m.struck() ? Math.toRadians(4.0) * Math.sin(3.0 * m.since()) * Math.exp(-0.35 * m.since())
                : 0.0;
        double lean = SWORD_LEAN + quiver;
        Vec3 up = SlamPainter.UP.scale(Math.cos(lean)).subtract(m.forward().scale(Math.sin(lean)));
        double plunge = m.struck() ? 1.0 * s : 0.0;
        Vec3 tip = m.ground().add(0.0, 0.6 * SlamPainter.drop(m), 0.0).subtract(up.scale(plunge));
        Frame frame = new Frame(tip, m.right(), up, up.cross(m.right()), s);
        SlamPainter.marker(painter, m, 0.8);
        SlamPainter.piece(painter, SWORD, frame, m);
        return frame.at(0.0, 3.9, 0.0);
    }

    // ---- The rockets ----

    /**
     * A rocket flying along z: a round body with two bands round it, a pointed nose, four fins at its back and a
     * nozzle that widens behind it.
     */
    private static final Shape ROCKET = Shape.of(
            Mesh.lathe(14, 1.0, 0.0, -0.95, 0.20, -0.95, 0.22, -0.85, 0.22, 0.55, 0.18, 0.75, 0.10, 0.95, 0.03, 1.08,
                    0.0, 1.10).alongZ(),
            Mesh.torus(14, 4, 0.225, 0.025, 1.3).moved(0.0, 0.30, 0.0).alongZ(),
            Mesh.torus(14, 4, 0.225, 0.025, 1.3).moved(0.0, -0.60, 0.0).alongZ(),
            Mesh.cone(10, 0.17, 0.12, -1.20, -0.95, 1.2).alongZ(),
            fin(0.0), fin(90.0), fin(180.0), fin(270.0));

    /** Where a rocket is on its way from its place in the row, over the top, down to where it lands. */
    private static Vec3 rocketAt(Vec3 from, Vec3 over, Vec3 to, double p) {
        double u = 1.0 - p;
        return from.scale(u * u).add(over.scale(2.0 * u * p)).add(to.scale(p * p));
    }

    /** The way a rocket points on its way: along its path. */
    private static Vec3 rocketWay(Vec3 from, Vec3 over, Vec3 to, double p) {
        Vec3 way = over.subtract(from).scale(2.0 * (1.0 - p)).add(to.subtract(over).scale(2.0 * p));
        return way.lengthSqr() < 1.0E-8 ? SlamPainter.UP : way.normalize();
    }

    private static Mesh fin(double degrees) {
        return Mesh.prism(-0.03, 0.03, 1.05, 0.20, -0.95, 0.55, -1.05, 0.55, -0.75, 0.20, -0.40)
                .turned(0.0, 1.0, 0.0, degrees).alongZ();
    }

    /**
     * The rockets: five of them take shape in a row in the air before him, noses up, their ends glowing; one after the
     * other they blast off, climb, turn over and dive down onto and around where it strikes, each trailing fire and
     * landing with a blast and chunks flying.
     */
    static Vec3 rockets(LanternPainter painter, Moment m) {
        Vec3 anchor = m.ground().add(0.0, 1.0, 0.0);
        double size = 1.9 * m.size() * m.grow();
        for (int i = 0; i < 5; i++) {
            Vec3 target = LandingSlam.rocketTarget(m.ground(), m.forward(), i);
            double lands = LandingSlam.IMPACT_TICK - 2.0 + i;
            double launch = lands - 4.5;
            // Where it waits, in the row, bobbing a little on its flame.
            Vec3 rack = m.ground().subtract(m.forward().scale(1.4 * m.size()))
                    .add(m.right().scale((i - 2) * 1.15 * m.size()))
                    .add(0.0, (SlamPainter.HANG + 0.3 + 0.08 * Math.sin(m.t() * 1.3 + i)) * m.size(), 0.0);
            Vec3 to = target.add(0.0, 0.3, 0.0);
            Vec3 over = rack.add(0.0, 4.5 * m.size(), 0.0).add(to.subtract(rack).scale(0.25));
            double p = (m.t() - launch) / (lands - launch);
            if (p < 1.0) {
                Vec3 at = rocketAt(rack, over, to, Math.max(0.0, p));
                Vec3 way = p <= 0.0 ? SlamPainter.UP : rocketWay(rack, over, to, p);
                Vec3 top = m.forward().subtract(way.scale(way.dot(m.forward())));
                top = top.lengthSqr() < 1.0E-6 ? m.right() : top.normalize();
                Frame frame = new Frame(at, way.cross(top), top, way, size);
                painter.shape(ROCKET, frame, 1.0, 1.1);
                painter.flare(frame.at(0.0, 0.0, -1.35), (p > 0.0 ? 0.6 : 0.3) * size, 1.0);
                // The trail of fire it leaves on its way.
                Vec3 last = frame.at(0.0, 0.0, -1.2);
                for (int k = 1; k <= 6 && p > 0.0; k++) {
                    double q = p - 0.07 * k;
                    if (q <= 0.0) {
                        break;
                    }
                    Vec3 next = rocketAt(rack, over, to, q);
                    painter.edge(last, next, (0.34 - 0.04 * k) * size, 1.0 - 0.14 * k);
                    last = next;
                }
                anchor = at;
            }
            double since = m.t() - lands;
            if (since >= 0.0 && since < 5.0) {
                double burst = 1.0 - since / 5.0;
                painter.flare(target.add(0.0, 0.5, 0.0), 0.6 + 1.6 * burst, burst);
            }
            SlamPainter.rubble(painter, target, lands, m.t(), 5, 0.22, 0.3, 20 + i);
        }
        return anchor;
    }
}
