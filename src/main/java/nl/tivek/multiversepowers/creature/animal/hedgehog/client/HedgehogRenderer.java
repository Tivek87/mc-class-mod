package nl.tivek.multiversepowers.creature.animal.hedgehog.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.hedgehog.Hedgehog;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The hedgehog drawn from its own model.
public final class HedgehogRenderer extends WildRenderer<Hedgehog, HedgehogModel> {
    static final ModelLayerLocation LAYER = layer("hedgehog");
    private static final ResourceLocation TEXTURE = texture("hedgehog");

    private HedgehogRenderer(EntityRendererProvider.Context context) {
        super(context, new HedgehogModel(context.bakeLayer(LAYER)), 0.25F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Hedgehog.TYPE.get(), HedgehogRenderer::new);
        act(Hedgehog.CURL, 70);
        act(Hedgehog.SNIFF, 60);
        act(Hedgehog.FORAGE, 80);
        act(Hedgehog.SCRATCH, 40);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Hedgehog.TYPE.get(), ticks, hedgehog -> hedgehog.play(kind), hedgehog -> hedgehog.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, HedgehogModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Hedgehog hedgehog) {
        return TEXTURE;
    }
}
