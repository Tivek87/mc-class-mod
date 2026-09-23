package nl.tivek.welcomescreen.client.character.lantern;

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
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Green Lantern's uniform, made by the ring over whatever you wear: the green and black suit, white gloves,
 * and the mask over your eyes. Your own face and hair stay. It sits just outside the skin's own outer layer,
 * so a jacket or sleeves of the skin never poke through; armour still goes over it.
 */
public final class GreenLanternSuitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID,
            "textures/entity/green_lantern_suit.png");
    // Just outside the skin's own outer layer (0.25). The mask is on the hat part, which is 0.5 bigger
    // again: just outside the skin's own hat.
    private static final CubeDeformation FIT = new CubeDeformation(0.3F);

    private static PlayerModel<AbstractClientPlayer> wide;
    private static PlayerModel<AbstractClientPlayer> slim;

    private GreenLanternSuitLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** Puts the uniform on the renderer of every kind of player skin (wide and slim arms). */
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new GreenLanternSuitLayer(player));
            }
        }
    }

    /** The uniform's model for this player's arms (wide or slim), made the first time it is needed. */
    static PlayerModel<AbstractClientPlayer> model(AbstractClientPlayer player) {
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
        // Only the suit and the mask; your own head stays your own.
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
        // Recharging: the lantern hangs from the left hand, which the arm pose has raised. While the ring dresses him
        // he holds it there already, from the moment it flew into his hand.
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
        // Kneeling in the landing of a slam his legs bend at the knee, which the game's straight legs cannot: they are
        // hidden (see FlightPose) and drawn here in two halves, his own and the uniform's.
        float[] knees = FlightPose.knees(player);
        if (knees != null) {
            KneelLegs.skin(poseStack, buffers, light, overlay, player, this.getParentModel(), knees);
        }
        ClientLooks.Uniform uniform = ClientLooks.uniform(player, partialTick);
        if (uniform == null) {
            return;
        }
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
            // Only as far as it has got, each part from where it starts, with a seam of light along its edge; the
            // lantern on the chest flares as the uniform bursts out of it.
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
        // While the ring works the uniform lights up, most of all down the right arm into the ring.
        float glow = uniform.complete() ? SuitGlow.level(player, partialTick) : 0.0F;
        SuitGlow.body(poseStack, buffers, suit, slim, glow, ageInTicks, knees != null,
                BeamCharge.charge(player, partialTick));
        poseStack.pushPose();
        suit.rightArm.translateAndRotate(poseStack);
        Ring.draw(poseStack, buffers, light, slim, uniform.ring(), ClientRing.charge(player), glow);
        RingSpot.onBody(player, poseStack, Ring.stone(slim));
        poseStack.popPose();
    }

}
