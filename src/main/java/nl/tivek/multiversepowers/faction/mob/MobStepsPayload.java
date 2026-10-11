package nl.tivek.multiversepowers.faction.mob;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// How many changes to the Mobs page can be taken back, for its Undo button.
public record MobStepsPayload(int steps) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MobStepsPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mob_steps"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobStepsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobStepsPayload::steps, MobStepsPayload::new);

    @Override
    public CustomPacketPayload.Type<MobStepsPayload> type() {
        return TYPE;
    }
}
