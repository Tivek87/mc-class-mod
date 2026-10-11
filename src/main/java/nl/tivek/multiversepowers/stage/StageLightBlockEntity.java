package nl.tivek.multiversepowers.stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A stage light's own settings, whether redstone has it on, and on the server the creature it follows, if any.
public final class StageLightBlockEntity extends BlockEntity {
    // Every lamp loaded, on either side; what draws or plays a show filters by its level.
    private static final Set<StageLightBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final int LOOK_EVERY = 10;
    private static final double CHANNEL_REACH = 128.0;

    private final LampKind kind;
    private LampSettings settings;
    private boolean powered;
    private int target = -1;

    public StageLightBlockEntity(BlockPos pos, BlockState state) {
        super(StageBlocks.LAMP.get(), pos, state);
        this.kind = state.getBlock() instanceof StageLightBlock block ? block.kind : LampKind.MOVING_HEAD;
        this.settings = LampSettings.of(this.kind);
    }

    public LampKind kind() {
        return this.kind;
    }

    public LampSettings settings() {
        return this.settings;
    }

    // The creature it follows, as its entity id in this level, or -1.
    public int target() {
        return this.target;
    }

    public boolean lit() {
        return this.settings.on && switch (this.settings.redstone) {
            case IGNORE -> true;
            case ON_WHEN_POWERED -> this.powered;
            case OFF_WHEN_POWERED -> !this.powered;
        };
    }

    public static List<StageLightBlockEntity> loaded(Level level) {
        List<StageLightBlockEntity> found = new ArrayList<>();
        for (StageLightBlockEntity lamp : LOADED) {
            if (lamp.level == level && !lamp.isRemoved()) {
                found.add(lamp);
            }
        }
        return found;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        LOADED.remove(this);
    }

    void toggle() {
        this.settings.on = !this.settings.on;
        this.changed();
    }

    void powered(boolean powered) {
        if (this.powered != powered) {
            this.powered = powered;
            this.changed();
        }
    }

    // A player's change: what `tag` holds, kept within range; with `toChannel` the show part also goes to every
    // lamp near on the same channel.
    public void edit(CompoundTag tag, boolean toChannel) {
        this.settings.read(tag);
        this.target = -1;
        this.changed();
        if (toChannel && this.settings.channel > 0 && this.level != null) {
            CompoundTag show = this.settings.show();
            for (StageLightBlockEntity other : loaded(this.level)) {
                if (other != this && other.settings.channel == this.settings.channel
                        && other.worldPosition.closerThan(this.worldPosition, CHANNEL_REACH)) {
                    other.settings.read(show);
                    other.target = -1;
                    other.changed();
                }
            }
        }
    }

    static void tick(Level level, BlockPos pos, BlockState state, StageLightBlockEntity lamp) {
        if (level.getGameTime() % LOOK_EVERY == Math.floorMod(pos.asLong(), LOOK_EVERY)
                && level instanceof ServerLevel server) {
            int found = lamp.lit() ? lamp.look(server) : -1;
            if (found != lamp.target) {
                lamp.target = found;
                lamp.sync();
            }
        }
    }

    private int look(ServerLevel level) {
        LampSettings.Aim aim = this.settings.aim;
        if (aim == LampSettings.Aim.FOLLOW) {
            Entity followed = this.settings.follow == null ? null : level.getEntity(this.settings.follow);
            return followed != null && followed.isAlive() && followed.distanceToSqr(Vec3.atCenterOf(this.worldPosition))
                    < LampSettings.MOST_LENGTH * LampSettings.MOST_LENGTH ? followed.getId() : -1;
        }
        if (aim != LampSettings.Aim.MOB && aim != LampSettings.Aim.PLAYER && aim != LampSettings.Aim.HOSTILE) {
            return -1;
        }
        Vec3 from = Vec3.atCenterOf(this.worldPosition);
        double range = this.settings.aimRange;
        LivingEntity best = null;
        double nearest = range * range;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(range),
                LivingEntity::isAlive)) {
            boolean player = living instanceof Player;
            if (living.isSpectator() || living.isInvisible()
                    || aim == LampSettings.Aim.PLAYER && !player
                    || aim == LampSettings.Aim.MOB && player
                    || aim == LampSettings.Aim.HOSTILE && !(living instanceof Enemy)) {
                continue;
            }
            double away = living.distanceToSqr(from);
            if (away < nearest) {
                nearest = away;
                best = living;
            }
        }
        return best == null ? -1 : best.getId();
    }

    private void changed() {
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            int light = this.lit() ? this.settings.light : 0;
            if (state.hasProperty(StageLightBlock.LIGHT) && state.getValue(StageLightBlock.LIGHT) != light) {
                this.level.setBlock(this.worldPosition, state.setValue(StageLightBlock.LIGHT, light),
                        Block.UPDATE_ALL);
            }
        }
        this.sync();
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("lamp", this.settings.write());
        tag.putBoolean("powered", this.powered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("lamp")) {
            this.settings.read(tag.getCompound("lamp"));
        }
        this.powered = tag.getBoolean("powered");
        this.target = tag.contains("target") ? tag.getInt("target") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("lamp", this.settings.write());
        tag.putBoolean("powered", this.powered);
        tag.putInt("target", this.target);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
