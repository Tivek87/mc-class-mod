package nl.tivek.multiversepowers.creature.animal.giraffe.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.giraffe.Giraffe;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The giraffe drawn from its own model; a calf half its size in its own paler coat.
public final class GiraffeRenderer extends WildRenderer<Giraffe, GiraffeModel> {
    static final ModelLayerLocation LAYER = layer("giraffe");
    private static final ResourceLocation TEXTURE = texture("giraffe");
    private static final ResourceLocation CALF = texture("giraffe_calf");

    private GiraffeRenderer(EntityRendererProvider.Context context) {
        super(context, new GiraffeModel(context.bakeLayer(LAYER)), 0.9F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Giraffe.TYPE.get(), GiraffeRenderer::new);
        act(Giraffe.BROWSE, 132);
        act(Giraffe.DRINK, 120);
        act(Giraffe.CHEW, 80);
        act(Giraffe.KICK, Giraffe.KICK_END);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Giraffe.TYPE.get(), ticks, giraffe -> giraffe.play(kind), giraffe -> giraffe.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, GiraffeModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Giraffe giraffe) {
        return giraffe.isBaby() ? CALF : TEXTURE;
    }
}
