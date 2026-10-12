package nl.tivek.multiversepowers.creature.animal.bear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.bear.BrownBear;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The brown bear drawn from its own model; a cub half its size.
public final class BearRenderer extends MobRenderer<BrownBear, BearModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "brown_bear"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "textures/entity/brown_bear.png");

    private BearRenderer(EntityRendererProvider.Context context) {
        super(context, new BearModel(context.bakeLayer(LAYER)), 0.9F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BrownBear.TYPE.get(), BearRenderer::new);
        act(BrownBear.SIT, 90);
        act(BrownBear.REAR, 70);
        act(BrownBear.REST, 110);
        act(BrownBear.SHAKE, BrownBear.SHAKE_END);
        act(BrownBear.ROAR, BrownBear.ROAR_END);
        act(BrownBear.MAUL, BrownBear.MAUL_END + 6);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(BrownBear.TYPE.get(), ticks, bear -> bear.play(kind), bear -> bear.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, BearModel::createLayer);
    }

    @Override
    protected void scale(BrownBear bear, PoseStack poseStack, float partialTick) {
        float size = bear.getAgeScale();
        poseStack.scale(size, size, size);
    }

    @Override
    protected float getShadowRadius(BrownBear bear) {
        return super.getShadowRadius(bear) * bear.getAgeScale();
    }

    @Override
    public ResourceLocation getTextureLocation(BrownBear bear) {
        return TEXTURE;
    }
}
