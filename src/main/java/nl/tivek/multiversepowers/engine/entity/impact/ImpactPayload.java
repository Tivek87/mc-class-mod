package nl.tivek.multiversepowers.engine.entity.impact;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near a creature how it took a blow it lived through: where on it the blow landed, the way it went,
// how hard it was for the creature (Staggers), how it took it and for how many ticks it reels.
public record ImpactPayload(int entity, Vec3 at, Vec3 way, float strength, Reaction reaction, int ticks)
        implements CustomPacketPayload {
    public enum Reaction {
        // It jerks where it was struck and goes on.
        FLINCH,
        // It reels back, stepping to keep its feet, a hand to where it was struck.
        STAGGER,
        // Its legs swept from under it, it falls on its face.
        TRIP,
        // Knocked off its feet the way the blow went.
        FALL,
        // Limp already (thrown, held): its body takes the blow where it struck.
        LIMP
    }

    private static final Reaction[] REACTIONS = Reaction.values();

    public static final CustomPacketPayload.Type<ImpactPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "impact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ImpactPayload> STREAM_CODEC = CustomPacketPayload
            .codec(ImpactPayload::write, ImpactPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeDouble(this.at.x);
        buf.writeDouble(this.at.y);
        buf.writeDouble(this.at.z);
        buf.writeFloat((float) this.way.x);
        buf.writeFloat((float) this.way.y);
        buf.writeFloat((float) this.way.z);
        buf.writeFloat(this.strength);
        buf.writeVarInt(this.reaction.ordinal());
        buf.writeVarInt(this.ticks);
    }

    private static ImpactPayload read(RegistryFriendlyByteBuf buf) {
        int entity = buf.readVarInt();
        Vec3 at = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 way = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        float strength = buf.readFloat();
        int reaction = buf.readVarInt();
        return new ImpactPayload(entity, at, way, strength,
                REACTIONS[Math.max(0, Math.min(REACTIONS.length - 1, reaction))], buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<ImpactPayload> type() {
        return TYPE;
    }
}
