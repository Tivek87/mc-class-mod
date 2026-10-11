package nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.handFrame;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.ringGem;

// The baseball bat and the hourglass, both solid hard light, and the light of all four: the bat's swing and its flip
// into the air, the sand running out and the clock of stopped time on the ground, the field that falls up, and the
// black hole (HandHoleLight).
public final class HandWonderLight {
    private static final int WHITE = 0xF2FFF4;
    private static final int LIGHT = 0x5CFF7A;
    private static final int PALE = 0xB8FFC8;
    private static final Vec3 HOLD = HandPose.BAT_HOLD;
    private static final Vec3 FOOT = HandPose.GLASS_FOOT;
    private static final double MIDDLE = HandPose.GLASS_TALL * 0.5;
    private static final ConstructPainter.Shape BAT = ConstructPainter.Shape.of(
            Mesh.lathe(16, 1.0, 0.0, -1.5, 0.46, -1.48, 0.5, -1.32, 0.42, -1.18, 0.28, -1.05, 0.28, 0.9, 0.33, 2.0,
                    0.45, 3.2, 0.55, 4.3, 0.58, 5.4, 0.56, 6.0, 0.44, 6.28, 0.0, 6.36)
                    .pointing(-1.0, 0.0, 0.0).moved(HOLD.x, HOLD.y, HOLD.z),
            Mesh.torus(14, 5, 0.3, 0.07, 1.8).alongX().moved(HOLD.x - 1.6, HOLD.y, HOLD.z),
            Mesh.torus(16, 5, 0.57, 0.06, 1.8).alongX().moved(HOLD.x - 4.9, HOLD.y, HOLD.z));
    private static final ConstructPainter.Shape GLASS = ConstructPainter.Shape.of(glass());
    private static final double BAT_MIDDLE = 2.4;
    private static final double TIME_RADIUS = 9.0;
    private static final double GRAVITY_RADIUS = 5.5;
    private static final int RISING = 28;
    private static final int ROUND = 20;

    private HandWonderLight() {
    }

    private static Mesh[] glass() {
        List<Mesh> parts = new ArrayList<>();
        double top = HandPose.GLASS_TALL;
        parts.add(Mesh.cylinder(18, 0.88, 0.0, 0.16, 1.6));
        parts.add(Mesh.cylinder(18, 0.88, top - 0.16, top, 1.6));
        parts.add(Mesh.lathe(18, 1.0, 0.0, 0.16, 0.6, 0.16, 0.68, 0.5, 0.6, 0.85, 0.32, 1.08, 0.13, MIDDLE, 0.32,
                top - 1.08, 0.6, top - 0.85, 0.68, top - 0.5, 0.6, top - 0.16, 0.0, top - 0.16));
        for (int k = 0; k < 3; k++) {
            double angle = Math.PI * 2.0 * k / 3.0;
            parts.add(Mesh.cylinder(8, 0.07, 0.16, top - 0.16, 1.6).moved(Math.cos(angle) * 0.76, 0.0,
                    Math.sin(angle) * 0.76));
        }
        List<Mesh> placed = new ArrayList<>();
        for (Mesh part : parts) {
            placed.add(part.alongZ().moved(FOOT.x, FOOT.y, FOOT.z));
        }
        return placed.toArray(Mesh[]::new);
    }

    // Solid parts, drawn with the hand (and cut where it is cut).
    public static void parts(LanternPainter painter, ConstructPayload hand, HandPose.Place place, Vec3 base,
            Vec3 facing, double clock, double reach, double bright, double apart) {
        switch (HandPose.move(hand.variant())) {
            case HandPose.BAT -> bat(painter, hand, place, base, facing, clock, reach, bright, apart);
            case HandPose.HOURGLASS -> hourglass(painter, place, clock, bright, apart);
            default -> {
            }
        }
    }

    public static void blows(LanternPainter painter, ConstructPayload hand, HandPose pose, HandPose.Place place,
            Vec3 base, Vec3 facing, double clock, double reach, double strength) {
        if (strength <= 0.01) {
            return;
        }
        switch (HandPose.move(hand.variant())) {
            case HandPose.BAT -> batLight(painter, hand, pose, place, base, facing, clock, reach, strength);
            case HandPose.HOURGLASS -> timeLight(painter, place, base, clock, strength);
            case HandPose.GRAVITY -> gravityLight(painter, hand.id(), base, facing, clock, place.scale(), strength);
            case HandPose.BLACKHOLE -> HandHoleLight.draw(painter, hand.id(), place, clock, strength);
            default -> {
            }
        }
    }

    // The handle pours out of the fist first, then the barrel swells; flipped away it tumbles up into the air and
    // breaks into solid pieces there.
    private static void bat(LanternPainter painter, ConstructPayload hand, HandPose.Place place, Vec3 base,
            Vec3 facing, double clock, double reach, double bright, double apart) {
        double grown = HandPose.batGrown(clock);
        double broken = HandPose.batBroken(clock);
        if (grown <= 0.01 || broken >= 1.0) {
            return;
        }
        double flying = HandPose.batFlying(clock);
        ConstructPainter.Frame frame = flying < 0.0 ? handFrame(place, false)
                : flown(hand.variant(), base, facing, reach, place.scale(), flying);
        if (broken > 0.0 || apart >= 0.0) {
            painter.fling(1.4);
            painter.shattered(BAT, frame, Math.max(Math.max(broken, 0.0), apart), bright * 1.1, 151);
            painter.fling(1.0);
            return;
        }
        painter.glare(0.8 * (1.0 - grown));
        ConstructPainter.Frame drawn = grown >= 1.0 ? frame
                : frame.moved(HOLD.x, HOLD.y, HOLD.z).stretched(Ease.smooth(grown), Ease.smooth(grown / 0.6),
                        Ease.smooth(grown / 0.6)).moved(-HOLD.x, -HOLD.y, -HOLD.z);
        painter.shape(BAT, drawn, 1.0, bright);
        painter.glare(0.0);
    }

    // The bat as the fist let it go, thrown up and on the way it swung, turning end over end.
    private static ConstructPainter.Frame flown(int variant, Vec3 base, Vec3 facing, double reach, double scale,
            double since) {
        double at = HandPose.BAT_FLIPS;
        HandPose.Place then = HandPose.at(variant, at, reach).place(base, facing, scale);
        HandPose.Place before = HandPose.at(variant, at - 1.0, reach).place(base, facing, scale);
        Vec3 middle = HandPose.batPoint(BAT_MIDDLE);
        Vec3 way = Vectors.flat(then.at(middle).subtract(before.at(middle)));
        Vec3 moved = way.scale(0.18 * scale * since).add(0.0, (0.6 * since - 0.035 * since * since) * scale, 0.0);
        ConstructPainter.Frame hand = handFrame(then, false);
        ConstructPainter.Frame thrown = new ConstructPainter.Frame(hand.center().add(moved), hand.right(), hand.up(),
                hand.forward(), hand.scale());
        return thrown.turned(middle.x, middle.y, middle.z, 0.0, 0.0, 1.0, 0.5 * since);
    }

    private static void batLight(LanternPainter painter, ConstructPayload hand, HandPose pose, HandPose.Place place,
            Vec3 base, Vec3 facing, double clock, double reach, double strength) {
        double grown = HandPose.batGrown(clock);
        Vec3 tip = place.at(HandPose.batPoint(HandPose.BAT_TIP));
        if (grown > 0.0 && grown < 1.0) {
            Vec3 gem = ringGem(pose, place);
            double on = 1.0 - grown * grown;
            painter.flare(gem, 1.6 * on * place.scale(), on * strength);
            painter.beamOfLight(gem, tip, on * strength, clock, 0.4);
        }
        double swing = clock - HandPose.BAT_SWINGS;
        double hit = clock - HandPose.BAT_HITS;
        if (swing >= 0.0 && hit < 4.0) {
            Vec3 last = tip;
            for (int k = 1; k <= 6; k++) {
                double then = clock - 0.5 * k;
                if (then < HandPose.BAT_SWINGS - 1.0) {
                    break;
                }
                Vec3 was = HandPose.at(hand.variant(), then, reach).place(base, facing, place.scale())
                        .at(HandPose.batPoint(HandPose.BAT_TIP));
                double fade = (1.0 - k / 7.0) * (hit < 0.0 ? 1.0 : 1.0 - hit / 4.0) * strength;
                painter.glowLine(last, was, 2.0 * place.scale(), LIGHT, Colors.alpha(0.35 * fade));
                painter.lightLine(last, was, 0.22 * place.scale(), WHITE, Colors.alpha(0.8 * fade));
                last = was;
            }
        }
        if (hit >= 0.0 && hit < 5.0) {
            Vec3 barrel = place.at(HandPose.batPoint(4.5));
            painter.flare(barrel, 4.0 * (1.0 - hit / 5.0) * place.scale(), strength);
        }
    }

    // Grown standing on the palm, tossed up and turned over; it breaks into solid pieces in the closing fist.
    private static void hourglass(LanternPainter painter, HandPose.Place place, double clock, double bright,
            double apart) {
        double grown = HandPose.glassGrown(clock);
        double broken = HandPose.glassBroken(clock);
        if (grown <= 0.01 || broken >= 1.0) {
            return;
        }
        ConstructPainter.Frame frame = glassFrame(place, clock);
        if (broken > 0.0 || apart >= 0.0) {
            painter.fling(1.3);
            painter.shattered(GLASS, frame, Math.max(Math.max(broken, 0.0), apart), bright * 1.1, 171);
            painter.fling(1.0);
            return;
        }
        painter.glare(0.8 * (1.0 - grown));
        ConstructPainter.Frame drawn = grown >= 1.0 ? frame
                : frame.moved(FOOT.x, FOOT.y, FOOT.z).stretched(grown, grown, Ease.backOut(grown))
                        .moved(-FOOT.x, -FOOT.y, -FOOT.z);
        painter.shape(GLASS, drawn, 1.0, bright);
        painter.glare(0.0);
    }

    private static ConstructPainter.Frame glassFrame(HandPose.Place place, double clock) {
        return handFrame(place, false).moved(0.0, 0.0, 1.4 * HandPose.glassToss(clock)).turned(FOOT.x, FOOT.y,
                FOOT.z + MIDDLE, 1.0, 0.0, 0.0, HandPose.glassTurn(clock));
    }

    // The sand as rings of light round the hourglass, its level falling in the upper half and rising in the lower;
    // round the hand the clock of stopped time lies on the ground, its hands standing still until time goes on.
    private static void timeLight(LanternPainter painter, HandPose.Place place, Vec3 base, double clock,
            double strength) {
        double still = HandPose.timeStill(clock);
        double scale = place.scale();
        if (HandPose.glassGrown(clock) >= 1.0 && HandPose.glassBroken(clock) < 0.0) {
            ConstructPainter.Frame glass = glassFrame(place, clock);
            Vec3 middle = glass.at(FOOT.x, FOOT.y, FOOT.z + MIDDLE);
            Vec3 axis = glass.at(FOOT.x, FOOT.y, FOOT.z + MIDDLE + 1.0).subtract(middle).normalize();
            Vec3 up = axis.y >= 0.0 ? axis : axis.scale(-1.0);
            Vec3[] round = Vectors.across(up);
            double sand = HandPose.glassSand(clock);
            double on = still > 0.0 ? 1.0 : 0.5;
            double topLevel = 0.12 + 0.92 * sand;
            double lowLevel = 1.04 - 0.92 * (1.0 - sand);
            ring(painter, middle.add(up.scale(topLevel * scale)), round, (bulb(topLevel) + 0.05) * scale, 0.06 * scale,
                    WHITE, 0.9 * on * strength);
            ring(painter, middle.subtract(up.scale(lowLevel * scale)), round, (bulb(lowLevel) + 0.05) * scale,
                    0.06 * scale, WHITE, 0.9 * on * strength);
            if (still > 0.0 && sand > 0.0) {
                painter.flare(middle, (0.5 + 0.15 * Math.sin(clock * 1.7)) * scale, 0.8 * still * strength);
            }
        }
        double wave = (clock - HandPose.TIME_STOPS + 6.0) / 6.0;
        Vec3 ground = new Vec3(base.x, base.y + 0.06, base.z);
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        double radius = TIME_RADIUS * scale;
        if (wave > 0.0 && wave < 1.0) {
            painter.circle(ground, east, south, radius * Ease.smooth(wave), 0.1, 0.8, Colors.alpha(strength),
                    Colors.alpha(0.6 * strength));
        }
        if (still <= 0.01) {
            return;
        }
        double fade = still * strength;
        painter.circle(ground, east, south, radius, 0.08, 0.6, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
        painter.circle(ground, east, south, radius * 0.86, 0.04, 0.3, Colors.alpha(0.5 * fade),
                Colors.alpha(0.25 * fade));
        for (int k = 0; k < 12; k++) {
            double angle = Math.PI * 2.0 * k / 12.0;
            Vec3 out = east.scale(Math.cos(angle)).add(south.scale(Math.sin(angle)));
            double inner = k % 3 == 0 ? 0.74 : 0.8;
            painter.lightLine(ground.add(out.scale(radius * inner)), ground.add(out.scale(radius * 0.92)),
                    (k % 3 == 0 ? 0.22 : 0.12) * scale, WHITE, Colors.alpha(0.85 * fade));
        }
        // Time stood still when the hands stopped; going on again they race round to catch up.
        double catching = Ease.smoother((clock - HandPose.TIME_GOES) / 10.0);
        double minute = Math.PI * 0.3 + Math.PI * 4.0 * catching;
        double hour = Math.PI * 1.15 + Math.PI * 0.4 * catching;
        hand(painter, ground, minute, radius * 0.7, 0.16 * scale, fade);
        hand(painter, ground, hour, radius * 0.45, 0.26 * scale, fade);
        painter.flare(ground.add(0.0, 0.1, 0.0), 0.8 * scale, 0.8 * fade);
        painter.shell(base.add(0.0, 0.1, 0.0), radius, PALE, 0.25 * fade);
    }

    // The hourglass's radius at height h from its middle.
    private static double bulb(double h) {
        double[] heights = { 0.0, 0.12, 0.35, 0.7, 1.04 };
        double[] radii = { 0.13, 0.32, 0.6, 0.68, 0.6 };
        for (int k = 1; k < heights.length; k++) {
            if (h <= heights[k]) {
                double u = (h - heights[k - 1]) / (heights[k] - heights[k - 1]);
                return radii[k - 1] + (radii[k] - radii[k - 1]) * u;
            }
        }
        return radii[radii.length - 1];
    }

    private static void hand(LanternPainter painter, Vec3 ground, double angle, double length, double width,
            double fade) {
        Vec3 out = new Vec3(Math.sin(angle), 0.0, -Math.cos(angle));
        painter.glowLine(ground, ground.add(out.scale(length)), width * 5.0, LIGHT, Colors.alpha(0.3 * fade));
        painter.lightLine(ground, ground.add(out.scale(length)), width, WHITE, Colors.alpha(0.9 * fade));
    }

    static void ring(LanternPainter painter, Vec3 center, Vec3[] round, double radius, double width, int rgb,
            double strength) {
        if (strength <= 0.01) {
            return;
        }
        Vec3 last = center.add(round[0].scale(radius));
        for (int i = 1; i <= ROUND; i++) {
            double angle = Math.PI * 2.0 * i / ROUND;
            Vec3 next = center.add(round[0].scale(Math.cos(angle) * radius))
                    .add(round[1].scale(Math.sin(angle) * radius));
            painter.lightLine(last, next, width, rgb, Colors.alpha(strength));
            last = next;
        }
    }

    // Under the turned hand a ring on the ground and streaks of light falling up out of it; as it slams down a
    // shockwave runs out over the ground.
    private static void gravityLight(LanternPainter painter, int id, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        Vec3 field = new Vec3(base.x + facing.x, base.y + 0.06, base.z + facing.z);
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        double radius = GRAVITY_RADIUS * scale;
        double flipped = HandPose.gravityFlipped(clock) * strength;
        if (flipped > 0.01) {
            painter.circle(field, east, south, radius, 0.08, 0.6, Colors.alpha(0.9 * flipped),
                    Colors.alpha(0.45 * flipped));
            painter.circle(field.add(0.0, 8.5 * scale, 0.0), east, south, radius * 0.8, 0.05, 0.4,
                    Colors.alpha(0.4 * flipped), Colors.alpha(0.2 * flipped));
            for (int k = 0; k < RISING; k++) {
                double angle = Math.PI * 2.0 * Noise.of(id, k, 1);
                double out = Math.sqrt(Noise.of(id, k, 2)) * radius;
                double run = (clock * 0.05 + Noise.of(id, k, 3)) % 1.0;
                Vec3 from = field.add(Math.cos(angle) * out, run * 10.0 * scale, Math.sin(angle) * out);
                double fade = flipped * Math.sin(Math.PI * run);
                painter.lightLine(from, from.add(0.0, 1.4 * scale, 0.0), 0.08 * scale, PALE,
                        Colors.alpha(0.8 * fade));
            }
        }
        double slammed = HandPose.gravitySlammed(clock);
        if (slammed > 0.0 && slammed < 1.0) {
            double fade = (1.0 - slammed) * (1.0 - slammed) * strength;
            painter.circle(field, east, south, radius * (0.4 + 1.4 * Ease.smooth(slammed)), 0.12, 1.0,
                    Colors.alpha(fade), Colors.alpha(0.6 * fade));
            if (slammed < 0.25) {
                painter.glowDisc(field.add(0.0, 0.2, 0.0), radius * (1.0 - slammed * 3.0), PALE,
                        0.6 * strength, 0.25, id);
            }
        }
    }
}
