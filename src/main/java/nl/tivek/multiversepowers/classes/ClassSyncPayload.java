package nl.tivek.multiversepowers.classes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record ClassSyncPayload(String classId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClassSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "class_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClassSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClassSyncPayload::classId, ClassSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<ClassSyncPayload> type() {
        return TYPE;
    }
}
