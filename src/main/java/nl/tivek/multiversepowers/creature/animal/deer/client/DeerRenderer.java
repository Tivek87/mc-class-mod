package nl.tivek.multiversepowers.creature.animal.deer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.deer.Deer;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The deer drawn from its own model; a fawn half its size in its spotted coat.
public final class DeerRenderer extends MobRenderer<Deer, DeerModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "deer"), "main");
    private static final ResourceLocation TEXTURE = texture("deer");
    private static final ResourceLocation FAWN = texture("deer_fawn");

    private DeerRenderer(EntityRendererProvider.Context context) {
        super(context, new DeerModel(context.bakeLayer(LAYER)), 0.6F);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "textures/entity/" + name + ".png");
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Deer.TYPE.get(), DeerRenderer::new);
        act(Deer.GRAZE, 120);
        act(Deer.ALERT, 60);
        act(Deer.GROOM, 60);
        act(Deer.BED, 120);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Deer.TYPE.get(), ticks, deer -> deer.play(kind), deer -> deer.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, DeerModel::createLayer);
    }

    @Override
    protected void scale(Deer deer, PoseStack poseStack, float partialTick) {
        float size = deer.getAgeScale();
        poseStack.scale(size, size, size);
    }

    @Override
    protected float getShadowRadius(Deer deer) {
        return super.getShadowRadius(deer) * deer.getAgeScale();
    }

    @Override
    public ResourceLocation getTextureLocation(Deer deer) {
        return deer.isBaby() ? FAWN : TEXTURE;
    }
}
