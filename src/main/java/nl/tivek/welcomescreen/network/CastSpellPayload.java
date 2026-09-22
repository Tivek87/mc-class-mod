package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Client asks the server to cast a spell picked from the spell wheel.
 */
public record CastSpellPayload(String spellId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CastSpellPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "cast_spell"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CastSpellPayload::spellId, CastSpellPayload::new);

    @Override
    public CustomPacketPayload.Type<CastSpellPayload> type() {
        return TYPE;
    }
}
