package nl.tivek.multiversepowers.schematic.client.blueprint;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicPlacement;
import nl.tivek.multiversepowers.schematic.blueprint.Blueprint;
import nl.tivek.multiversepowers.schematic.client.hologram.HologramMesh;
import nl.tivek.multiversepowers.schematic.client.hologram.HologramModel;

// How far a blueprint is built, as this game sees the world: each of its cells looked at a few thousand a tick, and at
// once when a block in it changes. Per cell: built, missing, the wrong block, the right block turned the wrong way, or
// a block in the way where the plan has air; per kind of item, how many it needs and how many are in place. A cell in
// a part of the world this game has not loaded keeps what it was last seen as.
final class BlueprintCheck {
    static final byte UNKNOWN = 0;
    static final byte DONE = 1;
    static final byte MISSING = 2;
    static final byte WRONG = 3;
    static final byte TURNED = 4;
    static final byte IN_THE_WAY = 5;
    static final byte NOTHING = 6;
    // What a block must face and how it stands to be right; the rest (how a fence joins, a leaf's distance) follows
    // by itself.
    private static final List<Property<?>> SHAPE = List.of(BlockStateProperties.FACING,
            BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.FACING_HOPPER,
            BlockStateProperties.VERTICAL_DIRECTION, BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_AXIS,
            BlockStateProperties.HALF, BlockStateProperties.DOUBLE_BLOCK_HALF, BlockStateProperties.SLAB_TYPE,
            BlockStateProperties.DOOR_HINGE, BlockStateProperties.ROTATION_16, BlockStateProperties.ATTACH_FACE,
            BlockStateProperties.ORIENTATION, BlockStateProperties.BELL_ATTACHMENT, BlockStateProperties.LAYERS,
            BlockStateProperties.CANDLES, BlockStateProperties.PICKLES, BlockStateProperties.EGGS,
            BlockStateProperties.BED_PART);

    final Blueprint blueprint;
    final Schematic schematic;
    final SchematicPlacement placement;
    private final BlockState[] turned;
    private final int[] itemOf;
    private final int[] weightOf;
    final List<Item> items = new ArrayList<>();
    final int[] needed;
    final int[] placed;
    final byte[] status;
    private final int wideX;
    private final int wideZ;
    private final int[] xz = new int[2];
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    private int cursor;
    int passes;
    // Cells of the plan not seen yet; blocks wrong, turned the wrong way and in the way.
    int unseen;
    int wrong;
    int turnedWrong;
    int inTheWay;
    long changed;

    BlueprintCheck(Blueprint blueprint, Schematic schematic) {
        this.blueprint = blueprint;
        this.schematic = schematic;
        this.placement = blueprint.placement();
        int kinds = schematic.palette.size();
        this.turned = new BlockState[kinds];
        this.itemOf = new int[kinds];
        this.weightOf = new int[kinds];
        Object2IntOpenHashMap<Item> index = new Object2IntOpenHashMap<>();
        index.defaultReturnValue(-1);
        for (int k = 0; k < kinds; k++) {
            this.turned[k] = schematic.palette.get(k).mirror(this.placement.mirror()).rotate(this.placement
                    .rotation());
            Item item = Materials.item(this.turned[k]);
            if (item == Items.AIR) {
                this.itemOf[k] = -1;
                continue;
            }
            int slot = index.getInt(item);
            if (slot < 0) {
                slot = this.items.size();
                this.items.add(item);
                index.put(item, slot);
            }
            this.itemOf[k] = slot;
            this.weightOf[k] = Materials.count(this.turned[k]);
        }
        this.needed = new int[this.items.size()];
        this.placed = new int[this.items.size()];
        for (short cell : schematic.cells) {
            if (cell != Schematic.VOID && this.itemOf[cell] >= 0) {
                this.needed[this.itemOf[cell]] += this.weightOf[cell];
            }
        }
        this.status = new byte[schematic.volume()];
        for (short cell : schematic.cells) {
            if (cell != Schematic.VOID && !this.turned[cell].isAir()) {
                this.unseen++;
            }
        }
        this.wideX = this.placement.sizeX(schematic);
        this.wideZ = this.placement.sizeZ(schematic);
    }

    // How the cell is drawn now.
    int look(int index) {
        return switch (this.status[index]) {
            case UNKNOWN, MISSING -> HologramMesh.PLAIN;
            case WRONG -> HologramMesh.WRONG;
            case TURNED -> HologramMesh.TURNED;
            case IN_THE_WAY -> HologramMesh.IN_THE_WAY;
            default -> HologramMesh.HIDDEN;
        };
    }

    boolean contains(BlockPos pos) {
        BlockPos origin = this.placement.origin();
        int u = pos.getX() - origin.getX();
        int y = pos.getY() - origin.getY();
        int w = pos.getZ() - origin.getZ();
        return u >= 0 && w >= 0 && y >= 0 && u < this.wideX && w < this.wideZ && y < this.schematic.sizeY;
    }

    // Looks at `budget` more cells, going round and round; the model's cubes whose cells changed are made again.
    void scan(ClientLevel level, int budget, @Nullable HologramModel model) {
        int volume = this.schematic.volume();
        for (int k = 0; k < budget; k++) {
            int u = this.cursor % this.wideX;
            int rest = this.cursor / this.wideX;
            int w = rest % this.wideZ;
            int y = rest / this.wideZ;
            this.look(level, u, y, w, model);
            if (++this.cursor >= volume) {
                this.cursor = 0;
                this.passes++;
            }
        }
    }

    // A block of the world changed: its cell is looked at again now.
    void changed(ClientLevel level, BlockPos pos, @Nullable HologramModel model) {
        BlockPos origin = this.placement.origin();
        this.look(level, pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ(), model);
    }

    private void look(ClientLevel level, int u, int y, int w, @Nullable HologramModel model) {
        this.placement.source(this.schematic, u, w, this.xz);
        int index = this.schematic.index(this.xz[0], y, this.xz[1]);
        short cell = this.schematic.cells[index];
        if (cell == Schematic.VOID) {
            return;
        }
        BlockPos origin = this.placement.origin();
        this.at.set(origin.getX() + u, origin.getY() + y, origin.getZ() + w);
        if (!level.hasChunk(this.at.getX() >> 4, this.at.getZ() >> 4)) {
            return;
        }
        byte now = judge(this.turned[cell], level.getBlockState(this.at));
        byte was = this.status[index];
        if (now == was) {
            return;
        }
        this.status[index] = now;
        this.count(cell, was, -1);
        this.count(cell, now, 1);
        this.changed = Util.getMillis();
        if (model != null) {
            model.refresh(this.xz[0], y, this.xz[1]);
        }
    }

    private void count(short cell, byte state, int sign) {
        boolean air = this.turned[cell].isAir();
        switch (state) {
            case UNKNOWN -> {
                if (!air) {
                    this.unseen += sign;
                }
            }
            case DONE, TURNED -> {
                if (this.itemOf[cell] >= 0) {
                    this.placed[this.itemOf[cell]] += sign * this.weightOf[cell];
                }
                if (state == TURNED) {
                    this.turnedWrong += sign;
                }
            }
            case WRONG -> this.wrong += sign;
            case IN_THE_WAY -> this.inTheWay += sign;
            default -> {
            }
        }
    }

    // What a cell is, the plan wanting `expected` where the world has `world`.
    static byte judge(BlockState expected, BlockState world) {
        if (expected.isAir()) {
            return world.isAir() || world.canBeReplaced() ? NOTHING : IN_THE_WAY;
        }
        if (world == expected) {
            return DONE;
        }
        if (world.getBlock() == expected.getBlock()) {
            for (Property<?> property : SHAPE) {
                if (expected.hasProperty(property) && !expected.getValue(property).equals(world.getValue(property))) {
                    return TURNED;
                }
            }
            return DONE;
        }
        if (world.isAir() || world.canBeReplaced()) {
            return MISSING;
        }
        return WRONG;
    }

    int neededTotal() {
        int total = 0;
        for (int n : this.needed) {
            total += n;
        }
        return total;
    }

    int placedTotal() {
        int total = 0;
        for (int n : this.placed) {
            total += n;
        }
        return total;
    }
}
