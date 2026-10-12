package nl.tivek.multiversepowers.creature.animal.moose;

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
import net.minecraft.world.entity.Entity;
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

// The moose of the taiga: the biggest animal of the woods, taller at its humped shoulder than a player, on long pale
// legs, with a long drooping nose and a bell of skin under its throat; a bull carries broad, flat antlers, a cow none,
// and a cow may have her calf with her. It leaves players be until one hits it or comes near a calf; then it lays
// back its ears, lowers its head and snorts, charges, rears and stamps down with both forefeet. At rest it browses
// on leaves held high, lowers its head to the ground on spread forelegs, lies down, and shakes itself dry after a
// swim.
public final class Moose extends NeutralAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Moose>> TYPE = TYPES.register("moose",
            () -> EntityType.Builder.<Moose>of(Moose::new, MobCategory.CREATURE).sized(1.5F, 2.1F).eyeHeight(1.9F)
                    .clientTrackingRange(10).build("moose"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "moose_food"));
    public static final int COW = 0;
    public static final int BULL = 1;
    public static final int BROWSE = 1;
    public static final int GRAZE = 2;
    public static final int BED = 3;
    public static final int SHAKE = 4;
    public static final int THREAT = 5;
    public static final int STOMP = 6;
    public static final int SHAKE_END = 40;
    public static final int THREAT_END = 34;
    public static final int STOMP_END = 20;
    // When the forefeet of a stomp come down.
    public static final int STOMP_HIT = 9;
    private static final double REACH = 0.6;
    private static final SoundEvent AMBIENT = Sounds.of("moose.ambient");
    private static final SoundEvent HURT = Sounds.of("moose.hurt");
    private static final SoundEvent DEATH = Sounds.of("moose.death");
    private static final SoundEvent SNORT = Sounds.of("moose.snort");
    private static final SoundEvent STOMPS = Sounds.of("moose.stomp");
    private static final SoundEvent STEP = Sounds.of("moose.step");
    private boolean wet;

    public Moose(EntityType<? extends Moose> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Moose::onAttributes);
        modEventBus.addListener(Moose::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0).add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.STEP_HEIGHT, 1.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WildAnimal::checkWildSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return STOMP;
    }

    @Override
    protected int fade(int kind) {
        return switch (kind) {
            case BROWSE, GRAZE -> 14;
            case BED -> 26;
            case THREAT -> 8;
            default -> 4;
        };
    }

    public boolean isBull() {
        return this.variant() == BULL;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new StompGoal());
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0,
                moose -> moose.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(5, new IdleActs(this, 200).add(BROWSE, 80, 200, 4).add(GRAZE, 80, 200, 2)
                .add(BED, 300, 700, 1));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new GuardGoal());
        this.targetSelector.addGoal(2, new CalfGoal());
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
        Moose calf = TYPE.get().create(level);
        if (calf != null) {
            calf.setVariant(this.random.nextBoolean() ? BULL : COW);
        }
        return calf;
    }

    // A second moose in a group is a calf, and the grown one with it then its mother.
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawn, group == null ? new Herd() : group);
        this.setVariant(this.random.nextBoolean() ? BULL : COW);
        if (data instanceof Herd herd) {
            if (!this.isBaby() && herd.first == null) {
                herd.first = this;
            } else if (this.isBaby() && herd.first != null) {
                herd.first.setVariant(COW);
            }
        }
        return data;
    }

    private static final class Herd extends AgeableMob.AgeableMobGroupData {
        @Nullable
        private Moose first;

        Herd() {
            super(1.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
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
        for (int k = 0; k < 4; k++) {
            float a = this.random.nextFloat() * Mth.TWO_PI;
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + Mth.cos(a) * width,
                    this.getY() + this.getBbHeight() * (0.5 + this.random.nextFloat() * 0.4),
                    this.getZ() + Mth.sin(a) * width, 0.0, 0.0, 0.0);
        }
    }

    // Its forefeet strike out further than the game's reach, and a body this wide stops its charge a block short of
    // what it chases.
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(REACH, 0.0, REACH);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.playSound(STOMPS, 1.0F, 0.8F);
        }
        return hit;
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
    public int getAmbientSoundInterval() {
        return 360;
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

    // A moose's moan is deeper than a camel's.
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (this.isBaby() ? 1.0F : 0.75F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.2F, 0.8F);
    }

    private void snort() {
        this.playSound(SNORT, 1.0F, 0.7F + this.random.nextFloat() * 0.1F);
    }

    // Charges, and when its foe is near rears and stamps down on it. Seeing its foe first from a little way off it
    // lowers its head, lays its ears back and snorts before it comes on.
    private final class StompGoal extends MeleeAttackGoal {
        private int warning;
        private int swing;

        StompGoal() {
            super(Moose.this, 1.5, true);
        }

        @Override
        public boolean canUse() {
            return !Moose.this.isBaby() && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            LivingEntity target = Moose.this.getTarget();
            if (target != null && Moose.this.distanceToSqr(target) > 16.0) {
                this.warning = THREAT_END;
                Moose.this.getNavigation().stop();
                Moose.this.start(THREAT, THREAT_END);
                Moose.this.snort();
            }
        }

        @Override
        public void tick() {
            LivingEntity target = Moose.this.getTarget();
            if (this.warning > 0) {
                this.warning--;
                if (target != null) {
                    Moose.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
                return;
            }
            if (this.swing > 0 && --this.swing == 0 && target != null && Moose.this.isWithinMeleeAttackRange(target)
                    && Moose.this.getSensing().hasLineOfSight(target)) {
                Moose.this.doHurtTarget(target);
            }
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.swing == 0 && this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.swing = STOMP_HIT;
                Moose.this.start(STOMP, STOMP_END);
            }
        }

        @Override
        public void stop() {
            this.warning = 0;
            this.swing = 0;
            Moose.this.end(THREAT);
            super.stop();
        }
    }

    private boolean nearCalf() {
        for (Moose moose : this.level().getEntitiesOfClass(Moose.class, this.getBoundingBox().inflate(8.0, 4.0,
                8.0))) {
            if (moose.isBaby()) {
                return true;
            }
        }
        return false;
    }

    // Hit, it turns on whoever hit it; a calf that is hit calls the grown moose near it instead.
    private final class GuardGoal extends HurtByTargetGoal {
        GuardGoal() {
            super(Moose.this);
        }

        @Override
        public void start() {
            super.start();
            if (Moose.this.isBaby()) {
                this.alertOthers();
                this.stop();
            }
        }

        @Override
        protected void alertOther(Mob mob, LivingEntity target) {
            if (mob instanceof Moose && !mob.isBaby()) {
                super.alertOther(mob, target);
            }
        }
    }

    // A grown moose with a calf near it goes for any player who comes close. The calf is asked in the target's own
    // test, so the Mobs page (MobDefaults) reads the moose as neutral, as it is without a calf.
    private final class CalfGoal extends NearestAttackableTargetGoal<Player> {
        CalfGoal() {
            super(Moose.this, Player.class, 20, true, true, player -> Moose.this.nearCalf());
        }

        @Override
        public boolean canUse() {
            return !Moose.this.isBaby() && super.canUse();
        }

        @Override
        protected double getFollowDistance() {
            return super.getFollowDistance() * 0.5;
        }
    }
}
