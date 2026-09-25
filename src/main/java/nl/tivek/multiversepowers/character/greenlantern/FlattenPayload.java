package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

public record FlattenPayload(int entity) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FlattenPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "flatten"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlattenPayload> STREAM_CODEC = CustomPacketPayload
            .codec(FlattenPayload::write, FlattenPayload::read);

    public static void send(LivingEntity living) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(living, new FlattenPayload(living.getId()));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
    }

    private static FlattenPayload read(RegistryFriendlyByteBuf buf) {
        return new FlattenPayload(buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<FlattenPayload> type() {
        return TYPE;
    }
}
