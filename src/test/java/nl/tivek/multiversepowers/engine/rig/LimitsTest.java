package nl.tivek.multiversepowers.engine.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class LimitsTest {
    private static final float[] X = { 1.0F, 0.0F, 0.0F };
    private static final float[] DOWN = { 0.0F, 1.0F, 0.0F };

    @Test
    void aHingeKeepsOnlyItsOwnTurnWithinItsRange() {
        Quaternionf inside = new Quaternionf().rotationX(1.0F);
        assertEquals(1.0F, Limits.about(Limits.hinge(new Quaternionf(inside), X, -0.1F, 2.0F), 1.0F, 0.0F, 0.0F),
                1.0E-5F);
        Quaternionf past = new Quaternionf().rotationX(2.8F);
        assertEquals(2.0F, Limits.about(Limits.hinge(past, X, -0.1F, 2.0F), 1.0F, 0.0F, 0.0F), 1.0E-5F);
        Quaternionf back = new Quaternionf().rotationX(-0.9F);
        assertEquals(-0.1F, Limits.about(Limits.hinge(back, X, -0.1F, 2.0F), 1.0F, 0.0F, 0.0F), 1.0E-5F);
        // A sideways turn is no hinge's: only the part about the hinge stays.
        Quaternionf skew = new Quaternionf().rotationZ(0.7F).rotateX(0.5F);
        Quaternionf kept = Limits.hinge(skew, X, -0.1F, 2.0F);
        Vector3f bone = kept.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        assertEquals(0.0F, bone.x, 1.0E-5F);
    }

    @Test
    void aimingFoldsAndLeansNoFurtherThanTheJointGoes() {
        // Within reach: the bone ends up pointing just that way.
        Vector3f way = new Vector3f(0.3F, 0.8F, 0.5F).normalize();
        Quaternionf q = Limits.aim(way, DOWN, X, -1.3F, 1.3F, 0.35F, new Quaternionf());
        Vector3f bone = q.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        assertEquals(0.0F, bone.distance(way), 1.0E-4F);
        // Straight back along the bone: folded as far as it goes, the same every time, never flipped round.
        Vector3f back = new Vector3f(0.0F, -1.0F, 1.0E-4F).normalize();
        Quaternionf a = Limits.aim(back, DOWN, X, -1.3F, 1.3F, 0.35F, new Quaternionf());
        Quaternionf b = Limits.aim(new Vector3f(0.0F, -1.0F, 2.0E-4F).normalize(), DOWN, X, -1.3F, 1.3F, 0.35F,
                new Quaternionf());
        assertEquals(1.3F, Math.abs(Limits.about(a, 1.0F, 0.0F, 0.0F)), 1.0E-4F);
        assertTrue(Math.abs(a.dot(b)) > 0.9999F);
        // Far out to the side: leaned no more than its side reach.
        Quaternionf side = Limits.aim(new Vector3f(1.0F, 0.2F, 0.0F).normalize(), DOWN, X, -1.3F, 1.3F, 0.35F,
                new Quaternionf());
        assertEquals(Math.sin(0.35), Math.abs(side.transform(new Vector3f(0.0F, 1.0F, 0.0F)).x), 1.0E-4);
    }

    @Test
    void anEndJointWithinItsRangeStaysJustAsItIs() {
        Quaternionf q = new Quaternionf().rotationX(0.6F).rotateY(0.2F).rotateZ(0.1F);
        Quaternionf kept = Limits.end(new Quaternionf(q), DOWN, X, -1.3F, 1.3F, 0.35F, 0.3F);
        assertTrue(Math.abs(kept.dot(q)) > 0.99999F);
    }

    @Test
    void anEndJointFoldsLeansAndTwistsNoFurtherThanItCan() {
        // Folded 2.5 back on itself: kept at 1.3, so the hand never turns over onto its forearm.
        Quaternionf folded = Limits.end(new Quaternionf().rotationX(2.5F), DOWN, X, -1.3F, 1.3F, 0.35F, 0.3F);
        Vector3f way = folded.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        assertEquals(Math.cos(1.3), way.y, 1.0E-4);
        // Twisted half round about its bone: kept at 0.3.
        Quaternionf twisted = Limits.end(new Quaternionf().rotationY(3.0F), DOWN, X, -1.3F, 1.3F, 0.35F, 0.3F);
        assertEquals(0.3F, Limits.about(twisted, 0.0F, 1.0F, 0.0F), 1.0E-4F);
        // Leaning far across its hinge: kept at 0.35.
        Quaternionf leaning = Limits.end(new Quaternionf().rotationZ(1.2F), DOWN, X, -1.3F, 1.3F, 0.35F, 0.3F);
        Vector3f lean = leaning.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        assertEquals(Math.sin(0.35), Math.abs(lean.x), 1.0E-4);
    }
}
