package nl.tivek.multiversepowers.character.thor;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;

// Mjolnir in flight, as everyone sees it: Mjolnir moves and turns it on the server, each player's game draws it
// (ThrownHammerRenderer). Never saved, so a world saved while it flew loads without it.
public final class ThrownHammer extends Entity {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownHammer>> TYPE = TYPES.register("mjolnir",
            () -> EntityType.Builder.<ThrownHammer>of(ThrownHammer::new, MobCategory.MISC).sized(0.5F, 0.5F)
                    .clientTrackingRange(10).updateInterval(1).noSave().noSummon().fireImmune().build("mjolnir"));
    // How big its thrower is, so it flies as big as it was in his hand.
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(ThrownHammer.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> CHARGED = SynchedEntityData.defineId(ThrownHammer.class,
            EntityDataSerializers.BOOLEAN);

    public ThrownHammer(EntityType<? extends ThrownHammer> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
    }

    public float size() {
        return this.entityData.get(SIZE);
    }

    void setSize(float size) {
        this.entityData.set(SIZE, size);
    }

    public boolean charged() {
        return this.entityData.get(CHARGED);
    }

    void setCharged(boolean charged) {
        this.entityData.set(CHARGED, charged);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 1.0F);
        builder.define(CHARGED, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(this.size());
    }
}
