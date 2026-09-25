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

public final class PlanePainter {
    static final double GROW_FROM = 6.0;
    private static final double GROW_TICKS = 30.0;
    static final double BREAK_TICKS = 40.0;
    static final double CRASH_FLING = 10.0;
    private static final double AIR_FLING = 5.0;
    private static final double PILLAR_THICK = 3.6;
    private static final double PILLAR_UNTIL = PlanePath.FORM + 4.0;
    public static final int BLAST_TICKS = 30;
    public static final double MISSILE_BREAKS = 12.0;
    private static final double SLUG_BREAKS = 4.0;
    static final double GLOWS = 0.3;

    private PlanePainter() {
    }

    public static PlanePath path(ConstructPayload plane) {
        return new PlanePath(plane.center(), plane.facing(), plane.size(), Math.round(plane.charge()),
                plane.variant() / 100.0);
    }

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

    private static double grown(double t) {
        return Ease.backOut((t - GROW_FROM) / GROW_TICKS);
    }

    static ConstructPainter.Frame frame(PlanePath path, double t, double scale) {
        Vec3[] axes = path.axes(t);
        return new ConstructPainter.Frame(path.at(t), axes[0], axes[1], axes[2], scale);
    }

    private static void flying(LanternPainter painter, int id, int owner, PlanePath path, double t,
            @Nullable Vec3 ring, float partialTick) {
        double grown = grown(t);
        double down = path.down(t);
        ConstructPainter.Frame frame = frame(path, t, Math.max(grown, 1.0E-3));
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
        scanCone(painter, owner, frame, partialTick);
        if (t >= path.failTick()) {
            burning(painter, path, frame, t);
        }
        if (grown <= 0.01 || !painter.visible(frame.center(), 40.0 * grown)) {
            return;
        }
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

    public static void fired(ConstructPayload plane, ConstructPayload round) {
        Vec3 next = round.facing();
        if (next.lengthSqr() < 1.0E-6 || round.variant() < 0 || round.variant() > 1) {
            return;
        }
        guns(round.owner(), plane.id(), path(plane)).turrets[round.variant()].fired(Math.round(round.charge()),
                next.normalize());
    }

    public static void broken(LanternPainter painter, ConstructPayload plane, double clock, double since) {
        PlanePath path = path(plane);
        double t = Math.min(clock - since, path.crashTick());
        double apart = since / BREAK_TICKS;
        ConstructPainter.Frame frame = frame(path, t, Math.max(grown(t), 1.0E-3));
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 6.0));
        painter.fling(AIR_FLING);
        painter.shattered(BODY, frame, apart, 1.2);
        partsBroken(painter, plane.owner(), path, frame, t, apart, 1.2);
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

    public static void missile(LanternPainter painter, boolean small, Vec3 at, Vec3 nose, Vec3 up, double burning,
            double broken) {
        double scale = small ? SMALL_MISSILE_SCALE : MISSILE_SCALE;
        Vec3 forward = nose.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : nose.normalize();
        ConstructPainter.Frame frame = ConstructPainter.Frame.of(at, forward, up, scale);
        if (broken >= 0.0) {
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
            double flash = 1.0 - burning / 4.0;
            painter.flare(tail, (small ? 1.2 : 3.6) * flash + 0.6, flash);
        }
        painter.exhaust(tail, forward.scale(-1.0), small ? 2.6 : 6.0, small ? 0.2 : 0.5, lit);
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

    public static int bulletTicks(ConstructPayload round) {
        return Math.round(round.size()) + 14;
    }

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
        double flash = Math.max(0.0, 1.0 - since / 5.0);
        if (flash > 0.0) {
            painter.flare(heart, reach * (1.2 + 3.0 * flash), flash);
        }
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

    public static float raised(Entity player, float partialTick) {
        float age = ClientConstructs.planeAge(player.getId(), partialTick);
        if (age < 0.0F) {
            return 0.0F;
        }
        return (float) (Ease.smooth(age / 3.0) * (1.0 - Ease.smooth((age
                - AirStrike.CALL_TICKS) / 6.0)));
    }

    public static void clear() {
        for (PlaneSound sound : SOUNDS.values()) {
            sound.stopAll();
        }
        SOUNDS.clear();
        GUNS.clear();
    }
}
