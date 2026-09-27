package nl.tivek.multiversepowers.engine.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class DeathStylePayloadTest {
    @Test
    void comesBackAsItWent() {
        for (int id : new int[] { 0, 5, 128, 2_000_000 }) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            DeathStylePayload sent = new DeathStylePayload(id, DeathStyles.Style.ASH.ordinal());
            DeathStylePayload.STREAM_CODEC.encode(buf, sent);
            assertEquals(sent, DeathStylePayload.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());
        }
    }
}
