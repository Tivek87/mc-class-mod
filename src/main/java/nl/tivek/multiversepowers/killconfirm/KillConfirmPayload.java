package nl.tivek.multiversepowers.killconfirm;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells a player that something they struck has died of it.
public record KillConfirmPayload() implements CustomPacketPayload {
    public static final KillConfirmPayload INSTANCE = new KillConfirmPayload();
    public static final CustomPacketPayload.Type<KillConfirmPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "kill_confirm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KillConfirmPayload> STREAM_CODEC = StreamCodec.unit(
            INSTANCE);

    @Override
    public CustomPacketPayload.Type<KillConfirmPayload> type() {
        return TYPE;
    }
}
