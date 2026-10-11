package nl.tivek.multiversepowers.schematic.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// How a player's paste or undo goes: `done` of `total` blocks, `waiting` chunks the server has not loaded yet.
public record SchematicStatusPayload(int state, String name, int done, int total, int waiting)
        implements CustomPacketPayload {
    public static final int RUNNING = 0;
    public static final int WAITING = 1;
    public static final int DONE = 2;
    public static final int CANCELLED = 3;
    public static final int UNDOING = 4;
    public static final int UNDONE = 5;

    public static final CustomPacketPayload.Type<SchematicStatusPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "schematic_status"));

    public static final StreamCodec<ByteBuf, SchematicStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SchematicStatusPayload::state, ByteBufCodecs.stringUtf8(128),
            SchematicStatusPayload::name, ByteBufCodecs.VAR_INT, SchematicStatusPayload::done, ByteBufCodecs.VAR_INT,
            SchematicStatusPayload::total, ByteBufCodecs.VAR_INT, SchematicStatusPayload::waiting,
            SchematicStatusPayload::new);

    public boolean over() {
        return this.state == DONE || this.state == CANCELLED || this.state == UNDONE;
    }

    @Override
    public CustomPacketPayload.Type<SchematicStatusPayload> type() {
        return TYPE;
    }
}
