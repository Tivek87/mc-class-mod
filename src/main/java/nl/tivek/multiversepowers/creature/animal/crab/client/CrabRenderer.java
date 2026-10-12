package nl.tivek.multiversepowers.creature.animal.crab.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.creature.animal.client.WildRenderer;
import nl.tivek.multiversepowers.creature.animal.crab.Crab;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The crab drawn from its own model, turned side on to the way it goes: a crab walks sideways.
public final class CrabRenderer extends WildRenderer<Crab, CrabModel> {
    static final ModelLayerLocation LAYER = layer("crab");
    private static final ResourceLocation TEXTURE = texture("crab");

    private CrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(LAYER)), 0.3F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Crab.TYPE.get(), CrabRenderer::new);
        act(Crab.FEED, 80);
        act(Crab.WAVE, 60);
        act(Crab.SNAP, 54);
        act(Crab.BURROW, 90);
        act(Crab.PINCH, Crab.PINCH_END);
    }

    private static void act(int kind, int ticks) {
        MobPreview.move(Crab.TYPE.get(), ticks, crab -> crab.play(kind), crab -> crab.play(0));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, CrabModel::createLayer);
    }

    @Override
    protected void setupRotations(Crab crab, PoseStack poseStack, float bob, float yBodyRot, float partialTick,
            float scale) {
        // Snapping at someone or pinching, it turns its front and claws to them.
        float snap = crab.blend(Crab.SNAP, partialTick);
        float front = Math.max(snap, crab.blend(Crab.PINCH, partialTick));
        float look = Mth.wrapDegrees(Mth.lerp(partialTick, crab.yHeadRotO, crab.yHeadRot) - yBodyRot) * snap;
        float side = (crab.leftward() ? 90.0F : -90.0F) * (1.0F - front);
        super.setupRotations(crab, poseStack, bob, yBodyRot + look + side, partialTick, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(Crab crab) {
        return TEXTURE;
    }
}
