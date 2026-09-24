package nl.tivek.welcomescreen.mixin;

import net.minecraft.world.entity.Entity;
import nl.tivek.welcomescreen.character.lantern.ScanGlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A creature the ring's scan marked for you glows in the colour of what it is to you: red when it is out to hurt you,
 * green otherwise (see {@link ScanGlow}). The game colours a glowing outline by this; only in your own game.
 */
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
