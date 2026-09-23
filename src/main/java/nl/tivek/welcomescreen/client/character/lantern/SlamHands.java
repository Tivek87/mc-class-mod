package nl.tivek.welcomescreen.client.character.lantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.character.lantern.LandingSlam;
import nl.tivek.welcomescreen.client.character.lantern.ConstructPainter.Frame;
import nl.tivek.welcomescreen.client.character.lantern.ConstructPainter.Shape;
import nl.tivek.welcomescreen.client.character.lantern.SlamPainter.Moment;

/**
 * The landing-slam constructs made of hands (see {@link SlamPainter}): the fist out of the sky, two hands that clap,
 * two fists that bump, the uppercut and the slapping hand. A right hand wears his ring on its middle finger.
 */
final class SlamHands {
    private SlamHands() {
    }

    // The width of the fist model at scale 1, in blocks, and how far its knuckles reach in front of its middle.
    private static final double FIST_WIDTH = 1.23;
    private static final double FIST_REACH = 0.57;

    /** The arm above the fist out of the sky, from its wrist up along -z, with a gauntlet flaring out at its end. */
    private static final Shape SKY_ARM = new Shape(new double[][] {
            { -0.35, -0.26, -3.20, 0.35, 0.24, -0.86, 0.95 } },
            Mesh.cone(16, 0.40, 0.58, 0.0, 0.70, 1.1).pointing(0.0, 0.0, -1.0).scaled(1.0, 0.75, 1.0)
                    .moved(0.0, -0.01, -2.30),
            Mesh.torus(16, 6, 0.40, 0.05, 1.3).alongZ().scaled(1.0, 0.75, 1.0).moved(0.0, -0.01, -2.30));
    /** The forearm behind the fist of an uppercut, from the wrist far down along -z (into the ground). */
    private static final double[][] FOREARM = {
            { -0.35, -0.26, -4.60, 0.35, 0.24, -0.86, 0.95 },
            { -0.38, -0.29, -2.40, 0.38, 0.27, -2.20, 1.15 } };
    /** The forearm behind a fist that bumps another: from its wrist back along -z, a band round it. */
    private static final Shape BUMP_ARM = new Shape(new double[][] {
            { -0.35, -0.26, -2.60, 0.35, 0.24, -0.86, 0.95 },
            { -0.38, -0.29, -1.90, 0.38, 0.27, -1.75, 1.15 } });

    // ---- The open hand ----

    /**
     * The palm of an open right hand, fingers up, palm facing -x, the back of the hand +x and the thumb towards +z:
     * the palm, the heel of the hand, pads under the thumb and the little finger, knuckles on its back, the wrist, a
     * cuff round it and the forearm.
     */
    private static final Shape PALM = new Shape(new double[][] {
            { -0.12, -0.48, -0.40, 0.12, 0.30, 0.40, 1.0 },
            { -0.15, -0.62, -0.36, 0.14, -0.40, 0.36, 0.95 },
            { -0.13, -0.90, -0.30, 0.13, -0.62, 0.30, 0.9 } },
            Mesh.ball(10, 6, 1.0, 1.0).scaled(0.12, 0.26, 0.16).moved(-0.08, -0.30, 0.28),
            Mesh.ball(10, 6, 1.0, 1.0).scaled(0.08, 0.22, 0.12).moved(-0.08, -0.30, -0.30),
            knuckle(0.30), knuckle(0.10), knuckle(-0.10), knuckle(-0.29),
            Mesh.torus(16, 6, 0.30, 0.06, 1.25).scaled(0.55, 1.0, 1.0).moved(0.0, -0.96, 0.0),
            Mesh.cylinder(12, 0.30, -1.45, -0.96, 0.9).scaled(0.5, 1.0, 1.0));
    // The fingers, from the index finger to the little finger: where along z they grow out of the palm, how thick
    // they are, and how long each of their three bones is.
    private static final double[][] FINGERS = {
            { 0.30, 0.085, 0.36, 0.24, 0.20 },
            { 0.10, 0.088, 0.40, 0.27, 0.21 },
            { -0.10, 0.085, 0.37, 0.25, 0.20 },
            { -0.29, 0.075, 0.29, 0.20, 0.17 } };
    // The thumb: how thick it is and how long its bones are.
    private static final double[] THUMB = { 0.10, 0.26, 0.22, 0.18 };
    /** Every bone of every finger (the thumb last), standing up along y from its joint. */
    private static final Shape[][] BONES = bones();
    /** His ring on the middle finger of an open right hand: a band round its first bone and a gem on its back. */
    private static final Shape HAND_RING = Shape.of(Mesh.torus(12, 5, 0.105, 0.025, 1.25).moved(0.0, 0.16, 0.0),
            Mesh.ball(10, 6, 0.06, 1.6).scaled(0.6, 1.0, 1.0).moved(0.13, 0.16, 0.0));

    private static Mesh knuckle(double z) {
        return Mesh.ball(8, 5, 0.075, 1.1).moved(0.10, 0.30, z);
    }

    private static Shape[][] bones() {
        Shape[][] bones = new Shape[5][3];
        for (int f = 0; f < 5; f++) {
            double r = f < 4 ? FINGERS[f][1] : THUMB[0];
            for (int b = 0; b < 3; b++) {
                double length = f < 4 ? FINGERS[f][2 + b] : THUMB[1 + b];
                double thick = r * (1.0 - 0.06 * b);
                Mesh bone = Mesh.lathe(10, 1.0, 0.0, -0.01, thick * 0.75, -0.01, thick, thick * 0.35, thick,
                        length - thick * 0.35, thick * 0.75, length + 0.01, 0.0, length + 0.01).scaled(0.9, 1.0, 1.0);
                if (b < 2) {
                    bones[f][b] = Shape.of(bone);
                } else {
                    // The last bone has a nail on its back.
                    bones[f][b] = new Shape(new double[][] { { thick * 0.35, length - 0.16, -thick * 0.6,
                            thick * 0.9 + 0.012, length - 0.02, thick * 0.6, 1.25 } }, bone);
                }
            }
        }
        return bones;
    }

    /**
     * An open hand (see {@link #PALM}) at {@code frame}, its fingers bent towards the palm.
     *
     * @param curl how far each finger is bent at each joint, in radians: the index finger to the little finger and
     *             then the thumb
     * @param ring whether it wears his ring: only a right hand does
     */
    private static void hand(ConstructPainter painter, Frame frame, Moment m, double[] curl, boolean ring) {
        SlamPainter.piece(painter, PALM, frame, m);
        for (int f = 0; f < 5; f++) {
            Frame joint;
            if (f < 4) {
                joint = frame.moved(0.0, 0.30, FINGERS[f][0]);
            } else {
                // The thumb grows out of the side of the palm, leaning out towards +z and a little over the palm.
                joint = frame.moved(-0.04, -0.22, 0.40).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Math.toRadians(40.0))
                        .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Math.toRadians(15.0));
            }
            for (int b = 0; b < 3; b++) {
                // Bending towards the palm, -x, turns about z; the middle joint bends furthest.
                joint = joint.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, curl[f] * (b == 1 ? 1.2 : 0.9));
                SlamPainter.piece(painter, BONES[f][b], joint, m);
                if (ring && f == 1 && b == 0) {
                    SlamPainter.piece(painter, HAND_RING, joint, m);
                }
                double length = f < 4 ? FINGERS[f][2 + b] : THUMB[1 + b];
                joint = joint.moved(0.0, length, 0.0);
            }
        }
    }

    /** The same bend for all four fingers, and the thumb bent this far. */
    private static double[] bent(double fingers, double thumb) {
        return new double[] { fingers, fingers, fingers, fingers, thumb };
    }

    // ---- The constructs ----

    /** The fist out of the sky: knuckles down, the back of the hand towards him, its arm up behind it. */
    static Vec3 skyFist(ConstructPainter painter, Moment m) {
        double s = m.scale(3.3 / FIST_WIDTH);
        double height = SlamPainter.drop(m) - (m.struck() ? 0.15 * s : 0.0);
        Vec3 towards = m.forward().scale(-1.0);
        Frame frame = new Frame(m.ground().add(0.0, FIST_REACH * s + height, 0.0), SlamPainter.DOWN.cross(towards),
                towards, SlamPainter.DOWN, s);
        SlamPainter.marker(painter, m, 1.5);
        fist(painter, frame, m, true);
        SlamPainter.piece(painter, SKY_ARM, frame, m);
        return frame.at(0.0, 0.0, -3.2);
    }

    /** A fist and, on a right one, his ring. */
    private static void fist(ConstructPainter painter, Frame frame, Moment m, boolean ring) {
        SlamPainter.piece(painter, ConstructPainter.fistModel(), frame, m);
        if (ring) {
            SlamPainter.piece(painter, ConstructPainter.FIST_RING, frame, m);
        }
    }

    /**
     * Two open hands, a left and a right, that clap together in front of him: their fingers bend a little as they
     * swing in, snap straight as they meet, and flex once after.
     */
    static Vec3 hands(ConstructPainter painter, Moment m) {
        double s = m.scale(1.6);
        Vec3 clap = m.ground().add(0.0, 1.9 * m.size(), 0.0);
        // Wide apart as they take shape, drawn back a little further as they wind up, then together.
        double apart = 0.14 * s + (3.2 + 0.7 * m.windup()) * (1.0 - m.fall()) + SlamPainter.vibrate(m, 0.06);
        Vec3 across = SlamPainter.facingHim(m.right());
        Vec3 back = SlamPainter.facingHim(m.forward().scale(-1.0));
        double bend = m.struck() ? 0.3 * Math.max(0.0, Math.sin(0.8 * m.since())) * Math.exp(-0.25 * m.since())
                : 0.3 * (1.0 - m.fall());
        double[] curl = bent(bend, 0.5 * bend);
        hand(painter, new Frame(clap.add(across.scale(apart)), across, SlamPainter.UP, back, s), m, curl, true);
        hand(painter, new Frame(clap.subtract(across.scale(apart)), across.scale(-1.0), SlamPainter.UP, back, s), m,
                curl, false);
        clapRings(painter, clap, across, m, s);
        return clap;
    }

    /**
     * Where two things clap together, the air bursts out: a ring of light, and a second one after it, spreading out
     * across the way they came together. Light, not a construct.
     */
    static void clapRings(ConstructPainter painter, Vec3 at, Vec3 across, Moment m, double s) {
        if (!m.struck()) {
            return;
        }
        Vec3 side = across.cross(SlamPainter.UP).normalize();
        for (int k = 0; k < 2; k++) {
            double age = m.since() - 1.5 * k;
            if (age > 0.0 && age < 7.0) {
                double fade = 1.0 - age / 7.0;
                painter.circle(at, side, SlamPainter.UP, (0.6 + 0.5 * age) * s, 0.07, 0.35,
                        ConstructPainter.alpha(0.9 * fade), ConstructPainter.alpha(0.45 * fade));
            }
        }
        if (m.since() < 4.0) {
            painter.flare(at, (0.8 + 0.6 * (1.0 - m.since() / 4.0)) * s, 1.0 - m.since() / 4.0);
        }
    }

    /** Two fists that bump knuckles in front of him, each with its forearm behind it; his ring on the right one. */
    static Vec3 fists(ConstructPainter painter, Moment m) {
        double s = m.scale(2.4 / FIST_WIDTH);
        Vec3 meet = m.ground().add(0.0, 1.7 * m.size(), 0.0);
        double apart = FIST_REACH * s + (3.0 + 0.7 * m.windup()) * (1.0 - m.fall()) + SlamPainter.vibrate(m, 0.05);
        Vec3 right = SlamPainter.facingHim(m.right());
        Vec3 in = right.scale(-1.0);
        Frame rightFist = new Frame(meet.add(right.scale(apart)), in.cross(SlamPainter.UP), SlamPainter.UP, in, s);
        fist(painter, rightFist, m, true);
        SlamPainter.piece(painter, BUMP_ARM, rightFist, m);
        // The left fist: the same fist, mirrored, so its thumb is on the other side.
        Frame leftFist = new Frame(meet.subtract(right.scale(apart)), right.cross(SlamPainter.UP).scale(-1.0),
                SlamPainter.UP, right, s);
        fist(painter, leftFist, m, false);
        SlamPainter.piece(painter, BUMP_ARM, leftFist, m);
        clapRings(painter, meet, right, m, s);
        return meet;
    }

    /**
     * The uppercut. First the ground cracks open where it will come out, brighter and brighter, and rumbles; then a
     * giant fist bursts up out of it knuckles first, its arm behind it, fastest as it breaks through (that is the
     * strike) and slowing down towards the top, while chunks of ground fly up around it. It hangs up there a moment,
     * and at the end it sinks back into the ground the way it came.
     */
    static Vec3 uppercut(ConstructPainter painter, Moment m) {
        Vec3 ground = m.ground();
        double s = 3.0 / FIST_WIDTH * m.size();
        double warn = Mth.clamp(m.t() / (LandingSlam.IMPACT_TICK - 1.0), 0.0, 1.0);
        SlamPainter.cracks(painter, ground, (1.2 + 1.6 * warn) * m.size(),
                m.struck() ? 1.0 - m.since() / 12.0 : 0.35 + 0.65 * warn, 7);
        SlamPainter.buildUp(painter, ground, 1.8 * m.size(), m, 7);
        double out = Mth.clamp((m.t() - (LandingSlam.IMPACT_TICK - 0.6)) / 3.5, 0.0, 1.0);
        double rise = 1.0 - Math.pow(1.0 - out, 2.2);
        double back = ConstructPainter.smooth((m.t() - LandingSlam.BURST_TICK) / 7.0);
        double knuckles = (Mth.lerp(rise, -0.8, 4.4) - back * 6.0) * m.size();
        Vec3 towards = m.forward().scale(-1.0);
        Frame frame = new Frame(ground.add(0.0, knuckles - FIST_REACH * s, 0.0), SlamPainter.UP.cross(towards),
                towards, SlamPainter.UP, s);
        if (knuckles > -1.5 * m.size()) {
            painter.model(ConstructPainter.fistModel(), frame, 1.0, m.bright());
            painter.shape(ConstructPainter.FIST_RING, frame, 1.0, m.bright());
            painter.model(FOREARM, frame, 1.0, m.bright());
        }
        SlamPainter.rubble(painter, ground, LandingSlam.IMPACT_TICK - 0.5, m.t(), 12, 0.34, 0.42, 3);
        // Before it comes out, the ring pours its light into the ground where it will.
        return out > 0.0 ? frame.at(0.0, 0.0, -1.6) : ground;
    }

    /**
     * A giant open right hand that drops palm down, its fingers pointing away from him, and slaps the ground flat;
     * then its fingers drum on the ground one after the other.
     */
    static Vec3 palm(ConstructPainter painter, Moment m) {
        double s = m.scale(2.2);
        double height = SlamPainter.drop(m) + 0.16 * s - (m.struck() ? 0.08 * s : 0.0);
        // The hand's own x (the back of the hand) is up, its y (the fingers) ahead, its z (the thumb) to his left.
        Frame frame = new Frame(m.ground().add(0.0, height, 0.0), SlamPainter.UP, m.forward(),
                m.right().scale(-1.0), s);
        double[] curl = new double[5];
        double since = m.since();
        for (int f = 0; f < 4; f++) {
            // Bent up off the ground while it falls, flat as it slaps, then each finger taps in turn.
            curl[f] = !m.struck() ? -0.15 * (1.0 - m.fall())
                    : -0.35 * Math.max(0.0, Math.sin(1.8 * (since - 1.0) - 0.8 * f)) * Math.exp(-0.15 * since)
                            * (since > 1.0 ? 1.0 : 0.0);
        }
        SlamPainter.marker(painter, m, 2.2);
        hand(painter, frame, m, curl, true);
        return frame.at(0.0, -1.2, 0.0);
    }
}
