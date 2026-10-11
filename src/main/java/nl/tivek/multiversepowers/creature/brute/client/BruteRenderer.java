package nl.tivek.multiversepowers.creature.brute.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.brute.ZombieBrute;

// The brute drawn from its own model, a fifth bigger than its pixels, its eyes glowing in the dark.
public final class BruteRenderer extends MobRenderer<ZombieBrute, BruteModel> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "zombie_brute"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "textures/entity/zombie_brute.png");
    private static final RenderType EYES = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "textures/entity/zombie_brute_eyes.png"));
    private static final float SCALE = 1.2F;

    private BruteRenderer(EntityRendererProvider.Context context) {
        super(context, new BruteModel(context.bakeLayer(LAYER)), 0.8F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ZombieBrute.TYPE.get(), BruteRenderer::new);
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, BruteModel::createLayer);
    }

    @Override
    protected void scale(ZombieBrute brute, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieBrute brute) {
        return TEXTURE;
    }
}
