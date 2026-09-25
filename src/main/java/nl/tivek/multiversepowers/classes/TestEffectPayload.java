package nl.tivek.multiversepowers.classes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record TestEffectPayload(String kind, String id, boolean showTitle) implements CustomPacketPayload {
    public static final String CEREMONY = "ceremony";
    public static final String DEATH = "death";
    public static final String LEVEL_UP = "level_up";

    public static final CustomPacketPayload.Type<TestEffectPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "test_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TestEffectPayload> STREAM_CODEC = StreamCodec.composite(
            // The kind and the id are single short words: a much longer text from a client is refused before it is read.
            ByteBufCodecs.stringUtf8(64), TestEffectPayload::kind,
            ByteBufCodecs.stringUtf8(64), TestEffectPayload::id,
            ByteBufCodecs.BOOL, TestEffectPayload::showTitle,
            TestEffectPayload::new);

    @Override
    public CustomPacketPayload.Type<TestEffectPayload> type() {
        return TYPE;
    }
}
