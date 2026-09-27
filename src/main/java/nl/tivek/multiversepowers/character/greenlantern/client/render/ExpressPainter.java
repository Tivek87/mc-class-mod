package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.ExpressScript;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.client.ExpressTrails;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.BOOM;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.STEAM;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.TIP;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ExpressShapes.AXLE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ExpressShapes.CRANK;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ExpressShapes.RAIL;

public final class ExpressPainter {
    private static final double GLOWS = 0.25;
    private static final double FLING = 2.4;
    private static final int PIECES = 60;
    private static final double RAM_SHAKE = 0.4;
    private static final double SLAM_SHAKE = 0.75;
    private static final double BOOM_SHAKE = 1.0;
    private static final double RUMBLE = 0.14;

    public record Gate(Vec3 center, Vec3 normal, double radius, double open, double shuts) {
    }

    private ExpressPainter() {
    }

    public static void draw(LanternPainter painter, ConstructPayload was, ConstructPayload now, float partialTick,
            @Nullable Gate gate) {
        ExpressTrails.Trail trail = ExpressTrails.of(now.id());
        if (trail == null) {
            return;
        }
        double s = Math.max(0.1, now.size());
        double o = Mth.lerp(partialTick, was.charge(), now.charge());
        double age = Mth.lerp(partialTick, was.age(), now.age());
        int side = ExpressScript.side(now.variant());
        boolean quiet = ExpressScript.quiet(now.variant());
        double tip = since(trail, TIP, age);
        double boom = since(trail, BOOM, age);
        double apart = boom < 0.0 ? -1.0 : Mth.clamp(boom / ExpressScript.BOOM_TICKS, 0.0, 1.0);
        Frame engine = car(trail, o - ExpressScript.BOGIE * s, o - ExpressScript.DRIVER * s, s, false, side, tip);
        Frame tender = car(trail, o - ExpressScript.TENDER_FRONT * s, o - ExpressScript.TENDER_BACK * s, s, true, side,
                tip - ExpressScript.TENDER_LAG);
        Vec3 middle = engine.at(0.0, ExpressShapes.BOILER_AXIS, -5.0);
        if (!painter.visible(middle, (ExpressScript.LENGTH * 0.7 + 4.0) * s)) {
            return;
        }
        double steam = since(trail, STEAM, age);
        double pressure = steam < 0.0 ? 0.0 : ExpressScript.pressure(steam);
        boolean clipped = gate != null && gate.open() > 0.01;
        if (clipped) {
            painter.clip(gate.center(), gate.normal(), HandPainter.PORTAL_SEAM);
        }
        painter.ambient(GLOWS);
        ExpressRails.draw(painter, trail, o, s, age, apart);
        if (apart >= 1.0) {
            painter.ambient(0.0);
            painter.noClip();
            ExpressLight.boom(painter, trail, engine, age, s, quiet);
            return;
        }
        painter.fling(FLING);
        cars(painter, engine, tender, o, s, apart, pressure, age);
        painter.fling(1.0);
        painter.ambient(0.0);
        if (clipped) {
            painter.noClip();
        }
        ExpressLight.lights(painter, trail, engine, tender, o, s, age, side, tip, pressure, apart);
        if (apart >= 0.0) {
            ExpressLight.boom(painter, trail, engine, age, s, quiet);
        }
    }

    private static Frame car(ExpressTrails.Trail trail, double front, double back, double s, boolean middle,
            int side, double tipSince) {
        Vec3 a = trail.at(front);
        Vec3 b = trail.at(back);
        Vec3 way = a.subtract(b);
        Frame frame = Frame.of(middle ? a.add(b).scale(0.5) : a, way.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 0.0, 1.0)
                : way, Vectors.UP, s);
        double roll = ExpressScript.roll(tipSince);
        if (roll <= 0.0) {
            return frame;
        }
        Frame fallen = frame.turned(side * ExpressScript.PIVOT_OUT, 0.0, 0.0, 0.0, 0.0, 1.0, -side * roll);
        double sink = ExpressScript.RAIL_LIFT * Math.min(1.0, roll / (Math.PI * 0.5));
        return new Frame(fallen.center().subtract(0.0, sink, 0.0), fallen.right(), fallen.up(), fallen.forward(), s);
    }

    private static void cars(LanternPainter painter, Frame engine, Frame tender, double o, double s, double apart,
            double pressure, double age) {
        HandPainter.part(painter, ExpressShapes.ENGINE, engine, 1.0, apart, 0);
        HandPainter.part(painter, ExpressShapes.TENDER, tender, 1.0, apart, PIECES * 9);
        Frame boiler = engine.moved(0.0, ExpressShapes.BOILER_AXIS, 0.0);
        if (pressure > 0.0 && apart < 0.0) {
            double swell = 1.0 + 0.05 * pressure * (0.6 + 0.4 * Math.sin(age * (0.7 + 1.8 * pressure)));
            boiler = boiler.stretched(swell, swell, 1.0);
            painter.glare(0.85 * pressure);
        }
        HandPainter.part(painter, ExpressShapes.BOILER, boiler, 1.0, apart, PIECES);
        painter.glare(0.0);
        double drivers = o / (ExpressScript.DRIVER_RADIUS * s);
        double leads = o / (ExpressShapes.LEAD_AXLE * s);
        double tenders = o / (ExpressShapes.TENDER_AXLE * s);
        int seed = PIECES * 2;
        for (int flank = -1; flank <= 1; flank += 2) {
            boolean right = flank > 0;
            double turn = drivers + (right ? 0.0 : Math.PI * 0.5);
            for (double z : new double[] { ExpressShapes.FRONT_DRIVER, ExpressShapes.REAR_DRIVER }) {
                wheel(painter, right ? ExpressShapes.DRIVER : ExpressShapes.DRIVER_LEFT, engine, flank * RAIL, AXLE,
                        z, turn, apart, seed += 20);
            }
            for (double z : ExpressShapes.LEAD_AXLES) {
                wheel(painter, right ? ExpressShapes.LEAD : ExpressShapes.LEAD_LEFT, engine, flank * RAIL,
                        ExpressShapes.LEAD_AXLE, z, leads + z, apart, seed += 20);
            }
            for (double z : ExpressShapes.TENDER_AXLES) {
                wheel(painter, right ? ExpressShapes.TENDER_WHEEL : ExpressShapes.TENDER_WHEEL_LEFT, tender,
                        flank * RAIL, ExpressShapes.TENDER_AXLE, z, tenders + z, apart, seed += 20);
            }
            double pinY = AXLE + CRANK * Math.cos(turn);
            double pinZ = ExpressShapes.REAR_DRIVER + CRANK * Math.sin(turn);
            double rise = AXLE - pinY;
            double reach = Math.sqrt(ExpressShapes.MAIN_ROD * ExpressShapes.MAIN_ROD - rise * rise);
            HandPainter.part(painter, ExpressShapes.SIDE_ROD, engine.moved(flank * ExpressShapes.SIDE_ROD_X, pinY, pinZ),
                    1.0, apart, seed += 20);
            HandPainter.part(painter, ExpressShapes.MAIN, engine.moved(flank * ExpressShapes.MAIN_ROD_X, pinY, pinZ)
                    .turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -Math.atan2(rise, reach)), 1.0, apart, seed += 20);
            HandPainter.part(painter, ExpressShapes.CROSSHEAD, engine.moved(flank * ExpressShapes.CROSSHEAD_X, AXLE,
                    pinZ + reach), 1.0, apart, seed += 20);
        }
    }

    private static void wheel(LanternPainter painter, Shape shape, Frame car, double x, double y, double z, double turn,
            double apart, int seed) {
        HandPainter.part(painter, shape, car.moved(x, y, z).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, turn), 1.0, apart,
                seed);
    }

    public static void gate(LanternPainter painter, ConstructPayload was, ConstructPayload now, float partialTick,
            @Nullable Vec3 ring) {
        double age = Mth.lerp(partialTick, was.age(), now.age());
        Gate gate = gateOf(was, now, partialTick);
        Vec3[] across = Vectors.across(gate.normal());
        HandDuo.Portal portal = new HandDuo.Portal(gate.center(), gate.normal(), across[0], across[1], gate.radius(),
                gate.open());
        int seed = now.id() * 5;
        HandPairLight.portal(painter, portal, seed, age, 1.0);
        HandPairLight.flashes(painter, new HandDuo.Portal(gate.center(), gate.normal(), across[0], across[1],
                gate.radius(), 1.0), age, 0.0, gate.shuts() < 0.0 ? Double.MAX_VALUE : gate.shuts()
                        + ExpressScript.OPEN_TICKS, 1.0);
        if (ring != null && age < ExpressScript.OPEN_TICKS + 4.0) {
            double fade = 1.0 - Ease.smooth((age - ExpressScript.OPEN_TICKS) / 4.0);
            painter.beamOfLight(ring, gate.center(), fade, age, 0.7);
        }
    }

    public static Gate gateOf(ConstructPayload was, ConstructPayload now, float partialTick) {
        return new Gate(now.center(), now.facing().normalize(), now.size(),
                Mth.clamp(Mth.lerp(partialTick, was.solid(), now.solid()), 0.0, 1.0), now.charge());
    }

    public static float shake(ConstructPayload now, double age, Vec3 from) {
        ExpressTrails.Trail trail = ExpressTrails.of(now.id());
        if (trail == null) {
            return 0.0F;
        }
        double s = Math.max(0.1, now.size());
        Vec3 middle = trail.at(now.charge() - ExpressScript.BOILER_BACK * s);
        double most = 0.0;
        double near = 1.0 - from.distanceTo(middle) / 40.0;
        int phase = ExpressScript.phase(now.variant());
        if (near > 0.0 && phase <= ExpressScript.BRAKE && age > ExpressScript.ROLLS) {
            double pace = Math.min(1.0, ExpressScript.speedAt(age - ExpressScript.ROLLS) / ExpressScript.TOP_SPEED);
            most = RUMBLE * (0.4 + 0.6 * pace) * Math.min(1.0, near * 1.5);
        }
        most = Math.max(most, jolt(age - trail.ramAt(), 7.0, RAM_SHAKE, near));
        if (trail.since(TIP) != Integer.MIN_VALUE) {
            most = Math.max(most, jolt(age - trail.since(TIP) - ExpressScript.TIP_TICKS, 12.0, SLAM_SHAKE, near));
        }
        if (trail.since(BOOM) != Integer.MIN_VALUE && !ExpressScript.quiet(now.variant())) {
            double far = 1.0 - from.distanceTo(middle) / 70.0;
            most = Math.max(most, jolt(age - trail.since(BOOM), 26.0, BOOM_SHAKE, far));
        }
        return (float) Math.min(1.0, most);
    }

    private static double jolt(double since, double ticks, double hard, double near) {
        if (since < 0.0 || since >= ticks || near <= 0.0) {
            return 0.0;
        }
        double fade = 1.0 - since / ticks;
        return hard * fade * fade * Math.min(1.0, near * 1.3);
    }

    static double since(ExpressTrails.Trail trail, int phase, double age) {
        int from = trail.since(phase);
        return from == Integer.MIN_VALUE ? -1.0 : Math.max(0.0, age - from);
    }
}
