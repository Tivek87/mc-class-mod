package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientLooks;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BeamCharge;

public final class GreenLanternSuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "textures/entity/green_lantern_suit.png");
    private static final CubeDeformation FIT = new CubeDeformation(0.3F);

    private static PlayerModel<AbstractClientPlayer> wide;
    private static PlayerModel<AbstractClientPlayer> slim;

    private GreenLanternSuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new GreenLanternSuitLayer(player));
            }
        }
    }

    public static PlayerModel<AbstractClientPlayer> model(AbstractClientPlayer player) {
        if (player.getSkin().model() == PlayerSkin.Model.SLIM) {
            if (slim == null) {
                slim = build(true);
            }
            return slim;
        }
        if (wide == null) {
            wide = build(false);
        }
        return wide;
    }

    private static PlayerModel<AbstractClientPlayer> build(boolean slimArms) {
        PlayerModel<AbstractClientPlayer> model = new PlayerModel<>(
                LayerDefinition.create(PlayerModel.createMesh(FIT, slimArms), 64, 64).bakeRoot(), slimArms);
        model.setAllVisible(false);
        model.hat.visible = true;
        model.body.visible = true;
        model.rightArm.visible = true;
        model.leftArm.visible = true;
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        return model;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        float recharge = ClientRing.recharge(player, partialTick);
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        if (recharge >= 0.0F) {
            RechargeAnimation.lanternInHand(poseStack, buffers, this.getParentModel().leftArm, slim, player, recharge,
                    FlightPose.bodyTurn(player));
        } else if (ArrivalAnimation.holdsLantern(player, partialTick)) {
            RechargeAnimation.lanternInHand(poseStack, buffers, this.getParentModel().leftArm, slim, 1.0F,
                    ArrivalAnimation.lanternGlow(player, partialTick), 0.0F, FlightPose.bodyTurn(player));
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        float[] knees = FlightPose.knees(player);
        if (knees != null) {
            KneelLegs.skin(poseStack, buffers, light, overlay, player, this.getParentModel(), knees);
        }
        ClientLooks.Uniform uniform = ClientLooks.uniform(player, partialTick);
        if (uniform == null) {
            return;
        }
        HandSpot.onRoot(player, poseStack);
        PlayerModel<AbstractClientPlayer> suit = model(player);
        this.getParentModel().copyPropertiesTo(suit);
        suit.rightLeg.visible = knees == null;
        suit.leftLeg.visible = knees == null;
        if (uniform.complete()) {
            VertexConsumer cloth = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            suit.renderToBuffer(poseStack, cloth, light, overlay);
            if (knees != null) {
                KneelLegs.suit(poseStack, cloth, light, overlay, suit, knees, FIT);
            }
        } else {
            SuitSpread spread = new SuitSpread(buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)));
            spread.render(suit.rightArm, uniform.armField(), poseStack, light, overlay);
            spread.render(suit.body, uniform.torsoField(), poseStack, light, overlay);
            spread.render(suit.leftArm, uniform.leftArmField(), poseStack, light, overlay);
            spread.render(suit.rightLeg, uniform.legField(), poseStack, light, overlay);
            spread.render(suit.leftLeg, uniform.legField(), poseStack, light, overlay);
            spread.render(suit.hat, uniform.maskField(), poseStack, light, overlay);
            spread.seam(buffers, 1.0F);
            SuitGlow.core(poseStack, buffers, suit, uniform.core(), ageInTicks);
        }
        suit.rightLeg.visible = true;
        suit.leftLeg.visible = true;
        float glow = uniform.complete() ? SuitGlow.level(player, partialTick) : 0.0F;
        SuitGlow.body(poseStack, buffers, suit, slim, glow, ageInTicks, knees != null,
                BeamCharge.charge(player, partialTick));
        poseStack.pushPose();
        suit.rightArm.translateAndRotate(poseStack);
        Ring.draw(poseStack, buffers, light, slim, uniform.ring(), ClientRing.charge(player), glow);
        RingSpot.onBody(player, poseStack, Ring.stone(slim));
        HandSpot.onArm(player, poseStack, true, slim);
        poseStack.popPose();
        poseStack.pushPose();
        suit.leftArm.translateAndRotate(poseStack);
        HandSpot.onArm(player, poseStack, false, slim);
        poseStack.popPose();
    }

}
