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
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.tivek.multiversepowers.character.greenlantern.summon.HardLight;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

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
        MobPreview.move(HardLight.SKELETON.get(), 50, s -> s.setAggressive(true), s -> s.setAggressive(false));
        MobPreview.move(HardLight.PIGLIN.get(), 50, p -> p.setAggressive(true), p -> p.setAggressive(false));
        MobPreview.looks(HardLight.CREEPER.get());
        MobPreview.looks(HardLight.BLAZE.get());
        MobPreview.move(HardLight.VINDICATOR.get(), 50, v -> v.setAggressive(true), v -> v.setAggressive(false));
        MobPreview.move(HardLight.VINDICATOR.get(), 60, v -> v.setCelebrating(true), v -> v.setCelebrating(false));
        MobPreview.move(HardLight.ENDERMAN.get(), 80, e -> e.setCarriedBlock(Blocks.GRASS_BLOCK.defaultBlockState()),
                e -> e.setCarriedBlock(null));
        // The game's own entity events: 11 holds out a poppy, 34 takes it back.
        MobPreview.move(HardLight.GOLEM.get(), 80, g -> g.handleEntityEvent((byte) 11),
                g -> g.handleEntityEvent((byte) 34));
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
