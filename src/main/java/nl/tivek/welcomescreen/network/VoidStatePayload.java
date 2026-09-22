package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Server tells the caster they entered the void for this many ticks (0 = they left it), so their
 * own client can switch to the void view and mark creatures around them.
 */
public record VoidStatePayload(int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoidStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "void_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VoidStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VoidStatePayload::ticks, VoidStatePayload::new);

    @Override
    public CustomPacketPayload.Type<VoidStatePayload> type() {
        return TYPE;
    }
}
