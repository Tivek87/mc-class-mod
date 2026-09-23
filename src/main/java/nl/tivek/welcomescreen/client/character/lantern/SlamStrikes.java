package nl.tivek.welcomescreen.client.character.lantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.client.character.lantern.ConstructPainter.Frame;
import nl.tivek.welcomescreen.client.character.lantern.ConstructPainter.Shape;
import nl.tivek.welcomescreen.client.character.lantern.SlamPainter.Moment;

/**
 * The landing-slam constructs that clap shut or are swung down (see {@link SlamPainter}): two cymbals, a bear trap, a
 * book, a fly swatter, a pickaxe, a judge's gavel on its block and drumsticks on a drum. Unless it says otherwise a
 * shape stands on y = 0 and faces -z, the way towards Green Lantern, with x to his right; in blocks at scale 1.
 */
final class SlamStrikes {
    private SlamStrikes() {
    }

    // ---- The cymbals ----

    private static final double CYMBAL_SCALE = 1.9;

    /**
     * A cymbal round its middle, its hollow side towards +y: a thin dish with a bell in its middle, two grooves turned
     * into it, and a knob and a strap on the back of the bell.
     */
    private static final Shape CYMBAL = Shape.of(
            Mesh.lathe(32, 1.0, 0.0, -0.26, 0.12, -0.26, 0.24, -0.22, 0.27, -0.13, 0.60, -0.07, 1.0, -0.01, 1.0, 0.01,
                    0.60, -0.05, 0.27, -0.11, 0.22, -0.18, 0.0, -0.20),
            Mesh.torus(32, 4, 0.52, 0.012, 1.35).moved(0.0, -0.058, 0.0),
            Mesh.torus(32, 4, 0.76, 0.012, 1.35).moved(0.0, -0.022, 0.0),
            Mesh.cylinder(8, 0.05, -0.34, -0.25, 1.1),
            Mesh.box(-0.25, -0.40, -0.04, 0.25, -0.34, 0.04, 1.05));

    /**
     * Two cymbals that swing in from either side and crash together in front of him: they bounce a little apart,
     * wobble as they ring, and rings of sound run out from between them.
     */
    static Vec3 cymbals(ConstructPainter painter, Moment m) {
        double s = m.scale(CYMBAL_SCALE);
        Vec3 meet = m.ground().add(0.0, 1.9 * m.size(), 0.0);
        double since = m.since();
        double apart = 0.1 * s + 3.2 * m.size() * (1.0 - m.fall())
                + (m.struck() ? 0.3 * s * ConstructPainter.smooth(since / 1.5) : 0.0);
        double wobble = m.struck() ? 0.3 * Math.sin(2.6 * since) * Math.exp(-0.3 * since) : 0.0;
        for (int k = -1; k <= 1; k += 2) {
            Vec3 toward = m.right().scale(-k);
            Frame frame = new Frame(meet.add(m.right().scale(k * apart)), m.forward(), toward, SlamPainter.UP, s)
                    .turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, k * wobble);
            SlamPainter.piece(painter, CYMBAL, frame, m);
        }
        if (m.struck()) {
            for (int k = 0; k < 3; k++) {
                double age = since - 1.4 * k;
                if (age > 0.0 && age < 8.0) {
                    double fade = 1.0 - age / 8.0;
                    painter.circle(meet, m.forward(), SlamPainter.UP, (0.9 + 0.45 * age) * s, 0.05, 0.25,
                            ConstructPainter.alpha(0.85 * fade), ConstructPainter.alpha(0.4 * fade));
                }
            }
        }
        return meet;
    }

    // ---- The bear trap ----

    private static final double TRAP_SCALE = 1.6;

    /**
     * The base of a bear trap along x: a plate, the round pan in its middle that sets it off, a coiled spring at
     * either end, and a chain to a stake in the ground.
     */
    private static final Shape TRAP_BASE = new Shape(new double[][] {
            { -1.45, 0.00, -0.40, 1.45, 0.10, 0.40, 1.0 },
            { 1.98, 0.00, -0.16, 2.12, 0.28, 0.16, 1.1 },
            { -2.12, 0.00, -0.16, -1.98, 0.28, 0.16, 1.1 } },
            Mesh.cylinder(16, 0.32, 0.10, 0.16, 1.2),
            Mesh.torus(16, 4, 0.32, 0.02, 1.3).moved(0.0, 0.16, 0.0),
            Mesh.tube(false, 5, 0.025, 1.15, coil(1.0)), Mesh.tube(false, 5, 0.025, 1.15, coil(-1.0)),
            chainLink(0, true), chainLink(1, false), chainLink(2, true), chainLink(3, false),
            Mesh.torus(12, 5, 0.12, 0.03, 1.2).alongZ().moved(-3.58, 0.17, 0.0),
            Mesh.cylinder(8, 0.06, 0.0, 0.06, 1.1).moved(-3.58, 0.0, 0.0));
    /** One jaw of the trap, shut: an arch up from its hinge along x, its teeth pointing -z. */
    private static final Shape JAW_A = jaw(new double[] { -1.05, -0.63, -0.21, 0.21, 0.63, 1.05 });
    /** The other jaw: its teeth in between those of the first, so they bite into each other. */
    private static final Shape JAW_B = jaw(new double[] { -0.84, -0.42, 0.0, 0.42, 0.84 });

    /** A coiled spring from the end of the plate outwards, along {@code side} x. */
    private static Vec3[] coil(double side) {
        int pieces = 32;
        Vec3[] points = new Vec3[pieces + 1];
        for (int i = 0; i <= pieces; i++) {
            double t = (double) i / pieces;
            double turn = Math.PI * 8.0 * t;
            points[i] = new Vec3(side * (1.45 + 0.53 * t), 0.13 + 0.12 * Math.sin(turn), 0.12 * Math.cos(turn));
        }
        return points;
    }

    /** A link of the chain that runs out along -x from the plate, lying flat or standing on its edge. */
    private static Mesh chainLink(int k, boolean flat) {
        Mesh link = SlamDrops.link().turned(0.0, 0.0, 1.0, 90.0);
        return (flat ? link.turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 0.04, 0.0) : link.moved(0.0, 0.15, 0.0))
                .moved(-2.35 - 0.36 * k, 0.0, 0.0);
    }

    private static Shape jaw(double[] teeth) {
        double radius = 1.35;
        int pieces = 16;
        Vec3[] arch = new Vec3[pieces + 1];
        for (int i = 0; i <= pieces; i++) {
            double angle = Math.PI * i / pieces;
            arch[i] = new Vec3(radius * Math.cos(angle), radius * Math.sin(angle), 0.0);
        }
        Mesh[] meshes = new Mesh[2 + teeth.length];
        meshes[0] = Mesh.tube(false, 6, 0.07, 1.05, arch);
        meshes[1] = Mesh.cylinder(8, 0.06, -1.40, 1.40, 1.0).alongX();
        for (int i = 0; i < teeth.length; i++) {
            double angle = Math.acos(teeth[i] / radius);
            meshes[2 + i] = Mesh.cone(6, 0.09, 0.0, 0.0, 0.30, 1.25).pointing(0.0, 0.0, -1.0)
                    .moved(teeth[i], radius * Math.sin(angle), 0.0);
        }
        return Shape.of(meshes);
    }

    /**
     * A bear trap: its jaws lie open flat on the ground, then snap up and shut, their teeth biting into each other,
     * and the whole trap and its chain rattle a moment.
     */
    static Vec3 trap(ConstructPainter painter, Moment m) {
        double s = m.scale(TRAP_SCALE);
        Vec3 ground = m.ground();
        Vec3 f = m.forward();
        Vec3 r = m.right();
        double rattle = SlamPainter.vibrate(m, 0.03);
        SlamPainter.piece(painter, TRAP_BASE, new Frame(ground.add(0.0, rattle * s, 0.0), r, SlamPainter.UP, f, s),
                m);
        double angle = Math.toRadians(Mth.lerp(m.fall(), 88.0, -10.0) + 4.0 * SlamPainter.vibrate(m, 1.0));
        double c = Math.cos(angle);
        double sin = Math.sin(angle);
        Vec3 up = SlamPainter.UP;
        Vec3 hingeA = ground.add(f.scale(0.40 * s)).add(0.0, 0.12 * s, 0.0);
        SlamPainter.piece(painter, JAW_A, new Frame(hingeA, r, up.scale(c).add(f.scale(sin)),
                f.scale(c).subtract(up.scale(sin)), s), m);
        Vec3 hingeB = ground.subtract(f.scale(0.40 * s)).add(0.0, 0.12 * s, 0.0);
        SlamPainter.piece(painter, JAW_B, new Frame(hingeB, r.scale(-1.0), up.scale(c).subtract(f.scale(sin)),
                f.scale(-c).subtract(up.scale(sin)), s), m);
        if (m.struck()) {
            SlamPainter.sparks(painter, ground.add(0.0, 1.4 * s, 0.0), m.since(), 10, 0.25, 47);
        }
        return ground.add(0.0, 0.3 * s, 0.0);
    }

    // ---- The book ----

    private static final double BOOK_SCALE = 1.3;
    // How far either cover sits from the middle of the spine, at scale 1.
    private static final double COVER_OUT = 0.34;

    /** A cover of the book, open from its spine along x: the board, corners, the emblem outside, the pages on +z. */
    private static final Shape COVER_POS = cover(1.0);
    /** The other cover: the pages on its -z side. */
    private static final Shape COVER_NEG = cover(-1.0);
    /** The spine of the book, standing along y: round at the back, with raised bands across it. */
    private static final Shape SPINE = Shape.of(
            Mesh.cylinder(16, 0.42, -1.45, 1.45, 1.05).scaled(1.0, 1.0, 0.35),
            spineBand(-1.05), spineBand(-0.35), spineBand(0.35), spineBand(1.05));
    /** A loose page, flipping over from one cover to the other while the book is open. */
    private static final Shape PAGE = new Shape(new double[][] { { 0.06, -1.30, -0.012, 1.90, 1.30, 0.012, 1.3 } });

    /** A cover with its pages on its {@code side} z. */
    private static Shape cover(double side) {
        double board0 = side > 0.0 ? 0.0 : -0.08;
        double out = side > 0.0 ? -0.012 : 0.012;
        double[][] boxes = {
                { 0.00, -1.40, board0, 2.00, 1.40, board0 + 0.08, 1.0 },
                pages(side, 0.08, 0.34, 1.2),
                // Metal corners on its outer edge
                { 1.80, 1.20, board0 - 0.01, 2.02, 1.42, board0 + 0.09, 1.25 },
                { 1.80, -1.42, board0 - 0.01, 2.02, -1.20, board0 + 0.09, 1.25 },
                // The bars of the emblem on the outside
                { 0.62, 0.50, Math.min(out, out * 3.0), 1.38, 0.60, Math.max(out, out * 3.0), 1.35 },
                { 0.62, -0.60, Math.min(out, out * 3.0), 1.38, -0.50, Math.max(out, out * 3.0), 1.35 } };
        return new Shape(boxes, Mesh.torus(18, 6, 0.30, 0.045, 1.35).alongZ().moved(1.0, 0.0, out * 2.0));
    }

    /** The block of pages on a cover, from {@code near} to {@code far} out from it on its {@code side}. */
    private static double[] pages(double side, double near, double far, double bright) {
        return side > 0.0 ? new double[] { 0.06, -1.32, near, 1.92, 1.32, far, bright }
                : new double[] { 0.06, -1.32, -far, 1.92, 1.32, -near, bright };
    }

    private static Mesh spineBand(double y) {
        return Mesh.torus(16, 4, 0.42, 0.03, 1.3).scaled(1.0, 1.0, 0.35).moved(0.0, y, 0.0);
    }

    /**
     * A giant book standing in the air ahead of him, spread open towards him. Its pages flip over as it opens wide,
     * then it slams shut: the covers swing in and clap together, and it shivers a moment.
     */
    static Vec3 book(ConstructPainter painter, Moment m) {
        double s = m.scale(BOOK_SCALE);
        Vec3 r = m.right();
        Vec3 f = m.forward();
        Vec3 up = SlamPainter.UP;
        Vec3 spine = m.ground().add(f.scale(1.0 * m.size())).add(0.0, 1.45 * s + 0.1, 0.0);
        double open = Math.toRadians(Mth.lerp(m.fall(), 70.0, 0.0) + 5.0 * SlamPainter.vibrate(m, 1.0));
        Vec3 coverA = f.scale(-Math.cos(open)).add(r.scale(Math.sin(open)));
        Vec3 coverB = f.scale(-Math.cos(open)).subtract(r.scale(Math.sin(open)));
        SlamPainter.piece(painter, COVER_NEG, new Frame(spine.add(r.scale(COVER_OUT * s)), coverA, up,
                up.cross(coverA), s), m);
        SlamPainter.piece(painter, COVER_POS, new Frame(spine.subtract(r.scale(COVER_OUT * s)), coverB, up,
                up.cross(coverB), s), m);
        SlamPainter.piece(painter, SPINE, new Frame(spine, r, up, f, s), m);
        if (!m.struck()) {
            // Three pages flip over from one cover to the other, one after the other.
            for (int i = 0; i < 3; i++) {
                double p = ConstructPainter.smooth((m.t() - 4.5 - 1.5 * i) / 1.6);
                if (p <= 0.0 || p >= 1.0) {
                    continue;
                }
                double angle = open * (1.0 - 2.0 * p);
                Vec3 way = f.scale(-Math.cos(angle)).subtract(r.scale(Math.sin(angle)));
                painter.shape(PAGE, new Frame(spine.subtract(r.scale(COVER_OUT * s * (1.0 - 2.0 * p))), way, up,
                        up.cross(way), s), 1.0, m.bright());
            }
        }
        return spine;
    }

    // ---- The fly swatter ----

    // Half the width of the swatter's head, and how far its corners are cut off, at scale 1.
    private static final double SWATTER_HALF = 1.6;
    private static final double SWATTER_CUT = 0.45;

    /**
     * The head of a fly swatter round its middle, flat across x and y: an eight-sided plate, a grid of bars over both
     * faces, a round rim, and a neck at its near edge (-y) where the handle goes in.
     */
    private static final Shape SWATTER = swatter();
    /** The grip at the end of the handle: wraps round it and a loop to hang it by, down along -y from its end. */
    private static final Shape GRIP = Shape.of(
            Mesh.torus(10, 4, 0.11, 0.025, 1.2).moved(0.0, 0.10, 0.0),
            Mesh.torus(10, 4, 0.11, 0.025, 1.2).moved(0.0, 0.24, 0.0),
            Mesh.torus(10, 4, 0.11, 0.025, 1.2).moved(0.0, 0.38, 0.0),
            Mesh.torus(10, 4, 0.11, 0.025, 1.2).moved(0.0, 0.52, 0.0),
            Mesh.torus(12, 5, 0.12, 0.03, 1.15).alongZ().moved(0.0, -0.16, 0.0));
    /** The handle: a round rod from y = 0 to y = 1, stretched to its length. */
    private static final Shape HANDLE = Shape.of(Mesh.cylinder(10, 0.1, 0.0, 1.0, 0.95));

    private static Shape swatter() {
        double h = SWATTER_HALF;
        double c = h - SWATTER_CUT;
        double[] outline = { c, -h, h, -c, h, c, c, h, -c, h, -h, c, -h, -c, -c, -h };
        Vec3[] rim = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            rim[i] = new Vec3(outline[2 * i], outline[2 * i + 1], 0.0);
        }
        double[] across = { -1.07, -0.53, 0.0, 0.53, 1.07 };
        double[][] boxes = new double[4 * across.length + 1][];
        for (int i = 0; i < across.length; i++) {
            double at = across[i];
            double reach = Math.min(h - 0.1, h + c - Math.abs(at) - 0.1);
            for (int face = 0; face < 2; face++) {
                double z0 = face == 0 ? 0.05 : -0.075;
                boxes[4 * i + 2 * face] = new double[] { -reach, at - 0.03, z0, reach, at + 0.03, z0 + 0.025, 1.2 };
                boxes[4 * i + 2 * face + 1] = new double[] { at - 0.03, -reach, z0, at + 0.03, reach, z0 + 0.025,
                        1.2 };
            }
        }
        boxes[4 * across.length] = new double[] { -0.14, -h - 0.45, -0.07, 0.14, -h + 0.05, 0.07, 1.1 };
        return new Shape(boxes, Mesh.prism(-0.05, 0.05, 0.95, outline), Mesh.tube(true, 6, 0.08, 1.15, rim));
    }

    /**
     * The fly swatter: its handle in his hand, it comes up from behind him over his shoulder and swings down flat onto
     * the ground ahead of him, and bounces a little on it.
     */
    static Vec3 swatter(ConstructPainter painter, Moment m) {
        Vec3 pivot = m.him().add(0.0, 1.0, 0.0).add(m.right().scale(0.35));
        double g = m.scale(1.0);
        Vec3 plate = m.ground().add(0.0, 0.12 * g, 0.0);
        Vec3 near = plate.subtract(m.forward().scale((SWATTER_HALF + 0.45) * g));
        // The whole swatter turns about his hand, from behind him to flat on the ground.
        double swing = Math.toRadians(160.0 * (1.0 - m.fall()) + 8.0 * SlamPainter.vibrate(m, 1.0));
        Vec3 axis = m.right();
        Vec3 nearNow = pivot.add(ConstructPainter.spin(near.subtract(pivot), axis, swing));
        Vec3 plateNow = pivot.add(ConstructPainter.spin(plate.subtract(pivot), axis, swing));
        Vec3 along = ConstructPainter.spin(m.forward(), axis, swing);
        Vec3 face = ConstructPainter.spin(SlamPainter.UP, axis, swing);
        Vec3 stick = nearNow.subtract(pivot);
        double length = Math.max(0.1, stick.length());
        Vec3 dir = stick.scale(1.0 / length);
        Frame hand = new Frame(pivot, axis, dir, dir.cross(axis), Math.max(0.3, g));
        SlamPainter.piece(painter, HANDLE, hand.stretched(1.0, length / hand.scale(), 1.0), m);
        SlamPainter.piece(painter, GRIP, hand, m);
        SlamPainter.piece(painter, SWATTER, new Frame(plateNow, face.cross(along), along, face, g), m);
        return pivot.add(dir.scale(0.5));
    }

    // ---- The pickaxe ----

    // The arms of the head: from the socket out and down, through these points (x out, y up the handle).
    private static final double[][] ARM = { { 0.15, 3.00 }, { 0.95, 3.10 }, { 1.35, 2.45 } };
    // How far the point sticks out past the end of the arm.
    private static final double POINT = 0.32;
    /** Where the point of the pickaxe's front arm is: x out along the arm, y up the handle. */
    private static final double[] PICK_TIP = tip();

    /**
     * A pickaxe: the handle up along y from its end, with a wrapped grip and a knob, and the head across its top: a
     * socket, and an arm curving down either way to a point.
     */
    private static final Shape PICKAXE = new Shape(new double[][] {
            { -0.19, 2.76, -0.19, 0.19, 3.22, 0.19, 1.05 },
            { -0.14, 3.22, -0.14, 0.14, 3.28, 0.14, 1.15 } },
            Mesh.lathe(10, 0.95, 0.0, 0.0, 0.12, 0.0, 0.13, 0.07, 0.10, 0.22, 0.085, 0.50, 0.085, 2.78, 0.0, 2.78),
            wrapAt(0.35), wrapAt(0.47), wrapAt(0.59), wrapAt(0.71), wrapAt(0.83), wrapAt(0.95),
            Mesh.tube(false, 8, 0.10, 1.0, arm(1.0)), Mesh.tube(false, 8, 0.10, 1.0, arm(-1.0)),
            point(1.0), point(-1.0));

    private static Mesh wrapAt(double y) {
        return Mesh.torus(10, 4, 0.09, 0.02, 1.15).moved(0.0, y, 0.0);
    }

    /** The arm curving out along {@code side} x, through {@link #ARM}. */
    private static Vec3[] arm(double side) {
        int pieces = 10;
        Vec3[] points = new Vec3[pieces + 1];
        for (int i = 0; i <= pieces; i++) {
            double t = (double) i / pieces;
            double u = 1.0 - t;
            double x = u * u * ARM[0][0] + 2.0 * u * t * ARM[1][0] + t * t * ARM[2][0];
            double y = u * u * ARM[0][1] + 2.0 * u * t * ARM[1][1] + t * t * ARM[2][1];
            points[i] = new Vec3(side * x, y, 0.0);
        }
        return points;
    }

    /** The way the arm runs out at its end, one long, for {@code side} x. */
    private static Vec3 outward(double side) {
        return new Vec3(side * (ARM[2][0] - ARM[1][0]), ARM[2][1] - ARM[1][1], 0.0).normalize();
    }

    private static Mesh point(double side) {
        Vec3 way = outward(side);
        return Mesh.cone(8, 0.10, 0.0, -0.02, POINT, 1.25).pointing(way.x, way.y, way.z).moved(side * ARM[2][0],
                ARM[2][1], 0.0);
    }

    private static double[] tip() {
        Vec3 way = outward(1.0);
        return new double[] { ARM[2][0] + way.x * POINT, ARM[2][1] + way.y * POINT };
    }

    /**
     * A pickaxe: its handle in his hand, it swings from behind him up over his head and down, its point biting into
     * the ground ahead of him with sparks; it stays stuck there, quivering.
     */
    static Vec3 pickaxe(ConstructPainter painter, Moment m) {
        Vec3 pivot = m.him().add(0.0, 1.05, 0.0).add(m.right().scale(0.35));
        Vec3 to = m.ground().subtract(0.0, 0.25, 0.0).subtract(pivot);
        double tip = Math.sqrt(PICK_TIP[0] * PICK_TIP[0] + PICK_TIP[1] * PICK_TIP[1]);
        double s = Math.max(0.5, to.length() / tip);
        // Angles in the upright plane through the way he faces, from straight up (0) towards ahead.
        double strike = Math.atan2(to.dot(m.forward()), to.y) - Math.atan2(PICK_TIP[0], PICK_TIP[1]);
        double quiver = m.struck() ? Math.toRadians(3.0) * Math.sin(3.0 * m.since()) * Math.exp(-0.3 * m.since())
                : 0.0;
        double angle = Mth.lerp(m.fall(), Math.toRadians(-120.0), strike) + quiver;
        Vec3 up = SlamPainter.UP.scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
        Vec3 lead = SlamPainter.UP.scale(-Math.sin(angle)).add(m.forward().scale(Math.cos(angle)));
        Frame frame = new Frame(pivot, lead, up, up.cross(lead), s * m.grow());
        SlamPainter.piece(painter, PICKAXE, frame, m);
        SlamPainter.sparks(painter, m.ground().add(0.0, 0.05, 0.0), m.since(), 10, 0.25, 48);
        return frame.at(0.0, 0.3, 0.0);
    }

    // ---- The gavel ----

    private static final double GAVEL_SCALE = 1.3;
    // How far along the gavel's handle its end lies from the middle of its head.
    private static final double GAVEL_REACH = 2.58;

    /** The round block a gavel strikes, with a bright ring set into its top. */
    private static final Shape SOUND_BLOCK = Shape.of(
            Mesh.lathe(24, 1.0, 0.0, 0.0, 1.20, 0.0, 1.26, 0.06, 1.26, 0.28, 1.18, 0.34, 1.05, 0.36, 1.05, 0.42, 0.0,
                    0.42),
            Mesh.torus(24, 4, 0.75, 0.02, 1.35).moved(0.0, 0.425, 0.0));
    /**
     * A judge's gavel: a turned head along x round its middle, with rings round both faces and bands round it, and a
     * turned handle up along y.
     */
    private static final Shape GAVEL = Shape.of(
            Mesh.lathe(20, 1.0, 0.0, -0.90, 0.30, -0.90, 0.36, -0.85, 0.36, -0.76, 0.31, -0.72, 0.29, -0.30, 0.33,
                    -0.24, 0.33, 0.24, 0.29, 0.30, 0.31, 0.72, 0.36, 0.76, 0.36, 0.85, 0.30, 0.90, 0.0, 0.90).alongX(),
            Mesh.torus(20, 4, 0.30, 0.02, 1.3).alongX().moved(0.52, 0.0, 0.0),
            Mesh.torus(20, 4, 0.30, 0.02, 1.3).alongX().moved(-0.52, 0.0, 0.0),
            Mesh.lathe(10, 0.95, 0.0, 0.26, 0.10, 0.26, 0.10, 0.45, 0.085, 0.90, 0.11, 2.20, 0.14, 2.34, 0.15, 2.50,
                    0.11, 2.62, 0.0, 2.66),
            Mesh.torus(10, 4, 0.105, 0.02, 1.25).moved(0.0, 0.34, 0.0));

    /**
     * A judge's gavel: its block stands on the ground, and the gavel swings down from high up and strikes it; it
     * lifts once more and knocks again, like a judge calling for order.
     */
    static Vec3 gavel(ConstructPainter painter, Moment m) {
        double s = m.scale(GAVEL_SCALE);
        SlamPainter.piece(painter, SOUND_BLOCK, new Frame(m.ground(), m.right(), SlamPainter.UP, m.forward(), s), m);
        double reach = GAVEL_REACH * s;
        // It turns about the end of its handle, beyond the block.
        Vec3 pivot = m.ground().add(m.forward().scale(reach)).add(0.0, 0.72 * s, 0.0);
        double since = m.since();
        double again = since > 1.4 && since < 4.0 ? Math.sin(Math.PI * (since - 1.4) / 2.6) : 0.0;
        double angle = Math.toRadians(80.0 * (1.0 - m.fall()) + 22.0 * again + 12.0 * SlamPainter.vibrate(m, 1.0));
        Vec3 out = m.forward().scale(-Math.cos(angle)).add(SlamPainter.UP.scale(Math.sin(angle)));
        Vec3 up = out.scale(-1.0);
        SlamPainter.piece(painter, GAVEL, new Frame(pivot.add(out.scale(reach)), m.right(), up, up.cross(m.right()),
                s), m);
        // The second knock rings out over the block.
        double ring = since - 4.0;
        if (ring > 0.0 && ring < 6.0) {
            double fade = 1.0 - ring / 6.0;
            painter.circle(m.ground().add(0.0, 0.44 * s, 0.0), m.right(), m.forward(), (0.6 + 0.35 * ring) * s, 0.05,
                    0.25, ConstructPainter.alpha(0.85 * fade), ConstructPainter.alpha(0.4 * fade));
        }
        SlamPainter.sparks(painter, m.ground().add(0.0, 0.42 * s, 0.0), since - 4.0, 6, 0.18, 49);
        return pivot;
    }

    // ---- The drum ----

    private static final double DRUM_SCALE = 1.4;
    // How high the drum's head is, and how far along a drumstick the middle of its ball lies.
    private static final double DRUM_TOP = 1.46;
    private static final double STICK_REACH = 2.53;

    /** A drum: a round shell, a hoop round its top and its foot, tension rods and lugs round its side. */
    private static final Shape DRUM = Shape.of(drum());
    /** The drumhead, on its own so it can give under the sticks, with a bright ring on it. */
    private static final Shape HEAD = Shape.of(Mesh.cylinder(24, 0.97, 1.38, 1.44, 1.3),
            Mesh.torus(24, 4, 0.40, 0.015, 1.45).moved(0.0, 1.445, 0.0));
    /** A drumstick up along y from its end, thinning towards a ball at its tip, with a wrapped grip. */
    private static final Shape STICK = Shape.of(
            Mesh.lathe(10, 1.0, 0.0, 0.0, 0.075, 0.0, 0.075, 0.30, 0.065, 1.40, 0.045, 2.30, 0.03, 2.43, 0.0, 2.44),
            Mesh.ball(8, 5, 0.085, 1.2).moved(0.0, STICK_REACH, 0.0),
            Mesh.torus(10, 4, 0.078, 0.015, 1.2).moved(0.0, 0.12, 0.0),
            Mesh.torus(10, 4, 0.078, 0.015, 1.2).moved(0.0, 0.24, 0.0));

    private static Mesh[] drum() {
        Mesh[] meshes = new Mesh[3 + 16];
        meshes[0] = Mesh.cylinder(24, 0.98, 0.0, 1.38, 0.95);
        meshes[1] = Mesh.torus(24, 6, 1.0, 0.06, 1.2).moved(0.0, 0.08, 0.0);
        meshes[2] = Mesh.torus(24, 6, 1.0, 0.07, 1.2).moved(0.0, 1.40, 0.0);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            meshes[3 + i] = Mesh.cylinder(6, 0.022, 0.12, 1.34, 1.25).moved(1.04 * Math.cos(angle), 0.0,
                    1.04 * Math.sin(angle));
            meshes[11 + i] = Mesh.box(-0.05, 0.60, -0.07, 0.05, 0.84, 0.07, 1.15).moved(1.0, 0.0, 0.0)
                    .turned(0.0, 1.0, 0.0, 45.0 * i + 22.5);
        }
        return meshes;
    }

    /**
     * A drum standing on the ground, and two drumsticks that come down on its head together; then they bounce on it in
     * a drum roll, the head giving under every blow and rings running out over it.
     */
    static Vec3 drum(ConstructPainter painter, Moment m) {
        double s = m.scale(DRUM_SCALE);
        Vec3 ground = m.ground();
        Vec3 f = m.forward();
        Vec3 r = m.right();
        double since = m.since();
        Frame body = new Frame(ground, r, SlamPainter.UP, f, s);
        SlamPainter.piece(painter, DRUM, body, m);
        double roll = m.struck() ? Math.exp(-0.18 * since) : 0.0;
        SlamPainter.piece(painter, HEAD, body.moved(0.0, -0.02 * Math.abs(Math.sin(2.4 * since)) * roll, 0.0), m);
        double head = DRUM_TOP * s;
        Vec3 raised = SlamPainter.UP.scale(0.85).subtract(f.scale(0.5)).normalize();
        for (int k = -1; k <= 1; k += 2) {
            Vec3 pivot = ground.subtract(f.scale(1.7 * s)).add(0.0, 1.9 * s, 0.0).add(r.scale(k * 0.55 * s));
            Vec3 tip = ground.add(0.0, head + 0.12 * s, 0.0).add(r.scale(k * 0.32 * s));
            Vec3 strike = tip.subtract(pivot);
            double length = strike.length();
            strike = strike.scale(1.0 / length);
            // After the blow the sticks bounce on the head one after the other: a drum roll.
            double bounce = m.struck() ? 0.35 * Math.abs(Math.sin(2.4 * since + (k + 1) * 0.8)) * roll : 0.0;
            Vec3 dir = raised.lerp(strike, m.fall()).lerp(raised, bounce).normalize();
            Vec3 side = r.subtract(dir.scale(r.dot(dir))).normalize();
            SlamPainter.piece(painter, STICK, new Frame(pivot, side, dir, dir.cross(side), length / STICK_REACH), m);
        }
        if (m.struck()) {
            for (int j = 0; j < 6; j++) {
                double age = since - 1.3 * j;
                if (age > 0.0 && age < 5.0) {
                    double fade = 1.0 - age / 5.0;
                    Vec3 at = ground.add(0.0, head + 0.01, 0.0).add(r.scale(((j & 1) == 0 ? 0.32 : -0.32) * s));
                    painter.circle(at, r, f, (0.1 + 0.3 * age) * s, 0.04, 0.2, ConstructPainter.alpha(0.9 * fade),
                            ConstructPainter.alpha(0.4 * fade));
                }
            }
        }
        return ground.add(0.0, head, 0.0);
    }
}
