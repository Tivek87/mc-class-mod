package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * The screen of {@code /constructshockwave}: the player picked one of the landing-slam constructs, to strike in front
 * of him a second later. Only for players who may cheat.
 *
 * @param variant which construct (see {@link ConstructPayload#SLAM_FIST})
 */
public record ConstructPickPayload(int variant) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConstructPickPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "construct_pick"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConstructPickPayload> STREAM_CODEC = StreamCodec
            .composite(ByteBufCodecs.VAR_INT, ConstructPickPayload::variant, ConstructPickPayload::new);

    @Override
    public CustomPacketPayload.Type<ConstructPickPayload> type() {
        return TYPE;
    }
}
