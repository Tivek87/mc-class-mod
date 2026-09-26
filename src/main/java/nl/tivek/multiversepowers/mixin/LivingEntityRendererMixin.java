package nl.tivek.multiversepowers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.greenlantern.client.HandVictims;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
        HandVictims.ears(((LivingEntityRenderer<?, ?>) (Object) this).getModel(), entity, partialTick);
    }
}
