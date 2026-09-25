package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;

final class KneelLegs {
    private static final float THIGH = 6.0F;
    // Bigger than THIGH on purpose: it reaches two pixels past the knee so no notch opens at a sharp bend.
    private static final float THIGH_BOX = 8.0F;
    private static final float SHIN = 6.0F;
    private static final CubeDeformation PANTS = new CubeDeformation(0.25F);

    private static Halves skinRight;
    private static Halves skinLeft;
    private static Halves pantsRight;
    private static Halves pantsLeft;
    private static Halves suitRight;
    private static Halves suitLeft;

    private KneelLegs() {
    }

    private record Halves(ModelPart thigh, ModelPart shin) {
        static Halves of(int u, int v, CubeDeformation grow) {
            return new Halves(part(u, v, THIGH_BOX, grow), part(u, v + (int) THIGH, SHIN, grow));
        }

        private static ModelPart part(int u, int v, float height, CubeDeformation grow) {
            MeshDefinition mesh = new MeshDefinition();
            mesh.getRoot().addOrReplaceChild("half", CubeListBuilder.create().texOffs(u, v)
                    .addBox(-2.0F, 0.0F, -2.0F, 4.0F, height, 4.0F, grow), PartPose.ZERO);
            return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("half");
        }

        void render(ModelPart leg, float knee, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
            this.thigh.copyFrom(leg);
            this.thigh.xScale = 1.0F;
            this.thigh.yScale = 1.0F;
            this.thigh.zScale = 1.0F;
            this.thigh.render(pose, buffer, light, overlay);
            pose.pushPose();
            this.thigh.translateAndRotate(pose);
            pose.translate(0.0F, THIGH / 16.0F, 0.0F);
            pose.mulPose(Axis.XP.rotation(knee));
            this.shin.render(pose, buffer, light, overlay);
            pose.popPose();
        }
    }

    static void skin(PoseStack pose, MultiBufferSource buffers, int light, int overlay, AbstractClientPlayer player,
            PlayerModel<AbstractClientPlayer> model, float[] knees) {
        if (skinRight == null) {
            skinRight = Halves.of(0, 16, CubeDeformation.NONE);
            skinLeft = Halves.of(16, 48, CubeDeformation.NONE);
            pantsRight = Halves.of(0, 32, PANTS);
            pantsLeft = Halves.of(0, 48, PANTS);
        }
        ResourceLocation texture = player.getSkin().texture();
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(texture));
        skinRight.render(model.rightLeg, knees[0], pose, buffer, light, overlay);
        skinLeft.render(model.leftLeg, knees[1], pose, buffer, light, overlay);
        if (player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG)) {
            pantsRight.render(model.rightLeg, knees[0], pose, buffer, light, overlay);
        }
        if (player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG)) {
            pantsLeft.render(model.leftLeg, knees[1], pose, buffer, light, overlay);
        }
    }

    static void suit(PoseStack pose, VertexConsumer buffer, int light, int overlay,
            PlayerModel<AbstractClientPlayer> model, float[] knees, CubeDeformation fit) {
        if (suitRight == null) {
            suitRight = Halves.of(0, 16, fit);
            suitLeft = Halves.of(16, 48, fit);
        }
        suitRight.render(model.rightLeg, knees[0], pose, buffer, light, overlay);
        suitLeft.render(model.leftLeg, knees[1], pose, buffer, light, overlay);
    }
}
