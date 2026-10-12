package nl.tivek.multiversepowers.creature.animal.monkey.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.monkey.Monkey;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The monkey drawn from its own model, white-faced or brown; its young half the size.
public final class MonkeyRenderer extends WildRenderer<Monkey, MonkeyModel> {
    static final ModelLayerLocation LAYER = layer("monkey");
    private static final ResourceLocation WHITE_FACED = texture("monkey_white_faced");
    private static final ResourceLocation BROWN = texture("monkey_brown");

    private MonkeyRenderer(EntityRendererProvider.Context context) {
        super(context, new MonkeyModel(context.bakeLayer(LAYER)), 0.3F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Monkey.TYPE.get(), MonkeyRenderer::new);
        act(Monkey.SIT, 100);
        act(Monkey.SCRATCH, 50);
        act(Monkey.EAT, 90);
        act(Monkey.HOOT, Monkey.HOOT_END);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Monkey.TYPE.get(), ticks, monkey -> monkey.play(kind), monkey -> monkey.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, MonkeyModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Monkey monkey) {
        return monkey.variant() == Monkey.BROWN ? BROWN : WHITE_FACED;
    }
}
