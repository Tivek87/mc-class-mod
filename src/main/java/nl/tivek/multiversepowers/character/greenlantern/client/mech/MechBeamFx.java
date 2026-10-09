package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechHeadShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBeam;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The mech's eyes as everyone sees them, aimed where its pilot's crosshair rests; nothing like the ring's own beam (no
// rings, no strands). A click fires a lance: streaks of light flash across both eye slits, a bolt of light shoots out
// of each and the two meet on what they strike in a star of spikes and a ring thrown off it. Held, a torrent: a stream
// out of each slit, the two joined a little ahead into one flat blade of light with hard bright edges, arrowheads of
// light racing along it to where it strikes, there a splash of molten light spraying sparks back.
final class MechBeamFx {
    // A pose's blow runs about two ticks behind the server's: drawn that far ahead, a beam shows as it hits.
    private static final double LEAD = 2.0;
    private static final int FADE = 4;
    // The lance's bolts shoot out LANCE_SPEED blocks a tick, LANCE_HEAD long; its star bursts over STAR ticks.
    private static final double LANCE_SPEED = 22.0;
    private static final double LANCE_HEAD = 6.0;
    private static final double STAR = 6.0;
    // The torrent's streams join JOIN ahead of the visor into a blade BLADE wide on either side, drawn in pieces
    // PIECE long; its arrowheads run EVERY apart at RACE blocks a tick.
    private static final double JOIN = 3.2;
    private static final double BLADE = 0.42;
    private static final double PIECE = 1.6;
    private static final double EVERY = 3.4;
    private static final double RACE = 2.2;
    private static final int SPARKS = 12;
    // Nearer the camera than this a piece is left out, so the pilot's own view is not filled with light.
    private static final double NEAR = 1.2;

    private MechBeamFx() {
    }

    static void draw(LanternPainter painter, MechPose pose, double t, int pilotId, boolean own) {
        MechAttacks.Blow blow = pose.blow();
        if (blow.kind() != MechAttacks.EYE && blow.kind() != MechAttacks.GLARE) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        Entity pilot = level == null ? null : level.getEntity(pilotId);
        if (pilot == null) {
            return;
        }
        Frame head = MechPainter.head(pose, t, -1.0, true);
        if (blow.kind() == MechAttacks.EYE) {
            lance(painter, head, blow.age(), level, pilot, own);
        } else {
            torrent(painter, head, blow.age(), level, pilot, own);
        }
    }

    private static Vec3 slit(Frame head, int side) {
        return head.at(side * (MechHeadShapes.EYE_X[0] + MechHeadShapes.EYE_X[1]) * 0.5, MechHeadShapes.EYE_Y,
                MechHeadShapes.EYE_Z + 0.05);
    }

    // The eye slits blazing `power` 0..1: a flare in each and a streak of light across it, out to either side.
    private static void slits(LanternPainter painter, Frame head, double power, boolean own) {
        double dim = own ? 0.45 : 1.0;
        Vec3 across = head.right().normalize();
        for (int side = -1; side <= 1; side += 2) {
            Vec3 at = slit(head, side);
            painter.flare(at, 0.55 * dim, power);
            double reach = (1.4 + 0.8 * power) * dim;
            painter.lightTaper(at, at.add(across.scale(reach)), 0.2 * dim, 0.0, LanternPainter.HOT, power, 0.0);
            painter.lightTaper(at, at.subtract(across.scale(reach)), 0.2 * dim, 0.0, LanternPainter.HOT, power, 0.0);
        }
    }

    // The click: a bolt out of each slit, meeting on what they strike in a star of light.
    private static void lance(LanternPainter painter, Frame head, double age, ClientLevel level, Entity pilot,
            boolean own) {
        double since = age + LEAD - MechAttacks.EYE_FIRE;
        if (since < -2.0 || since > MechAttacks.EYE_SHOWN) {
            return;
        }
        double on = 1.0 - Ease.smooth((since - MechAttacks.EYE_SHOWN + FADE) / FADE);
        slits(painter, head, since < 0.0 ? Ease.smooth((since + 2.0) / 2.0) : on, own);
        if (since < 0.0) {
            return;
        }
        Vec3 middle = head.at(0.0, MechHeadShapes.EYE_Y, MechHeadShapes.EYE_Z + 0.05);
        Vec3 end = end(level, pilot, middle, MechBeam.EYE_RANGE, true);
        double reach = since * LANCE_SPEED;
        double arrived = Double.MAX_VALUE;
        for (int side = -1; side <= 1; side += 2) {
            Vec3 from = slit(head, side);
            Vec3 way = end.subtract(from);
            double length = way.length();
            if (length < 1.0E-3) {
                continue;
            }
            way = way.scale(1.0 / length);
            arrived = Math.min(arrived, length / LANCE_SPEED);
            Vec3 tip = from.add(way.scale(Math.min(reach, length)));
            Vec3 tail = from.add(way.scale(Math.max(0.0, Math.min(reach, length) - LANCE_HEAD)));
            double bolt = reach < length + LANCE_HEAD ? on : 0.0;
            painter.lightTaper(tail, tip, 0.0, 0.34, LanternPainter.HOT, 0.0, bolt);
            painter.glowTaper(tail, tip, 0.0, 1.1, LanternPainter.GREEN, 0.0, 0.65 * bolt);
            // The thread it leaves along its way, burning out.
            double thread = on * (1.0 - Ease.smooth(since / MechAttacks.EYE_SHOWN));
            painter.lightLine(from, tip, 0.05, LanternPainter.BRIGHT, Colors.alpha(0.7 * thread));
            painter.glowLine(from, tip, 0.35, LanternPainter.GREEN, Colors.alpha(0.25 * thread));
        }
        double u = (since - arrived) / STAR;
        if (u < 0.0 || u > 1.0) {
            return;
        }
        Vec3 back = middle.subtract(end).normalize();
        Vec3[] across = Vectors.across(back);
        double fade = 1.0 - u;
        for (int k = 0; k < 6; k++) {
            double turn = Math.PI * 2.0 * k / 6.0 + 0.3;
            Vec3 out = across[0].scale(Math.cos(turn)).add(across[1].scale(Math.sin(turn)));
            double far = (k % 2 == 0 ? 2.6 : 1.6) * Ease.backOut(Math.min(1.0, u * 2.5));
            painter.lightTaper(end, end.add(out.scale(far)), 0.22, 0.0, LanternPainter.HOT, fade, 0.0);
        }
        painter.circle(end.add(back.scale(0.3)), across[0], across[1], 0.4 + 3.6 * Ease.smooth(u), 0.09, 0.6,
                Colors.alpha(0.95 * fade), Colors.alpha(0.5 * fade));
        painter.flare(end, 0.6 + 1.8 * fade, fade);
    }

    // The held beam: a stream out of each slit, joined ahead of the visor into one blade of light.
    private static void torrent(LanternPainter painter, Frame head, double age, ClientLevel level, Entity pilot,
            boolean own) {
        double since = age + LEAD - MechAttacks.GLARE_FIRE;
        double over = age - MechAttacks.GLARE_MOST;
        if (since < -3.0 || over > MechAttacks.GLARE_FADE) {
            return;
        }
        double on = Ease.smooth(since / 2.0) * (1.0 - Ease.smooth(over / MechAttacks.GLARE_FADE));
        double time = painter.time();
        double throb = 0.9 + 0.1 * Math.sin(time * 1.9) * Math.sin(time * 0.7 + 1.0);
        slits(painter, head, since < 0.0 ? Ease.smooth((since + 3.0) / 3.0) : Math.max(on, 0.0), own);
        if (since < 0.0 || on <= 0.0) {
            return;
        }
        Vec3 middle = head.at(0.0, MechHeadShapes.EYE_Y, MechHeadShapes.EYE_Z + 0.05);
        Vec3 end = end(level, pilot, middle, MechBeam.GLARE_RANGE, false);
        Vec3 axis = end.subtract(middle);
        double full = axis.length();
        if (full < 1.0E-3) {
            return;
        }
        axis = axis.scale(1.0 / full);
        // It shoots out over its first two ticks.
        double length = full * Ease.smooth(Math.min(1.0, since / 2.0));
        Vec3 join = middle.add(axis.scale(Math.min(JOIN, length)));
        for (int side = -1; side <= 1; side += 2) {
            Vec3 from = slit(head, side);
            painter.lightTaper(from, join, 0.16, 0.3, LanternPainter.HOT, on, on);
            painter.glowTaper(from, join, 0.5, 1.0, LanternPainter.GREEN, 0.5 * on, 0.6 * on);
        }
        Vec3 camera = painter.camera();
        Vec3 last = join;
        for (double d = JOIN; d < length; d += PIECE) {
            Vec3 next = middle.add(axis.scale(Math.min(length, d + PIECE)));
            Vec3 mid = last.add(next).scale(0.5);
            if (mid.distanceTo(camera) >= NEAR) {
                Vec3 side = next.subtract(last).cross(camera.subtract(mid));
                side = side.lengthSqr() < 1.0E-9 ? Vectors.across(axis)[0] : side.normalize();
                double wide = BLADE * throb * (1.0 + 0.08 * Math.sin(d * 0.9 - time * 1.4));
                Vec3 edge = side.scale(wide);
                painter.lightLine(last, next, 0.1, LanternPainter.HOT, Colors.alpha(on));
                painter.lightLine(last.add(edge), next.add(edge), 0.05, LanternPainter.HOT, Colors.alpha(0.9 * on));
                painter.lightLine(last.subtract(edge), next.subtract(edge), 0.05, LanternPainter.HOT,
                        Colors.alpha(0.9 * on));
                painter.lightLine(last, next, wide * 1.7, LanternPainter.BRIGHT, Colors.alpha(0.3 * on));
                painter.glowLine(last, next, wide * 4.2, LanternPainter.GREEN, Colors.alpha(0.3 * on));
            }
            last = next;
        }
        // Arrowheads of light racing along the blade to where it strikes.
        for (double d = JOIN + (time * RACE) % EVERY; d < length - 0.5; d += EVERY) {
            Vec3 tip = middle.add(axis.scale(d));
            if (tip.distanceTo(camera) < NEAR * 2.0) {
                continue;
            }
            Vec3 side = axis.cross(camera.subtract(tip));
            side = side.lengthSqr() < 1.0E-9 ? Vectors.across(axis)[0] : side.normalize();
            Vec3 back = tip.subtract(axis.scale(0.9));
            double a = on * Mth.clamp((length - d) / 3.0, 0.0, 1.0);
            for (int flip = -1; flip <= 1; flip += 2) {
                Vec3 wing = back.add(side.scale(flip * BLADE * 1.9));
                painter.lightLine(wing, tip, 0.07, LanternPainter.HOT, Colors.alpha(0.95 * a));
                painter.glowLine(wing, tip, 0.35, LanternPainter.GREEN, Colors.alpha(0.4 * a));
            }
        }
        if (length < full - 1.0E-3) {
            return;
        }
        splash(painter, end, axis.scale(-1.0), on, time);
    }

    // Where the torrent strikes: a splash of molten light, sparks sprayed back off it.
    private static void splash(LanternPainter painter, Vec3 at, Vec3 back, double on, double time) {
        int seed = (int) (time * 0.5);
        painter.glowDisc(at, 1.5 + 0.3 * Math.sin(time * 2.3), LanternPainter.GREEN, 0.6 * on, 0.35, seed);
        painter.lightDisc(at, 0.55, LanternPainter.HOT, 0.9 * on, 0.25, seed + 1);
        painter.flare(at, 1.8 + 0.3 * Math.sin(time * 1.7), on);
        Vec3[] across = Vectors.across(back);
        for (int k = 0; k < SPARKS; k++) {
            double u = Mth.frac(time * 0.09 + k * 0.618);
            int round = (int) Math.floor(time * 0.09 + k * 0.618);
            double turn = Math.PI * 2.0 * Noise.of(round, k, 51);
            double lean = 0.4 + 0.9 * Noise.of(round, k, 52);
            Vec3 way = back.add(across[0].scale(Math.cos(turn) * lean)).add(across[1].scale(Math.sin(turn) * lean))
                    .normalize();
            double far = 0.3 + 3.2 * u;
            Vec3 from = at.add(way.scale(far)).add(0.0, -1.4 * u * u, 0.0);
            Vec3 to = at.add(way.scale(far + 0.55)).add(0.0, -1.4 * (u + 0.1) * (u + 0.1), 0.0);
            painter.lightLine(from, to, 0.06, LanternPainter.HOT, Colors.alpha(on * (1.0 - u)));
        }
    }

    // Where a beam from `from` ends: past what the pilot's crosshair rests on till a block stops it, or, for the eye
    // beam, at the first creature in its way.
    private static Vec3 end(ClientLevel level, Entity pilot, Vec3 from, double range, boolean first) {
        Vec3 end = MechBeam.reach(level, from, MechBeam.aim(level, pilot, range), range, pilot);
        if (!first) {
            return end;
        }
        LivingEntity hit = MechBeam.first(level, pilot, from, end);
        return hit == null ? end : hit.getBoundingBox().getCenter();
    }
}
