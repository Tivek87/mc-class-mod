package nl.tivek.multiversepowers.schematic.paste;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.schematic.SchematicRules;
import nl.tivek.multiversepowers.schematic.net.SchematicStatusPayload;

// A paste taken back: every block it replaced put back, last first, and the creatures it brought gone. A block in a
// chunk the server has not loaded waits for it, as the paste did.
final class SchematicUndo implements Effect {
    private static final int REPORT = 10;

    final UUID owner;
    private final PasteUndo undo;
    private int next;
    private IntArrayList waiting = new IntArrayList();
    private int done;
    boolean cancelled;
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

    SchematicUndo(ServerPlayer owner, PasteUndo undo) {
        this.owner = owner.getUUID();
        this.undo = undo;
        this.next = undo.count - 1;
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.owner);
        if (this.cancelled) {
            if (player != null) {
                PacketDistributor.sendToPlayer(player, new SchematicStatusPayload(SchematicStatusPayload.CANCELLED,
                        this.undo.name, this.done, this.undo.count, 0));
            }
            SchematicPastes.undone(this);
            return false;
        }
        if (age == 0) {
            for (UUID id : this.undo.creatures) {
                Entity creature = level.getEntity(id);
                if (creature != null) {
                    creature.discard();
                }
            }
        }
        int budget = SchematicRules.blocksPerTick();
        IntArrayList still = new IntArrayList();
        for (int k = 0; k < this.waiting.size(); k++) {
            int entry = this.waiting.getInt(k);
            if (budget > 0 && this.restore(level, entry)) {
                budget--;
            } else {
                still.add(entry);
            }
        }
        this.waiting = still;
        while (this.next >= 0 && budget > 0) {
            if (!this.restore(level, this.next)) {
                this.waiting.add(this.next);
            }
            this.next--;
            budget--;
        }
        boolean over = this.next < 0 && this.waiting.isEmpty();
        if (player != null && (over || age % REPORT == 0)) {
            PacketDistributor.sendToPlayer(player, new SchematicStatusPayload(over ? SchematicStatusPayload.UNDONE
                    : SchematicStatusPayload.UNDOING, this.undo.name, this.done, this.undo.count, this.chunks()));
        }
        if (over) {
            SchematicPastes.undone(this);
        }
        return !over;
    }

    private int chunks() {
        LongOpenHashSet seen = new LongOpenHashSet();
        for (int k = 0; k < this.waiting.size(); k++) {
            long place = this.undo.places[this.waiting.getInt(k)];
            seen.add(ChunkPos.asLong(BlockPos.getX(place) >> 4, BlockPos.getZ(place) >> 4));
        }
        return seen.size();
    }

    private boolean restore(ServerLevel level, int entry) {
        this.at.set(this.undo.places[entry]);
        if (!level.hasChunk(this.at.getX() >> 4, this.at.getZ() >> 4)) {
            return false;
        }
        BlockEntity current = level.getBlockEntity(this.at);
        if (current != null) {
            Clearable.tryClear(current);
        }
        level.setBlock(this.at, Block.stateById(this.undo.states[entry]),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        CompoundTag data = this.undo.blockEntities.get(entry);
        if (data != null) {
            SchematicPaste.load(level, this.at, data);
        }
        this.done++;
        return true;
    }
}
