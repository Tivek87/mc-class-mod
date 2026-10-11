package nl.tivek.multiversepowers.schematic.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A player asks about blueprint `id`: its file, to draw it (anyone), or, with the wand, to take it away, build it or
// name it `text`.
public record BlueprintAskPayload(int action, int id, String text) implements CustomPacketPayload {
    public static final int FILE = 0;
    public static final int REMOVE = 1;
    public static final int BUILD = 2;
    public static final int RENAME = 3;
    public static final int NAME = 64;

    public static final CustomPacketPayload.Type<BlueprintAskPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "blueprint_ask"));

    public static final StreamCodec<ByteBuf, BlueprintAskPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BlueprintAskPayload::action, ByteBufCodecs.VAR_INT, BlueprintAskPayload::id,
            ByteBufCodecs.stringUtf8(NAME), BlueprintAskPayload::text, BlueprintAskPayload::new);

    public BlueprintAskPayload(int action, int id) {
        this(action, id, "");
    }

    @Override
    public CustomPacketPayload.Type<BlueprintAskPayload> type() {
        return TYPE;
    }
}
