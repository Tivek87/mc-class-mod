package nl.tivek.multiversepowers.character.greenlantern.summon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells a player that a creature they see is a hard-light summon, and how many ticks it has stood.
public record SummonPayload(int entity, int age) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SummonPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "summon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SummonPayload::entity, ByteBufCodecs.VAR_INT, SummonPayload::age,
            SummonPayload::new);

    @Override
    public CustomPacketPayload.Type<SummonPayload> type() {
        return TYPE;
    }
}
