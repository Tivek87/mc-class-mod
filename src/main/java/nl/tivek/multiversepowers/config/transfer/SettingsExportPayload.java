package nl.tivek.multiversepowers.config.transfer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Part `part` of `parts` of the zip of this world's settings a player asked for as `name`.
public record SettingsExportPayload(String name, int part, int parts, byte[] bytes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SettingsExportPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "settings_export"));

    public static final StreamCodec<ByteBuf, SettingsExportPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), SettingsExportPayload::name, ByteBufCodecs.VAR_INT,
            SettingsExportPayload::part, ByteBufCodecs.VAR_INT, SettingsExportPayload::parts,
            ByteBufCodecs.byteArray(SettingsTransfer.PART), SettingsExportPayload::bytes, SettingsExportPayload::new);

    @Override
    public CustomPacketPayload.Type<SettingsExportPayload> type() {
        return TYPE;
    }
}
