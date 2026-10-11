package nl.tivek.multiversepowers.creature.stalker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.stalker.Stalker;
import nl.tivek.multiversepowers.creature.stalker.StalkerMoves;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The Stalker drawn from its own model, its eyes a cold pale glow; wearing an animal's shape it is drawn as that animal
// (Stalker.shape). Folding into a bush it shrinks down small (and grows again rising out of it), blinking it is
// squeezed thin to nothing and back, and throwing off a borrowed shape it swells up out of it.
public final class StalkerRenderer extends MobRenderer<Stalker, StalkerModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "stalker"), "main");
    private static final ResourceLocation TEXTURE = texture("stalker");
    private static final RenderType EYES = RenderType.eyes(texture("stalker_eyes"));
    private static final float FOLDED = 0.62F;
    private static final float GONE = 3.0F;

    private StalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new StalkerModel(context.bakeLayer(LAYER)), 0.4F);
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
        event.registerEntityRenderer(Stalker.TYPE.get(), StalkerRenderer::new);
        MobPreview.move(Stalker.TYPE.get(), StalkerMoves.SWIPE_END + 8, s -> s.play(StalkerMoves.SWIPE),
                s -> s.play(0));
        // Folds small as in a bush, waits, and stands up again.
        MobPreview.move(Stalker.TYPE.get(), StalkerMoves.FOLD_END + 36, s -> s.play(StalkerMoves.FOLD),
                s -> s.play(StalkerMoves.RISE));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, StalkerModel::createLayer);
    }

    @Override
    public void render(Stalker stalker, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light) {
        Entity shape = stalker.shape();
        if (shape != null) {
            this.entityRenderDispatcher.getRenderer(shape).render(shape, yaw, partialTick, poseStack, buffers, light);
            return;
        }
        super.render(stalker, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    protected void scale(Stalker stalker, PoseStack poseStack, float partialTick) {
        double t = stalker.moveAge(partialTick);
        float across = 1.0F;
        float up = 1.0F;
        switch (stalker.move()) {
            case StalkerMoves.FOLD -> {
                float k = Trunk.in(t, 0.0, StalkerMoves.FOLD_END);
                across = up = 1.0F + (FOLDED - 1.0F) * k;
            }
            case StalkerMoves.RISE -> {
                float k = Trunk.in(t, 0.0, StalkerMoves.RISE_END);
                across = up = FOLDED + (1.0F - FOLDED) * k;
            }
            case StalkerMoves.BLINK -> {
                float k;
                if (t < StalkerMoves.VANISH) {
                    k = (float) (t / StalkerMoves.VANISH);
                    k *= k;
                } else if (t < StalkerMoves.VANISH + GONE) {
                    k = 1.0F;
                } else {
                    k = 1.0F - Trunk.in(t, StalkerMoves.VANISH + GONE, StalkerMoves.BLINK_END);
                }
                across = 1.0F - k;
                up = 1.0F + 0.3F * k;
            }
            case StalkerMoves.REVEAL -> {
                float k = Trunk.in(t, 0.0, StalkerMoves.REVEAL_END * 0.7);
                float over = 0.08F * (float) Math.sin(k * Math.PI);
                across = up = 0.5F + 0.5F * k + over;
            }
            default -> {
                if (stalker.hiding() == Stalker.BUSH) {
                    across = up = FOLDED;
                }
            }
        }
        poseStack.scale(across, up, across);
    }

    @Override
    public ResourceLocation getTextureLocation(Stalker stalker) {
        return TEXTURE;
    }
}
