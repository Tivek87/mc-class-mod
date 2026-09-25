package nl.tivek.multiversepowers.spell;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record CastSpellPayload(String spellId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CastSpellPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "cast_spell"));

    // Spell id is one short word; a longer string is refused before it's read.
    public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), CastSpellPayload::spellId, CastSpellPayload::new);

    @Override
    public CustomPacketPayload.Type<CastSpellPayload> type() {
        return TYPE;
    }
}
