package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.GLOWS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.PORTAL_SEAM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.drawHand;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.ringGem;

// A group of hands out of portals round one creature, and the light of what they do: the ring charging and blasting,
// the clap, the tear.
final class HandGroupPainter {
    private static final double CALL = 5.0;
    private static final double BLAST_TICKS = 14.0;
    private static final int SHARDS = 16;

    private HandGroupPainter() {
    }

    static void draw(LanternPainter painter, int id, int variant, Vec3 base, Vec3 facing, double clock,
            @Nullable Vec3 ring, double strength, double apart) {
        List<HandGroup.Sub> subs = HandGroup.at(variant, base, facing, clock);
        for (int k = 0; k < subs.size(); k++) {
            HandGroup.Sub sub = subs.get(k);
            HandPairLight.portal(painter, sub.portal(), id + 17 * k, clock, strength);
            HandPairLight.flashes(painter, sub.portal(), clock, 1.0, HandPose.life(variant) - 2.0, strength);
            if (ring != null && clock < CALL + 3.0) {
                double u = Ease.smooth(clock / CALL);
                painter.beamOfLight(ring, ring.lerp(sub.portal().center(), u),
                        1.0 - Ease.smooth((clock - CALL) / 3.0), clock, 0.45);
            }
        }
        if (!painter.visible(base, 12.0)) {
            return;
        }
        painter.ambient(GLOWS);
        if (apart > 0.0) {
            painter.fling(1.8);
        }
        for (int k = 0; k < subs.size(); k++) {
            HandGroup.Sub sub = subs.get(k);
            painter.clip(sub.portal().center(), sub.portal().normal(), PORTAL_SEAM);
            drawHand(painter, sub.pose(), sub.place(), sub.left(), apart > 0.0 ? 1.2 : 1.0, apart, 31 * k, true);
            painter.noClip();
        }
        painter.fling(1.0);
        painter.ambient(0.0);
        switch (HandPose.move(variant)) {
            case HandPose.CLAP -> clap(painter, base, facing, clock, strength);
            case HandPose.TEAR -> tear(painter, variant, base, facing, clock, strength);
            default -> ringCharge(painter, subs, base, facing, clock, strength);
        }
    }

    // The tear: crackles of light arc round the creature from grip to grip, flaring at every jerk, a seam glows round
    // its middle where it will part, and the light bursts out round it as it tears.
    private static void tear(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double strength) {
        Vec3 held = HandGroup.tearHeld(base, clock);
        Vec3 ahead = new Vec3(facing.x, 0.0, facing.z).normalize();
        Vec3 side = ahead.cross(Vectors.UP).normalize();
        double strain = HandGroup.tearStrain(clock) * strength;
        if (strain > 0.0 && clock < HandGroup.TEARS) {
            double tall = HandGroup.tearTall(variant);
            Vec3 top = HandGroup.tearGrip(true, tall, held, clock);
            Vec3 bottom = HandGroup.tearGrip(false, tall, held, clock);
            double rise = Ease.smooth((clock - HandGroup.TEAR_PULLS) / (HandGroup.TEARS - HandGroup.TEAR_PULLS));
            double jerk = Math.max(0.0, 1.0 - Math.max(0.0, clock - HandGroup.TEAR_PULLS)
                    % HandGroup.TEAR_PULL_EVERY / 4.0);
            int flick = (int) (clock * 0.5);
            for (int k = 0; k < 3; k++) {
                double turn = Math.PI * 2.0 * k / 3.0 + clock * 0.07;
                Vec3 out = ahead.scale(Math.cos(turn)).add(side.scale(Math.sin(turn)));
                Vec3 from = top;
                for (int s = 1; s <= 6; s++) {
                    Vec3 to = top.lerp(bottom, s / 6.0);
                    if (s < 6) {
                        double bulge = Math.sin(Math.PI * s / 6.0) * (0.3 + 0.15 * rise);
                        to = to.add(out.scale(bulge * (0.7 + 0.6 * Noise.of(k * 7 + s, flick, 51))))
                                .add(0.0, (Noise.of(k * 7 + s, flick, 52) - 0.5) * 0.2, 0.0);
                    }
                    painter.edge(from, to, 0.035, strain * (0.25 + 0.35 * rise + 0.4 * jerk));
                    from = to;
                }
            }
            double seam = Math.min(1.0, strain * (0.3 + 0.5 * rise + 0.3 * jerk));
            painter.circle(held, ahead, side, 0.42 + 0.08 * jerk, 0.05, 0.5, Colors.alpha(seam),
                    Colors.alpha(0.5 * seam));
            if (jerk > 0.0) {
                painter.flare(held, 0.6 + 1.2 * jerk * (0.5 + rise), jerk * strain);
                painter.flare(top, 0.5 * jerk, 0.6 * jerk * strain);
                painter.flare(bottom, 0.5 * jerk, 0.6 * jerk * strain);
            }
        }
        double since = clock - HandGroup.TEARS;
        if (since < 0.0 || since > BLAST_TICKS) {
            return;
        }
        double u = since / BLAST_TICKS;
        double fade = strength * (1.0 - u) * (1.0 - u);
        if (since < 4.0) {
            painter.flare(held, 5.0 * (1.0 - since / 4.0), strength);
            painter.glowDisc(held, 3.0 * (1.0 - since / 4.0), 0xB8FFC8, 0.7 * strength, 0.25, 11);
        }
        double radius = 0.4 + 5.5 * (1.0 - (1.0 - u) * (1.0 - u));
        painter.circle(held, ahead, side, radius, 0.1, 0.8, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
        painter.circle(held.add(0.0, 0.15, 0.0), ahead, side, radius * 0.7, 0.07, 0.6, Colors.alpha(0.7 * fade),
                Colors.alpha(0.35 * fade));
        for (int k = 0; k < SHARDS; k++) {
            double turn = Math.PI * 2.0 * (k + 0.6 * Noise.of(k, 3, 61)) / SHARDS;
            Vec3 way = ahead.scale(Math.cos(turn)).add(side.scale(Math.sin(turn)))
                    .add(0.0, (Noise.of(k, 4, 62) - 0.5) * 0.5, 0.0).normalize();
            double out = radius * (0.6 + 0.5 * Noise.of(k, 2, 63));
            painter.edge(held.add(way.scale(out * 0.55)), held.add(way.scale(out)), 0.1, fade);
        }
    }

    // Light gathers on the ring pressed to the creature, faster and brighter, then bursts in a blast of hard light.
    private static void ringCharge(LanternPainter painter, List<HandGroup.Sub> subs, Vec3 base, Vec3 facing,
            double clock, double strength) {
        HandGroup.Sub hand = subs.get(subs.size() - 1);
        Vec3 gem = ringGem(hand.pose(), hand.place());
        Vec3 ahead = new Vec3(facing.x, 0.0, facing.z).normalize();
        Vec3[] across = Vectors.across(ahead);
        double charge = (clock - HandGroup.RING_PRESSES) / (HandGroup.RING_BLASTS - HandGroup.RING_PRESSES);
        if (charge >= 0.0 && charge < 1.0) {
            double u = Ease.smooth(charge);
            double flicker = 0.8 + 0.2 * Math.sin(clock * (3.0 + 6.0 * u));
            painter.flare(gem, (0.3 + 1.6 * u) * flicker, (0.5 + 0.5 * u) * strength);
            for (int k = 0; k < 2; k++) {
                double turn = clock * (0.3 + 0.4 * u) * (k == 0 ? 1.0 : -1.3);
                Vec3 a = across[0].scale(Math.cos(turn)).add(across[1].scale(Math.sin(turn)));
                Vec3 b = a.cross(ahead).normalize();
                double radius = (0.4 + 0.9 * u) * (k == 0 ? 1.0 : 0.7);
                painter.circle(gem, a, b.scale(0.35).add(ahead.scale(0.94)).normalize(), radius, 0.03, 0.25,
                        Colors.alpha(0.9 * u * strength), Colors.alpha(0.4 * u * strength));
            }
            for (int k = 0; k < 10; k++) {
                double run = (clock * (0.08 + 0.12 * u) + Noise.of(k, 3, 7)) % 1.0;
                Vec3 from = gem.add(Noise.direction(k * 13 + (int) (clock * 0.1 + Noise.of(k, 1, 9) * 5.0), 21)
                        .scale(3.0 * (1.0 - run)));
                painter.edge(from, from.lerp(gem, 0.35), 0.05, 0.8 * u * Math.sin(Math.PI * run) * strength);
            }
        }
        double since = clock - HandGroup.RING_BLASTS;
        if (since < 0.0 || since > BLAST_TICKS) {
            return;
        }
        double u = since / BLAST_TICKS;
        double fade = strength * (1.0 - u) * (1.0 - u);
        Vec3 at = HandGroup.ringPoint(base, facing, HandGroup.RING_BLASTS);
        if (since < 4.0) {
            painter.flare(at, 7.0 * (1.0 - since / 4.0), strength);
            painter.glowDisc(at, 5.0 * (1.0 - since / 4.0), 0xB8FFC8, 0.8 * strength, 0.25, 7);
        }
        double radius = 0.5 + 7.5 * (1.0 - (1.0 - u) * (1.0 - u));
        Vec3 side = ahead.cross(Vectors.UP).normalize();
        painter.circle(at, ahead, Vectors.UP, radius, 0.1, 0.8, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
        painter.circle(at, side, Vectors.UP, radius * 0.85, 0.08, 0.7, Colors.alpha(0.8 * fade),
                Colors.alpha(0.4 * fade));
        painter.circle(at, ahead, side, radius * 1.1, 0.1, 0.8, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
        for (int k = 0; k < SHARDS; k++) {
            Vec3 way = Noise.direction(k, 44);
            double out = radius * (0.6 + 0.5 * Noise.of(k, 2, 45));
            painter.edge(at.add(way.scale(out * 0.55)), at.add(way.scale(out)), 0.12, fade);
        }
    }

    // The clap: a flash between the palms and rings of shock bursting out round them.
    private static void clap(LanternPainter painter, Vec3 base, Vec3 facing, double clock, double strength) {
        double since = clock - HandGroup.CLAP_HITS;
        if (since < 0.0 || since > 12.0) {
            return;
        }
        Vec3 at = HandGroup.clapPoint(base);
        Vec3 ahead = new Vec3(facing.x, 0.0, facing.z).normalize();
        Vec3 side = ahead.cross(Vectors.UP).normalize();
        double u = since / 12.0;
        double fade = strength * (1.0 - u) * (1.0 - u);
        if (since < 3.0) {
            painter.flare(at, 4.0 * (1.0 - since / 3.0), strength);
        }
        double radius = 0.5 + 5.0 * (1.0 - (1.0 - u) * (1.0 - u));
        painter.circle(at, ahead, Vectors.UP, radius, 0.08, 0.7, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
        painter.circle(at.subtract(0.0, 0.25, 0.0), ahead, side, radius * 1.2, 0.08, 0.7, Colors.alpha(0.7 * fade),
                Colors.alpha(0.35 * fade));
    }
}
