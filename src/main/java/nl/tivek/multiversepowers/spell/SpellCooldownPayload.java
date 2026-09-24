package nl.tivek.multiversepowers.spell;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Server tells the client how many ticks a spell is still on cooldown, so the wheel can show it.
 */
public record SpellCooldownPayload(String spellId, int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SpellCooldownPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "spell_cooldown"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpellCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SpellCooldownPayload::spellId,
            ByteBufCodecs.VAR_INT, SpellCooldownPayload::ticks,
            SpellCooldownPayload::new);

    @Override
    public CustomPacketPayload.Type<SpellCooldownPayload> type() {
        return TYPE;
    }
}
