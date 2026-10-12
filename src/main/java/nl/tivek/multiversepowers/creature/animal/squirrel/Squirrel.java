package nl.tivek.multiversepowers.creature.animal.squirrel;

import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.IdleActs;
import nl.tivek.multiversepowers.creature.animal.WildAnimal;
import nl.tivek.multiversepowers.creature.animal.owl.Owl;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The squirrel of the woods: red, with tufted ears, in taiga and spruce woods, grey everywhere else; a slight, quick
// creature that goes in bounds, its bushy tail curled up over its back. It darts off from a player walking up (not
// one who creeps up or holds out seeds), and from wolves, foxes and owls. At rest it sits up to look round, nibbles at
// something held in its forepaws, digs to bury a nut, and flicks its tail and chatters at a player it has seen.
public final class Squirrel extends WildAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Squirrel>> TYPE = TYPES.register("squirrel",
            () -> EntityType.Builder.<Squirrel>of(Squirrel::new, MobCategory.CREATURE).sized(0.4F, 0.45F)
                    .eyeHeight(0.3F).clientTrackingRange(8).build("squirrel"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "squirrel_food"));
    public static final int RED = 0;
    public static final int GREY = 1;
    public static final int SIT_UP = 1;
    public static final int NIBBLE = 2;
    public static final int DIG = 3;
    public static final int FLICK = 4;
    private static final SoundEvent AMBIENT = Sounds.of("squirrel.ambient");
    private static final SoundEvent HURT = Sounds.of("squirrel.hurt");
    private static final SoundEvent DEATH = Sounds.of("squirrel.death");
    private static final SoundEvent CHATTER = Sounds.of("squirrel.chatter");
    private static final SoundEvent EAT = Sounds.of("squirrel.eat");

    public Squirrel(EntityType<? extends Squirrel> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Squirrel::onAttributes);
        modEventBus.addListener(Squirrel::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 12.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WildAnimal::checkWildSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return FLICK;
    }

    @Override
    protected int fade(int kind) {
        return 4;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 7.0F, 1.4, 1.9, this::shiesFrom));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Wolf.class, 10.0F, 1.5, 2.0));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Fox.class, 10.0F, 1.5, 2.0));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Owl.class, 8.0F, 1.5, 2.0));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), true));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(6, new IdleActs(this, 110).add(SIT_UP, 40, 90, 3).add(NIBBLE, 60, 120, 3)
                .add(DIG, 40, 70, 2).add(FLICK, 30, 50, 2, Squirrel::playerNear));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    private boolean shiesFrom(LivingEntity living) {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && !living.isDiscrete()
                && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    private static boolean playerNear(WildAnimal squirrel) {
        return squirrel.level().getNearestPlayer(squirrel, 14.0) != null;
    }

    @Override
    public void start(int kind, int ticks) {
        super.start(kind, ticks);
        if (kind == FLICK) {
            this.playSound(CHATTER, 0.5F, 1.5F + this.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.act() == NIBBLE && this.tickCount % 14 == 0) {
            this.playSound(EAT, 0.25F, 1.7F);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        Squirrel kit = TYPE.get().create(level);
        if (kit != null) {
            kit.setVariant(other instanceof Squirrel mate && this.random.nextBoolean() ? mate.variant()
                    : this.variant());
        }
        return kit;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        this.setVariant(level.getBiome(this.blockPosition()).is(BiomeTags.IS_TAIGA) ? RED : GREY);
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.2F)
                : group);
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
        return EAT;
    }

    // A squirrel squeaks higher than a rabbit.
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.5F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }
}
