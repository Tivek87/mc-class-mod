package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.middleTip;

/**
 * The light round one of the Giant Hands (see {@link HandPainter}): where it comes up out of the ground, and the
 * light its blows leave.
 */
final class HandLight {
    private HandLight() {
    }

    /**
     * Where it comes up: a ring of light on the ground in a low haze of green light, and cracks running out from it as
     * it bursts out, fading once it is out, and glowing faintly again as it sinks back in.
     */
    static void ground(LanternPainter painter, int id, Vec3 base, double clock, int variant, double scale,
            double strength) {
        double arrives = HandPose.ARRIVES;
        int life = HandPose.life(variant);
        double burst = strength * Ease.smooth((clock - arrives * 0.25) / (arrives * 0.75))
                * (1.0 - Ease.smooth((clock - arrives - 12.0) / 18.0));
        double sink = strength * Ease.smooth((clock - HandPose.sinks(variant)) / 6.0)
                * (1.0 - Ease.smooth((clock - life + 3.0) / 3.0));
        double glow = Math.max(burst, 0.6 * sink);
        if (glow <= 0.01) {
            return;
        }
        Vec3 at = base.add(0.0, 0.06, 0.0);
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        double wide = (1.6 + 1.5 * Ease.smooth(clock / (HandPose.ARRIVES + 4.0))) * scale;
        painter.circle(at, east, south, wide, 0.12, 1.1, Colors.alpha(0.9 * glow), Colors.alpha(0.45 * glow));
        painter.circle(at, east, south, wide * 0.6, 0.08, 0.7, Colors.alpha(0.6 * glow), Colors.alpha(0.3 * glow));
        if (burst > 0.01) {
            painter.flare(base.add(0.0, 0.4, 0.0), (1.5 + 2.5 * burst) * scale, burst);
            // Its light glows in the dust it throws up.
            painter.haze(base.add(0.0, 0.7 * scale, 0.0), east.scale(3.2 * scale), Vectors.UP.scale(2.4 * scale),
                    south.scale(3.2 * scale), painter.material().glow(), 0.35 * burst);
            // The ground cracks open round it.
            cracks(painter, id, at, 10, 2.2 * scale, Ease.smooth((clock - HandPose.ARRIVES + 1.5) / 6.0), burst,
                    0.08 * scale);
        }
    }

    /** Cracks of light running out over the ground from {@code at}, each 1 to 2 times {@code length} long, grown. */
    static void cracks(LanternPainter painter, int seed, Vec3 at, int count, double length, double grow,
            double strength, double width) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * (i + 0.4 * Noise.of(seed, i, 1)) / count;
            crack(painter, seed, i, at, angle, length * (1.0 + Noise.of(seed, i, 2)), grow, strength, width);
        }
    }

    /** One crack of light running out over the ground from {@code at} the way {@code angle} points, in three bends. */
    static void crack(LanternPainter painter, int seed, int i, Vec3 at, double angle, double length,
            double grow, double strength, double width) {
        Vec3 from = at;
        for (int s = 1; s <= 3; s++) {
            double bend = angle + (Noise.of(seed, i, 3 + s) - 0.5) * 0.6;
            double out = length * grow * s / 3.0;
            Vec3 to = at.add(Math.cos(bend) * out, 0.0, Math.sin(bend) * out);
            painter.edge(from, to, width * (1.2 - 0.25 * s), strength);
            from = to;
        }
    }

    /**
     * The light its blows leave: a streak through the air behind a swatting or throwing palm, a streak behind a middle
     * finger shooting up, and rings running out over the ground where a palm or a fist lands or a middle finger bursts
     * out.
     */
    static void blows(LanternPainter painter, ConstructPayload hand, Vec3 facing, double clock, double scale,
            double strength) {
        Vec3 base = hand.center();
        int move = HandPose.move(hand.variant());
        double reach = Math.sqrt(facing.x * facing.x + facing.z * facing.z) / scale;
        double swing = HandPose.SWING_TICKS;
        int blow = move == HandPose.SMACK ? HandPose.SMACK_HITS : move == HandPose.GRAB ? HandPose.GRAB_THROWS : -1;
        if (blow >= 0 && clock > blow - swing * 0.5 && clock < blow + swing * 0.75) {
            // Fading in as the swing starts and out after the blow, never popping in or out.
            streak(painter, hand.variant(), base, facing, reach, clock, scale,
                    strength * (1.0 - Ease.smooth((clock - blow) / (swing * 0.6)))
                            * Ease.smooth((clock - (blow - swing * 0.5)) / 2.0));
        }
        double slammed = clock - HandPose.SLAM_HITS;
        if (move == HandPose.SLAM && slammed >= 0.0 && slammed <= shockwaveTicks(3) && strength > 0.01) {
            // Only while its rings run out (see shockwave): where it landed is worked out for nothing else.
            HandPose.Place land = HandPose.at(hand.variant(), HandPose.SLAM_HITS, reach).place(base, facing, scale);
            Vec3 palm = land.at(HandPose.PALM);
            shockwave(painter, new Vec3(palm.x, base.y, palm.z), clock - HandPose.SLAM_HITS, 4.5 * scale, 3,
                    strength);
        }
        if (move == HandPose.FINGER) {
            // Bursting out it throws a wave of light out over the ground, wider than any blow.
            shockwave(painter, base, clock - HandPose.FINGER_BURSTS, 6.5 * scale, 4, strength);
            fingerStreak(painter, hand.variant(), base, facing, reach, clock, scale, strength);
        }
        if (move == HandPose.POUND) {
            for (int hit : HandPose.POUND_HITS) {
                double since = clock - hit;
                if (since >= 0.0 && since <= shockwaveTicks(2)) {
                    Vec3 fist = HandPose.at(hand.variant(), hit, reach).place(base, facing, scale).at(HandPose.FIST);
                    shockwave(painter, new Vec3(fist.x, base.y, fist.z), since, 3.2 * scale, 2, strength);
                }
            }
        }
    }

    /** A streak of light behind a palm swinging through the air: from its wrist to its fingertips, over the last ticks. */
    private static void streak(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double reach, double clock,
            double scale, double strength) {
        if (strength <= 0.01) {
            return;
        }
        Vec3 lastIn = null;
        Vec3 lastOut = null;
        for (int k = 0; k <= 5; k++) {
            double t = clock - k * HandPose.SWING_TICKS * 0.15;
            HandPose.Place place = HandPose.at(variant, t, reach).place(base, facing, scale);
            Vec3 in = place.at(new Vec3(0.0, 1.2, 0.0));
            Vec3 out = place.at(new Vec3(0.0, 5.6, 0.0));
            if (lastIn != null) {
                double a = strength * (1.0 - (k - 1) / 5.0) * 0.55;
                double b = strength * (1.0 - k / 5.0) * 0.55;
                painter.sheet(lastIn, lastOut, out, in, a, a, b, b);
            }
            lastIn = in;
            lastOut = out;
        }
    }

    /**
     * The streak of light a middle finger leaves as it shoots up out of the ground: a ribbon of light along the path of
     * its tip over the last few ticks, bright along its middle and soft at its sides, with a bright line down it.
     */
    private static void fingerStreak(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double reach,
            double clock, double scale, double fade) {
        double bursts = HandPose.FINGER_BURSTS;
        double strength = fade * Ease.smooth((clock - bursts + 1.5) / 1.5)
                * (1.0 - Ease.smooth((clock - bursts - 3.0) / 7.0));
        if (strength <= 0.01) {
            return;
        }
        int count = 8;
        Vec3[] tips = new Vec3[count];
        for (int k = 0; k < count; k++) {
            HandPose pose = HandPose.at(variant, clock - k * 0.55, reach);
            Vec3 tip = middleTip(pose, pose.place(base, facing, scale));
            tips[k] = new Vec3(tip.x, Math.max(tip.y, base.y + 0.05), tip.z);
        }
        Vec3[] sides = new Vec3[count];
        double[] alphas = new double[count];
        for (int k = 0; k < count; k++) {
            Vec3 along = tips[Math.max(0, k - 1)].subtract(tips[Math.min(count - 1, k + 1)]);
            Vec3 side = along.cross(painter.camera().subtract(tips[k]));
            double f = (double) k / (count - 1);
            sides[k] = side.lengthSqr() < 1.0E-8 ? Vec3.ZERO : side.normalize().scale(0.9 * scale * (1.0 - 0.7 * f));
            alphas[k] = strength * (1.0 - f);
        }
        for (int k = 0; k + 1 < count; k++) {
            Vec3 a = tips[k];
            Vec3 b = tips[k + 1];
            if (a.distanceToSqr(b) < 1.0E-6) {
                continue;
            }
            painter.sheet(a, b, b.add(sides[k + 1]), a.add(sides[k]), alphas[k], alphas[k + 1], 0.0, 0.0);
            painter.sheet(a, b, b.subtract(sides[k + 1]), a.subtract(sides[k]), alphas[k], alphas[k + 1], 0.0, 0.0);
            painter.edge(a, b, 0.14 * scale * (1.0 - 0.6 * k / (count - 1.0)), 0.8 * alphas[k]);
        }
    }

    /** How long a blow's rings of light run out over the ground (see shockwave), in ticks: until the last is out. */
    private static double shockwaveTicks(int rings) {
        return 12.0 + (rings - 1) * 2.5;
    }

    /** Rings of light running out over the ground from where a blow landed, and a flash, {@code strength} strong. */
    static void shockwave(LanternPainter painter, Vec3 at, double since, double reach, int rings,
            double strength) {
        if (since < 0.0 || since > shockwaveTicks(rings) || strength <= 0.01) {
            return;
        }
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        Vec3 ground = at.add(0.0, 0.08, 0.0);
        for (int k = 0; k < rings; k++) {
            double ring = since - k * 2.5;
            if (ring < 0.0) {
                continue;
            }
            double wave = 1.0 - Math.pow(1.0 - Math.min(1.0, ring / 9.0), 2.0);
            double fade = strength * Math.max(0.0, 1.0 - ring / 12.0) * Ease.smooth(ring / 0.6);
            painter.circle(ground, east, south, 0.5 + reach * wave, 0.1, 0.8, Colors.alpha(fade),
                    Colors.alpha(0.5 * fade));
        }
        if (since < 4.0) {
            painter.flare(ground.add(0.0, 0.4, 0.0), reach * (1.0 - since / 4.0),
                    strength * (1.0 - since / 4.0) * Ease.smooth(since / 0.5));
        }
    }
}
