package nl.tivek.multiversepowers.mixin;

import net.minecraft.server.MinecraftServer;
import nl.tivek.multiversepowers.config.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every world keeps its own world settings: before a server (singleplayer or dedicated) starts and the game reads its
 * settings, a world without a copy of its own gets one (see {@link WorldSettings#prepare}).
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void welcomescreen$worldSettings(CallbackInfo info) {
        WorldSettings.prepare((MinecraftServer) (Object) this);
    }
}
