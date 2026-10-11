package nl.tivek.multiversepowers.schematic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

// Where and how a schematic goes into the world: the lowest corner of its box as turned, and the turn and mirror the
// hologram showed. The mirror comes first, then the turn, as the game's own structures do, so block states turn along.
public record SchematicPlacement(BlockPos origin, Rotation rotation, Mirror mirror) {
    public boolean quarter() {
        return this.rotation == Rotation.CLOCKWISE_90 || this.rotation == Rotation.COUNTERCLOCKWISE_90;
    }

    public int sizeX(Schematic schematic) {
        return this.quarter() ? schematic.sizeZ : schematic.sizeX;
    }

    public int sizeZ(Schematic schematic) {
        return this.quarter() ? schematic.sizeX : schematic.sizeZ;
    }

    // The schematic's cell at (u, w) across the turned box, as its x and z in `out`.
    public void source(Schematic schematic, int u, int w, int[] out) {
        int sx = schematic.sizeX;
        int sz = schematic.sizeZ;
        int x;
        int z;
        switch (this.rotation) {
            case CLOCKWISE_90 -> {
                x = w;
                z = sz - 1 - u;
            }
            case CLOCKWISE_180 -> {
                x = sx - 1 - u;
                z = sz - 1 - w;
            }
            case COUNTERCLOCKWISE_90 -> {
                x = sx - 1 - w;
                z = u;
            }
            default -> {
                x = u;
                z = w;
            }
        }
        if (this.mirror == Mirror.LEFT_RIGHT) {
            z = sz - 1 - z;
        } else if (this.mirror == Mirror.FRONT_BACK) {
            x = sx - 1 - x;
        }
        out[0] = x;
        out[1] = z;
    }

    // A point in the schematic (in blocks from its corner) where it lands in the world.
    public Vec3 world(Schematic schematic, Vec3 local) {
        double x = local.x;
        double z = local.z;
        if (this.mirror == Mirror.LEFT_RIGHT) {
            z = schematic.sizeZ - z;
        } else if (this.mirror == Mirror.FRONT_BACK) {
            x = schematic.sizeX - x;
        }
        double u;
        double w;
        switch (this.rotation) {
            case CLOCKWISE_90 -> {
                u = schematic.sizeZ - z;
                w = x;
            }
            case CLOCKWISE_180 -> {
                u = schematic.sizeX - x;
                w = schematic.sizeZ - z;
            }
            case COUNTERCLOCKWISE_90 -> {
                u = z;
                w = schematic.sizeX - x;
            }
            default -> {
                u = x;
                w = z;
            }
        }
        return new Vec3(this.origin.getX() + u, this.origin.getY() + local.y, this.origin.getZ() + w);
    }
}
