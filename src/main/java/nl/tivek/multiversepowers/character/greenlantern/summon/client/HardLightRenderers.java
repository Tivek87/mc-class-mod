package nl.tivek.multiversepowers.character.greenlantern.summon.client;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.tivek.multiversepowers.character.greenlantern.summon.HardLight;

// The hard-light creatures drawn as the game draws its own, each with its light laid over it (`HardLightLayer`).
public final class HardLightRenderers {
    private HardLightRenderers() {
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HardLight.SKELETON.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(HardLight.PIGLIN.get(), PiglinLook::new);
        event.registerEntityRenderer(HardLight.CREEPER.get(), CreeperRenderer::new);
        event.registerEntityRenderer(HardLight.BLAZE.get(), BlazeRenderer::new);
        event.registerEntityRenderer(HardLight.VINDICATOR.get(), VindicatorRenderer::new);
        event.registerEntityRenderer(HardLight.ENDERMAN.get(), EndermanRenderer::new);
        event.registerEntityRenderer(HardLight.GOLEM.get(), IronGolemRenderer::new);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>> kind : HardLight.ALL) {
            EntityRenderer<?> renderer = event.getRenderer(kind.get());
            if (renderer instanceof LivingEntityRenderer living) {
                living.addLayer(new HardLightLayer(living));
            }
        }
    }

    // The game's zombified piglin, whose renderer and model only know it by the game's own kind.
    private static final class PiglinLook extends HumanoidMobRenderer<Mob, PiglinModel<Mob>> {
        private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace(
                "textures/entity/piglin/zombified_piglin.png");

        PiglinLook(EntityRendererProvider.Context context) {
            super(context, new ZombieArms(context.bakeLayer(ModelLayers.ZOMBIFIED_PIGLIN)), 0.5F, 1.0019531F, 1.0F,
                    1.0019531F);
            this.addLayer(new HumanoidArmorLayer<>(this,
                    new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR)),
                    new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR)),
                    context.getModelManager()));
        }

        @Override
        public ResourceLocation getTextureLocation(Mob mob) {
            return TEXTURE;
        }
    }

    private static final class ZombieArms extends PiglinModel<Mob> {
        ZombieArms(ModelPart root) {
            super(root);
            this.rightEar.visible = false;
        }

        @Override
        public void setupAnim(Mob mob, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                float headPitch) {
            super.setupAnim(mob, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, mob.isAggressive(), this.attackTime,
                    ageInTicks);
        }
    }
}
