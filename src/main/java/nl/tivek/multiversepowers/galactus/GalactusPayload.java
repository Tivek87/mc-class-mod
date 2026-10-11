package nl.tivek.multiversepowers.galactus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Galactus comes into the sky at the world's clock `start`, his timeline played `speed` times as fast; a start below 0
// takes him away.
public record GalactusPayload(long start, float speed) implements CustomPacketPayload {
    public static final GalactusPayload GONE = new GalactusPayload(-1L, 1.0F);
    public static final CustomPacketPayload.Type<GalactusPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "galactus"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GalactusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, GalactusPayload::start, ByteBufCodecs.FLOAT, GalactusPayload::speed,
            GalactusPayload::new);

    @Override
    public CustomPacketPayload.Type<GalactusPayload> type() {
        return TYPE;
    }
}
