package nl.tivek.multiversepowers.engine.entity.impact;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near a falling creature that it caught hold of an edge: where its hands hold (the middle between
// them, on the edge), the way out of the wall (nx, nz), how many ticks it hangs (0: it lets go now) and whether it is
// a branch.
public record LedgePayload(int entity, Vec3 edge, float nx, float nz, int ticks, boolean branch)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LedgePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "ledge"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LedgePayload> STREAM_CODEC = CustomPacketPayload
            .codec(LedgePayload::write, LedgePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeDouble(this.edge.x);
        buf.writeDouble(this.edge.y);
        buf.writeDouble(this.edge.z);
        buf.writeFloat(this.nx);
        buf.writeFloat(this.nz);
        buf.writeVarInt(this.ticks);
        buf.writeBoolean(this.branch);
    }

    private static LedgePayload read(RegistryFriendlyByteBuf buf) {
        int entity = buf.readVarInt();
        Vec3 edge = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new LedgePayload(entity, edge, buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<LedgePayload> type() {
        return TYPE;
    }
}
