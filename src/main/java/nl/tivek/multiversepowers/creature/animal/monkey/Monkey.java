package nl.tivek.multiversepowers.creature.animal.monkey;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
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
import nl.tivek.multiversepowers.creature.animal.crocodile.Crocodile;
import nl.tivek.multiversepowers.creature.animal.lynx.Lynx;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The monkey of the jungle, a capuchin: small and quick, with long limbs and a long tail carried high, its tip hooked
// down; black with a cream face, chest and shoulders, or brown with a dark cap, limbs and tail. It lives in troops,
// the young among them, and does no harm: it keeps a few steps from players (not one who creeps up or holds out its
// food) and runs when hit. At rest it sits upright, scratches itself, eats with both hands, and stands up on its hind
// legs to hoot with its arms in the air.
public final class Monkey extends WildAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Monkey>> TYPE = TYPES.register("monkey",
            () -> EntityType.Builder.<Monkey>of(Monkey::new, MobCategory.CREATURE).sized(0.5F, 0.7F)
                    .eyeHeight(0.6F).clientTrackingRange(10).build("monkey"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "monkey_food"));
    public static final int WHITE_FACED = 0;
    public static final int BROWN = 1;
    public static final int SIT = 1;
    public static final int SCRATCH = 2;
    public static final int EAT = 3;
    public static final int HOOT = 4;
    public static final int HOOT_END = 40;
    private static final SoundEvent AMBIENT = Sounds.of("monkey.ambient");
    private static final SoundEvent HOOTS = Sounds.of("monkey.hoot");
    private static final SoundEvent HURT = Sounds.of("monkey.hurt");
    private static final SoundEvent DEATH = Sounds.of("monkey.death");
    private static final SoundEvent EATS = Sounds.of("monkey.eat");

    public Monkey(EntityType<? extends Monkey> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Monkey::onAttributes);
        modEventBus.addListener(Monkey::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 12.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monkey::checkMonkeySpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // On the jungle floor, which its trees keep too dark for the game's own animals: no light is asked.
    private static boolean checkMonkeySpawnRules(EntityType<Monkey> type, LevelAccessor level, MobSpawnType spawn,
            BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON);
    }

    @Override
    protected int kinds() {
        return HOOT;
    }

    @Override
    protected int fade(int kind) {
        return kind == SIT ? 8 : 5;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 3.5F, 1.2, 1.7, this::shiesFrom));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Crocodile.class, 8.0F, 1.4, 1.9));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Lynx.class, 8.0F, 1.4, 1.9));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(6, new IdleActs(this, 90).add(SIT, 60, 160, 3).add(SCRATCH, 30, 50, 2)
                .add(EAT, 60, 100, 2).add(HOOT, HOOT_END, HOOT_END, 2));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    private boolean shiesFrom(LivingEntity living) {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && !living.isDiscrete()
                && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    @Override
    public void start(int kind, int ticks) {
        super.start(kind, ticks);
        if (kind == HOOT) {
            this.playSound(HOOTS, 1.0F, this.getVoicePitch());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.act() == EAT && this.tickCount % 12 == 0) {
            this.playSound(EATS, 0.3F, 1.5F);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        Monkey young = TYPE.get().create(level);
        if (young != null) {
            young.setVariant(other instanceof Monkey mate && this.random.nextBoolean() ? mate.variant()
                    : this.variant());
        }
        return young;
    }

    // A troop is all of one kind.
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawn, group == null
                ? new Troop(this.random.nextBoolean() ? WHITE_FACED : BROWN) : group);
        this.setVariant(data instanceof Troop troop ? troop.variant : this.random.nextInt(2));
        return data;
    }

    private static final class Troop extends AgeableMob.AgeableMobGroupData {
        private final int variant;

        Troop(int variant) {
            super(0.25F);
            this.variant = variant;
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
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

    @Override
    public SoundEvent getEatingSound(ItemStack stack) {
        return EATS;
    }

    // A monkey's shriek is higher than a fox's.
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.25F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.7F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
    }
}
