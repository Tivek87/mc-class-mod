package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.engine.math.Noise;

public final class FireStream {
    public enum Kind {
        STREAM(1.3, 11.0, 0.22, 0.9, 5.5, 0.25, 0.6, 0.07),
        SWEEP(0.95, 8.0, 0.2, 0.55, 5.0, 0.3, 0.7, 0.1),
        BLAST(0.55, 16.0, 0.26, 1.4, 4.0, 0.5, 0.55, 0.2),
        LAY(0.9, 5.0, 0.16, 0.15, 3.0, 0.4, 0.8, 0.0),
        FEED(0.5, 6.0, 0.18, 0.1, 3.5, 0.45, 0.8, 0.05),
        SMOKE(0.05, 22.0, 0.1, 0.9, 5.0, 1.5, 1.0, 0.3);

        public final double speed;
        public final double life;
        final double size;
        final double rise;
        final double grow;
        final double every;
        final double bright;
        final double spread;

        Kind(double speed, double life, double size, double rise, double grow, double every, double bright,
                double spread) {
            this.speed = speed;
            this.life = life;
            this.size = size;
            this.rise = rise;
            this.grow = grow;
            this.every = every;
            this.bright = bright;
            this.spread = spread;
        }
    }

    private static final int MOST = 160;
    private static final Map<Integer, FireStream> STREAMS = new HashMap<>();
    private static int seeds;

    private static final class Puff {
        final Kind kind;
        final Vec3 start;
        final Vec3 way;
        final double speed;
        final double born;
        final double reach;
        final int seed;
        final int burst;

        Puff(Kind kind, Vec3 start, Vec3 way, double speed, double born, double reach, int burst) {
            this.kind = kind;
            this.start = start;
            this.seed = seeds++;
            this.way = way.add(Noise.direction(this.seed, 5).scale(kind.spread)).normalize();
            this.speed = speed;
            this.born = born;
            this.reach = reach;
            this.burst = burst;
        }

        double travel(double u) {
            return this.speed * this.kind.life * u * (1.0 - 0.4 * u);
        }

        Vec3 at(double age) {
            double u = age / this.kind.life;
            double along = Math.min(this.travel(u), this.reach);
            Vec3 drift = Noise.direction(this.seed, 3).scale(this.kind.size * 2.2 * u * Math.sqrt(u));
            return this.start.add(this.way.scale(along)).add(drift).add(0.0, this.kind.rise * u * u, 0.0);
        }

        double radius(double age) {
            double u = age / this.kind.life;
            double splash = Math.max(0.0, this.travel(u) - this.reach);
            return this.kind.size * (1.0 + this.kind.grow * Math.sqrt(u)) + 0.5 * splash;
        }
    }

    private final ArrayDeque<Puff> puffs = new ArrayDeque<>();
    private final Map<Kind, Double> lastEmit = new HashMap<>();
    private final Map<Kind, Vec3> lastFrom = new HashMap<>();
    private final Map<Kind, Integer> bursts = new HashMap<>();

    private FireStream() {
    }

    public static double now(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0.0 : minecraft.level.getGameTime() + partialTick;
    }

    public static void feed(Entity owner, Kind kind, Vec3 from, Vec3 way, double now) {
        feed(owner, kind, from, way, now, kind.speed);
    }

    public static void feed(Entity owner, Kind kind, Vec3 from, Vec3 way, double now, double speed) {
        if (way.lengthSqr() < 1.0E-8) {
            return;
        }
        FireStream stream = STREAMS.computeIfAbsent(owner.getId(), id -> new FireStream());
        Double last = stream.lastEmit.get(kind);
        Vec3 was = stream.lastFrom.get(kind);
        if (last == null || now - last > kind.every * 4.0 || now < last) {
            stream.bursts.merge(kind, 1, Integer::sum);
            last = now - kind.every;
            was = from;
        }
        Vec3 dir = way.normalize();
        double reach = reach(owner, from, dir, speed * kind.life * 0.6);
        int burst = stream.bursts.getOrDefault(kind, 0);
        for (double at = last + kind.every; at <= now + 1.0E-6; at += kind.every) {
            double share = (at - last) / Math.max(1.0E-6, now - last);
            Vec3 start = was == null ? from : was.lerp(from, Math.min(1.0, share));
            stream.puffs.add(new Puff(kind, start, dir, speed, at, reach, burst));
            stream.lastEmit.put(kind, at);
        }
        stream.lastFrom.put(kind, from);
        while (stream.puffs.size() > MOST) {
            stream.puffs.poll();
        }
    }

    private static double reach(Entity owner, Vec3 from, Vec3 way, double far) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return far;
        }
        Vec3 to = from.add(way.scale(far));
        HitResult hit = minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, CollisionContext.of(owner)));
        return hit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : from.distanceTo(hit.getLocation());
    }

    public static void draw(LanternPainter painter, double now) {
        Iterator<FireStream> streams = STREAMS.values().iterator();
        while (streams.hasNext()) {
            FireStream stream = streams.next();
            stream.puffs.removeIf(puff -> now - puff.born > puff.kind.life || now < puff.born - 40.0);
            if (stream.puffs.isEmpty()) {
                streams.remove();
                continue;
            }
            stream.drawPuffs(painter, now);
        }
    }

    private void drawPuffs(LanternPainter painter, double now) {
        List<Puff> order = new ArrayList<>(this.puffs);
        Puff previous = null;
        Vec3 previousAt = null;
        double previousWide = 0.0;
        double previousAlpha = 0.0;
        for (int i = order.size() - 1; i >= 0; i--) {
            Puff puff = order.get(i);
            double age = now - puff.born;
            if (age < 0.0) {
                continue;
            }
            double u = age / puff.kind.life;
            Vec3 at = puff.at(age);
            double radius = puff.radius(age);
            double alpha = Math.min(1.0, u / 0.08) * Math.pow(1.0 - u, 1.2) * puff.kind.bright;
            if (puff.kind == Kind.SMOKE) {
                FirePainter.smoke(painter, at, radius, Math.sin(Math.PI * u), puff.seed);
                continue;
            }
            double heat = Math.pow(1.0 - u, 0.8);
            if (u > 0.6 && Noise.of(puff.seed, 1, 1) < 0.3) {
                FirePainter.smoke(painter, at.add(0.0, radius * 0.7, 0.0), radius * 0.7, (u - 0.6) * 1.5,
                        puff.seed + 5);
            }
            FirePainter.ball(painter, at, radius, heat, alpha, puff.seed);
            if (u > 0.2 && u < 0.85 && puff.seed % 3 == 0) {
                Vec3 lick = puff.way.scale(0.5).add(0.0, 1.0, 0.0).normalize();
                FirePainter.tongue(painter, at, lick, radius * 1.4, radius * 0.75, alpha * 0.9, puff.seed,
                        painter.time());
            }
            double core = radius * 0.35 * (1.0 - u);
            if (previous != null && previous.kind == puff.kind && previous.burst == puff.burst
                    && Math.abs(previous.born - puff.born) < puff.kind.every * 1.6 && u < 0.6) {
                painter.lightTaper(previousAt, at, previousWide, core, FirePainter.CORE,
                        0.7 * previousAlpha, 0.7 * alpha * (1.0 - u));
                painter.glowTaper(previousAt, at, previousWide * 3.0, core * 3.0, FirePainter.FLAME,
                        0.35 * previousAlpha, 0.35 * alpha);
            }
            previous = puff;
            previousAt = at;
            previousWide = core;
            previousAlpha = alpha * (1.0 - u);
        }
    }

    public static boolean out() {
        return STREAMS.isEmpty();
    }

    public static void clear() {
        STREAMS.clear();
    }
}
