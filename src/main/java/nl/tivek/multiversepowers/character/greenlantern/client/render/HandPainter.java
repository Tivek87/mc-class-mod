package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
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

public final class HandPainter {
    private static final double BREAK_TICKS = 14.0;
    static final double GLOWS = 0.22;
    static final double GROUND_SEAM = 0.7;
    static final double PORTAL_SEAM = 1.0;
    // Small enough the seam still shows, big enough to hide the ground sliver
    static final double GROUND_CUT = 0.015;

    private HandPainter() {
    }

    // Base is where the hand stands this frame: a portal hand's portal can glide between ticks.
    public static void draw(LanternPainter painter, ConstructPayload hand, Vec3 base, Vec3 facing, double clock,
            @Nullable Vec3 ring) {
        int variant = hand.variant();
        double scale = Math.max(0.1, hand.size());
        if (HandPose.move(variant) == HandPose.AXE) {
            pair(painter, hand, facing, clock, ring, scale);
            return;
        }
        if (HandGroup.is(variant)) {
            HandGroupPainter.draw(painter, hand.id(), variant, base, facing, clock, ring, 1.0, -1.0);
            return;
        }
        double reach = Math.sqrt(facing.x * facing.x + facing.z * facing.z) / scale;
        HandPose pose = HandPose.at(variant, clock, reach);
        HandPose.Place place = pose.place(base, facing, scale);
        HandDuo.Portal portal = HandPose.portal(variant) ? HandPose.portalOf(variant, base, facing, clock, scale)
                : null;
        Vec3 root = HandPose.rootNormal(variant, facing);
        if (portal == null && HandPose.wall(variant)) {
            HandFeatLight.wallBurst(painter, hand.id(), base, root, clock, variant, scale, 1.0);
        } else if (portal == null) {
            ground(painter, hand.id(), base, clock, variant, scale, 1.0);
        } else {
            HandTrickLight.portal(painter, hand.id(), portal, variant, clock, 1.0);
        }
        if (ring != null && clock < HandPose.ARRIVES + 3.0) {
            double u = Ease.smooth(clock / HandPose.ARRIVES);
            double fade = 1.0 - Ease.smooth((clock - HandPose.ARRIVES) / 3.0);
            painter.beamOfLight(ring, ring.lerp(base, u), fade, clock, 0.55);
        }
        if (clock < HandPose.ARRIVES - 0.5 || !painter.visible(base, 16.0 * scale)) {
            return;
        }
        painter.glare(0.7 * (1.0 - Ease.smooth((clock - HandPose.ARRIVES) / 16.0)));
        painter.ambient(GLOWS);
        cut(painter, base, portal, root);
        drawHand(painter, pose, place, false, 1.0, -1.0, 0, false);
        HandMarvelLight.parts(painter, variant, pose, place, clock, 1.0, -1.0);
        painter.noClip();
        painter.ambient(0.0);
        painter.glare(0.0);
        blows(painter, hand, facing, clock, scale, 1.0);
        HandTrickLight.blows(painter, variant, base, facing, clock, scale, 1.0);
        HandFeatLight.blows(painter, variant, base, facing, clock, scale, 1.0);
        HandMarvelLight.blows(painter, hand.id(), variant, pose, place, clock, ring, 1.0);
    }

    // A hand out of the ground or a wall is cut at its surface, one out of a portal at the portal.
    private static void cut(LanternPainter painter, Vec3 base, @Nullable HandDuo.Portal portal, Vec3 root) {
        if (portal == null) {
            painter.clip(base.add(root.scale(GROUND_CUT)), root, GROUND_SEAM);
        } else {
            painter.clip(portal.center(), portal.normal(), PORTAL_SEAM);
        }
    }

    static void drawHand(LanternPainter painter, HandPose pose, HandPose.Place place, boolean left,
            double bright, double apart, int seed, boolean twists) {
        ConstructPainter.Frame hand = handFrame(place, left);
        part(painter, HAND, hand, bright, apart, seed);
        arm(painter, place, left, bright, apart, seed, twists);
        digits(painter, pose, hand, left, bright, apart, seed);
    }

    // A hand reaching into another portal: its arm is cut by the portal it came from, the hand itself by the other.
    static void drawHandCut(LanternPainter painter, HandPose pose, HandPose.Place place, boolean left,
            double bright, double apart, int seed, HandDuo.Portal from, HandDuo.Portal into) {
        painter.clip(from.center(), from.normal(), PORTAL_SEAM);
        arm(painter, place, left, bright, apart, seed, true);
        painter.clip(into.center(), into.normal(), PORTAL_SEAM);
        ConstructPainter.Frame hand = handFrame(place, left);
        part(painter, HAND, hand, bright, apart, seed);
        digits(painter, pose, hand, left, bright, apart, seed);
        painter.noClip();
    }

    private static void arm(LanternPainter painter, HandPose.Place place, boolean left, double bright, double apart,
            int seed, boolean twists) {
        if (twists) {
            // Carries the palm's forward vector across the wrist bend for the cuff twist
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
    }

    private static void digits(LanternPainter painter, HandPose pose, ConstructPainter.Frame hand, boolean left,
            double bright, double apart, int seed) {
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

    private static ConstructPainter.Frame armFrame(HandPose.Place place, Vec3 forward, boolean left) {
        Vec3 armRight = forward.cross(place.arm());
        armRight = armRight.lengthSqr() < 1.0E-8 ? place.right() : armRight.normalize();
        return new ConstructPainter.Frame(place.wrist(), left ? armRight.scale(-1.0) : armRight, place.arm(), forward,
                place.scale());
    }

    static ConstructPainter.Frame handFrame(HandPose.Place place, boolean left) {
        return new ConstructPainter.Frame(place.wrist(), left ? place.right().scale(-1.0) : place.right(), place.up(),
                place.forward(), place.scale());
    }

    static void part(LanternPainter painter, ConstructPainter.Shape shape, ConstructPainter.Frame frame,
            double bright, double apart, int seed) {
        if (apart < 0.0) {
            painter.shape(shape, frame, 1.0, bright);
        } else {
            painter.shattered(shape, frame, apart, bright, seed);
        }
    }

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
                    Mth.lerp(thumb, 0.0, j == 1 ? -0.95 : -0.45) + (j == 1 ? pose.thumbOut : 0.0))
                    .moved(0.0, back, 0.0);
            joints[j] = joint;
        }
        return joints;
    }

    static Vec3 middleTip(HandPose pose, HandPose.Place place) {
        return digit(handFrame(place, false), pose, 1)[2].at(0.0, JOINTS[1][2], 0.0);
    }

    static Vec3 thumbTip(HandPose pose, HandPose.Place place, boolean left) {
        return digit(handFrame(place, left), pose, 4)[2].at(0.0, THUMB[2] + THUMB_THICK[2] * 0.9, 0.0);
    }

    // The tip of finger k (0 index to 3 little finger, 4 the thumb), as drawn.
    static Vec3 fingerTip(HandPose pose, HandPose.Place place, int k) {
        return k == 4 ? thumbTip(pose, place, false)
                : digit(handFrame(place, false), pose, k)[2].at(0.0, JOINTS[k][2], 0.0);
    }

    static Vec3 indexTip(HandPose pose, HandPose.Place place, boolean left) {
        return digit(handFrame(place, left), pose, 0)[2].at(0.0, JOINTS[0][2], 0.0);
    }

    // The gem of the ring on the middle finger, as drawn.
    static Vec3 ringGem(HandPose pose, HandPose.Place place) {
        return digit(handFrame(place, false), pose, 1)[0].at(0.0, 0.62, -0.5);
    }

    static Vec3 thumbWay(HandPose pose, HandPose.Place place, boolean left) {
        ConstructPainter.Frame tip = digit(handFrame(place, left), pose, 4)[2];
        return tip.up().normalize();
    }

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
        double fade = 1.0 - Ease.smooth(since / 6.0);
        if (HandGroup.is(hand.variant())) {
            HandGroupPainter.draw(painter, hand.id(), hand.variant(), base, facing, clock, null, fade, apart);
            return;
        }
        double reach = Math.sqrt(facing.x * facing.x + facing.z * facing.z) / scale;
        HandPose pose = HandPose.at(hand.variant(), clock, reach);
        HandPose.Place place = pose.place(base, facing, scale);
        HandDuo.Portal portal = HandPose.portal(hand.variant())
                ? HandPose.portalOf(hand.variant(), base, facing, clock, scale) : null;
        Vec3 root = HandPose.rootNormal(hand.variant(), facing);
        if (portal == null && HandPose.wall(hand.variant())) {
            HandFeatLight.wallBurst(painter, hand.id(), base, root, clock, hand.variant(), scale, fade);
        } else if (portal == null) {
            ground(painter, hand.id(), base, clock, hand.variant(), scale, fade);
        } else {
            HandTrickLight.portal(painter, hand.id(), portal, hand.variant(), clock, fade);
        }
        blows(painter, hand, facing, clock, scale, fade);
        HandTrickLight.blows(painter, hand.variant(), base, facing, clock, scale, fade);
        HandFeatLight.blows(painter, hand.variant(), base, facing, clock, scale, fade);
        HandMarvelLight.blows(painter, hand.id(), hand.variant(), pose, place, clock, null, fade);
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 5.0));
        painter.ambient(GLOWS);
        painter.fling(1.8);
        cut(painter, base, portal, root);
        drawHand(painter, pose, place, false, 1.2, apart, 0, false);
        HandMarvelLight.parts(painter, hand.variant(), pose, place, clock, 1.2, apart);
        painter.noClip();
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
    }

    public static int breakTicks() {
        return (int) BREAK_TICKS;
    }
}
