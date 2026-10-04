package nl.tivek.multiversepowers.engine.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

class DeathBlowPayloadTest {
    @Test
    void comesBackAsItWent() {
        Vec3[] froms = { null, new Vec3(-29_999_981.25, 64.62, 12.5), Vec3.ZERO };
        for (int id : new int[] { 0, 128, 2_000_000 }) {
            for (Vec3 from : froms) {
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY,
                        ConnectionType.NEOFORGE);
                DeathBlowPayload sent = new DeathBlowPayload(id, from, new Vec3(0.25, -0.5, 1.75));
                DeathBlowPayload.STREAM_CODEC.encode(buf, sent);
                assertEquals(sent, DeathBlowPayload.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes());
            }
        }
    }
}
