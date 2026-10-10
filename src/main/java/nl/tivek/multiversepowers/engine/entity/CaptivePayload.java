package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells a player they are held and which escape game to play (0: free again).
public record CaptivePayload(int game) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CaptivePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "captive"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CaptivePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CaptivePayload::game, CaptivePayload::new);

    @Override
    public CustomPacketPayload.Type<CaptivePayload> type() {
        return TYPE;
    }
}
