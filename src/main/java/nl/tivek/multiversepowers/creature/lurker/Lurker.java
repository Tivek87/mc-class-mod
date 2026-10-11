package nl.tivek.multiversepowers.creature.lurker;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MovingMonster;

// The Lurker: an unnaturally tall, gaunt zombie that runs on all fours, ash-black, its pupils glowing white, its
// fingers grown into long bone claws. It creeps up unheard while it is not looked at, pounces at the head from 6 blocks,
// darts aside in a puff of black smoke when stared at or aimed at, and its claws leave wounds that bleed on, deeper with
// each tear, even through a shield (LurkerMoves).
public final class Lurker extends MovingMonster implements Crawler {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Lurker>> TYPE = TYPES.register("lurker",
            () -> EntityType.Builder.<Lurker>of(Lurker::new, MobCategory.MONSTER).sized(0.9F, 1.3F)
                    .eyeHeight(1.0F).clientTrackingRange(10).build("lurker"));
    private static final EntityDataAccessor<Boolean> STALKING = SynchedEntityData.defineId(Lurker.class,
            EntityDataSerializers.BOOLEAN);

    // Set when something shot at it: it darts aside at its next chance.
    boolean shotAt;

    public Lurker(EntityType<? extends Lurker> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Lurker::onAttributes);
        modEventBus.addListener(Lurker::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.STEP_HEIGHT, 1.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return LurkerMoves.KINDS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STALKING, false);
    }

    @Override
    public boolean stalking() {
        return this.entityData.get(STALKING);
    }

    void stalk(boolean stalking) {
        if (this.stalking() != stalking) {
            this.entityData.set(STALKING, stalking);
        }
    }

    @Override
    public float risen(float partialTick) {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LurkerMoves(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            this.shotAt = true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    public float getVoicePitch() {
        return 1.3F + this.random.nextFloat() * 0.15F;
    }

    // Creeping up it makes no sound at all.
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.stalking() ? null : SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!this.stalking()) {
            this.playSound(SoundEvents.SPIDER_STEP, 0.25F, 0.7F);
        }
    }
}
