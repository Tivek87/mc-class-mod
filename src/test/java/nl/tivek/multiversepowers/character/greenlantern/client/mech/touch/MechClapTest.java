package nl.tivek.multiversepowers.character.greenlantern.client.mech.touch;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import org.junit.jupiter.api.Test;

class MechClapTest {
    private static final double FINGER = 0.15;
    private static final double THUMB = 0.17;

    // How far the hand's pieces stay on its own side of the plane between the hands (negative: over it).
    private static double clearance(MechScript.Stage stage, boolean right, double t) {
        MechMoves.Arm arm = MechMoves.arm(right, stage, t);
        Frame hand = MechArmRig.hand(stage, arm, 0.0, 0.0);
        Frame[] bones = MechHandRig.frames(hand, arm, !right,
                MechHandRig.wall(hand, stage.base(), right ? stage.right() : stage.right().scale(-1.0)), null, null);
        double sign = right ? 1.0 : -1.0;
        double least = Double.POSITIVE_INFINITY;
        for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 3; j++) {
                Frame bone = bones[MechHandRig.bone(k, j)];
                double radius = k < 4 ? FINGER : THUMB;
                for (int i = 0; i <= 4; i++) {
                    Vec3 p = bone.at(0.0, MechHandRig.length(k, j) * i / 4.0, 0.0);
                    least = Math.min(least, sign * p.subtract(stage.base()).dot(stage.right()) - radius);
                }
            }
        }
        // The palm's inside face: its plate at the palm side, across the hand's width.
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                Vec3 p = hand.at(x * 0.32, MechArmShapes.WRIST + y * 0.62, 0.36);
                least = Math.min(least, sign * p.subtract(stage.base()).dot(stage.right()));
            }
        }
        return least;
    }

    @Test
    void clappingHandsNeverReachOverTheMiddle() {
        MechScript.Stage stage = MechScript.Stage.facing(new Vec3(12.5, 64.0, -3.25), 30.0F);
        double worst = Double.POSITIVE_INFINITY;
        double at = -1.0;
        for (double t = MechScript.SWING; t <= MechScript.RELEASE + 6.0; t += 0.25) {
            for (boolean right : new boolean[] { true, false }) {
                double c = clearance(stage, right, t);
                if (c < worst) {
                    worst = c;
                    at = t;
                }
            }
        }
        assertTrue(worst >= -MechHandRig.TOUCH - 1.0E-3, "a hand reaches " + (-worst) + " over the middle at tick " + at);
    }
}
