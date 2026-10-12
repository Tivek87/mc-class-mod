package nl.tivek.multiversepowers.creature.animal.crocodile.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.crocodile.Crocodile;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The crocodile drawn from its own model; a hatchling half its size, yellower and boldly banded.
public final class CrocodileRenderer extends WildRenderer<Crocodile, CrocodileModel> {
    static final ModelLayerLocation LAYER = layer("crocodile");
    private static final ResourceLocation TEXTURE = texture("crocodile");
    private static final ResourceLocation HATCHLING = texture("crocodile_hatchling");

    private CrocodileRenderer(EntityRendererProvider.Context context) {
        super(context, new CrocodileModel(context.bakeLayer(LAYER)), 0.8F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Crocodile.TYPE.get(), CrocodileRenderer::new);
        act(Crocodile.GAPE, 80);
        act(Crocodile.BITE, Crocodile.BITE_END);
        act(Crocodile.BASK, 120);
        act(Crocodile.RISE, 100);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Crocodile.TYPE.get(), ticks, crocodile -> crocodile.play(kind),
                crocodile -> crocodile.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, CrocodileModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Crocodile crocodile) {
        return crocodile.isBaby() ? HATCHLING : TEXTURE;
    }
}
