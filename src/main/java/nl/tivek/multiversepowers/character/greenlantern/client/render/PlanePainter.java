package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneGuns.Guns;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneCrash.crashed;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneGuns.GUNS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneGuns.guns;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneJets.JET_SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneJets.jets;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.burning;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.dropping;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.engines;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.hatch;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.lights;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.partsBroken;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.propellers;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.scanCone;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.BODY;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.JET;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.MISSILE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.MISSILE_SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.MISSILE_TAIL;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.SLUG;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.SLUG_SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.SMALL_MISSILE_SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneSound.SOUNDS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneSound.sound;

/**
 * The air strike as everyone sees it (see {@link AirStrike}). A pillar of light shoots out of the ring into the sky and
 * a big gunship grows out of the top of it, white-hot at first and cooling to the green of hard light, high over the
 * battlefield. It is solid hard light like every construct, and made in detail:
 * <ul>
 * <li>a long, round body with a radome nose, a windscreen and side windows burning bright, a crew door, paratroop doors
 * and rows of windows, bands of plating, a cargo ramp under its upswept tail, landing gear pods on its sides, blade
 * antennas and pitot tubes;</li>
 * <li>a high wing on top of it with flaps and ailerons, four engines in long nacelles slung under it, each with an air
 * intake, an exhaust stack and a big four-bladed propeller turning in front of it;</li>
 * <li>a tall fin with a rudder and a dorsal fillet, tailplanes with elevators, and the lantern emblem on its fin and on
 * top of its wings, with lights on its wingtips and its tail;</li>
 * <li>two miniguns on ball mounts in sponsons on its sides, that swing smoothly round to what they fire at, their six
 * barrels spinning and their muzzles flashing as they fire;</li>
 * <li>a bomb bay in its belly, whose two doors swing open on their hinges before a missile is lowered out and drops,
 * and swing shut behind it;</li>
 * <li>a sensor ball under its nose, that shines its scan down onto the ground in a cone of light.</li>
 * </ul>
 * Two jets fly with it (see {@link PlaneJets}), sleek fighters with a bubble canopy, twin tails and a small missile under each
 * wing, that grow out of the ring's light beside it and race round it, banking into their turns. Then an engine bursts
 * into flame, its propeller dies and the plane shudders and struggles: its jets break away, their flames roaring, a cone
 * of mist forms round them as they break the sound barrier with a ring of light, and they are gone in a star of light,
 * breaking into solid pieces. The plane's nose drops: it plunges into the ground, rolling over, trailing fire and light,
 * and crashes in a blast like a small sun: a flash, a fireball rising on a stem of light into a mushroom cloud, a ring of
 * light racing out round its middle, a shell of light and rings running out over the ground, while the plane breaks into
 * solid pieces that are flung far. Only that blast is see-through: it is light, not a construct. If its maker stops being
 * Green Lantern the plane and its jets break apart in the air (see {@link #broken}).
 */
public final class PlanePainter {
    // How long the plane takes to grow out of the light, from when, in ticks after the call; and how long it takes to
    // break up once it has crashed (or its maker lets go of it).
    static final double GROW_FROM = 6.0;
    private static final double GROW_TICKS = 30.0;
    static final double BREAK_TICKS = 40.0;
    // How far its pieces are flung when it crashes, and when it breaks up in the air.
    static final double CRASH_FLING = 10.0;
    private static final double AIR_FLING = 5.0;
    // The pillar of light: how thick it is, and until when it pours up into the plane.
    private static final double PILLAR_THICK = 3.6;
    private static final double PILLAR_UNTIL = PlanePath.FORM + 4.0;
    /** How long the blast of a missile goes on, in ticks. */
    public static final int BLAST_TICKS = 30;
    /** How long a missile takes to break into pieces once it has struck, in ticks. */
    public static final double MISSILE_BREAKS = 12.0;
    // How long a round that struck nothing takes to break into crumbs once it is out of reach, in ticks.
    private static final double SLUG_BREAKS = 4.0;
    // How much light from within the plane's hard light gets on top of the sky's, so its belly high over you still reads.
    static final double GLOWS = 0.3;

    private PlanePainter() {
    }

    // ---- The plane ----

    /** The way the plane of this air strike flies, from what the server said about it. */
    public static PlanePath path(ConstructPayload plane) {
        return new PlanePath(plane.center(), plane.facing(), plane.size(), Math.round(plane.charge()),
                plane.variant() / 100.0);
    }

    /**
     * One air strike.
     *
     * @param clock ticks since it was called, by the client's own clock
     * @param ring  where its maker's ring is, or null when he is out of sight
     */
    public static void draw(LanternPainter painter, int id, ConstructPayload plane, double clock, @Nullable Vec3 ring,
            float partialTick) {
        PlanePath path = path(plane);
        double crash = path.crashTick();
        double t = Math.min(clock, crash);
        sound(id, path, t, clock < crash ? path.down(t) : -1.0);
        if (clock < crash) {
            flying(painter, id, plane.owner(), path, clock, ring, partialTick);
            dropping(painter, plane.owner(), path, clock, partialTick);
            jets(painter, plane.owner(), path, clock, ring, partialTick);
        } else {
            crashed(painter, plane.owner(), path, clock - crash);
        }
    }

    /** How far the plane has grown out of the light, 0 to a hair over 1. */
    private static double grown(double t) {
        return Ease.backOut((t - GROW_FROM) / GROW_TICKS);
    }

    /** Where the plane is and how it is turned at {@code t}, at the size it has grown to. */
    static ConstructPainter.Frame frame(PlanePath path, double t, double scale) {
        Vec3[] axes = path.axes(t);
        return new ConstructPainter.Frame(path.at(t), axes[0], axes[1], axes[2], scale);
    }

    private static void flying(LanternPainter painter, int id, int owner, PlanePath path, double t,
            @Nullable Vec3 ring, float partialTick) {
        double grown = grown(t);
        double down = path.down(t);
        ConstructPainter.Frame frame = frame(path, t, Math.max(grown, 1.0E-3));
        // The pillar of light pours up out of the ring into it while it takes shape; after that a thread of the ring's
        // light keeps hanging on it, as on every construct, and gives out as the plane struggles, before its nose
        // drops.
        if (ring != null) {
            if (t < PILLAR_UNTIL) {
                double fade = 1.0 - Ease.smooth((t - PlanePath.FORM) / 4.0);
                painter.beamOfLight(ring, path.at(t), fade, t, PILLAR_THICK);
            } else {
                double holds = 1.0 - Ease.smooth(path.failing(t) * 1.25);
                if (holds > 0.01) {
                    painter.beam(ring, frame.at(0.0, -2.6, 2.0), 0.55 * holds, 2.0);
                }
            }
        }
        // Its scan and the trail of its burning engine reach far out from it: they show even with the plane itself out
        // of view.
        scanCone(painter, owner, frame, partialTick);
        if (t >= path.failTick()) {
            burning(painter, path, frame, t);
        }
        if (grown <= 0.01 || !painter.visible(frame.center(), 40.0 * grown)) {
            return;
        }
        // White-hot as it grows out of the light, cooling to green; plunging down, its hull flickers, more and more.
        double flicker = Ease.smooth(down / 0.15)
                * (0.25 + 0.45 * down * Math.max(0.0, Math.sin(t * 1.7) * Math.sin(t * 0.63 + 2.0)));
        double hot = 0.6 * (1.0 - Ease.smooth((t - GROW_FROM) / GROW_TICKS));
        painter.glare(Math.max(hot, flicker));
        painter.ambient(GLOWS);
        painter.shape(BODY, frame, 1.0, 1.0 + 0.3 * flicker);
        propellers(painter, path, frame, t, grown);
        guns(painter, id, owner, path, frame, t, grown);
        hatch(painter, path, frame, t);
        painter.ambient(0.0);
        painter.glare(0.0);
        lights(painter, frame, t, down);
        engines(painter, path, frame, t, grown);
    }

    /**
     * A round one of the miniguns of this plane fired: from then on its gun swings to where the round says it is to
     * point next, just as it does on the server.
     */
    public static void fired(ConstructPayload plane, ConstructPayload round) {
        Vec3 next = round.facing();
        if (next.lengthSqr() < 1.0E-6 || round.variant() < 0 || round.variant() > 1) {
            return;
        }
        guns(round.owner(), plane.id(), path(plane)).turrets[round.variant()].fired(Math.round(round.charge()),
                next.normalize());
    }

    /**
     * A plane whose maker let go of it in the air: it breaks into solid pieces where it was, flung apart, and they tumble
     * down.
     *
     * @param since ticks since it was let go
     */
    public static void broken(LanternPainter painter, ConstructPayload plane, double clock, double since) {
        PlanePath path = path(plane);
        double t = Math.min(clock - since, path.crashTick());
        double apart = since / BREAK_TICKS;
        ConstructPainter.Frame frame = frame(path, t, Math.max(grown(t), 1.0E-3));
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 6.0));
        painter.fling(AIR_FLING);
        painter.shattered(BODY, frame, apart, 1.2);
        partsBroken(painter, plane.owner(), path, frame, t, apart, 1.2);
        // Its jets break up where they were too, with the missiles under their wings.
        painter.fling(2.5);
        for (int k = 0; k < PlanePath.JETS; k++) {
            if (!path.hasJet(k) || t < path.jetFrom(k) || path.jetsFled(t) >= PlanePath.JET_GONE) {
                continue;
            }
            Vec3[] axes = path.jetAxes(k, t);
            ConstructPainter.Frame jet = ConstructPainter.Frame.of(path.jetAt(k, t), axes[0], axes[1],
                    JET_SCALE * Math.max(path.jetGrown(k, t), 1.0E-3));
            painter.shattered(JET, jet, apart, 1.2);
            for (int side = -1; side <= 1; side += 2) {
                painter.shattered(MISSILE, new ConstructPainter.Frame(jet.at(side * AirStrike.PYLON_X,
                        AirStrike.PYLON_Y, AirStrike.PYLON_Z), jet.right(), jet.up(), jet.forward(),
                        jet.scale() * SMALL_MISSILE_SCALE), apart, 1.2);
            }
        }
        painter.fling(1.0);
        painter.glare(0.0);
    }

    // ---- What it fires ----

    /**
     * A missile of hard light, a big one out of the hatch or a jet's {@code small} one, its middle at {@code at}, its
     * nose along {@code nose} and its roll given by {@code up} (carried along as it swings, so it never flips over). A
     * big one falls with its motor dead until the motor bursts into life with a flash; a jet's small one lights its
     * motor almost at once. Burning ({@code burning} ticks since its motor fired, below 0 before), it streaks on
     * trailing its own flame and a long, thinning streak of light. Once it has struck ({@code broken} ticks ago, below
     * 0 before) it bursts into solid pieces flung out from where it was.
     */
    public static void missile(LanternPainter painter, boolean small, Vec3 at, Vec3 nose, Vec3 up, double burning,
            double broken) {
        double scale = small ? SMALL_MISSILE_SCALE : MISSILE_SCALE;
        Vec3 forward = nose.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : nose.normalize();
        ConstructPainter.Frame frame = ConstructPainter.Frame.of(at, forward, up, scale);
        if (broken >= 0.0) {
            // Fast at first, then slowing: out of the blast's flash.
            double apart = 1.0 - Math.pow(1.0 - Math.min(1.0, broken / MISSILE_BREAKS), 2.0);
            if (apart < 1.0) {
                painter.glare(Math.max(0.0, 0.8 - broken / 6.0));
                painter.fling(small ? 1.6 : 2.6);
                painter.shattered(MISSILE, frame, apart, 1.3);
                painter.fling(1.0);
                painter.glare(0.0);
            }
            return;
        }
        painter.ambient(GLOWS);
        painter.shape(MISSILE, frame, 1.0, 1.15);
        painter.ambient(0.0);
        if (burning < 0.0) {
            return;
        }
        Vec3 tail = frame.at(0.0, 0.0, MISSILE_TAIL);
        double lit = Ease.smooth(burning / 2.0);
        if (burning < 4.0) {
            // Its motor bursts into life.
            double flash = 1.0 - burning / 4.0;
            painter.flare(tail, (small ? 1.2 : 3.6) * flash + 0.6, flash);
        }
        painter.exhaust(tail, forward.scale(-1.0), small ? 2.6 : 6.0, small ? 0.2 : 0.5, lit);
        // A long streak of light behind it, thinning out.
        Vec3 last = tail;
        int streak = 9;
        double step = small ? 1.1 : 1.9;
        for (int k = 1; k <= streak; k++) {
            Vec3 next = tail.subtract(forward.scale(step * k));
            double fade = 1.0 - (double) k / (streak + 1);
            painter.edge(last, next, (small ? 0.35 : 0.75) * fade, 0.75 * fade * lit);
            last = next;
        }
    }

    /**
     * A round from a minigun: a solid slug of hard light with a long, bright tracer streak behind it, flying out of the
     * muzzle of its gun as that pointed the tick it fired (see {@link PlanePath.Turret}) to where it strikes, and
     * splashing there in a flash, a spray of sparks and a ripple of light. One that strikes nothing flies off into the
     * air and breaks into solid crumbs out of reach.
     *
     * @param since ticks since it was fired, by the plane's clock (below 0: not yet)
     */
    public static void bullet(LanternPainter painter, ConstructPayload round, double since) {
        Guns guns = GUNS.get(round.owner());
        int gun = round.variant();
        if (since < 0.0 || guns == null || gun < 0 || gun > 1) {
            return;
        }
        int fired = Math.round(round.charge());
        Vec3 to = round.center();
        Vec3 muzzle = guns.path.pivot(gun, fired).add(guns.turrets[gun].aim(fired).scale(AirStrike.GUN_LENGTH));
        double distance = muzzle.distanceTo(to);
        double travel = Math.max(1.0, round.size());
        Vec3 way = distance < 1.0E-6 ? Vectors.UP.scale(-1.0) : to.subtract(muzzle).scale(1.0 / distance);
        if (since < travel) {
            double u = since / travel;
            Vec3 head = muzzle.lerp(to, u);
            painter.shape(SLUG, ConstructPainter.Frame.of(head, way, Vectors.UP, SLUG_SCALE), 1.0, 1.3);
            Vec3 tail = head.subtract(way.scale(Math.min(7.0, distance * u)));
            painter.edge(tail, head, 0.3, 1.0);
            painter.edge(head.subtract(way.scale(Math.min(2.2, distance * u))), head, 0.55, 0.8);
            painter.flare(head, 0.7, 0.6);
            return;
        }
        double after = since - travel;
        if (round.held()) {
            if (after < SLUG_BREAKS) {
                painter.shattered(SLUG, ConstructPainter.Frame.of(to, way, Vectors.UP, SLUG_SCALE),
                        after / SLUG_BREAKS, 1.3);
            }
            return;
        }
        if (after > 8.0) {
            return;
        }
        double fade = 1.0 - after / 8.0;
        Vec3 hit = to.add(0.0, 0.15, 0.0);
        painter.flare(hit, 0.6 + 1.6 * fade, fade);
        painter.circle(to.add(0.0, 0.06, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0),
                0.25 + 1.4 * (1.0 - fade), 0.06, 0.4, Colors.alpha(fade), Colors.alpha(0.5 * fade));
        if (after < 4.0) {
            double sparks = 1.0 - after / 4.0;
            for (int k = 0; k < 6; k++) {
                Vec3 spark = Noise.direction(round.id(), 261 + k);
                spark = new Vec3(spark.x, Math.abs(spark.y) + 0.35, spark.z).normalize();
                Vec3 from = hit.add(spark.scale(0.3 + 1.3 * (1.0 - sparks)));
                painter.edge(from, from.add(spark.scale(0.7 * sparks)), 0.09, sparks);
            }
        }
    }

    /**
     * How long a round stays on screen, from when it was fired: its flight and its splash, in ticks, and a few more for
     * a round that is heard of late.
     */
    public static int bulletTicks(ConstructPayload round) {
        return Math.round(round.size()) + 14;
    }

    /**
     * The small blast of a missile, light and nothing else (the missile itself breaks into solid pieces on its own, see
     * {@link #missile}): a white-hot flash, a fireball of light that swells and climbs as it burns out, a ring of light
     * racing out over the ground and rays and sparks shooting out.
     *
     * @param since ticks since it burst (below 0: not yet, its missile is still on its way)
     */
    public static void missileBlast(LanternPainter painter, ConstructPayload blast, double since) {
        double life = 1.0 - since / BLAST_TICKS;
        if (since < 0.0 || life <= 0.0) {
            return;
        }
        Vec3 heart = blast.center();
        double reach = Math.max(1.0, blast.size());
        if (!painter.visible(heart, reach * 3.0)) {
            return;
        }
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        Vec3 up = Vectors.UP;
        // The flash.
        double flash = Math.max(0.0, 1.0 - since / 5.0);
        if (flash > 0.0) {
            painter.flare(heart, reach * (1.2 + 3.0 * flash), flash);
        }
        // The fireball: it swells fast, climbs a little and burns out.
        double swell = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 7.0), 3.0);
        double radius = reach * (0.3 + 0.75 * swell);
        Vec3 ball = heart.add(0.0, 0.35 * reach * Math.min(1.0, since / 20.0), 0.0);
        double burn = life * life;
        painter.haze(ball, east.scale(radius), up.scale(radius * 0.9), south.scale(radius), LanternPainter.GREEN,
                0.55 * burn);
        painter.haze(ball, east.scale(radius * 0.55), up.scale(radius * 0.5), south.scale(radius * 0.55),
                LanternPainter.HOT, 0.7 * burn * Math.max(0.0, 1.0 - since / 14.0));
        int shell = Colors.alpha(0.85 * burn);
        int haze = Colors.alpha(0.4 * burn);
        for (int k = -2; k <= 2; k++) {
            double lat = k / 2.5 * (Math.PI * 0.5);
            double turn = painter.time() * 0.03 + k;
            Vec3 a = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
            painter.circle(ball.add(0.0, radius * Math.sin(lat), 0.0), a, a.cross(up), radius * Math.cos(lat), 0.08,
                    0.7, shell, haze);
        }
        for (int k = 0; k < 3; k++) {
            double turn = k * Math.PI / 3.0 + painter.time() * 0.02;
            painter.circle(ball, new Vec3(Math.cos(turn), 0.0, Math.sin(turn)), up, radius, 0.06, 0.55, shell, haze);
        }
        painter.flare(ball, radius * 1.6, 0.6 * burn);
        // A ring of light racing out over the ground, and a second one after it.
        for (int k = 0; k < 2; k++) {
            double ring = since - k * 3.0;
            if (ring < 0.0) {
                continue;
            }
            double wave = 1.0 - Math.pow(1.0 - Math.min(1.0, ring / 10.0), 2.0);
            double fade = Math.max(0.0, 1.0 - ring / 14.0);
            painter.circle(heart.subtract(0.0, 0.45, 0.0), east, south, 0.4 + reach * 1.9 * wave, 0.1, 0.8,
                    Colors.alpha(fade), Colors.alpha(0.5 * fade));
        }
        // Rays and sparks shooting out of it.
        if (since < 10.0) {
            double rays = 1.0 - since / 10.0;
            for (int k = 0; k < 14; k++) {
                Vec3 way = Noise.direction(k + blast.id(), 251);
                way = new Vec3(way.x, Math.abs(way.y) * 0.9 + 0.1, way.z).normalize();
                double out = reach * (0.8 + 2.4 * Noise.of(k, blast.id(), 5)) * Math.min(1.0,
                        since / 3.0 + 0.3);
                painter.edge(heart.add(way.scale(out * 0.35)), heart.add(way.scale(out)), 0.12 * rays, rays);
            }
        }
    }

    // ---- His arm ----

    /**
     * How far this player's ring fist is thrown up high to call the plane, 0 to 1: straight up as he calls it, down
     * again once the plane has taken shape.
     */
    public static float raised(Entity player, float partialTick) {
        float age = ClientConstructs.planeAge(player.getId(), partialTick);
        if (age < 0.0F) {
            return 0.0F;
        }
        return (float) (Ease.smooth(age / 3.0) * (1.0 - Ease.smooth((age
                - AirStrike.CALL_TICKS) / 6.0)));
    }

    // ---- Its sound ----

    /** The planes are gone (the world was left): so is their sound. */
    public static void clear() {
        for (PlaneSound sound : SOUNDS.values()) {
            sound.stopAll();
        }
        SOUNDS.clear();
        GUNS.clear();
    }
}
