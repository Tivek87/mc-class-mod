package nl.tivek.multiversepowers.character.greenlantern.summon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells a player where their summons were told to stay (`stay`), or that they follow them again.
public record SummonMarkerPayload(boolean stay, double x, double y, double z) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SummonMarkerPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "summon_marker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonMarkerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SummonMarkerPayload::stay, ByteBufCodecs.DOUBLE, SummonMarkerPayload::x,
            ByteBufCodecs.DOUBLE, SummonMarkerPayload::y, ByteBufCodecs.DOUBLE, SummonMarkerPayload::z,
            SummonMarkerPayload::new);

    @Override
    public CustomPacketPayload.Type<SummonMarkerPayload> type() {
        return TYPE;
    }
}
