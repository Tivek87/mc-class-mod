package nl.tivek.multiversepowers.engine.client.world;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.tivek.multiversepowers.engine.physics.Blocks;

// The solid blocks around a simulation (a ragdoll), as the player's own game has them (a chunk it has not got is empty
// air). A whole block with whole blocks on all six sides is left out: nothing reaches it before the blocks round it,
// and the deep ground under a fast body would crowd out the top it lands on. One is reused for every simulation, so
// gathering makes no garbage.
public final class LevelBlocks implements Blocks, Shapes.DoubleLineConsumer {
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
    // The region and a border of one block round it: each cell's block and whether it is a whole one.
    private BlockState[] states = new BlockState[0];
    private boolean[] whole = new boolean[0];

    public LevelBlocks in(@Nullable Level level) {
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
        if (y1 < y0) {
            return 0;
        }
        int sx = x1 - x0 + 3;
        int sy = y1 - y0 + 3;
        int sz = z1 - z0 + 3;
        int cells = sx * sy * sz;
        if (this.states.length < cells) {
            this.states = new BlockState[cells];
            this.whole = new boolean[cells];
        }
        for (int y = 0; y < sy; y++) {
            for (int x = 0; x < sx; x++) {
                for (int z = 0; z < sz; z++) {
                    int i = (y * sx + x) * sz + z;
                    this.at.set(x0 - 1 + x, y0 - 1 + y, z0 - 1 + z);
                    BlockState state = level.getBlockState(this.at);
                    this.states[i] = state;
                    this.whole[i] = !state.isAir() && state.isCollisionShapeFullBlock(level, this.at);
                }
            }
        }
        for (int y = 1; y < sy - 1; y++) {
            for (int x = 1; x < sx - 1; x++) {
                for (int z = 1; z < sz - 1; z++) {
                    if (this.count >= this.most) {
                        return this.count;
                    }
                    int i = (y * sx + x) * sz + z;
                    BlockState state = this.states[i];
                    if (state.isAir()) {
                        continue;
                    }
                    this.ox = x0 - 1 + x;
                    this.oy = y0 - 1 + y;
                    this.oz = z0 - 1 + z;
                    if (this.whole[i]) {
                        if (!this.buried(i, sx, sz)) {
                            this.consume(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
                        }
                        continue;
                    }
                    this.at.set(this.ox, this.oy, this.oz);
                    VoxelShape shape = state.getCollisionShape(level, this.at);
                    if (!shape.isEmpty()) {
                        shape.forAllBoxes(this);
                    }
                }
            }
        }
        return this.count;
    }

    private boolean buried(int i, int sx, int sz) {
        int layer = sx * sz;
        return this.whole[i - 1] && this.whole[i + 1] && this.whole[i - sz] && this.whole[i + sz]
                && this.whole[i - layer] && this.whole[i + layer];
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
