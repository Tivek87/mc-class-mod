package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

// What a hand does to a creature that everyone sees on the creature itself: strung up by the evil eye, turned to a
// statue, shattered; deafened by the megaphone, bursting.
public record HandVictimPayload(int entity, int hand, int kind) implements CustomPacketPayload {
    public static final int PUPPET = 0;
    public static final int STATUE = 1;
    public static final int SHATTER = 2;
    public static final int DEAF = 3;
    public static final int POP = 4;
    // The evil eye glaring at a puppet its partner holds: marks the eye, not the hand that holds it.
    public static final int GLARE = 5;
    // Held spread-eagled by the four hands of the ring blast until it goes off.
    public static final int SPREAD = 6;

    public static final CustomPacketPayload.Type<HandVictimPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "hand_victim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HandVictimPayload> STREAM_CODEC = CustomPacketPayload
            .codec(HandVictimPayload::write, HandVictimPayload::read);

    public static void send(LivingEntity living, int hand, int kind) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(living, new HandVictimPayload(living.getId(), hand,
                kind));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeVarInt(this.hand);
        buf.writeByte(this.kind);
    }

    private static HandVictimPayload read(RegistryFriendlyByteBuf buf) {
        return new HandVictimPayload(buf.readVarInt(), buf.readVarInt(), buf.readByte());
    }

    @Override
    public CustomPacketPayload.Type<HandVictimPayload> type() {
        return TYPE;
    }
}
