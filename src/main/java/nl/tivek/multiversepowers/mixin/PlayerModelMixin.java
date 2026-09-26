package nl.tivek.multiversepowers.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.WhipArms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void welcomescreen$lean(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, CallbackInfo info) {
        SwordArms.lean((PlayerModel<?>) (Object) this, entity);
        FlameArms.lean((PlayerModel<?>) (Object) this, entity);
        WhipArms.lean((PlayerModel<?>) (Object) this, entity);
    }
}
