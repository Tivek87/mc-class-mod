package nl.tivek.multiversepowers.faction.net;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// What the server keeps of one player and the factions, for their own game: what they earned with each (counted from
// where it starts), the standings pinned for them (by `Standing` order), and how many changes to the factions can be
// taken back.
public record FactionStatePayload(Map<String, Integer> earned, Map<String, Integer> pins, int steps)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FactionStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "faction_state"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<String, Integer>> NUMBERS = ByteBufCodecs.map(
            HashMap::new, ByteBufCodecs.stringUtf8(32), ByteBufCodecs.VAR_INT, 512);

    public static final StreamCodec<RegistryFriendlyByteBuf, FactionStatePayload> STREAM_CODEC = StreamCodec.composite(
            NUMBERS, FactionStatePayload::earned,
            NUMBERS, FactionStatePayload::pins,
            ByteBufCodecs.VAR_INT, FactionStatePayload::steps, FactionStatePayload::new);

    @Override
    public CustomPacketPayload.Type<FactionStatePayload> type() {
        return TYPE;
    }
}
