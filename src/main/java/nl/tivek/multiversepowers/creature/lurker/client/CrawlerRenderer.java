package nl.tivek.multiversepowers.creature.lurker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.lurker.Crawler;
import nl.tivek.multiversepowers.creature.lurker.Lurker;
import nl.tivek.multiversepowers.creature.screamer.Feral;

// A crawler drawn from LurkerModel in its own skin and size, its eyes glowing, sunk into the ground as far as it has
// yet to climb out of it.
public final class CrawlerRenderer<T extends MovingMonster & Crawler> extends MobRenderer<T, LurkerModel<T>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "crawler"), "main");
    // How deep (blocks) one still in the ground lies.
    private static final float BURIED = 1.7F;
    private final ResourceLocation texture;
    private final float size;

    public CrawlerRenderer(EntityRendererProvider.Context context, String skin, float size) {
        super(context, new LurkerModel<>(context.bakeLayer(LAYER)), 0.55F * size);
        this.texture = texture(skin);
        this.size = size;
        RenderType eyes = RenderType.eyes(texture(skin + "_eyes"));
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return eyes;
            }
        });
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "textures/entity/" + name + ".png");
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Lurker.TYPE.get(), context -> new CrawlerRenderer<>(context, "lurker", 1.0F));
        event.registerEntityRenderer(Feral.TYPE.get(), context -> new CrawlerRenderer<>(context, "feral", 0.7F));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, LurkerModel::createLayer);
    }

    @Override
    protected void scale(T crawler, PoseStack pose, float partialTick) {
        float buried = 1.0F - crawler.risen(partialTick);
        if (buried > 0.0F) {
            // The pose is flipped upside down here: +y is down.
            pose.translate(0.0F, buried * BURIED, 0.0F);
        }
        pose.scale(this.size, this.size, this.size);
    }

    @Override
    public ResourceLocation getTextureLocation(T crawler) {
        return this.texture;
    }
}
