package nl.tivek.multiversepowers.faction.mob;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// The settings window's Mobs page asks the server for `MobTablePayload`.
public record MobTableAskPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MobTableAskPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mob_table_ask"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobTableAskPayload> STREAM_CODEC =
            StreamCodec.unit(new MobTableAskPayload());

    @Override
    public CustomPacketPayload.Type<MobTableAskPayload> type() {
        return TYPE;
    }
}
