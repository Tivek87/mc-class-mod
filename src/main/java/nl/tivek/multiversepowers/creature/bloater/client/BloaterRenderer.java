package nl.tivek.multiversepowers.creature.bloater.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
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
import nl.tivek.multiversepowers.creature.bloater.Bloater;
import nl.tivek.multiversepowers.creature.bloater.BloaterMoves;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The Bloater drawn from its own model, its little eyes glowing, its boils glowing acid green over its skin: faintly as
// they pulse, brightly as it is hurt or swells to burst, while its whole body trembles.
public final class BloaterRenderer extends MobRenderer<Bloater, BloaterModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "bloater"), "main");
    private static final ResourceLocation TEXTURE = texture("bloater");
    private static final RenderType EYES = RenderType.eyes(texture("bloater_eyes"));
    private static final ResourceLocation GLOW = texture("bloater_glow");

    private BloaterRenderer(EntityRendererProvider.Context context) {
        super(context, new BloaterModel(context.bakeLayer(LAYER)), 0.9F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
        this.addLayer(new Glow(this));
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "textures/entity/" + name + ".png");
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Bloater.TYPE.get(), BloaterRenderer::new);
        event.registerEntityRenderer(Bloater.SPIT.get(), SpitRenderer::new);
        MobPreview.move(Bloater.TYPE.get(), BloaterMoves.SHRIEK_END + 10, b -> b.play(BloaterMoves.SHRIEK),
                b -> b.play(0));
        MobPreview.move(Bloater.TYPE.get(), BloaterMoves.SPIT_END + 8, b -> b.play(BloaterMoves.SPIT),
                b -> b.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, BloaterModel::createLayer);
        event.registerLayerDefinition(SpitRenderer.LAYER, SpitRenderer::createLayer);
    }

    // Swollen to burst it trembles all over.
    @Override
    protected void scale(Bloater bloater, PoseStack pose, float partialTick) {
        if (bloater.move() != BloaterMoves.RUSH) {
            return;
        }
        float full = (float) Mth.clamp(bloater.moveAge(partialTick) / BloaterMoves.BLOW, 0.0, 1.0);
        float age = bloater.tickCount + partialTick;
        float shake = 0.04F * full * full;
        pose.translate(shake * Mth.sin(age * 7.3F), 0.0F, shake * Mth.cos(age * 6.1F));
    }

    @Override
    public ResourceLocation getTextureLocation(Bloater bloater) {
        return TEXTURE;
    }

    // Its boils' glow, as bright as the model says, drawn over the body in full light.
    private static final class Glow extends RenderLayer<Bloater, BloaterModel> {
        Glow(LivingEntityRenderer<Bloater, BloaterModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, Bloater bloater, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            float glow = this.getParentModel().glow;
            if (glow <= 0.01F || bloater.isInvisible()) {
                return;
            }
            int alpha = Math.round(glow * 255.0F);
            this.getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW)),
                    0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(alpha, 255, 255, 255));
        }
    }
}
