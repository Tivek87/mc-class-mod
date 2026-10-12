package nl.tivek.multiversepowers.creature.animal.hedgehog;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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

// The hedgehog of woods and meadows: a small round creature with a coat of spines, a pointed snout and short legs.
// It does not run: when a player walks up (not one who creeps up or holds out its food), a wolf or a fox comes near, or
// anything hurts it, it rolls up into a ball of spines, which halves a blow and pricks whoever strikes it by hand. At
// rest it lifts its nose to sniff the air, snuffles through the ground, and scratches itself.
public final class Hedgehog extends WildAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Hedgehog>> TYPE = TYPES.register("hedgehog",
            () -> EntityType.Builder.<Hedgehog>of(Hedgehog::new, MobCategory.CREATURE).sized(0.45F, 0.35F)
                    .eyeHeight(0.2F).clientTrackingRange(8).build("hedgehog"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "hedgehog_food"));
    public static final int CURL = 1;
    public static final int SNIFF = 2;
    public static final int FORAGE = 3;
    public static final int SCRATCH = 4;
    private static final SoundEvent AMBIENT = Sounds.of("hedgehog.ambient");
    private static final SoundEvent HURT = Sounds.of("hedgehog.hurt");
    private static final SoundEvent HURT_CURLED = Sounds.of("hedgehog.hurt_curled");
    private static final SoundEvent DEATH = Sounds.of("hedgehog.death");
    private static final SoundEvent CURL_UP = Sounds.of("hedgehog.curl");
    private static final SoundEvent UNCURL = Sounds.of("hedgehog.uncurl");
    private static final SoundEvent STEP = Sounds.of("hedgehog.step");
    private int scaredUntil;

    public Hedgehog(EntityType<? extends Hedgehog> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Hedgehog::onAttributes);
        modEventBus.addListener(Hedgehog::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18).add(Attributes.FOLLOW_RANGE, 10.0).build());
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
        return kind == CURL ? 7 : 5;
    }

    public boolean isCurled() {
        return this.act() == CURL;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.8, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new CurlGoal());
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(6, new IdleActs(this, 150).add(SNIFF, 40, 80, 3).add(FORAGE, 60, 160, 4)
                .add(SCRATCH, 30, 50, 1));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // What makes it roll up: a player walking up in the open, a wolf or a fox.
    private boolean threatened() {
        return !this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0, 1.5, 3.0),
                this::fears).isEmpty();
    }

    private boolean fears(LivingEntity living) {
        if (living instanceof Wolf || living instanceof Fox) {
            return true;
        }
        return living instanceof Player && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(living)
                && !living.isDiscrete() && !living.getMainHandItem().is(FOOD) && !living.getOffhandItem().is(FOOD);
    }

    // Rolled up, its spines take half of a blow and prick the hand that dealt it.
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean curled = this.isCurled();
        if (curled && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount *= 0.5F;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.scaredUntil = this.tickCount + 100;
            if (curled && source.getDirectEntity() instanceof LivingEntity attacker && source.isDirect()
                    && attacker.getMainHandItem().isEmpty()) {
                attacker.hurt(this.damageSources().thorns(this), 1.0F);
            }
        }
        return hurt;
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
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(0.3F)
                : group);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isCurled() ? null : AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isCurled() ? HURT_CURLED : HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DEATH;
    }

    // A hedgehog's snuffling is higher and softer than an armadillo's.
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.4F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.08F, 1.5F);
    }

    // Rolls up where it stands and stays so until what scared it has been gone a while.
    private final class CurlGoal extends Goal {
        private int until;

        CurlGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        private boolean scared() {
            return Hedgehog.this.tickCount < Hedgehog.this.scaredUntil
                    || Hedgehog.this.tickCount % 5 == 0 && Hedgehog.this.threatened();
        }

        @Override
        public boolean canUse() {
            return Hedgehog.this.onGround() && !Hedgehog.this.isInWater() && !Hedgehog.this.isOnFire()
                    && this.scared();
        }

        @Override
        public boolean canContinueToUse() {
            if (Hedgehog.this.isInWater() || Hedgehog.this.isOnFire()) {
                return false;
            }
            if (this.scared()) {
                this.until = Hedgehog.this.tickCount + 60;
            }
            return Hedgehog.this.tickCount < this.until;
        }

        @Override
        public void start() {
            this.until = Hedgehog.this.tickCount + 60;
            Hedgehog.this.getNavigation().stop();
            Hedgehog.this.start(CURL, 20 * 60 * 30);
            Hedgehog.this.playSound(CURL_UP, 0.6F, 1.5F);
        }

        @Override
        public void stop() {
            Hedgehog.this.end(CURL);
            Hedgehog.this.playSound(UNCURL, 0.6F, 1.5F);
        }
    }
}
