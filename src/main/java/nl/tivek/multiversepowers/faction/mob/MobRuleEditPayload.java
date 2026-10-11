package nl.tivek.multiversepowers.faction.mob;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A host's change on the Mobs page, as `MobRules.edit` takes it.
public record MobRuleEditPayload(String actor, String target, int standing) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MobRuleEditPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mob_rule_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobRuleEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), MobRuleEditPayload::actor,
            ByteBufCodecs.stringUtf8(256), MobRuleEditPayload::target,
            ByteBufCodecs.VAR_INT, MobRuleEditPayload::standing, MobRuleEditPayload::new);

    @Override
    public CustomPacketPayload.Type<MobRuleEditPayload> type() {
        return TYPE;
    }
}
