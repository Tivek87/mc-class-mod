package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.RingScan;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneGuns.Guns;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneGuns.GUNS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter.GROW_FROM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter.missile;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.BARRELS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.BAY_DOOR;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.BAY_HALF_ANGLE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.BAY_INSIDE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.DOOR_OPEN;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.GUN;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.MISSILE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.MISSILE_SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.PROPELLER;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.PROP_RADIUS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.hull;

/**
 * The plane in flight (see {@link PlanePainter}): its bomb bay and the missiles dropping out of it, its propellers,
 * its lights, the flames of its engines, its scan and the engine that bursts; and its moving parts breaking up with
 * it.
 */
final class PlaneParts {
    // How fast the propellers turn at full speed, in radians per tick, and how long the one that bursts takes to run
    // down, in ticks.
    private static final double PROP_SPIN = 0.55;
    private static final double PROP_DIES = 12.0;
    // How far to its right the hubs of its four propellers are: the left outer one, the left inner one, the right inner
    // one (the one that bursts) and the right outer one.
    private static final double[] ENGINES = { -AirStrike.ENGINE_OUTER_X, -AirStrike.ENGINE_X, AirStrike.ENGINE_X,
            AirStrike.ENGINE_OUTER_X };
    // A big missile's motor fires this many ticks after it dropped at the earliest: until then it only falls.
    private static final int EARLIEST_IGNITION = 4;
    // Where a missile waits in the bay, how high in the body, before it is lowered out.
    private static final double IN_BAY_Y = -1.95;

    private PlaneParts() {
    }

    /** How many ticks there are between two missiles out of the hatch, as the world's settings have it. */
    private static int missileEvery() {
        CharacterAbility strike = GameCharacter.GREEN_LANTERN.byName("air_strike");
        return Math.max(4, strike == null ? 40 : strike.intValue("missileTicks"));
    }

    /**
     * The bomb bay: its two doors swing open on their hinges (and the dark hollow behind them shows) before a missile
     * drops, the missile is lowered out of it, and the doors swing shut again once it is clear.
     */
    static void hatch(LanternPainter painter, PlanePath path, ConstructPainter.Frame frame, double t) {
        int every = missileEvery();
        double open = path.hatch(t, every);
        if (open > 0.0) {
            painter.shape(BAY_INSIDE, frame, 1.0, 1.0);
        }
        // Each door turns about its hinge, where it meets the body, outwards and down.
        double[] hull = hull(AirStrike.BAY_Z);
        double hinge = Math.toRadians(270.0 + BAY_HALF_ANGLE);
        double hingeX = hull[1] * Math.cos(hinge);
        double hingeY = hull[3] + hull[2] * Math.sin(hinge);
        double swing = DOOR_OPEN * Ease.smoother(open);
        for (int side = -1; side <= 1; side += 2) {
            ConstructPainter.Frame door = side > 0 ? frame : frame.stretched(-1.0, 1.0, 1.0);
            painter.shape(BAY_DOOR, door.turned(hingeX, hingeY, 0.0, 0.0, 0.0, 1.0, swing), 1.0, 1.0);
        }
        double lowered = path.lowered(t, every);
        if (open > 0.0 && lowered >= 0.0) {
            double y = Mth.lerp(lowered, IN_BAY_Y, AirStrike.DROP_Y);
            ConstructPainter.Frame missile = new ConstructPainter.Frame(frame.at(0.0, y, AirStrike.BAY_Z),
                    frame.right(), frame.up(), frame.forward(), frame.scale() * MISSILE_SCALE);
            painter.shape(MISSILE, missile, 1.0, 1.0);
        }
    }

    /**
     * The big missiles that just dropped out of the hatch, falling away under the plane with their motors dead, the way
     * the server moves them (see {@link PlanePath#fall}), from the very spot they hung in the hatch: until each one's
     * motor fires or it strikes, when it is drawn as a missile of its own (see {@link ClientConstructs#launch}).
     */
    static void dropping(LanternPainter painter, int owner, PlanePath path, double t, float partialTick) {
        int every = missileEvery();
        for (int release = path.lastRelease(t, every); release >= 0 && t - release <= AirStrike.IGNITE_LATEST + 2;
                release -= every) {
            ClientConstructs.Launch launch = ClientConstructs.launch(owner, AirStrike.BIG_MISSILE, release,
                    partialTick);
            double since = launch == null ? t - release : launch.since();
            double until = launch == null ? EARLIEST_IGNITION : launch.leaves();
            if (since >= 0.0 && since <= until) {
                Vec3[] state = falling(path.dropsOut(release), false, since);
                missile(painter, false, state[0], state[1], state[2], -1.0, -1.0);
            }
            if (release - every < PlanePath.FORM + PlanePath.MISSILE_FIRST) {
                break;
            }
        }
    }

    /**
     * A missile let go of {@code since} ticks ago, still falling with its motor dead (see {@link PlanePath#fall}), from
     * how it was let go: {where it is, its nose, its up}, smooth between the ticks.
     */
    static Vec3[] falling(Vec3[] released, boolean small, double since) {
        int ticks = (int) Math.floor(Math.max(0.0, since));
        Vec3[] state = released;
        for (int k = 0; k < ticks; k++) {
            state = PlanePath.fall(state, small);
        }
        Vec3[] next = PlanePath.fall(state, small);
        double u = Mth.clamp(since - ticks, 0.0, 1.0);
        Vec3 nose = state[2].lerp(next[2], u).normalize();
        return new Vec3[] { state[0].lerp(next[0], u), nose, PlanePath.carried(state[3].lerp(next[3], u), nose) };
    }

    /**
     * The four propellers, spinning up as it takes shape. The one that bursts runs down and stands still; the others
     * roar on into the ground.
     */
    static void propellers(LanternPainter painter, PlanePath path, ConstructPainter.Frame frame, double t,
            double grown) {
        double failed = path.failTick();
        for (int e = 0; e < ENGINES.length; e++) {
            double speed = PROP_SPIN * Ease.smooth((t - GROW_FROM) / 40.0);
            if (e == 2 && t > failed) {
                speed = PROP_SPIN * Math.exp(-(t - failed) / PROP_DIES);
            }
            ConstructPainter.Frame hub = frame.moved(ENGINES[e], AirStrike.ENGINE_Y, AirStrike.ENGINE_Z);
            painter.shape(PROPELLER, hub.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, propeller(path, e, t)), 1.0, 1.0);
            // Turning fast, the air round its blades shimmers: a faint ring of light where their tips run.
            double blur = Mth.clamp(speed / PROP_SPIN, 0.0, 1.0) * grown;
            if (blur > 0.05) {
                Vec3 middle = hub.at(0.0, 0.0, 0.62);
                double radius = PROP_RADIUS * frame.scale();
                painter.circle(middle, frame.right(), frame.up(), radius, 0.05 * radius, 0.35 * radius,
                        Colors.alpha(0.3 * blur), Colors.alpha(0.12 * blur));
            }
        }
    }

    /** How far the propellers have turned {@code t} ticks after the call, in radians. */
    private static double spun(double t) {
        return PROP_SPIN * Math.max(0.0, t - GROW_FROM) * Ease.smooth((t - GROW_FROM) / 40.0);
    }

    /**
     * How far propeller {@code e} (see {@link #ENGINES}) is turned about its hub {@code t} ticks after the call, in
     * radians: the one that bursts runs down after the burst and stands still, and the left ones turn the other way
     * round, as they would on a real plane.
     */
    private static double propeller(PlanePath path, int e, double t) {
        double failed = path.failTick();
        double spun = spun(t);
        if (e == 2 && t > failed) {
            spun = spun(failed) + PROP_SPIN * PROP_DIES * (1.0 - Math.exp(-(t - failed) / PROP_DIES));
        }
        return (e < 2 ? -spun : spun) + e * 0.7;
    }

    /**
     * The plane's own moving parts breaking up with it (see {@link ConstructPainter#shattered}), as they were when it
     * broke: its propellers, its miniguns as they pointed, and the doors of its hatch.
     */
    static void partsBroken(LanternPainter painter, int owner, PlanePath path, ConstructPainter.Frame frame,
            double t, double apart, double bright) {
        for (int e = 0; e < ENGINES.length; e++) {
            painter.shattered(PROPELLER, frame.moved(ENGINES[e], AirStrike.ENGINE_Y, AirStrike.ENGINE_Z)
                    .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, propeller(path, e, t)), apart, bright);
        }
        Guns guns = GUNS.get(owner);
        for (int gun = 0; gun < 2; gun++) {
            double side = gun == 0 ? -1.0 : 1.0;
            Vec3 aim = guns != null && guns.drawn[gun] != null ? guns.drawn[gun] : path.gunRest(gun, t);
            Vec3 up = guns != null && guns.up[gun] != null ? guns.up[gun] : frame.up();
            ConstructPainter.Frame mount = ConstructPainter.Frame.of(frame.at(side * AirStrike.GUN_X, AirStrike.GUN_Y,
                    AirStrike.GUN_Z), aim, up, frame.scale());
            painter.shattered(GUN, mount, apart, bright);
            double turned = guns == null ? 0.0 : guns.turned[gun];
            painter.shattered(BARRELS, mount.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, turned), apart, bright);
        }
        double[] hull = hull(AirStrike.BAY_Z);
        double hinge = Math.toRadians(270.0 + BAY_HALF_ANGLE);
        double swing = DOOR_OPEN * Ease.smoother(path.hatch(t, missileEvery()));
        for (int side = -1; side <= 1; side += 2) {
            ConstructPainter.Frame door = side > 0 ? frame : frame.stretched(-1.0, 1.0, 1.0);
            painter.shattered(BAY_DOOR, door.turned(hull[1] * Math.cos(hinge), hull[3] + hull[2] * Math.sin(hinge), 0.0,
                    0.0, 0.0, 1.0, swing), apart, bright);
        }
    }

    /**
     * Lights on its wingtips and tail blinking in turn, beacons on its back and belly flashing, its landing lights
     * burning, and a faint glow on the tips of its wings.
     */
    static void lights(LanternPainter painter, ConstructPainter.Frame frame, double t, double down) {
        double blink = Math.max(0.0, 1.0 - Mth.frac(t / 30.0) * 6.0);
        double tail = Math.max(0.0, 1.0 - Mth.frac(t / 30.0 + 0.5) * 6.0);
        double beacon = Math.max(0.0, 1.0 - Mth.frac(t / 18.0 + 0.25) * 4.0);
        double s = frame.scale();
        for (int side = -1; side <= 1; side += 2) {
            painter.flare(frame.at(side * 28.0, 3.45, 0.4), (0.6 + 1.6 * blink) * s, 0.4 + 0.6 * blink);
            painter.flare(frame.at(side * 0.85, -2.3, 17.0), 1.3 * s, 0.7);
        }
        painter.flare(frame.at(0.0, 2.45, -22.75), (0.5 + 1.4 * tail) * s, 0.35 + 0.6 * tail);
        painter.flare(frame.at(0.0, -2.75, 7.0), (0.4 + 2.2 * beacon) * s, 0.3 + 0.7 * beacon);
        painter.flare(frame.at(0.0, 3.5, 1.5), (0.4 + 1.8 * beacon) * s, 0.3 + 0.7 * beacon);
        if (down > 0.0) {
            // Going down, the tips of its wings trail streaks of light.
            for (int side = -1; side <= 1; side += 2) {
                Vec3 tip = frame.at(side * 28.0, 3.45, 0.4);
                painter.edge(tip, tip.subtract(frame.forward().scale(8.0 * down * s)), 0.3 * s, 0.6 * down);
            }
        }
    }

    /** Every engine breathes a faint flame of light out of its exhaust stack; the one that burst only burns. */
    static void engines(LanternPainter painter, PlanePath path, ConstructPainter.Frame frame, double t,
            double grown) {
        double power = 0.55 * Ease.smooth((t - GROW_FROM - 10.0) / 20.0) * Math.min(1.0, grown);
        if (power <= 0.01) {
            return;
        }
        double failed = path.failTick();
        Vec3 back = frame.forward().scale(-1.0);
        double s = frame.scale();
        double[] xs = { -AirStrike.ENGINE_OUTER_X, -AirStrike.ENGINE_X, AirStrike.ENGINE_X, AirStrike.ENGINE_OUTER_X };
        for (int e = 0; e < xs.length; e++) {
            if (e == 2 && t > failed) {
                continue;
            }
            Vec3 stack = frame.at(xs[e] + Math.signum(xs[e]) * 1.2, AirStrike.ENGINE_Y + 0.42, -0.75);
            painter.exhaust(stack, back, 2.6 * s, 0.22 * s, power);
        }
    }

    /**
     * While its sensor scans, a cone of light shines out of the ball under its nose down onto the ground, where the wave
     * of the scan rolls out.
     */
    static void scanCone(LanternPainter painter, int owner, ConstructPainter.Frame frame, float partialTick) {
        Vec3 sensor = frame.at(0.0, AirStrike.SENSOR_Y, AirStrike.SENSOR_Z);
        for (ClientConstructs.Scan scan : ClientConstructs.scans(partialTick)) {
            if (scan.owner() != owner || !scan.hostileOnly()) {
                continue;
            }
            double rolling = scan.radius() / RingScan.SPEED;
            double clock = scan.reached() / RingScan.SPEED;
            double strength = (1.0 - Ease.smooth((clock - rolling * 0.7) / (rolling * 0.3)))
                    * Ease.smooth(clock / 3.0);
            if (strength <= 0.01) {
                continue;
            }
            painter.flare(sensor, 1.4 * frame.scale(), strength);
            int lines = 16;
            double reach = Math.max(1.0, scan.reached());
            for (int k = 0; k < lines; k++) {
                double angle = Math.PI * 2.0 * k / lines + painter.time() * 0.02;
                Vec3 foot = scan.center().add(Math.cos(angle) * reach, 0.1, Math.sin(angle) * reach);
                painter.edge(sensor, foot, 0.12, 0.35 * strength);
            }
            painter.edge(sensor, scan.center(), 0.5, 0.6 * strength);
        }
    }

    /**
     * An engine bursts: a flash and a spray of sparks, and from then on flames of light blow back out of it and a trail
     * of light and sparks follows the plane down.
     */
    static void burning(LanternPainter painter, PlanePath path, ConstructPainter.Frame frame, double t) {
        double failed = path.failTick();
        double after = t - failed;
        double s = frame.scale();
        Vec3 engine = frame.at(AirStrike.ENGINE_X, AirStrike.ENGINE_Y, 1.0);
        if (after < 6.0) {
            double flash = 1.0 - after / 6.0;
            painter.flare(engine, 7.0 * flash * s, flash);
            for (int k = 0; k < 14; k++) {
                Vec3 way = Noise.direction(k, 223);
                painter.edge(engine.add(way.scale(after * 0.9 * s)), engine.add(way.scale((after * 0.9 + 2.2) * s)),
                        0.2 * s, flash);
            }
        }
        double flicker = 0.75 + 0.25 * Math.sin(t * 2.9) * Math.sin(t * 1.3 + 0.4);
        Vec3 back = frame.forward().scale(-1.0).add(frame.up().scale(0.25)).normalize();
        painter.exhaust(frame.at(AirStrike.ENGINE_X, AirStrike.ENGINE_Y + 0.4, -3.8), back, 9.0 * s, 1.2 * s,
                flicker);
        painter.exhaust(frame.at(AirStrike.ENGINE_X, AirStrike.ENGINE_Y + 0.9, 3.0), back, 5.0 * s, 0.8 * s,
                0.6 * flicker);
        // Its trail: light and sparks along where the burning engine was on the last ticks.
        Vec3 last = path.point(t, AirStrike.ENGINE_X, AirStrike.ENGINE_Y, -4.0);
        for (int k = 1; k <= 12; k++) {
            double then = Math.max(failed, t - 2.0 * k);
            Vec3 next = path.point(then, AirStrike.ENGINE_X, AirStrike.ENGINE_Y, -4.0);
            double fade = 1.0 - k / 13.0;
            painter.edge(last, next, 1.6 * fade * s, 0.8 * fade);
            last = next;
        }
        int flick = (int) (t / 2.0);
        for (int k = 0; k < 6; k++) {
            Vec3 from = frame.at((Noise.of(flick, k, 231) - 0.5) * 30.0, 2.0,
                    (Noise.of(flick, k, 232) - 0.5) * 30.0);
            painter.edge(from, from.add(Noise.direction(flick, 233 + k).scale(3.0 * s)), 0.2 * s, 0.9);
        }
    }
}
