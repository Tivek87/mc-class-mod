package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandAbyss;
import nl.tivek.multiversepowers.engine.world.BlockRules;

// The ground the abyss opens: every block of a round pit taken away for a moment and put back just as it was. Only
// what its caster could break goes, never a block holding things (a chest), and never where water or lava could run in.
final class AbyssPit {
    // Sent to players, but no neighbour is told: sand round the pit stays put and nothing drops.
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    // Every pit still open, put back when the server stops.
    private static final Set<AbyssPit> OPEN = new HashSet<>();
    // How far round the pit no water or lava may be, in blocks.
    private static final double DRY = 1.5;

    private final ServerLevel level;
    private final Vec3 center;
    private final int depth;
    private final List<BlockPos> carved = new ArrayList<>();
    private final List<BlockState> was = new ArrayList<>();

    private AbyssPit(ServerLevel level, Vec3 center, int depth) {
        this.level = level;
        this.center = center;
        this.depth = depth;
    }

    // How deep a pit may open here, 0 when none may: ground its caster may not break, water or lava near, the world's
    // bottom, or rock nothing breaks too close under it.
    static int depthAt(ServerLevel level, ServerPlayer owner, Vec3 center) {
        BlockPos top = BlockPos.containing(center.x, center.y - 0.5, center.z);
        if (!level.isLoaded(top) || !BlockRules.mayBreak(level, owner, top, level.getBlockState(top))) {
            return 0;
        }
        int deepest = Math.min(HandAbyss.DEEPEST, top.getY() - level.getMinBuildHeight() - 1);
        int reach = (int) Math.ceil(HandAbyss.RADIUS + DRY);
        for (int layer = -1; layer <= deepest; layer++) {
            int y = top.getY() - layer;
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    BlockPos pos = new BlockPos(top.getX() + dx, y, top.getZ() + dz);
                    double far = flat(center, pos);
                    if (far > HandAbyss.RADIUS + DRY) {
                        continue;
                    }
                    if (!level.isLoaded(pos)) {
                        return 0;
                    }
                    boolean wet = !level.getFluidState(pos).isEmpty();
                    boolean hard = far <= HandAbyss.RADIUS && level.getBlockState(pos).getDestroySpeed(level, pos) < 0.0F;
                    if (wet || hard) {
                        return layer < HandAbyss.SHALLOWEST ? 0 : layer;
                    }
                }
            }
        }
        return deepest < HandAbyss.SHALLOWEST ? 0 : deepest;
    }

    private static double flat(Vec3 center, BlockPos pos) {
        double dx = pos.getX() + 0.5 - center.x;
        double dz = pos.getZ() + 0.5 - center.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    static AbyssPit open(ServerLevel level, ServerPlayer owner, Vec3 center, int depth) {
        AbyssPit pit = new AbyssPit(level, center, depth);
        if (depth > 0) {
            pit.carve(owner);
            OPEN.add(pit);
        }
        return pit;
    }

    // From the plants on the ground down to the floor, so nothing is left hanging over the pit to drop.
    private void carve(ServerPlayer owner) {
        BlockPos top = BlockPos.containing(this.center.x, this.center.y - 0.5, this.center.z);
        int reach = (int) Math.ceil(HandAbyss.RADIUS);
        for (int y = top.getY() + 2; y > top.getY() - this.depth; y--) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    BlockPos pos = new BlockPos(top.getX() + dx, y, top.getZ() + dz);
                    if (flat(this.center, pos) > HandAbyss.RADIUS) {
                        continue;
                    }
                    BlockState state = this.level.getBlockState(pos);
                    if (state.isAir() || y > top.getY() && !state.getCollisionShape(this.level, pos).isEmpty()
                            || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                            || state.getDestroySpeed(this.level, pos) < 0.0F
                            || !BlockRules.mayBreak(this.level, owner, pos, state)) {
                        continue;
                    }
                    this.carved.add(pos);
                    this.was.add(state);
                    this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                }
            }
        }
    }

    // Whatever is still down in it is set on the ground first, or the ground would close round it; then every block
    // comes back, the floor first, where nothing else has been put since.
    void close() {
        if (!OPEN.remove(this)) {
            return;
        }
        double wide = HandAbyss.RADIUS + 0.5;
        AABB pit = new AABB(this.center.x - wide, this.center.y - this.depth - 1.0, this.center.z - wide,
                this.center.x + wide, this.center.y - 0.05, this.center.z + wide);
        for (Entity entity : this.level.getEntities((Entity) null, pit, entity -> !entity.isPassenger())) {
            entity.teleportTo(entity.getX(), this.center.y + 0.05, entity.getZ());
            entity.resetFallDistance();
        }
        for (int i = this.carved.size() - 1; i >= 0; i--) {
            BlockPos pos = this.carved.get(i);
            if (this.level.getBlockState(pos).canBeReplaced()) {
                this.level.setBlock(pos, this.was.get(i), FLAGS);
            }
        }
    }

    static void closeAll() {
        for (AbyssPit pit : List.copyOf(OPEN)) {
            pit.close();
        }
        OPEN.clear();
    }
}
