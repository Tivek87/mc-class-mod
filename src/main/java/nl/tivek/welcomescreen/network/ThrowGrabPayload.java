package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Client asks the server to throw the mob its Iron Tentacle is holding (sent on left click).
 */
public record ThrowGrabPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ThrowGrabPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "throw_grab"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThrowGrabPayload> STREAM_CODEC =
            StreamCodec.unit(new ThrowGrabPayload());

    @Override
    public CustomPacketPayload.Type<ThrowGrabPayload> type() {
        return TYPE;
    }
}
