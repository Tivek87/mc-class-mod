package nl.tivek.multiversepowers.creature.rustbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.rustbound.RustShard;

// A jagged piece of rusted scrap: a bent strip with a torn-off corner, tumbling end over end as it flies.
final class ShardRenderer extends EntityRenderer<RustShard> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "rust_shard"), "main");
    static final ResourceLocation TEXTURE = RustboundRenderer.texture("rust_shard");
    private final ModelPart shard;

    ShardRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shard = context.bakeLayer(LAYER);
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("strip", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 5.0F, 1.0F)
                .texOffs(4, 0).addBox(-1.5F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F)
                .texOffs(0, 6).addBox(0.5F, 1.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.rotation(0.0F, 0.0F, 0.35F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void render(RustShard shard, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = shard.tickCount + partialTick;
        pose.pushPose();
        pose.translate(0.0F, 0.15F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(age * 31.0F + shard.getId() * 40.0F));
        pose.mulPose(Axis.XP.rotationDegrees(age * 47.0F));
        this.shard.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(shard, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RustShard shard) {
        return TEXTURE;
    }
}
