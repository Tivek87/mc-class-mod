package nl.tivek.multiversepowers.space;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Perlin;

// The shape of the moon's ground, worked out for any column by itself, so chunks join whatever order they are made
// in: a rolling plain of pale dust over rock, dark low seas of it, craters of three sizes each with a raised rim,
// bare rock where it is steep and loose boulders. The same moon in every world.
final class MoonGround {
    static final int LOWEST = 40;
    static final int HIGHEST = 200;
    private static final int DUST_DEEP = 3;
    private static final int STEEP = 3;
    private static final int DEEP_ROCK = 30;
    private static final Size[] CRATERS = { new Size(36, 0.5, 0.16, 0.4), new Size(104, 0.45, 0.18, 0.42),
        new Size(290, 0.4, 0.2, 0.42) };
    private static final Size BOULDERS = new Size(22, 0.3, 0.05, 0.13);
    // A crater's depth and its rim's height by its radius, and how far past the rim its slope runs out.
    private static final double DEPTH = 0.3;
    private static final double DEEPEST = 22.0;
    private static final double RIM = 0.25;
    private static final double REACH = 1.6;

    private static final BlockState DUST = Blocks.LIGHT_GRAY_CONCRETE_POWDER.defaultBlockState();
    private static final BlockState SEA_DUST = Blocks.GRAY_CONCRETE_POWDER.defaultBlockState();
    private static final BlockState ROCK = Blocks.STONE.defaultBlockState();
    private static final BlockState PALE_ROCK = Blocks.ANDESITE.defaultBlockState();
    private static final BlockState DEEP = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BOULDER = Blocks.COBBLESTONE.defaultBlockState();
    private static final BlockState DARK_BOULDER = Blocks.TUFF.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // Things scattered one to a square of `cell` blocks at most, each `least` to `most` of a cell in radius.
    private record Size(int cell, double chance, double least, double most) {
    }

    private MoonGround() {
    }

    // The highest block of the ground itself at a column, boulders left out.
    static int top(int x, int z) {
        double sea = sea(x, z);
        double rolling = 9.0 * Perlin.fbm(x / 170.0, 3.3, z / 170.0, 4) + 1.5 * Perlin.noise(x / 23.0, 9.1, z / 23.0);
        double height = Space.MOON_GROUND + rolling * (1.0 - 0.75 * sea) - 6.0 * sea;
        for (Size size : CRATERS) {
            height += craters(x, z, size);
        }
        return Mth.clamp(Mth.floor(height), LOWEST, HIGHEST);
    }

    // How much of a sea lies here, 0 to 1: the dark, low, flat plains.
    static double sea(int x, int z) {
        return Ease.smooth((Perlin.fbm(x / 620.0 + 40.0, 17.7, z / 620.0, 3) - 0.08) / 0.25);
    }

    private static double craters(int x, int z, Size size) {
        int cellX = Math.floorDiv(x, size.cell());
        int cellZ = Math.floorDiv(z, size.cell());
        double sum = 0.0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                long seed = seed(cellX + i, cellZ + j, size.cell());
                if (unit(seed, 0) >= size.chance()) {
                    continue;
                }
                double radius = size.cell() * Mth.lerp(unit(seed, 1), size.least(), size.most());
                double far = Math.hypot(x + 0.5 - (cellX + i + unit(seed, 2)) * size.cell(),
                        z + 0.5 - (cellZ + j + unit(seed, 3)) * size.cell()) / radius;
                if (far >= REACH) {
                    continue;
                }
                double depth = Math.min(DEEPEST, radius * DEPTH);
                double rim = depth * RIM;
                double out = (REACH - far) / (REACH - 1.0);
                sum += far < 1.0 ? rim * far * far * far * far - depth * (1.0 - far * far) : rim * out * out;
            }
        }
        return sum;
    }

    // How many blocks of a boulder stand on the ground at a column, and whether it is one of the dark ones.
    private static int boulder(int x, int z) {
        Size size = BOULDERS;
        int cellX = Math.floorDiv(x, size.cell());
        int cellZ = Math.floorDiv(z, size.cell());
        int most = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                long seed = seed(cellX + i, cellZ + j, size.cell());
                if (unit(seed, 0) >= size.chance()) {
                    continue;
                }
                double radius = size.cell() * Mth.lerp(unit(seed, 1) * unit(seed, 4), size.least(), size.most());
                double far = Math.hypot(x + 0.5 - (cellX + i + unit(seed, 2)) * size.cell(),
                        z + 0.5 - (cellZ + j + unit(seed, 3)) * size.cell());
                if (far < radius) {
                    int high = (int) Math.round(Math.sqrt(radius * radius - far * far) * 0.9);
                    most = Math.max(most, (high << 1) | (unit(seed, 5) < 0.35 ? 1 : 0));
                }
            }
        }
        return most;
    }

    // The highest solid block of a column, boulders too.
    static int surface(int x, int z) {
        return top(x, z) + (boulder(x, z) >> 1);
    }

    // A whole column from the world's floor up to `height` blocks: `tops` gives the ground's top at a column.
    static BlockState[] column(int x, int z, int height) {
        int top = top(x, z);
        int drop = 0;
        for (int side = 0; side < 4; side++) {
            drop = Math.max(drop, top - top(x + (side == 0 ? 1 : side == 1 ? -1 : 0),
                    z + (side == 2 ? 1 : side == 3 ? -1 : 0)));
        }
        return column(x, z, height, top, drop);
    }

    // `drop`: how far the ground falls to its lowest neighbour column.
    static BlockState[] column(int x, int z, int height, int top, int drop) {
        BlockState[] states = new BlockState[height];
        long seed = seed(x, z, 1);
        BlockState dust = sea(x, z) > 0.35 + 0.3 * unit(seed, 0) ? SEA_DUST : DUST;
        int dusty = drop >= STEEP ? 0 : DUST_DEEP;
        boolean pale = Perlin.noise(x / 31.0, 40.5, z / 31.0) > 0.25;
        int deep = DEEP_ROCK + (int) (unit(seed, 1) * 3.0);
        int boulder = boulder(x, z);
        for (int y = 0; y < height; y++) {
            if (y == 0) {
                states[y] = BEDROCK;
            } else if (y > top) {
                states[y] = y <= top + (boulder >> 1) ? (boulder & 1) != 0 ? DARK_BOULDER : BOULDER : AIR;
            } else if (y > top - dusty) {
                states[y] = dust;
            } else {
                states[y] = y < deep ? DEEP : pale ? PALE_ROCK : ROCK;
            }
        }
        return states;
    }

    private static long seed(int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h = (h ^ h >>> 30) * 0xBF58476D1CE4E5B9L;
        h = (h ^ h >>> 27) * 0x94D049BB133111EBL;
        return h ^ h >>> 31;
    }

    // The `n`th number from a seed, 0 to 1.
    private static double unit(long seed, int n) {
        long h = (seed + n * 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
        h = (h ^ h >>> 29) * 0x94D049BB133111EBL;
        return ((h ^ h >>> 32) >>> 11) * 0x1.0p-53;
    }
}
