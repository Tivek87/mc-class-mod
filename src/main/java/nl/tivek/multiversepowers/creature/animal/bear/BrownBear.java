package nl.tivek.multiversepowers.creature.animal.bear;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.animal.IdleActs;
import nl.tivek.multiversepowers.creature.animal.NeutralAnimal;
import nl.tivek.multiversepowers.creature.animal.WildAnimal;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The brown bear of the northern forests and taiga: a heavy, humped bear that roams on its own or a sow with her cub.
// It leaves players be until one hits it or comes near its cub; then it stands up and roars, charges and mauls. At rest
// it sits back on its rump, rears up to sniff the wind, lies down with its head on its paws, and shakes itself dry
// after a swim. Sweet berries, honeycomb and fish tempt it and breed it.
public final class BrownBear extends NeutralAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<BrownBear>> TYPE = TYPES.register("brown_bear",
            () -> EntityType.Builder.<BrownBear>of(BrownBear::new, MobCategory.CREATURE).sized(1.3F, 1.35F)
                    .eyeHeight(1.0F).clientTrackingRange(10).build("brown_bear"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "bear_food"));
    public static final int SIT = 1;
    public static final int REAR = 2;
    public static final int REST = 3;
    public static final int SHAKE = 4;
    public static final int ROAR = 5;
    public static final int MAUL = 6;
    public static final int SHAKE_END = 40;
    public static final int ROAR_END = 30;
    public static final int MAUL_END = 18;
    // When the swing of a maul lands.
    public static final int MAUL_HIT = 8;
    private static final double REACH = 0.7;
    private static final SoundEvent AMBIENT = Sounds.of("brown_bear.ambient");
    private static final SoundEvent AMBIENT_BABY = Sounds.of("brown_bear.ambient_baby");
    private static final SoundEvent HURT = Sounds.of("brown_bear.hurt");
    private static final SoundEvent DEATH = Sounds.of("brown_bear.death");
    private static final SoundEvent WARNING = Sounds.of("brown_bear.warning");
    private static final SoundEvent STEP = Sounds.of("brown_bear.step");
    private boolean wet;
    private int warned;

    public BrownBear(EntityType<? extends BrownBear> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(BrownBear::onAttributes);
        modEventBus.addListener(BrownBear::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.25)
                .add(Attributes.STEP_HEIGHT, 1.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WildAnimal::checkWildSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return MAUL;
    }

    @Override
    protected int fade(int kind) {
        return switch (kind) {
            case SIT, REST -> 14;
            case REAR -> 10;
            default -> 4;
        };
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MaulGoal());
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0,
                bear -> bear.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(5, new IdleActs(this, 220).add(SIT, 80, 220, 3).add(REAR, 50, 80, 2)
                .add(REST, 240, 600, 2, bear -> bear.level().isNight() || bear.getRandom().nextInt(3) == 0));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new GuardGoal());
        this.targetSelector.addGoal(2, new CubGoal());
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                this::isAngryAt));
        this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
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
        // A second bear in a group is the first one's cub.
        return super.finalizeSpawn(level, difficulty, spawn, group == null ? new AgeableMob.AgeableMobGroupData(1.0F)
                : group);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.warned > 0) {
            this.warned--;
        }
        if (this.level().isClientSide) {
            this.drip();
            return;
        }
        // Out of the water onto dry land it shakes the water out of its coat.
        if (this.isInWater()) {
            this.wet = true;
        } else if (this.act() == SHAKE) {
            this.getNavigation().stop();
        } else if (this.wet && this.onGround() && this.act() == 0 && this.getTarget() == null) {
            this.wet = false;
            this.getNavigation().stop();
            this.start(SHAKE, SHAKE_END);
        }
    }

    // Drops flung from its coat while it shakes.
    private void drip() {
        double t = this.actAge(0.0F);
        if (this.act() != SHAKE || t < 6 || t > SHAKE_END - 8) {
            return;
        }
        float width = this.getBbWidth() * 0.5F;
        for (int k = 0; k < 3; k++) {
            float a = this.random.nextFloat() * Mth.TWO_PI;
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + Mth.cos(a) * width,
                    this.getY() + this.getBbHeight() * (0.4 + this.random.nextFloat() * 0.4),
                    this.getZ() + Mth.sin(a) * width, 0.0, 0.0, 0.0);
        }
    }

    // A swipe of its long forelegs reaches further than the game's reach, and a bear this wide stops its charge a
    // block short of what it chases.
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(REACH, 0.0, REACH);
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }

    @Override
    protected int getBaseExperienceReward() {
        return this.isBaby() ? 0 : 3 + this.random.nextInt(3);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? AMBIENT_BABY : AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DEATH;
    }

    // A brown bear's voice is deeper than a polar bear's.
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (this.isBaby() ? 1.0F : 0.82F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.15F, 0.85F);
    }

    private void roar() {
        if (this.warned <= 0) {
            this.makeSound(WARNING);
            this.warned = 40;
        }
    }

    // Charges, and when its foe is near swings a paw down at it. Seeing its foe first from a little way off it
    // stands up and roars before it comes on.
    private final class MaulGoal extends MeleeAttackGoal {
        private int roaring;
        private int swing;

        MaulGoal() {
            super(BrownBear.this, 1.35, true);
        }

        @Override
        public void start() {
            super.start();
            LivingEntity target = BrownBear.this.getTarget();
            if (target != null && BrownBear.this.distanceToSqr(target) > 25.0 && BrownBear.this.random.nextInt(3) != 0) {
                this.roaring = ROAR_END;
                BrownBear.this.getNavigation().stop();
                BrownBear.this.start(ROAR, ROAR_END);
                BrownBear.this.roar();
            }
        }

        @Override
        public void tick() {
            LivingEntity target = BrownBear.this.getTarget();
            if (this.roaring > 0) {
                this.roaring--;
                if (target != null) {
                    BrownBear.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
                return;
            }
            if (this.swing > 0 && --this.swing == 0 && target != null && BrownBear.this.isWithinMeleeAttackRange(target)
                    && BrownBear.this.getSensing().hasLineOfSight(target)) {
                BrownBear.this.doHurtTarget(target);
            }
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.swing == 0 && this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.swing = MAUL_HIT;
                BrownBear.this.start(MAUL, MAUL_END);
                BrownBear.this.roar();
            }
        }

        @Override
        public void stop() {
            this.roaring = 0;
            this.swing = 0;
            BrownBear.this.end(ROAR);
            super.stop();
        }
    }

    private boolean nearCub() {
        for (BrownBear bear : this.level().getEntitiesOfClass(BrownBear.class, this.getBoundingBox().inflate(8.0, 4.0,
                8.0))) {
            if (bear.isBaby()) {
                return true;
            }
        }
        return false;
    }

    // Hit, it turns on whoever hit it; a cub that is hit calls the grown bears near it instead.
    private final class GuardGoal extends HurtByTargetGoal {
        GuardGoal() {
            super(BrownBear.this);
        }

        @Override
        public void start() {
            super.start();
            if (BrownBear.this.isBaby()) {
                this.alertOthers();
                this.stop();
            }
        }

        @Override
        protected void alertOther(Mob mob, LivingEntity target) {
            if (mob instanceof BrownBear && !mob.isBaby()) {
                super.alertOther(mob, target);
            }
        }
    }

    // A grown bear with a cub near it goes for any player who comes close. The cub is asked in the target's own test,
    // so the Mobs page (MobDefaults) reads the bear as neutral, as it is without a cub.
    private final class CubGoal extends NearestAttackableTargetGoal<Player> {
        CubGoal() {
            super(BrownBear.this, Player.class, 20, true, true, player -> BrownBear.this.nearCub());
        }

        @Override
        public boolean canUse() {
            return !BrownBear.this.isBaby() && super.canUse();
        }

        @Override
        protected double getFollowDistance() {
            return super.getFollowDistance() * 0.5;
        }
    }
}
