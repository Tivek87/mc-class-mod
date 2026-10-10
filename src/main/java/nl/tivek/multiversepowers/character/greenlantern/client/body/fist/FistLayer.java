package nl.tivek.multiversepowers.character.greenlantern.client.body.fist;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;

// The fists' gloves on a player as others see him (and he sees himself from outside), on each hand as it is posed.
public final class FistLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private FistLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new FistLayer(player));
            }
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        ClientFists.View view = ClientFists.view(player);
        if (view == null || player.isInvisible() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        double apart = view.apart(partialTick);
        if (apart >= 1.0) {
            return;
        }
        double formed = view.formed(partialTick);
        double size = FistArms.size(view, partialTick);
        for (int side = 0; side < 2; side++) {
            pose.pushPose();
            this.getParentModel().translateToHand(side == 0 ? HumanoidArm.RIGHT : HumanoidArm.LEFT, pose);
            LanternPainter painter = LanternPainter.hand(pose, ageInTicks);
            FistPainter.glove(painter, side == 0, formed, apart, size, player.getId() * 2 + side);
            painter.finish(Minecraft.getInstance().renderBuffers().bufferSource());
            pose.popPose();
        }
    }
}
