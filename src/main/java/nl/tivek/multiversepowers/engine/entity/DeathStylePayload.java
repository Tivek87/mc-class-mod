package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near a creature how a power killed it (a DeathStyles.Style), as it dies.
public record DeathStylePayload(int entity, int style) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeathStylePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "death_style"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeathStylePayload> STREAM_CODEC = CustomPacketPayload
            .codec(DeathStylePayload::write, DeathStylePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeVarInt(this.style);
    }

    private static DeathStylePayload read(RegistryFriendlyByteBuf buf) {
        return new DeathStylePayload(buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<DeathStylePayload> type() {
        return TYPE;
    }
}
