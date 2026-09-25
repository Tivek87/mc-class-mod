package nl.tivek.multiversepowers.spell;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record VoidStatePayload(int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoidStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "void_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VoidStatePayload::ticks, VoidStatePayload::new);

    @Override
    public CustomPacketPayload.Type<VoidStatePayload> type() {
        return TYPE;
    }
}
