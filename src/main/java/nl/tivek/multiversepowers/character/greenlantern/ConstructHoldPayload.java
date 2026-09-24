package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * The construct wheel: Green Lantern took a construct out, or put the one he had away. The server makes the construct
 * (the sword and shield, for one) and shows it to everyone around.
 *
 * @param construct the ordinal of the construct he now holds (see
 *                  {@link nl.tivek.multiversepowers.character.greenlantern.Construct}); 0 for empty hands
 */
public record ConstructHoldPayload(int construct) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConstructHoldPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "construct_hold"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConstructHoldPayload> STREAM_CODEC = StreamCodec
            .composite(ByteBufCodecs.VAR_INT, ConstructHoldPayload::construct, ConstructHoldPayload::new);

    @Override
    public CustomPacketPayload.Type<ConstructHoldPayload> type() {
        return TYPE;
    }
}
