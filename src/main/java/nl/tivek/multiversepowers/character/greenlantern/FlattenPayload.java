package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Server tells every client that sees it that a giant hand just slapped this creature flat against the ground, so each
 * draws it squashed a moment: the very creatures the server pressed, never a guess of the client's own.
 *
 * @param entity entity id of the creature
 */
public record FlattenPayload(int entity) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FlattenPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "flatten"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FlattenPayload> STREAM_CODEC = CustomPacketPayload
            .codec(FlattenPayload::write, FlattenPayload::read);

    /** Tells everyone who sees this creature (a player too) that it was slapped flat. */
    public static void send(LivingEntity living) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(living, new FlattenPayload(living.getId()));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
    }

    private static FlattenPayload read(RegistryFriendlyByteBuf buf) {
        return new FlattenPayload(buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<FlattenPayload> type() {
        return TYPE;
    }
}
