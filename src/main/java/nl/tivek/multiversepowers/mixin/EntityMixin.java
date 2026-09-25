package nl.tivek.multiversepowers.mixin;

import net.minecraft.world.entity.Entity;
import nl.tivek.multiversepowers.character.greenlantern.ability.ScanGlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$scanColour(CallbackInfoReturnable<Integer> result) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide()) {
            int colour = ScanGlow.colour(self.getId());
            if (colour >= 0) {
                result.setReturnValue(colour);
            }
        }
    }
}
