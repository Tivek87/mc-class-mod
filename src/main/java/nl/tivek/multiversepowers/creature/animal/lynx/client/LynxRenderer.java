package nl.tivek.multiversepowers.creature.animal.lynx.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.lynx.Lynx;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The lynx drawn from its own model, tawny or in its pale winter coat; a kitten half its size.
public final class LynxRenderer extends WildRenderer<Lynx, LynxModel> {
    static final ModelLayerLocation LAYER = layer("lynx");
    private static final ResourceLocation TAWNY = texture("lynx");
    private static final ResourceLocation PALE = texture("lynx_pale");

    private LynxRenderer(EntityRendererProvider.Context context) {
        super(context, new LynxModel(context.bakeLayer(LAYER)), 0.45F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Lynx.TYPE.get(), LynxRenderer::new);
        act(Lynx.SIT, 100);
        act(Lynx.GROOM, 90);
        act(Lynx.STRETCH, Lynx.STRETCH_END);
        act(Lynx.LIE, 120);
        act(Lynx.HISS, 40);
        act(Lynx.SWIPE, Lynx.SWIPE_END);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Lynx.TYPE.get(), ticks, lynx -> lynx.play(kind), lynx -> lynx.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, LynxModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Lynx lynx) {
        return lynx.variant() == Lynx.PALE ? PALE : TAWNY;
    }
}
