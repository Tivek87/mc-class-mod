package nl.tivek.multiversepowers.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import nl.tivek.multiversepowers.character.greenlantern.ability.MechAssembly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow
    public ServerPlayer player;

    // A mech's pilot sits where their mech walks, through the trees it wades through.
    @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
    private void welcomescreen$pilot(LevelReader level, AABB box, double x, double y, double z,
            CallbackInfoReturnable<Boolean> result) {
        if (MechAssembly.piloting(this.player)) {
            result.setReturnValue(false);
        }
    }
}
