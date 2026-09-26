package nl.tivek.multiversepowers.spell;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

public record ClapPayload(int entity) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClapPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "clap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClapPayload> STREAM_CODEC = CustomPacketPayload
            .codec(ClapPayload::write, ClapPayload::read);

    // Ticks after the cast when the hands meet; the server's boom and the client's arms both use it.
    public static final int HANDS_MEET = 6;

    static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ClapPayload(player.getId()));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
    }

    private static ClapPayload read(RegistryFriendlyByteBuf buf) {
        return new ClapPayload(buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<ClapPayload> type() {
        return TYPE;
    }
}
