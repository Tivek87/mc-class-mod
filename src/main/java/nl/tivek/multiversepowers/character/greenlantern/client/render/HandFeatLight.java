package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandLight.shockwave;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.ringGem;

// The light of the ragdoll, the catch, the finger gun, the scissors, the swallow, the ring beam and the scoop, and the
// burst of a hand out of a wall.
final class HandFeatLight {
    private static final int SMOKE = 0xD8E6DC;
    private static final double BEAM_RANGE = 24.0;

    private HandFeatLight() {
    }

    // The wall cracks open round a hand breaking out of it, as the ground does.
    static void wallBurst(LanternPainter painter, int id, Vec3 base, Vec3 out, double clock, int variant, double scale,
            double strength) {
        double arrives = HandPose.ARRIVES;
        double burst = strength * Ease.smooth((clock - arrives * 0.25) / (arrives * 0.75))
                * (1.0 - Ease.smooth((clock - arrives - 12.0) / 18.0));
        double sink = strength * Ease.smooth((clock - HandPose.sinks(variant)) / 6.0)
                * (1.0 - Ease.smooth((clock - HandPose.life(variant) + 3.0) / 3.0));
        double glow = Math.max(burst, 0.6 * sink);
        if (glow <= 0.01) {
            return;
        }
        Vec3 at = base.add(out.scale(0.06));
        Vec3 along = Vectors.UP.cross(out).normalize();
        double wide = (1.6 + 1.5 * Ease.smooth(clock / (arrives + 4.0))) * scale;
        painter.circle(at, along, Vectors.UP, wide, 0.12, 1.1, Colors.alpha(0.9 * glow), Colors.alpha(0.45 * glow));
        painter.circle(at, along, Vectors.UP, wide * 0.6, 0.08, 0.7, Colors.alpha(0.6 * glow),
                Colors.alpha(0.3 * glow));
        if (burst > 0.01) {
            painter.flare(base.add(out.scale(0.4)), (1.5 + 2.5 * burst) * scale, burst);
            double grow = Ease.smooth((clock - arrives + 1.5) / 6.0);
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI * 2.0 * (i + 0.4 * Noise.of(id, i, 1)) / 10.0;
                Vec3 from = at;
                for (int s = 1; s <= 3; s++) {
                    double bend = angle + (Noise.of(id, i, 3 + s) - 0.5) * 0.6;
                    double reach = 2.2 * scale * (1.0 + Noise.of(id, i, 2)) * grow * s / 3.0;
                    Vec3 to = at.add(along.scale(Math.cos(bend) * reach)).add(0.0, Math.sin(bend) * reach, 0.0);
                    painter.edge(from, to, 0.08 * scale * (1.2 - 0.25 * s), burst);
                    from = to;
                }
            }
        }
    }

    static void blows(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock, double scale,
            double strength) {
        if (strength <= 0.01) {
            return;
        }
        switch (HandPose.move(variant)) {
            case HandPose.RAGDOLL -> ragdoll(painter, variant, base, facing, clock, scale, strength);
            case HandPose.CATCH -> squeeze(painter, variant, base, facing, clock, scale, strength);
            case HandPose.FINGERGUN -> gun(painter, variant, base, facing, clock, scale, strength);
            case HandPose.SCISSORS -> snips(painter, variant, base, facing, clock, scale, strength);
            case HandPose.SWALLOW -> sky(painter, variant, base, clock, strength);
            case HandPose.RINGBEAM -> beam(painter, variant, base, facing, clock, scale, strength);
            case HandPose.SCOOP -> scoop(painter, variant, base, facing, clock, scale, strength);
            default -> {
            }
        }
    }

    private static HandPose.Place place(int variant, Vec3 base, Vec3 facing, double t, double scale) {
        return HandPose.at(variant, t, 0.0).place(base, facing, scale);
    }

    // Every slam sends rings of shock over the ground or wall it hits; the wind-up leaves a smear of light and the
    // throw a flash.
    private static void ragdoll(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        int extra = HandPose.extra(variant);
        Vec3 out = HandPose.rootNormal(variant, facing);
        for (int k = 0; k < HandPose.ragdollSlams(extra); k++) {
            double since = clock - HandPose.ragdollSlam(k);
            if (since < 0.0 || since > 14.0) {
                continue;
            }
            Vec3 grip = place(variant, base, facing, HandPose.ragdollSlam(k), scale).at(HandPose.GRIP);
            Vec3 on = grip.subtract(out.scale(grip.subtract(base).dot(out)));
            if (out.y > 0.5) {
                shockwave(painter, on, since, 3.5 * scale, 2, strength);
            } else {
                double u = Math.min(1.0, since / 9.0);
                double fade = strength * (1.0 - u) * (1.0 - u);
                Vec3 along = Vectors.UP.cross(out).normalize();
                painter.circle(on.add(out.scale(0.08)), along, Vectors.UP, 0.5 + 3.5 * scale * u, 0.1, 0.8,
                        Colors.alpha(fade), Colors.alpha(0.5 * fade));
                if (since < 3.0) {
                    painter.flare(on.add(out.scale(0.3)), 3.0 * (1.0 - since / 3.0), strength);
                }
            }
        }
        double wind = HandPose.ragdollWinds(extra);
        double launch = HandPose.ragdollLaunch(extra);
        if (clock > wind && clock < launch + 3.0) {
            double fade = strength * Ease.smooth((clock - wind) / (launch - wind)) * (1.0 - Ease.smooth((clock
                    - launch) / 3.0));
            Vec3 last = null;
            for (int k = 0; k <= 6; k++) {
                Vec3 at = place(variant, base, facing, clock - k * 0.5, scale).at(HandPose.GRIP);
                if (last != null) {
                    painter.edge(last, at, 0.25 * scale, 0.7 * fade * (1.0 - k / 7.0));
                }
                last = at;
            }
        }
        double since = clock - launch;
        if (since >= 0.0 && since < 4.0) {
            painter.flare(place(variant, base, facing, launch, scale).at(HandPose.GRIP), 3.0 * (1.0 - since / 4.0),
                    strength);
        }
    }

    // Each squeeze of the fist glows.
    private static void squeeze(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        if (clock < HandPose.CATCH_CATCHES || clock > HandPose.CATCH_LETS_GO) {
            return;
        }
        double pulse = Math.pow(0.5 + 0.5 * Math.sin((clock - HandPose.CATCH_CATCHES) * Math.PI * 2.0 / 10.0), 4.0);
        Vec3 fist = place(variant, base, facing, clock, scale).at(HandPose.GRIP);
        painter.flare(fist, (0.8 + 1.2 * pulse) * scale, (0.3 + 0.5 * pulse) * strength);
    }

    // A bolt of light out of the fingertip at each shot, a flash at the muzzle, and a curl of smoke at the end.
    private static void gun(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock, double scale,
            double strength) {
        for (int shot : HandPose.GUN_SHOTS) {
            double since = clock - shot;
            if (since < 0.0 || since > 6.0) {
                continue;
            }
            HandPose.Place place = place(variant, base, facing, shot, scale);
            Vec3 tip = place.at(HandPose.GUN_TIP);
            Vec3 way = place.up().normalize();
            Vec3 head = tip.add(way.scale(since * 5.0));
            Vec3 tail = head.subtract(way.scale(Math.min(since * 5.0, 3.0)));
            double fade = strength * (1.0 - since / 6.0);
            painter.beamOfLight(tail, head, fade, clock, 0.3);
            painter.flare(head, 0.6, fade);
            if (since < 2.0) {
                painter.flare(tip, 1.5 * (1.0 - since / 2.0), strength);
            }
        }
        double smoke = clock - HandPose.GUN_SMOKES + 4.0;
        if (smoke > 0.0 && smoke < 20.0) {
            Vec3 tip = place(variant, base, facing, clock, scale).at(HandPose.GUN_TIP);
            for (int k = 0; k < 5; k++) {
                double u = (smoke / 20.0 + k * 0.2) % 1.0;
                Vec3 at = tip.add(0.0, 0.3 + 2.0 * u, 0.0).add(Noise.direction(k, 5).scale(0.3 * u));
                painter.lightDisc(at, 0.25 + 0.6 * u, SMOKE, 0.25 * Math.sin(Math.PI * u) * strength, 0.35, k);
            }
        }
    }

    // A cross of light where the blades close.
    private static void snips(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        for (int snip : HandPose.SNIPS) {
            double since = clock - snip;
            if (since < 0.0 || since > 7.0) {
                continue;
            }
            HandPose.Place place = place(variant, base, facing, snip, scale);
            Vec3 cut = place.at(HandPose.SNIP_POINT);
            Vec3 a = place.right().normalize();
            Vec3 b = place.forward().normalize();
            double fade = strength * (1.0 - since / 7.0);
            double length = (1.0 + 2.0 * Ease.smooth(since / 2.0)) * scale;
            painter.edge(cut.subtract(a.add(b).scale(length)), cut.add(a.add(b).scale(length)), 0.1, fade);
            painter.edge(cut.subtract(a.subtract(b).scale(length)), cut.add(a.subtract(b).scale(length)), 0.1, fade);
            if (since < 2.5) {
                painter.flare(cut, 2.0 * (1.0 - since / 2.5), strength);
            }
        }
    }

    // The portal in the sky the swallowed creature falls out of.
    private static void sky(LanternPainter painter, int variant, Vec3 base, double clock, double strength) {
        if (clock < HandPose.SKY_OPENS - 2 || clock > HandPose.SKY_SHUTS + 4) {
            return;
        }
        double open = Ease.smoother((clock - HandPose.SKY_OPENS) / 3.0)
                * (1.0 - Ease.smoother((clock - HandPose.SKY_SHUTS) / 3.0));
        Vec3 down = Vectors.UP.scale(-1.0);
        Vec3[] across = Vectors.across(down);
        HandDuo.Portal sky = new HandDuo.Portal(base.add(0.0, HandPose.SKY_HEIGHT, 0.0), down, across[0], across[1],
                2.2, open);
        HandPairLight.portal(painter, sky, variant, clock, strength);
        HandPairLight.flashes(painter, sky, clock, HandPose.SKY_OPENS + 1.0, HandPose.SKY_SHUTS + 1.0, strength);
    }

    // The ring gathers light, then a beam of it runs out along the knuckles to what it hits.
    private static void beam(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock, double scale,
            double strength) {
        if (clock < HandPose.BEAM_FIRES - 16 || clock > HandPose.BEAM_STOPS + 3) {
            return;
        }
        HandPose pose = HandPose.at(variant, clock, 0.0);
        HandPose.Place place = pose.place(base, facing, scale);
        Vec3 gem = ringGem(pose, place);
        if (clock < HandPose.BEAM_FIRES) {
            double u = Ease.smooth((clock - HandPose.BEAM_FIRES + 16.0) / 16.0);
            painter.flare(gem, (0.3 + 1.2 * u) * scale, (0.4 + 0.6 * u) * strength);
            for (int k = 0; k < 8; k++) {
                double run = (clock * 0.12 + Noise.of(k, 3, 7)) % 1.0;
                Vec3 from = gem.add(Noise.direction(k * 7 + (int) (clock * 0.1), 31).scale(2.5 * (1.0 - run)));
                painter.edge(from, from.lerp(gem, 0.4), 0.05, 0.8 * u * Math.sin(Math.PI * run) * strength);
            }
            return;
        }
        double fade = strength * (1.0 - Ease.smooth((clock - HandPose.BEAM_STOPS) / 3.0));
        Vec3 way = place.up().normalize();
        Vec3 end = gem.add(way.scale(BEAM_RANGE));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            HitResult hit = minecraft.level.clip(new ClipContext(gem, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, minecraft.player));
            if (hit.getType() != HitResult.Type.MISS) {
                end = hit.getLocation();
            }
        }
        painter.beamOfLight(gem, end, fade, clock, 0.9);
        painter.flare(gem, 1.5 * scale, fade);
        painter.flare(end, 2.0, fade);
    }

    // A sweep of light along the palm as it tosses.
    private static void scoop(LanternPainter painter, int variant, Vec3 base, Vec3 facing, double clock,
            double scale, double strength) {
        double since = clock - HandPose.SCOOP_TOSSES;
        if (since < -2.0 || since > 4.0) {
            return;
        }
        double fade = strength * Ease.smooth((since + 2.0) / 1.5) * (1.0 - Ease.smooth(since / 4.0));
        Vec3 lastIn = null;
        Vec3 lastOut = null;
        for (int k = 0; k <= 5; k++) {
            HandPose.Place place = place(variant, base, facing, clock - k * 0.6, scale);
            Vec3 in = place.at(new Vec3(0.0, 1.2, 0.0));
            Vec3 out = place.at(new Vec3(0.0, 4.8, 0.0));
            if (lastIn != null) {
                double a = fade * (1.0 - (k - 1) / 5.0) * 0.5;
                double b = fade * (1.0 - k / 5.0) * 0.5;
                painter.sheet(lastIn, lastOut, out, in, a, a, b, b);
            }
            lastIn = in;
            lastOut = out;
        }
    }
}
