package nl.tivek.multiversepowers.character.thor.hammer;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;

// Mjolnir out of Thor's hands, as everyone sees it: flying out, resting in the world (lying, stuck in a block or
// hanging in the air) or flying back. Mjolnir moves it on the server, each player's game draws it
// (ThrownHammerRenderer) and finds whose it is. Never saved, so a world saved while it was out loads without it.
public final class ThrownHammer extends Entity {
    public static final byte OUT = 0;
    public static final byte LYING = 1;
    public static final byte STUCK = 2;
    public static final byte HANGING = 3;
    public static final byte BACK = 4;
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
    // The entity id of the Thor it belongs to, how it rests (OUT, LYING, ...) and the face of the block it is in.
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(ThrownHammer.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> REST = SynchedEntityData.defineId(ThrownHammer.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FACE = SynchedEntityData.defineId(ThrownHammer.class,
            EntityDataSerializers.BYTE);

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

    public int owner() {
        return this.entityData.get(OWNER);
    }

    void setOwner(int owner) {
        this.entityData.set(OWNER, owner);
    }

    public byte rest() {
        return this.entityData.get(REST);
    }

    public boolean resting() {
        byte rest = this.rest();
        return rest == LYING || rest == STUCK || rest == HANGING;
    }

    void setRest(byte rest) {
        this.entityData.set(REST, rest);
    }

    public Direction face() {
        return Direction.from3DDataValue(this.entityData.get(FACE));
    }

    void setFace(Direction face) {
        this.entityData.set(FACE, (byte) face.get3DDataValue());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 1.0F);
        builder.define(CHARGED, false);
        builder.define(OWNER, -1);
        builder.define(REST, OUT);
        builder.define(FACE, (byte) Direction.UP.get3DDataValue());
    }

    // Nothing moves it but its Thor: no blow, blast or push.
    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
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
