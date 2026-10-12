package nl.tivek.multiversepowers.creature.animal.moose.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.moose.Moose;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The moose drawn from its own model; a calf half its size in its own lighter coat.
public final class MooseRenderer extends WildRenderer<Moose, MooseModel> {
    static final ModelLayerLocation LAYER = layer("moose");
    private static final ResourceLocation TEXTURE = texture("moose");
    private static final ResourceLocation CALF = texture("moose_calf");

    private MooseRenderer(EntityRendererProvider.Context context) {
        super(context, new MooseModel(context.bakeLayer(LAYER)), 1.0F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Moose.TYPE.get(), MooseRenderer::new);
        act(Moose.BROWSE, 100);
        act(Moose.GRAZE, 120);
        act(Moose.BED, 120);
        act(Moose.SHAKE, Moose.SHAKE_END);
        act(Moose.THREAT, Moose.THREAT_END);
        act(Moose.STOMP, Moose.STOMP_END);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Moose.TYPE.get(), ticks, moose -> moose.play(kind), moose -> moose.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, MooseModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Moose moose) {
        return moose.isBaby() ? CALF : TEXTURE;
    }
}
