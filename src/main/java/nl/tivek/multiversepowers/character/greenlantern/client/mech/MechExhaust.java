package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The flames out of the exhaust pipes on the mech's forearms: a glow at their mouths while it stands, a steady flame as
// it runs or climbs, and a roaring burst as it winds up and lands a blow. Running on charges them up (MechPose.boost):
// they grow into huge roaring jets with bright shock rings down them as the mech runs ever faster. The arms say where
// their pipes end as they are drawn; the flames go on after the mech itself.
final class MechExhaust {
    private static final double LENGTH = 0.6;
    private static final double ROAR = 1.8;
    private static final double MOUTH = 0.62;
    // Fully charged, a flame is this many times as long and HUGE_WIDE times as wide.
    private static final double HUGE = 2.2;
    private static final double HUGE_WIDE = 0.8;
    private static final int DIAMONDS = 4;
    // Below this the pipes only glow at their mouths.
    private static final double LIT = 0.15;
    private static final int TONGUES = 3;
    private static final int MORE_TONGUES = 3;
    // Running, the wind bends the flames back: at most this far from their pipes' way towards straight behind, drawn
    // in BENDS strokes.
    private static final double SWEPT = 0.85;
    private static final int BENDS = 6;
    private static final List<Vec3[]> PIPES = new ArrayList<>();

    private MechExhaust() {
    }

    static void pipes(Frame forearm) {
        for (double x : MechArmShapes.PIPE_X) {
            Vec3 end = MechArmShapes.pipeEnd(x);
            Vec3 way = MechArmShapes.pipeWay(x);
            Vec3 at = forearm.at(end.x, end.y, end.z);
            PIPES.add(new Vec3[] { at, forearm.at(end.x + way.x, end.y + way.y, end.z + way.z).subtract(at) });
        }
    }

    static void flames(LanternPainter painter, MechPose pose, double t) {
        double thrust = thrust(pose, t);
        double boost = Ease.smooth(pose.boost());
        Vec3 trail = pose.stage().ahead().scale(-1.0).add(0.0, 0.2, 0.0).normalize();
        double swept = SWEPT * Mth.clamp(0.6 * pose.running() + 0.4 * boost, 0.0, 1.0);
        for (int k = 0; k < PIPES.size(); k++) {
            flame(painter, PIPES.get(k)[0], PIPES.get(k)[1].normalize(), trail, swept, thrust, boost, k);
        }
        PIPES.clear();
    }

    // One pipe's flame: a glowing cone licked by flickering tongues round a short white-hot core, all tapering off;
    // charged up (`boost`), a long roaring jet with shock rings standing in it. It leaves the pipe along it and bends,
    // the further out the more, `swept` of the way towards `trail`.
    private static void flame(LanternPainter painter, Vec3 mouth, Vec3 way, Vec3 trail, double swept, double thrust,
            double boost, int seed) {
        double time = painter.time();
        painter.flare(mouth, 0.15 + 0.3 * thrust + 0.5 * boost, 0.5 + 0.5 * thrust);
        if (thrust < LIT) {
            return;
        }
        double power = (thrust - LIT) / (1.0 - LIT);
        int flick = (int) (time * 0.7);
        double length = (LENGTH + ROAR * power) * (1.0 + HUGE * boost)
                * (0.85 + 0.15 * Math.sin(time * 1.9 + seed * 2.1));
        double mouthWide = MOUTH * (0.6 + 0.4 * power) * (1.0 + HUGE_WIDE * boost);
        Vec3[] spine = spine(mouth, way, trail, swept, length);
        Vec3[] across = Vectors.across(way);
        stroke(painter, false, spine, 1.0, mouthWide, LanternPainter.GREEN, (0.55 + 0.25 * boost) * power,
                Vec3.ZERO);
        int tongues = TONGUES + (int) Math.round(MORE_TONGUES * boost);
        for (int k = 0; k < tongues; k++) {
            double angle = Noise.of(flick + seed * 31, k, 1) * Math.PI * 2.0;
            Vec3 side = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)));
            double reach = 0.55 + 0.45 * Noise.of(flick + seed * 31, k, 2);
            stroke(painter, true, spine, reach, mouthWide * 0.45, painter.material().edge(), 0.75 * power,
                    side.scale((0.18 - 0.08 * boost) * length));
        }
        stroke(painter, true, spine, 0.35 + 0.2 * boost, mouthWide * 0.3, LanternPainter.HOT, 0.95 * power,
                Vec3.ZERO);
        if (boost <= 0.05) {
            return;
        }
        // Shock rings down the jet, bright and tight near the mouth, fading out along it; they shimmer as it roars.
        for (int k = 0; k < DIAMONDS; k++) {
            double along = (0.16 + 0.17 * k) * (1.0 + 0.04 * Math.sin(time * 2.3 + k * 1.7 + seed));
            double wide = mouthWide * (0.42 - 0.06 * k);
            double strength = boost * power * (1.0 - 0.2 * k);
            Vec3 at = at(spine, along);
            Vec3 there = at(spine, Math.min(1.0, along + 0.05)).subtract(at(spine, Math.max(0.0, along - 0.05)))
                    .normalize();
            Vec3[] ring = Vectors.across(there);
            painter.lightTaper(at.subtract(there.scale(wide * 0.9)), at.add(there.scale(wide * 0.9)), wide, 0.0,
                    LanternPainter.HOT, 0.6 * strength, 0.0);
            painter.circle(at, ring[0], ring[1], wide * 0.9, 0.04, 0.3, Colors.alpha(0.7 * strength),
                    Colors.alpha(0.3 * strength));
        }
    }

    // The flame's middle line, BENDS + 1 points from its mouth to its tip.
    private static Vec3[] spine(Vec3 mouth, Vec3 way, Vec3 trail, double swept, double length) {
        Vec3[] spine = new Vec3[BENDS + 1];
        spine[0] = mouth;
        for (int k = 1; k <= BENDS; k++) {
            Vec3 dir = way.lerp(trail, swept * Math.min(1.0, 1.5 * (k - 0.5) / BENDS));
            dir = dir.lengthSqr() < 0.04 ? way : dir.normalize();
            spine[k] = spine[k - 1].add(dir.scale(length / BENDS));
        }
        return spine;
    }

    // The point `s` of the way along the flame (0 at its mouth, 1 at its tip).
    private static Vec3 at(Vec3[] spine, double s) {
        double i = Mth.clamp(s, 0.0, 1.0) * BENDS;
        int k = Math.min(BENDS - 1, (int) i);
        return spine[k].lerp(spine[k + 1], i - k);
    }

    // The flame from its mouth to `reach` of the way along it, tapering from `wide` and `alpha` to nothing, leaning
    // further out by `lean` the further along.
    private static void stroke(LanternPainter painter, boolean light, Vec3[] spine, double reach, double wide, int rgb,
            double alpha, Vec3 lean) {
        int steps = Math.max(1, (int) Math.ceil(reach * BENDS));
        Vec3 last = spine[0];
        for (int k = 1; k <= steps; k++) {
            double f0 = (k - 1.0) / steps;
            double f1 = (double) k / steps;
            double s = reach * f1;
            Vec3 next = at(spine, s).add(lean.scale(s));
            if (light) {
                painter.lightTaper(last, next, wide * (1.0 - f0), wide * (1.0 - f1), rgb, alpha * (1.0 - f0),
                        alpha * (1.0 - f1));
            } else {
                painter.glowTaper(last, next, wide * (1.0 - f0), wide * (1.0 - f1), rgb, alpha * (1.0 - f0),
                        alpha * (1.0 - f1));
            }
            last = next;
        }
    }

    // Arms drawn with no flames after them (building, breaking up) leave none waiting.
    static void forget() {
        PIPES.clear();
    }

    private static double thrust(MechPose pose, double t) {
        double idle = 0.08 + 0.04 * Math.sin(t * 0.7) * Math.sin(t * 0.31);
        double run = Math.max(0.65 * pose.running() + 0.35 * pose.boost(), pose.climbing() ? 0.5 : 0.0);
        MechAttacks.Blow blow = pose.blow();
        double age = blow.age();
        double strike = switch (blow.kind()) {
            case MechAttacks.CROSS -> window(age, 5, MechAttacks.CROSS_HIT + 2);
            case MechAttacks.SWEEP -> window(age, 4, MechAttacks.SWEEP_TO + 2);
            case MechAttacks.STOMP -> window(age, 6, MechAttacks.STOMP_HIT + 3);
            case MechAttacks.SLAM -> window(age, 6, MechAttacks.SLAM_HIT + 3);
            case MechAttacks.THROW -> window(age, 18, MechAttacks.RELEASE + 4);
            case MechAttacks.FLY -> window(age, MechAttacks.FLY_LAUNCH, MechAttacks.FLY_FALL - 6) * 0.7;
            case MechAttacks.DIVE -> window(age, 0, MechAttacks.DIVE_LAND + 2);
            case MechAttacks.SPIN -> window(age, MechAttacks.SPIN_FROM, MechAttacks.SPIN_TO);
            default -> 0.0;
        };
        return Mth.clamp(Math.max(idle, Math.max(run, strike)), 0.0, 1.0);
    }

    // Full from `from` to `to`, easing in and out over three ticks.
    private static double window(double age, double from, double to) {
        return Ease.smooth((age - from + 3.0) / 3.0) * (1.0 - Ease.smooth((age - to) / 3.0));
    }
}
