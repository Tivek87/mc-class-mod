package nl.tivek.multiversepowers.character.greenlantern.mech;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.junit.jupiter.api.Test;

class MechDrivePayloadTest {
    // What the pilot's game sends reaches the server as it was, climb and jump and all.
    @Test
    void comesBackTheSameOverTheWire() {
        for (int climb : new int[] { 0, 1, 1 << 20 | 12345 }) {
            for (boolean jump : new boolean[] { false, true }) {
                MechDrivePayload sent = new MechDrivePayload(new Vec3(12.5, -3.25, 1024.125), -137.5F, climb, jump);
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY,
                        ConnectionType.NEOFORGE);
                MechDrivePayload.STREAM_CODEC.encode(buf, sent);
                assertEquals(sent, MechDrivePayload.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes());
            }
        }
    }
}
