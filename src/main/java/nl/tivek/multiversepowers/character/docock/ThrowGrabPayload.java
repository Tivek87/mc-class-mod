package nl.tivek.multiversepowers.character.docock;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record ThrowGrabPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ThrowGrabPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "throw_grab"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThrowGrabPayload> STREAM_CODEC =
            StreamCodec.unit(new ThrowGrabPayload());

    @Override
    public CustomPacketPayload.Type<ThrowGrabPayload> type() {
        return TYPE;
    }
}
