package nl.tivek.multiversepowers.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import nl.tivek.multiversepowers.MultiversePowers;

// A player's change to a stage light (anyone may make one), with `toChannel` passed on to its whole channel.
public record LampEditPayload(BlockPos pos, CompoundTag settings, boolean toChannel) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LampEditPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "lamp_edit"));
    private static final double REACH = 64.0;

    public static final StreamCodec<RegistryFriendlyByteBuf, LampEditPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LampEditPayload::pos, ByteBufCodecs.COMPOUND_TAG, LampEditPayload::settings,
            ByteBufCodecs.BOOL, LampEditPayload::toChannel, LampEditPayload::new);

    @Override
    public CustomPacketPayload.Type<LampEditPayload> type() {
        return TYPE;
    }

    public void apply(ServerPlayer player) {
        if (player.isSpectator() || !player.level().isLoaded(this.pos)
                || player.distanceToSqr(this.pos.getCenter()) > REACH * REACH) {
            return;
        }
        if (player.level().getBlockEntity(this.pos) instanceof StageLightBlockEntity lamp) {
            lamp.edit(this.settings, this.toChannel);
        }
    }
}
