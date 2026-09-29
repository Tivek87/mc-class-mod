package nl.tivek.multiversepowers.mixin.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.engine.client.pose.Poses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void welcomescreen$lean(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, CallbackInfo info) {
        Poses.apply(Poses.Stage.MODEL, (PlayerModel<?>) (Object) this, entity,
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false), null);
    }
}
