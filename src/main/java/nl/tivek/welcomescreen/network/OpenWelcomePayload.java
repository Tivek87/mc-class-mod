package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Server tells the client: this player has no class yet, show the welcome screen.
 */
public record OpenWelcomePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenWelcomePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "open_welcome"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWelcomePayload> STREAM_CODEC =
            StreamCodec.unit(new OpenWelcomePayload());

    @Override
    public CustomPacketPayload.Type<OpenWelcomePayload> type() {
        return TYPE;
    }
}
