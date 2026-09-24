package nl.tivek.multiversepowers.character.docock;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Server tells nearby clients where a tech portal, or an energy shield, of a spell is this tick and how far it is open.
 * Clients draw it as a 3D steel ring with a glowing energy field, and play its opening from
 * {@code open}: the ring assembles, the lamps light up one by one, the energy tears open.
 *
 * @param normal the side the portal faces; things come out of it this way
 * @param style  {@link #STYLE_PORTAL} or {@link #STYLE_SHIELD}
 * @param size   radius when fully open, in blocks
 * @param open   0 = shut, 1 = fully open; the same way back while closing. Below 0 = the portal is gone
 */
public record PortalPayload(int id, Vec3 center, Vec3 normal, float size, float open, int style)
        implements CustomPacketPayload {
    public static final int STYLE_PORTAL = 0;
    public static final int STYLE_SHIELD = 1;
    public static final CustomPacketPayload.Type<PortalPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "portal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortalPayload> STREAM_CODEC = CustomPacketPayload
            .codec(PortalPayload::write, PortalPayload::read);

    public static PortalPayload remove(int id) {
        return new PortalPayload(id, Vec3.ZERO, new Vec3(0, 1, 0), 0.0F, -1.0F, STYLE_PORTAL);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeDouble(this.center.x);
        buf.writeDouble(this.center.y);
        buf.writeDouble(this.center.z);
        buf.writeFloat((float) this.normal.x);
        buf.writeFloat((float) this.normal.y);
        buf.writeFloat((float) this.normal.z);
        buf.writeFloat(this.size);
        buf.writeFloat(this.open);
        buf.writeByte(this.style);
    }

    private static PortalPayload read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 normal = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        float size = buf.readFloat();
        float open = buf.readFloat();
        return new PortalPayload(id, center, normal, size, open, buf.readByte());
    }

    @Override
    public CustomPacketPayload.Type<PortalPayload> type() {
        return TYPE;
    }
}
