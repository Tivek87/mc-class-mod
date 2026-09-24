package nl.tivek.welcomescreen.network;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
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
 *                {@link #DESCENT} while an empty ring lets him down and {@link #DIVE} while he dives for a slam
 * @param arrival how many ticks ago the ring set out to make him Green Lantern, or -1 when that is over (see
 *                {@link nl.tivek.welcomescreen.character.lantern.Arrival})
 * @param from    where the ring showed up for that, or null when it is over
 * @param charge  how many ticks ago the ring started gathering its light for the beam (he holds the attack button on
 *                its way to the beam), or -1 when it gathers nothing
 */
public record RingPayload(int entity, float power, float pending, int lantern, int flight, int state, int arrival,
        @Nullable Vec3 from, int charge) implements CustomPacketPayload {
    /** The shield is up: a pane in front of him, or a ram cone while he flies. */
    public static final int SHIELD = 1;
    /** The dome is up all around him. */
    public static final int DOME = 2;
    /** The beam pours out of the ring. */
    public static final int BEAM = 4;
    /** The ring ran dry in the air: it lets him sink down gently, and does nothing else. */
    public static final int DESCENT = 8;
    /** He is on his way down to a slam (the shockwave key in the air): diving on his ring, or dropping. */
    public static final int DIVE = 16;

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
        boolean arriving = this.arrival >= 0 && this.from != null;
        buf.writeVarInt(arriving ? this.arrival + 1 : 0);
        if (arriving) {
            buf.writeDouble(this.from.x);
            buf.writeDouble(this.from.y);
            buf.writeDouble(this.from.z);
        }
        buf.writeVarInt(this.charge + 1);
    }

    private static RingPayload read(RegistryFriendlyByteBuf buf) {
        int entity = buf.readVarInt();
        float power = buf.readFloat();
        float pending = buf.readFloat();
        int lantern = buf.readVarInt() - 1;
        int flight = buf.readVarInt() - 1;
        int state = buf.readVarInt();
        int arrival = buf.readVarInt() - 1;
        Vec3 from = arrival >= 0 ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
        int charge = buf.readVarInt() - 1;
        return new RingPayload(entity, power, pending, lantern, flight, state, arrival, from, charge);
    }

    @Override
    public CustomPacketPayload.Type<RingPayload> type() {
        return TYPE;
    }
}
