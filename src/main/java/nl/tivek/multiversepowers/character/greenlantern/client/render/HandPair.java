package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.GLOWS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.GROUND_CUT;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.GROUND_SEAM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.PORTAL_SEAM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.drawHand;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.part;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPairLight.calls;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPairLight.flashes;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPairLight.lights;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPairLight.portal;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.AXE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.BLADE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.BLADE_SHARES;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.BLADE_STEPS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.BLADE_TOWARDS;

/**
 * A pair of hands with an axe (see {@link HandPainter} and {@link HandDuo}): both hands, each cut off at its own
 * portal, and the axe with the cracks of light in it; whole, or breaking up.
 */
final class HandPair {
    // How far round its middle a pair may reach while it is on screen, at scale 1: the axe's portal stands about 15
    // beyond it and the axe swung up over the top about 17 over it.
    private static final double PAIR_REACH = 26.0;
    // The numbers the pieces of the pair's left hand and axe start from as they break up, so no two pieces of the pair
    // fly off alike (a hand's own pieces count up to about 60 from its own start).
    private static final int LEFT_PIECES = 100;
    private static final int AXE_PIECES = 200;

    private HandPair() {
    }

    /**
     * A pair of hands with an axe: the ring's light shooting off to where its portals open, the three portals, both
     * hands each cut off at its own portal, the axe and all the light of what they do.
     */
    static void pair(LanternPainter painter, ConstructPayload hand, Vec3 facing, double clock,
            @Nullable Vec3 ring, double scale) {
        Vec3 base = hand.center();
        int variant = hand.variant();
        Vec3 aim = base.add(facing);
        HandDuo duo = HandDuo.at(base, variant, aim, clock, scale);
        if (ring != null) {
            calls(painter, duo, clock, ring);
        }
        if (!painter.visible(base, PAIR_REACH * scale)) {
            return;
        }
        int seed = hand.id() * 7;
        flashes(painter, duo, clock, 1.0);
        portal(painter, duo.rightPortal, seed, clock, 1.0);
        portal(painter, duo.leftPortal, seed + 1, clock, 1.0);
        portal(painter, duo.axePortal, seed + 2, clock, 1.0);
        if (duo.handsThere) {
            // White-hot as they push out of their portals, cooling to green, and warming up again as they pull back.
            double retract = Math.max(1.0, HandDuo.HANDS_GONE - HandDuo.RETRACT);
            painter.glare(Math.max(0.7 * (1.0 - Ease.smooth((clock - HandDuo.ARRIVES) / 16.0)),
                    0.35 * Ease.smooth((clock - HandDuo.RETRACT) / retract)));
            painter.ambient(GLOWS);
            pairHand(painter, duo.rightPortal, duo.rightPose, duo.rightPlace, false, 1.0, -1.0, 0);
            pairHand(painter, duo.leftPortal, duo.leftPose, duo.leftPlace, true, 1.0, -1.0, LEFT_PIECES);
            painter.ambient(0.0);
            painter.glare(0.0);
        }
        if (duo.axeThere) {
            ConstructPainter.Frame frame = ConstructPainter.Frame.of(duo.axeEnd, duo.axeFace, duo.axeUp, scale);
            painter.glare(axeGlare(clock, duo.axeBreak));
            painter.ambient(GLOWS);
            painter.fling(1.6);
            axe(painter, duo, base, frame, duo.axeBreak > 0.0 ? duo.axeBreak : -1.0);
            painter.fling(1.0);
            painter.ambient(0.0);
            painter.glare(0.0);
            axeCracks(painter, frame, seed, clock, duo.axeBreak);
        }
        lights(painter, seed, base, variant, aim, duo, clock, scale, 1.0);
    }

    /** One hand of the pair, cut off where it comes out of its portal (see drawHand). */
    private static void pairHand(LanternPainter painter, HandDuo.Portal portal, HandPose pose, HandPose.Place place,
            boolean left, double bright, double apart, int seed) {
        painter.clip(portal.center(), portal.normal(), PORTAL_SEAM);
        drawHand(painter, pose, place, left, bright, apart, seed, true);
        painter.noClip();
    }

    /**
     * How white-hot the axe glows: as it comes out of its portal, in a flash as it bites into the ground, and more and
     * more as the light cracks it before it breaks.
     */
    private static double axeGlare(double clock, double breaking) {
        double out = 0.6 * (1.0 - Ease.smooth((clock - HandDuo.AXE_OPENS) / 14.0));
        double bite = 0.45 * Ease.smooth((clock - HandDuo.IMPACT + 0.5) / 0.5)
                * (1.0 - Ease.smooth((clock - HandDuo.IMPACT) / 6.0));
        double cracked = 0.4 * crackGrowth(clock, 0.0, 1.0) + 0.3 * breaking;
        return Math.max(out, Math.max(bite, cracked));
    }

    /**
     * The axe, solid: cut off where it comes out of its portal while it still does, and never showing under the ground
     * (where its blade bites in, a seam of light runs round it).
     *
     * @param apart below 0 whole, else how far it has broken up into solid pieces, 0 to 1
     */
    private static void axe(LanternPainter painter, HandDuo duo, Vec3 base, ConstructPainter.Frame frame,
            double apart) {
        if (duo.axeCut) {
            painter.clip(duo.axePortal.center(), duo.axePortal.normal(), PORTAL_SEAM);
        } else {
            painter.clip(new Vec3(base.x, base.y + GROUND_CUT, base.z), Vectors.UP, GROUND_SEAM);
        }
        part(painter, AXE, frame, apart < 0.0 ? 1.0 : 1.2, apart, AXE_PIECES);
        painter.noClip();
    }

    /**
     * How far the cracks of light in the axe left in the ground have grown, 0 to 1, growing from {@code from} to
     * {@code to} of the time from a little after its blow until it breaks.
     */
    private static double crackGrowth(double clock, double from, double to) {
        double start = HandDuo.IMPACT + 2.0;
        double span = Math.max(4.0, HandDuo.AXE_BREAKS - start);
        return Ease.smooth((clock - start - from * span) / ((to - from) * span));
    }

    /**
     * Cracks of light creeping through the axe left in the ground: over both flats of its blade from its edge in
     * towards the socket, and then up its haft from the socket towards the pommel. They glow brighter as they grow,
     * flicker, and die away as it breaks up.
     */
    private static void axeCracks(LanternPainter painter, ConstructPainter.Frame frame, int seed, double clock,
            double breaking) {
        double strength = Ease.smooth((clock - HandDuo.IMPACT - 2.0) / 3.0) * (1.0 - Ease.smooth(breaking * 3.0));
        if (strength <= 0.01) {
            return;
        }
        double head = HandDuo.HEAD_AT;
        double half = HandDuo.BLADE_HALF;
        double out = HandDuo.EDGE_OUT;
        double r = HandDuo.HAFT_RADIUS;
        double width = 0.07 * frame.scale();
        double blade = crackGrowth(clock, 0.0, 0.5);
        for (int c = 0; c < 4; c++) {
            double side = c < 2 ? 1.0 : -1.0;
            double y = (Noise.of(seed, c, 1) - 0.5) * 0.9 * half;
            double z = out * 0.93;
            Vec3[] path = new Vec3[6];
            for (int s = 0; s < path.length; s++) {
                if (s > 0) {
                    y += (Noise.of(seed, c, 2 + s) - 0.5) * 0.22 * half;
                    z -= out * 0.15;
                }
                path[s] = frame.at(side * (flat(y, z) + 0.025), head + y, z);
            }
            double glow = strength * (0.6 + 0.4 * blade) * (0.85 + 0.15 * Math.sin(clock * 2.1 + c * 1.7));
            grown(painter, path, blade, width, glow);
        }
        double haft = crackGrowth(clock, 0.3, 1.0);
        double top = head - 0.6 * half - 0.1;
        for (int c = 0; c < 2; c++) {
            double angle = Math.PI * 2.0 * Noise.of(seed, 10 + c, 1);
            Vec3[] path = new Vec3[8];
            for (int s = 0; s < path.length; s++) {
                if (s > 0) {
                    angle += (Noise.of(seed, 10 + c, 2 + s) - 0.5) * 0.7;
                }
                double y = top - (top - 1.2) * s / (path.length - 1);
                path[s] = frame.at(Math.cos(angle) * (r + 0.05), y, Math.sin(angle) * (r + 0.05));
            }
            double glow = strength * (0.6 + 0.4 * haft) * (0.85 + 0.15 * Math.sin(clock * 1.9 + c * 2.3));
            grown(painter, path, haft, width, glow);
        }
    }

    /** How thick the axe's blade is either way at a point (y from the middle of its head, z out from its haft). */
    private static double flat(double y, double z) {
        for (int step = BLADE_STEPS.length - 1; step > 0; step--) {
            double[] towards = BLADE_TOWARDS[step];
            double share = BLADE_SHARES[step];
            // Inside this step's outline: the point, pushed back out from where the step is drawn in towards, lies
            // inside the whole blade's.
            if (inside(BLADE, towards[0] + (y - towards[0]) / share, towards[1] + (z - towards[1]) / share)) {
                return BLADE_STEPS[step];
            }
        }
        return BLADE_STEPS[0];
    }

    /** Whether a point lies inside an outline of pairs of numbers. */
    private static boolean inside(double[] outline, double x, double y) {
        boolean in = false;
        int n = outline.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = outline[2 * i];
            double yi = outline[2 * i + 1];
            double xj = outline[2 * j];
            double yj = outline[2 * j + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }

    /** A line of light along a path of points, drawn as far as {@code grow} (0 to 1) of it, thinning as it runs. */
    private static void grown(LanternPainter painter, Vec3[] path, double grow, double width, double strength) {
        double reach = grow * (path.length - 1);
        for (int i = 0; i + 1 < path.length && i < reach; i++) {
            Vec3 to = path[i].lerp(path[i + 1], Math.min(1.0, reach - i));
            painter.edge(path[i], to, width * (1.0 - 0.5 * i / (path.length - 1)), strength);
        }
    }

    /**
     * A pair its maker let go of: both hands, still cut off at their portals, and the axe break up (see broken), the
     * portals shrink shut, flashing as they go, and what light the pair was making dies away where it was.
     */
    static void brokenPair(LanternPainter painter, ConstructPayload hand, double clock, double since,
            double apart, double scale) {
        Vec3 base = hand.center();
        int variant = hand.variant();
        Vec3 aim = base.add(hand.facing());
        HandDuo duo = HandDuo.at(base, variant, aim, clock, scale);
        int seed = hand.id() * 7;
        double fade = 1.0 - Ease.smooth(since / 6.0);
        flashes(painter, duo, clock, fade);
        lights(painter, seed, base, variant, aim, duo, clock, scale, fade);
        double keep = 1.0 - Ease.smooth(apart * 1.4);
        double pop = Ease.smooth((apart - 0.45) / 0.25) * (1.0 - Ease.smooth((apart - 0.72) / 0.25));
        HandDuo.Portal[] portals = { duo.rightPortal, duo.leftPortal, duo.axePortal };
        for (int i = 0; i < portals.length; i++) {
            HandDuo.Portal portal = portals[i];
            portal(painter, portal, seed + i, clock + since, keep);
            if (pop > 0.01 && portal.open() > 0.01) {
                painter.flare(portal.center(), portal.radius() * Math.min(1.0, portal.open()) * 0.8 * pop, pop);
            }
        }
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 5.0));
        painter.ambient(GLOWS);
        painter.fling(1.8);
        if (duo.handsThere) {
            pairHand(painter, duo.rightPortal, duo.rightPose, duo.rightPlace, false, 1.2, apart, 0);
            pairHand(painter, duo.leftPortal, duo.leftPose, duo.leftPlace, true, 1.2, apart, LEFT_PIECES);
        }
        ConstructPainter.Frame frame = ConstructPainter.Frame.of(duo.axeEnd, duo.axeFace, duo.axeUp, scale);
        if (duo.axeThere) {
            axe(painter, duo, base, frame, apart);
        }
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
        if (duo.axeThere) {
            axeCracks(painter, frame, seed, clock, apart);
        }
    }
}
