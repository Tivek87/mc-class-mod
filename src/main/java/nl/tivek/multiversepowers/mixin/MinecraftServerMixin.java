package nl.tivek.multiversepowers.mixin;

import net.minecraft.server.MinecraftServer;
import nl.tivek.multiversepowers.config.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void welcomescreen$worldSettings(CallbackInfo info) {
        WorldSettings.prepare((MinecraftServer) (Object) this);
    }
}
