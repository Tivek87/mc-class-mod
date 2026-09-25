package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandLight.blows;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandLight.ground;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPair.brokenPair;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPair.pair;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.ARM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.BENDS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.CUFF;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.FINGERS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.FOREARM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.HAND;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.HOOKS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.JOINTS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.KNUCKLES;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.MIDDLE_BARE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.SPREAD;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.THUMB;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.THUMBS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.THUMB_HOOKS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.THUMB_ROOT;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandShapes.THUMB_THICK;

/**
 * The Giant Hands as everyone sees them (see {@link GiantHands}, and {@link HandPose} and {@link HandDuo} for how they
 * move). The ring's light shoots off from the ring to where a hand will come up, a ring of light glows on the ground
 * there in a haze of green light and the ground cracks, and the hand bursts up out of it, white-hot at first and
 * cooling to green. Each hand is a construct of hard light in the shape of Green Lantern's own right hand, and made in
 * detail: a forearm with a glowing gauntlet ring and seams, a glowing cuff round the wrist, a palm with its pads and
 * glowing creases, the lantern emblem, knuckles and glowing tendons on its back, four fingers of three joints each with
 * a nail on every tip, a thumb of three joints, and the ring itself on its middle finger. Nothing of it shows under the
 * ground: where it comes out of it a seam of light runs round it. Its blows leave streaks of light through the air and
 * rings running out over the ground, and a middle finger shooting up leaves a streak of light behind it; it sinks back
 * into the ground when it is done, and breaks into solid pieces, fingers and all, if its maker lets go of it.
 *
 * <p>A pair of hands with an axe comes out of two portals of the ring's light instead: rims of light turning round,
 * arms of light swirling into their middles, a green haze in them and sparks flung off them. Its right hand wears the
 * ring, its left hand is the mirror image of it, bare. Each is cut off where it comes out of its portal, with a seam of
 * light there. Its snap, its OK sign, its grip on the haft and its thumbs up flash and twinkle with light; a third
 * portal brings the axe, a battle axe of hard light that it chops down with a streak of light behind the head, a
 * flare, rings of light and cracks of light running out over the ground; left in the ground it cracks with light and
 * breaks into solid pieces.
 */
public final class HandPainter {
    // How long a hand that its maker let go of takes to break up, in ticks.
    private static final double BREAK_TICKS = 14.0;
    // How much light from within gets on top of the sky's, so its underside still reads.
    static final double GLOWS = 0.22;
    // How bright the seam of light is where a hand comes out of the ground, and where it or the axe comes out of a
    // portal.
    static final double GROUND_SEAM = 0.7;
    static final double PORTAL_SEAM = 1.0;
    // How far over the top of the ground a hand (or the axe) coming out of it is cut off, in blocks: its seam of light
    // then lies over the ground and shows however steeply it is looked down on, and the sliver of ground left under the
    // cut is far too thin to see.
    static final double GROUND_CUT = 0.015;

    private HandPainter() {
    }

    // ---- Drawing ----

    /**
     * One hand, with the ring's light on its way to it and the ground breaking open where it comes up; or a pair of
     * hands with an axe (see {@link HandDuo}).
     *
     * @param facing the flat way from its base to what it reaches for, blended between two updates
     * @param clock  ticks since it was called, by the client's own clock
     * @param ring   where its maker's ring is, or null when he is out of sight
     */
    public static void draw(LanternPainter painter, ConstructPayload hand, Vec3 facing, double clock,
            @Nullable Vec3 ring) {
        Vec3 base = hand.center();
        int variant = hand.variant();
        double scale = Math.max(0.1, hand.size());
        if (HandPose.move(variant) == HandPose.AXE) {
            pair(painter, hand, facing, clock, ring, scale);
            return;
        }
        double reach = Math.sqrt(facing.x * facing.x + facing.z * facing.z) / scale;
        HandPose pose = HandPose.at(variant, clock, reach);
        HandPose.Place place = pose.place(base, facing, scale);
        ground(painter, hand.id(), base, clock, variant, scale, 1.0);
        if (ring != null && clock < HandPose.ARRIVES + 3.0) {
            // The ring's light shoots off to where it will come up.
            double u = Ease.smooth(clock / HandPose.ARRIVES);
            double fade = 1.0 - Ease.smooth((clock - HandPose.ARRIVES) / 3.0);
            painter.beamOfLight(ring, ring.lerp(base, u), fade, clock, 0.55);
        }
        if (clock < HandPose.ARRIVES - 0.5 || !painter.visible(base, 16.0 * scale)) {
            return;
        }
        // White-hot as it bursts out of the ground, cooling to green; nothing of it shows under the ground.
        painter.glare(0.7 * (1.0 - Ease.smooth((clock - HandPose.ARRIVES) / 16.0)));
        painter.ambient(GLOWS);
        painter.clip(new Vec3(base.x, base.y + GROUND_CUT, base.z), Vectors.UP, GROUND_SEAM);
        drawHand(painter, pose, place, false, 1.0, -1.0, 0, false);
        painter.noClip();
        painter.ambient(0.0);
        painter.glare(0.0);
        blows(painter, hand, facing, clock, scale, 1.0);
    }

    /**
     * The hand in this pose: its palm, back and forearm, each finger joint by joint, and the thumb. A left hand is the
     * mirror image of the right, and bare: it has no ring.
     *
     * @param apart  below 0 for the whole hand, else how far it has broken up into solid pieces, 0 to 1
     * @param seed   the number its pieces start from as it breaks up
     * @param twists true for a hand whose wrist may twist round its forearm (a pair's): its cuff turns halfway with
     *               the hand, so the twist is shared between both sides of the cuff
     */
    static void drawHand(LanternPainter painter, HandPose pose, HandPose.Place place, boolean left,
            double bright, double apart, int seed, boolean twists) {
        ConstructPainter.Frame hand = handFrame(place, left);
        part(painter, HAND, hand, bright, apart, seed);
        // The forearm runs straight on back, however the wrist bends: down into the ground, or into its portal.
        if (twists) {
            // Where the forearm's palm side would be if the wrist only bent: the hand's palm carried onto the forearm
            // by the turn that takes the fingers' way onto the forearm's way (as HandDuo lays it). The cuff is turned
            // round the forearm halfway from the forearm's palm side to there.
            Vec3 u = place.up();
            Vec3 f = place.forward();
            Vec3 axis = u.cross(place.arm());
            double cos = u.dot(place.arm());
            Vec3 carried = f.scale(cos).add(axis.cross(f)).add(axis.scale(axis.dot(f) / (1.0 + cos)));
            double twist = Math.atan2(place.arm().dot(place.armForward().cross(carried)),
                    place.armForward().dot(carried));
            Vec3 cuffForward = Vectors.spin(place.armForward(), place.arm(), twist * 0.5);
            part(painter, FOREARM, armFrame(place, place.armForward(), left), bright, apart, seed + 20);
            part(painter, CUFF, armFrame(place, cuffForward, left), bright, apart, seed + 30);
        } else {
            part(painter, ARM, armFrame(place, place.armForward(), left), bright, apart, seed + 20);
        }
        for (int k = 0; k < 4; k++) {
            ConstructPainter.Frame[] joints = digit(hand, pose, k);
            for (int j = 0; j < 3; j++) {
                ConstructPainter.Shape shape = left && k == 1 && j == 0 ? MIDDLE_BARE : FINGERS[k][j];
                part(painter, shape, joints[j], bright, apart, seed + 40 + 3 * k + j);
            }
        }
        ConstructPainter.Frame[] thumb = digit(hand, pose, 4);
        for (int j = 0; j < 3; j++) {
            part(painter, THUMBS[j], thumb[j], bright, apart, seed + 60 + j);
        }
    }

    /**
     * The frame the forearm (or its cuff) is drawn in, its palm side facing {@code forward}: it has a right of its
     * own, square to its length, however the hand turns on the wrist (a left hand's mirrored, as the hand's).
     */
    private static ConstructPainter.Frame armFrame(HandPose.Place place, Vec3 forward, boolean left) {
        Vec3 armRight = forward.cross(place.arm());
        armRight = armRight.lengthSqr() < 1.0E-8 ? place.right() : armRight.normalize();
        return new ConstructPainter.Frame(place.wrist(), left ? armRight.scale(-1.0) : armRight, place.arm(), forward,
                place.scale());
    }

    /** The frame a hand's own shapes are drawn in: a left hand's is the right hand's mirrored across its right. */
    private static ConstructPainter.Frame handFrame(HandPose.Place place, boolean left) {
        return new ConstructPainter.Frame(place.wrist(), left ? place.right().scale(-1.0) : place.right(), place.up(),
                place.forward(), place.scale());
    }

    /** One part of a hand or the axe: whole (apart below 0), or breaking up into solid pieces. */
    static void part(LanternPainter painter, ConstructPainter.Shape shape, ConstructPainter.Frame frame,
            double bright, double apart, int seed) {
        if (apart < 0.0) {
            painter.shape(shape, frame, 1.0, bright);
        } else {
            painter.shattered(shape, frame, apart, bright, seed);
        }
    }

    /**
     * Where the three joints of a finger (0 the index finger to 3 the little finger, 4 the thumb) are in this pose: the
     * frame each one stands up along y in, from its root out. It curls, its two outer joints hook on top of that, and
     * an open hand spreads it out to the side.
     */
    private static ConstructPainter.Frame[] digit(ConstructPainter.Frame hand, HandPose pose, int k) {
        ConstructPainter.Frame[] joints = new ConstructPainter.Frame[3];
        if (k < 4) {
            double curl = pose.curl[k];
            double x = KNUCKLES[k][0];
            double y = KNUCKLES[k][1];
            ConstructPainter.Frame joint = hand.turned(x, y, 0.0, 0.0, 0.0, 1.0,
                    SPREAD[k] * pose.spread * (1.0 - curl)).turned(x, y, 0.0, 1.0, 0.0, 0.0, curl * BENDS[0])
                    .moved(x, y, 0.0);
            joints[0] = joint;
            for (int j = 1; j < 3; j++) {
                double back = JOINTS[k][j - 1];
                joint = joint.turned(0.0, back, 0.0, 1.0, 0.0, 0.0, curl * BENDS[j] + pose.hook[k] * HOOKS[j - 1])
                        .moved(0.0, back, 0.0);
                joints[j] = joint;
            }
            return joints;
        }
        double thumb = pose.curl[4];
        ConstructPainter.Frame joint = hand.turned(THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z, 0.0, 0.0, 1.0,
                Mth.lerp(thumb, 0.8, 0.12)).turned(THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z, 1.0, 0.0, 0.0,
                Mth.lerp(thumb, 0.2, 0.95)).moved(THUMB_ROOT.x, THUMB_ROOT.y, THUMB_ROOT.z);
        joints[0] = joint;
        for (int j = 1; j < 3; j++) {
            double back = THUMB[j - 1];
            joint = joint.turned(0.0, back, 0.0, 1.0, 0.0, 0.0, Mth.lerp(thumb, 0.1, j == 1 ? 0.45 : 0.5)
                    + pose.hook[4] * THUMB_HOOKS[j - 1]).turned(0.0, back, 0.0, 0.0, 0.0, 1.0,
                    Mth.lerp(thumb, 0.0, j == 1 ? -0.95 : -0.45)).moved(0.0, back, 0.0);
            joints[j] = joint;
        }
        return joints;
    }

    /** Where the tip of the middle finger of a right hand in this pose is, out in the world. */
    static Vec3 middleTip(HandPose pose, HandPose.Place place) {
        return digit(handFrame(place, false), pose, 1)[2].at(0.0, JOINTS[1][2], 0.0);
    }

    /** Where the tip of the thumb of a hand in this pose is, out in the world. */
    static Vec3 thumbTip(HandPose pose, HandPose.Place place, boolean left) {
        return digit(handFrame(place, left), pose, 4)[2].at(0.0, THUMB[2] + THUMB_THICK[2] * 0.9, 0.0);
    }

    // ---- Broken up ----

    /**
     * A hand its maker let go of before it was done: it breaks into solid pieces where it was, fingers and all, flung
     * apart, and they tumble down. A pair breaks up alike, both hands and the axe, and its portals shrink shut.
     *
     * @param since ticks since it was let go
     */
    public static void broken(LanternPainter painter, ConstructPayload hand, double clock, double since) {
        double apart = since / BREAK_TICKS;
        if (apart >= 1.0) {
            return;
        }
        Vec3 facing = hand.facing();
        Vec3 base = hand.center();
        double scale = Math.max(0.1, hand.size());
        if (HandPose.move(hand.variant()) == HandPose.AXE) {
            brokenPair(painter, hand, clock, since, apart, scale);
            return;
        }
        double reach = Math.sqrt(facing.x * facing.x + facing.z * facing.z) / scale;
        HandPose pose = HandPose.at(hand.variant(), clock, reach);
        HandPose.Place place = pose.place(base, facing, scale);
        // What light it was making dies away where it was.
        double fade = 1.0 - Ease.smooth(since / 6.0);
        ground(painter, hand.id(), base, clock, hand.variant(), scale, fade);
        blows(painter, hand, facing, clock, scale, fade);
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 5.0));
        painter.ambient(GLOWS);
        painter.fling(1.8);
        painter.clip(new Vec3(base.x, base.y + GROUND_CUT, base.z), Vectors.UP, GROUND_SEAM);
        drawHand(painter, pose, place, false, 1.2, apart, 0, false);
        painter.noClip();
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
    }

    /** How long a hand stays drawn broken up after its maker let go of it, in ticks. */
    public static int breakTicks() {
        return (int) BREAK_TICKS;
    }
}
