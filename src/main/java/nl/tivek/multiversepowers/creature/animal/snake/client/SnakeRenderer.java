package nl.tivek.multiversepowers.creature.animal.snake.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.snake.Snake;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The snake drawn from its own model: a grass snake or an adder.
public final class SnakeRenderer extends WildRenderer<Snake, SnakeModel> {
    static final ModelLayerLocation LAYER = layer("snake");
    private static final ResourceLocation GRASS = texture("snake_grass");
    private static final ResourceLocation ADDER = texture("snake_adder");

    private SnakeRenderer(EntityRendererProvider.Context context) {
        super(context, new SnakeModel(context.bakeLayer(LAYER)), 0.2F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Snake.TYPE.get(), SnakeRenderer::new);
        act(Snake.COIL, 70);
        act(Snake.STRIKE, Snake.STRIKE_END);
        act(Snake.BASK, 80);
        act(Snake.PERISCOPE, 60);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Snake.TYPE.get(), ticks, snake -> snake.play(kind), snake -> snake.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, SnakeModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Snake snake) {
        return snake.variant() == Snake.ADDER ? ADDER : GRASS;
    }
}
