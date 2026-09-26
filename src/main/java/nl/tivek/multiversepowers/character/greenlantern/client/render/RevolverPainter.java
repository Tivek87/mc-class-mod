package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.RevolverDuo;
import nl.tivek.multiversepowers.character.greenlantern.RevolverGun;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.GLOWS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.PORTAL_SEAM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.part;

public final class RevolverPainter {
    private static final double REACH = 30.0;
    private static final double BREAK_TICKS = 14.0;
    private static final int LEFT_PIECES = 100;
    static final int GUN_PIECES = 200;
    private static final double SHOT_SHAKE = 0.35;
    private static final double SHOT_SHAKE_TICKS = 7.0;
    private static final double SLAM_SHAKE = 0.7;
    private static final double SLAM_SHAKE_TICKS = 12.0;
    private static final double SHAKE_RANGE = 40.0;

    private RevolverPainter() {
    }

    public static void draw(LanternPainter painter, ConstructPayload show, Vec3 facing, double clock,
            @Nullable Vec3 ring) {
        Vec3 base = show.center();
        double scale = Math.max(0.1, show.size());
        Vec3 aim = base.add(facing);
        RevolverDuo duo = RevolverDuo.at(base, show.variant(), aim, clock, scale);
        if (ring != null) {
            RevolverLight.calls(painter, duo, clock, ring);
        }
        if (!painter.visible(base.add(0.0, 8.0 * scale, 0.0), REACH * scale)) {
            return;
        }
        int seed = show.id() * 7;
        RevolverLight.flashes(painter, duo, clock, 1.0);
        HandPairLight.portal(painter, duo.aPortal, seed, clock, 1.0);
        HandPairLight.portal(painter, duo.bPortal, seed + 1, clock, 1.0);
        HandPairLight.portal(painter, duo.topPortal, seed + 2, clock, 1.0);
        painter.ambient(GLOWS);
        if (duo.handsThere) {
            double gone = Math.max(1.0, RevolverDuo.HANDS_GONE - RevolverDuo.RETRACT);
            painter.glare(Math.max(0.7 * (1.0 - Ease.smooth((clock - RevolverDuo.ARRIVES) / 16.0)),
                    0.35 * Ease.smooth((clock - RevolverDuo.RETRACT) / gone)));
            hand(painter, duo, true, clock, 1.0, -1.0);
            hand(painter, duo, false, clock, 1.0, -1.0);
            painter.glare(0.0);
        }
        painter.glare(RevolverLight.propGlare(clock));
        RevolverProps.draw(painter, duo, aim, clock, -1.0);
        gun(painter, duo, clock, -1.0);
        painter.glare(0.0);
        painter.ambient(0.0);
        RevolverLight.lights(painter, seed, duo, aim, clock, 1.0);
    }

    public static boolean breaks(double clock) {
        return clock > RevolverDuo.ARRIVES && clock < RevolverDuo.HANDS_GONE - 2;
    }

    public static void broken(LanternPainter painter, ConstructPayload show, double clock, double since) {
        double apart = since / BREAK_TICKS;
        if (apart >= 1.0) {
            return;
        }
        Vec3 base = show.center();
        double scale = Math.max(0.1, show.size());
        Vec3 aim = base.add(show.facing());
        RevolverDuo duo = RevolverDuo.at(base, show.variant(), aim, clock, scale);
        int seed = show.id() * 7;
        double fade = 1.0 - Ease.smooth(since / 6.0);
        RevolverLight.flashes(painter, duo, clock, fade);
        double keep = 1.0 - Ease.smooth(apart * 1.4);
        double pop = Ease.smooth((apart - 0.45) / 0.25) * (1.0 - Ease.smooth((apart - 0.72) / 0.25));
        HandDuo.Portal[] portals = { duo.aPortal, duo.bPortal, duo.topPortal };
        for (int i = 0; i < portals.length; i++) {
            HandDuo.Portal portal = portals[i];
            HandPairLight.portal(painter, portal, seed + i, clock + since, keep);
            if (pop > 0.01 && portal.open() > 0.01) {
                painter.flare(portal.center(), portal.radius() * Math.min(1.0, portal.open()) * 0.8 * pop, pop);
            }
        }
        painter.glare(0.5 * Math.max(0.0, 1.0 - since / 5.0));
        painter.ambient(GLOWS);
        painter.fling(1.8);
        if (duo.handsThere) {
            hand(painter, duo, true, clock, 1.2, apart);
            hand(painter, duo, false, clock, 1.2, apart);
        }
        RevolverProps.draw(painter, duo, aim, clock, apart);
        gun(painter, duo, clock, apart);
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.glare(0.0);
    }

    public static float shake(ConstructPayload show, double clock, Vec3 from) {
        RevolverDuo.Stage stage = RevolverDuo.Stage.of(show.center(), show.variant(), Math.max(0.1, show.size()));
        double most = 0.0;
        double nearShow = 1.0 - from.distanceTo(show.center()) / SHAKE_RANGE;
        for (int shot : RevolverDuo.SHOTS) {
            most = Math.max(most, shaking(clock - shot, SHOT_SHAKE_TICKS, SHOT_SHAKE, nearShow));
        }
        double nearSlam = 1.0 - from.distanceTo(stage.point(RevolverDuo.SLAM_AT)) / SHAKE_RANGE;
        for (int i = 0; i < RevolverDuo.SLAMS.length; i++) {
            most = Math.max(most, shaking(clock - RevolverDuo.SLAMS[i], SLAM_SHAKE_TICKS, SLAM_SHAKE * (1.0 + 0.2 * i),
                    nearSlam));
        }
        return (float) Math.min(1.0, most);
    }

    private static double shaking(double since, double ticks, double hard, double near) {
        if (since < 0.0 || since >= ticks || near <= 0.0) {
            return 0.0;
        }
        double fade = 1.0 - since / ticks;
        return hard * fade * fade * Math.min(1.0, near * 1.3);
    }

    private static void hand(LanternPainter painter, RevolverDuo duo, boolean right, double clock, double bright,
            double apart) {
        HandDuo.Portal portal = right ? duo.aPortal : duo.bPortal;
        int seed = right ? 0 : LEFT_PIECES;
        if (RevolverDuo.dips(right, clock)) {
            HandPainter.drawHandCut(painter, right ? duo.aPose : duo.bPose, right ? duo.aPlace : duo.bPlace, !right,
                    bright, apart, seed, portal, duo.topPortal);
            return;
        }
        painter.clip(portal.center(), portal.normal(), PORTAL_SEAM);
        HandPainter.drawHand(painter, right ? duo.aPose : duo.bPose, right ? duo.aPlace : duo.bPlace, !right, bright,
                apart, seed, true);
        painter.noClip();
    }

    static ConstructPainter.Frame frame(RevolverGun.Gun gun) {
        return new ConstructPainter.Frame(gun.origin(), gun.right(), gun.up(), gun.forward(), gun.scale());
    }

    private static void gun(LanternPainter painter, RevolverDuo duo, double clock, double apart) {
        if (!RevolverGun.whole(clock)) {
            return;
        }
        ConstructPainter.Frame frame = frame(duo.gun);
        if (clock > RevolverDuo.TOSS) {
            painter.clip(duo.topPortal.center(), duo.topPortal.normal(), PORTAL_SEAM);
        }
        for (int part : new int[] { RevolverDuo.FRAME, RevolverDuo.GRIP, RevolverDuo.BARREL, RevolverDuo.GUARD }) {
            part(painter, RevolverShapes.PARTS[part], frame, 1.0, apart, GUN_PIECES + part * 20);
        }
        part(painter, RevolverShapes.PARTS[RevolverDuo.HAMMER], hammer(frame, clock), 1.0, apart,
                GUN_PIECES + RevolverDuo.HAMMER * 20);
        part(painter, RevolverShapes.PARTS[RevolverDuo.CYLINDER], cylinder(frame, clock), 1.0, apart,
                GUN_PIECES + RevolverDuo.CYLINDER * 20);
        painter.noClip();
    }

    static ConstructPainter.Frame hammer(ConstructPainter.Frame gun, double clock) {
        Vec3 pivot = RevolverShapes.HAMMER_PIVOT;
        return gun.turned(pivot.x, pivot.y, pivot.z, 1.0, 0.0, 0.0, -0.8 * RevolverGun.hammer(clock));
    }

    static ConstructPainter.Frame cylinder(ConstructPainter.Frame gun, double clock) {
        Vec3 pivot = RevolverGun.CRANE_PIVOT;
        Vec3 axis = RevolverGun.CYLINDER_AT;
        return gun.turned(pivot.x, pivot.y, 0.0, 0.0, 0.0, 1.0, RevolverGun.CRANE_OPEN * RevolverGun.crane(clock))
                .turned(axis.x, axis.y, 0.0, 0.0, 0.0, 1.0, -RevolverGun.cylinderTurn(clock));
    }
}
