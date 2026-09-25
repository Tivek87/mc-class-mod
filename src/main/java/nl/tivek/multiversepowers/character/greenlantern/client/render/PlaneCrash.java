package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter.BREAK_TICKS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter.CRASH_FLING;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter.frame;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.partsBroken;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.BODY;

final class PlaneCrash {
    private static final double SHELL = 46.0;
    private static final double FIREBALL_HIGH = 40.0;
    private static final double FIREBALL = 18.5;
    private static final double COLLAR = 64.0;

    private PlaneCrash() {
    }

    static void crashed(LanternPainter painter, int owner, PlanePath path, double since) {
        double apart = since / BREAK_TICKS;
        if (apart < 1.0) {
            ConstructPainter.Frame frame = frame(path, path.crashTick(), 1.0);
            painter.glare(Math.max(0.0, 0.9 - since / 10.0));
            painter.fling(CRASH_FLING);
            painter.shattered(BODY, frame, apart, 1.4);
            partsBroken(painter, owner, path, frame, path.crashTick(), apart, 1.4);
            painter.fling(1.0);
            painter.glare(0.0);
        }
        blast(painter, path.crash(), since);
    }

    private static void blast(LanternPainter painter, Vec3 ground, double since) {
        double life = Math.max(0.0, 1.0 - since / AirStrike.BLAST_TICKS);
        if (life <= 0.0) {
            return;
        }
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        Vec3 up = Vectors.UP;
        double flash = Math.max(0.0, 1.0 - since / 10.0);
        painter.flare(ground.add(0.0, 3.0, 0.0), 6.0 + 40.0 * flash * flash, Math.min(1.0, 0.4 + flash));
        if (flash > 0.0) {
            double burst = 4.0 + 18.0 * (1.0 - flash * flash);
            painter.haze(ground.add(0.0, 2.0, 0.0), east.scale(burst), up.scale(burst * 0.8), south.scale(burst),
                    LanternPainter.HOT, 0.8 * flash);
        }
        double swell = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 22.0), 3.0);
        double climb = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 70.0), 2.0);
        double radius = 2.5 + FIREBALL * swell;
        Vec3 ball = ground.add(0.0, 2.0 + FIREBALL_HIGH * climb, 0.0);
        double heat = Math.max(0.0, 1.0 - since / 45.0);
        painter.haze(ball, east.scale(radius), up.scale(radius * 0.92), south.scale(radius), LanternPainter.GREEN,
                0.55 * life);
        painter.haze(ball, east.scale(radius * 0.62), up.scale(radius * 0.58), south.scale(radius * 0.62),
                LanternPainter.HOT, 0.6 * heat * life);
        if (since > 6.0) {
            double cap = Ease.smooth((since - 6.0) / 30.0);
            double wide = radius * (1.25 + 0.55 * cap);
            painter.haze(ball.subtract(0.0, radius * 0.25, 0.0), east.scale(wide), up.scale(radius * 0.5),
                    south.scale(wide), LanternPainter.GREEN, 0.4 * cap * life);
        }
        double surge = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 26.0), 2.0);
        double spread = 5.0 + (SHELL - 4.0) * surge;
        painter.haze(ground.add(0.0, 1.2, 0.0), east.scale(spread), up.scale(1.6 + 2.2 * surge), south.scale(spread),
                LanternPainter.GREEN, 0.45 * life * (1.0 - 0.6 * surge));
        int shell = Colors.alpha(0.85 * life);
        int haze = Colors.alpha(0.4 * life);
        for (int k = -3; k <= 3; k++) {
            double lat = k / 3.5 * (Math.PI * 0.5);
            double turn = painter.time() * 0.01 + k;
            Vec3 a = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
            painter.circle(ball.add(0.0, radius * Math.sin(lat), 0.0), a, a.cross(up), radius * Math.cos(lat),
                    0.3, 2.4, shell, haze);
        }
        for (int k = 0; k < 4; k++) {
            double turn = k * Math.PI / 4.0 + painter.time() * 0.015;
            Vec3 across = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
            painter.circle(ball, across, up, radius, 0.2, 1.8, shell, haze);
        }
        painter.flare(ball, radius * 1.9, 0.55 * life);
        if (since > 6.0) {
            double cap = Ease.smooth((since - 6.0) / 24.0);
            double around = radius * (1.05 + 0.35 * cap);
            double thick = radius * (0.25 + 0.2 * cap);
            Vec3 middle = ball.subtract(0.0, radius * 0.35, 0.0);
            int rolls = 14;
            for (int k = 0; k < rolls; k++) {
                double turn = Math.PI * 2.0 * k / rolls;
                Vec3 out = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
                double spin = painter.time() * 0.08 + k;
                Vec3 a = out.scale(Math.cos(spin)).add(up.scale(Math.sin(spin)));
                Vec3 b = out.scale(-Math.sin(spin)).add(up.scale(Math.cos(spin)));
                painter.circle(middle.add(out.scale(around)), a, b, thick, 0.18, 1.4,
                        Colors.alpha(0.7 * cap * life), Colors.alpha(0.3 * cap * life));
            }
        }
        Vec3 foot = ground.add(0.0, 0.3, 0.0);
        Vec3 top = ball.subtract(0.0, radius * 0.7, 0.0);
        if (top.y > foot.y + 1.0) {
            painter.beamOfLight(foot, top, life, 20.0 + since, 3.2 + 1.6 * (1.0 - climb));
            double stem = 2.2 + 1.6 * (1.0 - climb);
            painter.haze(foot.lerp(top, 0.5), east.scale(stem), up.scale((top.y - foot.y) * 0.5), south.scale(stem),
                    LanternPainter.GREEN, 0.4 * life);
            for (int k = 0; k < 3; k++) {
                double skirt = 2.5 + 3.5 * climb + k * 1.4;
                painter.circle(foot.add(0.0, 0.4 + k * 0.8, 0.0), east, south, skirt, 0.2, 1.6, shell, haze);
            }
        }
        if (since > 3.0 && since < 40.0) {
            double ring = 1.0 - Math.pow(1.0 - Math.min(1.0, (since - 3.0) / 30.0), 2.0);
            double fade = (1.0 - Ease.smooth((since - 22.0) / 18.0)) * life;
            painter.circle(ground.add(0.0, FIREBALL_HIGH * 0.45, 0.0), east, south, 4.0 + COLLAR * ring, 0.3, 3.0,
                    Colors.alpha(0.8 * fade), Colors.alpha(0.35 * fade));
        }
        double out = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 16.0), 3.0);
        double dome = Math.max(0.5, SHELL * out);
        double domeFade = life * (1.0 - Ease.smooth((since - 14.0) / 20.0));
        int domeEdge = Colors.alpha(0.8 * domeFade);
        int domeHaze = Colors.alpha(0.35 * domeFade);
        for (int k = 0; k < 5; k++) {
            double lift = k / 5.0;
            painter.circle(ground.add(0.0, dome * lift, 0.0), east, south, dome * Math.sqrt(1.0 - lift * lift),
                    0.3 - 0.04 * k, 2.6 - 0.35 * k, domeEdge, domeHaze);
        }
        for (int k = 0; k < 4; k++) {
            double turn = k * Math.PI / 4.0;
            Vec3 across = new Vec3(Math.cos(turn), 0.0, Math.sin(turn));
            painter.circle(ground, across, up, dome * 0.97, 0.18, 1.6, domeEdge, domeHaze);
        }
        for (int k = 0; k < 5; k++) {
            double ring = since - k * 6.0;
            if (ring < 0.0) {
                continue;
            }
            double wave = 1.0 - Math.pow(1.0 - Math.min(1.0, ring / 20.0), 2.0);
            double fade = Math.max(0.0, 1.0 - ring / 34.0) * life;
            painter.circle(ground.add(0.0, 0.2, 0.0), east, south, 3.0 + (SHELL + 8.0) * wave, 0.35, 2.0,
                    Colors.alpha(fade), Colors.alpha(0.5 * fade));
        }
        if (since < 30.0) {
            double rays = 1.0 - since / 30.0;
            Vec3 heart = ground.add(0.0, 2.0, 0.0);
            for (int k = 0; k < 24; k++) {
                Vec3 way = Noise.direction(k, 241);
                way = new Vec3(way.x, Math.abs(way.y) * 0.8 + 0.1, way.z).normalize();
                double reach = (10.0 + 22.0 * Noise.of(k, 241, 5)) * Math.min(1.0, since / 4.0 + 0.2);
                painter.edge(heart.add(way.scale(2.0)), heart.add(way.scale(reach)), 0.6 * rays, rays);
            }
        }
        if (since < 1.0) {
            painter.flare(ground.add(0.0, 2.0, 0.0), 40.0, 1.0);
        }
    }
}
