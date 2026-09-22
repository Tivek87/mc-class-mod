package nl.tivek.welcomescreen.client.character.lantern;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.character.lantern.LandingSlam;
import nl.tivek.welcomescreen.network.ConstructPayload;

/**
 * The constructs of a landing slam (see {@link LandingSlam}), drawn with the same hard light as every construct
 * (see {@link ConstructPainter}). Each one takes shape in front of Green Lantern, fed by a beam from his ring,
 * strikes at {@link LandingSlam#IMPACT_TICK} and then breaks up into light, while the shockwave runs out over the
 * ground as a ring of light:
 * <ul>
 * <li>a giant fist that drops out of the sky knuckles first;</li>
 * <li>two giant open hands that clap together, or two fists that bump knuckles;</li>
 * <li>a war hammer that drops head first and stays planted;</li>
 * <li>the lantern emblem, standing up and then falling flat on its face away from him;</li>
 * <li>an anvil that drops;</li>
 * <li>two cymbals that crash together and ring.</li>
 * </ul>
 */
final class SlamPainter {
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);
    // How far the falling ones drop from, in blocks.
    private static final double DROP = 7.0;
    // The tick it has broken up completely.
    private static final double GONE_TICK = 20.0;
    // How long the shockwave takes to run out, and to fade away, in ticks.
    private static final double WAVE_TICKS = 12.0;
    private static final double WAVE_FADE = 16.0;
    // The width of the fist model at scale 1, in blocks (see ConstructPainter).
    private static final double FIST_WIDTH = 1.23;
    // How far the knuckles of the fist model reach in front of its middle, at scale 1.
    private static final double FIST_REACH = 0.57;

    /** An open right hand, fingers up, palm facing -x, thumb towards +z; as ConstructPainter's boxes. */
    private static final double[][] OPEN_HAND = {
            // The palm, and the heel of the hand a little thicker below it
            { -0.13, -0.50, -0.42, 0.13, 0.32, 0.42, 1.0 },
            { -0.16, -0.62, -0.38, 0.15, -0.40, 0.38, 0.95 },
            // The four fingers, from the index finger (next to the thumb) to the little finger, in two bones each
            { -0.10, 0.32, 0.21, 0.10, 0.80, 0.40, 1.0 },
            { -0.09, 0.80, 0.22, 0.09, 1.12, 0.39, 1.05 },
            { -0.10, 0.32, -0.01, 0.10, 0.86, 0.18, 1.0 },
            { -0.09, 0.86, 0.00, 0.09, 1.22, 0.17, 1.05 },
            { -0.10, 0.32, -0.21, 0.10, 0.82, -0.04, 1.0 },
            { -0.09, 0.82, -0.20, 0.09, 1.15, -0.05, 1.05 },
            { -0.10, 0.32, -0.40, 0.10, 0.70, -0.24, 1.0 },
            { -0.09, 0.70, -0.39, 0.09, 0.96, -0.25, 1.05 },
            // The thumb, out of the side of the palm
            { -0.12, -0.30, 0.42, 0.10, 0.02, 0.62, 1.0 },
            { -0.10, 0.02, 0.50, 0.08, 0.36, 0.68, 1.05 },
            // The wrist, a bright cuff, and the forearm fading out
            { -0.14, -0.86, -0.32, 0.14, -0.62, 0.32, 0.9 },
            { -0.18, -0.98, -0.38, 0.18, -0.86, 0.38, 1.2 },
            { -0.13, -1.40, -0.30, 0.13, -0.98, 0.30, 0.6 } };
    /** A war hammer standing on its head: the head along x at the bottom, the handle up along y. */
    private static final double[][] HAMMER = {
            { -0.95, -0.62, -0.46, 0.95, 0.00, 0.46, 1.0 },
            { -1.05, -0.68, -0.52, -0.85, 0.06, 0.52, 1.2 },
            { 0.85, -0.68, -0.52, 1.05, 0.06, 0.52, 1.2 },
            { -0.12, -0.66, -0.50, 0.12, 0.04, 0.50, 1.15 },
            { -0.11, 0.00, -0.11, 0.11, 2.35, 0.11, 0.95 },
            { -0.15, 1.55, -0.15, 0.15, 2.25, 0.15, 1.1 },
            { -0.19, 2.35, -0.19, 0.19, 2.60, 0.19, 1.2 } };
    // Where the head of the hammer ends below its middle.
    private static final double HAMMER_FOOT = 0.68;
    /** An anvil standing on its base, the horn towards +x. */
    private static final double[][] ANVIL = {
            { -0.78, 0.00, -0.52, 0.78, 0.24, 0.52, 0.95 },
            { -0.56, 0.24, -0.38, 0.56, 0.40, 0.38, 1.0 },
            { -0.30, 0.40, -0.22, 0.30, 0.86, 0.22, 0.9 },
            { -0.82, 0.86, -0.36, 0.72, 1.22, 0.36, 1.05 },
            { -1.02, 0.94, -0.26, -0.82, 1.22, 0.26, 1.0 },
            { 0.72, 0.92, -0.26, 1.06, 1.22, 0.26, 1.0 },
            { 1.06, 0.99, -0.16, 1.32, 1.20, 0.16, 1.05 },
            { 1.32, 1.04, -0.08, 1.52, 1.16, 0.08, 1.1 } };
    /** The two bars of the lantern emblem, above and below its ring, in the plane of x and y. */
    private static final double[][] EMBLEM_BARS = {
            { -0.95, 1.12, -0.12, 0.95, 1.40, 0.12, 1.1 },
            { -0.95, -1.40, -0.12, 0.95, -1.12, 0.12, 1.1 } };
    // The emblem's ring: how many pieces, and where it runs between, at scale 1; and how tall it all is.
    private static final int EMBLEM_PIECES = 16;
    private static final double EMBLEM_INNER = 0.66;
    private static final double EMBLEM_OUTER = 1.0;
    private static final double EMBLEM_HALF = 1.4;

    private SlamPainter() {
    }

    /**
     * One landing slam.
     *
     * @param slam what the server said about it: where it strikes, the way its maker faced, how far the wave goes
     *             and which construct it is
     * @param t    ticks since he landed, by the client's own clock
     * @param ring where its maker's ring is, or null when he is out of sight
     */
    static void draw(ConstructPainter painter, ConstructPayload slam, double t, @Nullable Vec3 ring) {
        Vec3 ground = slam.center();
        Vec3 forward = new Vec3(slam.facing().x, 0.0, slam.facing().z);
        forward = forward.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
        Vec3 right = forward.cross(ConstructPainter.UP).normalize();
        // It takes shape, strikes, holds a moment, and breaks up: swelling a little as its light scatters.
        double form = ConstructPainter.smooth(t / LandingSlam.FORM_TICKS);
        double burst = Mth.clamp((t - LandingSlam.BURST_TICK) / (GONE_TICK - LandingSlam.BURST_TICK), 0.0, 1.0);
        double solid = form * (1.0 - burst);
        double swell = 1.0 + 0.25 * burst;
        // How far it has come on its way down or in: slowly at first, then faster and faster, like a real fall.
        double go = Mth.clamp((t - LandingSlam.FORM_TICKS) / (LandingSlam.IMPACT_TICK - LandingSlam.FORM_TICKS),
                0.0, 1.0);
        double fall = go * go;
        boolean struck = t >= LandingSlam.IMPACT_TICK;
        double flash = struck ? Mth.clamp(1.0 - (t - LandingSlam.IMPACT_TICK) / 4.0, 0.0, 1.0) : 0.0;
        double bright = 1.0 + 0.5 * flash;
        // Once it strikes it sinks a hair into the ground.
        double sunk = struck ? 0.15 : 0.0;
        Vec3 anchor;
        double scale;
        if (solid > 0.0) {
            switch (slam.variant()) {
                case ConstructPayload.SLAM_HANDS -> {
                    scale = 1.45 * swell;
                    Vec3 clap = ground.add(0.0, 1.9, 0.0);
                    double apart = 0.14 * scale + 3.2 * (1.0 - fall) + vibrate(t, 0.06);
                    Vec3 back = forward.scale(-1.0);
                    painter.model(OPEN_HAND, new ConstructPainter.Frame(clap.add(right.scale(apart)), right,
                            ConstructPainter.UP, back, scale), solid, bright);
                    painter.model(OPEN_HAND, new ConstructPainter.Frame(clap.subtract(right.scale(apart)),
                            right.scale(-1.0), ConstructPainter.UP, back, scale), solid, bright);
                    anchor = clap;
                }
                case ConstructPayload.SLAM_FISTS -> {
                    scale = 2.2 / FIST_WIDTH * swell;
                    Vec3 meet = ground.add(0.0, 1.7, 0.0);
                    double apart = FIST_REACH * scale + 3.0 * (1.0 - fall) + vibrate(t, 0.05);
                    Vec3 in = right.scale(-1.0);
                    painter.model(ConstructPainter.fistModel(), new ConstructPainter.Frame(
                            meet.add(right.scale(apart)), in.cross(ConstructPainter.UP), ConstructPainter.UP, in,
                            scale), solid, bright);
                    // The left fist: the same fist, mirrored, so its thumb is on the other side.
                    painter.model(ConstructPainter.fistModel(), new ConstructPainter.Frame(
                            meet.subtract(right.scale(apart)), right.cross(ConstructPainter.UP).scale(-1.0),
                            ConstructPainter.UP, right, scale), solid, bright);
                    anchor = meet;
                }
                case ConstructPayload.SLAM_HAMMER -> {
                    scale = 1.5 * swell;
                    double height = DROP * (1.0 - fall) - sunk;
                    ConstructPainter.Frame frame = new ConstructPainter.Frame(
                            ground.add(0.0, HAMMER_FOOT * scale + height, 0.0), right, ConstructPainter.UP, forward,
                            scale);
                    painter.model(HAMMER, frame, solid, bright);
                    anchor = frame.at(0.0, 2.6, 0.0);
                }
                case ConstructPayload.SLAM_EMBLEM -> {
                    scale = LandingSlam.EMBLEM_HALF / EMBLEM_HALF * swell;
                    anchor = emblem(painter, ground, forward, right, scale, fall, solid, bright);
                }
                case ConstructPayload.SLAM_ANVIL -> {
                    scale = 1.9 * swell;
                    double height = (DROP + 2.0) * (1.0 - fall) - sunk;
                    ConstructPainter.Frame frame = new ConstructPainter.Frame(ground.add(0.0, height, 0.0), right,
                            ConstructPainter.UP, forward, scale);
                    painter.model(ANVIL, frame, solid, bright);
                    anchor = frame.at(0.0, 1.22, 0.0);
                }
                case ConstructPayload.SLAM_CYMBALS -> {
                    scale = 1.9 * swell;
                    Vec3 meet = ground.add(0.0, 1.9, 0.0);
                    double apart = 0.08 + 3.2 * (1.0 - fall) + vibrate(t, 0.12);
                    cymbal(painter, meet.add(right.scale(apart)), right.scale(-1.0), scale, solid, flash);
                    cymbal(painter, meet.subtract(right.scale(apart)), right, scale, solid, flash);
                    anchor = meet;
                }
                default -> {
                    // The fist from the sky: knuckles down, the back of the hand towards him.
                    scale = 3.3 / FIST_WIDTH * swell;
                    double height = DROP * (1.0 - fall) - sunk;
                    Vec3 towards = forward.scale(-1.0);
                    ConstructPainter.Frame frame = new ConstructPainter.Frame(
                            ground.add(0.0, FIST_REACH * scale + height, 0.0), DOWN.cross(towards), towards, DOWN,
                            scale);
                    painter.model(ConstructPainter.fistModel(), frame, solid, bright);
                    anchor = frame.at(0.0, 0.0, -1.25);
                }
            }
            // The ring feeds it while it takes shape and comes down, and lets go once it has struck.
            if (ring != null && t < LandingSlam.IMPACT_TICK + 2.0) {
                painter.beam(ring, anchor, solid * (struck ? 0.5 : 1.0), Math.min(scale, 3.0));
            }
        }
        if (struck) {
            wave(painter, ground, forward, right, slam.size(), t - LandingSlam.IMPACT_TICK);
        }
    }

    /** After the strike the halves still ring against each other a moment: a small shiver that dies away. */
    private static double vibrate(double t, double size) {
        double since = t - LandingSlam.IMPACT_TICK;
        if (since <= 0.0) {
            return 0.0;
        }
        return size * Math.abs(Math.sin(since * 2.6)) * Math.max(0.0, 1.0 - since / 10.0);
    }

    /**
     * The lantern emblem: a ring with a bar above and below it. It stands up on its bottom edge, then falls flat on
     * its face away from him, so it lands with its middle on the middle of the wave.
     *
     * @return where the ring's beam feeds it
     */
    private static Vec3 emblem(ConstructPainter painter, Vec3 ground, Vec3 forward, Vec3 right, double scale,
            double fall, double solid, double bright) {
        double half = EMBLEM_HALF * scale;
        Vec3 edge = ground.subtract(forward.scale(half));
        double angle = Mth.HALF_PI * fall;
        // Its own up and the way its face looks, as it tips over forwards.
        Vec3 up = ConstructPainter.UP.scale(Math.cos(angle)).add(forward.scale(Math.sin(angle)));
        Vec3 face = forward.scale(-Math.cos(angle)).add(ConstructPainter.UP.scale(Math.sin(angle)));
        Vec3 middle = edge.add(up.scale(half));
        painter.model(EMBLEM_BARS, new ConstructPainter.Frame(middle, right, up, face, scale), solid, bright);
        double radius = (EMBLEM_INNER + EMBLEM_OUTER) * 0.5;
        double length = radius * Math.tan(Math.PI / EMBLEM_PIECES) + 0.03;
        double[][] piece = { { -length, -(EMBLEM_OUTER - EMBLEM_INNER) * 0.5, -0.12, length,
                (EMBLEM_OUTER - EMBLEM_INNER) * 0.5, 0.12, 1.05 } };
        for (int i = 0; i < EMBLEM_PIECES; i++) {
            double a = Math.PI * 2.0 * i / EMBLEM_PIECES;
            Vec3 out = right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            Vec3 along = right.scale(-Math.sin(a)).add(up.scale(Math.cos(a)));
            painter.model(piece, new ConstructPainter.Frame(middle.add(out.scale(radius * scale)), along, out, face,
                    scale), solid, bright);
        }
        return middle;
    }

    /**
     * A cymbal: a thin dish of hard light with a bell in its middle, the hollow side facing {@code toward} (the
     * other cymbal), grooves round it and a bright rim.
     */
    private static void cymbal(ConstructPainter painter, Vec3 center, Vec3 toward, double radius, double solid,
            double flash) {
        Vec3 a = Math.abs(toward.y) < 0.9 ? toward.cross(ConstructPainter.UP).normalize()
                : toward.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
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
        double body = Math.min(1.0, solid * 0.9);
        for (int i = 0; i < sides; i++) {
            int j = (i + 1) % sides;
            double light = ConstructPainter.lit(rim[i], rim[j], base[j], center.add(toward));
            painter.side(rim[i], rim[j], base[j], base[i],
                    ConstructPainter.shade(ConstructPainter.MASS_GREEN, Math.min(1.0, light * (1.0 + 0.3 * flash))),
                    body);
            painter.side(base[i], base[j], top, top,
                    ConstructPainter.shade(ConstructPainter.MASS_GREEN, Math.min(1.0, light * 1.1)), body);
            painter.edge(rim[i], rim[j], 0.06, solid * (1.0 + flash));
            painter.edge(base[i], base[j], 0.035, solid * 0.7);
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
                    painter.edge(last, point, 0.02, solid * 0.45);
                }
                last = point;
            }
        }
    }

    /**
     * The shockwave: a ring of light racing out over the ground to the edge of its reach, with a fainter one behind
     * it, and a flash where it struck.
     *
     * @param since ticks since the strike
     */
    private static void wave(ConstructPainter painter, Vec3 ground, Vec3 forward, Vec3 right, double reach,
            double since) {
        if (since >= WAVE_FADE) {
            return;
        }
        double out = 1.0 - Math.pow(1.0 - Mth.clamp(since / WAVE_TICKS, 0.0, 1.0), 3.0);
        double fade = Math.pow(1.0 - since / WAVE_FADE, 1.5);
        Vec3 at = ground.add(0.0, 0.08, 0.0);
        painter.circle(at, right, forward, Math.max(0.1, reach * out), 0.12 + 0.12 * fade, 0.9,
                ConstructPainter.alpha(fade), ConstructPainter.alpha(0.6 * fade));
        painter.circle(at, right, forward, Math.max(0.1, reach * out * 0.7), 0.06, 0.5,
                ConstructPainter.alpha(0.5 * fade), ConstructPainter.alpha(0.3 * fade));
        if (since < 5.0) {
            double burst = 1.0 - since / 5.0;
            painter.flare(ground.add(0.0, 0.6, 0.0), 0.5 + 1.6 * burst, burst);
        }
    }
}
