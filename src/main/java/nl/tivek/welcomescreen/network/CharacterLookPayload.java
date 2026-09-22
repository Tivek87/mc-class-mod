package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Server tells a client who some player is right now (themselves included), so every client can draw that
 * player the way the character looks, such as Green Lantern's uniform.
 *
 * @param entity    entity id of the player
 * @param character index in GameCharacter, or -1 when they are just themselves
 * @param animate   true when they change right now (play the change), false when this only catches a client
 *                  up (someone walks into view)
 */
public record CharacterLookPayload(int entity, int character, boolean animate) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CharacterLookPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "character_look"));

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
