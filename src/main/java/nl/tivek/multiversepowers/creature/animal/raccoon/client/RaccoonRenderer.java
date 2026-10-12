package nl.tivek.multiversepowers.creature.animal.raccoon.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.raccoon.Raccoon;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The raccoon drawn from its own model; a kit half its size.
public final class RaccoonRenderer extends MobRenderer<Raccoon, RaccoonModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "raccoon"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "textures/entity/raccoon.png");

    private RaccoonRenderer(EntityRendererProvider.Context context) {
        super(context, new RaccoonModel(context.bakeLayer(LAYER)), 0.35F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Raccoon.TYPE.get(), RaccoonRenderer::new);
        act(Raccoon.SIT_UP, 70);
        act(Raccoon.WASH, 70);
        act(Raccoon.FORAGE, 80);
        act(Raccoon.SCRATCH, 40);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Raccoon.TYPE.get(), ticks, raccoon -> raccoon.play(kind), raccoon -> raccoon.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, RaccoonModel::createLayer);
    }

    @Override
    protected void scale(Raccoon raccoon, PoseStack poseStack, float partialTick) {
        float size = raccoon.getAgeScale();
        poseStack.scale(size, size, size);
    }

    @Override
    protected float getShadowRadius(Raccoon raccoon) {
        return super.getShadowRadius(raccoon) * raccoon.getAgeScale();
    }

    @Override
    public ResourceLocation getTextureLocation(Raccoon raccoon) {
        return TEXTURE;
    }
}
