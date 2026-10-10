package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A held player's game telling the server they won their escape game.
public record EscapePayload() implements CustomPacketPayload {
    public static final EscapePayload INSTANCE = new EscapePayload();
    public static final CustomPacketPayload.Type<EscapePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "escape"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EscapePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<EscapePayload> type() {
        return TYPE;
    }
}
