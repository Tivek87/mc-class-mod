package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.RevolverDuo;
import nl.tivek.multiversepowers.character.greenlantern.RevolverGun;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.RevolverDuo.*;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPairLight.twinkle;

final class RevolverLight {
    private static final double CALL = 5.0;
    private static final double TRACER_PAST = 16.0;
    private static final double DROP_TICKS = 12.0;
    private static final int DROPS = 5;
    private static final int CORE = 0xE6FFEC;

    private RevolverLight() {
    }

    static void calls(LanternPainter painter, RevolverDuo duo, double clock, Vec3 ring) {
        if (clock < ARRIVES + 3.0) {
            double u = Ease.smooth(clock / ARRIVES);
            double fade = 1.0 - Ease.smooth((clock - ARRIVES) / 3.0);
            painter.beamOfLight(ring, ring.lerp(duo.aPortal.center(), u), fade, clock, 0.55);
            painter.beamOfLight(ring, ring.lerp(duo.bPortal.center(), u), fade, clock, 0.55);
        }
        double from = TOP_OPENS - CALL;
        if (clock > from && clock < TOP_OPENS + 3.0) {
            double u = Ease.smooth((clock - from) / CALL);
            double fade = 1.0 - Ease.smooth((clock - TOP_OPENS) / 3.0);
            painter.beamOfLight(ring, ring.lerp(duo.topPortal.center(), u), fade, clock - from, 0.55);
        }
    }

    static void flashes(LanternPainter painter, RevolverDuo duo, double clock, double strength) {
        HandPairLight.flashes(painter, duo.aPortal, clock, ARRIVES, HANDS_GONE, strength);
        HandPairLight.flashes(painter, duo.bPortal, clock, ARRIVES, HANDS_GONE, strength);
        HandPairLight.flashes(painter, duo.topPortal, clock, TOP_OPENS, TOP_SHUT + 2.5, strength * 1.6);
    }

    static double propGlare(double clock) {
        double glare = 0.0;
        for (int built : BUILT) {
            glare = Math.max(glare, 0.4 * Ease.smooth((clock - built + 0.5) / 0.5)
                    * (1.0 - Ease.smooth((clock - built) / 4.0)));
        }
        glare = Math.max(glare, 0.5 * Ease.smooth((clock - WHOLE - 1.0) / 1.0)
                * (1.0 - Ease.smooth((clock - WHOLE - 2.0) / 8.0)));
        for (int born : BULLETS_IN_HAND) {
            glare = Math.max(glare, 0.5 * (1.0 - Ease.smooth((clock - born) / 3.0)) * Ease.smooth(clock - born + 1.0));
        }
        return glare;
    }

    static void lights(LanternPainter painter, int seed, RevolverDuo duo, Vec3 aim, double clock, double strength) {
        double scale = duo.stage.scale();
        pews(painter, duo, aim, clock, scale, strength);
        build(painter, duo, clock, scale, strength);
        shots(painter, duo, aim, clock, scale, strength);
        slams(painter, seed, duo, clock, scale, strength);
        bows(painter, duo, clock, scale, strength);
    }

    private static void pews(LanternPainter painter, RevolverDuo duo, Vec3 aim, double clock, double scale,
            double strength) {
        if (clock < PEWS[0] - 1 || clock > BLOW + 16) {
            return;
        }
        Vec3 tip = HandPainter.indexTip(duo.bPose, duo.bPlace, true);
        for (int pew : PEWS) {
            double since = clock - pew;
            double pop = strength * Ease.smooth((since + 0.3) / 0.3) * (1.0 - Ease.smooth(since / 3.0));
            if (pop > 0.01) {
                painter.flare(tip, 1.3 * scale * (0.6 + 0.4 * pop), pop);
            }
            double u = since / BOLT_TICKS;
            if (u >= 0.0 && u <= 1.0) {
                Vec3 head = tip.lerp(aim, u);
                Vec3 tail = tip.lerp(aim, Math.max(0.0, u - 0.4));
                painter.edge(tail, head, 0.16 * scale, strength);
                painter.flare(head, 0.7 * scale, 0.8 * strength);
            }
            double landed = since - BOLT_TICKS;
            double spark = strength * Ease.smooth((landed + 0.3) / 0.3) * (1.0 - Ease.smooth(landed / 5.0));
            if (spark > 0.01) {
                burst(painter, aim, 1.4 * scale, spark, pew);
            }
        }
        double smoke = clock - BLOW;
        if (smoke > 0.0 && smoke < 16.0) {
            for (int puff = 0; puff < 3; puff++) {
                double age = smoke - puff * 3.0;
                if (age <= 0.0 || age > 10.0) {
                    continue;
                }
                Vec3 at = tip.add(0.1 * Math.sin(age + puff), (0.3 + 0.18 * age) * scale, 0.0);
                painter.flare(at, (0.35 + 0.08 * age) * scale, 0.35 * strength * (1.0 - age / 10.0));
            }
        }
    }

    private static void build(LanternPainter painter, RevolverDuo duo, double clock, double scale, double strength) {
        if (clock < HAT_OUT - 1 || clock > GRABS + 30) {
            return;
        }
        double tada = strength * Ease.smooth((clock - HAT_OUT + 1.0) / 2.0) * (1.0 - Ease.smooth((clock - HAT_OUT
                - 8.0) / 5.0));
        if (tada > 0.01) {
            twinkle(painter, HandPainter.indexTip(duo.aPose, duo.aPlace, false), 1.2 * scale, tada, clock);
        }
        for (int part = 0; part < PARTS; part++) {
            double flies = BUILT[part] - FLY_TICKS;
            boolean mine = part == BARREL || part == HAMMER || part == GUARD;
            double cue = strength * Ease.smooth((clock - flies + 1.0) / 1.0) * (1.0 - Ease.smooth((clock - flies)
                    / 5.0));
            if (cue > 0.01) {
                Vec3 finger = mine ? HandPainter.middleTip(duo.aPose, duo.aPlace)
                        : HandPainter.indexTip(duo.bPose, duo.bPlace, true);
                twinkle(painter, finger, 0.8 * scale, cue, clock + part);
            }
            double since = clock - BUILT[part];
            double click = strength * Ease.smooth((since + 0.3) / 0.3) * (1.0 - Ease.smooth(since / 4.0));
            if (click > 0.01) {
                burst(painter, duo.gun.at(RevolverShapes.CENTERS[part]), 1.6 * scale, click, part * 3);
            }
        }
        double glint = strength * Ease.smooth((clock - WHOLE - 1.0) / 2.0) * (1.0 - Ease.smooth((clock - WHOLE - 10.0)
                / 6.0));
        if (glint > 0.01) {
            twinkle(painter, duo.gun.at(new Vec3(0.0, 1.9, 4.0)), 2.4 * scale, glint, clock);
        }
        for (int born : BULLETS_IN_HAND) {
            double since = clock - born;
            double pop = strength * Ease.smooth((since + 0.5) / 0.5) * (1.0 - Ease.smooth(since / 4.0));
            if (pop > 0.01) {
                painter.flare(duo.bPlace.at(PALM_AT.add(0.0, 0.0, 0.4)), 1.0 * scale, pop);
            }
        }
        double shut = clock - CYL_CLOSE;
        double klik = strength * Ease.smooth((shut + 0.3) / 0.3) * (1.0 - Ease.smooth(shut / 4.0));
        if (klik > 0.01) {
            burst(painter, duo.gun.at(RevolverGun.CYLINDER_AT), 1.8 * scale, klik, 7);
        }
    }

    private static void shots(LanternPainter painter, RevolverDuo duo, Vec3 aim, double clock, double scale,
            double strength) {
        if (clock < SHOTS[0] - 1 || clock > DRY[1] + 6) {
            return;
        }
        for (int shot : SHOTS) {
            double since = clock - shot;
            if (since < -0.3 || since > 7.0) {
                continue;
            }
            Vec3 muzzle = RevolverGun.at(duo.stage, aim, shot).at(RevolverGun.MUZZLE);
            Vec3 way = aim.subtract(muzzle);
            if (way.lengthSqr() < 1.0E-4) {
                continue;
            }
            double far = way.length() + TRACER_PAST * scale;
            way = way.normalize();
            double flash = strength * Ease.smooth((since + 0.3) / 0.3) * (1.0 - Ease.smooth(since / 4.0));
            double beam = strength * Ease.smooth((since + 0.3) / 0.3) * (1.0 - Ease.smooth(since / 6.0));
            if (beam > 0.01) {
                Vec3 end = muzzle.add(way.scale(far));
                double thick = scale * (0.35 + 0.25 * (1.0 - beam));
                painter.glowTaper(muzzle, end, 2.2 * thick, 1.2 * thick, painter.material().glow(), 0.6 * beam,
                        0.15 * beam);
                painter.lightTaper(muzzle, end, 0.8 * thick, 0.35 * thick, CORE, 0.95 * beam, 0.25 * beam);
            }
            painter.flare(muzzle, 4.0 * scale * (0.5 + 0.5 * flash), flash);
            Vec3[] across = Vectors.across(way);
            double u = Ease.smooth(since / 5.0);
            painter.circle(muzzle.add(way.scale(0.8 * scale * u)), across[0], across[1], (0.5 + 2.4 * u) * scale,
                    0.08 * scale, 0.5 * scale, Colors.alpha(0.9 * flash), Colors.alpha(0.4 * flash));
            burst(painter, aim, 2.0 * scale, flash, shot);
        }
        for (int dry : DRY) {
            double since = clock - dry;
            double click = strength * Ease.smooth((since + 0.3) / 0.3) * (1.0 - Ease.smooth(since / 3.0));
            if (click > 0.01) {
                painter.flare(duo.gun.at(new Vec3(0.0, 1.6, -0.9)), 0.8 * scale, click);
            }
        }
    }

    private static void slams(LanternPainter painter, int seed, RevolverDuo duo, double clock, double scale,
            double strength) {
        if (clock < SLAMS[0] - 1 || clock > SLAMS[2] + 40) {
            return;
        }
        Vec3 ground = duo.stage.point(SLAM_AT).add(0.0, 0.06, 0.0);
        for (int i = 0; i < SLAMS.length; i++) {
            double since = clock - SLAMS[i];
            if (since < -0.5) {
                continue;
            }
            HandLight.shockwave(painter, ground, since, (8.0 + 2.0 * i) * scale, 3, strength);
            double flash = strength * Ease.smooth((since + 0.5) / 0.6) * (1.0 - Ease.smooth(since / 6.0));
            if (flash > 0.01) {
                painter.flare(ground.add(0.0, 0.8 * scale, 0.0), 5.0 * scale * (0.5 + 0.5 * flash), flash);
            }
            double glow = strength * Ease.smooth((since + 0.5) / 1.0) * (1.0 - Ease.smooth((since - 14.0) / 16.0));
            if (glow > 0.01) {
                HandLight.cracks(painter, seed + 10 + i, ground, 8 + 2 * i, (2.6 + 0.5 * i) * scale,
                        Ease.smooth((since + 0.3) / 4.0), glow, 0.1 * scale);
            }
        }
    }

    private static void bows(LanternPainter painter, RevolverDuo duo, double clock, double scale, double strength) {
        if (clock < WIPED - 1 || clock > SALUTE + 14) {
            return;
        }
        double since = clock - WIPED;
        if (since > 0.0 && since < DROP_TICKS) {
            double fade = strength * (1.0 - since / DROP_TICKS);
            drops(painter, duo.aPlace.at(new Vec3(0.0, 3.0, -0.4)), duo.stage.side(), since, scale, fade, 0);
            drops(painter, duo.bPlace.at(new Vec3(0.0, 3.0, -0.4)), duo.stage.side().scale(-1.0), since, scale, fade,
                    DROPS);
        }
        double clap = clock - HIGH_FIVE;
        double flash = strength * Ease.smooth((clap + 0.3) / 0.3) * (1.0 - Ease.smooth(clap / 6.0));
        if (flash > 0.01) {
            Vec3 at = duo.aPlace.at(PALM_AT).lerp(duo.bPlace.at(PALM_AT), 0.5);
            painter.flare(at, 3.0 * scale * (0.5 + 0.5 * flash), flash);
            Vec3 side = duo.stage.side();
            Vec3[] across = Vectors.across(side);
            double u = Ease.smooth(clap / 5.0);
            painter.circle(at, across[0], across[1], (0.6 + 3.0 * u) * scale, 0.08 * scale, 0.6 * scale,
                    Colors.alpha(0.9 * flash), Colors.alpha(0.4 * flash));
        }
        double salute = strength * Ease.smooth((clock - SALUTE + 1.0) / 2.0) * (1.0 - Ease.smooth((clock - SALUTE
                - 8.0) / 5.0));
        if (salute > 0.01) {
            twinkle(painter, HandPainter.indexTip(duo.aPose, duo.aPlace, false), 1.0 * scale, salute, clock);
            twinkle(painter, HandPainter.indexTip(duo.bPose, duo.bPlace, true), 1.0 * scale, salute, clock + 2.3);
        }
    }

    private static void drops(LanternPainter painter, Vec3 from, Vec3 out, double since, double scale, double fade,
            int seed) {
        for (int i = 0; i < DROPS; i++) {
            double spread = Noise.of(seed + i, 3, 1) - 0.5;
            Vec3 speed = out.scale(0.18 + 0.08 * Noise.of(seed + i, 3, 2)).add(0.0, 0.16 + 0.1 * spread, spread * 0.1)
                    .scale(scale);
            Vec3 at = from.add(speed.scale(since)).add(0.0, -0.02 * since * since * scale, 0.0);
            Vec3 back = at.subtract(speed.add(0.0, -0.04 * since * scale, 0.0).scale(1.5));
            painter.edge(back, at, 0.1 * scale, fade);
            painter.flare(at, 0.3 * scale, 0.6 * fade);
        }
    }

    private static void burst(LanternPainter painter, Vec3 at, double size, double strength, int seed) {
        painter.flare(at, size * (0.6 + 0.4 * strength), strength);
        Vec3 view = painter.camera().subtract(at);
        if (view.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] across = Vectors.across(view.normalize());
        double grow = 1.0 - strength;
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * (i + 0.4 * Noise.of(seed, i, 1)) / 6.0;
            Vec3 way = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)));
            painter.edge(at.add(way.scale(size * (0.3 + 0.8 * grow))), at.add(way.scale(size * (0.7 + 1.1 * grow))),
                    0.06 * size, strength);
        }
    }
}
