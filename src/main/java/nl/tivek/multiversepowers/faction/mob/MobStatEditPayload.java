package nl.tivek.multiversepowers.faction.mob;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A host's change to one of a creature's numbers on the Mobs page, as `MobStats.edit` takes it.
public record MobStatEditPayload(String creature, String stat, double value) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MobStatEditPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mob_stat_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobStatEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), MobStatEditPayload::creature,
            ByteBufCodecs.stringUtf8(32), MobStatEditPayload::stat,
            ByteBufCodecs.DOUBLE, MobStatEditPayload::value, MobStatEditPayload::new);

    @Override
    public CustomPacketPayload.Type<MobStatEditPayload> type() {
        return TYPE;
    }
}
