package nl.tivek.multiversepowers.creature.animal.elephant.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.elephant.Elephant;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The elephant drawn from its own model; a calf half its size in its own browner, downier skin.
public final class ElephantRenderer extends WildRenderer<Elephant, ElephantModel> {
    static final ModelLayerLocation LAYER = layer("elephant");
    private static final ResourceLocation TEXTURE = texture("elephant");
    private static final ResourceLocation CALF = texture("elephant_calf");

    private ElephantRenderer(EntityRendererProvider.Context context) {
        super(context, new ElephantModel(context.bakeLayer(LAYER)), 1.6F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Elephant.TYPE.get(), ElephantRenderer::new);
        act(Elephant.REACH, 140);
        act(Elephant.GRAZE, 128);
        act(Elephant.DUST, Elephant.DUST_END);
        act(Elephant.THREAT, Elephant.THREAT_END);
        act(Elephant.STRIKE, Elephant.STRIKE_END);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Elephant.TYPE.get(), ticks, elephant -> elephant.play(kind), elephant -> elephant.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, ElephantModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Elephant elephant) {
        return elephant.isBaby() ? CALF : TEXTURE;
    }
}
