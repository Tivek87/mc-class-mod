package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near a creature that a throw has it down: still flying (-1), lying `ticks` more before it may move
// again, or free once more (0); their games let its limp body lie and get up in time.
public record KnockdownPayload(int entity, int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KnockdownPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "knockdown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KnockdownPayload> STREAM_CODEC = CustomPacketPayload
            .codec(KnockdownPayload::write, KnockdownPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeVarInt(this.ticks);
    }

    private static KnockdownPayload read(RegistryFriendlyByteBuf buf) {
        return new KnockdownPayload(buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<KnockdownPayload> type() {
        return TYPE;
    }
}
