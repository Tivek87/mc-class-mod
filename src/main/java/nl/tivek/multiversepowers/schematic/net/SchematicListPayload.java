package nl.tivek.multiversepowers.schematic.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.schematic.SchematicLibrary;

// What one folder of the library holds, for the wand's menu; `root` is where the library lies on the server's disk.
public record SchematicListPayload(String folder, String root, List<SchematicLibrary.Entry> entries)
        implements CustomPacketPayload {
    private static final StreamCodec<ByteBuf, SchematicLibrary.Entry> ENTRY = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), SchematicLibrary.Entry::name, ByteBufCodecs.BOOL,
            SchematicLibrary.Entry::folder, ByteBufCodecs.VAR_LONG, SchematicLibrary.Entry::size,
            ByteBufCodecs.VAR_LONG, SchematicLibrary.Entry::modified, SchematicLibrary.Entry::new);

    public static final CustomPacketPayload.Type<SchematicListPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "schematic_list"));

    public static final StreamCodec<ByteBuf, SchematicListPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(512), SchematicListPayload::folder, ByteBufCodecs.stringUtf8(1024),
            SchematicListPayload::root, ENTRY.apply(ByteBufCodecs.list(500)), SchematicListPayload::entries,
            SchematicListPayload::new);

    @Override
    public CustomPacketPayload.Type<SchematicListPayload> type() {
        return TYPE;
    }
}
