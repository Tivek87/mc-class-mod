package nl.tivek.multiversepowers.schematic.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A wand holder asks the server to do something with the schematic files or pastes: `path` is a file or folder inside
// the library, `text` a new folder's or file's name.
public record SchematicAskPayload(int action, String path, String text) implements CustomPacketPayload {
    public static final int LIST = 0;
    public static final int FOLDER = 1;
    public static final int DELETE = 2;
    public static final int VIEW = 3;
    public static final int DOWNLOAD = 4;
    public static final int UNDO = 5;
    public static final int CANCEL = 6;
    public static final int RENAME = 7;
    public static final int COPY = 8;

    public static final CustomPacketPayload.Type<SchematicAskPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "schematic_ask"));

    public static final StreamCodec<ByteBuf, SchematicAskPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SchematicAskPayload::action, ByteBufCodecs.stringUtf8(512), SchematicAskPayload::path,
            ByteBufCodecs.stringUtf8(64), SchematicAskPayload::text, SchematicAskPayload::new);

    @Override
    public CustomPacketPayload.Type<SchematicAskPayload> type() {
        return TYPE;
    }
}
