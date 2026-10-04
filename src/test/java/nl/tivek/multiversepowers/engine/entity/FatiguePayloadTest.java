package nl.tivek.multiversepowers.engine.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

class FatiguePayloadTest {
    @Test
    void comesBackAsItWent() {
        for (int id : new int[] { 0, 1, 127, 128, 300_000, Integer.MAX_VALUE }) {
            for (int hits = 0; hits <= Fatigue.HITS; hits++) {
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY,
                        ConnectionType.NEOFORGE);
                FatiguePayload sent = new FatiguePayload(id, hits);
                FatiguePayload.STREAM_CODEC.encode(buf, sent);
                assertEquals(sent, FatiguePayload.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes());
            }
        }
    }
}
