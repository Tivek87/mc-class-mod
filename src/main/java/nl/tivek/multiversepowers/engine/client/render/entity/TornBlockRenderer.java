package nl.tivek.multiversepowers.engine.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import nl.tivek.multiversepowers.engine.entity.TornBlock;
import nl.tivek.multiversepowers.engine.math.Noise;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A torn-out block on its way in: tumbling about an axis of its own, stretched out along its way and thinner the
// nearer it falls, smaller and smaller until it is gone.
public final class TornBlockRenderer extends EntityRenderer<TornBlock> {
    private TornBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TornBlock.TYPE.get(), TornBlockRenderer::new);
    }

    @Override
    public void render(TornBlock block, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        BlockState state = block.state();
        if (state.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        double run = block.run(partialTick);
        Vec3 at = block.at(run);
        Vec3 way = block.at(Math.min(1.0, run + 0.03)).subtract(at);
        double fall = Math.pow(run, 1.5);
        float size = (float) (1.0 - 0.75 * fall);
        float stretch = (float) (1.0 + 2.2 * fall * fall);
        float thin = (float) (1.0 - 0.45 * fall);
        Vec3 drawn = block.getPosition(partialTick);
        pose.pushPose();
        pose.translate(at.x - drawn.x, at.y - drawn.y, at.z - drawn.z);
        if (way.lengthSqr() > 1.0E-6) {
            Vector3f along = way.normalize().toVector3f();
            Quaternionf turn = new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), along);
            pose.mulPose(turn);
            pose.scale(thin, thin, stretch);
            pose.mulPose(turn.conjugate());
        }
        int seed = block.getId();
        Vector3f axis = new Vector3f((float) Noise.of(seed, 1, 3) - 0.5F, (float) Noise.of(seed, 2, 3) - 0.5F,
                (float) Noise.of(seed, 3, 3) - 0.5F);
        if (axis.lengthSquared() < 1.0E-4F) {
            axis.set(0.0F, 1.0F, 0.0F);
        }
        float spin = (float) (block.age(partialTick) * (0.12 + 0.18 * Noise.of(seed, 4, 3)) * (1.0 + 2.0 * fall));
        pose.mulPose(new Quaternionf().rotationAxis(spin, axis.normalize()));
        pose.scale(size, size, size);
        pose.translate(-0.5F, -0.5F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light,
                OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(TornBlock block) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
