package nl.tivek.multiversepowers.character.greenlantern.client.render.hand;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light.HandFeatLight;
import nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light.HandMarvelLight;
import nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light.HandRingLight;
import nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light.HandTrickLight;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.HandVictims;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.duo.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.rig.BoneView;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light.HandLight.blows;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light.HandLight.ground;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPair.brokenPair;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPair.pair;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.JOINTS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.THUMB;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.THUMB_THICK;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandShapes.ARM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandShapes.CUFF;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandShapes.FINGERS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandShapes.FOREARM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandShapes.HAND;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandShapes.MIDDLE_BARE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandShapes.THUMBS;

public final class HandPainter {
    private static final double BREAK_TICKS = 14.0;
    public static final double GLOWS = 0.22;
    static final double GROUND_SEAM = 0.7;
    public static final double PORTAL_SEAM = 1.0;
    // Small enough the seam still shows, big enough to hide the ground sliver
    static final double GROUND_CUT = 0.015;
    // The hand drawn last and what it holds, so its tips and ring are found on the very fingers drawn.
    private static HandPose holding;
    private static HandRig.Held holdingBox;

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
            HandGroupPainter.draw(painter, hand.id(), variant, base, facing, clock, ring,
                    hand.held() ? LightBubble.caughtId(hand.charge()) : -1, 1.0, -1.0);
            return;
        }
        double reach = Math.sqrt(facing.x * facing.x + facing.z * facing.z) / scale;
        HandPose pose = HandPose.at(variant, clock, reach);
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
        HandPose.Place place = pose.place(base, facing, scale);
        painter.glare(0.7 * (1.0 - Ease.smooth((clock - HandPose.ARRIVES) / 16.0)));
        painter.ambient(GLOWS);
        cut(painter, base, portal, root);
        holding = pose;
        holdingBox = held(hand);
        drawHand(painter, pose, place, false, 1.0, -1.0, 0, false, holdingBox);
        HandMarvelLight.parts(painter, variant, pose, place, clock, 1.0, -1.0);
        HandRingLight.parts(painter, hand, pose, place, base, facing, clock, 1.0, -1.0);
        painter.noClip();
        painter.ambient(0.0);
        painter.glare(0.0);
        blows(painter, hand, facing, clock, scale, 1.0);
        HandTrickLight.blows(painter, variant, base, facing, clock, scale, 1.0);
        HandFeatLight.blows(painter, variant, base, facing, clock, scale, 1.0);
        HandMarvelLight.blows(painter, hand.id(), variant, pose, place, clock, ring, 1.0);
        HandRingLight.blows(painter, hand, pose, place, base, facing, clock, reach, 1.0);
    }

    // A hand out of the ground or a wall is cut at its surface, one out of a portal at the portal.
    private static void cut(LanternPainter painter, Vec3 base, @Nullable HandDuo.Portal portal, Vec3 root) {
        if (portal == null) {
            painter.clip(base.add(root.scale(GROUND_CUT)), root, GROUND_SEAM);
        } else {
            painter.clip(portal.center(), portal.normal(), PORTAL_SEAM);
        }
    }

    // The creature this hand holds at its fingers, where it is drawn this frame; one the maw swallowed is out of
    // sight, so its fist closes whole.
    @Nullable
    private static HandRig.Held held(ConstructPayload hand) {
        Minecraft minecraft = Minecraft.getInstance();
        int move = HandPose.move(hand.variant());
        if (!hand.held() || minecraft.level == null || !ClientConstructs.atFingers(move) || move == HandPose.MAW) {
            return null;
        }
        Entity caught = minecraft.level.getEntity(LightBubble.caughtId(hand.charge()));
        return caught == null ? null : HandRig.Held.of(HandVictims.box(caught));
    }

    public static void drawHand(LanternPainter painter, HandPose pose, HandPose.Place place, boolean left,
            double bright, double apart, int seed, boolean twists) {
        drawHand(painter, pose, place, left, bright, apart, seed, twists, null);
    }

    static void drawHand(LanternPainter painter, HandPose pose, HandPose.Place place, boolean left, double bright,
            double apart, int seed, boolean twists, @Nullable HandRig.Held held) {
        ConstructPainter.Frame hand = handFrame(place, left);
        part(painter, HAND, hand, bright, apart, seed);
        arm(painter, place, left, bright, apart, seed, twists);
        digits(painter, pose, hand, left, bright, apart, seed, held);
        bones(pose, place, hand, held);
    }

    // A hand reaching into another portal: its arm is cut by the portal it came from, the hand itself by the other.
    public static void drawHandCut(LanternPainter painter, HandPose pose, HandPose.Place place, boolean left,
            double bright, double apart, int seed, HandDuo.Portal from, HandDuo.Portal into) {
        painter.clip(from.center(), from.normal(), PORTAL_SEAM);
        arm(painter, place, left, bright, apart, seed, true);
        painter.clip(into.center(), into.normal(), PORTAL_SEAM);
        ConstructPainter.Frame hand = handFrame(place, left);
        part(painter, HAND, hand, bright, apart, seed);
        digits(painter, pose, hand, left, bright, apart, seed, null);
        painter.noClip();
        bones(pose, place, hand, null);
    }

    // For the developer's view: the forearm down to where it stands, a bone from the wrist to each finger's root and
    // every finger bone.
    private static void bones(HandPose pose, HandPose.Place place, ConstructPainter.Frame hand,
            @Nullable HandRig.Held held) {
        if (!BoneView.shown()) {
            return;
        }
        Vec3 wrist = place.wrist();
        if (pose.length > 0.0) {
            BoneView.bone(wrist.subtract(place.arm().scale(pose.length * place.scale())), wrist, BoneView.CONSTRUCT);
        }
        ConstructPainter.Frame[] bones = HandRig.frames(hand, pose, held);
        for (int k = 0; k < 5; k++) {
            BoneView.bone(wrist, bones[HandRig.bone(k, 0)].center(), BoneView.CONSTRUCT);
            for (int j = 0; j < 3; j++) {
                BoneView.bone(bones[HandRig.bone(k, j)], k < 4 ? JOINTS[k][j] : THUMB[j], BoneView.CONSTRUCT);
            }
        }
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
            double bright, double apart, int seed, @Nullable HandRig.Held held) {
        ConstructPainter.Frame[] bones = HandRig.frames(hand, pose, held);
        for (int k = 0; k < 4; k++) {
            for (int j = 0; j < 3; j++) {
                ConstructPainter.Shape shape = left && k == 1 && j == 0 ? MIDDLE_BARE : FINGERS[k][j];
                part(painter, shape, bones[HandRig.bone(k, j)], bright, apart, seed + 40 + 3 * k + j);
            }
        }
        for (int j = 0; j < 3; j++) {
            part(painter, THUMBS[j], bones[HandRig.bone(4, j)], bright, apart, seed + 60 + j);
        }
    }

    static ConstructPainter.Frame armFrame(HandPose.Place place, Vec3 forward, boolean left) {
        Vec3 armRight = forward.cross(place.arm());
        armRight = armRight.lengthSqr() < 1.0E-8 ? place.right() : armRight.normalize();
        return new ConstructPainter.Frame(place.wrist(), left ? armRight.scale(-1.0) : armRight, place.arm(), forward,
                place.scale());
    }

    public static ConstructPainter.Frame handFrame(HandPose.Place place, boolean left) {
        return new ConstructPainter.Frame(place.wrist(), left ? place.right().scale(-1.0) : place.right(), place.up(),
                place.forward(), place.scale());
    }

    public static void part(LanternPainter painter, ConstructPainter.Shape shape, ConstructPainter.Frame frame,
            double bright, double apart, int seed) {
        if (apart < 0.0) {
            painter.shape(shape, frame, 1.0, bright);
        } else {
            painter.shattered(shape, frame, apart, bright, seed);
        }
    }

    // Finger k's three bones (0 index to 3 little finger, 4 the thumb).
    private static ConstructPainter.Frame[] digit(ConstructPainter.Frame hand, HandPose pose, int k) {
        ConstructPainter.Frame[] bones = HandRig.frames(hand, pose, pose == holding ? holdingBox : null);
        return new ConstructPainter.Frame[] { bones[HandRig.bone(k, 0)], bones[HandRig.bone(k, 1)],
                bones[HandRig.bone(k, 2)] };
    }

    public static Vec3 middleTip(HandPose pose, HandPose.Place place) {
        return digit(handFrame(place, false), pose, 1)[2].at(0.0, JOINTS[1][2], 0.0);
    }

    public static Vec3 thumbTip(HandPose pose, HandPose.Place place, boolean left) {
        return digit(handFrame(place, left), pose, 4)[2].at(0.0, THUMB[2] + THUMB_THICK[2] * 0.9, 0.0);
    }

    // The tip of finger k (0 index to 3 little finger, 4 the thumb), as drawn.
    public static Vec3 fingerTip(HandPose pose, HandPose.Place place, int k) {
        return k == 4 ? thumbTip(pose, place, false)
                : digit(handFrame(place, false), pose, k)[2].at(0.0, JOINTS[k][2], 0.0);
    }

    public static Vec3 indexTip(HandPose pose, HandPose.Place place, boolean left) {
        return digit(handFrame(place, left), pose, 0)[2].at(0.0, JOINTS[0][2], 0.0);
    }

    // The gem of the ring on the middle finger, as drawn.
    public static Vec3 ringGem(HandPose pose, HandPose.Place place) {
        return digit(handFrame(place, false), pose, 1)[0].at(0.0, 0.62, -0.5);
    }

    public static Vec3 thumbWay(HandPose pose, HandPose.Place place, boolean left) {
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
            HandGroupPainter.draw(painter, hand.id(), hand.variant(), base, facing, clock, null, -1, fade, apart);
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
        HandRingLight.blows(painter, hand, pose, place, base, facing, clock, reach, fade);
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 5.0));
        painter.ambient(GLOWS);
        painter.fling(1.8);
        cut(painter, base, portal, root);
        drawHand(painter, pose, place, false, 1.2, apart, 0, false);
        HandMarvelLight.parts(painter, hand.variant(), pose, place, clock, 1.2, apart);
        HandRingLight.parts(painter, hand, pose, place, base, facing, clock, 1.2, apart);
        painter.noClip();
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
    }

    public static int breakTicks() {
        return (int) BREAK_TICKS;
    }
}
