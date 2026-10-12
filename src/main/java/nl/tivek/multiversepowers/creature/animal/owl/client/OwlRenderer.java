package nl.tivek.multiversepowers.creature.animal.owl.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.owl.Owl;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The owl drawn from its own model: tawny or snowy, an owlet in grey down.
public final class OwlRenderer extends WildRenderer<Owl, OwlModel> {
    static final ModelLayerLocation LAYER = layer("owl");
    private static final ResourceLocation TAWNY = texture("owl_tawny");
    private static final ResourceLocation SNOWY = texture("owl_snowy");
    private static final ResourceLocation OWLET = texture("owl_owlet");

    private OwlRenderer(EntityRendererProvider.Context context) {
        super(context, new OwlModel(context.bakeLayer(LAYER)), 0.3F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Owl.TYPE.get(), OwlRenderer::new);
        act(Owl.SLEEP, 100);
        act(Owl.HOOT, Owl.HOOT_END);
        act(Owl.TURN, 60);
        act(Owl.PREEN, 60);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Owl.TYPE.get(), ticks, owl -> owl.play(kind), owl -> owl.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, OwlModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Owl owl) {
        return owl.isBaby() ? OWLET : owl.variant() == Owl.SNOWY ? SNOWY : TAWNY;
    }
}
