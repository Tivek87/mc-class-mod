package nl.tivek.multiversepowers.creature.animal.squirrel.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.squirrel.Squirrel;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The squirrel drawn from its own model, red or grey.
public final class SquirrelRenderer extends WildRenderer<Squirrel, SquirrelModel> {
    static final ModelLayerLocation LAYER = layer("squirrel");
    private static final ResourceLocation RED = texture("squirrel_red");
    private static final ResourceLocation GREY = texture("squirrel_grey");

    private SquirrelRenderer(EntityRendererProvider.Context context) {
        super(context, new SquirrelModel(context.bakeLayer(LAYER)), 0.2F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Squirrel.TYPE.get(), SquirrelRenderer::new);
        act(Squirrel.SIT_UP, 60);
        act(Squirrel.NIBBLE, 70);
        act(Squirrel.DIG, 50);
        act(Squirrel.FLICK, 40);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Squirrel.TYPE.get(), ticks, squirrel -> squirrel.play(kind), squirrel -> squirrel.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, SquirrelModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Squirrel squirrel) {
        return squirrel.variant() == Squirrel.GREY ? GREY : RED;
    }
}
