package nl.tivek.multiversepowers.creature.animal.duck.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.duck.Duck;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The duck drawn from its own model: a drake, a hen, or a duckling half their size in its own down.
public final class DuckRenderer extends WildRenderer<Duck, DuckModel> {
    static final ModelLayerLocation LAYER = layer("duck");
    private static final ResourceLocation DRAKE = texture("duck_drake");
    private static final ResourceLocation HEN = texture("duck_hen");
    private static final ResourceLocation DUCKLING = texture("duckling");

    private DuckRenderer(EntityRendererProvider.Context context) {
        super(context, new DuckModel(context.bakeLayer(LAYER)), 0.25F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Duck.TYPE.get(), DuckRenderer::new);
        act(Duck.PREEN, 70);
        act(Duck.STRETCH, 30);
        act(Duck.QUACK, Duck.QUACK_END);
        act(Duck.TUCK, 100);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Duck.TYPE.get(), ticks, duck -> duck.play(kind), duck -> duck.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, DuckModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Duck duck) {
        return duck.isBaby() ? DUCKLING : duck.variant() == Duck.HEN ? HEN : DRAKE;
    }
}
