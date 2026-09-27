package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near a creature that a power has picked it up or let it go, so their games let it hang limp.
public record HeldPayload(int entity, boolean held) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HeldPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "held"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeldPayload> STREAM_CODEC = CustomPacketPayload
            .codec(HeldPayload::write, HeldPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeBoolean(this.held);
    }

    private static HeldPayload read(RegistryFriendlyByteBuf buf) {
        return new HeldPayload(buf.readVarInt(), buf.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<HeldPayload> type() {
        return TYPE;
    }
}
