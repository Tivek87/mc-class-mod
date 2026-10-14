package nl.tivek.multiversepowers.creature.animal.flamingo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.flamingo.Flamingo;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The flamingo drawn from its own rig: the American flamingo, the larger greater flamingo, or a chick in grey down on
// a rig of its own. The design stands about two blocks tall; it is drawn at the height of a tall flamingo beside a
// player (the American 1.2 to 1.45 metres, the greater up to about 1.9), its chick a little under half of it.
public final class FlamingoRenderer extends WildRenderer<Flamingo, FlamingoModel> {
    static final ModelLayerLocation LAYER = layer("flamingo");
    static final ModelLayerLocation CHICK_LAYER = layer("flamingo_chick");
    private static final ResourceLocation AMERICAN = texture("flamingo");
    private static final ResourceLocation GREATER = texture("flamingo_greater");
    private static final ResourceLocation CHICK = texture("flamingo_chick");
    private final FlamingoModel adult;
    private final FlamingoModel chick;

    private FlamingoRenderer(EntityRendererProvider.Context context) {
        super(context, new FlamingoModel(context.bakeLayer(LAYER), false), 0.4F);
        this.adult = this.model;
        this.chick = new FlamingoModel(context.bakeLayer(CHICK_LAYER), true);
    }

    @Override
    public void render(Flamingo flamingo, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int light) {
        this.model = flamingo.isBaby() ? this.chick : this.adult;
        super.render(flamingo, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    protected void scale(Flamingo flamingo, PoseStack poseStack, float partialTick) {
        float size = flamingo.size();
        poseStack.scale(size, size, size);
    }

    @Override
    protected float getShadowRadius(Flamingo flamingo) {
        return 0.4F * flamingo.size();
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Flamingo.TYPE.get(), FlamingoRenderer::new);
        act(Flamingo.ONE_LEG, 140);
        act(Flamingo.FEED, 100);
        act(Flamingo.PREEN, 70);
        act(Flamingo.DISPLAY, Flamingo.DISPLAY_END);
        act(Flamingo.HONK, Flamingo.HONK_END);
        act(Flamingo.STRETCH, 44);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Flamingo.TYPE.get(), ticks, flamingo -> flamingo.play(kind), flamingo -> flamingo.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, FlamingoModel::createLayer);
        event.registerLayerDefinition(CHICK_LAYER, FlamingoModel::createChickLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(Flamingo flamingo) {
        return flamingo.isBaby() ? CHICK : flamingo.variant() == Flamingo.GREATER ? GREATER : AMERICAN;
    }
}
