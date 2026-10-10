package nl.tivek.multiversepowers.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.world.LocalSky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelSkyMixin {
    // Overrides LevelAccessor's own: the sky, sun, moon, stars and light read the time from here, while the world's
    // clock (ticked in its level data) is left as it is.
    public long dayTime() {
        ClientLevel level = (ClientLevel) (Object) this;
        return LocalSky.dayTime(level, level.getLevelData().getDayTime());
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void welcomescreen$skyColor(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> info) {
        info.setReturnValue(LocalSky.skyColor((ClientLevel) (Object) this, info.getReturnValue()));
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void welcomescreen$cloudColor(float partialTick, CallbackInfoReturnable<Vec3> info) {
        info.setReturnValue(LocalSky.skyColor((ClientLevel) (Object) this, info.getReturnValue()));
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void welcomescreen$stars(float partialTick, CallbackInfoReturnable<Float> info) {
        info.setReturnValue(LocalSky.stars((ClientLevel) (Object) this, info.getReturnValueF()));
    }
}
