package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechParts;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;

// The mech's gear for its missile arm and rocket boots, as everyone sees it. As the arm aims, the back of the right
// hand opens: two doors hinged at its sides swing out and a rack of six tubes rises between them, a missile's nose in
// each, their mouths ahead along the fingers (where MechMissiles fires them from); a salvo flashes out of the mouths
// and the next missiles slide in. On the rocket boots a pair of nozzles grows out of each sole and roars with flame.
final class MechGear {
    private static final double BACK = -0.3;
    private static final double RACK_DEEP = 0.62;
    private static final double MOUTH = MechArmShapes.KNUCKLES + 0.02;
    private static final double[] TUBE_X = { -0.38, 0.0, 0.38 };
    private static final double[] TUBE_Z = { -0.18, -0.44 };
    private static final double TUBE = 0.13;
    private static final double DOOR_OPEN = 2.0;
    private static final Shape DOOR = Shape.of(Mesh.bevel(-0.62, MechArmShapes.WRIST + 0.06, -0.1, 0.0,
            MechArmShapes.KNUCKLES - 0.02, 0.0, 0.03, 1.12));
    private static final Shape DOOR_LEFT = MechParts.mirrored(DOOR);
    private static final Shape RACK = Shape.of(rack());
    private static final Shape NOSES = Shape.of(noses());
    // Under each sole, behind and ahead of the middle of the foot, and how far the nozzles reach down out of it.
    private static final double[] NOZZLE_Z = { -0.55, 0.85 };
    private static final double NOZZLE_DOWN = 0.45;
    private static final Shape NOZZLE = Shape.of(nozzle());
    // The flames out of the nozzles: as long as FLAME (LAUNCH_FLAME as they ignite), RADIUS wide.
    private static final double FLAME = 4.5;
    private static final double LAUNCH_FLAME = 8.0;
    private static final double RADIUS = 0.42;

    @Nullable
    private static Frame hand;
    private static final List<Frame> SOLES = new ArrayList<>();

    private MechGear() {
    }

    // The rack: a block of six tubes, its back at z 0 on the back of the hand once risen, its mouths ahead.
    private static Mesh[] rack() {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.bevel(-0.58, MechArmShapes.WRIST + 0.12, -RACK_DEEP, 0.58, MOUTH - 0.04, 0.0, 0.06, 1.0));
        for (double x : TUBE_X) {
            for (double z : TUBE_Z) {
                m.add(Mesh.cylinder(10, TUBE + 0.05, MOUTH - 0.5, MOUTH + 0.08, 1.08).moved(x, 0.0, z));
                m.add(Mesh.torus(12, 4, TUBE + 0.04, 0.04, 1.4).moved(x, MOUTH + 0.08, z));
            }
        }
        m.add(Mesh.bevel(-0.5, MechArmShapes.WRIST + 0.2, -RACK_DEEP - 0.06, 0.5, MOUTH - 0.3, -RACK_DEEP + 0.02, 0.03,
                1.2));
        return m.toArray(Mesh[]::new);
    }

    private static Mesh[] noses() {
        List<Mesh> m = new ArrayList<>();
        for (double x : TUBE_X) {
            for (double z : TUBE_Z) {
                m.add(Mesh.cone(8, TUBE * 0.8, 0.0, MOUTH - 0.12, MOUTH + 0.14, 1.3).moved(x, 0.0, z));
            }
        }
        return m.toArray(Mesh[]::new);
    }

    // A bell nozzle reaching down out of a sole: its throat at the sole, its bell flared at the bottom.
    private static Mesh[] nozzle() {
        return new Mesh[] { Mesh.cone(12, 0.42, 0.22, -NOZZLE_DOWN, -0.05, 1.1),
                Mesh.torus(14, 4, 0.42, 0.06, 1.45).moved(0.0, -NOZZLE_DOWN, 0.0),
                Mesh.cylinder(10, 0.26, -0.08, 0.06, 1.0) };
    }

    // The pod on the back of the right hand (`hand` as the arm draws it), opened as far as the blow has it.
    static void pod(LanternPainter painter, Frame frame, MechAttacks.Blow blow, double apart, int seed) {
        hand = frame;
        double open = MechAttacks.opened(blow);
        if (open <= 0.01) {
            return;
        }
        double rise = Ease.smooth(Mth.clamp(open * 1.4 - 0.25, 0.0, 1.0));
        double swing = DOOR_OPEN * Ease.smooth(Mth.clamp(open * 1.6, 0.0, 1.0));
        double thick = Mth.clamp(open * 6.0, 0.05, 1.0);
        for (int side = -1; side <= 1; side += 2) {
            Frame door = frame.moved(side * 0.62, 0.0, BACK - 0.08).turned(0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                    -side * swing).stretched(1.0, 1.0, thick);
            MechParts.draw(painter, side > 0 ? DOOR : DOOR_LEFT, door, 1.0, apart, seed + (side > 0 ? 0 : 1));
        }
        if (rise <= 0.0) {
            return;
        }
        Frame rack = frame.moved(0.0, 0.0, BACK + RACK_DEEP * (1.0 - rise));
        if (apart < 0.0) {
            painter.clip(frame.at(0.0, 0.0, BACK), frame.forward().scale(-1.0), 1.0);
        }
        MechParts.draw(painter, RACK, rack, 1.0, apart, seed + 2);
        if (loaded(blow)) {
            MechParts.draw(painter, NOSES, rack, 1.0, apart, seed + 3);
        }
        painter.noClip();
    }

    // Whether missiles stand in the tubes: none for a moment after each salvo, none after the last.
    private static boolean loaded(MechAttacks.Blow blow) {
        int fired = MechAttacks.fired(blow);
        return fired < MechAttacks.SALVOS && MechAttacks.kick(blow) < 0.35
                && (fired == 0 || (blow.from() >>> 2) >= MechAttacks.SALVO_KICK - 2);
    }

    // Under a sole (`foot` as the leg draws it), the nozzles out as far as the blow has them.
    static void boot(LanternPainter painter, Frame foot, MechAttacks.Blow blow, double apart, int seed) {
        double out = nozzles(blow);
        if (out <= 0.01) {
            return;
        }
        Frame sole = foot.moved(0.0, -MechScript.ANKLE.y, 0.0);
        SOLES.add(sole);
        double grown = Ease.backOut(Mth.clamp(out, 0.0, 1.0));
        for (int k = 0; k < NOZZLE_Z.length; k++) {
            Frame nozzle = sole.moved(0.0, 0.0, NOZZLE_Z[k]).stretched(1.0, grown, 1.0);
            MechParts.draw(painter, NOZZLE, nozzle, 1.0, apart, seed + k);
        }
    }

    // How far the rocket boots' nozzles are out: punched out as they ignite, drawn in once their thrust is spent (or
    // as the dive plunges), so they never stand in the ground it lands on.
    private static double nozzles(MechAttacks.Blow blow) {
        double age = blow.age();
        return switch (blow.kind()) {
            case MechAttacks.FLY -> Ease.smooth((age - MechAttacks.FLY_LAUNCH + 2.0) / 3.0)
                    * (1.0 - Ease.smooth((age - MechAttacks.FLY_FALL + 6.0) / 4.0));
            case MechAttacks.DIVE -> 1.0 - Ease.smooth(age - MechAttacks.DIVE_PLUNGE + 1.0);
            default -> 0.0;
        };
    }

    // The light of both: the pod's tubes glowing, flashing out as a salvo fires; flame out of every nozzle while the
    // boots thrust.
    static void lights(LanternPainter painter, MechPose pose, boolean own) {
        MechAttacks.Blow blow = pose.blow();
        Frame frame = hand;
        double open = MechAttacks.opened(blow);
        if (frame != null && open > 0.3) {
            double kick = MechAttacks.kick(blow);
            boolean loaded = loaded(blow);
            for (double x : TUBE_X) {
                for (double z : TUBE_Z) {
                    Vec3 mouth = frame.at(x, MOUTH + 0.1, BACK + z);
                    if (loaded) {
                        painter.flare(mouth, 0.16, 0.5 * open);
                    }
                    if (kick > 0.0) {
                        painter.flare(mouth, 0.5 + 0.6 * kick, kick);
                        painter.exhaust(mouth, frame.up().normalize(), 1.6, 0.16, kick);
                    }
                }
            }
        }
        if (!MechAttacks.thrusting(blow)) {
            return;
        }
        double launch = blow.kind() == MechAttacks.FLY ? 1.0 - Ease.smooth((blow.age() - MechAttacks.FLY_LAUNCH) / 8.0)
                : 0.0;
        double reach = Mth.lerp(launch, FLAME, LAUNCH_FLAME);
        double time = painter.time();
        for (Frame sole : SOLES) {
            for (int k = 0; k < NOZZLE_Z.length; k++) {
                Vec3 mouth = sole.at(0.0, -NOZZLE_DOWN - 0.05, NOZZLE_Z[k]);
                Vec3 down = sole.up().normalize().scale(-1.0);
                double flicker = 0.9 + 0.1 * Math.sin(time * 2.9 + k * 1.7);
                painter.exhaust(mouth, down, reach * flicker, RADIUS, 0.85 + 0.15 * flicker);
                if (!own) {
                    painter.glowLine(mouth, mouth.add(down.scale(reach * 1.3)), 1.6, LanternPainter.GREEN,
                            Colors.alpha(0.12));
                }
            }
        }
    }

    static void forget() {
        hand = null;
        SOLES.clear();
    }
}
