package nl.tivek.multiversepowers.creature.animal.giraffe;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The giraffe of the savanna: the tallest animal there is, over four blocks to the horns on its head, its back
// sloping down from high shoulders, its coat a net of pale lines round chestnut patches. Small herds, sometimes with
// a calf. It does no one harm: it keeps a few steps off (not from one who creeps up or holds out its food), and hit
// it kicks out behind and bolts. At rest it reaches up to pull leaves down with its long dark tongue, spreads its
// forelegs wide to get its head down to the ground and drink, and stands chewing.
public final class Giraffe extends WildAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Giraffe>> TYPE = TYPES.register("giraffe",
            () -> EntityType.Builder.<Giraffe>of(Giraffe::new, MobCategory.CREATURE).sized(1.5F, 4.2F)
                    .eyeHeight(3.9F).clientTrackingRange(10).build("giraffe"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "giraffe_food"));
    public static final int BROWSE = 1;
    public static final int DRINK = 2;
    public static final int CHEW = 3;
    public static final int KICK = 4;
    public static final int KICK_END = 14;
    // When the hooves of a kick fly out.
    public static final int KICK_HIT = 5;
    private static final float KICK_DAMAGE = 6.0F;
    private static final SoundEvent AMBIENT = Sounds.of("giraffe.ambient");
    private static final SoundEvent HURT = Sounds.of("giraffe.hurt");
    private static final SoundEvent DEATH = Sounds.of("giraffe.death");
    private static final SoundEvent EATS = Sounds.of("giraffe.eat");
    private static final SoundEvent KICKS = Sounds.of("giraffe.kick");
    private static final SoundEvent STEP = Sounds.of("giraffe.step");
    @Nullable
    private LivingEntity kicking;
    private int kickAt;

    public Giraffe(EntityType<? extends Giraffe> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Giraffe::onAttributes);
        modEventBus.addListener(Giraffe::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.FOLLOW_RANGE, 16.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return KICK;
    }

    @Override
    protected int fade(int kind) {
        return switch (kind) {
            case BROWSE -> 16;
            case DRINK -> 28;
            case KICK -> 3;
            default -> 10;
        };
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.9));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5.0F, 1.0, 1.4, this::shiesFrom));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new IdleActs(this, 110).add(BROWSE, 100, 240, 4).add(DRINK, 100, 160, 1)
                .add(CHEW, 80, 160, 3));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    private boolean shiesFrom(LivingEntity living) {
        return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living) && !living.isDiscrete()
                && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    // Hit from close by, a grown giraffe kicks out at whoever did it as it bolts.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && !this.isBaby() && this.isAlive()
                && source.getEntity() instanceof LivingEntity attacker && this.distanceToSqr(attacker) < 12.0
                && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(attacker)) {
            this.kicking = attacker;
            this.kickAt = this.tickCount + KICK_HIT;
            this.start(KICK, KICK_END);
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity foe = this.kicking;
        if (foe != null && this.tickCount >= this.kickAt) {
            this.kicking = null;
            if (this.isAlive() && foe.isAlive() && this.distanceToSqr(foe) < 16.0
                    && foe.hurt(this.damageSources().mobAttack(this), KICK_DAMAGE)) {
                foe.knockback(1.0, this.getX() - foe.getX(), this.getZ() - foe.getZ());
                this.playSound(KICKS, 1.0F, 0.9F);
            }
        }
        if ((this.act() == BROWSE || this.act() == CHEW) && this.tickCount % 16 == 0) {
            this.playSound(EATS, 0.3F, 0.8F);
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
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.25F)
                : group);
    }

    @Override
    protected int getBaseExperienceReward() {
        return this.isBaby() ? 0 : 2 + this.random.nextInt(3);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
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

    // A giraffe's voice is far lower than a llama's.
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (this.isBaby() ? 0.9F : 0.6F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.2F, 0.8F);
    }
}
