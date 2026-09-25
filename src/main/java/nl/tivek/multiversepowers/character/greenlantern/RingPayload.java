package nl.tivek.multiversepowers.character.greenlantern;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

public record RingPayload(int entity, float power, float pending, int lantern, int flight, int state, int arrival,
        @Nullable Vec3 from, int charge) implements CustomPacketPayload {
    public static final int SHIELD = 1;
    public static final int DOME = 2;
    public static final int BEAM = 4;
    public static final int DESCENT = 8;
    public static final int DIVE = 16;

    public static final CustomPacketPayload.Type<RingPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "ring"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RingPayload> STREAM_CODEC = CustomPacketPayload
            .codec(RingPayload::write, RingPayload::read);

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
