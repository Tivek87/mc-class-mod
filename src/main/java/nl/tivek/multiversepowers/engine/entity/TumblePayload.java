package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near a creature that a blow set it tumbling: its limp body spins about (x, y, z), radians a
// second, as it flies (Tumbles).
public record TumblePayload(int entity, float x, float y, float z) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TumblePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "tumble"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TumblePayload> STREAM_CODEC = CustomPacketPayload
            .codec(TumblePayload::write, TumblePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeFloat(this.x);
        buf.writeFloat(this.y);
        buf.writeFloat(this.z);
    }

    private static TumblePayload read(RegistryFriendlyByteBuf buf) {
        return new TumblePayload(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public CustomPacketPayload.Type<TumblePayload> type() {
        return TYPE;
    }
}
