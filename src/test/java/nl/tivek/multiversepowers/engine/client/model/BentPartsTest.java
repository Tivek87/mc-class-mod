package nl.tivek.multiversepowers.engine.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BentPartsTest {
    private static final float CLOSE = 1.0E-3F;

    // Every corner drawn, in pixels.
    private static final class Corners implements VertexConsumer {
        final List<Vector3f> at = new ArrayList<>();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.at.add(new Vector3f(x, y, z).mul(16.0F));
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }

    @AfterEach
    void unbend() {
        BentParts.clear();
    }

    private static HumanoidModel<LivingEntity> person() {
        return new HumanoidModel<>(
                LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot());
    }

    private static ModelBends.Bend knee(HumanoidModel<LivingEntity> person) {
        for (ModelParts.Part part : ModelParts.of(person)) {
            if (part.part() == person.rightLeg) {
                ModelBends.Bend bend = ModelBends.bend(part);
                assertNotNull(bend);
                return bend;
            }
        }
        throw new AssertionError("no right leg");
    }

    private static Corners draw(ModelPart part) {
        Corners corners = new Corners();
        assertTrue(BentParts.draw(part, new PoseStack().last(), corners, 0, 0, -1));
        return corners;
    }

    // The room the drawn squares close in, in cubic pixels; its sign follows the way the faces run.
    private static double volume(Corners corners) {
        double sum = 0.0;
        for (int q = 0; q + 3 < corners.at.size(); q += 4) {
            Vector3f a = corners.at.get(q);
            sum += a.dot(new Vector3f(corners.at.get(q + 1)).cross(corners.at.get(q + 2)))
                    + a.dot(new Vector3f(corners.at.get(q + 2)).cross(corners.at.get(q + 3)));
        }
        return sum / 6.0;
    }

    @Test
    void aFoldedLegStaysWholeAndClosed() {
        HumanoidModel<LivingEntity> person = person();
        ModelBends.Bend bend = knee(person);
        BentParts.bend(person.rightLeg, bend, new Quaternionf());
        double straight = volume(draw(person.rightLeg));
        assertEquals(4.0 * 12.0 * 4.0, Math.abs(straight), 0.01);
        for (float angle : new float[] { 0.4F, 1.2F, (float) (Math.PI / 2.0), 2.0F, 2.3F, 2.45F }) {
            BentParts.bend(person.rightLeg, bend, new Quaternionf().rotationX(angle));
            double folded = Math.signum(straight) * volume(draw(person.rightLeg));
            // Up to a square bend the halves meet in a mitre, which keeps the leg's room; sharper, each reaches on past
            // the knee as far as the leg is thick there and ends flat.
            double t = Math.tan(angle * 0.5);
            double want = t <= 1.0 ? 192.0 : 224.0 - 16.0 * (t + 1.0 / t);
            assertEquals(want, folded, 0.01, "folded " + angle);
        }
    }

    @Test
    void aKneeFoldsSquare() {
        HumanoidModel<LivingEntity> person = person();
        ModelBends.Bend bend = knee(person);
        // Folding back, the front of the knee is its outside: the thigh's front reaches past the cut to the mitre.
        BentParts.bend(person.rightLeg, bend, new Quaternionf().rotationX(1.2F));
        Corners mitred = draw(person.rightLeg);
        float mitre = 6.0F + 2.0F * (float) Math.tan(0.6);
        for (float x : new float[] { -2.0F, 2.0F }) {
            Vector3f corner = new Vector3f(x, mitre, -2.0F);
            assertTrue(mitred.at.stream().anyMatch(p -> p.distance(corner) < CLOSE), "mitre corner " + corner);
        }
        // Sharper than square, the thigh ends flat as far past the knee as it is thick there, and so does the shin.
        Quaternionf turn = new Quaternionf().rotationX(2.3F);
        BentParts.bend(person.rightLeg, bend, turn);
        Corners sharp = draw(person.rightLeg);
        float[] k = bend.knee();
        for (float x : new float[] { -2.0F, 2.0F }) {
            Vector3f thigh = new Vector3f(x, 8.0F, -2.0F);
            Vector3f shin = turn.transform(new Vector3f(x - k[0], -2.0F, -2.0F - k[2])).add(k[0], k[1], k[2]);
            assertTrue(sharp.at.stream().anyMatch(p -> p.distance(thigh) < CLOSE), "thigh's end " + thigh);
            assertTrue(sharp.at.stream().anyMatch(p -> p.distance(shin) < CLOSE), "shin's end " + shin);
        }
    }

    @Test
    void theShinTurnsAboutTheKnee() {
        HumanoidModel<LivingEntity> person = person();
        ModelBends.Bend bend = knee(person);
        Quaternionf turn = new Quaternionf().rotationX(1.3F);
        BentParts.bend(person.rightLeg, bend, turn);
        Corners corners = draw(person.rightLeg);
        float[] k = bend.knee();
        for (float x : new float[] { -2.0F, 2.0F }) {
            for (float z : new float[] { -2.0F, 2.0F }) {
                Vector3f foot = turn.transform(new Vector3f(x - k[0], 12.0F - k[1], z - k[2])).add(k[0], k[1], k[2]);
                assertTrue(corners.at.stream().anyMatch(p -> p.distance(foot) < CLOSE), "foot corner " + foot);
            }
        }
    }

    @Test
    void aTwistIsSharedAlongTheLeg() {
        HumanoidModel<LivingEntity> person = person();
        ModelBends.Bend bend = knee(person);
        Quaternionf turn = new Quaternionf().rotationY(0.8F);
        BentParts.bend(person.rightLeg, bend, turn);
        Corners corners = draw(person.rightLeg);
        float[] k = bend.knee();
        Quaternionf half = new Quaternionf().rotationY(0.4F);
        for (float x : new float[] { -2.0F, 2.0F }) {
            for (float z : new float[] { -2.0F, 2.0F }) {
                // The hip stays, the foot turns all the way and the knee half as far.
                Vector3f hip = new Vector3f(x, 0.0F, z);
                Vector3f knee = half.transform(new Vector3f(x - k[0], 0.0F, z - k[2])).add(k[0], k[1], k[2]);
                Vector3f foot = turn.transform(new Vector3f(x - k[0], 12.0F - k[1], z - k[2])).add(k[0], k[1], k[2]);
                for (Vector3f want : new Vector3f[] { hip, knee, foot }) {
                    assertTrue(corners.at.stream().anyMatch(p -> p.distance(want) < CLOSE), "corner " + want);
                }
            }
        }
    }

    @Test
    void aTrunkFoldsSquareToo() {
        HumanoidModel<LivingEntity> person = person();
        List<ModelParts.Part> parts = ModelParts.of(person);
        ModelBends.Bend waist = ModelBends.waist(parts, ModelBends.core(parts));
        assertNotNull(waist);
        // Folding ahead less than square, its halves meet in a mitre and the trunk keeps its room.
        BentParts.bend(person.body, waist, new Quaternionf().rotationX(-0.9F));
        assertEquals(8.0 * 12.0 * 4.0, Math.abs(volume(draw(person.body))), 0.01);
    }
}
