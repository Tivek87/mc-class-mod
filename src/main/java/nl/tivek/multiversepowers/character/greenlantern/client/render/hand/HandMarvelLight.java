package nl.tivek.multiversepowers.character.greenlantern.client.render.hand;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.HandVictims;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.fingerTip;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.handFrame;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.ringGem;

// The eye in the palm of the evil eye and the megaphone, both solid hard light, and their light: the eye's gaze, the
// puppeteer's strings to the puppets, the megaphone's shockwaves.
final class HandMarvelLight {
    private static final double EYE = HandPose.EYE_SIZE;
    private static final ConstructPainter.Shape EYEBALL = ConstructPainter.Shape.of(
            Mesh.ball(16, 10, EYE, 1.05).scaled(1.3, 0.85, 0.7),
            Mesh.torus(24, 5, EYE * 1.08, 0.1, 1.6).alongZ().scaled(1.3, 0.85, 1.0).moved(0.0, 0.0, 0.02));
    private static final ConstructPainter.Shape UPPER_LID = ConstructPainter.Shape.of(
            dome(EYE * 1.07, 1.0).scaled(1.3, 0.85, 0.72));
    private static final ConstructPainter.Shape LOWER_LID = ConstructPainter.Shape.of(
            dome(EYE * 1.07, -1.0).scaled(1.3, 0.85, 0.72));
    private static final double LID_OPENS = 1.25;
    private static final ConstructPainter.Shape HORN = ConstructPainter.Shape.of(horn());
    private static final double HORN_BREAK_TICKS = 14.0;
    private static final double RING_WAVES = 14.0;
    private static final double WAVE_SPEED = 1.1;
    private static final double WAVE_WIDEN = 0.3;
    private static final int WHITE = 0xF2FFF4;
    private static final int GLOW = 0x5CFF7A;
    private static final int ROPE_STEPS = 10;

    private HandMarvelLight() {
    }

    // Half a ball, the upper (up = 1) or lower (up = -1) one, closed at the cut.
    private static Mesh dome(double radius, double up) {
        int rings = 6;
        // Bottom to top, as a lathe runs: the flat cut and the curve, in whichever order the half needs.
        double[] profile = new double[(rings + 2) * 2];
        for (int i = 0; i <= rings + 1; i++) {
            int step = up > 0.0 ? i : rings + 1 - i;
            double angle = Math.PI * 0.5 * (step - 1) / rings;
            boolean middle = step == 0;
            profile[2 * i] = middle || step == rings + 1 ? 0.0 : Math.cos(angle) * radius;
            profile[2 * i + 1] = middle ? 0.0 : up * Math.sin(angle) * radius;
        }
        return Mesh.lathe(20, 1.1, profile);
    }

    // The megaphone: a handle across the fist, a neck up to the horn on the index-finger side, the horn along the
    // fingers with a rim round its mouth.
    private static Mesh[] horn() {
        Vec3 axis = HandPose.HORN_AXIS;
        Vec3 handle = HandPose.HORN_HANDLE;
        double mouth = HandPose.HORN_MOUTH;
        double back = HandPose.HORN_BACK;
        double bell = HandPose.HORN_BELL;
        return new Mesh[] {
                Mesh.cylinder(14, 0.3, -1.5, 1.6, 1.0).turned(0.0, 0.0, 1.0, 90.0).moved(handle.x, handle.y, handle.z),
                Mesh.box(axis.x - 0.3, handle.y - 0.35, axis.z - 0.3, axis.x + 0.3, handle.y + 0.35, axis.z + 0.3,
                        1.1),
                Mesh.lathe(22, 1.0, 0.0, back, 0.32, back, 0.36, back + 0.5, 0.48, back + 0.8, 0.62, back + 1.9,
                        0.9, bell - 1.4, 1.25, bell - 0.4, mouth, bell, mouth - 0.18, bell, 0.9, bell - 0.9, 0.0,
                        bell - 1.2).moved(axis.x, 0.0, axis.z),
                Mesh.torus(26, 6, mouth, 0.12, 1.7).moved(axis.x, bell, axis.z),
                Mesh.torus(16, 5, 0.52, 0.09, 1.6).moved(axis.x, back + 1.2, axis.z),
                Mesh.box(axis.x - 0.12, back + 0.9, axis.z - 0.72, axis.x + 0.12, back + 2.6, axis.z - 0.5, 1.3) };
    }

    // Solid parts held by the hand, drawn with it (and cut where it is cut).
    static void parts(LanternPainter painter, int variant, HandPose pose, HandPose.Place place, double clock,
            double bright, double apart) {
        switch (HandPose.move(variant)) {
            case HandPose.EYE -> eye(painter, place, clock, bright, apart);
            case HandPose.MEGAPHONE -> megaphone(painter, place, clock, bright, apart);
            default -> {
            }
        }
    }

    private static void eye(LanternPainter painter, HandPose.Place place, double clock, double bright,
            double apart) {
        Vec3 at = HandPose.EYE_POINT;
        ConstructPainter.Frame eye = handFrame(place, false).moved(at.x, at.y, at.z);
        double wild = HandPose.eyeWild(clock);
        if (wild > 0.0) {
            eye = eye.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.06 * wild * Math.sin(clock * 3.1));
        }
        double open = HandPose.eyeOpen(clock) * LID_OPENS;
        ConstructPainter.Frame upper = eye.turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -open);
        ConstructPainter.Frame lower = eye.turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, open);
        // The eyeball paler than the hand round it, so it stands out once the lids part.
        HandPainter.part(painter, EYEBALL, eye, bright * 1.6, apart, 90);
        HandPainter.part(painter, UPPER_LID, upper, bright * 1.05, apart, 93);
        HandPainter.part(painter, LOWER_LID, lower, bright * 1.05, apart, 96);
    }

    private static void megaphone(LanternPainter painter, HandPose.Place place, double clock, double bright,
            double apart) {
        double grown = HandPose.hornGrown(clock);
        if (grown <= 0.01) {
            return;
        }
        Vec3 handle = HandPose.HORN_HANDLE;
        ConstructPainter.Frame hand = handFrame(place, false);
        double broken = (clock - HandPose.HORN_BREAKS) / HORN_BREAK_TICKS;
        if (broken >= 1.0) {
            return;
        }
        if (broken > 0.0 || apart >= 0.0) {
            painter.fling(1.3);
            painter.shattered(HORN, hand, Math.max(broken, apart), bright * 1.1, 101);
            painter.fling(1.0);
            return;
        }
        // Grows out of the handle in the fist, bursting to full size.
        double size = Ease.backOut(grown);
        ConstructPainter.Frame formed = grown >= 1.0 ? hand
                : hand.moved(handle.x, handle.y, handle.z).stretched(size, size, size)
                        .moved(-handle.x, -handle.y, -handle.z);
        painter.glare(0.8 * (1.0 - grown));
        painter.shape(HORN, formed, 1.0, bright);
        painter.glare(0.0);
    }

    static void blows(LanternPainter painter, int id, int variant, HandPose pose, HandPose.Place place,
            double clock, @Nullable Vec3 ring, double strength) {
        if (strength <= 0.01) {
            return;
        }
        switch (HandPose.move(variant)) {
            case HandPose.EYE -> gaze(painter, id, place, clock, strength);
            case HandPose.PUPPETEER -> strings(painter, id, pose, place, clock, strength);
            case HandPose.MEGAPHONE -> blares(painter, pose, place, clock, strength);
            default -> {
            }
        }
    }

    // The iris, rings turning round it and a slit of white-hot pupil; opening, the eye sends out a ring of light.
    // Wild, the pupil darts about, the eye glares and searching beams sweep over the puppets; as they turn to stone a
    // beam strikes each, and as they shatter a last ring bursts out of the eye.
    private static void gaze(LanternPainter painter, int id, HandPose.Place place, double clock, double strength) {
        double open = HandPose.eyeOpen(clock);
        ConstructPainter.Frame eye = handFrame(place, false).moved(HandPose.EYE_POINT.x, HandPose.EYE_POINT.y,
                HandPose.EYE_POINT.z);
        double wild = HandPose.eyeWild(clock);
        Vec3 front = eye.at(0.0, 0.0, EYE * 0.72);
        Vec3 right = eye.right().normalize();
        Vec3 up = eye.up().normalize();
        double size = place.scale() * EYE;
        if (open > 0.05) {
            double darting = wild * 0.22;
            int hop = (int) (clock * 0.7);
            Vec3 look = right.scale(darting * (Noise.of(id, hop, 1) - 0.5) * 2.0 * EYE)
                    .add(up.scale(darting * (Noise.of(id, hop, 2) - 0.5) * 1.4 * EYE));
            Vec3 pupil = front.add(look.scale(place.scale()));
            painter.circle(pupil, right, up, 0.42 * size * (1.0 + 0.25 * wild * Math.sin(clock * 2.0)),
                    0.12 * size, 0.4 * size, Colors.alpha(open * strength), Colors.alpha(0.6 * open * strength));
            for (int k = 0; k < 2; k++) {
                double turn = clock * (0.15 + 0.25 * wild) * (k == 0 ? 1.0 : -1.4);
                Vec3 a = right.scale(Math.cos(turn)).add(up.scale(Math.sin(turn)));
                Vec3 b = right.scale(-Math.sin(turn)).add(up.scale(Math.cos(turn)));
                painter.circle(pupil, a, b.scale(0.8), (0.62 + 0.18 * k) * size, 0.04 * size, 0.2 * size,
                        Colors.alpha(0.7 * open * strength), Colors.alpha(0.3 * open * strength));
            }
            painter.lightLine(pupil.subtract(up.scale(0.36 * size)), pupil.add(up.scale(0.36 * size)),
                    (0.2 + 0.1 * wild) * size, WHITE, Colors.alpha(open * strength));
            painter.flare(pupil, (0.8 + 2.4 * wild) * size, (0.5 + 0.5 * wild) * open * strength);
        }
        double opened = clock - HandPose.EYE_OPENS;
        if (opened >= 0.0 && opened < 12.0) {
            double u = opened / 12.0;
            painter.flare(front, 4.0 * (1.0 - u) * size, (1.0 - u) * strength);
            painter.circle(front, right, up, (0.6 + 9.0 * Ease.smooth(u)) * size, 0.12, 0.9,
                    Colors.alpha(0.9 * (1.0 - u) * strength), Colors.alpha(0.45 * (1.0 - u) * strength));
        }
        List<Entity> puppets = HandVictims.glaredAt(id);
        if (wild > 0.0) {
            for (int k = 0; k < puppets.size(); k++) {
                AABB box = HandVictims.box(puppets.get(k));
                double sweep = Math.sin(clock * 0.9 + k * 2.1);
                Vec3 spot = box.getCenter().add(right.scale(0.8 * sweep)).add(0.0, 0.5 * Math.cos(clock * 1.3 + k),
                        0.0);
                painter.beamOfLight(front, spot, 0.45 * wild * strength, clock + k, 0.25);
            }
        }
        double stone = clock - HandPose.EYE_STONE;
        if (stone > -5.0 && stone < 3.0) {
            double beam = Ease.smooth((stone + 5.0) / 3.0) * (1.0 - Ease.smooth(stone / 3.0));
            for (Entity puppet : puppets) {
                Vec3 at = HandVictims.box(puppet).getCenter();
                painter.beamOfLight(front, at, beam * strength, clock, 0.6);
                painter.flare(at, 2.0 * beam, beam * strength);
            }
        }
        double shattered = clock - HandPose.EYE_SHATTERS;
        if (shattered >= 0.0 && shattered < 14.0) {
            double u = shattered / 14.0;
            painter.flare(front, 6.0 * (1.0 - u) * size, (1.0 - u) * strength);
            painter.circle(front, right, up, (1.0 + 14.0 * Ease.smooth(u)) * size, 0.18, 1.2,
                    Colors.alpha((1.0 - u) * strength), Colors.alpha(0.5 * (1.0 - u) * strength));
        }
    }

    // Three strings of light from the puppeteer's fingertips to each puppet: its head and both its hands. Slack they
    // sag, worked they pull taut and tremble; light runs down them and knots of light glow where they hold.
    private static void strings(LanternPainter painter, int id, HandPose pose, HandPose.Place place, double clock,
            double strength) {
        List<Entity> puppets = HandVictims.puppets(id);
        double pull = HandPose.puppetPull(clock);
        for (int k = 0; k < puppets.size(); k++) {
            AABB box = HandVictims.box(puppets.get(k));
            double strung = Ease.smooth((clock - HandPose.puppetStart(k)) / 3.0);
            if (strung <= 0.0) {
                continue;
            }
            Vec3[] holds = holds(puppets.get(k), box);
            for (int j = 0; j < holds.length; j++) {
                Vec3 tip = fingerTip(pose, place, (k + 2 * j) % 5);
                rope(painter, tip, holds[j], strung, pull, clock + 7.0 * k + 2.3 * j, strength);
            }
        }
    }

    // Where the strings hold a puppet: the top of its head and its two hands, turned the way its body faces.
    private static Vec3[] holds(Entity puppet, AABB box) {
        double yaw = Math.toRadians(puppet instanceof LivingEntity living ? living.yBodyRot : puppet.getYRot());
        Vec3 side = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        Vec3 middle = box.getCenter();
        double wide = box.getXsize() * 0.5 + 0.15;
        double hands = box.minY + box.getYsize() * 0.62;
        return new Vec3[] { new Vec3(middle.x, box.maxY + 0.05, middle.z),
                new Vec3(middle.x, hands, middle.z).add(side.scale(wide)),
                new Vec3(middle.x, hands, middle.z).subtract(side.scale(wide)) };
    }

    private static void rope(LanternPainter painter, Vec3 from, Vec3 to, double strung, double pull, double time,
            double strength) {
        Vec3 end = from.lerp(to, strung);
        double length = from.distanceTo(end);
        if (length < 0.05) {
            return;
        }
        Vec3 way = end.subtract(from).scale(1.0 / length);
        Vec3 side = way.cross(Vectors.UP);
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        double sag = 0.12 * length * (1.0 - pull) * strung;
        double tremble = 0.04 * pull * Math.min(1.0, length / 4.0);
        Vec3 last = from;
        for (int i = 1; i <= ROPE_STEPS; i++) {
            double u = (double) i / ROPE_STEPS;
            double bow = 4.0 * u * (1.0 - u);
            Vec3 next = from.lerp(end, u).add(0.0, -sag * bow, 0.0)
                    .add(side.scale(tremble * bow * Math.sin(u * 9.0 - time * 1.7)));
            painter.lightLine(last, next, 0.07, WHITE, Colors.alpha(0.95 * strength));
            painter.glowLine(last, next, 0.3, GLOW, Colors.alpha((0.35 + 0.2 * pull) * strength));
            last = next;
        }
        double run = (time * 0.09) % 1.0;
        Vec3 pulse = from.lerp(end, run).add(0.0, -sag * 4.0 * run * (1.0 - run), 0.0);
        painter.flare(pulse, 0.35, (0.4 + 0.5 * pull) * strength);
        painter.flare(end, strung < 1.0 ? 0.6 : 0.3, strength);
    }

    // Each blare: a flash at the bell and rings of sound racing out of it, widening as they go.
    private static void blares(LanternPainter painter, HandPose pose, HandPose.Place place, double clock,
            double strength) {
        double grown = HandPose.hornGrown(clock);
        if (grown > 0.0 && grown < 1.0) {
            Vec3 gem = ringGem(pose, place);
            painter.beamOfLight(gem, place.at(HandPose.HORN_HANDLE), (1.0 - grown) * strength, clock, 0.35);
        }
        Vec3 axis = HandPose.HORN_AXIS;
        Vec3 bell = place.at(new Vec3(axis.x, HandPose.HORN_BELL, axis.z));
        Vec3 way = place.up().normalize();
        Vec3 a = place.right().normalize();
        Vec3 b = way.cross(a).normalize();
        double scale = place.scale();
        for (int blare : HandPose.BLARES) {
            double since = clock - blare;
            if (since < 0.0 || since > RING_WAVES + 6.0) {
                continue;
            }
            if (since < 3.0) {
                painter.flare(bell, 3.0 * (1.0 - since / 3.0) * scale, strength);
            }
            for (int k = 0; k < 4; k++) {
                double run = since - k * 1.5;
                if (run < 0.0 || run > RING_WAVES) {
                    continue;
                }
                double far = run * WAVE_SPEED * scale;
                double fade = strength * (1.0 - run / RING_WAVES);
                double radius = HandPose.HORN_MOUTH * scale + far * WAVE_WIDEN;
                painter.circle(bell.add(way.scale(far)), a, b, radius, 0.1, 0.8, Colors.alpha(0.9 * fade),
                        Colors.alpha(0.45 * fade));
            }
        }
    }
}
