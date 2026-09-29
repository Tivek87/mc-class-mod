package nl.tivek.multiversepowers.character.greenlantern.mech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

// Where the pilot walked their mech this tick: its ground spot, the way it faces and how far it has got climbing a
// ledge (0 when it does not climb).
public record MechDrivePayload(Vec3 base, float yaw, int climb) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MechDrivePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mech_drive"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MechDrivePayload> STREAM_CODEC = CustomPacketPayload
            .codec(MechDrivePayload::write, MechDrivePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(this.base.x);
        buf.writeDouble(this.base.y);
        buf.writeDouble(this.base.z);
        buf.writeFloat(this.yaw);
        buf.writeVarInt(this.climb);
    }

    private static MechDrivePayload read(RegistryFriendlyByteBuf buf) {
        return new MechDrivePayload(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readFloat(),
                buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<MechDrivePayload> type() {
        return TYPE;
    }
}
