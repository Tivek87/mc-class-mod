package nl.tivek.multiversepowers.faction.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A player's reputation with a faction just changed by `delta`; `turned`: how the faction stands to them from now
// (a `Standing`, or one past the last for as in the game), -1 where that stayed the same.
public record ReputationNotePayload(String faction, int delta, int turned) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ReputationNotePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "reputation_note"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReputationNotePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(32), ReputationNotePayload::faction,
                    ByteBufCodecs.VAR_INT, ReputationNotePayload::delta,
                    ByteBufCodecs.VAR_INT, ReputationNotePayload::turned, ReputationNotePayload::new);

    @Override
    public CustomPacketPayload.Type<ReputationNotePayload> type() {
        return TYPE;
    }
}
