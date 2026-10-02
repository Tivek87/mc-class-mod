package nl.tivek.multiversepowers.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.engine.client.pose.Poses;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ashes;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import nl.tivek.multiversepowers.engine.client.rig.BoneView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Right after any creature's model has taken its own pose, so a pose of ours wins over each model's own setupAnim.
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;"
                            + "FFFFF)V", shift = At.Shift.AFTER))
    private void welcomescreen$pose(LivingEntity entity, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, CallbackInfo info) {
        LivingEntityRenderer<?, ?> renderer = (LivingEntityRenderer<?, ?>) (Object) this;
        Poses.apply(Poses.Stage.CREATURE, renderer.getModel(), entity, partialTick, pose.last().pose());
        Ragdolls.pose(renderer.getModel(), entity, partialTick, pose);
        BoneView.model(renderer.getModel(), pose.last().pose());
        Ashes.shape(renderer.getModel(), entity, partialTick, pose.last().pose());
    }

    // A layer drawn from the creature's own frame rather than from a part of it follows its limp body.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render("
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
                            + "ILnet/minecraft/world/entity/Entity;FFFFFF)V"))
    private void welcomescreen$layer(RenderLayer layer, PoseStack pose, MultiBufferSource buffers, int light,
            Entity entity, float limbSwing, float limbSwingAmount, float partialTick, float age, float yaw,
            float pitch) {
        Ragdolls.carry(layer, entity, pose);
        layer.render(pose, buffers, light, entity, limbSwing, limbSwingAmount, partialTick, age, yaw, pitch);
        Ragdolls.carried(pose);
    }
}
