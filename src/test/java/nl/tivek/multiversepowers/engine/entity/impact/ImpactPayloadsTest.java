package nl.tivek.multiversepowers.engine.entity.impact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ImpactPayloadsTest {
    @Test
    void anImpactComesBackAsItWent() {
        for (int id : new int[] { 0, 128, 2_000_000 }) {
            for (ImpactPayload.Reaction reaction : ImpactPayload.Reaction.values()) {
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
                ImpactPayload sent = new ImpactPayload(id, new Vec3(-29_999_981.25, 64.62, 12.5),
                        new Vec3(0.5, -0.25, 0.75), 1.375F, reaction, 26);
                ImpactPayload.STREAM_CODEC.encode(buf, sent);
                assertEquals(sent, ImpactPayload.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes());
            }
        }
    }

    @Test
    void aLedgeComesBackAsItWent() {
        for (int ticks : new int[] { 0, 35, 70_000 }) {
            for (boolean branch : new boolean[] { false, true }) {
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
                LedgePayload sent = new LedgePayload(2_000_000, new Vec3(30_000_040.0, -52.0, 0.5), -1.0F, 0.0F,
                        ticks, branch);
                LedgePayload.STREAM_CODEC.encode(buf, sent);
                assertEquals(sent, LedgePayload.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes());
            }
        }
    }
}
