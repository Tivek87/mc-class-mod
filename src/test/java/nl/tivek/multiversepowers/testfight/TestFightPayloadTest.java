package nl.tivek.multiversepowers.testfight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class TestFightPayloadTest {
    @Test
    void comesBackAsItWent() {
        for (int fighter : new int[] { -1, 0, 1, 300_000 }) {
            for (int phase : new int[] { TestFightPayload.END, TestFightPayload.START, TestFightPayload.LET_GO }) {
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
                TestFightPayload sent = new TestFightPayload(fighter, 128, phase);
                TestFightPayload.STREAM_CODEC.encode(buf, sent);
                assertEquals(sent, TestFightPayload.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes());
            }
        }
    }

    @Test
    void theRequestComesBackAsItWent() {
        for (int target : new int[] { 0, 1, 127, 128, Integer.MAX_VALUE }) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            TestFightRequest sent = new TestFightRequest(target);
            TestFightRequest.STREAM_CODEC.encode(buf, sent);
            assertEquals(sent, TestFightRequest.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());
        }
    }
}
