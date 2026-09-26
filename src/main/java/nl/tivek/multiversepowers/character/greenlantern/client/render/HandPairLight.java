package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandLight.crack;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandLight.cracks;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandLight.shockwave;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.thumbTip;

final class HandPairLight {
    private static final double AXE_CALL = 5.0;
    private static final double SIDES_SHUT = shut(false);
    private static final double AXE_SHUT = shut(true);

    private HandPairLight() {
    }

    static void lights(LanternPainter painter, int seed, Vec3 base, int variant, Vec3 aim, HandDuo duo,
            double clock, double scale, double strength) {
        snap(painter, duo, clock, scale, strength);
        double ok = strength * Ease.smooth((clock - HandDuo.OK + 1.5) / 2.5)
                * (1.0 - Ease.smooth((clock - HandDuo.OK - 16.0) / 6.0));
        if (ok > 0.01) {
            twinkle(painter, duo.leftPlace.at(HandDuo.OK_AT), 0.8 * scale, ok, clock);
        }
        grab(painter, duo, clock, scale, strength);
        chop(painter, base, variant, aim, clock, scale, strength);
        impact(painter, seed, base, variant, aim, clock, scale, strength);
        double thumbs = strength * Ease.smooth((clock - HandDuo.THUMBS + 1.0) / 2.5)
                * (1.0 - Ease.smooth((clock - HandDuo.THUMBS - 11.0) / 5.0));
        if (thumbs > 0.01 && duo.handsThere) {
            twinkle(painter, thumbTip(duo.rightPose, duo.rightPlace, false), 0.9 * scale, thumbs, clock);
            twinkle(painter, thumbTip(duo.leftPose, duo.leftPlace, true), 0.9 * scale, thumbs, clock + 2.3);
        }
    }

    static void calls(LanternPainter painter, HandDuo duo, double clock, Vec3 ring) {
        double arrives = HandDuo.ARRIVES;
        if (clock < arrives + 3.0) {
            double u = Ease.smooth(clock / arrives);
            double fade = 1.0 - Ease.smooth((clock - arrives) / 3.0);
            painter.beamOfLight(ring, ring.lerp(duo.rightPortal.center(), u), fade, clock, 0.55);
            painter.beamOfLight(ring, ring.lerp(duo.leftPortal.center(), u), fade, clock, 0.55);
        }
        double from = HandDuo.AXE_OPENS - AXE_CALL;
        if (clock > from && clock < HandDuo.AXE_OPENS + 3.0) {
            double u = Ease.smooth((clock - from) / AXE_CALL);
            double fade = 1.0 - Ease.smooth((clock - HandDuo.AXE_OPENS) / 3.0);
            painter.beamOfLight(ring, ring.lerp(duo.axePortal.center(), u), fade, clock - from, 0.55);
        }
    }

    static void portal(LanternPainter painter, HandDuo.Portal portal, int seed, double spin, double keep) {
        double full = portal.radius();
        double open = Math.max(0.0, portal.open() * keep);
        double r = full * open;
        if (open <= 0.01 || full <= 0.0) {
            return;
        }
        double shown = Mth.clamp(open * 2.5, 0.0, 1.0);
        double w = full * 0.4;
        Vec3 c = portal.center();
        Vec3 n = portal.normal();
        Vec3 a = portal.a();
        Vec3 b = portal.b();
        painter.circle(c, a, b, r, 0.13 * w, 1.0 * w, Colors.alpha(0.95 * shown), Colors.alpha(0.5 * shown));
        painter.circle(c, a, b, r * (1.12 + 0.03 * Math.sin(spin * 0.3)), 0.05 * w, 0.7 * w,
                Colors.alpha(0.3 * shown), Colors.alpha(0.25 * shown));
        arcs(painter, c, a, b, r, 3, 0.9, spin * 0.14 + seed, 0.2 * w, 0.9 * shown);
        arcs(painter, c, a, b, r * 0.82, 5, 0.5, -spin * 0.22 - seed, 0.09 * w, 0.6 * shown);
        int arms = 5;
        int steps = 8;
        double turn = spin * 0.25 + seed * 1.7;
        for (int arm = 0; arm < arms; arm++) {
            Vec3 last = null;
            for (int s = 0; s <= steps; s++) {
                double q = (double) s / steps;
                double angle = turn + Math.PI * 2.0 * arm / arms + 2.6 * q;
                double out = r * (0.94 - 0.84 * q);
                Vec3 at = c.add(a.scale(Math.cos(angle) * out)).add(b.scale(Math.sin(angle) * out));
                if (last != null) {
                    painter.edge(last, at, 0.1 * w * (1.0 - 0.6 * q),
                            0.55 * shown * Math.sin(Math.PI * (0.15 + 0.85 * q)));
                }
                last = at;
            }
        }
        painter.haze(c, a.scale(r * 0.95), n.scale(r * 0.22), b.scale(r * 0.95), painter.material().glow(),
                0.28 * shown);
        painter.flare(c, r * 0.55, 0.3 * shown);
        for (int i = 0; i < 10; i++) {
            double period = 8.0 + 6.0 * Noise.of(seed, i, 1);
            double run = (spin + period * Noise.of(seed, i, 2)) / period;
            int round = (int) Math.floor(run);
            double f = run - round;
            double angle = Math.PI * 2.0 * Noise.of(seed * 31 + round, i, 3);
            Vec3 outward = a.scale(Math.cos(angle)).add(b.scale(Math.sin(angle)));
            Vec3 along = a.scale(-Math.sin(angle)).add(b.scale(Math.cos(angle)));
            Vec3 way = outward.scale(0.8).add(along.scale(0.7)).add(n.scale(0.35 * (Noise.of(seed, i, 4) - 0.3)));
            Vec3 at = c.add(outward.scale(r)).add(way.scale(full * 0.7 * f));
            painter.edge(at.subtract(way.scale(full * 0.18)), at, 0.05 * w, 0.85 * shown * Math.sin(Math.PI * f));
        }
    }

    private static void arcs(LanternPainter painter, Vec3 c, Vec3 a, Vec3 b, double radius, int count, double length,
            double turn, double width, double strength) {
        int pieces = 5;
        for (int k = 0; k < count; k++) {
            double start = turn + Math.PI * 2.0 * k / count;
            Vec3 last = null;
            for (int s = 0; s <= pieces; s++) {
                double angle = start + length * s / pieces;
                Vec3 at = c.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
                if (last != null) {
                    painter.edge(last, at, width, strength * Math.sin(Math.PI * (s - 0.5) / pieces));
                }
                last = at;
            }
        }
    }

    static void flashes(LanternPainter painter, HandDuo duo, double clock, double strength) {
        flashes(painter, duo.rightPortal, clock, HandDuo.ARRIVES, SIDES_SHUT, strength);
        flashes(painter, duo.leftPortal, clock, HandDuo.ARRIVES, SIDES_SHUT, strength);
        flashes(painter, duo.axePortal, clock, HandDuo.AXE_OPENS, AXE_SHUT, strength);
    }

    static void flashes(LanternPainter painter, HandDuo.Portal portal, double clock, double opens,
            double shut, double strength) {
        double full = portal.radius();
        double burst = strength * Ease.smooth((clock - opens + 1.0) / 1.2)
                * (1.0 - Ease.smooth((clock - opens) / 7.0));
        if (burst > 0.01) {
            painter.flare(portal.center(), full * (1.0 + 0.8 * burst), burst);
        }
        double pop = strength * Ease.smooth((clock - shut + 2.0) / 2.0) * (1.0 - Ease.smooth((clock - shut) / 5.0));
        if (pop > 0.01) {
            painter.flare(portal.center(), full * 0.9 * pop, pop);
            double u = Ease.smooth((clock - shut + 1.0) / 6.0);
            painter.circle(portal.center(), portal.a(), portal.b(), full * (0.2 + 1.1 * u), 0.05 * full,
                    0.35 * full, Colors.alpha(0.8 * pop), Colors.alpha(0.4 * pop));
        }
    }

    private static double shut(boolean axe) {
        int variant = HandPose.axeVariant(new Vec3(0.0, 0.0, 1.0));
        double from = axe ? HandDuo.AXE_FREE : HandDuo.RETRACT;
        for (double t = from; t <= HandDuo.LIFE; t += 0.25) {
            HandDuo duo = HandDuo.at(Vec3.ZERO, variant, Vec3.ZERO, t, 1.0);
            if ((axe ? duo.axePortal : duo.leftPortal).open() <= 0.01) {
                return t;
            }
        }
        return axe ? HandDuo.AXE_FREE + 3.0 : HandDuo.HANDS_GONE;
    }

    private static void snap(LanternPainter painter, HandDuo duo, double clock, double scale, double strength) {
        double since = clock - HandDuo.SNAP;
        if (since < -0.4 || since > 8.0 || strength <= 0.01) {
            return;
        }
        Vec3 at = duo.rightPlace.at(HandDuo.SNAP_AT);
        double in = strength * Ease.smooth((since + 0.4) / 0.5);
        double flash = in * (1.0 - Ease.smooth(since / 5.0));
        painter.flare(at, 2.2 * scale * (0.5 + 0.5 * flash), flash);
        Vec3 view = painter.camera().subtract(at);
        if (view.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] across = Vectors.across(view.normalize());
        double u = 1.0 - Math.pow(1.0 - Mth.clamp(since / 6.0, 0.0, 1.0), 2.0);
        double ring = in * (1.0 - Ease.smooth(since / 7.0));
        painter.circle(at, across[0], across[1], (0.3 + 2.4 * u) * scale, 0.07 * scale, 0.5 * scale,
                Colors.alpha(0.9 * ring), Colors.alpha(0.45 * ring));
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * (i + 0.3 * Noise.of(i, 5, 1)) / 6.0;
            Vec3 way = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)));
            painter.edge(at.add(way.scale((0.2 + 2.2 * u) * scale)), at.add(way.scale((0.4 + 3.0 * u) * scale)),
                    0.06 * scale, ring);
        }
    }

    private static void grab(LanternPainter painter, HandDuo duo, double clock, double scale, double strength) {
        double since = clock - HandDuo.GRAB;
        double flash = strength * Ease.smooth((since + 0.5) / 0.8) * (1.0 - Ease.smooth(since / 6.0));
        if (flash <= 0.01 || !duo.axeThere) {
            return;
        }
        Vec3[] round = Vectors.across(duo.axeUp);
        double u = Ease.smooth(since / 6.0);
        for (double grip : new double[] { HandDuo.GRIP_LOW, HandDuo.GRIP_HIGH }) {
            Vec3 at = duo.axeEnd.add(duo.axeUp.scale(grip * scale));
            painter.flare(at, 2.5 * scale * (0.6 + 0.4 * flash), flash);
            painter.circle(at, round[0], round[1], (HandDuo.HAFT_RADIUS * 2.2 + 1.8 * u) * scale, 0.06 * scale,
                    0.4 * scale, Colors.alpha(0.85 * flash), Colors.alpha(0.4 * flash));
        }
    }

    private static void chop(LanternPainter painter, Vec3 base, int variant, Vec3 aim, double clock, double scale,
            double strength) {
        double window = strength * Ease.smooth((clock - HandDuo.RAISED + 1.0) / 3.0)
                * (1.0 - Ease.smooth((clock - HandDuo.IMPACT - 1.0) / 5.0));
        if (window <= 0.01) {
            return;
        }
        int count = 8;
        double step = 0.7;
        Vec3[] in = new Vec3[count];
        Vec3[] out = new Vec3[count];
        for (int k = 0; k < count; k++) {
            // Uses the cheaper axe-only lookup; the hands aren't needed for this trail
            HandDuo.Axe past = HandDuo.axe(base, variant, aim, clock - k * step, scale);
            Vec3 head = past.end().add(past.up().scale(HandDuo.HEAD_AT * scale));
            in[k] = head.add(past.face().scale(HandDuo.HAFT_RADIUS * 1.5 * scale));
            out[k] = head.add(past.face().scale(HandDuo.EDGE_OUT * scale));
        }
        double[] alphas = new double[count - 1];
        for (int k = 0; k + 1 < count; k++) {
            double speed = out[k].distanceTo(out[k + 1]) / (step * scale);
            alphas[k] = window * Ease.smooth((speed - 0.6) / 1.6) * (1.0 - (double) k / (count - 1)) * 0.6;
        }
        for (int k = 0; k + 2 < count; k++) {
            painter.sheet(in[k], out[k], out[k + 1], in[k + 1], alphas[k], alphas[k], alphas[k + 1], alphas[k + 1]);
        }
    }

    private static void impact(LanternPainter painter, int seed, Vec3 base, int variant, Vec3 aim, double clock,
            double scale, double strength) {
        double since = clock - HandDuo.IMPACT;
        if (since < -0.5 || clock > HandDuo.AXE_BREAKS + 10.0 || strength <= 0.01) {
            return;
        }
        Vec3 strike = HandDuo.strike(base, variant, aim, scale);
        Vec3 ground = new Vec3(strike.x, base.y + 0.06, strike.z);
        shockwave(painter, ground, since, 9.0 * scale, 4, strength);
        double flash = strength * Ease.smooth((since + 0.5) / 0.6) * (1.0 - Ease.smooth(since / 7.0));
        if (flash > 0.01) {
            painter.flare(ground.add(0.0, 0.8 * scale, 0.0), 6.0 * scale * (0.5 + 0.5 * flash), flash);
        }
        double glow = strength * Ease.smooth((since + 0.5) / 1.0) * (0.3 + 0.7 * (1.0 - Ease.smooth(since / 25.0)))
                * (1.0 - Ease.smooth((clock - HandDuo.AXE_BREAKS) / 10.0));
        if (glow <= 0.01) {
            return;
        }
        double grow = Ease.smooth((since + 0.3) / 5.0);
        cracks(painter, seed + 3, ground, 12, 3.0 * scale, grow, glow, 0.1 * scale);
        Vec3 way = HandPose.axeWay(variant);
        double angle = Math.atan2(way.z, way.x);
        for (int side = 0; side < 2; side++) {
            crack(painter, seed + 4, side, ground, angle + Math.PI * side, 7.5 * scale, grow, glow, 0.14 * scale);
        }
    }

    static void twinkle(LanternPainter painter, Vec3 at, double size, double strength, double spin) {
        Vec3 view = painter.camera().subtract(at);
        if (view.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] across = Vectors.across(view.normalize());
        double pulse = 0.8 + 0.2 * Math.sin(spin * 0.9);
        painter.flare(at, size * 0.8 * pulse, strength * 0.8);
        for (int k = 0; k < 8; k++) {
            boolean big = k % 2 == 0;
            double angle = (big ? spin * 0.12 : -spin * 0.09) + Math.PI * 0.25 * k;
            Vec3 ray = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)))
                    .scale(size * (big ? 1.9 : 0.8) * pulse);
            painter.edge(at, at.add(ray), size * (big ? 0.07 : 0.05), strength * (big ? 0.9 : 0.6));
        }
    }
}
