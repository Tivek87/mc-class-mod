package nl.tivek.multiversepowers.character;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record CharacterStatePayload(int character, int[] cooldowns, int ultimate, int stance, int marks)
        implements CustomPacketPayload {
    private static final int MAX_SLOTS = 32;

    public static final CustomPacketPayload.Type<CharacterStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "character_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharacterStatePayload> STREAM_CODEC = CustomPacketPayload
            .codec(CharacterStatePayload::write, CharacterStatePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.character + 1);
        buf.writeVarInt(this.cooldowns.length);
        for (int ticks : this.cooldowns) {
            buf.writeVarInt(ticks);
        }
        buf.writeVarInt(this.ultimate);
        buf.writeVarInt(this.stance);
        buf.writeVarInt(this.marks);
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
