package nl.tivek.multiversepowers.character.thor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.engine.client.model.BentParts;

// Thor's axe, standing in for Mjolnir until the hammer is made: it hangs from his belt on his left hip, and in flight
// his left hand holds it out ahead of him.
public final class ThorAxeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ItemInHandRenderer items;
    @Nullable
    private ItemStack axe;

    private ThorAxeLayer(PlayerRenderer renderer, ItemInHandRenderer items) {
        super(renderer);
        this.items = items;
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new ThorAxeLayer(player, event.getContext().getItemInHandRenderer()));
            }
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (ClientCharacter.of(player) != GameCharacter.THOR || player.isInvisible()) {
            return;
        }
        if (this.axe == null) {
            this.axe = new ItemStack(Items.IRON_AXE);
        }
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        pose.pushPose();
        if (ThorPoses.axeInHand(player)) {
            model.translateToHand(HumanoidArm.LEFT, pose);
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.translate(-1.0F / 16.0F, 0.125F, -0.625F);
            this.items.renderItem(player, this.axe, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, true, pose, buffers,
                    light);
        } else {
            model.body.translateAndRotate(pose);
            // The belt sits below the waist, so it turns with the trunk's lower half.
            BentParts.farHalf(model.body, pose);
            pose.translate(ThorPoses.BELT.x / 16.0F, ThorPoses.BELT.y / 16.0F, ThorPoses.BELT.z / 16.0F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-80.0F));
            pose.scale(0.62F, 0.62F, 0.62F);
            pose.translate(0.05F, 0.2F, 0.0F);
            this.items.renderItem(player, this.axe, ItemDisplayContext.NONE, true, pose, buffers, light);
        }
        pose.popPose();
    }
}
