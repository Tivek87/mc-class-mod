package nl.tivek.multiversepowers.creature.elemental.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import nl.tivek.multiversepowers.creature.elemental.spell.HeavedStone;

// A heaved stone: its block, a little smaller than a whole one, turning slowly as it rises and tumbling as it flies.
final class HeavedStoneRenderer extends EntityRenderer<HeavedStone> {
    private static final float SIZE = 0.85F;

    HeavedStoneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(HeavedStone stone, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = stone.tickCount + partialTick;
        float spin = stone.rising() ? age * 3.0F : age * 17.0F;
        pose.pushPose();
        pose.translate(0.0F, SIZE * 0.5F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(spin + stone.getId() * 37.0F));
        pose.mulPose(Axis.XP.rotationDegrees(spin * 0.7F));
        pose.scale(SIZE, SIZE, SIZE);
        pose.translate(-0.5F, -0.5F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(stone.rock(), pose, buffers, light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(stone, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(HeavedStone stone) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
