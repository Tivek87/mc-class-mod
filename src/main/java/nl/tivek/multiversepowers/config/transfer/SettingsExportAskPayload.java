package nl.tivek.multiversepowers.config.transfer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A player asks for this world's settings, to keep them as `name`.
public record SettingsExportAskPayload(String name) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SettingsExportAskPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "settings_export_ask"));

    public static final StreamCodec<ByteBuf, SettingsExportAskPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), SettingsExportAskPayload::name, SettingsExportAskPayload::new);

    @Override
    public CustomPacketPayload.Type<SettingsExportAskPayload> type() {
        return TYPE;
    }
}
