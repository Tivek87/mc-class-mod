package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.client.HandVictims;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.fingerTip;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.handFrame;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.ringGem;

// The eye in the palm of the evil eye and the megaphone, both solid hard light, and their light: the eye's gaze and
// its strings to the puppets, the megaphone's shockwaves.
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
            case HandPose.EYE -> gaze(painter, id, pose, place, clock, strength);
            case HandPose.MEGAPHONE -> blares(painter, pose, place, clock, strength);
            default -> {
            }
        }
    }

    // The iris and a slit of white-hot pupil; wild, the pupil darts about and the eye glares. Strings of light run
    // from the fingertips to the puppets; as they turn to stone a beam from the eye strikes each.
    private static void gaze(LanternPainter painter, int id, HandPose pose, HandPose.Place place, double clock,
            double strength) {
        double open = HandPose.eyeOpen(clock);
        ConstructPainter.Frame eye = handFrame(place, false).moved(HandPose.EYE_POINT.x, HandPose.EYE_POINT.y,
                HandPose.EYE_POINT.z);
        double wild = HandPose.eyeWild(clock);
        Vec3 front = eye.at(0.0, 0.0, EYE * 0.72);
        Vec3 right = eye.right().normalize();
        Vec3 up = eye.up().normalize();
        if (open > 0.05) {
            double darting = wild * 0.22;
            int hop = (int) (clock * 0.7);
            Vec3 look = right.scale(darting * (Noise.of(id, hop, 1) - 0.5) * 2.0 * EYE)
                    .add(up.scale(darting * (Noise.of(id, hop, 2) - 0.5) * 1.4 * EYE));
            Vec3 pupil = front.add(look.scale(place.scale()));
            double size = place.scale() * EYE;
            painter.circle(pupil, right, up, 0.42 * size * (1.0 + 0.25 * wild * Math.sin(clock * 2.0)),
                    0.12 * size, 0.4 * size, Colors.alpha(open * strength), Colors.alpha(0.6 * open * strength));
            painter.lightLine(pupil.subtract(up.scale(0.36 * size)), pupil.add(up.scale(0.36 * size)),
                    (0.2 + 0.1 * wild) * size, WHITE, Colors.alpha(open * strength));
            painter.flare(pupil, (0.8 + 1.8 * wild) * size, (0.5 + 0.5 * wild) * open * strength);
        }
        List<Entity> puppets = HandVictims.puppets(id);
        if (puppets.isEmpty()) {
            return;
        }
        double strung = Ease.smooth((clock - HandPose.EYE_STRINGS) / 3.0);
        for (int k = 0; k < puppets.size(); k++) {
            Entity puppet = puppets.get(k);
            Vec3 head = puppet.getBoundingBox().getCenter().add(0.0, puppet.getBbHeight() * 0.5, 0.0);
            for (int f = 0; f < 2; f++) {
                Vec3 tip = fingerTip(pose, place, (k + f * 2) % 5);
                Vec3 end = tip.lerp(head, strung);
                painter.lightLine(tip, end, 0.09, WHITE, Colors.alpha(0.9 * strength));
                painter.glowLine(tip, end, 0.32, GLOW, Colors.alpha(0.45 * strength));
            }
            double stone = clock - HandPose.EYE_STONE;
            if (stone > -5.0 && stone < 3.0) {
                double beam = Ease.smooth((stone + 5.0) / 3.0) * (1.0 - Ease.smooth(stone / 3.0));
                painter.beamOfLight(front, puppet.getBoundingBox().getCenter(), beam * strength, clock, 0.4);
            }
        }
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
