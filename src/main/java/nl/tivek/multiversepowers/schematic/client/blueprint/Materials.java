package nl.tivek.multiversepowers.schematic.client.blueprint;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;

// What a block of a plan takes to build: its item and how many. A door or a tall flower is one item for both its
// halves, a bed for both its ends, a double slab two slabs; water and lava come by the bucket; a block with no item
// of its own (fire, a portal) takes nothing.
final class Materials {
    private Materials() {
    }

    static Item item(BlockState state) {
        if (state.isAir()) {
            return Items.AIR;
        }
        if (state.getBlock() instanceof LiquidBlock) {
            FluidState fluid = state.getFluidState();
            return fluid.isSource() ? fluid.getType().getBucket() : Items.AIR;
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return Items.AIR;
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)
                && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            return Items.AIR;
        }
        return state.getBlock().asItem();
    }

    static int count(BlockState state) {
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
            return 2;
        }
        if (state.hasProperty(BlockStateProperties.CANDLES)) {
            return state.getValue(BlockStateProperties.CANDLES);
        }
        if (state.hasProperty(BlockStateProperties.PICKLES)) {
            return state.getValue(BlockStateProperties.PICKLES);
        }
        if (state.hasProperty(BlockStateProperties.EGGS)) {
            return state.getValue(BlockStateProperties.EGGS);
        }
        if (state.hasProperty(BlockStateProperties.LAYERS)) {
            return state.getValue(BlockStateProperties.LAYERS);
        }
        return 1;
    }
}
