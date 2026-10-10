package nl.tivek.multiversepowers.character.greenlantern.fist;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near that a player throws one of the fists' blows (FistMoves), and which way he faces for a heavy
// one's giant fist (degrees).
public record FistPayload(int owner, int move, float yaw) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FistPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "fist"));
    // The move sent when the gloves are put away.
    public static final int AWAY = -1;

    public static final StreamCodec<RegistryFriendlyByteBuf, FistPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FistPayload::owner, ByteBufCodecs.VAR_INT, FistPayload::move, ByteBufCodecs.FLOAT,
            FistPayload::yaw, FistPayload::new);

    @Override
    public CustomPacketPayload.Type<FistPayload> type() {
        return TYPE;
    }
}
