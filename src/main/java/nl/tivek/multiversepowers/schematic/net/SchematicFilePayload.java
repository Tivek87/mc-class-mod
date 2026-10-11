package nl.tivek.multiversepowers.schematic.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Part `part` of `parts` of a schematic file, either way: to the player to show it (VIEW) or keep it on their own
// computer (DOWNLOAD), or to the server as an import (UPLOAD, `path` the folder and file name it goes to).
public record SchematicFilePayload(int purpose, String path, int part, int parts, byte[] bytes)
        implements CustomPacketPayload {
    public static final int VIEW = 0;
    public static final int DOWNLOAD = 1;
    public static final int UPLOAD = 2;
    // A blueprint's file, to draw it (`path` its file's name in the world's blueprint folder).
    public static final int BLUEPRINT = 3;
    // A player may send at most 32 kB at once; the server may send far more.
    public static final int UP = 30000;
    public static final int DOWN = 500000;

    public static final CustomPacketPayload.Type<SchematicFilePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "schematic_file"));

    public static final StreamCodec<ByteBuf, SchematicFilePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SchematicFilePayload::purpose, ByteBufCodecs.stringUtf8(512),
            SchematicFilePayload::path, ByteBufCodecs.VAR_INT, SchematicFilePayload::part, ByteBufCodecs.VAR_INT,
            SchematicFilePayload::parts, ByteBufCodecs.byteArray(DOWN), SchematicFilePayload::bytes,
            SchematicFilePayload::new);

    @Override
    public CustomPacketPayload.Type<SchematicFilePayload> type() {
        return TYPE;
    }
}
