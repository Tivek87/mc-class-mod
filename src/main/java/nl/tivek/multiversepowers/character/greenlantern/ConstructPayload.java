package nl.tivek.multiversepowers.character.greenlantern;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

public record ConstructPayload(int id, int owner, Vec3 center, Vec3 facing, float size, float solid,
        float charge, boolean held, int shape, int variant, int age, @Nullable ConstructPath path)
        implements CustomPacketPayload {
    public static final int FIST = 0;
    // Reaches past the ~128 update range so nobody nearby misses the removal.
    private static final double REMOVE_RANGE = 176.0;
    public static final int BOLT = 1;
    public static final int SHIELD = 2;
    public static final int BEAM = 3;
    public static final int DOME = 4;
    public static final int RAM = 5;
    public static final int SLAM = 6;
    public static final int SCAN = 7;
    public static final int SCAN_HOSTILE = 1;
    public static final int PLANE = 9;
    public static final int MISSILE = 10;
    public static final int BULLET = 11;
    public static final int BUBBLE = 12;
    public static final int SWORD = 13;
    public static final int BLAST = 14;
    public static final int POUND = 15;
    public static final int HAND = 16;
    public static final int FLAME = 17;
    public static final int FLAME_WALL = 18;
    public static final int BURN = 19;
    public static final int SLAM_FIST = 0;
    public static final int SLAM_HANDS = 1;
    public static final int SLAM_FISTS = 2;
    public static final int SLAM_HAMMER = 3;
    public static final int SLAM_EMBLEM = 4;
    public static final int SLAM_ANVIL = 5;
    public static final int SLAM_CYMBALS = 6;
    public static final int SLAM_UPPERCUT = 7;
    public static final int SLAM_SPIKES = 8;
    public static final int SLAM_BOOT = 9;
    public static final int SLAM_WEIGHT = 10;
    public static final int SLAM_SWORD = 11;
    public static final int SLAM_ROCKETS = 12;
    public static final int SLAM_SWATTER = 13;
    public static final int SLAM_LANTERN = 14;
    public static final int SLAM_SAFE = 15;
    public static final int SLAM_ANCHOR = 16;
    public static final int SLAM_MACE = 17;
    public static final int SLAM_BARBELL = 18;
    public static final int SLAM_BELL = 19;
    public static final int SLAM_METEOR = 20;
    public static final int SLAM_PALM = 21;
    public static final int SLAM_GAVEL = 22;
    public static final int SLAM_PICKAXE = 23;
    public static final int SLAM_TRAP = 24;
    public static final int SLAM_BOOK = 25;
    public static final int SLAM_DRUM = 26;
    public static final int SLAM_PILLAR = 27;
    public static final int SLAM_TNT = 28;
    public static final int SLAM_PIANO = 29;
    public static final int SLAM_BRICK = 30;
    public static final int SLAM_STAMP = 31;
    public static final int SLAM_KINDS = 32;

    public static final CustomPacketPayload.Type<ConstructPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "construct"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConstructPayload> STREAM_CODEC = CustomPacketPayload
            .codec(ConstructPayload::write, ConstructPayload::read);

    public ConstructPayload(int id, int owner, Vec3 center, Vec3 facing, float size, float solid, float charge,
            boolean held, int shape) {
        this(id, owner, center, facing, size, solid, charge, held, shape, 0, 0, null);
    }

    public static ConstructPayload remove(int id) {
        return new ConstructPayload(id, -1, Vec3.ZERO, new Vec3(0, 0, 1), 0.0F, -1.0F, 0.0F, false, FIST);
    }

    public static void sendRemove(ServerLevel level, int id, Vec3 near) {
        PacketDistributor.sendToPlayersNear(level, null, near.x, near.y, near.z, REMOVE_RANGE, remove(id));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.id);
        buf.writeVarInt(this.owner + 1);
        buf.writeDouble(this.center.x);
        buf.writeDouble(this.center.y);
        buf.writeDouble(this.center.z);
        buf.writeFloat((float) this.facing.x);
        buf.writeFloat((float) this.facing.y);
        buf.writeFloat((float) this.facing.z);
        buf.writeFloat(this.size);
        buf.writeFloat(this.solid);
        buf.writeFloat(this.charge);
        buf.writeBoolean(this.held);
        buf.writeByte(this.shape);
        buf.writeByte(this.variant);
        buf.writeVarInt(this.age);
        buf.writeBoolean(this.path != null);
        if (this.path != null) {
            this.path.write(buf);
        }
    }

    private static ConstructPayload read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int owner = buf.readVarInt() - 1;
        Vec3 center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 facing = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        float size = buf.readFloat();
        float solid = buf.readFloat();
        float charge = buf.readFloat();
        boolean held = buf.readBoolean();
        int shape = buf.readByte();
        int variant = buf.readByte();
        int age = buf.readVarInt();
        ConstructPath path = buf.readBoolean() ? ConstructPath.read(buf) : null;
        return new ConstructPayload(id, owner, center, facing, size, solid, charge, held, shape, variant, age, path);
    }

    @Override
    public CustomPacketPayload.Type<ConstructPayload> type() {
        return TYPE;
    }
}
