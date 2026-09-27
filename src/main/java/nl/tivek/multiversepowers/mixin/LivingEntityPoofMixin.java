package nl.tivek.multiversepowers.mixin;

import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// A body that stays where it fell does not vanish in a puff of smoke yet: it puffs away once it has sunk.
@Mixin(LivingEntity.class)
public abstract class LivingEntityPoofMixin {
    @Inject(method = "makePoofParticles", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$keepBody(CallbackInfo info) {
        if (Ragdolls.keepsBody((LivingEntity) (Object) this)) {
            info.cancel();
        }
    }
}
