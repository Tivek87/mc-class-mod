package nl.tivek.welcomescreen.client.character.lantern;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.character.lantern.LandingSlam;
import nl.tivek.welcomescreen.network.ConstructPayload;

/**
 * The constructs of a landing slam (see {@link LandingSlam}), solid like every construct (see
 * {@link ConstructPainter}): each one grows out of the ring's light, fed by a beam from his ring, strikes at
 * {@link LandingSlam#IMPACT_TICK} and in the end breaks into solid pieces that fly apart (or sinks back into the
 * ground); it never fades out. Meanwhile his own fist has cracked the ground where it smashed into it, and the
 * shockwave runs out over the ground as a low ring of hard light. The constructs and what they do:
 * <ul>
 * <li>dropped out of the sky: a fist, a war hammer, an anvil, a boot, a ton weight, his lantern, a safe, an anchor on
 * its chain, a spiked ball, a barbell, a bell, a slapping hand, a sword, a piano, a toy brick, a stamp that stamps the
 * emblem into the ground, a block of TNT that blows up, and a meteor streaking in;</li>
 * <li>clapped shut: two hands, two fists, two cymbals, a bear trap and a book;</li>
 * <li>burst out of the ground: an uppercut, two rings of spikes, and a pillar that topples over;</li>
 * <li>swung down: a fly swatter and a pickaxe over his shoulder, a gavel on its block, drumsticks on a drum;</li>
 * <li>the lantern emblem, standing up and falling flat on its face, and a volley of rockets.</li>
 * </ul>
 */
final class SlamPainter {
    private static final Vec3 UP = ConstructPainter.UP;
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);
    // How far the falling ones drop from, in blocks.
    private static final double DROP = 9.0;
    // The tick it has broken up completely.
    private static final double GONE_TICK = 26.0;
    // How long the shockwave takes to run out, and to sink away, in ticks.
    private static final double WAVE_TICKS = 12.0;
    private static final double WAVE_FADE = 16.0;
    // The width of the fist model at scale 1, in blocks, and how far its knuckles reach in front of its middle.
    private static final double FIST_WIDTH = 1.23;
    private static final double FIST_REACH = 0.57;
    // The emblem's ring: how many pieces, and where it runs between, at scale 1; and how tall it all is.
    private static final int EMBLEM_PIECES = 16;
    private static final double EMBLEM_INNER = 0.66;
    private static final double EMBLEM_OUTER = 1.0;
    private static final double EMBLEM_HALF = 1.4;
    // The ways the studs of the spiked ball stick out: along its axes and to its corners.
    private static final Vec3[] STUDS = studs();

    private SlamPainter() {
    }

    /**
     * Where one slam is at one moment, the same for every construct.
     *
     * @param ground  where it strikes
     * @param forward the way he faced, flat
     * @param right   his right
     * @param him     where he is (his feet)
     * @param t       ticks since he landed
     * @param grow    0 to 1 (a hair over on the way): how far it has taken shape
     * @param go      0 to 1: how far it is on its way from taking shape to striking
     * @param fall    the same, but slowly at first and faster and faster, like a real fall
     * @param since   ticks since it struck (below 0 before)
     * @param flash   1 the moment it strikes, dying down over a few ticks
     * @param apart   0 to 1: how far it has broken up at the end
     */
    private record Moment(Vec3 ground, Vec3 forward, Vec3 right, Vec3 him, double t, double grow, double go,
            double fall, double since, double flash, double apart) {
        boolean struck() {
            return this.since >= 0.0;
        }

        double bright() {
            return 1.0 + 0.5 * this.flash;
        }
    }

    /**
     * One landing slam.
     *
     * @param slam what the server said about it: where it strikes, the way its maker faced, how far the wave goes
     *             and which construct it is
     * @param t    ticks since he landed, by the client's own clock
     * @param ring where its maker's ring is, or null when he is out of sight
     * @param him  where its maker is, or null when he is out of sight
     */
    static void draw(ConstructPainter painter, ConstructPayload slam, double t, @Nullable Vec3 ring,
            @Nullable Vec3 him) {
        Vec3 ground = slam.center();
        Vec3 forward = new Vec3(slam.facing().x, 0.0, slam.facing().z);
        forward = forward.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
        Vec3 right = forward.cross(UP).normalize();
        Vec3 feet = him != null ? him : ground.subtract(forward.scale(LandingSlam.ahead(slam.variant())));
        double go = Mth.clamp((t - LandingSlam.FORM_TICKS) / (LandingSlam.IMPACT_TICK - LandingSlam.FORM_TICKS),
                0.0, 1.0);
        double since = t - LandingSlam.IMPACT_TICK;
        double flash = since >= 0.0 ? Mth.clamp(1.0 - since / 4.0, 0.0, 1.0) : 0.0;
        double apart = Mth.clamp((t - LandingSlam.BURST_TICK) / (GONE_TICK - LandingSlam.BURST_TICK), 0.0, 1.0);
        Moment m = new Moment(ground, forward, right, feet, t, backOut(t / LandingSlam.FORM_TICKS), go, go * go,
                since, flash, apart);
        if (apart < 1.0) {
            Vec3 anchor = construct(painter, slam.variant(), m);
            // The ring feeds it while it takes shape and comes down, and lets go once it has struck.
            if (ring != null && t < LandingSlam.IMPACT_TICK + 2.0) {
                painter.beam(ring, anchor, m.struck() ? 0.5 : 1.0, 2.0);
            }
        }
        fistImpact(painter, m);
        if (m.struck()) {
            wave(painter, m, slam.size());
        }
    }

    /** Draws the construct itself, and gives where the ring's beam feeds it. */
    private static Vec3 construct(ConstructPainter painter, int variant, Moment m) {
        Vec3 r = m.right();
        Vec3 f = m.forward();
        return switch (variant) {
            case ConstructPayload.SLAM_HANDS -> hands(painter, m);
            case ConstructPayload.SLAM_FISTS -> fists(painter, m);
            case ConstructPayload.SLAM_HAMMER -> drop(painter, m, SlamModels.HAMMER, 1.5, SlamModels.HAMMER_FOOT, r,
                    0.0, 2.6);
            case ConstructPayload.SLAM_EMBLEM -> emblem(painter, m);
            case ConstructPayload.SLAM_ANVIL -> drop(painter, m, SlamModels.ANVIL, 1.9, 0.0, r, 0.0, 1.22);
            case ConstructPayload.SLAM_CYMBALS -> cymbals(painter, m);
            case ConstructPayload.SLAM_UPPERCUT -> uppercut(painter, m);
            case ConstructPayload.SLAM_SPIKES -> spikes(painter, m);
            case ConstructPayload.SLAM_BOOT -> drop(painter, m, SlamModels.BOOT, 2.2, 0.0, r, 0.35, 2.1);
            case ConstructPayload.SLAM_WEIGHT -> drop(painter, m, SlamModels.WEIGHT, 1.6, 0.0, r, 0.0, 1.94);
            case ConstructPayload.SLAM_SWORD -> sword(painter, m);
            case ConstructPayload.SLAM_ROCKETS -> rockets(painter, m);
            case ConstructPayload.SLAM_SWATTER -> swatter(painter, m);
            case ConstructPayload.SLAM_LANTERN -> lantern(painter, m);
            case ConstructPayload.SLAM_SAFE -> drop(painter, m, SlamModels.SAFE, 1.8, 0.0, f, 0.5, 1.72);
            case ConstructPayload.SLAM_ANCHOR -> anchor(painter, m);
            case ConstructPayload.SLAM_MACE -> mace(painter, m);
            case ConstructPayload.SLAM_BARBELL -> drop(painter, m, SlamModels.BARBELL, 1.4, SlamModels.BARBELL_FOOT, f,
                    0.9, 0.0);
            case ConstructPayload.SLAM_BELL -> bell(painter, m);
            case ConstructPayload.SLAM_METEOR -> meteor(painter, m);
            case ConstructPayload.SLAM_PALM -> palm(painter, m);
            case ConstructPayload.SLAM_GAVEL -> gavel(painter, m);
            case ConstructPayload.SLAM_PICKAXE -> pickaxe(painter, m);
            case ConstructPayload.SLAM_TRAP -> trap(painter, m);
            case ConstructPayload.SLAM_BOOK -> book(painter, m);
            case ConstructPayload.SLAM_DRUM -> drum(painter, m);
            case ConstructPayload.SLAM_PILLAR -> pillar(painter, m);
            case ConstructPayload.SLAM_TNT -> tnt(painter, m);
            case ConstructPayload.SLAM_PIANO -> drop(painter, m, SlamModels.PIANO, 1.4, 0.0, r, 0.3, 2.1);
            case ConstructPayload.SLAM_BRICK -> drop(painter, m, SlamModels.BRICK, 1.5, 0.0, f, 0.7, 1.16);
            case ConstructPayload.SLAM_STAMP -> stamp(painter, m);
            default -> skyFist(painter, m);
        };
    }

    // ---- Shared ----

    /** A shape, whole while it lasts and breaking into solid pieces at the end. */
    private static void piece(ConstructPainter painter, double[][] model, ConstructPainter.Frame frame, Moment m) {
        if (m.apart() > 0.0) {
            painter.shattered(model, frame, m.apart(), m.bright());
        } else {
            painter.model(model, frame, 1.0, m.bright());
        }
    }

    /**
     * Something heavy that drops out of the sky: it takes shape high above where it strikes, falls faster and faster,
     * tumbling a little about {@code axis} on the way until it lands square, lands a hair into the ground, and breaks
     * up at the end.
     *
     * @param foot how far the shape reaches below its own middle (0 for shapes that stand on y = 0)
     * @param top  how high up the shape the ring's beam holds it
     */
    private static Vec3 drop(ConstructPainter painter, Moment m, double[][] model, double scale, double foot,
            Vec3 axis, double tumble, double top) {
        ConstructPainter.Frame frame = dropped(m, scale, foot, axis, tumble);
        marker(painter, m, scale);
        piece(painter, model, frame, m);
        return frame.at(0.0, top, 0.0);
    }

    /** Where a dropped shape is: upright over where it strikes, as high as its fall has got. */
    private static ConstructPainter.Frame dropped(Moment m, double scale, double foot, Vec3 axis, double tumble) {
        double s = scale * m.grow();
        double height = DROP * (1.0 - m.fall()) + foot * s - (m.struck() ? 0.12 * s : 0.0);
        double angle = tumble * (1.0 - m.go());
        return new ConstructPainter.Frame(m.ground().add(0.0, height, 0.0),
                ConstructPainter.spin(m.right(), axis, angle), ConstructPainter.spin(UP, axis, angle),
                ConstructPainter.spin(m.forward(), axis, angle), s);
    }

    /** While something drops, a ring of light on the ground shows where it will strike. */
    private static void marker(ConstructPainter painter, Moment m, double size) {
        if (m.struck() || m.t() < 1.0) {
            return;
        }
        double pulse = 0.6 + 0.4 * Math.sin(m.t() * 1.8);
        painter.circle(m.ground().add(0.0, 0.06, 0.0), m.right(), m.forward(), size * (1.3 - 0.5 * m.go()), 0.07,
                0.35, ConstructPainter.alpha(0.8 * pulse), ConstructPainter.alpha(0.4 * pulse));
    }

    /** After the strike, what struck still shivers a moment: a small shake that dies away. */
    private static double vibrate(Moment m, double size) {
        if (m.since() <= 0.0) {
            return 0.0;
        }
        return size * Math.abs(Math.sin(m.since() * 2.6)) * Math.max(0.0, 1.0 - m.since() / 10.0);
    }

    /** 0 to 1 with a hair of overshoot on the way, like something snapping into shape. */
    private static double backOut(double x) {
        double c = Mth.clamp(x, 0.0, 1.0) - 1.0;
        return 1.0 + 2.2 * c * c * c + 1.2 * c * c;
    }

    /**
     * Where his own fist smashed into the ground, beside him: cracks of light running out from it over the ground,
     * and chunks thrown up.
     */
    private static void fistImpact(ConstructPainter painter, Moment m) {
        Vec3 at = m.him().add(m.forward().scale(0.4)).add(m.right().scale(0.3));
        if (m.t() < 20.0) {
            cracks(painter, at, 1.5, 1.0 - m.t() / 20.0, 1);
        }
        rubble(painter, at, 0.0, m.t(), 7, 0.16, 0.24, 1);
    }

    /**
     * Cracks of light running out over the ground from {@code at}, jagged, some longer than others. They are light in
     * the ground, not a construct, so they may die down.
     */
    private static void cracks(ConstructPainter painter, Vec3 at, double reach, double strength, int seed) {
        if (strength <= 0.0) {
            return;
        }
        int count = 9;
        for (int k = 0; k < count; k++) {
            double angle = Math.PI * 2.0 * (k + ConstructPainter.noise(seed, k, 1)) / count;
            double length = reach * (0.55 + 0.45 * ConstructPainter.noise(seed, k, 2));
            Vec3 last = at.add(0.0, 0.03, 0.0);
            for (int j = 1; j <= 4; j++) {
                double d = length * j / 4.0;
                double bend = angle + (ConstructPainter.noise(seed, k, 3 + j) - 0.5) * 0.7;
                Vec3 next = at.add(Math.cos(bend) * d, 0.03, Math.sin(bend) * d);
                painter.edge(last, next, 0.07 * (1.25 - 0.2 * j), Math.min(1.0, strength));
                last = next;
            }
        }
    }

    /**
     * Chunks of hard light thrown up out of the ground at {@code at} from tick {@code from}: they fly up and out,
     * tumbling, and fall back into the ground.
     */
    private static void rubble(ConstructPainter painter, Vec3 at, double from, double t, int count, double size,
            double speed, int seed) {
        double age = t - from;
        if (age < 0.0 || age > 16.0) {
            return;
        }
        for (int k = 0; k < count; k++) {
            double yaw = Math.PI * 2.0 * (k + ConstructPainter.noise(seed, k, 5)) / count;
            double up = (0.8 + 0.8 * ConstructPainter.noise(seed, k, 6)) * speed * 1.4;
            double out = speed * (0.6 + 0.8 * ConstructPainter.noise(seed, k, 7));
            Vec3 pos = at.add(Math.cos(yaw) * out * age, up * age - 0.05 * age * age, Math.sin(yaw) * out * age);
            if (pos.y < at.y - 0.2) {
                continue;
            }
            double chunk = size * (0.6 + 0.8 * ConstructPainter.noise(seed, k, 8));
            painter.chunk(pos, chunk, ConstructPainter.direction(seed + k, 4), age * 0.5, 1.0);
        }
    }

    /**
     * The shockwave: a low ring of solid hard light racing out over the ground to the edge of its reach and sinking
     * away as it goes, with a line of light at its foot and a flash where it struck.
     */
    private static void wave(ConstructPainter painter, Moment m, double reach) {
        double since = m.since();
        if (since >= WAVE_FADE) {
            return;
        }
        double out = 1.0 - Math.pow(1.0 - Mth.clamp(since / WAVE_TICKS, 0.0, 1.0), 3.0);
        double radius = Math.max(0.3, reach * out);
        double height = 0.6 * Math.pow(1.0 - since / WAVE_FADE, 1.4);
        int segments = 40;
        double half = Math.PI * radius / segments;
        Vec3 foot = m.ground().add(0.0, 0.02, 0.0);
        double[][] block = { { -half, 0.0, -0.13, half, height, 0.13, 1.1 } };
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2.0 * (i + 0.5) / segments;
            Vec3 way = m.right().scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
            painter.model(block, new ConstructPainter.Frame(foot.add(way.scale(radius)), way.cross(UP), UP, way, 1.0),
                    1.0, 1.0);
        }
        double fade = Math.pow(1.0 - since / WAVE_FADE, 1.5);
        painter.circle(foot.add(0.0, 0.03, 0.0), m.right(), m.forward(), radius, 0.12 + 0.12 * fade, 0.9,
                ConstructPainter.alpha(fade), ConstructPainter.alpha(0.6 * fade));
        if (since < 5.0) {
            double burst = 1.0 - since / 5.0;
            painter.flare(m.ground().add(0.0, 0.6, 0.0), 0.5 + 1.6 * burst, burst);
        }
    }

    // ---- The constructs ----

    /** The fist out of the sky: knuckles down, the back of the hand towards him. */
    private static Vec3 skyFist(ConstructPainter painter, Moment m) {
        double s = 3.3 / FIST_WIDTH * m.grow();
        double height = DROP * (1.0 - m.fall()) - (m.struck() ? 0.15 * s : 0.0);
        Vec3 towards = m.forward().scale(-1.0);
        ConstructPainter.Frame frame = new ConstructPainter.Frame(m.ground().add(0.0, FIST_REACH * s + height, 0.0),
                DOWN.cross(towards), towards, DOWN, s);
        marker(painter, m, 1.5);
        piece(painter, ConstructPainter.fistModel(), frame, m);
        return frame.at(0.0, 0.0, -1.25);
    }

    /** Two open hands, a left and a right, that clap together in front of him. */
    private static Vec3 hands(ConstructPainter painter, Moment m) {
        double s = 1.45 * m.grow();
        Vec3 clap = m.ground().add(0.0, 1.9, 0.0);
        double apart = 0.14 * s + 3.2 * (1.0 - m.fall()) + vibrate(m, 0.06);
        Vec3 back = m.forward().scale(-1.0);
        piece(painter, SlamModels.OPEN_HAND, new ConstructPainter.Frame(clap.add(m.right().scale(apart)), m.right(),
                UP, back, s), m);
        piece(painter, SlamModels.OPEN_HAND, new ConstructPainter.Frame(clap.subtract(m.right().scale(apart)),
                m.right().scale(-1.0), UP, back, s), m);
        return clap;
    }

    /** Two fists that bump knuckles in front of him. */
    private static Vec3 fists(ConstructPainter painter, Moment m) {
        double s = 2.2 / FIST_WIDTH * m.grow();
        Vec3 meet = m.ground().add(0.0, 1.7, 0.0);
        double apart = FIST_REACH * s + 3.0 * (1.0 - m.fall()) + vibrate(m, 0.05);
        Vec3 right = m.right();
        Vec3 in = right.scale(-1.0);
        piece(painter, ConstructPainter.fistModel(), new ConstructPainter.Frame(meet.add(right.scale(apart)),
                in.cross(UP), UP, in, s), m);
        // The left fist: the same fist, mirrored, so its thumb is on the other side.
        piece(painter, ConstructPainter.fistModel(), new ConstructPainter.Frame(meet.subtract(right.scale(apart)),
                right.cross(UP).scale(-1.0), UP, right, s), m);
        return meet;
    }

    /**
     * The lantern emblem: a ring with a bar above and below it. It stands up on its bottom edge, then falls flat on
     * its face away from him, so it lands with its middle on the middle of the wave.
     */
    private static Vec3 emblem(ConstructPainter painter, Moment m) {
        double s = LandingSlam.EMBLEM_HALF / EMBLEM_HALF * m.grow();
        double half = EMBLEM_HALF * s;
        Vec3 edge = m.ground().subtract(m.forward().scale(half));
        double angle = Mth.HALF_PI * m.fall();
        // Its own up and the way its face looks, as it tips over forwards.
        Vec3 up = UP.scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
        Vec3 face = m.forward().scale(-Math.cos(angle)).add(UP.scale(Math.sin(angle)));
        Vec3 middle = edge.add(up.scale(half));
        piece(painter, SlamModels.EMBLEM_BARS, new ConstructPainter.Frame(middle, m.right(), up, face, s), m);
        double radius = (EMBLEM_INNER + EMBLEM_OUTER) * 0.5;
        double length = radius * Math.tan(Math.PI / EMBLEM_PIECES) + 0.03;
        double[][] bit = { { -length, -(EMBLEM_OUTER - EMBLEM_INNER) * 0.5, -0.12, length,
                (EMBLEM_OUTER - EMBLEM_INNER) * 0.5, 0.12, 1.05 } };
        for (int i = 0; i < EMBLEM_PIECES; i++) {
            double a = Math.PI * 2.0 * i / EMBLEM_PIECES;
            Vec3 out = m.right().scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            Vec3 along = m.right().scale(-Math.sin(a)).add(up.scale(Math.cos(a)));
            piece(painter, bit, new ConstructPainter.Frame(middle.add(out.scale(radius * s)), along, out, face, s), m);
        }
        return middle;
    }

    /** Two cymbals that crash together in front of him and ring against each other. */
    private static Vec3 cymbals(ConstructPainter painter, Moment m) {
        // At the end they shrink away as they fly apart, still solid.
        double s = 1.9 * m.grow() * (1.0 - m.apart());
        Vec3 meet = m.ground().add(0.0, 1.9, 0.0);
        double apart = 0.08 + 3.2 * (1.0 - m.fall()) + vibrate(m, 0.12) + 3.0 * m.apart();
        if (s > 0.02) {
            cymbal(painter, meet.add(m.right().scale(apart)), m.right().scale(-1.0), s, m.flash());
            cymbal(painter, meet.subtract(m.right().scale(apart)), m.right(), s, m.flash());
        }
        return meet;
    }

    /**
     * A cymbal: a thin dish of hard light with a bell in its middle, the hollow side facing {@code toward} (the
     * other cymbal), grooves round it and a bright rim.
     */
    private static void cymbal(ConstructPainter painter, Vec3 center, Vec3 toward, double radius, double flash) {
        Vec3 a = Math.abs(toward.y) < 0.9 ? toward.cross(UP).normalize() : toward.cross(new Vec3(1.0, 0.0, 0.0))
                .normalize();
        Vec3 b = toward.cross(a).normalize();
        Vec3 back = toward.scale(-1.0);
        int sides = 24;
        double bell = 0.26;
        double dish = 0.07 * radius;
        double dome = 0.12 * radius;
        Vec3 top = center.add(back.scale(dish + dome));
        Vec3[] rim = new Vec3[sides];
        Vec3[] base = new Vec3[sides];
        for (int i = 0; i < sides; i++) {
            double angle = Math.PI * 2.0 * i / sides;
            Vec3 out = a.scale(Math.cos(angle)).add(b.scale(Math.sin(angle)));
            rim[i] = center.add(out.scale(radius));
            base[i] = center.add(out.scale(bell * radius)).add(back.scale(dish));
        }
        for (int i = 0; i < sides; i++) {
            int j = (i + 1) % sides;
            double light = ConstructPainter.lit(rim[i], rim[j], base[j], center.add(toward));
            painter.side(rim[i], rim[j], base[j], base[i],
                    ConstructPainter.shade(ConstructPainter.MASS_GREEN, Math.min(1.0, light * (1.0 + 0.3 * flash))),
                    1.0);
            painter.side(base[i], base[j], top, top,
                    ConstructPainter.shade(ConstructPainter.MASS_GREEN, Math.min(1.0, light * 1.1)), 1.0);
            painter.edge(rim[i], rim[j], 0.06, 1.0 + flash);
            painter.edge(base[i], base[j], 0.035, 0.7);
        }
        // Two grooves turned into it.
        for (double groove : new double[] { 0.52, 0.76 }) {
            Vec3 last = null;
            for (int i = 0; i <= sides; i++) {
                double angle = Math.PI * 2.0 * i / sides;
                Vec3 point = center.add(a.scale(Math.cos(angle) * groove * radius))
                        .add(b.scale(Math.sin(angle) * groove * radius))
                        .add(back.scale(dish * (1.0 - groove) / (1.0 - bell)));
                if (last != null) {
                    painter.edge(last, point, 0.02, 0.45);
                }
                last = point;
            }
        }
    }

    /**
     * The uppercut. First the ground cracks open where it will come out, brighter and brighter, and rumbles; then a
     * giant fist bursts up out of it knuckles first, its arm behind it, fastest as it breaks through (that is the
     * strike) and slowing down towards the top, while chunks of ground fly up around it. It hangs up there a moment,
     * and at the end it sinks back into the ground the way it came.
     */
    private static Vec3 uppercut(ConstructPainter painter, Moment m) {
        Vec3 ground = m.ground();
        double s = 3.0 / FIST_WIDTH;
        double warn = Mth.clamp(m.t() / (LandingSlam.IMPACT_TICK - 1.0), 0.0, 1.0);
        cracks(painter, ground, 1.2 + 1.6 * warn, m.struck() ? 1.0 - m.since() / 12.0 : warn, 7);
        double out = Mth.clamp((m.t() - (LandingSlam.IMPACT_TICK - 0.6)) / 3.5, 0.0, 1.0);
        double rise = 1.0 - Math.pow(1.0 - out, 2.2);
        double back = ConstructPainter.smooth((m.t() - LandingSlam.BURST_TICK) / 7.0);
        double knuckles = Mth.lerp(rise, -0.8, 4.4) - back * 6.0;
        Vec3 towards = m.forward().scale(-1.0);
        ConstructPainter.Frame frame = new ConstructPainter.Frame(ground.add(0.0, knuckles - FIST_REACH * s, 0.0),
                UP.cross(towards), towards, UP, s);
        if (knuckles > -1.5) {
            painter.model(ConstructPainter.fistModel(), frame, 1.0, m.bright());
            painter.model(SlamModels.FOREARM, frame, 1.0, m.bright());
        }
        rubble(painter, ground, LandingSlam.IMPACT_TICK - 0.5, m.t(), 12, 0.34, 0.42, 3);
        // Before it comes out, the ring pours its light into the ground where it will.
        return out > 0.0 ? frame.at(0.0, 0.0, -1.6) : ground;
    }

    /**
     * The spikes: the ground cracks open all around where it strikes, then a ring close by and a wider one further
     * out shoot up out of it one after the other, leaning outwards, with chunks of ground flying; at the end they sink
     * back into the ground.
     */
    private static Vec3 spikes(ConstructPainter painter, Moment m) {
        Vec3 ground = m.ground();
        double warn = Mth.clamp(m.t() / (LandingSlam.IMPACT_TICK - 1.0), 0.0, 1.0);
        cracks(painter, ground, 1.0 + 2.6 * warn, m.struck() ? 1.0 - m.since() / 12.0 : warn, 11);
        int[] counts = { 7, 11 };
        double[] radii = { 1.5, 3.2 };
        double[] sizes = { 1.3, 1.0 };
        double back = ConstructPainter.smooth((m.t() - LandingSlam.BURST_TICK) / 7.0);
        for (int ring = 0; ring < counts.length; ring++) {
            double rise = Mth.clamp((m.t() - (LandingSlam.IMPACT_TICK - 1.0 + ring * 1.5)) / 2.5, 0.0, 1.0);
            double up = 1.0 - Math.pow(1.0 - rise, 3.0) - back;
            if (up <= 0.02) {
                continue;
            }
            for (int i = 0; i < counts[ring]; i++) {
                double angle = Math.PI * 2.0 * (i + 0.5 * ring) / counts[ring];
                Vec3 out = m.right().scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
                Vec3 lean = UP.add(out.scale(0.4)).normalize();
                Vec3 along = out.cross(UP);
                Vec3 base = ground.add(out.scale(radii[ring])).add(lean.scale((up - 1.0) * 3.2 * sizes[ring]));
                painter.model(SlamModels.SPIKE, new ConstructPainter.Frame(base, along.cross(lean), lean, along,
                        sizes[ring]), 1.0, m.bright());
            }
        }
        rubble(painter, ground, LandingSlam.IMPACT_TICK - 0.5, m.t(), 10, 0.3, 0.35, 11);
        return ground.add(0.0, 1.5, 0.0);
    }

    /** The sword: it drops point first out of the sky and plunges into the ground, where it stays standing. */
    private static Vec3 sword(ConstructPainter painter, Moment m) {
        double s = 1.4 * m.grow();
        double lean = Math.toRadians(6.0);
        Vec3 up = UP.scale(Math.cos(lean)).subtract(m.forward().scale(Math.sin(lean)));
        double plunge = m.struck() ? 1.0 * s : 0.0;
        Vec3 tip = m.ground().add(0.0, (DROP + 3.0) * (1.0 - m.fall()), 0.0).subtract(up.scale(plunge));
        ConstructPainter.Frame frame = new ConstructPainter.Frame(tip, m.right(), up, up.cross(m.right()), s);
        marker(painter, m, 0.8);
        piece(painter, SlamModels.SWORD, frame, m);
        return frame.at(0.0, 3.9, 0.0);
    }

    /**
     * The rockets: five of them go up from behind him and arc over his head, their ends burning, and come down on and
     * around where it strikes one after the other, each with a blast and chunks flying.
     */
    private static Vec3 rockets(ConstructPainter painter, Moment m) {
        Vec3 anchor = m.ground().add(0.0, 1.0, 0.0);
        for (int i = 0; i < 5; i++) {
            Vec3 target = LandingSlam.rocketTarget(m.ground(), m.forward(), i);
            double lands = LandingSlam.IMPACT_TICK - 2.0 + i;
            double p = Mth.clamp((m.t() - (lands - 6.0)) / 6.0, 0.0, 1.0);
            Vec3 from = m.him().subtract(m.forward()).add(m.right().scale((i - 2) * 0.8)).add(0.0, 2.6 + 0.3 * i,
                    0.0);
            Vec3 to = target.add(0.0, 0.4, 0.0);
            Vec3 over = from.lerp(to, 0.5).add(0.0, 6.0, 0.0);
            if (p > 0.0 && p < 1.0) {
                double u = 1.0 - p;
                Vec3 at = from.scale(u * u).add(over.scale(2.0 * u * p)).add(to.scale(p * p));
                Vec3 way = over.subtract(from).scale(2.0 * u).add(to.subtract(over).scale(2.0 * p)).normalize();
                Vec3 top = UP.subtract(way.scale(way.y));
                top = top.lengthSqr() < 1.0E-6 ? m.right() : top.normalize();
                double size = 1.1 * Math.min(1.0, p * 6.0);
                ConstructPainter.Frame frame = new ConstructPainter.Frame(at, way.cross(top), top, way, size);
                painter.model(SlamModels.ROCKET, frame, 1.0, 1.1);
                painter.flare(frame.at(0.0, 0.0, -1.5), 0.35 * size, 1.0);
                anchor = at;
            }
            double since = m.t() - lands;
            if (since >= 0.0 && since < 5.0) {
                double burst = 1.0 - since / 5.0;
                painter.flare(target.add(0.0, 0.5, 0.0), 0.6 + 1.6 * burst, burst);
            }
            rubble(painter, target, lands, m.t(), 5, 0.22, 0.3, 20 + i);
        }
        return anchor;
    }

    /**
     * The fly swatter: its handle in his hand, it comes up from behind him over his shoulder and swings down flat onto
     * the ground ahead of him, and bounces a little on it.
     */
    private static Vec3 swatter(ConstructPainter painter, Moment m) {
        Vec3 pivot = m.him().add(0.0, 1.0, 0.0).add(m.right().scale(0.35));
        double g = m.grow();
        Vec3 plate = m.ground().add(0.0, 0.12, 0.0);
        Vec3 near = plate.subtract(m.forward().scale(1.6 * g));
        // The whole swatter turns about his hand, from behind him to flat on the ground.
        double swing = Math.toRadians(160.0 * (1.0 - m.fall()) + 8.0 * vibrate(m, 1.0));
        Vec3 axis = m.right();
        Vec3 nearNow = pivot.add(ConstructPainter.spin(near.subtract(pivot), axis, swing));
        Vec3 plateNow = pivot.add(ConstructPainter.spin(plate.subtract(pivot), axis, swing));
        Vec3 along = ConstructPainter.spin(m.forward(), axis, swing);
        Vec3 face = ConstructPainter.spin(UP, axis, swing);
        Vec3 stick = nearNow.subtract(pivot);
        double length = Math.max(0.1, stick.length());
        Vec3 dir = stick.scale(1.0 / length);
        double[][] handle = { { -0.1, 0.0, -0.1, 0.1, length, 0.1, 1.0 } };
        piece(painter, handle, new ConstructPainter.Frame(pivot, axis, dir, dir.cross(axis), 1.0), m);
        piece(painter, SlamModels.SWATTER, new ConstructPainter.Frame(plateNow, face.cross(along), along, face, g), m);
        return pivot.add(dir.scale(0.5));
    }

    /** His lantern, the power battery, dropping out of the sky, its light burning inside it. */
    private static Vec3 lantern(ConstructPainter painter, Moment m) {
        ConstructPainter.Frame frame = dropped(m, 1.8, 0.0, m.right(), 0.0);
        marker(painter, m, 1.8);
        piece(painter, SlamModels.LANTERN, frame, m);
        if (m.apart() <= 0.0) {
            painter.flare(frame.at(0.0, 1.05, 0.0), 0.8 + 0.8 * m.flash(), 0.9);
        }
        return frame.at(0.0, 2.58, 0.0);
    }

    /** A ship's anchor dropping crown first, its chain running on up into the sky link by link. */
    private static Vec3 anchor(ConstructPainter painter, Moment m) {
        ConstructPainter.Frame frame = dropped(m, 1.5, 0.0, m.right(), 0.0);
        marker(painter, m, 1.6);
        piece(painter, SlamModels.ANCHOR, frame, m);
        for (int i = 0; i < 12; i++) {
            Vec3 at = frame.at(0.0, SlamModels.ANCHOR_TOP + 0.2 + 0.4 * i, 0.0);
            // Every other link turned a quarter round, the way a chain hangs.
            ConstructPainter.Frame link = (i & 1) == 0
                    ? new ConstructPainter.Frame(at, m.right(), UP, m.forward(), frame.scale())
                    : new ConstructPainter.Frame(at, m.forward(), UP, m.right().scale(-1.0), frame.scale());
            piece(painter, SlamModels.LINK, link, m);
        }
        return frame.at(0.0, SlamModels.ANCHOR_TOP, 0.0);
    }

    /** A spiked ball, tumbling as it drops, that lands with its spikes in the ground. */
    private static Vec3 mace(ConstructPainter painter, Moment m) {
        double s = 1.2 * m.grow();
        double height = DROP * (1.0 - m.fall()) + 1.35 * s - (m.struck() ? 0.5 * s : 0.0);
        Vec3 center = m.ground().add(0.0, height, 0.0);
        Vec3 axis = m.right().add(m.forward()).normalize();
        double turn = 5.0 * (1.0 - m.go());
        Vec3 r = ConstructPainter.spin(m.right(), axis, turn);
        Vec3 u = ConstructPainter.spin(UP, axis, turn);
        Vec3 f = ConstructPainter.spin(m.forward(), axis, turn);
        marker(painter, m, 1.4);
        piece(painter, SlamModels.BALL, new ConstructPainter.Frame(center, r, u, f, s), m);
        for (Vec3 d : STUDS) {
            Vec3 out = r.scale(d.x).add(u.scale(d.y)).add(f.scale(d.z));
            Vec3 side = Math.abs(out.y) < 0.9 ? out.cross(UP).normalize() : out.cross(m.right()).normalize();
            piece(painter, SlamModels.STUD, new ConstructPainter.Frame(center.add(out.scale(0.85 * s)),
                    side.cross(out), out, side, s), m);
        }
        return center;
    }

    /** A bell dropping on its rim, swinging a little on the way, that rings when it lands. */
    private static Vec3 bell(ConstructPainter painter, Moment m) {
        ConstructPainter.Frame frame = dropped(m, 1.6, 0.0, m.forward(), 0.4);
        if (m.struck() && m.apart() <= 0.0) {
            double ring = 0.05 * Math.sin(m.since() * 3.0) * Math.max(0.0, 1.0 - m.since() / 8.0);
            frame = new ConstructPainter.Frame(frame.center(), ConstructPainter.spin(frame.right(), m.forward(), ring),
                    ConstructPainter.spin(frame.up(), m.forward(), ring), frame.forward(), frame.scale());
        }
        marker(painter, m, 1.6);
        piece(painter, SlamModels.BELL, frame, m);
        return frame.at(0.0, 2.38, 0.0);
    }

    /**
     * A meteor: it streaks in out of the sky from far ahead of him, burning and tumbling, and slams half into the
     * ground where it strikes.
     */
    private static Vec3 meteor(ConstructPainter painter, Moment m) {
        double s = 1.3 * m.grow();
        Vec3 end = m.ground().add(0.0, 0.55 * s - (m.struck() ? 0.45 * s : 0.0), 0.0);
        Vec3 start = m.ground().add(m.forward().scale(14.0)).add(m.right().scale(4.0)).add(0.0, 13.0, 0.0);
        Vec3 at = start.lerp(end, m.fall());
        Vec3 axis = m.right().add(0.0, 0.4, 0.0).normalize();
        double turn = 6.0 * (1.0 - m.go());
        ConstructPainter.Frame frame = new ConstructPainter.Frame(at, ConstructPainter.spin(m.right(), axis, turn),
                ConstructPainter.spin(UP, axis, turn), ConstructPainter.spin(m.forward(), axis, turn), s);
        marker(painter, m, 1.4);
        piece(painter, SlamModels.METEOR, frame, m);
        if (!m.struck()) {
            // It burns as it comes in: a blaze on it and a streak of fire behind it.
            Vec3 back = start.subtract(end).normalize();
            painter.flare(at, 1.4 * s, 0.9);
            painter.edge(at.add(back.scale(0.8 * s)), at.add(back.scale(6.0 * s)), 0.5 * s, 0.8);
        }
        return at;
    }

    /** A giant open hand that drops palm down, its fingers pointing away from him, and slaps the ground flat. */
    private static Vec3 palm(ConstructPainter painter, Moment m) {
        double s = 2.2 * m.grow();
        double height = DROP * (1.0 - m.fall()) + 0.16 * s - (m.struck() ? 0.08 * s : 0.0);
        // The hand's own x (the back of the hand) is up, its y (the fingers) ahead, its z (the thumb) to his right.
        ConstructPainter.Frame frame = new ConstructPainter.Frame(m.ground().add(0.0, height, 0.0), UP, m.forward(),
                m.right(), s);
        marker(painter, m, 2.2);
        piece(painter, SlamModels.OPEN_HAND, frame, m);
        return frame.at(0.0, -1.2, 0.0);
    }

    /** A judge's gavel: its block stands on the ground, and the gavel swings down from high up and strikes it. */
    private static Vec3 gavel(ConstructPainter painter, Moment m) {
        double s = 1.3 * m.grow();
        piece(painter, SlamModels.SOUND_BLOCK, new ConstructPainter.Frame(m.ground(), m.right(), UP, m.forward(), s),
                m);
        double reach = SlamModels.GAVEL_REACH * s;
        // It turns about the end of its handle, beyond the block.
        Vec3 pivot = m.ground().add(m.forward().scale(reach)).add(0.0, 0.72 * s, 0.0);
        double angle = Math.toRadians(80.0 * (1.0 - m.fall()) + 12.0 * vibrate(m, 1.0));
        Vec3 out = m.forward().scale(-Math.cos(angle)).add(UP.scale(Math.sin(angle)));
        Vec3 up = out.scale(-1.0);
        piece(painter, SlamModels.GAVEL, new ConstructPainter.Frame(pivot.add(out.scale(reach)), m.right(), up,
                up.cross(m.right()), s), m);
        return pivot;
    }

    /**
     * A pickaxe: its handle in his hand, it swings from behind him up over his head and down, its point biting into
     * the ground ahead of him.
     */
    private static Vec3 pickaxe(ConstructPainter painter, Moment m) {
        Vec3 pivot = m.him().add(0.0, 1.05, 0.0).add(m.right().scale(0.35));
        Vec3 to = m.ground().subtract(0.0, 0.25, 0.0).subtract(pivot);
        double tip = Math.sqrt(SlamModels.PICK_TIP_X * SlamModels.PICK_TIP_X
                + SlamModels.PICK_TIP_Y * SlamModels.PICK_TIP_Y);
        double s = Math.max(0.5, to.length() / tip);
        // Angles in the upright plane through the way he faces, from straight up (0) towards ahead.
        double strike = Math.atan2(to.dot(m.forward()), to.y) - Math.atan2(SlamModels.PICK_TIP_X,
                SlamModels.PICK_TIP_Y);
        double angle = Mth.lerp(m.fall(), Math.toRadians(-120.0), strike);
        Vec3 up = UP.scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
        Vec3 lead = UP.scale(-Math.sin(angle)).add(m.forward().scale(Math.cos(angle)));
        ConstructPainter.Frame frame = new ConstructPainter.Frame(pivot, lead, up, up.cross(lead), s * m.grow());
        piece(painter, SlamModels.PICKAXE, frame, m);
        return frame.at(0.0, 0.3, 0.0);
    }

    /** A bear trap: its jaws lie open flat on the ground, then snap up and shut, their teeth biting into each other. */
    private static Vec3 trap(ConstructPainter painter, Moment m) {
        double s = 1.6 * m.grow();
        Vec3 ground = m.ground();
        Vec3 f = m.forward();
        Vec3 r = m.right();
        piece(painter, SlamModels.TRAP_BASE, new ConstructPainter.Frame(ground, r, UP, f, s), m);
        double angle = Math.toRadians(Mth.lerp(m.fall(), 88.0, -10.0) + 4.0 * vibrate(m, 1.0));
        double c = Math.cos(angle);
        double sin = Math.sin(angle);
        Vec3 hingeA = ground.add(f.scale(0.40 * s)).add(0.0, 0.12 * s, 0.0);
        piece(painter, SlamModels.JAW_A, new ConstructPainter.Frame(hingeA, r, UP.scale(c).add(f.scale(sin)),
                f.scale(c).subtract(UP.scale(sin)), s), m);
        Vec3 hingeB = ground.subtract(f.scale(0.40 * s)).add(0.0, 0.12 * s, 0.0);
        piece(painter, SlamModels.JAW_B, new ConstructPainter.Frame(hingeB, r.scale(-1.0),
                UP.scale(c).subtract(f.scale(sin)), f.scale(-c).subtract(UP.scale(sin)), s), m);
        return ground.add(0.0, 0.3 * s, 0.0);
    }

    /**
     * A giant book standing in the air ahead of him, spread open towards him, that slams shut: its covers swing in
     * and clap together.
     */
    private static Vec3 book(ConstructPainter painter, Moment m) {
        double s = 1.3 * m.grow();
        Vec3 r = m.right();
        Vec3 f = m.forward();
        Vec3 spine = m.ground().add(f.scale(1.0)).add(0.0, 1.45 * s + 0.1, 0.0);
        double open = Math.toRadians(Mth.lerp(m.fall(), 70.0, 0.0) + 5.0 * vibrate(m, 1.0));
        Vec3 coverA = f.scale(-Math.cos(open)).add(r.scale(Math.sin(open)));
        Vec3 coverB = f.scale(-Math.cos(open)).subtract(r.scale(Math.sin(open)));
        piece(painter, SlamModels.COVER_NEG, new ConstructPainter.Frame(spine.add(r.scale(0.34 * s)), coverA, UP,
                UP.cross(coverA), s), m);
        piece(painter, SlamModels.COVER_POS, new ConstructPainter.Frame(spine.subtract(r.scale(0.34 * s)), coverB, UP,
                UP.cross(coverB), s), m);
        piece(painter, SlamModels.SPINE, new ConstructPainter.Frame(spine, r, UP, f, s), m);
        return spine;
    }

    /** A drum standing on the ground, and two drumsticks that come down on its head together. */
    private static Vec3 drum(ConstructPainter painter, Moment m) {
        double s = 1.4 * m.grow();
        Vec3 ground = m.ground();
        Vec3 f = m.forward();
        Vec3 r = m.right();
        piece(painter, SlamModels.DRUM, new ConstructPainter.Frame(ground, r, UP, f, s), m);
        double head = SlamModels.DRUM_TOP * s;
        Vec3 raised = UP.scale(0.85).subtract(f.scale(0.5)).normalize();
        for (int k = -1; k <= 1; k += 2) {
            Vec3 pivot = ground.subtract(f.scale(1.7 * s)).add(0.0, 1.9 * s, 0.0).add(r.scale(k * 0.55 * s));
            Vec3 tip = ground.add(0.0, head + 0.12 * s, 0.0).add(r.scale(k * 0.32 * s));
            Vec3 strike = tip.subtract(pivot);
            double length = strike.length();
            strike = strike.scale(1.0 / length);
            // A little bounce back off the drumhead after the blow.
            Vec3 dir = raised.lerp(strike, m.fall()).lerp(raised, 0.25 * vibrate(m, 1.0)).normalize();
            Vec3 side = r.subtract(dir.scale(r.dot(dir))).normalize();
            piece(painter, SlamModels.STICK, new ConstructPainter.Frame(pivot, side, dir, dir.cross(side),
                    length / SlamModels.STICK_REACH), m);
        }
        return ground.add(0.0, head, 0.0);
    }

    /**
     * A pillar: the ground cracks open, the pillar bursts up out of it, and then it topples over away from him and
     * crashes down flat on the ground, its middle where it strikes.
     */
    private static Vec3 pillar(ConstructPainter painter, Moment m) {
        double s = 1.2;
        double height = SlamModels.PILLAR_HEIGHT * s;
        Vec3 base = m.ground().subtract(m.forward().scale(height * 0.5));
        double rise = Mth.clamp((m.t() - 2.0) / 4.0, 0.0, 1.0);
        double up = 1.0 - (1.0 - rise) * (1.0 - rise);
        double tip = Mth.clamp((m.t() - 6.0) / (LandingSlam.IMPACT_TICK - 6.0), 0.0, 1.0);
        double angle = Mth.HALF_PI * tip * tip;
        Vec3 along = UP.scale(Math.cos(angle)).add(m.forward().scale(Math.sin(angle)));
        Vec3 ahead = m.forward().scale(Math.cos(angle)).subtract(UP.scale(Math.sin(angle)));
        double crack = m.t() < 12.0 ? Math.min(1.0, m.t() / 4.0) : 1.0 - (m.t() - 12.0) / 8.0;
        cracks(painter, base, 1.2 + 1.2 * rise, crack, 23);
        rubble(painter, base, 2.5, m.t(), 10, 0.32, 0.35, 23);
        ConstructPainter.Frame frame = new ConstructPainter.Frame(base.subtract(along.scale((1.0 - up) * height)),
                m.right(), along, ahead, s);
        if (up > 0.02) {
            piece(painter, SlamModels.PILLAR, frame, m);
        }
        return frame.at(0.0, SlamModels.PILLAR_HEIGHT * 0.5, 0.0);
    }

    /**
     * A block of TNT: it drops, lands with a bounce, swells and flashes while its fuse burns, and blows apart as it
     * strikes.
     */
    private static Vec3 tnt(ConstructPainter painter, Moment m) {
        double s = 1.6 * m.grow();
        double lands = 7.0;
        double go = Mth.clamp((m.t() - LandingSlam.FORM_TICKS) / (lands - LandingSlam.FORM_TICKS), 0.0, 1.0);
        double height = DROP * (1.0 - go * go);
        double sitting = m.t() - lands;
        if (sitting > 0.0) {
            height = 0.4 * s * Math.sin(Math.PI * Mth.clamp(sitting / 1.4, 0.0, 1.0));
        }
        boolean waiting = sitting > 0.0 && !m.struck();
        double swell = waiting ? 1.0 + 0.1 * sitting / (LandingSlam.IMPACT_TICK - lands) : 1.0;
        double blink = waiting ? 0.35 * Math.abs(Math.sin(sitting * 4.0)) : 0.0;
        ConstructPainter.Frame frame = new ConstructPainter.Frame(m.ground().add(0.0, height, 0.0), m.right(), UP,
                m.forward(), s * swell);
        if (!m.struck()) {
            marker(painter, m, 1.6);
            painter.model(SlamModels.TNT, frame, 1.0, 1.0 + blink);
            if (sitting > -1.0) {
                // The fuse burns.
                painter.flare(frame.at(0.0, 1.85, 0.0), 0.25 + 0.08 * Math.sin(m.t() * 5.0), 1.0);
            }
        } else {
            painter.shattered(SlamModels.TNT, frame, Mth.clamp(m.since() / 7.0, 0.0, 1.0), 1.5);
            if (m.since() < 6.0) {
                double burst = 1.0 - m.since() / 6.0;
                painter.flare(m.ground().add(0.0, 1.2, 0.0), 1.0 + 3.0 * burst, burst);
            }
        }
        return frame.at(0.0, 0.75, 0.0);
    }

    /**
     * A rubber stamp: it drops and stamps the ground, lifts again, and leaves the lantern emblem glowing in the
     * ground where it stood.
     */
    private static Vec3 stamp(ConstructPainter painter, Moment m) {
        double s = 1.5 * m.grow();
        double lift = m.struck() ? 1.1 * s * ConstructPainter.smooth((m.since() - 1.5) / 5.0) : 0.0;
        ConstructPainter.Frame frame = new ConstructPainter.Frame(m.ground().add(0.0,
                DROP * (1.0 - m.fall()) + lift, 0.0), m.right(), UP, m.forward(), s);
        marker(painter, m, 1.6);
        piece(painter, SlamModels.STAMP, frame, m);
        if (m.struck()) {
            double glow = 1.0 - Math.max(0.0, m.since() - 8.0) / 10.0;
            print(painter, m.ground().add(0.0, 0.04, 0.0), m.right(), m.forward(), s, glow);
        }
        return frame.at(0.0, 2.02, 0.0);
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

    /** The ways the studs of the spiked ball stick out: along its six axes and to its eight corners. */
    private static Vec3[] studs() {
        Vec3[] all = new Vec3[14];
        int n = 0;
        for (int axis = 0; axis < 3; axis++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                all[n++] = new Vec3(axis == 0 ? sign : 0, axis == 1 ? sign : 0, axis == 2 ? sign : 0);
            }
        }
        double c = 1.0 / Math.sqrt(3.0);
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    all[n++] = new Vec3(x * c, y * c, z * c);
                }
            }
        }
        return all;
    }
}
