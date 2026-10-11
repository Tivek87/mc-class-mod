package nl.tivek.multiversepowers.config.transfer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Part `part` of `parts` of a zip of settings a player imports into this world.
public record SettingsImportPayload(int part, int parts, byte[] bytes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SettingsImportPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "settings_import"));

    public static final StreamCodec<ByteBuf, SettingsImportPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SettingsImportPayload::part, ByteBufCodecs.VAR_INT, SettingsImportPayload::parts,
            ByteBufCodecs.byteArray(SettingsTransfer.PART), SettingsImportPayload::bytes, SettingsImportPayload::new);

    @Override
    public CustomPacketPayload.Type<SettingsImportPayload> type() {
        return TYPE;
    }
}
