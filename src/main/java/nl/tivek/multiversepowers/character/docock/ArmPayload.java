package nl.tivek.multiversepowers.character.docock;

import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

/**
 * Server tells nearby clients where a robot arm of a spell is this tick, as a line of
 * points from its mount to its tip. Clients draw it as a real 3D arm. No points = the arm is gone.
 *
 * @param claw        how far the claw at the tip is open, in blocks; below 0 means no claw
 * @param thickness   1 for a normal arm, higher for a bigger one
 * @param anchorId    entity the arm is mounted on (-1: none); clients glue the mount to where they
 *                    draw that entity, so the arm never lags behind its owner
 * @param anchorPos   the anchor's position the server used for these points
 * @param anchorYaw   the anchor's yaw (degrees) the server used for these points
 * @param anchorBlend 0: the whole arm moves with the anchor; 1: only the mount does, the tip stays
 *                    where the server put it
 * @param tipOffset   how much more arm lies beyond the last point (hidden in a portal), in blocks;
 *                    keeps the segments sliding along instead of jumping
 * @param heldId      entity clamped in the claw (-1: none); clients put it exactly at the tip
 * @param cut         changes when the arm jumps (through a portal): clients then do not blend
 * @param clipPoint   with {@code clipNormal}: nothing on the side the normal points to is drawn
 * @param clipNormal  zero vector = no clipping
 * @param lamps       {@link #LAMPS_NORMAL}, or {@link #LAMPS_RAGE} for red-hot lamps
 * @param spike       0 = away, 1 = the sharp point between the claws is fully out
 * @param thrust      0 = away, 1 = the thrusters are fully out and burning
 * @param carried     blocks clamped in the claw: where each one sits relative to the tip, and what it
 *                    is (a block state id); empty when the claw carries no blocks
 */
public record ArmPayload(int id, List<Vec3> points, float claw, float thickness, int anchorId,
        Vec3 anchorPos, float anchorYaw, float anchorBlend, float tipOffset, int heldId, int cut,
        Vec3 clipPoint, Vec3 clipNormal, int lamps, float spike, float thrust,
        List<ArmPayload.Carried> carried) implements CustomPacketPayload {
    public static final int MAX_POINTS = 96;
    public static final int MAX_CARRIED = 64;
    public static final int LAMPS_NORMAL = 0;
    public static final int LAMPS_RAGE = 1;

    /** One block in the claw: {@code offset} from the tip, {@code state} from Block.getId. */
    public record Carried(Vec3 offset, int state) {
    }

    public static final CustomPacketPayload.Type<ArmPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "arm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ArmPayload> STREAM_CODEC = CustomPacketPayload
            .codec(ArmPayload::write, ArmPayload::read);

    public static ArmPayload remove(int id) {
        return new ArmPayload(id, List.of(), -1.0F, 1.0F, -1, Vec3.ZERO, 0.0F, 0.0F, 0.0F, -1, 0,
                Vec3.ZERO, Vec3.ZERO, LAMPS_NORMAL, 0.0F, 0.0F, List.of());
    }

    public boolean clips() {
        return this.clipNormal.lengthSqr() > 1.0E-6;
    }

    // The first point is sent exactly; the others as small offsets from it, which is half the size.
    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeVarInt(this.points.size());
        if (this.points.isEmpty()) {
            return;
        }
        buf.writeFloat(this.claw);
        buf.writeFloat(this.thickness);
        Vec3 origin = this.points.get(0);
        buf.writeDouble(origin.x);
        buf.writeDouble(origin.y);
        buf.writeDouble(origin.z);
        for (int i = 1; i < this.points.size(); i++) {
            writeOffset(buf, this.points.get(i), origin);
        }
        buf.writeVarInt(this.anchorId + 1);
        writeOffset(buf, this.anchorPos, origin);
        buf.writeFloat(this.anchorYaw);
        buf.writeFloat(this.anchorBlend);
        buf.writeFloat(this.tipOffset);
        buf.writeVarInt(this.heldId + 1);
        buf.writeByte(this.cut);
        writeOffset(buf, this.clipPoint, origin);
        writeOffset(buf, this.clipNormal, Vec3.ZERO);
        buf.writeByte(this.lamps);
        buf.writeFloat(this.spike);
        buf.writeFloat(this.thrust);
        buf.writeVarInt(this.carried.size());
        for (Carried block : this.carried) {
            writeOffset(buf, block.offset(), Vec3.ZERO);
            buf.writeVarInt(block.state());
        }
    }

    private static ArmPayload read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int count = buf.readVarInt();
        if (count > MAX_POINTS) {
            throw new DecoderException("Arm with too many points: " + count);
        }
        if (count == 0) {
            return remove(id);
        }
        float claw = buf.readFloat();
        float thickness = buf.readFloat();
        List<Vec3> points = new ArrayList<>(count);
        Vec3 origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        points.add(origin);
        for (int i = 1; i < count; i++) {
            points.add(readOffset(buf, origin));
        }
        int anchorId = buf.readVarInt() - 1;
        Vec3 anchorPos = readOffset(buf, origin);
        float anchorYaw = buf.readFloat();
        float anchorBlend = buf.readFloat();
        float tipOffset = buf.readFloat();
        int heldId = buf.readVarInt() - 1;
        int cut = buf.readByte();
        Vec3 clipPoint = readOffset(buf, origin);
        Vec3 clipNormal = readOffset(buf, Vec3.ZERO);
        int lamps = buf.readByte();
        float spike = buf.readFloat();
        float thrust = buf.readFloat();
        int blocks = buf.readVarInt();
        if (blocks > MAX_CARRIED) {
            throw new DecoderException("Claw with too many blocks: " + blocks);
        }
        List<Carried> carried = new ArrayList<>(blocks);
        for (int i = 0; i < blocks; i++) {
            carried.add(new Carried(readOffset(buf, Vec3.ZERO), buf.readVarInt()));
        }
        return new ArmPayload(id, points, claw, thickness, anchorId, anchorPos, anchorYaw, anchorBlend,
                tipOffset, heldId, cut, clipPoint, clipNormal, lamps, spike, thrust, List.copyOf(carried));
    }

    private static void writeOffset(RegistryFriendlyByteBuf buf, Vec3 point, Vec3 origin) {
        buf.writeFloat((float) (point.x - origin.x));
        buf.writeFloat((float) (point.y - origin.y));
        buf.writeFloat((float) (point.z - origin.z));
    }

    private static Vec3 readOffset(RegistryFriendlyByteBuf buf, Vec3 origin) {
        return origin.add(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public CustomPacketPayload.Type<ArmPayload> type() {
        return TYPE;
    }
}
