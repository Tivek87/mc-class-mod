package nl.tivek.multiversepowers.character.greenlantern.client.mech.walk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechArmRig;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechHandRig;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import org.junit.jupiter.api.Test;

class MechClimbTest {
    private static final double[] HEIGHTS = { 2.75, 3.4, 5.0, 7.0, 9.5, 12.0, 18.25, 24.0, MechClimb.HIGHEST };
    private static final double[] EDGES = { 1.625, 2.25, 2.5, 3.375 };
    // What the server takes from the pilot's game in one tick (MechAssembly.drive).
    private static final double MOST_STRIDE = 1.0;
    private static final double MOST_CLIMB = 3.0;
    private static final int MOST_CLIMB_BITS = 1 << 22;
    private static final double CROUCH = 0.6;
    private static final double ARM = MechScript.UPPER_ARM + MechScript.PALM_ALONG;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    // A ledge `height` high whose face stands `face` ahead along z, as a signed distance.
    private static MechHandRig.Ground ledge(double height, double face) {
        return (x, y, z, radius) -> {
            double d = y < height && z > face ? -Math.min(height - y, z - face)
                    : Math.sqrt(Math.pow(Math.max(y - height, 0.0), 2.0) + Math.pow(Math.max(face - z, 0.0), 2.0));
            return radius - d;
        };
    }

    // The torso as the climb poses it `age` ticks in (MechWalk), from where it set off.
    private static MechScript.Stage torso(MechScript.Stage start, double age, double height, double edge) {
        MechScript.Stage stage = start.turned(Vec3.ZERO, MechClimb.path(age, height, edge), 0.0, 0.0, 0.0);
        return stage.turned(new Vec3(0.0, MechScript.HIP.y, 0.0),
                new Vec3(0.0, MechClimb.low(age, height, edge), 0.0), 0.0, MechClimb.lean(age, height, edge), 0.0);
    }

    // Whenever its hands press down on the top of a ledge it climbs, with the body posed as the climb poses it, they
    // rest on it: palms turned down onto it, the fingers in it nowhere and not standing off it either.
    @Test
    void handsPressedOnALedgeRestOnItsTop() {
        MechScript.Stage start = MechScript.Stage.facing(new Vec3(0.5, 0.0, 0.5), 0.0F);
        String worst = "";
        double highest = Double.NEGATIVE_INFINITY;
        int pressed = 0;
        for (double height : HEIGHTS) {
            for (double edge : EDGES) {
                MechHandRig.Ground ledge = ledge(height, 0.5 + edge);
                for (int age = 0; age <= MechClimb.ticks(height); age++) {
                    if (MechClimb.grip(age, height) < 0.999 || MechClimb.turned(age, height) < 0.999) {
                        continue;
                    }
                    MechScript.Stage torso = torso(start, age, height, edge);
                    for (boolean right : new boolean[] { true, false }) {
                        Vec3 hold = start.point(MechPainter.side(new Vec3(MechClimbHolds.HAND_OUT, height, edge),
                                right));
                        Vec3[] laid = MechClimb.palm(hold, start.ahead(), 1.0);
                        MechMoves.Arm arm = MechClimb.laid(MechMoves.walking(right, MechScript.SETTLED, 0.0, 0.0),
                                torso, right, laid[0], laid[1], laid[2], 1.0, 0.7, ledge);
                        Frame hand = MechArmRig.hand(torso, arm, 0.0, 0.0);
                        double in = MechHandRig.rest(hand, arm, !right, ledge);
                        double down = hand.forward().y;
                        double off = hand.at(0.0, MechScript.PALM_ALONG, 0.0).y - height;
                        String where = String.format("height %.2f edge %.3f age %d %s: in %.3f, palm down %.2f, palm "
                                + "over the top %.2f, wrist %.2f %.2f, low %.2f, lean %.2f", height, edge, age,
                                right ? "right" : "left", in, down, off, arm.fold(), arm.tilt(),
                                MechClimb.low(age, height, edge), MechClimb.lean(age, height, edge));
                        pressed++;
                        if (off > highest) {
                            highest = off;
                            worst = where;
                        }
                        assertTrue(in <= MechHandRig.TOUCH + 0.01, "fingers in the ledge: " + where);
                        assertTrue(down < -0.9, "palm not turned down onto the top: " + where);
                        assertTrue(off < MechClimb.GRIP_UP + 0.3, "the hand stands off the top: " + where);
                    }
                }
            }
        }
        assertTrue(pressed > 100, "pressed poses tried: " + pressed);
        System.out.println("highest hand: " + worst);
    }

    // Hanging from a ledge over its head, a hand hooks its fingers over the edge, palm to the face: no finger goes
    // into the ledge and the forearm hangs clear of the wall, the elbow swung out round it where it has to.
    @Test
    void handsHookedOverAnEdgeKeepTheArmOffTheWall() {
        MechScript.Stage torso = MechScript.Stage.facing(new Vec3(0.5, 0.0, 0.5), 0.0F);
        double face = MechClimb.CHEST + 0.4;
        StringBuilder report = new StringBuilder();
        for (double height : new double[] { 9.5, 10.5, 11.2 }) {
            MechHandRig.Ground ledge = ledge(height, face);
            for (boolean right : new boolean[] { true, false }) {
                Vec3 from = torso.point(MechPainter.side(new Vec3(2.7, 0.0, 0.0), right));
                Vec3 spot = new Vec3(from.x, height + 0.15 - 0.62, face - 0.38);
                MechMoves.Arm arm = MechClimb.laid(MechMoves.walking(right, MechScript.SETTLED, 0.0, 0.0), torso,
                        right, spot, UP, torso.ahead(), 1.0, 0.7, ledge);
                Frame hand = MechArmRig.hand(torso, arm, 0.0, 0.0);
                Frame forearm = MechArmRig.forearm(torso, arm);
                double fingers = MechHandRig.rest(hand, arm, !right, ledge);
                double worst = Double.NEGATIVE_INFINITY;
                for (double[] p : new double[][] { { 0.35, 0.78 }, { 1.2, 0.82 }, { 1.9, 0.62 } }) {
                    Vec3 at = forearm.at(0.0, p[0], 0.0);
                    worst = Math.max(worst, ledge.depth(at.x, at.y, at.z, p[1]));
                }
                double upper = torso.point(arm.elbow()).distanceTo(torso.point(MechPainter.side(MechScript.SHOULDER,
                        right)));
                report.append(String.format("height %.1f %s: fingers in %.3f, forearm in %.3f, upper arm %.3f, "
                        + "wrist %.2f %.2f%n", height, right ? "right" : "left", fingers, worst, upper, arm.fold(),
                        arm.tilt()));
                assertTrue(fingers <= MechHandRig.TOUCH + 0.01, "fingers in the ledge:\n" + report);
                assertTrue(worst <= 0.15, "the forearm goes into the wall:\n" + report);
                assertEquals(MechScript.UPPER_ARM, upper, 1.0E-6, "the upper arm keeps its length:\n" + report);
                assertTrue(Math.abs(arm.fold()) <= MechArmRig.MOST_FOLD && Math.abs(arm.tilt()) <= MechArmRig.MOST_TILT);
            }
        }
        System.out.print(report);
    }

    // What the pilot's game packs comes out the same in every other game, and the server takes it.
    @Test
    void packedClimbsComeBackTheSame() {
        for (int age = 0; age < 256; age += 9) {
            for (double height : HEIGHTS) {
                for (double edge : EDGES) {
                    int packed = MechClimb.pack(age, height, edge);
                    assertTrue(packed != 0 && packed > 0 && packed < MOST_CLIMB_BITS);
                    assertEquals(age, MechClimb.age(packed));
                    assertEquals(MechClimb.quantizedHeight(height), MechClimb.height(packed), 1.0E-9);
                    assertEquals(MechClimb.quantizedEdge(edge), MechClimb.edge(packed), 1.0E-9);
                }
            }
        }
        assertTrue(MechClimb.ticks(MechClimb.HIGHEST) <= 255, "the longest climb fits the packed age");
    }

    // The mech's variant carries a blow, a climb or the build's target, and each comes back on its own.
    @Test
    void variantCarriesABlowAClimbOrATarget() {
        int climb = MechClimb.pack(33, 7.0, 1.75);
        int blow = MechAttacks.pack(MechAttacks.STOMP, 12, 3, 0.4);
        int target = MechScript.variant(false, 123456, 0, 0);
        assertEquals(123456, MechScript.target(target));
        assertEquals(0, MechScript.climb(target));
        assertEquals(MechAttacks.Blow.NONE, MechScript.blow(target));
        int climbing = MechScript.variant(false, -1, 0, climb);
        assertEquals(climb, MechScript.climb(climbing));
        assertEquals(-1, MechScript.target(climbing));
        assertEquals(MechAttacks.Blow.NONE, MechScript.blow(climbing));
        int striking = MechScript.variant(true, -1, blow, climb);
        assertTrue(MechScript.breaking(striking));
        assertEquals(MechAttacks.unpack(blow), MechScript.blow(striking));
        assertEquals(0, MechScript.climb(striking));
        assertEquals(-1, MechScript.target(striking));
    }

    // The base only ever goes up, within what the server takes in a tick, and ends on the ledge past its edge; hanging
    // it is pushed back off the wall no further than keeps its chest off it, and it only moves on over the edge once
    // its hips are up level with the top.
    @Test
    void pathGoesUpAndOverWithinWhatTheServerTakes() {
        for (double height : HEIGHTS) {
            for (double edge : EDGES) {
                int ticks = MechClimb.ticks(height);
                assertEquals(Vec3.ZERO, MechClimb.path(0, height, edge));
                Vec3 end = MechClimb.path(ticks, height, edge);
                assertEquals(height, end.y, 1.0E-9);
                assertTrue(end.z > edge + 1.0, "ends past the edge: " + end.z);
                Vec3 was = Vec3.ZERO;
                for (int age = 1; age <= ticks; age++) {
                    Vec3 at = MechClimb.path(age, height, edge);
                    String where = "height " + height + " edge " + edge + " age " + age;
                    assertTrue(at.y >= was.y - 1.0E-9, where);
                    assertTrue(at.z >= -MechClimb.standoff(edge) - 1.0E-9, where + ": pushed too far back");
                    assertTrue(at.y - was.y <= MOST_CLIMB && Math.abs(at.z - was.z) <= MOST_STRIDE, where);
                    boolean under = at.y + MechClimb.low(age, height, edge) + MechClimb.CHEST_BOTTOM < height;
                    if (at.y > MechClimb.LIFT_OFF && at.z < 0.0 && under) {
                        assertTrue(edge - at.z >= MechClimb.CHEST + 0.3, where + ": chest on the wall");
                    }
                    if (at.z > 0.05) {
                        assertTrue(at.y + MechScript.HIP.y - CROUCH >= height, where + ": hips under the top");
                    }
                    was = at;
                }
            }
        }
    }

    // The hands take hold after the reach and let go before the shoulders rise out of their reach; the body's crouch
    // and lean start and end at nothing, so the walk goes on from them without a jump.
    @Test
    void handsHoldWhileTheyReachAndTheBodyEndsAsItBegan() {
        for (double height : HEIGHTS) {
            int ticks = MechClimb.ticks(height);
            assertEquals(0.0, MechClimb.grip(0, height), 1.0E-9);
            assertEquals(0.0, MechClimb.grip(ticks, height), 1.0E-9);
            double most = 0.0;
            for (int age = 0; age <= ticks; age++) {
                double grip = MechClimb.grip(age, height);
                most = Math.max(most, grip);
                if (grip > 0.99) {
                    double shoulders = MechClimb.path(age, height, 2.0).y + MechScript.SHOULDER.y - CROUCH;
                    assertTrue(shoulders - (height + MechClimb.GRIP_UP) < ARM, "height " + height + " age " + age);
                }
            }
            assertEquals(1.0, most, 1.0E-9);
            assertEquals(0.0, MechClimb.low(0, height, 2.0), 1.0E-9);
            assertEquals(0.0, MechClimb.lean(0, height, 2.0), 1.0E-9);
            assertEquals(0.0, MechClimb.low(ticks, height, 2.0), 1.0E-9);
            assertEquals(0.0, MechClimb.lean(ticks, height, 2.0), 1.0E-9);
            for (int age = 1; age <= ticks; age++) {
                for (double edge : EDGES) {
                    String where = "height " + height + " edge " + edge + " age " + age;
                    assertTrue(Math.abs(MechClimb.low(age, height, edge) - MechClimb.low(age - 1, height, edge)) < 0.35,
                            where + ": the body drops or jumps up");
                    assertTrue(Math.abs(MechClimb.lean(age, height, edge) - MechClimb.lean(age - 1, height, edge))
                            < 0.12, where + ": the body tips over at once");
                }
            }
        }
    }

    // How deep the front of the chest, leaned `lean` ahead about its pivot, goes into a wall whose edge stands `ahead`
    // in front of and `over` above that pivot (less than 0: how far it stays off it).
    private static double chestIn(double ahead, double over, double lean) {
        double deepest = Double.NEGATIVE_INFINITY;
        for (int i = 0; i <= 200; i++) {
            double up = Mth.lerp(i / 200.0, MechClimb.CHEST_BOTTOM, MechClimb.CHEST_TOP) - MechClimb.LEAN_FROM;
            double z = MechClimb.CHEST * Math.cos(lean) + up * Math.sin(lean);
            double y = up * Math.cos(lean) - MechClimb.CHEST * Math.sin(lean);
            deepest = Math.max(deepest, Math.min(z - ahead, over - y));
        }
        return deepest;
    }

    // Wherever the wall's face stands, the body never leans the front of its chest into the wall under the top: one
    // that stood off it upright keeps off it, and one already against it goes in no deeper.
    @Test
    void theChestNeverLeansIntoTheWall() {
        int leaned = 0;
        for (double height : HEIGHTS) {
            for (double edge : EDGES) {
                for (int age = 0; age <= MechClimb.ticks(height); age++) {
                    Vec3 at = MechClimb.path(age, height, edge);
                    double lean = -MechClimb.lean(age, height, edge);
                    double over = height - at.y - MechClimb.LEAN_FROM - MechClimb.low(age, height, edge);
                    double upright = chestIn(edge - at.z, over, 0.0);
                    double in = chestIn(edge - at.z, over, lean);
                    leaned += lean > 0.05 && upright < 0.0 && upright > -1.0 ? 1 : 0;
                    assertTrue(in <= Math.max(upright, -0.1) + 0.01, "height " + height + " edge " + edge + " age "
                            + age + ": chest " + in + " in the wall leaned " + lean + ", " + upright + " upright");
                }
            }
        }
        assertTrue(leaned > 50, "leaned near the wall: " + leaned);
    }
}
