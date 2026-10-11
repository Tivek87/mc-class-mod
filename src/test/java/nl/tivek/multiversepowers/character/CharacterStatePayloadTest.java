package nl.tivek.multiversepowers.character;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

class CharacterStatePayloadTest {
    @Test
    void comesBackAsItWent() {
        for (int character : new int[] { -1, 0, 2 }) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY,
                    ConnectionType.NEOFORGE);
            CharacterStatePayload sent = new CharacterStatePayload(character, new int[] { 0, 40, 0, 7 }, 3);
            CharacterStatePayload.STREAM_CODEC.encode(buf, sent);
            CharacterStatePayload got = CharacterStatePayload.STREAM_CODEC.decode(buf);
            assertEquals(sent.character(), got.character());
            assertArrayEquals(sent.cooldowns(), got.cooldowns());
            assertEquals(sent.ultimate(), got.ultimate());
            assertEquals(0, buf.readableBytes());
        }
    }
}
