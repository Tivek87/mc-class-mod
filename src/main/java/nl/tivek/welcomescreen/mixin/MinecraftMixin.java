package nl.tivek.welcomescreen.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import nl.tivek.welcomescreen.character.lantern.ScanGlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A creature the ring's scan marked for you glows: the game draws its outline through walls and everything, as it does
 * for one hit by a spectral arrow, but only for you (see {@link ScanGlow}).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$scanned(Entity entity, CallbackInfoReturnable<Boolean> result) {
        if (ScanGlow.colour(entity.getId()) >= 0) {
            result.setReturnValue(true);
        }
    }
}
