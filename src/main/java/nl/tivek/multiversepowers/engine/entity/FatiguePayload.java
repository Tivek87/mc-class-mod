package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near a creature how worn down by blows it is: the hits it has taken running (0 when rested), so
// their games show it sagging.
public record FatiguePayload(int entity, int hits) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FatiguePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "fatigue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FatiguePayload> STREAM_CODEC = CustomPacketPayload
            .codec(FatiguePayload::write, FatiguePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeVarInt(this.hits);
    }

    private static FatiguePayload read(RegistryFriendlyByteBuf buf) {
        return new FatiguePayload(buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<FatiguePayload> type() {
        return TYPE;
    }
}
