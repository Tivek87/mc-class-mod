package nl.tivek.multiversepowers.schematic.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Place the schematic at `path` where its hologram stands: `origin` its box's lowest corner, `rotation` and `mirror`
// the ordinals of the game's own, `mode` what it may replace and `flags` what comes along (SchematicPaste).
public record SchematicPlacePayload(String path, BlockPos origin, int rotation, int mirror, int mode, int flags)
        implements CustomPacketPayload {
    // In `flags`: set it down as a blueprint rather than build it.
    public static final int BLUEPRINT = 1 << 8;

    public static final CustomPacketPayload.Type<SchematicPlacePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "schematic_place"));

    public static final StreamCodec<ByteBuf, SchematicPlacePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(512), SchematicPlacePayload::path, BlockPos.STREAM_CODEC,
            SchematicPlacePayload::origin, ByteBufCodecs.VAR_INT, SchematicPlacePayload::rotation,
            ByteBufCodecs.VAR_INT, SchematicPlacePayload::mirror, ByteBufCodecs.VAR_INT, SchematicPlacePayload::mode,
            ByteBufCodecs.VAR_INT, SchematicPlacePayload::flags, SchematicPlacePayload::new);

    @Override
    public CustomPacketPayload.Type<SchematicPlacePayload> type() {
        return TYPE;
    }
}
