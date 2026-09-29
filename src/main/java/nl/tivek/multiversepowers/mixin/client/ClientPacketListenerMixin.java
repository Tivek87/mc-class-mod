package nl.tivek.multiversepowers.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Every explosion a player hears of (TNT, a creeper, a wind charge) throws the limp bodies near it.
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleExplosion", at = @At("TAIL"))
    private void welcomescreen$blast(ClientboundExplodePacket packet, CallbackInfo info) {
        Ragdolls.blast(new Vec3(packet.getX(), packet.getY(), packet.getZ()), packet.getPower());
    }
}
