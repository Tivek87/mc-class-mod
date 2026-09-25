package nl.tivek.multiversepowers.config;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record WorldSettingsPayload(String file, byte[] contents) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WorldSettingsPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "world_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WorldSettingsPayload::file, ByteBufCodecs.BYTE_ARRAY,
            WorldSettingsPayload::contents, WorldSettingsPayload::new);

    @Override
    public CustomPacketPayload.Type<WorldSettingsPayload> type() {
        return TYPE;
    }
}
