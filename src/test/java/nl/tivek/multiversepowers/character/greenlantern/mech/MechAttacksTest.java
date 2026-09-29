package nl.tivek.multiversepowers.character.greenlantern.mech;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class MechAttacksTest {
    private static final double REACH = MechScript.UPPER_ARM + MechScript.PALM_ALONG;
    private static final MechScript.Stage FRAME = MechScript.Stage.facing(new Vec3(10.0, 64.0, -3.0), 37.0F);
    private static final MechAttacks.Held ZOMBIE = new MechAttacks.Held(FRAME.point(1.5, 0.975, 4.0), 0.3, 0.975);
    private static final int[] KINDS = { MechAttacks.SWEEP, MechAttacks.STOMP, MechAttacks.SLAM, MechAttacks.THROW };

    // Every place a blow sends a hand to lies within the arm's reach of its shoulder, the body bent as the blow bends
    // it at that moment: the arm gets there without stretching or stopping short.
    @Test
    void everyHandReachesItsMark() {
        for (int kind : KINDS) {
            for (double t = 0.0; t <= MechAttacks.length(kind); t += 0.25) {
                MechAttacks.Blow blow = new MechAttacks.Blow(kind, t, 0, 0.0);
                MechScript.Stage torso = MechAttacks.torso(FRAME, MechAttacks.body(blow));
                for (int side = 0; side < 2; side++) {
                    boolean right = side == 0;
                    MechAttacks.Aim aim = MechAttacks.aim(blow, right, FRAME, kind == MechAttacks.THROW ? ZOMBIE
                            : null);
                    if (aim == null || aim.weight() < 1.0 || aim.grab() > 0.0) {
                        continue;
                    }
                    Vec3 shoulder = torso.point(right ? MechScript.SHOULDER : MechScript.mirror(MechScript.SHOULDER));
                    double far = aim.at().distanceTo(shoulder);
                    assertTrue(far <= REACH - 0.02, "kind " + kind + " t " + t + " side " + side + ": " + far);
                    MechMoves.Arm arm = MechAttacks.arm(blow, right, FRAME, torso, kind == MechAttacks.THROW
                            ? ZOMBIE : null, MechMoves.arm(right, FRAME, MechScript.SETTLED));
                    assertTrue(torso.point(arm.hand()).distanceTo(aim.at()) < 1.0E-6, "kind " + kind + " t " + t);
                    assertEquals(MechScript.UPPER_ARM, arm.elbow().distanceTo(right ? MechScript.SHOULDER
                            : MechScript.mirror(MechScript.SHOULDER)), 1.0E-6);
                }
            }
        }
    }

    // A creature the throw may pick is one its hand can close round: turned to it and bent down for the grab, the
    // palm gets to it. Straight ahead and a little to the left count too.
    @Test
    void aPickedCreatureIsReached() {
        int reachable = 0;
        for (double x = -4.0; x <= 8.0; x += 0.5) {
            for (double z = -2.0; z <= 9.0; z += 0.5) {
                for (double y = -1.0; y <= 2.0; y += 1.0) {
                    MechAttacks.Held held = new MechAttacks.Held(FRAME.point(x, y + 0.975, z), 0.3, 0.975);
                    double turn = MechAttacks.turnFor(FRAME, held.center());
                    MechAttacks.Blow grab = new MechAttacks.Blow(MechAttacks.THROW, MechAttacks.GRAB, 0, turn);
                    MechAttacks.Body body = MechAttacks.body(grab);
                    MechScript.Stage frame = MechAttacks.aimed(FRAME, body);
                    if (!MechAttacks.reachable(frame, held)) {
                        continue;
                    }
                    reachable++;
                    MechScript.Stage torso = MechAttacks.torso(frame, body);
                    MechMoves.Arm arm = MechAttacks.arm(grab, true, frame, torso, held,
                            MechMoves.arm(true, frame, MechScript.SETTLED));
                    Vec3 grip = torso.point(MechAttacks.grip(arm, held.halfWidth()));
                    assertTrue(grip.distanceTo(held.center()) < 0.02, "x " + x + " z " + z + " y " + y + ": "
                            + grip.distanceTo(held.center()));
                }
            }
        }
        assertTrue(reachable > 40, "only " + reachable + " spots in reach");
    }

    @Test
    void theBlowTravelsWithTheMech() {
        int packed = MechAttacks.pack(MechAttacks.DROP, 9, 41, Math.toRadians(-37.0));
        int variant = MechScript.variant(false, -1, packed, 0);
        assertEquals(new MechAttacks.Blow(MechAttacks.DROP, 9, 41, Math.toRadians(-37.0)), MechScript.blow(variant));
        assertEquals(-1, MechScript.target(variant));
        assertTrue(!MechScript.breaking(variant));
        int building = MechScript.variant(true, 123456789, 0, 0);
        assertEquals(123456789, MechScript.target(building));
        assertEquals(MechAttacks.Blow.NONE, MechScript.blow(building));
        assertTrue(MechScript.breaking(building));
    }
}
