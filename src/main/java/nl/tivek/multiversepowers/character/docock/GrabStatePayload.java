package nl.tivek.multiversepowers.character.docock;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record GrabStatePayload(boolean holding, boolean blocks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GrabStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "grab_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GrabStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GrabStatePayload::holding,
            ByteBufCodecs.BOOL, GrabStatePayload::blocks,
            GrabStatePayload::new);

    @Override
    public CustomPacketPayload.Type<GrabStatePayload> type() {
        return TYPE;
    }
}
