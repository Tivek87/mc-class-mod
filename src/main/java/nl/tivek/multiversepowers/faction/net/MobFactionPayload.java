package nl.tivek.multiversepowers.faction.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// One creature's own faction ("" for its kind's, "-" for none) and how it stands to players by itself (-1: as its
// faction does), as `MobFactions` keeps them.
public record MobFactionPayload(int entity, String faction, int stance) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MobFactionPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mob_faction"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobFactionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobFactionPayload::entity,
            ByteBufCodecs.stringUtf8(32), MobFactionPayload::faction,
            ByteBufCodecs.VAR_INT, MobFactionPayload::stance, MobFactionPayload::new);

    @Override
    public CustomPacketPayload.Type<MobFactionPayload> type() {
        return TYPE;
    }
}
