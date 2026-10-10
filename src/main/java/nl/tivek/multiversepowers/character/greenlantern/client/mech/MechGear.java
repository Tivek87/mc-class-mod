package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechParts;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.fire.FireStream;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;

// The mech's gear for its missile arm, flamethrower and rocket boots, as everyone sees it. As the arm aims, the back
// of the right hand opens: two doors hinged at its sides swing out and a rack of five tubes rises between them, a
// missile's nose in each, their mouths ahead along the fingers (where MechMissiles fires them from); each click
// flashes one out of its tube, which stays empty. For the flamethrower the right hand sinks into the wrist (MechPainter)
// and a barrel telescopes out of it in its place, a tank on its back, and fire pours from its mouth (MechFlame). On the rocket boots a pair of nozzles grows out
// of each sole and roars with flame. For its helpers the doors of a hatch on its belly swing open.
final class MechGear {
    private static final double BACK = -0.3;
    private static final double RACK_DEEP = 0.62;
    private static final double MOUTH = MechArmShapes.KNUCKLES + 0.02;
    private static final double TUBE = 0.13;
    private static final double DOOR_OPEN = 2.0;
    private static final Shape DOOR = Shape.of(Mesh.bevel(-0.62, MechArmShapes.WRIST + 0.06, -0.1, 0.0,
            MechArmShapes.KNUCKLES - 0.02, 0.0, 0.03, 1.12));
    private static final Shape DOOR_LEFT = MechParts.mirrored(DOOR);
    private static final Shape RACK = Shape.of(rack());
    private static final Shape[] NOSES = noses();
    // Under each sole, behind and ahead of the middle of the foot, and how far the nozzles reach down out of it.
    private static final double[] NOZZLE_Z = { -0.55, 0.85 };
    private static final double NOZZLE_DOWN = 0.45;
    private static final Shape NOZZLE = Shape.of(nozzle());
    // The flames out of the nozzles: as long as FLAME (LAUNCH_FLAME as they ignite), RADIUS wide.
    private static final double FLAME = 4.5;
    private static final double LAUNCH_FLAME = 8.0;
    private static final double RADIUS = 0.42;
    // The flamethrower's barrel: grown from just behind the wrist, its middle this far out of the palm, its mouth
    // where the server pours the fire from.
    private static final double BARREL_FROM = MechArmShapes.WRIST - 0.5;
    private static final double BARREL_Z = 0.2;
    private static final double BARREL_MOUTH = MechScript.PALM_ALONG + MechAttacks.MUZZLE;
    // Where along the barrel its middle and its mouth sit once out; each slides there out of the piece behind it.
    private static final double MIDDLE_FROM = 1.1;
    private static final double MOUTH_FROM = 2.4;
    private static final Shape CUFF = Shape.of(cuff());
    private static final Shape MIDDLE = Shape.of(middle());
    private static final Shape MOUTH_PIECE = Shape.of(mouth());
    // Two cooling fins out of the cuff's sides, grown once the barrel is out.
    private static final Shape FIN = Shape.of(Mesh.box(0.0, 0.15, -0.05, 0.55, 1.05, 0.05, 1.3));
    private static final Shape FIN_LEFT = MechParts.mirrored(FIN);
    // The hatch under the cockpit: two doors on the belly, in the torso's places, each hinged at its outer edge.
    private static final double HATCH_HINGE = 0.95;
    private static final double HATCH_Z = 1.13;
    private static final double HATCH_OPEN = 1.9;
    private static final Shape HATCH_DOOR = Shape.of(Mesh.bevel(0.03, 7.55, HATCH_Z - 0.05, HATCH_HINGE, 8.55,
            HATCH_Z + 0.05, 0.03, 1.1));
    private static final Shape HATCH_DOOR_LEFT = MechParts.mirrored(HATCH_DOOR);

    @Nullable
    private static Frame hand;
    private static final List<Frame> SOLES = new ArrayList<>();

    private MechGear() {
    }

    // The rack: a block of five tubes, its back at z 0 on the back of the hand once risen, its mouths ahead.
    private static Mesh[] rack() {
        List<Mesh> m = new ArrayList<>();
        m.add(Mesh.bevel(-0.58, MechArmShapes.WRIST + 0.12, -RACK_DEEP, 0.58, MOUTH - 0.04, 0.0, 0.06, 1.0));
        for (int k = 0; k < MechAttacks.ROCKETS; k++) {
            m.add(Mesh.cylinder(10, TUBE + 0.05, MOUTH - 0.5, MOUTH + 0.08, 1.08).moved(tubeX(k), 0.0, tubeZ(k)));
            m.add(Mesh.torus(12, 4, TUBE + 0.04, 0.04, 1.4).moved(tubeX(k), MOUTH + 0.08, tubeZ(k)));
        }
        m.add(Mesh.bevel(-0.5, MechArmShapes.WRIST + 0.2, -RACK_DEEP - 0.06, 0.5, MOUTH - 0.3, -RACK_DEEP + 0.02, 0.03,
                1.2));
        return m.toArray(Mesh[]::new);
    }

    // A missile's crystal warhead in each tube (MechMissilePainter), one shape per tube so a fired one is left out.
    private static Shape[] noses() {
        Shape[] noses = new Shape[MechAttacks.ROCKETS];
        for (int k = 0; k < noses.length; k++) {
            noses[k] = Shape.of(Mesh.cone(4, TUBE * 0.8, 0.0, MOUTH - 0.12, MOUTH + 0.18, 1.9).moved(tubeX(k), 0.0,
                    tubeZ(k)));
        }
        return noses;
    }

    // Tube k in the rack's own frame (its back on the back of the hand).
    private static double tubeX(int k) {
        return MechAttacks.TUBE_X[k];
    }

    private static double tubeZ(int k) {
        return MechAttacks.TUBE_Z[k] - BACK;
    }

    // The flamethrower in three pieces that telescope out of the wrist, each from its own back end at y 0: a cuff with
    // a band round it, a middle narrowing over where the fist was with a tank on its back, and the flared mouth with a
    // bore in it.
    private static Mesh[] cuff() {
        return new Mesh[] { Mesh.lathe(16, 1.0, 0.0, 0.0, 0.92, 0.0, 1.0, 0.3, 1.0, 1.1, 0.9, 1.25, 0.0, 1.25),
                Mesh.torus(16, 5, 1.0, 0.08, 1.4).moved(0.0, 1.15, 0.0) };
    }

    private static Mesh[] middle() {
        double tank = -1.0 - BARREL_Z;
        return new Mesh[] { Mesh.lathe(16, 1.0, 0.0, 0.0, 0.92, 0.0, 0.88, 0.45, 0.72, 1.2, 0.7, 1.35, 0.0, 1.35),
                Mesh.torus(14, 5, 0.76, 0.07, 1.4).moved(0.0, 1.05, 0.0),
                Mesh.lathe(10, 1.1, 0.0, -0.55, 0.22, -0.5, 0.3, -0.35, 0.3, 0.85, 0.22, 1.0, 0.0, 1.05).moved(0.0, 0.0,
                        tank),
                Mesh.tube(false, 6, 0.08, 1.25, new Vec3(0.0, 1.0, tank), new Vec3(0.0, 1.35, tank + 0.2),
                        new Vec3(0.0, 1.5, -0.62)) };
    }

    private static Mesh[] mouth() {
        double mouth = BARREL_MOUTH - BARREL_FROM - MOUTH_FROM;
        return new Mesh[] { Mesh.lathe(16, 1.0, 0.0, 0.0, 0.7, 0.0, 0.66, mouth - 0.42, 0.8, mouth, 0.48, mouth, 0.42,
                mouth - 0.27, 0.0, mouth - 0.27), Mesh.torus(14, 5, 0.66, 0.09, 1.55).moved(0.0, mouth, 0.0) };
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
        for (int k = MechAttacks.fired(blow); k < MechAttacks.ROCKETS; k++) {
            MechParts.draw(painter, NOSES[k], rack, 1.0, apart, seed + 3 + k);
        }
        painter.noClip();
    }

    // The flamethrower where the right fist was (`frame`, the hand as the arm draws it, sunk into the wrist by then):
    // the cuff swells out of the wrist, the middle slides out of it and the mouth out of that, then the fins swing
    // out; shut, it folds back in the other way round.
    static void flamer(LanternPainter painter, Frame frame, MechAttacks.Blow blow, double apart, int seed) {
        double out = MechAttacks.nozzle(blow);
        if (out <= 0.01) {
            return;
        }
        Frame root = frame.moved(0.0, BARREL_FROM, BARREL_Z);
        double cuff = Ease.smooth(Mth.clamp(out / 0.4, 0.0, 1.0));
        double middle = Ease.backOut(Mth.clamp((out - 0.25) / 0.4, 0.0, 1.0));
        double mouth = Ease.backOut(Mth.clamp((out - 0.5) / 0.4, 0.0, 1.0));
        double wide = 0.55 + 0.45 * cuff;
        MechParts.draw(painter, CUFF, root.stretched(wide, Math.max(0.05, cuff), wide), 1.0, apart, seed);
        if (middle > 0.0) {
            double w = 0.75 + 0.25 * middle;
            MechParts.draw(painter, MIDDLE, root.moved(0.0, MIDDLE_FROM * middle, 0.0).stretched(w, 1.0, w), 1.0,
                    apart, seed + 1);
        }
        if (mouth > 0.0) {
            double w = 0.75 + 0.25 * mouth;
            MechParts.draw(painter, MOUTH_PIECE, root.moved(0.0, MIDDLE_FROM * middle + (MOUTH_FROM - MIDDLE_FROM)
                    * mouth, 0.0).stretched(w, 1.0, w), 1.0, apart, seed + 2);
        }
        double fins = Ease.backOut(Mth.clamp((out - 0.75) / 0.25, 0.0, 1.0));
        if (fins > 0.0) {
            for (int side = -1; side <= 1; side += 2) {
                Frame fin = root.moved(side * 0.95, 0.0, 0.0).stretched(Math.max(0.05, fins), 1.0, 1.0);
                MechParts.draw(painter, side > 0 ? FIN : FIN_LEFT, fin, 1.0, apart, seed + 3 + side);
            }
        }
    }

    // The hatch under the cockpit (`body`, the torso as the trunk is drawn): its doors grow out of the belly and swing
    // open, the right one to the right, the left one to the left.
    static void hatch(LanternPainter painter, Frame body, MechAttacks.Blow blow, double apart, int seed) {
        double open = MechAttacks.hatch(blow);
        if (open <= 0.01) {
            return;
        }
        double swing = HATCH_OPEN * Ease.smooth(Mth.clamp(open * 1.5 - 0.3, 0.0, 1.0));
        double thick = Mth.clamp(open * 6.0, 0.05, 1.0);
        for (int side = -1; side <= 1; side += 2) {
            Frame door = body.turned(side * HATCH_HINGE, 0.0, HATCH_Z, 0.0, 1.0, 0.0, side * swing);
            Frame grown = door.moved(0.0, 0.0, HATCH_Z).stretched(1.0, 1.0, thick).moved(0.0, 0.0, -HATCH_Z);
            MechParts.draw(painter, side > 0 ? HATCH_DOOR : HATCH_DOOR_LEFT, grown, 1.0, apart,
                    seed + (side > 0 ? 0 : 1));
        }
        if (apart < 0.0 && open > 0.3) {
            painter.flare(body.at(0.0, 8.05, HATCH_Z), 0.9 * open, 0.8 * open);
        }
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

    // The light of all three: the full tubes glowing, the one just fired flashing out; the flamethrower's mouth
    // alight and pouring fire towards its pilot's crosshair; flame out of every nozzle while the boots thrust.
    static void lights(LanternPainter painter, MechPose pose, int owner, boolean own) {
        MechAttacks.Blow blow = pose.blow();
        Frame frame = hand;
        if (frame != null && MechAttacks.nozzle(blow) > 0.5) {
            pour(painter, frame, pose, owner);
        }
        double open = MechAttacks.opened(blow);
        if (frame != null && open > 0.3) {
            double kick = MechAttacks.kick(blow);
            int fired = MechAttacks.fired(blow);
            for (int k = 0; k < MechAttacks.ROCKETS; k++) {
                Vec3 mouth = frame.at(MechAttacks.TUBE_X[k], MOUTH + 0.1, MechAttacks.TUBE_Z[k]);
                if (k >= fired) {
                    painter.flare(mouth, 0.16, 0.5 * open);
                } else if (k == fired - 1 && kick > 0.0) {
                    painter.flare(mouth, 0.5 + 0.6 * kick, kick);
                    painter.exhaust(mouth, frame.up().normalize(), 1.6, 0.16, kick);
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

    // A pilot light in the flamethrower's mouth; while it pours, fire out of it to as far as the setting reaches.
    private static void pour(LanternPainter painter, Frame frame, MechPose pose, int owner) {
        Vec3 mouth = frame.at(0.0, BARREL_MOUTH, BARREL_Z);
        boolean pouring = MechAttacks.pouring(pose.blow());
        painter.flare(mouth, pouring ? 0.9 : 0.35, pouring ? 0.9 : 0.6);
        Minecraft minecraft = Minecraft.getInstance();
        Entity pilot = minecraft.level == null ? null : minecraft.level.getEntity(owner);
        CharacterAbility mech = GameCharacter.GREEN_LANTERN.byName("mech");
        if (!pouring || pilot == null || mech == null) {
            return;
        }
        Vec3 aim = pose.aim();
        Vec3 way = aim == null || aim.distanceToSqr(mouth) < 1.0 ? frame.up().normalize() : aim.subtract(mouth);
        FireStream.Kind kind = FireStream.Kind.MECH;
        double now = FireStream.now(minecraft.getTimer().getGameTimeDeltaPartialTick(false));
        FireStream.feed(pilot, kind, mouth, way, now, mech.value("mechFlameReach") / (kind.life * 0.6));
    }

    static void forget() {
        hand = null;
        SOLES.clear();
    }
}
