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
 * shockwave runs out over the ground as a low ring of hard light. The constructs are drawn in these classes:
 * <ul>
 * <li>{@link SlamHands}: the fist out of the sky, two hands that clap, two fists that bump, the uppercut and the
 * slapping hand;</li>
 * <li>{@link SlamDrops}: dropped out of the sky: a war hammer, a boot, his lantern, an anchor on its chain, a spiked
 * ball, a barbell, a bell, a meteor streaking in, a sword, and a volley of rockets;</li>
 * <li>{@link SlamCartoon}: the cartoon ones: a safe, a piano, an anvil, a ton weight, TNT, a toy brick and a
 * stamp;</li>
 * <li>{@link SlamStrikes}: clapped shut or swung down: cymbals, a bear trap, a book, a fly swatter, a pickaxe, a
 * gavel on its block, drumsticks on a drum;</li>
 * <li>{@link SlamRisers}: out of the ground: two rings of spikes, a pillar that topples over, and the lantern emblem
 * falling flat on its face.</li>
 * </ul>
 * Every construct is timed in ticks at the pace it was made for and measured at the size it was made at; the server
 * says how slowly a slam plays and how big its construct is (see {@link #pace} and {@link #size}).
 */
final class SlamPainter {
    static final Vec3 UP = ConstructPainter.UP;
    static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);
    // How high over the ground the ones that drop take shape, in blocks (their foot, at scale 1): in the air before
    // him, where he can see them; and how far they rise from there as they wind up to strike.
    static final double HANG = 2.6;
    static final double RISE = 0.7;
    // How long a construct takes to break up, in ticks.
    private static final double BREAK_TICKS = 8.0;
    // How far the ones that clap shut are turned towards him, in radians: seen straight from behind, two things that
    // meet side on would show only their thin edges.
    private static final double FACING_HIM = Math.toRadians(35.0);
    // How long the shockwave takes to run out, and to sink away, in ticks.
    private static final double WAVE_TICKS = 12.0;
    private static final double WAVE_FADE = 16.0;

    private SlamPainter() {
    }

    /**
     * Where one slam is at one moment, the same for every construct.
     *
     * @param ground  where it strikes
     * @param forward the way he faced, flat
     * @param right   his right
     * @param him     where he is (his feet)
     * @param t       ticks since he landed, at the pace the constructs were made for (see {@link #pace})
     * @param size    how big the constructs are, next to the size they were made at (1 = that size)
     * @param grow    0 to 1 (a hair over on the way): how far it has taken shape
     * @param windup  0 to 1: how far it has wound up after taking shape, hanging in the air; it stays 1 as it strikes
     * @param go      0 to 1: how far it is on its way from its wound-up hang to striking
     * @param fall    the same, but slowly at first and faster and faster, like a real fall
     * @param since   ticks since it struck (below 0 before)
     * @param flash   1 the moment it strikes, dying down over a few ticks
     * @param apart   0 to 1: how far it has broken up at the end
     * @param burst   the tick it starts to break up
     */
    record Moment(Vec3 ground, Vec3 forward, Vec3 right, Vec3 him, double t, double size, double grow,
            double windup, double go, double fall, double since, double flash, double apart, double burst) {
        boolean struck() {
            return this.since >= 0.0;
        }

        double bright() {
            return 1.0 + 0.5 * this.flash;
        }

        /** A frame standing on the ground where it strikes, facing the way he faced, at this scale. */
        ConstructPainter.Frame standing(double scale) {
            return new ConstructPainter.Frame(this.ground, this.right, UP, this.forward, scale);
        }

        /** A construct's own scale, made as big as constructs are and as far as it has taken shape. */
        double scale(double scale) {
            return scale * this.size * this.grow;
        }
    }

    /**
     * How slowly a slam plays, from what the server said about it: 1 at the pace its constructs were made for, 1.5
     * half again as slow. Every construct is timed in ticks at that first pace, so the clock is divided by this.
     */
    static double pace(ConstructPayload slam) {
        return slam.charge() > 0.0F ? slam.charge() : 1.0;
    }

    /** How big a slam's construct is, from what the server said about it: 1 at the size it was made at. */
    static double size(ConstructPayload slam) {
        return slam.solid() > 0.0F ? slam.solid() : 1.0;
    }

    /**
     * One landing slam.
     *
     * @param slam what the server said about it: where it strikes, the way its maker faced, how far the wave goes
     *             and which construct it is
     * @param clock ticks since he landed, by the client's own clock
     * @param ring  where its maker's ring is, or null when he is out of sight
     * @param him   where its maker is, or null when he is out of sight
     */
    static void draw(ConstructPainter painter, ConstructPayload slam, double clock, @Nullable Vec3 ring,
            @Nullable Vec3 him) {
        Vec3 ground = slam.center();
        double t = clock / pace(slam);
        double size = size(slam);
        Vec3 forward = new Vec3(slam.facing().x, 0.0, slam.facing().z);
        forward = forward.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
        Vec3 right = forward.cross(UP).normalize();
        Vec3 feet = him != null ? him : ground.subtract(forward.scale(LandingSlam.ahead(slam.variant(), size)));
        double windup = ConstructPainter.smooth((t - LandingSlam.FORM_TICKS)
                / (LandingSlam.HANG_TICKS - LandingSlam.FORM_TICKS));
        double go = Mth.clamp((t - LandingSlam.HANG_TICKS) / (LandingSlam.IMPACT_TICK - LandingSlam.HANG_TICKS),
                0.0, 1.0);
        double since = t - LandingSlam.IMPACT_TICK;
        double flash = since >= 0.0 ? Mth.clamp(1.0 - since / 4.0, 0.0, 1.0) : 0.0;
        double burst = burst(slam.variant());
        double apart = Mth.clamp((t - burst) / BREAK_TICKS, 0.0, 1.0);
        Moment m = new Moment(ground, forward, right, feet, t, size, backOut(t / LandingSlam.FORM_TICKS), windup, go,
                go * go, since, flash, apart, burst);
        if (apart < 1.0) {
            // Fresh out of the ring it is white-hot and cools to green as it takes shape; the moment it strikes it
            // flares up once more.
            painter.glare(Math.max(0.75 * flash, 0.55 * (1.0 - ConstructPainter.smooth(t / LandingSlam.FORM_TICKS))));
            Vec3 anchor = construct(painter, slam.variant(), m);
            painter.glare(0.0);
            // The ring feeds it while it takes shape and comes down, and lets go once it has struck.
            if (ring != null && t < LandingSlam.IMPACT_TICK + 2.0) {
                painter.beam(ring, anchor, m.struck() ? 0.5 : 1.0, 2.0);
            }
            if (drops(slam.variant())) {
                streaks(painter, anchor, m);
            }
        }
        fistImpact(painter, m);
        if (m.struck()) {
            wave(painter, m, slam.size());
        }
    }

    /** True for the constructs that drop onto the ground from where they took shape in the air. */
    static boolean drops(int variant) {
        return switch (variant) {
            case ConstructPayload.SLAM_FIST, ConstructPayload.SLAM_HAMMER, ConstructPayload.SLAM_ANVIL,
                    ConstructPayload.SLAM_BOOT, ConstructPayload.SLAM_WEIGHT, ConstructPayload.SLAM_SWORD,
                    ConstructPayload.SLAM_LANTERN, ConstructPayload.SLAM_SAFE, ConstructPayload.SLAM_ANCHOR,
                    ConstructPayload.SLAM_MACE, ConstructPayload.SLAM_BARBELL, ConstructPayload.SLAM_BELL,
                    ConstructPayload.SLAM_PALM, ConstructPayload.SLAM_PIANO, ConstructPayload.SLAM_BRICK,
                    ConstructPayload.SLAM_STAMP -> true;
            default -> false;
        };
    }

    /**
     * How far your own view looks up from where you look, in degrees, while this construct hangs in the air before
     * you: enough to see all of a big one that drops, a little for the ones that clap shut in the air, nothing for
     * the ones that come out of the ground. It follows the construct back down as it strikes.
     */
    static float look(int variant) {
        if (drops(variant)) {
            return 15.0F;
        }
        return switch (variant) {
            case ConstructPayload.SLAM_SWATTER, ConstructPayload.SLAM_PICKAXE -> 12.0F;
            case ConstructPayload.SLAM_HANDS, ConstructPayload.SLAM_FISTS, ConstructPayload.SLAM_CYMBALS,
                    ConstructPayload.SLAM_BOOK, ConstructPayload.SLAM_ROCKETS, ConstructPayload.SLAM_METEOR,
                    ConstructPayload.SLAM_EMBLEM, ConstructPayload.SLAM_GAVEL -> 8.0F;
            default -> 0.0F;
        };
    }

    /**
     * While a construct drops, lines of light streak out above it, longer the faster it goes: the air it tears
     * through. Light, not a construct.
     *
     * @param top the top of the construct, where the ring's beam meets it
     */
    private static void streaks(ConstructPainter painter, Vec3 top, Moment m) {
        if (m.struck() || m.go() <= 0.0) {
            return;
        }
        double speed = 2.0 * m.go();
        double length = (0.6 + 2.6 * speed) * m.size();
        for (int k = 0; k < 7; k++) {
            double angle = Math.PI * 2.0 * (k + ConstructPainter.noise(k, 71, 0)) / 7.0;
            double out = (0.5 + 0.9 * ConstructPainter.noise(k, 71, 1)) * m.size();
            Vec3 foot = top.add(m.right().scale(Math.cos(angle) * out)).add(m.forward().scale(Math.sin(angle) * out))
                    .subtract(0.0, (0.4 + 1.2 * ConstructPainter.noise(k, 71, 2)) * m.size(), 0.0);
            painter.edge(foot, foot.add(0.0, length * (0.6 + 0.4 * ConstructPainter.noise(k, 71, 3)), 0.0),
                    0.05 * m.size(), 0.35 + 0.5 * speed);
        }
    }

    /**
     * The tick a construct starts to break up: later for the ones that have more to show once they struck, such as a
     * safe spilling its money, but always so that it is gone before the server ends the slam.
     */
    private static double burst(int variant) {
        return switch (variant) {
            case ConstructPayload.SLAM_SAFE -> LandingSlam.END_TICK - BREAK_TICKS - 1.0;
            case ConstructPayload.SLAM_PIANO, ConstructPayload.SLAM_ANCHOR -> LandingSlam.END_TICK - BREAK_TICKS - 3.0;
            default -> LandingSlam.BURST_TICK;
        };
    }

    /** Draws the construct itself, and gives where the ring's beam feeds it. */
    private static Vec3 construct(ConstructPainter painter, int variant, Moment m) {
        return switch (variant) {
            case ConstructPayload.SLAM_HANDS -> SlamHands.hands(painter, m);
            case ConstructPayload.SLAM_FISTS -> SlamHands.fists(painter, m);
            case ConstructPayload.SLAM_HAMMER -> SlamDrops.hammer(painter, m);
            case ConstructPayload.SLAM_EMBLEM -> SlamRisers.emblem(painter, m);
            case ConstructPayload.SLAM_ANVIL -> SlamCartoon.anvil(painter, m);
            case ConstructPayload.SLAM_CYMBALS -> SlamStrikes.cymbals(painter, m);
            case ConstructPayload.SLAM_UPPERCUT -> SlamHands.uppercut(painter, m);
            case ConstructPayload.SLAM_SPIKES -> SlamRisers.spikes(painter, m);
            case ConstructPayload.SLAM_BOOT -> SlamDrops.boot(painter, m);
            case ConstructPayload.SLAM_WEIGHT -> SlamCartoon.weight(painter, m);
            case ConstructPayload.SLAM_SWORD -> SlamDrops.sword(painter, m);
            case ConstructPayload.SLAM_ROCKETS -> SlamDrops.rockets(painter, m);
            case ConstructPayload.SLAM_SWATTER -> SlamStrikes.swatter(painter, m);
            case ConstructPayload.SLAM_LANTERN -> SlamDrops.lantern(painter, m);
            case ConstructPayload.SLAM_SAFE -> SlamCartoon.safe(painter, m);
            case ConstructPayload.SLAM_ANCHOR -> SlamDrops.anchor(painter, m);
            case ConstructPayload.SLAM_MACE -> SlamDrops.mace(painter, m);
            case ConstructPayload.SLAM_BARBELL -> SlamDrops.barbell(painter, m);
            case ConstructPayload.SLAM_BELL -> SlamDrops.bell(painter, m);
            case ConstructPayload.SLAM_METEOR -> SlamDrops.meteor(painter, m);
            case ConstructPayload.SLAM_PALM -> SlamHands.palm(painter, m);
            case ConstructPayload.SLAM_GAVEL -> SlamStrikes.gavel(painter, m);
            case ConstructPayload.SLAM_PICKAXE -> SlamStrikes.pickaxe(painter, m);
            case ConstructPayload.SLAM_TRAP -> SlamStrikes.trap(painter, m);
            case ConstructPayload.SLAM_BOOK -> SlamStrikes.book(painter, m);
            case ConstructPayload.SLAM_DRUM -> SlamStrikes.drum(painter, m);
            case ConstructPayload.SLAM_PILLAR -> SlamRisers.pillar(painter, m);
            case ConstructPayload.SLAM_TNT -> SlamCartoon.tnt(painter, m);
            case ConstructPayload.SLAM_PIANO -> SlamCartoon.piano(painter, m);
            case ConstructPayload.SLAM_BRICK -> SlamCartoon.brick(painter, m);
            case ConstructPayload.SLAM_STAMP -> SlamCartoon.stamp(painter, m);
            default -> SlamHands.skyFist(painter, m);
        };
    }

    // ---- Shared ----

    /**
     * A way flat along the ground turned towards him by {@link #FACING_HIM}: things that clap shut do so along this
     * instead of straight across in front of him, so he sees their faces come together, not only their edges.
     */
    static Vec3 facingHim(Vec3 way) {
        return ConstructPainter.spin(way, UP, FACING_HIM);
    }

    /** A shape, whole while it lasts and breaking into solid pieces at the end. */
    static void piece(ConstructPainter painter, double[][] model, ConstructPainter.Frame frame, Moment m) {
        if (m.apart() > 0.0) {
            painter.shattered(model, frame, m.apart(), m.bright());
        } else {
            painter.model(model, frame, 1.0, m.bright());
        }
    }

    /** A shape of boxes and round parts, whole while it lasts and breaking into solid pieces at the end. */
    static void piece(ConstructPainter painter, ConstructPainter.Shape shape, ConstructPainter.Frame frame,
            Moment m) {
        if (m.apart() > 0.0) {
            painter.shattered(shape, frame, m.apart(), m.bright());
        } else {
            painter.shape(shape, frame, 1.0, m.bright());
        }
    }

    /**
     * 0 until {@code since} reaches 0, then on to 1 like something on a spring: fast, a little past it, back and a few
     * smaller wobbles until it rests. A door that flies open, a lid that springs up.
     *
     * @param rate how fast it swings, in radians a tick; the bigger, the snappier
     */
    static double spring(double since, double rate) {
        if (since <= 0.0) {
            return 0.0;
        }
        return 1.0 - Math.exp(-0.45 * rate * since) * Math.cos(rate * since);
    }

    /**
     * Like {@link #spring}, but for something that flies up against a stop: it never goes past 1, but bounces back off
     * it a few times, each time less.
     */
    static double bounce(double since, double rate) {
        if (since <= 0.0) {
            return 0.0;
        }
        return 1.0 - Math.abs(Math.exp(-0.45 * rate * since) * Math.cos(rate * since));
    }

    /**
     * A frame for a loose part of a construct at a point of {@code base}'s model (x, y, z), turned by {@code angle}
     * about {@code axis} (a way in the world): a coin tumbling out of a safe, a link of a falling chain.
     */
    static ConstructPainter.Frame loose(ConstructPainter.Frame base, Vec3 at, Vec3 axis, double angle) {
        return new ConstructPainter.Frame(base.at(at.x, at.y, at.z), ConstructPainter.spin(base.right(), axis, angle),
                ConstructPainter.spin(base.up(), axis, angle), ConstructPainter.spin(base.forward(), axis, angle),
                base.scale());
    }

    /**
     * The frame of something that just landed hard, squashed down and bulging out, springing back up a little too far
     * and settling again, the way a cartoon lands. Its middle has to be where it stands on the ground.
     *
     * @param amount how far it squashes at most: 0.2 is a fifth of its height
     */
    static ConstructPainter.Frame squashed(ConstructPainter.Frame frame, Moment m, double amount) {
        if (!m.struck() || m.since() > 12.0) {
            return frame;
        }
        double squash = amount * Math.exp(-0.5 * m.since()) * Math.cos(1.4 * m.since());
        double tall = 1.0 - squash;
        double wide = 1.0 / Math.sqrt(tall);
        return frame.stretched(wide, tall, wide);
    }

    /**
     * Sparks thrown out of {@code at} when something strikes: short bright streaks flying out and up, dropping as they
     * go and burning out. Light, not a construct.
     *
     * @param since ticks since it struck
     * @param speed how far they fly a tick, in blocks
     */
    static void sparks(ConstructPainter painter, Vec3 at, double since, int count, double speed, int seed) {
        if (since < 0.0 || since > 7.0) {
            return;
        }
        double fade = 1.0 - since / 7.0;
        for (int k = 0; k < count; k++) {
            Vec3 way = ConstructPainter.direction(seed, k);
            way = new Vec3(way.x, 0.25 + 0.75 * Math.abs(way.y), way.z).normalize();
            double fast = speed * (0.5 + ConstructPainter.noise(seed, k, 14));
            double before = Math.max(0.0, since - 0.9);
            Vec3 head = at.add(way.scale(fast * since)).add(0.0, -0.035 * since * since, 0.0);
            Vec3 tail = at.add(way.scale(fast * before)).add(0.0, -0.035 * before * before, 0.0);
            painter.edge(tail, head, 0.06, fade);
        }
    }

    /**
     * How high the foot of a shape that drops is over the ground: it hangs where it took shape, rises a little as it
     * winds up, and then drops, faster and faster, onto the ground.
     */
    static double drop(Moment m) {
        return (HANG + RISE * m.windup()) * m.size() * (1.0 - m.fall());
    }

    /** Where a dropped shape is: upright over where it strikes, as high as its fall has got. */
    static ConstructPainter.Frame dropped(Moment m, double scale, double foot, Vec3 axis, double tumble) {
        double s = m.scale(scale);
        double height = drop(m) + foot * s - (m.struck() ? 0.12 * s : 0.0);
        // It tips back a little as it winds up, and swings round straight on the way down.
        double angle = tumble * (1.0 - m.go()) - 0.12 * m.windup() * (1.0 - m.go());
        return new ConstructPainter.Frame(m.ground().add(0.0, height, 0.0),
                ConstructPainter.spin(m.right(), axis, angle), ConstructPainter.spin(UP, axis, angle),
                ConstructPainter.spin(m.forward(), axis, angle), s);
    }

    /** While something drops, a ring of light on the ground shows where it will strike. */
    static void marker(ConstructPainter painter, Moment m, double size) {
        if (m.struck() || m.t() < 1.0) {
            return;
        }
        double pulse = 0.6 + 0.4 * Math.sin(m.t() * 1.8);
        painter.circle(m.ground().add(0.0, 0.06, 0.0), m.right(), m.forward(),
                size * m.size() * (1.3 - 0.5 * m.go()), 0.07, 0.35, ConstructPainter.alpha(0.8 * pulse),
                ConstructPainter.alpha(0.4 * pulse));
    }

    /** After the strike, what struck still shivers a moment: a small shake that dies away. */
    static double vibrate(Moment m, double size) {
        if (m.since() <= 0.0) {
            return 0.0;
        }
        return size * Math.abs(Math.sin(m.since() * 2.6)) * Math.max(0.0, 1.0 - m.since() / 10.0);
    }

    /** 0 to 1 with a hair of overshoot on the way, like something snapping into shape. */
    static double backOut(double x) {
        double c = Mth.clamp(x, 0.0, 1.0) - 1.0;
        return 1.0 + 2.2 * c * c * c + 1.2 * c * c;
    }

    /**
     * Where his own fist smashed into the ground, beside him: cracks of light running out from it over the ground,
     * and chunks thrown up.
     */
    private static void fistImpact(ConstructPainter painter, Moment m) {
        Vec3 at = m.him().add(m.forward().scale(0.5)).add(m.right().scale(0.3));
        if (m.t() < 20.0) {
            cracks(painter, at, 1.5, 1.0 - m.t() / 20.0, 1);
        }
        rubble(painter, at, 0.0, m.t(), 7, 0.16, 0.24, 1);
    }

    /**
     * Cracks of light running out over the ground from {@code at}, jagged, some longer than others. They are light in
     * the ground, not a construct, so they may die down.
     */
    static void cracks(ConstructPainter painter, Vec3 at, double reach, double strength, int seed) {
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
     * Before something bursts up out of the ground: light glows up out of it, harder and faster the closer the moment
     * comes, and shafts of light shoot up out of its cracks, flickering. Light, not a construct.
     *
     * @param reach how far round {@code at} the shafts come up, in blocks
     */
    static void buildUp(ConstructPainter painter, Vec3 at, double reach, Moment m, int seed) {
        if (m.struck()) {
            return;
        }
        double warn = Mth.clamp(m.t() / LandingSlam.IMPACT_TICK, 0.0, 1.0);
        double pulse = 0.65 + 0.35 * Math.sin(m.t() * (1.0 + 2.5 * warn));
        painter.flare(at.add(0.0, 0.15, 0.0), (0.7 + 1.6 * warn) * m.size(), (0.45 + 0.55 * warn) * pulse);
        for (int k = 0; k < 9; k++) {
            double angle = Math.PI * 2.0 * (k + ConstructPainter.noise(seed, k, 31)) / 9.0;
            double out = reach * (0.2 + 0.7 * ConstructPainter.noise(seed, k, 32));
            Vec3 foot = at.add(Math.cos(angle) * out, 0.03, Math.sin(angle) * out);
            double flicker = 0.5 + 0.5 * Math.sin(m.t() * (2.0 + ConstructPainter.noise(seed, k, 33) * 2.0) + k);
            double height = (0.3 + 2.4 * warn * ConstructPainter.noise(seed, k, 34)) * m.size() * flicker;
            painter.edge(foot, foot.add(0.0, height, 0.0), 0.09 * m.size(), (0.3 + 0.7 * warn) * flicker);
        }
    }

    /**
     * Chunks of hard light thrown up out of the ground at {@code at} from tick {@code from}: they fly up and out,
     * tumbling, and fall back into the ground.
     */
    static void rubble(ConstructPainter painter, Vec3 at, double from, double t, int count, double size,
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
}
