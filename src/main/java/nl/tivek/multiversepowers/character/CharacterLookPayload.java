package nl.tivek.multiversepowers.character;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record CharacterLookPayload(int entity, int character, boolean animate) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CharacterLookPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "character_look"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterLookPayload> STREAM_CODEC = CustomPacketPayload
            .codec(CharacterLookPayload::write, CharacterLookPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeVarInt(this.character + 1);
        buf.writeBoolean(this.animate);
    }

    private static CharacterLookPayload read(RegistryFriendlyByteBuf buf) {
        return new CharacterLookPayload(buf.readVarInt(), buf.readVarInt() - 1, buf.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<CharacterLookPayload> type() {
        return TYPE;
    }
}
