package nl.tivek.multiversepowers.engine.client.ragdoll;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.GetUp;
import org.joml.Quaterniond;
import org.junit.jupiter.api.Test;

class RiseFacingTest {
    private static double[] lay(Quaterniond turn) {
        return new double[] { 0.0, 0.0, 0.0, turn.x, turn.y, turn.z, turn.w };
    }

    // A person's pelvis lying with its head towards +z (the game's yaw 0) and its front pointing `front` (world axes):
    // its model's y runs down the spine and -z is its front.
    private static Quaterniond lying(double frontX, double frontY) {
        // Not Quaterniond.rotationX: in this JOML it puts the cosine in z.
        Quaterniond turn = new Quaterniond().rotateX(-Math.PI / 2.0);
        // Rolled about its spine so its front points (frontX, frontY, 0).
        return new Quaterniond().rotationZ(Math.atan2(frontX, -frontY)).mul(turn);
    }

    // A person gets up along its body: face down towards its head, face up or on its side towards its feet, and the way
    // it faces when it sits.
    @Test
    void aPersonRisesAlongItsBody() {
        assertEquals(0.0F, GetUp.facing(lay(lying(0.0, -1.0)), 3, 77.0F), 1.0E-3F);
        assertEquals(180.0F, Math.abs(GetUp.facing(lay(lying(0.0, 1.0)), 3, 77.0F)), 1.0E-3F);
        assertEquals(180.0F, Math.abs(GetUp.facing(lay(lying(1.0, 0.001)), 3, 77.0F)), 1.0E-3F);
        // Sitting up (drawn flipped, its spine upright), its front towards +x: the game's yaw -90 faces +x.
        Quaterniond sitting = new Quaterniond().rotationY(-Math.PI / 2.0).rotateZ(Math.PI);
        assertEquals(-90.0F, GetUp.facing(lay(sitting), 3, 77.0F), 1.0E-3F);
    }

    // Anything else gets up facing whichever way takes the least turning from how it lies: a creature rolled over
    // about its spine onto its side rises facing the way it lay, whatever way that was.
    @Test
    void aCreatureRolledOnItsSideRisesTheWayItLay() {
        // A four-legged trunk as built: turned a quarter about x in its model, drawn flipped and turned half round.
        Quaterniond standing = new Quaterniond().rotationY(Math.PI).rotateZ(Math.PI).rotateX(Math.PI / 2.0);
        for (float yaw = -170.0F; yaw < 180.0F; yaw += 37.0F) {
            Quaterniond facing = new Quaterniond().rotationY(Math.toRadians(-yaw)).mul(standing);
            // Its spine runs along the way it faces; rolled a quarter about it.
            double way = Math.toRadians(yaw);
            Quaterniond roll = new Quaterniond().rotationAxis(Math.PI / 2.0, -Math.sin(way), 0.0, Math.cos(way));
            float rises = RagdollBody.rising(lay(new Quaterniond(roll).mul(facing)), 3, standing, 999.0F);
            assertEquals(0.0F, Mth.wrapDegrees(rises - yaw), 1.0E-2F, "lying facing " + yaw);
        }
    }
}
