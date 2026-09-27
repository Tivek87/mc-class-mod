package nl.tivek.multiversepowers.engine.client.ragdoll;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.tivek.multiversepowers.engine.physics.Blocks;

// The solid blocks around a ragdoll, as the player's own game has them (a chunk it has not got is empty air). One is
// reused for every ragdoll, so gathering makes no garbage.
final class LevelBlocks implements Blocks, Shapes.DoubleLineConsumer {
    // Past this many blocks along a side the region is cut down round its middle.
    private static final int WIDEST = 16;

    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    @Nullable
    private Level level;
    private double[] out;
    private int count;
    private int most;
    private int ox;
    private int oy;
    private int oz;

    LevelBlocks in(@Nullable Level level) {
        this.level = level;
        return this;
    }

    @Override
    public int collect(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double[] out) {
        Level level = this.level;
        if (level == null) {
            return 0;
        }
        this.out = out;
        this.count = 0;
        this.most = out.length / 6;
        int x0 = low(minX, maxX);
        int y0 = Math.max(level.getMinBuildHeight(), low(minY, maxY));
        int z0 = low(minZ, maxZ);
        int x1 = high(minX, maxX);
        int y1 = Math.min(level.getMaxBuildHeight() - 1, high(minY, maxY));
        int z1 = high(minZ, maxZ);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (this.count >= this.most) {
                        return this.count;
                    }
                    this.at.set(x, y, z);
                    BlockState state = level.getBlockState(this.at);
                    if (state.isAir()) {
                        continue;
                    }
                    this.ox = x;
                    this.oy = y;
                    this.oz = z;
                    if (state.isCollisionShapeFullBlock(level, this.at)) {
                        this.consume(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
                        continue;
                    }
                    VoxelShape shape = state.getCollisionShape(level, this.at);
                    if (!shape.isEmpty()) {
                        shape.forAllBoxes(this);
                    }
                }
            }
        }
        return this.count;
    }

    private static int low(double min, double max) {
        int from = Mth.floor(min);
        int to = Mth.floor(max);
        return to - from >= WIDEST ? Mth.floor((min + max) * 0.5) - WIDEST / 2 : from;
    }

    private static int high(double min, double max) {
        int from = Mth.floor(min);
        int to = Mth.floor(max);
        return to - from >= WIDEST ? Mth.floor((min + max) * 0.5) + WIDEST / 2 - 1 : to;
    }

    @Override
    public void consume(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (this.count >= this.most) {
            return;
        }
        int o = this.count * 6;
        this.out[o] = this.ox + minX;
        this.out[o + 1] = this.oy + minY;
        this.out[o + 2] = this.oz + minZ;
        this.out[o + 3] = this.ox + maxX;
        this.out[o + 4] = this.oy + maxY;
        this.out[o + 5] = this.oz + maxZ;
        this.count++;
    }
}
