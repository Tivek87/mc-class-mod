package nl.tivek.multiversepowers.character;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class CharacterStatePayloadTest {
    @Test
    void comesBackAsItWent() {
        for (int limbs : new int[] { -1, 0, 0b11_10_01_00, 0xFF }) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            CharacterStatePayload sent = new CharacterStatePayload(2, new int[] { 0, 40, 0, 7 }, 3, 1, 5, limbs);
            CharacterStatePayload.STREAM_CODEC.encode(buf, sent);
            CharacterStatePayload got = CharacterStatePayload.STREAM_CODEC.decode(buf);
            assertEquals(sent.character(), got.character());
            assertArrayEquals(sent.cooldowns(), got.cooldowns());
            assertEquals(sent.ultimate(), got.ultimate());
            assertEquals(sent.stance(), got.stance());
            assertEquals(sent.marks(), got.marks());
            assertEquals(sent.limbs(), got.limbs());
            assertEquals(0, buf.readableBytes());
        }
    }
}
