package nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.revolver.RevolverPainter;
import nl.tivek.multiversepowers.character.greenlantern.duo.RevolverGun;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The beam eye's eye and the gunslinger's revolver, both solid hard light, and their light: the eye gathering light
// and its thick beam burning to what it hits, the revolver growing in the fist, its muzzle flashes, shots and smoke.
public final class HandShotLight {
    private static final int WHITE = 0xF2FFF4;
    private static final int LIGHT = 0x5CFF7A;
    private static final int SMOKE = 0xD8E6DC;
    private static final double BEAM_RANGE = 28.0;
    private static final double SHOT_RANGE = 30.0;
    private static final double SHOT_TICKS = 7.0;
    private static final double SHOT_SPEED = 14.0;
    private static final double BREAK_TICKS = 14.0;
    private static final int GATHER = 12;

    private HandShotLight() {
    }

    public static void parts(LanternPainter painter, int variant, HandPose.Place place, double clock, double bright,
            double apart) {
        switch (HandPose.move(variant)) {
            case HandPose.BEAMEYE -> HandMarvelLight.eyeball(painter, place, HandPose.beamEyeOpen(clock),
                    0.6 * HandPose.beamEyeFiring(clock), clock, bright, apart);
            case HandPose.GUNSLINGER -> {
                double broke = (clock - HandPose.GUN_BREAKS) / BREAK_TICKS;
                if (broke < 1.0) {
                    RevolverPainter.held(painter, RevolverGun.inFist(place), HandPose.slingGrown(clock),
                            HandPose.slingHammer(clock), HandPose.slingTurn(clock),
                            broke >= 0.0 ? Math.max(apart, broke) : apart);
                }
            }
            default -> {
            }
        }
    }

    public static void blows(LanternPainter painter, int id, int variant, HandPose.Place place, Vec3 base,
            Vec3 facing, double clock, double strength) {
        if (strength <= 0.01) {
            return;
        }
        switch (HandPose.move(variant)) {
            case HandPose.BEAMEYE -> beam(painter, id, place, base, facing, clock, strength);
            case HandPose.GUNSLINGER -> shots(painter, id, variant, place, base, facing, clock, strength);
            default -> {
            }
        }
    }

    // The middle of the eye's front, where its pupil is and the beam leaves.
    private static Vec3 pupil(HandPose.Place place) {
        return place.at(HandPose.EYE_POINT.add(0.0, 0.0, HandPose.EYE_SIZE * 0.72));
    }

    // Light runs in to the pupil from all round as it charges, rings turning faster and faster round it; then the
    // beam: a white-hot core in a thick green glow, rings of light racing along it, and a blaze where it strikes.
    private static void beam(LanternPainter painter, int id, HandPose.Place place, Vec3 base, Vec3 facing,
            double clock, double strength) {
        double open = HandPose.beamEyeOpen(clock);
        if (open <= 0.02) {
            return;
        }
        Vec3 front = pupil(place);
        Vec3 out = place.forward().normalize();
        Vec3 right = place.right().normalize();
        Vec3 up = out.cross(right).normalize();
        double size = place.scale() * HandPose.EYE_SIZE;
        double charge = HandPose.beamEyeCharge(clock);
        double firing = HandPose.beamEyeFiring(clock);
        painter.circle(front, right, up, 0.4 * size * (1.0 + 0.3 * charge), 0.12 * size, 0.4 * size,
                Colors.alpha(open * strength), Colors.alpha(0.6 * open * strength));
        painter.lightLine(front.subtract(up.scale(0.34 * size)), front.add(up.scale(0.34 * size)),
                (0.2 + 0.2 * charge) * size, WHITE, Colors.alpha(open * strength));
        painter.flare(front, (0.8 + 2.5 * charge) * size, (0.4 + 0.6 * charge) * open * strength);
        if (charge > 0.01 && firing < 0.5) {
            for (int k = 0; k < 2; k++) {
                double turn = clock * (0.2 + 0.8 * charge) * (k == 0 ? 1.0 : -1.3);
                Vec3 a = right.scale(Math.cos(turn)).add(up.scale(Math.sin(turn)));
                Vec3 b = right.scale(-Math.sin(turn)).add(up.scale(Math.cos(turn)));
                painter.circle(front.add(out.scale(0.2 * k)), a, b, (0.9 + 0.5 * k) * size * (1.2 - 0.5 * charge),
                        0.05, 0.25, Colors.alpha(0.8 * charge * strength), Colors.alpha(0.4 * charge * strength));
            }
            for (int k = 0; k < GATHER; k++) {
                double run = (clock * (0.07 + 0.1 * charge) + Noise.of(id, k, 3)) % 1.0;
                Vec3 from = front.add(Noise.direction(k * 13 + (int) (clock * 0.1 + Noise.of(id, k, 4) * 5.0), 23)
                        .scale(3.5 * (1.0 - run)));
                painter.edge(from, from.lerp(front, 0.4), 0.06, 0.8 * charge * Math.sin(Math.PI * run) * strength);
            }
        }
        if (firing <= 0.01) {
            return;
        }
        Vec3 way = HandPose.aimedAlong(out, front, base, facing);
        Vec3 end = front.add(way.scale(BEAM_RANGE));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            HitResult hit = minecraft.level.clip(new ClipContext(front, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, minecraft.player));
            if (hit.getType() != HitResult.Type.MISS) {
                end = hit.getLocation();
            }
        }
        double fade = firing * strength;
        double throb = 1.0 + 0.12 * Math.sin(clock * 2.3) + 0.06 * Math.sin(clock * 5.1);
        painter.glowLine(front, end, 3.0 * throb, LIGHT, Colors.alpha(0.35 * fade));
        painter.beamOfLight(front, end, fade, clock, 1.9 * throb);
        painter.lightLine(front, end, 0.42 * throb, WHITE, Colors.alpha(0.95 * fade));
        Vec3[] across = Vectors.across(way);
        double length = end.distanceTo(front);
        for (int k = 0; k < 5; k++) {
            double along = (clock * 0.9 + k * length / 5.0) % Math.max(1.0, length);
            double ring = 1.0 + 0.4 * Math.sin(k * 1.7 + clock * 0.3);
            painter.circle(front.add(way.scale(along)), across[0], across[1], ring, 0.06, 0.4,
                    Colors.alpha(0.7 * fade), Colors.alpha(0.35 * fade));
        }
        painter.flare(front, 3.0 * size * throb, fade);
        painter.flare(end, 3.2 * throb, fade);
        painter.glowDisc(end, 2.2 * throb, 0xB8FFC8, 0.6 * fade, 0.3, id);
        for (int k = 0; k < 8; k++) {
            Vec3 spark = Noise.direction(k * 11 + (int) (clock * 0.5), 41).subtract(way.scale(0.6)).normalize();
            painter.edge(end.add(spark.scale(0.3)), end.add(spark.scale(1.2 + 0.8 * Noise.of(id, k, 5))), 0.08,
                    0.8 * fade);
        }
    }

    // The revolver grows out of the light in the fist; each shot flashes at the muzzle and a bolt races to what it hits
    // with a ring of smoke behind it; at the end smoke curls up off the muzzle.
    private static void shots(LanternPainter painter, int id, int variant, HandPose.Place place, Vec3 base,
            Vec3 facing, double clock, double strength) {
        double grown = HandPose.slingGrown(clock);
        if (grown > 0.0 && grown < 1.0) {
            Vec3 fist = RevolverGun.inFist(place).at(RevolverGun.GRIP_HOLE);
            painter.flare(fist, (1.0 + 2.0 * grown) * place.scale(), (1.0 - grown) * strength);
            for (int k = 0; k < GATHER; k++) {
                double run = (clock * 0.12 + Noise.of(id, k, 7)) % 1.0;
                Vec3 from = fist.add(Noise.direction(k * 7 + (int) (clock * 0.1), 31).scale(3.0 * (1.0 - run)));
                painter.edge(from, from.lerp(fist, 0.4), 0.05, 0.7 * (1.0 - grown) * Math.sin(Math.PI * run)
                        * strength);
            }
        }
        for (int shot : HandPose.SLING_SHOTS) {
            double since = clock - shot;
            if (since < 0.0 || since > SHOT_TICKS) {
                continue;
            }
            HandPose.Place then = HandPose.at(variant, shot, 0.0).place(base, facing, place.scale());
            RevolverGun.Gun gun = RevolverGun.inFist(then);
            Vec3 muzzle = gun.at(RevolverGun.MUZZLE);
            Vec3 way = HandPose.aimedAlong(gun.forward(), muzzle, base, facing);
            Vec3 end = muzzle.add(way.scale(SHOT_RANGE));
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.player != null) {
                HitResult hit = minecraft.level.clip(new ClipContext(muzzle, end, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, minecraft.player));
                if (hit.getType() != HitResult.Type.MISS) {
                    end = hit.getLocation();
                }
            }
            double reach = muzzle.distanceTo(end);
            double fade = strength * (1.0 - since / SHOT_TICKS);
            double headAt = Math.min(reach, since * SHOT_SPEED + 2.0);
            Vec3 head = muzzle.add(way.scale(headAt));
            Vec3 tail = muzzle.add(way.scale(Math.max(0.0, headAt - 6.0)));
            painter.beamOfLight(tail, head, fade, clock, 0.35);
            painter.lightLine(tail, head, 0.08, WHITE, Colors.alpha(fade));
            painter.flare(head, 0.8, fade);
            if (headAt >= reach) {
                painter.flare(end, 2.0 * (1.0 - since / SHOT_TICKS), fade);
            }
            if (since < 3.0) {
                double flash = 1.0 - since / 3.0;
                Vec3 now = RevolverGun.inFist(place).at(RevolverGun.MUZZLE);
                painter.flare(now, 2.6 * flash * place.scale(), flash * strength);
                Vec3[] across = Vectors.across(way);
                for (int k = 0; k < 6; k++) {
                    double angle = Math.PI * 2.0 * k / 6.0 + shot;
                    Vec3 spike = way.scale(0.8).add(across[0].scale(Math.cos(angle) * 0.6))
                            .add(across[1].scale(Math.sin(angle) * 0.6)).normalize();
                    painter.edge(now, now.add(spike.scale((1.2 + 0.6 * Noise.of(id, k + shot, 9)) * flash)), 0.07,
                            flash * strength);
                }
            }
            double ring = Ease.smooth(since / SHOT_TICKS);
            Vec3[] across = Vectors.across(way);
            painter.circle(muzzle.add(way.scale(0.6 + 1.5 * ring)), across[0], across[1], 0.3 + 1.2 * ring, 0.06,
                    0.35, Colors.alpha(0.6 * fade), Colors.alpha(0.3 * fade));
        }
        double smoke = clock - HandPose.SLING_BLOWS + 6.0;
        if (smoke > 0.0 && smoke < 24.0 && clock < HandPose.GUN_BREAKS + 2) {
            Vec3 muzzle = RevolverGun.inFist(place).at(RevolverGun.MUZZLE);
            for (int k = 0; k < 5; k++) {
                double u = (smoke / 24.0 + k * 0.2) % 1.0;
                Vec3 at = muzzle.add(0.0, 0.3 + 2.0 * u, 0.0).add(Noise.direction(k, 5).scale(0.3 * u));
                painter.lightDisc(at, 0.25 + 0.6 * u, SMOKE, 0.25 * Math.sin(Math.PI * u) * strength, 0.35, k);
            }
        }
    }
}
