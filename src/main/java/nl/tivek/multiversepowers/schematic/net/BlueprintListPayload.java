package nl.tivek.multiversepowers.schematic.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.schematic.blueprint.Blueprint;

// Every blueprint set down in the world, to every player: as they join and whenever one is added or taken away.
public record BlueprintListPayload(List<Blueprint> blueprints) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BlueprintListPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "blueprint_list"));

    public static final StreamCodec<ByteBuf, BlueprintListPayload> STREAM_CODEC = StreamCodec.composite(
            Blueprint.STREAM_CODEC.apply(ByteBufCodecs.list(1024)), BlueprintListPayload::blueprints,
            BlueprintListPayload::new);

    @Override
    public CustomPacketPayload.Type<BlueprintListPayload> type() {
        return TYPE;
    }
}
