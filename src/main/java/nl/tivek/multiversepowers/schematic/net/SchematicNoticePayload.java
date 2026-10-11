package nl.tivek.multiversepowers.schematic.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A line for the wand's holder: `key` under schematic.welcomescreen. (a notice or an error), with up to two values.
public record SchematicNoticePayload(String key, String first, String second, boolean bad)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SchematicNoticePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "schematic_notice"));

    public static final StreamCodec<ByteBuf, SchematicNoticePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), SchematicNoticePayload::key, ByteBufCodecs.stringUtf8(256),
            SchematicNoticePayload::first, ByteBufCodecs.stringUtf8(256), SchematicNoticePayload::second,
            ByteBufCodecs.BOOL, SchematicNoticePayload::bad, SchematicNoticePayload::new);

    public Component text() {
        return Component.translatable("schematic." + MultiversePowers.MODID + "." + this.key, this.first,
                this.second);
    }

    @Override
    public CustomPacketPayload.Type<SchematicNoticePayload> type() {
        return TYPE;
    }
}
