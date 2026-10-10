package nl.tivek.multiversepowers.killconfirm;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells a player that a blow of theirs landed, or that what they struck has died of it (`kill`).
public record KillConfirmPayload(boolean kill) implements CustomPacketPayload {
    public static final KillConfirmPayload KILL = new KillConfirmPayload(true);
    public static final KillConfirmPayload HIT = new KillConfirmPayload(false);
    public static final CustomPacketPayload.Type<KillConfirmPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "kill_confirm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KillConfirmPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, KillConfirmPayload::kill, KillConfirmPayload::new);

    @Override
    public CustomPacketPayload.Type<KillConfirmPayload> type() {
        return TYPE;
    }
}
