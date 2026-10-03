package nl.tivek.multiversepowers.update;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import nl.tivek.multiversepowers.MultiversePowers;

// Never sent. Its network version is the mod's own version, so NeoForge refuses a player whose mod version differs from
// the server's while it sets up the connection, before anything else, and its refusal names the server's version
// (client/VersionMismatch reads it). Optional, so a server or player from before it connects as before.
public record VersionProbe() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VersionProbe> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "version"));
    public static final StreamCodec<ByteBuf, VersionProbe> STREAM_CODEC = StreamCodec.unit(new VersionProbe());

    @Override
    public CustomPacketPayload.Type<VersionProbe> type() {
        return TYPE;
    }

    public static String version() {
        return ModList.get().getModContainerById(MultiversePowers.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("0");
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(version()).optional().configurationToClient(TYPE, STREAM_CODEC, (payload, context) -> {
        });
    }
}
