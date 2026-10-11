package nl.tivek.multiversepowers.creature.screamer.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.screamer.Screamer;
import nl.tivek.multiversepowers.creature.screamer.ScreamerMoves;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The Screamer drawn from its own model, its sunken eyes glowing a sick red.
public final class ScreamerRenderer extends MobRenderer<Screamer, ScreamerModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "screamer"), "main");
    private static final ResourceLocation TEXTURE = texture("screamer");
    private static final RenderType EYES = RenderType.eyes(texture("screamer_eyes"));

    private ScreamerRenderer(EntityRendererProvider.Context context) {
        super(context, new ScreamerModel(context.bakeLayer(LAYER)), 0.45F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "textures/entity/" + name + ".png");
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Screamer.TYPE.get(), ScreamerRenderer::new);
        MobPreview.move(Screamer.TYPE.get(), ScreamerMoves.SCREECH_END + 8, s -> s.play(ScreamerMoves.SCREECH),
                s -> s.play(0));
        MobPreview.move(Screamer.TYPE.get(), ScreamerMoves.BITE_END + 8, s -> s.play(ScreamerMoves.BITE),
                s -> s.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, ScreamerModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Screamer screamer) {
        return TEXTURE;
    }
}
