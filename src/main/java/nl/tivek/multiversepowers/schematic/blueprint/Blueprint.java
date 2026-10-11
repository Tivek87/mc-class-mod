package nl.tivek.multiversepowers.schematic.blueprint;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import nl.tivek.multiversepowers.schematic.SchematicPlacement;

// A schematic set down in the world as a plan to build by hand: where it stands (its world, its box's lowest corner,
// turned and mirrored), how big it is, who set it down, and the copy of its file the world keeps (`file`).
public record Blueprint(int id, String name, String owner, ResourceLocation level, BlockPos origin, int rotation,
        int mirror, int sizeX, int sizeY, int sizeZ, String file) {
    public static final StreamCodec<ByteBuf, Blueprint> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public Blueprint decode(ByteBuf buffer) {
            return new Blueprint(ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.stringUtf8(256).decode(buffer),
                    ByteBufCodecs.stringUtf8(64).decode(buffer), ResourceLocation.STREAM_CODEC.decode(buffer),
                    BlockPos.STREAM_CODEC.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.stringUtf8(64).decode(buffer));
        }

        @Override
        public void encode(ByteBuf buffer, Blueprint blueprint) {
            ByteBufCodecs.VAR_INT.encode(buffer, blueprint.id());
            ByteBufCodecs.stringUtf8(256).encode(buffer, blueprint.name());
            ByteBufCodecs.stringUtf8(64).encode(buffer, blueprint.owner());
            ResourceLocation.STREAM_CODEC.encode(buffer, blueprint.level());
            BlockPos.STREAM_CODEC.encode(buffer, blueprint.origin());
            ByteBufCodecs.VAR_INT.encode(buffer, blueprint.rotation());
            ByteBufCodecs.VAR_INT.encode(buffer, blueprint.mirror());
            ByteBufCodecs.VAR_INT.encode(buffer, blueprint.sizeX());
            ByteBufCodecs.VAR_INT.encode(buffer, blueprint.sizeY());
            ByteBufCodecs.VAR_INT.encode(buffer, blueprint.sizeZ());
            ByteBufCodecs.stringUtf8(64).encode(buffer, blueprint.file());
        }
    };

    public SchematicPlacement placement() {
        return new SchematicPlacement(this.origin, Rotation.values()[Math.floorMod(this.rotation, 4)],
                Mirror.values()[Math.floorMod(this.mirror, 3)]);
    }

    // Its box across the ground, turned.
    public int wideX() {
        return this.rotation % 2 == 1 ? this.sizeZ : this.sizeX;
    }

    public int wideZ() {
        return this.rotation % 2 == 1 ? this.sizeX : this.sizeZ;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", this.id);
        tag.putString("name", this.name);
        tag.putString("owner", this.owner);
        tag.putString("level", this.level.toString());
        tag.put("origin", NbtUtils.writeBlockPos(this.origin));
        tag.putInt("rotation", this.rotation);
        tag.putInt("mirror", this.mirror);
        tag.putIntArray("size", new int[] { this.sizeX, this.sizeY, this.sizeZ });
        tag.putString("file", this.file);
        return tag;
    }

    static Blueprint load(CompoundTag tag) {
        int[] size = tag.getIntArray("size");
        ResourceLocation level = ResourceLocation.tryParse(tag.getString("level"));
        return new Blueprint(tag.getInt("id"), tag.getString("name"), tag.getString("owner"), level == null
                ? ResourceLocation.withDefaultNamespace("overworld") : level, NbtUtils.readBlockPos(tag, "origin")
                        .orElse(BlockPos.ZERO), tag.getInt("rotation"), tag.getInt("mirror"), size.length == 3
                                ? size[0] : 1, size.length == 3 ? size[1] : 1, size.length == 3 ? size[2] : 1,
                tag.getString("file"));
    }
}
