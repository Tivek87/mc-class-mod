package nl.tivek.multiversepowers.creature.animal.elephant;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
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
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The elephant of the savanna: the biggest animal on land, three blocks to the top of its head, grey and wrinkled, on
// legs like pillars, with great ears it fans itself with, a trunk that reaches the ground and tusks: long and curved
// on a bull, short on a cow, none yet on a calf. Small herds, a calf among them now and then. It leaves players be
// until one hits it or comes near a calf; then it spreads its ears wide, throws up its trunk and trumpets, comes on
// and tosses its foe with its tusks. At rest it pulls leaves down with its trunk, tears up grass and puts it in its
// mouth, and throws dust over its back.
public final class Elephant extends NeutralAnimal {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MultiversePowers.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<Elephant>> TYPE = TYPES.register("elephant",
            () -> EntityType.Builder.<Elephant>of(Elephant::new, MobCategory.CREATURE).sized(2.4F, 3.4F)
                    .eyeHeight(2.9F).clientTrackingRange(10).build("elephant"));
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "elephant_food"));
    public static final int COW = 0;
    public static final int BULL = 1;
    public static final int REACH = 1;
    public static final int GRAZE = 2;
    public static final int DUST = 3;
    public static final int THREAT = 4;
    public static final int STRIKE = 5;
    public static final int DUST_END = 56;
    // When the dust leaves the trunk, and when the last of it has.
    public static final int DUST_THROW = 26;
    public static final int DUST_OUT = 36;
    public static final int THREAT_END = 44;
    public static final int STRIKE_END = 20;
    // When the tusks of a strike come up.
    public static final int STRIKE_HIT = 8;
    private static final double ATTACK_REACH = 0.8;
    private static final SoundEvent AMBIENT = Sounds.of("elephant.ambient");
    private static final SoundEvent HURT = Sounds.of("elephant.hurt");
    private static final SoundEvent DEATH = Sounds.of("elephant.death");
    private static final SoundEvent TRUMPET = Sounds.of("elephant.trumpet");
    private static final SoundEvent STRIKES = Sounds.of("elephant.strike");
    private static final SoundEvent STEP = Sounds.of("elephant.step");

    public Elephant(EntityType<? extends Elephant> type, Level level) {
        super(type, level);
    }

    public static void register(IEventBus modEventBus) {
        TYPES.register(modEventBus);
        modEventBus.addListener(Elephant::onAttributes);
        modEventBus.addListener(Elephant::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.STEP_HEIGHT, 1.0).build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(TYPE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @Override
    protected int kinds() {
        return STRIKE;
    }

    @Override
    protected int fade(int kind) {
        return switch (kind) {
            case REACH, GRAZE -> 16;
            case DUST -> 10;
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
        this.goalSelector.addGoal(1, new ChargeGoal());
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.8,
                elephant -> elephant.isBaby() ? DamageTypeTags.PANIC_CAUSES
                        : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.is(FOOD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(5, new IdleActs(this, 160).add(REACH, 120, 240, 3).add(GRAZE, 120, 240, 3)
                .add(DUST, DUST_END, DUST_END, 2));
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
        Elephant calf = TYPE.get().create(level);
        if (calf != null) {
            calf.setVariant(this.random.nextBoolean() ? BULL : COW);
        }
        return calf;
    }

    // A herd is cows and their calves, led by the first of them; a bull comes alone or at the herd's edge.
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawn, @Nullable SpawnGroupData group) {
        boolean first = group == null;
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawn, first
                ? new AgeableMob.AgeableMobGroupData(0.3F) : group);
        this.setVariant(this.random.nextFloat() < (first ? 0.5F : 0.2F) ? BULL : COW);
        return data;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.dust();
        }
    }

    // The dust it throws over its back: the ground it stands on, falling from above its shoulders.
    private void dust() {
        double t = this.actAge(0.0F);
        if (this.act() != DUST || t < DUST_THROW || t > DUST_OUT) {
            return;
        }
        BlockState ground = this.level().getBlockState(this.getOnPos());
        if (ground.isAir() || ground.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        // Under grass it is earth that comes up, not the green of the blades.
        if (ground.is(BlockTags.DIRT)) {
            ground = Blocks.DIRT.defaultBlockState();
        }
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        float size = this.getScale();
        for (int k = 0; k < 6; k++) {
            double along = (this.random.nextDouble() - 0.4) * 1.6 * size;
            double across = (this.random.nextDouble() - 0.5) * 1.2 * size;
            this.level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, ground),
                    this.getX() - Mth.sin(yaw) * along + Mth.cos(yaw) * across,
                    this.getY() + this.getBbHeight() * (1.0 + this.random.nextDouble() * 0.15),
                    this.getZ() + Mth.cos(yaw) * along + Mth.sin(yaw) * across, 0.0, 0.0, 0.0);
        }
    }

    // Its tusks reach further than the game's reach, and a body this wide stops its charge a block short of what it
    // chases.
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(ATTACK_REACH, 0.0, ATTACK_REACH);
    }

    // Its tusks come up under what they hit and toss it.
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.35, 0.0));
            target.hurtMarked = true;
            this.playSound(STRIKES, 1.0F, 0.7F);
        }
        return hit;
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }

    @Override
    protected int getBaseExperienceReward() {
        return this.isBaby() ? 0 : 4 + this.random.nextInt(4);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 420;
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
    protected float getSoundVolume() {
        return 1.4F;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (this.isBaby() ? 1.0F : 0.7F);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(STEP, 0.25F, 0.6F);
    }

    private void trumpet() {
        this.playSound(TRUMPET, 2.0F, (this.isBaby() ? 1.4F : 1.0F) + (this.random.nextFloat() - 0.5F) * 0.1F);
    }

    // Comes on, and when its foe is near swings its head down and its tusks up under it. Seeing its foe first from a
    // little way off it spreads its ears, throws up its trunk and trumpets before it charges.
    private final class ChargeGoal extends MeleeAttackGoal {
        private int warning;
        private int swing;

        ChargeGoal() {
            super(Elephant.this, 1.6, true);
        }

        @Override
        public boolean canUse() {
            return !Elephant.this.isBaby() && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            LivingEntity target = Elephant.this.getTarget();
            if (target != null && Elephant.this.distanceToSqr(target) > 20.0) {
                this.warning = THREAT_END;
                Elephant.this.getNavigation().stop();
                Elephant.this.start(THREAT, THREAT_END);
                Elephant.this.trumpet();
            }
        }

        @Override
        public void tick() {
            LivingEntity target = Elephant.this.getTarget();
            if (this.warning > 0) {
                this.warning--;
                if (target != null) {
                    Elephant.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
                return;
            }
            if (this.swing > 0 && --this.swing == 0 && target != null
                    && Elephant.this.isWithinMeleeAttackRange(target)
                    && Elephant.this.getSensing().hasLineOfSight(target)) {
                Elephant.this.doHurtTarget(target);
            }
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.swing == 0 && this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.swing = STRIKE_HIT;
                Elephant.this.start(STRIKE, STRIKE_END);
            }
        }

        @Override
        public void stop() {
            this.warning = 0;
            this.swing = 0;
            Elephant.this.end(THREAT);
            super.stop();
        }
    }

    private boolean nearCalf() {
        for (Elephant elephant : this.level().getEntitiesOfClass(Elephant.class, this.getBoundingBox().inflate(8.0,
                4.0, 8.0))) {
            if (elephant.isBaby()) {
                return true;
            }
        }
        return false;
    }

    // Hit, it turns on whoever hit it, and the grown elephants of its herd with it; a calf that is hit only calls
    // them.
    private final class GuardGoal extends HurtByTargetGoal {
        GuardGoal() {
            super(Elephant.this);
            this.setAlertOthers();
        }

        @Override
        public void start() {
            super.start();
            if (Elephant.this.isBaby()) {
                this.alertOthers();
                this.stop();
            }
        }

        @Override
        protected void alertOther(Mob mob, LivingEntity target) {
            if (mob instanceof Elephant && !mob.isBaby()) {
                super.alertOther(mob, target);
            }
        }
    }

    // A grown elephant with a calf near it goes for any player who comes close. The calf is asked in the target's
    // own test, so the Mobs page (MobDefaults) reads the elephant as neutral, as it is without a calf.
    private final class CalfGoal extends NearestAttackableTargetGoal<Player> {
        CalfGoal() {
            super(Elephant.this, Player.class, 20, true, true, player -> Elephant.this.nearCalf());
        }

        @Override
        public boolean canUse() {
            return !Elephant.this.isBaby() && super.canUse();
        }

        @Override
        protected double getFollowDistance() {
            return super.getFollowDistance() * 0.3;
        }
    }
}
