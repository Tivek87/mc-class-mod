package nl.tivek.welcomescreen.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Server tells a player which character they are right now, the cooldown left on every ability slot,
 * and the few things the client needs to move along with (see the fields). Sent whenever any of it
 * changes, so the HUD and the client-side movement always match the server.
 *
 * @param character  index in GameCharacter, or -1 when the player is nobody special
 * @param cooldowns  ticks left per AbilitySlot, in slot order
 * @param ultimate   ticks left of the character's big ability (Doctor Octopus: Rampage, Green Lantern: the Construct
 *                   Storm); 0 when off
 * @param legs       how many tentacles the player walks on (0 = walking on their own feet)
 * @param marked     how many creatures are marked for the next Ground Strike
 */
public record CharacterStatePayload(int character, int[] cooldowns, int ultimate, int legs, int marked)
        implements CustomPacketPayload {
    private static final int MAX_SLOTS = 32;

    public static final CustomPacketPayload.Type<CharacterStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "character_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterStatePayload> STREAM_CODEC = CustomPacketPayload
            .codec(CharacterStatePayload::write, CharacterStatePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.character + 1);
        buf.writeVarInt(this.cooldowns.length);
        for (int ticks : this.cooldowns) {
            buf.writeVarInt(ticks);
        }
        buf.writeVarInt(this.ultimate);
        buf.writeVarInt(this.legs);
        buf.writeVarInt(this.marked);
    }

    private static CharacterStatePayload read(RegistryFriendlyByteBuf buf) {
        int character = buf.readVarInt() - 1;
        int count = buf.readVarInt();
        if (count > MAX_SLOTS) {
            throw new DecoderException("Too many ability slots: " + count);
        }
        int[] cooldowns = new int[count];
        for (int i = 0; i < count; i++) {
            cooldowns[i] = buf.readVarInt();
        }
        return new CharacterStatePayload(character, cooldowns, buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<CharacterStatePayload> type() {
        return TYPE;
    }
}
