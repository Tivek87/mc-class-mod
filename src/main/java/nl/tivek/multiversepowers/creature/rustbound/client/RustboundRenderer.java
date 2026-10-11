package nl.tivek.multiversepowers.creature.rustbound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.rustbound.Rustbound;
import nl.tivek.multiversepowers.creature.rustbound.RustboundMoves;

// The Rustbound drawn from its own model, its eyes glowing a cold blue and its magnet core glowing over its chest
// (brighter as it pulls), and the scrap circling round it: slowly as it walks, fast and close as it pulls, gathered to
// its left hand as it winds up a throw.
public final class RustboundRenderer extends MobRenderer<Rustbound, RustboundModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "rustbound"), "main");
    private static final ResourceLocation TEXTURE = texture("rustbound");
    private static final RenderType EYES = RenderType.eyes(texture("rustbound_eyes"));
    private static final ResourceLocation GLOW = texture("rustbound_glow");
    private final ModelPart shard;

    private RustboundRenderer(EntityRendererProvider.Context context) {
        super(context, new RustboundModel(context.bakeLayer(LAYER)), 0.5F);
        this.shard = context.bakeLayer(ShardRenderer.LAYER);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
        this.addLayer(new Glow(this));
    }

    static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "textures/entity/" + name + ".png");
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Rustbound.TYPE.get(), RustboundRenderer::new);
        event.registerEntityRenderer(Rustbound.SHARD.get(), ShardRenderer::new);
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, RustboundModel::createLayer);
        event.registerLayerDefinition(ShardRenderer.LAYER, ShardRenderer::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Rustbound rustbound) {
        return TEXTURE;
    }

    @Override
    public void render(Rustbound rustbound, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        super.render(rustbound, yaw, partialTick, pose, buffers, light);
        if (rustbound.isInvisible() || rustbound.deathTime > 0) {
            return;
        }
        this.circle(rustbound, partialTick, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ShardRenderer
                .TEXTURE)), light);
    }

    // The scrap circling it, each piece tumbling on its own path round its chest.
    private void circle(Rustbound rustbound, float partialTick, PoseStack pose, VertexConsumer buffer, int light) {
        int count = rustbound.shards();
        if (count <= 0) {
            return;
        }
        float age = rustbound.tickCount + partialTick;
        int move = rustbound.move();
        double t = rustbound.moveAge(partialTick);
        float pulling = move == RustboundMoves.PULL && !rustbound.wet() ? (float) Mth.clamp(t / RustboundMoves
                .PULL_FROM, 0.0, 1.0) * (float) (1.0 - Mth.clamp((t - RustboundMoves.YANK) / 4.0, 0.0, 1.0)) : 0.0F;
        float gather = move == RustboundMoves.SHRAPNEL ? (float) Mth.clamp(t / (RustboundMoves.THROW - 1), 0.0, 1.0)
                : 0.0F;
        float body = Mth.rotLerp(partialTick, rustbound.yBodyRotO, rustbound.yBodyRot);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-body));
        float spin = age * (0.06F + 0.22F * pulling);
        for (int i = 0; i < count; i++) {
            float phase = i * Mth.TWO_PI / Rustbound.MOST_SHARDS + rustbound.getId() * 0.7F;
            float angle = spin + phase;
            float radius = 0.8F - 0.25F * pulling + 0.06F * Mth.sin(age * 0.11F + i);
            float x = Mth.cos(angle) * radius;
            float z = Mth.sin(angle) * radius;
            float y = 1.25F + 0.18F * Mth.sin(age * 0.09F + i * 1.9F);
            // Gathered for a throw, they close in on its left hand, drawn back behind it.
            x = Mth.lerp(gather, x, 0.45F + 0.08F * Mth.cos(phase));
            y = Mth.lerp(gather, y, 1.15F + 0.08F * Mth.sin(phase));
            z = Mth.lerp(gather, z, -0.35F + 0.08F * Mth.sin(phase * 1.3F));
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees(age * (9.0F + i * 3.0F) + i * 50.0F));
            pose.mulPose(Axis.XP.rotationDegrees(age * (7.0F + i * 2.0F) + i * 30.0F));
            pose.scale(0.8F, 0.8F, 0.8F);
            this.shard.render(pose, buffer, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.popPose();
    }

    // Its core's glow, as bright as the model says, drawn over the body in full light.
    private static final class Glow extends RenderLayer<Rustbound, RustboundModel> {
        Glow(LivingEntityRenderer<Rustbound, RustboundModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, Rustbound rustbound,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                float headPitch) {
            float glow = this.getParentModel().glow;
            if (glow <= 0.01F || rustbound.isInvisible()) {
                return;
            }
            int alpha = Math.round(glow * 255.0F);
            this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW)),
                    0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(alpha, 255, 255, 255));
        }
    }
}
