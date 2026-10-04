package nl.tivek.multiversepowers.engine.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

class HeldPayloadTest {
    @Test
    void comesBackAsItWent() {
        for (int id : new int[] { 0, 1, 127, 128, 300_000, Integer.MAX_VALUE }) {
            for (boolean held : new boolean[] { true, false }) {
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY,
                        ConnectionType.NEOFORGE);
                HeldPayload sent = new HeldPayload(id, held);
                HeldPayload.STREAM_CODEC.encode(buf, sent);
                assertEquals(sent, HeldPayload.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes());
            }
        }
    }
}
