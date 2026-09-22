package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Server tells a Green Lantern and everyone who can see him how full his power ring is and what he does with
 * it right now: recharging at his lantern, flying, and which of the ring's lasting shapes he holds up. Clients
 * make the ring and the uniform glow that brightly and play the recharge and the flight.
 *
 * @param entity  entity id of the player the ring belongs to
 * @param power   what is in the ring, 0 up to 100
 * @param pending what the fist he is charging would cost if he let go now (0 when he charges nothing)
 * @param lantern how many ticks into recharging he is, or -1 when he is not recharging
 * @param flight  how many ticks ago he took off, or -1 when he is not flying
 * @param state   what the ring holds up or pours out right now: {@link #SHIELD}, {@link #DOME}, {@link #BEAM},
 *                and {@link #DESCENT} while an empty ring lets him down
 */
public record RingPayload(int entity, float power, float pending, int lantern, int flight, int state)
        implements CustomPacketPayload {
    /** The shield is up: a pane in front of him, or a ram cone while he flies. */
    public static final int SHIELD = 1;
    /** The dome is up all around him. */
    public static final int DOME = 2;
    /** The beam pours out of the ring. */
    public static final int BEAM = 4;
    /** The ring ran dry in the air: it lets him sink down gently, and does nothing else. */
    public static final int DESCENT = 8;

    public static final CustomPacketPayload.Type<RingPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "ring"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RingPayload> STREAM_CODEC = CustomPacketPayload
            .codec(RingPayload::write, RingPayload::read);

    /** True when {@code bit} is set in {@link #state}. */
    public boolean has(int bit) {
        return (this.state & bit) != 0;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeFloat(this.power);
        buf.writeFloat(this.pending);
        buf.writeVarInt(this.lantern + 1);
        buf.writeVarInt(this.flight + 1);
        buf.writeVarInt(this.state);
    }

    private static RingPayload read(RegistryFriendlyByteBuf buf) {
        return new RingPayload(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt() - 1,
                buf.readVarInt() - 1, buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<RingPayload> type() {
        return TYPE;
    }
}
