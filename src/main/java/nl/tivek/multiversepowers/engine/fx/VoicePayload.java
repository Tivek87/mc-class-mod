package nl.tivek.multiversepowers.engine.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record VoicePayload(int speaker, ResourceLocation sound) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoicePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "voice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VoicePayload::speaker, ResourceLocation.STREAM_CODEC, VoicePayload::sound,
            VoicePayload::new);

    @Override
    public CustomPacketPayload.Type<VoicePayload> type() {
        return TYPE;
    }
}
