package nl.tivek.multiversepowers.creature.bloater.client;

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
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.bloater.BloaterSpit;

// A ball of the Bloater's spit: a dark acid core in a wobbling see-through shell of slime, tumbling as it flies.
final class SpitRenderer extends EntityRenderer<BloaterSpit> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "bloater_spit"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "textures/entity/bloater_spit.png");
    private final ModelPart core;
    private final ModelPart shell;

    SpitRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(LAYER);
        this.core = root.getChild("core");
        this.shell = root.getChild("shell");
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -2.0F,
                4.0F, 4.0F, 4.0F), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 8).addBox(-3.0F, -3.0F, -3.0F,
                6.0F, 6.0F, 6.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void render(BloaterSpit spit, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = spit.tickCount + partialTick;
        float wobble = 1.0F + 0.14F * Mth.sin(age * 1.4F);
        pose.pushPose();
        pose.translate(0.0F, 0.2F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(age * 23.0F));
        pose.mulPose(Axis.XP.rotationDegrees(age * 17.0F));
        pose.scale(wobble, 2.0F - wobble, wobble);
        this.core.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), 0xF000F0,
                OverlayTexture.NO_OVERLAY);
        this.shell.render(pose, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)), 0xF000F0,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(spit, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BloaterSpit spit) {
        return TEXTURE;
    }
}
