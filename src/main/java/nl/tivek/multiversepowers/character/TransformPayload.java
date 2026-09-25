package nl.tivek.multiversepowers.character;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record TransformPayload(String characterId) implements CustomPacketPayload {
    private static final int MAX_LENGTH = 64;

    public static final CustomPacketPayload.Type<TransformPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "transform"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformPayload> STREAM_CODEC = CustomPacketPayload
            .codec((payload, buf) -> buf.writeUtf(payload.characterId(), MAX_LENGTH),
                    buf -> new TransformPayload(buf.readUtf(MAX_LENGTH)));

    @Override
    public CustomPacketPayload.Type<TransformPayload> type() {
        return TYPE;
    }
}
