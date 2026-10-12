package nl.tivek.multiversepowers.creature.deer;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.IdleActs;
import nl.tivek.multiversepowers.creature.animal.WildAnimal;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.creature.bear.BrownBear;

// The deer of forest, taiga and meadow, in small herds: a stag with antlers or a doe without, and spotted fawns. Shy,
// it bounds away from a player who comes near on foot (not one who creeps up, or holds out wheat, an apple or a
// carrot), and from wolves and bears, its white tail raised. At rest it grazes, stands alert with its ears forward and
// stamps a forefoot, grooms its flank, or lies down with its legs folded under it.
public final class Deer extends WildAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Deer>> TYPE = TYPES.register("deer",
            () -> EntityType.Builder.<Deer>of(Deer::new, MobCategory.CREATURE).sized(0.9F, 1.6F).eyeHeight(1.45F)
                    .clientTrackingRange(10).build("deer"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "deer_food"));
    public static final int GRAZE = 1;
    public static final int ALERT = 2;
    public static final int GROOM = 3;
    public static final int BED = 4;
    // When an alert deer stamps its forefoot, in ticks from the start.
    public static final int STAMP = 14;
    private static final EntityDataAccessor<Boolean> STAG = SynchedEntityData.defineId(Deer.class,
            EntityDataSerializers.BOOLEAN);
    private static final SoundEvent AMBIENT = Sounds.of("deer.ambient");
    private static final SoundEvent HURT = Sounds.of("deer.hurt");
    private static final SoundEvent DEATH = Sounds.of("deer.death");
    private static final SoundEvent SNORT = Sounds.of("deer.snort");
    private static final SoundEvent STEP = Sounds.of("deer.step");

    public Deer(EntityType<? extends Deer> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Deer::onAttributes);
        modEventBus.addListener(Deer::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.STEP_HEIGHT, 1.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WildAnimal::checkWildSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return BED;
    }

    @Override
    protected int fade(int kind) {
        return switch (kind) {
            case GRAZE -> 12;
            case BED -> 22;
            default -> 7;
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAG, false);
    }

    // A stag, with antlers, or a doe; a fawn grows its antlers only once grown, if it is a stag.
    public boolean isStag() {
        return this.entityData.get(STAG);
    }

    public void setStag(boolean stag) {
        this.entityData.set(STAG, stag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Stag", this.isStag());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setStag(tag.getBoolean("Stag"));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.3));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.7, 2.3, this::shiesFrom));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Wolf.class, 12.0F, 1.8, 2.3));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, BrownBear.class, 14.0F, 1.8, 2.3));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, stack -> stack.is(FOOD), true));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(6, new IdleActs(this, 160).add(GRAZE, 80, 240, 6, Deer::onGrass)
                .add(ALERT, 40, 70, 2, Deer::playerNear).add(GROOM, 40, 70, 2).add(BED, 300, 800, 1));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // A player walking up in the open; one creeping or holding out food may come close.
    private boolean shiesFrom(LivingEntity living) {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && !living.isDiscrete()
                && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    private static boolean onGrass(WildAnimal deer) {
        return deer.level().getBlockState(deer.blockPosition().below()).is(Blocks.GRASS_BLOCK);
    }

    private static boolean playerNear(WildAnimal deer) {
        return deer.level().getNearestPlayer(deer, 18.0) != null;
    }

    @Override
    public void start(int kind, int ticks) {
        super.start(kind, ticks);
        if (kind == ALERT) {
            this.playSound(SNORT, 0.6F, 1.5F + this.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.nibble();
        }
    }

    // Bits of grass at its mouth now and then while it grazes.
    private void nibble() {
        double t = this.actAge(0.0F);
        if (this.act() != GRAZE || t < 12 || this.tickCount % 9 != 0) {
            return;
        }
        BlockPos below = this.blockPosition().below();
        BlockState ground = this.level().getBlockState(below);
        if (!ground.is(Blocks.GRASS_BLOCK)) {
            return;
        }
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double reach = this.isBaby() ? 0.45 : 0.9;
        this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                this.getX() - Mth.sin(yaw) * reach, this.getY() + 0.1, this.getZ() + Mth.cos(yaw) * reach,
                0.0, 0.05, 0.0);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        Deer fawn = TYPE.get().create(level);
        if (fawn != null) {
            fawn.setStag(this.random.nextBoolean());
        }
        return fawn;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        this.setStag(this.random.nextInt(5) < 2);
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.25F)
                : group);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 320;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DEATH;
    }

    // A deer's bleat is higher and softer than a goat's, a fawn's higher still.
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.25F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.12F, 1.1F);
    }
}
