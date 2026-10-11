package nl.tivek.multiversepowers.faction.mob;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A button over the Mobs page: everyone hostile on or off, everything back to the game's own, or a step back.
public record MobPageActionPayload(int action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MobPageActionPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mob_page_action"));
    public static final int EVERYONE = 0;
    public static final int RESET = 1;
    public static final int UNDO = 2;

    public static final StreamCodec<RegistryFriendlyByteBuf, MobPageActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobPageActionPayload::action, MobPageActionPayload::new);

    @Override
    public CustomPacketPayload.Type<MobPageActionPayload> type() {
        return TYPE;
    }
}
