package nl.tivek.multiversepowers.engine.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A player's own leg seen from their own eyes, as FirstPersonArm draws an arm: the leg of their model, in their skin,
// its sole at `foot` and reaching back towards `hip` (view space: x right, y up, -z ahead).
public final class FirstPersonLeg {
    private static final Vector3f DOWN_THE_LEG = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final float LENGTH = 12.0F / 16.0F;

    private FirstPersonLeg() {
    }

    public static void leg(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            PlayerRenderer renderer, float side, Vector3f hip, Vector3f foot) {
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        ModelPart leg = side > 0.0F ? model.rightLeg : model.leftLeg;
        ModelPart pants = side > 0.0F ? model.rightPants : model.leftPants;
        PartPose legWas = leg.storePose();
        PartPose pantsWas = pants.storePose();
        leg.loadPose(PartPose.ZERO);
        pants.loadPose(PartPose.ZERO);
        Vector3f way = new Vector3f(foot).sub(hip);
        if (way.lengthSquared() < 1.0E-8F) {
            way.set(0.0F, -1.0F, 0.0F);
        }
        way.normalize();
        pose.pushPose();
        // The model's leg runs down its own +y from the hip: turned so it runs from the hip's side to the sole.
        pose.translate(foot.x - way.x * LENGTH, foot.y - way.y * LENGTH, foot.z - way.z * LENGTH);
        pose.mulPose(new Quaternionf().rotationTo(DOWN_THE_LEG, way));
        ResourceLocation skin = player.getSkin().texture();
        leg.render(pose, buffers.getBuffer(RenderType.entitySolid(skin)), light, OverlayTexture.NO_OVERLAY);
        pants.render(pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        leg.loadPose(legWas);
        pants.loadPose(pantsWas);
    }
}
