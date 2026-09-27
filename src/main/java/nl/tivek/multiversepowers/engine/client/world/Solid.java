package nl.tivek.multiversepowers.engine.client.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

// Whether a point lies inside a solid block, as the player's own game has the world (a chunk it has not got is air).
public final class Solid {
    private static final BlockPos.MutableBlockPos AT = new BlockPos.MutableBlockPos();

    private Solid() {
    }

    public static boolean at(Level level, double x, double y, double z) {
        return inside(level, x, y, z, false);
    }

    // As at, but foliage gives way: leaves do not count.
    public static boolean firm(Level level, double x, double y, double z) {
        return inside(level, x, y, z, true);
    }

    private static boolean inside(Level level, double x, double y, double z, boolean firm) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        AT.set(bx, by, bz);
        BlockState state = level.getBlockState(AT);
        if (state.isAir() || firm && state.is(BlockTags.LEAVES)) {
            return false;
        }
        if (state.isCollisionShapeFullBlock(level, AT)) {
            return true;
        }
        VoxelShape shape = state.getCollisionShape(level, AT);
        if (shape.isEmpty()) {
            return false;
        }
        double top = shape.max(Direction.Axis.Y, z - bz, x - bx);
        double bottom = shape.min(Direction.Axis.Y, z - bz, x - bx);
        return y - by < top && y - by >= bottom;
    }
}
