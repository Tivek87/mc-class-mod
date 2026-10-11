package nl.tivek.multiversepowers.creature.stalker;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The Stalker: a tall, gaunt thing with bark-dark skin and long claws that hunts a player through the woods without
// ever being seen. Looked at, it slips behind a tree trunk, folds itself small inside a bush or blinks away; unseen it
// may take the shape of an animal grazing near by and wander closer as one; and once it is close behind someone who is
// not looking it lunges, rends them and is gone again (StalkerMoves).
public final class Stalker extends MovingMonster {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Stalker>> TYPE = TYPES.register("stalker",
            () -> EntityType.Builder.<Stalker>of(Stalker::new, MobCategory.MONSTER).sized(0.6F, 2.3F)
                    .eyeHeight(2.05F).clientTrackingRange(10).build("stalker"));
    public static final int OPEN = 0;
    // Pressed flat behind a tree trunk.
    public static final int TRUNK = 1;
    // Folded small inside a bush.
    public static final int BUSH = 2;
    private static final EntityDimensions FOLDED = EntityDimensions.scalable(0.7F, 0.8F).withEyeHeight(0.6F);
    private static final EntityDataAccessor<String> DISGUISE = SynchedEntityData.defineId(Stalker.class,
            EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> HIDING = SynchedEntityData.defineId(Stalker.class,
            EntityDataSerializers.INT);
    @Nullable
    private StalkerMoves moves;
    @Nullable
    private Entity shape;

    public Stalker(EntityType<? extends Stalker> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Stalker::onAttributes);
        modEventBus.addListener(Stalker::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2).add(Attributes.FOLLOW_RANGE, 48.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return StalkerMoves.KINDS;
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISGUISE, "");
        builder.define(HIDING, OPEN);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Disguise", this.entityData.get(DISGUISE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DISGUISE, tag.getString("Disguise"));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.moves = new StalkerMoves(this);
        this.goalSelector.addGoal(1, this.moves);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false) {
            {
                // It knows where its prey is through the trees.
                this.targetConditions.ignoreLineOfSight();
            }
        });
    }

    // The animal whose shape it wears, if any.
    @Nullable
    public EntityType<?> disguise() {
        String id = this.entityData.get(DISGUISE);
        if (id.isEmpty()) {
            return null;
        }
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(id));
        return type.orElse(null);
    }

    void disguise(@Nullable EntityType<?> type) {
        this.entityData.set(DISGUISE, type == null ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    // In each game that sees it: the animal it looks like, never in the world, kept where it is and moving as it does,
    // so its renderer draws that instead.
    @Nullable
    public Entity shape() {
        return this.shape;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.followShape();
        }
    }

    private void followShape() {
        EntityType<?> type = this.disguise();
        if (type == null) {
            this.shape = null;
            return;
        }
        if (this.shape == null || this.shape.getType() != type) {
            this.shape = type.create(this.level());
            if (this.shape == null) {
                return;
            }
            this.shape.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        }
        Entity s = this.shape;
        s.setOldPosAndRot();
        s.setPos(this.getX(), this.getY(), this.getZ());
        s.setYRot(this.getYRot());
        s.setXRot(this.getXRot());
        s.tickCount = this.tickCount;
        s.setOnGround(this.onGround());
        if (s instanceof LivingEntity living) {
            living.yBodyRotO = living.yBodyRot;
            living.yBodyRot = this.yBodyRot;
            living.yHeadRotO = living.yHeadRot;
            living.yHeadRot = this.yHeadRot;
            living.walkAnimation.update(this.walkAnimation.speed(), 1.0F);
            living.hurtTime = this.hurtTime;
            living.hurtDuration = this.hurtDuration;
        }
    }

    public int hiding() {
        return this.entityData.get(HIDING);
    }

    void hide(int how) {
        if (this.hiding() != how) {
            this.entityData.set(HIDING, how);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (DISGUISE.equals(data) || HIDING.equals(data)) {
            this.refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityType<?> shape = this.disguise();
        if (shape != null) {
            return shape.getDimensions();
        }
        return this.hiding() == BUSH ? FOLDED : super.getDefaultDimensions(pose);
    }

    // Puffs of dark smoke where it changes or vanishes, as quiet as it can make them.
    void smoke(ServerLevel level, Vec3 at, int count) {
        ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.8, at.z, count, 0.35, 0.6, 0.35, 0.02);
        ParticleFx.send(level, ParticleTypes.SQUID_INK, at.x, at.y + 0.8, at.z, count / 2, 0.3, 0.5, 0.3, 0.02);
    }

    @Override
    public void die(DamageSource source) {
        // Its body is its own, whatever shape it wore.
        this.disguise(null);
        this.hide(OPEN);
        super.die(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.moves != null && this.isAlive()) {
            this.moves.hurt();
        }
        return hurt;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.disguise() == null && super.removeWhenFarAway(distance);
    }

    // Silent while it hunts.
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_SCREECH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public int getMaxHeadYRot() {
        return 60;
    }
}
