package nl.tivek.multiversepowers.engine.entity;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near a creature how the blow that killed it struck: from where (null: from nowhere in
// particular), how hard it pushed the creature (blocks a tick) and where on the body it landed (null: not known).
public record DeathBlowPayload(int entity, @Nullable Vec3 from, Vec3 push, @Nullable Vec3 at)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeathBlowPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "death_blow"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeathBlowPayload> STREAM_CODEC = CustomPacketPayload
            .codec(DeathBlowPayload::write, DeathBlowPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeBoolean(this.from != null);
        if (this.from != null) {
            buf.writeDouble(this.from.x);
            buf.writeDouble(this.from.y);
            buf.writeDouble(this.from.z);
        }
        buf.writeFloat((float) this.push.x);
        buf.writeFloat((float) this.push.y);
        buf.writeFloat((float) this.push.z);
        buf.writeBoolean(this.at != null);
        if (this.at != null) {
            buf.writeDouble(this.at.x);
            buf.writeDouble(this.at.y);
            buf.writeDouble(this.at.z);
        }
    }

    private static DeathBlowPayload read(RegistryFriendlyByteBuf buf) {
        int entity = buf.readVarInt();
        Vec3 from = buf.readBoolean() ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
        Vec3 push = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        Vec3 at = buf.readBoolean() ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
        return new DeathBlowPayload(entity, from, push, at);
    }

    @Override
    public CustomPacketPayload.Type<DeathBlowPayload> type() {
        return TYPE;
    }
}
