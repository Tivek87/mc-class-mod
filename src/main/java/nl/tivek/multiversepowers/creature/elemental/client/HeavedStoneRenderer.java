package nl.tivek.multiversepowers.creature.elemental.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.spell.HeavedStone;
import nl.tivek.multiversepowers.engine.client.fx.Energy;

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
        // Torn out of the ground in a cloud of clods; thrown, it drags its dust behind it.
        VertexConsumer energy = Energy.buffer(buffers);
        if (energy != null) {
            var facing = this.entityRenderDispatcher.cameraOrientation();
            Vec3 middle = new Vec3(0.0, SIZE * 0.5, 0.0);
            Vec3 way = stone.getDeltaMovement();
            double speed = way.length();
            Vec3 back = stone.rising() || speed < 1.0E-3 ? new Vec3(0.0, -1.0, 0.0) : way.scale(-1.0 / speed);
            Energy.puff(pose, energy, middle, 1.0F, facing, Energy.DUST, Element.EARTH.colour, 0.85F, stone.getId());
            for (int k = 1; k <= 3; k++) {
                Energy.puff(pose, energy, middle.add(back.scale(k * 0.6)), 0.9F - 0.18F * k, facing, Energy.DUST,
                        Element.EARTH.colour, 0.7F - 0.18F * k, stone.getId() + k);
            }
        }
        super.render(stone, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(HeavedStone stone) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
