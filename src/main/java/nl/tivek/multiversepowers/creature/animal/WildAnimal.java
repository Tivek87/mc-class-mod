package nl.tivek.multiversepowers.creature.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import nl.tivek.multiversepowers.engine.math.Ease;

// A wild animal of the mod's own that does things now and then (acts: grazing, sitting up, shaking itself dry): the
// server starts an act, every game that sees it learns which by an entity event and poses it by how long it has gone
// (actAge), fading it in as it starts and out again once it ends (blend), worked out from the clock alone so a screen's
// copy that never ticks poses the same. A kind that comes in more than one look (a red or a grey squirrel) keeps
// which in `variant`.
public abstract class WildAnimal extends Animal {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(WildAnimal.class,
            EntityDataSerializers.INT);
    // The game's own entity events are all above 0, so the acts take the negative ones.
    private static final byte FIRST_EVENT = Byte.MIN_VALUE;
    private int act;
    private int actAt;
    private int ended;
    private int endedFrom;
    private int endedAt = Integer.MIN_VALUE / 2;
    private int actUntil;

    protected WildAnimal(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.variant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Variant"));
    }

    // How many kinds of act it has (numbered from 1).
    protected abstract int kinds();

    // Ticks an act takes to fade in, and out again once it ends.
    protected int fade(int kind) {
        return 6;
    }

    public int act() {
        return this.act;
    }

    public double actAge(float partialTick) {
        return this.tickCount - this.actAt + partialTick;
    }

    // How long `kind` has gone, or went before it ended and while it fades out.
    public double actAge(int kind, float partialTick) {
        return this.tickCount + partialTick - (kind == this.act || kind != this.ended ? this.actAt : this.endedFrom);
    }

    // How far `kind` is posed now, from 0 to 1.
    public float blend(int kind, float partialTick) {
        double now = this.tickCount + partialTick;
        int fade = this.fade(kind);
        if (kind == this.act) {
            return (float) Ease.smooth((now - this.actAt) / fade);
        }
        if (kind == this.ended) {
            double was = Ease.smooth((double) (this.endedAt - this.endedFrom) / fade);
            return (float) (was * (1.0 - Ease.smooth((now - this.endedAt) / fade)));
        }
        return 0.0F;
    }

    // Starts an act here and in every game that sees it, ended by itself after `ticks` (0 ends the act going).
    public void start(int kind, int ticks) {
        this.set(kind);
        this.actUntil = kind == 0 ? 0 : this.tickCount + ticks;
        this.level().broadcastEntityEvent(this, (byte) (FIRST_EVENT + kind));
    }

    // Ends `kind` if it is still going.
    public void end(int kind) {
        if (this.act == kind && kind != 0) {
            this.start(0, 0);
        }
    }

    // Shows an act in this game alone, as if the server had started it (one shown in a screen).
    public void play(int kind) {
        this.set(kind);
    }

    private void set(int kind) {
        if (kind == this.act) {
            return;
        }
        if (this.act != 0) {
            this.ended = this.act;
            this.endedFrom = this.actAt;
            this.endedAt = this.tickCount;
        }
        this.act = kind;
        this.actAt = this.tickCount;
    }

    // In daylight on grass, snow, podzol or coarse dirt, as wolves come into the world: the floor of forest and taiga.
    public static boolean checkWildSpawnRules(EntityType<? extends Animal> type, LevelAccessor level,
            MobSpawnType spawn, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.WOLVES_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= FIRST_EVENT && id <= FIRST_EVENT + this.kinds()) {
            this.set(id - FIRST_EVENT);
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.act != 0 && this.tickCount >= this.actUntil) {
            this.start(0, 0);
        }
    }
}
