package nl.tivek.multiversepowers.creature.animal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.WildAnimal;

// A wild animal drawn from its own model, its young half the size.
public abstract class WildRenderer<T extends WildAnimal, M extends EntityModel<T>> extends MobRenderer<T, M> {
    protected WildRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
    }

    protected static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, name), "main");
    }

    protected static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "textures/entity/" + name + ".png");
    }

    @Override
    protected void scale(T animal, PoseStack poseStack, float partialTick) {
        float size = animal.getAgeScale();
        poseStack.scale(size, size, size);
    }

    @Override
    protected float getShadowRadius(T animal) {
        return super.getShadowRadius(animal) * animal.getAgeScale();
    }
}
