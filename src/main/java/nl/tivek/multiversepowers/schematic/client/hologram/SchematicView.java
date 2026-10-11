package nl.tivek.multiversepowers.schematic.client.hologram;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import nl.tivek.multiversepowers.schematic.Schematic;

// A schematic as a little world of its own for the game's block drawing: its blocks where it has them, air elsewhere,
// always fully lit and tinted as in one biome, so its hologram looks the same wherever it stands.
final class SchematicView implements BlockAndTintGetter {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final Schematic schematic;
    private final Biome biome;
    private final LevelLightEngine light;
    // The highest layer drawn: what lies above is air, so the faces under it show.
    volatile int top = Integer.MAX_VALUE;

    SchematicView(Schematic schematic, Biome biome, LevelLightEngine light) {
        this.schematic = schematic;
        this.biome = biome;
        this.light = light;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (pos.getY() > this.top) {
            return AIR;
        }
        BlockState state = this.schematic.state(pos.getX(), pos.getY(), pos.getZ());
        return state == null ? AIR : state;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        if (!shade) {
            return 1.0F;
        }
        return switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.light;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkened) {
        return 15;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return resolver.getColor(this.biome, pos.getX(), pos.getZ());
    }

    @Override
    public int getHeight() {
        return this.schematic.sizeY;
    }

    @Override
    public int getMinBuildHeight() {
        return 0;
    }
}
