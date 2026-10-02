package nl.tivek.multiversepowers.character.greenlantern.client.mech.touch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import org.junit.jupiter.api.Test;

class MechArmRigTest {
    private static final MechScript.Stage TORSO = MechScript.Stage.facing(new Vec3(4.5, 70.0, -2.5), 37.0F);

    private static void same(Vec3 a, Vec3 b, double within, String what) {
        assertTrue(a.distanceTo(b) <= within, what + ": " + a + " against " + b);
    }

    // A forearm found again for a shoulder its shrug has moved: the upper arm keeps its length, the hand stays exactly
    // where it was, turned at the wrist to fit, and a wrist out of reach is drawn in with the hand turned as before.
    @Test
    void aMovedShoulderLeavesTheHandAndTheUpperArmAsTheyWere() {
        int reached = 0;
        for (boolean right : new boolean[] { true, false }) {
            Vec3 shoulder = TORSO.point(MechPainter.side(MechScript.SHOULDER, right));
            for (int k = 0; k < 24; k++) {
                MechMoves.Arm arm = MechMoves.walking(right, MechScript.SETTLED + k * 3.0, Math.sin(k), 0.5 + 0.02 * k);
                double fold = 0.3 * Math.sin(k * 1.7);
                double tilt = 0.2 * Math.cos(k * 0.9);
                Frame hand = MechArmRig.hand(TORSO, arm, fold, tilt);
                Vec3 moved = shoulder.add(new Vec3(0.1 * Math.cos(k), 0.45 * Math.sin(k * 0.6), -0.1 * Math.sin(k)));
                Frame[] joined = MechArmRig.reattached(TORSO, arm, hand, moved, shoulder);
                Frame forearm = joined[0];
                Frame shown = joined[1];
                double upper = TORSO.point(arm.elbow()).distanceTo(shoulder);
                assertEquals(upper, forearm.center().distanceTo(moved), 1.0E-6, "upper arm " + k);
                Vec3 wrist = hand.at(0.0, MechArmShapes.WRIST, 0.0);
                if (wrist.distanceTo(moved) < (upper + MechArmShapes.WRIST) * 0.999) {
                    reached++;
                    same(shown.center(), hand.center(), 1.0E-9, "kept hand " + k);
                } else {
                    assertTrue(shown.center().distanceTo(moved) < hand.center().distanceTo(moved), "drawn in " + k);
                }
                same(forearm.at(0.0, MechArmShapes.WRIST, 0.0), shown.at(0.0, MechArmShapes.WRIST, 0.0), 1.0E-9,
                        "wrist " + k);
                double[] turn = MechArmRig.wristTurn(shown, forearm.up());
                Frame again = MechArmRig.wrist(forearm, turn[0], turn[1]);
                same(again.center(), shown.center(), 1.0E-9, "hand " + k);
                same(again.right(), hand.right(), 1.0E-9, "across the hand " + k);
                same(again.up(), hand.up(), 1.0E-9, "along the hand " + k);
                same(again.forward(), hand.forward(), 1.0E-9, "the palm " + k);
            }
        }
        assertTrue(reached > 10, "most of the moved shoulders still reach their wrists: " + reached);
    }

    // An arm laid with its hand on a hold in reach meets it: the upper arm as long as it is, the forearm reaching the
    // wrist, the hand lying as asked, the wrist within what it does.
    @Test
    void anArmLaidOnAHoldInReachMeetsIt() {
        for (boolean right : new boolean[] { true, false }) {
            Vec3 shoulder = TORSO.point(MechPainter.side(MechScript.SHOULDER, right));
            for (int k = 0; k < 20; k++) {
                Vec3 palm = shoulder.add(TORSO.dir(new Vec3((right ? -0.4 : 0.4) + 0.05 * k, 1.5 - 0.2 * k,
                        2.2 + 0.03 * k)));
                Vec3 along = TORSO.dir(new Vec3(0.0, Math.cos(k * 0.08), Math.sin(k * 0.08))).normalize();
                Vec3 facing = TORSO.ahead().subtract(along.scale(TORSO.ahead().dot(along))).normalize();
                MechMoves.Arm arm = MechArmRig.laid(TORSO, right, MechPainter.side(MechScript.SHOULDER, right),
                        MechPainter.side(new Vec3(0.8, -0.25, -0.55), right), palm, along, facing, 0.7, 0.25, null);
                assertEquals(MechScript.UPPER_ARM, TORSO.point(arm.elbow()).distanceTo(shoulder), 1.0E-6, "upper " + k);
                Frame hand = MechArmRig.hand(TORSO, arm, 0.0, 0.0);
                assertTrue(Math.abs(arm.fold()) <= MechArmRig.MOST_FOLD && Math.abs(arm.tilt()) <= MechArmRig.MOST_TILT);
                if (Math.abs(arm.fold()) < MechArmRig.MOST_FOLD && Math.abs(arm.tilt()) < MechArmRig.MOST_TILT) {
                    same(hand.at(0.0, MechScript.PALM_ALONG, 0.0), palm, 1.0E-6, "palm " + k);
                    same(hand.up(), along, 1.0E-6, "fingers " + k);
                    same(hand.forward(), facing, 1.0E-6, "palm facing " + k);
                }
                same(TORSO.point(arm.hand()), hand.at(0.0, MechScript.PALM_ALONG, 0.0), 1.0E-9, "Arm.hand " + k);
            }
        }
    }
}
