package nl.tivek.multiversepowers.stage.show;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.stage.LampKind;
import nl.tivek.multiversepowers.stage.StageBlocks;
import nl.tivek.multiversepowers.stage.StageLightBlock;

// The light show's stage, built in front of whoever starts it: a truss of two posts and a beam with moving heads
// under it, lasers and strobes on it, a mirror ball hanging in front, wash lights on the floor before it and a
// searchlight at each back corner. Only air and plants are built over; every block it took is put back as it was.
final class LightShowRig {
    // Groups the program sets as one.
    enum Group {
        HEADS, LASERS, STROBES, DISCO, PARS, SEARCH
    }

    record Lamp(BlockPos pos, Group group, int index) {
    }

    private static final BlockState TRUSS = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final int BACK = 14;
    private static final int HIGH = 7;
    private static final int WIDE = 8;

    final ServerLevel level;
    final Vec3 stage;
    private final BlockPos origin;
    private final Direction forward;
    private final Direction right;
    private final List<BlockPos> placed = new ArrayList<>();
    private final List<BlockState> was = new ArrayList<>();
    private final List<BlockState> put = new ArrayList<>();
    final List<Lamp> lamps = new ArrayList<>();
    final Map<Group, Integer> counts = new EnumMap<>(Group.class);

    LightShowRig(ServerLevel level, BlockPos feet, Direction forward) {
        this.level = level;
        this.origin = feet;
        this.forward = forward;
        this.right = forward.getClockWise();
        this.stage = Vec3.atCenterOf(this.at(0, 3, BACK));
    }

    // The block `side` to the right, `up` and `ahead` in front of where it was started.
    BlockPos at(int side, int up, int ahead) {
        return this.origin.relative(this.right, side).relative(this.forward, ahead).above(up);
    }

    // Builds it; false (and nothing left of it) when too little of it found room.
    boolean build() {
        for (int up = 0; up < HIGH; up++) {
            this.block(this.at(-WIDE, up, BACK), TRUSS);
            this.block(this.at(WIDE, up, BACK), TRUSS);
        }
        for (int side = -WIDE; side <= WIDE; side++) {
            this.block(this.at(side, HIGH, BACK), TRUSS);
        }
        int[] heads = { -6, -2, 2, 6 };
        for (int side : heads) {
            this.lamp(this.at(side, HIGH - 1, BACK), LampKind.MOVING_HEAD, Direction.DOWN, Group.HEADS);
        }
        for (int side : new int[] { -4, 4 }) {
            this.lamp(this.at(side, HIGH, BACK - 1), LampKind.LASER, this.forward.getOpposite(), Group.LASERS);
        }
        for (int side : new int[] { -WIDE, WIDE }) {
            this.lamp(this.at(side, HIGH + 1, BACK), LampKind.STROBE, this.forward.getOpposite(), Group.STROBES);
        }
        this.block(this.at(0, HIGH, BACK - 1), TRUSS);
        this.block(this.at(0, HIGH - 1, BACK - 1), Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS,
                Direction.Axis.Y));
        this.lamp(this.at(0, HIGH - 2, BACK - 1), LampKind.DISCO_BALL, Direction.DOWN, Group.DISCO);
        for (int side : heads) {
            this.lamp(this.at(side, 0, BACK - 3), LampKind.PAR, Direction.UP, Group.PARS);
        }
        for (int side : new int[] { -WIDE - 3, WIDE + 3 }) {
            this.lamp(this.at(side, 0, BACK + 2), LampKind.SEARCHLIGHT, Direction.UP, Group.SEARCH);
        }
        if (this.lamps.size() < 8) {
            this.remove();
            return false;
        }
        return true;
    }

    private void lamp(BlockPos pos, LampKind kind, Direction facing, Group group) {
        BlockState state = StageBlocks.LAMPS.get(kind).get().defaultBlockState().setValue(StageLightBlock.FACING, facing);
        if (this.block(pos, state)) {
            this.lamps.add(new Lamp(pos, group, this.counts.merge(group, 1, Integer::sum) - 1));
        }
    }

    private boolean block(BlockPos pos, BlockState state) {
        if (!this.level.isLoaded(pos) || !this.level.isInWorldBounds(pos)) {
            return false;
        }
        BlockState now = this.level.getBlockState(pos);
        if (!now.canBeReplaced() || now.hasBlockEntity() || !now.getFluidState().isEmpty()) {
            return false;
        }
        this.placed.add(pos);
        this.was.add(now);
        this.put.add(state);
        this.level.setBlock(pos, state, Block.UPDATE_ALL);
        return true;
    }

    // Takes the stage down, last block first, where each is still as it was built, and puts back what stood there.
    void remove() {
        for (int i = this.placed.size() - 1; i >= 0; i--) {
            BlockPos pos = this.placed.get(i);
            if (this.level.isLoaded(pos) && this.level.getBlockState(pos).getBlock() == this.put.get(i).getBlock()) {
                this.level.setBlock(pos, this.was.get(i), Block.UPDATE_ALL);
            }
        }
        this.placed.clear();
        this.lamps.clear();
    }

    int count(Group group) {
        return this.counts.getOrDefault(group, 0);
    }
}
