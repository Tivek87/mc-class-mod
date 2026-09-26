package nl.tivek.multiversepowers.character.greenlantern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// Hands that come as a group out of portals round one creature: four that hold it spread out while a fifth sets its
// ring on it and blasts it away, or two that clap it between them. Server and client both work them out from here.
public final class HandGroup {
    private static final double SLOW = 1.6;
    // Ring hold, in beats.
    private static final double HOLD_REACH = 3.0;
    private static final double HOLD_GRAB = 10.0;
    private static final double RING_REACH = 13.0;
    private static final double RING_PRESS = 21.0;
    private static final double RING_BLAST = 34.0;
    private static final double HOLD_BACK = 37.0;
    private static final double HOLD_GONE = 45.0;
    public static final int HOLD_GRABS = ticks(HOLD_GRAB);
    public static final int RING_PRESSES = ticks(RING_PRESS);
    public static final int RING_BLASTS = ticks(RING_BLAST);
    private static final double HOLDER_SCALE = 0.45;
    private static final double RING_SCALE = 0.7;
    private static final double LIFT = 1.2;
    // Clap, in beats.
    private static final double CLAP_OUT = 3.0;
    private static final double CLAP_AT = 17.0;
    private static final double CLAP_PART = 21.0;
    private static final double CLAP_BACK = 28.0;
    private static final double CLAP_GONE = 36.0;
    public static final int CLAP_HITS = ticks(CLAP_AT);
    private static final double CLAP_SCALE = 0.8;
    private static final double CLAP_WIDE = 6.5;
    private static final Vec3 GRIP = new Vec3(0.0, 3.05, 1.25);
    private static final Vec3 PALM = new Vec3(0.0, 1.55, 0.55);
    private static final double PORTAL = 2.6;

    public record Sub(HandPose pose, HandPose.Place place, HandDuo.Portal portal, boolean left) {
    }

    private HandGroup() {
    }

    private static int ticks(double beats) {
        return (int) Math.round(beats * SLOW);
    }

    public static boolean is(int variant) {
        int move = HandPose.move(variant);
        return move == HandPose.RINGHOLD || move == HandPose.CLAP;
    }

    private static Vec3 flat(Vec3 facing) {
        Vec3 flat = new Vec3(facing.x, 0.0, facing.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    // Where the held creature is: lifted a little once the hands have it.
    public static Vec3 held(Vec3 center, double t) {
        return center.add(0.0, LIFT * Ease.smoother((t / SLOW - HOLD_GRAB) / 4.0), 0.0);
    }

    // Where the ring touches the creature: its chest, on the caster's side.
    public static Vec3 ringPoint(Vec3 center, Vec3 facing, double t) {
        return held(center, t).subtract(flat(facing).scale(0.45)).add(0.0, 0.2, 0.0);
    }

    public static List<Sub> at(int variant, Vec3 center, Vec3 facing, double t) {
        return HandPose.move(variant) == HandPose.CLAP ? clap(center, facing, t) : ringHold(center, facing, t);
    }

    private static List<Sub> ringHold(Vec3 center, Vec3 facing, double t) {
        double beat = t / SLOW;
        Vec3 ahead = flat(facing);
        Vec3 side = ahead.cross(Vectors.UP).normalize();
        Vec3 now = held(center, t);
        double spread = Ease.smoother((beat - HOLD_GRAB - 0.5) / 3.0)
                * (1.0 - Ease.smoother((beat - RING_BLAST) / 1.0));
        double grabbed = Ease.smoother((beat - HOLD_GRAB + 1.0) / 1.4)
                * (1.0 - Ease.smoother((beat - RING_BLAST) / 0.8));
        double reach = Ease.smoother((beat - HOLD_REACH) / 6.0) * (1.0 - Ease.smoother((beat - HOLD_BACK) / 6.0));
        double open = Ease.smoother((beat - 0.5) / 2.5) * (1.0 - Ease.smoother((beat - HOLD_GONE) / 3.0));
        List<Sub> subs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double across = i % 2 == 0 ? -1.0 : 1.0;
            boolean upper = i < 2;
            Vec3 portal = center.add(side.scale(across * 3.2)).add(0.0, upper ? 2.8 : 0.2, 0.0);
            Vec3 aimAt = center.add(side.scale(across * (upper ? 0.55 : 0.35))).add(0.0, upper ? 0.55 : -0.7, 0.0);
            Vec3 grip = now.add(side.scale(across * ((upper ? 0.55 : 0.35) + 0.2 * spread)))
                    .add(0.0, upper ? 0.55 : -0.7, 0.0);
            Vec3 up = aimAt.subtract(portal).normalize();
            Vec3 palm = square(upper ? Vectors.UP.scale(-1.0) : Vectors.UP, up);
            HandPose pose = fingers(0.72 * grabbed, 0.22 * grabbed, 0.2 + 0.52 * grabbed, 0.9 - 0.85 * grabbed);
            subs.add(new Sub(pose, reaching(portal, grip, up, palm, GRIP, reach, HOLDER_SCALE),
                    portalAt(portal, up, PORTAL * HOLDER_SCALE * 1.1, open), across < 0.0));
        }
        double ringReach = Ease.smoother((beat - RING_REACH) / 7.0) * (1.0 - Ease.smoother((beat - HOLD_BACK) / 6.0))
                - 0.15 * Ease.recoil(beat - RING_BLAST, 1.0, 7.5, 0.32);
        double ringOpen = Ease.smoother((beat - (RING_REACH - 2.0)) / 2.5)
                * (1.0 - Ease.smoother((beat - HOLD_GONE) / 3.0));
        Vec3 ringPortal = center.subtract(ahead.scale(5.0)).add(0.0, 1.6, 0.0);
        Vec3 touch = ringPoint(center, facing, t).add(ahead.scale(0.1 * Ease.bump((beat - RING_PRESS) / 1.5)));
        Vec3 up = touch.subtract(ringPortal).normalize();
        Vec3 palm = square(Vectors.UP.scale(-1.0), up);
        subs.add(new Sub(fingers(1.0, 0.1, 0.9, 0.0), reaching(ringPortal, touch, up, palm, HandPose.RING_POINT,
                ringReach, RING_SCALE), portalAt(ringPortal, up, PORTAL * RING_SCALE * 1.1, ringOpen), false));
        return subs;
    }

    private static List<Sub> clap(Vec3 center, Vec3 facing, double t) {
        double beat = t / SLOW;
        Vec3 ahead = flat(facing);
        Vec3 side = ahead.cross(Vectors.UP).normalize();
        double out = Ease.smoother((beat - CLAP_OUT) / 6.0) * (1.0 - Ease.smoother((beat - CLAP_BACK) / 8.0));
        double open = Ease.smoother((beat - 0.5) / 2.5) * (1.0 - Ease.smoother((beat - CLAP_GONE) / 3.0));
        double u = (beat - CLAP_AT + 0.6) / 0.6;
        double shut = u <= 0.0 ? 0.0 : u >= 1.0 ? 1.0 : Ease.hermite(0.0, 0.0, 1.0, 2.5, u);
        double gap = Ease.keys(beat, 9.0, 3.5, 0.0, 15.0, 4.6, 0.0) * (1.0 - shut) + 0.35 * shut;
        gap = Mth.lerp(Ease.smoother((beat - CLAP_PART) / 4.0), gap, 3.5);
        double rub = 0.15 * Math.sin(beat * 2.2) * HandMotion.window(beat, CLAP_AT, CLAP_AT + 1.0, CLAP_PART - 1.0,
                CLAP_PART);
        List<Sub> subs = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            double across = i == 0 ? -1.0 : 1.0;
            Vec3 portal = center.add(side.scale(across * CLAP_WIDE)).add(0.0, 1.2, 0.0);
            Vec3 toward = side.scale(-across);
            Vec3 palmAt = center.add(side.scale(across * gap)).add(0.0, 0.3 + across * rub, 0.0);
            HandPose pose = fingers(0.05 + 0.1 * shut, 0.0, 0.3, 0.3);
            Vec3 wrist = palmAt.subtract(Vectors.UP.scale(PALM.y * CLAP_SCALE)).subtract(toward.scale(PALM.z
                    * CLAP_SCALE));
            Vec3 hidden = portal.subtract(toward.scale(4.0 * CLAP_SCALE));
            // The forearm lies level out of the portal, the wrist bent back so the palm faces the creature: the
            // forearm's own palm side faces down, never along the arm itself (that folds it flat).
            HandPose.Place place = frame(hidden.lerp(wrist, out), toward, Vectors.UP.scale(-1.0), Vectors.UP, toward,
                    CLAP_SCALE);
            subs.add(new Sub(pose, place, portalAt(portal, toward, PORTAL * CLAP_SCALE * 1.2, open), across < 0.0));
        }
        return subs;
    }

    // The clapping palms, where they meet: the middle of the creature.
    public static Vec3 clapPoint(Vec3 center) {
        return center.add(0.0, 0.3, 0.0);
    }

    // A hand whose fingers point along up, reaching out of its portal until its local point lies on the goal.
    private static HandPose.Place reaching(Vec3 portal, Vec3 goal, Vec3 up, Vec3 palm, Vec3 local, double reach,
            double scale) {
        HandPose.Place there = frame(Vec3.ZERO, up, up, palm, scale);
        Vec3 wrist = goal.subtract(there.at(local));
        Vec3 hidden = portal.subtract(up.scale(4.5 * scale));
        return frame(hidden.lerp(wrist, reach), up, up, palm, scale);
    }

    private static HandPose.Place frame(Vec3 wrist, Vec3 arm, Vec3 up, Vec3 palm, double scale) {
        return frame(wrist, arm, palm, up, palm, scale);
    }

    private static HandPose.Place frame(Vec3 wrist, Vec3 arm, Vec3 armForward, Vec3 up, Vec3 palm, double scale) {
        Vec3 right = palm.cross(up).normalize();
        return new HandPose.Place(wrist, arm, armForward, right, up, palm, scale);
    }

    private static Vec3 square(Vec3 want, Vec3 up) {
        Vec3 flat = want.subtract(up.scale(want.dot(up)));
        return flat.lengthSqr() < 1.0E-6 ? Vectors.across(up)[0] : flat.normalize();
    }

    private static HandDuo.Portal portalAt(Vec3 center, Vec3 normal, double radius, double open) {
        Vec3[] across = Vectors.across(normal);
        return new HandDuo.Portal(center, normal, across[0], across[1], radius, open);
    }

    private static HandPose fingers(double curl, double hook, double thumb, double spread) {
        HandPose pose = new HandPose();
        for (int k = 0; k < 4; k++) {
            pose.curl[k] = curl;
            pose.hook[k] = hook;
        }
        pose.curl[4] = thumb;
        pose.hook[4] = 0.3;
        pose.spread = spread;
        return pose;
    }
}
