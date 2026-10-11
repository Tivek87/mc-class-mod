package nl.tivek.multiversepowers.config.transfer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// How an import or export went, for the page that asked: `result` names the line (`share.<result>`), `count` how many
// settings changed.
public record SettingsResultPayload(String result, int count) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SettingsResultPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "settings_result"));

    public static final StreamCodec<ByteBuf, SettingsResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), SettingsResultPayload::result, ByteBufCodecs.VAR_INT,
            SettingsResultPayload::count, SettingsResultPayload::new);

    @Override
    public CustomPacketPayload.Type<SettingsResultPayload> type() {
        return TYPE;
    }
}
