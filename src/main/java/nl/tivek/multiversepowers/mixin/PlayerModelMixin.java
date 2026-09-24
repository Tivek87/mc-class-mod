package nl.tivek.multiversepowers.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Once the game has posed a player's body, a Green Lantern fighting with the sword and shield bends his upper body
 * forward into his moves and steps into them (see {@link SwordArms#lean}): the game itself only knows a body that
 * stands up straight or crouches all the way.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void welcomescreen$lean(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, CallbackInfo info) {
        SwordArms.lean((PlayerModel<?>) (Object) this, entity);
    }
}
