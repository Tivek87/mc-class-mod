package nl.tivek.multiversepowers.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// The server telling a player who sneak-clicked a stage light to open its settings.
public record LampOpenPayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LampOpenPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "lamp_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LampOpenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LampOpenPayload::pos, LampOpenPayload::new);

    @Override
    public CustomPacketPayload.Type<LampOpenPayload> type() {
        return TYPE;
    }
}
