package nl.tivek.multiversepowers.faction.net;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// What a player's side holds a grudge against, and until when (game time).
public record StandingsPayload(List<UUID> grudges, List<Long> until) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StandingsPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "standings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandingsPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), StandingsPayload::grudges,
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), StandingsPayload::until,
            StandingsPayload::new);

    @Override
    public CustomPacketPayload.Type<StandingsPayload> type() {
        return TYPE;
    }
}
