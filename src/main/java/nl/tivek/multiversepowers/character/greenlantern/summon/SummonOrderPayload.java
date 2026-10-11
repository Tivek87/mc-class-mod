package nl.tivek.multiversepowers.character.greenlantern.summon;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A player's order to their summons: `entity` -1 switches them between staying and following, else that summon of
// theirs breaks up.
public record SummonOrderPayload(int entity) implements CustomPacketPayload {
    public static final int SWITCH = -1;
    public static final CustomPacketPayload.Type<SummonOrderPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "summon_order"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonOrderPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SummonOrderPayload::entity, SummonOrderPayload::new);

    @Override
    public CustomPacketPayload.Type<SummonOrderPayload> type() {
        return TYPE;
    }
}
