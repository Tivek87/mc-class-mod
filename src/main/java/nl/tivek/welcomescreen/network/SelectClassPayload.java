package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Client tells the server which class the player picked.
 */
public record SelectClassPayload(String classId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SelectClassPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "select_class"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectClassPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SelectClassPayload::classId, SelectClassPayload::new);

    @Override
    public CustomPacketPayload.Type<SelectClassPayload> type() {
        return TYPE;
    }
}
