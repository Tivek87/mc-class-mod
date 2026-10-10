package nl.tivek.multiversepowers.character.greenlantern.client.render.hand;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.JOINTS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.THICK;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.THUMB;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandRig.THUMB_THICK;

// The snapping hand as the Infinity Gauntlet in hard light: every finger bone in a plate with ridges and a lip over
// the joint, the back of the hand under a raised plate with engraved lines and rivets, an oval setting for each
// knuckle's stone and a toothed sun round the stone in the middle, and a flared, banded cuff with vents. Its six
// stones glow up as thumb and finger press and flash as they snap.
public final class HandGauntlet {
    // The back plate's outline, wrist to knuckles, in the hand's frame.
    private static final double[] BACK = { -1.45, 0.1, 1.35, 0.1, 1.58, 1.3, 1.36, 2.72, 0.42, 3.0, -0.45, 3.05,
            -1.28, 2.95, -1.64, 1.6 };
    // Where each knuckle's stone sits on the plate (index to little finger), and the middle one.
    private static final double[][] KNUCKLE_STONES = { { -1.0, 2.4 }, { -0.33, 2.5 }, { 0.36, 2.42 },
            { 0.98, 2.24 } };
    private static final Vec3 MIND = new Vec3(0.0, 1.3, -0.9);
    private static final double PLATE_FACE = -0.7;
    private static final ConstructPainter.Shape PLATE = ConstructPainter.Shape.of(plate());
    private static final ConstructPainter.Shape CUFF = ConstructPainter.Shape.of(cuff());
    private static final ConstructPainter.Shape[][] FINGER_PLATES = fingerPlates();
    private static final ConstructPainter.Shape[] THUMB_PLATES = thumbPlates();
    private static final ConstructPainter.Shape STONE = ConstructPainter.Shape.of(
            Mesh.ball(7, 4, 1.0, 1.4).scaled(1.0, 1.25, 0.55));
    // Index to little finger, then the thumb, then the back of the hand: power, space, reality, soul, time, mind.
    private static final Material[] STONES = { stone(0x9B2CFF), stone(0x2F7BFF), stone(0xFF2A2A), stone(0xFF8A1C),
            stone(0x2BFF6A), stone(0xFFDC1A) };
    private static final int[] GLOWS = { 0xB45CFF, 0x5C9CFF, 0xFF5050, 0xFFA040, 0x60FF8C, 0xFFE860 };
    private static final double STONE_SIZE = 0.27;
    private static final double MIND_SIZE = 0.42;
    // Ticks before the snap the stones start to glow up.
    private static final double STRAIN = 29.0;

    private HandGauntlet() {
    }

    private static Material stone(int rgb) {
        return new Material(rgb, brighter(rgb, 0.45), rgb, brighter(rgb, 0.85));
    }

    private static int brighter(int rgb, double u) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) (r + (255 - r) * u) << 16 | (int) (g + (255 - g) * u) << 8 | (int) (b + (255 - b) * u);
    }

    // A part built facing +z, turned to face the back of the hand (-z).
    private static Mesh back(Mesh mesh) {
        return mesh.scaled(1.0, 1.0, -1.0);
    }

    private static Mesh[] plate() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(back(Mesh.plate(0.4, -PLATE_FACE, 0.1, 1.15, BACK)));
        Vec3[] rim = new Vec3[BACK.length / 2 + 1];
        for (int i = 0; i < rim.length; i++) {
            int k = i % (BACK.length / 2);
            rim[i] = new Vec3(BACK[2 * k] * 0.97, BACK[2 * k + 1] * 0.985 + 0.02, PLATE_FACE - 0.02);
        }
        parts.add(Mesh.tube(false, 4, 0.06, 1.6, rim));
        // The sun the middle stone sits in: a raised disc, a rim and blocky rays round it.
        parts.add(Mesh.cylinder(18, 0.66, -0.08, 0.08, 1.25).alongZ().moved(MIND.x, MIND.y, PLATE_FACE - 0.06));
        parts.add(Mesh.torus(22, 5, 0.5, 0.1, 1.6).alongZ().moved(MIND.x, MIND.y, PLATE_FACE - 0.15));
        for (int k = 0; k < 12; k++) {
            double reach = k % 2 == 0 ? 0.98 : 0.86;
            parts.add(Mesh.bevel(-0.08, 0.66, -0.06, 0.08, reach, 0.06, 0.03, 1.4).turned(0.0, 0.0, 1.0, 30.0 * k)
                    .moved(MIND.x, MIND.y, PLATE_FACE - 0.06));
        }
        // A raised oval setting round each knuckle's stone.
        for (double[] at : KNUCKLE_STONES) {
            parts.add(Mesh.cylinder(14, 0.36, -0.06, 0.06, 1.2).alongZ().scaled(1.0, 1.25, 1.0)
                    .moved(at[0], at[1], PLATE_FACE - 0.04));
            parts.add(Mesh.torus(18, 4, 0.33, 0.07, 1.6).alongZ().scaled(1.0, 1.25, 1.0)
                    .moved(at[0], at[1], PLATE_FACE - 0.1));
        }
        // Engraved lines running out from the sun, and chevrons down towards the wrist.
        double z = PLATE_FACE - 0.01;
        parts.add(Mesh.tube(false, 3, 0.035, 1.5, new Vec3(-0.62, 1.0, z), new Vec3(-1.05, 0.7, z),
                new Vec3(-1.3, 0.75, z)));
        parts.add(Mesh.tube(false, 3, 0.035, 1.5, new Vec3(0.62, 1.0, z), new Vec3(1.05, 0.7, z),
                new Vec3(1.3, 0.75, z)));
        parts.add(Mesh.tube(false, 3, 0.035, 1.5, new Vec3(-0.66, 1.55, z), new Vec3(-1.2, 1.75, z),
                new Vec3(-1.45, 1.6, z)));
        parts.add(Mesh.tube(false, 3, 0.035, 1.5, new Vec3(0.66, 1.55, z), new Vec3(1.15, 1.8, z),
                new Vec3(1.42, 1.65, z)));
        for (int k = 0; k < 2; k++) {
            double y = 0.22 + 0.2 * k;
            parts.add(Mesh.tube(false, 3, 0.035, 1.5, new Vec3(-0.9, y + 0.25, z), new Vec3(0.0, y, z),
                    new Vec3(0.9, y + 0.25, z)));
        }
        // Rivets.
        double[][] rivets = { { -1.36, 0.45 }, { 1.26, 0.45 }, { -1.45, 2.3 }, { 1.36, 2.1 }, { -0.68, 0.35 },
                { 0.68, 0.35 }, { -0.66, 2.0 }, { 0.68, 1.95 }, { 0.0, 2.15 } };
        for (double[] at : rivets) {
            parts.add(Mesh.ball(6, 4, 0.07, 1.6).moved(at[0], at[1], PLATE_FACE - 0.02));
        }
        return parts.toArray(Mesh[]::new);
    }

    // The cuff over the forearm, in the arm's frame (y down the arm from the wrist, -z the back): a flared bracer
    // banded in rims, a row of vents and a raised plate on its back.
    private static Mesh[] cuff() {
        List<Mesh> parts = new ArrayList<>();
        parts.add(Mesh.lathe(22, 1.0, 0.0, -6.2, 2.05, -6.2, 2.0, -5.2, 1.82, -3.6, 1.55, -1.6, 1.38, -0.3, 1.32,
                0.3, 0.0, 0.3).scaled(1.18, 1.0, 0.82));
        double[][] rims = { { -6.2, 2.1, 0.18 }, { -5.05, 1.98, 0.1 }, { -3.4, 1.8, 0.12 }, { -1.45, 1.53, 0.1 },
                { 0.25, 1.36, 0.13 } };
        for (double[] rim : rims) {
            parts.add(Mesh.torus(26, 5, rim[1], rim[2], 1.7).scaled(1.18, 1.0, 0.82).moved(0.0, rim[0], 0.0));
        }
        for (int k = 0; k < 6; k++) {
            double y = -4.85 + 0.2 * k;
            parts.add(Mesh.box(-0.95, y, -1.66, 0.95, y + 0.08, -1.48, 1.45));
        }
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 4; k++) {
                double y = -4.75 + 0.28 * k;
                parts.add(Mesh.box(-0.07, y, -0.12, 0.07, y + 0.14, 0.12, 1.4).moved(side * 2.22, 0.0, 0.0));
            }
        }
        parts.add(back(Mesh.plate(1.12, 1.42, 0.08, 1.2, -0.85, -3.1, 0.85, -3.1, 0.6, -0.7, -0.6, -0.7)));
        parts.add(Mesh.tube(false, 3, 0.035, 1.5, new Vec3(-0.5, -2.7, -1.43), new Vec3(0.0, -2.2, -1.43),
                new Vec3(0.5, -2.7, -1.43)));
        parts.add(Mesh.tube(false, 3, 0.035, 1.5, new Vec3(-0.42, -2.1, -1.43), new Vec3(0.0, -1.6, -1.43),
                new Vec3(0.42, -2.1, -1.43)));
        return parts.toArray(Mesh[]::new);
    }

    // A plate over the back and sides of every finger bone.
    private static ConstructPainter.Shape[][] fingerPlates() {
        ConstructPainter.Shape[][] plates = new ConstructPainter.Shape[4][3];
        for (int k = 0; k < 4; k++) {
            for (int j = 0; j < 3; j++) {
                plates[k][j] = ConstructPainter.Shape.of(bonePlate(THICK[k] * (1.0 - 0.05 * j), JOINTS[k][j], j == 2,
                        false));
            }
        }
        return plates;
    }

    private static ConstructPainter.Shape[] thumbPlates() {
        ConstructPainter.Shape[] plates = new ConstructPainter.Shape[3];
        for (int j = 0; j < 3; j++) {
            plates[j] = ConstructPainter.Shape.of(bonePlate(THUMB_THICK[j], THUMB[j], j == 2, j == 0));
        }
        return plates;
    }

    // Along the bone (y) from its joint: a bevelled shell over its back and sides, two ridges down its back, a lip
    // over the joint at its end (a rounded cap on a tip), and on the thumb's root the time stone's setting.
    private static Mesh[] bonePlate(double thick, double length, boolean tip, boolean setting) {
        List<Mesh> parts = new ArrayList<>();
        double w = thick * 1.16;
        double back = -thick * 1.3;
        parts.add(Mesh.bevel(-w, 0.04, back, w, length - 0.05, thick * 0.2, 0.09, 1.2));
        for (int side = -1; side <= 1; side += 2) {
            parts.add(Mesh.box(side * thick * 0.45 - 0.04, 0.12, back - 0.05, side * thick * 0.45 + 0.04,
                    length - 0.15, back + 0.02, 1.5));
        }
        if (tip) {
            parts.add(Mesh.ball(10, 6, 1.0, 1.25).scaled(w, thick * 0.7, thick * 1.2)
                    .moved(0.0, length - 0.08, -thick * 0.45));
        } else {
            parts.add(Mesh.bevel(-w - 0.06, length - 0.2, back - 0.07, w + 0.06, length + 0.02, thick * 0.3, 0.05,
                    1.4));
        }
        if (setting) {
            double y = length * 0.5;
            parts.add(Mesh.cylinder(12, 0.34, -0.05, 0.05, 1.2).alongZ().scaled(1.0, 1.25, 1.0)
                    .moved(0.0, y, back - 0.06));
            parts.add(Mesh.torus(16, 4, 0.3, 0.06, 1.6).alongZ().scaled(1.0, 1.25, 1.0).moved(0.0, y, back - 0.12));
        }
        return parts.toArray(Mesh[]::new);
    }

    public static void parts(LanternPainter painter, HandPose pose, HandPose.Place place, double clock,
            double bright, double apart) {
        ConstructPainter.Frame hand = HandPainter.handFrame(place, false);
        ConstructPainter.Frame[] bones = HandRig.frames(hand, pose, null);
        HandPainter.part(painter, PLATE, hand, bright, apart, 140);
        HandPainter.part(painter, CUFF, HandPainter.armFrame(place, place.armForward(), false), bright, apart, 141);
        for (int k = 0; k < 4; k++) {
            for (int j = 0; j < 3; j++) {
                HandPainter.part(painter, FINGER_PLATES[k][j], bones[HandRig.bone(k, j)], bright, apart,
                        142 + 3 * k + j);
            }
        }
        for (int j = 0; j < 3; j++) {
            HandPainter.part(painter, THUMB_PLATES[j], bones[HandRig.bone(4, j)], bright, apart, 156 + j);
        }
        double since = clock - HandPose.SNAP_HITS;
        double build = Ease.smooth((since + STRAIN) / STRAIN);
        double glow = since < 0.0 ? 0.3 + 0.5 * build + 0.2 * build * Math.sin(clock * 0.9)
                : 0.4 + 0.6 * Math.exp(-since / 6.0);
        ConstructPainter.Frame[] stones = new ConstructPainter.Frame[6];
        for (int k = 0; k < 4; k++) {
            stones[k] = hand.moved(KNUCKLE_STONES[k][0], KNUCKLE_STONES[k][1], PLATE_FACE - 0.13)
                    .stretched(STONE_SIZE, STONE_SIZE, STONE_SIZE);
        }
        stones[4] = bones[HandRig.bone(4, 0)].moved(0.0, THUMB[0] * 0.5, -THUMB_THICK[0] * 1.3 - 0.12)
                .stretched(STONE_SIZE * 0.9, STONE_SIZE * 0.9, STONE_SIZE * 0.9);
        stones[5] = hand.moved(MIND.x, MIND.y, MIND.z).stretched(MIND_SIZE, MIND_SIZE, MIND_SIZE);
        Material was = painter.material();
        for (int k = 0; k < 6; k++) {
            painter.material(STONES[k]);
            HandPainter.part(painter, STONE, stones[k], bright * (1.0 + 0.6 * glow), apart, 160 + k);
        }
        painter.material(was);
        if (apart >= 0.0) {
            return;
        }
        for (int k = 0; k < 6; k++) {
            double r = (k == 5 ? MIND_SIZE : STONE_SIZE) * place.scale() * (1.0 + 1.1 * glow);
            painter.haze(stones[k].at(0.0, 0.0, -0.3), new Vec3(r, 0.0, 0.0), new Vec3(0.0, r, 0.0),
                    new Vec3(0.0, 0.0, r), GLOWS[k], 0.45 * glow * bright);
        }
    }
}
