package nl.tivek.multiversepowers.engine.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Looking into the world only as far as it is loaded. On the server, the game's own look-ups load (or even make) a
 * chunk that is not loaded, there and then, and the whole server waits for it: a long ray, or a power still at work far
 * from its owner, can freeze everyone for a moment. What lies in chunks nobody has loaded, nobody sees; these treat it
 * as open air.
 */
public final class LoadedWorld {
    private LoadedWorld() {
    }

    /**
     * The game's own {@link Level#clip}, except that the ray passes through chunks that are not loaded as if they were
     * empty, instead of loading them.
     */
    public static BlockHitResult clip(Level level, ClipContext context) {
        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (ray, pos) -> {
            if (!level.isLoaded(pos)) {
                return null;
            }
            BlockState state = level.getBlockState(pos);
            FluidState fluid = level.getFluidState(pos);
            Vec3 from = ray.getFrom();
            Vec3 to = ray.getTo();
            VoxelShape shape = ray.getBlockShape(state, level, pos);
            BlockHitResult block = level.clipWithInteractionOverride(from, to, pos, shape, state);
            BlockHitResult water = ray.getFluidShape(fluid, level, pos).clip(from, to, pos);
            double blockDistance = block == null ? Double.MAX_VALUE : from.distanceToSqr(block.getLocation());
            double waterDistance = water == null ? Double.MAX_VALUE : from.distanceToSqr(water.getLocation());
            return blockDistance <= waterDistance ? block : water;
        }, ray -> {
            Vec3 back = ray.getFrom().subtract(ray.getTo());
            return BlockHitResult.miss(ray.getTo(), Direction.getNearest(back.x, back.y, back.z),
                    BlockPos.containing(ray.getTo()));
        });
    }
}
