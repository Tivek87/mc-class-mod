package nl.tivek.multiversepowers.creature.animal.raccoon;

import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.creature.animal.bear.BrownBear;

// The raccoon of woods and swamps: a hunched, grizzled grey creature with a black mask and a bushy ringed tail, alone
// or a mother with her kits. It keeps a little way off from players (not one who creeps up or holds out berries or an
// egg) and runs from wolves and bears. At rest it sits up on its haunches to look round, rubs its forepaws together as
// if washing them, noses through the ground for food, and scratches itself with a hind foot.
public final class Raccoon extends WildAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Raccoon>> TYPE = TYPES.register("raccoon",
            () -> EntityType.Builder.<Raccoon>of(Raccoon::new, MobCategory.CREATURE).sized(0.6F, 0.65F)
                    .eyeHeight(0.5F).clientTrackingRange(10).build("raccoon"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "raccoon_food"));
    public static final int SIT_UP = 1;
    public static final int WASH = 2;
    public static final int FORAGE = 3;
    public static final int SCRATCH = 4;
    private static final SoundEvent AMBIENT = Sounds.of("raccoon.ambient");
    private static final SoundEvent HURT = Sounds.of("raccoon.hurt");
    private static final SoundEvent DEATH = Sounds.of("raccoon.death");
    private static final SoundEvent EAT = Sounds.of("raccoon.eat");
    private static final SoundEvent SNIFF = Sounds.of("raccoon.sniff");

    public Raccoon(EntityType<? extends Raccoon> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Raccoon::onAttributes);
        modEventBus.addListener(Raccoon::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 12.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return SCRATCH;
    }

    @Override
    protected int fade(int kind) {
        return kind == SIT_UP ? 8 : 5;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 6.0F, 1.3, 1.9, this::shiesFrom));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Wolf.class, 10.0F, 1.5, 2.0));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, BrownBear.class, 10.0F, 1.5, 2.0));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), true));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(6, new IdleActs(this, 140).add(SIT_UP, 50, 110, 3).add(WASH, 50, 90, 2)
                .add(FORAGE, 60, 140, 3).add(SCRATCH, 30, 50, 1));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    private boolean shiesFrom(LivingEntity living) {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && !living.isDiscrete()
                && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.act() == FORAGE && this.tickCount % 24 == 0) {
            this.playSound(SNIFF, 0.5F, 1.25F);
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return TYPE.get().create(level);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.6F)
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

    // A raccoon chitters higher than a fox.
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.2F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }
}
