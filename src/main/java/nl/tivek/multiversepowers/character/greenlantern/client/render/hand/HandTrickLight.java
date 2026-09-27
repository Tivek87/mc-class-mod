package nl.tivek.multiversepowers.character.greenlantern.client.render.hand;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.duo.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandLight.shockwave;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.indexTip;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.middleTip;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.thumbTip;

// The light of the flick, the pinch and the snap, and the portal a hand comes through.
final class HandTrickLight {
    private static final double TENSION = 10.0;
    private static final double RING_TICKS = 9.0;
    private static final double SNAP_REACH = 7.0;

    private HandTrickLight() {
    }

    static void portal(LanternPainter painter, int seed, HandDuo.Portal portal, int variant, double clock,
            double strength) {
        HandPairLight.portal(painter, portal, seed, clock, strength);
        HandPairLight.flashes(painter, portal, clock, 1.0, HandPose.life(variant) - 1.0, strength);
    }

    static void blows(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock, double scale,
            double strength) {
        if (strength <= 0.01) {
            return;
        }
        switch (HandPose.move(variant)) {
            case HandPose.FLICK -> flick(painter, variant, base, facing, clock, scale, strength);
            case HandPose.PINCH -> pinch(painter, variant, base, facing, clock, scale, strength);
            case HandPose.SNAP -> snap(painter, variant, base, facing, clock, scale, strength);
            case HandPose.POKE -> poke(painter, variant, base, facing, clock, scale, strength);
            case HandPose.HAMMER -> hammer(painter, variant, base, facing, clock, scale, strength);
            case HandPose.RAKE -> rake(painter, variant, base, facing, clock, scale, strength);
            case HandPose.DRAG -> drag(painter, variant, base, facing, clock, scale, strength);
            default -> {
            }
        }
    }

    // Streaks of light trail off the portal's rim while it races away with its catch.
    private static void drag(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        double speed = HandPose.dragged(clock + 0.5) - HandPose.dragged(clock - 0.5);
        double fade = strength * Math.min(1.0, speed / 0.8);
        if (fade <= 0.01) {
            return;
        }
        HandDuo.Portal portal = HandPose.portalOf(variant, base, facing, clock, scale);
        Vec3 behind = portal.normal();
        for (int k = 0; k < 10; k++) {
            double angle = Math.PI * 2.0 * k / 10.0 + clock * 0.4;
            Vec3 rim = portal.center().add(portal.a().scale(Math.cos(angle) * portal.radius()))
                    .add(portal.b().scale(Math.sin(angle) * portal.radius()));
            double length = (1.5 + 4.0 * speed) * (0.6 + 0.4 * Math.sin(k * 2.3 + clock));
            painter.edge(rim, rim.add(behind.scale(length)), 0.08 * scale, 0.8 * fade);
        }
    }

    // Every poke flashes at the fingertip with a small ring of shock; the last a big one.
    private static void poke(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        for (int k = 0; k < HandPose.POKE_HITS.length; k++) {
            double since = clock - HandPose.POKE_HITS[k];
            if (since < 0.0 || since > 6.0) {
                continue;
            }
            boolean last = k == HandPose.POKE_HITS.length - 1;
            HandPose.Place struck = HandPose.at(variant, HandPose.POKE_HITS[k], 0.0).place(base, facing, scale);
            Vec3 tip = struck.at(HandPose.POKE_POINT);
            Vec3 way = struck.up();
            Vec3[] across = Vectors.across(way);
            double u = since / 6.0;
            double fade = strength * (1.0 - u) * (1.0 - u);
            double size = last ? 2.0 : 0.9;
            painter.flare(tip, size * scale * (1.0 - u), fade);
            painter.circle(tip.add(way.scale(size * u * scale)), across[0], across[1], (0.3 + size * u) * scale,
                    0.05, 0.4, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
        }
    }

    // The fist glows as it draws up; the blow flashes and sends rings and cracks of light out over the ground.
    private static void hammer(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        double since = clock - HandPose.HAMMER_HITS;
        if (since < -TENSION || since > 20.0) {
            return;
        }
        HandPose pose = HandPose.at(variant, Math.min(clock, HandPose.HAMMER_HITS), 0.0);
        Vec3 fist = pose.place(base, facing, scale).at(HandPose.HAMMER_POINT);
        if (since < 0.0) {
            double gather = Ease.smooth((since + TENSION) / TENSION);
            painter.flare(fist, (0.5 + 1.2 * gather) * scale, 0.6 * gather * strength);
            return;
        }
        shockwave(painter, fist, since, 5.5 * scale, 4, strength);
        HandLight.cracks(painter, variant * 7 + 3, fist.add(0.0, 0.06, 0.0), 10, 2.6 * scale,
                Ease.smooth(since / 4.0), strength * (1.0 - Ease.smooth((since - 8.0) / 12.0)), 0.08 * scale);
    }

    // Three glowing furrows the claws leave in the ground, burning out slowly.
    private static void rake(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        double since = clock - HandPose.RAKE_HITS;
        if (since < -2.0 || since > 40.0) {
            return;
        }
        double fade = strength * (1.0 - Ease.smooth((since - 10.0) / 30.0));
        double until = Math.min(clock, HandPose.RAKE_HITS + 3.0);
        for (double x : new double[] { -0.8, 0.0, 0.8 }) {
            Vec3 last = null;
            for (double t = HandPose.RAKE_HITS - 2.0; t <= until; t += 0.5) {
                Vec3 claw = HandPose.at(variant, t, 0.0).place(base, facing, scale)
                        .at(new Vec3(x, HandPose.RAKE_POINT.y, HandPose.RAKE_POINT.z));
                Vec3 at = new Vec3(claw.x, base.y + 0.05, claw.z);
                if (last != null && claw.y < base.y + 1.2 * scale) {
                    painter.edge(last, at, 0.12 * scale, fade);
                }
                last = at;
            }
        }
    }

    // The nail glows as the finger strains against the thumb; the flick leaves a smear of light and a ring of shock
    // round the way the creature flies.
    private static void flick(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        double since = clock - HandPose.FLICK_HITS;
        double strain = Ease.smooth((since + TENSION) / TENSION) * (1.0 - Ease.smooth(since / 1.0));
        if (strain > 0.01) {
            HandPose pose = HandPose.at(variant, clock, 0.0);
            Vec3 nail = middleTip(pose, pose.place(base, facing, scale));
            painter.flare(nail, (0.4 + 0.9 * strain) * scale, 0.7 * strain * strength);
        }
        if (since > -1.5 && since < 3.0) {
            double fade = strength * (1.0 - Ease.smooth(since / 3.0)) * Ease.smooth((since + 1.5) / 1.0);
            smear(painter, variant, base, facing, clock, scale, fade);
        }
        if (since >= 0.0 && since < RING_TICKS) {
            HandPose.Place struck = HandPose.at(variant, HandPose.FLICK_HITS, 0.0).place(base, facing, scale);
            Vec3 at = struck.at(HandPose.FLICK_POINT);
            Vec3 way = struck.arm();
            Vec3[] across = Vectors.across(way);
            double u = since / RING_TICKS;
            double fade = strength * (1.0 - u) * (1.0 - u);
            for (int k = 0; k < 2; k++) {
                double grow = Ease.smooth(Math.min(1.0, (since - 1.5 * k) / 5.0));
                if (grow <= 0.0) {
                    continue;
                }
                painter.circle(at.add(way.scale((1.0 + 3.0 * grow + 1.2 * k) * scale)), across[0], across[1],
                        (0.6 + 1.8 * grow) * scale, 0.06, 0.5, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
            }
            if (since < 3.0) {
                painter.flare(at, 2.5 * scale * (1.0 - since / 3.0), fade);
            }
        }
    }

    private static void smear(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double fade) {
        if (fade <= 0.01) {
            return;
        }
        Vec3 lastTip = null;
        Vec3 lastRoot = null;
        for (int k = 0; k <= 5; k++) {
            double t = clock - k * 0.4;
            HandPose pose = HandPose.at(variant, t, 0.0);
            HandPose.Place place = pose.place(base, facing, scale);
            Vec3 tip = middleTip(pose, place);
            Vec3 root = place.at(new Vec3(-0.38, 3.6, 0.3));
            if (lastTip != null) {
                double a = fade * (1.0 - (k - 1) / 5.0) * 0.6;
                double b = fade * (1.0 - k / 5.0) * 0.6;
                painter.sheet(lastRoot, lastTip, tip, root, a, a, b, b);
            }
            lastTip = tip;
            lastRoot = root;
        }
    }

    // A spark where the fingertips close on the creature, and again where they let go.
    private static void pinch(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        for (int moment : new int[] { HandPose.PINCH_CATCHES, HandPose.PINCH_DROPS }) {
            double since = clock - moment;
            if (since < -0.5 || since > 4.0) {
                continue;
            }
            HandPose pose = HandPose.at(variant, clock, 0.0);
            HandPose.Place place = pose.place(base, facing, scale);
            Vec3 between = indexTip(pose, place, false).lerp(thumbTip(pose, place, false), 0.5);
            double fade = strength * (1.0 - Ease.smooth(since / 4.0)) * Ease.smooth((since + 0.5) / 0.5);
            painter.flare(between, 1.4 * scale * fade, fade);
            Vec3[] across = Vectors.across(place.up());
            painter.circle(between, across[0], across[1], (0.3 + 1.2 * Ease.smooth(since / 4.0)) * scale, 0.05,
                    0.35, Colors.alpha(0.8 * fade), Colors.alpha(0.4 * fade));
        }
    }

    // Light gathers where thumb and finger press; the snap flashes and sends rings out every way and over the ground.
    private static void snap(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        double since = clock - HandPose.SNAP_HITS;
        if (since < -TENSION || since > 16.0) {
            return;
        }
        HandPose pose = HandPose.at(variant, Math.min(clock, HandPose.SNAP_HITS), 0.0);
        HandPose.Place place = pose.place(base, facing, scale);
        Vec3 fingers = middleTip(pose, place).lerp(thumbTip(pose, place, false), 0.5);
        if (since < 0.0) {
            double gather = Ease.smooth((since + TENSION) / TENSION);
            painter.flare(fingers, (0.3 + 1.0 * gather) * scale, 0.8 * gather * strength);
            return;
        }
        shockwave(painter, new Vec3(fingers.x, base.y, fingers.z), since, SNAP_REACH * scale, 3, strength);
        double u = Math.min(1.0, since / RING_TICKS);
        double fade = strength * (1.0 - u) * (1.0 - u);
        if (fade <= 0.01) {
            return;
        }
        double radius = (0.5 + SNAP_REACH * (1.0 - (1.0 - u) * (1.0 - u))) * scale;
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        painter.circle(fingers, east, Vectors.UP, radius, 0.07, 0.6, Colors.alpha(0.8 * fade),
                Colors.alpha(0.4 * fade));
        painter.circle(fingers, south, Vectors.UP, radius, 0.07, 0.6, Colors.alpha(0.8 * fade),
                Colors.alpha(0.4 * fade));
        painter.circle(fingers, east, south, radius, 0.09, 0.7, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
        if (since < 3.0) {
            painter.flare(fingers, 4.0 * scale * (1.0 - since / 3.0), strength * (1.0 - since / 3.0));
        }
    }
}
