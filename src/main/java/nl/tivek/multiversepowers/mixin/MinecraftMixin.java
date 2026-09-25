package nl.tivek.multiversepowers.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import nl.tivek.multiversepowers.character.greenlantern.ability.ScanGlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$scanned(Entity entity, CallbackInfoReturnable<Boolean> result) {
        if (ScanGlow.colour(entity.getId()) >= 0) {
            result.setReturnValue(true);
        }
    }
}
