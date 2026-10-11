package nl.tivek.multiversepowers.schematic.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A wand holder saves the blocks between two corners as a schematic named `name` in the library folder `folder`;
// with BLUEPRINT it is set down as a blueprint where it stands as well, with CREATURES the creatures in it come along.
public record SchematicSavePayload(BlockPos from, BlockPos to, String folder, String name, int flags)
        implements CustomPacketPayload {
    public static final int BLUEPRINT = 1;
    public static final int CREATURES = 2;

    public static final CustomPacketPayload.Type<SchematicSavePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "schematic_save"));

    public static final StreamCodec<ByteBuf, SchematicSavePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SchematicSavePayload::from, BlockPos.STREAM_CODEC, SchematicSavePayload::to,
            ByteBufCodecs.stringUtf8(512), SchematicSavePayload::folder, ByteBufCodecs.stringUtf8(64),
            SchematicSavePayload::name, ByteBufCodecs.VAR_INT, SchematicSavePayload::flags,
            SchematicSavePayload::new);

    @Override
    public CustomPacketPayload.Type<SchematicSavePayload> type() {
        return TYPE;
    }
}
