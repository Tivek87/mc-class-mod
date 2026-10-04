package nl.tivek.multiversepowers.mixin.client;

import net.minecraft.world.level.Level;
import nl.tivek.multiversepowers.engine.client.world.LocalWeather;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelWeatherMixin {
    // The integrated server's worlds run in this game too: only the world it draws gets the local weather.
    @Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
    private void welcomescreen$rain(float partialTick, CallbackInfoReturnable<Float> info) {
        Level level = (Level) (Object) this;
        if (level.isClientSide) {
            info.setReturnValue(LocalWeather.rain(level, partialTick, info.getReturnValueF()));
        }
    }

    @Inject(method = "getThunderLevel", at = @At("RETURN"), cancellable = true)
    private void welcomescreen$thunder(float partialTick, CallbackInfoReturnable<Float> info) {
        Level level = (Level) (Object) this;
        if (level.isClientSide) {
            info.setReturnValue(LocalWeather.thunder(level, partialTick, info.getReturnValueF()));
        }
    }
}
