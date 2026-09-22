package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Client picked a character in the wheel. An empty id means: change back to yourself.
 */
public record TransformPayload(String characterId) implements CustomPacketPayload {
    private static final int MAX_LENGTH = 64;

    public static final CustomPacketPayload.Type<TransformPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "transform"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformPayload> STREAM_CODEC = CustomPacketPayload
            .codec((payload, buf) -> buf.writeUtf(payload.characterId(), MAX_LENGTH),
                    buf -> new TransformPayload(buf.readUtf(MAX_LENGTH)));

    @Override
    public CustomPacketPayload.Type<TransformPayload> type() {
        return TYPE;
    }
}
