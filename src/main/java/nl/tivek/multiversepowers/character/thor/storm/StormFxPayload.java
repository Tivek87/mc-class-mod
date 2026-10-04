package nl.tivek.multiversepowers.character.thor.storm;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

// A bolt out of Thor's storm (from the cloud to where it strikes), a small one round his rising lightning bomb, or the
// bomb bursting (at `from`, `size` blocks round): every game near draws it and hears its thunder.
public record StormFxPayload(int kind, Vec3 from, Vec3 to, int seed, float size) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StormFxPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "storm_fx"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StormFxPayload> STREAM_CODEC = CustomPacketPayload
            .codec(StormFxPayload::write, StormFxPayload::read);

    public static final int BOLT = 0;
    public static final int SPARK = 1;
    public static final int BLAST = 2;
    // Thunder carries far: everyone this near hears it, later the further off they are.
    private static final double RANGE = 192.0;

    static void send(ServerLevel level, int kind, Vec3 from, Vec3 to, float size) {
        PacketDistributor.sendToPlayersNear(level, null, to.x, to.y, to.z, RANGE,
                new StormFxPayload(kind, from, to, level.getRandom().nextInt(), size));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.kind);
        buf.writeVec3(this.from);
        buf.writeVec3(this.to);
        buf.writeInt(this.seed);
        buf.writeFloat(this.size);
    }

    private static StormFxPayload read(RegistryFriendlyByteBuf buf) {
        return new StormFxPayload(buf.readVarInt(), buf.readVec3(), buf.readVec3(), buf.readInt(), buf.readFloat());
    }

    @Override
    public CustomPacketPayload.Type<StormFxPayload> type() {
        return TYPE;
    }
}
