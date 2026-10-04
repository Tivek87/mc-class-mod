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
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The flames out of the exhaust pipes on the mech's forearms: a glow at their mouths while it stands, a steady flame as
// it runs or climbs, and a roaring burst as it winds up and lands a blow. The arms say where their pipes end as they
// are drawn; the flames go on after the mech itself.
final class MechExhaust {
    private static final double LENGTH = 0.6;
    private static final double ROAR = 1.8;
    private static final double MOUTH = 0.5;
    // Below this the pipes only glow at their mouths.
    private static final double LIT = 0.15;
    private static final int TONGUES = 3;
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
        for (int k = 0; k < PIPES.size(); k++) {
            flame(painter, PIPES.get(k)[0], PIPES.get(k)[1].normalize(), thrust, k);
        }
        PIPES.clear();
    }

    // One pipe's flame: a glowing cone licked by flickering tongues round a short white-hot core, all tapering off.
    private static void flame(LanternPainter painter, Vec3 mouth, Vec3 way, double thrust, int seed) {
        double time = painter.time();
        painter.flare(mouth, 0.15 + 0.3 * thrust, 0.5 + 0.5 * thrust);
        if (thrust < LIT) {
            return;
        }
        double power = (thrust - LIT) / (1.0 - LIT);
        int flick = (int) (time * 0.7);
        double length = (LENGTH + ROAR * power) * (0.85 + 0.15 * Math.sin(time * 1.9 + seed * 2.1));
        Vec3[] across = Vectors.across(way);
        painter.glowTaper(mouth, mouth.add(way.scale(length)), MOUTH * (0.6 + 0.4 * power), 0.0,
                LanternPainter.GREEN, 0.55 * power, 0.0);
        for (int k = 0; k < TONGUES; k++) {
            double angle = Noise.of(flick + seed * 31, k, 1) * Math.PI * 2.0;
            Vec3 side = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)));
            double reach = length * (0.55 + 0.45 * Noise.of(flick + seed * 31, k, 2));
            Vec3 tip = mouth.add(way.scale(reach)).add(side.scale(0.18 * reach));
            painter.lightTaper(mouth, tip, MOUTH * 0.45, 0.0, painter.material().edge(), 0.75 * power, 0.0);
        }
        painter.lightTaper(mouth, mouth.add(way.scale(length * 0.35)), MOUTH * 0.3, 0.0, LanternPainter.HOT,
                0.95 * power, 0.0);
    }

    // Arms drawn with no flames after them (building, breaking up) leave none waiting.
    static void forget() {
        PIPES.clear();
    }

    private static double thrust(MechPose pose, double t) {
        double idle = 0.08 + 0.04 * Math.sin(t * 0.7) * Math.sin(t * 0.31);
        double run = Math.max(0.65 * pose.running(), pose.climbing() ? 0.5 : 0.0);
        MechAttacks.Blow blow = pose.blow();
        double age = blow.age();
        double strike = switch (blow.kind()) {
            case MechAttacks.SWEEP -> window(age, 4, MechAttacks.SWEEP_TO + 2);
            case MechAttacks.STOMP -> window(age, 6, MechAttacks.STOMP_HIT + 3);
            case MechAttacks.SLAM -> window(age, 6, MechAttacks.SLAM_HIT + 3);
            case MechAttacks.THROW -> window(age, 18, MechAttacks.RELEASE + 4);
            default -> 0.0;
        };
        return Mth.clamp(Math.max(idle, Math.max(run, strike)), 0.0, 1.0);
    }

    // Full from `from` to `to`, easing in and out over three ticks.
    private static double window(double age, double from, double to) {
        return Ease.smooth((age - from + 3.0) / 3.0) * (1.0 - Ease.smooth((age - to) / 3.0));
    }
}
