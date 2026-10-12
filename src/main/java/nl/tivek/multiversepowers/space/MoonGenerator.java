package nl.tivek.multiversepowers.space;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;

// Makes the moon's chunks from MoonGround: no caves, no ores, nothing growing or living.
public final class MoonGenerator extends ChunkGenerator {
    public static final MapCodec<MoonGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.fieldOf("biome").forGetter(generator -> generator.biome))
            .apply(instance, instance.stable(MoonGenerator::new)));
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(
            Registries.CHUNK_GENERATOR, MultiversePowers.MODID);

    static {
        GENERATORS.register("moon", () -> CODEC);
    }

    private final Holder<Biome> biome;

    public MoonGenerator(Holder<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.biome = biome;
    }

    public static void register(IEventBus modEventBus) {
        GENERATORS.register(modEventBus);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
            StructureManager structures, ChunkAccess chunk) {
        int fromX = chunk.getPos().getMinBlockX();
        int fromZ = chunk.getPos().getMinBlockZ();
        // The ground's tops a column past the chunk on every side, for how steep each column is.
        int[][] tops = new int[18][18];
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 18; j++) {
                tops[i][j] = MoonGround.top(fromX + i - 1, fromZ + j - 1);
            }
        }
        Heightmap floor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int height = Math.min(chunk.getHeight(), MoonGround.HIGHEST + 8);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = tops[x + 1][z + 1];
                int drop = Math.max(Math.max(top - tops[x][z + 1], top - tops[x + 2][z + 1]),
                        Math.max(top - tops[x + 1][z], top - tops[x + 1][z + 2]));
                BlockState[] column = MoonGround.column(fromX + x, fromZ + z, height, top, drop);
                for (int y = 0; y < height; y++) {
                    BlockState state = column[y];
                    if (state.isAir()) {
                        continue;
                    }
                    int at = chunk.getMinBuildHeight() + y;
                    chunk.setBlockState(pos.set(x, at, z), state, false);
                    floor.update(x, at, z, state);
                    surface.update(x, at, z, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return level.getMinBuildHeight() + MoonGround.surface(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        return new NoiseColumn(level.getMinBuildHeight(), MoonGround.column(x, z,
                Math.min(level.getHeight(), MoonGround.HIGHEST + 8)));
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structures, RandomState random,
            ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomes,
            StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }
}
