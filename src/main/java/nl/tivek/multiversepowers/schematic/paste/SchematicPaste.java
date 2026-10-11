package nl.tivek.multiversepowers.schematic.paste;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicPlacement;
import nl.tivek.multiversepowers.schematic.SchematicRules;
import nl.tivek.multiversepowers.schematic.net.SchematicStatusPayload;

// A schematic going into the world a few thousand cells a tick (world setting), the chunks nearest its owner first.
// A chunk the server has not loaded is waited for, never loaded for it (that would freeze the server): its owner is
// told, and it goes in once someone comes near enough. Its creatures come last, once every block is in.
final class SchematicPaste implements Effect {
    static final int ALL = 0;
    static final int SOLID = 1;
    static final int EMPTY = 2;
    static final int CREATURES = 1;
    static final int CONTENTS = 2;
    static final int PHYSICS = 4;
    private static final int REPORT = 10;

    final UUID owner;
    final Schematic schematic;
    private final SchematicPlacement placement;
    private final int mode;
    private final int flags;
    private final BlockState[] turned;
    private final long[] columns;
    // Per column, how many of its cells are done; -1 once all are.
    private final int[] progress;
    private final int sizeX;
    private final int sizeZ;
    final int total;
    int done;
    int waiting;
    final PasteUndo undo;
    boolean cancelled;
    private final int[] xz = new int[2];
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

    SchematicPaste(ServerPlayer owner, Schematic schematic, SchematicPlacement placement, int mode, int flags) {
        this.owner = owner.getUUID();
        this.schematic = schematic;
        this.placement = placement;
        this.mode = mode;
        this.flags = flags;
        this.turned = new BlockState[schematic.palette.size()];
        boolean[] air = new boolean[this.turned.length];
        for (int k = 0; k < this.turned.length; k++) {
            this.turned[k] = schematic.palette.get(k).mirror(placement.mirror()).rotate(placement.rotation());
            air[k] = this.turned[k].isAir();
        }
        int count = 0;
        for (short cell : schematic.cells) {
            if (cell != Schematic.VOID && !(mode == SOLID && air[cell])) {
                count++;
            }
        }
        this.total = count;
        this.sizeX = placement.sizeX(schematic);
        this.sizeZ = placement.sizeZ(schematic);
        BlockPos origin = placement.origin();
        List<Long> chunks = new ArrayList<>();
        for (int cx = origin.getX() >> 4; cx <= (origin.getX() + this.sizeX - 1) >> 4; cx++) {
            for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + this.sizeZ - 1) >> 4; cz++) {
                chunks.add(ChunkPos.asLong(cx, cz));
            }
        }
        ChunkPos near = owner.chunkPosition();
        chunks.sort(Comparator.comparingLong(chunk -> {
            long dx = ChunkPos.getX(chunk) - near.x;
            long dz = ChunkPos.getZ(chunk) - near.z;
            return dx * dx + dz * dz;
        }));
        this.columns = chunks.stream().mapToLong(Long::longValue).toArray();
        this.progress = new int[this.columns.length];
        this.undo = new PasteUndo(schematic.name, owner.level().dimension(), SchematicRules.undoBlocks());
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.owner);
        if (player == null || this.cancelled) {
            if (player != null) {
                this.report(player, SchematicStatusPayload.CANCELLED);
            }
            SchematicPastes.ended(this);
            return false;
        }
        int budget = SchematicRules.blocksPerTick();
        int unloaded = 0;
        boolean left = false;
        for (int c = 0; c < this.columns.length; c++) {
            if (this.progress[c] < 0) {
                continue;
            }
            int cx = ChunkPos.getX(this.columns[c]);
            int cz = ChunkPos.getZ(this.columns[c]);
            if (!level.hasChunk(cx, cz)) {
                unloaded++;
                left = true;
                continue;
            }
            if (budget > 0) {
                budget = this.column(level, c, cx, cz, budget);
            }
            left |= this.progress[c] >= 0;
        }
        this.waiting = unloaded;
        if (!left) {
            if ((this.flags & CREATURES) != 0) {
                this.creatures(level);
            }
            this.report(player, SchematicStatusPayload.DONE);
            SchematicPastes.ended(this);
            return false;
        }
        if (age % REPORT == 0) {
            this.report(player, unloaded > 0 ? SchematicStatusPayload.WAITING : SchematicStatusPayload.RUNNING);
        }
        return true;
    }

    // Goes on through one chunk's part of the box, up to `budget` cells; returns what is left of it.
    private int column(ServerLevel level, int c, int cx, int cz, int budget) {
        BlockPos origin = this.placement.origin();
        int x0 = Math.max(origin.getX(), cx << 4);
        int x1 = Math.min(origin.getX() + this.sizeX, (cx << 4) + 16);
        int z0 = Math.max(origin.getZ(), cz << 4);
        int z1 = Math.min(origin.getZ() + this.sizeZ, (cz << 4) + 16);
        int wide = x1 - x0;
        int layer = wide * (z1 - z0);
        int cells = layer * this.schematic.sizeY;
        int k = this.progress[c];
        while (k < cells && budget > 0) {
            int y = k / layer;
            int rest = k - y * layer;
            int z = rest / wide;
            this.place(level, x0 + rest - z * wide, y, z0 + z);
            k++;
            budget--;
        }
        this.progress[c] = k >= cells ? -1 : k;
        return budget;
    }

    private void place(ServerLevel level, int wx, int y, int wz) {
        BlockPos origin = this.placement.origin();
        this.placement.source(this.schematic, wx - origin.getX(), wz - origin.getZ(), this.xz);
        int index = this.schematic.index(this.xz[0], y, this.xz[1]);
        short cell = this.schematic.cells[index];
        if (cell == Schematic.VOID) {
            return;
        }
        BlockState state = this.turned[cell];
        if (this.mode == SOLID && state.isAir()) {
            return;
        }
        this.done++;
        this.at.set(wx, origin.getY() + y, wz);
        if (level.isOutsideBuildHeight(this.at)) {
            return;
        }
        BlockState old = level.getBlockState(this.at);
        if (this.mode == EMPTY && !old.isAir() && !old.canBeReplaced()) {
            return;
        }
        CompoundTag data = (this.flags & CONTENTS) != 0 ? this.schematic.blockEntities.get(index) : null;
        if (old == state && data == null) {
            return;
        }
        BlockEntity oldEntity = level.getBlockEntity(this.at);
        this.undo.add(this.at.asLong(), Block.getId(old),
                oldEntity == null ? null : oldEntity.saveWithFullMetadata(level.registryAccess()));
        if (oldEntity != null) {
            // Emptied first, or a replaced chest would spill what it held.
            Clearable.tryClear(oldEntity);
        }
        level.setBlock(this.at, state, (this.flags & PHYSICS) != 0 ? Block.UPDATE_ALL
                : Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (data != null) {
            load(level, this.at, data);
        }
    }

    static void load(ServerLevel level, BlockPos pos, CompoundTag data) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return;
        }
        CompoundTag tag = data.copy();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        try {
            entity.loadWithComponents(tag, level.registryAccess());
            entity.setChanged();
        } catch (RuntimeException ignored) {
            // Data of another kind of block entity than the block now has: it stays empty.
        }
    }

    // As the game places a structure's creatures: turned and mirrored with it, with new ids.
    private void creatures(ServerLevel level) {
        for (Schematic.Creature creature : this.schematic.creatures) {
            Vec3 pos = this.placement.world(this.schematic, creature.pos());
            if (!level.hasChunk(BlockPos.containing(pos).getX() >> 4, BlockPos.containing(pos).getZ() >> 4)) {
                continue;
            }
            CompoundTag tag = creature.tag().copy();
            tag.remove("UUID");
            ListTag place = new ListTag();
            place.add(DoubleTag.valueOf(pos.x));
            place.add(DoubleTag.valueOf(pos.y));
            place.add(DoubleTag.valueOf(pos.z));
            tag.put("Pos", place);
            try {
                EntityType.create(tag, level).ifPresent(entity -> {
                    float yaw = entity.rotate(this.placement.rotation());
                    yaw += entity.mirror(this.placement.mirror()) - entity.getYRot();
                    entity.moveTo(pos.x, pos.y, pos.z, yaw, entity.getXRot());
                    level.addFreshEntityWithPassengers(entity);
                    this.undo.creatures.add(entity.getUUID());
                });
            } catch (RuntimeException ignored) {
                // A creature this game cannot make from its data.
            }
        }
    }

    void report(@Nullable ServerPlayer player, int state) {
        if (player != null) {
            PacketDistributor.sendToPlayer(player, new SchematicStatusPayload(state, this.schematic.name, this.done,
                    this.total, this.waiting));
        }
    }
}
