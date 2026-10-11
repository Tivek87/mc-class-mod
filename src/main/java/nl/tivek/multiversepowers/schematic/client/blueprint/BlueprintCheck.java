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
import net.minecraft.world.level.block.Blocks;
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
    // By layer (y): the cells still to be built or cleared there.
    private final int[] open;
    // The lowest layer not finished yet, kept for the mesh threads that read it once a cell.
    private volatile int lowest;
    // Shown a layer at a time: only the lowest layer not finished yet and what lies under it.
    volatile boolean layered;
    // The nearest cell still to be done, as the last search round found it (-1: none), and that round's own.
    private int searched;
    private int bestCell = -1;
    private double bestDistance;
    int target = -1;

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
        this.open = new int[schematic.sizeY];
        int layer = schematic.sizeX * schematic.sizeZ;
        for (int i = 0; i < schematic.cells.length; i++) {
            short cell = schematic.cells[i];
            if (cell != Schematic.VOID && !this.turned[cell].isAir()) {
                this.unseen++;
                this.open[i / layer]++;
            }
        }
        this.wideX = this.placement.sizeX(schematic);
        this.wideZ = this.placement.sizeZ(schematic);
        this.lowest = this.lowestOpen();
    }

    // How the cell is drawn now.
    int look(int index) {
        if (this.layered && index / (this.schematic.sizeX * this.schematic.sizeZ) > this.lowest) {
            return HologramMesh.HIDDEN;
        }
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
        boolean air = this.turned[cell].isAir();
        int opened = (open(air, now) ? 1 : 0) - (open(air, was) ? 1 : 0);
        if (opened != 0) {
            this.open[y] += opened;
            this.lowest = this.lowestOpen();
        }
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

    // Whether a cell still has to be built (or, where the plan has air, cleared).
    private static boolean open(boolean air, byte state) {
        return air ? state == IN_THE_WAY : state != DONE;
    }

    // The lowest layer not finished yet; the height when all are.
    int layer() {
        return this.lowest;
    }

    private int lowestOpen() {
        for (int y = 0; y < this.open.length; y++) {
            if (this.open[y] > 0) {
                return y;
            }
        }
        return this.open.length;
    }

    // Goes on looking for the cell still to be done nearest `eye`, `budget` cells a call (only in the layer shown
    // while layered); each round's nearest becomes `target`. Cells not seen yet are left out.
    void seek(double eyeX, double eyeY, double eyeZ, int budget) {
        int area = this.wideX * this.wideZ;
        int layer = this.layer();
        int from = this.layered ? Math.min(layer, this.schematic.sizeY) * area : 0;
        int to = this.layered ? Math.min(this.status.length, from + area) : this.status.length;
        if (this.searched < from || this.searched >= to) {
            this.searched = from;
        }
        BlockPos origin = this.placement.origin();
        for (int k = 0; k < budget && from < to; k++) {
            int cursor = this.searched;
            int u = cursor % this.wideX;
            int rest = cursor / this.wideX;
            int w = rest % this.wideZ;
            int y = rest / this.wideZ;
            this.placement.source(this.schematic, u, w, this.xz);
            int index = this.schematic.index(this.xz[0], y, this.xz[1]);
            short cell = this.schematic.cells[index];
            byte state = this.status[index];
            if (cell != Schematic.VOID && state != UNKNOWN && open(this.turned[cell].isAir(), state)) {
                double dx = origin.getX() + u + 0.5 - eyeX;
                double dy = origin.getY() + y + 0.5 - eyeY;
                double dz = origin.getZ() + w + 0.5 - eyeZ;
                double distance = dx * dx + dy * dy + dz * dz;
                if (this.bestCell < 0 || distance < this.bestDistance) {
                    this.bestCell = cursor;
                    this.bestDistance = distance;
                }
            }
            if (++this.searched >= to) {
                this.target = this.bestCell;
                this.bestCell = -1;
                this.searched = from;
            }
        }
        if (this.target >= 0 && !this.stillOpen(this.target)) {
            this.target = -1;
        }
    }

    // Whether the cell at turned-box place `cursor` (x first, then z, then y) still has to be done.
    private boolean stillOpen(int cursor) {
        int u = cursor % this.wideX;
        int rest = cursor / this.wideX;
        this.placement.source(this.schematic, u, rest % this.wideZ, this.xz);
        int index = this.schematic.index(this.xz[0], rest / this.wideZ, this.xz[1]);
        short cell = this.schematic.cells[index];
        return cell != Schematic.VOID && open(this.turned[cell].isAir(), this.status[index]);
    }

    // Where in the world the target lies, and the block it wants there (air when it is to be cleared).
    @Nullable
    BlockPos targetPos() {
        int cursor = this.target;
        if (cursor < 0) {
            return null;
        }
        int u = cursor % this.wideX;
        int rest = cursor / this.wideX;
        return this.placement.origin().offset(u, rest / this.wideZ, rest % this.wideZ);
    }

    BlockState targetState() {
        int cursor = this.target;
        if (cursor < 0) {
            return Blocks.AIR.defaultBlockState();
        }
        int u = cursor % this.wideX;
        int rest = cursor / this.wideX;
        this.placement.source(this.schematic, u, rest % this.wideZ, this.xz);
        short cell = this.schematic.cells[this.schematic.index(this.xz[0], rest / this.wideZ, this.xz[1])];
        return cell == Schematic.VOID ? Blocks.AIR.defaultBlockState()
                : this.turned[cell];
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
