package nl.tivek.multiversepowers.character.greenlantern.client.render.express;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.client.express.ExpressTrails;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternBeams;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.express.ExpressPainter.since;
import static nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript.BOOM;
import static nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript.BRAKE;
import static nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript.SLIDE;
import static nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript.STEAM;

final class ExpressLight {
    private static final int CRACKS = 9;
    private static final int SPECKS = 70;
    private static final double BEAM_REACH = 9.0;
    private static final int PUFFS = 7;

    private ExpressLight() {
    }

    static void lights(LanternPainter painter, ExpressTrails.Trail trail, Frame engine, Frame tender, double o,
            double s, double age, int side, double tip, double pressure, double apart) {
        if (apart >= 0.0) {
            return;
        }
        double upright = tip < 0.0 ? 1.0 : 1.0 - Ease.smooth(tip / 5.0);
        headlamp(painter, engine, s, upright, age);
        firebox(painter, engine, s, age);
        double brake = since(trail, BRAKE, age);
        double slide = since(trail, SLIDE, age);
        double steam = since(trail, STEAM, age);
        if (brake < 0.0 && age > ExpressScript.ROLLS && o > ExpressScript.FUNNEL_BACK * s) {
            chuff(painter, trail, engine, o, s);
        }
        if (brake >= 0.0 && slide < 0.0) {
            for (double z : new double[] { ExpressShapes.FRONT_DRIVER, ExpressShapes.REAR_DRIVER }) {
                sparks(painter, engine, ExpressShapes.RAIL, z, s, age, 5, (int) z);
                sparks(painter, engine, -ExpressShapes.RAIL, z, s, age, 5, (int) z + 7);
            }
        }
        if (slide >= 0.0 && steam < 0.0) {
            double fading = 1.0 - Ease.smooth((slide - 10.0) / 8.0);
            for (int k = 0; k < 6; k++) {
                double z = 1.5 - k * 1.9;
                sparks(painter, k < 5 ? engine : tender, side * ExpressScript.PIVOT_OUT, k < 5 ? z : 0.0, s, age,
                        (int) Math.round(7 * fading), k * 13);
            }
        }
        if (pressure > 0.0) {
            cracks(painter, engine, s, age, pressure);
        }
    }

    // Lamplight in the coaches' windows, the tail lamps, and sparks off their wheels as the brakes lock and off their
    // sides as they scrape along on them.
    static void coaches(LanternPainter painter, ExpressTrails.Trail trail, Frame[] coaches, double[] burst, double s,
            double age, int side, double tip, double slide) {
        double brake = since(trail, BRAKE, age);
        for (int k = 0; k < coaches.length; k++) {
            if (burst[k] >= 0.0) {
                continue;
            }
            Frame coach = coaches[k];
            for (int flank = -1; flank <= 1; flank += 2) {
                for (int w = 0; w < ExpressCoaches.WINDOWS.length; w++) {
                    double flicker = 0.8 + 0.2 * Math.sin(age * 0.6 + k * 2.1 + w * 1.3 + flank);
                    Vec3 pane = coach.at(flank * 1.32, ExpressCoaches.WINDOW_Y, ExpressCoaches.WINDOWS[w]);
                    painter.flare(pane, 0.22 * s, 0.32 * flicker);
                }
            }
            if (k == coaches.length - 1) {
                for (int flank = -1; flank <= 1; flank += 2) {
                    Vec3 lamp = coach.at(flank * ExpressCoaches.TAIL_LAMP.x, ExpressCoaches.TAIL_LAMP.y,
                            ExpressCoaches.TAIL_LAMP.z);
                    painter.flare(lamp, 0.35 * s, 0.85 + 0.15 * Math.sin(age * 0.8));
                }
                Vec3 drum = coach.at(ExpressCoaches.DRUMHEAD.x, ExpressCoaches.DRUMHEAD.y,
                        ExpressCoaches.DRUMHEAD.z - 0.1);
                painter.glowDisc(drum, 0.8 * s, LanternBeams.GREEN, 0.3, 0.2, k);
            }
            if (brake >= 0.0 && slide < 0.0) {
                for (double z : new double[] { ExpressScript.COACH_BOGIE, -ExpressScript.COACH_BOGIE }) {
                    sparks(painter, coach, ExpressShapes.RAIL, z, s, age, 3, k * 31 + (int) z);
                    sparks(painter, coach, -ExpressShapes.RAIL, z, s, age, 3, k * 31 + (int) z + 7);
                }
            }
            if (slide >= 0.0 && since(trail, STEAM, age) < 0.0) {
                double fading = 1.0 - Ease.smooth((slide - 10.0 - k * 2.0) / 8.0);
                int zig = k % 2 == 0 ? -side : side;
                for (double z : new double[] { 3.5, 0.0, -3.5 }) {
                    sparks(painter, coach, zig * ExpressScript.PIVOT_OUT, z, s, age, (int) Math.round(5 * fading),
                            k * 17 + (int) z);
                }
            }
        }
    }

    private static void headlamp(LanternPainter painter, Frame engine, double s, double on, double age) {
        if (on <= 0.01) {
            return;
        }
        Vec3 lens = engine.at(ExpressShapes.LENS.x, ExpressShapes.LENS.y, ExpressShapes.LENS.z + 0.02);
        Vec3 ahead = engine.forward().normalize();
        double flicker = 0.92 + 0.08 * Math.sin(age * 1.9);
        painter.glowTaper(lens, lens.add(ahead.scale(BEAM_REACH * s)), 0.45 * s, 3.4 * s, LanternBeams.GREEN,
                0.22 * on * flicker, 0.0);
        painter.lightTaper(lens, lens.add(ahead.scale(BEAM_REACH * 0.5 * s)), 0.2 * s, 1.1 * s, LanternBeams.HOT,
                0.2 * on, 0.0);
        painter.flare(lens, 0.45 * s, 0.95 * on);
    }

    private static void firebox(LanternPainter painter, Frame engine, double s, double age) {
        Vec3 door = engine.at(ExpressShapes.FIRE_DOOR.x, ExpressShapes.FIRE_DOOR.y, ExpressShapes.FIRE_DOOR.z);
        double flicker = 0.75 + 0.25 * Math.sin(age * 1.7) * Math.sin(age * 0.9 + 1.0);
        painter.flare(door, 0.3 * s, 0.75 * flicker);
        painter.glowDisc(engine.at(0.0, 2.9, -7.2), 1.1 * s, LanternBeams.GREEN, 0.3 * flicker, 0.4, 5);
    }

    private static void chuff(LanternPainter painter, ExpressTrails.Trail trail, Frame engine, double o, double s) {
        double since = Mth.frac(o / ExpressScript.CHUFF_BLOCKS);
        double pulse = Math.exp(-5.0 * since);
        Vec3 top = engine.at(ExpressShapes.FUNNEL_TOP.x, ExpressShapes.FUNNEL_TOP.y + 0.25 + 0.9 * since,
                ExpressShapes.FUNNEL_TOP.z);
        painter.glowDisc(top, 0.75 * s * (1.0 + 0.8 * since), LanternBeams.GREEN, 0.5 * pulse, 0.35,
                (int) (o / ExpressScript.CHUFF_BLOCKS));
        painter.flare(engine.at(ExpressShapes.FUNNEL_TOP.x, ExpressShapes.FUNNEL_TOP.y, ExpressShapes.FUNNEL_TOP.z),
                0.35 * s, 0.4 * pulse);
        // The puffs of the last few beats hang in the air where the funnel threw them, rising, swelling and thinning.
        int beat = (int) Math.floor(o / ExpressScript.CHUFF_BLOCKS);
        for (int k = 1; k <= PUFFS; k++) {
            double thrown = (beat - k + 1) * ExpressScript.CHUFF_BLOCKS;
            double age = (o - thrown) / ExpressScript.CHUFF_BLOCKS;
            if (thrown < ExpressScript.FUNNEL_BACK * s) {
                break;
            }
            double left = 1.0 - age / (PUFFS + 1.0);
            Vec3 at = trail.at(thrown - ExpressScript.FUNNEL_BACK * s)
                    .add(0.0, (ExpressShapes.FUNNEL_TOP.y + 0.6 + 0.55 * age) * s, 0.0);
            painter.glowDisc(at, (0.8 + 0.35 * age) * s, LanternBeams.GREEN, 0.28 * left * left, 0.45,
                    beat - k);
        }
    }

    private static void sparks(LanternPainter painter, Frame car, double x, double z, double s, double age,
            int count, int seed) {
        int tick = (int) age;
        Vec3 contact = car.at(x, 0.02, z);
        Vec3 back = car.forward().normalize().scale(-1.0);
        Vec3 out = car.right().normalize().scale(Math.signum(x));
        for (int k = 0; k < count; k++) {
            double run = Noise.of(seed, tick, k);
            Vec3 way = back.scale(0.6 + 0.8 * run).add(out.scale(0.4 * Noise.of(seed, tick, k + 40) - 0.1))
                    .add(0.0, 0.3 + 0.9 * Noise.of(seed, tick, k + 80), 0.0);
            double length = (0.35 + 0.6 * Noise.of(seed, tick, k + 120)) * s;
            Vec3 from = contact.add(way.scale(0.3 * s * Noise.of(seed, tick, k + 160)));
            painter.lightLine(from, from.add(way.normalize().scale(length)), 0.035 * s, LanternBeams.HOT,
                    Colors.alpha(0.95));
            painter.glowLine(from, from.add(way.normalize().scale(length)), 0.14 * s, LanternBeams.GREEN,
                    Colors.alpha(0.5));
        }
        painter.flare(contact, 0.3 * s, 0.7);
    }

    private static void cracks(LanternPainter painter, Frame engine, double s, double age, double pressure) {
        double strength = Math.pow(pressure, 1.4) * (0.75 + 0.25 * Math.sin(age * (1.0 + 2.5 * pressure)));
        double radius = ExpressShapes.BOILER_RADIUS + 0.03;
        for (int k = 0; k < CRACKS; k++) {
            double shown = Mth.clamp(pressure * 1.4 - k * 0.07, 0.0, 1.0);
            if (shown <= 0.0) {
                continue;
            }
            double angle = Noise.of(k, 3, 1) * Math.PI * 2.0;
            double z = 0.0 - Noise.of(k, 3, 2) * 5.6;
            Vec3 last = onBoiler(engine, radius, angle, z);
            for (int step = 1; step <= 4; step++) {
                if (step > shown * 4.0 + 0.5) {
                    break;
                }
                angle += (Noise.of(k, step, 4) - 0.5) * 0.7;
                z -= 0.25 + 0.3 * Noise.of(k, step, 5);
                Vec3 next = onBoiler(engine, radius, angle, z);
                painter.lightLine(last, next, 0.05 * s, LanternBeams.HOT, Colors.alpha(strength));
                painter.glowLine(last, next, 0.3 * s, LanternBeams.GREEN, Colors.alpha(0.55 * strength));
                last = next;
            }
        }
        Vec3 middle = engine.at(ExpressShapes.BOILER_MIDDLE.x, ExpressShapes.BOILER_MIDDLE.y,
                ExpressShapes.BOILER_MIDDLE.z);
        double breath = 1.0 + 0.2 * Math.sin(age * 1.3);
        painter.haze(middle, engine.right().normalize().scale(1.8 * s * breath),
                engine.up().normalize().scale(1.8 * s * breath), engine.forward().normalize().scale(4.2 * s * breath),
                painter.material().glow(), 0.22 * pressure);
    }

    private static Vec3 onBoiler(Frame engine, double radius, double angle, double z) {
        return engine.at(Math.sin(angle) * radius, ExpressShapes.BOILER_AXIS + Math.cos(angle) * radius, z);
    }

    static void boom(LanternPainter painter, ExpressTrails.Trail trail, Frame engine, double age, double s,
            boolean quiet) {
        double since = since(trail, BOOM, age);
        if (since < 0.0) {
            return;
        }
        Vec3 middle = engine.at(ExpressShapes.BOILER_MIDDLE.x, ExpressShapes.BOILER_MIDDLE.y,
                ExpressShapes.BOILER_MIDDLE.z);
        if (!quiet) {
            double reach = blastRadius();
            if (since < 9.0) {
                double flash = 1.0 - since / 9.0;
                painter.flare(middle, (3.0 + 6.0 * (1.0 - flash)) * s, flash);
            }
            double ball = Ease.smooth(since / 10.0);
            double gone = 1.0 - Ease.smooth(since / 15.0);
            if (gone > 0.0) {
                double r = (1.5 + 4.0 * ball) * s;
                painter.haze(middle, new Vec3(r, 0.0, 0.0), new Vec3(0.0, r * 0.8, 0.0), new Vec3(0.0, 0.0, r),
                        painter.material().glow(), 0.85 * gone);
                painter.haze(middle, new Vec3(r * 0.5, 0.0, 0.0), new Vec3(0.0, r * 0.45, 0.0),
                        new Vec3(0.0, 0.0, r * 0.5), LanternBeams.HOT, 0.6 * gone);
            }
            double front = Ease.smooth(since / 9.0);
            if (since < 12.0) {
                painter.shell(middle, reach * 1.15 * front, painter.material().glow(), 0.9 * (1.0 - since / 12.0));
                Vec3 ground = new Vec3(middle.x, middle.y - 1.4 * s, middle.z);
                painter.circle(ground, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), reach * 1.3 * front,
                        0.14 * s, 0.8 * s, Colors.alpha(0.95 * (1.0 - since / 12.0)),
                        Colors.alpha(0.5 * (1.0 - since / 12.0)));
            }
        }
        double apart = Mth.clamp(since / ExpressScript.BOOM_TICKS, 0.0, 1.0);
        double shown = Math.sin(Math.PI * Mth.clamp((apart - 0.35) / 0.65, 0.0, 1.0));
        if (shown <= 0.01) {
            return;
        }
        for (int k = 0; k < SPECKS; k++) {
            Vec3 way = Noise.direction(k, 31);
            double out = (1.5 + 5.5 * Noise.of(k, 31, 5)) * s * (0.35 + 0.65 * apart);
            Vec3 at = middle.add(way.x * out, Math.abs(way.y) * out * 0.7 + 1.5 * apart * s, way.z * out);
            painter.flare(at, 0.13 * s, shown * (0.6 + 0.4 * Noise.of(k, 31, 7)));
        }
    }

    private static double blastRadius() {
        CharacterAbility train = GameCharacter.GREEN_LANTERN.byName("emerald_express");
        return train == null ? 7.0 : train.value("blastRadius");
    }
}
